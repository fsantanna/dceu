package dceu

import java.io.File
import java.io.Reader
import java.util.*

var DUMP = true
var DEBUG = false
var CEU = 1

    // 1: dyn-lex               ;; 3 "definitely lost"
    // 2: defer, throw/catch    ;; 5 "definitely lost"
    // 3: coro, yield, resume   ;; 0 "definitely lost"
    // 4: task, pub, bcast, toggle, delay   ;; 2 "definitely lost"
    // 5: tasks                 ;; 0 "definitely lost"
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
var STACK = 256
val MULTI = -1

// VERSION
const val MAJOR    = 0
const val MINOR    = 3
const val REVISION = 1
const val VERSION  = "v$MAJOR.$MINOR.$REVISION"

val PATH = File(File(System.getProperty("java.class.path")).absolutePath).parent

val KEYWORDS: SortedSet<String> = (
    setOf (
        "break", "data", "do", "else",
        "enum", "false", "func", "if",
        "loop", "nil", "set", "skip",
        "true", "val", "var",
    ) + (if (CEU < 2) setOf() else setOf (
        "catch", "defer",
    )) + (if (CEU < 3) setOf() else setOf(
        "coro", "resume", "yield",
    )) + (if (CEU < 4) setOf() else setOf(
        "broadcast", "delay", "in", "pub", "spawn", "task", "toggle",
    )) + (if (CEU < 99) setOf() else setOf(
        "await", "every", "ifs", "match",
        "par", "par-and", "par-or",
        "resume-yield-all", "thus", "until", "watching",
        "with", "where", "while"
    ))
).toSortedSet()

val OPERATORS = setOf('+', '-', '*', '/', '>', '<', '=', '!', '|', '&', '~', '%', '#', '@')
val XOPERATORS = if (CEU < 99) setOf() else {
    setOf("and", "in?", "in-not?", "is?", "is-not?", "not", "or")
}

val TAGS = listOf (
    ":nil", ":error", ":tag", ":bool", ":char", ":number", ":pointer",
    ":dynamic",
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
    "next-tasks", "tasks",
))

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
typealias Dcl_Idx = Pair<Expr.Dcl,Int>

sealed class Expr (val n: Int, val tk: Tk) {
    data class Proto  (val tk_: Tk.Fix, val nst: Boolean, val tag: Tk.Tag?, val dots: Boolean, val pars: List<Pair<Tk.Id, Tk.Tag?>>, val blk: Do): Expr(N++, tk_)
    data class Do     (val tk_: Tk, val es: List<Expr>) : Expr(N++, tk_)
    data class Dcl    (val tk_: Tk.Fix, val idtag: List<Id_Tag>, /*val poly: Boolean,*/ val src: Expr?):  Expr(N++, tk_)
    data class Set    (val tk_: Tk.Fix, val dst: Expr, /*val poly: Tk.Tag?,*/ val src: Expr): Expr(N++, tk_)
    data class If     (val tk_: Tk.Fix, val cnd: Expr, val t: Expr.Do, val f: Expr.Do): Expr(N++, tk_)
    data class Loop   (val tk_: Tk.Fix, val blk: Expr.Do): Expr(N++, tk_)
    data class Break  (val tk_: Tk.Fix, val cnd: Expr, val e: Expr?): Expr(N++, tk_)
    data class Skip   (val tk_: Tk.Fix, val cnd: Expr): Expr(N++, tk_)
    data class Enum   (val tk_: Tk.Fix, val tags: List<Pair<Tk.Tag,Tk.Nat?>>): Expr(N++, tk_)
    data class Data   (val tk_: Tk.Tag, val ids: List<Id_Tag>): Expr(N++, tk_)
    data class Pass   (val tk_: Tk.Fix, val e: Expr): Expr(N++, tk_)

    data class Catch  (val tk_: Tk.Fix, val cnd: Expr.Do, val blk: Expr.Do): Expr(N++, tk_)
    data class Defer  (val tk_: Tk.Fix, val blk: Expr.Do): Expr(N++, tk_)

    data class Yield  (val tk_: Tk.Fix, val args: Expr.Args): Expr(N++, tk_)
    data class Resume (val tk_: Tk.Fix, val co: Expr, val args: Expr.Args): Expr(N++, tk_)

    data class Spawn  (val tk_: Tk.Fix, val tsks: Expr?, val tsk: Expr, val args: Expr.Args): Expr(N++, tk_)
    data class Delay  (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Pub    (val tk_: Tk, val tsk: Expr?): Expr(N++, tk_)
    data class Toggle (val tk_: Tk.Fix, val tsk: Expr, val on: Expr): Expr(N++, tk_)

    data class Nat    (val tk_: Tk.Nat): Expr(N++, tk_)
    data class Acc    (val tk_: Tk.Id, val ign: Boolean=false): Expr(N++, tk_)
    data class Nil    (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Tag    (val tk_: Tk.Tag): Expr(N++, tk_)
    data class Bool   (val tk_: Tk.Fix): Expr(N++, tk_)
    data class Char   (val tk_: Tk.Chr): Expr(N++, tk_)
    data class Num    (val tk_: Tk.Num): Expr(N++, tk_)
    data class Tuple  (val tk_: Tk.Fix, val args: Expr.Args): Expr(N++, tk_)
    data class Vector (val tk_: Tk.Fix, val args: Expr.Args): Expr(N++, tk_)
    data class Dict   (val tk_: Tk.Fix, val args: List<Pair<Expr,Expr>>): Expr(N++, tk_)
    data class Index  (val tk_: Tk, val col: Expr, val idx: Expr): Expr(N++, tk_)
    data class Call   (val tk_: Tk, val clo: Expr, val args: Expr.Args): Expr(N++, tk_)
    data class VA_len (val tk_: Tk.Fix): Expr(N++, tk_)
    data class VA_idx (val tk_: Tk.Fix, val idx: Expr): Expr(N++, tk_)
    data class Args   (val tk_: Tk.Fix, val dots: Boolean, val es: List<Expr>): Expr(N++, tk_)
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
    //println(es.tostr())
    //println(es)
    val c = try {
        if (verbose) {
            System.err.println("... analysing ...")
        }
        //readLine()
        val pos = Pos("anon", 0, 0)
        val glbs = GLOBALS.map { Expr.Dcl(Tk.Fix("val",pos), listOf(Pair(Tk.Id(it,pos),null)), null) }
        val outer = Expr.Call (
            Tk.Fix("main", pos),
            Expr.Proto (
                Tk.Fix("func",pos), false, null,
                true, listOf(),
                Expr.Do(Tk.Fix("",pos), glbs + es)
            ),
            Expr.Args(Tk.Fix("(",pos), false, listOf())
        )
        val ups    = Ups(outer)
        val tags   = Tags(outer)
        val vars   = Vars(outer, ups)
        val sta    = Static(outer, ups, vars)
        val rets   = Rets(outer, ups)
        //rets.pub.forEach { println(listOf(it.value,it.key.javaClass.name,it.key.tk.pos.lin)) }
        if (verbose) {
            System.err.println("... ceu -> c ...")
        }
        val coder = Coder(outer, ups, vars, sta, rets)
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
