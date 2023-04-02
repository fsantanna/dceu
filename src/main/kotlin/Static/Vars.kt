// func (args) or block (locals)
data class Var (val blk: Expr.Do, val dcl: Expr.Dcl)    // blk = [Block,Group,Proto]

typealias LData = List<Pair<Tk.Id,Tk.Tag?>>

class Vars (val outer: Expr.Do, val ups: Ups) {
    val pub = mutableMapOf<Expr,MutableMap<String,Var>> (
        Pair (
            outer,
            GLOBALS.map {
                Pair (
                    it,
                    Var (
                        outer,
                        Expr.Dcl (
                            Tk.Fix("val", outer.tk.pos),
                            Tk.Id(it,outer.tk.pos,0),
                            /*false,*/ false, null, true, null
                        )
                    )
                )
            }.toMap().toMutableMap()
        )
    )

    val evts: MutableMap<Expr.EvtErr, String?> = mutableMapOf()
    val datas = mutableMapOf<String,LData>()

    init {
        this.outer.traverse()
    }

    fun data (e: Expr): Pair<Int?,LData?>? {
        return when (e) {
            is Expr.Acc -> {
                val xvar = get(e, e.tk.str)!!
                if (xvar.dcl.tag == null) {
                    null
                } else {
                    Pair(null, this.datas[xvar.dcl.tag.str]!!)
                }
            }
            is Expr.EvtErr -> {
                val x = evts[e]
                when {
                    (x == null) -> null
                    (this.datas[x] == null) -> null
                    else -> Pair(null, this.datas[x])
                }
            }
            is Expr.Pub -> when (e.x) {
                is Expr.Self -> {
                    // task.pub -> task (...) :T {...}
                    val task = ups.first_true_x(e,"task")
                    when {
                        (task == null) -> null
                        (task.task!!.first == null) -> null
                        else -> {
                            val tag = task.task.first!!.str
                            Pair(null, this.datas[tag]!!)
                        }
                    }
                }
                is Expr.Acc -> {
                    // x.pub -> x:T
                    val xvar = get(e, e.x.tk.str)!!
                    if (xvar.dcl.tag == null) {
                        null
                    } else {
                        val tag = xvar.dcl.tag.str
                        Pair(null, this.datas[tag]!!)
                    }
                }
                else -> null
            }
            is Expr.Index -> {
                val d = this.data(e.col)
                val l = d?.second
                when {
                    (d == null) -> null
                    (l == null) -> null
                    (e.idx !is Expr.Tag) -> null
                    else -> {
                        val idx = l.indexOfFirst { it.first.str == e.idx.tk.str.drop(1) }
                        val v = if (idx == -1) null else l[idx]
                        when {
                            (v == null) -> {
                                err(e.idx.tk, "index error : undeclared data field ${e.idx.tk.str}")
                                error("unreachable")
                            }
                            (v.second == null) -> Pair(idx, null)
                            else -> Pair(idx, this.datas[v.second!!.str]!!)
                        }
                    }
                }
            }
            else -> null
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
            (xvar.dcl.id.upv==0 && upv>0 || xvar.dcl.id.upv==1 && upv==0) -> err(tk, "access error : incompatible upval modifier") as Var
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

                this.args.forEach { (_,tag) ->
                    if (tag!=null && !datas.containsKey(tag.str)) {
                        err(tag, "declaration error : data ${tag.str} is not declared")
                    }
                }

                this.body.traverse()
            }
            is Expr.Do     -> {
                if (this!=outer && this.tag?.str!=":unnest") {
                    val proto = ups.pub[this]
                    pub[this] = if (proto !is Expr.Proto) {
                        mutableMapOf()
                    } else {
                        proto.args.let {
                            (it.map { (id,tag) ->
                                val dcl = Expr.Dcl (
                                    Tk.Fix("val", this.tk.pos),
                                    id, /*false,*/ false, tag, true, null
                                )
                                Pair(id.str, Var(this, dcl))
                            } + it.map { (id,_) ->
                                val dcl = Expr.Dcl(
                                    Tk.Fix("val", this.tk.pos),
                                    Tk.Id("_${id.str}_",id.pos,id.upv),
                                    /*false,*/
                                    false, null, false, null
                                )
                                Pair("_${id.str}_", Var(this, dcl))
                            })
                        }.toMap().toMutableMap()
                    }
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl    -> {
                this.src?.traverse()

                val id = this.id.str
                val bup = ups.first(this) { it is Expr.Do && it.tag?.str!=":unnest" }!! as Expr.Do
                val xup = pub[bup]!!
                assertIsNotDeclared(this, id, this.tk)
                xup[id] = Var(bup, this)
                val dcl = Expr.Dcl (
                    Tk.Fix("val", this.id.pos),
                    Tk.Id("_${id}_",this.id.pos,this.id.upv),
                    /*false,*/ false, null, false, null
                )
                xup["_${id}_"] = Var(bup, dcl)
                when {
                    (this.id.upv == 2) -> {
                        err(tk, "var error : cannot declare an upref")
                    }
                    (this.id.upv==1 && bup==outer) -> {
                        err(tk, "var error : cannot declare a global upvar")
                    }
                }

                if (id!="evt" && this.tag!=null && !datas.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.body.traverse()
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
                data(this)
            }
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
