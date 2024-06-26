package dceu

typealias Clock = List<Pair<Tk.Tag,Expr>>
typealias Patt = Pair<Pair<Tk.Id,Tk.Tag?>,Any>

fun Any.tostr (pre: Boolean): String {
    return when (this) {
        is Expr -> this.tostr(pre)
        else -> (this as Clock).map { it.second.tk.pos.pre() + ":"+it.second.tostr(false) + it.first.str }.joinToString("")
    }
}

fun Patt.tostr (pre: Boolean = false): String {
    val (idtag,cnd) = this
    return if (cnd is Expr) {
        "(" + idtag.tostr(pre) + ", " + cnd.tostr(pre) + ")"
    } else {
        "(" + cnd.tostr(pre) + ")"
    }
}

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
        //println("-=-=-")
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

    fun expr_in_parens(opt_arg: Boolean = false, opt_par: Boolean = false): Expr? {
        val par = if (opt_par) this.acceptFix("(") else this.acceptFix_err("(")
        val e = if (opt_arg && this.checkFix(")")) null else this.expr()
        if (par) {
            this.acceptFix_err(")")
        }
        return e
    }

    fun patt (): Patt {
        val xit = Tk.Id("it",this.tk0.pos)
        val par = this.acceptFix("(")

        val ret = when {
            // ()
            (CEU>=99 && par && this.checkFix(")")) -> {
                Pair(Pair(xit,null), this.nest("true"))
            }
            // (== 10)
            // ({{even?}})
            (CEU>=99 && this.acceptEnu("Op")) -> {
                val op = this.tk0.str.let {
                    if (it[0] in OPERATORS || it in XOPERATORS) "{{$it}}" else it
                }
                val e = if (this.checkFix("=>") || this.checkFix("{")) null else this.expr()
                val call = if (e == null) {
                    "$op(it)"
                } else {
                    "$op(it, ${e.tostr(true)})"
                }
                Pair(Pair(xit,null), this.nest(call))
            }
            // (id
            this.acceptEnu("Id") -> {
                val id = this.tk0 as Tk.Id
                when {
                    // (id :Tag
                    this.acceptEnu("Tag") -> {
                        val tag = this.tk0 as Tk.Tag
                        when {
                            // (id :Tag, cnd)
                            this.acceptFix(",") -> {
                                val cnd = this.expr()
                                Pair(Pair(id,tag), if (CEU<99) cnd else this.nest("(${id.str} is? ${tag.str}) and ${cnd.tostr(true)}"))
                            }
                            // (id :Tag)
                            else -> {
                                Pair(Pair(id, tag), this.nest("${id.str} is? ${tag.str}"))
                            }
                        }
                    }
                    // (id, cnd)
                    else -> {
                        this.acceptFix_err(",")
                        val cnd = if (this.checkFix(")") || this.checkFix("{") || this.checkFix("=>")) {
                            this.nest("true")
                        } else {
                            this.expr()
                        }
                        Pair(Pair(id, null), cnd)
                    }
                }
            }
            // (:X
            (CEU>=99 && this.acceptEnu("Tag")) -> {
                val tag = this.tk0 as Tk.Tag
                val unis = listOf(":h",":min",":s",":ms")
                when {
                    // (:x:ms)
                    unis.contains(this.tk1.str) -> {
                        // :X:ms[...]
                        this.acceptEnu_err("Tag")

                        fun Tk.Tag.tonum (): Expr {
                            val s = this.str.drop(1)
                            val n = s.toIntOrNull()
                            return if (n != null) {
                                Expr.Num(Tk.Num(s, this.pos))
                            } else {
                                Expr.Acc(Tk.Id(s, this.pos))
                            }
                        }

                        val l = mutableListOf(Pair(this.tk0 as Tk.Tag, tag.tonum()))
                        while (this.acceptEnu("Tag")) {
                            val t = this.tk0 as Tk.Tag
                            this.acceptEnu_err("Tag")
                            assert(unis.contains(this.tk0.str))
                            l.add(Pair(this.tk0 as Tk.Tag, t.tonum()))
                        }
                        Pair(Pair(xit,Tk.Tag(":Clock",tag.pos)), l as Clock)
                    }
                    // (:X,cnd)
                    this.acceptFix(",") -> {
                        val cnd = this.expr()
                        Pair(Pair(xit,tag), this.nest("(it is? ${tag.str}) and ${cnd.tostr(true)}"))
                    }
                    // (:X)
                    else -> Pair(Pair(xit,tag), this.nest("it is? ${tag.str}"))
                }
            }
            // (,cnd)
            (CEU>=99 && this.acceptFix(",")) -> {
                val cnd = this.expr()
                Pair(Pair(xit,null), cnd)
            }
            (CEU >= 99) -> {
                val e = this.expr()
                when {
                    // 10
                    // [1,2]
                    e.is_constructor() -> {
                        Pair(Pair(xit,null), this.nest("it === ${e.tostr(true)}"))
                    }
                    else -> err(this.tk1, "invalid pattern : unexpected \"${this.tk1.str}\"") as Pair<Pair<Tk.Id,Tk.Tag?>, Expr>
                }
            }
            else -> err_expected(this.tk1, "identifier") as Patt
        }
        if (par) {
            this.acceptFix_err(")")
        }
        return ret
    }

    fun lambda (n:Int): Pair<Pair<Tk.Id,Tk.Tag?>,List<Expr>> {
        this.acceptFix_err("{")
        val tk0 = this.tk0
        return when {
            (CEU < 99) -> {
                val id_tag = this.id_tag()
                this.acceptFix_err("=>")
                val es = this.exprs()
                this.acceptFix_err("}")
                Pair(id_tag, es)
            }
            this.acceptFix("}") -> {
                val id_tag = Pair(Tk.Id("ceu_$n", tk0.pos), null)
                val es = listOf(Expr.Nil(Tk.Fix("nil", tk0.pos.copy())))
                Pair(id_tag, es)
            }
            else -> {
                val e = this.expr()
                val isacc = (e is Expr.Acc)
                val istag = (e is Expr.Tag || isacc && this.acceptEnu("Tag"))
                val tag = this.tk0
                val isarr = (isacc || istag) && this.acceptFix("=>")
                val (es,id_tag) = when {
                    !isarr -> {
                        val xes = if (this.checkFix("}")) emptyList() else this.exprs()
                        val etag = if (istag) listOf(Expr.Tag(tag as Tk.Tag)) else emptyList()
                        Pair(listOf(e)+etag+xes, Pair(Tk.Id("it", tk0.pos), null))
                    }
                    (isacc && istag)  -> Pair(this.exprs(), Pair(e.tk as Tk.Id, tag as Tk.Tag))
                    (isacc && !istag) -> Pair(this.exprs(), Pair(e.tk as Tk.Id, null))
                    (!isacc && istag) -> Pair(this.exprs(), Pair(Tk.Id("it", tk0.pos), tag as Tk.Tag))
                    else -> error("impossible case")
                }
                this.acceptFix_err("}")
                Pair(id_tag, es)
            }
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

    fun method (f: Expr, e: Expr, pre: Boolean): Expr {
        return this.nest(when {
            (f is Expr.Call)  -> {
                val args = if (pre) {
                    e.tostr(true) + f.args.map { "," + it.tostr(true) }.joinToString("")
                } else {
                    f.args.map { it.tostr(true) + "," }.joinToString("") + e.tostr(true)
                }
                """
                ${f.clo.tostr(true)}($args)
                """
            }
            /*(f is Expr.Proto) -> {
                assert(f.args.size <= 1)
                val a = f.args.getOrNull(0)
                """
                ${e.tostr(true)} thus { ${a?.first?.str ?: "it"} ${a?.second?.str ?: ""} => 
                    ${f.blk.es.tostr(true)}
                }
                """
            }*/
            else -> "${f.tostr(true)}(${e.tostr(true)})}"
        })
    }

    fun expr_prim (): Expr {
        return when {
            this.acceptFix("do") -> {
                if (this.checkFix("{")) {
                    Expr.Do(this.tk0, this.block().es)
                } else {
                    Expr.Pass(this.tk0 as Tk.Fix, this.expr())
                }
            }
            (false && CEU>=6 && this.acceptFix("export")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val ids = if (CEU>=99 && this.checkFix("{")) emptyList() else {
                    this.acceptFix_err("[")
                    val l = list0("]",",") {
                        this.acceptFix("evt") || this.acceptEnu_err("Id")
                        this.tk0.str
                    }
                    this.acceptFix_err("]")
                    l
                }
                Expr.Export(tk0, ids, this.block())
            }
            this.acceptFix("val") || this.acceptFix("var") -> {
                val tk0 = this.tk0 as Tk.Fix
                val (id,tag1) = this.id_tag()
                val (tag2,src) = if (!this.acceptFix("=")) Pair(tag1,null) else {
                    val e = this.expr()
                    val tag = when {
                        (tag1 != null) -> tag1
                        (CEU < 99) -> null
                        (e !is Expr.Call) -> null
                        (e.clo !is Expr.Acc) -> null
                        (e.clo.tk.str != "tag") -> null
                        (e.args.size != 2) -> null
                        (e.args[0] !is Expr.Tag) -> null
                        (e.args[1] !is Expr.Tuple) -> null
                        else -> e.args[0].tk as Tk.Tag
                    }
                    Pair(tag, e)
                }
                Expr.Dcl(tk0, Pair(id,tag2), src)
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0 as Tk.Fix
                val dst = this.expr()
                if (dst is Expr.Acc && dst.tk.str == "...") {
                    err(this.tk0, "set error : unexpected ...")
                }
                this.acceptFix_err("=")
                val src = this.expr()
                if (CEU>=99 && dst is Expr.Do && dst.es.let { it.size==3 && it[0] is Expr.Dcl && it[1] is Expr.Nat && it[2] is Expr.Index }) {
                    val dcl = dst.es[0] as Expr.Dcl
                    val c   = dst.es[1] as Expr.Nat
                    val id  = dcl.idtag.first
                    when (c.tk.str) {
                        "/* = */" -> this.nest("""
                            do {
                                ${dcl.tostr(true)}
                                set ${id.str}[#${id.str}-1] = ${src.tostr(true)}
                            }
                        """)
                        "/* + */" -> this.nest("""
                            do {
                                ${dcl.tostr(true)}
                                set ${id.str}[#${id.str}] = ${src.tostr(true)}
                            }
                        """)
                        "/* - */" -> err(tk0, "set error : expected assignable destination") as Expr
                        else -> error("impossible case")
                    }
                } else {
                    if (!dst.is_lval()) {
                        err(tk0, "set error : expected assignable destination")
                    }
                    Expr.Set(tk0, dst, src)
                }
            }
            this.acceptFix("if") -> {
                val tk0 = this.tk0 as Tk.Fix
                val cnd = this.expr()
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
                Expr.If(tk0, cnd, t, f)
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
            this.acceptFix("skip") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("if")
                val cnd = this.expr()
                Expr.Skip(tk0, cnd)
            }
            this.acceptFix("loop") -> {
                if (CEU<99 || this.checkFix("{")) {
                    return Expr.Loop(this.tk0 as Tk.Fix, Expr.Do(this.tk0, this.block().es))
                }

                val idtag = if (this.checkEnu("Id")) this.id_tag() else Pair(Tk.Id("it",this.tk0.pos),null)
                val id = idtag.first.str

                when {
                    this.checkFix("{") -> {
                        val blk = this.block()
                        this.nest("""
                            do {
                                var ${idtag.tostr()} = 0
                                loop {
                                    ${blk.es.tostr(true)}
                                    set $id = $id + 1
                                }
                            }
                        """)
                    }
                    !this.acceptFix_err("in") -> error("impossible case")
                    (this.acceptFix("{") || this.acceptFix("}")) -> {
                        // [x -> y]
                        val tkA = this.tk0 as Tk.Fix
                        val eA = this.expr()
                        this.acceptFix_err("=>")
                        val eB = this.expr()
                        (this.acceptFix("{") || this.acceptFix_err("}"))
                        val tkB = this.tk0 as Tk.Fix

                        // :step +z
                        val (op, step) = if (this.acceptTag(":step")) {
                            (this.acceptOp("-") || acceptOp_err("+"))
                            Pair(this.tk0.str, this.expr())
                        } else {
                            Pair("+", null)
                        }

                        val blk = this.block()

                        val cmp = when {
                            (tkB.str == "}" && op == "+") -> ">"
                            (tkB.str == "{" && op == "+") -> ">="
                            (tkB.str == "}" && op == "-") -> "<"
                            (tkB.str == "{" && op == "-") -> "<="
                            else -> error("impossible case")
                        }

                        this.nest("""
                            do {
                                val ceu_ste_$N = ${if (step == null) 1 else step.tostr(true)}
                                var ${idtag.tostr(true)} = ${eA.tostr(true)} $op (
                                    ${if (tkA.str == "{") 0 else "ceu_ste_$N"}
                                )
                                val ceu_lim_$N = ${eB.tostr(true)}
                                loop {
                                    break(false) if ($id $cmp ceu_lim_$N)
                                    ${blk.es.tostr(true)}
                                    set $id = $id $op ceu_ste_$N
                                }                                
                            }
                        """)
                    }
                    else -> {
                        val iter = this.expr()
                        val blk = this.block()
                        this.nest("""
                            do {
                                val ceu_$N = to-iter(${iter.tostr(true)})
                                loop {
                                    val ${idtag.tostr(true)} = ceu_$N[0](ceu_$N)
                                    break(false) if ($id == nil)
                                    ${blk.es.tostr(true)}
                                }
                            }
                        """)
                    }
                }
            }
            this.acceptFix("func") || (CEU>=3 && this.acceptFix("coro")) || (CEU>=4 && this.acceptFix("task")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val nst = (CEU >= 4) && (tk0.str=="task") && this.acceptTag(":nested")
                val rec = this.acceptTag(":rec")
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
                val proto = Expr.Proto(tk0, nst, rec, tag, args, blk)
                if (dcl == null) {
                    proto
                } else {
                    this.nest("""
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
                        listOf(Expr.Data(tag, ids)) + when {
                            (CEU < 99) -> emptyList()
                            !this.acceptFix("{") -> emptyList()
                            else -> {
                                val ll = mutableListOf<Expr.Data>()
                                while (true) {
                                    val l = one(tag)
                                    if (l.isEmpty()) {
                                        break
                                    }
                                    ll.addAll(l)
                                }
                                this.acceptFix_err("}")
                                ll
                            }
                        }
                    }
                }
                val l = one(null)
                //l.forEach { println(it.tostr()) }
                if (l.size == 1) l.first() else {
                    Expr.Do(Tk.Fix("do",tpl.pos), l)
                }
            }

            (CEU>=2 && this.acceptFix("catch")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val (idtag,cnd) = when {
                    (CEU < 99) -> {
                        this.checkFix_err("(")
                        this.patt()
                    }
                    this.checkFix("{") -> {
                        Pair(Pair(Tk.Id("ceu_$N",this.tk0.pos), null), Expr.Bool(Tk.Fix("true",this.tk0.pos)))
                    }
                    else -> {
                        this.patt()
                    }
                }
                val blk = this.block()
                val xcnd = this.nest("""
                    do {
                        ;; [pay,err,nil]    ;; nil = catch sentinel
                        val ${idtag.tostr(true)} = `:ceu ceux_peek(X->S, XX(-1-1-1))` ;; TODO: -1 block sentinel?
                        ${cnd.tostr(true)}
                    }
                """)
                Expr.Catch(tk0, xcnd as Expr.Do, blk)
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
                if (CEU>=99 && this.checkFix("{")) {
                    val blk = this.block()
                    return this.nest("""
                        ${this.tk0.pos.pre()}(spawn (task :nested () {
                            ${blk.es.tostr(true)}
                        }) ())
                    """)
                }

                val tk0 = this.tk0 as Tk.Fix
                val tk1 = this.tk1
                val call = this.expr()
                if (call !is Expr.Call) {
                    err(this.tk1, "spawn error : expected call")
                }
                when {
                    (call !is Expr.Call) -> err(tk1, "spawn error : expected call")
                    (call.args.lastOrNull().let { it is Expr.Acc && it.tk.str=="..." }) -> err(tk1, "spawn error : \"...\" is not allowed")
                }
                val tsks = if (CEU<5 || !this.acceptFix("in")) null else {
                    this.expr()
                }
                call as Expr.Call
                Expr.Spawn(tk0, tsks, call.clo, call.args)
            }
            (CEU>=4 && this.acceptFix("delay")) -> Expr.Delay(this.tk0 as Tk.Fix)
            (CEU>=4 && this.acceptFix("pub")) -> Expr.Pub(this.tk0 as Tk.Fix, null)
            (CEU>=4 && this.acceptFix("broadcast")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val evt = this.expr_in_parens()!!
                val xin = if (this.acceptFix("in")) {
                    this.expr()
                } else {
                    Expr.Tag(Tk.Tag(":task",this.tk0.pos))
                }
                Expr.Call(tk0,
                    Expr.Acc(Tk.Id("broadcast'", tk0.pos)),
                    listOf(xin,evt)
                )
            }
            (CEU>=4 && this.acceptFix("toggle")) -> {
                val tk0 = this.tk0 as Tk.Fix
                if (CEU>=99 && this.acceptEnu("Tag")) {
                    val tag = this.tk0 as Tk.Tag
                    val blk = this.block()
                    this.nest("""
                        do {
                            val task_$N = spawn ;;{
                                ${blk.tostr(true)}
                            ;;}
                            if (status(task_$N) /= :terminated) { 
                                watching (,it==task_$N) {
                                    loop {
                                        await(${tag.str}, not it[0])
                                        toggle task_$N(false)
                                        await(${tag.str}, it[0])
                                        toggle task_$N(true)
                                    }
                                }
                            }
                            task_$N.pub
                        }
                    """)//.let { println(it.tostr()); it }
                } else {
                    val tsk = this.expr_prim()
                    val on  = this.expr_in_parens()!!
                    Expr.Toggle(tk0, tsk, on)
                }
            }

            this.acceptEnu("Nat")  -> Expr.Nat(this.tk0 as Tk.Nat)
            this.acceptEnu("Id")   -> when {
                (CEU < 99) -> Expr.Acc(this.tk0 as Tk.Id)
                (this.tk0.str.take(2) != "__") -> Expr.Acc(this.tk0 as Tk.Id)
                else -> this.tk0.let {
                    it as Tk.Id
                    Expr.Acc(it.copy(str_=it.str.drop(2)), true)
                }
            }
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

            (CEU>=99 && (this.acceptFix("while") || this.acceptFix("until"))) -> {
                val tk0 = this.tk0 as Tk.Fix
                val cnd = this.expr().let { if (tk0.str=="until") it else {
                    this.nest("not ${it.tostr(true)}")
                } }
                Expr.Break(tk0, cnd, null)
            }
            (CEU>=99 && this.acceptFix("\\")) -> {
                val (id_tag,es) = lambda(N)
                return this.nest("""
                    (func (${id_tag.tostr(true)}) {
                        ${es.tostr(true)}
                    })
                """)
            }
            (CEU>=99 && this.acceptFix("ifs")) -> {
                val V = if (this.checkFix("{")) null else {
                    this.expr()
                }
                this.acceptFix_err("{")
                val ifs = list0("}",null) {
                    val (idtag,cnd) = when {
                        this.acceptFix("else") -> {
                            Pair(null, Expr.Bool(Tk.Fix("true",this.tk0.pos)))
                        }
                        (V == null) -> Pair(null, this.expr())
                        else -> this.patt()
                    }
                    val blk = if (this.acceptFix("=>")) {
                        Expr.Do(this.tk0, listOf(this.expr()))
                    } else {
                        this.block()
                    }
                    Pair(Pair(idtag,cnd),blk)
                }
                //ifs.forEach { println(it.first.third.tostr()) ; println(it.second.tostr()) }
                this.acceptFix_err("}")
                this.nest("""
                    do {
                        ${V.cond { "val ceu_$N = ${it.tostr(true)}" }}
                        ${ifs.map { (xxx,blk) ->
                            val (idtag,cnd) = xxx
                            """
                            ${idtag.cond { "val ${it.tostr(true)} = ceu_$N"}}
                            if ${cnd.tostr(true)} {
                                ${blk.es.tostr(true)}
                            } else {
                            """}.joinToString("")}
                         ${ifs.map { """
                             }
                         """}.joinToString("")}
                    }
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
                                set ceu_arg_$N = yield(ceu_v_$N)
                            }
                            break if (status(ceu_co_$N) == :terminated)
                        }
                        ceu_arg_$N
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("await")) -> {
                if (this.checkFix("spawn")) {
                    val spw = this.expr()
                    spw as Expr.Spawn
                    return this.nest("""
                        do {
                            val ceu_$N = ${spw.tostr(true)}
                            if (status(ceu_$N) /= :terminated) {
                                await(,it==ceu_$N)
                            }
                            ceu_$N.pub
                        }
                    """)
                }

                val pre = this.tk0.pos.pre()
                val par = this.checkFix("(")
                val (idtag, cnd) = this.patt()
                if (!par) {
                    this.checkFix_err("{")
                }
                val cnt = if (!this.checkFix("{")) null else this.block().es
                val clk = if (cnd is Expr) null else {
                    val l = (cnd as Clock)
                    l.map { (tag,e) ->
                        val s = e.tostr(true)
                        "(" + when (tag.str) {
                            ":h"   -> "($s * ${1000*60*60})"
                            ":min" -> "($s * ${1000*60})"
                            ":s"   -> "($s * ${1000})"
                            ":ms"  -> "($s * ${1})"
                            else   -> error("impossible case")
                        }
                    }.joinToString("+") + (")").repeat(l.size)
                }
                return this.nest("""
                    do {
                        ${clk.cond { """
                            var ceu_$N = $it
                        """ }}
                        var ${idtag.tostr(true)}
                        loop {
                            set ${idtag.first.str} = ${pre}yield()
                            ${clk.cond2({
                                """
                                break if do {
                                    if ${idtag.first.str} is? :Clock {
                                        set ceu_$N = ceu_$N - ${idtag.first.str}.ms
                                        if (ceu_$N > 0) {
                                            false
                                        } else {
                                            true
                                        }
                                    }
                                }
                                """
                            },{
                                """
                                until ${cnd.tostr(true)}                                
                                """
                            })}                            
                        }
                        delay
                        ${cnt.cond2({ it.tostr(true) }, {idtag.first.str})}
                    }
                """) //.let { println(it.tostr());it }
            }
            (CEU>=99 && this.acceptFix("every")) -> {
                val patt = this.patt()
                val blk = this.block()
                this.nest("""
                    loop {
                        await ${patt.tostr(true)} {
                            ${blk.es.tostr(true)}
                        }
                    }
                """)
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
                    ${it.tk.pos.pre()}spawn {
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
                            val ceu_${i}_$n = spawn {
                                ${body.es.tostr(true)}
                            }
                        """}.joinToString("")}
                        loop {
                            break if (
                                ${pars.mapIndexed { i,_ -> """
                                    (((status(ceu_${i}_$n) == :terminated) and (ceu_${i}_$n.pub or true)) or
                                """}.joinToString("")} false ${")".repeat(pars.size)}
                            )
                            yield()
                            delay
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
                            val ceu_${i}_$n = spawn {
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
                            delay
                        }
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("watching")) -> {
                //val pre0 = this.tk0.pos.pre()
                val patt = this.patt()
                val blk = this.block()
                this.nest("""
                    par-or {
                        await ${patt.tostr(true)}
                    } with {
                        ${blk.es.tostr(true)}
                    }
                """)
            }
            else -> {
                err_expected(this.tk1, "expression")
                error("unreachable")
            }
        }
    }

    // expr_0_out : v --> f     f <-- v    v where {...}    v thus {...}
    // expr_1_bin : a + b
    // expr_2_pre : -a    :T [...]
    // expr_3_met : v->f    f<-v
    // expr_4_suf : v[0]    v.x    v.1    v.(:T).x    f()
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
                    // PPP
                    val xop = (CEU>=99 && (this.acceptFix("=") || this.acceptOp("+") || this.acceptOp("-")))
                    if (!xop) {
                        val idx = this.expr()
                        this.acceptFix_err("]")
                        Expr.Index(e.tk, e, idx)
                    } else {
                        val ret = when (this.tk0.str) {
                            "=" -> this.nest("""
                                ${e.tostr(true)} thus { ceu_$N =>
                                    `/* = */`
                                    ceu_$N[#ceu_$N-1]
                                }
                            """)
                            "+" -> this.nest("""
                                ${e.tostr(true)} thus { ceu_$N =>
                                    `/* + */`
                                    ceu_$N[#ceu_$N]
                                }
                            """)
                            "-" -> this.nest("""
                                ${e.tostr(true)} thus { ceu_x_$N =>
                                    `/* - */`
                                    ceu_x_$N[#ceu_x_$N-1] thus { ceu_y_$N =>
                                        set ceu_x_$N[#ceu_x_$N-1] = nil
                                        ceu_y_$N
                                    }
                                }
                            """)
                            else -> error("impossible case")
                        }
                        this.acceptFix_err("]")
                        ret
                    }
                }
                "." -> when {
                    (CEU>=99 && this.acceptFix("(")) -> {
                        val n = N
                        this.acceptEnu_err("Tag")
                        val tag = this.tk0
                        this.acceptFix_err(")")
                        val acc = Expr.Acc(Tk.Id("ceu_$n", e.tk.pos))
                        this.nest("""
                            ${e.tostr(true)} thus { ceu_$n ${tag.str} =>
                                ${this.expr_4_suf(acc).tostr(true)}
                            }
                        """) //.let { println(it);it })
                    }
                    (CEU>=4 && this.acceptFix("pub")) -> Expr.Pub(e.tk, e)
                    this.acceptEnu_err("Id") -> Expr.Index(e.tk, e, Expr.Tag(Tk.Tag(':' + this.tk0.str, this.tk0.pos)))
                    else -> error("impossible case")
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
        val ok = (CEU>=99) && (this.acceptFix("->") || this.acceptFix("<-"))
        if (!ok) {
            return e
        }
        if (xop!=null && xop!=this.tk0.str) {
            err(this.tk0, "sufix operation error : expected surrounding parentheses")
        }
        return when (this.tk0.str) {
            "->" -> this.expr_3_met(this.tk0.str, method(this.expr_4_suf(), e, true))
            "<-" -> method(e, this.expr_3_met(this.tk0.str, this.expr_4_suf()), false)
            else -> error("impossible case")
        }
    }
    fun expr_2_pre (): Expr {
        return when {
            (CEU>=99) && this.acceptEnu("Tag") -> {
                if (this.checkFix("[")) {
                    val tk0 = this.tk0 as Tk.Tag
                    val tup = this.expr_prim()
                    this.nest("""
                        ${tk0.pos.pre()}tag(${tk0.str}, ${tup.tostr(true)})
                    """)
                } else {
                    Expr.Tag(this.tk0 as Tk.Tag)
                }
            }
            this.acceptEnu("Op") -> {
                val op = this.tk0 as Tk.Op
                val e = this.expr_2_pre()
                //println(listOf(op,e))
                when {
                    (op.str == "not") -> this.nest("${op.pos.pre()}(if ${e.tostr(true)} { false } else { true })\n")
                    else -> Expr.Call(op, Expr.Acc(Tk.Id("{{${op.str}}}", op.pos)), listOf(e))
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
                    do {
                        val ceu_${e1.n} = ${e1.tostr(true)}
                        if ceu_${e1.n} {
                            ${e2.tostr(true)}
                        } else {
                            ceu_${e1.n}
                        }
                    }
                """)
                "or" -> this.nest("""
                    do {
                        val ceu_${e1.n} = ${e1.tostr(true)}
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
                    Expr.Call(op, Expr.Acc(Tk.Id(id, op.pos)), listOf(e1,e2))
                }
            }
        )
    }
    fun expr_0_out (xop: String? = null, xe: Expr? = null): Expr {
        val e = if (xe != null) xe else this.expr_1_bin()
        val ok = (CEU>=99 && (this.acceptFix("where") || this.acceptFix("thus") || this.acceptFix("-->") || this.acceptFix("<--")))
        if (!ok) {
            return e
        }
        if (xop!=null && xop!=this.tk0.str) {
            err(this.tk0, "sufix operation error : expected surrounding parentheses")
        }
        val op = this.tk0
        return when (op.str) {
            "where" -> {
                val body = this.block()
                this.expr_0_out(op.str,
                    this.nest("""
                        ${op.pos.pre()}do {
                            ${body.es.tostr(true)}
                            ${e.tostr(true)}
                        }
                    """)
                )
            }
            "thus" -> {
                val (id_tag,es) = lambda(N)
                this.nest( """
                        do {
                            ${id_tag.first.pos.pre()}val ${id_tag.tostr(true)} = ${e.tostr(true)}
                            ${es.tostr(true)}
                        }
                    """)
            }
            "-->" -> this.expr_0_out(op.str, method(this.expr_1_bin(), e, true))
            "<--" -> method(e, this.expr_0_out(op.str), false)
            else -> error("impossible case")
        }
    }

    fun expr (): Expr {
        return this.expr_0_out()
    }

    fun exprs (empty: Boolean=false): List<Expr> {
        val ret = mutableListOf<Expr>()
        while (!this.checkFix("}") && !this.checkEnu("Eof")) {
            ret.add(this.expr())
        }
        when {
            (ret.size > 0) -> {}
            empty -> {}
            (CEU >= 99) -> ret.add(Expr.Pass(this.tk0 as Tk.Fix, Expr.Nil(Tk.Fix("nil", this.tk0.pos))))
            else -> err_expected(this.tk1, "expression")
        }
        ret.forEachIndexed { i,e ->
            val ok = (i == ret.lastIndex) || !e.is_innocuous()
            if (!ok) {
                err(e.tk, "expression error : innocuous expression")
            }
        }
        return ret
    }
}
