sealed class Tk (val str: String, val lin: Int, val col: Int) {
    data class Fix (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
}

fun main () {
    val reader = "{}".reader()
    for (tk in lexer(reader)) {
        println(tk)
    }
}
