fun Expr.tostr (): String {
    return when (this) {
        is Expr.Var   -> this.tk.str
        is Expr.Num   -> this.tk.str
        is Expr.ECall -> {
            this.f.tostr() + "(" + this.args.map { it.tostr() }.joinToString(",") + ")"
        }
    }
}
