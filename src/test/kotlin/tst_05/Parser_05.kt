package tst_05

import dceu.*
import org.junit.Test

class Parser_05 {
    // TASKS

    @Test
    fun aa_01_tasks_err() {
        val l = lexer("""
            spawn {} in nil
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 19) : expected expression : have \"{\"")
    }
    @Test
    fun aa_02_tasks_err() {
        val l = lexer("""
            spawn f() in ()
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 27) : expected expression : have \")\"")
    }
    @Test
    fun aa_03_tasks_err() {
        val l = lexer("""
            spawn f in nil
        """)
        val parser = Parser(l)
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : invalid spawn : expected call")
        assert(trap { parser.exprs() } == "anon : (lin 2, col 21) : spawn error : expected call")
    }
    @Test
    fun aa_04_tasks() {
        val l = lexer("""
            spawn T() in ts
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(spawn T(nil) in ts)\n") { e.tostr() }
    }

    // DETRACK

    @Test
    fun bb_01_detrack_err() {
        val l = tst_03.lexer("""
            detrack(1)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "detrack''(1)\n") { e.tostr() }
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \"{\" : have end of file")
    }
    @Test
    fun bb_02_detrack_ok() {
        val l = tst_03.lexer(
            """
            detrack(1) { it=> nil }
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        //assert(e.tostr() == "((detrack(1)) thus { it =>\n" +
        //        "nil\n" +
        //        "})\n") { e.tostr() }
        assert(e.tostr() == "(detrack(1) {\n" +
                "it =>\n" +
                "```\n" +
                "                                CEU_Stack ceu_dstk_15 = { &ceu_acc.Dyn->Exe_Task, 1, ceu_dstk };\n" +
                "                                ceu_dstk = &ceu_dstk_15;\n" +
                "                                ```\n" +
                "nil\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun bb_03_as() {
        val out = test("""
            detrack(1) { 1 }
        """)
        assert(out == "anon : (lin 2, col 26) : expected identifier : have \"1\"\n") { out }
    }
    @Test
    fun bb_04_as() {
        val out = test("""
            detrack(1) { x }
        """)
        assert(out == "anon : (lin 2, col 28) : expected \"=>\" : have \"}\"\n") { out }
    }
    @Test
    fun bb_05_as() {
        val out = test("""
            detrack(1) { x => }
        """)
        assert(out == "anon : (lin 2, col 31) : expected expression : have \"}\"\n") { out }
    }
    @Test
    fun bb_06_as() {
        val out = test("""
            detrack(1) { x => 1
        """)
        assert(out == "anon : (lin 3, col 9) : expected \"}\" : have end of file\n") { out }
    }
    @Test
    fun bb_07_as() {
        val out = test("""
            catch ( x => 1 ) {nil}
        """)
        //assert(out == "anon : (lin 2, col 30) : expected \"in\" : have \"{\"\n") { out }
        assert(out == "") { out }
    }
    @Test
    fun bb_08_as() {
        val l = tst_03.lexer("""
            detrack(1) { x => 1 }
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(detrack(1) {\n" +
                "x =>\n" +
                "```\n" +
                "                                CEU_Stack ceu_dstk_15 = { &ceu_acc.Dyn->Exe_Task, 1, ceu_dstk };\n" +
                "                                ceu_dstk = &ceu_dstk_15;\n" +
                "                                ```\n" +
                "1\n" +
                "})\n") { e.tostr() }
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \"{\" : have end of file")
    }

    // TRACK

    @Test
    fun cc_01_track() {
        val l = lexer("""
            track(x)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "track(x)\n") { e.tostr() }
    }
}
