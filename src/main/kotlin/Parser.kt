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
        val lexer = Lexer(inps)
        val parser = Parser(lexer)
        return parser.expr()
    }

    fun checkSep (): Boolean {
        return (this.tk1 is Tk.Fix && this.tk1.str in listOf("{","}",",","]",")","->") || this.tk1 is Tk.Eof || this.tk1 is Tk.Op)
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

    fun expr_in_parens (req: Boolean, nil: Boolean): Expr? {
        this.acceptFix_err("(")
        val e = when {
            (req || !this.checkFix(")")) -> {
                val e = this.expr()
                this.acceptFix_err(")")
                e
            }
            nil -> {    // can be empty
                this.acceptFix_err(")")
                xnil(this.tk0.pos)
            }
            else -> {
                this.acceptFix_err(")")
                null
            }
        }
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

    fun clk_or_exp (): Pair<List<Pair<Expr,Tk.Tag>>?,Expr?> {
        fun isclk (): Boolean {
            return listOf(":h",":min",":s",":ms").any { this.acceptTag(it) }
        }
        val e = this.expr()
        if (!isclk()) {
            return Pair(null, e)
        } else {
            val es = mutableListOf(Pair(e, this.tk0 as Tk.Tag))
            while (isclk()) {
                es.add(Pair(this.expr(), this.tk0 as Tk.Tag))
            }
            return Pair(es, null)
        }
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

    fun lambda (): Expr.Proto {
        this.acceptFix_err("\\")
        val tk0 = this.tk0
        val args = if (!this.checkEnu("Id")) {
            listOf(Pair(Tk.Id("it",this.tk0.pos,0),null))
        } else {
            this.args("{")
        }
        val blk = this.block()
        // func ($args) $blk
        return Expr.Proto(Tk.Fix("func",tk0.pos), null, args, blk)
    }

    data class Await (val now: Boolean, val spw: Boolean, val clk: List<Pair<Expr, Tk.Tag>>?, val cnd: Expr?, val xcnd: Pair<Boolean?,Expr?>?)
    fun await (): Await {
        val now = this.acceptTag(":check-now")
        val spw = this.checkFix("spawn")
        val (clk,cnd) = if (spw) Pair(null,null) else this.clk_or_exp()
        val xcnd = when {
            (cnd !is Expr.Tag) -> null   // await :key
            !this.acceptFix(",") -> Pair(true,null)
            else -> Pair(null, this.expr())
        }
        return Await(now, spw, clk, cnd, xcnd)
    }
    fun Await.tostr (): String {
        val clk_expr = clk_or_exp_tostr(Pair(this.clk,this.cnd))
        val xcnd = when {
            (this.xcnd == null) -> ""
            (this.xcnd.first == true) -> ""
            else -> ", " + this.xcnd.second!!.tostr(true)
        }
        return "await ${this.now.cond { ":check-now" }} $clk_expr $xcnd"
    }

    fun clk_or_exp_tostr (clk_exp: Pair<List<Pair<Expr,Tk.Tag>>?,Expr?>): String {
        return when {
            (clk_exp.second != null) -> clk_exp.second!!.tostr(true)
            (clk_exp.first  != null) -> clk_exp.first!!.map { (e,t) -> e.tostr(true)+t.str }.joinToString(" ")
            else -> error("impossible case")
        }
    }

    fun id_tag_cnd (): Triple<String?,Tk.Tag?,Expr> {
        val id_or_cnd = this.expr()
        return when {
            !XCEU -> Triple(null, null, id_or_cnd)
            (id_or_cnd !is Expr.Acc) -> Triple(null, null, id_or_cnd)
            this.acceptFix("=") -> Triple(id_or_cnd.tk.str, null, this.expr())
            this.acceptEnu("Tag") -> {
                val tag = this.tk0 as Tk.Tag
                this.acceptFix_err("=")
                Triple(id_or_cnd.tk.str, tag, this.expr())
            }
            else -> Triple(null, null, id_or_cnd)
        }
    }

    fun xor (tk: Tk, e1: Expr, e2: Expr): Expr.Do {
        // do { val :tmp x=$e1 ; if x -> x -> $e2 }
        fun xid (): Tk.Id {
            return Tk.Id("ceu_${e1.n}", tk.pos, 0)
        }
        return Expr.Do(Tk.Fix("do", tk.pos), listOf(
            Expr.Dcl(Tk.Fix("val",tk.pos), xid(), true, null, true, e1),
            Expr.If(Tk.Fix("if",tk.pos),
                Expr.Acc(xid()),
                Expr.Do(tk, listOf(Expr.Acc(xid()))),
                Expr.Do(tk, listOf(e2))
            )
        ))
    }
    fun xnil (pos: Pos): Expr.Nil {
        return Expr.Nil(Tk.Fix("nil", pos))
    }
    fun xacc (pos: Pos, id: String): Expr.Acc {
        return Expr.Acc(Tk.Id(id, pos, 0))
    }
    fun xnum (pos: Pos, n: Int): Expr.Num {
        return Expr.Num(Tk.Num(n.toString(), pos))
    }

    fun xop (op: Tk.Op, e1: Expr, e2: Expr): Expr {
        return when (op.str) {
            "or" -> xor(op, e1, e2)
            "and" -> {
                // do { val :tmp x=$e ; if x -> $e2 -> x }
                fun xid (): Tk.Id {
                    return Tk.Id("ceu_${e1.n}", e1.tk.pos, 0)
                }
                Expr.Do(Tk.Fix("do", e1.tk.pos), listOf(
                    Expr.Dcl(Tk.Fix("val",e1.tk.pos), xid(), true, null, true, e1),
                    Expr.If(Tk.Fix("if",e1.tk.pos),
                        Expr.Acc(xid()),
                        Expr.Do(op, listOf(e2)),
                        Expr.Do(op, listOf(Expr.Acc(xid())))
                    )
                ))
            }
            "is?" -> Expr.Call(op, xacc(op.pos,"is'"), listOf(e1, e2))
            "is-not?" -> Expr.Call(op, xacc(op.pos,"is-not'"), listOf(e1, e2))
            "in?" -> Expr.Call(op, xacc(op.pos,"in'"), listOf(e1, e2))
            "in-not?" -> Expr.Call(op, xacc(op.pos,"in-not'"), listOf(e1, e2))
            else -> Expr.Call(op, xacc(op.pos,"{${op.str}}"), listOf(e1,e2))
        }
    }

    fun exprPrim (): Expr {
        return when {
            this.acceptFix("do") -> Expr.Do(this.tk0, this.block().es)
            this.acceptFix("export") -> {
                val tk0 = this.tk0 as Tk.Fix
                val ids = if (XCEU && this.checkFix("{")) emptyList() else {
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
                val tmp = this.acceptTag(":tmp")
                this.acceptFix("evt") || this.acceptEnu_err("Id")
                val id = this.tk0.let { if (it is Tk.Id) it else Tk.Id("evt",it.pos,0) }
                if (id.str == "...") {
                    err(this.tk0, "invalid declaration : unexpected ...")
                }
                if (tmp && tk0.str!="val") {
                    err(this.tk0, "invalid declaration : expected \"val\" for \":tmp\"")
                }
                val tag = if (!this.acceptEnu("Tag")) null else {
                    this.tk0 as Tk.Tag
                }
                val (tag2,src) = if (!this.acceptFix("=")) Pair(null,null) else {
                    val tag = if (XCEU && this.checkEnu("Tag")) this.tk1 as Tk.Tag else null
                    Pair(tag,this.expr())
                }
                if (tag==null && tag2!=null && src is Expr.Call) {
                    Expr.Dcl(tk0, id, tmp, tag2, true, src)
                } else {
                    Expr.Dcl(tk0, id, tmp, tag, true, src)
                }
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0 as Tk.Fix
                val dst = this.expr()
                if (dst is Expr.Acc && dst.tk.str == "...") {
                    err(this.tk0, "invalid set : unexpected ...")
                }
                this.acceptFix_err("=")
                val src = this.expr()
                if (!XCEU || !(dst is Expr.Export)) {
                    if (!(dst is Expr.Acc || dst is Expr.Index || (dst is Expr.Pub && dst.tk.str=="pub"))) {
                        err(tk0, "invalid set : invalid destination")
                    }
                    Expr.Set(tk0, dst, /*null,*/ src)
                } else {
                    Expr.Export(dst.tk_, dst.ids, Expr.Do(dst.body.tk, dst.body.es.dropLast(1) +
                        Expr.Set(dst.tk_, dst.body.es.last(), src)
                    ))
                }
            }
            this.acceptFix("if") -> {
                val tk0 = this.tk0 as Tk.Fix
                val (id,tag,cnd) = id_tag_cnd()
                val arr = XCEU && this.acceptFix("->")
                val t = if (arr) {
                    Expr.Do(this.tk0, listOf(this.expr()))
                } else {
                    this.block()
                }
                val f = when {
                    !XCEU -> {
                        this.acceptFix_err("else")
                        this.block()
                    }
                    this.acceptFix("else") -> {
                        this.block()
                    }
                    arr && this.acceptFix_err("->") -> {
                        Expr.Do(this.tk0, listOf(this.expr()))
                    }
                    else -> {
                        Expr.Do(tk0, listOf(Expr.Pass(Tk.Fix("pass", tk0.pos.copy()), xnil(tk0.pos))))
                    }
                }
                if (id == null) {
                    Expr.If(tk0, cnd, t, f)
                } else {
                    val nn = N
                    fun xid (): Tk.Id {
                        return Tk.Id("ceu_$nn", tk0.pos, 0)
                    }
                    // export { val x $tag=$cnd ; if x { val $id $tag=x ; $t } else { $f } }
                    Expr.Export(tk0, emptyList(), Expr.Do(tk0, listOf(
                        Expr.Dcl(Tk.Fix("val", tk0.pos), xid(), false, tag, true, cnd),
                        Expr.If(tk0, Expr.Acc(xid()),
                            Expr.Do(tk0, listOf(
                                Expr.Dcl(Tk.Fix("val",tk0.pos), Tk.Id(id,tk0.pos,0), false, tag, true, Expr.Acc(xid())),
                                t
                            )),
                            Expr.Do(tk0, f.es)
                        )
                    )))
                }
            }
            this.acceptFix("xbreak") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptEnu_err("Num")
                Expr.XBreak(tk0, this.tk0.str.toInt())
            }
            this.acceptFix("loop") -> {
                val tk0 = this.tk0 as Tk.Fix
                val pre0 = tk0.pos.pre()

                if (this.acceptEnu("Num")) {
                    val nn = this.tk0.str.toInt()
                    val blk = this.block()
                    return Expr.Loop(tk0, nn, Expr.Do(tk0, blk.es))
                }

                val xin = this.acceptFix("in")
                val nn = N++

                val f: (List<Expr>) -> Expr = when {
                    !xin -> {
                        // do { loop $nn { body } }
                        { body: List<Expr> ->
                            Expr.Do(Tk.Fix("do", tk0.pos), listOf(
                                Expr.Loop(tk0, nn, Expr.Do(tk0,body))
                            ))
                        }
                    }
                    this.acceptTag(":tasks") -> {
                        val tasks = this.expr()
                        val i = if (XCEU && !this.checkFix(",")) "it" else {
                            this.acceptFix_err(",")
                            this.acceptEnu_err("Id")
                            this.tk0.str
                        }
                        { body -> this.nest("""
                            ${pre0}do {
                                val ceu_tasks_$N = ${tasks.tostr(true)}
                                ```
                                if (ceu_mem->ceu_tasks_$N.type != CEU_VALUE_X_TASKS) {                
                                    CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${tasks.tk.pos.file} : (lin ${tasks.tk.pos.lin}, col ${tasks.tk.pos.col}) : loop error : expected tasks");
                                }
                                ```
                                val ceu_n_$N = `:number ceu_mem->ceu_tasks_$N.Dyn->Bcast.Tasks.dyns.its`
                                var ceu_i_$N = 0
                                ${pre0}loop $nn {
                                    if ceu_i_$N == ceu_n_$N {
                                        pass nil     ;; return value
                                        `goto CEU_LOOP_DONE_$nn;`
                                    } else { nil }
                                    val ceu_dyn_$N = `:pointer ceu_mem->ceu_tasks_$N.Dyn->Bcast.Tasks.dyns.buf[(int)ceu_mem->ceu_i_$N.Number]`
                                    if ceu_dyn_$N == `:pointer NULL` {
                                        ;; empty slot
                                        set ceu_i_$N = `:number ceu_mem->ceu_i_$N.Number + 1` ;; just to avoid prelude
                                    ${pre0}
                                    } else {
                                        ;;;
                                        val ceu_x_$N
                                        `ceu_mem->ceu_x_$N = (CEU_Value) { CEU_VALUE_X_TASK, {.Dyn=ceu_mem->ceu_dyn_$N.Pointer} };`
                                        val $i = track(ceu_x_$N)
                                        ;;;

                                        ```
                                        CEU_Value ceu_x_$N = { CEU_VALUE_X_TASK, {.Dyn=ceu_mem->ceu_dyn_$N.Pointer} };
                                        ```
                                        val $i = track(`:ceu ceu_x_$N`)
                                        ${body.tostr()}
                                        if detrack($i) {
                                            set ceu_i_$N = `:number ceu_mem->ceu_i_$N.Number + 1` ;; just to avoid prelude
                                        } else {
                                            set ceu_i_$N = ceu_n_$N
                                        }
                                    }
                                }
                            }
                            """)
                        }
                    }
                    XCEU && (this.acceptFix("[") || this.acceptFix("(")) -> {
                        // [x -> y]
                        val tkA = this.tk0 as Tk.Fix
                        val eA = this.expr()
                        this.acceptFix_err("->")
                        val eB = this.expr()
                        (this.acceptFix("]") || this.acceptFix_err(")"))
                        val tkB = this.tk0 as Tk.Fix

                        // , :step +z
                        var x = this.acceptFix(",")
                        val (op,step) = if (x && this.acceptTag(":step")) {
                            (this.acceptOp("-") || acceptOp_err("+"))
                            Pair(this.tk0.str, this.expr())
                        } else {
                            Pair("+", null)
                        }

                        // , i :T
                        x = (step==null && x) || this.acceptFix(",")
                        val (i,tag) = if (!x) Pair("it",null) else {
                            this.acceptEnu_err("Id")
                            val id = this.tk0 as Tk.Id
                            val tag = if (this.acceptEnu("Tag")) this.tk0 as Tk.Tag else null
                            Pair(id.str,tag)
                        }

                        val cmp = when {
                            (tkB.str=="]" && op=="+") -> ">"
                            (tkB.str==")" && op=="+") -> ">="
                            (tkB.str=="]" && op=="-") -> "<"
                            (tkB.str==")" && op=="-") -> "<="
                            else -> error("impossible case")
                        }

                        fun xstep (): Tk.Id {
                            return Tk.Id("ceu_step_$nn", tk0.pos, 0)
                        }
                        fun xi (): Tk.Id {
                            return Tk.Id(i, tk0.pos, 0)
                        }
                        fun xlimit (): Tk.Id {
                            return Tk.Id("ceu_limit_$nn", tk0.pos, 0)
                        }

                        { body ->
                            /*
                            do {
                                val $step = $step or 1
                                var $i $tag = $e $op (0 or $step)
                                val $limit = $eB
                                loop $nn {
                                    if $i $cmp $limit {
                                        pass nil     ;; return value
                                        xbreak $nn
                                    } else { nil }
                                    $body
                                    set $i = $i $op $step
                                }                                
                            }
                            */
                            Expr.Do(Tk.Fix("do",tk0.pos), listOf(
                                Expr.Dcl(Tk.Fix("val",tk0.pos), xstep(), false, null, true,
                                    if (step==null) xnum(tk0.pos,1) else step
                                ),
                                Expr.Dcl(Tk.Fix("var",tk0.pos), xi(), false, tag, true,
                                    Expr.Call(tk0, xacc(tk0.pos,"{$op}"), listOf(
                                        eA,
                                        if (tkA.str=="[") xnum(tk0.pos,0) else Expr.Acc(xstep())
                                    ))
                                ),
                                Expr.Dcl(Tk.Fix("val",tk0.pos), xlimit(), false, null, true, eB),
                                Expr.Loop(tk0, nn, Expr.Do(tk0,
                                    listOf(Expr.If(tk0,
                                        Expr.Call(tk0,
                                            xacc(tk0.pos, "{$cmp}"),
                                            listOf(
                                                Expr.Acc(xi()),
                                                Expr.Acc(xlimit())
                                            )
                                        ),
                                        Expr.Do(tk0, listOf(
                                            Expr.Pass(tk0, xnil(tk0.pos)),
                                            Expr.XBreak(tk0, nn)
                                        )),
                                        Expr.Do(tk0, listOf(xnil(tk0.pos)))
                                    )) +
                                    body +
                                    listOf(Expr.Set(tk0, Expr.Acc(xi()), Expr.Call(
                                        tk0, xacc(tk0.pos, "{$op}"), listOf(Expr.Acc(xi()), Expr.Acc(xstep()))
                                    )))
                                ))
                            ))
                        }
                    }
                    XCEU -> {
                        val iter = this.expr()
                        val (i,tag) = if (!this.checkFix(",")) Pair("it",null) else {
                            this.acceptFix_err(",")
                            this.acceptEnu_err("Id")
                            val id = this.tk0 as Tk.Id
                            val tag = if (this.acceptEnu("Tag")) this.tk0 as Tk.Tag else null
                            Pair(id.str,tag)
                        }
                        val nnn = N
                        fun xid (): Tk.Id {
                            return Tk.Id("ceu_it_$nnn", tk0.pos, 0)
                        }
                        fun xi (): Tk.Id {
                            return Tk.Id(i, tk0.pos, 0)
                        }
                        // do { val x :Iterator=iter ; loop $nn { val $i $tag=$xid.f($xid) ; if $i==nil { pass nil ; xbreak $nn } ; $body
                        { body -> Expr.Do(Tk.Fix("do",tk0.pos), listOf(
                            Expr.Dcl(Tk.Fix("val",tk0.pos), xid(), false, Tk.Tag(":Iterator",tk0.pos), true, iter),
                            Expr.Loop(tk0, nn, Expr.Do(tk0, listOf(
                                Expr.Dcl(Tk.Fix("val",tk0.pos), xi(), false, tag, true,
                                    Expr.Call(tk0,
                                        Expr.Index(tk0, Expr.Acc(xid()), xnum(tk0.pos,0)),
                                        listOf(Expr.Acc(xid()))
                                    )
                                ),
                                Expr.If(tk0,
                                    Expr.Call(tk0, xacc(tk0.pos, "{==}"), listOf(
                                        Expr.Acc(xi()),
                                        xnil(tk0.pos)
                                    )),
                                    Expr.Do(tk0, listOf(
                                        Expr.Pass(tk0, xnil(tk0.pos)),
                                        Expr.XBreak(tk0, nn)
                                    )),
                                    Expr.Do(tk0, listOf(xnil(tk0.pos)))
                                )) + body
                            ))))
                        }
                    }
                    else -> {
                        err(this.tk1, "invalid loop : unexpected ${this.tk1.str}")
                        error("unreachable")
                    }
                }

                val brk = if (!(this.acceptFix("until")||XCEU && this.acceptFix("while"))) emptyList() else {
                    val tk1 = this.tk0
                    val brk = (tk1.str == "until")
                    val (id,tag,cnd) = id_tag_cnd()
                    val nnn = N++
                    fun xid (): Tk.Id {
                        return Tk.Id(id ?: "ceu_$nnn", tk1.pos, 0)
                    }
                    // val $id $tag=cnd ; if not $id { xbreak $nn } else { nil }
                    val t = Expr.XBreak(Tk.Fix("xbreak", this.tk1.pos), nn)
                    val f = xnil(this.tk1.pos)
                    listOf(
                        Expr.Dcl(Tk.Fix("val",tk1.pos), xid(), false, tag, true, cnd),
                        Expr.If(Tk.Fix("if",tk1.pos), Expr.Acc(xid()),
                            Expr.Do(tk1, listOf(if (brk) t else f)),
                            Expr.Do(tk1, listOf(if (brk) f else t))
                        )
                    )
                }

                val blk = this.block().es

                fun untils (): List<Expr> {
                    if (! (this.acceptFix("until") || XCEU&&this.acceptFix("while"))) {
                        return emptyList()
                    }
                    val brk = (this.tk0.str == "until")
                    val (id,tag,cnd) = id_tag_cnd()
                    val xblk = if (!this.checkFix("{")) emptyList() else {
                        this.block().es + untils()
                    }
                    val nnn = N++
                    val tk1 = this.tk0
                    fun xid (): Tk.Id {
                        return Tk.Id(id ?: "ceu_$nnn", tk1.pos, 0)
                    }
                    // val $id $tag=$cnd ; if [$not] $id { xbreak $id } ; $xblk
                    val t = Expr.XBreak(Tk.Fix("xbreak",tk1.pos), nn)
                    val f = xnil(tk1.pos)
                    return listOf(
                        Expr.Dcl(Tk.Fix("val",tk1.pos), xid(), false, tag, true, cnd),
                        Expr.If(Tk.Fix("if",tk1.pos), Expr.Acc(xid()),
                            Expr.Do(tk1, listOf(if (brk) t else f)),
                            Expr.Do(tk1, listOf(if (brk) f else t))
                        )
                    ) + xblk
                }

                f(brk + blk + untils())
            }
            this.acceptFix("func") || this.acceptFix("coro") || this.acceptFix("task") -> {
                val tk0 = this.tk0 as Tk.Fix
                val isnote = (tk0.str=="func" || this.checkFix("(") || this.checkEnu("Tag") || (XCEU && this.checkEnu("Id")))
                if (!isnote) Expr.Self(tk0) else {
                    val isrec = XCEU && this.acceptTag(":rec")
                    val id = if (XCEU && this.acceptEnu("Id")) this.tk0 as Tk.Id else null
                    val args = if (XCEU && !this.checkFix("(")) {
                        listOf(Pair(Tk.Id("it",this.tk0.pos,0),null))
                    } else {
                        this.acceptFix_err("(")
                        val x = this.args(")")
                        this.acceptFix_err(")")
                        x
                    }
                    val task = when {
                        (tk0.str != "task") -> null
                        this.acceptTag(":fake") -> Pair(null, true)
                        this.acceptEnu("Tag") -> Pair(this.tk0 as Tk.Tag, false)
                        else -> Pair(null, false)
                    }
                    val blk = this.block(this.tk1)
                    val proto = Expr.Proto(tk0, task, args, blk)
                    when {
                        (id == null) -> proto
                        !isrec -> {
                            // val $id = $proto
                            Expr.Dcl(Tk.Fix("val",tk0.pos), id, false, null, true, proto)
                        }
                        else -> {
                            // export [$id] { var $id ;  set $id = $proto }
                            Expr.Export(tk0, listOf(id.str), Expr.Do(tk0, listOf(
                                Expr.Dcl(tk0, id, false, null, false, null),
                                Expr.Set(tk0, Expr.Acc(id), proto)
                            )))
                        }
                    }
                }
            }
            this.acceptFix("catch") -> {
                val tk0 = this.tk0 as Tk.Fix
                val cnd = if (XCEU && this.checkFix("{")) Expr.Bool(Tk.Fix("true",this.tk0.pos)) else this.expr()
                val blk = this.block()
                if (XCEU && (cnd is Expr.Tag)) {   // catch :err
                    // catch (err is? $cnd) $blk
                    val cndx = Expr.Call(tk0,
                        xacc(cnd.tk.pos,"is'"),
                        listOf(
                            Expr.EvtErr(Tk.Fix("err", cnd.tk.pos)),
                            cnd
                        ))
                    Expr.Catch(tk0, cndx, blk)
                } else {
                    Expr.Catch(this.tk0 as Tk.Fix, cnd, blk)
                }
            }
            this.acceptFix("defer") -> Expr.Defer(this.tk0 as Tk.Fix, this.block())
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
                            !XCEU -> emptyList()
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
            this.acceptFix("pass") -> Expr.Pass(this.tk0 as Tk.Fix, this.expr())

            this.acceptFix("spawn") -> {
                val tk0 = this.tk0 as Tk.Fix
                when {
                    this.acceptFix("in") -> {
                        val tasks = this.expr()
                        this.acceptFix_err(",")
                        val call = this.expr()
                        if (call !is Expr.Call && !(call is Expr.Export && call.body.es.last() is Expr.Call)) {
                            err(tk1, "invalid spawn : expected call")
                        }
                        Expr.Spawn(tk0, tasks, call)
                    }
                    (XCEU && this.acceptFix("coro")) -> {
                        // spawn (coro () $blk) ()
                        val tk1 = this.tk0 as Tk.Fix
                        this.checkFix_err("{")
                        val blk = this.block()
                        val coro = Expr.Proto(tk1, null, emptyList(), blk)
                        Expr.Spawn(tk0, null, Expr.Call(tk1, coro, emptyList()))
                    }
                    (XCEU && this.checkFix("{")) -> {
                        // spawn (task () :fake { blk }) ()
                        val blk = this.block()
                        val task = Expr.Proto(Tk.Fix("task", tk0.pos), Pair(null,true), emptyList(), blk)
                        Expr.Spawn(tk0, null, Expr.Call(tk0, task, emptyList()))
                    }
                    else -> {
                        val call = this.expr()
                        if (call !is Expr.Call && !(call is Expr.Export && call.body.es.last() is Expr.Call)) {
                            err(tk1, "invalid spawn : expected call")
                        }
                        Expr.Spawn(tk0, null, call)
                    }
                }
            }
            this.acceptFix("broadcast") -> {
                val tk0 = this.tk0 as Tk.Fix
                val xin = if (!this.acceptFix("in")) {
                    Expr.Tag(Tk.Tag(":global", tk0.pos))
                } else {
                    val e = this.expr()
                    this.acceptFix_err(",")
                    e
                }
                val evt = this.expr()
                Expr.Bcast(tk0, xin, evt)
            }
            this.acceptFix("yield") -> Expr.Yield(this.tk0 as Tk.Fix, this.expr_in_parens(!XCEU, XCEU)!!)
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
                val awt = await()
                if (!XCEU && (awt.now || awt.spw || awt.clk!=null || awt.xcnd!=null)) {
                    err(tk0, "invalid toggle")
                }
                val task = awt.cnd
                if (!XCEU || (task is Expr.Call && !(XCEU && this.checkFix("->")))) {
                    task!!
                    if (task !is Expr.Call) {
                        err(task.tk, "invalid toggle : expected argument")
                    }
                    task as Expr.Call
                    if (task.args.size != 1) {
                        err(task.tk, "invalid toggle : expected single argument")
                    }
                    Expr.Toggle(tk0, task.proto, task.args[0])
                } else {
                    this.acceptFix_err("->")
                    val (off,on) = Pair(awt, await())
                    val blk = this.block()
                    this.nest("""
                        ${pre0}do {
                            val task_$N = spawn ;;{
                                ${blk.tostr(true)}
                            ;;}
                            awaiting :check-now task_$N {
                                loop {
                                    ${off.tostr()}
                                    toggle task_$N(false)
                                    ${on.tostr()}
                                    toggle task_$N(true)
                                }
                            }
                            task_$N.pub
                        }
                    """)//.let { println(it.tostr()); it }
                }
            }

            this.acceptFix("evt") || this.acceptFix("err") -> Expr.EvtErr(this.tk0 as Tk.Fix)
            this.acceptEnu("Nat")  -> {
                Expr.Nat(this.tk0 as Tk.Nat)
            }
            this.acceptEnu("Id")   -> Expr.Acc(this.tk0 as Tk.Id)
            this.acceptEnu("Tag")  -> {
                val tag = this.tk0 as Tk.Tag
                val same = this.tk0.pos.isSameLine(this.tk1.pos)
                if (!XCEU || !same || this.checkSep()) {
                    Expr.Tag(tag)
                } else {
                    val nn = N
                    val e = this.exprPrim()
                    if (e is Expr.Tuple) {
                        // :X [...]
                        // tags($e, $tag, true)
                        val tags = xacc(tag.pos, "tags",)
                        Expr.Call(tk0, tags, listOf(e, Expr.Tag(tag), Expr.Bool(Tk.Fix("true",tag.pos))))
                    } else {
                        // do { val :tmp ceu_$nn $tag = $e ; e1 }
                        val id = Tk.Id("ceu_$nn",tag.pos,0)
                        val e1 = this.exprSufsX(Expr.Acc(id))
                        val dcl = Expr.Dcl(Tk.Fix("val",e1.tk.pos), id, true, tag, true, e)
                        Expr.Do(Tk.Fix("do", e.tk.pos), listOf(dcl, e1))
                    }
                }
            }
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
            }).let {
                this.acceptFix_err("]")
                it
            }
            this.checkFix("(")      -> this.expr_in_parens(true,false)!!

            /*
            this.acceptFix("poly") -> {
                val tk0 = this.tk0 as Tk.Fix
                //val pre0 = this.tk0.pos.pre()
                when {
                    this.acceptFix("var") -> {
                        this.acceptEnu_err("Id")
                        Expr.Dcl(this.tk0 as Tk.Id, true, false, null, true, null)
                    }
                    this.acceptFix("set") -> {
                        this.acceptEnu_err("Id")
                        val dst = Expr.Acc(this.tk0 as Tk.Id)
                        val tag = if (!this.acceptEnu("Tag")) null else this.tk0 as Tk.Tag
                        this.acceptFix_err("=")
                        if (tag == null) {
                            this.checkFix_err("func")
                        }
                        val src = this.expr()
                        when {
                            (tag != null) -> Expr.Set(tk0, dst, tag, src)
                            (src is Expr.Proto) -> this.nest("""
                                TODO
                            """)
                            else -> error("impossible case")
                        }
                    }
                    this.acceptFix("func") -> {
                        this.acceptEnu_err("Id")
                        this.nest("""
                            TODO
                        """)
                    }
                    else -> {
                        err(this.tk1, "poly error : expected var or set")
                        error("unreachable")
                    }
                }
            }
            */

            (XCEU && this.checkFix("\\")) -> this.lambda()
            (XCEU && this.acceptFix("ifs")) -> {
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
                    val (id,tag,cnd) = when {
                        this.acceptFix("else") -> {
                            Triple(null, null, Expr.Bool(Tk.Fix("true",this.tk0.pos)))
                        }
                        XCEU && (v!=null) && this.acceptEnu("Op") -> {
                            val op = this.tk0 as Tk.Op
                            val e = this.expr()
                            Triple(null, null, xop(op, xacc(op.pos,x), e))
                        }
                        else -> {
                            id_tag_cnd()
                        }
                    }
                    val blk = if (this.acceptFix("->")) {
                        Expr.Do(this.tk0, listOf(this.expr()))
                    } else {
                        this.block()
                    }
                    Pair(Triple(id,tag,cnd),blk)
                }
                //ifs.forEach { println(it.first.third.tostr()) ; println(it.second.tostr()) }
                this.acceptFix_err("}")
                this.nest("""
                    ${pre0}do {
                        ${v.cond { "val $x = ${v!!.tostr(true)}" }}
                        ${ifs.map { (xxx,blk) ->
                            val (id,tag,cnd) = xxx
                            """
                             if ${id.cond{ "$it ${tag?.str ?: ""} = "}} ${cnd.tostr(true)} {
                                ${blk.es.tostr(true)}
                             } else {
                            """}.joinToString("")}
                         ${ifs.map { """
                             }
                         """}.joinToString("")}
                    }
                """)
            }
            (XCEU && this.acceptFix("resume-yield-all")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val call = this.expr()
                if (call !is Expr.Call) {
                    err(call.tk, "resume-yield-call error : expected call")
                }
                call as Expr.Call
                val arg = if (call.args.size == 0) {
                    xnil(call.tk.pos)
                } else {
                    call.args[0]
                }
                val nn = N
                fun xco (): Tk.Id {
                    return Tk.Id("ceu_co_$nn", tk0.pos, 0)
                }
                fun xarg (): Tk.Id {
                    return Tk.Id("ceu_arg_$nn", tk0.pos, 0)
                }
                fun xv (): Tk.Id {
                    return Tk.Id("ceu_v_$nn", tk0.pos, 0)
                }
                /*
                    do {
                        val xco = $call.proto
                        var xarg = $arg
                        loop {
                            val xv = resume xco(xarg)
                            if (status(xco) /= :terminated) or (xv /= nil) {
                                set xarg = yield(xv)
                            }
                        } until (status(xco) == :terminated)
                        xarg
                    }
                */
                return Expr.Do(tk0, listOf(
                    Expr.Dcl(Tk.Fix("val",tk0.pos), xco(), false, null, true, call.proto),
                    Expr.Dcl(Tk.Fix("var",tk0.pos), xarg(), false, null, true, arg),
                    Expr.Do(tk0, listOf(
                        Expr.Loop(tk0, nn, Expr.Do(tk0, listOf(
                            Expr.Dcl(Tk.Fix("val",tk0.pos), xv(), false, null, true,
                                Expr.Resume(tk0, Expr.Call(tk0, Expr.Acc(xco()), listOf(Expr.Acc(xarg()))))
                            ),
                            Expr.If(tk0,
                                xor(tk0,
                                    Expr.Call(tk0, xacc(tk0.pos, "{/=}"), listOf(
                                        Expr.Call(tk0, xacc(tk0.pos,"status"), listOf(Expr.Acc(xco()))),
                                        Expr.Tag(Tk.Tag(":terminated", tk0.pos))
                                    )),
                                    Expr.Call(tk0, xacc(tk0.pos,"{/=}"), listOf(
                                        Expr.Acc(xv()),
                                        xnil(tk0.pos)
                                    ))
                                ),
                                Expr.Do(tk0, listOf(Expr.Set(tk0,Expr.Acc(xarg()),Expr.Yield(tk0,Expr.Acc(xv()))))),
                                Expr.Do(tk0, listOf(xnil(tk0.pos)))
                            ),
                            Expr.If(tk0,
                                Expr.Call(tk0, xacc(tk0.pos, "{==}"), listOf(
                                    Expr.Call(tk0, xacc(tk0.pos,"status"), listOf(Expr.Acc(xco()))),
                                    Expr.Tag(Tk.Tag(":terminated", tk0.pos))
                                )),
                                Expr.Do(tk0, listOf(Expr.XBreak(tk0, nn))),
                                Expr.Do(tk0, listOf(xnil(tk0.pos)))
                            )
                        )))
                    )),
                    Expr.Acc(xarg())
                ))
            }
            (XCEU && this.acceptFix("await")) -> {
                val pre0 = this.tk0.pos.pre()
                val awt = await()
                when {
                    (awt.xcnd != null) -> {   // await :key
                        awt.cnd!!
                        val xcnd = awt.xcnd.first ?: awt.xcnd.second!!.tostr(true)
                        this.nest("""
                            ${pre0}export [evt] {
                                val evt ${awt.cnd.tk.str}
                                await (evt is? ${awt.cnd.tk.str}) and $xcnd
                            }
                        """)
                    }
                    awt.spw -> { // await spawn T()
                        val e = this.expr()
                        if (!(e is Expr.Spawn && e.tasks==null)) {
                            err_expected(e.tk, "non-pool spawn")
                        }
                        this.nest("""
                            ${pre0}do {
                                val ceu_spw_$N = ${e.tostr(true)}
                                ${pre0}await :check-now (status(ceu_spw_$N) == :terminated)
                                `ceu_acc = ceu_mem->ceu_spw_$N.Dyn->Bcast.X.frame->X.pub;`
                            }
                        """) //.let { println(it.tostr());it }
                    }
                    (awt.clk != null) -> { // await 5s
                        this.nest("""
                            ${pre0}do {
                                var ceu_ms_$N = ${awt.clk.map { (e,tag) ->
                                    val s = e.tostr(true)
                                    "(" + when (tag.str) {
                                        ":h"   -> "($s * ${1000*60*60})"
                                        ":min" -> "($s * ${1000*60})"
                                        ":s"   -> "($s * ${1000})"
                                        ":ms"  -> "($s * ${1})"
                                        else   -> error("impossible case")
                                    }
                                }.joinToString("+") + (")").repeat(awt.clk.size)}
                                loop until ceu_ms_$N <= 0 {
                                    await (evt is? :frame)
                                    set ceu_ms_$N = ceu_ms_$N - evt.0
                                }
                            }
                        """)//.let { println(it.tostr()); it }
                    }
                    (awt.cnd != null) -> {  // await evt==x | await trk | await coro
                        this.nest("""
                            ${pre0}export [] {
                                ${pre0}${(!awt.now).cond { "yield ()" }}
                                loop {
                                    var ceu_cnd_$N = ${awt.cnd.tostr(true)}
                                    ifs _ = ceu_cnd_$N {
                                        type(ceu_cnd_$N) == :x-task {
                                            set ceu_cnd_$N = (status(ceu_cnd_$N) == :terminated)
                                        }
                                        type(ceu_cnd_$N) == :x-track {
                                            set ceu_cnd_$N = (detrack(ceu_cnd_$N) == nil)
                                        }
                                        else {
                                            ;;set ceu_cnd_$N = ceu_cnd_$N
                                        }
                                    }
                                } until (not (not ceu_cnd_$N)) {
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
                val awt = await()
                val brk = if ((this.acceptFix("until") || this.acceptFix("while"))) this.tk0 else null
                fun untils (): String {
                    return if (!(this.acceptFix("until")||this.acceptFix("while"))) "" else {
                        val brk = this.tk0
                        val (id,tag,cnd) = id_tag_cnd()
                        val xblk = if (!this.checkFix("{")) "" else {
                            "{" + this.block().es.tostr(true) + untils()
                        }
                        """
                        } ${brk.str} ${id.cond { "${id!!} ${tag?.str?:""} = " }} ${cnd.tostr(true)}
                        $xblk
                        """
                    }
                }

                this.nest("""
                    ${pre0}loop {
                        ${awt.tostr()}
                        ${brk.cond {
                            val (id,tag,cnd) = id_tag_cnd()
                            "} ${brk!!.str} ${id.cond { "${id!!} ${tag?.str?:""} = " }} ${cnd.tostr(true)} {" }}
                        ${this.block().es.tostr(true)}
                        ${untils()}
                    }
                """)//.let { println(it); it })
            }
            (XCEU && this.acceptFix("par")) -> {
                val tk0 = this.tk0 as Tk.Fix
                val pars = mutableListOf(this.block())
                this.acceptFix_err("with")
                pars.add(this.block())
                while (this.acceptFix("with")) {
                    pars.add(this.block())
                }
                val spws = pars.map {
                    // spawn { $it }
                    val tk1 = Tk.Fix("task", it.tk.pos)
                    val task = Expr.Proto(tk1, Pair(null,true), emptyList(), it)
                    Expr.Spawn(Tk.Fix("spawn",it.tk.pos), null, Expr.Call(tk0, task, emptyList()))
                }
                //do { $spws ; await false }
                val awt = Expr.Loop(tk0, 0, Expr.Do(tk0, listOf(Expr.Yield(tk0, xnil(tk0.pos)))))
                Expr.Do(Tk.Fix("do",tk0.pos), spws + awt)
            }
            (XCEU && this.acceptFix("par-and")) -> {
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
                        var _ceu_$n
                        ${pars.mapIndexed { i,body -> """
                            val ceu_${i}_$n = spawn {
                                set _ceu_$n = do {
                                    ${body.es.tostr(true)}
                                }
                            }
                        """}.joinToString("")}
                        await :check-now (
                            ${pars.mapIndexed { i,_ -> """
                                ((status(ceu_${i}_$n) == :terminated) and
                            """}.joinToString("")} true ${")".repeat(pars.size)}
                        )
                        _ceu_$n
                    }
                """)
            }
            (XCEU && this.acceptFix("par-or")) -> {
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
                        var _ceu_$n
                        ${pars.mapIndexed { i,body -> """
                            val ceu_${i}_$n = spawn {
                                val _ceu_${i}_$n = do {
                                    ${body.es.tostr(true)}
                                }
                                set _ceu_$n = _ceu_$n or _ceu_${i}_$n 
                            }
                        """}.joinToString("")}
                        await :check-now (
                            ${pars.mapIndexed { i,_ -> """
                                ((status(ceu_${i}_$n) == :terminated) or
                            """}.joinToString("")} false ${")".repeat(pars.size)}
                        )
                        _ceu_$n
                    }
                """)
            }
            (XCEU && this.acceptFix("awaiting")) -> {
                val pre0 = this.tk0.pos.pre()
                val awt = await()
                val body = this.block()
                this.nest("""
                    ${pre0}par-or {
                        ${awt.tostr()}
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
                //this.acceptFix("#") -> ops.add(this.tk0)
                this.acceptEnu("Op") -> ops.add(this.tk0)
                //(XCEU && this.acceptFix("not")) -> ops.add(this.tk0)
                else -> break
            }
        }
        var e = this.exprSufs()
        while (ops.size > 0) {
            val op = ops.removeLast()
            if (XCEU && op.str == "not") {
                op as Tk.Op
                // if $e { false } else { true }
                e = Expr.If(Tk.Fix("if",op.pos), e,
                    Expr.Do(op, listOf(Expr.Bool(Tk.Fix("false",op.pos)))),
                    Expr.Do(op, listOf(Expr.Bool(Tk.Fix("true",op.pos))))
                )
            } else {
                e = Expr.Call(op, xacc(op.pos,"{${op.str}}"), listOf(e))
            }
        }
        return e
    }

    fun exprSufs (): Expr {
        return this.exprSufsX(this.exprPrim())
    }
    fun exprSufsX (ex: Expr): Expr {
        var e = ex
        while (true) {
            // only accept simple sufix in the same line
            val same = this.tk0.pos.isSameLine(this.tk1.pos)
            val cplx = this.checkFix("where") || this.checkFix("thus")
            if (!(same || cplx)) {
                break
            }

            when {
                // INDEX / PUB / FIELD
                this.acceptFix("[") -> {
                    e = if (XCEU && (this.acceptOp("+") || this.acceptOp("-") || this.acceptFix("="))) {
                        val op = this.tk0
                        val isclose = if (op.str=="-") this.acceptFix_err("]") else this.acceptFix("]")
                        if (isclose) {
                            when (op.str) {
                                "=" -> {
                                    // export [] { val :tmp x=$e ; x[#x-1] }
                                    val id = Tk.Id("ceu_col_$N", op.pos, 0)
                                    Expr.Export(Tk.Fix("export",op.pos), emptyList(), Expr.Do(op, listOf(
                                        Expr.Dcl(Tk.Fix("val",op.pos), id, true, null, true, e),
                                        Expr.Index(op, Expr.Acc(id),
                                            Expr.Call(op, xacc(op.pos,"{-}"), listOf(
                                                Expr.Call(op, xacc(op.pos,"{#}"), listOf(Expr.Acc(id))),
                                                xnum(op.pos,1)
                                            ))
                                    ))))
                                }
                                "+" -> {
                                    // export [] { val :tmp x=$e ; x[#x] }
                                    val id = Tk.Id("ceu_col_$N", op.pos, 0)
                                    Expr.Export(Tk.Fix("export",op.pos), emptyList(), Expr.Do(op, listOf(
                                        Expr.Dcl(Tk.Fix("val",op.pos), id, true, null, true, e),
                                        Expr.Index(op, Expr.Acc(id),
                                            Expr.Call(op, xacc(op.pos,"{#}"), listOf(Expr.Acc(id))),
                                        )
                                    )))
                                } //.let { println(it.tostr());it }
                                "-" -> {
                                    // export [] { val :tmp x=$e ; val y=x[#x-1] ; set x[#x-1]=nil ; y }
                                    val id_col = Tk.Id("ceu_col_$N", op.pos, 0)
                                    val id_val = Tk.Id("ceu_val_$N", op.pos, 0)
                                    fun idx (): Expr.Index {
                                        return Expr.Index(op, Expr.Acc(id_col),
                                            Expr.Call(op, xacc(op.pos,"{-}"), listOf(
                                                Expr.Call(op, xacc(op.pos,"{#}"), listOf(Expr.Acc(id_col))),
                                                xnum(op.pos, 1)
                                            )))
                                    }
                                    Expr.Export(Tk.Fix("export",op.pos), emptyList(), Expr.Do(op, listOf(
                                        Expr.Dcl(Tk.Fix("val",op.pos), id_col, true, null, true, e),
                                        Expr.Dcl(Tk.Fix("val",op.pos), id_val, false, null, true, idx()),
                                        Expr.Set(Tk.Fix("set",op.pos), idx(), xnil(op.pos)),
                                        Expr.Acc(id_val)
                                    )))
                                }
                                else -> error("impossible case")
                            }
                        } else {
                            val idx = this.expr()
                            this.acceptFix_err("]")
                            Expr.Index(e.tk, e,
                                Expr.Call(op, xacc(op.pos,"{${op.str}}"), listOf(idx))
                            ) //.let { println(it.tostr());it }
                        }
                    } else {
                        val idx = this.expr()
                        this.acceptFix_err("]")
                        Expr.Index(e.tk, e, idx)
                    }
                }
                this.acceptFix(".") -> {
                    e = when {
                        this.acceptFix("pub") -> Expr.Pub(this.tk0 as Tk.Fix, e)
                        this.acceptEnu("Id") -> Expr.Index(e.tk, e, Expr.Tag(Tk.Tag(':'+this.tk0.str,this.tk0.pos)))
                        (XCEU && this.acceptEnu("Num")) -> {
                            val num = this.tk0 as Tk.Num
                            if (num.str.contains('.')) {
                                err(num, "index error : ambiguous dot : use brackets")
                            }
                            Expr.Index(e.tk, e, Expr.Num(num))
                        }
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
                    e = Expr.Call(e.tk, e, list0(")",",") {
                        val x = this.expr()
                        if (x is Expr.Acc && x.tk.str=="...") {
                            this.checkFix_err(")")
                        }
                        x
                    })
                    this.acceptFix_err(")")
                }
                // LAMBDA
                XCEU && this.checkFix("\\") -> {
                    // e = e($lambda)
                    e = Expr.Call(e.tk, e, listOf(this.lambda()))
                }
                // WHERE
                XCEU && this.acceptFix("where") -> {
                    // e = export [] { blk, e }
                    val tk0 = this.tk0 as Tk.Fix
                    val blk = this.block()
                    e = Expr.Export(tk0, emptyList(), Expr.Do(tk0, blk.es+e))
                }
                // THUS
                XCEU && this.acceptFix("thus") -> {
                    val tk0 = this.tk0
                    val x = if (!this.acceptEnu("Id")) null else this.tk0 as Tk.Id
                    val body = this.block()
                    // do { val $x = $e }
                    e = Expr.Do(Tk.Fix("do", tk0.pos),
                    listOf(
                            Expr.Dcl(Tk.Fix("val",tk0.pos), x ?: Tk.Id("it",tk0.pos,0), false, null, true, e)
                        ) + body.es
                    )
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
            this.acceptEnu("Op")
        ) {
            val op = this.tk0 as Tk.Op
            if (pre==null || pre.str==")" || this.tk1.str==")") {} else {
                err(op, "binary operation error : expected surrounding parentheses")
            }
            val e2 = this.exprPres()
            e = xop(op, e, e2)
            pre = this.tk0
        }
        return e
    }
    fun expr (): Expr {
        return this.exprBins()
    }

    fun exprs (): List<Expr> {
        val ret = mutableListOf<Expr>()
        while (!this.checkFix("}") && !this.checkEnu("Eof")) {
            ret.add(this.expr())
        }
        if (ret.size == 0) {
            if (XCEU) {
                ret.add(Expr.Pass(Tk.Fix("pass", tk0.pos.copy()), xnil(this.tk0.pos)))
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
