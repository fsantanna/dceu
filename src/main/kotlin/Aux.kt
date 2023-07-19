package dceu

fun Pos.isSameLine (oth: Pos): Boolean {
    return (this.file==oth.file && this.lin==oth.lin)
}

fun <T> T?.cond (f: (v:T)->String): String {
    return when (this) {
        null  -> ""
        false -> ""
        else  -> f(this)
    }
}

fun Expr.is_innocuous (): Boolean {
    return when (this) {
        is Expr.Tuple, is Expr.Vector, is Expr.Dict, is Expr.Index, is Expr.Acc,
        /*is Expr.Nil,*/ is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> true
        else -> false
    }
}

fun Expr.is_lval (): Boolean {
    return when (this) {
        is Expr.Acc -> true
        is Expr.Index -> true
        else -> false
    }
}

fun Expr.base (): Expr.Acc {
    return when (this) {
        is Expr.Acc   -> this
        is Expr.Index -> this.col.base()
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

fun String.id2c (): String {
    return if (this[0] == '{') {
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
        "op_" + this.drop(2).dropLast(2).toList().map { MAP[it] }.joinToString("_")
    } else {
        val MAP = mapOf(
            Pair('.', "_dot_"),
            Pair('-', "_dash_"),
            Pair('\'', "_plic_"),
            Pair('?', "_question_"),
            Pair('!', "_bang_"),
        )
        this.toList().map { MAP[it] ?: it }.joinToString("")
    }
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
