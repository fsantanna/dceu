// block: String -> current enclosing block for normal allocation
// ret: Pair<block,var> -> enclosing assignment with destination block and variable
fun fset(tk: Tk, ret: Pair<String, String>?, src: String): String {
    return if (ret == null) "" else fset(tk, ret.first, ret.second, src)
}
fun fset(tk: Tk, ret_block: String, ret_var: String, src: String): String {
    return """
        if ($src.tag == CEU_VALUE_TUPLE) {
            if ($src.Dyn->block->depth > $ret_block->depth) {                
                ceu_throw = &CEU_THROW_ERROR;
                strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${tk.pos.lin}, col ${tk.pos.col}) : set error : incompatible scopes", 256);
                continue;
            }
        }
        $ret_var = $src;
    """
}

fun Tk.dump (): String {
    return "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})\n"
}

// func (args) or block (locals)
data class XBlock (val syms: MutableSet<String>, val defers: MutableList<String>?)

class Coder (val outer: Expr.Block) {
    val code: String
    val mem: String
    val ups = outer.ups()
    val tags = mutableListOf (
        "nil",
        "tag",
        "bool",
        "number",
        "tuple",
        "func",
        "task",
        "coro",
        "coros",
        "error",
    )
    val xblocks = mutableMapOf<Expr,XBlock>()
    val tops = mutableListOf<Pair<String,String>>()

    init {
        this.xblocks[outer] = XBlock (
            mutableSetOf("tags", "print", "println", "op_eq_eq", "op_div_eq"),
            mutableListOf()
        )
        this.code = outer.code (null)
        this.mem = outer.mem()
    }

    fun Expr.up (f: (Expr)->Boolean): Expr? {
        val up = ups[this]
        return when {
            (up == null) -> null
            f(up) -> up
            else -> up.up(f)
        }
    }
    fun Expr.upBlock (): Expr.Block? {
        return this.up { it is Expr.Block } as Expr.Block?
    }
    fun Expr.upFunc (): Expr.Func? {
        return this.up { it is Expr.Func } as Expr.Func?
    }
    fun Expr.upFuncOrBlock (): Expr? {
        return this.up { it is Expr.Func || it is Expr.Block }
    }

    fun Expr.isDeclared (id: String): Boolean {
        val xblock = xblocks[this]!!
        val up = this.upFuncOrBlock()
        return (xblock.syms.contains(id) || (up!=null && up.isDeclared(id)))
    }
    fun Expr.assertIsNotDeclared (id: String, tk: Tk) {
        if (this.isDeclared(id)) {
            err(tk, "declaration error : variable \"$id\" is already declared")
        }
    }
    fun Expr.assertIsDeclared (id: String, tk: Tk) {
        if (!this.isDeclared(id)) {
             err(tk, "access error : variable \"$id\" is not declared")
        }
    }

    fun Expr.top (): String {
        return this.upFunc().let {
            when {
                (it == null) -> "NULL"
                (it.tk.str == "task") -> "(ceu_coro->Bcast.Coro.task)"
                else -> "ceu_func"
            }
        }
    }

    fun Expr.Block.toc (isptr: Boolean): String {
        return "ceu_mem->block_${this.n}".let {
            if (isptr) "(&($it))" else it
        }
    }
    fun Expr.Block.id2c (id: String): String {
        val top = this.top()
        fun Expr.aux (n: Int): String {
            val xblock = xblocks[this]!!
            val bup = this.upFuncOrBlock()
            val fup = this.upFunc()
            val ok = xblock.syms.contains(id)
            return when {
                (ok && this==outer) -> "(ceu_mem_${outer.n}->$id)"
                (ok && n==0) -> "(ceu_mem->$id)"
                (ok && n!=0) -> {
                    //println(id)
                    //println(this)
                    val blk = if (this is Expr.Func) this.n else fup!!.n
                    "(((CEU_Func_$blk*) $top ${"->up".repeat(n)}->mem)->$id)"
                }
                (this is Expr.Block) -> bup!!.aux(n)
                (this is Expr.Func) -> bup!!.aux(n+1)
                else -> TODO("bug found")
            }
        }
        return this.aux(0)
    }

