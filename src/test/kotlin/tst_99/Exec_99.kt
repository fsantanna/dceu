package tst_99

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_99 {
    // EMPTY IF / BLOCK

    @Test
    fun aa_01_if() {
        val out = test("""
            val x = if (true) { 1 }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_02_do() {
        val out = test("""
            println(do {})
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun aa_03_if() {
        val out = test("""
            var x
            set x = 10
            set x = if false { 1 }
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun aa_04_if() {
        val out = test("""
            var x
            set x = 10
            set x = if (nil) {} else { 1 }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_05_if() {
        val out = test("""
            println(if [] {})
        """)
        //assert(out == "anon : (lin 1, col 4) : if error : invalid condition\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun aa_06_if() {
        val out = test("""
            println(if false { true })
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun aa_07_func() {
        val out = test("""
            println(func () {} ())
        """)
        assert(out == "nil\n") { out }
    }

    // AS

    @Test
    fun ab_01_yield() {
        val out = test("""
            val x = if (true) { 1 }
            println(x)
        """)
        assert(out == "1\n") { out }
    }

    // OPS: not, and, or

    @Test
    fun bb_01_op_or_and() {
        val out = test("""
            println(true or println(1))
            println(false and println(1))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun bb_02_op_not() {
        val out = test("""
            println(not nil and not false)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_03_or_and() {
        val out = test("""
            println(1 or throw(5))
            println(1 and 2)
            println(nil and 2)
            println(nil or 2)
        """)
        assert(out == "1\n2\nnil\n2\n") { out }
    }
    @Test
    fun bb_03_or_and_no() {
        val out = test("""
            println(1 or throw(5))
            println(nil or 2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bb_03_or_and_ok() {
        val out = test("""
^[2,13]^[2,13]println(^[2,23](^[2,21]1 thus { ceu_6 =>
^[3,25](if ^[3,28]ceu_6 ^[3,34]{
^[4,29]ceu_6
} else ^[5,32]{
^[2,26]^[2,26]throw(^[2,32]5)
})
})
)
^[3,13]^[3,13]println(^[3,25](^[3,21]nil thus { ceu_41 =>
^[4,25](if ^[4,28]ceu_41 ^[4,35]{
^[5,29]ceu_41
} else ^[6,32]{
^[3,28]2
})
})
)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bb_04_or_and() {
        val out = test("""
            println(true and ([] or []))
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun bb_05_and_and() {
        val out = test("""
            val v = true and
                true and 10
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun bb_06_op_plus_plus() {
        val out = test("""
            $PLUS
            val v = 5 +
                5 + 10
            println(v)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun bb_07_ops() {
        val out = test("""
            println({{or}}(false, true))
            println({{not}}(true))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun bb_08_ops() {
        val out = test("""
            println({{not}}())
        """)
        assert(out == "anon : (lin 2, col 21) : operation error : invalid number of arguments\n") { out }
    }

    // is, is-not?, in?, in-not?

    @Test
    fun bc_01_is() {
        val out = test("""
            func to-bool (v) {
                not (not v)
            }
            func is' (v1,v2) {
                ifs {
                    (v1 == v2)         => true
                    (type(v2) /= :tag) => false
                    (type(v1) == v2)   => true
                    tags(v1,v2)        => true
                    else => false
                }
            }
            func is-not' (v1,v2) {
                not is'(v1,v2)
            }
            println([] is? :bool)
            println([] is? :tuple)
            println(1 is-not? :tuple)
            println(1 is-not? :number)
        """)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun bc_03_in() {
        val out = test("""
            $PLUS
            func to-bool (v) {
                not (not v)
            }
            func in' (v, xs) {
                var i = 0
                loop {
                    break(false) if i == #xs
                    break(true) if v == xs[i]
                    set i = i + 1
                }
            }            
            func in-not' (v, xs) {
                not in'(v,xs)
            }
            val t = [1,2,3]
            println(2 in? t)
            println(4 in? t)
            println(2 in-not? t)
            println(4 in-not? t)
        """)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }

    // FUNC / DCL

    @Test
    fun cc_01_func() {
        val out = test("""
            func f (v) {
                v
            }
            println(f(10))
        """)
        assert(out == "10\n") { out }
    }

    // IF / ID-TAG

    @Test
    fun cj_01_if() {
        val out = test("""
            val v = if x=1 { x }
            println(v)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cj_02_if() {
        val out = test("""
            data :X = [x]
            val i = if v:X=[1] { v.x }
            println(i)
        """)
        assert(out == "1\n") { out }
    }

    // IF cnd => t => f

    @Test
    fun dd_01_if() {
        val out = test("""
            println(if false => 1 => 2)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun dd_02_if() {
        val out = test("""
            println(if true => 1 => 2)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun dd_03_if() {
        val out = test("""
            println(if true => if true => 1 => 99 => 99)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun dd_04_if() {
        val out = test("""
            println(if x=10 => x => 99)
        """)
        assert(out == "10\n") { out }
    }

    // IFS

    @Test
    fun ff_01_ifs() {
        val out = test("""
            $PLUS
            func {{<}} () {}
            val x = ifs {
                10 < 1 => 99
                (5+5)==0 { 99 }
                else => 10
            }
            println(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ff_02_ifs() {
        val out = test("""
            val x = ifs { true=> `:number 1` }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ff_03_ifs() {
        val out = test("""
            val x = ifs 20 {
                == 10 => false
                == 20 => true
                else  => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_04_ifs() {
        val out = test("""
            var x = ifs it=20 {
                it == 10 => false
                true     => true
                it == 20 => false
                else     => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_05_ifs() {
        val out = test("""
            val x = ifs it=20 {
                it == 10 => false
                else     => true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }

    // AS / YIELD / CATCH / DETRACK / THUS

    @Test
    fun gg_01_yield() {
        val out = test("""
            val CO = coro () {
                yield() thus {
                    println(it)
                }
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            resume co1()
            resume co2()
            resume co1(1)
            resume co2(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun gg_02_yield() {
        val out = test("""
            val T = task (v) {
                yield()
                println(v)
            }
            val t1 = spawn T(1)
            val t2 = spawn T(2)
            broadcast(nil)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun gg_03_yield() {
        val out = test("""
            val CO = coro () {
                val x = yield()
                println(x)
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            resume co1()
            resume co2()
            resume co1(1)
            resume co2(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun gg_04_yield() {
        val out = test("""
            val CO = coro () {
                nil
                yield() thus { it => println(it);it }
                nil
                nil
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            do { do { do {
            resume co1()
            resume co2()
            resume co1([])
            resume co2([])
            }}}
        """)
        //assert(out == " |  anon : (lin 13, col 13) : resume (co1)([])\n" +
        //        " v  anon : (lin 5, col 17) : block escape error : cannot move pending reference in\n") { out }
        assert(out == "[]\n[]\n")
    }
    @Test
    fun gg_05_yield() {
        val out = test("""
            val CO = coro () {
                nil
                yield() thus {}
                nil
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            resume co1()
            resume co2()
            resume co1([])
            resume co2([])
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun gg_06_detrack() {
        val out = test("""
            val T = task () {
                set pub() = [10]
                yield(nil)
            }
            var t = spawn T ()
            var x = track(t)
            detrack(x) as { println(:1) }
            broadcast( nil )
            detrack(x) as { println(999) }
            println(:2)
        """)
        assert(out == ":1\n:2\n") { out }
    }

    // RESUME-YIELD-ALL

    @Test
    fun hh_01_yieldall() {
        val out = test("""
            coro foo () {
                yield('a')
                yield('b')
            }
            coro bar () {
                yield('x')
                resume-yield-all (coroutine(foo)) ()
                yield('y')
            }
            val co = coroutine(bar)
            loop {
                val v = resume co()
                break if status(co) == :terminated
                print(v)
            }
            println()
        """)
        assert(out == "xaby\n") { out }
    }
    @Test
    fun hh_02_yieldall() {
        val out = test("""
            $PLUS
            coro foo (x4) {
                val x6 = yield(x4+1)
                val x8 = yield(x6+1)
                nil
            }
            coro bar (x1) {
                val x3 = yield(x1+1)
                val x8 = resume-yield-all coroutine(foo) (x3+1)
                val x10 = yield(x8+1)
                nil
            }
            val co = coroutine(bar)
            val x2 = resume co(1)
            ;;println(:x2, x2)
            val x5 = resume co(x2+1)
            ;;println(:x5, x5)
            val x7 = resume co(x5+1)
            ;;println(:x7, x7)
            val x9 = resume co(x7+1)
            ;;println(:x9, x9)
            val xN = resume co(x9+1)
            ;;println(:xN, xN)
            println(x2, x5, x7, x9, xN)
        """)
        assert(out == "2\t5\t7\t9\tnil\n") { out }
    }
    @Test
    fun hh_03_yieldall() {
        val out = test("""
            coro foo () {
                yield("a")
                yield("b")
            }
            coro bar () {
                yield("x")
                resume-yield-all (coroutine(foo)) ()
                yield("y")
            }
            val co = coroutine(bar)
            loop {
                val v = resume co()
                break if status(co) == :terminated
                print(v)
            }
            println()
        """)
        assert(out == "xaby\n") { out }
    }

    // SPAWN

    @Test
    fun ii_01_spawn_task() {
        val out = test("""
            spawn task {
                println(1)
                yield()
                println(3)
            }
            println(2)
            broadcast(nil)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun todo_ii_02_spawn_coro() {
        val out = test("""
            val co = spawn (coro () {
                println(1)
                yield()
                println(3)
            }) ()
            println(2)
            resume co()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun todo_ii_03_spawn_coro() {
        val out = test("""
            val co = spawn coro {
                println(1)
                yield()
                println(3)
            }
            println(2)
            resume co()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun ii_04_spawn() {
        val out = test("""
            spawn task {
                spawn task {
                    println(1)
                }
                nil
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_ii_05_coro() {
        val out = test("""
            val co = spawn coro {
                println(1)
                val v = yield()
                println(v)
            }
            resume co(10)
        """)
        assert(out == "1\n10\n") { out }
    }
    @Test
    fun ii_06_spawn_defer() {
        val out = test("""
            spawn task {
                do {
                    val t1 = spawn task {
                        ${AWAIT()}
                        println(1)
                    }
                    spawn task {
                        defer { println(3) }
                        ${AWAIT()}
                        println(2)
                    }
                    ${AWAIT("it==t1")}
                    nil
                }
                println(:ok)
            }
            broadcast( nil)
        """)
        assert(out == "1\n3\n:ok\n") { out }
    }
    @Test
    fun ii_07_spawn() {
        val out = test("""
            spawn task {
                spawn task {
                    yield ()
                    println(1)
                }
                yield ()
                println(2)
            }
            broadcast (nil)
        """)
        assert(out == "1\n2\n") { out }
    }

    // PAR / PAR-AND / PAR-OR

    @Test
    fun jj_01_par_err() {
        val out = test("""
            par {
                println(1)
            } with {
                println(2)
            }
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing coro or task\n") { out }
    }
    @Test
    fun jj_02_par() {
        val out = test("""
            spawn task {
                par {
                    println(1)
                } with {
                    println(2)
                }
                println(999)
            }
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun jj_03_paror() {
        val out = test("""
            spawn task {
                par-or {
                    yield()
                    yield()
                    println(1)
                } with {
                    println(2)
                } with {
                    yield()
                    yield()
                    println(3)
                }
                println(:ok)
            }
        """)
        assert(out == "2\n:ok\n") { out }
    }
    @Test
    fun jj_04_paror() {
        val out = test("""
            spawn task {
                val v =
                    par-or {
                        1
                    } with {
                        2
                    }
                println(v)
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_05_parand() {
        val out = test("""
            spawn task {
                par-and {
                    println(1)
                } with {
                    println(2)
                }
                println(:ok)
            }
        """)
        assert(out == "1\n2\n:ok\n") { out }
    }
    @Test
    fun jj_06_parand() {
        val out = test("""
            spawn task {
                val v =
                    par-and {
                        1
                    } with {
                        2
                    }
                println(v)
            }
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun jj_07_paror() {
        val out = test("""
            spawn task {
                par-or {
                    yield()
                    yield()
                    yield()
                    println(1)
                } with {
                    yield()
                    println(2)
                } with {
                    yield()
                    yield()
                    println(3)
                }
                println(:ok)
            }
            broadcast(nil)
        """)
        assert(out == "2\n:ok\n") { out }
    }
    @Test
    fun jj_08_parand() {
        val out = test("""
            spawn task {
                par-and {
                    yield()
                    yield()
                    yield()
                    println(1)
                } with {
                    yield()
                    println(2)
                } with {
                    yield()
                    yield()
                    println(3)
                }
                println(:ok)
            }
            broadcast(nil)
            broadcast(nil)
            broadcast(nil)
        """)
        assert(out == "2\n3\n1\n:ok\n") { out }
    }
    @Test
    fun jj_09_paror_defer() {
        val out = test("""
            spawn task {
                par-or {
                    ${AWAIT()}
                    println(1)
                } with {
                    defer { println(3) }
                    ${AWAIT()}
                    println(2)
                }
                println(:ok)
            }
            broadcast(nil)
        """)
        assert(out == "1\n3\n:ok\n") { out }
    }
    @Test
    fun jj_10_paror_defer() {
        val out = test("""
            spawn task {
                par-or {
                    defer { println(3) }
                    ${AWAIT()}
                    println(999)
                } with {
                    println(2)
                }
                println(:ok)
            }
            broadcast (nil)
        """)
        assert(out == "2\n3\n:ok\n") { out }
    }
    @Test
    fun jj_11_paror_defer() {
        val out = test("""
            spawn task {
                par-or {
                    defer { println(1) }
                    ${AWAIT()}
                    ${AWAIT()}
                    println(999)
                } with {
                    ${AWAIT()}
                    println(2)
                } with {
                    defer { println(3) }
                    ${AWAIT()}
                    ${AWAIT()}
                    println(999)
                }
                println(999)
            }
            broadcast (nil)
        """)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun jj_11_parand_defer() {
        val out = test("""
            spawn task {
                par-and {
                    yield()
                    println(1)
                } with {
                    println(2)
                } with {
                    yield()
                    println(3)
                }
                println(:ok)
            }
            broadcast (nil)
        """)
        assert(out == "2\n1\n3\n:ok\n") { out }
    }
    @Test
    fun jj_12_parand_defer() {
        val out = test("""
            spawn task {
                par-and {
                    defer { println(1) }
                    ${AWAIT()}
                    ${AWAIT()}
                    println(1)
                } with {
                    ${AWAIT()}
                    println(2)
                } with {
                    defer { println(3) }
                    ${AWAIT()}
                    ${AWAIT()}
                    println(3)
                }
                println(:ok)
            }
            broadcast (nil)
            broadcast (nil)
        """)
        assert(out == "2\n1\n1\n3\n3\n:ok\n") { out }
    }

    // AWAIT

    @Test
    fun kk_01_await() {
        val out = test("""
            $IS
            task T () {
                await() as { it => it is? :x }
                println(1)
            }
            spawn T()
            broadcast (tags([],:x,true))
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun kk_02_await() {
        val out = test("""
            $IS
            spawn task {
                println(0)
                await as { (it/=nil) and (it[:type]==:x) }
                println(99)
            }
            do {
                println(1)
                broadcast (@[(:type,:y)])
                println(2)
                broadcast (@[(:type,:x)])
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun kk_03_await() {
        val out = test("""
            $IS
            spawn task {
                println(0)
                await(:x)
                println(99)
            }
            do {
                println(1)
                broadcast (tags([], :y, true))
                println(2)
                broadcast (tags([], :x, true))
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun kk_04_await() {
        val out = test("""
            $IS
            spawn task {
                println(0)
                await(:x)
                println(99)
            }
            do {
                println(1)
                broadcast (tags([], :y, true))
                println(2)
                broadcast (tags([], :x, true))
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun kk_05_await() {
        val out = test("""
            val f
            await()
        """)
        assert(out == "anon : (lin 3, col 13) : yield error : expected enclosing coro or task\n") { out }
    }
    @Test
    fun kk_06_await() {
        val out = test("""
            spawn task {
                loop {
                    await as {
                        println(it)
                    }
                }
            }
            broadcast (@[])
        """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun kk_07_await() {
        val out = test("""
            spawn task {
                await as {
                    println(it)
                }
                await as {
                    println(it)
                }
            }
            broadcast (:1)
            broadcast (:2)
        """)
        assert(out == ":1\n:2\n") { out }
    }

    // AWAIT / TASK

    @Test
    fun kl_01_await_task() {
        val out = test("""
            task T (v) {
                [v]
            }
            spawn task {
                val v = await spawn T(1)
                println(v)
            }
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun kl_02_await_task() {
        val out = test("""
            spawn task {
                task T () {
                    val v = await()
                    [v]
                }
                spawn task {
                    val v = await spawn T(1)
                    println(v)
                }
                broadcast(2)
            }
        """)
        assert(out == "[2]\n") { out }
    }

    // WATCHING

    @Test
    fun ll_01_watching() {
        val out = test("""
            spawn task {
                watching 1 {
                    defer { println(2) }
                    yield()
                    println(1)
                }
                println(:ok)
            }
            broadcast (nil)
            println(0)
            broadcast (1)
        """)
        assert(out == "0\n1\n2\n:ok\n") { out }
    }
}
