package dceu

fun cache_ups () {
    G.outer!!.dn_gather { me ->
        when (me) {
            is Expr.Proto -> {
                G.ups[me.blk] = me
                me.pars.forEach { G.ups[it] = me }
            }

            is Expr.Do -> me.es.forEach { G.ups[it] = me }
            is Expr.Escape -> if (me.e != null) G.ups[me.e] = me
            is Expr.Group -> me.es.forEach { G.ups[it] = me }
            is Expr.Dcl -> if (me.src != null) G.ups[me.src] = me
            is Expr.Set -> {
                G.ups[me.dst] = me
                G.ups[me.src] = me
            }

            is Expr.If -> {
                G.ups[me.cnd] = me
                G.ups[me.t] = me
                G.ups[me.f] = me
            }

            is Expr.Loop -> G.ups[me.blk] = me
            is Expr.Data -> {}
            is Expr.Drop -> G.ups[me.e] = me

            is Expr.Catch -> G.ups[me.blk] = me
            is Expr.Defer -> G.ups[me.blk] = me

            is Expr.Yield -> G.ups[me.e] = me
            is Expr.Resume -> {
                G.ups[me.co] = me
                me.args.forEach { G.ups[it] = me }
            }

            is Expr.Spawn -> {
                if (me.tsks != null) G.ups[me.tsks] = me
                G.ups[me.tsk] = me
                me.args.forEach { G.ups[it] = me }
            }

            is Expr.Delay -> {}
            is Expr.Pub -> if (me.tsk != null) G.ups[me.tsk] = me
            is Expr.Toggle -> {
                G.ups[me.tsk] = me
                G.ups[me.on] = me
            }

            is Expr.Tasks -> G.ups[me.max] = me

            is Expr.Nat -> {}
            is Expr.Acc -> {}
            is Expr.Nil -> {}
            is Expr.Tag -> {}
            is Expr.Bool -> {}
            is Expr.Char -> {}
            is Expr.Num -> {}
            is Expr.Tuple -> me.args.forEach { G.ups[it] = me }
            is Expr.Vector -> me.args.forEach { G.ups[it] = me }
            is Expr.Dict -> {
                me.args.forEach {
                    G.ups[it.first] = me
                    G.ups[it.second] = me
                }
            }

            is Expr.Index -> {
                G.ups[me.col] = me
                G.ups[me.idx] = me
            }

            is Expr.Call -> {
                G.ups[me.clo] = me
                me.args.forEach { G.ups[it] = me }
            }
        }
        emptyMap<Unit,Unit>()
    }
}

fun cache_tags (): Map<String,Tk.Tag> {
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