import java.io.PushbackReader
import java.io.StringReader

class Lexer (name_: String, reader_: StringReader) {
    val name = name_
    val reader = PushbackReader(reader_,2)
    var lin = 1
    var col = 1

    fun read (): Triple<Char?, Int, Int> {
        while (true) {
            val n1 = this.reader.read()
            val x1 = n1.toChar()
            if (n1==-1 || n1==65535) {  // TODO: reads 65535 after reading -1 for the first time
                return Triple(null, this.lin, this.col)
            }
            val (l, c) = Pair(this.lin, this.col)
            when (x1) {
                '\n' -> {
                    this.lin += 1
                    this.col = 1
                }
                ' ' -> {
                    this.col += 1
                }
                '-' -> {
                    val n2 = this.reader.read()
                    val x2 = n2.toChar()
                    if (n2==-1 || x2!='-') {
                        val c = this.col
                        this.col += 1
                        this.reader.unread(n2)
                        return Triple('-', this.lin, c)
                    }
                    // found comment '--': ignore everything up to \n or EOF
                    while (true) {
                        this.col += 1
                        val n3 = this.reader.read()
                        val x3 = n3.toChar()
                        if (n3 == -1) {     // EOF stops comment
                            return Triple(null, this.lin, this.col)
                        }
                        if (x3 == '\n') {   // LN stops comment
                            this.lin += 1
                            this.col = 1
                            break
                        }
                    }
                }
                else -> {
                    this.col += 1
                    return Triple(x1, l, c)
                }
            }
        }
    }

    fun lex (): Sequence<Tk> = sequence {
        while (true) {
            var (x1, l1, c1) = read()
            when {
                (x1 == null) -> break
                (x1 in listOf('{','}', '(',')', '[',']', ',',';','=','-')) -> yield(Tk.Fix(x1.toString(), l1, c1))
                x1.isLetter() -> {
                    var pay = ""
                    var n1 = -1
                    while (true) {
                        pay += x1
                        n1 = reader.read()
                        x1 = n1.toChar()
                        when {
                            (n1 == -1) -> break
                            (x1 == '_') -> { col++ }
                            (x1.isLetterOrDigit()) -> { col++ }
                            else -> {
                                reader.unread(n1)
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
                        n1 = reader.read()
                        x1 = n1.toChar()
                        when {
                            (n1 == -1) -> break
                            (x1 == '.') -> { col++ }
                            (x1.isLetterOrDigit()) -> { col++ }
                            else -> {
                                reader.unread(n1)
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