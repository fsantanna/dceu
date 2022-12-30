// func (args) or block (locals)
data class XBlock (val syms: MutableSet<String>, val defers: MutableList<String>?)

class Ups (val outer: Expr.Block) {
    val xblocks = mutableMapOf<Expr,XBlock>(Pair(outer, XBlock(GLOBALS.toMutableSet(), mutableListOf())))
    val ups = outer.calc()

    init {
        this.outer.check()
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

    fun isDeclared (e: Expr, id: String): Boolean {
        val xblock = this.xblocks[e]!!
        val up = this.proto_or_block_or_group(e)
        //println(listOf("GET", id, e.javaClass.name, xblock.syms.contains(id)))
        return (xblock.syms.contains(id) || (up!=null && this.isDeclared(up,id)))
    }
    fun assertIsNotDeclared (e: Expr, id: String, tk: Tk) {
        if (this.isDeclared(e,id)) {
            err(tk, "declaration error : variable \"$id\" is already declared")
        }
    }
    fun assertIsDeclared (e: Expr, id: String, tk: Tk) {
        if (!this.isDeclared(e,id)) {
            err(tk, "access error : variable \"$id\" is not declared")
        }
    }

    fun Expr.check () {
        when (this) {
            is Expr.Proto   -> {
                xblocks[this] = XBlock(this.args.let {
                    it.map { it.str } + it.map { "_${it.str}_" }
                }.toMutableSet(), null)
                this.body.check()
            }
            is Expr.Block -> {
                if (this != outer) {
                    xblocks[this] = XBlock(mutableSetOf(), mutableListOf())
                }
                this.es.forEach { it.check() }
            }
            is Expr.Group -> {
                if (this.isHide) {
                    xblocks[this] = XBlock(mutableSetOf(), mutableListOf())
                }
                this.es.forEach { it.check() }
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                val bup = block_or_group(this)!!
                //println(listOf("DCL", id, bup.javaClass.name))
                val xup = xblocks[bup]!!
                assertIsNotDeclared(bup, id, this.tk)
                xup.syms.add(id)
                xup.syms.add("_${id}_")
            }
            is Expr.Set    -> { this.dst.check() ; this.src.check() }
            is Expr.If     -> { this.cnd.check() ; this.t.check() ; this.f.check() }
            is Expr.While  -> { this.cnd.check() ; this.body.check() }
            is Expr.Catch  -> { this.cnd.check() ; this.body.check() }
            is Expr.Throw  -> this.ex.check()
            is Expr.Defer  -> this.body.check()

            is Expr.Coros  -> this.max?.check()
            is Expr.Coro   -> this.task.check()
            is Expr.Spawn  -> { this.call.check() ; this.coros?.check() }
            is Expr.CsIter -> { this.coros.check() ; this.body.check() }
            is Expr.Bcast  -> this.evt.check()
            is Expr.Yield  -> {
                if (!intask(this)) {
                    err(this.tk, "yield error : expected enclosing task")
                }
                this.arg.check()
            }
            is Expr.Resume -> this.call.check()
            is Expr.Toggle -> { this.coro.check() ; this.on.check() }
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
                this.coro?.check()
            }
            is Expr.Track  -> this.coro.check()

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.EvtErr -> {}
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach{ it.check() }
            is Expr.Vector -> this.args.forEach{ it.check() }
            is Expr.Dict   -> this.args.forEach { it.first.check() ; it.second.check() }
            is Expr.Index  -> { this.col.check() ; this.idx.check() }
            is Expr.Call   -> { this.proto.check() ; this.args.forEach { it.check() } }
        }
    }
    fun Expr.calc (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.calc() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
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
