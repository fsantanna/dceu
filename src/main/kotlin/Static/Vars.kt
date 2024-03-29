package dceu

import kotlin.math.max

typealias LData = List<Pair<Tk.Id,Tk.Tag?>>

enum class Type {
    GLOBAL, LOCAL, ARG, NESTED, UPVAL
}

class Vars (val outer: Expr.Call, val ups: Ups) {
    val global = (outer.clo as Expr.Proto).blk
    val datas = mutableMapOf<String,LData>()

    private val dcls: MutableList<Expr> = mutableListOf()
    public val dcl_to_enc: MutableMap<Expr,Expr> = mutableMapOf()
    public val acc_to_dcl: MutableMap<Expr.Acc,Expr.Dcl> = mutableMapOf()
    public val enc_to_dcls: MutableMap<Expr,MutableList<Expr>> = mutableMapOf(
        Pair(outer, dcls.toList().toMutableList())
    )
    public val nats: MutableMap<Expr.Nat,Pair<List<Expr.Dcl>,String>> = mutableMapOf()
    public val proto_to_upvs: MutableMap<Expr.Proto,MutableSet<Expr.Dcl>> = mutableMapOf()

    // enc_to_base: base stack index at beginning of block
    //  - must pop down to it on leave
    //  - proto base is required bc block must be relative to it
    //      - proto also has upvs at bebinning
    public val enc_to_base: MutableMap<Expr,Int> = mutableMapOf()

    // Proto/Do
    //  - max number of simultaneous locals
    //  - only locals, does not include Proto args/upvs
    public val blk_to_locs: MutableMap<Expr.Do,Pair<Int,Int>> = mutableMapOf()
    // proto_to_locs: max number of locals in proto
    //  - must allocate this space on call

