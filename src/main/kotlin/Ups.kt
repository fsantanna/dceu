// func (args) or block (locals)
data class XBlock (val syms: MutableMap<String, Int>, val defers: MutableList<String>?)

class Ups (val outer: Expr.Block) {
    val xblocks = mutableMapOf<Expr,XBlock> (
        Pair (
            outer,
            XBlock(GLOBALS.map { Pair(it,0) }.toMap().toMutableMap(), mutableListOf())
        )
    )
    val ups = outer.tree()

    // Protos that cannot be closures:
    //  - they access at least 1 free var w/o upval modifiers
    //  - for each var access ACC, we get its declaration DCL in block BLK
    //      - if ACC/DCL have no upval modifiers
    //      - we check if there's a func FUNC in between ACC -> [FUNC] -> BLK
    val noclos = mutableSetOf<Expr>()

    init {
        this.outer.traverse()
    }

    fun pred (e: Expr, f: (Expr)->Boolean): Expr? {
        return this.pred_stop(e, f) { false }
    }
    fun pred_stop (e: Expr, f: (Expr)->Boolean, g: (Expr)->Boolean): Expr? {
        val x = ups[e]
        return when {
            (x == null) -> null
            f(x) -> x
            g(x) -> null
            else -> this.pred(x, f)
        }
    }
    fun block (e: Expr): Expr.Block? {
        return this.pred(e) { it is Expr.Block } as Expr.Block?
    }
    fun block_or_group (e: Expr): Expr? {
        return this.pred(e) { it is Expr.Block || (it is Expr.Group && it.isHide) }
    }
    fun func_or_task (e: Expr): Expr.Proto? {
        return this.pred(e) { it is Expr.Proto } as Expr.Proto?
    }
    fun task (e: Expr): Expr.Proto? {
        return this.pred(e) { it is Expr.Proto && it.tk.str=="task" } as Expr.Proto?
    }
    fun proto_or_block (e: Expr): Expr? {
        return this.pred(e) { it is Expr.Proto || it is Expr.Block }
    }
    fun proto_or_block_or_group (e: Expr): Expr? {
        return this.pred(e) { it is Expr.Proto || it is Expr.Block || (it is Expr.Group && it.isHide) }
    }
    fun intask (e: Expr): Boolean {
        return (this.func_or_task(e)?.tk?.str == "task")
    }

    fun getDeclared (e: Expr, id: String): Pair<Expr,Int>? {
        val up = this.ups[e]
        val dcl = this.xblocks[e].let { if (it == null) null else it.syms[id] }
        return when {
            (dcl != null) -> Pair(e,dcl)
            (up == null) -> null
            else -> this.getDeclared(up, id)
        }
    }
    fun isDeclared (e: Expr, id: String): Boolean {
        return (this.getDeclared(e,id) != null)
    }
    fun assertIsNotDeclared (e: Expr, id: String, tk: Tk) {
        if (this.getDeclared(e,id) != null) {
            err(tk, "declaration error : variable \"$id\" is already declared")
        }
    }
    fun assertIsDeclared (e: Expr, v: Pair<String,Int>, tk: Tk): Int {
        val (id,upv) = v
        val dcl = this.getDeclared(e,id)
        val nocross = dcl?.first.let { blk ->
            (blk == null) || (blk == e) || (this.pred(e) { up -> up==blk || up is Expr.Proto } == blk)
        }
        return when {
            (dcl == null) -> err(tk, "access error : variable \"${id}\" is not declared") as Int
            (dcl.second==0 && upv>0 || dcl.second==1 && upv==0) -> err(tk, "access error : incompatible upval modifier") as Int
            (upv==2 && nocross) -> err(tk, "access error : unnecessary upref modifier") as Int
            else -> dcl.second
        }
    }

    /*
    fun isUp (e: Expr, id: String): Boolean {
        val xblock = this.xblocks[e]!!
        val up = this.proto_or_block_or_group(e)
        //println(listOf("GET", id, e.javaClass.name, xblock.syms.contains(id)))
        return (xblock.syms.contains(id) || (up!=null && this.isDeclared(up,id)))
    }
    fun assertIsNotUp (e: Expr, id: String, tk: Tk) {
        if (this.isUp(e,id)) {
            err(tk, "set error : cannot reassign an upvalue")
        }
    }
     */

