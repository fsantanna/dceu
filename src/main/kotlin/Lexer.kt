import java.io.PushbackReader

class Lexer (s: PushbackReader) {
    val s = s
    var lin = 1
    var col = 1

    fun read(): Triple<Char?, Int, Int> {
        while (true) {
            val n1 = s.read()
            if (n1 == -1) {
                return Triple(null, lin, col)
            }
            val x1 = n1.toChar()
            val (l, c) = Pair(lin, col)
            when (x1) {
                '\n' -> {
                    lin += 1
                    col = 1
                }

                ' ' -> {
                    col += 1
                }

                else -> {
                    col += 1
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
                (x1 in listOf('{', '}', '(', ')')) -> yield(Tk.Fix(x1.toString(), l1, c1))
                x1.isLetter() -> {
                    var pay = ""
                    var n1 = -1
                    while (true) {
                        pay += x1
                        n1 = s.read()
                        x1 = n1.toChar()
                        when {
                            (n1 == -1) -> break
                            (x1 == '_') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                s.unread(n1)
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

                else -> {
                    TODO(x1.toString())
                }
            }
        }
    }
}