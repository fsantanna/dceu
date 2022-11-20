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
        assert(e is Expr.XSeq && e.es[1] is Expr.Set)
    }
    @Test
    fun dcl2() {
        val l = lexer("do { var x = 1 }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nvar x\nset x = 1\n}\n") { e.tostr() }
    }

    // EMPTY BLOCKS

    @Test
    fun empty1_if() {
        val l = lexer("if true { 1 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n}\nelse {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun empty2_do() {  // set whole tuple?
        val l = lexer("do{}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Block && e.es.size==1)
        assert(e.tostr() == "do {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun empty3_func() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Func && e.args.size==0)
        assert(e.tostr() == "func () {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun empty4_while() {
        val l = lexer("while true { }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.While && e.body.es[0] is Expr.Nil)
        assert(e.tostr() == "while true {\nnil\n}\n") { e.tostr() }
    }
    @Test
    @Ignore
    fun todo_if3_noblk() {
        val l = lexer("if true 1")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true 1 else nil\n") { e.tostr() }
    }
    @Test
    @Ignore
    fun todo_if4_noblk() {
        val l = lexer("if true 1 else 2")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true 1 else 2\n") { e.tostr() }
    }

    // IFS

    @Test
    fun ifs1() {
        val l = lexer("ifs { a {1} else {0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if a {\n1\n}\nelse {\n0\n}\n") { e.tostr() }
    }
    @Test
    fun ifs2_err() {
        val l = lexer("ifs { }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }
    @Test
    fun ifs3_err() {
        val l = lexer("ifs { else {} }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"else\"")
    }
    @Test
    fun ifs4_err() {
        val l = lexer("ifs { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \"{\" : have \"}\"")
    }
    @Test
    fun ifs5() {
        val l = lexer("ifs { a {1} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if a {\n1\n}\nelse {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun ifs6() {
        val l = lexer("ifs { a{1} b{2} else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if a {\n1\n}\nelse {\nif b {\n2\n}\nelse {\n0\n}\n\n}\n") { e.tostr() }
    }

    // BIN AND OR

    @Test
    fun bin1_or() {
        N = 1
        val l = lexer("1 or 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nvar ceu_1\nset ceu_1 = 1\nif ceu_1 {\nceu_1\n}\nelse {\n2\n}\n\n}\n") { e.tostr() }
    }
    @Test
    fun bin2_and() {
        N = 1
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nvar ceu_1\nset ceu_1 = 1\nif ceu_1 {\n2\n}\nelse {\nceu_1\n}\n\n}\n") { e.tostr() }
    }
    @Test
    fun bin3_not_or_and() {
        N = 1
        val l = lexer("not true and false or true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            var ceu_24
            set ceu_24 = do {
            var ceu_7
            set ceu_7 = if true {
            false
            }
            else {
            true
            }
            
            if ceu_7 {
            false
            }
            else {
            ceu_7
            }
            
            }
            
            if ceu_24 {
            ceu_24
            }
            else {
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
        assert(e.tostr() == "{-}(if {-}(1) {\nfalse\n}\nelse {\ntrue\n}\n)") { e.tostr() }
    }

    // PAR

    @Test
    fun par1() {
        val l = lexer("""
            par {
                1
            } with {
                2
            } with {
                3
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        println(e.tostr())
        assert(e.tostr() == "do {\nspawn task () {\n1\n}\n()\nspawn task () {\n2\n}\n()\nspawn task () {\n3\n}\n()\nwhile true {\nyield nil\n}\n\n}\n") { e.tostr() }
    }
    @Test
    fun par2_err() {
        val l = lexer("par {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected \"with\" : have end of file")
    }

    // CATCH

    @Test
    fun todo_catch1() {
        val l = lexer("""
            set x = catch #e1 {
                throw #e1
                throw (#e1,10)
                throw (#e1)
            }
            
        """)
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e.tostr() == "set x = catch 1 {\nthrow (1,nil)\nthrow (1,10)\nthrow (1,nil)\n}\n") { e.tostr() }
    }

}
