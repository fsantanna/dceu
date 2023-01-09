package ceu

import D
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters

fun yield (ok: String = "ok"): String {
    return "do { var $ok; set $ok=true; while $ok { yield(nil); if type(evt)/=:coro { set $ok=false } else { nil } } }"
}
fun await (evt: String): String {
    return "do { var ok; set ok=true; yield(nil); while ok { if $evt { set ok=false } else { yield(nil) } } }"
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TTask {

    // TASK / COROUTINE / RESUME / YIELD

    @Test
    fun aa_task1() {
        val out = all("""
            var t
            set t = task (v) {
                println(v)          ;; 1
                set v = yield((v+1)) 
                println(v)          ;; 3
                set v = yield(v+1) 
                println(v)          ;; 5
                v+1
            }
            var a
            set a = coroutine(t)
            var v
            set v = resume a(1)
            println(v)              ;; 2
            set v = resume a(v+1)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """, true)
        assert(out == "1\n2\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun aa_task2_err() {
        val out = all("""
            coroutine(func () {nil})
        """.trimIndent())
        assert(out == "anon : (lin 1, col 11) : coroutine error : expected task\n:error\n") { out }
    }
    @Test
    fun aa_task3_err() {
        val out = all("""
            var f
            resume f()
        """.trimIndent())
        assert(out == "anon : (lin 2, col 1) : resume error : expected yielded task\n:error\n") { out }
    }
    @Test
    fun aa_task4_err() {
        val out = all("""
            var co
            set co = coroutine(task () {nil})
            resume co()
            resume co()
        """.trimIndent())
        assert(out == "anon : (lin 4, col 1) : resume error : expected yielded task\n:error\n") { out }
    }
    @Test
    fun aa_task5_err() {
        val out = all("""
            var co
            set co = coroutine(task () { nil
            })
            resume co()
            resume co(1,2)
        """)
        assert(out == "anon : (lin 6, col 13) : resume error : expected yielded task\n:error\n") { out }
    }
    @Test
    fun aa_task6() {
        val out = all("""
            var co
            set co = coroutine(task (v) {
                set v = yield(nil) 
                println(v)
            })
            resume co(1)
            resume co(2)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun aa_task7() {
        val out = all("""
            var co
            set co = coroutine(task (v) {
                println(v)
            })
            println(1)
            resume co(99)
            println(2)
        """)
        assert(out == "1\n99\n2\n") { out }
    }
    @Test
    fun aa_task8_err() {
        val out = all("""
            var xxx
            resume xxx() ;;(xxx(1))
        """)
        assert(out == "anon : (lin 3, col 13) : resume error : expected yielded task\n:error\n") { out }
    }
    @Test
    fun aa_task9_mult() {
        val out = all("""
            var co
            set co = coroutine(task (x,y) {
                println(x,y)
            })
            resume co(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun aa_task10_err() {
        val out = all("""
            var co
            set co = coroutine(task () {
                yield(nil)
            })
            resume co()
            resume co(1,2)
        """)
        assert(out.contains("bug found : not implemented : multiple arguments to resume")) { out }
    }
    @Test
    fun aa_task11_class() {
        val out = all("""
            var T
            set T = task (x,y) {
                println(x,y)
            }
            resume (coroutine(T)) (1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun aa_task12_tuple_leak() {
        val out = all("""
            var T
            set T = task () {
                [1,2,3]
                yield(nil)
            }
            resume (coroutine(T)) ()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_task13_defer() {
        val out = all("""
            var T
            set T = task () {
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
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing task") { out }
    }
    @Test
    fun aa_yield15_err() {
        val out = all("""
            task () {
                func () {
                    yield(nil)
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : expected enclosing task") { out }
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
    fun aa_task17_nest_err() {
        val out = all("""
            spawn task (v1) {
                spawn task (v2) {
                    spawn task (v3) {
                        nil ;;println(v1,v2,v3)
                    }(3)
                }(2)
            }(1)
        """)
        //assert(out == "1\t2\t3\n") { out }
        assert(out == "anon : (lin 2, col 19) : task (v1) { spawn task (v2) { spawn task (v3)...)\n" +
                "anon : (lin 3, col 23) : task (v2) { spawn task (v3) { nil }(3) }(2)\n" +
                "anon : (lin 3, col 33) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun aa_task18_defer() {
        val out = all("""
            var T
            set T = task () {
                defer {
                    println(3)
                }
                println(1)
                yield(nil)   ;; never awakes
                println(2)
            }
            var t
            set t = coroutine(T)
            println(0)
            resume t ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }

    // SPAWN

    @Test
    fun bb_spawn1() {
        val out = all("""
            var t
            set t = task (v) {
                println(v)          ;; 1
                set v = yield((v+1)) 
                println(v)          ;; 3
                set v = yield(v+1) 
                println(v)          ;; 5
                v+1
            }
            var a
            set a = spawn t(1)
            println(type(a))
            var v
            set v = resume a(3)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """, true)
        assert(out == "1\n:coro\n3\n4\n5\n6\n") { out }
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
        assert(out == "anon : (lin 2, col 20) : coroutine error : expected task\n:error\n") { out }
    }
    @Test
    fun bb_spawn4_err() {
        val out = all("""
            spawn (func () {nil})
        """)
        assert(out == "anon : (lin 3, col 9) : invalid spawn : expected call") { out }
    }
    @Test
    fun bb_spawn5() {
        val out = all("""
            var t
            set t = task () {
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
        assert(out == "anon : (lin 3, col 21) : set error : incompatible scopes\n:error\n") { out }
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
        """)
        assert(out == "anon : (lin 7, col 30) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun bb_spawn7_err() {
        val out = all("""
            var f
            set f = func () {
                spawn t()
            }
            var t
            set t = task () { nil }
            f()
        """)
        assert(out == "anon : (lin 8, col 13) : f()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun bb_spawn8_err() {
        val out = all("""
            var T
            set T = task () {
                spawn (task () :fake {
                    nil ;;println(1)
                }) ()
            }
            spawn T()
        """)
        //assert(out == "1\n")
        assert(out == "anon : (lin 8, col 19) : T()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
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
    fun bb_spawn10() {
        val out = all("""
            var t
            set t = task () {
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
    fun bb_spawn11() {
        val out = all("""
            var f
            set f = func () {
                t
            }
            var t
            set t = task () {
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
        //assert(out == "1\n") { out }
        assert(out == "anon : (lin 8, col 19) : T()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
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
        """)
        assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n:error\n") { out }
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
            set t = do :unnest :hide {
                var v
                set v = 10
                spawn T(v)  
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_group2() {
        val out = all("""
            do :unnest {
                var T
                set T = task (v) {
                    println(v)
                }
            }
            do :unnest {
                var t
                set t = spawn do :unnest :hide {
                    do :unnest {
                        var v
                        set v = 10
                    }
                    T(v)
                }
            }
            println(type(t))
        """)
        assert(out == "10\n:coro\n") { out }
    }
    @Test
    fun cc_group3() {
        val out = all("""
            var f
            set f = func () {
                nil
            }
            spawn task () :fake {
                do :unnest :hide {
                    f()
                }
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
    }

    // THROW

    @Test
    fun dd_throw1() {
        val out = all("""
            var co
            set co = coroutine(task (x,y) {
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
            set co = coroutine(task (x,y) {
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
            set T = task () :awakes {
                spawn task () :awakes {
                    yield(nil)
                    throw(:error )
                }()
                yield(nil)
            }
            spawn in coroutines(), T()
            broadcast in :global, nil
        """)
        assert(out == "anon : (lin 11, col 13) : broadcast in :global, nil\n" +
                "anon : (lin 6, col 21) : throw error : uncaught exception\n:error\n") { out }
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
            spawn (task () :fake :awakes {
                broadcast in :task, nil
            }) ()
        """)
        assert(out == "anon : (lin 2, col 20) : task () :fake :awakes { broadcast in :task, n...)\n" +
                "anon : (lin 3, col 30) : broadcast error : invalid target\n:error\n") { out }
    }
    @Test
    fun dd_throw6() {
        val out = all("""
            var co
            set co = coroutine (task () {
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
            set co = coroutine (task () :awakes {
                catch :e1 {
                    coroutine (task () :awakes {
                        yield(nil)
                        throw(:e1)
                    })()
                    while true {
                        yield(nil)
                    }
                }
                println(:e1)
                yield(nil)
                throw(:e2)
            })
            catch :e2 {
                resume co()
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
            set T = task () :awakes {
                catch err==:e1 {
                    spawn task () :awakes {
                        yield(nil)
                        throw(:e1)
                        println(:no)
                    } ()
                    while true { yield(nil) }
                }
                println(:ok1)
                throw(:e2)
                println(:no)
            }
            spawn (task () :awakes {
                catch :e2 {
                    spawn T()
                    while true { yield(nil) }
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
            spawn task () :awakes {
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
            var tk
            set tk = task (v) :awakes {
                println(v, evt)
                set v = yield(nil)
                println(v, evt)
            }
            var co
            set co = coroutine(tk)
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
            set tk = task (v) :awakes {
                println(v)
                println(evt)
                set v = yield(nil)
                println(v)
                println(evt)
            }
            var co1
            set co1 = coroutine(tk)
            var co2
            set co2 = coroutine(tk)
            broadcast in :global, 1
            broadcast in :global, 2
            broadcast in :global, 3
        """)
        //assert(out == "nil\n1\nnil\n1\nnil\n2\nnil\n2\n") { out }
        assert(out.contains("nil\n1\nnil\n1\nnil\n2\nnil\ncoro: 0x")) { out }
    }
    @Test
    fun ee_bcast2() {
        val out = all("""
            var co1
            set co1 = coroutine (task () :awakes {
                var co2
                set co2 = coroutine (task () :awakes {
                    yield(nil)  ;; awakes from outer bcast
                    println(2)
                })
                resume co2 ()
                yield(nil)      ;; awakes from co2 termination
                println(1)
            })
            resume co1 ()
            broadcast in :global, nil
        """)
        assert(out == "2\n1\n") { out }
    }
    @Test
    fun ee_bcast3() {
        val out = all("""
            var co1
            set co1 = coroutine(task () :awakes {
                var co2
                set co2 = coroutine(task () :awakes {
                    ${yield()}
                    throw(:error)
                })
                resume co2 ()
                ${yield()}
                println(1)
            })
            resume co1 ()
             broadcast in :global, nil
        """)
        assert(out == "anon : (lin 14, col 14) : broadcast in :global, nil\n" +
                "anon : (lin 7, col 21) : throw error : uncaught exception\n:error\n") { out }
    }
    @Test
    fun ee_bcast4() {
        val out = all("""
            var tk
            set tk = task () :awakes {
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
            var co
            set co = coroutine(tk)
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
            set tk = task (v) :awakes {
                println(v)
                yield(nil)
                println(evt)
            }
            var co
            set co = coroutine(tk)
            resume co(1)
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
            set tk = task () :awakes {
                yield(nil)
                println(evt)                
            }
            var co1
            set co1 = coroutine(tk)
            var co2
            set co2 = coroutine(tk)
            do {
                 broadcast in :global, 1
                 broadcast in :global, 2
                 broadcast in :global, 3
            }
        """)
        //assert(out == "2\n2\n") { out }
        assert(out.contains("2\ncoro: 0x")) { out }
    }
    @Test
    fun ee_bcast8() {
        val out = all("""
            var tk
            set tk = task (v) :awakes {
                do { var ok; set ok=true; while ok { yield(nil;) if type(evt)/=:coro { set ok=false } else { nil } } }
                println(evt)                
            }
            var co1
            set co1 = coroutine(tk)
            var co2
            set co2 = coroutine(tk)
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
            var tk
            set tk = task (v) :awakes {
                set v = yield(nil)
                throw(:1                )
            }
            var co1
            set co1 = coroutine(tk)
            var co2
            set co2 = coroutine(tk)
            catch err==:1 {
                func () {
                    println(1)
                     broadcast in :global, 1
                    println(2)
                     broadcast in :global, 2
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
            set tk = task (v) :awakes {
                println(v)
                do { var ok; set ok=true; while ok { yield(nil;) if type(evt)/=:coro { set ok=false } else { nil } } }
                ;;yield(nil)
                println(evt)                
                do { var ok2; set ok2=true; while ok2 { yield(nil;) if type(evt)/=:coro { set ok2=false } else { nil } } }
                ;;yield(nil)
                println(evt)                
            }
            var co1
            set co1 = coroutine(tk)
            var co2
            set co2 = coroutine(tk)
            catch err==:1 {
                func () {
                    println(1)
                    resume co1(10)
                    resume co2(10)
                    println(2)
                    broadcast in :global, [20]
                    println(3)
                    broadcast in :global, @[(30,30)]
                }()
            }
        """
        )
        assert(out == "1\n10\n10\n2\n[20]\n[20]\n3\n@[(30,30)]\n@[(30,30)]\n") { out }
    }
    @Test
    fun ee_bcast11_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) :awakes {
                do {
                    set v = evt
                }
            }
            var co
            set co = coroutine(tk)
            broadcast in :global, []
        """
        )
        assert(out == "anon : (lin 10, col 13) : broadcast in :global, []\n" +
                "anon : (lin 5, col 29) : invalid evt : cannot expose dynamic \"evt\"\n:error\n") { out }
    }

    // BCAST / SCOPE

    @Test
    fun ee_bcast_in1() {
        val out = ceu.all(
            """
            var T
            set T = task (v) :awakes {
                do { var ok; set ok=true; while ok { yield(nil;) if type(evt)/=:coro { set ok=false } else { nil } } }
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
            set T = task (v) :awakes {
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
            set T = task (v) :awakes {
                spawn (task () :awakes {
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
            set T = task (v) :awakes {
                spawn (task () :fake :awakes {
                    yield(nil)
                    println(v, evt)
                }) ()
                spawn (task () :fake :awakes {
                    do {
                        broadcast in :task, :ok
                    }
                }) ()
                yield(nil)
            }
            spawn (task () :awakes {
                yield(nil)
                println(999)
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
            spawn (task () :awakes {
                spawn (task () :awakes {
                    yield(nil)
                    broadcast in :global, nil
                }) ()
                yield(nil)
            }) ()
            broadcast in :global, nil
            println(1)
        """)
        assert(out == "1\n") { out }
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
            spawn in coroutines(), T(x)
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
            spawn in coroutines(), (task(){nil})(x)
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
            spawn in coroutines(), T(x)
            println(0)
        """
        )
        assert(out == "0\n") { out }
    }
    @Test
    fun ff_pool1() {
        val out = all("""
            var ts
            set ts = coroutines()
            println(type(ts))
            var T
            set T = task (v) :awakes {
                println(v)
                yield(nil)
                println(evt)
            }
            do {
                spawn in ts, T(1)
            }
             broadcast in :global, 2
        """)
        assert(out == ":coros\n1\n2\n") { out }
    }
    @Test
    fun ff_pool2_leak() {
        val out = all("""
            var T
            set T = task () {
                [1,2,3]
                yield(nil)
            }
            var ts
            set ts = coroutines()
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
            set ts = coroutines()
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
                set ts = coroutines()
                var T
                set T = task (v) {
                    println(v)
                    set v = yield(nil)
                    println(v)
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
                set ts = coroutines()
                var T
                set T = task (v) {
                    println(v)
                }
                spawn in ts, T(1)
                while in :coros ts, t {
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
            set ts = coroutines()
            spawn in ts, T()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ff_pool8_err() {
        val out = all("""
            while in :coros nil, x {
                nil
            }
        """)
        assert(out == "anon : (lin 2, col 29) : while error : expected coroutines\n:error\n") { out }
    }
    @Test
    fun ff_pool9_term() {
        val out = all("""
            var T
            set T = task () {
                yield(nil)
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while in :coros ts, xxx {
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
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while in :coros ts, xxx {
                println(1)
                 broadcast in :global, 1
                while in :coros ts, yyy {
                    println(2)
                }
            }
            println(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun ff_pool11_err_scope() {
        val out = all("""
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var yyy
            while in :coros ts, xxx {
                set yyy = xxx
            }
            println(yyy.status)
        """)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun ff_pool11a_err_scope() {
        val out = all("""
            var T
            set T = task () :awakes { yield(nil) }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var yyy
            while in :coros ts, xxx {
                set yyy = xxx
            }
            broadcast in :global, nil
            println(yyy.status)
        """)
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n:error\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun ff_pool12_err_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield(nil) }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while in :coros ts, xxx {
                var yyy
                while in :coros ts, zzz {
                    set yyy = zzz
                    println(yyy.status)
                }
                println(yyy.status)
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
            set ts = coroutines()
            spawn in ts, T()
            while in :coros ts, xxx {
                var yyy
                while in :coros ts, zzz {
                    nil
                }
                set yyy = xxx
                nil ;; otherwise scope err for yyy/xxx
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
            set ts = coroutines()
            spawn in ts, T()
            while in :coros ts, xxx {
                xxx
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
            coroutines(0)
        """
        )
        assert(out == "anon : (lin 2, col 24) : coroutines error : expected positive number\n:error\n") { out }
    }
    @Test
    fun ff_pool15_max_err() {
        val out = ceu.all(
            """
            coroutines(nil)
        """
        )
        assert(out == "anon : (lin 2, col 24) : coroutines error : expected positive number\n:error\n") { out }
    }
    @Test
    fun ff_pool16_max() {
        val out = ceu.all(
            """
            var ts = coroutines(1)
            var T = task () :awakes { yield(nil) }
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
            set ts = coroutines(2)
            var T
            set T = task (v) :awakes {
                println(10)
                defer {
                    println(20)
                    println(30)
                }
                do { var ok1; set ok1=true; while ok1 { yield(nil;) if type(evt)/=:coro { set ok1=false } else { nil } } }
                ;;yield(nil)
                if v {
                    do { var ok; set ok=true; while ok { yield(nil;) if type(evt)/=:coro { set ok=false } else { nil } } }
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
                spawn task () :awakes {
                    println(3)
                    ${yield()}
                    println(6)
                    throw(:ok)
                } ()
                spawn task () :awakes {
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
            set T = task (v) :awakes {
                spawn task () :awakes {
                    println(v)
                    yield(nil)
                    println(v)
                } ()
                while true { yield(nil) }
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
            set ts = coroutines(2)
            var T
            set T = task (v) :awakes {
                defer {
                    println(v)
                }
                catch err==:ok {
                    spawn task () :awakes {
                        yield(nil)
                        if v == 1 {
                            throw(:ok)
                        } else {
                            nil
                        }
                        while true { yield(nil) }
                    } ()
                    while true { yield(nil) }
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
            set ts = coroutines(2)
            var T
            set T = task (v) :awakes {
                defer {
                    println(v)
                }
                catch err==:ok {
                    spawn task () :awakes {
                        yield(nil)
                        if v == 2 {
                            throw(:ok)
                        } else {
                            nil
                        }
                        while true { yield(nil) }
                    } ()
                    while true { yield(nil) }
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
            set ts = coroutines()
            println(type(ts))
            var T
            set T = task (v) {
                set pub = v
                set v = yield(nil)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            
            while in :coros ts, t1 {
                while in :coros ts, t2 {
                    println(t1.pub, t2.pub)
                }
            }
             broadcast in :global, 2
        """)
        assert(out == ":coros\n1\t1\n1\t2\n2\t1\n2\t2\n") { out }
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
                    while true { yield(nil) }
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
            set T = task () :awakes {
                yield(nil)
                throw(nil)
            }
            spawn T()
            spawn T()
            broadcast in :global, @[]
        """
        )
        assert(out == "anon : (lin 9, col 13) : broadcast in :global, @[]\n" +
                "anon : (lin 5, col 17) : throw error : uncaught exception\nnil\n") { out }
    }
    @Test
    fun todo_pool25_valgrind() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(1)
            var T
            set T = task () :awakes { yield(nil) }
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

    // EVT

    @Test
    fun gg_evt_hld1_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) :awakes {
                set xxx = evt
            }
            var co
            set co = coroutine(tk)
            broadcast in :global, []
        """
        )
        assert(out == "anon : (lin 8, col 13) : broadcast in :global, []\n" +
                "anon : (lin 4, col 27) : invalid evt : cannot expose dynamic \"evt\"\n:error\n") { out }
    }
    @Test
    fun gg_evt_hld1_1_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) :awakes {
                set xxx = evt[0]
            }
            var co
            set co = coroutine(tk)
            broadcast in :global, [[]]
        """
        )
        assert(out == "anon : (lin 8, col 13) : broadcast in :global, [[]]\n" +
                "anon : (lin 4, col 31) : invalid index : cannot expose dynamic \"evt\" field\n:error\n") { out }
    }
    @Test
    fun gg_evt_hld2_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) :awakes {
                yield(nil)
                set xxx = evt
            }
            var co
            set co = coroutine(tk)
             broadcast in :global, 1
             broadcast in :global, []
        """
        )
        assert(out == "anon : (lin 10, col 14) : broadcast in :global, []\n" +
                "anon : (lin 5, col 27) : invalid evt : cannot expose dynamic \"evt\"\n:error\n") { out }
    }
    @Test
    fun gg_evt_hld3() {
        val out = ceu.all(
        """
            var fff
            set fff = func (x) { x }
            spawn task () :awakes {
                yield(nil)
                while evt[:type]/=:x {
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
            spawn task () :awakes {
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
            spawn task () :awakes {
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
            spawn task () :awakes {
                while (true) {
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
            spawn task () :awakes {
                while (true) {
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
    }
    @Test
    fun todo_gg_evt_hld8_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) :awakes {
                set xxx = evt[0]
            }
            var co
            set co = coroutine(tk)
            broadcast in :global, #[[]]
        """
        )
        assert(out == "anon : (lin 8, col 13) : broadcast in :global, [[]]\n" +
                "anon : (lin 4, col 31) : invalid index : cannot expose dynamic \"evt\" field\n") { out }
    }
    @Test
    fun todo_gg_evt_hld9_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) :awakes {
                set xxx = evt[0]
            }
            var co
            set co = coroutine(tk)
            broadcast in :global, @[(1,[])]
        """
        )
        assert(out == "anon : (lin 8, col 13) : broadcast in :global, [[]]\n" +
                "anon : (lin 4, col 31) : invalid index : cannot expose dynamic \"evt\" field\n") { out }
    }

    // PUB

    @Test
    fun hh_pub1_err() {
        val out = all("""
            var a
            a.pub
        """, true)
        assert(out == "anon : (lin 3, col 15) : pub error : expected coroutine\n:error\n") { out }
    }
    @Test
    fun hh_pub2_err() {
        val out = all("""
            pub
        """, true)
        assert(out == "anon : (lin 2, col 13) : pub error : expected enclosing task") { out }
    }
    @Test
    fun hh_pub3() {
        val out = all("""
            var t
            set t = task (v1) {
                set pub = v1
                var v2
                set v2 = yield(nil)
                set pub = pub + v2
                pub
            }
            var a
            set a = coroutine(t)
            println(a.pub)
            resume a(1)
            println(a.pub)
            resume a(2)
            println(a.pub)
        """, true)
        assert(out == "nil\n1\n3\n") { out }
    }
    @Test
    fun hh_pub4_err() {
        val out = all("""
            var t
            set t = task () {
                set pub = []
            }
            var x
            do {
                var a
                set a = coroutine(t)
                resume a()
                set x = a.pub
            }
            println(x)
        """)
        //assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "anon : (lin 10, col 24) : a()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub5() {
        val out = all("""
            var t
            set t = task () {
                set pub = 10
            }
            var a
            set a = coroutine(t)
            resume a()
            println(a.pub + a.pub)
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun hh_pub56_pool() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]  ;; valgrind test
            }
            spawn T()
            println(1)
        """)
        assert(out == "anon : (lin 6, col 19) : T()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub562_pool() {
        val out = all("""
            var T
            set T = task () {
                pub ;; useless test
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
                set pub = [10]
                yield(nil)
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var x
            while in :coros ts, t {
                println(t.pub[0])
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_pub7_pool_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil)
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var x
            while in :coros ts, t {
                set x = t.pub   ;; TODO: incompatible scope
            }
            println(999)
        """)
        //assert(out == "20\n") { out }
        assert(out == "anon : (lin 12, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub8_fake_task() {
        val out = all("""
            spawn (task () {
                set pub = 1
                spawn (task () :fake {
                    println(pub)
                }) ()
                nil
            }) ()
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_pub9_fake_task_err() {
        val out = all("""
            spawn (task () :awakes {
                set pub = []
                var x
                spawn (task () :fake :awakes {
                    set x = pub
                }) ()
                println(x)
            }) ()
        """)
        assert(out == "anon : (lin 2, col 20) : task () :awakes { set pub = [] var x spawn ta...)\n" +
                "anon : (lin 5, col 24) : task () :fake :awakes { set x = pub }()\n" +
                "anon : (lin 6, col 29) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub9_10_fake_task() {
        val out = all("""
            spawn (task () {
                set pub = [10]
                var x
                spawn (task () :fake {
                    set x = pub[0]
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
                pub
            }) ()
        """, true)
        assert(out == "anon : (lin 3, col 17) : pub error : expected enclosing task") { out }
    }
    @Test
    fun hh_pub11_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil)
            }
            var y
            do {
                var t
                set t = coroutine(T)
                resume t ()
                var x
                set x = t.pub  ;; pub expose
                set y = t.pub  ;; incompatible scopes
            }
            println(999)
        """)
        //assert(out == "anon : (lin 14, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 13, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub12_index_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil)
            }
            var t
            set t = coroutine(T)
            resume t()
            var x
            set x = t.pub   ;; no expose
        """)
        assert(out == "anon : (lin 11, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub13_index_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil)
            }
            var t
            set t = coroutine(T)
            resume t()
            println(t.pub)   ;; no expose
        """)
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun todo_pub14_index_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [[@[(:x,10)]]]
                yield(nil)
            }
            var t
            set t = coroutine(T)
            resume t()
            println(t.pub[0][:x])   ;; no expose
        """)
        assert(out == "anon : (lin 10, col 27) : invalid index : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub15_task_err() {
        val out = all("""
        spawn task () :fake :awakes { 
            var y
            set y = do {     
                var ceu_spw_54     
                set ceu_spw_54 = spawn task () :fake :awakes {         
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
        assert(out == "anon : (lin 16, col 9) : broadcast in :global, nil\n" +
                "anon : (lin 12, col 28) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub16() {
        val out = all("""
            var t
            set t = task (v) {
                set pub = v
                yield(nil)
                var x
                set x = [2]
                set pub = x
                var y
                set y = yield(nil)
                set pub = @[(:y,y)]
                move(pub)
            }
            var a
            set a = coroutine(t)
            resume a([1])
            println(a.pub)
            resume a()
            println(a.pub)
            resume a([3])
            println(a.pub)
        """, true)
        assert(out == "[1]\n[2]\n@[(:y,[3])]\n") { out }
    }
    @Test
    fun hh_pub17_err_expose() {
        val out = all("""
            var t
            set t = task () {
                set pub = []
            }
            var a
            set a = coroutine(t)
            resume a()
            var x
            set x = a.pub
            println(x)
        """)
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "anon : (lin 8, col 20) : a()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub18_err_expose() {
        val out = all("""
            var t
            set t = task () {
                set pub = @[]
            }
            var a
            set a = coroutine(t)
            resume a()
            var x
            set x = a.pub
            println(x)
        """)
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "anon : (lin 8, col 20) : a()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub19_err_expose() {
        val out = all("""
            var t
            set t = task () {
                set pub = #[]
            }
            var a
            set a = coroutine(t)
            resume a()
            var x
            set x = a.pub
            println(x)
        """)
        assert(out == "anon : (lin 8, col 20) : a()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub20_func() {
        val out = all("""
            var t
            set t = task (v) {
                set pub = v
                var f
                set f = func () {
                    pub
                }
                println(f())
            }
            var a
            set a = coroutine(t)
            resume a(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_pub21_func_expose() {
        val out = all("""
            var t
            set t = task (v) {
                set pub = v
                var f
                set f = func () {
                    pub
                }
                println(f())
            }
            var a
            set a = coroutine(t)
            resume a([1])
        """, true)
        assert(out == "anon : (lin 13, col 20) : a([1])\n" +
                "anon : (lin 9, col 25) : f()\n" +
                "anon : (lin 7, col 21) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun hh_pub_22_nopub() {
        val out = all("""
            var U
            set U = task () {
                set pub = func () {
                    10
                }
            }
            var T
            set T = task (u) {
                println(u.pub())
            }
            spawn T (spawn U())
        """)
        assert(out == "anon : (lin 12, col 28) : U()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub_23_nopub() {
        val out = all("""
            var U
            set U = task () {
                set pub = [10]
            }
            var T
            set T = task (u) {
                nil ;;println(u.pub.0)
            }
            spawn T (spawn U())
        """)
        assert(out == "anon : (lin 10, col 28) : U()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub_24_nopub() {
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
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun hh_pub25() {
        val out = all("""
            var t = task (v) {
                set pub = @[]
                nil
            }
            var a
            set a = coroutine(t)
            resume a()
            println(a.status)
        """, true)
        assert(out == ":terminated\n") { out }
    }

    // STATUS

    @Test
    fun ii_status1_err() {
        val out = all("""
            var a
            a.status
        """, true)
        assert(out == "anon : (lin 3, col 15) : status error : expected coroutine\n:error\n") { out }
    }
    @Test
    fun ii_status2_err() {
        val out = all("""
            status
        """, true)
        assert(out == "anon : (lin 2, col 13) : status error : expected enclosing task") { out }
    }
    @Test
    fun ii_status3_err() {
        val out = all("""
            var t
            set t = task () {
                set status = nil     ;; error: cannot assign to status
            }
        """, true)
        assert(out == "anon : (lin 4, col 17) : invalid set : invalid destination") { out }
    }
    @Test
    fun ii_status4() {
        val out = all("""
            var t
            set t = task () {
                println(10, status)
                yield(nil)
                println(20, status)
            }
            var a
            set a = coroutine(t)
            println(1, a.status)
            resume a()
            println(2, a.status)
            resume a()
            println(3, a.status)
        """)
        //assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "1\t:yielded\n10\t:resumed\n2\t:yielded\n20\t:resumed\n3\t:terminated\n") { out }
    }
    @Test
    fun todo_ii_status5() {
        val out = all("""
            var T
            set T = task (x) :awakes {
                println(10, status)
                yield(nil)
                if x {
                    yield(nil)
                } else {
                    nil
                }
                println(20, status)
            }
            spawn task () :awakes {
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
        assert(out.contains("Assertion `ceu_coro->Bcast.status==CEU_CORO_STATUS_YIELDED || (ceu_coro->Bcast.status==CEU_CORO_STATUS_TOGGLED && ceu_evt==&CEU_EVT_CLEAR)' failed")) { out }
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
            set T = task () :awakes {
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
    fun jj_toggle4_coros() {
        val out = all("""
            var T
            set T = task () :awakes {
                yield(nil)
                println(10)
            }
            var ts
            set ts = coroutines()
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
    fun jj_toggle6_defer_coros() {
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
            set ts = coroutines()
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
                coroutine(task() {nil})
            }
            println(f())
        """, true)
        assert(out == "anon : (lin 6, col 21) : f()\n" +
                "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun kk_esc2() {
        val out = all("""
            var T
            set T = task () { nil }
            var xxx
            do {
                var t
                set t = coroutine(T)
                set xxx = t ;; error
            }
        """)
        assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n:error\n") { out }
    }

    // TRACK

    @Test
    fun ll_track1_err() {
        val out = all("""
            track(nil)
        """)
        assert(out == "anon : (lin 2, col 19) : track error : expected coroutine\n:error\n") { out }
    }
    @Test
    fun ll_track2() {
        val out = all("""
            var T
            set T = task () { nil }
            var t
            set t = coroutine(T)
            var x
            set x = track(t)
            println(t, x)
        """)
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun ll_track3_err() {
        val out = all("""
            var T
            set T = task () { nil }
            var t
            set t = coroutine(T)
            resume t()
            var x
            set x = track(t) ;; error: dead coro
            ;;println(t.status)
            println(x)
        """)
        assert(out == "anon : (lin 8, col 27) : track error : expected unterminated coroutine\n:error\n") { out }
    }
    @Test
    fun ll_track4() {
        val out = all("""
            var T
            set T = task () {
                set pub = 10
                yield(nil)
            }
            var t
            set t = coroutine(T)
            var x
            set x = track(t)
            println(x.pub) 
            resume t()
            println(x.pub) 
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
        assert(out == "anon : (lin 4, col 23) : pub error : expected coroutine\n:error\n") { out }
    }
    @Test
    fun ll_track5_err2() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil)
            }
            var t
            set t = coroutine(T)
            resume t()
            var x
            set x = track(t)
            println(x.pub)      ;; expose (ok, global func)
        """)
        //assert(out == "anon : (lin 12, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun ll_track6_err() {
        val out = all("""
            var T
            set T = task () { nil }
            var x
            do {
                var t
                set t = coroutine(T)
                set x = track(t)         ;; error scope
            }
            println(x.status)
            println(x)
        """)
        //assert(out.contains("terminated\ntrack: 0x")) { out }
        assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun ll_track7() {
        val out = all("""
            var T
            set T = task () :awakes {
                set pub = [10]
                yield(nil)
            }
            var t
            set t = coroutine(T)
            resume t ()
            var x
            set x = track(t)
            println(x.pub[0])
            broadcast in :global, nil
            println(x.status)
        """)
        assert(out == "10\nnil\n") { out }
    }
    @Test
    fun ll_track8_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield(nil)
            }
            var x
            do {
                var t
                set t = coroutine(T)
                resume t ()
                set x = track(t)         ;; scope x < t
                println(x.pub[0])
            }
            println(x.status)
            println(x)
        """)
        //assert(out.contains("10\n:terminated\ntrack: 0x")) { out }
        assert(out == "anon : (lin 12, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun ll_track09_err() {
        val out = all("""
            var T = task (v) :awakes {
                yield(nil)
            }
            var x
            var ts = coroutines()
            spawn in ts, T(1)
            while in :coros ts, t {
                set x = track(t)
            }
        """)
        assert(out == "anon : (lin 9, col 31) : track error : expected coroutine\n" +
                ":error\n") { out }
    }
    @Test
    fun ll_track09() {
        val out = all("""
            var T = task (v) :awakes {
                set pub = [v]
                yield(nil)
            }
            var x
            var ts = coroutines()
            spawn in ts, T(1)
            spawn in ts, T(2)
            while in :coros ts, t {
                set x = t
            }
            println(x.pub[0])   ;; 2
            broadcast in :global, nil
            println(x.status)   ;; nil
        """)
        assert(out == "2\nnil\n") { out }
    }
    @Test
    fun ll_track10() {
        val out = all("""
            var T
            set T = task (v) :awakes {
                set pub = [v]
                yield(nil)
            }
            var x
            var ts
            set ts = coroutines()
            do {
                spawn in ts, T(1)
                spawn in ts, T(2)
                while in :coros ts, t {
                    set x = t    ;; track(t) up_hold in
                }
                println(x.pub[0])   ;; 2
                broadcast in :global, nil
                println(x.status)   ;; nil
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
                set pub = [v]
                yield(nil)
            }
            var x
            do {
                var ts
                set ts = coroutines()
                spawn in ts, T(1)
                while in :coros ts, t {
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
            set T = task (v) :awakes {
                yield(nil)
            }
            var ts
            set ts = coroutines()
            spawn in ts, T(1)
            var x
            set x = catch true {
                while in :coros ts, t {
                    throw(t)
                }
            }
            broadcast in :global, nil
            println(x.status)   ;; nil
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun ll_track13_throw() {
        val out = all("""
            var T
            set T = task (v) :awakes {
                set pub = [v]
                yield(nil)
            }
            var ts
            set ts = coroutines()
            spawn in ts, T(1)
            spawn in ts, T(2)
            var x
            set x = catch true {
                while in :coros ts, t {
                    throw(t)
                }
            }
            println(x.pub[0])   ;; 1
            println(x.status)   ;; :yielded
            broadcast in :global, nil
            println(x.status)   ;; nil
        """)
        assert(out == "1\n:yielded\nnil\n") { out }
    }
    @Test
    fun ll_track14() {
        val out = all("""
            var T
            set T = task () :awakes {
                set pub = [10]
                ${await("evt==:evt")}
            }
            var t
            set t = spawn T()
            var x
            set x = track(t)
            spawn task () :awakes {
                catch :paror {
                    spawn task () :awakes {
                        ${await("if x.status==:terminated { true } else { if x.status==nil { true } else { false } }")}
                        throw(:paror)
                    } ()
                    println(x.pub[0])
                    broadcast in :global, nil
                    println(x.pub[0])
                    broadcast in :global, :evt
                    println(x.pub[0]) ;; never printed
                    ${await("false")}
                }
                println(x.status)
            }()
            println(:ok)
        """)
        assert(out == "10\n10\nnil\n:ok\n") { out }
    }
    @Test
    fun ll_track15_simplify() {
        val out = all("""
            var T = task (v) :awakes {
                yield(nil)
            }
            var ts = coroutines()
            spawn in ts, T(1)
            spawn in ts, T(2)
            var x
            while in :coros ts, t {
                set x = (t)
            }
            broadcast in :global, nil
            println(x.status)   ;; nil
        """)
        assert(out == "nil\n") { out }
    }

    // XCEU

    @Test
    fun mm_xceu1() {
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
    fun mm_xceu2() {
        val out = all("""
            spawn task () :awakes {
                yield(nil)
                println(evt)
                spawn (task () :awakes {
                    while (true) {
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
    fun mm_xceu3() {
        val out = all("""
            spawn task () :awakes {
                yield(nil)
                println(evt)
                spawn (task () :fake :awakes {
                    while (true) {
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
    fun mm_xceu4() {
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
    fun mm_xceu5() {
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
    fun mm_xceu6() {
        val out = all("""
            var T
            set T = task (pos) :awakes {
                yield(nil)
                println(pos)
            }
            spawn (task () :awakes {
                var ts
                set ts = coroutines()
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
    fun mm_xceu7() {
        val out = all("""
            spawn task () :awakes {
                do {
                    spawn task () :awakes {
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
    fun mm_xceu8() {
        val out = all("""
            spawn task () :awakes {
                do {
                    spawn task () :awakes {
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
    fun mm_xceu9 () {
        val out = all("""
            spawn task () :awakes {
                do {
                    spawn task () :awakes {
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
    fun mm_xceu10 () {
        val out = all("""
            ;;println(:blk0, `:pointer ceu_block`)
            spawn task () :awakes {
                ;;println(:cor1, `:pointer ceu_coro`)
                ;;println(:blk11, `:pointer ceu_block`)
                while true {
                    ;;println(:blk12, `:pointer ceu_block`)
                    yield(nil); while evt/=10 { yield(nil) }
                    println(:1)
                    var t = spawn task () :awakes {
                        ;;println(:cor2, `:pointer ceu_coro`)
                        ;;println(:blk2, `:pointer ceu_block`)
                        yield(nil); while evt/=10 { yield(nil) }
                    } ()
                    while t.status/=:terminated { yield(nil) }
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
}
