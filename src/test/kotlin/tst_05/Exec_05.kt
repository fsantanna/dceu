package tst_05

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_05 {
    // TASKS / PROTO / SCOPE

    @Test
    fun ab_01_tasks_proto() {
        val out = test("""
            val ts = tasks()
            do {
                spawn(func () {
                    nil
                }) () in ts
            }
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ab_02_tasks_proto_err() {
        val out = test("""
            val ts = tasks()
            do {
                val T = func () {
                    nil
                }
                spawn T() in ts
            }
            print(:ok)
       """)
        //assert(out == " v  anon : (lin 7, col 17) : spawn throw : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 7, col 17) : spawn throw : task pool outlives task prototype\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ab_03_tasks_proto() {
        val out = test("""
            do {
                val ts = tasks()
                val T = func () {
                    nil
                }
                spawn T() in ts
            }
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ab_04_tasks_proto() {
        val out = test("""
            val T = func () {
                nil
            }
            do {
                val ts = tasks()
                spawn T() in ts
            }
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ab_05_tasks_proto() {
        val out = test("""
            val ts = tasks()
            spawn (func () {
                spawn (func () {    ;; anon task is dropped to ts
                    nil
                }) () in ts
                nil
            })()
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ab_06_tasks_prim() {
        val out = test("""
            val T = func () {
                defer {
                    print(:task)
                }
                loop' {
                    await(true)
                }
            }
            val f = tasks
            do {
                spawn T() in f()
            }
            print(:ok)
       """)
        assert(out == "anon : (lin 11, col 13) : expected \"(\" : have \"do\"\n") { out }
        //assert(out == ":ok\n") { out }
    }

    // TRACK

    @Test
    fun bb_01_track_err() {
        val out = test("""
            track(nil)
        """)
        //assert(out == " v  anon : (lin 2, col 13) : track(nil) : track throw : expected task\n") { out }
        assert(out == "anon : (lin 2, col 13) : access throw : variable \"track\" is not declared\n") { out }
    }
    @Test
    fun bb_02_track_err() {
        val out = test("""
            val T = func () {
                nil
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            print(t, x)
        """)
        assert(out.contains(Regex("exe-task: 0x.*exe-task: 0x"))) { out }
        //assert(out == " v  anon : (lin 6, col 21) : track(t) : track throw : expected unterminated task\n") { out }
        //assert(out == " v  anon : (lin 6, col 21) : track(t) : track throw : expected task\n") { out }
    }
    @Test
    fun bb_03_track() {
        val out = test("""
            val T = func () {
                await(true)
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            print(x)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bb_04_track() {
        val out = test("""
            val T = func () { await(true);nil }
            val t = spawn T ()
            val x = ;;;track;;;(t)
            val y = ;;;track;;;(t)
            var z = y
            print(x==y, y==z)
        """)
        //assert(out == ("false\ttrue\n")) { out }
        assert(out == ("true\ttrue\n")) { out }
    }
    @Test
    fun bb_05_bcast_in_task_err() {
        val out = test("""
            val T = func (v) {
                ${AWAIT()}
                ;;await(true)
                print(v)
            }
            val t1 = spawn T (1)
            val x1 = ;;;track;;;(t1)
            val t2 = spawn T (2)
            emit (nil) in x1
        """)
        //assert(out == " v  anon : (lin 10, col 13) : emit'(nil,x1) : invalid target\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_05_bcast_in_task_ok() {
        val out = test("""
            val T = func (v) {
                ${AWAIT()}
                ;;await(true)
                print(v)
            }
            val t1 = spawn T (1)
            val x1 = ;;;track;;;(t1)
            val t2 = spawn T (2)
            ;;detrack(x1) { it => emit (nil) in it }
            emit (nil) in x1
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_05_bcast_in_task() {
        val out = test("""
            val T = func (v) {
                ${AWAIT()}
                ;;await(true)
                print(v)
            }
            val t1 = spawn T (1)
            val x1 = ;;;track;;;(t1)
            val t2 = spawn T (2)
            ;;detrack(x1) { y1 =>
                emit (nil) in x1 ;;y1
            ;;}
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_06_track_up() {
        DEBUG = true
        val out = test("""
            val T = func () { await(true);await(true) }
            spawn (func () {
                val ts = tasks()
                spawn (func () {
                    spawn (func () {
                        spawn T() in ts
                    }) ()
                    nil
                }) ()
                do {
                    val t = next-tasks(ts)
                    do {
                        ;;dump(t)
                        emit(true) in :global
                        ;;dump(t)                    
                    }
                    print(;;;detrack;;;status(t))
                }
            }) ()
            print(:ok)
        """)
        assert(out == (":terminated\n:ok\n")) { out }
    }

    // TRACK / SCOPE / ERROR

    @Test
    fun bd_01_track_err() {
        val out = test("""
            var T
            set T = func () { await(true) }
            var x
            do {
                val t = spawn (T) ()
                set x = ;;;track;;;(t)         ;; throw scope
            }
            print(status(;;;detrack;;;(x)))
            print(x)
        """)
        assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == (" v  anon : (lin 7, col 21) : set throw : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun bd_02_track_err() {
        val out = test("""
            var T
            set T = func () { await(true) }
            val x = do {
                val t = spawn (T) ()
                ;;;track;;;(t)         ;; throw scope
            }
            print(status(;;;detrack;;;(x)))
            print(x)
        """)
        assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == (" v  anon : (lin 4, col 21) : block escape throw : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun bd_03_track_err() {
        val out = test("""
            var T
            set T = func () { await(true) }
            val t1 = spawn T()
            do {
                val t2 = spawn T()
                set t1.pub = ;;;track;;;(t2)         ;; throw scope
                nil
            }
            print(;;;detrack;;;(t1.pub))
        """)
        assert(out.contains("exe-task: 0x")) { out }
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == (" v  anon : (lin 5, col 13) : block escape throw : reference has immutable scope\n")) { out }
        //assert(out == (" v  anon : (lin 5, col 13) : block escape throw : cannot expose track outside its task scope\n")) { out }
        //assert(out == (" v  anon : (lin 8, col 21) : set throw : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun bd_04_track_err() {
        val out = test("""
            var T
            set T = func () { await(true) }
            var x =
            do {
                val t = spawn (T) ()
                val x' = ;;;track;;;(t)
                x'         ;; throw scope
            }
            print(status(;;;detrack;;;(x)))
            print(x)
        """)
        assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == (" v  anon : (lin 5, col 13) : block escape throw : cannot expose track outside its task scope\n")) { out }
    }

    // TRACK / DROP

    @Test
    fun bc_01_track_drop() {
        val out = test("""
            val T = func () { await(true) }
            val t = spawn T ()
            val y = do {
                val x = ;;;track;;;(t)
                ;;;drop;;;(x)
            }
            print(y)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_02_track_drop_err() {
        val out = test("""
            val T = func () { await(true) }
            val y = do {
                val t = spawn T ()
                ;;;track;;;(t)
            }
            print(y)
        """)
        //assert(out == (" v  anon : (lin 3, col 21) : block escape throw : cannot expose track outside its task scope\n")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_02x_track_drop_err() {
        val out = test("""
            val T = func () { await(true) }
            val y = do {
                val t = spawn T ()
                val x = ;;;track;;;(t)
                ;;;drop;;;(x)
            }
            print(y)
        """)
        //assert(out == (" v  anon : (lin 3, col 21) : block escape throw : cannot expose track outside its task scope\n")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_03_track_drop() {
        val out = test("""
            val T = func () { await(true) }
            val ts = tasks()
            val y = do {
                spawn T () in ts
                print()
                ;;;drop;;;(next-tasks(ts))
            }
            print(y)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_04_track_drop() {
        val out = test("""
            val T = func () { await(true) }
            val y = do {
                val ts = tasks()
                spawn T () in ts
                ;;;drop;;;(next-tasks(ts))
            }
            print(y)
        """)
        //assert(out == (" v  anon : (lin 3, col 21) : block escape throw : cannot expose track outside its task scope\n")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_05_track_drop() {
        val out = test("""
            val T = func () { await(true) }
            val t = spawn T ()
            val y = do {
                ;;;track;;;(t)
            }
            print(y)
        """)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_06_track_drop() {
        val out = test("""
            val T = func () { await(true) }
            val t = spawn T ()
            val y = do {
                ;;;drop;;;(;;;track;;;(t))
            }
            print(y)
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
        //        " v  anon : (lin 2, col 58) : detrack'(trk) : detrack throw : expected track value\n") { out }
        assert(out == "anon : (lin 3, col 13) : access throw : variable \"detrack\" is not declared\n") { out }
    }
    @Test
    fun cc_01_detrack() {
        val out = test("""
            detrack(nil) { it => nil }
        """)
        //assert(out == " v  anon : (lin 3, col 13) : detrack'(nil) : detrack throw : expected track value\n") { out }
        assert(out == "anon : (lin 3, col 26) : expected expression : have \"{\"\n") { out }
    }
    @Test
    fun cc_02_detrack() {
        val out = test("""
            val x
            detrack(nil) { x => nil }
        """)
        //assert(out == "anon : (lin 3, col 28) : declaration throw : variable \"x\" is already declared\n") { out }
        assert(out == "anon : (lin 3, col 26) : expected expression : have \"{\"\n") { out }
    }
     */
    @Test
    fun cc_03_detrack() {
        val out = test("""
            val T = func () { nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = 10 ;;detrack(t) { it => 10 }
            print(v)
        """)
        //assert(out == (" v  anon : (lin 4, col 21) : track(t) : track throw : expected unterminated task\n")) { out }
        //assert(out == (" v  anon : (lin 4, col 21) : track(t) : track throw : expected task\n")) { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_04_detrack() {
        val out = test("""
            val T = func () { await(true) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            emit(true)
            val v = 10 ;;detrack(x) { it => 10 }
            print(v)
        """)
        //assert(out == ("nil\n")) { out }
        assert(out == ("10\n")) { out }
    }
    @Test
    fun cc_05_detrack() {
        val out = test("""
            val T = func () { await(true) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = 10 ;;detrack(x) { it => 10 }
            print(v)
        """)
        assert(out == ("10\n")) { out }
    }
    @Test
    fun cc_06_detrack_err() {
        val out = test("""
            detrack(nil) { it => emit(true) }
        """)
        //assert(out == ("anon : (lin 2, col 37) : emit throw : unexpected enclosing detrack\n")) { out }
        //assert(out == (" v  anon : (lin 2, col 13) : detrack'(nil) : detrack throw : expected track value\n")) { out }
        assert(out == ("anon : (lin 2, col 26) : expected expression : have \"{\"\n")) { out }
    }
    @Test
    fun cc_07_detrack_err() {
        val out = test("""
            func () {
                detrack(nil) { it => func'(it) { nil } (yield(nil)) }
            }
        """)
        //assert(out == ("anon : (lin 3, col 43) : declaration throw : variable \"it\" is already declared\n")) { out }
        assert(out == ("anon : (lin 3, col 30) : expected expression : have \"{\"\n")) { out }
    }
    @Test
    fun cc_07_detrack_err2() {
        val out = test("""
            func () {
                detrack(nil) { yy => await(true) ; nil }
            }
            print(:ok)
        """)
        //assert(out == ("anon : (lin 3, col 38) : yield throw : unexpected enclosing func\n")) { out }
        //assert(out == (":ok\n")) { out }
        assert(out == ("anon : (lin 3, col 30) : expected expression : have \"{\"\n")) { out }
    }
    @Test
    fun cc_08_detrack() {
        val out = test("""
            val T = func () { await(true) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = 10 ;;detrack(x) { it => set it = 10 }
            print(v)
        """)
        //assert(out == ("anon : (lin 5, col 40) : set throw : destination is immutable\n")) { out }
        assert(out == ("10\n")) { out }
    }
    @Test
    fun cc_09_detrack() {
        val out = test("""
            val T = func (v) {
                await(true) ; nil
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            ;;dump(x)
            emit(true)
            print(;;;detrack;;;(x))
        """
        )
        //assert(out == "false\n") { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun cc_10_detrack() {
        val out = test("""
            val T = func (v) {
                ${AWAIT("it == v")}
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            emit(true)
            print(;;;detrack;;;(x))
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
            val T = func () { await(true) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = do { ;;detrack(x) { it =>
                val it = x
                print(:1, x)
                print(:2, t)
                print(:3, it)
                print(:4, `:bool ${D}it.type == CEU_VALUE_EXE_TASK`)
                print(:5, it == t)
            }
            print(:6, v)
        """)
        //assert(out.contains(":1\ttrack: 0x")) { out }
        assert(out.contains(":1\texe-task: 0x")) { out }
        assert(out.contains(":2\texe-task: 0x")) { out }
        assert(out.contains(":3\texe-task: 0x")) { out }
        assert(out.contains(":4\ttrue\n")) { out }
        assert(out.contains(":5\ttrue\n")) { out }
        assert(out.contains(":6\t:5\n")) { out }
    }
    @Test
    fun dd_02_detrack() {
        val out = test("""
            val T = func () { await(true) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = ;;;detrack;;;(x) ;;{ it=>it }
            print(status(v))
        """)
        assert(out.contains(":yielded\n")) { out }
    }
    @Test
    fun dd_03a_detrack_err() {
        val out = test("""
            val T = func () {
                await(true)
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            ;;detrack(x) { it =>
                val it = x
                print(it)
                emit(true)              ;; aborts it
                print(:xxx, status(it))   ;; dangling
            ;;}
            print(:ok)
        """)
        assert(out.contains("exe-task: 0x")) { out }
        //assert(!out.contains(":xxx")) { out }
        assert(out.contains(":xxx\t:terminated\n")) { out }
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun dd_03b_detrack_err() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn T() in ts
            val f = func' () {
                emit(true)
            }
            val x = next-tasks(ts)
            ;;detrack(x) { it =>
                val it = x
                print(it)
                f()
                print(:xxx, status(it))
            ;;}
            print(:ok)
        """)
        assert(out.contains("exe-task: 0x")) { out }
        //assert(!out.contains(":xxx")) { out }
        assert(out.contains(":xxx\t:terminated\n")) { out }
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun dd_04_detrack_eq() {
        val out = test("""
            val T = func () { await(true) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            ;;detrack(x) { v =>
                val v = x
                print(v == v)
                print(v == x)
            ;;}
        """)
        //assert(out == ("true\nfalse\n")) { out }
        assert(out == ("true\ntrue\n")) { out }
    }
    @Test
    fun dd_05_detrack_print() {
        val out = test("""
            val T = func () { await(true) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            ;;detrack(x) { v =>
                val v = x
                print(v)
            ;;}
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun dd_06_detrack_drop_err() {
        val out = test("""
            val T = func () { await(true) ; nil }
            val t = spawn T()
            val x = ;;;track;;;(t)
            val v = do { ;;detrack(x) { it =>
                val it = x
                ;;;drop;;;(it)
            }
            print(v)
        """)
        assert(out.contains("exe-task: 0x")) { out }
        //assert(out == " v  anon : (lin 6, col 22) : drop throw : value is not movable\n") { out }
        //assert(out == " |  anon : (lin 5, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 6, col 22) : drop throw : value contains multiple references\n") { out }
    }
    @Test
    fun dd_07_detrack_nested() {
        val out = test("""
            spawn (func () {
                val T = func () { await(true) ; nil }
                val t = spawn T()
                val x = ;;;track;;;(t)
                ;;detrack(x) { it => print(it) }
                print(x)
            }) ()
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun dd_08_detrack_nested() {
        val out = test("""
            spawn (func () {
                val z = 10
                val T = func () { await(true) ; nil }
                val t = spawn T()
                val x = ;;;track;;;(t)
                ;;detrack(x) { it => print(z, it) }
                print(z, x)
            }) ()
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun dd_09_detrack_nested() {
        val out = test("""
            spawn (func () {
                val z = 10
                val T = func () { await(true) ; nil }
                val t = spawn T()
                val x = ;;;track;;;(t)
                ;;detrack(x) { it1 =>
                    ;;detrack(x) { it2 =>
                        ;;print(z, it1, it2)
                        print(z, x, x)
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
            val T = func () {
                set pub = 10
                await(true)
            }
            val t = spawn T()
            print(t.pub)
        """)
        assert(out.contains("10\n")) { out }
    }
    @Test
    fun ee_02_pub() {
        val out = test("""
            val T = func () {
                set pub = 10
                await(true)
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            ;;detrack(x) { v =>
                val v = x
                val y = v.pub
                print(y)
            ;;}
        """)
        assert(out == ("10\n")) { out }
    }
    @Test
    fun ee_03_pub() {
        val out = test("""
            val T = func () {
                set pub = []
                await(true)
            }
            val t = spawn T()
            print(t.pub)
        """)
        assert(out.contains("[]\n")) { out }
    }
    @Test
    fun ee_04_pub() {
        val out = test("""
            val T = func () {
                set pub = [10]
                await(true)
            }
            val ts = tasks()
            spawn T() in ts
            val t = next-tasks(ts)
            ;;detrack(t) { it => print(it.pub) }
                print(t.pub)
        """)
        assert(out.contains("[10]\n")) { out }
    }
    @Test
    fun BUG_ee_05_data_pool_pub() {
        val out = test("""
            data :T = [x,y]
            var ts = tasks()
            spawn (func () {
                set ;;;task.;;;pub = [10,20]
                await(true)
            }) () in ts
            var xxx :T = nil
            loop' {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                print(;;;detrack;;;(xxx).pub.y)   // TODO: detrack needs to return to grammar
            }
        """, true)
        assert(out == "20\n") { out }
    }

    // THROW

    @Test
    fun ee_01_throw() {
        val out = test("""
            val T = func () {
                defer {
                    print(:ok)
                }
                spawn( func () {
                    await(true)
                    throw(:throw)
                })()
                await(true)
            }
            spawn T() in tasks()
            emit(true)
        """)
        assert(out == ":ok\n" +
                " |  anon : (lin 13, col 13) : emit'(:task,nil)\n" +
                " |  anon : (lin 8, col 21) : throw(:throw)\n" +
                " v  throw : :throw\n") { out }
    }
    @Test
    fun ee_02_pool_throw() {
        val out = test(
            """
            spawn (func () {
                catch :ok ;;;(err| err==:ok);;; {
                    spawn func () {
                        await(true)
                        throw(:ok)
                    } ()
                    loop' { await(true) }
                }
            })()
            emit(true)
            print(999)
        """
        )
        assert(out == "999\n") { out }
    }
    @Test
    fun ee_03_pool_term() {
        val out = test(
            """
            var T
            set T = func () {
                await(true)
                throw(nil)
            }
            spawn T()
            spawn T()
            emit( @[] )
        """
        )
        assert(out == " |  anon : (lin 9, col 13) : emit'(:task,@[])\n" +
                " |  anon : (lin 5, col 17) : throw(nil)\n" +
                " v  throw : nil\n") { out }
    }

    // SCOPE

    @Test
    fun ff_01_scope() {
        val out = test("""
            val T = func () { await(true) }
            var x
            do {
                val t = spawn T()
                set x = ;;;track;;;(t)
            }
            print(:ok)
        """)
        //assert(out == " v  anon : (lin 6, col 21) : set throw : cannot expose track outside its task scope\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ff_02_detrack_err() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            val t = ;;;detrack;;;(x) ;;{ it=>it }   ;; err: cannot escape here
            print(type(t))
            emit(true)
            print(status(t))
        """)
        //assert(out == " v  anon : (lin 9, col 24) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 10, col 21) : status(t) : status throw : expected running coroutine or task\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration throw : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape throw : cannot expose task in pool to outer scope\n") { out }
        assert(out == ":exe-task\n" +
                ":terminated\n") { out }
    }
    @Test
    fun ff_02x_detrack_err() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            var y
            val t = ;;detrack(x) { it =>
                set y = x ;;it  ;; ERR: cannot expose it
                ;;;do;;; nil
            ;;}
            emit(true)
            print(status(t))
        """)
        //assert(out == " v  anon : (lin 9, col 24) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 10, col 21) : status(t) : status throw : expected running coroutine or task\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration throw : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 9, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 10, col 21) : set throw : cannot expose task in pool to outer scope\n") { out }
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun ff_02y_detrack_err() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            spawn (func () {
                ;;detrack(x) { it =>
                    set pub = x ;;it  ;; ERR: cannot expose it
                    ;;;do;;; nil
                ;;}
                emit(true) in :global
                print(status(pub))
            }) ()
            print(:nooo)
        """)
        assert(out == ":terminated\n:nooo\n") { out }
        //assert(out == " v  anon : (lin 9, col 24) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 10, col 21) : status(t) : status throw : expected running coroutine or task\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration throw : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 8, col 13) : (spawn (task () { (detrack(x) { it => (set pu...)\n" +
        //        " |  anon : (lin 9, col 28) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 10, col 25) : set throw : cannot expose task in pool to outer scope\n") { out }
    }
    @Test
    fun ff_03_detrack_err() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            print(;;;detrack;;;(x) ;;;{it=>it};;;)
        """)
        //assert(out == " v  anon : (lin 7, col 32) : block escape throw : cannot copy reference out\n") { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun ff_04_detrack_ok() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            print(do { ;;detrack(x) { it =>
                val z = x ;;it
                10
            })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ff_05_track_err() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val x = do {
                val t = spawn T()
                ;;;track;;;(t)
            }
            print(status(x))
        """)
        //assert(out == " v  anon : (lin 5, col 21) : block escape throw : cannot expose track outside its task scope\n") { out }
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun ff_06_track_err() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val x = do {
                val ts = tasks()
                spawn T() in ts 
                next-tasks(ts)
            }
            print(x)
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun ff_07_track_err() {
        val out = test("""
            val T = func () {
                ${AWAIT()}
            }
            val ts = tasks()
            val x = do {
                spawn T() in ts
                next-tasks(ts)
            }
            ;;detrack(x) { it => print(it) }
            print(x)
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun ff_08_tasks() {
        val out = test("""
            do {
                val t = [tasks(), tasks()]
                print(#t)
            }
        """)
        assert(out == "2\n") { out }
    }

    // DETRACK / PUB / SCOPE

    @Test
    fun fg_01_detrack_pub() {
        val out = test("""
            val T = func () {
                set pub = [10]
                await(true) ; nil
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            ;;print(detrack(x) { it => it.pub })
            print(x.pub)
        """)
        //assert(out == "anon : (lin 12, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape throw : reference has immutable scope\n") { out }
    }
    @Test
    fun fg_02_detrack_pub() {
        val out = test("""
            val T = func () {
                set pub = [10]
                await(true) ; nil
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            emit(true)
            ;;print(detrack(x) { it => it.pub }) ;; expose (ok, global func)
            print((x).pub) ;; expose (ok, global func)
        """)
        assert(out == "nil\n") { out }
        //assert(out == " v  anon : (lin 9, col 49) : pub throw : expected task\n") { out }
    }
    @Test
    fun fg_03_detrack_pub() {
        val out = test("""
            val T = func () {
                set pub = [10]
                await(true) ; nil
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            ;;val v = ;;;detrack;;;(x) ;;;{ it => it;;;.pub ;;}
            val v = (x).pub
            emit(true)
            print(v)
        """)
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 8, col 32) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 8, col 32) : block escape throw : reference has immutable scope\n") { out }
    }
    @Test
    fun fg_04_expose_err() {
        val out = test("""
            val x = do {
                val ts = tasks()
                var T = func () {
                    set pub = []
                    await(true) ;; nil
                    nil
                }
                spawn (T) () in ts
                val trk = next-tasks(ts)
                val p = ;;;detrack;;;(trk) ;;{ it => it }
                p
            }
            print(status(x))
        """)
        assert(out == ":terminated\n") { out }
        //assert(out == ":pub\t[]\n" +
        //        " v  anon : (lin 2, col 21) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 11, col 17) : declaration throw : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 11, col 38) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 11, col 38) : block escape throw : cannot expose task in pool to outer scope\n") { out }
    }
    @Test
    fun fg_05_expose() {
        val out = test("""
            var T = func (t) {
                set pub = []
                if t {
                    val p = ;;;detrack;;;(t) .pub ;;{ it => pub(it) }
                } else {
                    nil
                }
                await(true) ;; nil
                nil
            }
            val t = spawn T ()
            spawn T (;;;track;;;(t))
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
        //        " |  anon : (lin 5, col 40) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 5, col 40) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 13, col 19) : T(track(t))\n" +
        //        "anon : (lin 5, col 21) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
        //        " |  anon : (lin 5, col 40) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 5, col 40) : block escape throw : reference has immutable scope\n") { out }
        //assert(out == " |  anon : (lin 13, col 13) : (spawn T(track(t)))\n" +
        //        " v  anon : (lin 5, col 21) : declaration throw : cannot hold alien reference\n") { out }
    }
    @Test
    fun fg_06_expose() {
        val out = test("""
            val f = func' (t) {
                print(t)
            }
            val T = func () {
                set pub = []
                await(true) ; nil
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
            print(:ok)
        """)
        assert(out == "[]\n:ok\n") { out }
    }
    @Test
    fun fg_06_expose_xxx() {
        val out = test("""
            val f = func' (t) {
                print(t)
            }
            val T = func () {
                set pub = []
                await(true) ;; nil
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            ;;detrack(x) { t => f(pub(t)) }
            f(x.pub)
            print(:ok)
        """)
        assert(out == "[]\n:ok\n") { out }
    }
    @Test
    fun fg_07_throw_track() {
        val out = test("""
            val T = func () {
                set pub = 10
                await(true) ; nil
            }
            val ts = tasks()
            val t = catch ;;;( it|true);;; {
                spawn T() in ts
                do {
                    val u = next-tasks(ts)
                    throw(:x,;;;drop;;;(u))
                }
            }
            ;;detrack(t) { it => print(it.pub) }
            print(t.pub)
        """)
        //assert(out == ":ok\n") { out }
        assert(out == "10\n") { out }
        //assert(out.contains("TODO: throw inside throw")) { out }
    }
    @Test
    fun fg_08_throw_track() {
        val out = test("""
            val T = func () {
                await(true) ; nil
            }
            val x = do {
                val ts = tasks()
                spawn T() in ts
                do {
                    val t = next-tasks(ts)
                    throw(:x,;;;drop;;;(t))
                    nil
                }
            }
            print(status(x))
        """)
        assert(out.contains(" |  anon : (lin 10, col 21) : throw(:x,t)\n" +
                " v  throw : exe-task: 0x")) { out }
        //assert(out.contains("TODO: throw inside throw")) { out }
        //assert(out == (" v  anon : (lin 5, col 21) : block escape throw : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun fg_09_throw_track() {
        val out = test("""
            val T = func () {
                await(true) ; nil
            }
            val ts = tasks()
            catch ;;;( it|true);;; {
                spawn T() in ts
                do {
                    val t = next-tasks(ts)
                    throw(:x,;;;drop;;;(t))
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // NEXT

    @Test
    fun hh_00_next() {
        val out = test("""
            val T = func () {
                nil
            }
            val ts = tasks()
            spawn T() in ts
            print(next-tasks(ts))
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun hh_00x_next() {
        val out = test("""
            val T = func () {
                await(true)
            }
            val ts = tasks()
            val t = spawn T() in ts
            print(next-tasks(ts))
            print(next-tasks(ts,t))
        """
        )
        assert(out.contains(Regex("exe-task: 0x.*\nnil\n"))) { out }
    }
    @Test
    fun hh_01_next() {
        val out = test("""
            val T = func () {
                await(true)
            }
            val ts = tasks()
            print(next-tasks(ts))
            print(next-tasks(ts, nil))
            print(next-tasks(ts, :err))
        """
        )
        //assert(out == "nil\n" +
        //        "nil\n" +
        //        " v  anon : (lin 8, col 21) : next-tasks(ts,:err) : next-tasks throw : expected task-in-pool track\n") { out }
        assert(out == "nil\n" +
                "nil\n" +
                " |  anon : (lin 8, col 21) : next-tasks(ts,:err)\n" +
                " v  throw : expected task\n") { out }
    }
    @Test
    fun hh_02_next() {
        val out = test("""
            val T = func () {
                await(true)
            }
            val ts = tasks()
            spawn T() in ts
            spawn T() in ts
            val x1 = next-tasks(ts)
            val x2 = next-tasks(ts, x1)
            val x3 = next-tasks(ts, x2)
            print(x1 /= nil)
            print(x2 /= nil)
            print(x1 /= x2)
            print(x3 == nil)
            print(x2)
        """
        )
        assert(out.contains("true\ntrue\ntrue\ntrue\nexe-task: 0x")) { out }
    }
    @Test
    fun hh_03_next() {
        val out = test("""
            val T = func () {
                await(true)
            }
            val ts = tasks()
            spawn T() in ts
            val x = next-tasks(ts)
            emit(true)
            print(next-tasks(ts, x))
        """
        )
        //assert(out == " v  anon : (lin 9, col 13) : next-tasks(ts,x) : next-tasks throw : expected task-in-pool track\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun hh_04_next() {
        val out = test("""
            val T = func (v) {
                ${AWAIT("it == v")}
            }
            val ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            val x1 = next-tasks(ts)
            val x2 = next-tasks(ts, x1)
            emit(1)
            print(next-tasks(ts, x1) == x2)
        """
        )
        //assert(out == " v  anon : (lin 11, col 13) : next-tasks(ts,x1) : next-tasks throw : expected task-in-pool track\n") { out }
        assert(out == "true\n") { out }
    }
    @Test
    fun hh_05_next() {
        val out = test("""
            val T = func (v) {
                set pub = [v]
                await(true)
            }
            val ts = tasks()
            spawn T(10) in ts
            val x1 = next-tasks(ts)
            ;;val v = detrack(x1) { it => print(it.pub) ; it.pub }
            val v = (x1).pub
            print(v)
        """
        )
        assert(out == "[10]\n") { out }
        //assert(out == "[10]\n" +
        //        " |  anon : (lin 9, col 33) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 9, col 33) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == "[10]\n" +
        //        " |  anon : (lin 9, col 33) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 9, col 33) : block escape throw : reference has immutable scope\n") { out }
    }
    @Test
    fun hh_06_pool_terminate() {
        val out = test("""
            do {
                var ts
                set ts = tasks()
                var T
                set T = func (v) {
                    print(v)
                }
                spawn T(1) in ts
                enclose' :break {
                    loop' {
                        val t = next-tasks(ts)
                        if (if t { false } else { true }) {
                            escape(:break,nil)
                        } else {nil}
                        throw(1)    ;; never reached
                    }
                }
            }
            emit(2)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_07_pool_err() {
        val out = test("""
            print(next-tasks(nil))
        """)
        assert(out == " |  anon : (lin 2, col 21) : next-tasks(nil)\n" +
                " v  throw : expected tasks\n") { out }
    }
    @Test
    fun hh_08_pool_term() {
        val out = test("""
            var T = func () {
                await(true)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' {
                    ;;val' z = next-tasks(ts, xxx)
                    ;;dump(z)
                    ;;set xxx = z
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    print(1)
                    emit (1)
                }
            }
            print(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun hh_09_pool_term() {
        val out = test("""
            var T
            set T = func () {
                await(true)
                await(true)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    print(1)
                    emit(1)
                    var yyy = nil
                    enclose' :break {
                        loop' {
                            set yyy = next-tasks(ts, yyy)
                            if (if yyy { false } else { true }) {
                                escape(:break,nil)
                            } else {nil}
                            print(2)
                        }
                    }
                }
            }
            print(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun hh_10_pool_plain() {
        val out = test("""
            var T = func () { await(true) }
            var ts = tasks()
            spawn T() in ts
            var yyy
            var xxx = nil
            enclose' :break {
                loop' {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    set yyy = xxx
                }
            }
            print(status(;;;detrack;;;(yyy)))
        """)
        //assert(out == "anon : (lin 9, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 7, col 21) : set throw : incompatible scopes\n:throw\n") { out }
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun hh_11_pool_move() {
        val out = test("""
            var T = func () { await(true) }
            var ts = tasks()
            spawn T() in ts
            var yyy
            var xxx = nil
            enclose' :break {
                loop' {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    set yyy = ;;;move;;;(xxx)
                }
            }
            print(status(;;;detrack;;;(yyy)))
        """)
        //assert(out == "anon : (lin 9, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set throw : incompatible scopes\n:throw\n") { out }
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun TODO_hh_12_pool_check() {   // copy task
        val out = test("""
            var T = func () { await(true) }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            loop ;;;in :tasks ts, xxx;;; {
                set xxx = next-tasks(ts, xxx)
                break if (if xxx { false } else { true })
                print(;;;detrack;;;(xxx) == ;;;detrack;;;(xxx))
            }
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun TODO_hh_13_pool_err_scope() {   // copy task
        val out = test("""
            var T
            set T = func () { await(true) }
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
            emit(true)
            print(;;;detrack;;;(yyy))
        """, true)
        //assert(out == "anon : (lin 9, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set throw : incompatible scopes\n:throw\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun hh_14_pool_bcast() {
        val out = test("""
            var T = func () { await(true); print(:ok) }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' ;;;in :tasks ts, xxx;;; {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    emit(true) in ;;;detrack;;;(xxx)
                }
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun hh_15_pool_err_scope() {
        val out = test(
            """
            var T
            set T = func () { await(true) }
            var ts
            set ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' ;;;in :tasks ts, xxx;;; {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    var yyy
                    var zzz = nil
                    enclose' :break {
                        loop' ;;;in :tasks ts, zzz;;; {
                            set zzz = next-tasks(ts, zzz)
                            if (if zzz { false } else { true }) {
                                escape(:break,nil)
                            } else {nil}
                            set yyy = ;;;copy;;;(zzz)
                            print(status(;;;detrack;;;(yyy)))
                        }
                    }
                    print(status(;;;detrack;;;(yyy)))
                    set yyy = xxx
                }
            }
        """
        )
        //assert(out == "anon : (lin 10, col 25) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 10, col 25) : set throw : incompatible scopes\n:throw\n") { out }
        assert(out == ":yielded\n:yielded\n") { out }
    }
    @Test
    fun hh_16_pool_scope() {
        val out = test(
            """
            var T
            set T = func () { await(true) }
            var ts
            set ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' ;;;in :tasks ts, xxx;;; {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    var yyy
                    var zzz = nil
                    enclose' :break {
                        loop' ;;;in :tasks ts, zzz;;; {
                            set zzz = next-tasks(ts, zzz)
                            if (if zzz { false } else { true }) {
                                escape(:break,nil)
                            } else {nil}
                            ;;;do;;; nil
                        }
                    }
                    set yyy = xxx
                    ;;pass nil ;; otherwise scope err for yyy/xxx
                }
            }
            print(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_17_pool_scope() {
        val out = test(
            """
            var T
            set T = func () { await(true) }
            var ts
            set ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' ;;;in :tasks ts, xxx;;; {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    ;;;do;;; xxx
                }
            }
            print(1)
        """
        )
        //assert(out == "anon : (lin 7, col 13) : set throw : incompatible scopes\n:throw\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_18_ff_pool() {
        val out = test("""
            var ts
            set ts = tasks()
            print(type(ts))
            var T
            set T = func (v) {
                set pub = v
                val v' = await(true)
            }
            spawn T(1) in ts
            spawn T(2) in ts
            
            var t1 = nil
            enclose' :break {
                loop' ;;;in :tasks ts, t1;;; {
                    set t1 = next-tasks(ts, t1)
                    if (if t1 { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    var t2 = nil
                    enclose' :break {
                        loop' ;;;in :tasks ts, t2;;; {
                            set t2 = next-tasks(ts, t2)
                            if (if t2 { false } else { true }) {
                                escape(:break,nil)
                            } else {nil}
                            print(;;;detrack;;;(t1).pub, ;;;detrack;;;(t2).pub)
                        }
                    }
                }
            }
            emit( 2 )
        """)
        assert(out == ":tasks\n1\t1\n1\t2\n2\t1\n2\t2\n") { out }
    }
    @Test
    fun hh_19_pub_pool() {
        val out = test("""
            var T
            set T = func () {
                set pub = [10]
                await(true)
            }
            var ts
            set ts = tasks()
            spawn T() in ts
            var t
            enclose' :break {
                loop' ;;;in :tasks ts, t;;; {
                    set t = next-tasks(ts,t)
                    if (if t { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    print(;;;detrack;;;(t).pub[0])
                }
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_20_pool_term() {
        val out = test("""
            var T = func () {
                spawn func () {
                    await(true)
                }()
                await(true)
            }
            var ts = tasks()
            spawn T() in ts 
            spawn T() in ts
            spawn func () {
                var xxx = nil
                enclose' :break {
                    loop' ;;;in :tasks ts, xxx;;; {
                        set xxx = next-tasks(ts, xxx)
                        if (if xxx { false } else { true }) {
                            escape(:break,nil)
                        } else {nil}
                        print(1)
                        emit (1)
                    }
                }
            } ()
            print(2)
        """)
        //assert(out == "1\n2\n") { out }
        assert(out == "1\n1\n2\n") { out }
    }
    @Test
    fun hh_21_pool_val() {
        val out = test("""
            val T = func () {
                await(true)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' ;;;in :tasks ts, xxx;;; {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    val tsk = ;;;detrack;;;(xxx)
                    emit (nil)
                }
            }
            print(:ok)
        """)
        //assert(out == "anon : (lin 8, col 17) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun hh_22_ff_pool_val() {
        val out = test("""
            val T = func () {
                await(true)
            }
            var ts = tasks()
            spawn T() in ts
            var trk = nil
            enclose' :break {
                loop' ;;;in :tasks ts, trk;;; {
                    set trk = next-tasks(ts, trk)
                    if (if trk { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    val      tsk1 = ;;;detrack;;;(trk)
                    val ;;;:tmp;;; tsk2 = ;;;detrack;;;(trk)
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 8, col 17) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
    }
    @Test
    fun hh_23_pool_scope() {
        val out = test("""
            var T = func () { await(true) }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' ;;;in :tasks ts, xxx;;; {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    do {
                        val zzz = ;;;detrack;;;(xxx)
                        nil
                    }
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun hh_24_pub_pool_err() {
        val out = test("""
            var T
            set T = func () {
                set pub = [10]
                await(true)
            }
            var ts
            set ts = tasks()
            spawn T() in ts
            var x
            var t
            enclose' :break {
                loop' ;;;in :tasks ts, t;;; {
                    set t = next-tasks(ts,t)
                    if (if t { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    set x = ;;;detrack;;;(t).pub   ;; TODO: incompatible scope
                }
            }
            print(999)
        """)
        assert(out == "999\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
        //assert(out == "anon : (lin 12, col 21) : set throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun hh_25_pub_pool_err() {
        val out = test("""
            var T = func () {
                set ;;;task.;;;pub = [10]
                await(true)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    var x = ;;;detrack;;;(xxx).pub
                    emit( nil )in ;;;detrack;;;(xxx)
                    print(x)
                }
            }
            print(999)
        """)
        assert(out == "[10]\n999\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
        //assert(out == "anon : (lin 9, col 17) : declaration throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun hh_26_pub_pool_err() {
        val out = test("""
            var T = func () {
                set ;;;task.;;;pub = [10]
                await(true)
            }
            var ts = tasks()
            spawn T() in ts
            var xxx = nil
            enclose' :break {
                loop' {
                    set xxx = next-tasks(ts, xxx)
                    if (if xxx { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    var f = func' (tt) {
                        var x = ;;;detrack;;;(tt).pub
                        emit(true) in ;;;detrack;;;(tt)
                        print(x)
                    }
                    f(xxx)
                }
            }
            print(999)
        """)
        assert(out == "[10]\n999\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
        //assert(out == "anon : (lin 14, col 17) : f(t)\n" +
        //        "anon : (lin 10, col 21) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
    }
    @Test
    fun hh_27_pool_scope() {
        val out = test("""
            var T = func () {
                await(true)
            }
            do {
                var ts = tasks()
                val x = do {
                    val t = spawn T() in ts
                    dump(t)
                    t
                }
                print(x)
            }
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }

    // ABORTION

    @Test
    fun ii_01_self() {
        val out = test("""
            spawn( func () {
                val t = spawn (func () {
                    await(true) ; nil ;;thus { it => nil }
                } )()
                yield (nil) ; nil ;;thus { it => nil }
            }) ()
            emit(true)
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_02a_self() {
        val out = test("""
            spawn (func () {
                val t = spawn( func () {
                    await(true) ; nil ;;thus { it => nil }
                }) () in tasks()
                yield (nil) ; nil ;;thus { it => nil }
            } )()
            emit(true)
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_02b_self() {
        val out = test("""
            spawn (func () {
                val ts = tasks()
                val t = spawn( func () {
                    await(true) ; nil ;;thus { it => nil }
                }) () in ts
                yield (nil) ; nil ;;thus { it => nil }
            } )()
            emit(true)
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_02c_self() {
        val out = test("""
            val ts = tasks()
            spawn (func () {
                val t = spawn( func () {
                    await(true) ; nil ;;thus { it => nil }
                }) () in ts
                yield (nil) ; nil ;;thus { it => nil }
            } )()
            emit(true)
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_03_self() {
        val out = test("""
            val T = func () {
                spawn (func () {
                    await(true)
                }) ()
                await(true)
                nil
            }
            val ts = tasks()
            spawn T() in ts
            emit(true)
            print(:ok)
       """)
        assert(out == ":ok\n") { out }
    }

    // TRACK / COLLECTION

    @Test
    fun jj_01_tracks() {
        val out = test("""
            val T = func () { await(true) ; nil }
            val ts = tasks()
            spawn T() in ts
            val vec = #[]
            val t = next-tasks(ts,nil)
            set vec[#vec] = t
            print(vec)
        """)
        //assert(out.contains("#[track: 0x")) { out }
        assert(out.contains("#[exe-task: 0x")) { out }
    }
    @Test
    fun jj_02_tracks() {
        val out = test("""
            val f = func' (trk) {
                ;;print(detrack(trk) { it => status(it) })
                print(status(trk))
            }
            val T = func () { await(true) ; nil }
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
        //        " v  anon : (lin 7, col 22) : block escape throw : reference has immutable scope\n")) { out }
        //assert(out.contains(":yielded\n" +
        //        " v  anon : (lin 7, col 22) : block escape throw : cannot expose track outside its task scope\n")) { out }
    }
    @Test
    fun jj_03_tracks() {
        val out = test("""
            val T = func () { await(true) ; nil }
            do {
                val ts = tasks()
                spawn T() in ts
                do {
                    val vec = #[]
                    var t = nil
                    enclose' :break {
                        loop' {
                            set t = next-tasks(ts,t)
                            if t==nil {
                                escape(:break,nil)
                            } else {nil}
                            set vec[#vec] = t
                        }
                    }
                    print(vec)
                }
            }
        """)
        //assert(out == "anon : (lin 9, col 29) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out.contains("#[track: 0x")) { out }
        assert(out.contains("#[exe-task: 0x")) { out }
        //assert(out == (" v  anon : (lin 12, col 29) : store throw : cannot hold reference to track or task in pool\n")) { out }
    }

    // TASKS / POOL / SIZE / MEM

    @Test
    fun kk_01_pool() {
        val out = test("""
            val T = func () {
                print(:1)
                await(true)
                print(:2)
            }
            val ts = tasks(1)
            var ok = false
            enclose' :break {
                loop' {
                    spawn T() in ts
                    val t = next-tasks(ts)
                    ;;print(t)
                    emit(true)
                    if ok {
                        escape(:break,nil)
                    } else {nil}
                    set ok = true
                }
            }
        """)
        assert(out.contains(":1\n:2\n:1\n:2\n")) { out }
    }
    @Test
    fun kk_02_pool() {
        val out = test("""
            val T = func (v) {
                print(:ok)
                enclose' :break {
                    loop' {
                        val it = await(true)
                        if {{==}}(it,:FIN) {
                            escape(:break,nil)
                        } else {nil}
                    }
                }
            }
            val ts = tasks(1)
            spawn T() in ts
            spawn (func () {
                loop' {
                    enclose' :break {
                        loop' {
                            val it = await(true)
                            if it==:CHK {
                                escape(:break,nil)
                            } else {nil}
                        }
                        val xxx = #[next-tasks(ts)]
                    }
                }
            }) ()
            spawn T() in ts
            emit(:CHK)
            emit(:FIN)
            spawn T() in ts
        """)
        assert(out.contains(":ok\n:ok\n")) { out }
    }

    // ORIGINAL

    @Test
    fun oo_02_track_err() {
        val out = test("""
            var T = func (v) {
                await(true) ; nil
            }
            var x
            var ts = tasks()
            spawn T(1) in ts
            do {
                val t = next-tasks(ts)
                set x = ;;;track;;;(t)
            }
            print(:ok)
        """)
        //assert(out == " v  anon : (lin 10, col 25) : track(t) : track throw : expected task\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun oo_03_track_err() {
        val out = test("""
            var T
            set T = func (v) {
                set pub = [v]
                await(true) ; nil
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
            print(:ok)
        """)
        //assert(out == "anon : (lin 13, col 25) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 13, col 25) : set throw : incompatible scopes\n:throw\n") { out }
        //assert(out == " v  anon : (lin 14, col 25) : set throw : cannot assign reference to outer scope\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun BUG_oo_04_track() {
        val out = test("""
            var T = func (v) {
                await(true) ; nil
            }
            var ts = tasks()
            spawn T(1) in ts
            val x = do {
                val t = next-tasks(ts)
                ;;detrack(t) { it => it } ;; ERR: cannot expose
                nil
            }
            print(x)
        """)
        //assert(out == " v  anon : (lin 7, col 13) : declaration throw : cannot expose task-in-pool reference\n") { out }
        //assert(out.contains("exe-task: 0x")) { out }
        //assert(out == (" |  anon : (lin 9, col 28) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 9, col 28) : block escape throw : cannot expose reference to task in pool\n")) { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun oo_05_xceu() {
        val out = test("""
            var T
            set T = func (pos) {
                await(true)
                print(pos)
            }
            spawn (func () {
                var ts
                set ts = tasks()
                do {
                    spawn T([]) in ts  ;; pass [] to ts
                }
                await(true)
                await(true)
            })()
            emit (nil)
        """)
        assert(out == "[]\n") { out }
    }

    // ORIGINAL / TRACK / DETRACK

    @Test
    fun op_00_track() {
        val out = test("""
            var T
            set T = func () {
                set pub = [10]
                await(true) ; nil
            }
            var t = spawn T ()
            var x = ;;;track;;;(t)
            print(;;;detrack;;;(x))
            emit( nil )
            print(;;;detrack;;;(x))
        """)
        //assert(out == "true\nfalse\n") { out }
        assert(out.contains(Regex("exe-task: 0x.*\nexe-task: 0x.*\n"))) { out }
    }
    @Test
    fun op_01_track() {
        val out = test("""
            val T = func () {
                await(true) ;; nil
                set pub = 10
                await(true) ; nil
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            ;;detrack(x) { it => print(it.pub) } 
            print(x.pub) 
            emit(true)
            ;;detrack(x) { it => print(it.pub) } 
            print(x.pub) 
        """)
        assert(out == "nil\n10\n") { out }
    }
    @Test
    fun op_02_track() {
        val out = test("""
            var T
            set T = func () {
                set pub = [10]
                await(true) ; nil
            }
            var t = spawn T ()
            var x = ;;;track;;;(t)
            ;;detrack(x) { it => print(pub(it)[0]) }
            print(x.pub[0])
            print(status(x))
            ;;print(detrack(x))
            emit( nil )
            ;;print(detrack(x) { it => 999 })
            print(status(x))
        """)
        //assert(out == "10\ntrue\nnil\n") { out }
        assert(out == "10\n:yielded\n:terminated\n") { out }
    }
    @Test
    fun op_02x_track() {
        val out = test("""
            val T = func () {
                await(true)
            }
            var t = spawn T ()
            val x = ;;;track;;;(t)
            print(status(x))
            ;;print(detrack''(x))
            ;;detrack(x) { it => nil }
            ;;print(detrack''(x))
            print(status(x))
        """)
        //assert(out == "true\ntrue\n") { out }
        assert(out == ":yielded\n:yielded\n") { out }
    }
    @Test
    fun op_03_track_err() {
        val out = test("""
            var T
            set T = func () {
                set pub = [10]
                await(true) ; nil
            }
            var x
            do {
                var t = spawn (T) ()
                set x = ;;;track;;;(t)         ;; scope x < t
                ;;print(detrack(x).pub[0])
            }
            ;;print(status(detrack(x)))
            ;;print(x)
            print(:ok)
        """)
        //assert(out.contains("10\n:terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 10, col 21) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == (" v  anon : (lin 10, col 21) : set throw : cannot expose track outside its task scope\n")) { out }
        assert(out == (":ok\n")) { out }
    }
    @Test
    fun op_04_track() {
        val out = test("""
            var T = func () {
                set pub = [10]
                ${AWAIT("it == :evt")}
            }
            var t = spawn T()
            var x = ;;;track;;;(t)
            spawn( func () {
                catch :par-or ;;;( err|err==:par-or );;; {
                    spawn( func () {
                        await(true) ;;thus { it => it==t }
                        throw(:par-or)
                    }) ()
                    ;;print(detrack(x) { it => it.pub[0] })
                    print((x).pub[0])
                    emit(true) in t
                    ;;print(detrack(x) { it => it.pub[0] })
                    print((x).pub[0])
                    emit(:evt) in t
                    print(999)
                }
                ;;print(detrack(x) { it => if it {999} else {nil} })
                print(status(x))
                nil
            })()
            print(:ok)
        """)
        assert(out == "10\n10\n:terminated\n:ok\n") { out }
    }
    @Test
    fun op_05_detrack_err() {
        val out = test("""
            val T = func () {
                await(true) ; nil
            }
            val t1 = spawn T()
            val r1 = ;;;track;;;(t1)
            ;;detrack(r1) { x1 =>
                val x1 = r1
                ;;print(t1, r1, x1, status(t1))
                print(status(t1))
                emit( nil )
                ;;print(t1, r1, x1, status(t1))
                print(status(t1)) ;; never reached
            ;;}
            print(:ok)
        """)
        //assert(out == ":yielded\n:ok\n") { out }
        assert(out == ":yielded\n:terminated\n:ok\n") { out }
        //assert(out == "anon : (lin 7, col 13) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " v  anon : (lin 7, col 34) : block escape throw : cannot copy reference out\n") { out }
    }
    @Test
    fun op_06_track_scope() {
        val out = test("""
            val T = func () {
                await(true) ; nil
            }
            val t = spawn T()
            val y = do {
                val x = ;;;track;;;(t)
                x
            }
            print(y)
        """)
        //assert(out == " v  anon : (lin 6, col 21) : block escape throw : cannot copy reference out\n") { out }
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun op_07_track_scope() {
        val out = test("""
            val T = func () {
                set pub = 1
                await(true) ; nil
            }
            val t = spawn T()
            val y = do {
                ;;;track;;;(t)
            }
            ;;detrack(y) { it => print(pub(it)) }
            print(y.pub)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun op_08_track_scope() {
        val out = test("""
            val T = func () {
                set pub = 1
                await(true) ; nil
            }
            val y = do {
                val t = spawn T()
                ;;;track;;;(t)
            }
            ;;detrack(y) { it => print(it.pub) }
            print(y.pub)
        """)
        assert(out == "1\n") { out }
        //assert(out == " v  anon : (lin 6, col 21) : block escape throw : cannot expose track outside its task scope\n") { out }
    }
    @Test
    fun BUG_op_09_track_throw() {
        // aborted trask in pool does not bcast itself to clear track
        val out = test("""
            val T = func () {
                defer {
                    print(:ok)
                }
                spawn( func () {
                    await(true) ; nil
                    print(:before-throw)
                    throw(:throw)               ;; 2. kill task
                })()
                await(true) ; nil
            }
            val ts = tasks()
            spawn T() in ts
            val t = next-tasks(ts)
            catch (it=>true) {
                emit(true)                  ;; 1. awake task
            }
            ;;`ceu_gc_collect();`
            detrack(t) { it =>
                print(:it, it, status(it))    ;; 3. should not execute
            }
        """)
        assert(!out.contains(":it")) { out }
    }
    @Test
    fun op_10_track() {
        val out = test("""
            var T = func (v) {
                set pub = [v]
                await(true) ;;{ as it => nil }
            }
            var x
            var ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            var t
            enclose' :break {
                loop' ;;;in :tasks ts, t;;; {
                    set t = next-tasks(ts,t)
                    if (if t { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    set x = ;;;copy;;;(t)
                }
            }
            print(;;;detrack;;;(x).pub[0])   ;; 2
            emit (nil)
            print(;;;detrack;;;status(x))   ;; nil
        """)
        //assert(out == "2\nnil\n") { out }
        assert(out == "2\n:terminated\n") { out }
    }
    @Test
    fun op_11_track() {
        val out = test("""
            var T
            set T = func (v) {
                set ;;;task.;;;pub = [v]
                await(true)
            }
            var x
            var ts
            set ts = tasks()
            do {
                spawn T(1) in ts
                spawn T(2) in ts
                var t
                enclose' :break {
                    loop' ;;;in :tasks ts, t;;; {
                        set t = next-tasks(ts,t)
                        if (if t { false } else { true }) {
                            escape(:break,nil)
                        } else {nil}
                        set x = ;;;copy;;;(t)    ;; track(t) up_hold in
                    }
                }
                print(;;;detrack;;;(x).pub[0])   ;; 2
                emit (nil)
                print(;;;detrack;;;status(x))   ;; nil
            }
        """)
        assert(out == "2\n:terminated\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set throw : incompatible scopes\n") { out }
    }
    @Test
    fun op_12_track_throw() {
        val out = test("""
            var T
            set T = func (v) {
                await(true)
            }
            var ts
            set ts = tasks()
            spawn T(1) in ts
            var x
            set x = catch ;;;(_|true);;; {
                var t
                enclose' :break {
                    loop' ;;;in :tasks ts, t;;; {
                        set t = next-tasks(ts,t)
                        if (if t { false } else { true }) {
                            escape(:break,nil)
                        } else {nil}
                        throw(:x,;;;copy;;;(t))
                    }
                }
            }
            emit (nil)
            print(;;;detrack;;;status(x))   ;; nil
        """)
        //assert(out == "nil\n") { out }
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun op_13_track_throw() {
        val out = test("""
            var T
            set T = func (v) {
                set ;;;task.;;;pub = [v]
                await(true)
            }
            var ts
            set ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            var x
            set x = catch ;;;(_|true);;; {
                var t
                enclose' :break {
                    loop' ;;;in :tasks ts, t;;; {
                        set t = next-tasks(ts,t)
                        if (if t { false } else { true }) {
                            escape(:break,nil)
                        } else {nil}
                        throw(:ok,;;;copy;;;(t))
                    }
                }
            }
            print(;;;detrack;;;(x).pub[0])   ;; 1
            print(status(;;;detrack;;;(x)))   ;; :yielded
            emit (nil)
            print(;;;detrack;;;status(x))   ;; nil
        """)
        //assert(out == "1\n:yielded\nnil\n") { out }
        assert(out == "1\n:yielded\n:terminated\n") { out }
    }
    @Test
    fun op_14_track_simplify() {
        val out = test("""
            var T = func (v) {
                await(true)
            }
            var ts = tasks()
            spawn T(1) in ts
            spawn T(2) in ts
            var x
            var t
            enclose' :break {
                loop' ;;;in :tasks ts, t;;; {
                    set t = next-tasks(ts,t)
                    if (if t { false } else { true }) {
                        escape(:break,nil)
                    } else {nil}
                    set x = ;;;copy;;;(t)
                }
            }
            emit( nil )
            print(;;;detrack;;;status(x))   ;; nil
        """)
        //assert(out == "nil\n") { out }
        assert(out == ":terminated\n") { out }
    }

    // ZZ / ALL

    @Test
    fun zz_01_all() {
        val out = test("""
            val T = func () {
                await(true)
            }
            spawn (func () {
                val ts = tasks(5)
                do {
                    spawn T() in ts
                }
                await(true) ;;thus { it =>
                    print(nil)
                ;;}
            }) ()
            emit(true)
       """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun zz_02_all() {
        val out = test("""
            val iter-tasks = func' (itr) {
                set itr[2] = next-tasks(itr[1],itr[2])
                itr[2]
            }
            val T = func () {
                await(true)
            }
            val ts = tasks()
            spawn T(nil) in ts
            val x = do {
                val tt = [nil]
                enclose' :break {
                    loop' {
                        set tt[0] = next-tasks(ts,tt[0])
                        val t = tt[0]
                        if {{==}}(t,nil) {
                            escape(:break,false)
                        } else {nil}
                        escape(:break,(t)) ;;if true
                    }
                }
            }
            print(x)
       """)
        //assert(out == "nil\n") { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun zz_03_all() {
        val out = test("""
            val x = enclose' :break {
                val tt = [nil]
                loop' {
                    set tt[0] = @[]
                    val t = tt[0]
                    if {{==}}(t,nil) {
                        escape(:break,false)
                    } else {nil}
                    escape(:break,(t)) ;;if true
                }
            }
            print(x)
       """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun zz_df_03_bcast_throw() {
        DEBUG = true
        val out = test("""
            spawn (func () {
                spawn (func () {
                    await(true)
                    await(true)
                    print(:ok)
                    throw(:XXX)
                }) ()
                spawn (func () {
                    await(true)
                    emit (nil) in :global
                }) ()
                loop' {
                    await(true)
                }
            }) ()            
            emit(true)
        """)
        assert(out == ":ok\n" +
                " |  anon : (lin 17, col 13) : emit'(:task,nil)\n" +
                " |  anon : (lin 11, col 21) : emit'(:global,nil)\n" +
                " |  anon : (lin 7, col 21) : throw(:XXX)\n" +
                " v  throw : :XXX\n") { out }
    }
    @Test
    fun zz_05_99() {
        val out = test("""
            (spawn (func () {
                do {
                    spawn (func () {
                        await(true)
                    }) ()
                    loop' {
                        await(true)
                    }
                }
            })())
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
}
