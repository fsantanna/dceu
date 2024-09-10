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
