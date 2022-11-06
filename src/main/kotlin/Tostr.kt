fun Expr.tostr (): String {
    return when (this) {
        is Expr.Var   -> this.tk.str
        is Expr.Num   -> this.tk.str
        is Expr.Tuple -> "[" + this.args.map { it.tostr() }.joinToString(",") + "]"
        is Expr.Index -> this.col.tostr() + "[" + this.idx.tostr() + "]"
        is Expr.ECall -> this.f.tostr() + "(" + this.args.map { it.tostr() }.joinToString(",") + ")"
    }
}

fun Stmt.tostr (): String {
    return when (this) {
        is Stmt.Nop   -> ""
        is Stmt.Seq   -> this.s1.tostr() + this.s2.tostr()
        is Stmt.SCall -> "call " + this.e.tostr() + "\n"
    }
}
