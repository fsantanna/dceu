package dceu

fun check_tags () {
    for ((id,tk) in G.tags) {
        val issub = id.contains('.')
        //println(listOf(id,issub))
        val sup = id.dropLastWhile { it != '.' }.dropLast(1)
        if (issub && !G.tags.containsKey(sup)) {
            err(tk, "tag error : parent tag $sup is not declared")
        }
    }
}
fun check_vars () {
    fun check (dcl: Expr.Dcl) {
        if (CEU>=50 && dcl.idtag.first.str=="it") {
            // ok
        } else {
            val xdcl = dcl.id_to_dcl(dcl.idtag.first.str, false, { it.n==dcl.n })
            if (xdcl === null) {
                // ok
            } else {
                err(dcl.tk, "declaration error : variable \"${dcl.idtag.first.str}\" is already declared")
            }
        }
    }

    fun acc (e: Expr, id: String): Expr.Dcl {
        val dcl: Expr.Dcl? = e.id_to_dcl(id)
        if (dcl === null) {
            err(e.tk, "access error : variable \"${id}\" is not declared")
        }

        // add upval to all protos upwards
        // stop at declaration (orig)
        // use blk bc of args
        if (type(dcl,e) == Scope.UPVAL) {
            if (dcl.tk.str!="val" && dcl.tk.str!="val'") {
                err(e.tk, "access error : outer variable \"${dcl.idtag.first.str}\" must be immutable")
            }
        }

        return dcl
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                if (this.tag !==null && !G.datas.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
                this.pars.forEach { check(it) }
                this.blk.traverse()
            }
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Group -> this.es.forEach { it.traverse() }
            is Expr.Enclose -> this.es.forEach { it.traverse() }
            is Expr.Escape -> this.e?.traverse()
            is Expr.Dcl    -> {
                this.src?.traverse()
                check(this)
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.blk.traverse()
            is Expr.Data   -> {
                val sup = this.tk.str.dropLastWhile { it != '.' }.dropLast(1)
                if (G.datas.containsKey(this.tk.str)) {
                    err(this.tk, "data error : data ${this.tk.str} is already declared")
                }
                val ids = (G.datas[sup] ?: emptyList()) + this.ids
                val xids = ids.map { it.first.str }
                if (xids.size != xids.distinct().size) {
                    err(this.tk, "data error : found duplicate ids")
                }
                ids.forEach { (_,tag) ->
                    if (tag!==null && !G.datas.containsKey(tag.str)) {
                        err(tag, "data error : data ${tag.str} is not declared")
                    }
                }
                G.datas[this.tk.str] = ids
            }
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> this.blk.traverse()
            is Expr.Defer  -> this.blk.traverse()

            is Expr.Yield  -> this.e.traverse()
            is Expr.Resume -> { this.co.traverse() ; this.args.forEach { it.traverse() } }

            is Expr.Spawn  -> { this.tsks?.traverse() ; this.tsk.traverse() ; this.args.forEach { it.traverse() } }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }
            is Expr.Tasks  -> this.max.traverse()

            is Expr.Nat    -> {
                G.nats[this.n] = this.tk.str.let {
                    assert(!it.contains("XXX")) { "TODO: native cannot contain XXX"}
                    val set = mutableListOf<NExpr>()
                    var str = ""
                    var i = 0

                    var lin = 1
                    var col = 1
                    fun read (): Char {
                        //assert(i < it.length) { "bug found" }
                        if (i >= it.length) {
                            err(tk, "native error : (lin $lin, col $col) : unterminated token")
                        }
                        val x = it[i++]
                        if (x == '\n') {
                            lin++; col=0
                        } else {
                            col++
                        }
                        return x
                    }

                    while (i < it.length) {
                        val x1 = read()
                        str += if (x1 != '$') x1 else {
                            val (l,c) = Pair(lin,col)
                            var id = ""
                            var no = ""
                            while (i < it.length) {
                                val x2 = read()
                                if (x2.isLetterOrDigit() || x2=='_' || x2=='-') {
                                    id += x2
                                } else {
                                    no += x2
                                    break
                                }
                            }
                            if (id.length == 0) {
                                err(tk, "native error : (lin $l, col $c) : invalid identifier")
                            }
                            val dcl = acc(this, id)
                            set.add(dcl.n)
                            "(XXX)$no"
                        }
                    }
                    Pair(set, str)
                }
            }
            is Expr.Acc    -> acc(this, this.tk.str)
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach { it.traverse() }
            is Expr.Vector -> this.args.forEach { it.traverse() }
            is Expr.Dict   -> this.args.forEach { it.first.traverse() ; it.second.traverse() }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()
                this.data()
            }
            is Expr.Call   -> { this.clo.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
    G.outer!!.traverse()
}

fun check_statics () {
    G.outer!!.dn_visit { me ->
        when (me) {
            is Expr.Proto  -> {
                if (me.nst) {
                    when {
                        (me.tk.str != "func'") -> {
                            // OK: nested coro/task always ok b/c of ceux/MEM
                        }
                        (G.nonlocs[me.n]!!.isEmpty()) -> {
                            // OK: no access to outer - unset :nested
                        }
                        me.up_any { it is Expr.Proto && it.tk.str != "func'" } -> {
                            val proto = me.up_first { it is Expr.Proto && it.tk.str != "func'" }!!
                            err(me.tk, "func :nested error : unexpected enclosing ${proto.tk.str}")
                        }
                    }
                }
            }
            is Expr.Escape -> {
                val fst = me.up_first { (it is Expr.Enclose && it.tag.str==me.tag.str) || (it is Expr.Proto && !it.nst)}
                if (fst !is Expr.Enclose) {
                    err(me.tk, "escape error : expected matching enclosing block")
                }
            }
            is Expr.Group  -> {
                val up = me.fupx()
                val ok = (up is Expr.Do) || (up is Expr.Group) || (up is Expr.Dcl) || (up is Expr.Set && up.src.n==me.n)
                if (!ok) {
                    err(me.tk, "group error : unexpected context")
                }
            }
            is Expr.Set    -> {
                if (me.dst is Expr.Acc) {
                    val dcl = me.dst.id_to_dcl(me.dst.tk.str)!!
                    if (dcl.tk.str=="val" || dcl.tk.str=="val'") {
                        err(me.tk, "set error : destination is immutable")
                    }
                }
            }
            is Expr.Defer  -> {
                val f = me.up_first { it is Expr.Proto && it.tk.str=="func'" }
                if (f !== null) {
                    val co = f.up_any { it is Expr.Proto && it.tk.str!="func'" }
                    if (co) {
                        err(me.tk, "defer error : unexpected func with enclosing coro or task")
                    }
                }
            }
            is Expr.Yield  -> {
                when {
                    me.up_any { defer -> (defer is Expr.Defer) }
                        -> err(me.tk, "yield error : unexpected enclosing defer")
                    me.up_first { it is Expr.Proto }.let { it?.tk?.str=="func'" }
                        -> err(me.tk, "yield error : unexpected enclosing func")
                    (me.up_exe() === null)
                        -> err(me.tk, "yield error : expected enclosing coro" + (if (CEU <= 3) "" else " or task"))
                }
            }
            is Expr.Delay  -> {
                if (!me.up_first { it is Expr.Proto }.let { it?.tk?.str=="task'" }) {
                    err(me.tk, "delay error : expected enclosing task")
                }
            }
            is Expr.Pub    -> {
                if (me.tsk === null) {
                    val outer = me.up_first_task_outer()
                    val ok = (outer !== null) && me.up_all_until { it == outer }.none { it is Expr.Proto && it.tk.str!="task'" }
                    if (!ok) {
                        err(me.tk, "pub error : expected enclosing task")
                    }
                    //(ups.first_task_outer(this) === null) -> err(this.tk, "pub error : expected enclosing task")
                }
            }
            else -> {}
        }
    }
}

class Static () {
    // protos_unused: const proto is not used: do not generate code
    /*
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
     */

    /*
    // void: block is innocuous -> should be a proxy to up block
    fun void (blk: Expr.Do): Boolean {
        // no declarations, no spawns, no tasks
        val dcls = blk.to_dcls()
        //println(listOf("-=-=-", blk.tk, G.ups[blk]?.javaClass?.name))
        //println(blk.tostr())
        return when {
            true -> false
            blk.is_mem(true) -> false
            (blk.tag !== null) -> false
            !dcls.isEmpty() -> false
            (G.ups[blk] is Expr.Proto) -> false
            this.defer_catch_spawn_tasks.contains(blk) -> false
            else -> true
        }
    }
    val defer_catch_spawn_tasks: MutableSet<Expr.Do> = mutableSetOf()
     */

    init {
        G.outer!!.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                if (this.nst) {
                    this.fupx().up_all_until { it is Expr.Proto }
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
            is Expr.Group  -> this.es.forEach { it.traverse() }
            is Expr.Enclose -> this.es.forEach { it.traverse() }
            is Expr.Escape -> this.e?.traverse()
            is Expr.Dcl    -> {
                if (this.src is Expr.Proto && (this.tk.str=="val" || this.tk.str=="val'")) {
                    //protos_use_unused.add(this.src)
                    //protos_use_map[this.src] = mutableSetOf()
                }
                this.src?.traverse()
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.blk.traverse()
            is Expr.Data   -> {}
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> this.blk.traverse()
            is Expr.Defer  -> {
                //defer_catch_spawn_tasks.add(this.up_first { it is Expr.Do } as Expr.Do)
                this.blk.traverse()
            }

            is Expr.Yield  -> {
                this.e.traverse()
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
                if (this.tsks === null) {
                    //defer_catch_spawn_tasks.add(this.up_first { it is Expr.Do } as Expr.Do)

                    // tasks is the one relevant, not the spawn itself
                    this.up_all_until { it is Expr.Proto }
                        .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                        .forEach { G.mems.add(it) }
                }
                /*
                when {
                    (ups.first(this) { f -> ((f is Expr.Proto) && f.tk.str == "func") } !== null)
                       -> err(this.tk, "spawn error : unexpected enclosing func")
                }
                 */
            }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }
            is Expr.Tasks  -> {
                this.max.traverse()
                this.up_all_until { it is Expr.Proto }
                    .filter  { it is Expr.Do || it is Expr.Proto }              // all blocks up to proto
                    .forEach { G.mems.add(it) }
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                /*
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
                        (up_proto === null) -> protos_use_f(dcl.src)
                        //!protos_use_unused.contains(up_proto) -> protos_use_f(dcl.src)
                        else -> protos_use_map[up_proto]!!.add(dcl.src)
                    }
                }
                 */
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
                    //defer_catch_spawn_tasks.add(this.up_first { it is Expr.Do } as Expr.Do)
                }

            }
        }
    }
}