    init {
        this.outer.locs()
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
                            else -> Pair(idx, this.datas[v.second!!.str]!!)
                        }
                    }
                }
            }
            else -> null
        }
    }

    fun type (dcl: Expr, src: Expr): Type {
        val enc = dcl_to_enc[dcl]!!
        val xups = ups.all_until(src) { it == enc } // all ups between src -> dcl
        return when {
            (enc == global) -> Type.GLOBAL
            xups.all { it !is Expr.Proto } -> Type.LOCAL
            xups.all { it !is Expr.Proto || it==enc } -> Type.ARG
            xups.all { it !is Expr.Proto || ups.isnst(it) || it==enc } -> Type.NESTED
            else -> Type.UPVAL
        }.let {
            //println(listOf(src.tk.pos, src.tostr(), it))
            it
        }
    }

    fun acc (e: Expr, id: String): Expr.Dcl {
        val dcl = dcls.findLast { it is Expr.Dcl && id==it.idtag.first.str } as Expr.Dcl? // last bc of it redeclaration
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
            val orig = ups.first(dcl_to_enc[dcl]!!) { it is Expr.Proto }
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

    fun idx (X: String, acc: Expr.Acc): Pair<String,String> {
        return this.idx(X, this.acc_to_dcl[acc]!!, acc)
    }
    fun idx (X: String, def: Expr.Defer): Pair<String,String> {
        assert(X == "X")
        return this.idx(X, def, def)
    }
    fun idx (X: String, dcl: Expr, src: Expr): Pair<String,String> {
        val enc  = this.dcl_to_enc[dcl]!!
        val dcls = this.enc_to_dcls[enc]!!

        // (not used for upval)
        // index of dcl inside its blk/proto
        val I = dcls.lastIndexOf(dcl)
        assert(I != -1)

        // number of upvals in enclosing proto
        // index of upval for dcl
        // (ignore if -1 not access to upval)
        val proto_src = ups.first(src) { it is Expr.Proto }
        val (upvs,upv) = if (proto_src == null) Pair(0,-1) else {
            //println(proto_to_upvs[proto_src])
            proto_to_upvs[proto_src]!!.let {
                Pair(it.size, it.indexOf(dcl))
            }
        }
        //println(listOf(upvs, upv, src.tostr()))

        // enclosing proto of declaration block
        // all blocks in between declaration block and proto
        val proto_blk = ups.first(enc) { it is Expr.Proto }
        val locs = ups.all_until(enc) { it == proto_blk }
            //.let { println(it) ; it }
            .drop(1)    // myself
            .filter { it is Expr.Do }
            .map { this.enc_to_dcls[it]!!.count() }
            .sum()
        //println(listOf(dcl.id.str,off,idx))

        val id = when (dcl) {
            is Expr.Dcl -> dcl.idtag.first.str
            is Expr.Defer -> "defer"
            else -> error("impossible case")
        }
        return when (type(dcl,src)) {
            Type.GLOBAL -> {
                val s = if (CEU >= 3) "CEU_GLOBAL_X->S" else "$X->S"
                Pair(s, "(1 + $locs + $I) /* global $id */")
            }
            Type.LOCAL -> {
                Pair("$X->S", "($X->base + $upvs + $locs + $I) /* local $id */")
            }
            Type.ARG -> {
                assert(locs == 0)
                Pair("$X->S", "ceux_arg($X, $I) /* arg $id */")
            }
            Type.NESTED -> {
                val xups = ups.all_until(src) { it == enc } // all ups between src -> dcl
                val n = xups.count { it is Expr.Proto && it!=enc }
                val XX = "$X${"->exe->clo.Dyn->Clo_Task.up_tsk->X".repeat(n)}"
                val (_,idx) = this.idx(XX,dcl,if (enc is Expr.Proto) enc.blk else dcl)
                //println(listOf(n,id,XX,idx,dcl,src))
                Pair("$XX->S /* nested */", idx)
            }
            Type.UPVAL -> {
                Pair("$X->S", "($X->base + $upv) /* upval $id */")
            }
        }
    }

    fun Expr.locs (): Int {
        return when (this) {
            is Expr.Proto  -> this.blk.locs()
            is Expr.Do     -> {
                val n = this.es.count { it is Expr.Dcl || it is Expr.Defer }
                val nn = n + this.es.maxOf { it.locs() }
                blk_to_locs[this] = Pair(n, nn)
                //println(listOf(n, this.es.maxOf { it.locs() }, this))
                nn
            }
            is Expr.Export -> this.blk.locs()
            is Expr.Dcl    -> (this.src?.locs() ?: 0)
            is Expr.Set    -> this.dst.locs() + this.src.locs()
            is Expr.If     -> this.cnd.locs() + max(this.t.locs(), this.f.locs())
            is Expr.Loop   -> this.blk.locs()
            is Expr.Break  -> this.cnd.locs() + (this.e?.locs() ?: 0)
            is Expr.Skip   -> this.cnd.locs()
            is Expr.Pass   -> this.e.locs()
            is Expr.Catch  -> this.cnd.locs() + this.blk.locs()
            is Expr.Defer  -> this.blk.locs()
            is Expr.Yield  -> this.arg.locs()
            is Expr.Resume -> this.co.locs() + this.arg.locs()
            is Expr.Spawn  -> (this.tsks?.locs() ?: 0) + this.tsk.locs() + this.args.sumOf { it.locs() }
            is Expr.Pub    -> this.tsk?.locs() ?: 0
            is Expr.Toggle -> this.tsk.locs() + this.on.locs()
            is Expr.Tuple  -> this.args.sumOf { it.locs() }
            is Expr.Vector -> this.args.sumOf { it.locs() }
            is Expr.Dict   -> this.args.sumOf { it.first.locs() ; it.second.locs() }
            is Expr.Index  -> this.col.locs() + this.idx.locs()
            is Expr.Call   -> this.clo.locs() + this.args.sumOf { it.locs() }
            is Expr.Enum, is Expr.Data, is Expr.Delay, is Expr.Nat, is Expr.Acc -> 0
            is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> 0
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                enc_to_dcls[this] = mutableListOf()
                proto_to_upvs[this] = mutableSetOf()
                if (this.tag !=null && !datas.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
                this.args.forEach { (id,tag) ->
                    val prv = dcls.firstOrNull { id.str!="..." && it is Expr.Dcl && id.str==it.idtag.first.str } as Expr.Dcl?
                    if (prv==null || (CEU>=99 && prv.idtag.first.str=="it")) {
                        // ok
                    } else {
                        err(id, "declaration error : variable \"${id.str}\" is already declared")
                    }
                    if (tag!=null && !datas.containsKey(tag.str)) {
                        //err(tag, "declaration error : data ${tag.str} is not declared")
                    }
                }
                this.args.forEach {
                    val dcl = Expr.Dcl(
                        Tk.Fix("val", this.tk.pos),
                        it, null
                    )
                    dcls.add(dcl)
                    dcl_to_enc[dcl] = this
                    enc_to_dcls[this]!!.add(dcl)
                }

                val base = dcls.size                                // 1. base before proto
                enc_to_base[this] = base                            //    and after args
                //println(listOf("xxx", base))

                this.blk.traverse()

                repeat(this.args.size) {
                    dcls.removeLast()   // dropLast(n) copies the list
                }
            }
            is Expr.Export -> {
                val size = dcls.size
                this.blk.traverse()
                for (i in dcls.lastIndex downTo size) {
                    val dcl = dcls[i]
                    if (dcl is Expr.Dcl) {
                        if (!this.ids.contains(dcl.idtag.first.str)) {
                            dcls.removeAt(i)
                        }
                    }
                }
            }
            is Expr.Do     -> {
                val proto = ups.first(this) { it is Expr.Proto } as Expr.Proto
                enc_to_dcls[this] = mutableListOf()
                //println(listOf(this.tk,dcls.size,enc_to_base[proto]))

                ups.first(ups.pub[this]!!) { it is Expr.Do || it is Expr.Proto }.let {
                    enc_to_base[this] = if (it==null || it is Expr.Proto) 0 else {
                        //println(listOf("yyy", enc_to_base[it], enc_to_locs[it], this, it))
                        (enc_to_base[it]!! + blk_to_locs[it]!!.first)
                    }
                }
                //println(listOf(enc_to_base[this], dcls.size - enc_to_base[proto]!!, dcls.size, enc_to_base[proto]))
                //enc_to_base[this] = dcls.size - enc_to_base[proto]!!
                //println(listOf("yyy", enc_to_base[this]))

                // X. restore this size after nested block
                val size = dcls.size

                // nest into expressions
                this.es.forEach { it.traverse() }

                // X. restore size
                // do not remove ids listed in outer export
                if (ups.pub[this] !is Expr.Export) {
                    repeat(dcls.size - size) {
                        dcls.removeLast()
                    }
                }


            }
            is Expr.Dcl    -> {
                val prv = dcls.firstOrNull { it is Expr.Dcl && this.idtag.first.str==it.idtag.first.str } as Expr.Dcl?
                if (prv==null || (CEU>=99 && prv.idtag.first.str=="it")) {
                    // ok
                } else {
                    err(this.idtag.first, "declaration error : variable \"${this.idtag.first.str}\" is already declared")
                }

                val blk = ups.first(this) { it is Expr.Do }!!
                dcls.add(this)
                dcl_to_enc[this] = blk
                enc_to_dcls[blk]!!.add(this)

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
            is Expr.Defer  -> {
                val blk = ups.first(this) { it is Expr.Do }!!
                dcls.add(this)
                dcl_to_enc[this] = blk
                enc_to_dcls[blk]!!.add(this)
                this.blk.traverse()
            }

            is Expr.Yield  -> this.arg.traverse()
            is Expr.Resume -> { this.co.traverse() ; this.arg.traverse() }

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
            is Expr.Tuple  -> this.args.forEach{ it.traverse() }
            is Expr.Vector -> this.args.forEach{ it.traverse() }
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
