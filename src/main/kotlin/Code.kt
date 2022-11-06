fun Expr.code (): String {
    return when (this) {
        is Expr.Num -> "(ceu_value) { CEU_NUMBER, {.number=${this.tk.str}} }"
        is Expr.Var -> this.tk.str
        is Expr.ECall -> this.f.code() + "(" + this.args.map { it.code() }.joinToString(",") + ")"
    }
}

fun Stmt.code (): String {
    return when (this) {
        is Stmt.Nop   -> ""
        is Stmt.Seq   -> this.s1.code() + ";\n" + this.s2.code() + ";\n"
        is Stmt.SCall -> this.e.code() + ";\n"
    }
}

fun Code (s: Stmt): String {
    return """
        #include <assert.h>
        #include <stdio.h>

        typedef enum CEU_VALUE {
            CEU_NUMBER
        } CEU_VALUE;
        
        typedef struct ceu_value {
            int tag;
            union {
                float number;
            };
        } ceu_value;
        
        void print (ceu_value v) {
            switch (v.tag) {
                case CEU_NUMBER:
                    printf("%f", v.number);
                    break;
                default:
                    assert(0 && "bug found");
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
