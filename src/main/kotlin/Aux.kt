package dceu

fun <K,V> List<Map<K,V>>.union (): Map<K,V> {
    return this.fold(emptyMap()) { acc, value -> acc + value }
}

fun <K,V> Expr.dn_collect (f: (Expr)->Map<K,V>?): Map<K,V> {
    val v = f(this)
    if (v == null) {
        return emptyMap()
    }
    return v + when (this) {
        is Expr.Proto  -> this.blk.dn_collect(f) + this.pars.map { it.dn_collect(f) }.union()
        is Expr.Do     -> this.es.map { it.dn_collect(f) }.union()
        is Expr.Escape -> this.e?.dn_collect(f) ?: emptyMap()
        is Expr.Group  -> this.es.map { it.dn_collect(f) }.union()
        is Expr.Dcl    -> this.src?.dn_collect(f) ?: emptyMap()
        is Expr.Set    -> this.dst.dn_collect(f) + this.src.dn_collect(f)
        is Expr.If     -> this.cnd.dn_collect(f) + this.t.dn_collect(f) + this.f.dn_collect(f)
        is Expr.Loop   -> this.blk.dn_collect(f)
        is Expr.Drop   -> this.e.dn_collect(f)

        is Expr.Catch  -> this.blk.dn_collect(f)
        is Expr.Defer  -> this.blk.dn_collect(f)

        is Expr.Yield  -> this.e.dn_collect(f)
        is Expr.Resume -> this.co.dn_collect(f) + this.args.map { it.dn_collect(f) }.union()

        is Expr.Spawn  -> (this.tsks?.dn_collect(f) ?: emptyMap()) + this.tsk.dn_collect(f) + this.args.map { it.dn_collect(f) }.union()
        is Expr.Delay  -> emptyMap()
        is Expr.Pub    -> this.tsk?.dn_collect(f) ?: emptyMap()
        is Expr.Toggle -> this.tsk.dn_collect(f) + this.on.dn_collect(f)
        is Expr.Tasks  -> this.max.dn_collect(f)

        is Expr.Tuple  -> this.args.map { it.dn_collect(f) }.union()
        is Expr.Vector -> this.args.map { it.dn_collect(f) }.union()
        is Expr.Dict   -> this.args.map { it.first.dn_collect(f) + it.second.dn_collect(f) }.union()
        is Expr.Index  -> this.col.dn_collect(f) + this.idx.dn_collect(f)
        is Expr.Call   -> this.clo.dn_collect(f) + this.args.map { it.dn_collect(f) }.union()

        is Expr.Acc, is Expr.Data, is Expr.Nat,
        is Expr.Nil, is Expr.Tag, is Expr.Bool,
        is Expr.Char, is Expr.Num -> emptyMap()
    }
}

fun Expr.dn_visit (f: (Expr)->Unit) {
    this.dn_collect { f(it) ; emptyMap<Unit,Unit>() }
}

fun trap (f: ()->Unit): String {
    try {
        f()
        error("impossible case")
    } catch (e: Throwable) {
        return e.message!!
    }
}

fun Pos.is_same_line (oth: Pos): Boolean {
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
