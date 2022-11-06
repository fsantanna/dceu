import org.junit.Test
import java.io.PushbackReader
import java.io.StringReader

fun trap (f: ()->Unit): String {
    try {
        f()
        error("impossible case")
    } catch (e: Throwable) {
        return e.message!!
    }

}
class TParser {
    @Test
    fun a01_expr_var () {
        val lexer = Lexer("anon", PushbackReader(" x ".reader(),2))
        val parser = Parser(lexer)
        val e = parser.expr()
        assert(e is Expr.Var && e.tk.str == "x")
    }
    @Test
    fun a02_expr_var_err () {
        val lexer = Lexer("anon", PushbackReader(" { ".reader(),2))
        val parser = Parser(lexer)
        assert(trap { parser.expr() } == "anon: (ln 1, col 2): expected expression : have {")
    }
}