    fun Expr.code(set: Pair<String, String>?): String {
        return when (this) {
            is Expr.Block -> {
                val bup = this.upBlock()
                val f_b = this.upFuncOrBlock()
                val depth = when {
                    (f_b == null) -> "(0 + 1)"
                    (f_b is Expr.Func) -> "(ceu_ret==NULL ? 1 : ceu_ret->depth + 1)"
                    else -> "(${bup!!.toc(false)}.depth + 1)"
                }
                if (this != outer) {
                    xblocks[this] = XBlock(mutableSetOf(), mutableListOf())
                }
                val es = this.es.mapIndexed { i, it ->
                    it.code(if (i == this.es.size - 1) set else null) + "\n"
                }.joinToString("")
                """
                { // BLOCK ${this.tk.dump()}
                    assert($depth <= UINT8_MAX);
                    ceu_mem->block_$n = (CEU_Block) { $depth, NULL, {NULL,NULL} };
                    ${if (this.upFuncOrBlock().let { it==null || it is Expr.Block || it.tk.str!="task" }) "" else "ceu_coro->Bcast.Coro.block = &ceu_mem->block_$n;"}
                    ${if (this.upBlock() != null) "" else "ceu_block_global = &ceu_mem->block_$n;"}
                    ${if (f_b==null || f_b is Expr.Func) "" else "ceu_mem->block_${bup!!.n}.bcast.block = &ceu_mem->block_$n;"}
                    do {
                        $es
                    } while (0);
                    { // DEFERS ${this.tk.dump()}
                        ${xblocks[this]!!.defers!!.reversed().joinToString("")}
                    }
                    ${if (f_b==null || f_b is Expr.Func) "" else "ceu_mem->block_${bup!!.n}.bcast.block = NULL;"}
                    ${if (this.upFuncOrBlock().let { it==null || it is Expr.Block || it.tk.str!="task" }) "" else "ceu_coro->Bcast.Coro.block = NULL;"}
                    ceu_block_free(&ceu_mem->block_$n);
                    if (ceu_throw != NULL) {
                        continue;   // escape to end of enclosing block
                    }
                }
                """
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                val bup = this.upBlock()!!
                val xup = xblocks[bup]!!
                bup.assertIsNotDeclared(id, this.tk)
                xup.syms.add(id)
                xup.syms.add("_${id}_")

                val (x,_x_) = Pair("(ceu_mem->$id)","(ceu_mem->_${id}_)")
                """
                // DCL ${this.tk.dump()}
                $x = (CEU_Value) { CEU_VALUE_NIL };
                $_x_ = ${bup.toc(true)};   // can't be static b/c recursion
                ${fset(this.tk, set, id)}                
                """
            }
            is Expr.Set -> {
                val (scp, dst) = when (this.dst) {
                    is Expr.Index -> Pair(
                        "ceu_mem->col_${this.dst.n}.Dyn->block",
                        "((CEU_Value*)ceu_mem->col_${this.dst.n}.Dyn->Tuple.mem)[(int) ceu_mem->idx_${this.dst.n}.Number]"
                    )

                    is Expr.Acc -> {
                        val id = this.dst.tk_.fromOp().noSpecial()
                        val bup = this.upBlock()!!
                        bup.assertIsDeclared(id, this.tk)
                        bup.assertIsDeclared("_${id}_", this.tk)
                        Pair(bup.id2c("_${id}_"), bup.id2c(id)) // x = src / block of _x_
                    }

                    else -> error("bug found")
                }
                """
                { // SET ${this.tk.dump()}
                    CEU_Value ceu_$n;
                    ${this.dst.code(null)}
                    ${this.src.code(Pair(scp, "ceu_$n"))}
                    $dst = ceu_$n;
                    ${fset(this.tk, set, "ceu_$n")}
                }
                """
            }
            is Expr.If -> """
                { // IF ${this.tk.dump()}
                    CEU_Value ceu_cnd_$n;
                    ${this.cnd.code(Pair(this.upBlock()!!.toc(true), "ceu_cnd_$n"))}
                    int nok = (ceu_cnd_$n.tag==CEU_VALUE_NIL || (ceu_cnd_$n.tag==CEU_VALUE_BOOL && !ceu_cnd_$n.Bool));
                    if (!nok) {
                        ${this.t.code(set)}
                    } else {
                        ${this.f.code(set)}
                    }
                }
                """
            is Expr.While -> """
                { // WHILE ${this.tk.dump()}
                    do {
                CEU_WHILE_$n:;
                        CEU_Value ceu_cnd_$n;
                        ${this.cnd.code(Pair(this.upBlock()!!.toc(true), "ceu_cnd_$n"))}
                        int nok = (ceu_cnd_$n.tag==CEU_VALUE_NIL || (ceu_cnd_$n.tag==CEU_VALUE_BOOL && !ceu_cnd_$n.Bool));
                        if (nok) {
                            continue; // escape enclosing block
                        }
                        ${this.body.code(null)}
                        goto CEU_WHILE_$n;
                    } while (0);
                    if (ceu_throw != NULL) {
                        continue; // escape enclosing block
                    }
                }
                """
            is Expr.Func -> {
                xblocks[this] = XBlock(this.args.let {
                    it.map { it.str } + it.map { "_${it.str}_" }
                }.toMutableSet(), null)
                fun xtask (v: String): String {
                    return if (this.isTask()) v else ""
                }
                fun xfunc (v: String): String {
                    return if (!this.isTask()) v else ""
                }
                val type = """ // TYPE ${this.tk.dump()}
                typedef struct {
                    void* ceu_up;
                    ${this.args.map {
                        """
                        CEU_Value ${it.str};
                        CEU_Block* _${it.str}_;
                        """
                    }.joinToString("")}
                    ${this.body.mem()}
                } CEU_Func_$n;
                """
                val func = """ // BODY ${this.tk.dump()}
                CEU_Value ceu_f_$n (
                    ${xfunc("CEU_Proto* ceu_func,")}
                    ${xtask("CEU_Dynamic* ceu_coro,")}
                    CEU_Block* ceu_ret,
                    int ceu_n,
                    CEU_Value* ceu_args[]
                ) {
                    ${xfunc("""
                        CEU_Func_$n _ceu_mem_;
                        CEU_Func_$n* ceu_mem = &_ceu_mem_;
                        ceu_func->mem = ceu_mem;
                    """)}
                    ${xtask("""
                        assert(ceu_coro->Bcast.Coro.status == CEU_CORO_STATUS_YIELDED);
                        ceu_coro->Bcast.Coro.status = CEU_CORO_STATUS_RESUMED;
                        CEU_Func_$n* ceu_mem = (CEU_Func_$n*) ceu_coro->Bcast.Coro.mem;
                        ceu_coro->Bcast.Coro.task->mem = ceu_mem;
                    """)}
                    CEU_Func_$n* ceu_mem_$n = ceu_mem;
                    CEU_Value ceu_$n = { CEU_VALUE_NIL };
                    """ +
                    """ // WHILE
                    do { // FUNC
                        ${xtask("""
                            switch (ceu_coro->Bcast.Coro.pc) {
                                case -1:
                                    assert(0 && "bug found");
                                    break;
                                case 0: {
                        """)}
                        { // ARGS
                            int ceu_i = 0;
                            ${this.args.map {
                                val id = it.str.noSpecial()
                                """
                                ceu_mem->_${id}_ = NULL; // TODO: create Block at Func top-level
                                if (ceu_i < ceu_n) {
                                    ceu_mem->$id = *ceu_args[ceu_i];
                                } else {
                                    ceu_mem->$id = (CEU_Value) { CEU_VALUE_NIL };
                                }
                                ceu_i++;
                                """
                            }.joinToString("")}
                        }
                        // BODY
                        ${this.body.code(Pair("ceu_ret", "ceu_$n"))}
                        ${xtask("}\n}\n")}
                    } while (0);
                    """ +
                    """
                    ${xtask("""
                        ceu_coro->Bcast.Coro.pc = -1;
                        ceu_coro->Bcast.Coro.status = CEU_CORO_STATUS_TERMINATED;
                    """)}
                    return ceu_$n;
                }
                """
                tops.add(Pair(type,func))
                """
                ${xfunc("""
                    static CEU_Proto ceu_func_$n;
                    ceu_func_$n = (CEU_Proto) { ${this.top()}, NULL, {.Func=ceu_f_$n} };
                    ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_FUNC, {.Proto=&ceu_func_$n} })")}
                """)}
                ${xtask("""
                    static CEU_Proto ceu_task_$n;
                    ceu_task_$n = (CEU_Proto) {
                        ${this.top()}, NULL, {
                            .Task = { ceu_f_$n, sizeof(CEU_Func_$n) }
                        }
                    };
                    ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_TASK, {.Proto=&ceu_task_$n} })")}
                """)}
                """
            }
            is Expr.Catch -> {
                val bup = this.upBlock()!!
                val scp = if (set != null) set.first else bup.toc(true)
                """
                { // CATCH ${this.tk.dump()}
                    ${this.catch.code(Pair(bup.toc(true), "ceu_mem->catch_$n"))}
                    if (ceu_mem->catch_$n.tag != CEU_VALUE_TAG) {
                        ceu_throw = &CEU_THROW_ERROR;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : catch error : expected tag", 256);
                        continue; // escape enclosing block
                    }
                    do {
                        ${this.body.code(set)}
                    } while (0);
                    if (ceu_throw != NULL) {          // pending throw
                        assert(ceu_throw->tag == CEU_VALUE_TAG);
                        if (ceu_throw->Tag == ceu_mem->catch_$n.Tag) { // CAUGHT: reset throw, set arg
                            ${fset(this.tk, set, "ceu_throw_arg")}
                            if (ceu_block_global != $scp) {
                                // assign ceu_throw_arg to set.first
                                if (ceu_throw_arg.tag >= CEU_VALUE_TUPLE) { // is dynamic
                                    assert(ceu_throw_arg.tag == CEU_VALUE_TUPLE && "bug found");
                                    ceu_block_move(ceu_throw_arg.Dyn, ceu_block_global, $scp);
                                }
                            }
                            ceu_throw = NULL;
                        } else {                                // UNCAUGHT: escape to outer
                            continue; // escape enclosing block;
                        }
                    }
                }
                """
            }
            is Expr.Throw -> """
                { // THROW ${this.tk.dump()}
                    static CEU_Value ceu_$n;    // static b/c may cross function call
                    ${this.ex.code(Pair(this.upBlock()!!.toc(true), "ceu_$n"))}
                    ceu_throw = &ceu_$n;
                    if (ceu_throw->tag == CEU_VALUE_TAG) {
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception", 256);
                    } else {
                        ceu_throw = &CEU_THROW_ERROR;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : expected tag", 256);
                    }
                    continue; // escape enclosing block;
                }
                """
            is Expr.Defer -> { xblocks[this.upBlock()!!]!!.defers!!.add(this.body.code(null)); "" }

