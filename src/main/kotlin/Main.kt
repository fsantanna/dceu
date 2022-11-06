import java.util.*

val keywords: SortedSet<String> = sortedSetOf (
    "call",
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

fun exec (cmds: List<String>): Pair<Boolean,String> {
    //System.err.println(cmds.joinToString(" "))
    val p = ProcessBuilder(cmds)
        //.redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectErrorStream(true)
        .start()
    val ret = p.waitFor()
    val str = p.inputStream.bufferedReader().readText()
    return Pair(ret==0, str)
}

fun exec (cmd: String): Pair<Boolean,String> {
    return exec(cmd.split(' '))
}

fun main () {
    val lexer = Lexer("anon", "{}".reader())
    for (tk in lexer.lex()) {
        println(tk)
    }
}
