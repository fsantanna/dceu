package dceu

typealias LData = List<Pair<Tk.Id,Tk.Tag?>>

fun Expr.Do.ismem (sta: Static, clos: Clos): Boolean {
    return sta.exes.contains(this) || /*sta.ylds.contains(this) ||*/ clos.blks_nested.contains(this)
}

fun String.idc (n: Int? = null): String {
    return when {
        (this[0] == '{') -> {
            val MAP = mapOf(
                Pair('+', "plus"),
                Pair('-', "minus"),
                Pair('*', "asterisk"),
                Pair('/', "slash"),
                Pair('>', "greater"),
                Pair('<', "less"),
                Pair('=', "equals"),
                Pair('!', "exclaim"),
                Pair('|', "bar"),
                Pair('&', "ampersand"),
                Pair('#', "hash"),
            )
            "op_" + this.drop(2).dropLast(2).toList().map { MAP[it] }.joinToString("_")
        }
        else -> {
            val MAP = mapOf(
                Pair('.', "_dot_"),
                Pair('-', "_dash_"),
                Pair('\'', "_plic_"),
                Pair('?', "_question_"),
                Pair('!', "_bang_"),
            )
            "id_" + this.toList().map { MAP[it] ?: it }.joinToString("")
        }
    }.let {
        it + (if (n==null) "" else "_" + n)
    }
}

class Vars (val outer: Expr.Do, val ups: Ups) {
    val datas = mutableMapOf<String,LData>()

    // allow it to be redeclared as long as it is not accessed
    private val it_uses: MutableMap<Expr.Dcl,Expr> = mutableMapOf()
        // Acc = previous use
        // Dcl = previous hide without previous use

    private val dcls: MutableList<Expr.Dcl> = GLOBALS.map {
        Expr.Dcl (
            Tk.Fix("val", outer.tk.pos),
            Tk.Id(it,outer.tk.pos,0),
            null, true, null
        )
    }.toMutableList()
    public  val dcl_to_blk: MutableMap<Expr.Dcl,Expr.Do> = dcls.map {
        Pair(it, outer)
    }.toMap().toMutableMap()
    private val acc_to_dcl: MutableMap<Expr.Acc,Expr.Dcl> = mutableMapOf()
    public  val blk_to_dcls: MutableMap<Expr.Do,MutableList<Expr.Dcl>> = mutableMapOf(
        Pair(outer, dcls.toList().toMutableList())
    )
    public val nats: MutableMap<Expr.Nat,Pair<List<Expr.Dcl>,String>> = mutableMapOf()

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

    fun find (e: Expr, id: String, upv: Int): Expr.Dcl {
        val dcl = dcls.findLast { id == it.id.str }
        when {
            (dcl == null) -> err(e.tk, "access error : variable \"${id}\" is not declared")
            (upv==0 && dcl.id.upv==1) -> err(e.tk, "access error : incompatible upval modifier")
            (upv >0 && dcl.id.upv==0) -> err(e.tk, "access error : incompatible upval modifier")
            (upv == 2) -> {
                val nocross = dcl_to_blk[dcl].let { blk ->
                    ups.all_until(e) { it == blk }.none { it is Expr.Proto }
                }
                if (nocross) {
                    err(e.tk, "access error : unnecessary upref modifier")
                }
            }
        }
        return dcl!!
    }

    fun get (acc: Expr.Acc): Pair<Expr.Do,Expr.Dcl> {
        val dcl = acc_to_dcl[acc]!!
        return Pair(dcl_to_blk[dcl]!!, dcl)
    }

    fun get (blk: Expr.Do, id: String): Expr.Dcl {
        return blk_to_dcls[blk]!!.findLast { it.id.str == id }!!
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
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
                blk_to_dcls[this] = mutableListOf()
                val size = dcls.size    // restore this size after nested block

                // func (a,b,...) { ... }
                val proto = ups.pub[this]
                if (proto is Expr.Proto) {
                    proto.args.forEach { (id, tag) ->
                        val dcl1 = Expr.Dcl(
                            Tk.Fix("val", this.tk.pos),
                            id, /*false,*/  tag, true, null
                        )
                        val dcl2 = Expr.Dcl(
                            Tk.Fix("val", this.tk.pos),
                            Tk.Id("_${id.str}_", id.pos, id.upv),
                            /*false,*/
                            null, false, null
                        )
                        dcls.add(dcl1)
                        dcls.add(dcl2)
                        dcl_to_blk[dcl1] = this
                        dcl_to_blk[dcl2] = this
                        blk_to_dcls[this]!!.add(dcl1)
                        blk_to_dcls[this]!!.add(dcl2)
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
                    err(this.tk, "declaration error : variable \"${this.id.str}\" is already declared")
                }

                val blk = ups.first_block(this)!!
                dcls.add(this)
                dcl_to_blk[this] = blk
                blk_to_dcls[blk]!!.add(this)

                //val bup = ups.first_block(this)
                val bup = ups.first(this) {
                    it is Expr.Do && !ups.pub[it].let { it is Expr.Export && it.ids.any { it == this.id.str } }
                }!! as Expr.Do
                when {
                    (this.id.upv == 2) -> {
                        err(tk, "var error : cannot declare an upref")
                    }
                    (this.id.upv==1 && bup==outer) -> {
                        err(tk, "var error : cannot declare a global upvar")
                    }
                }

                if (this.tag !=null && !datas.containsKey(this.tag.str)) {
                    //err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop  -> this.blk.traverse()
            is Expr.Break -> { this.cnd.traverse() ; this.e?.traverse() }
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
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> { this.cnd.traverse() ; this.blk.traverse() }
            is Expr.Defer  -> this.blk.traverse()

            is Expr.Yield  -> this.arg.traverse()
            is Expr.Resume -> { this.co.traverse() ; this.arg.traverse() }

            is Expr.Spawn  -> { this.tsks?.traverse() ; this.tsk.traverse() ; this.arg.traverse() }
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Dtrack -> { this.tsk.traverse() ; this.blk.traverse() }
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
                            val dcl = find(this, id, 0)
                            set.add(dcl)
                            "(XXX)$no"
                        }
                    }
                    Pair(set, str)
                }
            }
            is Expr.Acc    -> {
                val dcl = find(this, this.tk.str, this.tk_.upv)
                acc_to_dcl[this] = dcl

                if (CEU>=99 && dcl.id.str=="it") {
                    val prv = it_uses[dcl]
                    when (prv) {
                        is Expr.Dcl -> err(prv.tk, "declaration error : variable \"${prv.id.str}\" is already declared")
                        is Expr.Acc -> {}
                        else -> {
                            if (!this.ign) {
                                it_uses[dcl] = this        // ignore __acc
                            }
                        }
                    }
                }
            }
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
