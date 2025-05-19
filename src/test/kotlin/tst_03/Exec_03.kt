package tst_03

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_03 {
    // CORO / YIELD / RESUME

    @Test
    fun aa_01_coro() {
        val out = test("""
            val t = coro' (v) {
                v
            }
            print(t)
        """)
        assert(out.contains("coro: 0x")) { out }
    }
    @Test
    fun aa_02_coro() {
        val out = test("""
            val t = coro' (vvv) {
                yield(vvv)
            }
            print(t)
        """)
        assert(out.contains("coro: 0x")) { out }
    }
    @Test
    fun aa_03_yield_err() {
        val out = test("""
            await(true)
        """)
        assert(out == "anon : (lin 2, col 13) : yield throw : expected enclosing coro\n") { out }
    }
    @Test
    fun aa_04_resume_err() {
        val out = test("""
            val f
            resume f()
        """)
        assert(out == " |  anon : (lin 3, col 13) : (resume (f)())\n" +
                " v  throw : expected yielded coro\n") { out }
    }
    @Test
    fun aa_05_yield_err() {
        val out = test("""
            resume (coro' () {
                func' (it) { set it = nil } (yield(nil))
            }) ()
        """)
        assert(out == "anon : (lin 3, col 30) : set throw : destination is immutable\n") { out }
    }
    @Test
    fun aa_06_yield_err() {
        val out = test("""
            resume (coro' () {
                func' (it) { func' (x) { nil } (yield(nil)) } (yield(nil))
            }) ()
        """)
        assert(out == "anon : (lin 3, col 49) : yield throw : unexpected enclosing func\n") { out }
    }
    @Test
    fun aa_07_val_same() {
        val out = test("""
            coro' () {
                do {
                    val x
                    await(true)
                }
                do {
                    val x
                    await(true)
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // NATIVE

    @Test
    fun TODO_ab_01_native() {    // cannot write C -> Ceu
        val out = test("""
            func' () {
                val x = 1
                val y = `:number ${D}x.Number`
                ```
                    ${D}x.Number = 2;
                ```
                print(x,y)
            }()
        """)
        assert(out == "2\t1\n") { out }
    }

    // COROUTINE

    @Test
    fun bb_01_coroutine_err() {
        val out = test("""
            val t = func' (v) {
                v
            }
            val x = coroutine(t)
            print(x)
        """)
        assert(out == " |  anon : (lin 5, col 21) : coroutine(t)\n" +
                " v  throw : expected coro\n") { out }
    }
    @Test
    fun bb_02_coroutine_err() {
        val out = test("""
            val t = coro' (v) {
                v
            }
            val x = coroutine(t)
            print(x)
        """)
        assert(out.contains("exe-coro: 0x")) { out }
    }
    @Test
    fun bb_03_coroutine_err() {
        val out = test("""
            coroutine(func' () {nil})
        """)
        assert(out == " |  anon : (lin 2, col 13) : coroutine((func' () { nil; }))\n" +
                " v  throw : expected coro\n") { out }
    }

    // RESUME / YIELD

    @Test
    fun cc_00_resume() {
        val out = test("""
            val a = coro' () {
                print(:ok)
            }
            val b = coroutine(a)
            resume b()
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cc_01_resume() {
        val out = test("""
            val t = coro' (v) {
                v
            }
            val a = coroutine(t)
            var v = resume a(1)
            print(v)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_01_var() {
        val out = test("""
            val CO = coro' () {
                val v = 10
                await(true)
                v
            }
            val co = coroutine(CO)
            resume co()
            val v = resume co()
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_01_par() {
        val out = test("""
            val CO = coro' (v) {
                v
            }
            val co = coroutine(CO)
            val v = resume co(10)
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_01_par_yield() {
        val out = test("""
            val CO = coro' (v) {
                val v' = v
                await(true)
                v'
            }
            val co = coroutine(CO)
            resume co(10)
            val v = resume co(10)
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_02_resume_dead_err() {
        val out = test("""
            val co = coroutine(coro' () {nil})
            resume co()
            resume co()
        """)
        assert(out == " |  anon : (lin 4, col 13) : (resume (co)())\n" +
                " v  throw : expected yielded coro\n") { out }
    }
    @Test
    fun cc_03_resume_dead_err() {
        val out = test("""
            val CO = coro' () {
                nil
            }
            val co = coroutine(CO)
            resume co()
            resume co()
        """)
        assert(out == " |  anon : (lin 7, col 13) : (resume (co)())\n" +
                " v  throw : expected yielded coro\n") { out }
    }
    @Test
    fun cc_04_resume_yield() {
        val out = test("""
            val t = coro' () {
                print(1)
                await(true)
                print(2)
                await(true)
                print(3)
            }
            val a = coroutine(t)
            resume a()
            resume a()
            resume a()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun cc_05_resume_arg() {
        val out = test("""
            val t = coro' (v) {
                print(v)
            }
            val a = coroutine(t)
            resume a(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_06_yield_ret() {
        val out = test("""
            val CO = coro' () {
                yield(10) ;;thus { it => nil }
            }
            val co = coroutine(CO)
            val v = resume co()
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_07_resume_yield() {
        val out = test("""
            $PLUS
            val CO = coro' () {
                func' (it) { 
                    print(it)
                } (yield(nil))
            }
            val co = coroutine(CO)
            resume co()
            resume co(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_08_resume_yield() {
        val out = test("""
            $PLUS
            val CO = coro' () {
                yield(10) ;;thus { it => nil }
            }
            val co = coroutine(CO)
            val v = resume co()
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_09_resume_yield() {
        val out = test("""
            val CO = coro' (v1) {
                func' (x) { x } (yield(v1))
            }
            val co = coroutine(CO)
            val v1 = resume co(10)
            val v2 = resume co(v1)
            print(v2)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_10_resume() {
        val out = test("""
            $PLUS
            val CO = coro' (v1) {    ;; 10
                func' (it) {     ;; 12
                    it + 1
                } (yield(v1+1))
            }
            val co = coroutine(CO)
            val v1 = resume co(10)      ;; 11
            val v2 = resume co(v1+1)    ;; 13
            print(v2)
        """)
        assert(out == "13\n") { out }
    }
    @Test
    fun cc_10x_resume() {
        val out = test("""
            $PLUS
            val CO = coro' (v1) {
                print(:v1, v1)        ;; 10
                val v2 = yield(v1+1)
                print(:v2, v2)        ;; 12
                v2 + 1
            }
            val co = coroutine(CO)
            val x1 = resume co(10)
            print(:x1, x1)            ;; 11
            val x2 = resume co(x1+1)    ;; 13
            print(:x2, x2)
        """)
        assert(out == ":v1\t10\n:x1\t11\n:v2\t12\n:x2\t13\n") { out }
    }
    @Test
    fun cc_11_mult() {
        val out = test("""
            var co
            set co = coroutine(coro' (x,y) {
                print(x,y)
            })
            resume co(1,2)  ;; pass multiple values
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun cc_12_multi_err() {
        val out = test("""
            var co
            set co = coroutine(coro' (x,y) {
                print(x,y)
                print(yield(nil))
                print(yield(nil))
            })
            resume co(1,2)
            resume co()
            resume co(3,4)
        """)
        assert(out == ("1\t2\nnil\n3\n")) { out }
    }
    @Test
    fun cc_13_tuple_leak() {
        val out = test("""
            val T = coro' () {
                ;;;do;;; [1,2,3]
                func' (it) {
                    nil
                } (
                    await(true)
                )
            }
            resume (coroutine(T)) ()
            print(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_15_yield_err() {
        val out = test("""
            coro' () {
                func' () {
                    await(true) ;;thus { it =>nil }
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing func\n") { out }
    }
    @Test
    fun cc_16_tags() {
        val out = test("""
            val co = coro' () {
                yield(:x) ;;thus { it =>nil }
                nil
            }
            print(:y)
        """)
        assert(out == ":y\n") { out }
    }
    @Test
    fun cc_17x_tags() {
        val out = test("""
            val CO = coro' () {
                await(true)
            }
            val co = coroutine(CO)
            resume co()
            resume co([])
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cc_17_tags() {
        val out = test("""
            val CO = coro' () {
                func' (it) {
                    print(sup?(:X,tag(it))) ;; drop(it)
                } (yield(nil))
            }
            val co = coroutine(CO)
            resume co()
            resume co(tag(:X,[]))
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun cc_18_bug_stack() {
        val out = test("""
            val T = coroutine(coro' () {
                val x =
                    do {
                        val it = await(true)
                        print(:in, it)
                    }     
                val y
            })
            resume T()
            resume T(10)
            print(:ok)
        """)
        assert(out == ":in\t10\n:ok\n") { out }
    }

    // AS

    @Test
    fun cd_01_yield_as() {
        val out = test("""
            val CO = coro' () {
                func' (v) {
                    print(v)
                } (yield(nil))
            }
            val co = coroutine(CO)
            resume co()
            resume co(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cd_02_yield_as() {
        val out = test("""
            coro' () {
                func' (it :T) {
                    it[0]
                } (yield(nil))
            }
            print(:ok)
        """)
        //assert(out == "anon : (lin 3, col 26) : declaration throw : data :T is not declared\n") { out }
        assert(out == ":ok\n") { out }
    }

    // MEM vs STACK

    @Test
    fun TODO_dd_01_access_from_c() {    // TODO: for now, all exes use mem
        val out = test("""
            val CO = coro' (v1) {
                val v2 = 2
                print(v1, v2)
                ```                 // access from c
                printf("%f\t%f\n", id_v1.Number, id_v2.Number);
                ```
            }
            val co = coroutine(CO)
            resume co(1)
        """)
        assert(out == "1\t2\n1.000000\t2.000000\n") { out }
    }
    @Test
    fun dd_02_mem() {
        val out = test("""
            val CO = coro' (v1) {
                val v2 = 2
                print(v1, v2)
                await(true)
                ```
                //printf("%f\t%f\n", ceu_mem->id_v1_129.Number, ceu_mem->id_v2_17.Number);
                printf("%f\t%f\n", ${D}v1.Number, ${D}v2.Number);
                ```
            }
            val co = coroutine(CO)
            resume co(1)
            resume co(1)
        """)
        assert(out == "1\t2\n1.000000\t2.000000\n") { out }
    }
    @Test
    fun dd_03_tup() {
        val out = test("""
            print([])
        """,)
        assert(out == "[]\n") { out }
    }

    // ORIGINAL

    @Test
    fun ee_02_coro_defer() {
        val out = test("""
            val T = coro' () {
                defer {
                    print(3)
                }
                print(1)
                await(true)  ;; never awakes
                print(2)
            }
            print(0)
            val co = coroutine(T)
            resume co ()
            print(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun ee_03_coro_defer() {
        val out = test("""
            var T
            set T = coro' () {
                defer {
                    print(3)
                }
                print(1)
                await(true)   ;; never awakes
                print(2)
            }
            val t = coroutine(T)
            print(0)
            resume t ()
            print(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun ee_04_coro_defer() {
        val out = test("""
            val T = coro' () {
                print(1)
                await(true)   ;; never awakes
                defer {
                    print(999)
                }
            }
            resume (coroutine(T)) ()
            print(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun ee_05_coro_defer() {
        val out = test("""
            var T
            set T = coro' () {
                defer {
                    print(3)
                }
                print(1)
                await(true)   ;; never awakes
                defer {
                    print(999)
                }
                print(2)
            }
            print(0)
            val co = coroutine(T)
            resume co ()
            print(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun ee_06_coro_defer() {
        val out = test("""
            val F = coro' () {
                defer {
                    print(:xxx)
                }
                await(true)
                defer {
                    print(:yyy)
                }
                await(true)
            }
            do {
                val f = coroutine(F)
                resume f()
                resume f()
            }
        """)
        assert(out == ":yyy\n:xxx\n") { out }
    }
    @Test
    fun ee_07_move() {
        val out = test("""
            do {
                val F = (coro' () { print(:ok) })
                val f = do {        ;; dst=2
                    coroutine(F)    ;; src=3
                }
                resume f()
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ee_09_tags() {
        val out = test("""
            val co = coro' () {
                yield(:x) ;;thus { it => nil }
            }
            print(:y)
        """)
        assert(out == ":y\n") { out }
    }
    @Test
    fun ee_10_defer_loop() {
        val out = test("""
            val CO = coro' () {
                print(:1)
                defer {
                    print(:ok)
                }
                loop' {
                    print(:2)
                    await(true)
                }
                print(999)
            }
            val co = coroutine(CO)
            resume co ()
        """)
        assert(out == ":1\n:2\n:ok\n") { out }
    }
    @Test
    fun ee_11_defer_do() {
        val out = test("""
            val CO = coro' () {
                print(:1)
                defer {
                    print(:ok)
                }
                do {
                    print(:2)
                    await(true)
                }
                print(999)
            }
            val co = coroutine(CO)
            resume co ()
        """)
        assert(out == ":1\n:2\n:ok\n") { out }
    }

    // DROP / MOVE / OUT

    @Test
    fun ff_01_drop() {
        val out = test("""
        val f = func' (co) {
            resume co()
        }
        val C = coro' () {
            var t = []
            yield(;;;drop;;;(t)) ;;thus { it => nil }
            print(:in, t)
        }
        do {
            val co = coroutine(C)
            do {
                val v = f(co)
                print(:out, v)
            }
            f(co)
        }
        """)
        //assert(out == ":out\t[]\n:in\tnil\n") { out }
        assert(out == ":out\t[]\n:in\t[]\n") { out }
    }
    @Test
    fun ff_02() {
        val out = test("""
            var f
            set f = func' () {
                coroutine(coro'() {nil})
            }
            print(f())
        """)
        //assert(out == "anon : (lin 6, col 21) : f()\n" +
        //        "anon : (lin 3, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
        assert(out.contains("coro: 0x")) { out }
    }
    @Test
    fun ff_03() {
        val out = test("""
            var T
            set T = coro' () { nil }
            var xxx
            do {
                var t
                set t = coroutine(T)
                set xxx = t ;; throw
            }
            print(xxx)
        """)
        //assert(out == " v  anon : (lin 8, col 21) : set throw : cannot assign reference to outer scope\n") { out }
        assert(out.contains("coro: 0x")) { out }
    }
    @Test
    fun ff_04_move_err () {
        val out = test("""
            val y = do {
                val x = coroutine(coro' () {
                    print(:1)
                    await(true)
                    print(:2)
                })
                resume x()
                ;;;drop;;;(x)
            }
            resume y()
        """)
        //assert(out == "anon : (lin 9, col 22) : move throw : value is not movable\n" +
        //        ":throw\n") { out }
        assert(out == (":1\n:2\n"))
    }
    @Test
    fun ff_05_move_ok () {
        val out = test("""
            val tup = [nil]
            val co = do {
                var x = coroutine (coro' () {
                    await(true)
                    print(:ok)
                })
                resume x()
                ;;;move;;;(x)
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
                    await(true)
                    x
                }))
                resume y()
                ;;;move;;;(y)
            }
            do {
                val x = []
                val co = F(x)
                print(resume co())
            }
        """)
        assert(out == "[]\n") { out }
    }

    // SCOPE

    @Test
    fun gg_01_scope() {
        val out = test("""
            val T = coro' (v) {
                await(true)
                print(v)                
            }
            val t = coroutine(T)
            do {
                val v ;; true block
                resume t([])
            }
            resume t()
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun gg_02_scope() {
        val out = test("""
            val T = coro' (v) {
                await(true)
                print(v)                
            }
            val t = coroutine(T)
            do {
                val a
                do {
                    val b
                    do {
                        val v = []
                        resume t(v)
                    }
                    ;;`ceu_gc_collect();`
                }
            }
            resume t()
        """)
        //assert(out == " |  anon : (lin 9, col 24) : t(v)\n" +
        //        " v  anon : (lin 2, col 30) : resume throw : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 2, col 27) : argument throw : cannot hold alien reference\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun gg_03_scope() {
        val out = test("""
            val T = coro' () {
                val v = func' (x) { x } (yield(nil))
                await(true)
                print(v)                
            }
            val t = coroutine(T)
            resume t()
            do {
                do {
                    do {
                        val v = []
                        resume t(v)
                    }
                }
            }
            resume t()
        """)
        //assert(out == " |  anon : (lin 11, col 24) : t(v)\n" +
        //        " v  anon : (lin 3, col 25) : resume throw : cannot receive assigned reference\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : resume (t)(v)\n" +
        //        " v  anon : (lin 3, col 36) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 34) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " |  anon : (lin 3, col 25) : (func (x) { x })(yield(nil))\n" +
        //        " v  anon : (lin 3, col 34) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 41) : resume throw : cannot receive alien reference\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun gg_03x_scope() {
        val out = test("""
            val T = coro' () {
                val v = await(true)
                await(true)
                print(v)                
            }
            val t = coroutine(T)
            resume t()
            do {
                val v = []
                resume t(v)
            }
            resume t()
        """)
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 25) : resume throw : cannot receive alien reference\n") { out }
    }
    @Test
    fun gg_03y_scope() {
        val out = test("""
            val T = coro' (v) {
                await(true)
                print(v)                
            }
            val t = coroutine(T)
            do {
                val v = []
                resume t(v)
            }
            resume t()
        """)
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : (resume (t)(v))\n" +
        //        " v  anon : (lin 2, col 27) : argument throw : cannot hold alien reference\n") { out }
    }
    @Test
    fun gg_04_scope() {
        val out = test("""
            val T = coro' (v) {
                val e = await(true) ;;thus { it => it }
                print(e)                
            }
            val t = coroutine(T)
            func' () {
                do { do {
                    resume t([])
                    resume t([])
                } }
            }()
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun gg_05_scope() {
        val out = test("""
            val T = coro' () {
                do {
                    val x = []
                    yield(x) ;;thus { it => nil }    ;; err
                    print(:in, x)
                }
                await(true)
            }
            val t = coroutine(T)
            do {
                val a
                do {
                    val b
                    do {
                        val x = resume t()
                        resume t()
                        print(:out, x)
                    }
                }
            }
            resume t()
        """)
        assert(out == ":in\t[]\n:out\t[]\n") { out }
        //assert(out == " |  anon : (lin 16, col 33) : (resume (t)(nil))\n" +
        //        " v  anon : (lin 5, col 21) : yield throw : cannot return pending reference\n") { out }
    }
    @Test
    fun gg_06_scope() {
        val out = test("""
            val T = coro' () {
                val x = []
                yield(;;;drop;;;(x)) ;;thus { it => nil }    ;; err
                print(:in, x)
            }
            val t = coroutine(T)
            do {
                val x = resume t()
                print(:out, x)
            }
            resume t()
        """)
        //assert(out == ":out\t[]\n:in\tnil\n") { out }
        assert(out == ":out\t[]\n:in\t[]\n") { out }
    }
    @Test
    fun gg_07_scope() {
        val out = test("""
            val f = func' (x, y) {
                nil
            }
            val C = coro' () {
                nil
            }
            print(f(coroutine(C)))
        """)
        //assert(out == " |  anon : (lin 8, col 21) : f(coroutine(C))\n" +
        //        " v  anon : (lin 2, col 27) : argument throw : cannot move pending reference in\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun gg_08_scope() {
        val out = test("""
            val f = func' (x, y) {
                nil
            }
            val C = coro' () {
                nil
            }
            val c = coroutine(C)
            print(f(c))
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun gg_19_scope_err() {
        val out = test("""
            val T = coro' (v) {
                val e = await(true)
                print(v,e)                
            }
            val co = coroutine(T)
            resume co()
            do {
                val e = []
                resume co (e)
            }
        """)
        //assert(out == " |  anon : (lin 10, col 17) : (resume (co)(e))\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : (resume (co)(e))\n" +
        //        " v  anon : (lin 3, col 25) : resume throw : cannot receive alien reference\n") { out }
        assert(out == "nil\t[]\n") { out }
    }

    // CATCH / THROW

    @Test
    fun hh_01_throw() {
        val out = test("""
            var co
            set co = coroutine(coro' (x) {
                throw(:e2)
            })
            catch :e2 {
                resume co(1)
                print(99)
            }
            print(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_02_throw() {
        val out = test("""
            var co
            set co = coroutine(coro' () {
                await(true)
                throw(:e2)
            })
            catch :e2 {
                resume co()
                print(1)
                resume co()
                print(2)
            }
            print(3)
        """)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun hh_03_throw() {
        val out = test("""
            var co
            set co = coroutine (coro' () {
                catch :e1 {
                    await(true)
                    throw(:e1)
                }
                print(:e1)
                await(true)
                throw(:e2)
            })
            catch :e2 {
                resume co()
                resume co()
                resume co()
                print(99)
            }
            print(:e2)
        """)
        assert(out == ":e1\n:e2\n") { out }
    }
    @Test
    fun hh_04_catch_yield_err() {
        val out = test("""
            coro' () {
                catch ;;;( it | do {
                    func' (it) { nil } (nil)
                } );;;
                {
                    throw(:e1)
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 4, col 27) : declaration throw : variable \"it\" is already declared\n") { out }
    }
    @Test
    fun hh_05_throw() {
        val out = test(
            """
            val CO = coro' () {
                catch :x {
                    await(true)
                }
                print(999)
            }
            val co = coroutine(CO)
            resume co()
            catch {
                throw(nil)
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun hh_06_throw() {
        val out = test(
            """
            val CO = coro' () {
                throw(:ok)
            }
            val co = coroutine(CO)
            resume co()
        """
        )
        assert(out == " |  anon : (lin 6, col 13) : (resume (co)())\n" +
                " |  anon : (lin 3, col 17) : throw(:ok)\n" +
                " v  throw : :ok\n") { out }
    }
    @Test
    fun hh_07_throw() {
        val out = test("""
            val co = coroutine (coro' () {
                nil
            })
            print(:1)
            throw(:99)
            print(:2)
        """)
        assert(out == ":1\n" +
                " |  anon : (lin 6, col 13) : throw(:99)\n" +
                " v  throw : :99\n") { out }
    }

    // STATUS

    @Test
    fun ii_01_status_err() {
        val out = test("""
            val CO
            status(CO)
        """)
        assert(out == " |  anon : (lin 3, col 13) : status(CO)\n" +
                " v  throw : expected running coroutine\n") { out }
    }
    @Test
    fun ii_02_status_err() {
        val out = test("""
            val CO = coro' () { nil }
            status(CO)
        """)
        assert(out == " |  anon : (lin 3, col 13) : status(CO)\n" +
                " v  throw : expected running coroutine\n") { out }
    }
    @Test
    fun ii_02_status() {
        val out = test("""
            val CO = coro' () { await(true) ;;;thus { it => nil };;; }
            val co = coroutine(CO)
            print(status(co))
            resume co()
            print(status(co))
            resume co()
            print(status(co))
        """,)
        assert(out == ":yielded\n:yielded\n:terminated\n") { out }
    }

    // IT

    @Test
    fun jj_01_it() {
        val out = test("""
            val CO = coro' () {
                func' (it) {
                    print(:it, it)
                } (yield(nil))
            }
            val co = coroutine(CO)
            resume co()
            resume co()
        """,)
        assert(out == ":it\tnil\n") { out }
    }
    @Test
    fun jj_02_it() {
        val out = test("""
            val CO = coro' () {
                func' (x) {
                    print(:it, x)
                } (yield(nil))
            }
            val co = coroutine(CO)
            resume co()
            resume co([])
        """,)
        assert(out == ":it\t[]\n") { out }
    }
    @Test
    fun TODO_jj_03_it_err() {
        val out = test("""
            val CO = coro' (x) {     ;; TODO: x
                func' (x) {          ;; x redeclared
                    print(:it, x)
                } (yield(nil))
            }
            val co = coroutine(CO)
            resume co()
            resume co([])
        """,)
        assert(out == "anon : (lin 3, col 23) : declaration throw : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun jj_04_it_data() {
        val out = test("""
            data :X = [x]
            val CO = coro' () {
                func' (x :X) {
                    print(:it, x.x)
                } (yield(nil))
            }
            val co = coroutine(CO)
            resume co()
            resume co([10])
        """,)
        assert(out == ":it\t10\n") { out }
    }
    @Test
    fun TODO_jj_04_it_it_err() {
        val out = test("""
            val CO = coro' () {
                func' (x) {          ;; TODO: x
                    func' (x) {      ;; x redeclared
                        x
                    } (yield(nil))
                } (yield(nil))
            }
        """,)
        //assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing yield\n") thus { out }
        assert(out == "anon : (lin 4, col 27) : declaration throw : variable \"x\" is already declared\n") { out }
    }

    // INDEX / TUPLE / VECTOR / DICT

    @Test
    fun kk_01_tup() {
        val out = test("""
            val CO = coro' () {
                val i = 1
                do { func' (it) { [99,10,99]} } (yield(nil)) [1]
            }
            val co = coroutine(CO)
            resume co()
            print(resume co())
        """,)
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_02_vec() {
        val out = test("""
            val CO = coro' () {
                val i = 1
                do { func' (it) { #[99,10,99]} } (yield(nil)) [1]
            }
            val co = coroutine(CO)
            resume co()
            print(resume co())
        """,)
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_03_dic() {
        val out = test("""
            val CO = coro' () {
                val i = 1
                (func' (it) { @[(1,10)]}) (yield(nil)) [1]
            }
            val co = coroutine(CO)
            resume co()
            print(resume co())
        """,)
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_04_tup() {
        val out = test("""
            val CO = coro' () {
                [func' (it) { [10] } (yield(nil))]
            }
            val co = coroutine(CO)
            resume co()
            print(resume co())
        """,)
        assert(out == "[[10]]\n") { out }
    }
    @Test
    fun kk_05_vec() {
        val out = test("""
            val CO = coro' () {
                #[do {await(true); 10}]
            }
            val co = coroutine(CO)
            resume co()
            print(resume co())
        """,)
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun kk_06_dic() {
        val out = test("""
            val CO = coro' () {
                @[(do { await(true) ; :x }, do { await(true) ; 10})]
            }
            val co = coroutine(CO)
            resume co()
            resume co()
            print(resume co())
        """,)
        assert(out == "@[(:x,10)]\n") { out }
    }
    @Test
    fun kk_07_set() {
        val out = test("""
            val CO = coro' () {
                val t = [99,99,99]
                set t[do { await(true) ; 1}] = 10
                t[1]
            }
            val co = coroutine(CO)
            resume co()
            print(resume co())
        """,)
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_08_call() {
        val out = test("""
            val CO = coro' () {
                await(true)
                print (
                    do { await(true) ; 1 },
                    do { await(true) ; 2 }
                )
            }
            val co = coroutine(CO)
            resume co()
            resume co()
            resume co()
            resume co()
        """,)
        assert(out == "1\t2\n") { out }
    }

    // YIELD / BLOCK

    @Test
    fun kk_01_scope() {
        val out = test("""
            val T = coro' () {
                val v = func' (it) { 
                    print(it)
                    10
                } (yield(nil))
                await(true)
                print(v)                
            }
            val t = coroutine(T)
            resume t()
            do {
                val v = []
                resume t(v)
            }
            resume t()
        """)
        assert(out == "[]\n10\n") { out }
        //assert(out == " |  anon : (lin 14, col 17) : (resume (t)(v))\n" +
        //        " v  anon : (lin 6, col 20) : resume throw : cannot receive alien reference\n") { out }
    }
    @Test
    fun kk_02_scope() {
        val out = test("""
            val T = coro' () {
                val t = []
                yield(t)
                print(t)                
            }
            val t = coroutine(T)
            do {
                resume t()
            }
            resume t()
        """)
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : (resume (t)(nil))\n" +
        //        " v  anon : (lin 4, col 17) : yield throw : cannot return pending reference\n") { out }
    }

    // KILL

    @Test
    fun ll_01_kill() {
        val out = test("""
            coroutine(coro' () {
                print(:no)
            })
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ll_02_kill() {
        val out = test("""
            val t = @[]
            val co = coroutine(coro' () {
                defer {
                    print(t)
                    ;;nil
                }
                await(true)
                print(:no)
            })
            resume co()
            print(:ok)
        """)
        assert(out == ":ok\n@[]\n") { out }
    }
    @Test
    fun ll_03_coro_defer() {
        val out = test("""
            val T = coro' () {
                defer {
                    print(:ok)
                }
                await(true)
            }
            resume (coroutine(T)) ()
            print(:end)
        """)
        assert(out == ":ok\n:end\n") { out }
    }
    @Test
    fun ll_04_coro_defer() {
        val out = test("""
            val T = coro' () {
                defer {
                    print(:1)
                    print(:2)
                }
                await(true)
            }
            val co = coroutine(T)
            resume co()
            print(:end)
        """)
        assert(out == ":end\n:1\n:2\n") { out }
    }

    // NESTED

    @Test
    fun mm_01_nested() {
        val out = test("""
            resume coroutine(coro' () {
                val v = 10
                resume coroutine(coro' () {
                    print(v)
                }) ()
                await(true)
            }) ()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_02_nested() {
        val out = test("""
            val F = func' () {
                val v = 10
                val f = func' () {
                    v
                }
                f()
            }
            print(F())
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_03_nested() {
        val out = test("""
            val co = coroutine(coro' () {
                var xxx = 1
                await(true)
                resume coroutine(coro' () {
                    set xxx = 10
                }) ()
                print(xxx)
            })
            resume co()
            resume co()
        """)
        //assert(out == "10\n") { out }
        assert(out == "anon : (lin 6, col 25) : access throw : outer variable \"xxx\" must be immutable\n") { out }
    }
    @Test
    fun mm_04_nested() {
        val out = test("""
            val a = coroutine(coro' () {
                val t = []
                val b = coroutine(coro' () {
                    val x = []
                    await(true)
                    print(t,x)
                })
                resume b()
                await(true)
                resume b()
            })
            resume a()
            resume a()
        """)
        assert(out == "[]\t[]\n") { out }
    }
    @Test
    fun mm_05_nested() {
        val out = test("""
            val a = coroutine(coro' () {
                val t = []
                val b = coroutine(coro' () {
                    val x = []
                    await(true)
                    print(t,x)
                })
                resume b()
                await(true)
                resume b()
            })
            resume a()
            resume (coroutine(coro' () { nil })) ()
            resume a()
        """)
        assert(out == "[]\t[]\n") { out }
    }
    @Test
    fun mm_06_mem() {
        val out = test("""
            resume coroutine(coro' () {
                val xx = 2
                resume coroutine(coro' () {
                    print(xx)
                }) ()
            }) ()
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun mm_07_upv () {
        val out = test("""
            val v = 10
            resume coroutine(coro' () {
                print(v)
            }) ()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_08_upv () {
        val out = test("""
            do {
                val v = 10
                resume coroutine(coro' () {
                    print(v)
                }) ()
            }
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 5, col 29) : access throw : cannot access local across coro\n") { out }
    }
    @Test
    fun mm_09_upv () {
        val out = test("""
            do {
                val v = 10
                (func' () {
                    print(v)
                }) ()
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_10_upv () {
        val out = test("""
            (func' () {
                val v = 10
                resume coroutine(coro' () {
                    print(v)
                }) ()
            }) ()
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 5, col 29) : access throw : cannot access local across coro\n") { out }
    }
    @Test
    fun mm_11_nst() {
        val out = test("""
            val T = coro' (t) {
                val ang = [0]
                val C2 = coro' () {
                    set ang[0] = 10
                }
                resume (coroutine(C2)) ()
                print(ang)
            }
            resume (coroutine(T)) ()
        """)
        assert(out == "[10]\n") { out }
    }

    // YIELD / ENCLOSING / ERROR

    @Test
    fun nn_01_yield() {
        val out = test("""
            coro' () {
                func' (x) {
                    await(true) ;;thus { y => nil }
                } (yield(nil))
            }
        """)
        //assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing thus\n") { out }
        assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing func\n") { out }
    }
    @Test
    fun nn_02_catch() {
        val out = test("""
            val CO = coro' () {
                catch :x ;;;( it | do {
                    await(true)
                } );;;
                {
                    throw(:e1)
                }
            }
            resume (coroutine(CO)) ()
        """)
        assert(out == " |  anon : (lin 10, col 13) : (resume (coroutine(CO))())\n" +
                " |  anon : (lin 7, col 21) : throw(:e1)\n" +
                " v  throw : :e1\n") { out }
        //assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing catch\n") { out }
    }

    // TMP / VAR

    @Test
    fun oo_01_tmp_index() {
        val out = test("""
            val CO = coro' () {
                val t = [1,2,3]
                t[await(true)]
            }
            val co = coroutine(CO)
            resume co()
            val v = resume co(1)
            print(v)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun oo_02_tmp_call() {
        val out = test("""
            val f = func' (a,b,c) {
                a
            }
            val CO = coro' () {
                val t = [1,2,3]
                f(1,await(true),3)
            }
            val co = coroutine(CO)
            resume co()
            val v = resume co(2)
            print(v)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun oo_03_tmp_tuple() {
        val out = test("""
            val CO = coro' () {
                [1,await(true),3]
            }
            val co = coroutine(CO)
            resume co()
            val v = resume co(2)
            print(v)
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun oo_04_tmp_tuple() {
        val out = test("""
            val CO = coro' () {
                val t = [await(true),await(true),await(true)]
                await(true)
                t
            }
            val co = coroutine(CO)
            resume co()
            resume co(1)
            resume co(2)
            resume co(3)
            val t = resume co()
            print(t)
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun oo_05_tmp_vector() {
        val out = test("""
            val CO = coro' () {
                val t = #[await(true),await(true),await(true)]
                await(true)
                t
            }
            val co = coroutine(CO)
            resume co()
            resume co(1)
            resume co(2)
            resume co(3)
            val t = resume co()
            print(t)
        """)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun oo_06_tmp_dict() {
        val out = test("""
            val CO = coro' () {
                val t = @[(1,yield(nil)),(await(true),20)]
                await(true)
                t
            }
            val co = coroutine(CO)
            resume co()
            resume co(10)
            resume co(2)
            val t = resume co()
            print(t)
        """)
        assert(out == "@[(1,10),(2,20)]\n") { out }
    }
    @Test
    fun oo_07_tmp_fun() {
        val out = test("""
            val f = func' (a,b,c) {
                b
            }
            val CO = coro' () {
                f(1,await(true),3)
            }
            val co = coroutine(CO)
            resume co()
            val v = resume co(10)
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oo_08_tmp() {
        val out = test("""
            val CO = coro' () {
                val f = func' (x) {
                    x
                }
                f(yield(nil))
            }
            val co = coroutine(CO)
            resume co()
            val v = resume co([])
            print(v)
        """,)
        assert(out == "[]\n") { out }
    }

    // ALL

    @Test
    fun zz_01_valgrind() {
        val out = test("""
            val f = func' () {
                nil
            }
            coroutine(coro' () {
                print(:no)
                f()
            })
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_02_valgrind() {
        val out = test("""
            resume coroutine (coro' () {
                val x
                do {
                    func' (it) {
                        nil
                    }(yield(nil))
                }
                val y
            }) ()
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_03_nested() {
        val out = test("""
            val CO = coro' (x) {
                func' () {
                    x
                }()
            }
            print(resume (coroutine(CO)) (10))
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun zz_04_valgrind() {
        val out = test("""
            val CO1 = coro' () {
                nil
            }
            val CO2 = coro' () {
                coroutine (CO1) ()
            }
            resume (coroutine (CO2)) ()
            print(:ok)
        """)
        assert(out == " |  anon : (lin 8, col 13) : (resume (coroutine(CO2))())\n" +
                " |  anon : (lin 6, col 17) : coroutine(CO1)()\n" +
                " v  throw : expected function\n") { out }
    }
    @Test
    fun zz_05_valgrind() {
        val out = test("""
            val CO1 = coro' () {
                nil
            }
            coroutine (CO1) ()
            print(:ok)
        """)
        assert(out == " |  anon : (lin 5, col 13) : coroutine(CO1)()\n" +
                " v  throw : expected function\n") { out }
    }
    @Test
    fun zz_06_mem() {
        val out = test("""
            func' () {
                coro' () {
                    val f = nil
                    await(true)
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
}
