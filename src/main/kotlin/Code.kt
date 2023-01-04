import java.lang.Integer.min

class Coder (val outer: Expr.Do, val ups: Ups) {
    val tags = TAGS.map { Pair(it,it.drop(1).replace('.','_')) }.toMutableList()
    val tops: Triple<MutableList<String>, MutableList<String>, MutableList<String>> = Triple(mutableListOf(),mutableListOf(), mutableListOf())
    val mem: String = outer.mem()
    val code: String = outer.code()

    fun Expr.Do.toc (isptr: Boolean): String {
        return "ceu_mem->block_${this.n}".let {
            if (isptr) "(&($it))" else it
        }
    }

    fun Expr.id2c (dcl: Dcl, upv: Int): String {
        val (Mem,mem) = if (upv == 2) Pair("Upvs","upvs") else Pair("Mem","mem")
        val start = if (upv==2) this else dcl.blk
        val fup = ups.first(start) { it is Expr.Proto }
        val N = if (upv==2) 0 else {
            ups
                .all_until(this) { it==dcl.blk }  // go up until find dcl blk
                .count { it is Expr.Proto }          // count protos in between acc-dcl
        }
        return when {
            (fup == null) -> "(ceu_${mem}_${outer.n}->${dcl.id})"
            (N == 0) -> "(ceu_${mem}->${dcl.id})"
            else -> "(((CEU_Proto_${Mem}_${fup.n}*) ceu_frame ${"->proto->up".repeat(N)}->${mem})->${dcl.id})"
        }
    }

    fun Expr.non_fake_task_c (): String? {
        val n = ups     // find first non fake
            .all_until(this) { it is Expr.Proto && it.task!=null && !it.task.first }
            .filter { it is Expr.Proto } // but count all protos in between
            .count()
        return if (n == 0) null else "(ceu_frame${"->proto->up".repeat(n-1)})"
    }

    fun Expr.gcall (): Boolean {
        return ups.ups[this].let { it is Expr.Call && it.proto.let {
            it is Expr.Acc && it.tk.str in EXPOSE
        } }
    }

    fun Expr.isdst (): Boolean {
        return ups.ups[this].let { it is Expr.Set && it.dst==this }
    }
    fun Expr.asdst_src (): String {
        return "(ceu_mem->set_${(ups.ups[this] as Expr.Set).n})"
    }
    fun Expr.assrc (v: String): String {
        return if (this.isdst()) {
            "ceu_acc = (CEU_Value) { CEU_VALUE_NIL };\n"
        } else {
            "ceu_acc = $v;\n"
        }
    }

    fun Expr.gc_chk (): String {
        return when (this) {
            is Expr.Proto, is Expr.Tuple, is Expr.Vector, is Expr.Dict -> {
                "ceu_gc_chk(ceu_acc.Dyn);\n"
            }
            else -> ""
        }
    }

