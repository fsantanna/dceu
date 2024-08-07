package dceu

import kotlin.math.max

class Coder (val outer: Expr.Do, val ups: Ups, val vars: Vars, val sta: Static) {
    // Pair<mems,protos>: need to separate b/c protos must be inner->outer, while mems outer->inner
    val pres: MutableList<Pair<String,String>> = mutableListOf()
    val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>> = mutableMapOf()
    val code: String = outer.code()

    fun List<Expr>.code (): String {
        return this.map { it.code() }.joinToString("")
    }

    fun Expr.toerr (): String {
        val src = this.tostr(false).quote(45)
        return "\"${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : $src\""
    }

    fun Expr.check_aborted (cmd: String, msg: String): String {
        val exe = ups.exe(this)
        val defer = ups.first_without(this, { it is Expr.Defer }, { it is Expr.Proto })
        return (CEU>=3 && exe!=null && defer==null).cond { """
            if (ceux->exe->status == CEU_EXE_STATUS_TERMINATED) {
                $cmd;
            }
        """ }
    }
    fun Expr.check_error_aborted (cmd: String, msg: String): String {
        return """
            CEU_ERROR_CHK_ACC($cmd, $msg);
            ${this.check_aborted(cmd, msg)}
        """
    }

    fun Expr.code(): String {
        if (ups.isdst(this)) {
            assert(this is Expr.Acc || this is Expr.Index || this is Expr.Pub)
        }
        return when (this) {
            is Expr.Proto -> {
                val isexe = (this.tk.str != "func")
                val istsk = (this.tk.str == "task")
                val isnst = ups.isnst(this)
                val code = this.blk.code()
                val id = this.id(outer,ups)

                pres.add(Pair("""
                    // PROTO | ${this.dump()}
                    ${isexe.cond { """
                        typedef struct CEU_Pro_$id {
                            ${Mem(ups, vars, sta, defers).pub(this)}
                        } CEU_Pro_$id;                        
                    """ }}
                ""","""
                    // PROTO | ${this.dump()}
                    void ceu_pro_$id (CEUX* ceux) {
                        //{ // upvs
                            ${vars.proto_to_upvs[this]!!.mapIndexed { i, dcl -> """
                                CEU_Value ceu_upv_${dcl.idtag.first.str.idc()} = ceux->clo->upvs.buf[$i];                            
                            """ }.joinToString("")}
                        //}

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
                            ${this.pars.mapIndexed { i,(id,_) -> """
                                ${sta.ismem(this.blk).cond2({ """
                                    ceu_mem->${id.str.idc()}
                                """ },{ """
                                    CEU_Value ceu_par_${id.str.idc()}
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

                        //{ // pars
                            ${this.pars.mapIndexed { i,(id,_) -> """
                                ceu_gc_dec_val (
                                    ${sta.ismem(this.blk).cond2({ """
                                        ceu_mem->${id.str.idc()}
                                    """ },{ """
                                        ceu_par_${id.str.idc()}
                                    """ })}
                                );
                            """ }.joinToString("")}
                        //}

                        ${isexe.cond{"""
                                ceu_exe_term(ceux);
                            } // close switch
                        """}}
                    }
                """))

                //assert(!this.isva) { "TODO" }
                """ // CREATE | ${this.dump()}
                {
                    ${isnst.cond { "assert(ceux->exe!=NULL && ceux->exe->type==CEU_VALUE_EXE_TASK);" }}
                    CEU_ACC (
                        ceu_create_clo_${this.tk.str} (
                            ceu_pro_$id,
                            ${this.pars.size},
                            ${vars.proto_to_upvs[this]!!.size}
                            ${isexe.cond {", sizeof(CEU_Pro_$id)"}}
                        )
                    );
                    ${isexe.cond { """
                        // TODO: use args+locs+upvs+tmps?
                        //clo.Dyn->Clo_Exe.mem_n = sizeof(CEU_Clo_Mem_$id);                    
                    """ }}
                    
                    // UPVALS = ${vars.proto_to_upvs[this]!!.size}
                    {                        
                        ${vars.proto_to_upvs[this]!!.mapIndexed { i,dcl ->
                        """
                        {
                            CEU_Value upv = ${sta.idx(dcl, ups.pub[this]!!)};
                            ceu_gc_inc_val(upv);
                            ceu_acc.Dyn->Clo.upvs.buf[$i] = upv;
                        }
                        """
                        }.joinToString("\n")}
                    }
                }
                """
            }
            is Expr.Do -> {
                val body = this.es.code()   // before defers[this] check
                val up = ups.pub[this]

                val void = sta.void(this)
                if (void) {
                    """
                    { // BLOCK | void | ${this.dump()}
                        $body
                    }
                    """
                } else {
                    val blkc = sta.idx(this, "block_$n")
                    """
                    { // BLOCK | ${this.dump()}
                        ${(CEU >= 4).cond { """
                             ${(!sta.ismem(this)).cond { "CEU_Block" }} $blkc = NULL;
                        """}}
                        ${(this == outer).cond { """
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
                        
                        ${(this != outer).cond { 
                            vars.blk_to_dcls[this]!!.let { dcls ->
                                """
                                ${(!sta.ismem(this)).cond { """
                                    //{ // inline vars dcls
                                        ${dcls.map { """
                                            CEU_Value ${sta.idx(it,it)};
                                        """ }.joinToString("")}
                                    //}
                                """ }}
                                { // vars inits
                                    ${dcls.map { """
                                        ${sta.idx(it,it)} = (CEU_Value) { CEU_VALUE_NIL };
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
                            ${vars.blk_to_dcls[this]!!
                                .asReversed()
                                //.filter { !GLOBALS.contains(it.idtag.first.str) }
                                .map { """
                                    ceu_gc_dec_val(${sta.idx(it, it)});
                                """ }
                                .joinToString("")
                            }
                        }                        

                        ceu_acc = ceu_acc_$n;
                        
                        ${(CEU >= 2).cond { this.check_error_aborted("continue", "NULL")} }
                    }
                    """
                }
            }
            is Expr.Group -> "// GROUP | ${this.dump()}\n" + this.es.code()
            is Expr.Dcl -> {
                val idx = sta.idx(this, this)
                """
                // DCL | ${this.dump()}
                ${when {
                    sta.protos_use_unused.contains(this.src) -> """
                        // $idx: unused function
                    """
                    (this.src != null) -> """
                        ${this.src.code()}
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
                    if (CEU_BREAK) {
                        CEU_BREAK = 0;
                    } else {
                        goto CEU_LOOP_START_${this.n};
                    }
            """
            is Expr.Break -> """ // BREAK | ${this.dump()}
            {
                ${this.e.cond { """
                    ${it.code()}
                """ }}
                CEU_BREAK = 1;
                goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
            }
            """
            is Expr.Skip -> """ // SKIP | ${this.dump()}
                goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
            """
            is Expr.Data -> "// DATA | ${this.dump()}\n"

            is Expr.Catch -> {
                """
                { // CATCH ${this.dump()}
                    do { // catch
                        ${this.blk.code()}
                    } while (0); // catch
                    // check free
                    ${(CEU>=3 && ups.any(this) { it is Expr.Proto && it.tk.str!="func" }).cond { """
                        if (ceux->act == CEU_ACTION_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                    if (ceu_acc.type == CEU_VALUE_ERROR) {  // caught internal throw
                        // [msgs,val,err]
                        CEU_Value ceu_acc_$n = ceu_acc;
                        ceu_gc_inc_val(ceu_acc);
                        ${this.cnd.code()}                  // ceu_ok = 1|0
                        assert(ceu_acc.type!=CEU_VALUE_ERROR && "TODO: throw in catch condition");
                        if (!ceu_as_bool(ceu_acc)) {        // condition fail: rethrow error, escape catch block
                            ceu_acc = ceu_acc_$n;
                            continue;
                        } else {                            // condition true: catch error, continue after catch block
                            CEU_ACC(*(ceu_acc_$n.Dyn->Error.val));
                            ceu_gc_dec_val(ceu_acc_$n);
                        }
                    }
                }
                """
            }
            is Expr.Defer -> {
                val bup = ups.first(this) { it is Expr.Do } as Expr.Do
                val (ns,ini,end) = defers.getOrDefault(bup, Triple(mutableListOf(),"",""))
                val id = sta.idx(this, "defer_$n")
                val inix = """
                    ${sta.dcl(this,"int")} $id = 0;   // not yet reached
                """
                val endx = """
                    if ($id) {     // if true: reached, finalize
                        do {
                            ${this.blk.code()}
                        } while (0);    // catch throw
                        assert(ceu_acc.type!=CEU_VALUE_ERROR && "TODO: error in defer");
                        CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
                    }
                """
                ns.add(n)
                defers[bup] = Triple(ns, ini+inix, endx+end)
                """
                $id = 1;   // now reached
                CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));
                """
            }

            is Expr.Resume -> """
                { // RESUME | ${this.dump()}
                    ${(!sta.ismem(this)).cond { """
                        CEU_Value ceu_args_$n[${this.args.size}];
                    """ }}
                    ${this.args.mapIndexed { i,e ->
                        e.code() + """
                            ${sta.idx(this,"args_$n")}[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}
                    
                    ${this.co.code()}
                    CEU_Value ceu_coro_$n = CEU_ACC_KEEP();
                    if (ceu_coro_$n.type!=CEU_VALUE_EXE_CORO || (ceu_coro_$n.Dyn->Exe.status!=CEU_EXE_STATUS_YIELDED)) {                
                        ceu_gc_dec_val(ceu_coro_$n);
                        CEU_ERROR_CHK_PTR (
                            continue,
                            "resume error : expected yielded coro",
                            ${this.toerr()}
                        );
                    }

                    CEUX ceux_$n = {
                        ceu_coro_$n.Dyn->Exe.clo,
                        {.exe = (CEU_Exe*) ceu_coro_$n.Dyn},
                        CEU_ACTION_RESUME,
                    #if CEU >= 4
                        ceux,
                    #endif
                        ${this.args.size},
                        ${sta.idx(this,"args_$n")}
                    };
                    ceu_coro_$n.Dyn->Exe.clo->proto(&ceux_$n);
                    ceu_gc_dec_val(ceu_coro_$n);
                    ${this.check_error_aborted("continue", this.toerr())}
                } // CALL | ${this.dump()}
            """
            is Expr.Yield -> {
                """
                { // YIELD ${this.dump()}
                    ${this.e.code()}
                    ceux->exe->status = CEU_EXE_STATUS_YIELDED;
                    ceux->exe->pc = $n;
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
                    ceu_gc_dec_val(ceu_acc);
                    ceu_acc = (ceux->n > 0) ? ceux->args[0] : (CEU_Value) { CEU_VALUE_NIL };
                }
            """
            }

            is Expr.Spawn -> {
                val blk = ups.first(this) { it is Expr.Do } as Expr.Do
                val blkc = sta.idx(blk, "block_${blk.n}")
                """
                { // SPAWN | ${this.dump()}
                    ${(!sta.ismem(this)).cond { """
                        CEU_Value ceu_tsks_$n;
                        CEU_Value ceu_args_$n[${this.args.size}];
                    """ }}

                    ${(CEU>=5 && this.tsks!=null).cond {
                        this.tsks!!.code() + """
                            ${sta.idx(this,"tsks_$n")} = ceu_acc;                            
                        if (ceu_acc.type != CEU_VALUE_TASKS) {
                            CEU_ERROR_CHK_PTR (
                                continue,
                                "spawn error : invalid pool",
                                ${this.toerr()}
                            );
                        }
                        """
                    }}
                    
                    ${this.args.mapIndexed { i,e ->
                        e.code() + """
                            ${sta.idx(this,"args_$n")}[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}

                    ${this.tsk.code()}
                    CEU_Dyn* ceu_a_$n = ${when {
                        (CEU<5 || this.tsks==null) -> "(CEU_Dyn*)ceu_task_up(ceux)"
                        else -> sta.idx(this,"tsks_$n") + ".Dyn"
                    }};
                    CEU_Block* ceu_b_$n = ${this.tsks.cond2({"NULL"}, {"&$blkc"})};
                    CEU_Value ceu_exe_$n = ceu_create_exe_task(ceu_acc, ceu_a_$n, ceu_b_$n);
                    CEU_ACC(ceu_exe_$n);
                    CEU_ERROR_CHK_ACC(continue, ${this.toerr()});
                    
                    ${(CEU>=5 && this.tsks!=null).cond { """
                        if (ceu_acc.type != CEU_VALUE_NIL)
                    """ }}
                    {
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                        CEUX ceux_$n = {
                            ceu_exe_$n.Dyn->Exe.clo,
                            {.exe = (CEU_Exe*) ceu_exe_$n.Dyn},
                            CEU_ACTION_RESUME,
                            ceux,
                            ${this.args.size},
                            ${sta.idx(this,"args_$n")}
                        };
                        ceu_exe_$n.Dyn->Exe.clo->proto(&ceux_$n);
                        CEU_ERROR_CHK_ACC({ceu_gc_dec_val(ceu_exe_$n);continue;}, ${this.toerr()});
                        ceu_gc_dec_val(ceu_acc);
                        ${this.check_aborted("{ceu_gc_dec_val(ceu_exe_$n);continue;}", this.toerr())}
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
                val id = sta.idx(this, "val_$n")
                val exe = if (this.tsk != null) "" else {
                    ups.first_task_outer(this).let { outer ->
                        val n = ups.all_until(this) {
                            it is Expr.Proto && it.tk.str=="task" && !ups.isnst(it)
                        }
                            .filter { it is Expr.Proto } // but count all protos in between
                            .count() - 1
                        "(ceux->exe_task${"->lnks.up.tsk".repeat(n)})"
                    }
                }
            """
            { // PUB | ${this.dump()}
                ${ups.isdst(this).cond{ """
                    ${sta.dcl(this,"CEU_Value")} $id = CEU_ACC_KEEP();
                """ }}
                ${this.tsk.cond2({"""
                    ${it.code()}
                    CEU_Value tsk = ceu_acc;
                    if (!ceu_istask_val(tsk)) {
                        CEU_ERROR_CHK_PTR (
                            continue,
                            "pub error : expected task",
                            ${this.toerr()}
                        );
                    }
                """},{"""
                    CEU_Value tsk = ceu_dyn_to_val((CEU_Dyn*)$exe);
                """})}
                ${ups.isdst(this).cond2({ """
                    ceu_gc_dec_val(tsk.Dyn->Exe_Task.pub);
                    tsk.Dyn->Exe_Task.pub = $id;
                """ },{ """
                    CEU_ACC(tsk.Dyn->Exe_Task.pub);
                """ })}
            }
            """
            }
            is Expr.Toggle -> {
                val id = sta.idx(this, "tsk_$n")
                """
                {  // TOGGLE | ${this.dump()}
                    ${this.tsk.code()}
                    ${sta.dcl(this,"CEU_Value")} $id = CEU_ACC_KEEP();
                    ${this.on.code()}
                    {   // TOGGLE | ${this.dump()}
                        int on = ceu_as_bool(ceu_acc);
                        int ceu_err_$n = 0;
                        if (!ceu_istask_val($id)) {
                            CEU_ERROR_CHK_PTR (
                                continue,
                                "toggle error : expected yielded task",
                                ${this.toerr()}
                            );
                        }
                        if (!ceu_err_$n && on && $id.Dyn->Exe_Task.status!=CEU_EXE_STATUS_TOGGLED) {                
                            CEU_ERROR_CHK_PTR (
                                {ceu_err_$n = 1;},
                                "toggle error : expected toggled task",
                                ${this.toerr()}
                            );
                        }
                        if (!ceu_err_$n && !on && $id.Dyn->Exe_Task.status!=CEU_EXE_STATUS_YIELDED) {                
                            CEU_ERROR_CHK_PTR (
                                {ceu_err_$n = 1;},
                                "toggle error : expected yielded task",
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
                val blk = ups.first(this) { it is Expr.Do } as Expr.Do
                val blkc = sta.idx(blk, "block_${blk.n}")
                """
                {  // TASKS | ${this.dump()}
                    ${this.max.code()}
                    CEU_Value ceu_tsks_$n = ceu_create_tasks(ceux, &$blkc, ceu_acc);
                    CEU_ACC(ceu_tsks_$n);
                    CEU_ERROR_CHK_ACC(continue, ${this.toerr()});
                }
                """
            }

            is Expr.Nat -> {
                val body = vars.nats[this]!!.let { (set, str) ->
                    var x = str
                    for (dcl in set) {
                        val idx = sta.idx(dcl, this)
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
                val idx = sta.idx(this)
                when {
                    ups.isdst(this) -> """
                        // ACC - SET | ${this.dump()}
                        ceu_gc_dec_val($idx);
                        ceu_gc_inc_val(ceu_acc);
                        $idx = ceu_acc;
                    """
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
                val id_args = sta.idx(this,"args_$n")
                """
                { // TUPLE | ${this.dump()}
                    ${(!sta.ismem(this)).cond { """
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
                val id_vec = sta.idx(this,"vec_$n")
                """
                { // VECTOR | ${this.dump()}
                    ${sta.dcl(this)} $id_vec = ceu_create_vector();
                    ${this.args.mapIndexed { i, it ->
                    it.code() + """
                         ceu_vector_set(&$id_vec.Dyn->Vector, $i, ceu_acc);
                        """
                }.joinToString("")}
                    CEU_ACC($id_vec);
                }
            """
            }
            is Expr.Dict -> {
                val id_dic = sta.idx(this,"dic_$n")
                val id_key = sta.idx(this,"key_$n")
                """
                { // DICT | ${this.dump()}
                    ${sta.dcl(this)} $id_dic = ceu_create_dict();
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            ${sta.dcl(this)} $id_key = CEU_ACC_KEEP();
                            ${it.second.code()}
                            CEU_Value ceu_val_$n = CEU_ACC_KEEP();
                            CEU_ERROR_CHK_PTR (
                                continue,
                                ceu_dict_set(&${id_dic}.Dyn->Dict, $id_key, ceu_val_$n),
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
                val idx = vars.data(this).let { if (it == null) -1 else it.first!! }
                val id_col = sta.idx(this, "col_$n")
                val id_val = sta.idx(this, "val_$n")
                """
                { // INDEX | ${this.dump()}
                    // VAL
                    ${ups.isdst(this).cond { """
                        ${sta.dcl(this)} $id_val = CEU_ACC_KEEP();
                    """ }}
                    
                    // COL
                    ${this.col.code()}
                    ${sta.dcl(this)} $id_col = CEU_ACC_KEEP();

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
                if (ups.isdst(this)) {
                    """
                    {
                        char* ceu_err_$n = ceu_col_set($id_col, ceu_idx_$n, $id_val);
                        ceu_gc_dec_val($id_col);
                        ceu_gc_dec_val(ceu_idx_$n);
                        CEU_ERROR_CHK_PTR (
                            continue,
                            ceu_err_$n,
                            ${this.toerr()}
                        );
                        ceu_acc = $id_val;
                    }
                    """
                } else {
                    """
                    CEU_ACC(ceu_col_get($id_col, ceu_idx_$n));
                    ceu_gc_dec_val($id_col);
                    ceu_gc_dec_val(ceu_idx_$n);
                    CEU_ERROR_CHK_ACC(continue, ${this.toerr()});
                    """
                } + """
                }
                """
            }
            is Expr.Call -> """
                { // CALL | ${this.dump()}
                    ${(!sta.ismem(this)).cond { """
                        CEU_Value ceu_args_$n[${this.args.size}];
                    """ }}
                    ${this.args.mapIndexed { i,e ->
                        e.code() + """
                            ${sta.idx(this,"args_$n")}[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}
                    ${this.clo.code()}
                    CEU_Value ceu_clo_$n = CEU_ACC_KEEP();
                    if (ceu_clo_$n.type != CEU_VALUE_CLO_FUNC) {
                        ceu_gc_dec_val(ceu_clo_$n);
                        CEU_ERROR_CHK_PTR (
                            continue,
                            "call error : expected function",
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
                        ${this.args.size},
                        ${sta.idx(this, "args_$n")}
                    };
                    ceu_clo_$n.Dyn->Clo.proto(&ceux_$n);
                    ceu_gc_dec_val(ceu_clo_$n);
                    ${this.check_error_aborted("continue", this.toerr())}
                } // CALL | ${this.dump()}
            """
        }
    }
}
