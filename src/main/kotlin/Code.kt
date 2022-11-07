fun Expr.code (): Pair<String,String> {
    return when (this) {
        is Expr.Do -> {
            val (ss,e) = this.es.code()
            val up = if (this.tk.str=="") "NULL" else "&ceu_block"
            val s = """
                CEU_Value ceu_$n = { CEU_VALUE_NIL };
                {
                    assert(CEU_DEPTH < UINT8_MAX);
                    CEU_DEPTH++;
                    CEU_Block* ceu_up = $up;
                    CEU_Block ceu_block = { CEU_DEPTH, NULL, ceu_up };
                    ceu_scope = &ceu_block;
                    $ss
                    ceu_$n = $e;
                    ceu_scope = ceu_up;
                    ceu_block_free(&ceu_block);
                    CEU_DEPTH--;
                }
                
            """.trimIndent()
            Pair(s, "ceu_$n")
        }
        is Expr.Dcl -> Pair (
            """
                CEU_Value ${this.tk.str} = { CEU_VALUE_NIL };
                CEU_Block* _${this.tk.str}_ = &ceu_block; // enclosing block
                
            """.trimIndent(),
            this.tk.str
        )
        is Expr.Set -> {
            val (s1, e1) = this.dst.code()
            val (s2, e2) = this.src.code()
            val isidx = if (this.dst is Expr.Index) 1 else 0
            assert(isidx==1 || this.dst is Expr.Acc) { "bug found" }
            val pre = """
                {
                    if ($isidx) {                           // x[i] = src
                        ceu_scope = ceu_col->block;         // scope of x
                    } else {                                // x = src
                        ceu_scope = _${this.dst.tk.str}_;   // scope of x
                    }
                }
                
            """.trimIndent()
            val pos = """
                CEU_Value ceu_$n = $e2;
                {
                    if (ceu_$n.tag == CEU_VALUE_TUPLE) {
                        assert(ceu_$n.tuple->block->depth <= ceu_scope->depth && "set error : incompatible scopes");
                    }
                    $e1 = ceu_$n;
                }
                
            """.trimIndent()
            Pair(s1+pre+s2+pos, "ceu_$n")
        }
        is Expr.If -> {
            val (sc, ec) = this.cnd.code()
            val (st, et) = this.t.code()
            val (sf, ef) = this.f.code()
            val s = """
                CEU_Value ceu_$n;
                {
                    CEU_Value ceu1_$n = $ec;
                    int ceu2_$n; {
                        switch (ceu1_$n.tag) {
                            case CEU_VALUE_NIL:  { ceu2_$n=0; break; }
                            case CEU_VALUE_BOOL: { ceu2_$n=ceu1_$n.bool; break; }
                            default:
                                assert(0 && "if error : invalid condition");
                        }
                    }
                    if (ceu2_$n) {
                        $st
                        ceu_$n = $et;
                    } else {
                        $sf
                        ceu_$n = $ef;
                    }
                }
                
            """.trimIndent()
            Pair(sc+s, "ceu_$n")
        }
        is Expr.Func -> TODO()
        is Expr.Acc -> Pair("", this.tk.str)
        is Expr.Nil -> Pair("", "((CEU_Value) { CEU_VALUE_NIL })")
        is Expr.Bool -> Pair("", "((CEU_Value) { CEU_VALUE_BOOL, {.bool=${if (this.tk.str=="true") 1 else 0}} })")
        is Expr.Num -> Pair("", "((CEU_Value) { CEU_VALUE_NUMBER, {.number=${this.tk.str}} })")
        is Expr.Tuple -> {
            val (ss, es) = this.args.map { it.code() }.unzip()
            val tup = """
                CEU_Value_Tuple* ceut_$n = malloc(sizeof(CEU_Value_Tuple));
                {
                    assert(${es.size} < UINT8_MAX);
                    CEU_Value ceu1_$n[${es.size}] = { ${es.joinToString(",")} };
                    CEU_Value* ceu2_$n = malloc(${es.size} * sizeof(CEU_Value));
                    memcpy(ceu2_$n, ceu1_$n, ${es.size} * sizeof(CEU_Value));
                    *ceut_$n = (CEU_Value_Tuple) { ceu_scope, ceu_scope->tofree, ceu2_$n, ${es.size} };
                    ceu_scope->tofree = ceut_$n;
                }
                
            """.trimIndent()
            Pair (
                ss.joinToString("") + tup,
                "((CEU_Value) { CEU_VALUE_TUPLE, {.tuple=ceut_$n} })"
            )
        }
        is Expr.Index -> {
            val (s1, e1) = this.col.code()
            val (s2, e2) = this.idx.code()
            val s = """
                int ceu_$n = (int) $e2.number;
                {
                    assert($e1.tag == CEU_VALUE_TUPLE && "index error : expected tuple");
                    assert($e2.tag == CEU_VALUE_NUMBER && "index error : expected number");
                    assert($e1.tuple->n > ceu_$n && "index error : out of bounds");
                    ceu_col = $e1.tuple;
                }
                
            """.trimIndent()
            Pair(s1+s2+s, "$e1.tuple->buf[ceu_$n]")
        }
        is Expr.Call -> {
            val (s, e) = this.f.code()
            val (ss, es) = this.args.map { it.code() }.unzip()
            val pre = "ceu_scope = &ceu_block;\n" // allocate in current block
            val pos = "CEU_Value ceu_$n = $e(${es.size}${if (es.size>0) "," else ""}${es.joinToString(",")});\n"
            Pair(s+pre+ss.joinToString("")+pos, "ceu_$n")
        }
    }
}

fun List<Expr>.code (): Pair<String,String> {
    val (ss,es) = this.map { it.code() }.unzip()
    return Pair(ss.joinToString("\n")+"\n", es.lastOrNull() ?: "((CEU_Value) { CEU_VALUE_NIL })")
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
            CEU_VALUE_TUPLE
        } CEU_VALUE;
        
        struct CEU_Value;
        struct CEU_Block;
        
        typedef struct CEU_Value_Tuple {
            struct CEU_Block* block;
            struct CEU_Value_Tuple* nxt;
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
            };
        } CEU_Value;
        
        typedef struct CEU_Block {
            uint8_t depth;
            CEU_Value_Tuple* tofree;    // list of allocated tuples to free on exit
            struct CEU_Block* up;           // up link to find allocation scope 
        } CEU_Block;
        void ceu_block_free (CEU_Block* block) {
            while (block->tofree != NULL) {
                CEU_Value_Tuple* cur = block->tofree;
                block->tofree = block->tofree->nxt;
                free(cur->buf);
                free(cur);
            }
        }

        void print1 (CEU_Value v) {
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
                        print1(v.tuple->buf[i]);
                    }                    
                    printf("]");
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        CEU_Value vprint (int n, va_list args) {
            if (n > 0) {
                for (int i=0; i<n; i++) {
                    print1(va_arg(args, CEU_Value));
                }
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Value print (int n, ...) {
            if (n > 0) {
                va_list args;
                va_start(args, n);
                vprint(n, args);
                va_end(args);
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Value println (int n, ...) {
            va_list args;
            va_start(args, n);
            vprint(n, args);
            va_end(args);
            printf("\n");
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        
        CEU_Block* ceu_scope;
        CEU_Value_Tuple* ceu_col;
        uint8_t CEU_DEPTH = 0;
        
        void main (void) {
            ${es.code().first}
        }
    """.trimIndent()
}
