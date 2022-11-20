package xceu

import ceu.lexer
import org.junit.Test

val D = "\$"

class TXLexer {
    @Test
    fun ids2() {
        val l = lexer("and or not with ifs await par")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "and" })
        assert(tks.next().let { it is Tk.Fix && it.str == "or" })
        assert(tks.next().let { it is Tk.Fix && it.str == "not" })
        assert(tks.next().let { it is Tk.Fix && it.str == "with" })
        assert(tks.next().let { it is Tk.Fix && it.str == "ifs" })
        assert(tks.next().let { it is Tk.Fix && it.str == "await" })
        assert(tks.next().let { it is Tk.Fix && it.str == "par" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

}
