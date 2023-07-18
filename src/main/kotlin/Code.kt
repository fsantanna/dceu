package dceu

import java.lang.Integer.min

class Coder (val outer: Expr.Do, val ups: Ups, val defers: Defers, val vars: Vars, val clos: Clos, val unsf: Unsafe) {
    val pres: MutableList<String> = mutableListOf()
    val code: String = outer.code()

    fun Expr.Do.toc (isptr: Boolean): String {
        return "ceu_block_${this.n}".let {
            if (isptr) "(&($it))" else it
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
    fun Expr.assrc (v: String): String {
        return if (this.isdst()) {
            "ceu_acc = (CEU_Value) { CEU_VALUE_NIL };\n"
        } else {
            "ceu_acc = $v;\n"
        }
    }

    fun Expr.tmp_hold (tmp: Boolean?): String {
        return when {
            (tmp == null) -> "CEU_HOLD_MUTABLE"
            (tmp == true) -> "CEU_HOLD_FLEETING"
            (ups.first(this){ it is Expr.Proto } != null) -> "CEU_HOLD_FLEETING"
            else -> "CEU_HOLD_MUTABLE"
        }

    }
    fun Expr.code(): String {
        if (this.isdst()) {
            assert(this is Expr.Acc || this is Expr.Index)
        }
        return when (this) {
            is Expr.Proto -> {
                val up_blk = ups.first_block(this)!!

                """ // TYPE ${this.tk.dump()}
                    ${clos.protos_refs[this].cond { """
                        typedef struct {
                            ${clos.protos_refs[this]!!.map {
                            "CEU_Value ${it.id.str.id2c(it.n)};"
                        }.joinToString("")}
                        } CEU_Proto_Upvs_$n;                    
                    """ }}
                """ + """ // PROTO ${this.tk.dump()}
                    CEU_RET ceu_proto_$n (
                        CEU_Frame* ceu_frame,
                        int ceu_n,
                        CEU_Value ceu_args[]
                    ) {
                        CEU_RET ceu_ret = CEU_RET_RETURN;
                        ${clos.protos_refs[this].cond { """
                            CEU_Proto_Upvs_$n* ceu_upvs = (CEU_Proto_Upvs_$n*) ceu_frame->closure->upvs.buf;                    
                        """ }}
                        ${this.args.map { (id,_) ->
                            val dcl = vars.get(this.body, id.str)
                            val idc = id.str.id2c(dcl.n)
                            """
                            CEU_Value $idc;
                            CEU_Block* _${idc}_;
                            """
                        }.joinToString("")}
                        do { // func
                            ${this.body.code()}
                        } while (0); // func
                        return ceu_ret;
                    }
                """ + """ // CLOSURE ${this.tk.dump()}
                CEU_Closure* ceu_closure_$n = ceu_closure_create (
                    ${up_blk.toc(true)},
                    ${if (clos.protos_noclos.contains(this)) "CEU_HOLD_IMMUTABLE" else "CEU_HOLD_FLEETING"},
                    ${if (up_blk == outer) "NULL" else "ceu_frame"},
                    ceu_proto_$n,
                    ${clos.protos_refs[this]?.size ?: 0}
                );
                ${clos.protos_refs[this].cond {
                    it.map { dcl ->
                        val dcl_blk = vars.dcl_to_blk[dcl]!!
                        val idc = dcl.id.str.id2c(dcl.n)
                        val btw = ups
                            .all_until(this) { dcl_blk==it }
                            .filter { it is Expr.Proto }
                            .count() // other protos in between myself and dcl, so it its an upref (upv=2)
                        val upv = min(2, btw)
                        """
                        {
                            CEU_Value ceu_up = ${vars.id2c(ups.pub[this]!!, dcl_blk, dcl, upv).first};
                            if (ceu_up.type > CEU_VALUE_DYNAMIC) {
                                assert(ceu_block_chk_set_mutual(&ceu_up.Dyn->Any, (CEU_Any*)ceu_closure_$n));
                                //assert(CEU_RET_RETURN == ceu_block_set(ceu_closure_$n, ceu_up.Dyn->Any.hold.up_block, ceu_up->Dyn->Any.hold.type));
                            } else {
                                assert(ceu_block_chk_set(ceu_up, ceu_closure_$n->hold.up_block, ceu_closure_$n->hold.type));
                            }
                            ceu_gc_inc(ceu_up);
                            ((CEU_Proto_Upvs_$n*)ceu_closure_$n->upvs.buf)->${idc} = ceu_up;
                        }
                        """   // TODO: use this.body (ups.ups[this]?) to not confuse with args
                    }.joinToString("\n")                    
                }}
                assert(ceu_closure_$n != NULL);
                ${assrc("(CEU_Value) { CEU_VALUE_CLOSURE, {.Dyn=(CEU_Dyn*)ceu_closure_$n} }")}
                """
            }
            is Expr.Do -> {
                val body = this.es.map { it.code() }.joinToString("")   // before defers[this] check
                val up = ups.pub[this]
                val bup = up?.let { ups.first_block(it) }
                val f_b = up?.let { ups.first_proto_or_block(it) }
                val depth = when {
                    (f_b == null) -> "1"
                    (f_b is Expr.Proto) -> "ceu_frame->up_block->depth + 1"
                    else -> "(${bup!!.toc(false)}.depth + 1)"
                }
                val dcls = vars.blk_to_dcls[this]!!
                val args = if (f_b !is Expr.Proto) emptySet() else f_b.args.map { it.first.str }.toSet()
                val ids = dcls.filter { it.init }
                    .filter { !GLOBALS.contains(it.id.str) }
                    .filter { !(f_b is Expr.Proto && args.contains(it.id.str)) }
                    .map    { it.id.str.id2c(it.n) }
                """
                { // BLOCK ${this.tk.dump()}
                    CEU_Block ceu_block_$n = (CEU_Block) { $depth, ceu_frame, NULL };
                    void* ceu_block = &ceu_block_$n;   // generic name to debug
                    #ifdef CEU_DEBUG
                    printf(">>> BLOCK = %p in %p\n", &ceu_block_$n, ceu_frame);
                    #endif
                    ${(f_b == null).cond { """
                    //{   // ... for main block
                        CEU_Tuple* tup = ceu_tuple_create(&ceu_block_$n, ceu_argc);
                        for (int i=0; i<ceu_argc; i++) {
                            CEU_Vector* vec = ceu_vector_from_c_string(&ceu_block_$n, ceu_argv[i]);
                            assert(ceu_tuple_set(tup, i, (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=(CEU_Dyn*)vec} }));
                        }
                        CEU_Value _dot__dot__dot_ = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)tup} };
                    //}
                    """ }}
                    ${(f_b is Expr.Proto).cond { // initialize parameters from outer proto
                        f_b as Expr.Proto
                        val dots = (f_b.args.lastOrNull()?.first?.str == "...")
                        val args_n = f_b.args.size - 1
                        """
                        { // func args
                            int ceu_i = 0;
                            ${f_b.args.filter { it.first.str!="..." }.map {
                                val dcl = vars.get(this, it.first.str)
                                val idc = it.first.str.id2c(dcl.n)
                                """
                                _${idc}_ = &ceu_block_$n;
                                if (ceu_i < ceu_n) {
                                    if (!ceu_block_chk_set(ceu_args[ceu_i], _${idc}_, CEU_HOLD_FLEETING)) {
                                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : argument error : incompatible scopes");
                                    }
                                    $idc = ceu_args[ceu_i];
                                    ceu_gc_inc($idc);
                                } else {
                                    $idc = (CEU_Value) { CEU_VALUE_NIL };
                                }
                                ceu_i++;
                                """
                            }.joinToString("")}
                            ${dots.cond {
                                val idc = f_b.args.last()!!.first.str.id2c(null)
                                """
                                int ceu_tup_n = MAX(0,ceu_n-$args_n);
                                CEU_Tuple* ceu_tup = ceu_tuple_create(&ceu_block_$n, ceu_tup_n);
                                for (int i=0; i<ceu_tup_n; i++) {
                                    assert(ceu_tuple_set(ceu_tup, i, ceu_args[$args_n+i]));
                                }
                                $idc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ceu_tup} };
                                ceu_gc_inc($idc);
                            """ }}
                        }
                        """ 
                    }}
                    ${ids.map {
                        if (it in listOf("evt","_")) "" else """
                            CEU_Value $it = (CEU_Value) { CEU_VALUE_NIL };
                    """ }.joinToString("")
                    }
                    ${defers.pub[this]!!.map {
                        "int ceu_defer_${it.key.n} = 0;\n"
                    }.joinToString("")}
                    do { // block
                        $body
                    } while (0); // block
                    #ifdef CEU_DEBUG
                    printf("<<< BLOCK = %d/%p in %p\n", $n, &ceu_block_$n, ceu_frame);
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
                                ceu_gc_inc(ceu_acc);
                            }
                            """
                        }}
                        {
                            { // move up dynamic ceu_acc (return or error)
                                ${
                                    (f_b != null).cond {
                                        val up1 = if (f_b is Expr.Proto) "ceu_frame->up_block" else bup!!.toc(true)
                                        """
                                        if (!ceu_block_chk_set(ceu_acc, $up1, CEU_HOLD_FLEETING)) {
                                            CEU_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : block escape error : incompatible scopes");
                                            // prioritize scope error over whatever there is now
                                            ceu_acc_$n = CEU_ERR_ERROR;
                                            ceu_ret_$n = MIN(ceu_ret_$n, CEU_RET_THROW);
                                        }
                                        """
                                        }
                                }
                            }
                            ${(defers.pub[this]!!.size > 0).cond { """
                                { // DEFERS ${this.tk.dump()}
                                    ceu_ret = CEU_RET_RETURN;
                                    ${defers.pub[this]!!.map{it.value}.reversed().joinToString("")}
                                    if (ceu_ret_$n!=CEU_RET_THROW && ceu_ret==CEU_RET_THROW) {
                                        ceu_acc_$n = ceu_acc;
                                    }
                                    ceu_ret_$n = MIN(ceu_ret_$n, ceu_ret);
                                }
                            """}}
                            { // decrement refs
                                ${ids.map { if (it in listOf("evt","_")) "" else
                                    """
                                    if ($it.type > CEU_VALUE_DYNAMIC) {
                                        ceu_gc_dec($it, ($it.Dyn->Any.hold.up_block == &ceu_block_$n));
                                    }
                                    """
                                }.joinToString("")}
                                ${(f_b is Expr.Proto).cond {
                                    (f_b as Expr.Proto).args.map {
                                        val dcl = vars.get(this, it.first.str)
                                        val idc = it.first.str.id2c(dcl.n)
                                        "ceu_gc_dec($idc, 1);"
                                    }.joinToString("")
                                }}
                            }
                            ceu_block_free(&ceu_block_$n);
                        }
                        ceu_acc = ceu_acc_$n;
                        ceu_ret = ceu_ret_$n;
                        CEU_CONTINUE_ON_THROW();
                        ${(f_b is Expr.Proto).cond { "ceu_gc_dec(ceu_acc, 0);" }}
                    } // ceu_ret/ceu_acc: save/restore
                }
                """
            }
            is Expr.Dcl -> {
                val id = this.id.str
                val idc = id.id2c(this.n)
                val bupc = ups.first_block(this)!!.toc(true)
                val unused = false // TODO //sta.unused.contains(this) && (this.src is Expr.Closure)

                if (this.id.upv==1 && clos.vars_refs.none { it.second==this }) {
                    err(this.tk, "var error : unreferenced upvar")
                }

                """
                CEU_Value $idc = (CEU_Value) { CEU_VALUE_NIL };      // src may fail (protect var w/ nil)
                ${(this.init && this.src!=null && !unused).cond {
                    this.src!!.code() + """
                        if (!ceu_block_chk_set(ceu_acc, $bupc, ${this.tmp_hold(this.tmp)})) {
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : declaration error : incompatible scopes");
                        }
                    """
                }}
                ${if (id in listOf("evt","_")) "" else {
                    """
                    ${when {
                        !this.init -> ""
                        (this.src == null) -> ""
                        else -> "$idc = ceu_acc;"
                    }}
                    CEU_Block* _${idc}_ = $bupc;   // can't be static b/c recursion
                    ceu_gc_inc(${idc});
                    #if 1
                        ${assrc(idc)}
                    #else // b/c of ret scope
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                    #endif
                    """
                }}
                """
            }
            is Expr.Set -> {
                """
                { // SET ${this.tk.dump()}
                    ${this.src.code()}
                    CEU_Value ceu_set_$n = ceu_acc;
                    ${this.dst.code()}
                    #if 1
                        ${assrc("ceu_set_$n")}
                    #else // b/c of ret scope
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                    #endif
                }
                """
            }
            is Expr.If -> """
                { // IF ${this.tk.dump()}
                    ${this.cnd.code()}
                    if (ceu_as_bool(ceu_acc)) {
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
            is Expr.XBreak -> """
                goto CEU_LOOP_DONE_${this.nn};
            """.trimIndent()
            is Expr.Catch -> """
                { // CATCH ${this.tk.dump()}
                    do { // catch
                        ${this.body.code()}
                    } while (0); // catch
                    if (ceu_ret == CEU_RET_THROW) {
                        ceu_ret = CEU_RET_RETURN;
                        CEU_Value ceu_err = ceu_acc;
                        do {
                            ${this.cnd.code()}
                        } while (0);
                        CEU_CONTINUE_ON_THROW();
                        if (!ceu_as_bool(ceu_acc)) {
                            if (ceu_err.type>CEU_VALUE_DYNAMIC && ceu_err.Dyn->Any.hold.type!=CEU_HOLD_FLEETING) {
                                CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : rethrow error : incompatible scopes");
                            }
                            CEU_THROW_DO(ceu_err, continue); // uncaught, rethrow
                        }
                        ceu_gc_dec(ceu_err, 0);    // do not check, bc throw value may be captured in assignment
                        ${assrc("ceu_acc = ceu_err")}
                        /*
                        ceu_gc_dec(ceu_err, 1);
                        ${assrc("ceu_acc = (CEU_Value) { CEU_VALUE_NIL }")}
                        */
                    }
                }
                """
            is Expr.Defer -> {
                defers.pub[ups.first_block(this)!!]!![this] = """
                    if (ceu_defer_$n) {
                        ${this.body.code()}
                    }
                """
                """
                ceu_defer_$n = 1;
                ${assrc("((CEU_Value) { CEU_VALUE_NIL })")}
                """
            }
            is Expr.Enum -> ""
            is Expr.Data -> ""
            is Expr.Pass -> this.e.code()
            is Expr.Drop -> this.e.code()

            is Expr.Nat -> {
                val body = vars.nat_to_str[this]!!
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
                    pres.add(pre)
                }
                pos
            }
            is Expr.Acc -> {
                val (blk,dcl) = vars.get(this)
                val (idc,_idc_) = vars.id2c(this,blk,dcl,this.tk_.upv)
                when {
                    this.isdst() -> {
                        val src = this.asdst_src()
                        if (dcl.id.upv > 0) {
                            err(tk, "set error : cannot reassign an upval")
                        }
                        """
                        { // ACC - SET
                            if (!ceu_block_chk_set($src, ${_idc_}, ${this.tmp_hold(dcl.tmp)})) {
                                CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                            }
                            ceu_gc_inc($src);
                            ceu_gc_dec($idc, 1);
                            $idc = $src;
                        }
                        """
                    }
                    this.isdrop() -> {
                        val bupc = ups.first_block(this)!!.toc(true)
                        """
                        { // ACC - DROP
                            CEU_Value ceu_$n = $idc;
                            CEU_Value args[1] = { ceu_$n };
                            CEU_Frame ceu_frame_$n = { NULL, $bupc };
                            ceu_ret = ceu_drop_f(&ceu_frame_$n, 1, args);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                            ceu_gc_dec(ceu_$n, 0);
                            $idc = (CEU_Value) { CEU_VALUE_NIL };
                            ceu_acc = ceu_$n;
                        }
                        """
                    }
                    else -> assrc(idc)
                    /*
                    when {
                        !xvar.dcl.poly -> assrc(idc)    // ACC ${this.tk.dump()}
                        xvar.dcl.poly -> """
                            assert($idc.type==CEU_VALUE_DICT && "TODO");
                            CEU_Value ceu_tag = { CEU_VALUE_TAG, {.Tag=CEU_TAG_number} };
                            CEU_Value ceu_fld = ceu_dict_get($idc.Dyn, &ceu_tag);
                            ${assrc("ceu_fld")}
                        """
                        else -> error("impossible case")
                    }
                     */
                }
            }
            is Expr.Err -> {
                when (this.tk.str) {
                    "err" -> assrc("ceu_err")
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
                    CEU_Tuple* ceu_tup_$n = ceu_tuple_create(${ups.first_block(this)!!.toc(true)}, ${this.args.size});
                    assert(ceu_tup_$n != NULL);
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        if (!ceu_tuple_set(ceu_tup_$n, $i, ceu_acc)) {
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : tuple error : incompatible scopes");
                        }
                        """
                    }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ceu_tup_$n} }")}
                }
            """
            is Expr.Vector -> """
                { // VECTOR ${this.tk.dump()}
                    CEU_Vector* ceu_vec_$n = ceu_vector_create(${ups.first_block(this)!!.toc(true)});
                    assert(ceu_vec_$n != NULL);
                    ${this.args.mapIndexed { i, it ->
                        it.code() + """
                        ceu_ret = ceu_vector_set(ceu_vec_$n, $i, ceu_acc);
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : vector error : incompatible scopes");
                        """
                    }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=(CEU_Dyn*)ceu_vec_$n} }")}
                }
            """
            is Expr.Dict -> {
                """
                { // DICT ${this.tk.dump()}
                    CEU_Dict* ceu_dict_$n = ceu_dict_create(${ups.first_block(this)!!.toc(true)});
                    assert(ceu_dict_$n != NULL);
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            CEU_Value ceu_key_$n = ceu_acc;
                            ${it.second.code()}
                            CEU_Value ceu_val_$n = ceu_acc;
                            ceu_ret = ceu_dict_set(ceu_dict_$n, ceu_key_$n, ceu_val_$n);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : dict error : incompatible scopes");
                        }
                    """ }.joinToString("")}
                    ${assrc("(CEU_Value) { CEU_VALUE_DICT, {.Dyn=(CEU_Dyn*)ceu_dict_$n} }")}
                }
                """
            }
            is Expr.Index -> {
                val idx = vars.data(this).let { if (it == null) -1 else it.first!! }
                """
                { // INDEX  ${this.tk.dump()}
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
                    ceu_ret = ceu_col_check(ceu_acc, ceu_idx_$n);
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                """ + when {
                    this.isdst() -> {
                        val src = this.asdst_src()
                        """
                        if (!ceu_block_chk_set(ceu_idx_$n, ceu_acc.Dyn->Any.hold.up_block, CEU_HOLD_FLEETING)) {
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                        }
                        if (!ceu_block_chk_set($src, ceu_acc.Dyn->Any.hold.up_block, CEU_HOLD_FLEETING)) {
                            CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                        }
                        switch (ceu_acc.type) {
                            case CEU_VALUE_TUPLE:
                                if (!ceu_tuple_set(&ceu_acc.Dyn->Tuple, ceu_idx_$n.Number, $src)) {
                                    ceu_ret = CEU_RET_THROW;
                                }
                                break;
                            case CEU_VALUE_VECTOR:
                                if (!ceu_vector_set(&ceu_acc.Dyn->Vector, ceu_idx_$n.Number, $src)) {
                                    ceu_ret = CEU_RET_THROW;
                                }
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_acc;
                                if (!ceu_dict_set(&ceu_dict.Dyn->Dict, ceu_idx_$n, $src)) {
                                    ceu_ret = CEU_RET_THROW;
                                }
                                break;
                            }
                            default:
                                assert(0 && "bug found");
                        }
                        CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                        """
                    }
                    this.isdrop() -> {
                        val bupc = ups.first_block(this)!!.toc(true)
                        """
                        {   // INDEX - DROP
                            CEU_Value ceu_col_$n = ceu_acc;
                            switch (ceu_col_$n.type) {
                                case CEU_VALUE_TUPLE:
                                    ${assrc("ceu_col_$n.Dyn->Tuple.buf[(int) ceu_idx_$n.Number]")}
                                    break;
                                case CEU_VALUE_VECTOR:
                                    ceu_ret = ceu_vector_get(&ceu_col_$n.Dyn->Vector, ceu_idx_$n.Number);
                                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                    break;
                                case CEU_VALUE_DICT: {
                                    CEU_Value ceu_dict = ceu_col_$n;
                                    ${assrc("ceu_dict_get(&ceu_dict.Dyn->Dict, ceu_idx_$n)")}
                                    break;
                                }
                                default:
                                    assert(0 && "bug found");
                            }
                            
                            CEU_Value ceu_val_$n = ceu_acc;
                            CEU_Value args[1] = { ceu_val_$n };
                            CEU_Frame ceu_frame_$n = { NULL, $bupc };
                            ceu_ret = ceu_drop_f(&ceu_frame_$n, 1, args);
                            CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
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
                                ${assrc("ceu_acc.Dyn->Tuple.buf[(int) ceu_idx_$n.Number]")}
                                break;
                            case CEU_VALUE_VECTOR:
                                ceu_ret = ceu_vector_get(&ceu_acc.Dyn->Vector, ceu_idx_$n.Number);
                                CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_acc;
                                ${assrc("ceu_dict_get(&ceu_dict.Dyn->Dict, ceu_idx_$n)")}
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
                val bupc = ups.first_block(this)!!.toc(true)
                val dots = this.args.lastOrNull()
                val has_dots = (dots!=null && dots is Expr.Acc && dots.tk.str=="...") && !this.closure.let { it is Expr.Acc && it.tk.str=="{{#}}" }
                val id_dots = if (!has_dots) "" else {
                    val (blk,dcl) = vars.get(dots as Expr.Acc)
                    vars.id2c(this, blk, dcl, 0).first
                }
                //println(listOf(id_dots,has_dots,(dots!=null && dots is Expr.Acc && dots.tk.str=="..."),dots))

                """
                { // CALL ${this.tk.dump()}
                    ${this.closure.code()}
                    CEU_Value ceu_closure_$n = ceu_acc;
                    if (ceu_closure_$n.type != CEU_VALUE_CLOSURE) {
                        CEU_THROW_DO_MSG(CEU_ERR_ERROR, continue, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : call error : expected function");
                    }
                    CEU_Frame ceu_frame_$n = { &ceu_closure_$n.Dyn->Closure, $bupc };
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

                    ceu_ret = ceu_frame_$n.closure->proto (
                        &ceu_frame_$n,
                        ${this.args.let {
                            if (!has_dots) it.size.toString() else {
                                "(" + (it.size-1) + " + ceu_dots_$n)"
                            }
                        }},
                        ceu_args_$n
                    );
                    CEU_CONTINUE_ON_THROW_MSG("${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tostr(false).let { it.replace('\n',' ').replace('"','\'').let { str -> str.take(45).let { if (str.length<=45) it else it+"...)" }}}}");
                } // CALL
                """
            }
        }
    }
}
