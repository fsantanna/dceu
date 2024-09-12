package dceu

class Tags (outer: Expr.Do) {
    val xxx: Map<String,Tk.Tag>
    val pub: Map<String,Pair<String,String>>

    init {
        this.xxx = TAGS.map { Pair(it,Tk.Tag(it,outer.tk.pos.copy())) }.toMap() + outer.dn_gather {
            when (it) {
                is Expr.Do     -> if (it.tag == null) emptyMap() else mapOf(Pair(it.tag.str,it.tag))
                is Expr.Escape -> mapOf(Pair(it.tag.str,it.tag))
                is Expr.Data   -> mapOf(Pair(it.tk_.str,it.tk_))
                is Expr.Catch  -> if (it.tag == null) emptyMap() else mapOf(Pair(it.tag.str,it.tag))
                is Expr.Tag    -> mapOf(Pair(it.tk_.str,it.tk_))
                else           -> emptyMap()
            }
        }
        this.pub = this.xxx.map { (id,tk)->Pair(id,Pair(id,id.idc())) }.toMap()
        for ((id,tk) in this.xxx) {
            val issub = id.contains('.')
            //println(listOf(id,issub))
            val sup = id.dropLastWhile { it != '.' }.dropLast(1)
            if (issub && !this.xxx.containsKey(sup)) {
                err(tk, "tag error : parent tag $sup is not declared")
            }
        }
    }
}
