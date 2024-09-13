package dceu

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

fun Expr.up_first (cnd: (Expr)->Boolean): Expr? {
    return when {
        cnd(this) -> this
        (this.up == null) -> null
        else -> this.up!!.up_first(cnd)
    }
}

fun Expr.up_all_until (cnd: (Expr)->Boolean): List<Expr> {
    return listOf(this) + when {
        cnd(this) -> emptyList()
        (this.up == null) -> emptyList()
        else -> this.up!!.up_all_until(cnd)
    }
}

fun Expr.up_first_without (cnd1: (Expr)->Boolean, cnd2: (Expr)->Boolean): Expr? {
    return when {
        cnd2(this) -> null
        cnd1(this) -> this
        (up == null) -> null
        else -> this.up!!.up_first_without(cnd1,cnd2)
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
            (it.up_first { it is Expr.Do }!!.up == null) -> true
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
    return this.up.let { it is Expr.Set && it.dst==this }
}

fun Expr.isdrop (): Boolean {
    return LEX && this.up.let { it is Expr.Drop && it.e==this }
}
