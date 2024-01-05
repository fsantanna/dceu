package dceu

typealias Clock = List<Pair<Tk.Tag,Expr>>

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

    fun id_tag_cnd__clock__catch_await (): Triple<Tk.Id?,Any?,Expr?> {
        // ()                   ;; (null, null, null)
        // (x :Y => z)          ;; (id,   tag,  exp)
        // (x :Y)               ;; error
        // (x => z)             ;; (id,   null, exp)
        // :Y => z              ;; (null, tag,  exp)
        // :Y                   ;; (null, tag,  null)
        // z                    ;; (null, null, exp)
        val par = this.acceptFix("(")
        return when {
            (CEU < 99) -> {
                val (id,tag) = this.id_tag()
                this.acceptFix_err("=>")
                val es = this.expr()
                this.acceptFix_err(")")
                Triple(id, tag, es)
            }
            this.checkFix("{") -> {
                Triple(null, null, null)
            }
            (par && this.acceptFix(")")) -> {
                Triple(null, null, null)
            }
            else -> {
                fun fclk (): Any {  // (Either Expr Clock)
                    val e = this.expr()

                    fun isuni (): Boolean {
                        return listOf(":h",":min",":s",":ms").any { this.acceptTag(it) }
                    }

                    fun aux (tag: Tk.Tag): Clock {
                        val e = let {
                            val s = tag.str.drop(1)
                            val n = s.toIntOrNull()
                            if (n != null) {
                                Expr.Num(Tk.Num(s, tag.pos))
                            } else {
                                Expr.Acc(Tk.Id(s, tag.pos, 0))
                            }
                        }
                        val uni = this.tk0 as Tk.Tag
                        val ret = listOf(Pair(uni,e))
                        return if (this.checkFix("{") || (par && this.checkFix(")"))) {
                            ret
                        } else {
                            this.acceptEnu_err("Tag")   // expr
                            val tag = this.tk0 as Tk.Tag
                            if (!isuni()) {                 // unit
                                error("TODO")
                            }
                            ret + aux(tag)
                        }
                    }

                    return when {
                        (e !is Expr.Tag) -> e
                        !isuni() -> e
                        else -> aux(e.tk as Tk.Tag)
                    }
                }

                val e = fclk()
                //println(e)
                val isacc = (e is Expr.Acc)
                val (istag,tag_clk) = when {
                    (e is Expr.Tag) -> Pair(true, e.tk as Tk.Tag)
                    (e is List<*>) -> Pair(true, e)
                    (isacc && this.acceptEnu("Tag")) -> Pair(true, this.tk0 as Tk.Tag)
                    else -> Pair(false, null)
                }
                val isarr = (isacc || istag) && this.acceptFix("=>")
                //println(listOf(isacc, istag, isarr, e2))
                val ret = when {
                    ( isacc &&  istag &&  isarr) -> Triple((e as Expr.Acc).tk as Tk.Id, tag_clk, this.expr())
                    ( isacc &&  istag && !isarr) -> Triple((e as Expr.Acc).tk as Tk.Id, tag_clk, null)
                    ( isacc && !istag &&  isarr) -> Triple((e as Expr.Acc).tk as Tk.Id, null, this.expr())
                    (!isacc &&  istag &&  isarr) -> Triple(null, tag_clk, this.expr())
                    (!isacc &&  istag && !isarr) -> Triple(null, tag_clk, null)
                    (          !istag && !isarr) -> Triple(null, null, e as Expr)
                    else -> error("impossible case")
                }

                if (par) {
                    this.acceptFix_err(")")
                }
                ret
            }
        }
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
                val id_tag = Pair(Tk.Id("ceu_$n", tk0.pos, 0), null)
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
                        Pair(listOf(e)+etag+xes, Pair(Tk.Id("it", tk0.pos, 0), null))
                    }
                    (isacc && istag)  -> Pair(this.exprs(), Pair(e.tk as Tk.Id, tag as Tk.Tag))
                    (isacc && !istag) -> Pair(this.exprs(), Pair(e.tk as Tk.Id, null))
                    (!isacc && istag) -> Pair(this.exprs(), Pair(Tk.Id("it", tk0.pos, 0), tag as Tk.Tag))
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
        return Expr.Do(tk, null, es)
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

    fun id_tag_cnd__ifs (): Pair<Pair<Tk.Id,Tk.Tag?>?,Expr> {
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

    fun await (type: String): Expr {
        // ()                   ;; (null, null, null)   ;; (_  :_ => (_ or true))
        // (x :Y => z)          ;; (id,   tag,  exp)    ;; (x  :Y => chk(:Y) and z)
        // (x :Y)               ;; error                ;; error
        // (x => z)             ;; (id,   null, exp)    ;; (x  :_ => z)
        // :Y => z              ;; (null, tag,  exp)    ;; (it :Y => chk(:Y) and z)
        // :Y                   ;; (null, tag,  null)   ;; (_  :Y => chk(:Y) and (_ or true))
        // z                    ;; (null, null, Acc)    ;; (_  :_ => chk(z))
        // z                    ;; (null, null, exp)    ;; (it :_ => z)

        val n = N
        val tk0 = this.tk0
        val pre0 = tk0.pos.pre()
        val par = this.checkFix("(")
        val (id, tag_clk, cnd) = this.id_tag_cnd__clock__catch_await()
        if (!par) {
            this.checkFix_err("{")
        }

        val cnt = when (type) {
            "await"    -> if (!this.checkFix("{")) null else this.block().es
            "every"    -> { this.checkFix_err("{") ; this.block().es }
            "watching" -> null
            else -> error("impossible case)")
        }

        val (a,b,c) = Triple(id!=null, tag_clk!=null, cnd!=null)
        val (x,y) = Pair(tag_clk is Tk.Tag, tag_clk is List<*>)
        //println(listOf(a,b,c,x,y,tag_clk))
        val tag = if (x) tag_clk as Tk.Tag else null
        val clk1 = Tk.Tag(":Clock", tk0.pos)
        val clk2 = if (y) tag_clk as Clock else null
        if (a && b && !c && cnt==null) {
            err(id!!, "await error : innocuous identifier")
        }
        val xit = Tk.Id("it",tk0.pos,0)
        val xno = if (cnt != null) xit else Tk.Id("ceu_$n",tk0.pos,0)
        val scnd = cnd?.tostr(true)

        fun sret (id: Tk.Id): String {
            return (cnt==null).cond { "and await-ret(${id.str})" }
        }

        val kexp = { clk2!!.map { (tag,e) ->
            val s = e.tostr(true)
            "(" + when (tag.str) {
                ":h"   -> "($s * ${1000*60*60})"
                ":min" -> "($s * ${1000*60})"
                ":s"   -> "($s * ${1000})"
                ":ms"  -> "($s * ${1})"
                else   -> error("impossible case")
            }
        }.joinToString("+") + (")").repeat(clk2!!.size) }
        val kdcl = { "var ceu_clk_$n = ${kexp()}" }
        val kchk = { id: String -> """do {
            ;;println(:AWAKE, $id, ceu_clk_$n)
            set ceu_clk_$n = ceu_clk_$n - __$id.ms
            if (ceu_clk_$n > 0) {
                false
            } else {
                set ceu_clk_$n = ${kexp()}
                true
            }
        }""" }

        val (xid,xtag,xcnd) = when {
            (!a && !b && !c) -> Triple(xno, null, (cnt==null).cond2({"(${xno.str} or true)"},{"true"}))
            ( a && !b &&  c) -> Triple(id!!, null, "($scnd ${sret(id)})")
            (!a && !b && cnd is Expr.Acc) -> Triple(xno, null, "(await-chk(__${xno.str},$scnd) ${sret(xno)})")
            (!a && !b &&  c) -> Triple(xit, null, "($scnd ${sret(xit)})")
            ( a &&  x &&  c) -> Triple(id!!, tag, "(await-chk(__${id.str},${tag!!.str}) and $scnd ${sret(id)})")
            ( a &&  x && !c) -> Triple(id!!, tag, "(await-chk(__${id.str},${tag!!.str} ${sret(id)}))")
            (!a &&  x &&  c) -> Triple(xit, tag, "(await-chk(__${xit.str},${tag!!.str}) and $scnd ${sret(xit)})")
            (!a &&  x && !c) -> Triple(xno, tag, "(await-chk(__${xno.str},${tag!!.str}) ${sret(xno)})")
            ( a &&  y &&  c) -> Triple(id!!, clk1, "(await-chk(__${id!!.str},:Clock) and ${kchk(id!!.str)} and $scnd ${sret(id)})")
            ( a &&  y && !c) -> Triple(id!!, clk1, "(await-chk(__${id!!.str},:Clock) and ${kchk(id!!.str)} ${sret(id)}))")
            (!a &&  y &&  c) -> Triple(xit, clk1, "(await-chk(__${xit.str},:Clock) and ${kchk(xit.str)} and $scnd ${sret(xit)})")
            (!a &&  y && !c) -> Triple(xno, clk1, "(await-chk(__${xno.str},:Clock) and ${kchk(xno.str)} ${sret(xno)})")
            else -> error("impossible case")
        }
        val xtask = (!a && !b && cnd is Expr.Acc).cond2({ """
            ${cnd!!.tostr(true)} thus { (type(it)==:exe-task) and (status(it)==:terminated) }
        """ },{
            "false"
        })

        return this.nest(when (type) {
            "await" -> """
                ${pre0}loop {
                    ${pre0}break if (${pre0}yield() thus { ${xid.str} ${xtag.cond{it.str}} =>
                        ${pre0}$xcnd ${cnt.cond { "and (do { ${it.tostr(true)} } or true)" }}
                    })
                }
            """ //.let { println(it);it }
            "every" -> """
                ${pre0}loop {
                    ${pre0}break if (${pre0}yield() thus { ${xid.str} ${xtag.cond{it.str}} =>
                        ${pre0}$xcnd and loop {
                             ${cnt!!.tostr(true)}
                             break (false) if true
                        }
                    })
                }
            """
            "watching" -> """
                ${pre0}par-or {
                    ${pre0}loop {
                        ${pre0}break if (${pre0}yield() thus { ${xid.str} ${xtag.cond{it.str}} =>
                            ${pre0}$xcnd
                        })
                    }
                } with {
                    ${this.block().es.tostr(true)}
                }
            """
            else -> error("impossible case")
        }.let {
            if (!y) {
                """
                if ($xtask) {
                    ;; task terminated
                } else {
                    $it
                    delay
                }
                """
            } else {
                """
                do {
                    ${kdcl()}
                    $it
                    delay
                }
                """
            }
        }) //.let { println(it);it })
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
                val tk0 = this.tk0
                val arg = if (!this.acceptFix("(")) null else {
                    val ret = this.id_tag()
                    this.acceptFix_err(")")
                    ret
                }
                Expr.Do(tk0, arg, this.block().es)
            }
            (CEU>=6 && this.acceptFix("export")) -> {
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
                if (CEU>=99 && dst is Expr.Do && dst.es[0] is Expr.Pass && dst.es[1].let { it is Expr.Do && (it.arg!=null) }) {
                    val xdo = dst.es[1] as Expr.Do
                    val arg = dst.es[0] as Expr.Pass
                    val (dcl,_) = xdo.arg!!
                    val c = xdo.es[0] as Expr.Nat
                    when (c.tk.str) {
                        "/* = */" -> this.nest("""
                            do {
                                pass ${arg.e.tostr()}
                                do (${dcl.str}) {
                                    set ${dcl.str}[#${dcl.str}-1] = ${src.tostr(true)}
                                }
                            }
                        """)
                        "/* + */" -> this.nest("""
                            do {
                                pass ${arg.e.tostr()}
                                do (${dcl.str}) {
                                    set ${dcl.str}[#${dcl.str}] = ${src.tostr(true)}
                                }
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
                val (id_tag,cnd) = if (CEU >= 99) id_tag_cnd__ifs() else Pair(null,this.expr())
                val arr = (CEU>=99) && this.acceptFix("=>")
                val t = if (arr) {
                    Expr.Do(this.tk0, null, listOf(this.expr_1_bin()))
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
                        Expr.Do(this.tk0, null, listOf(this.expr_1_bin()))
                    }
                    else -> {
                        Expr.Do(tk0, null, listOf(Expr.Nil(Tk.Fix("nil", tk0.pos.copy()))))
                    }
                }
                if (id_tag == null) {
                    Expr.If(tk0, cnd, t, f)
                } else {
                    val (id,tag) = id_tag
                    this.nest("""
                        ((${cnd.tostr(true)}) thus { ${id.str} ${tag.cond{it.str}} =>
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
            this.acceptFix("loop") -> {
                if (CEU<99 || this.checkFix("{")) {
                    return Expr.Loop(this.tk0 as Tk.Fix, Expr.Do(this.tk0, null, this.block().es))
                }

                val xid = this.acceptEnu("Id")
                val (id,tag) = if (!xid) Pair("it","") else {
                    Pair(this.tk0.str, if (this.acceptEnu("Tag")) this.tk0.str else "")
                }
                this.acceptFix_err("in")

                when {
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
                                var $id $tag = ${eA.tostr(true)} $op (
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
                                val ceu_$N = iter(${iter.tostr(true)})
                                loop {
                                    val $id $tag = ceu_$N[0](ceu_$N)
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
                val isrec = (CEU>=99 && this.acceptTag(":rec") && this.checkEnu_err("Id"))
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
                    !isrec -> this.nest("""
                        ${tk0.pos.pre()}val ${dcl.str} = ${proto.tostr(true)}
                    """)
                    else -> this.nest("""
                        export [${dcl.str}] {
                            ${tk0.pos.pre()}var ${dcl.str}
                            set ${dcl.str} = ${proto.tostr(true)}
                        }
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
                    Expr.Do(Tk.Fix("do",tpl.pos), null, l)
                }
            }
            this.acceptFix("pass") -> Expr.Pass(this.tk0 as Tk.Fix, this.expr())
            this.acceptFix("drop") -> Expr.Drop(this.tk0 as Tk.Fix, this.expr_in_parens()!!)

            (CEU>=2 && this.acceptFix("catch")) -> {
                // ()                   ;; (null, null, null)   ;; (_  :_ => (_ or true))
                // (x :Y => z)          ;; (id,   tag,  exp)    ;; (x  :Y => chk(:Y) and z)
                // (x :Y)               ;; error                ;; error
                // (x => z)             ;; (id,   null, exp)    ;; (x  :_ => z)
                // :Y => z              ;; (null, tag,  exp)    ;; (it :Y => chk(:Y) and z)
                // :Y                   ;; (null, tag,  null)   ;; (_  :Y => chk(:Y) and (_ or true))
                // z                    ;; (null, null, Acc)    ;; (_  :_ => chk(z))
                // z                    ;; (null, null, exp)    ;; (it :_ => z)

                val tk0 = this.tk0 as Tk.Fix
                val (id,tag_clk,cnd) = this.id_tag_cnd__clock__catch_await()
                if (tag_clk is List<*>) {
                    err((tag_clk as Clock)[0].second.tk, "catch error : invalid condition")
                }

                val tag = if (tag_clk is Tk.Tag) (tag_clk as Tk.Tag) else null
                val (a,b,c) = Triple(id!=null, tag_clk!=null, cnd!=null)
                if (CEU < 99) {
                    assert(a && c)
                }
                if (a && b && !c) {
                    err(id!!, "catch error : innocuous identifier")
                }

                val xit = Tk.Id("it",tk0.pos,0)
                val xno = Tk.Id("ceu_$N",tk0.pos,0)
                val scnd = cnd?.tostr(true)
                val (xid,xtag,xcnd) = when {
                    (CEU < 99)       -> Triple(id!!, tag, scnd)
                    (!a && !b && !c) -> Triple(xno, null, "true")
                    ( a &&  b &&  c) -> Triple(id!!, tag, "((${id.str} is? ${tag!!.str}) and $scnd)")
                    ( a && !b &&  c) -> Triple(id!!, null, scnd)
                    (!a &&  b &&  c) -> Triple(xit, tag, "((${xit.str} is? ${tag!!.str}) and $scnd)")
                    (!a &&  b && !c) -> Triple(xno, tag, "(${xno.str} is? ${tag!!.str})")
                    (!a && !b && cnd is Expr.Acc) -> Triple(xno, null, "(${xno.str} is? $scnd)")
                    (!a && !b && c) -> Triple(xit, null, scnd)
                    else -> error("impossible case")
                }

                val xxcnd = this.nest("""
                    do {
                        pass `:ceu ceu_acc.Dyn->Throw.val`
                        do (${xid.pos.pre()+xid.str} ${xtag.cond{it.pos.pre()+it.str}}) {
                            $xcnd
                        }
                    }
                """)

                Expr.Catch(tk0, xxcnd as Expr.Do, this.block())
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
                    (call.args.lastOrNull().let { it is Expr.Acc && it.tk.str=="..." }) -> err(tk1, "spawn error : \"...\" is not allowed")
                }
                val tasks = if (CEU<5 || !this.acceptFix("in")) null else {
                    this.expr()
                }
                call as Expr.Call
                Expr.Spawn(tk0, tasks, call.clo, call.args)
            }
            (CEU>=4 && this.acceptFix("delay")) -> Expr.Delay(this.tk0 as Tk.Fix)
            (CEU>=4 && this.acceptFix("pub")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val tsk = this.expr_in_parens(true)
                Expr.Pub(tk0, tsk)
            }
            (CEU>=4 && this.acceptFix("broadcast")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val evt = this.expr_in_parens()!!
                val xin = if (this.acceptFix("in")) {
                    this.expr()
                } else {
                    Expr.Tag(Tk.Tag(":task",this.tk0.pos))
                }
                Expr.Call(tk0,
                    Expr.Acc(Tk.Id("broadcast'", tk0.pos, 0)),
                    listOf(evt,xin)
                )
            }
            (CEU>=4 && this.acceptFix("delay")) -> {
                Expr.Call(this.tk0 as Tk.Fix,
                    Expr.Acc(Tk.Id("delay'", this.tk0.pos, 0)),
                    listOf()
                )
            }
            (CEU>=4 && this.acceptFix("toggle")) -> {
                val tk0 = this.tk0 as Tk.Fix
                if (CEU>=99 && this.acceptEnu("Tag")) {
                    val tag = this.tk0 as Tk.Tag
                    val blk = this.block()
                    this.nest("""
                        do {
                            val task_$N = spawn task ;;{
                                ${blk.tostr(true)}
                            ;;}
                            watching task_$N {
                                loop {
                                    await(${tag.str} => not it[0])
                                    toggle task_$N(false)
                                    await(${tag.str} => it[0])
                                    toggle task_$N(true)
                                }
                            }
                            pub(task_$N)
                        }
                    """)//.let { println(it.tostr()); it }
                } else {
                    val tsk = this.expr_prim()
                    val on  = this.expr_in_parens()!!
                    Expr.Toggle(tk0, tsk, on)
                }
            }
            (CEU>=5 && this.acceptFix("detrack")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val tsk = this.expr_in_parens()!!
                //this.acceptFix_err("thus")
                return if (this.checkFix("{")) {
                    val tk1 = this.tk1
                    val (id_tag, es) = lambda(N)
                    val (id, tag) = id_tag
                    val blk = this.nest("""
                        (${tk1.pos.pre()}func (${id.pos.pre() + id.str} ${tag.cond { it.pos.pre() + it.str }}) {
                            if ${id.str} {
                                ```
                                CEU_Stack ceu_dstk_$N = { &ceu_acc.Dyn->Exe_Task, 1, ceu_dstk };
                                ceu_dstk = &ceu_dstk_$N;
                                ```
                                ${es.tostr(true)}
                            } else {
                                nil
                            }
                        }) (${tk0.pos.pre()}detrack'(${tsk.tostr(true)}))
                    """)
                    Expr.Dtrack(tk0, blk as Expr.Call)
                } else {
                    this.nest("${tk0.pos.pre()}detrack''(${tsk.tostr(true)})")
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
                val (id,tag) = id_tag
                return this.nest("""
                    (func (${id.str} ${tag.cond{it.str}}) {
                        ${es.tostr(true)}
                    })
                """)
            }
            (CEU>=99 && this.acceptFix("ifs")) -> {
                val (idtag1,v) = if (this.checkFix("{")) {
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
                                "$op($idtag1)"
                            } else {
                                "$op($idtag1, ${e.tostr(true)})"
                            }
                            Pair(null, this.nest(call))
                        }
                        else -> {
                            id_tag_cnd__ifs()
                        }
                    }
                    val blk = if (this.acceptFix("=>")) {
                        Expr.Do(this.tk0, null, listOf(this.expr()))
                    } else {
                        this.block()
                    }
                    Pair(Pair(id_tag,cnd),blk)
                }
                //ifs.forEach { println(it.first.third.tostr()) ; println(it.second.tostr()) }
                this.acceptFix_err("}")
                this.nest("""
                    ((${v.cond2({it.tostr(true)},{"nil"})}) thus { $idtag1 =>
                    ${ifs.map { (xxx,blk) ->
                        val (idtag2,cnd) = xxx
                        """
                        if ${idtag2.cond{ (id,tag)-> "${id.str} ${tag?.str ?: ""} = "}} ${cnd.tostr(true)} {
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
                await("await")
            }
            (CEU>=99 && this.acceptFix("every")) -> await("every")
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
                            nil
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
            (CEU>=99 && this.acceptFix("watching")) -> await("watching")
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
    // expr_4_suf : v[0]    v.x    v.1    v.(:T).x    f()    f \{...}    v thus {...}
    // expr_prim

    fun expr_4_suf (xe: Expr? = null): Expr {
        val e = if (xe != null) xe else this.expr_prim()
        val ok = this.tk0.pos.isSameLine(this.tk1.pos) && (
                (CEU>=99 && this.acceptFix("thus")) || this.acceptFix("[") || this.acceptFix(".") || this.acceptFix("(")
                 )
        val op = this.tk0
        if (!ok) {
            return e
        }

        return this.expr_4_suf(
            when (this.tk0.str) {
                "[" -> {
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
                    (CEU>=99 && this.acceptEnu("Num")) -> {
                        val num = this.tk0 as Tk.Num
                        if (num.str.contains('.')) {
                            err(num, "index error : ambiguous dot : use brackets")
                        }
                        Expr.Index(e.tk, e, Expr.Num(num))
                    }
                    (CEU>=99 && this.acceptFix("(")) -> {
                        val n = N
                        this.acceptEnu_err("Tag")
                        val tag = this.tk0
                        this.acceptFix_err(")")
                        val acc = Expr.Acc(Tk.Id("ceu_$n",e.tk.pos,0))
                        this.nest("""
                            ${e.tostr(true)} thus { ceu_$n ${tag.str} =>
                                ${this.expr_4_suf(acc).tostr(true)}
                            }
                        """) //.let { println(it);it })
                    }
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
                "thus" -> {
                    val tk1 = this.tk1
                    val (id_tag,es) = lambda(N)
                    val (id,tag) = id_tag
                    this.nest( """
                        do {
                            pass (${e.tostr(true)})
                            ${tk1.pos.pre()}do (${id.pos.pre()}${id.str} ${tag.cond {it.str}}) {
                                ${es.tostr(true)}
                            }
                        }
                    """)
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
                    val tk0 = this.tk0
                    val tup = this.expr_prim()
                    this.nest("""
                            ${tk0.pos.pre()}tags(${tup.tostr(true)}, ${tk0.str}, true)
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
                "and" -> {
                    if (e1.is_evaled()) {
                        this.nest("""
                            if ${e1.tostr(true)} {
                                ${e2.tostr(true)}
                            } else {
                                ${e1.tostr(true)}
                            }
                    """)
                    } else {
                        this.nest("""
                        ((${e1.tostr(true)}) thus { ceu_${e1.n} =>
                            if ceu_${e1.n} {
                                ${e2.tostr(true)}
                            } else {
                                ceu_${e1.n}
                            }
                        })
                    """)
                    }
                }
                "or" -> {
                    if (e1.is_evaled()) {
                        this.nest("""
                            if ${e1.tostr(true)} {
                                ${e1.tostr(true)}
                            } else {
                                ${e2.tostr(true)}
                            }
                        })
                    """)
                    } else {
                        this.nest("""
                        ((${e1.tostr(true)}) thus { ceu_${e1.n} =>  
                            if ceu_${e1.n} {
                                ceu_${e1.n}
                            } else {
                                ${e2.tostr(true)}
                            }
                        })
                    """)
                    }
                }
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
        val e = if (xe != null) xe else this.expr_1_bin()
        val ok = (CEU>=99 && (this.acceptFix("where")) || this.acceptFix("-->") || this.acceptFix("<--"))
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
                        ${op.pos.pre()}export [] {
                            ${body.es.tostr(true)}
                            ${e.tostr(true)}
                        }
                    """)
                )
            }
            "-->" -> this.expr_0_out(op.str, method(this.expr_1_bin(), e, true))
            "<--" -> method(e, this.expr_0_out(op.str), false)
            else -> error("impossible case")
        }
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
