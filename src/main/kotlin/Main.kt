package dceu

import java.io.File
import java.io.Reader
import java.util.*

var TEST = false
var DUMP = true
var DEBUG = false
var CEU = 1

    //  1: dyn-lex                           ;; 20 "definitely lost"
    //  2: defer, throw/catch, patts         ;;  6 "definitely lost"
    //  3: coro, yield, resume               ;;  0
    //  4: task, pub, bcast, toggle, delay   ;;  0
    //  5: tasks                             ;;  0
    // 99: sugar                             ;;  0
    // TODO: copy, underscore, self (coro/task)

// search in tests output for
//  definitely|Invalid read|Invalid write|uninitialised|uninitialized|free'd|alloc'd
//  - definitely lost
//  - Invalid read of size
//  - uninitialised value
val VALGRIND = ""
//val VALGRIND = "valgrind "
val THROW = false
//val THROW = true

var N = 1
val D = "\$"

// VERSION
const val MAJOR    = 0
const val MINOR    = 4
const val REVISION = 0
const val VERSION  = "v$MAJOR.$MINOR.$REVISION"

val PATH = File(File(System.getProperty("java.class.path")).absolutePath).parent

val KEYWORDS: SortedSet<String> = (
    setOf (
        "break", "data", "do", "else",
        "false", "func", "group", "if",
        "loop", "nil", "set", "skip",
        "true", "val", "var",
    ) + (if (CEU < 2) setOf() else setOf (
        "catch", "catch'", "defer",
    )) + (if (CEU < 3) setOf() else setOf(
        "coro", "resume", "yield",
    )) + (if (CEU < 4) setOf() else setOf(
        "broadcast", "delay", "in", "pub", "spawn", "task", "toggle",
    )) + (if (CEU < 5) setOf() else setOf(
        "tasks",
    )) + (if (CEU < 99) setOf() else setOf(
        "await", "enum", "every", "ifs", "match",
        "par", "par-and", "par-or",
        "resume-yield-all", "test",
        "thus", "until", "watching",
        "with", "where", "while",
    ))
).toSortedSet()

val OPERATORS = setOf('+', '-', '*', '/', '%', '>', '<', '=', '|', '&', '~')
val XOPERATORS = if (CEU < 99) setOf() else {
    setOf("and", "in?", "in-not?", "is?", "is-not?", "not", "or")
}

val TAGS = listOf (
    ":nil", ":tag", ":bool", ":char", ":number", ":pointer",
) + (if (CEU < 2) listOf(":error") else emptyList()) + listOf(
    ":dynamic", // error before or after dynamic
) + (if (CEU >= 2) listOf(":error") else emptyList()) + listOf(
    ":func",
) + (if (CEU < 3) listOf() else listOf(
    ":coro",
)) + (if (CEU < 4) listOf() else listOf(
    ":task",
)) + listOf (
    ":tuple", ":vector", ":dict",
) + (if (CEU < 3) listOf() else listOf(
    ":exe-coro",
)) + (if (CEU < 4) listOf() else listOf(
    ":exe-task",
)) + (if (CEU < 5) listOf() else listOf(
    ":tasks",
)) + (if (CEU < 3) listOf() else listOfNotNull(
    ":yielded", (if (CEU<4) null else ":toggled"), ":resumed", ":terminated"
)) + (if (CEU < 4) listOf() else listOf(
    ":global", ":task", ":nested"
)) + listOf(
    ":ceu", ":pre",
) + (if (CEU < 99) listOf() else listOf(
    ":h", ":min", ":s", ":ms",
    ":idx", ":key", ":val",
))

val GLOBALS = setOf (
    "dump", "error", "next-dict", "print", "println",
    "sup?", "tag",
    "to-string-number", "to-string-pointer", "to-string-tag",
    "to-tag-string",
    "tuple", "type", "{{#}}", "{{==}}", "{{/=}}",
) + (if (CEU < 3) setOf() else setOf(
    "coroutine", "status"
)) + (if (CEU < 4) setOf() else setOf(
    "broadcast'"
)) + (if (CEU < 5) setOf() else setOf(
    "next-tasks",
))

