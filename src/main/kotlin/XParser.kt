fun xeq (tk: Tk, e1: Expr, e2: Expr): Expr.Call {
    return Expr.Call(tk, xacc(tk.pos,"{==}"), listOf(e1, e2))
}

fun xor (tk: Tk, e1: Expr, e2: Expr): Expr.Do {
    // do { val :tmp x=$e1 ; if x -> x -> $e2 }
    fun xid (): Tk.Id {
        return Tk.Id("ceu_${e1.n}", tk.pos, 0)
    }
    return Expr.Do(Tk.Fix("do", tk.pos), listOf(
        Expr.Dcl(Tk.Fix("val",tk.pos), xid(), true, null, true, e1),
        Expr.If(Tk.Fix("if",tk.pos),
            Expr.Acc(xid()),
            Expr.Do(tk, listOf(Expr.Acc(xid()))),
            Expr.Do(tk, listOf(e2))
        )
    ))
}

fun xand (tk: Tk, e1: Expr, e2: Expr): Expr.Do {
    // do { val :tmp x=$e1 ; if x -> $e2 -> x }
    fun xid (): Tk.Id {
        return Tk.Id("ceu_${e1.n}", tk.pos, 0)
    }
    return Expr.Do(Tk.Fix("do", tk.pos), listOf(
        Expr.Dcl(Tk.Fix("val",tk.pos), xid(), true, null, true, e1),
        Expr.If(Tk.Fix("if",tk.pos),
            Expr.Acc(xid()),
            Expr.Do(tk, listOf(e2)),
            Expr.Do(tk, listOf(Expr.Acc(xid())))
        )
    ))
}

fun xnot (tk: Tk, e: Expr): Expr.If {
    // if $e { false } else { true }
    return Expr.If(Tk.Fix("if",tk.pos), e,
        Expr.Do(tk, listOf(xbool(tk.pos,false))),
        Expr.Do(tk, listOf(xbool(tk.pos,true)))
    )
}

fun xnil (pos: Pos): Expr.Nil {
    return Expr.Nil(Tk.Fix("nil", pos))
}

fun xacc (pos: Pos, id: String): Expr.Acc {
    return Expr.Acc(Tk.Id(id, pos, 0))
}

fun xbool (pos: Pos, v: Boolean): Expr.Bool {
    return Expr.Bool(Tk.Fix(if (v) "true" else "false",pos))
}

fun xnum (pos: Pos, n: Int): Expr.Num {
    return Expr.Num(Tk.Num(n.toString(), pos))
}

fun xtag (pos: Pos, tag: String): Expr.Tag {
    return Expr.Tag(Tk.Tag(tag, pos))
}

fun xnat (pos: Pos, nat: String): Expr.Nat {
    return Expr.Nat(Tk.Nat(nat, pos, null))
}

fun xop (op: Tk.Op, e1: Expr, e2: Expr): Expr {
    return when (op.str) {
        "or" -> xor(op, e1, e2)
        "and" -> xand(op, e1, e2)
        "is?" -> Expr.Call(op, xacc(op.pos,"is'"), listOf(e1, e2))
        "is-not?" -> Expr.Call(op, xacc(op.pos,"is-not'"), listOf(e1, e2))
        "in?" -> Expr.Call(op, xacc(op.pos,"in'"), listOf(e1, e2))
        "in-not?" -> Expr.Call(op, xacc(op.pos,"in-not'"), listOf(e1, e2))
        else -> Expr.Call(op, xacc(op.pos,"{${op.str}}"), listOf(e1,e2))
    }
}

fun xstatus (tk: Tk.Fix, e: Expr): Expr.Call {
    return Expr.Call(tk, xacc(tk.pos,"status"), listOf(e))
}

