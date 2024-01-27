package dceu

class Mem (val ups: Ups, val vars: Vars) {
    fun pub (dcl: Expr.Dcl): String {
        // Use ups[blk] instead of ups[dcl]
        //  - some dcl are args
        //  - dcl args are created after ups

        val blk = vars.dcl_to_blk[dcl]!!
        val idx = 1 + vars.blk_to_dcls[blk]!!.lastIndexOf(dcl)
                    // +1 = block sentinel
        assert(idx != -1)
        val proto = ups.first(blk) { it is Expr.Proto }
        val off = ups.all_until(blk) { it == proto }
            //.let { println(it) ; it }
            .drop(1)    // myself
            .filter { it is Expr.Do }
            .map { 1 + vars.blk_to_dcls[it]!!.count() }
            .sum()  // +1 = block sentinel
        //println(listOf(dcl.id.str,off,idx))
        return when {
            (proto == null) -> {            // global
                (off + idx).toString()
            }
            (ups.pub[dcl] == null) -> {     // argument
                // arguments are before the block sentinel
                "-1 + ceu_base + " + (off + idx).toString()
            }
            else -> {                       // local
                "ceu_base + " + (off + idx).toString()
            }
        }.let { "(" + it + ")" }
    }
    fun pub (acc: Expr.Acc): String {
        return this.pub(vars.acc_to_dcl[acc]!!)
    }
}