val union = "union"

fun Expr.coexists (): Boolean {
    return when (this) {
        is Expr.Export, is Expr.Dcl, is Expr.Defer -> true
        is Expr.Set    -> this.dst.coexists() || this.src.coexists()
        is Expr.If     -> this.cnd.coexists()
        is Expr.Catch  -> this.cnd.coexists()
        is Expr.Spawn  -> this.call.coexists()
        is Expr.Bcast  -> this.xin.coexists() || this.evt.coexists()
        is Expr.Yield  -> this.arg.coexists()
        is Expr.Resume -> this.call.coexists()
        is Expr.Toggle -> this.task.coexists() || this.on.coexists()
        is Expr.Pub    -> this.x.coexists()
        is Expr.Tuple  -> this.args.any { it.coexists() }
        is Expr.Vector -> this.args.any { it.coexists() }
        is Expr.Dict   -> this.args.any { it.first.coexists() || it.second.coexists() }
        is Expr.Index  -> this.col.coexists() || this.idx.coexists()
        is Expr.Call   -> this.proto.coexists() || this.args.any { it.coexists() }
        else -> false
    }
}

fun Expr.union_or_struct (): String {
    return if (this.coexists()) "struct" else union
}

fun List<Expr>.seq (i: Int): String {
    return (i != this.size).cond {
        """
            ${this[i].union_or_struct()} { // SEQ
                ${this[i].mem()}
                ${this.seq(i+1)}
            };
        """
    }
}

fun Expr.mem (): String {
    return when (this) {
        is Expr.Export -> this.body.mem()
        is Expr.Do -> """
            struct { // BLOCK
                CEU_Block block_$n;
                ${es.seq(0)}
            };
        """
        is Expr.Dcl -> {
            val id = this.id.str.id2c(this.n)
            """
            struct { // DCL
                struct {
                    ${if (id in listOf("evt","_")) "" else {
                        """
                        CEU_Value ${id};
                        CEU_Block* _${id}_; // can't be static b/c recursion
                        """
                    }}
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
        is Expr.Loop -> this.body.mem()
        is Expr.Catch -> """
            $union { // CATCH
                ${this.cnd.mem()}
                ${this.body.mem()}
            };
            """
        is Expr.Defer -> """
            struct { // DEFER
                int defer_$n;
                ${this.body.mem()}
            };
        """
        is Expr.Pass -> this.e.mem()

        is Expr.Spawn -> """
            struct { // SPAWN
                ${this.tasks.cond{"CEU_Value tasks_$n;"}}
                $union {
                    ${this.tasks.cond { it.mem() }}
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
                    ${this.task.mem()}
                    ${this.on.mem()}
                };
            };
            """
        is Expr.Pub -> this.x.mem()

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
        is Expr.Self, is Expr.Proto, is Expr.Enum, is Expr.Data -> ""
    }
}
