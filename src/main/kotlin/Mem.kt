package dceu

class Mem (val ups: Ups, val vars: Vars) {
    fun pub (dcl: Expr.Dcl): String {
        val blk = vars.dcl_to_blk[dcl]!!
        val idx = 1 + vars.blk_to_dcls[blk]!!.lastIndexOf(dcl)
                    // +1 = BLOCK
        assert(idx != -1)
        val off = ups.all_until(dcl) { it is Expr.Proto}
            .filter { it is Expr.Do }
            .map { 1 + vars.blk_to_dcls[it]!!.count() }
            .sum()  // +1 = BLOCK
        return (off+idx).toString()
    }
    fun pub (acc: Expr.Acc): String {
        return this.pub(vars.acc_to_dcl[acc]!!)
    }
}