fun Expr.tostr (): String {
    return when (this) {
        is Expr.Block  -> (if (this.tk.str=="do") "do " else "") + "{\n" + this.es.tostr() + "}\n"
        is Expr.Dcl    -> "var " + this.tk.str
        is Expr.Set    -> "set " + this.dst.tostr() + " = " + this.src.tostr()
        is Expr.If     -> "if " + this.cnd.tostr() + " " + this.t.tostr() + "else " + this.f.tostr()
        is Expr.While  -> "while " + this.cnd.tostr() + " " + this.body.tostr()
        is Expr.Func   -> this.tk.str + " (" + this.args.map { it.str }.joinToString(",") + ") " + this.body.tostr()
        is Expr.Throw  -> "throw " + this.ex.tostr()
        is Expr.Catch  -> "catch " + this.catch.tostr() + " " + this.body.tostr()
        is Expr.Defer  -> "defer " + this.body.tostr()

        is Expr.Coro   -> "coroutine " + this.task.tostr()
        is Expr.Bcast  -> "broadcast " + this.arg.tostr()
        is Expr.Resume -> "resume " + this.call.tostr()
        is Expr.Yield  -> "yield (" + this.arg.tostr() + ")"
        is Expr.Spawn  -> "spawn " + this.call.tostr() + (if (this.coros==null) "" else " in"+this.coros.tostr())
        is Expr.Coros  -> "coroutines()"
        is Expr.Iter   -> "while ${this.loc.str} in ${this.coros.tostr()} ${this.body.es[1].tostr()}"

        is Expr.Nat    -> "native " + "```" + (this.tk_.tag ?: "") + " " + this.tk.str + "```"
        is Expr.Acc    -> this.tk.str
        is Expr.Nil    -> this.tk.str
        is Expr.Tag    -> this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Tuple  -> "[" + this.args.map { it.tostr() }.joinToString(",") + "]"
        is Expr.Index  -> this.col.tostr() + "[" + this.idx.tostr() + "]"
        is Expr.Call   -> this.f.tostr() + "(" + this.args.map { it.tostr() }.joinToString(",") + ")"

        is Expr.XSeq -> error("bug found")
    }
}

fun List<Expr>.tostr (): String {
    return this.map { it.tostr() }.joinToString("\n") + "\n"
}
