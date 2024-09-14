package dceu

typealias LData = List<Id_Tag>

enum class Type {
    GLOBAL, LOCAL, NESTED, UPVAL
}

fun Expr.Dcl.toblk (): Expr {
    return this.up_first { it is Expr.Do || it is Expr.Proto }!! // ?: outer /*TODO: remove outer*/
}

fun Expr.id_to_dcl (id: String, cross: Boolean=true, but: ((Expr.Dcl)->Boolean)?=null): Expr.Dcl? {
    val up = this.up!!.up_first { it is Expr.Do || it is Expr.Proto }
    val dcl: Expr.Dcl? = when {
        (up is Expr.Proto) -> up.pars.firstOrNull { (but==null||!but(it)) && it.idtag.first.str==id }
        (up is Expr.Do) -> up.to_dcls()[id]
        else -> null
    }
    return when {
        (dcl != null) -> dcl
        (up!!.up == null) -> null
        (up is Expr.Proto && !cross) -> null
        else -> up!!.id_to_dcl(id, cross, but)
    }
}

fun Expr.Do.to_dcls (): Map<String,Expr.Dcl> {
    return this.dn_gather {
        when (it) {
            this -> emptyMap()
            is Expr.Proto -> null
            is Expr.Do -> null
            is Expr.Dcl -> mapOf(Pair(it.idtag.first.str,it))
            else -> emptyMap()
        }
    }
}

fun Expr.Proto.to_nonlocs (): List<Expr.Dcl> {
    return this
        .dn_gather { if (it is Expr.Acc) mapOf(Pair(it,true)) else emptyMap() }
        .map { (acc,_) -> acc.id_to_dcl(acc.tk.str)!!.let {
            Pair(it, it.toblk())
        }}
        .filter { (_,blk) -> blk.up!=null }
        .filter { (_,blk) -> this.up!!.up_first { it==blk } != null }
        .map { (dcl,_) -> dcl }
        .sortedBy { it.n }
}

class Vars (val outer: Expr.Do) {
    val datas = mutableMapOf<String,LData>()

    public val nats: Map<Expr.Nat,Pair<List<String>,String>>

