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

fun Expr.ups (): Map<Expr,Expr> {
    fun Expr.map (l: List<Expr>): Map<Expr,Expr> {
        return l.map { it.ups() }.fold(l.map { Pair(it,this) }.toMap(), {a,b->a+b})
    }
    return when (this) {
        is Expr.Block  -> this.map(this.es)
        is Expr.Dcl    -> emptyMap()
        is Expr.Set    -> this.map(listOf(this.dst, this.src))
        is Expr.If     -> this.map(listOf(this.cnd, this.t, this.f))
        is Expr.While  -> this.map(listOf(this.cnd, this.body))
        is Expr.Func   -> this.map(listOf(this.body))
        is Expr.Catch  -> this.map(listOf(this.catch, this.body))
        is Expr.Throw  -> this.map(listOf(this.ex))
        is Expr.Defer  -> this.map(listOf(this.body))

        is Expr.Coro   -> this.map(listOf(this.task))
        is Expr.Bcast  -> this.map(listOf(this.arg))
        is Expr.Resume -> this.map(listOf(this.call))
        is Expr.Yield  -> this.map(listOf(this.arg))
        is Expr.Spawn  -> this.map(listOf(this.call) + listOfNotNull(this.coros))
        is Expr.Coros  -> emptyMap()
        is Expr.Iter   -> this.map(listOf(this.coros,this.body))

        is Expr.Nat    -> emptyMap()
        is Expr.Acc    -> emptyMap()
        is Expr.Nil    -> emptyMap()
        is Expr.Tag    -> emptyMap()
        is Expr.Bool   -> emptyMap()
        is Expr.Num    -> emptyMap()
        is Expr.Tuple  -> this.map(this.args)
        is Expr.Index  -> this.map(listOf(this.col, this.idx))
        is Expr.Call   -> this.map(listOf(this.f)) + this.map(this.args)
    }
}
