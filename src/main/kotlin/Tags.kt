package dceu

class Tags (outer: Expr.Do) {
    val pub: Map<String,Tk.Tag>

    init {
        this.pub = TAGS.map { Pair(it,Tk.Tag(it,outer.tk.pos.copy())) }.toMap() + outer.dn_gather {
            when (it) {
                is Expr.Do     -> if (it.tag == null) emptyMap() else mapOf(Pair(it.tag.str,it.tag))
                is Expr.Escape -> mapOf(Pair(it.tag.str,it.tag))
                is Expr.Data   -> mapOf(Pair(it.tk_.str,it.tk_))
                is Expr.Catch  -> if (it.tag == null) emptyMap() else mapOf(Pair(it.tag.str,it.tag))
                is Expr.Tag    -> mapOf(Pair(it.tk_.str,it.tk_))
                else           -> emptyMap()
            }
        }
        for ((id,tk) in this.pub) {
            val issub = id.contains('.')
            //println(listOf(id,issub))
            val sup = id.dropLastWhile { it != '.' }.dropLast(1)
            if (issub && !this.pub.containsKey(sup)) {
                err(tk, "tag error : parent tag $sup is not declared")
            }
        }
    }
}