sealed class Patt (val id: Tk.Id, val tag: Tk.Tag?, val pos: Expr) {
    data class None (val id_: Tk.Id, val tag_: Tk.Tag?, val pos_: Expr): Patt(id_,tag_,pos_)
    data class One  (val id_: Tk.Id, val tag_: Tk.Tag?, val e: Expr, val pos_: Expr): Patt(id_,tag_,pos_)
    data class Tup  (val id_: Tk.Id, val tag_: Tk.Tag?, val l: List<Patt>, val pos_: Expr): Patt(id_,tag_,pos_)
}

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof (val pos_: Pos, val n_: Int=N++): Tk("", pos_)
    data class Fix (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Tag (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Op  (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Id  (val str_: String, val pos_: Pos, val n_: Int = N++): Tk(str_, pos_)  // up: 0=var, 1=upvar, 2=upref
    data class Num (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Chr (val str_: String, val pos_: Pos, val n_: Int=N++): Tk(str_, pos_)
    data class Nat (val str_: String, val pos_: Pos, val tag: String?, val n_: Int=N++): Tk(str_, pos_)
}

typealias Id_Tag  = Pair<Tk.Id,Tk.Tag?>

sealed class Expr (val n: Int, val tk: Tk) {
    data class Proto  (val tk_: Tk.Fix, val nst: Boolean, val tag: Tk.Tag?, val pars: List<Id_Tag>, val blk: Do): Expr(N++, tk_)
    data class Do     (val tk_: Tk, val es: List<Expr>) : Expr(N++, tk_)
    data class Group  (val tk_: Tk.Fix, val es: List<Expr>) : Expr(N++, tk_)
    data class Dcl    (val tk_: Tk.Fix, val idtag: Id_Tag, /*val poly: Boolean,*/ val src: Expr?):  Expr(N++, tk_)
    data class Set    (val tk_: Tk.Fix, val dst: Expr, /*val poly: Tk.Tag?,*/ val src: Expr): Expr(N++, tk_)
    data class If     (val tk_: Tk.Fix, val cnd: Expr, val t: Expr.Do, val f: Expr.Do): Expr(N++, tk_)
    data class Loop   (val tk_: Tk.Fix, val blk: Expr.Do): Expr(N++, tk_)
    data class Break  (val tk_: Tk.Fix, val e: Expr?): Expr(N++, tk_)
    data class Skip   (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Data   (val tk_: Tk.Tag, val ids: List<Id_Tag>): Expr(N++, tk_)

    data class Catch  (val tk_: Tk.Fix, val cnd: Expr.Do, val blk: Expr.Do): Expr(N++, tk_)
    data class Defer  (val tk_: Tk.Fix, val blk: Expr.Do): Expr(N++, tk_)

    data class Yield  (val tk_: Tk.Fix, val e: Expr): Expr(N++, tk_)
    data class Resume (val tk_: Tk.Fix, val co: Expr, val args: List<Expr>): Expr(N++, tk_)

    data class Spawn  (val tk_: Tk.Fix, val tsks: Expr?, val tsk: Expr, val args: List<Expr>): Expr(N++, tk_)
    data class Delay  (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Pub    (val tk_: Tk, val tsk: Expr?): Expr(N++, tk_)
    data class Toggle (val tk_: Tk.Fix, val tsk: Expr, val on: Expr): Expr(N++, tk_)
    data class Tasks  (val tk_: Tk.Fix, val max: Expr): Expr(N++, tk_)

    data class Nat    (val tk_: Tk.Nat): Expr(N++, tk_)
    data class Acc    (val tk_: Tk.Id, val ign: Boolean=false): Expr(N++, tk_)
    data class Nil    (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Tag    (val tk_: Tk.Tag): Expr(N++, tk_)
    data class Bool   (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Char   (val tk_: Tk.Chr): Expr(N++, tk_)
    data class Num    (val tk_: Tk.Num): Expr(N++, tk_)
    data class Tuple  (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Vector (val tk_: Tk.Fix, val args: List<Expr>): Expr(N++, tk_)
    data class Dict   (val tk_: Tk.Fix, val args: List<Pair<Expr,Expr>>): Expr(N++, tk_)
    data class Index  (val tk_: Tk, val col: Expr, val idx: Expr): Expr(N++, tk_)
    data class Call   (val tk_: Tk, val clo: Expr, val args: List<Expr>): Expr(N++, tk_)
}

fun exec (hold: Boolean, cmds: List<String>): Pair<Boolean,String> {
    //System.err.println(cmds.joinToString(" "))
    val (x,y) = if (hold) {
        Pair(ProcessBuilder.Redirect.PIPE, true)
    } else {
        Pair(ProcessBuilder.Redirect.INHERIT, false)
    }
    val p = ProcessBuilder(cmds)
        .redirectOutput(x)
        .redirectError(x)
        .redirectErrorStream(y)
        .start()
    val ret = p.waitFor()
    val str = p.inputStream.bufferedReader().readText()
    return Pair(ret==0, str)
}
fun exec (hold: Boolean, cmd: String): Pair<Boolean,String> {
    return exec(hold, cmd.split(' '))
}

fun all (tst: Boolean, verbose: Boolean, inps: List<Pair<Triple<String, Int, Int>, Reader>>, out: String, args: List<String>): String {
    if (verbose) {
        System.err.println("... parsing ...")
    }
    val lexer = Lexer(inps)
    val parser = Parser(lexer)
    val es = try {
        parser.exprs()
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
        return e.message!! + "\n"
    }
    //println(es.tostr())
    val c = try {
        if (verbose) {
            System.err.println("... analysing ...")
        }
        //readLine()
        val pos = Pos("anon", 0, 0, 0)
        val ARGS   = Expr.Dcl(Tk.Fix("val",pos), Pair(Tk.Id("ARGS",pos),null), null)
        val outer  = Expr.Do(Tk.Fix("", pos), listOf(ARGS)+es)
        val ups    = Ups(outer)
        val tags   = Tags(outer)
        val vars   = Vars(outer, ups)
        val sta    = Static(outer, ups, vars)
        //rets.pub.forEach { println(listOf(it.value,it.key.javaClass.name,it.key.tk.pos.lin)) }
        if (verbose) {
            System.err.println("... ceu -> c ...")
        }
        val coder = Coder(outer, ups, vars, sta)
        coder.main(tags)
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
        return e.message!! + "\n"
    }
    if (verbose) {
        System.err.println("... c -> exe ...")
    }
    File("$out.c").writeText(c)
    val (ok2, out2) = exec(true, listOf("gcc", "-Werror", "$out.c", "-l", "m", "-o", "$out.exe") + args)
    if (!ok2) {
        return out2
    }
    if (verbose) {
        System.err.println("... executing ...")
    }
    val (_, out3) = exec(tst, "$VALGRIND./$out.exe")
    //println(out3)
    return out3
}

fun test (inp: String, pre: Boolean=false): String {
    //println(inp)
    val prelude = if (CEU == 99) "build/prelude-x.ceu" else "build/prelude-0.ceu"
    val inps = listOf(Pair(Triple("anon",1,1), inp.reader())) + if (!pre) emptyList() else {
        listOf(Pair(Triple(prelude,1,1), File(prelude).reader()))
    }
    return all(true, false, inps, "out", emptyList())
}

fun main (args: Array<String>) {
    DUMP = false
    val (xs, ys) = args.cmds_opts()
    try {
        val xinp = if (xs.size > 0) xs[0] else null
        val xccs = (when {
            !ys.containsKey("--cc") -> emptyList()
            (ys["--cc"] == null) -> {
                throw Exception("argument error : --cc : expected \"=\"")
            }
            else -> ys["--cc"]!!.split(" ")
        }) + (when {
            !ys.containsKey("--lib") -> emptyList()
            (ys["--lib"] == null) -> {
                throw Exception("argument error : --lib : expected \"=\"")
            }
            else -> {
                File(PATH + "/" + ys["--lib"] + "/ceu.lib")
                    .readText()
                    .trim()
                    .replace("@/",PATH+"/")
                    .split(" ")
            }
        })

        if (ys.containsKey("--test")) {
            TEST = true
        }

        when {
            ys.containsKey("--version") -> println("dceu " + VERSION)
            (xinp == null) -> println("expected filename")
            else -> {
                val f = File(xinp)
                val inps = listOf(
                    Pair(Triple(xinp,1,1), f.reader()),
                    Pair(Triple("prelude.ceu",1,1), FileX("@/prelude.ceu").reader())
                )
                val out = all(false, ys.containsKey("--verbose"), inps, f.nameWithoutExtension, xccs)
                print(out)
            }
        }
    } catch (e: Throwable) {
        println(e.message!!)
    }
}
