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
            broadcast in :global, nil
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
        assert(out.contains("false\ttrue\n")) { out }
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
