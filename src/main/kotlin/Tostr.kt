fun Pos.pre (): String {
    return "^[${this.lin},${this.col}]"
}

fun Expr.tostr (pre: Boolean = false): String {
    return when (this) {
        is Expr.Func   -> this.tk.str + this.isFake.cond{" :nopub"} +  " (" + this.args.map { it.str }.joinToString(",") + ") " + this.body.tostr(pre)
        is Expr.Block  -> if (this.isFake) {
            assert(this.es.size == 1)
            this.es[0].tostr(pre)
        } else {
            (this.tk.str=="do").cond{"do "} + "{\n" + this.es.tostr(pre) + "}"
        }
        is Expr.Dcl    -> "var " + this.tk.str
        is Expr.Set    -> "set " + this.dst.tostr(pre) + " = " + this.src.tostr(pre)
        is Expr.If     -> "if " + this.cnd.tostr(pre) + " " + this.t.tostr(pre) + " else " + this.f.tostr(pre)
        is Expr.While  -> "while " + this.cnd.tostr(pre) + " " + this.body.tostr(pre)
        is Expr.Throw  -> "throw " + this.ex.tostr(pre)
        is Expr.Catch  -> "catch " + this.cnd.tostr(pre) + " " + this.body.tostr(pre)
        is Expr.Defer  -> "defer " + this.body.tostr(pre)

        is Expr.Coros  -> "coroutines(${this.max.cond { it.tostr(pre) }})"
        is Expr.Coro   -> "coroutine " + this.task.tostr(pre)
        is Expr.Spawn  -> "spawn " + this.coros.cond{"in "+it.tostr(pre)+", "} + this.call.tostr(pre)
        is Expr.Iter   -> "while ${this.loc.str} in ${this.coros.tostr(pre)} ${this.body.es[1].tostr(pre)}"
        is Expr.Bcast  -> "broadcast " + this.evt.tostr(pre)
        is Expr.Yield  -> "yield " + this.arg.tostr(pre)
        is Expr.Resume -> "resume " + this.call.tostr(pre)
        is Expr.Pub    -> if (this.coro == null) "pub" else (this.coro.tostr(pre) + ".pub")

        is Expr.Nat    -> "native " + "```" + (this.tk_.tag ?: "") + " " + this.tk.str + "```"
        is Expr.Acc    -> this.tk.str
        is Expr.EvtErr -> this.tk.str
        is Expr.Nil    -> this.tk.str
        is Expr.Tag    -> this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Tuple  -> "[" + this.args.map { it.tostr(pre) }.joinToString(",") + "]"
        is Expr.Dict   -> "@[" + this.args.map { "(${it.first.tostr(pre)},${it.second.tostr(pre)})" }.joinToString(",") + "]"
        is Expr.Index  -> this.col.tostr(pre) + "[" + this.idx.tostr(pre) + "]"
        is Expr.Call   -> this.f.tostr(pre) + "(" + this.args.map { it.tostr(pre) }.joinToString(",") + ")"

        is Expr.XSeq -> error("bug found")
    }.let { if (pre) this.tk.pos.pre()+it else it }
}

fun List<Expr>.tostr (pre: Boolean=false): String {
    return this.map { it.tostr(pre) }.joinToString("\n") + "\n"
}
