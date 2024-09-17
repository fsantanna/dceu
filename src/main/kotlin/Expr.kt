package dceu

fun Expr.is_dst (): Boolean {
    return G.ups[this].let { it is Expr.Set && it.dst==this }
}

fun Expr.isdrop (): Boolean {
    return LEX && G.ups[this].let { it is Expr.Drop && it.e==this }
}

fun Expr.Call.main (): Expr.Proto {
    assert(this.tk.str == "main")
    return this.clo as Expr.Proto
}

fun Expr.Proto.id (outer: Expr.Do): String {
    return G.ups[this].let {
        when {
            (it !is Expr.Dcl) -> this.n.toString()
            (it.src != this) -> error("bug found")
            else -> it.idtag.first.str.idc() + (this.up_first() { it is Expr.Do } != outer).cond { "_${this.n}" }
        }
    }
}

fun Expr.is_constructor (): Boolean {
    return when {
        this.is_static() -> true
        else -> when (this) {
            is Expr.Tuple, is Expr.Vector, is Expr.Dict -> true
            else -> false
        }
    }
}

fun Expr.is_static (): Boolean {
    return when (this) {
        is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> true
        else -> false
    }
}

fun Expr.is_lval (): Boolean {
    return when (this) {
        is Expr.Acc -> true
        is Expr.Index -> true
        is Expr.Pub -> true
        else -> false
    }
}

fun Expr.base (): Expr {
    return when (this) {
        is Expr.Acc   -> this
        is Expr.Index -> this.col.base()
        is Expr.Pub   -> TODO() //this.tsk?.base(ups) ?: ups.first(this) { it is Expr.Proto }!!
        else -> {
            println(this)
            TODO()
        }
    }
}

fun Expr.is_mem (out: Boolean=false): Boolean {
    val proto = this.up_first { it is Expr.Proto }.let {
        when {
            (it == null) -> null
            (it.tk.str == "func") -> null
            else -> it
        }
    }
    val up = this.up_first() { it is Expr.Do || it is Expr.Proto }!!
    return when {
        (!out && proto==null) -> false
        //true -> true
        G.mems.contains(up) -> true
        else -> false
    }
}

