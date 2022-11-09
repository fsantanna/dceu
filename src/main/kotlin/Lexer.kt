import java.io.PushbackReader
import java.io.StringReader

class Lexer (name_: String, reader_: StringReader) {
    val name = name_
    val reader = PushbackReader(reader_,2)
    var lin = 1
    var col = 1

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
                (x in listOf('{','}', '(',')', '[',']', ',',';','=','-')) -> yield(Tk.Fix(x.toString(), l, c))
                (x=='_' || x.isLetter()) -> {
                    var pay = x.toString()
                    while (true) {
                        val (n,x) = reader.read2()
                        when {
                            iseof(n) -> break
                            (x == '_') -> {}
                            (x.isLetterOrDigit()) -> {}
                            else -> {
                                reader.unread2(n)
                                break
                            }
                        }
                        pay += x
                    }
                    when {
                        keywords.contains(pay) -> yield(Tk.Fix(pay, l, c))
                        (pay != "native") -> yield(Tk.Id(pay, l, c))
                        else -> {
                            val (x,_,_) = next()
                            if (x!='(' && x!='{') {
                                yield(Tk.Err("unterminated native token", l, c))
                                return@sequence
                            }

                            var open = x
                            var close = if (x == '(') ')' else '}'
                            var open_close = 1

                            var nat = ""
                            while (true) {
                                val (n,x) = reader.read2()
                                when {
                                    iseof(n) -> {
                                        yield(Tk.Err("unterminated native token", l, c))
                                        return@sequence
                                    }
                                    (x == open) -> open_close++
                                    (x == close) -> {
                                        open_close--
                                        if (open_close == 0) {
                                            break
                                        }
                                    }
                                }
                                nat += x
                            }
                            //println("#$pay#")
                            yield(Tk.Nat(nat, l, c))
                        }
                    }
                }
                x.isDigit() -> {
                    var pay = x.toString()
                    while (true) {
                        val (n,x) = reader.read2()
                        when {
                            iseof(n) -> break
                            (x == '.') -> {}
                            (x.isLetterOrDigit()) -> {}
                            else -> {
                                reader.unread2(n)
                                break
                            }
                        }
                        pay += x
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