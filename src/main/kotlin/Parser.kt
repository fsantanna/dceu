class Parser (lexer_: Lexer)
{
    val lexer = lexer_
    var tk0: Tk = Tk.Err("", 1, 1)
    var tk1: Tk = Tk.Err("", 1, 1)
    val tks: Iterator<Tk>

    init {
        this.tks = this.lexer.lex().iterator()
        this.lex()
    }

    fun lex () {
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
            this.lex()
        }
        return ret
    }

    fun err_expected (str: String): Boolean {
        error(this.lexer.name + ": (ln ${this.tk1.lin}, col ${this.tk1.col}): expected $str : have ${this.tk1.str}")
    }

    fun expr (): Expr {
        return when {
            (this.accept("Id")) -> Expr.Var(this.tk0 as Tk.Id)
            else -> {
                this.err_expected("expression")
                error("unreachable")
            }
        }
    }
}
