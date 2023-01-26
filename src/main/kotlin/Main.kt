import java.io.File
import java.io.Reader
import java.util.*

val XCEU = false
//val XCEU = true
var N = 1

val D = "\$"

val KEYWORDS: SortedSet<String> = (setOf (
    "broadcast", "catch", "coro", "defer", "do", "else", "enum", "err", "evt",
    "false", "func", "if", "in", "nil", "pass", "pub", "resume", "set", "spawn", "status",
    "task", "data", "toggle", "true", "var", "yield", "while"
) + if (!XCEU) setOf() else setOf (
    "and", "await", "awaiting", "every", "ifs", "is", "isnot", "not", "or", "par",
    "par-and", "par-or", "poly", "until", "with", "where"
)).toSortedSet()

val OPERATORS = setOf('+', '-', '*', '/', '>', '<', '=', '!', '|', '&', '~', '%', '#')

val TAGS = listOf (
    ":nil", ":tag", ":bool", ":char", ":number", ":pointer",
    ":dynamic",
    ":func", ":coro", ":task",
    ":tuple", ":vector", ":dict",
    ":bcast",
    ":x-coro", ":x-task", ":x-tasks", ":x-track",
    ":fake", ":hide", ":check-now", ":all",
    ":ceu", ":clear", ":error",           // bcast-clear
    ":tmp", ":global", ":local", //":task"   // bcast scope
    ":yielded", ":toggled", ":resumed", ":terminated"
) + if (!XCEU) emptySet() else setOf(":h",":min",":s",":ms")

val GLOBALS = setOf (
    "copy", "coroutine", "detrack", "move", "next", "print", "println",
    "supof", "tags", "tasks", "throw", "track", "type",
    "{==}", "{#}", "{/=}"
)

val ITERS = setOf (
    ":tasks"
) + if (!XCEU) emptySet() else setOf(":coro", ":tuple", ":vector", ":dict")

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof (val pos_: Pos, val n_: Int=N++): Tk("", pos_)
    data class Fix (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Tag (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Op  (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Id  (val str_: String, val pos_: Pos, val upv: Int, val n_: Int=N++): Tk(str_, pos_)  // up: 0=var, 1=upvar, 2=upref
    data class Num (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Chr (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Nat (val str_: String, val pos_: Pos, val tag: String?, val n_: Int=N++): Tk(str_, pos_)
}
sealed class Expr (val n: Int, val tk: Tk) {
    data class Proto  (val tk_: Tk.Fix, val task: Pair<Tk.Tag?,Boolean>?, val args: List<Pair<Tk.Id,Tk.Tag?>>, val body: Expr.Do): Expr(N++, tk_)
    data class Do     (val tk_: Tk, val isnest: Boolean, val ishide: Boolean, val es: List<Expr>) : Expr(N++, tk_)
    data class Dcl    (val tk_: Tk.Id,  val tmp: Boolean, val tag: Tk.Tag?, val init: Boolean, val src: Expr?):  Expr(N++, tk_)  // init b/c of iter var
    data class Set    (val tk_: Tk.Fix, val dst: Expr, val src: Expr): Expr(N++, tk_)
    data class If     (val tk_: Tk.Fix, val cnd: Expr, val t: Expr.Do, val f: Expr.Do): Expr(N++, tk_)
    data class While  (val tk_: Tk.Fix, val cnd: Expr, val body: Expr.Do): Expr(N++, tk_)
    data class Catch  (val tk_: Tk.Fix, val cnd: Expr, val body: Expr.Do): Expr(N++, tk_)
    data class Defer  (val tk_: Tk.Fix, val body: Expr.Do): Expr(N++, tk_)
    data class Enum   (val tk_: Tk.Fix, val tags: List<Pair<Tk.Tag,Tk.Nat?>>): Expr(N++, tk_)
    data class Data (val tk_: Tk.Tag, val ids: List<Pair<Tk.Id,Tk.Tag?>>): Expr(N++, tk_)
    data class Pass   (val tk_: Tk.Fix, val e: Expr): Expr(N++, tk_)

    data class Spawn  (val tk_: Tk.Fix, val tasks: Expr?, val call: Expr): Expr(N++, tk_)
    data class Bcast  (val tk_: Tk.Fix, val xin: Expr, val evt: Expr): Expr(N++, tk_)
    data class Yield  (val tk_: Tk.Fix, val arg: Expr): Expr(N++, tk_)
    data class Resume (val tk_: Tk.Fix, val call: Expr.Call): Expr(N++, tk_)
    data class Toggle (val tk_: Tk.Fix, val task: Expr, val on: Expr): Expr(N++, tk_)
    data class Pub    (val tk_: Tk.Fix, val x: Expr): Expr(N++, tk_)
    data class Self   (val tk_: Tk.Fix): Expr(N++, tk_)

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
        //throw e;
        return e.message!! + "\n"
    }
    //println(es.map { it.tostr()+"\n" }.joinToString(""))
    val c = try {
        val outer = Expr.Do(Tk.Fix("", Pos("anon", 0, 0)), true, true, es)
        val ups   = Ups(outer)
        val tags  = Tags(outer)
        val vars  = Vars(outer, ups)
        val clos  = Clos(outer, ups, vars)
        val sta   = Static(outer, ups, vars)
        val coder = Coder(outer, ups, vars, clos, sta)
        coder.main(tags)
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
