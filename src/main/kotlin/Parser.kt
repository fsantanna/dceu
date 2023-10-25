package dceu

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
        //println(inp)
        val top = lexer.stack.first()
        val inps = listOf(Pair(Triple(top.file,this.tk0.pos.lin,this.tk0.pos.col), inp.reader()))
        val lexer = Lexer(inps, false)
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
            else  -> error("bug found")
        }
    }
    fun checkEnu_err (str: String): Boolean {
        val ret = this.checkEnu(str)
        val err = when (str) {
            "Eof" -> "end of file"
            "Fix" -> "TODO"
            "Id"  -> "identifier"
            "Tag" -> "tag"
            "Num" -> "number"
            "Nat" -> "native"
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

    fun checkTag (str: String): Boolean {
        return (this.tk1 is Tk.Tag && this.tk1.str == str)
    }
    fun checkTag_err (str: String): Boolean {
        val ret = this.checkTag(str)
        if (!ret) {
            err_expected(this.tk1, '"'+str+'"')
        }
        return ret
    }
    fun acceptTag (str: String): Boolean {
        val ret = this.checkTag(str)
        if (ret) {
            this.lex()
        }
        return ret
    }
    fun acceptTag_err (str: String): Boolean {
        this.checkTag_err(str)
        this.acceptTag(str)
        return true
    }

    fun checkOp (str: String): Boolean {
        return (this.tk1 is Tk.Op && this.tk1.str == str)
    }
    fun checkOp_err (str: String): Boolean {
        val ret = this.checkOp(str)
        if (!ret) {
            err_expected(this.tk1, '"'+str+'"')
        }
        return ret
    }
    fun acceptOp (str: String): Boolean {
        val ret = this.checkOp(str)
        if (ret) {
            this.lex()
        }
        return ret
    }
    fun acceptOp_err (str: String): Boolean {
        this.checkOp_err(str)
        this.acceptOp(str)
        return true
    }

    fun expr_in_parens(opt: Boolean = false): Expr? {
        this.acceptFix_err("(")
        val e = if (opt && this.checkFix(")")) null else this.expr()
        this.acceptFix_err(")")
        return e
    }

    fun xas(): Pair<List<Expr>,Pair<Tk.Id,Tk.Tag?>> {
        val (xblk,xit) = when {
            (CEU < 99) -> {
                this.acceptFix_err("{")
                this.acceptFix_err("as")
                Pair(true, true)
            }
            this.acceptFix("{") -> {
                Pair(true, this.acceptFix("as"))
            }
            else -> Pair(false, false)
        }
        return when {
            (xblk && xit) -> {
                val id_tag = this.id_tag()
                this.acceptFix_err("=>")
                val es = this.exprs()
                this.acceptFix_err("}")
                Pair(es, id_tag)
            }
            (xblk && !xit) -> {
                val es = this.exprs()
                this.acceptFix_err("}")
                Pair(es, Pair(Tk.Id("it", this.tk0.pos, 0), null))
            }
            (!xblk && !xit) -> {
                val n = N
                val tk = this.tk0.pos
                val es = listOf(Expr.Acc(Tk.Id("ceu_$n", tk,0)))
                Pair(es, Pair(Tk.Id("ceu_$n", tk, 0), null))
            }
            else -> error("impossible case")
        }
    }

    fun <T> list0 (close: String, sep: String?, func: ()->T): List<T> {
        val l = mutableListOf<T>()
        while (!this.checkFix(close)) {
            l.add(func())
            if (sep!=null && !this.acceptFix(sep)) {
                break
            }
        }
        return l
    }

    fun block (tk0: Tk? = null): Expr.Do {
        val tk = when {
            (tk0 != null) -> tk0
            (this.tk0.str=="do") -> this.tk0
            else -> this.tk1
        }
        this.acceptFix_err("{")
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Do(tk, es)
    }

    fun args (close: String): List<Pair<Tk.Id,Tk.Tag?>> {
        return this.list0(close,",") {
            this.acceptEnu_err("Id")
            val xid = this.tk0 as Tk.Id
            if (this.tk0.str == "...") {
                this.checkFix_err(")")
            }
            val tag = if (!this.acceptEnu("Tag")) null else {
                this.tk0 as Tk.Tag
            }
            Pair(xid, tag)
        }
    }

    fun id_tag (): Pair<Tk.Id, Tk.Tag?> {
        this.acceptEnu_err("Id")
        val id = this.tk0 as Tk.Id
        if (id.str == "...") {
            err(this.tk0, "declaration error : unexpected ...")
        }
        val tag = if (!this.acceptEnu("Tag")) null else {
            this.tk0 as Tk.Tag
        }
        return Pair(id, tag)
    }

    fun id_tag__cnd (): Pair<Pair<Tk.Id,Tk.Tag?>?,Expr> {
        val e = this.expr()
        return when {
            (e !is Expr.Acc) -> Pair(null, e)
            this.acceptFix("=") -> {
                if (e.tk.str == "...") {
                    err(this.tk0, "declaration error : unexpected ...")
                }
                Pair(Pair(e.tk_,null), this.expr())
            }
            this.acceptEnu("Tag") -> {
                if (e.tk.str == "...") {
                    err(this.tk0, "declaration error : unexpected ...")
                }
                val tag = this.tk0 as Tk.Tag
                this.acceptFix_err("=")
                Pair(Pair(e.tk_,tag), this.expr())
            }
            else -> Pair(null, e)
        }
    }

    fun await (tk0: Tk): Expr.Loop {
        val pre0 = tk0.pos.pre()
        val evt = if (!this.checkFix("(")) null else {
            this.expr_in_parens(true)
        }
        val has = this.checkFix("{")
        if (evt==null && !has) {
            err_expected(this.tk1, "\")\"")
        }
        val (es,id_tag) = xas()
        val (id, tag) = id_tag
        val cnd = when {
            (evt == null) -> es.tostr()
            !has -> "${id.str} is? ${evt.tostr()}"
            else -> "(${id.str} is? ${evt.tostr()}) and ${es.tostr()}"
        }
        return this.nest("""
            ${pre0}loop {
                ${pre0}break if (${pre0}yield() thus { as ${id.str} ${tag.cond{it.str}} =>
                    ${pre0}$cnd
                })
            }
        """) as Expr.Loop
    }

    fun expr_prim (): Expr {
        return when {
            this.acceptFix("do") -> Expr.Do(this.tk0, this.block().es)
            this.acceptFix("val") || this.acceptFix("var") -> {
                val tk0 = this.tk0 as Tk.Fix
                val (id,tag) = this.id_tag()
                val src = if (!this.acceptFix("=")) null else {
                    this.expr()
                }
                Expr.Dcl(tk0, id, tag, true, src)
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0 as Tk.Fix
                val dst = this.expr()
                if (dst is Expr.Acc && dst.tk.str == "...") {
                    err(this.tk0, "set error : unexpected ...")
                }
                this.acceptFix_err("=")
                val src = this.expr()
                if (!dst.is_lval()) {
                    err(tk0, "set error : expected assignable destination")
                }
                Expr.Set(tk0, dst, /*null,*/ src)
            }
            this.acceptFix("if") -> {
                val tk0 = this.tk0 as Tk.Fix
                val (id_tag,cnd) = if (CEU >= 99) id_tag__cnd() else Pair(null,this.expr())
                val arr = (CEU>=99) && this.acceptFix("=>")
                val t = if (arr) {
                    Expr.Do(this.tk0, listOf(this.expr_1_bin()))
                } else {
                    this.block()
                }
                val f = when {
                    (CEU < 99) -> {
                        this.acceptFix_err("else")
                        this.block()
                    }
                    this.acceptFix("else") -> {
                        this.block()
                    }
                    arr && this.acceptFix_err("=>") -> {
                        Expr.Do(this.tk0, listOf(this.expr_1_bin()))
                    }
                    else -> {
                        Expr.Do(tk0, listOf(Expr.Nil(Tk.Fix("nil", tk0.pos.copy()))))
                    }
                }
                if (id_tag == null) {
                    Expr.If(tk0, cnd, t, f)
                } else {
                    val (id,tag) = id_tag
                    this.nest("""
                        ((${cnd.tostr(true)}) thus { as ${id.str} ${tag.cond{it.str}} =>
                            if ${id.str} {
                                ${t.es.tostr(true)}
                            } else {
                                ${f.es.tostr(true)}
                            }
                        })
                    """)
                }
            }
            this.acceptFix("break") -> {
                val tk0 = this.tk0 as Tk.Fix
                val e = if (!this.checkFix("(")) null else {
                    this.expr()
                }
                this.acceptFix_err("if")
                val cnd = this.expr()
                Expr.Break(tk0, cnd, e)
            }
            this.acceptFix("loop") -> Expr.Loop(this.tk0 as Tk.Fix, Expr.Do(this.tk0, this.block().es))
            this.acceptFix("func") || (CEU>=3 && this.acceptFix("coro")) || (CEU>=4 && this.acceptFix("task")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val dcl = if (CEU>=99 && this.acceptEnu("Id")) {
                    this.tk0
                } else {
                    null
                }
                this.acceptFix_err("(")
                val args = this.args(")")
                this.acceptFix_err(")")
                val tag = when {
                    (tk0.str != "task") -> null
                    !this.acceptEnu("Tag") -> null
                    else -> this.tk0 as Tk.Tag
                }
                val blk = this.block(this.tk1)
                val proto = Expr.Proto(tk0, tag, args, blk)
                when {
                    (dcl == null) -> proto
                    else -> this.nest("""
                        ${tk0.pos.pre()}val ${dcl.str} = ${proto.tostr(true)}
                    """)
                }
            }
            this.acceptFix("enum") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("{")
                val tags = this.list0("}",",") {
                    this.acceptEnu_err("Tag")
                    val tag = this.tk0 as Tk.Tag
                    val nat = if (!this.acceptFix("=")) null else {
                        this.acceptEnu_err("Nat")
                        this.tk0 as Tk.Nat
                    }
                    Pair(tag, nat)
                }
                this.acceptFix_err("}")
                Expr.Enum(tk0, tags)
            }
            this.acceptFix("data") -> {
                val tpl = this.tk0 as Tk.Fix
                this.checkEnu_err("Tag")
                fun one (pre: Tk.Tag?): List<Expr.Data> {
                    return if (!this.acceptEnu("Tag")) emptyList() else {
                        val tag = (this.tk0 as Tk.Tag).let {
                            if (pre == null) it else {
                                Tk.Tag(pre.str+'.'+it.str.drop(1), it.pos)
                            }
                        }
                        this.acceptFix_err("=")
                        this.acceptFix_err("[")
                        val ids = this.list0("]",",") {
                            this.acceptEnu_err("Id")
                            val id = this.tk0 as Tk.Id
                            val tp = if (!this.acceptEnu("Tag")) null else {
                                this.tk0 as Tk.Tag
                            }
                            Pair(id, tp)
                        }
                        this.acceptFix_err("]")
                        listOf(Expr.Data(tag, ids))
                    }
                }
                val l = one(null)
                //l.forEach { println(it.tostr()) }
                if (l.size == 1) l.first() else {
                    Expr.Do(Tk.Fix("do",tpl.pos), l)
                }
            }
            this.acceptFix("pass") -> Expr.Pass(this.tk0 as Tk.Fix, this.expr())
            this.acceptFix("drop") -> Expr.Drop(this.tk0 as Tk.Fix, this.expr_in_parens()!!)

            (CEU>=2 && this.acceptFix("catch")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val (cnd, id_tag) = xas()
                this.acceptFix_err("in")
                Expr.Catch (
                    tk0, id_tag,
                    Expr.Do(Tk.Fix("do",this.tk0.pos), cnd),
                    this.block()
                )
            }
            (CEU>=2 && this.acceptFix("defer")) -> Expr.Defer(this.tk0 as Tk.Fix, this.block())

            (CEU>=3 && this.acceptFix("yield")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val out = this.expr_in_parens(CEU>=99) ?: Expr.Nil(Tk.Fix("nil",this.tk0.pos))
                //this.checkFix_err("thus")
                Expr.Yield(tk0, out)
            }
            (CEU>=3 && this.acceptFix("resume")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val tkx = this.tk1
                val call = this.expr_2_pre()
                when {
                    (call !is Expr.Call) -> err(tkx, "resume error : expected call")
                    (call.args.size > 1) -> err(tkx, "resume error : invalid number of arguments")
                }
                call as Expr.Call
                val arg = call.args.getOrNull(0) ?: Expr.Nil(Tk.Fix("nil",tk1.pos))
                Expr.Resume(tk0, call.clo, arg)
            }

            (CEU>=4 && this.acceptFix("spawn")) -> {
                when {
                    (CEU < 99) -> {}
                    this.acceptFix("coro") -> {
                        return this.nest("""
                            TODO
                        """)
                    }
                    this.acceptFix("task") -> {
                        return this.nest("""
                            ${this.tk0.pos.pre()}(spawn (task () :void {
                                ${this.block().es.tostr(true)}
                            }) ())
                        """)
                    }
                }
                if (this.acceptFix("task")) {
                    err(this.tk0, "spawn error : unexpected \"task\"")
                }

                val tk0 = this.tk0 as Tk.Fix
                val tk1 = this.tk1
                val call = this.expr()
                if (call !is Expr.Call) {
                    err(this.tk1, "spawn error : expected call")
                }
                when {
                    (call !is Expr.Call) -> err(tk1, "spawn error : expected call")
                    (call.args.size > 1) -> err(tk1, "spawn error : invalid number of arguments")
                }
                val tasks = if (CEU<5 || !this.acceptFix("in")) null else {
                    this.expr()
                }
                call as Expr.Call
                val arg = call.args.getOrNull(0) ?: Expr.Nil(Tk.Fix("nil",tk1.pos))
                Expr.Spawn(tk0, tasks, call.clo, arg)
            }
            (CEU>=4 && this.acceptFix("pub")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val tsk = this.expr_in_parens(true)
                Expr.Pub(tk0, tsk)
            }
            (CEU>=4 && this.acceptFix("broadcast")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val evt = this.expr_in_parens()!!
                val xin = if (!this.acceptFix("in")) null else {
                    this.expr()
                }
                Expr.Bcast(tk0,
                    Expr.Call(tk0,
                        Expr.Acc(Tk.Id("broadcast", tk0.pos, 0)),
                        listOf(evt) + listOfNotNull(xin)
                    )
                )
            }
            (CEU>=4 && this.acceptFix("toggle")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val tsk = this.expr_prim()
                val on = this.expr_in_parens()!!
                Expr.Toggle(tk0, tsk, on)
            }
            (CEU>=5 && this.acceptFix("detrack")) -> {
                val tk0 = this.tk0
                val ret = Expr.Dtrack(tk0 as Tk.Fix, this.expr_in_parens()!!)
                if (CEU<99 || !this.checkFix("{")) {
                    ret
                } else {
                    val (es,id_tag) = xas()
                    val (id,tag) = id_tag
                    this.nest("""
                        ${tk0.pos.pre()}${ret.tostr(true)} thus { as ${id.str} ${tag.cond{it.str}} =>
                            if it {
                                ${es.tostr(true)}
                            }
                        }
                    """)
                }
            }

            this.acceptEnu("Nat")  -> Expr.Nat(this.tk0 as Tk.Nat)
            this.acceptEnu("Id")   -> Expr.Acc(this.tk0 as Tk.Id)
            this.acceptEnu("Tag")  -> Expr.Tag(this.tk0 as Tk.Tag)
            this.acceptFix("nil")   -> Expr.Nil(this.tk0 as Tk.Fix)
            this.acceptFix("false") -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptFix("true")  -> Expr.Bool(this.tk0 as Tk.Fix)
            this.acceptEnu("Chr")  -> Expr.Char(this.tk0 as Tk.Chr)
            this.acceptEnu("Num")  -> Expr.Num(this.tk0 as Tk.Num)
            this.acceptFix("[")     -> Expr.Tuple(this.tk0 as Tk.Fix, list0("]",",") { this.expr() }).let {
                this.acceptFix_err("]")
                it
            }
            this.acceptFix("#[")    -> Expr.Vector(this.tk0 as Tk.Fix, list0("]",",") { this.expr() }).let {
                this.acceptFix_err("]")
                it
            }
            this.acceptFix("@[")    -> Expr.Dict(this.tk0 as Tk.Fix, list0("]",",") {
                val tk1 = this.tk1
                val k = if (this.acceptEnu("Id")) {
                    val e = Expr.Tag(Tk.Tag(':' + tk1.str, tk1.pos))
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
            }).let {
                this.acceptFix_err("]")
                it
            }
            this.checkFix("(")      -> this.expr_in_parens()!!

            (CEU>=99 && this.acceptFix("ifs")) -> {
                val (x,v) = if (this.checkFix("{")) {
                    Pair("ceu_$N", null)
                } else {
                    val e = this.expr()
                    if (e is Expr.Acc && this.acceptFix("=")) {
                        Pair(e.tk.str, this.expr())
                    } else {
                        Pair("ceu_$N", e)
                    }
                }
                this.acceptFix_err("{")

                val ifs = list0("}",null) {
                    val (id_tag,cnd) = when {
                        this.acceptFix("else") -> {
                            Pair(null, Expr.Bool(Tk.Fix("true",this.tk0.pos)))
                        }
                        this.acceptEnu("Op") -> {
                            if (v == null) {
                                err(this.tk0, "case error : expected ifs condition")
                            }
                            val op = this.tk0.str.let {
                                if (it[0] in OPERATORS || it in XOPERATORS) "{{$it}}" else it
                            }
                            val e = if (this.checkFix("=>") || this.checkFix("{")) null else this.expr()
                            val call = if (e == null) {
                                "$op($x)"
                            } else {
                                "$op($x, ${e.tostr(true)})"
                            }
                            Pair(null, this.nest(call))
                        }
                        else -> {
                            id_tag__cnd()
                        }
                    }
                    val blk = if (this.acceptFix("=>")) {
                        Expr.Do(this.tk0, listOf(this.expr()))
                    } else {
                        this.block()
                    }
                    Pair(Pair(id_tag,cnd),blk)
                }
                //ifs.forEach { println(it.first.third.tostr()) ; println(it.second.tostr()) }
                this.acceptFix_err("}")
                this.nest("""
                    ((${v.cond2({it.tostr(true)},{"nil"})}) thus { as $x =>
                    ${ifs.map { (xxx,blk) ->
                        val (id_tag,cnd) = xxx
                        """
                        if ${id_tag.cond{ (id,tag)-> "${id.str} ${tag?.str ?: ""} = "}} ${cnd.tostr(true)} {
                            ${blk.es.tostr(true)}
                        } else {
                        """}.joinToString("")}
                     ${ifs.map { """
                         }
                     """}.joinToString("")}
                    })
                """)
            }
            (CEU>=99 && this.acceptFix("resume-yield-all")) -> {
                val tkx = this.tk1
                val call = this.expr_2_pre()
                when {
                    (call !is Expr.Call) -> err(tkx, "resume-yield-all error : expected call")
                    (call.args.size > 1) -> err(tkx, "resume-yield-all error : invalid number of arguments")
                }
                call as Expr.Call
                this.nest("""
                    ;; TODO: use thus with yield as last statement
                    do {
                        val ceu_co_$N = ${call.clo.tostr(true)}
                        var ceu_arg_$N = ${if (call.args.size==0) "nil" else call.args[0].tostr(true)}
                        loop {
                            val ceu_v_$N = resume ceu_co_$N(ceu_arg_$N)
                            if (status(ceu_co_$N) /= :terminated) or (ceu_v_$N /= nil) {
                                set ceu_arg_$N = yield(drop(ceu_v_$N))
                            }
                            break if (status(ceu_co_$N) == :terminated)
                        }
                        ceu_arg_$N
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("await")) -> {
                val tk0 = this.tk0
                val pre0 = tk0.pos.pre()
                if (this.checkFix("spawn")) {
                    val spw = this.expr()
                    spw as Expr.Spawn
                    if (spw.tsks != null) {
                        err(tk0, "await error : expected non-pool spawn")
                    }
                    return this.nest("""
                        do {
                            val ceu_$N = ${spw.tostr(true)}
                            loop {
                                break (pub(ceu_$N)) if (status(ceu_$N) == :terminated)
                                yield()
                            }
                        }
                    """)
                }
                return await(tk0)
            }
            (CEU>=99 && this.acceptFix("par")) -> {
                val pre0 = this.tk0.pos.pre()
                val pars = mutableListOf(this.block())
                this.acceptFix_err("with")
                pars.add(this.block())
                while (this.acceptFix("with")) {
                    pars.add(this.block())
                }
                val spws = pars.map { """
                    ${it.tk.pos.pre()}spawn task {
                        ${it.es.tostr(true)}
                    }
                """}.joinToString("")
                //println(spws)
                this.nest("""
                    ${pre0}do {
                        $spws
                        ${pre0}loop {
                            ${pre0}yield()
                        }
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("par-or")) -> {
                val pre0 = this.tk0.pos.pre()
                val pars = mutableListOf(this.block())
                val n = pars[0].n
                this.acceptFix_err("with")
                pars.add(this.block())
                while (this.acceptFix("with")) {
                    pars.add(this.block())
                }
                this.nest("""
                    ${pre0}do {
                        ${pars.mapIndexed { i,body -> """
                            val ceu_${i}_$n = spawn task {
                                ${body.es.tostr(true)}
                            }
                        """}.joinToString("")}
                        loop {
                            break if (
                                ${pars.mapIndexed { i,_ -> """
                                    (((status(ceu_${i}_$n) == :terminated) and (pub(ceu_${i}_$n) or true)) or
                                """}.joinToString("")} false ${")".repeat(pars.size)}
                            )
                            yield()
                        }
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("par-and")) -> {
                val pre0 = this.tk0.pos.pre()
                val pars = mutableListOf(this.block())
                val n = pars[0].n
                this.acceptFix_err("with")
                pars.add(this.block())
                while (this.acceptFix("with")) {
                    pars.add(this.block())
                }
                //println(spws)
                this.nest("""
                    ${pre0}do {
                        ${pars.mapIndexed { i,body -> """
                            val ceu_${i}_$n = spawn task {
                                ${body.es.tostr(true)}
                            }
                        """}.joinToString("")}
                        loop {
                            break(nil) if (
                                ${pars.mapIndexed { i,_ -> """
                                    ((status(ceu_${i}_$n) == :terminated) and
                                """}.joinToString("")} true ${")".repeat(pars.size)}
                            )
                            yield()
                        }
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("watching")) -> {
                val tk0 = this.tk0
                val pre0 = tk0.pos.pre()
                val awt = await(tk0)
                val body = this.block()
                this.nest("""
                    ${pre0}par-or {
                        ${pre0}${awt.tostr()}
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

    // expr_0_out : v --> f     f <-- v    v where {...}
    // expr_1_bin : a + b
    // expr_2_pre : -a    :T [...]
    // expr_3_met : v->f    f<-v
    // expr_4_suf : v[0]    v.x    v.(:T).x    f()    f \{...}    v thus {...}
    // expr_prim

    fun expr_4_suf (xe: Expr? = null): Expr {
        val e = if (xe != null) xe else this.expr_prim()
        val ok = this.tk0.pos.isSameLine(this.tk1.pos) && (
                    this.acceptFix("thus") || this.acceptFix("[") || this.acceptFix(".") || this.acceptFix("(")
                 )
        val op = this.tk0
        if (!ok) {
            return e
        }
        return this.expr_4_suf(
            when (this.tk0.str) {
                "[" -> {
                    val idx = this.expr()
                    this.acceptFix_err("]")
                    Expr.Index(e.tk, e, idx)
                }
                "." -> {
                    this.acceptEnu_err("Id")
                    Expr.Index(e.tk, e, Expr.Tag(Tk.Tag(':'+this.tk0.str,this.tk0.pos)))
                }
                "(" -> {
                    val args = list0(")",",") {
                        val x = this.expr()
                        if (x is Expr.Acc && x.tk.str=="...") {
                            this.checkFix_err(")")
                        }
                        x
                    }
                    this.acceptFix_err(")")
                    when {
                        (e is Expr.Acc && e.tk.str in XOPERATORS) -> {
                            when (args.size) {
                                1 -> this.nest("${e.tostr(true)} ${args[0].tostr(true)}")
                                2 -> this.nest("${args[0].tostr(true)} ${e.tostr(true)} ${args[1].tostr(true)}")
                                else -> err(e.tk, "operation error : invalid number of arguments") as Expr
                            }
                        }
                        else -> Expr.Call(e.tk, e, args)
                    }
                }
                "thus" -> {
                    val (es, id_tag) = xas()
                    val (id,tag) = id_tag
                    val dcl = Expr.Dcl(Tk.Fix("val",id.pos), id, tag, true, e)
                    Expr.Do(op, listOf(dcl) + es)
                }
                else -> error("impossible case")
            }
        )
    }
    fun expr_3_met (xop: String? = null, xe: Expr? = null): Expr {
        val e = if (xe != null) xe else this.expr_4_suf()
        return e
    }
    fun expr_2_pre (): Expr {
        return when {
            this.acceptEnu("Op") -> {
                val op = this.tk0 as Tk.Op
                val e = this.expr_2_pre()
                //println(listOf(op,e))
                when {
                    (op.str == "not") -> this.nest("${op.pos.pre()}(if ${e.tostr(true)} { false } else { true })\n")
                    else -> Expr.Call(op, Expr.Acc(Tk.Id("{{${op.str}}}", op.pos, 0)), listOf(e))
                }
            }
            else -> this.expr_3_met()
        }
    }
    fun expr_1_bin (xop: String? = null, xe1: Expr? = null): Expr {
        val e1 = if (xe1 != null) xe1 else this.expr_2_pre()
        val ok = this.tk1.pos.isSameLine(this.tk0.pos) && // x or \n y (ok) // x \n or y (not allowed) // problem with '==' in 'ifs'
                    this.acceptEnu("Op")
        if (!ok) {
            return e1
        }
        if (xop!=null && xop!=this.tk0.str) {
            err(this.tk0, "binary operation error : expected surrounding parentheses")
        }
        val op = this.tk0
        val e2 = this.expr_2_pre()
        return this.expr_1_bin(op.str,
            when (op.str) {
                "and" -> this.nest("""
                    ((${e1.tostr(true)}) thus { as ceu_${e1.n} =>
                        if ceu_${e1.n} {
                            ${e2.tostr(true)}
                        } else {
                            ceu_${e1.n}
                        }
                    })
                """)
                "or" -> this.nest("""
                    ((${e1.tostr(true)}) thus { as ceu_${e1.n} =>  
                        if ceu_${e1.n} {
                            ceu_${e1.n}
                        } else {
                            ${e2.tostr(true)}
                        }
                    })
                """)
                "is?" -> this.nest("is'(${e1.tostr(true)}, ${e2.tostr(true)})")
                "is-not?" -> this.nest("is-not'(${e1.tostr(true)}, ${e2.tostr(true)})")
                "in?" -> this.nest("in'(${e1.tostr(true)}, ${e2.tostr(true)})")
                "in-not?" -> this.nest("in-not'(${e1.tostr(true)}, ${e2.tostr(true)})")
                else -> {
                    val id = if (op.str[0] in OPERATORS) "{{${op.str}}}" else op.str
                    Expr.Call(op, Expr.Acc(Tk.Id(id,op.pos,0)), listOf(e1,e2))
                }
            }
        )
    }
    fun expr_0_out (xop: String? = null, xe: Expr? = null): Expr {
        return this.expr_1_bin()
        /*
        val e = if (xe != null) xe else this.expr_1_bin()
        val ok = this.acceptFix("thus")
        if (!ok) {
            return e
        }
        val tk0 = this.tk0 as Tk.Fix
        if (xop!=null && xop!=this.tk0.str) {
            err(this.tk0, "sufix operation error : expected surrounding parentheses")
        }
        val op = this.tk0
        return when (op.str) {
            else -> error("impossible case")
        }
         */
    }

    fun expr (): Expr {
        return this.expr_0_out()
    }

    fun exprs (): List<Expr> {
        val ret = mutableListOf<Expr>()
        while (!this.checkFix("}") && !this.checkEnu("Eof")) {
            ret.add(this.expr())
        }
        if (ret.size == 0) {
            if (CEU >= 99) {
                ret.add(Expr.Nil(Tk.Fix("nil", this.tk0.pos.copy())))
            } else {
                err_expected(this.tk1, "expression")
            }
        }
        ret.forEachIndexed { i,e ->
            val ok = (i == ret.size-1) || !e.is_innocuous()
            if (!ok) {
                err(e.tk, "expression error : innocuous expression")
            }
        }
        return ret
    }
}
