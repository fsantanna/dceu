fun fset (set: Pair<String,String>?, src: String): String {
    return if (set == null) "" else fset(set.first, set.second, src)
}
fun fset (scope: String, dst: String, src: String): String {
    return """
        if ($src.tag == CEU_VALUE_TUPLE) {
            assert($src.tuple->block->depth <= $scope->depth && "set error : incompatible scopes");
        }
        $dst = $src;

    """.trimIndent()
}

// block: String -> current enclosing block for normal allocation
// set: Pair<scope,dst> -> enclosing assignment with block and destination

fun Expr.code (block: String, set: Pair<String,String>?): String {
    return when (this) {
        is Expr.Do -> {
            val depth = if (block == "") 0 else "$block->depth+1"
            """
            { // DO
                assert($depth < UINT8_MAX);
                CEU_Block ceu_block_$n = { $depth, NULL };
                ${this.es.code("(&ceu_block_$n)", set)}
                ceu_block_free(&ceu_block_$n);
            }
            
        """.trimIndent()
        }
        is Expr.Dcl -> """
            // DCL
            CEU_Value ${this.tk.str} = { CEU_VALUE_NIL };
            CEU_Block* _${this.tk.str}_ = $block;   // TODO: remove (pass symtable to code())
            ${fset(set,this.tk.str)}            
                
        """.trimIndent()
        is Expr.Set -> {
            val (scp,dst) = when (this.dst) {
                is Expr.Index -> Pair (
                    "ceu_col_${this.dst.n}.tuple->block",
                    "ceu_col_${this.dst.n}.tuple->buf[(int) ceu_idx_${this.dst.n}.number]"
                )
                is Expr.Acc -> Pair (
                    "_${this.dst.tk.str}_",  // x = src / scope of _x_
                    this.dst.tk.str
                )
                else -> error("bug found")
            }
            """
            { // SET
                CEU_Value ceu_$n;
                ${this.dst.code(block, null)}
                ${this.src.code(block, Pair(scp,"ceu_$n"))}
                $dst = ceu_$n;
                ${fset(set,"ceu_$n")}
            }
                
            """.trimIndent()
            //Pair(s1+pre+s2+pos, "ceu_$n")
        }
        is Expr.If -> """
            { // IF
                CEU_Value ceu_cnd_$n;
                ${this.cnd.code(block, Pair(block,"ceu_cnd_$n"))}
                int ceu_ret_$n; {
                    switch (ceu_cnd_$n.tag) {
                        case CEU_VALUE_NIL:  { ceu_ret_$n=0; break; }
                        case CEU_VALUE_BOOL: { ceu_ret_$n=ceu_cnd_$n.bool; break; }
                        default:
                            assert(0 && "if error : invalid condition");
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
        is Expr.Break -> """
            { // BREAK
                ${this.arg.code(block, set)}
                break;
            }
                
        """.trimIndent()
        is Expr.Func -> """
            CEU_Value ceu_func_$n (CEU_Block* ceu_block, CEU_Block* ceu_scope, int ceu_n, ...) {
                int ceu_i = 0;
                va_list ceu_args;
                va_start(ceu_args, ceu_n);
                ${this.args.map {
                """
                CEU_Value ${it.str} = { CEU_VALUE_NIL };
                if (ceu_i < ceu_n) {
                    ${it.str} = va_arg(ceu_args, CEU_Value);
                }
                ceu_i++;
                """.trimIndent()
            }.joinToString("")}
                va_end(ceu_args);
                CEU_Value ceu_$n;
                ${this.body.code("ceu_block", Pair("ceu_scope","ceu_$n"))}
                return ceu_$n;
            }
            ${fset(set,"((CEU_Value) { CEU_VALUE_FUNC, {.func=ceu_func_$n} })")}            

        """.trimIndent()
        is Expr.Acc -> fset(set, this.tk.str)
        is Expr.Nil -> fset(set, "((CEU_Value) { CEU_VALUE_NIL })")
        is Expr.Bool -> fset(set, "((CEU_Value) { CEU_VALUE_BOOL, {.bool=${if (this.tk.str=="true") 1 else 0}} })")
        is Expr.Num -> fset(set, "((CEU_Value) { CEU_VALUE_NUMBER, {.number=${this.tk.str}} })")
        is Expr.Tuple -> {
            assert(this.args.size <= 256) { "bug found" }
            val scp = if (set==null) block else set.first
            val args = this.args.mapIndexed { i,it ->
                // allocate in the same scope of set (set.first) or use default block
                it.code(block, Pair(scp, "ceu_${i}_$n"))
            }.joinToString("")
            """
            { // TUPLE
                ${this.args.mapIndexed { i,_->"CEU_Value ceu_${i}_$n;\n" }.joinToString("")}
                $args
                CEU_Value ceu_sta_$n[${this.args.size}] = {
                    ${this.args.mapIndexed { i,_->"ceu_${i}_$n" }.joinToString(",")}
                };
                CEU_Value* ceu_dyn_$n = malloc(${this.args.size} * sizeof(CEU_Value));
                assert(ceu_dyn_$n != NULL);
                memcpy(ceu_dyn_$n, ceu_sta_$n, ${this.args.size} * sizeof(CEU_Value));
                CEU_Value_Tuple* ceu_$n = malloc(sizeof(CEU_Value_Tuple));
                assert(ceu_$n != NULL);
                *ceu_$n = (CEU_Value_Tuple) { $scp, $block->tofree, ceu_dyn_$n, ${this.args.size} };
                $scp->tofree = ceu_$n;
                ${fset(set, "((CEU_Value) { CEU_VALUE_TUPLE, {.tuple=ceu_$n} })")}
            }

            """.trimIndent()
        }
        is Expr.Index -> """
            //{ // INDEX    // (removed {} b/c set uses col[idx])
                CEU_Value ceu_col_$n;
                ${this.col.code(block, Pair(block,"ceu_col_$n"))}
                assert(ceu_col_$n.tag == CEU_VALUE_TUPLE && "index error : expected tuple");
                
                CEU_Value ceu_idx_$n;
                ${this.idx.code(block, Pair(block,"ceu_idx_$n"))}
                assert(ceu_idx_$n.tag == CEU_VALUE_NUMBER && "index error : expected number");
                
                assert(ceu_col_$n.tuple->n > ceu_idx_$n.number && "index error : out of bounds");
                ${fset(set, "ceu_col_$n.tuple->buf[(int) ceu_idx_$n.number]")}
            //}
            
        """.trimIndent()
        is Expr.Call -> """
            { // CALL
                CEU_Value ceu_f_$n;
                ${this.f.code(block, Pair(block,"ceu_f_$n"))}
                assert(ceu_f_$n.tag==CEU_VALUE_FUNC && "call error : expected function");
                ${this.args.mapIndexed { i,_ ->
                    "CEU_Value ceu_${i}_$n;\n"
                }.joinToString("")}
                ${this.args.mapIndexed { i,it ->
                    it.code(block, Pair(block, "ceu_${i}_$n"))
                }.joinToString("")}
                CEU_Value ceu_$n = ceu_f_$n.func(
                    $block,
                    ${if (set == null) block else set.first},
                    ${this.args.size}
                    ${(if (this.args.size>0) "," else "")}
                    ${this.args.mapIndexed { i,_->"ceu_${i}_$n" }.joinToString(",")}
                );
                ${fset(set, "ceu_$n")}
            }

        """.trimIndent()
    }
}

fun List<Expr>.code (block: String, set: Pair<String,String>?): String {
    return this.mapIndexed { i,it ->
        it.code(block, if (i==this.size-1) set else null) + "\n"
    }.joinToString("")
}

fun Code (es: Expr.Do): String {
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
                struct CEU_Value (*func) (struct CEU_Block* block, struct CEU_Block* scope, int ceu_n, ...);
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

        void ceu_print1 (CEU_Value v) {
            switch (v.tag) {
                case CEU_VALUE_NIL:
                    printf("nil");
                    break;
                case CEU_VALUE_BOOL:
                    if (v.bool) {
                        printf("true");
                    } else {
                        printf("false");
                    }
                    break;
                case CEU_VALUE_NUMBER:
                    printf("%f", v.number);
                    break;
                case CEU_VALUE_TUPLE:
                    printf("[");
                    for (int i=0; i<v.tuple->n; i++) {
                        if (i > 0) {
                            printf(",");
                        }
                        ceu_print1(v.tuple->buf[i]);
                    }                    
                    printf("]");
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        CEU_Value ceu_vprint (int n, va_list args) {
            if (n > 0) {
                for (int i=0; i<n; i++) {
                    ceu_print1(va_arg(args, CEU_Value));
                }
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Value ceu_print (CEU_Block* block, CEU_Block* scope, int n, ...) {
            if (n > 0) {
                va_list args;
                va_start(args, n);
                ceu_vprint(n, args);
                va_end(args);
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Value ceu_println (CEU_Block* block, CEU_Block* scope, int n, ...) {
            va_list args;
            va_start(args, n);
            ceu_vprint(n, args);
            va_end(args);
            printf("\n");
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Value print   = { CEU_VALUE_FUNC, {.func=ceu_print}   };
        CEU_Value println = { CEU_VALUE_FUNC, {.func=ceu_println} };
        
        void main (void) {
            ${es.code("", null)}
        }
    """.trimIndent()
}
