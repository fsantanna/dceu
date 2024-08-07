package tst_02

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_02 {

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
        assert(out == "1\n3\n2\n4\n6\n5\n") { out }
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
            do {
                defer { println(3) }
                do {
                    defer { println(6) }
                }
            }
            defer { println(12) }
        """)
        assert(out == "6\n3\n12\n") { out }
    }
    @Test
    fun ee_05_defer() {
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
            catch (it :T| it[0]) {
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
            val err = catch ( it |  set it=nil ) {
                error(:x)
            }
        """)
        assert(out == "anon : (lin 2, col 37) : set error : destination is immutable\n") { out }
    }
    @Test
    fun jj_01_catch() {
        val out = test("""
            val err = catch (v| do {
                ;;println(:v,v)
                v == :x
            }) {
                error(:x)
                println(9)
            }
            println(err)
        """)
        assert(out == ":x\n") { out }
    }
    @Test
    fun jj_02_catch_err() {
        val out = test("""
            catch (it|it==:x) {
                error(:y)
                println(9)
            }
            println(1)
        """)
        assert(out == " |  anon : (lin 3, col 17) : error(:y)\n" +
                " v  error : :y\n") { out }
    }
    @Test
    fun jj_03_catch_err() {
        val out = test("""
            val f = func () {
                error(:y)
                println(9)
            }
            catch (it|it==:x) {
                f()
                println(9)
            }
            println(1)
        """)
        assert(out == " |  anon : (lin 7, col 17) : f()\n" +
                " |  anon : (lin 3, col 17) : error(:y)\n" +
                " v  error : :y\n") { out }
    }
    @Test
    fun jj_04_catch() {
        val out = test("""
            var f
            set f = func () {
                catch (it | it==:xxx) {
                    error(:yyy)
                    println(91)
                }
                println(9)
            }
            catch (it | it==:yyy) {
                catch (it2 | it2==:xxx) {
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
            catch (it| it==:x) {
                error([])
                println(9)
            }
            println(1)
        """)
        //assert(out == "anon : (lin 2, col 5) : throw error : expected tag\n") { out }
        assert(out == " |  anon : (lin 3, col 17) : error([])\n" +
                " v  error : []\n") { out }
    }
    @Test
    fun jj_06_catch() {
        val out = test("""
            catch ( it|it==:e1) {
                catch ( it|it==:e2) {
                    catch ( it|it==:e3) {
                        catch ( it | it==:e4 ) {
                            println(1)
                            error(:e3)
                            println(99)
                        }
                        println(99)
                    }
                    println(2)
                    error(:e1)
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
            catch ( it | true ) {
                error(:y)
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
            catch ( it | it==do {
                :x
            } ) {
                error(:x)
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
            catch ( it | false) {
                error(:xxx)
                println(9)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == " |  anon : (lin 2, col 5) : error(:xxx)\n" +
                " v  error : :xxx\n") { out }
    }
    @Test
    fun jj_10_catch() {
        val out = test("""
            catch (it | false) {
                error([])
                println(9)
            }
            println(1)
        """)
        assert(out == " |  anon : (lin 3, col 17) : error([])\n" +
                " v  error : []\n") { out }
    }
    @Test
    fun jj_11_catch() {
        val out = test("""
            catch ( it | it==[]) {
                val xxx = []
                error(xxx)
            }
            println(1)
        """)
        //assert(out == " v  anon : (lin 2, col 35) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 4, col 17) : error(xxx)\n" +
                " v  error : []\n") { out }
    }
    @Test
    fun jj_12_catch() {
        val out = test("""
            val t = catch ( it|true) {
                val xxx = []
                error(;;;drop;;;(xxx))
            }
            println(t)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun BUG_jj_13_throw_catch_condition() {
        val out = test("""
            catch ( it | error(2)) {
                error(1)
            }
        """)
        assert(out.contains("main: Assertion `ceu_acc.type!=CEU_VALUE_THROW && \"TODO: throw in catch condition\"' failed.")) { out }
    }
    @Test
    fun jj_14_blocks() {
        val out = test("""
            val v = catch (it | true) {
                do {
                    error(:x)
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
            catch ( x | true) {
                nil
            }
        """)
        assert(out == "anon : (lin 3, col 21) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun jj_14_catch_data() {
        val out = test("""
            data :X = [x]
            catch ( x:X | x.x==10 ) {
                error([10])
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_15_catch_set() {
        val out = test("""
            var x
            catch ( it | do {
                set x = it
                it[0]==:x
            }) {
                error([:x])
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
                error(t)
                nil
            }
        """)
        assert(out == " |  anon : (lin 4, col 17) : error(t)\n" +
                " v  error : @[]\n") { out }
        //assert(out.contains(" v  anon : (lin 2, col 13) : block escape error : cannot copy reference out\n")) { out }
    }
    @Test
    fun jj_18_throw_err() {
        val out = test("""
            val x = catch (it | true) {
                val t = @[]
                error(t)
                nil
            }
            println(x)
        """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun jj_19_catch() {
        val out = test("""
            val x = catch (_|true) {
                error([10])
            }[0]
            println(x)
        """)
        assert(out == "10\n") { out }
    }

    // CALL STACK

    @Test
    fun kk_01_func_err() {
        val out = test("1(1)")
        assert(out == " |  anon : (lin 1, col 1) : 1(1)\n" +
                " v  call error : expected function\n") { out }
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
                " |  anon : (lin 3, col 17) : 1(1)\n" +
                " v  call error : expected function\n") { out }
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
            catch ( it | true) {
                defer {
                    error(nil)
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
                    error(:err)     ;; ERR
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
                    error(:err)     ;; ERR
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
            error(:error)
        """)
        assert(out == " |  anon : (lin 5, col 13) : error(:error)\n" +
                " v  error : :error\n") { out }
    }
    @Test
    fun BUG_pp_04_throw_defer() {
        val out = test("""
            defer {
                error(:2)
            }
            error(:1)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun BUG_pp_05_throw_defer() {
        val out = test("""
            do {
                defer {
                    error(nil)
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
                catch (it | do {
                    val x = it
                    println(it) ;; [:x]
                    false
                }) {
                    error([:x])
                    println(:no)
                }
            }
            println(:ok)
        """)
        assert(out == "[:x]\n" +
                " |  anon : (lin 8, col 21) : error([:x])\n" +
                " v  error : [:x]\n") { out }
    }
    @Test
    fun zz_02() {
        val out = test("""
            do {
                val y = catch (it | do {
                    val x = it
                    println(it) ;; [:x]
                    x
                }) {
                    error([:x])
                    println(:no)
                }
                println(y)
            }
            println(:ok)
        """)
        assert(out == ("[:x]\n[:x]\n:ok\n"))
    }
    @Test
    fun zz_03_optim() {
        val out = test("""
            catch (it| do {
                println(it)
                false
            }) {
                error([:x])
            }
            println(:ok)
        """)
        assert(out == "[:x]\n" +
                " |  anon : (lin 6, col 17) : error([:x])\n" +
                " v  error : [:x]\n") { out }
    }
    @Test
    fun zz_04_err() {
        val out = test("""
            error()
        """)
        assert(out == " |  anon : (lin 2, col 13) : error()\n" +
                " v  error : nil\n") { out }
    }
    @Test
    fun zz_05_tplate_valgrind() {
        val out = test("""
            val u = [[]]
            println(u[1])
        """)
        //assert(out == "anon : (lin 5, col 25) : index error : field \"X\" is not a data") { out }
        assert(out == " |  anon : (lin 3, col 21) : u[1]\n" +
                " v  index error : out of bounds\n") { out }
    }
}
