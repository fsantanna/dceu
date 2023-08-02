package tmp

import dceu.*
import org.junit.Test

fun trap (f: ()->Unit): String {
    try {
        f()
        error("impossible case")
    } catch (e: Throwable) {
        return e.message!!
    }
}

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_02 {
    @Test
    fun ids() {
        val l =
            lexer("evt export pub task poly track enum coro defer err coroutine spawn yield while catch resume native broadcast data in")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.str == "evt" })
        assert(tks.next().let { it is Tk.Id  && it.str == "export" })
        assert(tks.next().let { it is Tk.Id  && it.str == "pub" })
        assert(tks.next().let { it is Tk.Id  && it.str == "task" })
        assert(tks.next().let { it is Tk.Id  && it.str == "poly" })
        assert(tks.next().let { it is Tk.Id  && it.str == "track" })
        assert(tks.next().let { it is Tk.Fix && it.str == "enum" })
        assert(tks.next().let { it is Tk.Id  && it.str == "coro" })
        assert(tks.next().let { it is Tk.Id  && it.str == "defer" })
        assert(tks.next().let { it is Tk.Id  && it.str == "err" })
        assert(tks.next().let { it is Tk.Id  && it.str == "coroutine" })
        assert(tks.next().let { it is Tk.Id  && it.str == "spawn" })
        assert(tks.next().let { it is Tk.Id  && it.str == "yield" })
        assert(tks.next().let { it is Tk.Id  && it.str == "while" })
        assert(tks.next().let { it is Tk.Id  && it.str == "catch" })
        assert(tks.next().let { it is Tk.Id  && it.str == "resume" })
        assert(tks.next().let { it is Tk.Id  && it.str == "native" })
        assert(tks.next().let { it is Tk.Id  && it.str == "broadcast" })
        assert(tks.next().let { it is Tk.Fix && it.str == "data" })
        assert(tks.next().let { it is Tk.Id  && it.str == "in" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun ids2() {
        val l = lexer("and or not")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Op && it.str == "and" })
        assert(tks.next().let { it is Tk.Op && it.str == "or" })
        assert(tks.next().let { it is Tk.Op && it.str == "not" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
