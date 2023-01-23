// func (args) or block (locals)
data class XBlock (val syms: MutableMap<String,Dcl>, val defers: MutableList<Pair<Int,String>>)    // Triple<n,code>
data class Dcl (val id: String, val tmp: Boolean, val tag: String?, val init: Boolean, val upv: Int, val blk: Expr.Do)    // blk = [Block,Group,Proto]

class Ups (val outer: Expr.Do) {
    val xblocks = mutableMapOf<Expr,XBlock> (
        Pair (
            outer,
            XBlock(GLOBALS.map { Pair(it,Dcl(it,false, null, true,0,outer)) }.toMap().toMutableMap(), mutableListOf())
        )
    )
    val evts: MutableMap<Expr.EvtErr, String?> = mutableMapOf()
    val tags: MutableMap<String,Triple<String,String,String?>> = TAGS.map { Pair(it,Triple(it, it.tag2c(), null)) }.toMap().toMutableMap()
    val tplates = mutableMapOf<String,List<Pair<Tk.Id,Tk.Tag?>>>()
    val ups = outer.tree()

    fun add_tag (tk: Tk, id: String, c: String, enu: String?) {
        if (tags.containsKey(id)) {
            // already there
        } else {
            val issub = id.contains('.')
            val sup = id.dropLastWhile { it != '.' }.dropLast(1)
            if (issub && !tags.containsKey(sup)) {
                err(tk, "tag error : parent tag $sup is not declared")
            }
            tags[id] = Triple(id, c, enu)
        }
    }

    // Protos that cannot be closures:
    //  - they access at least 1 free var w/o upval modifiers
    //  - for each var access ACC, we get its declaration DCL in block BLK
    //      - if ACC/DCL have no upval modifiers
    //      - we check if there's a func FUNC in between ACC -> [FUNC] -> BLK
    val upvs_protos_noclos = mutableSetOf<Expr>()

    // Upvars (var ^up) with refs (^^up):
    //  - at least one refs access the var
    //  - for each var access ACC, we get its declaration DCL and set here
    //  - in another round (Code), we assert that the DCL appears here
    //  - TODO: can also be used to warn for unused normal vars
    val upvs_vars_refs = mutableSetOf<Dcl>()

    // Set of uprefs within protos:
    //  - for each ^^ACC, we get the enclosing PROTOS and add ACC.ID to them
    val upvs_protos_refs = mutableMapOf<Expr.Proto,MutableSet<String>>()

    // funcs that set vars in enclosing tasks
    //  - they are marked and cannot receive "evt"
    //  - otherwise, we do not check with ceu_block_set
    val funcs_vars_tasks = mutableSetOf<Expr.Proto>()

    init {
        this.outer.traverse()
    }

    fun all_until (e: Expr, cnd: (Expr)->Boolean): List<Expr> {
        val up = ups[e]
        return when {
            cnd(e) -> listOf(e)
            (up == null) -> emptyList()
            else -> this.all_until(up,cnd).let { if (it.isEmpty()) it else it+e }
        }
    }
    fun hasfirst (e: Expr, cnd: (Expr)->Boolean): Boolean {
        return this.first(e,cnd) != null
    }
    fun first (e: Expr, cnd: (Expr)->Boolean): Expr? {
        return this.all_until(e,cnd).firstOrNull()
    }
    fun first_block (e: Expr): Expr.Do? {
        return this.first(e) { it is Expr.Do && it.isnest && it.ishide } as Expr.Do?
    }
    fun first_proto_or_block (e: Expr): Expr? {
        return this.first(e) { it is Expr.Proto || (it is Expr.Do && it.isnest && it.ishide) }
    }
    fun intask (e: Expr): Boolean {
        return this.first(e) { it is Expr.Proto }.let { (it!=null && it.tk.str!="func") }
    }
    fun first_true_x (e: Expr, x: String): Expr.Proto? {
        return this.first(e) {
            it is Expr.Proto && it.tk.str==x && (it.task==null || !it.task.second)
        } as Expr.Proto?
    }
    fun true_x_c (e: Expr, str: String): String? {
        val x = this.first_true_x(e, str)
        val n = this     // find first non fake
            .all_until(e) { it == x }
            .filter { it is Expr.Proto } // but count all protos in between
            .count()
        return if (n == 0) null else "(ceu_frame${"->proto->up_frame".repeat(n-1)})"
    }

