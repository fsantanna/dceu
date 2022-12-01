fun Tk.dump (): String {
    return "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})\n"
}

class Coder (val outer: Expr.Block, val ups: Ups) {
    val tags = TAGS.toMutableList()
    val tops = mutableListOf<Pair<String,String>>()
    val mem: String = outer.mem()
    val code: String = outer.code(null, false, null)

    fun Expr.top (): String {
        return ups.func(this).let {
            when {
                (it == null) -> "NULL"
                (it.tk.str == "task") -> "(&ceu_coro->Bcast.Coro.task)"
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
                    "(((CEU_Func_$blk*) ($top) ${"->up".repeat(n)}->mem)->$id)"
                }
                (this is Expr.Block) -> bup!!.aux(n)
                (this is Expr.Func) -> bup!!.aux(n+1)
                else -> TODO("bug found")
            }
        }
        return this.aux(0)
    }

    // assrc_dst: calling expr is a source and here's its destination
    // assrc_hld: calling expr destination hold block is set, do not call ceu_block_set here, otherwise call it and hold in enclosing block
    // asdst_src: calling expr is a destination and here's its source
    fun Expr.code(assrc_dst: String?, assrc_hld: Boolean, asdst_src: String?): String {
        fun SET (v: String, bup: String): String {
            return """
            {
                CEU_Value ceu_tmp_$n = $v;
                ${when {
                    (assrc_dst == null) -> """ // nothing to set, hold in local block
                        assert(NULL == ceu_block_set($bup, &ceu_tmp_$n));
                        """
                    assrc_hld -> """ // do not set block yet
                        $assrc_dst = ceu_tmp_$n;
                        """
                    else -> """ // assign and set local block (nowhere else to hold)
                        assert(NULL == ceu_block_set($bup, &ceu_tmp_$n));
                        $assrc_dst = ceu_tmp_$n;
                        """
                }}
            }
            """
        }
        return when (this) {
            is Expr.Func -> {
                val isfunc = (this.tk.str == "func")
                val istask = (this.tk.str == "task")
                val type = """ // TYPE ${this.tk.dump()}
                typedef struct {
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
                    ${isfunc.cond{"CEU_Frame* ceu_func,"}}
                    ${istask.cond{"CEU_Dynamic* ceu_coro,"}}
                    int ceu_depth,
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
                        CEU_Func_$n* ceu_mem = (CEU_Func_$n*) ceu_coro->Bcast.Coro.__mem;
                        //ceu_coro->Bcast.Coro.task.mem = ceu_mem; // ceu_coro_create does this
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
                        ${this.body.code("ceu_$n", assrc_hld, null)}
                        ${istask.cond{"}\n}\n"}}
                    } while (0);
                    """ +
                        """ // TERMINATE
                    ${istask.cond{"""
                        ceu_coro->Bcast.Coro.pc = -1;
                        ceu_coro->Bcast.Coro.status = CEU_CORO_STATUS_TERMINATED;
                        if (ceu_coro->Bcast.Coro.coros != NULL) {
                            if (ceu_coro->Bcast.Coro.coros->Bcast.Coros.open == 0) {
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
                    static CEU_Frame ceu_func_$n;
                    ceu_func_$n = (CEU_Frame) { ${this.top()}, NULL, {.Func=ceu_f_$n} };
                    ${assrc_dst.cond { "$assrc_dst = ((CEU_Value) { CEU_VALUE_FUNC, {.Frame=&ceu_func_$n} });" }}
                """}}
                ${istask.cond{"""
                    static CEU_Frame ceu_task_$n;
                    ceu_task_$n = (CEU_Frame) {
                        ${this.top()}, NULL, {
                            .Task = { ceu_f_$n, sizeof(CEU_Func_$n) }
                        }
                    };
                    ${assrc_dst.cond { "$assrc_dst = ((CEU_Value) { CEU_VALUE_TASK, {.Frame=&ceu_task_$n} });" }}
                """}}
                """
            }
            is Expr.Block -> {
                val bup = ups.block(this)
                val f_b = ups.func_or_block(this)
                val depth = when {
                    (f_b == null) -> "(0 + 1)"
                    (f_b is Expr.Func) -> "(ceu_depth + 1)"
                    else -> "(${bup!!.toc(false)}.depth + 1)"
                }
                val es = this.es.mapIndexed { i, it ->
                    if (i == this.es.size-1) {
                        it.code(assrc_dst, assrc_hld, null) + assrc_hld.cond { """
                        // would fail later, but memory is reclaimed here, so need to check before return
                        if ($assrc_dst.tag>=CEU_VALUE_TUPLE && $assrc_dst.Dyn->hold!=NULL && $assrc_dst.Dyn->hold->depth>=$depth) {
                            // scope of dyn ret must still be NULL or at most outer depth
                            ceu_has_throw = 1;
                            ceu_err = &CEU_ERR_ERROR;
                            strncpy(ceu_err_error_msg, "${it.tk.pos.file} : (lin ${it.tk.pos.lin}, col ${it.tk.pos.col}) : return error : incompatible scopes", 256);
                            continue;   // escape to end of enclosing block
                        }                        
                        """}
                    } else {
                        it.code(null, false, null) + "\n"
                    }
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
                    // may yield in inner block, need to reset evt depth here
                    ceu_evt_block.depth = ceu_mem->block_$n.depth;
                }
                """
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                val (x,_x_) = Pair("(ceu_mem->$id)","(ceu_mem->_${id}_)")
                """
                // DCL ${this.tk.dump()}
                ${this.init.cond{"$x = (CEU_Value) { CEU_VALUE_NIL };"}}
                $_x_ = ${ups.block(this)!!.toc(true)};   // can't be static b/c recursion
                ${assrc_dst.cond { "$assrc_dst = $id;" }}
                """
            }
            is Expr.Set -> """
                { // SET ${this.tk.dump()}
                    ${this.src.code("ceu_mem->set_$n", true, null)}
                    ${this.dst.code(null, assrc_hld, "ceu_mem->set_$n")}
                    ${assrc_dst.cond { "$assrc_dst = ceu_mem->set_$n;" }}
                }
                """
            is Expr.If -> """
                { // IF ${this.tk.dump()}
                    CEU_Value ceu_cnd_$n;
                    ${this.cnd.code("ceu_cnd_$n", false, null)}
                    if (ceu_as_bool(&ceu_cnd_$n)) {
                        ${this.t.code(assrc_dst, assrc_hld, null)}
                    } else {
                        ${this.f.code(assrc_dst, assrc_hld, null)}
                    }
                }
                """
            is Expr.While -> """
                { // WHILE ${this.tk.dump()}
                CEU_WHILE_START_$n:;
                    CEU_Value ceu_cnd_$n;
                    ${this.cnd.code("ceu_cnd_$n", false, null)}
                    if (ceu_as_bool(&ceu_cnd_$n)) {
                        ${this.body.code(null, false, null)}
                        goto CEU_WHILE_START_$n;
                    }
                }
                """
            is Expr.Catch -> """
                { // CATCH ${this.tk.dump()}
                    do {
                        ${this.body.code(assrc_dst, assrc_hld, null)}
                    } while (0);
                    if (ceu_has_bcast>0 && ceu_evt==&CEU_EVT_CLEAR) {
                        // do not catch anything while clearing up
                        continue; // escape enclosing block;
                    }
                    if (ceu_has_throw_clear()) {
                        ceu_has_throw = 0;
                        //ceu_print1(ceu_err);
                        CEU_Value ceu_catch_$n;
                        ceu_err_block.depth = ${ups.block(this)!!.toc(true)}->depth + 1;
                        ${this.cnd.code("ceu_catch_$n", false, null)}
                        if (!ceu_as_bool(&ceu_catch_$n)) {
                            ceu_has_throw = 1; // UNCAUGHT: escape to outer
                            continue; // escape enclosing block;
                        }
                        ceu_err = &CEU_ERR_NIL;
                        ceu_block_free(&ceu_err_block);
                    }
                }
                """
            is Expr.Throw -> """
                { // THROW ${this.tk.dump()}
                    static CEU_Value ceu_ex_$n;    // static b/c may cross function call
                    ${this.ex.code("ceu_ex_$n", true, null)}
                    assert(NULL == ceu_block_set(&ceu_err_block, &ceu_ex_$n));
                    ceu_err = &ceu_ex_$n;
                    ceu_has_throw = 1;
                    strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception", 256);
                    continue; // escape enclosing block;
                }
                """
            is Expr.Defer -> { ups.xblocks[ups.block(this)!!]!!.defers!!.add(this.body.code(null, false, null)); "" }

            is Expr.Coros -> {
                """
                { // COROS ${this.tk.dump()}
                    ${this.max.cond { """
                        CEU_Value ceu_max_$n;
                        ${this.max!!.code("ceu_max_$n", false, null)}
                        if (ceu_max_$n.tag!=CEU_VALUE_NUMBER || ceu_max_$n.Number<=0) {                
                            ceu_has_throw = 1;
                            ceu_err = &CEU_ERR_ERROR;
                            strncpy(ceu_err_error_msg,
                                "${this.tk.pos.file} : (lin ${this.max.tk.pos.lin}, col ${this.max.tk.pos.col}) : coroutines error : expected positive number",
                                 256);
                            continue; // escape enclosing block;
                        }
                    """}}
                    CEU_Dynamic* ceu_$n = malloc(sizeof(CEU_Dynamic));
                    assert(ceu_$n != NULL);
                    *ceu_$n = (CEU_Dynamic) {
                        CEU_VALUE_COROS, NULL, NULL, {
                            .Bcast = { NULL, {.Coros = {${if (this.max==null) 0 else "ceu_max_$n.Number"}, 0, 0, NULL}} }
                        }
                    };
                    ${SET("((CEU_Value) { CEU_VALUE_COROS, {.Dyn=ceu_$n} })", ups.block(this)!!.toc(true))}
                }
                """
            }
            is Expr.Coro -> {
                """
                { // CORO ${this.tk.dump()}
                    CEU_Value ceu_task_$n;
                    CEU_Value ceu_coro_$n;
                    ${this.task.code("ceu_task_$n", false, null)}
                    char* ceu_err_$n = ceu_coro_create(&ceu_task_$n, &ceu_coro_$n);
                    if (ceu_err_$n != NULL) {
                        ceu_has_throw = 1;
                        ceu_err = &CEU_ERR_ERROR;
                        snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    ${SET("ceu_coro_$n", ups.block(this)!!.toc(true))}
                }
                """
            }
            is Expr.Spawn -> this.call.code(assrc_dst, assrc_hld, null)
            is Expr.Iter -> {
                val loc = this.loc.str
                """
                { // ITER ${this.tk.dump()}
                    ${this.coros.code("ceu_mem->coros_$n", false, null)}
                    if (ceu_mem->coros_$n.tag != CEU_VALUE_COROS) {                
                        ceu_has_throw = 1;
                        ceu_err = &CEU_ERR_ERROR;
                        strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.coros.tk.pos.lin}, col ${this.coros.tk.pos.col}) : while error : expected coroutines", 256);
                        continue; // escape enclosing block;
                    }
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.open++;
                    ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->coros_$n.Dyn->Bcast.Coros.first} };
                    do {
                CEU_ITER_$n:;
                        if (ceu_mem->$loc.Dyn == NULL) {
                            continue; // escape enclosing block
                        }
                        ceu_mem->hold_$n = ceu_mem->$loc.Dyn->hold; 
                        ceu_mem->$loc.Dyn->hold = ${this.body.toc(true)};
                        ${this.body.code(null, false, null)}
                        ceu_mem->$loc.Dyn->hold = ceu_mem->hold_$n; 
                        ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->$loc.Dyn->Bcast.next} };
                        goto CEU_ITER_$n;
                    } while (0);
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.open--;
                    if (ceu_mem->coros_$n.Dyn->Bcast.Coros.open == 0) {
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
                    CEU_Value ceu_evt_$n;
                    ${this.evt.code("ceu_evt_$n", true, null)}
                    char* ceu_err_$n = ceu_block_set(&ceu_evt_block, &ceu_evt_$n);
                    if (ceu_err_$n == NULL) {
                        ceu_err_$n = ceu_bcast_blocks((&ceu_mem_${outer.n}->block_${outer.n}), &ceu_evt_$n);
                    }
                    if (ceu_err_$n != NULL) {
                        ceu_has_throw = 1;
                        ceu_err = &CEU_ERR_ERROR;
                        snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                }
                """
            }
            is Expr.Yield -> """
                { // YIELD ${this.tk.dump()}
                    ${this.arg.code("ceu_${ups.func(this)!!.n}", false, null)}
                    ceu_coro->Bcast.Coro.pc = $n;      // next resume
                    ceu_coro->Bcast.Coro.status = CEU_CORO_STATUS_YIELDED;
                    return ceu_${ups.func(this)!!.n};
                case $n:                    // resume here
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    assert(ceu_n <= 1 && "bug found : not implemented : multiple arguments to resume");
                    ceu_evt_block.depth = ${ups.block(this)!!.toc(true)}->depth + 1;
                    ${assrc_dst.cond { "$assrc_dst = *ceu_args[0];" }}
                }
                """
            is Expr.Resume -> this.call.code(assrc_dst, assrc_hld, null)
            is Expr.Pub -> {
                val X: Int? = if (this.coro != null) null else {
                    var n = 0
                    var fup = ups.func(this)!!
                    while (fup.isFake) {
                        n++
                        fup = ups.func(fup)!!
                    }
                    n
                }
                """
                { // PUB
                    CEU_Dynamic* ceu_dyn_$n;
                    ${if (this.coro == null) {
                        """
                        char* ceu_ptr_$n = ((&ceu_coro->Bcast.Coro.task) ${"->up".repeat(X!!)}->mem);
                        ceu_dyn_$n = (CEU_Dynamic*) (ceu_ptr_$n - offsetof(struct CEU_Dynamic, Bcast.Coro.__mem));
                        """
                    } else { """
                        CEU_Value ceu_coro_$n;
                        ${this.coro.code("ceu_coro_$n", false, null)}
                        if (ceu_coro_$n.tag != CEU_VALUE_CORO) {                
                            ceu_has_throw = 1;
                            ceu_err = &CEU_ERR_ERROR;
                            strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : pub error : expected coroutine", 256);
                            continue; // escape enclosing block;
                        }
                        ceu_dyn_$n = ceu_coro_$n.Dyn;
                    """ }}
                    ${when {
                        (assrc_dst != null) -> """ // PUB - read
                            $assrc_dst = ceu_dyn_$n->Bcast.Coro.pub;
                            """
                        (asdst_src != null) -> """ // PUB - SET
                            char* ceu_err_$n = ceu_block_set(ceu_dyn_$n->hold, &$asdst_src);
                            if (ceu_err_$n != NULL) {
                                snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                continue;
                            }
                            ceu_dyn_$n->Bcast.Coro.pub = $asdst_src;
                            """
                        else -> "// PUB - useless"
                    }}
                }
                """
            }

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
                //{ // NATIVE ${this.tk.dump()} // (use comment b/c native may declare var to be used next)
                    ${if (this.tk_.tag == null) {
                        body
                    } else {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        """
                        CEU_Value ceu_$n = ((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} });
                        ${assrc_dst.cond { "$assrc_dst = ceu_$n;" }}
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
                when {
                    (assrc_dst !== null) -> """
                        // ACC ${this.tk.dump()}
                        $assrc_dst = ${bup.id2c(id)};
                        """
                    (asdst_src != null) -> {
                        ups.assertIsDeclared(bup, "_${id}_", this.tk)
                        """
                        { // ACC - SET
                            char* ceu_err_$n = ceu_block_set(${bup.id2c("_${id}_")}, &$asdst_src);
                            if (ceu_err_$n != NULL) {
                                snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                continue;
                            }
                            ${bup.id2c(id)} = $asdst_src;
                        }
                        """
                    }
                    else -> "// ACC - useless"
                }
            }
            is Expr.EvtErr -> {
                if (asdst_src == null) {
                    "$assrc_dst = *ceu_${this.tk.str};\n"
                } else {
                    """
                    { // EVT/ERR - SET
                        char* ceu_err_$n = ceu_block_set(&ceu_${this.tk.str}_blk, &$asdst_src);
                        if (ceu_err_$n != NULL) {
                            snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                            continue;
                        }
                        ceu_${this.tk.str} = &$asdst_src;
                    }
                    """
                }
            }
            is Expr.Nil -> assrc_dst.cond { "$assrc_dst = ((CEU_Value) { CEU_VALUE_NIL });\n" }
            is Expr.Tag -> {
                val tag = this.tk.str.drop(1)
                if (!tags.contains(tag)) {
                    tags.add(tag)
                }
                assrc_dst.cond { "$assrc_dst = ((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_$tag} });" }
            }
            is Expr.Bool -> assrc_dst.cond {
                 "$assrc_dst = ((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} });";
            }
            is Expr.Num -> assrc_dst.cond {
                "$assrc_dst = ((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} });";
            }
            is Expr.Tuple -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code("ceu_mem->arg_${i}_$n", true, null)
                }.joinToString("")
                """
                { // TUPLE ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_$n = ceu_tuple_create(${this.args.size}, ceu_args_$n);
                    assert(ceu_$n != NULL);
                    ${SET("((CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_$n} })", ups.block(this)!!.toc(true))}
                }
                """
            }
            is Expr.Dict -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.first.code ("ceu_mem->arg_${i}_a_$n", true, null)+
                    it.second.code("ceu_mem->arg_${i}_b_$n", true, null)
                }.joinToString("")
                """
                { // DICT ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}][2] = {
                        ${this.args.mapIndexed { i, _ -> "{ceu_mem->arg_${i}_a_$n,ceu_mem->arg_${i}_b_$n}" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_$n = ceu_dict_create(${this.args.size}, &ceu_args_$n);
                    assert(ceu_$n != NULL);
                    ${SET("((CEU_Value) { CEU_VALUE_DICT, {.Dyn=ceu_$n} })", ups.block(this)!!.toc(true))}
                }
                """
            }
            is Expr.Index -> {
                """
                { // INDEX  ${this.tk.dump()}
                    CEU_Value ceu_col_$n;
                    ${this.idx.code("ceu_mem->idx_$n", false, null)}
                    ${this.col.code("ceu_col_$n", false, null)}
                    char* ceu_err_$n = ceu_col_check(&ceu_col_$n, &ceu_mem->idx_$n);
                    ${asdst_src.cond { """
                        if (ceu_err_$n == NULL) {
                            ceu_err_$n = ceu_block_set(ceu_col_$n.Dyn->hold, &$asdst_src);
                        }
                    """}}
                    if (ceu_err_$n != NULL) {                
                        snprintf(ceu_err_error_msg, 256, "${this.col.tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    switch (ceu_col_$n.tag) { // OK
                        case CEU_VALUE_TUPLE:                
                            ${when {
                                (asdst_src != null) -> "ceu_col_$n.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number] = $asdst_src;"
                                (assrc_dst != null) -> "$assrc_dst = ceu_col_$n.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number];"
                                else -> "" //"ceu_col_$n.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number];"
                            }}
                            break;
                        case CEU_VALUE_DICT: {
                            int idx = ceu_dict_key_index(ceu_col_$n.Dyn, &ceu_mem->idx_$n);
                            ${when {
                                (asdst_src != null) -> """ // SET
                                    if (idx == -1) {
                                        idx = ceu_dict_empty_index(ceu_col_$n.Dyn);
                                        (*ceu_col_$n.Dyn->Dict.mem)[idx][0] = ceu_mem->idx_$n;
                                    }
                                    (*ceu_col_$n.Dyn->Dict.mem)[idx][1] = $asdst_src;
                                    """
                                (assrc_dst != null) -> "$assrc_dst = ((idx==-1) ? (CEU_Value) { CEU_VALUE_NIL } : (*ceu_col_$n.Dyn->Dict.mem)[idx][1]);"
                                else -> "" //"((idx==-1) ? (CEU_Value) { CEU_VALUE_NIL } : (*ceu_col_$n.Dyn->Dict.mem)[idx][1]);"
                            }}
                            break;
                        }
                    }
                }
                """
            }
            is Expr.Call -> {
                val up1 = ups.ups[this]
                val up2 = ups.ups[up1]
                val bup = ups.block(this)!!
                val resume = (if (up1 is Expr.Block && up1.isFake && up2 is Expr.Resume) up2 else null)
                val spawn  = (if (up1 is Expr.Block && up1.isFake && up2 is Expr.Spawn)  up2 else null)
                val iscall = (resume==null && spawn==null)
                val iscoros = (spawn?.coros != null)

                val (frame,dyn) = if (iscall) {
                    Pair("ceu_f_$n.Frame->Func", "ceu_f_$n.Frame")
                } else {
                    Pair("ceu_coro_$n.Dyn->Bcast.Coro.task.Task.f", "ceu_coro_$n.Dyn")
                }

                val (sets,args) = this.args.let {
                    Pair (
                        it.mapIndexed { i,x ->
                            x.code("ceu_mem->arg_${i}_$n", true, null) +
                            """
                            { // check scopes of args
                                char* ceu_err_$n = ceu_block_set(${bup.toc(true)}, &ceu_mem->arg_${i}_$n);
                                if (ceu_err_$n != NULL) {
                                    snprintf(ceu_err_error_msg, 256, "${x.tk.pos.file} : (lin ${x.tk.pos.lin}, col ${x.tk.pos.col}) : %s", ceu_err_$n);
                                    continue;
                                }
                            }
                            """
                        }.joinToString(""),
                        it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                    )
                }

                iscall.cond{"""
                { // CALL ${this.tk.dump()}
                    CEU_Value ceu_f_$n;
                    ${this.f.code("ceu_f_$n", false, null)}
                    char* ceu_err_$n = NULL;
                    if (ceu_f_$n.tag != CEU_VALUE_FUNC) {
                        ceu_err_$n = "call error : expected function";
                    }
                """} +
                spawn.cond{"""
                { // SPAWN/CORO ${this.tk.dump()}
                    ${iscoros.cond{spawn!!.coros!!.code("ceu_mem->coros_${spawn.n}", false, null)}}
                    CEU_Value ceu_task_$n;
                    CEU_Value ceu_coro_$n;
                    ${this.f.code("ceu_task_$n", false, null)}
                    ${iscoros.cond { "CEU_Value ceu_ok_$n = { CEU_VALUE_BOOL, {.Bool=1} };" }}
                    char* ceu_err_$n = ${if (!iscoros) {
                        """
                        ceu_coro_create(&ceu_task_$n, &ceu_coro_$n);
                        ${SET("ceu_coro_$n", ups.block(ups.block(this)!!)!!.toc(true))}
                        """
                    } else {
                        """
                        ceu_coros_create(&ceu_ok_$n.Bool, ceu_mem->coros_${spawn!!.n}.Dyn, &ceu_task_$n, &ceu_coro_$n);
                        ${assrc_dst.cond { "$assrc_dst = ceu_ok_$n;" }}
                        if (ceu_ok_$n.Bool) {
                            // call task only if ok
                        //} // closes below
                        """
                    }}
                """} +
                resume.cond{"""
                { // RESUME ${this.tk.dump()}
                    CEU_Value ceu_coro_$n;
                    ${this.f.code("ceu_coro_$n", false, null)}
                    char* ceu_err_$n = NULL;
                    if (ceu_coro_$n.tag!=CEU_VALUE_CORO || ceu_coro_$n.Dyn->Bcast.Coro.status!=CEU_CORO_STATUS_YIELDED) {                
                        ceu_err_$n = "resume error : expected yielded task";
                    }
                """} +
                """
                    if (ceu_err_$n != NULL) {
                        ceu_has_throw = 1;
                        ceu_err = &CEU_ERR_ERROR;
                        snprintf(ceu_err_error_msg, 256, "${this.f.tk.pos.file} : (lin ${this.f.tk.pos.lin}, col ${this.f.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    { // SETS
                        $sets
                    }
                    CEU_Value* ceu_args_$n[] = { $args };
                    ${iscall.cond { "CEU_Value ceu_$n = " }}
                    ${resume.cond { "CEU_Value ceu_$n = " }}
                    $frame(
                        $dyn,
                        ${bup.toc(false)}.depth,
                        ${this.args.size},
                        ceu_args_$n
                    );
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    ${iscall.cond{assrc_dst.cond { "$assrc_dst = ceu_$n;" }}}
                    ${iscoros.cond{"}"}}
                    ${resume.cond{assrc_dst.cond { "$assrc_dst = ceu_$n;" }}}
                }
                """
            }

            is Expr.XSeq -> error("bug found")
        }
    }
}
