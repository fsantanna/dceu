package xceu

import ceu.all
import org.junit.Test

class TXJS {

    //////////////////////////////////////////////////////////////////////////
    // TITLE
    //  - https://exploringjs.com/es6/ch_generators.html
    // Summary:
    //  - Declaring a generator prototype:
    //      - JS:  function* genFunc (...) { ... }
    //      - Ceu: task genFunc (...) { ... }
    //  - Instantiating a generator:
    //      - JS:  gen = genFunc(...)
    //      - Ceu: gen = coroutine(genFunc)     ;; cannot pass argument (like Lua), if arg is required, init yield() required
    //  - Resuming an instantiated generator:
    //      - JS:  gen.next(...)
    //      - Ceu: resume gen(...)
    //  - Yielding from a generator:
    //      - JS:  v = yield(...)
    //      - Ceu: v = yield(...)
    //
    // 22.1.3 Use case: implementing iterables
    //      - spawn (pass parameter, but requires yield) vs coroutine (no parameter, no yield)
    // 22.3.2 Returning from a generator
    //      - no return in Ceu, no done in Ceu, expects nil or :terminated
    // 22.3.3 Throwing an exception from a generator
    //      - That means that next() can produce three different “results”:
    //      - Ceu: value, nil, throw
    // 22.3.6.1 yield* considers end-of-iteration values
    //      - Ceu: yield/return are the same
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
                while in :dict, obj, (k, v) {
                    yield([k, v])
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

    // 22.3.4 Example: iterating over properties
    // (same as) 22.1.3 Use case: implementing iterables

    //  22.3.5 You can only yield in generators

    @Test
    fun x8() {
        val out = all("""
            task genFunc () {
                func () {
                    yield() ;; anon : (lin 4, col 21) : yield error : expected enclosing task
                }()
            }
        """, true)
        assert(out == "anon : (lin 4, col 21) : yield error : expected enclosing task") { out }
    }
    @Test
    fun x9() {
        val out = all("""
            task genFunc () {
                while in :vector, #['a','b'], (i, v) {
                    yield([i,v])
                }
            }
            var arr = tovector(coroutine(genFunc))
            println(arr)
        """, true)
        assert(out == "#[[0,a],[1,b]]\n") { out }
    }

    // 22.3.6 Recursion via yield*

    @Test
    fun x10() {
        val out = all("""
            task foo () {
                yield('a')
                yield('b')
            }
            task bar () {
                yield('x')
                while in :coro, coroutine(foo), i {
                    yield(i)
                }
                yield('y')
            }
            var arr = tovector(coroutine(bar))
            println(arr)
        """, true)
        assert(out == "xaby\n") { out }
    }

    @Test
    fun todo_x11() {
        val out = all("""
            task foo () {
                yield('a')
                yield('b')
            }
            task bar () {
                yield('x')
                yield :all (coroutine(foo))
                yield('y')
            }
            var arr = tovector(coroutine(bar))
            println(arr)
        """, true)
        assert(out == "xaby\n") { out }
    }

    // 22.3.6.1 yield* considers end-of-iteration values
    //  - In Ceu, like in Lua, yield/return are the same.
    //  - The last expression is an implicit "return yield".
    //      - Same in Lua, even w/o explicit return ("return yield" becomes nil).
    //  - Return yield *always* happens, since it is implicit.
    //  - To distinguish, may return tuple as [:next, v], [:return, v]

    @Test
    fun x12() {
        val out = all("""
            task genFuncWithReturn () {
                yield('a')
                yield('b')
                'c'
            }
            task logReturned (genObj) {
                yield()
                ;;>>> yield :all
                while in :coro, genObj, i {
                    yield(i)
                }
                ;;<<< yield :all
            }
            println(tovector(spawn logReturned(coroutine(genFuncWithReturn))))
        """, true)
        assert(out == "abc\n") { out }
    }
}
