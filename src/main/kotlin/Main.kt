package dceu

import java.io.File
import java.io.Reader
import java.util.*

var CEU = 1
    // 1: dyn-lex
    // 2: defer, throw/catch, as, =>
    // 3: coro, yield, resume
    // 4: task, pub, bcast, toggle, ref
    // 5: tasks, track, detrack
    // TODO: export, copy, underscore, self (coro/task)
    // 99: empty blocks, not/and/or, is/is-not?/in?/in-not?, func dcl
    //     if id-tag, if => =>, ifs, yield empty arg/block, resume-yield-all
    //     spawn coro/task, par/par-and/par-or

// search in tests output for
//  definitely|Invalid read|Invalid write|uninitialised|uninitialized
//  - definitely lost
//  - Invalid read of size
//  - uninitialised value
val VALGRIND = ""
//val VALGRIND = "valgrind "
val THROW = false
//val THROW = true

var DUMP = true
var N = 1
val D = "\$"

// VERSION
const val MAJOR    = 0
const val MINOR    = 3
const val REVISION = 0
const val VERSION  = "v$MAJOR.$MINOR.$REVISION"

val PATH = File(File(System.getProperty("java.class.path")).absolutePath).parent

val KEYWORDS: SortedSet<String> = (
    setOf (
        "as", "break", "data", "do", "drop", "else",
        "enum", "false", "func", "if",
        "loop", "nil", "pass", "set",
        "thus", "true", "val", "var",
    ) + (if (CEU < 2) setOf() else setOf (
        "catch", "defer", "in",
    )) + (if (CEU < 3) setOf() else setOf(
        "coro", "resume", "yield",
    )) + (if (CEU < 4) setOf() else setOf(
        "broadcast", "pub", "spawn", "task", "toggle",
    )) + (if (CEU < 5) setOf() else setOf(
        "detrack",
    )) + (if (CEU < 99) setOf() else setOf(
        "ifs", "resume-yield-all"
    ))
).toSortedSet()

val OPERATORS = setOf('+', '-', '*', '/', '>', '<', '=', '!', '|', '&', '~', '%', '#', '@')
val XOPERATORS = if (CEU < 99) setOf() else {
    setOf("and", "in?", "in-not?", "is?", "is-not?", "not", "or")
}

val TAGS = listOf (
    ":nil", ":error", ":tag", ":bool", ":char", ":number", ":pointer",
) + (if (CEU < 4) listOf() else listOf(
    ":ref",
)) + listOf(
    ":dynamic",
    ":func",
) + (if (CEU < 3) listOf() else listOf(
    ":coro",
)) + (if (CEU < 4) listOf() else listOf(
    ":task",
)) + listOf (
    ":tuple", ":vector", ":dict",
) + (if (CEU < 2) listOf() else listOf(
    ":throw",
)) + (if (CEU < 3) listOf() else listOf(
    ":exe-coro",
)) + (if (CEU < 4) listOf() else listOf(
    ":exe-task",
)) + (if (CEU < 5) listOf() else listOf(
    ":exe-task-in", ":tasks", ":track"
)) + (if (CEU < 3) listOf() else listOfNotNull(
    ":yielded", (if (CEU<4) null else ":toggled"), ":resumed", ":terminated"
)) + (if (CEU < 4) listOf() else listOf(
    ":void",
)) + listOf(
    ":ceu",
)

