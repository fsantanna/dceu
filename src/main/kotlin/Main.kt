import java.io.File
import java.io.Reader
import java.util.*

val XCEU = false
//val XCEU = true
var N = 1

val keywords: SortedSet<String> = (setOf (
    "broadcast", "catch", "defer", "do", "else", "false", "func", "if", "nil",
    "resume", "set", "spawn", "task", "throw", "true", "var", "yield", "while"
) + if (!XCEU) setOf() else setOf (
    "and", "not", "or"
)).toSortedSet()

val operators = setOf('+', '-', '*', '/', '>', '<', '=', '!', '|', '&')

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof (val pos_: Pos): Tk("", pos_)
    data class Fix (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Tag (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Op  (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Id  (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Num (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Nat (val str_: String, val pos_: Pos): Tk(str_, pos_)
}
sealed class Expr (val n: Int, val tk: Tk) {
    data class Block  (val tk_: Tk.Fix, val es: List<Expr>) : Expr(N++, tk_)
    data class Dcl    (val tk_: Tk.Id):  Expr(N++, tk_)
    data class Set    (val tk_: Tk.Fix, val dst: Expr, val src: Expr): Expr(N++, tk_)
    data class If     (val tk_: Tk.Fix, val cnd: Expr, val t: Expr.Block, val f: Expr.Block): Expr(N++, tk_)
    data class While  (val tk_: Tk.Fix, val cnd: Expr, val body: Expr.Block): Expr(N++, tk_)
    data class Func   (val tk_: Tk.Fix, val args: List<Tk.Id>, val body: Expr.Block): Expr(N++, tk_)
    data class Catch  (val tk_: Tk.Fix, val catch: Expr, val body: Expr.Block): Expr(N++, tk_)
    data class Throw  (val tk_: Tk.Fix, val ex: Expr): Expr(N++, tk_)
    data class Spawn  (val tk_: Tk.Fix, val task: Expr): Expr(N++, tk_)
    data class Bcast  (val tk_: Tk.Fix, val arg: Expr): Expr(N++, tk_)
    data class Resume (val tk_: Tk.Fix, val call: Expr.Call): Expr(N++, tk_)
    data class Yield  (val tk_: Tk.Fix, val arg: Expr): Expr(N++, tk_)
    data class Defer  (val tk_: Tk.Fix, val body: Expr.Block): Expr(N++, tk_)

    data class Nat    (val tk_: Tk): Expr(N++, tk_)
    data class Acc    (val tk_: Tk.Id): Expr(N++, tk_)
    data class Nil    (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Tag    (val tk_: Tk.Tag): Expr(N++, tk_)
    data class Bool   (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Num    (val tk_: Tk.Num): Expr(N++, tk_)
    data class Tuple  (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Index  (val tk_: Tk, val col: Expr, val idx: Expr): Expr(N++, tk_)
    data class Call   (val tk_: Tk, val f: Expr, val args: List<Expr>): Expr(N++, tk_)
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
fun all (name: String, reader: Reader, args: List<String>): String {
    val inps = listOf(
        Pair(name, reader),
        Pair("prelude.ceu", File("prelude.ceu").reader())
    )
    val lexer = Lexer(inps)
    val parser = Parser(lexer)
    val es = try {
        parser.exprs()
    } catch (e: Throwable) {
        return e.message!! + "\n"
    }
    val c = try {
        val coder = Coder(Expr.Block(Tk.Fix("",Pos("anon",0,0)),es))
        coder.main()
    } catch (e: Throwable) {
        return e.message!! + "\n"
    }
    File("out.c").writeText(c)
    val (ok2, out2) = exec(listOf("gcc", "out.c", "-o", "out.exe") + args)
    if (!ok2) {
        return out2
    }
    val (_, out3) = exec("./out.exe")
    //println(out3)
    return out3
}

fun main (args: Array<String>) {
    var xinp: String? = null
    var xccs = emptyList<String>()
    var i = 0
    while (i < args.size) {
        when {
            (args[i] == "-cc") -> { i++ ; xccs=args[i].split(" ") }
            else               -> xinp = args[i]
        }
        i++
    }
    print(all(xinp!!, File(xinp).reader(), xccs))
}
