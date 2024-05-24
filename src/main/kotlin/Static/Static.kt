package dceu

class Static (val outer: Expr.Do, val ups: Ups, val vars: Vars) {
    // protos_unused: const proto is not used: do not generate code
    val protos_use_unused: MutableSet<Expr.Proto> = mutableSetOf()
    val protos_use_map: MutableMap<Expr.Proto, MutableSet<Expr.Proto>> = mutableMapOf()
    fun protos_use_f (proto: Expr.Proto) {
        if (protos_use_unused.contains(proto)) {     // if breaks recursion
            protos_use_unused.remove(proto)          // found access, remove it
            protos_use_map[proto]!!.forEach {
                protos_use_f(it)
            }
        }
    }

    // at least 1 yield (including subs) or nested coro/task
    val ylds: MutableSet<Expr.Do>  = mutableSetOf()

    // void: block is innocuous -> should be a proxy to up block
    fun void (blk: Expr.Do): Boolean {
        // no declarations, no spawns, no tasks
        val dcls = vars.blk_to_dcls[blk]!!
        //println(listOf("-=-=-", blk.tk, ups.pub[blk]?.javaClass?.name))
        //println(blk.tostr())
        return when {
            //true -> false
            (ups.pub[blk] is Expr.Loop) -> false
            !dcls.isEmpty() -> false
            (ups.pub[blk] is Expr.Proto) -> false
            this.defer_catch_spawn_tasks.contains(blk) -> false
            else -> true
        }
    }
    val defer_catch_spawn_tasks: MutableSet<Expr.Do> = mutableSetOf()

    fun ismem (blk: Expr.Do): Boolean {
        return ylds.contains(blk) && !void(blk)
    }
    fun ismem (e: Expr): Boolean {
        return this.ismem(ups.first(e) { it is Expr.Do } as Expr.Do)
    }

    fun idx (acc: Expr.Acc): String {
        val dcl = vars.acc_to_dcl[acc]!!
        return this.idx(dcl, acc)
    }
    fun idx (dcl: Expr.Dcl, src: Expr): String {
        val id = dcl.idtag.first.str.idc()
        val ismem = this.ismem(vars.dcl_to_blk[dcl]!!)
        //println(listOf(src.tk.pos.lin, id, type(dcl,src)))
        return when (vars.type(dcl,src)) {
            Type.GLOBAL -> "ceu_glb_$id"
            Type.LOCAL -> if (ismem) "(ceu_mem->$id)" else "ceu_loc_$id"
            Type.PARAM -> if (ismem) "(ceu_mem->$id)" else "ceu_par_$id"
            /*
            Type.NESTED -> {
                val enc  = this.dcl_to_blk[dcl]!!
                val xups = ups.all_until(src) { it == enc } // all ups between src -> dcl
                val n = xups.count { it is Expr.Proto && it!=enc }
                val XX = "$X${"->exe->clo.Dyn->Clo_Task.up_tsk->X".repeat(n)}"
                val (_,idx) = this.idx(XX,dcl,ii,if (enc is Expr.Proto) enc.blk else dcl)
                //println(listOf(n,id,XX,idx,dcl,src))
                Pair("$XX->S /* nested */", idx)
            }
            Type.UPVAL -> {
                Pair("$X->S", "($X->clo + 1 + $X->args + $upv) /* upval $id */")
            }
             */
            else -> "ceu_upv_$id"
        }
    }
    fun idx (e: Expr, idc: String): String {
        return if (this.ismem(e)) "(ceu_mem->$idc)" else "ceu_$idc"
    }
    fun dcl (e: Expr, tp: String="CEU_Value"): String {
        return if (this.ismem(e)) "" else tp
    }

    init {
        outer.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                if (this.nst) {
                    if (ups.first(ups.pub[this]!!) { it is Expr.Proto }.let {
                        //(CEU>=99 && it==outer.clo) /* bc of top-level spawn {...} */ ||
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
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> {
                if (this.src is Expr.Proto && this.tk.str=="val") {
                    protos_use_unused.add(this.src)
                    protos_use_map[this.src] = mutableSetOf()
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
                //defer_catch_spawn_tasks.add(this.blk)
                //defer_catch_spawn_tasks.add(this.cnd)
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
                defer_catch_spawn_tasks.add(ups.first(this) { it is Expr.Do } as Expr.Do)
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
                    ups.first(this) { it is Expr.Proto }.let { it?.tk?.str=="func" }
                        -> err(this.tk, "yield error : unexpected enclosing func")
                    (ups.exe(this) == null)
                        -> err(this.tk, "yield error : expected enclosing coro" + (if (CEU <= 3) "" else " or task"))
                    //ups.any(this) { it is Expr.Do && it.arg!=null }
                    //    -> err(this.tk, "yield error : unexpected enclosing thus")
                }
                ups.all_until(this) { it is Expr.Proto }
                    .filter  { it is Expr.Do }              // all blocks up to proto
                    .forEach { ylds.add(it as Expr.Do) }
            }
            is Expr.Resume -> {
                this.co.traverse()
                this.args.forEach { it.traverse() }
            }

            is Expr.Spawn  -> {
                this.tsks?.traverse()
                this.tsk.traverse()
                this.args.forEach { it.traverse() }
                if (this.tsks == null) {
                    defer_catch_spawn_tasks.add(ups.first(this) { it is Expr.Do } as Expr.Do)
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
                    err(this.tk, "delay error : expected enclosing task")
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
                if (dcl.src is Expr.Proto && dcl.tk.str=="val") {
                    // f is accessed
                    //  - from an enclosing const g
                    //      - g calls f
                    //      - add f to g such that f is ok if g is ok
                    //  - elsewhere
                    //      - f is ok and all fs' accessed from f
                    val up_proto = ups.first(this) { it is Expr.Proto && ups.pub[it].let { it is Expr.Dcl && it.tk.str=="val" } }
                    when {
                        (up_proto == null) -> protos_use_f(dcl.src)
                        //!protos_use_unused.contains(up_proto) -> protos_use_f(dcl.src)
                        else -> protos_use_map[up_proto]!!.add(dcl.src)
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
                if (this.clo is Expr.Acc && this.clo.tk.str=="tasks") {
                    defer_catch_spawn_tasks.add(ups.first(this) { it is Expr.Do } as Expr.Do)
                }

            }
        }
    }
}
