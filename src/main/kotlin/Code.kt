package dceu

import java.lang.Integer.min

class Coder (val outer: Expr.Do, val ups: Ups, val vars: Vars, val clos: Clos, val sta: Static) {
    val pres: MutableList<Pair<String,String>> = mutableListOf()
    val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>> = mutableMapOf()
    val mem = Mem(ups, vars)
    val code: String = outer.code()

    fun Expr.idc (pre: String): String {
        return "ceu_${pre}_${this.n}"
    }
    fun Expr.Dcl.idc (upv: Int): String {
        return when {
            (upv == 2) -> {
                val idc = this.id.str.idc()
                "(ceu_upvs->$idc)"
            }
            else -> {
                this.id.str.idc()
            }
        }
    }
    fun Expr.Proto.idc (): String {
        return ups.pub[this].let {
            when {
                (it !is Expr.Dcl) -> this.n.toString()
                (it.src != this) -> error("bug found") as String
                else -> it.id.str.idc()
            }
        }
    }

    fun Expr.isdst (): Boolean {
        return ups.pub[this].let { it is Expr.Set && it.dst==this }
    }

    fun Expr.up_task_real_c (): String {
        val n = ups.all_until(this) {
                it is Expr.Proto && it.tk.str=="task" && it.tag?.str!=":void"
            }
            .filter { it is Expr.Proto } // but count all protos in between
            .count()
        val (x,y) = Pair("ceu_frame_up_frame(".repeat(n-1), ")".repeat(n-1))
        return "(($x ceu_frame $y)->exe_task)"
    }

    fun List<Expr>.code (): String {
        return "int ceu_seq_$N;\n" +
            this.mapIndexed { i,e -> """
                ceu_seq_$N = ceu_vstk_top();
                ${e.code()}
                assert(ceu_vstk_top()-ceu_seq_$N == 1);
                ${(i < this.size-1).cond { """
                    ceu_vstk_pop(1);
                """ }}
            """ }.joinToString("")
    }

