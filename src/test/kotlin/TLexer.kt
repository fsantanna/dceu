import org.junit.Test

class TLexer {
    @Test
    fun a01_syms () {
        val tks = lexer("{ } ( ( ) )".reader()).iterator()
        assert(tks.next().str == "{")
        assert(tks.next().str == "}")
        assert(tks.next().str == "(")
        assert(tks.next().str == "()")
        assert(tks.next().str == ")")
        assert(!tks.hasNext())
    }
}
