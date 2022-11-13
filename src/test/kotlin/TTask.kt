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
                set v = yield v+1 
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
        assert(out == "anon : (lin 2, col 8) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task4_err() {
        val out = all("""
            var co
            set co = spawn task () {}
            resume co()
            resume co()
        """.trimIndent())
        assert(out == "anon : (lin 4, col 8) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task5_err() {
        val out = all("""
            var co
            set co = spawn task () {
            }
            resume co()
            resume co(1,2)
        """)
        assert(out == "anon : (lin 6, col 20) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task6() {
        val out = all("""
            var co
            set co = spawn task (v) {
                set v = yield () 
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
            resume xxx() ;;(xxx(1))
        """)
        assert(out == "anon : (lin 3, col 20) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task9_mult() {
        val out = all("""
            var co
            set co = spawn task (x,y) {
                println(x,y)
            }
            resume co(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun task10_err() {
        val out = all("""
            var co
            set co = spawn task () {
                yield ()
            }
            resume co()
            resume co(1,2)
        """)
        assert(out.contains("bug found : not implemented : multiple arguments to resume")) { out }
    }

    // THROW

    @Test
    fun throw1() {
        val out = all("""
            var co
            set co = spawn task (x,y) {
                throw 2
            }
            catch 2 {
                resume co(1,2)
                println(99)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun throw2() {
        val out = all("""
            var co
            set co = spawn task (x,y) {
                yield ()
                throw 2
            }
            catch 2 {
                resume co(1,2)
                println(1)
                resume co()
                println(2)
            }
            println(3)
        """)
        assert(out == "1\n3\n") { out }
    }

    // BROADCAST

    @Test
    fun bcast1() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v)
                set v = yield ()
                println(v)                
            }
            var co1
            set co1 = spawn tk
            var co2
            set co2 = spawn tk
            broadcast 1
            broadcast 2
            broadcast 3
        """)
        assert(out == "1\n1\n2\n2\n") { out }
    }
    @Test
    fun bcast2() {
        val out = all("""
            resume spawn task () {
                resume spawn task () {
                    yield ()
                    println(2)
                } ()
                yield ()
                println(1)
            } ()
            broadcast ()
        """)
        assert(out == "1\n2\n") { out }
    }
}
