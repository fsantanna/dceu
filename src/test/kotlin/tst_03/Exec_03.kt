package tst_03

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_03 {
    // COPY

    // COPY VECTOR

    @Test
    fun aa_01_copy_tuple() {
        val out = test("""
            val t1 = [1,2,3]
            val t2 = copy(t1)
            val t3 = t1
            set t1[2] = 999
            set t2[0] = 10
            println(t1)
            println(t2)
            println(t3)
        """)
        assert(out == "[1,2,999]\n[10,2,3]\n[1,2,999]\n") { out }
    }
    @Test
    fun aa_02_copy_tuple_rec() {
        val out = test("""
            var f
            set f = func (v) {
                ;;println(v)
                if v > 0 {
                    copy([f(v - 1)])
                } else {
                    0
                }
            }
            println(f(3))
        """, true)
        assert(out == "[[[0]]]\n") { out }
    }
    @Test
    fun aa_03_copy_tuple_out() {
        val out = test("""
            val out = do {
                val ins = [1,2,3]
                copy(ins)
            }
            println(out)
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun aa_04_copy_tuple_scope() {
        val out = test("""
            var x = [1,2,3]
            do {
                val y = copy(x)
                do {
                    set x = y
                }
            }
            println(x)
        """)
        assert(out == "anon : (lin 6, col 25) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun aa_05_copy_tuple_scope() {
        val out = test("""
            var x = [1,2,3]
            do {
                val y = copy(x)
                do {
                    set x = copy(y)
                }
            }
            println(x)
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun aa_06_copy_tuple_scope() {
        val out = test("""
            var v
            do {
                var x = [1,2,3]
                do {
                    val y = copy(x)
                    do {
                        set x = copy(y)
                        ;;`printf(">>> %d\n", ceu_mem->x.Dyn->tphold);`
                        set v = x       ;; err
                    }
                }
            }
            println(v)
        """)
        assert(out == "anon : (lin 10, col 29) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }

    // COPY DICT

    @Test
    fun ab_01_copy_dict() {
        val out = test("""
            val t1 = @[]
            set t1[:x] = 1
            val t2 = t1
            val t3 = copy(t1)
            set t1[:y] = 2
            set t3[:y] = 20
            println(t1)
            println(t2)
            println(t3)
        """)
        assert(out == "@[(:x,1),(:y,2)]\n@[(:x,1),(:y,2)]\n@[(:x,1),(:y,20)]\n") { out }
    }
    @Test
    fun ab_02_copy_dict() {
        val out = test("""
            var x
            set x = @[(nil,10)]
            println(x[nil])
        """)
        assert(out.contains("ceu_dict_set: Assertion `key->type != CEU_VALUE_NIL' failed")) { out }
    }
    @Test
    fun ab_03_copy_dict() {
        val out = test("""
            val x
            set x = @[]
            set x[nil] = 10
            println(x[nil])
        """)
        assert(out.contains("ceu_dict_set: Assertion `key->type != CEU_VALUE_NIL' failed")) { out }
    }

    // COPY VECTOR

    @Test
    fun ac_01_copy_vector() {
        val out = test("""
            val v = #[1,2,3]
            val x = export [] {
                val i = v[#v - 1]
                set v[#v - 1] = nil
                i
            }
            println(x, #v)
        """, true)
        assert(out == "3\t2\n") { out }
    }
    @Test
    fun ac_02_copy_vector() {
        val out = test("""
            val t1 = #[]
            set t1[#t1] = 1
            println(t1)
        """)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun ac_03_copy_vector() {
        val out = test("""
            val t1 = #[]        ;; [1,2]
            set t1[#t1] = 1
            val t2 = t1         ;; [1,2]
            val t3 = copy(t1)   ;; [1,20]
            set t1[#t1] = 2
            set t3[#t3] = 20
            println(t1)
            println(t2)
            println(t3)
        """)
        assert(out == "#[1,2]\n#[1,2]\n#[1,20]\n") { out }
    }

    // COPY CLOSURE
    //@Ignore
    @Test
    fun todo_ad_01_copy_clos() {
        val out = test("""
            var f = func (^a) {
                func () {
                    ^^a
                }
            }
            var g = do {
                var t = [1]
                var i = copy(f(t))
                set t[0] = 10
                drop(i)
            }
            println(g())
        """)
        assert(out == "[1]\n") { out }
    }

    // EXPORT

    @Test
    fun bb_01_export() {
        val out = test("""
            export [aaa] {
                val aaa = 10
            }
            export [xxx] {
                var xxx
                set xxx = aaa
            }
            print(xxx)
        """)
        assert(out == "10") { out }
    }
    @Test
    fun bb_02_export() {
        val out = test("""
            export [] {
                var a       ;; invisible
                set a = 10
            }
            var x
            set x = a
            print(x)
        """)
        assert(out == "anon : (lin 7, col 21) : access error : variable \"a\" is not declared") { out }
    }
    @Test
    fun bb_03_export() {
        val out = test("""
            val x = export [] {
                val a = []
                a
            }
            print(x)
        """)
        assert(out == "[]") { out }
    }
    @Test
    fun bb_04_export() {
        val out = test("""
            export [aaa] {
                val aaa = 10
            }
            export [bbb] {
                val bbb = 20
            }
            println(aaa,bbb)
        """)
        assert(out == "10\t20\n") { out }
    }
    @Test
    fun bb_05_export() {
        val out = test("""
            export [f] {
                val v = []
                val f = func () {
                    v
                }
                ;;println(v, f)
            }
            do {
                val x = f
                nil
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun bb_06_export() {
        val out = test("""
            do {
                export [f] {
                    val v = []
                    val f = func () {
                        v
                    }
                    ;;println(v, f)
                }
                do {
                    val x = f
                    nil
                }
                println(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }

    // SCOPE

    @Test
    fun cc_01_scope_tasks() {
        val out = test("""
            do {
                val t = [tasks(), tasks()]
                println(#t)
            }
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun cc_02_scope_tracks() {
        val out = test("""
            val T = task () { yield(nil) }
            do {
                val ts = tasks()
                spawn in ts, T()
                do {
                    val vec = #[]
                    loop t in :tasks ts {
                        set vec[#vec] = t
                    }
                    println(vec)
                }
            }
        """)
        //assert(out == "anon : (lin 9, col 29) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out.contains("#[x-track: 0x")) { out }
    }
    @Test
    fun cc_03_scope_tracks() {
        val out = test("""
            val T = task () { yield(nil) }
            do {
                val ts = tasks()
                spawn in ts, T()
                do {
                    val vec = #[]
                    loop t in :tasks ts {
                        set vec[#vec] = drop(t)
                    }
                    println(#vec)
                }
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_04_scope_task() {
        val out = test("""
            val T = task (t1) {
                val t2 = []
                pass [t1,[],t2]
                yield(nil)
            }
            do {
                spawn T([])
                nil
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // THROW / CATCH

    @Test
    fun dd_01_catch() {
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
    fun dd_02_catch_err() {
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
    fun dd_03_catch() {
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
    fun dd_04_catch_valgrind() {
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
    fun dd_05_catch() {
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
    fun dd_06_catch_err() {
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
    fun dd_07_catch() {
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
    fun dd_08_catch() {
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
    fun dd_09_catch_err() {
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
    fun dd_10_catch() {
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
    fun dd_11_catch_err() {
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
    fun dd_12_catch() {
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
    fun dd_13_catch() {
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
    fun dd_14_catch() {
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
    fun dd_15_catch() {
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

    // DEFER

    @Test
    fun ee_01_defer() {
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
    fun todo_ee_01_defer2_err() {
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
    fun ee_03_defer() {
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
    @Test
    fun ee_04_defer_err() {
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

    // GC / BCAST

    @Test
    fun ff_01_gc_bcast() {
        val out = test("""
            broadcast in :global, []
            println(`:number ceu_gc_count`)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ff_02_gc_bcast() {
        val out = test(
            """
            var tk = task () {
                yield(nil)
                do {
                    val xxx = evt
                }
                nil
                ;;println(:out)
            }
            var co = spawn tk ()
            broadcast in :global, []
            println(`:number ceu_gc_count`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 11, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun ff_03_gc_bcast_err() {
        val out = test(
            """
            var tk = task () {
                do {
                    yield(nil)
                    var v = evt
                }
                nil
                ;;println(:out)
            }
            var co = spawn tk ()
            broadcast in :global, []
            println(`:number ceu_gc_count`)
        """
        )
        //assert(out == "1\n") { out }
        assert(out == "anon : (lin 11, col 13) : broadcast in :global, []\n" +
                "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun ff_04_gc_bcast_err() {
        val out = test(
            """
            var tk = task () {
                do {
                    yield(nil)
                    do {
                        var v = evt
                    }
                }
                nil
                ;;println(:out)
            }
            var co = spawn tk ()
            broadcast in :global, []
            println(`:number ceu_gc_count`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 11, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun ff_05_gc_bcast_arg() {
        val out = test(
            """
            var tk = task (v) {
                do {
                    yield(nil)
                    ;;println(evt)
                    do {
                        val v' = evt
                    }
                }
                nil
                ;;println(:out)
            }
            var co = spawn (tk) ()
            broadcast in :global, []
            println(`:number ceu_gc_count`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 12, col 13) : broadcast in :global, []\n" +
        //        "anon : (lin 6, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }

    // UNDERSCORE

    @Test
    fun gg_01_und() {
        val out = test(
            """
            val _ = 10
            println(_)
        """
        )
        assert(out == "anon : (lin 3, col 21) : access error : cannot access \"_\"") { out }
    }
    @Test
    fun gg_02_und() {
        val out = test(
            """
            do {
                val _ = println(10)
            }
            do {
                val _ = println(20)
            }
            println(:ok)
        """
        )
        assert(out == "10\n20\n:ok\n") { out }
    }
    @Test
    fun gg_03_und() {
        val out = test("""
            val _ = 10
            val _ = 10
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // XXX

    @Test
    fun todo_use_bef_dcl_func18() {
        val out = test("""
            var f
            set f = func () {
                println(v)
            }
            var v
            set v = 10
            f()
        """)
        assert(out == "10\n") { out }
    }

    @Test
    fun todo_op_id2() {
        val out = test("""
            set (+) = (-)
            println((+)(10,4))
        """)
        assert(out == "6\n") { out }
    }
    @Test
    fun tags14() {
        val out = test("""
            val co = coro () {
                yield(:x)
            }
            println(:y)
        """)
        assert(out == ":y\n") { out }
    }
    @Test
    fun tags15() {
        val out = test("""
            val t = tags([], :x, true)
            val s = copy(t)
            println(s)
        """)
        assert(out == ":x []\n") { out }
    }
    @Test
    fun all_01() {
        val out = test("""
            val T = (task () {
                set task.pub = []
                yield(nil)
            })
            val f = (func (v) {
                nil
            })
            do {
                val xxx = spawn T()
                do {
                    val zzz = xxx
                    nil
                }
                f(xxx.pub)
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // NOT, AND, OR

    // OPS: not, and, or

    @Test
    fun op_or_and() {
        val out = test("""
            println(true or println(1))
            println(false and println(1))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun op_not() {
        val out = test("""
            println(not nil and not false)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun op2_or_and() {
        val out = test("""
            println(1 or error(5))
            println(1 and 2)
            println(nil and 2)
            println(nil or 2)
        """)
        assert(out == "1\n2\nnil\n2\n") { out }
    }
    @Test
    fun op3_or_and() {
        val out = test("""
            println(true and ([] or []))
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun xop3_or_and() {
        val out = test("""
            val v = do {
                val :tmp x = []
                if x { x } else { [] }
            }
            println(v)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun op4_and_and() {
        val out = test("""
            val v = true and
                true and 10
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun op5_plus_plus() {
        val out = test("""
            val v = 5 +
                5 + 10
            println(v)
        """, true)
        assert(out == "20\n") { out }
    }
}
