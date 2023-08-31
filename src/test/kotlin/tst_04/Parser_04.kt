package tst_04

import dceu.*
import org.junit.BeforeClass
import org.junit.Test

class Parser_04 {
    // TASK

    @Test
    fun aa_01_task() {
        val l = lexer("task (a,b) { 10 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.args.size==2)
        assert(e.tostr() == "(task (a,b) {\n10\n})") { e.tostr() }
    }

}
