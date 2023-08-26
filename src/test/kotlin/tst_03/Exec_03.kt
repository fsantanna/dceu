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
                yield(v)
            }
            println(t)
        """)
        assert(out.contains("coro: 0x")) { out }
    }
    @Test
    fun aa_03_yield_err() {
        val out = test("""
            yield(nil)
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
        assert(out.contains("x-coro: 0x")) { out }
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
                yield(nil)
                println(2)
                yield(nil)
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
                yield(10)
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
                val v = yield(nil)
                println(v)
            }
            val co = coroutine(CO)
            resume co(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_08_resume_yield() {
        val out = test("""
            $PLUS
            val CO = coro () {
                yield(10)
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
            $PLUS
            val CO = coro (v1) {        ;; 10
                val v2 = yield(v1+1)    ;; 12
                v2 + 1
            }
            val co = coroutine(CO)
            val v1 = resume co(10)      ;; 11
            val v2 = resume co(v1+1)    ;; 13
            println(v2)
        """)
        assert(out == "13\n") { out }
    }

    ///////////
    @Test
    fun zz_04_tags() {
        val out = test("""
            val co = coro () {
                yield(:x)
            }
            println(:y)
        """)
        assert(out == ":y\n") { out }
    }
}
