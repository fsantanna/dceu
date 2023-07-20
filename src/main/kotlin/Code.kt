package dceu

import java.lang.Integer.min

class Coder (val outer: Expr.Do, val ups: Ups, val vars: Vars, val clos: Clos) {
    val pres: MutableList<String> = mutableListOf()
    val code: String = outer.code()

    fun Expr.Do.toc (): String {
        return "ceu_block_${this.n}"
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

                val pre = """ // TYPE | ${this.dump()}
                    ${clos.protos_refs[this].cond { """
                        typedef struct {
                            ${clos.protos_refs[this]!!.map {
                                "CEU_Value ${it.id.str.id2c()};"
                            }.joinToString("")}
                        } CEU_Proto_Upvs_$n;                    
                    """ }}
                """ + """ // PROTO | ${this.dump()}
                    CEU_Value ceu_proto_$n (
                        CEU_Frame* ceu_frame,
                        int ceu_n,
                        CEU_Value ceu_args[]
                    ) {
                        CEU_Value ceu_acc;        
                        ${clos.protos_refs[this].cond { """
                            CEU_Proto_Upvs_$n* ceu_upvs = (CEU_Proto_Upvs_$n*) ceu_frame->closure->upvs.buf;                    
                        """ }}
                        ${this.args.map { (id,_) ->
                            val idc = id.str.id2c()
                            """
                            CEU_Value $idc;
                            CEU_Block* _${idc}_;
                            """
                        }.joinToString("")}
                        ${this.body.code()}
                        return ceu_acc;
                    }
                """

                val pos = """ // CLOSURE | ${this.dump()}
                CEU_Closure* ceu_closure_$n = ceu_closure_create (
                    ${up_blk.toc()},
                    ${if (clos.protos_noclos.contains(this)) "CEU_HOLD_IMMUTABLE" else "CEU_HOLD_FLEETING"},
                    ${if (up_blk == outer) "NULL" else "ceu_frame"},
                    ceu_proto_$n,
                    ${clos.protos_refs[this]?.size ?: 0}
                );
                assert(ceu_closure_$n != NULL);
                CEU_Value ceu_ret_$n = ((CEU_Value) { CEU_VALUE_CLOSURE, {.Dyn=(CEU_Dyn*)ceu_closure_$n} });
                ${assrc("ceu_ret_$n")}
                
                // UPVALS
                ${clos.protos_refs[this].cond {
                    it.map { dcl ->
                        val dcl_blk = vars.dcl_to_blk[dcl]!!
                        val idc = dcl.id.str.id2c()
                        val btw = ups
                            .all_until(this) { dcl_blk==it }
                            .filter { it is Expr.Proto }
                            .count() // other protos in between myself and dcl, so it its an upref (upv=2)
                        val upv = min(2, btw)
                        """
                        {
                            CEU_Value ceu_up = ${vars.id2c(dcl, upv).first};
                            assert(ceu_hold_chk_set_col((CEU_Dyn*)ceu_closure_$n, ceu_up));
                            ceu_gc_inc(ceu_up);
                            ((CEU_Proto_Upvs_$n*)ceu_closure_$n->upvs.buf)->${idc} = ceu_up;
                        }
                        """   // TODO: use this.body (ups.ups[this]?) to not confuse with args
                    }.joinToString("\n")
                }}
                """

                if (clos.protos_refs.containsKey(this)) {
                    pres.add(pre)
                    pos
                } else {
                    pre + pos
                }
            }
            is Expr.Do -> {
                val body = this.es.map { it.code() }.joinToString("")   // before defers[this] check
                val up = ups.pub[this]
                val bupc = up?.let { ups.first_block(it) }?.toc()
                val f_b = up?.let { ups.first_proto_or_block(it) }
                val (depth,bf,ptr) = when {
                    (f_b == null) -> Triple("1", "1", "{.frame=&_ceu_frame_}")
                    (f_b is Expr.Proto) -> Triple("ceu_frame->up_block->depth + 1", "1", "{.frame=ceu_frame}")
                    else -> Triple("(${bupc!!}->depth + 1)", "0", "{.block=${bupc!!}}")
                }
                val args = if (f_b !is Expr.Proto) emptySet() else f_b.args.map { it.first.str }.toSet()
                val dcls = vars.blk_to_dcls[this]!!.filter { it.init }
                    .filter { !GLOBALS.contains(it.id.str) }
                    .filter { !(f_b is Expr.Proto && args.contains(it.id.str)) }
                    .map    { it.id.str.id2c() }
                if (f_b is Expr.Do && dcls.isEmpty()) {
                    """
                    CEU_Block* ceu_block_$n = ${bupc!!};
                    // >>> block
                    $body
                    // <<< block
                    """
                } else {
                    """
                    { // BLOCK | ${this.dump()}
                        CEU_Block _ceu_block_$n = (CEU_Block) { $depth, $bf, $ptr, NULL };
                        CEU_Block* ceu_block_$n = &_ceu_block_$n; 
                        ${(f_b == null).cond { """
                            // main block varargs (...)
                            CEU_Tuple* ceu_tup_$n = ceu_tuple_create(ceu_block_$n, ceu_argc);
                            for (int i=0; i<ceu_argc; i++) {
                                CEU_Vector* vec = ceu_vector_from_c_string(ceu_block_$n, ceu_argv[i]);
                                assert(ceu_tuple_set(ceu_tup_$n, i, (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=(CEU_Dyn*)vec} }));
                            }
                            CEU_Value _dot__dot__dot_ = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ceu_tup_$n} };
                        """ }}
                        ${(f_b is Expr.Proto).cond { // initialize parameters from outer proto
                            f_b as Expr.Proto
                            val dots = (f_b.args.lastOrNull()?.first?.str == "...")
                            val args_n = f_b.args.size - 1
                            """
                            { // func args
                                ${f_b.args.filter { it.first.str!="..." }.mapIndexed { i,arg ->
                                    val idc = arg.first.str.id2c()
                                    """
                                    _${idc}_ = ceu_block_$n;
                                    if ($i < ceu_n) {
                                        if (!ceu_hold_chk_set(&ceu_block_$n->dyns, ceu_block_$n->depth, CEU_HOLD_FLEETING, ceu_args[$i])) {
                                            ceu_error1(ceu_block_$n, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : argument error : incompatible scopes");
                                        }
                                        $idc = ceu_args[$i];
                                        ceu_gc_inc($idc);
                                    } else {
                                        $idc = (CEU_Value) { CEU_VALUE_NIL };
                                    }
                                    """
                                }.joinToString("")}
                                ${dots.cond {
                                    val idc = f_b.args.last()!!.first.str.id2c()
                                    """
                                    int ceu_tup_n_$n = MAX(0,ceu_n-$args_n);
                                    CEU_Tuple* ceu_tup_$n = ceu_tuple_create(ceu_block_$n, ceu_tup_n_$n);
                                    for (int i=0; i<ceu_tup_n_$n; i++) {
                                        assert(ceu_tuple_set(ceu_tup_$n, i, ceu_args[$args_n+i]));
                                    }
                                    $idc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ceu_tup_$n} };
                                    ceu_gc_inc($idc);
                                """ }}
                            }
                            """ 
                        }}
                        ${dcls.filter { it != "_" }.map { """
                            CEU_Value $it = (CEU_Value) { CEU_VALUE_NIL };
                            CEU_Block* _${it}_ = NULL;
                        """ }.joinToString("")}
                        ${(f_b == null).cond{ pres.joinToString("") }}
                        
                        // >>> block
                        $body
                        // <<< block
                        
                        ${(f_b != null).cond {
                            val up1 = if (f_b is Expr.Proto) "ceu_frame->up_block" else bupc!!
                            """
                            // move up dynamic ceu_acc (return or error)
                            if (!ceu_hold_chk_set(&$up1->dyns, $up1->depth, CEU_HOLD_FLEETING, ceu_acc)) {
                                ceu_error1(ceu_block_$n, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : block escape error : incompatible scopes");
                            }
                            """
                        }}
                        ${dcls.filter { it != "_" }.map { """
                            if ($it.type > CEU_VALUE_DYNAMIC) {
                                ceu_gc_dec($it, ($it.Dyn->Any.hld_depth == ceu_block_$n->depth));
                            }
                        """ }.joinToString("")}
                        ${(f_b is Expr.Proto).cond { """
                            ${(f_b as Expr.Proto).args.map {
                                val idc = it.first.str.id2c()
                                """
                                if ($idc.type > CEU_VALUE_DYNAMIC) {
                                    ceu_gc_dec($idc, !(ceu_acc.type>CEU_VALUE_DYNAMIC && ceu_acc.Dyn==$idc.Dyn));
                                }
                                """
                            }.joinToString("")}
                        """}}
                        ceu_block_free(ceu_block_$n);
                    }
                    """
                }
            }
            is Expr.Dcl -> {
                val id = this.id.str
                val idc = id.id2c()
                val bupc = ups.first_block(this)!!.toc()
                val unused = false // TODO //sta.unused.contains(this) && (this.src is Expr.Closure)

                if (this.id.upv==1 && clos.vars_refs.none { it.second==this }) {
                    err(this.tk, "var error : unreferenced upvar")
                }

                """
                ${(this.init && this.src!=null && !unused).cond {
                    this.src!!.code() + """
                        assert(ceu_hold_chk_set(&$bupc->dyns, $bupc->depth, ${this.tmp_hold(this.tmp)}, ceu_acc));
                    """
                }}
                ${if (id == "_") "" else {
                    """
                    ${when {
                        !this.init -> ""
                        (this.src == null) -> ""
                        else -> "$idc = ceu_acc;"
                    }}
                    _${idc}_ = $bupc;
                    ceu_gc_inc(${idc});
                    ${assrc(idc)}
                    """
                }}
                """
            }
            is Expr.Set -> {
                """
                { // SET | ${this.dump()}
                    ${this.src.code()}
                    CEU_Value ceu_set_$n = ceu_acc;
                    ${this.dst.code()}
                    ${assrc("ceu_set_$n")}
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
                while (1) { // LOOP | ${this.dump()}
                    ${this.body.code()}
                }
                """
            is Expr.Break -> "break;"
            is Expr.Enum -> ""
            is Expr.Data -> ""
            is Expr.Pass -> "// PASS | ${this.dump()}\n" + this.e.code()
            is Expr.Drop -> this.e.code()

            is Expr.Nat -> {
                val body = vars.nat_to_str[this]!!
                when (this.tk_.tag) {
                    null   -> body + assrc("((CEU_Value){ CEU_VALUE_NIL })")
                    ":ceu" -> assrc(body)
                    else -> {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        assrc("((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} })")
                    }
                }
            }
            is Expr.Acc -> {
                val (blk,dcl) = vars.get(this)
                val (idc,_idc_) = vars.id2c(dcl, this.tk_.upv)
                when {
                    this.isdst() -> {
                        val bupc = ups.first_block(this)!!.toc()
                        val src = this.asdst_src()
                        if (dcl.id.upv > 0) {
                            err(tk, "set error : cannot reassign an upval")
                        }
                        """
                        { // ACC - SET
                            if (!ceu_hold_chk_set(&${_idc_}->dyns, ${_idc_}->depth, ${this.tmp_hold(dcl.tmp)}, $src)) {
                                ceu_error1($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                            }
                            ceu_gc_inc($src);
                            ceu_gc_dec($idc, 1);
                            $idc = $src;
                        }
                        """
                    }
                    this.isdrop() -> {
                        val bupc = ups.first_block(this)!!.toc()
                        """
                        { // ACC - DROP
                            CEU_Value ceu_$n = $idc;
                            CEU_Value args[1] = { ceu_$n };
                            CEU_Frame ceu_frame_$n = { NULL, $bupc };
                            ceu_assert2($bupc, ceu_drop_f(&ceu_frame_$n, 1, args), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                            ceu_gc_dec(ceu_$n, 0);
                            $idc = (CEU_Value) { CEU_VALUE_NIL };
                            ceu_acc = ceu_$n;
                        }
                        """
                    }
                    else -> assrc(idc)
                }
            }
            is Expr.Nil -> assrc("((CEU_Value) { CEU_VALUE_NIL })")
            is Expr.Tag -> assrc("((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_${this.tk.str.tag2c()}} })")
            is Expr.Bool -> assrc("((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} })")
            is Expr.Char -> assrc("((CEU_Value) { CEU_VALUE_CHAR, {.Char=${this.tk.str}} })")
            is Expr.Num -> assrc("((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} })")

            is Expr.Tuple -> {
                val bupc = ups.first_block(this)!!.toc()
                """
                { // TUPLE | ${this.dump()}
                    CEU_Tuple* ceu_tup_$n = ceu_tuple_create(${ups.first_block(this)!!.toc()}, ${this.args.size});
                    assert(ceu_tup_$n != NULL);
                    ${this.args.mapIndexed { i, it ->
                    it.code() + """
                        if (!ceu_tuple_set(ceu_tup_$n, $i, ceu_acc)) {
                            ceu_error1($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : tuple error : incompatible scopes");
                        }
                        """
                }.joinToString("")}
                    ${assrc("((CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ceu_tup_$n} })")}
                }
                """
            }
            is Expr.Vector -> {
                val bupc = ups.first_block(this)!!.toc()
                """
                { // VECTOR | ${this.dump()}
                    CEU_Vector* ceu_vec_$n = ceu_vector_create(${ups.first_block(this)!!.toc()});
                    assert(ceu_vec_$n != NULL);
                    ${this.args.mapIndexed { i, it ->
                    it.code() + """
                        if (!ceu_vector_set(ceu_vec_$n, $i, ceu_acc)) {
                            ceu_error1($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : vector error : incompatible scopes");
                        }
                        """
                }.joinToString("")}
                    ${assrc("((CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=(CEU_Dyn*)ceu_vec_$n} })")}
                }
                """
            }
            is Expr.Dict -> {
                val bupc = ups.first_block(this)!!.toc()
                """
                { // DICT | ${this.dump()}
                    CEU_Dict* ceu_dict_$n = ceu_dict_create(${ups.first_block(this)!!.toc()});
                    assert(ceu_dict_$n != NULL);
                    ${this.args.map { """
                        {
                            ${it.first.code()}
                            CEU_Value ceu_key_$n = ceu_acc;
                            ${it.second.code()}
                            CEU_Value ceu_val_$n = ceu_acc;
                            if (!ceu_dict_set(ceu_dict_$n, ceu_key_$n, ceu_val_$n)) {
                                ceu_error1($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : dict error : incompatible scopes");
                            }
                        }
                    """ }.joinToString("")}
                    ${assrc("((CEU_Value) { CEU_VALUE_DICT, {.Dyn=(CEU_Dyn*)ceu_dict_$n} })")}
                }
                """
            }
            is Expr.Index -> {
                val bupc = ups.first_block(this)!!.toc()
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
                    ceu_assert2($bupc, ceu_col_check(ceu_acc, ceu_idx_$n), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
                """ + when {
                    this.isdst() -> {
                        val src = this.asdst_src()
                        """
                        int ok = 1;
                        switch (ceu_acc.type) {
                            case CEU_VALUE_TUPLE:
                                ok = ceu_tuple_set(&ceu_acc.Dyn->Tuple, ceu_idx_$n.Number, $src);
                                break;
                            case CEU_VALUE_VECTOR:
                                ok = ceu_vector_set(&ceu_acc.Dyn->Vector, ceu_idx_$n.Number, $src);
                                break;
                            case CEU_VALUE_DICT: {
                                CEU_Value ceu_dict = ceu_acc;
                                ok = ceu_dict_set(&ceu_dict.Dyn->Dict, ceu_idx_$n, $src);
                                break;
                            }
                            default:
                                assert(0 && "bug found");
                        }
                        if (!ok) {
                            ceu_error1($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : set error : incompatible scopes");
                        }
                        """
                    }
                    this.isdrop() -> {
                        val bupc = ups.first_block(this)!!.toc()
                        """
                        {   // INDEX - DROP
                            CEU_Value ceu_col_$n = ceu_acc;
                            switch (ceu_col_$n.type) {
                                case CEU_VALUE_TUPLE:
                                    ${assrc("ceu_col_$n.Dyn->Tuple.buf[(int) ceu_idx_$n.Number]")}
                                    break;
                                case CEU_VALUE_VECTOR:
                                    ${assrc("""ceu_assert2($bupc, ceu_vector_get(&ceu_col_$n.Dyn->Vector, ceu_idx_$n.Number), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})")""")}
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
                            ceu_assert2($bupc, ceu_drop_f(&ceu_frame_$n, 1, args), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})");
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
                                ${assrc("""ceu_assert2($bupc, ceu_vector_get(&ceu_acc.Dyn->Vector, ceu_idx_$n.Number), "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col})")""")}
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
                val bupc = ups.first_block(this)!!.toc()
                val dots = this.args.lastOrNull()
                val has_dots = (dots!=null && dots is Expr.Acc && dots.tk.str=="...") && !this.closure.let { it is Expr.Acc && it.tk.str=="{{#}}" }
                val id_dots = if (!has_dots) "" else {
                    val (blk,dcl) = vars.get(dots as Expr.Acc)
                    vars.id2c(dcl, 0).first
                }
                //println(listOf(id_dots,has_dots,(dots!=null && dots is Expr.Acc && dots.tk.str=="..."),dots))

                """
                { // CALL | ${this.dump()}
                    ${this.closure.code()}
                    CEU_Value ceu_closure_$n = ceu_acc;
                    if (ceu_closure_$n.type != CEU_VALUE_CLOSURE) {
                        ceu_error1($bupc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : call error : expected function");
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

                    ceu_acc = ceu_frame_$n.closure->proto (
                        &ceu_frame_$n,
                        ${this.args.let {
                            if (!has_dots) it.size.toString() else {
                                "(" + (it.size-1) + " + ceu_dots_$n)"
                            }
                        }},
                        ceu_args_$n
                    );
                    ceu_assert2($bupc, ceu_acc, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : call error");
                } // CALL
                """
            }
        }
    }
}
