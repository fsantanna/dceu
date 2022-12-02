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
            "Clk" -> this.tk1 is Tk.Clk
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
        while (!this.checkFix(close)) {
            l.add(func())
            if (!this.acceptFix(",")) {
                break
            }
        }
        this.acceptFix_err(close)
        return l
    }

    fun block (): Expr.Block {
        val tk0 = this.tk0
        this.acceptFix_err("{")
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Block(tk0, false, es)
    }

    fun clk_or_expr (): Pair<Tk.Clk?,Expr?> {
        return if (this.acceptEnu("Clk")) {
            Pair(this.tk0 as Tk.Clk, null)
        } else {
            Pair(null, this.expr())
        }
    }

    fun exprPrim (): Expr {
        return when {
            this.acceptFix("do") -> this.block()
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
                if (!(dst is Expr.Acc || dst is Expr.Index || dst is Expr.Pub)) {
                    err(tk0, "invalid set : invalid destination")
                }
                Expr.Set(tk0, dst, src)
            }
            this.acceptFix("if") -> {
                val tk0 = this.tk0 as Tk.Fix
                val cnd = this.expr()
                val t = this.block()
                val f = if (!XCEU) {
                    this.acceptFix_err("else")
                    this.block()
                } else {
                    if (this.acceptFix("else")) {
                        this.block()
                    } else {
                        Expr.Block(tk0, false, listOf(Expr.Nil(Tk.Fix("nil", tk0.pos.copy()))))
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
                        Expr.Block(tk0, true, listOf(Expr.Dcl(e.tk,false), this.block())))
                } else {
                    Expr.While(tk0, e, this.block())
                }
            }
            this.acceptFix("func") || this.acceptFix("task") -> {
                val tk0 = this.tk0 as Tk.Fix
                val isFake = this.acceptEnu("Tag")
                if (isFake) {
                    if (tk0.str=="func" || this.tk0.str!=":nopub") {
                        err(tk0, "invalid ${tk0.str} : unexpected \"${this.tk0.str}\"")
                    }
                }
                val id = if (XCEU && this.acceptEnu("Id")) this.tk0 as Tk.Id else null
                this.acceptFix_err("(")
                val args = this.list0(")") { this.acceptEnu("Id"); this.tk0 as Tk.Id }
                val body = this.block()
                val func = Expr.Func(tk0, isFake, args, body)
                if (id == null) func else {
                    this.nest("""
                        ${tk0.pos.pre()}var ${id.str} = ${func.tostr(true)} 
                    """)
                }
            }
            this.acceptFix("catch") -> Expr.Catch(this.tk0 as Tk.Fix, this.expr(), this.block())
            this.acceptFix("throw") -> Expr.Throw(this.tk0 as Tk.Fix, checkLine(this.tk0, this.expr()))
            this.acceptFix("defer") -> Expr.Defer(this.tk0 as Tk.Fix, this.block())

            this.acceptFix("coroutines") -> {
                this.acceptFix_err("(")
                val max = if (this.checkFix(")")) null else {
                    this.expr()
                }
                this.acceptFix_err(")")
                Expr.Coros(this.tk0 as Tk.Fix, max)
            }
            this.acceptFix("coroutine") -> Expr.Coro(this.tk0 as Tk.Fix, checkLine(this.tk0, this.expr()))
            this.acceptFix("spawn") -> {
                val tk0 = this.tk0 as Tk.Fix
                when {
                    this.acceptFix("in") -> {
                        val coros = this.expr()
                        this.acceptFix_err(",")
                        val call = this.expr()
                        if (call !is Expr.Block || call.es[0] !is Expr.Call) {
                            err(tk1, "invalid spawn : expected call")
                        }
                        Expr.Spawn(tk0, coros, call as Expr.Block)
                    }
                    (XCEU && this.checkFix("{")) -> {
                        this.nest("""
                            ${tk0.pos.pre()}spawn (task :nopub () {
                                ${this.block().es.tostr(true)}
                            }) ()
                        """)
                    }
                    else -> {
                        val call = this.expr()
                        if (call !is Expr.Block || call.es[0] !is Expr.Call) {
                            err(tk1, "invalid spawn : expected call")
                        }
                        Expr.Spawn(tk0, null, call as Expr.Block)
                    }
                }
            }
            this.acceptFix("broadcast") -> {
                val coro = if (!this.acceptFix("in")) null else {
                    val v = this.expr()
                    this.acceptFix_err(",")
                    v
                }
                Expr.Bcast(this.tk0 as Tk.Fix, coro, checkLine(this.tk0, this.expr()))
            }
            this.acceptFix("yield") -> Expr.Yield(this.tk0 as Tk.Fix, checkLine(this.tk0, this.expr()))
            this.acceptFix("resume") -> {
                val tk0 = this.tk0 as Tk.Fix
                val call = this.expr()
                if (call !is Expr.Block || call.es[0] !is Expr.Call) {
                    err(tk1, "invalid resume : expected call")

                }
                Expr.Resume(tk0, call as Expr.Block)
            }
            this.acceptFix("toggle") -> {
                val tk0 = this.tk0 as Tk.Fix
                val pre0 = tk0.pos.pre()
                val coro = this.expr()
                val iscall = coro is Expr.Block && coro.isFake && coro.es[0] is Expr.Call
                if (!XCEU || (iscall && !(XCEU && this.checkFix("->")))) {
                    if (!iscall) {
                        err(coro.tk, "invalid toggle : expected argument")
                    }
                    val call = (coro as Expr.Block).es[0] as Expr.Call
                    if (call.args.size != 1) {
                        err(call.tk, "invalid toggle : expected single argument")
                    }
                    Expr.Toggle(tk0, call.f, call.args[0])
                } else {
                    this.acceptFix_err("->")
                    val (off,on) = Pair(coro, this.expr())
                    val blk = this.block()
                    this.nest("""
                        ${pre0}do {
                            var task_$N = spawn ;;{
                                ${blk.tostr(true)}
                            ;;}
                            par {
                                every ${on.tostr(true)} {
                                    toggle task_$N(true)
                                }
                            } with {
                                every ${off.tostr(true)} {
                                    toggle task_$N(false)
                                }
                            }
                        }
                    """)//.let { println(it.tostr()); it }
                }
            }
            this.acceptFix("pub") -> Expr.Pub(this.tk0, null)

            this.acceptFix("evt") || this.acceptFix("err") -> Expr.EvtErr(this.tk0 as Tk.Fix)
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
            this.acceptFix("@[")    -> Expr.Dict(this.tk0 as Tk.Fix, list0("]") {
                this.acceptFix_err("(")
                val k = this.expr()
                this.acceptFix(",")
                val v = this.expr()
                this.acceptFix(")")
                Pair(k,v)
            })
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
                val pre0 = this.tk0.pos.pre()
                this.acceptFix_err("{")
                val e1 = this.expr()
                this.acceptFix_err("->")
                val b1 = if (this.checkFix("{")) this.block() else Expr.Block(this.tk0,false,listOf(this.expr()))
                var ifs = "${pre0}if ${e1.tostr(true)} ${b1.tostr(true)}else {\n"
                var n = 1
                while (!this.acceptFix("}")) {
                    if (this.acceptFix("else")) {
                        this.acceptFix_err("->")
                        val be = if (this.checkFix("{")) this.block() else Expr.Block(this.tk0,false,listOf(this.expr()))
                        ifs += be.es.map { it.tostr(true)+"\n" }.joinToString("")
                        this.acceptFix("}")
                        break
                    }
                    val pre1 = this.tk0.pos.pre()
                    val ei = this.expr()
                    this.acceptFix_err("->")
                    val bi = if (this.checkFix("{")) this.block() else Expr.Block(this.tk0,false,listOf(this.expr()))
                    ifs += """
                        ${pre1}if ${ei.tostr(true)} ${bi.tostr(true)}
                        else {
                    """
                    n++
                }
                ifs += "}\n".repeat(n)
                //println(es)
                this.nest(ifs)
            }
            (XCEU && this.acceptFix("par")) -> {
                val pre0 = this.tk0.pos.pre()
                val pars = mutableListOf(this.block())
                this.acceptFix_err("with")
                pars.add(this.block())
                while (this.acceptFix("with")) {
                    pars.add(this.block())
                }
                val spws = pars.map { """
                    ${it.tk.pos.pre()}spawn {
                        ${it.es.tostr(true)}
                    }
                """}.joinToString("")
                //println(spws)
                this.nest("""
                    ${pre0}do {
                        $spws
                        await false
                    }
                """)
            }
            (XCEU && this.acceptFix("parand")) -> {
                val pre0 = this.tk0.pos.pre()
                val pars = mutableListOf(this.block())
                val n = pars[0].n
                this.acceptFix_err("with")
                pars.add(this.block())
                while (this.acceptFix("with")) {
                    pars.add(this.block())
                }
                val spws = pars.map { """
                    spawn {
                        ${it.es.tostr(true)}
                        set ceu_n_$n = ceu_n_$n + 1
                        if ceu_n_$n == ${pars.size} {
                            throw :ceu_parand_$n
                        }
                    }
                """}.joinToString("")
                //println(spws)
                this.nest("""
                    ${pre0}do {
                        var ceu_n_$n = 0
                        ${pre0}catch err==:ceu_parand_$n {
                            $spws
                            await false
                        }
                    }
                """)
            }
            (XCEU && this.acceptFix("paror")) -> {
                val pre0 = this.tk0.pos.pre()
                val pars = mutableListOf(this.block())
                val n = pars[0].n
                this.acceptFix_err("with")
                pars.add(this.block())
                while (this.acceptFix("with")) {
                    pars.add(this.block())
                }
                val spws = pars.map { """
                    ${it.tk.pos.pre()}spawn {
                        ${it.es.tostr(true)}
                        throw :ceu_paror_$n
                    }
                """}.joinToString("")
                //println(spws)
                this.nest("""
                    ${pre0}catch err==:ceu_paror_$n {
                        $spws
                        await false
                    }
                """)
            }
            (XCEU && this.acceptFix("await")) -> {
                val pre0 = this.tk0.pos.pre()
                val (clk,cnd) = this.clk_or_expr()
                when {
                    (clk != null) -> this.nest("""
                        ${pre0}do {
                            var ceu_ms = ${clk.ms}
                            while ceu_ms > 0 {
                                await evt[:type]==:timer
                                set ceu_ms = ceu_ms - evt[:dt]
                            }
                        }
                    """)//.let { println(it.tostr()); it }
                    (cnd != null) -> this.nest("""
                        ${pre0}do {
                            ${pre0}yield ()
                            ;;println(evt)
                            while not (${cnd!!.tostr(true)}) {
                                yield ()
                            }
                        }
                    """)//.let { println(it.tostr()); it }
                    else -> error("bug found")
                }
            }
            (XCEU && this.acceptFix("every")) -> {
                val pre0 = this.tk0.pos.pre()
                val (clk,cnd) = this.clk_or_expr()
                val body = this.block()
                this.nest("""
                    ${pre0}while true {
                        await ${if (clk!=null) clk.str else cnd!!.tostr(true) }
                        ${body.es.tostr(true)}
                    }
                """)//.let { println(it.tostr()); it }
            }
            (XCEU && this.acceptFix("awaiting")) -> {
                val pre0 = this.tk0.pos.pre()
                val (clk,cnd) = this.clk_or_expr()
                val body = this.block()
                this.nest("""
                    ${pre0}paror {
                        await ${if (clk!=null) clk.str else cnd!!.tostr(true) }
                    } with {
                        ${body.es.tostr(true)}
                    }
                """)//.let { println(it.tostr()); it }
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
                e = this.nest("${op.pos.pre()}if ${e.tostr(true)} { false } else { true }\n")
            } else {
                e = Expr.Block(op, true, listOf(Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos)), listOf(e))))
            }
        }
        return e
    }
    fun exprSufs (): Expr {
        var e = this.exprPrim()
        // only accept sufix in the same line
        while (this.tk0.pos.isSameLine(this.tk1.pos)) {
            when {
                // INDEX / PUB / FIELD
                this.acceptFix("[") -> {
                    e = Expr.Index(e.tk, e, this.expr())
                    this.acceptFix_err("]")
                }
                this.acceptFix(".") -> {
                    e = when {
                        this.acceptFix("pub") -> Expr.Pub(e.tk, e)
                        (XCEU && this.acceptEnu("Id")) -> Expr.Index(e.tk, e, Expr.Tag(Tk.Tag(':'+this.tk0.str,this.tk0.pos)))
                        (XCEU && this.acceptEnu("Num")) -> Expr.Index(e.tk, e, Expr.Num(this.tk0 as Tk.Num))
                        XCEU -> {
                            err_expected(this.tk1, "field")
                            error("unreachable")
                        }
                        else -> {
                            err_expected(this.tk1, "\"pub\"")
                            error("unreachable")
                        }
                    }
                }
                // ECALL
                this.acceptFix("(") -> {
                    e = Expr.Block(e.tk, true, listOf(Expr.Call(e.tk, e, (list0(")"){this.expr()}))))
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
                    ${op.pos.pre()}do {
                        var ceu_${e.n}
                        set ceu_${e.n} = ${e.tostr(true)} 
                        if ceu_${e.n} { ceu_${e.n} } else { ${e2.tostr(true)} }
                    }
                """)
                "and" -> this.nest("""
                    ${op.pos.pre()}do {
                        var ceu_${e.n}
                        set ceu_${e.n} = ${e.tostr(true)} 
                        if ceu_${e.n} { ${e2.tostr(true)} } else { ceu_${e.n} }
                    }
                """)
                else  -> Expr.Block(op, true, listOf(Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos)), listOf(e,e2))))
            }
        }
        return e
    }
    fun expr (): Expr {
        val e = this.exprBins()
        return when {
            !XCEU -> e
            !this.acceptFix("where") -> e
            else -> {
                val tk0 = this.tk0
                val body = this.block()
                this.nest("""
                    ${tk0.pos.pre()}do {
                        ${body.es.tostr(true)}
                        ${e.tostr(true)}
                    }
                """)
            }
        }
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
