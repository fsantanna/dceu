// block: String -> current enclosing block for normal allocation
// ret: Pair<block,var> -> enclosing assignment with destination block and variable
fun fset(tk: Tk, ret: Pair<String, String>?, src: String): String {
    return if (ret == null) "" else fset(tk, ret.first, ret.second, src)
}
fun fset(tk: Tk, ret_block: String, ret_var: String, src: String): String {
    return """
        if ($src.tag == CEU_VALUE_TUPLE) {
            if ($src.tuple->dyn.block->depth > $ret_block->depth) {                
                ceu_throw = CEU_THROW_RUNTIME;
                strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${tk.pos.lin}, col ${tk.pos.col}) : set error : incompatible scopes", 256);
                continue;
            }
        }
        $ret_var = $src;
    """
}

fun Tk.dump (pre: String = ""): String {
    return "// $pre (${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})\n"
}

class CBlock (up_: CBlock?, ns_: Pair<Int,Int?>, syms_: MutableSet<String>) {
    val up = up_
    val ns = ns_
    val syms = syms_
    val defers = mutableListOf<String>()

    fun idFind (id: String): CBlock? {
        return when {
            this.syms.contains(id) -> this
            (this.up != null) -> this.up.idFind(id)
            else -> null
        }
    }
    fun idCheck (id: String, isDcl: Boolean, tk: Tk): CBlock? {
        val blk = this.idFind(id)
        when {
            (!isDcl && blk==null) -> err(tk, "access error : variable \"$id\" is not declared")
            ( isDcl && blk!=null) -> err(tk, "declaration error : variable \"$id\" is already declared")
        }
        return blk
    }
    fun id2mem (id: String, tk: Tk): String {
        val cblock = this.idCheck(id, false, tk)!!
        return "(ceu_mem_${cblock.ns.first}->$id)"
    }
    fun block (): String {
        return "(&ceu_mem->block_${this.ns.second!!})"
    }
}

