package xceu

import Expr
import N
import Parser
import ceu.lexer
import ceu.trap
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
        assert(e.tostr() == "if true {\n1\n} else {\nnil\n}") { e.tostr() }
    }
    @Test
    fun empty2_do() {  // set whole tuple?
        val l = lexer("do{}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Do && e.es.size==1)
        assert(e.tostr() == "do {\nnil\n}") { e.tostr() }
    }
    @Test
    fun empty3_func() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Proto && e.args.size==0)
        assert(e.tostr() == "func () {\nnil\n}") { e.tostr() }
    }
    @Test
    fun empty4_while() {
        val l = lexer("while true { }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.While && e.body.es[0] is Expr.Nil)
        assert(e.tostr() == "while true {\nnil\n}") { e.tostr() }
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
        assert(e.tostr() == "if a {\n1\n} else {\nnil\n}") { e.tostr() }
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
        assert(e.tostr() == "do {\nvar _ceu_5\nset _ceu_5 = 1\nif _ceu_5 {\n_ceu_5\n} else {\n2\n}\n}") { e.tostr() }
    }
    @Test
    fun bin2_and() {
        N = 1
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nvar _ceu_5\nset _ceu_5 = 1\nif _ceu_5 {\n2\n} else {\n_ceu_5\n}\n}") { e.tostr() }
    }
    @Test
    fun bin3_not_or_and() {
        N = 1
        val l = lexer("((not true) and false) or true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            var _ceu_75
            set _ceu_75 = do {
            var _ceu_26
            set _ceu_26 = if true {
            false
            } else {
            true
            }
            if _ceu_26 {
            false
            } else {
            _ceu_26
            }
            }
            if _ceu_75 {
            _ceu_75
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
        assert(e.tostr() == "spawn task () :fake :awakes {\n1\n}()") { e.tostr() }
    }
}
