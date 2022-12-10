fun Tk.dump (): String {
    return "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})\n"
}

class Coder (val outer: Expr.Block, val ups: Ups) {
    val tags = TAGS.toMutableList()
    val tops = mutableListOf<Pair<String,String>>()
    val mem: String = outer.mem()
    val code: String = outer.code()

    fun Expr.Block.toc (isptr: Boolean): String {
        return "ceu_mem->block_${this.n}".let {
            if (isptr) "(&($it))" else it
        }
    }
    fun Expr.Block.id2c (id: String): String {
        fun Expr.aux (n: Int): String {
            val xblock = ups.xblocks[this]!!
            val bup = ups.proto_or_block(this)
            val fup = ups.func(this)
            val ok = xblock.syms.contains(id)
            return when {
                (ok && this==outer) -> "(ceu_mem_${outer.n}->$id)"
                (ok && n==0) -> "(ceu_mem->$id)"
                (ok && n!=0) -> {
                    //println(id)
                    //println(this)
                    val blk = if (this is Expr.Proto) this.n else fup!!.n
                    "(((CEU_Proto_Mem_$blk*) ceu_frame ${"->proto->up".repeat(n)}->mem)->$id)"
                }
                (this is Expr.Block) -> bup!!.aux(n)
                (this is Expr.Proto) -> bup!!.aux(n+1)
                else -> error("bug found")
            }
        }
        return this.aux(0)
    }

    fun Expr.fupc (): String? {
        var n = 0
        var fup = ups.func(this)
        while (fup!=null && fup.isFake) {
            n++
            fup = ups.func(fup)
        }
        return if (fup == null) null else "(ceu_frame${"->proto->up".repeat(n)})"
    }

