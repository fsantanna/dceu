package tst_02

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_02 {

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
            do {
                println(1)
                defer { println(2) }
                println(3)
            }
            println(4)
            defer { println(5) }
            println(6)
        """)
        assert(out == "2\n4\n5\n111\n333\n222\n7\n6\n8\n10\n9\n3\n11\n13\n12\n1\n") { out }
    }
    @Test
    fun ee_03_defer() {
        val out = test("""
            val f = func () {
                defer { 99 }
                1
            }
            println(f())
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ee_04_defer() {
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
    fun jj_00_0_err() {
        val out = test("""
            catch (it :T => it[0]) {
                nil
            }
            println(:ok)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_00_catch_err() {
        val out = test("""
            val err = catch ( it => set it=nil ) {
                throw(:x)
            }
        """)
        assert(out == "anon : (lin 2, col 37) : set error : destination is immutable\n") { out }
    }
    @Test
    fun jj_01_catch() {
        val out = test("""
            val err = catch ( v => v==:x ) {
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
            catch (it=>it==:x) {
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
            catch (it=>it==:x) {
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
                catch (it => it==:xxx) {
                    throw(:yyy)
                    println(91)
                }
                println(9)
            }
            catch (it => it==:yyy) {
                catch (it2 => it2==:xxx) {
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
        DEBUG = true
        val out = test("""
            catch (it=> it==:x) {
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
            catch ( it=>it==:e1) {
                catch ( it=>it==:e2) {
                    catch ( it=>it==:e3) {
                        catch ( it => it==:e4 ) {
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
            catch ( it => true ) {
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
            catch ( it => it==do {
                :x
            } ) {
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
            catch ( it => false) {
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
            catch (it => false) {
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
            catch ( it => it==[]) {
                val xxx = []
                throw(xxx)
            }
            println(1)
        """)
        //assert(out == " v  anon : (lin 2, col 35) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 4, col 17) : throw(xxx)\n" +
                " v  throw error : []\n") { out }
    }
    @Test
    fun jj_12_catch() {
        val out = test("""
            val t = catch ( it=>true) {
                val xxx = []
                throw(drop(xxx))
            }
            println(t)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun BUG_jj_13_throw_catch_condition() {
        val out = test("""
            catch ( it => throw(2)) {
                throw(1)
            }
        """)
        assert(out.contains("main: Assertion `ceu_acc.type!=CEU_VALUE_THROW && \"TODO: throw in catch condition\"' failed.")) { out }
    }
    @Test
    fun jj_14_blocks() {
        val out = test("""
            val v = catch (it => true) {
                do {
                    throw(:x)
                }
                println(9)
            }
            println(v)
        """)
        assert(out == ":x\n") { out }
    }
    @Test
    fun jj_13_catch_dcl_err() {
        val out = test("""
            val x
            catch ( x => true) {
                nil
            }
        """)
        assert(out == "anon : (lin 3, col 21) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun jj_14_catch_data() {
        val out = test("""
            data :X = [x]
            catch ( x:X => x.x==10 ) {
                throw([10])
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_15_catch_set() {
        val out = test("""
            var x
            catch ( it => do {
                set x = it
                it[0]==:x
            }) {
                throw([:x])
                println(9)
            }
            println(x)
        """)
        assert(out == "[:x]\n") { out }
    }
    @Test
    fun jj_17_throw() {
        val out = test("""
            do {
                val t = @[]
                throw(t)
                nil
            }
        """)
        assert(out == " |  anon : (lin 4, col 17) : throw(t)\n" +
                " v  throw error : @[]\n") { out }
        //assert(out.contains(" v  anon : (lin 2, col 13) : block escape error : cannot copy reference out\n")) { out }
    }
    @Test
    fun jj_18_throw_err() {
        val out = test("""
            val x = catch (it => true) {
                val t = @[]
                throw(t)
                nil
            }
            println(x)
        """)
        assert(out == "@[]\n") { out }
    }

    // CALL STACK

    @Test
    fun kk_01_func_err() {
        val out = test("1(1)")
        assert(out == " v  anon : (lin 1, col 1) : call error : expected function\n") { out }
    }
    @Test
    fun kk_02_func_err() {
        val out = test("""
            val f = func () {
                1(1)
            }
            f()
        """)
        assert(out == " |  anon : (lin 5, col 13) : f()\n" +
                " v  anon : (lin 3, col 17) : call error : expected function\n") { out }
    }
    @Test
    fun kk_03_func_args() {
        val out = test(
            """
            val f = func (x) {
                println(x)
            }
            f(10)
        """
        )
        assert(out == "10\n") { out }
    }

    // THROW/CATCH / DEFER

    @Test
    fun BUG_pp_01_throw_defer() {
        val out = test("""
            catch ( it => true) {
                defer {
                    throw(nil)
                }
            }
            println(:ok)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun BUG_ppx_02_defer_err() {
        val out = test("""
            val v = do {
                defer {
                    println(:2)
                }
                defer {
                    println(:1)
                    throw(:err)     ;; ERR
                }
            }
            println(:3)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun BUG_pp_02_defer_err() {
        val out = test("""
            val v = do {
                defer {
                    println(:1)
                }
                defer {
                    println(:2)
                    throw(:err)     ;; ERR
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
    @Test
    fun pp_03_throw_defer() {
        val out = test("""
            defer {
                nil
            }
            throw(:error)
        """)
        assert(out == " |  anon : (lin 5, col 13) : throw(:error)\n" +
                " v  throw error : :error\n") { out }
    }
    @Test
    fun BUG_pp_04_throw_defer() {
        val out = test("""
            defer {
                throw(:2)
            }
            throw(:1)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun BUG_pp_05_throw_defer() {
        val out = test("""
            do {
                defer {
                    throw(nil)
                }
            }
            println(:ok)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }


    // ORIGINAL

    @Test
    fun zz_01() {
        val out = test("""
            do {
                catch (it => do {
                    val x = it
                    println(it) ;; [:x]
                    false
                }) {
                    throw([:x])
                    println(:no)
                }
            }
            println(:ok)
        """)
        assert(out == "[:x]\n" +
                " |  anon : (lin 8, col 21) : throw([:x])\n" +
                " v  throw error : [:x]\n") { out }
    }
    @Test
    fun zz_02() {
        val out = test("""
            do {
                val y = catch (it => do {
                    val x = it
                    println(it) ;; [:x]
                    x
                }) {
                    throw([:x])
                    println(:no)
                }
                println(y)
            }
            println(:ok)
        """)
        assert(out == ("[:x]\n[:x]\n:ok\n"))
    }

}
