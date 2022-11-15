package xceu

import ceu.lexer
import org.junit.Test

class TXLexer {
    @Test
    fun ids2() {
        val l = lexer("and or not")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "and" })
        assert(tks.next().let { it is Tk.Fix && it.str == "or" })
        assert(tks.next().let { it is Tk.Fix && it.str == "not" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

}
