package xceu

import ceu.all
import ceu.lexer
import ceu.yield
import org.junit.Test

class TXGens {

    // Summary:
    //  - Declaring a generator prototype:
    //      - JS:  function* genFunc (...) { ... }
    //      - Ceu: task genFunc (...) { ... }
    //  - Instantiating a generator:
    //      - JS:  gen = genFunc()
    //      - Ceu: gen = coroutine(genFunc)
    //  - Resuming an instantiated generator:
    //      - JS:  gen.next(...)
    //      - Ceu: resume gen(...)
    //  - Yielding from a generator:
    //      - JS:  v = yield(...)
    //      - Ceu: v = yield(...)

    // 22.1 Overview

    // 22.1.1 What are generators?

    @Test
    fun x1() {
        val out = all("""
            task genFunc () {
                println("First")
                yield()
                println("Second")
            }
            var genObj = coroutine(genFunc)
            resume genObj()     ;; First
            resume genObj()     ;; Second
        """)
        assert(out == "First\nSecond\n") { out }
    }

    // 22.1.2 Kinds of generators

    @Test
    fun x2() {
        val out = all("""
            task gen1 (v) { println(v) }
            var co1 = coroutine(gen1)
            resume co1(1)

            var gen2 = task (v) { println(v) }
            var co2 = coroutine(gen1)
            resume co2(2)
            
            var obj3 = @[
                gen = task (v) { println(v) }
            ]
            var co3 = coroutine(obj3.gen)
            resume co3(3)
            
            ;; no co4
        """)
        assert(out == "1\n2\n3\n") { out }
    }

    // 22.1.3 Use case: implementing iterables

    @Test
    fun x3() {
        val out = all("""
            task objectEntries (obj) {
                var key = next(obj)
                while key /= nil {
                    yield([key, obj[key]])
                    set key = next(obj,key)
                }
            }
            
            var jane = @[
                first = "Jane",
                last  = "Doe",
            ]
            var co = spawn objectEntries(jane)
            while in co, v {
                println((v.0 ++ ": ") ++ v.1)
            }
        """, true)
        assert(out == "First\nSecond\n") { out }
    }
}