            is Expr.Coro -> {
                val bupc = this.upBlock()!!.toc(true)
                val scp = if (set == null) bupc else set.first
                """
                { // CORO ${this.tk.dump()}
                    CEU_Value ceu_task_$n;
                    CEU_Value ceu_coro_$n;
                    ${this.task.code(Pair(bupc, "ceu_task_$n"))}
                    char* err = ceu_coro_coroutine(&ceu_coro_$n, &ceu_task_$n, $scp);
                    if (err != NULL) {
                        ceu_throw = &CEU_THROW_ERROR;
                        snprintf(ceu_throw_msg, 256, "${tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col}) : %s", err);
                        continue; // escape enclosing block;
                    }
                    ${fset(this.tk, set, "ceu_coro_$n")}
                }
                """
            }
            is Expr.Bcast -> {
                var bup = this.upBlock()!!
                while (true) {
                    val nxt = bup.upBlock()
                    if (nxt == null) {
                        break
                    } else {
                        bup = nxt
                    }
                }
                val bupc = bup.toc(true)
                """
                { // BCAST ${this.tk.dump()}
                    CEU_Value ceu_arg_$n;
                    ${this.arg.code(Pair(bupc, "ceu_arg_$n"))}
                    ceu_bcast_blocks($bupc, &ceu_arg_$n);
                }
                """
            }
            is Expr.Resume -> {
                val bupc = this.upBlock()!!.toc(true)
                val (sets,args) = this.call.args.let {
                    Pair(
                        it.mapIndexed { i,x -> x.code(Pair(bupc, "ceu_mem->arg_${i}_$n")) }.joinToString(""),
                        it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                    )
                }
                """
                { // RESUME ${this.tk.dump()}
                    { // SETS
                        $sets
                    }
                    CEU_Value ceu_coro_$n;
                    ${this.call.f.code(Pair(bupc, "ceu_coro_$n"))}
                    if (ceu_coro_$n.tag!=CEU_VALUE_CORO || ceu_coro_$n.Dyn->Bcast.Coro.status!=CEU_CORO_STATUS_YIELDED) {                
                        ceu_throw = &CEU_THROW_ERROR;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.call.f.tk.pos.lin}, col ${this.call.f.tk.pos.col}) : resume error : expected yielded task", 256);
                        continue; // escape enclosing block;
                    }
                    CEU_Value* ceu_args_$n[] = { $args };
                    CEU_Value ceu_ret_$n = ceu_coro_$n.Dyn->Bcast.Coro.task->Task.f(
                        ceu_coro_$n.Dyn,
                        ${if (set == null) bupc else set.first},
                        ${this.call.args.size},
                        ceu_args_$n
                    );
                    if (ceu_throw != NULL) {
                        continue; // escape enclosing block;
                    }
                    ${fset(this.tk, set, "ceu_ret_$n")}
                }
                """
            }
            is Expr.Yield -> """
                { // YIELD ${this.tk.dump()}
                    ${this.arg.code(Pair("ceu_ret","ceu_${this.upFunc()!!.n}"))}
                    ceu_coro->Bcast.Coro.pc = $n;      // next resume
                    ceu_coro->Bcast.Coro.status = CEU_CORO_STATUS_YIELDED;
                    return ceu_${this.upFunc()!!.n};
                case $n:                    // resume here
                    if (ceu_throw != NULL) {
                        continue; // escape enclosing block;
                    }
                    assert(ceu_n <= 1 && "bug found : not implemented : multiple arguments to resume");
                    ${fset(this.tk, set, "(*ceu_args[0])")}
                }
                """
            is Expr.Spawn -> {
                val bupc = this.upBlock()!!.toc(true)
                val scp = if (set == null) bupc else set.first
                val (sets,args) = this.call.args.let {
                    Pair(
                        it.mapIndexed { i,x -> x.code(Pair(bupc, "ceu_mem->arg_${i}_$n")) }.joinToString(""),
                        it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                    )
                }
                """
                { // SPAWN/CORO ${this.tk.dump()}
                    CEU_Value ceu_task_$n;
                    CEU_Value ceu_coro_$n;
                    ${this.call.f.code(Pair(bupc, "ceu_task_$n"))}
                    char* err = ceu_coro_coroutine(&ceu_coro_$n, &ceu_task_$n, $scp);
                    if (err != NULL) {
                        ceu_throw = &CEU_THROW_ERROR;
                        snprintf(ceu_throw_msg, 256, "${tk.pos.file} : (lin ${this.call.f.tk.pos.lin}, col ${this.call.f.tk.pos.col}) : %s", err);
                        continue; // escape enclosing block;
                    }
                    ${fset(this.tk, set, "ceu_coro_$n")}            
                // SPAWN/RESUME ${this.tk.dump()}
                    { // SETS
                        $sets
                    }
                    CEU_Value* ceu_args_$n[] = { $args };
                    ceu_coro_$n.Dyn->Bcast.Coro.task->Task.f(
                        ceu_coro_$n.Dyn,
                        ${if (set == null) bupc else set.first},
                        ${this.call.args.size},
                        ceu_args_$n
                    );
                    if (ceu_throw != NULL) {
                        continue; // escape enclosing block;
                    }
                }
                """
            }
            is Expr.Coros -> {
                val scp = if (set == null) this.upBlock()!!.toc(true) else set.first
                """
                { // COROS ${this.tk.dump()}
                    CEU_Dynamic* ceu_$n = malloc(sizeof(CEU_Dynamic));
                    assert(ceu_$n != NULL);
                    *ceu_$n = (CEU_Dynamic) {
                        CEU_VALUE_COROS, $scp->tofree, $scp, {
                            .Bcast = { NULL, {.Coros = {0, NULL}} }
                        }
                    };
                    ceu_bcast_enqueue($scp, ceu_$n);
                    $scp->tofree = ceu_$n;
                    ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_COROS, {.Dyn=ceu_$n} })")}
                }
                """
            }

