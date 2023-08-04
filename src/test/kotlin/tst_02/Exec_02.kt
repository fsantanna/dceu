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
        """, true)
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
        """, true)
        assert(out == "true\nfalse\ntrue\n") { out }
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
    @Test
    fun todo_ee_03_defer_err() {
        val out = test("""
            task () {
                defer {
                    yield(nil)   ;; no yield inside defer
                }
            }
            println(1)
        """)
        assert(out == "TODO") { out }
    }
    @Test
    fun ee_05_defer_err() {
        val out = test("""
            do {
                defer {
                    throw(nil)
                }
            }
            println(:ok)
        """)
        assert(out == "anon : (lin 4, col 21) : throw(nil)\n" +
                "throw error : uncaught exception\n" +
                "nil\n") { out }
    }

    // THROW / CATCH

    @Test
    fun jj_01_catch() {
        val out = test("""
            catch err==:x {
                throw(:x)
                println(9)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_02_catch_err() {
        val out = test("""
            catch err==:x {
                throw(:y)
                println(9)
            }
            println(1)
        """.trimIndent())
        assert(out == "anon : (lin 2, col 5) : throw(:y)\n" +
                "throw error : uncaught exception\n" +
                ":y\n") { out }
    }
    @Test
    fun jj_03_catch() {
        val out = test("""
            var f
            set f = func () {
                catch err==:xxx {
                    throw(:yyy)
                    println(91)
                }
                println(9)
            }
            catch err==:yyy {
                catch err==:xxx {
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
    fun jj_04_catch_valgrind() {
        val out = test("""
            catch err==:x {
                throw([])
                println(9)
            }
            println(1)
        """.trimIndent())
        //assert(out == "anon : (lin 2, col 5) : throw error : expected tag\n") { out }
        assert(out == "anon : (lin 2, col 5) : throw([])\n" +
                "throw error : uncaught exception\n" +
                "[]\n") { out }
    }
    @Test
    fun jj_05_catch() {
        val out = test("""
            catch err==:e1 {
                catch err==:e2 {
                    catch err==:e3 {
                        catch err==:e4 {
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
    fun jj_06_catch_err() {
        val out = test("""
            catch true {
                throw(:y)
                println(9)
            }
            println(1)
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 1) : catch error : expected tag\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_07_catch() {
        val out = test(
            """
            catch do {
                err==:x
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
    fun jj_08_catch() {
        val out = test(
            """
            var x
            catch do {
                set x = err
                err[0]==:x
            } {
                throw([:x])
                println(9)
            }
            println(x)
        """
        )
        //assert(out == "anon : (lin 4, col 21) : set error : incompatible scopes\n" +
        //        "anon : (lin 7, col 17) : throw([:x])\n" +
        //        "throw error : uncaught exception\n" +
        //        ":error\n") { out }
        assert(out == "[:x]\n") { out }
    }
    @Test
    fun jj_09_catch_err() {
        val out = test(
            """
            do {
                catch do {  ;; err is binded to x and is being moved up
                    var x
                    set x = err
                    println(err) `/* XXXX */`
                    false
                } {
                    throw([:x])
                    println(9)
                }
            }
            println(:ok)
            """
        )
        //assert(out == "anon : (lin 2, col 13) : set error : incompatible scopes\n" +
        //        "anon : (lin 8, col 21) : throw([:x])\n" +
        //        "throw error : uncaught exception\n" +
        //        ":error\n") { out }
        //assert(out == "anon : (lin 5, col 25) : set error : incompatible scopes\n" +
        //        "anon : (lin 9, col 21) : throw([:x])\n" +
        //        "throw error : uncaught exception\n" +
        //        ":error\n") { out }
        assert(out == "anon : (lin 11, col 17) : rethrow error : incompatible scopes\n" +
                "anon : (lin 9, col 21) : throw([:x])\n" +
                "throw error : uncaught exception\n" +
                "[:x]\n" +
                ":error\n") { out }
    }
    @Test
    fun jj_10_catch() {
        val out = test(
            """
            var x
            catch do {
                set x = err
                err==:x
            } {
                throw(:x)
                println(9)
            }
            println(x)
        """
        )
        assert(out == ":x\n") { out }
    }
    @Test
    fun jj_11_catch_err() {
        val out = test(
            """
            catch err[0]==:x {
                throw([:x])
                println(9)
            }
            println(err)
        """
        )
        //assert(out == "nil\n") { out }
        assert(out.contains("error: ‘ceu_err’ undeclared")) { out }
    }
    @Test
    fun jj_12_catch() {
        val out = test(
            """
            catch false {
                throw(:xxx)
                println(9)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == "anon : (lin 2, col 5) : throw(:xxx)\n" +
                "throw error : uncaught exception\n" +
                ":xxx\n") { out }
    }
    @Test
    fun jj_13_catch() {
        val out = test("""
            catch err==[] {
                throw([])
                println(9)
            }
            println(1)
        """)
        assert(out == "anon : (lin 3, col 17) : throw([])\n" +
                "throw error : uncaught exception\n" +
                "[]\n") { out }
    }
    @Test
    fun jj_14_catch() {
        val out = test(
            """
            var x
            set x = err
            do {
                set x = err
            }
            println(1)
            """
        )
        //assert(out == "anon : (lin 4, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "1\n") { out }
        assert(out.contains("error: ‘ceu_err’ undeclared")) { out }
    }
    @Test
    fun jj_15_catch() {
        val out = test("""
            catch err==[] {
                var xxx
                set xxx = []
                throw(xxx)
            }
            println(1)
        """)
        assert(out == "anon : (lin 2, col 27) : block escape error : incompatible scopes\n" +
                "anon : (lin 5, col 17) : throw(xxx)\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
        //assert(out == "anon : (lin 5, col 17) : throw(xxx) : throw error : incompatible scopes\n" +
        //        "throw error : uncaught exception\n" +
        //        ":error\n") { out }
    }

    // THROW/CATCH / DEFER

    @Test
    fun cc_01_throw_defer() {
        val out = test("""
            catch err==nil {
                defer {
                    throw(nil)
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

}
