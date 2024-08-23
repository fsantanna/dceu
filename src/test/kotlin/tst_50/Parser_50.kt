package tst_50

import dceu.*
import org.junit.Test

class Parser_50 {
    @Test
    fun aa_01_func_nested() {
        val l = lexer(
            """
            func :nested () { nil }
        """
        )
        val parser = Parser(l)
        //val e = parser.expr()
        //assert(e.tostr() == "(func :nested () {\nnil\n})") { e.tostr() }
        assert(trap { parser.expr() } == "anon : (lin 2, col 18) : expected \"(\" : have \":nested\"")
    }
    @Test
    fun aa_02_coro_nested() {
        val l = tst_04.lexer(
            """
            coro :nested () { nil }
        """
        )
        val parser = Parser(l)
        //val e = parser.expr()
        //assert(e.tostr() == "(coro :nested () {\nnil\n})") { e.tostr() }
        assert(trap { parser.expr() } == "anon : (lin 2, col 18) : expected \"(\" : have \":nested\"")
    }

}
