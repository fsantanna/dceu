import java.io.File
import java.io.PushbackReader
import java.io.StringReader

fun op2f (op: String): String {
    return when (op) {
        "+" -> "op_plus"
        "-" -> "op_minus"
        "*" -> "op_mult"
        "/" -> "op_div"
        else -> TODO(op)
    }
}

data class Lex(var file: String, var lin: Int, var col: Int, val reader: PushbackReader)
data class Pos (val file: String, val lin: Int, val col: Int)

fun Lex.toPos (): Pos {
    return Pos(this.file, this.lin, this.col)
}

class Lexer (name_: String, reader_: StringReader) {
    val stack = ArrayDeque(listOf(Lex(name_, 1, 1, PushbackReader(reader_,2))))

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
    fun read2While (f: (x: Char)->Boolean): String? {
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
    fun read2While (x: Char): String? {
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
                '-' -> {
                    val (n2,x2) = read2()
                    if (iseof(n2) || x2!='-') {
                        unread2(n2)
                        return Pair('-', pos)
                    }
                    // found comment '--': ignore everything up to \n or EOF
                    val end = read2Until('\n')
                    if (end == null) {
                        unread2(-1)     // unread EOF
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
                (x in listOf('{','}',')','[',']', ',',';','=', '-','+','*','/')) -> yield(Tk.Fix(x.toString(), pos))
                (x == '(') -> {
                    val (n1,x1) = read2()
                    if (x1 in listOf('-','+','*','/')) {
                        val (n2,x2) = read2()
                        if (x2 == ')') {
                            yield(Tk.Id(op2f(x1.toString()), pos))
                        } else {
                            unread2(n2)
                            yield(Tk.Fix("(", pos))
                            yield(Tk.Fix(x1.toString(), pos))
                        }
                    } else {
                        unread2(n1)
                        yield(Tk.Fix("(", pos))
                    }
                }
                (x=='_' || x.isLetter()) -> {
                    var pay = x.toString()
                    while (true) {
                        val (n1,x1) = read2()
                        when {
                            iseof(n1) -> break
                            (x1 == '_') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                unread2(n1)
                                break
                            }
                        }
                        pay += x1
                    }
                    when {
                        keywords.contains(pay) -> yield(Tk.Fix(pay, pos))
                        (pay != "native") -> yield(Tk.Id(pay, pos))
                        else -> {
                            val (x1,_) = next()
                            if (x1!='(' && x1!='{') {
                                err(pos,"native token error : expected \"(\" or \"{\"")
                            }

                            var open = x1
                            var close = if (x1 == '(') ')' else '}'
                            var open_close = 1

                            var nat = x1.toString()
                            while (true) {
                                val (n2,x2) = read2()
                                when {
                                    iseof(n2) -> {
                                        err(pos, "native token error : expected \"$close\"")
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
                    var pay = x.toString()
                    while (true) {
                        val (n1,x1) = read2()
                        when {
                            iseof(n1) -> break
                            (x1 == '.') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                unread2(n1)
                                break
                            }
                        }
                        pay += x1
                    }
                    yield(Tk.Num(pay, pos))
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