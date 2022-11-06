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

    fun checkFix (str: String): Boolean {
        return (this.tk1 is Tk.Fix && this.tk1.str == str)
    }
    fun checkFix_err (str: String): Boolean {
        val ret = this.checkFix(str)
        if (!ret) {
            this.err_expected('"'+str+'"')
        }
        return ret
    }
    fun acceptFix (str: String): Boolean {
        val ret = this.checkFix(str)
        if (ret) {
            this.lex()
        }
        return ret
    }
    fun acceptFix_err (str: String): Boolean {
        this.checkFix_err(str)
        this.acceptFix(str)
        return true
    }

    fun checkEnu (enu: String): Boolean {
        return when (enu) {
            "Id"  -> this.tk1 is Tk.Id
            "Num" -> this.tk1 is Tk.Num
            else  -> error("bug found")
        }
    }
    fun acceptEnu (enu: String): Boolean {
        val ret = this.checkEnu(enu)
        if (ret) {
            this.lex()
        }
        return ret
    }

    fun err_expected (str: String): Boolean {
        val tk = when {
            (this.tk1 is Tk.Eof) -> "end of file"
            else -> '"' + this.tk1.str + '"'
        }
        error(this.lexer.name + ": (ln ${this.tk1.lin}, col ${this.tk1.col}): expected $str : have $tk")
    }

    fun expr (): Expr {
        return when {
            this.acceptFix("(") -> {
                val e = this.expr()
                this.acceptFix_err(")")
                e
            }
            this.acceptEnu("Id")  -> Expr.Var(this.tk0 as Tk.Id)
            this.acceptEnu("Num") -> Expr.Num(this.tk0 as Tk.Num)
            else -> {
                this.err_expected("expression")
                error("unreachable")
            }
        }
    }

    fun stmt (): Stmt {
        return when {
            else -> {
                this.err_expected("statement")
                error("unreachable")
            }
        }
    }
}
