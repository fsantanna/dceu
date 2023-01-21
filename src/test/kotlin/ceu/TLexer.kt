package ceu

import D
import Lexer
import Tk
import org.junit.Test

fun trap (f: ()->Unit): String {
    try {
        f()
        error("impossible case")
    } catch (e: Throwable) {
        return e.message!!
    }
}

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class TLexer {
    @Test
    fun syms() {
        val l = lexer("{ } ( ; ( = ) ) - , ][ / * + .")
        val tks = l.lex().iterator()
        assert(tks.next().str == "{")
        assert(tks.next().str == "}")
        assert(tks.next().str == "(")
        assert(tks.next().str == "(")
        assert(tks.next().str == "=")
        assert(tks.next().str == ")")
        assert(tks.next().str == ")")
        assert(tks.next().str == "-")
        assert(tks.next().str == ",")
        assert(tks.next().str == "]")
        assert(tks.next().str == "[")
        assert(tks.next().str == "/")
        assert(tks.next().str == "*")
        assert(tks.next().str == "+")
        assert(tks.next().str == ".")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun syms2() {
        val l = lexer("@[ #[ @1")
        val tks = l.lex().iterator()
        assert(tks.next().str == "@[")
        assert(tks.next().str == "#[")
        assert(trap { tks.next() } == "anon : (lin 1, col 7) : operator error : expected \"@[\"")
    }

    @Test
    fun ids() {
        val l = lexer("status if aaa throw tasks evt nil pub task group track enum XXX coro defer err set coroutine spawn loop yield while vary10 catch resume else var do native _do_ broadcast true data func b10 in false")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "status" })
        assert(tks.next().let { it is Tk.Fix && it.str == "if" })
        assert(tks.next().let { it is Tk.Id  && it.str == "aaa" })
        assert(tks.next().let { it is Tk.Id  && it.str == "throw" })
        assert(tks.next().let { it is Tk.Id  && it.str == "tasks" })
        assert(tks.next().let { it is Tk.Fix && it.str == "evt" })
        assert(tks.next().let { it is Tk.Fix && it.str == "nil" })
        assert(tks.next().let { it is Tk.Fix && it.str == "pub" })
        assert(tks.next().let { it is Tk.Fix && it.str == "task" })
        assert(tks.next().let { it is Tk.Id  && it.str == "group" })
        assert(tks.next().let { it is Tk.Id  && it.str == "track" })
        assert(tks.next().let { it is Tk.Fix && it.str == "enum" })
        assert(tks.next().let { it is Tk.Id  && it.str == "XXX" })
        assert(tks.next().let { it is Tk.Fix && it.str == "coro" })
        assert(tks.next().let { it is Tk.Fix && it.str == "defer" })
        assert(tks.next().let { it is Tk.Fix && it.str == "err" })
        assert(tks.next().let { it is Tk.Fix && it.str == "set" })
        assert(tks.next().let { it is Tk.Id  && it.str == "coroutine" })
        assert(tks.next().let { it is Tk.Fix && it.str == "spawn" })
        assert(tks.next().let { it is Tk.Id  && it.str == "loop" })
        assert(tks.next().let { it is Tk.Fix && it.str == "yield" })
        assert(tks.next().let { it is Tk.Fix && it.str == "while" })
        assert(tks.next().let { it is Tk.Id  && it.str == "vary10" })
        assert(tks.next().let { it is Tk.Fix && it.str == "catch" })
        assert(tks.next().let { it is Tk.Fix && it.str == "resume" })
        assert(tks.next().let { it is Tk.Fix && it.str == "else" })
        assert(tks.next().let { it is Tk.Fix && it.str == "var" })
        assert(tks.next().let { it is Tk.Fix && it.str == "do" })
        assert(tks.next().let { it is Tk.Id  && it.str == "native" })
        assert(tks.next().let { it is Tk.Id  && it.str == "_do_" })
        assert(tks.next().let { it is Tk.Fix && it.str == "broadcast" })
        assert(tks.next().let { it is Tk.Fix && it.str == "true" })
        assert(tks.next().let { it is Tk.Fix && it.str == "data" })
        assert(tks.next().let { it is Tk.Fix && it.str == "func" })
        assert(tks.next().let { it is Tk.Id  && it.str == "b10" })
        assert(tks.next().let { it is Tk.Fix && it.str == "in" })
        assert(tks.next().let { it is Tk.Fix && it.str == "false" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun ids2() {
        val l = lexer("and or not")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "and" })
        assert(tks.next().let { it is Tk.Id && it.str == "or" })
        assert(tks.next().let { it is Tk.Id && it.str == "not" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun ids3() {
        val l = lexer("x-1 x-a")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.str == "x" })
        assert(tks.next().let { it is Tk.Op  && it.str == "-" })
        assert(tks.next().let { it is Tk.Num && it.str == "1" })
        assert(tks.next().let { it is Tk.Id  && it.str == "x-a" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun is_isnot() {
        val l = lexer("is isnot")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "is" })
        assert(tks.next().let { it is Tk.Id && it.str == "isnot" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun vararg() {
        val l = lexer(".. ... . .. ....")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "..." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "..." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun comments1() {
        val l = lexer("""
            x - y - z ;;
            var ;;x
            ;;
            val ;; x
            ;; -
            -
        """.trimIndent())
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Op && it.pos.lin==1 && it.pos.col==3 && it.str == "-" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==1 && it.pos.col==5 && it.str == "y" })
        assert(tks.next().let { it is Tk.Op && it.pos.lin==1 && it.pos.col==7 && it.str == "-" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==1 && it.pos.col==9 && it.str == "z" })
        assert(tks.next().let { it is Tk.Fix && it.pos.lin==2 && it.pos.col==1 && it.str == "var" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==4 && it.pos.col==1 && it.str == "val" })
        assert(tks.next().let { it is Tk.Op && it.pos.lin==6 && it.pos.col==1 && it.str == "-" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==6 && it.pos.col==2 })
    }
    @Test
    fun comments2() {
        val l = lexer("""
            x ;;;
            var ;;x
            val ;;; y
            z
        """.trimIndent())
        val tks = l.lex().iterator()
        //println(tks.next())
        assert(tks.next().let { it is Tk.Id && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==3 && it.pos.col==9 && it.str == "y" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==4 && it.pos.col==1 && it.str == "z" })
    }
    @Test
    fun comments3() {
        val l = lexer("""
            x
            ;;;
            ;;;;
            ;;;
            ;;
            ;;;;
            ;;;;
            ;;;
            ;;;;
            ;;;
            y
        """.trimIndent())
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "x" })
        assert(tks.next().let { it is Tk.Id && it.str == "y" })
    }
    @Test
    fun comments4_err() {
        val l = lexer("""
            x
            ;;;
            ;;;;
            ;;;
            y
        """.trimIndent())
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "x" })
        assert(tks.next().let { it is Tk.Eof })
    }
    @Test
    fun comments5_err() {
        val l = lexer("""
            x
            ;;;;
            ;;;
            ;;;;
            y
        """.trimIndent())
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "x" })
        assert(tks.next().let { it is Tk.Id && it.str == "y" })
        assert(tks.next().let { it is Tk.Eof })
    }

    @Test
    fun native() {
        val l = lexer("""
            ` abc `
            `{ijk}`
            ` {i${D}jk} `
            `  {ijk} 
        """.trimIndent())
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==1 && it.pos.col==1 && it.str==" abc " })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==2 && it.pos.col==1 && it.str=="{ijk}" })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==3 && it.pos.col==1 && it.str==" {i\$jk} " })
        //println(tks.next())
        assert(trap { tks.next() } == "anon : (lin 4, col 10) : native error : expected \"`\"")
    }

    @Test
    fun ops1() {
        val l = lexer("{-} {+} {x} {*/} -> ( + ({==})")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.str == "{-}" })
        assert(tks.next().let { it is Tk.Id  && it.str == "{+}" })
        assert(tks.next().let { it is Tk.Fix && it.str == "{" })
        assert(tks.next().let { it is Tk.Id  && it.str == "x" })
        assert(tks.next().let { it is Tk.Fix && it.str == "}" })
        assert(tks.next().let { it is Tk.Id  && it.str == "{*/}" })
        //println(tks.next())
        assert(tks.next().let { it is Tk.Op  && it.str == "->" })
        assert(tks.next().let { it is Tk.Fix && it.str == "(" })
        assert(tks.next().let { it is Tk.Op  && it.str == "+" })
        assert(tks.next().let { it is Tk.Fix && it.str == "(" })
        assert(tks.next().let { it is Tk.Id  && it.str == "{==}" })
        assert(tks.next().let { it is Tk.Fix && it.str == ")" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==1 && it.pos.col==31 })
    }
    @Test
    fun ops2() {
        val l = lexer("> < >= <= == /=")
        val tks = l.lex().iterator()
        assert(tks.next().str == ">")
        assert(tks.next().str == "<")
        assert(tks.next().str == ">=")
        assert(tks.next().str == "<=")
        assert(tks.next().str == "==")
        assert(tks.next().str == "/=")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun ops3() {
        val l = lexer("=== =/= !! {===} {!!}")
        val tks = l.lex().iterator()
        assert(tks.next().str == "===")
        assert(tks.next().str == "=/=")
        assert(tks.next().str == "!!")
        assert(tks.next().str == "{===}")
        assert(tks.next().str == "{!!}")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun ops4_err() {
        val l = lexer("{===")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : operator error : expected \"}\"")
    }

    @Test
    fun chr1() {
        val l = lexer("'x' '\\n' 'abc' '\\\''")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Chr && it.str == "'x'" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'\\n'" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'abc'" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'\\\''" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==1 && it.pos.col==20 })
    }


    // INCLUDE

    @Test
    fun inc1() {
        val l = lexer("""
            before
            ^["test.ceu"]
            after
            ^[5]
            first
            ^["xxx.ceu",7,9]
            xxx
        """.trimIndent())
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.pos.file=="anon"     && it.pos.lin==1 && it.pos.col==1 && it.str == "before" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="test.ceu" && it.pos.lin==1 && it.pos.col==1 && it.str == "1" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="test.ceu" && it.pos.lin==2 && it.pos.col==1 && it.str == "2" })
        assert(tks.next().let { it is Tk.Id && it.pos.file=="anon"     && it.pos.lin==3 && it.pos.col==1 && it.str == "after" })
        assert(tks.next().let { it is Tk.Id && it.pos.file=="anon"     && it.pos.lin==5 && it.pos.col==1 && it.str == "first" })
        assert(tks.next().let { it is Tk.Id && it.pos.file=="xxx.ceu"  && it.pos.lin==7 && it.pos.col==9 && it.str == "xxx" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==7 && it.pos.col==12 })
    }
    @Test
    fun inc2_err() {
        val l = lexer("^[")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error")
    }
    @Test
    fun inc3_err() {
        val l = lexer("^[]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error")
    }
    @Test
    fun inc4_err() {
        val l = lexer("^[jkj]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error")
    }
    @Test
    fun inc5_err() {
        val l = lexer("^[1,]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : invalid ^ token : expected number")
    }
    @Test
    fun inc6_err() {
        val l = lexer("^[1,1,1]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : expected \"]\"")
    }
    @Test
    fun inc7_err() {
        val l = lexer("^[\"")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : unterminated \"")
    }
    @Test
    fun inc8_err() {
        val l = lexer("^[\"xxx\"]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : file not found : xxx")
    }
    @Test
    fun inc9_err() {
        val l = lexer("^")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : expected \"[\"")
    }
    @Test
    fun inc10() {
        val l = lexer("^[1]x")
        val tks = l.lex().iterator()
        assert(tks.next().let { it.pos.lin==1 && it.pos.col==1 && it.str=="x" })
    }
    @Test
    fun inc11() {
        val l = lexer("^[2,10]x")
        val tks = l.lex().iterator()
        assert(tks.next().let { it.pos.lin==2 && it.pos.col==10 && it.str=="x" })
    }

    // UPVALS

    @Test
    fun up1() {
        val l = lexer("^id ^if")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.upv==1 && it.str=="id" && it.pos.lin==1 && it.pos.col==1 })
        assert(trap { tks.next() } == "anon : (lin 1, col 5) : token ^ error : unexpected keyword")
    }
    @Test
    fun up2() {
        val l = lexer("^^x ^^^z")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.upv==2 && it.str=="x" && it.pos.lin==1 && it.pos.col==1 })
        assert(trap { tks.next() } == "anon : (lin 1, col 5) : token ^ error : expected \"[\"")
    }

    // TAGS

    @Test
    fun tags1() {
        val l = lexer(":tag")
        val tks = l.lex().iterator()
        assert(tks.next().str == ":tag")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun tags2_err() {
        val l = lexer(":(")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
    }
    @Test
    fun tags3_err() {
        val l = lexer(":")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
    }
    @Test
    fun tags4() {
        val l = lexer(":X.y.1")
        val tks = l.lex().iterator()
        assert(tks.next().str == ":X.y.1")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun tags6() {
        val l = lexer(":X-y-z")
        val tks = l.lex().iterator()
        assert(tks.next().str == ":X-y-z")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun tags7() {
        val l = lexer(":1.2.3.4.5")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : excess of '.' : max hierarchy of 4")
    }
}