    fun Expr.code(): String {
        if (this.isdst()) {
            assert(this is Expr.Acc || this is Expr.Index || this is Expr.Pub)
        }
        return when (this) {
            is Expr.Proto -> {
                val blk = ups.first_block(this)!!
                val isexe = (this.tk.str != "func")
                val istsk = (this.tk.str == "task")
                val code = this.blk.code()
                val id = this.idc()

                val pres = Pair(""" // UPVS | ${this.dump()}
                    ${clos.protos_refs[this].cond { """
                        typedef struct {
                            ${clos.protos_refs[this]!!.map {
                                "CEU_Value ${it.id.str.idc()};"
                            }.joinToString("")}
                        } CEU_Clo_Upvs_$id;                    
                    """ }}
                """ , """ // FUNC | ${this.dump()}
                    void ceu_f_$id (
                        CEU5(CEU_Stack* ceu_dstk COMMA)
                        CEU4(CEU_Stack* ceu_bstk COMMA)
                        CEU_Frame* ceu_frame,
                        int ceu_base
                    ) {
                        ${istsk.cond { """
                        CEU_Value id_evt = { CEU_VALUE_NIL };
                           // - C does not allow redeclaration (after each yield)
                           // - A task can only awake once per cycle
                           // - So it is safe to use one "global" id_evt per task
                        """ }}

                        ${clos.protos_refs[this].cond { """
                            CEU_Clo_Upvs_$id* ceu_upvs = (CEU_Clo_Upvs_$id*) ceu_frame->clo->upvs.buf;                    
                        """ }}
                        ${isexe.cond { """
                            CEU_Clo_Mem_$id* ceu_mem = (CEU_Clo_Mem_$id*) ceu_frame->exe->mem;                    
                        """ }}
                        ${isexe.cond{"""
                            ceu_frame->exe->status = CEU_EXE_STATUS_RESUMED;
                            switch (ceu_frame->exe->pc) {
                                case 0:
                                    if (ceu_base == CEU_ARG_ABORT) {
                                        ceu_frame->exe->status = CEU_EXE_STATUS_TERMINATED;
                                        return (CEU_Value) { CEU_VALUE_NIL };
                                    }
                        """}}
                        $code
                        ${isexe.cond{"""
                                    ceu_frame->exe->status = CEU_EXE_STATUS_TERMINATED;
                            }
                        """}}
                    }
                """)

                val pos = """ // CLO | ${this.dump()}
                CEU_Value ceu_clo_$n = ceu_create_clo${isexe.cond{"_exe"}} (
                    ${isexe.cond{"CEU_VALUE_CLO_${this.tk.str.uppercase()},"}}
                    ${if (clos.protos_noclos.contains(this)) "-=- TODO -=-" else ""}
                    ${when {
                        (blk == outer) -> "NULL"
                        isexe -> "&ceu_frame->exe->frame"
                        else -> "ceu_frame"
                    }},
                    ceu_f_$id,
                    ${clos.protos_refs[this]?.size ?: 0}
                );
                ceu_vstk_push(ceu_clo_$n, 1);
                ${isexe.cond { """
                    ceu_clo_$n.Dyn->Clo_Exe.mem_n = sizeof(CEU_Clo_Mem_$id);                    
                """ }}
                
                // UPVALS
                ${clos.protos_refs[this].cond {
                    it.map { dcl ->
                        val dcl_blk = vars.dcl_to_blk[dcl]!!
                        val idc = dcl.id.str.idc()
                        val btw = ups
                            .all_until(this) { dcl_blk==it }
                            .filter { it is Expr.Proto }
                            .count() // other protos in between myself and dcl, so it its an upref (upv=2)
                        val upv = min(2, btw)
                        """
                        {
                            CEU_Value ceu_up = ${dcl.idc(upv)};
                            ceu_gc_inc(ceu_up);
                            ((CEU_Clo_Upvs_$id*)ceu_clo_$n.Dyn->Clo.upvs.buf)->${idc} = ceu_up;
                        }
                        """   // TODO: use this.body (ups.ups[this]?) to not confuse with args
                    }.joinToString("\n")
                }}
                """

                if (clos.protos_noclos.contains(this) && this.tk.str=="func") {
                    pres.first + pres.second + pos
                } else {
                    this@Coder.pres.add(pres)
                    pos
                }
            }
            is Expr.Export -> this.blk.es.code()
            is Expr.Do -> {
                val body = this.es.code()   // before defers[this] check
                val _blkc = this.idc("_block")
                val blkc = this.idc("block")
                val up = ups.pub[this]
                val f_b = up?.let { ups.first_proto_or_block(it) }
                val bupc = when {
                    (up == null)        -> "(&CEU_BLOCK)"
                    (f_b is Expr.Proto) -> "NULL"
                    else                -> ups.first_block(up)!!.idc("block")
                }
                val (bf,ptr) = when {
                    (f_b == null) -> Pair("1", "{.frame=&CEU_FRAME}")
                    (f_b is Expr.Proto) -> Pair("1", "{.frame=${if (f_b.tk.str=="func") "ceu_frame" else "&ceu_frame->exe->frame"}}")
                    else -> Pair("0", "{.block=$bupc}")
                }
                val inexe  = ups.inexe(this, null,true)

                """
                { // BLOCK | ${this.dump()}
                    ${(CEU >= 4).cond { """ 
                        CEU_Block $_blkc = (CEU_Block) { $bf, $ptr, { CEU4(NULL COMMA) {NULL,NULL} } };
                        CEU_Block* $blkc = &$_blkc;
                        // link task.dn_block = me
                        // link up.dn.block = me
                        ${when {
                            (f_b is Expr.Proto && f_b.tk.str == "task") -> """
                                ceu_frame->exe_task->dn_block = $blkc;                        
                            """
                            (f_b !is Expr.Proto) -> """
                                $bupc->dn.block = $blkc;
                            """
                            else -> ""
                        }}
                    """ }}

                    ceu_vstk_block_enter(${vars.blk_to_dcls[this]!!.size});
                    
                    // GLOBALS
                    ${(up == null).cond { """ 
                        // funcs
                        ${GLOBALS.first.mapIndexed { i,id -> """
                        {
                            CEU_Value clo = ceu_create_clo(NULL, ceu_${id.idc()}_f, 0);
                            ceu_vstk_repl(1+$i, clo);
                                // +1 = BLOCK
                        }
                        """ }.joinToString("")}

                        // ... args ...
                        {
                            CEU_Value xxx = ceu_create_tuple(ceu_argc);
                            for (int i=0; i<ceu_argc; i++) {
                                CEU_Value vec = ceu_vector_from_c_string(ceu_argv[i]);
                                ceu_tuple_set(&xxx.Dyn->Tuple, i, vec);
                            }
                            ceu_vstk_repl(1+${GLOBALS.first.size+GLOBALS.second.indexOf("...")}, xxx);
                                // +1 = BLOCK
                        }
                        
                    """ }}

                    // defers init
                    ${defers[this].cond { it.second }}
                    // pres funcs
                    ${(f_b == null).cond {
                        pres.unzip().let {
                            it.first.joinToString("") +
                            it.second.joinToString("")
                        }
                    }}
                    
                    ${(CEU >= 2).cond { "do {" }} // error escape with `continue`

                    // func ... args
                    ${(f_b is Expr.Proto && f_b.args.lastOrNull()?.first?.str=="...").cond {
                        f_b as Expr.Proto
                        val N = f_b.args.size - 1  // -1 = ...
                        val idx = mem.pub(TODO() as Expr.Dcl)
                        """
                        {
                            int N = MAX(0, ceu_vstk_top()-ceu_base-$N-1);
                            CEU_Value xxx = ceu_create_tuple(N);
                            for (int i=0; i<N; i++) {
                                ceu_tuple_set(&xxx.Dyn->Tuple, i, ceu_vstk_peek(ceu_base+$N+i));
                            }
                            ceu_vstk_repl($idx, xxx);
                        }
                    """ }}

                    ${(CEU >= 2).cond { "ceu_vstk_block(1);" }}
                    $body
                    ${(up is Expr.Loop).cond { """
                        CEU_LOOP_STOP_${up!!.n}:
                    """ }}
                    ${(CEU >= 2).cond { "} while (0);" }}

                    ${(CEU >= 2).cond { "ceu_vstk_block(0);" }}

                    ${(CEU >= 4).cond { """
                        ceu_stack_kill(ceu_bstk, $blkc);
                    """ }}

                    // defers execute
                    #if CEU >= 2
                    ${defers[this].cond { it.third }}
                    #endif
                    
                    ceu_vstk_block_leave();
                    
                    // unlink task/block
                    ${(CEU >= 4).cond {
                        when {
                            (f_b is Expr.Proto && f_b.tk.str == "task") -> """
                                    ceu_frame->exe_task->dn_block = NULL;                        
                                """
                            (f_b !is Expr.Proto) -> """
                                    $bupc->dn.block = NULL;
                                """
                            else -> ""
                        }
                    }}

                    // uncaught throw
                    ${(f_b == null).cond {
                        """
                        #if CEU >= 2
                            if (ceu_acc.type == CEU_VALUE_THROW) {
                                int iserr = (ceu_acc.Dyn->Throw.val.type == CEU_VALUE_ERROR);
                                int N = ceu_acc.Dyn->Throw.stk.Dyn->Vector.its;
                                CEU_Vector* vals = &ceu_acc.Dyn->Throw.stk.Dyn->Vector;
                                for (int i=N-1; i>=0; i--) {
                                    if (iserr && i==0) {
                                        printf(" v  ");
                                    } else {
                                        printf(" |  ");
                                    }
                                    printf("%s", ceu_vector_get(vals,i).Dyn->Vector.buf);
                                    if (iserr && i==0) {
                                        printf(" : ");
                                    } else {
                                        puts("");
                                    }
                                }
                                if (!iserr) {
                                    printf(" v  throw error : ");
                                }
                                ceu_print1(ceu_frame, ceu_acc.Dyn->Throw.val);
                                puts("");
                            }
                        #endif
                        """
                    }}
                    // check error
                    ${(CEU>=2 && (f_b is Expr.Do)).cond { """
                        if (CEU_ISERR(ceu_acc)) {
                            continue;
                        }                        
                    """ }}
                    // check free
                    ${(CEU>=3 && (f_b is Expr.Do) && inexe).cond { """
                        if (ceu_base == CEU_ARG_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                }
                """
            }
            is Expr.Dcl -> {
                if (this.id.upv==1 && clos.vars_refs.none { it==this }) {
                    err(this.tk, "var error : unreferenced upvar")
                }
                when {
                    !this.init -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_NIL }, 1);"
                    (this.src == null) -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_NIL }, 1);"
                    else -> {
                        val idx = mem.pub(this)
                        """
                        // DCL | ${this.dump()}
                        ${this.src.code()}
                        ceu_vstk_repl($idx, ceu_vstk_peek(-1));
                        """
                    }
                }
            }
            is Expr.Set -> """
                { // SET | ${this.dump()}
                    ${this.src.code()}  // src is on the stack and should be returned
                    ${this.dst.code()}  // dst should not pop src
                }
            """
            is Expr.If -> """
                { // IF | ${this.dump()}
                    ${this.cnd.code()}
                    int ceu_$n = ceu_as_bool(ceu_vstk_peek(-1));
                    ceu_vstk_pop(1);
                    if (ceu_$n) {
                        ${this.t.code()}
                    } else {
                        ${this.f.code()}
                    }
                }
                """
            is Expr.Loop -> """
                // LOOP | ${this.dump()}
                CEU_LOOP_START_${this.n}:
                    ${this.blk.code()}
                    if (CEU_BREAK) {
                        CEU_BREAK = 0;
                    } else {
                        ceu_vstk_pop(1);
                        goto CEU_LOOP_START_${this.n};
                    }
            """
            is Expr.Break -> """ // BREAK | ${this.dump()}
                ${this.cnd.code()}
                int ceu_$n = ceu_as_bool(ceu_vstk_peek(-1));
                if (ceu_$n) {
                    //ceu_vstk_pop(1);
                    ${this.e.cond { """
                        ceu_vstk_pop(1); // pop cnd only if e
                        ${it.code()}
                    """ }}
                    CEU_BREAK = 1;
                    goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
                }
            """
            is Expr.Skip -> """ // SKIP | ${this.dump()}
                ${this.cnd.code()}
                int ceu_$n = ceu_as_bool(ceu_vstk_peek(-1));
                //ceu_vstk_pop(1);
                if (ceu_$n) {
                    goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
                }
            """
            is Expr.Enum -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_NIL}, 1);"
            is Expr.Data -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_NIL}, 1);"
            is Expr.Pass -> "// PASS | ${this.dump()}\n" + this.e.code()

            is Expr.Catch -> {
                val blkc = ups.first_block(this)!!.idc("block")
                """
                { // CATCH ${this.dump()}
                    do { // catch
                        ${this.blk.code()}
                    } while (0); // catch
                    // check free
                    ${(CEU>=3 && ups.any(this) { it is Expr.Proto && it.tk.str!="func" }).cond { """
                        if (ceu_base == CEU_ARG_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                    if (ceu_acc.type == CEU_VALUE_THROW) {      // caught internal throw
                        CEU_Value ceu_err = ceu_acc;
                        // in case err=FLEET and condition tries to hold it: do { val x=err }
                        assert(NULL == ceu_hold_set_rec(CEU_HOLD_CMD_SET, ceu_err, CEU_HOLD_MUTAB, $blkc CEU5(COMMA NULL)));
                        do {
                            ${this.cnd.code()}  // ceu_ok = 1|0
                        } while (0);
                        assert(ceu_acc.type!=CEU_VALUE_THROW && "TODO: throw in catch condition");
                        if (!ceu_as_bool(ceu_acc)) {  // condition fail: rethrow error, escape catch block
                            CEU_REPL(ceu_err);
                            continue;
                        } else {        // condition true: catch error, continue after catch block
                            assert(ceu_err.Dyn->Throw.refs == 0);
                            CEU_REPL(ceu_err.Dyn->Throw.val);
                            ceu_gc_rem(ceu_err.Dyn);
                        }
                    }
                }
                """
            }
            is Expr.Defer -> {
                val bup = ups.first_block(this)!!
                val idc = this.idc("defer")
                val (ns,ini,end) = defers.getOrDefault(bup, Triple(mutableListOf(),"",""))
                val inix = """
                    int ceu_defer_$n;
                    $idc = 0;       // NO: do not yet execute on termination
                """
                val endx = """
                    if ($idc) {     // ??: execute only if activate
                        do {
                            ${this.blk.code()}
                        } while (0);    // catch throw
                        assert(ceu_acc.type != CEU_VALUE_THROW && "TODO: throw in defer");
                    }
                """
                ns.add(n)
                defers[bup] = Triple(ns, ini+inix, endx+end)
                """
                $idc = 1;           // OK: execute on termination
                CEU_REPL(((CEU_Value) { CEU_VALUE_NIL }));
                """
            }

            is Expr.Resume -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val coc = this.idc("co")
                val inexeT = ups.inexe(this, null,true)
                val bstk = if (inexeT) "(&ceu_bstk_$n)" else "ceu_bstk"

                """
                ${this.co.code()}
                CEU_Value $coc;
                $coc = ceu_acc;
                if ($coc.type!=CEU_VALUE_EXE_CORO || ($coc.Dyn->Exe.status!=CEU_EXE_STATUS_YIELDED)) {                
                    CEU_Value err = { CEU_VALUE_ERROR, {.Error="resume error : expected yielded coro"} };
                    CEU_ERROR("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                }
                ${this.arg.code()}
                
                ${(CEU>=4 && inexeT).cond { """
                    CEU_Stack ceu_bstk_$n = { $bupc, 1, ceu_bstk };
                """ }}
                
                CEU_REPL($coc.Dyn->Exe.frame.clo->proto(CEU5(ceu_dstk COMMA) CEU4($bstk COMMA) &$coc.Dyn->Exe.frame, 1, &ceu_acc));

                static char* ceu_err_$n = "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}";
                
                ${(CEU>=4 && ups.any(this) { it is Expr.Proto }).cond { """                        
                    if (${(CEU >= 5).cond { "ceu_dstk_isoff(ceu_dstk) ||" }} !$bstk->on) {
                        if (CEU_ISERR(ceu_acc)) {
                            CEU_ERROR_PUSH(ceu_err_$n, ceu_acc);
                        }
                        return ceu_acc;       // TODO: func may leak
                    }
                """ }}

                CEU_ASSERT($bupc, ceu_vstk_peek(-1), ceu_err_$n);                
                """
            }
            is Expr.Yield -> {
                val bupc = ups.first_block(this)!!.idc("block")
                val intsk = ups.inexe(this, "task", true)
                """
                { // YIELD ${this.dump()}
                    ${this.arg.code()}
                    ceu_frame->exe->status = CEU_EXE_STATUS_YIELDED;
                    ceu_frame->exe->pc = $n;      // next resume
                    if (ceu_acc.type>CEU_VALUE_DYNAMIC && ceu_acc.Dyn->Any.hld.type!=CEU_HOLD_FLEET) {
                        CEU_Value err = { CEU_VALUE_ERROR, {.Error="yield error : cannot return pending reference"} };
                        CEU_ERROR("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                    }
                    return ceu_acc;
                case $n: // YIELD ${this.dump()}
                    if (ceu_base == CEU_ARG_ABORT) {
                        CEU_REPL((CEU_Value) { CEU_VALUE_NIL }); // to be ignored in further move/checks
                        continue;
                    }
                    assert(ceu_base <= 1 && "TODO: multiple arguments to resume");
                #if CEU >= 4
                    if (ceu_base == CEU_ARG_ERROR) {
                        CEU_REPL(ceu_args[0]);
                        continue;
                    }
                #endif
                    CEU_REPL((ceu_n == 1) ? ceu_args[0] : (CEU_Value) { CEU_VALUE_NIL });
                    ${intsk.cond2({ """
                        id_evt = ceu_acc;
                        //CEU_REPL((CEU_Value) { CEU_VALUE_NIL });
                    """ }, { """
                        if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                            // must check CEU_HOLD_FLEET for parallel scopes, but only for exes:
                            // [gg_03x_scope]
                            if (ceu_acc.Dyn->Any.hld.type!=CEU_HOLD_FLEET &&
                                !ceu_block_is_up_dn(CEU_HLD_BLOCK(ceu_acc.Dyn), $bupc))
                            {
                                CEU_Value ceu_err_$n = { CEU_VALUE_ERROR, {.Error="resume error : cannot receive alien reference"} };
                                CEU_ERROR("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", ceu_err_$n);
                            }
                        }
                        """
                    })}
                }
                """
            }

            is Expr.Spawn -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val argsc = this.idc("args")
                val tsksc = this.idc("tsks")
                val tskc = this.idc("tsk")
                val inexeT = ups.inexe(this, null, true)
                val bstk = if (inexeT) "(&ceu_bstk_$n)" else "ceu_bstk"

                """
                { // SPAWN | ${this.dump()}
                    ceu_bstk_assert(ceu_bstk);
    
                    ${this.tsks.cond { "CEU_Value $tsksc;" }}
                    CEU_Value $tskc;
                    ${this.tsk.code()}
                    $tskc = ceu_acc;
                    
                    CEU_Value ceu_args_$n[${this.args.size}];
                    ${this.args.mapIndexed { i,e -> """
                        ${e.code()}
                        $argsc[$i] = ceu_acc;
                    """ }.joinToString("")}

                    ${this.tsks.cond2({ """
                        ${it.code()}
                        $tsksc = ceu_acc;
                        CEU_Value ceu_x_$n = ceu_create_exe_task_in($bupc, $tskc, &$tsksc.Dyn->Tasks);
                        if (ceu_x_$n.type == CEU_VALUE_NIL) {
                            CEU_REPL((CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} });
                        } else {
                            // ... below ...
                    """ }, { """
                        CEU_Value ceu_x_$n = ceu_create_exe_task($bupc, $tskc);                    
                    """ })}
                    
                    CEU_ASSERT($bupc, ceu_x_$n, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                    
                    ${inexeT.cond { """
                        if (ceu_base != CEU_ARG_ABORT) {
                            ceu_frame->exe->pc = $n;
                            case $n: // YIELD ${this.dump()}
                                if (ceu_base == CEU_ARG_ABORT) {
                                    CEU_REPL((CEU_Value) { CEU_VALUE_NIL }); // to be ignored in further move/checks
                                    continue;
                                }
                        }
                        CEU_Stack ceu_bstk_$n = { $bupc, 1, ceu_bstk };
                    """ }}
    
                    // for some reason, gcc complains about args[0] for bcast_task(), but not for proto(), so we pass NULL here
                    ceu_acc = ceu_bcast_task(CEU5(ceu_dstk COMMA) $bstk, CEU_TIME_MAX, &ceu_x_$n.Dyn->Exe_Task, ${this.args.size}, ${if (this.args.size>0) argsc else "NULL"});
    
                    static char* ceu_err_$n = "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}";
                    
                    ${(CEU>=4 && ups.any(this) { it is Expr.Proto }).cond { """                        
                        if (${(CEU >= 5).cond { "ceu_dstk_isoff(ceu_dstk) ||" }} !$bstk->on) {
                            if (CEU_ISERR(ceu_acc)) {
                                CEU_ERROR_PUSH(ceu_err_$n, ceu_acc);
                            }
                            return ceu_acc;       // TODO: func may leak
                        }
                    """ }}
                    
                    CEU_ASSERT($bupc, ceu_vstk_peek(-1), ceu_err_$n);
    
                    ${this.tsks.cond2({"""
                            CEU_REPL((CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} });
                        }
                    """}, {"""
                        CEU_REPL(ceu_x_$n);
                    """})}
                } // SPAWN | ${this.dump()}
                """
            }
            is Expr.Delay -> "ceu_frame->exe_task->time = CEU_TIME_MAX;"
            is Expr.Pub -> {
                val bupc = ups.first_block(this)!!.idc("block")
                this.tsk.cond2({
                    tsk as Expr
                    it.code() + """ // PUB | ${this.dump()}
                        if (!ceu_istask_val(ceu_acc)) {
                            CEU_Value err = { CEU_VALUE_ERROR, {.Error="pub error : expected task"} };
                            CEU_ERROR("${this.tsk.tk.pos.file} : (lin ${this.tsk.tk.pos.lin}, col ${this.tsk.tk.pos.col})", err);
                        }
                    """
                },{ """ // PUB | ${this.dump()}
                    ceu_acc = ceu_dyn_to_val((CEU_Dyn*)${up_task_real_c()});
                """ }) +
                when {
                    this.isdst() -> {
                        val src = ups.pub[this]!!.idc("src")
                        """
                        // PUB - SET | ${this.dump()}
                        if ($src.type > CEU_VALUE_DYNAMIC) {
                            // set pub = []   ;; FLEET ;; change to MUTAB type ;; change to pub blk
                            // set pub = src  ;; ELSE  ;; keep ELSE type       ;; keep block
                            // NEW: in both cases, change to IMMUT
                            //  - Check for type=ELSE:
                            //      - blk(pub) >= blk(src) (deeper)
                            // Also error:
                            // set pub = evt
                            char* ceu_err_$n = ceu_hold_set_msg (
                                CEU_HOLD_CMD_PUB,
                                $src,
                                "set error",
                                (ceu_hold_cmd) {.Pub={
                                    ceu_acc.Dyn->Exe_Task.dn_block
                                    CEU5(COMMA $bupc)
                                }}
                            );
                            if (ceu_err_$n != NULL) {
                                CEU_Value err = { CEU_VALUE_ERROR, {.Error=ceu_err_$n} };
                                CEU_ERROR("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                            }

                            ceu_gc_inc($src);
                        }                        
                        ceu_gc_dec(ceu_acc.Dyn->Exe_Task.pub);
                        ceu_acc.Dyn->Exe_Task.pub = $src;
                        """
                    }
                    else -> "CEU_REPL(ceu_acc.Dyn->Exe_Task.pub);\n"
                }
            }
            is Expr.Dtrack -> this.blk.code()
            is Expr.Toggle -> {
                val tskc = this.idc("tsk")
                """
                ${this.tsk.code()}
                CEU_Value $tskc;
                $tskc = ceu_acc;
                ${this.on.code()}
                if (!ceu_istask_val($tskc) || $tskc.Dyn->Exe_Task.status>CEU_EXE_STATUS_TOGGLED) {                
                    CEU_Value err = { CEU_VALUE_ERROR, {.Error="toggle error : expected yielded task"} };
                    CEU_ERROR("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                }
                $tskc.Dyn->Exe_Task.status = (ceu_as_bool(ceu_acc) ? CEU_EXE_STATUS_YIELDED : CEU_EXE_STATUS_TOGGLED);
                CEU_Value ceu_$n = ceu_bcast_task(CEU5(ceu_dstk COMMA) ceu_bstk, 0, &$tskc.Dyn->Exe_Task, CEU_ARG_TOGGLE, NULL);
                assert(ceu_$n.type==CEU_VALUE_BOOL && ceu_$n.Bool);
                """
            }

