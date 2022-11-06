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
    fun print1() {
        val out = all("""
            print([10])
        """.trimIndent()
        )
        assert(out == "10.000000") { out }
    }
    @Test
    fun print2() {
        val out = all("""
            print([10])
            println([20])
        """.trimIndent()
        )
        assert(out == "10.00000020.000000\n") { out }
    }
    @Test
    fun print3() {
        val out = all("""
            println([])
            println([[],[1,2,3]])
        """.trimIndent()
        )
        assert(out == "\n[]\t[1.000000,2.000000,3.000000]\n") { out }
    }
    @Test
    fun print_err1() {
        val out = all("""
            println(1)
        """.trimIndent()
        )
        assert(out.contains("cannot print : expected tuple argument")) { out }
    }
    @Test
    fun print_err2() {
        val out = all("""
            println()
        """.trimIndent()
        )
        assert(out.contains("error: too few arguments to function ‘println’")) { out }
    }

    // INDEX

    @Test
    fun index() {
        val out = all("""
            println([[1,2,3][1]])
        """.trimIndent()
        )
        assert(out == "2.000000\n") { out }
    }
    @Test
    fun index_err1() {
        val out = all("""
            println(1[1])
        """.trimIndent()
        )
        assert(out.contains("index error : expected tuple")) { out }
    }
    @Test
    fun index_err2() {
        val out = all("""
            println([1][[]])
        """.trimIndent()
        )
        assert(out.contains("index error : expected number")) { out }
    }
    @Test
    fun index_err3() {
        val out = all("""
            println([1][2])
        """.trimIndent()
        )
        assert(out.contains("index error : out of bounds")) { out }
    }

    // DCL

    @Test
    fun dcl() {
        val out = all("""
            var x
            println([x])
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }

    // SET

    @Test
    fun set1() {
        val out = all("""
            var x
            set x = [10]
            println([x])
        """.trimIndent()
        )
        assert(out == "[10.000000]\n") { out }
    }
    @Test
    fun set2() {
        val out = all("""
            var x
            set x = [10,20,[30]]
            set x[1] = 22
            set x[2][0] = 33
            println([x])
        """.trimIndent()
        )
        assert(out == "[10.000000,22.000000,[33.000000]]\n") { out }
    }
    @Test
    fun todo_set_err1() {
        val out = all("""
            set 1 = 1
        """.trimIndent()
        )
        assert(out == "TODO: cannot assign to 1") { out }
    }
    @Test
    fun todo_set_err2() {
        val out = all("""
            set [1] = 1
        """.trimIndent()
        )
        assert(out == "TODO: cannot assign to [1]\n") { out }
    }
}