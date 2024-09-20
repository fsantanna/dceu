package dceu

fun Expr.up_first (cnd: (Expr)->Boolean): Expr? {
    return when {
        cnd(this) -> this
        (this.fup() === null) -> null
        else -> this.fupx().up_first(cnd)
    }
}

fun Expr.up_all_until (cnd: (Expr)->Boolean): List<Expr> {
    return listOf(this) + when {
        cnd(this) -> emptyList()
        (this.fup() === null) -> emptyList()
        else -> this.fupx().up_all_until(cnd)
    }
}

fun Expr.up_first_without (cnd1: (Expr)->Boolean, cnd2: (Expr)->Boolean): Expr? {
    return when {
        cnd2(this) -> null
        cnd1(this) -> this
        (this.fup() === null) -> null
        else -> this.fupx().up_first_without(cnd1,cnd2)
    }
}

fun Expr.up_any (cnd: (Expr)->Boolean): Boolean {
    return this.up_first(cnd) !== null
}

fun Expr.up_none (cnd: (Expr)->Boolean): Boolean {
    return this.up_first(cnd) === null
}

fun Expr.up_first_task_outer (): Expr.Proto? {
    return this.up_first {
        when {
            (it !is Expr.Proto) -> false
            (it.tk.str != "task") -> false
            !it.fake -> true
            (it.up_first { it is Expr.Do }!!.fup() === null) -> true
            else -> false
        }
    } as Expr.Proto?
}

fun Expr.up_exe (tp: String?=null): Expr.Proto? {
    return this.up_first { it is Expr.Proto }.let {
        if (it===null || it.tk.str=="func" || (tp!==null && it.tk.str!=tp)) {
            null
        } else {
            it as Expr.Proto
        }
    }
}
