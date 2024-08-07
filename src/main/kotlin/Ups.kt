package dceu

class Ups (val outer: Expr.Do) {
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
    fun first_without (e: Expr, cnd1: (Expr)->Boolean, cnd2: (Expr)->Boolean): Expr? {
        val up = pub[e]
        return when {
            cnd2(e) -> null
            cnd1(e) -> e
            (up == null) -> null
            else -> this.first_without(up,cnd1,cnd2)
        }
    }
    fun any (e: Expr, cnd: (Expr)->Boolean): Boolean {
        return this.first(e,cnd) != null
    }
    fun none (e: Expr, cnd: (Expr)->Boolean): Boolean {
        return this.first(e,cnd) == null
    }
    fun first_task_outer (e: Expr): Expr.Proto? {
        return this.first(e) { it is Expr.Proto && it.tk.str=="task" && !this.isnst(it) } as Expr.Proto?
    }
    fun exe (e: Expr, tp: String?=null): Expr.Proto? {
        return this.first(e) { it is Expr.Proto }.let {
            if (it==null || it.tk.str=="func" || (tp!=null && it.tk.str!=tp)) {
                null
            } else {
                it as Expr.Proto
            }
        }
    }
    fun isnst (proto: Expr.Proto): Boolean {
        return (proto.nst && (CEU<99 || this.any(this.pub[proto]!!) { it is Expr.Proto }))
    }
    fun isdst (e: Expr): Boolean {
        return this.pub[e].let { it is Expr.Set && it.dst==e }
    }


    fun Expr.traverse (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.traverse() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
        }
        return when (this) {
            is Expr.Proto  -> this.map(listOf(this.blk))
            is Expr.Do     -> this.map(this.es)
            is Expr.Group  -> this.map(this.es)
            is Expr.Dcl    -> this.map(listOfNotNull(this.src))
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.Loop   -> this.map(listOf(this.blk))
            is Expr.Break  -> this.map(listOfNotNull(this.e))
            is Expr.Skip   -> emptyMap()
            is Expr.Data   -> emptyMap()

            is Expr.Catch  -> this.map(listOf(this.cnd, this.blk))
            is Expr.Defer  -> this.map(listOf(this.blk))

            is Expr.Yield  -> this.map(listOf(this.e))
            is Expr.Resume -> this.map(listOf(this.co)+this.args)

            is Expr.Spawn  -> this.map(listOfNotNull(this.tsks,this.tsk) + this.args)
            is Expr.Delay  -> emptyMap()
            is Expr.Pub    -> this.map(listOfNotNull(this.tsk))
            is Expr.Toggle -> this.map(listOf(this.tsk, this.on))
            is Expr.Tasks  -> this.map(listOf(this.max))

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
            is Expr.Call   -> this.map(listOf(this.clo)+this.args)
        }
    }
}