    fun getDcl (e: Expr, id: String): Dcl? {
        val up = this.ups[e]
        val dcl = this.xblocks[e]?.syms?.get(id)
        return when {
            (dcl != null) -> dcl
            (up == null) -> null
            else -> this.getDcl(up, id)
        }
    }
    fun assertIsNotDeclared (e: Expr, id: String, tk: Tk) {
        if (this.getDcl(e,id)!=null && id!="evt") {
            err(tk, "declaration error : variable \"$id\" is already declared")
        }
    }
    fun assertIsDeclared (e: Expr, v: Pair<String,Int>, tk: Tk): Dcl {
        val (id,upv) = v
        val dcl = this.getDcl(e,id)
        val nocross = dcl?.blk.let { blk ->
            (blk == null) || this.all_until(e) { it==blk }.none { it is Expr.Proto }
        }
        return when {
            (dcl == null) -> {
                val l = id.split('-')
                val x = l
                    .mapIndexed { i,_ ->
                        l.drop(i).scan(emptyList<String>()) { acc, s -> acc + s }
                    }
                    .flatten()
                    .filter { it.size>0 && it.size<l.size }
                    .map { it.joinToString("-") }
                val amb = x.firstOrNull { this.getDcl(e,it) != null }
                if (amb != null) {
                    err(tk, "access error : \"${id}\" is ambiguous with \"${amb}\"") as Dcl
                } else {
                    err(tk, "access error : variable \"${id}\" is not declared") as Dcl
                }
            }
            (dcl.upv==0 && upv>0 || dcl.upv==1 && upv==0) -> err(tk, "access error : incompatible upval modifier") as Dcl
            (upv==2 && nocross) -> err(tk, "access error : unnecessary upref modifier") as Dcl
            else -> dcl
        }
    }

    fun tpl_is (e: Expr.Index): Boolean {
        val id = e.col.tk.str
        val dcl = getDcl(e, id)
        return when (e.col) {
            is Expr.Pub -> when (e.col.x) {
                // task.pub -> task (...) :T {...}
                is Expr.Self -> (e.idx is Expr.Tag) && (this.first_true_x(e,"task") != null)
                // x.pub -> x:T
                is Expr.Acc -> (e.idx is Expr.Tag) && (getDcl(e, e.col.x.tk.str)!!.tag != null)
                // x.y.pub -> x.y?
                is Expr.Index -> this.tpl_is(e.col.x)
                // detrack(x).pub
                is Expr.Call -> false   // TODO
                else -> error("impossible case")
            }
            is Expr.EvtErr -> (e.idx is Expr.Tag) && (dcl != null) && (evts[e.col] != null)
            is Expr.Acc    -> (e.idx is Expr.Tag) && (dcl!!.tag != null)
            is Expr.Index  -> this.tpl_is(e.col)
            else           -> false
        }
    }
    fun tpl_lst (e: Expr.Index): List<Pair<Tk.Id, Tk.Tag?>> {
        val id = e.col.tk.str
        return when {
            (e.col is Expr.Pub) -> when (e.col.x) {
                is Expr.Self -> {
                    // task.pub -> task (...) :T {...}
                    val tag = this.first_true_x(e,"task")!!.task!!.first!!.str
                    this.tplates[tag]!!
                }
                is Expr.Acc -> {
                    // x.pub -> x:T
                    val tag = getDcl(e, e.col.x.tk.str)!!.tag!!
                    this.tplates[tag]!!
                }
                is Expr.Index -> {
                    // x.y.pub -> x.y?
                    this.tpl_is(e.col.x)
                    TODO()
                }
                else -> error("impossible case")
            }
            (e.col is Expr.EvtErr) -> {
                val tag = evts[e.col]!!
                this.tplates[tag]!!
            }
            (e.col is Expr.Acc) -> {
                val dcl = getDcl(e, id)!!
                this.tplates[dcl.tag]!!
            }
            (e.col is Expr.Index) -> {
                e.col.idx as Expr.Tag
                val xid = e.col.idx.tk.str.drop(1)
                val lst = this.tpl_lst(e.col)
                val id_tag = lst.firstOrNull { it.first.str==xid }!!
                if (id_tag.second == null) {
                    err(e.idx.tk, "index error : field \"$xid\" is not a data")
                }
                this.tplates[id_tag.second!!.str]!!
            }
            else -> error("impossible case")
        }
    }

