package dceu

class Tags (outer: Expr.Call) {
    val pub: MutableMap<String,Triple<String,String,String?>> = TAGS.map { Pair(it,Triple(it, it.idc(), null)) }.toMap().toMutableMap()

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
            is Expr.Proto -> this.blk.traverse()
            is Expr.Do -> this.es.forEach { it.traverse() }
            is Expr.Dcl -> this.src?.traverse()
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.blk.traverse()
            is Expr.Break  -> { this.cnd.traverse() ; this.e?.traverse() }
            is Expr.Skip   -> this.cnd.traverse()
            is Expr.Enum   -> this.tags.forEach {
                if (it.first.str.contains('.')) {
                    err(it.first, "enum error : enum tag cannot contain '.'")
                }
                var E = ""
                var I = 0
                this.tags.forEach { (tag,nat) ->
                    val n = if (nat == null) {
                        I++
                        "($E) + $I"
                    } else {
                        E = nat.str
                        I = 0
                        nat.str
                    }
                    add(tag, tag.str, tag.str.idc(), n)
                }
            }
            is Expr.Data -> add(this.tk, this.tk.str, this.tk.str.idc(), null)
            is Expr.Pass   -> this.e.traverse()

            is Expr.Catch  -> { this.cnd.traverse() ; this.blk.traverse() }
            is Expr.Defer  -> this.blk.traverse()

            is Expr.Yield  -> this.args.traverse()
            is Expr.Resume -> { this.co.traverse() ; this.args.traverse() }

            is Expr.Spawn  -> { this.tsks?.traverse() ; this.tsk.traverse() ; this.args.traverse() }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.Nil    -> {}
            is Expr.Tag    -> add(this.tk, this.tk.str, this.tk.str.idc(), null)
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.traverse()
            is Expr.Vector -> this.args.traverse()
            is Expr.Dict   -> this.args.forEach { it.first.traverse() ; it.second.traverse() }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()
            }
            is Expr.Call   -> { this.clo.traverse() ; this.args.traverse() }

            is Expr.VA_len -> {}
            is Expr.VA_idx -> this.idx.traverse()
            is Expr.Args   -> this.es.forEach { it.traverse() }
        }
    }
}
