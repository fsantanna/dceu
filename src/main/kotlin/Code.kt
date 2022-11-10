class Coder (parser_: Parser) {
    val parser = parser_

    // block: String -> current enclosing block for normal allocation
    // set: Pair<scope,dst> -> enclosing assignment with block and destination
    fun fset(tk: Tk, set: Pair<String, String>?, src: String): String {
        return if (set == null) "" else fset(tk, set.first, set.second, src)
    }
    fun fset(tk: Tk, scope: String, dst: String, src: String): String {
        return """
            if ($src.tag == CEU_VALUE_TUPLE) {
                if ($src.tuple->block->depth > $scope->depth) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : set error : incompatible scopes", ${tk.pos.lin}, ${tk.pos.col});
                    break;
                }
            }
            $dst = $src;
    
        """.trimIndent()
    }

    fun Expr.code(block: String, set: Pair<String, String>?): String {
        return when (this) {
            is Expr.Do -> {
                val depth = if (block == "") 0 else "$block->depth+1"
                val scp = if (set != null) set.first else "(&ceu_block_$n)"
                """
            { // DO
                assert($depth < UINT8_MAX);
                CEU_Block ceu_block_$n = { $depth, NULL };
                if (ceu_block_global == NULL) {
                    ceu_block_global = &ceu_block_$n;
                }

                CEU_Throw ceu_catch_n_$n; {
                    ${
                    if (this.catch == null) "ceu_catch_n_$n = CEU_THROW_NONE;" else {
                        """
                            CEU_Value ceu_catch_$n;
                            ${this.catch.code(block, Pair(block, "ceu_catch_$n"))}
                            assert(ceu_catch_$n.tag == CEU_VALUE_NUMBER && "catch error : invalid exception : expected number");
                            ceu_catch_n_$n = ceu_catch_$n.number;
                        """.trimIndent()
                    }
                }
                }

                do {
                    ${this.es.code("(&ceu_block_$n)", set)}
                } while (0);
                ceu_block_free(&ceu_block_$n);
                if (ceu_throw != CEU_THROW_NONE) {          // pending throw
                    if (ceu_throw == ceu_catch_n_$n) {      // CAUGHT: reset throw, set arg
                        ${fset(this.tk, set, "ceu_throw_arg")}
                        if (ceu_throw_arg.tag==CEU_VALUE_TUPLE && ceu_block_global!=$scp) {
                            // assign ceu_throw_arg to set.first
                            ceu_block_move(ceu_throw_arg.tuple, ceu_block_global, $scp);
                        }
                        ceu_throw = CEU_THROW_NONE;
                    } else {                                // UNCAUGHT: escape to outer
                        break;
                    }
                }
            }
            
        """.trimIndent()
            }
            is Expr.Dcl -> """
            // DCL
            CEU_Value ${this.tk.str} = { CEU_VALUE_NIL };
            CEU_Block* _${this.tk.str}_ = $block;   // can't be static b/c recursion
            ${fset(this.tk, set, this.tk.str)}            
                
        """.trimIndent()
            is Expr.Set -> {
                val (scp, dst) = when (this.dst) {
                    is Expr.Index -> Pair(
                        "ceu_col_${this.dst.n}.tuple->block",
                        "ceu_col_${this.dst.n}.tuple->buf[(int) ceu_idx_${this.dst.n}.number]"
                    )

                    is Expr.Acc -> Pair(
                        "_${this.dst.tk.str}_",  // x = src / scope of _x_
                        this.dst.tk.str
                    )

                    else -> error("bug found")
                }
                """
                { // SET
                    CEU_Value ceu_$n;
                    ${this.dst.code(block, null)}
                    ${this.src.code(block, Pair(scp, "ceu_$n"))}
                    $dst = ceu_$n;
                    ${fset(this.tk, set, "ceu_$n")}
                }
                    
                """.trimIndent()
                //Pair(s1+pre+s2+pos, "ceu_$n")
            }
            is Expr.If -> """
            { // IF
                CEU_Value ceu_cnd_$n;
                ${this.cnd.code(block, Pair(block, "ceu_cnd_$n"))}
                int ceu_ret_$n; {
                    switch (ceu_cnd_$n.tag) {
                        case CEU_VALUE_NIL:  { ceu_ret_$n=0; break; }
                        case CEU_VALUE_BOOL: { ceu_ret_$n=ceu_cnd_$n.bool; break; }
                        default: {                
                            ceu_throw = CEU_THROW_RUNTIME;
                            snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : if error : invalid condition", ${this.cnd.tk.pos.lin}, ${this.cnd.tk.pos.col});
                            break; // need to break again below
                        }
                    }
                    if (ceu_throw != CEU_THROW_NONE) {
                        break;  // break in switch above wont escape
                    }
                }
                if (ceu_ret_$n) {
                    ${this.t.code(block, set)}
                } else {
                    ${this.f.code(block, set)}
                }
            }
            
        """.trimIndent()
            is Expr.Loop -> """
            while (1) { // LOOP
                ${this.body.code(block, set)}
            }
                
        """.trimIndent()
            is Expr.Func -> """
                CEU_Value ceu_func_$n (CEU_Block* ceu_block, CEU_Block* ceu_scope, int ceu_n, CEU_Value* ceu_args[]) {
                    int ceu_i = 0;
                    ${
                        this.args.map {
                            """
                            CEU_Value ${it.str} = { CEU_VALUE_NIL };
                            if (ceu_i < ceu_n) {
                                ${it.str} = *ceu_args[ceu_i];
                            }
                            ceu_i++;
                            """.trimIndent()
                        }.joinToString("")
                    }
                    CEU_Value ceu_$n;
                    do {
                        ${this.body.code("ceu_block", Pair("ceu_scope", "ceu_$n"))}
                    } while (0);
                    return ceu_$n;
                }
                ${fset(this.tk, set, "((CEU_Value) { CEU_VALUE_FUNC, {.func=ceu_func_$n} })")}            
    
            """.trimIndent()
            is Expr.Throw -> """
            { // THROW
                CEU_Value ceu_ex_$n;
                ${this.ex.code(block, Pair(block, "ceu_ex_$n"))}
                ${this.arg.code(block, Pair("ceu_block_global", "ceu_throw_arg"))}  // arg scope to be set in catch set
                if (ceu_ex_$n.tag == CEU_VALUE_NUMBER) {
                    ceu_throw = ceu_ex_$n.number;
                    snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : throw error : uncaught exception", ${this.tk.pos.lin}, ${this.tk.pos.col});
                } else {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : throw error : invalid exception : expected number", ${this.tk.pos.lin}, ${this.tk.pos.col});
                }
                break;
            }
    
        """.trimIndent()

            is Expr.Nat -> {
                val body = this.tk.str.drop(1).dropLast(1).let {
                    var ret = ""
                    var i = 0

                    var lin = 1
                    var col = 1
                    fun read (): Char {
                        //assert(i < it.length) { "bug found" }
                        if (i >= it.length) {
                            parser.lexer.err(tk, "native error : (lin $lin, col $col) : unterminated token")
                        }
                        val x = it[i++]
                        if (x == '\n') {
                            lin++; col=0
                        } else {
                            col++
                        }
                        return x
                    }

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
                                parser.lexer.err(tk, "native error : (lin $l, col $c) : invalid identifier")
                            }
                            "($id.number)$x"
                        }
                    }
                    ret
                }
                """
            {
                float ceu_f_$n (void) {
                    $body
                    return 0;
                }
                CEU_Value ceu_$n = { CEU_VALUE_NUMBER, {.number=ceu_f_$n()} };
                if (ceu_throw != CEU_THROW_NONE) {
                    break;
                }
                ${fset(this.tk, set, "ceu_$n")}
            }
            """.trimIndent()
            }
            is Expr.Acc -> fset(this.tk, set, this.tk.str)
            is Expr.Nil -> fset(this.tk, set, "((CEU_Value) { CEU_VALUE_NIL })")
            is Expr.Bool -> fset(
                this.tk,
                set,
                "((CEU_Value) { CEU_VALUE_BOOL, {.bool=${if (this.tk.str == "true") 1 else 0}} })"
            )
            is Expr.Num -> fset(this.tk, set, "((CEU_Value) { CEU_VALUE_NUMBER, {.number=${this.tk.str}} })")
            is Expr.Tuple -> {
                assert(this.args.size <= 256) { "bug found" }
                val scp = if (set == null) block else set.first
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code(block, Pair(scp, "ceu_${i}_$n"))
                }.joinToString("")
                """
                { // TUPLE
                    ${this.args.mapIndexed { i, _ -> "CEU_Value ceu_${i}_$n;\n" }.joinToString("")}
                    $args
                    CEU_Value ceu_sta_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_${i}_$n" }.joinToString(",")}
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
                CEU_Value ceu_col_$n;
                ${this.col.code(block, Pair(block, "ceu_col_$n"))}
                if (ceu_col_$n.tag != CEU_VALUE_TUPLE) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : index error : expected tuple", ${this.col.tk.pos.lin}, ${this.col.tk.pos.col});
                    break;
                }
                                
                CEU_Value ceu_idx_$n;
                ${this.idx.code(block, Pair(block, "ceu_idx_$n"))}
                if (ceu_idx_$n.tag != CEU_VALUE_NUMBER) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : index error : expected number", ${this.idx.tk.pos.lin}, ${this.idx.tk.pos.col});
                    break;
                }
                
                if (ceu_col_$n.tag != CEU_VALUE_TUPLE) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : index error : expected tuple", ${this.col.tk.pos.lin}, ${this.col.tk.pos.col});
                    break;
                }
                
                if (ceu_col_$n.tuple->n <= ceu_idx_$n.number) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : index error : out of bounds", ${this.idx.tk.pos.lin}, ${this.idx.tk.pos.col});
                    break;
                }

                ${fset(this.tk, set, "ceu_col_$n.tuple->buf[(int) ceu_idx_$n.number]")}
            //}
            
        """.trimIndent()
            is Expr.Call -> """
            { // CALL
                CEU_Value ceu_f_$n;
                ${this.f.code(block, Pair(block, "ceu_f_$n"))}
                if (ceu_f_$n.tag != CEU_VALUE_FUNC) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    snprintf(ceu_throw_msg, 256, "anon : (lin %d, col %d) : call error : expected function", ${this.f.tk.pos.lin}, ${this.f.tk.pos.col});
                    break;
                }
                ${
                    this.args.mapIndexed { i, _ ->
                        "CEU_Value ceu_${i}_$n;\n"
                    }.joinToString("")
                }
                ${
                    this.args.mapIndexed { i, it ->
                        it.code(block, Pair(block, "ceu_${i}_$n"))
                    }.joinToString("")
                }
                CEU_Value* ceu_args_$n[] = { ${this.args.mapIndexed { i, _ -> "&ceu_${i}_$n" }.joinToString(",")} };
                CEU_Value ceu_$n = ceu_f_$n.func(
                    $block,
                    ${if (set == null) block else set.first},
                    ${this.args.size},
                    ceu_args_$n
                );
                if (ceu_throw != CEU_THROW_NONE) {
                    break;
                }
                ${fset(this.tk, set, "ceu_$n")}
            }

        """.trimIndent()
        }
    }

    fun List<Expr>.code(block: String, set: Pair<String, String>?): String {
        return this.mapIndexed { i, it ->
            it.code(block, if (i == this.size - 1) set else null) + "\n"
        }.joinToString("")
    }

    fun expr (es: Expr.Do): String {
        return """
            #include <stdio.h>
            #include <stdlib.h>
            #include <stdint.h>
            #include <string.h>
            #include <assert.h>
            #include <stdarg.h>
    
            typedef enum CEU_VALUE {
                CEU_VALUE_NIL,
                CEU_VALUE_BOOL,
                CEU_VALUE_NUMBER,
                CEU_VALUE_TUPLE,
                CEU_VALUE_FUNC
            } CEU_VALUE;
            
            struct CEU_Value;
            struct CEU_Block;
            struct CEU_Stack;
            
            typedef struct CEU_Value_Tuple {
                struct CEU_Block* block;        // compare on set
                struct CEU_Value_Tuple* nxt;    // next in block->tofree
                struct CEU_Value* buf;
                uint8_t n;
            } CEU_Value_Tuple;
            typedef struct CEU_Value {
                int tag;
                union {
                    //void nil;
                    int bool;
                    float number;
                    CEU_Value_Tuple* tuple;
                    struct CEU_Value (*func) (struct CEU_Block* block, struct CEU_Block* scope, int n, struct CEU_Value* args[]);
                };
            } CEU_Value;
            
            typedef struct CEU_Block {
                uint8_t depth;              // compare on set
                CEU_Value_Tuple* tofree;    // list of allocated tuples to free on exit
            } CEU_Block;
            void ceu_block_free (CEU_Block* block) {
                while (block->tofree != NULL) {
                    CEU_Value_Tuple* cur = block->tofree;
                    block->tofree = block->tofree->nxt;
                    free(cur->buf);
                    free(cur);
                }
            }
            void ceu_block_move (CEU_Value_Tuple* V, CEU_Block* FR, CEU_Block* TO) {
                CEU_Value_Tuple* prv = NULL;
                CEU_Value_Tuple* cur = FR->tofree;
                while (cur != NULL) {
                    if (cur == V) {
                        if (prv == NULL) {
                            FR->tofree = NULL;
                        } else {
                            prv->nxt = cur->nxt;
                        }              
                        //assert(0 && "OK");
                        cur->block = TO;
                        cur->nxt = TO->tofree;
                        TO->tofree = cur;
                        break;
                    }
                    prv = cur;
                    cur = cur->nxt;
                }
            }
    
            void ceu_print1 (CEU_Value* v) {
                switch (v->tag) {
                    case CEU_VALUE_NIL:
                        printf("nil");
                        break;
                    case CEU_VALUE_BOOL:
                        if (v->bool) {
                            printf("true");
                        } else {
                            printf("false");
                        }
                        break;
                    case CEU_VALUE_NUMBER:
                        printf("%g", v->number);
                        break;
                    case CEU_VALUE_TUPLE:
                        printf("[");
                        for (int i=0; i<v->tuple->n; i++) {
                            if (i > 0) {
                                printf(",");
                            }
                            ceu_print1(&v->tuple->buf[i]);
                        }                    
                        printf("]");
                        break;
                    default:
                        assert(0 && "bug found");
                }
            }
            CEU_Value ceu_print (CEU_Block* block, CEU_Block* scope, int n, CEU_Value* args[]) {
                for (int i=0; i<n; i++) {
                    ceu_print1(args[i]);
                }
                return (CEU_Value) { CEU_VALUE_NIL };
            }
            CEU_Value ceu_println (CEU_Block* block, CEU_Block* scope, int n, CEU_Value* args[]) {
                ceu_print(block, scope, n, args);
                printf("\n");
                return (CEU_Value) { CEU_VALUE_NIL };
            }
            CEU_Value print   = { CEU_VALUE_FUNC, {.func=ceu_print}   };
            CEU_Value println = { CEU_VALUE_FUNC, {.func=ceu_println} };
            
            CEU_Value ceu_op_umn (CEU_Block* block, CEU_Block* scope, int n, CEU_Value* args[]) {
                assert(n == 1);
                CEU_Value* e = args[0];
                assert(e->tag == CEU_VALUE_NUMBER);
                return (CEU_Value) { CEU_VALUE_NUMBER, {.number=(-e->number)} };
            }
            CEU_Value ceu_op_plus (CEU_Block* block, CEU_Block* scope, int n, CEU_Value* args[]) {
                assert(n == 2);
                CEU_Value* e1 = args[0];
                CEU_Value* e2 = args[1];
                assert(e1->tag == CEU_VALUE_NUMBER);
                assert(e2->tag == CEU_VALUE_NUMBER);
                return (CEU_Value) { CEU_VALUE_NUMBER, {.number=(e1->number+e2->number)} };
            }
            CEU_Value ceu_op_minus (CEU_Block* block, CEU_Block* scope, int n, CEU_Value* args[]) {
                assert(n == 2);
                CEU_Value* e1 = args[0];
                CEU_Value* e2 = args[1];
                assert(e1->tag == CEU_VALUE_NUMBER);
                assert(e2->tag == CEU_VALUE_NUMBER);
                return (CEU_Value) { CEU_VALUE_NUMBER, {.number=(e1->number-e2->number)} };
            }
            CEU_Value ceu_op_mult (CEU_Block* block, CEU_Block* scope, int n, CEU_Value* args[]) {
                assert(n == 2);
                CEU_Value* e1 = args[0];
                CEU_Value* e2 = args[1];
                assert(e1->tag == CEU_VALUE_NUMBER);
                assert(e2->tag == CEU_VALUE_NUMBER);
                return (CEU_Value) { CEU_VALUE_NUMBER, {.number=(e1->number*e2->number)} };
            }
            CEU_Value ceu_op_div (CEU_Block* block, CEU_Block* scope, int n, CEU_Value* args[]) {
                assert(n == 2);
                CEU_Value* e1 = args[0];
                CEU_Value* e2 = args[1];
                assert(e1->tag == CEU_VALUE_NUMBER);
                assert(e2->tag == CEU_VALUE_NUMBER);
                return (CEU_Value) { CEU_VALUE_NUMBER, {.number=(e1->number/e2->number)} };
            }
            CEU_Value op_umn   = { CEU_VALUE_FUNC, {.func=ceu_op_umn}   };
            CEU_Value op_plus  = { CEU_VALUE_FUNC, {.func=ceu_op_plus}  };
            CEU_Value op_minus = { CEU_VALUE_FUNC, {.func=ceu_op_minus} };
            CEU_Value op_mult  = { CEU_VALUE_FUNC, {.func=ceu_op_mult}  };
            CEU_Value op_div   = { CEU_VALUE_FUNC, {.func=ceu_op_div}   };

            typedef enum {
                CEU_THROW_NONE = 0,
                CEU_THROW_RUNTIME
            } CEU_Throw;
            CEU_Throw ceu_throw = CEU_THROW_NONE;
            CEU_Value ceu_throw_arg;
            CEU_Block* ceu_block_global = NULL;     // used as throw scope. then, catch fixes it
            char ceu_throw_msg[256];
    
            int main (void) {
                do {
                    ${es.code("", null)}
                    return 0;
                } while (0);
                fprintf(stderr, "%s\n", ceu_throw_msg);
                return 1;
            }
        """.trimIndent()
    }
}