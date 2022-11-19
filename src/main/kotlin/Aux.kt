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
        is Expr.Dcl    -> Expr.Dcl(this.tk_, this.init)
        is Expr.Set    -> Expr.Set(this.tk_, this.dst.copy(), this.src.copy())
        is Expr.If     -> Expr.If(this.tk_, this.cnd.copy(), this.t.copy(), this.f.copy())
        is Expr.While  -> Expr.While(this.tk_, this.cnd.copy(), this.body.copy())
        is Expr.Func   -> Expr.Func(this.tk_, this.args.map { it.copy() }, this.body.copy())
        is Expr.Catch  -> Expr.Catch(this.tk_, this.catch.copy(), this.body.copy())
        is Expr.Throw  -> Expr.Throw(this.tk_, this.ex.copy())
        is Expr.Defer  -> Expr.Defer(this.tk_, this.body.copy())

        is Expr.Coro   -> Expr.Coro(this.tk_, this.task.copy())
        is Expr.Bcast  -> Expr.Bcast(this.tk_, this.arg.copy())
        is Expr.Resume -> Expr.Resume(this.tk_, this.call.copy())
        is Expr.Yield  -> Expr.Yield(this.tk_, this.arg.copy())
        is Expr.Spawn  -> Expr.Spawn(this.tk_, this.coros?.copy(), this.call.copy())
        is Expr.Coros  -> Expr.Coros(this.tk_)
        is Expr.Iter   -> Expr.Iter(this.tk_, this.loc, this.coros.copy(), this.body.copy())

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