fun Expr.code(cblock: CBlock, set: Pair<String, String>?): String {
    return when (this) {
        is Expr.Block -> {
            val depth = if (cblock.ns.second == null) 0 else "${cblock.block()}->depth+1"
            val newcblock = CBlock(cblock, Pair(cblock.ns.first,n), mutableSetOf())
            val es = this.es.mapIndexed { i, it ->
                it.code(newcblock, if (i == this.es.size - 1) set else null) + "\n"
            }.joinToString("")
            """
            { // BLOCK
                assert($depth <= UINT8_MAX);
                ceu_mem->block_$n = (CEU_Block) { $depth, NULL, NULL };
                if (ceu_block_global == NULL) {
                    ceu_block_global = &ceu_mem->block_$n;
                }    
                ceu_mem->brk_$n = 0;
                while (!ceu_mem->brk_$n) {
                    ceu_mem->brk_$n = 1;
                    $es
                }
                { // DEFERS
                    ${newcblock.defers.reversed().joinToString("")}
                }
                ceu_block_free(&ceu_mem->block_$n);
                if (ceu_throw != CEU_THROW_NONE) {
                    continue;
                }
            }
            """
        }
        is Expr.Dcl -> {
            val id = this.tk_.fromOp().noSpecial()
            cblock.idCheck(id, true, this.tk)
            cblock.syms.add(id)
            cblock.syms.add("_${id}_")
            val (x,_x_) = Pair(cblock.id2mem(id,this.tk),cblock.id2mem("_${id}_",this.tk))
            """
            // DCL
            $x = (CEU_Value) { CEU_VALUE_NIL };
            $_x_ = ${cblock.block()};   // can't be static b/c recursion
            ${fset(this.tk, set, id)}                
            """
        }
        is Expr.Set -> {
            val (scp, dst) = when (this.dst) {
                is Expr.Index -> Pair(
                    "ceu_mem->col_${this.dst.n}.tuple->dyn.block",
                    "((CEU_Value*)ceu_mem->col_${this.dst.n}.tuple->mem)[(int) ceu_mem->idx_${this.dst.n}.number]"
                )

                is Expr.Acc -> {
                    val id = this.dst.tk_.fromOp().noSpecial()
                    Pair("(ceu_mem->_${id}_)", "(ceu_mem->${id})")     // x = src / block of _x_
                }

                else -> error("bug found")
            }
            """
            { // SET
                CEU_Value ceu_$n;
                ${this.dst.code(cblock, null)}
                ${this.src.code(cblock, Pair(scp, "ceu_$n"))}
                $dst = ceu_$n;
                ${fset(this.tk, set, "ceu_$n")}
            }
            """
        }
        is Expr.If -> """
            { // IF
                CEU_Value ceu_cnd_$n;
                ${this.cnd.code(cblock, Pair(cblock.block(), "ceu_cnd_$n"))}
                int nok = (ceu_cnd_$n.tag==CEU_VALUE_NIL || (ceu_cnd_$n.tag==CEU_VALUE_BOOL && !ceu_cnd_$n.bool));
                if (!nok) {
                    ${this.t.code(cblock, set)}
                } else {
                    ${this.f.code(cblock, set)}
                }
            }
            """
        is Expr.While -> """
            { // WHILE
                ceu_mem->brk_$n = 0;
                while (!ceu_mem->brk_$n) {
                    ceu_mem->brk_$n = 1;
                    CEU_Value ceu_cnd_$n;
                    ${this.cnd.code(cblock, Pair(cblock.block(), "ceu_cnd_$n"))}
                    int nok = (ceu_cnd_$n.tag==CEU_VALUE_NIL || (ceu_cnd_$n.tag==CEU_VALUE_BOOL && !ceu_cnd_$n.bool));
                    if (nok) {
                        continue;
                    }
                    ${this.body.code(cblock, null)}
                    ceu_mem->brk_$n = 0;
                }
                if (ceu_throw != 0) {
                    continue;
                }
            }
            """
        is Expr.Func -> {
            val newcblock = CBlock(cblock, Pair(this.n,null), this.args.map { it.str }.toMutableSet())
            fun xtask (v: String): String {
                return if (this.isTask()) v else ""
            }
            fun xfunc (v: String): String {
                return if (!this.isTask()) v else ""
            }
            """
            typedef struct {
                ${this.args.map {
                    """
                    CEU_Value ${it.str};
                    CEU_Block* _${it.str}_;
                    """
                }.joinToString("")}
                ${this.body.mem()}
            } CEU_Func_$n;
            CEU_Value ceu_func_$n (${xtask("CEU_Value_Coro* ceu_coro,")} CEU_Block* ceu_ret, int ceu_n, CEU_Value* ceu_args[]) {
                ${xfunc("""
                    CEU_Func_$n _ceu_mem_;
                    CEU_Func_$n* ceu_mem = &_ceu_mem_;
                """)}
                ${xtask("""
                    CEU_Func_$n* ceu_mem = (CEU_Func_$n*) ceu_coro->mem;
                """)}
                CEU_Func_$n* ceu_mem_$n = ceu_mem;
                CEU_Value ceu_$n;
                ${xtask("ceu_coro->status = CEU_CORO_STATUS_RESUMED;")}
                int ceu_brk_$n = 0;
                while (!ceu_brk_$n) {  // FUNC
                    ceu_brk_$n = 1;
                    ${xtask("switch (ceu_coro->pc) {\ncase 0: {\n")}
                    { // ARGS
                        int ceu_i = 0;
                        ${this.args.map {"""
                            ceu_mem->_${it.str}_ = NULL; // TODO: create Block at Func top-level
                            if (ceu_i < ceu_n) {
                                ${newcblock.id2mem(it.str,it)} = *ceu_args[ceu_i];
                            } else {
                                ${newcblock.id2mem(it.str, it)} = (CEU_Value) { CEU_VALUE_NIL };
                            }
                            ceu_i++;
                            """
                        }.joinToString("")}
                    }
                    // BODY
                    ${this.body.code(newcblock, Pair("ceu_ret", "ceu_$n"))}
                    ${xtask("}\n}\n")}
                }
                ${xtask("ceu_coro->status = CEU_CORO_STATUS_TERMINATED;")}
                return ceu_$n;
            }
            ${xfunc(fset(this.tk, set, "((CEU_Value) { CEU_VALUE_FUNC, {.func=ceu_func_$n} })"))}
            ${xtask("""
                static CEU_Value_Task ceu_task_$n;
                ceu_task_$n = (CEU_Value_Task) { ceu_func_$n, sizeof(CEU_Func_$n) };
                ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_TASK, {.task=&ceu_task_$n} })")}
            """)}
            """
        }
        is Expr.Catch -> {
            val scp = if (set != null) set.first else cblock.block()
            """
            { // CATCH
                ${this.catch.code(cblock, Pair(cblock.block(), "ceu_mem->catch_$n"))}
                assert(ceu_mem->catch_$n.tag == CEU_VALUE_NUMBER && "catch error : invalid exception : expected number");
                ceu_mem->brk_$n = 0;
                while (!ceu_mem->brk_$n) {
                    ceu_mem->brk_$n = 1;
                    ${this.body.code(cblock, set)}
                }
                if (ceu_throw != CEU_THROW_NONE) {          // pending throw
                    if (ceu_throw == ceu_mem->catch_$n.number) { // CAUGHT: reset throw, set arg
                        ${fset(this.tk, set, "ceu_throw_arg")}
                        if (ceu_block_global != $scp) {
                            // assign ceu_throw_arg to set.first
                            switch (ceu_throw_arg.tag) {
                                case CEU_VALUE_TUPLE:
                                    ceu_block_move((CEU_Dynamic*)ceu_throw_arg.tuple, ceu_block_global, $scp);
                                    break;
                                case CEU_VALUE_CORO:
                                    ceu_block_move((CEU_Dynamic*)ceu_throw_arg.coro, ceu_block_global, $scp);
                                    break;
                            }
                        }
                        ceu_throw = CEU_THROW_NONE;
                    } else {                                // UNCAUGHT: escape to outer
                        continue;
                    }
                }
            }
            """
        }
        is Expr.Throw -> """
            { // THROW
                CEU_Value ceu_ex_$n;
                ${this.ex.code(cblock, Pair(cblock.block(), "ceu_ex_$n"))}
                ${this.arg.code(cblock, Pair("ceu_block_global", "ceu_throw_arg"))}  // arg scope to be set in catch set
                if (ceu_ex_$n.tag == CEU_VALUE_NUMBER) {
                    ceu_throw = ceu_ex_$n.number;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception", 256);
                } else {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : invalid exception : expected number", 256);
                }
                continue;
            }
            """
        is Expr.Spawn -> {
            val scp = if (set == null) cblock.block() else set.first
            """
            { // SPAWN
                CEU_Value ceu_task_$n;
                ${this.task.code(cblock, Pair(cblock.block(), "ceu_task_$n"))}
                if (ceu_task_$n.tag != CEU_VALUE_TASK) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col}) : spawn error : expected task", 256);
                    continue;
                }
                CEU_Value_Coro* ceu_$n = malloc(sizeof(CEU_Value_Coro) + (ceu_task_$n.task->size));
                assert(ceu_$n != NULL);
                *ceu_$n = (CEU_Value_Coro) { {$scp->tofree,$scp}, {NULL,NULL}, CEU_CORO_STATUS_YIELDED, ceu_task_$n.task, 0 };
                $scp->tofree = (CEU_Dynamic*) ceu_$n;
                ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_CORO, {.coro=ceu_$n} })")}            
            }
            """
        }
        is Expr.Bcast -> """
            { // BCAST
                CEU_Value ceu_arg_$n;
                ${this.arg.code(cblock, Pair(cblock.block(), "ceu_arg_$n"))}
                ceu_bcast(${cblock.block()}, &ceu_arg_$n);
            }
            """
        is Expr.Resume -> {
            val (sets,args) = this.call.args.let {
                Pair(
                    it.mapIndexed { i,x -> x.code(cblock, Pair(cblock.block(), "ceu_mem->arg_${i}_$n")) }.joinToString(""),
                    it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                )
            }
            """
            { // RESUME
                { // SETS
                    $sets
                }
                CEU_Value ceu_coro_$n;
                ${this.call.f.code(cblock, Pair(cblock.block(), "ceu_coro_$n"))}
                if (ceu_coro_$n.tag!=CEU_VALUE_CORO || ceu_coro_$n.coro->status!=CEU_CORO_STATUS_YIELDED) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.call.f.tk.pos.lin}, col ${this.call.f.tk.pos.col}) : resume error : expected yielded task", 256);
                    continue;
                }
                CEU_Value* ceu_args_$n[] = { $args };
                CEU_Value ceu_ret_$n = ceu_coro_$n.coro->task->func(
                    ceu_coro_$n.coro,
                    ${if (set == null) cblock.block() else set.first},
                    ${this.call.args.size},
                    ceu_args_$n
                );
                if (ceu_throw != CEU_THROW_NONE) {
                    continue;
                }
                ${fset(this.tk, set, "ceu_ret_$n")}
            }
            """
        }
        is Expr.Yield -> """
            { // YIELD
                CEU_Value ceu_$n;
                ${this.arg.code(cblock, Pair("ceu_ret","ceu_$n"))}
                ceu_coro->pc = $n;      // next resume
                ceu_coro->status = CEU_CORO_STATUS_YIELDED;
                return ceu_$n; // yield
            case $n:                    // resume here
                assert(ceu_n <= 1 && "bug found : not implemented : multiple arguments to resume");
                ceu_coro->status = CEU_CORO_STATUS_RESUMED;
                ${fset(this.tk, set, "(*ceu_args[0])")}
            }
            """
        is Expr.Defer -> { cblock.defers.add(this.body.code(cblock,null)); "" }
        is Expr.Nat -> {
            val (ids,body) = this.tk.str.drop(1).dropLast(1).let {
                var ret = ""
                var i = 0

                var lin = 1
                var col = 1
                fun read (): Char {
                    //assert(i < it.length) { "bug found" }
                    if (i >= it.length) {
                        err(tk, "native error : (lin $lin, col $col) : unterminated token")
                    }
                    val x = it[i++]
                    if (x == '\n') {
                        lin++; col=0
                    } else {
                        col++
                    }
                    return x
                }

                val ids = mutableListOf<String>()
                while (i < it.length) {
                    ret += if (it[i] != '$') read() else {
                        read()
                        val (l,c) = Pair(lin,col)
                        var id = ""
                        var x = read()
                        while (x.isLetterOrDigit() || x=='_') {
                            id += x
                            x = read()
                        }
                        if (id.length == 0) {
                            err(tk, "native error : (lin $l, col $c) : invalid identifier")
                        }
                        ids.add(id)
                        "(ceu_mem->$id.number)$x"
                    }
                }
                Pair(ids,ret)
            }
            """
            { // NATIVE
                float ceu_f_$n (void) {
                    ${ids.map { "ceu_mem->$it.tag = CEU_VALUE_NUMBER;\n" }.joinToString("") }
                    $body
                    return 0;
                }
                CEU_Value ceu_$n = { CEU_VALUE_NUMBER, {.number=ceu_f_$n()} };
                if (ceu_throw != CEU_THROW_NONE) {
                    continue;
                }
                ${fset(this.tk, set, "ceu_$n")}
            }
            """
        }
        is Expr.Acc -> this.tk.dump("ACC") + fset(this.tk, set, cblock.id2mem(this.tk_.fromOp().noSpecial(), this.tk))
        is Expr.Nil -> fset(this.tk, set, "((CEU_Value) { CEU_VALUE_NIL })")
        is Expr.Tag -> {
            val tag = this.tk.str.drop(1)
            """
                #ifndef CEU_TAG_$tag
                #define CEU_TAG_$tag //__COUNTER__
                static CEU_Tags ceu_tag_$tag = { "@$tag", NULL };
                ceu_tag_$tag.next = CEU_TAGS;
                CEU_TAGS = &ceu_tag_$tag;
                CEU_TAGS_MAX++;
                #endif
                //{fset(this.tk, set, "((CEU_Value) { CEU_VALUE_TAG, {._tag_=CEU_TAG_$tag} })")}
                ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_TAG, {._tag_=ceu_tag_from_string(\"@$tag\")} })")}
            """
        }
        is Expr.Bool -> {
            fset(
                this.tk,
                set,
                "((CEU_Value) { CEU_VALUE_BOOL, {.bool=${if (this.tk.str == "true") 1 else 0}} })"
            )
        }
        is Expr.Num -> fset(this.tk, set, "((CEU_Value) { CEU_VALUE_NUMBER, {.number=${this.tk.str}} })")
        is Expr.Tuple -> {
            assert(this.args.size <= 256) { "bug found" }
            val scp = if (set == null) cblock.block() else set.first
            val args = this.args.mapIndexed { i, it ->
                // allocate in the same scope of set (set.first) or use default block
                it.code(cblock, Pair(scp, "ceu_mem->arg_${i}_$n"))
            }.joinToString("")
            """
            { // TUPLE /* ${this.tostr()} */
                $args
                CEU_Value ceu_sta_$n[${this.args.size}] = {
                    ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                };
                CEU_Value_Tuple* ceu_$n = malloc(sizeof(CEU_Value_Tuple) + ${this.args.size} * sizeof(CEU_Value));
                assert(ceu_$n != NULL);
                *ceu_$n = (CEU_Value_Tuple) { {$scp->tofree,$scp}, ${this.args.size} };
                memcpy(ceu_$n->mem, ceu_sta_$n, ${this.args.size} * sizeof(CEU_Value));
                $scp->tofree = (CEU_Dynamic*) ceu_$n;
                ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_TUPLE, {.tuple=ceu_$n} })")}
            }
            """
        }
        is Expr.Index -> """
            { // INDEX /* ${this.tostr()} */
                { // COL
                    ${this.col.code(cblock, Pair(cblock.block(), "ceu_mem->col_$n"))}
                    if (ceu_mem->col_$n.tag != CEU_VALUE_TUPLE) {                
                        ceu_throw = CEU_THROW_RUNTIME;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : index error : expected tuple", 256);
                        continue;
                    }
                }
                CEU_Value ceu_idx_$n;
                { // IDX        
                    ${this.idx.code(cblock, Pair(cblock.block(), "ceu_mem->idx_$n"))}
                    if (ceu_mem->idx_$n.tag != CEU_VALUE_NUMBER) {                
                        ceu_throw = CEU_THROW_RUNTIME;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : expected number", 256);
                        continue;
                    }
                }
                { // OK
                    if (ceu_mem->col_$n.tuple->n <= ceu_mem->idx_$n.number) {                
                        ceu_throw = CEU_THROW_RUNTIME;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : out of bounds", 256);
                        break;
                    }    
                    ${fset(this.tk, set, "((CEU_Value*)ceu_mem->col_$n.tuple->mem)[(int) ceu_mem->idx_$n.number]")}
                }
            }
            """
        is Expr.Call -> {
            val (sets,args) = this.args.let {
                Pair (
                    it.mapIndexed { i,x -> x.code(cblock, Pair(cblock.block(), "ceu_mem->arg_${i}_$n")) }.joinToString(""),
                    it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                )
            }
            """
            { // CALL
                { // SETS
                    $sets
                }
                CEU_Value ceu_f_$n;
                ${this.f.code(cblock, Pair(cblock.block(), "ceu_f_$n"))}
                if (ceu_f_$n.tag != CEU_VALUE_FUNC) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.f.tk.pos.lin}, col ${this.f.tk.pos.col}) : call error : expected function", 256);
                    break;
                }
                CEU_Value* ceu_args_$n[] = { $args };
                CEU_Value ceu_$n = ceu_f_$n.func(
                    ${if (set == null) cblock.block() else set.first},
                    ${this.args.size},
                    ceu_args_$n
                );
                if (ceu_throw != CEU_THROW_NONE) {
                    break;
                }
                ${fset(this.tk, set, "ceu_$n")}
            }
            """
        }
    }
}
