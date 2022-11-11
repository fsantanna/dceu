import java.io.File
import java.io.PushbackReader
import java.io.Reader
import java.io.StringReader

data class Lex(var file: String, var lin: Int, var col: Int, val reader: PushbackReader)
data class Pos (val file: String, val lin: Int, val col: Int)

fun Lex.toPos (): Pos {
    return Pos(this.file, this.lin, this.col)
}

class Lexer (inps: List<Pair<String,Reader>>) {
    val stack = ArrayDeque<Lex>()

    init {
        for (inp in inps) {
            stack.addFirst(Lex(inp.first, 1, 1, PushbackReader(inp.second,2)))
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
        this.err(tk, "expected $str : have $have")
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
            (x == '\n') -> { pos.lin--; pos.col =0 }
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
            //println("$l + $c: $x1")
            when (x1) {
                ' ', '\n' -> {}
                ';' -> {
                    val (n2,x2) = read2()
                    if (x2 == ';') {
                        read2Until('\n')
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
                (x in listOf('}','(',')','[',']', ',')) -> yield(Tk.Fix(x.toString(), pos))
                (x in operators) -> {
                    val op = x + read2While { it in operators }
                    if (op == "=") {
                        yield(Tk.Fix(op, pos))
                    } else {
                        yield(Tk.Op(op, pos))
                    }
                }
                (x == '{') -> {
                    val (n1,x1) = read2()
                    if (x1 !in operators) {
                        unread2(n1)
                        yield(Tk.Fix("{", pos))
                    } else {
                        val op = x1 + read2While { it in operators }
                        val (_,x2) = read2()
                        if (x2 != '}') {
                            if (op.length == 1) {
                                yield(Tk.Id("{$op}", pos))
                                unread2(1)
                            } else {
                                err(pos, "operator error : expected \"}\"")
                            }
                        }
                        yield(Tk.Id("{$op}", pos))
                    }
                }
                (x == '@') -> {
                    val tag = x + read2While { it=='_' || it.isLetterOrDigit() }
                    if (tag.length < 2) {
                        err(pos, "tag error : expected identifier")
                    }
                    yield(Tk.Tag(tag, pos))
                }
                (x=='_' || x.isLetter()) -> {
                    val id = x + read2While { it=='_' || it.isLetterOrDigit() }
                    when {
                        keywords.contains(id) -> yield(Tk.Fix(id, pos))
                        (id != "native") -> yield(Tk.Id(id, pos))
                        else -> {
                            val (x1,_) = next()
                            if (x1!='(' && x1!='{') {
                                err(pos,"native error : expected \"(\" or \"{\"")
                            }

                            var open = x1
                            var close = if (x1 == '(') ')' else '}'
                            var open_close = 1

                            var nat = x1.toString()
                            while (true) {
                                val (n2,x2) = read2()
                                when {
                                    iseof(n2) -> {
                                        err(pos, "native error : expected \"$close\"")
                                    }
                                    (x2 == open) -> open_close++
                                    (x2 == close) -> {
                                        open_close--
                                        if (open_close == 0) {
                                            nat += x2
                                            break
                                        }
                                    }
                                }
                                nat += x2
                            }
                            //println("#$pay#")
                            yield(Tk.Nat(nat, pos))
                        }
                    }
                }
                x.isDigit() -> {
                    val num = x + read2While { it=='.' || it.isLetterOrDigit() }
                    yield(Tk.Num(num, pos))
                }
                (x == '^') -> {
                    val (_,x2) = read2()
                    if (x2 != '[') {
                        err(pos, "token ^ error : expected \"[\"")
                    }

                    val (n3,x3) = read2()
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
                        read2While { it.isDigit() }?.toIntOrNull()
                    } else {
                        val (n4,x4) = read2()
                        if (x4 == ',') {
                            read2While { it.isDigit() }.let {
                                if (it.isNullOrEmpty()) {
                                    err(pos, "invalid ^ token : expected number")
                                }
                                it!!.toInt()
                            }
                        } else {
                            unread2(n4)
                            null
                        }
                    }
                    val col: Int? = if (lin == null) {
                        null
                    } else {
                        val (n5,x5) = read2()
                        if (x5 == ',') {
                            read2While { it.isDigit() }.let {
                                if (it.isNullOrEmpty()) {
                                    err(pos, "invalid ^ token : expected number")
                                }
                                it!!.toInt()
                            }
                        } else {
                            unread2(n5)
                            null
                        }
                    }

                    if (file==null && lin==null) {
                        err(pos, "token ^ error")
                    }

                    val (_,x6) = read2()
                    if (x6 != ']') {
                        err(pos, "token ^ error : expected \"]\"")
                    }
                    val (n7,x7) = read2()
                    when {
                        iseof(n7) -> unread2(n7)
                        (x7 == '\n') -> {}  // skip leading \n
                        else -> err(pos, "token ^ error : expected end of line")
                    }

                    when {
                        (file!=null && lin==null && col==null) -> {
                            val f = File(file)
                            if (!f.exists()) {
                                err(pos, "token ^ error : file not found : $file")
                            }
                            stack.addFirst(Lex(file, 1, 1, PushbackReader(StringReader(f.readText()), 2)))
                        }
                        (lin != null) -> stack.first().let {
                            it.file = if (file==null) it.file else file
                            it.lin  = lin
                            it.col  = if (col==null) it.col else col
                        }
                        else -> error("bug found")
                    }
                }

                else -> {
                    TODO("$x - $pos")
                }
            }
        }
    }
}