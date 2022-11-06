import org.junit.Test
import java.io.File

class TExec {

    fun all(inp: String): String {
        val lexer = Lexer("anon", inp.reader())
        val parser = Parser(lexer)
        val es = parser.exprs()
        val c = Code(es)
        File("out.c").writeText(c)
        val (ok2, out2) = exec("gcc -Werror out.c -o out.exe")
        if (!ok2) {
            return out2
        }
        val (_, out3) = exec("./out.exe")
        //println(out3)
        return out3
    }

    // PRINT

    @Test
    fun a01_print() {
        val out = all("""
            print([10])
        """.trimIndent()
        )
        assert(out == "10.000000") { out }
    }
    @Test
    fun a02_print() {
        val out = all("""
            print([10])
            println([20])
        """.trimIndent()
        )
        assert(out == "10.00000020.000000\n") { out }
    }
    @Test
    fun a03_print() {
        val out = all("""
            println([])
            println([[],[1,2,3]])
        """.trimIndent()
        )
        assert(out == "\n[]\t[1.000000,2.000000,3.000000]\n") { out }
    }
    @Test
    fun a04_print_err() {
        val out = all("""
            println(1)
        """.trimIndent()
        )
        assert(out.contains("cannot print : expected tuple argument")) { out }
    }
    @Test
    fun a05_print_err() {
        val out = all("""
            println()
        """.trimIndent()
        )
        assert(out.contains("error: too few arguments to function ‘println’")) { out }
    }

    // INDEX

    @Test
    fun a06_index() {
        val out = all("""
            println([[1,2,3][1]])
        """.trimIndent()
        )
        assert(out == "2.000000\n") { out }
    }
    @Test
    fun a07_index_err() {
        val out = all("""
            println(1[1])
        """.trimIndent()
        )
        assert(out.contains("index error : expected tuple")) { out }
    }
    @Test
    fun a08_index_err() {
        val out = all("""
            println([1][[]])
        """.trimIndent()
        )
        assert(out.contains("index error : expected number")) { out }
    }
    @Test
    fun a09_index_err() {
        val out = all("""
            println([1][2])
        """.trimIndent()
        )
        assert(out.contains("index error : out of bounds")) { out }
    }
}