class Static (val outer: Expr.Do, val ups: Ups, val vars: Vars) {
    val evts: MutableMap<Expr.EvtErr, String?> = mutableMapOf()
    val tags: MutableMap<String,Triple<String,String,String?>> = TAGS.map { Pair(it,Triple(it, it.tag2c(), null)) }.toMap().toMutableMap()
    val datas = mutableMapOf<String,List<Pair<Tk.Id,Tk.Tag?>>>()

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
    val upvs_vars_refs = mutableSetOf<Var>()

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

    fun data_is (e: Expr.Index): Boolean {
        val id = e.col.tk.str
        val dcl = vars.get(e, id)
        return when (e.col) {
            is Expr.Pub -> when (e.col.x) {
                // task.pub -> task (...) :T {...}
                is Expr.Self -> (e.idx is Expr.Tag) && (ups.first_true_x(e,"task").let { it!=null && it.task!!.first!=null })
                // x.pub -> x:T
                is Expr.Acc -> (e.idx is Expr.Tag) && (vars.get(e, e.col.x.tk.str)!!.tag != null)
                // x.y.pub -> x.y?
                is Expr.Index -> this.data_is(e.col.x)
                // detrack(x).pub
                is Expr.Call -> false   // TODO
                else -> error("impossible case")
            }
            is Expr.EvtErr -> (e.idx is Expr.Tag) && (dcl != null) && (evts[e.col] != null)
            is Expr.Acc    -> (e.idx is Expr.Tag) && (dcl!!.tag != null)
            is Expr.Index  -> this.data_is(e.col)
            else           -> false
        }
    }
    fun data_lst (e: Expr.Index): List<Pair<Tk.Id, Tk.Tag?>> {
        val id = e.col.tk.str
        return when {
            (e.col is Expr.Pub) -> when (e.col.x) {
                is Expr.Self -> {
                    // task.pub -> task (...) :T {...}
                    val tag = ups.first_true_x(e,"task")!!.task!!.first!!.str
                    this.datas[tag]!!
                }
                is Expr.Acc -> {
                    // x.pub -> x:T
                    val tag = vars.get(e, e.col.x.tk.str)!!.tag!!
                    this.datas[tag]!!
                }
                is Expr.Index -> {
                    // x.y.pub -> x.y?
                    this.data_is(e.col.x)
                    TODO()
                }
                else -> error("impossible case")
            }
            (e.col is Expr.EvtErr) -> {
                val tag = evts[e.col]!!
                this.datas[tag]!!
            }
            (e.col is Expr.Acc) -> {
                val dcl = vars.get(e, id)!!
                this.datas[dcl.tag]!!
            }
            (e.col is Expr.Index) -> {
                e.col.idx as Expr.Tag
                val xid = e.col.idx.tk.str.drop(1)
                val lst = this.data_lst(e.col)
                val id_tag = lst.firstOrNull { it.first.str==xid }!!
                if (id_tag.second == null) {
                    err(e.idx.tk, "index error : field \"$xid\" is not a data")
                }
                this.datas[id_tag.second!!.str]!!
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
                if (this.task!=null && this.task.first!=null && !datas.containsKey(this.task.first!!.str)) {
                    val tag = this.task.first!!
                    err(tag, "declaration error : data ${tag.str} is not declared")
                }
                this.body.traverse()
            }
            is Expr.Do -> {
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl -> {
                this.src?.traverse()
                val id = this.tk.str
                if (id!="evt" && this.tag!=null && !datas.containsKey(this.tag.str)) {
                    err(this.tag, "declaration error : data ${this.tag.str} is not declared")
                }
            }
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
                val func = ups.first(this) { it is Expr.Proto && it.tk.str=="func" }
                if (func != null) {
                    val acc = this.dst.base()
                    val dcl = vars.get(this, acc.tk.str)!!
                    val intask = ups.first(dcl.blk) { it is Expr.Proto }.let { it!=null && it.tk.str!="func" }
                    if (intask) {
                        funcs_vars_tasks.add(func as Expr.Proto)
                    }
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

            is Expr.Spawn  -> { this.call.traverse() ; this.tasks?.traverse() }
            is Expr.Bcast  -> { this.xin.traverse() ; this.evt.traverse() }
            is Expr.Yield  -> {
                if (!ups.intask(this)) {
                    err(this.tk, "yield error : expected enclosing coro or task")
                }
                this.arg.traverse()
            }
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> {
                this.x.traverse()
                if (this.x is Expr.Self) {
                    val ok = (ups.first_true_x(this,this.x.tk.str) != null)
                    if (!ok) {
                        err(this.tk, "${this.tk.str} error : expected enclosing task")
                    }
                }
            }
            is Expr.Self   -> {
                if (ups.true_x_c(this, this.tk.str) == null) {
                    err(this.tk, "task error : missing enclosing task")
                }
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val dcl = vars.get(this, this.tk.str)
                when {
                    (dcl == null) -> {}
                    (dcl.upv==1 && this.tk_.upv==2) -> {
                        upvs_vars_refs.add(dcl) // UPVS_VARS_REFS

                        // UPVS_PROTOS_REFS
                        ups.all_until(this) { dcl.blk==it }
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
                        ups.all_until(this) { it == dcl.blk }       // stop at enclosing declaration block
                            .filter { it is Expr.Proto }            // all crossing protos
                            .forEach { upvs_protos_noclos.add(it) }        // mark them as noclos
                    }
                }
            }
            is Expr.EvtErr -> {
                val dcl = vars.get(this, "evt")
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

                if (data_is(this)) {
                    val id = this.idx.tk.str.drop(1)
                    val idx = data_lst(this).indexOfFirst { it.first.str==id }
                    if (idx == -1) {
                        err(this.idx.tk, "index error : undeclared field \"$id\"")
                    }
                }
            }
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
