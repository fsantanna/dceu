package tst_99

import dceu.*
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_99 {
    @Test
    fun aa_02_ops() {
        val l = lexer("\\")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Id  && it.str == "not" })
        assert(tks.next().let { it is Tk.Op  && it.str == "not" })
        assert(tks.next().let { it is Tk.Fix && it.str == "\\" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun aa_03_cmds() {
        val l =
            lexer("ifs thus resume-yield-all await while enum watching par every func where par-and par-or until break skip with")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "thus" })
        assert(tks.next().let { it is Tk.Fix && it.str == "resume-yield-all" })
        assert(tks.next().let { it is Tk.Fix && it.str == "enum" })
        assert(tks.next().let { it is Tk.Fix && it.str == "watching" })
        assert(tks.next().let { it is Tk.Fix && it.str == "where" })
        assert(tks.next().let { it is Tk.Fix && it.str == "skip" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
}
