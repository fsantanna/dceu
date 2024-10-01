package tst_02

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_02 {

    // DO / ESCAPE

    @Test
    fun cc_01_escape() {
        val out = test("""
            enclose' :x {
                println(:1)
                escape(:x,nil)
                println(:2)
            }
            println(:3)
        """)
        assert(out == ":1\n:3\n") { out }
    }
    @Test
    fun cc_02_escape() {
        val out = test("""
            val v = enclose' :x {
                escape(:x, 10)
            }
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_03_escape() {
        val out = test("""
            val v = enclose' :x {
                enclose' :y {
                    escape(:x, 10)
                }
                println(:no)
            }
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_04_escape() {
        val out = test("""
            val v = enclose' :x {
                enclose' :y {
                    escape(:y, 10)
                    println(:no)
                }
            }
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_05_escape() {
        val out = test("""
            val v = enclose' :x {
                10
            }
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_06_escape_err() {
        val out = test("""
            enclose' :X {
                func' () {
                    escape(:X,nil)
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : escape error : expected matching enclosing block\n") { out }
    }

    // LOOP

    @Test
    fun dd_01_loop() {
        val out = test("""
            enclose' :break {
                loop' {
                    if true {
                        escape(:break,nil)
                    } else { nil }
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_00_loop() {
        val out = test("""
            enclose' :break {
                loop' {
                    if true {
                        escape(:break, nil)
                    } else { nil }
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_01_loop_no_err() {
        val out = test("""
            enclose' :break {
                loop' {
                    do {
                        escape(:break, nil)   ;; should not be allowed
                    }   ;; currently allowed ;;bc of late decls that nest blocks transparently
                }
            }
            println(:out)
        """)
        //assert(out == "anon : (lin 4, col 21) : break error : expected immediate parent loop\n") { out }
        assert(out == ":out\n") { out }
    }
    @Test
    fun dd_01x_loop_err_no() {
        val out = test("""
            enclose' :break {
                loop' {
                    do { ;;do(nil)
                        escape(:break, nil) ;; if true
                    }
                }
            }
            println(:out)
        """)
        assert(out == ":out\n") { out }
        //assert(out == "anon : (lin 4, col 21) : break error : expected immediate parent loop\n") { out }
    }
    @Test
    fun dd_01y_loop_err() {
        val out = test("""
            enclose' :break {
                loop' {
                    escape(:break, nil)
                    enclose' :skip {
                        escape(:skip, nil)
                    }
                }
            }
            println(:out)
        """)
        assert(out == ":out\n") { out }
        //assert(out == "anon : (lin 4, col 21) : skip error : expected immediate parent loop\n") { out }
    }
    @Test
    fun dd_01z_loop_err() {
        val out = test("""
            var ok = false
            enclose' :break {
                loop' {
                    enclose' :skip {
                        if ok {
                            escape(:break, nil)
                        } else { nil }
                        set ok = true
                        ;;;do;;; []
                        escape(:skip, nil) ;;if true
                    }
                }
            }
            println(:out)
        """)
        assert(out == ":out\n") { out }
    }
    @Test
    fun dd_02_loop() {
        val out = test(
            """
            do {
                enclose' :break {
                    loop' {
                        println(:in)
                        if true {
                            escape(:break, nil)
                        } else {nil}
                    }
                }
            }
            println(:out)
        """
        )
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun dd_02x_loop() {
        val out = test(
            """
            enclose' :break {
                loop' {
                    enclose' :skip {
                        println(:in)
                        if false {
                            escape(:skip, nil)
                        } else {nil}
                        if true {
                            escape(:break, nil)
                        } else {nil}
                    }
                }
            }
            println(:out)
        """
        )
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun dd_03_loop() {
        val out = test(
            """
            var x
            set x = false
            enclose' :break {
                loop' {
                    if x {
                        escape(:break, nil)
                    } else {nil}
                    set x = true
                }
            }
            println(x)
        """
        )
        assert(out == "true\n") { out }
    }
    @Test
    fun dd_04_loop() {
        val out = test(
            """
            val f = func' (t) {
                if t[1] == 5 {
                    nil
                } else {
                    set t[1] = t[1] + 1
                    t[1]
                }
            }
            do {
                val it = [f, 0]
                var i = it[0](it)
                enclose' :break {
                    loop' {
                        if (i == nil) {
                            escape(:break, nil)
                        } else {nil}
                        println(i)
                        set i = it[0](it)
                    }
                }
            }
        """, true
        )
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun dd_05_loop() {
        val out = test(
            """
            val f = func' (t) {
                nil
            }
            val v = []
            f(v)
            println(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_06_loop() {
        val out = test(
            """
            val v = enclose' :break {
                loop' {
                    if (10) {
                        escape(:break, 10)
                    } else {nil}
                }
            }
            println(v)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun dd_07_loop() {
        val out = test(
            """
            val v1 = enclose' :break {
                loop' {
                    if (10) {
                        escape(:break, 10)
                    } else {nil}
                }
            }
            val v2 = enclose' :break {
                loop' {
                    if true {
                        escape(:break, nil)
                    } else {nil}
                }
            }
            println(v1, v2)
        """
        )
        assert(out == "10\tnil\n") { out }
    }
    @Test
    fun dd_08_loop() {
        val out = test("""
            val x = 10
            println(enclose' :break {
                loop' {
                    if (x) {
                        escape(:break,x)
                    } else {nil}
                }
            })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun dd_09_loop_break() {
        val out = test("""
            enclose' :break {
                loop' {
                    func' () {
                        escape(:break,nil)
                    }
                }
            }
        """)
        //assert(out == "anon : (lin 4, col 21) : break error : expected immediate parent loop\n") { out }
        //assert(out == "anon : (lin 4, col 21) : break error : expected parent loop\n") { out }
        assert(out == "anon : (lin 5, col 25) : escape error : expected matching enclosing block\n") { out }
    }
    @Test
    fun dd_10_loop() {
        val out = test("""
            enclose' :break {
                loop' {
                    do {
                        val t = []
                        escape(:break, nil)
                    }
                }
            }
            println(:ok)
        """)
        //assert(out == "anon : (lin 5, col 21) : break error : expected immediate parent loop\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_11_loop() {
        val out = test("""
            enclose' :break {
                loop' {
                    val t = []
                    if true {
                        escape(:break,nil)
                    } else {nil}
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_12_iter() {
        val out = test("""
            $PLUS
            val f = func' (t) {
                if t[1] == 5 {
                    nil
                } else {
                    set t[1] = t[1] + 1
                    t[1]
                }
            }
            enclose' :break {
                val it = [f, 0]
                var i = it[0](it)
                loop' {
                    if i == nil {
                        escape(:break,nil)
                    } else {nil}
                    println(i)
                    set i = it[0](it)
                }
            }
        """)
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun dd_13_iter() {
        val out = test("""
            $PLUS
            var i = 0
            enclose' :break {
                loop' {
                    enclose' :skip {
                        set i = i + 1
                        println(i)
                        if i /= 2 {
                            escape(:skip,nil)
                        } else {nil}
                        println(i)
                        escape(:break,nil)
                    }
                }
            }
            println(i)
        """)
        assert(out == "1\n2\n2\n2\n") { out }
    }
    @Test
    fun dd_14_loop_break_error_bug() {
        val out = test("""
            enclose' :break {
                loop' {
                    do { nil }
                    if true {
                        escape(:break,nil)
                    } else {nil}
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_15_loop_break_immed() {
        val out = test("""
            enclose' :break {
                loop' {
                    do { nil }
                    val e = nil
                    if true {
                        escape(:break,nil)
                    } else {nil}
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_16_until() {
        val out = test("""
            println(enclose' :break {
                loop' {
                    if 10 {
                        escape(:break,10)
                    } else {nil}
                }
            })
        """)
        assert(out == "10\n") { out }
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
        assert(out == "1\n3\n2\n4\n6\n5\n") { out }
    }
    @Test
    fun ee_03_defer() {
        val out = test("""
            val f = func' () {
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
            set f = func' () {
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
    fun ee_06_defer () {
        val out = test("""
            do {
                defer {
                    println(:ok)
                };
                do {
                    nil
                }
            }
        """)
        assert(out == ":ok\n") { out }
    }

    // THROW / CATCH

    @Test
    fun jj_00_0_err() {
        val out = test("""
            catch :x ;;;(it :T| it[0]);;; {
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
            val err = catch :x ;;;( it |  set it=nil );;; {
                error(:x)
            }
            println(err)
        """)
        //assert(out == "anon : (lin 2, col 37) : set error : destination is immutable\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == ":x\n") { out }
    }
    @Test
    fun jj_01_catch() {
        val out = test("""
            val err = catch :x ;;;(v| do {
                ;;println(:v,v)
                v == :x
            });;; {
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
            catch :z ;;;(it|it==:x);;; {
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
            val f = func' () {
                error(:y)
                println(9)
            }
            catch :x ;;;(it|it==:x);;; {
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
            set f = func' () {
                catch :xxx ;;;(it | it==:xxx);;; {
                    error(:yyy)
                    println(91)
                }
                println(9)
            }
            catch :yyy ;;;(it | it==:yyy);;; {
                catch :xxx ;;;(it2 | it2==:xxx);;; {
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
            catch :x ;;;(it| it==:x);;; {
                error([])
                println(9)
            }
            println(1)
        """)
        assert(out.contains("ceu_pro_error: Assertion `t.type == CEU_VALUE_TAG' failed.")) { out }
        //assert(out == "anon : (lin 2, col 5) : throw error : expected tag\n") { out }
        //assert(out == " |  anon : (lin 3, col 17) : error([])\n" +
        //        " v  error : []\n") { out }
    }
    @Test
    fun jj_06_catch() {
        val out = test("""
            catch ( :e1 ;;;it|it==:e1;;;) {
                catch :e2 ;;;( it|it==:e2);;; {
                    catch :e3 ;;;( it|it==:e3);;; {
                        catch :e4 ;;;( it | it==:e4 );;; {
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
            catch ;;;( it | true );;; {
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
            catch :x ;;;( it | it==do {
                :x
            } );;; {
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
            catch :z ;;;( it | false);;; {
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
            catch :z ;;;(it | false);;; {
                error(:x)
                ;;error([])
                println(9)
            }
            println(1)
        """)
        assert(out == " |  anon : (lin 3, col 17) : error(:x)\n" +
                " v  error : :x\n") { out }
        //assert(out == " |  anon : (lin 3, col 17) : error([])\n" +
        //        " v  error : []\n") { out }
    }
    @Test
    fun jj_11_catch() {
        val out = test("""
            catch :z ;;;( it | it==[]);;; {
                val xxx = :x ;;[]
                error(xxx)
            }
            println(1)
        """)
        //assert(out == " v  anon : (lin 2, col 35) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 4, col 17) : error(xxx)\n" +
        //        " v  error : []\n") { out }
        assert(out == " |  anon : (lin 4, col 17) : error(xxx)\n" +
                " v  error : :x\n") { out }
    }
    @Test
    fun jj_12_catch() {
        val out = test("""
            val t = catch ;;;( it|true);;; {
                val xxx = []
                error(:x, ;;;drop;;;(xxx))
            }
            println(t)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun BUG_jj_13_throw_catch_condition() {
        val out = test("""
            catch ;;;( it | error(2));;; {
                error(:x,1)
            }
            println(:ok)
        """)
        //assert(out.contains("main: Assertion `ceu_acc.type!=CEU_VALUE_THROW && \"TODO: throw in catch condition\"' failed.")) { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_14_blocks() {
        val out = test("""
            val v = catch ;;;(it | true);;; {
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
            catch ;;;( x | true);;; {
                nil
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 3, col 21) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun jj_14_catch_data() {
        val out = test("""
            data :X = [x]
            catch :x ;;;( x:X | x.x==10 );;; {
                error(:x, [10])
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_15_catch_set() {
        val out = test("""
            var x
            catch ;;;( it | do {
                set x = it
                it[0]==:x
            });;; {
                error(:x, [:x])
                println(9)
            }
            println(x)
        """)
        //assert(out == "[:x]\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun jj_17_throw() {
        val out = test("""
            do {
                val t = @[]
                error(:x,t)
                nil
            }
        """)
        assert(out == " |  anon : (lin 4, col 17) : error(:x,t)\n" +
                " v  error : @[]\n") { out }
        //assert(out.contains(" v  anon : (lin 2, col 13) : block escape error : cannot copy reference out\n")) { out }
    }
    @Test
    fun jj_18_throw_err() {
        val out = test("""
            val x = catch ;;;(it | true);;; {
                val t = @[]
                error(:x,t)
                nil
            }
            println(x)
        """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun jj_19_catch() {
        val out = test("""
            val x = catch ;;;(_|true);;; {
                error(:x,[10])
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
                " v  error : expected function\n") { out }
    }
    @Test
    fun kk_02_func_err() {
        val out = test("""
            val f = func' () {
                1(1)
            }
            f()
        """)
        assert(out == " |  anon : (lin 5, col 13) : f()\n" +
                " |  anon : (lin 3, col 17) : 1(1)\n" +
                " v  error : expected function\n") { out }
    }
    @Test
    fun kk_03_func_args() {
        val out = test(
            """
            val f = func' (x) {
                println(x)
            }
            f(10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_04_double_throw() {
        val out = test(
            """
            func' () {
                catch {
                    (func' () {
                        error(:X)
                    }) ()
                }
                error(:Y)
            } ()
        """
        )
        assert(out == " |  anon : (lin 2, col 13) : (func' () { catch { (func' () { error(:X);...\n" +
                " |  anon : (lin 8, col 17) : error(:Y)\n" +
                " v  error : :Y\n") { out }
    }
    @Test
    fun kk_05_index() {
        val out = test("""
            val str = #[0]
            error(:X, str)
            println(str)
        """)
        assert(out == " |  anon : (lin 3, col 13) : error(:X,str)\n" +
                " v  error : #[0]\n") { out }
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
            error(:x)
        """)
        assert(out == " |  anon : (lin 5, col 13) : error(:x)\n" +
                " v  error : :x\n") { out }
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
    @Test
    fun pp_06_throw_defer_print() {
        val out = test("""
            defer {
                println(:ok)
            }
            error(:x)
        """)
        assert(out == ":ok\n" +
                " |  anon : (lin 5, col 13) : error(:x)\n" +
                " v  error : :x\n") { out }
    }
    @Test
    fun pp_07_error_error() {
        val out = test("""
            error(:error)
        """)
        assert(out == " |  anon : (lin 2, col 13) : error(:error)\n" +
                " v  error : :error\n") { out }
    }

    // LOOPS

    @Test
    fun de_03_next() {
        val out = test(
            """
            val t = @[]
            set t[:x] = 1
            set t[:y] = 2
            set t[:z] = 3
            set t[:y] = nil
            set t[:x] = nil
            set t[:a] = 10
            set t[:b] = 20
            set t[:c] = 30
            var k = next-dict(t)
            enclose' :break {
                loop' {
                    if (k == nil) {
                        escape(:break,nil)
                    } else { nil }
                    println(k, t[k])
                    set k = next-dict(t,k)
                }
            }
        """
        )
        assert(out == ":a\t10\n:b\t20\n:z\t3\n:c\t30\n") { out }
    }
    @Test
    fun gc_19_pool() {
        DEBUG = true
        val out = test("""
            do {
                var t1 = []
                var ok = false
                enclose' :break {
                    loop' {
                        val t2 = t1
                        set t1 = nil
                        if ok {
                            escape(:break,nil)
                        } else { nil }
                        set ok = true
                    }
                }
                println(`:number CEU_GC.free`)
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun zz_01_sum() {
        val out = test("""
            var sum = func' (n) {                                                            
                var i = n                                                                   
                var s = 0
                enclose' :break {
                    loop' {                                                                      
                        if i == 0 {
                            escape(:break,s)
                        } else {nil}
                        set s = s + i                                                           
                        set i = i - 1                                                           
                    }
                }
            }                                                                               
            println(sum(5))                                                                
        """, true)
        assert(out == "15\n") { out }
    }

    // ORIGINAL

    @Test
    fun zz_01() {
        val out = test("""
            do {
                catch :z ;;;(it | do {
                    val x = it
                    println(it) ;; [:x]
                    false
                });;; {
                    error(:x,[:x])
                    println(:no)
                }
            }
            println(:ok)
        """)
        assert(out == //"[:x]\n" +
                " |  anon : (lin 8, col 21) : error(:x,[:x])\n" +
                " v  error : [:x]\n") { out }
    }
    @Test
    fun zz_02() {
        val out = test("""
            do {
                val y = catch ;;;(it | do {
                    val x = it
                    println(it) ;; [:x]
                    x
                });;; {
                    error(:x,[:x])
                    println(:no)
                }
                println(y)
            }
            println(:ok)
        """)
        //assert(out == ("[:x]\n[:x]\n:ok\n")) { out }
        assert(out == ("[:x]\n:ok\n")) { out }
    }
    @Test
    fun zz_03_optim() {
        val out = test("""
            catch :y ;;;(it| do {
                println(it)
                false
            });;; {
                error(:x,[:x])
            }
            println(:ok)
        """)
        assert(out == //"[:x]\n" +
                " |  anon : (lin 6, col 17) : error(:x,[:x])\n" +
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
    fun zz_05_err() {
        val out = test("""
            val v = catch :y {
                error(nil, :ok)
            }
            println(v)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_05_tplate_valgrind() {
        val out = test("""
            val u = [[]]
            println(u[1])
        """)
        //assert(out == "anon : (lin 5, col 25) : index error : field \"X\" is not a data") { out }
        assert(out == " |  anon : (lin 3, col 21) : u[1]\n" +
                " v  error : out of bounds\n") { out }
    }
}
