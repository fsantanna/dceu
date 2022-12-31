import java.io.File
import java.io.PushbackReader
import java.io.Reader
import java.io.StringReader

data class Lex(var file: String, var lin: Int, var col: Int, val reader: PushbackReader)
data class Pos (val file: String, val lin: Int, val col: Int)

fun Lex.toPos (): Pos {
    return Pos(this.file, this.lin, this.col)
}

class Lexer (inps: List<Pair<Triple<String,Int,Int>,Reader>>) {
    val stack = ArrayDeque<Lex>()
    val comms = ArrayDeque<String>()

    init {
        for (inp in inps) {
            stack.addFirst(Lex(inp.first.first, inp.first.second, inp.first.third, PushbackReader(inp.second,2)))
        }
    }

    // TODO: reads 65535 after unreading -1
    fun iseof (n: Int): Boolean {
        return (n==-1 || n==65535)
    }

    fun read2 (): Pair<Int,Char> {
        val pos = stack.first()
        val n = pos.reader.read()
        val x = n.toChar()
        if (x == '\n') {
            pos.lin++; pos.col =1
        } else if (!iseof(n)) {
            pos.col++
        }
        return Pair(n,x)
    }
    fun unread2 (n: Int) {
        val pos = stack.first()
        val x = n.toChar()
        pos.reader.unread(n)
        when {
            iseof(n) -> {}
            (x == '\n') -> { pos.lin--; pos.col=0 }
            else -> pos.col--
        }
    }
    fun read2Until (f: (x: Char)->Boolean): String? {
        var ret = ""
        while (true) {
            val (n,x) = read2()
            when {
                iseof(n) -> {
                    unread2(n)
                    return null
                }
                f(x) -> break
                else -> ret += x
            }
        }
        return ret
    }
    fun read2Until (x: Char): String? {
        return read2Until { it == x }
    }
    fun read2While (f: (x: Char)->Boolean): String {
        var ret = ""
        while (true) {
            val (n,x) = read2()
            when {
                f(x) -> ret += x
                else -> {
                    unread2(n)
                    break
                }
            }
        }
        return ret
    }
    fun read2While (x: Char): String {
        return read2While { it == x }
    }

    fun next (): Pair<Char?, Pos> {
        while (true) {
            val lex = this.stack.first()
            val pos = lex.toPos()
            val (n1,x1) = read2()
            when (x1) {
                ' ', '\t', '\n' -> {}
                ';' -> {
                    val (n2,x2) = read2()
                    if (x2 == ';') {
                        val x3 = ";;" + read2While(';')
                        if (x3 == ";;") {
                            read2Until('\n')
                        } else {
                            var x4 = x3
                            outer@ while (true) {
                                if (this.comms.firstOrNull() == x4) {
                                    this.comms.removeFirst()
                                    if (this.comms.size == 0) {
                                        break
                                    }
                                } else {
                                    this.comms.addFirst(x4)
                                }
                                do {
                                    if (read2Until(';') == null) {
                                        break@outer
                                    }
                                    x4 = ";" + read2While(';')
                                } while (x4.length<=2 || x4.length<this.comms.first().length)
                            }
                        }
                    } else {
                        unread2(n2)
                    }
                }
                else -> {
                    return if (iseof(n1)) {
                        Pair(null, pos)
                    } else {
                        Pair(x1, pos)
                    }
                }
            }
        }
    }

