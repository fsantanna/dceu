// func (args) or block (locals)
data class Var (val blk: Expr.Do, val dcl: Expr.Dcl)    // blk = [Block,Group,Proto]

class Vars (val outer: Expr.Do, val ups: Ups) {
    val pub = mutableMapOf<Expr,MutableMap<String,Var>> (
        Pair (
            outer,
            GLOBALS.map {
                Pair (
                    it,
                    Var (
                        outer,
                        Expr.Dcl(Tk.Id(it,outer.tk.pos,0),/*false,*/ false, null,true,null)
                    )
                )
            }.toMap().toMutableMap()
        )
    )

    val evts: MutableMap<Expr.EvtErr, String?> = mutableMapOf()
    val datas = mutableMapOf<String,List<Pair<Tk.Id,Tk.Tag?>>>()

    init {
        this.outer.traverse()
    }

    fun data_is (e: Expr.Index): Boolean {
        val id = e.col.tk.str
        val xvar = get(e, id)
        return when (e.col) {
            is Expr.Pub -> when (e.col.x) {
                // task.pub -> task (...) :T {...}
                is Expr.Self -> (e.idx is Expr.Tag) && (ups.first_true_x(e,"task").let { it!=null && it.task!!.first!=null })
                // x.pub -> x:T
                is Expr.Acc -> (e.idx is Expr.Tag) && (get(e, e.col.x.tk.str)!!.dcl.tag != null)
                // x.y.pub -> x.y?
                is Expr.Index -> this.data_is(e.col.x)
                // detrack(x).pub
                is Expr.Call -> false   // TODO
                else -> error("impossible case")
            }
            is Expr.EvtErr -> (e.idx is Expr.Tag) && (xvar != null) && (evts[e.col] != null)
            is Expr.Acc    -> (e.idx is Expr.Tag) && (xvar!!.dcl.tag != null)
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
                    val tag = get(e, e.col.x.tk.str)!!.dcl.tag!!.str
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
                val xvar = get(e, id)!!
                this.datas[xvar.dcl.tag!!.str]!!
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

    fun get (e: Expr, id: String): Var? {
        val up = ups.pub[e]
        val dcl = this.pub[e]?.get(id)
        return when {
            (dcl != null) -> dcl
            (up == null) -> null
            else -> this.get(up, id)
        }
    }
    fun assertIsNotDeclared (e: Expr, id: String, tk: Tk) {
        if (this.get(e,id)!=null && id!="evt") {
            err(tk, "declaration error : variable \"$id\" is already declared")
        }
    }
    fun assertIsDeclared (e: Expr, v: Pair<String,Int>, tk: Tk): Var {
        val (id,upv) = v
        val xvar = this.get(e,id)
        val nocross = xvar?.blk.let { blk ->
            (blk == null) || ups.all_until(e) { it==blk }.none { it is Expr.Proto }
        }
        return when {
            (xvar == null) -> {
                val l = id.split('-')
                val x = l
                    .mapIndexed { i,_ ->
                        l.drop(i).scan(emptyList<String>()) { acc, s -> acc + s }
                    }
                    .flatten()
                    .filter { it.size>0 && it.size<l.size }
                    .map { it.joinToString("-") }
                val amb = x.firstOrNull { this.get(e,it) != null }
                if (amb != null) {
                    err(tk, "access error : \"${id}\" is ambiguous with \"${amb}\"") as Var
                } else {
                    err(tk, "access error : variable \"${id}\" is not declared") as Var
                }
            }
            (xvar.dcl.tk_.upv==0 && upv>0 || xvar.dcl.tk_.upv==1 && upv==0) -> err(tk, "access error : incompatible upval modifier") as Var
            (upv==2 && nocross) -> err(tk, "access error : unnecessary upref modifier") as Var
            else -> xvar
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                if (this.task!=null && this.task.first!=null && !datas.containsKey(this.task.first!!.str)) {
                    val tag = this.task.first!!
                    err(tag, "declaration error : data ${tag.str} is not declared")
                }
                this.body.traverse()
            }
            is Expr.Do     -> {
                if (this!=outer && this.ishide) {
                    val proto = ups.pub[this]
                    pub[this] = if (proto !is Expr.Proto) {
                        mutableMapOf()
                    } else {
                        proto.args.let {
                            (it.map { (id,tag) ->
                                val dcl = Expr.Dcl(id, /*false,*/ false, tag, true, null)
                                Pair(id.str, Var(this, dcl))
                            } + it.map { (id,_) ->
                                val dcl = Expr.Dcl(Tk.Id("_${id.str}_",id.pos,id.upv), /*false,*/ false, null, false, null)
                                Pair("_${id.str}_", Var(this, dcl))
                            })
                        }.toMap().toMutableMap()
                    }
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl    -> {
                val id = this.tk.str
                val bup = ups.first(this) { it is Expr.Do && it.ishide }!! as Expr.Do
                val xup = pub[bup]!!
                assertIsNotDeclared(this, id, this.tk)
                xup[id] = Var(bup, this)
                val dcl = Expr.Dcl(Tk.Id("_${id}_",this.tk.pos,this.tk_.upv), /*false,*/ false, null, false, null)
                xup["_${id}_"] = Var(bup, dcl)
                when {
                    (this.tk_.upv == 2) -> {
                        err(tk, "var error : cannot declare an upref")
                    }
                    (this.tk_.upv==1 && bup==outer) -> {
                        err(tk, "var error : cannot declare a global upvar")
                    }
                }

                if (id!="evt" && this.tag!=null && !datas.containsKey(this.tag.str)) {
                    //err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }

                this.src?.traverse()
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.While  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> {}
            is Expr.Data   -> {
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
            is Expr.Yield  -> this.arg.traverse()
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> this.x.traverse()
            is Expr.Self   -> {}

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val id = this.tk.str
                assertIsDeclared(this, Pair(id,this.tk_.upv), this.tk)
                if (GLOBALS.contains(id)) {
                    // TODO: create _id_ for globals
                } else {
                    assertIsDeclared(this, Pair("_${id}_",this.tk_.upv), this.tk)
                }
            }
            is Expr.EvtErr -> {
                val xvar = get(this, "evt")
                if (xvar != null) {
                    evts[this] = xvar.dcl.tag!!.str
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