    fun Expr.code(): String {
        if (this.isdst()) {
            assert(this is Expr.Acc || this is Expr.Index || this is Expr.Pub)
        }
        return when (this) {
            is Expr.Proto -> {
                val isfunc = (this.tk.str == "func")
                val istask = (this.tk.str == "task")
                val type = """ // TYPE ${this.tk.dump()}
                typedef struct {
                    ${(ups.upvs_protos_refs[this] ?: emptySet()).map {
                        "CEU_Value $it;"
                    }.joinToString("")}
                } CEU_Proto_Upvs_$n;
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
                    CEU_Proto_Upvs_$n* ceu_upvs = (CEU_Proto_Upvs_$n*) ceu_frame->proto->upvs.buf;
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
                                            ceu_mem->_${id}_ = ceu_coro->hold.block;
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
                                        ceu_gc_inc(ceu_args[ceu_i]);
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
                            if (ceu_coro->hold.block->bcast.up != NULL) {
                                // enclosing coro of enclosing block
                                ceu_ret = MIN(ceu_ret, ceu_bcast_dyn(ceu_coro->hold.block->bcast.up, &ceu_evt_$n));
                            } else {
                                // enclosing block
                                ceu_ret = MIN(ceu_ret, ceu_bcast_blocks(ceu_coro->hold.block, &ceu_evt_$n));
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
                    ${ups.first_block(this)!!.toc(true)},
                    ${if (ups.upvs_protos_noclos.contains(this)) 1 else 0},     // noclo must be perm=1
                    CEU_VALUE_${this.tk.str.uppercase()},
                    (CEU_Proto) {
                        ceu_frame,
                        ceu_proto_f_$n,
                        { ${ups.upvs_protos_refs[this]?.size ?: 0}, NULL },
                        { .Task = {
                            ${if (istask && this.task!!.second) 1 else 0},
                            sizeof(CEU_Proto_Mem_$n)
                        } }
                    }
                );
                ${(ups.upvs_protos_refs[this] ?: emptySet()).map {
                    val dcl = ups.assertIsDeclared(this, Pair(it,1), this.tk)
                    val btw = ups
                        .all_until(this) { dcl.blk==it }
                        .filter { it is Expr.Proto }
                        .count() // other protos in between myself and dcl, so it its an upref (upv=2)
                    val upv = min(2, btw)
                    "((CEU_Proto_Upvs_$n*)ceu_proto_$n->Ncast.Proto.upvs.buf)->${it} = ${ups.ups[this]!!.id2c(dcl,upv)};"   // TODO: use this.body to not confuse with args
                }.joinToString("\n")}
                assert(ceu_proto_$n != NULL);
                ${assrc("(CEU_Value) { CEU_VALUE_${this.tk.str.uppercase()}, {.Dyn=ceu_proto_$n} }")}
                """
            }
            is Expr.Do -> {
                val ES = this.es.mapIndexed { i,e ->
                    e.code() + (i<this.es.size-1).cond { e.gc_chk() }
                }.joinToString("")

                if (!this.isnest) ES else {
                    val up = ups.ups[this]
                    val bup = up?.let { ups.first_block(it) }
                    val f_b = up?.let { ups.first_proto_or_block(it) }
                    val depth = when {
                        (f_b == null) -> "(0 + 1)"
                        (f_b is Expr.Proto) -> "(ceu_frame->up->depth + 1)"
                        else -> "(${bup!!.toc(false)}.depth + 1)"
                    }
                    val coro = if (ups.intask(this)) "ceu_coro" else "NULL"
                    """
                    { // BLOCK ${this.tk.dump()}
                        ceu_mem->block_$n = (CEU_Block) { $depth, ${if (f_b?.tk?.str == "task") 1 else 0}, NULL, {$coro,NULL,{NULL,NULL}} };
                        ${
                            (f_b is Expr.Proto && f_b.tk.str == "task").cond {
                                " ceu_coro->Bcast.Coro.block = &ceu_mem->block_$n;"
                            }
                        }
                        ${
                            (f_b is Expr.Do).cond {
                                "ceu_mem->block_${bup!!.n}.bcast.block = &ceu_mem->block_$n;"
                            }
                        }
                        do { // block
                            $ES
                        } while (0); // block
                        if (ceu_ret == CEU_RET_THROW) {
                            // must be before frees
                            ${(f_b == null).cond { "ceu_error_list_print();" }}
                        }
                        { // ceu_ret/ceu_acc: save/restore
                            CEU_RET   ceu_ret_$n = ceu_ret;
                            CEU_Value ceu_acc_$n = ceu_acc;
                            {
                                { // move up dynamic ceu_acc (return or error)
                                    ${
                                        (f_b != null).cond {
                                            val up1 = if (f_b is Expr.Proto) "ceu_frame->up" else bup!!.toc(true)
                                            """
                                            ${
                                                (f_b!!.tk.str == "task").cond {
                                                    "ceu_mem->block_$n.ispub = 0;"
                                                }
                                            }
                                            if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                                                ceu_ret = ceu_block_set($up1, ceu_acc.Dyn, 0);
                                                if (ceu_ret == CEU_RET_THROW) {
                                                    CEU_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                                    // prioritize scope error over whatever there is now
                                                    ceu_acc_$n = ceu_acc;
                                                }
                                                ceu_ret_$n = MIN(ceu_ret_$n, ceu_ret);
                                            }
                                            """
                                            }
                                    }
                                }
                                { // cleanup active nested spawns in this block
                                    ceu_bcasting++;
                                    assert(CEU_RET_RETURN == ceu_bcast_dyns(ceu_mem->block_$n.bcast.list.first, &CEU_EVT_CLEAR));
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
                                { // decrement refs
                                    ${ups.xblocks[this]!!.syms.keys
                                        .filter { it.first()!='_' && it.last()!='_' }
                                        .filter { this!=outer || it !in GLOBALS }
                                        .map { """
                                            ceu_gc_dec(&ceu_mem->$it);                    
                                        """ }
                                        .joinToString("")
                                    }
                                }
                                { // relink blocks
                                    ${
                                        (f_b is Expr.Do).cond {
                                            "ceu_mem->block_${bup!!.n}.bcast.block = NULL;"
                                        }
                                    }
                                    ${
                                        (f_b is Expr.Proto && f_b.tk.str == "task").cond {
                                            "ceu_coro->Bcast.Coro.block = NULL;"
                                        }
                                    }
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
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                val isperm = if (id[0] == '_') 0 else 1
                val dcl = ups.getDcl(this, this.tk.str)
                if (dcl!=null && dcl.upv==1 && !ups.upvs_vars_refs.contains(dcl)) {
                    err(this.tk, "var error : unreferenced upvar")
                }
                """
                { // DCL ${this.tk.dump()}
                    ${when {
                        !this.init -> ""
                        (this.src == null) -> "ceu_mem->$id = (CEU_Value) { CEU_VALUE_NIL };"
                        else -> this.src.code() + "ceu_mem->$id = ceu_acc;"
                    }}
                    ceu_mem->_${id}_ = ${ups.first_block(this)!!.toc(true)};   // can't be static b/c recursion
                    if (ceu_mem->${id}.type > CEU_VALUE_DYNAMIC) {
                        ceu_ret = ceu_block_set(ceu_mem->_${id}_, ceu_mem->${id}.Dyn, $isperm);
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                    }
                    ceu_gc_inc(&ceu_mem->${id});
                    ${assrc("ceu_mem->$id")}
                }
                """
            }
            is Expr.Set -> """
                { // SET ${this.tk.dump()}
                    ${this.src.code()}
                    ceu_mem->set_$n = ceu_acc;
                    ${this.dst.code()}
                    ${assrc("ceu_mem->set_$n")}
                }
                """
            is Expr.If -> """
                { // IF ${this.tk.dump()}
                    ${this.cnd.code()}
                    CEU_Value ceu_accx = ceu_acc;
                    if (ceu_as_bool(&ceu_accx)) {
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
                    CEU_Value ceu_accx = ceu_acc;
                    if (ceu_as_bool(&ceu_accx)) {
                        ${this.body.code()}
                        goto CEU_WHILE_START_$n;
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                }
                """
            is Expr.Catch -> """
                { // CATCH ${this.tk.dump()}
                    do { // catch
                        ${this.body.code()}
                    } while (0); // catch
                    CEU_CONTINUE_ON_CLEAR();
                    if (ceu_ret == CEU_RET_THROW) {
                        ceu_ret = CEU_RET_RETURN;
                        CEU_Value ceu_err = ceu_acc;
                        do {
                            ${this.cnd.code()}
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
                    ${this.ex.code()}
                    CEU_THROW_DO_MSG(ceu_acc, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception");
                }
                """
            is Expr.Defer -> {
                ups.xblocks[ups.first_block(this)!!]!!.defers!!.add(this.body.code())
                assrc("((CEU_Value) { CEU_VALUE_NIL })")
            }

            is Expr.Coros -> {
                """
                { // COROS ${this.tk.dump()}
                    ${this.max.cond { """
                        ${it.code()}
                        if (ceu_acc.type!=CEU_VALUE_NUMBER || ceu_acc.Number<=0) {                
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${it.tk.pos.file} : (lin ${it.tk.pos.lin}, col ${it.tk.pos.col}) : coroutines error : expected positive number");
                        }
                    """}}
                    assert(CEU_RET_RETURN == ceu_coros_create (
                        ${ups.first_block(this)!!.toc(true)},
                        ${if (this.max==null) 0 else "ceu_acc.Number"},
                        &ceu_acc
                    ));
                }
                """
            }
            is Expr.Coro -> {
                """
                { // CORO ${this.tk.dump()}
                    ${this.task.code()}
                    CEU_Value ceu_coro_$n;
                    ceu_ret = ceu_coro_create(${ups.first_block(this)!!.toc(true)}, &ceu_acc, &ceu_coro_$n);
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col})");
                    ${assrc("ceu_acc = ceu_coro_$n")}
                }
                """
            }
            is Expr.Spawn -> this.call.code()
            is Expr.CsIter -> {
                val loc = this.loc.str
                """
                { // ITER ${this.tk.dump()}
                    ${this.coros.code()}
                    ceu_mem->coros_$n = ceu_acc;
                    if (ceu_mem->coros_$n.type != CEU_VALUE_COROS) {                
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.coros.tk.pos.lin}, col ${this.coros.tk.pos.col}) : while error : expected coroutines");
                    }
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.open++;
                    ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->coros_$n.Dyn->Bcast.Coros.list.first} };
                    ceu_ret = CEU_RET_RETURN;
                    do { // iter
                CEU_ITER_$n:;
                        if (ceu_mem->$loc.Dyn == NULL) {
                            continue; // escape enclosing block
                        }
                        ceu_mem->hold_$n = ceu_mem->$loc.Dyn->hold.block;  
                        ceu_mem->$loc.Dyn->hold.block = ${this.body.toc(true)}; // tmp coro.hold to nested block
                        ${this.body.code()}
                        ceu_mem->$loc.Dyn->hold.block = ceu_mem->hold_$n;
                        ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->$loc.Dyn->Bcast.next} };
                        goto CEU_ITER_$n;
                    } while (0); // iter
                    assert(ceu_ret!=CEU_RET_YIELD && "bug found: cannot yield in iter");
                    if (ceu_mem->$loc.Dyn != NULL) { // repeat in case body error
                        ceu_mem->$loc.Dyn->hold.block = ceu_mem->hold_$n;
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
                val bupc = ups.first_block(this)!!.toc(true)
                val intask = (ups.first(this){ it is Expr.Proto }?.tk?.str == "task")
                val oktask = this.non_fake_task_c()
                """
                { // BCAST ${this.tk.dump()}
                    ${this.evt.code()}
                    ceu_mem->evt_$n = ceu_acc;
                    if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                        assert(CEU_RET_RETURN == ceu_block_set($bupc, ceu_mem->evt_$n.Dyn, 1));
                    }
                    ${this.xin.code()}
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
                            ${if (intask && oktask!=null) {
                                "ceu_ret = ceu_bcast_dyn(${oktask}->Task.coro, &ceu_mem->evt_$n);"
                            } else {
                                "ceu_err_$n = 1;"
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
                    ${this.arg.code()}
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
            is Expr.Resume -> this.call.code()
            is Expr.Toggle -> """
                ${this.on.code()}
                ceu_mem->on_$n = ceu_acc;
                ${this.coro.code()}
                if (ceu_acc.type<CEU_VALUE_BCAST || (ceu_acc.Dyn->Bcast.status!=CEU_CORO_STATUS_YIELDED && ceu_acc.Dyn->Bcast.status!=CEU_CORO_STATUS_TOGGLED)) {                
                    CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : toggle error : expected yielded/toggled coroutine");
                }
                ceu_acc.Dyn->Bcast.status = (ceu_as_bool(&ceu_mem->on_$n) ? CEU_CORO_STATUS_YIELDED : CEU_CORO_STATUS_TOGGLED);
                """
            is Expr.Pub -> """
                { // PUB
                    CEU_Dynamic* ceu_dyn_$n;
                    ${if (this.coro == null) {
                        "ceu_dyn_$n = ${this.non_fake_task_c()}->Task.coro;"
                    } else { """
                        ${this.coro.code()}
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
                    ${if (!this.isdst()) {
                        val inidx  = ups.all_until(this) { it is Expr.Index }.isNotEmpty()
                        val incall = (ups.ups[this] is Expr.Call)
                        """
                        { // PUB - GET
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
                    } else {
                        val src = this.asdst_src()
                        val task = (ups.first(this) { it is Expr.Proto && it.tk.str=="task" } as Expr.Proto).body.n 
                        """ // PUB - SET
                        if ($src.type > CEU_VALUE_DYNAMIC) {
                            ceu_ret = ceu_block_set(&ceu_mem->block_${task}, $src.Dyn, 1);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        }
                        ceu_gc_inc(&$src);
                        ceu_gc_dec(&ceu_dyn_$n->Bcast.Coro.frame->Task.pub);
                        ceu_dyn_$n->Bcast.Coro.frame->Task.pub = $src;
                        """
                    }}
                    CEU_PUB_$n:;
                }
                """
            is Expr.Track -> """
                { // TRACK
                    ${this.coro.code()}
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
                            val dcl = ups.assertIsDeclared(this, Pair(id,0), this.tk)
                            id = this.id2c(dcl,0)
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
                val dcl = ups.assertIsDeclared(this, Pair(id,this.tk_.upv), this.tk)
                if (!this.isdst()) {
                    assrc(this.id2c(dcl,this.tk_.upv)) // ACC ${this.tk.dump()}
                } else {
                    val src = this.asdst_src()
                    ups.assertIsDeclared(this, Pair("_${id}_",this.tk_.upv), this.tk)
                    if (dcl.upv > 0) {
                        err(tk, "set error : cannot reassign an upval")
                    }
                    val isperm = if (id[0] == '_') 0 else 1
                    """
                    { // ACC - SET
                        if ($src.type > CEU_VALUE_DYNAMIC) {
                            ceu_ret = ceu_block_set(${this.id2c(Dcl("_${id}_",dcl.upv,dcl.blk),this.tk_.upv)}, $src.Dyn, $isperm);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        }
                        ceu_gc_inc(&$src);
                        ceu_gc_dec(&${this.id2c(dcl,this.tk_.upv)});
                        ${this.id2c(dcl,this.tk_.upv)} = $src;
                    }
                    """
                }
            }
            is Expr.EvtErr -> {
                when (this.tk.str) {
                    "err" -> assrc("ceu_err")
                    "evt" -> {
                        val inidx = ups.all_until(this) { it is Expr.Index }.isNotEmpty()
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

            is Expr.Tuple -> """
                { // TUPLE ${this.tk.dump()}
                    ceu_mem->tup_$n = ceu_tuple_create(${ups.first_block(this)!!.toc(true)}, ${this.args.size});
                    assert(ceu_mem->tup_$n != NULL);
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        ceu_tuple_set(ceu_mem->tup_$n, $i, ceu_acc);
                        """
                    }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_mem->tup_$n} }")}
                }
            """
            is Expr.Vector -> """
                { // VECTOR ${this.tk.dump()}
                    ceu_mem->vec_$n = ceu_vector_create(${ups.first_block(this)!!.toc(true)});
                    assert(ceu_mem->vec_$n != NULL);
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        ceu_vector_set(ceu_mem->vec_$n, $i, ceu_acc);
                        """
                    }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=ceu_mem->vec_$n} }")}
                }
            """
            is Expr.Dict -> {
                """
                { // DICT ${this.tk.dump()}
                    ceu_mem->dict_$n = ceu_dict_create(${ups.first_block(this)!!.toc(true)});
                    assert(ceu_mem->dict_$n != NULL);
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            ceu_mem->key_$n = ceu_acc;
                            ${it.second.code()}
                            CEU_Value ceu_val_$n = ceu_acc;
                            ceu_dict_set(ceu_mem->dict_$n, &ceu_mem->key_$n, &ceu_val_$n);
                        }
                    """ }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_DICT, {.Dyn=ceu_mem->dict_$n} }")}
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
                    ${this.idx.code()}
                    ceu_mem->idx_$n = ceu_acc;
                    // COL
                    ${this.col.code()}
                    CEU_Value ceu_accx = ceu_acc;
                    ceu_ret = ceu_col_check(&ceu_accx, &ceu_mem->idx_$n);
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                    ${if (!this.isdst()) {
                        val x = this.has_pub_evt()
                        """
                        switch (ceu_acc.type) {
                            case CEU_VALUE_TUPLE:
                                ${(x!=null && !this.gcall()).cond { """
                                    if (ceu_acc.Dyn->Ncast.Tuple.mem[(int) ceu_mem->idx_$n.Number].type > CEU_VALUE_DYNAMIC) {
                                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.idx.tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : invalid index : cannot expose dynamic \"$x\" field");
                                    }
                                """ }}
                                ${assrc("ceu_acc.Dyn->Ncast.Tuple.mem[(int) ceu_mem->idx_$n.Number]")}
                                break;
                            case CEU_VALUE_VECTOR:
                                ceu_ret = ceu_vector_get(ceu_acc.Dyn, ceu_mem->idx_$n.Number);
                                CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_acc;
                                ${assrc("ceu_dict_get(ceu_dict.Dyn, &ceu_mem->idx_$n)")}
                                break;
                            }
                            default:
                                assert(0 && "bug found");
                        }
                        """                        
                    } else {
                        val src = this.asdst_src()
                        """
                        if ($src.type > CEU_VALUE_DYNAMIC) {
                            ceu_ret = ceu_block_set(ceu_acc.Dyn->hold.block, $src.Dyn, 0);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        }
                        switch (ceu_acc.type) {
                            case CEU_VALUE_TUPLE:
                                ceu_tuple_set(ceu_acc.Dyn, ceu_mem->idx_$n.Number, $src);
                                break;
                            case CEU_VALUE_VECTOR:
                                ceu_vector_set(ceu_acc.Dyn, ceu_mem->idx_$n.Number, $src);
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_acc;
                                assert(CEU_RET_RETURN == ceu_dict_set(ceu_dict.Dyn, &ceu_mem->idx_$n, &$src));
                                break;
                            }
                            default:
                                assert(0 && "bug found");
                        }
                        """
                    }}
                }
                """
            }
            is Expr.Call -> {
                val bupc = ups.first_block(this)!!.toc(true)
                val up = ups.ups[this]!!
                val resume = ups.all_until(up) { it is Expr.Resume }.let { es ->
                    when {
                        es.isEmpty() -> null
                        es.drop(1).all { it is Expr.Do && !it.isnest } -> es.first() as Expr.Resume
                        else -> null
                    }
                }
                val spawn = ups.all_until(up) { it is Expr.Spawn }.let { es ->
                    when {
                        es.isEmpty() -> null
                        es.drop(1).all { grp ->
                            (grp is Expr.Do && !grp.isnest) && (grp.es.last() is Expr.Call) && ups.ups[grp].let { it is Expr.Spawn && it.call==grp }
                        } -> es.first() as Expr.Spawn
                        else -> null
                    }
                }

                val iscall = (resume==null && spawn==null)
                val iscoros = (spawn?.coros != null)
                val frame = if (iscall) "(&ceu_frame_$n)" else "(ceu_coro_$n.Dyn->Bcast.Coro.frame)"
                val pass_evt = ups.intask(this) && (this.proto is Expr.Proto) && this.proto.task!!.first && (this.args.size == 0)

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
                    if (ceu_proto_$n.type != CEU_VALUE_FUNC) {
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : call error : expected function");
                    }
                    CEU_Frame ceu_frame_$n = { &ceu_proto_$n.Dyn->Ncast.Proto, $bupc, NULL, {} };
                """} +

                spawn.cond{"""
                // SPAWN/CORO ${this.tk.dump()}
                    ${iscoros.cond {
                        spawn!!.coros!!.code() + """
                        ceu_mem->coros_${spawn!!.n} = ceu_acc;
                        """
                    }}
                    ${this.proto.code()}
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
                    ${this.proto.code()}
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
