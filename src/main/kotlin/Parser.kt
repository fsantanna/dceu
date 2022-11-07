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
            this.err_expected(this.tk1, '"'+str+'"')
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
            "Eof" -> this.tk1 is Tk.Eof
            "Id"  -> this.tk1 is Tk.Id
            "Num" -> this.tk1 is Tk.Num
            else  -> error("bug found")
        }
    }
    fun checkEnu_err (str: String): Boolean {
        val ret = this.checkEnu(str)
        val err = when (str) {
            "Eof" -> "end of file"
            "Id"  -> "identifier"
            "Num" -> "number"
            else   -> TODO(this.toString())
        }

        if (!ret) {
            this.err_expected(this.tk1, err)
        }
        return ret
    }
    fun acceptEnu (enu: String): Boolean {
        val ret = this.checkEnu(enu)
        if (ret) {
            this.lex()
        }
        return ret
    }
    fun acceptEnu_err (str: String): Boolean {
        this.checkEnu_err(str)
        this.acceptEnu(str)
        return true
    }

    fun err (tk: Tk, str: String) {
        error(this.lexer.name + ": (ln ${tk.lin}, col ${tk.col}): $str")
    }
    fun err_expected (tk: Tk, str: String) {
        val have = when {
            (tk is Tk.Eof) -> "end of file"
            else -> '"' + tk.str + '"'
        }
        this.err(tk, "expected $str : have $have")
    }

    fun list_expr_0 (close: String): List<Expr> {
        val l = mutableListOf<Expr>()
        if (!this.checkFix(close)) {
            l.add(this.exprN())
            while (this.acceptFix(",")) {
                l.add(this.exprN())
            }
        }
        this.acceptFix_err(close)
        return l
    }

    fun expr1 (): Expr {
        return when {
            this.acceptFix("do") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("{")
                val es = this.exprs()
                this.acceptFix_err("}")
                Expr.Do(tk0, es)
            }
            this.acceptFix("var") -> {
                this.acceptEnu_err("Id")
                Expr.Dcl(this.tk0 as Tk.Id)
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0
                val dst = this.exprN()
                this.acceptFix_err("=")
                val src = this.exprN()
                if (dst !is Expr.Acc && dst !is Expr.Index) {
                    err(tk0, "invalid set : invalid destination")
                }
                Expr.Set(tk0 as Tk.Fix, dst, src)
            }
            this.acceptEnu("Id")  -> Expr.Acc(this.tk0 as Tk.Id)
            this.acceptEnu("Num") -> Expr.Num(this.tk0 as Tk.Num)
            this.acceptFix("[")    -> Expr.Tuple(this.tk0 as Tk.Fix, list_expr_0("]"))
            this.acceptFix("(") -> {
                val e = this.expr1()
                this.acceptFix_err(")")
                e
            }
            else -> {
                this.err_expected(this.tk1, "expression")
                error("unreachable")
            }
        }
    }
    fun exprN (): Expr {
        var e = this.expr1()
        while (true) {
            when {
                // INDEX
                this.acceptFix("[") -> {
                    e = Expr.Index(e.tk, e, this.exprN())
                    this.acceptFix_err("]")
                }
                // ECALL
                this.acceptFix("(") -> {
                    e = Expr.Call(e.tk, e, list_expr_0(")"))
                }
                else -> break
            }
        }
        return e
    }

    fun exprs (): List<Expr> {
        val ret = mutableListOf<Expr>()
        while (this.acceptFix(";")) {}
        while (!this.checkFix("}") && !this.checkEnu("Eof")) {
            val e = this.exprN()
            while (this.acceptFix(";")) {}
            ret.add(e)
        }
        return ret
    }
}
