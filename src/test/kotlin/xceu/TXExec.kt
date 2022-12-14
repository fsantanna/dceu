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
    fun if1() {
        val out = all("""
            var x
            set x = if (true) { 1 }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun if2() {
        val out = all("""
            var x
            set x = 10
            set x = if false { 1 }
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun if3() {
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
    fun if4_err() {
        val out = all("""
            println(if [] {})
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 4) : if error : invalid condition\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun if5() {
        val out = all("""
            println(if false { true })
        """.trimIndent())
        assert(out == "nil\n") { out }
    }


    // IFS

    @Test
    fun ifs1() {
        val out = all("""
            var x = ifs {
                10 < 1 -> 99
                (5+5)==0 -> { 99 }
                else -> 10
            }
            println(x)
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ifs2() {
        val out = all("""
            var x = ifs { true -> `:number 1` }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ifs3() {
        val out = all("""
            var x = ifs 20 {
                == 10 -> false
                == 20 -> true
                else  -> false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ifs4() {
        val out = all("""
            var x = ifs 20 {
                == 10 -> false
                true  -> true
                == 20 -> false
                else  -> false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ifs5() {
        val out = all("""
            var x = ifs 20 {
                == 10 -> false
                else -> true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun todo_ifs6_nocnd() {
        val out = all("""
            var x = ifs 20 {
                true -> ifs {
                    == 20 -> true   ;; err: no ifs expr
                }
            }
            println(x)
        """)
        assert(out == "ERROR\n") { out }
    }
    @Test
    fun ifs7() {
        val out = all("""
            var x = ifs 20 {
                is 10 -> false
                true  -> true
                is 20 -> false
                else  -> false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
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

    // is, isnot

    @Test
    fun is1() {
        val out = all("""
            println([] is :bool)
            println([] is :tuple)
            println(1 isnot :tuple)
            println(1 isnot :number)
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun is2() {
        val out = all("""
            var t
            set t = []
            tags(t,:x,true)
            println(t is :x)
            tags(t,:y,true)
            println(t isnot :y)
            tags(t,:x,false)
            println(t isnot :x)
        """, true)
        assert(out == "true\nfalse\ntrue\n") { out }
    }

    // assert

    @Test
    fun assert1() {
        val out = all("""
            catch :assert {
                assert([] is :bool, "ok")
            }
            assert(1 isnot :number)
        """, true)
        assert(out.contains("assertion error : ok\n" +
                "assertion error : no reason given\n")) { out }
    }

    // YIELD

    @Test
    fun bcast1() {
        val out = all("""
            var tk = task () :awakes {
                println(evt)
                do { var ok; set ok=true; while ok { yield(nil); if (evt isnot :coro) { set ok=false } else { nil } } }
                ;;yield()
                println(evt)                
            }
            var co1 = coroutine(tk)
            var co2 = coroutine(tk)
            broadcast in :global, 1
            broadcast in :global, 2
            broadcast in :global, 3
        """, true)
        assert(out == "1\n1\n2\n2\n") { out }
    }

    // YIELD ALL

    @Test
    fun yieldall1() {
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
            println(tovector(coroutine(bar)))
        """, true)
        assert(out == "xaby\n") { out }
    }


    // SPAWN, PAR

    @Test
    fun par1() {
        val out = all("""
            spawn task () :awakes {
                par {
                    do { var ok1; set ok1=true; while ok1 { yield(nil); if type(evt)/=:coro { set ok1=false } else { nil } } }
                    ;;yield()
                    do { var ok2; set ok2=true; while ok2 { yield(nil); if type(evt)/=:coro { set ok2=false } else { nil } } }
                    ;;yield()
                    println(1)
                } with {
                    do { var ok3; set ok3=true; while ok3 { yield(nil); if type(evt)/=:coro { set ok3=false } else { nil } } }
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
            (spawn in ts, T()) where {
            }
        """)
        assert(out == "anon : (lin 2, col 23) : access error : variable \"ts\" is not declared") { out }
    }

    // PARAND / PAROR / WATCHING

    @Test
    fun paror1() {
        val out = all("""
            spawn task () {
                paror {
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
            spawn task () :awakes {
                paror {
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
                paror {
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
            spawn task () :awakes {
                paror {
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
            spawn task () :awakes {
                parand {
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
            spawn task () :awakes {
                parand {
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
             broadcast in :global, nil
        """, true)
        assert(out == "2\n1\n1\n3\n3\n999\n") { out }
    }
    @Test
    fun watching5() {
        val out = all("""
            spawn task () :awakes {
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
                var t = spawn task () {
                    set xxx = defer { println(2) }
                    println(:111, xxx)
                    xxx
                }()
                xxx
            }
        """, true)
        assert(out == ":111\tnil\n2\n") { out }
    }
    @Test
    fun watching6_clk() {
        val out = ceu.all("""
            spawn task () :awakes {
                awaiting 10s {
                    defer { println(10) }
                    await false
                    println(1)
                }
                println(999)
            } ()
            println(0)
            broadcast in :global, tags([5000], :frame, true)
            println(1)
            broadcast in :global, tags([5000], :frame, true)
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
                    every 500ms {
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
            task T () :awakes {
                awaiting (throw(:error)) {
                    await false
                }
            }            
            spawn in coroutines(), T()
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
                var x = paror {
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
                var x = parand {
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
                task f () {
                    paror {
                        1
                    } with {
                        999
                    }
                }
                var x = await spawn f()
                println(x)
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun paror13_ret_func() {
        val out = all("""
            task T () {
                await evt==:x
            }
            spawn {
                paror {
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
                paror {
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
                paror {
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
            task T () :awakes {
                set task.pub = [10]
                await :evt
            }
            var t = coroutine(T)
            resume t ()
            var x = track(t)
            spawn {
                awaiting :check.now x {
                    println(x.pub[0])
                    broadcast in :global, nil
                    println(x.pub[0])
                    broadcast in :global, :evt
                    println(x.pub[0])   ;; never printed
                    await false
                }
                println(x.status)
            }
            println(:ok)
        """, true)
        assert(out == "10\n10\nnil\n:ok\n") { out }
    }
    @Test
    fun todo_awaiting17_track() {
        val out = all("""
            task T () :awakes {
                set task.pub = :pub
                await evt==:evt
            }
            var t = spawn T()
            var x = track(t)
            spawn {
                awaiting x {
                    broadcast in :global, :evt
                    println(x.pub)   ;; never printed
                    await false
                }
                println(x.status)
            }
            println(:ok)
        """, true)
        assert(out == "nil\n:ok\n") { out }
    }
    @Test
    fun parand18_immediate() {
        val out = all("""
            spawn task () {
                parand {
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
                paror {
                    while true { yield(nil) }
                } with {
                    paror {
                        yield()
                    } with {
                        while true { yield(nil) }
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
            var t = spawn T()
            var x = track(t)
            spawn {
                parand {
                    println(:0)
                    await x
                    println(:2)
                } with {
                    println(:1)
                    resume t()
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
            var t = spawn T()
            var x = track(t)
            spawn {
                parand {
                    println(:0)
                    await x
                    println(:2)
                } with {
                    println(:1)
                    resume t()
                }
                println(:3)
            }
            println(:4)
        """, true)
        assert(out == ":0\n:1\n:2\n:3\n:4\n") { out }
    }

    // TUPLE / VECTOR / DICT / STRING

    @Test
    fun todo_index1_tuple() {
        val out = all("""
            var t = [1,2,3]
            println(t.a, t.c)
        """)
        assert(out == "1\t3\n") { out }
    }
    @Test
    fun index2_dict() {
        val out = all("""
            var t = @[ (:x,1), (:y,2) ]
            println(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun vector3_size() {
        val out = all("""
            var v = #[]
            println(#v, v)
            set v[+] = 1
            set v[+] = 2
            println(#v, v)
            var top = v[-]
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
                true -> {
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
    fun dict10_iter() {
        val out = all("""
            var t = @[x=1, y=2, z=3]
            while in :dict t, (k, v) {
                println(k,v)
            }
        """, true)
        assert(out == ":x\t1\n:y\t2\n:z\t3\n") { out }
    }
    @Test
    fun dict10a_iter() {
        val out = all("""
            var t = @[]
            while in :dict t, (k, v) {
                println(k,v)
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dict10_iter_err() {
        val out = all("""
            var t = @[x=1, y=2, z=3]
            while in :dict t, k, v {
                println(k,v)
            }
        """, true)
        assert(out == "anon : (lin 3, col 31) : expected \"(\" : have \"k\"") { out }
    }
    @Test
    fun vect11_iter_err() {
        val out = all("""
            var t = #[1, 2, 3]
            while in :vector t, i, v {
                println(i, v)
            }
        """, true)
        assert(out == "anon : (lin 3, col 33) : expected \"(\" : have \"i\"") { out }
    }
    @Test
    fun vect11_iter() {
        val out = all("""
            var t = #[1, 2, 3]
            while in :vector t, (i, v) {
                println(i, v)
            }
        """, true)
        assert(out == "0\t1\n1\t2\n2\t3\n") { out }
    }
    @Test
    fun vect12_iter_err() {
        val out = all("""
            var t = #[1, 2, 3]
            while in :vector t, (i {
                println(i, v)
            }
        """, true)
        assert(out == "anon : (lin 3, col 36) : expected \",\" : have \"{\"") { out }
    }
    @Test
    fun dict13_iter() {
        val out = all("""
            var t = @[x=1, y=2, z=3]
            while in :dict t, (k, v) {
                println(k, v)
            }
        """, true)
        assert(out == ":x\t1\n:y\t2\n:z\t3\n") { out }
    }
    @Test
    fun dict14_iter_err() {
        val out = all("""
            while in :dict t, (i {
                println(i, v)
            }
        """, true)
        assert(out == "anon : (lin 2, col 34) : expected \",\" : have \"{\"") { out }
    }
    @Test
    fun string_concat15() {
        val out = all("""
            var s = #[]
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
            var v = ""
            println(v)
            `printf(">%s<\n", ${D}v.Dyn->Ncast.Vector.buf);`
        """)
        assert(out == "#[]\n><\n") { out }
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
                broadcast in :global, @[(:type,:y)]
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
                await evt is :x
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
            await f()
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing task") { out }
    }
    @Test
    fun await5() {
        val out = ceu.all(
            """
            spawn task () :awakes {
                while (true) {
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
            spawn task () :awakes {
                while (true) {
                    await 10s                    
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
            spawn task () :awakes {
                every 10s {
                    println(10)
                }
            }()
            println(0)
            broadcast in :global, tags([5000], :frame, true)
            println(1)
            broadcast in :global, tags([5000], :frame, true)
            println(2)
            broadcast in :global, tags([10000], :frame, true)
            println(3)
        """, true)
        assert(out == "0\n1\n10\n2\n10\n3\n") { out }
    }
    @Test
    fun todo_every9_clk_multi() { // awake twice from single bcast
        val out = ceu.all("""
            spawn task () :awakes {
                every 10s {
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
        assert(out == "anon : (lin 2, col 20) : task () :fake :awakes { var x = do { var ceu_...)\n" +
                "anon : (lin 3, col 38) : task () :fake :awakes { var y = [] y }()\n" +
                "anon : (lin 3, col 60) : set error : incompatible scopes\n" +
                ":error\n") { out }
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
                await :check.now true
                println(2)
            }
            println(3)
        """, true)
        assert(out == "1\n2\n3\n") { out }
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
        assert(out == "anon : (lin 2, col 20) : expected \"(\" : have \"{\"") { out }
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
        assert(out == "anon : (lin 5, col 38) : pub error : expected enclosing task") { out }
    }
    @Test
    fun task5_pub_fake() {
        val out = all("""
            spawn (task () :awakes {
                set task.pub = 1
                awaiting evt==:a {
                    every evt==:b {
                        println(task.pub)
                    }
                }
            }) ()
             broadcast in :global, :b
             broadcast in :global, :b
             broadcast in :global, :a
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

    // WHERE

    @Test
    fun where1() {
        val out = ceu.all(
            """
            println(x) where {
                var x = 1
            }
            var z = y + 10 where {
                var y = 20
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
            var t = (spawn T(v)) where { var v = 10 }
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 5, col 34) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun where3_err() {
        val out = ceu.all(
            """
            task T (v) {
                println(v)
            }
            (var t = spawn T(v)) where { var v = 10 }
            println(t)
        """)
        assert(out == "anon : (lin 6, col 21) : access error : variable \"t\" is not declared") { out }
    }
    @Test
    fun where4() {
        val out = ceu.all(
            """
            task T (v) {
                println(v)
            }
            (var t = spawn T(v)) where { var v = 10 }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun where5() {
        val out = ceu.all(
            """
            task T (v) {
                println(v)
            }
            var t = spawn T(v) where {
                var v = 10
            }
            println(type(t))
        """)
        assert(out == "10\n:coro\n") { out }
    }
    @Test
    fun where6() {
        val out = ceu.all(
            """
            task T (v) {
                println(v)
                yield()
            }
            var ts = coroutines()
            spawn in ts, T(v) where {
                var v = 10
            }
            while in :coros ts, t {
                println(type(t))
            }
        """)
        assert(out == "10\n:track\n") { out }
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
            task T (v) :awakes {
                set task.pub = v
                toggle evt==:hide -> evt==:show {
                    println(task.pub)
                    every (evt is :dict) and (evt.sub==:draw) {
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
            task T (v) :awakes {
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

    // WHILE / BREAK / UNTIL

    @Test
    fun break1() {
        val out = all("""
            while true { {:break}
                throw(:break)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun break2() {
        val out = all("""
            while false {
                while true { {:break}
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
            while true { {:break2}
                while true { {:break1}
                    throw(:break2)
                }
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun until4() {
        val out = all("""
            println(until {
                10
            })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun until5() {
        val out = all("""
            var x = 0
            until {
                set x = x + 1
                println(x)
                x == 3
            }
            println(99)
        """, true)
        assert(out == "1\n2\n3\n99\n") { out }
    }
    @Test
    fun until6() {
        val out = all("""
            var x = 0
            var v = until { {:break}
                set x = x + 1
                println(x)
                if x == 3 {
                    throw(:break)
                }
                false
            }
            println(v)
        """, true)
        assert(out == "1\n2\n3\n:break\n") { out }
    }

    // TASK ITERATOR

    @Test
    fun iter1() {
        val out = all("""
            task T () {
                yield(1)
                yield(2)
                yield(3)
                ;;nil
            }
            while in :coro coroutine(T), i {
                println(i)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun iter2() {
        val out = all("""
            task T () {
                yield(1)
                yield(2)
                yield(3)
            }
            while in :coro coroutine(T), i { {:x}
                println(i)
                throw(:x)
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun iter3() {
        val out = all("""
            task T () {
                yield(1)
                yield(2)
                yield(3)
            }
            while in :coro coroutine(T), i {
                println(i)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun iter3_err() {
        val out = all("""
            task T () {
                yield(1)
                yield(2)
                3
            }
            while in :coro coroutine(T), i {
                println(i)
            }
        """, true)
        //assert(out == "anon : (lin 12, col 57) : resume error : expected yielded task\n1\n2\n3\n:error\n") { out }
        assert(out == "1\n2\n") { out }

    }
    @Test
    fun iter4() {
        val out = all("""
            task T () {
                yield(1)
                yield(2)
                nil
            }
            while in :coro coroutine(T), i {
                println(i)
            }
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun iter5() {
        val out = all("""
            task T () {
                yield(1)
                yield(2)
                yield(3)
            }
            println(tovector(coroutine(T)))
        """, true)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun iter6() {
        val out = all("""
            task T () {
                yield(1)
                yield(2)
                3
            }
            var co = coroutine(T)
            println(resume co())
            println(resume co())
            println(resume co())
            println(resume co())
        """, true)
        assert(out == "anon : (lin 11, col 21) : resume error : expected yielded task\n1\n2\n3\n:error\n") { out }
    }

    // THROW / CATCH

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
            println(x)
        """, true)
        assert(out == "10\n") { out }
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
        assert(out == "anon : (lin 2, col 27) : set error : incompatible scopes\n" +
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
        assert(out == "[10]\n") { out }
    }
    @Test
    fun catch8() {
        val out = all("""
            var x
            set x = catch :x {
                var y
                set y = catch :y {
                    throw(tags([10],:y,true))
                    println(9)
                }
                ;;println(1)
                y
            }
            println(x)
        """.trimIndent(), true)
        //assert(out == "anon : (lin 9, col 5) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 2, col 18) : set error : incompatible scopes\n" +
                "anon : (lin 5, col 9) : throw(tags([10],:y,true))\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun while1() {
        val out = all("""
            println(catch :x { while true { throw(tags([1],:x,true)) }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun while2() {
        val out = all("""
            println(catch :x { while true { throw(tags([1],:x,true)) }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun while3() {
        val out = all("""
            println(catch :2 { while true { throw(tags([1],:2,true)) }})
        """, true)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun while4() {
        val out = all("""
            println(catch :x { while true {
                var x
                set x = [1] ;; memory released
                throw(tags([1],:x,true))
            }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun while5_err() {
        val out = all("""
            println(catch :x { while true {
                var x
                set x = [1]
                throw(tags(x,:x,true))
            }})
        """.trimIndent(), true)
        //assert(out == "anon : (lin 4, col 14) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 1, col 31) : set error : incompatible scopes\n" +
                "anon : (lin 4, col 5) : throw(tags(x,:x,true))\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }

    // PEEK, PUSH, POP

    @Test
    fun ppp1() {
        val out = all("""
            var v = #[1]
            set v[=] = 10
            println(v)
        """, true)
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun ppp2() {
        val out = all("""
            var v = #[10]
            println(v[=])
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ppp3() {
        val out = all("""
            var v = #[]
            set v[+] = 1
            println(v)
        """)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun ppp4_err() {
        val out = all("""
            var v = #[]
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
            var v = #[1]
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
            var x = v[-]
            println(#v, v[=], x)
        """, true)
        assert(out == "3\t3\n2\t20\t3\n") { out }
    }
    @Test
    fun ppp8_debug() {
        val out = all("""
            var v
            set v = #[10]
            println(v[#v-1])
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

    // tonumber, tostring, tovector

    @Test
    fun tostring1() {
        val out = all("""
            var s = tostring(10)
            println(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun tonumber2() {
        val out = all("""
            var n = tonumber("10")
            println(type(n), n)
        """, true)
        assert(out == ":number\t10\n") { out }
    }
    @Test
    fun tonumber_tostring3() {
        val out = all("""
            var s = tostring(tonumber("10"))
            println(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun tovector4() {
        val out = all("""
            task T() {
                yield([1])
            }
            var t = coroutine(T)
            var v = tovector(t)
            println(v)
        """, true)
        assert(out == "#[[1]]\n") { out }
    }

    // COMPOSITION

    @Test
    fun comp1() {
        val out = all("""
            func square (x) {
                x**2
            }
            var quad = square <|< square
            println(quad(3))
        """, true)
        assert(out == "81\n") { out }
    }

    // ALL

    @Test
    fun all1() {
        val out = all("""
            task T (pos) :awakes {
                await true
                println(pos)
            }
            spawn {
                var ts = coroutines()
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
            var t = spawn T ([1,2])
            println(t.pub())
        """, true)
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun all3() {
        val out = all("""
            task T () {
                do {
                    var x = []
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
                "anon : (lin 2, col 23) : set error : incompatible scopes\n:error\n") { out }
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
                while true {
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
                while true {
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
                while true {
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
            task T () :awakes {
                println(:1)
                awaiting false {
                    await true
                }
                println(:2)
                ;;println(:t)
            }
            spawn {
                var ts = coroutines()
                spawn in ts, T()
                ;;println(:every)
                every :e {
                    ;;println(:while)
                    while in :coros ts, t {
                        ;;println(t, detrack(t), detrack(t).status)
                        assert(detrack(t).status /= :terminated)
                    }
                }
            }
            ;;println(:bcast)
            broadcast in :global, :e
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
}
