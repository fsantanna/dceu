fun Expr.code (): Pair<String,String> {
    return when (this) {
        is Expr.Do -> {
            val (ss,e) = this.es.code()
            val s = """
                ceu_value do_$n = { CEU_VALUE_NIL };
                {
                    ceu_value_tuple* ceu_tofree = NULL;
                    $ss
                    do_$n = $e;
                    while (ceu_tofree != NULL) {
                        ceu_value_tuple* cur = ceu_tofree;
                        ceu_tofree = ceu_tofree->nxt;
                        free(cur->buf);
                        free(cur);
                    }
                }
                
            """.trimIndent()
            Pair(s, "do_$n")
        }
        is Expr.Dcl -> Pair("ceu_value ${this.tk.str} = { CEU_VALUE_NIL };", this.tk.str)
        is Expr.Set -> {
            val (s1, e1) = this.dst.code()
            val (s2, e2) = this.src.code()
            val set = """
                ceu_value src_$n = $e2;
                $e1 = src_$n;
                
            """.trimIndent()
            Pair(s1+s2+set, "src_$n")
        }
        is Expr.Acc -> Pair("", this.tk.str)
        is Expr.Num -> Pair("", "((ceu_value) { CEU_VALUE_NUMBER, {.number=${this.tk.str}} })")
        is Expr.Tuple -> {
            val (ss, es) = this.args.map { it.code() }.unzip()
            val tup = """
                assert(${es.size} < UINT8_MAX);
                ceu_value _buf_$n[${es.size}] = { ${es.joinToString(",")} };
                ceu_value* buf_$n = malloc(${es.size} * sizeof(ceu_value));
                memcpy(buf_$n, _buf_$n, ${es.size} * sizeof(ceu_value));
                ceu_value_tuple* tup_$n = malloc(sizeof(ceu_value_tuple));
                *tup_$n = (ceu_value_tuple) { CEU_SCOPE, ceu_tofree, buf_$n, ${es.size} };
                ceu_tofree = tup_$n;
                
            """.trimIndent()
            Pair (
                ss.joinToString("") + tup,
                "((ceu_value) { CEU_VALUE_TUPLE, {.tuple=tup_$n} })"
            )
        }
        is Expr.Index -> {
            val (s1, e1) = this.col.code()
            val (s2, e2) = this.idx.code()
            val s = """
                assert($e1.tag == CEU_VALUE_TUPLE && "index error : expected tuple");
                assert($e2.tag == CEU_VALUE_NUMBER && "index error : expected number");
                assert($e1.tuple->n > $e2.number && "index error : out of bounds");
                
            """.trimIndent()
            Pair(s1+s2+s, "$e1.tuple->buf[(int)$e2.number]")
        }
        is Expr.Call -> {
            val (s, e) = this.f.code()
            val (ss, es) = this.args.map { it.code() }.unzip()
            val call = """
                ceu_value call_$n = $e(${es.joinToString(",")});
                
            """.trimIndent()
            Pair(s+ss.joinToString("")+call, "call_$n")
        }
    }
}

fun List<Expr>.code (): Pair<String,String> {
    val (ss,es) = this.map { it.code() }.unzip()
    return Pair(ss.joinToString("\n")+"\n", es.lastOrNull() ?: "((ceu_value) { CEU_VALUE_NIL })")
}

fun Code (es: List<Expr>): String {
    return """
        #include <stdio.h>
        #include <stdlib.h>
        #include <stdint.h>
        #include <string.h>
        #include <assert.h>

        typedef enum CEU_VALUE {
            CEU_VALUE_NIL,
            CEU_VALUE_NUMBER,
            CEU_VALUE_TUPLE
        } CEU_VALUE;
        
        struct ceu_value;
        typedef struct ceu_value_tuple {
            uint8_t scope;
            struct ceu_value_tuple* nxt;
            struct ceu_value* buf;
            uint8_t n;
        } ceu_value_tuple;
        typedef struct ceu_value {
            int tag;
            union {
                //void nil;
                float number;
                ceu_value_tuple* tuple;
            };
        } ceu_value;
        
        ceu_value print (ceu_value v) {
            switch (v.tag) {
                case CEU_VALUE_NIL:
                    printf("nil");
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
                        print(v.tuple->buf[i]);
                    }                    
                    printf("]");
                    break;
                default:
                    assert(0 && "bug found");
            }
            return (ceu_value) { CEU_VALUE_NIL };
        }
        ceu_value println (ceu_value v) {
            print(v);
            printf("\n");
            return (ceu_value) { CEU_VALUE_NIL };
        }
        
        uint8_t CEU_SCOPE = 0;
        
        void main (void) {
            ceu_value_tuple* ceu_tofree = NULL;
            ${es.code().first}
            while (ceu_tofree != NULL) {
                ceu_value_tuple* cur = ceu_tofree;
                ceu_tofree = ceu_tofree->nxt;
                free(cur->buf);
                free(cur);
            }
        }
    """.trimIndent()
}
