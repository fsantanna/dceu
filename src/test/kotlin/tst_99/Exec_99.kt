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
}
