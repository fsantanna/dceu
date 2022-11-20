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

    fun nest (inp: String): Expr {
        val top = lexer.stack.first()
        val inps = listOf(Pair(Triple(top.file,this.tk0.pos.lin,this.tk0.pos.col), inp.reader()))
        val lexer = Lexer(inps)
        val parser = Parser(lexer)
        return parser.expr()
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

    fun checkLine (tk: Tk, e: Expr): Expr {
        if (!tk.pos.isSameLine(e.tk.pos)) {
            err(tk, "${tk.str} error : line break before expression")
        }
        return e
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
                val tk0 = this.tk0 as Tk.Fix
                this.acceptEnu_err("Id")
                val id = this.tk0 as Tk.Id
                if (XCEU && this.acceptFix("=")) {
                    val eq = this.tk0 as Tk.Fix
                    val e = this.expr()
                    Expr.XSeq(tk0, listOf(
                        Expr.Dcl(id, false),
                        Expr.Set(eq, Expr.Acc(id), e)
                    ))
                } else {
                    Expr.Dcl(this.tk0 as Tk.Id, true)
                }
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
                val f = if (!XCEU) {
                    this.acceptFix_err("else")
                    this.block(null)
                } else {
                    if (this.acceptFix("else")) {
                        this.block(null)
                    } else {
                        val tk = Tk.Fix("{",this.tk0.pos.copy())
                        Expr.Block(tk, listOf(Expr.Nil(Tk.Fix("nil", tk0.pos.copy()))))
                    }
                }
                Expr.If(tk0, cnd, t, f)
            }
            this.acceptFix("while") -> {
                val tk0 = this.tk0 as Tk.Fix
                val e = this.expr()
                if (this.acceptFix("in")) {
                    if (e !is Expr.Acc) {
                        err(e.tk, "invalid while : expected identifier")
                    }
                    Expr.Iter(tk0, e.tk as Tk.Id, this.expr(),
                        Expr.Block(tk0, listOf(Expr.Dcl(e.tk,false), this.block(null))))
                } else {
                    Expr.While(tk0, e, this.block(null))
                }
            }
            this.acceptFix("func") || this.acceptFix("task") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("(")
                val args = this.list0(")") { this.acceptEnu("Id"); this.tk0 as Tk.Id }
                val body = this.block(null)
                Expr.Func(tk0, args, body)
            }
            this.acceptFix("catch") -> Expr.Catch(this.tk0 as Tk.Fix, this.expr(), this.block(null))
            this.acceptFix("throw") -> Expr.Throw(this.tk0 as Tk.Fix, checkLine(this.tk0, this.expr()))
            this.acceptFix("defer") -> Expr.Defer(this.tk0 as Tk.Fix, this.block(null))

            this.acceptFix("coroutine") -> Expr.Coro(this.tk0 as Tk.Fix, checkLine(this.tk0, this.expr()))
            this.acceptFix("broadcast") -> Expr.Bcast(this.tk0 as Tk.Fix, checkLine(this.tk0, this.expr()))
            this.acceptFix("resume") -> {
                val tk0 = this.tk0 as Tk.Fix
                val call = this.expr()
                if (call !is Expr.Call) {
                    err_expected(tk1, "invalid resume : expected call")

                }
                Expr.Resume(tk0, call as Expr.Call)
            }
            this.acceptFix("yield") -> Expr.Yield(this.tk0 as Tk.Fix, checkLine(this.tk0, this.expr()))
            this.acceptFix("spawn") -> {
                val tk0 = this.tk0 as Tk.Fix
                val call = this.expr()
                if (call !is Expr.Call) {
                    err_expected(tk1, "invalid spawn : expected call")
                }
                val coros = if (!this.acceptFix("in")) null else {
                    this.expr()
                }
                Expr.Spawn(tk0, coros, call as Expr.Call)
            }
            this.acceptFix("coroutines") -> {
                this.acceptFix_err("(")
                this.acceptFix_err(")")
                Expr.Coros(this.tk0 as Tk.Fix)
            }

            this.acceptEnu("Nat") || this.acceptFix("native") && this.acceptEnu("Nat") -> {
                Expr.Nat(this.tk0 as Tk.Nat)
            }
            this.acceptEnu("Id")   -> Expr.Acc(this.tk0 as Tk.Id)
            this.acceptEnu("Tag")  -> Expr.Tag(this.tk0 as Tk.Tag)
            this.acceptFix("nil")   -> Expr.Nil(this.tk0 as Tk.Fix)
            this.acceptFix("false") -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptFix("true")  -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptEnu("Num")  -> Expr.Num(this.tk0 as Tk.Num)
            this.acceptFix("[")     -> Expr.Tuple(this.tk0 as Tk.Fix, list0("]") { this.expr() })
            this.acceptFix("(") -> {
                val tk0 = this.tk0
                if (XCEU && this.acceptFix(")")) {
                    Expr.Nil(Tk.Fix("nil", tk0.pos))
                } else {
                    val e = this.expr()
                    this.acceptFix_err(")")
                    e
                }
            }

            (XCEU && this.acceptFix("ifs")) -> {
                this.acceptFix_err("{")
                val e1 = this.expr()
                val b1 = this.block(null)
                var ifs = "if ${e1.tostr()} ${b1.tostr()}else {\n"
                var n = 1
                while (!this.acceptFix("}")) {
                    if (this.acceptFix("else")) {
                        val be = this.block(null)
                        ifs += be.es.map { it.tostr()+"\n" }.joinToString("")
                        this.acceptFix("}")
                        break
                    }
                    val ei = this.expr()
                    val bi = this.block((null))
                    ifs += """
                        if ${ei.tostr()} ${bi.tostr()}
                        else {
                    """
                    n++
                }
                ifs += "}\n".repeat(n)
                //println(es)
                this.nest(ifs)
            }
            (XCEU && this.acceptFix("par")) -> {
                var pars = mutableListOf(this.block(null))
                this.acceptFix_err("with")
                pars.add(this.block(null))
                while (this.acceptFix("with")) {
                    pars.add(this.block(null))
                }
                val spws = pars.map { """
                    spawn (task () {
                        ${it.es.tostr()}
                    }) ()
                """}.joinToString("")
                //println(spws)
                this.nest("""
                    do {
                        $spws
                        while true {
                            yield ()
                        }
                    }
                """)
            }
            else -> {
                err_expected(this.tk1, "expression")
                error("unreachable")
            }
        }
    }
    fun exprPres (): Expr {
        val ops = mutableListOf<Tk>()
        while (true) {
            when {
                this.acceptEnu("Op") -> ops.add(this.tk0)
                (XCEU && this.acceptFix("not")) -> ops.add(this.tk0)
                else -> break
            }
        }
        var e = this.exprSufs()
        while (ops.size > 0) {
            val op = ops.removeLast()
            if (XCEU && op.str == "not") {
                op as Tk.Fix
                e = this.nest("if ${e.tostr()} { false } else { true }\n")
            } else {
                e = Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos)), listOf(e))
            }
        }
        return e
    }
    fun exprSufs (): Expr {
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
        return e
    }
    fun exprBins (): Expr {
        var e = this.exprPres()
        while ((this.acceptEnu("Op") || (XCEU && this.acceptFix("or") || this.acceptFix("and")))) {
            val op = this.tk0
            val e2 = this.exprPres()
            e = when (op.str) {
                "or" -> this.nest("""
                    do {
                        var ceu_${e.n}
                        set ceu_${e.n} = ${e.tostr()} 
                        if ceu_${e.n} { ceu_${e.n} } else { ${e2.tostr()} }
                    }
                """)
                "and" -> this.nest("""
                    do {
                        var ceu_${e.n}
                        set ceu_${e.n} = ${e.tostr()} 
                        if ceu_${e.n} { ${e2.tostr()} } else { ceu_${e.n} }
                    }
                """)
                else  -> Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos)), listOf(e,e2))
            }
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
            if (XCEU && e is Expr.XSeq) {
                ret.addAll(e.es)
            } else {
                ret.add(e)
            }
        }
        if (ret.size == 0) {
            if (XCEU) {
                ret.add(Expr.Nil(Tk.Fix("nil", this.tk0.pos.copy())))
            } else {
                err_expected(this.tk1, "expression")
            }
        }
        return ret
    }
}
