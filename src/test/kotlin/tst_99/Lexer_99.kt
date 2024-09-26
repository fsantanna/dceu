package tst_99

import dceu.*
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_99 {
    @Test
    fun aa_01_ops() {
        val l =
            lexer("and or not in? is? in-not? is-not?")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Op && it.str == "and" })
        assert(tks.next().let { it is Tk.Op && it.str == "or" })
        assert(tks.next().let { it is Tk.Op && it.str == "not" })
        assert(tks.next().let { it is Tk.Op && it.str == "in?" })
        assert(tks.next().let { it is Tk.Op && it.str == "is?" })
        assert(tks.next().let { it is Tk.Op && it.str == "in-not?" })
        assert(tks.next().let { it is Tk.Op && it.str == "is-not?" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun aa_02_ops() {
        val l = lexer("{{not}} not -> --> <-- <- \\")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.str == "not" })
        assert(tks.next().let { it is Tk.Op  && it.str == "not" })
        assert(tks.next().let { it is Tk.Fix && it.str == "->" })
        assert(tks.next().let { it is Tk.Fix && it.str == "-->" })
        assert(tks.next().let { it is Tk.Fix && it.str == "<--" })
        assert(tks.next().let { it is Tk.Fix && it.str == "<-" })
        assert(tks.next().let { it is Tk.Fix && it.str == "\\" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun BUG_aa_03_ops_err() {
        val l = lexer("{{f.x}}")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.str == "TODO" })
        assert(!tks.hasNext())
    }
    @Test
    fun aa_03_cmds() {
        val l =
            lexer("ifs thus resume-yield-all await while enum watching par every func where par-and par-or until break skip with")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "ifs" })
        assert(tks.next().let { it is Tk.Fix && it.str == "thus" })
        assert(tks.next().let { it is Tk.Fix && it.str == "resume-yield-all" })
        assert(tks.next().let { it is Tk.Fix && it.str == "await" })
        assert(tks.next().let { it is Tk.Fix && it.str == "while" })
        assert(tks.next().let { it is Tk.Fix && it.str == "enum" })
        assert(tks.next().let { it is Tk.Fix && it.str == "watching" })
        assert(tks.next().let { it is Tk.Fix && it.str == "par" })
        assert(tks.next().let { it is Tk.Fix && it.str == "every" })
        assert(tks.next().let { it is Tk.Fix && it.str == "func" })
        assert(tks.next().let { it is Tk.Fix && it.str == "where" })
        assert(tks.next().let { it is Tk.Fix && it.str == "par-and" })
        assert(tks.next().let { it is Tk.Fix && it.str == "par-or" })
        assert(tks.next().let { it is Tk.Fix && it.str == "until" })
        assert(tks.next().let { it is Tk.Fix && it.str == "break" })
        assert(tks.next().let { it is Tk.Fix && it.str == "skip" })
        assert(tks.next().let { it is Tk.Fix && it.str == "with" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    @Test
    fun bb_01_clk() {
        val l = lexer("1:h x :min 30:s (1) :ms")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Num && it.str == "1" })
        assert(tks.next().let { it is Tk.Tag && it.str == ":h" })
        assert(tks.next().let { it is Tk.Id  && it.str == "x" })
        assert(tks.next().let { it is Tk.Tag && it.str == ":min" })
        assert(tks.next().let { it is Tk.Num && it.str == "30" })
        assert(tks.next().let { it is Tk.Tag && it.str == ":s" })
        assert(tks.next().let { it is Tk.Fix && it.str == "(" })
        assert(tks.next().let { it is Tk.Num && it.str == "1" })
        assert(tks.next().let { it is Tk.Fix && it.str == ")" })
        assert(tks.next().let { it is Tk.Tag && it.str == ":ms" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun bb_02_clk() {
        val l = lexer("1s10k")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Num && it.str == "1s10k" })
        //println(tks.next())
        //assert(trap { tks.next() } == "anon : (lin 1, col 1) : invalid time constant")
    }
}
