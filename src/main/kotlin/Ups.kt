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
        return this.all_until(e,cnd).firstOrNull()
    }
    fun first_block (e: Expr): Expr.Do? {
        return this.first(e) { it is Expr.Do && it.isnest && it.ishide } as Expr.Do?
    }
    fun first_proto_or_block (e: Expr): Expr? {
        return this.first(e) { it is Expr.Proto || (it is Expr.Do && it.isnest && it.ishide) }
    }
    fun intask (e: Expr): Boolean {
        return this.first(e) { it is Expr.Proto }.let { (it!=null && it.tk.str!="func") }
    }
    fun first_true_x (e: Expr, x: String): Expr.Proto? {
        return this.first(e) {
            it is Expr.Proto && it.tk.str==x && (it.task==null || !it.task.second)
        } as Expr.Proto?
    }
    fun true_x_c (e: Expr, str: String): String? {
        val x = this.first_true_x(e, str)
        val n = this     // find first non fake
            .all_until(e) { it == x }
            .filter { it is Expr.Proto } // but count all protos in between
            .count()
        return if (n == 0) null else "(ceu_frame${"->proto->up_frame".repeat(n-1)})"
    }

    fun Expr.traverse (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.traverse() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
        }
        return when (this) {
            is Expr.Proto  -> this.map(listOf(this.body))
            is Expr.Do     -> this.map(this.es)
            is Expr.Dcl    -> this.map(listOfNotNull(this.src))
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.Loop  -> this.map(listOf(this.cnd, this.body))
            is Expr.Catch  -> this.map(listOf(this.cnd, this.body))
            is Expr.Defer  -> this.map(listOf(this.body))
            is Expr.Enum   -> emptyMap()
            is Expr.Data   -> emptyMap()
            is Expr.Pass   -> this.map(listOf(this.e))

            is Expr.Spawn  -> this.map(listOf(this.call) + listOfNotNull(this.tasks))
            is Expr.Bcast  -> this.map(listOf(this.evt, this.xin))
            is Expr.Yield  -> this.map(listOf(this.arg))
            is Expr.Resume -> this.map(listOf(this.call))
            is Expr.Toggle -> this.map(listOf(this.task, this.on))
            is Expr.Pub    -> this.map(listOf(this.x))
            is Expr.Self   -> emptyMap()

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
