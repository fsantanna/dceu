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

fun Expr.ups_reset () {
    when (this) {
        is Expr.Proto  -> {
            G.ups[this.blk] = this
            this.pars.forEach { G.ups[it] = this }
        }
        is Expr.Do     -> this.es.forEach { G.ups[it] = this }
        is Expr.Escape -> if (this.e != null) G.ups[this.e] = this
        is Expr.Group  -> this.es.forEach { G.ups[it] = this }
        is Expr.Dcl    -> if (this.src != null) G.ups[this.src] = this
        is Expr.Set    -> {
            G.ups[this.dst] = this
            G.ups[this.src] = this
        }
        is Expr.If     -> {
            G.ups[this.cnd] = this
            G.ups[this.t]   = this
            G.ups[this.f]   = this
        }
        is Expr.Loop   -> G.ups[this.blk] = this
        is Expr.Data   -> {}
        is Expr.Drop   -> G.ups[this.e] = this

        is Expr.Catch  -> G.ups[this.blk] = this
        is Expr.Defer  -> G.ups[this.blk] = this

        is Expr.Yield  -> G.ups[this.e] = this
        is Expr.Resume -> {
            G.ups[this.co] = this
            this.args.forEach { G.ups[it] = this }
        }

        is Expr.Spawn  -> {
            if (this.tsks != null) G.ups[this.tsks] = this
            G.ups[this.tsk] = this
            this.args.forEach { G.ups[it] = this }
        }
        is Expr.Delay  -> {}
        is Expr.Pub    -> if (this.tsk != null) G.ups[this.tsk] = this
        is Expr.Toggle -> {
            G.ups[this.tsk] = this
            G.ups[this.on]  = this
        }
        is Expr.Tasks  -> G.ups[this.max] = this

        is Expr.Nat    -> {}
        is Expr.Acc    -> {}
        is Expr.Nil    -> {}
        is Expr.Tag    -> {}
        is Expr.Bool   -> {}
        is Expr.Char   -> {}
        is Expr.Num    -> {}
        is Expr.Tuple  -> this.args.forEach { G.ups[it] = this }
        is Expr.Vector -> this.args.forEach { G.ups[it] = this }
        is Expr.Dict   -> {
            this.args.forEach {
                G.ups[it.first]  = this
                G.ups[it.second] = this
            }
        }
        is Expr.Index  -> {
            G.ups[this.col] = this
            G.ups[this.idx] = this
        }
        is Expr.Call   -> {
            G.ups[this.clo] = this
            this.args.forEach { G.ups[it] = this }
        }
    }
}
