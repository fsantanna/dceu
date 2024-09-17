package dceu

class Static () {
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

    // void: block is innocuous -> should be a proxy to up block
    fun void (blk: Expr.Do): Boolean {
        // no declarations, no spawns, no tasks
        val dcls = blk.to_dcls()
        //println(listOf("-=-=-", blk.tk, G.ups[blk]?.javaClass?.name))
        //println(blk.tostr())
        return when {
            true -> false
            ismem(blk,true) -> false
            (blk.tag != null) -> false
            !dcls.isEmpty() -> false
            (G.ups[blk] is Expr.Proto) -> false
            this.defer_catch_spawn_tasks.contains(blk) -> false
            else -> true
        }
    }
    val defer_catch_spawn_tasks: MutableSet<Expr.Do> = mutableSetOf()

    fun idx (acc: Expr.Acc): String {
        val dcl = acc.id_to_dcl(acc.tk.str)!!
        return this.idx(dcl, acc)
    }
    fun idx (dcl: Expr.Dcl, src: Expr): String {
        val id = dcl.idtag.first.str.idc()
        val blk = dcl.to_blk()
        val ismem = ismem(blk)
        //println(listOf(src.tk.pos.lin, id, type(dcl,src)))

        return when (type(dcl,src)) {
            Type.GLOBAL -> "ceu_glb_$id"
            Type.LOCAL -> if (ismem) "(ceu_mem->${id}_${dcl.n})" else "ceu_loc_${id}_${dcl.n}"  // idx b/c of "it"
            Type.NESTED -> {
                val xups = src.up_all_until { it == blk } // all ups between src -> dcl
                val pid = (blk.up_first { it is Expr.Proto } as Expr.Proto).id(G.outer!!)
                val xn = xups.count { it is Expr.Proto && it!=blk }
                "((CEU_Pro_$pid*)ceux->exe_task->${"clo->up_nst->".repeat(xn)}mem)->${id}_${dcl.n}"
            }
            else -> {
                val proto = src.up_first { it is Expr.Proto } as Expr.Proto
                val i = G.proto_to_upvs[proto]!!.indexOfFirst { it == dcl }
                assert(i != -1)
                "ceux->clo->upvs.buf[$i]"
            }
        }
    }
    fun idx (e: Expr, idc: String): String {
        return if (ismem(e)) "(ceu_mem->$idc)" else "ceu_$idc"
    }
    fun dcl (e: Expr, tp: String="CEU_Value"): String {
        return if (ismem(e)) "" else tp
    }

