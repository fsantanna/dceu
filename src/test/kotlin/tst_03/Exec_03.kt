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
    fun cc_01_coro_resume() {
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
    fun cc_02_coro_resume_dead_err() {
        val out = test("""
            val co = coroutine(coro () {nil})
            resume co()
            resume co()
        """)
        assert(out == " v  anon : (lin 4, col 13) : resume error : expected yielded coro\n") { out }
    }
    @Test
    fun cc_03_coro_resume_dead_err() {
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
