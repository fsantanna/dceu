import java.io.File
import java.io.Reader
import java.util.*

val XCEU = false
//val XCEU = true
var N = 1

val KEYWORDS: SortedSet<String> = (setOf (
    "broadcast", "catch", "coroutine", "coroutines", "defer", "do", "else", "false", "func",
    "if", "in", "native", "nil", "resume", "set", "spawn", "task", "throw", "true",
    "var", "yield", "while"
) + if (!XCEU) setOf() else setOf (
    "and", "await", "every", "ifs", "not", "or", "par", "parand", "paror", "watching", "with", "where"
)).toSortedSet()

val OPERATORS = setOf('+', '-', '*', '/', '>', '<', '=', '!', '|', '&', '~', '%')

val TAGS = listOf (
    "nil", "tag", "bool", "number", "pointer",
    "func", "task",
    "tuple", "dict",
    "coro", "coros",
    "clear", "error",      // bcast-clear
)

val GLOBALS = setOf (
    "tags", "print", "println", "op_eq_eq", "op_div_eq", "err", "evt"
)

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof (val pos_: Pos): Tk("", pos_)
    data class Fix (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Tag (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Op  (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Id  (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Num (val str_: String, val pos_: Pos): Tk(str_, pos_)
    data class Nat (val str_: String, val pos_: Pos, val tag: String?): Tk(str_, pos_)
    data class Clk (val str_: String, val pos_: Pos, val ms: Int): Tk(str_, pos_)
}
sealed class Expr (val n: Int, val tk: Tk) {
    data class Block  (val tk_: Tk, val isFake: Boolean, val es: List<Expr>) : Expr(N++, tk_)
    data class Dcl    (val tk_: Tk.Id, val init: Boolean):  Expr(N++, tk_)
    data class Set    (val tk_: Tk.Fix, val dst: Expr, val src: Expr): Expr(N++, tk_)
    data class If     (val tk_: Tk.Fix, val cnd: Expr, val t: Expr.Block, val f: Expr.Block): Expr(N++, tk_)
    data class While  (val tk_: Tk.Fix, val cnd: Expr, val body: Expr.Block): Expr(N++, tk_)
    data class Func   (val tk_: Tk.Fix, val args: List<Tk.Id>, val body: Expr.Block): Expr(N++, tk_)
    data class Catch  (val tk_: Tk.Fix, val cnd: Expr, val body: Expr.Block): Expr(N++, tk_)
    data class Throw  (val tk_: Tk.Fix, val ex: Expr): Expr(N++, tk_)
    data class Defer  (val tk_: Tk.Fix, val body: Expr.Block): Expr(N++, tk_)

    data class Coros  (val tk_: Tk.Fix, val max: Expr?): Expr(N++, tk_)
    data class Coro   (val tk_: Tk.Fix, val task: Expr): Expr(N++, tk_)
    data class Spawn  (val tk_: Tk.Fix, val coros: Expr?, val call: Expr.Block): Expr(N++, tk_)
    data class Iter   (val tk_: Tk.Fix, val loc: Tk.Id, val coros: Expr, val body: Expr.Block): Expr(N++, tk_)
    data class Bcast  (val tk_: Tk.Fix, val evt: Expr): Expr(N++, tk_)
    data class Yield  (val tk_: Tk.Fix, val arg: Expr): Expr(N++, tk_)
    data class Resume (val tk_: Tk.Fix, val call: Expr.Block): Expr(N++, tk_)

    data class Nat    (val tk_: Tk.Nat): Expr(N++, tk_)
    data class Acc    (val tk_: Tk.Id): Expr(N++, tk_)
    data class Nil    (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Tag    (val tk_: Tk.Tag): Expr(N++, tk_)
    data class Bool   (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Num    (val tk_: Tk.Num): Expr(N++, tk_)
    data class Tuple  (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Dict   (val tk_: Tk.Fix, val args: List<Pair<Expr,Expr>>): Expr(N++, tk_)
    data class Index  (val tk_: Tk, val col: Expr, val idx: Expr): Expr(N++, tk_)
    data class Call   (val tk_: Tk, val f: Expr, val args: List<Expr>): Expr(N++, tk_)

    data class XSeq   (val tk_: Tk, val es: List<Expr>): Expr(N++, tk_)
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
        Pair(Triple(name,1,1), reader),
        Pair(Triple("prelude.ceu",1,1), File("prelude.ceu").reader())
    )
    val lexer = Lexer(inps)
    val parser = Parser(lexer)
    val es = try {
        parser.exprs()
    } catch (e: Throwable) {
        return e.message!! + "\n"
    }
    val c = try {
        val outer = Expr.Block(Tk.Fix("", Pos("anon", 0, 0)), false, es)
        val ups = Ups(outer)
        val coder = Coder(outer, ups)
        coder.main()
    } catch (e: Throwable) {
        //throw e;
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
