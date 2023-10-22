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
            spawn T() in ts
            println(:out)
        """)
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun aa_04_tasks() {
        val out = test("""
            spawn (task () { println(:in) })() in tasks()
            println(:out)
        """)
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun aa_05_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
                println(:in)
            }
            val ts = tasks()
            spawn T() in ts
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
            val ok1 = spawn T() in ts 
            val ok2 = spawn T() in ts
            println(ok1, ok2)
        """)
        assert(out == "true\ttrue\n") { out }
    }
    @Test
    fun aa_07_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
            }
            val ts = tasks()
            val ok = spawn T() in ts
            println(ok)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun aa_08_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
            }
            val ts = tasks(1)
            val ok1 = spawn T() in ts
            val ok2 = spawn T() in ts
            println(ok1, ok2)
        """)
        assert(out == "true\tfalse\n") { out }
    }
    @Test
    fun todo_debug_gc_aa_09_gc() {
        val out = test("""
            val T = task () {
                set pub() = []
                yield(nil) thus { as it=>nil }
            }
            val ts = tasks(1)
            do {
                spawn T() in ts
                spawn T() in ts
                spawn T() in ts
                broadcast([])
                spawn T() in ts
                spawn T() in ts
                spawn T() in ts
                broadcast([])
                spawn T() in ts
                spawn T() in ts
                spawn T() in ts
                broadcast([])
                spawn T() in ts
                spawn T() in ts
                spawn T() in ts
                spawn T() in ts
                broadcast([])
                println(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }

    // TRACK

    @Test
    fun bb_01_track_err() {
        val out = test("""
            track(nil)
        """)
        assert(out == " v  anon : (lin 2, col 13) : track(nil) : track error : expected task\n") { out }
    }
    @Test
    fun bb_02_track_err() {
        val out = test("""
            val T = task () {
                nil
            }
            val t = spawn T()
            val x = track(t)
            println(t, x)
        """)
        assert(out == " v  anon : (lin 6, col 21) : track(t) : track error : expected unterminated task\n") { out }
        //assert(out == " v  anon : (lin 6, col 21) : track(t) : track error : expected task\n") { out }
    }
    @Test
    fun bb_03_track() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
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
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T ()
            val x = track(t)
            val y = track(t)
            var z = y
            println(x==y, y==z)
        """)
        assert(out == ("false\ttrue\n")) { out }
    }
    @Test
    fun bb_05_bcast_in_task() {
        val out = test("""
            val T = task (v) {
                ${AWAIT()}
                ;;yield(nil)
                println(v)
            }
            val t1 = spawn T (1)
            val x1 = track(t1)
            val t2 = spawn T (2)
            broadcast (nil) in x1
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_06_track_err() {
        val out = test("""
            var T
            set T = task () { yield(nil) thus {as it=>nil} }
            var x
            do {
                var t = spawn (T) ()
                set x = track(t)         ;; error scope
            }
            ;;println(status(detrack(x)))
            println(x)
        """)
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == (" v  anon : (lin 7, col 21) : set error : cannot move track outside its task scope\n")) { out }
    }

    // TRACK / DROP

    @Test
    fun bc_01_track_drop() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T ()
            val y = do {
                val x = track(t)
                drop(x)
            }
            println(y)
        """)
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun bc_02_track_drop_err() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val y = do {
                val t = spawn T ()
                val x = track(t)
                drop(x)
            }
            println(y)
        """)
        assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot move track outside its task scope\n")) { out }
    }
    @Test
    fun bc_03_track_drop() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val ts = tasks()
            val y = do {
                spawn T () in ts
                println()
                drop(next(ts))
            }
            println(y)
        """)
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun bc_04_track_drop() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val y = do {
                val ts = tasks()
                spawn T () in ts
                drop(next(ts))
            }
            println(y)
        """)
        assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot move track outside its task scope\n")) { out }
    }
    @Test
    fun bc_05_track_drop() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T ()
            val y = do {
                track(t)
            }
            println(y)
        """)
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun bc_06_track_drop() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T ()
            val y = do {
                drop(track(t))
            }
            println(y)
        """)
        assert(out.contains("track: 0x")) { out }
    }

    // DETRACK

    @Test
    fun cc_01_detrack() {
        val out = test("""
            detrack(nil)
        """)
        assert(out == " v  anon : (lin 2, col 13) : detrack error : expected track value\n") { out }
    }
    @Test
    fun cc_02_detrack() {
        val out = test("""
            val x
            detrack(nil) thus { as x => nil }
        """)
        assert(out == "anon : (lin 3, col 36) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun cc_03_detrack() {
        val out = test("""
            val T = task () { nil }
            val t = spawn T()
            val x = track(t)
            val v = detrack(t) thus { as it => 10 }
            println(v)
        """)
        assert(out == (" v  anon : (lin 4, col 21) : track(t) : track error : expected unterminated task\n")) { out }
        //assert(out == (" v  anon : (lin 4, col 21) : track(t) : track error : expected task\n")) { out }
    }
    @Test
    fun cc_04_detrack() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T()
            val x = track(t)
            broadcast(nil)
            val v = detrack(x) ;;thus { as it => 10 }
            println(v)
        """)
        assert(out == ("nil\n")) { out }
    }
    @Test
    fun cc_05_detrack() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) thus { as it => 10 }
            println(v)
        """)
        assert(out == ("10\n")) { out }
    }
    @Test
    fun cc_06_detrack_err() {
        val out = test("""
            detrack(nil) thus { as it => broadcast(nil) }
        """)
        //assert(out == ("anon : (lin 2, col 37) : broadcast error : unexpected enclosing detrack\n")) { out }
        assert(out == (" v  anon : (lin 2, col 13) : detrack error : expected track value\n")) { out }
    }
    @Test
    fun cc_07_detrack_err() {
        val out = test("""
            task () {
                detrack(nil) thus { as it => yield(nil) thus { as it => nil } }
            }
        """)
        assert(out == ("anon : (lin 3, col 67) : declaration error : variable \"it\" is already declared\n")) { out }
    }
    @Test
    fun cc_07_detrack_err2() {
        val out = test("""
            task () {
                detrack(nil) thus { as yy => yield(nil) thus { as xx => nil } }
            }
        """)
        assert(out == ("anon : (lin 3, col 46) : thus error : unexpected enclosing yield\n")) { out }
    }
    @Test
    fun cc_08_detrack() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) thus { as it => set it = 10 }
            println(v)
        """)
        assert(out == ("anon : (lin 5, col 48) : invalid set : destination is immutable\n")) { out }
    }
    @Test
    fun cc_09_detrack() {
        val out = test("""
            val T = task (v) {
                yield(nil) thus { as it => nil }
            }
            val ts = tasks()
            spawn T() in ts
            val x = next(ts)
            ;;dump(x)
            broadcast(nil)
            println(detrack(x) ;;;thus { as it => 99 };;;)
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
            println(detrack(x) ;;;thus { as it => 99 };;;)
            ;;dump(x)
        """
        )
        assert(out == "nil\n") { out }
    }

    // DETRACK / ACCESS

    @Test
    fun dd_01_detrack() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) thus { as it =>
                println(:1, x)
                println(:2, t)
                println(:3, it)
                println(:4, `:bool ${D}it.type == CEU_VALUE_EXE_TASK`)
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
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) thus { as it =>
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
            spawn T() in ts
            val f = func () {
                broadcast(nil)
            }
            val x = next(ts)
            detrack(x) thus { as it =>
                f()                     ;; cannot broadcast
                println(status(it))
            }
        """)
        assert(out == ("anon : (lin 8, col 17) : broadcast error : unexpected enclosing func\n")) { out }
    }
    @Test
    fun dd_04_detrack_eq() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) thus { as kkk =>
                println(kkk == kkk)
                println(kkk == x)
            }
        """)
        assert(out == ("true\nfalse\n")) { out }
    }
    @Test
    fun dd_05_detrack_print() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
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
            val T = task () { yield(nil) thus { as it => nil } }
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
                set pub() = 10
                yield(nil) thus { as it => nil }
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
                set pub() = 10
                yield(nil) thus { as it => nil }
            }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { as it =>
                pub(it)
            }
            println(v)
        """)
        assert(out == ("10\n")) { out }
    }
    @Test
    fun ee_03_pub() {
        val out = test("""
            val T = task () {
                set pub() = []
                yield(nil) thus { as it => nil }
            }
            val t = spawn T()
            println(pub(t))
        """)
        assert(out.contains("[]\n")) { out }
    }
    @Test
    fun ee_04_pub() {
        val out = test("""
            val T = task () {
                set pub() = [10]
                yield(nil) thus { as it => nil }
            }
            val ts = tasks()
            spawn T() in ts
            val t = next(ts)
            detrack(t) { as it =>
                println(pub(it))
            }
        """)
        assert(out.contains("[10]\n")) { out }
    }

    // THROW

    @Test
    fun ee_01_throw() {
        val out = test("""
            val T = task () {
                spawn( task () {
                    yield(nil) thus { as it => nil }
                    throw(:error)
                })()
                yield(nil) thus { as it => nil }
            }
            spawn T() in tasks()
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
            val T = task () { yield(nil) thus { as it => nil } }
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
            spawn T() in ts
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
                spawn T() in ts 
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
                spawn T() in ts
                next(ts)
            }
            detrack(x) { as it =>
                println(it)
            }            
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun ff_08_tasks() {
        val out = test("""
            do {
                val t = [tasks(), tasks()]
                println(#t)
            }
        """)
        assert(out == "2\n") { out }
    }

    // DETRACK / PUB / SCOPE

    @Test
    fun fg_01_detrack_pub() {
        val out = test("""
            val T = task () {
                set pub() = [10]
                yield(nil) thus { as it => nil }
            }
            var t = spawn T()
            var x = track(t)
            println(detrack(x) { as it => pub(it) })      ;; expose (ok, global func)
        """)
        //assert(out == "anon : (lin 12, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun fg_02_detrack_pub() {
        val out = test("""
            val T = task () {
                set pub() = [10]
                yield(nil) thus { as it => nil }
            }
            var t = spawn T()
            var x = track(t)
            broadcast(nil)
            println(detrack(x) { as it => pub(it) })      ;; expose (ok, global func)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun fg_03_detrack_pub() {
        val out = test("""
            val T = task () {
                set pub() = [10]
                yield(nil) thus { as it => nil }
            }
            var t = spawn T()
            var x = track(t)
            val v = detrack(x) { as it => pub(it) }
            broadcast(nil)
            println(v)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun fg_04_expose_err() {
        val out = test("""
            val x = do {
                val ts = tasks()
                var T = task () {
                    set pub() = []
                    yield(nil) thus { as it => nil }
                    nil
                }
                spawn (T) () in ts
                val trk = next(ts)
                val p = detrack(trk) { as it => pub(it) }
                println(:pub, p)
                p
            }
            println(x)
        """)
        assert(out == ":pub\t[]\n" +
                " v  anon : (lin 2, col 21) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun fg_05_expose() {
        val out = test("""
            var T = task (t) {
                set pub() = []
                if t {
                    val p = detrack(t) { as it => pub(it) }
                } else {
                    nil
                }
                yield(nil) thus { as it => nil }
                nil
            }
            val t = spawn T ()
            spawn T (track(t))
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 13, col 19) : T(track(t))\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun fg_06_expose() {
        val out = test("""
            val f = func (t) { false }
            val T = task () {
                set pub() = []
                yield(nil) thus { as it => nil }
            }
            val ts = tasks()
            do {
                do {
                    do {
                        do {
                            spawn T() in ts
                        }
                    }
                }
            }
            do {
                val xx1 = next(ts)
                f(detrack(xx1) { as it => pub(it) } )
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fg_07_throw_track() {
        val out = test("""
            val T = task () {
                set pub() = 10
                yield(nil) thus {as it => nil}
            }
            val ts = tasks()
            val t = catch { as it=>true} in {
                spawn T() in ts
                do {
                    val t = next(ts)
                    throw(drop(t))
                }
            }
            println(detrack(t) { as it => pub(it) })
        """)
        //assert(out == ":ok\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun fg_08_throw_track() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it=>nil }
            }
            val t = do {
                val ts = tasks()
                spawn T() in ts
                do {
                    val t = next(ts)
                    throw(drop(t))
                    nil
                }
            }
            println(t)
        """)
        //assert(out == ":ok\n") { out }
        assert(out.contains(" v  anon : (lin 5, col 21) : block escape error : cannot move track outside its task scope\n")) { out }
    }
    @Test
    fun fg_09_throw_track() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
            }
            val ts = tasks()
            catch {as it=>true} in {
                spawn T() in ts
                do {
                    val t = next(ts)
                    throw(drop(t))
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // NEXT

    @Test
    fun hh_01_next() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
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
                yield(nil) thus { as it => nil }
            }
            val ts = tasks()
            spawn T() in ts
            spawn T() in ts
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
                yield(nil) thus { as it => nil }
            }
            val ts = tasks()
            spawn T() in ts
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
            spawn T(1) in ts
            spawn T(2) in ts
            val x1 = next(ts)
            val x2 = next(ts, x1)
            broadcast(1)
            next(ts, x1)
        """
        )
        assert(out == " v  anon : (lin 11, col 13) : next(ts,x1) : next error : expected task in pool track\n") { out }
    }

    // ABORTION

    @Test
    fun ii_01_self() {
        val out = test("""
            spawn( task () {
                val t = spawn (task () {
                    yield(nil) thus { as it => nil }
                } )()
                yield (nil) thus { as it => nil }
            }) ()
            broadcast(nil)
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_02_self() {
        val out = test("""
            spawn (task () {
                val t = spawn( task () {
                    yield(nil) thus { as it => nil }
                }) () in tasks()
                yield (nil) thus { as it => nil }
            } )()
            broadcast(nil)
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }

    // ORIGINAL

    @Test
    fun oo_01_tracks() {
        val out = test("""
            val T = task () { yield(nil) thus { as it => nil } }
            do {
                val ts = tasks()
                spawn T() in ts
                do {
                    val vec = #[]
                    var t = nil
                    loop {
                        set t = next(ts,t)
                        break if t==nil
                        set vec[#vec] = t
                    }
                    println(vec)
                }
            }
        """)
        //assert(out == "anon : (lin 9, col 29) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out.contains("#[track: 0x")) { out }
    }
    @Test
    fun oo_02_track_err() {
        val out = test("""
            var T = task (v) {
                yield(nil) thus { as it => nil }
            }
            var x
            var ts = tasks()
            spawn T(1) in ts
            do {
                val t = next(ts)
                set x = track(t)
            }
        """)
        assert(out == " v  anon : (lin 10, col 25) : track(t) : track error : expected task\n") { out }
    }
    @Test
    fun oo_03_track_err() {
        val out = test("""
            var T
            set T = task (v) {
                set pub() = [v]
                yield(nil) thus { as it => nil }
            }
            var x
            do {
                var ts
                set ts = tasks()
                spawn T(1) in ts
                do {
                    val t = next(ts)
                    set x = t       ;; err: escope 
                }
            }
        """)
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n:error\n") { out }
        assert(out == " v  anon : (lin 14, col 25) : set error : cannot move track outside its task scope\n") { out }
    }
    @Test
    fun oo_04_track() {
        val out = test("""
            var T = task (v) {
                yield(nil) thus { as it => nil }
            }
            var ts = tasks()
            spawn T(1) in ts
            do {
                val t = next(ts)
                detrack(t) { as x =>
                    println(x)
                }
            }
        """)
        //assert(out == "anon : (lin 8, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out.contains("exe-task: 0x")) { out }
    }

    // ORIGINAL / TRACK / DETRACK

    @Test
    fun op_01_track() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
                set pub() = 10
                yield(nil) thus { as it => nil }
            }
            var t = spawn T()
            var x = track(t)
            println(detrack(x) { as it => pub(it) }) 
            broadcast(nil)
            println(detrack(x) { as it => pub(it) }) 
        """)
        assert(out == "nil\n10\n") { out }
    }
    @Test
    fun op_02_track() {
        val out = test("""
            var T
            set T = task () {
                set pub() = [10]
                yield(nil) thus { as it => nil }
            }
            var t = spawn T ()
            var x = track(t)
            println(detrack(x) { as it => pub(it)[0] })
            broadcast( nil )
            println(detrack(x) { as it => it })
        """)
        assert(out == "10\nnil\n") { out }
    }
    @Test
    fun op_03_track_err() {
        val out = test("""
            var T
            set T = task () {
                set pub() = [10]
                yield(nil) thus { as it => nil }
            }
            var x
            do {
                var t = spawn (T) ()
                set x = track(t)         ;; scope x < t
                ;;println(detrack(x).pub[0])
            }
            ;;println(status(detrack(x)))
            ;;println(x)
        """)
        //assert(out.contains("10\n:terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 10, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == (" v  anon : (lin 10, col 21) : set error : cannot move track outside its task scope\n")) { out }
    }
    @Test
    fun op_04_track() {
        val out = test("""
            var T = task () {
                set pub() = [10]
                ${AWAIT("it == :evt")}
            }
            var t = spawn T()
            var x = track(t)
            spawn( task () {
                catch { as err=>err==:par-or } in {
                    spawn( task () {
                        yield(nil) thus { as it => it==t }
                        throw(:par-or)
                    }) ()
                    println(detrack(x) { as it => pub(it)[0] })
                    broadcast(nil) in t
                    println(detrack(x) { as it => pub(it)[0] })
                    broadcast(:evt) in t
                    println(999)
                }
                println(detrack(x) { as it => 999 })
            })()
            println(:ok)
        """)
        assert(out == "10\n10\nnil\n:ok\n") { out }
    }
    @Test
    fun op_05_detrack_err() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
            }
            val t1 = spawn T()
            val r1 = track(t1)
            val x1 = detrack(r1) { as it => it }
            ;;println(t1, r1, x1, status(t1))
            broadcast( nil )
            ;;println(t1, r1, x1, status(t1))
            println(status(t1))
        """)
        //assert(out == ":terminated\n") { out }
        //assert(out == "anon : (lin 7, col 13) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == " v  anon : (lin 7, col 34) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun op_06_track_scope() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
            }
            val t = spawn T()
            val y = do {
                val x = track(t)
                x
            }
            println(y)
        """)
        assert(out == " v  anon : (lin 6, col 21) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun op_07_track_scope() {
        val out = test("""
            val T = task () {
                set pub() = 1
                yield(nil) thus { as it => nil }
            }
            val t = spawn T()
            val y = do {
                track(t)
            }
            println(detrack(y) { as it => pub(it) })
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun op_08_track_scope() {
        val out = test("""
            val T = task () {
                set pub() = 1
                yield(nil) thus { as it => nil }
            }
            val y = do {
                val t = spawn T()
                track(t)
            }
            println(detrack(y) { as it => pub(it) })
        """)
        assert(out == " v  anon : (lin 6, col 21) : block escape error : cannot move track outside its task scope\n") { out }
    }

    // ZZ / ALL

    @Test
    fun zz_01_all() {
        val out = test("""
            val T = task () {
                yield(nil) thus { as it => nil }
            }
            spawn (task () {
                val ts = tasks(5)
                do {
                    spawn T() in ts
                }
                yield(nil) thus { as it =>
                    println(nil)
                }
            }) ()
            broadcast(nil)
       """)
        assert(out == "nil\n") { out }
    }
}
