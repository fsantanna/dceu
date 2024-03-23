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
    fun aa_03x_tasks() {
        val out = test("""
            tasks()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun aa_03y_tasks() {
        val out = test("""
            do {
                tasks()
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
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
                yield(nil)
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
        assert(out.contains(Regex("exe-task: 0x.*nil\n"))) { out }
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
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun aa_08_tasks() {
        val out = test("""
            val T = task () {
                yield(nil)
            }
            val ts = tasks(1)
            val t1 = spawn T() in ts
            val t2 = spawn T() in ts
            println(t1, t2)
        """)
        assert(out.contains(Regex("exe-task: 0x.*nil\n"))) { out }
    }
    @Test
    fun aa_09_gc() {
        val out = test("""
            val T = task () {
                set pub = []
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
        //assert(out.contains("ceu_gc_inc_dyn: Assertion `dyn->Any.refs < 255'")) { out }
    }
    @Test
    fun aa_12_pool1() {
        val out = test("""
            var ts
            set ts = tasks()
            println(type(ts))
            var T
            set T = task (v) {
                println(v)
                val evt = yield(nil)
                println(evt)
            }
            do {
                spawn T(1) in ts
            }
             broadcast(2)
        """)
        assert(out == ":tasks\n1\n2\n") { out }
    }
    @Test
    fun aa_13_pool_leak() {
        val out = test("""
            var T
            set T = task () {
                do [1,2,3]
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_14_pool_defer() {
        val out = test("""
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            println(0)
        """)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun aa_15_pool_scope() {
        val out = test("""
            do {
                var ts
                set ts = tasks()
                var T
                set T = task (v) {
                    println(v)
                    val v' = yield(nil)
                    println(v')
                }
                spawn T(1) in ts
            }
             broadcast(2)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_16_pool_leak() {
        val out = test("""
            var T
            set T = task () {
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn T() in ts
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_17_pool_term() {
        val out = test(
            """
            var ts
            set ts = tasks(2)
            var T
            set T = task (v) {
                println(10)
                defer {
                    println(20)
                    println(30)
                }
                do {
                    var ok1
                    set ok1=false
                    loop {
                        break if ok1
                        val evt = yield(nil) if type(evt)/=:exe-task { set ok1=true } else { nil } }
                    }
                ;;yield(nil)
                if v {
                    do { var ok; set ok=false; loop { break if ok ; val evt=yield(nil;) if type(evt)/=:exe-task { set ok=true } else { nil } } }
                    ;;yield(nil)
                } else {
                    nil
                }
            }
            println(0)
            spawn T(false) in ts 
            spawn T(true) in ts
            println(1)
            broadcast(@[])
            println(2)
            broadcast(@[])
            println(3)
        """
        )
        assert(out == "0\n10\n10\n1\n20\n30\n2\n20\n30\n3\n") { out }
    }
    @Test
    fun aa_18_pool_throw() {
        val out = test(
            """
            var ts
            set ts = tasks(2)
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                catch (err,err==:ok) {
                    spawn task () {
                        yield(nil)
                        if v == 1 {
                            error(:ok)
                        } else {
                            nil
                        }
                        loop { yield(nil) }
                    } ()
                    loop { yield(nil) }
                }
                println(v)
            }
            spawn T(1) in ts
            spawn T(2) in ts
            broadcast(nil)
            broadcast(nil)
            println(999)
        """
        )
        assert(out == "1\n1\n999\n2\n") { out }
    }
    @Test
    fun aa_19_pool_throw() {
        val out = test(
            """
            var ts
            set ts = tasks(2)
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                catch (err,err==:ok) {
                    spawn task () {
                        yield(nil)
                        if v == 2 {
                            error(:ok)
                        } else {
                            nil
                        }
                        loop { yield(nil) }
                    } ()
                    loop { yield(nil) }
                }
                println(v)
            }
            spawn T(1) in ts
            spawn T(2) in ts
            broadcast(nil)
            broadcast(nil)
            println(999)
        """
        )
        assert(out == "2\n2\n999\n1\n") { out }
    }
    @Test
    fun aa_20_pub_tasks_tup() {
        val out = test("""
            val tup = []
            val T = task () {
                set ;;;task.;;;pub = tup
                yield(nil)
            }
            val ts = tasks()
            spawn T() in ts
            spawn T() in ts
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun aa_21_pub_pool_err() {
        val out = test("""
            var T = task () {
                set ;;;task.;;;pub = [10]
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, t {
                var x = detrack(t).pub
                broadcast in detrack(t), nil
                println(x)
            }
            println(999)
        """)
        //assert(out == "20\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        assert(out == "anon : (lin 9, col 17) : declaration error : incompatible scopes\n:error\n") { out }
    }

    // TASKS / lims

    @Test
    fun ab_01_pool_max_err() {
        val out = test(
            """
            tasks(0)
        """
        )
        assert(out == " |  anon : (lin 2, col 13) : tasks(0)\n" +
                " v  tasks error : expected positive number\n") { out }
    }
    @Test
    fun ab_02_pool_max_err() {
        val out = test(
            """
            tasks(nil)
        """
        )
        assert(out == " |  anon : (lin 2, col 13) : tasks(nil)\n" +
                " v  tasks error : expected positive number\n") { out }
    }
    @Test
    fun BUG_ab_03_pool_max() {  // remove from ts when terminates?
        val out = test(
            """
            var ts = tasks(1)
            var T = task () { yield(nil) }
            var ok1 = spawn T() in ts
            var ok2 = spawn T() in ts
            broadcast(nil)
            var ok3 = spawn T() in ts
            var ok4 = spawn T() in ts
            println(ok1, ok2, ok3, ok4)
        """
        )
        assert(out == "true\tfalse\ttrue\tfalse\n") { out }
    }
    @Test
    fun BUG_ab_04_pool_valgrind() {  // remove from ts when terminates?
        val out = test(
            """
            var ts
            set ts = tasks(1)
            var T
            set T = task () { yield(nil) }
            var ok1
            set ok1 = spawn T() in ts
            broadcast(nil)
            var ok2
            set ok2 = spawn T() in ts
            println(status(ok1), ok2)
        """
        )
        assert(out == ":terminated\tTODO\n") { out }
    }
    @Test
    fun BUG_ab_05_pool_reuse_awake() {
        val out = test(
            """
            var T = task (n) {
                set pub = n
                var evt = yield(nil)
                ;;println(:awake, evt, n)
                loop {
                    break if evt == n
                    set evt = yield(nil)
                }
                ;;println(:term, n)
            }
            var ts = tasks(2)
            spawn T(1) in ts
            spawn T(2) in ts
            var t = nil
            loop {
                set t = next-tasks(ts, t)
                break if (if t { false } else { true })
                println(:t, ;;;detrack;;;(t).pub)
                ;;println(:bcast1)
                broadcast( 2 )        ;; opens hole for 99 below
                ;;println(:bcast2)
                var ok = spawn T(99) in ts     ;; must not fill hole b/c ts in the stack
                println(ok)
            }
            ;;;
            println("-=-=-=-")
            loop in :tasks ts, x {
                println(:t, detrack(x).pub)
            }
            ;;;
        """
        )
        assert(out == "1\nfalse\n") { out }
    }

    // GC

    @Test
    fun ab_01_spawn() {
        DEBUG = true
        val out = test("""
            val ts = tasks()
            println(`:number CEU_GC.free`)
            spawn (task () { nil }) () in ts
            println(`:number CEU_GC.free`)
        """)
        assert(out == "0\n2\n") { out }
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
        //assert(out == " v  anon : (lin 7, col 17) : spawn error : task pool outlives task prototype\n") { out }
        assert(out == ":ok\n") { out }
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
        //assert(out == " v  anon : (lin 2, col 13) : track(nil) : track error : expected task\n") { out }
        assert(out == "anon : (lin 2, col 13) : access error : variable \"track\" is not declared\n") { out }
    }
    @Test
    fun bb_02_track_err() {
        val out = test("""
            val T = task () {
                nil
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            println(t, x)
        """)
        assert(out.contains(Regex("exe-task: 0x.*exe-task: 0x"))) { out }
        //assert(out == " v  anon : (lin 6, col 21) : track(t) : track error : expected unterminated task\n") { out }
        //assert(out == " v  anon : (lin 6, col 21) : track(t) : track error : expected task\n") { out }
    }
    @Test
    fun bb_03_track() {
        val out = test("""
            val T = task () {
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            println(x)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bb_04_track() {
        val out = test("""
            val T = task () { yield(nil);nil }
            val t = spawn T ()
            val x = ;;;track;;;(t)
            val y = ;;;track;;;(t)
            var z = y
            println(x==y, y==z)
        """)
        //assert(out == ("false\ttrue\n")) { out }
        assert(out == ("true\ttrue\n")) { out }
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
            val x1 = ;;;track;;;(t1)
            val t2 = spawn T (2)
            broadcast (nil) in x1
        """)
        //assert(out == " v  anon : (lin 10, col 13) : broadcast'(nil,x1) : invalid target\n") { out }
        assert(out == "1\n") { out }
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
            val x1 = ;;;track;;;(t1)
            val t2 = spawn T (2)
            ;;detrack(x1) { it => broadcast (nil) in it }
            broadcast (nil) in x1
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
            val x1 = ;;;track;;;(t1)
            val t2 = spawn T (2)
            ;;detrack(x1) { y1 =>
                broadcast (nil) in x1 ;;y1
            ;;}
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_06_track_up() {
        DEBUG = true
        val out = test("""
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
                    println(;;;detrack;;;status(t))
                }
            }) ()
            println(:ok)
        """)
        assert(out == (":terminated\n:ok\n")) { out }
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
                set x = ;;;track;;;(t)         ;; error scope
            }
            println(status(;;;detrack;;;(x)))
            println(x)
        """)
        assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 7, col 21) : set error : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun bd_02_track_err() {
        val out = test("""
            var T
            set T = task () { yield(nil) }
            val x = do {
                val t = spawn (T) ()
                ;;;track;;;(t)         ;; error scope
            }
            println(status(;;;detrack;;;(x)))
            println(x)
        """)
        assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 4, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun bd_03_track_err() {
        val out = test("""
            var T
            set T = task () { yield(nil) }
            val t1 = spawn T()
            do {
                val t2 = spawn T()
                set t1.pub = ;;;track;;;(t2)         ;; error scope
                nil
            }
            println(;;;detrack;;;(t1.pub))
        """)
        assert(out.contains("exe-task: 0x")) { out }
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 5, col 13) : block escape error : reference has immutable scope\n")) { out }
        //assert(out == (" v  anon : (lin 5, col 13) : block escape error : cannot expose track outside its task scope\n")) { out }
        //assert(out == (" v  anon : (lin 8, col 21) : set error : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun bd_04_track_err() {
        val out = test("""
            var T
            set T = task () { yield(nil) }
            var x =
            do {
                val t = spawn (T) ()
                val x' = ;;;track;;;(t)
                x'         ;; error scope
            }
            println(status(;;;detrack;;;(x)))
            println(x)
        """)
        assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 5, col 13) : block escape error : cannot expose track outside its task scope\n")) { out }
    }

    // TRACK / DROP

    @Test
    fun bc_01_track_drop() {
        val out = test("""
            val T = task () { yield(nil) }
            val t = spawn T ()
            val y = do {
                val x = ;;;track;;;(t)
                ;;;drop;;;(x)
            }
            println(y)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_02_track_drop_err() {
        val out = test("""
            val T = task () { yield(nil) }
            val y = do {
                val t = spawn T ()
                ;;;track;;;(t)
            }
            println(y)
        """)
        //assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_02x_track_drop_err() {
        val out = test("""
            val T = task () { yield(nil) }
            val y = do {
                val t = spawn T ()
                val x = ;;;track;;;(t)
                ;;;drop;;;(x)
            }
            println(y)
        """)
        //assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_03_track_drop() {
        val out = test("""
            val T = task () { yield(nil) }
            val ts = tasks()
            val y = do {
                spawn T () in ts
                println()
                ;;;drop;;;(next-tasks(ts))
            }
            println(y)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_04_track_drop() {
        val out = test("""
            val T = task () { yield(nil) }
            val y = do {
                val ts = tasks()
                spawn T () in ts
                ;;;drop;;;(next-tasks(ts))
            }
            println(y)
        """)
        //assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_05_track_drop() {
        val out = test("""
            val T = task () { yield(nil) }
            val t = spawn T ()
            val y = do {
                ;;;track;;;(t)
            }
            println(y)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_06_track_drop() {
        val out = test("""
            val T = task () { yield(nil) }
            val t = spawn T ()
            val y = do {
                ;;;drop;;;(;;;track;;;(t))
            }
            println(y)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }

    // DETRACK

    /*
    @Test
    fun cc_00_detrack() {
        val out = test("""
            detrack(nil)
        """)
        //assert(out == " |  anon : (lin 3, col 13) : detrack''(nil)\n" +
        //        " v  anon : (lin 2, col 58) : detrack'(trk) : detrack error : expected track value\n") { out }
        assert(out == "anon : (lin 3, col 13) : access error : variable \"detrack\" is not declared\n") { out }
    }
    @Test
    fun cc_01_detrack() {
        val out = test("""
            detrack(nil) { it => nil }
        """)
        //assert(out == " v  anon : (lin 3, col 13) : detrack'(nil) : detrack error : expected track value\n") { out }
        assert(out == "anon : (lin 3, col 26) : expected expression : have \"{\"\n") { out }
    }
    @Test
    fun cc_02_detrack() {
        val out = test("""
            val x
            detrack(nil) { x => nil }
        """)
        //assert(out == "anon : (lin 3, col 28) : declaration error : variable \"x\" is already declared\n") { out }
        assert(out == "anon : (lin 3, col 26) : expected expression : have \"{\"\n") { out }
    }
     */
    @Test
    fun cc_03_detrack() {
        val out = test("""
            val T = task () { nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = 10 ;;detrack(t) { it => 10 }
            println(v)
        """)
        //assert(out == (" v  anon : (lin 4, col 21) : track(t) : track error : expected unterminated task\n")) { out }
        //assert(out == (" v  anon : (lin 4, col 21) : track(t) : track error : expected task\n")) { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_04_detrack() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            broadcast(nil)
            val v = 10 ;;detrack(x) { it => 10 }
            println(v)
        """)
        //assert(out == ("nil\n")) { out }
        assert(out == ("10\n")) { out }
    }
    @Test
    fun cc_05_detrack() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = 10 ;;detrack(x) { it => 10 }
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
        //assert(out == (" v  anon : (lin 2, col 13) : detrack'(nil) : detrack error : expected track value\n")) { out }
        assert(out == ("anon : (lin 2, col 26) : expected expression : have \"{\"\n")) { out }
    }
    @Test
    fun cc_07_detrack_err() {
        val out = test("""
            task () {
                detrack(nil) { it => func(it) { nil } (yield(nil)) }
            }
        """)
        //assert(out == ("anon : (lin 3, col 43) : declaration error : variable \"it\" is already declared\n")) { out }
        assert(out == ("anon : (lin 3, col 30) : expected expression : have \"{\"\n")) { out }
    }
    @Test
    fun cc_07_detrack_err2() {
        val out = test("""
            task () {
                detrack(nil) { yy => yield(nil) ; nil }
            }
            println(:ok)
        """)
        //assert(out == ("anon : (lin 3, col 38) : yield error : unexpected enclosing func\n")) { out }
        //assert(out == (":ok\n")) { out }
        assert(out == ("anon : (lin 3, col 30) : expected expression : have \"{\"\n")) { out }
    }
    @Test
    fun cc_08_detrack() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = 10 ;;detrack(x) { it => set it = 10 }
            println(v)
        """)
        //assert(out == ("anon : (lin 5, col 40) : set error : destination is immutable\n")) { out }
        assert(out == ("10\n")) { out }
    }
    @Test
    fun cc_09_detrack() {
        val out = test("""
            val T = task (v) {
                yield(nil) ; nil
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            ;;dump(x)
            broadcast(nil)
            println(;;;detrack;;;(x))
        """
        )
        //assert(out == "false\n") { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun cc_10_detrack() {
        val out = test("""
            val T = task (v) {
                ${AWAIT("it == v")}
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            broadcast(nil)
            println(;;;detrack;;;(x))
            ;;dump(x)
        """
        )
        //assert(out == "false\n") { out }
        assert(out.contains("exe-task: 0x")) { out }
    }

    // DETRACK / ACCESS

    @Test
    fun dd_01_detrack() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = do { ;;detrack(x) { it =>
                val it = x
                println(:1, x)
                println(:2, t)
                println(:3, it)
                println(:4, `:bool ${D}it.type == CEU_VALUE_EXE_TASK`)
                println(:5, it == t)
            }
            println(:6, v)
        """)
        //assert(out.contains(":1\ttrack: 0x")) { out }
        assert(out.contains(":1\texe-task: 0x")) { out }
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
            val x = ;;;track;;;(t)
            val v = ;;;detrack;;;(x) ;;{ it=>it }
            println(status(v))
        """)
        assert(out.contains(":yielded\n")) { out }
    }
    @Test
    fun dd_03a_detrack_err() {
        val out = test("""
            val T = task () {
                yield(nil)
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            ;;detrack(x) { it =>
                val it = x
                println(it)
                broadcast(nil)              ;; aborts it
                println(:xxx, status(it))   ;; dangling
            ;;}
            println(:ok)
        """)
        assert(out.contains("exe-task: 0x")) { out }
        //assert(!out.contains(":xxx")) { out }
        assert(out.contains(":xxx\t:terminated\n")) { out }
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun dd_03b_detrack_err() {
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
            ;;detrack(x) { it =>
                val it = x
                println(it)
                f()
                println(:xxx, status(it))
            ;;}
            println(:ok)
        """)
        assert(out.contains("exe-task: 0x")) { out }
        //assert(!out.contains(":xxx")) { out }
        assert(out.contains(":xxx\t:terminated\n")) { out }
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun dd_04_detrack_eq() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            ;;detrack(x) { v =>
                val v = x
                println(v == v)
                println(v == x)
            ;;}
        """)
        //assert(out == ("true\nfalse\n")) { out }
        assert(out == ("true\ntrue\n")) { out }
    }
    @Test
    fun dd_05_detrack_print() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            ;;detrack(x) { v =>
                val v = x
                println(v)
            ;;}
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun dd_06_detrack_drop_err() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = do { ;;detrack(x) { it =>
                val it = x
                ;;;drop;;;(it)
            }
            println(v)
        """)
        assert(out.contains("exe-task: 0x")) { out }
        //assert(out == " v  anon : (lin 6, col 22) : drop error : value is not movable\n") { out }
        //assert(out == " |  anon : (lin 5, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 6, col 22) : drop error : value contains multiple references\n") { out }
    }
    @Test
    fun dd_07_detrack_nested() {
        val out = test("""
            spawn (task () {
                val T = task () { yield(nil) ; nil }
                val t = spawn T()
                val x = ;;;track;;;(t)
                ;;detrack(x) { it => println(it) }
                println(x)
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
                val x = ;;;track;;;(t)
                ;;detrack(x) { it => println(z, it) }
                println(z, x)
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
                val x = ;;;track;;;(t)
                ;;detrack(x) { it1 =>
                    ;;detrack(x) { it2 =>
                        ;;println(z, it1, it2)
                        println(z, x, x)
                    ;;}
                ;;}
            }) ()
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }

    // PUB

    @Test
    fun ee_01_pub() {
        val out = test("""
            val T = task () {
                set pub = 10
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            println(t.pub)
        """)
        assert(out.contains("10\n")) { out }
    }
    @Test
    fun ee_02_pub() {
        val out = test("""
            val T = task () {
                set pub = 10
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            ;;detrack(x) { v =>
                val v = x
                val y = v.pub
                println(y)
            ;;}
        """)
        assert(out == ("10\n")) { out }
    }
    @Test
    fun ee_03_pub() {
        val out = test("""
            val T = task () {
                set pub = []
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            println(t.pub)
        """)
        assert(out.contains("[]\n")) { out }
    }
    @Test
    fun ee_04_pub() {
        val out = test("""
            val T = task () {
                set pub = [10]
                yield(nil) ;;thus { it => nil }
            }
            val ts = tasks()
            spawn T() in ts
            val t = next-tasks(ts)
            ;;detrack(t) { it => println(it.pub) }
                println(t.pub)
        """)
        assert(out.contains("[10]\n")) { out }
    }
    @Test
    fun BUG_ee_05_data_pool_pub() {
        val out = test("""
            data :T = [x,y]
            var ts = tasks()
            spawn (task () {
                set ;;;task.;;;pub = [10,20]
                yield(nil)
            }) () in ts
            var xxx :T = nil
            loop {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                println(;;;detrack;;;(xxx).pub.y)   // TODO: detrack needs to return to grammar
            }
        """, true)
        assert(out == "20\n") { out }
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
                    error(:error)
                })()
                yield(nil) ;;thus { it => nil }
            }
            spawn T() in tasks()
            broadcast(nil)
        """)
        assert(out == ":ok\n" +
                " |  anon : (lin 13, col 13) : broadcast'(:task,nil)\n" +
                " |  anon : (lin 8, col 21) : error(:error)\n" +
                " v  error : :error\n") { out }
    }
    @Test
    fun ee_02_pool_throw() {
        val out = test(
            """
            spawn (task () {
                catch (err, err==:ok) {
                    spawn task () {
                        yield(nil)
                        error(:ok)
                    } ()
                    loop { yield(nil) }
                }
            })()
            broadcast(nil)
            println(999)
        """
        )
        assert(out == "999\n") { out }
    }
    @Test
    fun ee_03_pool_term() {
        val out = test(
            """
            var T
            set T = task () {
                yield(nil)
                error(nil)
            }
            spawn T()
            spawn T()
            broadcast( @[] )
        """
        )
        assert(out == " |  anon : (lin 9, col 13) : broadcast'(:task,@[])\n" +
                " |  anon : (lin 5, col 17) : error(nil)\n" +
                " v  error : nil\n") { out }
    }

    // SCOPE

    @Test
    fun ff_01_scope() {
        val out = test("""
            val T = task () { yield(nil) }
            var x
            do {
                val t = spawn T()
                set x = ;;;track;;;(t)
            }
            println(:ok)
        """)
        //assert(out == " v  anon : (lin 6, col 21) : set error : cannot expose track outside its task scope\n") { out }
        assert(out == ":ok\n") { out }
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
            val t = ;;;detrack;;;(x) ;;{ it=>it }   ;; err: cannot escape here
            println(type(t))
            broadcast(nil)
            println(status(t))
        """)
        //assert(out == " v  anon : (lin 9, col 24) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 10, col 21) : status(t) : status error : expected running coroutine or task\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration error : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape error : cannot expose task in pool to outer scope\n") { out }
        assert(out == ":exe-task\n" +
                ":terminated\n") { out }
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
            val t = ;;detrack(x) { it =>
                set y = x ;;it  ;; ERR: cannot expose it
                do nil
            ;;}
            broadcast(nil)
            println(status(t))
        """)
        //assert(out == " v  anon : (lin 9, col 24) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 10, col 21) : status(t) : status error : expected running coroutine or task\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration error : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 9, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 10, col 21) : set error : cannot expose task in pool to outer scope\n") { out }
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun ff_02y_detrack_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            spawn (task () {
                ;;detrack(x) { it =>
                    set pub = x ;;it  ;; ERR: cannot expose it
                    do nil
                ;;}
                broadcast(nil) in :global
                println(status(pub))
            }) ()
            println(:nooo)
        """)
        assert(out == ":terminated\n:nooo\n") { out }
        //assert(out == " v  anon : (lin 9, col 24) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 10, col 21) : status(t) : status error : expected running coroutine or task\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration error : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 8, col 13) : (spawn (task () { (detrack(x) { it => (set pu...)\n" +
        //        " |  anon : (lin 9, col 28) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 10, col 25) : set error : cannot expose task in pool to outer scope\n") { out }
    }
    @Test
    fun ff_03_detrack_err() {
        val out = test("""
            val T = task () {
                ${AWAIT()}
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            println(;;;detrack;;;(x) ;;;{it=>it};;;)
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
            val x = ;;;track;;;(t)
            println(do { ;;detrack(x) { it =>
                val z = x ;;it
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
                ;;;track;;;(t)
            }
            println(status(x))
        """)
        //assert(out == " v  anon : (lin 5, col 21) : block escape error : cannot expose track outside its task scope\n") { out }
        assert(out == ":terminated\n") { out }
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
            println(x)
        """)
        assert(out.contains("exe-task: 0x")) { out }
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
            ;;detrack(x) { it => println(it) }
            println(x)
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
                set pub = [10]
                yield(nil) ; nil
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            ;;println(detrack(x) { it => it.pub })
            println(x.pub)
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
                set pub = [10]
                yield(nil) ; nil
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            broadcast(nil)
            ;;println(detrack(x) { it => it.pub }) ;; expose (ok, global func)
            println((x).pub) ;; expose (ok, global func)
        """)
        assert(out == "nil\n") { out }
        //assert(out == " v  anon : (lin 9, col 49) : pub error : expected task\n") { out }
    }
    @Test
    fun fg_03_detrack_pub() {
        val out = test("""
            val T = task () {
                set pub = [10]
                yield(nil) ; nil
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            val v = ;;;detrack;;;(x) ;;;{ it => it;;;.pub ;;}
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
                    set pub = []
                    yield(nil) ;; nil
                    nil
                }
                spawn (T) () in ts
                val trk = next-tasks(ts)
                val p = ;;;detrack;;;(trk) ;;{ it => it }
                p
            }
            println(status(x))
        """)
        assert(out == ":terminated\n") { out }
        //assert(out == ":pub\t[]\n" +
        //        " v  anon : (lin 2, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 11, col 17) : declaration error : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 11, col 38) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 11, col 38) : block escape error : cannot expose task in pool to outer scope\n") { out }
    }
    @Test
    fun fg_05_expose() {
        val out = test("""
            var T = task (t) {
                set pub = []
                if t {
                    val p = ;;;detrack;;;(t) .pub ;;{ it => pub(it) }
                } else {
                    nil
                }
                yield(nil) ;; nil
                nil
            }
            val t = spawn T ()
            spawn T (;;;track;;;(t))
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
        //        " |  anon : (lin 5, col 40) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 5, col 40) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 13, col 19) : T(track(t))\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
        //        " |  anon : (lin 5, col 40) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 5, col 40) : block escape error : reference has immutable scope\n") { out }
        //assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
        //        " v  anon : (lin 5, col 21) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun fg_06_expose() {
        val out = test("""
            val f = func (t) {
                println(t)
            }
            val T = task () {
                set pub = []
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
                ;;detrack(xx1) { it => f(it.pub) }
                f(xx1.pub)
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
                set pub = []
                yield(nil) ;; nil
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            ;;detrack(x) { t => f(pub(t)) }
            f(x.pub)
            println(:ok)
        """)
        assert(out == "[]\n:ok\n") { out }
    }
    @Test
    fun fg_07_throw_track() {
        val out = test("""
            val T = task () {
                set pub = 10
                yield(nil) ; nil
            }
            val ts = tasks()
            val t = catch ( it,true) {
                spawn T() in ts
                do {
                    val u = next-tasks(ts)
                    error(;;;drop;;;(u))
                }
            }
            ;;detrack(t) { it => println(it.pub) }
            println(t.pub)
        """)
        //assert(out == ":ok\n") { out }
        assert(out == "10\n") { out }
        //assert(out.contains("TODO: error inside throw")) { out }
    }
    @Test
    fun fg_08_throw_track() {
        val out = test("""
            val T = task () {
                yield(nil) ; nil
            }
            val x = do {
                val ts = tasks()
                spawn T() in ts
                do {
                    val t = next-tasks(ts)
                    error(;;;drop;;;(t))
                    nil
                }
            }
            println(status(x))
        """)
        assert(out.contains(" |  anon : (lin 10, col 21) : error(t)\n" +
                " v  error : exe-task: 0x")) { out }
        //assert(out.contains("TODO: error inside throw")) { out }
        //assert(out == (" v  anon : (lin 5, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun fg_09_throw_track() {
        val out = test("""
            val T = task () {
                yield(nil) ; nil
            }
            val ts = tasks()
            catch ( it,true) {
                spawn T() in ts
                do {
                    val t = next-tasks(ts)
                    error(;;;drop;;;(t))
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // NEXT

    @Test
    fun hh_00_next() {
        val out = test("""
            val T = task () {
                nil
            }
            val ts = tasks()
            spawn T() in ts
            println(next-tasks(ts))
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun hh_00x_next() {
        val out = test("""
            val T = task () {
                yield(nil)
            }
            val ts = tasks()
            val t = spawn T() in ts
            println(next-tasks(ts))
            println(next-tasks(ts,t))
        """
        )
        assert(out.contains(Regex("exe-task: 0x.*\nnil\n"))) { out }
    }
    @Test
    fun hh_01_next() {
        val out = test("""
            val T = task () {
                yield(nil)
            }
            val ts = tasks()
            println(next-tasks(ts))
            println(next-tasks(ts, nil))
            println(next-tasks(ts, :err))
        """
        )
        //assert(out == "nil\n" +
        //        "nil\n" +
        //        " v  anon : (lin 8, col 21) : next-tasks(ts,:err) : next-tasks error : expected task-in-pool track\n") { out }
        assert(out == "nil\n" +
                "nil\n" +
                " |  anon : (lin 8, col 21) : next-tasks(ts,:err)\n" +
                " v  next-tasks error : expected task\n") { out }
    }
    @Test
    fun hh_02_next() {
        val out = test("""
            val T = task () {
                yield(nil)
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
        assert(out.contains("true\ntrue\ntrue\ntrue\nexe-task: 0x")) { out }
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
            println(next-tasks(ts, x1) == x2)
        """
        )
        //assert(out == " v  anon : (lin 11, col 13) : next-tasks(ts,x1) : next-tasks error : expected task-in-pool track\n") { out }
        assert(out == "true\n") { out }
    }
    @Test
    fun hh_05_next() {
        val out = test("""
            val T = task (v) {
                set pub = [v]
                yield(nil)
            }
            val ts = tasks()
            spawn T(10) in ts
            val x1 = next-tasks(ts)
            ;;val v = detrack(x1) { it => println(it.pub) ; it.pub }
            val v = (x1).pub
            println(v)
        """
        )
        assert(out == "[10]\n") { out }
        //assert(out == "[10]\n" +
        //        " |  anon : (lin 9, col 33) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 9, col 33) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[10]\n" +
        //        " |  anon : (lin 9, col 33) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 9, col 33) : block escape error : reference has immutable scope\n") { out }
    }
    @Test
    fun hh_06_pool_terminate() {
        val out = test("""
            do {
                var ts
                set ts = tasks()
                var T
                set T = task (v) {
                    println(v)
                }
                spawn T(1) in ts
                loop {
                    val t = next-tasks(ts)
                    break if (if t { false } else { true })
                    error(1)    ;; never reached
                }
            }
            broadcast(2)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_07_pool_err() {
        val out = test("""
            println(next-tasks(nil))
        """)
        assert(out == " |  anon : (lin 2, col 21) : next-tasks(nil)\n" +
                " v  next-tasks error : expected tasks\n") { out }
    }
    @Test
    fun hh_08_pool_term() {
        val out = test("""
            var T = task () {
                yield(nil)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                println(1)
                broadcast (1)
            }
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun hh_09_pool_term() {
        val out = test("""
            var T
            set T = task () {
                yield(nil)
                yield(nil)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                println(1)
                broadcast(1)
                var yyy = nil
                loop  {
                    set yyy = next-tasks(ts, yyy)
                    break if (if yyy { false } else { true })
                    println(2)
                }
            }
            println(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun hh_10_pool_plain() {
        val out = test("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn T() in ts
            var yyy
            var xxx = nil
            loop {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                set yyy = xxx
            }
            println(status(;;;detrack;;;(yyy)))
        """)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun hh_11_pool_move() {
        val out = test("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn T() in ts
            var yyy
            var xxx = nil
            loop {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                set yyy = ;;;move;;;(xxx)
            }
            println(status(;;;detrack;;;(yyy)))
        """)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun TODO_hh_12_pool_check() {   // copy task
        val out = test("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                println(;;;detrack;;;(xxx) == ;;;detrack;;;(xxx))
            }
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun TODO_hh_13_pool_err_scope() {   // copy task
        val out = test("""
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = tasks()
            spawn T() in ts
            var xxx = nil
            var yyy
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                set yyy = copy(xxx)
            }
            broadcast(nil)
            println(;;;detrack;;;(yyy))
        """, true)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun hh_14_pool_bcast() {
        val out = test("""
            var T = task () { yield(nil); println(:ok) }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                broadcast(nil) in ;;;detrack;;;(xxx)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun hh_15_pool_err_scope() {
        val out = test(
            """
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                var yyy
                var zzz = nil
                loop ;;;in :tasks ts, zzz;;; {
                    set zzz = next-tasks(ts, zzz)
                    break if (if zzz { false } else { true })
                    set yyy = ;;;copy;;;(zzz)
                    println(status(;;;detrack;;;(yyy)))
                }
                println(status(;;;detrack;;;(yyy)))
                set yyy = xxx
            }
        """
        )
        //assert(out == "anon : (lin 10, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 10, col 25) : set error : incompatible scopes\n:error\n") { out }
        assert(out == ":yielded\n:yielded\n") { out }
    }
    @Test
    fun hh_16_pool_scope() {
        val out = test(
            """
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                var yyy
                var zzz = nil
                loop ;;;in :tasks ts, zzz;;; {
                    set zzz = next-tasks(ts, zzz)
                    break if (if zzz { false } else { true })
                    do nil
                }
                set yyy = xxx
                ;;pass nil ;; otherwise scope err for yyy/xxx
            }
            println(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_17_pool_scope() {
        val out = test(
            """
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                do xxx
            }
            println(1)
        """
        )
        //assert(out == "anon : (lin 7, col 13) : set error : incompatible scopes\n:error\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_18_ff_pool() {
        val out = test("""
            var ts
            set ts = tasks()
            println(type(ts))
            var T
            set T = task (v) {
                set pub = v
                val v' = yield(nil)
            }
            spawn T(1) in ts
            spawn T(2) in ts
            
            var t1 = nil
            loop ;;;in :tasks ts, t1;;; {
                set t1 = next-tasks(ts, t1)
                break if (if t1 { false } else { true })
                var t2 = nil
                loop ;;;in :tasks ts, t2;;; {
                    set t2 = next-tasks(ts, t2)
                    break if (if t2 { false } else { true })
                    println(;;;detrack;;;(t1).pub, ;;;detrack;;;(t2).pub)
                }
            }
             broadcast( 2 )
        """)
        assert(out == ":tasks\n1\t1\n1\t2\n2\t1\n2\t2\n") { out }
    }
    @Test
    fun hh_19_pub_pool() {
        val out = test("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn T() in ts
            var t
            loop ;;;in :tasks ts, t;;; {
                set t = next-tasks(ts,t)
                break if (if t { false } else { true })
                println(;;;detrack;;;(t).pub[0])
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_20_pool_term() {
        val out = test("""
            var T = task () {
                spawn task () {
                    yield(nil)
                }()
                yield(nil)
            }
            var ts = tasks()
            spawn T() in ts 
            spawn T() in ts
            spawn task () {
                var xxx = nil
                loop ;;;in :tasks ts, xxx;;; {
                    set xxx = next-tasks(ts, xxx)
                    break if (if xxx { false } else { true })
                    println(1)
                    broadcast (1)
                }
            } ()
            println(2)
        """)
        //assert(out == "1\n2\n") { out }
        assert(out == "1\n1\n2\n") { out }
    }
    @Test
    fun hh_21_pool_val() {
        val out = test("""
            val T = task () {
                yield(nil)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                val tsk = ;;;detrack;;;(xxx)
                broadcast (nil)
            }
            println(:ok)
        """)
        //assert(out == "anon : (lin 8, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun hh_22_ff_pool_val() {
        val out = test("""
            val T = task () {
                yield(nil)
            }
            var ts = tasks()
            spawn T() in ts
            var trk = nil
            loop ;;;in :tasks ts, trk;;; {
                set trk = next-tasks(ts, trk)
                break if (if trk { false } else { true })
                val      tsk1 = ;;;detrack;;;(trk)
                val ;;;:tmp;;; tsk2 = ;;;detrack;;;(trk)
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 8, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun hh_23_pool_scope() {
        val out = test("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                do {
                    val zzz = ;;;detrack;;;(xxx)
                    nil
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun hh_24_pub_pool_err() {
        val out = test("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn T() in ts
            var x
            var t
            loop ;;;in :tasks ts, t;;; {
                set t = next-tasks(ts,t)
                break if (if t { false } else { true })
                set x = ;;;detrack;;;(t).pub   ;; TODO: incompatible scope
            }
            println(999)
        """)
        assert(out == "999\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        //assert(out == "anon : (lin 12, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_25_pub_pool_err() {
        val out = test("""
            var T = task () {
                set ;;;task.;;;pub = [10]
                yield(nil)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                var x = ;;;detrack;;;(xxx).pub
                broadcast( nil )in ;;;detrack;;;(xxx)
                println(x)
            }
            println(999)
        """)
        //assert(out == "[10]\n999\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 17) : declaration error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_26_pub_pool_err() {
        val out = test("""
            var T = task () {
                set ;;;task.;;;pub = [10]
                yield(nil)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                var f = func (tt) {
                    var x = ;;;detrack;;;(tt).pub
                    broadcast(nil) in ;;;detrack;;;(tt)
                    println(x)
                }
                f(xxx)
            }
            println(999)
        """)
        assert(out == "[10]\n999\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        //assert(out == "anon : (lin 14, col 17) : f(t)\n" +
        //        "anon : (lin 10, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
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
    fun jj_01_tracks() {
        val out = test("""
            val T = task () { yield(nil) ; nil }
            val ts = tasks()
            spawn T() in ts
            val vec = #[]
            val t = next-tasks(ts,nil)
            set vec[#vec] = t
            println(vec)
        """)
        //assert(out.contains("#[track: 0x")) { out }
        assert(out.contains("#[exe-task: 0x")) { out }
    }
    @Test
    fun jj_02_tracks() {
        val out = test("""
            val f = func (trk) {
                ;;println(detrack(trk) { it => status(it) })
                println(status(trk))
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
        assert(out==(":yielded\n:terminated\n")) { out }
        //assert(out.contains(":yielded\n" +
        //        " v  anon : (lin 7, col 22) : block escape error : reference has immutable scope\n")) { out }
        //assert(out.contains(":yielded\n" +
        //        " v  anon : (lin 7, col 22) : block escape error : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun jj_03_tracks() {
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
        //assert(out.contains("#[track: 0x")) { out }
        assert(out.contains("#[exe-task: 0x")) { out }
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
                set x = ;;;track;;;(t)
            }
            println(:ok)
        """)
        //assert(out == " v  anon : (lin 10, col 25) : track(t) : track error : expected task\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun oo_03_track_err() {
        val out = test("""
            var T
            set T = task (v) {
                set pub = [v]
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
            println(:ok)
        """)
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == " v  anon : (lin 14, col 25) : set error : cannot assign reference to outer scope\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun BUG_oo_04_track() {
        val out = test("""
            var T = task (v) {
                yield(nil) ; nil
            }
            var ts = tasks()
            spawn T(1) in ts
            val x = do {
                val t = next-tasks(ts)
                ;;detrack(t) { it => it } ;; ERR: cannot expose
                nil
            }
            println(x)
        """)
        //assert(out == " v  anon : (lin 7, col 13) : declaration error : cannot expose task-in-pool reference\n") { out }
        //assert(out.contains("exe-task: 0x")) { out }
        //assert(out == (" |  anon : (lin 9, col 28) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 9, col 28) : block escape error : cannot expose reference to task in pool\n")) { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun oo_05_xceu() {
        val out = test("""
            var T
            set T = task (pos) {
                yield(nil)
                println(pos)
            }
            spawn (task () {
                var ts
                set ts = tasks()
                do {
                    spawn T([]) in ts  ;; pass [] to ts
                }
                yield(nil)
                yield(nil)
            })()
            broadcast (nil)
        """)
        assert(out == "[]\n") { out }
    }

    // ORIGINAL / TRACK / DETRACK

    @Test
    fun op_00_track() {
        val out = test("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil) ; nil
            }
            var t = spawn T ()
            var x = ;;;track;;;(t)
            println(;;;detrack;;;(x))
            broadcast( nil )
            println(;;;detrack;;;(x))
        """)
        //assert(out == "true\nfalse\n") { out }
        assert(out.contains(Regex("exe-task: 0x.*\nexe-task: 0x.*\n"))) { out }
    }
    @Test
    fun op_01_track() {
        val out = test("""
            val T = task () {
                yield(nil) ;; nil
                set pub = 10
                yield(nil) ; nil
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            ;;detrack(x) { it => println(it.pub) } 
            println(x.pub) 
            broadcast(nil)
            ;;detrack(x) { it => println(it.pub) } 
            println(x.pub) 
        """)
        assert(out == "nil\n10\n") { out }
    }
    @Test
    fun op_02_track() {
        val out = test("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil) ; nil
            }
            var t = spawn T ()
            var x = ;;;track;;;(t)
            ;;detrack(x) { it => println(pub(it)[0]) }
            println(x.pub[0])
            println(status(x))
            ;;println(detrack(x))
            broadcast( nil )
            ;;println(detrack(x) { it => 999 })
            println(status(x))
        """)
        //assert(out == "10\ntrue\nnil\n") { out }
        assert(out == "10\n:yielded\n:terminated\n") { out }
    }
    @Test
    fun op_02x_track() {
        val out = test("""
            val T = task () {
                yield(nil)
            }
            var t = spawn T ()
            val x = ;;;track;;;(t)
            println(status(x))
            ;;println(detrack''(x))
            ;;detrack(x) { it => nil }
            ;;println(detrack''(x))
            println(status(x))
        """)
        //assert(out == "true\ntrue\n") { out }
        assert(out == ":yielded\n:yielded\n") { out }
    }
    @Test
    fun op_03_track_err() {
        val out = test("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil) ; nil
            }
            var x
            do {
                var t = spawn (T) ()
                set x = ;;;track;;;(t)         ;; scope x < t
                ;;println(detrack(x).pub[0])
            }
            ;;println(status(detrack(x)))
            ;;println(x)
            println(:ok)
        """)
        //assert(out.contains("10\n:terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 10, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 10, col 21) : set error : cannot expose track outside its task scope\n")) { out }
        assert(out == (":ok\n")) { out }
    }
    @Test
    fun op_04_track() {
        val out = test("""
            var T = task () {
                set pub = [10]
                ${AWAIT("it == :evt")}
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            spawn( task () {
                catch ( err,err==:par-or ) {
                    spawn( task () {
                        yield(nil) ;;thus { it => it==t }
                        error(:par-or)
                    }) ()
                    ;;println(detrack(x) { it => it.pub[0] })
                    println((x).pub[0])
                    broadcast(nil) in t
                    ;;println(detrack(x) { it => it.pub[0] })
                    println((x).pub[0])
                    broadcast(:evt) in t
                    println(999)
                }
                ;;println(detrack(x) { it => if it {999} else {nil} })
                println(status(x))
                nil
            })()
            println(:ok)
        """)
        assert(out == "10\n10\n:terminated\n:ok\n") { out }
    }
    @Test
    fun op_05_detrack_err() {
        val out = test("""
            val T = task () {
                yield(nil) ; nil
            }
            val t1 = spawn T()
            val r1 = ;;;track;;;(t1)
            ;;detrack(r1) { x1 =>
                val x1 = r1
                ;;println(t1, r1, x1, status(t1))
                println(status(t1))
                broadcast( nil )
                ;;println(t1, r1, x1, status(t1))
                println(status(t1)) ;; never reached
            ;;}
            println(:ok)
        """)
        //assert(out == ":yielded\n:ok\n") { out }
        assert(out == ":yielded\n:terminated\n:ok\n") { out }
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
                val x = ;;;track;;;(t)
                x
            }
            println(y)
        """)
        //assert(out == " v  anon : (lin 6, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun op_07_track_scope() {
        val out = test("""
            val T = task () {
                set pub = 1
                yield(nil) ; nil
            }
            val t = spawn T()
            val y = do {
                ;;;track;;;(t)
            }
            ;;detrack(y) { it => println(pub(it)) }
            println(y.pub)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun op_08_track_scope() {
        val out = test("""
            val T = task () {
                set pub = 1
                yield(nil) ; nil
            }
            val y = do {
                val t = spawn T()
                ;;;track;;;(t)
            }
            ;;detrack(y) { it => println(it.pub) }
            println(y.pub)
        """)
        assert(out == "1\n") { out }
        //assert(out == " v  anon : (lin 6, col 21) : block escape error : cannot expose track outside its task scope\n") { out }
    }
    @Test
    fun BUG_op_09_track_throw() {
        // aborted trask in pool does not bcast itself to clear track
        val out = test("""
            val T = task () {
                defer {
                    println(:ok)
                }
                spawn( task () {
                    yield(nil) ; nil
                    println(:before-throw)
                    throw(:error)               ;; 2. kill task
                })()
                yield(nil) ; nil
            }
            val ts = tasks()
            spawn T() in ts
            val t = next-tasks(ts)
            catch (it=>true) {
                broadcast(nil)                  ;; 1. awake task
            }
            ;;`ceu_gc_collect();`
            detrack(t) { it =>
                println(:it, it, status(it))    ;; 3. should not execute
            }
        """)
        assert(!out.contains(":it")) { out }
    }
    @Test
    fun op_10_track() {
        val out = test("""
            var T = task (v) {
                set pub = [v]
                yield(nil) ;;{ as it => nil }
            }
            var x
            var ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            var t
            loop ;;;in :tasks ts, t;;; {
                set t = next-tasks(ts,t)
                break if (if t { false } else { true })
                set x = ;;;copy;;;(t)
            }
            println(;;;detrack;;;(x).pub[0])   ;; 2
            broadcast (nil)
            println(;;;detrack;;;status(x))   ;; nil
        """)
        //assert(out == "2\nnil\n") { out }
        assert(out == "2\n:terminated\n") { out }
    }
    @Test
    fun op_11_track() {
        val out = test("""
            var T
            set T = task (v) {
                set ;;;task.;;;pub = [v]
                yield(nil)
            }
            var x
            var ts
            set ts = tasks()
            do {
                spawn T(1) in ts
                spawn T(2) in ts
                var t
                loop ;;;in :tasks ts, t;;; {
                    set t = next-tasks(ts,t)
                    break if (if t { false } else { true })
                    set x = ;;;copy;;;(t)    ;; track(t) up_hold in
                }
                println(;;;detrack;;;(x).pub[0])   ;; 2
                broadcast (nil)
                println(;;;detrack;;;status(x))   ;; nil
            }
        """)
        assert(out == "2\n:terminated\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun op_12_track_throw() {
        val out = test("""
            var T
            set T = task (v) {
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn T(1) in ts
            var x
            set x = catch (_,true) {
                var t
                loop ;;;in :tasks ts, t;;; {
                    set t = next-tasks(ts,t)
                    break if (if t { false } else { true })
                    error(;;;copy;;;(t))
                }
            }
            broadcast (nil)
            println(;;;detrack;;;status(x))   ;; nil
        """)
        //assert(out == "nil\n") { out }
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun op_13_track_throw() {
        val out = test("""
            var T
            set T = task (v) {
                set ;;;task.;;;pub = [v]
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            var x
            set x = catch (_,true) {
                var t
                loop ;;;in :tasks ts, t;;; {
                    set t = next-tasks(ts,t)
                    break if (if t { false } else { true })
                    error(;;;copy;;;(t))
                }
            }
            println(;;;detrack;;;(x).pub[0])   ;; 1
            println(status(;;;detrack;;;(x)))   ;; :yielded
            broadcast (nil)
            println(;;;detrack;;;status(x))   ;; nil
        """)
        //assert(out == "1\n:yielded\nnil\n") { out }
        assert(out == "1\n:yielded\n:terminated\n") { out }
    }
    @Test
    fun op_14_track_simplify() {
        val out = test("""
            var T = task (v) {
                yield(nil)
            }
            var ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            var x
            var t
            loop ;;;in :tasks ts, t;;; {
                set t = next-tasks(ts,t)
                break if (if t { false } else { true })
                set x = ;;;copy;;;(t)
            }
            broadcast( nil )
            println(;;;detrack;;;status(x))   ;; nil
        """)
        //assert(out == "nil\n") { out }
        assert(out == ":terminated\n") { out }
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
    fun zz_02_all() {
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
                    break((t)) if true
                }
            }
            println(x)
       """)
        //assert(out == "nil\n") { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun zz_03_all() {
        val out = test("""
            val x = do {
                val tt = [nil]
                loop {
                    set tt[0] = @[]
                    val t = tt[0]
                    break(false) if {{==}}(t,nil)
                    break((t)) if true
                }
            }
            println(x)
       """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun zz_df_03_bcast_throw() {
        DEBUG = true
        val out = test("""
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    yield(nil)
                    println(:ok)
                    error(:XXX)
                }) ()
                spawn (task () {
                    yield(nil)
                    broadcast (nil) in :global
                }) ()
                loop {
                    yield(nil)
                }
            }) ()            
            broadcast(nil)
        """)
        assert(out == ":ok\n" +
                " |  anon : (lin 17, col 13) : broadcast'(:task,nil)\n" +
                " |  anon : (lin 11, col 21) : broadcast'(:global,nil)\n" +
                " |  anon : (lin 7, col 21) : error(:XXX)\n" +
                " v  error : :XXX\n") { out }
    }
    @Test
    fun zz_05_99() {
        val out = test("""
            (spawn (task () {
                do {
                    spawn (task () {
                        yield(nil)
                    }) ()
                    loop {
                        yield(nil)
                    }
                }
            })())
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
}
