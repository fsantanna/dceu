package dceu

import java.lang.Integer.min

class Coder (val outer: Expr.Do, val ups: Ups, val vars: Vars, val clos: Clos, val sta: Static) {
    val pres: MutableList<Pair<String,String>> = mutableListOf()
    val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>> = mutableMapOf()
    val code: String = outer.code()

    fun Expr.idc (pre: String, nst: Int=0): String {
        return if (ups.first_block(this)!!.ismem(sta,clos)) {
            if (nst == 0) {
                "(ceu_mem->${pre}_${this.n})"
            } else {
                val fup = (ups.first(this) { it is Expr.Proto }!! as Expr.Proto).idc()
                "(((CEU_Clo_Mem_$fup*) ceu_frame ${"->clo->up_frame".repeat(nst)}->exe->mem)->${pre}_${this.n})"
            }
        } else {
            "ceu_${pre}_${this.n}"
        }
    }
    fun Expr.Dcl.idc (upv: Int, nst: Int=0): String {
        val blk = vars.dcl_to_blk[this]!!
        return when {
            (upv == 2) -> {
                val idc = this.id.str.idc()
                "(ceu_upvs->$idc)"
            }
            blk.ismem(sta,clos) -> {
                val idc = this.id.str.idc(this.n)
                if (nst == 0) {
                    "(ceu_mem->$idc)"
                } else {
                    val fup = (ups.first(this) { it is Expr.Proto }!! as Expr.Proto).idc()
                    "(((CEU_Clo_Mem_$fup*) ceu_frame ${"->clo->up_frame".repeat(nst)}->exe->mem)->$idc)"
                }
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
    fun Expr.isdrop (): Boolean {
        return ups.pub[this].let { it is Expr.Drop && it.e==this }
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

    fun Expr.code(): String {
        if (this.isdst()) {
            assert(this is Expr.Acc || this is Expr.Index || this is Expr.Pub)
        }
        return when (this) {
            is Expr.Proto -> {
                val blk = ups.first_block(this)!!
                val isexe = (this.tk.str != "func")
                val code = this.blk.code()
                val mem = Mem(ups, vars, clos, sta, defers)
                val id = this.idc()

                val pres = Pair(""" // UPVS | ${this.dump()}
                    ${clos.protos_refs[this].cond { """
                        typedef struct {
                            ${clos.protos_refs[this]!!.map {
                                "CEU_Value ${it.id.str.idc()};"
                            }.joinToString("")}
                        } CEU_Clo_Upvs_$id;                    
                    """ }}
                """ +
                """ // MEM | ${this.dump()}
                    ${isexe.cond { """
                        typedef struct {
                            ${mem.pub(this.blk)}
                        } CEU_Clo_Mem_$id;                        
                    """ }}
                """, """ // FUNC | ${this.dump()}
                    CEU_Value ceu_clo_$id (
                        CEU_Frame* ceu_frame,
                        int ceu_n,
                        CEU_Value ceu_args[]
                    ) {
                        CEU_Value ceu_acc;
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
                                    if (ceu_n == CEU_ARG_ABORT) {
                                        ceu_frame->exe->status = CEU_EXE_STATUS_ABORTED;
                                        return (CEU_Value) { CEU_VALUE_NIL };
                                    }
                        """}}
                        $code
                        ${isexe.cond{"""
                                    ceu_frame->exe->status = (ceu_n==CEU_ARG_ABORT || CEU_ISERR(ceu_acc)) ? CEU_EXE_STATUS_ABORTED : CEU_EXE_STATUS_TERMINATED;
                            }
                        """}}
                        return ceu_acc;
                    }
                """)

                val pos = """ // CLO | ${this.dump()}
                ceu_acc = ceu_create_clo${isexe.cond{"_exe"}} (
                    ${isexe.cond{"CEU_VALUE_CLO_${this.tk.str.uppercase()},"}}
                    ${blk.idc("block")},
                    ${if (clos.protos_noclos.contains(this)) "CEU_HOLD_IMMUT" else "CEU_HOLD_FLEET"},
                    ${when {
                        (blk == outer) -> "NULL"
                        isexe -> "&ceu_frame->exe->frame"
                        else -> "ceu_frame"
                    }},
                    ceu_clo_$id,
                    ${clos.protos_refs[this]?.size ?: 0}
                );
                ${isexe.cond { """
                    ceu_acc.Dyn->Clo_Exe.mem_n = sizeof(CEU_Clo_Mem_$id);                    
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
                            assert(ceu_hold_chk_set_col(ceu_acc.Dyn, ceu_up).type != CEU_VALUE_ERROR);
                            ceu_gc_inc(ceu_up);
                            ((CEU_Clo_Upvs_$id*)ceu_acc.Dyn->Clo.upvs.buf)->${idc} = ceu_up;
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
            is Expr.Export -> this.blk.es.map { it.code() }.joinToString("")   // skip do{}
            is Expr.Do -> {
                val body = this.es.map { it.code() }.joinToString("")   // before defers[this] check
                val _blkc = this.idc("_block")
                val blkc = this.idc("block")
                val up = ups.pub[this]
                val f_b = up?.let { ups.first_proto_or_block(it) }
                val bupc = when {
                    (up == null)        -> "(&_ceu_block_)"
                    (f_b is Expr.Proto) -> "NULL"
                    else                -> ups.first_block(up)!!.idc("block")
                }
                val (bf,ptr) = when {
                    (f_b == null) -> Pair("1", "{.frame=&_ceu_frame_}")
                    (f_b is Expr.Proto) -> Pair("1", "{.frame=${if (f_b.tk.str=="func") "ceu_frame" else "&ceu_frame->exe->frame"}}")
                    else -> Pair("0", "{.block=$bupc}")
                }
                val args = if (f_b !is Expr.Proto) emptySet() else f_b.args.map { it.first.str }.toSet()
                val dcls = vars.blk_to_dcls[this]!!.filter { it.init }
                    .filter { !GLOBALS.contains(it.id.str) }
                    .filter { !(f_b is Expr.Proto && args.contains(it.id.str)) }
                    .map    { it.idc(0) }

                val ismem  = this.ismem(sta,clos)
                val isvoid = sta.void(this)
                //println(listOf(isvoid,this.tk))
                val inexe  = ups.first(this) { it is Expr.Proto }.let { it!=null && it.tk.str!="func" }
                val istsk  = (f_b?.tk?.str == "task")
                val isthus = (this.tk.str == "thus")

                """
                { // BLOCK | ${this.dump()}
                    // CEU_Block ceu_block;
                    ${(!ismem).cond { """
                        ${(!isvoid).cond { """
                            CEU_Block ceu__block_$n;
                        """ }}
                        CEU_Block* ceu_block_$n;
                    """}}
                    // ceu_block = ...;
                    ${if (isvoid) {
                        """
                        $blkc = ${bupc};
                        """
                    } else {
                        """
                        $_blkc = (CEU_Block) { $bf, $ptr, { CEU4(NULL COMMA) {NULL,NULL} } };
                        $blkc = &$_blkc;                                 
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
                            (!ismem).cond {
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
                    ${(!ismem).cond { """
                        ${dcls.map { """
                            CEU_Value $it;
                        """ }.joinToString("")}
                    """ }}
                    // vars inits
                    ${dcls.map { """
                        ${when {
                            (up is Expr.Catch && up.cnd == this) -> """
                                $it = ceu_err.Dyn->Throw.val;
                                ceu_gc_inc($it);
                            """
                            else -> "$it = (CEU_Value) { CEU_VALUE_NIL };"
                        }};
                    """ }.joinToString("")}
                    // defers init
                    ${defers[this].cond { it.second }}
                    // pres funcs
                    ${(f_b == null).cond {
                        pres.unzip().let {
                            it.first.joinToString("") +
                            it.second.joinToString("")
                        }
                    }}
                    
                    ${(CEU >= 2).cond { "do {" }}
                        // main args, func args
                        ${when {
                            (f_b == null) -> """
                                // main block varargs (...)
                                id__dot__dot__dot_ = ceu_create_tuple($blkc, ceu_argc);
                                for (int i=0; i<ceu_argc; i++) {
                                    CEU_Value vec = ceu_vector_from_c_string($blkc, ceu_argv[i]);
                                    assert(ceu_tuple_set(&id__dot__dot__dot_.Dyn->Tuple, i, vec).type != CEU_VALUE_ERROR);
                                }
                            """
                            (f_b is Expr.Proto) -> {
                                val dots = (f_b.args.lastOrNull()?.first?.str == "...")
                                val args_n = f_b.args.size - 1
                                """
                                { // func args
                                    ceu_gc_inc_args(ceu_n, ceu_args);
                                    ${f_b.args.map {
                                        val idc = vars.get(this, it.first.str).idc(0)
                                        """
                                        $idc = (CEU_Value) { CEU_VALUE_NIL };
                                        """
                                    }.joinToString("")}
                                    ${f_b.args.filter { it.first.str!="..." }.mapIndexed { i,arg ->
                                        val idc = vars.get(this, arg.first.str).idc(0)
                                        """
                                        if (ceu_n > $i) {
                                            $idc = ceu_args[$i];
                                            if ($idc.type > CEU_VALUE_DYNAMIC) {
                                                int ceu_type_$n = ($idc.Dyn->Any.hld.type > CEU_HOLD_FLEET) ? CEU_HOLD_FLEET :
                                                                    ($idc.Dyn->Any.hld.type - 1);
                                                // must check CEU_HOLD_FLEET for parallel scopes, but only for exes:
                                                // [gg_02_scope] v -> coro/task
                                                ${(!inexe).cond {"if (ceu_type_$n != CEU_HOLD_FLEET)"}} 
                                                {
                                                    CEU_ASSERT(
                                                        $blkc,
                                                        ceu_hold_chk_set($blkc, ceu_type_$n, $idc, 1, "argument error"),
                                                        "${arg.first.pos.file} : (lin ${arg.first.pos.lin}, col ${arg.first.pos.col})"
                                                    );
                                                }
                                            }
                                        }
                                        """
                                    }.joinToString("")}
                                    ${dots.cond {
                                        val idc = f_b.args.last().first.str.idc()
                                        """
                                        int ceu_tup_n_$n = MAX(0,ceu_n-$args_n);
                                        $idc = ceu_create_tuple($blkc, ceu_tup_n_$n);
                                        for (int i=0; i<ceu_tup_n_$n; i++) {
                                            assert(ceu_tuple_set(&$idc.Dyn->Tuple, i, ceu_args[$args_n+i]).type != CEU_VALUE_ERROR);
                                        }
                                        ceu_gc_inc($idc);
                                    """ }}
                                }
                                """
                        }
                            else -> ""
                        }}
                        $body
                        ${isthus.cond { """
                            if (ceu_thus_fleet_${this.n}) {
                                CEU_Value ret_$N = _ceu_drop_(${(this.es[0] as Expr.Dcl).idc(0)});
                                assert(ret_$N.type == CEU_VALUE_NIL && "TODO-01");
                            }
                        """ }}                
                        ${(up is Expr.Loop).cond { "CEU_LOOP_STOP_${up!!.n}:" }}
                    ${(CEU >= 2).cond { "} while (0);" }}
                    
                    // defers execute
                    CEU_Value ceu_acc_$n = ceu_acc;
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                    ${defers[this].cond { it.third }}
                    if (ceu_acc_$n.type==CEU_VALUE_ERROR && ceu_acc.type==CEU_VALUE_ERROR) {
                        assert(0 && "TODO: double throw in defer");
                    }
                    ceu_acc = ceu_acc_$n;
                    
                    // dcls gc-dec
                    ${dcls.map { """
                        // free below b/c of
                        //   - pending refs defers
                        //   - drop w/ multiple refs
                        ceu_gc_dec($it, 0);
                    """ }.joinToString("")}
                    // args gc-dec (cannot call ceu_gc_dec_args b/c of copy to ids)
                    ${(f_b is Expr.Proto).cond {
                        f_b as Expr.Proto
                        f_b.args.map { arg ->
                            val idc = vars.get(this, arg.first.str).idc(0)
                            """
                            if ($idc.type > CEU_VALUE_DYNAMIC) { // required b/c check below
                                // do not check if they are returned back (this is not the case with locals created here)
                                if ($idc.Dyn->Any.hld.type <= CEU_HOLD_PASSD) {
                                    CEU_Value ceu_err_$n = ceu_hold_chk_set($blkc, $idc.Dyn->Any.hld.type+1, $idc, 1, "TODO");
                                    assert(ceu_err_$n.type == CEU_VALUE_NIL);
                                }
                                ceu_gc_dec($idc, !(ceu_acc.type>CEU_VALUE_DYNAMIC && ceu_acc.Dyn==$idc.Dyn));
                            }
                            """
                        }.joinToString("")
                    }}
                    
                    // pub gc-dec
                    ${istsk.cond { """
                        if (ceu_frame->exe_task->pub.type > CEU_VALUE_DYNAMIC) {
                            // do not check if it is returned back (this is not the case with locals created here)
                            ceu_gc_dec(ceu_frame->exe_task->pub, !(ceu_acc.type>CEU_VALUE_DYNAMIC && ceu_acc.Dyn==ceu_frame->exe_task->pub.Dyn));
                        }
                        ceu_frame->exe_task->pub = ceu_acc;
                    """ }}
                    
                    // move up dynamic ceu_acc (return or error)
                    // after gc-dec b/c of PASSD -> FLEET
                    ${(f_b!=null && !isvoid).cond {
                    val up1 = if (f_b is Expr.Proto) "ceu_frame->up_block" else bupc
                    """
                        //ceu_dump_value(ceu_acc);
                        CEU_Value ceu_err_$n = ceu_hold_chk_set($up1, CEU_HOLD_FLEET, ceu_acc, 0, "block escape error");
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
                    // block free | ${this.dump()}
                    ${(!isvoid).cond { "ceu_gc_rem_all(&$blkc->dn.dyns);" }}
                    // check error
                    ${(CEU>=2 && (f_b is Expr.Do)).cond { """
                        if (CEU_ISERR(ceu_acc)) {
                            continue;
                        }                        
                    """ }}
                    // check free
                    ${(CEU>=3 && (f_b is Expr.Do) && inexe).cond { """
                        if (ceu_n == CEU_ARG_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                }
                """
            }
            is Expr.Dcl -> {
                val idc = this.idc(0)
                val blk = ups.first_block(this)!!
                val bupc = blk.idc("block")
                val unused = false // TODO //sta.unused.contains(this) && (this.src is Expr.Closure)
                val isthus = ups.pub[this].let { it is Expr.Do && it.tk.str=="thus" && it.es[0]==this }

                if (this.id.upv==1 && clos.vars_refs.none { it.second==this }) {
                    err(this.tk, "var error : unreferenced upvar")
                }

                """
                // DCL | ${this.dump()}
                ${(this.init && this.src !=null && !unused).cond {
                    this.src!!.code() + isthus.cond2({ """
                        int ceu_thus_fleet_${blk.n} = 0;
                        #if CEU >= 2
                        if (ceu_acc.type == CEU_VALUE_THROW) {
                            ceu_acc = ceu_acc.Dyn->Throw.val;
                        } else
                        #endif
                        if (ceu_acc.type>CEU_VALUE_DYNAMIC /*CEU2(&& ceu_acc.type!=CEU_VALUE_THROW)*/ && ceu_acc.Dyn->Any.hld.type==CEU_HOLD_FLEET) {
                            ceu_thus_fleet_${blk.n} = 1;
                            CEU_Value ret_$N = ceu_hold_chk_set($bupc, CEU_HOLD_IMMUT, ceu_acc, 0, "TODO");
                            //ceu_dump_value(ret_$N);
                            assert((CEU5(ceu_acc.type==CEU_VALUE_EXE_TASK_IN ||) ret_$N.type==CEU_VALUE_NIL) && "TODO-02");
                        }
                    """ },{ """ 
                        CEU_ASSERT(
                            $bupc,
                            ceu_hold_chk_set($bupc, CEU_HOLD_MUTAB, ceu_acc, 0, "declaration error"),
                            "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                        );
                    """ })
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
                val srcc = this.idc("src")
                """
                { // SET | ${this.dump()}
                    ${this.src.code()}
                    ${(!ups.first_block(this)!!.ismem(sta,clos)).cond {
                        "CEU_Value ceu_src_$n;\n"
                    }}
                    $srcc = ceu_acc;
                    ${this.dst.code()}
                    ceu_acc = $srcc;
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
                    ${this.e.cond { it.code() }}
                    CEU_BREAK = 1;
                    goto CEU_LOOP_STOP_${ups.first(this) { it is Expr.Loop }!!.n};
                }
            """
            is Expr.Enum -> ""
            is Expr.Data -> ""
            is Expr.Pass -> "// PASS | ${this.dump()}\n" + this.e.code()
            is Expr.Drop -> this.e.code()

            is Expr.Catch -> """
                { // CATCH ${this.dump()}
                    do { // catch
                        ${this.blk.code()}
                    } while (0); // catch
                    // check free
                    ${(CEU>=3 && ups.any(this) { it is Expr.Proto && it.tk.str!="func" }).cond { """
                        if (ceu_n == CEU_ARG_ABORT) {
                            continue;   // do not execute next statement, instead free up block
                        }
                    """ }}
                    if (ceu_acc.type == CEU_VALUE_THROW) {
                        CEU_Value ceu_err = ceu_acc;
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
                val bup = ups.first_block(this)!!
                val idc = this.idc("defer")
                val (ns,ini,end) = defers.getOrDefault(bup, Triple(mutableListOf(),"",""))
                val inix = """
                    ${(!bup.ismem(sta,clos)).cond {
                        "int ceu_defer_$n;\n"
                    }}
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
                ceu_acc = ((CEU_Value) { CEU_VALUE_NIL });
                """
            }

            is Expr.Resume -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val coc = this.idc("co")
                """
                ${this.co.code()}
                ${(!bup.ismem(sta,clos)).cond {
                    "CEU_Value $coc;\n"
                }}
                $coc = ceu_acc;
                if ($coc.type!=CEU_VALUE_EXE_CORO || ($coc.Dyn->Exe.status!=CEU_EXE_STATUS_YIELDED)) {                
                    CEU_Value err = { CEU_VALUE_ERROR, {.Error="resume error : expected yielded coro"} };
                    CEU_ERROR($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                }
                ${this.arg.code()}
                ceu_acc = $coc.Dyn->Exe.frame.clo->proto(&$coc.Dyn->Exe.frame, 1, &ceu_acc);
                CEU_ASSERT($bupc, ceu_acc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");                
                """
            }
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
                    if (ceu_n == CEU_ARG_ABORT) {
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL }; // to be ignored in further move/checks
                        continue;
                    }
                    assert(ceu_n <= 1 && "TODO: multiple arguments to resume");
                #if CEU >= 4
                    if (ceu_n == CEU_ARG_ERROR) {
                        ceu_acc = ceu_args[0];
                        continue;
                    }
                #endif
                    ceu_acc = (ceu_n == 1) ? ceu_args[0] : (CEU_Value) { CEU_VALUE_NIL };
                }
                """
            }

            is Expr.Spawn -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val tsksc = this.idc("tsks")
                val tskc = this.idc("tsk")
                """
                ${(!bup.ismem(sta,clos)).cond {"""
                    ${this.tsks.cond { "CEU_Value $tsksc;" }}
                    CEU_Value $tskc;
                """ }}
                ${this.tsk.code()}
                $tskc = ceu_acc;
                ${this.arg.code()}
                CEU_Value ceu_arg_$n = ceu_acc;                
                ${this.tsks.cond2({ """
                    ${it.code()}
                    $tsksc = ceu_acc;
                    CEU_Value ceu_x_$n = ceu_create_exe_task_in($bupc, $tskc, &$tsksc.Dyn->Tasks);
                    if (ceu_x_$n.type == CEU_VALUE_NIL) {
                        ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} };
                    } else {
                        // ... below ...
                """ }, { """
                    CEU_Value ceu_x_$n = ceu_create_exe_task($bupc, $tskc);                    
                """ })}
                CEU_ASSERT($bupc, ceu_x_$n, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                ceu_acc = ceu_bcast_task(&ceu_x_$n.Dyn->Exe_Task, 1, &ceu_arg_$n);
                CEU_ASSERT($bupc, ceu_acc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");
                ${this.tsks.cond2({"""
                        ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
                    }
                """}, {"""
                    ceu_acc = ceu_x_$n;
                """})}
                """
            }
            is Expr.Pub -> {
                val bupc = ups.first_block(this)!!.idc("block")
                this.tsk.cond2({
                    tsk as Expr
                    it.code() + """ // PUB | ${this.dump()}
                        if (!ceu_istask_val(ceu_acc)) {
                            CEU_Value err = { CEU_VALUE_ERROR, {.Error="pub error : expected task"} };
                            CEU_ERROR($bupc, "${this.tsk.tk.pos.file} : (lin ${this.tsk.tk.pos.lin}, col ${this.tsk.tk.pos.col})", err);
                        }
                    """
                },{ """ // PUB | ${this.dump()}
                    ceu_acc = ceu_dyn_to_val((CEU_Dyn*)${up_task_real_c()});
                """ }) +
                when {
                    this.isdst() -> {
                        val src = ups.pub[this]!!.idc("src")
                        """
                        { // PUB - SET | ${this.dump()}
                            CEU_ASSERT(
                                $bupc,
                                ceu_hold_chk_set(ceu_block_up_block($bupc), CEU_HOLD_MUTAB, $src, 0, "set error"),
                                "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                            );
                            ceu_gc_inc($src);
                            ceu_gc_dec(ceu_acc.Dyn->Exe_Task.pub, 1);
                            ceu_acc.Dyn->Exe_Task.pub = $src;
                        }                        
                        """
                    }
                    this.isdrop() -> "assert(0 && \"TODO: drop pub\");"
                    else -> "ceu_acc = ceu_acc.Dyn->Exe_Task.pub;\n"
                }
            }
            is Expr.Bcast -> {
                this.call.code() + """
                    ${ups.first(this) { it is Expr.Proto }.cond {
                        it as Expr.Proto
                        assert(it.tk.str != "func") { "bug found" }
                        """
                        if (ceu_frame->exe->status != CEU_EXE_STATUS_RESUMED) {
                            ceu_n = CEU_ARG_ABORT;
                            continue;
                        }
                        """
                    }}
                """
            }
            is Expr.Dtrack -> {
                val bupc = ups.first_block(this)!!.idc("block")
                """
                { // DTRACK ${this.dump()}
                    ${this.tsk.code()}
                    if (ceu_acc.type != CEU_VALUE_TRACK) {                
                        CEU_Value err = { CEU_VALUE_ERROR, {.Error="detrack error : expected track value"} };
                        CEU_ERROR($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                    }
                    if (ceu_acc.Dyn->Track.task == NULL) {
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                    } else {
                        ceu_acc = ceu_dyn_to_val((CEU_Dyn*)ceu_acc.Dyn->Track.task);
                    }
                }
                """
            }
            is Expr.Toggle -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val tskc = this.idc("tsk")
                """
                ${this.tsk.code()}
                ${(!bup.ismem(sta,clos)).cond {"""
                    CEU_Value $tskc;
                """ }}
                $tskc = ceu_acc;
                ${this.on.code()}
                if (!ceu_istask_val($tskc) || $tskc.Dyn->Exe_Task.status>CEU_EXE_STATUS_TOGGLED) {                
                    CEU_Value err = { CEU_VALUE_ERROR, {.Error="toggle error : expected yielded task"} };
                    CEU_ERROR($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                }
                $tskc.Dyn->Exe_Task.status = (ceu_as_bool(ceu_acc) ? CEU_EXE_STATUS_YIELDED : CEU_EXE_STATUS_TOGGLED);
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
                        val idc = dcl.idc(0, nst)
                        //println(setOf(x, v))
                        x = x.replaceFirst("XXX", idc)
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
                val (vblk,dcl) = vars.get(this)
                val nst = ups
                    .all_until(this) { it==vblk }  // go up until find dcl blk
                    .count { it is Expr.Proto }          // count protos in between acc-dcl
                val idc = dcl.idc(this.tk_.upv, nst)
                when {
                    this.isdst() -> {
                        val ublk = ups.first_block(this)!!
                        val bupc = ublk.idc("block")
                        val src = ups.pub[this]!!.idc("src")
                        if (dcl.id.upv > 0) {
                            err(tk, "set error : cannot reassign an upval")
                        }
                        """
                        { // ACC - SET
                            CEU_ASSERT(
                                $bupc,
                                ceu_hold_chk_set(${vblk.idc("block",nst)}, CEU_HOLD_MUTAB, $src, 0, "set error"),
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
                            CEU_Frame ceu_frame_$n = { $bupc, NULL CEU3(COMMA {.exe=NULL}) };
                            CEU_ASSERT($bupc, ceu_drop_f(&ceu_frame_$n, 1, &ceu_$n), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                            ceu_gc_dec(ceu_$n, 0);
                            $idc = (CEU_Value) { CEU_VALUE_NIL };
                            ceu_acc = ceu_$n;
                        }
                        """
                    }
                    else -> "ceu_acc = $idc;\n"
                }
            }
            is Expr.Nil  -> "ceu_acc = ((CEU_Value) { CEU_VALUE_NIL });"
            is Expr.Tag  -> "ceu_acc = ((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_${this.tk.str.tag2c()}} });"
            is Expr.Bool -> "ceu_acc = ((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} });"
            is Expr.Char -> "ceu_acc = ((CEU_Value) { CEU_VALUE_CHAR, {.Char=${this.tk.str}} });"
            is Expr.Num  -> "ceu_acc = ((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} });"

            is Expr.Tuple -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val tupc = this.idc("tup")
                """
                { // TUPLE | ${this.dump()}
                    ${(!bup.ismem(sta,clos)).cond {
                        "CEU_Value ceu_tup_$n;\n"
                    }}
                    $tupc = ceu_create_tuple(${bup.idc("block")}, ${this.args.size});
                    ${this.args.mapIndexed { i, it ->
                    it.code() + """
                        CEU_ASSERT(
                            $bupc,
                            ceu_tuple_set(&$tupc.Dyn->Tuple, $i, ceu_acc),
                            "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                        );
                        """
                }.joinToString("")}
                    ceu_acc = $tupc;
                }
                """
            }
            is Expr.Vector -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val vecc = this.idc("vec")
                """
                { // VECTOR | ${this.dump()}
                    ${(!bup.ismem(sta,clos)).cond {
                        "CEU_Value ceu_vec_$n;\n"
                    }}
                    $vecc = ceu_create_vector(${bup.idc("block")});
                    ${this.args.mapIndexed { i, it ->
                    it.code() + """
                        CEU_ASSERT(
                            $bupc,
                            ceu_vector_set(&$vecc.Dyn->Vector, $i, ceu_acc),
                            "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                        );
                        """
                }.joinToString("")}
                    ceu_acc = $vecc;
                }
                """
            }
            is Expr.Dict -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val dicc = this.idc("dic")
                val keyc = this.idc("key")
                val ismem = bup.ismem(sta,clos)
                """
                { // DICT | ${this.dump()}
                    ${(!ismem).cond {
                        "CEU_Value ceu_dic_$n;\n"
                    }}
                    $dicc = ceu_create_dict(${bup.idc("block")});
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            ${(!ismem).cond {
                                "CEU_Value ceu_key_$n;\n"
                            }}
                            $keyc = ceu_acc;
                            ${it.second.code()}
                            CEU_Value ceu_val_$n = ceu_acc;
                            CEU_ASSERT(
                                $bupc,
                                ceu_dict_set(&$dicc.Dyn->Dict, $keyc, ceu_val_$n),
                                "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})"
                            );
                        }
                    """ }.joinToString("")}
                    ceu_acc = $dicc;
                }
                """
            }
            is Expr.Index -> {
                val blk = ups.first_block(this)!!
                val bupc = blk.idc("block")
                val idx = vars.data(this).let { if (it == null) -1 else it.first!! }
                val idxc = this.idc("idx")
                """
                { // INDEX | ${this.dump()}
                    // IDX
                    ${(!blk.ismem(sta,clos)).cond {
                        "CEU_Value ceu_idx_$n;\n"
                    }}
                    ${if (idx == -1) {
                        """
                        ${this.idx.code()}
                        $idxc = ceu_acc;
                        """
                    } else {
                        """
                        $idxc = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=$idx} };
                        """
                    }}
                    // COL
                    ${this.col.code()}
                    CEU_Value ceu_col_$n = ceu_acc;
                    CEU_ASSERT($bupc, ceu_col_check(ceu_col_$n, $idxc), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                """ + when {
                    this.isdst() -> {
                        val src = ups.pub[this]!!.idc("src")
                        """
                        CEU_Value ok = { CEU_VALUE_NIL };
                        switch (ceu_col_$n.type) {
                            case CEU_VALUE_TUPLE:
                                ok = ceu_tuple_set(&ceu_col_$n.Dyn->Tuple, $idxc.Number, $src);
                                break;
                            case CEU_VALUE_VECTOR:
                                ok = ceu_vector_set(&ceu_col_$n.Dyn->Vector, $idxc.Number, $src);
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_col_$n;
                                ok = ceu_dict_set(&ceu_dict.Dyn->Dict, $idxc, $src);
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
                            switch (ceu_col_$n.type) {
                                case CEU_VALUE_TUPLE:
                                    ceu_acc = ceu_col_$n.Dyn->Tuple.buf[(int) $idxc.Number];
                                    break;
                                case CEU_VALUE_VECTOR:
                                    ceu_acc = CEU_ASSERT($bupc, ceu_vector_get(&ceu_col_$n.Dyn->Vector, $idxc.Number), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                    break;
                                case CEU_VALUE_DICT: {
                                    CEU_Value ceu_dict = ceu_col_$n;
                                    ceu_acc = ceu_dict_get(&ceu_dict.Dyn->Dict, $idxc);
                                    break;
                                }
                                default:
                                    assert(0 && "bug found");
                            }
                            
                            CEU_Value ceu_val_$n = ceu_acc;
                            CEU_Frame ceu_frame_$n = { $bupc, NULL CEU3(COMMA NULL) };
                            CEU_ASSERT($bupc, ceu_drop_f(&ceu_frame_$n, 1, &ceu_val_$n), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                            ceu_gc_dec(ceu_val_$n, 0);
                            
                            switch (ceu_col_$n.type) {
                                case CEU_VALUE_TUPLE:
                                    ceu_col_$n.Dyn->Tuple.buf[(int)$idxc.Number] = (CEU_Value) {CEU_VALUE_NIL};
                                    break;
                                case CEU_VALUE_VECTOR:
                                    assert($idxc.Number == ceu_col_$n.Dyn->Vector.its-1);
                                    ceu_col_$n.Dyn->Vector.its--;
                                    break;
                                case CEU_VALUE_DICT: {
                                    int ceu_old;
                                    ceu_dict_key_to_index(&ceu_col_$n.Dyn->Dict, $idxc, &ceu_old);
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
                        switch (ceu_col_$n.type) {
                            case CEU_VALUE_TUPLE:
                                ceu_acc = ceu_col_$n.Dyn->Tuple.buf[(int) $idxc.Number];
                                break;
                            case CEU_VALUE_VECTOR:
                                ceu_acc = CEU_ASSERT($bupc, ceu_vector_get(&ceu_col_$n.Dyn->Vector, $idxc.Number), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                break;
                            case CEU_VALUE_DICT:
                                ceu_acc = ceu_dict_get(&ceu_col_$n.Dyn->Dict, $idxc);
                                break;
                            default:
                                assert(0 && "bug found");
                        }
                    """
                } + """
                }
                """
            }
            is Expr.Call -> {
                val bup = ups.first_block(this)!!
                val bupc = bup.idc("block")
                val argsc = this.idc("args")
                val dots = this.args.lastOrNull()
                val has_dots = (dots!=null && dots is Expr.Acc && dots.tk.str=="...") && !this.clo.let { it is Expr.Acc && it.tk.str=="{{#}}" }
                val id_dots = if (!has_dots) "" else {
                    val (blk,dcl) = vars.get(dots as Expr.Acc)
                    dcl.idc(0)
                }
                """
                { // CALL | ${this.dump()}
                    ${(!bup.ismem(sta,clos)).cond {
                        "CEU_Value ceu_args_$n[${this.args.size}];\n"
                    }}
                    ${this.args.filter{!(has_dots && it.tk.str=="...")}.mapIndexed { i,e -> """
                        ${e.code()}
                        $argsc[$i] = ceu_acc;
                    """ }.joinToString("")}
                    
                    ${has_dots.cond { """
                        int ceu_dots_$n = $id_dots.Dyn->Tuple.its;
                        CEU_Value _ceu_args_$n[ceu_dots_$n + ${this.args.size-1}];
                        memcpy(_ceu_args_$n, $argsc, ${this.args.size-1} * sizeof(CEU_Value));
                        for (int ceu_i_$n=0; ceu_i_$n<ceu_dots_$n; ceu_i_$n++) {
                            _ceu_args_$n[${this.args.size-1} + ceu_i_$n] = $id_dots.Dyn->Tuple.buf[ceu_i_$n];
                        }
                    """ }}
                        
                    ${this.clo.code()}
                    if (ceu_acc.type != CEU_VALUE_CLO_FUNC) {
                        CEU_Value err = { CEU_VALUE_ERROR, {.Error="call error : expected function"} };
                        CEU_ERROR($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})", err);
                    }
                    
                    CEU_Frame ceu_frame_$n = { $bupc, &ceu_acc.Dyn->Clo CEU3(COMMA {.exe=NULL}) };
                    ceu_acc = ceu_frame_$n.clo->proto (
                        &ceu_frame_$n,
                        ${this.args.let {
                            if (!has_dots) it.size.toString() else {
                                "(" + (it.size-1) + " + ceu_dots_$n)"
                            }
                        }},
                        ${if (has_dots) "_ceu_args_$n" else argsc}
                    );
                    CEU_ASSERT($bupc, ceu_acc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");
                } // CALL | ${this.dump()}
                """
            }
        }
    }
}
