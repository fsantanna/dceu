package dceu

class Static (val outer: Expr.Do, val ups: Ups, val vars: Vars) {
    val unused: MutableSet<Expr.Dcl> = mutableSetOf()
    val spws:   MutableSet<Expr.Do>  = mutableSetOf() // at least 1 spawn
    val ylds:   MutableSet<Expr.Do>  = mutableSetOf() // at least 1 yield (including subs) or nested coro/task

    init {
        outer.traverse()
    }

    // ylds: block yields -> vars must be in Mem (TODO: spawn also, see gh_01_set)
    // void: block is innocuous -> should be a proxy to up block

    fun void (blk: Expr.Do): Boolean {
        //return false
        // has up block, no declarations, no spawns
        val dcls = vars.blk_to_dcls[blk]!!
        val f_b = ups.pub[blk]?.let { ups.first_proto_or_block(it) }
        return (f_b is Expr.Do) && dcls.isEmpty() && !this.spws.contains(blk)
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                spws.add(ups.first_block(this)!!)
                this.blk.traverse()
            }
            is Expr.Export -> this.blk.traverse()
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> {
                unused.add(this)
                this.src?.traverse()
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
                /*
                val thus = ups.first(this) { it is Expr.Do && it.tk.str=="thus"}
                if (thus != null) {
                    if (ups.any(this.dst.base(ups)) { it==thus }) {
                        err(this.tk, "set error : destination across thus")
                    }
                }
                 */
                if (this.dst is Expr.Acc) {
                    val (_,dcl) = vars.get(this.dst)
                    if (dcl.tk.str == "val") {
                        err(this.tk, "set error : destination is immutable")
                    }
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop  -> {
                this.blk.es.last().let {
                    if (it.is_innocuous()) {
                        err(it.tk, "loop error : innocuous last expression")
                    }
                }
                this.blk.traverse()
            }
            is Expr.Break -> {
                if (ups.pub[this] is Expr.Do && ups.pub[ups.pub[this]] is Expr.Loop) {
                    // ok
                } else {
                    err(this.tk, "break error : expected parent loop")
                }
                this.cnd.traverse()
                this.e?.traverse()
            }
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> {
                this.cnd.traverse()
                this.blk.traverse()
            }
            is Expr.Defer  -> {
                val f = ups.first(this) { it is Expr.Proto && it.tk.str=="func" }
                if (f != null) {
                    val co = ups.any(f) { it is Expr.Proto && it.tk.str!="func" }
                    if (co) {
                        err(this.tk, "defer error : unexpected func with enclosing coro or task")
                    }
                }
                this.blk.traverse()
            }

            is Expr.Yield  -> {
                ups.all_until(this) { it is Expr.Proto }
                    .filter  { it is Expr.Do }              // all blocks up to proto
                    .forEach { ylds.add(it as Expr.Do) }
                this.arg.traverse()
                when {
                    !ups.inexe(this,true)
                        -> err(this.tk, "yield error : expected enclosing coro" + (if (CEU <= 3) "" else " or task"))
                    ups.any(this) { defer -> (defer is Expr.Defer) }
                        -> err(this.tk, "yield error : unexpected enclosing defer")
                    ups.any(this) { cnd ->
                        ups.pub[cnd].let { catch ->
                            ((catch is Expr.Catch) && catch.cnd==cnd)
                        }
                    }
                        -> err(this.tk, "yield error : unexpected enclosing catch")
                    ups.any(this) { blk -> blk is Expr.Do && blk.tk.str=="thus" && !blk.es[0].let { it is Expr.Dcl && it.src==this } }
                        -> err(this.tk, "yield error : unexpected enclosing thus")
                }
            }
            is Expr.Resume -> {
                this.co.traverse()
                this.arg.traverse()
            }

            is Expr.Spawn  -> {
                spws.add(ups.first_block(this)!!)
                this.tsks?.traverse()
                this.tsk.traverse()
                this.arg.traverse()
                when {
                    (ups.first(this) { f -> ((f is Expr.Proto) && f.tk.str == "func") } != null)
                       -> err(this.tk, "spawn error : unexpected enclosing func")
                }
            }
            is Expr.Pub    -> {
                this.tsk?.traverse()
                when {
                    (this.tsk != null) -> {}
                    (ups.first_task_real(this) == null) -> err(this.tk, "pub error : expected enclosing task")
                    else -> {}
                }
            }
            is Expr.Dtrack -> this.tsk.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val (blk,dcl) = vars.get(this)
                unused.remove(dcl)
                //err(this.tk, "access error : cannot access \"_\"")

                if (blk!=outer && ups.none(blk) { it is Expr.Proto && it.tk.str!="func" }) {
                    val coro = ups.first(this) { it is Expr.Proto && it.tk.str!="func" }
                    if (coro != null) {
                        if (ups.any(coro) { it==blk}) {
                            err(this.tk, "access error : cannot access local across coro" + (CEU>=4).cond { " or task" })
                        }
                    }
                }
            }
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach { it.traverse() }
            is Expr.Vector -> this.args.forEach { it.traverse() }
            is Expr.Dict   -> this.args.forEach { (k,v) -> k.traverse() ; v.traverse() }
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
