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
                ${this.dst.mem()}
                ${this.src.mem()}
            };
            """
        is Expr.If -> """
            union { // IF
                ${this.cnd.mem()}
                ${this.t.mem()}
                ${this.f.mem()}
            };
            """
        is Expr.While -> """
            union { // WHILE
                ${this.cnd.mem()}
                ${this.body.mem()}
            };
            """
        is Expr.Catch -> """
            struct { // CATCH
                CEU_Value catch_$n;
                union {
                    ${this.catch.mem()}
                    ${this.body.mem()}
                };
            };
            """
        is Expr.Throw -> this.ex.mem()
        is Expr.Defer -> this.body.mem()

        is Expr.Coro -> this.task.mem()
        is Expr.Bcast -> this.arg.mem()
        is Expr.Resume -> """
            union { // RESUME
                // FUNC
                ${this.call.f.mem()}
                struct { // ARGS
                    ${this.call.args.map { it.mem() }.joinToString("")}
                    ${this.call.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
                };
            };
            """
        is Expr.Yield -> this.arg.mem()
        is Expr.Spawn -> """
            union { // SPAWN
                // CORO
                ${this.call.f.mem()}
                struct { // ARGS
                    ${this.call.args.map { it.mem() }.joinToString("")}
                    ${this.call.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
                };
            };
            """

        is Expr.Tuple -> """
            struct { // TUPLE
                ${this.args.map { it.mem() }.joinToString("")}
                ${this.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
            };
            """
        is Expr.Index -> """
            struct { // INDEX
                CEU_Value col_$n;   // both required
                CEU_Value idx_$n;   // for set as well
                union {
                    ${this.col.mem()}
                    ${this.idx.mem()}
                };
            };
            """
        is Expr.Call -> """
            union { // CALL
                ${this.f.mem()}
                struct {
                    ${this.args.map { it.mem() }.joinToString("")}
                    ${this.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
                };
            };
            """
        else -> ""
    }
}