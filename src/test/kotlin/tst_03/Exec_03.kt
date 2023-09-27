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
            val t = coro (v) {
                v
            }
            println(t)
        """)
        assert(out.contains("coro: 0x")) { out }
    }
    @Test
    fun aa_02_coro() {
        val out = test("""
            val t = coro (v) {
                yield(v) { nil }
            }
            println(t)
        """)
        assert(out.contains("coro: 0x")) { out }
    }
    @Test
    fun aa_03_yield_err() {
        val out = test("""
            yield(nil) { nil }
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing coro\n") { out }
    }
    @Test
    fun aa_04_resume_err() {
        val out = test("""
            val f
            resume f()
        """)
        assert(out == " v  anon : (lin 3, col 13) : resume error : expected yielded coro\n") { out }
    }

    // COROUTINE

    @Test
    fun bb_01_coroutine_err() {
        val out = test("""
            val t = func (v) {
                v
            }
            val x = coroutine(t)
            println(x)
        """)
        assert(out == " v  anon : (lin 5, col 21) : coroutine(t) : coroutine error : expected coro\n") { out }
    }
    @Test
    fun bb_02_coroutine_err() {
        val out = test("""
            val t = coro (v) {
                v
            }
            val x = coroutine(t)
            println(x)
        """)
        assert(out.contains("exe-coro: 0x")) { out }
    }
    @Test
    fun bb_03_coroutine_err() {
        val out = test("""
            coroutine(func () {nil})
        """)
        assert(out == " v  anon : (lin 2, col 13) : coroutine((func () { nil })) : coroutine error : expected coro\n") { out }
    }

    // RESUME / YIELD

    @Test
    fun cc_01_resume() {
        val out = test("""
            val t = coro (v) {
                v
            }
            val a = coroutine(t)
            var v = resume a(1)
            println(v)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_02_resume_dead_err() {
        val out = test("""
            val co = coroutine(coro () {nil})
            resume co()
            resume co()
        """)
        assert(out == " v  anon : (lin 4, col 13) : resume error : expected yielded coro\n") { out }
    }
    @Test
    fun cc_03_resume_dead_err() {
        val out = test("""
            val CO = coro () {
                nil
            }
            val co = coroutine(CO)
            resume co()
            resume co()
        """)
        assert(out == " v  anon : (lin 7, col 13) : resume error : expected yielded coro\n") { out }
    }
    @Test
    fun cc_04_resume_yield() {
        val out = test("""
            val t = coro () {
                println(1)
                yield(nil) { nil }
                println(2)
                yield(nil) { nil }
                println(3)
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
            val t = coro (v) {
                println(v)
            }
            val a = coroutine(t)
            resume a(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_06_yield_ret() {
        val out = test("""
            val CO = coro () {
                yield(10) { nil }
            }
            val co = coroutine(CO)
            val v = resume co()
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_07_resume_yield() {
        val out = test("""
            $PLUS
            val CO = coro () {
                yield(nil) {
                    println(it)
                }
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
            val CO = coro () {
                yield(10) { nil }
            }
            val co = coroutine(CO)
            val v = resume co()
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_09_resume_yield() {
        val out = test("""
            val CO = coro (v1) {
                yield(v1) { it }
            }
            val co = coroutine(CO)
            val v1 = resume co(10)
            val v2 = resume co(v1)
            println(v2)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_10_resume() {
        val out = test("""
            $PLUS
            val CO = coro (v1) {    ;; 10
                yield(v1+1) {       ;; 12
                    it + 1
                }
            }
            val co = coroutine(CO)
            val v1 = resume co(10)      ;; 11
            val v2 = resume co(v1+1)    ;; 13
            println(v2)
        """)
        assert(out == "13\n") { out }
    }
    @Test
    fun cc_11_mult() {
        val out = test("""
            var co
            set co = coroutine(coro (x,y) {
                println(x,y)
            })
            resume co(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun todo_cc_12_multi_err() {
        val out = test("""
            var co
            set co = coroutine(coro () {
                yield(nil) { nil }
            })
            resume co()
            resume co(1,2)
        """)
        assert(out.contains("TODO: multiple arguments to resume")) { out }
    }
    @Test
    fun cc_13_tuple_leak() {
        val out = test("""
            val T = coro () {
                pass [1,2,3]
                yield(nil) { nil }
            }
            resume (coroutine(T)) ()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_14_coro_defer() {
        val out = test("""
            val T = coro () {
                defer {
                    println(:ok)
                }
                yield(nil)  { nil }  ;; never awakes
            }
            resume (coroutine(T)) ()
            println(:end)
        """)
        assert(out == ":end\n:ok\n") { out }
    }
    @Test
    fun cc_15_yield_err() {
        val out = test("""
            coro () {
                func () {
                    yield(nil) { nil }
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : expected enclosing coro\n") { out }
    }
    @Test
    fun cc_16_tags() {
        val out = test("""
            val co = coro () {
                yield(:x) { nil }
            }
            println(:y)
        """)
        assert(out == ":y\n") { out }
    }

    // MEM vs STACK

    @Test
    fun dd_01_stack() {
        val out = test("""
            val CO = coro (v1) {
                val v2 = 2
                println(v1, v2)
                ```
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
            val CO = coro (v1) {
                val v2 = 2
                println(v1, v2)
                yield(nil) { nil }
                ```
                printf("%f\t%f\n", ceu_mem->id_v1_131.Number, ceu_mem->id_v2_17.Number);
                ```
            }
            val co = coroutine(CO)
            resume co(1)
            resume co(1)
        """)
        assert(out == "1\t2\n1.000000\t2.000000\n") { out }
    }

    // ORIGINAL

    @Test
    fun ee_01_coro() {
        val out = test("""
            $PLUS
            var t
            set t = coro (v) {
                var v' = v
                println(v')          ;; 1
                set v' = yield((v'+1)) { it } 
                println(v')          ;; 3
                set v' = yield(v'+1) { it }
                println(v')          ;; 5
                v'+1
            }
            val a = coroutine(t)
            var v = resume a(1)
            println(v)              ;; 2
            set v = resume a(v+1)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """)
        assert(out == "1\n2\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun ee_02_coro_defer() {
        val out = test("""
            var T
            set T = coro () {
                defer {
                    println(3)
                }
                println(1)
                yield(nil) { nil }   ;; never awakes
                println(2)
            }
            println(0)
            resume (coroutine(T)) ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun ee_03_coro_defer() {
        val out = test("""
            var T
            set T = coro () {
                defer {
                    println(3)
                }
                println(1)
                yield(nil) { nil }   ;; never awakes
                println(2)
            }
            val t = coroutine(T)
            println(0)
            resume t ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun ee_04_coro_defer() {
        val out = test("""
            val T = coro () {
                println(1)
                yield(nil) { nil }   ;; never awakes
                defer {
                    println(999)
                }
            }
            resume (coroutine(T)) ()
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun ee_05_coro_defer() {
        val out = test("""
            var T
            set T = coro () {
                defer {
                    println(3)
                }
                println(1)
                yield(nil) { nil }   ;; never awakes
                defer {
                    println(999)
                }
                println(2)
            }
            println(0)
            resume (coroutine(T)) ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun ee_06_coro_defer() {
        val out = test("""
            val F = coro () {
                defer {
                    println(:xxx)
                }
                yield(nil) { nil }
                defer {
                    println(:yyy)
                }
                yield(nil) { nil }
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
                val F = (coro () { println(:ok) })
                val f = do {        ;; dst=2
                    coroutine(F)    ;; src=3
                }
                resume f()
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ee_08_yield_tmp() {
        val out = test("""
            val F = coro () {
                val :tmp x
                yield(nil) { nil }
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid declaration : \":tmp\" across yield\n") { out }
    }

    // DROP / MOVE / OUT

    @Test
    fun ff_01_drop() {
        val out = test("""
        val f = func (co) {
            resume co()
        }
        val C = coro () {
            var t = []
            yield(drop(t)) { nil }
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
        assert(out == ":out\t[]\n" +
                ":in\tnil\n") { out }
    }
    @Test
    fun ff_02() {
        val out = test("""
            var f
            set f = func () {
                coroutine(coro() {nil})
            }
            println(f())
        """)
        //assert(out == "anon : (lin 6, col 21) : f()\n" +
        //        "anon : (lin 3, col 29) : block escape error : incompatible scopes\n:error\n") { out }
        assert(out.contains("coro: 0x"))
    }
    @Test
    fun ff_03() {
        val out = test("""
            var T
            set T = coro () { nil }
            var xxx
            do {
                var t
                set t = coroutine(T)
                set xxx = t ;; error
            }
        """)
        assert(out == " v  anon : (lin 8, col 21) : set error : cannot copy reference to outer scope\n") { out }
    }

    // SCOPE

    @Test
    fun gg_01_scope() {
        val out = test("""
            val T = coro (v) {
                yield(nil) { nil }
                println(v)                
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
            val T = coro (v) {
                yield(nil) { nil }
                println(v)                
            }
            val t = coroutine(T)
            do {
                val v = []
                resume t(v)
            }
            resume t()
        """)
        assert(out == " |  anon : (lin 9, col 24) : t(v)\n" +
                " v  anon : (lin 2, col 30) : resume error : incompatible scopes\n") { out }
    }
    @Test
    fun gg_03_scope() {
        val out = test("""
            val T = coro () {
                val v = yield(nil) { it }
                yield(nil) { nil }
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
        assert(out == " |  anon : (lin 11, col 24) : t(v)\n" +
                " v  anon : (lin 3, col 25) : resume error : cannot receive assigned reference\n") { out }
    }
    @Test
    fun gg_04_scope() {
        val out = test("""
            val T = coro (v) {
                val e = yield(nil) { it }
                println(e)                
            }
            val t = coroutine(T)
            func () {
                resume t([])
                resume t([])
            }()
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun gg_05_scope() {
        val out = test("""
            val T = coro () {
                val x = []
                yield(x) { nil }    ;; err
                println(:in, x)
            }
            val t = coroutine(T)
            do {
                val x = resume t()
                println(:out, x)
            }
            resume t()
        """)
        assert(out == " |  anon : (lin 9, col 32) : t()\n" +
                " v  anon : (lin 4, col 17) : yield error : cannot receive assigned reference\n") { out }
    }
    @Test
    fun gg_06_scope() {
        val out = test("""
            val T = coro () {
                val x = []
                yield(drop(x)) { nil }    ;; err
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
    }

    // CATCH / THROW

    @Test
    fun hh_01_throw() {
        val out = test("""
            var co
            set co = coroutine(coro (x,y) {
                throw(:e2)
            })
            catch :e2 {
                resume co(1,2)
                println(99)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun hh_02_throw() {
        val out = test("""
            var co
            set co = coroutine(coro (x,y) {
                yield(nil) { nil }
                throw(:e2)
            })
            catch :e2 {
                resume co(1,2)
                println(1)
                resume co()
                println(2)
            }
            println(3)
        """)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun hh_03_throw() {
        val out = test("""
            var co
            set co = coroutine (coro () {
                catch :e1 {
                    yield(nil) { nil }
                    throw(:e1)
                }
                println(:e1)
                yield(nil) { nil }
                throw(:e2)
            })
            catch :e2 {
                resume co()
                resume co()
                resume co()
                println(99)
            }
            println(:e2)
        """)
        assert(out == ":e1\n:e2\n") { out }
    }
    @Test
    fun hh_04_catch_yield_err() {
        val out = test("""
            coro () {
                catch do {
                    yield(nil) { nil }
                }
                {
                    throw(:e1)
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing catch\n") { out }
    }
    @Test
    fun hh_05_throw() {
        val out = test(
            """
            val CO = coro () {
                catch false {
                    yield(nil) { nil }
                }
                println(999)
            }
            val co = coroutine(CO)
            resume co()
            catch true {
                throw(nil)
            }
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    // STATUS

    @Test
    fun ii_01_status_err() {
        val out = test("""
            val CO
            status(CO)
        """)
        assert(out == " v  anon : (lin 3, col 13) : status(CO) : status error : expected running coroutine\n") { out }
    }
    @Test
    fun ii_02_status_err() {
        val out = test("""
            val CO = coro () { nil }
            status(CO)
        """)
        assert(out == " v  anon : (lin 3, col 13) : status(CO) : status error : expected running coroutine\n") { out }
    }
    @Test
    fun ii_02_status() {
        val out = test("""
            val CO = coro () { yield(nil) { nil } }
            val co = coroutine(CO)
            println(status(co))
            resume co()
            println(status(co))
            resume co()
            println(status(co))
        """,)
        assert(out == ":yielded\n:yielded\n:terminated\n") { out }
    }

    // IT

    @Test
    fun jj_01_it() {
        val out = test("""
            val CO = coro () {
                yield(nil) {
                    println(:it, it)
                }
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
            val CO = coro () {
                yield(nil) {
                    println(:it, it)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co([])
        """,)
        assert(out == ":it\t[]\n") { out }
    }

    // YIELD / BLOCK

    @Test
    fun kk_01_yield_err() {
        val out = test("""
            coro () {
                yield(nil) {
                    yield(nil) { nil }
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing yield\n") { out }
    }
    @Test
    fun kk_02_scope() {
        val out = test("""
            val T = coro () {
                val v = yield(nil) {
                    println(it)
                    10
                }
                yield(nil) { nil }
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
        assert(out == "[]\n10\n") { out }
    }
}
