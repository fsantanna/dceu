import org.junit.Test
import java.io.File

class TExec {

    fun all(inp: String): String {
        val lexer = Lexer("anon", inp.reader())
        val parser = Parser(lexer)
        val s = parser.stmts()
        val c = Code(s)
        File("out.c").writeText(c)
        val (ok2, out2) = exec("gcc -Werror out.c -o out.exe")
        if (!ok2) {
            return out2
        }
        val (_, out3) = exec("./out.exe")
        //println(out3)
        return out3
    }

    @Test
    fun a01_print() {
        val out = all("""
            call print([10])
        """.trimIndent()
        )
        assert(out == "10.000000") { out }
    }
    @Test
    fun a02_print() {
        val out = all("""
            call print([10])
            call println([20])
        """.trimIndent()
        )
        assert(out == "10.00000020.000000\n") { out }
    }
    @Test
    fun a03_print() {
        val out = all("""
            call println([])
            call println([[],[1,2,3]])
        """.trimIndent()
        )
        assert(out == "\n[]\t[1.000000,2.000000,3.000000]\n") { out }
    }
}