    init {
        G.outer!!.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                if (this.nst) {
                    when {
                        (this.tk.str != "func") -> {
                            // OK: nested coro/task always ok b/c of ceux/MEM
                        }
                        (!G.proto_has_outer.contains(this)) -> {
                            // OK: no access to outer - unset :nested
                        }
                        this.up_any { it is Expr.Proto && it.tk.str!="func" } -> {
                            val proto = this.up_first { it is Expr.Proto && it.tk.str!="func" }!!
                            err(this.tk, "func :nested error : unexpected enclosing ${proto.tk.str}")
                        }
                    }

                    G.ups[this]!!.up_all_until { it is Expr.Proto }
                        .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                        .forEach { G.mems.add(it) }

                    /*
                    val up1 = G.ups[this]
                    val up2 = if (up1==null) null else G.ups[up1]
                    when {
                        (up1 !is Expr.Spawn) -> err(this.tk, "task :nested error : expected enclosing spawn")
                        (up2 !is Expr.Do) -> err(up1.tk, "spawn task :nested error : expected immediate enclosing block")
                        (G.ups[up2] == outer.clo) -> {}    // OK: outer spawn
                        (up2.es.last() == up1) -> err(up1.tk, "spawn task :nested error : cannot escape enclosing block")
                    }
                     */
                }
                this.blk.traverse()
            }
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Escape -> {
                val fst = this.up_first { (it is Expr.Do && it.tag?.str==this.tag.str) || (it is Expr.Proto && !it.nst)}
                if (fst !is Expr.Do) {
                    err(this.tk, "escape error : expected matching \"do\" block")
                }
                this.e?.traverse()
            }
            is Expr.Group  -> {
                val up = G.ups[this]!!
                val ok = (up is Expr.Do) || (up is Expr.Group) || (up is Expr.Dcl) || (up is Expr.Set && up.src==this)
                if (!ok) {
                    err(this.tk, "group error : unexpected context")
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl    -> {
                if (this.src is Expr.Proto && (this.tk.str=="val" || this.tk.str=="val'")) {
                    protos_use_unused.add(this.src)
                    protos_use_map[this.src] = mutableSetOf()
                }
                this.src?.traverse()
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
                if (this.dst is Expr.Acc) {
                    val dcl = this.dst.id_to_dcl(this.dst.tk.str)!!
                    if (dcl.tk.str=="val" || dcl.tk.str=="val'") {
                        err(this.tk, "set error : destination is immutable")
                    }
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.blk.traverse()
            is Expr.Data   -> {}
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> this.blk.traverse()
            is Expr.Defer  -> {
                val f = this.up_first { it is Expr.Proto && it.tk.str=="func" }
                if (f != null) {
                    val co = f.up_any { it is Expr.Proto && it.tk.str!="func" }
                    if (co) {
                        err(this.tk, "defer error : unexpected func with enclosing coro or task")
                    }
                }
                defer_catch_spawn_tasks.add(this.up_first { it is Expr.Do } as Expr.Do)
                this.blk.traverse()
            }

            is Expr.Yield  -> {
                this.e.traverse()
                when {
                    this.up_any { defer -> (defer is Expr.Defer) }
                        -> err(this.tk, "yield error : unexpected enclosing defer")
                    this.up_first { it is Expr.Proto }.let { it?.tk?.str=="func" }
                        -> err(this.tk, "yield error : unexpected enclosing func")
                    (this.up_exe() == null)
                        -> err(this.tk, "yield error : expected enclosing coro" + (if (CEU <= 3) "" else " or task"))
                    //ups.any(this) { it is Expr.Do && it.arg!=null }
                    //    -> err(this.tk, "yield error : unexpected enclosing thus")
                }
                this.up_all_until { it is Expr.Proto }
                    .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                    .forEach { G.mems.add(it) }
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
                    defer_catch_spawn_tasks.add(this.up_first { it is Expr.Do } as Expr.Do)

                    // tasks is the one relevant, not the spawn itself
                    this.up_all_until { it is Expr.Proto }
                        .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                        .forEach { G.mems.add(it) }
                }
                /*
                when {
                    (ups.first(this) { f -> ((f is Expr.Proto) && f.tk.str == "func") } != null)
                       -> err(this.tk, "spawn error : unexpected enclosing func")
                }
                 */
            }
            is Expr.Delay  -> {
                if (!this.up_first { it is Expr.Proto }.let { it?.tk?.str=="task" }) {
                    err(this.tk, "delay error : expected enclosing task")
                }
            }
            is Expr.Pub    -> {
                if (this.tsk == null) {
                    val outer = this.up_first_task_outer()
                    val ok = (outer != null) && this.up_all_until { it == outer }.none { it is Expr.Proto && it.tk.str!="task" }
                    if (!ok) {
                        err(this.tk, "pub error : expected enclosing task")
                    }
                    //(ups.first_task_outer(this) == null) -> err(this.tk, "pub error : expected enclosing task")
                } else {
                    this.tsk.traverse()
                }
            }
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }
            is Expr.Tasks  -> {
                this.max.traverse()
                this.up_all_until { it is Expr.Proto }
                    .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                    .forEach { G.mems.add(it) }
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val dcl = this.id_to_dcl(this.tk.str)!!
                if (dcl.src is Expr.Proto && (dcl.tk.str=="val" || dcl.tk.str=="val'")) {
                    // f is accessed
                    //  - from an enclosing const g
                    //      - g calls f
                    //      - add f to g such that f is ok if g is ok
                    //  - elsewhere
                    //      - f is ok and all fs' accessed from f
                    val up_proto = this.up_first { it is Expr.Proto && G.ups[it].let { it is Expr.Dcl && (it.tk.str=="val" || it.tk.str=="val'") } }
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
                    defer_catch_spawn_tasks.add(this.up_first { it is Expr.Do } as Expr.Do)
                }

            }
        }
    }
}
