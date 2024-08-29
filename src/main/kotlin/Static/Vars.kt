package dceu

typealias LData = List<Id_Tag>

enum class Type {
    GLOBAL, LOCAL, NESTED, UPVAL
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

class Vars (val outer: Expr.Do, val ups: Ups) {
    val datas = mutableMapOf<String,LData>()

    public val nats: MutableMap<Expr.Nat,Pair<List<Expr.Dcl>,String>> = mutableMapOf()
    public val proto_to_upvs: MutableMap<Expr.Proto,MutableSet<Expr.Dcl>> = mutableMapOf()
    public val proto_has_outer: MutableSet<Expr.Proto> = mutableSetOf()

    init {
        this.outer.traverse()
    }

    fun data (e: Expr): Pair<Int?,LData?>? {
        return when (e) {
            is Expr.Acc -> {
                val dcl = ups.id_to_dcl(e.tk.str,e)!!
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
                    val task = ups.first_task_outer(e)
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
        val blk = ups.dcl_to_blk(dcl)
        val up  = ups.first(src) { it is Expr.Proto || it==blk }
        return when {
            (blk == outer) -> Type.GLOBAL
            (blk == up)    -> Type.LOCAL
            else -> {
                up as Expr.Proto
                this.proto_has_outer.add(up)
                val nst = ups.all_until(up) { it == blk }
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

    fun check (dcl: Expr.Dcl) {
        if (CEU>=99 && dcl.idtag.first.str=="it") {
            // ok
        } else {
            val xdcl = ups.id_to_dcl(dcl.idtag.first.str, dcl, false, { it==dcl })
            if (xdcl == null) {
                // ok
            } else {
                err(dcl.tk, "declaration error : variable \"${dcl.idtag.first.str}\" is already declared")
            }
        }
    }

    fun acc (e: Expr, id: String): Expr.Dcl {
        val dcl: Expr.Dcl? = ups.id_to_dcl(id, e)
        if (dcl == null) {
            err(e.tk, "access error : variable \"${id}\" is not declared")
        }

        // add upval to all protos upwards
        // stop at declaration (orig)
        // use blk bc of args
        if (type(dcl,e) == Type.UPVAL) {
            if (dcl.tk.str!="val" && dcl.tk.str!="val'") {
                err(e.tk, "access error : outer variable \"${dcl.idtag.first.str}\" must be immutable")
            }
            val orig = ups.first(ups.dcl_to_blk(dcl)) { it is Expr.Proto }
            //println(listOf(dcl.id.str, orig?.tk))
            val proto = ups.first(e) { it is Expr.Proto }!!
            ups.all_until(proto) { it == orig }
                .let {  // remove orig
                    if (orig==null) it else it.dropLast(1)
                }
                .filter { it is Expr.Proto }
                .forEach {
                    //println(listOf(dcl.id.str, it.tk))
                    this.proto_to_upvs[it]!!.add(dcl)
                }
        }

        return dcl
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                proto_to_upvs[this] = mutableSetOf()
                if (this.tag !=null && !datas.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
                this.pars.forEach { check(it) }
                this.blk.traverse()
            }
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Escape -> this.e?.traverse()
            is Expr.Group -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> {
                this.src?.traverse()
                check(this)
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.blk.traverse()
            is Expr.Data   -> {
                val sup = this.tk.str.dropLastWhile { it != '.' }.dropLast(1)
                if (datas.containsKey(this.tk.str)) {
                    err(this.tk, "data error : data ${this.tk.str} is already declared")
                }
                val ids = (datas[sup] ?: emptyList()) + this.ids
                val xids = ids.map { it.first.str }
                if (xids.size != xids.distinct().size) {
                    err(this.tk, "data error : found duplicate ids")
                }
                ids.forEach { (_,tag) ->
                    if (tag!=null && !datas.containsKey(tag.str)) {
                        err(tag, "data error : data ${tag.str} is not declared")
                    }
                }
                datas[this.tk.str] = ids
            }
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> this.blk.traverse()
            is Expr.Defer  -> this.blk.traverse()

            is Expr.Yield  -> this.e.traverse()
            is Expr.Resume -> { this.co.traverse() ; this.args.forEach { it.traverse() } }

            is Expr.Spawn  -> { this.tsks?.traverse() ; this.tsk.traverse() ; this.args.forEach { it.traverse() } }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }
            is Expr.Tasks  -> this.max.traverse()

            is Expr.Nat    -> {
                nats[this] = this.tk.str.let {
                    assert(!it.contains("XXX")) { "TODO: native cannot contain XXX"}
                    val set = mutableListOf<Expr.Dcl>()
                    var str = ""
                    var i = 0

                    var lin = 1
                    var col = 1
                    fun read (): Char {
                        //assert(i < it.length) { "bug found" }
                        if (i >= it.length) {
                            err(tk, "native error : (lin $lin, col $col) : unterminated token")
                        }
                        val x = it[i++]
                        if (x == '\n') {
                            lin++; col=0
                        } else {
                            col++
                        }
                        return x
                    }

                    while (i < it.length) {
                        val x1 = read()
                        str += if (x1 != '$') x1 else {
                            val (l,c) = Pair(lin,col)
                            var id = ""
                            var no = ""
                            while (i < it.length) {
                                val x2 = read()
                                if (x2.isLetterOrDigit() || x2=='_' || x2=='-') {
                                    id += x2
                                } else {
                                    no += x2
                                    break
                                }
                            }
                            if (id.length == 0) {
                                err(tk, "native error : (lin $l, col $c) : invalid identifier")
                            }
                            val dcl = acc(this, id)
                            set.add(dcl)
                            "(XXX)$no"
                        }
                    }
                    Pair(set, str)
                }
            }
            is Expr.Acc    -> acc(this, this.tk.str)
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach { it.traverse() }
            is Expr.Vector -> this.args.forEach { it.traverse() }
            is Expr.Dict   -> this.args.forEach { it.first.traverse() ; it.second.traverse() }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()
                data(this)
            }
            is Expr.Call   -> { this.clo.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
