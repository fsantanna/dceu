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
    @Test
    fun aa_05_yield_err() {
        val out = test("""
            yield(nil) { as it => nil }
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing coro or task\n") { out }
    }

    // SPAWN

    @Test
    fun bb_01_spawn() {
        val out = test("""
            val T = task (v) { nil }
            val t = spawn T()
            println(t)
        """)
        assert(out.contains("exe-task: 0x")) { out }
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
        assert(out == " v  anon : (lin 2, col 13) : spawn error : expected task\n") { out }
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
        //assert(out == ":ok\n") { out }
        assert(out == "anon : (lin 6, col 17) : spawn error : unexpected enclosing func\n") { out }
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
            println(broadcast(1))
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun dd_02_bcast() {
        val out = test(
            """
            spawn task () {
                println(1)
                yield(nil) { as it => nil }
                println(2)
            }()
            broadcast(nil)
        """
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun dd_03_bcast() {
        val out = test("""
            var tk = task (v) {
                val e1 = yield(nil) { as it => it }
                println(:1, e1)
                val e2 = yield(nil) { as it => it }
                println(:2, e2)
            }
            spawn tk ()
            broadcast(1)
            broadcast(2)
            broadcast(3)
        """)
        assert(out == ":1\t1\n:2\t2\n") { out }
    }
    @Test
    fun dd_04_bcast() {
        val out = test("""
            var tk
            set tk = task (v) {
                val e1 = yield(nil) { as it => it }
                println(v,e1)
                var e2 = yield(nil) { as it => it }
                println(v,e2)
            }
            var co1 = spawn tk(:1)
            var co2 = spawn tk(:2)
            broadcast(1)
            broadcast(2)
            broadcast(3)
        """)
        //assert(out == "nil\n1\nnil\n1\nnil\n2\nnil\n2\n") { out }
        assert(out.contains(":1\t1\n:2\t1\n:1\t2\n:2\texe-task: 0x")) { out }
    }
    @Test
    fun dd_05_bcast() {
        val out = test("""
            var co1 = spawn (task () {
                var co2 = spawn (task () {
                    yield(nil) { as it => nil }  ;; awakes from outer bcast
                    println(2)
                }) ()
                yield(nil) { as it => nil }      ;; awakes from co2 termination
                println(1)
            }) ()
            broadcast(nil)
        """)
        assert(out == "2\n1\n") { out }
    }
    @Test
    fun dd_06_bcast() {
        val out = test("""
            var tk
            set tk = task (v) {
                println(v)
                val e = yield(nil) { as it => it }
                println(e)
            }
            var co = spawn(tk)(1)
            broadcast(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun dd_07_bcast() {
        val out = test("""
            func () {
                 broadcast(1)
            }
            println(1)
        """)
        //assert(out == "1\n") { out }
        assert(out == "anon : (lin 3, col 18) : broadcast error : unexpected enclosing func\n") { out }
    }
    @Test
    fun dd_08_bcast() {
        val out = test("""
            val T = task () {
                yield(nil) { as it => nil }
                println(:ok)
            }
            var t = spawn T()
            do {
                broadcast(1)
            }
        """)
        assert(out.contains(":ok\n")) { out }
    }
    @Test
    fun dd_09_bcast() {
        val out = test("""
            var tk
            set tk = task () {
                yield(nil) { as it => nil }
                val e = yield(nil) { as it => it }
                println(e)                
            }
            var co1 = spawn (tk) ()
            var co2 = spawn tk ()
            do {
                 broadcast(1)
                 broadcast(2)
                 broadcast(3)
            }
        """)
        //assert(out == "2\n2\n") { out }
        assert(out.contains("2\nexe-task: 0x")) { out }
    }
    @Test
    fun dd_10_bcast() {
        val out = test("""
            var tk
            set tk = task () {
                val e1 = yield(nil) { as it => it }
                var e2
                do {
                    println(e1)
                    set e2 = yield(nil) { as it => it }
                    println(e2)
                }
                do {
                    println(e2)
                    val e3 = yield(nil) { as it => it }
                    println(e3)
                }
            }
            spawn tk ()
            broadcast(1)
            broadcast(2)
            broadcast(3)
            broadcast(4)
        """)
        assert(out == "1\n2\n2\n3\n") { out }
    }
    @Test
    fun dd_11_bcast_await() {
        val out = test("""
            val T = task (v) {
                println(:1)
                val e = ${AWAIT()}
                println(:2, e)                
            }
            spawn T()
            broadcast(10)
        """)
        assert(out == ":1\n:2\t10\n") { out }
    }
    @Test
    fun dd_12_bcast() {
        val out = test("""
            var tk
            set tk = task (v) {
                yield(nil) { as it => nil }
                val e = ${AWAIT()}
                println(e)                
            }
            var co1 = spawn tk ()
            var co2 = spawn tk ()
            ;;func () {
                 broadcast(1)
                 broadcast(2)
                 broadcast(3)
            ;;}()
        """)
        assert(out == "2\n2\n") { out }
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
        val out = test("""
            val T = task () {
                yield(nil) { as it =>
                    println(tags(it,:X))
                }
            }
            spawn T()
            broadcast (tags([],:X,true))
        """)
        assert(out == "true\n") { out }
    }

    // THROW / CATCH

    @Test
    fun ee_01_throw() {
        val out = test("""
            catch {as it=>:xxx}in{
                spawn task () {
                    yield(nil) { as it => nil }
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
    fun ee_02_throw() {
        val out = test("""
            spawn task () {
                throw(:err)
            }()
        """)
        assert(out == " |  anon : (lin 2, col 19) : (task () { throw(:err) })()\n" +
                " |  anon : (lin 3, col 17) : throw(:err)\n" +
                " v  throw error : :err\n") { out }
    }
    @Test
    fun ee_03_throw() {
        val out = test("""
            spawn task () {
                yield(nil) { as it => nil }
                throw(:err)
            }()
            broadcast(nil)
        """)
        assert(out == " |  anon : (lin 6, col 13) : broadcast(nil)\n" +
                " |  anon : (lin 4, col 17) : throw(:err)\n" +
                " v  throw error : :err\n") { out }
    }
    @Test
    fun ee_04_throw() {
        val out = test("""
            spawn task () {
                yield(nil) { as it => nil }
                throw(:err)
            }()
            broadcast(nil)
        """)
        assert(out == " |  anon : (lin 6, col 13) : broadcast(nil)\n" +
                " |  anon : (lin 4, col 17) : throw(:err)\n" +
                " v  throw error : :err\n") { out }
    }
    @Test
    fun ee_05_throw() {
        val out = test("""
            spawn task () {
                spawn task () {
                    yield(nil) { as it => nil }
                    throw(:err)
                }()
                yield(nil) { as it => nil }
            }()
            broadcast(nil)
        """)
        assert(out == " |  anon : (lin 9, col 13) : broadcast(nil)\n" +
                " |  anon : (lin 5, col 21) : throw(:err)\n" +
                " v  throw error : :err\n") { out }
    }
    @Test
    fun ee_06_throw() {
        val out = test("""
            spawn task () {
                yield(nil) { as it => nil }
                throw(:err)
            }()
            spawn task () {
                nil
            }()
        """)
        assert(out == " |  anon : (lin 6, col 19) : (task () { nil })()\n" +
                " |  anon : (lin 4, col 17) : throw(:err)\n" +
                " v  throw error : :err\n") { out }
    }
    @Test
    fun ee_07_throw() {
        val out = test("""
            spawn task () {
                yield(nil) { as it => nil }
                throw(:err)
            }()
            spawn task () {
                nil
            }()
        """)
        assert(out == " |  anon : (lin 6, col 19) : (task () { nil })()\n" +
                " |  anon : (lin 4, col 17) : throw(:err)\n" +
                " v  throw error : :err\n") { out }
    }
    @Test
    fun ee_08_throw() {
        val out = test("""
            spawn task () {
                yield(nil) { as it => nil }
                nil
            }()
            spawn task () {
                yield(nil) { as it => nil }
                yield(nil) { as it => nil }
                throw(:err)
            }()
            broadcast(nil)
        """)
        assert(out == " |  anon : (lin 11, col 13) : broadcast(nil)\n" +
                " |  anon : (lin 9, col 17) : throw(:err)\n" +
                " v  throw error : :err\n") { out }
    }
    @Test
    fun ee_09_bcast() {
        val out = test("""
            val T = task () {
                yield(nil) { as it => nil }
                throw(:err)
            }
            spawn T()
            spawn T()
            broadcast(nil)
        """)
        assert(out == " |  anon : (lin 8, col 13) : broadcast(nil)\n" +
                " |  anon : (lin 4, col 17) : throw(:err)\n" +
                " v  throw error : :err\n") { out }
    }
    @Test
    fun ee_10_bcast() {
        val out = test("""
            val T = task (v) {
                val e = yield(nil) { as it => it }
                println(e)                
            }
            spawn T(10)
            ;;catch {
                ;;func () {
                    broadcast ([])
                ;;}()
            ;;}
        """)
        //assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : (func () { broadcast [] })()\n" +
        //        " |  anon : (lin 9, col 21) : broadcast []\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold event reference\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : (func () { broadcast [] })()\n" +
        //        " |  anon : (lin 9, col 21) : broadcast []\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 9, col 21) : broadcast([])\n" +
                " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun ee_11_bcast() {
        val out = test("""
            val T = task (v) {
                val e = yield(nil) { as it => it }
                println(e)                
            }
            spawn T(10)
            catch { as it => (do { println(it) ; true }) } in {
                ;;func () {
                    broadcast ([])
                ;;}()
            }
        """)
        //assert(out == "[]\n") { out }
        //assert(out == "declaration error : cannot hold event reference\n") { out }
        assert(out == "block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun ee_12_bcast() {
        val out = test("""
            var co1 = spawn (task () {
                var co2 = spawn (task () {
                    ${AWAIT()}
                    throw(:error)
                })()
                ${AWAIT()}
                println(1)
            })()
            broadcast(nil)
        """)
        assert(out == " |  anon : (lin 10, col 13) : broadcast(nil)\n" +
                " |  anon : (lin 5, col 21) : throw(:error)\n" +
                " v  throw error : :error\n") { out }
    }
    @Test
    fun ee_14_throw() {     // catch from nesting and broadcast
        val out = test(
            """
            spawn task () {
                catch {as it=> it==:e1 }in {                     ;; catch 1st (yes catch)
                    spawn task () {
                        yield(nil) { as it => nil }
                        println(222)
                        throw(:e1)                  ;; throw
                    } ()
                    xloop { yield(nil) { as it => nil } }
                }
                println(333)
            } ()
            catch {as it => true} in {                            ;; catch 2nd (no catch)
                println(111)
                broadcast(nil)
                println(444)
            }
            println(:END)
        """
        )
        assert(out == "111\n" +
                "222\n" +
                "333\n" +
                "444\n" +
                ":END\n") { out }
    }
    @Test
    fun ee_15_throw() {
        val out = test(
            """
            spawn task () {
                catch {as it=>false} in {
                    yield(nil) { as it => nil }
                }
                println(999)
            } ()
            catch {as it=>true} in {
                throw(nil)
            }
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    // TASK TERMINATION

    @Test
    fun ff_00_term() {
        val out = test("""
            spawn task () {
                val e = yield(nil) { as it => it }
                println(:ok, e)
            }()
            spawn task () {
                nil
            }()
        """)
        assert(out.contains(" |  anon : (lin 6, col 19) : (task () { nil })()\n" +
                " v  anon : (lin 3, col 36) : block escape error : cannot move to deeper scope with pending references")) { out }
    }
    @Test
    fun ff_01_term() {
        val out = test("""
            spawn task () {
                yield(nil) { as it =>
                    println(:ok, it)
                }
            }()
            spawn task () {
                nil
            }()
        """)
        assert(out.contains(":ok\texe-task: 0x")) { out }
    }
    @Test
    fun ff_02_term() {
        val out = test("""
            spawn task () {
                yield(nil) { as it => nil }
                println(:1)
                yield(nil) { as it =>
                    println(:ok, it)
                }
            }()
            spawn task () {
                yield(nil) { as it => nil }
                println(:2)
            }()
            broadcast(nil)
        """)
        assert(out.contains(":1\n:2\n:ok\texe-task: 0x")) { out }
    }
    @Test
    fun ff_03_term() {
        val out = test("""
            spawn task () {
                val t = spawn task () {
                    yield(nil) { as it =>
                        println(:1, it)
                    }
                }()
                println(:0)
                yield(nil) { as it =>
                    if (type(it) == :exe-task) {
                        println(:2, it == t)
                    } else {
                        nil
                    }
                }
            } ()
            broadcast(:a)
            broadcast (:b)
        """)
        assert(out == ":0\n:1\t:a\n:2\ttrue\n") { out }
    }

    // SCOPE / BCAST

    @Test
    fun gg_01_scope() {
        val out = test("""
            val T = task (v) {
                val e = yield(nil) { as it => it }
                println(e)                
            }
            spawn T(10)
            ;;func () {
                broadcast ([])
            ;;}()
        """)
        //assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 7, col 13) : (func () { broadcast [] })()\n" +
        //        " |  anon : (lin 8, col 17) : broadcast []\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold event reference\n") { out }
        //assert(out == " |  anon : (lin 7, col 13) : (func () { broadcast [] })()\n" +
        //        " |  anon : (lin 8, col 17) : broadcast []\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 8, col 17) : broadcast([])\n" +
                " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun gg_02_scope() {
        val out = test("""
            val T = task (v) {
                yield(nil) { as it =>
                    println(it)
                }                                
            }
            spawn T(10)
            ;;func () {
                broadcast ([])
            ;;}()
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun gg_02_bcast() {
        val out = test("""
            val T = task () {
                val e = yield(nil) { as it => it }
                println(e)
                yield(nil) { as it => nil }
            }
            spawn T()
            spawn T()
            broadcast ([])
        """)
        //assert(out == "[]\n" +
        //        " |  anon : (lin 9, col 13) : broadcast []\n" +
        //        " v  anon : (lin 3, col 25) : resume error : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 9, col 13) : broadcast []\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold event reference\n") { out }
        assert(out == " |  anon : (lin 9, col 13) : broadcast([])\n" +
                " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun gg_03_bcast() {
        val out = test("""
            val T = task (v) {
                yield(nil) { as it => nil }
                println(v)                
            }
            spawn T([])
            broadcast(nil)
        """)
        //assert(out == ":1\n10\n10\n:2\ndeclaration error : incompatible scopes\n") { out }
        assert(out == "[]\n") { out }
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
            val e = catch {as it=>true} in {
                ;;func () {
                    println(:2)
                    broadcast ([20])
                    println(:3)
                    broadcast (@[(30,30)])
                ;;}()
            }
            println(e)
        """
        )
        //assert(out == ":1\n10\n10\n:2\n[20]\nresume error : incompatible scopes\n") { out }
        //assert(out == ":1\n" + "10\n" + "10\n" + ":2\n" +
        //        "declaration error : cannot hold event reference\n") { out }
        assert(out == ":1\n" + "10\n" + "10\n" + ":2\n" +
                "block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun gg_05_bcast_tuple_func_no() {
        val out = test("""
            var f = func (v) {
                val :tmp x = [0]
                set x[0] = v[0]
                println(x[0])
            }
            var T = task () {
                yield(nil) { as it=>f(it) }
            }
            spawn T()
            broadcast ([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun gg_05_bcast_tuple_func_xx() {
        val out = test("""
            var f = func (v) {
                println(v[0])
            }
            var T = task () {
                yield(nil) { as it => f(it) }
            }
            spawn T()
            broadcast ([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun gg_05_bcast_tuple_func_ok() {
        val out = test("""
            var f = func (v) {
                val :tmp x = [0]
                set x[0] = v[0]
                println(x[0])
            }
            var T = task () {
                yield(nil) { as it => f(it) }
            }
            spawn T()
            broadcast ([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun gg_06_bcast_tuple_func_ok() {
        val out = test("""
            val f = func (v) {
                println(v)
            }
            val T = task () {
                f(yield(nil) { as it => it })
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
        """)
        //assert(out == "[]\n") { out }
        assert(out == " |  anon : (lin 14, col 33) : broadcast([])\n" +
                " v  anon : (lin 6, col 30) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun gg_07_bcast_tuple_func_ok() {
        val out = test("""
            val f = func (v) {
                println(v)
            }
            val T = task () {
                do {
                    f(yield(nil){as it => it})
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
        """)
        //assert(out == "[]\n") { out }
        assert(out == " |  anon : (lin 14, col 25) : broadcast([])\n" +
                " v  anon : (lin 7, col 33) : block escape error : cannot copy reference out\n") { out }
    }

    // SCOPE / TUPLE / NEST

    @Test
    fun todo_gh_01_set() {
        val out = test("""
            spawn task () {
                var t = [1]
                spawn task () {
                    set t = [2]
                } ()
                println(t)
            } ()
        """)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun dh_02_set() {
        val out = test("""
            spawn task () {
                var t = [1]
                spawn task () {
                    yield(nil) { as it => nil }
                    set t = [2]
                } ()
                yield(nil) { as it => nil }
                println(t)
            } ()
            broadcast(nil)
        """)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun gh_03_set() {
        val out = test("""
            spawn task () {
                var t = [1]
                func () {
                    set t = [2]
                } ()
                println(t)
            } ()
        """)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun dh_04_set() {
        val out = test("""
            spawn task () {
                var t = [1]
                spawn task () {
                    yield(nil) { as it =>
                        set t = copy(it)
                    }
                } ()
                yield(nil) { as it => nil }
                println(t)
            } ()
            broadcast ([1])
        """, true)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun dh_05_set() {
        val out = test("""
            spawn task () {
                var t = [1]
                spawn task () {
                    set t = yield(nil) { as it =>
                        copy(it)
                    }
                } ()
                yield(nil) { as it => nil }
                println(t)
            } ()
            broadcast ([1])
        """, true)
        assert(out == "[1]\n") { out }
    }

    // STATUS

    @Test
    fun hh_01_status() {
        val out = test("""
            val t = spawn (task () {
                nil
            }) ()
            println(status(t))
        """)
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun hh_02_status() {
        val out = test("""
            val t = spawn (task () {
                yield(nil) { as it => nil }
            }) ()
            println(status(t))
        """)
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun hh_04_status() {
        val out = test("""
            val t = spawn (task () {
                yield(nil) { as it => nil }
            }) ()
            broadcast(nil)
            println(status(t))
        """)
        assert(out == ":terminated\n") { out }
    }

    // TASK / BCAST IN

    @Test
    fun jj_01_bcast_in_err() {
        val out = test("""
            broadcast (nil) in nil
        """)
        assert(out == " v  anon : (lin 2, col 13) : broadcast(nil,nil) error : expected task\n") { out }
    }
    @Test
    fun jj_02_bcast_in_task() {
        val out = test("""
            val T = task (v) {
                ${AWAIT()}
                ;;yield(nil)
                println(v)
            }
            val t1 = spawn T (1)
            val t2 = spawn T (2)
            broadcast (nil) in t1
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_03_bcast_in_self() {
        val out = test("""
            val T = task (v) {
                ${AWAIT()}
                println(v)
                spawn task () {
                    println(${AWAIT()})
                } ()
                broadcast(10)
            }
            val t1 = spawn T(1)
            val t2 = spawn T(2)
            do {
                broadcast (nil) in t1
            }
        """)
        assert(out == "1\n10\n") { out }
    }

    // DROP / MOVE / OUT

    @Test
    fun jj_01_bcast_move() {
        val out = test(
            """
            var T = task () {
                val evt = yield(nil) {as it => it}
                do {
                    var v = evt
                    println(v)
                }
            }
            var t = spawn T()
            ;;println(:1111)
            var e = []
            broadcast (drop(e))
            println(e)
            """
        )
        //assert(out == "[]\nnil\n") { out }
        //assert(out == "anon : (lin 10, col 13) : broadcast move(e)\n" +
        //        "anon : (lin 4, col 17) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == " |  anon : (lin 12, col 13) : broadcast(drop(e))\n" +
                " v  anon : (lin 3, col 38) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun todo_jj_02_task_nest() {
        val out = test("""
            spawn task (v1) {
                spawn task (v2) {
                    spawn task (v3) {
                        println(v1,v2,v3)
                        nil
                    }(3)
                }(2)
            }(1)
        """)
        assert(out == "1\t2\t3\n" +
                " |  anon : (lin 2, col 19) : (task (v1) { spawn (task (v2) { spawn (task (...)\n" +
                " |  anon : (lin 3, col 23) : (task (v2) { spawn (task (v3) { println(v1,v2...)\n" +
                " v  anon : (lin 3, col 33) : block escape error : cannot copy reference to outer scope\n") { out }
    }

    // PUB

    @Test
    fun kk_01_pub() {
        val out = test("""
            pub()
        """)
        assert(out == " v  anon : (lin 2, col 13) : pub() : pub error : expected task\n") { out }
    }
    @Test
    fun kk_02_pub() {
        val out = test("""
            pub(nil)
        """)
        assert(out == " v  anon : (lin 2, col 13) : pub(nil) : pub error : expected task\n") { out }
    }
    @Test
    fun kk_03_pub() {
        val out = test("""
            val T = task () {
                println(pub())
                yield(nil) { as it => nil }
            }
            val t = spawn T()
            println(pub(t))
        """)
        assert(out == "nil\nnil\n") { out }
    }
    @Test
    fun kk_04_pub() {
        val out = test("""
            val T = task () {
                pub(10)
                yield(nil) { as it => nil }
            }
            val t = spawn T()
            println(pub(t))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_05_pub_err() {
        val out = test("""
            val T = task () {
                do {
                    val x = []
                    pub(x)
                }
            }
            val t = spawn T()
        """)
        assert(out == " |  anon : (lin 8, col 27) : T()\n" +
                " v  anon : (lin 5, col 21) : pub(x) : set error : cannot copy reference to outer scope\n") { out }
    }
    @Test
    fun kk_06_pub_err() {
        val out = test("""
            val T = task () {
                yield(nil) { as it => nil }
            }
            val t = spawn T()
            do {
                val x = []
                pub(t, x)
            }
            println(pub(t))
        """)
        assert(out == " v  anon : (lin 8, col 17) : pub(t,x) : set error : cannot copy reference to outer scope\n") { out }
    }

    // NESTED

    @Test
    fun ll_01_nested() {
        val out = test("""
            spawn task () {
                val v = 10
                spawn task () {
                    println(v)
                } ()
                yield(nil) { as it => nil }
            } ()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_02_nested() {
        val out = test("""
            val F = func () {
                val v = 10
                val f = func () {
                    v
                }
                f()
            }
            println(F())
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_03_nested() {
        val out = test("""
            spawn task () {
                var xxx = 1
                yield(nil) { as it => nil }
                spawn task () {
                    set xxx = 10
                } ()
                println(xxx)
            } ()
            broadcast(nil)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_04_nested() {
        val out = test("""
            spawn task () {
                var xxx = 1
                yield(nil) { as it => nil }
                spawn task () {
                    set xxx = 10
                } ()
                println(xxx)
            } ()
            do {
                broadcast(nil)
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ll_05_nested() {
        val out = test("""
            spawn task () {
                val t = []
                spawn task () {
                    yield(nil) { as it => nil }
                    println(t)
                } ()
                yield(nil) { as it => nil }
            } ()
            coroutine(coro () { nil })
            broadcast(nil)
       """)
        assert(out == "[]\n") { out }
    }

    // ORIG

    @Test
    fun zz_00_spawn() {
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
    fun zz_02_spawn_err() {
        val out = test("""
            spawn (func () {nil}) ()
        """)
        assert(out == " v  anon : (lin 2, col 13) : spawn error : expected task\n") { out }
    }
    @Test
    fun zz_03_spawn_err() {
        val out = test("""
            spawn (func () {nil})
        """)
        assert(out == "anon : (lin 3, col 9) : invalid spawn : expected call\n") { out }
    }
    @Test
    fun zz_04_bcast() {
        val out = test("""
            var tk = task (v) {
                yield(nil) { as it => nil }
                val v' = yield(nil) { as it => it }
                throw(:1)
            }
            var co1 = spawn tk ()
            var co2 = spawn tk ()
            catch { as it => it==:1 } in {
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
        """)
        assert(out == "1\n2\n99\n") { out }
    }
    @Test
    fun zz_05_bcast_in() {
        val out = test("""
            val T = task (v) {
                spawn (task () {
                    yield(nil) { as it => nil }
                    println(:ok)
                }) ()
                broadcast (:ok)
            }
            spawn T(2)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_06_bcast_in() {
        val out = test("""
            spawn (task () {
                spawn (task () {
                    yield(nil) { as it => nil }
                    broadcast(nil)
                }) ()
                yield(nil) { as it => nil }
            }) ()
            broadcast(nil)
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun zz_07_throw() {
        val out = test("""
            var co
            set co = spawn (task () {
                catch { as it => :e1 } in {
                    coroutine (coro () {
                        yield(nil) { as it => nil }
                        throw(:e1)
                    })()
                    xloop {
                        yield(nil) { as it => nil }
                    }
                }
                println(:e1)
                yield(nil) { as it => nil }
                throw(:e2)
            })()
            catch { as it => :e2 } in {
                broadcast(nil)
                broadcast(nil)
                println(99)
            }
            println(:e2)
        """)
        assert(out == ":e1\n:e2\n") { out }
    }
    @Test
    fun zz_09_throw() {
        val out = test(
            """
            var T
            set T = task () {
                catch { as it => it==:e1 } in {
                    spawn task () {
                        yield(nil) { as it => nil }
                        throw(:e1)
                        println(:no)
                    } ()
                    xloop { yield(nil) { as it => nil } }
                }
                println(:ok1)
                throw(:e2)
                println(:no)
            }
            spawn (task () {
                catch { as it => :e2 } in {
                    spawn T()
                    xloop { yield(nil) { as it => nil } }
                }
                println(:ok2)
                throw(:e3)
                println(:no)
            }) ()
            catch { as it => :e3 } in {
                broadcast(nil)
                println(:no)
            }
            println(:ok3)
        """
        )
        assert(out == ":ok1\n:ok2\n:ok3\n") { out }
    }
    @Test
    fun zz_10_bcast() {
        val out = test("""
            ;;println(:BLOCK0, `:pointer ceu_block`)
            spawn (task () {
                ;;println(:CORO1, `:pointer ceu_x`)
                ;;println(:BLOCK1, `:pointer ceu_block`)
                spawn (task () {
                    ;;println(:CORO2, `:pointer ceu_x`)
                    ;;println(:BLOCK2, `:pointer ceu_block`)
                    yield(nil) { as it => nil }
                    ;;println(:1)
                    broadcast(nil)
                }) ()
                yield(nil) { as it => nil }
                ;;println(:2)
            }) ()
            broadcast(nil)
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun zz_11_bcast() {
        val out = test(
            """
            var tk
            set tk = task () {
                yield(nil) {as it => println(it)}
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
                yield(nil) { as it => println(it) }
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
                    yield(nil) { as it => println(it) }
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
                        yield(nil) { as it => it }
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
        //assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : broadcast e\n" +
        //        " v  anon : (lin 5, col 25) : resume error : incompatible scopes\n") { out }
        assert(out == " |  anon : (lin 12, col 13) : broadcast(e)\n" +
                " v  anon : (lin 5, col 36) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_15_bcast_err() {
        val out = test(
            """
            var T = task () {
                var v = yield(nil) { as it => it }
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
        //assert(out == ":1\n:2\n1\n") { out }
        //assert(out == "anon : (lin 10, col 35) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 9, col 13) : broadcast e\n" +
        //        " v  anon : (lin 3, col 25) : resume error : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast e\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive assigned reference\n") { out }
        assert(out == " |  anon : (lin 13, col 25) : broadcast(e)\n" +
                " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_15_bcast_okr() {
        val out = test(
            """
            var T = task () {
                yield(nil) { as it => println(it) }
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
                yield(nil) {as it => it}
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
        //assert(out == ":1\n:2\n1\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : broadcast e\n" +
        //        " v  anon : (lin 4, col 17) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == "anon : (lin 11, col 39) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == " |  anon : (lin 14, col 21) : broadcast(e)\n" +
                " v  anon : (lin 4, col 28) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_17_bcast() {
        val out = test(
            """
            var T1 = task () {
                yield(nil) {as it => nil}
                spawn task () {
                    val evt = yield(nil) {as it => it}
                    println(:1)
                    var v = evt
                } ()
                nil
            }
            var t1 = spawn T1()
            var T2 = task () {
                yield(nil) {as it => nil}
                val evt = yield(nil) {as it => nil}
                ;;println(:2)
                do {
                    var v = evt
                    ;;println(:evt, v, evt)
                }
            }
            var t2 = spawn T2()
            broadcast ([])
            println(`:number ceu_gc_count`)
            """
        )
        assert(out == "0\n") { out }
        //assert(out == "1\n") { out }
        //assert(out == "anon : (lin 20, col 13) : broadcast []\n" +
        //        "anon : (lin 16, col 17) : declaration error : incompatible scopes\n" +
        //        ":2\n" +
        //        ":error\n") { out }
    }
    @Test
    fun zz_18_bcast_tuple_func_ok() {
        val out = test("""
            var fff = func (v) {
                println(v)
            }
            var T = task () {
                yield(nil) {as it => fff(it)}
            }
            spawn T()
            broadcast ([1])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun zz_19_bcast_tuple_func_no() {
        val out = test("""
            var f = func (v) {
                val x = v[0]    ;; v also holds x, both are fleeting -> unsafe
                println(x)      ;; x will be freed and v would contain dangling pointer
            }
            var T = task () {
                f(yield(nil) {as it => it})
            }
            spawn T()
            broadcast ([[1]])
        """)
        assert(out == " |  anon : (lin 10, col 13) : broadcast([[1]])\n" +
                " v  anon : (lin 7, col 30) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 10, col 13) : broadcast [[1]]\n" +
        //        " |  anon : (lin 7, col 17) : f(yield(nil) { it })\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot move to deeper scope with pending references\n") { out }
    }
    @Test
    fun zz_19_bcast_tuple_func_ok_not_fleet() {
        val out = test("""
            var f = func (v) {
                val x = v[0]    ;; v also holds x, both are fleeting -> unsafe
                println(x)      ;; x will be freed and v would contain dangling pointer
            }
            var T = task () {
                val evt = yield(nil) {as it => it}   ;; NOT FLEETING (vs prv test)
                f(evt)
            }
            spawn T()
            broadcast ([[1]])
        """)
        //assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 11, col 13) : broadcast [[1]]\n" +
        //        " v  anon : (lin 7, col 17) : declaration error : cannot hold event reference\n") { out }
        assert(out == " |  anon : (lin 11, col 13) : broadcast([[1]])\n" +
                " v  anon : (lin 7, col 38) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_20_bcast_tuple_func_ok() {
        val out = test("""
            var f = func (v) {
                val :tmp x = v[0]
                println(x)
            }
            var T = task () {
                yield(nil) { as it => f(it) }
            }
            spawn T()
            broadcast ([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun zz_21_bcast_tuple_func_ok() {
        val out = test("""
            var f = func (v) {
                val :tmp x = v[0]
                println(x)
            }
            var g = func (v) {
                val evt = v
                f(evt)
            }
            g([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun zz_22_pool_throw() {
        val out = test(
            """
            println(1)
            catch {as it => it==:ok} in {
                println(2)
                spawn task () {
                    println(3)
                    ${AWAIT()}
                    println(6)
                    throw(:ok)
                } ()
                spawn task () {
                    println(4)
                    ${AWAIT()}
                    println(999)
                } ()
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
        val out = test("""
            spawn task () {
                var t = []
                spawn task () {
                    yield(nil) { as it => nil }
                    println(t)
                } ()
                yield(nil) { as it => nil }
            } ()
            broadcast ([])
        """)
        assert(out == "[]\n") { out }
    }
}
