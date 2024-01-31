package dceu

import kotlin.math.max

typealias LData = List<Pair<Tk.Id,Tk.Tag?>>

class Vars (val outer: Expr.Call, val ups: Ups) {
    val datas = mutableMapOf<String,LData>()

    // allow it to be redeclared as long as it is not accessed
    private val it_uses: MutableMap<Expr.Dcl,Expr> = mutableMapOf()
        // Acc = previous use
        // Dcl = previous hide without previous use

    private val dcls: MutableList<Expr.Dcl> = mutableListOf()
    public val dcl_to_enc: MutableMap<Expr.Dcl,Expr> = dcls.map {
        Pair(it, outer)
    }.toMap().toMutableMap()
    public val acc_to_dcl: MutableMap<Expr.Acc,Expr.Dcl> = mutableMapOf()
    public val enc_to_dcls: MutableMap<Expr,MutableList<Expr.Dcl>> = mutableMapOf(
        Pair(outer, dcls.toList().toMutableList())
    )
    public val nats: MutableMap<Expr.Nat,String> = mutableMapOf()
    public val proto_to_upvs: MutableMap<Expr.Proto,MutableSet<Expr.Dcl>> = mutableMapOf()

    // proto_to_locs: max number of locals in proto
    //  - must allocate this space on call
    // enc_to_base: base stack index at beginning of block
    //  - must pop down to it on leave
    //  - proto base is required bc block must be relative to it
    public val proto_to_locs: MutableMap<Expr.Proto,Int> = mutableMapOf()
    public val enc_to_base: MutableMap<Expr,Int> = mutableMapOf()

    init {
        this.outer.traverse()
    }

