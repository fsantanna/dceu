fun Expr.code (): Pair<String,String> {
    return when (this) {
        is Expr.Num -> Pair("", "((ceu_value) { CEU_VALUE_NUMBER, {.number=${this.tk.str}} })")
        is Expr.Var -> Pair("", this.tk.str)
        is Expr.Tuple -> {
            val (ss, es) = this.args.map { it.code() }.unzip()
            val n = this.hashCode()
            val tup = """
                ceu_value buf_$n[${es.size}] = { ${es.joinToString(",")} };
                ceu_value_tuple tup_$n = { ${es.size}, buf_$n };
                
            """.trimIndent()
            Pair (
                ss.joinToString("") + tup,
                "((ceu_value) { CEU_VALUE_TUPLE, {.tuple=&tup_$n} })"
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
        is Expr.ECall -> {
            val (s, e) = this.f.code()
            val (ss, es) = this.args.map { it.code() }.unzip()
            Pair(s+ss.joinToString(""), e + "(" + es.joinToString(",") + ")")
        }
    }
}

fun Stmt.code (): String {
    return when (this) {
        is Stmt.Nop   -> ""
        is Stmt.Seq   -> this.s1.code() + ";\n" + this.s2.code() + ";\n"
        is Stmt.SCall -> {
            val (s,e) = this.e.code()
            s + e + ";\n"
        }
    }
}

fun Code (s: Stmt): String {
    return """
        #include <stdio.h>
        #include <stdlib.h>
        #include <assert.h>

        typedef enum CEU_VALUE {
            CEU_VALUE_NUMBER,
            CEU_VALUE_TUPLE
        } CEU_VALUE;
        
        struct ceu_value;
        typedef struct ceu_value_tuple {
            int n;
            struct ceu_value* buf;
        } ceu_value_tuple;
        typedef struct ceu_value {
            int tag;
            union {
                float number;
                ceu_value_tuple* tuple;
            };
        } ceu_value;
        
        void print_aux (ceu_value v) {
            switch (v.tag) {
                case CEU_VALUE_NUMBER:
                    printf("%f", v.number);
                    break;
                case CEU_VALUE_TUPLE:
                    printf("[");
                    for (int i=0; i<v.tuple->n; i++) {
                        if (i > 0) {
                            printf(",");
                        }
                        print_aux(v.tuple->buf[i]);
                    }                    
                    printf("]");
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        void print (ceu_value v) {
            assert(v.tag==CEU_VALUE_TUPLE && "cannot print : expected tuple argument");
            for (int i=0; i<v.tuple->n; i++) {
                if (i > 0) {
                    printf("\t");
                }
                print_aux(v.tuple->buf[i]);
            }
        }
        void println (ceu_value v) {
            print(v);
            printf("\n");
        }
        
        void main (void) {
            ${s.code()}
        }
    """.trimIndent()
}
