package tst_01

import dceu.*
import org.junit.Test

class Parser_01 {
    // NATIVE

    @Test
    fun native0() {
        val l = lexer("`a`")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.to_str() == "```a```") { e.to_str() }
    }
    @Test
    fun native1() {
        val l = lexer(
            """
            ```
                printf("xxx\n");
            ```
        """.trimIndent()
        )
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Nat)
        assert(e.to_str() == "```\n    printf(\"xxx\\n\");\n```") { "."+e.to_str()+"." }
    }
    @Test
    fun native2_err() {
        val l = lexer(
            """
            native ``
                printf("xxx\n");
            ```
        """.trimIndent()
        )
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 3, col 1) : native throw : expected \"``\"")
    }
    @Test
    fun native3_err() {
        val l = lexer(
            """
            native ``
                printf("xxx\n");
        """
        )
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 4, col 9) : native throw : expected \"``\"")
    }
    @Test
    fun native4_err() {
        val l = lexer(
            """
            native `:
        """.trimIndent()
        )
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : tag throw : expected identifier")
    }
    @Test
    fun native5() {
        val l = lexer(
            """
            ```:ola```
        """.trimIndent()
        )
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Nat)
        assert(e.to_str() == "```:ola ```") { "."+e.to_str()+"." }
    }


    // TEMPLATE

    @Test
    fun tplate00() {
        val l = lexer(
            """
            data :T = [x,y]
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "(data :T = [x,y]);\n") { e.to_str() }
    }
    @Test
    fun tplate01() {
        val l = lexer(
            """
            var t :T = [1,2]
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "(var t :T = [1,2]);\n") { e.to_str() }
    }
    @Test
    fun tplate02_err() {
        val l = lexer(
            """
            data X [x,y]
        """
        )
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 18) : expected tag : have \"X\"")
    }
    @Test
    fun tplate03_err() {
        val l = lexer(
            """
            data :X [x,y]
        """
        )
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 21) : expected \"=\" : have \"[\"")
    }
    @Test
    fun tplate04_err() {
        val l = lexer(
            """
            data :X = nil
        """
        )
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected \"[\" : have \"nil\"")
    }
    @Test
    fun tplate05_err() {
        val l = lexer(
            """
            data :X = [1,2]
        """
        )
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 24) : expected identifier : have \"1\"")
    }
    @Test
    fun tplate06() {
        val l = lexer(
            """
            data :U = [t:T]
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "(data :U = [t :T]);\n") { e.to_str() }
    }
}
