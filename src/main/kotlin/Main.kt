package dceu

import java.io.File
import java.io.Reader
import java.util.*

var DUMP = true
var N = 1
val D = "\$"

// VERSION
const val MAJOR    = 0
const val MINOR    = 2
const val REVISION = 1
const val VERSION  = "v$MAJOR.$MINOR.$REVISION"

val PATH = File(File(System.getProperty("java.class.path")).absolutePath).parent

val KEYWORDS: SortedSet<String> = setOf (
    "break", "data", "do", "drop", "else",
    "enum", "false", "func", "if", "loop",
    "nil", "pass", "set",
    "true", "val", "var"
).toSortedSet()

val OPERATORS = setOf('+', '-', '*', '/', '>', '<', '=', '!', '|', '&', '~', '%', '#', '@')

val TAGS = listOf (
    ":nil", ":error", ":tag", ":bool", ":char", ":number", ":pointer",
    ":dynamic",
    ":func",
    ":tuple", ":vector", ":dict",
    ":ceu",
    ":tmp", ":xtmp"
)

val GLOBALS = setOf (
    "copy", "error", "next-dict", "print", "println",
    "string-to-tag", "sup?", "tags",
    "tuple", "type", "{{#}}", "{{==}}", "{{/=}}", "..."
)

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
    data class Proto  (val tk_: Tk.Fix, val args: List<Pair<Tk.Id,Tk.Tag?>>, val body: Expr.Do): Expr(N++, tk_)
    data class Do     (val tk_: Tk, val es: List<Expr>) : Expr(N++, tk_)
    data class Dcl    (val tk_: Tk.Fix, val id: Tk.Id, /*val poly: Boolean,*/ val tmp: Boolean?, val tag: Tk.Tag?, val init: Boolean, val src: Expr?):  Expr(N++, tk_)  // init b/c of iter var
    data class Set    (val tk_: Tk.Fix, val dst: Expr, /*val poly: Tk.Tag?,*/ val src: Expr): Expr(N++, tk_)
    data class If     (val tk_: Tk.Fix, val cnd: Expr, val t: Expr.Do, val f: Expr.Do): Expr(N++, tk_)
    data class Loop   (val tk_: Tk.Fix, val body: Expr.Do): Expr(N++, tk_)
    data class Break  (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Enum   (val tk_: Tk.Fix, val tags: List<Pair<Tk.Tag,Tk.Nat?>>): Expr(N++, tk_)
    data class Data   (val tk_: Tk.Tag, val ids: List<Pair<Tk.Id,Tk.Tag?>>): Expr(N++, tk_)
    data class Pass   (val tk_: Tk.Fix, val e: Expr): Expr(N++, tk_)
    data class Drop   (val tk_: Tk.Fix, val e: Expr): Expr(N++, tk_)

    data class Nat    (val tk_: Tk.Nat): Expr(N++, tk_)
    data class Acc    (val tk_: Tk.Id): Expr(N++, tk_)
    data class Nil    (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Tag    (val tk_: Tk.Tag): Expr(N++, tk_)
    data class Bool   (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Char   (val tk_: Tk.Chr): Expr(N++, tk_)
    data class Num    (val tk_: Tk.Num): Expr(N++, tk_)
    data class Tuple  (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Vector (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Dict   (val tk_: Tk.Fix, val args: List<Pair<Expr,Expr>>): Expr(N++, tk_)
    data class Index  (val tk_: Tk, val col: Expr, val idx: Expr): Expr(N++, tk_)
    data class Call   (val tk_: Tk, val closure: Expr, val args: List<Expr>): Expr(N++, tk_)
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
fun all (verbose: Boolean, name: String, reader: Reader, out: String, args: List<String>): String {
    DUMP = false
    if (verbose) {
        System.err.println("... parsing ...")
    }
    val inps = listOf(
        Pair(Triple(name,1,1), reader),
        Pair(Triple("prelude.ceu",1,1), FileX("@/prelude.ceu").reader())
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
        if (verbose) {
            System.err.println("... analysing ...")
        }
        //readLine()
        val outer  = Expr.Do(Tk.Fix("", Pos("anon", 0, 0)), es)
        val ups    = Ups(outer)
        val tags   = Tags(outer)
        val vars   = Vars(outer, ups)
        val clos   = Clos(outer, ups, vars)
        val sta    = Static(outer, ups, vars)
        if (verbose) {
            System.err.println("... ceu -> c ...")
        }
        val coder  = Coder(outer, ups, vars, clos)
        coder.main(tags)
    } catch (e: Throwable) {
        //throw e;
        return e.message!! + "\n"
    }
    if (verbose) {
        System.err.println("... c -> exe ...")
    }
    File("$out.c").writeText(c)
    val (ok2, out2) = exec(listOf("gcc", "$out.c", "-l", "m", "-o", "$out.exe") + args)
    if (!ok2) {
        return out2
    }
    if (verbose) {
        System.err.println("... executing ...")
    }
    val (_, out3) = exec("./$out.exe")
    //println(out3)
    return out3
}

fun main (args: Array<String>) {
    val (xs, ys) = args.cmds_opts()

    try {
        val xinp = if (xs.size > 0) xs[0] else null
        val xccs = (when {
            !ys.containsKey("--cc") -> emptyList()
            (ys["--cc"] == null) -> {
                throw Exception("invalid argument : --cc : expected \"=\"")
            }
            else -> ys["--cc"]!!.split(" ")
        }) + (when {
            !ys.containsKey("--lib") -> emptyList()
            (ys["--lib"] == null) -> {
                throw Exception("invalid argument : --lib : expected \"=\"")
            }
            else -> {
                File(PATH + "/" + ys["--lib"] + "/ceu.lib")
                    .readText()
                    .trim()
                    .replace("@/",PATH+"/")
                    .split(" ")
            }
        })

        when {
            ys.containsKey("--version") -> println("ceu " + VERSION)
            (xinp == null) -> println("expected filename")
            else -> {
                    val f = File(xinp)
                    val out = all(ys.containsKey("--verbose"), xinp, f.reader(), f.nameWithoutExtension, xccs)
                    print(out)
            }
        }
    } catch (e: Throwable) {
        println(e.message!!)
    }
}
