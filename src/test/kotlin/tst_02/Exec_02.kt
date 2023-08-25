package tst_02

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_02 {

    // IS / IS-NOT

    // is', is-not´

    @Test
    fun aa_01_is() {
        val out = test("""
            println(is'([], :bool))
            println(is'([], :tuple))
            println(is-not'(1, :tuple))
            println(is-not'(1, :number))
        """)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun aa_02_is() {
        val out = test("""
            val t = []
            tags(t,:x,true)
            println(is'(t, :x))
            tags(t,:y,true)
            println(is-not'(t, :y))
            tags(t,:x,false)
            println(is-not'(t, :x))
        """)
        assert(out == "true\nfalse\ntrue\n") { out }
    }

    // POINTER-TO-STRING

    @Test
    fun bb_01() {
        val out = test("""
            val ptr = `:pointer "abc"`
            val str = pointer-to-string(ptr)
            println(str)
        """)
        assert(out == "abc\n") { out }
    }

    // DEFER

    @Test
    fun ee_01_defer() {
        val out = test("""
            println(1)
            defer { println(2) }
            defer { println(3) }
            println(4)
        """)
        assert(out == "1\n4\n3\n2\n") { out }
    }
    @Test
    fun ee_02_defer() {
        val out = test("""
            var f
            set f = func () {
                println(111)
                defer { println(222) }
                println(333)
            }
            defer { println(1) }
            do {
                println(2)
                defer { println(3) }
                println(4)
                do {
                    println(5)
                    f()
                    defer { println(6) }
                    println(7)
                }
                println(8)
                defer { println(9) }
                println(10)
            }
            println(11)
            defer { println(12) }
            println(13)
        """)
        assert(out == "2\n4\n5\n111\n333\n222\n7\n6\n8\n10\n9\n3\n11\n13\n12\n1\n") { out }
    }

    // THROW / CATCH

    @Test
    fun jj_01_catch() {
        val out = test("""
            val err = catch :x {
                throw(:x)
                println(9)
            }
            println(err)
        """)
        assert(out == ":x\n") { out }
    }
    @Test
    fun jj_02_catch_err() {
        val out = test("""
            catch :x {
                throw(:y)
                println(9)
            }
            println(1)
        """)
        assert(out == " |  anon : (lin 3, col 17) : throw(:y)\n" +
                " v  throw error : :y\n") { out }
    }
    @Test
    fun jj_03_catch_err() {
        val out = test("""
            val f = func () {
                throw(:y)
                println(9)
            }
            catch :x {
                f()
                println(9)
            }
            println(1)
        """)
        assert(out == " |  anon : (lin 7, col 17) : f()\n" +
                " |  anon : (lin 3, col 17) : throw(:y)\n" +
                " v  throw error : :y\n") { out }
    }
    @Test
    fun jj_04_catch() {
        val out = test("""
            var f
            set f = func () {
                catch :xxx {
                    throw(:yyy)
                    println(91)
                }
                println(9)
            }
            catch :yyy {
                catch :xxx {
                    f()
                    println(92)
                }
                println(93)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_05_catch_valgrind() {
        val out = test("""
            catch :x {
                throw([])
                println(9)
            }
            println(1)
        """.trimIndent())
        //assert(out == "anon : (lin 2, col 5) : throw error : expected tag\n") { out }
        assert(out == " |  anon : (lin 2, col 5) : throw([])\n" +
                " v  throw error : []\n") { out }
    }
    @Test
    fun jj_06_catch() {
        val out = test("""
            catch :e1 {
                catch :e2 {
                    catch :e3 {
                        catch :e4 {
                            println(1)
                            throw(:e3)
                            println(99)
                        }
                        println(99)
                    }
                    println(2)
                    throw(:e1)
                    println(99)
                }
                println(99)
            }
            println(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun jj_07_catch_err() {
        val out = test("""
            catch {
                throw(:y)
                println(9)
            }
            println(1)
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 1) : catch error : expected tag\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_08_catch() {
        val out = test(
            """
            catch do {
                :x
            } {
                throw(:x)
                println(9)
            }
            println(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_09_catch() {
        val out = test(
            """
            catch false {
                throw(:xxx)
                println(9)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == " |  anon : (lin 2, col 5) : throw(:xxx)\n" +
                " v  throw error : :xxx\n") { out }
    }
    @Test
    fun jj_10_catch() {
        val out = test("""
            catch [] {
                throw([])
                println(9)
            }
            println(1)
        """)
        assert(out == " |  anon : (lin 3, col 17) : throw([])\n" +
                " v  throw error : []\n") { out }
    }
    @Test
    fun jj_11_catch() {
        val out = test("""
            catch [] {
                val xxx = []
                throw(xxx)
            }
            println(1)
        """)
        assert(out == " v  anon : (lin 2, col 22) : block escape error : incompatible scopes\n") { out }
    }
    @Test
    fun jj_12_catch() {
        val out = test("""
            val t = catch {
                val xxx = []
                throw(drop(xxx))
            }
            println(t)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun todo_jj_13_throw_catch_condition() {
        val out = test("""
            catch throw(2) {
                throw(1)
            }
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in catch condition\"' failed.")) { out }
    }

    // THROW/CATCH / DEFER

    @Test
    fun todo_pp_01_throw_defer() {
        val out = test("""
            catch nil {
                defer {
                    throw(nil)
                }
            }
            println(:ok)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun todo_pp_02_defer_err() {
        val out = test("""
            val v = do {
                defer {
                    println(:1)
                }
                defer {
                    println(:2)
                    throw(:err)
                    println(:3)
                }
                defer {
                    println(:4)
                }
            }
            println(:ok, v)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }

    // MISC

    @Test
    fun zz_01_func_err() {
        val out = test("1(1)")
        assert(out == " v  anon : (lin 1, col 1) : call error : expected function\n") { out }
    }
    @Test
    fun zz_02_func_err() {
        val out = test("""
            val f = func () {
                1(1)
            }
            f()
        """)
        assert(out == " |  anon : (lin 5, col 13) : f()\n" +
                " v  anon : (lin 3, col 17) : call error : expected function\n") { out }
    }

}