            is Expr.Nat -> {
                val bup = this.upBlock()!!
                val (ids,body) = this.tk.str.drop(1).dropLast(1).let {
                    var ret = ""
                    var i = 0

                    var lin = 1
                    var col = 1
                    fun read (): Char {
                        //assert(i < it.length) { "bug found" }
                        if (i >= it.length) {
                            err(tk, "native error : (lin $lin, col $col) : unterminated token")
                        }
                        val x = it[i++]
                        if (x == '\n') {
                            lin++; col=0
                        } else {
                            col++
                        }
                        return x
                    }

                    val ids = mutableListOf<String>()
                    while (i < it.length) {
                        ret += if (it[i] != '$') read() else {
                            read()
                            val (l,c) = Pair(lin,col)
                            var id = ""
                            var x = read()
                            while (x.isLetterOrDigit() || x=='_') {
                                id += x
                                x = read()
                            }
                            if (id.length == 0) {
                                err(tk, "native error : (lin $l, col $c) : invalid identifier")
                            }
                            bup.assertIsDeclared(id, this.tk)
                            id = bup.id2c(id)
                            ids.add(id)
                            "($id.Number)$x"
                        }
                    }
                    Pair(ids,ret)
                }
                """
                { // NATIVE ${this.tk.dump()}
                    double ceu_f_$n (void) {
                        ${ids.map { "$it.tag = CEU_VALUE_NUMBER;\n" }.joinToString("") }
                        $body
                        return 0;
                    }
                    CEU_Value ceu_$n = { CEU_VALUE_NUMBER, {.Number=ceu_f_$n()} };
                    if (ceu_throw != NULL) {
                        continue; // escape enclosing block;
                    }
                    ${fset(this.tk, set, "ceu_$n")}
                }
                """
            }
            is Expr.Acc -> {
                val bup = this.upBlock()!!
                val id = this.tk_.fromOp().noSpecial()
                bup.assertIsDeclared(id, this.tk)
                "// ACC " + this.tk.dump() + "\n" + fset(this.tk, set, bup.id2c(id))
            }
            is Expr.Nil -> fset(this.tk, set, "((CEU_Value) { CEU_VALUE_NIL })")
            is Expr.Tag -> {
                val tag = this.tk.str.drop(1)
                if (!tags.contains(tag)) {
                    tags.add(tag)
                }
                fset(this.tk, set, "((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_$tag} })")
            }
            is Expr.Bool -> {
                fset(
                    this.tk,
                    set,
                    "((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} })"
                )
            }
            is Expr.Num -> fset(this.tk, set, "((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} })")
            is Expr.Tuple -> {
                assert(this.args.size <= 256) { "bug found" }
                val scp = if (set == null) this.upBlock()!!.toc(true) else set.first
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code(Pair(scp, "ceu_mem->arg_${i}_$n"))
                }.joinToString("")
                """
                { // TUPLE ${this.tk.dump()}
                    $args
                    CEU_Value ceu_sta_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_$n = malloc(sizeof(CEU_Dynamic) + ${this.args.size} * sizeof(CEU_Value));
                    assert(ceu_$n != NULL);
                    *ceu_$n = (CEU_Dynamic) { CEU_VALUE_TUPLE, $scp->tofree, $scp, {.Tuple={${this.args.size},{}}} };
                    memcpy(ceu_$n->Tuple.mem, ceu_sta_$n, ${this.args.size} * sizeof(CEU_Value));
                    $scp->tofree = ceu_$n;
                    ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_$n} })")}
                }
                """
            }
            is Expr.Index -> {
                val bupc = this.upBlock()!!.toc(true)
                """
                { // INDEX  ${this.tk.dump()}
                    { // COL
                        ${this.col.code(Pair(bupc, "ceu_mem->col_$n"))}
                        if (ceu_mem->col_$n.tag != CEU_VALUE_TUPLE) {                
                            ceu_throw = &CEU_THROW_ERROR;
                            strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : index error : expected tuple", 256);
                            continue; // escape enclosing block;
                        }
                    }
                    CEU_Value ceu_idx_$n;
                    { // IDX        
                        ${this.idx.code(Pair(bupc, "ceu_mem->idx_$n"))}
                        if (ceu_mem->idx_$n.tag != CEU_VALUE_NUMBER) {                
                            ceu_throw = &CEU_THROW_ERROR;
                            strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : expected number", 256);
                            continue; // escape enclosing block;
                        }
                    }
                    { // OK
                        if (ceu_mem->col_$n.Dyn->Tuple.n <= ceu_mem->idx_$n.Number) {                
                            ceu_throw = &CEU_THROW_ERROR;
                            strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : out of bounds", 256);
                            continue; // escape enclosing block
                        }    
                        ${fset(this.tk, set, "((CEU_Value*)ceu_mem->col_$n.Dyn->Tuple.mem)[(int) ceu_mem->idx_$n.Number]")}
                    }
                }
                """
            }
            is Expr.Call -> {
                val (sets,args) = this.args.let {
                    Pair (
                        it.mapIndexed { i,x -> x.code(Pair(this.upBlock()!!.toc(true), "ceu_mem->arg_${i}_$n")) }.joinToString(""),
                        it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                    )
                }
                """
                { // CALL ${this.tk.dump()}
                    { // SETS
                        $sets
                    }
                    CEU_Value ceu_f_$n;
                    ${this.f.code(Pair(this.upBlock()!!.toc(true), "ceu_f_$n"))}
                    if (ceu_f_$n.tag != CEU_VALUE_FUNC) {                
                        ceu_throw = &CEU_THROW_ERROR;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.f.tk.pos.lin}, col ${this.f.tk.pos.col}) : call error : expected function", 256);
                        continue; // escape enclosing block
                    }
                    CEU_Value* ceu_args_$n[] = { $args };
                    CEU_Value ceu_$n = ceu_f_$n.Proto->Func(
                        ceu_f_$n.Proto,
                        ${if (set == null) this.upBlock()!!.toc(true) else set.first},
                        ${this.args.size},
                        ceu_args_$n
                    );
                    if (ceu_throw != NULL) {
                        continue; // escape enclosing block
                    }
                    ${fset(this.tk, set, "ceu_$n")}
                }
                """
            }
        }
    }
}