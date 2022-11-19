package xceu

import Expr
import Parser
import ceu.lexer
import org.junit.Ignore
import org.junit.Test
import tostr

class TXParser {
    @Test
    fun expr_if2() {  // set whole tuple?
        val l = lexer("if true { 1 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n}\nelse {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun expr_do1() {  // set whole tuple?
        val l = lexer("do{}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Block && e.es.size==1)
        assert(e.tostr() == "do {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun expr_func1() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Func && e.args.size==0)
        assert(e.tostr() == "func () {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun expr_while1() {
        val l = lexer("while true { }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.While && e.body.es[0] is Expr.Nil)
        assert(e.tostr() == "while true {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun pre1() {
        val l = lexer("- not - 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{-}(if {-}(1) {\nfalse\n}\nelse {\ntrue\n}\n)") { e.tostr() }
    }

    // TODO

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

    // BIN AND OR

    @Test
    fun bin1_or() {
        val l = lexer("1 or 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nvar ceu_1\nset ceu_1 = 1\nif ceu_1 {\nceu_1\n}\nelse {\n2\n}\n\n}\n") { e.tostr() }
    }
    @Test
    fun bin2_and() {
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\nvar ceu_1\nset ceu_1 = 1\nif ceu_1 {\n2\n}\nelse {\nceu_1\n}\n\n}\n") { e.tostr() }
    }
    @Test
    fun bin3_not_or_and() {
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
    @Ignore
    fun todo_expr_if3_noblk() {
        val l = lexer("if true 1")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true 1 else nil\n") { e.tostr() }
    }
    @Test
    @Ignore
    fun todo_expr_if4_noblk() {
        val l = lexer("if true 1 else 2")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true 1 else 2\n") { e.tostr() }
    }
}
