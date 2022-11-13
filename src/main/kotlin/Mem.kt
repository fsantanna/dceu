fun Expr.mem (): String {
    return when (this) {
        is Expr.Block -> {
            fun List<Expr>.seq (i: Int): String {
                return if (i == this.size) "" else {
                    val s = if (this[i] is Expr.Block) "union" else "struct"
                    return """
                        $s {
                            ${this[i].mem()}
                            ${this.seq(i+1)}
                        };
                    """
                }
            }
            """
            struct { // BLOCK
                CEU_Block block_$n;
                int brk_$n;
                ${es.seq(0)}
            };
            """
        }
        is Expr.Dcl -> {
            val id = this.tk_.fromOp().noSpecial()
            """
            struct { // DCL
                CEU_Value $id;
                CEU_Block* _${id}_; // can't be static b/c recursion
            };
            """
        }
        is Expr.Set -> """
            struct {
                CEU_Value set_$n;
                ${this.dst.mem()}
                ${this.src.mem()}
            };
            """
        is Expr.If -> """
            struct { // IF
                CEU_Value ret_$n;
                CEU_Value cnd_$n;
                union {
                    ${this.cnd.mem()}
                    ${this.t.mem()}
                    ${this.f.mem()}
                };
            };
            """
        is Expr.While -> """
            struct { // WHILE
                int brk_$n;
                CEU_Value cnd_$n;
                union {
                    ${this.cnd.mem()}
                    ${this.body.mem()}
                };
            };
            """
        is Expr.Catch -> """
            struct { // CATCH
                int brk_$n;
                CEU_Value catch_$n;
                union {
                    ${this.catch.mem()}
                    ${this.body.mem()}
                };
            };
            """
        is Expr.Throw -> """
            struct { // THROW
                CEU_Value ex_$n;
                union {
                    ${this.ex.mem()}
                    ${this.arg.mem()}
                };
            };
            """
        is Expr.Spawn -> """
            struct { // SPAWN
                CEU_Value task_$n;
                ${this.task.mem()}
            };
            """
        is Expr.Resume -> """
            struct { // RESUME
                CEU_Value ret_$n;
                CEU_Value coro_$n;
                union {
                    // FUNC
                    ${this.call.f.mem()}
                    struct { // ARGS
                        ${this.call.args.map { it.mem() }.joinToString("")}
                        ${this.call.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
                    };
                };
            };
            """
        is Expr.Yield -> """
            struct { // YIELD
                CEU_Value ret_$n;
                ${this.arg.mem()}
            };
            """
        is Expr.Defer -> this.body.mem()

        is Expr.Tuple -> """
            struct { // TUPLE
                ${this.args.map { it.mem() }.joinToString("")}
                ${this.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
            };
            """
        is Expr.Index -> """
            struct { // INDEX
                CEU_Value col_$n;
                CEU_Value idx_$n;
                union {
                    ${this.col.mem()}
                    ${this.idx.mem()}
                };
            };
            """
        is Expr.Call -> """
            struct { // CALL
                CEU_Value ret_$n;
                CEU_Value f_$n;
                union {
                    ${this.f.mem()}
                    struct {
                        ${this.args.map { it.mem() }.joinToString("")}
                        ${this.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
                    };
                };
            };
            """
        else -> ""
    }
}