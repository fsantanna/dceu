package dceu

class Static (val outer: Expr.Call, val ups: Ups, val vars: Vars) {
    // funs: const func is not used: do not generate code
    // val f = func ()
    val xfuns: MutableMap<Expr.Proto, MutableSet<Expr.Proto>> = mutableMapOf(
        Pair(outer.clo as Expr.Proto, mutableSetOf())
    )
    val funs: MutableSet<Expr.Proto> = mutableSetOf()

    // void: block is innocuous -> should be a proxy to up block
    fun void (blk: Expr.Do): Boolean {
        //return false
        // no declarations, no spawns, no tasks
        val dcls = vars.enc_to_dcls[blk]!!
        return dcls.isEmpty() && (ups.pub[blk] !is Expr.Proto) && !this.tsk_or_tsks.contains(blk)
    }
    val tsk_or_tsks: MutableSet<Expr.Do> = mutableSetOf()

    init {
        outer.traverse()

        // xfuns -> funs
        fun f (proto: Expr.Proto) {
            if (funs.contains(proto)) {     // if breaks recursion
                funs.remove(proto)          // found access, remove it
                xfuns[proto]!!.forEach { f(it) }
            }
        }
        funs.addAll(xfuns.keys)     // assume none are acessed
        f(outer.clo as Expr.Proto)  // start with accesses from outer
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                if (this.rec) {
                    if (!this.is_val(ups)) {
                        err(this.tk, "${this.tk.str} :rec error : expected enclosing val declaration")
                    }
                }
                if (this.nst) {
                    if (ups.first(ups.pub[this]!!) { it is Expr.Proto }.let {
                        (CEU>=99 && it==outer.clo) /* bc of top-level spawn {...} */ ||
                        (it!=null && it.tk.str=="task") })
                    {
                        // ok
                    } else {
                        err(this.tk, "task :nested error : expected enclosing task")
                    }

                    /*
                    val up1 = ups.pub[this]
                    val up2 = if (up1==null) null else ups.pub[up1]
                    when {
                        (up1 !is Expr.Spawn) -> err(this.tk, "task :nested error : expected enclosing spawn")
                        (up2 !is Expr.Do) -> err(up1.tk, "spawn task :nested error : expected immediate enclosing block")
                        (ups.pub[up2] == outer.clo) -> {}    // OK: outer spawn
                        (up2.es.last() == up1) -> err(up1.tk, "spawn task :nested error : cannot escape enclosing block")
                    }
                     */
                }
                this.blk.traverse()
            }
            is Expr.Export -> this.blk.traverse()
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> {
                if (this.tk.str=="val" && this.src is Expr.Proto) {
                    xfuns[this.src] = mutableSetOf()
                }
                this.src?.traverse()
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
                if (this.dst is Expr.Acc) {
                    val dcl = vars.acc_to_dcl[this.dst]!!
                    if (dcl.tk.str == "val") {
                        err(this.tk, "set error : destination is immutable")
                    }
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> {
                this.blk.es.last().let {
                    if (it.is_innocuous()) {
                        err(it.tk, "loop error : innocuous last expression")
                    }
                }
                this.blk.traverse()
            }
            is Expr.Break  -> {
                var up = ups.pub[this]
                while (up is Expr.Do) {
                    up = ups.pub[up]    // skip nested do's from late declarations
                }
                if (up is Expr.Loop) {
                    // ok
                } else {
                    err(this.tk, "break error : expected immediate parent loop")
                }
                this.cnd.traverse()
                this.e?.traverse()
            }
            is Expr.Skip   -> {
                if (ups.pub[this] is Expr.Do && ups.pub[ups.pub[this]] is Expr.Loop) {
                    // ok
                } else {
                    err(this.tk, "skip error : expected immediate parent loop")
                }
                this.cnd.traverse()
            }
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()

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
                this.arg.traverse()
                when {
                    ups.any(this) { defer -> (defer is Expr.Defer) }
                        -> err(this.tk, "yield error : unexpected enclosing defer")
                    ups.any(this) { cnd ->
                        ups.pub[cnd].let { catch ->
                            ((catch is Expr.Catch) && catch.cnd==cnd)
                        }
                    }
                        -> err(this.tk, "yield error : unexpected enclosing catch")
                    ups.first(this) { it is Expr.Proto }.let { it?.tk?.str=="func" && it!=outer.main() }
                        -> err(this.tk, "yield error : unexpected enclosing func")
                    (ups.exe(this) == null)
                        -> err(this.tk, "yield error : expected enclosing coro" + (if (CEU <= 3) "" else " or task"))
                    //ups.any(this) { it is Expr.Do && it.arg!=null }
                    //    -> err(this.tk, "yield error : unexpected enclosing thus")
                }
            }
            is Expr.Resume -> {
                this.co.traverse()
                this.arg.traverse()
            }

            is Expr.Spawn  -> {
                this.tsks?.traverse()
                this.tsk.traverse()
                this.args.forEach { it.traverse() }
                if (this.tsks == null) {
                    tsk_or_tsks.add(ups.first(this) { it is Expr.Do } as Expr.Do)
                }
                /*
                when {
                    (ups.first(this) { f -> ((f is Expr.Proto) && f.tk.str == "func") } != null)
                       -> err(this.tk, "spawn error : unexpected enclosing func")
                }
                 */
            }
            is Expr.Delay  -> {
                if (!ups.first(this) { it is Expr.Proto }.let { it?.tk?.str=="task" }) {
                    //err(this.tk, "delay error : expected enclosing task")
                }
            }
            is Expr.Pub    -> {
                if (this.tsk == null) {
                    val outer = ups.first_task_outer(this)
                    val ok = (outer != null) && ups.all_until(this) { it == outer }.none { it is Expr.Proto && it.tk.str!="task" }
                    if (!ok) {
                        err(this.tk, "pub error : expected enclosing task")
                    }
                    //(ups.first_task_outer(this) == null) -> err(this.tk, "pub error : expected enclosing task")
                } else {
                    this.tsk.traverse()
                }
            }
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val dcl = vars.acc_to_dcl[this]!!
                if (xfuns.containsKey(dcl.src)) {
                    val up = ups.first(this) { it is Expr.Proto && (it==outer.clo || it.is_val(ups)) }
                    xfuns[up]!!.add(dcl.src as Expr.Proto)
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
                if (this.clo is Expr.Acc && this.clo.tk.str=="tasks") {
                    tsk_or_tsks.add(ups.first(this) { it is Expr.Do } as Expr.Do)
                }

            }
        }
    }
}
