import org.junit.Test
import java.io.PushbackReader
import java.io.StringReader

class TParser {
    @Test
    fun a01_expr_var() {
        val lexer = Lexer(PushbackReader(" x ".reader(),2))
        val parser = Parser(lexer)
        val e = parser.expr()
        assert(e is Expr.Var && e.tk.str == "x")
    }
}
