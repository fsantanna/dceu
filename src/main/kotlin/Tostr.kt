fun Expr.tostr (): String {
    return when (this) {
        is Expr.Dcl   -> "var " + this.tk.str
        is Expr.Acc   -> this.tk.str
        is Expr.Num   -> this.tk.str
        is Expr.Tuple -> "[" + this.args.map { it.tostr() }.joinToString(",") + "]"
        is Expr.Index -> this.col.tostr() + "[" + this.idx.tostr() + "]"
        is Expr.Call  -> this.f.tostr() + "(" + this.args.map { it.tostr() }.joinToString(",") + ")"
    }
}

fun List<Expr>.tostr (): String {
    return this.map { it.tostr() }.joinToString("\n") + "\n"
}
