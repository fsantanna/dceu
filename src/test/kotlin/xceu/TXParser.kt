package xceu

import dceu.*
import ceu.lexer
import ceu.trap
import org.junit.Ignore
import org.junit.Test

class TXParser {
    // DCL + SET
    @Test
    fun dcl1() {
        val l = lexer("var x = 1")
        val parser = Parser(l)
        val e = parser.expr_prim()
        //println(e)
        //assert(e is Expr.Group && e.es[1] is Expr.Set)
        assert(e is Expr.Dcl && e.src is Expr.Num)
    }
    @Test
    fun dcl2() {
        val l = lexer("do { var x = 1 }")
        val parser = Parser(l)
        val e = parser.expr()
        //assert(e.tostr() == "do {\ngroup {\nvar x\nset x = 1\n}\n}") { e.tostr() }
        assert(e.tostr() == "do {\nvar x = 1\n}") { e.tostr() }
    }

    // EMPTY BLOCKS

    @Test
    fun empty1_if() {
        val l = lexer("if true { 1 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n} else {\nnil\n}") { e.tostr() }
    }
    @Test
    fun empty2_do() {  // set whole tuple?
        val l = lexer("do{}")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Do && e.es.size==1)
        assert(e.tostr() == "do {\nnil\n}") { e.tostr() }
    }
    @Test
    fun empty3_func() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.args.size==0)
        assert(e.tostr() == "(func () {\nnil\n})") { e.tostr() }
    }
    @Test
    fun empty4_loop() {
        val l = lexer("loop until false { }")
        val parser = Parser(l)
        val x = parser.expr_prim() as Expr.Do
        val e = x.es.last() as Expr.Loop
        assert(e.body.es.last() is Expr.Nil)
        //assert(e.tostr() == "loop until false {\npass nil\n}") { e.tostr() }
    }

    // IFS

    @Test
    fun ifs1() {
        val l = lexer("ifs { a->1 else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "if a {\n" +
                "1\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ifs2_err() {
        val l = lexer("ifs { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "nil\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }
    @Test
    fun ifs3_err() {
        val l = lexer("ifs it=nil { else -> it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "val :xtmp it = nil\n" +
                "if true {\n" +
                "it\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ifs4_err() {
        val l = lexer("ifs { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \"{\" : have \"}\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \"->\" : have \"}\"")
    }
    @Test
    fun ifs5() {
        val l = lexer("ifs it=v { a -> it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "val :xtmp it = v\n" +
                "if a {\n" +
                "it\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ifs6() {
        val l = lexer("ifs it=v { a{1} b->it else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "val :xtmp it = v\n" +
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
                "}") { e.tostr() }
    }

    // BIN AND OR

    @Test
    fun bin1_or() {
        N = 1
        val l = lexer("1 or 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nval :xtmp ceu_5 = 1\nif ceu_5 {\nceu_5\n} else {\n2\n}\n}") { e.tostr() }
    }
    @Test
    fun bin2_and() {
        N = 1
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nval :xtmp ceu_5 = 1\nif ceu_5 {\n2\n} else {\nceu_5\n}\n}") { e.tostr() }
    }
    @Test
    fun bin3_not_or_and() {
        N = 1
        val l = lexer("((not true) and false) or true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            val :xtmp ceu_73 = do {
            val :xtmp ceu_26 = if true {
            false
            } else {
            true
            }
            if ceu_26 {
            false
            } else {
            ceu_26
            }
            }
            if ceu_73 {
            ceu_73
            } else {
            true
            }
            }
        """.trimIndent()) { e.tostr() }
    }
    @Test
    fun pre1() {
        val l = lexer("- not - 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{{-}}(if {{-}}(1) {\nfalse\n} else {\ntrue\n})") { e.tostr() }
    }

    // TUPLE / INDEX

    @Test
    fun index1() {
        N = 1
        val l = lexer("x.1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "x[1]") { e.tostr() }
    }
    @Test
    fun index2() {
        val l = lexer("x . a")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Index && e.col is Expr.Acc && e.idx is Expr.Tag)
        assert(e.tostr() == "x[:a]") { e.tostr() }
    }
    @Test
    fun index3_err() {
        val l = lexer("x . .")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected field : have \".\"")
    }

    // SPAWN, PAR, RESUME-YIELD-ALL

    @Test
    fun par2_err() {
        val l = lexer("par {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected \"with\" : have end of file")
    }
    @Test
    fun spawn3() {
        val l = lexer("""
            spawn {
                1
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "spawn (task () :fake {\n1\n})()") { e.tostr() }
    }
    @Test
    fun kk_03_resume_yield() {
        val l = lexer("resume-yield-all f")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 18) : resume-yield-call error : expected call")
    }

    // ITER

    @Ignore // N ids vary
    @Test
    fun loop01_n() {
        val l = lexer("""
            loop in [0 -> n), :step +1, i {
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            var ceu_step_27 = 1
            var i = {{+}}(0,0)
            var ceu_limit_27 = n
            loop until {{==}}(i,ceu_limit_27) {
            nil
            set i = {{+}}(i,ceu_step_27)
            }
            }
            """.trimIndent()) { e.tostr() }
    }

    // POLY
    /*
    @Test
    fun gg_05_poly() {
        val l = lexer("poly func f () {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : poly error : expected declaration")
    }
    */
}
