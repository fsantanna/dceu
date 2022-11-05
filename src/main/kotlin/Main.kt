import java.io.PushbackReader
import java.util.*

val keywords: SortedSet<String> = sortedSetOf (
    "active", "await", "break", "call", "catch", "else", "emit", "func",
    "if", "ifs", "in", "input", "loop", "native", "new", "Null", "output",
    "pause", "resume", "return", "set", "spawn", "task", "throw", "type",
    "var", "defer", "every", "par", "parand", "paror", "pauseon", "until",
    "watching", "where", "with",
)

sealed class Tk (val str: String, val lin: Int, val col: Int) {
    data class Fix (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Id  (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
}

fun main () {
    val reader = PushbackReader("{}".reader())
    for (tk in lexer(reader)) {
        println(tk)
    }
}
