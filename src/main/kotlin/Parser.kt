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

    fun block (tk0: Tk? = null, tag: Tk.Tag? = null): Expr.Do {
        val tk = when {
            (tk0 != null) -> tk0
            (this.tk0.str=="do") -> this.tk0
            else -> this.tk1
        }
        this.acceptFix_err("{")
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Do(tk, tag, es)
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

    fun exprPrim (): Expr {
        return when {
            this.acceptFix("do") -> {
                val tk0 = this.tk0
                val tag = if (this.acceptTag(":unnest") || this.acceptTag(":unnest-hide")) {
                    this.tk0 as Tk.Tag
                } else {
                    null
                }
                val blk = this.block(tag)
                Expr.Do(tk0, tag, blk.es)
            }
            this.acceptFix("val") || this.acceptFix("var") -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix("evt") || this.acceptEnu_err("Id")
                val id = this.tk0.let { if (it is Tk.Id) it else Tk.Id("evt",it.pos,0) }
                if (id.str == "...") {
                    err(this.tk0, "invalid declaration : unexpected ...")
                }
                val tmp = this.acceptTag(":tmp")
                val tag = if (!this.acceptEnu("Tag")) null else {
                    this.tk0 as Tk.Tag
                }
                val src = if (!this.acceptFix("=")) null else {
                    this.expr()
                }
                Expr.Dcl(tk0, id, /*false,*/ tmp, tag, true, src)
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0 as Tk.Fix
                val dst = this.expr()
                if (dst is Expr.Acc && dst.tk.str == "...") {
                    err(this.tk0, "invalid set : unexpected ...")
                }
                this.acceptFix_err("=")
                val src = this.expr()
                if (!XCEU || !(dst is Expr.Do && (dst.tag != null))) {
                    if (!(dst is Expr.Acc || dst is Expr.Index || (dst is Expr.Pub && dst.tk.str=="pub"))) {
                        err(tk0, "invalid set : invalid destination")
                    }
                    Expr.Set(tk0, dst, /*null,*/ src)
                } else {
                    val hack = dst.tostr(false).let {
                        val s3 = it.substringAfterLast("\n}")
                        val aa = it.substringBeforeLast("\n}")
                        val s2 = aa.substringAfterLast("\n")
                        val s1 = aa.substringBeforeLast("\n")
                        s1 + "\n" + "set " + s2 + " = " + src.tostr(false) + "\n}\n"+ s3
                    }
                    this.nest(hack)
                }
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
                        Expr.Do(tk0, null, listOf(Expr.Pass(Tk.Fix("pass", tk0.pos.copy()), Expr.Nil(Tk.Fix("nil", tk0.pos.copy())))))
                    }
                }
                Expr.If(tk0, cnd, t, f)
            }
            this.acceptFix("loop") -> {
                val tk0 = this.tk0 as Tk.Fix
                val pre0 = tk0.pos.pre()
                val raw = this.acceptEnu("Num")
                val xin = !raw && this.acceptFix("in")
                val nn = if (raw) this.tk0.str.toInt() else N++

                val f = when {
                    raw -> { { blk -> error("never called") } }
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
                        this.acceptFix_err(",")
                        this.acceptEnu_err("Id")
                        val i = this.tk0 as Tk.Id
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
                                        `goto CEU_LOOP_DONE_$nn;`
                                    } else { nil }
                                    val ceu_dyn_$N = `:pointer ceu_mem->ceu_tasks_$N.Dyn->Bcast.Tasks.dyns.buf[(int)ceu_mem->ceu_i_$N.Number]`
                                    if ceu_dyn_$N == `:pointer NULL` {
                                        ;; empty slot
                                        set ceu_i_$N = `:number ceu_mem->ceu_i_$N.Number + 1` ;; just to avoid prelude
                                    } else {
                                        ;;;
                                        val ceu_x_$N
                                        `ceu_mem->ceu_x_$N = (CEU_Value) { CEU_VALUE_X_TASK, {.Dyn=ceu_mem->ceu_dyn_$N.Pointer} };`
                                        val ${i.str} = track(ceu_x_$N)
                                        ;;;

                                        ```
                                            CEU_Value ceu_x_$N = { CEU_VALUE_X_TASK, {.Dyn=ceu_mem->ceu_dyn_$N.Pointer} };
                                        ```
                                        val ${i.str} = track(`:ceu ceu_x_$N`)
                                        $body
                                        if detrack(${i.str}) {
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

                        // , i
                        x = (step==null && x) || this.acceptFix(",")
                        val i = if (x && this.acceptEnu_err("Id")) this.tk0.str else "ceu_i_$N"

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
                                var $i = ${eA.tostr(true)} $op (
                                    ${if (tkA.str=="[") 0 else "ceu_step_$N"}
                                )
                                val ceu_limit_$N = ${eB.tostr(true)}
                                loop $nn {
                                    if $i $cmp ceu_limit_$N {
                                        pass nil     ;; return value
                                        `goto CEU_LOOP_DONE_$nn;`
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
                        this.acceptFix_err(",")
                        this.acceptEnu_err("Id")
                        val i = this.tk0 as Tk.Id
                        { body: String -> """
                            ${pre0}do {
                                val ceu_it_$N :Iterator = ${iter.tostr(true)}
                                ;;assert(ceu_it_$N is? :Iterator, "expected :Iterator")
                                loop $nn {
                                    val ${i.str} = ceu_it_$N.f(ceu_it_$N)
                                    if ${i.str} == nil {
                                        pass nil     ;; return value
                                        `goto CEU_LOOP_DONE_$nn;`
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

                val brk = if (raw || !this.acceptFix("until")) "" else {
                    val cnd = this.expr().tostr(true)
                    """
                    if $cnd {
                        `goto CEU_LOOP_DONE_$nn;`
                    } else { nil }
                    """
                }

                val blk = this.block().es

                fun untils (): String {
                    return if (!this.acceptFix("until")) "" else {
                        val cnd = this.expr().tostr(true)
                        val xblk = if (!this.checkFix("{")) "" else {
                            this.block().es.tostr(true) + untils()
                        }
                        """
                        if $cnd {
                            `goto CEU_LOOP_DONE_$nn;`
                        } else { nil }
                        $xblk
                        """
                    }
                }

                if (raw) {
                    Expr.Loop(tk0, nn, Expr.Do(tk0, null, blk))
                } else {
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
            }
            this.acceptFix("func") || this.acceptFix("coro") || this.acceptFix("task") -> {
                val tk0 = this.tk0 as Tk.Fix
                val isnote = (tk0.str=="func" || this.checkFix("(") || this.checkEnu("Tag") || this.checkFix("(") || (XCEU && this.checkEnu("Id")))
                if (isnote) {
                    val id = if (XCEU && this.acceptEnu("Id")) this.tk0 as Tk.Id else null
                    this.acceptFix_err("(")
                    val args = this.list0(")") {
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
                    val task = when {
                        (tk0.str != "task") -> null
                        this.acceptTag(":fake") -> Pair(null, true)
                        this.acceptEnu("Tag") -> Pair(this.tk0 as Tk.Tag, false)
                        else -> Pair(null, false)
                    }
                    val blk = this.block(this.tk1)
                    val proto = Expr.Proto(tk0, task, args, blk)
                    if (id == null) proto else {
                        this.nest("""
                            ${tk0.pos.pre()}val ${id.str} = ${proto.tostr(true)} 
                        """)
                    }
                } else {
                    Expr.Self(tk0)
                }
            }
            this.acceptFix("catch") -> {
                val cnd = this.expr()
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
                val tags = this.list0("}") {
                    this.acceptEnu_err("Tag")
                    val tag = this.tk0 as Tk.Tag
                    val nat = if (!this.acceptFix("=")) null else {
                        this.acceptEnu_err("Nat")
                        this.tk0 as Tk.Nat
                    }
                    Pair(tag, nat)
                }
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
                        val ids = this.list0("]") {
                            this.acceptEnu_err("Id")
                            val id = this.tk0 as Tk.Id
                            val tp = if (!this.acceptEnu("Tag")) null else {
                                this.tk0 as Tk.Tag
                            }
                            Pair(id, tp)
                        }
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
                    Expr.Do(Tk.Fix("do",tpl.pos), null, l)
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
                        if (call !is Expr.Call && !((call is Expr.Do && (call.tag!=null)) && call.es.last() is Expr.Call)) {
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
                        if (call !is Expr.Call && !((call is Expr.Do && (call.tag!=null)) && call.es.last() is Expr.Call)) {
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
                if (XCEU && this.checkFix("[")) {
                    val tup = this.exprPrim()
                    assert(tup is Expr.Tuple)
                    this.nest("""
                        ${tag.pos.pre()}tags(${tup.tostr(true)}, ${tag.str}, true)
                    """)
                } else {
                    Expr.Tag(tag)
                }
            }
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
                        val ceu_ifs_${cnd!!.n} = ${cnd.tostr(true)}
                """ }

                val eq1 = (cnd!=null && (this.acceptOp("==") || this.acceptOp("in?") || this.acceptOp("is?") || this.acceptOp("is-not?")))
                val eq1_op = this.tk0.str
                val e1 = this.expr().let { if (!eq1) it.tostr(true) else "(ceu_ifs_${cnd!!.n} $eq1_op ${it.tostr(true)})" }
                this.acceptFix_err("->")
                val b1 = if (this.checkFix("{")) this.block() else Expr.Do(this.tk0, null, listOf(this.expr()))
                ifs += """
                    ${pre0}if $e1 ${b1.tostr(true)} else {
                """
                var n = 1
                while (!this.acceptFix("}")) {
                    if (this.acceptFix("else")) {
                        this.acceptFix_err("->")
                        val be = if (this.checkFix("{")) this.block() else Expr.Do(this.tk0, null, listOf(this.expr()))
                        ifs += be.es.map { it.tostr(true)+"\n" }.joinToString("")
                        this.acceptFix("}")
                        break
                    }
                    val pre1 = this.tk0.pos.pre()
                    val eqi = (cnd!=null && (this.acceptOp("==") || this.acceptOp("in?") || this.acceptOp("is?") || this.acceptOp("is-not?")))
                    val eqi_op = this.tk0.str
                    val ei = this.expr().let { if (!eqi) it.tostr(true) else "(ceu_ifs_${cnd!!.n} $eqi_op ${it.tostr(true)})" }
                    this.acceptFix_err("->")
                    val bi = if (this.checkFix("{")) this.block() else Expr.Do(this.tk0, null, listOf(this.expr()))
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
                    (awt.xcnd != null) -> {   // await :key
                        awt.cnd!!
                        val xcnd = awt.xcnd.first ?: awt.xcnd.second!!.tostr(true)
                        this.nest("""
                            ${pre0}do :unnest {
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
                            ${pre0}do :unnest {
                                ${pre0}${(!awt.now).cond { "yield ()" }}
                                loop {
                                    var ceu_cnd_$N = ${awt.cnd.tostr(true)}
                                    ifs {
                                        type(ceu_cnd_$N) == :x-task -> {
                                            set ceu_cnd_$N = (status(ceu_cnd_$N) == :terminated)
                                        }
                                        type(ceu_cnd_$N) == :x-track -> {
                                            set ceu_cnd_$N = (detrack(ceu_cnd_$N) == nil)
                                        }
                                        else -> {
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
                val body = this.block()
                this.nest("""
                    ${pre0}loop {
                        ${awt.tostr()}
                        ${body.es.tostr(true)}
                    }
                """)//.let { println(it.tostr()); it }
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
                e = this.nest("${op.pos.pre()}if ${e.tostr(true)} { false } else { true }\n")
            } else {
                e = Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos,0)), listOf(e))
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
                    e = if (XCEU && (this.acceptOp("+") || this.acceptOp("-") || this.acceptFix("="))) {
                        val op = this.tk0
                        val isclose = if (op.str=="-") this.acceptFix_err("]") else this.acceptFix("]")
                        if (isclose) {
                            when (op.str) {
                                "=" -> this.nest("""
                                    do :unnest-hide { 
                                        val ceu_col_$N :tmp = ${e.tostr(true)}
                                        ceu_col_$N[(#ceu_col_$N)-1]
                                    }
                                """)
                                "+" -> this.nest("""
                                    do :unnest-hide { 
                                        val ceu_col_$N :tmp = ${e.tostr(true)}
                                        ceu_col_$N[#ceu_col_$N]
                                    }
                                """) //.let { println(it.tostr());it }
                                "-" -> this.nest("""
                                    do :unnest-hide { 
                                        val ceu_col_$N :tmp = ${e.tostr(true)}
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
                    e = Expr.Call(e.tk, e, list0(")") {
                        val x = this.expr()
                        if (x is Expr.Acc && x.tk.str=="...") {
                            this.checkFix_err(")")
                        }
                        x
                    })
                }
                // LAMBDA
                XCEU && this.acceptFix("\\") -> {
                    val blk = this.block()
                    e = this.nest("""
                        ${e.tostr(true)}(func (it) {
                            ${blk.es.tostr(true)}
                        })
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
                        val ceu_${e.n} :tmp = ${e.tostr(true)} 
                        if ceu_${e.n} { ceu_${e.n} } else { ${e2.tostr(true)} }
                    }
                """)
                "and" -> this.nest("""
                    ${op.pos.pre()}do {
                        val ceu_${e.n} :tmp = ${e.tostr(true)} 
                        if ceu_${e.n} { ${e2.tostr(true)} } else { ceu_${e.n} }
                    }
                """)
                "is?" -> this.nest("is'(${e.tostr(true)}, ${e2.tostr(true)})")
                "is-not?" -> this.nest("is-not'(${e.tostr(true)}, ${e2.tostr(true)})")
                "in?" -> this.nest("in'(${e.tostr(true)}, ${e2.tostr(true)})")
                else -> Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos,0)), listOf(e,e2))
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
                    ${tk0.pos.pre()}do :unnest-hide {
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
                ret.add(Expr.Pass(Tk.Fix("pass", tk0.pos.copy()), Expr.Nil(Tk.Fix("nil", this.tk0.pos.copy()))))
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
