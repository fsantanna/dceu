package dceu

fun trap (f: ()->Unit): String {
    try {
        f()
        error("impossible case")
    } catch (e: Throwable) {
        return e.message!!
    }
}

fun Pos.isSameLine (oth: Pos): Boolean {
    return (this.file==oth.file && this.lin==oth.lin)
}

fun <T> T?.cond2 (f: (v:T)->String, g: (()->String)?): String {
    return when (this) {
        false, null -> if (g != null) g() else ""
        else  -> f(this)
    }
}

fun <T> T?.cond (f: (v:T)->String): String {
    return this.cond2(f) {""}
}

fun Boolean.toc (): String {
    return if (this) "1" else "0"
}

fun Expr.Call.main (): Expr.Proto {
    assert(this.tk.str == "main")
    return this.clo as Expr.Proto
}

fun Expr.has_block (): Boolean {
    return when (this) {
        is Expr.Proto, is Expr.Enum, is Expr.Data, is Expr.Delay -> false
        is Expr.Nat, is Expr.Acc, is Expr.Nil -> false
        is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> false
        is Expr.Do, is Expr.If, is Expr.Loop, is Expr.Catch, is Expr.Defer -> true
        is Expr.Export -> this.blk.es.any { it.has_block() }
        is Expr.Dcl -> this.src?.has_block() ?: false
        is Expr.Set -> this.src.has_block()
        is Expr.Pass -> this.e.has_block()
        is Expr.Break -> this.cnd.has_block() || (this.e?.has_block() ?: false)
        is Expr.Skip -> this.cnd.has_block()
        is Expr.Yield -> this.arg.has_block()
        is Expr.Resume -> this.arg.has_block()
        is Expr.Spawn -> this.args.any { it.has_block() }
        is Expr.Pub -> this.tsk?.has_block() ?: false
        is Expr.Toggle -> this.tsk.has_block() || this.on.has_block()
        is Expr.Tuple -> this.args.any { it.has_block() }
        is Expr.Vector -> this.args.any { it.has_block() }
        is Expr.Dict -> this.args.any { it.first.has_block() || it.second.has_block()}
        is Expr.Index -> this.col.has_block() || this.idx.has_block()
        is Expr.Call -> this.clo.has_block() || this.args.any { it.has_block() }
    }
}

fun Expr.is_innocuous (): Boolean {
    return when {
        this.is_constructor() -> true
        this is Expr.Acc -> true
        this is Expr.Index -> true
        else -> false
    }
}

fun Expr.is_constructor (): Boolean {
    return when {
        this.is_static() -> true
        else -> when (this) {
            is Expr.Tuple, is Expr.Vector, is Expr.Dict -> true
            else -> false
        }
    }
}

fun Expr.is_static (): Boolean {
    return when (this) {
        is Expr.Nil, is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> true
        else -> false
    }
}

fun Expr.is_lval (): Boolean {
    return when (this) {
        is Expr.Acc -> true
        is Expr.Index -> true
        is Expr.Pub -> true
        else -> false
    }
}

fun Expr.Proto.is_val (ups: Ups): Boolean {
    return (ups.pub[this].let { it is Expr.Dcl && it.tk.str=="val" })
}

fun Expr.base (ups: Ups): Expr {
    return when (this) {
        is Expr.Acc   -> this
        is Expr.Index -> this.col.base(ups)
        is Expr.Pub   -> TODO() //this.tsk?.base(ups) ?: ups.first(this) { it is Expr.Proto }!!
        else -> {
            println(this)
            TODO()
        }
    }
}
fun String.tag2c (): String {
    return this
        .drop(1)
        .replace('.','_')
        .replace('-','_')
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

fun Array<String>.cmds_opts () : Pair<List<String>,Map<String,String?>> {
    val cmds = this.filter { !it.startsWith("--") }
    val opts = this
        .filter { it.startsWith("--") }
        .map {
            if (it.contains('=')) {
                val (k,v) = Regex("(--.*)=(.*)").find(it)!!.destructured
                Pair(k,v)
            } else {
                Pair(it, null)
            }
        }
        .toMap()
    return Pair(cmds,opts)
}

fun String.idc (): String {
    return when {
        (this[0] == '{') -> {
            val MAP = mapOf(
                Pair('+', "plus"),
                Pair('-', "minus"),
                Pair('*', "asterisk"),
                Pair('/', "slash"),
                Pair('>', "greater"),
                Pair('<', "less"),
                Pair('=', "equals"),
                Pair('!', "exclaim"),
                Pair('|', "bar"),
                Pair('&', "ampersand"),
                Pair('#', "hash"),
            )
            /*"op_" +*/ this.drop(2).dropLast(2).toList().map { MAP[it] }.joinToString("_")
        }
        else -> {
            val MAP = mapOf(
                Pair('.', "_dot_"),
                Pair('-', "_dash_"),
                Pair('\'', "_plic_"),
                Pair('?', "_question_"),
                Pair('!', "_bang_"),
            )
            /*"id_" +*/ this.toList().map { MAP[it] ?: it }.joinToString("")
        }
    }
}
