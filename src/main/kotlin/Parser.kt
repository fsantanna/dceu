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

    fun clock (): Clock {
        this.acceptOp_err("<")
        val us = listOf(":h", ":min", ":s", ":ms")
        val l = list0(null, {this.acceptOp(">")}) {
            val e = this.expr()
            this.acceptEnu_err("Tag")
            val u = this.tk0 as Tk.Tag
            if (!us.contains(u.str)) {
                err(u, "invalid clock unit : unexpected \"${u.str}\"")
            }
            Pair(u,e)
        }
        return l
    }

    fun patt (xid: String? = "it"): Patt {
        // Patt : ([id] [:Tag] [
        //          (<op> <expr>) |
        //          <const> |
        //          `[´{<Patt>,}]
        //        ] [| cnd])

        val par = this.acceptFix("(")

        val id: Tk.Id = when {
            (CEU<99 || this.checkEnu("Id")) -> {
                this.acceptEnu_err("Id")
                this.tk0 as Tk.Id
            }
            (xid == null) -> Tk.Id("ceu_patt_${G.N}", this.tk0.pos.copy())
            else -> Tk.Id(xid, this.tk0.pos.copy())
        }

        val tag: Tk.Tag? = if (this.acceptEnu("Tag")) this.tk0 as Tk.Tag else null

        val f: (Expr)->Patt = when {
            (CEU < 99) -> { pos -> Patt.None(id,tag,pos) }
            !this.checkOp("|") && this.acceptEnu("Op") -> {
                // ambiguous with `|´
                // (== 10)
                // ({{even?}})
                val op = this.tk0.str.let {
                    if (it[0] in OPERATORS || it in XOPERATORS) "{{$it}}" else it
                }
                val e2 = if (this.checkFix("=>") || this.checkFix("{")) null else this.expr()
                val xe = if (e2 === null) {
                    "$op(${id.str})"
                } else {
                    "$op(${id.str}, ${e2.to_str(true)})"
                }.let { this.nest(it) }
                ;
                { pos -> Patt.One(id,tag,xe,pos) }
            }
            (this.checkFix("nil") || this.checkFix("false") || this.checkFix("true") || this.checkEnu("Chr") || this.checkEnu("Num")) -> {
                // const
                val e = this.expr()
                val xe = this.nest("${id.str} == ${e.to_str(true)}")
                ;
                { pos -> Patt.One(id,tag,xe,pos) }
            }
            // [...]
            this.acceptFix("[") -> {
                val l = this.list0(",","]") {
                    this.patt(xid)
                }
                ;
                { pos -> Patt.Tup(id,tag,l,pos) }
            }
            else -> { pos -> Patt.None(id,tag,pos) }
        }

        val pos = if (CEU>=99 && !this.checkOp("|")) {
            Expr.Bool(Tk.Fix("true",this.tk0.pos.copy()))
        } else {
            this.acceptOp_err("|")
            this.expr()
        }

        if (par) {
            this.acceptFix_err(")")
        }

        return f(pos)
    }

    fun Patt.code2 (v: String?): String {
        val idtag = Pair(this.id, this.tag)
        val pre = idtag.first.pos.pre()
        return """
            group {
                val ${idtag.to_str(true)} = ${v.cond2({it},{"nil"})}
                ${when (this) {
                    is Patt.None -> """
                        ${pre}assert(${this.pos.to_str(true)}, :Patt)
                    """
                    is Patt.One  -> """
                        ${pre}assert(${this.e.to_str(true)}, :Patt)
                        ${pre}assert(${this.pos.to_str(true)}, :Patt)
                    """
                    is Patt.Tup  -> {
                        val nn = G.N++
                        """
                        ${v.cond { """
                            ${pre}assert((type(${id.str})==:tuple) and (#${id.str}==${l.size}), :Patt)
                            val ceu_tup_$nn = ${id.str}
                        """ }}
                        ${pre}assert(${this.pos.to_str(true)}, :Patt)
                        ${this.l.mapIndexed { i,x ->
                            x.code2(if (v == null) null else "ceu_tup_$nn[$i]")
                        }.joinToString("")}
                        ${pre}assert(${this.pos.to_str(true)}, :Patt)
                        """
                    }
                }}
            }
        """
    }

    fun Patt.code3 (v: String, cnt: String): String {
        val idtag = Pair(this.id, this.tag)
        return """
            do {
                val ${idtag.to_str(true)} = $v
                ${this.tag.cond{ "if ${this.id.str} is? ${it.str} {" }}
                ${when (this) {
            is Patt.None -> """
                        if ${this.pos.to_str(true)} {
                            $cnt
                        }
                    """
            is Patt.One  -> """
                        if ${this.e.to_str(true)} {
                            if ${this.pos.to_str(true)} {
                                $cnt
                            }
                        }
                    """
            is Patt.Tup  -> {
                val nn = G.N++
                val cnt2 = """
                            if ${this.pos.to_str(true)} {
                                $cnt
                            }
                        """
                """
                        if (type(${id.str})==:tuple) and (#${id.str} >= ${l.size}) {
                            val ceu_tup_$nn = ${id.str}
                            if ${this.pos.to_str(true)} {
                                ${this.l.foldRightIndexed(cnt2) { i,x,acc ->
                    x.code3("ceu_tup_$nn[$i]", acc)
                }}
                            }
                        }
                        """
            }
        }}
                ${this.tag.cond{ "}" }}
            }
        """
    }

    fun <T> list0 (sep: String?, close: String, func: () -> T): List<T> {
        return list0(sep, { this.acceptFix(close) }, func)

    }
    fun <T> list0 (sep: String?, close: ()->Boolean, func: () -> T): List<T> {
        val l = mutableListOf<T>()
        if (!close()) {
            l.add(func())
            while (true) {
                if (close()) {
                    break
                }
                if (sep != null) {
                    this.acceptFix_err(sep)
                    if (close()) {
                        break
                    }
                }
                l.add(func())
            }
        }
        return l
    }

    fun block (tk0: Tk? = null): Expr.Do {
        val tk = when {
            (tk0 !== null) -> tk0
            (this.tk0.str=="do") -> this.tk0
            else -> this.tk1
        }
        this.acceptFix_err("{")
        val es = this.exprs()
        this.acceptFix_err("}")
        return Expr.Do(tk, es)
    }

    fun lambda (it: Boolean): Pair<List<Id_Tag>,List<Expr>> {
        this.acceptFix_err("{")
        val tk0 = this.tk0
        val idstags = when {
            !this.acceptFix("\\") -> if (!it) emptyList() else listOf(Pair(Tk.Id("it", tk0.pos.copy()), null))
            this.acceptEnu("Tag") -> {
                val tag = this.tk0 as Tk.Tag
                this.acceptFix_err("=>")
                listOf(Pair(Tk.Id("it", tk0.pos.copy()), tag))
            }
            else -> {
                list0(",", "=>") {
                    this.acceptEnu_err("Id")
                    val id = this.tk0 as Tk.Id
                    val tag = if (!this.acceptEnu("Tag")) null else this.tk0 as Tk.Tag
                    Pair(id, tag)
                }
            }
        }
        val es = this.exprs()
        this.acceptFix_err("}")
        return Pair(idstags, es)
    }

    fun id_tag (): Pair<Tk.Id, Tk.Tag?> {
        this.acceptEnu_err("Id")
        val id = this.tk0 as Tk.Id
        val tag = if (!this.acceptEnu("Tag")) null else {
            this.tk0 as Tk.Tag
        }
        return Pair(id, tag)
    }

    fun method (f: Expr, e: Expr, pre: Boolean): Expr {
        return this.nest(when {
            (f is Expr.Call)  -> {
                val args = if (pre) {
                    e.to_str(true) + f.args.map { "," + it.to_str(true) }.joinToString("")
                } else {
                    f.args.map { it.to_str(true) + "," }.joinToString("") + e.to_str(true)
                }
                """
                ${f.clo.to_str(true)}($args)
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
            else -> "${f.to_str(true)}(${e.to_str(true)})}"
        })
    }

    fun expr_prim (): Expr {
        return when {
            this.acceptFix("val") || this.acceptFix("var") || (CEU>=50 && (this.acceptFix("val'") || this.acceptFix("var'"))) -> {
                val pat = this.patt(null)
                TODO()
            }
            this.acceptFix("func'") || (CEU>=3 && this.acceptFix("coro'")) || (CEU>=4 && this.acceptFix("task'")) -> {
                val fak = (CEU >= 50) && (tk0.str=="task'") && this.acceptTag(":fake")
                TODO()
            }
            this.acceptFix("data") -> {
                val pos = this.tk0.pos.copy()
                this.acceptEnu_err("Tag")
                val tag = this.tk0 as Tk.Tag

                fun one (pre: Tk.Tag?, me: Tk.Tag): List<Expr.Data> {
                    val xme = if (pre === null) me else {
                        Tk.Tag(pre.str+'.'+me.str.drop(1), me.pos.copy())
                    }
                    this.acceptFix_err("=")
                    this.acceptFix_err("[")
                    val (ids,dtss) = this.list0(",", "]") {
                        val id = if (this.acceptEnu("Fix")) {
                            if (!KEYWORDS.contains(this.tk0.str)) {
                                err(this.tk0, "invalid field : unexpected \"${this.tk0.str}\"")
                            }
                            Tk.Id(this.tk0.str, this.tk0.pos.copy())
                        } else {
                            this.acceptEnu_err("Id")
                            this.tk0 as Tk.Id
                        }
                        val xtag = if (!this.acceptEnu("Tag")) null else {
                            this.tk0 as Tk.Tag
                        }
                        if (this.checkFix("=")) {
                            val xxtag = if (xtag!==null) xtag else Tk.Tag(":ceu_tag_${G.N}",id.pos.copy())
                            Pair(Pair(id, xxtag), one(null, xxtag))
                        } else {
                            Pair(Pair(id, xtag), emptyList())
                        }
                    }.unzip()
                    return dtss.flatten() + listOf(Expr.Data(xme, ids)) + when {
                        (CEU < 99) -> emptyList()
                        !this.acceptFix("{") -> emptyList()
                        else -> {
                            val ll = mutableListOf<Expr.Data>()
                            while (this.acceptEnu("Tag")) {
                                val l = one(xme, this.tk0 as Tk.Tag)
                                //if (l.isEmpty()) {
                                //    break
                                //}
                                ll.addAll(l)
                            }
                            this.acceptFix_err("}")
                            ll
                        }
                    }
                }

                val dts = one(null, tag)
                //l.forEach { println(it.tostr()) }
                when {
                    (dts.size == 1) -> dts.first()
                    (CEU < 99) -> error("bug found")
                    else -> Expr.Do(Tk.Fix("do",pos), dts)
                }
            }

            (CEU>=4 && this.acceptFix("spawn")) -> {
                if (CEU>=99 && this.checkFix("{")) {
                    val blk = this.block()
                    return this.nest("""
                        ${this.tk0.pos.pre()}(spawn (task :fake () {
                            ${blk.es.to_str(true)}
                        }) ())
                    """)
                }

                val tk0 = this.tk0 as Tk.Fix
                val call = this.expr()
                if (call !is Expr.Call) {
                    err(this.tk1, "spawn error : expected call")
                }
                val tsks = if (CEU<5 || !this.acceptFix("in")) null else {
                    this.expr()
                }
                Expr.Spawn(tk0, tsks, call.clo, call.args)
            }
            (CEU>=4 && this.acceptFix("delay")) -> Expr.Delay(this.tk0 as Tk.Fix)
            (CEU>=4 && this.acceptFix("pub")) -> Expr.Pub(this.tk0 as Tk.Fix, null)
            (CEU>=4 && this.acceptFix("broadcast")) -> {
                this.acceptFix("in")
                TODO()
            }
            (CEU>=4 && this.acceptFix("toggle")) -> {
                val tk0 = this.tk0 as Tk.Fix
                if (CEU>=99 && this.acceptEnu("Tag")) {
                    val tag = this.tk0 as Tk.Tag
                    val blk = this.block()
                    this.nest("""
                        do {
                            val task_${G.N} = spawn ;;{
                                ${blk.to_str_x(true)}
                            ;;}
                            if (status(task_${G.N}) /= :terminated) { 
                                watching (|it==task_${G.N}) {
                                    loop {
                                        await(${tag.str} | not it[0])
                                        toggle task_${G.N}(false)
                                        await(${tag.str} | it[0])
                                        toggle task_${G.N}(true)
                                    }
                                }
                            }
                            task_${G.N}.pub
                        }
                    """)//.let { println(it.tostr()); it }
                } else {
                    val tsk = this.expr_prim()
                    this.acceptFix_err("(")
                    val on = this.expr()
                    this.acceptFix_err(")")
                    Expr.Toggle(tk0, tsk, on)
                }
            }
            (CEU>=5 && this.acceptFix("tasks")) -> {
                if (this.checkFix(")")) {
                    Expr.Nil(Tk.Fix("nil", this.tk0.pos.copy()))
                } else {
                    this.expr()
                }
                TODO()
            }

            this.acceptEnu("Nat")  -> Expr.Nat(this.tk0 as Tk.Nat)

            (CEU>=99 && (this.acceptFix("func") || this.acceptFix("coro") || this.acceptFix("task"))) -> {
                val fak = (tk0.str=="task'") && this.acceptTag(":fake")
                TODO()
            }
            (CEU>=99 && this.acceptFix("enum")) -> {
                if (this.acceptEnu("Tag")) {
                    val tag = this.tk0 as Tk.Tag
                    this.acceptFix_err("{")
                    val ids = this.list0(",", "}") {
                        this.acceptEnu_err("Id")
                        this.tk0 as Tk.Id
                    }
                    this.nest("""
                        group {
                            ${tag.str}
                            ${ids.map { tag.str + "-" + it.str }.joinToString("\n")}
                        }
                    """)
                } else {
                    this.acceptFix_err("{")
                    val tags = this.list0(",", "}") {
                        this.acceptEnu_err("Tag")
                        val tag = this.tk0 as Tk.Tag
                        if (tag.str.contains('.')) {
                            err(tag, "enum error : enum tag cannot contain '.'")
                        }
                        tag
                    }
                    this.nest("""
                        group {
                            ${tags.map { it.str }.joinToString("\n")}
                        }
                """)
                }
            }
            (CEU>=99 && this.acceptFix("skip")) -> {
                val tk0 = this.tk0 as Tk.Fix
                this.acceptFix_err("(")
                val e = if (this.checkFix(")")) null else {
                    this.expr()
                }
                this.acceptFix_err(")")
                Expr.Escape(tk0, Tk.Tag(":"+tk0.str,tk0.pos.copy()), e)
            }
            (CEU>=99 && this.checkFix("{")) -> {
                val (idstags,es) = lambda(true)
                return this.nest("""
                    (func (${idstags.map { it.to_str(true)}.joinToString(",")}) {
                        ${es.to_str(true)}
                    })
                """)
            }
            (CEU>=99 && this.acceptFix("ifs")) -> {
                val tk0 = this.tk0
                this.acceptFix_err("{")
                val ifs = list0(null, "}") {
                    val cnd = when {
                        this.acceptFix("do") -> null
                        this.acceptFix("else") -> Expr.Bool(Tk.Fix("true",this.tk0.pos.copy()))
                        else -> this.expr()
                    }
                    val es = if (this.acceptFix("=>")) {
                        Pair(emptyList(), listOf(this.expr()))
                    } else {
                        this.lambda(false)
                    }
                    Pair(cnd, es)
                }
                this.nest("""
                    do {
                        ;;`/* IFS | ${tk0.dump()} */`
                        ${ifs.map { (cnd,idstags_es) ->
                            val (idstags,es) = idstags_es
                            val idtagx = if (idstags.isEmpty()) Pair(Tk.Id("ceu_ifs_${G.N}",tk0.pos.copy()),null) else idstags.first() 
                            if (cnd === null) {
                                es.to_str(true) // do ...
                            } else {
                                """
                                val ${idtagx.to_str(true)} = ${cnd.to_str(true)}
                                if ${idtagx.first.str} {
                                    ${es.to_str(true)}
                                } else {
                                """
                            }
                        }.joinToString("")}
                        ${ifs.map { (cnd,_) -> cnd.cond {"""
                            }   ;; ignore "do {}"
                        """}}.joinToString("")}
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("match")) -> {
                val nn = G.N++
                val xv = this.expr()
                this.acceptFix_err("{")
                fun case (): String {
                    fun cont (): String {
                        return when {
                            this.acceptFix("=>") -> {
                                val e = this.expr()
                                """
                                set ceu_ret_$nn = ${e.to_str(true)}
                                true
                                """
                            }
                            else -> {
                                val (idstags, es) = this.lambda(false)
                                """
                                set ceu_ret_$nn = group {
                                    ${(idstags.isNotEmpty()).cond { "val ${idstags.first().to_str(true)} = `:ceu ceu_acc`" }}
                                    ${es.to_str(true)}
                                }
                                true
                                """
                            }
                        }
                    }
                    return when {
                        this.acceptFix("}") -> "nil"
                        this.acceptFix("else") -> {
                            val ret = cont()
                            this.acceptFix_err("}")
                            "do { $ret }"
                        }
                        this.acceptFix("do") -> {
                            val pat1 = this.patt("it")
                            val pat2 = pat1.code2("ceu_val_$nn")
                            val cnt = if (this.checkFix("{") || this.checkFix("=>")) {
                                cont()
                            } else {
                                null
                            }
                            """
                            do {
                                $pat2
                                ${cnt.cond { it }}
                                ${case()}
                            }
                            """
                        }
                        else -> {
                            val pat1 = this.patt("it")
                            val pat2 = pat1.code3("ceu_val_$nn", cont())
                            """
                            (${pat2.trimEnd()} or ${case()})
                            """
                        }
                    }
                }
                this.nest("""
                    do {
                        var ceu_ret_$nn
                        val ceu_val_$nn = ${xv.to_str(true)}
                        ${case()}
                        drop(ceu_ret_$nn)
                    }
                """)//.let { println(it);it })
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
                    do {
                        val ceu_co_${G.N} = ${call.clo.to_str(true)}
                        var ceu_arg_${G.N} = ${if (call.args.size==0) "nil" else call.args[0].to_str(true)}
                        loop {
                            val ceu_v_${G.N} = resume ceu_co_${G.N}(ceu_arg_${G.N})
                            if (status(ceu_co_${G.N}) == :terminated) {
                                break(drop(ceu_v_${G.N}))
                            }
                            set ceu_arg_${G.N} = yield(drop(ceu_v_${G.N}))
                        }
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("await")) -> {
                val pre = this.tk0.pos.pre()
                val par = this.acceptFix("(")
                when {
                    this.checkOp("<") -> {
                        val l = clock()
                        val clk = l.map { (tag, e) ->
                            val s = e.to_str(true)
                            "(" + when (tag.str) {
                                ":h" -> "($s * ${1000 * 60 * 60})"
                                ":min" -> "($s * ${1000 * 60})"
                                ":s" -> "($s * ${1000})"
                                ":ms" -> "($s * ${1})"
                                else -> error("impossible case")
                            }
                        }.joinToString("+") + (")").repeat(l.size)
                        val ret = this.nest("""
                            do {
                                var ceu_clk_${G.N} = $clk
                                await(:Clock | do {
                                    set ceu_clk_${G.N} = ceu_clk_${G.N} - it.ms
                                    (ceu_clk_${G.N} <= 0)
                                })
                            }
                        """)
                        if (par) {
                            this.acceptFix_err(")")
                        }
                        ret
                    }
                    this.checkFix("spawn") -> {
                        val spw = this.expr()
                        spw as Expr.Spawn
                        val ret = this.nest("""
                            do {
                                val ceu_spw_${G.N} = ${spw.to_str(true)}
                                if (status(ceu_spw_${G.N}) /= :terminated) {
                                    await(|it==ceu_spw_${G.N})
                                }
                                ceu_spw_${G.N}.pub
                            }
                        """)
                        if (par) {
                            this.acceptFix_err(")")
                        }
                        ret
                    }
                    else -> {
                        val pat1 = this.patt("it")
                        if (par) {
                            this.acceptFix_err(")")
                        } else {
                            this.checkFix_err("{")
                        }
                        val nn = G.N++
                        val cnt = if (!this.checkFix("{")) "true" else """
                            set ceu_ret_$nn = group {
                                ${this.block().es.to_str(true)}
                            }
                            true
                        """
                        val pat2 = pat1.code3("ceu_ret_$nn", cnt)
                        this.nest("""
                            do {
                                var' ceu_ret_$nn
                                loop {
                                    set ceu_ret_$nn = ${pre}yield()
                                    until $pat2                                
                                }
                                delay
                                ceu_ret_$nn
                            }
                        """
                        ) //.let { println(it.tostr());it }
                    }
                }
            }
            (CEU>=99 && this.acceptFix("every")) -> {
                val nn = G.N++
                if (this.checkOp("<")) {
                    val clk = clock()
                    val blk = this.block()
                    this.nest("""
                        loop {
                            await ${clk.to_str(true)}
                            ${blk.es.to_str(true)}
                        }
                    """)
                } else {
                    val pat = this.patt()
                    val blk = this.block()
                    this.nest("""
                        do {
                            var ceu_ret_$nn
                            loop {
                                until await ${pat.to_str(true)} {
                                    var ceu_brk_$nn = true
                                    set ceu_ret_$nn = loop {
                                        ${blk.es.to_str(true)}
                                        set ceu_brk_$nn = false
                                        until true
                                    }
                                    ceu_brk_$nn
                                }
                            }
                            ceu_ret_$nn
                        }
                    """)
                }
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
                        ${it.es.to_str(true)}
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
                            val ceu_par_${i}_$n = spawn {
                                ${body.es.to_str(true)}
                            }
                        """}.joinToString("")}
                        loop {
                            until (
                                ${pars.mapIndexed { i,_ -> """
                                    (((status(ceu_par_${i}_$n) == :terminated) and (ceu_par_${i}_$n.pub or true)) or
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
                            val ceu_par_${i}_$n = spawn {
                                ${body.es.to_str(true)}
                            }
                        """}.joinToString("")}
                        loop {
                            until (
                                ${pars.mapIndexed { i,_ -> """
                                    ((status(ceu_par_${i}_$n) == :terminated) and
                                """}.joinToString("")} true ${")".repeat(pars.size)}
                            )
                            yield()
                            delay
                        }
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("watching")) -> {
                val pat = if (this.checkOp("<")) {
                    clock().to_str(true)
                } else {
                    this.patt().to_str(true)
                }
                val blk = this.block()
                this.nest("""
                    par-or {
                        await $pat
                    } with {
                        ${blk.es.to_str(true)}
                    }
                """)
            }
            (CEU>=99 && this.acceptFix("test")) -> {
                val blk = this.block()
                if (TEST) {
                    blk
                } else {
                    Expr.Nil(Tk.Fix("nil", this.tk0.pos.copy()))
                }
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
        val e = if (xe !== null) xe else this.expr_prim()
        val ok = this.tk0.pos.is_same_line(this.tk1.pos) && (
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
                                ${e.to_str(true)} thus { \ceu_ppp_${G.N} =>
                                    `/* = */`
                                    ceu_ppp_${G.N}[#ceu_ppp_${G.N}-1]
                                }
                            """)
                            "+" -> this.nest("""
                                ${e.to_str(true)} thus { \ceu_ppp_${G.N} =>
                                    `/* + */`
                                    ceu_ppp_${G.N}[#ceu_ppp_${G.N}]
                                }
                            """)
                            "-" -> this.nest("""
                                ${e.to_str(true)} thus { \ceu_x_${G.N} =>
                                    `/* - */`
                                    ceu_x_${G.N}[#ceu_x_${G.N}-1] thus { \ceu_y_${G.N} =>
                                        set ceu_x_${G.N}[#ceu_x_${G.N}-1] = nil
                                        ceu_y_${G.N}
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
                        val nn = G.N++
                        this.acceptEnu_err("Tag")
                        val tag = this.tk0
                        this.acceptFix_err(")")
                        val acc = Expr.Acc(Tk.Id("ceu_cast_$nn", e.tk.pos.copy()))
                        this.nest("""
                            ${e.to_str(true)} thus { \ceu_cast_$nn ${tag.str} =>
                                ${this.expr_4_suf(acc).to_str(true)}
                            }
                        """) //.let { println(it);it })
                    }
                    (CEU>=4 && this.acceptFix("pub")) -> Expr.Pub(e.tk, e)
                    this.acceptEnu("Fix") -> {
                        if (!KEYWORDS.contains(this.tk0.str)) {
                            err(this.tk0, "invalid field : unexpected \"${this.tk0.str}\"")
                        }
                        Expr.Index(e.tk, e, Expr.Tag(Tk.Tag(':' + this.tk0.str, this.tk0.pos.copy())))
                    }
                    this.acceptEnu_err("Id") -> Expr.Index(e.tk, e, Expr.Tag(Tk.Tag(':' + this.tk0.str, this.tk0.pos.copy())))
                    else -> error("impossible case")
                }
                "(" -> {
                    val args = this.list0(",",")") { this.expr() }
                    when {
                        (e is Expr.Acc && e.tk.str in XOPERATORS) -> {
                            when (args.size) {
                                1 -> this.nest("${e.to_str(true)} ${args[0].to_str(true)}")
                                2 -> this.nest("${args[0].to_str(true)} ${e.to_str(true)} ${args[1].to_str(true)}")
                                else -> err(e.tk, "operation error : invalid number of arguments")
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
        val e = if (xe !== null) xe else this.expr_4_suf()
        val ok = (CEU>=99) && (this.acceptFix("->") || this.acceptFix("<-"))
        if (!ok) {
            return e
        }
        if (xop!==null && xop!==this.tk0.str) {
            //err(this.tk0, "sufix operation error : expected surrounding parentheses")
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
                        ${tk0.pos.pre()}tag(${tk0.str}, ${tup.to_str(true)})
                    """)
                } else {
                    this.expr_3_met(null, Expr.Tag(this.tk0 as Tk.Tag))
                }
            }
            this.acceptEnu("Op") -> {
                val op = this.tk0 as Tk.Op
                val e = this.expr_2_pre()
                //println(listOf(op,e))
                when {
                    (op.str == "not") -> this.nest("${op.pos.pre()}(if ${e.to_str(true)} { false } else { true })\n")
                    else -> Expr.Call(op, Expr.Acc(Tk.Id("{{${op.str}}}", op.pos.copy())), listOf(e))
                }
            }
            else -> this.expr_3_met()
        }
    }
    fun expr_1_bin (xop: String? = null, xe1: Expr? = null): Expr {
        val e1 = if (xe1 !== null) xe1 else this.expr_2_pre()
        val ok = this.tk1.pos.is_same_line(this.tk0.pos.copy()) && // x or \n y (ok) // x \n or y (not allowed) // problem with '==' in 'ifs'
                    this.acceptEnu("Op")
        if (!ok) {
            return e1
        }
        if (xop!==null && xop!=this.tk0.str) {
            err(this.tk0, "binary operation error : expected surrounding parentheses")
        }
        val op = this.tk0
        val e2 = this.expr_2_pre()
        return this.expr_1_bin(op.str,
            when (op.str) {
                "and" -> this.nest("""
                    ${e1.to_str(true)} thus { \ceu_and_${G.N} =>
                        if ceu_and_${G.N} => ${e2.to_str(true)} => ceu_and_${G.N}
                    }
                """)
                "or" -> this.nest("""
                    ${e1.to_str(true)} thus { \ceu_or_${G.N} =>
                        if ceu_or_${G.N} => ceu_or_${G.N} => ${e2.to_str(true)}
                    }
                """)
                "is?" -> this.nest("is'(${e1.to_str(true)}, ${e2.to_str(true)})")
                "is-not?" -> this.nest("is-not'(${e1.to_str(true)}, ${e2.to_str(true)})")
                "in?" -> this.nest("in'(${e1.to_str(true)}, ${e2.to_str(true)})")
                "in-not?" -> this.nest("in-not'(${e1.to_str(true)}, ${e2.to_str(true)})")
                else -> {
                    val id = if (op.str[0] in OPERATORS) "{{${op.str}}}" else op.str
                    Expr.Call(op, Expr.Acc(Tk.Id(id, op.pos.copy())), listOf(e1,e2))
                }
            }
        )
    }
    fun expr_0_out (xop: String? = null, xe: Expr? = null): Expr {
        val e = if (xe !== null) xe else this.expr_1_bin()
        val ok = (CEU>=99 && (this.acceptFix("where") || this.acceptFix("thus") || this.acceptFix("-->") || this.acceptFix("<--")))
        if (!ok) {
            return e
        }
        if (xop!==null && xop!=this.tk0.str) {
            //err(this.tk0, "sufix operation error : expected surrounding parentheses")
        }
        val op = this.tk0
        return when (op.str) {
            "where" -> {
                val body = this.block()
                this.expr_0_out(op.str,
                    this.nest("""
                        ${op.pos.pre()}do {
                            ${body.es.to_str(true)}
                            ${e.to_str(true)}
                        }
                    """)
                )
            }
            "thus" -> {
                val (idstags,es) = lambda(true)
                this.nest( """
                    do {
                        ${idstags.first().first.pos.pre()}val' ${idstags.first().to_str(true)} = ${e.to_str(true)}
                        ${es.to_str(true)}
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

    fun exprs (): List<Expr> {
        val ret = mutableListOf<Expr>()
        if (CEU < 99) {
            ret.add(this.expr())
        }
        while (!this.checkFix("}") && !this.checkEnu("Eof")) {
            ret.add(this.expr())
        }
        if (CEU >= 99) {
            if (ret.size == 0) {
                ret.add(Expr.Nil(Tk.Fix("nil", this.tk0.pos.copy())))
            }
        }
        return ret
    }
}
