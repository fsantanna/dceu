package ceu

import D
import Lexer
import Parser
import Expr
import Coder
import Pos
import Ups
import XCEU
import exec
import main
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters
import tostr
import java.io.File

// search in tests output for
//  definitely|Invalid read|Invalid write|uninitialised|uninitialized
//  - definitely lost
//  - Invalid read of size
//  - uninitialised value
val VALGRIND = ""
val THROW = false
//val VALGRIND = "valgrind "
//val THROW = true

fun all (inp: String, pre: Boolean=false): String {
    val prelude = if (XCEU) "xprelude.ceu" else "cprelude.ceu"
    val inps = listOf(Pair(Triple("anon",1,1), inp.reader())) + if (!pre) emptyList() else {
        listOf(Pair(Triple(prelude,1,1), File(prelude).reader()))
    }
    val lexer = Lexer(inps)
    val parser = Parser(lexer)
    val es = try {
        parser.exprs()
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
        return e.message!!
    }
    //println(es.map { it.tostr(false)+"\n" }.joinToString(""))
    val c = try {
        val outer = Expr.Do(Tk.Fix("", Pos("anon", 0, 0)), true, true, es)
        val ups = Ups(outer)
        val coder = Coder(outer, ups)
        coder.main()
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
        return e.message!!
    }
    File("out.c").writeText(c)
    val (ok2, out2) = exec("gcc -Werror out.c -lm -o out.exe")
    if (!ok2) {
        return out2
    }
    val (_, out3) = exec("$VALGRIND./out.exe")
    //println(out3)
    return out3
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TExec {

    // PRINT

    @Test
    fun aa_print1() {
        val out = all("""
            print([10])
        """)
        assert(out == "[10]") { out }
    }
    @Test
    fun aa_print2() {
        val out = all("""
            print(10)
            println(20)
        """)
        assert(out == "1020\n") { out }
    }
    @Test
    fun aa_print3() {
        val out = all("""
            println([[],[1,2,3]])
            println(func () { nil })
        """)
        assert(out.contains("[[],[1,2,3]]\nfunc: 0x")) { out }
    }
    @Test
    fun aa_print34() {
        val out = all("""
            var f
            set f = (func () { nil })
            do {
                var g
                set g = f
            }
            println(f)
        """)
        assert(out.contains("func: 0x")) { out }
    }
    @Test
    fun aa_print_err1() {
        val out = all("""
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_print_err2() {
        val out = all("""
            print(1)
            print()
            print(2)
            println()
            println(3)
        """)
        assert(out == "12\n3\n") { out }
    }
    @Test
    fun aa_print4() {
        val out = all("print(nil)")
        assert(out == "nil") { out }
    }
    @Test
    fun aa_print5() {
        val out = all("print(true)")
        assert(out == "true") { out }
    }
    @Test
    fun aa_print6() {
        val out = all("println(false)")
        assert(out == "false\n") { out }
    }

    // VAR

    @Test
    fun bb_var1() {
        val out = all("""
            var v
            print(v)
        """)
        assert(out == "nil") { out }
    }
    @Test
    fun bb_var2() {
        val out = all("""
            var vvv = 1
            print(vvv)
        """)
        assert(out == "1") { out }
    }

    // INDEX / TUPLE

    @Test
    fun cc_index01() {
        val out = all("""
            [1,2,3][1]
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_index011() {
        val out = all("""
            println([1,2,3][1])
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun cc_index_err01() {
        val out = all("""
            println([1,[2],3][1])   ;; [2] is at block, not at call arg // index made it outside call
        """)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun cc_index_err1() {
        val out = all("""
            println(1[1])
        """.trimIndent())
        assert(out == "anon : (lin 1, col 9) : index error : expected collection\n:error\n") { out }
    }
    @Test
    fun cc_index_err2() {
        val out = all("""
            println([1][[]])
        """.trimIndent())
        assert(out == "anon : (lin 1, col 9) : index error : expected number\n:error\n") { out }
    }
    @Test
    fun cc_index23() {
        val out = all("""
            println([[1]][[0][0]])
        """.trimIndent())
        assert(out == "[1]\n") { out }
    }
    @Test
    fun cc_index_err3() {
        val out = all("""
            println([1][2])
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 9) : index error : out of bounds\n") { out }
        assert(out == "anon : (lin 1, col 9) : index error : out of bounds\n:error\n") { out }
    }
    @Test
    fun cc_tuple4_free() {
        val out = all("""
            [1,2,3]
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_tuple45_free() {
        val out = all("""
            [1,2,3][1]
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_tuple5_free() {
        val out = all("""
            var f
            set f = func () { nil }
            f([1,2,3])
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cc_tuple56_free() {
        val out = all("""
            var x
            set x = do {
                [1]
            }
            println(x)
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun cc_tuple6_free() {
        val out = all("""
            var f
            set f = func (v) {
                ;;println(v)
                if v > 0 {
                    [f(v-1)]
                } else {
                    0
                }
            }
            println(f(3))
        """, true)
        assert(out == "[[[0]]]\n") { out }
    }
    @Test
    fun cc_tuple7_hold_err() {
        val out = all("""
            var f
            set f = func (v) {
                var x
                if v > 0 {
                    set x = f(v-1)  ;; invalid set
                    [x]
                } else {
                    0
                }
            }
            println(f(3))
        """, true)
        assert(out == "anon : (lin 12, col 21) : f(3)\n" +
                "anon : (lin 6, col 29) : f({-}(v,1))\n" +
                "anon : (lin 3, col 30) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun cc_tuple8_hold_err() {
        val out = all("""
            var f
            set f = func (v) {
                if v > 0 {
                    var x
                    set x = f(v-1)
                    [x] ;; invalid return
                } else {
                    0
                }
            }
            println(f(3))
        """, true)
        assert(out == "anon : (lin 12, col 21) : f(3)\n" +
                "anon : (lin 6, col 29) : f({-}(v,1))\n" +
                "anon : (lin 4, col 26) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun cc_tuple9_hold_err() {
        val out = all("""
            do {
                var x
                set x = [0]
                x   ;; escape but no access
            }
            println(1)
        """)
        //assert(out == "1\n") { out }
        assert(out == "anon : (lin 2, col 13) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun cc_tuple10_hold_err() {
        val out = all("""
            println(do {
                var xxx
                set xxx = [0]
                xxx
            })
        """)
        //assert(out == "anon : (lin 2, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 5, col 17) : return error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 2, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun cc_tuple11_copy() {
        val out = all("""
            var t1 = [1,2,3]
            var t2 = copy(t1)
            var t3 = t1
            set t1[2] = 999
            set t2[0] = 10
            println(t1)
            println(t2)
            println(t3)
        """, true)
        assert(out == "[1,2,999]\n[10,2,3]\n[1,2,999]\n") { out }
    }
    @Test
    fun cc_tuple12_free_copy() {
        val out = all("""
            var f
            set f = func (v) {
                ;;println(v)
                if v > 0 {
                    copy([f(v-1)])
                } else {
                    0
                }
            }
            println(f(3))
        """, true)
        assert(out == "[[[0]]]\n") { out }
    }
    @Test
    fun cc_tuple13_copy_out() {
        val out = all("""
            var out
            set out = do {
                var ins
                set ins = [1,2,3]
                copy(ins)
            }
            println(out)
        """, true)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun cc_tuple14_move_out() {
        val out = all("""
            var out
            set out = do {
                var ins
                set ins = [1,2,3]
                move(ins)
            }
            println(out)
        """, true)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun cc_tuple15_call_scope() {
        val out = all("""
            var f
            set f = func (v) {
                v
            }
            var x
            set x = f([10])
            println(x)
        """)
        //assert(out == "anon : (lin 7, col 21) : f([10])\nanon : (lin 3, col 30) : set error : incompatible scopes\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun cc_tuple16_move() {
        val out = all("""
            var v
            set v = do {
                move([[1,2]])
            }
            println(v)
        """)
        assert(out == "[[1,2]]\n") { out }
    }
    @Test
    fun cc_vector17_move() {
        val out = all("""
            var ttt
            set ttt = #[#[1,2]]
            println(ttt)
        """)
        assert(out == "#[#[1,2]]\n") { out }
    }
    @Test
    fun cc_dict18_move() {
        val out = all("""
            var v
            set v = do {
                @[(:v,@[(:v,2)])]
            }
            println(v)
        """)
        assert(out == "@[(:v,@[(:v,2)])]\n") { out }
    }
    @Test
    fun cc_vector19_print() {
        val out = all("""
            println(#[#[1,2]])
        """)
        assert(out == "#[#[1,2]]\n") { out }
    }
    @Test
    fun cc_vector20() {
        val out = all("""
            var v
            set v = #[10]
            println(v[#v-1])
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_tuple21_scope_copy() {
        val out = all("""
            var x = [1,2,3]
            do {
                var y = copy(x)
                do {
                    set x = y
                }
            }
            println(x)
        """, true)
        assert(out == "anon : (lin 6, col 25) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun cc_tuple22_scope_copy() {
        val out = all("""
            var x = [1,2,3]
            do {
                var y = copy(x)
                do {
                    set x = copy(y)
                }
            }
            println(x)
        """, true)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun cc_tuple23_scope_copy() {
        val out = all("""
            var v
            do {
                var x = [1,2,3]
                do {
                    var y = copy(x)
                    do {
                        set x = copy(y)
                        set v = x       ;; err
                    }
                }
            }
            println(v)
        """, true)
        assert(out == "anon : (lin 9, col 29) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }

    // DICT

    @Test
    fun dd_dict1() {
        val out = all("""
            println(type(@[(1,2)]))
            println(@[(1,2)])
        """)
        assert(out == ":dict\n@[(1,2)]\n") { out }
    }
    @Test
    fun dd_dict2() {
        val out = all("""
            var t
            set t = @[(:x,1)]
            println(t[:x])
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun dd_dict3() {
        val out = all("""
            var t
            set t = @[(:x,1)]
            println(t[:y])
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun dd_dict4() {
        val out = all("""
            var t
            set t = @[(:x,1)]
            set t[:x] = 2
            println(t[:x])
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun dd_dict5() {
        val out = all("""
            var t
            set t = @[]
            set t[:x] = 1
            set t[:y] = 2
            println(t)
        """)
        assert(out == "@[(:x,1),(:y,2)]\n") { out }
    }
    @Test
    fun dd_dict6_copy() {
        val out = all("""
            var t1
            set t1 = @[]
            set t1[:x] = 1
            var t2
            set t2 = t1
            var t3
            set t3 = copy(t1)
            set t1[:y] = 2
            set t3[:y] = 20
            println(t1)
            println(t2)
            println(t3)
        """)
        assert(out == "@[(:x,1),(:y,2)]\n@[(:x,1),(:y,2)]\n@[(:x,1),(:y,20)]\n") { out }
    }
    @Test
    fun todo_dd_dict7_err() {
        val out = all("""
            var x
            set x = @[(nil,10)]
            println(x[nil])
        """)
        assert(out.contains("ceu_dict_set: Assertion `key->type != CEU_VALUE_NIL' failed")) { out }
    }
    @Test
    fun todo_dd_dict8_err() {
        val out = all("""
            var x
            set x = @[]
            set x[nil] = 10
            println(x[nil])
        """)
        assert(out.contains("ceu_dict_set: Assertion `key->type != CEU_VALUE_NIL' failed")) { out }
    }
    @Test
    fun dd_dict9_next() {
        val out = all("""
            var t
            set t = @[]
            set t[:x] = 1
            set t[:y] = 2
            var k
            set k = next(t)
            println(k, t[k])
            set k = next(t,k)
            println(k, t[k])
            set k = next(t,k)
            println(k, t[k])
        """)
        assert(out == ":x\t1\n:y\t2\nnil\tnil\n") { out }
    }
    @Test
    fun dd_dict10_next() {
        val out = all("""
            var t
            set t = @[]
            set t[:x] = 1
            set t[:y] = 2
            set t[:z] = 3
            set t[:y] = nil
            set t[:x] = nil
            set t[:a] = 10
            set t[:b] = 20
            set t[:c] = 30
            var k
            set k = next(t)
            while k /= nil {
                println(k, t[k])
                set k = next(t,k)
            }
        """)
        assert(out == ":a\t10\n:b\t20\n:z\t3\n:c\t30\n") { out }
    }

    // VECTOR

    @Test
    fun ee_vector1() {
        val out = all("""
            println(#[])
        """)
        assert(out == "#[]\n") { out }
    }
    @Test
    fun vector2() {
        val out = all("""
            println(type(#[]))
            println(#[1,2,3])
        """)
        assert(out == ":vector\n#[1,2,3]\n") { out }
    }
    @Test
    fun vector3_err() {
        val out = all("""
            var v
            set v = #[]
            ;;println(#v)
            set v[#v] = 10
            ;;println(v)
            set v[5] = 10   ;; error
        """)
        //assert(out == "anon : (lin 7, col 17) : index error : out of bounds\n0\n#[10]\n") { out }
        assert(out == "anon : (lin 7, col 17) : index error : out of bounds\n:error\n") { out }
    }
    @Test
    fun vector4() {
        val out = all("""
            println(#(#[1,2,3]))
        """)
        assert(out == "3\n") { out }
    }
    @Test
    fun vector5() {
        val out = all("""
            var v
            set v = #[1,2,3]
            println(#v, v)
            set v[#v] = 4
            set v[#v] = 5
            println(#v, v)
            set v[#v-1] = nil
            println(#v, v)
            ;;set #v = 2       ;; error
        """, true)
        assert(out == "3\t#[1,2,3]\n5\t#[1,2,3,4,5]\n4\t#[1,2,3,4]\n") { out }
    }
    @Test
    fun todo_vector6a_err() {
        val out = all("""
            #[1,nil,3]  ;; v[2] = nil, but #v===1
        """)
        assert(out.contains("ceu_vector_set: Assertion `i == vec->Ncast.Vector.its-1' failed.")) { out }
    }
    @Test
    fun todo_vector6b_err() {
        val out = all("""
            #[1,'a',3]  ;; different type
        """)
        assert(out.contains("ceu_vector_set: Assertion `v.type == vec->Ncast.Vector.type' failed.")) { out }
    }
    @Test
    fun vector7_err() {
        val out = all("""
            #1
        """)
        assert(out == "anon : (lin 2, col 13) : {#}(1) : length error : not a vector\n:error\n") { out }
    }
    @Test
    fun vector8_err() {
        val out = all("""
            var v
            set v = #[1,2,3]
            println(v[#v])   ;; err
        """)
        //assert(out == "anon : (lin 4, col 23) : index error : out of bounds\n") { out }
        assert(out == "anon : (lin 4, col 21) : index error : out of bounds\n:error\n") { out }
    }
    @Test
    fun vector9_err() {
        val out = all("""
            set #v = 0
        """)
        assert(out == "anon : (lin 2, col 13) : invalid set : invalid destination") { out }
    }
    @Test
    fun vector10_pop_acc() {
        val out = all("""
            var v
            set v = #[1,2,3]
            var x
            set x = do :unnest :hide {
                var i
                set i = v[#v-1]
                set v[#v-1] = nil
                i
            }
            println(x, #v)
        """, true)
        assert(out == "3\t2\n") { out }
    }
    @Test
    fun vector11_copy() {
        val out = all("""
            var t1
            set t1 = #[]
            set t1[#t1] = 1
            println(t1)
        """, true)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun vector12_copy() {
        val out = all("""
            var t1
            set t1 = #[]        ;; [1,2]
            set t1[#t1] = 1
            var t2
            set t2 = t1         ;; [1,2]
            var t3
            set t3 = copy(t1)   ;; [1,20]
            set t1[#t1] = 2
            set t3[#t3] = 20
            println(t1)
            println(t2)
            println(t3)
        """, true)
        assert(out == "#[1,2]\n#[1,2]\n#[1,20]\n") { out }
    }
    @Test
    fun vector13_add() {
        val out = all("""
            do {       
                var ceu_ifs_17
                set ceu_ifs_17 = true    
                var v
                set v = #[]
                if true {                                                           
                    set v[{#}(v)] = 10                                              
                } else {                                                            
                    nil
                }
                println(v)
            }
        """, true)
        assert(out == "#[10]\n") { out }
    }

    // STRINGS / CHAR

    @Test
    fun string1() {
        val out = all("""
            var v
            set v = #['a','b','c']
            set v[#v] = 'a'
            set v[2] = 'b'
            println(v[0])
            `puts(${D}v.Dyn->Ncast.Vector.buf);`
            println(v)
        """)
        assert(out == "a\nabba\nabba\n") { out }
    }

    // DCL

    @Test
    fun dcl() {
        val out = all("""
            var x
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun dcl_chars() {
        val out = all("""
            var x'
            var f!
            var even?
            println(x')
            println(f!)
            println(even?)
        """)
        assert(out == "nil\nnil\nnil\n") { out }
    }
    @Test
    fun dcl_redeclaration_err() {
        val out = all("""
            var x
            var x
        """)
        assert(out == "anon : (lin 3, col 17) : declaration error : variable \"x\" is already declared") { out }
    }
    @Test
    fun todo_dcl4_dup() {
        val out = all("""
            do {
                var x
                println(x)
            }
            do {
                var x
                println(x)
            }
        """)
        assert(out == "nil\nnil\n") { out }
    }

    // SET

    @Test
    fun set1() {
        val out = all("""
            var x
            set x = [10]
            println(x)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun set2() {
        val out = all("""
            var x
            set x = [10,20,[30]]
            set x[1] = 22
            set x[2][0] = 33
            println(x)
        """)
        assert(out == "[10,22,[33]]\n") { out }
    }
    @Test
    fun set_err1() {
        val out = all("""
            set 1 = 1
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : invalid set : invalid destination") { out }
    }
    @Test
    fun set_err2() {
        val out = all("""
            set [1] = 1
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : invalid set : invalid destination") { out }
    }
    @Test
    fun set_index() {
        val out = all("""
            var i
            set i = 1
            println([1,2,3][i])
        """)
        assert(out == "2\n") { out }
    }

    // DO

    @Test
    fun do1() {  // set whole tuple?
        val out = all("""
            do { nil }
        """)
        assert(out == "") { out }
    }
    @Test
    fun do2() {
        val out = all("""
            do {
                var a
                set a = 1
                println(a)
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun do3() {
        val out = all("""
            var x
            set x = do {
                var a
                set a = 10
                a
            }
            print(x)
        """)
        assert(out == "10") { out }
    }
    @Test
    fun do4() {
        val out = all("""
            var x
            set x = do {nil}
            println(x)
        """)
        assert(out == "nil\n") { out }
    }

    // GROUP

    @Test
    fun group1() {
        val out = all("""
            do :unnest {
                var a
                set a = 10
            }
            do :unnest {
                var x
                set x = a
            }
            print(x)
        """)
        assert(out == "10") { out }
    }
    @Test
    fun group2_err() {
        val out = all("""
            do :unnest :hide {
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
    fun group3() {
        val out = all("""
            var x
            set x = do :unnest :hide {
                var a       ;; invisible
                set a = []
                a
            }
            print(x)
        """)
        assert(out == "[]") { out }
    }

    // SCOPE

    @Test
    fun scope1() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
            }
            println(x)
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun scope_err2() {
        val out = all("""
            var x
            do {
                var a
                set a = [1,2,3]
                set x = a
            }
        """)
        //assert(out == "anon : (lin 3, col 13) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 6, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun scope4() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                set x[1] = [4,5,6]
                do {
                    var y
                    set y = [10,20,30]
                    set y[1] = x[1]
                    set x[2] = y[1]
                }
            }
            println(x)
        """)
        assert(out == "[1,[4,5,6],[4,5,6]]\n") { out }
    }
    @Test
    fun scope5_err() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                var y
                set y = [10,20,30]
                set x[2] = y
            }
            println(x)
        """)
        //assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun scope6() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                var y
                set y = 30
                set x[2] = y
            }
            println(x)
        """)
        assert(out == "[1,2,30]\n") { out }
    }
    @Test
    fun scope7() {
        val out = all("""
            var xs
            set xs = do {
                [10]
            }
            println(xs)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun scope8_underscore() {
        val out = all("""
            var x
            do {
                var _a
                set _a = [1,2,3]
                set x = _a
            }
            println(x)
        """)
        assert(out == "[1,2,3]\n") { out }
        //assert(out == "anon : (lin 3, col 13) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun scope9_err() {
        val out = all("""
            var x
            do {
                var a
                set a = @[(1,[])]
                set x = a
            }
        """)
        assert(out == "anon : (lin 6, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun scope10_err() {
        val out = all("""
            var out
            do {
                var x
                set x = []
                set out = [x]
            }
            println(1)
        """)
        assert(out == "anon : (lin 6, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun scope11_err() {
        val out = all("""
            var out
            do {
                var x
                set x = []
                set out = #[x]
            }
            println(1)
        """)
        assert(out == "anon : (lin 6, col 21) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun scope12_err() {
        val out = all("""
            var out
            do {
                var x
                set x = []
                set out = @[(1,x)]
            }
            println(1)
        """)
        assert(out == "anon : (lin 6, col 21) : set error : incompatible scopes\n:error\n") { out }
    }

    // IF

    @Test
    fun if1_err() {
        val out = all("""
            var x
            set x = if (true) { 1 }
            println(x)
        """)
        assert(out == "anon : (lin 4, col 13) : expected \"else\" : have \"println\"") { out }
    }
    @Test
    fun if2_err() {
        val out = all("""
            var x
            set x = 10
            set x = if false { 1 }
            println(x)
        """)
        assert(out == "anon : (lin 5, col 13) : expected \"else\" : have \"println\"") { out }
    }
    @Test
    fun if3_err() {
        val out = all("""
            var x
            set x = 10
            set x = if (nil) {} else { 1 }
            println(x)
        """.trimIndent())
        //assert(out == "anon : (lin 3, col 13) : if error : invalid condition\n") { out }
        assert(out == "anon : (lin 3, col 19) : expected expression : have \"}\"") { out }
    }
    @Test
    fun if4_err() {
        val out = all("""
            println(if [] {nil} else {nil})
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 4) : if error : invalid condition\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun if_hld() {
        val out = all("""
            if [] {nil} else {nil}
            println(1)
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 4) : if error : invalid condition\n") { out }
        assert(out == "1\n") { out }
    }

    // FUNC / CALL

    @Test
    fun func1() {
        val out = all("""
            var f
            set f = func () { nil }
            var x
            set x = f()
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun func2() {
        val out = all("""
            var f
            set f = func () {
                1
            }
            var x
            set x = f()
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun func3() {
        val out = all("""
            var f
            set f = func (xxx) {
                ;;println(type(xxx))
                xxx
            }
            var yyy
            set yyy = f(10)
            println(yyy)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun func4() {
        val out = all("""
            var f
            set f = func (x) {
                x
            }
            var x
            set x = f()
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun func5_err() {
        val out = all("""
            var f
            set f = func (x) {
                [x]
            }
            var x
            set x = f(10)
            println(x)
        """)
        assert(out == "[10]\n") { out }
        //assert(out.contains("set error : incompatible scopes")) { out }
    }
    @Test
    fun func7_err() {
        val out = all("1(1)")
        assert(out == "anon : (lin 1, col 1) : call error : expected function\n:error\n") { out }
    }
    @Test
    fun func8() {
        val out = all("""
            1
            (1)
        """)
        //assert(out == "anon : (lin 2, col 2) : call error : \"(\" in the next line") { out }
        assert(out == "") { out }
    }
    @Test
    fun func9() {
        val out = all("""
            var f
            set f = func (a,b) {
                [a,b]
            }
            println(f())
            println(f(1))
            println(f(1,2))
            println(f(1,2,3))
        """)
        assert(out == "[nil,nil]\n[1,nil]\n[1,2]\n[1,2]\n") { out }
    }
    @Test
    fun func10_err() {
        val out = all("""
            f()
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : access error : variable \"f\" is not declared") { out }
    }
    @Test
    fun func11() {
        val out = ceu.all(
            """
            println(func (x) {
                var fff
                set fff = func (xxx) {
                    println(type(xxx))
                    xxx
                }
                fff(x)
            } (10))
        """)
        assert(out == ":number\n10\n") { out }
    }
    @Test
    fun func12() {
        val out = ceu.all(
            """
            var fff
            set fff = func (xxx) {
                println(type(xxx))
                xxx
            }
            println(func () {
                fff(10)
            } ())
        """)
        assert(out == ":number\n10\n") { out }
    }
    @Test
    fun func13() {
        val out = ceu.all(
            """
            func () {
                var fff
                set fff = func () {
                    println(1)
                }
                fff()
            } ()
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun func14() {
        val out = ceu.all(
            """
            println(func (x) {
                var fff
                set fff = func (xxx) {
                    println(type(xxx))
                    xxx
                }
                fff(x)
            } (10))
        """)
        assert(out == ":number\n10\n") { out }
    }
    @Test
    fun func15() {
        val out = ceu.all(
            """
            func (xxx) {
                println(xxx)
                func () {
                    println(xxx)
                }()
            }(10)
        """)
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun func16_print() {
        val out = all("""
            var f
            set f = println
            f(false)
        """)
        assert(out == "false\n") { out }
    }
    @Test
    fun func17_ff() {
        val out = all("""
            var f
            f()()
        """)
        assert(out == "anon : (lin 3, col 13) : call error : expected function\n:error\n") { out }
    }
    @Test
    fun todo_use_bef_dcl_func18() {
        val out = all("""
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

    // WHILE

    @Test
    fun while6() {
        val out = all("""
            var x
            set x = true
            while x {
                set x = false
            }
            println(x)
        """)
        assert(out == "false\n") { out }
    }

    // ceu.getTHROW / CATCH

    @Test
    fun catch1() {
        val out = all("""
            catch err==:x {
                throw(:x)
                println(9)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun catch2_err() {
        val out = all("""
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
    fun catch3() {
        val out = all("""
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
    fun catch4_valgrind() {
        val out = all("""
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
    fun catch5() {
        val out = all("""
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
    fun catch6_err() {
        val out = all("""
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
    fun catch7() {
        val out = ceu.all(
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
    fun catch8() {
        val out = ceu.all(
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
        //assert(out == "anon : (lin 4, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "[:x]\n") { out }
    }
    @Test
    fun catch89_err() {
        val out = ceu.all(
            """
            do {
                catch do {  ;; err is binded to x and is being moved up
                    var x
                    set x = err
                    false
                } {
                    throw([:x])
                    println(9)
                }
            }
        """
        )
        assert(out == "anon : (lin 2, col 13) : set error : incompatible scopes\n" +
                "anon : (lin 8, col 21) : throw([:x])\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun catch9() {
        val out = ceu.all(
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
    fun catch10_err() {
        val out = ceu.all(
            """
            catch err[0]==:x {
                throw([:x])
                println(9)
            }
            println(err)
        """
        )
        //assert(out == "nil\n") { out }
        assert(out.contains("error: ???ceu_err??? undeclared")) { out }
    }
    @Test
    fun catch11() {
        val out = ceu.all(
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
    fun catch12() {
        val out = all(
            """
            catch err==[] {
                throw([])
                println(9)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun catch13() {
        val out = ceu.all(
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
        assert(out.contains("error: ???ceu_err??? undeclared")) { out }
    }
    @Test
    fun catch14() {
        val out = all("""
            catch err==[] {
                var xxx
                set xxx = []
                throw(xxx)
            }
            println(1)
        """, true)
        assert(out == "anon : (lin 2, col 27) : set error : incompatible scopes\n" +
                "anon : (lin 5, col 17) : throw(xxx)\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }

    // NATIVE

    @Test
    fun native1() {
        val out = all("""
            ```
                printf("xxx\n");
            ```
        """)
        assert(out == "xxx\n") { out }
    }
    @Test
    fun native2_num() {
        val out = all("""
            var x
            set x = `:number 1.5`
            println(x)
            println(`:number 1.5`)
        """)
        assert(out == "1.5\n1.5\n") { out }
    }
    @Test
    fun native3_str() {
        val out = all("""
            var x
            set x = `:pointer "ola"`
            ```
                puts(${D}x.Pointer);
            ```
        """)
        assert(out == "ola\n") { out }
    }
    @Test
    fun native4_err() {
        val out = all("""
            var x
            set x = ``` ```
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun native5() {
        val out = all("""
            var x
            set x = 10
            set x =  ```:number
                (printf(">>> %g\n", ${D}x.Number),
                ${D}x.Number*2)
            ```
            println(x)
        """)
        assert(out == ">>> 10\n20\n") { out }
    }
    @Test
    fun native6() {
        val out = all("""
            var x
            set x = 1
            ```
                ${D}x.Number = 20;
            ```
            println(x)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun native7_err() {
        val out = all("""
             `
             
                $D{x.y}
                
             `
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : native error : (lin 3, col 4) : invalid identifier") { out }
    }
    @Test
    fun native8() {
        val out = all("""
            var x
            set x = 0
            var f
            set f = func () {
                ```
                    ${D}x.Number = 20;
                ```
            }
            f()
            println(x)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun native9_err() {
        val out = all("""
             `
                $D 
             `
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : native error : (lin 2, col 4) : invalid identifier") { out }
    }
    @Test
    fun native10_err() {
        val out = all("""
            ` ($D) `
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : native error : (lin 1, col 4) : invalid identifier") { out }
    }
    @Test
    fun native11_pointer() {
        val out = all("""
            var f
            set f = func () {
                `:pointer
                    "ola"
                `
            }
            var g
            set g = func (x) {
                `
                    printf("%s\n", (char*)${D}x.Pointer);
                `
            }
            var x
            set x = f()
            g(x)
        """)
        assert(out == "ola\n") { out }
    }
    @Test
    fun native12_pointer() {
        val out = all("""
            println(`:pointer"oi"`)
        """)
        assert(out.contains("pointer: 0x")) { out }
    }
    @Test
    fun native13_pre() {
        val out = all("""
            ```:pre
            int X = 1;
            ```
            var f
            set f = func () {
                `:number X`
            }
            println(f())
        """)
        assert(out.contains("1\n")) { out }
    }
    @Test
    fun native14_char() {
        val out = all("""
            var c
            set c = `:char 'x'`
            `putchar(${D}c.Char);`
        """)
        assert(out == "x") { out }
    }
    @Test
    fun native15_func() {
        val out = all("""
            var f = func (v) {
                println(v)
                v
            }
            var f' = `:func ${D}f.Dyn`
            println(f'(10))
        """)
        assert(out == "10\n10\n") { out }
    }

    // OPERATORS

    @Test
    fun op_umn() {
        val out = all("""
            println(-10)
        """, true)
        assert(out == "-10\n") { out }
    }
    @Test
    fun op_id1() {
        val out = all("""
            println({-}(10,4))
        """, true)
        assert(out == "6\n") { out }
    }
    @Test
    fun op_arithX() {
        val out = all("""
            println(((10 + -20)*2)/5)
        """, true)
        assert(out == "-4\n") { out }
    }
    @Test
    fun todo_op_id2() {
        val out = all("""
            set (+) = (-)
            println((+)(10,4))
        """)
        assert(out == "6\n") { out }
    }
    @Test
    fun op_cmp() {
        val out = all("""
            println(1 > 2)
            println(1 < 2)
            println(1 == 1)
            println(1 /= 1)
            println(2 >= 1)
            println(2 <= 1)
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\ntrue\nfalse\n") { out }
    }
    @Test
    fun op_eq() {
        val out = all("""
            println(1 == 1)
            println(1 /= 1)
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun op_assoc_err() {
        val out = all("""
            println(2 * 3 - 1)
        """, true)
        assert(out == "anon : (lin 2, col 27) : binary operation error : expected surrounding parentheses") { out }
    }
    @Test
    fun op_assoc() {
        val out = all("""
            println((2 * 3) - 1)
        """, true)
        assert(out == "5\n") { out }
    }
    @Test
    fun op_eq_tup() {
        val out = ceu.all(
            """
            println([1] == [1])
            println([ ] == [1])
            println([1] /= [1])
            println([1,[],[1,2,3]] == [1,[],[1,2,3]])
        """)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }
    @Test
    fun op_eq_tupXX() {
        val out = ceu.all(
            """
            println([1,[1],1] == [1,[1],1])
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ops_oth() {
        val out = all("""
            println(2**3)
            println(8//3)
            println(8%3)
        """, true)
        assert(out == "8\n2\n2\n") { out }
    }

    // tonumber, tostring

    @Test
    fun tostring1() {
        val out = all("""
            var s
            set s = tostring(10)
            println(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun tonumber2() {
        val out = all("""
            var n
            set n = tonumber(#['1','0'])
            println(type(n), n)
        """, true)
        assert(out == ":number\t10\n") { out }
    }
    @Test
    fun tonumber_tostring3() {
        val out = all("""
            var s
            set s = tostring(tonumber(#['1','0']))
            println(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }

    // TYPE

    @Test
    fun type1() {
        val out = all("""
            var t
            set t = type(1)
            println(t)
            println(type(t))
            println(type(type(t)))
        """)
        assert(out == ":number\n:tag\n:tag\n") { out }
    }

    // TAGS

    @Test
    fun tags1() {
        val out = all("""
            println(:xxx)
            println(:xxx == :yyy)
            println(:xxx /= :yyy)
            println(:xxx == :xxx)
            println(:xxx /= :xxx)
        """)
        assert(out == ":xxx\nfalse\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun tags2() {
        val out = all("""
            func () {
                println(:xxx)
            }()
            func () {
                println(:xxx)
            }()
        """)
        assert(out == ":xxx\n:xxx\n") { out }
    }
    @Test
    fun tags3() {
        val out = all("""
            func () {
                println(:Xxx.Yyy)
            }()
            func () {
                println(:1.2.3)
            }()
        """)
        assert(out == ":Xxx.Yyy\n:1.2.3\n") { out }
    }
    @Test
    fun todo_tags4_err() {
        val out = all("""
            tags([])
        """)
        assert(out.contains("ceu_tags_f: Assertion `n >= 2' failed")) { out }
    }
    @Test
    fun tags5() {
        val out = all("""
            println(tags(1,:2))
        """)
        assert(out == "false\n") { out }
    }
    @Test
    fun tags6_err() {
        val out = all("""
            tags([],2)
        """)
        assert(out.contains("Assertion `tag->type == CEU_VALUE_TAG'")) { out }
    }
    @Test
    fun tags7_err() {
        val out = all("""
            tags([],:x,nil)
        """)
        assert(out.contains("Assertion `bool->type == CEU_VALUE_BOOL' failed")) { out }
    }
    @Test
    fun tags8() {
        val out = all("""
            var t
            set t = []
            var x1
            set x1 = tags(t,:x,true)
            var x2
            set x2 = tags(t,:x,true)
            println(x1, x2, x1==t)
            set x1 = tags(t,:x,false)
            set x2 = tags(t,:x,false)
            println(x1, x2, x1==t)
        """)
        assert(out == "[]\tnil\ttrue\n[]\tnil\ttrue\n") { out }
    }
    @Test
    fun tags9() {
        val out = all("""
            var t
            set t = []
            tags(t,:x,true)
            println(tags(t, :x))
            tags(t,:y,true)
            println(tags(t, :y))
            tags(t,:x,false)
            println(tags(t, :x))
        """, true)
        assert(out == "true\ntrue\nfalse\n") { out }
    }

    // DEFER

    @Test
    fun defer1() {
        val out = all("""
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
    fun todo_defer2_err() {
        val out = all("""
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
    fun defer3() {
        val out = all("""
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
    fun defer4_err() {
        val out = all("""
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

    // CLOSURE / ESCAPE / FUNC / UPVALS

    @Test
    fun clo1_err() {
        val out = all(
            """
            var ^^x     ;; can't declare upref
        """
        )
        assert(out == "anon : (lin 2, col 17) : var error : cannot declare an upref") { out }
    }
    @Test
    fun clo2_err() {
        val out = all("""
            var ^x     ;; upvar can't be global
        """)
        assert(out == "anon : (lin 2, col 17) : var error : cannot declare a global upvar") { out }
    }
    @Test
    fun clo3_err() {
        val out = all("""
            ^x     ;; upvar can't be in global
        """)
        assert(out == "anon : (lin 2, col 13) : access error : variable \"x\" is not declared") { out }
    }
    @Test
    fun clo4_err() {
        val out = all("""
            ^^x     ;; upref can't be in global
        """)
        assert(out == "anon : (lin 2, col 13) : access error : variable \"x\" is not declared") { out }
    }
    @Test
    fun clo5_err() {
        val out = all("""
            var g
            set g = 10
            var f
            set f = func (^x) {
                set ^x = []  ;; err: cannot reassign
                func () {
                    ^^x == g
                }
            }
            println(f([])())
        """)
        assert(out == "anon : (lin 6, col 21) : set error : cannot reassign an upval") { out }
    }
    @Test
    fun clo6_err() {
        val out = all("""
            var g
            set g = 10
            var f
            set f = func (^x) {
                func () {
                    set ^^x = []  ;; err: cannot reassign
                    ^^x + g
                }
            }
            println(f([])())
        """)
        assert(out == "anon : (lin 7, col 25) : set error : cannot reassign an upval") { out }
    }
    @Test
    fun clo7_err() {
        val out = all("""
            do {
                var ^x     ;; err: no associated upref
                ^x
            }
            println(1)
        """)
        assert(out == "anon : (lin 3, col 21) : var error : unreferenced upvar") { out }
    }
    @Test
    fun clo8_err() {
        val out = all("""
            do {
                ^^x     ;; err: no associated upvar
            }
        """)
        assert(out == "anon : (lin 3, col 17) : access error : variable \"x\" is not declared") { out }
    }
    @Test
    fun clo9_err() {
        val out = all("""
            do {
                var x
                ^^x     ;; err: no associated upvar
            }
        """)
        assert(out == "anon : (lin 4, col 17) : access error : incompatible upval modifier") { out }
    }
    @Test
    fun clo10_err() {
        val out = all("""
            do {
                var ^x
                ^^x     ;; err: no associated upvar
            }
        """)
        assert(out == "anon : (lin 4, col 17) : access error : unnecessary upref modifier") { out }
    }
    @Test
    fun clo11_err() {
        val out = all("""
            var f
            set f = do {
                var x
                func () {   ;; block_set(1)
                    x       ;; because of x
                }           ;; err: scope on return
            }
            println(f(10))
        """)
        assert(out == "anon : (lin 3, col 21) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun clo12_err() {
        val out = all("""
            var f
            set f = func (x) {
                func () {   ;; block_set(1)
                    x       ;; because of x
                }           ;; err: scope on return
            }
            println(f(10)())
        """)
        assert(out == "anon : (lin 8, col 21) : f(10)\n" +
                "anon : (lin 3, col 30) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun clo13_err() {
        val out = all("""
            var g
            set g = 10
            var f
            set f = func (^x) {
                func () {       ;; block_set(0)
                    ^^x + g     ;; all (non-global) upvals are marked
                }
            }
            println(f(20)())
        """, true)
        assert(out == "30\n") { out }
    }
    @Test
    fun clo14() {
        val out = all("""
            var g
            set g = 10
            var f
            set f = func (^x) {
                func () {
                    ^^x[0] + g
                }
            }
            println(f([20])())
        """, true)
        assert(out == "30\n") { out }
    }
    @Test
    fun clo15() {
        val out = all("""
            var f
            set f = func (^x) {
                func (y) {
                    [^^x,y]
                }
            }
            println(f([10])(20))
        """)
        assert(out == "[[10],20]\n") { out }
    }
    @Test
    fun clo16() {
        val out = all("""
            var f
            set f = func () {
                var ^x = 10     ;; TODO: needs initialization
                func (y) {
                    [^^x,y]
                }
            }
            println(f()(20))
        """)
        assert(out == "[10,20]\n") { out }
    }
    @Test
    fun clo17() {
        val out = all("""
            var f
            set f = func (^x) {
                println(:1, ^x)
                func () {
                    println(:2, ^^x)
                    ^^x
                }
            }
            println(:3, f(10)())
        """)
        assert(out == ":1\t10\n:2\t10\n:3\t10\n") { out }
    }
    @Test
    fun clo18() {
        val out = all("""
            var f
            set f = func (^x) {
                func () {
                    ^^x
                }
            }
            println(f(10)())
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun clo19() {
        val out = all("""
            var curry
            set curry = func (^fff) {
                func (^xxx) {
                    func (yyy) {
                        ^^fff(^^xxx,yyy)
                    }
                }
            }

            var f = func (a,b) {
                [a,b]
            }
            var f' = curry(f)
            println(f'(1)(2))
        """)
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun clo20() {
        val out = all("""
            var curry
            set curry = func (^fff) {
                func (^xxx) {
                    func (yyy) {
                        ^^fff(^^xxx,yyy)
                    }
                }
            }
            var f = func (a,b) {
                [a,b]
            }
            println(curry(f)(1)(2))
        """)
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun clo21() {
        val out = all("""
            var f = func (^a) {
                func () {
                    ^^a
                }
            }
            var g = do {
                var t = [1]
                f(t)
            }
            println(g())
        """)
        assert(out == "anon : (lin 7, col 21) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun tup22() {
        val out = all("""
            var g = do {
                var t = [1]
                [t]
            }
            println(g)
        """)
        assert(out == "anon : (lin 2, col 21) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun clo23() {
        val out = all("""
            var f = func (^a) {
                func () {
                    ^^a
                }
            }
            var g = do {
                var t = [1]
                move(f(t))
            }
            println(g())
        """)
        assert(out == "[1]\n") { out }
    }
    @Ignore
    @Test
    fun todo_clo24_copy() {
        val out = all("""
            var f = func (^a) {
                func () {
                    ^^a
                }
            }
            var g = do {
                var t = [1]
                var i = copy(f(t))
                set t[0] = 10
                move(i)
            }
            println(g())
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun clo25_compose() {
        val out = all(
            """
            var comp = func (^f) {
                func (v) {
                    ^^f(v)
                }
            }
            var f = func (x) {
                x
            }
            var ff = comp(f)
            println(ff(2))
        """,
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun clo26_compose() {
        val out = all(
            """
            var comp = func (^f,^g) {
                func (v) {
                    ^^f(^^g(v))
                }
            }
            var f = func (x) {
                x
            }
            var ff = comp(f,f)
            println(ff(2))
        """,
        )
        assert(out == "2\n") { out }
    }

    //  MEM-GC-REF-COUNT

    @Test
    fun gc1() {
        val out = all("""
            var xxx = []
            set xxx = []
            println(`:number ceu_gc_count`)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun gc2() {
        val out = all("""
            []  ;; not checked
            []  ;; not checked
            println(`:number ceu_gc_count`)
        """)
        //assert(out == "2\n") { out }
        assert(out == "0\n") { out }
    }
    @Test
    fun gc3_cycle() {
        val out = all("""
            var x = [nil]
            var y = [x]
            set x[0] = y
            set x = nil
            set y = nil
            println(`:number ceu_gc_count`)
        """)
        assert(out == "0\n") { out }
    }
    @Test
    fun gc4() {
        val out = all("""
            var x = []
            var y = [x]
            set x = nil
            println(`:number ceu_gc_count`)
            set y = nil
            println(`:number ceu_gc_count`)
        """)
        assert(out == "0\n2\n") { out }
    }
    @Test
    fun gc5() {
        val out = all("""
            var x = []
            do {
                var y = x
            }
            set x = nil
            println(`:number ceu_gc_count`)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun gc6() {
        val out = all("""
            var x = [[],[]]
            set x = nil
            println(`:number ceu_gc_count`)
        """)
        assert(out == "3\n") { out }
    }
    @Test
    fun gc7() {
        val out = all("""
            var f = func (v) {
                v
            }
            #( #[ f([1]) ] )
            println(`:number ceu_gc_count`)
        """)
        //assert(out == "2\n") { out }
        assert(out == "0\n") { out }
    }
    @Test
    fun gc8() {
        val out = all("""
            do {
                var out
                set out = do {
                    var ins = [1,2,3]
                    move(ins)
                }
                println(`:number ceu_gc_count`)
            }
            println(`:number ceu_gc_count`)
        """, true)
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun gc9() {
        val out = all("""
            var out
            set out = do {
                var ins
                set ins = [1,2,3]
                ins
            }
            println(out)
        """, true)
        assert(out == "anon : (lin 3, col 23) : set error : incompatible scopes\n" +
                ":error\n") { out }
    }
    @Test
    fun gc10() {
        val out = all("""
            do {
                do {
                    var v = []
                    move(v)
                }
                ;; [] not captured, should be checked 
                println(`:number ceu_gc_count`)
            }
            println(`:number ceu_gc_count`)
        """, true)
        //assert(out == "1\n1\n") { out }
        assert(out == "0\n0\n") { out }
    }
    @Test
    fun gc11() {
        val out = all("""
            var f = func (v) {
                v
            }
            f([])
            ;; [] not captured, should be checked 
            println(`:number ceu_gc_count`)
        """)
        //assert(out == "anon : (lin 7, col 21) : f([10])\nanon : (lin 3, col 30) : set error : incompatible scopes\n") { out }
        //assert(out == "1\n") { out }
        assert(out == "0\n") { out }
    }
    @Test
    fun gc12() {
        val out = all("""
            println([]) ;; println does not check
            println(`:number ceu_gc_count`)
        """)
        assert(out == "[]\n0\n") { out }
    }

    // MISC

    @Test
    fun id_c() {
        val out = all("""
            var xxx
            set xxx = func () {nil}
            println(xxx())
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun clo1() {
        val out = all("""
            var f
            set f = func (^x) {
                func (y) {
                    if ^^x { ^^x } else { y }
                }
            }
            println(f(3)(1))
        """)
        assert(out == "3\n") { out }
    }

    // TEMPLATE

    @Test
    fun todo_tpl1() {
        val out = all("""
            template :T = [x,y]
            var t :T
            set t = [1,2]
            println(t.x, t.y)
        """, true)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun todo_tpl2() {
        val out = all("""
            template :T = [x,y]
            template :S = [a:T,b:T]
            var s :S
            set s = [[1,2],[10,20]]
            println(s.a, s.b.y)
        """, true)
        assert(out == "[1,2]\t20\n") { out }
    }
}
