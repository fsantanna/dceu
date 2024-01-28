package tst_01

import dceu.*
import org.junit.BeforeClass
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_01 {
    @Test
    fun aa_01_syms() {
        val l = lexer("{ } ( ; ( = ) ) - , ][ / * + . =>")
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
        assert(tks.next().str == "=>")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun aa_02_syms() {
        val l = lexer("@[ #[ @1")
        val tks = l.lex().iterator()
        assert(tks.next().str == "@[")
        assert(tks.next().str == "#[")
        assert(tks.next().str == "@")
        assert(tks.next().str == "1")
    }
    @Test
    fun aa_03_syms_err() {
        val l = lexer(":")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
    }

    @Test
    fun bb_01_ids() {
        val l =
            lexer("status if aaa skip thus break throw tasks evt export as nil pub task poly group track enum XXX coro defer err set coroutine spawn loop yield while vary10 catch resume else var do native _do_ broadcast true data func b10 in false")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.str == "status" })
        assert(tks.next().let { it is Tk.Fix && it.str == "if" })
        assert(tks.next().let { it is Tk.Id  && it.str == "aaa" })
        assert(tks.next().let { it is Tk.Fix && it.str == "skip" })
        assert(tks.next().let { it is Tk.Id  && it.str == "thus" })
        assert(tks.next().let { it is Tk.Fix && it.str == "break" })
        assert(tks.next().let { it is Tk.Id  && it.str == "throw" })
        assert(tks.next().let { it is Tk.Id  && it.str == "tasks" })
        assert(tks.next().let { it is Tk.Id  && it.str == "evt" })
        assert(tks.next().let { it is Tk.Id  && it.str == "export" })
        assert(tks.next().let { it is Tk.Id  && it.str == "as" })
        assert(tks.next().let { it is Tk.Fix && it.str == "nil" })
        assert(tks.next().let { it is Tk.Id  && it.str == "pub" })
        assert(tks.next().let { it is Tk.Id  && it.str == "task" })
        //assert(tks.next().let { it is Tk.Fix && it.str == "poly" })
        assert(tks.next().let { it is Tk.Id  && it.str == "poly" })
        assert(tks.next().let { it is Tk.Id  && it.str == "group" })
        assert(tks.next().let { it is Tk.Id  && it.str == "track" })
        assert(tks.next().let { it is Tk.Fix && it.str == "enum" })
        assert(tks.next().let { it is Tk.Id  && it.str == "XXX" })
        assert(tks.next().let { it is Tk.Id  && it.str == "coro" })
        assert(tks.next().let { it is Tk.Id  && it.str == "defer" })
        assert(tks.next().let { it is Tk.Id  && it.str == "err" })
        assert(tks.next().let { it is Tk.Fix && it.str == "set" })
        assert(tks.next().let { it is Tk.Id  && it.str == "coroutine" })
        assert(tks.next().let { it is Tk.Id  && it.str == "spawn" })
        assert(tks.next().let { it is Tk.Fix && it.str == "loop" })
        assert(tks.next().let { it is Tk.Id  && it.str == "yield" })
        assert(tks.next().let { it is Tk.Id  && it.str == "while" })
        assert(tks.next().let { it is Tk.Id  && it.str == "vary10" })
        assert(tks.next().let { it is Tk.Id  && it.str == "catch" })
        assert(tks.next().let { it is Tk.Id  && it.str == "resume" })
        assert(tks.next().let { it is Tk.Fix && it.str == "else" })
        assert(tks.next().let { it is Tk.Fix && it.str == "var" })
        assert(tks.next().let { it is Tk.Fix && it.str == "do" })
        assert(tks.next().let { it is Tk.Id  && it.str == "native" })
        assert(tks.next().let { it is Tk.Id  && it.str == "_do_" })
        assert(tks.next().let { it is Tk.Id  && it.str == "broadcast" })
        assert(tks.next().let { it is Tk.Fix && it.str == "true" })
        assert(tks.next().let { it is Tk.Fix && it.str == "data" })
        assert(tks.next().let { it is Tk.Fix && it.str == "func" })
        assert(tks.next().let { it is Tk.Id  && it.str == "b10" })
        assert(tks.next().let { it is Tk.Id  && it.str == "in" })
        assert(tks.next().let { it is Tk.Fix && it.str == "false" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun bb_02_ids() {
        val l = lexer("and or not")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "and" })
        assert(tks.next().let { it is Tk.Id && it.str == "or" })
        assert(tks.next().let { it is Tk.Id && it.str == "not" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun bb_03_ids() {
        val l = lexer("x-1 x-a")
        val tks = l.lex().iterator()
        //assert(tks.next().let { it is Tk.Id  && it.str == "x-1" })
        assert(tks.next().let { it is Tk.Id  && it.str == "x" })
        assert(tks.next().let { it is Tk.Op  && it.str == "-" })
        assert(tks.next().let { it is Tk.Num && it.str == "1" })
        assert(tks.next().let { it is Tk.Id  && it.str == "x-a" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun cc_01_vararg() {
        val l = lexer(".. ... . .. ....")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Id  && it.str == "..." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Id  && it.str == "..." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun dd_01_comments() {
        val l = lexer(
            """
            x - y - z ;;
            var ;;x
            ;;
            val ;; x
            ;; -
            -
        """.trimIndent()
        )
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Op && it.pos.lin==1 && it.pos.col==3 && it.str == "-" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==1 && it.pos.col==5 && it.str == "y" })
        assert(tks.next().let { it is Tk.Op && it.pos.lin==1 && it.pos.col==7 && it.str == "-" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==1 && it.pos.col==9 && it.str == "z" })
        assert(tks.next().let { it is Tk.Fix && it.pos.lin==2 && it.pos.col==1 && it.str == "var" })
        assert(tks.next().let { it is Tk.Fix && it.pos.lin==4 && it.pos.col==1 && it.str == "val" })
        assert(tks.next().let { it is Tk.Op && it.pos.lin==6 && it.pos.col==1 && it.str == "-" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==6 && it.pos.col==2 })
    }
    @Test
    fun dd_02_comments() {
        val l = lexer(
            """
            x ;;;
            var ;;x
            val ;;; y
            z
        """.trimIndent()
        )
        val tks = l.lex().iterator()
        //println(tks.next())
        assert(tks.next().let { it is Tk.Id && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==3 && it.pos.col==9 && it.str == "y" })
        assert(tks.next().let { it is Tk.Id && it.pos.lin==4 && it.pos.col==1 && it.str == "z" })
    }
    @Test
    fun dd_03_comments() {
        val l = lexer(
            """
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
        """.trimIndent()
        )
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "x" })
        assert(tks.next().let { it is Tk.Id && it.str == "y" })
    }
    @Test
    fun dd_04_comments_err() {
        val l = lexer(
            """
            x
            ;;;
            ;;;;
            ;;;
            y
        """.trimIndent()
        )
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "x" })
        assert(tks.next().let { it is Tk.Eof })
    }
    @Test
    fun dd_05_comments_err() {
        val l = lexer(
            """
            x
            ;;;;
            ;;;
            ;;;;
            y
        """.trimIndent()
        )
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id && it.str == "x" })
        assert(tks.next().let { it is Tk.Id && it.str == "y" })
        assert(tks.next().let { it is Tk.Eof })
    }

    @Test
    fun ee_01_native() {
        val l = lexer(
            """
            ` abc `
            `{ijk}`
            ` {i${D}jk} `
            `  {ijk} 
        """.trimIndent()
        )
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==1 && it.pos.col==1 && it.str==" abc " })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==2 && it.pos.col==1 && it.str=="{ijk}" })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==3 && it.pos.col==1 && it.str==" {i\$jk} " })
        //println(tks.next())
        assert(trap { tks.next() } == "anon : (lin 4, col 10) : native error : expected \"`\"")
    }

    @Test
    fun ff_01_ops() {
        val l = lexer("{{-}} {{+}} {{x}} {{*/}} -> <- --> <-- ( + ({{==}})")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.str == "{{-}}" })
        assert(tks.next().let { it is Tk.Id  && it.str == "{{+}}" })
        //assert(tks.next().let { it is Tk.Fix && it.str == "{" })
        assert(tks.next().let { it is Tk.Op  && it.str == "x" })
        //assert(tks.next().let { it is Tk.Fix && it.str == "}" })
        assert(tks.next().let { it is Tk.Id  && it.str == "{{*/}}" })
        assert(tks.next().let { it is Tk.Op  && it.str == "->" })
        assert(tks.next().let { it is Tk.Op  && it.str == "<-" })
        assert(tks.next().let { it is Tk.Op  && it.str == "-->" })
        assert(tks.next().let { it is Tk.Op  && it.str == "<--" })
        assert(tks.next().let { it is Tk.Fix && it.str == "(" })
        assert(tks.next().let { it is Tk.Op  && it.str == "+" })
        assert(tks.next().let { it is Tk.Fix && it.str == "(" })
        assert(tks.next().let { it is Tk.Id  && it.str == "{{==}}" })
        assert(tks.next().let { it is Tk.Fix && it.str == ")" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==1 && it.pos.col==52 })
    }
    @Test
    fun ff_02_ops() {
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
    fun ff_03_ops() {
        val l = lexer("=== =/= !! {{===}} {{!!}}")
        val tks = l.lex().iterator()
        assert(tks.next().str == "===")
        assert(tks.next().str == "=/=")
        assert(tks.next().str == "!!")
        assert(tks.next().str == "{{===}}")
        assert(tks.next().str == "{{!!}}")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun ff_04_ops_err() {
        val l = lexer("{{===}")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : operator error : expected \"}\"")
    }
    @Test
    fun ff_05_ops_err() {
        val l = lexer("{{x-1}} {{x=}}")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Op && it.str == "x-1" })
        assert(tks.next().let { it is Tk.Op && it.str == "x=" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==1 && it.pos.col==15 })
        //assert(trap { tks.next() } == "anon : (lin 1, col 9) : operator error : invalid identifier")
    }

    @Test
    fun gg_01_chr() {
        val l = lexer("'x' '\\n' '\\'' '\\\\'")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Chr && it.str == "'x'" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'\\n'" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'\\\''" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'\\\\'" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==1 && it.pos.col==19 })
    }
    @Test
    fun gg_02_chr_err() {
        val l = lexer("'x")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 3) : char error : expected '")
    }
    @Test
    fun gg_03_chr_err() {
        val l = lexer("'\\'")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 4) : char error : expected '")
    }
    @Test
    fun gg_04_chr_err() {
        val l = lexer("'\\n")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 4) : char error : expected '")
    }
    @Test
    fun gg_05_chr_err() {
        val l = lexer("'abc'")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 4) : char error : expected '")
    }


    // INCLUDE

    @Test
    fun hh_01_inc() {
        val l = lexer(
            """
            before
            ^["test.ceu"]
            after
            ^[5]
            first
            ^["xxx.ceu",7,9]
            xxx
        """.trimIndent()
        )
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.pos.file=="anon"     && it.pos.lin==1 && it.pos.col==1 && it.str == "before" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="test.ceu" && it.pos.lin==1 && it.pos.col==1 && it.str == "1" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="test.ceu" && it.pos.lin==2 && it.pos.col==1 && it.str == "2" })
        assert(tks.next().let { it is Tk.Id  && it.pos.file=="anon"     && it.pos.lin==3 && it.pos.col==1 && it.str == "after" })
        assert(tks.next().let { it is Tk.Id  && it.pos.file=="anon"     && it.pos.lin==5 && it.pos.col==1 && it.str == "first" })
        assert(tks.next().let { it is Tk.Id  && it.pos.file=="xxx.ceu"  && it.pos.lin==7 && it.pos.col==9 && it.str == "xxx" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==7 && it.pos.col==12 })
    }
    @Test
    fun hh_02_inc_err() {
        val l = lexer("^[")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error")
    }
    @Test
    fun hh_03_inc_err() {
        val l = lexer("^[]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error")
    }
    @Test
    fun hh_04_inc_err() {
        val l = lexer("^[jkj]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error")
    }
    @Test
    fun hh_05_inc_err() {
        val l = lexer("^[1,]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : expected number")
    }
    @Test
    fun hh_06_inc_err() {
        val l = lexer("^[1,1,1]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : expected \"]\"")
    }
    @Test
    fun hh_07_inc_err() {
        val l = lexer("^[\"")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : unterminated \"")
    }
    @Test
    fun hh_08_inc_err() {
        val l = lexer("^[\"xxx\"]")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : file not found : xxx")
    }
    @Test
    fun hh_09_inc_err() {
        val l = lexer("^")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : expected \"^[\"")
    }
    @Test
    fun hh_10_inc() {
        val l = lexer("^[1]x")
        val tks = l.lex().iterator()
        assert(tks.next().let { it.pos.lin==1 && it.pos.col==1 && it.str=="x" })
    }
    @Test
    fun hh_11_inc() {
        val l = lexer("^[2,10]x")
        val tks = l.lex().iterator()
        assert(tks.next().let { it.pos.lin==2 && it.pos.col==10 && it.str=="x" })
    }

    // UPVALS

    @Test
    fun ii_01_up() {
        val l = lexer("^id ^if")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : expected \"^[\"")
        //assert(tks.next().let { it is Tk.Id && it.upv==1 && it.str=="id" && it.pos.lin==1 && it.pos.col==1 })
        //assert(trap { tks.next() } == "anon : (lin 1, col 5) : token ^ error : unexpected keyword")
    }
    @Test
    fun ii_02_up() {
        val l = lexer("^^x ^^^z")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : expected \"^[\"")
        //assert(tks.next().let { it is Tk.Id && it.upv==2 && it.str=="x" && it.pos.lin==1 && it.pos.col==1 })
        //assert(trap { tks.next() } == "anon : (lin 1, col 5) : token ^ error : expected \"[\"")
    }

    // TAGS

    @Test
    fun jj_01_tags() {
        val l = lexer(":tag")
        val tks = l.lex().iterator()
        assert(tks.next().str == ":tag")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun jj_02_tags_err() {
        val l = lexer(":(")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
    }
    @Test
    fun jj_03_tags_err() {
        val l = lexer(":")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
    }
    @Test
    fun jj_04_tags() {
        val l = lexer(":X.y.z")
        val tks = l.lex().iterator()
        assert(tks.next().str == ":X.y.z")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun jj_05_tags() {
        val l = lexer(":X.y.1")
        val tks = l.lex().iterator()
        assert(tks.next().str == ":X.y")
        assert(tks.next().str == ".")
        assert(tks.next().str == "1")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun jj_06_tags() {
        val l = lexer(":X-y-z")
        val tks = l.lex().iterator()
        assert(tks.next().str == ":X-y-z")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun jj_07_tags() {
        val l = lexer(":a.b.c.d.e")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : excess of '.' : max hierarchy of 4")
    }
    @Test
    fun jj_08_tags_err() {
        val l = lexer(":{x")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
        //assert(trap { tks.next() } == "anon : (lin 1, col 1) : token : error : unterminated {")
    }
    @Test
    fun jj_09_tags() {
        val l = lexer(":{()}")
        val tks = l.lex().iterator()
        //println(tks.next())
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
        //assert(tks.next().str == ":()")
        //assert(tks.next() is Tk.Eof)
    }
}
