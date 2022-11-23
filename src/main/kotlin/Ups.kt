class Ups (outer: Expr.Block) {

    val ups = outer.calc()
    
    fun pred (e: Expr, f: (Expr)->Boolean): Expr? {
        val x = ups[e]
        return when {
            (x == null) -> null
            f(x) -> x
            else -> this.pred(x, f)
        }
    }

    fun block (e: Expr): Expr.Block? {
        return this.pred(e) { it is Expr.Block } as Expr.Block?
    }

    fun func (e: Expr): Expr.Func? {
        return this.pred(e) { it is Expr.Func } as Expr.Func?
    }

    fun func_or_block (e: Expr): Expr? {
        return this.pred(e) { it is Expr.Func || it is Expr.Block }
    }

    fun Expr.calc (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.calc() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
        }
        return when (this) {
            is Expr.Block  -> this.map(this.es)
            is Expr.Dcl    -> emptyMap()
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.While  -> this.map(listOf(this.cnd, this.body))
            is Expr.Func   -> this.map(listOf(this.body))
            is Expr.Catch  -> this.map(listOf(this.cnd, this.body))
            is Expr.Throw  -> this.map(listOf(this.ex))
            is Expr.Defer  -> this.map(listOf(this.body))

            is Expr.Coros  -> emptyMap()
            is Expr.Coro   -> this.map(listOf(this.task))
            is Expr.Spawn  -> this.map(listOf(this.call) + listOfNotNull(this.coros))
            is Expr.Iter   -> this.map(listOf(this.coros,this.body))
            is Expr.Bcast  -> this.map(listOf(this.evt))
            is Expr.Yield  -> this.map(listOf(this.arg))
            is Expr.Resume -> this.map(listOf(this.call))

            is Expr.Nat    -> emptyMap()
            is Expr.Acc    -> emptyMap()
            is Expr.Nil    -> emptyMap()
            is Expr.Tag    -> emptyMap()
            is Expr.Bool   -> emptyMap()
            is Expr.Num    -> emptyMap()
            is Expr.Tuple  -> this.map(this.args)
            is Expr.Dict   -> this.map(this.args.map { listOf(it.first,it.second) }.flatten())
            is Expr.Index  -> this.map(listOf(this.col, this.idx))
            is Expr.Call   -> this.map(listOf(this.f)) + this.map(this.args)

            is Expr.XSeq -> error("bug found")
        }
    }
}