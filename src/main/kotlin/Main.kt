package dceu

import java.io.File
import java.io.Reader
import java.util.*

var LEX = true
var TEST = false
var DUMP = true
var DEBUG = true
var CEU = 1

    //  1: dyn-lex                              ;; 25 "definitely lost" (errors or cycles)
    //  2: defer, throw/catch, do/escape, loop  ;;  4 "definitely lost" (C errors)
    //  3: coro, yield, resume                  ;;  0
    //  4: task, pub, bcast, toggle, delay      ;;  0
    //  5: tasks                                ;;  0
    // 50: lex, drop, val', var'                ;;  6 "definitely lost" (C errors or cycles)
    // 99: sugar                                ;;  0
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

val D = "\$"

// VERSION
const val MAJOR    = 0
const val MINOR    = 4
const val REVISION = 0
const val VERSION  = "v$MAJOR.$MINOR.$REVISION"

val PATH = File(File(System.getProperty("java.class.path")).absolutePath).parent

val KEYWORDS: SortedSet<String> = (
    setOf (
        "data", "do", "else",
        "false", "func'", "group", "if",
        "nil", "set", "true",
        "val", "var",
    ) + (if (CEU < 2) setOf() else setOf (
        "catch", "defer", "enclose'", "escape", "loop'",
    )) + (if (CEU < 3) setOf() else setOf(
        "coro'", "resume", "yield",
    )) + (if (CEU < 4) setOf() else setOf(
        "broadcast", "delay", "in", "pub", "spawn", "task'", "toggle",
    )) + (if (CEU < 5) setOf() else setOf(
        "tasks",
    )) + (if (CEU < 50) setOf() else setOf(
        "drop", "drop'", "val'", "var'",
    )) + (if (CEU < 99) setOf() else setOf(
        "await", "break", "coro", "enum", "every",
        "func", "ifs", "loop", "match",
        "par", "par-and", "par-or",
        "resume-yield-all", "return",
        "skip", "task", "test",
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
    ":dynamic", // error before or after dynamic
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
    ":global", ":task",
)) + (if (CEU < 50) listOf() else listOfNotNull(
    ":fake", ":nested",
)) + listOf(
    ":ceu", ":error", ":pre",
) + (if (CEU < 99) listOf() else listOf(
    ":break", ":skip", ":return",
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

typealias NExpr = Int

object G {
    var N: Int = 1
    var outer: Expr.Do? = null
    var ns: MutableMap<NExpr,Expr> = mutableMapOf()
    var ups: MutableMap<NExpr,NExpr> = mutableMapOf()
    var tags: MutableMap<String,Tk.Tag> = mutableMapOf()
    val datas = mutableMapOf<String,LData>()
    val nats: MutableMap<NExpr,Pair<List<NExpr>,String>> = mutableMapOf()
    var nonlocs: MutableMap<NExpr,List<NExpr>> = mutableMapOf()
    val mems: MutableSet<Expr>  = mutableSetOf()

    fun reset () {
        N = 1
        outer = null
        ns.clear()
        ups.clear()
        tags.clear()
        datas.clear()
        nats.clear()
        nonlocs.clear()
        mems.clear()
    }
}

typealias LData = List<Id_Tag>

enum class Scope {
    GLOBAL, LOCAL, NESTED, UPVAL
}

sealed class Patt (val id: Tk.Id, val tag: Tk.Tag?, val pos: Expr) {
    data class None (val id_: Tk.Id, val tag_: Tk.Tag?, val pos_: Expr): Patt(id_,tag_,pos_)
    data class One  (val id_: Tk.Id, val tag_: Tk.Tag?, val e: Expr, val pos_: Expr): Patt(id_,tag_,pos_)
    data class Tup  (val id_: Tk.Id, val tag_: Tk.Tag?, val l: List<Patt>, val pos_: Expr): Patt(id_,tag_,pos_)
}

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof (val pos_: Pos, val n_: Int=G.N++): Tk("", pos_)
    data class Fix (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Tag (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Op  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Id  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)  // up: 0=var, 1=upvar, 2=upref
    data class Num (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Chr (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Nat (val str_: String, val pos_: Pos, val tag: String?, val n_: Int=G.N++): Tk(str_, pos_)
}

typealias Id_Tag  = Pair<Tk.Id,Tk.Tag?>

sealed class Expr (var n: Int, val tk: Tk) {
    data class Proto   (val tk_: Tk.Fix, val nst: Boolean, val fake: Boolean, val tag: Tk.Tag?, val pars: List<Expr.Dcl>, val blk: Do): Expr(G.N++, tk_)
    data class Do      (val tk_: Tk, val es: List<Expr>) : Expr(G.N++, tk_)
    data class Group   (val tk_: Tk.Fix, val es: List<Expr>) : Expr(G.N++, tk_)
    data class Enclose (val tk_: Tk.Fix, val tag: Tk.Tag, val es: List<Expr>): Expr(G.N++, tk_)
    data class Escape  (val tk_: Tk.Fix, val tag: Tk.Tag, val e: Expr?): Expr(G.N++, tk_)
    data class Dcl     (val tk_: Tk.Fix, val lex: Boolean, /*val poly: Boolean,*/ val idtag: Id_Tag, val src: Expr?):  Expr(G.N++, tk_)
    data class Set     (val tk_: Tk.Fix, val dst: Expr, /*val poly: Tk.Tag?,*/ val src: Expr): Expr(G.N++, tk_)
    data class If      (val tk_: Tk.Fix, val cnd: Expr, val t: Expr, val f: Expr): Expr(G.N++, tk_)
    data class Loop    (val tk_: Tk.Fix, val blk: Expr): Expr(G.N++, tk_)
    data class Data    (val tk_: Tk.Tag, val ids: List<Id_Tag>): Expr(G.N++, tk_)
    data class Drop    (val tk_: Tk.Fix, val e: Expr, val prime: Boolean): Expr(G.N++, tk_)

    data class Catch   (val tk_: Tk.Fix, val tag: Tk.Tag?, val blk: Expr.Do): Expr(G.N++, tk_)
    data class Defer   (val tk_: Tk.Fix, val blk: Expr.Do): Expr(G.N++, tk_)

    data class Yield   (val tk_: Tk.Fix, val e: Expr): Expr(G.N++, tk_)
    data class Resume  (val tk_: Tk.Fix, val co: Expr, val args: List<Expr>): Expr(G.N++, tk_)

    data class Spawn   (val tk_: Tk.Fix, val tsks: Expr?, val tsk: Expr, val args: List<Expr>): Expr(G.N++, tk_)
    data class Delay   (val tk_: Tk.Fix): Expr(G.N++, tk_)
    data class Pub     (val tk_: Tk, val tsk: Expr?): Expr(G.N++, tk_)
    data class Toggle  (val tk_: Tk.Fix, val tsk: Expr, val on: Expr): Expr(G.N++, tk_)
    data class Tasks   (val tk_: Tk.Fix, val max: Expr): Expr(G.N++, tk_)

    data class Nat     (val tk_: Tk.Nat): Expr(G.N++, tk_)
    data class Acc     (val tk_: Tk.Id, val ign: Boolean=false): Expr(G.N++, tk_)
    data class Nil     (val tk_: Tk.Fix): Expr(G.N++, tk_)
    data class Tag     (val tk_: Tk.Tag): Expr(G.N++, tk_)
    data class Bool    (val tk_: Tk.Fix): Expr(G.N++, tk_)
    data class Char    (val tk_: Tk.Chr): Expr(G.N++, tk_)
    data class Num     (val tk_: Tk.Num): Expr(G.N++, tk_)
    data class Tuple   (val tk_: Tk.Fix, val args: List<Expr>): Expr(G.N++, tk_)
    data class Vector  (val tk_: Tk.Fix, val args: List<Expr>): Expr(G.N++, tk_)
    data class Dict    (val tk_: Tk.Fix, val args: List<Pair<Expr,Expr>>): Expr(G.N++, tk_)
    data class Index   (val tk_: Tk, val col: Expr, val idx: Expr): Expr(G.N++, tk_)
    data class Call    (val tk_: Tk, val clo: Expr, val args: List<Expr>): Expr(G.N++, tk_)
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
    G.reset()
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
    //println(es.to_str())
    val c = try {
        if (verbose) {
            System.err.println("... analysing ...")
        }
        //readLine()
        val pos = Pos("anon", 0, 0, 0)
        val tk0 = Tk.Fix("", pos.copy())

        val glbs = GLOBALS.map {
            Expr.Dcl (
                Tk.Fix("val", pos.copy()),
                true,
                Pair(Tk.Id(it,pos.copy(),0), null),
                null
            )
        }
        val xargs = Expr.Dcl (
            Tk.Fix("val",pos.copy()),
            true,
            Pair(Tk.Id("ARGS",pos.copy()),null),
            null
        )

        G.outer = Expr.Do(tk0, listOf(xargs)+glbs+es)
        //println(G.outer)
        cache_ns()
        cache_ups()
        cache_tags()
        check_tags()
        check_vars()
        cache_nonlocs()
        check_statics()
        //Static()
        //G.outer = G.outer!!.prune() as Expr.Do
        //println(G.outer!!.to_str())
        //rets.pub.forEach { println(listOf(it.value,it.key.javaClass.name,it.key.tk.pos.lin)) }
        if (verbose) {
            System.err.println("... ceu -> c ...")
        }
        val coder = Coder()
        coder.main()
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
    val cmd = listOf("gcc", "-Werror", "$out.c", "-l", "m", "-o", "$out.exe") + args
    if (verbose) {
        System.err.println("\t" + cmd.joinToString(" "))
    }
    val (ok2, out2) = exec(true, cmd)
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

        LEX  = ys.containsKey("--lex")
        TEST = ys.containsKey("--test")
        DEBUG = ys.containsKey("--debug")
        DUMP = DEBUG

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
