package tst_06

import dceu.*
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_06 {
    @Test
    fun aa_01_ids() {
        val l = lexer("export")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "export" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