fun xawait (tk: Tk.Fix, awt: Await): Expr {
    fun aux (now: Boolean, cnd: Expr): Expr.Do {
        val nn = N
        fun xid(): Tk.Id {
            return Tk.Id("ceu_cnd_$nn", tk.pos, 0)
        }
        fun xtype(): Expr.Call {
            return Expr.Call(tk, xacc(tk.pos, "type"), listOf(Expr.Acc(xid())))
        }
        /*
            if !awt.now { yield () }
            loop {
                var $cnd = $awt.cnd
                ifs {
                    type($cnd) == :x-task {
                        set $cnd = (status($cnd) == :terminated)
                    }
                    type($cnd) == :x-track {
                        set $cnd = (detrack($cnd) == nil)
                    }
                }
            } until (not (not $cnd)) {
                yield ()
            }
        */
        return Expr.Do(
            Tk.Fix("do", tk.pos),
            (if (now) emptyList() else listOf(
                Expr.Yield(tk, xnil(tk.pos))
            )) + listOf(
                Expr.Loop(
                    tk, nn, Expr.Do(
                        tk, listOf(
                            //xprintln(tk,xtag(tk.pos,":1")),
                            Expr.Dcl(Tk.Fix("var", tk.pos), xid(), false, null, true, cnd),
                            Expr.If(
                                tk, xeq(tk, xtype(), xtag(tk.pos, ":x-task")),
                                Expr.Do(
                                    tk, listOf(
                                        Expr.Set(
                                            tk, Expr.Acc(xid()), xeq(
                                                tk,
                                                xstatus(tk, Expr.Acc(xid())),
                                                xtag(tk.pos, ":terminated")
                                            )
                                        )
                                    )
                                ),
                                Expr.Do(
                                    tk, listOf(
                                        Expr.If(
                                            tk,
                                            xeq(tk, xtype(), xtag(tk.pos, ":x-task")),
                                            Expr.Do(
                                                tk, listOf(
                                                    Expr.Set(
                                                        tk, Expr.Acc(xid()),
                                                        xeq(
                                                            tk,
                                                            Expr.Call(
                                                                tk,
                                                                xacc(tk.pos, "detrack"),
                                                                listOf(Expr.Acc(xid()))
                                                            ),
                                                            xnil(tk.pos)
                                                        )
                                                    )
                                                )
                                            ),
                                            Expr.Do(
                                                tk, listOf(
                                                    xnil(tk.pos)
                                                )
                                            )
                                        ),
                                    )
                                )
                            ),
                            //xprintln(tk,xtag(tk.pos,":2")),
                            Expr.If(
                                tk, xnot(tk, xnot(tk, Expr.Acc(xid()))),
                                Expr.Do(tk, listOf(Expr.XBreak(tk, nn))),
                                Expr.Do(tk, listOf(xnil(tk.pos)))
                            ),
                            //xprintln(tk,xtag(tk.pos,":3")),
                            Expr.Yield(tk, xnil(tk.pos)),
                            //xprintln(tk,xtag(tk.pos,":9"))
                        )
                    )
                )
            )
        )
    }

    return when {
        (awt.cnd != null) -> {  // await evt==x | await trk | await coro
            aux(awt.now, awt.cnd)
        }
        (awt.tag != null) -> {   // await :key
            /*
                export [evt] {
                    val evt ${awt.tag}
                    await (evt is? ${awt.tag}) and $xcnd
                }
            */
            Expr.Export(tk, listOf("evt"),
                Expr.Do(tk, listOf(
                    Expr.Dcl(Tk.Fix("val",tk.pos), Tk.Id("evt",tk.pos,0), false, Tk.Tag(awt.tag.first.tk.str,tk.pos), true, xnil(tk.pos)),
                    aux(awt.now, xand(tk,
                        xop(Tk.Op("is?",tk.pos), Expr.EvtErr(Tk.Fix("evt",tk.pos)), awt.tag.first),
                        if (awt.tag.second == null) xbool(tk.pos,true) else awt.tag.second!!
                    ))
                ))
            )
        }
        (awt.spw != null) -> { // await spawn T()
            /*
                do {
                    val x = $e
                    await :check-now (status(x) == :terminated)
                    `ceu_acc = ceu_mem->$x.Dyn->Bcast.X.frame->X.pub;`
                }
            */
            val nn = N
            fun xid (): Tk.Id {
                return Tk.Id("ceu_spw_$nn", tk.pos, 0)
            }
            Expr.Do(Tk.Fix("do",tk.pos), listOf(
                Expr.Dcl(Tk.Fix("val",tk.pos), xid(), false, null, true, awt.spw),
                aux(true, xeq(tk,
                    xstatus(tk, Expr.Acc(xid())),
                    xtag(tk.pos,":terminated")
                )),
                xnat(tk.pos, "ceu_acc = ceu_mem->ceu_spw_$nn.Dyn->Bcast.X.frame->X.pub;")
            ))
        }
        (awt.clk != null) -> { // await 5s
            val nn = N
            fun xid (): Tk.Id {
                return Tk.Id("ceu_ms_$nn", tk.pos, 0)
            }
            /*
                do {
                    var ms = $awt.clk.fold(0) { acc,(e,tag) ->
                        acc + when (tag.) {
                            ":h"   -> 1000*60*60
                            ":min" -> 1000*60
                            ":s"   -> 1000
                            ":ms"  -> 1
                            else   -> error("impossible case")
                        }
                    }
                    loop until ms <= 0 {
                        await (evt is? :frame)
                        set ms = ms - evt.0
                    }
                }
            */
            Expr.Do(Tk.Fix("do",tk.pos), listOf(
                Expr.Dcl(Tk.Fix("var",tk.pos), xid(), false, null, true,
                    awt.clk.fold(xnum(tk.pos,0) as Expr) { acc,nxt ->
                        val (e,tag) = nxt
                        Expr.Call(tk, xacc(tk.pos,"{+}"), listOf(acc,
                            Expr.Call(tk, xacc(tk.pos,"{*}"), listOf(e,
                                when (tag.str) {
                                    ":h"   -> xnum(tk.pos,1000*60*60)
                                    ":min" -> xnum(tk.pos,1000*60)
                                    ":s"   -> xnum(tk.pos,1000)
                                    ":ms"  -> xnum(tk.pos,1)
                                    else   -> error("impossible case")
                                }
                            ))
                        ))
                    },
                ),
                Expr.Loop(tk, nn, Expr.Do(tk, listOf(
                    Expr.If(tk,
                        Expr.Call(tk, xacc(tk.pos,"{<=}"), listOf(Expr.Acc(xid()), xnum(tk.pos,0))),
                        Expr.Do(tk, listOf(Expr.XBreak(tk, nn))),
                        Expr.Do(tk, listOf(xnil(tk.pos)))
                    ),
                    aux(false,
                        Expr.Call(tk, xacc(tk.pos,"is'"), listOf(
                            Expr.EvtErr(Tk.Fix("evt", tk.pos)),
                            xtag(tk.pos, ":frame")
                        ))
                    ),
                    Expr.Set(tk, Expr.Acc(xid()),
                        Expr.Call(tk, xacc(tk.pos,"{-}"), listOf(
                            Expr.Acc(xid()),
                            Expr.Index(tk, Expr.EvtErr(Tk.Fix("evt",tk.pos)), xnum(tk.pos,0))
                        ))
                    )
                )))
            ))
        }
        else -> error("bug found")
    }
    //println(ret.tostr())
}

fun xspawn (tk: Tk.Fix, blk: Expr.Do): Expr.Spawn {
    val task = Expr.Proto(Tk.Fix("task", tk.pos), Pair(null,true), emptyList(), blk)
    // spawn (task () :fake { blk }) ()
    return Expr.Spawn(tk, null, Expr.Call(tk, task, emptyList()))
}
fun xprintln (tk: Tk, e: Expr): Expr.Call {
    return Expr.Call(tk, xacc(tk.pos,"println"), listOf(e))
}