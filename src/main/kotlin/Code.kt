package dceu

import kotlin.math.max

class Coder () {
    // Pair<mems,protos>: need to separate b/c protos must be inner->outer, while mems outer->inner
    val pres: MutableList<Pair<String,String>> = mutableListOf()
    val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>> = mutableMapOf()
    val code: String = G.outer!!.code()

    fun List<Expr>.code (): String {
        return this.map { it.code() }.joinToString("")
    }

    fun Expr.toerr (): String {
        val src = this.to_str(false).replace("_\\d+".toRegex(), "").quote(45)
        return "\"${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : $src\""
    }

    fun Expr.check_aborted (cmd: String): String {
        val exe = this.up_exe()
        val defer = this.up_first_without({ it is Expr.Defer }, { it is Expr.Proto })
        return (CEU>=3 && exe!==null && defer===null).cond { """
            if (ceux->exe->status == CEU_EXE_STATUS_TERMINATED) {
                $cmd;
            }
        """ }
    }
    fun Expr.check_error_aborted (cmd: String, msg: String): String {
        return """
            CEU_ERROR_CHK_ERR($cmd, $msg);
            ${this.check_aborted(cmd)}
        """
    }

    fun Expr.code(): String {
        if (this.is_dst()) {
            assert(this is Expr.Acc || this is Expr.Index || this is Expr.Pub)
        }
        return when (this) {
            is Expr.Proto -> {
                val isexe = (this.tk.str != "func")
                val code = this.blk.code()
                val id = this.id(G.outer!!)

                val mem = """
                    // PROTO | ${this.dump()}
                    ${isexe.cond { """
                        typedef struct CEU_Pro_$id {
                            ${Mem(defers).pub(this)}
                        } CEU_Pro_$id;                        
                    """ }}
                """
                val src = """
                    // PROTO | ${this.dump()}
                    void ceu_pro_$id (CEUX* ceux) {
                        ${isexe.cond{"""
                            CEU_Pro_$id* ceu_mem = (CEU_Pro_$id*) ceux->exe->mem;                    
                            ceux->exe->status = (ceux->act == CEU_ACTION_ABORT) ? CEU_EXE_STATUS_TERMINATED : CEU_EXE_STATUS_RESUMED;
                            switch (ceux->exe->pc) {
                                case 0:
                                    if (ceux->act == CEU_ACTION_ABORT) {
                                        CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
                                        return;
                                    }
                        """}}
                        
                        //{ // pars
                            ${this.pars.mapIndexed { i,dcl -> """
                                ${this.blk.is_mem().cond2({ """
                                    ceu_mem->${dcl.idtag.first.str.idc()}_${dcl.n}
                                """ },{ """
                                    CEU_Value ceu_loc_${dcl.idtag.first.str.idc()}_${dcl.n}
                                """ })}
                                    = ($i < ceux->n) ? ceux->args[$i] : (CEU_Value) { CEU_VALUE_NIL };
                            """ }.joinToString("")}
                            for (int i=${this.pars.size}; i<ceux->n; i++) {
                                ceu_gc_dec_val(ceux->args[i]);
                            }
                        //}

                        do {
                            $code
                        } while (0);
                        
                    #if CEU >= 2
                        assert(CEU_ESCAPE == CEU_ESCAPE_NONE);
                    #endif

                        { // pars
                            CEU_Value ceu_acc_$n = CEU_ACC_KEEP();
                            ${this.pars.mapIndexed { i,dcl -> """
                                ceu_gc_dec_val (
                                    ${this.blk.is_mem().cond2({ """
                                        ceu_mem->${dcl.idtag.first.str.idc()}_${dcl.n}
                                    """ },{ """
                                        ceu_loc_${dcl.idtag.first.str.idc()}_${dcl.n}
                                    """ })}
                                );
                            """ }.joinToString("")}
                            CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
                            ceu_acc = ceu_acc_$n;
                        }

                        ${isexe.cond{"""
                                ceu_exe_term(ceux);
                            } // close switch
                        """}}
                    }
                """
                val cre = """ // CREATE | ${this.dump()}
                {
                    CEU_ACC (
                        ceu_create_clo_${this.tk.str} (
                            ceu_pro_$id,
                            ${this.pars.size},
                            ${G.nonlocs[this.n]!!.size}
                            ${isexe.cond {", sizeof(CEU_Pro_$id)"}}
                            CEU50(COMMA ceux->exe)
                            CEU_LEX_X(COMMA ((CEU_Lex) { ${if (this.nst) "CEU_LEX_IMMUT, ceux->depth" else "CEU_LEX_FLEET, CEU_LEX_UNDEF"} }))
                        )
                    );
                    
                    // UPVALS = ${G.nonlocs[this.n]!!.size}
                    {                        
                        ${G.nonlocs[this.n]!!.mapIndexed { i,dcl ->
                            """
                            {
                                CEU_Value upv = ${(dcl.fnex() as Expr.Dcl).idx(this.fupx())};
                                ceu_gc_inc_val(upv);
                                ceu_acc.Dyn->Clo.upvs.buf[$i] = upv;
                            }
                            """
                        }.joinToString("\n")}
                    }
                }
                """

                when {
                    isexe -> {
                        pres.add(Pair(mem, src))
                        cre
                    }
                    (this.nst && G.nonlocs[this.n]!!.isNotEmpty()) -> {
                        src + cre
                    }
                    else -> {
                        pres.add(Pair("", src))
                        cre
                    }
                }
            }
            is Expr.Do -> {
                val body = this.es.code()   // before defers[this] check
                val up = this.fup()
                val blkc = this.idx("block_$n")
                """
                { // BLOCK | ${this.dump()}
            #ifdef CEU_LEX
                    ceux->depth++;
            #endif
                    ${(CEU >= 4).cond { """
                         ${(!this.is_mem()).cond { "CEU_Block" }} $blkc = NULL;
                    """}}
                    ${(this.n == G.outer!!.n).cond { """
                        { // ARGC / ARGV
                            CEU_Value args[ceu_argc];
                            for (int i=0; i<ceu_argc; i++) {
                                args[i] = ceu_pointer_to_string(ceu_argv[i]);
                                ceu_gc_inc_val(args[i]);
                            }
                            ceu_glb_ARGS = ceu_create_tuple(1, ceu_argc, args);
                            ceu_gc_inc_val(ceu_glb_ARGS);
                        }
                    """}}
                    
                    ${(this.n != G.outer!!.n).cond { 
                        this.to_dcls().let { dcls ->
                            """
                            ${(!this.is_mem()).cond { """
                                //{ // inline vars dcls
                                    ${dcls.map { """
                                        CEU_Value ${it.idx(it)};
                                    """ }.joinToString("")}
                                //}
                            """ }}
                            { // vars inits
                                ${dcls.map { """
                                    ${it.idx(it)} = (CEU_Value) { CEU_VALUE_NIL };
                                """ }.joinToString("")}
                            }
                            """
                        }
                    }}
                
                    ${defers[this].cond { """
                        //{ // BLOCK | defers | init | ${this.dump()}
                            ${it.second}
                        //}
                    """ }}
                    
                    do { // BLOCK | ${this.dump()}
                        $body
                        ${(up is Expr.Loop).cond { """
                            CEU_LOOP_STOP_${up!!.n}:
                        """ }}
                    } while (0);

                    // keep ceu_acc and restore after defers/gc_dec/kills
                    // b/c they may change ceu_acc
                    
                    CEU_Value ceu_acc_$n = CEU_ACC_KEEP();

                    ${defers[this].cond { """
                        { // BLOCK | defers | term | ${this.dump()}
                            ${it.third}
                        }
                    """ }}
                    
                    ${(CEU >= 4).cond { """
                        if ($blkc != NULL) {
                            CEU_LNKS($blkc)->up.blk = NULL; // also on ceu_task_unlink (if unlinked before leave)
                        }
                        {
                            CEU_Block cur = ceu_task_get($blkc);
                            while (cur != NULL) {
                                ceu_abort_dyn(cur);
                                CEU_Dyn* nxt = ceu_task_get(CEU_LNKS(cur)->sd.nxt);
                                ceu_gc_dec_dyn(cur); // TODO: could affect nxt?
                                cur = nxt;
                            }
                        }                            
                    """ }}

                    { // dcls gc-dec
                        ${this.to_dcls()
                            .asReversed()
                            .filter { !GLOBALS.contains(it.idtag.first.str) }
                            .map { """
                                ceu_gc_dec_val(${it.idx(it)});
                            """ }
                            .joinToString("")
                        }
                    }                        

                    CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
                    ceu_acc = ceu_acc_$n;
                    
            #ifdef CEU_LEX
                    ceux->depth--;
            #endif

                    ${(CEU >= 2).cond { """
                        if (CEU_ERROR != CEU_ERROR_NONE) {
                            continue;
                        }
                        ${this.check_aborted("continue")}
                        if (CEU_ESCAPE != CEU_ESCAPE_NONE) {
                            continue;
                        }
                    """ }}
                }
                """
            }
            is Expr.Enclose -> {
                """
                do { // ENCLOSE | ${this.dump()}
                    ${this.blk.code()}
                } while (0);
                if (CEU_ERROR != CEU_ERROR_NONE) {
                    continue;
                }
                ${this.check_aborted("continue")}
                if (CEU_ESCAPE == CEU_ESCAPE_NONE) {
                    // no escape
                } else if (CEU_ESCAPE == CEU_TAG_${this.tag.str.idc()}) {
                    CEU_ESCAPE = CEU_ESCAPE_NONE;   // caught escape: go ahead
                } else {
                    continue;                       // uncaught escape: propagate up
                }                                                            
                """
            }
            is Expr.Escape -> """ // ESCAPE | ${this.dump()}
            {
                ${this.e.cond { """
                    ${it.code()}
                """ }}
                CEU_ESCAPE = CEU_TAG_${this.tag.str.idc()};
                continue;
            }
            """
            is Expr.Group -> "// GROUP | ${this.dump()}\n" + this.es.code()
            is Expr.Dcl -> {
                val idx = this.idx(this)
                """
                // DCL | ${this.dump()}
                ${when {
                    //sta.protos_use_unused.contains(this.src) -> """
                    //    // $idx: unused function
                    //"""
                    (this.src !== null) -> """
                        ${this.src.code()}
                        #ifdef CEU_LEX
                        CEU_ERROR_CHK_PTR (
                            continue,
                            ceu_lex_chk_own(ceu_acc, (CEU_Lex) { ${if (this.lex) "CEU_LEX_MUTAB" else "CEU_LEX_FLEET"}, ceux->depth }),
                            ${this.toerr()}
                        );
                        #endif
                        ceu_gc_inc_val(ceu_acc);
                        $idx = ceu_acc;
                    """
                    else -> ""
                }}
                """
            }
            is Expr.Set -> """
                { // SET | ${this.dump()}
                    ${this.src.code()}  // src is on the stack and should be returned
                    // <<< SRC | DST >>>
                    ${this.dst.code()}  // dst should not pop src
                }
            """
            is Expr.If -> """
                { // IF | ${this.dump()}
                    ${this.cnd.code()}
                    {
                        int v = ceu_as_bool(ceu_acc);
                        if (v) {
                            ${this.t.code()}
                        } else {
                            ${this.f.code()}
                        }
                    }
                }
                """
            is Expr.Loop -> """
                // LOOP | ${this.dump()}
                CEU_LOOP_START_${this.n}:
                    ${this.blk.code()}
                    goto CEU_LOOP_START_${this.n};
            """
            is Expr.Data -> "// DATA | ${this.dump()}\n"
            is Expr.Drop -> """
                // DROP | ${this.dump()}
                ${this.e.code()}
                ${(LEX && !this.e.is_lval()).cond { """
                    CEU_ERROR_CHK_PTR (
                        continue,
                        ceu_drop(${if (this.prime) "1" else "0"}, ceu_acc, ceux->depth),
                        ${this.fupx().toerr()}
                    );
                """ }}
            """

            is Expr.Catch -> {
                """
                { // CATCH ${this.dump()}
                    do { // catch
                        ${this.blk.code()}
                    } while (0); // catch
                    ${(CEU>=3 && this.up_any { it is Expr.Proto && it.tk.str!="func" }).cond { """
                        if (ceux->act == CEU_ACTION_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                    if (CEU_ERROR == CEU_ERROR_NONE) {
                        // no error
                    } else {
                        ${this.tag.cond2({"""
                            if (CEU_ERROR==CEU_TAG_nil || _ceu_sup_(CEU_TAG_${it.str.idc()}, CEU_ERROR)) {
                                CEU_ERROR = CEU_ERROR_NONE; // caught error: go ahead
                            } else {
                                continue;                   // uncaught error: propagate up
                            }                            
                        """},{"""
                            CEU_ERROR = CEU_ERROR_NONE; // caught error: go ahead
                        """})}
                    }
                }
                """
            }
            is Expr.Defer -> {
                val bup = this.up_first { it is Expr.Do } as Expr.Do
                val (ns,ini,end) = defers.getOrDefault(bup, Triple(mutableListOf(),"",""))
                val id = this.idx("defer_$n")
                val inix = """
                    ${this.dcl("int")} $id = 0;   // not yet reached
                """
                val endx = """
                    if ($id) {     // if true: reached, finalize
                        int ceu_error_$n = CEU_ERROR;
                        CEU_ERROR = CEU_ERROR_NONE;
                        do {
                            ${this.blk.code()}
                        } while (0);    // catch throw
                        assert(CEU_ERROR==CEU_ERROR_NONE && "TODO: error in defer");
                        CEU_ERROR = ceu_error_$n;
                    }
                """
                ns.add(n)
                defers[bup] = Triple(ns, ini+inix, endx+end)
                """
                $id = 1;   // now reached
                CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));
                """
            }

            is Expr.Resume -> {
                val coro = this.idx("coro_$n")
                """
                { // RESUME | ${this.dump()}
                    ${(!this.is_mem()).cond { """
                        CEU_Value $coro;
                        CEU_Value ceu_args_$n[${this.args.size}];
                    """ }}

                    ${this.co.code()}
                    $coro = CEU_ACC_KEEP();
                    if ($coro.type!=CEU_VALUE_EXE_CORO || ($coro.Dyn->Exe.status!=CEU_EXE_STATUS_YIELDED)) {                
                        ceu_gc_dec_val($coro);
                        CEU_ERROR_CHK_PTR (
                            continue,
                            "expected yielded coro",
                            ${this.toerr()}
                        );
                    }

                    ${this.args.mapIndexed { i,e ->
                        e.code() + """
                            #ifdef CEU_LEX
                                CEU_ERROR_CHK_PTR (
                                    { ceu_gc_dec_val($coro); continue; },
                                    ceu_lex_chk_own(ceu_acc, $coro.Dyn->Any.lex),
                                    ${this.toerr()}
                                );
                            #endif
                            ${this.idx("args_$n")}[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}
                    
                    CEUX ceux_$n = {
                        $coro.Dyn->Exe.clo,
                        {.exe = (CEU_Exe*) $coro.Dyn},
                        CEU_ACTION_RESUME,
                        CEU4(ceux COMMA)
                        CEU_LEX_X($coro.Dyn->Exe.depth COMMA)
                        ${this.args.size},
                        ${this.idx("args_$n")}
                    };
                    $coro.Dyn->Exe.clo->proto(&ceux_$n);
                    ceu_gc_dec_val($coro);
                    ${this.check_error_aborted("continue", this.toerr())}
                } // CALL | ${this.dump()}
            """
            }
            is Expr.Yield -> {
                """
                { // YIELD ${this.dump()}
                    ${this.e.code()}
                    ceux->exe->status = CEU_EXE_STATUS_YIELDED;
                    ceux->exe->pc = $n;
                #ifdef CEU_LEX
                    ceux->exe->depth = ceux->depth;
                    CEU_ERROR_CHK_PTR (
                        continue,
                        ceu_lex_chk_own(ceu_acc, (CEU_Lex) { CEU_LEX_MUTAB, 1 }),
                        ${this.toerr()}
                    );
                #endif
                    return;
                case $n: // YIELD ${this.dump()}
                    if (ceux->act == CEU_ACTION_ABORT) {
                        //CEU_ACC((CEU_Value) { CEU_VALUE_NIL }); // to be ignored in further move/checks
                        continue;
                    }
                #if CEU >= 4
                    if (ceux->act == CEU_ACTION_ERROR) {
                        continue;
                    }
                #endif
                #ifdef CEU_LEX
                    ceux->depth = ceux->exe->depth;
                #endif
                    ceu_gc_dec_val(ceu_acc);
                    ceu_acc = (ceux->n > 0) ? ceux->args[0] : (CEU_Value) { CEU_VALUE_NIL };
                }
            """
            }

            is Expr.Spawn -> {
                val blk = this.up_first { it is Expr.Do } as Expr.Do
                val blkc = blk.idx("block_${blk.n}")
                """
                { // SPAWN | ${this.dump()}
                    ${(!this.is_mem()).cond { """
                        CEU_Value ceu_tsks_$n;
                        CEU_Value ceu_args_$n[${this.args.size}];
                    """ }}

                    ${(CEU>=5 && this.tsks!==null).cond {
                        this.tsks!!.code() + """
                            ${this.idx("tsks_$n")} = ceu_acc;                            
                            if (ceu_acc.type != CEU_VALUE_TASKS) {
                                CEU_ERROR_CHK_PTR (
                                    continue,
                                    "invalid pool",
                                    ${this.toerr()}
                                );
                            }
                        """
                    }}
                    
                    ${this.args.mapIndexed { i,e ->
                        e.code() + """
                            #ifdef CEU_LEX
                                CEU_ERROR_CHK_PTR (
                                    continue,
                                    ceu_lex_chk_own(ceu_acc, (CEU_Lex) { CEU_LEX_MUTAB, ceux->depth }),
                                    ${this.toerr()}
                                );
                            #endif
                            ${this.idx("args_$n")}[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}

                    ${this.tsk.code()}
                    CEU_Dyn* ceu_a_$n = ${when {
                        (CEU<5 || this.tsks===null) -> "(CEU_Dyn*)ceu_task_up(ceux)"
                        else -> this.idx("tsks_$n") + ".Dyn"
                    }};
                    CEU_Block* ceu_b_$n = ${this.tsks.cond2({"NULL"}, {"&$blkc"})};
                    CEU_Value ceu_exe_$n = ceu_create_exe_task(ceu_acc, ceu_a_$n, ceu_b_$n CEU_LEX_X(COMMA ceux->depth));
                    CEU_ACC(ceu_exe_$n);
                    CEU_ERROR_CHK_ERR(continue, ${this.toerr()});
                    
                #ifdef CEU_LEX
                    CEU_ERROR_CHK_PTR (
                        continue,
                        ceu_lex_chk_own(ceu_acc,
                            ${if (CEU>=5 && this.tsks!==null) {
                                "ceu_a_$n->Any.lex"
                            } else {
                                "(CEU_Lex) { CEU_LEX_IMMUT, ceux->depth }"  
                            }}
                        ),
                        ${this.toerr()}
                    );
                #endif
        
                    ${(CEU>=5 && this.tsks!==null).cond { """
                        if (ceu_acc.type != CEU_VALUE_NIL)
                    """ }}
                    {
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                        CEUX ceux_$n = {
                            ceu_exe_$n.Dyn->Exe.clo,
                            {.exe = (CEU_Exe*) ceu_exe_$n.Dyn},
                            CEU_ACTION_RESUME,
                            ceux,
                            CEU_LEX_X(ceu_exe_$n.Dyn->Exe.depth COMMA)
                            ${this.args.size},
                            ${this.idx("args_$n")}
                        };
                        ceu_exe_$n.Dyn->Exe.clo->proto(&ceux_$n);
                        CEU_ERROR_CHK_ERR({ceu_gc_dec_val(ceu_exe_$n);continue;}, ${this.toerr()});
                        if (CEU_ESCAPE != CEU_ESCAPE_NONE) {
                            ceu_gc_dec_val(ceu_exe_$n);
                            continue;
                        }                                                            
                        ceu_gc_dec_val(ceu_acc);
                        ${this.check_aborted("{ceu_gc_dec_val(ceu_exe_$n);continue;}")}
                        ceu_acc = ceu_exe_$n;
                    }
                } // SPAWN | ${this.dump()}
                """
            }
            is Expr.Delay -> """
                // DELAY | ${this.dump()}
                ceux->exe_task->time = CEU_TIME;
            """
            is Expr.Pub -> {
                val id = this.idx( "val_$n")
                val exe = if (this.tsk !== null) "" else {
                    this.up_first_task_outer().let { outer ->
                        val n = this.up_all_until() {
                            it is Expr.Proto && it.tk.str=="task" && !it.fake
                        }
                            .filter { it is Expr.Proto } // but count all protos in between
                            .count() - 1
                        "(ceux->exe_task${"->clo->up_nst".repeat(n)})"
                    }
                }
                if (this.is_drop()) {
                    error("TODO: drop pub")
                }
            """
            { // PUB | ${this.dump()}
                ${this.is_dst().cond{ """
                    ${this.dcl("CEU_Value")} $id = CEU_ACC_KEEP();
                """ }}
                ${this.tsk.cond2({"""
                    ${it.code()}
                    CEU_Value tsk = ceu_acc;
                    if (!ceu_istask_val(tsk)) {
                        CEU_ERROR_CHK_PTR (
                            continue,
                            "expected task",
                            ${this.toerr()}
                        );
                    }
                """},{"""
                    CEU_Value tsk = ceu_dyn_to_val((CEU_Dyn*)$exe);
                """})}
                ${this.is_dst().cond2({ """
                    ceu_gc_dec_val(tsk.Dyn->Exe_Task.pub);
                    tsk.Dyn->Exe_Task.pub = $id;
                """ },{ """
                    CEU_ACC(tsk.Dyn->Exe_Task.pub);
                """ })}
            }
            """
            }
            is Expr.Toggle -> {
                val id = this.idx( "tsk_$n")
                """
                {  // TOGGLE | ${this.dump()}
                    ${this.tsk.code()}
                    ${this.dcl("CEU_Value")} $id = CEU_ACC_KEEP();
                    ${this.on.code()}
                    {   // TOGGLE | ${this.dump()}
                        int on = ceu_as_bool(ceu_acc);
                        int ceu_err_$n = 0;
                        if (!ceu_istask_val($id)) {
                            CEU_ERROR_CHK_PTR (
                                continue,
                                "expected yielded task",
                                ${this.toerr()}
                            );
                        }
                        if (!ceu_err_$n && on && $id.Dyn->Exe_Task.status!=CEU_EXE_STATUS_TOGGLED) {                
                            CEU_ERROR_CHK_PTR (
                                {ceu_err_$n = 1;},
                                "expected toggled task",
                                ${this.toerr()}
                            );
                        }
                        if (!ceu_err_$n && !on && $id.Dyn->Exe_Task.status!=CEU_EXE_STATUS_YIELDED) {                
                            CEU_ERROR_CHK_PTR (
                                {ceu_err_$n = 1;},
                                "expected yielded task",
                                ${this.toerr()}
                            );
                        }
                        ceu_gc_dec_val($id);
                        if (ceu_err_$n) {
                            continue;
                        }
                        $id.Dyn->Exe_Task.status = (on ? CEU_EXE_STATUS_YIELDED : CEU_EXE_STATUS_TOGGLED);
                    }
                }
            """
            }
            is Expr.Tasks -> {
                val blk = this.up_first { it is Expr.Do } as Expr.Do
                val blkc = blk.idx("block_${blk.n}")
                """
                {  // TASKS | ${this.dump()}
                    ${this.max.code()}
                    CEU_Value ceu_tsks_$n = ceu_create_tasks(ceux, &$blkc, ceu_acc CEU_LEX_X(COMMA CEU_LEX_UNDEF));
                    CEU_ACC(ceu_tsks_$n);
                    CEU_ERROR_CHK_ERR(continue, ${this.toerr()});
                }
                """
            }

            is Expr.Nat -> {
                val body = G.nats[this.n]!!.let { (set, str) ->
                    var x = str
                    for (dcl in set) {
                        val idx = (dcl.fnex() as Expr.Dcl).idx(this)
                        //println(setOf(x, v))
                        x = x.replaceFirst("XXX", idx)
                    }
                    x
                }
                when (this.tk_.tag) {
                    null   -> """
                        CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));
                        $body 
                        ${this.check_error_aborted("continue", this.toerr())}
                    """
                    ":pre" -> {
                        pres.add(Pair("",body))
                        "CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));"
                    }
                    ":ceu" -> "CEU_ACC($body);"
                    else -> {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        "CEU_ACC(((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} }));"
                    }
                }
            }
            is Expr.Acc -> {
                val idx = this.idx()
                val dcl = this.id_to_dcl(this.tk.str)!!
                when {
                    this.is_dst() -> {
                        val depth = this.id_to_dcl(this.tk.str)!!.to_blk().let { blk ->
                            this.up_all_until { it.n == blk.n }.filter { it is Expr.Do }.count() - 1
                        }
                        """
                        // ACC - SET | ${this.dump()}
                        #ifdef CEU_LEX
                        CEU_ERROR_CHK_PTR (
                            continue,
                            ceu_lex_chk_own(ceu_acc, (CEU_Lex) { ${if (dcl.lex) "CEU_LEX_MUTAB" else "CEU_LEX_FLEET"}, ceux->depth-$depth }),
                            ${this.toerr()}
                        );
                        #endif
                        ceu_gc_dec_val($idx);
                        ceu_gc_inc_val(ceu_acc);
                        $idx = ceu_acc;
                        """
                    }
                    this.is_drop() -> {
                        val prime = (this.up_first { it is Expr.Drop } as Expr.Drop).prime
                        """
                        { // ACC - DROP | ${this.dump()}
                            CEU_ACC((CEU_Value) { CEU_VALUE_NIL });     // ceu_acc may be equal to $idx (hh_05_coro)
                            CEU_Value ceu_$n = $idx;
                            $idx = (CEU_Value) { CEU_VALUE_NIL };
                            CEU_ERROR_CHK_PTR (
                                continue,
                                ceu_drop(${if (prime) "1" else "0"}, ceu_$n, ceux->depth),
                                ${this.fupx().toerr()}
                            );
                            CEU_ACC(ceu_$n);
                            ceu_gc_dec_val(ceu_$n);
                        }
                        """
                    }
                    else -> """
                        // ACC - GET | ${this.dump()}
                        CEU_ACC($idx);
                    """
                }
            }
            is Expr.Nil  -> "CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));"
            is Expr.Tag  -> "CEU_ACC(((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_${this.tk.str.idc()}} }));"
            is Expr.Bool -> "CEU_ACC(((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} }));"
            is Expr.Char -> "CEU_ACC(((CEU_Value) { CEU_VALUE_CHAR, {.Char=${this.tk.str}} }));"
            is Expr.Num  -> "CEU_ACC(((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} }));"

            is Expr.Tuple -> {
                val id_args = this.idx("args_$n")
                """
                { // TUPLE | ${this.dump()}
                    ${(!this.is_mem()).cond { """
                        CEU_Value ceu_args_$n[${max(1,this.args.size)}];
                    """ }}
                    ${this.args.mapIndexed { i, it ->
                    it.code() + """
                            $id_args[$i] = CEU_ACC_KEEP();
                        """
                }.joinToString("")}
                    CEU_ACC (
                        ceu_create_tuple(1, ${this.args.size}, $id_args);
                    );
                }
            """
            }
            is Expr.Vector -> {
                val id_vec = this.idx("vec_$n")
                """
                { // VECTOR | ${this.dump()}
                    ${this.dcl()} $id_vec = ceu_create_vector();
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                            assert(NULL == ceu_col_set($id_vec, (CEU_Value) { CEU_VALUE_NUMBER, {.Number=$i} }, ceu_acc));
                        """
                    }.joinToString("")}
                    CEU_ACC($id_vec);
                }
            """
            }
            is Expr.Dict -> {
                val id_dic = this.idx("dic_$n")
                val id_key = this.idx("key_$n")
                """
                { // DICT | ${this.dump()}
                    ${this.dcl()} $id_dic = ceu_create_dict();
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            ${this.dcl()} $id_key = CEU_ACC_KEEP();
                            ${it.second.code()}
                            CEU_Value ceu_val_$n = CEU_ACC_KEEP();
                            CEU_ERROR_CHK_PTR (
                                continue,
                                ceu_col_set(${id_dic}, $id_key, ceu_val_$n),
                                ${this.toerr()}
                            );
                            ceu_gc_dec_val($id_key);
                            ceu_gc_dec_val(ceu_val_$n);
                        }
                    """ }.joinToString("")}
                    CEU_ACC($id_dic);
                }
            """
            }
            is Expr.Index -> {
                val idx = this.data().let { if (it === null) -1 else it.first!! }
                val id_col = this.idx("col_$n")
                val id_val = this.idx("val_$n")
                """
                { // INDEX | ${this.dump()}
                    // VAL
                    ${this.is_dst().cond { """
                        ${this.dcl()} $id_val = CEU_ACC_KEEP();
                    """ }}
                    
                    // COL
                    ${this.col.code()}
                    ${this.dcl()} $id_col = CEU_ACC_KEEP();

                    // IDX
                    ${if (idx == -1) {
                        this.idx.code()
                    } else {
                        """
                        CEU_ACC(((CEU_Value) { CEU_VALUE_NUMBER, {.Number=$idx} }));
                        """
                    }}
                    CEU_Value ceu_idx_$n = CEU_ACC_KEEP();
                """ +
                when {
                    this.is_dst() -> """
                        { // INDEX | DST | ${this.dump()}
                            char* ceu_err_$n = ceu_col_set($id_col, ceu_idx_$n, $id_val);
                            ceu_gc_dec_val($id_col);
                            ceu_gc_dec_val(ceu_idx_$n);
                            ceu_acc = $id_val;
                            CEU_ERROR_CHK_PTR (
                                continue,
                                ceu_err_$n,
                                ${this.toerr()}
                            );
                        }
                        """
                    this.is_drop() -> {
                        val prime = (this.up_first { it is Expr.Drop } as Expr.Drop).prime
                        """
                        { // INDEX | DROP | ${this.dump()}
                            CEU_ACC((CEU_Value) { CEU_VALUE_NIL });     // ceu_acc may be equal to $idx (hh_05_coro)
                            CEU_Value ceu_$n = ceu_col_get($id_col, ceu_idx_$n);
                            ceu_gc_inc_val(ceu_$n);
                            char* ceu_err_$n = ceu_col_set($id_col, ceu_idx_$n, (CEU_Value) { CEU_VALUE_NIL });
                            ceu_gc_dec_val($id_col);
                            CEU_ERROR_CHK_PTR (
                                continue,
                                ceu_drop(${if (prime) "1" else "0"}, ceu_$n, ceux->depth),
                                ${this.fupx().toerr()}
                            );
                            CEU_ACC(ceu_$n);
                            ceu_gc_dec_val(ceu_$n);
                        }
                    """
                    }
                    else -> """
                        CEU_ACC(ceu_col_get($id_col, ceu_idx_$n));
                        ceu_gc_dec_val($id_col);
                        ceu_gc_dec_val(ceu_idx_$n);
                        CEU_ERROR_CHK_ERR(continue, ${this.toerr()});
                    """
                } + """
                }
                """
            }
            is Expr.Call -> """
                { // CALL | ${this.dump()}
                    ${(!this.is_mem()).cond { """
                        CEU_Value ceu_args_$n[${this.args.size}];
                    """ }}
                    ${this.args.mapIndexed { i,e ->
                        e.code() + """
                            ${this.idx("args_$n")}[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}
                    ${this.clo.code()}
                    CEU_Value ceu_clo_$n = CEU_ACC_KEEP();
                    if (ceu_clo_$n.type != CEU_VALUE_CLO_FUNC) {
                        ceu_gc_dec_val(ceu_clo_$n);
                        CEU_ERROR_CHK_PTR (
                            continue,
                            "expected function",
                            ${this.toerr()}
                        );
                    }
                    CEUX ceux_$n = {
                        (CEU_Clo*) ceu_clo_$n.Dyn,
                    #if CEU >= 3
                        {NULL}, CEU_ACTION_INVALID,
                    #endif
                    #if CEU >= 4
                        ceux,
                    #endif
                        CEU_LEX_X(ceux->depth+1 COMMA)
                        ${this.args.size},
                        ${this.idx("args_$n")}
                    };
                    ceu_clo_$n.Dyn->Clo.proto(&ceux_$n);
                    ceu_gc_dec_val(ceu_clo_$n);
                    ${this.check_error_aborted("continue", this.toerr())}
                } // CALL | ${this.dump()}
            """
        }
    }
}
