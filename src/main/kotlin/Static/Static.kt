package dceu

class Static (outer: Expr.Do, val ups: Ups, val vars: Vars) {
    val unused: MutableSet<Expr.Dcl> = mutableSetOf()
    val spws:   MutableSet<Expr.Do>  = mutableSetOf() // at least 1 spawn
    val ylds:   MutableSet<Expr.Do>  = mutableSetOf() // at least 1 yield (including subs)

    init {
        outer.traverse()
    }

    // ylds: block yields -> vars must be in mem
    // void: block is innocuous -> should be a proxy to up block

    fun void (blk: Expr.Do): Boolean {
        // has up block, no declarations, no spawns
        val dcls = vars.blk_to_dcls[blk]!!
        val f_b = ups.pub[blk]?.let { ups.first_proto_or_block(it) }
        return (f_b is Expr.Do) && dcls.isEmpty() && !this.spws.contains(blk)
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                spws.add(ups.first_block(this)!!)
                this.body.traverse()
            }
            is Expr.Do     -> {
                //if (ups.first(this) { it is Expr.Proto }.let { it!=null && it.tk.str!="func" }) {
                //    ylds.add(this)
                //}
                this.es.forEach { it.traverse() }
                if (ylds.contains(this)) {
                    vars.blk_to_dcls[this]?.forEach {
                        if (it.tmp) {
                            err(it.tk, "invalid declaration : \":tmp\" across yield")
                        }
                    }
                }
            }
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
                if (ups.pub[this] is Expr.Do && ups.pub[ups.pub[this]] is Expr.XLoop) {
                    // ok
                } else {
                    err(this.tk, "xbreak error : expected parent loop")
                }
                this.cnd.traverse()
                this.e?.traverse()
            }
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()
            is Expr.Drop   -> this.e.traverse()

            is Expr.It     -> {}
            is Expr.Catch  -> {
                this.cnd.traverse()
                this.body.traverse()
            }
            is Expr.Defer  -> this.body.traverse()

            is Expr.Yield  -> {
                when {
                    !ups.inexe(this)
                        -> err(this.tk, "yield error : expected enclosing coro" + (if (CEU <= 3) "" else " or task"))
                    ups.any(this) { cnd ->
                        ups.pub[cnd].let { catch ->
                            ((catch is Expr.Catch) && catch.cnd==cnd)
                        }
                    }
                        -> err(this.tk, "yield error : unexpected enclosing catch")
                    ups.any(this) { blk ->
                        ups.pub[blk].let { yld ->
                            ((yld is Expr.Yield) && yld.blk==blk)
                        }
                    }
                        -> err(this.tk, "yield error : unexpected enclosing yield")
                }
                ups.all_until(this) { it is Expr.Proto }
                    .filter  { it is Expr.Do }              // all blocks up to proto
                    .forEach { ylds.add(it as Expr.Do) }
                this.arg.traverse()
                this.blk.traverse()
            }
            is Expr.Resume -> {
                this.call.traverse()
            }

            is Expr.Spawn  -> {
                spws.add(ups.first_block(this)!!)
                this.tasks?.traverse()
                this.call.traverse()
            }
            is Expr.Bcast  -> {
                this.xin.traverse()
                this.evt.traverse()
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
            is Expr.Tuple  -> this.args.forEach{ it.traverse() }
            is Expr.Vector -> this.args.forEach{ it.traverse() }
            is Expr.Dict   -> spws.add(ups.first_block(this)!!)
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
