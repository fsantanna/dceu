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
    // Tradeoff JS:create=resume, Ceu:start=yield
    // Ceu is better because coro knows if init yield is required or not.
    // If func receives arg, use yield, otherwise no yield, no chnanes in caller side
    // JS caller doesnt know about coro body implementation
    // JS's coroutine constructor trick calls the first next automatically to advance the
    // coro to the first yield.
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
    // 22.4.1.1 The first next()
    //      - JS:  the only purpose of the first invocation of next() is to start the observer
    //             therefore, any input you send via the first next() is ignored
    //      - Ceu: first resume is received as arg (in JS it is in the coro first creation)
    // 22.4.3 return() and throw()
    //      - Ceu: TODO: kill(coro), no throw (makes sense?)
    //          - kill is also solved with scope
    // 22.4.4.1 Preventing termination
    //      - Ceu: not possible
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
                while in :dict obj, (k, v) {
                    yield([k, v])
                }
            }
            
            var jane = @[
                first = "Jane",
                last  = "Doe",
            ]
            var co = spawn objectEntries(jane)
            while in :coro co, v {
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
                while in :vector #['a','b'], (i, v) {
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
                while in :coro coroutine(foo), i {
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
    fun x11() {
        val out = all("""
            task foo () {
                yield('a')
                yield('b')
            }
            task bar () {
                yield('x')
                yield :all coroutine(foo)
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
                yield :all genObj
            }
            println(tovector(spawn logReturned(coroutine(genFuncWithReturn))))
        """, true)
        assert(out == "abc\n") { out }
    }

    // 22.3.6.2 Iterating over trees

    @Test
    fun x13() {
        val out = all("""
            var tree = @[
                v = 'a',
                l = @[
                    v = 'b',
                    l = @[v = 'c'],
                    r = @[v = 'd'],
                ],
                r = @[v = 'e']
            ]
            task T (tree) {
                yield()
                yield(tree.v)
                if tree.l {
                    yield :all spawn T(tree.l)
                }
                if tree.r {
                    yield :all spawn T(tree.r)
                }
            }
            println(tovector(spawn T(tree)))
        """, true)
        assert(out == "abcde\n") { out }
    }

    // 22.4 Generators as observers (data consumption)

    // 22.4.1 Sending values via next()

    @Test
    fun x14() {
        val out = all("""
            task dataConsumer () {
                println(:started)
                println(1, yield()) ;; (A)
                println(2, yield())
                :result
            }
            
            var genObj = coroutine(dataConsumer)
            println(resume genObj())
            println(resume genObj('a'))
            println(resume genObj('b'))
        """, true)
        assert(out == ":started\nnil\n1\ta\nnil\n2\tb\n:result\n") { out }
    }

    // 22.4.1.1 The first next()
    // In Ceu, both inputs are received.

    @Test
    fun x15() {
        val out = all("""
            task gen (input) {
                println(input)
                while true {
                    set input = yield() ;; (B)
                    println(input)
                }
            }
            var obj = coroutine(gen);
            resume obj('a');
            resume obj('b');
        """, true)
        assert(out == "a\nb\n") { out }
    }

    // 22.4.2 yield binds loosely
    // 22.4.2.1 yield in the ES6 grammar
    // In Ceu it is like a function call.

    // 22.4.3 return() and throw()
    // 22.4.4 return() terminates the generator
    // - TODO: kill(coro), no throw (makes sense?)

    @Test
    fun todo_x16_kill() {
        val out = all("""
            task genFunc1() {
                defer {
                    println(:exiting)
                }
                yield () ;; (A)
            }
            var genObj1 = coroutine(genFunc1)
            resume genObj1()
            kill genObj1()
            println(:end)
        """, true)
        assert(out == ":exiting\n:end\n") { out }
    }

    @Test
    fun x17_scope() {
        val out = all("""
            task genFunc1() {
                defer {
                    println(:exiting)
                }
                yield () ;; (A)
            }
            do {
                var genObj1 = coroutine(genFunc1)
                resume genObj1()
            }
            println(:end)
        """, true)
        assert(out == ":exiting\n:end\n") { out }
    }

    // 22.4.4.1 Preventing termination
    // Ceu: not possible

    // 22.4.4.2 Returning from a newborn generator

    @Test
    fun todo_x18() {
        val out = all("""
            task genFunc() {}
            var genObj = coroutine(genFunc)
            kill genObj(:yes)
            println(genObj.pub)
        """, true)
        assert(out == ":yes") { out }
    }

    // 22.4.5 throw() signals an error
    // 22.4.5.1 Throwing from a newborn generator
    // Ceu: not possible

    // 22.4.6 Example: processing asynchronously pushed data
    // Ceu uses a scope for the generator and uses a normal loop to feed data.

    @Test
    fun x19() {
        val out = all("""
            func readFile (fileName, target) {
                ;; TODO: from fileName
                resume target("ab\nc")
                resume target("")
                resume target("\ndefg\n")
            }
            
            task splitLines (target) {
                var cur = ""
                while true {
                    var tmp = yield()
                    while in :vector tmp, (_,c) {
                        if c == '\n' {
                            resume target(cur)
                            set cur = ""
                        } else {
                            set cur[+] = c
                        }
                    }
                }
            }
            
            task numberLines (target) {
                var n = 0
                while true {
                    var line = yield()
                    set n = n + 1
                    resume target((tostring(n) ++ ": ") ++ line)
                }
            }
            
            task printLines () {
                while true {
                    var line = yield()
                    println(line)
                }
            }
            
            var co_print = spawn printLines()
            var co_nums  = spawn numberLines(co_print)
            var co_split = spawn splitLines(co_nums)
            readFile(nil, co_split) 
        """, true)
        assert(out == "1: ab\n2: c\n3: defg\n") { out }
    }

    @Test
    fun x20() {
        val out = all("""
            task readFile (fileName) {
                ;; TODO: from fileName
                yield("ab\nc")
                yield("")
                yield("\ndefg\n")
            }
            
            task splitLines () {
                var cur = ""
                while true {
                    var tmp = yield()
                    while in :vector tmp, (_,c) {
                        if c == '\n' {
                            yield(cur)
                            set cur = ""
                        } else {
                            set cur[+] = c
                        }
                    }
                }
            }
            
            task numberLines () {
                var n = 0
                while true {
                    var line = yield()
                    set n = n + 1
                    yield((tostring(n) ++ ": ") ++ line)
                }
            }

            task printLines () {
                while true {
                    var line = yield()
                    println(line)
                }
            }
            
            var co_read  = coroutine(readFile)
            var co_split = spawn splitLines()
            var co_nums  = spawn numberLines()
            var co_print = spawn printLines()
            spawn {
                while in :coro co_read, chars {
                    while {
                        var line = if chars {
                            resume co_split(chars)
                        }
                        if line {
                            line <++ resume co_nums(line)
                            resume co_print(line)
                        }
                        (line /= nil)
                    }
                }
            }
        """, true)
        assert(out == "ab\nc\ndefg\n") { out }
    }

    @Test
    fun x21() {
        val out = all("""
            readFile |> splitLines |> printLines
        """, true)
        assert(out == ":yes") { out }
    }

    // TODO
    // usar operador de pipe
}
