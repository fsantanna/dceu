import java.io.File
import java.io.Reader
import java.util.*

val XCEU = false
//val XCEU = true
var N = 1

val D = "\$"

val KEYWORDS: SortedSet<String> = (setOf (
    "broadcast", "catch", "coroutine", "coroutines", "defer", "do", "else", "err", "evt",
    "false", "func", "group", "if", "in", "nil", "pub", "resume", "set", "spawn", "status",
    "task", "throw", "toggle", "track", "true", "var", "yield", "while"
) + if (!XCEU) setOf() else setOf (
    "and", "await", "awaiting", "every", "ifs", "is", "isnot", "not", "or", "par",
    "parand", "paror", "until", "with", "where"
)).toSortedSet()

val OPERATORS = setOf('+', '-', '*', '/', '>', '<', '=', '!', '|', '&', '~', '%', '#')

val TAGS = listOf (
    ":nil", ":tag", ":bool", ":char", ":number", ":pointer",
    ":dynamic",
    ":func", ":task",
    ":tuple", ":vector", ":dict",
    ":bcast",
    ":coro", ":coros", ":track",
    ":fake", ":hide", ":check.now", ":all", ":awakes",
    ":clear", ":error",           // bcast-clear
    ":global", ":local", //":task"   // bcast scope
    ":yielded", ":toggled", ":resumed", ":terminated", ":destroyed"
)

val GLOBALS = setOf (
    "copy", "move", "next", "print", "println", "tags", "type",
    "op_slash_equals", "op_equals_equals", "op_hash"
)

val EXPOSE = setOf (
    "copy", "is'", "isnot'", "print", "println", "type", "{==}", "{/=}"
)

val ITERS = setOf (
    ":coros"
) + if (!XCEU) emptySet() else setOf(":coro", ":vector", ":dict")

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof (val pos_: Pos, val n_: Int=N++): Tk("", pos_)
    data class Fix (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Tag (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Op  (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Id  (val str_: String, val pos_: Pos, val upv: Int, val n_: Int=N++): Tk(str_, pos_)  // up: 0=var, 1=upvar, 2=upref
    data class Num (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Chr (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Nat (val str_: String, val pos_: Pos, val tag: String?, val n_: Int=N++): Tk(str_, pos_)
    data class Clk (val str_: String, val pos_: Pos, val ms: Int, val n_: Int=N++): Tk(str_, pos_)
}
sealed class Expr (val n: Int, val tk: Tk) {
    data class Proto  (val tk_: Tk.Fix, val task: Pair<Boolean,Boolean>?, val args: List<Tk.Id>, val body: Expr.Block): Expr(N++, tk_)
    data class Block  (val tk_: Tk, val es: List<Expr>) : Expr(N++, tk_)
    data class Group  (val tk_: Tk.Fix, val isHide: Boolean, val es: List<Expr>): Expr(N++, tk_)
    data class Dcl    (val tk_: Tk.Id, val init: Boolean):  Expr(N++, tk_)  // init b/c of iter var
    data class Set    (val tk_: Tk.Fix, val dst: Expr, val src: Expr): Expr(N++, tk_)
    data class If     (val tk_: Tk.Fix, val cnd: Expr, val t: Expr.Block, val f: Expr.Block): Expr(N++, tk_)
    data class While  (val tk_: Tk.Fix, val cnd: Expr, val body: Expr.Block): Expr(N++, tk_)
    data class Catch  (val tk_: Tk.Fix, val cnd: Expr, val body: Expr.Block): Expr(N++, tk_)
    data class Throw  (val tk_: Tk.Fix, val ex: Expr): Expr(N++, tk_)
    data class Defer  (val tk_: Tk.Fix, val body: Expr.Block): Expr(N++, tk_)

    data class Coros  (val tk_: Tk.Fix, val max: Expr?): Expr(N++, tk_)
    data class Coro   (val tk_: Tk.Fix, val task: Expr): Expr(N++, tk_)
    data class Spawn  (val tk_: Tk.Fix, val coros: Expr?, val call: Expr): Expr(N++, tk_)
    data class CsIter (val tk_: Tk.Fix, val loc: Tk.Id, val coros: Expr, val body: Expr.Block): Expr(N++, tk_)
    data class Bcast  (val tk_: Tk.Fix, val xin: Expr, val evt: Expr): Expr(N++, tk_)
    data class Yield  (val tk_: Tk.Fix, val arg: Expr): Expr(N++, tk_)
    data class Resume (val tk_: Tk.Fix, val call: Expr.Call): Expr(N++, tk_)
    data class Toggle (val tk_: Tk.Fix, val coro: Expr, val on: Expr): Expr(N++, tk_)
    data class Pub    (val tk_: Tk.Fix, val coro: Expr?): Expr(N++, tk_)
    data class Track  (val tk_: Tk.Fix, val coro: Expr): Expr(N++, tk_)

    data class Nat    (val tk_: Tk.Nat): Expr(N++, tk_)
    data class Acc    (val tk_: Tk.Id): Expr(N++, tk_)
    data class EvtErr (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Nil    (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Tag    (val tk_: Tk.Tag): Expr(N++, tk_)
    data class Bool   (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Char   (val tk_: Tk.Chr): Expr(N++, tk_)
    data class Num    (val tk_: Tk.Num): Expr(N++, tk_)
    data class Tuple  (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Vector (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Dict   (val tk_: Tk.Fix, val args: List<Pair<Expr,Expr>>): Expr(N++, tk_)
    data class Index  (val tk_: Tk, val col: Expr, val idx: Expr): Expr(N++, tk_)
    data class Call   (val tk_: Tk, val proto: Expr, val args: List<Expr>): Expr(N++, tk_)
        // call args must be enclosed with a "fake" block, which is a normal block is not output in tostr()
        // the block is required to create a separate environment for the call arguments such that
        // `evt` is allowed to be passed forward
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
    //println(es.map { it.tostr()+"\n" }.joinToString(""))
    val c = try {
        val outer = Expr.Block(Tk.Fix("", Pos("anon", 0, 0)), es)
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
