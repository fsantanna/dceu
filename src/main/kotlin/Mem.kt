val union = "union"

fun List<Expr>.seq (i: Int): String {
    return (i != this.size).cond {
        val s = if (this[i].let { it is Expr.Do && it.ishide }) "$union" else "struct"
        """
            $s {
                ${this[i].mem()}
                ${this.seq(i+1)}
            };
        """
    }
}

fun Expr.mem (): String {
    return when (this) {
        is Expr.Do -> """
            struct { // BLOCK
                CEU_Block block_$n;
                ${es.seq(0)}
            };
        """
        is Expr.Dcl -> {
            val id = this.tk_.fromOp().noSpecial()
            """
            struct { // DCL
                struct {
                    CEU_Value $id;
                    CEU_Block* _${id}_; // can't be static b/c recursion
                    ${this.src.cond { it.mem() } }
                };
            };
            """
        }
        is Expr.Set -> """
            struct { // SET
                CEU_Value set_$n;
                $union {
                    ${this.dst.mem()}
                    ${this.src.mem()}
                };
            };
            """
        is Expr.If -> """
            $union { // IF
                ${this.cnd.mem()}
                ${this.t.mem()}
                ${this.f.mem()}
            };
            """
        is Expr.While -> """
            $union { // WHILE
                ${this.cnd.mem()}
                ${this.body.mem()}
            };
            """
        is Expr.Catch -> """
            $union { // CATCH
                ${this.cnd.mem()}
                ${this.body.mem()}
            };
            """
        is Expr.Defer -> this.body.mem()

        is Expr.Spawn -> """
            struct { // SPAWN
                ${this.coros.cond{"CEU_Value coros_$n;"}}
                $union {
                    ${this.coros.cond { it.mem() }}
                    ${this.call.mem()}
                };
            };
        """
        is Expr.Bcast -> """
            struct { // BCAST
                CEU_Value evt_$n;
                $union {
                    ${this.xin.mem()}
                    ${this.evt.mem()}
                };
            };
            """
        is Expr.Yield -> this.arg.mem()
        is Expr.Resume -> this.call.mem()
        is Expr.Toggle -> """
            struct { // TOGGLE
                CEU_Value on_$n;
                $union {
                    ${this.coro.mem()}
                    ${this.on.mem()}
                };
            };
            """
        is Expr.Pub -> this.coro.mem()

        is Expr.Tuple -> """
            struct { // TUPLE
                CEU_Dyn* tup_$n;
                $union {
                    ${this.args.map { it.mem() }.joinToString("")}
                };
            };
            """
        is Expr.Vector -> """
            struct { // VECTOR
                CEU_Dyn* vec_$n;
                $union {
                    ${this.args.map { it.mem() }.joinToString("")}
                };
            };
            """
        is Expr.Dict -> """
            struct { // DICT
                CEU_Dyn* dict_$n;
                CEU_Value key_$n;
                $union {
                    ${this.args.map {
                        listOf(it.first.mem(),it.second.mem())
                    }.flatten().joinToString("")}
                };
            };
            """
        is Expr.Index -> """
            struct { // INDEX
                CEU_Value idx_$n;
                $union {
                    ${this.col.mem()}
                    ${this.idx.mem()}
                };
            };
            """
        is Expr.Call -> """
            struct { // CALL
                ${this.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
                $union {
                    ${this.proto.mem()}
                    ${this.args.map { it.mem() }.joinToString("")}
                };
            };
            """

        is Expr.Nat, is Expr.Acc, is Expr.EvtErr, is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> ""
        is Expr.Task, is Expr.Proto -> ""
    }
}
