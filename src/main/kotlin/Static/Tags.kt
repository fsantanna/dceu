class Tags (outer: Expr.Do) {
    val pub: MutableMap<String,Triple<String,String,String?>> = TAGS.map { Pair(it,Triple(it, it.tag2c(), null)) }.toMap().toMutableMap()

    fun add (tk: Tk, id: String, c: String, enu: String?) {
        if (pub.containsKey(id)) {
            // already there
        } else {
            val issub = id.contains('.')
            val sup = id.dropLastWhile { it != '.' }.dropLast(1)
            if (issub && !pub.containsKey(sup)) {
                err(tk, "tag error : parent tag $sup is not declared")
            }
            pub[id] = Triple(id, c, enu)
        }
    }

    init {
        outer.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> this.body.traverse()
            is Expr.Export -> this.body.traverse()
            is Expr.Do -> this.es.forEach { it.traverse() }
            is Expr.Dcl -> this.src?.traverse()
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.body.traverse()
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
                    add(tag, tag.str, tag.str.tag2c(), n)
                }
            }
            is Expr.Data -> add(this.tk, this.tk.str, this.tk.str.tag2c(), null)
            is Expr.Pass   -> this.e.traverse()

            is Expr.Spawn  -> { this.call.traverse() ; this.tasks?.traverse() }
            is Expr.Bcast  -> { this.xin.traverse() ; this.evt.traverse() }
            is Expr.Yield  -> this.arg.traverse()
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> this.x.traverse()
            is Expr.Self   -> {}

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.EvtErr -> {}
            is Expr.Nil    -> {}
            is Expr.Tag    -> add(this.tk, this.tk.str, this.tk.str.tag2c(), null)
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach{ it.traverse() }
            is Expr.Vector -> this.args.forEach{ it.traverse() }
            is Expr.Dict   -> this.args.forEach { it.first.traverse() ; it.second.traverse() }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()
            }
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
