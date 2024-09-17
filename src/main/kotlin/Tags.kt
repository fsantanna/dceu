package dceu

class Tags () {
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
        G.outer!!.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> this.blk.traverse()
            is Expr.Do -> {
                if (this.tag != null) {
                    add(this.tag, this.tag.str, this.tag.str.idc(), null)
                }
                this.es.forEach { it.traverse() }
            }
            is Expr.Escape -> {
                add(this.tag, this.tag.str, this.tag.str.idc(), null)
                this.e?.traverse()
            }
            is Expr.Group -> this.es.forEach { it.traverse() }
            is Expr.Dcl -> this.src?.traverse()
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.blk.traverse()
            is Expr.Data   -> add(this.tk, this.tk.str, this.tk.str.idc(), null)
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> {
                if (this.tag != null) {
                    add(this.tag, this.tag.str, this.tag.str.idc(), null)
                }
                this.blk.traverse()
            }
            is Expr.Defer  -> this.blk.traverse()

            is Expr.Yield  -> this.e.traverse()
            is Expr.Resume -> { this.co.traverse() ; this.args.forEach { it.traverse() } }

            is Expr.Spawn  -> { this.tsks?.traverse() ; this.tsk.traverse() ; this.args.forEach { it.traverse() } }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }
            is Expr.Tasks  -> this.max.traverse()

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.Nil    -> {}
            is Expr.Tag    -> add(this.tk, this.tk.str, this.tk.str.idc(), null)
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach { it.traverse() }
            is Expr.Vector -> this.args.forEach { it.traverse() }
            is Expr.Dict   -> this.args.forEach { it.first.traverse() ; it.second.traverse() }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()
            }
            is Expr.Call   -> { this.clo.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
