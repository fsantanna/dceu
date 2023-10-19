package tst_99

import dceu.*
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_99 {
    @Test
    fun aa_01_ops() {
        val l =
            lexer("and or not in? is? in-not? is-not?")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Op && it.str == "and" })
        assert(tks.next().let { it is Tk.Op && it.str == "or" })
        assert(tks.next().let { it is Tk.Op && it.str == "not" })
        assert(tks.next().let { it is Tk.Op && it.str == "in?" })
        assert(tks.next().let { it is Tk.Op && it.str == "is?" })
        assert(tks.next().let { it is Tk.Op && it.str == "in-not?" })
        assert(tks.next().let { it is Tk.Op && it.str == "is-not?" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
