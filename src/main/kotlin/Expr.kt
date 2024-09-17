package dceu

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

fun Expr.is_dst (): Boolean {
    return G.ups[this].let { it is Expr.Set && it.dst==this }
}

fun Expr.is_drop (): Boolean {
    return LEX && G.ups[this].let { it is Expr.Drop && it.e==this }
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

fun Expr.Do.to_dcls (): List<Expr.Dcl> {
    fun aux (es: List<Expr>): List<Expr.Dcl> {
        return es.flatMap {
            when {
                (it is Expr.Group) -> aux(it.es)
                (it is Expr.Dcl) -> listOf(it) + aux(listOfNotNull(it.src))
                (it is Expr.Set) -> aux(listOf(it.src))
                else -> emptyList()
            }
        }
    }
    return aux(this.es)
}

fun Expr.Dcl.to_blk (): Expr {
    return this.up_first { it is Expr.Do || it is Expr.Proto }!! // ?: outer /*TODO: remove outer*/
}

fun Expr.id_to_dcl (id: String, cross: Boolean=true, but: ((Expr.Dcl)->Boolean)?=null): Expr.Dcl? {
    val up = G.ups[this]!!.up_first { it is Expr.Do || it is Expr.Proto }
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
        (G.ups[up] == null) -> null
        (up is Expr.Proto && !cross) -> null
        else -> up!!.id_to_dcl(id, cross, but)
    }
}


