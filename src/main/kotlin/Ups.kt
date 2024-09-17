package dceu

fun ups_reset () {
    G.outer!!.dn_gather { me ->
        when (me) {
            is Expr.Proto -> {
                G.ups[me.blk] = me
                me.pars.forEach { G.ups[it] = me }
            }

            is Expr.Do -> me.es.forEach { G.ups[it] = me }
            is Expr.Escape -> if (me.e != null) G.ups[me.e] = me
            is Expr.Group -> me.es.forEach { G.ups[it] = me }
            is Expr.Dcl -> if (me.src != null) G.ups[me.src] = me
            is Expr.Set -> {
                G.ups[me.dst] = me
                G.ups[me.src] = me
            }

            is Expr.If -> {
                G.ups[me.cnd] = me
                G.ups[me.t] = me
                G.ups[me.f] = me
            }

            is Expr.Loop -> G.ups[me.blk] = me
            is Expr.Data -> {}
            is Expr.Drop -> G.ups[me.e] = me

            is Expr.Catch -> G.ups[me.blk] = me
            is Expr.Defer -> G.ups[me.blk] = me

            is Expr.Yield -> G.ups[me.e] = me
            is Expr.Resume -> {
                G.ups[me.co] = me
                me.args.forEach { G.ups[it] = me }
            }

            is Expr.Spawn -> {
                if (me.tsks != null) G.ups[me.tsks] = me
                G.ups[me.tsk] = me
                me.args.forEach { G.ups[it] = me }
            }

            is Expr.Delay -> {}
            is Expr.Pub -> if (me.tsk != null) G.ups[me.tsk] = me
            is Expr.Toggle -> {
                G.ups[me.tsk] = me
                G.ups[me.on] = me
            }

            is Expr.Tasks -> G.ups[me.max] = me

            is Expr.Nat -> {}
            is Expr.Acc -> {}
            is Expr.Nil -> {}
            is Expr.Tag -> {}
            is Expr.Bool -> {}
            is Expr.Char -> {}
            is Expr.Num -> {}
            is Expr.Tuple -> me.args.forEach { G.ups[it] = me }
            is Expr.Vector -> me.args.forEach { G.ups[it] = me }
            is Expr.Dict -> {
                me.args.forEach {
                    G.ups[it.first] = me
                    G.ups[it.second] = me
                }
            }

            is Expr.Index -> {
                G.ups[me.col] = me
                G.ups[me.idx] = me
            }

            is Expr.Call -> {
                G.ups[me.clo] = me
                me.args.forEach { G.ups[it] = me }
            }
        }
        emptyMap<Unit,Unit>()
    }
}

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
