package dceu

typealias LData = List<Id_Tag>

enum class Type {
    GLOBAL, LOCAL, PARAM, NESTED, UPVAL
}

class Vars (val outer: Expr.Do, val ups: Ups) {
    val datas = mutableMapOf<String,LData>()

    // enc (enclosure) = Dcl or Proto
    private val dcls: MutableList<Expr.Dcl> = GLOBALS.map {
        Expr.Dcl (
            Tk.Fix("val", outer.tk.pos),
            Pair(Tk.Id(it,outer.tk.pos,0), null),
            null
        )
    }.toMutableList()
    public  val dcl_to_blk: MutableMap<Expr.Dcl,Expr.Do> = dcls.map {
        Pair(it, outer)
    }.toMap().toMutableMap()
    public val acc_to_dcl: MutableMap<Expr.Acc,Expr.Dcl> = mutableMapOf()
    public val blk_to_dcls: MutableMap<Expr.Do,MutableList<Expr.Dcl>> = mutableMapOf(
        Pair(outer, dcls.toList().toMutableList())
    )
    public val nats: MutableMap<Expr.Nat,Pair<List<Expr.Dcl>,String>> = mutableMapOf()
    public val proto_to_upvs: MutableMap<Expr.Proto,MutableSet<Expr.Dcl>> = mutableMapOf()

    init {
        this.outer.traverse()
    }

