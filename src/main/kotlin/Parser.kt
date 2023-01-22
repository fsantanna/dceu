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

    fun block (): Expr.Do {
        val tk0 = if (this.tk0.str=="do") this.tk0 else this.tk1
        this.acceptFix_err("{")
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Do(tk0, true, true, es)
    }

    fun catch_block (tk0: Tk, isnest:Boolean=true, ishide:Boolean=true): Pair<(Expr)->Expr,Expr.Do> {
        this.acceptFix_err("{")
        val cnd = if (!XCEU || !this.acceptFix("{")) null else {
            val cnd = this.expr()
            this.acceptFix_err("}")
            cnd
        }
        val es = this.exprs()
        this.acceptFix_err("}")
        val blk = Expr.Do(tk0, isnest, ishide, es)
        if (cnd == null) {
            return Pair({it}, blk)
        } else {
            val catch: (Expr)->Expr = {
                val xcnd = this.nest("(err == ${cnd.tostr(true)})")
                Expr.Catch(tk0 as Tk.Fix, xcnd, if (it is Expr.Do) it else Expr.Do(tk0, isnest, ishide, listOf(it)))
            }
            return Pair(catch, blk)
        }
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
                val unnest = this.acceptTag(":unnest")
                val hide = this.acceptTag(":hide")
                val (isnest,ishide) = when {
                    (unnest && hide) -> Pair(false, true)
                    unnest -> Pair(false, false)
                    else -> {
                        assert(!hide)
                        Pair(true, true)
                    }
                }
                this.catch_block(tk0,isnest,ishide).let { (C,b)->C(b) }
            }
            this.acceptFix("var") -> {
                this.acceptFix("evt") || this.acceptEnu_err("Id")
                val id = this.tk0.let { if (it is Tk.Id) it else Tk.Id("evt",it.pos,0) }
                val tmp = this.acceptTag(":tmp")
                val tag = if (!this.acceptEnu("Tag")) null else {
                    this.tk0 as Tk.Tag
                }
                val src = if (!this.acceptFix("=")) null else {
                    this.expr()
                }
                Expr.Dcl(id, tmp, tag, true, src)
            }
            this.acceptFix("set") -> {
                val tk0 = this.tk0 as Tk.Fix
                val dst = this.expr()
                this.acceptFix_err("=")
                val src = this.expr()
                if (!XCEU || !(dst is Expr.Do && !dst.isnest)) {
                    if (!(dst is Expr.Acc || dst is Expr.Index || (dst is Expr.Pub && dst.tk.str=="pub"))) {
                        err(tk0, "invalid set : invalid destination")
                    }
                    Expr.Set(tk0, dst, src)
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
                        Expr.Do(tk0, true, true, listOf(Expr.Pass(Tk.Fix("pass", tk0.pos.copy()), Expr.Nil(Tk.Fix("nil", tk0.pos.copy())))))
                    }
                }
                Expr.If(tk0, cnd, t, f)
            }
            this.acceptFix("while") -> {
                val tk0 = this.tk0 as Tk.Fix
                when {
                    !this.acceptFix("in") -> {
                        val cnd = this.expr()
                        this.catch_block(this.tk1).let { (C,b) ->
                            val e = b.es.last()
                            if (e.is_innocuous()) {
                                err(e.tk, "invalid expression : innocuous expression")
                            }
                            C(Expr.While(tk0, cnd, b))
                        }
                    }
                    XCEU && (this.acceptFix("[") || this.acceptFix("(")) -> {
                        val pre0 = tk0.pos.pre()

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

                        // { ... }
                        val blk = this.block()

                        val cmp = when {
                            (tkB.str=="]" && op=="+") -> "<="
                            (tkB.str==")" && op=="+") -> "<"
                            (tkB.str=="]" && op=="-") -> ">="
                            (tkB.str==")" && op=="-") -> ">"
                            else -> error("impossible case")
                        }

                        this.nest("""
                            ${pre0}do {
                                var ceu_step_$N = ${if (step==null) 1 else step.tostr(true) }
                                var $i = ${eA.tostr(true)} $op (
                                    ${if (tkA.str=="[") 0 else "ceu_step_$N"}
                                )
                                var ceu_limit_$N = ${eB.tostr(true)}
                                while $i $cmp ceu_limit_$N {
                                    ${blk.es.tostr(true)}
                                    set $i = $i $op ceu_step_$N
                                }                                
                            }
                        """)
                    }
                    else -> {
                        this.acceptEnu_err("Tag")
                        val tktag = this.tk0 as Tk.Tag
                        if (tktag.str !in ITERS) {
                            err(tk0, "invalid iterator : unexpected \"${tktag.str}\"")
                        }
                        val col = this.expr()
                        this.acceptFix_err(",")
                        val (i,v) = if (tktag.str in listOf(":tuple",":vector",":dict")) {
                            this.acceptFix_err("(")
                            this.acceptEnu_err("Id")
                            val ii = this.tk0 as Tk.Id
                            this.acceptFix_err(",")
                            this.acceptEnu_err("Id")
                            val vv = this.tk0 as Tk.Id
                            this.acceptFix_err(")")
                            Pair(ii,vv)
                        } else {
                            this.acceptEnu_err("Id")
                            Pair(this.tk0 as Tk.Id, null)
                        }
                        when {
                            tktag.str == ":tasks" -> {
                                val pre0 = tk0.pos.pre()
                                val blk = this.block()
                                this.nest("""
                                    ${pre0}do {
                                        var ceu_tasks_$N = ${col.tostr(true)}
                                        ```
                                        if (ceu_mem->ceu_tasks_$N.type != CEU_VALUE_X_TASKS) {                
                                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${col.tk.pos.file} : (lin ${col.tk.pos.lin}, col ${col.tk.pos.col}) : while error : expected tasks");
                                        }
                                        ```
                                        var ceu_n_$N = `:number ceu_mem->ceu_tasks_$N.Dyn->Bcast.Tasks.dyns.its`
                                        var ceu_i_$N = 0
                                        ${pre0}while ceu_i_$N /= ceu_n_$N {
                                            var ceu_dyn_$N = `:pointer ceu_mem->ceu_tasks_$N.Dyn->Bcast.Tasks.dyns.buf[(int)ceu_mem->ceu_i_$N.Number]`
                                            if ceu_dyn_$N == `:pointer NULL` {
                                                ;; empty slot
                                                set ceu_i_$N = `:number ceu_mem->ceu_i_$N.Number + 1` ;; just to avoid prelude
                                            } else {
                                                ;;;
                                                var ceu_x_$N
                                                `ceu_mem->ceu_x_$N = (CEU_Value) { CEU_VALUE_X_TASK, {.Dyn=ceu_mem->ceu_dyn_$N.Pointer} };`
                                                var ${i.str} = track(ceu_x_$N)
                                                ;;;

                                                ```
                                                    CEU_Value ceu_x_$N = { CEU_VALUE_X_TASK, {.Dyn=ceu_mem->ceu_dyn_$N.Pointer} };
                                                ```
                                                var ${i.str} = track(`:ceu ceu_x_$N`)
                                                ${blk.es.tostr(true)}
                                                if detrack(${i.str}) {
                                                    set ceu_i_$N = `:number ceu_mem->ceu_i_$N.Number + 1` ;; just to avoid prelude
                                                } else {
                                                    set ceu_i_$N = ceu_n_$N
                                                }
                                            }
                                        }
                                    }
                                """) //.let { println(it.tostr()); it }
                            }
                            tktag.str == ":coro" -> this.catch_block(this.tk1).let { (C,b) ->
                                C(this.nest("""
                                    do {
                                        var ceu_col_$N = ${col.tostr(true)}
                                        assert(type(ceu_col_$N) == :x-coro)
                                        var ${i.str} = nil
                                        until {
                                            set ${i.str} = resume ceu_col_$N(${i.str})
                                            var ceu_stop_$N = (ceu_col_$N.status == :terminated)
                                            if not ceu_stop_$N {
                                                set ${i.str} = do :unnest {
                                                    ${b.es.tostr(true)}
                                                }
                                            }
                                            ceu_stop_$N
                                        }
                                    }
                                    """) //.let { println(it.tostr());it })
                                )
                            }
                            tktag.str == ":tuple" -> this.catch_block(this.tk1).let { (C,b) ->
                                v!!
                                C(this.nest("""
                                    do {
                                        var ceu_tup_$N = ${col.tostr(true)}
                                        assert(type(ceu_tup_$N) == :tuple)
                                        var ${i.str} = 0
                                        var ${v.str} = nil
                                        while ${i.str} < #ceu_tup_$N {
                                            set ${v.str} = ceu_tup_$N[${i.str}]
                                            ${b.es.tostr(true)}
                                            set ${i.str} = ${i.str} + 1
                                        }
                                        ;;nil ;; iterators always evaluate to nil (b/c of nested iters)
                                    }
                                    """) //.let { println(it.tostr());it })
                                )
                            }
                            tktag.str == ":vector" -> this.catch_block(this.tk1).let { (C,b) ->
                                v!!
                                C(this.nest("""
                                    do {
                                        var ceu_vec_$N = ${col.tostr(true)}
                                        assert(type(ceu_vec_$N) == :vector)
                                        var ${i.str} = 0
                                        var ${v.str} = nil
                                        while ${i.str} < #ceu_vec_$N {
                                            set ${v.str} = ceu_vec_$N[${i.str}]
                                            ${b.es.tostr(true)}
                                            set ${i.str} = ${i.str} + 1
                                        }
                                        ;;nil ;; iterators always evaluate to nil (b/c of nested iters)
                                    }
                                    """) //.let { println(it.tostr());it })
                                )
                            }
                            tktag.str == ":dict" -> this.catch_block(this.tk1).let { (C,b) ->
                                v!!
                                C(this.nest("""
                                    do {
                                        var ceu_dict_$N = ${col.tostr(true)}
                                        assert(type(ceu_dict_$N) == :dict)
                                        var ${i.str} = next(ceu_dict_$N)
                                        while ${i.str} /= nil {
                                            var ${v.str} = ceu_dict_$N[${i.str}]
                                            ${b.es.tostr(true)}
                                            set ${i.str} = next(ceu_dict_$N, ${i.str})
                                        }
                                        ;;nil ;; iterators always evaluate to nil (b/c of nested iters)
                                    }
                                    """) //.let { println(it.tostr());it })
                                )
                            }
                            else -> error("impossible case")
                        }
                    }
                }
            }
            this.acceptFix("func") || this.acceptFix("coro") || this.acceptFix("task") -> {
                val tk0 = this.tk0 as Tk.Fix
                val isnote = (tk0.str=="func" || this.checkFix("(") || this.checkEnu("Tag") || this.checkFix("(") || (XCEU && this.checkEnu("Id")))
                if (isnote) {
                    val id = if (XCEU && this.acceptEnu("Id")) this.tk0 as Tk.Id else null
                    this.acceptFix_err("(")
                    val args = this.list0(")") {
                        this.acceptEnu("Id")
                        val id = this.tk0 as Tk.Id
                        val tag = if (!this.acceptEnu("Tag")) null else {
                            this.tk0 as Tk.Tag
                        }
                        Pair(id, tag)
                    }
                    val fake = (tk0.str == "task") && this.acceptTag(":fake")
                    val body = this.catch_block(this.tk1).let { (C,b) -> C(b) }.let {
                        if (it is Expr.Do) it else Expr.Do(tk0, true, true, listOf(it))
                    }
                    val proto = Expr.Proto(tk0, fake, args, body)
                    if (id == null) proto else {
                    this.nest("""
                        ${tk0.pos.pre()}var ${id.str} = ${proto.tostr(true)} 
                    """)
                }
                } else {
                    Expr.X(tk0)
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
                    Expr.Do(Tk.Fix("do",tpl.pos), false, false, l)
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
                        if (call !is Expr.Call && !((call is Expr.Do && !call.isnest) && call.es.last() is Expr.Call)) {
                            err(tk1, "invalid spawn : expected call")
                        }
                        Expr.Spawn(tk0, tasks, call)
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
                        if (call !is Expr.Call && !((call is Expr.Do && !call.isnest) && call.es.last() is Expr.Call)) {
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
            this.acceptFix("yield") -> {
                val all = XCEU && this.acceptTag(":all")
                if (!all) {
                    Expr.Yield(this.tk0 as Tk.Fix, this.expr_in_parens(!XCEU, XCEU)!!)
                } else {
                    val x = this.expr()
                    this.nest("""
                        do {
                            var ceu_x_$N = ${x.tostr(true)}
                            while in :coro ceu_x_$N, ceu_i_$N {
                                yield(ceu_i_$N)  ;; return of yield is used as arg to iter resume()
                            }
                        }
                    """)
                }
            }
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
                val task = this.expr()
                if (!XCEU || (task is Expr.Call && !(XCEU && this.checkFix("->")))) {
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
                    val (off,on) = Pair(task, this.expr())
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
                val b1 = if (this.checkFix("{")) this.block() else Expr.Do(this.tk0, true, true, listOf(this.expr()))
                ifs += """
                    ${pre0}if $e1 ${b1.tostr(true)} else {
                """
                var n = 1
                while (!this.acceptFix("}")) {
                    if (this.acceptFix("else")) {
                        this.acceptFix_err("->")
                        val be = if (this.checkFix("{")) this.block() else Expr.Do(this.tk0, true, true, listOf(this.expr()))
                        ifs += be.es.map { it.tostr(true)+"\n" }.joinToString("")
                        this.acceptFix("}")
                        break
                    }
                    val pre1 = this.tk0.pos.pre()
                    val eqi = (cnd!=null && (this.acceptOp("==") || this.acceptFix("is")))
                    val eqi_op = this.tk0.str
                    val ei = this.expr().let { if (!eqi) it.tostr(true) else "(ceu_ifs_${cnd!!.n} $eqi_op ${it.tostr(true)})" }
                    this.acceptFix_err("->")
                    val bi = if (this.checkFix("{")) this.block() else Expr.Do(this.tk0, true, true, listOf(this.expr()))
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
            (XCEU && this.acceptFix("until")) -> {
                val pre0 = this.tk0.pos.pre()
                val (C,cnd) = this.catch_block(this.tk1)
                C(this.nest("""
                    ${pre0}do {
                        var ceu_$N
                        ${pre0}while not (
                            set ceu_$N = do ${cnd.tostr(true)}
                        ) {
                            ;; cnd is the body
                        }
                        ceu_$N
                    }
                """))
            }
            (XCEU && this.acceptFix("await")) -> {
                val pre0 = this.tk0.pos.pre()
                val now = this.acceptTag(":check-now")
                val spw = this.checkFix("spawn")
                val (clk,cnd) = if (spw) Pair(null,null) else this.clk_or_exp()
                when {
                    (cnd is Expr.Tag) -> {   // await :key
                        val xcnd = if (!this.acceptFix(",")) "true" else {
                            this.expr().tostr(true)
                        }
                        this.nest("""
                            ${pre0}do :unnest {
                                var evt ${cnd.tk.str}
                                await (evt is ${cnd.tk.str}) and $xcnd
                            }
                        """)
                    }
                    spw -> { // await spawn T()
                        val e = this.expr()
                        if (!(e is Expr.Spawn && e.tasks==null)) {
                            err_expected(e.tk, "non-pool spawn")
                        }
                        this.nest("""
                            ${pre0}do {
                                var ceu_spw_$N = ${e.tostr(true)}
                                ${pre0}await :check-now (ceu_spw_$N.status == :terminated)
                                `ceu_acc = ceu_mem->ceu_spw_$N.Dyn->Bcast.X.frame->X.pub;`
                            }
                        """) //.let { println(it.tostr());it }
                    }
                    (clk != null) -> { // await 5s
                        this.nest("""
                            ${pre0}do {
                                var ceu_ms_$N = ${clk.map { (e,tag) ->
                                    val s = e.tostr(true)
                                    "(" + when (tag.str) {
                                        ":h"   -> "($s * ${1000*60*60})"
                                        ":min" -> "($s * ${1000*60})"
                                        ":s"   -> "($s * ${1000})"
                                        ":ms"  -> "($s * ${1})"
                                        else   -> error("impossible case")
                                    }
                                }.joinToString("+") + (")").repeat(clk.size)}
                                while ceu_ms_$N > 0 {
                                    await (evt is :frame)
                                    set ceu_ms_$N = ceu_ms_$N - evt.0
                                }
                            }
                        """)//.let { println(it.tostr()); it }
                    }
                    (cnd != null) -> {  // await evt==x | await trk | await coro
                        this.nest("""
                            ${pre0}do :unnest {
                                ${pre0}${(!now).cond { "yield ()" }}
                                until {
                                    var ceu_cnd_$N = ${cnd.tostr(true)}
                                    ifs {
                                        type(ceu_cnd_$N) == :x-task -> {
                                            set ceu_cnd_$N = (ceu_cnd_$N.status == :terminated)
                                        }
                                        type(ceu_cnd_$N) == :x-track -> {
                                            set ceu_cnd_$N = (detrack(ceu_cnd_$N) == nil)
                                        }
                                        else -> {
                                            ;;set ceu_cnd_$N = ceu_cnd_$N
                                        }
                                    }
                                    if not ceu_cnd_$N {
                                        yield ()
                                    }
                                    (not (not ceu_cnd_$N))
                                }
                            }
                        """)//.let { println(it.tostr()); it }
                    }
                    else -> error("bug found")
                }
            }
            (XCEU && this.acceptFix("every")) -> {
                val pre0 = this.tk0.pos.pre()
                val now = this.acceptTag(":check-now")
                val (clk,cnd) = this.clk_or_exp()
                val clk_expr = this.clk_or_exp_tostr(Pair(clk,cnd))
                val xcnd = when {
                    (cnd !is Expr.Tag) -> ""
                    !this.acceptFix(",") -> ""
                    else -> ", " + this.expr().tostr(true)
                }
                val body = this.block()
                this.nest("""
                    ${pre0}while true {
                        await ${now.cond { ":check-now" }} $clk_expr $xcnd
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
                            var ceu_${i}_$n = spawn {
                                set _ceu_$n = do {
                                    ${body.es.tostr(true)}
                                }
                            }
                        """}.joinToString("")}
                        await :check-now (
                            ${pars.mapIndexed { i,_ -> """
                                ((ceu_${i}_$n.status == :terminated) and
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
                            var ceu_${i}_$n = spawn {
                                var _ceu_${i}_$n = do {
                                    ${body.es.tostr(true)}
                                }
                                set _ceu_$n = _ceu_$n or _ceu_${i}_$n 
                            }
                        """}.joinToString("")}
                        await :check-now (
                            ${pars.mapIndexed { i,_ -> """
                                ((ceu_${i}_$n.status == :terminated) or
                            """}.joinToString("")} false ${")".repeat(pars.size)}
                        )
                        _ceu_$n
                    }
                """)
            }
            (XCEU && this.acceptFix("awaiting")) -> {
                val pre0 = this.tk0.pos.pre()
                val now = this.acceptTag(":check-now")
                val (clk,cnd) = this.clk_or_exp()
                val clk_expr = this.clk_or_exp_tostr(Pair(clk,cnd))
                val xcnd = when {
                    (cnd !is Expr.Tag) -> ""
                    !this.acceptFix(",") -> ""
                    else -> ", " + this.expr().tostr(true)
                }
                val body = this.block()
                this.nest("""
                    ${pre0}par-or {
                        await ${now.cond { ":check-now" }} $clk_expr $xcnd
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
                                    do :unnest :hide { 
                                        var ceu_col_$N :tmp = ${e.tostr(true)}
                                        ceu_col_$N[(#ceu_col_$N)-1]
                                    }
                                """)
                                "+" -> this.nest("""
                                    do :unnest :hide { 
                                        var ceu_col_$N :tmp = ${e.tostr(true)}
                                        ceu_col_$N[#ceu_col_$N]
                                    }
                                """) //.let { println(it.tostr());it }
                                "-" -> this.nest("""
                                    do :unnest :hide { 
                                        var ceu_col_$N :tmp = ${e.tostr(true)}
                                        var ceu_i_$N = ceu_col_$N[(#ceu_col_$N)-1]
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
                        this.acceptFix("pub") || this.acceptFix("status") -> Expr.Pub(this.tk0 as Tk.Fix, e)
                        this.acceptEnu("Id") -> Expr.Index(e.tk, e, Expr.Tag(Tk.Tag(':'+this.tk0.str,this.tk0.pos)))
                        (XCEU && this.acceptEnu("Num")) -> {
                            val num = this.tk0 as Tk.Num
                            val l = num.str.split('.')
                            var idx = Expr.Index(e.tk, e, Expr.Num(Tk.Num(l.first(), tk0.pos)))
                            for (i in 1..l.size-1) {
                                idx = Expr.Index(idx.tk, idx, Expr.Num(Tk.Num(l[i], tk0.pos)))
                            }
                            idx as Expr.Index
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
                        var ceu_${e.n} :tmp
                        set ceu_${e.n} = ${e.tostr(true)} 
                        if ceu_${e.n} { ceu_${e.n} } else { ${e2.tostr(true)} }
                    }
                """)
                "and"   -> this.nest("""
                    ${op.pos.pre()}do {
                        var ceu_${e.n} :tmp
                        set ceu_${e.n} = ${e.tostr(true)} 
                        if ceu_${e.n} { ${e2.tostr(true)} } else { ceu_${e.n} }
                    }
                """)
                "is"    -> this.nest("is'(${e.tostr(true)}, ${e2.tostr(true)})")
                "isnot" -> this.nest("isnot'(${e.tostr(true)}, ${e2.tostr(true)})")
                else    -> Expr.Call(op, Expr.Acc(Tk.Id("{${op.str}}",op.pos,0)), listOf(e,e2))
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
                    ${tk0.pos.pre()}do :unnest :hide {
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
