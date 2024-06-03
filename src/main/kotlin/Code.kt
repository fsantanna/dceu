package dceu

import kotlin.math.max

class Coder (val outer: Expr.Do, val ups: Ups, val vars: Vars, val sta: Static) {
    val pres: MutableList<String> = mutableListOf()
    val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>> = mutableMapOf()
    val code: String = outer.code()
    fun Expr.Proto.idc (): String {
        return ups.pub[this].let {
            when {
                (it !is Expr.Dcl) -> this.n.toString()
                (it.src != this) -> error("bug found")
                else -> it.idtag.first.str.idc()
            }
        }
    }

    fun Expr.up_task_real_c (): String {
        val n = ups.all_until(this) {
                it is Expr.Proto && it.tk.str=="task" && !ups.isnst(it)
            }
            .filter { it is Expr.Proto } // but count all protos in between
            .count()
        val (x,y) = Pair("ceu_frame_up_frame(".repeat(n-1), ")".repeat(n-1))
        return "(($x ceu_frame $y)->exe_task)"
    }

    fun List<Expr>.code (): String {
        return this.map { it.code() }.joinToString("")
    }

    fun Expr.toerr (): String {
        val src = this.tostr(false).quote(45)
        return "\"${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : $src\""
    }

    fun Expr.check_error_aborted (msg: String): String {
        val exe = ups.exe(this)
        val defer = ups.first_without(this, { it is Expr.Defer }, { it is Expr.Proto })
        return """
            CEU_ERROR_CHK_ACC(continue, $msg);
            ${(CEU>=3 && exe!=null && defer==null).cond { """
                if (ceux->exe->status == CEU_EXE_STATUS_TERMINATED) {
                    continue;
                }
            """ }}
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
                val id = this.idc() + (ups.first(this) { it is Expr.Do } != outer).cond { "_${this.n}" }

                pres.add("""
                    // PROTO | ${this.dump()}
                    ${isexe.cond { """
                        typedef struct {
                            ${Mem(ups, vars, sta, defers).pub(this)}
                        } CEU_Pro_$id;                        
                    """ }}
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
                                //ceu_exe_term(X);
                                ceux->exe->status = CEU_EXE_STATUS_TERMINATED;
                            } // close switch
                        """}}
                    }
                """)

                //assert(!this.isva) { "TODO" }
                """ // CREATE | ${this.dump()}
                {
                    ${isnst.cond { "assert(X->exe!=NULL && X->exe->type==CEU_VALUE_EXE_TASK);" }}
                    CEU_ACC (
                        ceu_create_clo_${this.tk.str} (
                            ceu_pro_$id,
                            ${this.pars.size},
                            ${vars.proto_to_upvs[this]!!.size}
                            ${isexe.cond {", sizeof(CEU_Pro_$id)"}}
                            ${istsk.cond { ", ${if (isnst) "X->exe_task" else "NULL"}" }}
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
                        ${(!sta.ismem(this)).cond { "CEU_Block" }} $blkc = NULL;
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
    
                        ${defers[this].cond { """
                            { // BLOCK | defers | term | ${this.dump()}
                                CEU_Value ceu_acc_$n = CEU_ACC_KEEP();
                                ${it.third}
                                ceu_acc = ceu_acc_$n;
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
                        ${(CEU >= 2).cond { this.check_error_aborted("NULL")} }
                    }
                    """
                }
            }
            is Expr.Dcl -> {
                val idx = sta.idx(this, this)
                """
                // DCL | ${this.dump()}
                ${when {
                    sta.protos_use_unused.contains(this.src) -> """
                        // $idx: unused function
                    """
                    (this.src != null) -> """
                        ${this.src!!.code()}
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
                ${this.cnd.code()}
                if (ceu_as_bool(ceu_acc)) {
                    ${this.e.cond { """
                        ${it.code()}
                    """ }}
                    CEU_BREAK = 1;
                    goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
                }
            """
            is Expr.Skip -> """ // SKIP | ${this.dump()}
                ${this.cnd.code()}
                {
                    int v = ceu_as_bool(ceu_acc);
                    if (v) {
                        goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
                    }
                }
            """
            is Expr.Enum -> "// ENUM | ${this.dump()}\n"
            is Expr.Data -> "// DATA | ${this.dump()}\n"
            is Expr.Pass -> "// PASS | ${this.dump()}\n" + this.e.code()

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
                        ${this.args.size},
                        ${sta.idx(this,"args_$n")}
                    };
                    ceu_coro_$n.Dyn->Exe.clo->proto(&ceux_$n);
                    ceu_gc_dec_val(ceu_coro_$n);
                    ${this.check_error_aborted(this.toerr())}
                } // CALL | ${this.dump()}
            """

            is Expr.Yield -> {
                """
                { // YIELD ${this.dump()}
                    ${this.arg.code()}
                    ceux->exe->status = CEU_EXE_STATUS_YIELDED;
                    ceux->exe->pc = $n;
                    return;
                case $n: // YIELD ${this.dump()}
                    if (ceux->act == CEU_ACTION_ABORT) {
                        CEU_ACC((CEU_Value) { CEU_VALUE_NIL }); // to be ignored in further move/checks
                        continue;
                    }
                    ceu_gc_dec_val(ceu_acc);
                    ceu_acc = ((ceux->n > 0) ? ceux->args[0] : (CEU_Value) { CEU_VALUE_NIL });
                #if CEU >= 4
                    if (ceux->act == CEU_ACTION_ERROR) {
                        continue;
                    }
                #endif
                }
            """
            }

            is Expr.Spawn -> {
                val blk = ups.first(this) { it is Expr.Do } as Expr.Do
                val blkc = sta.idx(blk, "block_${blk.n}")
                """
                { // SPAWN | ${this.dump()}
                    ${(CEU >= 5).cond { """
                        ${this.tsks.cond2({
                            it.code()
                        },{
                            "ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });"
                        })}
                    """ }}
                    
                    ${(!sta.ismem(this)).cond { """
                        CEU_Value ceu_args_$n[${this.args.size}];
                    """ }}
                    ${this.args.mapIndexed { i,e ->
                        e.code() + """
                            ${sta.idx(this,"args_$n")}[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}

                    ${this.tsk.code()}
                    CEU_Value ceu_exe_$n = ceu_create_exe_task(ceu_acc, (CEU_Dyn*)&CEU_GLOBAL_TASK, &$blkc);
                    CEU_ACC(ceu_exe_$n);
                    CEU_ERROR_CHK_ACC(continue, ${this.toerr()});
                    ceu_gc_inc_val(ceu_exe_$n);

                    CEUX ceux_$n = {
                        ceu_exe_$n.Dyn->Exe.clo,
                        {.exe = (CEU_Exe*) ceu_exe_$n.Dyn},
                        CEU_ACTION_RESUME,
                        ${this.args.size},
                        ${sta.idx(this,"args_$n")}
                    };
                    ceu_exe_$n.Dyn->Exe.clo->proto(&ceux_$n);
                    ceu_acc = ceu_exe_$n;
                    ${this.check_error_aborted(this.toerr())}
                } // SPAWN | ${this.dump()}
                """
            }
            is Expr.Delay -> """
                // DELAY | ${this.dump()}
                ceux->exe_task->time = CEU_TIME;
            """
            is Expr.Pub -> {
                val exe = if (this.tsk != null) "" else {
                    ups.first_task_outer(this).let { outer ->
                        val xups = ups.all_until(this) { it == outer } // all ups between this -> outer
                        val n = xups.count { it is Expr.Proto }
                        "ceux${"->exe_task->clo.Dyn->Clo_Task.up_tsk->X".repeat(n-1)}->exe_task"
                    }
                }
            """
            { // PUB | ${this.dump()}
                ${this.tsk.cond { it.code() }}
                CEU_Value tsk = ${this.tsk.cond2({"""
                    ceux_peek(X->S, XX(-1))
                """},{"""
                    //ceu_dyn_to_val((CEU_Dyn*)${up_task_real_c()});
                    ceu_dyn_to_val((CEU_Dyn*)$exe)
                """})}
                ;
                ${this.tsk.cond { """
                    if (!ceu_istask_val(tsk)) {
                        CEU_ERROR_THR_S(continue, "pub error : expected task", ${this.toerr()});
                    }
                """ }}
                ${ups.isdst(this).cond2({ """
                    // [v,(tsk)]
                    CEU_Value v = ceux_peek(X->S, XX(${this.tsk.cond2({"-2"},{"-1"})}));
                    ceu_gc_inc_val(v);
                    ceu_gc_dec_val(tsk.Dyn->Exe_Task.pub);
                    tsk.Dyn->Exe_Task.pub = v;
                    ${this.tsk.cond { "ceux_pop(X->S, 1);" }}
                    // [v]
                """ },{ """
                    // [(tsk)]
                    ${this.tsk.cond2({"""
                        ceux_repl(X->S, XX(-1), tsk.Dyn->Exe_Task.pub);                        
                    """},{"""
                        ceux_push(X->S, 1, tsk.Dyn->Exe_Task.pub);
                    """})}
                    // [pub]
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
                        if (!ceu_istask_val($id)) {
                            CEU_ERROR_CHK_PTR (
                                continue,
                                "toggle error : expected yielded task",
                                ${this.toerr()}
                            );
                        }
                        if (on && $id.Dyn->Exe_Task.status!=CEU_EXE_STATUS_TOGGLED) {                
                            CEU_ERROR_CHK_PTR (
                                continue,
                                "toggle error : expected toggled task",
                                ${this.toerr()}
                            );
                        }
                        if (!on && $id.Dyn->Exe_Task.status!=CEU_EXE_STATUS_YIELDED) {                
                            CEU_ERROR_CHK_PTR (
                                continue,
                                "toggle error : expected yielded task",
                                ${this.toerr()}
                            );
                        }
                        $id.Dyn->Exe_Task.status = (on ? CEU_EXE_STATUS_YIELDED : CEU_EXE_STATUS_TOGGLED);
                    }
                    ceu_gc_dec_val($id);
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
                        ${this.check_error_aborted(this.toerr())}
                    """
                    ":pre" -> {
                        pres.add(body)
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
                    CEU_ERROR_CHK_PTR (
                        continue,
                        ceu_col_set($id_col, ceu_idx_$n, $id_val),
                        ${this.toerr()}
                    );
                    ceu_acc = $id_val;
                    """
                } else {
                    """
                    CEU_ACC(ceu_col_get($id_col, ceu_idx_$n));
                    CEU_ERROR_CHK_ACC(continue, ${this.toerr()});
                    """
                } + """
                    ceu_gc_dec_val($id_col);
                    ceu_gc_dec_val(ceu_idx_$n);
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
                        ${this.args.size},
                        ${sta.idx(this, "args_$n")}
                    };
                    ceu_clo_$n.Dyn->Clo.proto(&ceux_$n);
                    ceu_gc_dec_val(ceu_clo_$n);
                    ${this.check_error_aborted(this.toerr())}
                } // CALL | ${this.dump()}
            """
        }
    }
}
