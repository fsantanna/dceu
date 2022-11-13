fun Pos.isSameLine (oth: Pos): Boolean {
    return (this.file==oth.file && this.lin==oth.lin)
}

fun Tk.Id.fromOp (): String {
    val MAP = mapOf(
        Pair('+', "plus"),
        Pair('-', "minus"),
        Pair('*', "mul"),
        Pair('/', "div"),
        Pair('>', "gt"),
        Pair('<', "lt"),
        Pair('=', "eq"),
        Pair('!', "not"),
        Pair('|', "or"),
        Pair('&', "and"),
    )
    return if (this.str[0] != '{') this.str else {
        "op_" + this.str.drop(1).dropLast(1).toList().map { MAP[it] }.joinToString("_")
    }
}

fun String.noSpecial (): String {
    val MAP = mapOf(
        Pair('\'', "_plic"),
        Pair('?', "_question"),
        Pair('!', "_bang"),
    )
    return this.toList().map { MAP[it] ?: it }.joinToString("")
}

fun Expr.Func.isTask (): Boolean {
    return (this.tk.str == "task")
}

fun err (pos: Pos, str: String) {
    error(pos.file + " : (lin ${pos.lin}, col ${pos.col}) : $str")
}
fun err (tk: Tk, str: String) {
    err(tk.pos, str)
}
fun err_expected (tk: Tk, str: String) {
    val have = when {
        (tk is Tk.Eof) -> "end of file"
        else -> '"' + tk.str + '"'
    }
    err(tk, "expected $str : have $have")
}

fun Expr.copy (): Expr {
    return when (this) {
        is Expr.Block  -> Expr.Block(this.tk_, this.es.map { it.copy() })
        is Expr.Dcl    -> Expr.Dcl(this.tk_)
        is Expr.Set    -> Expr.Set(this.tk_, this.dst.copy(), this.src.copy())
        is Expr.If     -> Expr.If(this.tk_, this.cnd.copy(), this.t.copy(), this.f.copy())
        is Expr.While  -> Expr.While(this.tk_, this.cnd.copy(), this.body.copy())
        is Expr.Func   -> Expr.Func(this.tk_, this.args.map { it.copy() }, this.body.copy())
        is Expr.Catch  -> Expr.Catch(this.tk_, this.catch.copy(), this.body.copy())
        is Expr.Throw  -> Expr.Throw(this.tk_, this.ex.copy(), this.arg.copy())
        is Expr.Spawn  -> Expr.Spawn(this.tk_, this.task.copy())
        is Expr.Resume -> Expr.Resume(this.tk_, this.call.copy())
        is Expr.Yield  -> Expr.Yield(this.tk_, this.arg.copy())
        is Expr.Defer  -> Expr.Defer(this.tk_, this.body.copy())

        is Expr.Nat    -> Expr.Nat(this.tk_)
        is Expr.Acc    -> Expr.Acc(this.tk_)
        is Expr.Nil    -> Expr.Nil(this.tk_)
        is Expr.Tag    -> Expr.Tag(this.tk_)
        is Expr.Bool   -> Expr.Bool(this.tk_)
        is Expr.Num    -> Expr.Num(this.tk_)
        is Expr.Tuple  -> Expr.Tuple(this.tk_, this.args.map { it.copy() })
        is Expr.Index  -> Expr.Index(this.tk_, this.col.copy(), this.idx.copy())
        is Expr.Call   -> Expr.Call(this.tk_, this.f.copy(), this.args.map { it.copy() })
    }
}