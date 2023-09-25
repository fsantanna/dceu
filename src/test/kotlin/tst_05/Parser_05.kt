package tst_05

import dceu.*
import org.junit.Test

class Parser_05 {
    // TASKS

    @Test
    fun aa_01_tasks_err() {
        val l = lexer("""
            spawn in nil, {}
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 27) : expected expression : have \"{\"")
    }
    @Test
    fun aa_02_tasks_err() {
        val l = lexer("""
            spawn in ()
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected expression : have \")\"")
    }
    @Test
    fun aa_03_tasks_err() {
        val l = lexer("""
            spawn in nil, f
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : invalid spawn : expected call")
    }
    @Test
    fun aa_04_tasks() {
        val l = lexer("""
            spawn in ts, T()
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "spawn in ts, T()\n") { e.tostr() }
    }

    // DETRACK

    @Test
    fun bb_01_detrack_err() {
        val l = tst_03.lexer(
            """
            detrack(1)
        """
        )
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 3, col 9) : expected \"{\" : have end of file")
    }
    @Test
    fun bb_02_detrack_ok() {
        val l = tst_03.lexer(
            """
            detrack(1) { nil }
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "detrack(1) {\n" +
                "nil\n" +
                "}\n") { e.tostr() }
    }
}
