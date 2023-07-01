package ceu

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters

fun yield (ok: String = "ok"): String {
    return "do { var $ok=false; loop until $ok { yield(nil); if type(evt)/=:x-task { set $ok=true } else { nil } } }"
}
fun await (evt: String): String {
    return "do { var ok; set ok=false; yield(nil); loop until ok { if $evt { set ok=true } else { yield(nil) } } }"
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TTask {

    // TASK / COROUTINE / RESUME / YIELD

    @Test
    fun aa_coro0() {
        val out = all("""
            val t = coro (v) {
                v
            }
            val a = coroutine(t)
            var v = resume a(1)
            println(v)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_coro1() {
        val out = all("""
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
            val a = coroutine(t)
            var v = resume a(1)
            println(v)              ;; 2
            set v = resume a(v+1)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """, true)
        assert(out == "1\n2\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun aa_coro2_err() {
        val out = all("""
            coroutine(func () {nil})
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : coroutine((func () { nil })) : coroutine error : expected coro\n" +
                ":error\n") { out }
    }
    @Test
    fun aa_coro3_err() {
        val out = all("""
            var f
            resume f()
        """.trimIndent())
        assert(out == "anon : (lin 2, col 1) : resume error : expected yielded coro\n:error\n") { out }
    }
    @Test
    fun aa_coro4_err() {
        val out = all("""
            var co
            set co = coroutine(coro () {nil})
            resume co()
            resume co()
        """.trimIndent())
        assert(out == "anon : (lin 4, col 1) : resume error : expected yielded coro\n:error\n") { out }
    }
    @Test
    fun aa_coro5_err() {
        val out = all("""
            val co = coroutine(coro () { nil
            })
            resume co()
            resume co(1,2)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : expected yielded coro\n:error\n") { out }
    }
    @Test
    fun aa_coro6() {
        val out = all("""
            var co
            set co = coroutine(coro (v) {
                val v' = yield(nil) 
                println(v')
            })
            resume co(1)
            resume co(2)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun aa_coro7() {
        val out = all("""
            var co
            set co = coroutine(coro (v) {
                println(v)
            })
            println(1)
            resume co(99)
            println(2)
        """)
        assert(out == "1\n99\n2\n") { out }
    }
    @Test
    fun aa_coro8_err() {
        val out = all("""
            var xxx
            resume xxx() ;;(xxx(1))
        """)
        assert(out == "anon : (lin 3, col 13) : resume error : expected yielded coro\n:error\n") { out }
    }
    @Test
    fun aa_coro9_mult() {
        val out = all("""
            var co
            set co = coroutine(coro (x,y) {
                println(x,y)
            })
            resume co(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun aa_coro10_err() {
        val out = all("""
            var co
            set co = coroutine(coro () {
                yield(nil)
            })
            resume co()
            resume co(1,2)
        """)
        assert(out.contains("bug found : not implemented : multiple arguments to resume")) { out }
    }
    @Test
    fun aa_coro11_class() {
        val out = all("""
            var T
            set T = coro (x,y) {
                println(x,y)
            }
            resume (coroutine(T)) (1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun aa_coro12_tuple_leak() {
        val out = all("""
            val T = coro () {
                pass [1,2,3]
                yield(nil)
            }
            resume (coroutine(T)) ()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_coro13_defer() {
        val out = all("""
            var T
            set T = coro () {
                defer {
                    println(3)
                }
                println(1)
                yield(nil)   ;; never awakes
                println(2)
            }
            println(0)
            resume (coroutine(T)) ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun aa_yield14_err() {
        val out = all("""
            yield(nil)
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing coro or task") { out }
    }
    @Test
    fun aa_yield15_err() {
        val out = all("""
            coro () {
                func () {
                    yield(nil)
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : expected enclosing coro or task") { out }
    }
    @Test
    fun aa_task16_nest() {
        val out = all("""
            spawn (task (v1) {
                println(v1)
            }) (1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_17_coro_nest_err() {
        val out = ceu.all("""
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
        assert(out.contains("x-coro: 0x")) { out }
    }
    @Test
    fun aa_task17_nest_err() {
        val out = all("""
            spawn task (v1) {
                spawn task (v2) {
                    spawn task (v3) {
                        println(v1,v2,v3)
                        nil
                    }(3)
                }(2)
            }(1)
        """)
        ////assert(out == "1\t2\t3\n") { out }
        //assert(out == "anon : (lin 2, col 19) : task (v1) { spawn task (v2) { spawn task (v3)...)\n" +
        //        "anon : (lin 3, col 23) : task (v2) { spawn task (v3) { nil }(3) }(2)\n" +
        //        "anon : (lin 3, col 33) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun aa_coro18_defer() {
        val out = all("""
            var T
            set T = coro () {
                defer {
                    println(3)
                }
                println(1)
                yield(nil)   ;; never awakes
                println(2)
            }
            val t = coroutine(T)
            println(0)
            resume t ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun aa_task19_self_escape_err() {
        val out = all("""
            var T1 = coro (co) {
                yield(nil)
            }
            var T2 = coro () {
                var t1 = spawn T1(coro)
                yield(t1)          ;; t1 cannot escape
            }
            var t2 = coroutine(T2)
            var t1 = resume t2()
        """)
        assert(out == "anon : (lin 10, col 29) : t2()\n" +
                "anon : (lin 7, col 17) : yield error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun todo_aa_task19a_self_escape() {
        val out = all("""
            val T1 = task (co) {
                println(:3)
                yield(nil)
                println(:999)
            }
            var T2 = task () {
                println(:2)
                yield(spawn T1(task))
                println(:999)
            }
            var t2 = coroutine(T2)
            println(:1)
            var t1 = resume t2()
            println(:4)
        """)
        assert(out == ":1\n:2\n:3\n:4\n") { out }
    }
    @Test
    fun todo_aa_task20_self() {
        val out = all("""
            var T1 = task (co) {
                println(:3)
                yield(nil)
                println(:6)
                resume co(:t1)  ;; will kill myself
                println(:999)
            }
            var T2 = task () {
                println(:2)
                var t1 = spawn T1(task)
                println(:4)
                yield(track(t1))
                println(:7)
            }
            println(:1)
            var t2 = coroutine(T2)
            var t1 = resume t2()
            println(:5)
            resume detrack(t1)()
            println(:8)
        """)
        assert(out == ":1\n:2\n:3\n:4\n:5\n:6\n:7\n:8\n") { out }
    }
    @Test
    fun aa_task21_self() {
        val out = all("""
            var t = []
            resume t()
        """)
        assert(out == "anon : (lin 3, col 13) : resume error : expected yielded coro\n:error\n") { out }
    }
    @Test
    fun aa_coro22a_defer() {
        val out = all("""
            val T = coro () {
                println(1)
                yield(nil)   ;; never awakes
                defer {
                    println(999)
                }
            }
            resume (coroutine(T)) ()
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun aa_coro22_defer() {
        val out = all("""
            var T
            set T = coro () {
                defer {
                    println(3)
                }
                println(1)
                yield(nil)   ;; never awakes
                defer {
                    println(999)
                }
                println(2)
            }
            println(0)
            resume (coroutine(T)) ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun aa_task23_notask() {
        val out = all("""
            task
        """)
        assert(out == "anon : (lin 2, col 13) : task error : missing enclosing task") { out }
    }
    @Test
    fun aa_coro24_defer() {
        val out = all("""
            val F = coro () {
                defer {
                    println(:xxx)
                }
                yield(nil)
                defer {
                    println(:yyy)
                }
                yield(nil)
            }
            do {
                val f = coroutine(F)
                resume f()
                resume f()
            }
        """)
        assert(out == ":yyy\n:xxx\n") { out }
    }

    // SPAWN

    @Test
    fun bb_spawn1() {
        val out = all("""
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
        assert(out == "1\n:x-coro\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun bb_spawn2() {
        val out = all("""
            var T
            set T = task (x,y) {
                println(x,y)
            }
            spawn T(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun bb_spawn3_err() {
        val out = all("""
            spawn (func () {nil}) ()
        """)
        assert(out == "anon : (lin 2, col 20) : spawn error : expected coro or task\n:error\n") { out }
    }
    @Test
    fun bb_spawn4_err() {
        val out = all("""
            spawn (func () {nil})
        """)
        assert(out == "anon : (lin 3, col 9) : invalid spawn : expected call") { out }
    }
    @Test
    fun bb_coro_spawn5() {
        val out = all("""
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
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun bb_spawn6_err() {
        val out = all("""
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
    fun bb_spawn67_err() {
        val out = all("""
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
    fun bb_spawn7_err() {
        val out = all("""
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
    fun bb_spawn8_err() {
        val out = all("""
            var T
            set T = task () {
                spawn (task () :fake {
                    println(1)
                    nil
                }) ()
            }
            spawn T()
        """)
        assert(out == "1\n")
        //assert(out == "anon : (lin 8, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n" +
        //        "1\n" +
        //        ":error\n") { out }
    }
    @Test
    fun bb_spawn9() {
        val out = all("""
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
    fun bb_coro_spawn10() {
        val out = all("""
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
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun bb_coro_spawn11() {
        val out = all("""
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
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun bb_spawn12_err() {
        val out = all("""
            var T
            set T = task () {
                spawn (task () :fake {
                    (999)
                })()
            }
            spawn T()
            println(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 8, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun bb_spawn13_err() {
        val out = all("""
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
    @Test
    fun bb_14_err() {
        val out = all("""
            var f = func () {
                t
            }
            var t
        """)
        assert(out == "anon : (lin 3, col 17) : access error : variable \"t\" is not declared") { out }
    }

    // MOVE

    @Test
    fun cc_01_move_err () {
        val out = ceu.all("""
            val co = do {
                val v
                val x = spawn (coro () {
                    yield(nil)
                    println(v)
                    println(:ok)
                }) ()
                move(x)
            }
            resume co()
        """)
        assert(out == "anon : (lin 9, col 22) : move error : value is not movable\n" +
                ":error\n") { out }
    }

    @Test
    fun cc_02_move_ok () {
        val out = ceu.all("""
            val tup = [nil]
            val co = do {
                var x = spawn (coro () {
                    yield(nil)
                    println(:ok)
                }) ()
                move(x)
            }
            resume co()
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cc_03_move() {
        val out = all("""
        val f = func (x) {
            val :tmp v = x()
            v
        }
        val F = func () {
            []
        }
        do {
            val l = f(F)
            println(l)
            nil
        }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun cc_04_move() {
        val out = all("""
        val f = func (co) {
            resume co()
        }
        val C = coro () {
            var t = []
            yield(move(t))
            println(:in, t)
        }
        do {
            val co = coroutine(C)
            do {
                val v = f(co)
                println(:out, v)
            }
            f(co)
        }
        """)
        assert(out == ":out\t[]\n" +
                ":in\tnil\n") { out }
    }
    @Test
    fun cc_05_move() {
        val out = all("""
            val F = func (^x) {
                move(spawn (coro () {
                    yield(nil)
                    ^^x
                }) ())
            }
            do {
                val x = []
                val co = F(x)
                println(resume co())
            }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun cc_06_move() {
        val out = all("""
            do {
                val F = (coro () { println(:ok) })
                val f = do {        ;; dst=2
                    coroutine(F)    ;; src=3
                }
                resume f()
            }
        """)
        assert(out == ":ok\n") { out }
    }

    // SPAWN / GROUP

    @Test
    fun cc_group1() {
        val out = all("""
            var T
            set T = task (v) {
                println(v)
            }
            var t
            set t = export [] {
                var v
                set v = 10
                spawn T(v)  
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_group2_no_more_spawn_export() {
        val out = all("""
            export [T] {
                var T
                set T = task (v) {
                    println(v)
                }
            }
            export [t] {
                var t
                set t = spawn export [] {
                    export [v] {
                        var v
                        set v = 10
                    }
                    T(v)
                }
            }
            println(type(t))
        """)
        //assert(out == "10\n:x-task\n") { out }
        assert(out == "anon : (lin 15, col 21) : call error : expected function\n" +
                ":error\n") { out }
    }
    @Test
    fun cc_group3() {
        val out = all("""
            var f
            set f = func () {
                nil
            }
            spawn task () :fake {
                export [] {
                    f()
                }
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_group4() {
        val out = all("""
            export [f] {
                var cur = nil
                val f = func () {
                    set cur = 65
                    cur
                }
            }
            val co = spawn (coro () {
                yield(nil)
                loop {
                    val v = f()
                    yield(v)
                }
            }) ()
            loop {
                var v = resume co()
                println(v)
                throw(99)
            }
        """)
        assert(out == "anon : (lin 19, col 17) : throw(99)\n" +
                "throw error : uncaught exception\n" +
                "65\n" +
                "99\n") { out }
    }

    // THROW

    @Test
    fun dd_throw1() {
        val out = all("""
            var co
            set co = coroutine(coro (x,y) {
                throw(:e2)
            })
            catch :e2 {
                resume co(1,2)
                println(99)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun dd_throw2() {
        val out = all("""
            var co
            set co = coroutine(coro (x,y) {
                yield(nil)
                throw(:e2)
            })
            catch :e2 {
                resume co(1,2)
                println(1)
                resume co()
                println(2)
            }
            println(3)
        """)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun dd_throw3_err() {
        val out = all("""
            var T
            set T = task () {
                spawn task () {
                    yield(nil)
                    throw(:error )
                }()
                yield(nil)
            }
            spawn in tasks(), T()
            broadcast in :global, nil
        """)
        assert(out == "anon : (lin 11, col 13) : broadcast in :global, nil\n" +
                "anon : (lin 6, col 21) : throw(:error)\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun dd_throw4_err() {
        val out = all("""
            broadcast in :task, nil
        """)
        assert(out == "anon : (lin 2, col 26) : broadcast error : invalid target\n:error\n") { out }
    }
    @Test
    fun dd_throw5_err() {
        val out = all("""
            spawn (task () :fake {
                broadcast in :task, nil
            }) ()
        """)
        assert(out == "anon : (lin 2, col 20) : (task () :fake { broadcast in :task, nil })()\n" +
                "anon : (lin 3, col 30) : broadcast error : invalid target\n:error\n") { out }
    }
    @Test
    fun dd_throw6() {
        val out = all("""
            var co
            set co = coroutine (coro () {
                catch :e1 {
                    yield(nil)
                    throw(:e1)
                }
                println(:e1)
                yield(nil)
                throw(:e2)
            })
            catch :e2 {
                resume co()
                resume co()
                resume co()
                println(99)
            }
            println(:e2)
        """)
        assert(out == ":e1\n:e2\n") { out }
    }
    @Test
    fun dd_throw7() {
        val out = all("""
            var co
            set co = spawn (task () {
                catch :e1 {
                    coroutine (coro () {
                        yield(nil)
                        throw(:e1)
                    })()
                    loop {
                        yield(nil)
                    }
                }
                println(:e1)
                yield(nil)
                throw(:e2)
            })()
            catch :e2 {
                broadcast in :global, nil
                broadcast in :global, nil
                println(99)
            }
            println(:e2)
        """)
        assert(out == ":e1\n:e2\n") { out }
    }
    @Test
    fun dd_throw8() {
        val out = ceu.all(
            """
            var T
            set T = task () {
                catch err==:e1 {
                    spawn task () {
                        yield(nil)
                        throw(:e1)
                        println(:no)
                    } ()
                    loop { yield(nil) }
                }
                println(:ok1)
                throw(:e2)
                println(:no)
            }
            spawn (task () {
                catch :e2 {
                    spawn T()
                    loop { yield(nil) }
                }
                println(:ok2)
                throw(:e3)
                println(:no)
            }) ()
            catch :e3 {
                broadcast in :global, nil
                println(:no)
            }
            println(:ok3)
        """
        )
        assert(out == ":ok1\n:ok2\n:ok3\n") { out }
    }
    @Test
    fun dd_throw9_fake() {
        val out = all("""
            catch err==:err {
                spawn (task () :fake {
                    throw(:err)
                }) ()
            }
            println(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun dd_throw10_fake() {
        val out = all("""
            catch err == :xxx {
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
    @Test
    fun dd_throw11_fake() {
        val out = all("""
            spawn task () { 
                catch {==}(err,:err) {
                    spawn task () :fake {
                        ${yield()}
                        throw(:err)
                    }()
                    ${yield()}
                }
            }() 
            broadcast in :global, nil 
            println(10)
        """)
        assert(out == "10\n") { out }
    }

    // BCAST / BROADCAST

    @Test
    fun ee_bcast0() {
        val out = all("""
            println(broadcast in :global, 1)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun ee_bcast001() {
        val out = ceu.all(
            """
            spawn task () {
                println(1)
                yield(nil)
                println(2)
            }()
             broadcast in :global, nil
        """
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun ee_bcast01() {
        val out = all("""
            var tk = task (v) {
                yield(nil)
                println(v, evt)
                val v' = yield(nil)
                println(v', evt)
            }
            spawn tk ()
            broadcast in :global, 1
            broadcast in :global, 2
            broadcast in :global, 3
        """)
        assert(out == "nil\t1\nnil\t2\n") { out }
    }
    @Test
    fun ee_bcast1() {
        val out = all("""
            var tk
            set tk = task (v) {
                yield(nil)
                println(v)
                println(evt)
                var v' = yield(nil)
                println(v')
                println(evt)
            }
            var co1 = spawn tk()
            var co2 = spawn tk()
            broadcast in :global, 1
            broadcast in :global, 2
            broadcast in :global, 3
        """)
        //assert(out == "nil\n1\nnil\n1\nnil\n2\nnil\n2\n") { out }
        assert(out.contains("nil\n1\nnil\n1\nnil\n2\nnil\nx-task: 0x")) { out }
    }
    @Test
    fun ee_bcast2() {
        val out = all("""
            var co1 = spawn (task () {
                var co2 = spawn (task () {
                    yield(nil)  ;; awakes from outer bcast
                    println(2)
                }) ()
                yield(nil)      ;; awakes from co2 termination
                println(1)
            }) ()
            broadcast in :global, nil
        """)
        assert(out == "2\n1\n") { out }
    }
    @Test
    fun ee_bcast3() {
        val out = all("""
            var co1 = spawn (task () {
                var co2 = spawn (task () {
                    ${yield()}
                    throw(:error)
                })()
                ${yield()}
                println(1)
            })()
            broadcast in :global, nil
        """)
        assert(out == "anon : (lin 10, col 13) : broadcast in :global, nil\n" +
                "anon : (lin 5, col 21) : throw(:error)\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun ee_bcast4() {
        val out = all("""
            var tk
            set tk = task () {
                yield(nil)
                do {
                    println(evt)
                    yield(nil)
                    println(evt)
                }
                do {
                    println(evt)
                    yield(nil)
                    println(evt)
                }
            }
            spawn tk ()
            broadcast in :global, 1
            broadcast in :global, 2
            broadcast in :global, 3
            broadcast in :global, 4
        """)
        assert(out == "1\n2\n2\n3\n") { out }
    }
    @Test
    fun ee_bcast5() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v)
                yield(nil)
                println(evt)
            }
            var co = spawn(tk)(1)
            broadcast in :global, 2
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun ee_bcast6() {
        val out = all("""
            func () {
                 broadcast in :global, 1
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ee_bcast7() {
        val out = all("""
            var tk
            set tk = task () {
                yield(nil)
                yield(nil)
                println(evt)                
            }
            var co1 = spawn (tk) ()
            var co2 = spawn tk ()
            do {
                 broadcast in :global, 1
                 broadcast in :global, 2
                 broadcast in :global, 3
            }
        """)
        //assert(out == "2\n2\n") { out }
        assert(out.contains("2\nx-task: 0x")) { out }
    }
    @Test
    fun ee_bcast8() {
        val out = all("""
            var tk
            set tk = task (v) {
                yield(nil)
                do { var ok=false; loop until ok { yield(nil;) if type(evt)/=:x-task { set ok=true } else { nil } } }
                println(evt)                
            }
            var co1 = spawn tk ()
            var co2 = spawn tk ()
            func () {
                 broadcast in :global, 1
                 broadcast in :global, 2
                 broadcast in :global, 3
            }()
        """)
        assert(out == "2\n2\n") { out }
    }
    @Test
    fun ee_bcast9() {
        val out = ceu.all(
            """
            var tk = task (v) {
                yield(nil)
                val v' = yield(nil)
                throw(:1)
            }
            var co1 = spawn tk ()
            var co2 = spawn tk ()
            catch err==:1 {
                func () {
                    println(1)
                    broadcast in :global, 1
                    println(2)
                    broadcast 2
                    println(3)
                    broadcast in :global, 3
                }()
            }
            println(99)
        """
        )
        assert(out == "1\n2\n99\n") { out }
    }
    @Test
    fun ee_bcast10() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                println(v)
                ;;yield(nil)
                do { var ok=false; loop until ok { yield(nil;) if type(evt)/=:x-task { set ok=true } else { nil } } }
                ;;yield(nil)
                println(evt)                
                do { var ok2=false; loop until ok2 { yield(nil;) if type(evt)/=:x-task { set ok2=true } else { nil } } }
                ;;yield(nil)
                println(evt)                
            }
            println(1)
            var co1 = spawn (tk) (10)
            var co2 = spawn (tk) (10)
            catch err==:1 {
                func () {
                    println(2)
                    broadcast in :global, [20]
                    println(3)
                    broadcast @[(30,30)]
                }()
            }
        """
        )
        assert(out == "1\n10\n10\n2\n[20]\n[20]\n3\n@[(30,30)]\n@[(30,30)]\n") { out }
    }

    // BCAST / SCOPE

    @Test
    fun ee_bcast_in1() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                do { var ok=false; loop until ok { yield(nil;) if type(evt)/=:x-task { set ok=true } else { nil } } }
                ;;yield(nil)
                println(v)
            }
            var t1
            set t1 = spawn T (1)
            var t2
            set t2 = spawn T (2)
            broadcast in t1, nil
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun ee_bcast_in2_err() {
        val out = ceu.all(" broadcast in nil, nil")
        assert(out == "anon : (lin 1, col 15) : broadcast error : invalid target\n:error\n") { out }
    }
    @Test
    fun ee_bcast_in3_err() {
        val out = ceu.all(" broadcast in :xxx, nil")
        assert(out == "anon : (lin 1, col 15) : broadcast error : invalid target\n:error\n") { out }
    }
    @Test
    fun ee_bcast_in4() {
        val out = ceu.all(
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
                broadcast in :local, nil
            }
        """
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun ee_bcast_in5() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                spawn (task () {
                    yield(nil)
                    println(:ok)
                }) ()
                broadcast in :global, :ok
            }
            spawn T (2)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ee_bcast_in6() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                spawn (task () :fake {
                    yield(nil)
                    println(v, evt)
                }) ()
                spawn (task () :fake {
                    do {
                        broadcast in :task, :ok
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
    fun ee_bcast_in7() {
        val out = all("""
            spawn (task () {
                spawn (task () {
                    yield(nil)
                    broadcast nil
                }) ()
                yield(nil)
            }) ()
            broadcast in :global, nil
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ee_bcast_in7a() {
        val out = all("""
            ;;println(:BLOCK0, `:pointer ceu_block`)
            spawn (task () {
                ;;println(:CORO1, `:pointer ceu_x`)
                ;;println(:BLOCK1, `:pointer ceu_block`)
                spawn (task () {
                    ;;println(:CORO2, `:pointer ceu_x`)
                    ;;println(:BLOCK2, `:pointer ceu_block`)
                    yield(nil)
                    ;;println(:1)
                    broadcast in :global, nil
                }) ()
                yield(nil)
                ;;println(:2)
            }) ()
            broadcast in :global, nil
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ee_bcast08_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task () {
                yield(nil)
                var v = evt
                nil
            }
            var co
            set co = spawn tk()
            var f = func () {
                var g = func () {
                    broadcast in :global, []
                }
                g()
            }
            f()
        """
        )
        assert(out == "anon : (lin 16, col 13) : f()\n" +
                "anon : (lin 14, col 17) : g()\n" +
                "anon : (lin 12, col 21) : broadcast in :global, []\n" +
                "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun todo_ee_bcast09_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task () {
                var v = evt  ;; ERR: v is at same depth as f, but in a parallel scope
                nil
            }
            var co
            set co = coroutine(tk)
            var f = func () {
                broadcast in :global, []
            }
            f()
        """
        )
        assert(out == "TODO\n") { out }
    }
    @Test
    fun ee_bcast10_err() {
        val out = ceu.all(
            """
            var T = task () {
                yield(nil)
                do {
                    var v = evt
                    println(v)
                }
            }
            var t = spawn T()
            ;;println(:1111)
            broadcast in :global, []
            ;;println(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 4, col 17) : declaration error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun ee_11_bcast_err() {
        val out = ceu.all(
            """
            var T = task () {
                yield(nil)
                do {
                    var v = evt
                    println(v)
                }
            }
            var t = spawn T()
            ;;println(:1111)
            var e = []
            broadcast in :global, e
            ;;println(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 10, col 35) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == "anon : (lin 10, col 13) : broadcast in :global, e\n" +
        //        "anon : (lin 4, col 17) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun ee_12_bcast_err() {
        val out = ceu.all(
            """
            var T = task () {
                yield(nil)
                var v = evt
                println(v)
            }
            var t = spawn T()
            do {
            var e = []
            broadcast in :global, e
            }
            """
        )
        //assert(out == ":1\n:2\n1\n") { out }
        //assert(out == "anon : (lin 10, col 35) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == "anon : (lin 10, col 13) : broadcast in :global, e\n" +
                "anon : (lin 4, col 17) : declaration error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun ee_11_bcast_err_a() {
        val out = ceu.all(
            """
            broadcast in :global, []
            println(:ok)
            """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ee_11_bcast_move() {
        val out = ceu.all(
            """
            var T = task () {
                yield(nil)
                do {
                    var v = evt
                    println(v)
                }
            }
            var t = spawn T()
            ;;println(:1111)
            var e = []
            broadcast in :global, move(e)
            ;;println(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 10, col 13) : broadcast in :global, move(e)\n" +
        //        "anon : (lin 4, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun ee_bcast12_err() {
        val out = ceu.all(
            """
            var T = task () {
                yield(nil)
                var v = evt
                println(v)
            }
            var t = spawn T()
            ;;println(:1111)
            do {
                var e = []
                broadcast in :global, e
            }
            ;;println(:2222)
            """
        )
        //assert(out == ":1\n:2\n1\n") { out }
        assert(out == "anon : (lin 11, col 17) : broadcast in :global, e\n" +
                "anon : (lin 4, col 17) : declaration error : incompatible scopes\n" +
                ":error\n") { out }
        //assert(out == "anon : (lin 11, col 39) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun ee_bcast13_err() {
        val out = ceu.all(
            """
            var T1 = task () {
                yield(nil)
                spawn task () {
                    yield(nil)
                    println(:1)
                    var v = evt
                } ()
                nil
            }
            var t1 = spawn T1()
            var T2 = task () {
                yield(nil)
                yield(nil)
                ;;println(:2)
                do {
                    var v = evt
                    ;;println(:evt, v, evt)
                }
            }
            var t2 = spawn T2()
            broadcast in :global, []
            println(`:number ceu_gc_count`)
            """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 20, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 16, col 17) : declaration error : incompatible scopes\n" +
        //        ":2\n" +
        //        ":error\n") { out }
    }

    @Test
    fun ee_14_bcast_tuple_func_ok() {
        val out = all("""
            var fff = func (v) {
                println(v)
            }
            var T = task () {
                yield(nil)
                fff(evt)
            }
            spawn T()
            broadcast in :global, [1]
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun ee_15_bcast_tuple_func_no() {
        val out = all("""
            var f = func (v) {
                var x = v[0]
                println(x)
            }
            var T = task () {
                yield(nil)
                f(evt)
            }
            spawn T()
            broadcast in :global, [[1]]
        """)
        assert(out == "[1]\n") { out }
        //assert(out == "anon : (lin 11, col 13) : broadcast in :global, [[1]]\n" +
        //        "anon : (lin 8, col 17) : f(evt)\n" +
        //        "anon : (lin 3, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun ee_15_bcast_tuple_func_ok() {
        val out = all("""
            var f = func (v) {
                val :tmp x = v[0]
                println(x)
            }
            var T = task () {
                yield(nil)
                f(evt)
            }
            spawn T()
            broadcast in :global, [[1]]
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun ee_16_bcast_tuple_func_ok() {
        val out = all("""
            var f = func (v) {
                val :tmp x = [0]
                set x[0] = v[0]
                println(x[0])
            }
            var T = task () {
                yield(nil)
                f(evt)
            }
            spawn T()
            broadcast in :global, [[1]]
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun ee_17_bcast_tuple_func_ok() {
        val out = all("""
            val f = func (v) {
                println(v)
            }
            val T = task () {
                yield(nil)
                f(evt)
            }
            spawn T()
            do {
                do {
                    do {
                        do {
                            do {
                                broadcast in :global, []
                            }
                        }
                    }
                }
            }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun ee_18_bcast_tuple_func_ok() {
        val out = all("""
            val f = func (v) {
                println(v)
            }
            val T = task () {
                yield(nil)
                do {
                    f(evt)
                }
            }
            spawn T()
            do {
                do {
                    do {
                        broadcast in :global, []
                    }
                }
            }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun ee_19_bcast_copy() {
        val out = all("""
            val T = task () {
                yield(nil)
                val v = copy(evt)
                println(v)
            }
            spawn T()
            broadcast in :global, [1,2,3]
        """)
        assert(out == "[1,2,3]\n") { out }
    }

    // POOL
    // COROS ITERATOR

    @Test
    fun ff_pool0() {
        val out = ceu.all(
            """
            var T
            set T = task () { nil }
            var x
            spawn in tasks(), T(x)
            println(0)
        """
        )
        assert(out == "0\n") { out }
    }
    @Test
    fun ff_pool0a() {
        val out = ceu.all(
            """
            var x
            spawn in tasks(), (task(){nil})(x)
            println(0)
        """
        )
        assert(out == "0\n") { out }
    }
    @Test
    fun ff_pool0b() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield(nil) }
            var x
            spawn in tasks(), T(x)
            println(0)
        """
        )
        assert(out == "0\n") { out }
    }
    @Test
    fun ff_pool1() {
        val out = all("""
            var ts
            set ts = tasks()
            println(type(ts))
            var T
            set T = task (v) {
                println(v)
                yield(nil)
                println(evt)
            }
            do {
                spawn in ts, T(1)
            }
             broadcast in :global, 2
        """)
        assert(out == ":x-tasks\n1\n2\n") { out }
    }
    @Test
    fun ff_pool2_leak() {
        val out = all("""
            var T
            set T = task () {
                pass [1,2,3]
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn in ts, T(1)
            spawn in ts, T(2)
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ff_pool4_defer() {
        val out = all("""
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn in ts, T(1)
            spawn in ts, T(2)
            println(0)
        """)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun ff_pool5_scope() {
        val out = all("""
            do {
                var ts
                set ts = tasks()
                var T
                set T = task (v) {
                    println(v)
                    val v' = yield(nil)
                    println(v')
                }
                spawn in ts, T(1)
            }
             broadcast in :global, 2
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ff_pool6_terminate() {
        val out = all("""
            do {
                var ts
                set ts = tasks()
                var T
                set T = task (v) {
                    println(v)
                }
                spawn in ts, T(1)
                loop in :tasks ts, t {
                    throw(1)    ;; never reached
                }
            }
            broadcast in :global, 2
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ff_pool7_leak() {
        val out = all("""
            var T
            set T = task () {
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn in ts, T()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ff_pool8_err() {
        val out = all("""
            loop in :tasks nil, x {
                10
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid expression : innocuous expression") { out }
    }
    @Test
    fun ff_pool8a_err() {
        val out = all("""
            loop in :tasks nil, x {
                pass nil
            }
        """)
        assert(out == "anon : (lin 2, col 28) : loop error : expected tasks\n:error\n") { out }
    }
    @Test
    fun ff_pool9_term() {
        val out = all("""
            var T = task () {
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, xxx {
                println(1)
                broadcast in :global, 1
            }
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun ff_pool10_term() {
        val out = all("""
            var T
            set T = task () {
                yield(nil)
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, xxx {
                println(1)
                broadcast in :global, 1
                loop in :tasks ts, yyy {
                    println(2)
                }
            }
            println(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun ff_pool11_plain() {
        val out = all("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn in ts, T()
            var yyy
            loop in :tasks ts, xxx {
                set yyy = xxx
            }
            println(status(detrack(yyy)))
        """)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun ff_pool11_move() {
        val out = all("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn in ts, T()
            var yyy
            loop in :tasks ts, xxx {
                set yyy = move(xxx)
            }
            println(status(detrack(yyy)))
        """)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun ff_pool11_check() {
        val out = all("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, xxx {
                println(detrack(xxx) == detrack(xxx))
            }
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_pool11_copy() {
        val out = all("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn in ts, T()
            var yyy
            loop in :tasks ts, xxx {
                set yyy = copy(xxx)
            }
            println(status(detrack(yyy)))
        """)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun ff_pool11a_err_scope() {
        val out = all("""
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = tasks()
            spawn in ts, T()
            var yyy
            loop in :tasks ts, xxx {
                set yyy = copy(xxx)
            }
            broadcast in :global, nil
            println(detrack(yyy))
        """)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun ff_pool12_bcast() {
        val out = all("""
            var T = task () { yield(nil); println(:ok) }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, xxx {
                broadcast in detrack(xxx), nil
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ff_pool12_err_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, xxx {
                var yyy
                loop in :tasks ts, zzz {
                    set yyy = copy(zzz)
                    println(status(detrack(yyy)))
                }
                println(status(detrack(yyy)))
                set yyy = xxx
            }
        """
        )
        //assert(out == "anon : (lin 10, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 10, col 25) : set error : incompatible scopes\n:error\n") { out }
        assert(out == ":yielded\n:yielded\n") { out }
    }
    @Test
    fun ff_pool13_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, xxx {
                var yyy
                loop in :tasks ts, zzz {
                    pass nil
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
    fun ff_poolX_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, xxx {
                pass xxx
            }
            println(1)
        """
        )
        //assert(out == "anon : (lin 7, col 13) : set error : incompatible scopes\n:error\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun ff_pool14_max_err() {
        val out = ceu.all(
            """
            tasks(0)
        """
        )
        assert(out == "anon : (lin 2, col 13) : tasks(0)\n" +
                "tasks error : expected positive number\n" +
                ":error\n") { out }
    }
    @Test
    fun ff_pool15_max_err() {
        val out = ceu.all(
            """
            tasks(nil)
        """
        )
        assert(out == "anon : (lin 2, col 13) : tasks(nil)\n" +
                "tasks error : expected positive number\n" +
                ":error\n") { out }
    }
    @Test
    fun ff_pool16_max() {
        val out = ceu.all(
            """
            var ts = tasks(1)
            var T = task () { yield(nil) }
            var ok1 = spawn in ts, T()
            var ok2 = spawn in ts, T()
            broadcast in :global, nil
            var ok3 = spawn in ts, T()
            var ok4 = spawn in ts, T()
            println(ok1, ok2, ok3, ok4)
        """
        )
        assert(out == "true\tfalse\ttrue\tfalse\n") { out }
    }
    @Test
    fun ff_pool17_term() {
        val out = ceu.all(
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
                do { var ok1; set ok1=false; loop until ok1 { yield(nil;) if type(evt)/=:x-task { set ok1=true } else { nil } } }
                ;;yield(nil)
                if v {
                    do { var ok; set ok=false; loop until ok { yield(nil;) if type(evt)/=:x-task { set ok=true } else { nil } } }
                    ;;yield(nil)
                } else {
                    nil
                }
            }
            println(0)
            spawn in ts, T(false)
            spawn in ts, T(true)
            println(1)
            broadcast in :global, @[]
            println(2)
            broadcast in :global, @[]
            println(3)
        """
        )
        assert(out == "0\n10\n10\n1\n20\n30\n2\n20\n30\n3\n") { out }
    }
    @Test
    fun ff_pool18_throw() {
        val out = ceu.all(
            """
            println(1)
            catch err==:ok {
                println(2)
                spawn task () {
                    println(3)
                    ${yield()}
                    println(6)
                    throw(:ok)
                } ()
                spawn task () {
                    catch :ok {
                        println(4)
                        ${yield()}
                    }
                    println(999)
                } ()
                println(5)
                broadcast in :global, nil
                println(9999)
            }
            println(7)
        """
        )
        assert(out == "1\n2\n3\n4\n5\n6\n7\n") { out }
    }
    @Test
    fun ff_pool19_throw() {
        val out = ceu.all(
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
            broadcast in :global, nil
        """
        )
        assert(out == "1\n2\n1\n2\n") { out }
    }
    @Test
    fun ff_pool20_throw() {
        val out = ceu.all(
            """
            var ts
            set ts = tasks(2)
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                catch err==:ok {
                    spawn task () {
                        yield(nil)
                        if v == 1 {
                            throw(:ok)
                        } else {
                            nil
                        }
                        loop { yield(nil) }
                    } ()
                    loop { yield(nil) }
                }
                println(v)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            broadcast in :global, nil
            broadcast in :global, nil
            println(999)
        """
        )
        assert(out == "1\n1\n999\n2\n") { out }
    }
    @Test
    fun ff_pool21_throw() {
        val out = ceu.all(
            """
            var ts
            set ts = tasks(2)
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                catch err==:ok {
                    spawn task () {
                        yield(nil)
                        if v == 2 {
                            throw(:ok)
                        } else {
                            nil
                        }
                        loop { yield(nil) }
                    } ()
                    loop { yield(nil) }
                }
                println(v)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            broadcast in :global, nil
            broadcast in :global, nil
            println(999)
        """
        )
        assert(out == "2\n2\n999\n1\n") { out }
    }
    @Test
    fun ff_pool22() {
        val out = all("""
            var ts
            set ts = tasks()
            println(type(ts))
            var T
            set T = task (v) {
                set task.pub = v
                val v' = yield(nil)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            
            loop in :tasks ts, t1 {
                loop in :tasks ts, t2 {
                    println(detrack(t1).pub, detrack(t2).pub)
                }
            }
             broadcast in :global, 2
        """)
        assert(out == ":x-tasks\n1\t1\n1\t2\n2\t1\n2\t2\n") { out }
    }
    @Test
    fun ff_pool23_throw() {
        val out = ceu.all(
            """
            spawn (task () {
                catch err==:ok {
                    spawn task () {
                        yield(nil)
                        throw(:ok)
                    } ()
                    loop { yield(nil) }
                }
            })()
            broadcast in :global, nil
            println(999)
        """
        )
        assert(out == "999\n") { out }
    }
    @Test
    fun ff_pool24_term() {
        val out = ceu.all(
            """
            var T
            set T = task () {
                yield(nil)
                throw(nil)
            }
            spawn T()
            spawn T()
            broadcast in :global, @[]
        """
        )
        assert(out == "anon : (lin 9, col 13) : broadcast in :global, @[]\n" +
                "anon : (lin 5, col 17) : throw(nil)\n" +
                "throw error : uncaught exception\n" +
                "nil\n") { out }
    }
    @Test
    fun todo_pool25_valgrind() {
        val out = ceu.all(
            """
            var ts
            set ts = tasks(1)
            var T
            set T = task () { yield(nil) }
            var ok1
            set ok1 = spawn in ts, T()
            broadcast in :global, nil
            var ok2
            set ok2 = spawn in ts, T()
            println(ok1, ok2)
        """
        )
        assert(out == "true\tfalse\ttrue\tfalse\n") { out }
    }
    @Test
    fun todo_ff_pool26_reuse_awake() {
        val out = ceu.all(
            """
            var T = task (n) {
                set task.pub = n
                yield(nil)
                ;;println(:awake, evt, n)
                loop until evt == n {
                    yield(nil)
                }
                ;;println(:term, n)
            }
            var ts = tasks(2)
            spawn in ts, T(1)
            spawn in ts, T(2)
            loop in :tasks ts, t {
                println(:t, detrack(t).pub)
                ;;println(:bcast1)
                broadcast in :global, 2         ;; opens hole for 99 below
                ;;println(:bcast2)
                var ok = spawn in ts, T(99)     ;; must not fill hole b/c ts in the stack
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
    @Test
    fun ff_pool27_term() {
        val out = all("""
            var T = task () {
                spawn task () {
                    yield(nil)
                }()
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T()
            spawn in ts, T()
            spawn task () {
                loop in :tasks ts, xxx {
                    println(1)
                    broadcast in :global, 1
                }
            } ()
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun ff_pool28_val() {
        val out = all("""
            val T = task () {
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, trk {
                val :tmp tsk = detrack(trk)
                broadcast nil
            }
            println(:ok)
        """)
        assert(out == "anon : (lin 8, col 17) : declaration error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun ff_pool29_val() {
        val out = all("""
            val T = task () {
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, trk {
                val      tsk1 = detrack(trk)
                val :tmp tsk2 = detrack(trk)
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 8, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun ff_pool30_scope() {
        val out = all("""
            var T = task () { yield(nil) }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, xxx {
                do {
                    val zzz = detrack(xxx)
                    nil
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // EVT

    @Test
    fun gg_evt_hld1_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                yield(nil)
                do {
                    val xxx' = evt
                    nil
                }
            }
            var co = spawn(tk)()
            broadcast in :global, []
            println(`:number ceu_gc_count`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 4, col 27) : invalid evt : cannot expose dynamic \"evt\"\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun gg_evt_hld1_1_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                yield(nil)
                do {
                    val xxx' = evt[0]
                }
            }
            spawn (tk) ()
            broadcast in :global, [[]]
            println(`:number ceu_gc_count`)
        """
        )
        //assert(out == "anon : (lin 8, col 13) : broadcast in :global, [[]]\n" +
        //        "anon : (lin 4, col 31) : invalid index : cannot expose dynamic \"evt\" field\n:error\n") { out }
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 8, col 13) : broadcast in :global, [[]]\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun gg_evt_hld2_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                yield(nil)
                do {
                    val xxx' = evt
                    nil
                }
            }
            var co = spawn (tk) (1)
            broadcast in :global, []
            println(`:number ceu_gc_count`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 10, col 14) : broadcast in :global, []\n" +
        //        "anon : (lin 5, col 27) : invalid evt : cannot expose dynamic \"evt\"\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun gg_evt_hld2a() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                yield(nil)
                do {
                    val xxx' = evt
                }
                    yield(nil)
            }
            var co = spawn (tk)(1)
            broadcast in :global, []
            println(`:number ceu_gc_count`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 10, col 14) : broadcast in :global, []\n" +
        //        "anon : (lin 5, col 27) : invalid evt : cannot expose dynamic \"evt\"\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun gg_evt_hld3() {
        val out = ceu.all(
        """
            var fff
            set fff = func (x) { x }
            spawn task () {
                yield(nil)
                loop until evt[:type]==:x {
                    yield(nil)
                }
                println(99)
            }()
            println(1)
            broadcast in :global, @[(:type,:y)]
            println(2)
            broadcast in :global, @[(:type,:x)]
            println(3)
        """)
        assert(out == "1\n2\n99\n3\n") { out }
    }
    @Test
    fun gg_evt_hld4() {
        val out = ceu.all(
            """
            var fff
            set fff = func (x) { x }
            spawn task () {
                println(1)
                do {
                    println(2)
                    yield(nil)
                    println(3)
                }
                println(4)
                fff(evt[:type])
                println(99)
            }()
            broadcast in :global, @[(:type,:y)]
            broadcast in :global, @[(:type,:x)]
        """
        )
        //assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "1\n2\n3\n4\n99\n") { out }
    }
    @Test
    fun gg_evt45() {
        val out = ceu.all(
            """
            spawn task () {
                println(111)
                yield(nil)
                println(222)
            }()
            println(1)
            broadcast in :global, nil
            println(2)
        """
        )
        assert(out == "111\n1\n222\n2\n") { out }
    }
    @Test
    fun gg_evt5() {
        val out = ceu.all(
            """
            spawn task () {
                loop {
                    println(evt)
                    yield(nil)
                }
            }()
            broadcast in :global, @[]
        """
        )
        assert(out == "nil\n@[]\n") { out }
    }
    @Test
    fun gg_evt6() {
        val out = ceu.all(
            """
            spawn task () {
                loop {
                    do {
                        yield(nil)
                    }
                    println(evt)
                }
            }()
            broadcast in :global, @[]
        """
        )
        assert(out == "@[]\n") { out }
    }
    @Test
    fun gg_evt7_err() {
        val out = ceu.all(
            """
            var x
            set x = []
            broadcast in :global, x
            println(x)
        """
        )
        //assert(out == "anon : (lin 4, col 13) : set error : incompatible scopes\n") { out }
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 4, col 35) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun gg_evt_hld8_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                yield(nil)
                do {
                    val xxx' = evt[0]
                    nil
                }
            }
            var co = spawn (tk) ()
            broadcast in :global, #[[]]
            println(`:number ceu_gc_count`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 8, col 13) : broadcast in :global, [[]]\n" +
        //        "anon : (lin 4, col 31) : invalid index : cannot expose dynamic \"evt\" field\n") { out }
        //assert(out == "anon : (lin 9, col 13) : broadcast in :global, #[[]]\n" +
        //        "anon : (lin 5, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun gg_evt_hld9_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                yield(nil)
                val xxx' = evt[0]
            }
            var co = spawn (tk) ()
            broadcast in :global, @[(1,[])]
            println(`:number ceu_gc_count`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 8, col 13) : broadcast in :global, [[]]\n" +
        //        "anon : (lin 4, col 31) : invalid index : cannot expose dynamic \"evt\" field\n") { out }
    }

    // PUB

    @Test
    fun hh_pub1_err() {
        val out = all("""
            var a
            a.pub
        """, true)
        assert(out == "anon : (lin 3, col 15) : pub error : expected task\n:error\n") { out }
    }
    @Test
    fun hh_pub2_err() {
        val out = all("""
            task.pub
        """, true)
        //assert(out == "anon : (lin 2, col 18) : pub error : expected enclosing task") { out }
        assert(out == "anon : (lin 2, col 13) : task error : missing enclosing task") { out }
    }
    @Test
    fun hh_pub3() {
        val out = all("""
            var t
            set t = task (v1) {
                set task.pub = v1
                yield(nil)
                set task.pub = task.pub + evt
                task.pub
            }
            var a = spawn (t) (1)
            println(a.pub)
            broadcast in a, 2
            println(a.pub)
        """, true)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun hh_pub4_err() {
        val out = all("""
            val t = task () {
                set task.pub = []
            }
            var a = spawn (t) ()
        """)
        //assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "anon : (lin 5, col 28) : t()\n" +
                "anon : (lin 2, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub4a_err() {
        val out = all("""
            val T = task () {
                set task.pub = []
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
        assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n" +
                ":error\n") { out }
        //assert(out == "anon : (lin 11, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 8, col 32) : t()\n" +
        //        "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub5() {
        val out = all("""
            var t
            set t = task () {
                set task.pub = 10
            }
            var a = spawn t()
            println(a.pub + a.pub)
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun hh_pub56_pool() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]  ;; valgrind test
            }
            spawn T()
            println(1)
        """)
        assert(out == "anon : (lin 6, col 19) : T()\n" +
                "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub562_pool() {
        val out = all("""
            var T
            set T = task () {
                pass task.pub ;; useless test
                nil
            }
            spawn T()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_pub6_pool() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn in ts, T()
            var x
            loop in :tasks ts, t {
                println(detrack(t).pub[0])
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_pub7_pool_err() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn in ts, T()
            var x
            loop in :tasks ts, t {
                set x = detrack(t).pub   ;; TODO: incompatible scope
            }
            println(999)
        """)
        //assert(out == "20\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        assert(out == "anon : (lin 12, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub8_fake_task() {
        val out = all("""
            spawn (task () {
                set task.pub = 1
                spawn (task () :fake {
                    println(task.pub)
                }) ()
                nil
            }) ()
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_pub9_fake_task_err() {
        val out = all("""
            spawn (task () {
                set task.pub = []
                var x
                spawn (task () :fake {
                    set x = task.pub
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
    fun hh_pub9_10_fake_task() {
        val out = all("""
            spawn (task () {
                set task.pub = [10]
                var x
                spawn (task () :fake {
                    set x = task.pub[0]
                }) ()
                println(x)
            }) ()
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_pub10_fake_err() {
        val out = all("""
            spawn (task () :fake {
                task.pub
            }) ()
        """, true)
        //assert(out == "anon : (lin 3, col 22) : pub error : expected enclosing task") { out }
        assert(out == "anon : (lin 3, col 17) : task error : missing enclosing task") { out }
    }
    @Test
    fun hh_pub11_err() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]
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
        assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == "anon : (lin 13, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub12_index_err() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]
                yield(nil)
            }
            var t = spawn(T)()
            var x
            set x = t.pub   ;; no expose
        """)
        assert(out == "anon : (lin 9, col 17) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == "anon : (lin 11, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub13_index_err() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]
                yield(nil)
            }
            var t = spawn(T)()
            println(t.pub)   ;; no expose
        """)
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun pub14_index_err() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [[@[(:x,10)]]]
                yield(nil)
            }
            var t = spawn T ()
            println(t.pub[0][0][:x])   ;; no expose
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 10, col 27) : invalid index : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub15_task() {
        val out = all("""
        spawn task () :fake { 
            var y
            set y = do {     
                var ceu_spw_54     
                set ceu_spw_54 = spawn task () :fake {         
                    yield(nil)         
                    [2]             
                }()        
                yield(nil)     
                ;;println(ceu_spw_54.pub)     
                ceu_spw_54.pub        
            }     
            println(y) 
        }()
        broadcast in :global, nil
        """)
        assert(out == "[2]\n") { out }
        //assert(out == "anon : (lin 16, col 9) : broadcast in :global, nil\n" +
        //        "anon : (lin 12, col 28) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub15a_task_err() {
        val out = all("""
        spawn task () :fake { 
            var y
            set y = do {     
                var ceu_spw_54     
                set ceu_spw_54 = spawn task () {         
                    set task.pub = [2]             
                    yield(nil)         
                }()        
                ;;yield(nil)     
                ;;println(ceu_spw_54.pub)     
                ceu_spw_54.pub        
            }     
            println(y) 
        }()
        broadcast in :global, nil
        """)
        assert(out == "anon : (lin 2, col 15) : (task () :fake { var y set y = do { var ceu_s...)\n" +
                "anon : (lin 4, col 21) : block escape error : incompatible scopes\n" +
                ":error\n") { out }
        //assert(out == "anon : (lin 16, col 9) : broadcast in :global, nil\n" +
        //        "anon : (lin 12, col 28) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun todo_hh_pub16_err() {
        val out = all("""
            var t
            set t = task (v) {
                set task.pub = v
                yield(nil)
                var x
                set x = [2]
                set task.pub = x
                yield(nil)
                set task.pub = @[(:y,copy(evt))]
                move(task.pub)
            }
            var a = spawn (t) ([1])
            ;;println(a.pub)
            broadcast in a, nil
            ;;println(a.pub)
            broadcast in a, [3]
            ;;println(a.pub)
        """, true)
        //assert(out == "[1]\n[2]\n@[(:y,[3])]\n") { out }
        assert(out == "TODO\n") { out }
    }
    @Test
    fun hh_17_pub_out() {
        val out = all("""
            var t = task () {
                set task.pub = []
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
    fun hh_18_pub_out_err() {
        val out = all("""
            var t = task () {
                set task.pub = []
                task.pub
            }
            var a = spawn (t) ()
            println(a.pub)
        """)
        //assert(out == "[]\n") { out }
        assert(out == "anon : (lin 6, col 28) : t()\n" +
                "anon : (lin 2, col 29) : block escape error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun hh_19_pub_tasks_tup() {
        val out = all("""
            val tup = []
            val T = task () {
                set task.pub = tup
                yield(nil)
            }
            val ts = tasks()
            spawn in ts, T()
            spawn in ts, T()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun hh_20_pub_pass() {
        val out = all("""
            val S = task (t) {
                println(t)
                yield(nil)
            }
            val T = task () {
                set task.pub = [1,2,3]
                spawn S(task.pub)
                nil
            }
            spawn T()
        """)
        assert(out == "[1,2,3]\n") { out }
    }

    // STATUS

    @Test
    fun ii_status1_err() {
        val out = all("""
            var a
            status(a)
        """, true)
        assert(out == "anon : (lin 3, col 13) : status(a) : status error : expected coroutine\n:error\n") { out }
    }
    @Test
    fun ii_status2_err() {
        val out = all("""
            status(task)
        """, true)
        //assert(out == "anon : (lin 2, col 18) : status error : expected enclosing task") { out }
        assert(out == "anon : (lin 2, col 20) : task error : missing enclosing task") { out }
    }
    @Test
    fun ii_status3_err() {
        val out = all("""
            var t
            set t = task () {
                set status(task) = nil     ;; error: cannot assign to status
            }
        """, true)
        assert(out == "anon : (lin 4, col 17) : invalid set : expected assignable destination") { out }
    }
    @Test
    fun ii_status4() {
        val out = all("""
            var t
            set t = coro () {
                println(10, status(coro))
                yield(nil)
                println(20, status(coro))
            }
            var a
            set a = coroutine(t)
            println(1, status(a))
            resume a()
            println(2, status(a))
            resume a()
            println(3, status(a))
        """)
        //assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "1\t:yielded\n10\t:resumed\n2\t:yielded\n20\t:resumed\n3\t:terminated\n") { out }
    }
    @Test
    fun todo_ii_status5() {
        val out = all("""
            var T
            set T = task (x) {
                println(10, status(task))
                yield(nil)
                if x {
                    yield(nil)
                } else {
                    nil
                }
                println(20, status(task))
            }
            spawn task () {
                do {
                    var t1
                    set t1 = coroutine(T)
                    resume t1(false)
                    var t2
                    set t2 = coroutine(T)
                    resume t2(true)
                    ${await("evt == t1")}
                    println(:ok)
                }
            } ()
            broadcast in :global, nil
        """)
        //assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "1\t:yielded\n10\t:resumed\n2\t:yielded\n20\t:resumed\n3\t:terminated\n") { out }
    }

    // TOGGLE

    @Test
    fun todo_jj_toggle1() { // should be rt error
        val out = all("""
            var T
            set T = task () {
                yield(nil)
                println(10)
            }
            var t
            set t = spawn T()
            toggle t (false)
            resume t ()
        """)
        assert(out.contains("Assertion `ceu_x->Bcast.status==CEU_X_STATUS_YIELDED || ceu_evt==&CEU_EVT_CLEAR' failed")) { out }
    }
    @Test
    fun jj_toggle2_err() {
        val out = all("""
            toggle 1 (true)
        """)
        assert(out == "anon : (lin 2, col 20) : toggle error : expected yielded/toggled coroutine\n:error\n") { out }
    }
    @Test
    fun jj_toggle3_coro() {
        val out = all("""
            var T
            set T = task () {
                yield(nil)
                println(10)
            }
            var t
            set t = spawn T()
            toggle t (false)
            println(1)
            broadcast in :global, nil
            toggle t (true)
            println(2)
            broadcast in :global, nil
        """)
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun jj_toggle4_tasks() {
        val out = all("""
            var T
            set T = task () {
                yield(nil)
                println(10)
            }
            var ts
            set ts = tasks()
            spawn in ts, T()
            toggle ts (false)
            println(1)
            broadcast in :global, nil
            toggle ts (true)
            println(2)
            broadcast in :global, nil
        """)
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun jj_toggle5_defer() {
        val out = all("""
            var T
            set T = task () {
                defer {
                    println(10)
                }
                ${yield()}
                println(999)
            }
            var t
            set t = spawn T()
            toggle t (false)
            println(1)
            broadcast in :global, nil
            println(2)
        """)
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun jj_toggle6_defer_tasks() {
        val out = all("""
            var T
            set T = task () {
                defer {
                    println(10)
                }
                yield(nil)
                println(999)
            }
            var ts
            set ts = tasks()
            spawn in ts, T()
            toggle ts (false)
            println(1)
            broadcast in :global, nil
            println(2)
        """)
        assert(out == "1\n2\n10\n") { out }
    }

    // ESCAPE

    @Test
    fun kk_esc1_err() {
        val out = all("""
            var f
            set f = func () {
                coroutine(coro() {nil})
            }
            println(f())
        """, true)
        //assert(out == "anon : (lin 6, col 21) : f()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out.contains("coro: 0x"))
    }
    @Test
    fun kk_esc2() {
        val out = all("""
            var T
            set T = coro () { nil }
            var xxx
            do {
                var t
                set t = coroutine(T)
                set xxx = t ;; error
            }
        """)
        assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n:error\n") { out }
    }

    // TRACK / DETRACK

    @Test
    fun ll_track1_err() {
        val out = all("""
            track(nil)
        """)
        assert(out == "anon : (lin 2, col 13) : track(nil)\n" +
                "track error : expected task\n" +
                ":error\n") { out }
    }
    @Test
    fun ll_track2() {
        val out = all("""
            var T = task () { yield(nil) }
            var t = spawn T()
            var x = track(t)
            println(t, x)
        """)
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun ll_track2a() {
        val out = all("""
            var T = task () { yield(nil) }
            var t = spawn T ()
            var x = track(t)
            var y = track(t)
            var z = y
            println(x==y, y==z)
        """)
        assert(out.contains("false\ttrue\n")) { out }
    }
    @Test
    fun ll_track3_err() {
        val out = all("""
            var T = task () { nil }
            var t = spawn T()
            var x
            set x = track(t) ;; error: dead coro
            ;;println(t.status)
            println(x)
        """)
        assert(out == "anon : (lin 5, col 21) : track(t)\n" +
                "track error : expected unterminated task\n" +
                ":error\n") { out }
    }
    @Test
    fun ll_track4() {
        val out = all("""
            var T
            set T = task () {
                yield(nil)
                set task.pub = 10
                yield(nil)
            }
            var t = spawn T()
            var x = track(t)
            println(detrack(x).pub) 
            broadcast in :global, nil ;;resume t()
            println(detrack(x).pub) 
        """)
        assert(out == "nil\n10\n") { out }
    }
    @Test
    fun ll_track5_err() {
        val out = all("""
            var x
            set x = nil
            println(x.pub)  ;; not coro/track()
        """)
        assert(out == "anon : (lin 4, col 23) : pub error : expected task\n:error\n") { out }
    }
    @Test
    fun ll_track5_err2() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]
                yield(nil)
            }
            var t = spawn T()
            var x = track(t)
            println(detrack(x).pub)      ;; expose (ok, global func)
        """)
        //assert(out == "anon : (lin 12, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun ll_track6_err() {
        val out = all("""
            var T
            set T = task () { yield(nil) }
            var x
            do {
                var t = spawn (T) ()
                set x = track(t)         ;; error scope
            }
            println(status(detrack(x)))
            println(x)
        """)
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun ll_track7() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]
                yield(nil)
            }
            var t = spawn T ()
            var x = track(t)
            println(detrack(x).pub[0])
            broadcast in :global, nil
            println(detrack(x))
        """)
        assert(out == "10\nnil\n") { out }
    }
    @Test
    fun ll_track8_err() {
        val out = all("""
            var T
            set T = task () {
                set task.pub = [10]
                yield(nil)
            }
            var x
            do {
                var t = spawn (T) ()
                set x = track(t)         ;; scope x < t
                println(detrack(x).pub[0])
            }
            println(status(detrack(x)))
            println(x)
        """)
        //assert(out.contains("10\n:terminated\nx-track: 0x")) { out }
        assert(out == "anon : (lin 10, col 21) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun ll_track09_err() {
        val out = all("""
            var T = task (v) {
                yield(nil)
            }
            var x
            var ts = tasks()
            spawn in ts, T(1)
            loop in :tasks ts, t {
                set x = track(t)
            }
        """)
        assert(out == "anon : (lin 9, col 25) : track(t)\n" +
                "track error : expected task\n" +
                ":error\n") { out }
    }
    @Test
    fun ll_track09() {
        val out = all("""
            var T = task (v) {
                set task.pub = [v]
                yield(nil)
            }
            var x
            var ts = tasks()
            spawn in ts, T(1)
            spawn in ts, T(2)
            loop in :tasks ts, t {
                set x = copy(t)
            }
            println(detrack(x).pub[0])   ;; 2
            broadcast in :global, nil
            println(detrack(x))   ;; nil
        """)
        assert(out == "2\nnil\n") { out }
    }
    @Test
    fun ll_track10() {
        val out = all("""
            var T
            set T = task (v) {
                set task.pub = [v]
                yield(nil)
            }
            var x
            var ts
            set ts = tasks()
            do {
                spawn in ts, T(1)
                spawn in ts, T(2)
                loop in :tasks ts, t {
                    set x = copy(t)    ;; track(t) up_hold in
                }
                println(detrack(x).pub[0])   ;; 2
                broadcast in :global, nil
                println(detrack(x))   ;; nil
            }
        """)
        assert(out == "2\nnil\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun ll_track11_err() {
        val out = all("""
            var T
            set T = task (v) {
                set task.pub = [v]
                yield(nil)
            }
            var x
            do {
                var ts
                set ts = tasks()
                spawn in ts, T(1)
                loop in :tasks ts, t {
                    set x = t       ;; err: escope 
                }
            }
        """)
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun ll_track12_throw() {
        val out = all("""
            var T
            set T = task (v) {
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn in ts, T(1)
            var x
            set x = catch true {
                loop in :tasks ts, t {
                    throw(copy(t))
                }
            }
            broadcast in :global, nil
            println(detrack(x))   ;; nil
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun ll_track13_throw() {
        val out = all("""
            var T
            set T = task (v) {
                set task.pub = [v]
                yield(nil)
            }
            var ts
            set ts = tasks()
            spawn in ts, T(1)
            spawn in ts, T(2)
            var x
            set x = catch true {
                loop in :tasks ts, t {
                    throw(copy(t))
                }
            }
            println(detrack(x).pub[0])   ;; 1
            println(status(detrack(x)))   ;; :yielded
            broadcast in :global, nil
            println(detrack(x))   ;; nil
        """)
        assert(out == "1\n:yielded\nnil\n") { out }
    }
    @Test
    fun ll_track14() {
        val out = all("""
            var T = task () {
                set task.pub = [10]
                ${await("evt==:evt")}
            }
            var t = spawn T()
            var x = track(t)
            spawn task () {
                catch :par-or {
                    spawn task () {
                        ${await("if status(detrack(x))==:terminated { true } else { if detrack(x)==nil { true } else { false } }")}
                        throw(:par-or)
                    } ()
                    println(detrack(x).pub[0])
                    broadcast in :global, nil
                    println(detrack(x).pub[0])
                    broadcast in :global, :evt
                    println(detrack(x).pub[0]) ;; never printed
                    ${await("false")}
                }
                println(detrack(x))
            }()
            println(:ok)
        """)
        assert(out == "10\n10\nnil\n:ok\n") { out }
    }
    @Test
    fun ll_track15_simplify() {
        val out = all("""
            var T = task (v) {
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T(1)
            spawn in ts, T(2)
            var x
            loop in :tasks ts, t {
                set x = copy(t)
            }
            broadcast in :global, nil
            println(detrack(x))   ;; nil
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun ll_track16_hold_err() {
        val out = all("""
            var T = task (v) {
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T(1)
            loop in :tasks ts, t {
                var x = detrack(t)
                println(x)
            }
        """)
        //assert(out == "anon : (lin 8, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out.contains("x-task: 0x")) { out }
    }
    @Test
    fun ll_17_detrack_err() {
        val out = all("""
            val T = task () {
                yield(nil)
            }
            val t1 = spawn T()
            val r1 = track(t1)
            val x1 = detrack(r1)
            ;;println(t1, r1, x1, status(t1))
            broadcast in :global, nil
            ;;println(t1, r1, x1, status(t1))
            println(status(t1))
        """)
        //assert(out == ":terminated\n") { out }
        assert(out == "anon : (lin 7, col 13) : declaration error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun ll_18_track_scope() {
        val out = all("""
            val T = task () {
                yield(nil)
            }
            val t = spawn T()
            val y = do {
                val x = track(t)
                x
            }
            println(y)
        """)
        assert(out == "anon : (lin 6, col 21) : block escape error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun ll_19_track_scope() {
        val out = all("""
            val T = task () {
                set task.pub = 1
                yield(nil)
            }
            val t = spawn T()
            val y = do {
                track(t)
            }
            println(detrack(y).pub)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ll_20_track_scope() {
        val out = all("""
            val T = task () {
                set task.pub = 1
                yield(nil)
            }
            val y = do {
                val t = spawn T()
                track(t)
            }
            println(detrack(y).pub)
        """)
        assert(out == "anon : (lin 6, col 21) : block escape error : incompatible scopes\n" +
                ":error\n") { out }
    }

    // EVT / DATA

    @Test
    fun mm_01_data_await() {
        val out = all("""
            data :E = [x,y]
            spawn task () {
                var evt :E
                yield(nil)
                println(evt.x)
            } ()
            broadcast in :global, tags([10,20], :E, true)
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_02_data_await() {
        val out = all("""
            data :E = [x,y]
            data :F = [i,j]
            spawn task () {
                var evt :E
                yield(nil)
                println(evt.x)
                var evt :F
                yield(nil)
                println(evt.j)
            } ()
            broadcast in :global, tags([10,20], :E, true)
            broadcast in :global, tags([10,20], :F, true)
        """, true)
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun mm_03_data_pub_err() {
        val out = all("""
            task () :T { nil }
        """, true)
        assert(out == "anon : (lin 2, col 21) : declaration error : data :T is not declared") { out }
    }
    @Test
    fun mm_04_data_pub() {
        val out = all("""
            data :T = [x,y]
            spawn task () :T {
                set task.pub = [10,20]
                println(task.pub.x)
            } ()
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_05_data_pub_err() {
        val out = all("""
            var t = spawn task () { nil } ()
            println(t.pub.y)
        """, true)
        assert(out == "anon : (lin 3, col 23) : index error : expected collection\n" +
                ":error\n") { out }
    }
    @Test
    fun mm_06_data_pub() {
        val out = all("""
            data :T = [x,y]
            var t :T = spawn task () {
                set task.pub = [10,20]
                yield(nil)
            } ()
            println(t.pub.y)
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun todo_mm_07_data_pub() {
        val out = all("""
            data :T = [x,y]
            var t :T = spawn task () {
                set task.pub = [10,20]
                nil
            } ()
            data :X = [a:T]
            var x :X = [t]
            println(x.a.pub.y)  // TODO: combine Pub/Index
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun todo_mm_08_data_pool_pub() {
        val out = all("""
            data :T = [x,y]
            var ts = tasks()
            spawn in ts, task () {
                set task.pub = [10,20]
                yield(nil)
            } ()
            loop in :tasks ts, t:T {
                println(detrack(t).pub.y)   // TODO: detrack needs to return to grammar
            }
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun mm_09_data_pool_pub() {
        val out = all("""
            spawn task () {
                set task.pub.x = 10
                nil
            } ()
        """)
        assert(out == "anon : (lin 2, col 19) : (task () { set task.pub[:x] = 10 nil })()\n" +
                "anon : (lin 3, col 26) : index error : expected collection\n" +
                ":error\n") { out }
    }

    // EXPOSE

    @Test
    fun nn_01_expose_err() {
        val out = all("""
            var t = task () {
                set task.pub = []
                yield(nil)
                nil
            }
            var a = spawn (t) ()
            var x = a.pub
            println(x)
        """)
        assert(out == "anon : (lin 8, col 13) : declaration error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun nn_02_expose_err() {
        val out = all("""
            var f = func (t) {
                var p = t.pub   ;; ok
                set p = t.pub   ;; ok
                set p = p       ;; ok
                println(p)      ;; ok
                p               ;; ok
            }
            var t = task () {
                set task.pub = []
                yield(nil)
                nil
            }
            var a = spawn (t) ()
            f(a)
            nil
        """)
        //assert(out == "[]\n") { out }
        assert(out == "anon : (lin 15, col 13) : f(a)\n" +
                "anon : (lin 2, col 30) : block escape error : incompatible scopes\n" +
                "[]\n" +
                ":error\n") { out }
        //assert(out == "anon : (lin 15, col 13) : f(a)\n" +
        //        "anon : (lin 3, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun nn_03_expose_err() {
        val out = all("""
            var f = func (t) {
                var p = t.pub   ;; ok
                p               ;; ok
            }
            var t = task () {
                set task.pub = []
                yield(nil)
                nil
            }
            var a = spawn (t) ()
            var x = f(a)        ;; no
            println(x)
        """)
        assert(out == "anon : (lin 12, col 21) : f(a)\n" +
                "anon : (lin 2, col 30) : block escape error : incompatible scopes\n" +
                ":error\n") { out }
        //assert(out == "anon : (lin 12, col 21) : f(a)\n" +
        //        "anon : (lin 3, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun nn_04_expose_err() {
        val out = all("""
            var p
            var f = func (t) {
                set p = t.pub   ;; no
            }
            var t = task () {
                set task.pub = []
                yield(nil)
                nil
            }
            var a = spawn (t) ()
            f(a)
            nil
        """)
        assert(out == "anon : (lin 12, col 13) : f(a)\n" +
                "anon : (lin 4, col 21) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun nn_05_expose() {
        val out = all("""
            var T = task (t) {
                set task.pub = []
                if t {
                    val p = detrack(t).pub
                } else {
                    nil
                }
                yield(nil)
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
    fun nn_06_expose() {
        val out = all("""
            val f = func (t) { false }
            val T = task () {
                set task.pub = []
                yield(nil)
            }
            val ts = tasks()
            do {
                do {
                    do {
                        do {
                            spawn in ts, T()
                        }
                    }
                }
            }
            loop in :tasks ts, xx1 {
                ;;println(xx1)
                f(detrack(xx1).pub)
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun nn_09_throw_track() {
        val out = all("""
            val T = task () {
                set task.pub = 10
                yield(nil)
            }
            val ts = tasks()
            val t = catch true {
                spawn in ts, T()
                loop in :tasks ts, t {
                    throw(move(t))
                }
            }
            println(detrack(t).pub)
        """)
        //assert(out == ":ok\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_10_throw_track() {
        val out = all("""
            val T = task () {
                yield(nil)
            }
            val t = do {
                val ts = tasks()
                spawn in ts, T()
                loop in :tasks ts, t {
                    throw(move(t))
                    nil
                }
            }
            println(t)
        """)
        //assert(out == ":ok\n") { out }
        assert(out.contains("anon : (lin 5, col 21) : block escape error : incompatible scopes\n")) { out }
    }
    @Test
    fun nn_11_throw_track() {
        val out = all("""
            val T = task () {
                yield(nil)
            }
            val ts = tasks()
            catch true {
                spawn in ts, T()
                loop in :tasks ts, t {
                    throw(move(t))
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    @Test
    fun nn_pub17_err_expose() {
        val out = all("""
            var t
            set t = task () {
                set task.pub = []
            }
            var a = spawn (t) ()
            var x
            set x = a.pub
            println(x)
        """)
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "anon : (lin 6, col 28) : t()\n" +
                "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun nn_pub18_err_expose() {
        val out = all("""
            var t
            set t = task () {
                set task.pub = @[]
            }
            var a = spawn (t) ()
            var x
            set x = a.pub
            println(x)
        """)
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "anon : (lin 6, col 28) : t()\n" +
                "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun nn_pub19_err_expose() {
        val out = all("""
            var t
            set t = task () {
                set task.pub = #[]
            }
            var a = spawn (t) ()
            var x
            set x = a.pub
            println(x)
        """)
        assert(out == "anon : (lin 6, col 28) : t()\n" +
                "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun nn_pub20_func() {
        val out = all("""
            var t
            set t = task (v) {
                set task.pub = v
                var f
                set f = func () {
                    task.pub
                }
                println(f())
            }
            var a = spawn (t)(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun nn_pub21_func_expose() {
        val out = all("""
            var t = task (v) {
                set task.pub = v
                var f = func () {
                    task.pub
                }
                println(f())
            }
            var a = spawn (t) ([1])
        """, true)
        assert(out == "[1]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun nn_pub_22_nopub() {
        val out = all("""
            var U
            set U = task () {
                set task.pub = func () {
                    10
                }
            }
            var T
            set T = task (u) {
                println(u.pub())
            }
            spawn T (spawn U())
            println(:ok)
        """)
        assert(out == "anon : (lin 12, col 28) : U()\n" +
                "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun nn_pub_23_nopub() {
        val out = all("""
            var U
            set U = task () {
                set task.pub = [10]
            }
            var T
            set T = task (u) {
                nil ;;println(u.pub.0)
            }
            spawn T (spawn U())
            println(:ok)
        """)
        assert(out == "anon : (lin 10, col 28) : U()\n" +
                "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun nn_pub_24_nopub() {
        val out = all("""
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
        """)
        assert(out == "anon : (lin 11, col 28) : U()\n" +
                "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun nn_pub25() {
        val out = all("""
            var t = task (v) {
                set task.pub = @[]
                nil
            }
            var a = spawn (t) ()
            println(status(a))
        """, true)
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun nn_pub26_pool_err() {
        val out = all("""
            var T = task () {
                set task.pub = [10]
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
    @Test
    fun nn_pub27_func_expose() {
        val out = all("""
            var t = task (v) {
                set task.pub = v
                var f = func (p) {
                    p
                }
                println(f(task.pub))
            }
            var a = spawn (t) ([1])
        """, true)
        assert(out == "[1]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun nn_pub28_func_tst() {
        val out = all("""
            var t = task (v) {
                set task.pub = v
                var xxx
                nil
            }
            var a = spawn (t) ([])
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun nn_pub30_pool_err() {
        val out = all("""
            var T = task () {
                set task.pub = [10]
                yield(nil)
            }
            var ts = tasks()
            spawn in ts, T()
            loop in :tasks ts, t {
                var f = func (tt) {
                    var x = detrack(tt).pub
                    broadcast in detrack(tt), nil
                    println(x)
                }
                f(t)
            }
            println(999)
        """)
        //assert(out == "20\n") { out }
        //assert(out == "anon : (lin 12, col 36) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        assert(out == "anon : (lin 14, col 17) : f(t)\n" +
                "anon : (lin 10, col 21) : declaration error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun nn_pub31_func_expose() {
        val out = all("""
            var f = func (t) {
                t.pub
            }
            var T = task () {
                set task.pub = []
                yield(nil)
            }
            var t = spawn (T) ()
            println(f(t))
        """)
        //assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        assert(out == "anon : (lin 10, col 21) : f(t)\n" +
                "anon : (lin 2, col 30) : block escape error : incompatible scopes\n" +
                ":error\n") { out }
    }

    // XCEU

    @Test
    fun zz_xceu1() {
        val out = all("""
            spawn task () {
                spawn (task () {
                    yield(nil)
                }) ()
                yield(nil)
                throw(nil)
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun zz_xceu2() {
        val out = all("""
            spawn task () {
                yield(nil)
                println(evt)
                spawn (task () {
                    loop {
                        println(evt)    ;; lost reference
                        yield(nil)
                    }
                }) ()
                yield(nil)
            }()
            broadcast in :global, 10
            broadcast in :global, 20
        """)
        assert(out == "10\nnil\n20\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun zz_xceu3() {
        val out = all("""
            spawn task () {
                yield(nil)
                println(evt)
                spawn (task () :fake {
                    loop {
                        println(evt)    ;; kept reference
                        yield(nil)
                    }
                }) ()
                yield(nil)
            }()
            broadcast in :global, 10
            broadcast in :global, 20
        """)
        assert(out == "10\n10\n20\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun zz_xceu4() {
        val out = all("""
            catch true {
                spawn task () :fake {
                    throw([tags([],:x,true)])
                }()
            }
            println(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun zz_xceu5() {
        val out = all("""
            spawn task () {
                catch err==:or {
                    spawn task () {
                        yield(nil)
                        ;;println(:evt, evt)
                        ;;println(111)
                        throw(:or)
                    }()
                    spawn task () {
                        yield(nil)
                        ;;println(:evt, evt)
                        ;;println(222)
                        throw(:or)
                    }()
                    yield(nil)
                    ;;println(:in)
                }
                ;;println(:out)
            }()
            broadcast in :global, nil
            println(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun zz_xceu6() {
        val out = all("""
            var T
            set T = task (pos) {
                yield(nil)
                println(pos)
            }
            spawn (task () {
                var ts
                set ts = tasks()
                do {
                    spawn in ts, T([])  ;; pass [] to ts
                }
                yield(nil)
                yield(nil)
            })()
            broadcast in :global, nil
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_xceu7() {
        val out = all("""
            spawn task () {
                do {
                    spawn task () {
                        yield(nil)
                    } ()
                    yield(nil)
                }
                broadcast in :global, [[]]
                ;;await true
            }()
            broadcast in :global, nil
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_xceu8() {
        val out = all("""
            spawn task () {
                do {
                    spawn task () {
                        yield(nil)
                    } ()
                    yield(nil)
                }
                broadcast in :global, tags([], :x, true)
            }()
            broadcast in :global, nil
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_xceu9 () {
        val out = all("""
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
            broadcast in :global, nil
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_xceu10 () {
        val out = all("""
            ;;println(:blk0, `:pointer ceu_block`)
            spawn task () {
                ;;println(:cor1, `:pointer ceu_x`)
                ;;println(:blk11, `:pointer ceu_block`)
                loop {
                    ;;println(:blk12, `:pointer ceu_block`)
                    yield(nil); loop until evt==10 { yield(nil) }
                    println(:1)
                    var t = spawn task () {
                        ;;println(:cor2, `:pointer ceu_x`)
                        ;;println(:blk2, `:pointer ceu_block`)
                        yield(nil); loop until evt==10 { yield(nil) }
                    } ()
                    loop until status(t)==:terminated { yield(nil) }
                    println(:2)
                }
            } ()
            ;;println(:xxxxxxxxxxxxxxxxxxxxxxx)
            broadcast in :global, 10    ;; :1
            ;;println(:xxxxxxxxxxxxxxxxxxxxxxx)
            broadcast in :global, 10    ;; :2 (not :1 again)
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun zz_xceu11 () {
        val out = all("""
            data :X = [x]
            task () :X {
                task () :fake {
                    task.pub.x
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_12_kill() {
        val out = all("""
            spawn task () {
                do {
                    val t1 = spawn task () {
                        ${yield()}
                        println(1)
                    } ()
                    spawn task () {
                        defer { println(3) }
                        ${yield()}
                        println(2)
                    } ()
                    ${await ("t1")}
                }
                println(999)
            } ()
            broadcast in :global, nil
        """, true)
        assert(out == "1\n3\n999\n") { out }
    }

}
