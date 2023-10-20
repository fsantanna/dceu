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
        assert(e.tostr() == "(1 thus { as ceu_3 =>\n" +
                "if ceu_3 {\n" +
                "ceu_3\n" +
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
        assert(e.tostr() == "(1 thus { as ceu_3 =>\n" +
                "if ceu_3 {\n" +
                "2\n" +
                "} else {\n" +
                "ceu_3\n" +
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
            } thus { as ceu_17 =>
            if ceu_17 {
            false
            } else {
            ceu_17
            }
            })
             thus { as ceu_41 =>
            if ceu_41 {
            ceu_41
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
        assert(e.tostr() == "if a {\n" +
                "1\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ee_02_ifs() {
        val l = lexer("ifs { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(nil thus { as ceu_3 =>\n" +
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
        assert(e.tostr() == "if f() {\n" +
                "nil\n" +
                "} else {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }

    // YIELD

    @Test
    fun ff_01_yield() {
        val l = lexer("yield()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "yield(nil) { as ceu_7 =>\n" +
                "ceu_7\n" +
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

    // RESUME-YIELD-ALL

    @Test
    fun gg_01_resume_yield_all_err() {
        val l = lexer("resume-yield-all f")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 18) : resume-yield-call error : expected call")
    }
    @Test
    fun gg_02_resume_yield_all_err() {
        val l = lexer("resume-yield-all f(1,2)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 18) : resume-yield-call error : invalid number of arguments")
    }
    @Test
    fun gg_03_resume_yield_all() {
        val l = lexer("resume-yield-all f()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "TODO") { e.tostr() }
    }
}
