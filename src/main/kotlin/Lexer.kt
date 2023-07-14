package dceu

import java.io.File
import java.io.PushbackReader
import java.io.Reader
import java.io.StringReader
import java.lang.Integer.max

data class Lex(var file: String, var lin: Int, var col: Int, val reader: PushbackReader)
data class Pos (val file: String, val lin: Int, val col: Int)

fun Lex.toPos (): Pos {
    return Pos(this.file, this.lin, this.col)
}

fun FileX (path: String): File {
    val xpath = if (path[0] != '@') path else {
        PATH + "/" + path.drop(2)
    }
    return File(xpath)
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
            pos.lin++; pos.col=1
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
            else -> pos.col = max(0,pos.col-1)    // TODO: should remeber col from previous line
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
    fun read2While2 (f: (x: Char, y: Char)->Boolean): String {
        var ret = ""
        while (true) {
            val (n1,x) = read2()
            val (n2,y) = read2()
            when {
                f(x,y) -> {
                    unread2(n2)
                    ret += x
                }
                else -> {
                    unread2(n2)
                    unread2(n1)
                    break
                }
            }
        }
        return ret
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
                            yield(Tk.Id("...", pos, 0))
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
                        (x1 !in OPERATORS) -> {
                            yield(Tk.Op(x.toString(), pos))
                            unread2(n1)
                        }
                        else -> {
                            val op = x.toString() + x1 + read2While { it in OPERATORS }
                            yield(Tk.Op(op, pos))
                        }
                    }
                }
                (x in OPERATORS) -> {
                    val op = x + read2While { it in OPERATORS }
                    when {
                        (op == "=") -> yield(Tk.Fix(op, pos))
                        else -> yield(Tk.Op(op, pos))
                    }
                }
                (x == '{') -> {
                    val (n1,x1) = read2()
                    if (x1 != '{') {
                        unread2(n1)
                        yield(Tk.Fix("{", pos))
                    } else {
                        val op = read2While { it != '}' }
                        val (_,x2) = read2()
                        val (_,x3) = read2()
                        if (!(x2=='}' && x3=='}')) {
                            err(pos, "operator error : expected \"}\"")
                        }
                        when {
                            op.all  { it in OPERATORS } -> yield(Tk.Id("{{$op}}", pos, 0))
                            op.none { it in OPERATORS } -> yield(Tk.Op(op, pos, 0))
                            else -> err(pos, "operator error : invalid identifier")
                        }
                    }
                }
                (x == ':') -> {
                    // no '_' b/c of C ids: X.Y -> X_Y
                    val tag = x + read2While2 { x,y -> x.isLetterOrDigit() || ((x=='.' || x=='-') && y.isLetter()) }
                    if (tag.length < 2) {
                        err(pos, "tag error : expected identifier")
                    }
                    if (tag.count { it=='.' } > 3) {
                        err(pos, "tag error : excess of '.' : max hierarchy of 4")
                    }
                    yield(Tk.Tag(tag, pos))
                }
                (x.isLetter() || x=='_') -> {
                    val id = x + read2While2 { x,y -> x.isLetterOrDigit() || x in listOf('_','\'','?','!') || (x=='-' && y.isLetter()) }
                    when {
                        KEYWORDS.contains(id) -> yield(Tk.Fix(id, pos))
                        else -> yield(Tk.Id(id, pos, 0))
                    }
                }
                x.isDigit() -> {
                    val num = x + read2While { it=='.' || it.isLetterOrDigit() }
                    yield(Tk.Num(num, pos))
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
                    val (x3,upv) = if (x2 == '^') {
                        val (n3,x3) = read2()
                        Pair(x3,2)
                    } else {
                        Pair(x2, 1)
                    }
                    when {
                        (x3.isLetter() || x3 == '_') -> {
                            val id = x3 + read2While2 { x,y -> x.isLetterOrDigit() || x in listOf('_','\'','?','!') || (x=='-' && y.isLetter()) }
                            if (KEYWORDS.contains(id)) {
                                err(pos, "token ^ error : unexpected keyword")
                                yield(Tk.Fix(id, pos))
                            } else {
                                yield(Tk.Id(id, pos, upv))
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
                                    val f = FileX(file)
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
                    val (n2,x2) = read2()
                    if (iseof(n2)) {
                        err(stack.first().toPos(), "char error : expected '")
                    }
                    val c = if (x2 != '\\') x2.toString() else {
                        val (n3,x3) = read2()
                        if (iseof(n3)) {
                            err(stack.first().toPos(), "char error : expected '")
                        }
                        x2.toString()+x3
                    }
                    val (n3,x3) = read2()
                    if (iseof(n3) || x3!='\'') {
                        err(stack.first().toPos(), "char error : expected '")
                    }
                    yield(Tk.Chr("'$c'", pos))
                }
                else -> {
                    TODO("$x - $pos")
                }
            }
        }
    }
}
