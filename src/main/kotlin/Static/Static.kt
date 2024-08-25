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

    // Do or Proto that requires mem:
    //  - yield // nested coro/task // spawn/tasks (block needs mem)
    val mems: MutableSet<Expr>  = mutableSetOf()

    // void: block is innocuous -> should be a proxy to up block
    fun void (blk: Expr.Do): Boolean {
        // no declarations, no spawns, no tasks
        val dcls = blk.to_dcls()
        //println(listOf("-=-=-", blk.tk, ups.pub[blk]?.javaClass?.name))
        //println(blk.tostr())
        return when {
            true -> false
            ismem(blk,true) -> false
            (blk.tag != null) -> false
            !dcls.isEmpty() -> false
            (ups.pub[blk] is Expr.Proto) -> false
            this.defer_catch_spawn_tasks.contains(blk) -> false
            else -> true
        }
    }
    val defer_catch_spawn_tasks: MutableSet<Expr.Do> = mutableSetOf()

    fun ismem (e: Expr, out: Boolean=false): Boolean {
        val proto = ups.first(e) { it is Expr.Proto }.let {
            when {
                (it == null) -> null
                (it.tk.str == "func") -> null
                else -> it
            }
        }
        val up = ups.first(e) { it is Expr.Do || it is Expr.Proto }!!
        return when {
            (!out && proto==null) -> false
            //true -> true
            mems.contains(up) -> true
            else -> false
        }
    }

    fun idx (acc: Expr.Acc): String {
        val dcl = ups.id_to_dcl(acc.tk.str,acc)!!
        return this.idx(dcl, acc)
    }
    fun idx (dcl: Expr.Dcl, src: Expr): String {
        val id = dcl.idtag.first.str.idc()
        val blk = ups.dcl_to_blk(dcl)
        val ismem = this.ismem(blk)
        //println(listOf(src.tk.pos.lin, id, type(dcl,src)))
        return when (vars.type(dcl,src)) {
            Type.GLOBAL -> "ceu_glb_$id"
            Type.LOCAL -> if (ismem) "(ceu_mem->${id}_${dcl.n})" else "ceu_loc_${id}_${dcl.n}"  // idx b/c of "it"
            Type.NESTED -> {
                val xups = ups.all_until(src) { it == blk } // all ups between src -> dcl
                val pid = (ups.first(blk) { it is Expr.Proto } as Expr.Proto).id(outer, ups)
                val xn = xups.count { it is Expr.Proto && it!=blk }
                "((CEU_Pro_$pid*)ceux->exe_task->${"lnks.up.tsk->".repeat(xn)}mem)->${id}_${dcl.n}"
            }
            else -> "ceu_upv_${id}_${dcl.n}"
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
                    when {
                        (ups.first(ups.pub[this]!!) { it is Expr.Do } == outer) -> {}
                        (ups.first(ups.pub[this]!!) { it is Expr.Proto } == null) -> err(this.tk, ":nested error : expected enclosing prototype")
                        else -> {}
                    }
                    ups.all_until(ups.pub[this]!!) { it is Expr.Proto }
                        .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                        .forEach { mems.add(it) }

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
            is Expr.Escape -> {
                val fst = ups.first(this) { (it is Expr.Do && it.tag?.str==this.tag.str) || (it is Expr.Proto && !it.nst)}
                if (fst !is Expr.Do) {
                    err(this.tk, "escape error : expected matching \"do\" block")
                }
                this.e?.traverse()
            }
            is Expr.Group  -> {
                val up = ups.pub[this]!!
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
                    val dcl = ups.id_to_dcl(this.dst.tk.str,this.dst)!!
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
                this.e.traverse()
                when {
                    ups.any(this) { defer -> (defer is Expr.Defer) }
                        -> err(this.tk, "yield error : unexpected enclosing defer")
                    ups.first(this) { it is Expr.Proto }.let { it?.tk?.str=="func" }
                        -> err(this.tk, "yield error : unexpected enclosing func")
                    (ups.exe(this) == null)
                        -> err(this.tk, "yield error : expected enclosing coro" + (if (CEU <= 3) "" else " or task"))
                    //ups.any(this) { it is Expr.Do && it.arg!=null }
                    //    -> err(this.tk, "yield error : unexpected enclosing thus")
                }
                ups.all_until(this) { it is Expr.Proto }
                    .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                    .forEach { mems.add(it) }
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

                    // tasks is the one relevant, not the spawn itself
                    ups.all_until(this) { it is Expr.Proto }
                        .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                        .forEach { mems.add(it) }
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
            is Expr.Tasks  -> {
                this.max.traverse()
                ups.all_until(this) { it is Expr.Proto }
                    .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                    .forEach { mems.add(it) }
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val dcl = ups.id_to_dcl(this.tk.str,this)!!
                if (dcl.src is Expr.Proto && (dcl.tk.str=="val" || dcl.tk.str=="val'")) {
                    // f is accessed
                    //  - from an enclosing const g
                    //      - g calls f
                    //      - add f to g such that f is ok if g is ok
                    //  - elsewhere
                    //      - f is ok and all fs' accessed from f
                    val up_proto = ups.first(this) { it is Expr.Proto && ups.pub[it].let { it is Expr.Dcl && (it.tk.str=="val" || it.tk.str=="val'") } }
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
