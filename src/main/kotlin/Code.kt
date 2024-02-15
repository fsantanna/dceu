package dceu

fun do_while (code: String): String {
    return (CEU >= 2).cond { "do {" } + code + (CEU >= 2).cond { "} while (0);" }

}

class Coder (val outer: Expr.Call, val ups: Ups, val vars: Vars, val rets: Rets) {
    val pres: MutableList<String> = mutableListOf()
    val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>> = mutableMapOf()
    val code: String = outer.code()

    fun Expr.idc (pre: String): String {
        return "ceu_${pre}_${this.n}"
    }
    fun Expr.Proto.idc (): String {
        return ups.pub[this].let {
            when {
                (it !is Expr.Dcl) -> this.n.toString()
                (it.src != this) -> error("bug found") as String
                else -> it.idtag.first.str.idc()
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
        return this.map { it.code() }.joinToString("")
    }

    fun Expr.toerr (): String {
        val src = this.tostr(false).let {
            it.replace('\n',' ').replace('"','\'').let { str ->
                str.take(45).let {
                    if (str.length<=45) it else it+"...)"
                }
            }
        }
        return "\"${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : $src\""
    }

    // POP_IF_0
    fun Expr.PI0 (v: String): String {
        return v + (rets.pub[this]!! == 0).cond { """
            ceux_pop(X->S,1); // PI0 (${this.dump()})
        """ }
    }

    fun Expr.code(): String {
        if (this.isdst()) {
            assert(this is Expr.Acc || this is Expr.Index || this is Expr.Pub)
        }
        return when (this) {
            is Expr.Proto -> {
                val isexe = (this.tk.str != "func")
                val istsk = (this.tk.str == "task")
                val code = this.blk.code()
                val id = this.idc()

                pres.add("""
                    // FUNC | ${this.dump()}
                    int ceu_f_$id (CEUX* X) {
                        ${isexe.cond{"""
                            X->exe->status = CEU_EXE_STATUS_RESUMED;
                            switch (X->exe->pc) {
                                case 0:
                                    if (X->action == CEU_ACTION_ABORT) {
                                        X->exe->status = CEU_EXE_STATUS_TERMINATED;
                                        ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });
                                        return 1;
                                    }
                        """}}
                        ${do_while(code)}
                        ${isexe.cond{"""
                                    X->exe->status = CEU_EXE_STATUS_TERMINATED;
                            }
                        """}}
                        return 1;
                    }
                """)

                this.PI0(""" // CLO | ${this.dump()}
                CEU_Value ceu_clo_$n = ceu_create_clo${isexe.cond{"_exe"}} (
                    ${isexe.cond{"CEU_VALUE_CLO_${this.tk.str.uppercase()},"}}
                    ceu_f_$id,
                    ${this.args.let { assert(it.lastOrNull()?.first?.str!="...") { "TODO: ..." }; it.size }},  // TODO: remove assert
                    ${vars.proto_to_locs[this]!!},
                    ${vars.proto_to_upvs[this]!!.size}
                );
                ceux_push(X->S, 1, ceu_clo_$n);
                ${isexe.cond { """
                    // TODO: use args+locs+upvs+tmps?
                    //ceu_clo_$n.Dyn->Clo_Exe.mem_n = sizeof(CEU_Clo_Mem_$id);                    
                """ }}
                
                // UPVALS = ${vars.proto_to_upvs[this]!!.size}
                ${vars.proto_to_upvs[this]!!.mapIndexed { i,dcl ->
                    """
                    {
                        CEU_Value ceu_up = ceux_peek(X->S, ${vars.idx(dcl,ups.pub[this]!!)});
                        ceu_gc_inc(ceu_up);
                        ceu_clo_$n.Dyn->Clo.upvs.buf[$i] = ceu_up;
                    }
                    """
                }.joinToString("\n")}
                """)
            }
            is Expr.Export -> this.blk.es.code()
            is Expr.Do -> {
                val body = this.es.code()   // before defers[this] check
                val up = ups.pub[this]
                val upvs = ups.first(this) { it is Expr.Proto }.let {
                    if (it == null) 0 else {
                        vars.proto_to_upvs[it]!!.size
                    }
                }
                """
                { // BLOCK | ${this.dump()}
                    // do not clear upvs
                    ceux_block_enter(X->S, X->base+${vars.enc_to_base[this]!!+upvs}, ${vars.enc_to_dcls[this]!!.size});
                    
                    // GLOBALS (must be after ceux_block_enter)
                    ${(ups.pub[this] == outer.main()).cond { """
                    {
                        ${GLOBALS.mapIndexed { i,id -> """
                        {
                            CEU_Value clo = ceu_create_clo(ceu_${id.idc()}_f, 0, 0, 0);
                            ceux_repl(X->S, X->base + $i, clo);
                        }
                        """ }.joinToString("")}
                    }
                    """ }}

                    // defers init
                    ${defers[this].cond { it.second }}
                    
                    ${do_while ( """    
                        $body
                        ${(up is Expr.Loop).cond { """
                            CEU_LOOP_STOP_${up!!.n}:
                        """ }}
                    """)}

                    // defers execute
                    ${(CEU >= 2).cond { defers[this].cond { it.third } }}
                    
                    ceux_block_leave(X->S, X->base+${vars.enc_to_base[this]!!+upvs}, ${vars.enc_to_dcls[this]!!.size}, ${rets.pub[this]!!});
                    
                    // check error
                    ${(CEU >= 2).cond { """
                        CEU_ERROR_CHK(continue, NULL);
                    """ }}
                    // check free
                    ${(CEU >= 3 /*&& inexe*/).cond { """
                        if (X->action == CEU_ACTION_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                }
                """
            }
            is Expr.Dcl -> """
                // DCL | ${this.dump()}
                ${(this.src != null).cond {
                    val idx = vars.idx(this,this)
                    this.PI0("""
                    ${this.src!!.code()}
                    ceux_copy(X->S, $idx, XX(-1));
                    
                    // recursive func requires its self ref upv to be reset to itself
                    ${this.src.let { proto -> (proto is Expr.Proto && proto.rec).cond {
                        val i = vars.proto_to_upvs[proto]!!.indexOf(this)
                        (i != -1).cond { """
                        {
                            CEU_Value clo = ceux_peek(X->S, XX(-1));
                            ceu_gc_inc(clo);    // TODO: creates cycle, never collected
                            clo.Dyn->Clo.upvs.buf[$i] = clo;
                        }
                        """ }
                    }}}
                    """)
                }}
            """
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
                    int ceu_$n = ceu_as_bool(ceux_peek(X->S, XX(-1)));
                    ceux_pop(X->S, 1);
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
                        //ceux_pop(X->S, 1);
                        goto CEU_LOOP_START_${this.n};
                    }
            """
            is Expr.Break -> """ // BREAK | ${this.dump()}
                ${this.cnd.code()}
                int ceu_$n = ceu_as_bool(ceux_peek(X->S, XX(-1)));
                    // pop condition:
                    //  1. when false, clear for next iteration
                    //  2. when true,  but return e is given
                    //  3. when true,  but ret=0
                if (!ceu_$n) {
                    ceux_pop(X->S, 1);            // (1)
                } else {
                    ${this.e.cond2({ """
                        ceux_pop(X->S, 1);        // (2)
                        ${it.code()}
                    """ }, { """
                        ${(rets.pub[this] == 0).cond { """
                            ceux_pop(X->S, 1);    // (3)
                        """ }}
                    """ })}
                    CEU_BREAK = 1;
                    goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
                }
            """
            is Expr.Skip -> """ // SKIP | ${this.dump()}
                ${this.cnd.code()}
                int ceu_$n = ceu_as_bool(ceux_peek(X->S, XX(-1)));
                ceux_pop(X->S, 1);
                if (ceu_$n) {
                    goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
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
                        if (X->action == CEU_ACTION_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                    if (ceux_peek(X->S, XX(-1)).type == CEU_VALUE_ERROR) {      // caught internal throw
                        // [msgs,val,err]
                        do {
                            ${this.cnd.code()}  // ceu_ok = 1|0
                        } while (0);
                        assert(ceux_peek(X->S, XX(-1)).type!=CEU_VALUE_ERROR && "TODO: throw in catch condition");
                        if (!ceu_as_bool(ceux_peek(X->S, XX(-1)))) {  // condition fail: rethrow error, escape catch block
                            ceux_pop(X->S, 1);
                            continue;
                        } else {        // condition true: catch error, continue after catch block
                            // [...,n,pay,err]
                            CEU_Value cnd = ceux_pop(X->S, 1);
                            CEU_Value pay = ceux_peek(X->S, XX(-2));
                            ceu_gc_inc(pay);
                            CEU_Value n   = ceux_peek(X->S, XX(-3));
                            assert(n.type==CEU_VALUE_NUMBER && "bug found");
                            ceux_pop(X->S, n.Number+1+1+1);
                            ceux_push(X->S, 0, pay); // evaluates catch to pay as a whole
                        }
                    }
                }
                """
            }
            is Expr.Defer -> {
                val bup = ups.first_block(this)!!
                val idx = vars.idx(this)
                val (ns,ini,end) = defers.getOrDefault(bup, Triple(mutableListOf(),"",""))
                val inix = """
                    ceux_repl(X->S, $idx, (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} });
                        // false: not reached, dont finalize
                """
                val endx = """
                    if (ceux_peek(X->S,$idx).Bool) {     // if true: reached, finalize
                        do {
                            ${this.blk.code()}
                        } while (0);    // catch throw
                        assert(ceux_peek(X->S,XX(-1)).type!=CEU_VALUE_ERROR && "TODO: error in defer");
                        ceux_pop(X->S, 1);
                    }
                """
                ns.add(n)
                defers[bup] = Triple(ns, ini+inix, endx+end)
                """
                ceux_repl(X->S, $idx, (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} });
                        // true: reached, finalize
                ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });
                """
            }

            is Expr.Resume -> """
                ${this.co.code()}
                ${this.arg.code()}
                ceux_resume(X, 1 /* TODO: MULTI */, ${rets.pub[this]!!});
                CEU_ERROR_CHK(continue, ${this.toerr()});
            """
            is Expr.Yield -> {
                val intsk = ups.inexe(this, "task", true)
                """
                { // YIELD ${this.dump()}
                    ${this.arg.code()}
                    X->exe->status = CEU_EXE_STATUS_YIELDED;
                    X->exe->pc = $n;
                    return 1;   // TODO: args MULTI
                case $n: // YIELD ${this.dump()}
                    if (X->action == CEU_ACTION_ABORT) {
                        //CEU_REPL((CEU_Value) { CEU_VALUE_NIL }); // to be ignored in further move/checks
                        continue;
                    }
                    assert(X->args <= 1 && "TODO: multiple arguments to resume");
                #if CEU >= 4
                    if (X->action == CEU_ACTION_ERROR) {
                        continue;
                    }
                #endif
                #if 0
                    // fill missing args with nils
                    {
                        int N = ${rets.pub[this]!!} - X->args;
                        assert(N > 0);
                        for (int i=0; i<N; i++) {
                            for (int i=0; i<N; i++) {
                                ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });
                            }
                        }
                    }
                #endif
                    ${intsk.cond { """
                        id_evt = ceux_peek(X->S, XX(-1));
                    """ }}
                }
                """
            }

            is Expr.Spawn -> {
                val dots = this.args.lastOrNull()
                assert(dots==null || dots !is Expr.Acc || dots.tk.str!="...") { "TODO" }
                """
                { // SPAWN | ${this.dump()}
                    ${this.tsk.code()}
                    ${this.args.mapIndexed { i, e -> """
                        ${e.code()}
                    """ }.joinToString("")}
                    ceux_spawn(X, CEU_TIME_MAX, ${this.args.size});
                    CEU_ERROR_CHK(continue, ${this.toerr()});
                } // SPAWN | ${this.dump()}
            """
            }
            is Expr.Delay -> "ceu_frame->exe_task->time = CEU_TIME_MAX;"
            is Expr.Pub -> {
                this.tsk.cond2({
                    tsk as Expr
                    it.code() + """ // PUB | ${this.dump()}
                        CEU_Value ceu_tsk_$n = ceux_peek(X->S, XX(-1));
                        if (!ceu_istask_val(ceu_tsk_$n)) {
                            CEU_Value err = { CEU_VALUE_ERROR, {.Error="pub error : expected task"} };
                            CEU_ERROR_THR("${this.tsk.tk.pos.file} : (lin ${this.tsk.tk.pos.lin}, col ${this.tsk.tk.pos.col})", err);
                        }
                    """
                },{ """ // PUB | ${this.dump()}
                    CEU_Value ceu_tsk_$n = ceu_dyn_to_val((CEU_Dyn*)${up_task_real_c()});
                """ }) +
                when {
                    this.isdst() -> {
                        """
                        ceu_gc_dec(ceu_tsk_$n.Dyn->Exe_Task.pub);
                        ceu_tsk_$n.Dyn->Exe_Task.pub = ceux_peek(X->S, XX(${this.tsk.cond2({"-2"},{"-1"})}));;
                        """
                    }
                    else -> "ceux_push(X->S, 1, ceu_tsk_$n.Dyn->Exe_Task.pub);\n"
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
                    CEU_ERROR_THR("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                }
                $tskc.Dyn->Exe_Task.status = (ceu_as_bool(ceu_acc) ? CEU_EXE_STATUS_YIELDED : CEU_EXE_STATUS_TOGGLED);
                CEU_Value ceu_$n = ceu_bcast_task(0, &$tskc.Dyn->Exe_Task, CEU_ACTION_TOGGLE, NULL);
                assert(ceu_$n.type==CEU_VALUE_BOOL && ceu_$n.Bool);
                """
            }

            is Expr.Nat -> {
                val body = vars.nats[this]!!.let { (set, str) ->
                    var x = str
                    for (dcl in set) {
                        val idx = vars.idx(dcl,this)
                        //println(setOf(x, v))
                        x = x.replaceFirst("XXX", "ceux_peek(X->S,$idx)")
                    }
                    x
                }
                this.PI0(when (this.tk_.tag) {
                    null   -> body + "\n" + "ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });\n"
                    ":ceu" -> "ceux_push(X->S, 1, $body);"
                    else -> {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        "ceux_push(X->S, 1, ((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} }));"
                    }
                })
            }
            is Expr.Acc -> {
                val idx = vars.idx(this)
                when {
                    this.isdst() -> """
                        // ACC - SET | ${this.dump()}
                        ceux_copy(X->S, $idx, XX(-1));  // peek keeps src at the top
                    """
                    else -> this.PI0("ceux_push(X->S, 1, ceux_peek(X->S,$idx));\n")
                }
            }
            is Expr.Nil  -> this.PI0("ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });")
            is Expr.Tag  -> this.PI0("ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_${this.tk.str.tag2c()}} });")
            is Expr.Bool -> this.PI0("ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} });")
            is Expr.Char -> this.PI0("ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_CHAR, {.Char=${this.tk.str}} });")
            is Expr.Num  -> this.PI0("ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} });")

            is Expr.Tuple -> this.PI0("""
                { // TUPLE | ${this.dump()}
                    ceux_push(X->S, 1, ceu_create_tuple(${this.args.size}));
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        ceu_tuple_set(&ceux_peek(X->S,XX(-2)).Dyn->Tuple, $i, ceux_peek(X->S,XX(-1)));
                        ceux_pop(X->S, 1);
                        """
                    }.joinToString("")}
                }
            """)
            is Expr.Vector -> this.PI0("""
                { // VECTOR | ${this.dump()}
                    ceux_push(X->S, 1, ceu_create_vector());
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        ceu_vector_set(&ceux_peek(X->S,XX(-2)).Dyn->Vector, $i, ceux_peek(X->S,XX(-1)));
                        ceux_pop(X->S, 1);
                        """
                    }.joinToString("")}
                }
            """)
            is Expr.Dict -> this.PI0("""
                { // DICT | ${this.dump()}
                    ceux_push(X->S, 1, ceu_create_dict());
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            ${it.second.code()}
                            CEU_ERROR_ASR(
                                continue,
                                ceu_dict_set(&ceux_peek(X->S,XX(-3)).Dyn->Dict, ceux_peek(X->S,XX(-2)), ceux_peek(X->S,XX(-1))),
                                ${this.toerr()}
                            );
                            ceux_drop(X->S, 2);
                        }
                    """ }.joinToString("")}
                }
            """)
            is Expr.Index -> {
                val idx = vars.data(this).let { if (it == null) -1 else it.first!! }
                """
                { // INDEX | ${this.dump()}
                    // IDX
                    ${if (idx == -1) {
                        this.idx.code()
                    } else {
                        """
                        ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NUMBER, {.Number=$idx} });
                        """
                    }}
                    
                    // COL
                    ${this.col.code()}
                    CEU_ERROR_ASR (
                        continue,
                        ceu_col_check(ceux_peek(X->S,XX(-1)),ceux_peek(X->S,XX(-2))),
                        ${this.toerr()}
                    );
                """ + when {
                    this.isdst() -> {
                        """
                        CEU_Value ceu_$n = ceu_col_set(ceux_peek(X->S,XX(-1)), ceux_peek(X->S,XX(-2)), ceux_peek(X->S,XX(-3)));
                        CEU_ERROR_ASR(continue, ceu_$n, ${this.toerr()});
                        ceux_drop(X->S, 2);    // keep src
                        """
                    }
                    else -> this.PI0("""
                        CEU_Value ceu_$n = CEU_ERROR_ASR(continue, ceu_col_get(ceux_peek(X->S,XX(-1)),ceux_peek(X->S,XX(-2))), ${this.toerr()});
                        ceu_gc_inc(ceu_$n);
                        ceux_drop(X->S, 2);
                        ceux_push(X->S, 1, ceu_$n);
                        ceu_gc_dec(ceu_$n);
                    """)
                } + """
                }
                """
            }
            is Expr.Call -> {
                val dots = this.args.lastOrNull()
                val has_dots = (dots!=null && dots is Expr.Acc && dots.tk.str=="...") && !this.clo.let { it is Expr.Acc && it.tk.str=="{{#}}" }
                val id_dots = if (!has_dots) "" else {
                    TODO()
                }
                """
                { // CALL | ${this.dump()}
                    ${this.clo.code()}
                    ${this.args.filter{!(has_dots && it.tk.str=="...")}.mapIndexed { i,e -> """
                        ${e.code()}
                    """ }.joinToString("")}                    
                    ${has_dots.cond { """
                        -=- TODO -=-
                        for (int ceu_i_$n=0; ceu_i_$n<$id_dots.Dyn->Tuple.its; ceu_i_$n++) {
                            ceux_push(X->S, 1, $id_dots.Dyn->Tuple.buf[ceu_i_$n]);
                        }
                    """ }}

                    ceux_call(X, ${this.args.size}, ${rets.pub[this]!!});
                    
                    CEU_ERROR_CHK(continue, ${this.toerr()});
                } // CALL | ${this.dump()}
                """
            }
        }
    }
}
