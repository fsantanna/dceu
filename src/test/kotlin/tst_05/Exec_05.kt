package tst_05

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_05 {
    // TASKS

    @Test
    fun aa_01_tasks() {
        val out = test("""
            println(tasks())
        """)
        assert(out.contains("tasks: 0x")) { out }
    }
    @Test
    fun aa_02_tasks() {
        val out = test("""
            println(type(tasks()))
        """)
        assert(out.contains(":tasks")) { out }
    }
    @Test
    fun aa_03_tasks() {
        val out = test("""
            val T = task () {
                println(:in)
            }
            val ts = tasks()
            spawn in ts, T()
            println(:out)
        """)
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun aa_04_tasks() {
        val out = test("""
            spawn in tasks(), (task () { println(:in) })()
            println(:out)
        """)
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun aa_05_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) { nil }
                println(:in)
            }
            val ts = tasks()
            spawn in ts, T()
            println(:out)
            broadcast nil
        """)
        assert(out == ":out\n:in\n") { out }
    }
    @Test
    fun aa_06_tasks() {
        val out = test("""
            val T = task () {
                nil
            }
            val ts = tasks(1)
            val ok1 = spawn in ts, T()
            val ok2 = spawn in ts, T()
            println(ok1, ok2)
        """)
        assert(out == "true\ttrue\n") { out }
    }
    @Test
    fun aa_07_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) { nil }
            }
            val ts = tasks()
            val ok = spawn in ts, T()
            println(ok)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun aa_08_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) { nil }
            }
            val ts = tasks(1)
            val ok1 = spawn in ts, T()
            val ok2 = spawn in ts, T()
            println(ok1, ok2)
        """)
        assert(out == "true\tfalse\n") { out }
    }

    // TRACK

    @Test
    fun bb_01_track() {
        val out = test("""
            track(nil)
        """)
        assert(out == " v  anon : (lin 2, col 13) : track(nil) : track error : expected task\n") { out }
    }
    @Test
    fun bb_02_track() {
        val out = test("""
            val T = task () {
                nil
            }
            val t = spawn T()
            val x = track(t)
            println(t, x)
        """)
        assert(out == " v  anon : (lin 6, col 21) : track(t) : track error : expected unterminated task\n") { out }
    }
    @Test
    fun bb_03_track() {
        val out = test("""
            val T = task () {
                yield(nil) { nil }
            }
            val t = spawn T()
            val x = track(t)
            println(x)
        """)
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun bb_04_track() {
        val out = test("""
            val T = task () { yield(nil) { nil } }
            val t = spawn T ()
            val x = track(t)
            val y = track(t)
            var z = y
            println(x==y, y==z)
        """)
        assert(out == ("false\ttrue\n")) { out }
    }

    // DETRACK

    @Test
    fun cc_01_detrack() {
        val out = test("""
            detrack(nil) { nil }
        """)
        assert(out == " v  anon : (lin 2, col 13) : detrack error : expected track value\n") { out }
    }
    @Test
    fun cc_03_detrack() {
        val out = test("""
            val T = task () { nil }
            val t = spawn T()
            val v = detrack(t) { 10 }
            println(v)
        """)
        assert(out.contains("nil\n")) { out }
    }
    @Test
    fun cc_04_detrack() {
        val out = test("""
            val T = task () { yield(nil) { nil } }
            val t = spawn T()
            val v = detrack(t) { 10 }
            println(v)
        """)
        assert(out.contains("10\n")) { out }
    }
    @Test
    fun cc_05_detrack_err() {
        val out = test("""
            detrack(nil) { broadcast nil }
        """)
        assert(out.contains("anon : (lin 2, col 28) : broadcast error : unexpected enclosing detrack\n")) { out }
    }
    @Test
    fun cc_06_detrack_err() {
        val out = test("""
            task () {
                detrack(nil) { yield(nil) { nil } }
            }
        """)
        assert(out.contains("anon : (lin 3, col 32) : yield error : unexpected enclosing detrack\n")) { out }
    }

    // THROW

    @Test
    fun dd_01_throw() {
        val out = test("""
            val T = task () {
                spawn task () {
                    yield(nil) { nil }
                    throw(:error)
                }()
                yield(nil) { nil }
            }
            spawn in tasks(), T()
            broadcast nil
        """)
        assert(out == " |  anon : (lin 10, col 13) : broadcast nil\n" +
                " |  anon : (lin 5, col 21) : throw(:error)\n" +
                " v  throw error : :error\n") { out }
    }

    // NEXT

    @Ignore
    @Test
    fun bb_01_next() {
        val out = test("""
            val T = task () {
                yield(nil) { nil }
            }
            val ts = tasks()
            spawn in ts, T()
            spawn in ts, T()
            
            
            println(ok1, ok2)

            val t = @[]
            set t[:x] = 1
            set t[:y] = 2
            var k
            set k = next(t)
            println(k, t[k])
            set k = next(t,k)
            println(k, t[k])
            set k = next(t,k)
            println(k, t[k])
        """
        )
        assert(out == ":x\t1\n:y\t2\nnil\tnil\n") { out }
    }
}
