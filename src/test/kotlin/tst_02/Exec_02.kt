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
                print(:1)
                escape(:x,nil)
                print(:2)
            }
            print(:3)
        """)
        assert(out == ":1\n:3\n") { out }
    }
    @Test
    fun cc_02_escape() {
        val out = test("""
            val v = enclose' :x {
                escape(:x, 10)
            }
            print(v)
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
                print(:no)
            }
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_04_escape() {
        val out = test("""
            val v = enclose' :x {
                enclose' :y {
                    escape(:y, 10)
                    print(:no)
                }
            }
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_05_escape() {
        val out = test("""
            val v = enclose' :x {
                10
            }
            print(v)
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
        assert(out == "anon : (lin 4, col 21) : escape throw : expected matching enclosing block\n") { out }
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
            print(:ok)
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
            print(:ok)
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
            print(:out)
        """)
        //assert(out == "anon : (lin 4, col 21) : break throw : expected immediate parent loop\n") { out }
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
            print(:out)
        """)
        assert(out == ":out\n") { out }
        //assert(out == "anon : (lin 4, col 21) : break throw : expected immediate parent loop\n") { out }
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
            print(:out)
        """)
        assert(out == ":out\n") { out }
        //assert(out == "anon : (lin 4, col 21) : skip throw : expected immediate parent loop\n") { out }
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
            print(:out)
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
                        print(:in)
                        if true {
                            escape(:break, nil)
                        } else {nil}
                    }
                }
            }
            print(:out)
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
                        print(:in)
                        if false {
                            escape(:skip, nil)
                        } else {nil}
                        if true {
                            escape(:break, nil)
                        } else {nil}
                    }
                }
            }
            print(:out)
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
            print(x)
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
                        print(i)
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
            print(:ok)
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
            print(v)
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
            print(v1, v2)
        """
        )
        assert(out == "10\tnil\n") { out }
    }
    @Test
    fun dd_08_loop() {
        val out = test("""
            val x = 10
            print(enclose' :break {
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
        //assert(out == "anon : (lin 4, col 21) : break throw : expected immediate parent loop\n") { out }
        //assert(out == "anon : (lin 4, col 21) : break throw : expected parent loop\n") { out }
        assert(out == "anon : (lin 5, col 25) : escape throw : expected matching enclosing block\n") { out }
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
            print(:ok)
        """)
        //assert(out == "anon : (lin 5, col 21) : break throw : expected immediate parent loop\n") { out }
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
            print(:ok)
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
                    print(i)
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
                        print(i)
                        if i /= 2 {
                            escape(:skip,nil)
                        } else {nil}
                        print(i)
                        escape(:break,nil)
                    }
                }
            }
            print(i)
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
            print(:ok)
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
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun dd_16_until() {
        val out = test("""
            print(enclose' :break {
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
            print(1)
            defer { print(2) }
            defer { print(3) }
            print(4)
        """)
        assert(out == "1\n4\n3\n2\n") { out }
    }
    @Test
    fun ee_02_defer() {
        val out = test("""
            do {
                print(1)
                defer { print(2) }
                print(3)
            }
            print(4)
            defer { print(5) }
            print(6)
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
            print(f())
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ee_04_defer() {
        val out = test("""
            do {
                defer { print(3) }
                do {
                    defer { print(6) }
                }
            }
            defer { print(12) }
        """)
        assert(out == "6\n3\n12\n") { out }
    }
    @Test
    fun ee_05_defer() {
        val out = test("""
            var f
            set f = func' () {
                print(111)
                defer { print(222) }
                print(333)
            }
            defer { print(1) }
            do {
                print(2)
                defer { print(3) }
                print(4)
                do {
                    print(5)
                    f()
                    defer { print(6) }
                    print(7)
                }
                print(8)
                defer { print(9) }
                print(10)
            }
            print(11)
            defer { print(12) }
            print(13)
        """)
        assert(out == "2\n4\n5\n111\n333\n222\n7\n6\n8\n10\n9\n3\n11\n13\n12\n1\n") { out }
    }
    @Test
    fun ee_06_defer () {
        val out = test("""
            do {
                defer {
                    print(:ok)
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
            print(:ok)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration throw : data :T is not declared\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_00_catch_err() {
        val out = test("""
            val err = catch :x ;;;( it |  set it=nil );;; {
                throw(:x)
            }
            print(err)
        """)
        //assert(out == "anon : (lin 2, col 37) : set throw : destination is immutable\n") { out }
        //assert(out == ":ok\n") { out }
        assert(out == ":x\n") { out }
    }
    @Test
    fun jj_01_catch() {
        val out = test("""
            val err = catch :x ;;;(v| do {
                ;;print(:v,v)
                v == :x
            });;; {
                throw(:x)
                print(9)
            }
            print(err)
        """)
        assert(out == ":x\n") { out }
    }
    @Test
    fun jj_02_catch_err() {
        val out = test("""
            catch :z ;;;(it|it==:x);;; {
                throw(:y)
                print(9)
            }
            print(1)
        """)
        assert(out == " |  anon : (lin 3, col 17) : throw(:y)\n" +
                " v  throw : :y\n") { out }
    }
    @Test
    fun jj_03_catch_err() {
        val out = test("""
            val f = func' () {
                throw(:y)
                print(9)
            }
            catch :x ;;;(it|it==:x);;; {
                f()
                print(9)
            }
            print(1)
        """)
        assert(out == " |  anon : (lin 7, col 17) : f()\n" +
                " |  anon : (lin 3, col 17) : throw(:y)\n" +
                " v  throw : :y\n") { out }
    }
    @Test
    fun jj_04_catch() {
        val out = test("""
            var f
            set f = func' () {
                catch :xxx ;;;(it | it==:xxx);;; {
                    throw(:yyy)
                    print(91)
                }
                print(9)
            }
            catch :yyy ;;;(it | it==:yyy);;; {
                catch :xxx ;;;(it2 | it2==:xxx);;; {
                    f()
                    print(92)
                }
                print(93)
            }
            print(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_05_catch_valgrind() {
        DEBUG = true
        val out = test("""
            catch :x ;;;(it| it==:x);;; {
                throw([])
                print(9)
            }
            print(1)
        """)
        assert(out.contains("ceu_pro_error: Assertion `t.type == CEU_VALUE_TAG' failed.")) { out }
        //assert(out == "anon : (lin 2, col 5) : throw throw : expected tag\n") { out }
        //assert(out == " |  anon : (lin 3, col 17) : throw([])\n" +
        //        " v  throw : []\n") { out }
    }
    @Test
    fun jj_06_catch() {
        val out = test("""
            catch ( :e1 ;;;it|it==:e1;;;) {
                catch :e2 ;;;( it|it==:e2);;; {
                    catch :e3 ;;;( it|it==:e3);;; {
                        catch :e4 ;;;( it | it==:e4 );;; {
                            print(1)
                            throw(:e3)
                            print(99)
                        }
                        print(99)
                    }
                    print(2)
                    throw(:e1)
                    print(99)
                }
                print(99)
            }
            print(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun jj_07_catch_err() {
        val out = test("""
            catch ;;;( it | true );;; {
                throw(:y)
                print(9)
            }
            print(1)
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 1) : catch throw : expected tag\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_08_catch() {
        val out = test(
            """
            catch :x ;;;( it | it==do {
                :x
            } );;; {
                throw(:x)
                print(9)
            }
            print(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_09_catch() {
        val out = test(
            """
            catch :z ;;;( it | false);;; {
                throw(:xxx)
                print(9)
            }
            print(1)
        """.trimIndent()
        )
        assert(out == " |  anon : (lin 2, col 5) : throw(:xxx)\n" +
                " v  throw : :xxx\n") { out }
    }
    @Test
    fun jj_10_catch() {
        val out = test("""
            catch :z ;;;(it | false);;; {
                throw(:x)
                ;;throw([])
                print(9)
            }
            print(1)
        """)
        assert(out == " |  anon : (lin 3, col 17) : throw(:x)\n" +
                " v  throw : :x\n") { out }
        //assert(out == " |  anon : (lin 3, col 17) : throw([])\n" +
        //        " v  throw : []\n") { out }
    }
    @Test
    fun jj_11_catch() {
        val out = test("""
            catch :z ;;;( it | it==[]);;; {
                val xxx = :x ;;[]
                throw(xxx)
            }
            print(1)
        """)
        //assert(out == " v  anon : (lin 2, col 35) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 4, col 17) : throw(xxx)\n" +
        //        " v  throw : []\n") { out }
        assert(out == " |  anon : (lin 4, col 17) : throw(xxx)\n" +
                " v  throw : :x\n") { out }
    }
    @Test
    fun jj_12_catch() {
        val out = test("""
            val t = catch ;;;( it|true);;; {
                val xxx = []
                throw(:x, ;;;drop;;;(xxx))
            }
            print(t)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun BUG_jj_13_throw_catch_condition() {
        val out = test("""
            catch ;;;( it | throw(2));;; {
                throw(:x,1)
            }
            print(:ok)
        """)
        //assert(out.contains("main: Assertion `ceu_acc.type!=CEU_VALUE_THROW && \"TODO: throw in catch condition\"' failed.")) { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_14_blocks() {
        val out = test("""
            val v = catch ;;;(it | true);;; {
                do {
                    throw(:x)
                }
                print(9)
            }
            print(v)
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
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 3, col 21) : declaration throw : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun jj_14_catch_data() {
        val out = test("""
            data :X = [x]
            catch :x ;;;( x:X | x.x==10 );;; {
                throw(:x, [10])
            }
            print(:ok)
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
                throw(:x, [:x])
                print(9)
            }
            print(x)
        """)
        //assert(out == "[:x]\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun jj_17_throw() {
        val out = test("""
            do {
                val t = @[]
                throw(:x,t)
                nil
            }
        """)
        assert(out == " |  anon : (lin 4, col 17) : throw(:x,t)\n" +
                " v  throw : @[]\n") { out }
        //assert(out.contains(" v  anon : (lin 2, col 13) : block escape throw : cannot copy reference out\n")) { out }
    }
    @Test
    fun jj_18_throw_err() {
        val out = test("""
            val x = catch ;;;(it | true);;; {
                val t = @[]
                throw(:x,t)
                nil
            }
            print(x)
        """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun jj_19_catch() {
        val out = test("""
            val x = catch ;;;(_|true);;; {
                throw(:x,[10])
            }[0]
            print(x)
        """)
        assert(out == "10\n") { out }
    }

    // CALL STACK

    @Test
    fun kk_01_func_err() {
        val out = test("1(1)")
        assert(out == " |  anon : (lin 1, col 1) : 1(1)\n" +
                " v  throw : expected function\n") { out }
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
                " v  throw : expected function\n") { out }
    }
    @Test
    fun kk_03_func_args() {
        val out = test(
            """
            val f = func' (x) {
                print(x)
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
                        throw(:X)
                    }) ()
                }
                throw(:Y)
            } ()
        """
        )
        assert(out == " |  anon : (lin 2, col 13) : (func' () { catch { (func' () { throw(:X);...\n" +
                " |  anon : (lin 8, col 17) : throw(:Y)\n" +
                " v  throw : :Y\n") { out }
    }
    @Test
    fun kk_05_index() {
        val out = test("""
            val str = #[0]
            throw(:X, str)
            print(str)
        """)
        assert(out == " |  anon : (lin 3, col 13) : throw(:X,str)\n" +
                " v  throw : #[0]\n") { out }
    }

    // THROW/CATCH / DEFER

    @Test
    fun BUG_pp_01_throw_defer() {
        val out = test("""
            catch ( it | true) {
                defer {
                    throw(nil)
                }
            }
            print(:ok)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun BUG_ppx_02_defer_err() {
        val out = test("""
            val v = do {
                defer {
                    print(:2)
                }
                defer {
                    print(:1)
                    throw(:err)     ;; ERR
                }
            }
            print(:3)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun BUG_pp_02_defer_err() {
        val out = test("""
            val v = do {
                defer {
                    print(:1)
                }
                defer {
                    print(:2)
                    throw(:err)     ;; ERR
                    print(:3)
                }
                defer {
                    print(:4)
                }
            }
            print(:ok, v)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun pp_03_throw_defer() {
        val out = test("""
            defer {
                nil
            }
            throw(:x)
        """)
        assert(out == " |  anon : (lin 5, col 13) : throw(:x)\n" +
                " v  throw : :x\n") { out }
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
            print(:ok)
        """)
        assert(out.contains("main: Assertion `ceu_acc.type != CEU_VALUE_THROW && \"TODO: throw in defer\"' failed.")) { out }
    }
    @Test
    fun pp_06_throw_defer_print() {
        val out = test("""
            defer {
                print(:ok)
            }
            throw(:x)
        """)
        assert(out == ":ok\n" +
                " |  anon : (lin 5, col 13) : throw(:x)\n" +
                " v  throw : :x\n") { out }
    }
    @Test
    fun pp_07_error_error() {
        val out = test("""
            throw(:throw)
        """)
        assert(out == " |  anon : (lin 2, col 13) : throw(:throw)\n" +
                " v  throw : :throw\n") { out }
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
                    print(k, t[k])
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
                print(`:number CEU_GC.free`)
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
            print(sum(5))                                                                
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
                    print(it) ;; [:x]
                    false
                });;; {
                    throw(:x,[:x])
                    print(:no)
                }
            }
            print(:ok)
        """)
        assert(out == //"[:x]\n" +
                " |  anon : (lin 8, col 21) : throw(:x,[:x])\n" +
                " v  throw : [:x]\n") { out }
    }
    @Test
    fun zz_02() {
        val out = test("""
            do {
                val y = catch ;;;(it | do {
                    val x = it
                    print(it) ;; [:x]
                    x
                });;; {
                    throw(:x,[:x])
                    print(:no)
                }
                print(y)
            }
            print(:ok)
        """)
        //assert(out == ("[:x]\n[:x]\n:ok\n")) { out }
        assert(out == ("[:x]\n:ok\n")) { out }
    }
    @Test
    fun zz_03_optim() {
        val out = test("""
            catch :y ;;;(it| do {
                print(it)
                false
            });;; {
                throw(:x,[:x])
            }
            print(:ok)
        """)
        assert(out == //"[:x]\n" +
                " |  anon : (lin 6, col 17) : throw(:x,[:x])\n" +
                " v  throw : [:x]\n") { out }
    }
    @Test
    fun zz_04_err() {
        val out = test("""
            throw()
        """)
        assert(out == " |  anon : (lin 2, col 13) : throw()\n" +
                " v  throw : nil\n") { out }
    }
    @Test
    fun zz_05_err() {
        val out = test("""
            val v = catch :y {
                throw(nil, :ok)
            }
            print(v)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_05_tplate_valgrind() {
        val out = test("""
            val u = [[]]
            print(u[1])
        """)
        //assert(out == "anon : (lin 5, col 25) : index throw : field \"X\" is not a data") { out }
        assert(out == " |  anon : (lin 3, col 21) : u[1]\n" +
                " v  throw : out of bounds\n") { out }
    }
}
