import java.lang.Integer.min

class Coder (val outer: Expr.Do, val ups: Ups, val defers: Defers, val vars: Vars, val clos: Clos, val unsf: Unsafe, val sta: Static) {
    val tops: Triple<MutableList<String>, MutableList<String>, MutableList<String>> = Triple(mutableListOf(),mutableListOf(), mutableListOf())
    val mem: String = outer.mem(defers)
    val code: String = outer.code()

    fun Expr.Do.toc (isptr: Boolean): String {
        return "ceu_mem->block_${this.n}".let {
            if (isptr) "(&($it))" else it
        }
    }

    fun Expr.id2c (xvar: Var, upv: Int): Pair<String,String> {
        val (Mem,mem) = if (upv == 2) Pair("Upvs","upvs") else Pair("Mem","mem")
        val start = if (upv==2) this else xvar.blk
        val fup = ups.first(start) { it is Expr.Proto }
        val N = if (upv==2) 0 else {
            ups
                .all_until(this) { it==xvar.blk }  // go up until find dcl blk
                .count { it is Expr.Proto }          // count protos in between acc-dcl
        }
        val idc = xvar.dcl.id.str.id2c(xvar.dcl.n)
        return when {
            (fup == null) -> Pair("(ceu_${mem}_${outer.n}->$idc)","(ceu_${mem}_${outer.n}->_${idc}_)")
            (N == 0) -> Pair("(ceu_${mem}->$idc)", "(ceu_${mem}->_${idc}_)")
            else -> Pair("(((CEU_Proto_${Mem}_${fup.n}*) ceu_frame ${"->proto->up_frame".repeat(N)}->${mem})->$idc)",
                         "(((CEU_Proto_${Mem}_${fup.n}*) ceu_frame ${"->proto->up_frame".repeat(N)}->${mem})->_${idc}_)")
        }
    }

    fun Expr.isdst (): Boolean {
        return ups.pub[this].let { it is Expr.Set && it.dst==this }
    }
    fun Expr.asdst_src (): String {
        return "(ceu_mem->set_${(ups.pub[this] as Expr.Set).n})"
    }
    fun Expr.assrc (v: String): String {
        return if (this.isdst()) {
            "ceu_acc = (CEU_Value) { CEU_VALUE_NIL };\n"
        } else {
            "ceu_acc = $v;\n"
        }
    }

    fun Expr.do_issafe (): Boolean {
        val blk = ups.first_block(this)!!
        return !unsf.dos.contains(blk)
    }

