package tst_04

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_04 {
    // TASK

    @Test
    fun aa_01_task() {
        val out = test("""
            val T = task (v) { nil }
            println(T)
        """)
        assert(out.contains("task: 0x")) { out }
    }
    @Test
    fun aa_02_task_equal() {
        val out = test("""
            val T1 = task (v) { nil }
            val T2 = task (v) { nil }
            println(T1 == T1)
            println(T1 == T2)
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun aa_03_task_err() {
        val out = test("""
            val T = task (v) { nil }
            coroutine(T)
        """)
        assert(out == " v  anon : (lin 3, col 13) : coroutine(T) : coroutine error : expected coro\n") { out }
    }
    @Test
    fun aa_04_task_err() {
        val out = test("""
            val T = task (v) { nil }
            T()
        """)
        assert(out == " v  anon : (lin 3, col 13) : call error : expected function\n") { out }
    }

    // SPAWN

    @Test
    fun bb_01_spawn() {
        val out = test("""
            val T = task (v) { nil }
            val t = spawn T()
            println(t)
        """)
        assert(out.contains("x-task: 0x")) { out }
    }
    @Test
    fun bb_02_resume_err() {
        val out = test("""
            val T = task (v) { nil }
            val t = spawn T()
            resume t()
        """)
        assert(out == " v  anon : (lin 4, col 13) : resume error : expected yielded coro\n") { out }
    }
    @Test
    fun bb_03_spawn() {
        val out = test("""
            spawn (task (v1) {
                println(v1)
            }) (1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_04_spawn_err() {
        val out = test("""
            spawn nil()
        """)
        assert(out == " v  anon : (lin 2, col 23) : spawn error : expected task\n") { out }
    }

    // SCOPE

    @Test
    fun cc_01_scope() {
        val out = test("""
            var x
            set x = do {
                spawn (task() {nil}) ()
            }
            println(2)
        """)
        //assert(out == "anon : (lin 3, col 21) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out == "2\n") { out }
    }
    @Test
    fun cc_02_scope() {
        val out = test("""
            var t
            set t = task () {
                nil
            }
            var co
            set co = if true { spawn t() } else { nil }
            println(:ok)
        """)
        //assert(out == "anon : (lin 7, col 30) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cc_03_scope() {
        val out = test("""
            var t
            set t = task () { nil }
            var f
            set f = func () {
                spawn t()
            }
            f()
            println(:ok)
        """)
        //assert(out == "anon : (lin 8, col 13) : f()\n" +
        //        "anon : (lin 5, col 29) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cc_04_scope() {
        val out = test("""
            var x
            set x = do {
                spawn (task() {println(1)}) ()
                nil
            }
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun cc_05_scope() {
        val out = test("""
            var T
            set T = task (v) {
                nil ;;println(v)
            }
            var t
            set t = do {
                var v
                set v = 10
                spawn T(v)  ;; ERR: coro in nested scope cannot escape
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 7, col 21) : block escape error : incompatible scopes\n:error\n") { out }
    }

    // BROADCAST

    @Test
    fun dd_01_bcast() {
        val out = test("""
            println(broadcast in :global, 1)
        """)
        assert(out == "nil\n") { out }
    }

    // THROW

    @Test
    fun ee_01_throw() {
        val out = test("""
            catch :xxx {
                spawn task () {
                    yield(nil)
                }()
                spawn task () {
                    throw(:xxx)
                }()
            }
            println(10)
        """)
        assert(out == "10\n") { out }
    }

    // MOVE

    // ORIG

    @Test
    fun jj_00_spawn() {
        val out = test("""
            var T
            set T = task (x,y) {
                println(x,y)
            }
            spawn T(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun jj_02_spawn_err() {
        val out = test("""
            spawn (func () {nil}) ()
        """)
        assert(out == " v  anon : (lin 2, col 36) : spawn error : expected task\n") { out }
    }
    @Test
    fun jj_03_spawn_err() {
        val out = test("""
            spawn (func () {nil})
        """)
        assert(out == "anon : (lin 3, col 9) : invalid spawn : expected call\n") { out }
    }
}
