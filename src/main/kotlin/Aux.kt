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

fun String.tag2c (): String {
    return this
        .drop(1)
        .replace('.','_')
        .replace('-','_')
}

fun String.id2c (): String {
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
            return "op_" + this.drop(1).dropLast(1).toList().map { MAP[it] }.joinToString("_")
        } else {
            val MAP = mapOf(
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
