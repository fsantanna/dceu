package tst_04

import dceu.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_04 {
    // TASK

    @Before
    fun init() {
        //DEBUG = false
    }

    @Test
    fun aa_01_task() {
        val out = test(
            """
            val T = task (v) { nil }
            println(T)
        """
        )
        assert(out.contains("task: 0x")) { out }
    }
    @Test
    fun aa_02_task_equal() {
        val out = test(
            """
            val T1 = task (v) { nil }
            val T2 = task (v) { nil }
            println(T1 == T1)
            println(T1 == T2)
        """
        )
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun aa_03_task_err() {
        val out = test(
            """
            val T = task (v) { nil }
            coroutine(T)
        """
        )
        assert(
            out == " |  anon : (lin 3, col 13) : coroutine(T)\n" +
                    " v  coroutine error : expected coro\n"
        ) { out }
    }
    @Test
    fun aa_04_task_err() {
        val out = test(
            """
            val T = task (v) { nil }
            T()
        """
        )
        assert(
            out == " |  anon : (lin 3, col 13) : T()\n" +
                    " v  call error : expected function\n"
        ) { out }
    }
    @Test
    fun aa_05_yield_err() {
        val out = test(
            """
            yield(nil) ;;thus { it => nil }
        """
        )
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing coro or task\n") { out }
    }
    @Test
    fun aa_06_innoc() {
        val out = test("""
            val T = task () {
                :ok ;; innocuous
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 3, col 17) : expression error : innocuous expression\n") { out }
    }

    // SPAWN

    @Test
    fun bb_01_spawn() {
        val out = test(
            """
            val T = task (v) { nil }
            func () { nil } ()
            val t = spawn T()
            println(t)
        """
        )
        assert(out.contains("exe-task: 0x")) { out }
        //assert(out == ("nil\n")) { out }
    }
    @Test
    fun bb_02_resume_err() {
        val out = test(
            """
            val T = task (v) { nil }
            val t = spawn T()
            resume t()
        """
        )
        assert(
            out == " |  anon : (lin 4, col 13) : (resume (t)())\n" +
                    " v  resume error : expected yielded coro\n"
        ) { out }
    }
    @Test
    fun bb_03_spawn() {
        val out = test(
            """
            spawn (task (v1) {
                println(v1)
            }) (1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_04_spawn_err() {
        val out = test(
            """
            spawn nil()
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : (spawn nil())\n" +
                    " v  spawn error : expected task\n"
        ) { out }
    }
    @Test
    fun bb_05_spawn() {
        val out = test(
            """
            val T = task (v) { yield(nil) ; nil }
            val t = spawn T()
            println(t)
        """
        )
        assert(out.contains("exe-task: 0x")) { out }
    }

    // TASK / CORO / (no longer mixable)

    @Test
    fun bc_01_coro_spawn_nest_err() {
        val out = test("""
            var T = coro () {
                spawn coro () {
                    yield(nil)
                } ()
            }
            var t = coroutine(T)
            println(resume t())
            """
        )
        //assert(out == ":1\n:2\n1\n") { out }
        //assert(out.contains("x-coro: 0x")) { out }
        assert(out == " |  anon : (lin 8, col 21) : (resume (t)())\n" +
                " |  anon : (lin 3, col 17) : (spawn (coro () { yield(nil) })())\n" +
                " v  spawn error : expected task\n") { out }
    }
    @Test
    fun bc_02_spawn() {
        val out = test("""
            var t
            set t = coro (v) {
                var v' = v
                println(v')          ;; 1
                set v' = yield((v'+1)) 
                println(v')          ;; 3
                set v' = yield(v'+1) 
                println(v')          ;; 5
                v'+1
            }
            val a = spawn t(1)
            println(type(a))
            var v
            set v = resume a(3)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """, true)
        //assert(out == "1\n:x-coro\n3\n4\n5\n6\n") { out }
        assert(out == " |  anon : (lin 12, col 21) : (spawn t(1))\n" +
                " v  spawn error : expected task\n") { out }
    }
    @Test
    fun bc_04_coro_spawn() {
        val out = test("""
            var t
            set t = coro () {
                println(1)
                do {
                    println(2)
                    yield(nil)
                    println(3)
                }
                println(4)
            }
            var co
            set co = spawn t()
            resume co()
            println(5)
        """)
        //assert(out == "1\n2\n3\n4\n5\n") { out }
        assert(out == " |  anon : (lin 13, col 22) : (spawn t())\n" +
                " v  spawn error : expected task\n") { out }
    }
    @Test
    fun bc_05_coro_spawn() {
        val out = test("""
            var t
            set t = coro () {
                println(1)
                do {
                    println(2)
                    yield(nil)
                    println(3)
                }
                println(4)
            }
            var co
            set co = spawn (if true { t } else { nil }) ()
            resume co()
            println(5)
        """)
        //assert(out == "1\n2\n3\n4\n5\n") { out }
        assert(out == " |  anon : (lin 13, col 22) : (spawn if true { t } else { nil }())\n" +
                " v  spawn error : expected task\n") { out }
    }
    @Test
    fun bc_06_coro_spawn() {
        val out = test("""
            var t
            var f
            set f = func () {
                t
            }
            set t = coro () {
                println(1)
                do {
                    println(2)
                    yield(nil)
                    println(3)
                }
                println(4)
            }
            var co
            set co = spawn f() ()
            resume co()
            println(5)
        """)
        //assert(out == "1\n2\n3\n4\n5\n") { out }
        assert(out == " |  anon : (lin 17, col 22) : (spawn f()())\n" +
                " v  spawn error : expected task\n") { out }
    }

    // DELAY

    @Test
    fun bj_01_delay_err() {
        val out = test(
            """
            task () {
                func () {
                    delay
                }
            }
        """
        )
        assert(out.contains("anon : (lin 4, col 21) : delay error : expected enclosing task\n")) { out }
        //assert(out == ("anon : (lin 4, col 21) : access error : variable \"delay\" is not declared\n")) { out }
    }
    @Test
    fun bj_01x_delay_err() {
        val out = test(
            """
            delay
        """
        )
        assert(out.contains("anon : (lin 2, col 13) : delay error : expected enclosing task\n")) { out }
        //assert(out == ("anon : (lin 4, col 21) : access error : variable \"delay\" is not declared\n")) { out }
    }
    @Test
    fun bj_02y_delay() {
        val out = test(
            """
            spawn (task () {
                yield(nil)
                println(2)
            }) ()
            ;;println(:1)
            broadcast(nil)
            ;;println(:2)
            println(:ok)
        """
        )
        assert(out == "2\n:ok\n") { out }
    }
    @Test
    fun bj_02_delay() {
        val out = test(
            """
            spawn (task () {
                yield(nil)
                yield(nil)
                yield(nil)
                println(1)
            }) ()
            spawn (task () {
                yield(nil)
                println(2)
            }) ()
            spawn (task () {
                yield(nil)
                yield(nil)
                println(3)
            }) ()
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == "2\n3\n1\n:ok\n") { out }
    }
    @Test
    fun bj_02x_delay() {
        val out = test(
            """
            spawn (task () {
                yield(nil)
                println(2)
            }) ()
            spawn (task () {
                yield(nil)
                yield(nil)
                println(3)
            }) ()
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == "2\n3\n:ok\n") { out }
    }
    @Test
    fun bj_03_delay() {
        val out = test(
            """
            spawn (task () {
                yield(nil)
                delay
                yield(nil)
                delay
                yield(nil)
                println(1)
            }) ()
            spawn (task () {
                yield(nil)
                println(2)
            }) ()
            spawn (task () {
                yield(nil)
                delay
                yield(nil)
                println(3)
            }) ()
            println(:a)
            broadcast(nil)
            println(:b)
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == ":a\n2\n:b\n1\n3\n:ok\n") { out }
    }
    @Test
    fun bj_03_par() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    println(1)
                }) ()
                spawn (task () {
                    println(2)
                }) ()
                println(3)
            }) ()
        """
        )
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun bj_04_toggle() {
        DEBUG = true
        val out = test(
            """
            $PLUS
            var i = 0
            loop {
                broadcast(nil)
                break if i == 254
                set i = i + 1
            }
            val t = spawn (task () {
                println(`:number CEU_TIME`)
                yield(nil)
                delay
                println(:ok)
            }) ()
            toggle t (false)
            broadcast(nil)
            toggle t (true)
            broadcast(nil)
        """
        )
        assert(out == "255\n:ok\n") { out }
    }
    @Test
    fun bj_05_spawn_spawn() {
        val out = test(
            """
            spawn (task () {
                spawn (task (v) {
                    println(v)
                }) (10)
                nil
            }) ()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun bj_06_delay() {
        val out = test("""
            spawn (task () {
                val v = do {
                    do 100
                    delay
                }
                println(v)
            }) ()
        """)
        assert(out == "100\n") { out }
    }
    @Test
    fun bj_07_delay() {
        DEBUG = true
        val out = test("""
            spawn (task () {
                val v = do {
                    loop {
                        (break (100) if true)
                    }
                    delay
                }
                println(v)
                ;;dump(v)
            }) ()
        """)
        assert(out == "100\n") { out }
    }

    // SCOPE

    @Test
    fun cc_00_scope() {
        val out = test(
            """
            var x
            set x = do {
                spawn (task() {nil}) ()
            }
            println(2)
        """
        )
        //assert(out == " v  anon : (lin 3, col 21) : block escape error : cannot copy reference out\n") { out }
        assert(out == "2\n") { out }
        //assert(out == " v  anon : (lin 3, col 21) : block escape error : cannot assign running coro or task to outer scope\n") { out }
    }
    @Test
    fun cc_01_scope() {
        val out = test(
            """
            val x = do {
                spawn (task() { yield(nil) ; nil }) ()
            }
            println(:ok)
        """
        )
        //assert(out == "anon : (lin 3, col 21) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out == ":ok\n") { out }
        //assert(out == " v  anon : (lin 2, col 21) : block escape error : cannot assign running coro or task to outer scope\n") { out }
    }
    @Test
    fun cc_02_scope() {
        val out = test(
            """
            var t
            set t = task () {
                yield(nil) ; nil
            }
            var co
            set co = if true { spawn t() } else { nil }
            println(:ok)
        """
        )
        //assert(out == "anon : (lin 7, col 30) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out == ":ok\n") { out }
        //assert(out == " v  anon : (lin 7, col 30) : block escape error : cannot assign running coro or task to outer scope\n") { out }
    }
    @Test
    fun cc_03_scope() {
        val out = test(
            """
            val t = task () { nil }
            var f
            set f = func () {
                spawn t()
                nil
            }
            f()
            println(:ok)
        """
        )
        //assert(out == "anon : (lin 8, col 13) : f()\n" +
        //        "anon : (lin 5, col 29) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 6, col 17) : spawn error : unexpected enclosing func\n") { out }
    }
    @Test
    fun cc_04_scope() {
        val out = test(
            """
            var x
            set x = do {
                spawn (task() {println(1)}) ()
                nil
            }
            println(2)
        """
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun cc_05_scope() {
        val out = test(
            """
            var T
            set T = task (v) {
                yield(nil) ; nil ;;println(v)
            }
            var t
            set t = do {
                var v
                set v = 10
                spawn T(v)  ;; ERR: coro in nested scope cannot escape
            }
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 7, col 21) : block escape error : incompatible scopes\n:error\n") { out }
        //assert(out == " v  anon : (lin 7, col 21) : block escape error : cannot assign running coro or task to outer scope\n") { out }
    }
    @Test
    fun cc_06_scope() {
        val out = test(
            """
            val T = task (t1) {
                val t2 = []
                do [t1,[],t2]
                yield(nil) ;;thus { it => nil }
            }
            do {
                spawn T([])
                nil
            }
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    // ALIEN SCOPE

    @Test
    fun cd_01_every() {
        val out = test(
            """
            spawn (task () {
                val it = yield(nil)
                println(it;;;, evt;;;)
                yield(nil)
            }) (nil)
            do {
                val e = []
                broadcast'(:task,e)
            }
            println(:ok)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : yield error : unexpected enclosing thus\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 21) : argument error : cannot copy reference out\n") { out }
        assert(out == "[]\n:ok\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun cd_02_bcast_spawn_arg() {
        val out = test(
            """
            val T = task () {
                val x = yield(nil)
            }
            spawn T() 
            do {
                val e = []
                broadcast(e)
            }
            println(:ok)
        """
        )
        //assert(out == " |  anon : (lin 8, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cd_03_bcast_pub_arg() {
        val out = test(
            """
            val T = task () {
                val evt = yield(nil)
                set pub = evt
                println(:in, pub)
                :ok
            }
            val t = spawn T() 
            do {
                val e = []
                broadcast(e)
            }
            println(:out, t.pub)
        """
        )
        //assert(out == " |  anon : (lin 8, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 21) : set error : cannot hold alien reference\n") { out }
        assert(out == ":in\t[]\n:out\t:ok\n") { out }
    }
    @Test
    fun cd_04_bcast_copy() {
        val out = test("""
            val T = task () {
                val evt = yield(nil)
                val v = copy(evt)
                println(v)
            }
            spawn T()
            broadcast([1,2,3])
        """, true)
        assert(out == "[1,2,3]\n") { out }
    }

    // BROADCAST

    @Test
    fun dd_01_bcast() {
        val out = test(
            """
            println(broadcast(1))
        """
        )
        assert(out == "nil\n") { out }
        //assert(out == "\n") { out }
        //assert(out == "true\n") { out }
    }
    @Test
    fun dd_02_bcast() {
        val out = test(
            """
            spawn (task () {
                println(1)
                yield(nil) ;;thus { it => nil }
                println(2)
            })()
            broadcast(nil)
        """
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun dd_02x_bcast() {
        val out = test(
            """
            val T = task (x) {
                println(x)
                val y = yield(nil)
                println(y)
            }
            spawn T(1)
            broadcast(2)
        """
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun dd_03_bcast() {
        val out = test(
            """
            var tk = task (v) {
                val e1 = yield(nil) ;;thus { it => it }
                println(:1, ;;;evt,;;; e1)
                val e2 = yield(nil) ;;thus { it => it }
                println(:2, ;;;evt,;;; e2)
            }
            spawn tk ()
            broadcast(1)
            broadcast(2)
            broadcast(3)
        """
        )
        //assert(out == ":1\t1\t1\n:2\t2\t2\n") { out }
        assert(out == ":1\t1\n:2\t2\n") { out }
    }
    @Test
    fun dd_04_bcast() {
        val out = test(
            """
            var tk
            set tk = task (v) {
                val e1 = yield(nil)
                println(v,e1;;;evt;;;)
                var e2 = yield(nil)
                println(v,e2;;;evt;;;)
            }
            var co1 = spawn tk(:1)
            var co2 = spawn tk(:2)
            broadcast(1)
            broadcast(2)
            broadcast(3)
        """
        )
        //assert(out == "nil\n1\nnil\n1\nnil\n2\nnil\n2\n") { out }
        assert(out.contains(":1\t1\n:2\t1\n:1\t2\n:2\texe-task: 0x")) { out }
        //assert(out == (":1\t1\n:2\t1\n:1\t2\n:2\t2\n")) { out }
    }
    @Test
    fun dd_04y_bcast() {
        val out = test(
            """
            val T = task (v) {
                yield(nil)
            }
            val t = spawn T()
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun dd_04x_bcast() {
        val out = test(
            """
            val T = task (v) {
                ;;`printf(">>> %p\n", X->exe);`
                val e = yield(nil)
                println(v, e)
            }
            var t1 = spawn T(:1)
            var t2 = spawn T(:2)
            broadcast(1)
        """
        )
        assert(out.contains(":1\t1\n:2\texe-task: 0x")) { out }
        //assert(out == (":1\t1\n:2\t1\n")) { out }
    }
    @Test
    fun dd_05_bcast() {
        val out = test(
            """
            var co1 = spawn (task () {
                var co2 = spawn (task () {
                    yield(nil)              ;; awakes from outer bcast
                    println(:2)
                }) ()
                yield(nil)                  ;; awakes from co2 termination
                println(:1)
            }) ()
            ;;`printf(">>> %d\n", CEU_DEPTH);`
            println(:bcast)
            broadcast(nil)
        """
        )
        assert(out == ":bcast\n:2\n:1\n") { out }
    }
    @Test
    fun dd_05y_bcast() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    println(:1)
                    yield(nil)              ;; 1. awakes from outer bcast
                    println(:3)
                }) ()
                yield(nil)                  ;; 2. awakes from nested task
                yield(nil)                  ;; 3. awakes from outer bcast
                println(:ok)
            }) ()
            println(:2)
            broadcast(nil)
            println(:4)
        """
        )
        assert(out == ":1\n:2\n:3\n:ok\n:4\n") { out }
    }
    @Test
    fun dd_05z_bcast() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    println(:1)
                    yield(nil)              ;; awakes from outer bcast
                    println(:3)
                }) ()
                yield(nil)                  ;; awakes from nested task
                delay
                yield(nil)                  ;; does not awake from outer bcast
                println(:no)
            }) ()
            println(:2)
            broadcast(nil)
            println(:4)
        """
        )
        assert(out == ":1\n:2\n:3\n:4\n") { out }
    }
    @Test
    fun dd_05x_bcast() {
        DEBUG = true
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    println(:1, yield(nil)) ;; awakes from outer bcast
                }) ()
                spawn (task () {
                    loop {
                        yield(nil)
                    }
                }) ()
                println(:2, yield(nil))      ;; awakes from :1 termination
            }) ()               ;; kill anon task which is pending on traverse
            broadcast(:out)
        """
        )
        assert(out.contains(":1\t:out\n:2\texe-task: 0x")) { out }
    }
    @Test
    fun dd_06_bcast() {
        val out = test(
            """
            var tk
            set tk = task (v) {
                println(v)
                val e = yield(nil) ;;thus { it => it }
                println(e;;;evt;;;)
            }
            var co = spawn(tk)(1)
            broadcast(2)
        """
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun dd_07_bcast() {
        val out = test(
            """
            func () {
                 broadcast(1)
            }
            println(1)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 3, col 18) : broadcast error : unexpected enclosing func\n") { out }
    }
    @Test
    fun dd_08_bcast() {
        val out = test(
            """
            val T = task () {
                yield(nil) ;;thus { it => nil }
                println(:ok)
            }
            var t = spawn T()
            do {
                broadcast(1)
            }
        """
        )
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun dd_09_bcast() {
        val out = test(
            """
            var tk
            set tk = task () {
                yield(nil) ;;thus { it => nil }
                val e = yield(nil) ;;thus { it => it }
                println(e;;;evt;;;)                
            }
            var co1 = spawn (tk) ()
            var co2 = spawn tk ()
            do {
                 broadcast(1)
                 broadcast(2)
                 broadcast(3)
            }
        """
        )
        //assert(out == "2\n2\n") { out }
        assert(out.contains("2\nexe-task: 0x")) { out }
    }
    @Test
    fun dd_10_bcast() {
        val out = test(
            """
            var tk
            set tk = task () {
                val e1 = yield(nil) ;;thus { it => it }
                var e2
                do {
                    println(e1)
                    set e2 = yield(nil) ;;thus { it => it }
                    println(e2)
                }
                do {
                    println(e2)
                    val e3 = yield(nil) ;;thus { it => it }
                    println(e3)
                }
            }
            spawn tk ()
            broadcast(1)
            broadcast(2)
            broadcast(3)
            broadcast(4)
        """
        )
        assert(out == "1\n2\n2\n3\n") { out }
    }
    @Test
    fun dd_11_bcast_await() {
        val out = test(
            """
            val T = task (v) {
                println(:1)
                val e = ${AWAIT()}
                println(:2, e)                
            }
            spawn T()
            broadcast(10)
        """
        )
        assert(out == ":1\n:2\t10\n") { out }
    }
    @Test
    fun dd_12_bcast() {
        val out = test(
            """
            val T = task (v) {
                yield(nil)
                ;;println(:time, `:number CEU_TIME_MAX`)
                val e = ${AWAIT()}
                println(e)                
            }
            spawn T ()
            spawn T ()
            func () {
                 broadcast(1)
                 broadcast(2)
                 broadcast(3)
            }()
        """
        )
        assert(out == "2\n2\n") { out }
    }
    @Test
    fun dd_12x_bcast() {
        val out = test(
            """
            val T = task () {
                loop {
                    val it = yield(nil)
                }
            }
            spawn T ()
            broadcast(2)
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_13_bcast() {
        val out = test(
            """
            var tk
            set tk = task (v) {
                println(v)
                val e1 = ${AWAIT()}
                println(e1)                
                val e2 = ${AWAIT()}
                println(e2)                
            }
            println(:1)
            var co1 = spawn (tk) (10)
            var co2 = spawn (tk) (10)
            ;;catch {
                ;;func () {
                    println(:2)
                    broadcast(20)
                    println(:3)
                    broadcast(30)
                ;;}()
            ;;}
        """
        )
        assert(out == ":1\n10\n10\n:2\n20\n20\n:3\n30\n30\n") { out }
    }
    @Test
    fun dd_13x_bcast() {
        val out = test(
            """
            val T = task () {
                val e1 =
                    loop {
                        val it = yield(nil)
                        do {
                            val x = it
                            println(:in, it)    ;; TODO: 10
                        }
                        break if true
                   }     
                val x
            }
            spawn T()
            broadcast(10)
        """
        )
        assert(out == ":in\t10\n") { out }
    }
    @Test
    fun dd_15_bcast() {
        val out = test(
            """
            broadcast ([])
            println(:ok)
            """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_16_tags() {
        val out = test(
            """
            val T = task () {
                func (it) {
                    println(sup?(:X,tag(it)))
                } (yield(nil))
            }
            spawn T()
            broadcast (tag(:X,[]))
        """
        )
        assert(out == "true\n") { out }
    }
    @Test
    fun gc_dd_16_bcast() {
        val out = test(
            """
            do {
                broadcast([])
                broadcast([])
                broadcast([])
                broadcast([])
                println(:ok)
            }
            """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun gc_17_ff() {
        val out = test(
            """
            var T
            set T = task (v) {
                spawn task () {
                    println(v)
                    yield(nil)
                    println(v)
                } ()
                loop { yield(nil) }
            }
            spawn T(1)
            spawn T(2)
            broadcast(nil)
        """
        )
        assert(out == "1\n2\n1\n2\n") { out }
    }
    @Test
    fun dd_18_xceu() {
        val out = test("""
            spawn (task () {
                spawn (task () {
                    yield(nil)
                }) ()
                nil
            }) ()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_19_xceu() {
        val out = test("""
            spawn task () {
                var evt1 = yield(nil)
                val evtx = evt1
                println(evt1)
                spawn (task () {
                    var evt2 = evtx
                    loop {
                        println(evt2)    ;; lost reference
                        set evt2 = yield(nil)
                    }
                }) ()
                set evt1 = yield(nil)
            }()
            broadcast (10)
            broadcast (20)
        """)
        //assert(out == "10\nnil\n20\n") { out }
        assert(out == "10\n10\n20\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }

    // BCAST / ARGS

    @Test
    fun de_01_bcast() {
        val out = test(
            """
            broadcast(1) in nil
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : broadcast'(nil,1)\n" +
                    " v  broadcast error : invalid target\n"
        ) { out }
    }
    @Test
    fun de_02_bcast() {
        val out = test(
            """
            broadcast(1) in :x
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : broadcast'(:x,1)\n" +
                    " v  broadcast error : invalid target\n"
        ) { out }
    }
    @Test
    fun de_03_bcast() {
        val out = test(
            """
            println(broadcast(1) in :global)
        """
        )
        //assert(out == "true\n") { out }
        assert(out == "nil\n") { out }
        //assert(out == "\n") { out }
    }
    @Test
    fun de_04_bcast() {
        val out = test(
            """
            println(broadcast(1) in :task)
        """
        )
        //assert(out == "true\n") { out }
        assert(out == "nil\n") { out }
        //assert(out == "\n") { out }
    }
    @Test
    fun de_05_bcast() {
        val out = test(
            """
            val t = spawn (task () { nil }) ()
            println(broadcast(1) in t)
        """
        )
        //assert(out == "\n") { out }
        assert(out == "nil\n") { out }
        //assert(out == "true\n") { out }
        //assert(
        //    out == " |  anon : (lin 3, col 21) : broadcast'(t,1)\n" +
        //            " v  broadcast error : invalid target\n"
        //) { out }
    }
    @Test
    fun de_06_bcast() {
        val out = test(
            """
            val T = task () {
                println(yield(nil))
                nil
            }
            val t = spawn T()
            broadcast ([])
        """
        )
        //assert(out == "anon : (lin 16, col 13) : f()\n" +
        //        "anon : (lin 14, col 17) : g()\n" +
        //        "anon : (lin 12, col 21) : broadcast []\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == "[]\n") { out }
    }

    // BCAST / TARGETS

    @Test
    fun df_01_global() {
        val out = test(
            """
            spawn (task () {
                val x = yield(nil)
                println(:ok, x)
            }) ()
            spawn (task () {
                broadcast(10) in :global
            }) ()
        """
        )
        assert(out == ":ok\t10\n") { out }
    }
    @Test
    fun df_02_non_global() {
        val out = test(
            """
            spawn (task () {
                val x = yield(nil)
                println(:ok, x)
            }) ()
            spawn (task () {
                broadcast(10) in :task
                println(:no)
                broadcast(20) in :global
            }) ()
        """
        )
        assert(out == ":no\n:ok\t20\n") { out }
    }
    @Test
    fun df_03_bcast_throw() {
        DEBUG = true
        val out = test(
            """
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
        """
        )
        assert(
            out == ":ok\n" +
                    " |  anon : (lin 17, col 13) : broadcast'(:task,nil)\n" +
                    " |  anon : (lin 11, col 21) : broadcast'(:global,nil)\n" +
                    " |  anon : (lin 7, col 21) : error(:XXX)\n" +
                    " v  error : :XXX\n"
        ) { out }
    }
    @Test
    fun TODO_df_04_bcast_local() {
        val out = test(
            """
        var T
        set T = task (v) {
            yield(nil)
            println(v)
        }
        var t1
        set t1 = spawn T (1)
        do {
            var t2
            set t2 = spawn T (2)
            broadcast(nil) in :local
        }
    """
        )
        assert(out == "2\n") { out }
    }

    // EVT

    @Test
    fun de_01_evt() {
        val out = test("""
            println(evt)
        """)
        //assert(out == "nil\n") { out }
        assert(out == "anon : (lin 2, col 21) : access error : variable \"evt\" is not declared\n") { out }
    }
    @Test
    fun de_02_evt() {
        val out = test("""
            var evt
            spawn (task () {
                println(evt)
                set evt = yield(nil)
                println(evt)
            }) ()
            broadcast(10)
        """)
        assert(out == "nil\n10\n") { out }
    }
    @Test
    fun de_03_evt_err() {
        DEBUG = true
        val out = test("""
            spawn (task () {
                val evt = yield(nil)
                val x = evt
                println(x)
            }) ()
            do {
                val x
                broadcast([10])
            }
        """)
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'([10],:task)\n" +
        //        " v  anon : (lin 4, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun de_03x_evt_err() {
        DEBUG = true
        val out = test("""
            spawn (task () {
                val evt = yield(nil)
                var x
                set x = evt
                println(x)
            }) ()
            do {
                val x
                broadcast([10])
            }
        """)
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'([10],:task)\n" +
        //        " v  anon : (lin 5, col 21) : set error : cannot hold alien reference\n") { out }
    }
    @Test
    fun de_03y_evt_err() {
        DEBUG = true
        val out = test("""
            spawn (task () {
                val evt = yield(nil)
                val x = [nil]
                set x[0] = evt
                println(x)
            }) ()
            do {
                val x
                broadcast([10])
            }
        """)
        assert(out == "[[10]]\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'([10],:task)\n" +
        //        " v  anon : (lin 5, col 21) : store error : cannot hold alien reference\n") { out }
    }
    @Test
    fun de_04_evt_err() {
        val out = test("""
            spawn (task () {
                val evt = yield(nil)
                do {
                    val x = evt
                    println(x)
                    yield(nil)
                }
            }) ()
            do {
                val x
                broadcast([10])
            }
        """)
        //assert(out == "[10]\n" +
        //        " |  anon : (lin 10, col 13) : broadcast'([10],:task)\n" +
        //        " v  anon : (lin 7, col 21) : yield error : cannot hold alien reference\n") { out }
        //assert(out == " |  anon : (lin 12, col 17) : broadcast'([10],:task)\n" +
        //        " v  anon : (lin 5, col 21) : declaration error : cannot hold alien reference\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun de_05_evt() {
        val out = test("""
            spawn (task () {
                val evt = yield(nil)
                do {
                    val x = evt
                    println(x)
                }
                yield(nil)
            }) ()
            do {
                val x
                broadcast([10])
            }
        """)
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 12, col 17) : broadcast'([10],:task)\n" +
        //        " v  anon : (lin 5, col 21) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun de_05_evt_err_valgrind() {
        val out = test("""
            spawn (task () {
                var evt = yield(nil)
                val x = evt
                println(x)
                set evt = yield(nil)
                println(x)
            }) ()
            do {
                val e = [10]
                broadcast(e)
            }
            broadcast(nil)
        """)
        assert(out == "[10]\n[10]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun de_06_evt() {
        val out = test("""
            spawn (task () {
                val evt = yield(nil)
                println(do {
                    evt
                })
            }) ()
            do {
                val x
                broadcast([10])
            }
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun de_07_evt_err() {
        val out = test("""
            spawn (task () {
                val evt = yield(nil)
                val x = evt[0]
                println(x)
            }) ()
            do {
                val e = [[10]]
                broadcast(e)
            }
        """)
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun de_08_evt_hld_err() {
        DEBUG = true
        val out = test(
            """
            var tk
            set tk = task (xxx) {
                val evt = yield(nil)
                do {
                    val xxx' = evt
                    nil
                }
            }
            var co = spawn(tk)()
            broadcast ([])
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast []\n" +
        //        "anon : (lin 4, col 27) : invalid evt : cannot expose dynamic alien\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast []\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun de_09_evt_hld_err() {
        DEBUG = true
        val out = test(
            """
            var tk
            set tk = task (xxx) {
                val evt = yield(nil)
                do {
                    val xxx' = evt[0]
                }
            }
            spawn (tk) ()
            broadcast( [[]] )
            println(`:number CEU_GC.free`)
        """
        )
        //assert(out == "anon : (lin 8, col 13) : broadcast [[]]\n" +
        //        "anon : (lin 4, col 31) : invalid index : cannot expose dynamic alien field\n:error\n") { out }
        assert(out == "2\n") { out }
        //assert(out == "3\n") { out }
        //assert(out == "anon : (lin 8, col 13) : broadcast [[]]\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun de_10_evt_hld_err() {
        DEBUG = true
        val out = test(
            """
            var tk
            set tk = task (xxx) {
                val evt = yield(nil)
                do {
                    val xxx' = evt
                    nil
                }
            }
            var co = spawn (tk) (1)
            broadcast ([])
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 10, col 14) : broadcast []\n" +
        //        "anon : (lin 5, col 27) : invalid evt : cannot expose dynamic alien\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast []\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun de_11_evt_hld() {
        DEBUG = true
        val out = test(
            """
            var tk
            set tk = task (xxx) {
                do {
                val evt = yield(nil)
                    val xxx' = evt
                }
                    yield(nil)
            }
            var co = spawn (tk)(1)
            broadcast ([])
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "0\n") { out }
        //assert(out == "anon : (lin 10, col 14) : broadcast []\n" +
        //        "anon : (lin 5, col 27) : invalid evt : cannot expose dynamic alien\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast []\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun de_12_gg_evt_hld() {
        val out = test(
            """
            var fff
            set fff = func (x) { x }
            spawn task () {
                var evt = yield(nil)
                loop {
                    break if evt[:type]==:x
                    set evt = yield(nil)
                }
                println(99)
            }()
            println(1)
            broadcast (@[(:type,:y)])
            println(2)
            broadcast (@[(:type,:x)])
            println(3)
        """)
        assert(out == "1\n2\n99\n3\n") { out }
    }
    @Test
    fun de_13_evt_hld() {
        val out = test(
            """
            var fff
            set fff = func (x) { x }
            spawn task () {
                println(1)
                var evt
                do {
                    println(2)
                    set evt = yield(nil)
                    println(3)
                }
                println(4)
                fff(evt[:type])
                println(99)
            }()
            broadcast (@[(:type,:y)])
            broadcast (@[(:type,:x)])
        """
        )
        //assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "1\n2\n3\n4\n99\n") { out }
    }
    @Test
    fun de_14__evt() {
        val out = test(
            """
            spawn task () {
                println(111)
                yield(nil)
                println(222)
            }()
            println(1)
            broadcast( nil)
            println(2)
        """
        )
        assert(out == "111\n1\n222\n2\n") { out }
    }
    @Test
    fun de_15_gg_evt() {
        val out = test(
            """
            spawn task () {
                var evt
                loop {
                    println(evt)
                    set evt = yield(nil)
                }
            }()
            broadcast (@[])
        """
        )
        assert(out == "nil\n@[]\n") { out }
    }
    @Test
    fun de_16_gg_evt() {
        val out = test(
            """
            spawn task () {
                loop {
                    var evt
                    do {
                        set evt = yield(nil)
                    }
                    println(evt)
                }
            }()
            broadcast( @[] )
        """
        )
        assert(out == "@[]\n") { out }
    }
    @Test
    fun de_17_evt_err() {
        val out = test(
            """
            var x
            set x = []
            broadcast (x)
            println(x)
        """
        )
        //assert(out == "anon : (lin 4, col 13) : set error : incompatible scopes\n") { out }
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 4, col 35) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun de_18_gg_evt_hld_err() {
        DEBUG = true
        val out = test(
            """
            var tk
            set tk = task (xxx) {
                val evt = yield(nil)
                do {
                    val xxx' = evt[0]
                    nil
                }
            }
            var co = spawn (tk) ()
            broadcast (#[[]])
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 8, col 13) : broadcast [[]]\n" +
        //        "anon : (lin 4, col 31) : invalid index : cannot expose dynamic alien field\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast #[[]]\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun de_19_gg_evt_hld_err() {
        DEBUG = true
        val out = test(
            """
            var tk
            set tk = task (xxx) {
                val evt = yield(nil)
                val xxx' = evt[0]
            }
            var co = spawn (tk) ()
            broadcast (@[(1,[])])
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 8, col 13) : broadcast [[]]\n" +
        //        "anon : (lin 4, col 31) : invalid index : cannot expose dynamic alien field\n") { out }
    }

    // BCAST / TARGETS

    @Test
    fun TODO_dh_01_multi() {
        val out = test(
            """
            spawn (task () {
                println(yield(nil))
            }) ()
            broadcast(10,20)
        """
        )
        assert(out == ":ok\t10\n") { out }
    }

    // THROW / CATCH

    @Test
    fun ee_00x_throw() {
        val out = test(
            """
            catch (it|false) {
                spawn (task () {
                    error(:xxx)
                })()
            }
            println(10)
        """
        )
        assert(out == " |  anon : (lin 3, col 17) : (spawn (task () { error(:xxx) })())\n" +
                " |  anon : (lin 4, col 21) : error(:xxx)\n" +
                " v  error : :xxx\n") { out }
    }
    @Test
    fun ee_00y_throw() {
        val out = test(
            """
            catch (it|true) {
                spawn (task () {
                    error(:xxx)
                })()
            }
            println(10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun ee_01_throw() {
        val out = test(
            """
            catch (it | :xxx){
                spawn (task () {
                    yield(nil) ;;thus { it => nil }
                })()
                spawn (task () {
                    error(:xxx)
                })()
            }
            println(10)
        """
        )
        assert(out == "10\n") { out }
    }

    @Test
    fun ee_02_throw() {
        val out = test(
            """
            spawn (task () {
                error(:err)
            })()
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : (spawn (task () { error(:err) })())\n" +
                    " |  anon : (lin 3, col 17) : error(:err)\n" +
                    " v  error : :err\n"
        ) { out }
    }

    @Test
    fun ee_03_throw() {
        val out = test(
            """
            spawn (task () {
                yield(nil) ;;thus { it => nil }
                error(:err)
            })()
            broadcast(nil)
        """
        )
        assert(
            out == " |  anon : (lin 6, col 13) : broadcast'(:task,nil)\n" +
                    " |  anon : (lin 4, col 17) : error(:err)\n" +
                    " v  error : :err\n"
        ) { out }
    }

    @Test
    fun ee_04_throw() {
        val out = test(
            """
            spawn (task () {
                yield(nil) ;;thus { it => nil }
                error(:err)
            })()
            broadcast(nil)
        """
        )
        assert(
            out == " |  anon : (lin 6, col 13) : broadcast'(:task,nil)\n" +
                    " |  anon : (lin 4, col 17) : error(:err)\n" +
                    " v  error : :err\n"
        ) { out }
    }

    @Test
    fun ee_05_throw() {
        val out = test(
            """
            spawn (task () {
                spawn( task () {
                    yield(nil) ;;thus { it => nil }
                    error(:err)
                })()
                yield(nil) ;;thus { it => nil }
            })()
            broadcast(nil)
        """
        )
        assert(
            out == " |  anon : (lin 9, col 13) : broadcast'(:task,nil)\n" +
                    " |  anon : (lin 5, col 21) : error(:err)\n" +
                    " v  error : :err\n"
        ) { out }
    }

    @Test
    fun ee_06_throw() {
        val out = test(
            """
            spawn (task () {
                yield(nil) ;;thus { it => nil }
                error(:err)
            })()
            spawn (task () {
                nil
            })()
            ;;broadcast(nil)
        """
        )
        //assert(out == " |  anon : (lin 9, col 13) : broadcast'(nil,:task)\n" +
        //        " |  anon : (lin 4, col 17) : error(:err)\n" +
        //        " v  error : :err\n") { out }
        assert(
            out == " |  anon : (lin 6, col 13) : (spawn (task () { nil })())\n" +
                    " |  anon : (lin 4, col 17) : error(:err)\n" +
                    " v  error : :err\n"
        ) { out }
    }

    @Test
    fun ee_08_throw() {
        val out = test(
            """
            spawn (task () {
                yield(nil) ;;thus { it => nil }
                nil
            })()
            spawn (task () {
                yield(nil) ;;thus { it => nil }
                yield(nil) ;;thus { it => nil }
                error(:err)
            })()
            broadcast(nil)
            ;;broadcast(nil)
        """
        )
        assert(
            out == " |  anon : (lin 11, col 13) : broadcast'(:task,nil)\n" +
                    " |  anon : (lin 9, col 17) : error(:err)\n" +
                    " v  error : :err\n"
        ) { out }
    }

    @Test
    fun ee_09_bcast() {
        val out = test(
            """
            val T = task () {
                yield(nil) ;;thus { it => nil }
                error(:err)
            }
            spawn T()
            spawn T()
            broadcast(nil)
        """
        )
        assert(
            out == " |  anon : (lin 8, col 13) : broadcast'(:task,nil)\n" +
                    " |  anon : (lin 4, col 17) : error(:err)\n" +
                    " v  error : :err\n"
        ) { out }
    }

    @Test
    fun ee_10_bcast() {
        val out = test(
            """
            val T = task (v) {
                val e = yield(nil) ;;thus { it => it }
                println(v,e)                
            }
            spawn T(10)
            ;;catch {
                ;;func () {
                    broadcast ([])
                ;;}()
            ;;}
        """
        )
        assert(out == "10\t[]\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : (func () { broadcast [] })()\n" +
        //        " |  anon : (lin 9, col 21) : broadcast []\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold event reference\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : (func () { broadcast [] })()\n" +
        //        " |  anon : (lin 9, col 21) : broadcast []\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 9, col 21) : broadcast'([])\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
    }

    @Test
    fun ee_10_bcast_err() {
        val out = test(
            """
            val T = task (v) {
                if (v == 11) {
                    yield(nil)
                } else {nil}
                val e = yield(nil)
                println(v,e)                
            }
            spawn T(10)
            spawn T(11)
            catch (it| true) {
                ;;func () {
                    broadcast ([])
                ;;}()
                broadcast ([])
            }
        """
        )
        assert(
            out == "10\t[]\n" +
                    "11\t[]\n"
        ) { out }
        //assert(out == "[]\n") { out }
        //assert(out == "10\t[]\n" +
        //        " |  anon : (lin 13, col 21) : broadcast'([])\n" +
        //        " v  anon : (lin 6, col 17) : declaration error : cannot copy reference out\n") { out }
    }

    @Test
    fun ee_10_bcast_err_xx() {
        val out = test(
            """
            val T = task (v) {
                val e = yield(nil)
                yield(nil)
                println(v,e)                
            }
            spawn T(10)
            do {
                val x
                broadcast ([])
            }
            broadcast(nil)
        """
        )
        assert(out == "10\t[]\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'([],:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'([],:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }

    @Test
    fun ee_10_bcast_err2() {
        val out = test(
            """
            val T = task (v) {
                val e = yield(nil)
                println(v,e)                
            }
            spawn T(10)
            do {
                val e = []
                broadcast (e)
            }
        """
        )
        assert(out == "10\t[]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }

    @Test
    fun ee_11_bcast() {
        val out = test(
            """
            val T = task (v) {
                val e = yield(nil) ;;thus { it => it }
                println(e)                
            }
            spawn T(10)
            catch (it| (do { println(it) ; true }) ) {
                ;;func () {
                    broadcast ([])
                ;;}()
            }
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == "declaration error : cannot hold event reference\n") { out }
        //assert(out == "block escape error : cannot copy reference out\n") { out }
    }

    @Test
    fun ee_12_bcast() {
        val out = test(
            """
            var co1 = spawn (task () {
                var co2 = spawn (task () {
                    ${AWAIT()}
                    error(:error)
                })()
                ${AWAIT()}
                println(1)
            })()
            broadcast(nil)
        """
        )
        assert(
            out == " |  anon : (lin 10, col 13) : broadcast'(:task,nil)\n" +
                    " |  anon : (lin 5, col 21) : error(:error)\n" +
                    " v  error : :error\n"
        ) { out }
    }

    @Test
    fun ee_14_throw() {     // catch from nesting and broadcast
        val out = test(
            """
            spawn (task () {
                catch (it| it==:e1 ){  ;; catch 1st (yes catch)
                    spawn (task () {
                        yield(nil) ;;thus { it => nil }
                        println(222)
                        error(:e1)                  ;; error
                    }) ()
                    loop { yield(nil) } ;;thus { it => nil }
                }
                println(333)
            }) ()
            catch (it| true) {   ;; catch 2nd (no catch)
                println(111)
                broadcast(nil)
                println(444)
            }
            println(:END)
        """
        )
        assert(
            out == "111\n" +
                    "222\n" +
                    "333\n" +
                    "444\n" +
                    ":END\n"
        ) { out }
    }

    @Test
    fun ee_15_throw() {
        val out = test(
            """
            spawn (task () {
                catch (it| false) {
                    yield(nil) ;;thus { it => nil }
                }
                println(999)
            }) ()
            catch (it| true) {
                error(nil)
            }
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    // TASK TERMINATION

    @Test
    fun ff_00_term() {
        val out = test(
            """
            spawn (task () {
                val e = yield(nil) ;;thus { it => it }
                println(:ok, e)
            })()
            val t = spawn (task () {
                nil
            })()
            broadcast(t)
        """
        )
        assert(out.contains(":ok\texe-task: 0x")) { out }
        //assert(out.contains(" |  anon : (lin 6, col 13) : spawn (task () { nil })(nil)\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot move pending reference in")) { out }
    }

    @Test
    fun ff_01_term() {
        val out = test(
            """
            spawn (task () {
                val x = yield(nil)
                println(:ok, x)
            })()
            val t = spawn (task () {
                nil
            })()
            broadcast(t)
        """
        )
        assert(out.contains(":ok\texe-task: 0x")) { out }
    }

    @Test
    fun ff_02_term() {
        val out = test(
            """
            spawn (task () {
                yield(nil) ;;thus { it => nil }
                println(:1)
                val x = yield(nil)
                println(:ok, x)
            })()
            val t = spawn (task () {
                yield(nil) ;;thus { it => nil }
                println(:2)
            })()
            broadcast(nil)
            broadcast(t)
        """
        )
        assert(out.contains(":1\n:2\n:ok\texe-task: 0x")) { out }
    }

    @Test
    fun ff_03_term() {
        val out = test(
            """
            spawn (task () {
                val t = spawn (task () {
                    val it = yield(nil)
                    println(:1, it)
                })()
                println(:0)
                func (it) {
                    if (type(it) == :exe-task) {
                        println(:2, it == t)
                    } else {
                        nil
                    }
                } (yield(nil))
            } )()
            broadcast(:a)
            broadcast (:b)
        """
        )
        assert(out == ":0\n:1\t:a\n:2\ttrue\n") { out }
    }

    // SCOPE / BCAST

    @Test
    fun gg_01_scope() {
        val out = test(
            """
            val T = task (v) {
                val e = yield(nil) ;;thus { it => it }
                println(e)                
            }
            spawn T(10)
            ;;func () {
                broadcast ([])
            ;;}()
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 7, col 13) : (func () { broadcast [] })()\n" +
        //        " |  anon : (lin 8, col 17) : broadcast []\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold event reference\n") { out }
        //assert(out == " |  anon : (lin 7, col 13) : (func () { broadcast [] })()\n" +
        //        " |  anon : (lin 8, col 17) : broadcast []\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : broadcast'([])\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
    }

    @Test
    fun gg_02_scope() {
        val out = test(
            """
            val T = task (v) {
                val it = yield(nil)
                println(it)
            }
            spawn T(10)
            ;;func () {
                broadcast ([])
            ;;}()
        """
        )
        assert(out == "[]\n") { out }
    }

    @Test
    fun gg_02_bcast() {
        val out = test(
            """
            val T = task () {
                val e = yield(nil) ;;thus { it => it }
                println(e)
                yield(nil) ;;thus { it => nil }
            }
            spawn T()
            spawn T()
            broadcast ([])
        """
        )
        //assert(out == "[]\n" +
        //        " |  anon : (lin 9, col 13) : broadcast []\n" +
        //        " v  anon : (lin 3, col 25) : resume error : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 9, col 13) : broadcast []\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold event reference\n") { out }
        //assert(out == " |  anon : (lin 9, col 13) : broadcast'([])\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[]\n" +
        //        " |  anon : (lin 9, col 13) : broadcast'([])\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        assert(
            out == "[]\n" +
                    "[]\n"
        ) { out }
    }

    @Test
    fun gg_03_bcast() {
        val out = test(
            """
            val T = task (v) {
                yield(nil) ;;thus { it => nil }
                println(v)                
            }
            spawn T([])
            broadcast(nil)
        """
        )
        //assert(out == ":1\n10\n10\n:2\ndeclaration error : incompatible scopes\n") { out }
        assert(out == "[]\n") { out }
    }

    @Test
    fun gg_04_bcast_ok() {
        val out = test(
            """
            val T = task () {
                val e =
                    func (it) {
                        type(it)
                        it
                    } (yield(nil))
                println(e)                
            }
            spawn T()
            do {
                broadcast ([20])
            }
            println(:ok)
        """
        )
        assert(out == "[20]\n:ok\n") { out }
    }

    @Test
    fun gg_04_bcast() {
        val out = test(
            """
            var tk
            set tk = task (v) {
                println(v)
                val e1 = ${AWAIT()}
                println(e1)                
                val e2 = ${AWAIT()}
                println(e2)                
            }
            println(:1)
            var co1 = spawn (tk) (10)
            var co2 = spawn (tk) (10)
            val e = catch (it|true) {
                ;;func () {
                    println(:2)
                    broadcast ([20])
                    println(:3)
                    broadcast (@[(30,30)])
                    true
                ;;}()
            }
            println(e)
        """
        )
        //assert(out == ":1\n10\n10\n:2\n[20]\nresume error : incompatible scopes\n") { out }
        //assert(out == ":1\n" + "10\n" + "10\n" + ":2\n" +
        //        "declaration error : cannot hold event reference\n") { out }
        //assert(out == ":1\n" + "10\n" + "10\n" + ":2\n" +
        //        "block escape error : cannot copy reference out\n") { out }
        //assert(out == ":1\n" + "10\n" + "10\n" + ":2\n" +
        //        "block escape error : cannot move pending reference in\n") { out }
        assert(
            out == ":1\n" +
                    "10\n" +
                    "10\n" +
                    ":2\n" +
                    "[20]\n" +
                    "[20]\n" +
                    ":3\n" +
                    "@[(30,30)]\n" +
                    "@[(30,30)]\n" +
                    "true\n"
        ) { out }
    }

    @Test
    fun gg_05_bcast_tuple_func_no() {
        val out = test(
            """
            val f = func (v) {
                func (x) {
                    set x[0] = v[0]
                    println(x[0])
                } ([0])
            }
            var T = task () {
                f(yield(nil))
            }
            spawn T()
            broadcast ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
    }

    @Test
    fun gg_05_bcast_tuple_func_xx() {
        val out = test(
            """
            val f = func (v) {
                println(v[0])
            }
            var T = task () {
                f(yield(nil))
            }
            spawn T()
            broadcast ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
    }

    @Test
    fun gg_05_bcast_tuple_func_ok() {
        val out = test(
            """
            val f = func (v) {
                func (x) {
                    set x[0] = v[0]
                    println(x[0])
                } ([0])
            }
            var T = task () {
                f(yield(nil))
            }
            spawn T()
            broadcast ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
    }

    @Test
    fun gg_06_bcast_tuple_func_ok() {
        val out = test(
            """
            val f = func (v) {
                println(v)
            }
            val T = task () {
                f(yield(nil)) ;;thus { it => it }
            }
            spawn T()
            do {
                do {
                    do {
                        do {
                            do {
                                broadcast ([])
                            }
                        }
                    }
                }
            }
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 14, col 33) : broadcast'([])\n" +
        //        " v  anon : (lin 6, col 30) : block escape error : cannot copy reference out\n") { out }
    }

    @Test
    fun gg_07_bcast_tuple_func_ok() {
        val out = test(
            """
            val f = func (v) {
                println(v)
            }
            val T = task () {
                do {
                    f(yield(nil))
                }
            }
            spawn T()
            do {
                do {
                    do {
                        broadcast ([])
                    }
                }
            }
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 14, col 25) : broadcast'([])\n" +
        //        " v  anon : (lin 7, col 33) : block escape error : cannot copy reference out\n") { out }
    }

    // BCAST / DSTK,BSTK=NULL
    // todo_*
    //  - Option 1: should actually accept
    //  - Option 2: should at least raise an exception and not panic program

    @Test
    fun TODO_gh_01_coro_defer_bcast_err() {
        val out = test(
            """
            val f = func (v) {
                println(:4)
                broadcast(nil)
                println(:a)
            }
            spawn (task () {
                var x = coroutine(coro () {
                    println(:1)
                    defer {
                        println(:3)
                        f()
                    }
                    yield(nil)
                })
                resume x()
                set x = nil
                println(:2)
            }) ()
            println(:b)
        """
        )
        assert(out.contains(": Assertion `0 && \"TODO: cannot spawn or broadcast during abortion\"'")) { out }
    }

    @Test
    fun TODO_gh_02_coro_defer_spawn_err() {
        val out = test(
            """
            val f = func (v) {
                spawn (task () {
                    nil
                }) ()
            }
            spawn (task () {
                var x = coroutine(coro () {
                    defer {
                        f()
                    }
                    yield(nil)
                })
                resume x()
                set x = nil
            }) ()
        """
        )
        assert(out.contains(": Assertion `0 && \"TODO: cannot spawn or broadcast during abortion\"'")) { out }
    }

    // SCOPE / TUPLE / NEST

    @Test
    fun gh_01_set() {
        val out = test(
            """
            spawn( task () {
                var t = [1]
                spawn( task :nested () {
                    set t = [2]
                }) ()
                println(t)
            } )()
        """
        )
        assert(out == "[2]\n") { out }
    }

    @Test
    fun dh_02_set() {
        val out = test(
            """
            spawn( task () {
                var t = [1]
                spawn (task :nested () {
                    yield(nil) ;;thus { it => nil }
                    set t = [2]
                } )()
                yield(nil) ;;thus { it => nil }
                println(t)
            }) ()
            broadcast(nil)
        """
        )
        assert(out == "[2]\n") { out }
    }

    @Test
    fun gh_03_set() {
        val out = test(
            """
            spawn( task () {
                val t = [1]
                ;;func () {
                    set t[0] = 2
                ;;} ()
                println(t)
            }) ()
        """
        )
        assert(out == "[2]\n") { out }
    }

    @Test
    fun TODO_dh_04_set() {
        val out = test(
            """
            spawn (task () {
                var t = [1]
                spawn( task :nested () {
                    func (it) {
                        set t = copy(it)    ;; TODO: func -> nested -> task
                    } (yield(nil))
                }) ()
                yield(nil) ;;thus { it => nil }
                println(t)
            }) ()
            broadcast ([1])
        """, true
        )
        //assert(out == "[1]\n") { out }
        assert(out == "anon : (lin 6, col 29) : access error : outer variable \"t\" must be immutable\n") { out }
    }

    @Test
    fun dh_05_set() {
        val out = test(
            """
            spawn (task () {
                var t = [1]
                spawn( task :nested () {
                    set t = copy(yield(nil))
                }) ()
                yield(nil) ;;thus { it => nil }
                println(t)
            } )()
            broadcast ([1])
        """, true
        )
        assert(out == "[1]\n") { out }
    }

    @Test
    fun dh_06_set() {
        val out = test(
            """
            var ang = 0
            loop {
                ang
            }
        """
        )
        assert(out == "anon : (lin 4, col 17) : loop error : innocuous last expression\n") { out }
    }

    @Test
    fun dh_07_nst() {
        val out = test(
            """
            val T = task (t) {
                var ang = 0
                spawn (task :nested () {
                    set ang = 10
                })()
                println(ang)
            }
            spawn T()
        """
        )
        assert(out == "10\n") { out }
    }

    // STATUS

    @Test
    fun hh_01_status() {
        val out = test(
            """
            val t = spawn (task () {
                nil
            }) ()
            println(status(t))
        """
        )
        assert(out == ":terminated\n") { out }
        //assert(out == " v  anon : (lin 5, col 21) : status(t) : status error : expected running coroutine or task\n") { out }
    }

    @Test
    fun hh_02_status() {
        val out = test(
            """
            val t = spawn (task () {
                yield(nil) ;;thus { it => nil }
            }) ()
            println(status(t))
        """
        )
        assert(out == ":yielded\n") { out }
    }

    @Test
    fun hh_04_status() {            // TODO: return track for both task task_in / awake with error?
        val out = test(
            """
            val t = spawn (task () {
                yield(nil) ;;thus { it => nil }
            }) ()
            broadcast(nil)
            println(status(t))
        """
        )
        assert(out == ":terminated\n") { out }
    }

    // TASK / BCAST IN

    @Test
    fun jj_01_bcast_in_err() {
        val out = test(
            """
            broadcast (nil) in nil
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : broadcast'(nil,nil)\n" +
                    " v  broadcast error : invalid target\n"
        ) { out }
    }

    @Test
    fun jj_02_bcast_in_task() {
        val out = test(
            """
            val T = task (v) {
                ${AWAIT()}
                println(v)
            }
            val t1 = spawn T (1)
            val t2 = spawn T (2)
            broadcast (:x) in t1
        """
        )
        assert(out == "1\n") { out }
    }

    @Test
    fun jj_03_bcast_in_self() {
        val out = test(
            """
            val T = task (v) {
                ${AWAIT()}
                println(v)
                spawn( task () {
                    println(${AWAIT()})
                }) ()
                broadcast(10)
            }
            val t1 = spawn T(1)
            val t2 = spawn T(2)
            do {
                broadcast (nil) in t1
            }
        """
        )
        assert(out == "1\n10\n") { out }
    }

    // DROP / MOVE / OUT

    @Test
    fun jj_01_bcast_move() {
        val out = test(
            """
            var T = task () {
                val e = yield(nil) ;;thus { it => it}
                do {
                    var v = e
                    println(v)
                }
            }
            var t = spawn T()
            ;;println(:1111)
            var e = []
            broadcast (;;;drop;;;(e))
            println(e)
            """
        )
        assert(out == "[]\n[]\n") { out }
        //assert(out == "[]\nnil\n") { out }
        //assert(out == "anon : (lin 10, col 13) : broadcast move(e)\n" +
        //        "anon : (lin 4, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : broadcast'(drop(e))\n" +
        //        " v  anon : (lin 3, col 38) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun jj_02_task_nest() {
        val out = test(
            """
            spawn( task (v1) {
                spawn (task (v2) {
                    spawn( task (v3) {
                        println(v1,v2,v3)
                        nil
                    })(3)
                })(2)
            })(1)
        """
        )
        //assert(out == "1\t2\t3\n" +
        //        " |  anon : (lin 2, col 19) : (task (v1) { spawn (task (v2) { spawn (task (...)\n" +
        //        " |  anon : (lin 3, col 23) : (task (v2) { spawn (task (v3) { println(v1,v2...)\n" +
        //        " v  anon : (lin 3, col 33) : block escape error : cannot copy reference out\n") { out }
        assert(out == "1\t2\t3\n") { out }
    }

    // PUB

    @Test
    fun kk_00_pub_err() {
        val out = test(
            """
            pub
        """
        )
        //assert(out == " v  anon : (lin 2, col 13) : pub() : pub error : expected task\n") { out }
        assert(out == "anon : (lin 2, col 13) : pub error : expected enclosing task\n") { out }
    }
    @Test
    fun kk_00_x_pub_err() {
        val out = test("""
            task.pub
        """)
        //assert(out == "anon : (lin 2, col 18) : pub error : expected enclosing task") { out }
        //assert(out == "anon : (lin 2, col 13) : task error : missing enclosing task") { out }
        assert(out == "anon : (lin 2, col 17) : expected \"(\" : have \".\"\n") { out }
    }
    @Test
    fun kk_01_pub_err() {
        val out = test(
            """
            task :nested () {
                pub
            }
        """
        )
        //assert(out == " v  anon : (lin 2, col 13) : pub() : pub error : expected task\n") { out }
        //assert(out == "anon : (lin 3, col 17) : pub error : expected enclosing task\n") { out }
        assert(out == "anon : (lin 2, col 13) : task :nested error : expected enclosing task\n") { out }
    }
    @Test
    fun kk_02_pub_err() {
        val out = test(
            """
            nil.pub
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : nil.pub\n" +
                    " v  pub error : expected task\n"
        ) { out }
    }
    @Test
    fun kk_02_pub_err_x() {
        val out = test(
            """
            val t = task () {
                set pub = []
                pub
            }
            val a = spawn (t) ()
            val x = a.pub
            println(x)
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == " v  anon : (lin 6, col 25) : pub error : expected active task\n") { out }
        //assert(out == " |  anon : (lin 5, col 21) : (spawn t())\n" +
        //        " v  anon : (lin 2, col 29) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 5, col 21) : (spawn t())\n" +
        //        " v  anon : (lin 2, col 29) : block escape error : reference has immutable scope\n") { out }
    }
    @Test
    fun kk_03_pub() {
        val out = test(
            """
            val T = task () {
                println(pub)
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            println(t.pub)
        """
        )
        assert(out == "nil\nnil\n") { out }
    }
    @Test
    fun kk_04_pub() {
        val out = test(
            """
            val T = task () {
                set pub = 10
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            println(t.pub)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_05_pub_err() {
        val out = test(
            """
            val T = task () {
                do {
                    val x = []
                    set pub = x
                    pub
                }
            }
            val t = spawn T()
            println(t.pub)
        """
        )
        //assert(out == " |  anon : (lin 8, col 21) : (spawn T())\n" +
        //        " v  anon : (lin 5, col 25) : set error : cannot assign reference to outer scope\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun kk_06_pub_err() {
        val out = test(
            """
            val T = task () {
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            do {
                val x = []
                set t.pub = x
            }
            println(t.pub)
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == " v  anon : (lin 8, col 21) : set error : cannot hold alien reference\n") { out }
    }
    @Test
    fun kk_07_pub_tag() {
        val out = test(
            """
            data :X = [x]
            val T = task () :X {
                set pub = [10]
                println(pub.x)
            }
            spawn T()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_08_pub_tag() {
        val out = test(
            """
            data :Z = [z]
            data :X = [x:Z]
            val T = task () :X {
                set pub = [[10]]
                println(pub.x.z)
            }
            spawn T()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_09_pub_tag() {
        val out = test(
            """
            data :X = [x]
            val T = task () :X {
                set pub = [10]
                yield(nil) ;;thus { it => nil }
            }
            val t :X = spawn T()
            println(t.pub.x)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_10_pub_tag() {
        val out = test(
            """
            data :Z = [z]
            data :X = [x:Z]
            val T = task () :X {
                set pub = [[10]]
                yield(nil) ;;thus { it => nil }
            }
            val t :X = spawn T()
            println(t.pub.x.z)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_11_task_tag_err() {
        val out = test(
            """
            val T = task () :X {
                nil
            }
            println(:ok)
        """
        )
        assert(out == "anon : (lin 2, col 29) : declaration error : data :X is not declared\n") { out }
        //assert(out == ":ok\n") { out }
    }
    @Test
    fun kk_12_pub() {
        val out = test(
            """
            pub.x
        """
        )
        assert(out == "anon : (lin 2, col 13) : pub error : expected enclosing task\n") { out }
    }
    @Test
    fun kk_13_pub() {
        val out = test(
            """
            spawn (task () {
                set pub.x = 10
                nil
            }) ()
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : (spawn (task () { (set pub[:x] = 10) nil }...\n" +
                    " |  anon : (lin 3, col 21) : pub[:x]\n" +
                    " v  index error : expected collection\n"
        ) { out }
    }
    @Test
    fun kk_14_pub() {
        val out = test(
            """
            val t = spawn (task () {
                set pub = yield(nil)
                ;;set pub = evt
                pub
            }) ()
            do {
                val x
                broadcast([:x])
            }
            println(t.pub)
        """
        )
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'([],:task)\n" +
        //        " v  anon : (lin 4, col 21) : set error : cannot hold alien reference\n") { out }
        assert(out == "[:x]\n") { out }
    }
    @Test
    fun kk_15_pub() {
        val out = test(
            """
            val t = spawn (task () {
                set pub = 0
                yield(nil)
            }) ()
            set t.pub = 10
            println(t.pub)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_16_pub() {
        val out = test("""
            var t
            set t = task (v1) {
                set pub = v1
                val evt = yield(nil)
                set pub = pub + evt
                pub
            }
            var a = spawn (t) (1)
            println(a.pub)
            broadcast(2) in a
            println(a.pub)
        """, true)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun kk_17_pub_err() {
        val out = test("""
            val t = task () {
                set pub = []
            }
            var a = spawn (t) ()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 5, col 28) : t()\n" +
        //        "anon : (lin 2, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun kk_18_pub_err() {
        val out = test("""
            val T = task () {
                set pub = []
                yield(nil)
            }
            var x
            do {
                var a = spawn (T) ()
                set x = a.pub
                nil
            }
            println(x)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == "anon : (lin 11, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 8, col 32) : t()\n" +
        //        "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun kk_19_pub() {
        val out = test("""
            var t
            set t = task () {
                ;;;set pub =;;; 10
            }
            var a = spawn t()
            println(a.pub + a.pub)
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun kk_20_pub_pool() {
        val out = test("""
            var T
            set T = task () {
                set pub = [10]  ;; valgrind test
            }
            spawn T()
            println(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 6, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun kk_21_pub_pool() {
        val out = test("""
            var T
            set T = task () {
                do pub ;; useless test
                nil
            }
            spawn T()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun kk_22_pub_err() {
        val out = test("""
            var T
            set T = task () {
                set ;;;task.;;;pub = [10]
                yield(nil)
            }
            var y
            do {
                var t = spawn (T) ()
                var x
                set x = t.pub  ;; pub expose
                set y = t.pub  ;; incompatible scopes
            }
            println(999)
        """)
        assert(out == "999\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == "anon : (lin 13, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun kk_23_pub_index_err() {
        val out = test("""
            var T
            set T = task () {
                set ;;;task.;;;pub = [10]
                yield(nil)
            }
            var t = spawn(T)()
            var x
            set x = t.pub   ;; no expose
            println(x)
        """)
        assert(out == "[10]\n") { out }
        //assert(out == "anon : (lin 9, col 17) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == "anon : (lin 11, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun kk_24_pub_index_err() {
        val out = test("""
            var T
            set T = task () {
                set ;;;task.;;;pub = [10]
                yield(nil)
            }
            var t = spawn(T)()
            println(t.pub)   ;; no expose
        """)
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun kk_25_pub_index_err() {
        val out = test("""
            var T
            set T = task () {
                set ;;;task.;;;pub = [[@[(:x,10)]]]
                yield(nil)
            }
            var t = spawn T ()
            println(t.pub[0][0][:x])   ;; no expose
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 10, col 27) : invalid index : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun kk_26_hh_pub_task() {
        val out = test("""
        spawn task () { 
            var y
            set y = do {     
                var ceu_spw_54     
                set ceu_spw_54 = spawn task :nested () {         
                    yield(nil)         
                    [2]             
                }()        
                yield(nil)     
                ;;println(ceu_spw_54.pub)     
                ceu_spw_54.pub        
            }     
            println(y) 
        }()
        broadcast( nil )
        """)
        assert(out == "[2]\n") { out }
        //assert(out == "anon : (lin 16, col 9) : broadcast nil\n" +
        //        "anon : (lin 12, col 28) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun kk_27_pub_task_err() {
        val out = test("""
        spawn task () { 
            var y
            set y = do {     
                var ceu_spw_54     
                set ceu_spw_54 = spawn task () {         
                    set ;;;task.;;;pub = [2]             
                    yield(nil)         
                }()        
                ;;yield(nil)     
                ;;println(ceu_spw_54.pub)     
                ceu_spw_54.pub        
            }     
            println(y) 
        }()
        broadcast (nil)
        """)
        assert(out == "[2]\n") { out }
       // assert(out == "anon : (lin 2, col 15) : (task () :fake { var y set y = do { var ceu_s...)\n" +
       //         "anon : (lin 4, col 21) : block escape error : incompatible scopes\n" +
       //         ":error\n") { out }
        //assert(out == "anon : (lin 16, col 9) : broadcast nil\n" +
        //        "anon : (lin 12, col 28) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun kk_28_pub_err() {
        val out = test("""
            var t
            set t = task (v) {
                set ;;;task.;;;pub = v
                var evt = yield(nil)
                var x
                set x = [2]
                set ;;;task.;;;pub = x
                set evt = yield(nil)
                set ;;;task.;;;pub = @[(:y,copy(evt))]
                ;;;move;;;(;;;task.;;;pub)
            }
            var a = spawn (t) ([1])
            println(a.pub)
            broadcast(nil) in a
            println(a.pub)
            broadcast([3]) in a
            println(a.pub)
        """, true)
        assert(out == "[1]\n[2]\n@[(:y,[3])]\n") { out }
        //assert(out == "TODO\n") { out }
    }
    @Test
    fun kk_29_pub_out() {
        val out = test("""
            var t = task () {
                set ;;;task.;;;pub = []
                yield(nil)
                nil
            }
            var a = spawn (t) ()
            println(a.pub)
            a.pub
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun kk_30_pub_out_err() {
        val out = test("""
            var t = task () {
                set ;;;task.;;;pub = []
                ;;;task.;;;pub
            }
            var a = spawn (t) ()
            println(a.pub)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 6, col 28) : t()\n" +
        //        "anon : (lin 2, col 29) : block escape error : incompatible scopes\n" +
        //        ":error\n") { out }
    }

    // ORIGINAL / PUB / EXPOSE

    @Test
    fun kj_01_expose() {
        val out = test(
            """
            var t = task () {
                set pub = []
                yield(nil)
                nil
            }
            var a = spawn (t) ()
            var x = a.pub
            println(x)
        """
        )
        //assert(out == "anon : (lin 8, col 13) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == "[]\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration error : cannot copy reference out\n") { out }
    }
    @Test
    fun kj_02_expose_err() {
        val out = test(
            """
            val x = do {
                var t = task () {
                    set pub = []
                    yield(nil)
                    nil
                }
                var a = spawn (t) ()
                a.pub
            }
            println(x)
        """
        )
        //assert(out == " v  anon : (lin 2, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 2, col 21) : block escape error : reference has immutable scope\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun kj_03_expose_err() {
        val out = test(
            """
            var f = func (t) {
                do { do {
                var p = t.pub   ;; ok
                set p = t.pub   ;; ok  ;;ERROR: spawn A and f() are not comparable
                set p = p       ;; ok
                println(p)      ;; ok
                ;;p               ;; ok
                nil
                } }
            }
            var t = task () {
                set pub = []
                yield(nil) ;;thus { it => nil }
                nil
            }
            var a = spawn (t) ()
            f(a)
            nil
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 15, col 13) : f(a)\n" +
        //        "anon : (lin 2, col 30) : block escape error : incompatible scopes\n" +
        //        "[]\n" +
        //        ":error\n") { out }
        //assert(out == "anon : (lin 15, col 13) : f(a)\n" +
        //        "anon : (lin 3, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 18, col 13) : f(a)\n" +
        //        " v  anon : (lin 4, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 18, col 13) : f(a)\n" +
        //        " v  anon : (lin 5, col 21) : set error : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun kj_05_expose_err() {
        val out = test(
            """
            var f = func (t) {
                var p = t.pub   ;; ok
                p               ;; ok
            }
            var t = task () {
                set pub = []
                yield(nil) ;;thus { it => nil }
                nil
            }
            var a = spawn (t) ()
            var x = f(a)        ;; no
            println(x)
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 12, col 21) : f(a)\n" +
        //        "anon : (lin 2, col 30) : block escape error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == "anon : (lin 12, col 21) : f(a)\n" +
        //        "anon : (lin 3, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 12, col 21) : f(a)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 12, col 21) : f(a)\n" +
        //        " v  anon : (lin 2, col 30) : block escape error : reference has immutable scope\n") { out }
    }
    @Test
    fun kj_06_expose_err() {
        val out = test(
            """
            var p
            var f = func (t) {
                set p = t.pub   ;; no
            }
            var t = task () {
                set pub = []
                yield(nil) ;;thus { it => nil }
                nil
            }
            var a = spawn (t) ()
            val x = f(a)
            println(x)
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 12, col 13) : f(a)\n" +
        //        "anon : (lin 4, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 12, col 21) : f(a)\n" +
        //        " v  anon : (lin 4, col 21) : set error : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun TODO_kj_07_pub_func() {
        val out = test(
            """
            var t
            set t = task (v) {
                set pub = v
                var f
                set f = func () {
                    pub           ;; TODO: crosses func
                }
                println(f())
            }
            var a = spawn (t)(1)
        """
        )
        //assert(out == "1\n") { out }
        assert(out == "anon : (lin 7, col 21) : pub error : expected enclosing task\n") { out }
    }
    @Test
    fun TODO_kj_08_pub_func_expose() {
        val out = test(
            """
            var t = task (v) {
                set pub = v
                var f = func () {
                    pub   ;; should be ok bc f is called inside t
                }
                println(f())
            }
            var a = spawn (t) ([1])
        """
        )
        assert(out == "anon : (lin 5, col 21) : pub error : expected enclosing task\n") { out }
        //assert(out == "[1]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun kj_09_pub() {
        val out = test(
            """
            var T = task (v) {
                set pub = @[]
                nil
            }
            val t = spawn T()
            println(status(t))
        """
        )
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun kj_10_pub_expose() {
        val out = test(
            """
            val f = func (t) {
                println(t)
            }
            val T = task () {
                set pub = []
                yield(nil)
                nil
            }
            val t = spawn T()
            f(t.pub)
            println(:ok)
        """
        )
        assert(out == "[]\n:ok\n") { out }
    }
    @Test
    fun kj_12_pub_pass() {
        val out = test("""
            val S = task (t) {
                println(t)
                yield(nil)
            }
            val T = task () {
                set ;;;task.;;;pub = [1,2,3]
                spawn S(;;;task.;;;pub)
                nil
            }
            spawn T()
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun kj_13_pub_err_expose() {
        val out = test("""
            var t
            set t = task () {
                set pub = []
                pub
            }
            var a = spawn (t) ()
            var x
            set x = a.pub   ;; seria bom que o pub guardasse o retorno,
                            ;; uma vez que ele já se "desfez" (gc_dec) do pub
                            ;; poderia ser uma union pub/ret
            println(x)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 6, col 28) : t()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun kj_14_pub_err_expose() {
        val out = test("""
            var t
            set t = task () {
                set ;;;task.;;;pub = @[]
                pub
            }
            var a = spawn (t) ()
            var x
            set x = a.pub
            println(x)
        """)
        assert(out == "@[]\n") { out }
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 6, col 28) : t()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun kj_15_pub_err_expose() {
        val out = test("""
            var t
            set t = task () {
                set ;;;task.;;;pub = #[]
                pub
            }
            var a = spawn (t) ()
            var x
            set x = a.pub
            println(x)
        """)
        assert(out == "#[]\n") { out }
        //assert(out == "anon : (lin 6, col 28) : t()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun kj_16_pub_nopub() {
        val out = test("""
            var U
            set U = task () {
                set pub = func () {
                    10
                }
                pub
            }
            var T
            set T = task (u) {
                println(type(u.pub))
            }
            spawn T (spawn U())
            println(:ok)
        """)
        assert(out == ":func\n:ok\n") { out }
        //assert(out == "anon : (lin 12, col 28) : U()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun kj_17_pub_nopub() {
        val out = test("""
            var U
            set U = task () {
                set ;;;task.;;;pub = [10]
            }
            var T
            set T = task (u) {
                nil ;;println(u.pub.0)
            }
            spawn T (spawn U())
            println(:ok)
        """)
        //assert(out == "anon : (lin 10, col 28) : U()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun kj_18_pub_nopub() {
        val out = test("""
            var U
            set U = task () {
                var x
                set x = [10]
            }
            var T
            set T = task (u) {
                nil ;;println(u.pub.0)
            }
            spawn T (spawn U())
            println(:ok)
        """)
        //assert(out == "anon : (lin 11, col 28) : U()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun kj_19_func_expose() {
        val out = test("""
            var t = task (v) {
                set ;;;task.;;;pub = v
                var f = func (p) {
                    p
                }
                println(f(;;;task.;;;pub))
            }
            var a = spawn (t) ([1])
        """, true)
        assert(out == "[1]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun kj_20_pub_func_tst() {
        val out = test("""
            var t = task (v) {
                set ;;;task.;;;pub = v
                var xxx
                nil
            }
            var a = spawn (t) ([])
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun kj_21_pub_func_expose() {
        val out = test("""
            var f = func (t) {
                t.pub
            }
            var T = task () {
                set ;;;task.;;;pub = []
                yield(nil)
            }
            var t = spawn (T) ()
            println(f(t))
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        //assert(out == "anon : (lin 10, col 21) : f(t)\n" +
        //        "anon : (lin 2, col 30) : block escape error : incompatible scopes\n" +
        //        ":error\n") { out }
    }

    // PUB / DATA

    @Test
    fun BUG_ki_01_data_pub() {
        val out = test("""
            data :T = [x,y]
            var t :T = spawn task () {
                set ;;;task.;;;pub = [10,20]
                nil
            } ()
            data :X = [a:T]
            var x :X = [t]
            println(x.a.pub.y)  // TODO: combine Pub/Index
        """, true)
        assert(out == "20\n") { out }
    }

    // NESTED

    @Test
    fun ll_00_nested() {
        val out = test(
            """
            task :nested () {
                nil
            }
            println(:ok)
        """
        )
        assert(out == "anon : (lin 2, col 13) : task :nested error : expected enclosing task\n") { out }
    }
    @Test
    fun ll_01_nested() {
        val out = test(
            """
            spawn (task () {
                val v = 10
                spawn( task () {
                    println(v)
                }) ()
                yield(nil) ;;thus { it => nil }
            }) ()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_02_nested() {
        val out = test(
            """
            val F = func () {
                val v = 10
                val f = func () {
                    v
                }
                f()
            }
            println(F())
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_03_nested() {
        val out = test(
            """
            spawn (task () {
                var xxx = 1
                yield(nil)
                spawn(task :nested () {
                    set xxx = 10
                }) ()
                println(xxx)
            } )()
            broadcast(nil)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_04_nested() {
        val out = test(
            """
            spawn (task () {
                var xxx = 1
                yield(nil)
                spawn( task :nested () {
                    set xxx = 10
                }) ()
                println(xxx)
            } )()
            do {
                broadcast(nil)
            }
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_05_nested() {
        val out = test(
            """
            spawn( task () {
                val t = []
                spawn (task () {
                    yield(nil) ;;thus { it => nil }
                    println(t)
                }) ()
                yield(nil)
                nil
            }) ()
            coroutine(coro () { nil })
            broadcast(nil)
       """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun ll_06_upv() {
        val out = test(
            """
            do {
                val v = 10
                 spawn (task () {
                    println(v)
                }) ()
            }
        """
        )
        //assert(out == "anon : (lin 5, col 29) : access error : cannot access local across coro or task\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_07_task_up_task() {
        val out = test(
            """
            spawn (task () {
                do {
                    spawn (task () {
                        yield(nil)
                    }) ()
                    yield(nil)
                    nil
                }
                broadcast([])
            })()
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ll_08_nested_err() {
        val out = test(
            """
            spawn (task () {
                var xxx = 1
                yield(nil)
                spawn(task () {     ;; ERROR: crosses non :nested
                    spawn(task :nested () {
                        set xxx = 10
                    }) ()
                }) ()
                println(xxx)
            } )()
            broadcast(nil)
        """
        )
        //assert(out == "ERROR\n") { out }
        assert(out == "anon : (lin 7, col 29) : access error : outer variable \"xxx\" must be immutable\n") { out }
    }
    @Test
    fun ll_09_nested_err() {
        val out = test(
            """
            spawn (task :nested () {
                nil
            } )()
        """
        )
        assert(out == "anon : (lin 2, col 20) : task :nested error : expected enclosing task\n") { out }
    }
    @Test
    fun ll_10_nested() {
        val out = test(
            """
            spawn (task () :X {
                set pub.x = nil
            }) ()
            println(:ok)
        """
        )
        assert(out == "anon : (lin 2, col 28) : declaration error : data :X is not declared\n") { out }
    }
    @Test
    fun BUG_ll_11_bcast_in() {  // bcast in outer of :nested
        val out = test(
            """
            var T
            set T = task (v) {
                spawn (task :nested () {
                    val evt = yield(nil)
                    println(v, evt)
                }) ()
                spawn (task :nested () {
                    do {
                        broadcast(:ok) in :task
                    }
                }) ()
                yield(nil)
                println(:err)
            }
            spawn (task () {
                yield(nil)
                println(:err)
            }) ()
            spawn T (1)
            spawn T (2)
        """
        )
        assert(out == "1\t:ok\n2\t:ok\n") { out }
    }
    @Test
    fun ll_12_pub_fake_task() {
        val out = test("""
            spawn (task () {
                set pub = 1
                spawn (task :nested () {
                    println(pub)
                }) ()
                nil
            }) ()
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun ll_13_pub_fake_task_err() {
        val out = test("""
            spawn (task () {
                set pub = []
                var x
                spawn (task :nested () {
                    set x = pub
                }) ()
                println(x)
            }) ()
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 2, col 20) : task () { set task.pub = [] var x spa...)\n" +
        //        "anon : (lin 5, col 24) : task () :fake { set x = task.pub }()\n" +
        //        "anon : (lin 6, col 34) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        //assert(out == "anon : (lin 2, col 20) : task () { set task.pub = [] var x spawn task ...)\n" +
        //        "anon : (lin 5, col 24) : task () :fake { set x = task.pub }()\n" +
        //        "anon : (lin 6, col 25) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun ll_14_pub_fake_task() {
        val out = test("""
            spawn (task () {
                set pub = [10]
                var x
                spawn (task :nested () {
                    set x = pub[0]
                }) ()
                println(x)
            }) ()
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_15_pub_fake_err() {
        val out = test("""
            spawn (task :nested () {
                pub
            }) ()
        """, true)
        //assert(out == "anon : (lin 3, col 22) : pub error : expected enclosing task") { out }
        //assert(out == "anon : (lin 3, col 17) : task error : missing enclosing task") { out }
        assert(out == "anon : (lin 2, col 20) : task :nested error : expected enclosing task\n") { out }
    }
    @Test
    fun ll_16_xceu3() {
        val out = test("""
            spawn task () {
                var evt = yield(nil)
                println(evt)
                spawn (task :nested () {
                    loop {
                        println(evt)    ;; kept reference
                        set evt = yield(nil)
                    }
                }) ()
                set evt = yield(nil)
            }()
            broadcast (10)
            broadcast (20)
        """)
        assert(out == "10\n10\n20\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }

    // NESTED / BCAST / THROW

    @Test
    fun lm_01_bcast_err() {
        val out = test("""
            broadcast(nil) in :task
            println(:ok)
        """)
        //assert(out == "anon : (lin 2, col 26) : broadcast error : invalid target\n:error\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun lm_02_bcast_err() {
        val out = test("""
            spawn (task :nested () {
                broadcast(nil) in :task
            }) ()
            println(:ok)
        """)
        //assert(out == "anon : (lin 2, col 20) : (task () :fake { broadcast in :task, nil })()\n" +
        //        "anon : (lin 3, col 30) : broadcast error : invalid target\n:error\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == "anon : (lin 2, col 20) : task :nested error : expected enclosing task\n") { out }
    }
    @Test
    fun lm_03_throw_fake() {
        val out = test("""
            spawn (task () {
                catch (err|err==:err) {
                    spawn (task :nested () {
                        error(:err)
                    }) ()
                }
                println(10)
            }) ()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun lm_04_throw_fake() {
        val out = test("""
            spawn task () { 
                catch (err|{{==}}(err,:err)) {
                    spawn (task :nested () {
                        yield(nil)
                        error(:err)
                    })()
                    yield(nil)
                }
            }() 
            broadcast(nil)
            println(10)
        """)
        assert(out == "10\n") { out }
    }

    // ABORTION

    @Test
    fun mm_00a_abortion() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    nil
                } )()
            } )()
            println(:ok)
       """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun mm_00b_abortion() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    defer {
                        println(:defer)
                    }
                } )()
            } )()
       """
        )
        assert(out == ":defer\n") { out }
    }
    @Test
    fun mm_00c_abortion() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    defer {
                        println(:defer)
                    }
                    yield(nil)
                } )()
                nil
            } )()
       """
        )
        assert(out == ":defer\n") { out }
    }
    @Test
    fun mm_01_abortion() {
        val out = test(
            """
            spawn (task () {
                println(:1)
                do {
                    println(:2)
                    spawn (task () {
                        defer {
                            println(:defer)
                        }
                        yield(nil)
                    } )()
                    println(:3)
                }
                println(:4)
            } )()
       """
        )
        assert(out == ":1\n:2\n:3\n:defer\n:4\n") { out }
    }

    @Test
    fun mm_02_abortion() {
        val out = test(
            """
            $PLUS
            println(:1)
            var x = 0
            loop {
                break if x == 2
                set x = x + 1
                println(:2)
                spawn( task () {
                    defer {
                        println(:defer)
                    }
                    yield(nil) ;;thus { it => nil }
                }) ()
                println(:3)
            }
            println(:4)
       """
        )
        assert(out == ":1\n:2\n:3\n:defer\n:2\n:3\n:defer\n:4\n") { out }
    }

    @Test
    fun mm_03_self() {
        val out = test(
            """
            spawn (task () {
                val t = spawn( task () {
                    yield(nil) ;;thus { it => nil }
                }) ()
                yield (nil)
                nil
            } )()
            broadcast(nil)
            println(:ok)
       """
        )
        assert(out == ":ok\n") { out }
    }

    @Test
    fun mm_04_self() {
        val out = test(
            """
            do {
                spawn (task () {
                    do {
                        val t1 = spawn (task () {
                            val t2 = spawn (task () {
                                yield(nil) ;;thus { it => nil }
                                println(:1)
                            }) ()
                            ${AWAIT("it==t2")}
                            println(:2)
                        }) ()
                        ${AWAIT("it==t1")}
                        println(:3)
                    }
                    ${AWAIT("it==:X")}
                    println(:99)
                }) ()
                println(:0)
                broadcast(nil)
                println(:4)
            }
       """
        )
        assert(out == ":0\n:1\n:2\n:3\n:4\n") { out }
    }

    @Test
    fun mm_04a_self() {
        val out = test(
            """
            spawn (task () {
                ;;println(:x, `:number ceu_depth(ceu_block)`)
                do {
                    spawn (task () {
                        yield(nil)
                    }) ()
                    yield(nil)
                    nil
                }
                ;;println(:y, `:number *ceu_dmin`)
                do {
                    val x1
                    do {
                        val x2
                        ;;println(:z, `:number *ceu_dmin`, `:number ceu_depth(ceu_block)`)
                        yield(nil)
                    }
                }
            }) ()
            broadcast(nil)
            println(:ok)
       """
        )
        assert(out == ":ok\n") { out }
    }

    @Test
    fun mm_04b_self() {
        val out = test(
            """
            spawn (task () {
                do {
                    spawn (task () {
                        spawn (task () {
                            yield(nil)
                            println(:1)
                        }) ()
                        yield(nil)
                        println(:2)
                    }) ()
                    yield(nil)
                    println(:3)
                }
                println(:4)
                yield(nil)
                println(:5)
            }) ()
            println(:0)
            broadcast(nil)
            println(:6)
       """
        )
        assert(out == ":0\n:1\n:2\n:3\n:4\n:5\n:6\n") { out }
    }

    @Test
    fun mm_05_defer() {
        val out = test(
            """
            task () {
                defer {
                    yield(nil) ;;thus { it => nil }   ;; no yield inside defer
                }
            }
            println(1)
        """
        )
        assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing defer\n") { out }
    }

    @Test
    fun mm_06_bcast_term() {
        val out = test(
            """
            spawn (task () {
                val T = task () {
                    println(:1)
                    yield(nil) ;;thus { it => nil }
                    println(:a)
                    yield(nil) ;;thus { it => nil }
                }
                val t = spawn T()
                spawn( task () {
                    println(:2)
                    yield(nil) ;;thus { it => nil }
                    println(:b)
                    broadcast(nil) in t     ;; pending
                    println(999)
                } )()
                println(:3)
                yield(nil) ;;thus { it => nil }
                println(:ok)
            })()
            broadcast(nil)
        """
        )
        assert(out == ":1\n:2\n:3\n:a\n:b\n:ok\n") { out }
    }

    @Test
    fun mm_06x_bcast_term() {
        val out = test(
            """
            spawn (task () {
                val T = task () {
                    yield(nil) ;;thus { it => nil }
                    yield(nil) ;;thus { it => nil }
                }
                val t = spawn T()
                spawn( task () {
                    yield(nil) ;;thus { it => nil }
                    broadcast(nil) in t
                    println(999)
                } )()
                yield(nil) ;;thus { it => nil }
                println(:ok)
            })()
            broadcast(nil)
        """
        )
        assert(out == ":ok\n") { out }
    }

    @Test
    fun mm_07_bcast_term() {
        val out = test(
            """
            val T = task () {
                yield(nil) ;;thus { it => nil }
            }
            val t = spawn T()
            ;;spawn( task () {
                spawn (task () {
                    println(:A)
                    func (it) { println(it==t) } (yield(nil))
                    println(:C)
                }) ()
                broadcast(nil) in t
            ;;})()
            println(:ok)
        """
        )
        assert(out == ":A\ntrue\n:C\n:ok\n") { out }
    }

    @Test
    fun mm_08_defer_loop() {
        val out = test(
            """
            val T = task () {
                println(:1)
                defer {
                    println(:ok)
                }
                println(:2)
                loop {
                    yield(nil)
                }
                println(999)
            }
            spawn T()
        """
        )
        assert(out == ":1\n:2\n:ok\n") { out }
    }

    // NEW ABORTION

    @Test
    fun mo_01_abort() {
        val out = test(
            """
            spawn (task () {
                ;;yield(nil)
                do {
                    spawn (task () {
                        yield(nil)
                    }) ()
                    yield(nil)
                    nil
                }
                broadcast([])
            })()
            ;;broadcast(nil)
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    @Test
    fun mo_02_global_abort() {
        val out = test(
            """
            spawn (task () {
                yield(nil)
                do {
                    spawn (task () {
                        yield(nil)
                        broadcast(nil) in :global   ;; TODO
                    }) ()
                    yield(nil)
                }
                broadcast(nil)
            })()
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    @Test
    fun mo_04_bcast_term() {
        val out = test(
            """
            spawn (task () {
                val T = task () {
                    println(:1)
                    yield(nil)
                    println(:a)
                    yield(nil)
                }
                val t = spawn T()
                spawn( task () {
                    println(:2)
                    yield(nil)
                    println(:b)
                    broadcast(nil) in t
                    println(999)
                } )()
                println(:3)
                yield(nil)
                println(:ok)
            })()
            broadcast(nil)
        """
        )
        assert(out == ":1\n:2\n:3\n:a\n:b\n:ok\n") { out }
    }

    @Test
    fun mo_05_bcast_term() {
        val out = test(
            """
            spawn (task () {
                val T = task () {
                    yield(nil)
                    yield(nil)
                    println(:2)
                }
                val t = spawn T()
                spawn( task () {
                    yield(nil)
                    do {
                        println(:1)
                        broadcast(nil) in t
                        println(:no)
                    }
                    println(:no)
                } )()
                yield(nil)
                println(:3)
            })()
            println(:0)
            broadcast(nil)
            println(:4)
        """
        )
        assert(out == ":0\n:1\n:2\n:3\n:4\n") { out }
    }

    @Test
    fun mo_06_bcast_term() {
        val out = test(
            """
            spawn (task () {
                val T = task () {
                    yield(nil)
                    yield(nil)
                    println(:2)
                }
                val t = spawn T()
                spawn( task () {
                    yield(nil)
                    do {
                        defer {
                            println(:ok)
                        }
                        println(:1)
                        broadcast(nil) in t
                        println(:no)
                    }
                    println(:no)
                } )()
                yield(nil)
                println(:3)
            })()
            println(:0)
            broadcast(nil)
            println(:4)
        """
        )
        assert(out == ":0\n:1\n:2\n:3\n:ok\n:4\n") { out }
    }

    @Test
    fun mo_07_bcast_term() {
        val out = test(
            """
            spawn (task () {
                val T = task () {
                    yield(nil)
                    yield(nil)
                    println(:2)
                }
                val t = spawn T()
                spawn( task () {
                    yield(nil)
                    do {
                        defer {
                            println(:ok)
                        }
                        println(:1)
                        (func () {
                            broadcast(nil) in t
                        }) ()
                        println(:no)
                    }
                    println(:no)
                } )()
                yield(nil)
                println(:3)
            })()
            println(:0)
            broadcast(nil)
            println(:4)
        """
        )
        assert(out == ":0\n:1\n:2\n:3\n:ok\n:4\n") { out }
    }

    @Test
    fun BUG_mo_08_bcast_func_defer() {
        val out = test(
            """
            val f = func (t) {
                defer {
                    println(:ok)    ;; TODO: aborted func should execute defer
                }
                println(:1)
                broadcast(nil) in t
                println(:no)
            }

            spawn (task () {
                val T = task () {
                    yield(nil)
                    yield(nil)
                    println(:2)
                }
                var t = spawn T()
                spawn( task () {
                    yield(nil)
                    do {
                        f(t)
                        println(:no)
                    }
                    println(:no)
                } )()
                yield(nil)
                println(:3)
            })()
            println(:0)
            broadcast(nil)
            println(:4)
        """
        )
        assert(out == ":0\n:1\n:2\n:3\n:ok\n:4\n") { out }
    }

    // ABORTION / GLOBAL BCAST

    @Test
    fun mp_01_abort() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    do {
                        defer {
                            println(:3)
                        }
                        println(:1)
                        broadcast(nil) in :global
                        println(:999)
                    }
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
        """
        )
        assert(out == ":1\n:2\n:3\n") { out }
    }

    @Test
    fun mp_01y_abort() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    println(:1)
                    broadcast(nil) in :global
                    println(:999)
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
            println(:3)
        """
        )
        assert(out == ":1\n:2\n:3\n") { out }
    }

    @Test
    fun mp_01x_abort() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    broadcast(nil) in :global
                }) ()
                yield(nil)
            }) ()
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    @Test
    fun BUG_mp_02_bcast_func_defer() {
        val out = test(
            """
            val f = func () {
                do {
                    defer {
                        println(:4)    ;; TODO: aborted func should execute defer
                    }
                    println(:1)
                    broadcast(nil) in :global
                    println(:999)
                }
            }
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    do {
                        defer {
                            println(:3)
                        }
                        f()
                        println(:999)
                    }
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
        """
        )
        assert(out == ":1\n:2\n:3\n:4\n") { out }
    }

    @Test
    fun mp_03_abort() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    do {
                        defer {
                            println(:3)
                        }
                        println(:1)
                        resume (coroutine (coro () {
                            broadcast(nil) in :global
                        })) ()
                        println(:999)
                    }
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
        """
        )
        assert(out == ":1\n:2\n:3\n") { out }
    }

    @Test
    fun mp_04_abort() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    do {
                        defer {
                            println(:3)
                        }
                        println(:1)
                        spawn (task () {
                            broadcast(nil) in :global
                        }) ()
                        println(:999)
                    }
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
        """
        )
        assert(out == ":1\n:2\n:3\n") { out }
    }

    @Test
    fun BUG_mp_05_bcast_func_defer() {
        val out = test(
            """
            val f = func () {
                do {
                    defer {
                        println(:4)    ;; TODO: aborted func should execute defer
                    }
                    println(:1)
                    resume (coroutine(coro () {
                        broadcast(nil) in :global
                    })) ()
                    println(:y999)
                }
            }
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    do {
                        defer {
                            println(:3)
                        }
                        f()
                        println(:x999)
                    }
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
        """
        )
        assert(out == ":1\n:2\n:3\n:4\n") { out }
    }

    @Test
    fun BUG_mp_05_bcast_func_leak() {
        val out = test(
            """
            val f = func () {
                val x = []
                broadcast(nil) in :global
            }
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    f()
                    println(:nooo)
                }) ()
                yield(nil)
            }) ()
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    @Test
    fun BUG_mp_06_bcast_func_defer() {
        val out = test(
            """
            val f = func () {
                do {
                    defer {
                        println(:4)    ;; TODO: aborted func should execute defer
                    }
                    println(:1)
                    spawn (task () {
                        broadcast(nil) in :global
                    }) ()
                    println(:y999)
                }
            }
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    do {
                        defer {
                            println(:3)
                        }
                        f()
                        println(:x999)
                    }
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
        """
        )
        assert(out == ":1\n:2\n:3\n:4\n") { out }
    }

    @Test
    fun mp_07_abort() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    do {
                        resume (coroutine (coro () {
                            do {
                                defer {
                                    println(:3)
                                }
                                println(:1)
                                broadcast(nil) in :global
                            }
                        })) ()
                        println(:999)
                    }
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
        """
        )
        assert(out == ":1\n:2\n:3\n") { out }
    }

    @Test
    fun mp_08_abort() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    do {
                        spawn (task () {
                            do {
                                defer {
                                    println(:3)
                                }
                                println(:1)
                                broadcast(nil) in :global
                            }
                        }) ()
                        println(:999)
                    }
                }) ()
                yield(nil)
                println(:2)
            }) ()
            broadcast(nil) in :global
        """
        )
        assert(out == ":1\n:2\n:3\n") { out }
    }

    // TASK / CORO / ABORT

    @Test
    fun mq_01_abort() {
        val out = test(
            """
            spawn (task () {
                println(:1)
                resume (coroutine (coro () {
                    defer {
                        println(:ok)
                    }
                    println(:2)
                    yield(nil)
                    println(:999)
                })) ()
                ;; refs=0 --> :ok
                println(:3)
            }) ()
        """
        )
        assert(out == ":1\n:2\n:ok\n:3\n") { out }
    }

    @Test
    fun mq_01x_abort() {
        val out = test(
            """
            spawn (task () {
                println(:1)
                val co = (coroutine (coro () {
                    defer {
                        println(:ok)
                    }
                    println(:2)
                    yield(nil)
                    println(:999)
                }))
                resume co ()
                println(:3)
            }) ()
        """
        )
        assert(out == ":1\n:2\n:3\n:ok\n") { out }
    }

    @Test
    fun mq_02_abort() {
        val out = test(
            """
            spawn (task () {
                println(:1)
                resume (coroutine (coro () {
                    defer {
                        println(:ok)
                    }
                    println(:2)
                    yield(nil)
                })) ()
                ;; refs=0 --> :ok
                yield(nil)
                println(:3)
            }) ()
            broadcast(nil)
            println(:4)
        """
        )
        assert(out == ":1\n:2\n:ok\n:3\n:4\n") { out }
    }

    @Test
    fun mq_03_abort() {
        val out = test(
            """
            spawn (task () {
                println(:1)
                spawn (task () {
                    println(:2)
                    yield(nil)
                    println(:6)
                    broadcast(nil) in :global
                }) ()
                resume (coroutine (coro () {
                    defer {
                        println(:ok)
                    }
                    println(:3)
                    yield(nil)
                })) ()
                println(:4)
                yield(nil)
                println(:7)
            }) ()
            println(:5)
            broadcast(nil)
            println(:8)
        """
        )
        assert(out == ":1\n:2\n:3\n:ok\n:4\n:5\n:6\n:7\n:8\n") { out }
    }

    // TASK / VOID

    @Test
    fun nn_01_anon() {
        val out = test(
            """
            data :X = [x]
            val T = task () :X {
                set pub = [10]
                spawn (task :nested () {
                    println(pub.x)
                }) ()
                nil
            }
            spawn T()
       """
        )
        assert(out == "10\n") { out }
    }

    @Test
    fun nn_02_anon() {
        val out = test(
            """
            spawn (task () {
                do {
                    println(:xxx, pub)
                    spawn (task :nested () {
                        println(:yyy, pub)
                        yield(nil) ;;thus { it => nil }
                    }) ()
                    yield(nil) ;;thus { it => nil }
                }
                yield(nil) ;;thus { it => nil }
            }) ()
       """
        )
        assert(out == ":xxx\tnil\n:yyy\tnil\n") { out }
    }

    @Test
    fun nn_03_anon() {
        val out = test(
            """
            var T
            set T = task () {
                spawn (task :nested () {
                    println(1)
                    nil
                }) ()
                nil
            }
            spawn T()
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 8, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n" +
        //        "1\n" +
        //        ":error\n") { out }
    }

    @Test
    fun nn_04_anon() {
        val out = test(
            """
            var T
            set T = task () {
                spawn (task :nested () {
                    (999)
                })()
                nil
            }
            spawn T()
            println(1)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 8, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }

    //  MEM-GC-REF-COUNT

    @Test
    fun oo_01_gc_bcast() {
        DEBUG = true
        val out = test(
            """
            broadcast([])
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "0\n") { out }
    }

    @Test
    fun oo_02_gc_bcast() {
        DEBUG = true
        val out = test(
            """
            var tk = task () {
                func (it) {
                    do {
                        val xxx = it
                        nil
                    }
                } (yield(nil))
                nil
                ;;println(:out)
            }
            var co = spawn tk ()
            broadcast ([])
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 11, col 13) : broadcast []\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }

    @Test
    fun oo_03_gc_bcast() {
        DEBUG = true
        val out = test(
            """
            var tk = task () {
                do {
                    func (it) {
                        var v = it
                        nil
                    } (yield(nil))
                }
                nil
                ;;println(:out)
            }
            var co = spawn tk ()
            broadcast( [])
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 11, col 13) : broadcast []\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }

    @Test
    fun oo_04_gc_bcast() {
        DEBUG = true
        val out = test(
            """
            var tk = task () {
                do {
                    func (it) {
                        do {
                            var v = it
                            nil
                        }
                    } (yield(nil))
                }
                nil
                ;;println(:out)
            }
            var co = spawn tk ()
            broadcast ([] )
            println(`:number CEU_GC.free`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 11, col 13) : broadcast []\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }

    @Test
    fun oo_05_gc() {
        DEBUG = true
        val out = test(
            """
            spawn (task () {    ;; not gc'd b/c task remains in memory
                nil             ;; see task in @ ab_01_spawn
            }) ()
            println(`:number CEU_GC.free`)
            """
        )
        assert(out == "0\n") { out }
    }

    // DEPTH

    /*
    @Test
    fun op_01_depth() {
        DEBUG = true
        val out = test("""
            println(`:number ceu_depth(&CEU_BLOCK)`)
            println(`:number ceu_depth(ceu_block)`)
        """)
        //assert(out == "0\n0\n") { out }
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun op_02_depth() {
        DEBUG = true
        val out = test("""
            println(`:number ceu_depth(&CEU_BLOCK)`)
            spawn (task () {
                do {
                    val x
                    println(`:number ceu_depth(ceu_block)`)
                }
            }) ()
        """)
        //assert(out == "0\n1\n") { out }
        assert(out == "0\n3\n") { out }
    }
    @Test
    fun op_03_depth() {
        DEBUG = true
        val out = test("""
            println(`:number ceu_depth(&CEU_BLOCK)`)
            val f = func () {
                println(`:number ceu_depth(ceu_block)`)
            }
            println(`:number ceu_depth(ceu_block)`)
            do {
                val x
                println(`:number ceu_depth(ceu_block)`)
                f()
            }
        """)
        assert(out == "0\n1\n2\n3\n") { out }
        //assert(out == "0\n0\n0\n0\n") { out }
    }
    @Test
    fun op_04_depth() {
        DEBUG = true
        val out = test("""
            val f = func () {
                println(`:number ceu_depth(ceu_block)`)
                do {
                    val x
                    println(`:number ceu_depth(ceu_block)`)
                }
            }
            println(`:number ceu_depth(&CEU_BLOCK)`)
            println(`:number ceu_depth(ceu_block)`)
            do {
                val x
                println(`:number ceu_depth(ceu_block)`)
                f()
            }
        """)
        assert(out == "0\n1\n2\n3\n4\n") { out }
        //assert(out == "0\n0\n0\n0\n0\n") { out }
    }
    @Test
    fun op_05_depth() {
        DEBUG = true
        val out = test("""
            val f = func () {
                println(`:number ceu_depth(ceu_block)`)
            }
            spawn (task () {
                f()
            }) ()
        """)
        //assert(out == "1\n") { out }
        assert(out == "3\n") { out }
    }
    @Test
    fun op_06_depth() {
        DEBUG = true
        val out = test("""
            val f = func () {
                println(`:number ceu_depth(ceu_block)`)
                do {
                    val x
                    println(`:number ceu_depth(ceu_block)`)
                }
            }
            println(`:number ceu_depth(&CEU_BLOCK)`)
            println(`:number ceu_depth(ceu_block)`)
            do {
                val x
                println(`:number ceu_depth(ceu_block)`)
                spawn (task () {
                    println(`:number ceu_depth(ceu_block)`)
                    f()
                    println(`:number ceu_depth(ceu_block)`)
                }) ()
                println(`:number ceu_depth(ceu_block)`)
            }
        """)
        assert(out == "0\n1\n2\n3\n4\n5\n3\n2\n") { out }
        //assert(out == "0\n0\n0\n1\n1\n1\n1\n0\n") { out }
    }
    @Test
    fun op_07_depth() {
        val out = test("""
            ;;println(`:number *ceu_dmin`)
            do {
                println(:1)
            }
            ;;println(`:number *ceu_dmin`)
            do {
                println(:2)
                println(:3)
            }
        """)
        //assert(out == ("255\n:1\n255\n:2\n:3\n")) { out }
        assert(out == (":1\n:2\n:3\n")) { out }
        //assert(out == ("0\n:1\n0\n:2\n:3\n")) { out }
    }
    @Test
    fun op_07x_depth() {
        DEBUG = true
        val out = test("""
            println(;;;`:number *ceu_dmin`,;;; `:number ceu_depth(ceu_block)`)
            do {
                println(;;;`:number *ceu_dmin`,;;; `:number ceu_depth(ceu_block)`)
            }
            println(;;;`:number *ceu_dmin`,;;; `:number ceu_depth(ceu_block)`)
            println(:ok)
        """)
        //assert(out == ("255\t1\n255\t1\n255\t1\n:ok\n")) { out }
        assert(out == ("1\n1\n1\n:ok\n")) { out }
        //assert(out == ("0\n:1\n0\n:2\n:3\n")) { out }
    }
    @Test
    fun op_08_depth() {     // catch from nesting and broadcast
        val out = test(
            """
            spawn (task () {
                do {
                    spawn (task () {
                        yield(nil)
                        println(:2)
                    }) ()
                    loop { yield(nil) } ;;thus { it => nil }
                }
                println(333)
            }) ()
            do {
                println(:1)
                broadcast(nil)
                println(:3)
            }
            println(:END)
        """
        )
        assert(out == ":1\n" +
                ":2\n" +
                ":3\n" +
                ":END\n") { out }
    }
     */

    // THROW

    @Test
    fun pp_01_throw() {
        val out = test(
            """
            spawn (task () {
                catch (it | it==:e1) {
                    error(:e1)
                }
                println(:e1)
                yield(nil)
                error(:e2)
            })()
            catch (it| :e2) {
                broadcast(nil)
                broadcast(nil)
                println(99)
            }
            println(:e2)
        """
        )
        assert(out == ":e1\n:e2\n") { out }
    }
    @Test
    fun pp_02_throw() {
        val out = test(
            """
            var co
            set co = spawn (task () {
                catch (it| it==:e1) {
                    resume (coroutine (coro () {
                        ;;yield(nil) ;;thus { it => nil }
                        error(:e1)
                    })) ()
                    loop {
                        yield(nil)
                    }
                }
                println(:e1)
                yield(nil) ;;thus { it => nil }
                error(:e2)
            })()
            catch (it | it==:e2 ) {
                broadcast(nil)
                broadcast(nil)
                println(99)
            }
            println(:e2)
        """
        )
        assert(out == ":e1\n:e2\n") { out }
    }
    @Test
    fun pp_02x_throw() {
        val out = test(
            """
            task () {
                catch (it|nil) {
                    loop {
                        yield(nil)
                    }
                }
            }
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun pp_03_throw() {
        val out = test(
            """
            val T = task () {
                catch (it| it==:e1 ) {
                    spawn( task () {
                        yield(nil) ;;thus { it => nil }
                        error(:e1)
                        println(:no)
                    }) ()
                    loop { yield(nil) } ;;thus { it => nil }
                }
                println(:ok1)
                error(:e2)
                println(:no)
            }
            spawn (task () {
                catch (it| :e2 ) {
                    spawn T()
                    loop { yield(nil) } ;;thus { it => nil }
                }
                println(:ok2)
                error(:e3)
                println(:no)
            }) ()
            catch (it | :e3 ) {
                broadcast(nil)
                println(:no)
            }
            println(:ok3)
        """
        )
        assert(out == ":ok1\n:ok2\n:ok3\n") { out }
    }
    @Test
    fun pp_04_08_spawn() {
        val out = test("""
            spawn task () {
                spawn (task () {
                    yield(nil)
                }) ()
                yield(nil)
                error(nil)
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun pp_05_xceu() {
        val out = test("""
            catch (_|true) {
                spawn task () {
                    error([tag(:x,[])])
                }()
            }
            println(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun pp_06_xceu5() {
        val out = test("""
            spawn task () {
                catch (err|err==:or) {
                    spawn task () {
                        yield(nil)
                        ;;println(:evt, evt)
                        ;;println(111)
                        error(:or)
                    }()
                    spawn task () {
                        yield(nil)
                        ;;println(:evt, evt)
                        ;;println(222)
                        error(:or)
                    }()
                    yield(nil)
                    ;;println(:in)
                }
                ;;println(:out)
            }()
            broadcast (nil)
            println(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }

    // RETURN

    @Test
    fun nn_01_term() {
        val out = test(
            """
            val t = spawn (task () {
                set pub = [1]
                yield(nil) ;;thus { it => nil }
                [2]
            } )()
            println(status(t), t.pub)
            broadcast(nil)
            println(status(t), t.pub)
       """
        )
        assert(
            out == ":yielded\t[1]\n" +
                    ":terminated\t[2]\n"
        ) { out }
    }
    @Test
    fun nn_02_term() {
        //DEBUG = true
        val out = test(
            """
            spawn( task () {
                val t = spawn (task () {
                    yield(nil) ;;thus { it => nil }
                    10
                } )()
                func (it) { println(it.pub) } (yield (nil))
            } )()
            broadcast(nil)
            println(:ok)
       """
        )
        assert(out == "10\n:ok\n") { out }
    }

    // TOGGLE

    @Test
    fun pp_01_toggle_err() {
        val out = test(
            """
            toggle 1(true)
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : (toggle 1(true))\n" +
                    " v  toggle error : expected yielded task\n"
        ) { out }
    }
    @Test
    fun pp_02_toggle() {
        val out = test(
            """
            val T = task () {
                yield(nil)
                println(10)
            }
            val t = spawn T()
            toggle t (false)
            println(1)
            broadcast(nil)
            broadcast(nil)
            toggle t (true)
            println(2)
            broadcast(nil)
        """
        )
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun pp_03_toggle_defer() {
        val out = test(
            """
            var T
            set T = task () {
                defer {
                    println(10)
                }
                ${AWAIT()}
                println(999)
            }
            var t
            set t = spawn T()
            toggle t (false)
            println(1)
            broadcast (nil)
            println(2)
        """
        )
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun pp_04_toggle_err() { // should be rt error
        val out = test(
            """
            val T = task () {
                nil
            }
            val t = spawn T()
            toggle t (false)
        """
        )
        assert(
            out == " |  anon : (lin 6, col 13) : (toggle t(false))\n" +
                    " v  toggle error : expected yielded task\n"
        ) { out }
    }
    @Test
    fun pp_05_toggle_nest() {
        val out = test(
            """
            val T = task () {
                spawn (task () {
                    ${AWAIT()}
                    println(3)
                }) ()
                ${AWAIT()}
                println(4)
            }
            println(1)
            val t = spawn T()
            toggle t (false)
            broadcast (nil)
            println(2)
            toggle t (true)
            broadcast (nil)
        """
        )
        assert(out == "1\n2\n3\n4\n") { out }
    }

    // ORIGINAL

    @Test
    fun zz_00_spawn() {
        val out = test(
            """
            var T
            set T = task (x,y) {
                println(x,y)
            }
            spawn T(1,2)
        """
        )
        assert(out == "1\t2\n") { out }
        //assert(out == "anon : (lin 6, col 19) : spawn error : invalid number of arguments\n") { out }
    }
    @Test
    fun zz_02_spawn_err() {
        val out = test(
            """
            spawn (func () {nil}) ()
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : (spawn (func () { nil })())\n" +
                    " v  spawn error : expected task\n"
        ) { out }
    }
    @Test
    fun zz_03_spawn_err() {
        val out = test(
            """
            spawn (func () {nil})
        """
        )
        assert(out == "anon : (lin 3, col 9) : spawn error : expected call\n") { out }
    }
    @Test
    fun zz_04_bcast() {
        val out = test(
            """
            var tk = task (v) {
                yield(nil) ;;thus { it => nil }
                val v' = yield(nil) ;;thus { it => it }
                error(:1)
            }
            var co1 = spawn tk ()
            var co2 = spawn tk ()
            catch (it| it==:1 ) {
                ;;func () {
                    println(1)
                    broadcast(1)
                    println(2)
                    broadcast(2)
                    println(3)
                    broadcast(3)
                ;;}()
            }
            println(99)
        """
        )
        assert(out == "1\n2\n99\n") { out }
    }
    @Test
    fun zz_05_bcast_in() {
        val out = test(
            """
            val T = task (v) {
                spawn (task () {
                    yield(nil)
                    println(:ok)
                }) ()
                broadcast (nil)
            }
            spawn T(2)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_06_bcast_in() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil) ;;thus { it => nil }
                    broadcast(nil)
                }) ()
                yield(nil)
                nil
            }) ()
            broadcast(nil)
            println(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun zz_10_bcast() {
        val out = test(
            """
            ;;println(:BLOCK0, `:pointer ceu_block`)
            spawn (task () {
                ;;println(:CORO1, `:pointer ceu_x`)
                ;;println(:BLOCK1, `:pointer ceu_block`)
                spawn (task () {
                    ;;println(:CORO2, `:pointer ceu_x`)
                    ;;println(:BLOCK2, `:pointer ceu_block`)
                    yield(nil) ;;thus { it => nil }
                    ;;println(:1)
                    broadcast(nil)
                }) ()
                yield(nil)
                nil
                ;;println(:2)
            }) ()
            broadcast(nil)
            println(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun zz_11_bcast() {
        val out = test(
            """
            var tk
            set tk = task () {
                println(yield(nil))
                nil
            }
            var co
            set co = spawn tk()
            ;;var f = func () {
                ;;var g = func () {
                    broadcast ([])
                ;;}
                ;;g()
            ;;}
            ;;f()
        """
        )
        //assert(out == "anon : (lin 16, col 13) : f()\n" +
        //        "anon : (lin 14, col 17) : g()\n" +
        //        "anon : (lin 12, col 21) : broadcast []\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_12_bcast() {
        val out = test(
            """
            var tk
            set tk = task () {
                println(yield(nil))
                nil
            }
            var co
            set co = spawn(tk)()
            ;;var f = func () {
                broadcast ([])
            ;;}
            ;;f()
        """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_13_bcast() {
        val out = test(
            """
            var T = task () {
                do {
                    println(yield(nil))
                }
            }
            var t = spawn T()
            ;;println(:1111)
            broadcast ([])
            ;;println(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast []\n" +
        //        "anon : (lin 4, col 17) : declaration error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun zz_14_bcast() {
        val out = test(
            """
            var T = task () {
                do {
                    var v =
                        yield(nil) ;;thus { it => it }
                    println(v)
                }
            }
            var t = spawn T()
            ;;println(:1111)
            var e = []
            broadcast (e)
            ;;println(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : broadcast e\n" +
        //        " v  anon : (lin 5, col 25) : resume error : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : broadcast'(e)\n" +
        //        " v  anon : (lin 5, col 36) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_15_bcast_err() {
        val out = test(
            """
            var T = task () {
                var v = yield(nil)
                println(v)
            }
            var t = spawn T()
            do {
                val a
                do {
                    val b
                    do {
                        var e = []
                        broadcast (e)
                    }
                }
            }
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 10, col 35) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 9, col 13) : broadcast e\n" +
        //        " v  anon : (lin 3, col 25) : resume error : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast e\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : broadcast'(e)\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun zz_15_bcast_okr() {
        val out = test(
            """
            var T = task () {
                println(yield(nil))
            }
            var t = spawn T()
            do {
                var e = []
                broadcast (e)
            }
            """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_16_bcast_err() {
        val out = test(
            """
            var T = task () {
                var v =
                    func (it) {it} (yield(nil))
                println(v)
            }
            var t = spawn T()
            ;;println(:1111)
            do {
                val a
                do {
                    val b
                    var e = []
                    broadcast (e)
                }
            }
            ;;println(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : broadcast e\n" +
        //        " v  anon : (lin 4, col 17) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == "anon : (lin 11, col 39) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 28) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " |  anon : (lin 4, col 17) : (func (it) { it })(yield(nil))\n" +
        //        " v  anon : (lin 4, col 27) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun zz_17_bcast() {
        DEBUG = true
        val out = test(
            """
            var T1 = task () {
                yield(nil) ;;thus { it => nil}
                spawn( task () {                ;; GC = task (no more)
                    val xevt = yield(nil) ;;thus { it => it}
                    println(:1)
                    var v = xevt
                } )()
                nil
            }
            var t1 = spawn T1()
            var T2 = task () {
                yield(nil) ;;thus { it => nil}
                val xevt = yield(nil) ;;thus { it => nil}
                ;;println(:2)
                do {
                    var v = xevt
                    ;;println(:evt, v, xevt)
                }
            }
            var t2 = spawn T2()
            broadcast ([])                      ;; GC = []
            println(`:number CEU_GC.free`)
            """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 20, col 13) : broadcast []\n" +
        //        "anon : (lin 16, col 17) : declaration error : incompatible scopes\n" +
        //        ":2\n" +
        //        ":error\n") { out }
    }
    @Test
    fun zz_18_bcast_tuple_func_ok() {
        val out = test(
            """
            var fff = func (v) {
                println(v)
            }
            var T = task () {
                fff(yield(nil))
            }
            spawn T()
            broadcast ([1])
        """
        )
        assert(out == "[1]\n") { out }
    }
    @Test
    fun zz_19_bcast_tuple_func_no() {
        val out = test(
            """
            var f = func (v) {  ;; *** v is no longer fleeting ***
                val x = v[0]    ;; v also holds x, both are fleeting -> unsafe
                println(x)      ;; x will be freed and v would contain dangling pointer
            }
            var T = task () {
                f(yield(nil)) ;;thus { it => it})
            }
            spawn T()
            broadcast ([[1]])
        """
        )
        //assert(out == " |  anon : (lin 10, col 13) : broadcast'([[1]])\n" +
        //        " v  anon : (lin 7, col 30) : block escape error : cannot copy reference out\n") { out }
        assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 10, col 13) : broadcast'([[1]])\n" +
        //        " |  anon : (lin 7, col 17) : f((yield(nil) thus { it => it }) )\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot move pending reference in\n") { out }
    }
    @Test
    fun zz_19_bcast_tuple_func_ok_not_fleet() {
        val out = test(
            """
            var f = func (v) {
                val x = v[0]    ;; v also holds x, both are fleeting -> unsafe
                println(x)      ;; x will be freed and v would contain dangling pointer
            }
            var T = task () {
                val xevt = yield(nil) ;;thus { it => it}   ;; NOT FLEETING (vs prv test)
                f(xevt)
            }
            spawn T()
            broadcast ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 11, col 13) : broadcast [[1]]\n" +
        //        " v  anon : (lin 7, col 17) : declaration error : cannot hold event reference\n") { out }
        //assert(out == " |  anon : (lin 11, col 13) : broadcast'([[1]])\n" +
        //        " v  anon : (lin 7, col 38) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_20_bcast_tuple_func_ok() {
        val out = test(
            """
            var f = func (v) {
                println(v[0])
            }
            var T = task () {
                f(yield(nil))
            }
            spawn T()
            broadcast ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 11, col 13) : broadcast'([[1]])\n" +
        //        " |  anon : (lin 8, col 44) : f(it)\n" +
        //        " v  anon : (lin 3, col 27) : declaration error : cannot move pending reference in\n") { out }
    }
    @Test
    fun zz_20_bcast_tuple_func_no() {
        val out = test(
            """
            val f = func (v) {
                func (x) {
                    val y = x
                    println(y)
                } (v[0])
            }
            var T = task () {
                f(yield(nil))
            }
            spawn T()
            broadcast ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : broadcast'([[1]])\n" +
        //        " |  anon : (lin 9, col 44) : f(it)\n" +
        //        " v  anon : (lin 4, col 21) : declaration error : cannot move pending reference in\n") { out }
    }
    @Test
    fun zz_20_bcast_tuple_func_nox() {
        val out = test(
            """
            val f = func (v) {
                val g = func (x) {
                    println(x)
                    x
                }
                g(v[0])
            }
            var T = task () {
                f(yield(nil)) ;;thus { it => f(it) }
            }
            spawn T()
            broadcast ([[1]])
        """
        )
        assert(out == "[1]\n")
        //assert(out == " |  anon : (lin 12, col 13) : broadcast'([[1]])\n" +
        //        " |  anon : (lin 9, col 44) : f(it)\n" +
        //        " |  anon : (lin 6, col 17) : g(v[0])\n" +
        //        " v  anon : (lin 3, col 31) : argument error : cannot move pending reference in\n") { out }
    }
    @Test
    fun zz_21_bcast_tuple_func_ok() {
        val out = test(
            """
            val f = func (v) {
                val x = v[0]
                println(x)
            }
            var g = func (v) {
                val xevt = v
                f(xevt)
            }
            g([[1]])
        """
        )
        assert(out == "[1]\n") { out }
    }
    @Test
    fun zz_22_pool_throw() {
        val out = test(
            """
            println(1)
            catch (it| it==:ok) {
                println(2)
                spawn (task () {
                    println(3)
                    ${AWAIT()}
                    println(6)
                    error(:ok)
                }) ()
                spawn (task () {
                    println(4)
                    ${AWAIT()}
                    println(999)
                }) ()
                println(5)
                broadcast(nil)
                println(9999)
            }
            println(7)
        """
        )
        assert(out == "1\n2\n3\n4\n5\n6\n7\n") { out }
    }
    @Test
    fun zz_23_valgrind() {
        val out = test(
            """
            spawn( task () {
                val t = []
                spawn( task () {
                    yield(nil) ;;thus { it => nil }
                    println(t)
                }) ()
                yield(nil)
                nil
            }) ()
            broadcast ([])
        """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_24_valgrind() {
        val out = test(
            """
            spawn (task () {
                do {
                    yield(nil)
                    nil
                }
                val y
            }) ()
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_25_break() {
        val out = test(
            """
            spawn (task () {
                println(loop {
                    val t = [10]
                    break if t[0]
                    yield(nil) ;;thus { it => nil }
                })
            }) ()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun zz_26_xceu() {
        val out = test("""
            spawn task () {
                do {
                    spawn task () {
                        yield(nil)
                    } ()
                    yield(nil)
                }
                broadcast( [[]] )
                ;;await true
            }()
            broadcast (nil)
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_27_xceu() {
        val out = test("""
            spawn task () {
                do {
                    spawn task () {
                        yield(nil)
                    } ()
                    yield(nil)
                }
                broadcast( tag(:x, []) )
            }()
            broadcast (nil)
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_28_xceu () {
        val out = test("""
            spawn task () {
                do {
                    spawn task () {
                        yield(nil)
                    }()
                    yield(nil)
                    nil
                }
                do {
                    nil
                }
            } ()
            broadcast (nil)
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_29_xceu () {
        val out = test("""
            spawn task () {
                loop {
                    var evt = yield(nil);
                    loop {
                        break if evt==10
                        set evt = yield(nil)
                    }
                    println(:1)
                    var t = spawn task () {
                        var evt2 = yield(nil);
                        loop {
                            break if evt2==10 
                            set evt2 = yield(nil)
                        }
                    } ()
                    loop {
                        break if status(t)==:terminated
                        set evt = yield(nil)
                        delay
                    }
                    println(:2)
                }
            } ()
            broadcast (10)    ;; :1
            broadcast (10)    ;; :2 (not :1 again)
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun zz_30_xceu () {
        val out = test("""
            data :X = [x]
            task () :X {
                task :nested () {
                    ;;;task.;;;pub.x
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_31_kill() {
        val out = test("""
            spawn task () {
                do {
                    val t1 = spawn task () {
                        ${AWAIT()}
                        println(1)
                    } ()
                    spawn task () {
                        defer { println(3) }
                        ${AWAIT()}
                        println(2)
                    } ()
                    ${AWAIT ("t1")}
                }
                println(999)
            } ()
            broadcast (nil)
        """, true)
        assert(out == "1\n3\n999\n") { out }
    }
    @Test
    fun zz_32_all() {
        val out = test("""
            val T = (task () {
                set ;;;task.;;;pub = []
                yield(nil)
            })
            val f = (func (v) {
                nil
            })
            do {
                val xxx = spawn T()
                do {
                    val zzz = xxx
                    nil
                }
                f(xxx.pub)
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // ORIGINAL / DATA / EVT

    @Test
    fun z1_01_data_await() {
        val out = test(
            """
            data :E = [x,y]
            spawn (task () {
                func (it :E) {
                    println(it.x)
                } (yield(nil))
            } )()
            broadcast (tag(:E, [10,20]))
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun z1_02_data_await() {
        val out = test(
            """
            data :E = [x,y]
            data :F = [i,j]
            spawn (task () {
                func (it :E) {
                    println(it.x)
                } (yield(nil))
                func (it :F) {
                    println(it.j)
                } (yield(nil))
            } )()
            broadcast (tag(:E, [10,20]))
            broadcast (tag(:F, [10,20]))
        """
        )
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun z1_03_data_pub_err() {
        val out = test(
            """
            task () :T { nil }
            println(:ok)
        """
        )
        assert(out == "anon : (lin 2, col 21) : declaration error : data :T is not declared\n") { out }
        //assert(out == ":ok\n") { out }
    }
    @Test
    fun z1_04_data_pub() {
        val out = test(
            """
            data :T = [x,y]
            spawn( task () :T {
                set pub = [10,20]
                println(pub.x)
            }) ()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun z1_05_data_pub_err() {
        val out = test(
            """
            var t = spawn (task () {
                yield(nil) ;;thus { it => nil }
            } )()
            println(t.pub.y)
        """
        )
        assert(
            out == " |  anon : (lin 5, col 21) : t.pub[:y]\n" +
                    " v  index error : expected collection\n"
        ) { out }
    }
    @Test
    fun z1_06_data_pub() {
        val out = test(
            """
            data :T = [x,y]
            var t :T = spawn( task () {
                set pub = [10,20]
                yield(nil) ;;thus { it => nil }
            }) ()
            println(t.pub.y)
        """
        )
        assert(out == "20\n") { out }
    }

    // EXTRA

    @Test
    fun z2_01_valgrind() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    yield(nil)
                    yield(nil)
                    println(:1)
                }) ()
                spawn (task () {
                    yield(nil)
                    println(:2)
                }) ()
                spawn (task () {
                    yield(nil)
                    yield(nil)
                    println(:3)
                }) ()
                yield(nil)
                yield(nil)
                println(:ok)
            }) ()
            broadcast(nil)
        """
        )
        assert(out == ":2\n:3\n:1\n:ok\n") { out }
    }

    @Test
    fun z2_02_parand() {
        val out = test(
            """
            do {
                spawn (task () {
                    func (it) {
                        false
                    } (yield(nil))
                    println(999)
                }) (nil)
                val x = spawn (task () {
                    nil
                }) (nil)
                nil
            }
        """
        )
        assert(out == "999\n") { out }
    }

    @Test
    fun z2_03_nested_func() {
        val out = test(
            """
            val T = task (x) {
                val f = func () {
                    x
                }
                yield(nil)
                println(f())
            }
            spawn T([])
            broadcast(nil)
        """
        )
        assert(out == "[]\n") { out }
    }

    @Test
    fun z2_04_nested_func() {
        val out = test(
            """
            val T = task (x) {
                yield(nil)
                val y = 10
                val f = func () {
                    y
                }
                println(f())
            }
            spawn T([])
            broadcast(nil)
        """
        )
        assert(out == "10\n") { out }
    }

    @Test
    fun z2_03_skip_valgrind() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    loop {
                        yield(nil)
                        skip if (func () {
                            true
                        } ())
                    }
                }) ()
                spawn (task () {
                    nil
                }) ()
                yield(nil)
            }) ()
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    @Test
    fun z2_04_op_is() {
        val out = test(
            """
            println(is?(1,  :number))
            println(is?(:x, :number))
        """, true
        )
        assert(out == "true\nfalse\n") { out }
    }

    @Test
    fun z2_05_99() {
        val out = test(
            """
            spawn (task () {
                spawn (task () {
                    yield(nil)
                }) ()
                spawn (task () {
                    yield(nil)
                }) ()
                loop {
                    yield(nil)
                    break if true
                }
            }) ()
            broadcast(nil)
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_06_99() {
        val out = test("""
            spawn (task (v) {
                spawn (task :nested () {
                    println(v)
                }) ()
            }) (100)
            println(:ok)
        """)
        assert(out == "100\n:ok\n") { out }
    }
    @Test
    fun zz_08_99() {
        val out = test("""
            val T = task (x, y) {
                println(:a, x)
                yield(nil)
                println(:b, x)
            }            
            spawn T(1, 2)
            broadcast(10)
        """)
        assert(out == ":a\t1\n:b\t1\n") { out }
    }
    @Test
    fun zz_09_99_double_awake() {
        val out = test("""
            spawn (task () {
                loop {
                    yield(nil) ; delay
                    println(false)
                    val t = spawn (task () {
                        yield(nil) ; delay
                    }) ()
                    ${AWAIT("t")}
                    delay
                    println(true)
                    broadcast(:Y)
                }
            }) ()
            broadcast(:X)
            broadcast(:X)
        """)
        assert(out == "false\ntrue\n") { out }
    }
    @Test
    fun zz_10_optim() {
        val out = test("""
            spawn (task () {
                println(do {
                    loop {
                        yield(nil)
                        break if true
                    }
                    delay
                    nil
                })
            }) ()
            broadcast(nil)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun zz_11_valgrind() {
        val out = test("""
            spawn (task () {
                spawn (task () {
                    loop {
                        do {
                            (var it)
                            (set it = yield(nil))
                        }
                    }
                }) ()
                spawn (task () {
                    nil
                }) ()
                yield(nil)
            }) ()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
}