    init {
        // check variable declarations
        val blks = outer.dn_gather {
            when (it) {
                is Expr.Do -> mapOf(Pair(it,true))
                is Expr.Proto -> mapOf(Pair(it,true))
                else -> emptyMap()
            }
        }
        for (blk in blks.keys) {
            val dcls = when (blk) {
                is Expr.Do -> blk.to_dcls().values
                is Expr.Proto -> blk.pars
                else -> error("impossible case")
            }
            val oths = blk.dn_gather {
                when (it) {
                    is Expr.Dcl -> mapOf(Pair(it,true))
                    is Expr.Proto -> null
                    else -> emptyMap()
                }
            }.keys
            for (dcl in dcls) {
                if (dcl.idtag.first.str == "it") {
                    // ok
                } else {
                    for (oth in oths) {
                        if (dcl != oth && dcl.idtag.first.str == oth.idtag.first.str) {
                            err(oth.tk, "declaration error : variable \"${oth.idtag.first.str}\" is already declared")
                        }
                    }
                }
            }
        }

        // check data declarations
        val dats = outer.dn_gather {
            when (it) {
                is Expr.Data -> {
                    val sup = it.tk.str.dropLastWhile { it != '.' }.dropLast(1)
                    val ids = (datas[sup] ?: emptyList()) + it.ids
                    datas[it.tk.str] = ids
                    mapOf(Pair(it, true))
                }
                is Expr.Index -> {
                    data(it)
                    emptyMap()
                }
                else -> emptyMap()
            }
        }.keys
        val ids_dats = dats.map { Pair(it.tk.str,it) }.toMap()

        fun Expr.Data.fields (): LData {
            val sup = this.tk.str.dropLastWhile { it != '.' }.dropLast(1)
            return (ids_dats[sup]?.fields() ?: emptyList()) + this.ids
        }

        for (dat in dats) {
            if (ids_dats[dat.tk.str] != dat) {
                err(dat.tk, "data error : data ${dat.tk.str} is already declared")
            }
            val flds = dat.fields()
            flds.map { it.first.str }.let {
                if (it.size != it.distinct().size) {
                    err(dat.tk, "data error : found duplicate ids")
                }
            }
            for ((_,tag) in flds) {
                if (tag != null && !ids_dats.containsKey(tag.str)) {
                    err(tag, "data error : data ${tag.str} is not declared")
                }
            }
        }

        outer.dn_gather {
            if (it is Expr.Proto) {
                if (it.tag !=null && !datas.containsKey(it.tag.str)) {
                    err(it.tag, "declaration error : data ${it.tag.str} is not declared")
                }
            }
            emptyMap<Unit,Unit>()
        }

        // gather nats
        this.nats = outer.dn_gather { nat ->
            if (nat !is Expr.Nat) emptyMap() else {
                val src = nat.tk.str
                assert(!src.contains("XXX")) { "TODO: native cannot contain XXX"}
                val set = mutableListOf<String>()
                var str = ""
                var i = 0

                var lin = 1
                var col = 1
                fun read (): Char {
                    //assert(i < src.length) { "bug found" }
                    if (i >= src.length) {
                        err(nat.tk, "native error : (lin $lin, col $col) : unterminated token")
                    }
                    val x = src[i++]
                    if (x == '\n') {
                        lin++; col=0
                    } else {
                        col++
                    }
                    return x
                }

                while (i < src.length) {
                    val x1 = read()
                    str += if (x1 != '$') x1 else {
                        val (l,c) = Pair(lin,col)
                        var id = ""
                        var no = ""
                        while (i < src.length) {
                            val x2 = read()
                            if (x2.isLetterOrDigit() || x2=='_' || x2=='-') {
                                id += x2
                            } else {
                                no += x2
                                break
                            }
                        }
                        if (id.length == 0) {
                            err(nat.tk, "native error : (lin $l, col $c) : invalid identifier")
                        }
                        set.add(id)
                        "(XXX)$no"
                    }
                }
                mapOf(Pair(nat, Pair(set, str)))
            }
        }

        // check accs
        val xaccs = outer.dn_gather {
            if (it is Expr.Acc) mapOf(Pair(it,true)) else emptyMap()
        }.keys.map { Pair(it, it.tk.str) }
        val xnats = nats.map { v -> v.value.first.map { Pair(v.key, it) } }.flatten()
        for ((e,id) in xaccs+xnats) {
            val dcl: Expr.Dcl? = e.id_to_dcl(id)
            if (dcl == null) {
                err(e.tk, "access error : variable \"${id}\" is not declared")
            }
            if (type(dcl,e) == Type.UPVAL) {
                if (dcl.tk.str=="var" || dcl.tk.str=="var'") {
                    err(e.tk, "access error : outer variable \"${dcl.idtag.first.str}\" must be immutable")
                }
            }
        }
    }

    fun data (e: Expr): Pair<Int?,LData?>? {
        return when (e) {
            is Expr.Acc -> {
                val dcl = e.id_to_dcl(e.tk.str)!!
                dcl.idtag.second.let {
                    if (it == null) {
                        null
                    } else {
                        Pair(null, this.datas[it.str])
                    }
                }
            }
            is Expr.Pub -> {
                if (e.tsk != null) {
                    this.data(e.tsk)
                } else {
                    val task = e.up_first_task_outer()
                    if (task?.tag == null) null else {
                        Pair(null, this.datas[task.tag.str]!!)
                    }
                }
            }
            is Expr.Index -> {
                val d = this.data(e.col)
                val l = d?.second
                when {
                    (d == null) -> null
                    (l == null) -> null
                    (e.idx !is Expr.Tag) -> null
                    else -> {
                        val idx = l.indexOfFirst { it.first.str == e.idx.tk.str.drop(1) }
                        val v = if (idx == -1) null else l[idx]
                        when {
                            (v == null) -> {
                                err(e.idx.tk, "index error : undeclared data field ${e.idx.tk.str}")
                            }
                            (v.second == null) -> Pair(idx, null)
                            else -> {
                                Pair(idx, this.datas[v.second!!.str]!!)
                            }
                        }
                    }
                }
            }
            else -> null
        }
    }

    fun type (dcl: Expr.Dcl, src: Expr): Type {
        val blk = dcl.toblk()
        val up  = src.up_first { it is Expr.Proto || it==blk }
        //println(dcl)
        //println(src)
        return when {
            (blk.up == null) -> Type.GLOBAL
            (blk == up)    -> Type.LOCAL
            else -> {
                up as Expr.Proto
                val nst = up.up_all_until { it == blk }
                    .filter { it is Expr.Proto }
                    .let { it as List<Expr.Proto> }
                    .all { it.nst }
                when {
                    !nst -> Type.UPVAL
                    (up.tk.str == "func") -> Type.LOCAL
                    else -> Type.NESTED
                }
            }
        }
    }
}
