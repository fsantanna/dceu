package tst_05

import dceu.*
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_04 {
    @Test
    fun aa_01_ids() {
        val l =
            lexer("task broadcast in as tasks track detrack")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "task" })
        assert(tks.next().let { it is Tk.Fix && it.str == "broadcast" })
        assert(tks.next().let { it is Tk.Fix && it.str == "in" })
        assert(tks.next().let { it is Tk.Fix && it.str == "as" })
        assert(tks.next().let { it is Tk.Id && it.str == "tasks" })
        assert(tks.next().let { it is Tk.Id && it.str == "track" })
        assert(tks.next().let { it is Tk.Fix && it.str == "detrack" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
