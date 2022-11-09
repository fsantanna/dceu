class Parser (lexer_: Lexer)
{
    val lexer = lexer_
    var tk0: Tk = Tk.Eof(1, 1)
    var tk1: Tk = Tk.Eof(1, 1)
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
            this.lexer.err_expected(this.tk1, '"'+str+'"')
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
            "Nat" -> this.tk1 is Tk.Nat
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
            this.lexer.err_expected(this.tk1, err)
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

    fun block (tk0: Tk.Fix?, catch: Expr?): Expr.Do {
        this.acceptFix_err("{")
        val tk0_ = tk0 ?: this.tk0 as Tk.Fix
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Do(tk0_, catch, es)
    }

    fun exprPrim (): Expr {
        return when {
            this.acceptFix("do") || this.acceptFix("catch") -> {
                val tk0 = this.tk0 as Tk.Fix
                val catch = if (this.tk0.str != "catch") null else {
                    this.expr()
                }
                this.block(tk0, catch)
            }
            this.acceptFix("var") -> {
                this.acceptEnu_err("Id")
                Expr.Dcl(this.tk0 as Tk.Id)
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0 as Tk.Fix
                val dst = this.expr()
                this.acceptFix_err("=")
                val src = this.expr()
                if (dst !is Expr.Acc && dst !is Expr.Index) {
                    this.lexer.err(tk0, "invalid set : invalid destination")
                }
                Expr.Set(tk0, dst, src)
            }
            this.acceptFix("if") -> {
                val tk0 = this.tk0 as Tk.Fix
                val cnd = this.expr()
                val t = this.block(null, null)
                val f = if (this.acceptFix("else")) {
                    this.block(null, null)
                } else {
                    val tk = Tk.Fix("{",this.tk0.lin,this.tk0.col)
                    Expr.Do(tk, null, listOf(Expr.Nil(Tk.Fix("nil", tk0.lin, tk0.col))))
                }
                Expr.If(tk0, cnd, t, f)
            }
            this.acceptFix("loop") -> {
                val tk = Tk.Fix("catch",this.tk0.lin,this.tk0.col)
                val num = Expr.Num(Tk.Num("1",tk.lin,tk.col))
                // loop -> catch (1) { loop { ... } }   // 1: same code as break
                Expr.Do(tk, num, listOf(Expr.Loop(this.tk0 as Tk.Fix, this.block(null, null))))
            }
            this.acceptFix("break") -> {
                val tk0 = this.tk0 as Tk.Fix
                val arg = if (this.checkFix("}") || this.checkEnu("Eof")) {
                    Expr.Nil(Tk.Fix("nil", tk0.lin, tk0.col))
                } else {
                    this.expr()
                }
                // break x -> throw (1,x)               // 1: same code as loop
                Expr.Throw(tk0, Expr.Num(Tk.Num("1", tk0.lin, tk0.col)), arg)
            }
            this.acceptFix("func") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("(")
                val args = this.list0(")") { this.acceptEnu("Id"); this.tk0 as Tk.Id }
                val body = this.block(null, null)
                Expr.Func(tk0, args, body)
            }
            this.acceptFix("throw") -> {
                val tk0 = this.tk0 as Tk.Fix
                val (ex,arg) = if (this.acceptFix("(")) {
                    val ex = this.expr()
                    val arg = if (this.acceptFix(",")) {
                        this.expr()
                    } else {
                        Expr.Nil(Tk.Fix("nil", tk0.lin, tk0.col))
                    }
                    this.acceptFix_err(")")
                    Pair(ex, arg)
                } else {
                    Pair(this.expr(), Expr.Nil(Tk.Fix("nil", tk0.lin, tk0.col)))
                }
                Expr.Throw(tk0, ex, arg)
            }

            this.acceptEnu("Nat") -> Expr.Nat(this.tk0 as Tk.Nat)
            this.acceptEnu("Id")   -> Expr.Acc(this.tk0 as Tk.Id)
            this.acceptFix("nil")   -> Expr.Nil(this.tk0 as Tk.Fix)
            this.acceptFix("false") -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptFix("true")  -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptEnu("Num")  -> Expr.Num(this.tk0 as Tk.Num)
            this.acceptFix("[")     -> Expr.Tuple(this.tk0 as Tk.Fix, list0("]") { this.expr() })
            this.acceptFix("(") -> {
                val e = this.exprPrim()
                this.acceptFix_err(")")
                e
            }
            else -> {
                this.lexer.err_expected(this.tk1, "expression")
                error("unreachable")
            }
        }
    }
    fun exprFixs (): Expr {
        val umn = this.acceptFix("-")
        val tk0 = this.tk0
        var e = this.exprPrim()
        while (true) {
            when {
                // INDEX
                this.acceptFix("[") -> {
                    e = Expr.Index(e.tk, e, this.expr())
                    this.acceptFix_err("]")
                }
                // ECALL
                this.acceptFix("(") -> {
                    e = Expr.Call(e.tk, e, list0(")") { this.expr() })
                }
                else -> break
            }
        }
        if (umn) {
            e = Expr.Call(tk0, Expr.Acc(Tk.Id("-",tk0.lin,tk0.col)), listOf(e))
        }
        return e
    }
    fun exprBins (): Expr {
        return this.exprFixs()
    }
    fun expr (): Expr {
        return this.exprBins()
    }

    fun exprs (): List<Expr> {
        val ret = mutableListOf<Expr>()
        while (this.acceptFix(";")) {}
        while (!this.checkFix("}") && !this.checkEnu("Eof")) {
            val e = this.expr()
            while (this.acceptFix(";")) {}
            ret.add(e)
        }
        if (ret.size == 0) {
            ret.add(Expr.Nil(Tk.Fix("nil", this.tk0.lin, this.tk0.col)))
        }
        return ret
    }
}
