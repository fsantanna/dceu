import org.junit.Test

val D = "\$"

fun trap (f: ()->Unit): String {
    try {
        f()
        error("impossible case")
    } catch (e: Throwable) {
        return e.message!!
    }
}

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair("anon", str.reader())))
}

class TLexer {
    @Test
    fun syms() {
        val l = lexer("{ } ( ; ( = ) ) - , ][ / * +")
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
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun ids() {
        val l = lexer(" if aaa throw nil XXX set loop while vary10 catch else var do _do_ true func b10 false")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "if" })
        assert(tks.next().str == "aaa")
        assert(tks.next().str == "throw")
        assert(tks.next().str == "nil")
        assert(tks.next().str == "XXX")
        assert(tks.next().str == "set")
        assert(tks.next().str == "loop")
        assert(tks.next().str == "while")
        assert(tks.next().str == "vary10")
        assert(tks.next().str == "catch")
        assert(tks.next().str == "else")
        assert(tks.next().str == "var")
        assert(tks.next().str == "do")
        assert(tks.next().str == "_do_")
        assert(tks.next().str == "true")
        assert(tks.next().str == "func")
        assert(tks.next().str == "b10")
        assert(tks.next().str == "false")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun comments() {
        val l = lexer("""
            x - y - z ;;
            var ;;x
            ;;
            val ;;; x
            ;; -
            -
        """.trimIndent())
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==1 && it.pos.col==3 && it.str == "-" })
        assert(tks.next().let { it is Tk.Id  && it.pos.lin==1 && it.pos.col==5 && it.str == "y" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==1 && it.pos.col==7 && it.str == "-" })
        assert(tks.next().let { it is Tk.Id  && it.pos.lin==1 && it.pos.col==9 && it.str == "z" })
        assert(tks.next().let { it is Tk.Fix && it.pos.lin==2 && it.pos.col==1 && it.str == "var" })
        assert(tks.next().let { it is Tk.Id  && it.pos.lin==4 && it.pos.col==1 && it.str == "val" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==6 && it.pos.col==1 && it.str == "-" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==6 && it.pos.col==2 })
    }

    @Test
    fun native() {
        val l = lexer("""
            native { abc }
            native {{ijk}}
            native ( {i${D}jk} )
            native ( {ijk}
        """.trimIndent())
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==1 && it.pos.col==1 && it.str=="{ abc }" })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==2 && it.pos.col==1 && it.str=="{{ijk}}" })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==3 && it.pos.col==1 && it.str=="( {i\$jk} )" })
        assert(trap { tks.next() } == "anon : (lin 4, col 1) : native error : expected \")\"")
    }

    @Test
    fun ops1() {
        val l = lexer("(-) (+) (x) (*/) ( + ((==))")
        val tks = l.lex().iterator()
        //println(tks.next())
        assert(tks.next().let { it is Tk.Id  && it.str == "(-)" })
        assert(tks.next().let { it is Tk.Id  && it.str == "(+)" })
        assert(tks.next().let { it is Tk.Fix && it.str == "(" })
        assert(tks.next().let { it is Tk.Id  && it.str == "x" })
        assert(tks.next().let { it is Tk.Fix && it.str == ")" })
        assert(tks.next().let { it is Tk.Id  && it.str == "(*/)" })
        assert(tks.next().let { it is Tk.Fix && it.str == "(" })
        assert(tks.next().let { it is Tk.Op  && it.str == "+" })
        assert(tks.next().let { it is Tk.Fix && it.str == "(" })
        assert(tks.next().let { it is Tk.Id  && it.str == "(==)" })
        assert(tks.next().let { it is Tk.Fix && it.str == ")" })
        //println(tks.next())
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==1 && it.pos.col==28 })
    }
    @Test
    fun ops2() {
        val l = lexer("> < >= <= == !=")
        val tks = l.lex().iterator()
        assert(tks.next().str == ">")
        assert(tks.next().str == "<")
        assert(tks.next().str == ">=")
        assert(tks.next().str == "<=")
        assert(tks.next().str == "==")
        assert(tks.next().str == "!=")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun ops3() {
        val l = lexer("=== =/= !! (===) (!!)")
        val tks = l.lex().iterator()
        assert(tks.next().str == "===")
        assert(tks.next().str == "=/=")
        assert(tks.next().str == "!!")
        assert(tks.next().str == "(===)")
        assert(tks.next().str == "(!!)")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun ops4_err() {
        val l = lexer("(===")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : operator error : expected \")\"")
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
        assert(tks.next().let { it is Tk.Id  && it.pos.file=="anon"     && it.pos.lin==1 && it.pos.col==1 && it.str == "before" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="test.ceu" && it.pos.lin==1 && it.pos.col==1 && it.str == "1" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="test.ceu" && it.pos.lin==2 && it.pos.col==1 && it.str == "2" })
        assert(tks.next().let { it is Tk.Id  && it.pos.file=="anon"     && it.pos.lin==3 && it.pos.col==1 && it.str == "after" })
        assert(tks.next().let { it is Tk.Id  && it.pos.file=="anon"     && it.pos.lin==5 && it.pos.col==1 && it.str == "first" })
        assert(tks.next().let { it is Tk.Id  && it.pos.file=="xxx.ceu"  && it.pos.lin==7 && it.pos.col==9 && it.str == "xxx" })
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
    fun inc10_err() {
        val l = lexer("^[1]x")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : token ^ error : expected end of line")
    }

    // TAGS

    @Test
    fun tags1() {
        val l = lexer("@tag")
        val tks = l.lex().iterator()
        assert(tks.next().str == "@tag")
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun tags2_err() {
        val l = lexer("@(")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
    }
    @Test
    fun tags3_err() {
        val l = lexer("@")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : tag error : expected identifier")
    }
}
