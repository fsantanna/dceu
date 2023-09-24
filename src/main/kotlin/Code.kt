package dceu

import java.lang.Integer.min

class Coder (val outer: Expr.Do, val ups: Ups, val vars: Vars, val clos: Clos, val sta: Static) {
    val pres: MutableList<String> = mutableListOf()
    val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>> = mutableMapOf()
    val code: String = outer.code()

    fun Expr.idc (pre: String): String {
        return if (sta.ylds.contains(ups.first_block(this)!!)) {
            "(ceu_mem->${pre}_${this.n})"
        } else {
            "ceu_${pre}_${this.n}"
        }
    }
    fun Expr.Do.idc (pre: String): String {
        return if (sta.ylds.contains(this)) {
            "(ceu_mem->ceu_${pre}_${this.n})"
        } else {
            "ceu_${pre}_${this.n}"
        }
    }
    fun Expr.Dcl.idc (upv: Int): Pair<String,String> {
        return when {
            (upv == 2) -> {
                val idc = this.id.str.idc()
                Pair("(ceu_upvs->$idc)", "(ceu_upvs->_${idc}_)")
            }
            sta.ylds.contains(vars.dcl_to_blk[this]) -> {
                val idc = this.id.str.idc(this.n)
                Pair("(ceu_mem->$idc)", "(ceu_mem->_${idc}_)")
            }
            else -> {
                val idc = this.id.str.idc()
                Pair(idc, "_${idc}_")
            }
        }
    }

    fun Expr.isdst (): Boolean {
        return ups.pub[this].let { it is Expr.Set && it.dst==this }
    }
    fun Expr.isdrop (): Boolean {
        return ups.pub[this].let { it is Expr.Drop && it.e==this }
    }
    fun Expr.asdst_src (): String {
        return "(ceu_set_${(ups.pub[this] as Expr.Set).n})"
    }

