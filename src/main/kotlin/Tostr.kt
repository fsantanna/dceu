fun Expr.tostr (): String {
    return when (this) {
        is Expr.Block -> (if (this.tk.str=="do") "do " else "") + "{\n" + this.es.tostr() + "}\n"
        is Expr.Dcl   -> "var " + this.tk.str
        is Expr.Set   -> "set " + this.dst.tostr() + " = " + this.src.tostr()
        is Expr.If    -> "if " + this.cnd.tostr() + " " + this.t.tostr() + "else " + this.f.tostr()
        is Expr.Loop  -> "loop " + this.body.tostr()
        is Expr.Func  -> "func (" + this.args.map { it.str }.joinToString(",") + ") " + this.body.tostr()
        is Expr.Throw -> "throw (" + this.ex.tostr() + "," + this.arg.tostr() + ")"
        is Expr.Catch -> "catch " + this.catch.tostr() + " " + this.body.tostr()
        is Expr.Nat   -> "native " + this.tk.str

        is Expr.Acc   -> this.tk.str
        is Expr.Nil   -> this.tk.str
        is Expr.Bool  -> this.tk.str
        is Expr.Num   -> this.tk.str
        is Expr.Tuple -> "[" + this.args.map { it.tostr() }.joinToString(",") + "]"
        is Expr.Index -> this.col.tostr() + "[" + this.idx.tostr() + "]"
        is Expr.Call  -> this.f.tostr() + "(" + this.args.map { it.tostr() }.joinToString(",") + ")"
    }
}

fun List<Expr>.tostr (): String {
    return this.map { it.tostr() }.joinToString("\n") + "\n"
}
