// func (args) or block (locals)
data class Var (val id: String, val tmp: Boolean, val tag: String?, val init: Boolean, val upv: Int, val blk: Expr.Do)    // blk = [Block,Group,Proto]

class Vars (val outer: Expr.Do, val ups: Ups) {
    val pub = mutableMapOf<Expr,MutableMap<String,Var>> (
        Pair (
            outer,
            GLOBALS.map {
                Pair(it,Var(it,false, null, true,0,outer))
            }.toMap().toMutableMap()
        )
    )

    init {
        this.outer.traverse()
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
        val dcl = this.get(e,id)
        val nocross = dcl?.blk.let { blk ->
            (blk == null) || ups.all_until(e) { it==blk }.none { it is Expr.Proto }
        }
        return when {
            (dcl == null) -> {
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
            (dcl.upv==0 && upv>0 || dcl.upv==1 && upv==0) -> err(tk, "access error : incompatible upval modifier") as Var
            (upv==2 && nocross) -> err(tk, "access error : unnecessary upref modifier") as Var
            else -> dcl
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> this.body.traverse()
            is Expr.Do -> {
                if (this!=outer && this.ishide) {
                    val proto = ups.pub[this]
                    pub[this] = if (proto !is Expr.Proto) {
                        mutableMapOf()
                    } else {
                        proto.args.let {
                            (it.map { (id,tag) ->
                                Pair(id.str, Var(id.str, false, tag?.str, true, id.upv, this))
                            } + it.map { (id,_) ->
                                Pair("_${id.str}_", Var("_${id.str}_", false, null, false, id.upv, this))
                            })
                        }.toMap().toMutableMap()
                    }
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl -> {
                this.src?.traverse()
                val id = this.tk.str
                val bup = ups.first(this) { it is Expr.Do && it.ishide }!! as Expr.Do
                val xup = pub[bup]!!
                assertIsNotDeclared(this, id, this.tk)
                xup[id] = Var(id, this.tmp, this.tag?.str, this.init, this.tk_.upv, bup)
                xup["_${id}_"] = Var("_${id}_", false,null, false, this.tk_.upv, bup)
                when {
                    (this.tk_.upv == 2) -> {
                        err(tk, "var error : cannot declare an upref")
                    }
                    (this.tk_.upv==1 && bup==outer) -> {
                        err(tk, "var error : cannot declare a global upvar")
                    }
                }
            }
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.While  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
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
            is Expr.EvtErr -> {}
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
            }
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
