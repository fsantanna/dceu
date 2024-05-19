package dceu

import kotlin.math.max

fun do_while (code: String): String {
    return (CEU >= 2).cond { "do {" } + code + (CEU >= 2).cond { "} while (0);" }

}

class Coder (val outer: Expr.Do, val ups: Ups, val vars: Vars, val sta: Static) {
    val pres: MutableList<String> = mutableListOf()
    val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>> = mutableMapOf()
    val code: String = outer.code()
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
            CEU_ERROR_CHK_STK(continue, $msg);
            ${(CEU>=3 && exe!=null && defer==null).cond { """
                if (X->exe->status == CEU_EXE_STATUS_TERMINATED) {
                    continue;
                }
            """ }}
        """
    }

    fun Expr.code(): String {
        if (this.isdst()) {
            assert(this is Expr.Acc || this is Expr.Index || this is Expr.Pub)
        }
        return when (this) {
            is Expr.Proto -> {
                val isexe = (this.tk.str != "func")
                val istsk = (this.tk.str == "task")
                val isnst = ups.isnst(this)
                val code = this.blk.code()
                val id = this.idc()

                pres.add("""
                    // PROTO | ${this.dump()}
                    void ceu_pro_$id (CEU_Clo* ceu_clo, int ceu_n, CEU_Value ceu_args[]) {
                        ${this.pars.map { """
                            CEU_Value ceu_par_${it.first.str.idc()};
                        """ }.joinToString("")}
                        ${vars.proto_to_upvs[this]!!.mapIndexed { i, dcl -> """
                            CEU_Value ceu_upv_${dcl.idtag.first.str.idc()} = ceu_clo->upvs.buf[$i];                            
                        """ }.joinToString("")}
                        ${isexe.cond{"""
                            X->exe->status = (X->action == CEU_ACTION_ABORT) ? CEU_EXE_STATUS_TERMINATED : CEU_EXE_STATUS_RESUMED;
                            switch (X->exe->pc) {
                                case 0:
                                    if (X->action == CEU_ACTION_ABORT) {
                                        ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });
                                        return 1;
                                    }
                        """}}
                        ${do_while(code)}
                        ${isexe.cond{"""
                                return ceu_exe_term(X);
                            } // close switch
                        """}}
                    }
                """)

                //assert(!this.isva) { "TODO" }
                """ // CREATE | ${this.dump()}
                {
                    ${isnst.cond { "assert(X->exe!=NULL && X->exe->type==CEU_VALUE_EXE_TASK);" }}
                    CEU_ACC (
                        ceu_create_clo${istsk.cond { "_task" }} (
                            ${(!istsk).cond { "CEU_VALUE_CLO_${this.tk.str.uppercase()}," }}
                            ceu_pro_$id,
                            ${this.pars.size},
                            ${vars.proto_to_upvs[this]!!.size}
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
                            CEU_Value upv = ${vars.idx("X",dcl,ups.pub[this]!!)};
                            //ceu_gc_inc(upv);
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
                val upvs = ups.first(this) { it is Expr.Proto }.let {
                    if (it == null) 0 else {
                        vars.proto_to_upvs[it]!!.size
                    }
                }
                /*
                {(this == outer.args).cond { """
                    // ... args ...
                    {
                        for (int i=0; i<ceu_argc; i++) {
                            CEU_Value vec = ceu_to_dash_string_dash_pointer(ceu_argv[i]);
                            ceux_push(X->S, 1, vec);
                        }
                    }
                    ceu_${this.n} += ceu_argc;
                """ }}
                 */

                // func (a,b)
                val proto = ups.pub[this]
                val args = if (proto !is Expr.Proto) null else {
                    proto.pars.mapIndexed { i,(id, _) -> """
                        ceu_par_${id.str.idc()} = ($i < ceu_n) ? ceu_args[$i] : (CEU_Value) { CEU_VALUE_NIL };
                    """
                    }.joinToString("")
                }

                val void = sta.void(this)
                if (void) {
                    assert(args == null)
                    """
                    { // BLOCK | void | ${this.dump()}
                        $body
                    }
                    """
                } else {
                    """
                    ${args.cond { """
                        // PROTO | ARGS | ${proto!!.dump()}
                        $it
                    """ }}
                    { // BLOCK | ${this.dump()}
                        // do not clear upvs
                        //ceux_block_enter(X->S, X->clo+1+X->args+{upvs+vars.enc_to_base[this]!!}, {vars.size(vars.enc_to_dcls[this]!!)} CEU4(COMMA X->exe));
                        
                        // defers init
                        ${defers[this].cond { it.second }}
                        
                        ${do_while ( """    
                            $body
                            ${(up is Expr.Loop).cond { """
                                CEU_LOOP_STOP_${up!!.n}:
                            """ }}
                        """)}
    
                        // BLOCK (escape) | ${this.dump()}
                        // defers execute
                        ${(CEU >= 2).cond { defers[this].cond { it.third } }}
                        
                        // out=0 when loop iterates (!CEU_BREAK)
                        //ceux_block_leave(X->S, X->clo+1+X->args+{upvs+vars.enc_to_base[this]!!}, {vars.size(vars.enc_to_dcls[this]!!)} CEU4(COMMA X->exe), ${(up is Expr.Loop).cond { "(!CEU_BREAK) ? 0 : " }} {rets.exts[this]!!});
                        
                        ${(CEU >= 2).cond { this.check_error_aborted("NULL")} }
                    }
                    """
                }
            }
            is Expr.Dcl -> {
                val idx = vars.idx("X", this, this)
                val isglb = (vars.dcl_to_blk[this] == outer)
                val issrc = (this.src != null)
                """
                // DCL | ${this.dump()}
                ${when {
                    sta.protos_use_unused.contains(this.src) -> """
                        // $idx: unused function
                    """
                    (isglb && !issrc) -> """
                        // $idx: uninited global
                    """
                    (isglb && issrc) -> """
                        ${this.src!!.code()}
                        $idx = CEU_ACC_INC();
                    """
                    (!isglb && !issrc) -> """
                        CEU_Value $idx = { CEU_VALUE_NIL };
                    """
                    (!isglb && issrc) -> """
                        ${this.src!!.code()}
                        CEU_Value $idx = CEU_ACC_INC();
                    """
                    else -> error("impossible case")
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
                        //ceux_pop(X->S, 1);
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
                        //ceux_pop(X->S, 1);
                        goto CEU_LOOP_START_${this.n};
                    }
            """
            is Expr.Break -> """ // BREAK | ${this.dump()}
                ${this.cnd.code()}
                {
                    int v = ceu_as_bool(ceux_peek(X->S, XX(-1)));
                        // pop condition:
                        //  1. when false, clear for next iteration
                        //  2. when true,  but return e is given
                        //  3. when true,  but ret=0
                    if (!v) {
                        ceux_pop(X->S, 1);            // (1)
                    } else {
                        ${this.e.cond { """
                            ceux_pop(X->S, 1);        // (2)
                            ${it.code()}
                        """ }}
                        CEU_BREAK = 1;
                        goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
                    }
                }
            """
            is Expr.Skip -> """ // SKIP | ${this.dump()}
                ${this.cnd.code()}
                {
                    int v = ceu_as_bool(ceux_peek(X->S, XX(-1)));
                    ceux_pop(X->S, 1);
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
                        if (X->action == CEU_ACTION_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                    if (CEU_ERROR_IS(X->S)) {       // caught internal throw
                        // [msgs,val,err]
                        ${this.cnd.code()}          // ceu_ok = 1|0
                        assert(!CEU_ERROR_IS(X->S) && "TODO: throw in catch condition");
                        if (!ceu_as_bool(ceux_pop(X->S, XX(-1)))) {  // condition fail: rethrow error, escape catch block
                            continue;
                        } else {        // condition true: catch error, continue after catch block
                            // [...,n,pay,err,cnd]
                            CEU_Value pay = ceux_peek(X->S, XX(-2));
                            ceu_gc_inc_val(pay);
                            CEU_Value n   = ceux_peek(X->S, XX(-3));
                            assert(n.type==CEU_VALUE_NUMBER && "bug found");
                            ceux_drop(X->S, n.Number+1+1+1);
                            ceux_push(X->S, 0, pay); // evaluates catch to pay as a whole
                        }
                    }
                }
                """
            }
            is Expr.Defer -> {
                val bup = ups.first(this) { it is Expr.Do } as Expr.Do
                val (ns,ini,end) = defers.getOrDefault(bup, Triple(mutableListOf(),"",""))
                val inix = """
                    ceux_repl(X->S, $ idx, (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} });
                        // false: not reached, dont finalize
                """
                val endx = """
                    if (ceux_peek(X->S,$ idx).Bool) {     // if true: reached, finalize
                        ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });  // detect error inside
                        do {
                            ${this.blk.code()}
                        } while (0);    // catch throw
                        assert(!CEU_ERROR_IS(X->S) && "TODO: error in defer");
                        ceux_pop(X->S, 1);
                    }
                """
                ns.add(n)
                defers[bup] = Triple(ns, ini+inix, endx+end)
                """
                ceux_repl(X->S, $ idx, (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} });
                        // true: reached, finalize
                """
            }

            is Expr.Resume -> {
                //assert(!this.args.dots && this.args.es.size<=1)
                """
                ${this.co.code()}
                ${this.args.code()}
                ceux_resume(X, ceu_{this.args.n}, {rets.exts[this]!!}, CEU_ACTION_RESUME CEU4(COMMA X->now));
                ${this.check_error_aborted(this.toerr())}
            """
            }

            is Expr.Yield -> {
                """
                { // YIELD ${this.dump()}
                    {this.args.code()}
                    X->exe->status = CEU_EXE_STATUS_YIELDED;
                    X->exe->pc = $n;
                    return ceu_{this.args.n};
                case $n: // YIELD ${this.dump()}
                    if (X->action == CEU_ACTION_ABORT) {
                        //ceux_push(X->S, 1, (CEU_Value){CEU_VALUE_NIL}); // fake out=1
                        continue;
                    }
                #if CEU >= 4
                    if (X->action == CEU_ACTION_ERROR) {
                        //assert(X->args>1 && CEU_ERROR_IS(X->S) && "TODO: varargs resume");
                        continue;
                    }
                #endif
                    {rets.exts[this]!!.let { v -> (v != MULTI).cond { ""
                        ceux_yield_args(X, $ v);
                    "" }}}
                }
            """
            }

            is Expr.Spawn -> {
                """
                { // SPAWN | ${this.dump()}
                    ${(CEU >= 5).cond { """
                        ${this.tsks.cond2({
                            it.code()
                        },{
                            "ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });"
                        })}
                    """ }}
                    ${this.tsk.code()}
                    ${this.args.code()}
                    ceux_spawn(X, ceu_{this.args.n}, X->now);
                    ${this.check_error_aborted(this.toerr())}
                } // SPAWN | ${this.dump()}
                """
            }
            is Expr.Delay -> """
                // DELAY | ${this.dump()}
                X->exe_task->time = CEU_TIME;
            """
            is Expr.Pub -> {
                val exe = if (this.tsk != null) "" else {
                    ups.first_task_outer(this).let { outer ->
                        val xups = ups.all_until(this) { it == outer } // all ups between this -> outer
                        val n = xups.count { it is Expr.Proto }
                        "X${"->exe_task->clo.Dyn->Clo_Task.up_tsk->X".repeat(n-1)}->exe_task"
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
                ${this.isdst().cond2({ """
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
            is Expr.Toggle -> """{  // TOGGLE | ${this.dump()}
                ${this.tsk.code()}
                ${this.on.code()}
                {   // TOGGLE | ${this.dump()}
                    CEU_Value tsk = ceux_peek(X->S, XX(-2));
                    int on = ceu_as_bool(ceux_peek(X->S, XX(-1)));
                    if (!ceu_istask_val(tsk)) {
                        CEU_ERROR_THR_S(continue, "toggle error : expected yielded task", ${this.toerr()});
                    }
                    if (on && tsk.Dyn->Exe_Task.status!=CEU_EXE_STATUS_TOGGLED) {                
                        CEU_ERROR_THR_S(continue, "toggle error : expected toggled task", ${this.toerr()});
                    }
                    if (!on && tsk.Dyn->Exe_Task.status!=CEU_EXE_STATUS_YIELDED) {                
                        CEU_ERROR_THR_S(continue, "toggle error : expected yielded task", ${this.toerr()});
                    }
                    tsk.Dyn->Exe_Task.status = (on ? CEU_EXE_STATUS_YIELDED : CEU_EXE_STATUS_TOGGLED);
                }
            }"""

            is Expr.Nat -> {
                val body = vars.nats[this]!!.let { (set, str) ->
                    var x = str
                    for (dcl in set) {
                        val idx = vars.idx("X", dcl, this)
                        //println(setOf(x, v))
                        x = x.replaceFirst("XXX", idx)
                    }
                    x
                }
                when (this.tk_.tag) {
                    null   -> body + "\n" + """
                        ${this.check_error_aborted(this.toerr())}
                        CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));
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
                val idx = vars.idx("X",this)
                when {
                    this.isdst() -> """
                        // ACC - SET | ${this.dump()}
                        $idx = CEU_ACC_KEEP();
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

            is Expr.Tuple -> """
                { // TUPLE | ${this.dump()}
                    CEU_Value ceu_args_$n[${max(1,this.args.size)}];
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                            ceu_args_$n[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}
                    CEU_ACC (
                        ceu_create_tuple(1, ${this.args.size}, ceu_args_$n);
                    );
                }
            """
            is Expr.Vector -> """
                { // VECTOR | ${this.dump()}
                    CEU_Value ceu_vec_$n = ceu_create_vector();
                    ${this.args.mapIndexed { i, it ->
                         it.code() + """
                         ceu_vector_set(&ceu_vec_$n.Dyn->Vector, $i, CEU_ACC_KEEP());
                        """
                    }.joinToString("")}
                    CEU_ACC(ceu_vec_$n);
                }
            """
            is Expr.Dict -> """
                { // DICT | ${this.dump()}
                    CEU_Value ceu_dic_$n = ceu_create_dict();
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            CEU_Value ceu_key_$n = CEU_ACC_KEEP();
                            ${it.second.code()}
                            CEU_Value ceu_val_$n = CEU_ACC_KEEP();
                            CEU_ERROR_CHK_PTR (
                                continue,
                                ceu_dict_set(&ceu_dic_$n.Dyn->Dict, ceu_key_$n, ceu_val_$n),
                                ${this.toerr()}
                            );
                            //ceu_gc_dec_val(ceu_key_$n);
                            //ceu_gc_dec_val(ceu_col_$n);
                            //CEU_ERROR_CHK_STK(continue, );
                        }
                    """ }.joinToString("")}
                    CEU_ACC(ceu_dic_$n);
                }
            """
            is Expr.Index -> {
                val idx = vars.data(this).let { if (it == null) -1 else it.first!! }
                """
                { // INDEX | ${this.dump()}
                    // VAL
                    ${this.isdst().cond { """
                        CEU_Value ceu_val_$n = CEU_ACC_KEEP();
                    """ }}
                    
                    // COL
                    ${this.col.code()}
                    CEU_Value ceu_col_$n = CEU_ACC_KEEP();

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
                if (this.isdst()) {
                    """
                    CEU_ERROR_CHK_PTR (
                        continue,
                        ceu_col_set(ceu_col_$n, ceu_idx_$n, ceu_val_$n),
                        ${this.toerr()}
                    );
                    ceu_acc = ceu_val_$n;
                    """
                } else {
                    """
                    CEU_ACC(ceu_col_get(ceu_col_$n, ceu_idx_$n));
                    CEU_ERROR_CHK_ACC(continue, ${this.toerr()});
                    """
                } + """
                    //ceu_gc_dec_val(ceu_idx_$n);
                    //ceu_gc_dec_val(ceu_col_$n);
                }
                """
            }
            is Expr.Call -> {
                """
                { // CALL | ${this.dump()}
                    ${this.clo.code()}
                    CEU_Value ceu_clo_$n = CEU_ACC_KEEP();
                    CEU_Value ceu_args_$n[${this.args.size}];
                    ${this.args.mapIndexed { i,e ->
                        e.code() + """
                            ceu_args_$n[$i] = CEU_ACC_KEEP();
                        """
                    }.joinToString("")}
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                    ceu_clo_$n.Dyn->Clo.proto((CEU_Clo*)ceu_clo_$n.Dyn, ${this.args.size}, ceu_args_$n);
                    //{this.check_error_aborted(this.toerr())}
                } // CALL | ${this.dump()}
                """
            }
        }
    }
}
