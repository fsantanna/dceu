import java.io.PushbackReader
import java.io.StringReader

class Lexer (name_: String, reader_: StringReader) {
    val name = name_
    val reader = PushbackReader(reader_,2)
    var lin = 1
    var col = 1

    fun err (lin: Int, col: Int, str: String) {
        error(this.name + " : (lin $lin, col $col) : $str")
    }
    fun err (tk: Tk, str: String) {
        err(tk.lin, tk.col, str)
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

    fun PushbackReader.read2 (): Pair<Int,Char> {
        val n = this.read()
        val x = n.toChar()
        if (x == '\n') {
            lin++; col=1
        } else if (!iseof(n)) {
            col++
        }
        return Pair(n,x)
    }
    fun PushbackReader.unread2 (n: Int) {
        val x = n.toChar()
        this.unread(n)
        when {
            iseof(n) -> {}
            (x == '\n') -> { lin--; col=0 }
            else -> col--
        }
    }

    fun next (): Triple<Char?, Int, Int> {
        while (true) {
            val (l, c) = Pair(this.lin, this.col)
            val (n1,x1) = this.reader.read2()
            //println("$l + $c: $x1")
            when (x1) {
                ' ', '\n' -> {}
                '-' -> {
                    val (n2,x2) = this.reader.read2()
                    if (iseof(n2) || x2!='-') {
                        this.reader.unread2(n2)
                        return Triple('-', l, c)
                    }
                    // found comment '--': ignore everything up to \n or EOF
                    while (true) {
                        val (n3,x3) = this.reader.read2()
                        when {
                            (x3 == '\n') -> break   // LN stops comment
                            iseof(n3) -> {         // EOF stops comment
                                this.reader.unread2(n3)
                                break
                            }
                        }
                    }
                }
                else -> {
                    return if (iseof(n1)) {
                        Triple(null, l, c)
                    } else {
                        Triple(x1, l, c)
                    }
                }
            }
        }
    }

    fun lex (): Sequence<Tk> = sequence {
        while (true) {
            val (x,l,c) = next()
            when {
                (x == null) -> break
                (x in listOf('{','}',')','[',']', ',',';','=', '-','+','*','/')) -> yield(Tk.Fix(x.toString(), l, c))
                (x == '(') -> {
                    val (n1,x1) = reader.read2()
                    if (x1 in listOf('-','+','*','/')) {
                        val (n2,x2) = reader.read2()
                        if (x2 == ')') {
                            yield(Tk.Id(x1.toString(), l, c))
                        } else {
                            reader.unread2(n2)
                            yield(Tk.Fix("(", l, c))
                            yield(Tk.Fix(x1.toString(), l, c))
                        }
                    } else {
                        reader.unread2(n1)
                        yield(Tk.Fix("(", l, c))
                    }
                }
                (x=='_' || x.isLetter()) -> {
                    var pay = x.toString()
                    while (true) {
                        val (n1,x1) = reader.read2()
                        when {
                            iseof(n1) -> break
                            (x1 == '_') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                reader.unread2(n1)
                                break
                            }
                        }
                        pay += x1
                    }
                    when {
                        keywords.contains(pay) -> yield(Tk.Fix(pay, l, c))
                        (pay != "native") -> yield(Tk.Id(pay, l, c))
                        else -> {
                            val (x1,_,_) = next()
                            if (x1!='(' && x1!='{') {
                                err(l,c,"unterminated native token")
                            }

                            var open = x1
                            var close = if (x1 == '(') ')' else '}'
                            var open_close = 1

                            var nat = x1.toString()
                            while (true) {
                                val (n2,x2) = reader.read2()
                                when {
                                    iseof(n2) -> {
                                        err(l,c, "unterminated native token")
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
                            yield(Tk.Nat(nat, l, c))
                        }
                    }
                }
                x.isDigit() -> {
                    var pay = x.toString()
                    while (true) {
                        val (n1,x1) = reader.read2()
                        when {
                            iseof(n1) -> break
                            (x1 == '.') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                reader.unread2(n1)
                                break
                            }
                        }
                        pay += x1
                    }
                    yield(Tk.Num(pay, l, c))
                }

                else -> {
                    TODO("$x - $l - $c")
                }
            }
        }
        yield(Tk.Eof(lin, col))
    }
}