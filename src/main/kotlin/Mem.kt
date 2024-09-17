package dceu

import kotlin.math.max

val union = "union"
//val union = "struct"

class Mem (val sta: Static, val defers: MutableMap<Expr.Do, Triple<MutableList<Int>,String,String>>) {
    fun pub (e: Expr.Proto): String {
        return """
            struct { // PROTO | ${e.dump()}
                ${e.pars.map { """
                    CEU_Value ${it.idtag.first.str.idc()}_${it.n};
                """ }.joinToString("") }
                ${e.blk.mem()}
            };
        """
    }

    fun Expr.coexists (): Boolean {
        return when (this) {
            is Expr.Escape -> (this.e?.coexists() ?: false)
            is Expr.Group  -> this.es.any { it.coexists() }
            is Expr.Dcl    -> true
            is Expr.Set    -> this.dst.coexists() || this.src.coexists()
            is Expr.If     -> this.cnd.coexists()
            is Expr.Loop   -> this.blk.coexists()
            is Expr.Drop   -> this.e.coexists()

            is Expr.Yield  -> this.e.coexists()
            is Expr.Resume -> this.co.coexists() || this.args.any { it.coexists() }

            is Expr.Spawn  -> (this.tsks?.coexists() ?: false) || this.tsk.coexists() || this.args.any { it.coexists() }
            is Expr.Pub    -> this.tsk?.coexists() ?: false
            is Expr.Toggle -> this.tsk.coexists() || this.on.coexists()

            is Expr.Tasks  -> this.max.coexists()

            is Expr.Tuple  -> this.args.any { it.coexists() }
            is Expr.Vector -> this.args.any { it.coexists() }
            is Expr.Dict   -> this.args.any { it.first.coexists() || it.second.coexists() }
            is Expr.Index  -> this.col.coexists() || this.idx.coexists()
            is Expr.Call   -> this.clo.coexists() || this.args.any { it.coexists() }

            is Expr.Proto, is Expr.Do  -> false
            is Expr.Data, is Expr.Catch, is Expr.Defer, is Expr.Delay -> false
            is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> false
        }
    }

    fun Expr.union_or_struct (): String {
        return if (this.coexists()) "struct" else union
    }

    fun List<Expr>.seq (i: Int): String {
        return (i != this.size).cond {
            """
            ${this[i].union_or_struct()} { // SEQ | ${this[i].dump()}
                ${this[i].mem()}
                ${this.seq(i+1)}
            };
        """
        }
    }

    fun Expr.mem (): String {
        return when (this) {
            is Expr.Proto -> ""
            is Expr.Do -> this.is_mem().cond {
                """
                struct { // BLOCK | ${this.dump()}
                    ${(CEU >= 4).cond { """
                        CEU_Block block_$n;
                    """ }}
                    ${this.to_dcls().map { """
                        CEU_Value ${it.idtag.first.str.idc() + "_" + it.n };
                    """ }.joinToString("") }
                    ${defers[this].cond { it.first.map { """
                        int defer_${it};
                    """ }.joinToString("")} }
                    ${es.seq(0)}
                };
                """
            }
            is Expr.Escape -> this.e.cond { it.mem() }
            is Expr.Group -> """
                $union {
                    ${this.es.map { it.mem() }.joinToString("")}
                };
            """
            is Expr.Dcl -> this.src.cond { it.mem() }
            is Expr.Set -> """
                struct { // SET
                    CEU_Value src_$n;
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
            is Expr.Loop -> this.blk.mem()
            is Expr.Drop -> this.e.mem()

            is Expr.Catch -> this.blk.mem()
            is Expr.Defer -> this.blk.mem()

            is Expr.Yield -> this.e.mem()
            is Expr.Resume -> """
                struct {
                    CEU_Value coro_$n;  // b/c of CEU=50 to chk_set depth
                    CEU_Value args_$n[${this.args.size}];
                    $union {
                        ${this.co.mem()}
                        ${this.args.map { it.mem() }.joinToString("")}
                    };
                };
            """

            is Expr.Spawn -> """
                struct { // SPAWN | ${this.dump()}
                    ${this.tsks.cond { "CEU_Value tsks_${this.n};" }} 
                    CEU_Value args_$n[${this.args.size}];
                    $union {
                        ${this.tsks.cond { it.mem() }} 
                        ${this.tsk.mem()}
                        ${this.args.map { it.mem() }.joinToString("")}
                    };
                };
            """
            is Expr.Pub -> """
                struct { // PUB
                    ${this.is_dst().cond { """
                        CEU_Value val_$n;
                    """ }}
                    ${this.tsk?.mem() ?: ""}
                };
            """.trimIndent()
            is Expr.Toggle -> """
                struct { // TOGGLE
                    CEU_Value tsk_${this.n};
                    $union {
                        ${this.tsk.mem()}
                        ${this.on.mem()}
                    };
                };
            """
            is Expr.Tasks  -> this.max.mem()

            is Expr.Tuple -> """
                struct { // TUPLE
                    CEU_Value args_$n[${max(1,this.args.size)}];
                    $union {
                        ${this.args.map { it.mem() }.joinToString("")}
                    };
                };
                """
            is Expr.Vector -> """
                struct { // VECTOR
                    CEU_Value vec_$n;
                    $union {
                        ${this.args.map { it.mem() }.joinToString("")}
                    };
                };
                """
            is Expr.Dict -> """
                struct { // DICT
                    CEU_Value dic_$n;
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
                    CEU_Value col_$n;
                    ${this.is_dst().cond { """
                        CEU_Value val_$n;
                    """ }}
                    $union {
                        ${this.col.mem()}
                        ${this.idx.mem()}
                    };
                };
                """
            is Expr.Call -> """
                struct { // CALL
                    CEU_Value args_$n[${this.args.size}];
                    $union {
                        ${this.clo.mem()}
                        ${this.args.map { it.mem() }.joinToString("")}
                    };
                };
                """

            is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> ""
            is Expr.Data, is Expr.Delay -> ""
        }
    }
}

