package tst_50

import dceu.*
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_50 {
    @Test
    fun aa_01_ids() {
        val l = lexer("val' var' drop")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "val'" })
        assert(tks.next().let { it is Tk.Fix && it.str == "var'" })
        assert(tks.next().let { it is Tk.Fix && it.str == "drop" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