    fun lex (): Sequence<Tk> = sequence {
        while (true) {
            val (x,pos) = next()
            when {
                (x == null) -> {
                    if (stack.size > 1) {
                        stack.removeFirst()
                    } else {
                        yield(Tk.Eof(pos))
                        break
                    }
                }
                (x in listOf('}','(',')','[',']',',','\$')) -> yield(Tk.Fix(x.toString(), pos))
                (x == '.') -> {
                    val (n1,x1) = read2()
                    if (x1 == '.') {
                        val (n2, x2) = read2()
                        if (x2 == '.') {
                            yield(Tk.Fix("...", pos))
                        } else {
                            yield(Tk.Fix(".", pos))
                            yield(Tk.Fix(".", pos))
                            unread2(n2)
                        }
                    } else {
                        yield(Tk.Fix(".", pos))
                        unread2(n1)
                    }
                }
                (x=='@' || x=='#') -> {
                    val (n1,x1) = read2()
                    when {
                        (x1 == '[') -> yield(Tk.Fix("$x[", pos))
                        (x == '@') -> err(pos, "operator error : expected \"@[\"")
                        (x == '#') -> { yield(Tk.Fix("#", pos)); unread2(n1) }
                        else -> error("impossible case")
                    }
                }
                (x in OPERATORS) -> {
                    val op = x + read2While { it in OPERATORS }
                    when {
                        (op == "=") -> yield(Tk.Fix(op, pos))
                        XCEU && (op == "->") -> yield(Tk.Fix(op, pos))
                        else -> yield(Tk.Op(op, pos))
                    }
                }
                (x == '{') -> {
                    val (n1,x1) = read2()
                    if (x1 !in OPERATORS) {
                        unread2(n1)
                        yield(Tk.Fix("{", pos))
                    } else {
                        val op = x1 + read2While { it in OPERATORS }
                        val (_,x2) = read2()
                        if (x2 != '}') {
                            if (op.length == 1) {
                                yield(Tk.Id("{$op}", pos, 0))
                                unread2(1)
                            } else {
                                err(pos, "operator error : expected \"}\"")
                            }
                        }
                        yield(Tk.Id("{$op}", pos, 0))
                    }
                }
                (x == ':') -> {
                    // no '_' b/c of C ids: X.Y -> X_Y
                    val tag = x + read2While { it.isLetterOrDigit() || it=='.' }
                    if (tag.length < 2) {
                        err(pos, "tag error : expected identifier")
                    }
                    yield(Tk.Tag(tag, pos))
                }
                (x.isLetter() || x=='_') -> {
                    val id = x + read2While { (it.isLetterOrDigit() || it in listOf('_','\'','?','!')) }
                    if (KEYWORDS.contains(id)) {
                         yield(Tk.Fix(id, pos))
                    } else {
                        yield(Tk.Id(id, pos, 0))
                    }
                }
                x.isDigit() -> {
                    val num = x + read2While { it=='.' || it.isLetterOrDigit() }
                    if (!XCEU) {
                        yield(Tk.Num(num, pos))
                    } else {
                        val l = num.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)".toRegex());
                        if (l.size==1 || l[1] !in listOf("ms","s","min","h")) {
                            yield(Tk.Num(num, pos))
                        } else {
                            val ms = l.chunked(2).map { (n,u) ->
                                val v = n.toInt()
                                when (u) {
                                    "ms"  -> v
                                    "s"   -> v * 1000
                                    "min" -> v * 1000 * 60
                                    "h"   -> v * 1000 * 60 * 60
                                    else -> { err(pos, "invalid time constant"); 0 }
                                }
                            }.sum()
                            yield(Tk.Clk(num, pos, ms))
                        }
                    }
                }
                (x == '`') -> {
                    val open = x + read2While('`')
                    var nat = ""
                    val tag = read2().let { (n2,x2) ->
                        if (x2 != ':') {
                            unread2(n2)
                            null
                        } else {
                            val tag = x2 + read2While { it.isLetterOrDigit() || it=='.' }
                            if (tag.length < 2) {
                                err(pos, "tag error : expected identifier")
                            }
                            tag
                        }
                    }
                    while (true) {
                        val (n2,x2) = read2()
                        when {
                            iseof(n2) -> {
                                err(stack.first().toPos(), "native error : expected \"$open\"")
                            }
                            (x2 == '`') -> {
                                val xxx = stack.first().toPos().let { Pos(it.file,it.lin,it.col-1) }
                                val close = x2 + read2While('`')
                                if (open == close) {
                                    break
                                } else {
                                    err(xxx, "native error : expected \"$open\"")
                                }
                            }
                        }
                        nat += x2
                    }
                    //println(":$pay:")
                    yield(Tk.Nat(nat, pos, tag))
                }
                (x == '^') -> {
                    val (n2,x2) = read2()
                    val (x3,ups) = if (x2 == '^') {
                        val (n3,x3) = read2()
                        Pair(x3,2)
                    } else {
                        Pair(x2, 1)
                    }
                    when {
                        (x3.isLetter() || x3 == '_') -> {
                            val id = x3 + read2While { (it.isLetterOrDigit() || it in listOf('_', '\'', '?', '!')) }
                            if (KEYWORDS.contains(id)) {
                                err(pos, "token ^ error : unexpected keyword")
                                yield(Tk.Fix(id, pos))
                            } else {
                                yield(Tk.Id(id, pos, ups))
                            }
                        }
                        (x3 != '[') -> {
                            err(pos, "token ^ error : expected \"[\"")
                        }
                        else -> {
                            val (n3, x3) = read2()
                            val file: String? = if (x3 == '"') {
                                val f = read2Until('"')
                                if (f == null) {
                                    err(pos, "token ^ error : unterminated \"")
                                }
                                f
                            } else {
                                unread2(n3)
                                null
                            }

                            val lin: Int? = if (file == null) {
                                read2While { it.isDigit() }.toIntOrNull()
                            } else {
                                val (n4, x4) = read2()
                                if (x4 == ',') {
                                    read2While { it.isDigit() }.let {
                                        if (it.isEmpty()) {
                                            err(pos, "invalid ^ token : expected number")
                                        }
                                        it.toInt()
                                    }
                                } else {
                                    unread2(n4)
                                    null
                                }
                            }
                            val col: Int? = if (lin == null) {
                                null
                            } else {
                                val (n5, x5) = read2()
                                if (x5 == ',') {
                                    read2While { it.isDigit() }.let {
                                        if (it.isEmpty()) {
                                            err(pos, "invalid ^ token : expected number")
                                        }
                                        it.toInt()
                                    }
                                } else {
                                    unread2(n5)
                                    null
                                }
                            }

                            if (file == null && lin == null) {
                                err(pos, "token ^ error")
                            }

                            val (_, x6) = read2()
                            if (x6 != ']') {
                                err(pos, "token ^ error : expected \"]\"")
                            }
                            val (n7, x7) = read2()
                            when {
                                iseof(n7) -> unread2(n7)
                                (x7 == '\n') -> {}  // skip leading \n
                                else -> unread2(n7) //err(pos, "token ^ error : expected end of line")
                            }

                            when {
                                (file != null && lin == null && col == null) -> {
                                    val f = File(file)
                                    if (!f.exists()) {
                                        err(pos, "token ^ error : file not found : $file")
                                    }
                                    stack.addFirst(Lex(file, 1, 1, PushbackReader(StringReader(f.readText()), 2)))
                                }

                                (lin != null) -> stack.first().let {
                                    it.file = if (file == null) it.file else file
                                    it.lin = lin
                                    it.col = if (col == null) 1 else col
                                }

                                else -> error("bug found")
                            }
                        }
                    }
                }
                (x == '\'') -> {
                    var c = '\''
                    val v = read2Until {
                        val brk = (it=='\'' && c!='\\')
                        c = it
                        brk
                    }
                    yield(Tk.Chr("'$v'", pos))
                }
                XCEU && (x == '"') -> {
                    var c = '"'
                    val v = read2Until {
                        val brk = (it=='"' && c!='\\')
                        c = it
                        brk
                    }
                    yield(Tk.Fix("#[", pos))
                    var i = 0
                    while (i < v!!.length) {
                        if (i > 0) {
                            yield(Tk.Fix(",", pos))
                        }
                        val z = v[i]
                        val zz = when {
                            (z == '\'') -> "\\'"
                            (z != '\\') -> z.toString()
                            else -> {
                                i++
                                z.toString() + v[i]
                            }
                        }
                        yield(Tk.Chr("'$zz'", pos))
                        i++
                    }
                    yield(Tk.Fix("]", pos))
                }
                else -> {
                    TODO("$x - $pos")
                }
            }
        }
    }
}