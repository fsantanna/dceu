import org.junit.Test
import java.io.BufferedReader
import java.io.PushbackReader

class TLexer {
    @Test
    fun a01_syms () {
        val tks = lexer(PushbackReader("{ } ( ( ) ) ".reader())).iterator()
        assert(tks.next().str == "{")
        assert(tks.next().str == "}")
        assert(tks.next().str == "(")
        assert(tks.next().str == "(")
        assert(tks.next().str == ")")
        assert(tks.next().str == ")")
        assert(!tks.hasNext())
    }
    @Test
    fun a02_ids () {
        val tks = lexer(PushbackReader(" if aaa XXX y10".reader())).iterator()
        //println(tks.next())
        assert(tks.next().let { it is Tk.Fix && it.str=="if" })
        assert(tks.next().str == "aaa")
        assert(tks.next().str == "XXX")
        assert(tks.next().str == "y10")
        assert(!tks.hasNext())
    }
}
