package dceu

class Ups (outer: Expr.Do) {
    val pub = outer.traverse()

    fun all_until (e: Expr, cnd: (Expr)->Boolean): List<Expr> {
        val up = pub[e]
        return when {
            cnd(e) -> listOf(e)
            (up == null) -> emptyList()
            else -> this.all_until(up,cnd).let { if (it.isEmpty()) it else it+e }
        }
    }
    fun first (e: Expr, cnd: (Expr)->Boolean): Expr? {
        val up = pub[e]
        return when {
            cnd(e) -> e
            (up == null) -> null
            else -> this.first(up,cnd)
        }
    }
    fun any (e: Expr, cnd: (Expr)->Boolean): Boolean {
        return this.first(e,cnd) != null
    }
    fun first_block (e: Expr): Expr.Do? {
        return this.first(e) { it is Expr.Do } as Expr.Do?
    }
    fun first_proto_or_block (e: Expr): Expr? {
        return this.first(e) { it is Expr.Proto || (it is Expr.Do) }
    }
    fun first_true_x (e: Expr, x: String): Expr.Proto? {
        return this.first(e) { it is Expr.Proto && it.tk.str==x } as Expr.Proto?
    }

    fun inexe (e: Expr): Boolean {
        return this.first(e) { it is Expr.Proto }.let { (it!=null && it.tk.str!="func") }
    }

    fun Expr.traverse (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.traverse() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
        }
        return when (this) {
            is Expr.Proto  -> this.map(listOf(this.blk))
            is Expr.Do     -> this.map(this.es)
            is Expr.Dcl    -> this.map(listOfNotNull(this.src))
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.XLoop  -> this.map(listOf(this.blk))
            is Expr.XBreak -> this.map(listOf(this.cnd) + listOfNotNull(this.e))
            is Expr.Enum   -> emptyMap()
            is Expr.Data   -> emptyMap()
            is Expr.Pass   -> this.map(listOf(this.e))
            is Expr.Drop   -> this.map(listOf(this.e))

            is Expr.Catch  -> this.map(listOf(this.cnd) + listOf(this.blk))
            is Expr.Defer  -> this.map(listOf(this.blk))

            is Expr.Yield  -> this.map(listOf(this.arg) + listOf(this.blk))
            is Expr.Resume -> this.map(listOf(this.call))

            is Expr.Spawn  -> this.map(listOfNotNull(this.tsks) + listOf(this.call))
            is Expr.Bcast  -> this.map(listOf(this.call))
            is Expr.Dtrack -> this.map(listOf(this.trk) + listOf(this.blk))

            is Expr.Nat    -> emptyMap()
            is Expr.Acc    -> emptyMap()
            is Expr.Nil    -> emptyMap()
            is Expr.Tag    -> emptyMap()
            is Expr.Bool   -> emptyMap()
            is Expr.Char   -> emptyMap()
            is Expr.Num    -> emptyMap()
            is Expr.Tuple  -> this.map(this.args)
            is Expr.Vector -> this.map(this.args)
            is Expr.Dict   -> this.map(this.args.map { listOf(it.first,it.second) }.flatten())
            is Expr.Index  -> this.map(listOf(this.col, this.idx))
            is Expr.Call   -> this.map(listOf(this.clo)) + this.map(this.args)
        }
    }
}
