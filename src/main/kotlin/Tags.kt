package dceu

fun tags_reset (): Map<String,Tk.Tag> {
    val ret = TAGS.map { Pair(it,Tk.Tag(it,G.outer!!.tk.pos.copy())) }.toMap() + G.outer!!.dn_gather {
        when (it) {
            is Expr.Do     -> if (it.tag == null) emptyMap() else mapOf(Pair(it.tag.str,it.tag))
            is Expr.Escape -> mapOf(Pair(it.tag.str,it.tag))
            is Expr.Data   -> mapOf(Pair(it.tk_.str,it.tk_))
            is Expr.Catch  -> if (it.tag == null) emptyMap() else mapOf(Pair(it.tag.str,it.tag))
            is Expr.Tag    -> mapOf(Pair(it.tk_.str,it.tk_))
            else           -> emptyMap()
        }
    }
    for ((id,tk) in ret) {
        val issub = id.contains('.')
        //println(listOf(id,issub))
        val sup = id.dropLastWhile { it != '.' }.dropLast(1)
        if (issub && !ret.containsKey(sup)) {
            err(tk, "tag error : parent tag $sup is not declared")
        }
    }
    return ret
}