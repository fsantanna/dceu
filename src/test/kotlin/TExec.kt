import org.junit.Test
import java.io.File

class TExec {

    fun all(inp: String): String {
        val lexer = Lexer("anon", inp.reader())
        val parser = Parser(lexer)
        val es = try {
            parser.exprs()
        } catch (e: Throwable) {
            return e.message!!
        }
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
        assert(out == "[10.000000]") { out }
    }
    @Test
    fun print2() {
        val out = all("""
            print(10)
            println(20)
        """.trimIndent()
        )
        assert(out == "10.00000020.000000\n") { out }
    }
    @Test
    fun print3() {
        val out = all("""
            println([[],[1,2,3]])
        """.trimIndent()
        )
        assert(out == "[[],[1.000000,2.000000,3.000000]]\n") { out }
    }
    @Test
    fun print_err1() {
        val out = all("""
            println(1)
        """.trimIndent()
        )
        assert(out.contains("1")) { out }
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
            println([1,2,3][1])
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
            println(x)
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
            println(x)
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
            println(x)
        """.trimIndent()
        )
        assert(out == "[10.000000,22.000000,[33.000000]]\n") { out }
    }
    @Test
    fun set_err1() {
        val out = all("""
            set 1 = 1
        """.trimIndent()
        )
        assert(out == "anon: (ln 1, col 1): invalid set : invalid destination") { out }
    }
    @Test
    fun set_err2() {
        val out = all("""
            set [1] = 1
        """.trimIndent()
        )
        assert(out == "anon: (ln 1, col 1): invalid set : invalid destination") { out }
    }
    @Test
    fun set_index() {
        val out = all("""
            var i
            set i = 1
            println([1,2,3][i])
        """.trimIndent()
        )
        assert(out == "2.000000\n") { out }
    }

    // DO

    @Test
    fun do1() {  // set whole tuple?
        val out = all("""
            do {}
        """.trimIndent()
        )
        assert(out == "") { out }
    }
    @Test
    fun do2() {
        val out = all("""
            do {
                var a
                set a = 1
                println(a)
            }
        """.trimIndent()
        )
        assert(out == "1.000000\n") { out }
    }
    @Test
    fun do3() {
        val out = all("""
            var x
            set x = do {
                var a
                set a = 10
                a
            }
            print(x)
        """.trimIndent()
        )
        assert(out == "10.000000") { out }
    }

    // SCOPE

    @Test
    fun scope1() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1.000000,2.000000,3.000000]\n") { out }
    }
    @Test
    fun scope_err2() {
        val out = all("""
            var x
            do {
                var a
                set a = [1,2,3]
                set x = a
            }
        """.trimIndent()
        )
        assert(out == "ERROR") { out }
    }
    @Test
    fun scope3() {
        val out = all("""
            var x
            do {
                var a
                set a = [1,2,3] @x
                set x = a
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1.000000,2.000000,3.000000]") { out }
    }

}