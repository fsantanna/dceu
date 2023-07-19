package dceu

typealias LData = List<Pair<Tk.Id,Tk.Tag?>>

class Vars (val outer: Expr.Do, val ups: Ups) {
    val datas = mutableMapOf<String,LData>()

    private val dcls: MutableList<Expr.Dcl> = GLOBALS.map {
        Expr.Dcl (
            Tk.Fix("val", outer.tk.pos),
            Tk.Id(it,outer.tk.pos,0),
            false, null, true, null
        )
    }.toMutableList()
    public  val dcl_to_blk: MutableMap<Expr.Dcl,Expr.Do> = dcls.map {
        Pair(it, outer)
    }.toMap().toMutableMap()
    private val acc_to_dcl: MutableMap<Expr.Acc,Expr.Dcl> = mutableMapOf()
    public  val blk_to_dcls: MutableMap<Expr.Do,MutableList<Expr.Dcl>> = mutableMapOf(
        Pair(outer, dcls.toList().toMutableList())
    )
    public val nat_to_str: MutableMap<Expr.Nat,String> = mutableMapOf()

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
                    Pair(null, this.datas[dcl.tag.str]!!)
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

    fun id2c (dcl: Expr.Dcl, upv: Int): Pair<String,String> {
        val idc = dcl.id.str.id2c()
        return if (upv == 2) {
            Pair("(ceu_upvs->$idc)", "(ceu_upvs->_${idc}_)")
        } else {
            Pair(idc, "_${idc}_")
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                this.args.forEach { (_,tag) ->
                    if (tag!=null && !datas.containsKey(tag.str)) {
                        err(tag, "declaration error : data ${tag.str} is not declared")
                    }
                }

                this.body.traverse()
            }
            is Expr.Do     -> {
                blk_to_dcls[this] = mutableListOf()
                val size = dcls.size    // restore this size after nested block

                // func (a,b,...) { ... }
                val up = ups.pub[this]
                if (up is Expr.Proto) {
                    up.args.forEach { (id,tag) ->
                        val dcl1 = Expr.Dcl (
                            Tk.Fix("val", this.tk.pos),
                            id, /*false,*/ false, tag, true, null
                        )
                        val dcl2 = Expr.Dcl (
                            Tk.Fix("val", this.tk.pos),
                            Tk.Id("_${id.str}_",id.pos,id.upv),
                            /*false,*/
                            false, null, false, null
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

                repeat(dcls.size - size) {
                    dcls.removeLast()
                }
            }
            is Expr.Dcl    -> {
                this.src?.traverse()

                if (dcls.findLast { this.id.str == it.id.str } != null) {    // TODO
                    err(this.tk, "declaration error : variable \"${this.id.str}\" is already declared")
                }

                val blk = ups.first_block(this)!!
                dcls.add(this)
                dcl_to_blk[this] = blk
                blk_to_dcls[blk]!!.add(this)

                val bup = ups.first_block(this)
                when {
                    (this.id.upv == 2) -> {
                        err(tk, "var error : cannot declare an upref")
                    }
                    (this.id.upv==1 && bup==outer) -> {
                        err(tk, "var error : cannot declare a global upvar")
                    }
                }

                if (this.tag!=null && !datas.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.body.traverse()
            is Expr.Break -> {}
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

            is Expr.Nat    -> {
                nat_to_str[this] = this.tk.str.let {
                    var ret = ""
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
                        ret += if (x1 != '$') x1 else {
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
                            val blk = dcl_to_blk[dcl]!!
                            val (idx,_) = id2c(dcl, 0)
                            "($idx)$no"
                        }
                    }
                    ret
                }
            }
            is Expr.Acc    -> acc_to_dcl[this] = find(this, this.tk.str, this.tk_.upv)
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
            is Expr.Call   -> { this.closure.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
