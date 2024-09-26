package tst_50

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_50 {
    // DEPTH
    @Test
    fun aa_01_depth() {
        val out = test("""
            val t1 = []
            do {
                nil
            }
            val t2 = []
            println(`:number ${D}t1.Dyn->Any.lex.depth`, `:number ${D}t2.Dyn->Any.lex.depth`)
        """)
        assert(out == "1\t1\n") { out }
    }
    @Test
    fun aa_02_depth() {
        val out = test("""
            val t1 = []
            (func' () { nil })()
            val t2 = []
            println(`:number ${D}t1.Dyn->Any.lex.depth`, `:number ${D}t2.Dyn->Any.lex.depth`)
        """)
        assert(out == "1\t1\n") { out }
    }
    @Test
    fun aa_03_depth() {
        val out = test("""
            val t1 = []
            resume (coroutine(coro' () { nil })) ()
            val t2 = []
            println(`:number ${D}t1.Dyn->Any.lex.depth`, `:number ${D}t2.Dyn->Any.lex.depth`)
        """)
        assert(out == "1\t1\n") { out }
    }
    @Test
    fun aa_04_depth() {
        val out = test("""
            val t1 = []
            enclose' :X {
                loop' {
                    escape(:X, nil)
                }
            }
            val t2 = []
            println(`:number ${D}t1.Dyn->Any.lex.depth`, `:number ${D}t2.Dyn->Any.lex.depth`)
        """)
        assert(out == "1\t1\n") { out }
    }

    // BASIC

    @Test
    fun bb_01_multi() {
        val out = test("""
            val t1 = []
            val t2 = t1
            val f = func' (v) {
                println(`:number ${D}v.Dyn->Any.lex.type`, `:number ${D}v.Dyn->Any.lex.depth`)
            }
            println(`:number ${D}t1.Dyn->Any.lex.type`, `:number ${D}t1.Dyn->Any.lex.depth`)
            f(drop(t1))
            println(t1, t2)
        """)
        assert(out == "2\t1\n" +
                "1\t1\n" +
                "nil\t[]\n") { out }
    }
    @Test
    fun bb_02_acc() {
        val out = test("""
            val x = do {
                val y = []
                y
                drop(y)
            }
            println(x)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun bb_03_acc() {
        val out = test("""
            val f = func' (x) {
                x
            }
            val x = do {
                val y = []
                y
                f(drop(y))
            }
            println(x)
        """)
        assert(out == "[]\n") { out }
    }

    // COLLECTIONS

    @Test
    fun cc_01_col() {
        val out = test("""
            val t = [[10]]
            val x = drop(t[0])
            println(t, x)
        """)
        assert(out == "[nil]\t[10]\n") { out }
    }
    @Test
    fun cc_02_col() {
        val out = test("""
            val y = do {
                val t = [[10]]
                val' x = drop(t[0])
                x
            }
            println(y)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun cc_03_col() {
        val out = test("""
            val x = do {
                val t = [[10]]
                drop(t[0])
            }
            println(x)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun cc_04_col_err() {
        val out = test("""
            val y = do {
                val t = [[10]]
                t[0]
            }
            println(y)
        """)
        assert(out == " |  anon : (lin 2, col 13) : (val y = do { (val t = [[10]]); t[0]; })\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun cc_05_col() {
        val out = test("""
            val f = func' (inp) {
                val out = [inp[0]]
                drop(out)
            }
            val v = []
            val t = f([v])
            println(v == t[0])
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun cc_06_col() {
        val out = test("""
            val copy = func' (vec) {
                val ret = #[]
                set ret[0] = vec[0]
                drop(ret)
            }
            func' () {
                val f = func' :nested () {
                    nil
                }
                val t = copy([f])
                println(f == t[0])
            } ()
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun cc_10_col() {   // TODO: criacao de closure faz upval virar MUTAB
        val out = test("""
            val f = func' (t, v) {
                val' t' = t
                set t[0] = v
                set t'[0] = v
                t
            }
            val v = f([nil], [])
            println(v)
        """)
        assert(out == "[[]]\n") { out }
    }

    // PROTOS / COROS

    @Test
    fun hh_01_coro() {
        val out = test("""
            val CO = coro' () {
                yield([1,2])
                yield([3,4])
            }
            val co = coroutine(CO)
            enclose' :break {
                loop' {
                    val v = resume co()
                    if status(co) == :terminated {
                        escape(:break,nil)
                    } else {
                        nil
                    }
                    print(v)
                }
            }
            println()
        """)
        assert(out == "[1,2][3,4]\n") { out }
    }
    @Test
    fun hh_02_coro_upval() {
        val out = test("""
            val F = func' (x) {
                coro' () {
                    yield(drop(x))  ;; x is an upval
                }
            }
            ;;do {
                val x = []
                val CO = F(drop(x))
                val co = coroutine(CO)
                val t = resume co()
                println(t)
            ;;}
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun hh_03_coro_upval() {
        val out = test("""
            val F = func' (x) {
                coro' () {
                    yield(drop(x))  ;; x is an upval
                }
            }
            do {
                val x = []
                val CO = F(drop(x))
                val co = coroutine(CO)
                resume co()
            }
        """)
        assert(out == " |  anon : (lin 11, col 17) : (resume (co)())\n" +
                " |  anon : (lin 4, col 21) : yield(drop(x))\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun hh_04_func_upval() {
        val out = test("""
            val F = func' (x) {
                func' () {
                    (drop(x))  ;; x is an upval
                }
            }
            do {
                val x = []
                val f = F(drop(x))
                println(f())
            }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun hh_04x_func_upval() {
        val out = test("""
            val F = func' (x) {
                func' () {
                    drop(x)  ;; x is an upval
                }
            }
            val f = F([])
            val t = f()
            println(t)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun hh_05_coro() {
        val out = test("""
            val CO = coro' () {
                val x = [99]
                do {
                    yield(drop'(x))
                }
            }
            val co = coroutine(CO)
            val x = resume co()
            println(x)
        """)
        assert(out == "[99]\n") { out }
    }
    @Test
    fun hh_06_coro_err() {
        val out = test("""
            val f = func' (co1, xco2) {
                val' xco1 = coroutine(CO1)
                resume xco1()
            }
            val CO2 = coro' () {
                nil
            }
            val CO1 = coro' () {
                var x
                do {
                    val y = []
                    set x = y
                }
            }
            println(resume (f(CO1, coroutine(CO2))) ())
        """)
        assert(out == " |  anon : (lin 16, col 29) : f(CO1,coroutine(CO2))\n" +
                " |  anon : (lin 4, col 17) : (resume (xco1)())\n" +
                " |  anon : (lin 13, col 25) : x\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun hh_07_coro_depth() {
        val out = test("""
            val CO = coro' (x) {
                println(x)
            }
            val co = coroutine(CO)
            do {    ;; block optimized out
                val t = []
                do {
                    resume co(drop(t))
                }
            }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun hh_07x_coro_depth() {
        val out = test("""
            val CO = coro' (x) {
                println(x)
            }
            val co = coroutine(CO)
            do {
                val t = []
                do {
                    resume co(drop(t))
                    val x
                }
            }
        """)
        assert(out == " |  anon : (lin 9, col 21) : (resume (co)(drop(t)))\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun hh_08_coro_depth() {
        val out = test("""
            val CO = coro' (x) {
                println(x)
            }
            val co = coroutine(CO)
            do {
                val t = []
                do {
                    resume co(drop'(t))
                }
            }
        """)
        assert(out == "[]\n") { out }
    }

    // NESTED

    @Test
    fun nn_01_nested() {
        val out = test("""
            spawn (task' () {
                val t1 = spawn (task' :nested () {
                    println(:ok)
                }) ()
                nil
            }) ()
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun nn_02_nested() {
        val out = test("""
            val f = do {
                func' :nested () {
                    nil
                }
                val x
            }
            println(:no)
        """)
        //assert(out == "anon : (lin 3, col 17) : :nested error : expected enclosing prototype\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == " |  anon : (lin 2, col 13) : (val f = do { (func' :nested () { nil; });...\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun nn_02x_nested() {
        val out = test("""
            val f = do {
                func' :nested () {
                    nil
                }
            }
            println(:ok)
        """)
        //assert(out == "anon : (lin 3, col 17) : :nested error : expected enclosing prototype\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun nn_03_nested() {
        val out = test("""
            ;;do {
                var x = 10
                val g = func' :nested () {
                    set x = 100
                }
                g()
                println(x)
            ;;}
        """
        )
        assert(out == "100\n") { out }
        //assert(out == "anon : (lin 4, col 30) : expected \"(\" : have \":nested\"\n") { out }
    }
    @Test
    fun nn_04_nested() {
        val out = test("""
            val f = func' () { 
                var x = 10
                val g = func' :nested () {
                    set x = 100
                }
                g()
                println(x)
            }
            f()
        """
        )
        assert(out == "100\n") { out }
        //assert(out == "anon : (lin 4, col 30) : expected \"(\" : have \":nested\"\n") { out }
    }
    @Test
    fun nn_05_nested() {
        val out = test("""
            val f = func' (x) { 
                val g = func' :nested () {
                    println(x)
                }
                g()
            }
            f(10)
        """
        )
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 4, col 30) : expected \"(\" : have \":nested\"\n") { out }
    }
    @Test
    fun nn_06_nested() {
        val out = test("""
            spawn (task' () {
                val T = task' :nested () {
                    set pub = [99]
                    println(:A, pub)
                    spawn (task' :fake () {
                        println(:B, pub)
                    }) ()
                    yield(nil)
                }
                val t = spawn T()
                println(:C, t.pub)
            }) ()
        """
        )
        assert(out == ":A\t[99]\n" +
                ":B\t[99]\n" +
                ":C\t[99]\n") { out }
    }
    @Test
    fun nn_07_nested() {
        val out = test("""
            val T = task' () {
                val t = 10
                val S = task' :nested () {
                    println(t)
                }
                spawn (task' :nested () {
                    spawn S()
                }) ()
            }
            spawn T()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_08_nested() {
        val out = test("""
            val T = func' () {
                val t = 10
                val S = func' :nested () {
                    println(t)
                }
                (func' :nested () {
                    S()
                }) ()
            }
            T()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_09_nested() {
        val out = test("""
            val T = coro' () {
                val t = 10
                val S = coro' :nested () {
                    println(t)
                }
                resume coroutine(coro' :nested () {
                    resume coroutine(S)()
                }) ()
            }
            resume coroutine(T)()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_10_nested() {
        val out = test("""
            val T = task' () {
                set pub = 10
                val S = task' :fake () {
                    println(pub)
                }
                spawn (task' :fake () {
                    spawn S()
                }) ()
                spawn (task' () {
                    spawn S()
                }) ()
            }
            spawn T()
        """)
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun nn_11_nested() {
        val out = test("""
            spawn (task' () {
                val T = task' () {
                    set pub = [10]
                    spawn (task' :fake () {
                        println(pub[0])
                    }) ()
                }
                spawn T()
            }) ()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_12_nested() {
        val out = test("""
            spawn (task' :fake () {
                val T = task' :nested () {
                    set pub = [10]
                    spawn (task' :fake () {
                        println(pub[0])
                    }) ()
                }
                spawn T()
            }) ()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun TODO_nn_13_nest_yield_func() {
        val out = test("""
            spawn (task' () {
                val f = func' :nested () {
                    nil
                }
                yield(nil)
                f()
            }) ()
            broadcast(nil)
            println(:ok)
        """)
        assert(out == "anon : (lin 3, col 25) : TODO - nested function with enclosing coro/task\n") { out }
    }
    @Test
    fun TODO_nn_14_nest_task_func() {
        val out = test("""
            spawn (task' () {
                val f = func' :nested () {
                    set pub = :pub
                }
                yield(nil)
                f()
                println(pub)
            }) ()
            broadcast(nil)
        """)
        assert(out == "anon : (lin 3, col 25) : TODO - nested function with enclosing coro/task\n") { out }
    }
    @Test
    fun TODO_nn_15_nest_task_func() {
        val out = test("""
            spawn (task' () {
                val x = :x
                val f = func' :nested () {
                    println(x)
                }
                yield(nil)
                f()
            }) ()
            broadcast(nil)
        """)
        assert(out == "anon : (lin 4, col 25) : TODO - nested function with enclosing coro/task\n") { out }
    }
    @Test
    fun nn_16_nest_task_func() {
        val out = test("""
            spawn (task' () {
                val x = :x
                val f = func' :nested () {
                    println(:ok)
                }
                yield(nil)
                f()
            }) ()
            broadcast(nil)
        """)
        //assert(out == "anon : (lin 4, col 25) : func :nested error : unexpected enclosing task\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun nn_17_nest_rec() {
        val out = test("""
            $PLUS
            do {
                val f = func' :nested (v) {
                    ;;println(:F, f)      ;; f is upval which is assigned nil
                    if v /= 0 {
                        println(v)
                        f(v - 1)
                    } else {
                        nil
                    }
                }
                f(3)
            }
        """)
        assert(out == "3\n2\n1\n") { out }
    }
    @Test
    fun nn_18_nest_rec() {
        val out = test("""
            spawn (task' () {
                val f = func' :nested () {
                    nil
                }
                yield(nil)
                f()
            }) ()
            broadcast(nil)
            println(:ok)
        """)
        //assert(out == "anon : (lin 3, col 25) : func :nested error : unexpected enclosing task\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun nn_19_nested() {
        val out = test("""
            spawn (task' () {
                val fff = func' :nested () {
                    println(:ok)
                }
                val T = task' () {
                    fff()
                }
                yield(nil)
                spawn T()
                yield(nil)
            }) ()
            broadcast(nil)
        """)
        assert(out == ":ok\n") { out }
    }

    // FAKE

    @Test
    fun lm_02_fake() {
        val out = test("""
            spawn (task' () {
                set pub = 10
                val x = [99]
                spawn (task' :fake () {
                    set x[0] = pub
                }) ()
                println(x)
            }) ()
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun lm_03_fake() {
        val out = test("""
            val T = task' () {
                val t = 10
                val S = task' :fake () {
                    println(t)
                }
                spawn (task' :fake () {
                    spawn S()
                }) ()
            }
            spawn T()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun lm_04_fake() {
        val out = test("""
            spawn (task' () {
                val T = task' () {
                    set pub = [10]
                    spawn (task' :fake () {
                        println(pub[0])
                    }) ()
                }
                spawn T()
            }) ()
        """)
        assert(out == "10\n") { out }
    }

    // ORIGINAL / NESTED

    @Test
    fun zna_01_set() {
        val out = test(
            """
            spawn( task' () {
                var t = [1]
                spawn( task' :nested () {
                    set t = [2]
                }) ()
                println(t)
            } )()
        """
        )
        assert(out == "[2]\n") { out }
    }
    @Test
    fun zna_02_set() {
        val out = test(
            """
            spawn( task' () {
                var t = [1]
                spawn (task' :nested () {
                    yield(nil) ;;thus { it => nil }
                    set t = [2]
                } )()
                yield(nil) ;;thus { it => nil }
                println(t)
            }) ()
            broadcast(nil)
        """
        )
        assert(out == "[2]\n") { out }
    }
    @Test
    fun zna_03_set() {
        val out = test(
            """
            spawn( task' () {
                val t = [1]
                ;;func' () {
                    set t[0] = 2
                ;;} ()
                println(t)
            }) ()
        """
        )
        assert(out == "[2]\n") { out }
    }
    @Test
    fun TODO_zna_04_set() {
        val out = test(
            """
            spawn (task' () {
                var t = [1]
                spawn( task' :nested () {
                    func' (it) {
                        set t = copy(it)    ;; TODO: func -> nested -> task
                    } (yield(nil))
                }) ()
                yield(nil) ;;thus { it => nil }
                println(t)
            }) ()
            broadcast ([1])
        """, true
        )
        //assert(out == "[1]\n") { out }
        assert(out == "anon : (lin 6, col 29) : access error : outer variable \"t\" must be immutable\n") { out }
    }
    @Test
    fun zna_05_set() {
        val out = test(
            """
            spawn (task' () {
                var t = [1]
                spawn( task' :nested () {
                    set t = copy(yield(nil))
                }) ()
                yield(nil) ;;thus { it => nil }
                println(t)
            } )()
            broadcast ([1])
        """, true
        )
        assert(out == "[1]\n") { out }
    }
    @Test
    fun zna_06_set() {
        val out = test(
            """
            var ang = 0
            enclose' :break {
                loop' {
                    escape(:break, nil) ;; if true
                    ang
                }
            }
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 4, col 17) : loop error : innocuous last expression\n") { out }
    }
    @Test
    fun zna_07_nst() {
        val out = test(
            """
            val T = task' (t) {
                var ang = 0
                spawn (task' :nested () {
                    set ang = 10
                })()
                println(ang)
            }
            spawn T()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun zna_08_escape() {
        val out = test("""
            spawn (task' () {
                val v = enclose' :X {
                    spawn (task' :fake () {
                        escape(:X,:ok)
                    })()
                    loop' {
                        yield(nil)
                    }
                    nil
                }
                println(v)
            }) ()
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zna_09_escape() {
        val out = test("""
            spawn (task' () {
                val v = enclose' :X {
                    spawn (task' :fake () {
                        defer {
                            println(:def)
                        }
                        escape(:X,:ok)
                    })()
                }
                println(v)
            }) ()
        """)
        assert(out == ":def\n:ok\n") { out }
    }
    @Test
    fun zna_10_escape() {
        val out = test("""
            spawn (task' () {
                val v = catch :X {
                    spawn (task' :nested () {
                        defer {
                            println(:def)
                        }
                        error(:X,:ok)
                    })()
                }
                println(v)
            }) ()
        """)
        assert(out == ":def\n:ok\n") { out }
    }
    @Test
    fun zna_11_escape() {
        val out = test("""
            spawn (task' () {
                val v = enclose' :X {
                    spawn (task' :fake () {
                        yield(nil)
                        escape(:X,:ok)
                    })()
                    yield(nil)
                }
                println(v)
            }) ()
            broadcast(nil)
        """)
        assert(out == ":ok\n") { out }
    }

    @Test
    fun znb_01_pub_err() {
        val out = test(
            """
            task' :nested () {
                pub
            }
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
        //assert(out == " v  anon : (lin 2, col 13) : pub() : pub error : expected task\n") { out }
        //assert(out == "anon : (lin 3, col 17) : pub error : expected enclosing task\n") { out }
        //assert(out == "anon : (lin 2, col 13) : task :nested error : expected enclosing task\n") { out }
    }

    @Test
    fun znc_01_nested() {
        val out = test(
            """
            task' :nested () {
                nil
            }
            println(:ok)
        """
        )
        //assert(out == "anon : (lin 2, col 13) : task :nested error : expected enclosing spawn\n") { out }
        //assert(out == "anon : (lin 2, col 13) : task :nested error : expected enclosing task\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun znc_02_nested() {
        val out = test(
            """
            spawn (task' () {
                var xxx = 1
                yield(nil)
                spawn(task' :nested () {
                    set xxx = 10
                }) ()
                println(xxx)
            } )()
            broadcast(nil)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun znc_03_nested() {
        val out = test(
            """
            spawn (task' () {
                var xxx = 1
                yield(nil)
                spawn( task' :nested () {
                    set xxx = 10
                }) ()
                println(xxx)
            } )()
            do {
                broadcast(nil)
            }
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun znc_04_nested_err() {
        val out = test(
            """
            spawn (task' () {
                var xxx = 1
                yield(nil)
                spawn(task' () {     ;; ERROR: crosses non :nested
                    spawn(task' :nested () {
                        set xxx = 10
                    }) ()
                }) ()
                println(xxx)
            } )()
            broadcast(nil)
        """
        )
        //assert(out == "ERROR\n") { out }
        assert(out == "anon : (lin 7, col 29) : access error : outer variable \"xxx\" must be immutable\n") { out }
    }
    @Test
    fun znc_05_nested_err() {
        val out = test(
            """
            spawn (task' :nested () {
                nil
            } )()
            println(:ok)
        """
        )
        //assert(out == "anon : (lin 2, col 20) : task :nested error : expected enclosing task\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun BUG_znc_06_bcast_in() {  // bcast in outer of :nested
        val out = test(
            """
            var T
            set T = task' (v) {
                spawn (task' :nested () {
                    val evt = yield(nil)
                    println(v, evt)
                }) ()
                spawn (task' :nested () {
                    do {
                        broadcast(:ok) in :task
                    }
                }) ()
                yield(nil)
                println(:err)
            }
            spawn (task' () {
                yield(nil)
                println(:err)
            }) ()
            spawn T (1)
            spawn T (2)
        """
        )
        assert(out == "1\t:ok\n2\t:ok\n") { out }
    }
    @Test
    fun znc_07_pub_fake_task() {
        val out = test("""
            spawn (task' () {
                set pub = 1
                spawn (task' :fake () {
                    println(pub)
                }) ()
                nil
            }) ()
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun znc_08_pub_fake_task_err() {
        val out = test("""
            spawn (task' () {
                set pub = []
                var x
                spawn (task' :fake () {
                    set x = pub
                }) ()
                println(x)
            }) ()
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 2, col 20) : task () { set task.pub = [] var x spa...)\n" +
        //        "anon : (lin 5, col 24) : task () :fake { set x = task.pub }()\n" +
        //        "anon : (lin 6, col 34) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
        //assert(out == "anon : (lin 2, col 20) : task () { set task.pub = [] var x spawn task ...)\n" +
        //        "anon : (lin 5, col 24) : task () :fake { set x = task.pub }()\n" +
        //        "anon : (lin 6, col 25) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun znc_09_pub_fake_task() {
        val out = test("""
            spawn (task' () {
                set pub = [10]
                var x
                spawn (task' :fake () {
                    set x = pub[0]
                }) ()
                println(x)
            }) ()
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun znc_10_pub_fake_err() {
        val out = test("""
            spawn (task' :nested () {
                println(pub)
            }) ()
        """)
        assert(out == "nil\n") { out }
        //assert(out == "anon : (lin 3, col 17) : pub error : expected enclosing task\n") { out }
        //assert(out == "anon : (lin 3, col 17) : task error : missing enclosing task") { out }
        //assert(out == "anon : (lin 2, col 20) : task :nested error : expected enclosing task\n") { out }
    }
    @Test
    fun znc_11_xceu3() {
        val out = test("""
            spawn task' () {
                var evt = yield(nil)
                println(evt)
                spawn (task' :nested () {
                    loop' {
                        println(evt)    ;; kept reference
                        set evt = yield(nil)
                    }
                }) ()
                set evt = yield(nil)
            }()
            broadcast (10)
            broadcast (20)
        """)
        assert(out == "10\n10\n20\n") { out }
        //assert(out == "anon : (lin 14, col 25) : set error : incompatible scopes\n") { out }
    }

    @Test
    fun znd_01_bcast_err() {
        val out = test("""
            spawn (task' :nested () {
                broadcast(nil) in :task
            }) ()
            println(:ok)
        """)
        //assert(out == "anon : (lin 2, col 20) : (task () :fake { broadcast in :task, nil })()\n" +
        //        "anon : (lin 3, col 30) : broadcast error : invalid target\n:error\n") { out }
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 2, col 20) : task :nested error : expected enclosing task\n") { out }
    }
    @Test
    fun znd_02_throw_fake() {
        val out = test("""
            spawn (task' () {
                catch :err ;;;(err|err==:err);;; {
                    spawn (task' :nested () {
                        error(:err)
                    }) ()
                }
                println(10)
            }) ()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun znd_03_throw_fake() {
        val out = test("""
            spawn task' () { 
                catch :err ;;;(err|{{==}}(err,:err));;; {
                    spawn (task' :nested () {
                        yield(nil)
                        error(:err)
                    })()
                    yield(nil)
                }
            }() 
            broadcast(nil)
            println(10)
        """)
        assert(out == "10\n") { out }
    }

    @Test
    fun zne_01_anon() {
        val out = test(
            """
            spawn (task' () {
                do {
                    println(:xxx, pub)
                    spawn (task' :nested () {
                        println(:yyy, pub)
                        yield(nil) ;;thus { it => nil }
                    }) ()
                    yield(nil) ;;thus { it => nil }
                }
                yield(nil) ;;thus { it => nil }
            }) ()
       """
        )
        assert(out == ":xxx\tnil\n:yyy\tnil\n") { out }
    }
    @Test
    fun zne_02_anon() {
        val out = test(
            """
            var T
            set T = task' () {
                spawn (task' :nested () {
                    println(1)
                    nil
                }) ()
                nil
            }
            spawn T()
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 8, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == "anon : (lin 9, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n" +
        //        "1\n" +
        //        ":error\n") { out }
    }
    @Test
    fun zne_03_anon() {
        val out = test(
            """
            var T
            set T = task' () {
                spawn (task' :nested () {
                    (999)
                })()
                nil
            }
            spawn T()
            println(1)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 8, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
    }

    @Test
    fun znf_01_99() {
        val out = test("""
            spawn (task' (v) {
                spawn (task' :nested () {
                    println(v)
                }) ()
            }) (100)
            println(:ok)
        """)
        assert(out == "100\n:ok\n") { out }
    }
    @Test
    fun znf_02_hh_pub_task() {
        val out = test("""
        spawn task' () { 
            var y
            set y = do {     
                var ceu_spw_54     
                set ceu_spw_54 = spawn task' :nested () {         
                    yield(nil)         
                    [2]             
                }()        
                yield(nil)     
                ;;println(ceu_spw_54.pub)     
                ceu_spw_54.pub        
            }     
            println(y) 
        }()
        broadcast( nil )
        """)
        assert(out == "[2]\n") { out }
        //assert(out == "anon : (lin 16, col 9) : broadcast nil\n" +
        //        "anon : (lin 12, col 28) : invalid pub : cannot expose dynamic \"pub\" field\n:error\n") { out }
    }
    @Test
    fun znf_03_anon() {
        val out = test(
            """
            data :X = [x]
            val T = task' () :X {
                set pub = [10]
                spawn (task' :fake () {
                    ;;println(pub)
                    println(pub.x)
                }) ()
                nil
            }
            spawn T()
       """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun znf_04_xceu () {
        val out = test("""
            data :X = [x]
            task' () :X {
                task' :nested () {
                    ;;;task.;;;pub.x
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // ORIGINAL

    @Test
    fun cc_tuple7_hold_err() {
        val out = test("""
            val f = func' (v) {
                var x
                if v > 0 {
                    set x = f(v - 1)
                    [x]     ;; set error: cannot return "var x" from this scope
                } else {
                    0
                }
            }
            println(f(3))
        """, true)
        assert(out == " |  anon : (lin 11, col 21) : f(3)\n |  anon : (lin 5, col 25) : x\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 3, col 30) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[[[0]]]\n") { out }
    }
    @Test
    fun cc_tuple8_hold_err() {
        val out = test(
            """
            val f = func' (v) {
                if v > 0 {
                    val x = f(v - 1)
                    [x] ;; invalid return
                } else {
                    0
                }
            }
            println(f(3))
        """, true
        )
        //assert(out == "anon : (lin 4, col 26) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 10, col 21) : f(3)\n |  anon : (lin 4, col 21) : (val x = f({{-}}(v,1)))\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[[[0]]]\n") { out }
    }
    @Test
    fun cc_tuple14_drop_out() {
        val out = test(
            """
            val out = do {
                val ins = [1,2,3]
                drop(ins)
            }
            println(out)
        """
        )
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun cm_01x_drop () {
        val out = test(
            """
            val g = do {
                val v = do {
                    val x = [0,'a']
                    drop(x)
                }
                drop(v)
            }
            println(g)
        """
        )
        assert(out == "[0,a]\n") { out }
    }
    @Test
    fun cc_07_global() {
        //DEBUG = true
        val out = test("""
            val e = func' () {nil}
            ;;dump(e)
            val g = func' () {
                val co = [e]
                drop(co)
            }
            val x = g()
            println(x)
        """)
        assert(out.contains("[func: 0x")) { out }
    }
    @Test
    fun cc_07x_global() {
        val out = test("""
            val e = func' () {nil}
            val g = func' () {
                val co = [e]
                println(:e,e)
                ;;dump(co)
                drop(co)
            }
            val x = g()
            println(x)
        """)
        assert(out.contains("[func: 0x")) { out }
    }
    @Test
    fun cc_10_drop_multi_err() {
        val out = test("""
            val x = do {
                val t1 = [1,2,3]
                val t2 = t1
                drop(t1)        ;; ~ERR~: `t1` has multiple references
            }                   ;; not a problem b/c gc_dec does not chk current block
            println(x)
        """)
        //assert(out == " |  anon : (lin 5, col 17) : drop(t1)\n v  error : value has multiple references\n") { out }
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun cc_10_drop_multi_err_why() {
        val out = test("""
            val t = [1,[99],3]
            do {
                val y = do {
                    val x = t[1]
                    drop(x)
                }
                println(y)
            }
            ;;`ceu_gc_collect();`
            println(t)
        """)
        //assert(out == " |  anon : (lin 4, col 17) : (val y = do { (val x = t[1]); drop(x); })\n" +
        //        " v  error : dropped value has pending outer reference\n") { out }
        //assert(out == "anon : (lin 4, col 25) : block escape error : cannot move pending reference in\n") { out }
        assert(out == "[99]\n" +
                "[1,[99],3]\n") { out }
    }
    @Test
    fun cc_10y_drop_multi_err_why() {
        val out = test("""
            val x = [[99]]          ;; 3. [99] would be pending here
            ;;dump(x)
            ;;dump(x[0])
            do {
                val y = do {        ;; 2. [99] is captured and will be released in this block
                    val z = x[0]
                    drop(z)         ;; 1. [99] is dropped
                }
                ;;dump(y)
                println(y)
            }
            ;;`ceu_gc_collect();`
            println(x)
        """)
        //assert(out == " |  anon : (lin 6, col 17) : (val y = do { (val z = x[0]); drop(z); })\n" +
        //        " v  error : dropped value has pending outer reference\n") { out }
        //assert(out == " |  anon : (lin 6, col 21) : drop(x)\n v  error : value has multiple references\n") { out }
        //assert(out == "anon : (lin 4, col 25) : block escape error : cannot move pending reference in\n") { out }
        assert(out == "[99]\n" +
                "[[99]]\n") { out }
    }
    @Test
    fun cc_10x_drop_multi_err_why() {
        val out = test("""
            val t = [99]
            do {
                val y = do {
                    val x = t
                    drop(x)
                }
                println(y)
            }
            ;;`ceu_gc_collect();`
            println(t)
        """)
        //assert(out == " |  anon : (lin 4, col 17) : (val y = do { (val x = t); drop(x); })\n" +
        //        " v  error : dropped value has pending outer reference\n") { out }
        //assert(out == " |  anon : (lin 6, col 21) : drop(x)\n v  error : value has multiple references\n") { out }
        //assert(out == "anon : (lin 4, col 25) : block escape error : cannot move pending reference in\n") { out }
        assert(out == "[99]\n" +
                "[99]\n") { out }
    }
    @Test
    fun cc_13_drop_cycle() {
        val out = test(
            """
            val z = do {
                var x = [nil]
                var y = [x]
                set x[0] = y
                drop(x)
            }
            println(z[0][0] == z)
        """
        )
        //assert(out == " |  anon : (lin 6, col 17) : drop(x)\n v  error : value has multiple references\n") { out }
        assert(out == "true\n") { out }
    }
    @Test
    fun cc_13_drop_cycle_x() {
        val out = test(
            """
            val z = do {
                var x = [nil]
                var y = [x]
                set x[0] = y
                drop(x)
            }
            println(z[0][0] == z)
        """
        )
        assert(out == "true\n") { out }
        //assert(out == " |  anon : (lin 6, col 17) : drop(x)\n" +
        //        " v  error : value has multiple references\n") { out }
    }
    @Test
    fun cc_14_drop_cycle() {
        val out = test(
            """
            val z = do {
                var x = [nil]
                var y = [x]
                set x[0] = y
                drop(x)
                y
            }
            println(z[0][0] == z)
        """
        )
        //assert(out == " |  anon : (lin 6, col 17) : drop(x)\n" +
        //        " v  error : value has multiple references\n") { out }
        assert(out == "true\n") { out }
    }

    // LEX / VAL' / VAR'

    @Test
    fun cd_01_val_prime() {
        val out = test("""
            var x
            do {
                val y = [99]
                set x = y
            }
            println(x)
        """)
        assert(out == " |  anon : (lin 5, col 21) : x\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun cd_02_val_prime_err() {
        val out = test("""
            var x
            do {
                val' y = [99]
                set x = y
            }
            println(x)
        """)
        assert(out == "[99]\n") { out }
    }
    @Test
    fun cd_03_val_prime() {
        val out = test("""
            println(do {
                (val' xxx = true);
                xxx
            });
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun cd_04_val_prime() {
        val out = test("""
            println(do {
                (val' xxx = true);
            });
        """)
        assert(out == "true\n") { out }
    }

    @Test
    fun dd_12_dict_set_err() {
        val out = test(
            """
            val v = @[]
            do {
                val k = []
                set v[k] = true
            }
            println(v)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : store error : cannot assign reference to outer scope\n") { out }
        //assert(out == "@[([],true)]\n") { out }
        assert(out == " |  anon : (lin 5, col 21) : v[k]\n" +
                " v  error : cannot copy reference out\n") { out }
    }

    @Test
    fun scope_err2() {
        val out = test(
            """
            var x
            do {
                var a
                set a = [1,2,3]
                set x = a           ;; err: x<a
            }
            println(x)
        """
        )
        //assert(out == "anon : (lin 3, col 13) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set error : cannot assign reference to outer scope\n") { out }
        //assert(out == "[1,2,3]\n") { out }
        assert(out == " |  anon : (lin 6, col 21) : x\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun scope5_err() {
        val out = test("""
            var x
            do {
                set x = [1,2,3]
                var y
                set y = [10,20,30]
                set x[2] = y
            }
            println(x)
        """)
        assert(out == " |  anon : (lin 7, col 21) : x[2]\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 7, col 21) : store error : cannot assign reference to outer scope\n") { out }
        //assert(out == "[1,2,[10,20,30]]\n") { out }
    }
    @Test
    fun scope9_err() {
        val out = test(
            """
            var x
            do {
                var a
                set a = @[(1,[])]
                set x = a
            }
            println(x)
        """
        )
        assert(out == " |  anon : (lin 6, col 21) : x\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "@[(1,[])]\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set error : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun scope10x() {
        DEBUG = true
        val out = test(
            """
            var out
            do {
                var x
                set x = []
                ;;dump(x)
                set out = [x]   ;; err
            }
            println(1)
        """
        )
        assert(out == " |  anon : (lin 7, col 21) : out\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "1\n") { out }
        //assert(out == "anon : (lin 7, col 21) : set error : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun scope10_err() {
        val out = test("""
            var out
            do {
                val x = []
                set out = [x]   ;; err
            }
            println(1)
        """)
        assert(out == " |  anon : (lin 5, col 21) : out\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 5, col 21) : set error : cannot assign reference to outer scope\n") { out }
        //assert(out == "1\n") { out }
    }
    @Test
    fun scope11_err() {
        val out = test(
            """
            var out
            do {
                var x
                set x = []
                set out = #[x]
            }
            println(1)
        """
        )
        assert(out == " |  anon : (lin 6, col 21) : out\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set error : cannot assign reference to outer scope\n") { out }
        //assert(out == "1\n") { out }
    }
    @Test
    fun scope12_err() {
        val out = test(
            """
            var out
            do {
                var x
                set x = []
                set out = @[(1,x)]
            }
            println(1)
        """
        )
        assert(out == " |  anon : (lin 6, col 21) : out\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set error : cannot assign reference to outer scope\n") { out }
        //assert(out == "1\n") { out }
    }
    @Test
    fun scope13_tuple_err() {
        val out = test(
            """
            val v = do {
                val x = []
                [x]         ;; invalid return
            }
            println(v)
        """
        )
        assert(out == " |  anon : (lin 2, col 13) : (val v = do { (val x = []); [x]; })\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 2, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[[]]\n") { out }
    }
    @Test
    fun scope22a_tup() {
        val out = test(
            """
            val d = [nil]
            do {
                val t2 = []
                set d[0] = t2
                nil
            }
            println(1)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : store error : cannot assign reference to outer scope\n") { out }
        //assert(out == "1\n") { out }
        assert(out == " |  anon : (lin 5, col 21) : d[0]\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun scope22b_vec() {
        val out = test(
            """
            val d = [nil]
            do {
                val t2 = #[]
                set d[0] = t2
                nil
            }
            println(1)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : store error : cannot assign reference to outer scope\n") { out }
        //assert(out == "1\n") { out }
        assert(out == " |  anon : (lin 5, col 21) : d[0]\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun scope22c_dic() {
        val out = test(
            """
            val d = @[]
            do {
                val t2 = []
                set d[t2] = 10
                nil
            }
            println(1)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : store error : cannot assign reference to outer scope\n") { out }
        //assert(out == "1\n") { out }
        assert(out == " |  anon : (lin 5, col 21) : d[t2]\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun scope22d_dic() {
        val out = test(
            """
            val d = @[]
            do {
                val t2 = []
                set d[10] = t2
                nil
            }
            println(1)
        """
        )
        assert(out == " |  anon : (lin 5, col 21) : d[10]\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "1\n") { out }
        //assert(out == "anon : (lin 5, col 21) : store error : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun scope22x_dict() {
        val out = test(
            """
            do {
                val t1 = []
                val d = @[(t1,t1)]
                do {
                    val t2 = []
                    set d[t2] = t2
                    nil
                }
            }
            println(:ok)
        """
        )
        //assert(out == "anon : (lin 7, col 25) : store error : cannot assign reference to outer scope\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == " |  anon : (lin 7, col 25) : d[t2]\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun scope22y_dict() {
        val out = test(
            """
            val t1 = []
            val d = @[(t1,t1)]
            do {
                val t2 = []
                set d[:x] = t2
                nil
            }
            println(:ok)
        """
        )
        assert(out == " |  anon : (lin 6, col 21) : d[:x]\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 6, col 21) : store error : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun scope26x_args_err() {
        val out = test(
            """
            val f = func' (v) {
                [v, [2]]
            }
            val y = do {
                val v = [1]
                val x = f(v)
                x
            }
            println(y)
        """
        )
        assert(out == " |  anon : (lin 5, col 13) : (val y = do { (val v = [1]); (val x = f(v)...\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 5, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[[1],[2]]\n") { out }
    }
    @Test
    fun scope28_err() {
        val out = test(
            """
            var f = func' (v) {
                [v]
            }
            var g = do {
                val t = [1]
                f(t)
            }
            println(g)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 5, col 13) : (var g = do { (val t = [1]); f(t); })\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[[1]]\n") { out }
    }
    @Test
    fun scope30_cyc() {
        val out = test("""
            val cycle = func' (v) {
                set v[3] = v
                v
            }
            var a = [1]
            var d = do {
                var b = [2]
                var c = cycle([a,b,[3],nil])
                drop(c)
            }
            ;;println(d)  ;; OK: [[1],[2],[3],*]
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : drop(c)\n" +
        //        " v  error : value has multiple references\n") { out }
    }
    @Test
    fun scope30x_cyc() {
        val out = test("""
            val cycle = func' (v) {
                set v[3] = v
                v
            }
            var a = [1]
            var d = do {
                var b = [2]
                var c = cycle([a,b,[3],nil])
                drop(c)
            }
            ;;println(d)  ;; OK: [[1],[2],[3],*]
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : drop(c)\n" +
        //        " v  error : value has multiple references\n") { out }
    }
    @Test
    fun scope31_xxx() {
        val out = test("""
            (val x = do {
                (var' ceu_ret_8);
                (val' ceu_val_8 = []);
                do {
                    (val' ceu_or_66 = do {
                        (val' it = ceu_val_8);
                        if true {
                            (set ceu_ret_8 = it);
                            true;
                        } else {
                            nil;
                        };
                    });
                    if ceu_or_66 {
                        ceu_or_66;
                    } else {
                        nil;
                    };
                };
                ceu_ret_8;
            });
            println(x);
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun scope_32() {
        val out = test("""
            val' x = do {
                val y = []
                y
            }
            println(x)
        """)
        assert(out == " |  anon : (lin 2, col 13) : (val' x = do { (val y = []); y; })\n" +
                " v  error : cannot copy reference out\n") { out }
    }

    @Test
    fun mm_06_tmp_err() {
        val out = test("""
            val v = do {
                val x = []
                if x { x } else { [] }
            }
            println(v)
        """)
        assert(out == " |  anon : (lin 2, col 13) : (val v = do { (val x = []); if x { x; } el...\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[]\n") { out }
    }

    @Test
    fun clo11() {
        val out = test("""
            val f = do {
                val x = []
                ;;println(x)
                func' () {   ;; block_set(1)
                    x       ;; because of x
                }           ;; err: scope on return
            }
            println(f())
        """
        )
        //assert(out == "anon : (lin 3, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 3, col 21) : block escape error : reference has immutable scope\n") { out }
        //assert(out == "[]\n") { out }
        assert(out == " |  anon : (lin 2, col 13) : (val f = do { (val x = []); (func' () { x;...\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun clo21_err() {
        val out = test(
            """
            var f = func' (a) {
                func' () {
                    a
                }
            }
            var g = do {
                val t = [1]
                f(t)
            }
            println(g())
        """
        )
        //assert(out == "anon : (lin 7, col 21) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 7, col 13) : (var g = do { (val t = [1]); f(t); })\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[1]\n") { out }
    }
    @Test
    fun clo23_err() {
        val out = test(
            """
            var f = func' (a) {
                func' () {       ;; fleet
                    a           ;; non-fleet [1]
                }
            }
            var g = do {
                var t = [1]
                val x = drop(f(t))  ;; drop stops at fleet func (not at non-fleet a=[1])
                x
            }
            println(g())
        """
        )
        //assert(out == "anon : (lin 7, col 21) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 7, col 13) : (var g = do { (var t = [1]); (val x = drop...\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[1]\n") { out }
    }
    @Test
    fun clo23x_err() {
        val out = test(
            """
            var f = func' (v) {
                [v]
            }
            var g = do {
                var t = [1]
                drop(f(t))
            }
            println(g)
        """
        )
        //assert(out == "anon : (lin 7, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out == "[[1]]\n") { out }
        assert(out == " |  anon : (lin 5, col 13) : (var g = do { (var t = [1]); drop(f(t)); })\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun clo23x() {
        val out = test(
            """
            var f = func' (a) {
                func' () {
                    a
                }
            }
            var g = do {
                var t = [1]
                drop(f(drop(t)))
            }
            println(g())
        """
        )
        assert(out == "[1]\n") { out }
    }

    @Test
    fun gc_08() {
        DEBUG = true
        val out = test(
            """
            do {
                val out = do {
                    val ins = [1,2,3]
                    drop(ins)
                }   ;; gc'd by block
                println(`:number CEU_GC.free`, `:number CEU_GC.free`)
            }
            println(`:number CEU_GC.free`, `:number CEU_GC.free`)
        """
        )
        assert(out == "0\t0\n1\t1\n") { out }
        //assert(out == "0\t0\n0\t0\n") { out }
    }
    @Test
    fun gc_09_err() {
        val out = test(
            """
            var out
            set out = do {
                var ins
                set ins = [1,2,3]
                ins
            }
            println(out)
        """
        )
        assert(out == " |  anon : (lin 3, col 17) : out\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 3, col 23) : error : cannot copy reference out\n") { out }
        //assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun qq_06_copy() {
        val out = test("""
            var x = [1,2,3]
            do {
                val y = copy(x)
                do {
                    set x = y
                }
            }
            println(x)
        """, true)
        assert(out == " |  anon : (lin 6, col 25) : x\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[1,2,3]\n") { out }
        //assert(out == "anon : (lin 6, col 25) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
    }

    @Test
    fun ff_01_drop() {
        val out = test("""
        val f = func' (co) {
            resume co()
        }
        val C = coro' () {
            var t = []
            yield(drop(t)) ;;thus { it => nil }
            println(:in, t)
        }
        do {
            val co = coroutine(C)
            do {
                val v = f(co)
                println(:out, v)
            }
            f(co)
        }
        """)
        assert(out == ":out\t[]\n:in\tnil\n") { out }
        //assert(out == ":out\t[]\n:in\t[]\n") { out }
    }
    @Test
    fun ff_04_move_err () {
        val out = test("""
            val y = do {
                val x = coroutine(coro' () {
                    println(:1)
                    yield(nil)
                    println(:2)
                })
                resume x()
                drop(x)
            }
            resume y()
        """)
        //assert(out == "anon : (lin 9, col 22) : move error : value is not movable\n" +
        //        ":error\n") { out }
        assert(out == (":1\n:2\n"))
    }
    @Test
    fun ff_05_move_ok () {
        val out = test("""
            val tup = [nil]
            val co = do {
                var x = coroutine (coro' () {
                    yield(nil)
                    println(:ok)
                })
                resume x()
                drop(x)
            }
            resume co()
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ff_06_move() {
        val out = test("""
            val F = func' (x) {
                val y = (coroutine (coro' () {
                    yield(nil)
                    x
                }))
                resume y()
                drop(y)
            }
            do {
                val x = []
                val co = F(x)
                println(resume co())
            }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun ff_07_move() {
        val out = test("""
            val CO = coro' () {
                val a = [:a]
                do {
                    yield(nil)
                    val b = [:b]
                    do {
                        yield(nil)
                        val c = [:c]
                        val xa = `:number ${D}a.Dyn->Any.lex.depth`
                        val xb = `:number ${D}b.Dyn->Any.lex.depth`
                        val xc = `:number ${D}c.Dyn->Any.lex.depth`
                        println(a,b,c)
                        println(xa,xb,xc)
                    }
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co()
            resume co()
        """)
        assert(out == "[:a]\t[:b]\t[:c]\n2\t3\t4\n") { out }
    }

    @Test
    fun gg_02_scope() {
        val out = test("""
            val T = coro' (v) {
                yield(nil) ;;thus { it => nil }
                println(v)                
            }
            val t = coroutine(T)
            do {
                val a
                do {
                    val b
                    do {
                        val v = []
                        resume t(drop(v))
                    }
                    ;;`ceu_gc_collect();`
                }
            }
            resume t()
        """)
        //assert(out == " |  anon : (lin 9, col 24) : t(v)\n" +
        //        " v  anon : (lin 2, col 30) : resume error : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 2, col 27) : argument error : cannot hold alien reference\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun gg_03_scope() {
        val out = test("""
            val T = coro' () {
                val v = func' (x) {
                    x
                } (
                    yield(nil)
                )
                yield(nil) ;;thus { it => nil }
                println(v)                
            }
            val t = coroutine(T)
            resume t()
            do {
                do {
                    do {
                        val v = []
                        resume t(drop(v))
                    }
                }
            }
            resume t()
        """)
        //assert(out == " |  anon : (lin 11, col 24) : t(v)\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : resume (t)(v)\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 34) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " |  anon : (lin 3, col 25) : (func (x) { x })(yield(nil))\n" +
        //        " v  anon : (lin 3, col 34) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 41) : resume error : cannot receive alien reference\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun gg_03x_scope() {
        val out = test("""
            val T = coro' () {
                val v = yield(nil)
                yield(nil)
                println(v)                
            }
            val t = coroutine(T)
            resume t()
            do {
                val v = []
                resume t(v)
            }
            resume t()
        """)
        assert(out == " |  anon : (lin 11, col 17) : (resume (t)(v))\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive alien reference\n") { out }
    }
    @Test
    fun gg_03y_scope() {
        val out = test("""
            val T = coro' (v) {
                yield(nil)
                println(v)                
            }
            val t = coroutine(T)
            do {
                val v = []
                resume t(drop(v))
            }
            resume t()
        """)
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : (resume (t)(v))\n" +
        //        " v  anon : (lin 2, col 27) : argument error : cannot hold alien reference\n") { out }
    }
    @Test
    fun gg_05_scope() {
        val out = test("""
            val T = coro' () {
                do {
                    val x = []
                    yield(x) ;;thus { it => nil }    ;; err
                    println(:in, x)
                }
                yield(nil) ;;thus { it => nil }
            }
            val t = coroutine(T)
            do {
                val a
                do {
                    val b
                    do {
                        val x = resume t()
                        resume t()
                        println(:out, x)
                    }
                }
            }
            resume t()
        """)
        //assert(out == " |  anon : (lin 16, col 25) : (val x = (resume (t)()))\n" +
        //        " v  error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 16, col 33) : (resume (t)())\n" +
                " |  anon : (lin 5, col 21) : yield(x)\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == ":in\t[]\n:out\t[]\n") { out }
        //assert(out == " |  anon : (lin 16, col 33) : (resume (t)(nil))\n" +
        //        " v  anon : (lin 5, col 21) : yield error : cannot return pending reference\n") { out }
    }
    @Test
    fun gg_06_scope() {
        val out = test("""
            val T = coro' () {
                val x = []
                yield(drop(x)) ;;thus { it => nil }    ;; err
                println(:in, x)
            }
            val t = coroutine(T)
            do {
                val x = resume t()
                println(:out, x)
            }
            resume t()
        """)
        assert(out == ":out\t[]\n:in\tnil\n") { out }
        //assert(out == ":out\t[]\n:in\t[]\n") { out }
    }

    @Test
    fun kk_02_scope() {
        val out = test("""
            val T = coro' () {
                val t = []
                yield(t)
                println(t)                
            }
            val t = coroutine(T)
            do {
                resume t()
            }
            resume t()
        """)
        assert(out == " |  anon : (lin 9, col 17) : (resume (t)())\n" +
                " |  anon : (lin 4, col 17) : yield(t)\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : (resume (t)(nil))\n" +
        //        " v  anon : (lin 4, col 17) : yield error : cannot return pending reference\n") { out }
    }

    @Test
    fun nn_02_catch() {
        val out = test("""
            val CO = coro' () {
                catch :x ;;;( it | do {
                    yield(nil)
                } );;;
                {
                    error(:e1)
                }
            }
            resume (coroutine(CO)) ()
        """)
        assert(out == " |  anon : (lin 10, col 13) : (resume (coroutine(CO))())\n" +
                " |  anon : (lin 7, col 21) : error(:e1)\n" +
                " v  error : :e1\n") { out }
        //assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing catch\n") { out }
    }

    @Test
    fun oo_04_tmp_tuple() {
        val out = test("""
            val CO = coro' () {
                val t = [yield(nil),yield(nil),yield(nil)]
                yield(nil)
                drop(t)
            }
            val co = coroutine(CO)
            resume co()
            resume co(1)
            resume co(2)
            resume co(3)
            val t = resume co()
            println(t)
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun oo_05_tmp_vector() {
        val out = test("""
            val CO = coro' () {
                val t = #[yield(nil),yield(nil),yield(nil)]
                yield(nil)
                drop(t)
            }
            val co = coroutine(CO)
            resume co()
            resume co(1)
            resume co(2)
            resume co(3)
            val t = resume co()
            println(t)
        """)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun oo_06_tmp_dict() {
        val out = test("""
            val CO = coro' () {
                val t = @[(1,yield(nil)),(yield(nil),20)]
                yield(nil)
                drop(t)
            }
            val co = coroutine(CO)
            resume co()
            resume co(10)
            resume co(2)
            val t = resume co()
            println(t)
        """)
        assert(out == "@[(1,10),(2,20)]\n") { out }
    }

    @Test
    fun cd_01_every() {
        val out = test(
            """
            spawn (task' () {
                val it = yield(nil)
                println(it;;;, evt;;;)
                yield(nil)
            }) (nil)
            do {
                val e = []
                broadcast'(:task,e)
            }
            println(:ok)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : yield error : unexpected enclosing thus\n") { out }
        assert(out == " |  anon : (lin 9, col 17) : broadcast'(:task,e)\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "[]\n:ok\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun cd_02_bcast_spawn_arg() {
        val out = test(
            """
            val T = task' () {
                val x = yield(nil)
            }
            spawn T() 
            do {
                val e = []
                broadcast(drop(e))
            }
            println(:ok)
        """
        )
        //assert(out == " |  anon : (lin 8, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cd_03_bcast_pub_arg() {
        val out = test(
            """
            val T = task' () {
                val evt = yield(nil)
                set pub = evt
                println(:in, pub)
                :ok
            }
            val t = spawn T() 
            do {
                val e = []
                broadcast(drop(e))
            }
            println(:out, t.pub)
        """
        )
        //assert(out == " |  anon : (lin 8, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 21) : set error : cannot hold alien reference\n") { out }
        assert(out == ":in\t[]\n:out\t:ok\n") { out }
    }

    @Test
    fun ab_02_tasks_proto_err() {
        val out = test("""
            val ts = tasks()
            do {
                val T = task' () {
                    nil
                }
                spawn T() in ts
            }
            println(:ok)
       """)
        //assert(out == " v  anon : (lin 7, col 17) : spawn error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 7, col 17) : spawn error : task pool outlives task prototype\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == " |  anon : (lin 7, col 17) : (spawn T() in ts)\n" +
                " v  error : cannot copy reference out\n") { out }
    }

    @Test
    fun bd_01_track_err() {
        val out = test("""
            var T
            set T = task' () { yield(nil) }
            var x
            do {
                val t = spawn (T) ()
                set x = ;;;track;;;(t)         ;; error scope
            }
            println(status(;;;detrack;;;(x)))
            println(x)
        """)
        //assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 7, col 21) : set error : cannot expose track outside its task scope\n")) { out }
        assert(out == (" |  anon : (lin 7, col 21) : x\n" +
                " v  error : cannot copy reference out\n")) { out }
    }
    @Test
    fun bd_02_track_err() {
        val out = test("""
            var T
            set T = task' () { yield(nil) }
            val x = do {
                val t = spawn (T) ()
                ;;;track;;;(t)         ;; error scope
            }
            println(status(;;;detrack;;;(x)))
            println(x)
        """)
        //assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out.contains("terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 4, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
        assert(out == (" |  anon : (lin 4, col 13) : (val x = do { (val t = (spawn T())); t; })\n" +
                " v  error : cannot copy reference out\n")) { out }
    }
    @Test
    fun bd_04_track_err() {
        val out = test("""
            var T
            set T = task' () { yield(nil) }
            var x =
            do {
                val t = spawn (T) ()
                val x' = ;;;track;;;(t)
                x'         ;; error scope
            }
            println(status(;;;detrack;;;(x)))
            println(x)
        """)
        //assert(out.contains("terminated\nexe-task: 0x")) { out }
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 5, col 13) : block escape error : cannot expose track outside its task scope\n")) { out }
        assert(out == (" |  anon : (lin 4, col 13) : (var x = do { (val t = (spawn T())); (val ...\n" +
                " v  error : cannot copy reference out\n")) { out }
    }
    @Test
    fun bc_02_track_drop_err() {
        val out = test("""
            val T = task' () { yield(nil) }
            val y = do {
                val t = spawn T ()
                ;;;track;;;(t)
            }
            println(y)
        """)
        assert(out == (" |  anon : (lin 3, col 13) : (val y = do { (val t = (spawn T())); t; })\n" +
                " v  error : cannot copy reference out\n")) { out }
        //assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
        //assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun bc_02x_track_drop_err() {
        val out = test("""
            val T = task' () { yield(nil) }
            val y = do {
                val t = spawn T ()
                val x = ;;;track;;;(t)
                drop(t)
            }
            println(y)
        """)
        //assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
        //assert(out.contains("exe-task: 0x")) { out }
        assert(out.contains(" |  anon : (lin 6, col 17) : drop(t)\n" +
                " v  error : value is not droppable\n")) { out }
    }
    @Test
    fun bc_02y_track_drop_err() {
        val out = test("""
            val T = task' () { yield(nil) }
            val y = do {
                val t = spawn T ()
                ;;val x = ;;;track;;;(t)
                drop(t)
            }
            println(y)
        """)
        //assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
        //assert(out.contains("exe-task: 0x")) { out }
        assert(out.contains(" |  anon : (lin 6, col 17) : drop(t)\n" +
                " v  error : value is not droppable\n")) { out }
    }
    @Test
    fun bc_04_track_drop() {
        val out = test("""
            val T = task' () { yield(nil) }
            val y = do {
                val ts = tasks()
                spawn T () in ts
                drop(next-tasks(ts))
            }
            println(y)
        """)
        assert(out == ("TODO - o drop tem que ser em lval")) { out }
        //assert(out == (" v  anon : (lin 3, col 21) : block escape error : cannot expose track outside its task scope\n")) { out }
        //assert(out.contains("exe-task: 0x")) { out }
        //assert(out.contains(" |  anon : (lin 3, col 21) : do { (val ts = tasks(nil)); (spawn T() in ...\n" +
        //        " v  error : value has multiple references")) { out }
    }

    @Test
    fun ff_01_scope() {
        val out = test("""
            val T = task' () { yield(nil) }
            var x
            do {
                val t = spawn T()
                set x = ;;;track;;;(t)
            }
            println(:ok)
        """)
        //assert(out == " v  anon : (lin 6, col 21) : set error : cannot expose track outside its task scope\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == " |  anon : (lin 6, col 21) : x\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun ff_05_track_err() {
        val out = test("""
            val T = task' () {
                ${AWAIT()}
            }
            val x = do {
                val t = spawn T()
                ;;;track;;;(t)
            }
            println(status(x))
        """)
        //assert(out == " v  anon : (lin 5, col 21) : block escape error : cannot expose track outside its task scope\n") { out }
        //assert(out == ":terminated\n") { out }
        assert(out == " |  anon : (lin 5, col 13) : (val x = do { (val t = (spawn T())); t; })\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun ff_06_track_err() {
        val out = test("""
            val T = task' () {
                ${AWAIT()}
            }
            val x = do {
                val ts = tasks()
                spawn T() in ts 
                next-tasks(ts)
            }
            println(x)
        """)
        //assert(out.contains("exe-task: 0x")) { out }
        assert(out == " |  anon : (lin 5, col 13) : (val x = do { (val ts = tasks(nil)); (spaw...\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun fg_04_expose_err() {
        val out = test("""
            val x = do {
                val ts = tasks()
                var T = task' () {
                    set pub = []
                    yield(nil) ;; nil
                    nil
                }
                spawn (T) () in ts
                val trk = next-tasks(ts)
                val p = ;;;detrack;;;(trk) ;;{ it => it }
                p
            }
            println(status(x))
        """)
        assert(out == " |  anon : (lin 2, col 13) : (val x = do { (val ts = tasks(nil)); (var ...\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == ":terminated\n") { out }
        //assert(out == ":pub\t[]\n" +
        //        " v  anon : (lin 2, col 21) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 11, col 17) : declaration error : cannot expose task-in-pool reference\n") { out }
        //assert(out == " |  anon : (lin 11, col 38) : (func (it) { if it { ```                     ...)\n" +
        //        " v  anon : (lin 11, col 38) : block escape error : cannot expose task in pool to outer scope\n") { out }
    }

    // IT

    @Test
    fun gg_01_it() {
        val out = test("""
            do {
                val it = 10
                println(it)
                do {
                    val it = 100
                    println(it)
                }
                println(it)
            }
        """)
        assert(out == "10\n100\n10\n") { out }
    }
    @Test
    fun gg_02_it() {
        val out = test("""
            do {
                val' it = 10
                println(it)
                do {
                    val' it = 100
                    println(it)
                }
                println(it)
            }
        """)
        assert(out == "10\n100\n10\n") { out }
    }
    @Test
    fun gg_03_it() {
        val out = test("""
            val' it = 10
            println(it)
            val' it = 100
            println(it)
        """)
        assert(out == "10\n100\n10\n") { out }
    }
    @Test
    fun gg_10_loop_optim_it() {
        val out = test("""
            enclose' :break {
                (var it = 10);
                loop' {
                    println(it);
                    do {
                        (val' it = true);
                        escape(:break,it);
                    };
                };
            };
        """)
        assert(out == "10\n") { out }
    }


    @Test
    fun jj_02_tracks() {
        val out = test("""
            val f = func' (trk) {
                ;;println(detrack(trk) { it => status(it) })
                println(status(trk))
            }
            val T = task' () { yield(nil) ; nil }
            val x' = do {
                val ts = tasks()
                spawn T() in ts
                val x = [next-tasks(ts,nil)]
                ;;dump(x)
                f(x[0])
                x
            }
            f(x'[0])
        """)
        assert(out==(":yielded\n" +
                " |  anon : (lin 7, col 13) : (val x' = do { (val ts = tasks(nil)); (spa...\n" +
                " v  error : cannot copy reference out\n")) { out }
        //assert(out==(":yielded\n:terminated\n")) { out }
        //assert(out.contains(":yielded\n" +
        //        " v  anon : (lin 7, col 22) : block escape error : reference has immutable scope\n")) { out }
        //assert(out.contains(":yielded\n" +
        //        " v  anon : (lin 7, col 22) : block escape error : cannot expose track outside its task scope\n")) { out }
    }

    @Test
    fun oo_03_track_err() {
        val out = test("""
            var T
            set T = task' (v) {
                set pub = [v]
                yield(nil) ; nil
            }
            var x
            do {
                var ts
                set ts = tasks()
                spawn T(1) in ts
                do {
                    val t = next-tasks(ts)
                    set x = t       ;; err: escope 
                }
            }
            println(:ok)
        """)
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n:error\n") { out }
        //assert(out == " v  anon : (lin 14, col 25) : set error : cannot assign reference to outer scope\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == " |  anon : (lin 14, col 25) : x\n" +
                " v  error : cannot copy reference out\n") { out }
    }
    @Test
    fun op_03_track_err() {
        val out = test("""
            var T
            set T = task' () {
                set pub = [10]
                yield(nil) ; nil
            }
            var x
            do {
                var t = spawn (T) ()
                set x = ;;;track;;;(t)         ;; scope x < t
                ;;println(detrack(x).pub[0])
            }
            ;;println(status(detrack(x)))
            ;;println(x)
            println(:ok)
        """)
        //assert(out.contains("10\n:terminated\nx-track: 0x")) { out }
        //assert(out == "anon : (lin 10, col 21) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == (" v  anon : (lin 10, col 21) : set error : cannot expose track outside its task scope\n")) { out }
        //assert(out == (":ok\n")) { out }
        assert(out == (" |  anon : (lin 10, col 21) : x\n" +
                " v  error : cannot copy reference out\n")) { out }
    }
    @Test
    fun op_08_track_scope() {
        val out = test("""
            val T = task' () {
                set pub = 1
                yield(nil) ; nil
            }
            val y = do {
                val t = spawn T()
                ;;;track;;;(t)
            }
            ;;detrack(y) { it => println(it.pub) }
            println(y.pub)
        """)
        assert(out == " |  anon : (lin 6, col 13) : (val y = do { (val t = (spawn T())); t; })\n" +
                " v  error : cannot copy reference out\n") { out }
        //assert(out == "1\n") { out }
        //assert(out == " v  anon : (lin 6, col 21) : block escape error : cannot expose track outside its task scope\n") { out }
    }

    @Test
    fun de_05_evt_err_valgrind() {
        val out = test("""
            spawn (task' () {
                var evt = yield(nil)
                val x = evt
                println(x)
                set evt = yield(nil)
                println(x)
            }) ()
            do {
                val e = [10]
                broadcast(drop(e))
            }
            broadcast(nil)
        """)
        assert(out == "[10]\n[10]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun de_07_evt_err() {
        val out = test("""
            spawn (task' () {
                val evt = yield(nil)
                val x = evt[0]
                println(x)
            }) ()
            do {
                val e = [[10]]
                broadcast(drop(e))
            }
        """)
        assert(out == "[10]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 17) : declaration error : cannot hold alien reference\n") { out }
    }

    @Test
    fun ee_10_bcast_err2() {
        val out = test(
            """
            val T = task' (v) {
                val e = yield(nil)
                println(v,e)                
            }
            spawn T(10)
            do {
                val e = []
                broadcast (drop(e))
            }
        """
        )
        assert(out == "10\t[]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }

    @Test
    fun zz_15_bcast_err() {
        val out = test(
            """
            var T = task' () {
                var v = yield(nil)
                println(v)
            }
            var t = spawn T()
            do {
                val a
                do {
                    val b
                    do {
                        var e = []
                        broadcast (drop(e))
                    }
                }
            }
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 10, col 35) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 9, col 13) : broadcast e\n" +
        //        " v  anon : (lin 3, col 25) : resume error : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : broadcast e\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : broadcast'(e)\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun zz_15_bcast_okr() {
        val out = test(
            """
            var T = task' () {
                println(yield(nil))
            }
            var t = spawn T()
            do {
                var e = []
                broadcast (drop(e))
            }
            """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_16_bcast_err() {
        val out = test(
            """
            var T = task' () {
                var v =
                    func' (it) {it} (yield(nil))
                println(v)
            }
            var t = spawn T()
            ;;println(:1111)
            do {
                val a
                do {
                    val b
                    var e = []
                    broadcast (drop(e))
                }
            }
            ;;println(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : broadcast e\n" +
        //        " v  anon : (lin 4, col 17) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == "anon : (lin 11, col 39) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 28) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " |  anon : (lin 4, col 17) : (func (it) { it })(yield(nil))\n" +
        //        " v  anon : (lin 4, col 27) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration error : cannot hold alien reference\n") { out }
    }
}
