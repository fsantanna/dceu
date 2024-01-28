package dceu

typealias LData = List<Pair<Tk.Id,Tk.Tag?>>

class Vars (val outer: Expr.Do, val ups: Ups) {
    val datas = mutableMapOf<String,LData>()

    // allow it to be redeclared as long as it is not accessed
    private val it_uses: MutableMap<Expr.Dcl,Expr> = mutableMapOf()
        // Acc = previous use
        // Dcl = previous hide without previous use

    private val dcls: MutableList<Expr.Dcl> = (GLOBALS.first + GLOBALS.second)
        .map {
            Expr.Dcl (
                Tk.Fix("val", outer.tk.pos),
                Tk.Id(it, outer.tk.pos),
                null, true, null
            )
        }.toMutableList()
    public val dcl_to_blk: MutableMap<Expr.Dcl,Expr.Do> = dcls.map {
        Pair(it, outer)
    }.toMap().toMutableMap()
    public val acc_to_dcl: MutableMap<Expr.Acc,Expr.Dcl> = mutableMapOf()
    public val blk_to_dcls: MutableMap<Expr.Do,MutableList<Expr.Dcl>> = mutableMapOf(
        Pair(outer, dcls.toList().toMutableList())
    )
    public val nats: MutableMap<Expr.Nat,String> = mutableMapOf()
    public val proto_to_upvs: MutableMap<Expr.Proto,MutableSet<Expr.Dcl>> = mutableMapOf()

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

    fun isup (dcl: Expr.Dcl, src: Expr): Boolean {
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
        val blk = dcl_to_blk[dcl]!!
        return (blk!=outer) && ups.all_until(src) { it == blk }.any { it is Expr.Proto }
    }

    fun acc (e: Expr, id: String): Expr.Dcl {
        val dcl = dcls.findLast { id == it.id.str } // last bc of it redeclaration
        if (dcl == null) {
            err(e.tk, "access error : variable \"${id}\" is not declared")
        }
        dcl!!

        if (isup(dcl,e)) {
            val proto = ups.first(e) { it is Expr.Proto }!!
            this.proto_to_upvs[proto]!!.add(dcl)
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
        return blk_to_dcls[blk]!!.findLast { it.id.str == id }!!
    }

    fun idx (acc: Expr.Acc): String {
        return this.idx(this.acc_to_dcl[acc]!!, acc)
    }
    fun idx (dcl: Expr.Dcl, src: Expr): String {
        // Use ups[blk] instead of ups[dcl]
        //  - some dcl are args
        //  - dcl args are created after ups

        val isarg = (ups.pub[dcl] == null)
        val blk = this.dcl_to_blk[dcl]!!

        // index of dcl inside its block
        // (ignore if access to upval)
        val idx = 1 + this.blk_to_dcls[blk]!!.lastIndexOf(dcl)
            // +1 = block sentinel
        assert(idx > 0)

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
        val proto_blk = ups.first(blk) { it is Expr.Proto }
        val blks = ups.all_until(blk) { it == proto_blk }
            //.let { println(it) ; it }
            .drop(1)    // myself
            .filter { it is Expr.Do }
            .map { 1 + this.blk_to_dcls[it]!!.count() }
            .sum()  // +1 = block sentinel
        //println(listOf(dcl.id.str,off,idx))

        return when {
            isup(dcl,src) -> {                // upval
                "(ceu_base + $upv) /* upval */"
            }
            (proto_blk == null) -> {        // global
                "($blks + $idx) /* global */"
            }
            isarg -> {                      // argument
                assert(blks == 0)
                // -1 = arguments are before the block sentinel
                "(ceu_base + $upvs + -1 + $idx) /* arg */"
            }
            else -> {                       // local
                "(ceu_base + $upvs + $blks + $idx) /* local */"
            }
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                proto_to_upvs[this] = mutableSetOf()
                if (this.tag!=null && this.tag.str!=":void" && !datas.containsKey(this.tag.str)) {
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
                this.blk.traverse()
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
                if (this != outer) {
                    blk_to_dcls[this] = mutableListOf()
                }
                val size = dcls.size    // restore this size after nested block

                // func (a,b,...) { ... }
                val proto = ups.pub[this]
                if (proto is Expr.Proto) {
                    proto.args.forEach { (id, tag) ->
                        val dcl = Expr.Dcl(
                            Tk.Fix("val", this.tk.pos),
                            id, /*false,*/  tag, true, null
                        )
                        dcls.add(dcl)
                        dcl_to_blk[dcl] = this
                        blk_to_dcls[this]!!.add(dcl)
                    }
                }

                // nest into expressions
                this.es.forEach { it.traverse() }

                // do not remove ids listed in outer export
                if (ups.pub[this] !is Expr.Export) {
                    repeat(dcls.size - size) {
                        dcls.removeLast()
                    }
                }
            }
            is Expr.Dcl    -> {
                this.src?.traverse()

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
                dcl_to_blk[this] = blk
                blk_to_dcls[blk]!!.add(this)

                if (this.tag !=null && !datas.containsKey(this.tag.str)) {
                    //err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
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
                            "(ceu_x_peek($x))$no"
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
