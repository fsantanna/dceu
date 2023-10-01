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
        val l = lexer("catch {as it=>1} in { throw(1) }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "catch { as it => 1 } in {\nthrow(1)\n}\n") { e.tostr() }
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
            catch { as 1 }
        """)
        assert(out == "anon : (lin 2, col 24) : expected identifier : have \"1\"\n") { out }
    }
    @Test
    fun cc_03_as() {
        val out = test("""
            catch { as x }
        """)
        assert(out == "anon : (lin 2, col 26) : expected \"=>\" : have \"}\"\n") { out }
    }
    @Test
    fun cc_04_as() {
        val out = test("""
            catch { as x => }
        """)
        assert(out == "anon : (lin 2, col 29) : expected expression : have \"}\"\n") { out }
    }
    @Test
    fun cc_05_as() {
        val out = test("""
            catch { as x => 1
        """)
        assert(out == "anon : (lin 3, col 9) : expected \"}\" : have end of file\n") { out }
    }
    @Test
    fun cc_06_as() {
        val out = test("""
            catch { as x => 1 } {nil}
        """)
        assert(out == "anon : (lin 2, col 33) : expected \"in\" : have \"{\"\n") { out }
    }
}
