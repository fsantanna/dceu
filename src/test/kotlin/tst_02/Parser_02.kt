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
        val l = lexer("catch ( it|1) { error(1) }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "catch (it | 1) {\nerror(1)\n}\n") { e.tostr() }
    }
    @Test
    fun bb_02_throw_catch() {
        val l = lexer("catch (it :T| it[0]) { nil }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "catch (it :T | it[0]) {\nnil\n}\n") { e.tostr() }
    }

    // IT / AS

    @Test
    fun cc_01_it() {
        val l = lexer("it")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "it\n") { e.tostr() }
    }
    @Test
    fun cc_02_as() {
        val out = test("""
            catch ( 1 )
        """)
        assert(out == "anon : (lin 2, col 21) : expected identifier : have \"1\"\n") { out }
    }
    @Test
    fun cc_03_as() {
        val out = test("""
            catch ( x )
        """)
        assert(out == "anon : (lin 2, col 23) : expected \"|\" : have \")\"\n") { out }
    }
    @Test
    fun cc_04_as() {
        val out = test("""
            catch ( x |  )
        """)
        assert(out == "anon : (lin 2, col 26) : expected expression : have \")\"\n") { out }
        //assert(out == "anon : (lin 3, col 9) : expected \"{\" : have end of file\n") { out }
    }
    @Test
    fun cc_05_as() {
        val out = test("""
            catch ( x | 1
        """)
        assert(out == "anon : (lin 3, col 9) : expected \")\" : have end of file\n") { out }
    }
    @Test
    fun cc_06_as() {
        val out = test("""
            catch ( x| 1 ) {nil}
            println(:ok)
        """)
        //assert(out == "anon : (lin 2, col 27) : expected \"in\" : have \"{\"\n") { out }
        assert(out == ":ok\n") { out }
    }
}
