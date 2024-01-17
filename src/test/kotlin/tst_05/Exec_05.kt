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
                yield(nil) ;;thus { it => nil }
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
                yield(nil) ;;thus { it => nil }
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
                yield(nil) ;;thus { it => nil }
            }
            val ts = tasks(1)
            val ok1 = spawn T() in ts
            val ok2 = spawn T() in ts
            println(ok1, ok2)
        """)
        assert(out == "true\tfalse\n") { out }
    }
    @Test
    fun aa_09_gc() {
        val out = test("""
            val T = task () {
                set pub() = []
                yield(nil) ;;thus { it=>nil }
                nil
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
    @Test
    fun aa_10_tasks() {
        val out = test("""
            println(tasks() == nil)
        """)
        assert(out == "false\n") { out }
    }
    @Test
    fun aa_11_spawn() {
        val out = test("""
            $PLUS
            val T = task (v) { nil }
            val ts = tasks()
            var x = 0
            loop {
                spawn T() in ts
                set x = x + 1
                break if x==500
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // GC

    @Test
    fun ab_01_spawn() {
        DEBUG = true
        val out = test("""
            val ts = tasks()
            println(`:number CEU_GC.gc`)
            spawn (task () { nil }) () in ts
            println(`:number CEU_GC.gc`)
        """)
        assert(out == "0\n1\n") { out }
    }

    // TASKS / PROTO / SCOPE

    @Test
    fun ab_01_tasks_proto() {
        val out = test("""
            val ts = tasks()
            do {
                spawn(task () {
                    nil
                }) () in ts
            }
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ab_02_tasks_proto_err() {
        val out = test("""
            val ts = tasks()
            do {
                val T = task () {
                    nil
                }
                spawn T() in ts
            }
            println(:ok)
       """)
        //assert(out == " v  anon : (lin 7, col 17) : spawn error : cannot copy reference out\n") { out }
        assert(out == " v  anon : (lin 7, col 17) : spawn error : task pool outlives task prototype\n") { out }
    }
    @Test
    fun ab_03_tasks_proto() {
        val out = test("""
            do {
                val ts = tasks()
                val T = task () {
                    nil
                }
                spawn T() in ts
            }
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ab_04_tasks_proto() {
        val out = test("""
            val T = task () {
                nil
            }
            do {
                val ts = tasks()
                spawn T() in ts
            }
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ab_05_tasks_proto() {
        val out = test("""
            val ts = tasks()
            spawn (task () {
                spawn (task () {    ;; anon task is dropped to ts
                    nil
                }) () in ts
                nil
            })()
            println(:ok)
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
                yield(nil) ;;thus { it => nil }
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
            val T = task () { yield(nil);nil }
            val t = spawn T ()
            val x = track(t)
            val y = track(t)
            var z = y
            println(x==y, y==z)
        """)
        assert(out == ("false\ttrue\n")) { out }
    }
    @Test
    fun bb_05_bcast_in_task_err() {
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
        assert(out == " v  anon : (lin 10, col 13) : broadcast'(nil,x1) : invalid target\n") { out }
    }
    @Test
    fun bb_05_bcast_in_task_ok() {
        val out = test("""
            val T = task (v) {
                ${AWAIT()}
                ;;yield(nil)
                println(v)
            }
            val t1 = spawn T (1)
            val x1 = track(t1)
            val t2 = spawn T (2)
            detrack(x1) { it => broadcast (nil) in it }
        """)
        assert(out == "1\n") { out }
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
            detrack(x1) { y1 =>
                broadcast (nil) in y1
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_06_track_up() {
        DEBUG = true
        val out = test("""
            $DETRACK
            val T = task () { yield(nil);yield(nil) }
            spawn (task () {
                val ts = tasks()
                spawn (task () {
                    spawn (task () {
                        spawn T() in ts
                    }) ()
                    nil
                }) ()
                do {
                    val t = next-tasks(ts)
                    do {
                        ;;dump(t)
                        broadcast(nil) in :global
                        ;;dump(t)                    
                    }
                    println(detrack(t))
                }
            }) ()
            println(:ok)
        """)
        assert(out == ("false\n:ok\n")) { out }
    }

    // TRACK / SCOPE / ERROR

    @Test
    fun bd_01_track_err() {
        val out = test("""
            var T
            set T = task () { yield(nil) }
            var x
            do {
                val t = spawn (T) ()
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
    @Test
    fun bd_02_track_err() {
        val out = test("""
            var T
            set T = task () { yield(nil) }
            val x = do {
                val t = spawn (T) ()
                track(t)         ;; error scope
            }
            ;;println(status(detrack(x)))
            println(x)
        """)
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == (" v  anon : (lin 4, col 21) : block escape error : cannot move track outside its task scope\n")) { out }
    }
    @Test
    fun bd_03_track_err() {
        val out = test("""
            $DETRACK
            var T
            set T = task () { yield(nil) }
            val t1 = spawn T()
            do {
                val t2 = spawn T()
                set pub(t1) = track(t2)         ;; error scope
                nil
            }
            println(detrack(pub(t1)))
        """)
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == (" v  anon : (lin 5, col 13) : block escape error : reference has immutable scope\n")) { out }
        //assert(out == (" v  anon : (lin 5, col 13) : block escape error : cannot move track outside its task scope\n")) { out }
    }
    @Test
    fun bd_04_track_err() {
        val out = test("""
            var T
            set T = task () { yield(nil) }
            var x
            do {
                val t = spawn (T) ()
                val x' = track(t)
                x'         ;; error scope
            }
            ;;println(status(detrack(x)))
            println(x)
        """)
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == (" v  anon : (lin 5, col 13) : block escape error : cannot move track outside its task scope\n")) { out }
    }

    // TRACK / DROP

    @Test
    fun bc_01_track_drop() {
        val out = test("""
            val T = task () { yield(nil) }
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
            val T = task () { yield(nil) }
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
            val T = task () { yield(nil) }
            val ts = tasks()
            val y = do {
                spawn T () in ts
                println()
                drop(next-tasks(ts))
            }
            println(y)
        """)
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun bc_04_track_drop() {
        val out = test("""
            val T = task () { yield(nil) }
            val y = do {
                val ts = tasks()
                spawn T () in ts
                drop(next-tasks(ts))
            }
            println(y)
        """)
        assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot move track outside its task scope\n")) { out }
    }
    @Test
    fun bc_05_track_drop() {
        val out = test("""
            val T = task () { yield(nil) }
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
            val T = task () { yield(nil) }
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
    fun cc_00_detrack() {
        val out = test("""
            $DETRACK
            detrack(nil)
        """)
        assert(out == " |  anon : (lin 3, col 13) : detrack''(nil)\n" +
                " v  anon : (lin 2, col 58) : detrack'(trk) : detrack error : expected track value\n") { out }
    }
    @Test
    fun cc_01_detrack() {
        val out = test("""
            $DETRACK
            detrack(nil) { it => nil }
        """)
        assert(out == " v  anon : (lin 3, col 13) : detrack'(nil) : detrack error : expected track value\n") { out }
    }
    @Test
    fun cc_02_detrack() {
        val out = test("""
            val x
            detrack(nil) { x => nil }
        """)
        assert(out == "anon : (lin 3, col 28) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun cc_03_detrack() {
        val out = test("""
            val T = task () { nil }
            val t = spawn T()
            val x = track(t)
            val v = detrack(t) { it => 10 }
            println(v)
        """)
        assert(out == (" v  anon : (lin 4, col 21) : track(t) : track error : expected unterminated task\n")) { out }
        //assert(out == (" v  anon : (lin 4, col 21) : track(t) : track error : expected task\n")) { out }
    }
    @Test
    fun cc_04_detrack() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = track(t)
            broadcast(nil)
            val v = detrack(x) { it => 10 }
            println(v)
        """)
        assert(out == ("nil\n")) { out }
    }
    @Test
    fun cc_05_detrack() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { it => 10 }
            println(v)
        """)
        assert(out == ("10\n")) { out }
    }
    @Test
    fun cc_06_detrack_err() {
        val out = test("""
            detrack(nil) { it => broadcast(nil) }
        """)
        //assert(out == ("anon : (lin 2, col 37) : broadcast error : unexpected enclosing detrack\n")) { out }
        assert(out == (" v  anon : (lin 2, col 13) : detrack'(nil) : detrack error : expected track value\n")) { out }
    }
    @Test
    fun cc_07_detrack_err() {
        val out = test("""
            task () {
                detrack(nil) { it => func(it) { nil } (yield(nil)) }
            }
        """)
        assert(out == ("anon : (lin 3, col 43) : declaration error : variable \"it\" is already declared\n")) { out }
    }
    @Test
    fun cc_07_detrack_err2() {
        val out = test("""
            task () {
                detrack(nil) { yy => yield(nil) ; nil }
            }
            println(:ok)
        """)
        assert(out == ("anon : (lin 3, col 38) : yield error : unexpected enclosing func\n")) { out }
        //assert(out == (":ok\n")) { out }
    }
    @Test
    fun cc_08_detrack() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { it => set it = 10 }
            println(v)
        """)
        assert(out == ("anon : (lin 5, col 40) : set error : destination is immutable\n")) { out }
    }
    @Test
    fun cc_09_detrack() {
        val out = test("""
            $DETRACK
            val T = task (v) {
                yield(nil) ; nil
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            ;;dump(x)
            broadcast(nil)
            println(detrack(x))
        """
        )
        assert(out == "false\n") { out }
    }
    @Test
    fun cc_10_detrack() {
        val out = test("""
            $DETRACK
            val T = task (v) {
                ${AWAIT("it == v")}
            }
            val t = spawn T()
            val x = track(t)
            broadcast(nil)
            println(detrack(x))
            ;;dump(x)
        """
        )
        assert(out == "false\n") { out }
    }

    // DETRACK / ACCESS

    @Test
    fun dd_01_detrack() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { it =>
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
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { it=>it }
            println(status(v))
        """)
        assert(out.contains(":yielded\n")) { out }
    }
    @Test
    fun BUG_dd_03a_detrack_err() {
        val out = test("""
            val T = task () {
                yield(nil)
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            detrack(x) { it =>
                println(it)
                broadcast(nil)          ;; aborts it
                println(status(it))     ;; dangling
            }
            println(:ok)
        """)
        assert(out.contains("exe-task: 0x")) { out }
        assert(!out.contains(":xxx")) { out }
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun TODO_dd_03b_detrack_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn T() in ts
            val f = func () {
                broadcast(nil)
            }
            val x = next-tasks(ts)
            detrack(x) thus { it =>
                println(it)
                println(status(it))
                println(:xxx)
            }
        """)
        assert(out.contains("exe-task: 0x")) { out }
        assert(!out.contains(":xxx")) { out }
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun dd_04_detrack_eq() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = track(t)
            detrack(x) { v =>
                println(v == v)
                println(v == x)
            }
        """)
        assert(out == ("true\nfalse\n")) { out }
    }
    @Test
    fun dd_05_detrack_print() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = track(t)
            detrack(x) { v =>
                println(v)
            }
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun dd_06_detrack_drop_err() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = track(t)
            val v = detrack(x) { it =>
                drop(it)
            }
            println(v)
        """)
        //assert(out.contains("exe-task: 0x")) { out }
        //assert(out == " v  anon : (lin 6, col 22) : drop error : value is not movable\n") { out }
        assert(out == " |  anon : (lin 5, col 32) : (func (it) { if it { ```                     ...)\n" +
                " v  anon : (lin 6, col 22) : drop error : value contains multiple references\n") { out }
    }
    @Test
    fun dd_07_detrack_nested() {
        val out = test("""
            spawn (task () {
                val T = task () { yield(nil) ; nil }
                val t = spawn T()
                val x = track(t)
                detrack(x) { it => println(it) }
            }) ()
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun dd_08_detrack_nested() {
        val out = test("""
            spawn (task () {
                val z = 10
                val T = task () { yield(nil) ; nil }
                val t = spawn T()
                val x = track(t)
                detrack(x) { it => println(z, it) }
            }) ()
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun dd_09_detrack_nested() {
        val out = test("""
            spawn (task () {
                val z = 10
                val T = task () { yield(nil) ; nil }
                val t = spawn T()
                val x = track(t)
                detrack(x) { it1 =>
                    detrack(x) { it2 =>
                        println(z, it1, it2)
                    }
                }
            }) ()
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }

    // PUB

    @Test
    fun ee_01_pub() {
        val out = test("""
            val T = task () {
                set pub() = 10
                yield(nil) ;;thus { it => nil }
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
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            val x = track(t)
            detrack(x) { v =>
                val y = pub(v)
                println(y)
            }
        """)
        assert(out == ("10\n")) { out }
    }
    @Test
    fun ee_03_pub() {
        val out = test("""
            val T = task () {
                set pub() = []
                yield(nil) ;;thus { it => nil }
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
                yield(nil) ;;thus { it => nil }
            }
            val ts = tasks()
            spawn T() in ts
            val t = next-tasks(ts)
            detrack(t) { it => println(pub(it)) }
        """)
        assert(out.contains("[10]\n")) { out }
    }

    // THROW

    @Test
    fun ee_01_throw() {
        val out = test("""
            val T = task () {
                defer {
                    println(:ok)
                }
                spawn( task () {
                    yield(nil) ;;thus { it => nil }
                    throw(:error)
                })()
                yield(nil) ;;thus { it => nil }
            }
            spawn T() in tasks()
            broadcast(nil)
        """)
        assert(out == ":ok\n" +
                " |  anon : (lin 13, col 13) : broadcast'(nil,:task)\n" +
                " |  anon : (lin 8, col 21) : throw(:error)\n" +
                " v  throw error : :error\n") { out }
    }

    // SCOPE

    @Test
    fun ff_01_scope() {
        val out = test("""
            val T = task () { yield(nil) }
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
            val x = next-tasks(ts)
            val t = detrack(x) { it=>it }
            broadcast(nil)
            println(status(t))
        """)
        //assert(out == " v  anon : (lin 9, col 24) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 10, col 21) : status(t) : status error : expected running coroutine or task\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration error : cannot expose task-in-pool reference\n") { out }
        assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
                " v  anon : (lin 8, col 32) : block escape error : cannot expose reference to task in pool\n") { out }
    }
    @Test
    fun ff_02x_detrack_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            var y
            val t = detrack(x) { it =>
                set y = it
            }
            broadcast(nil)
            println(status(t))
        """)
        //assert(out == " v  anon : (lin 9, col 24) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 10, col 21) : status(t) : status error : expected running coroutine or task\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration error : cannot expose task-in-pool reference\n") { out }
        assert(out == " |  anon : (lin 9, col 32) : (func (it) { if it { ```                     ...)\n" +
                " v  anon : (lin 10, col 21) : set error : cannot expose reference to task in pool\n") { out }
    }
    @Test
    fun ff_03_detrack_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val t = spawn T()
            val x = track(t)
            println(detrack(x) {it=>it})
        """)
        //assert(out == " v  anon : (lin 7, col 32) : block escape error : cannot copy reference out\n") { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun ff_04_detrack_ok() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val t = spawn T()
            val x = track(t)
            println(detrack(x) { it =>
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
                next-tasks(ts)
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
                next-tasks(ts)
            }
            detrack(x) { it => println(it) }
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
                yield(nil) ; nil
            }
            var t = spawn T()
            var x = track(t)
            println(detrack(x) { it => pub(it) })
        """)
        //assert(out == "anon : (lin 12, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape error : reference has immutable scope\n") { out }
    }
    @Test
    fun fg_02_detrack_pub() {
        val out = test("""
            val T = task () {
                set pub() = [10]
                yield(nil) ; nil
            }
            var t = spawn T()
            var x = track(t)
            broadcast(nil)
            println(detrack(x) { it => pub(it) }) ;; expose (ok, global func)
        """)
        assert(out == "nil\n") { out }
        //assert(out == " v  anon : (lin 9, col 49) : pub error : expected task\n") { out }
    }
    @Test
    fun fg_03_detrack_pub() {
        val out = test("""
            val T = task () {
                set pub() = [10]
                yield(nil) ; nil
            }
            var t = spawn T()
            var x = track(t)
            val v = detrack(x) { it => pub(it) }
            broadcast(nil)
            println(v)
        """)
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape error : reference has immutable scope\n") { out }
    }
    @Test
    fun fg_04_expose_err() {
        val out = test("""
            val x = do {
                val ts = tasks()
                var T = task () {
                    set pub() = []
                    yield(nil) ; nil
                    nil
                }
                spawn (T) () in ts
                val trk = next-tasks(ts)
                val p = detrack(trk) { it => it }
                p
            }
            println(x)
        """)
        //assert(out == ":pub\t[]\n" +
        //        " v  anon : (lin 2, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 11, col 17) : declaration error : cannot expose task-in-pool reference\n") { out }
        assert(out == " |  anon : (lin 11, col 38) : (func (it) { if it { ```                     ...)\n" +
                " v  anon : (lin 11, col 38) : block escape error : cannot expose reference to task in pool\n") { out }
    }
    @Test
    fun fg_05_expose() {
        val out = test("""
            var T = task (t) {
                set pub() = []
                if t {
                    val p = detrack(t) { it => pub(it) }
                } else {
                    nil
                }
                yield(nil) ; nil
                nil
            }
            val t = spawn T ()
            spawn T (track(t))
            println(:ok)
        """)
        //assert(out == ":ok\n") { out }
        //assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
        //        " |  anon : (lin 5, col 40) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 5, col 40) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 13, col 19) : T(track(t))\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
        //        " |  anon : (lin 5, col 40) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 5, col 40) : block escape error : reference has immutable scope\n") { out }
        assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
                " v  anon : (lin 5, col 21) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun fg_06_expose() {
        val out = test("""
            val f = func (t) {
                println(t)
            }
            val T = task () {
                set pub() = []
                yield(nil) ; nil
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
                val xx1 = next-tasks(ts)
                detrack(xx1) { it => f(pub(it)) }
            }
            println(:ok)
        """)
        assert(out == "[]\n:ok\n") { out }
    }
    @Test
    fun fg_06_expose_xxx() {
        val out = test("""
            val f = func (t) {
                println(t)
            }
            val T = task () {
                set pub() = []
                yield(nil) ; nil
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            detrack(x) { t => f(pub(t)) }
            println(:ok)
        """)
        assert(out == "[]\n:ok\n") { out }
    }
    @Test
    fun todo_fg_07_throw_track() {
        val out = test("""
            val T = task () {
                set pub() = 10
                yield(nil) ; nil
            }
            val ts = tasks()
            val t = catch ( it=>true) {
                spawn T() in ts
                do {
                    val t = next-tasks(ts)
                    throw(drop(t))
                }
            }
            detrack(t) { it => println(pub(it)) }
        """)
        //assert(out == ":ok\n") { out }
        //assert(out == "10\n") { out }
        assert(out.contains("TODO: error inside throw")) { out }
    }
    @Test
    fun todo_fg_08_throw_track() {
        val out = test("""
            val T = task () {
                yield(nil) ; nil
            }
            val x = do {
                val ts = tasks()
                spawn T() in ts
                do {
                    val t = next-tasks(ts)
                    throw(drop(t))
                    nil
                }
            }
            println(x)
        """)
        //assert(out == ":ok\n") { out }
        assert(out.contains("TODO: error inside throw")) { out }
    }
    @Test
    fun todo_fg_09_throw_track() {
        val out = test("""
            val T = task () {
                yield(nil) ; nil
            }
            val ts = tasks()
            catch ( it=>true) {
                spawn T() in ts
                do {
                    val t = next-tasks(ts)
                    throw(drop(t))
                }
            }
            //println(:ok)
            assert(out.contains("TODO: error inside throw")) { out }
        """)
        assert(out == ":ok\n") { out }
    }

    // NEXT

    @Test
    fun hh_01_next() {
        val out = test("""
            val T = task () {
                yield(nil) ;;thus { it => nil }
            }
            val ts = tasks()
            println(next-tasks(ts))
            println(next-tasks(ts, nil))
            println(next-tasks(ts, :err))
        """
        )
        assert(out == "nil\n" +
                "nil\n" +
                " v  anon : (lin 8, col 21) : next-tasks(ts,:err) : next-tasks error : expected task-in-pool track\n") { out }
    }
    @Test
    fun hh_02_next() {
        val out = test("""
            val T = task () {
                yield(nil) ;;thus { it => nil }
            }
            val ts = tasks()
            spawn T() in ts
            spawn T() in ts
            val x1 = next-tasks(ts)
            val x2 = next-tasks(ts, x1)
            val x3 = next-tasks(ts, x2)
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
                yield(nil) ;;thus { it => nil }
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            broadcast(nil)
            println(next-tasks(ts, x))
        """
        )
        //assert(out == " v  anon : (lin 9, col 13) : next-tasks(ts,x) : next-tasks error : expected task-in-pool track\n") { out }
        assert(out == "nil\n") { out }
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
            val x1 = next-tasks(ts)
            val x2 = next-tasks(ts, x1)
            broadcast(1)
            println(next-tasks(ts, x1))
        """
        )
        //assert(out == " v  anon : (lin 11, col 13) : next-tasks(ts,x1) : next-tasks error : expected task-in-pool track\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun hh_05_next() {
        val out = test("""
            val T = task (v) {
                set pub() = [v]
                yield(nil)
            }
            val ts = tasks()
            spawn T(10) in ts
            val x1 = next-tasks(ts)
            val v = detrack(x1) { it => println(pub(it)) ; pub(it) }
            println(v)
        """
        )
        assert(out == "[10]\n[10]\n") { out }
        //assert(out == "[10]\n" +
        //        " |  anon : (lin 9, col 33) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 9, col 33) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[10]\n" +
        //        " |  anon : (lin 9, col 33) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 9, col 33) : block escape error : reference has immutable scope\n") { out }
    }

    // ABORTION

    @Test
    fun ii_01_self() {
        val out = test("""
            spawn( task () {
                val t = spawn (task () {
                    yield(nil) ; nil ;;thus { it => nil }
                } )()
                yield (nil) ; nil ;;thus { it => nil }
            }) ()
            broadcast(nil)
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_02a_self() {
        val out = test("""
            spawn (task () {
                val t = spawn( task () {
                    yield(nil) ; nil ;;thus { it => nil }
                }) () in tasks()
                yield (nil) ; nil ;;thus { it => nil }
            } )()
            broadcast(nil)
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_02b_self() {
        val out = test("""
            spawn (task () {
                val ts = tasks()
                val t = spawn( task () {
                    yield(nil) ; nil ;;thus { it => nil }
                }) () in ts
                yield (nil) ; nil ;;thus { it => nil }
            } )()
            broadcast(nil)
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_02c_self() {
        val out = test("""
            val ts = tasks()
            spawn (task () {
                val t = spawn( task () {
                    yield(nil) ; nil ;;thus { it => nil }
                }) () in ts
                yield (nil) ; nil ;;thus { it => nil }
            } )()
            broadcast(nil)
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_03_self() {
        val out = test("""
            val T = task () {
                spawn (task () {
                    yield(nil)
                }) ()
                yield(nil)
                nil
            }
            val ts = tasks()
            spawn T() in ts
            broadcast(nil)
            println(:ok)
       """)
        assert(out == ":ok\n") { out }
    }

    // TRACK / COLLECTION

    @Test
    fun TODO_jj_01_tracks() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val ts = tasks()
            spawn T() in ts
            val vec = #[]
            val t = next-tasks(ts,nil)
            set vec[#vec] = t
            println(vec)
        """)
        assert(out.contains("#[track: 0x")) { out }
    }
    @Test
    fun TODO_jj_02_tracks() {
        val out = test("""
            $DETRACK
            val f = func (trk) {
                println(detrack(trk) { it => status(it) })
            }
            val T = task () { yield(nil) ; nil }
            val x' = do {
                val ts = tasks()
                spawn T() in ts
                val x = [next-tasks(ts,nil)]
                ;;dump(x)
                f(x[0])
                x
            }
            f(x'[0])
        """)
        assert(out.contains(":yielded\n" +
                " v  anon : (lin 7, col 22) : block escape error : reference has immutable scope\n")) { out }
    }
    @Test
    fun TODO_jj_03_tracks() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            do {
                val ts = tasks()
                spawn T() in ts
                do {
                    val vec = #[]
                    var t = nil
                    loop {
                        set t = next-tasks(ts,t)
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
        //assert(out == (" v  anon : (lin 12, col 29) : store error : cannot hold reference to track or task in pool\n")) { out }
    }

    // ORIGINAL

    @Test
    fun oo_02_track_err() {
        val out = test("""
            var T = task (v) {
                yield(nil) ; nil
            }
            var x
            var ts = tasks()
            spawn T(1) in ts
            do {
                val t = next-tasks(ts)
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
                yield(nil) ; nil
            }
            var x
            do {
                var ts
                set ts = tasks()
                spawn T(1) in ts
                do {
                    val t = next-tasks(ts)
                    set x = t       ;; err: escope 
                }
            }
        """)
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n:error\n") { out }
        assert(out == " v  anon : (lin 14, col 25) : set error : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun oo_04_track() {
        val out = test("""
            var T = task (v) {
                yield(nil) ; nil
            }
            var ts = tasks()
            spawn T(1) in ts
            val x = do {
                val t = next-tasks(ts)
                detrack(t) { it => it }
            }
            println(x)
        """)
        //assert(out == " v  anon : (lin 7, col 13) : declaration error : cannot expose task-in-pool reference\n") { out }
        //assert(out.contains("exe-task: 0x")) { out }
        assert(out == (" |  anon : (lin 9, col 28) : (func (it) { if it { ```                     ...)\n" +
                " v  anon : (lin 9, col 28) : block escape error : cannot expose reference to task in pool\n")) { out }
    }

    // ORIGINAL / TRACK / DETRACK

    @Test
    fun op_00_track() {
        val out = test("""
            $DETRACK
            var T
            set T = task () {
                set pub() = [10]
                yield(nil) ; nil
            }
            var t = spawn T ()
            var x = track(t)
            println(detrack(x))
            broadcast( nil )
            println(detrack(x))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun op_01_track() {
        val out = test("""
            val T = task () {
                yield(nil) ; nil
                set pub() = 10
                yield(nil) ; nil
            }
            var t = spawn T()
            var x = track(t)
            detrack(x) { it => println(pub(it)) } 
            broadcast(nil)
            detrack(x) { it => println(pub(it)) } 
        """)
        assert(out == "nil\n10\n") { out }
    }
    @Test
    fun op_02_track() {
        val out = test("""
            $DETRACK
            var T
            set T = task () {
                set pub() = [10]
                yield(nil) ; nil
            }
            var t = spawn T ()
            var x = track(t)
            detrack(x) { it => println(pub(it)[0]) }
            println(detrack(x))
            broadcast( nil )
            println(detrack(x) { it => 999 })
        """)
        assert(out == "10\ntrue\nnil\n") { out }
    }
    @Test
    fun op_02x_track() {
        val out = test("""
            $DETRACK
            val T = task () {
                yield(nil)
            }
            var t = spawn T ()
            val x = track(t)
            println(detrack''(x))
            detrack(x) { it => nil }
            println(detrack''(x))
        """)
        assert(out == "true\ntrue\n") { out }
    }
    @Test
    fun op_03_track_err() {
        val out = test("""
            var T
            set T = task () {
                set pub() = [10]
                yield(nil) ; nil
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
                catch ( err=>err==:par-or ) {
                    spawn( task () {
                        yield(nil) ;;thus { it => it==t }
                        throw(:par-or)
                    }) ()
                    println(detrack(x) { it => pub(it)[0] })
                    broadcast(nil) in t
                    println(detrack(x) { it => pub(it)[0] })
                    broadcast(:evt) in t
                    println(999)
                }
                println(detrack(x) { it => if it {999} else {nil} })
            })()
            println(:ok)
        """)
        assert(out == "10\n10\nnil\n:ok\n") { out }
    }
    @Test
    fun op_05_detrack_err() {
        val out = test("""
            val T = task () {
                yield(nil) ; nil
            }
            val t1 = spawn T()
            val r1 = track(t1)
            detrack(r1) { x1 => 
                ;;println(t1, r1, x1, status(t1))
                println(status(t1))
                broadcast( nil )
                ;;println(t1, r1, x1, status(t1))
                println(status(t1)) ;; never reached
            }
            println(:ok)
        """)
        assert(out == ":yielded\n:ok\n") { out }
        //assert(out == "anon : (lin 7, col 13) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " v  anon : (lin 7, col 34) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun op_06_track_scope() {
        val out = test("""
            val T = task () {
                yield(nil) ; nil
            }
            val t = spawn T()
            val y = do {
                val x = track(t)
                x
            }
            println(y)
        """)
        //assert(out == " v  anon : (lin 6, col 21) : block escape error : cannot copy reference out\n") { out }
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun op_07_track_scope() {
        val out = test("""
            val T = task () {
                set pub() = 1
                yield(nil) ; nil
            }
            val t = spawn T()
            val y = do {
                track(t)
            }
            detrack(y) { it => println(pub(it)) }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun op_08_track_scope() {
        val out = test("""
            val T = task () {
                set pub() = 1
                yield(nil) ; nil
            }
            val y = do {
                val t = spawn T()
                track(t)
            }
            detrack(y) { it => println(pub(it)) }
        """)
        assert(out == " v  anon : (lin 6, col 21) : block escape error : cannot move track outside its task scope\n") { out }
    }
    @Test
    fun BUG_op_08_track_throw() {
        val out = test("""
            val T = task () {
                defer {
                    println(:ok)
                }
                spawn( task () {
                    yield(nil) ; nil
                    throw(:error)
                })()
                yield(nil) ; nil
            }
            val ts = tasks()
            spawn T() in ts
            val t = next-tasks(ts)
            catch (it=>true) {
                broadcast(nil)
            }
            ;;`ceu_gc_collect();`
            detrack(t) { it => println(it) }
        """)
        assert(out == " |  anon : (lin 10, col 13) : broadcast'(nil,:task)\n" +
                " |  anon : (lin 5, col 21) : throw(:error)\n" +
                " v  throw error : :error\n") { out }
    }

    // ZZ / ALL

    @Test
    fun zz_01_all() {
        val out = test("""
            val T = task () {
                yield(nil) ;;thus { it => nil }
            }
            spawn (task () {
                val ts = tasks(5)
                do {
                    spawn T() in ts
                }
                yield(nil) ;;thus { it =>
                    println(nil)
                ;;}
            }) ()
            broadcast(nil)
       """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun TODO_zz_02_all() {
        val out = test("""
            val iter-tasks = func (itr) {
                set itr[2] = next-tasks(itr[1],itr[2])
                itr[2]
            }
            val T = task () {
                yield(nil)
            }
            val ts = tasks()
            spawn T(nil) in ts
            val x = do {
                val tt = [nil]
                loop {
                    set tt[0] = next-tasks(ts,tt[0])
                    val t = tt[0]
                    break(false) if {{==}}(t,nil)
                    break(drop(t)) if true
                }
            }
            println(x)
       """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun TODO_zz_03_all() {
        val out = test("""
            val x = do {
                val tt = [nil]
                loop {
                    set tt[0] = @[]
                    val t = tt[0]
                    break(false) if {{==}}(t,nil)
                    break(drop(t)) if true
                }
            }
            println(x)
       """)
        assert(out == "nil\n") { out }
    }
}
