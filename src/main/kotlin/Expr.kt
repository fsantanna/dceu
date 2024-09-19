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
    return this.fupx().let {
        when {
            (it !is Expr.Dcl) -> this.n.toString()
            (it.src != this) -> error("bug found")
            else -> it.idtag.first.str.idc() + (this.up_first() { it is Expr.Do } != outer).cond { "_${this.n}" }
        }
    }
}

fun Expr.is_dst (): Boolean {
    return this.fup().let { it is Expr.Set && it.dst==this }
}

fun Expr.is_drop (): Boolean {
    return LEX && this.fupx().let { it is Expr.Drop && it.e==this }
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
        true -> true
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

fun Expr.data (): Pair<Int?,LData?>? {
    return when (this) {
        is Expr.Acc -> {
            val dcl = this.id_to_dcl(this.tk.str)!!
            dcl.idtag.second.let {
                if (it == null) {
                    null
                } else {
                    Pair(null, G.datas[it.str])
                }
            }
        }
        is Expr.Pub -> {
            if (this.tsk != null) {
                this.tsk.data()
            } else {
                val task = this.up_first_task_outer()
                if (task?.tag == null) null else {
                    Pair(null, G.datas[task.tag.str]!!)
                }
            }
        }
        is Expr.Index -> {
            val d = this.col.data()
            val l = d?.second
            when {
                (d == null) -> null
                (l == null) -> null
                (this.idx !is Expr.Tag) -> null
                else -> {
                    val idx = l.indexOfFirst { it.first.str == this.idx.tk.str.drop(1) }
                    val v = if (idx == -1) null else l[idx]
                    when {
                        (v == null) -> {
                            err(this.idx.tk, "index error : undeclared data field ${this.idx.tk.str}")
                        }
                        (v.second == null) -> Pair(idx, null)
                        else -> {
                            Pair(idx, G.datas[v.second!!.str]!!)
                        }
                    }
                }
            }
        }
        else -> null
    }
}

fun Expr.id_to_dcl (id: String, cross: Boolean=true, but: ((Expr.Dcl)->Boolean)?=null): Expr.Dcl? {
    val up = this.fupx().up_first { it is Expr.Do || it is Expr.Proto }
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
        (up?.fup() == null) -> null
        (up is Expr.Proto && !cross) -> null
        else -> up!!.id_to_dcl(id, cross, but)
    }
}

fun Expr.Acc.idx (): String {
    val dcl = this.id_to_dcl(this.tk.str)!!
    return dcl.idx(this)
}
fun Expr.Dcl.idx (src: Expr): String {
    val id = this.idtag.first.str.idc()
    val blk = this.to_blk()
    val ismem = blk.is_mem()
    //println(listOf(src.tk.pos.lin, id, type(dcl,src)))

    return when (type(this,src)) {
        Type.GLOBAL -> "ceu_glb_$id"
        Type.LOCAL -> if (ismem) "(ceu_mem->${id}_${this.n})" else "ceu_loc_${id}_${this.n}"  // idx b/c of "it"
        Type.NESTED -> {
            val xups = src.up_all_until { it == blk } // all ups between src -> dcl
            val pid = (blk.up_first { it is Expr.Proto } as Expr.Proto).id(G.outer!!)
            val xn = xups.count { it is Expr.Proto && it!=blk }
            "((CEU_Pro_$pid*)ceux->exe_task->${"clo->up_nst->".repeat(xn)}mem)->${id}_${this.n}"
        }
        else -> {
            val proto = src.up_first { it is Expr.Proto } as Expr.Proto
            val i = G.proto_to_upvs[proto]!!.indexOfFirst { it == this }
            assert(i != -1)
            "ceux->clo->upvs.buf[$i]"
        }
    }
}
fun Expr.idx (idc: String): String {
    return if (this.is_mem()) "(ceu_mem->$idc)" else "ceu_$idc"
}

fun Expr.dcl (tp: String="CEU_Value"): String {
    return if (this.is_mem()) "" else tp
}


