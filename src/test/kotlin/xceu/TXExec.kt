package xceu

import ceu.all
import ceu.await
import ceu.lexer
import ceu.yield
import org.junit.Ignore
import org.junit.Test

class TXExec {

    // EMPTY IF

    @Test
    fun aa_if1() {
        val out = all("""
            val x = if (true) { 1 }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_if2() {
        val out = all("""
            var x
            set x = 10
            set x = if false { 1 }
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun aa_if3() {
        val out = all("""
            var x
            set x = 10
            set x = if (nil) {} else { 1 }
            println(x)
        """.trimIndent())
        //assert(out == "anon : (lin 3, col 13) : if error : invalid condition\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_if4_err() {
        val out = all("""
            println(if [] {})
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 4) : if error : invalid condition\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun aa_if5() {
        val out = all("""
            println(if false { true })
        """.trimIndent())
        assert(out == "nil\n") { out }
    }

    // IF cnd -> t -> f

    @Test
    fun aa_if6() {
        val out = all("""
            println(if false -> 1 -> 2)
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun aa_if7() {
        val out = all("""
            println(if true -> 1 -> 2)
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_if8_err() {
        val out = all("""
            println(if true -> 1)
        """.trimIndent())
        assert(out == "anon : (lin 1, col 21) : expected \"->\" : have \")\"") { out }
    }
    @Test
    fun aa_if9() {
        val out = all("""
            println(if true -> if true -> 1 -> 99 -> 99)
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_if10() {
        val out = all("""
            println(if x=10 -> x -> 99)
        """.trimIndent())
        assert(out == "10\n") { out }
    }

    // IFS

    @Test
    fun bb_ifs1() {
        val out = all("""
            val x = ifs {
                10 < 1 -> 99
                (5+5)==0 { 99 }
                else -> 10
            }
            println(x)
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun bb_ifs2() {
        val out = all("""
            val x = ifs { true -> `:number 1` }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_ifs3() {
        val out = all("""
            val x = ifs 20 {
                == 10 -> false
                == 20 -> true
                else  -> false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_ifs4() {
        val out = all("""
            var x = ifs it=20 {
                it == 10 -> false
                true  -> true
                it == 20 -> false
                else  -> false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_ifs5() {
        val out = all("""
            val x = ifs it=20 {
                it == 10 -> false
                else -> true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun todo_bb_ifs6_nocnd() {
        val out = all("""
            val x = ifs 20 {
                true -> ifs {
                    == 20 -> true   ;; err: no ifs expr
                }
            }
            println(x)
        """)
        assert(out == "ERROR\n") { out }
    }
    @Test
    fun bb_ifs7() {
        val out = all("""
            var x = ifs it=20 {
                it is? 10 -> false
                true  -> true
                it is? 20 -> false
                else  -> false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_ifs8() {
        val out = all("""
            data :T = []
            val x = ifs 10 {
                true -> :T []
                is? 0 -> nil
            }
            println(x)
        """)
        assert(out == "anon : (lin 5, col 21) : access error : variable \"is'\" is not declared") { out }
    }
    @Test
    fun bb_ifs9() {
        val out = all("""
            var x = ifs it=20 {
                it in? [1,20,1] -> true
                else  -> false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_ifs10() {
        val out = all("""
            val x = ifs it=[] {
                true -> it
            }
            println(x)
        """, true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun bb_11_ifs () {
        val out = all("""
            val x = ifs {
                true -> it
            }
            println(x)
        """, true)
        assert(out == "anon : (lin 3, col 25) : access error : variable \"it\" is not declared") { out }
    }
    @Test
    fun bb_12_ifs () {
        val out = all("""
            val x = ifs {
                v=10 -> v
            }
            println(x)
        """)
        assert(out == "10\n") { out }
    }

    // OPS: not, and, or

    @Test
    fun op_or_and() {
        val out = all("""
            println(true or println(1))
            println(false and println(1))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun op_not() {
        val out = all("""
            println(not nil and not false)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun op2_or_and() {
        val out = all("""
            println(1 or throw(5))
            println(1 and 2)
            println(nil and 2)
            println(nil or 2)
        """)
        assert(out == "1\n2\nnil\n2\n") { out }
    }
    @Test
    fun op3_or_and() {
        val out = all("""
            println(true and ([] or []))
        """)
        assert(out == "[]\n") { out }
    }

    // is, is-not?, in?

    @Test
    fun is1() {
        val out = all("""
            println([] is? :bool)
            println([] is? :tuple)
            println(1 is-not? :tuple)
            println(1 is-not? :number)
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun is2() {
        val out = all("""
            val t = []
            tags(t,:x,true)
            println(t is? :x)
            tags(t,:y,true)
            println(t is-not? :y)
            tags(t,:x,false)
            println(t is-not? :x)
        """, true)
        assert(out == "true\nfalse\ntrue\n") { out }
    }
    @Test
    fun in3() {
        val out = all("""
            val t = [1,2,3]
            println(2 in? t)
            println(4 in? t)
        """, true)
        assert(out == "true\nfalse\n") { out }
    }

    // ==, ===, /=, =/=

    @Test
    fun ii_00_op_eqeqeq_tup() {
        val out = ceu.all(
            """
            println([1] === [1])
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun ii_01_op_eqeqeq_tup() {
        val out = ceu.all(
            """
            println([1] === [1])
            println([ ] === [1])
            println([1] =/= [1])
            println([1,[],[1,2,3]] === [1,[],[1,2,3]])
        """, true)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }
    @Test
    fun ii_02_op_eqeqeq_tup() {
        val out = ceu.all(
            """
            println([1,[1],1] === [1,[1],1])
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun ii_03_op_eqs_dic() {
        val out = ceu.all(
            """
            println(@[] ==  @[])
            println(@[] === @[])
            println(@[] /=  @[])
            println(@[] =/= @[])
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun ii_04_op_eqs_vec() {
        val out = ceu.all(
            """
            println(#[]  ==  #[])
            println(#[1] === #[1])
            println(#[1] /=  #[1])
            println(#[]  =/= #[])
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun ii_05_op_eqs_vec_dic_tup() {
        val out = ceu.all(
            """
            println(@[(:y,false)] === @[(:x,true)])
        """, true)
        assert(out == "false\n") { out }
    }
    @Test
    fun ii_06_op_eqs_vec_dic_tup() {
        val out = ceu.all(
            """
            println([#[],@[]] ==  [#[],@[]])
            println([#[],@[]] /=  [#[],@[]])
            println([#[1],@[(:y,false),(:x,true)]] === [#[1],@[(:x,true),(:y,false)]])
            println([#[],@[]] =/= [#[],@[]])
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun ii_07_valgrind() {
        val out = ceu.all(
            """
            val f = func (v) {
                do {
                    do {
                        do {
                            val x
                            println(x)
                            do {
                                nil
                            }
                            val y = x[0]
                        }
                    }
                }
            }
            do {
                f(@[(:y,false)])
            }
        """)
        assert(out == "anon : (lin 17, col 17) : f(@[(:y,false)])\n" +
                "anon : (lin 11, col 37) : index error : expected collection\n" +
                "nil\n" +
                ":error\n") { out }
    }
    @Test
    fun ii_08_xxx() {
        val out = ceu.all(
            """
            println([@[]] === [@[]])
        """, true)
        assert(out == "true\n") { out }
    }

    // assert

    @Test
    fun assert1() {
        val out = all("""
            catch :assert {
                assert([] is? :bool, "ok")
            }
            assert(1 is-not? :number)
        """, true)
        assert(out.contains("assertion error : ok\n" +
                "assertion error : no reason given\n")) { out }
    }

    // YIELD

    @Test
    fun bcast1() {
        val out = all("""
            val tk = task () {
                yield()
                println(evt)
                do { var ok; set ok=true; loop until not ok { yield(nil); if (evt is-not? :x-task) { set ok=false } else { nil } } }
                ;;yield()
                println(evt)                
            }
            val co1 = spawn(tk)()
            val co2 = spawn(tk)()
            broadcast 1
            broadcast in :global, 2
            broadcast 3
        """, true)
        assert(out == "1\n1\n2\n2\n") { out }
    }
    @Test
    fun bcast2() {
        val out = all("""
            task T () {
                await evt is? :x
                println(1)
            }
            spawn T()
            broadcast tags([],:x,true)
            println(2)
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bcast3() {
        val out = all("""
            do {
                val :tmp e = []
                broadcast e
            }
            do {
                val e = []
                broadcast e
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // YIELD ALL

    @Test
    fun kk_01_yieldall() {
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
            println(to-vector(coroutine(bar)))
        """, true)
        assert(out == "xaby\n") { out }
    }
    @Test
    fun kk_02_yieldall() {
        val out = all("""
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
        """, true)
        assert(out == "2\t5\t7\t9\tnil\n") { out }
    }


    // SPAWN, PAR

    @Test
    fun par1() {
        val out = all("""
            spawn task () {
                par {
                    do { var ok1; set ok1=true; loop until not ok1 { yield(nil); if type(evt)/=:x-task { set ok1=false } else { nil } } }
                    ;;yield()
                    do { var ok2; set ok2=true; loop until not ok2 { yield(nil); if type(evt)/=:x-task { set ok2=false } else { nil } } }
                    ;;yield()
                    println(1)
                } with {
                    do { var ok3; set ok3=true; loop until not ok3 { yield(nil); if type(evt)/=:x-task { set ok3=false } else { nil } } }
                    ;;yield()
                    println(2)
                } with {
                    println(3)
                }
            } ()
            broadcast in :global, nil
        """, true)
        assert(out == "3\n2\n") { out }
    }
    @Test
    fun spawn2() {
        val out = all("""
            spawn {
                println(1)
                yield()
                println(3)
            }
            println(2)
            broadcast in :global, nil
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun spawn3() {
        val out = all("""
            spawn {
                spawn {
                    println(1)
                }
                nil
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun spawn4() {
        val out = all("""
            task T () {}
            (spawn in ts, T()) where {
            }
        """)
        assert(out == "anon : (lin 3, col 23) : access error : variable \"ts\" is not declared") { out }
    }
    @Test
    fun spawn5_coro() {
        val out = all("""
            val co = spawn coro {
                println(1)
                val v = yield()
                println(v)
            }
            resume co(10)
        """)
        assert(out == "1\n10\n") { out }
    }

    // PARAND / PAROR / WATCHING

    @Test
    fun paror1() {
        val out = all("""
            spawn task () {
                par-or {
                    ${yield()}
                    println(1)
                } with {
                    println(2)
                } with {
                    ${yield()}
                    println(3)
                }
                println(999)
            } ()
        """, true)
        assert(out == "2\n999\n") { out }
    }
    @Test
    fun paror1a() {
        val out = all("""
            spawn task () {
                par-or {
                    ${yield()}
                    println(1)
                } with {
                    defer { println(3) }
                    ${yield()}
                    println(2)
                }
                println(999)
            } ()
            broadcast in :global, nil
        """, true)
        assert(out == "1\n3\n999\n") { out }
    }
    @Test
    fun paror1b() {
        val out = all("""
            spawn task () {
                par-or {
                    defer { println(3) }
                    ${yield()}
                    println(999)
                } with {
                    println(2)
                }
                println(:ok)
            } ()
            broadcast in :global, nil
        """, true)
        assert(out == "2\n3\n:ok\n") { out }
    }
    @Test
    fun paror2() {
        val out = all("""
            spawn task () {
                par-or {
                    defer { println(1) }
                    ${yield("ok1")}
                    ${yield("ok2")}
                    println(999)
                } with {
                    ${yield()}
                    println(2)
                } with {
                    defer { println(3) }
                    ${yield("ok1")}
                    ${yield("ok2")}
                    println(999)
                }
                println(999)
            } ()
            broadcast in :global, nil
        """, true)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun parand3() {
        val out = all("""
            spawn task () {
                par-and {
                    yield()
                    println(1)
                } with {
                    println(2)
                } with {
                    yield()
                    println(3)
                }
                println(999)
            } ()
             broadcast in :global, nil
        """, true)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun parand4() {
        val out = all("""
            spawn task () {
                par-and {
                    defer { println(1) }
                    ${yield("ok1")}
                    ${yield("ok2")}
                    println(1)
                } with {
                    ${yield()}
                    println(2)
                } with {
                    defer { println(3) }
                    ${yield("ok1")}
                    ${yield("ok2")}
                    println(3)
                }
                println(999)
            } ()
             broadcast in :global, nil
             broadcast nil
        """, true)
        assert(out == "2\n1\n1\n3\n3\n999\n") { out }
    }
    @Test
    fun watching5() {
        val out = all("""
            spawn task () {
                awaiting evt==1 {
                    defer { println(2) }
                    yield()
                    println(1)
                }
                println(999)
            } ()
            broadcast in :global, nil
            broadcast in :global, 1
        """, true)
        assert(out == "1\n2\n999\n") { out }
    }
    @Test
    fun watchingX() {
        val out = all("""
            do {
                var xxx
                val t = spawn (coro () {
                    set xxx = defer { println(2) }
                    println(:111, xxx)
                    xxx
                }())
                xxx
            }
        """)
        assert(out == ":111\tnil\n2\n") { out }
    }
    @Test
    fun watchingXX() {
        val out = all("""
            do {
                var xxx
                val t = spawn (coro () {
                    set xxx = nil
                    defer { println(2) }
                    println(:111, xxx)
                    xxx
                }())
                xxx
            }
        """)
        assert(out == ":111\tnil\n2\n") { out }
    }
    @Test
    fun watching6_clk() {
        val out = ceu.all("""
            spawn task () {
                awaiting 10:s {
                    defer { println(10) }
                    await false
                    println(1)
                }
                println(999)
            } ()
            println(0)
            broadcast in :global, tags([5000], :frame, true)
            println(1)
            broadcast tags([5000], :frame, true)
            println(2)
        """, true)
        assert(out == "0\n1\n10\n999\n2\n") { out }
    }
    @Test
    fun watching7() {
        val out = ceu.all(
            """
            task Bird () {
                awaiting true {
                    par {
                    } with {
                    }
                }
            }            
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun par_every8() {
        val out = all("""
            spawn {
                par {
                    every 500:ms {
                    }
                } with {
                    every true { }
                }
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun par_tasks9() {
        val out = all("""
            spawn task () {
                ^[9,29]yield(nil)                                          
            }()                                                       
            spawn task () {                                           
                ^[9,29]yield(nil)                       
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun awaiting10_err() {
        val out = all("""
            task T () {
                awaiting (throw(:error)) {
                    await false
                }
            }            
            spawn in tasks(), T()
            broadcast in :global, nil
        """, true)
        assert(out == "anon : (lin 8, col 13) : broadcast in :global, nil\n" +
                "anon : (lin 3, col 27) : throw(:error)\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun paror11_ret() {
        val out = all("""
            spawn {
                val x = par-or {
                    1
                } with {
                    2
                }
                println(x)
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun parand11_ret() {
        val out = all("""
            spawn {
                val x = par-and {
                    1
                } with {
                    2
                }
                println(x)
            }
        """, true)
        assert(out == "2\n") { out }
    }
    @Test
    fun paror12_ret_func() {
        val out = all("""
            spawn {
                coro f () {
                    par-or {
                        1
                    } with {
                        999
                    }
                }
                val x = await spawn f()
                println(x)
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun paror13_ret_func() {
        val out = all("""
            coro T () {
                await evt==:x
            }
            spawn {
                par-or {
                    await spawn T()
                } with {
                    await spawn T()
                }
            }
            broadcast in :global, :x
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun paror14() {
        val out = all("""
            spawn {
                par-or {
                    await true
                } with {
                    await true
                }
            }
            broadcast in :global, true
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun paror15() {
        val out = all("""
            spawn {
                par-or {
                    await true
                } with {
                    await true
                }
            }
            do {
                broadcast in :global, tags([40], :frame, true)
                broadcast in :global, tags([], :draw, true)
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_awaiting16_track() {
        val out = all("""
            task T () {
                set task.pub = [10]
                await :evt
            }
            val t = coroutine(T)
            resume t ()
            val x = track(t)
            spawn {
                awaiting :check-now x {
                    println(x.pub[0])
                    broadcast in :global, nil
                    println(x.pub[0])
                    broadcast in :global, :evt
                    println(x.pub[0])   ;; never printed
                    await false
                }
                println(status(x))
            }
            println(:ok)
        """, true)
        assert(out == "10\n10\nnil\n:ok\n") { out }
    }
    @Test
    fun todo_awaiting17_track() {
        val out = all("""
            task T () {
                set task.pub = :pub
                await evt==:evt
            }
            val t = spawn T()
            val x = track(t)
            spawn {
                awaiting x {
                    broadcast in :global, :evt
                    println(x.pub)   ;; never printed
                    await false
                }
                println(status(x))
            }
            println(:ok)
        """, true)
        assert(out == "nil\n:ok\n") { out }
    }
    @Test
    fun parand18_immediate() {
        val out = all("""
            spawn task () {
                par-and {
                    println(1)
                } with {
                    println(2)
                }
                println(999)
            } ()
        """, true)
        assert(out == "1\n2\n999\n") { out }
    }
    @Test
    fun paror19_valgrind() {
        val out = all("""
            spawn {
                par-or {
                    loop { yield(nil) }
                } with {
                    par-or {
                        yield()
                    } with {
                        loop { yield(nil) }
                    }
                }
            }
            broadcast in :global, nil
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun await20_track() {
        val out = all("""
            task T () {
                yield()
            }
            val t = spawn T()
            val x = track(t)
            spawn {
                par-and {
                    println(:0)
                    await x
                    println(:2)
                } with {
                    println(:1)
                    broadcast in t, nil
                }
                println(:3)
            }
            println(:4)
        """, true)
        assert(out == ":0\n:1\n:2\n:3\n:4\n") { out }
    }
    @Test
    fun await22_track() {
        val out = all("""
            task T () {
                yield()
            }
            val t = spawn T()
            val x = track(t)
            spawn {
                par-and {
                    println(:0)
                    await x
                    println(:2)
                } with {
                    println(:1)
                    broadcast in t, nil
                }
                println(:3)
            }
            println(:4)
        """, true)
        assert(out == ":0\n:1\n:2\n:3\n:4\n") { out }
    }

    // EVERY

    @Test
    fun every_01() {
        val out = ceu.all(
            """
            task T () {
                println(:1)
                every true until true {
                    throw(999)
                }
                println(:2)
            }
            spawn T()
            broadcast nil
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun every_02() {
        val out = ceu.all(
            """
            task T () {
                println(:1)
                every true until false {
                    println(:xxx)
                } while false
                println(:2)
            }
            spawn T()
            broadcast nil
        """)
        assert(out == ":1\n:xxx\n:2\n") { out }
    }

    // TUPLE / VECTOR / DICT / STRING

    @Test
    fun todo_index1_tuple() {
        val out = all("""
            val t = [1,2,3]
            println(t.a, t.c)
        """)
        assert(out == "1\t3\n") { out }
    }
    @Test
    fun index2_dict() {
        val out = all("""
            val t = @[ (:x,1), (:y,2) ]
            println(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun vector3_size() {
        val out = all("""
            val v = #[]
            println(#v, v)
            set v[+] = 1
            set v[+] = 2
            println(#v, v)
            val top = v[-]
            println(#v, v, v[=], top)
        """, true)
        assert(out == "0\t#[]\n2\t#[1,2]\n1\t#[1]\t1\t2\n") { out }
    }
    @Test
    fun string4() {
        val out = all("""
            var v = "abc"
            set v[#v] = 'a'
            set v[2] = 'b'
            println(v[0])
            `puts(${D}v.Dyn->Ncast.Vector.buf);`
        """)
        assert(out == "a\nabba\n") { out }
    }
    @Test
    fun string5() {
        val out = all("""
            println("")
            println("a\tb")
            println("a\nb")
            println("a'\"b")
        """)
        assert(out == "#[]\na\tb\na\nb\na'\"b\n") { out }
    }
    @Test
    fun dict6_init_err() {
        val out = all("""
            var t = @[x,y]
            println(t.x, t.y)
        """)
        assert(out == "anon : (lin 2, col 24) : expected \"=\" : have \",\"") { out }
    }
    @Test
    fun dict7_init() {
        val out = all("""
            var t = @[x=1, y=2]
            println(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun vector8() {
        val out = all("""
            var v
            set v = #[]
            ifs true {
                true {
                    set v[#v] = 10
                }
            }
            println(v)
        """)
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun vector9_concat() {
        val out = all("""
            var v1
            set v1 = #[1,2,3]
            var v2
            set v2 = #[4,5,6]
            println(v1 ++ v2)
        """, true)
        assert(out == "#[1,2,3,4,5,6]\n") { out }
    }
    @Test
    fun dict10_iter_nil() {
        val out = all("""
            val t = @[x=1, y=2, z=3]
            loop in iter(t), v {
                println(v)
            }
        """, true)
        assert(out == ":x\n:y\n:z\n") { out }
    }
    @Test
    fun dict10_iter_val() {
        val out = all("""
            val t = @[x=1, y=2, z=3]
            loop in iter(t,:val), v {
                println(v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun dict10_iter_key() {
        val out = all("""
            val t = @[x=1, y=2, z=3]
            loop in iter(t,:key), v {
                println(v)
            }
        """, true)
        assert(out == ":x\n:y\n:z\n") { out }
    }
    @Test
    fun dict10_iter_all() {
        val out = all("""
            val t = @[x=1, y=2, z=3]
            loop in iter(t,:all), v {
                println(v)
            }
        """, true)
        assert(out == "[:x,1]\n[:y,2]\n[:z,3]\n") { out }
    }
    @Test
    fun dict10a_iter() {
        val out = all("""
            val t = @[]
            loop in iter(t), v {
                println(v)
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun vect11_iter_nil() {
        val out = all("""
            val t = #[1, 2, 3]
            loop in iter(t), v {
                println(v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun vect11_iter_val() {
        val out = all("""
            val t = #[1, 2, 3]
            loop in iter(t,:val), v {
                println(v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun vect11_iter_all() {
        val out = all("""
            val t = #[1, 2, 3]
            loop in iter(t,:all), v {
                println(v)
            }
        """, true)
        assert(out == "[0,1]\n[1,2]\n[2,3]\n") { out }
    }
    @Test
    fun vect11_iter_idx() {
        val out = all("""
            val t = #[1, 2, 3]
            loop in iter(t,:idx), v {
                println(v)
            }
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun vect12_iter_err() {
        val out = all("""
            val t = #[1, 2, 3]
            loop in iter(t), (i {
                println(i, v)
            }
        """, true)
        //assert(out == "anon : (lin 3, col 36) : expected \",\" : have \"{\"") { out }
        assert(out == "anon : (lin 3, col 30) : expected identifier : have \"(\"") { out }
    }
    @Test
    fun dict13_iter() {
        val out = all("""
            val t = @[x=1, y=2, z=3]
            loop in iter(t,:all), x {
                val k = x.0
                val v = x.1
                println(k, v)
            }
        """, true)
        assert(out == ":x\t1\n:y\t2\n:z\t3\n") { out }
    }
    @Test
    fun string_concat15() {
        val out = all("""
            val s = #[]
            s <++ #['1']
            s <++ #['2']
            s <++ #['3']
            println(s)
        """, true)
        assert(out == "123\n") { out }
    }
    @Test
    fun concat16() {
        val out = all("""
            func f (v) {
                set v[+] = 1
                v
            }
            func g () {
                f(#[])
            }
            println(g())
        """, true)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun string17() {
        val out = all("""
            val v = ""
            println(v)
            `printf(">%s<\n", ${D}v.Dyn->Ncast.Vector.buf);`
        """)
        assert(out == "#[]\n><\n") { out }
    }
    @Test
    fun tuple18_size() {
        val out = all("""
            val t = [1, 2, 3]
            println(#t)
        """)
        assert(out == "3\n") { out }
    }
    @Test
    fun tuple19_iter() {
        val out = all("""
            val t = [1, 2, 3]
            loop in iter(t,:all), v {
                println(v)
            }
        """, true)
        assert(out == "[0,1]\n[1,2]\n[2,3]\n") { out }
    }
    @Test
    fun tupl20_dots() {
        val out = ceu.all(
            """
            val x = [[10]]
            println(x, x.0, x.0.0)
        """
        )
        assert(out == "anon : (lin 3, col 31) : index error : ambiguous dot : use brackets") { out }
    }
    @Test
    fun tupl21_dots() {
        val out = ceu.all(
            """
            val x = [[10]]
            println(x, x.0, x.0[0])
        """
        )
        assert(out == "[[10]]\t[10]\t10\n") { out }
    }
    @Test
    fun dict22_iter_it() {
        val out = all("""
            val t = @[x=1, y=2, z=3]
            loop in iter(t) {
                println(it)
            }
        """, true)
        assert(out == ":x\n:y\n:z\n") { out }
    }

    // AWAIT / EVERY

    @Test
    fun await1() {
        val out = all("""
            spawn {
                println(0)
                await (evt/=nil) and (evt[:type]==:x)
                println(99)
            }
            do {
                println(1)
                broadcast @[(:type,:y)]
                println(2)
                broadcast in :global, @[(:type,:x)]
                println(3)
            }
        """, true)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun await2() {
        val out = all("""
            spawn {
                println(0)
                await evt is? :x
                println(99)
            }
            do {
                println(1)
                broadcast in :global, tags([], :y, true)
                println(2)
                broadcast in :global, tags([], :x, true)
                println(3)
            }
        """, true)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun await3() {
        val out = all("""
            spawn {
                println(0)
                await :x
                println(99)
            }
            do {
                println(1)
                broadcast in :global, tags([], :y, true)
                println(2)
                broadcast in :global, tags([], :x, true)
                println(3)
            }
        """, true)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun await4_err() {
        val out = all("""
            val f
            await f()
        """)
        assert(out == "anon : (lin 3, col 13) : yield error : expected enclosing coro or task") { out }
    }
    @Test
    fun await5() {
        val out = ceu.all(
            """
            spawn task () {
                loop {
                    await true                    
                    println(evt)
                }
            }()
             broadcast in :global, @[]
        """, true)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun every6() {
        val out = all("""
            spawn {
                println(0)
                every :x {
                    println(evt.0)
                }
            }
            do {
                println(1)
                broadcast in :global, tags([10], :x, true)
                println(2)
                broadcast in :global, tags([20], :y, true)
                println(3)
                broadcast in :global, tags([30], :x, true)
                println(4)
            }
        """, true)
        assert(out == "0\n1\n10\n2\n3\n30\n4\n") { out }
    }
    @Test
    fun await7_clk() {
        val out = ceu.all("""
            spawn task () {
                loop {
                    await 10:s
                    println(999)
                }
            }()
            println(0)
            broadcast in :global, tags([5000], :frame, true)
            println(1)
            broadcast in :global, tags([5000], :frame, true)
            println(2)
        """, true)
        assert(out == "0\n1\n999\n2\n") { out }
    }
    @Test
    fun every8_clk() {
        val out = ceu.all("""
            spawn task () {
                every 10:s {
                    println(10)
                }
            }()
            println(0)
            broadcast in :global, tags([5000], :frame, true)
            println(1)
            broadcast tags([5000], :frame, true)
            println(2)
            broadcast in :global, tags([10000], :frame, true)
            println(3)
        """, true)
        assert(out == "0\n1\n10\n2\n10\n3\n") { out }
    }
    @Test
    fun todo_every9_clk_multi() { // awake twice from single bcast
        val out = ceu.all("""
            spawn task () {
                every 10:s {
                    println(10)
                }
            }()
            println(0)
            broadcast in :global, tags([20000], :frame, true)
            println(1)
        """, true)
        assert(out == "0\n10\n10\n1") { out }
    }
    @Test
    fun await10_task() {
        val out = all("""
            spawn {
                await spawn { 1 }
                println(1)
            }
            println(2)
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun await11_task() {
        val out = all("""
            spawn {
                spawn {
                    yield ()
                    println(1)
                }
                yield ()
                println(2)
            }
            broadcast in :global, nil
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun todo_await12_task() {
        val out = all("""
            spawn {
                spawn {
                    yield ()
                    println(1)
                    broadcast in :global, nil
                    println(3)
                }
                yield ()
                println(2)
            }
            broadcast in :global, nil
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun await13_task_rets() {
        val out = all("""
            spawn {
                var y = await spawn {
                    yield ()
                    [2]
                }
                println(y)
            }
            broadcast in :global, nil
        """, true)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun await14_task_err() {
        val out = all("""
            var x = await spawn in nil, nil()
        """)
        assert(out == "anon : (lin 2, col 27) : expected non-pool spawn : have \"spawn\"") { out }
    }
    @Test
    fun await15_task_rets() {
        val out = all("""
            spawn {
                var x = await spawn {
                    var y = []
                    y
                }
                println(x)
            }
        """, true)
        assert(out.contains("anon : (lin 3, col 52) : block escape error : incompatible scopes")) { out }
        //assert(out == "anon : (lin 2, col 20) : task :fake () { group { var x set x = do { gr...)\n" +
        //        "anon : (lin 3, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun await16_task_rets_valgrind () {
        val out = all("""
            spawn {
                var x = await spawn {
                    1
                }
                var y = await spawn {
                    yield ()
                    [2]
                }
                task T () {
                    3
                }
                var z = await spawn T()
                println(x,y,z)
            }
            broadcast in :global, nil
        """, true)
        assert(out == "1\t[2]\t3\n") { out }
    }
    @Test
    fun await17_task() {
        val out = all("""
            task Main_Menu () {
                await false
            }            
            spawn {
                await spawn Main_Menu ()
                println(999)
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun await18_now() {
        val out = all("""
            spawn {
                println(1)
                await :check-now true
                println(2)
            }
            println(3)
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun await19_notfalse() {
        val out = all("""
            spawn {
                println(1)
                await 10
                println(2)
            }
            broadcast in :global, nil
        """, true)
        assert(out == "1\n2\n") { out }
    }

    // FUNC / TASK

    @Test
    fun func1() {
        val out = ceu.all(
            """
            func f (x) {
                println(x)
            }
            f(10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun task2() {
        val out = ceu.all(
            """
            task f (x) {
                println(x)
            }
            spawn f (10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun func3_err() {
        val out = ceu.all(
            """
            func f {
                println(x)
            }
        """
        )
        //assert(out == "anon : (lin 2, col 20) : expected \"(\" : have \"{\"") { out }
        assert(out == "anon : (lin 3, col 25) : access error : variable \"x\" is not declared") { out }
    }
    @Test
    fun task4_pub_fake_err() {
        val out = all("""
            spawn {
                awaiting evt==:a {
                    every evt==:b {
                        println(task.pub)    ;; no enclosing task
                    }
                }
            }
            println(1)
        """)
        assert(out == "anon : (lin 5, col 33) : task error : missing enclosing task") { out }
    }
    @Test
    fun task5_pub_fake() {
        val out = all("""
            spawn (task () {
                set task.pub = 1
                awaiting evt==:a {
                    every evt==:b {
                        println(task.pub)
                    }
                }
            }) ()
             broadcast in :global, :b
             broadcast :b
             broadcast :a
             broadcast in :global, :b
        """, true)
        assert(out == "1\n1\n") { out }
    }
    @Test
    fun task6_pub_fake() {
        val out = all("""
            task T () {
                set task.pub = 10
                println(task.pub)
                spawn {
                    println(task.pub)
                    await false
                }
                nil
            }
            spawn T()
            broadcast in :global, nil
        """, true)
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun task7_tup_status() {
        val out = all("""
            task T () {}
            val ts = [spawn T()]
            println(status(ts[0]))
        """)
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun func8_it() {
        val out = ceu.all(
            """
            func f {
                println(it)
            }
            f(10)
        """
        )
        assert(out == "10\n") { out }
    }

    // LAMBDA

    @Test
    fun cc_01_lambda () {
        val out = ceu.all("""
            println(\{ it })
        """)
        assert(out.contains("func: 0x")) { out }
    }
    @Test
    fun cc_02_lambda () {
        val out = ceu.all("""
            println(\x,y{x+y}(1,2))
        """, true)
        assert(out.contains("3\n")) { out }
    }
    @Test
    fun cc_03_lambda () {
        val out = ceu.all("""
            println(\x,{x}(1))
        """, true)
        assert(out.contains("1\n")) { out }
    }
    @Test
    fun cc_04_lambda () {
        val out = ceu.all(
            """
            println(\{ it }(10))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_05_lambda () {
        val out = ceu.all(
            """
            func f (g) {
                g(10)
            }
            println(f \{ it })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_06_it_it () {
        val out = ceu.all(
            """
            \{ \{ it } }    ;; it1/it2
        """)
        assert(out == "10\n") { out }
    }

    // WHERE

    @Test
    fun where1() {
        val out = ceu.all(
            """
            println(x) where {
                val x = 1
            }
            val z = (y + 10) where {
                val y = 20
            }
            println(z)
        """,true)
        assert(out == "1\n30\n") { out }
    }
    @Test
    fun where2() {
        val out = ceu.all(
            """
            task T (v) {
                println(v)
            }
            val t = (spawn T(v)) where { val v = 10 }
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 5, col 34) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun where3_err() {
        val out = ceu.all(
            """
            coro T (v) {
                println(v)
            }
            (val t = spawn T(v)) where { val v = 10 }
            println(t)
        """)
        assert(out == "anon : (lin 6, col 21) : access error : variable \"t\" is not declared") { out }
    }
    @Test
    fun where4() {
        val out = ceu.all(
            """
            coro T (v) {
                println(v)
            }
            (val t = spawn T(v)) where { val v = 10 }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun where5() {
        val out = ceu.all(
            """
            coro T (v) {
                println(v)
            }
            val t = (spawn T(v)) where {
                val v = 10
            }
            println(type(t))
        """)
        assert(out == "10\n:x-coro\n") { out }
    }
    @Test
    fun where6() {
        val out = ceu.all(
            """
            task T (v) {
                println(v)
                yield()
            }
            val ts = tasks()
            (spawn in ts, T(v)) where {
                val v = 10
            }
            loop in :tasks ts, t {
                println(type(t))
            }
        """)
        assert(out == "10\n:x-track\n") { out }
    }
    @Test
    fun where7_err() {
        val out = ceu.all(
            """
            val z = y + 10 where {
                val y = 20
            }
        """,true)
        assert(out == "anon : (lin 2, col 21) : access error : variable \"y\" is not declared") { out }
    }
    @Test
    fun where8() {
        val out = ceu.all("""
            val x = y
                where {
                    val y = 10
                }
            println(x)
        """)
        assert(out == "10\n") { out }
    }

    // THUS
    @Test
    fun qq_01_thus() {
        val out = ceu.all(
            """
            val x = 1 thus {
                it
            }
            println(x)
        """,true)
        assert(out == "1\n") { out }
    }
    @Test
    fun qq_02_thus() {
        val out = ceu.all(
            """
            val x = 1 thus x {
                x
            }
            println(x)
        """,true)
        assert(out == "1\n") { out }
    }

    // TOGGLE

    @Test
    fun toggle1_err() {
        val out = all("""
            toggle f() -> {
        """)
        assert(out == "anon : (lin 2, col 27) : expected expression : have \"{\"") { out }
    }
    @Test
    fun toggle2() {
        val out = all("""
            task T (v) {
                set task.pub = v
                toggle evt==:hide -> evt==:show {
                    println(task.pub)
                    every (evt is? :dict) and (evt.sub==:draw) {
                        println(evt.v)
                    }
                }
            }
            spawn T (0)
            broadcast in :global, @[(:sub,:draw),(:v,1)]
            broadcast in :global, :hide
            broadcast in :global, @[(:sub,:draw),(:v,99)]
            broadcast in :global, :show
            broadcast in :global, @[(:sub,:draw),(:v,2)]
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun toggle3() {
        val out = all("""
            task T (v) {
                set task.pub = v
                toggle :hide -> :show {
                    println(task.pub)
                    every :draw {
                        println(evt.0)
                    }
                }
            }
            spawn T (0)
            broadcast in :global, tags([1], :draw, true)
            broadcast in :global, tags([], :hide, true)
            broadcast in :global, tags([99], :draw, true)
            broadcast in :global, tags([], :show, true)
            broadcast in :global, tags([2], :draw, true)
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun toggle4_ret() {
        val out = all("""
            spawn {
                val x = toggle evt==:hide -> evt==:show {
                    10
                }
                println(x)
            }
            println(:ok)
        """)
        assert(out == "10\n:ok\n") { out }
    }

    // LOOP / BREAK / UNTIL

    /*
    @Test
    fun break1() {
        val out = all("""
            loop { {:break}
                throw(:break)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun break2() {
        val out = all("""
            loop if false {
                loop { {:break}
                    throw(:break)
                }
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun break3() {
        val out = all("""
            loop { {:break2}
                loop { {:break1}
                    throw(:break2)
                }
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    */

    @Test
    fun until4() {
        val out = all("""
            println(loop {
            } until 10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun until5() {
        val out = all("""
            var x = 0
            loop {
                set x = x + 1
                println(x)
            } until x == 3
            println(99)
        """, true)
        assert(out == "1\n2\n3\n99\n") { out }
    }
    @Test
    fun until6() {
        val out = all("""
            var x = 0
            val v = loop {
                set x = x + 1
                println(x)
            } until x == 3 {
            } until false
            println(v)
        """, true)
        assert(out == "1\n2\n3\ntrue\n") { out }
    }
    @Test
    fun until7() {
        val out = all("""
            println(0)
            loop {
                println(1)
            } until true {
                println(2)
            }
            println(3)
        """)
        assert(out == "0\n1\n3\n") { out }
    }
    @Test
    fun until8() {
        val out = all("""
            println(0)
            var x = false
            loop {
                println(1)
            } until x {
                set x = true
                println(2)
            }
            println(3)
        """)
        assert(out == "0\n1\n2\n1\n3\n") { out }
    }
    @Test
    fun until9() {
        val out = all("""
            println(0)
            var x = false
            loop {
                println(1)
            } until x {
                set x = true
                println(2)
            } until x {
                println(3)
            } until x {
                println(4)
            }
            println(5)
        """)
        assert(out == "0\n1\n2\n5\n") { out }
    }
    @Test
    fun until10() {
        val out = all("""
            var x = 0
            loop {
                set x = x + 1
                println(x)
            } until v = (x == 3) {
                println(v)
            }
            println(99)
        """, true)
        assert(out == "1\nfalse\n2\nfalse\n3\n99\n") { out }
    }
    @Test
    fun until11() {
        val out = all("""
            var x = 5
            val f = func () {
                set x = x - 1
                if x>0 { x } else { nil }
            }
            loop while v1=f() {
                println(v1)
            } while v2=f() {
                println(v2)
            }
        """, true)
        assert(out == "4\n3\n2\n1\n") { out }
    }
    @Test
    fun until12() {
        val out = all("""
            val v = loop in [1->10] {
            }
            println(v)
        """, true)
        assert(out == "nil\n") { out }
    }
    @Test
    fun while13() {
        val out = all("""
            val v = loop while false {nil}
            println(v)
        """)
        assert(out == "true\n") { out }
    }

    // LOOP / NUMERIC

    @Test
    fun loop_01_num() {
        val out = all("""
            loop in [0 -> 1], i {
                println(i)
            }
        """, true)
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun loop_02_num() {
        val out = all("""
            println(:0)
            loop in (0 -> 1], a {
                println(a)
            }
            println(:1)
            loop in (0 -> 3), b {
                println(b)
            }
            println(:2)
            loop in [0 -> 4], :step +2, c {
                println(c)
            }
            println(:3)
            loop in (2 -> 0], :step -1, d {
                println(d)
            }
            println(:4)
            loop in [0 -> -2), :step -1 {
                println(:x)
            }
            println(:5)
            loop in [1 -> 2] {
                println(:y)
            }
            println(:6)
        """, true)
        assert(out == ":0\n1\n:1\n1\n2\n:2\n0\n2\n4\n:3\n1\n0\n:4\n:x\n:x\n:5\n:y\n:y\n:6\n") { out }
    }
    @Test
    fun loop_03_num_it() {
        val out = all("""
            loop in [0 -> 1] {
                println(it)
            }
        """, true)
        assert(out == "0\n1\n") { out }
    }

    // LOOP / ITERATOR

    @Test
    fun nn_01_iter() {
        val out = all("""
            func f (t) {
                if t.1 == 5 {
                    nil
                } else {
                    set t.1 = t.1 + 1
                    t.1 - 1
                }
            }
            do {
                val it = [f, 0]
                loop {
                    val i = it.0(it)
                } until (i == nil) {
                    println(i)
                }
            }
        """, true)
        assert(out == "0\n1\n2\n3\n4\n") { out }
    }
    @Test
    fun nn_02_iter() {
        val out = all("""
            func f (t) {
                if t.1 == 5 {
                    nil
                } else {
                    set t.1 = t.1 + 1
                    t.1 - 1
                }
            }
            val it = :Iterator [f,0]
            loop in it, v {
                println(v)
            }
        """, true)
        assert(out == "0\n1\n2\n3\n4\n") { out }
    }
    @Test
    fun nn_03_iter_err() {
        val out = all("""
            loop in nil, v {
                println(v)
            }
        """, true)
        assert(out.contains("assertion error : expected :Iterator")) { out }
    }

    // LOOP / TASK ITERATOR

    @Test
    fun iter1() {
        val out = all("""
            coro T () {
                yield(1)
                yield(2)
                yield(3)
                ;;nil
            }
            loop in iter(coroutine(T)), v {
                println(v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun iter2() {
        val out = all("""
            coro T () {
                yield(1)
                yield(2)
                yield(3)
            }
            catch :x {
                loop in iter(coroutine(T)), i {
                    println(i)
                    throw(:x)
                }
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun iter3() {
        val out = all("""
            coro T () {
                yield(1)
                yield(2)
                yield(3)
            }
            loop in iter(coroutine(T)), i {
                println(i)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun iter3_ok() {
        val out = all("""
            coro T () {
                yield(1)
                yield(2)
                3
            }
            loop in iter(coroutine(T)), i {
                println(i)
            }
        """, true)
        //assert(out == "anon : (lin 12, col 57) : resume error : expected yielded task\n1\n2\n3\n:error\n") { out }
        assert(out == "1\n2\n") { out }

    }
    @Test
    fun iter4() {
        val out = all("""
            coro T () {
                yield(1)
                yield(2)
                nil
            }
            loop in iter(coroutine(T)), i {
                println(i)
            }
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun iter5() {
        val out = all("""
            coro T () {
                yield(1)
                yield(2)
                yield(3)
            }
            println(to-vector(coroutine(T)))
        """, true)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun iter6() {
        val out = all("""
            coro T () {
                yield(1)
                yield(2)
                3
            }
            val co = coroutine(T)
            println(resume co())
            println(resume co())
            println(resume co())
            println(resume co())
        """, true)
        assert(out == "anon : (lin 11, col 21) : resume error : expected yielded coro\n1\n2\n3\n:error\n") { out }
    }
    @Test
    fun iter7() {
        val out = all("""
            val y = loop in iter([1,2,3]), x {
            } until x == 2
            println(y)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun iter8() {
        val out = all("""
            val y = loop in iter([1,2,3]), x {
            } until x == 4
            println(y)
        """, true)
        assert(out == "nil\n") { out }
    }
    @Test
    fun iter9_it() {
        val out = all("""
            val y = loop in iter([1,2,3]) {
            } until it == 4
            println(y)
        """, true)
        assert(out == "nil\n") { out }
    }
    @Test
    fun iter10_it() {
        val out = all("""
            data :Iterator = [f,s,tp,i]
            func iter (v, tp) {
                :Iterator [v]
            }
            export [f] {
                val cur = []
                func f () {
                    cur
                }
            }
            loop in iter(f) {   ;; assigns f to local which confronts cur
            } until true
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 13, col 33) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
    }

    // THROW / CATCH

    @Test
    fun catch1() {
        val out = all("""
            var x
            set x = catch :x {
                throw([])
                println(9)
            }.0
            println(x)
        """, true)
        assert(out == "anon : (lin 4, col 17) : throw([])\n" +
                "throw error : uncaught exception\n" +
                "[]\n") { out }
    }
    @Test
    fun catch2() {
        val out = all("""
            func f (v) {
                false
            }
            catch false {
                catch f(err) {
                    throw([])
                }
            }
            println(`:number ceu_gc_count`)
            println(:ok)
        """)
        assert(out == "anon : (lin 7, col 21) : throw([])\n" +
                "throw error : uncaught exception\n" +
                "[]\n") { out }
    }
    @Test
    fun catch3() {
        val out = all("""
            var x
            set x = catch :x {
                catch :2 {
                    throw(tags([10], :x, true))
                    println(9)
                }
                println(9)
            }.0
            println(`:number ceu_gc_count`) ;; throw might be caught, so zero but no check
            println(:x, x)
        """, true)
        assert(out == "0\n:x\t10\n") { out }
    }
    @Test
    fun catch6_err() {
        val out = all("""
            catch err==[] {
                var x
                set x = []
                throw(x)
                println(9)
            }
            println(1)
        """, true)
        //assert(out == "anon : (lin 5, col 28) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 2, col 27) : block escape error : incompatible scopes\n" +
                "anon : (lin 5, col 17) : throw(x)\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun catch7() {
        val out = all("""
            do {
                println(catch :x {
                    throw(tags([10],:x,true))
                    println(9)
                })
            }
        """, true)
        assert(out == ":x [10]\n") { out }
    }
    @Test
    fun catch8() {
        val out = all("""
            catch false {
                catch {
                    throw([10])
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun catch9() {
        val out = all("""
            var x
            set x = catch :x {
                var y
                set y = catch true {
                    throw([10])
                    println(9)
                }
                ;;println(1)
                y
            }
            println(x)
        """.trimIndent(), true)
        //assert(out == "anon : (lin 9, col 5) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 2, col 18) : block escape error : incompatible scopes\n" +
                "anon : (lin 5, col 9) : throw([10])\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun loop_01() {
        val out = all("""
            println(catch :x { loop { throw(tags([1],:x,true)) }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun loop_02() {
        val out = all("""
            println(catch :x { loop { throw(tags([1],:x,true)) }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun loop_03() {
        val out = all("""
            println(catch :2 { loop { throw(tags([1],:2,true)) }})
        """, true)
        assert(out == ":2 [1]\n") { out }
    }
    @Test
    fun loop_04() {
        val out = all("""
            println(catch :x { loop {
                var x
                set x = [1] ;; memory released
                throw(tags([1],:x,true))
            }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun loop_05_err() {
        val out = all("""
            println(catch :x { loop {
                var x
                set x = [1]
                throw(tags(x,:x,true))
            }})
        """.trimIndent(), true)
        //assert(out == "anon : (lin 4, col 14) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 1, col 33) : block escape error : incompatible scopes\n" +
                "anon : (lin 4, col 5) : throw(tags(x,:x,true))\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun catch10() {
        val out = all("""
            catch err===[] {
                throw([])
                println(9)
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }


    // PEEK, PUSH, POP

    @Test
    fun ppp1() {
        val out = all("""
            val v = #[1]
            set v[=] = 10
            println(v)
        """, true)
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun ppp2() {
        val out = all("""
            val v = #[10]
            println(v[=])
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ppp3() {
        val out = all("""
            val v = #[]
            set v[+] = 1
            println(v)
        """)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun ppp4_err() {
        val out = all("""
            val v = #[]
            v[+]
        """, true)
        assert(out == "anon : (lin 4, col 41) : index error : out of bounds\n" +
                ":error\n") { out }
    }
    @Test
    fun ppp5() {
        val out = all("""
            var v = #[1]
            var x = v[-]
            println(#v, x)
        """, true)
        assert(out == "0\t1\n") { out }
    }
    @Test
    fun todo_ppp6_err() {
        val out = all("""
            val v = #[1]
            set v[-] = 10   ;; cannot set v[-]
            println(v)
        """, true)
        assert(out.contains("anon : (lin 6, col 41) : expected \"=\" : have")) { out }
    }
    @Test
    fun ppp7() {
        val out = all("""
            var v
            set v = #[]
            set v[+] = 1
            set v[+] = 2
            set v[=] = 20
            set v[+] = 3
            println(#v, v[=])
            val x = v[-]
            println(#v, v[=], x)
        """, true)
        assert(out == "3\t3\n2\t20\t3\n") { out }
    }
    @Test
    fun ppp8_debug() {
        val out = all("""
            var v
            set v = #[10]
            println(v[#v - 1])
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ppp9_debug() {
        val out = all("""
            var v
            set v = #[10]
            println(v[-1+1])
        """, true)
        assert(out == "anon : (lin 4, col 24) : expected \"]\" : have \"1\"") { out }
    }

    // to-number, to-string, to-vector

    @Test
    fun tostring1() {
        val out = all("""
            val s = to-string(10)
            println(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun tonumber2() {
        val out = all("""
            val n = to-number("10")
            println(type(n), n)
        """, true)
        assert(out == ":number\t10\n") { out }
    }
    @Test
    fun tonumber_tostring3() {
        val out = all("""
            val s = to-string(to-number("10"))
            println(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun tovector4() {
        val out = all("""
            coro T() {
                yield([1])
            }
            val t = coroutine(T)
            val v = to-vector(t)
            println(v)
        """, true)
        assert(out == "#[[1]]\n") { out }
    }

    // string-to-tag

    @Test
    fun ff_01_string_to_tag() {
        val out = all("""
            pass :xyz
            println(string-to-tag(":x"))
            println(string-to-tag(":xyz"))
            println(string-to-tag("xyz"))
        """, true)
        assert(out == "nil\n:xyz\nnil\n") { out }
    }
    @Test
    fun ff_02_string_to_tag() {
        val out = all("""
            data :A = [] {
                :B = [] {
                    :C = []
                }
            }
            println(string-to-tag(":A"), string-to-tag(":A.B"), string-to-tag(":A.B.C"))
        """, true)
        assert(out == ":A\t:A.B\t:A.B.C\n") { out }
    }

    // COMPOSITION

    @Test
    fun comp1() {
        val out = all("""
            func square (x) {
                x**2
            }
            val quad = square <|< square
            println(quad(3))
        """, true)
        assert(out == "81\n") { out }
    }

    // ENUM

    @Test
    fun enum01() {
        val out = all("""
            pass :antes
            enum {
                :x = `1000`,
                :y, :z,
                :a = `10`,
                :b, :c
            }
            pass :meio
            enum {
                :i = `100`,     ;; ignored b/c of itr.i in iter-tuple
                :j,
            }
            pass :depois
            val t = [:antes, :x, :y, :z, :a, :b, :c, :meio, :i, :j, :depois]
            loop in iter(t,:all), v {
                set t[v.0] = to-number(v.1)
            }
            println(t)
        """, true)
        assert(out == "[44,1000,1001,1002,10,11,12,45,37,101,46]\n") { out }
    }

    // TAGS / PRE

    @Test
    fun tt_00_tags() {
        val out = all("""
            val x  = tags([],:X,true)
            val xy = tags(tags([],:X,true), :Y, true)
            println(x, xy)
        """)
        assert(out == ":X []\t[:Y,:X] []\n") { out }
    }
    @Test
    fun tt_01_tags() {
        val out = all("""
            data :T = [x]
            val x = :T [1]
            println(x, tags(x))
        """)
        assert(out == ":T [1]\t[:T]\n") { out }
    }
    @Test
    fun tt_02_tags() {
        val out = all("""
            data :T = [x]
            val x = :T [1]
            val y = :T x.x
            println(y)
        """)
        assert(out == "1\n") { out }
    }

    // TEMPLATE

    @Test
    fun tplate01() {
        val out = all("""
            data :T = [x,y]
            data :T.S = [z]
            val t :T = :T []
            var s :T.S
            set s = :T.S []
            println(t is? :T, t is? :T.S)
            println(s is? :T, s is? :T.S)
        """, true)
        assert(out == "true\tfalse\ntrue\ttrue\n") { out }
    }
    @Test
    fun tplate02() {
        val out = all("""
            data :U = [a]
            data :T = [x,y]
            data :T.S = [z:U]
            var s :T.S
            set s = :T.S [1,2,:U[3]]
            println(s is? :T, s.z is? :U)
            set s.z = :U [10]
            println(s is? :T.S, s.z is? :U)
        """, true)
        assert(out == "true\ttrue\ntrue\ttrue\n") { out }
    }
    @Test
    fun tplate03_nest() {
        val out = all("""
            data :T = [t] {
                :A = [a] {
                    :I = []
                    :J = [j]
                }
                :B = []
                :C = [] {
                    :Q = [q] {
                        :X = []
                        :Y = []
                    }
                }
            }
            val a :T.A   = :T.A [10,20]
            val b :T     = :T.B [30]
            val c :T.C.Q = :T.C.Q.Y [40,50]
            println(a.a, b.t, c.q)
            println(a is? :T, b is? :T.C, c is? :T.C.Q.Y)
        """, true)
        assert(out == "20\t30\t50\ntrue\tfalse\ttrue\n") { out }
    }
    @Test
    fun todo_tplateXX() {
        val out = all("""
            data :T = [x,y]
            val t :T = [x=1,y=2]
            set t.x = 3
            println(t)      ;; [x=3,y=2]
        """, true)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun tplate05_err() {
        val out = all("""
            data :T = [v]
            data :U = [t:T,X]
            var u :U = [[10]]
            println(u.X.v)
        """, true)
        //assert(out == "anon : (lin 5, col 25) : index error : field \"X\" is not a data") { out }
        assert(out == "anon : (lin 5, col 21) : index error : out of bounds\n" +
                ":error\n") { out }
    }
    @Test
    fun tplate06_tup() {
        val out = all("""
            data :T = [v]
            val t :T = [[1,2,3]]
            println(t.v.1)
        """, true)
        assert(out == "2\n") { out }
    }
    @Test
    fun todo_tplate07_nest() {
        val out = all("""
            data :T = [] {
                :A = [v] {
                    :x,:y,:z        ;; TODO: list of subtypes w/o data
                }
            }
            val x :T.A.x = :T.A.x [10]
            println(x)
            println(x.v)
            println(string-to-tag(":T.A.z"))
        """, true)
        assert(out == "20\t30\t50\ntrue\tfalse\ttrue\n") { out }
    }
    @Test
    fun tplate08_ifs() {
        val out = all("""
            data :T = [v]
            val v = ifs {
                t :T = [10] -> t.v
            }
            println(v)
        """)
        assert(out == "10\n") { out }
    }

    // AWAIT / EVT / TEMPLATE / DATA

    @Test
    fun xx_01_await_data() {
        val out = all("""
            data :E = [x,y]
            spawn {
                await :E
                println(evt.x)
            }
            broadcast in :global, :E [10,20]
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun xx_02_await_data() {
        val out = all("""
            data :E = [x,y]
            spawn {
                await :E, evt.y==20
                println(evt.x)
            }
            broadcast in :global, :E [10,10]
            println(:mid)
            broadcast in :global, :E [10,20]
        """, true)
        assert(out == ":mid\n10\n") { out }
    }
    @Test
    fun xx_03_await_data() {
        val out = all("""
            data :E = [x,y]
            data :F = [i,j]
            spawn {
                await :E, evt.y==20
                println(evt.x)
                await :F, evt.i==10
                println(evt.j)
            }
            broadcast in :global, :E [10,20]
            broadcast in :global, :F [10,20]
        """, true)
        assert(out == "10\n20\n") { out }
    }

    // TESTS

    @Test
    fun yy_01() {
        val out = all("""
            func g () {
            }
            coro bar () {
                pass [g, coroutine(coro () {})]
                nil
            }
            val it = [g, coroutine(bar)]
            resume it[1]()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun yy_02() {
        val out = all("""
            data :Iterator = [f,s,tp,i]
            func iter-coro (itr :Iterator) {
                val co = itr.s
                val v = resume co()
                ((status(co) /= :terminated) and v) or nil
            }
            func iter (v) {
                [iter-coro,  v]
            }
            
            func bar (v) {
                [iter-coro, v]
            }
            bar(coroutine(coro () {}))
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun yy_03() {
        val out = all("""
            func g () {}
            func f (v) {
                [g, v]
            }
            func x () {
                val t = coro () {}
                f(t)
                nil
            }
            x()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // ALL

    @Test
    fun all1() {
        val out = all("""
            task T (pos) {
                await true
                println(pos)
            }
            spawn {
                val ts = tasks()
                do {
                    spawn in ts, T([])
                }
                await false
            }
            broadcast in :global, nil
        """, true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun all2() {
        val out = all("""
            task T (pos) {
                set task.pub = func () { pos }
                await false
            }
            val t = spawn T ([1,2])
            println(t.pub())
        """, true)
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun all3() {
        val out = all("""
            task T () {
                do {
                    val x = []
                    set task.pub = func () { x }
                }
            }
            spawn T ()
        """, true)
        assert(out == "anon : (lin 8, col 19) : T()\n" +
                "anon : (lin 5, col 30) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun all4() {
        val out = all("""
            task U () {
                set task.pub = func () {
                    10
                }
            }
            task T (u) {
                println(u.pub())
            }
            spawn T (spawn U())
        """, true)
        assert(out == "anon : (lin 10, col 28) : U()\n" +
                "anon : (lin 2, col 23) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun all5() {
        val out = all("""
            task U () {
                set task.pub = func () {
                    10
                }
                await false
            }
            task T (u) {
                println(u.pub())
            }
            spawn T (spawn U())
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun all6() {
        val out = all("""
            task U () {
                set task.pub = func () {
                    10
                }
                await false
            }
            task T (u) {
                println(u.pub())
            }
            spawn T (spawn U())
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun all7() {
        val out = all("""
            func f () {}
            spawn {
                f() where {}
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun all8() {
        val out = all("""
            spawn {
                loop {
                    await evt==10
                    broadcast in :global, tags([], :pause, true)
                    awaiting evt==10 {
                        await false
                    }
                    broadcast in :global, tags([], :resume, true)
                }
            }
            broadcast in :global, 10
            broadcast in :global, 10
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun all9() {
        val out = all("""
            spawn {
                loop {
                    await evt==10
                    broadcast in :global, tags([], :pause, true)
                    awaiting evt==10 {
                        await false
                    }
                    broadcast in :global, tags([], :resume, true)
                    await true
                }
            }
            broadcast in :global, 10
            broadcast in :global, 10
            broadcast in :global, 10
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun all10_valgrind () {
        val out = all("""
            spawn {
                loop {
                    await evt==10
                    println(:1)
                    awaiting evt==10 {
                        await false
                    }
                    println(:2)
                }
            }
            broadcast in :global, 10    ;; :1
            broadcast in :global, 10    ;; :2 (not :1 again)
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun all11_term_coro () {
        val out = all("""
            task T () {
                println(:1)
                awaiting false {
                    await true
                }
                println(:2)
                ;;println(:t)
            }
            spawn {
                val ts = tasks()
                spawn in ts, T()
                ;;println(:every)
                every :e {
                    ;;println(:while)
                    loop in :tasks ts, t {
                        ;;println(t, detrack(t), status(detrack(t)))
                        assert(status(detrack(t)) /= :terminated)
                    }
                }
            }
            ;;println(:bcast)
            broadcast in :global, :e
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun all12_tk_pre () {
        val out = all("""
            ifs v {
                is? :pointer -> c-to-string(v)
                is? :number -> 1
            }
        """)
        assert(out == "anon : (lin 2, col 17) : access error : variable \"v\" is not declared") { out }
    }
    @Test
    fun all13_self_kill () {
        val out = all("""
            spawn {
                loop {
                    println(:10)
                    spawn {
                        println(:a)
                        await evt==:E
                        do {
                            println(:b)
                            broadcast in :global, :E
                            println(:c)
                        }
                        println(:d)
                    }
                    println(:20)
                    await evt==:E
                    println(:30)
                }
            }
            println(:1)
            broadcast in :global, nil
            println(:2)
            broadcast in :global, :E
            println(:3)
        """)
        assert(out == ":10\n:a\n:20\n:1\n:2\n:b\n:30\n:10\n:a\n:20\n:3\n") { out }
    }
    @Test
    fun all14_tasks_it() {
        val out = all("""
            var ts
            set ts = tasks()
            println(type(ts))
            var T
            set T = task (v) {
                set task.pub = v
                val v' = yield(nil)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            
            loop in :tasks ts, t1 {
                loop in :tasks ts {
                    println(detrack(t1).pub, detrack(it).pub)
                }
            }
             broadcast in :global, 2
        """)
        assert(out == ":x-tasks\n1\t1\n1\t2\n2\t1\n2\t2\n") { out }
    }
}
