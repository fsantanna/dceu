package ceu

import org.junit.Test

class TTask {

    // TASK / COROUTINE / RESUME / YIELD

    @Test
    fun task1() {
        val out = all("""
            var t
            set t = task (v) {
                println(v)          ;; 1
                set v = yield (v+1) 
                println(v)          ;; 3
                set v = yield v+1 
                println(v)          ;; 5
                v+1
            }
            var a
            set a = coroutine t
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
    fun task2_err() {
        val out = all("""
            coroutine func () {nil}
        """.trimIndent())
        assert(out == "anon : (lin 1, col 11) : coroutine error : expected task\n") { out }
    }
    @Test
    fun task3_err() {
        val out = all("""
            var f
            resume f()
        """.trimIndent())
        assert(out == "anon : (lin 2, col 8) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task4_err() {
        val out = all("""
            var co
            set co = coroutine task () {nil}
            resume co()
            resume co()
        """.trimIndent())
        assert(out == "anon : (lin 4, col 8) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task5_err() {
        val out = all("""
            var co
            set co = coroutine task () { nil
            }
            resume co()
            resume co(1,2)
        """)
        assert(out == "anon : (lin 6, col 20) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task6() {
        val out = all("""
            var co
            set co = coroutine task (v) {
                set v = yield nil 
                println(v)
            }
            resume co(1)
            resume co(2)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun task7() {
        val out = all("""
            var co
            set co = coroutine task (v) {
                println(v)
            }
            println(1)
            resume co(99)
            println(2)
        """)
        assert(out == "1\n99\n2\n") { out }
    }
    @Test
    fun tak8_err() {
        val out = all("""
            var xxx
            resume xxx() ;;(xxx(1))
        """)
        assert(out == "anon : (lin 3, col 20) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task9_mult() {
        val out = all("""
            var co
            set co = coroutine task (x,y) {
                println(x,y)
            }
            resume co(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun task10_err() {
        val out = all("""
            var co
            set co = coroutine task () {
                yield nil
            }
            resume co()
            resume co(1,2)
        """)
        assert(out.contains("bug found : not implemented : multiple arguments to resume")) { out }
    }
    @Test
    fun task11_class() {
        val out = all("""
            var T
            set T = task (x,y) {
                println(x,y)
            }
            resume (coroutine T) (1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun task12_tuple_leak() {
        val out = all("""
            var T
            set T = task () {
                [1,2,3]
                yield nil
            }
            resume (coroutine T) ()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun task13_defer() {
        val out = all("""
            var T
            set T = task () {
                defer {
                    println(2)
                }
                yield nil
            }
            resume (coroutine T) ()
            println(1)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun yield14_err() {
        val out = all("""
            yield nil
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing task") { out }
    }
    @Test
    fun yield15_err() {
        val out = all("""
            task () {
                func () {
                    yield nil
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : expected enclosing task") { out }
    }
    @Test
    fun task16_nest() {
        val out = all("""
            spawn task (v1) {
                spawn task (v2) {
                    spawn task (v3) {
                        println(v1,v2,v3)
                    }(3)
                }(2)
            }(1)
        """)
        assert(out == "1\t2\t3\n") { out }
    }

    // SPAWN

    @Test
    fun spawn1() {
        val out = all("""
            var t
            set t = task (v) {
                println(v)          ;; 1
                set v = yield (v+1) 
                println(v)          ;; 3
                set v = yield v+1 
                println(v)          ;; 5
                v+1
            }
            var a
            set a = spawn t(1)
            println(tags(a))
            var v
            set v = resume a(3)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """, true)
        assert(out == "1\n:coro\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun spawn2() {
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
    fun spawn3_err() {
        val out = all("""
            spawn (func () {nil}) ()
        """)
        assert(out == "anon : (lin 2, col 20) : coroutine error : expected task\n") { out }
    }
    @Test
    fun spawn4_err() {
        val out = all("""
            spawn (func () {nil})
        """)
        assert(out == "anon : (lin 3, col 9) : invalid spawn : expected call") { out }
    }
    @Test
    fun spawn5() {
        val out = all("""
            var t
            set t = task () {
                println(1)
                do {
                    println(2)
                    yield nil
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

    // THROW

    @Test
    fun throw1() {
        val out = all("""
            var co
            set co = coroutine task (x,y) {
                throw :e2
            }
            catch :e2 {
                resume co(1,2)
                println(99)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun throw2() {
        val out = all("""
            var co
            set co = coroutine task (x,y) {
                yield nil
                throw :e2
            }
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
    fun throw3() {
        val out = all("""
            var T
            set T = task () {
                spawn task () {
                    yield nil
                    throw :error 
                }()
                yield nil
            }
            spawn in coroutines(), T()
            broadcast nil
        """)
        assert(out == "anon : (lin 6, col 21) : throw error : uncaught exception\n") { out }
    }

    // BCAST / BROADCAST

    @Test
    fun bcast0() {
        val out = all("""
            println(broadcast 1)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun bcast1() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v)
                println(evt)
                set v = yield nil
                println(v)                
                println(evt)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            broadcast 1
            broadcast 2
            broadcast 3
        """)
        assert(out == "nil\n1\nnil\n1\nnil\n2\nnil\n2\n") { out }
    }
    @Test
    fun bcast2() {
        val out = all("""
            var co1
            set co1 = coroutine task () {
                var co2
                set co2 = coroutine task () {
                    yield nil
                    println(2)
                }
                resume co2 ()
                yield nil
                println(1)
            }
            resume co1 ()
            broadcast nil
        """)
        assert(out == "2\n1\n") { out }
    }
    @Test
    fun bcast3() {
        val out = all("""
            var co1
            set co1 = coroutine task () {
                var co2
                set co2 = coroutine task () {
                    yield nil
                    throw :error
                }
                resume co2 ()
                yield nil
                println(1)
            }
            resume co1 ()
            broadcast nil
        """)
        assert(out == "anon : (lin 7, col 21) : throw error : uncaught exception\n") { out }
    }
    @Test
    fun bcast4() {
        val out = all("""
            var tk
            set tk = task () {
                do {
                    println(evt)
                    yield nil
                    println(evt)
                }
                do {
                    println(evt)
                    yield nil
                    println(evt)
                }
            }
            var co
            set co = coroutine tk
            broadcast 1
            broadcast 2
            broadcast 3
            broadcast 4
        """)
        assert(out == "1\n2\n2\n3\n") { out }
    }
    @Test
    fun bcast5() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v)
                yield nil
                println(evt)
            }
            var co
            set co = coroutine tk
            resume co(1)
            broadcast 2
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bcast6() {
        val out = all("""
            func () {
                broadcast 1
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bcast7() {
        val out = all("""
            var tk
            set tk = task () {
                yield nil
                println(evt)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            do {
                broadcast 1
                broadcast 2
                broadcast 3
            }
        """)
        assert(out == "2\n2\n") { out }
    }
    @Test
    fun bcast8() {
        val out = all("""
            var tk
            set tk = task (v) {
                yield nil
                println(evt)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            func () {
                broadcast 1
                broadcast 2
                broadcast 3
            }()
        """)
        assert(out == "2\n2\n") { out }
    }
    @Test
    fun bcast9() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                set v = yield nil
                throw :1                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            catch err==:1 {
                func () {
                    println(1)
                    broadcast 1
                    println(2)
                    broadcast 2
                    println(3)
                    broadcast 3
                }()
            }
            println(99)
        """
        )
        assert(out == "1\n2\n99\n") { out }
    }
    @Test
    fun bcast10() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                println(v)
                yield nil
                println(evt)                
                yield nil
                println(evt)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            catch err==:1 {
                func () {
                    println(1)
                    resume co1(10)
                    resume co2(10)
                    println(2)
                    broadcast [20]
                    println(3)
                    broadcast @[(30,30)]
                }()
            }
        """
        )
        assert(out == "1\n10\n10\n2\n[20]\n[20]\n3\n@[(30,30)]\n@[(30,30)]\n") { out }
    }
    @Test
    fun bcast11() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                do {
                    set v = evt
                }
            }
            var co
            set co = coroutine tk
            broadcast []
        """
        )
        assert(out == "anon : (lin 5, col 29) : set error : incompatible scopes\n") { out }
    }

    // POOL

    @Test
    fun pool0() {
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
    fun pool1() {
        val out = all("""
            var ts
            set ts = coroutines()
            println(tags(ts))
            var T
            set T = task (v) {
                println(v)
                yield nil
                println(evt)
            }
            do {
                spawn in ts, T(1)
            }
            broadcast 2
        """)
        assert(out == ":coros\n1\n2\n") { out }
    }
    @Test
    fun pool2_leak() {
        val out = all("""
            var T
            set T = task () {
                [1,2,3]
                yield nil
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
    fun pool4_defer() {
        val out = all("""
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                yield nil
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
    fun pool5_scope() {
        val out = all("""
            do {
                var ts
                set ts = coroutines()
                var T
                set T = task (v) {
                    println(v)
                    set v = yield nil
                    println(v)
                }
                spawn in ts, T(1)
            }
            broadcast 2
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pool6_terminate() {
        val out = all("""
            do {
                var ts
                set ts = coroutines()
                var T
                set T = task (v) {
                    println(v)
                }
                spawn in ts, T(1)
                while t in ts {
                    throw 1     ;; never reached
                }
            }
            broadcast 2
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pool7_leak() {
        val out = all("""
            var T
            set T = task () {
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pool8_err() {
        val out = all("""
            while x in nil {
                nil
            }
        """)
        assert(out == "anon : (lin 2, col 24) : while error : expected coroutines\n") { out }
    }
    @Test
    fun pool9_term() {
        val out = all("""
            var T
            set T = task () {
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while xxx in ts {
                println(1)
                broadcast 1
            }
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun pool10_term() {
        val out = all("""
            var T
            set T = task () {
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while xxx in ts {
                println(1)
                broadcast 1
                while yyy in ts {
                    println(2)
                }
            }
            println(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun pool11_err_scope() {
        val out = all("""
            var T
            set T = task () { yield nil }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var yyy
            while xxx in ts {
                set yyy = xxx
            }
        """)
        assert(out == "anon : (lin 9, col 27) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun pool12_err_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield nil }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while xxx in ts {
                var yyy
                while zzz in ts {
                    set yyy = zzz
                }
                set yyy = xxx
            }
        """
        )
        assert(out == "anon : (lin 10, col 31) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun pool13_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield nil }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while xxx in ts {
                var yyy
                while zzz in ts {
                    nil
                }
                set yyy = xxx
            }
            println(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun pool14_max_err() {
        val out = ceu.all(
            """
            coroutines(0)
        """
        )
        assert(out == "anon : (lin 2, col 24) : coroutines error : expected positive number\n") { out }
    }
    @Test
    fun pool15_max_err() {
        val out = ceu.all(
            """
            coroutines(nil)
        """
        )
        assert(out == "anon : (lin 2, col 24) : coroutines error : expected positive number\n") { out }
    }
    @Test
    fun pool16_max() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(1)
            var T
            set T = task () { yield nil }
            var ok1
            set ok1 = spawn in ts, T()
            var ok2
            set ok2 = spawn in ts, T()
            broadcast nil
            var ok3
            set ok3 = spawn in ts, T()
            var ok4
            set ok4 = spawn in ts, T()
            println(ok1, ok2, ok3, ok4)
        """
        )
        assert(out == "true\tfalse\ttrue\tfalse\n") { out }
    }
    @Test
    fun pool17_term() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(2)
            var T
            set T = task (v) {
                println(10)
                defer {
                    println(20)
                }
                yield nil
                if v {
                    yield nil
                } else {
                    nil
                }
            }
            println(0)
            spawn in ts, T(false)
            spawn in ts, T(true)
            println(1)
            broadcast @[]
            println(2)
            broadcast @[]
            println(3)
        """
        )
        assert(out == "0\n10\n10\n1\n20\n2\n20\n3\n") { out }
    }
    @Test
    fun pool18_throw() {
        val out = ceu.all(
            """
            println(1)
            catch err==:ok {
                println(2)
                spawn task () {
                    println(3)
                    yield nil
                    println(6)
                    throw :ok
                } ()
                spawn task () {
                    catch :ok {
                        println(4)
                        yield nil
                    }
                    println(999)
                } ()
                println(5)
                broadcast nil
                println(9999)
            }
            println(7)
        """
        )
        assert(out == "1\n2\n3\n4\n5\n6\n7\n") { out }
    }
    @Test
    fun pool19_throw() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                spawn task () {
                    println(v)
                    yield nil
                    println(v)
                } ()
                while true { yield nil }
            }
            spawn T(1)
            spawn T(2)
            broadcast nil
        """
        )
        assert(out == "1\n2\n1\n2\n") { out }
    }
    @Test
    fun pool20_throw() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(2)
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                catch err==:ok {
                    spawn task () {
                        yield nil
                        if v == 1 {
                            throw :ok
                        } else {
                            nil
                        }
                        while true { yield nil }
                    } ()
                    while true { yield nil }
                }
                println(v)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            broadcast nil
            broadcast nil
            println(999)
        """
        )
        assert(out == "1\n1\n999\n2\n") { out }
    }
    @Test
    fun pool21_throw() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(2)
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                catch err==:ok {
                    spawn task () {
                        yield nil
                        if v == 2 {
                            throw :ok
                        } else {
                            nil
                        }
                        while true { yield nil }
                    } ()
                    while true { yield nil }
                }
                println(v)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            broadcast nil
            broadcast nil
            println(999)
        """
        )
        assert(out == "2\n2\n999\n1\n") { out }
    }
    @Test
    fun todo_poolN() {
        val out = all("""
            var ts
            set ts = coroutines()
            println(tags(ts))
            var T
            set T = task (v) {
                set pub = v
                println(v)
                set v = yield nil
                println(v)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            
            var x
            while t1 in ts {
                set x = t1
                while t2 in ts {
                    set x = t2
                    println(t1.pub, t2.pub)
                }
            }
            broadcast 2
        """)
        assert(out == ":coros\n1\n2\n") { out }
    }

    // EVT

    @Test
    fun evt_hld1_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                set xxx = evt
            }
            var co
            set co = coroutine tk
            broadcast []
        """
        )
        assert(out == "anon : (lin 4, col 27) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun evt_hld2_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                yield nil
                set xxx = evt
            }
            var co
            set co = coroutine tk
            broadcast 1
            broadcast []
        """
        )
        assert(out == "anon : (lin 5, col 27) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun evt_hld3() {
        val out = ceu.all(
        """
            var fff
            set fff = func (x) { x }
            spawn task () {
                yield nil
                while evt[:type]/=:x {
                    yield nil
                }
                println(99)
            }()
            println(1)
            broadcast @[(:type,:y)]
            println(2)
            broadcast @[(:type,:x)]
            println(3)
        """)
        assert(out == "1\n2\n99\n3\n") { out }
    }
    @Test
    fun evt_hld4_err() {
        val out = ceu.all(
            """
            var fff
            set fff = func (x) { x }
            spawn task () {
                do {
                    yield nil
                }
                fff(evt[:type])
                println(99)
            }()
            broadcast @[(:type,:y)]
            broadcast @[(:type,:x)]
        """
        )
        //assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "99\n") { out }
    }
    @Test
    fun evt5() {
        val out = ceu.all(
            """
            spawn task () {
                while (true) {
                    println(evt)
                    yield nil
                }
            }()
            broadcast @[]
        """
        )
        assert(out == "nil\n@[]\n") { out }
    }
    @Test
    fun evt6() {
        val out = ceu.all(
            """
            spawn task () {
                while (true) {
                    do {
                        yield nil
                    }
                    println(evt)
                }
            }()
            broadcast @[]
        """
        )
        assert(out == "@[]\n") { out }
    }

    // PUB

    @Test
    fun pub1_err() {
        val out = all("""
            var a
            a.pub
        """, true)
        assert(out == "anon : (lin 3, col 13) : pub error : expected coroutine\n") { out }
    }
    @Test
    fun pub2_err() {
        val out = all("""
            pub
        """, true)
        assert(out == "anon : (lin 2, col 13) : pub error : expected enclosing task") { out }
    }
    @Test
    fun pub3() {
        val out = all("""
            var t
            set t = task (v1) {
                set pub = v1
                var v2
                set v2 = yield nil
                set pub = pub + v2
            }
            var a
            set a = coroutine t
            println(a.pub)
            resume a(1)
            println(a.pub)
            resume a(2)
            println(a.pub)
        """, true)
        assert(out == "nil\n1\n3\n") { out }
    }
    @Test
    fun pub4_err() {
        val out = all("""
            var t
            set t = task () {
                set pub = []
            }
            var x
            do {
                var a
                set a = coroutine t
                resume a()
                set x = a.pub
            }
            println(x)
        """)
        assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun pub5() {
        val out = all("""
            var t
            set t = task () {
                set pub = 10
            }
            var a
            set a = coroutine t
            resume a()
            println(a.pub + a.pub)
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun pub6_pool() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var x
            while t in ts {
                println(t.pub[0]+t.pub[0])
            }
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun todo_pub7_pool_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var x
            while t in ts {
                set x = t.pub   ;; TODO: incompatible scope
            }
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun todo_pub8_fake_task() {
        val out = all("""
            spawn (task () {
                set pub = 1
                spawn (task :nopub () {
                    println(pub)
                }) ()
            }) ()
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_pub9_fake_task() {
        val out = all("""
            spawn (task () {
                set pub = []
                var x
                spawn (task :nopub () {
                    set x = pub
                }) ()
                println(x)
            }) ()
        """, true)
        assert(out == "[]\n") { out }
    }
}
