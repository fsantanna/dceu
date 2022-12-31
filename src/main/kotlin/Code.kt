class Coder (val outer: Expr.Block, val ups: Ups) {
    val tags = TAGS.map { Pair(it,it.drop(1).replace('.','_')) }.toMutableList()
    val tops: Triple<MutableList<String>, MutableList<String>, MutableList<String>> = Triple(mutableListOf(),mutableListOf(), mutableListOf())
    val mem: String = outer.mem()
    val code: String = outer.code(false, null)

    fun Expr.Block.toc (isptr: Boolean): String {
        return "ceu_mem->block_${this.n}".let {
            if (isptr) "(&($it))" else it
        }
    }

    fun Expr.id2c (id: String): String {
        val dcl = ups.getDcl(this, id)!!
        val mem = if (dcl.upv == 0) "mem" else "upvs"
        val fup = if (dcl.blk is Expr.Proto) this else ups.func_or_task(dcl.blk)
        val N = ups.path_until(this) { it==dcl.blk }.count{ it is Expr.Proto } - 1
        return when {
            (fup == null) -> "(ceu_${mem}_${outer.n}->$id)"
            (N <= 0) -> "(ceu_${mem}->$id)"
            else -> {
                val blk = if (this is Expr.Proto) this.n else fup.n
                "(((CEU_Proto_Mem_$blk*) ceu_frame ${"->proto->up".repeat(N)}->${mem})->$id)"
            }
        }
    }

    fun Expr.fupc (tk: String?=null): String? {
        var n = 0
        var fup = ups.func_or_task(this)
        while (fup!=null && ((fup.task!=null && fup.task!!.first) || tk==null || fup.tk.str!=tk)) {
            n++
            fup = ups.func_or_task(fup)
        }
        return if (fup == null) null else "(ceu_frame${"->proto->up".repeat(n)})"
    }

    fun Expr.gcall (): Boolean {
        return ups.ups[this].let { it is Expr.Call && it.proto.let {
            it is Expr.Acc && it.tk.str in EXPOSE
        } }
    }

    // assrc_dst: calling expr is a source and here's its destination
    // asdst_src: calling expr is a destination and here's its source
    fun Expr.code (issrc: Boolean, asdst_src: String?): String {
        fun assrc (v: String): String {
            return if (issrc) "ceu_acc = $v;\n" else "ceu_acc = (CEU_Value) { CEU_VALUE_NIL };\n"
        }
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
                CEU_RET ceu_proto_f_$n (
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
                        #ifdef XXX
                        printf("pc=%2d, status=%d, coro=%p\n", ceu_frame->Task.pc, ceu_coro->Bcast.status, ceu_coro);
                        #endif
                        CEU_Proto_Mem_$n* ceu_mem = (CEU_Proto_Mem_$n*) ceu_frame->mem;
                        CEU_Value* ceu_evt = &CEU_EVT_NIL;
                        if (ceu_n == CEU_ARG_EVT) {
                            ceu_evt = ceu_args[0];
                        }
                        assert(ceu_coro->Bcast.status==CEU_CORO_STATUS_YIELDED || (ceu_coro->Bcast.status==CEU_CORO_STATUS_TOGGLED && ceu_evt==&CEU_EVT_CLEAR));
                        //if (ceu_evt != &CEU_EVT_CLEAR) {
                            ceu_coro->Bcast.status = CEU_CORO_STATUS_RESUMED;
                        //}
                    """}}
                    CEU_RET ceu_ret = (ceu_n == CEU_ARG_ERR) ? CEU_RET_THROW : CEU_RET_RETURN;
                    CEU_Proto_Mem_$n* ceu_mem_$n = ceu_mem;
                    """ +
                    """ // WHILE
                    do { // func
                        ${istask.cond{"""
                        switch (ceu_frame->Task.pc) {
                            case -1:
                                assert(0 && "bug found");
                                break;
                            case 0: {
                                CEU_CONTINUE_ON_CLEAR_THROW(); // may start with clear w/ coroutine() w/o resume
                        """}}
                            { // initialize parameters
                                int ceu_i = 0;
                                ${this.args.map {
                                    val id = it.str.noSpecial()
                                    """
                                    ${istask.cond { """
                                        if (ceu_coro->Bcast.Coro.coros != NULL) {
                                            ceu_mem->_${id}_ = ceu_coro->hold;
                                        } else
                                    """}}
                                    { // else
                                        ceu_mem->_${id}_ = ${this.body.toc(true)};
                                        ceu_mem->_${id}_->depth = ceu_frame->up->depth + 1;
                                    }
                                    if (ceu_i < ceu_n) {
                                        if (ceu_args[ceu_i]->type > CEU_VALUE_DYNAMIC) {
                                            ceu_ret = ceu_block_set(ceu_mem->_${id}_, ceu_args[ceu_i]->Dyn, 0);
                                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                        }
                                        ceu_mem->$id = *ceu_args[ceu_i];
                                    } else {
                                        ceu_mem->$id = (CEU_Value) { CEU_VALUE_NIL };
                                    }
                                    ceu_i++;
                                    """
                                }.joinToString("")}
                            }
                            // BODY
                            ${this.body.code(true, null)}
                        ${istask.cond{"}\n}\n"}}
                    } while (0); // func
                    """ + istask.cond{ """  // TERMINATE
                    if (ceu_coro->Bcast.status < CEU_CORO_STATUS_TERMINATED) {
                        ceu_coro->Bcast.status = CEU_CORO_STATUS_TERMINATED;
                        ceu_coro->Bcast.Coro.frame->Task.pub = ceu_acc;
                        ceu_frame->Task.pc = -1;

                        if (ceu_n==-1 && ceu_evt==&CEU_EVT_CLEAR) {
                            // do not signal termination: clear comes from clearing enclosing block,
                            // which also clears all possible interested awaits
                        } else if (ceu_ret == CEU_RET_THROW) {
                            // do not signal termination: throw clears enclosing block,
                            // which also clears all possible interested awaits
                        } else {
                            // only signal on normal termination

                            // ceu_ret/ceu_acc: save/restore
                            CEU_RET   ceu_ret_$n = ceu_ret;
                            CEU_Value ceu_acc_$n = ceu_acc;

                            CEU_Value ceu_evt_$n = { CEU_VALUE_CORO, {.Dyn=ceu_coro} };
                            ceu_bcasting++;
                            if (ceu_coro->hold->bcast.up != NULL) {
                                // enclosing coro of enclosing block
                                ceu_ret = MIN(ceu_ret, ceu_bcast_dyn(ceu_coro->hold->bcast.up, &ceu_evt_$n));
                            } else {
                                // enclosing block
                                ceu_ret = MIN(ceu_ret, ceu_bcast_blocks(ceu_coro->hold, &ceu_evt_$n));
                            }
                            ceu_bcasting--;
                        
                            if (ceu_coro->Bcast.Coro.coros != NULL) {
                                if (ceu_coro->Bcast.Coro.coros->Bcast.Coros.open == 0) {
                                    ceu_coros_destroy(ceu_coro->Bcast.Coro.coros, ceu_coro);
                                }
                            }

                            ceu_bcast_free();

                            if (ceu_ret_$n==CEU_RET_THROW || ceu_ret!=CEU_RET_THROW) {
                                ceu_acc = ceu_acc_$n;
                            } else {
                                // do not restore acc: we were ok, but now we did throw
                            }
                        }
                    }
                    """} + """
                    return ceu_ret;
                }
                """
                tops.second.add(type)
                tops.third.add(func)
                """
                CEU_Dynamic* ceu_proto_$n = ceu_proto_create (
                    ${ups.block(this)!!.toc(true)},
                    ${if (ups.noclos.contains(this)) 1 else 0},     // noclo must be perm=1
                    CEU_VALUE_${this.tk.str.uppercase()},
                    (CEU_Proto) {
                        ceu_frame,
                        ceu_proto_f_$n,
                        0,
                        { .Task = {
                            ${if (istask && this.task!!.second) 1 else 0},
                            sizeof(CEU_Proto_Mem_$n)
                        } }
                    }
                );
                assert(ceu_proto_$n != NULL);
                ${assrc("(CEU_Value) { CEU_VALUE_${this.tk.str.uppercase()}, {.Dyn=ceu_proto_$n} }")}
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
                val es = this.es.mapIndexed { i,it ->
                    it.code(issrc && i==this.es.size-1, null)
                }.joinToString("")
                val coro = if (ups.intask(this)) "ceu_coro" else "NULL"
                """
                { // BLOCK ${this.tk.dump()}
                    ceu_mem->block_$n = (CEU_Block) { $depth, ${if (f_b?.tk?.str=="task") 1 else 0}, NULL, {$coro,NULL,NULL} };
                    ${ups.proto_or_block(this).let { it!=null && it !is Expr.Block && it.tk.str=="task" }.cond {
                        " ceu_coro->Bcast.Coro.block = &ceu_mem->block_$n;"}
                    }
                    ${(f_b is Expr.Block).cond {
                        "ceu_mem->block_${bup!!.n}.bcast.block = &ceu_mem->block_$n;"}
                    }
                    do { // block
                        $es
                    } while (0); // block
                    if (ceu_ret == CEU_RET_THROW) {
                        // must be before frees
                        ${(f_b == null).cond {"ceu_error_list_print();" }}
                    }
                    { // ceu_ret/ceu_acc: save/restore
                        CEU_RET   ceu_ret_$n = ceu_ret;
                        CEU_Value ceu_acc_$n = ceu_acc;
                        {
                            { // move up dynamic ceu_acc (return or error)
                                ${(f_b != null).cond {
                                    val up = if (f_b is Expr.Proto) "ceu_frame->up" else bup!!.toc(true)
                                    """
                                    ${(f_b!!.tk.str=="task").cond {
                                        "ceu_mem->block_$n.ispub = 0;"
                                    }}
                                    if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                                        ceu_ret = ceu_block_set($up, ceu_acc.Dyn, 0);
                                        if (ceu_ret == CEU_RET_THROW) {
                                            CEU_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                            // prioritize scope error over whatever there is now
                                            ceu_acc_$n = ceu_acc;
                                        }
                                        ceu_ret_$n = MIN(ceu_ret_$n, ceu_ret);
                                    }
                                    """
                                }}
                            }
                            { // cleanup active nested spawns in this block
                                ceu_bcasting++;
                                assert(CEU_RET_RETURN == ceu_bcast_dyns(ceu_mem->block_$n.bcast.dyn, &CEU_EVT_CLEAR));
                                ceu_bcasting--;
                                ceu_bcast_free();
                            }
                            { // DEFERS ${this.tk.dump()}
                                ceu_ret = CEU_RET_RETURN;
                                ${ups.xblocks[this]!!.defers!!.reversed().joinToString("")}
                                if (ceu_ret_$n!=CEU_RET_THROW && ceu_ret==CEU_RET_THROW) {
                                    ceu_acc_$n = ceu_acc;
                                }
                                ceu_ret_$n = MIN(ceu_ret_$n, ceu_ret);
                            }
                            { // relink blocks
                                ${(f_b is Expr.Block).cond{
                                    "ceu_mem->block_${bup!!.n}.bcast.block = NULL;"
                                }}
                                ${ups.proto_or_block(this).let { it!=null && it !is Expr.Block && it.tk.str=="task" }.cond{
                                    "ceu_coro->Bcast.Coro.block = NULL;"
                                }}
                                ceu_dyns_free(ceu_mem->block_$n.tofree);
                            }
                        }
                        ceu_acc = ceu_acc_$n;
                        ceu_ret = ceu_ret_$n;
                        CEU_CONTINUE_ON_CLEAR_THROW();
                    } // ceu_ret/ceu_acc: save/restore
                }
                """
            }
            is Expr.Group -> this.es.mapIndexed { i,it ->
                it.code(issrc && i==this.es.size-1, null)
            }.joinToString("")
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                """
                { // DCL ${this.tk.dump()}
                    ${this.init.cond{"ceu_mem->$id = (CEU_Value) { CEU_VALUE_NIL };"}}
                    ceu_mem->_${id}_ = ${ups.block(this)!!.toc(true)};   // can't be static b/c recursion
                    ${assrc("ceu_mem->$id")}
                }
                """
            }
            is Expr.Set -> """
                { // SET ${this.tk.dump()}
                    ${this.src.code(true, null)}
                    ceu_mem->set_$n = ceu_acc;
                    ${this.dst.code(issrc, "ceu_mem->set_$n")}
                    ${assrc("ceu_mem->set_$n")}
                }
                """
            is Expr.If -> """
                { // IF ${this.tk.dump()}
                    ${this.cnd.code(true, null)}
                    CEU_Value ceu_accx = ceu_acc;
                    if (ceu_as_bool(&ceu_accx)) {
                        ${this.t.code(issrc, null)}
                    } else {
                        ${this.f.code(issrc, null)}
                    }
                }
                """
            is Expr.While -> """
                { // WHILE ${this.tk.dump()}
                CEU_WHILE_START_$n:;
                    ${this.cnd.code(true, null)}
                    CEU_Value ceu_accx = ceu_acc;
                    if (ceu_as_bool(&ceu_accx)) {
                        ${this.body.code(false, null)}
                        goto CEU_WHILE_START_$n;
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                }
                """
            is Expr.Catch -> """
                { // CATCH ${this.tk.dump()}
                    do { // catch
                        ${this.body.code(issrc, null)}
                    } while (0); // catch
                    CEU_CONTINUE_ON_CLEAR();
                    if (ceu_ret == CEU_RET_THROW) {
                        ceu_ret = CEU_RET_RETURN;
                        CEU_Value ceu_err = ceu_acc;
                        do {
                            ${this.cnd.code(true, null)}
                        } while (0);
                        assert(ceu_ret!=CEU_RET_YIELD && "bug found: cannot yield in catch condition");
                        CEU_CONTINUE_ON_THROW();
                        CEU_Value ceu_accx = ceu_acc;
                        if (!ceu_as_bool(&ceu_accx)) {
                            CEU_THROW_DO(ceu_err, continue); // uncaught, rethrow
                        }
                        ${assrc("ceu_acc = ceu_err")}
                    }
                }
                """
            is Expr.Throw -> """
                { // THROW ${this.tk.dump()}
                    ${this.ex.code(true, null)}
                    CEU_THROW_DO_MSG(ceu_acc, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception");
                }
                """
            is Expr.Defer -> { ups.xblocks[ups.block(this)!!]!!.defers!!.add(this.body.code(false, null)); "" }

            is Expr.Coros -> {
                """
                { // COROS ${this.tk.dump()}
                    ${this.max.cond { """
                        ${it.code(true, null)}
                        if (ceu_acc.type!=CEU_VALUE_NUMBER || ceu_acc.Number<=0) {                
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${it.tk.pos.file} : (lin ${it.tk.pos.lin}, col ${it.tk.pos.col}) : coroutines error : expected positive number");
                        }
                    """}}
                    assert(CEU_RET_RETURN == ceu_coros_create (
                        ${ups.block(this)!!.toc(true)},
                        ${if (this.max==null) 0 else "ceu_acc.Number"},
                        &ceu_acc
                    ));
                }
                """
            }
            is Expr.Coro -> {
                """
                { // CORO ${this.tk.dump()}
                    ${this.task.code(true, null)}
                    CEU_Value ceu_coro_$n;
                    ceu_ret = ceu_coro_create(${ups.block(this)!!.toc(true)}, &ceu_acc, &ceu_coro_$n);
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col})");
                    ${assrc("ceu_acc = ceu_coro_$n")}
                }
                """
            }
            is Expr.Spawn -> this.call.code(issrc, null)
            is Expr.CsIter -> {
                val loc = this.loc.str
                """
                { // ITER ${this.tk.dump()}
                    ${this.coros.code(true, null)}
                    ceu_mem->coros_$n = ceu_acc;
                    if (ceu_mem->coros_$n.type != CEU_VALUE_COROS) {                
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.coros.tk.pos.lin}, col ${this.coros.tk.pos.col}) : while error : expected coroutines");
                    }
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.open++;
                    ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->coros_$n.Dyn->Bcast.Coros.first} };
                    ceu_ret = CEU_RET_RETURN;
                    do { // iter
                CEU_ITER_$n:;
                        if (ceu_mem->$loc.Dyn == NULL) {
                            continue; // escape enclosing block
                        }
                        ceu_mem->hold_$n = ceu_mem->$loc.Dyn->hold;  
                        ceu_mem->$loc.Dyn->hold = ${this.body.toc(true)}; // tmp coro.hold to nested block
                        ${this.body.code(false, null)}
                        ceu_mem->$loc.Dyn->hold = ceu_mem->hold_$n;
                        ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->$loc.Dyn->Bcast.next} };
                        goto CEU_ITER_$n;
                    } while (0); // iter
                    assert(ceu_ret!=CEU_RET_YIELD && "bug found: cannot yield in iter");
                    if (ceu_mem->$loc.Dyn != NULL) { // repeat in case body error
                        ceu_mem->$loc.Dyn->hold = ceu_mem->hold_$n;
                    }
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.open--;
                    if (ceu_mem->coros_$n.Dyn->Bcast.Coros.open == 0) {
                        ceu_coros_cleanup(ceu_mem->coros_$n.Dyn);
                    }
                    CEU_CONTINUE_ON_THROW();
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                }
                """
            }
            is Expr.Bcast -> {
                val bupc = ups.block(this)!!.toc(true)
                val intask = ups.intask(this)
                """
                { // BCAST ${this.tk.dump()}
                    ${this.evt.code(true, null)}
                    ceu_mem->evt_$n = ceu_acc;
                    if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                        assert(CEU_RET_RETURN == ceu_block_set($bupc, ceu_mem->evt_$n.Dyn, 1));
                    }
                    ${this.xin.code(true, null)}
                    int ceu_err_$n = 0;
                    ceu_bcasting++;
                    if (ceu_acc.type == CEU_VALUE_CORO) {
                        ceu_ret = ceu_bcast_dyn(ceu_acc.Dyn, &ceu_mem->evt_$n);
                    } else if (ceu_acc.type == CEU_VALUE_TAG) {
                        if (ceu_acc.Tag == CEU_TAG_global) {
                            ceu_ret = ceu_bcast_blocks(&ceu_mem_${outer.n}->block_${outer.n}, &ceu_mem->evt_$n);
                        } else if (ceu_acc.Tag == CEU_TAG_local) {
                            ceu_ret = ceu_bcast_blocks($bupc, &ceu_mem->evt_$n);
                        } else if (ceu_acc.Tag == CEU_TAG_task) {
                            ${this.fupc("task").let {
                                if (it == null) {
                                    "ceu_err_$n = 1;"
                                } else {
                                    "ceu_ret = ceu_bcast_dyn($it->Task.coro, &ceu_mem->evt_$n);"
                                }
                            }}
                        } else {
                            ceu_err_$n = 1;
                        }
                    } else {
                        ceu_err_$n = 1;
                    }
                    ceu_bcasting--;
                    ${intask.cond {
                        "//int ceu_term_$n = ceu_coro->Bcast.status>=CEU_CORO_STATUS_TERMINATED;"
                    }}
                    ceu_bcast_free();
                    ${intask.cond {
                        "//if (ceu_term_$n) return CEU_RET_RETURN;"
                    }}
                    if (ceu_err_$n) {
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.xin.tk.pos.file} : (lin ${this.xin.tk.pos.lin}, col ${this.xin.tk.pos.col}) : broadcast error : invalid target");
                    }
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                }
                """
            }
            is Expr.Yield -> """
                { // YIELD ${this.tk.dump()}
                    ${this.arg.code(true, null)}
                    ceu_frame->Task.pc = $n;      // next resume
                    ceu_coro->Bcast.status = CEU_CORO_STATUS_YIELDED;
                    if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                        ceu_ret = ceu_block_set(ceu_frame->up, ceu_acc.Dyn, 0);
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                    }
                    return CEU_RET_YIELD;
                case $n:                    // resume here
                    CEU_CONTINUE_ON_CLEAR_THROW();
                    if (ceu_n == CEU_ARG_EVT) {
                        ${assrc("(CEU_Value) { CEU_VALUE_NIL }")} // resume single argument
                    } else {
                        assert(ceu_n <= 1 && "bug found : not implemented : multiple arguments to resume");
                        if (ceu_n == 0) {
                            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                        } else {
                            ${assrc("*ceu_args[0]")} // resume single argument
                        }
                    }
                }
                """
            is Expr.Resume -> this.call.code(issrc, null)
            is Expr.Toggle -> """
                ${this.on.code(true, null)}
                ceu_mem->on_$n = ceu_acc;
                ${this.coro.code(true, null)}
                if (ceu_acc.type<CEU_VALUE_BCAST || (ceu_acc.Dyn->Bcast.status!=CEU_CORO_STATUS_YIELDED && ceu_acc.Dyn->Bcast.status!=CEU_CORO_STATUS_TOGGLED)) {                
                    CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : toggle error : expected yielded/toggled coroutine");
                }
                ceu_acc.Dyn->Bcast.status = (ceu_as_bool(&ceu_mem->on_$n) ? CEU_CORO_STATUS_YIELDED : CEU_CORO_STATUS_TOGGLED);
                """
            is Expr.Pub -> """
                { // PUB
                    CEU_Dynamic* ceu_dyn_$n;
                    ${if (this.coro == null) {
                        "ceu_dyn_$n = ${this.fupc("task")}->Task.coro;"
                    } else { """
                        ${this.coro.code(true, null)}
                        ${(this.tk.str=="status").cond { """
                            // track with destroyed coro: status -> :destroyed
                            if (ceu_acc.type == CEU_VALUE_TRACK) {
                                CEU_Value ceu_accx = ceu_acc;
                                ceu_acc = ceu_track_to_coro(&ceu_accx);
                                if (ceu_acc.type != CEU_VALUE_CORO) {
                                    ${assrc("(CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_destroyed} }")}
                                    goto CEU_PUB_$n;    // special case, skip everything else
                                }
                            }
                            """
                         }}
                        CEU_Value ceu_accx = ceu_acc;
                        ceu_acc = ceu_track_to_coro(&ceu_accx);
                        if (ceu_acc.type != CEU_VALUE_CORO) {                
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tk.str} error : expected coroutine");
                        }
                        ceu_dyn_$n = ceu_acc.Dyn;
                    """ }}
                    ${if (asdst_src != null) {
                            """ // PUB - SET
                            if ($asdst_src.type > CEU_VALUE_DYNAMIC) {
                                ceu_ret = ceu_block_set(&ceu_mem->block_${ups.task(this)!!.body.n}, $asdst_src.Dyn, 1);
                                CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                            }
                            ceu_dyn_$n->Bcast.Coro.frame->Task.pub = $asdst_src;
                            """
                    } else {
                        val inidx  = ups.path_until(this) { it is Expr.Index }.isNotEmpty()
                        val incall = (ups.ups[this] is Expr.Call)
                        """
                        { // PUB - read
                            ${(!inidx && !incall && !this.gcall() && this.tk.str=="pub").cond { """
                                if (ceu_dyn_$n->Bcast.Coro.frame->Task.pub.type > CEU_VALUE_DYNAMIC) {
                                    CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : invalid ${this.tk.str} : cannot expose dynamic \"pub\" field");
                                }                                    
                            """ }}
                            ${assrc(if (this.tk.str=="pub") {
                                "ceu_dyn_$n->Bcast.Coro.frame->Task.pub"
                            } else {
                                "(CEU_Value) { CEU_VALUE_TAG, {.Tag=ceu_dyn_$n->Bcast.status + CEU_TAG_yielded - 1} }"
                            })}
                        }
                        """
                    }}
                    CEU_PUB_$n:;
                }
                """
            is Expr.Track -> """
                { // TRACK
                    ${this.coro.code(true, null)}
                    if (ceu_acc.type != CEU_VALUE_CORO) {                
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : track error : expected coroutine");
                    } else if (ceu_acc.Dyn->Bcast.status == CEU_CORO_STATUS_TERMINATED) {                
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : track error : expected unterminated coroutine");
                    }
                    {
                        CEU_Value ceu_$n;
                        assert(NULL == ceu_track_create(ceu_acc.Dyn, &ceu_$n));
                        ${assrc("ceu_$n")}
                    }
                }
                """

            is Expr.Nat -> {
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
                            ups.assertIsDeclared(this, Pair(id,0), this.tk)
                            id = this.id2c(id)
                            "($id)$x"
                        }
                    }
                    ret
                }
                val (pre,pos) = when (this.tk_.tag) {
                    null -> Pair(null, body)
                    ":pre" -> Pair(body, "")
                    else -> {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        val v = assrc("((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} })")
                        Pair(null, """
                        //{ // NATIVE ${this.tk.dump()} // (use comment b/c native may declare var to be used next)
                            $v
                        //}
                        """)
                    }
                }
                if (pre != null) {
                    tops.first.add(pre)
                }
                pos
            }
            is Expr.Acc -> {
                val id = this.tk_.fromOp().noSpecial()
                ups.assertIsDeclared(this, Pair(id,this.tk_.upv), this.tk)
                if (asdst_src == null) {
                    assrc(this.id2c(id)) // ACC ${this.tk.dump()}
                } else {
                    val dcl = ups.assertIsDeclared(this, Pair("_${id}_",this.tk_.upv), this.tk)
                    if (dcl.upv > 0) {
                        err(tk, "set error : cannot reassign an upval")
                    }
                    val isperm = if (id[0] == '_') 0 else 1
                    """
                    { // ACC - SET
                        if ($asdst_src.type > CEU_VALUE_DYNAMIC) {
                            ceu_ret = ceu_block_set(${this.id2c("_${id}_")}, $asdst_src.Dyn, $isperm);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        }
                        ${this.id2c(id)} = $asdst_src;
                    }
                    """
                }
            }
            is Expr.EvtErr -> {
                when (this.tk.str) {
                    "err" -> assrc("ceu_err")
                    "evt" -> {
                        val inidx = ups.path_until(this) { it is Expr.Index }.isNotEmpty()
                        (!inidx && !this.gcall()).cond { """
                            if (ceu_evt->type > CEU_VALUE_DYNAMIC) {
                                CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : invalid evt : cannot expose dynamic \"evt\"");
                            }                                    
                        """ } + assrc("(*ceu_evt)")
                    }
                    else -> error("impossible case")
                }
            }
            is Expr.Nil -> assrc("((CEU_Value) { CEU_VALUE_NIL })")
            is Expr.Tag -> {
                val tag = this.tk.str
                val ctag = tag.drop(1).replace('.','_')
                if (tags.none { it.first==tag }) {
                    tags.add(Pair(tag,ctag))
                }
                assrc("((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_$ctag} })")
            }
            is Expr.Bool -> assrc("((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} })")
            is Expr.Char -> assrc("((CEU_Value) { CEU_VALUE_CHAR, {.Char=${this.tk.str}} })")
            is Expr.Num -> assrc("((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} })")

            is Expr.Tuple -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code(true, null) + """
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
                    ${assrc("(CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_tup_$n} }")}
                }
                """
            }
            is Expr.Vector -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code(true, null) + """
                    ceu_mem->arg_${i}_$n = ceu_acc;
                    """
                }.joinToString("")
                """
                { // VECTOR ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                    };
                    int ceu_tag_$n = CEU_VALUE_NIL;
                    { // check if vector is homogeneous
                        for (int i=0; i<${this.args.size}; i++) {
                            if (i == 0) {
                                ceu_tag_$n = ceu_args_$n[i].type;
                            } else if (ceu_tag_$n != ceu_args_$n[i].type) {
                                CEU_THROW_DO_MSG(CEU_ERR_ERROR, break, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : vector error : non homogeneous arguments");
                            }
                        }
                        CEU_CONTINUE_ON_THROW();
                    }
                    CEU_Dynamic* ceu_vec_$n = ceu_vector_create(${ups.block(this)!!.toc(true)}, ceu_tag_$n, ${this.args.size}, ceu_args_$n);
                    assert(ceu_vec_$n != NULL);
                    ${assrc("(CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=ceu_vec_$n} }")}
                }
                """
            }
            is Expr.Dict -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.first.code(true, null)  + "ceu_mem->arg_${i}_a_$n = ceu_acc;\n" +
                    it.second.code(true, null) + "ceu_mem->arg_${i}_b_$n = ceu_acc;\n"
                }.joinToString("")
                """
                { // DICT ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}][2] = {
                        ${this.args.mapIndexed { i, _ -> "{ceu_mem->arg_${i}_a_$n,ceu_mem->arg_${i}_b_$n}" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_dict_$n = ceu_dict_create(${ups.block(this)!!.toc(true)}, ${this.args.size}, &ceu_args_$n);
                    assert(ceu_dict_$n != NULL);
                    ${assrc("(CEU_Value) { CEU_VALUE_DICT, {.Dyn=ceu_dict_$n} }")}
                }
                """
            }
            is Expr.Index -> {
                fun Expr.Index.has_pub_evt (): String? {
                    val up = ups.ups[this]
                    return when {
                        (this.col is Expr.Pub) -> "pub"
                        (this.col is Expr.EvtErr && this.col.tk.str=="evt") -> "evt"
                        (up == null) -> null
                        (up !is Expr.Index) -> null
                        else -> up.has_pub_evt()
                    }
                }
                """
                { // INDEX  ${this.tk.dump()}
                    // IDX
                    ${this.idx.code(true, null)}
                    ceu_mem->idx_$n = ceu_acc;
                    // COL
                    ${this.col.code(true, null)}
                    CEU_Value ceu_accx = ceu_acc;
                    ceu_ret = ceu_col_check(&ceu_accx, &ceu_mem->idx_$n);
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                    ${asdst_src.cond { """
                        if ($it.type > CEU_VALUE_DYNAMIC) {
                            ceu_ret = ceu_block_set(ceu_acc.Dyn->hold, $it.Dyn, 0);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        }
                    """}}
                    switch (ceu_acc.type) { // OK
                        case CEU_VALUE_TUPLE:                
                            ${if (asdst_src != null) {
                                "ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number] = $asdst_src;\n"
                            } else {
                                val x = this.has_pub_evt()
                                (x!=null && !this.gcall()).cond { """
                                    if (ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number].type > CEU_VALUE_DYNAMIC) {
                                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.idx.tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : invalid index : cannot expose dynamic \"$x\" field");
                                    }
                                """ } +
                                """
                                ${assrc("ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number]")}
                                """
                            }}
                            break;
                        case CEU_VALUE_VECTOR: {
                            ${if (asdst_src != null) {
                                "ceu_vector_set(ceu_acc.Dyn, ceu_mem->idx_$n.Number, $asdst_src);"
                            } else {
                                """
                                ceu_ret = ceu_vector_get(ceu_acc.Dyn, ceu_mem->idx_$n.Number);
                                CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                """
                            }}
                            break;
                        }
                        case CEU_VALUE_DICT: {
                            CEU_Value ceu_dict = ceu_acc;
                            ${if (asdst_src != null) {
                                """ // SET
                                assert(CEU_RET_RETURN == ceu_dict_set(ceu_dict.Dyn, &ceu_mem->idx_$n, &$asdst_src));
                                """
                            } else {
                                assrc("ceu_dict_get(ceu_dict.Dyn, &ceu_mem->idx_$n)")
                            }}
                            break;
                        }
                        default:
                            assert(0 && "bug found");
                    }
                }
                """
            }
            is Expr.Call -> {
                val bupc = ups.block(this)!!.toc(true)
                //val up = ups.ups[this]
                //val resume = (if (up is Expr.Resume) up else null)
                val resume = ups.path_until(this) { it is Expr.Resume }.let { es ->
                    when {
                        es.isEmpty() -> null
                        //es.drop(1).all { it is Expr.Group } -> es.first() as Expr.Resume
                        //else -> null
                        else -> es.first() as Expr.Resume
                    }
                }
                //println(resume?.tostr())
                val spawn = ups.path_until(this) { it is Expr.Spawn }.let { es ->
                    when {
                        es.isEmpty() -> null
                        //es.drop(1).all { it is Expr.Group } -> es.first() as Expr.Resume
                        //else -> null
                        else -> es.first() as Expr.Spawn
                    }
                }
                    //{ grp -> grp !is Expr.Group || grp.es.last() !is Expr.Call || !ups.ups[grp].let { it is Expr.Spawn && it.call==grp } }
                val iscall = (resume==null && spawn==null)
                val iscoros = (spawn?.coros != null)
                val frame = if (iscall) "(&ceu_frame_$n)" else "(ceu_coro_$n.Dyn->Bcast.Coro.frame)"
                val pass_evt = ups.intask(this) && (this.proto is Expr.Proto) && this.proto.task!!.first && (this.args.size == 0)

                val (args_sets,args_vs) = this.args.mapIndexed { i,e ->
                    Pair (
                        e.code(true, null) + "ceu_mem->arg_${i}_$n = ceu_acc;\n",
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
                    ${this.proto.code(true, null)}
                    CEU_Value ceu_proto_$n = ceu_acc;
                    if (ceu_proto_$n.type != CEU_VALUE_FUNC) {
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : call error : expected function");
                    }
                    CEU_Frame ceu_frame_$n = { &ceu_proto_$n.Dyn->Proto, $bupc, NULL, NULL, {} };
                """} +

                spawn.cond{"""
                // SPAWN/CORO ${this.tk.dump()}
                    ${iscoros.cond {
                        spawn!!.coros!!.code(true, null) + """
                        ceu_mem->coros_${spawn!!.n} = ceu_acc;
                        """
                    }}
                    ${this.proto.code(true, null)}
                    CEU_Value ceu_task_$n = ceu_acc;
                    CEU_Value ceu_coro_$n;
                    ${iscoros.cond { "CEU_Value ceu_ok_$n = { CEU_VALUE_BOOL, {.Bool=1} };" }}
                    ${if (!iscoros) {
                        """
                        ceu_ret = ceu_coro_create($bupc, &ceu_task_$n, &ceu_coro_$n);
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        """
                    } else {
                        """
                        ceu_ret = ceu_coro_create_in (
                            $bupc,
                            ceu_mem->coros_${spawn!!.n}.Dyn,
                            &ceu_task_$n,
                            &ceu_coro_$n,
                            &ceu_ok_$n.Bool
                        );
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : call error");
                        if (ceu_ok_$n.Bool) {
                            // call task only if ok
                        //} // closes below
                        """
                    }}
                """} +

                resume.cond{"""
                // RESUME ${this.tk.dump()}
                    ${this.proto.code(true, null)}
                    CEU_Value ceu_coro_$n = ceu_acc;
                    if (ceu_coro_$n.type<CEU_VALUE_BCAST || (ceu_coro_$n.Dyn->Bcast.status!=CEU_CORO_STATUS_YIELDED && ceu_coro_$n.Dyn->Bcast.status!=CEU_CORO_STATUS_TOGGLED)) {                
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${resume!!.tk.pos.file} : (lin ${resume.tk.pos.lin}, col ${resume.tk.pos.col}) : resume error : expected yielded task");
                    }
                """} +

                """
                    CEU_Value* ceu_args_$n[] = { ${if (pass_evt) "ceu_evt" else args_vs} };
                    ceu_ret = $frame->proto->f (
                        $frame,
                        ${if (pass_evt) -1 else this.args.size},
                        ceu_args_$n
                    );
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");
                    ${iscoros.cond{"}"}}
                    ${spawn.cond{ assrc(if (iscoros) "ceu_ok_$n" else "ceu_coro_$n") }}
                } // CALL (close)
                """
            }
        }
    }
}
