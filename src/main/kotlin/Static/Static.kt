package dceu

class Static (outer: Expr.Do, val ups: Ups, val vars: Vars) {
    val unused: MutableSet<Expr.Dcl> = mutableSetOf()
    val cons:   MutableSet<Expr.Do>  = mutableSetOf() // at least 1 constructor
    val ylds:   MutableSet<Expr.Do>  = mutableSetOf() // at least 1 yield (including subs)

    init {
        outer.traverse()
    }

    // ylds: block yields -> vars must be in mem
    // void: block is innocuous -> should be a proxy to up block

    fun void (blk: Expr.Do): Boolean {
        val dcls = vars.blk_to_dcls[blk]!!
        val f_b = ups.pub[blk]?.let { ups.first_proto_or_block(it) }
        return (f_b is Expr.Do) && dcls.isEmpty() && !this.cons.contains(blk)
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                cons.add(ups.first_block(this)!!)
                this.body.traverse()
            }
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

            is Expr.Catch  -> { this.cnd?.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()

            is Expr.Yield  -> {
                if (!ups.inexe(this)) {
                    err(this.tk, "yield error : expected enclosing coro" + (if (CEU<=3) "" else "or task"))
                }
                ups.all_until(this) { it is Expr.Proto }
                    .filter  { it is Expr.Do }              // all blocks up to proto
                    .forEach { ylds.add(it as Expr.Do) }
                this.arg.traverse()
            }
            is Expr.Resume -> {
                this.call.traverse()
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val (_,dcl) = vars.get(this)
                unused.remove(dcl)
                //err(this.tk, "access error : cannot access \"_\"")
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
                this.clo.traverse()
                this.args.forEach { it.traverse() }
            }
        }
    }
}
