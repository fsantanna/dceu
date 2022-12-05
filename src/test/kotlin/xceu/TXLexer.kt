package xceu

import ceu.lexer
import ceu.trap
import org.junit.Test

val D = "\$"

class TXLexer {
    @Test
    fun ids2() {
        val l = lexer("and or where not with ifs every break await par")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "and" })
        assert(tks.next().let { it is Tk.Fix && it.str == "or" })
        assert(tks.next().let { it is Tk.Fix && it.str == "where" })
        assert(tks.next().let { it is Tk.Fix && it.str == "not" })
        assert(tks.next().let { it is Tk.Fix && it.str == "with" })
        assert(tks.next().let { it is Tk.Fix && it.str == "ifs" })
        assert(tks.next().let { it is Tk.Fix && it.str == "every" })
        assert(tks.next().let { it is Tk.Fix && it.str == "break" })
        assert(tks.next().let { it is Tk.Fix && it.str == "await" })
        assert(tks.next().let { it is Tk.Fix && it.str == "par" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun arrow1() {
        val l = lexer("->")
        val tks = l.lex().iterator()
        //println(tks.next())
        assert(tks.next().let { it is Tk.Fix && it.str == "->" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun clk1() {
        val l = lexer("1s 1 h 1h10min30s15ms 10ms")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Clk && it.str == "1s" && it.ms==1000 })
        assert(tks.next().let { it is Tk.Num && it.str == "1" })
        assert(tks.next().let { it is Tk.Id && it.str == "h" })
        assert(tks.next().let { it is Tk.Clk && it.str=="1h10min30s15ms" && it.ms==4230015})
        assert(tks.next().let { it is Tk.Clk && it.str == "10ms" && it.ms==10 })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun clk2() {
        val l = lexer("1s10k")
        val tks = l.lex().iterator()
        //println(tks.next())
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : invalid time constant")
    }
}
