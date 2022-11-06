import java.io.PushbackReader

class Parser (lexer: Lexer)
{
    var tk0: Tk = Tk.Err("", 1, 1)
    var tk1: Tk = Tk.Err("", 1, 1)
    val tks: Iterator<Tk>

    init {
        this.tks = lexer.lex().iterator()
        this.lexer()
    }

    fun lexer () {
        this.tk0 = tk1
        this.tk1 = tks.next()
    }

    fun check (enu: String): Boolean {
        return when (enu) {
            "Id"  -> this.tk1 is Tk.Id
            else  -> error("bug found")
        }
    }
    fun accept (enu: String): Boolean {
        val ret = this.check(enu)
        if (ret) {
            this.lexer()
        }
        return ret
    }

    fun expr (): Expr {
        return when {
            (this.accept("Id")) -> Expr.Var(this.tk0 as Tk.Id)
            else -> {
                TODO("expected expression")
            }
        }
    }
}