    // Traverse the tree structure from top down
    // 1. assigns this.xblocks
    // 2. assigns this.upvs_protos_noclos, upvs_vars_refs, upvs_protos_refs
    // 3. compiles all proto uprefs
    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> {
                if (this.task!=null && this.task.first!=null && !tplates.containsKey(this.task.first!!.str)) {
                    val tag = this.task.first!!
                    err(tag, "declaration error : data ${tag.str} is not declared")
                }
                this.body.traverse()
            }
            is Expr.Do -> {
                if (this!=outer && this.ishide) {
                    val proto = ups[this]
                    val args = if (proto !is Expr.Proto) {
                        mutableMapOf()
                    } else {
                        proto.args.let {
                            (it.map { (id,tag) ->
                                Pair(id.str, Dcl(id.str, false, tag?.str, true, id.upv, this))
                            } + it.map { (id,_) ->
                                Pair("_${id.str}_", Dcl("_${id.str}_", false, null, false, id.upv, this))
                            })
                        }.toMap().toMutableMap()
                    }
                    xblocks[this] = XBlock(args, mutableListOf())
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl -> {
                this.src?.traverse()
                val id = this.tk.str
                val bup = first(this) { it is Expr.Do && it.ishide }!! as Expr.Do
                val xup = xblocks[bup]!!
                assertIsNotDeclared(this, id, this.tk)
                if (id!="evt" && this.tag!=null && !tplates.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
                xup.syms[id] = Dcl(id, this.tmp, this.tag?.str, this.init, this.tk_.upv, bup)
                xup.syms["_${id}_"] = Dcl("_${id}_", false,null, false, this.tk_.upv, bup)
                when {
                    (this.tk_.upv == 2) -> {
                        err(tk, "var error : cannot declare an upref")
                    }
                    (this.tk_.upv==1 && bup==outer) -> {
                        err(tk, "var error : cannot declare a global upvar")
                    }
                }
            }
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
                val func = first(this) { it is Expr.Proto && it.tk.str=="func" }
                if (func != null) {
                    val acc = this.dst.base()
                    val dcl = getDcl(this, acc.tk.str)!!
                    val intask = first(dcl.blk) { it is Expr.Proto }.let { it!=null && it.tk.str!="func" }
                    if (intask) {
                        funcs_vars_tasks.add(func as Expr.Proto)
                    }
                }
                when (this.dst) {
                    is Expr.Acc   -> ""
                    is Expr.Index -> ""
                    is Expr.Pub   -> ""
                    else -> error("impossible case")
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.While  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> this.tags.forEach {
                if (it.first.str.contains('.')) {
                    err(it.first, "enum error : enum tag cannot contain '.'")
                }
                var E = ""
                var I = 0
                this.tags.forEachIndexed { i, (tag,nat) ->
                    val n = if (nat == null) {
                        I++
                        "($E) + $I"
                    } else {
                        E = nat.str
                        I = 0
                        nat.str
                    }
                    add_tag(tag, tag.str, tag.str.tag2c(), n)
                }
            }
            is Expr.Data -> {
                add_tag(this.tk, this.tk.str, this.tk.str.tag2c(), null)
                val sup = this.tk.str.dropLastWhile { it != '.' }.dropLast(1)
                if (tplates.containsKey(this.tk.str)) {
                    err(this.tk, "data error : data ${this.tk.str} is already declared")
                }
                val ids = (tplates[sup] ?: emptyList()) + this.ids
                val xids = ids.map { it.first.str }
                if (xids.size != xids.distinct().size) {
                    err(this.tk, "data error : found duplicate ids")
                }
                ids.forEach { (_,tag) ->
                    if (tag!=null && !tplates.containsKey(tag.str)) {
                        err(tag, "data error : data ${tag.str} is not declared")
                    }
                }
                tplates[this.tk.str] = ids
            }
            is Expr.Pass   -> this.e.traverse()

            is Expr.Spawn  -> { this.call.traverse() ; this.tasks?.traverse() }
            is Expr.Bcast  -> { this.xin.traverse() ; this.evt.traverse() }
            is Expr.Yield  -> {
                if (!intask(this)) {
                    err(this.tk, "yield error : expected enclosing coro or task")
                }
                this.arg.traverse()
            }
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> {
                this.x.traverse()
                if (this.x is Expr.Self) {
                    val ok = (first_true_x(this,this.x.tk.str) != null)
                    if (!ok) {
                        err(this.tk, "${this.tk.str} error : expected enclosing task")
                    }
                }
            }
            is Expr.Self   -> {
                if (true_x_c(this, this.tk.str) == null) {
                    err(this.tk, "task error : missing enclosing task")
                }
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val dcl = getDcl(this, this.tk.str)
                when {
                    (dcl == null) -> {}
                    (dcl.upv==1 && this.tk_.upv==2) -> {
                        upvs_vars_refs.add(dcl) // UPVS_VARS_REFS

                        // UPVS_PROTOS_REFS
                        all_until(this) { dcl.blk==it }
                            .filter { it is Expr.Proto }
                            .let { it as List<Expr.Proto> }
                            .forEach { proto ->
                                val set = upvs_protos_refs[proto] ?: mutableSetOf()
                                set.add(this.tk.str)
                                if (upvs_protos_refs[proto] == null) {
                                    upvs_protos_refs[proto] = set
                                }
                            }
                    }
                    // UPVS_PROTOS_NOCLOS
                    (dcl.blk!=outer && dcl.upv==0 && this.tk_.upv==0) -> {
                        // access to normal noglb w/o upval modifier
                        all_until(this) { it == dcl.blk }       // stop at enclosing declaration block
                            .filter { it is Expr.Proto }            // all crossing protos
                            .forEach { upvs_protos_noclos.add(it) }        // mark them as noclos
                    }
                }
            }
            is Expr.EvtErr -> {
                val dcl = getDcl(this, "evt")
                if (dcl?.tag != null) {
                    evts[this] = dcl.tag
                }
            }
            is Expr.Nil    -> {}
            is Expr.Tag    -> add_tag(this.tk, this.tk.str, this.tk.str.tag2c(), null)
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach{ it.traverse() }
            is Expr.Vector -> this.args.forEach{ it.traverse() }
            is Expr.Dict   -> this.args.forEach { it.first.traverse() ; it.second.traverse() }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()

                if (tpl_is(this)) {
                    val id = this.idx.tk.str.drop(1)
                    val idx = tpl_lst(this).indexOfFirst { it.first.str==id }
                    if (idx == -1) {
                        err(this.idx.tk, "index error : undeclared field \"$id\"")
                    }
                }
            }
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }

    // builds the tree structure from bottom up
    fun Expr.tree (): Map<Expr,Expr> {
        fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
            return l.map { it.tree() }.fold(l.map { Pair(it,this) }.toMap(), { a, b->a+b})
        }
        return when (this) {
            is Expr.Proto  -> this.map(listOf(this.body))
            is Expr.Do     -> this.map(this.es)
            is Expr.Dcl    -> this.map(listOfNotNull(this.src))
            is Expr.Set    -> this.map(listOf(this.dst, this.src))
            is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
            is Expr.While  -> this.map(listOf(this.cnd, this.body))
            is Expr.Catch  -> this.map(listOf(this.cnd, this.body))
            is Expr.Defer  -> this.map(listOf(this.body))
            is Expr.Enum   -> emptyMap()
            is Expr.Data -> emptyMap()
            is Expr.Pass   -> this.map(listOf(this.e))

            is Expr.Spawn  -> this.map(listOf(this.call) + listOfNotNull(this.tasks))
            is Expr.Bcast  -> this.map(listOf(this.evt, this.xin))
            is Expr.Yield  -> this.map(listOf(this.arg))
            is Expr.Resume -> this.map(listOf(this.call))
            is Expr.Toggle -> this.map(listOf(this.task, this.on))
            is Expr.Pub    -> this.map(listOf(this.x))
            is Expr.Self   -> emptyMap()

            is Expr.Nat    -> emptyMap()
            is Expr.Acc    -> emptyMap()
            is Expr.EvtErr -> emptyMap()
            is Expr.Nil    -> emptyMap()
            is Expr.Tag    -> emptyMap()
            is Expr.Bool   -> emptyMap()
            is Expr.Char   -> emptyMap()
            is Expr.Num    -> emptyMap()
            is Expr.Tuple  -> this.map(this.args)
            is Expr.Vector -> this.map(this.args)
            is Expr.Dict   -> this.map(this.args.map { listOf(it.first,it.second) }.flatten())
            is Expr.Index  -> this.map(listOf(this.col, this.idx))
            is Expr.Call   -> this.map(listOf(this.proto)) + this.map(this.args)
        }
    }
}
