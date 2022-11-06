import org.junit.Test
import java.io.File

class TExec {

    fun all(inp: String): String {
        val lexer = Lexer("anon", inp.reader())
        val parser = Parser(lexer)
        val s = parser.stmt()
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
            call print(10)
        """.trimIndent()
        )
        assert(out == "10.000000\n") { out }
    }
}