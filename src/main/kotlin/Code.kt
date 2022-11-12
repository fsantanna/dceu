// block: String -> current enclosing block for normal allocation
// ret: Pair<block,var> -> enclosing assignment with destination block and variable
fun fset(tk: Tk, ret: Pair<String, String>?, src: String): String {
    return if (ret == null) "" else fset(tk, ret.first, ret.second, src)
}
fun fset(tk: Tk, ret_block: String, ret_var: String, src: String): String {
    return """
        if ($src.tag == CEU_VALUE_TUPLE) {
            if ($src.tuple->block->depth > $ret_block->depth) {                
                ceu_throw = CEU_THROW_RUNTIME;
                strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${tk.pos.lin}, col ${tk.pos.col}) : set error : incompatible scopes", 256);
                continue;
            }
        }
        $ret_var = $src;

    """.trimIndent()
}

fun Expr.code(block: String?, set: Pair<String, String>?): String {
    return when (this) {
        is Expr.Block -> {
            val depth = if (block == null) 0 else "$block->depth+1"
            val es = this.es.mapIndexed { i, it ->
                it.code("(&ceu_mem->block_$n)", if (i == this.es.size - 1) set else null) + "\n"
            }.joinToString("")
            """
            { // BLOCK
                assert($depth < UINT8_MAX);
                ceu_mem->block_$n = (CEU_Block) { $depth, NULL };
                if (ceu_block_global == NULL) {
                    ceu_block_global = &ceu_mem->block_$n;
                }    
                ceu_mem->brk_$n = 0;
                while (!ceu_mem->brk_$n) {
                    ceu_mem->brk_$n = 1;
                    $es
                }
                ceu_block_free(&ceu_mem->block_$n);
                if (ceu_throw != CEU_THROW_NONE) {
                    continue;
                }
            }
            
            """.trimIndent()
        }
        is Expr.Dcl -> {
            val id = this.tk_.fromOp()
            """
            // DCL
            ceu_mem->$id = (CEU_Value) { CEU_VALUE_NIL };
            ceu_mem->_${id}_ = ${block!!};   // can't be static b/c recursion
            ${fset(this.tk, set, id)}            
                
            """.trimIndent()
        }
        is Expr.Set -> {
            val (scp, dst) = when (this.dst) {
                is Expr.Index -> Pair(
                    "ceu_mem->col_${this.dst.n}.tuple->block",
                    "ceu_mem->col_${this.dst.n}.tuple->buf[(int) ceu_mem->idx_${this.dst.n}.number]"
                )

                is Expr.Acc -> {
                    val id = this.dst.tk_.fromOp()
                    Pair("(ceu_mem->_${id}_)", "(ceu_mem->${id})")     // x = src / block of _x_
                }

                else -> error("bug found")
            }
            """
            { // SET
                ${this.dst.code(block, null)}
                ${this.src.code(block, Pair(scp, "ceu_mem->set_$n"))}
                $dst = ceu_mem->set_$n;
                ${fset(this.tk, set, "ceu_mem->set_$n")}
            }
                
            """.trimIndent()
        }
        is Expr.If -> """
            { // IF
                ${this.cnd.code(block, Pair(block!!, "ceu_mem->cnd_$n"))}
                if (ceu_mem->cnd_$n.tag != CEU_VALUE_BOOL) {
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.cnd.tk.pos.lin}, col ${this.cnd.tk.pos.col}) : if error : invalid condition", 256);
                    continue;
                }
                if (ceu_mem->cnd_$n.bool) {
                    ${this.t.code(block, set)}
                } else {
                    ${this.f.code(block, set)}
                }
            }
            
        """.trimIndent()
        is Expr.While -> """
            { // WHILE
                ceu_mem->brk_$n = 0;
                while (!ceu_mem->brk_$n) {
                    ceu_mem->brk_$n = 1;
                    ${this.cnd.code(block, Pair(block!!, "ceu_mem->cnd_$n"))}
                    if (ceu_mem->cnd_$n.tag != CEU_VALUE_BOOL) {
                        ceu_throw = CEU_THROW_RUNTIME;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.cnd.tk.pos.lin}, col ${this.cnd.tk.pos.col}) : while error : invalid condition", 256);
                        continue;
                    }
                    if (!ceu_mem->cnd_$n.bool) {
                        continue;
                    }
                    ${this.body.code(block, null)}
                    ceu_mem->brk_$n = 0;
                }
                if (ceu_throw != 0) {
                    continue;
                }
            }
                
            """.trimIndent()
        is Expr.Func -> {
            val (fld,tag) = if (this.isTask()) {
                Pair("task", "CEU_VALUE_TASK")
            } else {
                Pair("func", "CEU_VALUE_FUNC")
            }
            fun tsk (v: String): String {
                return if (this.isTask()) v else ""
            }
            """
            CEU_Value ceu_func_$n (${tsk("CEU_Coro* ceu_coro,")} CEU_Block* ceu_ret, int ceu_n, CEU_Value* ceu_args[]) {
                typedef struct {
                    ${this.mem()}
                } CEU_Func_$n;
                CEU_Func_$n _ceu_mem_;
                CEU_Func_$n* ceu_mem = &_ceu_mem_;
                CEU_Value ceu_$n;
                ${tsk("ceu_coro->status = CEU_CORO_STATUS_RESUMED;")}
                int ceu_brk_$n = 0;
                while (!ceu_brk_$n) {
                    ceu_brk_$n = 1;
                    ${tsk("switch (ceu_coro->pc) {\ncase 0: {\n")}
                    // ARGS
                    int ceu_i = 0;
                    ${this.args.map {"""
                        CEU_Value ${it.str} = { CEU_VALUE_NIL };
                        CEU_Block* _${it.str}_ = $block;   // TODO: should be the upcoming block
                        if (ceu_i < ceu_n) {
                            ${it.str} = *ceu_args[ceu_i];
                        }
                        ceu_i++;
                        
                        """.trimIndent()
                    }.joinToString("")}
                    ${this.body.code(null, Pair("ceu_ret", "ceu_$n"))}
                    ${tsk("}\n}\n")}
                }
                ${tsk("ceu_coro->status = CEU_CORO_STATUS_TERMINATED;")}
                return ceu_$n;
            }
            ${fset(this.tk, set, "((CEU_Value) { $tag, {.$fld=ceu_func_$n} })")}

            """.trimIndent()
        }
        is Expr.Throw -> """
            { // THROW
                ${this.ex.code(block, Pair(block!!, "ceu_mem->ex_$n"))}
                ${this.arg.code(block, Pair("ceu_block_global", "ceu_throw_arg"))}  // arg scope to be set in catch set
                if (ceu_mem->ex_$n.tag == CEU_VALUE_NUMBER) {
                    ceu_throw = ceu_mem->ex_$n.number;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception", 256);
                } else {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : invalid exception : expected number", 256);
                }
                continue;
            }
    
            """.trimIndent()
        is Expr.Catch -> {
            val scp = if (set != null) set.first else block!!
            """
            { // CATCH
                ${this.catch.code(block, Pair(block!!, "ceu_mem->catch_$n"))}
                assert(ceu_mem->catch_$n.tag == CEU_VALUE_NUMBER && "catch error : invalid exception : expected number");
                ceu_mem->brk_$n = 0;
                while (!ceu_mem->brk_$n) {
                    ceu_mem->brk_$n = 1;
                    ${this.body.code(block, set)}
                }
                if (ceu_throw != CEU_THROW_NONE) {          // pending throw
                    if (ceu_throw == ceu_mem->catch_$n.number) { // CAUGHT: reset throw, set arg
                        ${fset(this.tk, set, "ceu_throw_arg")}
                        if (ceu_throw_arg.tag==CEU_VALUE_TUPLE && ceu_block_global!=$scp) {
                            // assign ceu_throw_arg to set.first
                            ceu_block_move(ceu_throw_arg.tuple, ceu_block_global, $scp);
                        }
                        ceu_throw = CEU_THROW_NONE;
                    } else {                                // UNCAUGHT: escape to outer
                        continue;
                    }
                }
            }
            """.trimIndent()
        }
        is Expr.Spawn -> """
            { // SPAWN
                CEU_Value ceu_mem->task_$n;
                ${this.task.code(block, Pair(block!!, "ceu_mem->task_$n"))}
                if (ceu_mem->task_$n.tag != CEU_VALUE_TASK) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col}) : spawn error : expected task", 256);
                    continue;
                }
                CEU_Coro* ceu_mem->coro_$n = malloc(sizeof(CEU_Coro));
                *ceu_mem->coro_$n = (CEU_Coro) { CEU_CORO_STATUS_YIELDED, ceu_mem->task_$n.task, 0 };
                ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_CORO, {.coro=ceu_mem->coro_$n} })")}            
            }
            """.trimIndent()
        is Expr.Resume -> {
            assert(this.call.args.size <= 1) { "bug found : not implemented : multiple arguments to resume" }
            val (sets,args) = this.call.args.let {
                Pair(
                    it.mapIndexed { i,x -> x.code(block!!, Pair(block, "ceu_mem->arg_${i}_$n")) }.joinToString(""),
                    it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                )
            }
            """
            { // RESUME
                CEU_Value ceu_mem->coro_$n;
                ${this.call.f.code(block, Pair(block!!, "ceu_mem->coro_$n"))}
                if (ceu_mem->coro_$n.tag!=CEU_VALUE_CORO || ceu_mem->coro_$n.coro->status!=CEU_CORO_STATUS_YIELDED) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.call.f.tk.pos.lin}, col ${this.call.f.tk.pos.col}) : resume error : expected spawned task", 256);
                    continue;
                }
                $sets
                CEU_Value* ceu_args_$n[] = { $args };
                CEU_Value ceu_mem->ret_$n = ceu_mem->coro_$n.coro->task(
                    ceu_mem->coro_$n.coro,
                    ${if (set == null) block else set.first},
                    ${this.call.args.size},
                    ceu_args_$n
                );
                if (ceu_throw != CEU_THROW_NONE) {
                    continue;
                }
                ${fset(this.tk, set, "ceu_mem->ret_$n")}
            }
            """.trimIndent()
        }
        is Expr.Yield -> """
            { // YIELD
                CEU_Value ceu_mem->ret_$n;
                ${this.arg.code(block, Pair("ceu_ret","ceu_mem->ret_$n"))}
                ceu_coro->pc = $n;      // next resume
                ceu_coro->status = CEU_CORO_STATUS_YIELDED;
                return ceu_mem->ret_$n; // yield
            case $n:                    // resume here
                ceu_coro->status = CEU_CORO_STATUS_RESUMED;
                ${fset(this.tk, set, "(*ceu_args[0])")}
            }
            """.trimIndent()

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
                        "($id.number)$x"
                    }
                }
                Pair(ids,ret)
            }
            """
            {
                float ceu_f_$n (void) {
                    ${ids.map { "$it.tag = CEU_VALUE_NUMBER;\n" }.joinToString("") }
                    $body
                    return 0;
                }
                CEU_Value ceu_$n = { CEU_VALUE_NUMBER, {.number=ceu_f_$n()} };
                if (ceu_throw != CEU_THROW_NONE) {
                    continue;
                }
                ${fset(this.tk, set, "ceu_$n")}
            }
            """.trimIndent()
        }
        is Expr.Acc -> fset(this.tk, set, "(ceu_mem->"+this.tk_.fromOp()+")")
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
            """.trimIndent()
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
            val scp = if (set == null) block!! else set.first
            val args = this.args.mapIndexed { i, it ->
                // allocate in the same scope of set (set.first) or use default block
                it.code(block, Pair(scp, "ceu_mem->arg_${i}_$n"))
            }.joinToString("")
            """
            { // TUPLE
                $args
                CEU_Value ceu_sta_$n[${this.args.size}] = {
                    ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                };
                CEU_Value* ceu_dyn_$n = malloc(${this.args.size} * sizeof(CEU_Value));
                assert(ceu_dyn_$n != NULL);
                memcpy(ceu_dyn_$n, ceu_sta_$n, ${this.args.size} * sizeof(CEU_Value));
                CEU_Value_Tuple* ceu_$n = malloc(sizeof(CEU_Value_Tuple));
                assert(ceu_$n != NULL);
                *ceu_$n = (CEU_Value_Tuple) { $scp, $scp->tofree, ceu_dyn_$n, ${this.args.size} };
                $scp->tofree = ceu_$n;
                ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_TUPLE, {.tuple=ceu_$n} })")}
            }

            """.trimIndent()
        }
        is Expr.Index -> """
            //{ // INDEX    // (removed {} b/c set uses col[idx])
                ${this.col.code(block, Pair(block!!, "ceu_mem->col_$n"))}
                if (ceu_mem->col_$n.tag != CEU_VALUE_TUPLE) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : index error : expected tuple", 256);
                    continue;
                }
                                
                ${this.idx.code(block, Pair(block, "ceu_mem->idx_$n"))}
                if (ceu_mem->idx_$n.tag != CEU_VALUE_NUMBER) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : expected number", 256);
                    continue;
                }
                
                if (ceu_mem->col_$n.tag != CEU_VALUE_TUPLE) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : index error : expected tuple", 256);
                    continue;
                }
                
                if (ceu_mem->col_$n.tuple->n <= ceu_mem->idx_$n.number) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : out of bounds", 256);
                    break;
                }
    
                ${fset(this.tk, set, "ceu_mem->col_$n.tuple->buf[(int) ceu_mem->idx_$n.number]")}
            //}
            
        """.trimIndent()
        is Expr.Call -> {
            val (sets,args) = this.args.let {
                Pair (
                    it.mapIndexed { i,x -> x.code(block, Pair(block!!, "ceu_mem->arg_${i}_$n")) }.joinToString(""),
                    it.mapIndexed { i,_ -> "&ceu_mem->arg_${i}_$n" }.joinToString(",")
                )
            }
            """
            { // CALL
                ${this.f.code(block, Pair(block!!, "ceu_mem->f_$n"))}
                if (ceu_mem->f_$n.tag != CEU_VALUE_FUNC) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.f.tk.pos.lin}, col ${this.f.tk.pos.col}) : call error : expected function", 256);
                    break;
                }
                $sets
                CEU_Value* ceu_args_$n[] = { $args };
                ceu_mem->ret_$n = ceu_mem->f_$n.func(
                    ${if (set == null) block else set.first},
                    ${this.args.size},
                    ceu_args_$n
                );
                if (ceu_throw != CEU_THROW_NONE) {
                    break;
                }
                ${fset(this.tk, set, "ceu_mem->ret_$n")}
            }

            """.trimIndent()
        }
    }
}
