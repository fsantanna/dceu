package dceu

val union = "union"

class Mem  (val outer: Expr.Do, val ups: Ups) {
    fun expr (e: Expr): Pair<String, String> {
        return when (e) {
            is Expr.Export -> this.expr(e.body)
            is Expr.Do -> {
                val (fs, ss) = e.es.map { this.expr(it) }.unzip()
                if (ups.pub[e] is Expr.Export) {
                    Pair(fs.joinToString(""), ss.joinToString(""))
                } else {
                    Pair("", """
                        struct { // BLOCK
                            CEU_Block block_${e.n};
                            ${fs.joinToString("")}
                            $union {
                                ${ss.joinToString("")}
                            };
                        };
                    """)
                }
            }

            is Expr.Dcl -> {
                val id = e.id.str.id2c(e.n)
                val xsrc = if (e.src == null) Pair("","") else this.expr(e.src)
                val dcl = """
                    struct { // DCL
                        ${if (id in listOf("evt", "_")) "" else {
                            """
                            CEU_Value ${id};
                            CEU_Block* _${id}_; // can't be static b/c recursion
                            """
                        }}
                    };
                """
                Pair(xsrc.first + dcl, xsrc.second)
            }

            is Expr.Set -> {
                val xdst = this.expr(e.dst)
                val xsrc = this.expr(e.src)
                Pair(xdst.first + xsrc.first, """
                    struct { // SET
                        CEU_Value set_${e.n};
                        $union {
                            ${xdst.second}
                            ${xsrc.second}
                        };
                    };
                """)
            }

            is Expr.If -> {
                val xcnd = this.expr(e.cnd)
                val xt = this.expr(e.t)
                val xf = this.expr(e.f)
                Pair(xcnd.first + xt.first + xf.first, """
                    $union { // IF
                        ${xcnd.second}
                        ${xt.second}
                        ${xf.second}
                    };
                """)
            }

            is Expr.Loop -> this.expr(e.body)
            is Expr.Catch -> {
                val xcnd = this.expr(e.cnd)
                val xbdy = this.expr(e.body)
                Pair(xcnd.first + xbdy.first, """
                    $union { // CATCH
                        ${xcnd.second}
                        ${xbdy.second}
                    };
                """)
            }

            is Expr.Defer -> {
                val xe = this.expr(e.body)
                Pair(xe.first + """
                    int defer_${e.n};                    
                """, xe.second)
            }
            is Expr.Pass -> this.expr(e.e)
            is Expr.Drop -> this.expr(e.e)

            is Expr.Spawn -> {
                val xtsk = if (e.tasks == null) Pair("", "") else this.expr(e.tasks)
                val xcal = this.expr(e.call)
                Pair(xtsk.first + xcal.first, """
                    struct { // SPAWN
                        ${e.tasks.cond { "CEU_Value tasks_${e.n};" }}
                        $union {
                            ${xtsk.second}
                            ${xcal.second}
                        };
                    };
                """)
            }

            is Expr.Bcast -> {
                val xxin = this.expr(e.xin)
                val xevt = this.expr(e.evt)
                Pair(xxin.first + xevt.first, """
                    struct { // BCAST
                        CEU_Value evt_${e.n};
                        $union {
                            ${xxin.second}
                            ${xevt.second}
                        };
                    };
                """)
            }

            is Expr.Yield -> this.expr(e.arg)
            is Expr.Resume -> this.expr(e.call)
            is Expr.Toggle -> {
                val xtsk = this.expr(e.task)
                val xon = this.expr(e.on)
                Pair(xtsk.first + xon.first, """
                    struct { // TOGGLE
                        CEU_Value on_${e.n};
                        $union {
                            ${xtsk.second}
                            ${xon.second}
                        };
                    };
                """)
            }

            is Expr.Pub -> this.expr(e.x)

            is Expr.Tuple -> {
                val (fs, ss) = e.args.map { this.expr(it) }.unzip()
                Pair(fs.joinToString(""), """
                    struct { // TUPLE
                        CEU_Dyn* tup_${e.n};
                        $union {
                            ${ss.joinToString("")}
                        };
                    };
                """)
            }

            is Expr.Vector -> {
                val (fs, ss) = e.args.map { this.expr(it) }.unzip()
                Pair(fs.joinToString(""), """
                    struct { // VECTOR
                        CEU_Dyn* vec_${e.n};
                        $union {
                            ${ss.joinToString("")}
                        };
                    };
                """)
            }

            is Expr.Dict -> {
                val (fs1, ss1) = e.args.map { it.first  }.map { this.expr(it) }.unzip()
                val (fs2, ss2) = e.args.map { it.second }.map { this.expr(it) }.unzip()
                Pair(fs1.joinToString("") + fs2.joinToString(""), """
                    struct { // DICT
                        CEU_Dyn* dict_${e.n};
                        CEU_Value key_${e.n};
                        $union {
                            ${ss1.joinToString("")}
                            ${ss2.joinToString("")}
                        };
                    };
                """)
            }

            is Expr.Index -> {
                val xcol = this.expr(e.col)
                val xidx = this.expr(e.idx)
                Pair(xcol.first + xidx.first, """
                    struct { // INDEX
                        CEU_Value idx_${e.n};
                        $union {
                            ${xcol.second}
                            ${xidx.second}
                        };
                    };
                """)
            }

            is Expr.Call -> {
                val xpro = this.expr(e.proto)
                val (fs, ss) = e.args.map { this.expr(it) }.unzip()
                Pair(xpro.first + fs.joinToString(""), """
                    struct { // CALL
                        ${e.args.mapIndexed { i, _ -> "CEU_Value arg_${i}_${e.n};\n" }.joinToString("")}
                        $union {
                            ${xpro.second}
                            ${ss.joinToString("")}
                        };
                    };
                """)
            }

            is Expr.Nat, is Expr.Acc, is Expr.EvtErr, is Expr.Nil, is Expr.Tag,
            is Expr.Bool, is Expr.Char, is Expr.Num, is Expr.Self, is Expr.Proto,
            is Expr.Enum, is Expr.Data, is Expr.XBreak -> Pair("", "")
        }
    }
}
