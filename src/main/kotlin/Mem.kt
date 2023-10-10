package dceu

val union = "union"
//val union = "struct"

fun Expr.coexists (): Boolean {
    return when (this) {
        is Expr.Dcl    -> true
        is Expr.Set    -> this.dst.coexists() || this.src.coexists()
        is Expr.If     -> this.cnd.coexists()
        is Expr.Drop   -> this.e.coexists()

        is Expr.Catch  -> this.cnd.coexists()

        is Expr.Yield  -> this.arg.coexists() || this.blk.coexists()
        is Expr.Resume -> this.co.coexists() || this.arg.coexists()

        is Expr.Spawn  -> (this.tsks?.coexists() ?: false) || this.tsk.coexists() || this.arg.coexists()
        is Expr.Bcast  -> this.call.coexists()
        is Expr.Dtrack -> this.trk.coexists() || this.blk.coexists()

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

fun List<Expr>.seq (sta: Static, defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>>, i: Int): String {
    return (i != this.size).cond {
        """
            ${this[i].union_or_struct()} { // SEQ
                ${this[i].mem(sta, defers)}
                ${this.seq(sta, defers, i+1)}
            };
        """
    }
}

fun Expr.mem (sta: Static, defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>>): String {
    return when (this) {
        is Expr.Do -> {
            if (!sta.ylds.contains(this)) "" else {
                """
                struct { // BLOCK
                    ${(!sta.void(this)).cond { """
                        CEU_Block _block_$n;
                    """ }}
                    CEU_Block* block_$n;
                    ${defers[this].cond { it.first.map { "int defer_${it};\n" }.joinToString("")} }
                    ${es.seq(sta, defers, 0)}
                };
                """
            }
        }
        is Expr.Dcl -> {
            val id = this.id.str.idc(this.n)
            """
            struct { // DCL
                struct {
                    CEU_Value ${id};
                    ${this.src.cond { it.mem(sta, defers) } }
                };
            };
            """
        }
        is Expr.Set -> """
            struct { // SET
                CEU_Value src_$n;
                $union {
                    ${this.dst.mem(sta, defers)}
                    ${this.src.mem(sta, defers)}
                };
            };
            """
        is Expr.If -> """
            $union { // IF
                ${this.cnd.mem(sta, defers)}
                ${this.t.mem(sta, defers)}
                ${this.f.mem(sta, defers)}
            };
            """
        is Expr.XLoop -> this.blk.mem(sta, defers)
        is Expr.Pass -> this.e.mem(sta, defers)
        is Expr.Drop -> this.e.mem(sta, defers)

        is Expr.Catch -> """
            $union { // CATCH
                ${this.cnd.mem(sta, defers)}
                ${this.blk.mem(sta, defers)}
            };
        """
        is Expr.Defer -> this.blk.mem(sta, defers)

        is Expr.Yield -> """
            $union { // YIELD
                ${this.arg.mem(sta, defers)}
                ${this.blk.mem(sta, defers)}
            };
        """
        is Expr.Resume -> """
            struct {
                CEU_Value co_${this.n};
                $union {
                    ${this.co.mem(sta, defers)}
                    ${this.arg.mem(sta, defers)}
                };
            };
        """

        is Expr.Spawn -> """
            struct {
                ${this.tsks.cond { "CEU_Value tsks_${this.n};" }} 
                CEU_Value tsk_${this.n}; 
                $union {
                    ${this.tsks.cond { it.mem(sta, defers) }} 
                    ${this.tsk.mem(sta, defers)}
                    ${this.arg.mem(sta, defers)}
                };
            };
        """
        is Expr.Bcast -> this.call.mem(sta, defers)
        is Expr.Dtrack -> """
            $union { // DTRACK
                ${this.trk.mem(sta, defers)}
                ${this.blk.mem(sta, defers)}
            };
        """

        is Expr.Tuple -> """
            struct { // TUPLE
                CEU_Value tup_$n;
                $union {
                    ${this.args.map { it.mem(sta, defers) }.joinToString("")}
                };
            };
            """
        is Expr.Vector -> """
            struct { // VECTOR
                CEU_Value vec_$n;
                $union {
                    ${this.args.map { it.mem(sta, defers) }.joinToString("")}
                };
            };
            """
        is Expr.Dict -> """
            struct { // DICT
                CEU_Value dic_$n;
                CEU_Value key_$n;
                $union {
                    ${this.args.map {
                        listOf(it.first.mem(sta, defers),it.second.mem(sta, defers))
                    }.flatten().joinToString("")}
                };
            };
            """
        is Expr.Index -> """
            struct { // INDEX
                CEU_Value idx_$n;
                $union {
                    ${this.col.mem(sta, defers)}
                    ${this.idx.mem(sta, defers)}
                };
            };
            """
        is Expr.Call -> """
            struct { // CALL
                CEU_Value args_$n[${this.args.size}];
                $union {
                    ${this.clo.mem(sta, defers)}
                    ${this.args.map { it.mem(sta, defers) }.joinToString("")}
                };
            };
            """

        is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> ""
        is Expr.Proto, is Expr.Enum, is Expr.Data, is Expr.XBreak -> ""
    }
}
