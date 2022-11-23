// block: String -> current enclosing block for normal allocation (if null, free on enclosing, but hold refuses any assignment)
// ret: Pair<block,var> -> enclosing assignment with destination holding block and variable
// hold: where to hold the assignment (scope block & variable name)
fun fset(tk: Tk, hold: Pair<String, String>?, src: String): String {
    return hold.cond{"""
        if ($src.tag >= CEU_VALUE_TUPLE) { // any Dyn
            if ($src.Dyn->hold->depth > ${hold!!.first}->depth) {
                ceu_has_throw = 1;
                ceu_err = &CEU_ERR_ERROR;
                strncpy(ceu_err_error_msg, "${tk.pos.file} : (lin ${tk.pos.lin}, col ${tk.pos.col}) : set error : incompatible scopes", 256);
                continue;
            }
        }
        ${hold.second} = $src;
    """}
}

fun Tk.dump (): String {
    return "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})\n"
}

class Coder (val outer: Expr.Block, val ups: Ups) {
    val tags = TAGS.toMutableList()
    val tops = mutableListOf<Pair<String,String>>()
    val mem: String = outer.mem()
    val code: String = outer.code(null)

    fun Expr.hld_or_up (hold: Pair<String, String>?): String {
        return hold?.first ?: ups.block(this)!!.toc(true)
    }

