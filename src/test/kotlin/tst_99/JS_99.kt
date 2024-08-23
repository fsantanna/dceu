package tst_99

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class JS_99 {

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
    fun x_01() {
        val out = test("""
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
    fun x_02() {
        val out = test("""
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
    fun x_03() {
        val out = test("""
            coro objectEntries (obj) {
                yield()
                loop kv in to-iter(obj) {
                    yield(kv)
                }
            }
            
            val jane = @[
                first = "Jane",
                last  = "Doe",
            ]
            val co = create-resume(objectEntries, jane)
            loop [k,v] in to.iter(co) {
                println((to.string(k) ++ ": ") ++ v)
            }
        """, true)
        assert(out == ":first: Jane\n:last: Doe\n") { out }
    }

    //  22.1.4 Use case: simpler asynchronous code

    @Test
    fun x_04() {
        val out = test("""
            ;;export [fetch, text, json] { ;; mock functions
                coro fetch (url) {
                    if url == :error {
                        error(:error)
                    }
                    url
                }
                coro text (url) {
                    to.string(url)
                }
                coro json (txt) {
                    "json " ++ txt
                }
            ;;}
            coro fetchJson (url) {
                val co_fetch = coroutine(fetch)
                val co_text  = coroutine(text)
                val co_json  = coroutine(json)
                val' req = resume-yield-all co_fetch(url)
                val' txt = resume-yield-all co_text(req)
                resume-yield-all co_json(txt)
            }
            spawn {
                val co1 = coroutine(fetchJson)
                val co2 = coroutine(fetchJson)
                resume-yield-all co1(:good) thus {
                    println(it)   ;; json :good
                }
                resume-yield-all co2(:error) thus {
                    println(it)   ;; never printed
                }
            }
        """, true)
        assert(out.contains("json :good\n" +
                " |  anon : (lin 33, col 14) : (spawn (task :nested () { (val co1 = corou...\n" +
                " |  anon : (lin 32, col 48) : (resume (ceu_co)(ceu_arg))\n" +
                " |  anon : (lin 22, col 48) : (resume (ceu_co)(ceu_arg))\n" +
                " |  anon : (lin 5, col 25) : error(:error)\n" +
                " v  error : :error\n")) { out }
    }

    // 22.3 Generators as iterators (data production)

    @Test
    fun x_05() {
        val out = test("""
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
    fun x_06() {
        val out = test("""
            coro genFunc() {
                yield('a')
                yield('b')
            }
            val arr = to.vector(coroutine(genFunc))
            println(arr)
            
            ;; val [x,y] = ...  ;; TODO: destructor
        """, true)
        assert(out == "ab\n") { out }
    }

    // 22.3.2 Returning from a generator

    // 22.3.3 Throwing an exception from a generator

    @Test
    fun x_07() {
        val out = test("""
            coro genFunc() {
                error(:problem)
            }
            val genObj = coroutine(genFunc)
            resume genObj()
                ;; anon : (lin 3, col 17) : throw error : uncaught exception
                ;; :problem
        """, true)
        assert(out == " |  anon : (lin 6, col 13) : (resume (genObj)())\n" +
                " |  anon : (lin 3, col 17) : error(:problem)\n" +
                " v  error : :problem\n") { out }
    }

    // 22.3.4 Example: iterating over properties
    // (same as) 22.1.3 Use case: implementing iterables

    //  22.3.5 You can only yield in generators

    @Test
    fun x_08() {
        val out = test("""
            coro genFunc () {
                func () {
                    yield() ;; anon : (lin 4, col 21) : yield error : unexpected enclosing func
                }()
            }
        """, true)
        assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing func\n") { out }
    }
    @Test
    fun x_09() {
        val out = test("""
            coro genFunc () {
                loop [v,i] in to.iter(#['a','b'],[:val,:idx]) {
                    yield(;;;drop;;;([i,v]))
                }
            }
            val arr = to.vector(coroutine(genFunc))
            println(arr)
        """, true)
        assert(out == "#[[0,a],[1,b]]\n") { out }
    }

    // 22.3.6 Recursion via yield*

    @Test
    fun x_10() {
        val out = test("""
            coro foo () {
                yield('a')
                yield('b')
            }
            coro bar () {
                yield('x')
                loop i in (coroutine(foo)) {
                    yield(i)
                }
                yield('y')
            }
            val arr = to.vector(coroutine(bar))
            println(arr)
        """, true)
        assert(out == "xaby\n") { out }
    }

    @Test
    fun x_11() {
        val out = test("""
            coro foo () {
                yield('a')
                yield('b')
            }
            coro bar () {
                yield('x')
                resume-yield-all coroutine(foo) ()
                yield('y')
            }
            val arr = to.vector(coroutine(bar))
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
    fun x_12() {
        val out = test("""
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
            println(to.vector(create-resume(logReturned, coroutine(genFuncWithReturn))))
        """, true)
        assert(out == "abc\n") { out }
    }

    // 22.3.6.2 Iterating over trees

    @Test
    fun x_13() {
        val out = test("""
            val TREE = @[
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
                    resume-yield-all (create-resume(T, tree.l)) ()
                }
                if tree.r {
                    resume-yield-all (create-resume(T, tree.r)) ()
                }
            }
            println(to.vector(create-resume(T, TREE)))
        """, true)
        assert(out == "abcde\n") { out }
    }

    // 22.4 Generators as observers (data consumption)

    // 22.4.1 Sending values via next()

    @Test
    fun x_14() {
        val out = test("""
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
    fun x_15() {
        val out = test("""
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
        val out = test("""
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
    fun x_17_scope() {
        val out = test("""
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
    fun todo_x_18() {
        val out = test("""
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
    fun x_19() {
        val out = test("""
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
                    loop c in to.iter(tmp) {
                        if c == '\n' {
                            resume target(drop(cur))
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
                    resume target((to.string(n) ++ ": ") ++ line)
                }
            }
            
            coro printLines () {
                loop {
                    val line = yield()
                    println(line)
                }
            }
            
            val co_print = create-resume(printLines)
            val co_nums  = create-resume(numberLines, co_print)
            val co_split = create-resume(splitLines, co_nums)
            readFile(nil, co_split) 
        """, true)
        assert(out == "1: ab\n2: c\n3: defg\n") { out }
    }

    @Test
    fun x_20() {
        val out = test("""
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
                    loop c in to.iter(tmp) {
                        if c == '\n' {
                            yield(drop(cur))
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
                    yield((to.string(n) ++ ": ") ++ line)
                }
            }

            coro printLines () {
                loop {
                    val line = yield()
                    println(line)
                }
            }
            
            val co_read  = coroutine(readFile)
            val co_split = create-resume(splitLines)
            val co_nums  = create-resume(numberLines)
            val co_print = create-resume(printLines)
            spawn {
                loop chars in to.iter(co_read) {
                    loop {
                        val line = if chars {
                            resume co_split(drop(chars))
                        }
                        if line {
                            loop {
                                val nums = if line {
                                    resume co_nums(drop(line))
                                }
                                until (nums == nil)
                                resume co_print(nums)
                            }
                        }
                        until (line == nil)
                    }
                }
            }
        """, true)
        assert(out == "1: ab\n2: c\n3: defg\n") { out }
    }

    @Test
    fun todo_x_21() {
        val out = test("""
            ;; f >|>> co   co >>|>> co
            ((readFile >|>> splitLines) >>|>> numLines) >>|>> printLines
        """, true)
        assert(out == ":yes") { out }
    }

    // 22.4.7 yield*: the full story
    // TODO: JS is inverted: callee has no while loop, caller has while loop

    @Test
    fun x_22() {
        val out = test("""
            coro callee () {
                loop {
                    val x = yield()
                    println(:callee, x)
                }
            }
            coro caller () {
                resume-yield-all coroutine(callee) ()
            }
            val co_caller = create-resume(caller)
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
    fun x_23() {
        val out = test("""
        coro Split (chars) {
            yield()
            var line = ""
            loop c in to.iter(chars) {
                if c == '\n' {
                    yield(drop(line))
                    set line = ""
                } else {
                    set line[+] = c
                }
            }
        }
        coro Number (lines) {
            yield()
            var i = 1
            loop l in to.iter(lines) {
                yield(to.string(i) ++ (": " ++ l))
                set i = i + 1
            }
        }
        coro Take (n, lines) {
            yield()
            var i = 0
            loop {
                until i == n
                yield(resume lines())
                set i = i + 1
            }
        }
        coro FS-Read (filename) {
            val buf = to.pointer(filename)
            val f = `:pointer fopen(${D}buf.Pointer, "r")`
            defer {
                `fclose(${D}f.Pointer);`
            }
            yield()
            loop {
                val c = `:char fgetc(${D}f.Pointer)`
                yield(c)
                until c == `:char EOF`
            }
        }
        do { ;; PULL
            val' read1   = create-resume(FS-Read, "build/prelude-0.ceu")
            val' split1  = create-resume(Split, read1)
            val' number1 = create-resume(Number, split1)
            val' take1   = create-resume(Take, 3, number1)
            loop l in to.iter(take1) {
                println(l)
            }
        }
        do { ;; PUSH
            val' read2   = create-resume(FS-Read, "build/prelude-x.ceu")
            val' split2  = create-resume(Split, read2)
            val' number2 = create-resume(Number, split2)
            val' take2   = create-resume(Take, 3, number2)
            coro Show () {
                var line = yield()
                loop {
                    until not line
                    println(line)
                    set line = yield()
                }
            }
            coro Send (co, nxt) {
                loop v in to-iter(co) {
                    resume nxt(drop(v))
                }
                nil
            }
            create-resume(Send, take2, create-resume(Show))
            nil
        }
        """, true)
        assert(out == "1: ;; is', is-not'\n" +
                "2: \n" +
                "3: val not = func (v) {\n" +
                "1: data :Clock = [ms]\n" +
                "2: \n" +
                "3: func {{+}} (v1, v2) {\n") { out }
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