val GLOBALS = setOf (
    "dump", "error", "next", "print", "println",
    "string-to-tag", "sup?", "tags",
    "tuple", "type", "{{#}}", "{{==}}", "{{/=}}", "..."
) + (if (CEU < 2) setOf() else setOf(
    "pointer-to-string", "throw"
)) + (if (CEU < 3) setOf() else setOf(
    "coroutine", "status"
)) + (if (CEU < 4) setOf() else setOf(
    "broadcast"
)) + (if (CEU < 5) setOf() else setOf(
    "tasks", "track"
))

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
    data class Proto  (val tk_: Tk.Fix, val tag: Tk.Tag?, val args: List<Pair<Tk.Id,Tk.Tag?>>, val blk: Expr.Do): Expr(N++, tk_)
    data class Do     (val tk_: Tk, val es: List<Expr>) : Expr(N++, tk_)
    data class Dcl(val tk_: Tk.Fix, val id: Tk.Id, /*val poly: Boolean,*/  val tag: Tk.Tag?, val init: Boolean, val src: Expr?):  Expr(N++, tk_)  // init b/c of iter var
    data class Set    (val tk_: Tk.Fix, val dst: Expr, /*val poly: Tk.Tag?,*/ val src: Expr): Expr(N++, tk_)
    data class If     (val tk_: Tk.Fix, val cnd: Expr, val t: Expr.Do, val f: Expr.Do): Expr(N++, tk_)
    data class Loop   (val tk_: Tk.Fix, val blk: Expr.Do): Expr(N++, tk_)
    data class Break  (val tk_: Tk.Fix, val cnd: Expr, val e: Expr?): Expr(N++, tk_)
    data class Enum   (val tk_: Tk.Fix, val tags: List<Pair<Tk.Tag,Tk.Nat?>>): Expr(N++, tk_)
    data class Data   (val tk_: Tk.Tag, val ids: List<Pair<Tk.Id,Tk.Tag?>>): Expr(N++, tk_)
    data class Pass   (val tk_: Tk.Fix, val e: Expr): Expr(N++, tk_)
    data class Drop   (val tk_: Tk.Fix, val e: Expr): Expr(N++, tk_)

    data class Catch  (val tk_: Tk.Fix, val it: Pair<Tk.Id,Tk.Tag?>, val cnd: Expr.Do, val blk: Expr.Do): Expr(N++, tk_)
    data class Defer  (val tk_: Tk.Fix, val blk: Expr.Do): Expr(N++, tk_)

    data class Yield  (val tk_: Tk.Fix, val it: Pair<Tk.Id,Tk.Tag?>, val arg: Expr, val blk: Expr.Do): Expr(N++, tk_)
    data class Resume (val tk_: Tk.Fix, val co: Expr, val arg: Expr): Expr(N++, tk_)

    data class Spawn  (val tk_: Tk.Fix, val tsks: Expr?, val tsk: Expr, val arg: Expr): Expr(N++, tk_)
    data class Pub    (val tk_: Tk.Fix, val tsk: Expr?): Expr(N++, tk_)
    data class Bcast  (val tk_: Tk.Fix, val call: Expr.Call): Expr(N++, tk_)
    data class Dtrack (val tk_: Tk.Fix, val it: Pair<Tk.Id,Tk.Tag?>, val trk: Expr, val blk: Expr.Do): Expr(N++, tk_)
    data class Toggle (val tk_: Tk.Fix, val tsk: Expr, val on: Expr): Expr(N++, tk_)

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
    data class Call   (val tk_: Tk, val clo: Expr, val args: List<Expr>): Expr(N++, tk_)
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

val PLUS = """
    val {{+}} = func (v1, v2) {
        `:number (${D}v1.Number + ${D}v2.Number)`
    }    
"""
fun OR (v1:String, v2:String): String {
    return "($v1 thus { as it => if it { it } else { $v2 } })"
}
fun AND (v1:String, v2:String): String {
    return "($v1 thus { as it => if it { $v2 } else { $v1 } })"
}
fun AWAIT (v:String="true"): String {
    return """
        loop {
            break if yield(nil) { as it =>
                if type(it) == :exe-task {
                    false
                } else {
                    if $v {
                        if it { it } else { true }
                    } else {
                        false
                    }                    
                }
            }
        }
    """.replace("\n", "")
}

fun all (verbose: Boolean, inps: List<Pair<Triple<String, Int, Int>, Reader>>, out: String, args: List<String>): String {
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
    //println(es.tostr(true))
    //println(es)
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
        val coder  = Coder(outer, ups, vars, clos, sta)
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
    val (ok2, out2) = exec(listOf("gcc", "-Werror", "$out.c", "-l", "m", "-o", "$out.exe") + args)
    if (!ok2) {
        return out2
    }
    if (verbose) {
        System.err.println("... executing ...")
    }
    val (_, out3) = exec("$VALGRIND./$out.exe")
    //println(out3)
    return out3
}

fun test (inp: String, pre: Boolean=false): String {
    //println(inp)
    val prelude = if (CEU == 99) "build/prelude-x.ceu" else "build/prelude-0.ceu"
    val inps = listOf(Pair(Triple("anon",1,1), inp.reader())) + if (!pre) emptyList() else {
        listOf(Pair(Triple(prelude,1,1), File(prelude).reader()))
    }
    return all(false, inps, "out", emptyList())
}

fun main (args: Array<String>) {
    DUMP = false
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
            ys.containsKey("--version") -> println("dceu " + VERSION)
            (xinp == null) -> println("expected filename")
            else -> {
                val f = File(xinp)
                val inps = listOf(
                    Pair(Triple(xinp,1,1), f.reader()),
                    Pair(Triple("prelude.ceu",1,1), FileX("@/prelude.ceu").reader())
                )
                val out = all(ys.containsKey("--verbose"), inps, f.nameWithoutExtension, xccs)
                print(out)
            }
        }
    } catch (e: Throwable) {
        println(e.message!!)
    }
}
