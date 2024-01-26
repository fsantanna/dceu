package dceu

class Mem (val ups: Ups, val vars: Vars) {

    fun pub (acc: Expr.Acc): String {
        val dcl = vars.acc_to_dcl[acc]!!
        val blk = vars.dcl_to_blk[dcl]!!
        if (ups.pub[blk] == null) {
            val idx = vars.blk_to_dcls[blk]!!.lastIndexOf(dcl)
            assert(idx != -1)
            return idx.toString()
        } else {
            return "TODO"
        }
    }
}