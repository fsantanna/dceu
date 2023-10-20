package tst_99

import dceu.*
import org.junit.Test

class Parser_99 {
    // EMPTY IF / BLOCK

    @Test
    fun aa_01_empty_if() {
        val l = lexer("if true { 1 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n} else {\nnil\n}") { e.tostr() }
    }
    @Test
    fun aa_02_empty_do() {  // set whole tuple?
        val l = lexer("do{}")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Do && e.es.size==1)
        assert(e.tostr() == "do {\nnil\n}") { e.tostr() }
    }
    @Test
    fun aa_03_empty_func() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.args.size==0)
        assert(e.tostr() == "(func () {\nnil\n})") { e.tostr() }
    }
    @Test
    fun aa_04_empty_loop() {
        val l = lexer("loop { }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e.tostr() == "loop {\nnil\n}") { e.tostr() }
    }

    // OPS: not, and, or

    @Test
    fun bb_01_bin_or() {
        N = 1
        val l = lexer("1 or 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(1 thus { as ceu_5 =>\n" +
                "if ceu_5 {\n" +
                "ceu_5\n" +
                "} else {\n" +
                "2\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun bb_02_bin_and() {
        N = 1
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(1 thus { as ceu_5 =>\n" +
                "if ceu_5 {\n" +
                "2\n" +
                "} else {\n" +
                "ceu_5\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun bb_03_not() {
        N = 1
        val l = lexer("not true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if true {\nfalse\n} else {\ntrue\n}") { e.tostr() }
    }
    @Test
    fun bb_04_bin_not_or_and() {
        N = 1
        val l = lexer("((not true) and false) or true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            ((if true {
            false
            } else {
            true
            } thus { as ceu_27 =>
            if ceu_27 {
            false
            } else {
            ceu_27
            }
            })
             thus { as ceu_75 =>
            if ceu_75 {
            ceu_75
            } else {
            true
            }
            })
            
        """.trimIndent()) { e.tostr() }
    }
    @Test
    fun bb_05_pre() {
        val l = lexer("- not - 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{{-}}(if {{-}}(1) {\nfalse\n} else {\ntrue\n})") { e.tostr() }
    }

    // OPS: is?, is-not?, in?, in-not?

    @Test
    fun bc_01_pre() {
        val l = lexer("a is? b ; c is-not? d ; e in? f ; g in-not? h")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "is'(a,b)\nis-not'(c,d)\nin'(e,f)\nin-not'(g,h)\n") { e.tostr() }
    }

    // FUNC / DCL

    @Test
    fun cc_01_func() {
        val l = lexer("func f () {}")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "val f = (func () {\nnil\n})\n") { e.tostr() }
    }

    // IF / ID-TAG

    @Test
    fun dd_01_if() {
        val l = lexer("if x:X { :ok }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : expected \"=\" : have \"{\"")
    }
    @Test
    fun dd_02_if() {
        val l = lexer("if x=1 { x }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(1 thus { as x =>\n" +
                "if x {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n\n") { e.tostr() }
    }
    @Test
    fun dd_03_if() {
        val l = lexer("if x:X=1 { x }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(1 thus { as x :X =>\n" +
                "if x {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n\n") { e.tostr() }
    }
    @Test
    fun dd_04_if() {
        val l = lexer("if f() {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if f() {\n" +
                "nil\n" +
                "} else {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }

    // IF / =>

    @Test
    fun de_01_if() {
        val l = lexer("if false => 1 => 2")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "if false {\n" +
                "1\n" +
                "} else {\n" +
                "2\n" +
                "}\n") { e.tostr() }
    }
    @Test
    fun de_02_if() {
        val l = lexer("if false { 1 } => 2")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 16) : expected expression : have \"=>\"")
    }
    @Test
    fun de_03_if_err() {
        val l = lexer("(if true => 1)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 14) : expected \"=>\" : have \")\"")
    }
    @Test
    fun de_0x_if_err() {
        val l = lexer("if false => 1 --> nil => 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "ERR") { e.tostr() }
    }
    @Test
    fun de_0x_if() {
        val l = lexer("if false => 1 --> nil nil")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "OK") { e.tostr() }
    }

    // IFS

    @Test
    fun ee_01_ifs() {
        val l = lexer("ifs { a=>1 else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(nil thus { as ceu_5 =>\n" +
                "if a {\n" +
                "1\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun ee_02_ifs() {
        val l = lexer("ifs { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(nil thus { as ceu_5 =>\n" +
                "nil\n" +
                "})\n") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }
    @Test
    fun ee_03_ifs() {
        val l = lexer("ifs it=nil { else => it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(nil thus { as it =>\n" +
                "if true {\n" +
                "it\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun ee_04_ifs_err() {
        val l = lexer("ifs { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \"{\" : have \"}\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \"->\" : have \"}\"")
    }
    @Test
    fun ee_05_ifs() {
        val l = lexer("ifs it=v { a => it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(v thus { as it =>\n" +
                "if a {\n" +
                "it\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun ee_06_ifs() {
        val l = lexer("ifs it=v { a{1} b=>it else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(v thus { as it =>\n" +
                "if a {\n" +
                "1\n" +
                "} else {\n" +
                "if b {\n" +
                "it\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun ee_07_ifs() {
        val l = lexer("ifs { f() => nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(nil thus { as ceu_5 =>\n" +
                "if f() {\n" +
                "nil\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }

    // AS / YIELD / CATCH / DETRACK / THUS

    @Test
    fun ff_01_yield() {
        val l = lexer("yield()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "yield(nil) { as ceu_9 =>\n" +
                "ceu_9\n" +
                "\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ff_02_yield() {
        val l = lexer("yield() { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "yield(nil) { as it =>\n" +
                "nil\n" +
                "\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ff_03_catch() {
        val l = lexer("catch {} in {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch { as it => nil } in {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ff_04_detrack() {
        val l = lexer("detrack(nil) { x }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "detrack(nil) { as it =>\n" +
                "x\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ff_05_thus() {
        val l = lexer("f() thus { it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(f() thus { as it =>\n" +
                "it\n" +
                "})\n") { e.tostr() }
    }

    // RESUME-YIELD-ALL

    @Test
    fun gg_01_resume_yield_all_err() {
        val l = lexer("resume-yield-all f")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 18) : resume-yield-all error : expected call")
    }
    @Test
    fun gg_02_resume_yield_all_err() {
        val l = lexer("resume-yield-all f(1,2)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 18) : resume-yield-all error : invalid number of arguments")
    }
    @Test
    fun gg_03_resume_yield_all() {
        val l = lexer("resume-yield-all f()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(f thus { as ceu_co_10 =>\n" +
                "var ceu_arg_10 = nil\n" +
                "loop {\n" +
                "break if (resume (ceu_co_10)(ceu_arg_10) thus { as ceu_v_10 =>\n" +
                "if ({{/=}}(status(ceu_co_10),:terminated) thus { as ceu_59 =>\n" +
                "if ceu_59 {\n" +
                "ceu_59\n" +
                "} else {\n" +
                "{{/=}}(ceu_v_10,nil)\n" +
                "}\n" +
                "})\n" +
                " {\n" +
                "set ceu_arg_10 = yield(ceu_v_10) { as ceu_134 =>\n" +
                "ceu_134\n" +
                "\n" +
                "}\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "{{==}}(status(ceu_co_10),:terminated)\n" +
                "})\n" +
                "\n" +
                "}\n" +
                "ceu_arg_10\n" +
                "})\n") { e.tostr() }
    }

    // SPAWN

    @Test
    fun hh_01_spawn_task() {
        val l = lexer("spawn task {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "spawn (task () :void {\n" +
                "nil\n" +
                "})(nil)") { e.tostr() }
    }
    @Test
    fun hh_02_spawn_coro() {
        val l = lexer("spawn coro {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "spawn (coro () {\n" +
                "nil\n" +
                "})(nil)") { e.tostr() }
    }

    // PAR / PAR-OR

    @Test
    fun ii_01_par() {
        val l = lexer("par {} with {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "spawn (task () :void {\n" +
                "nil\n" +
                "})(nil)\n" +
                "spawn (task () :void {\n" +
                "nil\n" +
                "})(nil)\n" +
                "loop {\n" +
                "yield(nil) { as ceu_90 =>\n" +
                "ceu_90\n" +
                "\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ii_02_paror() {
        val l = lexer("par-or {} with {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "val ceu_0_9 = spawn (task () :void {\n" +
                "nil\n" +
                "})(nil)\n" +
                "val ceu_1_9 = spawn (task () :void {\n" +
                "nil\n" +
                "})(nil)\n" +
                "loop {\n" +
                "break if (({{==}}(status(ceu_0_9),:terminated) thus { as ceu_111 =>\n" +
                "if ceu_111 {\n" +
                "(pub(ceu_0_9) thus { as ceu_120 =>\n" +
                "if ceu_120 {\n" +
                "ceu_120\n" +
                "} else {\n" +
                "true\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "} else {\n" +
                "ceu_111\n" +
                "}\n" +
                "})\n" +
                " thus { as ceu_232 =>\n" +
                "if ceu_232 {\n" +
                "ceu_232\n" +
                "} else {\n" +
                "(({{==}}(status(ceu_1_9),:terminated) thus { as ceu_251 =>\n" +
                "if ceu_251 {\n" +
                "(pub(ceu_1_9) thus { as ceu_260 =>\n" +
                "if ceu_260 {\n" +
                "ceu_260\n" +
                "} else {\n" +
                "true\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "} else {\n" +
                "ceu_251\n" +
                "}\n" +
                "})\n" +
                " thus { as ceu_372 =>\n" +
                "if ceu_372 {\n" +
                "ceu_372\n" +
                "} else {\n" +
                "false\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "yield(nil) { as ceu_682 =>\n" +
                "ceu_682\n" +
                "\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ii_03_parand() {
        val l = lexer("par-and {} with {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "val ceu_0_9 = spawn (task () :void {\n" +
                "nil\n" +
                "})(nil)\n" +
                "val ceu_1_9 = spawn (task () :void {\n" +
                "nil\n" +
                "})(nil)\n" +
                "loop {\n" +
                "break(nil) if ({{==}}(status(ceu_0_9),:terminated) thus { as ceu_114 =>\n" +
                "if ceu_114 {\n" +
                "({{==}}(status(ceu_1_9),:terminated) thus { as ceu_131 =>\n" +
                "if ceu_131 {\n" +
                "true\n" +
                "} else {\n" +
                "ceu_131\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "} else {\n" +
                "ceu_114\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "yield(nil) { as ceu_272 =>\n" +
                "ceu_272\n" +
                "\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ii_04_par_err() {
        val l = lexer("par {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected \"with\" : have end of file")
    }

    // AWAIT

    @Test
    fun jj_01_await() {
        val l = lexer("await()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "break if yield(nil) { as ceu_7 =>\n" +
                "ceu_7\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jj_02_await() {
        val l = lexer("await(x)")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "break if yield(nil) { as ceu_9 =>\n" +
                "is'(ceu_9,x)\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jj_03_await() {
        val l = lexer("await { it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "break if yield(nil) { as it =>\n" +
                "it\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jj_04_await() {
        val l = lexer("await(:X) { as x:X => x.x>2 }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "break if yield(nil) { as x :X =>\n" +
                "(is'(x,:X) thus { as ceu_63 =>\n" +
                "if ceu_63 {\n" +
                "{{>}}(x[:x],2)\n" +
                "} else {\n" +
                "ceu_63\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jj_05_task_err() {
        val l = lexer("await spawn T() in ts")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : await error : expected non-pool spawn")
    }
    @Test
    fun jj_06_task() {
        val l = lexer("await spawn T()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "val ceu_14 = spawn T(nil)\n" +
                "loop {\n" +
                "break(pub(ceu_14)) if {{==}}(status(ceu_14),:terminated)\n" +
                "yield(nil) { as ceu_65 =>\n" +
                "ceu_65\n" +
                "\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
}
