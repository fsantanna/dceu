package ceu

import Lexer
import Parser
import Expr
import Coder
import Pos
import Ups
import XCEU
import exec
import main
import org.junit.Ignore
import org.junit.Test
import java.io.File

// search in tests output for
//  definitely|Invalid read|uninitialized
//  - definitely lost
//  - Invalid read of size
//  - uninitialised value
val VALGRIND = ""
val THROW = false
//val VALGRIND = "valgrind "
//val THROW = true

fun all (inp: String, pre: Boolean=false): String {
    val prelude = if (XCEU) "xprelude.ceu" else "prelude.ceu"
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
    //println(es.map { it.toString()+"\n" }.joinToString(""))
    //println(es.map { it.tostr(true)+"\n" }.joinToString(""))
    //println(es.map { it.tostr()+"\n" }.joinToString(""))
    val c = try {
        val outer = Expr.Block(Tk.Fix("", Pos("anon", 0, 0)), es)
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

class TExec {

    // PRINT

    @Test
    fun print1() {
        val out = all("""
            print([10])
        """)
        assert(out == "[10]") { out }
    }
    @Test
    fun print2() {
        val out = all("""
            print(10)
            println(20)
        """)
        assert(out == "1020\n") { out }
    }
    @Test
    fun print3() {
        val out = all("""
            println([[],[1,2,3]])
            println(func () { nil })
        """)
        assert(out.contains("[[],[1,2,3]]\nfunc: 0x")) { out }
    }
    @Test
    fun print34() {
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
    fun print_err1() {
        val out = all("""
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun print_err2() {
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
    fun print4() {
        val out = all("print(nil)")
        assert(out == "nil") { out }
    }
    @Test
    fun print5() {
        val out = all("print(true)")
        assert(out == "true") { out }
    }
    @Test
    fun print6() {
        val out = all("println(false)")
        assert(out == "false\n") { out }
    }

    // INDEX / TUPLE

    @Test
    fun index01() {
        val out = all("""
            [1,2,3][1]
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun index011() {
        val out = all("""
            println([1,2,3][1])
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun index_err01() {
        val out = all("""
            println([1,[2],3][1])   ;; [2] is at block, not at call arg // index made it outside call
        """)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun index_err1() {
        val out = all("""
            println(1[1])
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 9) : index error : expected collection\n") { out }
        assert(out == "core library : index error : expected collection\n") { out }
    }
    @Test
    fun index_err2() {
        val out = all("""
            println([1][[]])
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 9) : index error : expected number\n") { out }
        assert(out == "core library : index error : expected number\n") { out }
    }
    @Test
    fun index23() {
        val out = all("""
            println([[1]][[0][0]])
        """.trimIndent())
        assert(out == "[1]\n") { out }
    }
    @Test
    fun index_err3() {
        val out = all("""
            println([1][2])
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 9) : index error : out of bounds\n") { out }
        assert(out == "core library : index error : out of bounds\n") { out }
    }
    @Test
    fun tuple4_free() {
        val out = all("""
            [1,2,3]
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun tuple45_free() {
        val out = all("""
            [1,2,3][1]
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun tuple5_free() {
        val out = all("""
            var f
            set f = func () { nil }
            f([1,2,3])
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun tuple56_free() {
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
    fun tuple6_free() {
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
    fun tuple7_hold_err() {
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
        //assert(out == "anon : (lin 5, col 17) : return error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 6, col 29) : return error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 3, col 30) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 5, col 26) : set error : incompatible scopes\n") { out }
        assert(out == "core library : set error : incompatible scopes\n") { out }
    }
    @Test
    fun tuple8_hold_err() {
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
        //assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 7, col 21) : return error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 3, col 30) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 4, col 26) : set error : incompatible scopes\n") { out }
        assert(out == "core library : set error : incompatible scopes\n") { out }
    }
    @Test
    fun tuple9_hold_err() {
        val out = all("""
            do {
                var x
                set x = [0]
                x   ;; escape but no access
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 2, col 13) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun tuple10_hold_err() {
        val out = all("""
            println(do {
                var x
                set x = [0]
                x
            })
        """)
        //assert(out == "anon : (lin 2, col 21) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 5, col 17) : return error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "core library : set error : incompatible scopes\n") { out }
    }
    @Test
    fun tuple11_copy() {
        val out = all("""
            var t1
            set t1 = [1,2,3]
            var t2
            set t2 = copy(t1)
            var t3
            set t3 = t1
            set t1[2] = 999
            set t2[0] = 10
            println(t1)
            println(t2)
            println(t3)
        """, true)
        assert(out == "[1,2,999]\n[10,2,3]\n[1,2,999]\n") { out }
    }
    @Test
    fun tuple12_free_copy() {
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
    fun tuple13_copy_out() {
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
    fun tuple14_move_out() {
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

    // DICT

    @Test
    fun dict1() {
        val out = all("""
            println(tags(@[(1,2)]))
            println(@[(1,2)])
        """)
        assert(out == ":dict\n@[(1,2)]\n") { out }
    }
    @Test
    fun dict2() {
        val out = all("""
            var t
            set t = @[(:x,1)]
            println(t[:x])
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun dict3() {
        val out = all("""
            var t
            set t = @[(:x,1)]
            println(t[:y])
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun dict4() {
        val out = all("""
            var t
            set t = @[(:x,1)]
            set t[:x] = 2
            println(t[:x])
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun dict5() {
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
    fun dict6_copy() {
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

    // VECTOR

    @Test
    fun vector1() {
        val out = all("""
            println(#[])
        """)
        assert(out == "#[]\n") { out }
    }
    @Test
    fun vector2() {
        val out = all("""
            println(tags(#[]))
            println(#[1,2,3])
        """)
        assert(out == ":vector\n#[1,2,3]\n") { out }
    }
    @Test
    fun vector3_err() {
        val out = all("""
            var v
            set v = #[]
            println(#v)
            set v[#v] = 10
            println(v)
            set v[5] = 10   ;; error
        """)
        //assert(out == "anon : (lin 7, col 17) : index error : out of bounds\n0\n#[10]\n") { out }
        assert(out == "core library : index error : out of bounds\n0\n#[10]\n") { out }
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
    fun vector6_err() {
        val out = all("""
            #[1,nil,3]
        """)
        assert(out == "anon : (lin 2, col 13) : vector error : non homogeneous arguments\n") { out }
    }
    @Test
    fun vector7_err() {
        val out = all("""
            #1
        """)
        assert(out == "core library : length error : not a vector\n") { out }
    }
    @Test
    fun vector8_err() {
        val out = all("""
            var v
            set v = #[1,2,3]
            println(v[#v])   ;; err
        """)
        //assert(out == "anon : (lin 4, col 23) : index error : out of bounds\n") { out }
        assert(out == "core library : index error : out of bounds\n") { out }
    }
    @Test
    fun vector9_err() {
        val out = all("""
            set #v = 0
        """)
        assert(out == "anon : (lin 2, col 13) : invalid set : invalid destination") { out }
    }
    @Test
    fun todo_vector10_pop() {
        val out = all("""
            var v
            set v = #[1,2,3]
            var x
            set x = (set v[#v-1] = nil)
            println(x)
        """, true)
        assert(out == "3\n") { out }
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
            set t1 = #[]
            set t1[#t1] = 1
            var t2
            set t2 = t1
            var t3
            set t3 = copy(t1)
            set t1[#t1] = 2
            set t3[#t3] = 20
            println(t1)
            println(t2)
            println(t3)
        """, true)
        assert(out == "#[1,2]\n#[1,2]\n#[1,20]\n") { out }
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
            `puts(${D}v.Dyn->Vector.mem);`
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
        assert(out == "core library : set error : incompatible scopes\n") { out }
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
        assert(out == "core library : set error : incompatible scopes\n") { out }
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
                ;;println(tags(xxx))
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
        assert(out == "anon : (lin 1, col 1) : call error : expected function\n") { out }
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
                    println(tags(xxx))
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
                println(tags(xxx))
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
                    println(tags(xxx))
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
        assert(out == "anon : (lin 3, col 13) : call error : expected function\n") { out }
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
        assert(out == "anon : (lin 2, col 5) : throw error : uncaught exception\n") { out }
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
    fun catch4() {
        val out = all("""
            catch err==:x {
                throw([])
                println(9)
            }
            println(1)
        """.trimIndent())
        //assert(out == "anon : (lin 2, col 5) : throw error : expected tag\n") { out }
        assert(out == "anon : (lin 2, col 5) : throw error : uncaught exception\n") { out }
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
        assert(out == "anon : (lin 2, col 13) : set error : incompatible scopes\n") { out }
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
    fun catch10() {
        val out = ceu.all(
            """
            catch err[0]==:x {
                throw([:x])
                println(9)
            }
            println(err)
        """
        )
        assert(out == "nil\n") { out }
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
        assert(out == "anon : (lin 2, col 5) : throw error : uncaught exception\n") { out }
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
        assert(out == "1\n") { out }
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
            println((10 + -20*2)/5)
        """, true)
        assert(out == "-4\n") { out }
    }
    @Ignore
    fun todo_scope_op_id2() {
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
    fun op_assoc() {
        val out = all("""
            println(2 * 3 - 1)
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

    // TAGS

    @Test
    fun tag1() {
        val out = all("""
            var t
            set t = tags(1)
            println(t)
            println(tags(t))
            println(tags(tags(t)))
        """)
        assert(out == ":number\n:tag\n:tag\n") { out }
    }
    @Test
    @Ignore
    fun todo_tag2_err() {
        val out = all("""
            tags()
        """)
        assert(out == "-10\n") { out }
    }
    @Test
    @Ignore
    fun todo_tag3_err() {
        val out = all("""
            tags(1,2)
        """)
        assert(out == "-10\n") { out }
    }
    @Test
    fun tag2() {
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
    fun tag3() {
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
        assert(out == "anon : (lin 4, col 21) : throw error : uncaught exception\n") { out }
    }

    // ESCAPE / FUNC

    @Test
    fun todo_closure_esc1() {
        val out = all("""
            var g
            var f
            set f = func (v) {
                set g = func () {
                    println(v)
                }
            }
            println(f(10)())
        """)
        assert(out == "anon : (lin 5, col 21) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun todo_closure_esc2() {
        val out = all("""
            var f
            set f = func (v) {
                func () {
                    println(v)
                }
            }
            println(f(10)())
        """)
        //assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 3, col 30) : set error : incompatible scopes\n") { out }
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
    fun todo_closure() {    // TODO: solution: pass tuple that is compared on return
        val out = all("""
            var smallerc
            set smallerc = func (x) {
                func (y) {  ;; TODO: cannot return func that uses x in this block
                    if x { x } else { y }
                }
            }
            println(smallerc(3)(1))
        """)
        //assert(out == "3\n") { out }
        //assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 3, col 37) : set error : incompatible scopes\n") { out }
    }
}