            is Expr.Nat -> {
                val body = vars.nats[this]!!.let { (set, str) ->
                    var x = str
                    for (dcl in set) {
                        val vblk = vars.dcl_to_blk[dcl]!!
                        val nst = ups
                            .all_until(this) { it==vblk }  // go up until find dcl blk
                            .count { it is Expr.Proto }          // count protos in between acc-dcl
                        val idc = dcl.idc(0)
                        //println(setOf(x, v))
                        x = x.replaceFirst("XXX", idc)
                    }
                    x
                }
                when (this.tk_.tag) {
                    null   -> body + "\n" + "ceu_vstk_push((CEU_Value) { CEU_VALUE_NIL}, 1);\n"
                    ":ceu" -> "ceu_vstk_push($body, 1);"
                    else -> {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        "ceu_vstk_push(((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} }), 1);"
                    }
                }
            }
            is Expr.Acc -> {
                val dcl = vars.acc_to_dcl[this]!!
                val idx = mem.pub(this)
                when {
                    this.isdst() -> {
                        if (dcl.id.upv > 0) {
                            err(tk, "set error : cannot reassign an upval")
                        }
                        """
                        // ACC - SET | ${this.dump()}
                        ceu_vstk_repl($idx, ceu_vstk_peek(-1));
                        """
                    }
                    else -> "ceu_vstk_push(ceu_vstk_peek($idx), 1);\n"
                }
            }
            is Expr.Nil  -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_NIL }, 1);"
            is Expr.Tag  -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_${this.tk.str.tag2c()}} }, 1);"
            is Expr.Bool -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} }, 1);"
            is Expr.Char -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_CHAR, {.Char=${this.tk.str}} }, 1);"
            is Expr.Num  -> "ceu_vstk_push((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} }, 1);"

            is Expr.Tuple -> {
                """
                { // TUPLE | ${this.dump()}
                    ceu_vstk_push(ceu_create_tuple(${this.args.size}), 1);
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        ceu_tuple_set(&ceu_vstk_peek(-2).Dyn->Tuple, $i, ceu_vstk_peek(-1));
                        ceu_vstk_pop(1);
                        """
                    }.joinToString("")}
                }
                """
            }
            is Expr.Vector -> {
                """
                { // VECTOR | ${this.dump()}
                    ceu_vstk_push(ceu_create_vector(), 1);
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        ceu_vector_set(&ceu_vstk_peek(-2).Dyn->Vector, $i, ceu_vstk_peek(-1));
                        ceu_vstk_pop(1);
                        """
                    }.joinToString("")}
                }
                """
            }
            is Expr.Dict -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                """
                { // DICT | ${this.dump()}
                    ceu_vstk_push(ceu_create_dict(), 1);
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            ${it.second.code()}
                            CEU_ASSERT(
                                ceu_dict_set(&ceu_vstk_peek(-3).Dyn->Dict, ceu_vstk_peek(-2), ceu_vstk_peek(-1)),
                                "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                            );
                            ceu_vstk_drop(2);
                        }
                    """ }.joinToString("")}
                }
                """
            }
            is Expr.Index -> {
                val blk = ups.first_block(this)!!
                val bupc = blk.idc("block")
                val idx = vars.data(this).let { if (it == null) -1 else it.first!! }
                """
                { // INDEX | ${this.dump()}
                    // IDX
                    ${if (idx == -1) {
                        this.idx.code()
                    } else {
                        """
                        ceu_vstk_push((CEU_Value) { CEU_VALUE_NUMBER, {.Number=$idx} }, 1);
                        """
                    }}
                    
                    // COL
                    ${this.col.code()}
                    CEU_ASSERT(ceu_col_check(ceu_vstk_peek(-1),ceu_vstk_peek(-2)), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                """ + when {
                    this.isdst() -> {
                        """
                        CEU_Value ceu_$n = ceu_col_set(ceu_vstk_peek(-1), ceu_vstk_peek(-2), ceu_vstk_peek(-3));
                        CEU_ASSERT(ceu_$n, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        ceu_vstk_drop(2);    // keep src
                        """
                    }
                    else -> """
                        CEU_Value ceu_$n = CEU_ASSERT(ceu_col_get(ceu_vstk_peek(-1),ceu_vstk_peek(-2)), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        ceu_gc_inc(ceu_$n);
                        ceu_vstk_drop(2);
                        ceu_vstk_push(ceu_$n, 1);
                        ceu_gc_dec(ceu_$n);
                    """
                } + """
                }
                """
            }
            is Expr.Call -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val dots = this.args.lastOrNull()
                val has_dots = (dots!=null && dots is Expr.Acc && dots.tk.str=="...") && !this.clo.let { it is Expr.Acc && it.tk.str=="{{#}}" }
                val id_dots = if (!has_dots) "" else {
                    vars.acc_to_dcl[dots as Expr.Acc]!!.idc(0)
                }
                val inexeT = ups.inexe(this, null, true)
                val bstk = if (inexeT) "(&ceu_bstk_$n)" else "ceu_bstk"
                """
                { // CALL | ${this.dump()}
                    ${this.clo.code()}
                    ${this.args.filter{!(has_dots && it.tk.str=="...")}.mapIndexed { i,e -> """
                        ${e.code()}
                    """ }.joinToString("")}                    
                    ${has_dots.cond { """
                        -=- TODO -=-
                        for (int ceu_i_$n=0; ceu_i_$n<$id_dots.Dyn->Tuple.its; ceu_i_$n++) {
                            ceu_vstk_push($id_dots.Dyn->Tuple.buf[ceu_i_$n], 1);
                        }
                    """ }}

                    // call -> bcast -> outer abortion -> need to clean up from here
                    ${(CEU>=4 && inexeT).cond { """
                        if (ceu_base != CEU_ARG_ABORT) {
                            ceu_frame->exe->pc = $n;
                            case $n: // YIELD ${this.dump()}
                                if (ceu_base == CEU_ARG_ABORT) {
                                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL }; // to be ignored in further move/checks
                                    continue;
                                }
                        }
                        CEU_Stack ceu_bstk_$n = { $bupc, 1, ceu_bstk };
                    """ }}
                    
                    ceu_vstk_call(${this.args.size});
                    
                    ${(CEU>=4 && ups.any(this) { it is Expr.Proto }).cond { """                        
                        if (${(CEU >= 5).cond { "ceu_dstk_isoff(ceu_dstk) ||" }} !$bstk->on) {
                            if (CEU_ISERR(ceu_acc)) {
                                CEU_ERROR_PUSH(ceu_err_$n, ceu_acc);
                            }
                            return ceu_acc;       // TODO: func may leak
                        }
                    """ }}

                    static char* ceu_err_$n = "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}";
                    CEU_ASSERT(ceu_vstk_peek(-1), ceu_err_$n);
                } // CALL | ${this.dump()}
                """
            }
        }
    }
}
