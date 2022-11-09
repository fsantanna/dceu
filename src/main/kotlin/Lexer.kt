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
            var (x1, l1, c1) = next()
            when {
                (x1 == null) -> break
                (x1 in listOf('{','}', '(',')', '[',']', ',',';','=','-')) -> yield(Tk.Fix(x1.toString(), l1, c1))
                (x1=='_' || x1.isLetter()) -> {
                    var pay = ""
                    var n1 = -1
                    while (true) {
                        pay += x1
                        val nx = reader.read2()
                        n1 = nx.first
                        x1 = nx.second
                        when {
                            iseof(n1) -> break
                            (x1 == '_') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                reader.unread2(n1)
                                break
                            }
                        }
                    }
                    if (keywords.contains(pay)) {
                        yield(Tk.Fix(pay, l1, c1))
                    } else {
                        yield(Tk.Id(pay, l1, c1))
                    }
                }
                x1.isDigit() -> {
                    var pay = ""
                    var n1 = -1
                    while (true) {
                        pay += x1
                        val nx = reader.read2()
                        n1 = nx.first
                        x1 = nx.second
                        when {
                            iseof(n1) -> break
                            (x1 == '.') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                reader.unread2(n1)
                                break
                            }
                        }
                    }
                    yield(Tk.Num(pay, l1, c1))
                }

                else -> {
                    TODO("$x1 - $l1 - $c1")
                }
            }
        }
        yield(Tk.Eof(lin, col))
    }
}