    // assrc_dst: calling expr is a source and here's its destination
    // asdst_src: calling expr is a destination and here's its source
    fun Expr.code (asdst_src: String?=null): String {
        return when (this) {
            is Expr.Proto -> {
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
                } CEU_Proto_Mem_$n;
                """
                val func = """ // BODY ${this.tk.dump()}
                CEU_Value ceu_proto_f_$n (
                    CEU_Frame* ceu_frame,
                    int ceu_n,
                    CEU_Value* ceu_args[]
                ) {
                    ${isfunc.cond{"""
                        CEU_Proto_Mem_$n _ceu_mem_;
                        CEU_Proto_Mem_$n* ceu_mem = &_ceu_mem_;
                        ceu_frame->mem = (char*) ceu_mem;
                    """}}
                    ${istask.cond{"""
                        CEU_Dynamic* ceu_coro = ceu_frame->Task.coro;
                        assert(ceu_coro->Bcast.status==CEU_CORO_STATUS_YIELDED || (ceu_coro->Bcast.status==CEU_CORO_STATUS_TOGGLED && ceu_evt==&CEU_EVT_CLEAR));
                        ceu_coro->Bcast.status = CEU_CORO_STATUS_RESUMED;
                        CEU_Proto_Mem_$n* ceu_mem = (CEU_Proto_Mem_$n*) ceu_frame->mem;
                    """}}
                    CEU_Proto_Mem_$n* ceu_mem_$n = ceu_mem;
                    """ +
                    """ // WHILE
                    do { // FUNC
                        ${istask.cond{"""
                            switch (ceu_frame->Task.pc) {
                                case -1:
                                    assert(0 && "bug found");
                                    break;
                                case 0: {
                                    if (ceu_has_throw_clear()) { // started with BCAST-CLEAR
                                        continue; // from BCAST-CLEAR: escape enclosing block
                                    }
                        """}}
                        { // ARGS
                            // no block yet, set now, will be reset in body
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
                        ${this.body.code()}
                        ${istask.cond{"}\n}\n"}}
                    } while (0);
                    """ +
                    """ // TERMINATE
                        ${istask.cond{"""
                            ceu_frame->Task.pc = -1;
                            {
                                CEU_Value ceu_evt_$n = { CEU_VALUE_POINTER, {.Pointer=ceu_coro} };
                                ceu_bcast_blocks(ceu_coro->hold, &ceu_evt_$n);
                            }
                            ceu_coro->Bcast.status = CEU_CORO_STATUS_TERMINATED;
                            if (ceu_coro->Bcast.Coro.coros != NULL) {
                                if ( ceu_coro->Bcast.Coro.coros->Bcast.Coros.open == 0) {
                                    ceu_coros_destroy( ceu_coro->Bcast.Coro.coros, ceu_coro);
                                }
                            }
                        """}}
                    return ceu_acc;
                }
                """
                tops.add(Pair(type,func))
                """
                CEU_Dynamic* ceu_proto_$n = ceu_proto_create(CEU_VALUE_${this.tk.str.uppercase()}, ceu_frame, ceu_proto_f_$n, sizeof(CEU_Proto_Mem_$n));
                assert(ceu_proto_$n != NULL);
                ceu_acc = (CEU_Value) { CEU_VALUE_${this.tk.str.uppercase()}, {.Dyn=ceu_proto_$n} };
                """
            }
            is Expr.Block -> {
                val bup = ups.block(this)
                val f_b = ups.proto_or_block(this)
                val depth = when {
                    (f_b == null) -> "(0 + 1)"
                    (f_b is Expr.Proto) -> "(ceu_frame->up->depth + 1)"
                    else -> "(${bup!!.toc(false)}.depth + 1)"
                }
                val es = this.es.map { it.code() }.joinToString("")
                """
                { // BLOCK ${this.tk.dump()}
                    ceu_mem->block_$n = (CEU_Block) { $depth, NULL, {NULL,NULL} };
                    ${ups.proto_or_block(this).let { it!=null && it !is Expr.Block && it.tk.str=="task" }.cond {
                        " ceu_coro->Bcast.Coro.block = &ceu_mem->block_$n;"}
                    }
                    ${(f_b is Expr.Block).cond {
                        "ceu_mem->block_${bup!!.n}.bcast.block = &ceu_mem->block_$n;"}
                    }
                    do {
                        $es
                    } while (0);
                    ${(f_b != null).cond {
                        val up = if (f_b is Expr.Proto) "ceu_frame->up" else bup!!.toc(true)
                        """
                        if (ceu_acc.tag > CEU_VALUE_DYNAMIC) {
                            char* ceu_err_$n = ceu_block_set($up, ceu_acc.Dyn, 0);
                            if (ceu_err_$n != NULL) {
                                // ${this.tk}
                                snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                continue;
                            }
                        }
                        """
                    }}
                    ceu_bcast_blocks(&ceu_mem->block_$n, &CEU_EVT_CLEAR);
                    { // DEFERS ${this.tk.dump()}
                        ${ups.xblocks[this]!!.defers!!.reversed().joinToString("")}
                    }
                    ${(f_b is Expr.Block).cond{"ceu_mem->block_${bup!!.n}.bcast.block = NULL;"}}
                    ${ups.proto_or_block(this).let { it!=null && it !is Expr.Block && it.tk.str=="task" }.cond{" ceu_coro->Bcast.Coro.block = NULL;"}}
                    ceu_block_free(&ceu_mem->block_$n);
                    if (ceu_has_throw_clear()) {
                        continue;   // escape to end of enclosing block
                    }
                }
                """
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                """
                { // DCL ${this.tk.dump()}
                    ${this.init.cond{"ceu_mem->$id = (CEU_Value) { CEU_VALUE_NIL };"}}
                    ceu_mem->_${id}_ = ${ups.block(this)!!.toc(true)};   // can't be static b/c recursion
                    ceu_acc = ceu_mem->$id;
                }
                """
            }
            is Expr.Set -> """
                { // SET ${this.tk.dump()}
                    ${this.src.code()}
                    ceu_mem->set_$n = ceu_acc;
                    ${this.dst.code("ceu_mem->set_$n")}
                    ceu_acc = ceu_mem->set_$n;
                }
                """
            is Expr.If -> """
                { // IF ${this.tk.dump()}
                    ${this.cnd.code()}
                    if (ceu_as_bool(&ceu_acc)) {
                        ${this.t.code()}
                    } else {
                        ${this.f.code()}
                    }
                }
                """
            is Expr.While -> """
                { // WHILE ${this.tk.dump()}
                CEU_WHILE_START_$n:;
                    ${this.cnd.code()}
                    if (ceu_as_bool(&ceu_acc)) {
                        ${this.body.code()}
                        goto CEU_WHILE_START_$n;
                    }
                }
                """
            is Expr.Catch -> """
                { // CATCH ${this.tk.dump()}
                    do {
                        ${this.body.code()}
                    } while (0);
                    if (ceu_has_bcast>0 && ceu_evt==&CEU_EVT_CLEAR) {
                        // do not catch anything while clearing up
                        continue; // escape enclosing block;
                    }
                    if (ceu_has_throw_clear()) {
                        ceu_has_throw = 0;
                        ${this.cnd.code()}
                        if (!ceu_as_bool(&ceu_acc)) {
                            ceu_has_throw = 1; // UNCAUGHT: escape to outer
                            continue; // escape enclosing block;
                        }
                        ceu_acc = ceu_err;
                        ceu_err = (CEU_Value) { CEU_VALUE_NIL };
                    }
                }
                """
            is Expr.Throw -> """
                { // THROW ${this.tk.dump()}
                    ${this.ex.code()}
                    ceu_err = ceu_acc;
                    ceu_has_throw = 1;
                    strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception", 256);
                    continue; // escape enclosing block;
                }
                """
            is Expr.Defer -> { ups.xblocks[ups.block(this)!!]!!.defers!!.add(this.body.code()); "" }

            is Expr.Coros -> {
                """
                { // COROS ${this.tk.dump()}
                    ${this.max.cond { """
                        ${it.code()}
                        if (ceu_acc.tag!=CEU_VALUE_NUMBER || ceu_acc.Number<=0) {                
                            ceu_has_throw = 1;
                            ceu_err = CEU_ERR_ERROR;
                            strncpy(ceu_err_error_msg,
                                "${it.tk.pos.file} : (lin ${it.tk.pos.lin}, col ${it.tk.pos.col}) : coroutines error : expected positive number",
                                 256);
                            continue; // escape enclosing block;
                        }
                    """}}
                    CEU_Dynamic* ceu_$n = malloc(sizeof(CEU_Dynamic));
                    assert(ceu_$n != NULL);
                    *ceu_$n = (CEU_Dynamic) {
                        CEU_VALUE_COROS, NULL, NULL, {
                            .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                                .Coros = { ${if (this.max==null) 0 else "ceu_acc.Number"}, 0, 0, NULL}
                            } }
                        }
                    };
                    ceu_acc = (CEU_Value) { CEU_VALUE_COROS, {.Dyn=ceu_$n} };
                }
                """
            }
            is Expr.Coro -> {
                """
                { // CORO ${this.tk.dump()}
                    ${this.task.code()}
                    CEU_Value ceu_coro_$n;
                    char* ceu_err_$n = ceu_coro_create(&ceu_acc, ${ups.block(this)!!.toc(true)}, &ceu_coro_$n);
                    if (ceu_err_$n != NULL) {
                        ceu_has_throw = 1;
                        ceu_err = CEU_ERR_ERROR;
                        snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    ceu_acc = ceu_coro_$n;
                }
                """
            }
            is Expr.Spawn -> this.call.code()
            is Expr.Iter -> {
                val loc = this.loc.str
                """
                { // ITER ${this.tk.dump()}
                    ${this.coros.code()}
                    ceu_mem->coros_$n = ceu_acc;
                    if (ceu_mem->coros_$n.tag != CEU_VALUE_COROS) {                
                        ceu_has_throw = 1;
                        ceu_err = CEU_ERR_ERROR;
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
                        ${this.body.code()}
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
                val bupc = ups.block(this)!!.toc(true)
                """
                { // BCAST ${this.tk.dump()}
                    ${this.evt.code()}
                    ceu_mem->evt_$n = ceu_acc;
                    assert(NULL == ceu_block_set($bupc, &ceu_mem->evt_$n, 1);
                    ${this.xin.code()}
                    int ceu_ok_$n = 0;
                    if (ceu_acc.tag == CEU_VALUE_CORO) {
                        ceu_err_$n = ceu_bcast_dyn(ceu_acc.Dyn, &ceu_mem->evt_$n);
                        ceu_ok_$n = 1;
                    } else if (ceu_acc.tag == CEU_VALUE_TAG) {
                        ceu_ok_$n = 1;
                        if (ceu_acc.Tag == CEU_TAG_global) {
                            ceu_err_$n = ceu_bcast_blocks(&ceu_mem_${outer.n}->block_${outer.n}, &ceu_mem->evt_$n);
                        } else if (ceu_acc.Tag == CEU_TAG_local) {
                            ceu_err_$n = ceu_bcast_blocks($bupc, &ceu_mem->evt_$n);
                        } else if (ceu_acc.Tag == CEU_TAG_task) {
                            ${this.fupc().let {
                                if (it == null) {
                                    "ceu_ok_$n = 0;"
                                } else {
                                    "ceu_err_$n = ceu_bcast_dyn($it->Task.coro, &ceu_mem->evt_$n);"
                                } 
                            }}
                        } else {
                            ceu_ok_$n = 0;
                        }
                    }
                    if (!ceu_ok_$n) {
                        ceu_has_throw = 1;
                        ceu_err = CEU_ERR_ERROR;
                        strncpy(ceu_err_error_msg, "${this.xin.tk.pos.file} : (lin ${this.xin.tk.pos.lin}, col ${this.xin.tk.pos.col}) : broadcast error : invalid target", 256);
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
                    ${this.arg.code()}
                    ceu_frame->Task.pc = $n;      // next resume
                    ceu_coro->Bcast.status = CEU_CORO_STATUS_YIELDED;
                    if (ceu_acc.tag > CEU_VALUE_DYNAMIC) {
                        char* ceu_err_$n = ceu_block_set(ceu_frame->up, ceu_acc.Dyn, 0);
                        if (ceu_err_$n != NULL) {
                            snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                            continue;
                        }
                    }
                    return ceu_acc;
                case $n:                    // resume here
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    assert(ceu_n <= 1 && "bug found : not implemented : multiple arguments to resume");
                    ceu_acc = *ceu_args[0]; // resume single argument
                }
                """
            is Expr.Resume -> this.call.code()
            is Expr.Toggle -> """
                ${this.on.code()}
                ceu_mem->on_$n = ceu_acc;
                ${this.coro.code()}
                if (ceu_acc.tag<CEU_VALUE_BCAST || (ceu_acc.Dyn->Bcast.status!=CEU_CORO_STATUS_YIELDED && ceu_coro_$n.Dyn->Bcast.status!=CEU_CORO_STATUS_TOGGLED)) {                
                    ceu_has_throw = 1;
                    ceu_err = CEU_ERR_ERROR;
                    strncpy(ceu_err_error_msg, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : toggle error : expected yielded/toggled coroutine", 256);
                    continue; // escape enclosing block;
                }
                ceu_acc.Dyn->Bcast.status = (ceu_as_bool(&ceu_mem->on_$n) ? CEU_CORO_STATUS_YIELDED : CEU_CORO_STATUS_TOGGLED);
                """
            is Expr.Pub -> """
                { // PUB
                    CEU_Dynamic* ceu_dyn_$n;
                    ${if (this.coro == null) {
                        "ceu_dyn_$n = ${this.fupc()}->Task.coro;"
                    } else { """
                        ${this.coro.code()}
                        ${(this.tk.str=="status").cond { """
                            // track with destroyed coro: status -> :destroyed
                            if (ceu_acc.tag == CEU_VALUE_TRACK) {
                                ceu_acc = ceu_track_to_coro(&ceu_acc);
                                if (ceu_acc.tag != CEU_VALUE_CORO) {
                                    ceu_acc = (CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_destroyed} };
                                    goto CEU_PUB_$n;    // special case, skip everything else
                                }
                            }
                            """
                         }}
                        ceu_acc = ceu_track_to_coro(&ceu_acc);
                        if (ceu_acc.tag != CEU_VALUE_CORO) {                
                            ceu_has_throw = 1;
                            ceu_err = CEU_ERR_ERROR;
                            strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tk.str} error : expected coroutine", 256);
                            continue; // escape enclosing block;
                        }
                        ceu_dyn_$n = ceu_acc.Dyn;
                    """ }}
                    ${if (asdst_src != null) {
                            """ // PUB - SET
                            char* ceu_err_$n = ceu_block_set(ceu_dyn_$n->hold, &$asdst_src, 0);
                            if (ceu_err_$n != NULL) {
                                snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                continue;
                            }
                            ceu_dyn_$n->Bcast.Coro.frame->Task.pub = $asdst_src;
                            """
                    } else {
                        val inidx = (ups.pred(this) { it is Expr.Index } != null)
                        """
                        { // PUB - read
                            ${(!inidx).cond { """
                                if (ceu_dyn_$n->Bcast.Coro.frame->Task.pub.tag > CEU_VALUE_DYNAMIC) {
                                    ceu_has_throw = 1;
                                    ceu_err = CEU_ERR_ERROR;
                                    strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : invalid ${this.tk.str} : cannot expose dynamic public field", 256);
                                    continue; // escape enclosing block;
                                }                                    
                            """ }}
                            ceu_acc = ${if (this.tk.str=="pub") {
                                "ceu_dyn_$n->Bcast.Coro.frame->Task.pub"
                            } else {
                                "(CEU_Value) { CEU_VALUE_TAG, {.Tag=ceu_dyn_$n->Bcast.status + CEU_TAG_resumed} }"
                            }};
                        }
                        """
                    }}
                    CEU_PUB_$n:;
                }
                """
            is Expr.Track -> """
                { // TRACK
                    ${this.coro.code()}
                    if (ceu_acc.tag != CEU_VALUE_CORO) {                
                        ceu_has_throw = 1;
                        ceu_err = CEU_ERR_ERROR;
                        strncpy(ceu_err_error_msg, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : track error : expected coroutine", 256);
                        continue; // escape enclosing block;
                    } else if (ceu_acc.Dyn->Bcast.status == CEU_CORO_STATUS_TERMINATED) {                
                        ceu_has_throw = 1;
                        ceu_err = CEU_ERR_ERROR;
                        strncpy(ceu_err_error_msg, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : track error : expected unterminated coroutine", 256);
                        continue; // escape enclosing block;
                    }
                    CEU_Dynamic* ceu_dyn_$n = ceu_track_create(ceu_acc.Dyn);
                    assert(ceu_dyn_$n != NULL);
                    CEU_Block* ceu_hld_$n = (ceu_coro_$n.Dyn->Bcast.Coro.coros == NULL) ? ceu_coro_$n.Dyn->hold : ceu_coro_$n.Dyn->Bcast.Coro.coros->hold;  // TODO!!!
                    ${SET("((CEU_Value) { CEU_VALUE_TRACK, {.Dyn=ceu_dyn_$n} })")}
                }
                """

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
                        "ceu_acc = ((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} });"
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
                if (asdst_src == null) {
                    "ceu_acc = ${bup.id2c(id)};  // ACC ${this.tk.dump()}\n"
                } else {
                    ups.assertIsDeclared(bup, "_${id}_", this.tk)
                    """
                    { // ACC - SET
                        if ($asdst_src.tag > CEU_VALUE_DYNAMIC) {
                            char* ceu_err_$n = ceu_block_set(${bup.id2c("_${id}_")}, $asdst_src.Dyn, 1);
                            if (ceu_err_$n != NULL) {
                                snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                continue;
                            }
                        }
                        ${bup.id2c(id)} = $asdst_src;
                    }
                    """
                }
            }
            is Expr.EvtErr -> "ceu_acc = ceu_${this.tk.str};\n"
            is Expr.Nil -> "ceu_acc = ((CEU_Value) { CEU_VALUE_NIL });\n"
            is Expr.Tag -> {
                val tag = this.tk.str.drop(1)
                if (!tags.contains(tag)) {
                    tags.add(tag)
                }
                "ceu_acc = ((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_$tag} });\n"
            }
            is Expr.Bool -> "ceu_acc = ((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} });\n"
            is Expr.Num -> "ceu_acc = ((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} });\n"

            is Expr.Tuple -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code() + """
                    ceu_mem->arg_${i}_$n = ceu_acc;
                    """
                }.joinToString("")
                """
                { // TUPLE ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_tup_$n = ceu_tuple_create(${ups.block(this)!!.toc(true)}, ${this.args.size}, ceu_args_$n);
                    assert(ceu_tup_$n != NULL);
                    ceu_acc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_tup_$n} };
                }
                """
            }
            is Expr.Dict -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.first.code()  + "ceu_mem->arg_${i}_a_$n = ceu_acc;\n" +
                    it.second.code() + "ceu_mem->arg_${i}_b_$n = ceu_acc;\n"
                }.joinToString("")
                """
                { // DICT ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}][2] = {
                        ${this.args.mapIndexed { i, _ -> "{ceu_mem->arg_${i}_a_$n,ceu_mem->arg_${i}_b_$n}" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_dict_$n = ceu_dict_create(${ups.block(this)!!.toc(true)}, ${this.args.size}, &ceu_args_$n);
                    assert(ceu_dict_$n != NULL);
                    ceu_acc = (CEU_Value) { CEU_VALUE_DICT, {.Dyn=ceu_dict_$n} };
                }
                """
            }
            is Expr.Index -> {
                fun Expr.Index.has_pub (): Boolean {
                    val up = ups.ups[this]
                    return when {
                        (this.col is Expr.Pub) -> true
                        (up == null) -> false
                        (up !is Expr.Index) -> false
                        else -> up.has_pub()
                    }
                }
                val haspub = this.has_pub()

                """
                { // INDEX  ${this.tk.dump()}
                    // IDX
                    ${this.idx.code()}
                    ceu_mem->idx_$n = ceu_acc;
                    // COL
                    ${this.col.code()}
                    char* ceu_err_$n = ceu_col_check(&ceu_acc, &ceu_mem->idx_$n);
                    ${asdst_src.cond { """
                        if (ceu_err_$n==NULL && $it.tag>CEU_VALUE_DYNAMIC) {
                            ceu_err_$n = ceu_block_set(ceu_acc.Dyn->hold, $it.Dyn, 0);
                        }
                    """}}
                    if (ceu_err_$n != NULL) {                
                        snprintf(ceu_err_error_msg, 256, "${this.col.tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    switch (ceu_acc.tag) { // OK
                        case CEU_VALUE_TUPLE:                
                            ${if (asdst_src != null) {
                                "ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number] = $asdst_src;\n"
                            } else {
                                """
                                ${haspub.cond { """
                                    if (ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number].tag > CEU_VALUE_DYNAMIC) {
                                        ceu_has_throw = 1;
                                        ceu_err = CEU_ERR_ERROR;
                                        strncpy(ceu_err_error_msg, "${this.idx.tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : invalid index : cannot expose dynamic public field", 256);
                                        continue; // escape enclosing block;
                                    }
                                """}}
                                ceu_acc = ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number];
                                """
                            }}
                            break;
                        case CEU_VALUE_DICT: {
                            int idx = ceu_dict_key_index(ceu_acc.Dyn, &ceu_mem->idx_$n);
                            ${if (asdst_src != null) {
                                    """ // SET
                                    if (idx == -1) {
                                        idx = ceu_dict_empty_index(ceu_acc.Dyn);
                                        (*ceu_acc.Dyn->Dict.mem)[idx][0] = ceu_mem->idx_$n;
                                    }
                                    (*ceu_acc.Dyn->Dict.mem)[idx][1] = $asdst_src;
                                    """
                            } else {
                                "ceu_acc = ((idx==-1) ? (CEU_Value) { CEU_VALUE_NIL } : (*ceu_acc.Dyn->Dict.mem)[idx][1]);\n"
                            }}
                            break;
                        }
                    }
                }
                """
            }
            is Expr.Call -> {
                val up = ups.ups[this]
                val bupc = ups.block(this)!!.toc(true)
                val resume = (if (up is Expr.Resume) up else null)
                val spawn  = (if (up is Expr.Spawn)  up else null)
                val iscall = (resume==null && spawn==null)
                val iscoros = (spawn?.coros != null)
                val frame = if (iscall) "(&ceu_frame_$n)" else "(ceu_coro_$n.Dyn->Bcast.Coro.frame)"

                val (args_sets,args_vs) = this.args.mapIndexed { i,e ->
                    Pair (
                        e.code() + "ceu_mem->arg_${i}_$n = ceu_acc;\n",
                        "&ceu_mem->arg_${i}_$n"
                    )
                }.unzip().let {
                    Pair(it.first.joinToString(""), it.second.joinToString(", "))
                }

                """
                { // SETS
                    $args_sets
                }
                { // CALL (open)
                """ +

                iscall.cond{"""
                // CALL ${this.tk.dump()}
                    ${this.proto.code()}
                    CEU_Value ceu_proto_$n = ceu_acc;
                    char* ceu_err_$n = NULL;
                    if (ceu_proto_$n.tag != CEU_VALUE_FUNC) {
                        ceu_err_$n = "call error : expected function";
                    }
                    CEU_Frame ceu_frame_$n = { &ceu_proto_$n.Dyn->Proto, $bupc, NULL, {} };
                """} +

                spawn.cond{"""
                // SPAWN/CORO ${this.tk.dump()}
                    ${iscoros.cond{spawn!!.coros!!.code()}}
                    ${this.proto.code()}
                    CEU_Value ceu_task_$n = ceu_acc;
                    CEU_Value ceu_coro_$n;
                    ${iscoros.cond { "CEU_Value ceu_ok_$n = { CEU_VALUE_BOOL, {.Bool=1} };" }}
                    ${if (!iscoros) {
                        """
                        char* ceu_err_$n = ceu_coro_create(&ceu_task_$n, $bupc, &ceu_coro_$n);
                        """
                    } else {
                        """
                        char* ceu_err_$n = ceu_coros_create(&ceu_ok_$n.Bool, ceu_mem->coros_${spawn!!.n}.Dyn, &ceu_task_$n, $bupc->depth, &ceu_coro_$n);
                        if (ceu_ok_$n.Bool) {
                            // call task only if ok
                        //} // closes below
                        """
                    }}
                """} +

                resume.cond{"""
                // RESUME ${this.tk.dump()}
                    ${this.proto.code()}
                    CEU_Value ceu_coro_$n = ceu_acc;
                    char* ceu_err_$n = NULL;
                    if (ceu_coro_$n.tag<CEU_VALUE_BCAST || (ceu_coro_$n.Dyn->Bcast.status!=CEU_CORO_STATUS_YIELDED && ceu_coro_$n.Dyn->Bcast.status!=CEU_CORO_STATUS_TOGGLED)) {                
                        ceu_err_$n = "resume error : expected yielded task";
                    }
                """} +

                """
                    if (ceu_err_$n != NULL) {
                        ceu_has_throw = 1;
                        ceu_err = CEU_ERR_ERROR;
                        snprintf(ceu_err_error_msg, 256, "${this.proto.tk.pos.file} : (lin ${this.proto.tk.pos.lin}, col ${this.proto.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    CEU_Value* ceu_args_$n[] = { $args_vs };
                    ${iscall.cond { "CEU_Value ceu_$n = " }}
                    ${resume.cond { "CEU_Value ceu_$n = " }}
                        $frame->proto->f (
                            $frame,
                            ${this.args.size},
                            ceu_args_$n
                        );
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    ${iscall.cond{ "ceu_acc = ceu_$n;" }}
                    ${spawn.cond{ "ceu_acc = ${if (iscoros) "ceu_ok_$n" else "ceu_coro_$n" };" }}
                    ${iscoros.cond{"}"}}
                    ${resume.cond{ "ceu_acc = ceu_$n;" }}
                } // CALL (close)
                """
            }

            is Expr.XSeq -> error("bug found")
        }
    }
}
