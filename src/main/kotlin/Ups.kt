// func (args) or block (locals)
data class XBlock (val syms: MutableMap<String,Dcl>, val defers: MutableList<String>?)
data class Dcl (val id: String, val init: Boolean, val upv: Int, val blk: Expr.Do)    // blk = [Block,Group,Proto]

class Ups (val outer: Expr.Do) {
    val xblocks = mutableMapOf<Expr,XBlock> (
        Pair (
            outer,
            XBlock(GLOBALS.map { Pair(it,Dcl(it,true,0,outer)) }.toMap().toMutableMap(), mutableListOf())
        )
    )
    val ups = outer.tree()

    // Protos that cannot be closures:
    //  - they access at least 1 free var w/o upval modifiers
    //  - for each var access ACC, we get its declaration DCL in block BLK
    //      - if ACC/DCL have no upval modifiers
    //      - we check if there's a func FUNC in between ACC -> [FUNC] -> BLK
    val upvs_protos_noclos = mutableSetOf<Expr>()

    // Upvars (var ^up) with refs (^^up):
    //  - at least one refs access the var
    //  - for each var access ACC, we get its declaration DCL and set here
    //  - in another round (Code), we assert that the DCL appears here
    //  - TODO: can also be used to warn for unused normal vars
    val upvs_vars_refs = mutableSetOf<Dcl>()

    // Set of uprefs within protos:
    //  - for each ^^ACC, we get the enclosing PROTOS and add ACC.ID to them
    val upvs_protos_refs = mutableMapOf<Expr.Proto,MutableSet<String>>()

    init {
        this.outer.traverse()
    }

    fun all_until (e: Expr, cnd: (Expr)->Boolean): List<Expr> {
        val up = ups[e]
        return when {
            cnd(e) -> listOf(e)
            (up == null) -> emptyList()
            else -> this.all_until(up,cnd).let { if (it.isEmpty()) it else it+e }
        }
    }
    fun hasfirst (e: Expr, cnd: (Expr)->Boolean): Boolean {
        return this.first(e,cnd) != null
    }
    fun first (e: Expr, cnd: (Expr)->Boolean): Expr? {
        return this.all_until(e,cnd).firstOrNull()
    }
    fun first_block (e: Expr): Expr.Do? {
        return this.first(e) { it is Expr.Do && it.isnest && it.ishide } as Expr.Do?
    }
    fun first_proto_or_block (e: Expr): Expr? {
        return this.first(e) { it is Expr.Proto || (it is Expr.Do && it.isnest && it.ishide) }
    }
    fun intask (e: Expr): Boolean {
        return (this.first(e) { it is Expr.Proto }?.tk?.str == "task")
    }

    fun getDcl (e: Expr, id: String): Dcl? {
        val up = this.ups[e]
        val dcl = this.xblocks[e]?.syms?.get(id)
        return when {
            (dcl != null) -> dcl
            (up == null) -> null
            else -> this.getDcl(up, id)
        }
    }
    fun assertIsNotDeclared (e: Expr, id: String, tk: Tk) {
        if (this.getDcl(e,id) != null) {
            err(tk, "declaration error : variable \"$id\" is already declared")
        }
    }
    fun assertIsDeclared (e: Expr, v: Pair<String,Int>, tk: Tk): Dcl {
        val (id,upv) = v
        val dcl = this.getDcl(e,id)
        val nocross = dcl?.blk.let { blk ->
            (blk == null) || this.all_until(e) { it==blk }.none { it is Expr.Proto }
        }
        return when {
            (dcl == null) -> err(tk, "access error : variable \"${id}\" is not declared") as Dcl
            (dcl.upv==0 && upv>0 || dcl.upv==1 && upv==0) -> err(tk, "access error : incompatible upval modifier") as Dcl
            (upv==2 && nocross) -> err(tk, "access error : unnecessary upref modifier") as Dcl
            else -> dcl
        }
    }

