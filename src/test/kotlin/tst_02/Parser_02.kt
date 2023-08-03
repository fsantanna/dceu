package tst_02

import dceu.*
import org.junit.BeforeClass
import org.junit.Test

class Parser_02 {
    // DEFER

    @Test
    fun aa_01_defer() {
        val l = lexer("defer { nil }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "defer {\nnil\n}\n") { e.tostr() }
    }

    // THROW / CATCH

    @Test
    fun bb_01_throw_catch() {
        val l = lexer("catch 1 { throw(1) }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "catch 1 {\nthrow(1)\n}\n") { e.tostr() }
    }

}
