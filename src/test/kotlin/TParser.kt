import org.junit.Test

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
        val lexer = Lexer("anon", " x ".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Var && e.tk.str == "x")
    }
    @Test
    fun a02_expr_var_err () {
        val lexer = Lexer("anon", " { ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.expr1() } == "anon: (ln 1, col 2): expected expression : have \"{\"")
    }
    @Test
    fun a03_expr_var_err () {
        val lexer = Lexer("anon", "  ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.expr1() } == "anon: (ln 1, col 3): expected expression : have end of file")
    }

    // EXPR.PARENS

    @Test
    fun a04_expr_parens() {
        val lexer = Lexer("anon", " ( a ) ".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Var && e.tk.str == "a")
    }
    @Test
    fun a05_expr_parens_err() {
        val lexer = Lexer("anon", " ( a  ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.expr1() } == "anon: (ln 1, col 7): expected \")\" : have end of file")
    }

    // EXPR.NUM

    @Test
    fun a06_expr_num() {
        val lexer = Lexer("anon", " 1.5F ".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }

    // EXPR.ECALL

    @Test
    fun a07_expr_call() {
        val lexer = Lexer("anon", " f (1.5F, x) ".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.ECall && e.tk.str=="f" && e.f is Expr.Var && e.args.size==2)
    }
    @Test
    fun a08_expr_call() {
        val lexer = Lexer("anon", " f() ".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.ECall && e.f.tk.str=="f" && e.f is Expr.Var && e.args.size==0)
    }
    @Test
    fun a09_expr_call() {
        val lexer = Lexer("anon", " f(x,8)() ".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.ECall && e.f is Expr.ECall && e.args.size==0)
        assert(e.tostr() == "f(x,8)()")
    }
    @Test
    fun a10_expr_call_err() {
        val lexer = Lexer("anon", "f (999 ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.exprN() } == "anon: (ln 1, col 8): expected \")\" : have end of file")
    }
    @Test
    fun a11_expr_call_err() {
        val lexer = Lexer("anon", " f ({ ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.exprN() } == "anon: (ln 1, col 5): expected expression : have \"{\"")
    }

    // EXPR.TUPLE

    @Test
    fun a12_expr_tuple() {
        val lexer = Lexer("anon", " [ 1.5F, x] ".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.Tuple && e.args.size==2)
    }
    @Test
    fun a13_expr_tuple() {
        val lexer = Lexer("anon", "[[],[1,2,3]]".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e.tostr() == "[[],[1,2,3]]")
    }
    @Test
    fun a14_expr_tuple_err() {
        val lexer = Lexer("anon", "[{".reader())
        val parser = Parser(lexer)
        assert(trap { parser.exprN() } == "anon: (ln 1, col 2): expected expression : have \"{\"")
    }

    // EXPR.INDEX

    @Test
    fun a15_expr_index() {
        val lexer = Lexer("anon", "x[10]".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.Index && e.col is Expr.Var && e.idx is Expr.Num)
    }
    @Test
    fun a16_expr_index_err() {
        val lexer = Lexer("anon", "x[10".reader())
        val parser = Parser(lexer)
        assert(trap { parser.exprN() } == "anon: (ln 1, col 5): expected \"]\" : have end of file")
    }

    // STMT.SCALL

    @Test
    fun b01_stmt_call() {
        val lexer = Lexer("anon", "call f ()".reader())
        val parser = Parser(lexer)
        val s = parser.stmt()
        assert(s is Stmt.SCall && s.e.tostr() == "f()")
        assert(s.tostr() == "call f()\n")
    }
    @Test
    fun b02_stmt_call_err() {
        val lexer = Lexer("anon", "call f".reader())
        val parser = Parser(lexer)
        assert(trap { parser.stmt() } == "anon: (ln 1, col 6): expected call expression : have \"f\"")
    }

    // STMT.SEQ

    @Test
    fun b03_stmt_seq() {
        val lexer = Lexer("anon", ";; call f () call g () ; call h()\ncall i() ;\n;".reader())
        val parser = Parser(lexer)
        val s = parser.stmts()
        assert(s.tostr() == "call f()\ncall g()\ncall h()\ncall i()\n") { s.tostr() }
    }
}
