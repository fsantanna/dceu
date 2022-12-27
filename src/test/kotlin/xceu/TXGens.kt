package xceu

import ceu.all
import ceu.lexer
import ceu.yield
import org.junit.Test

class TXGens {

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
            
            var dict3 = @[
                gen = task (v) { println(v) }
            ]
            set obj3.co = coroutine(obj3.gen)
            resume obj3.co(3)
            
            ;; no gen4/co4
        """)
        assert(out == "1\n2\n3\n") { out }
    }

    // 22.1.3 Use case: implementing iterables

    @Test
    fun x3() {
        val out = all("""
            task objectEntries (obj) {
                var key = next(obj)
                while key != nil {
                    yield([key, obj[key]])
                    set key = next(obj,key)
                }
            }
            
            var jane = @[
                first = "Jane",
                last  = "Doe",
            ]
            while in (spawn objectEntries(jane)), v {
                println((v.0 ++ ": ") ++ v.1)
            }
        """)
        assert(out == "First\nSecond\n") { out }
    }
}