    // Traverse the tree structure from top down
    // 1. assigns this.xblocks
    // 2. assigns this.noclos
    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> {
                xblocks[this] = XBlock (
                    this.args.let {
                        (it.map { Pair(it.str,it.upv) } + it.map { Pair("_${it.str}_",it.upv) })
                    }.toMap().toMutableMap(),
                    null
                )
                this.body.traverse()
            }
            is Expr.Block -> {
                if (this != outer) {
                    xblocks[this] = XBlock(mutableMapOf(), mutableListOf())
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Group -> {
                if (this.isHide) {
                    xblocks[this] = XBlock(mutableMapOf(), mutableListOf())
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                val bup = block_or_group(this)!!
                //println(listOf("DCL", id, bup.javaClass.name))
                val xup = xblocks[bup]!!
                assertIsNotDeclared(bup, id, this.tk)
                xup.syms[id] = this.tk_.upv
                xup.syms["_${id}_"] = this.tk_.upv
                //println(listOf(this.tk_.ups, block(bup)))
                when {
                    (this.tk_.upv == 2) -> {
                        err(tk, "var error : cannot declare an upref")
                    }
                    (this.tk_.upv==1 && block(bup)==null) -> {
                        err(tk, "var error : cannot declare a global upvar")
                    }
                }
            }
            is Expr.Set    -> { this.dst.traverse() ; this.src.traverse() }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.While  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Throw  -> this.ex.traverse()
            is Expr.Defer  -> this.body.traverse()

            is Expr.Coros  -> this.max?.traverse()
            is Expr.Coro   -> this.task.traverse()
            is Expr.Spawn  -> { this.call.traverse() ; this.coros?.traverse() }
            is Expr.CsIter -> { this.coros.traverse() ; this.body.traverse() }
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
                if (this.coro == null) {
                    var ok = false
                    var up = func_or_task(this)
                    while (up != null) {
                        if (up.tk.str=="task" && !up.task!!.first) {
                            ok = true
                            break
                        }
                        up = func_or_task(up)
                    }
                    if (!ok) {
                        err(this.tk, "${this.tk.str} error : expected enclosing task")
                    }
                }
                this.coro?.traverse()
            }
            is Expr.Track  -> this.coro.traverse()

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val dcl = getDeclared(this, this.tk.str)
                if (this.tk_.upv==0 && dcl?.second==0) { // access with no upval modifier && matching declaration
                    pred(this) {
                        if (it is Expr.Proto) {
                            noclos.add(it)          // mark all crossing protos as noclos
                        }
                        (it == dcl.first)    // stop at enclosing declaration block
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
            is Expr.Block  -> this.map(this.es)
            is Expr.Group  -> this.map(this.es)
            is Expr.Dcl    -> emptyMap()
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.While  -> this.map(listOf(this.cnd, this.body))
            is Expr.Catch  -> this.map(listOf(this.cnd, this.body))
            is Expr.Throw  -> this.map(listOf(this.ex))
            is Expr.Defer  -> this.map(listOf(this.body))

            is Expr.Coros  -> this.map(listOfNotNull(this.max))
            is Expr.Coro   -> this.map(listOf(this.task))
            is Expr.Spawn  -> this.map(listOf(this.call) + listOfNotNull(this.coros))
            is Expr.CsIter -> this.map(listOf(this.coros,this.body))
            is Expr.Bcast  -> this.map(listOf(this.evt, this.xin))
            is Expr.Yield  -> this.map(listOf(this.arg))
            is Expr.Resume -> this.map(listOf(this.call))
            is Expr.Toggle -> this.map(listOf(this.coro, this.on))
            is Expr.Pub    -> this.map(listOfNotNull(this.coro))
            is Expr.Track  -> this.map(listOfNotNull(this.coro))

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
