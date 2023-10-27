package tst_06

import dceu.*
import org.junit.Test

class Parser_06 {

    // EXPORT

    @Test
    fun aa_01_export_err() {
        val l = lexer("export {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : expected \"[\" : have \"{\"")
    }
    @Test
    fun aa_02_export_err() {
        val l = lexer("export [:x] {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected identifier : have \":x\"")
    }
    @Test
    fun aa_03_export() {
        val l = lexer("export [] { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Export && e.ids.isEmpty() && e.blk.es.size==1)
        assert(e.tostr() == "export [] {\nnil\n}") { e.tostr() }
    }
    @Test
    fun aa_04_export() {
        val l = lexer("export [x] { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Export && e.ids.first()=="x" && e.blk.es.size==1)
        assert(e.tostr() == "export [x] {\nnil\n}") { e.tostr() }
    }
    @Test
    fun aa_05_export() {
        val l = lexer("export [x,y] { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Export && e.ids.last()=="y" && e.blk.es.size==1 && e.ids.size==2)
        assert(e.tostr() == "export [x,y] {\nnil\n}") { e.tostr() }
    }
}
