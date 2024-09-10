package dceu

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

fun Expr.Dcl.toblk (): Expr {
    return this.up_first { it is Expr.Do || it is Expr.Proto }!! // ?: outer /*TODO: remove outer*/
}

fun Expr.id_to_dcl (id: String, cross: Boolean=true, but: ((Expr.Dcl)->Boolean)?=null): Expr.Dcl? {
    val up = this.up!!.up_first { it is Expr.Do || it is Expr.Proto }
    fun aux (es: List<Expr>): Expr.Dcl? {
        return es.firstNotNullOfOrNull {
            when {
                (it is Expr.Set) -> aux(listOfNotNull(it.src))
                (it is Expr.Group) -> aux(it.es)
                (it !is Expr.Dcl) -> null
                (but!=null && but(it)) -> aux(listOfNotNull(it.src))
                (it.idtag.first.str == id) -> it
                else -> aux(listOfNotNull(it.src))
            }
        }

    }
    val dcl: Expr.Dcl? = when {
        (up is Expr.Proto) -> up.pars.firstOrNull { (but==null||!but(it)) && it.idtag.first.str==id }
        (up is Expr.Do) -> aux(up.es)
        else -> null
    }
    return when {
        (dcl != null) -> dcl
        (up!!.up == null) -> null
        (up is Expr.Proto && !cross) -> null
        else -> up!!.id_to_dcl(id, cross, but)
    }
}

class Ups (val outer: Expr.Do) {
    val pub = outer.traverse()

    fun id_to_dcl (id: String, from: Expr, cross: Boolean=true, but: ((Expr.Dcl)->Boolean)?=null): Expr.Dcl? {
        val up = this.pub[from]!!.up_first { it is Expr.Do || it is Expr.Proto }
        fun aux (es: List<Expr>): Expr.Dcl? {
            return es.firstNotNullOfOrNull {
                when {
                    (it is Expr.Set) -> aux(listOfNotNull(it.src))
                    (it is Expr.Group) -> aux(it.es)
                    (it !is Expr.Dcl) -> null
                    (but!=null && but(it)) -> aux(listOfNotNull(it.src))
                    (it.idtag.first.str == id) -> it
                    else -> aux(listOfNotNull(it.src))
                }
            }

        }
        val dcl: Expr.Dcl? = when {
            (up is Expr.Proto) -> up.pars.firstOrNull { (but==null||!but(it)) && it.idtag.first.str==id }
            (up is Expr.Do) -> aux(up.es)
            else -> null
        }
        return when {
            (dcl != null) -> dcl
            (up == outer) -> null
            (up is Expr.Proto && !cross) -> null
            else -> up!!.id_to_dcl(id, cross, but)
        }
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
