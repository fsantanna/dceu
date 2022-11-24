fun Expr.mem (): String {
    return when (this) {
        is Expr.Func -> (this.tk.str == "task").cond {
            "CEU_Frame task_$n;"
        }
        is Expr.Block -> {
            fun List<Expr>.seq (i: Int): String {
                return (i != this.size).cond {
                    val s = if (this[i] is Expr.Block) "union" else "struct"
                    """
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
            union { // CATCH
                ${this.cnd.mem()}
                ${this.body.mem()}
            };
            """
        is Expr.Throw -> this.ex.mem()
        is Expr.Defer -> this.body.mem()

        is Expr.Coros -> this.max?.mem() ?: ""
        is Expr.Coro -> this.task.mem()
        is Expr.Spawn -> """
            struct { // SPAWN
                ${this.coros.cond{"CEU_Value coros_$n;"}}
                union {
                    ${this.coros?.mem() ?: ""}
                    ${this.call.mem()}
                };
            };
        """
        is Expr.Iter -> """
            struct { // ITER
                CEU_Value coros_$n;
                CEU_Block* hold_$n;
                ${this.body.mem()}
            };
            """
        is Expr.Bcast -> this.evt.mem()
        is Expr.Yield -> this.arg.mem()
        is Expr.Resume -> this.call.mem()

        is Expr.Tuple -> """
            struct { // TUPLE
                ${this.args.map { it.mem() }.joinToString("")}
                ${this.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
            };
            """
        is Expr.Dict -> """
            struct { // DICT
                ${this.args.map {
                    listOf(it.first.mem(),it.second.mem())
                }.flatten().joinToString("")}
                ${this.args.mapIndexed { i,_ ->
                    """
                    CEU_Value arg_${i}_a_$n;
                    CEU_Value arg_${i}_b_$n;
                    """
                }.joinToString("")}
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

        is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Num -> ""
        is Expr.XSeq -> ""
    }
}
