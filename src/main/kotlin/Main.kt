import java.util.*

var N = 1

val keywords: SortedSet<String> = sortedSetOf (
    "break", "catch", "do", "else", "false", "func",
    "if", "loop", "nil", "set", "throw", "true", "var"
)

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof (val pos_: Pos): Tk("", pos_)
    data class Fix (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Id  (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Num (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Nat (val str_: String, val pos_: Pos): Tk(str_, pos_)
}

sealed class Expr (val n: Int, val tk: Tk) {
    data class Do    (val tk_: Tk.Fix, val catch: Expr?, val es: List<Expr>) : Expr(N++, tk_)
    data class Dcl   (val tk_: Tk.Id):  Expr(N++, tk_)
    data class Set   (val tk_: Tk.Fix, val dst: Expr, val src: Expr): Expr(N++, tk_)
    data class If    (val tk_: Tk, val cnd: Expr, val t: Expr, val f: Expr): Expr(N++, tk_)
    data class Loop  (val tk_: Tk, val body: Expr.Do): Expr(N++, tk_)
    data class Func  (val tk_: Tk, val args: List<Tk.Id>, val body: Expr.Do): Expr(N++, tk_)
    data class Throw (val tk_: Tk, val ex: Expr, val arg: Expr): Expr(N++, tk_)

    data class Nat   (val tk_: Tk): Expr(N++, tk_)
    data class Acc   (val tk_: Tk.Id): Expr(N++, tk_)
    data class Nil   (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Bool  (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Num   (val tk_: Tk.Num): Expr(N++, tk_)
    data class Tuple (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Index (val tk_: Tk, val col: Expr, val idx: Expr): Expr(N++, tk_)
    data class Call  (val tk_: Tk, val f: Expr, val args: List<Expr>): Expr(N++, tk_)
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
