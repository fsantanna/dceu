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
            "Chr" -> this.tk1 is Tk.Chr
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

    fun checkOp (str: String): Boolean {
        return (this.tk1 is Tk.Op && this.tk1.str == str)
    }
    fun acceptOp (str: String): Boolean {
        val ret = this.checkOp(str)
        if (ret) {
            this.lex()
        }
        return ret
    }

    fun expr_in_parens (req: Boolean, nil: Boolean): Expr? {
        this.acceptFix_err("(")
        val e = when {
            (req || !this.checkFix(")")) -> {
                val e = this.expr()
                this.acceptFix_err(")")
                e
            }
            nil -> {
                this.acceptFix_err(")")
                Expr.Nil(Tk.Fix("nil", this.tk0.pos))
            }
            else -> {
                this.acceptFix_err(")")
                null
            }
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
        val tk0 = if (this.tk0.str=="do") this.tk0 else this.tk1
        this.acceptFix_err("{")
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Block(tk0, es)
    }

    fun catch_block (): Pair<(Expr)->Expr,Expr.Block> {
        val tk0 = if (this.tk0.str=="do") this.tk0 else this.tk1
        this.acceptFix_err("{")
        val cnd = if (!XCEU || !this.acceptFix("{")) null else {
            val cnd = this.expr()
            this.acceptFix_err("}")
            cnd
        }
        val es = this.exprs()
        this.acceptFix_err("}")
        val blk = Expr.Block(tk0, es)
        if (cnd == null) {
            return Pair({it}, blk)
        } else {
            val catch: (Expr)->Expr = {
                val xcnd = this.nest("(err == ${cnd.tostr(true)})")
                Expr.Catch(tk0 as Tk.Fix, xcnd, if (it is Expr.Block) it else Expr.Block(tk0,listOf(it)))
            }
            return Pair(catch, blk)
        }
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
            this.acceptFix("do") -> this.catch_block().let { (C,b)->C(b) }
            this.acceptFix("group") -> {
                val tk0  = this.tk0 as Tk.Fix
                val isHide = this.acceptEnu("Tag")
                if (isHide) {
                    if (this.tk0.str != ":hide") {
                        err(tk0, "invalid ${tk0.str} : unexpected \"${this.tk0.str}\"")
                    }
                }
                val blk = this.block()
                Expr.Group(tk0, isHide, blk.es)
            }
            this.acceptFix("var") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptEnu_err("Id")
                val id = this.tk0 as Tk.Id
                if (XCEU && this.acceptFix("=")) {
                    val eq = this.tk0 as Tk.Fix
                    val e = this.expr()
                    Expr.Group(tk0, false, listOf(
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
                if (!(dst is Expr.Acc || dst is Expr.Index || (dst is Expr.Pub && dst.tk.str=="pub"))) {
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
                        Expr.Block(tk0, listOf(Expr.Nil(Tk.Fix("nil", tk0.pos.copy()))))
                    }
                }
                Expr.If(tk0, cnd, t, f)
            }
            this.acceptFix("while") -> {
                val tk0 = this.tk0 as Tk.Fix
                if (this.acceptFix("in")) {
                    val col = this.expr()
                    this.acceptFix_err(",")
                    this.acceptEnu_err("Id")
                    val i = this.tk0 as Tk.Id
                    this.catch_block().let { (C,b) ->
                        C(Expr.Iter(tk0, i, col,
                            Expr.Block(tk0, listOf(Expr.Dcl(i,false), b))))
                    }
                } else {
                    val cnd = this.expr()
                    this.catch_block().let { (C,b)->C(Expr.While(tk0, cnd, b)) }
                }
            }
            this.acceptFix("func") || this.acceptFix("task") -> {
                val tk0 = this.tk0 as Tk.Fix
                val isFake = this.acceptEnu("Tag")
                if (isFake) {
                    if (tk0.str=="func" || this.tk0.str!=":fake") {
                        err(tk0, "invalid ${tk0.str} : unexpected \"${this.tk0.str}\"")
                    }
                }
                val id = if (XCEU && this.acceptEnu("Id")) this.tk0 as Tk.Id else null
                this.acceptFix_err("(")
                val args = this.list0(")") { this.acceptEnu("Id"); this.tk0 as Tk.Id }
                val body = this.catch_block().let { (C,b) -> C(b) }.let {
                    if (it is Expr.Block) it else Expr.Block(tk0,listOf(it))
                }
                val proto = Expr.Proto(tk0, isFake, args, body)
                if (id == null) proto else {
                    this.nest("""
                        ${tk0.pos.pre()}var ${id.str} = ${proto.tostr(true)} 
                    """)
                }
            }
            this.acceptFix("catch") -> {
                val cnd = this.expr()
                val blk = this.block()
                if (XCEU && (cnd is Expr.Tag)) {   // catch :err
                    this.nest("catch (err is ${cnd.tostr(true)}) ${blk.tostr(true)}")
                } else {
                    Expr.Catch(this.tk0 as Tk.Fix, cnd, blk)
                }
            }
            this.acceptFix("throw") -> Expr.Throw(this.tk0 as Tk.Fix, this.expr_in_parens(!XCEU,XCEU)!!)
            this.acceptFix("defer") -> Expr.Defer(this.tk0 as Tk.Fix, this.block())

            this.acceptFix("coroutines") -> Expr.Coros(this.tk0 as Tk.Fix, this.expr_in_parens(false,false))
            this.acceptFix("coroutine") -> Expr.Coro(this.tk0 as Tk.Fix, this.expr_in_parens(true,false)!!)
            this.acceptFix("spawn") -> {
                val tk0 = this.tk0 as Tk.Fix
                when {
                    this.acceptFix("in") -> {
                        val coros = this.expr()
                        this.acceptFix_err(",")
                        val call = this.expr()
                        if (call !is Expr.Call && !(call is Expr.Group && call.es.last() is Expr.Call)) {
                            err(tk1, "invalid spawn : expected call")
                        }
                        Expr.Spawn(tk0, coros, call)
                    }
                    (XCEU && this.checkFix("{")) -> {
                        this.nest("""
                            ${tk0.pos.pre()}spawn (task :fake () {
                                ${this.block().es.tostr(true)}
                            }) ()
                        """)
                    }
                    else -> {
                        val call = this.expr()
                        if (call !is Expr.Call && !(call is Expr.Group && call.es.last() is Expr.Call)) {
                            err(tk1, "invalid spawn : expected call")
                        }
                        Expr.Spawn(tk0, null, call)
                    }
                }
            }
            this.acceptFix("broadcast") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("in")
                val xin = this.expr()
                this.acceptFix_err(",")
                val evt = this.expr()
                Expr.Bcast(tk0, xin, evt)
            }
            this.acceptFix("yield") -> Expr.Yield(this.tk0 as Tk.Fix, this.expr_in_parens(!XCEU,XCEU)!!)
            this.acceptFix("resume") -> {
                val tk0 = this.tk0 as Tk.Fix
                val call = this.expr()
                if (call !is Expr.Call) {
                    err(tk1, "invalid resume : expected call")
                }
                Expr.Resume(tk0, call as Expr.Call)
            }
            this.acceptFix("toggle") -> {
                val tk0 = this.tk0 as Tk.Fix
                val pre0 = tk0.pos.pre()
                val coro = this.expr()
                if (!XCEU || (coro is Expr.Call && !(XCEU && this.checkFix("->")))) {
                    if (coro !is Expr.Call) {
                        err(coro.tk, "invalid toggle : expected argument")
                    }
                    coro as Expr.Call
                    if (coro.args.size != 1) {
                        err(coro.tk, "invalid toggle : expected single argument")
                    }
                    Expr.Toggle(tk0, coro.proto, coro.args[0])
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
            this.acceptFix("pub") || this.acceptFix("status") -> Expr.Pub(this.tk0 as Tk.Fix, null)
            this.acceptFix("track") -> Expr.Track(this.tk0 as Tk.Fix, this.expr_in_parens(true,false)!!)

            this.acceptFix("evt") || this.acceptFix("err") -> Expr.EvtErr(this.tk0 as Tk.Fix)
            this.acceptEnu("Nat")  -> {
                Expr.Nat(this.tk0 as Tk.Nat)
            }
            this.acceptEnu("Id")   -> Expr.Acc(this.tk0 as Tk.Id)
            this.acceptEnu("Tag")  -> Expr.Tag(this.tk0 as Tk.Tag)
            this.acceptFix("nil")   -> Expr.Nil(this.tk0 as Tk.Fix)
            this.acceptFix("false") -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptFix("true")  -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptEnu("Chr")  -> Expr.Char(this.tk0 as Tk.Chr)
            this.acceptEnu("Num")  -> Expr.Num(this.tk0 as Tk.Num)
            this.acceptFix("[")     -> Expr.Tuple(this.tk0 as Tk.Fix, list0("]") { this.expr() })
            this.acceptFix("#[")    -> Expr.Vector(this.tk0 as Tk.Fix, list0("]") { this.expr() })
            this.acceptFix("@[")    -> Expr.Dict(this.tk0 as Tk.Fix, list0("]") {
                val tk1 = this.tk1
                val k = if (XCEU && this.acceptEnu("Id")) {
                    val e = Expr.Tag(Tk.Tag(':'+tk1.str, tk1.pos))
                    this.acceptFix_err("=")
                    e
                } else {
                    this.acceptFix_err("(")
                    val e = this.expr()
                    this.acceptFix(",")
                    e
                }
                val v = this.expr()
                if (tk1 !is Tk.Id) {
                    this.acceptFix_err(")")
                }
                Pair(k,v)
            })
            this.checkFix("(") -> this.expr_in_parens(true,false)!!

            (XCEU && this.acceptFix("ifs")) -> {
                val pre0 = this.tk0.pos.pre()

                val noexp = this.acceptFix("{")
                val cnd = if (noexp) null else {
                    val e = this.expr()
                    this.acceptFix_err("{")
                    e
                }
                var ifs = cnd.cond { """
                    ${pre0}do {
                        var ceu_ifs_${cnd!!.n} = ${cnd.tostr(true)}
                """ }

                val eq1 = (cnd!=null && (this.acceptOp("==") || this.acceptFix("is")))
                val eq1_op = this.tk0.str
                val e1 = this.expr().let { if (!eq1) it.tostr(true) else "(ceu_ifs_${cnd!!.n} $eq1_op ${it.tostr(true)})" }
                this.acceptFix_err("->")
                val b1 = if (this.checkFix("{")) this.block() else Expr.Block(this.tk0, listOf(this.expr()))
                ifs += """
                    ${pre0}if $e1 ${b1.tostr(true)} else {
                """
                var n = 1
                while (!this.acceptFix("}")) {
                    if (this.acceptFix("else")) {
                        this.acceptFix_err("->")
                        val be = if (this.checkFix("{")) this.block() else Expr.Block(this.tk0, listOf(this.expr()))
                        ifs += be.es.map { it.tostr(true)+"\n" }.joinToString("")
                        this.acceptFix("}")
                        break
                    }
                    val pre1 = this.tk0.pos.pre()
                    val eqi = (cnd!=null && (this.acceptOp("==") || this.acceptFix("is")))
                    val eqi_op = this.tk0.str
                    val ei = this.expr().let { if (!eqi) it.tostr(true) else "(ceu_ifs_${cnd!!.n} $eqi_op ${it.tostr(true)})" }
                    this.acceptFix_err("->")
                    val bi = if (this.checkFix("{")) this.block() else Expr.Block(this.tk0, listOf(this.expr()))
                    ifs += """
                        ${pre1}if $ei ${bi.tostr(true)}
                        else {
                    """
                    n++
                }
                ifs += "}\n".repeat(n)
                ifs += cnd.cond { "}" }
                //println(ifs)
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
                            throw(:ceu.parand.$n)
                        }
                    }
                """}.joinToString("")
                //println(spws)
                this.nest("""
                    ${pre0}do {
                        var ceu_n_$n = 0
                        ${pre0}catch err==:ceu.parand.$n {
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
                        throw(tags([do {
                            ${it.es.tostr(true)}
                        }], :ceu.paror.$n, true))
                    }
                """}.joinToString("")
                //println(spws)
                this.nest("""
                    ${pre0}do {
                        var ceu_ret_$n =
                            ${pre0}catch err is :ceu.paror.$n {
                                $spws
                                await false
                            }
                        ceu_ret_$n.0
                    }
                """) //.let { println(it.tostr(false));it }
            }
            (XCEU && this.acceptFix("await")) -> {
                val pre0 = this.tk0.pos.pre()
                val spw = this.checkFix("spawn")
                val (clk,cnd) = if (spw) Pair(null,null) else this.clk_or_expr()
                when {
                    (cnd is Expr.Tag) -> {   // await :key
                        this.nest("await evt is ${cnd.tk.str}")
                    }
                    spw -> { // await spawn T()
                        val e = this.expr()
                        if (!(e is Expr.Spawn && e.coros==null)) {
                            err_expected(e.tk, "non-pool spawn")
                        }
                        this.nest("""
                            ${pre0}do {
                                var ceu_spw_$N = ${e.tostr(true)}
                                if (ceu_spw_$N.status /= :terminated) {
                                    ${pre0}await evt==ceu_spw_$N
                                }
                                `ceu_acc = ceu_mem->ceu_spw_$N.Dyn->Bcast.Coro.frame->Task.pub;`
                            }
                        """) //.let { println(it.tostr());it }
                    }
                    (clk != null) -> { // await 5s
                        this.nest("""
                            ${pre0}do {
                                var ceu_ms_$N = ${clk.ms}
                                while ceu_ms_$N > 0 {
                                    await (evt is :frame)
                                    set ceu_ms_$N = ceu_ms_$N - evt.0
                                }
                            }
                        """)//.let { println(it.tostr()); it }
                    }
                    (cnd != null) -> {  // await evt=x
                        this.nest("""
                            ${pre0}do {
                                ${pre0}yield ()
                                while (do {
                                    var ceu_cnd_$N = ${cnd.tostr(true)}
                                    if (type(ceu_cnd_$N) == :track) {
                                        (ceu_cnd_$N.status /= :destroyed)
                                    } else {
                                        (not ceu_cnd_$N)
                                    }
                                }) {
                                    yield ()
                                }
                            }
                        """)//.let { println(it.tostr()); it }
                    }
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
                this.acceptFix("#") -> ops.add(this.tk0)
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
                // INDEX / PUB / FIELD
                this.acceptFix("[") -> {
                    e = Expr.Index(e.tk, e, this.expr())
                    this.acceptFix_err("]")
                }
                this.acceptFix(".") -> {
                    e = when {
                        this.acceptFix("pub") || this.acceptFix("status") -> Expr.Pub(this.tk0 as Tk.Fix, e)
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
                    e = Expr.Call(e.tk, e, list0(")"){this.expr()})
                }
                else -> break
            }
        }
        return e
    }
    fun exprBins (): Expr {
        var e = this.exprPres()
        var pre: Tk? = null
        while (
            this.tk1.pos.isSameLine(e.tk.pos) && // x or \n y (ok) // x \n or y (not allowed) // problem with '==' in 'ifs'
            (this.acceptEnu("Op") || (XCEU &&
                (this.acceptFix("or") || this.acceptFix("and") || this.acceptFix("is") || this.acceptFix("isnot")))
            )
        ) {
            val op = this.tk0
            if (pre==null || pre.str==")" || this.tk1.str==")") {} else {
                err(op, "binary operation error : expected surrounding parentheses")
            }
            val e2 = this.exprPres()
            e = when (op.str) {
                "or"    -> this.nest("""
                    ${op.pos.pre()}do {
                        var _ceu_${e.n}
                        set _ceu_${e.n} = ${e.tostr(true)} 
                        if _ceu_${e.n} { _ceu_${e.n} } else { ${e2.tostr(true)} }
                    }
                """)
                "and"   -> this.nest("""
                    ${op.pos.pre()}do {
                        var _ceu_${e.n}
                        set _ceu_${e.n} = ${e.tostr(true)} 
                        if _ceu_${e.n} { ${e2.tostr(true)} } else { _ceu_${e.n} }
                    }
                """)
                "is"    -> this.nest("is'(${e.tostr(true)}, ${e2.tostr(true)})")
                "isnot" -> this.nest("isnot'(${e.tostr(true)}, ${e2.tostr(true)})")
                else    -> Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos)), listOf(e,e2))
            }
            pre = this.tk0
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
                    ${tk0.pos.pre()}group :hide {
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
            ret.add(this.expr())
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
