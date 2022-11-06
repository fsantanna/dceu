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

    // EXPR.VAR

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
        assert(trap { parser.expr() } == "anon: (ln 1, col 2): expected expression : have \"{\"")
    }
    @Test
    fun a03_expr_var_err () {
        val lexer = Lexer("anon", PushbackReader("  ".reader(),2))
        val parser = Parser(lexer)
        assert(trap { parser.expr() } == "anon: (ln 1, col 3): expected expression : have end of file")
    }

    // EXPR.PARENS

    @Test
    fun a04_expr_parens() {
        val lexer = Lexer("anon", PushbackReader(" ( a ) ".reader(),2))
        val parser = Parser(lexer)
        val e = parser.expr()
        assert(e is Expr.Var && e.tk.str == "a")
    }
    @Test
    fun a05_expr_parens_err() {
        val lexer = Lexer("anon", PushbackReader(" ( a  ".reader(),2))
        val parser = Parser(lexer)
        assert(trap { parser.expr() } == "anon: (ln 1, col 7): expected \")\" : have end of file")
    }

    // EXPR.NUM

    @Test
    fun a06_expr_num() {
        val lexer = Lexer("anon", PushbackReader(" 1.5F ".reader(),2))
        val parser = Parser(lexer)
        val e = parser.expr()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }

    // EXPR.ECALL

    @Test
    fun a07_expr_call() {
        val lexer = Lexer("anon", PushbackReader(" f (1.5F, x) ".reader(),2))
        val parser = Parser(lexer)
        val e = parser.expr()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }

    // STMT
}
