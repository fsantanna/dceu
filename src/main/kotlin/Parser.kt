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
                Expr.Nil(Tk.Fix("nil", this.tk0.pos))
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
        val args = if (!this.checkEnu("Id")) {
            listOf(Pair(Tk.Id("it",this.tk0.pos,0),null))
        } else {
            this.args("{")
        }.let {
            it.map { it.first.str + (it.second?.str?:"") }.joinToString(",")
        }
        val blk = this.block()
        return this.nest("""
            (func ($args) {
                ${blk.es.tostr(true)}
            })
        """) as Expr.Proto
    }

    fun await (): Await {
        val now = this.acceptTag(":check-now")
        val isspw = this.checkFix("spawn")
        val e = this.expr()
        fun isclk (): Boolean {
            return listOf(":h",":min",":s",":ms").any { this.acceptTag(it) }
        }
        return when {
            isspw -> {
                if (!(e is Expr.Spawn && e.tasks == null)) {
                    err_expected(e.tk, "non-pool spawn")
                }
                Await(now, null, null, null, e as Expr.Spawn)
            }
            (e is Expr.Tag) -> {
                val cnd = if (this.acceptFix(",")) this.expr() else null
                Await(now, null, Pair(Expr.Tag(e.tk as Tk.Tag), cnd), null, null)
            }
            isclk() -> {
                val es = mutableListOf(Pair(e, this.tk0 as Tk.Tag))
                while (isclk()) {
                    es.add(Pair(this.expr(), this.tk0 as Tk.Tag))
                }
                Await(now, null, null, es, null)
            }
            else -> Await(now, e, null, null, null)
        }
    }
    fun Await.tostr (): String {
        val now = this.now.cond { ":check-now" }
        return when {
            (this.cnd != null) -> "await $now ${this.cnd.tostr()}"
            (this.tag != null) -> "await $now ${this.tag.first.tk.str} ${this.tag.second.cond { ", ${it.tostr()}" }}"
            (this.clk != null) -> "await $now ${this.clk.map { (e,t) -> e.tostr(true)+t.str }.joinToString(" ")}"
            (this.spw != null) -> "await $now ${this.spw.tostr()}"
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
                        Expr.Do(tk0, listOf(Expr.Nil(Tk.Fix("nil", tk0.pos.copy()))))
                    }
                }
                if (id == null) {
                    Expr.If(tk0, cnd, t, f)
                } else {
                    this.nest("""
                        ${tk0.pos.pre()}export {
                            val ceu_$N ${tag ?: ""} = ${cnd.tostr(true)}
                            if ceu_$N {
                                val $id ${tag ?: ""} = ceu_$N
                                ${t.es.tostr(true)}
                            } else {
                                ${f.es.tostr(true)}
                            }
                        }
                    """)
                }
            }
            this.acceptFix("xbreak") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptEnu_err("Num")
                Expr.XBreak(tk0, this.tk0.str.toInt())
            }
            this.acceptFix("loop") -> {
                val tk0 = this.tk0 as Tk.Fix

                if (this.acceptEnu("Num")) {
                    val nn = this.tk0.str.toInt()
                    val blk = this.block()
                    return Expr.Loop(tk0, nn, Expr.Do(tk0, blk.es))
                }

                val pre0 = tk0.pos.pre()
                val xin = this.acceptFix("in")
                val nn = N++
                val f = when {
                    !xin -> {
                        { body -> """
                            do {
                                loop $nn {
                                    $body
                                }
                            }
                        """ }
                    }
                    this.acceptTag(":tasks") -> {
                        val tasks = this.expr()
                        val i = if (XCEU && !this.checkFix(",")) "it" else {
                            this.acceptFix_err(",")
                            this.acceptEnu_err("Id")
                            this.tk0.str
                        }
                        { body: String -> """
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
                                        xbreak $nn
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
                                        $body
                                        if detrack($i) {
                                            set ceu_i_$N = `:number ceu_mem->ceu_i_$N.Number + 1` ;; just to avoid prelude
                                        } else {
                                            set ceu_i_$N = ceu_n_$N
                                        }
                                    }
                                }
                            }
                            """
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

                        { body: String ->"""
                            ${pre0}do {
                                val ceu_step_$N = ${if (step==null) 1 else step.tostr(true) }
                                var $i ${tag?.str ?: ""} = ${eA.tostr(true)} $op (
                                    ${if (tkA.str=="[") 0 else "ceu_step_$N"}
                                )
                                val ceu_limit_$N = ${eB.tostr(true)}
                                loop $nn {
                                    if $i $cmp ceu_limit_$N {
                                        pass nil     ;; return value
                                        xbreak $nn
                                    } else { nil }
                                    $body
                                    set $i = $i $op ceu_step_$N
                                }                                
                            }
                            """
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
                        { body: String -> """
                            ${pre0}do {
                                val ceu_it_$N :Iterator = ${iter.tostr(true)}
                                ;;assert(ceu_it_$N is? :Iterator, "expected :Iterator")
                                loop $nn {
                                    val $i ${tag?.str ?: ""} = ceu_it_$N.f(ceu_it_$N)
                                    if $i == nil {
                                        pass nil     ;; return value
                                        xbreak $nn
                                    } else { nil }
                                    $body
                                }
                            }
                            """
                        }
                    }
                    else -> {
                        err(this.tk1, "invalid loop : unexpected ${this.tk1.str}")
                        error("unreachable")
                    }
                }

                val brk = if (!(this.acceptFix("until")||XCEU && this.acceptFix("while"))) "" else {
                    val not = if (this.tk0.str == "until") "" else "not"
                    val (id,tag,cnd) = id_tag_cnd()
                    N++
                    """
                    val ${id ?: "ceu_$N"} ${tag ?: ""} = ${cnd.tostr(true)}
                    if $not ${id ?: "ceu_$N"} {
                        xbreak $nn
                    } else { nil }
                    """
                }

                val blk = this.block().es

                fun untils (): String {
                    return if (!(this.acceptFix("until")||this.acceptFix("while"))) "" else {
                        val not = if (this.tk0.str == "until") "" else "not"
                        val (id,tag,cnd) = id_tag_cnd()
                        val xblk = if (!this.checkFix("{")) "" else {
                            this.block().es.tostr(true) + untils()
                        }
                        N++
                        """
                        val ${id ?: "ceu_$N"} ${tag ?: ""} = ${cnd.tostr(true)}
                        if $not ${id ?: "ceu_$N"} {
                            xbreak $nn
                        } else { nil }
                        $xblk
                        """
                    }
                }

                val body = """
                    ;; brk
                    $brk
                    ;; blk
                    ${blk.tostr(true)}
                    ;; untils
                    ${untils()}
                """
                this.nest(f(body))
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
                        !isrec -> this.nest("""
                            ${tk0.pos.pre()}val ${id.str} = ${proto.tostr(true)}
                        """)
                        else -> this.nest("""
                            export [${id.str}] {
                                ${tk0.pos.pre()}var ${id.str}
                                set ${id.str} = ${proto.tostr(true)}
                            }
                        """)
                    }
                }
            }
            this.acceptFix("catch") -> {
                val cnd = if (XCEU && this.checkFix("{")) Expr.Bool(Tk.Fix("true",this.tk0.pos)) else this.expr()
                val blk = this.block()
                if (XCEU && (cnd is Expr.Tag)) {   // catch :err
                    this.nest("catch (err is? ${cnd.tostr(true)}) ${blk.tostr(true)}")
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
                        this.checkFix_err("{")
                        this.nest("""
                            ${tk0.pos.pre()}spawn (coro () {
                                ${this.block().es.tostr(true)}
                            }) ()
                        """)
                    }
                    (XCEU && this.checkFix("{")) -> {
                        this.nest("""
                            ${tk0.pos.pre()}spawn (task () :fake {
                                ${this.block().es.tostr(true)}
                            }) ()
                        """)
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
                        this.nest("""
                            ${tag.pos.pre()}tags(${e.tostr(true)}, ${tag.str}, true)
                        """)
                    } else {
                        val e1 = this.exprSufsX(Expr.Acc(Tk.Id("ceu_$nn",tag.pos,0)))
                        val e2 = this.nest("""
                            ${tag.pos.pre()}do {
                                val :tmp ceu_$nn ${tag.str} = ${e.tostr(true)}
                                ${e1.tostr(true)}
                            }
                        """)
                        e2
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
                            val op = this.tk0.str
                            val e = this.expr()
                            Triple(null, null, this.nest("$x $op ${e.tostr(true)}"))
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
                             if ${id.cond{ "$it ${tag ?: ""} = "}} ${cnd.tostr(true)} {
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
                val call = this.expr()
                if (call !is Expr.Call) {
                    err(call.tk, "resume-yield-call error : expected call")
                }
                call as Expr.Call
                val arg = if (call.args.size == 0) {
                    Expr.Nil(Tk.Fix("nil", call.tk.pos.copy()))
                } else {
                    call.args[0]
                }
                this.nest("""
                    do {
                        val ceu_co_$N  = ${call.proto.tostr(true)}
                        var ceu_arg_$N = ${arg.tostr(true)}
                        loop {
                            ;;println(:resume, ceu_arg_$N)
                            val ceu_v_$N = resume ceu_co_$N(ceu_arg_$N)
                            ;;println(:yield, ceu_v_$N)
                            if (status(ceu_co_$N) /= :terminated) or (ceu_v_$N /= nil) {
                                set ceu_arg_$N = yield(ceu_v_$N)
                                ;;println(:loop, ceu_arg_$N)
                            }
                        } until (status(ceu_co_$N) == :terminated) ;; or (ceu_v_$N == nil)
                        ceu_arg_$N
                    }
                """)
            }
            (XCEU && this.acceptFix("await")) -> {
                val pre0 = this.tk0.pos.pre()
                val awt = await()
                when {
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
                    (awt.tag != null) -> {   // await :key
                        this.nest("""
                            ${pre0}export [evt] {
                                val evt ${awt.tag.first.tk.str}
                                await (evt is? ${awt.tag.first.tk.str}) and ${awt.tag.second?.tostr(true) ?: "true"}
                            }
                        """)
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
                    (awt.spw != null) -> { // await spawn T()
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
                        } ${brk.str} ${id.cond { "${id!!} ${tag?:""} = " }} ${cnd.tostr(true)}
                        $xblk
                        """
                    }
                }

                this.nest("""
                    ${pre0}loop {
                        ${awt.tostr()}
                        ${brk.cond {
                            val (id,tag,cnd) = id_tag_cnd()
                            "} ${brk!!.str} ${id.cond { "${id!!} ${tag?:""} = " }} ${cnd.tostr(true)} {" }}
                        ${this.block().es.tostr(true)}
                        ${untils()}
                    }
                """)//.let { println(it); it })
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
                val n = N
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
                e = this.nest("${op.pos.pre()}if ${e.tostr(true)} { false } else { true }\n")
            } else {
                e = Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos,0)), listOf(e))
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
                                "=" -> this.nest("""
                                    export [] { 
                                        val :tmp ceu_col_$N = ${e.tostr(true)}
                                        ceu_col_$N[(#ceu_col_$N)-1]
                                    }
                                """)
                                "+" -> this.nest("""
                                    export [] { 
                                        val :tmp ceu_col_$N = ${e.tostr(true)}
                                        ceu_col_$N[#ceu_col_$N]
                                    }
                                """) //.let { println(it.tostr());it }
                                "-" -> this.nest("""
                                    export [] { 
                                        val :tmp ceu_col_$N = ${e.tostr(true)}
                                        val ceu_i_$N = ceu_col_$N[(#ceu_col_$N)-1]
                                        set ceu_col_$N[(#ceu_col_$N)-1] = nil
                                        ceu_i_$N
                                    }
                                """)
                                else -> error("impossible case")
                            }
                        } else {
                            val idx = this.expr()
                            this.acceptFix_err("]")
                            Expr.Index(e.tk, e,
                                Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos,0)), listOf(idx))
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
                    val f = this.lambda()
                    e = this.nest("""
                        ${e.tostr(true)}(${f.tostr(true)})
                    """)
                }
                // WHERE
                XCEU && this.acceptFix("where") -> {
                    val tk0 = this.tk0
                    val body = this.block()
                    e = this.nest("""
                        ${tk0.pos.pre()}export [] {
                            ${body.es.tostr(true)}
                            ${e.tostr(true)}
                        }
                    """)
                }
                // THUS
                XCEU && this.acceptFix("thus") -> {
                    val tk0 = this.tk0
                    val x = if (!this.acceptEnu("Id")) null else this.tk0 as Tk.Id
                    val body = this.block()
                    e = this.nest("""
                        ${tk0.pos.pre()}do {
                            val ${x?.str ?: "it"} = ${e.tostr(true)}
                            ${body.es.tostr(true)}
                        }
                    """)
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
            val op = this.tk0
            if (pre==null || pre.str==")" || this.tk1.str==")") {} else {
                err(op, "binary operation error : expected surrounding parentheses")
            }
            val e2 = this.exprPres()
            e = when (op.str) {
                "or" -> this.nest("""
                    ${op.pos.pre()}do {
                        val :tmp ceu_${e.n} = ${e.tostr(true)} 
                        if ceu_${e.n} { ceu_${e.n} } else { ${e2.tostr(true)} }
                    }
                """)
                "and" -> this.nest("""
                    ${op.pos.pre()}do {
                        val :tmp ceu_${e.n} = ${e.tostr(true)} 
                        if ceu_${e.n} { ${e2.tostr(true)} } else { ceu_${e.n} }
                    }
                """)
                "is?" -> this.nest("is'(${e.tostr(true)}, ${e2.tostr(true)})")
                "is-not?" -> this.nest("is-not'(${e.tostr(true)}, ${e2.tostr(true)})")
                "in?" -> this.nest("in'(${e.tostr(true)}, ${e2.tostr(true)})")
                "in-not?" -> this.nest("in-not'(${e.tostr(true)}, ${e2.tostr(true)})")
                else -> Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos,0)), listOf(e,e2))
            }
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