    fun Expr.code(): String {
        if (this.isdst()) {
            assert(this is Expr.Acc || this is Expr.Index || this is Expr.Pub)
        }
        return when (this) {
            is Expr.Proto -> {
                val isfunc = (this.tk.str == "func")
                val isx = (this.tk.str != "func")
                val blk = ups.first_block(this)!!

                val type = """ // TYPE ${this.tk.dump()}
                typedef struct {
                    ${(clos.protos_refs[this] ?: emptySet()).map {
                        "CEU_Value ${vars.id2c(this.body, it)};"
                    }.joinToString("")}
                } CEU_Proto_Upvs_$n;
                typedef struct {
                    ${this.args.map { (id,_) ->
                        val idc = vars.id2c(this.body, id.str)
                        """
                        CEU_Value $idc;
                        CEU_Block* _${idc}_;
                        """
                }.joinToString("")}
                    ${this.body.mem(defers)}
                } CEU_Proto_Mem_$n;
                """
                val func = """ // BODY ${this.tk.dump()}
                CEU_RET ceu_proto_f_$n (
                    CEU_Frame* ceu_frame,
                    CEU_BStack* ceu_bstack,
                    int ceu_n,
                    CEU_Value* ceu_args[]
                ) {
                    ${isfunc.cond{"""
                        CEU_Proto_Mem_$n _ceu_mem_;
                        CEU_Proto_Mem_$n* ceu_mem = &_ceu_mem_;
                        ceu_frame->mem = (char*) ceu_mem;
                    """}}
                    ${isx.cond{"""
                        CEU_Dyn* ceu_x = ceu_frame->X.x;
                        #ifdef CEU_DEBUG
                        printf("pc=%2d, status=%d, coro=%p, evt=%d\n", ceu_frame->X.pc, ceu_x->Bcast.status, ceu_x, ceu_n==CEU_ARG_EVT && ceu_args[0]==&CEU_EVT_CLEAR);
                        #endif
                        CEU_Proto_Mem_$n* ceu_mem = (CEU_Proto_Mem_$n*) ceu_frame->mem;
                        CEU_Value* ceu_evt = &CEU_EVT_NIL;
                        if (ceu_n == CEU_ARG_EVT) {
                            ceu_evt = ceu_args[0];
                        }
                        assert(ceu_x->Bcast.status==CEU_X_STATUS_YIELDED || ceu_evt==&CEU_EVT_CLEAR);
                        //if (ceu_evt != &CEU_EVT_CLEAR) {
                            ceu_x->Bcast.status = CEU_X_STATUS_RESUMED;
                        //}
                    """}}
                    CEU_RET ceu_ret = (ceu_n == CEU_ARG_ERR) ? CEU_RET_THROW : CEU_RET_RETURN;
                    CEU_Proto_Mem_$n* ceu_mem_$n = ceu_mem;
                    CEU_Proto_Upvs_$n* ceu_upvs = (CEU_Proto_Upvs_$n*) ceu_frame->proto->upvs.buf;
                    """ +
                    """ // WHILE
                    do { // func
                        ${isx.cond{"""
                        switch (ceu_frame->X.pc) {
                            case -1:
                                assert(0 && "bug found");
                                break;
                            case 0: {
                                //ceu_x->tphold = 1;   // do not allow started coro to change scope
                                CEU_CONTINUE_ON_CLEAR_THROW(); // may start with clear w/ coroutine() w/o resume
                        """}}
                            // BODY
                            ${this.body.code()}
                        ${isx.cond{"}\n}\n"}}
                    } while (0); // func
                    """ + isx.cond{ """  // TERMINATE
                    assert(ceu_x->Bcast.status != CEU_X_STATUS_TERMINATED);
                    ceu_x->Bcast.status = CEU_X_STATUS_TERMINATED;
                    ceu_x->Bcast.X.frame->X.pub = ceu_acc;
                    ceu_frame->X.pc = -1;
                    int intasks = (ceu_x->Bcast.X.up_tasks != NULL);

                    if (ceu_n==-1 && ceu_evt==&CEU_EVT_CLEAR) {
                        // do not signal termination: clear comes from clearing enclosing block,
                        // which also clears all possible interested awaits
                    } else if (ceu_ret == CEU_RET_THROW) {
                        // do not signal termination: throw clears enclosing block,
                        // which also clears all possible interested awaits
                    } else if (ceu_x->type == CEU_VALUE_X_TASK) {
                        // only signal on normal termination

                        // ceu_ret/ceu_acc: save/restore
                        CEU_RET   ceu_ret_$n = ceu_ret;
                        CEU_Value ceu_acc_$n = ceu_acc;

                        CEU_Value ceu_evt_$n = { CEU_VALUE_X_TASK, {.Dyn=ceu_x} };
                        CEU_BStack ceu_bstack_$n = { ceu_x->up_dyns.dyns->up_block, ceu_bstack };
                        if (ceu_x->up_dyns.dyns->up_block->up_x != NULL) {
                            // enclosing coro of enclosing block
                            ceu_ret = MIN(ceu_ret, ceu_bcast_dyn(&ceu_bstack_$n, ceu_x->up_dyns.dyns->up_block->up_x, &ceu_evt_$n));
                        } else {
                            // enclosing block
                            ceu_ret = MIN(ceu_ret, ceu_bcast_blocks(&ceu_bstack_$n, ceu_x->up_dyns.dyns->up_block, &ceu_evt_$n, NULL));
                        }
                        if (ceu_bstack_$n.block == NULL) {
                            return ceu_ret;
                        }
                    
                        if (intasks) {
                            ceu_hold_rem(ceu_x);
                            ceu_dyn_free(ceu_x);
                        }

                        if (ceu_ret_$n==CEU_RET_THROW || ceu_ret!=CEU_RET_THROW) {
                            ceu_acc = ceu_acc_$n;
                        } else {
                            // do not restore acc: we were ok, but now we did throw
                        }
                    }
                    """} + """
                    return ceu_ret;
                }
                """
                tops.second.add(type)
                //println(listOf(this.tk.pos, clos.protos_noclos.contains(this)))
                tops.third.add(func)
                """
                CEU_Dyn* ceu_proto_$n = ceu_proto_create (
                    CEU_VALUE_P_${this.tk.str.uppercase()},
                    (CEU_Proto) {
                        ${if (blk == outer) "NULL" else "ceu_frame"},
                        ceu_proto_f_$n,
                        { ${clos.protos_refs[this]?.size ?: 0}, NULL },
                        { .X = {
                            sizeof(CEU_Proto_Mem_$n)
                        } }
                    },
                    &${blk.toc(true)}->dn_dyns,
                    ${if (clos.protos_noclos.contains(this)) "CEU_HOLD_FIX" else "CEU_HOLD_NON"}
                );
                ${(clos.protos_refs[this] ?: emptySet()).map {
                    val dcl = vars.assertIsDeclared(this, Pair(it,1), this.tk)
                    val idc = vars.id2c(this.body, it)
                    val btw = ups
                        .all_until(this) { dcl.blk==it }
                        .filter { it is Expr.Proto }
                        .count() // other protos in between myself and dcl, so it its an upref (upv=2)
                    val upv = min(2, btw)
                    """
                    {
                        CEU_Value* ceu_up = &${ups.pub[this]!!.id2c(dcl,upv).first};
                        if (ceu_up->type > CEU_VALUE_DYNAMIC) {
                            assert(ceu_block_chk_set_mutual(ceu_proto_$n,ceu_up->Dyn));
                            //assert(CEU_RET_RETURN == ceu_block_set(ceu_proto_$n, ceu_up->Dyn->up_dyns.dyns, ceu_up->Dyn->tphold));
                        }
                        ceu_gc_inc(ceu_up);
                        ((CEU_Proto_Upvs_$n*)ceu_proto_$n->Ncast.Proto.upvs.buf)->${idc} = *ceu_up;
                    }
                    """   // TODO: use this.body (ups.ups[this]?) to not confuse with args
                }.joinToString("\n")}
                assert(ceu_proto_$n != NULL);
                ${assrc("(CEU_Value) { CEU_VALUE_P_${this.tk.str.uppercase()}, {.Dyn=ceu_proto_$n} }")}
                """
            }
            is Expr.Export -> this.body.es.map { it.code() }.joinToString("")   // skip do{}
            is Expr.Do -> {
                val body = this.es.map { it.code() }.joinToString("")   // before defers[this] check
                val up = ups.pub[this]
                val bup = up?.let { ups.first_block(it) }
                val f_b = up?.let { ups.first_proto_or_block(it) }
                val depth = when {
                    (f_b == null) -> "(0 + 1)"
                    (f_b is Expr.Proto) -> "(ceu_frame->up_block->depth + 1)"
                    else -> "(${bup!!.toc(false)}.depth + 1)"
                }
                val x = if (ups.intask(this)) "ceu_x" else "NULL"
                val xvars = vars.pub[this]!!.values.let { dcls ->
                    val args = if (f_b !is Expr.Proto) emptySet() else f_b.args.map { it.first.str }.toSet()
                    dcls.filter { it.dcl.init }
                        .filter { !GLOBALS.contains(it.dcl.id.str) }
                        .filter { !(f_b is Expr.Proto && args.contains(it.dcl.id.str)) }
                        .map    { it.dcl.id.str.id2c(it.dcl.n) }
                }
                """
                { // BLOCK ${this.tk.dump()}
                    ceu_mem->block_$n = (CEU_Block) { $depth, ${if (f_b?.tk?.str != "func") 1 else 0}, $x, {0,0,NULL,&ceu_mem->block_$n}, NULL };
                    void* ceu_block = &ceu_mem->block_$n;   // generic name to debug
                    ${(this == outer).cond { "ceu_block_global = ceu_block;" }}
                    #ifdef CEU_DEBUG
                    printf(">>> BLOCK = %p in %p\n", &ceu_mem->block_$n, $x);
                    #endif
                    ${(f_b == null).cond { """
                    {   // ... for main block
                        CEU_Dyn* tup = ceu_tuple_create(&ceu_mem->block_$n.dn_dyns, ceu_argc);
                        for (int i=0; i<ceu_argc; i++) {
                            CEU_Dyn* vec = ceu_vector_from_c_string(&ceu_mem->block_$n.dn_dyns, ceu_argv[i]);
                            assert(ceu_tuple_set(tup, i, (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=vec} }));
                        }
                        ceu_mem->_dot__dot__dot_ = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=tup} };
                    }
                    """ }}
                    ${(f_b is Expr.Proto).cond { // initialize parameters from outer proto
                        f_b as Expr.Proto
                        val istask = (f_b.tk.str != "func")
                        val dots = (f_b.args.lastOrNull()?.first?.str == "...")
                        val args_n = f_b.args.size - 1
                        """
                        {
                            int ceu_i = 0;
                            ${f_b.args.filter { it.first.str!="..." }.map {
                                val idc = vars.id2c(this, it.first.str)
                                """
                                ${istask.cond { """
                                    if (ceu_x->Bcast.X.up_tasks != NULL) {
                                        ceu_mem->_${idc}_ = ceu_x->up_dyns.dyns->up_block;
                                    } else
                                """}}
                                { // else
                                    ceu_mem->_${idc}_ = &ceu_mem->block_$n;
                                }
                                if (ceu_i < ceu_n) {
                                    if (ceu_args[ceu_i]->type > CEU_VALUE_DYNAMIC) {
                                        if (!ceu_block_chk_set(ceu_args[ceu_i]->Dyn, &ceu_mem->_${idc}_->dn_dyns, ${if (this.do_issafe()) "CEU_HOLD_NON" else "CEU_HOLD_VAR"})) {
                                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                                        }
                                    }
                                    ceu_mem->$idc = *ceu_args[ceu_i];
                                    ceu_gc_inc(&ceu_mem->$idc);
                                } else {
                                    ceu_mem->$idc = (CEU_Value) { CEU_VALUE_NIL };
                                }
                                ceu_i++;
                                """
                            }.joinToString("")}
                            ${dots.cond {
                                val idc = f_b.args.last()!!.first.str.id2c(null)
                                """
                                int ceu_tup_n = MAX(0,ceu_n-$args_n);
                                CEU_Dyn* ceu_tup = ceu_tuple_create(&ceu_mem->block_$n.dn_dyns, ceu_tup_n);
                                for (int i=0; i<ceu_tup_n; i++) {
                                    assert(ceu_tuple_set(ceu_tup, i, *ceu_args[$args_n+i]));
                                }
                                ceu_mem->$idc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_tup} };
                                ceu_gc_inc(&ceu_mem->$idc);
                            """ }}
                        }
                        """ 
                    }}
                    ${
                        (f_b is Expr.Proto && f_b.tk.str != "func").cond {
                            "ceu_x->Bcast.X.dn_block = &ceu_mem->block_$n;"
                        }
                    }
                    ${
                        (f_b is Expr.Do).cond {
                            "ceu_mem->block_${bup!!.n}.dn_block = &ceu_mem->block_$n;"
                        }
                    }
                    { // because of "decrement refs" below
                        ${xvars.map {
                            if (it in listOf("evt","_")) "" else """
                                ceu_mem->$it = (CEU_Value) { CEU_VALUE_NIL };
                        """ }.joinToString("")
                        }
                    }
                    { // reset defers
                        ${defers.pub[this]!!.map {
                            "ceu_mem->defer_${it.key.n} = 0;\n"
                        }.joinToString("")}
                    }
                    do { // block
                        $body
                    } while (0); // block
                    #ifdef CEU_DEBUG
                    printf("<<< BLOCK = %d/%p in %p\n", $n, &ceu_mem->block_$n, $x);
                    #endif
                    if (ceu_ret == CEU_RET_THROW) {
                        // must be before frees
                        ${(f_b == null).cond { "ceu_error_list_print();" }}
                    }
                    { // ceu_ret/ceu_acc: save/restore
                        CEU_RET   ceu_ret_$n = ceu_ret;
                        CEU_Value ceu_acc_$n = ceu_acc;
                        ${(f_b is Expr.Proto).cond {
                            """
                            if (ceu_ret != CEU_RET_THROW) {
                                ceu_gc_inc(&ceu_acc);
                            }
                            """
                        }}
                        {
                            ceu_bstack_clear(ceu_bstack, &ceu_mem->block_$n);
                            { // move up dynamic ceu_acc (return or error)
                                ${
                                    (f_b != null).cond {
                                        val up1 = if (f_b is Expr.Proto) "ceu_frame->up_block" else bup!!.toc(true)
                                        """
                                        ${
                                            (f_b!!.tk.str != "func").cond {
                                                "ceu_mem->block_$n.ispub = 0;"
                                            }
                                        }
                                        if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                                            if (!ceu_block_chk_set(ceu_acc.Dyn, &$up1->dn_dyns, CEU_HOLD_NON)) {
                                                CEU_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : block escape error : incompatible scopes");
                                                // prioritize scope error over whatever there is now
                                                ceu_acc_$n = CEU_ERR_ERROR;
                                                ceu_ret_$n = MIN(ceu_ret_$n, CEU_RET_THROW);
                                            }
                                        }
                                        """
                                        }
                                }
                            }
                            int dead; {
                                // cleanup active nested spawns in this block
                                CEU_BStack ceu_bstack_$n = { &ceu_mem->block_$n, ceu_bstack };
                                assert(CEU_RET_RETURN == ceu_bcast_dyns(&ceu_bstack_$n, &ceu_mem->block_$n.dn_dyns, &CEU_EVT_CLEAR));
                                dead = (ceu_bstack!=NULL && ceu_bstack_$n.block==NULL);
                            }
                            { // DEFERS ${this.tk.dump()}
                                ceu_ret = CEU_RET_RETURN;
                                ${defers.pub[this]!!.map{it.value}.reversed().joinToString("")}
                                if (ceu_ret_$n!=CEU_RET_THROW && ceu_ret==CEU_RET_THROW) {
                                    ceu_acc_$n = ceu_acc;
                                }
                                ceu_ret_$n = MIN(ceu_ret_$n, ceu_ret);
                            }
                            { // decrement refs
                                ${xvars.map { if (it in listOf("evt","_")) "" else
                                    """
                                    if (ceu_mem->$it.type > CEU_VALUE_DYNAMIC) {
                                        ceu_gc_dec(&ceu_mem->$it,
                                                   (ceu_mem->$it.Dyn->up_dyns.dyns != NULL) &&
                                                   (ceu_mem->$it.Dyn->up_dyns.dyns->up_block == &ceu_mem->block_$n)
                                        );
                                    }
                                    """
                                }.joinToString("")}
                                ${(f_b is Expr.Proto).cond {
                                    (f_b as Expr.Proto).args.map {
                                        val idc = vars.id2c(this, it.first.str)
                                        "ceu_gc_dec(&ceu_mem->$idc, 1);"
                                    }.joinToString("")
                                }}
                            }
                            if (!dead) {
                                // blocks: relink up, free down
                                ${
                                    (f_b is Expr.Do).cond {
                                        "ceu_mem->block_${bup!!.n}.dn_block = NULL;"
                                    }
                                }
                                ${
                                    (f_b is Expr.Proto && f_b.tk.str != "func").cond {
                                        "ceu_x->Bcast.X.dn_block = NULL;"
                                    }
                                }
                                ceu_block_free(&ceu_mem->block_$n);
                            }
                        }
                        ceu_acc = ceu_acc_$n;
                        ceu_ret = ceu_ret_$n;
                        CEU_CONTINUE_ON_CLEAR_THROW();
                    } // ceu_ret/ceu_acc: save/restore
                }
                """
            }
            is Expr.Dcl -> {
                val id = this.id.str
                val idc = id.id2c(this.n)
                val bupc = ups.first_block(this)!!.toc(true)
                val xvar = vars.get(this, id)
                if (xvar!=null && xvar.dcl.id.upv==1 && !clos.vars_refs.contains(xvar)) {
                    err(this.tk, "var error : unreferenced upvar")
                }
                """
                { // DCL ${this.tk.dump()}
                    ceu_mem->$idc = (CEU_Value) { CEU_VALUE_NIL };      // src may fail (protect var w/ nil)
                    ${(this.init && this.src!=null).cond {
                        this.src!!.code() + """
                            if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                                if (!ceu_block_chk_set(ceu_acc.Dyn, &$bupc->dn_dyns, ${if (this.tmp && this.do_issafe()) "CEU_HOLD_NON" else "CEU_HOLD_VAR"})) {
                                    CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : declaration error : incompatible scopes");
                                }
                            }
                        """
                    }}
                    ${if (id in listOf("evt","_")) "" else {
                        """
                        ${when {
                            /*
                            this.poly -> """
                                ceu_mem->$idc = (CEU_Value) { CEU_VALUE_DICT, {.Dyn=ceu_dict_create()} };
                            """
                            */
                            !this.init -> ""
                            (this.src == null) -> ""
                            else -> "ceu_mem->$idc = ceu_acc;"
                        }}
                        ceu_mem->_${idc}_ = $bupc;   // can't be static b/c recursion
                        ceu_gc_inc(&ceu_mem->${idc});
                        #if 1
                            ${assrc("ceu_mem->$idc")}
                        #else // b/c of ret scope
                            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                        #endif
                        """
                    }}
                }
                """
            }
            is Expr.Set -> {
                """
                { // SET ${this.tk.dump()}
                    ${this.src.code()}
                    ceu_mem->set_$n = ceu_acc;
                    ${this.dst.code()}
                    #if 1
                        ${assrc("ceu_mem->set_$n")}
                    #else // b/c of ret scope
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                    #endif
                }
                """
            }
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
            is Expr.Loop -> """
                { // LOOP ${this.tk.dump()}
                CEU_LOOP_NEXT_${this.nn}:;
                    ${this.body.code()}
                    goto CEU_LOOP_NEXT_${this.nn};
                CEU_LOOP_DONE_${this.nn}:;
                    //ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
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
                            if (ceu_err.type>CEU_VALUE_DYNAMIC && ceu_err.Dyn->tphold!=CEU_HOLD_NON) {
                                CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : rethrow error : incompatible scopes");
                            }
                            CEU_THROW_DO(ceu_err, continue); // uncaught, rethrow
                        }
                        ceu_gc_dec(&ceu_err, 0);    // do not check, bc throw value may be captured in assignment
                        ${assrc("ceu_acc = ceu_err")}
                        /*
                        ceu_gc_dec(&ceu_err, 1);
                        ${assrc("ceu_acc = (CEU_Value) { CEU_VALUE_NIL }")}
                        */
                    }
                }
                """
            is Expr.Defer -> {
                defers.pub[ups.first_block(this)!!]!![this] = """
                    if (ceu_mem->defer_$n) {
                        ${this.body.code()}
                    }
                """
                """
                ceu_mem->defer_$n = 1;
                ${assrc("((CEU_Value) { CEU_VALUE_NIL })")}
                """
            }
            is Expr.Enum -> ""
            is Expr.Data -> ""
            is Expr.Pass -> this.e.code()

            is Expr.Spawn -> this.call.code()
            is Expr.Bcast -> {
                val bupc = ups.first_block(this)!!.toc(true)
                val intask = (ups.first(this){ it is Expr.Proto }?.tk?.str == "task")
                val oktask = ups.true_x_c(this, "task")
                """
                { // BCAST ${this.tk.dump()}
                    ${intask.cond {"""
                    ceu_frame->X.pc = $n;   // because of clear
                case $n:
                    CEU_CONTINUE_ON_CLEAR_THROW();
                    """}}

                    ${this.evt.code()}
                    ceu_mem->evt_$n = ceu_acc;
                    ceu_gc_inc(&ceu_mem->evt_$n);

                    if (ceu_mem->evt_$n.type>CEU_VALUE_DYNAMIC) {
                        if (!ceu_block_chk_hold(CEU_HOLD_EVT,ceu_mem->evt_$n.Dyn->tphold)) {
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.evt.tk.pos.file} : (lin ${this.evt.tk.pos.lin}, col ${this.evt.tk.pos.col}) : broadcast error : incompatible scopes");
                        }
                        //if (!CEU_ISGLBDYN(ceu_mem->evt_$n.Dyn)) { // (already tested inside rec below)
                            ceu_block_set(ceu_mem->evt_$n.Dyn, NULL, CEU_HOLD_EVT);
                        //}
                    }
                    
                    ${this.xin.code()}
                    int ceu_err_$n = 0;
                    CEU_BStack ceu_bstack_$n = { $bupc, ceu_bstack };
                    if (ceu_acc.type == CEU_VALUE_X_TASK) {
                        ceu_ret = ceu_bcast_dyn(&ceu_bstack_$n, ceu_acc.Dyn, &ceu_mem->evt_$n);
                    } else if (ceu_acc.type == CEU_VALUE_TAG) {
                        if (ceu_acc.Tag == CEU_TAG_global) {
                            ceu_ret = ceu_bcast_blocks(&ceu_bstack_$n, &ceu_mem_${outer.n}->block_${outer.n}, &ceu_mem->evt_$n, NULL);
                        } else if (ceu_acc.Tag == CEU_TAG_local) {
                            ceu_ret = ceu_bcast_blocks(&ceu_bstack_$n, $bupc, &ceu_mem->evt_$n, NULL);
                        } else if (ceu_acc.Tag == CEU_TAG_task) {
                            ${if (intask && oktask!=null) {
                                "ceu_ret = ceu_bcast_dyn(&ceu_bstack_$n, ${oktask}->X.x, &ceu_mem->evt_$n);"
                            } else {
                                "ceu_err_$n = 1;"
                            }}
                        } else {
                            ceu_err_$n = 1;
                        }
                    } else {
                        ceu_err_$n = 1;
                    }
                    
                    if (ceu_bstack_$n.block == NULL) {
                        return ceu_ret;
                    }
                    
                    ceu_gc_dec(&ceu_mem->evt_$n, 1);
                    
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
                    ceu_frame->X.pc = $n;      // next resume
                    ceu_x->Bcast.status = CEU_X_STATUS_YIELDED;
                    if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                        if (!ceu_block_chk_set(ceu_acc.Dyn, &ceu_frame->up_block->dn_dyns, CEU_HOLD_NON)) {
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : yield error : incompatible scopes");
                        }
                    }
                    return CEU_RET_YIELD;
                case $n:                    // resume here
                    if (ceu_ret != CEU_RET_THROW) {
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                    }
                    CEU_CONTINUE_ON_CLEAR_THROW();
                    if (ceu_n == CEU_ARG_EVT) {
                        // resume single argument
                    } else {
                        assert(ceu_n <= 1 && "bug found : not implemented : multiple arguments to resume");
                        if (ceu_n == 0) {
                            // no argument
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
                ${this.task.code()}
                if (ceu_acc.type<CEU_VALUE_BCAST || (ceu_acc.Dyn->Bcast.status!=CEU_X_STATUS_YIELDED && ceu_acc.Dyn->Bcast.status!=CEU_X_STATUS_TOGGLED)) {                
                    CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.task.tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col}) : toggle error : expected yielded/toggled coroutine");
                }
                ceu_acc.Dyn->Bcast.status = (ceu_as_bool(&ceu_mem->on_$n) ? CEU_X_STATUS_YIELDED : CEU_X_STATUS_TOGGLED);
                """
            is Expr.Pub -> """
                { // PUB
                    CEU_Dyn* ceu_dyn_$n;
                    ${this.x.code()}
                    if (ceu_acc.type != CEU_VALUE_X_TASK) {                
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : pub error : expected task");
                    }
                    ceu_dyn_$n = ceu_acc.Dyn;
                    ${if (!this.isdst()) {
                        assrc("ceu_dyn_$n->Bcast.X.frame->X.pub")
                    } else {
                        val src = this.asdst_src()
                        val task = (ups.first(this) { it is Expr.Proto && it.tk.str!="func" } as Expr.Proto).body.n 
                        """ // PUB - SET
                        if ($src.type > CEU_VALUE_DYNAMIC) {
                            //ceu_ret = ceu_block_set($src.Dyn, ceu_x->up_dyns.dyns, CEU_HOLD_FIX);
                            if (!ceu_block_chk_set($src.Dyn, &ceu_mem->block_$task.dn_dyns, CEU_HOLD_PUB)) {
                                CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                            }
                        }
                        ceu_gc_inc(&$src);
                        ceu_gc_dec(&ceu_dyn_$n->Bcast.X.frame->X.pub, 1);
                        ceu_dyn_$n->Bcast.X.frame->X.pub = $src;
                        """
                    }}
                }
                """
            is Expr.Self -> assrc("(CEU_Value) { CEU_VALUE_X_${this.tk.str.uppercase()}, {.Dyn=${ups.true_x_c(this,this.tk.str)}->X.x} }")

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
                        val x1 = read()
                        ret += if (x1 != '$') x1 else {
                            val (l,c) = Pair(lin,col)
                            var id = ""
                            var no = ""
                            while (i < it.length) {
                                val x2 = read()
                                if (x2.isLetterOrDigit() || x2=='_' || x2=='-') {
                                    id += x2
                                } else {
                                    no += x2
                                    break
                                }
                            }
                            if (id.length == 0) {
                                err(tk, "native error : (lin $l, col $c) : invalid identifier")
                            }
                            val dcl = vars.assertIsDeclared(this, Pair(id,0), this.tk)
                            id = this.id2c(dcl,0).first
                            "($id)$no"
                        }
                    }
                    ret
                }
                val (pre,pos) = when (this.tk_.tag) {
                    null -> Pair(null, body)
                    ":pre" -> Pair(body, "")
                    ":ceu" -> Pair(null, assrc(body))
                    else -> {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        Pair(null, assrc("((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} })"))
                    }
                }
                if (pre != null) {
                    tops.first.add(pre)
                }
                pos
            }
            is Expr.Acc -> {
                val id = this.tk.str
                val xvar = vars.get(this, id)!!
                val (idc,_idc_) = this.id2c(xvar,this.tk_.upv)
                when {
                    !this.isdst() -> when {
                        //!xvar.dcl.poly ->
                        true -> assrc(idc) // ACC ${this.tk.dump()}
                        /*xvar.dcl.poly -> """
                            assert($idc.type==CEU_VALUE_DICT && "TODO");
                            CEU_Value ceu_tag = { CEU_VALUE_TAG, {.Tag=CEU_TAG_number} };
                            CEU_Value ceu_fld = ceu_dict_get($idc.Dyn, &ceu_tag);
                            ${assrc("ceu_fld")}
                        """*/
                        else -> error("impossible case")
                    }
                    this.isdst() -> {
                        val src = this.asdst_src()
                        if (xvar.dcl.id.upv > 0) {
                            err(tk, "set error : cannot reassign an upval")
                        }
                        //val poly = (ups.pub[this] as Expr.Set).poly
                        when {
                            /*(poly != null) -> """
                                assert($idc.type==CEU_VALUE_DICT && "bug found");
                                CEU_Value ceu_tag_$n = { CEU_VALUE_TAG, {.Tag=CEU_TAG_${poly.str.tag2c()}} };
                                ceu_dict_set($idc.Dyn, &ceu_tag_$n, &$src);
                            """*/
                            //(poly == null) -> """
                            true -> """
                                { // ACC - SET
                                    if ($src.type > CEU_VALUE_DYNAMIC) {
                                        if (!ceu_block_chk_set($src.Dyn, &${_idc_}->dn_dyns, ${if (xvar.dcl.tmp) "CEU_HOLD_NON" else "CEU_HOLD_VAR"})) {
                                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                                        }
                                    }
                                    ceu_gc_inc(&$src);
                                    ceu_gc_dec(&$idc, 1);
                                    $idc = $src;
                                }
                            """
                            else -> error("impossible case")
                        }
                    }
                    else -> error("impossible case")
                }
            }
            is Expr.EvtErr -> {
                when (this.tk.str) {
                    "err" -> assrc("ceu_err")
                    "evt" -> assrc("(*ceu_evt)")
                    else -> error("impossible case")
                }
            }
            is Expr.Nil -> assrc("((CEU_Value) { CEU_VALUE_NIL })")
            is Expr.Tag -> assrc("((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_${this.tk.str.tag2c()}} })")
            is Expr.Bool -> assrc("((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} })")
            is Expr.Char -> assrc("((CEU_Value) { CEU_VALUE_CHAR, {.Char=${this.tk.str}} })")
            is Expr.Num -> assrc("((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} })")

            is Expr.Tuple -> """
                { // TUPLE ${this.tk.dump()}
                    ceu_mem->tup_$n = ceu_tuple_create(&${ups.first_block(this)!!.toc(true)}->dn_dyns, ${this.args.size});
                    assert(ceu_mem->tup_$n != NULL);
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                            assert(ceu_mem->tup_$n->up_dyns.dyns != NULL);
                            if (ceu_acc.Dyn->up_dyns.dyns->up_block->depth > ceu_mem->tup_$n->up_dyns.dyns->up_block->depth) {
                                ceu_hold_rem(ceu_mem->tup_$n);
                                ceu_hold_add(ceu_acc.Dyn->up_dyns.dyns, ceu_mem->tup_$n);
                            }
                        }
                        if (!ceu_tuple_set(ceu_mem->tup_$n, $i, ceu_acc)) {
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : tuple error : incompatible scopes");
                        }
                        """
                    }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_mem->tup_$n} }")}
                }
            """
            is Expr.Vector -> """
                { // VECTOR ${this.tk.dump()}
                    ceu_mem->vec_$n = ceu_vector_create(&${ups.first_block(this)!!.toc(true)}->dn_dyns);
                    assert(ceu_mem->vec_$n != NULL);
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                            assert(ceu_mem->vec_$n->up_dyns.dyns != NULL);
                            if (ceu_acc.Dyn->up_dyns.dyns->up_block->depth > ceu_mem->vec_$n->up_dyns.dyns->up_block->depth) {
                                ceu_hold_rem(ceu_mem->vec_$n);
                                ceu_hold_add(ceu_acc.Dyn->up_dyns.dyns, ceu_mem->vec_$n);
                            }
                        }
                        ceu_ret = ceu_vector_set(ceu_mem->vec_$n, $i, ceu_acc);
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : vector error : incompatible scopes");
                        """
                    }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=ceu_mem->vec_$n} }")}
                }
            """
            is Expr.Dict -> {
                """
                { // DICT ${this.tk.dump()}
                    ceu_mem->dict_$n = ceu_dict_create(&${ups.first_block(this)!!.toc(true)}->dn_dyns);
                    assert(ceu_mem->dict_$n != NULL);
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                                assert(ceu_mem->dict_$n->up_dyns.dyns != NULL);
                                if (ceu_acc.Dyn->up_dyns.dyns->up_block->depth > ceu_mem->dict_$n->up_dyns.dyns->up_block->depth) {
                                    ceu_hold_rem(ceu_mem->dict_$n);
                                    ceu_hold_add(ceu_acc.Dyn->up_dyns.dyns, ceu_mem->dict_$n);
                                }
                                assert(ceu_block_chk_set_mutual(ceu_mem->dict_$n, ceu_acc.Dyn));
                            }
                            ceu_mem->key_$n = ceu_acc;
                            ${it.second.code()}
                            if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                                assert(ceu_mem->dict_$n->up_dyns.dyns != NULL);
                                if (ceu_acc.Dyn->up_dyns.dyns->up_block->depth > ceu_mem->dict_$n->up_dyns.dyns->up_block->depth) {
                                    ceu_hold_rem(ceu_mem->dict_$n);
                                    ceu_hold_add(ceu_acc.Dyn->up_dyns.dyns, ceu_mem->dict_$n);
                                }
                            }
                            CEU_Value ceu_val_$n = ceu_acc;
                            ceu_ret = ceu_dict_set(ceu_mem->dict_$n, &ceu_mem->key_$n, &ceu_val_$n);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : dict error : incompatible scopes");
                        }
                    """ }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_DICT, {.Dyn=ceu_mem->dict_$n} }")}
                }
                """
            }
            is Expr.Index -> {
                val idx = vars.data(this).let { if (it == null) -1 else it.first!! }
                fun Expr.Index.has_pub_evt (): String? {
                    val up = ups.pub[this]
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
                    ${if (idx == -1) {
                        """
                        ${this.idx.code()}
                        ceu_mem->idx_$n = ceu_acc;
                        """
                    } else {
                        """
                        ceu_mem->idx_$n = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=$idx} };
                        """
                    }}
                    // COL
                    ${this.col.code()}
                    ceu_ret = ceu_col_check(&ceu_acc, &ceu_mem->idx_$n);
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                    ${if (!this.isdst()) {
                        """
                        switch (ceu_acc.type) {
                            case CEU_VALUE_TUPLE:
                                ${assrc("ceu_acc.Dyn->Ncast.Tuple.buf[(int) ceu_mem->idx_$n.Number]")}
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
                        if (ceu_mem->idx_$n.type > CEU_VALUE_DYNAMIC) {
                            if (!ceu_block_chk_set(ceu_mem->idx_$n.Dyn, ceu_acc.Dyn->up_dyns.dyns, CEU_HOLD_NON)) {
                                CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                            }
                        }
                        if ($src.type > CEU_VALUE_DYNAMIC) {
                            if (!ceu_block_chk_set($src.Dyn, ceu_acc.Dyn->up_dyns.dyns, CEU_HOLD_NON)) {
                                CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                            }
                        }
                        switch (ceu_acc.type) {
                            case CEU_VALUE_TUPLE:
                                if (!ceu_tuple_set(ceu_acc.Dyn, ceu_mem->idx_$n.Number, $src)) {
                                    ceu_ret = CEU_RET_THROW;
                                }
                                break;
                            case CEU_VALUE_VECTOR:
                                if (!ceu_vector_set(ceu_acc.Dyn, ceu_mem->idx_$n.Number, $src)) {
                                    ceu_ret = CEU_RET_THROW;
                                }
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_acc;
                                if (!ceu_dict_set(ceu_dict.Dyn, &ceu_mem->idx_$n, &$src)) {
                                    ceu_ret = CEU_RET_THROW;
                                }
                                break;
                            }
                            default:
                                assert(0 && "bug found");
                        }
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                        """
                    }}
                }
                """
            }
            is Expr.Call -> {
                val bupc = ups.first_block(this)!!.toc(true)
                val up = ups.pub[this]!!
                val resume = if (up is Expr.Resume) up else null
                val spawn  = if (up is Expr.Spawn && up.tasks!=this) up else null
                /*
                val resume = ups.all_until(up) { it is Expr.Resume }.let { es ->
                    when {
                        es.isEmpty() -> null
                        es.drop(1).all { it is Expr.Export } -> es.first() as Expr.Resume
                        else -> null
                    }
                }
                val spawn = ups.all_until(up) { it is Expr.Spawn }.let { es ->
                    val fst = es.firstOrNull() as Expr.Spawn?
                    when {
                        (fst == null) -> null
                        (fst.tasks == this) -> null
                        es.drop(1).all { grp ->
                            (grp is Expr.Export) && (grp.body.es.last() is Expr.Call) && ups.pub[grp].let { it is Expr.Spawn && it.call==grp }
                        } -> fst
                        else -> null
                    }
                }
                */

                val iscall = (resume==null && spawn==null)
                val istasks = (spawn?.tasks != null)
                val frame = if (iscall) "(&ceu_frame_$n)" else "(ceu_x_$n.Dyn->Bcast.X.frame)"
                val pass_evt = ups.intask(this) && (this.proto is Expr.Proto) && (this.proto.task.let { it!=null && it.second } && (this.args.size == 0))

                val has_dots = (this.args.lastOrNull().let { it!=null && it is Expr.Acc && it.tk.str == "..." } && !this.proto.let { it is Expr.Acc && it.tk.str=="{#}" })
                val id_dots = vars.get(this, "...")?.let { this.id2c(it,0).first }

                val (args_sets,args_vs) = this.args.filter{!(has_dots && it.tk.str=="...")}.mapIndexed { i,e ->
                    Pair (
                        e.code() + "ceu_mem->arg_${i}_$n = ceu_acc;\n",
                        "ceu_args_$n[$i] = &ceu_mem->arg_${i}_$n;\n"
                    )
                }.unzip().let {
                    Pair(it.first.joinToString(""), it.second.joinToString(""))
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
                    if (ceu_proto_$n.type != CEU_VALUE_P_FUNC) {
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : call error : expected function");
                    }
                    CEU_Frame ceu_frame_$n = { &ceu_proto_$n.Dyn->Ncast.Proto, $bupc, NULL, {} };
                """} +

                spawn.cond{"""
                // SPAWN/CORO ${this.tk.dump()}
                    ${istasks.cond {
                        spawn!!.tasks!!.code() + """
                        ceu_mem->tasks_${spawn!!.n} = ceu_acc;
                        """
                    }}
                    ${this.proto.code()}
                    CEU_Value ceu_task_$n = ceu_acc;
                    CEU_Value ceu_x_$n;
                    ${istasks.cond { "CEU_Value ceu_ok_$n = { CEU_VALUE_BOOL, {.Bool=1} };" }}
                    ${if (!istasks) {
                        """
                        ceu_ret = ceu_x_create(&$bupc->dn_dyns, &ceu_task_$n, &ceu_x_$n);
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        """
                    } else {
                        """
                        ceu_ret = ceu_x_create_in (
                            ceu_mem->tasks_${spawn!!.n}.Dyn,
                            &ceu_task_$n,
                            &ceu_x_$n,
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
                    CEU_Value ceu_x_$n = ceu_acc;
                    if (ceu_x_$n.type!=CEU_VALUE_X_CORO || (ceu_x_$n.Dyn->Bcast.status!=CEU_X_STATUS_YIELDED && ceu_x_$n.Dyn->Bcast.status!=CEU_X_STATUS_TOGGLED)) {                
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${resume!!.tk.pos.file} : (lin ${resume.tk.pos.lin}, col ${resume.tk.pos.col}) : resume error : expected yielded coro");
                    }
                """} +

                """
                    ${has_dots.cond { """
                        int ceu_dots_$n = $id_dots.Dyn->Ncast.Tuple.its;
                    """ }}
                    CEU_Value* ceu_args_$n[${when {
                        pass_evt  -> "1"
                        !has_dots -> this.args.size
                        else      -> "ceu_dots_$n + " + (this.args.size-1)
                    }}];
                    ${when {
                        pass_evt  -> "ceu_args_$n[0] = ceu_evt;"
                        !has_dots -> args_vs
                        else      -> """
                            $args_vs
                            for (int ceu_i_$n=0; ceu_i_$n<ceu_dots_$n; ceu_i_$n++) {
                                ceu_args_$n[${this.args.size-1} + ceu_i_$n] = &$id_dots.Dyn->Ncast.Tuple.buf[ceu_i_$n];
                            }
                        """
                    }}

                    CEU_BStack ceu_bstack_$n = { $bupc, ceu_bstack };
                    ceu_ret = $frame->proto->f (
                        $frame,
                        &ceu_bstack_$n,
                        ${if (pass_evt) -1 else this.args.let {
                            if (!has_dots) it.size.toString() else {
                                "(" + (it.size-1) + " + ceu_dots_$n)"
                            }
                        }},
                        ceu_args_$n
                    );
                    if (ceu_bstack_$n.block == NULL) {
                        return ceu_ret;
                    }
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");
                    ${istasks.cond{"}"}}
                    ${spawn.cond{ assrc(if (istasks) "ceu_ok_$n" else "ceu_x_$n") }}
                } // CALL (close)
                """
            }
        }
    }
}
