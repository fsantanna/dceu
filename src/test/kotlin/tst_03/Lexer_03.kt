package tst_03

import dceu.*
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_03 {
    @Test
    fun aa_01_ids() {
        val l =
            lexer("coro coroutine spawn yield resume")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "coro" })
        assert(tks.next().let { it is Tk.Id  && it.str == "coroutine" })
        assert(tks.next().let { it is Tk.Id  && it.str == "spawn" })
        assert(tks.next().let { it is Tk.Fix && it.str == "yield" })
        assert(tks.next().let { it is Tk.Fix && it.str == "resume" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
