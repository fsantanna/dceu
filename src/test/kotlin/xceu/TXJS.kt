package xceu

import ceu.all
import org.junit.Test

class TXJS {

    //////////////////////////////////////////////////////////////////////////
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
    //
    // 22.1.3 Use case: implementing iterables
    //      - spawn vs coroutine
    // 22.3.2 Returning from a generator
    //      - no return in Ceu, no done in Ceu, expects nil or :terminated
    // 22.3.3 Throwing an exception from a generator
    //      - That means that next() can produce three different “results”:
    //      - Ceu: value, nil, throw
    //////////////////////////////////////////////////////////////////////////

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
                yield()
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
            while in :coro, co, v {
                println((tostring(v.0) ++ ": ") ++ v.1)
            }
        """, true)
        assert(out == ":first: Jane\n:last: Doe\n") { out }
    }

    //  22.1.4 Use case: simpler asynchronous code

    @Test
    fun x4() {
        val out = all("""
            group { ;; mock functions
                task fetch (url) {
                    if url == :error {
                        throw(:error)
                    }
                    url
                }
                task text (url) {
                    tostring(url)
                }
                task json (txt) {
                    "json " ++ txt
                }
            }
            task fetchJson (url) {
                var req = await spawn fetch(url)
                var txt = await spawn text(req)
                await spawn json(txt)
            }
            spawn {
                var obj1 = await spawn fetchJson(:good)
                println(obj1)   ;; json :good
                var obj2 = await spawn fetchJson(:error)
                println(obj2)   ;; never printed
            }
        """, true)
        assert(out.contains("uncaught exception\njson :good")) { out }
    }

    // 22.3 Generators as iterators (data production)

    @Test
    fun x5() {
        val out = all("""
            task genFunc() {
                yield('a')
                yield('b')
            }
            var genObj = coroutine(genFunc)
            println(resume genObj())
            println(resume genObj())
            println(resume genObj())
        """, true)
        assert(out == "a\nb\nnil\n") { out }
    }

    // 22.3.1 Ways of iterating over a generator

    @Test
    fun x6() {
        val out = all("""
            task genFunc() {
                yield('a')
                yield('b')
            }
            var arr = tovector(coroutine(genFunc))
            println(arr)
            
            ;; var [x,y] = ...  ;; TODO: destructor
        """, true)
        assert(out == "ab\n") { out }
    }

    // 22.3.2 Returning from a generator

    // 22.3.3 Throwing an exception from a generator

    @Test
    fun x7() {
        val out = all("""
            task genFunc() {
                throw(:problem)
            }
            var genObj = coroutine(genFunc)
            resume genObj()
                ;; anon : (lin 3, col 17) : throw error : uncaught exception
                ;; :problem
        """, true)
        assert(out == "anon : (lin 6, col 20) : genObj()\n" +
                "anon : (lin 3, col 17) : throw error : uncaught exception\n" +
                ":problem\n") { out }
    }
}
