class Static (val outer: Expr.Do, val ups: Ups, val vars: Vars) {
    val evts: MutableMap<Expr.EvtErr, String?> = mutableMapOf()
    val datas = mutableMapOf<String,List<Pair<Tk.Id,Tk.Tag?>>>()

    // funcs that set vars in enclosing tasks
    //  - they are marked and cannot receive "evt"
    //  - otherwise, we do not check with ceu_block_set
    val funcs_vars_tasks = mutableSetOf<Expr.Proto>()

    init {
        this.outer.traverse()
    }

    fun data_is (e: Expr.Index): Boolean {
        val id = e.col.tk.str
        val dcl = vars.get(e, id)
        return when (e.col) {
            is Expr.Pub -> when (e.col.x) {
                // task.pub -> task (...) :T {...}
                is Expr.Self -> (e.idx is Expr.Tag) && (ups.first_true_x(e,"task").let { it!=null && it.task!!.first!=null })
                // x.pub -> x:T
                is Expr.Acc -> (e.idx is Expr.Tag) && (vars.get(e, e.col.x.tk.str)!!.tag != null)
                // x.y.pub -> x.y?
                is Expr.Index -> this.data_is(e.col.x)
                // detrack(x).pub
                is Expr.Call -> false   // TODO
                else -> error("impossible case")
            }
            is Expr.EvtErr -> (e.idx is Expr.Tag) && (dcl != null) && (evts[e.col] != null)
            is Expr.Acc    -> (e.idx is Expr.Tag) && (dcl!!.tag != null)
            is Expr.Index  -> this.data_is(e.col)
            else           -> false
        }
    }
    fun data_lst (e: Expr.Index): List<Pair<Tk.Id, Tk.Tag?>> {
        val id = e.col.tk.str
        return when {
            (e.col is Expr.Pub) -> when (e.col.x) {
                is Expr.Self -> {
                    // task.pub -> task (...) :T {...}
                    val tag = ups.first_true_x(e,"task")!!.task!!.first!!.str
                    this.datas[tag]!!
                }
                is Expr.Acc -> {
                    // x.pub -> x:T
                    val tag = vars.get(e, e.col.x.tk.str)!!.tag!!
                    this.datas[tag]!!
                }
                is Expr.Index -> {
                    // x.y.pub -> x.y?
                    this.data_is(e.col.x)
                    TODO()
                }
                else -> error("impossible case")
            }
            (e.col is Expr.EvtErr) -> {
                val tag = evts[e.col]!!
                this.datas[tag]!!
            }
            (e.col is Expr.Acc) -> {
                val dcl = vars.get(e, id)!!
                this.datas[dcl.tag]!!
            }
            (e.col is Expr.Index) -> {
                e.col.idx as Expr.Tag
                val xid = e.col.idx.tk.str.drop(1)
                val lst = this.data_lst(e.col)
                val id_tag = lst.firstOrNull { it.first.str==xid }!!
                if (id_tag.second == null) {
                    err(e.idx.tk, "index error : field \"$xid\" is not a data")
                }
                this.datas[id_tag.second!!.str]!!
            }
            else -> error("impossible case")
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> {
                if (this.task!=null && this.task.first!=null && !datas.containsKey(this.task.first!!.str)) {
                    val tag = this.task.first!!
                    err(tag, "declaration error : data ${tag.str} is not declared")
                }
                this.body.traverse()
            }
            is Expr.Do -> this.es.forEach { it.traverse() }
            is Expr.Dcl -> {
                this.src?.traverse()
                val id = this.tk.str
                if (id!="evt" && this.tag!=null && !datas.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
            }
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
                val func = ups.first(this) { it is Expr.Proto && it.tk.str=="func" }
                if (func != null) {
                    val acc = this.dst.base()
                    val dcl = vars.get(this, acc.tk.str)!!
                    val intask = ups.first(dcl.blk) { it is Expr.Proto }.let { it!=null && it.tk.str!="func" }
                    if (intask) {
                        funcs_vars_tasks.add(func as Expr.Proto)
                    }
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.While  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> {}
            is Expr.Data -> {
                val sup = this.tk.str.dropLastWhile { it != '.' }.dropLast(1)
                if (datas.containsKey(this.tk.str)) {
                    err(this.tk, "data error : data ${this.tk.str} is already declared")
                }
                val ids = (datas[sup] ?: emptyList()) + this.ids
                val xids = ids.map { it.first.str }
                if (xids.size != xids.distinct().size) {
                    err(this.tk, "data error : found duplicate ids")
                }
                ids.forEach { (_,tag) ->
                    if (tag!=null && !datas.containsKey(tag.str)) {
                        err(tag, "data error : data ${tag.str} is not declared")
                    }
                }
                datas[this.tk.str] = ids
            }
            is Expr.Pass   -> this.e.traverse()

            is Expr.Spawn  -> { this.call.traverse() ; this.tasks?.traverse() }
            is Expr.Bcast  -> { this.xin.traverse() ; this.evt.traverse() }
            is Expr.Yield  -> {
                if (!ups.intask(this)) {
                    err(this.tk, "yield error : expected enclosing coro or task")
                }
                this.arg.traverse()
            }
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> {
                this.x.traverse()
                if (this.x is Expr.Self) {
                    val ok = (ups.first_true_x(this,this.x.tk.str) != null)
                    if (!ok) {
                        err(this.tk, "${this.tk.str} error : expected enclosing task")
                    }
                }
            }
            is Expr.Self   -> {
                if (ups.true_x_c(this, this.tk.str) == null) {
                    err(this.tk, "task error : missing enclosing task")
                }
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.EvtErr -> {
                val dcl = vars.get(this, "evt")
                if (dcl?.tag != null) {
                    evts[this] = dcl.tag
                }
            }
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach{ it.traverse() }
            is Expr.Vector -> this.args.forEach{ it.traverse() }
            is Expr.Dict   -> this.args.forEach { it.first.traverse() ; it.second.traverse() }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()

                if (data_is(this)) {
                    val id = this.idx.tk.str.drop(1)
                    val idx = data_lst(this).indexOfFirst { it.first.str==id }
                    if (idx == -1) {
                        err(this.idx.tk, "index error : undeclared field \"$id\"")
                    }
                }
            }
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
