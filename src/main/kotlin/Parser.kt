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

    fun <T> list0 (close: String, func: ()->T): List<T> {
        val l = mutableListOf<T>()
        if (!this.checkFix(close)) {
            l.add(func())
            while (this.acceptFix(",")) {
                l.add(func())
            }
        }
        this.acceptFix_err(close)
        return l
    }

    fun block (): Expr.Do {
        val tk0 = this.tk0 as Tk.Fix
        this.acceptFix_err("{")
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Do(tk0, es)
    }

    fun expr1 (): Expr {
        return when {
            this.acceptFix("do") -> this.block()
            this.acceptFix("var") -> {
                this.acceptEnu_err("Id")
                Expr.Dcl(this.tk0 as Tk.Id)
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0 as Tk.Fix
                val dst = this.exprN()
                this.acceptFix_err("=")
                val src = this.exprN()
                if (dst !is Expr.Acc && dst !is Expr.Index) {
                    err(tk0, "invalid set : invalid destination")
                }
                Expr.Set(tk0, dst, src)
            }
            this.acceptFix("if") -> {
                val tk0 = this.tk0 as Tk.Fix
                val cnd = this.exprN()
                val t = this.block()
                val f = if (this.acceptFix("else")) {
                    this.block()
                } else {
                    Expr.Do(tk0, listOf(Expr.Nil(Tk.Fix("nil", tk0.lin, tk0.col))))
                }
                Expr.If(tk0, cnd, t, f)
            }
            this.acceptFix("loop") -> Expr.Loop(this.tk0 as Tk.Fix, this.block())
            this.acceptFix("break") -> {
                val tk0 = this.tk0 as Tk.Fix
                val arg = if (this.checkFix("}") || this.checkEnu("Eof")) {
                    Expr.Nil(Tk.Fix("nil", tk0.lin, tk0.col))
                } else {
                    this.exprN()
                }
                Expr.Break(tk0, arg)
            }
            this.acceptFix("func") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("(")
                val args = this.list0(")") { this.acceptEnu("Id"); this.tk0 as Tk.Id }
                val body = this.block()
                Expr.Func(tk0, args, body)
            }
            this.acceptEnu("Id")   -> Expr.Acc(this.tk0 as Tk.Id)
            this.acceptFix("nil")   -> Expr.Nil(this.tk0 as Tk.Fix)
            this.acceptFix("false") -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptFix("true")  -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptEnu("Num")  -> Expr.Num(this.tk0 as Tk.Num)
            this.acceptFix("[")     -> Expr.Tuple(this.tk0 as Tk.Fix, list0("]") { this.exprN() })
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
                    e = Expr.Call(e.tk, e, list0(")") { this.exprN() })
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
        if (ret.size == 0) {
            ret.add(Expr.Nil(Tk.Fix("nil", this.tk0.lin, this.tk0.col)))
        }
        return ret
    }
}
