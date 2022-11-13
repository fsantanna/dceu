class Parser (lexer_: Lexer)
{
    val lexer = lexer_
    var tk0: Tk = Tk.Eof(lexer.stack.first().toPos())
    var tk1: Tk = Tk.Eof(lexer.stack.first().toPos())
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
            err_expected(this.tk1, '"'+str+'"')
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
            "Fix" -> this.tk1 is Tk.Fix
            "Tag" -> this.tk1 is Tk.Tag
            "Op"  -> this.tk1 is Tk.Op
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
            "Fix" -> "TODO"
            "Id"  -> "identifier"
            "Num" -> "number"
            else   -> TODO(this.toString())
        }

        if (!ret) {
            err_expected(this.tk1, err)
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

    fun block (tk0: Tk.Fix?): Expr.Block {
        this.acceptFix_err("{")
        val tk0_ = tk0 ?: this.tk0 as Tk.Fix
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Block(tk0_, es)
    }

    fun exprPrim (): Expr {
        return when {
            this.acceptFix("do") -> this.block(this.tk0 as Tk.Fix)
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
                    err(tk0, "invalid set : invalid destination")
                }
                Expr.Set(tk0, dst, src)
            }
            this.acceptFix("if") -> {
                val tk0 = this.tk0 as Tk.Fix
                val cnd = this.expr()
                val t = this.block(null)
                val f = if (this.acceptFix("else")) {
                    this.block(null)
                } else {
                    val tk = Tk.Fix("{",this.tk0.pos.copy())
                    Expr.Block(tk, listOf(Expr.Nil(Tk.Fix("nil", tk0.pos.copy()))))
                }
                Expr.If(tk0, cnd, t, f)
            }
            this.acceptFix("while") -> Expr.While(this.tk0 as Tk.Fix, this.expr(), this.block(null))
            this.acceptFix("func") || this.acceptFix("task") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("(")
                val args = this.list0(")") { this.acceptEnu("Id"); this.tk0 as Tk.Id }
                val body = this.block(null)
                Expr.Func(tk0, args, body)
            }
            this.acceptFix("catch") -> Expr.Catch(this.tk0 as Tk.Fix, this.expr(), this.block(null))
            this.acceptFix("throw") -> {
                val tk0 = this.tk0 as Tk.Fix
                val (ex,arg) = if (this.acceptFix("(")) {
                    val ex = this.expr()
                    val arg = if (this.acceptFix(",")) {
                        this.expr()
                    } else {
                        Expr.Nil(Tk.Fix("nil", tk0.pos.copy()))
                    }
                    this.acceptFix_err(")")
                    Pair(ex, arg)
                } else {
                    Pair(this.expr(), Expr.Nil(Tk.Fix("nil", tk0.pos.copy())))
                }
                Expr.Throw(tk0, ex, arg)
            }
            this.acceptFix("spawn") -> Expr.Spawn(this.tk0 as Tk.Fix, this.expr())
            this.acceptFix("resume") -> {
                val tk0 = this.tk0 as Tk.Fix
                val call = this.expr()
                if (call !is Expr.Call) {
                    err_expected(tk1, "invalid resume : expected call")

                }
                Expr.Resume(tk0, call as Expr.Call)
            }
            this.acceptFix("yield") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("(")
                if (!tk0.pos.isSameLine(this.tk0.pos)) {
                    err(this.tk0, "yield error : line break before expression")
                }
                val arg = if (this.acceptFix(")")) {
                    Expr.Nil(Tk.Fix("nil", this.tk0.pos.copy()))
                } else {
                    val e = this.expr()
                    this.acceptFix_err(")")
                    e
                }
                Expr.Yield(tk0, arg)
            }

            this.acceptEnu("Nat")  -> Expr.Nat(this.tk0 as Tk.Nat)
            this.acceptEnu("Id")   -> Expr.Acc(this.tk0 as Tk.Id)
            this.acceptEnu("Tag")  -> Expr.Tag(this.tk0 as Tk.Tag)
            this.acceptFix("nil")   -> Expr.Nil(this.tk0 as Tk.Fix)
            this.acceptFix("false") -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptFix("true")  -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptEnu("Num")  -> Expr.Num(this.tk0 as Tk.Num)
            this.acceptFix("[")     -> Expr.Tuple(this.tk0 as Tk.Fix, list0("]") { this.expr() })
            this.acceptFix("(") -> {
                val e = this.expr()
                this.acceptFix_err(")")
                e
            }
            else -> {
                err_expected(this.tk1, "expression")
                error("unreachable")
            }
        }
    }
    fun exprFixs (): Expr {
        val isop = this.acceptEnu("Op")
        val tk0 = this.tk0
        var e = this.exprPrim()

        // only accept sufix in the same line
        while (this.tk0.pos.isSameLine(this.tk1.pos)) {
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

        if (isop) {
            e = Expr.Call(tk0, Expr.Acc(Tk.Id("{${tk0.str}}",tk0.pos)), listOf(e))
        }
        return e
    }
    fun exprBins (): Expr {
        var e = this.exprFixs()
        if (this.acceptEnu("Op")) {
            val tk0 = this.tk0
            val e2 = this.expr()
            e = Expr.Call(tk0, Expr.Acc(Tk.Id("{${tk0.str}}",tk0.pos)), listOf(e,e2))
        }
        return e
    }
    fun expr (): Expr {
        return this.exprBins()
    }

    fun exprs (): List<Expr> {
        val ret = mutableListOf<Expr>()
        while (!this.checkFix("}") && !this.checkEnu("Eof")) {
            val e = this.expr()
            ret.add(e)
        }
        if (ret.size == 0) {
            ret.add(Expr.Nil(Tk.Fix("nil", this.tk0.pos.copy())))
        }
        return ret
    }
}
