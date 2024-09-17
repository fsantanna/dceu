package tst_02

import dceu.*
import org.junit.Test

class Parser_02 {
    // DEFER

    @Test
    fun aa_01_defer() {
        val l = lexer("defer { nil }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "defer {\nnil;\n};\n") { e.to_str() }
    }

    // THROW / CATCH

    @Test
    fun bb_01_throw_catch() {
        val l = lexer("catch :x { error(1) }")
        //val l = lexer("catch ( it|1) { error(1) }")
        val parser = Parser(l)
        val e = parser.exprs()
        /*
        assert(e.tostr() == "catch' (do {\n" +
                "(val it = ```:ceu  *(ceu_acc.Dyn->Error.val)```);\n" +
                "1;\n" +
                "}) {\n" +
                "error(1);\n" +
                "};\n") { e.tostr() }
         */
        assert(e.to_str() == "catch :x {\n" +
                "error(1);\n" +
                "};\n") { e.to_str() }
    }
    @Test
    fun bb_02_throw_catch() {
        val l = lexer("catch (it :T| it[0]) { nil }")
        val parser = Parser(l)
        /*
        val e = parser.exprs()
        assert(e.tostr() == "catch' (do {\n" +
                "(val it :T = ```:ceu  *(ceu_acc.Dyn->Error.val)```);\n" +
                "it[0];\n" +
                "}) {\n" +
                "nil;\n" +
                "};\n") { e.tostr() }
         */
    }

    // IT / AS

    @Test
    fun cc_01_it() {
        val l = lexer("it")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "it;\n") { e.to_str() }
    }
    @Test
    fun cc_02_as() {
        val out = test("""
            catch ( 1 )
        """)
        assert(out == "anon : (lin 2, col 21) : expected tag : have \"1\"\n") { out }
        //assert(out == "anon : (lin 2, col 21) : expected tag : have \"1\"\n") { out }
        //assert(out == "anon : (lin 2, col 21) : expected identifier : have \"1\"\n") { out }
    }
    @Test
    fun cc_03_as() {
        val out = test("""
            catch ( x )
        """)
        assert(out == "anon : (lin 2, col 21) : expected tag : have \"x\"\n") { out }
        //assert(out == "anon : (lin 2, col 21) : expected tag : have \"x\"\n") { out }
        //assert(out == "anon : (lin 2, col 23) : expected \"|\" : have \")\"\n") { out }
    }
    @Test
    fun cc_04_as() {
        val out = test("""
            catch ( :x |  )
        """)
        assert(out == "anon : (lin 2, col 24) : expected \")\" : have \"|\"\n") { out }
        //assert(out == "anon : (lin 2, col 26) : expected expression : have \")\"\n") { out }
        //assert(out == "anon : (lin 3, col 9) : expected \"{\" : have end of file\n") { out }
    }
    @Test
    fun cc_05_as() {
        val out = test("""
            catch ( x | 1
        """)
        assert(out == "anon : (lin 2, col 21) : expected tag : have \"x\"\n") { out }
        //assert(out == "anon : (lin 3, col 9) : expected \")\" : have end of file\n") { out }
    }
    @Test
    fun cc_06_as() {
        val out = test("""
            catch ( x| 1 ) {nil}
            println(:ok)
        """)
        //assert(out == "anon : (lin 2, col 27) : expected \"in\" : have \"{\"\n") { out }
        assert(out == "anon : (lin 2, col 21) : expected tag : have \"x\"\n") { out }
        //assert(out == ":ok\n") { out }
    }

    // LOOP

    @Test
    fun qq_01_loop_err() {
        val l = tst_01.lexer("loop' { ;;;do;;; nil }")
        val parser = Parser(l)
        val e1 = parser.expr() as Expr.Loop
        assert(e1.blk.to_str() == "{\nnil;\n}") { e1.blk.to_str() }
    }
    @Test
    fun qq_02_loop_err() {
        val l = tst_01.lexer("loop' until {")
        val parser = Parser(l)
        assert(trap { parser.expr_1_bin() } == "anon : (lin 1, col 7) : expected \"{\" : have \"until\"")
        //assert(trap { parser.expr_1_bin() } == "anon : (lin 1, col 12) : expected expression : have \"{\"")
    }
    @Test
    fun qq_03_loop_err() {
        val l = tst_02.lexer("loop' x { }")
        val parser = Parser(l)
        //assert(trap { parser.expr_prim() } == "anon : (lin 1, col 7) : invalid loop : unexpected x")
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 7) : expected \"{\" : have \"x\"")
    }

    // PATTS

    /*
    @Test
    fun dd_01_patt () {
        val l = tst_01.lexer(
            """
            ()
        """
        )
        val parser = Parser(l)
        assert(trap { parser.patt() } == "anon : (lin 2, col 14) : expected identifier : have \")\"")
    }
    @Test
    fun dd_02_patt () {
        val l = tst_01.lexer(
            """
            (x)
        """
        )
        val parser = Parser(l)
        assert(trap { parser.patt() } == "anon : (lin 2, col 15) : expected \"|\" : have \")\"")
    }
    @Test
    fun dd_03_patt () {
        val l = tst_01.lexer(
            """
            (x|)
        """
        )
        val parser = Parser(l)
        assert(trap { parser.patt() } == "anon : (lin 2, col 16) : expected expression : have \")\"")
    }
    @Test
    fun dd_04_patt () {
        val l = tst_01.lexer(
            """
            (x|true)
        """
        )
        val parser = Parser(l)
        val p = parser.patt()
        assert(Patts_Any_tostr(p) == "(x | true)") { Patts_Any_tostr(p) }
    }
    @Test
    fun dd_05_patt () {
        val l = tst_01.lexer(
            """
            (x:X|nil)
        """
        )
        val parser = Parser(l)
        val p = parser.patt()
        assert(Patts_Any_tostr(p) == "(x :X | nil)") { Patts_Any_tostr(p) }
    }
     */
}
