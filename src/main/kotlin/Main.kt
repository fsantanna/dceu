import java.io.StringReader

fun main () {
    val reader = "{}".reader()
    for (tk in lexer(reader)) {
        println(tk)
    }
}

sealed class Tk (val str: String, val lin: Int, val col: Int) {
    data class Fix (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
}

fun lexer (s: StringReader): Sequence<Tk> = sequence {
    var lin = 1
    var col = 1

    fun read (): Triple<Char?,Int,Int> {
        while (true) {
            val n1 = s.read()
            if (n1 == -1) {
                return Triple(null,lin,col)
            }
            val x1 = n1.toChar()
            val (l, c) = Pair(lin, col)
            when (x1) {
                '\n' -> {
                    lin += 1
                    col = 1
                }
                ' '  -> {
                    col += 1
                }
                else -> {
                    col += 1
                    return Triple(x1,l,c)
                }
            }
        }
    }

    var (x1,l1,c1) = read()

    while (x1 != null) {
        var ok2 = false
        when {
            (x1 in listOf('{','}',')')) -> {
                yield(Tk.Fix(x1.toString(), l1, c1))
            }
            (x1 == '(') -> {
                val (x2,l2,c2) = read()
                if (x2 == ')') {
                    yield(Tk.Fix("()", l1, c1))
                } else {
                    yield(Tk.Fix("(", l1, c1))
                    ok2 = true
                    x1 = x2
                    l1 = l2
                    c1 = c2
                }
            }
            else -> {
                error("TODO")
            }
        }
        if (!ok2) {
            val (x2,l2,c2) = read()
            x1 = x2
            l1 = l2
            c1 = c2
        }
    }
}