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
                set v = yield () 
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
                yield ()
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
        assert(out == "1\n#coro\n3\n4\n5\n6\n") { out }
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
        assert(out == "anon : (lin 3, col 9) : expected invalid spawn : expected call : have end of file") { out }
    }

    // THROW

    @Test
    fun throw1() {
        val out = all("""
            var co
            set co = coroutine task (x,y) {
                throw #e2
            }
            catch #e2 {
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
                yield ()
                throw #e2
            }
            catch #e2 {
                resume co(1,2)
                println(1)
                resume co()
                println(2)
            }
            println(3)
        """)
        assert(out == "1\n3\n") { out }
    }

    // BROADCAST

    @Test
    fun bcast1() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v)
                set v = yield ()
                println(v)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            broadcast 1
            broadcast 2
            broadcast 3
        """)
        assert(out == "1\n1\n2\n2\n") { out }
    }
    @Test
    fun bcast2() {
        val out = all("""
            var co1
            set co1 = coroutine task () {
                var co2
                set co2 = coroutine task () {
                    yield ()
                    println(2)
                }
                resume co2 ()
                yield ()
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
                    yield ()
                    throw #error
                }
                resume co2 ()
                yield ()
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
            set tk = task (v) {
                do {
                    println(v)
                    set v = yield ()
                    println(v)
                }
                do {
                    println(v)
                    set v = yield ()
                    println(v)
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
                set v = yield ()
                println(v)
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
            set tk = task (v) {
                println(v)
                set v = yield ()
                println(v)                
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
        assert(out == "1\n1\n2\n2\n") { out }
    }
    @Test
    fun bcast8() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v)
                set v = yield ()
                println(v)                
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
        assert(out == "1\n1\n2\n2\n") { out }
    }

    // POOL

    @Test
    fun pool1() {
        val out = all("""
            var ts
            set ts = coroutines()
            println(tags(ts))
            var T
            set T = task (v) {
                println(v)
                set v = yield ()
                println(v)
            }
            do {
                spawn T(1) in ts
            }
            broadcast 2
        """)
        assert(out == "#coros\n1\n2\n") { out }
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
            spawn T(1) in ts
            spawn T(2) in ts
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
            spawn T(1) in ts
            spawn T(2) in ts
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
                    set v = yield ()
                    println(v)
                }
                spawn T(1) in ts
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
                spawn T(1) in ts
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
            spawn T() in ts
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
    fun poolN() {
        val out = all("""
            var ts
            set ts = coroutines()
            println(tags(ts))
            var T
            set T = task (v) {
                pub = v
                println(v)
                set v = yield ()
                println(v)
            }
            spawn T(1) in ts
            spawn T(2) in ts
            
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
        assert(out == "#coros\n1\n2\n") { out }
    }

}
