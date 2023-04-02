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
    //      - Ceu: coro genFunc (...) { ... }
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
            coro genFunc () {
                println("First")
                yield()
                println("Second")
            }
            val genObj = coroutine(genFunc)
            resume genObj()     ;; First
            resume genObj()     ;; Second
        """)
        assert(out == "First\nSecond\n") { out }
    }

    // 22.1.2 Kinds of generators

    @Test
    fun x2() {
        val out = all("""
            coro gen1 (v) { println(v) }
            val co1 = coroutine(gen1)
            resume co1(1)

            val gen2 = coro (v) { println(v) }
            val co2 = coroutine(gen1)
            resume co2(2)
            
            val obj3 = @[
                gen = coro (v) { println(v) }
            ]
            val co3 = coroutine(obj3.gen)
            resume co3(3)
            
            ;; no co4
        """)
        assert(out == "1\n2\n3\n") { out }
    }

    // 22.1.3 Use case: implementing iterables

    @Test
    fun x3() {
        val out = all("""
            coro objectEntries (obj) {
                yield()
                loop in iter(obj,:all), v {
                    yield([v.0, v.1])
                }
            }
            
            val jane = @[
                first = "Jane",
                last  = "Doe",
            ]
            val co = spawn objectEntries(jane)
            loop in iter(co), v {
                println((to-string(v.0) ++ ": ") ++ v.1)
            }
        """, true)
        assert(out == ":first: Jane\n:last: Doe\n") { out }
    }

    //  22.1.4 Use case: simpler asynchronous code

    @Test
    fun x4() {
        val out = all("""
            do :unnest { ;; mock functions
                coro fetch (url) {
                    if url == :error {
                        throw(:error)
                    }
                    url
                }
                coro text (url) {
                    to-string(url)
                }
                coro json (txt) {
                    "json " ++ txt
                }
            }
            coro fetchJson (url) {
                val req = await spawn fetch(url)
                val txt = await spawn text(req)
                await spawn json(txt)
            }
            spawn {
                val obj1 = await spawn fetchJson(:good)
                println(obj1)   ;; json :good
                val obj2 = await spawn fetchJson(:error)
                println(obj2)   ;; never printed
            }
        """, true)
        assert(out.contains("uncaught exception\njson :good")) { out }
    }

    // 22.3 Generators as iterators (data production)

    @Test
    fun x5() {
        val out = all("""
            coro genFunc() {
                yield('a')
                yield('b')
            }
            val genObj = coroutine(genFunc)
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
            coro genFunc() {
                yield('a')
                yield('b')
            }
            val arr = to-vector(coroutine(genFunc))
            println(arr)
            
            ;; val [x,y] = ...  ;; TODO: destructor
        """, true)
        assert(out == "ab\n") { out }
    }

    // 22.3.2 Returning from a generator

    // 22.3.3 Throwing an exception from a generator

    @Test
    fun x7() {
        val out = all("""
            coro genFunc() {
                throw(:problem)
            }
            val genObj = coroutine(genFunc)
            resume genObj()
                ;; anon : (lin 3, col 17) : throw error : uncaught exception
                ;; :problem
        """, true)
        assert(out == "anon : (lin 6, col 20) : genObj()\n" +
                "anon : (lin 3, col 17) : throw(:problem)\n" +
                "throw error : uncaught exception\n" +
                ":problem\n") { out }
    }

    // 22.3.4 Example: iterating over properties
    // (same as) 22.1.3 Use case: implementing iterables

    //  22.3.5 You can only yield in generators

    @Test
    fun x8() {
        val out = all("""
            coro genFunc () {
                func () {
                    yield() ;; anon : (lin 4, col 21) : yield error : expected enclosing coro or task
                }()
            }
        """, true)
        assert(out == "anon : (lin 4, col 21) : yield error : expected enclosing coro or task") { out }
    }
    @Test
    fun x9() {
        val out = all("""
            coro genFunc () {
                loop in iter(#['a','b'],:all), v {
                    yield(move(v))
                }
            }
            val arr = to-vector(coroutine(genFunc))
            println(arr)
        """, true)
        assert(out == "#[[0,a],[1,b]]\n") { out }
    }

    // 22.3.6 Recursion via yield*

    @Test
    fun x10() {
        val out = all("""
            coro foo () {
                yield('a')
                yield('b')
            }
            coro bar () {
                yield('x')
                loop in iter(coroutine(foo)), i {
                    yield(i)
                }
                yield('y')
            }
            val arr = to-vector(coroutine(bar))
            println(arr)
        """, true)
        assert(out == "xaby\n") { out }
    }

    @Test
    fun x11() {
        val out = all("""
            coro foo () {
                yield('a')
                yield('b')
            }
            coro bar () {
                yield('x')
                resume-yield-all coroutine(foo) ()
                yield('y')
            }
            val arr = to-vector(coroutine(bar))
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
            coro genFuncWithReturn () {
                yield('a')
                yield('b')
                yield('c')
                nil
            }
            coro logReturned (genObj) {
                yield()
                resume-yield-all genObj ()
            }
            println(to-vector(spawn logReturned(coroutine(genFuncWithReturn))))
        """, true)
        assert(out == "abc\n") { out }
    }

    // 22.3.6.2 Iterating over trees

    @Test
    fun x13() {
        val out = all("""
            val tree = @[
                v = 'a',
                l = @[
                    v = 'b',
                    l = @[v = 'c'],
                    r = @[v = 'd'],
                ],
                r = @[v = 'e']
            ]
            coro T (tree) {
                yield()
                yield(tree.v)
                if tree.l {
                    resume-yield-all (spawn T(tree.l)) ()
                }
                if tree.r {
                    resume-yield-all (spawn T(tree.r)) ()
                }
            }
            println(to-vector(spawn T(tree)))
        """, true)
        assert(out == "abcde\n") { out }
    }

    // 22.4 Generators as observers (data consumption)

    // 22.4.1 Sending values via next()

    @Test
    fun x14() {
        val out = all("""
            coro dataConsumer () {
                println(:started)
                println(1, yield()) ;; (A)
                println(2, yield())
                :result
            }
            
            val genObj = coroutine(dataConsumer)
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
            coro gen (input) {
                println(input)
                loop {
                    val input' = yield() ;; (B)
                    println(input')
                }
            }
            val obj = coroutine(gen);
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
            coro genFunc1() {
                defer {
                    println(:exiting)
                }
                yield () ;; (A)
            }
            val genObj1 = coroutine(genFunc1)
            resume genObj1()
            kill genObj1()
            println(:end)
        """, true)
        assert(out == ":exiting\n:end\n") { out }
    }

    @Test
    fun x17_scope() {
        val out = all("""
            coro genFunc1() {
                defer {
                    println(:exiting)
                }
                yield () ;; (A)
            }
            do {
                val genObj1 = coroutine(genFunc1)
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
            coro genFunc() {}
            val genObj = coroutine(genFunc)
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
    // See also: https://github.com/fsantanna/uv-ceu/blob/master/ceu/04-fs-lines-push.ceu

    @Test
    fun x19() {
        val out = all("""
            func readFile (fileName, target) {
                ;; TODO: from fileName
                resume target("ab\nc")
                resume target("")
                resume target("\ndefg\n")
            }
            
            coro splitLines (target) {
                var cur = ""
                loop {
                    val tmp = yield()
                    loop in iter(tmp,:val), c {
                        if c == '\n' {
                            resume target(cur)
                            set cur = ""
                        } else {
                            set cur[+] = c
                        }
                    }
                }
            }
            
            coro numberLines (target) {
                var n = 0
                loop {
                    val line = yield()
                    set n = n + 1
                    resume target((to-string(n) ++ ": ") ++ line)
                }
            }
            
            coro printLines () {
                loop {
                    val line = yield()
                    println(line)
                }
            }
            
            val co_print = spawn printLines()
            val co_nums  = spawn numberLines(co_print)
            val co_split = spawn splitLines(co_nums)
            readFile(nil, co_split) 
        """, true)
        assert(out == "1: ab\n2: c\n3: defg\n") { out }
    }

    @Test
    fun x20() {
        val out = all("""
            coro readFile (fileName) {
                ;; TODO: from fileName
                yield("ab\nc")
                yield("")
                yield("\ndefg\n")
            }
            
            coro splitLines () {
                var cur = ""
                loop {
                    val tmp = yield(nil)
                    loop in iter(tmp), c {
                        if c == '\n' {
                            yield(move(cur))
                            set cur = ""
                        } else {
                            set cur[+] = c
                        }
                    }
                }
            }
            
            coro numberLines () {
                var n = 0
                loop {
                    val line = yield(nil)
                    set n = n + 1
                    yield((to-string(n) ++ ": ") ++ line)
                }
            }

            coro printLines () {
                loop {
                    val line = yield()
                    println(line)
                }
            }
            
            val co_read  = coroutine(readFile)
            val co_split = spawn splitLines()
            val co_nums  = spawn numberLines()
            val co_print = spawn printLines()
            spawn {
                loop in iter(co_read), chars {
                    loop {
                        val line = if chars {
                            resume co_split(chars)
                        }
                        if line {
                            loop {
                                val nums = if line {
                                    resume co_nums(line)
                                }
                            } until (nums == nil) {
                                resume co_print(nums)
                            }
                        }
                    } until
                        (line == nil)
                }
            }
        """, true)
        assert(out == "1: ab\n2: c\n3: defg\n") { out }
    }

    @Test
    fun todo_x21() {
        val out = all("""
            ;; f >|>> co   co >>|>> co
            ((readFile >|>> splitLines) >>|>> numLines) >>|>> printLines
        """, true)
        assert(out == ":yes") { out }
    }

    // 22.4.7 yield*: the full story
    // TODO: JS is inverted: callee has no while loop, caller has while loop

    @Test
    fun x22() {
        val out = all("""
            coro callee () {
                loop {
                    val x = yield()
                    println(:callee, x)
                }
            }
            coro caller () {
                resume-yield-all coroutine(callee) ()
            }
            val co_caller = spawn caller ()
            println(:resume, resume co_caller('a'))
            println(:resume, resume co_caller('b'))
        """, true)
        assert(out == ":callee\ta\n:resume\tnil\n:callee\tb\n:resume\tnil\n") { out }
    }

    // 22.5 Generators as tasks (cooperative multitasking)
    // 22.5.1 The full generator interface
    // 22.5.2 Cooperative multitasking

    // 22.5.2.1 Simplifying asynchronous computations via generators
    // TODO: uv, The following task reads the texts of two files, parses the JSON inside them and logs the result.

    // 22.5.3 The limitations of cooperative multitasking via generators
    // 22.5.3.1 The benefits of the limitations of generators

    // 22.6 Examples of generators
    // 22.6.1 Implementing iterables via generators
    // 22.6.1.1 The iterable combinator take()
    // 22.6.1.2 Infinite iterables
    // 22.6.1.3 Array-inspired iterable combinators: map, filter
    // 22.6.1.3.1 A generalized map()
    // 22.6.1.3.2 A generalized filter()
    // TODO: need interfaces now

    // TODO: all, uv
    // 22.6.2 Generators for lazy evaluation
    // 22.6.2.1 Lazy pull (generators as iterators)
    //  - addNumbers(extractNumbers(tokenize(CHARS)))
    // See also: https://github.com/fsantanna/uv-ceu/blob/master/ceu/05-fs-lines-pull.ceu
    // 22.6.2.1.1 Step 1 – tokenizing
    // 22.6.2.1.2 Step 2 – extracting numbers
    // 22.6.2.1.3 Step 3 – adding numbers
    // 22.6.2.1.4 Pulling the output

    // 22.6.2.2 Lazy push (generators as observables)
    // See also: https://github.com/fsantanna/uv-ceu/blob/master/ceu/05-fs-lines-pull.ceu
    @Test
    fun x23() {
        val out = all("""
        coro Split (chars) {
            yield()
            var line = ""
            loop in iter(chars), c {
                if c == '\n' {
                    yield(move(line))
                    set line = ""
                } else {
                    set line[+] = c
                }
            }
        }
        coro Number (lines) {
            yield()
            var i = 1
            loop in iter(lines), l {
                yield(to-string(i) ++ (": " ++ l))
                set i = i + 1
            }
        }
        coro Take (n, lines) {
            yield()
            var i = 0
            loop until i == n {
                yield(resume lines())
                set i = i + 1
            }
        }
        coro FS-Read (filename) {
            val f = `:pointer fopen(${D}filename.Dyn->Ncast.Vector.buf, "r")`
            defer {
                `fclose(${D}f.Pointer);`
            }
            yield()
            loop {
                val c = `:char fgetc(${D}f.Pointer)`
                yield(c)
            } until
                c == `:char EOF`
        }
        do { ;; PULL
            val read1   = spawn FS-Read("build/prelude.ceu")
            val split1  = spawn Split(read1)
            val number1 = spawn Number(split1)
            val take1   = spawn Take(3, number1)
            loop in iter(take1), l {
                println(l)
            }
        }
        do { ;; PUSH
            val read2   = spawn FS-Read("build/prelude.ceu")
            val split2  = spawn Split(read2)
            val number2 = spawn Number(split2)
            val take2   = spawn Take(3, number2)
            coro Show () {
                var line = yield()
                loop until not line {
                    println(line)
                    set line = yield()
                }
            }
            coro Send (co, next) {
                loop in iter(co), v {
                    resume next(v)
                }
            }
            spawn Send(take2, spawn Show())
            nil
        }
        """, true)
        assert(out == "1: ;; is', is-not'\n" +
                "2: \n" +
                "3: func is' (v1,v2) {\n" +
                "1: ;; is', is-not'\n" +
                "2: \n" +
                "3: func is' (v1,v2) {\n") { out }
    }

    // 22.6.2.2.1 Step 1 – tokenize
    // 22.6.2.2.2 Step 2 – extract numbers
    // 22.6.2.2.3 Step 3 – add numbers
    // 22.6.2.2.4 Pushing the input
    //  - tokenize(extractNumbers(addNumbers(logItems())))

    // 22.6.3 Cooperative multi-tasking via generators
    // 22.6.3.1 Pausing long-running tasks
    // 22.6.3.2 Cooperative multitasking with generators and Node.js-style callbacks
    // 22.6.3.3 Communicating Sequential Processes (CSP)

    // 22.7 Inheritance within the iteration API (including generators)
    // ...
}
