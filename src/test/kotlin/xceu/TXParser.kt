package xceu

import Expr
import N
import Parser
import ceu.lexer
import ceu.trap
import org.junit.Ignore
import org.junit.Test
import tostr

class TXParser {
    // DCL + SET
    @Test
    fun dcl1() {
        val l = lexer("var x = 1")
        val parser = Parser(l)
        val e = parser.exprPrim()
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
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n} else {\npass nil\n}") { e.tostr() }
    }
    @Test
    fun empty2_do() {  // set whole tuple?
        val l = lexer("do{}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Do && e.es.size==1)
        assert(e.tostr() == "do {\npass nil\n}") { e.tostr() }
    }
    @Test
    fun empty3_func() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Proto && e.args.size==0)
        assert(e.tostr() == "func () {\npass nil\n}") { e.tostr() }
    }
    @Test
    fun empty4_while() {
        val l = lexer("while true { }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.While && e.body.es[0] is Expr.Pass)
        assert(e.tostr() == "while true {\npass nil\n}") { e.tostr() }
    }

    // IFS

    @Test
    fun ifs1() {
        val l = lexer("ifs { a->{1} else->{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if a {\n1\n} else {\n0\n}") { e.tostr() }
    }
    @Test
    fun ifs2_err() {
        val l = lexer("ifs { }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }
    @Test
    fun ifs3_err() {
        val l = lexer("ifs { else -> {} }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"else\"")
    }
    @Test
    fun ifs4_err() {
        val l = lexer("ifs { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \"->\" : have \"}\"")
    }
    @Test
    fun ifs5() {
        val l = lexer("ifs { a -> {1} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if a {\n1\n} else {\npass nil\n}") { e.tostr() }
    }
    @Test
    fun ifs6() {
        val l = lexer("ifs { a->{1} b->2 else->{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if a {\n1\n} else {\nif b {\n2\n} else {\n0\n}\n}") { e.tostr() }
    }

    // BIN AND OR

    @Test
    fun bin1_or() {
        N = 1
        val l = lexer("1 or 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nvar ceu_5 :tmp\nset ceu_5 = 1\nif ceu_5 {\nceu_5\n} else {\n2\n}\n}") { e.tostr() }
    }
    @Test
    fun bin2_and() {
        N = 1
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nvar ceu_5 :tmp\nset ceu_5 = 1\nif ceu_5 {\n2\n} else {\nceu_5\n}\n}") { e.tostr() }
    }
    @Test
    fun bin3_not_or_and() {
        N = 1
        val l = lexer("((not true) and false) or true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            var ceu_76 :tmp
            set ceu_76 = do {
            var ceu_26 :tmp
            set ceu_26 = if true {
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
            if ceu_76 {
            ceu_76
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
        assert(e.tostr() == "{-}(if {-}(1) {\nfalse\n} else {\ntrue\n})") { e.tostr() }
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
        assert(trap { parser.exprSufs() } == "anon : (lin 1, col 5) : expected field : have \".\"")
    }

    // SPAWN, PAR

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
        assert(e.tostr() == "spawn task () :fake {\n1\n}()") { e.tostr() }
    }

    // ITER

    @Ignore // N ids vary
    @Test
    fun while01_n() {
        val l = lexer("""
            while in [0 -> n), :step +1, i {
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            var ceu_step_25 = 1
            var i = {+}(0,0)
            var ceu_limit_25 = n
            while {<}(i,ceu_limit_25) {
            nil
            set i = {+}(i,ceu_step_25)
            }
            }
            """.trimIndent()) { e.tostr() }
    }

    // POLY

    @Test
    fun gg_01_poly_err() {
        val l = lexer("poly x")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : poly error : expected var or set")
    }
    @Test
    fun gg_02_poly_err() {
        val l = lexer("poly var x = 1")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 12) : expected expression : have \"=\"")
    }
    @Test
    fun gg_03_poly() {
        val l = lexer("poly var x")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr(false) == "var x = @[]") { e.tostr() }
    }
    @Test
    fun gg_04_poly() {
        val l = lexer("poly set x = 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr(false) == "var x = @[]") { e.tostr() }
    }
    @Test
    fun gg_05_poly() {
        val l = lexer("poly func f () {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : poly error : expected declaration")
    }
}
