package tst_03

import dceu.*
import org.junit.Test

class Parser_03 {

    // CORO

    @Test
    fun aa_01_coro_err() {
        val l = lexer("coro (a,b) :fake { 10 }")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 12) : expected \"{\" : have \":fake\"")
    }
    @Test
    fun aa_02_coro_err() {
        val l = lexer("coro (a,b) :xxx { 10 }")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 12) : expected \"{\" : have \":xxx\"")
    }
    @Test
    fun aa_03_coro() {
        val l = lexer("coro (a,b) { 10 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.args.size==2)
        assert(e.tostr() == "(coro (a,b) {\n10\n})") { e.tostr() }
    }

    // COROUTINE / YIELD / RESUME

    @Test
    fun bb_01_coro() {
        val l = lexer("""
            set t = coro (v) {
                set v = yield((1)) 
                yield((2)) 
            }
            coroutine(t)
            set v = resume a(1)
            resume a(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == """
            set t = (coro (v) {
            set v = yield(1)
            yield(2)
            })
            coroutine(t)
            set v = resume a(1)
            resume a(2)
            
        """.trimIndent()) { e.tostr() }
    }
    @Test
    fun bb_02_resume_err() {
        val l = lexer("""
            resume a
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : invalid resume : expected call")
    }
    @Test
    fun bb_03_yield_err() {
        val l = lexer("""
            yield
            1
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 2, col 1) : expected \"(\" : have \"1\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : yield error : line break before expression")
    }
    @Test
    fun bb_04_yield_err() {
        val l = lexer("""
            yield
            (1)
        """.trimIndent())
        val parser = Parser(l)
        val e = parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : yield error : line break before expression")
        assert(e.tostr() == "yield(1)")
    }
}
