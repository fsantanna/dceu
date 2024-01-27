package dceu

class Ups (outer: Expr.Do) {
    val pub = outer.traverse()

    fun all_until (e: Expr, cnd: (Expr)->Boolean): List<Expr> {
        val up = pub[e]
        return listOf(e) + when {
            cnd(e) -> emptyList()
            (up == null) -> emptyList()
            else -> this.all_until(up,cnd)
        }
    }
    fun all (e: Expr): List<Expr> {
        val up = pub[e]
        return listOf(e) + when {
            (up == null) -> emptyList()
            else -> this.all(up)
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
    fun none (e: Expr, cnd: (Expr)->Boolean): Boolean {
        return this.first(e,cnd) == null
    }
    fun first_block (e: Expr): Expr.Do? {
        return this.first(e) { it is Expr.Do && this.pub[it] !is Expr.Export } as Expr.Do?
    }
    fun first_proto_or_block (e: Expr): Expr? {
        return this.first(e) { it is Expr.Proto || (it is Expr.Do && this.pub[it] !is Expr.Export) }
    }
    fun first_task_real (e: Expr): Expr.Proto? {
        return this.first(e) { it is Expr.Proto && it.tk.str=="task" && it.tag?.str!=":void" } as Expr.Proto?
    }
    fun inexe (e: Expr, spc: String?, immediate: Boolean): Boolean {
        fun f (s: String): Boolean {
            return (spc==null && s!="func") || spc==s
        }
        return if (immediate) {
            this.first(e) { it is Expr.Proto }.let { it!=null && f(it.tk.str) }
        } else {
            this.any(e) { it is Expr.Proto && f(it.tk.str) }
        }
    }

    fun Expr.traverse (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.traverse() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
        }
        return when (this) {
            is Expr.Proto  -> this.map(listOf(this.blk))
            is Expr.Export -> this.map(listOf(this.blk))
            is Expr.Do     -> this.map(this.es)
            is Expr.Dcl    -> this.map(listOfNotNull(this.src))
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.Loop   -> this.map(listOf(this.blk))
            is Expr.Break  -> this.map(listOfNotNull(this.cnd, this.e))
            is Expr.Skip   -> this.map(listOf(this.cnd))
            is Expr.Enum   -> emptyMap()
            is Expr.Data   -> emptyMap()
            is Expr.Pass   -> this.map(listOf(this.e))

            is Expr.Catch  -> this.map(listOf(this.cnd, this.blk))
            is Expr.Defer  -> this.map(listOf(this.blk))

            is Expr.Yield  -> this.map(listOf(this.arg))
            is Expr.Resume -> this.map(listOf(this.co, this.arg))

            is Expr.Spawn  -> this.map(listOfNotNull(this.tsks, this.tsk) + this.args)
            is Expr.Delay  -> emptyMap()
            is Expr.Pub    -> this.map(listOfNotNull(this.tsk))
            is Expr.Dtrack -> this.map(listOf(this.blk))
            is Expr.Toggle -> this.map(listOf(this.tsk, this.on))

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
            is Expr.Call   -> this.map(listOf(this.clo) + this.args)
        }
    }
}
