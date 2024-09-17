package dceu

/*
fun <K,V> List<Map<K,V>>.union (): Map<K,V> {
    return this.fold(emptyMap()) { acc, value -> acc + value }
}

fun <K,V> Expr.dn_gather (f: (Expr)->Map<K,V>?): Map<K,V> {
    val v = f(this)
    if (v == null) {
        return emptyMap()
    }
    return v + when (this) {
        is Expr.Proto  -> this.blk.dn_gather(f)
        is Expr.Do     -> this.es.map { it.dn_gather(f) }.union()
        is Expr.Escape -> this.e?.dn_gather(f) ?: emptyMap()
        is Expr.Group  -> this.es.map { it.dn_gather(f) }.union()
        is Expr.Dcl    -> this.src?.dn_gather(f) ?: emptyMap()
        is Expr.Set    -> this.dst.dn_gather(f) + this.src.dn_gather(f)
        is Expr.If     -> this.cnd.dn_gather(f) + this.t.dn_gather(f) + this.f.dn_gather(f)
        is Expr.Loop   -> this.blk.dn_gather(f)
        is Expr.Drop   -> this.e.dn_gather(f)

        is Expr.Catch  -> this.blk.dn_gather(f)
        is Expr.Defer  -> this.blk.dn_gather(f)

        is Expr.Yield  -> this.e.dn_gather(f)
        is Expr.Resume -> this.co.dn_gather(f) + this.args.map { it.dn_gather(f) }.union()

        is Expr.Spawn  -> (this.tsks?.dn_gather(f) ?: emptyMap()) + this.tsk.dn_gather(f) + this.args.map { it.dn_gather(f) }.union()
        is Expr.Delay  -> emptyMap()
        is Expr.Pub    -> this.tsk?.dn_gather(f) ?: emptyMap()
        is Expr.Toggle -> this.tsk.dn_gather(f) + this.on.dn_gather(f)
        is Expr.Tasks  -> this.max.dn_gather(f)

        is Expr.Tuple  -> this.args.map { it.dn_gather(f) }.union()
        is Expr.Vector -> this.args.map { it.dn_gather(f) }.union()
        is Expr.Dict   -> this.args.map { it.first.dn_gather(f) + it.second.dn_gather(f) }.union()
        is Expr.Index  -> this.col.dn_gather(f) + this.idx.dn_gather(f)
        is Expr.Call   -> this.clo.dn_gather(f) + this.args.map { it.dn_gather(f) }.union()

        is Expr.Acc, is Expr.Data, is Expr.Nat,
        is Expr.Nil, is Expr.Tag, is Expr.Bool,
        is Expr.Char, is Expr.Num -> emptyMap()
    }
}
 */

fun Expr.up_first (cnd: (Expr)->Boolean): Expr? {
    return when {
        cnd(this) -> this
        (G.ups[this] == null) -> null
        else -> G.ups[this]!!.up_first(cnd)
    }
}

fun Expr.up_all_until (cnd: (Expr)->Boolean): List<Expr> {
    return listOf(this) + when {
        cnd(this) -> emptyList()
        (G.ups[this] == null) -> emptyList()
        else -> G.ups[this]!!.up_all_until(cnd)
    }
}

fun Expr.up_first_without (cnd1: (Expr)->Boolean, cnd2: (Expr)->Boolean): Expr? {
    return when {
        cnd2(this) -> null
        cnd1(this) -> this
        (G.ups[this] == null) -> null
        else -> G.ups[this]!!.up_first_without(cnd1,cnd2)
    }
}

fun Expr.up_any (cnd: (Expr)->Boolean): Boolean {
    return this.up_first(cnd) != null
}
fun Expr.up_none (cnd: (Expr)->Boolean): Boolean {
    return this.up_first(cnd) == null
}

fun Expr.up_first_task_outer (): Expr.Proto? {
    return this.up_first {
        when {
            (it !is Expr.Proto) -> false
            (it.tk.str != "task") -> false
            !it.fake -> true
            (G.ups[it.up_first { it is Expr.Do }!!] == null) -> true
            else -> false
        }
    } as Expr.Proto?
}

fun Expr.up_exe (tp: String?=null): Expr.Proto? {
    return this.up_first { it is Expr.Proto }.let {
        if (it==null || it.tk.str=="func" || (tp!=null && it.tk.str!=tp)) {
            null
        } else {
            it as Expr.Proto
        }
    }
}

fun Expr.isdst (): Boolean {
    return G.ups[this].let { it is Expr.Set && it.dst==this }
}

fun Expr.isdrop (): Boolean {
    return LEX && G.ups[this].let { it is Expr.Drop && it.e==this }
}

class Ups (val outer: Expr.Do) {
    init {
        G.ups = outer.traverse().toMutableMap()
    }

    fun Expr.traverse (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.traverse() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
        }
        return when (this) {
            is Expr.Proto  -> this.map(listOf(this.blk) + this.pars)
            is Expr.Do     -> this.map(this.es)
            is Expr.Escape -> this.map(listOfNotNull(this.e))
            is Expr.Group  -> this.map(this.es)
            is Expr.Dcl    -> this.map(listOfNotNull(this.src))
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.Loop   -> this.map(listOf(this.blk))
            is Expr.Data   -> emptyMap()
            is Expr.Drop   -> this.map(listOf(this.e))

            is Expr.Catch  -> this.map(listOf(this.blk))
            is Expr.Defer  -> this.map(listOf(this.blk))

            is Expr.Yield  -> this.map(listOf(this.e))
            is Expr.Resume -> this.map(listOf(this.co) + this.args)

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
