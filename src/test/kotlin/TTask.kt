import org.junit.Test
import java.io.File

class TTask {

    // TASK / SPAWN / RESUME / YIELD

    @Test
    fun task1() {
        val out = all("""
            var t
            set t = task (v) {
                println(v)          ;; 1
                set v = yield (v+1) 
                println(v)          ;; 3
                set v = yield (v+1) 
                println(v)          ;; 5
                v+1
            }
            var a
            set a = spawn t
            var v
            set v = resume a(1)
            println(v)              ;; 2
            set v = resume a(v+1)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """, true)
        assert(out == "1\n2\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun task2_err() {
        val out = all("""
            spawn func () {}
        """.trimIndent())
        assert(out == "anon : (lin 1, col 7) : spawn error : expected task\n") { out }
    }
    @Test
    fun task3_err() {
        val out = all("""
            var f
            resume f()
        """.trimIndent())
        assert(out == "anon : (lin 2, col 8) : resume error : expected spawned task\n") { out }
    }
    @Test
    fun task4_err() {
        val out = all("""
            var co
            set co = spawn task () {}
            resume co()
            resume co()
        """.trimIndent())
        assert(out == "anon : (lin 4, col 8) : resume error : expected spawned task\n") { out }
    }
    @Test
    fun task5_err() {
        val out = all("""
            var co
            set co = spawn task () {}
            resume co(1,2)
        """)
        assert(out == "bug found : not implemented : multiple arguments to resume") { out }
    }
    @Test
    fun task6() {
        val out = all("""
            var co
            set co = spawn task (v) {
                set v = yield (nil) 
                println(v)
            }
            resume co(1)
            resume co(2)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun task7() {
        val out = all("""
            var co
            set co = spawn task (v) {
                println(v)
            }
            println(1)
            resume co(99)
            println(2)
        """)
        assert(out == "1\n99\n2\n") { out }
    }
    @Test
    fun tak8_err() {
        val out = all("""
            var xxx
            resume xxx(xxx(1))
        """)
        assert(out == "anon : (lin 3, col 20) : resume error : expected spawned task\n") { out }
    }

    // MISC

}
