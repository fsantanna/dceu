package dceu

class Parser (lexer_: Lexer)
{
    val lexer = lexer_
    var tk0: Tk = Tk.Eof(lexer.stack.first().toPos())
    var tk1: Tk = Tk.Eof(lexer.stack.first().toPos())
    val tks: Iterator<Tk>

    init {
        N = 1
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
            err(this.tk0, "invalid declaration : unexpected ...")
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
                    err(this.tk0, "invalid declaration : unexpected ...")
                }
                Pair(Pair(e.tk_,null), this.expr())
            }
            this.acceptEnu("Tag") -> {
                if (e.tk.str == "...") {
                    err(this.tk0, "invalid declaration : unexpected ...")
                }
                val tag = this.tk0 as Tk.Tag
                this.acceptFix_err("=")
                Pair(Pair(e.tk_,tag), this.expr())
            }
            else -> Pair(null, e)
        }
    }

    fun expr_prim (): Expr {
        return when {
            this.acceptFix("do") -> Expr.Do(this.tk0, this.block().es)
            this.acceptFix("val") || this.acceptFix("var") -> {
                val tk0 = this.tk0 as Tk.Fix
                val tmp = this.acceptTag(":fleet")
                if (tmp && tk0.str!="val") {
                    err(this.tk0, "invalid declaration : expected \"val\" for \":fleet\"")
                }
                val (id,tag) = this.id_tag()
                val src = if (!this.acceptFix("=")) null else {
                    this.expr()
                }
                Expr.Dcl(tk0, id, tmp, tag, true, src)
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0 as Tk.Fix
                val dst = this.expr()
                if (dst is Expr.Acc && dst.tk.str == "...") {
                    err(this.tk0, "invalid set : unexpected ...")
                }
                this.acceptFix_err("=")
                val src = this.expr()
                if (!dst.is_lval()) {
                    err(tk0, "invalid set : expected assignable destination")
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
                        ${tk0.pos.pre()}do {
                            val :fleet ceu_$N ${tag.cond{it.str}} = ${cnd.tostr(true)}
                            if ceu_$N {
                                val :fleet ${id.str} ${tag.cond{it.str}} = ceu_$N
                                ${t.es.tostr(true)}
                            } else {
                                ${f.es.tostr(true)}
                            }
                        }
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
                this.acceptFix_err("{")
                this.acceptFix_err("as")
                val (it,tag) = this.id_tag()
                this.acceptFix_err("=>")
                val cnd = this.exprs()
                this.acceptFix_err("}")
                this.acceptFix_err("in")
                Expr.Catch (
                    tk0, Pair(it,tag),
                    Expr.Do(Tk.Fix("do",this.tk0.pos), cnd),
                    this.block()
                )
            }
            (CEU>=2 && this.acceptFix("defer")) -> Expr.Defer(this.tk0 as Tk.Fix, this.block())

            (CEU>=3 && this.acceptFix("yield")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val out = this.expr_in_parens()!!
                this.acceptFix_err("{")
                val tk1 = this.tk0
                val it = if (CEU<99 || this.checkFix("as")) {
                    this.acceptFix_err("as")
                    val (id,tag) = this.id_tag()
                    this.acceptFix_err("=>")
                    Pair(id, tag)
                } else {
                    Pair(Tk.Id("it", this.tk0.pos, 0), null)
                }
                val es = this.exprs()
                this.acceptFix_err("}")
                val inp = Expr.Do (
                    Tk.Fix("do", tk1.pos),
                    listOf(Expr.Do(Tk.Fix("do",tk1.pos), es))
                )
                Expr.Yield(tk0, it, out, inp)
            }
            (CEU>=3 && this.acceptFix("resume")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val tk1 = this.tk1
                val call = this.expr_2_pre()
                when {
                    (call !is Expr.Call) -> err(tk1, "invalid resume : expected call")
                    (call.args.size > 1) -> err(tk1, "invalid resume : invalid number of arguments")
                }
                call as Expr.Call
                val arg = call.args.getOrNull(0) ?: Expr.Nil(Tk.Fix("nil",tk1.pos))
                Expr.Resume(tk0, call.clo, arg)
            }

            (CEU>=4 && this.acceptFix("spawn")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val tk1 = this.tk1
                val call = this.expr()
                if (call !is Expr.Call) {
                    err(this.tk1, "invalid spawn : expected call")
                }
                when {
                    (call !is Expr.Call) -> err(tk1, "invalid spawn : expected call")
                    (call.args.size > 1) -> err(tk1, "invalid spawn : invalid number of arguments")
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
                val tk0 = this.tk0 as Tk.Fix
                val trk = this.expr_in_parens()!!
                this.acceptFix_err("{")
                val tk1 = this.tk0 as Tk.Fix
                this.acceptFix_err("as")
                val it_tag = this.id_tag()
                this.acceptFix_err("=>")
                val es = this.exprs()
                this.acceptFix_err("}")
                Expr.Dtrack(tk0, it_tag, trk,
                    Expr.Do(Tk.Fix("do",tk1.pos), es)
                )
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
                val pre0 = this.tk0.pos.pre()
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
                        (v!=null) && this.acceptEnu("Op") -> {
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
                    ${pre0}do {
                        ${v.cond { "val :fleet $x = ${v!!.tostr(true)}" }}
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
                    }
                """)
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
    // expr_4_suf : v[0]    v.x    v.(:T).x    f()    f \{...}
    // expr_prim

    fun expr_4_suf (xe: Expr? = null): Expr {
        val e = if (xe != null) xe else this.expr_prim()
        val ok = this.tk0.pos.isSameLine(this.tk1.pos) && (
                    this.acceptFix("[") || this.acceptFix(".") || this.acceptFix("(")
                 )
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
                    (op.str == "not") -> this.nest("${op.pos.pre()}if ${e.tostr(true)} { false } else { true }\n")
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
                    ${op.pos.pre()}do {
                        val :fleet ceu_${e1.n} = ${e1.tostr(true)} 
                        if ceu_${e1.n} {
                            ${e2.tostr(true)}
                        } else {
                            ceu_${e1.n}
                        }
                    }
                """)
                "or" -> this.nest("""
                    ${op.pos.pre()}do {
                        val :fleet ceu_${e1.n} = ${e1.tostr(true)} 
                        if ceu_${e1.n} {
                            ceu_${e1.n}
                        } else {
                            ${e2.tostr(true)}
                        }
                    }
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
    fun expr_0_out (): Expr {
        return this.expr_1_bin()
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
                err(e.tk, "invalid expression : innocuous expression")
            }
        }
        return ret
    }
}
