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
    return (this.file==oth.file && this.lin==oth.lin && this.brks==oth.brks)
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

fun Expr.Call.main (): Expr.Proto {
    assert(this.tk.str == "main")
    return this.clo as Expr.Proto
}

fun Expr.has_block (): Boolean {
    return when (this) {
        is Expr.Proto, is Expr.Skip, is Expr.Data, is Expr.Delay -> false
        is Expr.Nat, is Expr.Acc, is Expr.Nil -> false
        is Expr.Tag, is Expr.Bool, is Expr.Char, is Expr.Num -> false
        is Expr.Do, is Expr.If, is Expr.Loop, is Expr.Catch, is Expr.Defer -> true
        is Expr.Group -> this.es.any { it.has_block() }
        is Expr.Dcl -> this.src?.has_block() ?: false
        is Expr.Set -> this.src.has_block()
        is Expr.Break -> this.e?.has_block() ?: false
        is Expr.Yield -> this.e.has_block()
        is Expr.Resume -> this.args.any { it.has_block() }
        is Expr.Spawn -> this.args.any { it.has_block() }
        is Expr.Pub -> this.tsk?.has_block() ?: false
        is Expr.Toggle -> this.tsk.has_block() || this.on.has_block()
        is Expr.Tasks -> this.max.has_block()
        is Expr.Tuple -> this.args.any { it.has_block() }
        is Expr.Vector -> this.args.any { it.has_block() }
        is Expr.Dict -> this.args.any { it.first.has_block() || it.second.has_block()}
        is Expr.Index -> this.col.has_block() || this.idx.has_block()
        is Expr.Call -> this.clo.has_block() || this.args.any { it.has_block() }
    }
}

fun Expr.Proto.id (outer: Expr.Do, ups: Ups): String {
    return ups.pub[this].let {
        when {
            (it !is Expr.Dcl) -> this.n.toString()
            (it.src != this) -> error("bug found")
            else -> it.idtag.first.str.idc() + (ups.first(this) { it is Expr.Do } != outer).cond { "_${this.n}" }
        }
    }
}

fun Expr.is_innocuous (): Boolean {
    return when (this) {
        is Expr.Do -> this.es.lastOrNull()?.is_innocuous() ?: true
        is Expr.If -> this.t.is_innocuous() || this.f.is_innocuous()
        is Expr.Loop -> this.blk.es.any { it is Expr.Break && (it.e?.is_innocuous() ?: false) }
        is Expr.Catch -> this.blk.is_innocuous()
        is Expr.Nil -> false
        is Expr.Acc -> true
        is Expr.Index -> true
        else -> this.is_constructor()
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

fun String.quote (n: Int): String {
    return this
        .replace('\n',' ')
        .replace('"','.')
        .replace('\\','.')
        .let {
            if (it.length<=n) it else it.take(n-3)+"..."
        }

}

fun err (pos: Pos, str: String): Nothing {
    error(pos.file + " : (lin ${pos.lin}, col ${pos.col}) : $str")
}
fun err (tk: Tk, str: String): Nothing {
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
            this.drop(2).dropLast(2).toList().map { MAP[it] }.joinToString("_")
        }
        else -> {
            val MAP = mapOf(
                Pair(':', ""),
                Pair('.', "_"),
                Pair('-', "_dash_"),
                Pair('\'', "_plic_"),
                Pair('?', "_question_"),
                Pair('!', "_bang_"),
            )
            this.toList().map { MAP[it] ?: it }.joinToString("")
        }
    }
}
