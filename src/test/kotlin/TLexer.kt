import org.junit.Test

class TLexer {
    @Test
    fun syms() {
        val lexer = Lexer("anon", "{ } ( ; ( = ) ) - , ][".reader())
        val tks = lexer.lex().iterator()
        assert(tks.next().str == "{")
        assert(tks.next().str == "}")
        assert(tks.next().str == "(")
        assert(tks.next().str == ";")
        assert(tks.next().str == "(")
        assert(tks.next().str == "=")
        assert(tks.next().str == ")")
        assert(tks.next().str == ")")
        assert(tks.next().str == "-")
        assert(tks.next().str == ",")
        assert(tks.next().str == "]")
        assert(tks.next().str == "[")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun ids() {
        val lexer = Lexer(
            "anon",
            " if aaa throw nil XXX set loop vary10 catch else var do ado true func b10 false break".reader()
        )
        val tks = lexer.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "if" })
        assert(tks.next().str == "aaa")
        assert(tks.next().str == "throw")
        assert(tks.next().str == "nil")
        assert(tks.next().str == "XXX")
        assert(tks.next().str == "set")
        assert(tks.next().str == "loop")
        assert(tks.next().str == "vary10")
        assert(tks.next().str == "catch")
        assert(tks.next().str == "else")
        assert(tks.next().str == "var")
        assert(tks.next().str == "do")
        assert(tks.next().str == "ado")
        assert(tks.next().str == "true")
        assert(tks.next().str == "func")
        assert(tks.next().str == "b10")
        assert(tks.next().str == "false")
        assert(tks.next().str == "break")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun comments() {
        val lexer = Lexer(
            "anon", """
            x - y - z --
            var --x
            --
            val --- x
            -- -
            -
        """.trimIndent().reader()
        )
        val tks = lexer.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.lin==1 && it.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Fix && it.lin==1 && it.col==3 && it.str == "-" })
        assert(tks.next().let { it is Tk.Id  && it.lin==1 && it.col==5 && it.str == "y" })
        assert(tks.next().let { it is Tk.Fix && it.lin==1 && it.col==7 && it.str == "-" })
        assert(tks.next().let { it is Tk.Id  && it.lin==1 && it.col==9 && it.str == "z" })
        assert(tks.next().let { it is Tk.Fix && it.lin==2 && it.col==1 && it.str == "var" })
        assert(tks.next().let { it is Tk.Id  && it.lin==4 && it.col==1 && it.str == "val" })
        assert(tks.next().let { it is Tk.Fix && it.lin==6 && it.col==1 && it.str == "-" })
        assert(tks.next().let { it is Tk.Eof && it.lin==6 && it.col==2 })
    }
}
