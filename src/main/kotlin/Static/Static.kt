package dceu

class Static (outer: Expr.Do, val ups: Ups, val vars: Vars) {
    val unused: MutableSet<Expr.Dcl> = mutableSetOf()
    val cons: MutableSet<Expr.Do> = mutableSetOf() // block has at least 1 constructor

    init {
        outer.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                cons.add(ups.first_block(this)!!)
                this.body.traverse()
            }
            is Expr.Export -> this.body.traverse()
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> {
                unused.add(this)
                this.src?.traverse()
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
                if (this.dst is Expr.Acc) {
                    val (_,dcl) = vars.get(this.dst)
                    if (dcl.tk.str == "val") {
                        err(this.tk, "invalid set : destination is immutable")
                    }
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.XLoop  -> {
                this.body.es.last().let {
                    if (it.is_innocuous()) {
                        //err(it.tk, "invalid expression : innocuous expression")
                        TODO("never reachable - checked in parser - remove in the future")
                    }
                }
                this.body.traverse()
            }
            is Expr.XBreak -> {
                if (ups.first(this) { it is Expr.XLoop || it is Expr.Proto } !is Expr.XLoop) {
                    err(this.tk, "xbreak error : expected enclosing loop")
                }
            }
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val (_,dcl) = vars.get(this)
                unused.remove(dcl)
                if (CEU>=2 && this.tk.str=="_") {
                    err(this.tk, "access error : cannot access \"_\"")
                }
            }
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> {
                cons.add(ups.first_block(this)!!)
                this.args.forEach{ it.traverse() }
            }
            is Expr.Vector -> {
                cons.add(ups.first_block(this)!!)
                this.args.forEach{ it.traverse() }
            }
            is Expr.Dict   -> {
                cons.add(ups.first_block(this)!!)
                this.args.forEach { it.first.traverse() ; it.second.traverse() }
            }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()
            }
            is Expr.Call   -> {
                this.closure.traverse()
                this.args.forEach { it.traverse() }
            }
        }
    }
}