    fun data (e: Expr): Pair<Int?,LData?>? {
        return when (e) {
            is Expr.Acc -> {
                val dcl = acc_to_dcl[e]!!
                dcl.idtag.second.let {
                    if (it == null) {
                        null
                    } else {
                        Pair(null, this.datas[it.str])
                    }
                }
            }
            is Expr.Pub -> {
                when {
                    (e.tsk == null) -> {
                        val task = ups.first_task_outer(e)
                        if (task?.tag == null) null else {
                            Pair(null, this.datas[task.tag.str]!!)
                        }
                    }
                    (e.tsk != null) -> {
                        this.data(e.tsk)
                    }
                    else -> error("impossible case")
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
                                error("unreachable")
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

    fun type (dcl: Expr, src: Expr): Type {
        val blk = dcl_to_blk[dcl]!!
        val xups = ups.all_until(src) { it == blk } // all ups between src -> dcl
        return when {
            (blk == outer) -> Type.GLOBAL
            xups.any { it is Expr.Proto } -> Type.UPVAL
            (ups.pub[blk] is Expr.Proto) -> Type.PARAM
            //xups.all { it !is Expr.Proto || ups.isnst(it) || it==blk } -> Type.NESTED
            else -> Type.LOCAL
        }
    }

    fun acc (e: Expr, id: String): Expr.Dcl {
        val dcl: Expr.Dcl? = dcls.findLast { it.idtag.first.str==id } as Expr.Dcl?
        if (dcl == null) {
            err(e.tk, "access error : variable \"${id}\" is not declared")
        }
        dcl!!

        // add upval to all protos upwards
        // stop at declaration (orig)
        // use blk bc of args
        if (type(dcl,e) == Type.UPVAL) {
            if (dcl.tk.str != "val") {
                err(e.tk, "access error : outer variable \"${dcl.idtag.first.str}\" must be immutable")
            }
            val orig = ups.first(dcl_to_blk[dcl]!!) { it is Expr.Proto }
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

        if (e is Expr.Acc) {        // TODO: what about Expr.Nat?
            acc_to_dcl[e] = dcl
        }
        return dcl
    }

    fun idx (acc: Expr.Acc): String {
        val dcl = this.acc_to_dcl[acc]!!
        return this.idx(dcl, acc)
    }
    fun idx (dcl: Expr.Dcl, src: Expr): String {
        val enc  = this.dcl_to_blk[dcl]!!

        // number of upvals in enclosing proto
        // index of upval for dcl
        // (ignore if -1 not access to upval)
        /*
        val proto_src = ups.first(src) { it is Expr.Proto }
        val (upvs,upv) = if (proto_src == null) Pair(0,-1) else {
            //println(proto_to_upvs[proto_src])
            proto_to_upvs[proto_src]!!.let {
                Pair(it.size, it.indexOf(Pair(dcl,ii)))
            }
        }
         */
        //println(listOf(upvs, upv, src.tostr()))

        val id = dcl.idtag.first.str.idc()
        //println(listOf(src.tk.pos.lin, id, type(dcl,src)))
        return when (type(dcl,src)) {
            Type.GLOBAL -> "ceu_glb_$id"
            Type.LOCAL -> "ceu_loc_$id"
            Type.PARAM -> "ceu_par_$id"
            /*
            Type.NESTED -> {
                val xups = ups.all_until(src) { it == enc } // all ups between src -> dcl
                val n = xups.count { it is Expr.Proto && it!=enc }
                val XX = "$X${"->exe->clo.Dyn->Clo_Task.up_tsk->X".repeat(n)}"
                val (_,idx) = this.idx(XX,dcl,ii,if (enc is Expr.Proto) enc.blk else dcl)
                //println(listOf(n,id,XX,idx,dcl,src))
                Pair("$XX->S /* nested */", idx)
            }
            Type.UPVAL -> {
                Pair("$X->S", "($X->clo + 1 + $X->args + $upv) /* upval $id */")
            }
             */
            else -> "ceu_upv_$id"
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                proto_to_upvs[this] = mutableSetOf()
                if (this.tag !=null && !datas.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
                this.blk.traverse()
            }
            is Expr.Do     -> {
                //val proto = ups.first(this) { it is Expr.Proto } as Expr.Proto
                blk_to_dcls[this] = mutableListOf()
                //println(listOf(this.tk,dcls.size,enc_to_base[proto]))

                // X. restore this size after nested block
                val size = dcls.size

                // func (a,b,...) { ... }
                val proto = ups.pub[this]
                if (proto is Expr.Proto) {
                    proto.pars.forEach { (id, tag) ->
                        val dcl = Expr.Dcl(
                            Tk.Fix("val", this.tk.pos),
                            Pair(id,tag), null
                        )
                        dcls.add(dcl)
                        dcl_to_blk[dcl] = this
                        blk_to_dcls[this]!!.add(dcl)
                    }
                }

                // nest into expressions
                this.es.forEach { it.traverse() }

                // X. restore size
                repeat(dcls.size - size) {
                    dcls.removeLast()
                }
            }
            is Expr.Dcl    -> {
                val (id,_) = this.idtag
                val prv = dcls.firstOrNull { it.idtag.first.str == id.str }
                when {
                    (prv == null) -> {}
                    (CEU>=99 && id.str=="it") -> {}
                    else -> {
                        err(id, "declaration error : variable \"${id.str}\" is already declared")
                    }
                }

                val blk = ups.first(this) { it is Expr.Do }!! as Expr.Do
                dcls.add(this)
                dcl_to_blk[this] = blk
                blk_to_dcls[blk]!!.add(this)

                this.idtag.second.let {
                    if (it !=null && !datas.containsKey(it.str)) {
                        //err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                    }
                }

                this.src?.traverse()
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.blk.traverse()
            is Expr.Break  -> { this.cnd.traverse() ; this.e?.traverse() }
            is Expr.Skip   -> this.cnd.traverse()
            is Expr.Enum   -> {}
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
            is Expr.Pass   -> this.e.traverse()

            is Expr.Catch  -> { this.cnd.traverse() ; this.blk.traverse() }
            is Expr.Defer  -> this.blk.traverse()

            is Expr.Yield  -> this.arg.traverse()
            is Expr.Resume -> { this.co.traverse() ; this.args.forEach { it.traverse() } }

            is Expr.Spawn  -> { this.tsks?.traverse() ; this.tsk.traverse() ; this.args.forEach { it.traverse() } }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }

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
