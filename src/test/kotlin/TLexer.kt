import org.junit.Test
import java.io.PushbackReader

class TLexer {
    @Test
    fun a01_syms () {
        val lexer = Lexer("anon", PushbackReader("{ } ( ( ) ) , ".reader(),2))
        val tks = lexer.lex().iterator()
        assert(tks.next().str == "{")
        assert(tks.next().str == "}")
        assert(tks.next().str == "(")
        assert(tks.next().str == "(")
        assert(tks.next().str == ")")
        assert(tks.next().str == ")")
        assert(tks.next().str == ",")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun a02_ids () {
        val lexer = Lexer("anon", PushbackReader(" if aaa XXX y10".reader(),2))
        val tks = lexer.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str=="if" })
        assert(tks.next().str == "aaa")
        assert(tks.next().str == "XXX")
        assert(tks.next().str == "y10")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
