package dceu

fun Pos.isSameLine (oth: Pos): Boolean {
    return (this.file==oth.file && this.lin==oth.lin)
}

fun Tk.dump (): String {
    return "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})\n"
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
        is Expr.Pub, is Expr.Tuple, is Expr.Vector, is Expr.Dict, is Expr.Index, is Expr.Acc,
        is Expr.EvtErr, /*is Expr.Nil,*/ is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> true
        else -> false
    }
}

fun Expr.is_lval (): Boolean {
    return when (this) {
        is Expr.Pub -> true
        is Expr.Acc -> true
        is Expr.Index -> true
        is Expr.Export -> this.body.es.last().is_lval()
        else -> false
    }
}

fun Expr.base (): Expr {
    return when (this) {
        is Expr.Acc   -> this
        is Expr.Index -> this.col.base()
        is Expr.Pub   -> this.x
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

fun String.id2c (n: Int?): String {
    fun String.aux (): String {
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
    return if (this.length>=3 && this.first()=='_' && this.last()=='_') {
        '_' + this.drop(1).dropLast(1).aux() + '_'
    } else {
        this.aux()
    }.let {
        if (n==null || this in GLOBALS || this.startsWith("ceu_")) {
            it
        } else {
            it + "_" + n
        }
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
