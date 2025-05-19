package tst_01

import dceu.*
import org.junit.BeforeClass
import org.junit.Test

fun lexer (str: String): Lexer {
    return Lexer(listOf(Pair(Triple("anon",1,1), str.reader())))
}

class Lexer_01 {
    @Test
    fun cc_01_vararg() {
        val l = lexer(".. ... .")
        val tks = l.lex().iterator()
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next().let { it is Tk.Fix && it.str == "." })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
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
        //print(tks.next())
        assert(trap { tks.next() } == "anon : (lin 4, col 10) : native throw : expected \"`\"")
    }
    @Test
    fun ff_03_ops() {
        val l = lexer("=== =/=")
        val tks = l.lex().iterator()
        assert(tks.next().str == "===")
        assert(tks.next().str == "=/=")
        assert(tks.next().str == "{{===}}")
        assert(tks.next().str == "!!")
        assert(tks.next() is Tk.Eof)
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
        assert(trap { tks.next() } == "anon : (lin 1, col 3) : char throw : expected '")
    }
    @Test
    fun gg_03_chr_err() {
        val l = lexer("'\\'")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 4) : char throw : expected '")
    }
    @Test
    fun gg_04_chr_err() {
        val l = lexer("'\\n")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 4) : char throw : expected '")
    }
    @Test
    fun gg_06_chr() {
        val l = lexer("\"\\\\\"")
        val tks = l.lex().iterator()
        assert(tks.next().let { it.str=="#["})
        print(tks.next().let { it.str=="\\" })
        assert(tks.next().let { it.str=="]"})
    }
    @Test
    fun gg_07_chr() {
        val l = lexer("\"\\\"")
        val tks = l.lex().iterator()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : string throw : unterminated \"")
    }
    @Test
    fun gg_08_chr() {
        val l = lexer("'\\n'")
        val tks = l.lex().iterator()
        assert(tks.next().str == "'\\n'")
    }
}