    fun Expr.top (): String {
        return ups.func(this).let {
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
            val xblock = ups.xblocks[this]!!
            val bup = ups.func_or_block(this)
            val fup = ups.func(this)
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
        return when {
            (id == "err") -> "(*ceu_err)"
            (id == "evt") -> "(*ceu_evt)"
            else -> this.aux(0)
        }
    }

    fun Expr.code(hold: Pair<String, String>?): String {
        return when (this) {
            is Expr.Block -> {
                val bup = ups.block(this)
                val f_b = ups.func_or_block(this)
                val depth = when {
                    (f_b == null) -> "(0 + 1)"
                    (f_b is Expr.Func) -> "(ceu_ret==NULL ? 1 : ceu_ret->depth + 1)"
                    else -> "(${bup!!.toc(false)}.depth + 1)"
                }
                val es = this.es.mapIndexed { i, it ->
                    it.code(if (i == this.es.size - 1) hold else null) + "\n"
                }.joinToString("")
                """
                { // BLOCK ${this.tk.dump()}
                    assert($depth <= UINT8_MAX);
                    ceu_mem->block_$n = (CEU_Block) { $depth, NULL, {NULL,NULL} };
                    ${ups.func_or_block(this).let { it!=null && it !is Expr.Block && it.tk.str=="task" }.cond {
                        "ceu_coro->Bcast.Coro.block = &ceu_mem->block_$n;"}
                    }
                    ${(f_b!=null && f_b !is Expr.Func).cond {
                        "ceu_mem->block_${bup!!.n}.bcast.block = &ceu_mem->block_$n;"}
                    }
                    do {
                        $es
                    } while (0);
                    ceu_bcast_blocks(&ceu_mem->block_$n, &CEU_EVT_CLEAR);
                    { // DEFERS ${this.tk.dump()}
                        ${ups.xblocks[this]!!.defers!!.reversed().joinToString("")}
                    }
                    ${(f_b!=null && f_b !is Expr.Func).cond{"ceu_mem->block_${bup!!.n}.bcast.block = NULL;"}}
                    ${ups.func_or_block(this).let { it!=null && it !is Expr.Block && it.tk.str=="task" }.cond{"ceu_coro->Bcast.Coro.block = NULL;"}}
                    ceu_block_free(&ceu_mem->block_$n);
                    if (ceu_has_throw_clear()) {
                        continue;   // escape to end of enclosing block
                    }
                }
                """
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                val bup = ups.block(this)!!
                val (x,_x_) = Pair("(ceu_mem->$id)","(ceu_mem->_${id}_)")
                """
                // DCL ${this.tk.dump()}
                ${this.init.cond{"$x = (CEU_Value) { CEU_VALUE_NIL };"}}
                $_x_ = ${bup.toc(true)};   // can't be static b/c recursion
                ${fset(this.tk, hold, id)}                
                """
            }
            is Expr.Set -> {
                val col = "ceu_mem->col_${this.dst.n}"
                val idx = "ceu_mem->idx_${this.dst.n}"
                val (hldSrc, dst) = when (this.dst) {
                    is Expr.Index -> Pair(
                        "$col.Dyn->hold",
                        """
                        switch ($col.tag) { // OK
                            case CEU_VALUE_TUPLE:                
                                $col.Dyn->Tuple.mem[(int) $idx.Number] = ceu_$n;
                                break;
                            case CEU_VALUE_DICT: {
                                int idx = ceu_dict_key_index($col.Dyn, &$idx);
                                if (idx == -1) {
                                    idx = ceu_dict_empty_index($col.Dyn);
                                    (*$col.Dyn->Dict.mem)[idx][0] = $idx;
                                }
                                (*$col.Dyn->Dict.mem)[idx][1] = ceu_$n;
                                break;
                            }
                        }
                        """
                    )
                    is Expr.Acc -> {
                        val id = this.dst.tk_.fromOp().noSpecial()
                        val bup = ups.block(this)!!
                        ups.assertIsDeclared(bup, id, this.tk)
                        ups.assertIsDeclared(bup, "_${id}_", this.tk)
                        Pair ( // x = src / block of _x_
                            bup.id2c("_${id}_"),
                            "${bup.id2c(id)} = ceu_$n;"
                        )
                    }
                    else -> error("bug found")
                }
                """
                { // SET ${this.tk.dump()}
                    CEU_Value ceu_$n;
                    ${this.dst.code(null)}
                    ${this.src.code(Pair(hldSrc, "ceu_$n"))}
                    $dst
                    ${fset(this.tk, hold, "ceu_$n")}
                }
                """
            }
            is Expr.If -> """
                { // IF ${this.tk.dump()}
                    CEU_Value ceu_cnd_$n;
                    ${this.cnd.code(Pair(ups.block(this)!!.toc(true), "ceu_cnd_$n"))}
                    if (ceu_as_bool(&ceu_cnd_$n)) {
                        ${this.t.code(hold)}
                    } else {
                        ${this.f.code(hold)}
                    }
                }
                """
            is Expr.While -> """
                { // WHILE ${this.tk.dump()}
                CEU_WHILE_START_$n:;
                    // may yield in inner block, need to reset evt depth here
                    ceu_evt_block.depth = ${ups.block(this)!!.toc(true)}->depth + 1;
                    CEU_Value ceu_cnd_$n;
                    ${this.cnd.code(Pair(ups.block(this)!!.toc(true), "ceu_cnd_$n"))}
                    if (ceu_as_bool(&ceu_cnd_$n)) {
                        ${this.body.code(null)}
                        goto CEU_WHILE_START_$n;
                    }
                }
                """
            is Expr.Func -> {
                val isfunc = (this.tk.str == "func")
                val istask = (this.tk.str == "task")
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
                    ${isfunc.cond{"CEU_Proto* ceu_func,"}}
                    ${istask.cond{"CEU_Dynamic* ceu_coro,"}}
                    CEU_Block* ceu_ret,
                    int ceu_n,
                    CEU_Value* ceu_args[]
                ) {
                    ${isfunc.cond{"""
                        CEU_Func_$n _ceu_mem_;
                        CEU_Func_$n* ceu_mem = &_ceu_mem_;
                        ceu_func->mem = ceu_mem;
                    """}}
                    ${istask.cond{"""
                        assert(ceu_coro->Bcast.Coro.status == CEU_CORO_STATUS_YIELDED);
                        ceu_coro->Bcast.Coro.status = CEU_CORO_STATUS_RESUMED;
                        CEU_Func_$n* ceu_mem = (CEU_Func_$n*) ceu_coro->Bcast.Coro.mem;
                        ceu_coro->Bcast.Coro.task->mem = ceu_mem;
                    """}}
                    CEU_Func_$n* ceu_mem_$n = ceu_mem;
                    CEU_Value ceu_$n = { CEU_VALUE_NIL };
                    """ +
                    """ // WHILE
                    do { // FUNC
                        ${istask.cond{"""
                            switch (ceu_coro->Bcast.Coro.pc) {
                                case -1:
                                    assert(0 && "bug found");
                                    break;
                                case 0: {
                                    if (ceu_has_throw_clear()) { // started with BCAST-CLEAR
                                        continue; // from BCAST-CLEAR: escape enclosing block
                                    }
                                    int ceu_depth = (ceu_ret==NULL ? 1 : ceu_ret->depth + 1);
                                    ceu_evt_block.depth = ceu_depth + 1;  // no block depth yet
                        """}}
                        { // ARGS
                            int ceu_i = 0;
                            ${this.args.map {
                                val id = it.str.noSpecial()
                                """
                                ceu_mem->_${id}_ = ${this.body.toc(true)};
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
                        ${istask.cond{"}\n}\n"}}
                    } while (0);
                    """ +
                    """ // TERMINATE
                    ${istask.cond{"""
                        ceu_coro->Bcast.Coro.pc = -1;
                        ceu_coro->Bcast.Coro.status = CEU_CORO_STATUS_TERMINATED;
                        if (ceu_coro->Bcast.Coro.coros != NULL) {
                            if (ceu_coro->Bcast.Coro.coros->Bcast.Coros.n == 0) {
                                ceu_coros_destroy(ceu_coro->Bcast.Coro.coros, ceu_coro);
                            }
                        }
                    """}}
                    return ceu_$n;
                }
                """
                tops.add(Pair(type,func))
                """ // STATIC
                ${isfunc.cond{"""
                    static CEU_Proto ceu_func_$n;
                    ceu_func_$n = (CEU_Proto) { ${this.top()}, NULL, {.Func=ceu_f_$n} };
                    ${fset(this.tk, hold, "((CEU_Value) { CEU_VALUE_FUNC, {.Proto=&ceu_func_$n} })")}
                """}}
                ${istask.cond{"""
                    static CEU_Proto ceu_task_$n;
                    ceu_task_$n = (CEU_Proto) {
                        ${this.top()}, NULL, {
                            .Task = { ceu_f_$n, sizeof(CEU_Func_$n) }
                        }
                    };
                    ${fset(this.tk, hold, "((CEU_Value) { CEU_VALUE_TASK, {.Proto=&ceu_task_$n} })")}
                """}}
                """
            }
            is Expr.Catch -> {
                val bupc = ups.block(this)!!.toc(true)
                """
                { // CATCH ${this.tk.dump()}
                    do {
                        ${this.body.code(hold)}
                    } while (0);
                    if (ceu_has_throw_clear()) {
                        ceu_has_throw = 0;
                        //ceu_print1(ceu_err);
                        CEU_Value ceu_catch_$n;
                        ceu_err_block.depth = $bupc->depth + 1;
                        ${this.cnd.code(Pair(bupc, "ceu_catch_$n"))}
                        if (!ceu_as_bool(&ceu_catch_$n)) {
                            ceu_has_throw = 1; // UNCAUGHT: escape to outer
                            continue; // escape enclosing block;
                        }
                        ceu_err = &CEU_ERR_NIL;
                        ceu_block_free(&ceu_err_block);
                    }
                }
                """
            }
            is Expr.Throw -> """
                { // THROW ${this.tk.dump()}
                    static CEU_Value ceu_$n;    // static b/c may cross function call
                    ${this.ex.code(Pair("(&ceu_err_block)", "ceu_$n"))}
                    ceu_err = &ceu_$n;
                    ceu_has_throw = 1;
                    strncpy(ceu_err_error_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception", 256);
                    continue; // escape enclosing block;
                }
                """
            is Expr.Defer -> { ups.xblocks[ups.block(this)!!]!!.defers!!.add(this.body.code(null)); "" }

            is Expr.Coros -> {
                val hld = this.hld_or_up(hold)
                """
                { // COROS ${this.tk.dump()}
                    CEU_Dynamic* ceu_$n = malloc(sizeof(CEU_Dynamic));
                    assert(ceu_$n != NULL);
                    *ceu_$n = (CEU_Dynamic) {
                        CEU_VALUE_COROS, $hld->tofree, $hld, {
                            .Bcast = { NULL, {.Coros = {0, NULL}} }
                        }
                    };
                    ceu_bcast_enqueue(&$hld->bcast.dyn, ceu_$n);
                    $hld->tofree = ceu_$n;
                    ${fset(this.tk, hold, "((CEU_Value) { CEU_VALUE_COROS, {.Dyn=ceu_$n} })")}
                }
                """
            }
            is Expr.Coro -> {
                val bupc = ups.block(this)!!.toc(true)
                val hld = this.hld_or_up(hold)
                """
                { // CORO ${this.tk.dump()}
                    CEU_Value ceu_task_$n;
                    CEU_Value ceu_coro_$n;
                    ${this.task.code(Pair(bupc, "ceu_task_$n"))}
                    char* err = ceu_coro_create($hld, &ceu_task_$n, &ceu_coro_$n);
                    if (err != NULL) {
                        ceu_has_throw = 1;
                        ceu_err = &CEU_ERR_ERROR;
                        snprintf(ceu_err_error_msg, 256, "${tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col}) : %s", err);
                        continue; // escape enclosing block;
                    }
                    ${fset(this.tk, hold, "ceu_coro_$n")}
                }
                """
            }
            is Expr.Spawn -> this.call.code(hold)
            is Expr.Iter -> {
                val bupc = ups.block(this)!!.toc(true)
                val loc = this.loc.str
                """
                { // ITER ${this.tk.dump()}
                    ${this.coros.code(Pair(bupc, "ceu_mem->coros_$n"))}
                    if (ceu_mem->coros_$n.tag != CEU_VALUE_COROS) {                
                        ceu_has_throw = 1;
                        ceu_err = &CEU_ERR_ERROR;
                        strncpy(ceu_err_error_msg, "${tk.pos.file} : (lin ${this.coros.tk.pos.lin}, col ${this.coros.tk.pos.col}) : while error : expected coroutines", 256);
                        continue; // escape enclosing block;
                    }
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.n++;
                    ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->coros_$n.Dyn->Bcast.Coros.first} };
                    do {
                CEU_ITER_$n:;
                        if (ceu_mem->$loc.Dyn == NULL) {
                            continue; // escape enclosing block
                        }
                        ceu_mem->hold_$n = ceu_mem->$loc.Dyn->hold; 
                        ceu_mem->$loc.Dyn->hold = ${this.body.toc(true)};
                        ${this.body.code(null)}
                        ceu_mem->$loc.Dyn->hold = ceu_mem->hold_$n; 
                        ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->$loc.Dyn->Bcast.next} };
                        goto CEU_ITER_$n;
                    } while (0);
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.n--;
                    if (ceu_mem->coros_$n.Dyn->Bcast.Coros.n == 0) {
                        ceu_coros_cleanup(ceu_mem->coros_$n.Dyn);
                    }
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                }
                """
            }
            is Expr.Bcast -> {
                """
                { // BCAST ${this.tk.dump()}
                    CEU_Value ceu_$n;
                    ${this.evt.code(Pair("(&ceu_evt_block)", "ceu_$n"))}
                    ceu_bcast_blocks((&ceu_mem_${outer.n}->block_${outer.n}), &ceu_$n);
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                }
                """
            }
            is Expr.Yield -> """
                { // YIELD ${this.tk.dump()}
                    ${this.arg.code(Pair("ceu_ret","ceu_${ups.func(this)!!.n}"))}
                    ceu_coro->Bcast.Coro.pc = $n;      // next resume
                    ceu_coro->Bcast.Coro.status = CEU_CORO_STATUS_YIELDED;
                    return ceu_${ups.func(this)!!.n};
                case $n:                    // resume here
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    assert(ceu_n <= 1 && "bug found : not implemented : multiple arguments to resume");
                    ceu_evt_block.depth = ${ups.block(this)!!.toc(true)}->depth + 1;
                    ${fset(this.tk, hold, "(*ceu_args[0])")}
                }
                """
            is Expr.Resume -> this.call.code(hold)

            is Expr.Nat -> {
                val bup = ups.block(this)!!
                val body = this.tk.str.let {
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
                            ups.assertIsDeclared(bup, id, this.tk)
                            id = bup.id2c(id)
                            "($id)$x"
                        }
                    }
                    ret
                }
                """
                //{ // NATIVE ${this.tk.dump()}
                    ${if (this.tk_.tag == null) {
                        body
                    } else {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        """
                        CEU_Value ceu_$n = ((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} });
                        ${fset(this.tk, hold, "ceu_$n")}
                        """
                    }}
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block;
                    }
                //}
                """
            }
            is Expr.Acc -> {
                val bup = ups.block(this)!!
                val id = this.tk_.fromOp().noSpecial()
                ups.assertIsDeclared(bup, id, this.tk)
                "// ACC " + this.tk.dump() + "\n" + fset(this.tk, hold, bup.id2c(id))
            }
            is Expr.Nil -> fset(this.tk, hold, "((CEU_Value) { CEU_VALUE_NIL })")
            is Expr.Tag -> {
                val tag = this.tk.str.drop(1)
                if (!tags.contains(tag)) {
                    tags.add(tag)
                }
                fset(this.tk, hold, "((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_$tag} })")
            }
            is Expr.Bool -> {
                fset(
                    this.tk,
                    hold,
                    "((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} })"
                )
            }
            is Expr.Num -> fset(this.tk, hold, "((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} })")
            is Expr.Tuple -> {
                val hld = this.hld_or_up(hold)
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code(Pair(hld, "ceu_mem->arg_${i}_$n"))
                }.joinToString("")
                """
                { // TUPLE ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_$n = ceu_tuple_create($hld, ${this.args.size}, ceu_args_$n);
                    assert(ceu_$n != NULL);
                    ${fset(this.tk, hold, "((CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_$n} })")}
                }
                """
            }
            is Expr.Dict -> {
                val hld = this.hld_or_up(hold)
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.first.code (Pair(hld, "ceu_mem->arg_${i}_a_$n"))+
                    it.second.code(Pair(hld, "ceu_mem->arg_${i}_b_$n"))
                }.joinToString("")
                """
                { // DICT ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}][2] = {
                        ${this.args.mapIndexed { i, _ -> "{ceu_mem->arg_${i}_a_$n,ceu_mem->arg_${i}_b_$n}" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_$n = ceu_dict_create($hld, ${this.args.size}, &ceu_args_$n);
                    assert(ceu_$n != NULL);
                    ${fset(this.tk, hold, "((CEU_Value) { CEU_VALUE_DICT, {.Dyn=ceu_$n} })")}
                }
                """
            }
            is Expr.Index -> {
                val bupc = ups.block(this)!!.toc(true)
                """
                { // INDEX  ${this.tk.dump()}
                    { // COL
                        ${this.col.code(Pair(bupc, "ceu_mem->col_$n"))}
                        if (ceu_mem->col_$n.tag!=CEU_VALUE_TUPLE && ceu_mem->col_$n.tag!=CEU_VALUE_DICT) {                
                            ceu_has_throw = 1;
                            ceu_err = &CEU_ERR_ERROR;
                            strncpy(ceu_err_error_msg, "${tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : index error : expected collection", 256);
                            continue; // escape enclosing block;
                        }
                    }
                    CEU_Value ceu_idx_$n;
                    { // IDX        
                        ${this.idx.code(Pair(bupc, "ceu_mem->idx_$n"))}
                        if (ceu_mem->col_$n.tag == CEU_VALUE_DICT) {
                            // ok
                        } else if (ceu_mem->idx_$n.tag != CEU_VALUE_NUMBER) {                
                            ceu_has_throw = 1;
                            ceu_err = &CEU_ERR_ERROR;
                            strncpy(ceu_err_error_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : expected number", 256);
                            continue; // escape enclosing block;
                        }
                    }
                    switch (ceu_mem->col_$n.tag) { // OK
                        case CEU_VALUE_TUPLE:                
                            if (ceu_mem->col_$n.Dyn->Tuple.n <= ceu_mem->idx_$n.Number) {                
                                ceu_has_throw = 1;
                                ceu_err = &CEU_ERR_ERROR;
                                strncpy(ceu_err_error_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : out of bounds", 256);
                                continue; // escape enclosing block
                            }    
                            ${fset(this.tk, hold, "ceu_mem->col_$n.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number]")}
                            break;
                        case CEU_VALUE_DICT: {
                            int idx = ceu_dict_key_index(ceu_mem->col_$n.Dyn, &ceu_mem->idx_$n);
                            ${fset(this.tk, hold, "((idx==-1) ? (CEU_Value) { CEU_VALUE_NIL } : (*ceu_mem->col_$n.Dyn->Dict.mem)[idx][1])")}
                            break;
                        }
                    }
                }
                """
            }
            is Expr.Call -> {
                val up1 = ups.ups[this]
                val up2 = ups.ups[up1]
                val resume = (if (up1 is Expr.Block && up1.isFake && up2 is Expr.Resume) up2 else null)
                val spawn  = (if (up1 is Expr.Block && up1.isFake && up2 is Expr.Spawn)  up2 else null)
                val iscall = (resume==null && spawn==null)

                val (bupc,bupupc) = ups.block(this)!!.let {
                    Pair(it.toc(true), ups.block(it)!!.toc(true))
                }
                val uphld = this.hld_or_up(hold)
                val upuphld = ups.block(this)!!.hld_or_up(hold)
                val (f,dyn) = if (iscall) {
                    Pair("ceu_f_$n.Proto->Func", "ceu_f_$n.Proto")
                } else {
                    Pair("ceu_coro_$n.Dyn->Bcast.Coro.task->Task.f", "ceu_coro_$n.Dyn")
                }

                val (sets,args) = this.args.let {
                    Pair (
                        it.mapIndexed { i,x -> x.code(Pair(ups.block(this)!!.toc(true), "ceu_mem->arg_${i}_$n")) }.joinToString(""),
                        it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                    )
                }

                iscall.cond{"""
                { // CALL ${this.tk.dump()}
                    CEU_Value ceu_f_$n;
                    ${this.f.code(Pair(ups.block(this)!!.toc(true), "ceu_f_$n"))}
                    char* ceu_err_$n = NULL;
                    if (ceu_f_$n.tag != CEU_VALUE_FUNC) {
                        ceu_err_$n = "call error : expected function";
                    }
                """} +
                spawn.cond{"""
                { // SPAWN/CORO ${this.tk.dump()}
                    ${spawn!!.coros.cond{spawn.coros!!.code(Pair(bupupc, "ceu_mem->coros_${spawn.n}"))}}
                    CEU_Value ceu_task_$n;
                    CEU_Value ceu_coro_$n;
                    ${this.f.code(Pair(bupupc, "ceu_task_$n"))}
                    char* ceu_err_$n = ${if (spawn.coros == null) {
                        "ceu_coro_create($upuphld, &ceu_task_$n, &ceu_coro_$n);"
                    } else {
                        "ceu_coros_create(ceu_mem->coros_${spawn.n}.Dyn, &ceu_task_$n, &ceu_coro_$n);"
                    }}
                    ${fset(this.tk, hold, "ceu_coro_$n")}            
                """} +
                resume.cond{"""
                { // RESUME ${this.tk.dump()}
                    CEU_Value ceu_coro_$n;
                    ${this.f.code(Pair(bupupc, "ceu_coro_$n"))}
                    char* ceu_err_$n = NULL;
                    if (ceu_coro_$n.tag!=CEU_VALUE_CORO || ceu_coro_$n.Dyn->Bcast.Coro.status!=CEU_CORO_STATUS_YIELDED) {                
                        ceu_err_$n = "resume error : expected yielded task";
                    }
                """} +
                """
                    if (ceu_err_$n != NULL) {
                        ceu_has_throw = 1;
                        ceu_err = &CEU_ERR_ERROR;
                        snprintf(ceu_err_error_msg, 256, "${tk.pos.file} : (lin ${this.f.tk.pos.lin}, col ${this.f.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    { // SETS
                        $sets
                    }
                    CEU_Value* ceu_args_$n[] = { $args };
                    ${iscall.cond { "CEU_Value ceu_$n = " }}
                    ${resume.cond { "CEU_Value ceu_$n = " }}
                    $f(
                        $dyn,
                        $uphld,
                        ${this.args.size},
                        ceu_args_$n
                    );
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    ${iscall.cond{fset(this.tk, hold, "ceu_$n")}}
                    ${resume.cond{fset(resume!!.tk, hold, "ceu_$n")}}
                }
                """
            }

            is Expr.XSeq -> error("bug found")
        }
    }
}