    // Traverse the tree structure from top down
    // 1. assigns this.xblocks
    // 2. assigns this.upvs_protos_noclos, upvs_vars_refs, upvs_protos_refs
    // 3. compiles all proto uprefs
    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> this.body.traverse()
            is Expr.Do -> {
                if (this!=outer && this.ishide) {
                    val proto = ups[this]
                    val args = if (proto !is Expr.Proto) {
                        mutableMapOf()
                    } else {
                        proto.args.let {
                            (it.map {
                                Pair(it.str, Dcl(it.str, true, it.upv, this))
                            } + it.map {
                                Pair("_${it.str}_", Dcl("_${it.str}_", false, it.upv, this))
                            })
                        }.toMap().toMutableMap()
                    }
                    xblocks[this] = XBlock(args, mutableListOf())
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl -> {
                this.src?.traverse()
                val id = this.tk_.fromOp().noSpecial()
                val bup = first(this) { it is Expr.Do && it.ishide }!! as Expr.Do
                val xup = xblocks[bup]!!
                assertIsNotDeclared(this, id, this.tk)
                xup.syms[id] = Dcl(id, this.init, this.tk_.upv, bup)
                xup.syms["_${id}_"] = Dcl("_${id}_", false, this.tk_.upv, bup)
                when {
                    (this.tk_.upv == 2) -> {
                        err(tk, "var error : cannot declare an upref")
                    }
                    (this.tk_.upv==1 && bup==outer) -> {
                        err(tk, "var error : cannot declare a global upvar")
                    }
                }
            }
            is Expr.Set    -> { this.dst.traverse() ; this.src.traverse() }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.While  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()

            is Expr.Spawn  -> { this.call.traverse() ; this.coros?.traverse() }
            is Expr.Bcast  -> this.evt.traverse()
            is Expr.Yield  -> {
                if (!intask(this)) {
                    err(this.tk, "yield error : expected enclosing task")
                }
                this.arg.traverse()
            }
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.coro.traverse() ; this.on.traverse() }
            is Expr.Pub    -> {
                if (this.coro is Expr.Task) {
                    val ok = hasfirst(this) { it is Expr.Proto && it.task!=null && !it.task.first }
                    if (!ok) {
                        err(this.tk, "${this.tk.str} error : expected enclosing task")
                    }
                }
                this.coro.traverse()
            }
            is Expr.Task   -> {}

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val dcl = getDcl(this, this.tk.str)
                when {
                    (dcl == null) -> {}
                    (dcl.upv==1 && this.tk_.upv==2) -> {
                        upvs_vars_refs.add(dcl) // UPVS_VARS_REFS

                        // UPVS_PROTOS_REFS
                        all_until(this) { dcl.blk==it }
                            .filter { it is Expr.Proto }
                            .let { it as List<Expr.Proto> }
                            .forEach { proto ->
                                val set = upvs_protos_refs[proto] ?: mutableSetOf()
                                set.add(this.tk.str)
                                if (upvs_protos_refs[proto] == null) {
                                    upvs_protos_refs[proto] = set
                                }
                            }
                    }
                    // UPVS_PROTOS_NOCLOS
                    (dcl.blk!=outer && dcl.upv==0 && this.tk_.upv==0) -> {
                        // access to normal noglb w/o upval modifier
                        all_until(this) { it == dcl.blk }       // stop at enclosing declaration block
                            .filter { it is Expr.Proto }            // all crossing protos
                            .forEach { upvs_protos_noclos.add(it) }        // mark them as noclos
                    }
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
            is Expr.Index  -> { this.col.traverse() ; this.idx.traverse() }
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }

    // builds the tree structure from bottom up
    fun Expr.tree (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.tree() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
        }
        return when (this) {
            is Expr.Proto  -> this.map(listOf(this.body))
            is Expr.Do     -> this.map(this.es)
            is Expr.Dcl    -> this.map(listOfNotNull(this.src))
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.While  -> this.map(listOf(this.cnd, this.body))
            is Expr.Catch  -> this.map(listOf(this.cnd, this.body))
            is Expr.Defer  -> this.map(listOf(this.body))

            is Expr.Spawn  -> this.map(listOf(this.call) + listOfNotNull(this.coros))
            is Expr.Bcast  -> this.map(listOf(this.evt, this.xin))
            is Expr.Yield  -> this.map(listOf(this.arg))
            is Expr.Resume -> this.map(listOf(this.call))
            is Expr.Toggle -> this.map(listOf(this.coro, this.on))
            is Expr.Pub    -> this.map(listOf(this.coro))
            is Expr.Task   -> emptyMap()

            is Expr.Nat    -> emptyMap()
            is Expr.Acc    -> emptyMap()
            is Expr.EvtErr -> emptyMap()
            is Expr.Nil    -> emptyMap()
            is Expr.Tag    -> emptyMap()
            is Expr.Bool   -> emptyMap()
            is Expr.Char   -> emptyMap()
            is Expr.Num    -> emptyMap()
            is Expr.Tuple  -> this.map(this.args)
            is Expr.Vector -> this.map(this.args)
            is Expr.Dict   -> this.map(this.args.map { listOf(it.first,it.second) }.flatten())
            is Expr.Index  -> this.map(listOf(this.col, this.idx))
            is Expr.Call   -> this.map(listOf(this.proto)) + this.map(this.args)
        }
    }
}
