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
                yield(nil) { as it => nil }
                println(:in)
            }
            val ts = tasks()
            spawn in ts, T()
            println(:out)
            broadcast(nil)
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
                yield(nil) { as it => nil }
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
                yield(nil) { as it => nil }
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
                yield(nil) { as it => nil }
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
            val T = task () { yield(nil) { as it => nil } }
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
            detrack(nil) { as it => nil }
        """)
        assert(out == " v  anon : (lin 2, col 13) : detrack error : expected track value\n") { out }
    }
    @Test
    fun cc_02_detrack() {
        val out = test("""
            val x
            detrack(nil) { as x => nil }
        """)
        assert(out == "anon : (lin 3, col 31) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun cc_03_detrack() {
        val out = test("""
            val T = task () { nil }
            val t = spawn T()
            val x = track(t)
            val v = detrack(t) { as it => 10 }
            println(v)
        """)
        assert(out.contains(" v  anon : (lin 4, col 21) : track(t) : track error : expected unterminated task\n")) { out }
    }
    @Test
    fun cc_04_detrack() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            val t = spawn T()
            val x = track(t)
            broadcast(nil)
            val v = detrack(x) { as it => 10 }
            println(v)
        """)
        assert(out.contains("nil\n")) { out }
    }
    @Test
    fun cc_05_detrack() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as it => 10 }
            println(v)
        """)
        assert(out.contains("10\n")) { out }
    }
    @Test
    fun cc_06_detrack_err() {
        val out = test("""
            detrack(nil) { as it => broadcast(nil) }
        """)
        assert(out.contains("anon : (lin 2, col 37) : broadcast error : unexpected enclosing detrack\n")) { out }
    }
    @Test
    fun cc_07_detrack_err() {
        val out = test("""
            task () {
                detrack(nil) { as it => yield(nil) { as it => nil } }
            }
        """)
        assert(out.contains("anon : (lin 3, col 57) : declaration error : variable \"it\" is already declared\n")) { out }
    }
    @Test
    fun cc_07_detrack_err2() {
        val out = test("""
            task () {
                detrack(nil) { as yy => yield(nil) { as xx => nil } }
            }
        """)
        assert(out.contains("anon : (lin 3, col 41) : yield error : unexpected enclosing detrack\n")) { out }
    }
    @Test
    fun cc_08_detrack() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as it => set it = 10 }
            println(v)
        """)
        assert(out.contains("anon : (lin 5, col 43) : invalid set : destination is immutable\n")) { out }
    }
    @Test
    fun cc_09_detrack() {
        val out = test("""
            val T = task (v) {
                yield(nil) { as it => nil }
            }
            val ts = tasks()
            spawn in ts, T()
            val x = next(ts)
            ;;dump(x)
            broadcast(nil)
            println(detrack(x) { as it => 99 })
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun cc_10_detrack() {
        val out = test("""
            val T = task (v) {
                ${AWAIT("it == v")}
            }
            val t = spawn T()
            val x = track(t)
            broadcast(nil)
            println(detrack(x) { as it => 99 })
            ;;dump(x)
        """
        )
        assert(out == "nil\n") { out }
    }

    // DETRACK / ACCESS

    @Test
    fun dd_01_detrack() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as it =>
                println(:1, x)
                println(:2, t)
                println(:3, it)
                println(:4, `:bool ${D}it.type == CEU_VALUE_REF`)
                println(:5, it == t)
            }
            println(:6, v)
        """)
        assert(out.contains(":1\ttrack: 0x")) { out }
        assert(out.contains(":2\texe-task: 0x")) { out }
        assert(out.contains(":3\texe-task: 0x")) { out }
        assert(out.contains(":4\ttrue\n")) { out }
        assert(out.contains(":5\ttrue\n")) { out }
        assert(out.contains(":6\tnil\n")) { out }
    }
    @Test
    fun dd_02_detrack() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as it =>
                println(status(it))
            }
        """)
        assert(out.contains(":yielded\n")) { out }
    }
    @Test
    fun todo_dd_03_detrack_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn in ts, T()
            val f = func () {
                broadcast(nil)
            }
            val x = next(ts)
            detrack(x) { as it =>
                f()                     ;; cannot broadcast
                println(status(it))
            }
        """)
        assert(out == ("anon : (lin 8, col 17) : broadcast error : unexpected enclosing func\n")) { out }
    }
    @Test
    fun dd_04_detrack_eq() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as kkk =>
                println(kkk == kkk)
                println(kkk == x)
            }
        """)
        assert(out == ("true\nfalse\n")) { out }
    }
    @Test
    fun dd_05_detrack_print() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as it =>
                println(it)
            }
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun dd_06_detrack_drop_err() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as it =>
                drop(it)
            }
            println(v)
        """)
        assert(out == " v  anon : (lin 6, col 22) : drop error : value is not movable\n") { out }
    }

    // PUB

    @Test
    fun ee_01_pub() {
        val out = test("""
            val T = task () {
                set pub = 10
                yield(nil) { as it => nil }
            }
            val t = spawn T()
            println(pub(t))
        """)
        assert(out.contains("10\n")) { out }
    }
    @Test
    fun ee_02_pub() {
        val out = test("""
            val T = task () {
                set pub = 10
                yield(nil) { as it => nil }
            }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as it =>
                println(pub(it))
            }
            println(v)
        """)
        assert(out.contains("track: 0x")) { out }
        assert(out.contains("ref-task: 0x")) { out }
        assert(out.contains("false\n")) { out }
    }


    // THROW

    @Test
    fun ee_01_throw() {
        val out = test("""
            val T = task () {
                spawn task () {
                    yield(nil) { as it => nil }
                    throw(:error)
                }()
                yield(nil) { as it => nil }
            }
            spawn in tasks(), T()
            broadcast(nil)
        """)
        assert(out == " |  anon : (lin 10, col 13) : broadcast(nil)\n" +
                " |  anon : (lin 5, col 21) : throw(:error)\n" +
                " v  throw error : :error\n") { out }
    }

    // SCOPE

    @Test
    fun ff_01_scope() {
        val out = test("""
            val T = task () { yield(nil) { as it => nil } }
            var x
            do {
                val t = spawn T()
                set x = track(t)
            }
        """)
        assert(out == " v  anon : (lin 6, col 21) : set error : cannot move track outside its task scope\n") { out }
    }
    @Test
    fun ff_02_detrack_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn in ts, T()
            val x = next(ts)
            var t
            detrack(x) { as it =>
                set t = it
            }
            broadcast(nil)
            println(status(t))
        """)
        assert(out == " v  anon : (lin 9, col 24) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun ff_03_detrack_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val t = spawn T()
            val x = track(t)
            println(detrack(x) { as it => it })
        """)
        assert(out == " v  anon : (lin 7, col 32) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun ff_04_detrack_ok() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val t = spawn T()
            val x = track(t)
            println(detrack(x) { as it =>
                val z = it
                10
            })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ff_05_track_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val x = do {
                val t = spawn T()
                track(t)
            }
        """)
        assert(out == " v  anon : (lin 5, col 21) : block escape error : cannot move track outside its task scope\n") { out }
    }
    @Test
    fun ff_06_track_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val x = do {
                val ts = tasks()
                spawn in ts, T()
                next(ts)
            }
        """)
        assert(out == " v  anon : (lin 5, col 21) : block escape error : cannot move track outside its task scope\n") { out }
    }
    @Test
    fun ff_07_track_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val ts = tasks()
            val x = do {
                spawn in ts, T()
                next(ts)
            }
            detrack(x) { as it =>
                println(it)
            }            
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }

    // NEXT

    @Test
    fun hh_01_next() {
        val out = test("""
            val T = task () {
                yield(nil) { as it => nil }
            }
            val ts = tasks()
            println(next(ts))
            println(next(ts, nil))
            println(next(ts, :err))
        """
        )
        assert(out == "nil\n" +
                "nil\n" +
                " v  anon : (lin 8, col 21) : next(ts,:err) : next error : expected task in pool track\n") { out }
    }
    @Test
    fun hh_02_next() {
        val out = test("""
            val T = task () {
                yield(nil) { as it => nil }
            }
            val ts = tasks()
            spawn in ts, T()
            spawn in ts, T()
            val x1 = next(ts)
            val x2 = next(ts, x1)
            val x3 = next(ts, x2)
            println(x1 /= nil)
            println(x2 /= nil)
            println(x1 /= x2)
            println(x3 == nil)
            println(x2)
        """
        )
        assert(out.contains("true\ntrue\ntrue\ntrue\ntrack: 0x")) { out }
    }
    @Test
    fun hh_03_next() {
        val out = test("""
            val T = task () {
                yield(nil) { as it => nil }
            }
            val ts = tasks()
            spawn in ts, T()
            val x = next(ts)
            broadcast(nil)
            next(ts, x)
        """
        )
        assert(out == " v  anon : (lin 9, col 13) : next(ts,x) : next error : expected task in pool track\n") { out }
    }
    @Test
    fun hh_04_next() {
        val out = test("""
            val T = task (v) {
                ${AWAIT("it == v")}
            }
            val ts = tasks()
            spawn in ts, T(1)
            spawn in ts, T(2)
            val x1 = next(ts)
            val x2 = next(ts, x1)
            broadcast(1)
            next(ts, x1)
        """
        )
        assert(out == " v  anon : (lin 11, col 13) : next(ts,x1) : next error : expected task in pool track\n") { out }
    }
}
