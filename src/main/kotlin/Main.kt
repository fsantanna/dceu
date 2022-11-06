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
    data class Err (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Eof (val lin_: Int, val col_: Int): Tk("", lin_, col_)
    data class Fix (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Id  (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Num (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
}

sealed class Expr (val tk: Tk) {
    data class Var   (val tk_: Tk.Id):  Expr(tk_)
    data class Num   (val tk_: Tk.Num): Expr(tk_)
    data class ECall (
        val tk_:   Tk,
        val f:     Expr,        // f
        val args:  List<Expr>,  // [_1,_2]
    ): Expr(tk_)
}

sealed class Stmt (val tk: Tk) {
    data class SCall (val tk_: Tk.Fix, val e: Expr.ECall): Stmt(tk_)
}

fun main () {
    val lexer = Lexer("anon", PushbackReader("{}".reader(),2))
    for (tk in lexer.lex()) {
        println(tk)
    }
}
