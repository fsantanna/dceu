package dceu

val union = "union"

fun Expr.coexists (): Boolean {
    return when (this) {
        is Expr.Export, is Expr.Dcl -> true
        is Expr.Set    -> this.dst.coexists() || this.src.coexists()
        is Expr.If     -> this.cnd.coexists()
        is Expr.Drop   -> this.e.coexists()

        is Expr.Catch  -> this.cnd?.coexists() ?: false

        is Expr.Yield  -> this.arg.coexists()
        is Expr.Resume -> this.call.coexists()

        is Expr.Tuple  -> this.args.any { it.coexists() }
        is Expr.Vector -> this.args.any { it.coexists() }
        is Expr.Dict   -> this.args.any { it.first.coexists() || it.second.coexists() }
        is Expr.Index  -> this.col.coexists() || this.idx.coexists()
        is Expr.Call   -> this.clo.coexists() || this.args.any { it.coexists() }

        is Expr.Proto, is Expr.Do, is Expr.XLoop, is Expr.XBreak, is Expr.Enum, is Expr.Data, is Expr.Pass -> false
        is Expr.Defer -> false
        is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> false
    }
}

fun Expr.union_or_struct (): String {
    return if (this.coexists()) "struct" else union
}

fun List<Expr>.seq (defers: MutableMap<Expr.Do, Pair<String,String>>, i: Int): String {
    return (i != this.size).cond {
        """
            ${this[i].union_or_struct()} { // SEQ
                ${this[i].mem(defers)}
                ${this.seq(defers, i+1)}
            };
        """
    }
}

fun Expr.mem (defers: MutableMap<Expr.Do, Pair<String,String>>): String {
    return when (this) {
        is Expr.Export -> this.body.mem(defers)
        is Expr.Do -> """
            struct { // BLOCK
                ${sta.void(this).cond { """
                    CEU_Block _ceu_block_$n;
                """ }}
                CEU_Block* ceu_block_$n;
                ${defers.pub[this]!!.map { "int defer_${it.key.n};\n" }.joinToString("")}
                ${es.seq(defers, 0)}
            };
        """
        is Expr.Dcl -> {
            val id = this.id.str.id2c(this.n)
            """
            struct { // DCL
                struct {
                    CEU_Value ${id};
                    CEU_Block* _${id}_;
                    ${this.src.cond { it.mem(defers) } }
                };
            };
            """
        }
        is Expr.Set -> """
            struct { // SET
                CEU_Value set_$n;
                $union {
                    ${this.dst.mem(defers)}
                    ${this.src.mem(defers)}
                };
            };
            """
        is Expr.If -> """
            $union { // IF
                ${this.cnd.mem(defers)}
                ${this.t.mem(defers)}
                ${this.f.mem(defers)}
            };
            """
        is Expr.XLoop -> this.body.mem(defers)
        is Expr.Pass -> this.e.mem(defers)
        is Expr.Drop -> this.e.mem(defers)

        is Expr.Catch -> """
            $union { // CATCH
                ${this.cnd?.mem(defers) ?: ""}
                ${this.body.mem(defers)}
            };
        """
        is Expr.Defer -> this.body.mem(defers)

        is Expr.Yield -> this.arg.mem(defers)
        is Expr.Resume -> this.call.mem(defers)

        is Expr.Tuple -> """
            struct { // TUPLE
                CEU_Dyn* tup_$n;
                $union {
                    ${this.args.map { it.mem(defers) }.joinToString("")}
                };
            };
            """
        is Expr.Vector -> """
            struct { // VECTOR
                CEU_Dyn* vec_$n;
                $union {
                    ${this.args.map { it.mem(defers) }.joinToString("")}
                };
            };
            """
        is Expr.Dict -> """
            struct { // DICT
                CEU_Dyn* dict_$n;
                CEU_Value key_$n;
                $union {
                    ${this.args.map {
                        listOf(it.first.mem(defers),it.second.mem(defers))
                    }.flatten().joinToString("")}
                };
            };
            """
        is Expr.Index -> """
            struct { // INDEX
                CEU_Value idx_$n;
                $union {
                    ${this.col.mem(defers)}
                    ${this.idx.mem(defers)}
                };
            };
            """
        is Expr.Call -> """
            struct { // CALL
                ${this.args.mapIndexed { i,_ -> "CEU_Value arg_${i}_$n;\n" }.joinToString("")}
                $union {
                    ${this.clo.mem(defers)}
                    ${this.args.map { it.mem(defers) }.joinToString("")}
                };
            };
            """

        is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> ""
        is Expr.Proto, is Expr.Enum, is Expr.Data, is Expr.XBreak -> ""
    }
}