    fun Expr.code(): String {
        if (this.isdst()) {
            assert(this is Expr.Acc || this is Expr.Index)
        }
        return when (this) {
            is Expr.Proto -> {
                val blk = ups.first_block(this)!!
                val isexe = (this.tk.str != "func")
                val code = this.body.code()

                val pre = """ // UPVS | ${this.dump()}
                    ${clos.protos_refs[this].cond { """
                        typedef struct {
                            ${clos.protos_refs[this]!!.map {
                                "CEU_Value ${it.id.str.idc()};"
                            }.joinToString("")}
                        } CEU_Clo_Upvs_$n;                    
                    """ }}
                """ +
                """ // MEM | ${this.dump()}
                    ${isexe.cond { """
                        typedef struct {
                            ${this.args.map { (arg,_) ->
                                val dcl = vars.get(this.body, arg.str)
                                val idc = dcl.id.str.idc(dcl.n)
                                """
                                CEU_Value $idc;
                                CEU_Block* _${idc}_;
                                """
                            }.joinToString("")}
                            ${this.body.mem(sta, defers)}
                        } CEU_Clo_Mem_$n;                        
                    """ }}
                """ + """ // FUNC | ${this.dump()}
                    CEU_Value ceu_clo_$n (
                        CEU_Frame* ceu_frame,
                        int ceu_n,
                        CEU_Value ceu_args[]
                    ) {
                        CEU_Value ceu_acc;
                        ${clos.protos_refs[this].cond { """
                            CEU_Clo_Upvs_$n* ceu_upvs = (CEU_Clo_Upvs_$n*) ceu_frame->clo->upvs.buf;                    
                        """ }}
                        ${isexe.cond { """
                            CEU_Clo_Mem_$n* ceu_mem = (CEU_Clo_Mem_$n*) ceu_frame->exe->mem;                    
                        """ }}
                        ${isexe.cond{"""
                            ceu_frame->exe->status = CEU_EXE_STATUS_RESUMED;
                            switch (ceu_frame->exe->pc) {
                                case 0:
                        """}}
                        $code
                        // terminated
                        ${isexe.cond{"""
                            ceu_frame->exe->status = CEU_EXE_STATUS_TERMINATED;
                            if (!CEU_ISERR(ceu_acc)) {
                                ${(this.tk.str == "task").cond { """
                                    ceu_acc = ceu_bcast_blocks(CEU_HLD_BLOCK((CEU_Dyn*)ceu_frame->exe_task), (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=(CEU_Dyn*)ceu_frame->exe_task} });                     
                                """ }}
                            }
                        }
                        #if CEU >= 5
                        if (ceu_frame->exe->type == CEU_VALUE_EXE_TASK_IN) {
                            ceu_hold_rem((CEU_Dyn*)ceu_frame->exe CEU5(COMMA &((CEU_Tasks*)(ceu_frame->exe->hld.block))->dyns));
                            ceu_dyn_free((CEU_Dyn*)ceu_frame->exe);
                        }
                        #endif
                        """}}
                        return ceu_acc;
                    }
                """

                val pos = """ // CLO | ${this.dump()}
                ceu_acc = ceu_create_clo${isexe.cond{"_exe"}} (
                    CEU_VALUE_CLO_${this.tk.str.uppercase()},
                    ${blk.idc("block")},
                    ${if (clos.protos_noclos.contains(this)) "CEU_HOLD_IMMUT" else "CEU_HOLD_FLEET"},
                    ${if (blk == outer) "NULL" else "ceu_frame"},
                    ceu_clo_$n,
                    ${clos.protos_refs[this]?.size ?: 0}
                );
                ${isexe.cond { """
                    ceu_acc.Dyn->Clo_Exe.mem_n = sizeof(CEU_Clo_Mem_$n);                    
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
                            CEU_Value ceu_up = ${dcl.idc(upv).first};
                            assert(ceu_hold_chk_set_col(ceu_acc.Dyn, ceu_up CEU4(COMMA ${if (sta.ylds.contains(blk)) 1 else 0})).type != CEU_VALUE_ERROR);
                            ceu_gc_inc(ceu_up);
                            ((CEU_Clo_Upvs_$n*)ceu_acc.Dyn->Clo.upvs.buf)->${idc} = ceu_up;
                        }
                        """   // TODO: use this.body (ups.ups[this]?) to not confuse with args
                    }.joinToString("\n")
                }}
                """

                if (!clos.protos_noclos.contains(this)) {
                    pres.add(pre)
                    pos
                } else {
                    pre + pos
                }
            }
            is Expr.Do -> {
                val isvoid = sta.void(this)
                val body = this.es.map { it.code() }.joinToString("")   // before defers[this] check
                val blkc = this.idc("block")
                val up = ups.pub[this]
                val ylds = sta.ylds.contains(this)
                val yldsi = if (ylds) 1 else 0
                val f_b = up?.let { ups.first_proto_or_block(it) }
                val bupc = when {
                    (up == null)        -> "(&_ceu_block_)"
                    (f_b is Expr.Proto) -> "NULL"
                    else                -> ups.first_block(up)!!.idc("block")
                }
                val (depth,bf,ptr) = when {
                    (f_b == null) -> Triple("1", "1", "{.frame=&_ceu_frame_}")
                    (f_b is Expr.Proto) -> Triple("ceu_frame->up_block->depth + 1", "1", "{.frame=ceu_frame}")
                    else -> Triple("($bupc->depth + 1)", "0", "{.block=$bupc}")
                }
                val args = if (f_b !is Expr.Proto) emptySet() else f_b.args.map { it.first.str }.toSet()
                val dcls = vars.blk_to_dcls[this]!!.filter { it.init }
                    .filter { !GLOBALS.contains(it.id.str) }
                    .filter { !(f_b is Expr.Proto && args.contains(it.id.str)) }
                    .map    { it.idc(0) }

                val loop_body = if (up !is Expr.XLoop) body else """
                    // LOOP | ${up.dump()}
                    CEU_LOOP_START_${up.n}:
                        $body
                        goto CEU_LOOP_START_${up.n};
                    CEU_LOOP_STOP_${up.n}:
                """

                """
                { // BLOCK | ${this.dump()}
                    // CEU_Block ceu_block;
                    ${(!ylds).cond { """
                        ${(!isvoid).cond { """
                            CEU_Block _ceu_block_$n;
                        """ }}
                        CEU_Block* ceu_block_$n;
                    """}}
                    // ceu_block = ...;
                    ${when {
                        isvoid -> """
                            $blkc = ${bupc};
                        """
                        ylds -> """
                            ceu_mem->_ceu_block_$n = (CEU_Block) { $depth, $bf, $ptr, { CEU4(NULL COMMA) {NULL,NULL} } };
                            $blkc = &ceu_mem->_ceu_block_$n;                                 
                        """
                        else -> """
                            _ceu_block_$n = (CEU_Block) { $depth, $bf, $ptr, { CEU4(NULL COMMA) {NULL,NULL} } };
                            $blkc = &_ceu_block_$n;                                 
                        """
                    }}
                    // link task.dn_block = me
                    // link up.dn.block = me
                    ${when {
                        (CEU < 4) -> ""
                        (f_b is Expr.Proto && f_b.tk.str == "task") -> """
                            ceu_frame->exe_task->dn_block = $blkc;                        
                        """
                        (f_b !is Expr.Proto && !isvoid) -> """
                            $bupc->dn.block = $blkc;
                        """
                        else -> ""
                    }}
                    // main args, func args
                    ${when {
                        (f_b == null) -> """
                            // main block varargs (...)
                            CEU_Value id__dot__dot__dot_;
                        """
                        (f_b is Expr.Proto) -> {
                            (!ylds).cond {
                                f_b.args.map { (id,_) ->
                                    val idc = id.str.idc()
                                    """
                                    CEU_Value $idc;
                                    CEU_Block* _${idc}_;
                                    """
                            }.joinToString("")}
                        }
                        else -> ""
                    }}
                    // inline vars dcls
                    ${(!ylds).cond { """
                        ${dcls.map { """
                            CEU_Value ${it.first};
                            CEU_Block* ${it.second};
                        """ }.joinToString("")}
                    """ }}
                    // vars inits
                    ${dcls.map { """
                        ${it.first} = (CEU_Value) { CEU_VALUE_NIL };
                        ${it.second} = $blkc;
                    """ }.joinToString("")}
                    // defers init
                    ${defers[this].cond { it.second }}
                    // pres funcs
                    ${(f_b == null).cond{ pres.joinToString("") }}
                    
                    ${(CEU >= 2).cond { "do {" }}
                        // main args, func args
                        ${when {
                            (f_b == null) -> """
                                // main block varargs (...)
                                id__dot__dot__dot_ = ceu_create_tuple($blkc, ceu_argc);
                                for (int i=0; i<ceu_argc; i++) {
                                    CEU_Value vec = ceu_vector_from_c_string($blkc, ceu_argv[i]);
                                    assert(ceu_tuple_set(&id__dot__dot__dot_.Dyn->Tuple, i, vec CEU4(COMMA $yldsi)).type != CEU_VALUE_ERROR);
                                }
                            """
                            (f_b is Expr.Proto) -> {
                                val dots = (f_b.args.lastOrNull()?.first?.str == "...")
                                val args_n = f_b.args.size - 1
                                """
                                int ceu_gc_todo = 1;
                                { // func args
                                    ${f_b.args.filter { it.first.str!="..." }.mapIndexed { i,arg ->
                                        val (idc,_idc_) = vars.get(this, arg.first.str).idc(0)
                                        """
                                        $_idc_ = $blkc;
                                        if ($i < ceu_n) {
                                            $idc = ceu_args[$i];
                                            ceu_gc_todo = 0;
                                            CEU_ASSERT(
                                                $blkc,
                                                ceu_hold_chk_set($blkc, CEU_HOLD_FLEET, $idc, 0, "argument error" CEU4(COMMA $yldsi)),
                                                "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                                            );
                                            ceu_gc_todo = 1;
                                            ceu_gc_inc($idc);
                                            ${(f_b.tk.str != "func").cond {"""
                                                if ($idc.type>CEU_VALUE_DYNAMIC && $idc.Dyn->Any.hld.type!=CEU_HOLD_FLEET && CEU_HLD_BLOCK($idc.Dyn)->depth>1) {
                                                    CEU_Value err = { CEU_VALUE_ERROR, {.Error="resume error : incompatible scopes"} };
                                                    CEU_ERROR($blkc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                                                }
                                            """ }}
                                        } else {
                                            $idc = (CEU_Value) { CEU_VALUE_NIL };
                                        }
                                        """
                                    }.joinToString("")}
                                    ${dots.cond {
                                        val idc = f_b.args.last()!!.first.str.idc()
                                        """
                                        int ceu_tup_n_$n = MAX(0,ceu_n-$args_n);
                                        $idc = ceu_create_tuple($blkc, ceu_tup_n_$n);
                                        for (int i=0; i<ceu_tup_n_$n; i++) {
                                            assert(ceu_tuple_set(&$idc.Dyn->Tuple, i, ceu_args[$args_n+i] CEU4(COMMA 1)).type != CEU_VALUE_ERROR);
                                        }
                                        ceu_gc_inc($idc);
                                    """ }}
                                }
                                """
                        }
                            else -> ""
                        }}
                        $loop_body
                    ${(CEU >= 2).cond { "} while (0);" }}
                    
                    // defers execute
                    ${defers[this].cond { it.third }}
                    // move up dynamic ceu_acc (return or error)
                    ${(f_b!=null && !isvoid).cond {
                        val up1 = if (f_b is Expr.Proto) "ceu_frame->up_block" else bupc
                        """
                        CEU_Value ceu_err_$n = ceu_hold_chk_set($up1, CEU_HOLD_FLEET, ceu_acc, 0, "block escape error" CEU4(COMMA $yldsi));
                        if (ceu_err_$n.type == CEU_VALUE_ERROR) {
                        #if CEU <= 1
                            // free from this block
                            CEU_ERROR($blkc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", ceu_err_$n);
                        #else
                            do {
                                // allocate throw on up
                                CEU_ERROR($up1, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", ceu_err_$n);
                            } while (0);    // catch continue in CEU_ERROR
                        #endif
                        }
                        """
                    }}
                    // dcls gc-dec
                    ${dcls.map { """
                        if (${it.first}.type > CEU_VALUE_DYNAMIC) { // required b/c check below
                            ceu_gc_dec(${it.first}, (CEU_HLD_BLOCK(${it.first}.Dyn)->depth == $blkc->depth));
                        }
                    """ }.joinToString("")}
                    // args gc-dec
                    ${(f_b is Expr.Proto).cond { """
                        ${(f_b as Expr.Proto).args.map {
                            val (idc,_) = vars.get(this, it.first.str).idc(0)
                            """
                            if (ceu_gc_todo) {
                                if ($idc.type > CEU_VALUE_DYNAMIC) { // required b/c check below
                                    ceu_gc_dec($idc, !(ceu_acc.type>CEU_VALUE_DYNAMIC && ceu_acc.Dyn==$idc.Dyn));
                                }
                            }
                            """
                        }.joinToString("")}
                    """}}
                    // unlink task.dn_block = me
                    // unlink up.dn.block = me
                    ${when {
                        (CEU < 4) -> ""
                        (f_b is Expr.Proto && f_b.tk.str == "task") -> """
                            ceu_frame->exe_task->dn_block = NULL;                        
                        """
                        (f_b !is Expr.Proto && !isvoid) -> """
                            $bupc->dn.block = NULL;
                        """
                        else -> ""
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
                    // block free
                    ${(!isvoid).cond { "ceu_dyns_free(&$blkc->dn.dyns);" }}
                    // check error
                    ${(CEU>=2 && (f_b is Expr.Do)).cond { """
                        if (CEU_ISERR(ceu_acc)) {
                            continue;
                        }                        
                    """ }}
                    // check free
                    ${(CEU>=3 && (f_b is Expr.Do) && ups.any(this) { it is Expr.Proto && it.tk.str!="func" }).cond { """
                        if (ceu_n == CEU_ARG_FREE) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                }
                """
            }
            is Expr.Dcl -> {
                val (idc,_) = this.idc(0)
                val blk = ups.first_block(this)!!
                val bupc = blk.idc("block")
                val unused = false // TODO //sta.unused.contains(this) && (this.src is Expr.Closure)

                if (this.id.upv==1 && clos.vars_refs.none { it.second==this }) {
                    err(this.tk, "var error : unreferenced upvar")
                }

                """
                // DCL | ${this.dump()}
                ${(this.init && this.src!=null && !unused).cond {
                    this.src!!.code() + (!this.tmp).cond { """
                        CEU_ASSERT(
                            $bupc,
                            ceu_hold_chk_set($bupc, CEU_HOLD_MUTAB, ceu_acc, 0, "declaration error" CEU4(COMMA ${if (sta.ylds.contains(blk)) 1 else 0})),
                            "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                        );
                    """ }
                }}
                ${when {
                    !this.init -> ""
                    (this.src == null) -> ""
                    else -> "$idc = ceu_acc;"
                }}
                ceu_gc_inc($idc);
                ceu_acc = $idc;
                """
            }
            is Expr.Set -> {
                """
                { // SET | ${this.dump()}
                    ${this.src.code()}
                    CEU_Value ceu_set_$n = ceu_acc;
                    ${this.dst.code()}
                    ceu_acc = ceu_set_$n;
                }
                """
            }
            is Expr.If -> """
                { // IF | ${this.dump()}
                    ${this.cnd.code()}
                    if (ceu_as_bool(ceu_acc)) {
                        ${this.t.code()}
                    } else {
                        ${this.f.code()}
                    }
                }
                """
            is Expr.XLoop -> this.body.code()
            is Expr.XBreak -> """ // XBREAK | ${this.dump()}
                ${this.cnd.code()}
                if (ceu_as_bool(ceu_acc)) {
                    ${this.e.cond { it.code() }}
                    goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.XLoop }!!.n};
                }
            """
            is Expr.Enum -> ""
            is Expr.Data -> ""
            is Expr.Pass -> "// PASS | ${this.dump()}\n" + this.e.code()
            is Expr.Drop -> this.e.code()

            is Expr.It    -> "ceu_acc = ceu_it;\n"
            is Expr.Catch -> """
                { // CATCH ${this.dump()}
                    do { // catch
                        ${this.body.code()}
                    } while (0); // catch
                    // check free
                    ${(CEU>=3 && ups.any(this) { it is Expr.Proto && it.tk.str!="func" }).cond { """
                        if (ceu_n == CEU_ARG_FREE) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                    if (ceu_acc.type == CEU_VALUE_THROW) {
                        CEU_Value ceu_err = ceu_acc;
                        CEU_Value ceu_it  = ceu_err.Dyn->Throw.val;
                        do {
                            ${this.cnd.code()}
                        } while (0);
                        assert(ceu_acc.type != CEU_VALUE_THROW && "TODO: throw in catch condition");
                        if (!ceu_as_bool(ceu_acc)) {
                            ceu_acc = ceu_err;
                            continue; // uncaught, rethrow
                        }
                        ceu_gc_inc(ceu_err.Dyn->Throw.val);
                        ceu_gc_dec(ceu_err, 1);
                        ceu_acc = ceu_err.Dyn->Throw.val;
                    }
                }
                """
            is Expr.Defer -> {
                val idc = this.idc("defer")
                val (ns,ini,end) = defers.getOrDefault(ups.first_block(this)!!, Triple(mutableListOf(),"",""))
                val inix = """
                    ${(!sta.ylds.contains(ups.first_block(this))).cond {
                    "int ceu_defer_$n;\n"
                }}
                    $idc = 0;       // NO: do not yet execute on termination
                """
                val endx = """
                    if ($idc) {     // ??: execute only if activate
                        do {
                            ${this.body.code()}
                        } while (0);    // catch throw
                        assert(ceu_acc.type != CEU_VALUE_THROW && "TODO: throw in defer");
                    }
                """
                ns.add(n)
                defers[ups.first_block(this)!!] = Triple(ns, ini+inix, endx+end)
                """
                $idc = 1;           // OK: execute on termination
                ceu_acc = ((CEU_Value) { CEU_VALUE_NIL });
                """
            }

            is Expr.Resume -> this.call.code()
            is Expr.Yield -> {
                val bupc = ups.first_block(this)!!.idc("block")
                """
                { // YIELD ${this.dump()}
                    ${this.arg.code()}
                    ceu_frame->exe->pc = $n;      // next resume
                    ceu_frame->exe->status = CEU_EXE_STATUS_YIELDED;
                    if (ceu_acc.type>CEU_VALUE_DYNAMIC && ceu_acc.Dyn->Any.hld.type!=CEU_HOLD_FLEET) {
                        CEU_Value err = { CEU_VALUE_ERROR, {.Error="yield error : cannot receive assigned reference"} };
                        CEU_ERROR($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                    }
                    return ceu_acc;
                case $n: // YIELD ${this.dump()}
                    if (ceu_n == CEU_ARG_FREE) {
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL }; // to be ignored in further move/checks
                        continue;
                    }
                    assert(ceu_n <= 1 && "TODO: multiple arguments to resume");
                    CEU_Value ceu_it = (CEU_Value) { CEU_VALUE_NIL };
                    if (ceu_n == 0) {
                        // no argument
                    } else {
                        ceu_it = ceu_args[0];
                    }
                    if (CEU_ISERR(ceu_it)) {
                        ceu_acc = ceu_it;
                        continue;
                    }
                    ${this.blk.code()}
                    if (ceu_acc.type>CEU_VALUE_DYNAMIC && ceu_acc.Dyn->Any.hld.type!=CEU_HOLD_FLEET && CEU_HLD_BLOCK(ceu_acc.Dyn)->depth>1 CEU4(&& ceu_acc.Dyn->Any.hld.type!=CEU_HOLD_EVENT)) {
                        CEU_Value err = { CEU_VALUE_ERROR, {.Error="resume error : cannot receive assigned reference"} };
                        CEU_ERROR($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                    }
                }
                """
            }

            is Expr.Spawn -> this.call.code()
            is Expr.Bcast -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val ylds = sta.ylds.contains(bup)
                val evtc = this.idc("evt")
                """
                { // BCAST ${this.dump()}
                    ${this.evt.code()}
                    ${(!ylds).cond { "CEU_Value ceu_evt_$n;" }}
                    $evtc = ceu_acc;
                    ${this.xin.code()}
                    assert(ceu_acc.type==CEU_VALUE_TAG && ceu_acc.Tag==CEU_TAG_global);
                    int ceu_isfleet_$n = ($evtc.type>CEU_VALUE_DYNAMIC && $evtc.Dyn->Any.hld.type==CEU_HOLD_FLEET);
                    if (ceu_isfleet_$n) {
                        assert(ceu_hold_chk_set($bupc, CEU_HOLD_EVENT, $evtc, 0, NULL CEU4(COMMA ${if (ylds) 1 else 0})).type != CEU_VALUE_ERROR);
                    }
                    //ceu_acc = ceu_bcast_blocks(&_ceu_block_, $evtc);
                    ceu_gc_inc($evtc);
                    ceu_acc = ceu_bcast_blocks(ceu_bcast_global_block($bupc), $evtc);
                    if (ceu_isfleet_$n) {
                        ceu_gc_chk($evtc.Dyn);
                    }
                    ceu_gc_dec($evtc, 1);
                    CEU_ASSERT($bupc, ceu_acc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");
                }
                """
            }

            is Expr.Nat -> {
                val body = vars.nats[this]!!.let { (set, str) ->
                    var x = str
                    for (v in set) {
                        //println(setOf(x, v))
                        x = x.replaceFirst("XXX", v.idc(0).first)
                    }
                    x
                }
                when (this.tk_.tag) {
                    null   -> body + "\n" + "ceu_acc = ((CEU_Value){ CEU_VALUE_NIL });"
                    ":ceu" -> "ceu_acc = $body;"
                    else -> {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        "ceu_acc = ((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} });"
                    }
                }
            }
            is Expr.Acc -> {
                val (blk,dcl) = vars.get(this)
                val (idc,_idc_) = dcl.idc(this.tk_.upv)
                when {
                    this.isdst() -> {
                        val blk = ups.first_block(this)!!
                        val bupc = blk.idc("block")
                        val src = this.asdst_src()
                        if (dcl.id.upv > 0) {
                            err(tk, "set error : cannot reassign an upval")
                        }
                        assert(!dcl.tmp)    // removed support for "val :tmp x"
                        """
                        { // ACC - SET
                            CEU_ASSERT(
                                $bupc,
                                ceu_hold_chk_set(${_idc_}, CEU_HOLD_MUTAB, $src, 0, "set error" CEU4(COMMA ${if (sta.ylds.contains(blk)) 1 else 0})),
                                "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                            );
                            ceu_gc_inc($src);
                            ceu_gc_dec($idc, 1);
                            $idc = $src;
                        }
                        """
                    }
                    this.isdrop() -> {
                        val bupc = ups.first_block(this)!!.idc("block")
                        """
                        { // ACC - DROP
                            CEU_Value ceu_$n = $idc;
                            CEU_Value args[1] = { ceu_$n };
                            CEU_Frame ceu_frame_$n = { $bupc, NULL CEU3(COMMA NULL) };
                            CEU_ASSERT($bupc, ceu_drop_f(&ceu_frame_$n, 1, args), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                            ceu_gc_dec(ceu_$n, 0);
                            $idc = (CEU_Value) { CEU_VALUE_NIL };
                            ceu_acc = ceu_$n;
                        }
                        """
                    }
                    else -> "ceu_acc = $idc;"
                }
            }
            is Expr.Nil  -> "ceu_acc = ((CEU_Value) { CEU_VALUE_NIL });"
            is Expr.Tag  -> "ceu_acc = ((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_${this.tk.str.tag2c()}} });"
            is Expr.Bool -> "ceu_acc = ((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} });"
            is Expr.Char -> "ceu_acc = ((CEU_Value) { CEU_VALUE_CHAR, {.Char=${this.tk.str}} });"
            is Expr.Num  -> "ceu_acc = ((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} });"

            is Expr.Tuple -> {
                val bupc = ups.first_block(this)!!.idc("block")
                """
                { // TUPLE | ${this.dump()}
                    CEU_Value ceu_tup_$n = ceu_create_tuple(${ups.first_block(this)!!.idc("block")}, ${this.args.size});
                    ${this.args.mapIndexed { i, it ->
                    it.code() + """
                        CEU_ASSERT(
                            $bupc,
                            ceu_tuple_set(&ceu_tup_$n.Dyn->Tuple, $i, ceu_acc CEU4(COMMA 1)),
                            "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                        );
                        """
                }.joinToString("")}
                    ceu_acc = ceu_tup_$n;
                }
                """
            }
            is Expr.Vector -> {
                val bupc = ups.first_block(this)!!.idc("block")
                """
                { // VECTOR | ${this.dump()}
                    CEU_Value ceu_vec_$n = ceu_create_vector(${ups.first_block(this)!!.idc("block")});
                    ${this.args.mapIndexed { i, it ->
                    it.code() + """
                        CEU_ASSERT(
                            $bupc,
                            ceu_vector_set(&ceu_vec_$n.Dyn->Vector, $i, ceu_acc CEU4(COMMA 1)),
                            "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                        );
                        """
                }.joinToString("")}
                    ceu_acc = ceu_vec_$n;
                }
                """
            }
            is Expr.Dict -> {
                val bupc = ups.first_block(this)!!.idc("block")
                """
                { // DICT | ${this.dump()}
                    CEU_Value ceu_dict_$n = ceu_create_dict(${ups.first_block(this)!!.idc("block")});
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            CEU_Value ceu_key_$n = ceu_acc;
                            ${it.second.code()}
                            CEU_Value ceu_val_$n = ceu_acc;
                            CEU_ASSERT(
                                $bupc,
                                ceu_dict_set(&ceu_dict_$n.Dyn->Dict, ceu_key_$n, ceu_val_$n CEU4(COMMA 1)),
                                "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                            );
                        }
                    """ }.joinToString("")}
                    ceu_acc = ceu_dict_$n;
                }
                """
            }
            is Expr.Index -> {
                val blk = ups.first_block(this)!!
                val bupc = blk.idc("block")
                val ylds = if (sta.ylds.contains(blk)) 1 else 0
                val idx = vars.data(this).let { if (it == null) -1 else it.first!! }
                """
                { // INDEX | ${this.dump()}
                    // IDX
                    ${if (idx == -1) {
                        """
                        ${this.idx.code()}
                        CEU_Value ceu_idx_$n = ceu_acc;
                        """
                    } else {
                        """
                        CEU_Value ceu_idx_$n = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=$idx} };
                        """
                    }}
                    // COL
                    ${this.col.code()}
                    CEU_ASSERT($bupc, ceu_col_check(ceu_acc, ceu_idx_$n), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                """ + when {
                    this.isdst() -> {
                        val src = this.asdst_src()
                        """
                        CEU_Value ok = { CEU_VALUE_NIL };
                        switch (ceu_acc.type) {
                            case CEU_VALUE_TUPLE:
                                ok = ceu_tuple_set(&ceu_acc.Dyn->Tuple, ceu_idx_$n.Number, $src CEU4(COMMA $ylds));
                                break;
                            case CEU_VALUE_VECTOR:
                                ok = ceu_vector_set(&ceu_acc.Dyn->Vector, ceu_idx_$n.Number, $src CEU4(COMMA $ylds));
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_acc;
                                ok = ceu_dict_set(&ceu_dict.Dyn->Dict, ceu_idx_$n, $src CEU4(COMMA $ylds));
                                break;
                            }
                            default:
                                assert(0 && "bug found");
                        }
                        CEU_ASSERT($bupc, ok, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                        """
                    }
                    this.isdrop() -> {
                        val bupc = ups.first_block(this)!!.idc("block")
                        """
                        {   // INDEX - DROP
                            CEU_Value ceu_col_$n = ceu_acc;
                            switch (ceu_col_$n.type) {
                                case CEU_VALUE_TUPLE:
                                    ceu_acc = ceu_col_$n.Dyn->Tuple.buf[(int) ceu_idx_$n.Number];
                                    break;
                                case CEU_VALUE_VECTOR:
                                    ceu_acc = CEU_ASSERT($bupc, ceu_vector_get(&ceu_col_$n.Dyn->Vector, ceu_idx_$n.Number), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                    break;
                                case CEU_VALUE_DICT: {
                                    CEU_Value ceu_dict = ceu_col_$n;
                                    ceu_acc = ceu_dict_get(&ceu_dict.Dyn->Dict, ceu_idx_$n);
                                    break;
                                }
                                default:
                                    assert(0 && "bug found");
                            }
                            
                            CEU_Value ceu_val_$n = ceu_acc;
                            CEU_Value args[1] = { ceu_val_$n };
                            CEU_Frame ceu_frame_$n = { $bupc, NULL CEU3(COMMA NULL) };
                            CEU_ASSERT($bupc, ceu_drop_f(&ceu_frame_$n, 1, args), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                            ceu_gc_dec(ceu_val_$n, 0);
                            
                            switch (ceu_col_$n.type) {
                                case CEU_VALUE_TUPLE:
                                    ceu_col_$n.Dyn->Tuple.buf[(int)ceu_idx_$n.Number] = (CEU_Value) {CEU_VALUE_NIL};
                                    break;
                                case CEU_VALUE_VECTOR:
                                    assert(ceu_idx_$n.Number == ceu_col_$n.Dyn->Vector.its-1);
                                    ceu_col_$n.Dyn->Vector.its--;
                                    break;
                                case CEU_VALUE_DICT: {
                                    int ceu_old;
                                    ceu_dict_key_to_index(&ceu_col_$n.Dyn->Dict, ceu_idx_$n, &ceu_old);
                                    if (ceu_old != -1) {
                                        (*ceu_col_$n.Dyn->Dict.buf)[ceu_old][1] = (CEU_Value) { CEU_VALUE_NIL };
                                    }
                                    break;
                                }
                                default:
                                    assert(0 && "bug found");
                            }
                            
                            ceu_acc = ceu_val_$n;
                        }
                        """
                    }
                    else -> """
                        switch (ceu_acc.type) {
                            case CEU_VALUE_TUPLE:
                                ceu_acc = ceu_acc.Dyn->Tuple.buf[(int) ceu_idx_$n.Number];
                                break;
                            case CEU_VALUE_VECTOR:
                                ceu_acc = CEU_ASSERT($bupc, ceu_vector_get(&ceu_acc.Dyn->Vector, ceu_idx_$n.Number), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_acc;
                                ceu_acc = ceu_dict_get(&ceu_dict.Dyn->Dict, ceu_idx_$n);
                                break;
                            }
                            default:
                                assert(0 && "bug found");
                        }
                    """
                } + """
                }
                """
            }
            is Expr.Call -> {
                val up = ups.pub[this]!!
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val dots = this.args.lastOrNull()
                val has_dots = (dots!=null && dots is Expr.Acc && dots.tk.str=="...") && !this.clo.let { it is Expr.Acc && it.tk.str=="{{#}}" }
                val id_dots = if (!has_dots) "" else {
                    val (blk,dcl) = vars.get(dots as Expr.Acc)
                    dcl.idc(0).first
                }

                val upspawn = if (up is Expr.Spawn && up.tasks?.n!=this.n) up else null
                val istasks = (upspawn?.tasks != null)

                """
                { // CALL - open | ${this.dump()}
                    ${has_dots.cond { """
                        int ceu_dots_$n = $id_dots.Dyn->Tuple.its;
                    """ }}
                    CEU_Value ceu_args_$n[${when {
                        !has_dots -> this.args.size
                        else      -> "ceu_dots_$n + " + (this.args.size-1)
                    }}];
                    
                    ${this.args.filter{!(has_dots && it.tk.str=="...")}.mapIndexed { i,e ->
                        e.code() + "ceu_args_$n[$i] = ceu_acc;\n"
                    }.joinToString("")}
                    
                    ${has_dots.cond { """
                        for (int ceu_i_$n=0; ceu_i_$n<ceu_dots_$n; ceu_i_$n++) {
                            ceu_args_$n[${this.args.size-1} + ceu_i_$n] = $id_dots.Dyn->Tuple.buf[ceu_i_$n];
                        }
                    """}}

                    ${istasks.cond {
                        upspawn!!.tasks!!.code() + """
                            ${(!sta.ylds.contains(bup)).cond { "CEU_Value " }}
                            ${up.idc("tasks")} = ceu_acc;
                        """
                    }}
                
                    ${this.clo.code()}
                    ${when {
                        up is Expr.Resume -> """
                                if (ceu_acc.type!=CEU_VALUE_EXE_CORO || (ceu_acc.Dyn->Exe.status!=CEU_EXE_STATUS_YIELDED)) {                
                                    CEU_Value err = { CEU_VALUE_ERROR, {.Error="resume error : expected yielded coro"} };
                                    CEU_ERROR($bupc, "${up.tk.pos.file} : (lin ${up.tk.pos.lin}, col ${up.tk.pos.col})", err);
                                }                            
                                CEU_Frame ceu_frame_$n = ceu_acc.Dyn->Exe.frame;
                            """
                        (upspawn != null) -> """
                            ${istasks.cond2({"""
                                CEU_Value ceu_x_$n = ceu_create_exe_task_in($bupc, ceu_acc, &${up.idc("tasks")}.Dyn->Tasks);
                                if (ceu_x_$n.type == CEU_VALUE_NIL) {
                                    ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} };
                                } else {
                                    // ... below ...
                            """ }, { """
                                CEU_Value ceu_x_$n = ceu_create_exe_task($bupc, ceu_acc);
                            """ })}
                            CEU_ASSERT($bupc, ceu_x_$n, "${up.tk.pos.file} : (lin ${up.tk.pos.lin}, col ${up.tk.pos.col})");
                            CEU_Frame ceu_frame_$n = ceu_x_$n.Dyn->Exe_Task.frame;
                        """
                        else -> """
                            if (ceu_acc.type != CEU_VALUE_CLO_FUNC) {
                                CEU_Value err = { CEU_VALUE_ERROR, {.Error="call error : expected function"} };
                                CEU_ERROR($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                            }
                            CEU_Frame ceu_frame_$n = { $bupc, &ceu_acc.Dyn->Clo CEU3(COMMA {.exe=NULL}) };
                        """
                    }}

                    ceu_acc = ceu_frame_$n.clo->proto (
                        &ceu_frame_$n,
                        ${this.args.let {
                            if (!has_dots) it.size.toString() else {
                                "(" + (it.size-1) + " + ceu_dots_$n)"
                            }
                        }},
                        ceu_args_$n
                    );
                    CEU_ASSERT($bupc, ceu_acc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");
                    ${upspawn.cond { """
                        ${istasks.cond2({"""
                                ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
                            }
                        """ }, { """
                            ceu_acc = ceu_x_$n;
                        """ })}
                    """ }}
                } // CALL - close
                """
            }
        }
    }
}
