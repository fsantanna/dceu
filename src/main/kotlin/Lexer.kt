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
                iseof(n) -> return null
                f(x) -> break
                else -> ret += x
            }
        }
        return ret
    }
    fun read2Until (x: Char): String? {
        return read2Until { it == x }
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
                                err(pos,"lexer error : unterminated native token")
                            }

                            var open = x1
                            var close = if (x1 == '(') ')' else '}'
                            var open_close = 1

                            var nat = x1.toString()
                            while (true) {
                                val (n2,x2) = read2()
                                when {
                                    iseof(n2) -> {
                                        err(pos, "lexer error : unterminated native token")
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
                        err(pos, "lexer error : expected \"[\"")
                    }

                    val (n3,x3) = read2()
                    if (iseof(n3)) {
                        err(pos, "lexer error : unterminated ^ token")
                    }
                    val file: String? = if (x3 != '"') null else {
                        val f = read2Until('"')
                        if (f == null) {
                            err(pos, "lexer error : unterminated ^ token")
                        }
                        f
                    }

                    val (_,x4) = read2()
                    val lin: Int? = if (x4 != ',') null else {
                        read2Until { !it.isDigit() }?.toIntOrNull()
                    }
                    val col: Int? = if (lin == null) null else {
                        val (_,x5) = read2()
                        if (x5 != ',') null else {
                            read2Until { !it.isDigit() }?.toIntOrNull()
                        }
                    }

                    val (_,x5) = read2()
                    if (x5 != ']') {
                        err(pos, "lexer error : unterminated ^ token")
                    }

                    when {
                        (file!=null && lin==null && col==null) -> {
                            val f = File(file)
                            if (!f.exists()) {
                                err(pos, "lexer error : file not found : $file")
                            }
                            stack.addFirst(Lex(file, 1, 1, PushbackReader(StringReader(f.readText()), 2)))
                        }
                        (file!=null && lin!=null && col!=null) -> stack.first().let {
                            it.lin = lin
                            it.col = col
                        }
                        else -> err(pos, "lexer error : invalid ^ token")
                    }
                }

                else -> {
                    TODO("$x - $pos")
                }
            }
        }
    }
}