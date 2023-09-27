package tst_02

import dceu.*
import org.junit.BeforeClass
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_02 {
    @Test
    fun aa_01_ids() {
        val l =
            lexer("defer catch throw it")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "defer" })
        assert(tks.next().let { it is Tk.Fix && it.str == "catch" })
        assert(tks.next().let { it is Tk.Id  && it.str == "throw" })
        assert(tks.next().let { it is Tk.Id  && it.str == "it" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