    fun data (e: Expr): Pair<Int?,LData?>? {
        return when (e) {
            is Expr.Acc -> {
                val dcl = acc_to_dcl[e]!!
                if (dcl.tag == null) {
                    null
                } else {
                    Pair(null, this.datas[dcl.tag.str])
                }
            }
            is Expr.Pub -> {
                when {
                    (e.tsk == null) -> {
                        val task = ups.first_task_real(e)
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

    fun isupv (dcl: Expr.Dcl, src: Expr): Boolean {
        // 1. <src> -> <dcl> must cross proto
        // 2. but <dcl> must not be in outer block
        //  ...             ;; NO: not up (outer block)
        //  do {
        //      <dcl>       ;; OK: is up
        //      func () {
        //          ...     ;; NO: not up (same proto)
        //          <src>
        //      }
        //  }
        val enc = dcl_to_enc[dcl]!!
        return /*(enc!=outer) &&*/ ups.all_until(src) { it == enc }.any { it is Expr.Proto && it!=enc }
    }

    fun acc (e: Expr, id: String): Expr.Dcl {
        val dcl = dcls.findLast { id == it.id.str } // last bc of it redeclaration
        if (dcl == null) {
            err(e.tk, "access error : variable \"${id}\" is not declared")
        }
        dcl!!

        // add upval to all protos upwards
        // stop at declaration (orig)
        // use blk bc of args
        if (isupv(dcl,e)) {
            if (dcl.tk.str != "val") {
                err(e.tk, "access error : outer variable \"${dcl.id.str}\" must be immutable")
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
        if (CEU>=99 && dcl.id.str=="it") {
            val prv = it_uses[dcl]
            when (prv) {
                is Expr.Dcl -> err(prv.id, "declaration error : variable \"${prv.id.str}\" is already declared")
                is Expr.Acc -> {}
                else -> {
                    if (e is Expr.Acc && !e.ign) {
                        it_uses[dcl] = e        // ignore __acc
                    }
                }
            }
        }
        return dcl
    }

    fun get (blk: Expr.Do, id: String): Expr.Dcl {
        return enc_to_dcls[blk]!!.findLast { it.id.str == id }!!
    }

    fun idx (acc: Expr.Acc): String {
        return this.idx(this.acc_to_dcl[acc]!!, acc)
    }
    fun idx (dcl: Expr.Dcl, src: Expr): String {
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
        //println(listOf(dcl.id.str,upv))

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

        return when {
            isupv(dcl,src) -> {                // upval
                "(ceux.base + $upv) /* upval ${dcl.id.str} */"
            }
            (proto_blk == null) -> {        // global
                "($locs + $I) /* global ${dcl.id.str} */"
            }
            (enc is Expr.Proto) -> {        // argument
                assert(locs == 0)
                "ceux_arg(ceux, $I) /* arg ${dcl.id.str} */"
            }
            else -> {                       // local
                "(ceux.base + $upvs + $locs + $I) /* local ${dcl.id.str} */"
            }
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                enc_to_dcls[this] = mutableListOf()
                proto_to_upvs[this] = mutableSetOf()
                enc_to_base[this] = dcls.size
                if (this.tag !=null && this.tag.str!=":void" && !datas.containsKey(this.tag.str)) {
                    //err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
                this.args.forEach { (id,tag) ->
                    val prv = dcls.firstOrNull { id.str!="..." && id.str==it.id.str }
                    if (prv==null || (CEU>=99 && prv.id.str=="it" && it_uses[prv]==null)) {
                        // ok
                        if (CEU>=99 && prv!=null) {
                            it_uses[prv] = this // found new dcl w/o uses of prv dcl
                        }
                    } else {
                        err(id, "declaration error : variable \"${id.str}\" is already declared")
                    }
                    if (tag!=null && !datas.containsKey(tag.str)) {
                        //err(tag, "declaration error : data ${tag.str} is not declared")
                    }
                }
                this.args.forEach { (id, tag) ->
                    val dcl = Expr.Dcl(
                        Tk.Fix("val", this.tk.pos),
                        id, /*false,*/  tag, null
                    )
                    dcls.add(dcl)
                    dcl_to_enc[dcl] = this
                    enc_to_dcls[this]!!.add(dcl)
                }

                val base = dcls.size                                // 1. base before proto
                proto_to_locs[this] = 0                             // 2. blk.traverse max

                this.blk.traverse()

                proto_to_locs[this] = proto_to_locs[this]!! - base  // 3. then we subtract base from max

                repeat(this.args.size) {
                    dcls.removeLast()   // dropLast(n) copies the list
                }
            }
            is Expr.Export -> {
                val size = dcls.size
                this.blk.traverse()
                for (i in dcls.size-1 downTo size) {
                    if (!this.ids.contains(dcls[i].id.str)) {
                        dcls.removeAt(i)
                    }
                }
            }
            is Expr.Do     -> {
                val proto = ups.first(this) { it is Expr.Proto } as Expr.Proto
                enc_to_dcls[this] = mutableListOf()
                enc_to_base[this] = dcls.size - enc_to_base[proto]!!

                // X. restore this size after nested block
                val size = dcls.size

                // nest into expressions
                this.es.forEach { it.traverse() }

                // brefore (X)
                // max number of simultaneous locals in outer proto
                proto_to_locs[proto] = max(proto_to_locs[proto]!!, dcls.size)

                // X. restore size
                // do not remove ids listed in outer export
                if (ups.pub[this] !is Expr.Export) {
                    repeat(dcls.size - size) {
                        dcls.removeLast()
                    }
                }


            }
            is Expr.Dcl    -> {
                val prv = dcls.firstOrNull { this.id.str == it.id.str }
                if (prv==null || (CEU>=99 && prv.id.str=="it" && it_uses[prv]==null)) {
                    // ok
                    if (CEU>=99 && prv!=null) {
                        it_uses[prv] = this // found new dcl w/o uses of prv dcl
                    }
                } else {
                    err(this.id, "declaration error : variable \"${this.id.str}\" is already declared")
                }

                val blk = ups.first_block(this)!!
                dcls.add(this)
                dcl_to_enc[this] = blk
                enc_to_dcls[blk]!!.add(this)

                val proto = ups.first(this) { it is Expr.Proto } as Expr.Proto?
                if (proto != null) {
                    proto_to_locs[proto] = max(proto_to_locs[proto]!!, dcls.size)
                }

                if (this.tag !=null && !datas.containsKey(this.tag.str)) {
                    //err(this.tag, "declaration error : data ${this.tag.str} is not declared")
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
            is Expr.Resume -> { this.co.traverse() ; this.arg.traverse() }

            is Expr.Spawn  -> { this.tsks?.traverse() ; this.tsk.traverse() ; this.args.forEach { it.traverse() } }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Dtrack -> this.blk.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }

            is Expr.Nat    -> {
                nats[this] = this.tk.str.let {
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
                            val x = idx(dcl,this)
                            "(ceux_peek($x))$no"
                        }
                    }
                    //println(str)
                    str
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
