package tst_99

import dceu.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_99 {
    @Before
    fun init() {
        TEST = false
    }

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
        //assert(out == "\n") { out }
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
        //assert(out == "\n") { out }
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
        //assert(out == "\n") { out }
    }

    // AS

    @Test
    fun ab_01_yield() {
        val out = test("""
            val x = if (true) { 1 }
            println(x)
        """)
        assert(out == "1\n") { out }
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
            println(true and (not false))
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_02x_op_not() {
        val out = test("""
            println(not nil and not false)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_03_or_and() {
        val out = test("""
            println(1 or error(5))
            println(1 and 2)
            println(nil and 2)
            println(nil or 2)
        """)
        assert(out == "1\n2\nnil\n2\n") { out }
    }
    @Test
    fun bb_03_or_and_no() {
        val out = test("""
            println(1 or error(5))
            println(nil or 2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bb_03_or_and_ok() {
        val out = test("""
            println(
                (1 thus { ,ceu_6 =>
                    (if ceu_6 {
                       ceu_6
                    } else {
                        error(5)
                    })
                })
            )
            println((nil thus { ,ceu_41 =>
                (if ceu_41 {
                    ceu_41
                } else {
                    2
                })
            }))
        """)
        assert(out == "1\n2\n") { out }
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
    @Test
    fun bb_07_ops() {
        val out = test("""
            println({{or}}(false, true))
            println({{not}}(true))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun bb_08_ops() {
        val out = test("""
            println({{not}}())
        """)
        assert(out == "anon : (lin 2, col 21) : operation error : invalid number of arguments\n") { out }
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
                    sup?(v2,tag(v1))   => true
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
    @Test
    fun bc_02_in() {
        val out = test("""
            $PLUS
            func to-bool (v) {
                not (not v)
            }
            func in' (v, xs) {
                var i = 0
                loop {
                    break(false) if i == #xs
                    break(true) if v == xs[i]
                    set i = i + 1
                }
            }            
            func in-not' (v, xs) {
                not in'(v,xs)
            }
            val t = [1,2,3]
            println(2 in? t)
            println(4 in? t)
            println(2 in-not? t)
            println(4 in-not? t)
        """)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }
    @Test
    fun bc_03_is() {
        val out = test("""
            val t = []
            tag(:x,t)
            println(t is? :x)
            tag(:y,t)
            println(t is-not? :y)
            tag(nil,t)
            println(t is-not? :x)
        """, true)
        assert(out == "true\nfalse\ntrue\n") { out }
    }
    @Test
    fun bc_04_is() {
        val out = test("""
            println({{is?}}    (4, :nil))
            println({{is-not?}}(4, :nil))
        """, true)
        assert(out == "false\ntrue\n") { out }
    }

    // FUNC / DCL / :REC

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
    @Test
    fun cc_02_func() {
        val out = test("""
            $PLUS
            func f (v) {
                if v /= 0 {
                    println(v)
                    f(v - 1)
                }
            }
            f(3)
        """)
        assert(out == "3\n2\n1\n") { out }
    }
    @Test
    fun cc_03_func() {
        val out = test(
            """
            func f (x) {
                println(x)
            }
            f(10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_04_task() {
        val out = test(
            """
            task f (x) {
                println(x)
            }
            spawn f (10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_05_func_err() {
        val out = test(
            """
            func f {        ;; TODO: implicit it?
                println(x)
            }
        """
        )
        assert(out == "anon : (lin 2, col 20) : expected \"(\" : have \"{\"\n") { out }
        //assert(out == "anon : (lin 3, col 25) : access error : variable \"x\" is not declared") { out }
    }
    @Test
    fun cc_06_func_it() {
        val out = test(
            """
            func f {        ;; TODO: implicit it?
                println(it)
            }
            f(10)
        """
        )
        assert(out == "anon : (lin 2, col 20) : expected \"(\" : have \"{\"\n") { out }
        //assert(out == "10\n") { out }
    }

    // IF / ID-TAG

    @Test
    fun cj_01_if() {
        val out = test("""
            val v = if 1 { it }
            println(v)
        """)
        //assert(out == "1\n") { out }
        assert(out == "anon : (lin 2, col 28) : access error : variable \"it\" is not declared\n") { out }
    }
    @Test
    fun cj_02_if() {
        val out = test("""
            data :X = [x]
            val i = if [10] { ,v:X => v.x }
            println(i)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cj_03_ifs() {
        val out = test("""
            data :X = [x]
            val i = ifs {
                [10] { ,v:X => v.x }
            }
            println(i)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cj_04_ifs() {
        val out = test("""
            data :X = [x]
            val i = match nil {
                |[10] { ,v:X => v.x }
            }
            println(i)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cj_05_ifs() {
        val out = test("""
            match nil {
                |10 { ,v => println(v) }
            }
        """)
        assert(out == "10\n") { out }
    }

    // IF cnd => t => f

    @Test
    fun dd_01_if() {
        val out = test("""
            println(if false => 1 => 2)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun dd_02_if() {
        val out = test("""
            println(if true => 1 => 2)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun dd_03_if() {
        val out = test("""
            println(if true => if true => 1 => 99 => 99)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun TODO_dd_04_if_assign() {
        val out = test("""
            println(if x=10 => x => 99)
        """)
        assert(out == "10\n") { out }
    }

    // IT / HIDE

    @Test
    fun ee_01_it() {
        val out = test("""
            val it
            do {
                val it = 10
                println(it)     ;; dcl from last to first
            }            
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ee_02_it() {
        val out = test("""
            val it
            println(it)
            do {
                val it = 10
            }            
        """)
        //assert(out == "anon : (lin 5, col 21) : declaration error : variable \"it\" is already declared\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun ee_03_it() {
        val out = test("""
            val it
            do {
                val it = 10
            }            
            println(it)
        """)
        assert(out == "nil\n") { out }
        //assert(out == "anon : (lin 4, col 21) : declaration error : variable \"it\" is already declared\n") { out }
    }
    @Test
    fun ee_04_it() {
        val out = test("""
            val it = 10
            println(__it)
            do {
                val it = 99
            }            
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
            val x = match 20 {
                == 10 => false
                == 20 => true
                else  => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_03x_ifs() {
        val out = test("""
            val x = match 20 {
                == 20 => true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_04_ifs() {
        val out = test("""
            $IS
            var x = match 20 {
                == 10 => false
                (|true)  => true
                == 20 => false
                else  => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_05_ifs() {
        val out = test("""
            $COMP
            val x = match 20 {
                10 => false
                else     => true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_06_ifs_is() {
        val out = test("""
            $IS
            val t = :X []
            val x = match t {
                is? :Y   => false
                is? :X   => true
                else => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_07_ifs() {
        val out = test("""
            var x = match 20 {
                is? 10 => false
                (|true)  => true
                is? 20 => false
                else  => false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_08_ifs() {
        val out = test("""
            data :T = []
            val x = match 10 {
                (|true) => :T []
                is? 0 => nil
            }
            println(x)
        """)
        assert(out == "anon : (lin 5, col 21) : access error : variable \"is'\" is not declared\n") { out }
    }
    @Test
    fun ff_09_ifs() {
        val out = test("""
            var x = match 20 {
                in? [1,20,1] => true
                else  => false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_10_ifs() {
        val out = test("""
            $IS ; $COMP
            var x = match 20 {
                :no => false
                10  => false
                20  => true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_10x_ifs() {
        val out = test("""
            $IS ; $COMP
            match 20 {
                :no => println(:no)
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ff_11_ifs() {
        val out = test("""
            $IS ; $COMP
            data :T = [v]
            var x = match :T [20] {
                (t1:T| t1.v == 10) => false
                false     => false
                (t2:T| t2.v == 20) => true
                else      => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_12_ifs() {
        val out = test("""
            ifs {
                true => error()
            }
        """)
        assert(out == " |  anon : (lin 3, col 25) : error()\n" +
                " v  error : nil\n") { out }
    }

    // IFS / ORIGINAL

    @Test
    fun fg_01_ifs() {
        val out = test("""
            var x = match ;;;it=;;;20 {
                in-not? [1,1] => true
                else  => false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun fg_02_ifs() {
        val out = test("""
            val x = match [] {
                it|true => it
            }
            println(x)
        """, true)
        //assert(out == "anon : (lin 2, col 21) : block escape error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun fg_03_ifs() {
        val out = test("""
            val x = match [] {
                it|true => ;;;drop;;;(it)
            }
            println(x)
        """, true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun fg_04_ifs () {
        val out = test("""
            val x = ifs {
                true => it
            }
            println(x)
        """, true)
        assert(out == "anon : (lin 3, col 25) : access error : variable \"it\" is not declared\n") { out }
    }
    @Test
    fun TODO_fg_05_ifs () {
        val out = test("""
            val x = ifs {
                v=10 => v
            }
            println(x)
        """)
        assert(out == "anon : (lin 3, col 18) : expected \"{\" : have \"=\"\n") { out }
        //assert(out == "10\n") { out }
    }
    @Test
    fun fg_06_ifs () {
        val out = test("""
            val x = match false {
                and nil => true
            }
            println(x)
        """)
        //assert(out == "anon : (lin 3, col 17) : expected expression : have \"{\"") { out }
        //assert(out == "anon : (lin 3, col 17) : access error : variable \"{{and}}\" is not declared") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun fg_07_ifs () {
        val out = test("""
            val x = match 4 {
                is? nil => false
                in? [4] => true
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun fg_08_ifs () {
        val out = test("""
            and nil
        """)
        //assert(out == "anon : (lin 3, col 17) : expected expression : have \"{\"") { out }
        assert(out == "anon : (lin 2, col 13) : access error : variable \"{{and}}\" is not declared\n") { out }
    }
    @Test
    fun fg_09_ifs () {
        val out = test("""
            val x = match "oi" {
                {{string?}} { true }
                else => false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }

    // IFS / MULTI

    @Test
    fun fh_00_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match [10,20] {
                [10,20] => :ok
            }
            println(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fh_01_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match (10,20) {
                (30,40) => error(:no)
                (10,20) => :ok
            }
            println(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fh_02_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match 1 {
                (1,2) => error(:no)     ;; 2 compares to nil
                1 => :ok
            }
            println(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fh_02x_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match 1 {
                (1,nil) => (:ok)     ;; 2 compares to nil
                1 => error(:no)
            }
            println(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fh_03_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match (1,2) {
                1 => :ok
                (1,2) => error(:no)
            }
            println(x)
        """)
        assert(out == ":ok\n") { out }
    }

    // IFS / NO CATCH ALL

    @Test
    fun fi_01_ifs_no_catch_all() {
        val out = test("""
            $IS ; $COMP
            data :T = [v]
            var x = match [20] {
                false         => error()
                (t :T| false) {}
                ;;do (t :T)
                (|t.v == 10)  => error()
                (|t.v == 20)  => :ok
                else          => error()
            }
            println(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fi_02_ifs_no_catch_all() {
        val out = test("""
            $IS ; $COMP
            data :T = [v]
            var x = match [20] {
                false         => error()
                ;;(t :T| false) {}
                do (t :T)
                (|t.v == 10)  => error()
                (|t.v == 20)  => :ok
                else          => error()
            }
            println(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fi_03_ifs_no_catch_all() {
        val out = test("""
            $IS ; $COMP
            data :T = [v]
            var x = match [20] {
                false         => error()
                ;;(t :T| false) {}
                do (t :T) {
                    println(:ok)
                }
                (|t.v == 10)  => error()
                (|t.v == 20)  => :ok
                else          => error()
            }
            println(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fi_03x_ifs_no_catch_all() {
        val out = test("""
            $IS ; $COMP
            data :T = [v]
            var x = match :T [20] {
                false         => error()
                ;;(t :T| false) {}
                do (t :T) {
                    println(:ok)
                }
                (|t.v == 10)  => error()
                (|t.v == 20)  => :ok
                else          => error()
            }
            println(x)
        """)
        assert(out == ":ok\n:ok\n") { out }
    }
    @Test
    fun fi_04_ifs_no_catch_all() {
        val out = test("""
            ifs {
                do {
                    val v = 10
                    println(:1)
                }
                (v == 10) => println(:2)
            }
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun fi_05_ifs_no_catch_all() {
        val out = test("""
            val x = ifs {
                do {
                    val v = 10
                    println(:1)
                }
                (v == 10) => (:2)
            }
            println(x)
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun fi_07_ifs_no_catch_all() {
        val out = test("""
            $IS ; $COMP
            data :T = [v]
            var x = ifs {
                false => error()
                do {
                    val t :T = [20]
                    println(:ok)
                }
                (t.v == 10)  => error()
                (t.v == 20)  => :ok
                else          => error()
            }
            println(x)
        """)
        assert(out == ":ok\n:ok\n") { out }
    }

    // PATTS / TUPLES

    @Test
    fun REM_00() {
        val out = test("""
            
            x :X ==10 | ...
            x :X | ...
            x :X [] | ...
            
            match nil {
                [10]  => error()
            }
            
            match src {
                [v,x] = src
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_00() {
        val out = test("""
            match [1,2] {
                [x,y] => println(x,y)
            }
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun fj_01() {
        val out = test("""
            match [1,2] {
                [10,x]  => error()
                [1,2,3] => error()
                [1,2]   => println(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_02() {
        val out = test("""
            match [1,2] {
                [1] => println(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_03() {
        val out = test("""
            match [1,2] {
                [x] => println(x)
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun fj_04() {
        val out = test("""
            match [1,2] {
                [x,|false] => error()
                [|it==1,y] => println(y)
            }
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun fj_05() {
        val out = test("""
            match :X [] {
                :X [1] => error()
                :Y []  => error()
                :X []  => println(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_06() {
        val out = test("""
            match [1,[:x,:y],2] {
                [1,xy,3]    => error()
                [1,[x,y],2] => println(x,y)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_07_err() {
        val out = test("""
            $ASR
            match nil {
                [] => println(:ok)
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // PATTS / SET

    @Test
    fun fk_01() {
        val out = test("""
            var (x,y,z,w)
            set (x,y) = (1,2,3)
            set (z,w) = 4
            println(x,y,z,w)
        """)
        assert(out == "1\t2\t3\tnil\n") { out }
    }
    @Test
    fun fk_02() {
        val out = test("""
            var x
            set [x] = [10]
            println(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun fk_03() {
        val out = test("""
            var x
            set (x|true) = 1
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun fk_04() {
        val out = test("""
            var x
            set (x|false) = 1
            println(x)
        """)
        assert(out == "ERROR\n") { out }
    }
    @Test
    fun fk_05() {
        val out = test("""
            var x
            set [x] = 1
            println(x)
        """)
        assert(out == "ERROR\n") { out }
    }
    @Test
    fun fk_06() {
        val out = test("""
            var x
            set [x|x>0] = 1
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun fk_07() {
        val out = test("""
            var x
            set [x|x<0] = 1
            println(x)
        """)
        assert(out == "ERROR\n") { out }
    }

    // CATCH

    @Test
    fun gg_01_catch() {
        val out = test("""
            var x
            set x = catch :x {
                error([])
                println(9)
            }[0]
            println(x)
        """, true)
        assert(out == " |  anon : (lin 4, col 17) : error([])\n" +
                " v  error : []\n") { out }
    }
    @Test
    fun gg_02_catch() {
        val out = test("""
            func f (v) {
                false
            }
            catch |false {
                catch err|f(err) {
                    error([])
                }
            }
            println(`:number CEU_GC.free`)
            println(:ok)
        """)
        assert(out == " |  anon : (lin 7, col 21) : error([])\n" +
                " v  error : []\n") { out }
    }
    @Test
    fun gg_03_catch() {
        val out = test("""
            var x
            set x = catch :x {
                catch :2 {
                    error(tag(:x, [10]))
                    println(9)
                }
                println(9)
            }[0]
            println(:gc, `:number CEU_GC.free`) ;; error might be caught, so zero but no check
            println(:x, x)
        """, true)
        assert(out == ":gc\t5\n:x\t10\n") { out }
    }
    @Test
    fun gg_04_catch_err() {
        val out = test("""
            catch err|err==[] {
                var x
                set x = []
                error(x)
                println(9)
            }
            println(1)
        """, true)
        //assert(out == "anon : (lin 5, col 28) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 27) : block escape error : incompatible scopes\n" +
        //        "anon : (lin 5, col 17) : error(x)\n" +
        //        "error error : uncaught exception\n" +
        //        ":error\n") { out }
        assert(out == " |  anon : (lin 5, col 17) : error(x)\n" +
                " v  error : []\n") { out }
    }
    @Test
    fun gg_05_catch() {
        val out = test("""
            do {
                println(catch :x {
                    error(tag(:x,[10]))
                    println(9)
                })
            }
        """, true)
        assert(out == ":x [10]\n") { out }
    }
    @Test
    fun gg_06_catch() {
        val out = test("""
            catch |false {
                catch {
                    error([10])
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun gg_07_catch() {
        val out = test("""
            var x
            set x = catch :x {
                var y
                set y = catch |true {
                    error([10])
                    println(9)
                }
                ;;println(1)
                y
            }
            println(x)
        """.trimIndent(), true)
        //assert(out == "anon : (lin 9, col 5) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 18) : block escape error : incompatible scopes\n" +
        //        "anon : (lin 5, col 9) : error([10])\n" +
        //        "error error : uncaught exception\n" +
        //        ":error\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun gg_08_loop_() {
        val out = test("""
            println(catch :x { loop { error(tag(:x,[1])) }}[0])
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun gg_09_loop() {
        val out = test("""
            println(catch :x { loop { error(tag(:x,[1])) }}[0])
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun gg_10_loop_() {
        val out = test("""
            println(catch :2 { loop { error(tag(:2,[1])) }})
        """, true)
        assert(out == ":2 [1]\n") { out }
    }
    @Test
    fun gg_11_loop() {
        val out = test("""
            println(catch :x { loop {
                var x
                set x = [1] ;; memory released
                error(tag(:x,[1]))
            }}[0])
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun gg_12_loop_err() {
        val out = test("""
            println(catch :x { loop {
                var x
                set x = [1]
                error(tag(:x,x))
            }})
        """, true)
        assert(out == ":x [1]\n") { out }
        //assert(out == "anon : (lin 4, col 14) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 1, col 33) : block escape error : incompatible scopes\n" +
        //        "anon : (lin 4, col 5) : error(tag(x,:x,true))\n" +
        //        "error error : uncaught exception\n" +
        //        ":error\n") { out }
    }
    @Test
    fun gg_13_catch() {
        val out = test("""
            catch err|err===[] {
                error([])
                println(9)
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }

    // ENUM / TAGS / TEMPLATES

    @Test
    fun hh_01_enum() {
        val out = test("""
            do :antes
            enum {
                :x = `1000`,
                :y, :z,
                :a = `10`,
                :b, :c
            }
            do :meio
            enum {
                :i = `100`,     ;; ignored b/c of itr.i in to-iter-tuple
                :j,
            }
            do :depois
            val t = [:antes, :x, :y, :z, :a, :b, :c, :meio, :i, :j, :depois]
            loop (v,i) in to.iter(t) {
                set t[i] = to.number(v)
            }
            println(t)
        """, true)
        assert(out == "[35,1000,1001,1002,10,11,12,36,31,101,37]\n") { out }
    }

    //

    @Test
    fun hi_01_tags() {
        val out = test("""
            val x  = tag(:X,[])
            val xy = tag(:Y, tag(:X,[]))
            println(x, xy)
        """)
        //assert(out == ":X []\t[:Y,:X] []\n") { out }
        assert(out == ":X []\t:Y []\n") { out }
    }
    @Test
    fun hi_02_tags() {
        val out = test("""
            data :T = [x]
            val x = :T [1]
            println(x, tag(x))
        """)
        assert(out == ":T [1]\t:T\n") { out }
    }
    @Test
    fun hi_03_tags() {
        val out = test("""
            data :T = [x]
            val x = :T [1]
            val y = x.(:T).x
            println(y)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun hi_04_tags() {
        val out = test("""
            data :T = [x]
            val x = :T [1]
            println(x.x, tag(x))
        """)
        assert(out == "1\t:T\n") { out }
    }
    @Test
    fun hi_04x_tags() {
        val out = test("""
            data :T = [x]
            val (a,b) = (:T [1], :T [2])
            println(a.x, b.x)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun hi_05_tags() {
        val out = test("""
            data :T = [x]
            task T () :T {
                set pub = [10]
                yield()
            }
            val t :T = spawn T()
            println(t.pub.x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hi_06_tags() {
        val out = test("""
            data :T = [x]
            task T () :T {
                set pub = [10]
                yield()
            }
            val t = spawn T()
            println(t.(:T).pub)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun hi_07_tags() {
        val out = test("""
            data :T = [x]
            task T () :T {
                set ;;;task.;;;pub = [10]
                yield()
            }
            val t = spawn T()
            val p = do {
                t.pub
            }
            println(p)
        """)
        assert(out == "[10]\n") { out }
    }

    // DATA / HIER / TEMPLATE

    @Test
    fun hj_01_tplate() {
        val out = test("""
            data :T = [x,y]
            data :T.S = [z]
            val t :T = :T []
            var s :T.S
            set s = :T.S []
            println(t is? :T, t is? :T.S)
            println(s is? :T, s is? :T.S)
        """, true)
        assert(out == "true\tfalse\ntrue\ttrue\n") { out }
    }
    @Test
    fun hj_02_tplate() {
        val out = test("""
            data :U = [a]
            data :T = [x,y]
            data :T.S = [z:U]
            var s :T.S
            set s = :T.S [1,2,:U[3]]
            println(s is? :T, s.z is? :U)
            set s.z = :U [10]
            println(s is? :T.S, s.z is? :U)
        """, true)
        assert(out == "true\ttrue\ntrue\ttrue\n") { out }
    }
    @Test
    fun hj_03_tplate_nest() {
        val out = test("""
            data :T = [t] {
                :A = [a] {
                    :I = []
                    :J = [j]
                }
                :B = []
                :C = [] {
                    :Q = [q] {
                        :X = []
                        :Y = []
                    }
                }
            }
            val a :T.A   = :T.A [10,20]
            val b :T     = :T.B [30]
            val c :T.C.Q = :T.C.Q.Y [40,50]
            println(a.a, b.t, c.q)
            println(a is? :T, b is? :T.C, c is? :T.C.Q.Y)
        """, true)
        assert(out == "20\t30\t50\ntrue\tfalse\ttrue\n") { out }
    }
    @Test
    fun TODO_hj_04_tplate() {
        val out = test("""
            data :T = [x,y]
            val t :T = [x=1,y=2]    ;; TODO: syntax sugar
            set t.x = 3
            println(t)      ;; [x=3,y=2]
        """, true)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun hj_05_tplate_err() {
        val out = test("""
            data :T = [v]
            data :U = [t:T,X]
            var u :U = [[10]]
            println(u.X.v)
        """, true)
        //assert(out == "anon : (lin 5, col 25) : index error : field \"X\" is not a data") { out }
        assert(out == " |  anon : (lin 5, col 21) : u[:X]\n" +
                " v  index error : out of bounds\n") { out }
    }
    @Test
    fun hj_06_tplate_tup() {
        val out = test("""
            data :T = [v]
            val t :T = [[1,2,3]]
            println(t.v[1])
        """, true)
        assert(out == "2\n") { out }
    }
    @Test
    fun TODO_hj_07_tplate_nest() {
        val out = test("""
            data :T = [] {
                :A = [v] {
                    :x,:y,:z        ;; TODO: list of subtypes w/o data
                }
            }
            val x :T.A.x = :T.A.x [10]
            println(x)
            println(x.v)
            println(to-tag-string(":T.A.z"))
        """, true)
        assert(out == "20\t30\t50\ntrue\tfalse\ttrue\n") { out }
    }
    @Test
    fun hj_08_tplate_ifs() {
        val out = test("""
            data :T = [v]
            val v = ifs {
                t :T = [10] => t.v
            }
            println(v)
        """)
        //assert(out == "10\n") { out }
        assert(out == "anon : (lin 4, col 19) : expected \"{\" : have \":T\"\n") { out }
    }
    @Test
    fun hj_09_tplate_nest() {
        val out = test("""
            data :X = [v, t=[a,b]]
            val x :X = [10, [1,2]]
            println(x.v, x.t.a, x.t.b)
        """)
        assert(out == "10\t1\t2\n") { out }
    }
    @Test
    fun hj_10_tplate_nest() {
        val out = test("""
            data :X = [v, t=[a,z=[i,j]]]
            val x :X = [10, [:a,[1,2]]]
            println(x.v, x.t.a, x.t.z.j)
        """)
        assert(out == "10\t:a\t2\n") { out }
    }
    @Test
    fun hj_11_tplate_nest() {
        val out = test("""
            data :X = [v, t :T=[a,b]]
            val x :X = [10, [1,2]]
            val t :T = x.t
            println(t.a, t.b)
        """)
        assert(out == "1\t2\n") { out }
    }

    // THUS / SCOPE / :FLEET / :fleet

    @Test
    fun mm_01_tmp() {
        val out = test(
            """
            var x
            do {
                [1,2,3] thus { ,a =>
                    set x = a
                }
            }
            println(x)
        """
        )
        assert(out == "[1,2,3]\n") { out }
        //assert(out == " v  anon : (lin 5, col 25) : set error : cannot assign reference to outer scope\n") { out }
        //assert(out == " |  anon : (lin 4, col 30) : (func (a) { (set x = a) })([1,2,3])\n" +
        //        " v  anon : (lin 5, col 25) : set error : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_01_tmp_err() {
        val out = test(
            """
            var x
            do {
                [1,2,3] thus { ,a =>
                    set x = ;;;drop;;;(a)
                }
            }
            println(x)
        """
        )
        assert(out == "[1,2,3]\n") { out }
        //assert(out == "anon : (lin 5, col 34) : drop error : value is not movable\n") { out }
    }
    @Test
    fun mm_01_tmp_ok() {
        val out = test(
            """
            val x = do {
                [1,2,3] thus { ,a =>
                    a
                }
            }
            println(x)
        """
        )
        assert(out == "[1,2,3]\n") { out }
        //assert(out == "anon : (lin 5, col 25) : set error : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_02_thus_err() {
        val out = test("""
            var x
            nil thus { ,it =>
                set x = 10  ;; err
            }
            println(x)
        """)
        //assert(out == "anon : (lin 4, col 17) : set error : destination across thus\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_03_thus_err() {
        val out = test("""
            var x
            nil thus { ,it =>
                set x = it  ;; err
                println(x)
            }
        """)
        //assert(out == "anon : (lin 4, col 17) : set error : destination across thus\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun mm_04_tmp() {
        val out = test(
            """
            [0] thus { ,x =>
                set x[0] = []
                println(x)
            }
        """
        )
        assert(out == "[[]]\n") { out }
    }
    @Test
    fun mm_05_tmp() {
        val out = test("""
            val v = do {
                [] thus { ,x =>
                    if x { x } else { [] }
                }
            }
            println(v)
        """)
        //assert(out == "anon : (lin 3, col 20) : block escape error : cannot copy reference out\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun mm_05_tmp_x() {
        val out = test("""
            val v = do {
                [] thus { ,x =>
                    if x { ;;;drop;;;(x) } else { [] }
                }
            }
            println(v)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 4, col 33) : drop error : value is not movable\n") { out }
    }
    @Test
    fun mm_06_tmp_err() {
        val out = test("""
            val v = do {
                val x = []
                if x { x } else { [] }
            }
            println(v)
        """)
        //assert(out == " v  anon : (lin 2, col 21) : block escape error : cannot copy reference out\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun mm_07_and_or() {
        val out = test("""
            val t = func () { println(:t) ; true  }
            val f = func () { println(:f) ; false }
            println(${AND("t()", "f()")})
            println(${OR("t()", "f()")})
            println(${AND("[]", "false")})
            println(${OR("false", "[]")})
        """)
        assert(out == ":t\n:f\nfalse\n:t\ntrue\nfalse\n[]\n") { out }
    }
    @Test
    fun mm_08_fleet_tuple_func_err() {
        val out = test("""
            var f = func (v) {
                v[0] thus { ,it =>
                    println(it)
                }
            }
            var g = func (v) {
                val e = v
                f(e)
            }
            g([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun mm_09_yield_err() {
        val out = test("""
            resume (coro () {
                yield(nil) thus { ,it => set it = nil }
            }) ()
        """)
        assert(out == "anon : (lin 3, col 42) : set error : destination is immutable\n") { out }
    }
    @Test
    fun mm_10_yield_err() {
        val out = test("""
            resume (coroutine (coro () {
                yield(nil) thus { ,it => yield(nil) thus { ,x => nil } }
            })) ()
            println(:ok)
        """)
        //assert(out == "anon : (lin 3, col 41) : yield error : unexpected enclosing func\n") { out }
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 3, col 41) : yield error : unexpected enclosing thus\n") { out }
    }
    @Test
    fun mm_11_resume_yield() {
        val out = test("""
            $PLUS
            val CO = coro () {
                yield(nil) thus {, it => 
                    println(it)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_12_resume_yield() {
        val out = test("""
            val CO = coro (v1) {
                yield(v1) thus { , x => x }
            }
            val co = coroutine(CO)
            val v1 = resume co(10)
            val v2 = resume co(v1)
            println(v2)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_13_tags() {
        val out = test("""
            val CO = coro () {
                yield(nil) thus { , it =>
                    println(sup?(:X,tag(it))) ;; drop(it)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co(tag(:X,[]))
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun mm_14_yield_as() {
        val out = test("""
            val CO = coro () {
                yield(nil) thus {, v =>
                    println(v)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_15_yield_as() {
        val out = test("""
            coro () {
                yield(nil) thus { ,it :T =>
                    it[0]
                }
            }
            println(:ok)
        """)
        //assert(out == "anon : (lin 3, col 38) : declaration error : data :T is not declared\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun mm_16_scope() {
        val out = test("""
            val T = coro () {
                val v = yield(nil) thus { ,x => x }
                yield(nil) ;;thus { ,it => nil }
                println(v)                
            }
            val t = coroutine(T)
            resume t()
            do {
                do {
                    do {
                        val v = []
                        resume t(v)
                    }
                }
            }
            resume t()
        """)
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 11, col 24) : t(v)\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : resume (t)(v)\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 41) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " |  anon : (lin 3, col 41) : (func (x) { x })(yield(nil))\n" +
        //        " v  anon : (lin 3, col 41) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive alien reference\n") { out }
    }
    @Test
    fun mm_17_catch_yield_err() {
        val out = test("""
            coro () {
                catch (it| do {
                    yield(nil) thus {, it => nil }
                } )
                {
                    error(:e1)
                }
            }
        """)
        //assert(out == "anon : (lin 4, col 39) : declaration error : variable \"it\" is already declared\n") { out }
        assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing catch\n") { out }
    }
    @Test
    fun mm_17a_catch_yield_err() {
        val out = test("""
            coro () {
                catch (it| do {
                    do it
                    yield(nil) thus { ,it => nil }
                } )
                {
                    error(:e1)
                }
            }
        """)
        //assert(out == "anon : (lin 5, col 39) : declaration error : variable \"it\" is already declared\n") { out }
        assert(out == "anon : (lin 5, col 21) : yield error : unexpected enclosing catch\n") { out }
    }
    @Test
    fun mm_18_it() {
        val out = test("""
            val CO = coro () {
                yield(nil) thus { ,it =>
                    println(:it, it)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co()
        """,)
        assert(out == ":it\tnil\n") { out }
    }
    @Test
    fun mm_19_it() {
        val out = test("""
            val CO = coro () {
                yield(nil) thus { ,x =>
                    println(:it, x)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co([])
        """,)
        assert(out == ":it\t[]\n") { out }
    }
    @Test
    fun mm_20_it_err() {
        val out = test("""
            val CO = coro (x) {
                yield(nil) thus {, x =>
                    println(:it, x)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co([])
        """,)
        assert(out == "anon : (lin 3, col 36) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun mm_21_it_data() {
        val out = test("""
            data :X = [x]
            val CO = coro () {
                yield(nil) thus { ,x :X =>
                    println(:it, x.x)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co([10])
        """,)
        assert(out == ":it\t10\n") { out }
    }
    @Test
    fun mm_22_it_it_err() {
        val out = test("""
            val CO = coro () {
                yield(nil) thus { ,x =>
                    yield(nil) thus { ,x =>
                        x
                    }
                }
            }
        """,)
        //assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing yield\n") thus { out }
        assert(out == "anon : (lin 4, col 40) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun mm_23_scope() {
        val out = test("""
            val T = coro () {
                val v = yield(nil) thus { ,it => 
                    println(it)
                    10
                }
                yield(nil) ;;thus {, it => nil }
                println(v)                
            }
            val t = coroutine(T)
            resume t()
            do {
                val v = []
                resume t(v)
            }
            resume t()
        """)
        assert(out == "[]\n10\n") { out }
        //assert(out == " |  anon : (lin 14, col 17) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive alien reference\n") { out }
    }
    @Test
    fun mm_24_yield() {
        val out = test("""
            coro () {
                yield(nil) thus {, x =>
                    yield(nil) thus { ,y => nil }
                }
            }
            println(:ok)
        """)
        //assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing func\n") { out }
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing thus\n") { out }
    }
    @Test
    fun mm_25_gc_bcast() {
        DEBUG = true
        val out = test("""
            var tk = task () {
                yield(nil) thus {, it =>
                    do {
                        val xxx = it
                        nil
                    }
                }
                nil
                ;;println(:out)
            }
            var co = spawn tk ()
            broadcast ([])
            println(`:number CEU_GC.free`)
        """)
        //assert(out == "0\n") { out }
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 11, col 13) : broadcast []\n" +
        //        "anon : (lin 5, col 21) : declaration error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun mm_26_term() {
        val out = test("""
            spawn( task () {
                val t = spawn (task () {
                    yield(nil) ;;thus { ,it => nil }
                    10
                } )()
                yield (nil) thus {, it => println(it.pub) }
            } )()
            broadcast(nil)
            println(:ok)
       """)
        assert(out == "10\n:ok\n") { out }
    }
    @Test
    fun mm_27_bcast_err() {
        val out = test(
            """
            var T = task () {
                var v =
                yield(nil) thus { ,it => it}
                println(v)
            }
            var t = spawn T()
            ;;println(:1111)
            do {
                val a
                do {
                    val b
                    var e = []
                    broadcast (e)
                }
            }
            ;;println(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : broadcast e\n" +
        //        " v  anon : (lin 4, col 17) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == "anon : (lin 11, col 39) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " |  anon : (lin 4, col 33) : (func (it) { it })(yield(nil))\n" +
        //        " v  anon : (lin 4, col 33) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 33) : block escape error : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 35) : declaration error : cannot hold alien reference\n") { out }
    }
    @Test
    fun mm_28_data_await() {
        val out = test("""
            data :E = [x,y]
            spawn (task () {
                yield(nil) thus {, it :E =>
                    println(it.x)
                }
            } )()
            broadcast (tag(:E, [10,20]))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_29_data_await() {
        val out = test("""
            data :E = [x,y]
            data :F = [i,j]
            spawn (task () {
                yield(nil) thus { ,it :E =>
                    println(it.x)
                }
                yield(nil) thus { ,it :F =>
                    println(it.j)
                }
            } )()
            broadcast (tag(:E, [10,20]))
            broadcast (tag(:F, [10,20]))
        """)
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun mm_30_thus_yield() {
        val out = test(
            """
            val co = coroutine(coro (it) {
                yield()
                1 thus {
                    yield(it)
                }
            })
            resume co ()
            val v = resume co()
            println(v)
        """)
        assert(out == "1\n") { out }
    }

    // LOOP / ITER / NUMERIC FOR

    @Test
    fun fg_01_iter() {
        val out = test("""
            $PLUS
            func to-iter (v) { v }
            func f (t) {
                if t[1] == t[2] {
                    nil
                } else {
                    set t[1] = t[1] + 1
                    t[1]
                }
            }
            data :Iterator = [f,s,i]
            val it = :Iterator [f, 0, 5]
            loop v in it {
                until not v
                println(v)
            }
        """)
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun fg_02_iter() {
        val out = test("""
            $PLUS
            func to-iter (v) { v }
            func f (t) {
                if t[1] == t[2] {
                    nil
                } else {
                    set t[1] = t[1] + 1
                    t[1]
                }
            }
            data :Iterator = [f,s,i]
            val it = [f, 0, 5]
            loop v in it {
                until not v
                println(v)
                break if true
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun fg_03_iter() {
        val out = test("""
            task T () {
                await()
            }
            val ts = tasks()
            spawn T() in ts
            loop t in ts {
                println(t)
            }
        """, true)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun fh_02_num() {
        val out = test("""
            loop i in {0 => 1} {
                println(i)
            }
        """, true)
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun fg_05_dict_iter_nil() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop (_,v) in t {
                println(v)
            }
        """, true)
        assert(out == ":x\n:y\n:z\n") { out }
    }
    @Test
    fun fg_06_dict_iter_val() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop v in to-iter(t) {
                println(v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun fg_07_dict_iter_key() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop (_,k) in to-iter(t) {
                println(k)
            }
        """, true)
        assert(out == ":x\n:y\n:z\n") { out }
    }
    @Test
    fun fg_08_dict_iter_all() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop (v,k) in to-iter(t) {
                println(k,v)
            }
        """, true)
        assert(out == ":x\t1\n:y\t2\n:z\t3\n") { out }
    }
    @Test
    fun fg_09_dict_iter() {
        val out = test("""
            val t = @[]
            loop v in to-iter(t) {
                println(v)
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fg_10_vect_iter_nil() {
        val out = test("""
            loop v in #[10, 20, 30] {
                println(v)
            }
        """, true)
        assert(out == "10\n20\n30\n") { out }
    }
    @Test
    fun fg_11_vect_iter_val() {
        val out = test("""
            val t = #[10, 20, 30]
            loop (_,i) in to-iter(t) {
                println(i)
            }
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun fg_12_vect_iter_all() {
        val out = test("""
            loop (v,i) in to-iter(#[10, 20, 30]) {
                println(i,v)
            }
        """, true)
        assert(out == "0\t10\n1\t20\n2\t30\n") { out }
    }
    @Test
    fun fg_13_vect_iter_idx() {
        val out = test("""
            val t = #[1, 2, 3]
            loop (_,v) in to-iter(t,:idx) {
                println(v)
            }
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun fg_14_vect_iter_err() {
        val out = test("""
            val t = #[1, 2, 3]
            loop [i in to-iter(t) {
                println(i, v)
            }
        """, true)
        //assert(out == "anon : (lin 3, col 36) : expected \",\" : have \"{\"") { out }
        //assert(out == "anon : (lin 3, col 30) : expected identifier : have \"(\"") { out }
        assert(out == "anon : (lin 3, col 18) : expected \"in\" : have \"[\"\n") { out }
    }
    @Test
    fun fg_15_dict_iter() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop (v,k) in to-iter(t) {
                println(k, v)
            }
        """, true)
        assert(out == ":x\t1\n:y\t2\n:z\t3\n") { out }
    }
    @Test
    fun fg_16_string_concat() {
        val out = test("""
            val s = #[]
            s <++ #['1']
            s <++ #['2']
            s <++ #['3']
            println(s)
        """, true)
        assert(out == "123\n") { out }
    }
    @Test
    fun fg_17_concat() {
        val out = test("""
            func f (v) {
                set v[+] = 1
                v
            }
            func g () {
                f(#[])
            }
            println(g())
        """, true)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun fg_18_string() {
        val out = test("""
            val v = ""
            println(v)
            `printf(">%s<\n", ${D}v.Dyn->Vector.buf);`
        """)
        assert(out == "#[]\n><\n") { out }
    }
    @Test
    fun fg_19_tuple_size() {
        val out = test("""
            val t = [1, 2, 3]
            println(#t)
        """)
        assert(out == "3\n") { out }
    }
    @Test
    fun fg_20_tuple_iter() {
        val out = test("""
            val t = [1, 2, 3]
            loop (v,k) in to-iter(t) {
                println([k,v])
            }
        """, true)
        assert(out == "[0,1]\n[1,2]\n[2,3]\n") { out }
    }
    @Test
    fun fg_21_dict_iter_it() {
        val out = test("""
            loop in @[x=1, y=2, z=3] {
                println(it)
            }
        """, true)
        //assert(out == ":x\n:y\n:z\n") { out }
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun fg_22_tuple_iter_tag() {
        val out = test("""
            data :T = [v]
            val t = [[1], [2], [3]]
            loop v:T in to-iter(t) {
                println(v.v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun fg_23_loop_num() {
        val out = test("""
            loop i {
                println(i)
                until i == 3
            }
        """, true)
        assert(out == "0\n1\n2\n3\n") { out }
    }
    @Test
    fun fg_24_loop_num() {
        val out = test("""
            println(:0)
            loop a in }0 => 1} {
                println(a)
            }
            println(:1)
            loop b in }0 => 3{ {
                println(b)
            }
            println(:2)
            loop c in {0 => 4} :step +2 {
                println(c)
            }
            println(:3)
            loop d in }2 => 0} :step -1 {
                println(d)
            }
            println(:4)
            loop in {0 => -2{ :step -1 {
                println(:x)
            }
            println(:5)
            loop in {1 => 2} {
                println(:y)
            }
            println(:6)
        """, true)
        assert(out == ":0\n1\n:1\n1\n2\n:2\n0\n2\n4\n:3\n1\n0\n:4\n:x\n:x\n:5\n:y\n:y\n:6\n") { out }
    }
    @Test
    fun fg_25_loop_num_it() {
        val out = test("""
            loop in {0 => 1} {
                println(it)
            }
        """, true)
        assert(out == "0\n1\n") { out }
    }

    // LOOP / ITER / :ITERATOR

    @Test
    fun fx_01_iter() {
        val out = test("""
            func f (t) {
                set t[2] = t[2] or 0
                if t[2] == 5 {
                    nil
                } else {
                    set t[2] = t[2] + 1
                    t[2] - 1
                }
            }
            loop v in f {
                println(v)
            }
        """, true)
        assert(out == "0\n1\n2\n3\n4\n") { out }
    }
    @Test
    fun fx_02_iter_err() {
        val out = test("""
            loop v in nil {
                println(v)
            }
        """, true)
        //assert(out.contains("assertion error : expected :Iterator")) { out }
        assert(out.contains(" |  anon : (lin 2, col 23) : ceu_21966[:f]\n" +
                " v  index error : expected collection\n")) { out }
    }
    @Test
    fun fx_03_iter() {
        val out = test("""
            val y = loop x in to-iter([1,2,3]) {
            until x == 2 }
            println(y)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun fx_04_iter() {
        val out = test("""
            val y = loop x in [1,2,3] {
            until x == 4 }
            println(y)
        """, true)
        assert(out == "nil\n") { out }
    }
    @Test
    fun fx_05_iter_it() {
        val out = test("""
            val y = loop in to-iter([1,2,3]) {
            until it == 4 }
            println(y)
        """, true)
        assert(out == "nil\n") { out }
    }
    @Test
    fun fx_06_iter_it() {
        val out = test("""
            data :Iterator = [f,s,tp,i]
            func to-iter (v, tp) {
                :Iterator [v]
            }
            ;;export [f] {
                val cur = []
                func f () {
                    cur
                }
            ;;}
            loop in f {   ;; assigns f to local which confronts cur
                until true
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 13, col 33) : set error : incompatible scopes\n" +
        //        ":error\n") { out }
    }
    @Test
    fun fx_07_drop() {
        val out = test("""
            val F = func (x) {
                coro () {
                    yield(x)
                } --> \{
                    to-iter(it)
                }
            }
            do {
                val x = []
                val itr :Iterator = F(x)
                println(itr.f(itr))
            }
        """, true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun fx_08_drop() {
        val out = test("""
            func F () {
                coro () {
                    loop {
                        val pos = []
                        yield(;;;drop;;;(pos))
                    }
                } --> \{
                    to-iter(it)
                }
            }
            do {
                val x :Iterator = F()
                x.f(x) --> \{ }
                x.f(x)
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fx_09_iter_nil() {
        val out = test("""
            val t = [1,nil,3]
            loop v in t {
                println(v)
            }
        """, true)
        assert(out == "1\nnil\n3\n") { out }
    }
    @Test
    fun fx_10_eq() {
        val out = test("""
            val t1 = [1,nil,3]
            val t2 = [1,nil,4]
            println(t1 === t2)
        """, true)
        assert(out == "false\n") { out }
    }

    // ITER / DROP

    @Test
    fun fh_01_iter() {
        val out = test("""
            val t1 = [[1],[2],[3]]
            val t2 = #[]
            loop i in {0 => #t1{ {
                set t2[+] = ;;;drop;;;(t1[i])
            }
            println(t2)
            val t3 = #[]
            loop v in to-iter(t2) {
                set t3[+] = v
            }
            println(t3)
        """, true)
        assert(out == "#[[1],[2],[3]]\n" +
                "#[[1],[2],[3]]\n") { out }
    }
    @Test
    fun fh_01x_iter() {
        val out = test("""
            val t2 = #[[1],[2],[3]]
            val t3 = #[]
            loop (v) in to-iter(t2) {
                set t3[+] = v
            }
            println(t3)
        """, true)
        assert(out == "#[[1],[2],[3]]\n") { out }
    }
    @Test
    fun fh_01y_iter() {
        val out = test("""
            val t2 = [1,2,3]
            val t3 = #[]
            loop (v,i) in to-iter(t2) {
                ;;println(i, v)
                set t3[+] = v
            }
            println(t3)
        """, true)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun fh_01z_iter() {
        val out = test("""
            $PLUS
            func iter-tuple (itr) {
                val i = itr[3]
                if i == #itr[1] {
                    set itr[0] = nil
                    nil
                } else {
                    set itr[3] = i + 1
                    (i, itr[1][i])
                }
            }
            func to-iter (v) { v }
            val t2 = [1,2,3]
            val t3 = #[]
            data :Iterator = [f,s,i]
            loop (i,v) in [iter-tuple, t2, nil, 0] {
                ;;println(i, v)
                set t3[+] = v
            }
            println(t3)
        """, false)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun fh_02() {
        val out = test("""
            coro genFunc () {
                var v1 = [0,'a']
                yield(;;;drop;;;(v1))
                var v2 = [1,'b']
                yield(;;;drop;;;(v2))
            }
            loop v in genFunc {
                println(v)
            }
        """, true)
        assert(out == "[0,a]\n[1,b]\n") { out }
    }
    @Test
    fun fh_03() {
        val out = test("""
        val e = func () {nil}
        val f = func (v) {
            match v {
                |true => [e,v]
            }
        }
        val g = func () {
            val co = []
            f(;;;drop;;;(co))
        }
        val x = g()
        println(x)
        """, true)
        assert(out.contains("[func: 0x")) { out }
    }
    @Test
    fun fh_04_drop() {
        val out = test("""
            val F = func (x) {
                val co = coroutine (coro () {
                    yield(nil)
                    x
                })
                resume co()
                co --> \{
                    it
                }
            }
            do {
                val x = []
                val co = F(x)
                println(resume co())
            }
        """)
        assert(out == "[]\n") { out }
    }

    // ITER / NEXT

    @Test
    fun TODO_multi_fi_01_iter_next() {
        val out = test("""
            val itr :Iterator = to-iter([1,2,3,4])
            println(itr[0](itr))
            println(itr.f(itr))
            println(next(itr))
            println(itr->next())
        """, true)
        assert(out == "1\n2\n3\n4\n") { out }
    }
    @Test
    fun TODO_multi_fi_02_coro_next() {
        val out = test("""
            val co = coroutine <-- coro (v1) {
                val v2 = yield(v1)
                val v3 = yield(v2)
                v3
            }
            println(co->next(1))
            println(co->next(2))
            println(co->next(3))
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }

    // LOOP / RET / UNTIL

    @Test
    fun fi_01_ret() {
        val out = test("""
            println(loop i in {0 => 1} {
                do nil
            })
        """, true)
        assert(out == "false\n") { out }
    }
    @Test
    fun fi_02_ret() {
        val out = test("""
            println(loop i in {0 => 1} {
                until 10
            })
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun fi_03_ret() {
        val out = test("""
            val ts = tasks()
            spawn ((task(){yield()})()) in ts
            println(loop i in ts {
                do nil
            })
        """, true)
        assert(out == "nil\n") { out }
    }
    @Test
    fun fi_04_ret() {
        val out = test("""
            val ts = tasks()
            spawn ((task(){yield()})()) in ts
            println(loop i in ts {
                until true
            })
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun fi_05_loop() {
        val out = test("""
            $PLUS
            loop it {
                println(it)
                 until true
            }
        """)
        assert(out == "0\n") { out }
    }
    @Test
    fun fi_06_loop() {
        val out = test("""
            loop {
                until true
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fi_07_until() {
        val out = test("""
            println(loop {
            until 10 })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun fi_08_until() {
        val out = test("""
            var x = 0
            loop {
                set x = x + 1
                println(x)
            until x == 3
            }
            println(99)
        """, true)
        assert(out == "1\n2\n3\n99\n") { out }
    }
    @Test
    fun fi_09_until() {
        val out = test("""
            var x = 0
            val v = loop {
                set x = x + 1
                println(x)
                until x == 3
                until false
            }
            println(v)
        """, true)
        assert(out == "1\n2\n3\ntrue\n") { out }
    }
    @Test
    fun fi_10_until() {
        val out = test("""
            println(0)
            loop {
                println(1)
                until true
                println(2)
            }
            println(3)
        """)
        assert(out == "0\n1\n3\n") { out }
    }
    @Test
    fun fi_11_until() {
        val out = test("""
            println(0)
            var x = false
            loop {
                println(1)
                until x
                set x = true
                println(2)
            }
            println(3)
        """)
        assert(out == "0\n1\n2\n1\n3\n") { out }
    }
    @Test
    fun fi_12_until() {
        val out = test("""
            println(0)
            var x = false
            loop {
                println(1)
                until x
                set x = true
                println(2)
                until x
                println(3)
                until x
                println(4)
            }
            println(5)
        """)
        assert(out == "0\n1\n2\n5\n") { out }
    }
    @Test
    fun fi_13_until() {
        val out = test("""
            var x = 0
            loop {
                set x = x + 1
                println(x)
                until v = (x == 3)  ;; TODO: declare var on until?
                println(v)
            }
            println(99)
        """, true)
        //assert(out == "1\nfalse\n2\nfalse\n3\n99\n") { out }
        assert(out == "anon : (lin 6, col 25) : expected expression : have \"=\"\n") { out }
    }
    @Test
    fun fi_14_until() {
        val out = test("""
            var x = 5
            val f = func () {
                set x = x - 1
                if x>0 { x } else { nil }
            }
            loop {
                while v1=f()  ;; TODO: declare var on while?
                println(v1)
                while v2=f()  ;; TODO: declare var on while?
                println(v2)
            }
        """, true)
        assert(out == "anon : (lin 8, col 25) : expected expression : have \"=\"\n") { out }
        //assert(out == "4\n3\n2\n1\n") { out }
    }
    @Test
    fun fi_15_until() {
        val out = test("""
            val v = loop in {1=>10} {
            }
            println(v)
        """, true)
        //assert(out == "nil\n") { out }
        assert(out == "false\n") { out }
    }
    @Test
    fun fi_16_while() {
        val out = test("""
            val v = loop { while false do nil }
            println(v)
        """)
        assert(out == "true\n") { out }
    }

    // TASKS / ITER / DROP

    @Test
    fun fj_01_iter() {
        val out = test("""
            task T () {
                yield()
            }
            val ts = tasks()
            spawn T() in ts
            val x = loop t in ts {
                break(;;;drop;;;(t)) if true
            }
            println(x)
        """, true)
        //assert(out == (" v  anon : (lin 7, col 13) : declaration error : cannot copy reference out\n")) { out }
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun TODO_fj_02_iter() {     // copy x-task?
        val out = test("""
            task T () {
                yield()
            }
            val ts = tasks()
            spawn T() in ts
            val x = loop t in ts {
                break(copy(t)) if true
            }
            println(x)
        """, true)
        assert(out.contains("track: 0x")) { out }
    }

    // ITER / CORO

    @Test
    fun fk_01_iter_coro() {
        val out = test("""
            coro T () {
                yield(1)
                yield(2)
                yield(3)
                ;;nil
            }
            loop v in (T) {
                println(v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun fk_02_iter_coro() {
        val out = test("""
            coro T () {
                yield(1)
                yield(2)
                yield(3)
            }
            catch :x {
                loop i in coroutine(T) {
                    println(i)
                    error(:x)
                }
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun fk_03_iter() {
        val out = test("""
            coro T () {
                yield(1)
                yield(2)
                yield(3)
            }
            loop i in T {
                println(i)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun fk_04_iter_ok() {
        val out = test("""
            coro T () {
                yield(1)
                yield(2)
                3
            }
            loop i in coroutine(T) {
                println(i)
            }
        """, true)
        //assert(out == "anon : (lin 12, col 57) : resume error : expected yielded task\n1\n2\n3\n:error\n") { out }
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun fk_05_iter() {
        val out = test("""
            coro T () {
                yield(1)
                yield(2)
                nil
            }
            loop i in to-iter(coroutine(T)) {
                println(i)
            }
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun fk_06_iter() {
        val out = test("""
            coro T () {
                yield(1)
                yield(2)
                yield(3)
            }
            println(to.vector(coroutine(T)))
        """, true)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun fk_07_iter() {
        val out = test("""
            coro T () {
                yield(1)
                yield(2)
                3
            }
            val co = coroutine(T)
            println(resume co())
            println(resume co())
            println(resume co())
            println(resume co())
        """, true)
        //assert(out == "anon : (lin 11, col 21) : resume error : expected yielded coro\n1\n2\n3\n:error\n") { out }
        assert(out == "1\n" +
                "2\n" +
                "3\n" +
                " |  anon : (lin 11, col 21) : (resume (co)())\n" +
                " v  resume error : expected yielded coro\n") { out }
    }

    // AS / YIELD / CATCH / DETRACK / THUS

    @Test
    fun gg_01_yield() {
        val out = test("""
            val CO = coro () {
                yield() thus {
                    println(it)
                }
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            resume co1()
            resume co2()
            resume co1(1)
            resume co2(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun gg_02_yield() {
        val out = test("""
            val T = task (v) {
                yield()
                println(v)
            }
            val t1 = spawn T(1)
            val t2 = spawn T(2)
            broadcast(nil)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun gg_03_yield() {
        val out = test("""
            val CO = coro () {
                val x = yield()
                println(x)
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            resume co1()
            resume co2()
            resume co1(1)
            resume co2(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun gg_04_yield() {
        val out = test("""
            val CO = coro () {
                do nil
                yield() thus { ,it => println(it);it }
                do nil
                nil
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            do { do { do {
            resume co1()
            resume co2()
            resume co1([])
            resume co2([])
            }}}
        """)
        //assert(out == " |  anon : (lin 13, col 13) : resume (co1)([])\n" +
        //        " v  anon : (lin 5, col 17) : block escape error : cannot move pending reference in\n") { out }
        assert(out == "[]\n[]\n")
    }
    @Test
    fun gg_05_yield() {
        val out = test("""
            val CO = coro () {
                do nil
                yield() thus {}
                nil
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            resume co1()
            resume co2()
            resume co1([])
            resume co2([])
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun gg_06_detrack() {
        val out = test("""
            val T = task () {
                set pub = [10]
                yield(nil)
            }
            var t = spawn T ()
            ;;var x = track(t)
            ;;;detrack(x);;; do { println(:1) }
            broadcast( nil )
            ;;detrack(x) { println(999) }
            println(status(t))
            println(:2)
        """)
        assert(out == ":1\n:terminated\n:2\n") { out }
    }

    // RESUME-YIELD-ALL

    @Test
    fun hh_01_yieldall() {
        val out = test("""
            coro foo () {
                yield('a')
                yield('b')
            }
            coro bar () {
                yield('x')
                resume-yield-all (coroutine(foo)) ()
                yield('y')
            }
            val co = coroutine(bar)
            loop {
                val v = resume co()
                break if status(co) == :terminated
                print(v)
            }
            println()
        """)
        assert(out == "xaby\n") { out }
    }
    @Test
    fun hh_02_yieldall() {
        val out = test("""
            $PLUS
            coro foo (x4) {
                val x6 = yield(x4+1)
                val x8 = yield(x6+1)
                x8
            }
            coro bar (x1) {
                val x3 = yield(x1+1)
                val x8 = resume-yield-all coroutine(foo) (x3+1)
                ;;println(:x8, x8, x8+1)
                val x10 = yield(x8+1)
                nil
            }
            val co = coroutine(bar)
            val x2 = resume co(1)
            ;;println(:x2, x2)
            val x5 = resume co(x2+1)
            ;;println(:x5, x5)
            val x7 = resume co(x5+1)
            ;;println(:x7, x7)
            val x9 = resume co(x7+1)
            ;;println(:x9, x9)
            val xN = resume co(x9+1)
            ;;println(:xN, xN)
            println(x2, x5, x7, x9, xN)
        """)
        assert(out == "2\t5\t7\t9\tnil\n") { out }
    }
    @Test
    fun hh_03_yieldall() {
        val out = test("""
            coro foo () {
                yield("a")
                yield("b")
            }
            coro bar () {
                yield("x")
                resume-yield-all (coroutine(foo)) ()
                yield("y")
            }
            val co = coroutine(bar)
            loop {
                val v = resume co()
                break if status(co) == :terminated
                print(v)
            }
            println()
        """)
        assert(out == "xaby\n") { out }
    }

    // SPAWN

    @Test
    fun ii_01_spawn_task() {
        val out = test("""
            spawn {
                println(1)
                yield()
                println(3)
            }
            println(2)
            broadcast(nil)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun TODO_ii_02_spawn_coro() {
        val out = test("""
            val co = coroutine (coro () {   ;; spawn coro
                println(1)
                yield()
                println(3)
            })
            resume co()
            println(2)
            resume co()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun ii_03_spawn_coro() {
        val out = test("""
            val co = coroutine(coro () {   ;; spawn coro
                println(1)
                yield()
                println(3)
            })
            resume co()
            println(2)
            resume co()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun ii_04_spawn() {
        val out = test("""
            spawn {
                spawn {
                    println(1)
                }
                nil
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ii_05_spawn_coro() {
        val out = test("""
            val co = coroutine (coro () {   ;; spawn coro
                println(1)
                val v = yield()
                println(v)
            })
            resume co()
            resume co(10)
        """)
        assert(out == "1\n10\n") { out }
    }
    @Test
    fun ii_06_spawn_defer() {
        val out = test("""
            spawn {
                do {
                    val t1 = spawn {
                        ${AWAIT()}
                        println(1)
                    }
                    spawn {
                        defer { println(3) }
                        ${AWAIT()}
                        println(2)
                    }
                    ${AWAIT("it==t1")}
                    nil
                }
                println(:ok)
            }
            broadcast( nil)
        """)
        assert(out == "1\n3\n:ok\n") { out }
    }
    @Test
    fun ii_07_spawn() {
        val out = test("""
            spawn {
                spawn {
                    yield ()
                    println(1)
                }
                yield ()
                println(2)
            }
            broadcast (nil)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun ii_08_par1() {
        val out = test("""
            spawn task () {
                par {
                    do { var ok1; set ok1=true;
                        loop {
                            until not ok1
                            val evt = yield(nil);
                            if type(evt)/=:exe-task {
                                set ok1=false
                            } else { nil }
                        } 
                    }
                    ;;yield()
                    do { var ok2; set ok2=true; loop { until not ok2 ; val evt=yield(nil); if type(evt)/=:exe-task { set ok2=false } else { nil } } }
                    ;;yield()
                    println(1)
                } with {
                    do { var ok3; set ok3=true; loop { until not ok3 ; val evt=yield(nil); if type(evt)/=:exe-task { set ok3=false } else { nil } } }
                    ;;yield()
                    println(2)
                } with {
                    println(3)
                }
            } ()
            broadcast( nil )
        """, true)
        assert(out == "3\n2\n") { out }
    }
    @Test
    fun ii_09_spawn() {
        val out = test("""
            task T () {}
            (spawn T() in ts) where {
            }
        """)
        assert(out == "anon : (lin 3, col 27) : access error : variable \"ts\" is not declared\n") { out }
    }

    // SPAWN / NESTED

    @Test
    fun TODO_ij_01_nested() {
        val out = test("""
            task :nested () {
                nil
            }
            println(:ok)
        """)
        //assert(out == "anon : (lin 2, col 13) : task :nested error : expected enclosing spawn\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun TODO_ij_02_nested() {
        val out = test("""
            val t = spawn (task :nested () {
                nil
            })()
            println(type(t))
        """)
        //assert(out == "anon : (lin 2, col 21) : spawn task :nested error : expected immediate enclosing block\n") { out }
        assert(out == ":exe-task\n") { out }
    }
    @Test
    fun TODO_ij_03_nested() {
        val out = test("""
            do {
                spawn (task :nested () {
                    nil
                })()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : spawn task :nested error : cannot escape enclosing block\n") { out }
    }
    @Test
    fun ij_04_nested() {
        val out = test("""
            do {
                spawn (task :nested () {
                    println(:ok)
                })()
                nil
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ij_05_task_pub_fake() {
        val out = test("""
            task T () {
                set ;;;task.;;;pub = 10
                println(;;;task.;;;pub)
                spawn {
                    println(;;;task.;;;pub)
                    await (|false)
                }
                nil
            }
            spawn T()
            broadcast (nil) in :global
        """, true)
        assert(out == "10\n10\n") { out }
    }


    // PAR / PAR-AND / PAR-OR

    @Test
    fun jj_01_par_err() {
        val out = test("""
            par {
                println(1)
            } with {
                println(2)
            }
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing coro or task\n") { out }
    }
    @Test
    fun jj_02_par() {
        val out = test("""
            spawn {
                par {
                    println(1)
                } with {
                    println(2)
                }
                println(999)
            }
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun jj_03_paror() {
        val out = test("""
            spawn {
                par-or {
                    yield()
                    yield()
                    println(1)
                } with {
                    println(2)
                } with {
                    yield()
                    yield()
                    println(3)
                }
                println(:ok)
            }
        """)
        assert(out == "2\n:ok\n") { out }
    }
    @Test
    fun jj_04_paror() {
        val out = test("""
            spawn {
                val v =
                    par-or {
                        1
                    } with {
                        2
                    }
                println(v)
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_05_parand() {
        val out = test("""
            spawn {
                par-and {
                    println(1)
                } with {
                    println(2)
                }
                println(:ok)
            }
        """)
        assert(out == "1\n2\n:ok\n") { out }
    }
    @Test
    fun jj_06_parand() {
        val out = test("""
            spawn {
                val v =
                    par-and {
                        1
                    } with {
                        2
                    }
                println(v)
            }
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun jj_07_paror() {
        val out = test("""
            spawn {
                par-or {
                    yield()
                    yield()
                    yield()
                    println(1)
                } with {
                    yield()
                    println(2)
                } with {
                    yield()
                    yield()
                    println(3)
                }
                println(:ok)
            }
            broadcast(nil)
        """)
        assert(out == "2\n:ok\n") { out }
    }
    @Test
    fun jj_08_parand() {
        val out = test("""
            spawn {
                par-and {
                    yield()
                    yield()
                    yield()
                    println(1)
                } with {
                    yield()
                    println(2)
                } with {
                    yield()
                    yield()
                    println(3)
                }
                println(:ok)
            }
            broadcast(nil)
            broadcast(nil)
            broadcast(nil)
        """)
        assert(out == "2\n3\n1\n:ok\n") { out }
        //assert(out == "2\n1\n3\n:ok\n") { out }
    }
    @Test
    fun jj_08a_parand() {
        val out = test("""
            spawn {
                par-and {
                    yield()
                    yield()
                    yield()
                    println(1)
                } with {
                    yield()
                    println(2)
                } with {
                    yield()
                    yield()
                    println(3)
                }
                println(:ok)
            }
            broadcast(nil)
            broadcast(nil)
            broadcast(nil)
        """)
        assert(out == "2\n3\n1\n:ok\n") { out }
    }
    @Test
    fun jj_09_paror_defer() {
        val out = test("""
            spawn {
                par-or {
                    ${AWAIT()}
                    println(1)
                } with {
                    defer { println(3) }
                    ${AWAIT()}
                    println(2)
                }
                println(:ok)
            }
            broadcast(nil)
        """)
        assert(out == "1\n3\n:ok\n") { out }
    }
    @Test
    fun jj_10_paror_defer() {
        val out = test("""
            spawn {
                par-or {
                    defer { println(3) }
                    ${AWAIT()}
                    println(999)
                } with {
                    println(2)
                }
                println(:ok)
            }
            broadcast (nil)
        """)
        assert(out == "2\n3\n:ok\n") { out }
    }
    @Test
    fun jj_10x_paror_defer() {
        val out = test("""
            spawn {
                par-and {
                    func (it) {
                        false
                    } (yield(nil))
                    println(999)
                } with {
                    nil
                }
                nil
            }
            broadcast (nil)
        """)
        assert(out == "999\n") { out }
    }
    @Test
    fun jj_11_paror_defer() {
        val out = test("""
            spawn {
                par-or {
                    defer { println(1) }
                    ${AWAIT()}
                    ${AWAIT()}
                    println(999)
                } with {
                    ${AWAIT()}
                    println(2)
                } with {
                    defer { println(3) }
                    ${AWAIT()}
                    ${AWAIT()}
                    println(999)
                }
                println(999)
            }
            broadcast (nil)
        """)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun jj_11_parand_defer() {
        val out = test("""
            spawn {
                par-and {
                    yield()
                    println(1)
                } with {
                    println(2)
                } with {
                    yield()
                    println(3)
                }
                println(:ok)
            }
            broadcast (nil)
        """)
        assert(out == "2\n1\n3\n:ok\n") { out }
    }
    @Test
    fun jj_12_parand_defer() {
        val out = test("""
            spawn {
                par-and {
                    defer { println(1) }
                    ${AWAIT()}
                    ${AWAIT()}
                    println(1)
                } with {
                    ${AWAIT()}
                    println(2)
                } with {
                    defer { println(3) }
                    ${AWAIT()}
                    ${AWAIT()}
                    println(3)
                }
                println(:ok)
            }
            broadcast (nil)
            broadcast (nil)
        """)
        assert(out == "2\n1\n1\n3\n3\n:ok\n") { out }
    }
    @Test
    fun jj_13_paror_dyn() {
        val out = test("""
            spawn {
                par-or {
                    yield()
                    yield()
                } with {
                    yield()
                    yield()
                }
            }
            do {
                val now
                broadcast([])
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_14_paror_dyn() {
        val out = test("""
            spawn (task () {
                par-or {
                    yield()
                } with {
                    yield()
                }
            }) ()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_15_paror_dyn() {
        val out = test("""
            spawn {
                par-or {
                    yield()
                } with {
                    yield()
                }
            }
            do {
                val now
                broadcast([])
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun BUG_jj_16_par_bcast() {     // bcast in outer of :nested
        val out = test("""
            spawn {
                par-or {
                    val e = yield()
                    println(e)
                } with {
                    broadcast(:1)
                }
            }
            println(:2)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_17_par_tasks() {
        val out = test("""
            spawn task () {
                ^[9,29]yield(nil)                                          
            }()                                                       
            spawn task () {                                           
                ^[9,29]yield(nil)                       
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_18_paror_ret() {
        val out = test("""
            spawn {
                val x = par-or {
                    1
                } with {
                    2
                }
                println(x)
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun BUG_jj_19_parand_ret() {
        val out = test("""
            spawn {
                val x = par-and {
                    1
                } with {
                    2
                }
                println(x)
            }
        """, true)
        assert(out == "2\n") { out }
    }
    @Test
    fun jj_20_paror_ret_func() {
        val out = test("""
            spawn {
                task f () {
                    par-or {
                        1
                    } with {
                        999
                    }
                }
                val x = await spawn f()
                println(x)
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_21_paror_ret_func() {
        val out = test("""
            task T () {
                await(:x)
            }
            spawn {
                par-or {
                    await spawn T()
                } with {
                    await spawn T()
                }
            }
            broadcast (:x) in :global
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_22_paror() {
        val out = test("""
            spawn {
                par-or {
                    await(| true)
                } with {
                    await(| true)
                }
            }
            broadcast (true) in :global
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_23_paror() {
        val out = test("""
            spawn {
                par-or {
                    await (|true)
                } with {
                    await (|true)
                }
            }
            do {
                broadcast (tag(:frame, [40])) in :global 
                broadcast (tag(:draw, [])) in :global
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_24_parand_immediate() {
        val out = test("""
            spawn task () {
                par-and {
                    println(1)
                } with {
                    println(2)
                }
                println(999)
            } ()
        """, true)
        assert(out == "1\n2\n999\n") { out }
    }
    @Test
    fun jj_25_paror_valgrind() {
        val out = test("""
            spawn {
                par-or {
                    loop { yield(nil) }
                } with {
                    par-or {
                        yield()
                    } with {
                        loop { yield(nil) }
                    }
                }
            }
            broadcast (nil) in :global
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_26_await_track() {
        val out = test("""
            task T () {
                yield()
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            spawn {
                par-and {
                    println(:0)
                    await (|it==x)
                    println(:2)
                } with {
                    println(:1)
                    broadcast (nil) in t
                }
                println(:3)
            }
            println(:4)
        """, true)
        assert(out == ":0\n:1\n:2\n:3\n:4\n") { out }
    }
    @Test
    fun jj_27_await_track() {
        val out = test("""
            task T () {
                yield()
            }
            val t = spawn T()
            val x = ;;;track;;;(t)
            spawn {
                par-and {
                    println(:0)
                    await (|it==x)
                    println(:2)
                } with {
                    println(:1)
                    broadcast (nil) in t
                }
                println(:3)
            }
            println(:4)
        """, true)
        assert(out == ":0\n:1\n:2\n:3\n:4\n") { out }
    }

    // AWAIT

    @Test
    fun kk_01_await() {
        val out = test("""
            $IS
            task T () {
                await(it| it is? :x)
                println(1)
            }
            spawn T()
            broadcast (tag(:x,[]))
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun kk_02_await() {
        val out = test("""
            $IS
            spawn {
                println(0)
                await ( |(it/=nil) and (it[:type]==:x) )
                println(99)
            }
            do {
                println(1)
                broadcast (@[(:type,:y)])
                println(2)
                broadcast (@[(:type,:x)])
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun kk_03_await() {
        val out = test("""
            $IS
            data :x = []
            spawn {
                println(0)
                await(:x)
                println(99)
            }
            do {
                println(1)
                broadcast (tag(:y, []))
                println(2)
                broadcast (tag(:x, []))
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun kk_04_await() {
        val out = test("""
            $IS
            data :x = []
            spawn {
                println(0)
                await(:x)
                println(99)
            }
            do {
                println(1)
                broadcast (tag(:y, []))
                println(2)
                broadcast (tag(:x, []))
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun kk_05_await() {
        val out = test("""
            val f
            await()
        """)
        assert(out == "anon : (lin 3, col 13) : yield error : expected enclosing coro or task\n") { out }
    }
    @Test
    fun kk_06_await() {
        val out = test("""
            spawn {
                loop {
                    await (|true) {
                        println(it)
                    }
                }
            }
            broadcast (@[])
        """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun kk_06x_await() {
        val out = test("""
            spawn {
                loop {
                    await {
                        println(it)
                    }
                }
            }
            broadcast (@[])
        """)
        //assert(out == "anon : (lin 4, col 27) : expected expression : have \"{\"\n") { out }
        assert(out == "@[]\n") { out }
    }
    @Test
    fun kk_07_await() {
        val out = test("""
            spawn {
                await (|true) {
                    println(it)
                }
                await (|true) {
                    println(it)
                }
            }
            broadcast (:1)
            broadcast (:2)
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun kk_08_await() {
        val out = test("""
            $COMP
            spawn {
                await (2)
                println(2)
                await (==1)
                println(1)
            }
            broadcast (1)
            broadcast (2)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun kk_09_await_it() {
        val out = test("""
            $IS
            data :X = []
            spawn {
                await :X {
                    nil thus {
                        println(it)
                    }
                }
            }
            broadcast(:X [])
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun kk_10_await_escape() {
        val out = test("""
            $IS
            spawn {
                println(await())
            }
            do {
                val e = []
                broadcast(e)
            }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun kk_11_await_thus_yield() {
        val out = test("""
            $IS
            spawn {
                await(:X) {
                    yield()
                }
            }
            do {
                val e = :X []
                broadcast(e)
            }
            println(:ok)
        """)
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 27) : argument error : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 5, col 21) : yield error : unexpected enclosing func\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun kk_12_await_detrack() {
        val out = test("""
            val t = spawn {
                yield()
            }
            val x = ;;;track;;;(t)
            spawn {
                await(==x)
                println(:1)
            }
            broadcast(nil)
            println(:2)
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun kk_13_await_drag() {
        val out = test("""
            $IS
            spawn {
                val click = await(:X) {
                    println(:it, it)
                    it
                }
                println(:click, click)
            }            
            broadcast(:X [1,2])
            println(nil)
        """)
        assert(out == ":it\t:X [1,2]\n" +
                ":click\t:X [1,2]\n" +
                "nil\n") { out }
    }
    @Test
    fun kk_14_await_data() {
        val out = test("""
            $IS
            data :T = [v]
            spawn {
                await :T {
                    println(it.v)
                }
            }
            broadcast(:T [:ok])
        """)
        assert(out == ":ok\n") { out }
    }

    // AWAIT / EVT / TEMPLATE / DATA

    @Test
    fun ka_01_await_data() {
        val out = test("""
            data :E = [x,y]
            spawn {
                await :E {
                    println(it.x)
                }
            }
            broadcast (:E [10,20]) in :global
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ka_02_await_data() {
        val out = test("""
            data :E = [x,y]
            spawn {
                await :E| it.y==20 {
                    println(it.x)
                }
            }
            broadcast (:E [10,10]) in :global 
            println(:mid)
            broadcast (:E [10,20]) in :global
        """, true)
        assert(out == ":mid\n10\n") { out }
    }
    @Test
    fun ka_03_await_data() {
        val out = test("""
            data :E = [x,y]
            data :F = [i,j]
            spawn {
                await :E| it.y==20 {
                    println(it.x)
                }
                await :F| it.i==10 {
                    println(it.j)
                }
            }
            broadcast(:E [10,20]) in :global 
            broadcast(:F [10,20]) in :global
        """, true)
        assert(out == "10\n20\n") { out }
    }

    // AWAIT / TASK

    @Test
    fun kl_01_await_task() {
        val out = test("""
            task T (v) {
                [v]
            }
            spawn {
                val v = await spawn T(1)
                println(v)
            }
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun kl_02_await_task() {
        val out = test("""
            spawn {
                task T () {
                    val v = await()
                    [v]
                }
                spawn {
                    val v = await spawn T(1)
                    println(v)
                }
                broadcast(2)
            }
        """)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun kl_03_await_task() {
        val out = test("""
            spawn {
                val t = spawn {
                    println(:1)
                }
                await(|it==t)
                println(:2)
            }
            println(:3)
        """, true)
        //assert(out == ":1\n:2\n:3\n") { out }
        assert(out == ":1\n:3\n") { out }
    }

    // EVERY

    @Test
    fun km_01_every() {
        val out = test(
            """
            $IS
            task T () {
                println(:1)
                every (|true) {
                    until true
                    error(999)
                }
                println(:2)
            }
            spawn T()
            broadcast (nil)
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun km_02_every() {
        val out = test(
            """
            $IS
            task T () {
                println(:1)
                every (|true) {
                    until false
                    println(:xxx)
                }
                println(:2)
            }
            spawn T()
            broadcast (nil)
        """)
        assert(out == ":1\n:xxx\n") { out }
    }
    @Test
    fun km_03_every() {
        val out = test("""
            $IS
            data :X = []
            spawn {
                par {
                    every :X {
                    }
                } with {
                    ;;every false { }
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun km_04_every() {
        val out = test("""
            $IS
            spawn {
                every (|true) {
                    yield()
                }
            }
            do {
                val e = :X []
                broadcast(e)
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 5, col 21) : yield error : unexpected enclosing func\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : broadcast'(e,:task)\n" +
        //        " v  anon : (lin 4, col 28) : argument error : cannot copy reference out\n") { out }
        //assert(out == ":ok\n") { out }
    }
    @Test
    fun km_05_every() {
        val out = test("""
            $IS
            spawn {
                var rect = []
                spawn {
                    every :X {
                        do rect
                    }
                }
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun km_05x_every() {
        val out = test("""
            spawn (task () {
                var rect = []
                spawn (task :nested () {
                    println(rect)
                }) ()
            }) ()
            println(:ok)
        """)
        assert(out == "[]\n:ok\n") { out }
    }
    @Test
    fun km_06_every_data() {
        val out = test("""
            $IS
            data :T = [v]
            spawn {
                every :T {
                    println(it.v)
                }
            }
            broadcast(:T [:ok])
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun km_07_every() {
        val out = test("""
            spawn {
                println(0)
                every :x {
                    println(it[0])
                }
            }
            do {
                println(1)
                broadcast (tag(:x, [10])) in :global 
                println(2)
                broadcast (tag(:y, [20])) in :global
                println(3)
                broadcast (tag(:x, [30])) in :global
                println(4)
            }
        """, true)
        assert(out == "0\n1\n10\n2\n3\n30\n4\n") { out }
    }
    @Test
    fun km_08_every_clk() {
        val out = test("""
            spawn task () {
                every <10:s> {
                    println(10)
                }
            }()
            println(0)
            broadcast (tag(:Clock, [5000])) in :global 
            println(1)
            broadcast (tag(:Clock, [5000]))
            println(2)
            broadcast (tag(:Clock, [10000])) in :global 
            println(3)
        """, true)
        assert(out == "0\n1\n10\n2\n10\n3\n") { out }
    }
    @Test
    fun TODO_km_09_every_clk_multi() { // awake twice from single bcast
        val out = test("""
            spawn task () {
                every <10:s> {
                    println(10)
                }
            }()
            println(0)
            broadcast in :global, tag(:Clock, [20000])
            println(1)
        """, true)
        assert(out == "0\n10\n10\n1") { out }
    }
    @Test
    fun km_10_await_clk() {
        val out = test("""
            spawn task () {
                loop {
                    await <10:s>
                    println(999)
                }
            }()
            println(0)
            broadcast (tag(:Clock, [5000])) in :global
            println(1)
            broadcast (tag(:Clock, [5000])) in :global 
            println(2)
        """, true)
        assert(out == "0\n1\n999\n2\n") { out }
    }

    // CLOCK

    @Test
    fun km_01_clock() {
        val out = test("""
            $IS ; $PLUS ; $MULT ; $COMP
            data :Clock = [ms]
            spawn {
                await <2:ms>
                println(:ok)
            }
            println(:0)
            broadcast(:Clock [1])
            println(:1)
            broadcast(:Clock [1])
            println(:2)
        """)
        assert(out == ":0\n:1\n:ok\n:2\n") { out }
    }
    @Test
    fun km_02_clock() {
        val out = test("""
            $IS ; $PLUS ; $MULT ; $COMP
            data :Clock = [ms]
            spawn {
                var x = 10
                every <x:ms> {
                    println(:x, x)
                    set x = x - 1
                }
                println(:ok)
            }
            println(:0)
            broadcast(:Clock [5])
            broadcast(:Clock [5])
            println(:1)
            broadcast(:Clock [5])
            broadcast(:Clock [5])
            println(:2)
            broadcast(:Clock [5])
            broadcast(:Clock [5])
            println(:3)
        """)
        assert(out == ":0\n:x\t10\n:1\n:x\t9\n:2\n:x\t8\n:3\n") { out }
    }

    // AWAIT / ORIGINAL

    @Test
    fun kn_01_await_task() {
        val out = test("""
            spawn {
                await spawn { 1 }
                println(1)
            }
            println(2)
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun kn_02_await_task() {
        val out = test("""
            spawn {
                spawn {
                    yield ()
                    println(1)
                    broadcast(nil) in :global
                    println(3)
                }
                yield ()
                println(2)
            }
            broadcast(nil) in :global
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun kn_03_await_task_rets() {
        val out = test("""
            spawn {
                var y = await spawn {
                    yield ()
                    [2]
                }
                println(y)
            }
            broadcast (nil) in :global
        """, true)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun kn_04_await_task_err() {
        val out = test("""
            spawn {
                var x = await spawn nil() in nil
            }
        """)
        //assert(out == "anon : (lin 2, col 27) : expected non-pool spawn : have \"spawn\"") { out }
        assert(out == " |  anon : (lin 4, col 14) : (spawn (task :nested () { (var (x) = do { ...\n" +
                " |  anon : (lin 3, col 31) : (spawn nil() in nil)\n" +
                " v  spawn error : expected task\n") { out }
    }
    @Test
    fun kn_05_await_task_rets() {
        val out = test("""
            spawn {
                var x = await spawn {
                    var y = []
                    y
                }
                println(x)
            }
        """, true)
        assert(out.contains("[]\n")) { out }
        //assert(out.contains("anon : (lin 3, col 53) : block escape error : incompatible scopes")) { out }
        //assert(out == "anon : (lin 2, col 20) : task :fake () { group { var x set x = do { gr...)\n" +
        //        "anon : (lin 3, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun kn_06_await_task_rets_valgrind () {
        val out = test("""
            spawn {
                var x = await spawn {
                    1
                }
                var y = await spawn {
                    yield ()
                    [2]
                }
                task T () {
                    3
                }
                var z = await spawn T()
                println(x,y,z)
            }
            broadcast(nil) in :global
        """, true)
        assert(out == "1\t[2]\t3\n") { out }
    }
    @Test
    fun kn_07_await_task() {
        val out = test("""
            task Main_Menu () {
                await(|false)
            }            
            spawn {
                await spawn Main_Menu ()
                println(999)
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun TODO_kn_08_await_now() {    // :check-now removed
        val out = test("""
            spawn {
                println(1)
                await( ;;;:check-now;;;| true)
                println(2)
            }
            println(3)
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun kn_09_await_notfalse() {
        val out = test("""
            spawn {
                println(1)
                await (|10)
                println(2)
            }
            broadcast(nil) in :global
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun TODO_kn_10_task_pub_fake_err() {
        val out = test("""
            spawn {
                watching evt|evt==:a {
                    every evt|evt==:b {
                        println(;;;task.;;;pub)    ;; no enclosing task
                    }
                }
            }
            println(1)
        """)
        //assert(out == "anon : (lin 5, col 33) : task error : missing enclosing task") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun kn_11_task_pub_fake() {
        val out = test("""
            spawn (task () {
                set ;;;task.;;;pub = 1
                watching evt| evt==:a {
                    every evt| evt==:b {
                        println(;;;task.;;;pub)
                    }
                }
            }) ()
             broadcast (:b) in :global
             broadcast (:b)
             broadcast (:a)
             broadcast (:b) in :global
        """, true)
        assert(out == "1\n1\n") { out }
    }
    @Test
    fun kn_12_task_tup_status() {
        val out = test("""
            task T () {}
            val ts = [spawn T()]
            println(status(ts[0]))
        """)
        assert(out == ":terminated\n") { out }
    }

    // WATCHING

    @Test
    fun ll_01_watching() {
        val out = test("""
            $COMP
            spawn {
                watching 1 {
                    defer { println(:z) }
                    println(:x)
                    ${AWAIT()}
                    println(:y)
                    ${AWAIT()}
                    println(999)
                }
                println(:A)
            }
            println(1)
            broadcast (nil)
            println(2)
            broadcast (1)
            println(3)
        """)
        assert(out == ":x\n1\n:y\n2\n:z\n:A\n3\n") { out }
    }
    @Test
    fun ll_02_watching() {
        val out = test("""
            spawn {
                watching <100:ms> {
                    every |false {
                    }
                }
            }
        """)
        assert(out == "anon : (lin 3, col 31) : access error : variable \"{{*}}\" is not declared\n") { out }
    }
    @Test
    fun ll_03_watching_clk() {
        val out = test("""
            spawn {
                watching <10:s> {
                    defer { println(10) }
                    await (|false)
                    println(1)
                }
                println(999)
            }
            println(0)
            broadcast (tag(:Clock,[5000])) in :global 
            println(1)
            broadcast (tag(:Clock, [5000]) )
            println(2)
        """, true)
        assert(out == "0\n1\n10\n999\n2\n") { out }
    }
    @Test
    fun ll_04_watching() {
        val out = test(
            """
            task Bird () {
                watching |true {
                    par {
                    } with {
                    }
                }
            }            
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun ll_05_watching() {
        val out = test("""
            task T () {
                watching (|error(:error)) {
                    await (|false)
                }
            }            
            spawn T() in tasks()
            broadcast (nil)
        """, true)
        assert(out == " |  anon : (lin 8, col 13) : broadcast'(:task,nil)\n" +
                " |  anon : (lin 3, col 28) : error(:error)\n" +
                " v  error : :error\n") { out }
    }
    @Test
    fun BUG_ll_06_watching_track() {
        val out = test("""
            task T () {
                set ;;;task.;;;pub = [10]
                await (:evt)
                println(:end)
            }
            val t = spawn(T)()
            val x = ;;;track;;;(t)
            spawn {
                watching ;;;:check-now;;; |it==x {
                    println(x.pub[0])
                    broadcast(nil) in :global
                    println(x.pub[0])
                    broadcast(:evt) in :global          ;; BUG: same tick as watching?
                    println(:nooo)   ;; never printed
                    await (|false)
                }
                println(status(x))
            }
            println(:ok)
        """, true)
        assert(out == "10\n10\nnil\n:ok\n") { out }
    }
    @Test
    fun BUG_ll_07_awaiting17_track() {  // same as above
        val out = test("""
            task T () {
                set pub = :pub
                await (|it==:evt)
            }
            val t = spawn T()
            spawn {
                watching |it==t {
                    broadcast (:evt) in :global
                    println(:nooo)
                }
                println(status(x))
            }
            println(:ok)
        """, true)
        assert(out == "nil\n:ok\n") { out }
    }
    @Test
    fun ll_08_awaiting() {
        val out = test("""
            spawn {
                watching :x {
                    watching :y {
                    }
                }
                println(:ok)
            }
        """, true)
        assert(out == ":ok\n") { out }
    }

    // TOGGLE

    @Test
    fun mm_01_toggle() {
        val out = test("""
            task T (v) {
                set pub = v
                toggle :Show {
                    println(pub)
                    every (it| (it is? :dict) and (it.sub==:draw)) {
                        println(it.v)
                    }
                }
            }
            spawn T(0)
            broadcast(@[(:sub,:draw),(:v,1)])
            broadcast(:Show [false])
            broadcast(:Show [false])
            broadcast(@[(:sub,:draw),(:v,99)])
            broadcast(:Show [true])
            broadcast(:Show [true])
            broadcast(@[(:sub,:draw),(:v,2)])
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun mm_02_toggle() {
        val out = test("""
            task T (v) {
                set pub = v
                toggle :Show {
                    println(pub)
                    every :draw {
                        println(it[0])
                    }
                }
            }
            spawn T (0)
            broadcast (tag(:draw, [1]))
            broadcast (tag(:Show, [false]))
            broadcast (tag(:Show, [false]))
            broadcast (tag(:draw, [99]))
            broadcast (tag(:Show, [true]))
            broadcast (tag(:Show, [true]))
            broadcast (tag(:draw, [2]))
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun mm_03_toggle() {
        val out = test("""
            spawn {
                val x = toggle :Show {
                    10
                }
                println(x)
            }
            println(:ok)
        """, true)
        assert(out == "10\n:ok\n") { out }
    }

    // METHODS

    @Test
    fun oo_01_method() {
        val out = test("""
            func f (v) { v }
            val v = 10->f()
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oo_02_method() {
        val out = test("""
            func f (v) { 10 }
            func g (v) { v }
            val v = 99->f()->g()
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oo_03_method() {
        val out = test("""
            func f (v) { 10 }
            func g (v) { v }
            val v = 99->f->g
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oo_04_method() {
        val out = test("""
            $PLUS
            func f (v,x) { v - x }
            val v = 10->f(20)
            println(v)
        """)
        assert(out == "-10\n") { out }
    }
    @Test
    fun oo_05_method() {
        val out = test("""
            func f (v) { v }
            val v = f<-20
            println(v)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun oo_06_method() {
        val out = test("""
            $PLUS
            func f (v,x) { v - x }
            val v = f(10)<-20
            println(v)
        """)
        assert(out == "-10\n") { out }
    }
    @Test
    fun oo_07_method() {
        val out = test("""
            func f (v) { v() }
            val v = f <- \{10} thus { it }
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oo_08_method() {
        val out = test("""
            func f (x,y) { y(x) }
            val v = 10 -> f <- \{it} thus { it }
            println(v)
        """)
        assert(out == "10\n") { out }
    }

    // PIPE

    @Test
    fun op_01_pipe() {
        val out = test("""
            $PLUS
            func f (v,x) { v+x }
            val v = 10-->f(20)
            println(v)
        """)
        assert(out == "30\n") { out }
    }
    @Test
    fun op_02_pipe() {
        val out = test("""
            func g (v) { v }
            func f (v) { g }
            val v = 10-->f->g
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun op_03_pipe() {
        val out = test("""
            $PLUS
            func g (v) { v+1 }
            func f (v) { -v }
            val v = f<--10->g
            println(v)
        """)
        assert(out == "-11\n") { out }
    }
    @Test
    fun op_04_thus() {
        val out = test(
            """
            val x = 1 --> \{
                it
            }
            println(x)
        """,true)
        assert(out == "1\n") { out }
    }
    @Test
    fun op_05_thus_err() {
        val out = test(
            """
            val x = [] --> \ { ,x =>
                x
            }
            println(x)
        """,true)
        assert(out == "anon : (lin 2, col 33) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun op_05_thus() {
        val out = test(
            """
            val y = [] --> \ {, x =>
                x
            }
            println(y)
        """,true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun op_06_thus() {
        val out = test(
            """
            val x = \{
                it
            } <-- 1
            println(x)
        """,true)
        assert(out == "1\n") { out }
    }
    @Test
    fun op_07_thus() {
        val out = test(
            """
            val x = \ {,y =>
                y
            } <-- []
            println(x)
        """,true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun op_08_thus() {
        val out = test(
            """
            val x = 2 --> \{ it + 1 } --> \{ it * 2 }
            println(x)
        """,true)
        assert(out == "6\n") { out }
    }
    @Test
    fun op_09_thus() {
        val out = test(
            """
            val x = \{ it + 1 } <-- \{ it * 2 } <-- 2
            println(x)
        """,true)
        assert(out == "5\n") { out }
    }

    // CAST

    @Test
    fun oq_01_cast() {
        val out = test("""
            data :X = [x]
            val t = [[10]]
            println(t[0].(:X).x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oq_02_cast() {
        val out = test("""
            data :X = [x]
            val t = [[[10]]]
            println(t[0].(:X).x)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun BUG_oq_03_cast() {
        val out = test("""
            data :X = [x]
            task T () {
                set pub = [10]
                yield()
            }
            val t = spawn T()
            pub(t).(:X)
            nil
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun BUG_oq_04_cast() {
        val out = test("""
            data :X = [x]
            val T = task () {
                set pub = [10]
                yield(nil)
            }
            val t = spawn T(nil)
            t.pub thus { ceu_94 :X =>
                ceu_94
            }
            nil
         """)
         assert(out == "[10]\n") { out }
     }

    // WHERE

    @Test
    fun oq_01_where() {
        val out = test(
            """
                $PLUS
            println(x) where {
                val x = 1
            }
            val z = (y + 10) where {
                val y = 20
            }
            println(z)
        """)
        assert(out == "1\n30\n") { out }
    }
    @Test
    fun oq_02_where() {
        val out = test(
            """
            task T (v) {
                println(v)
            }
            val t = (spawn T(v)) where { val v = 10 }
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 5, col 34) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun oq_03_where() {
        val out = test(
            """
            coro T (v) {
                println(v)
            }
            (val t = spawn T(v)) where { val v = 10 }
            println(t)
        """)
        assert(out == "anon : (lin 6, col 21) : access error : variable \"t\" is not declared\n") { out }
    }
    @Test
    fun op_04_where() {
        val out = test(
            """
            $PLUS
            val z = y + 10 where {
                val y = 20
            }
            println(z)
        """)
        //assert(out == "anon : (lin 2, col 21) : access error : variable \"y\" is not declared") { out }
        assert(out == "30\n") { out }
    }
    @Test
    fun op_05_where() {
        val out = test("""
            val x = y
                where {
                    val y = 10
                }
            println(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oq_06_where() {
        val out = test(
            """
            task T (v) {
                println(v)
            }
            val t = (spawn T(v)) where { val v = 10 }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun op_07_where() {
        val out = test(
            """
            task T (v) {
                println(v)
            }
            val t = (spawn T(v)) where {
                val v = 10
            }
            println(type(t))
        """)
        assert(out == "10\n:exe-task\n") { out }
    }
    @Test
    fun todo_iter_op_08_where() {
        val out = test(
            """
            task T (v) {
                println(v)
                yield()
            }
            val ts = tasks()
            (spawn T(v) in ts) where {
                val v = 10
            }
            loop t in ts {
                println(type(t))
            }
        """, true)
        assert(out == "10\n:exe-task\n") { out }
    }

    // LAMBDA

    @Test
    fun pp_01_lambda () {
        val out = test("""
            println(\{ it })
        """)
        assert(out.contains("func: 0x")) { out }
    }
    @Test
    fun pp_02_lambda () {
        val out = test("""
            $PLUS
            println(\{,x=>x+x}(2))
        """)
        assert(out.contains("4\n")) { out }
    }
    @Test
    fun pp_03_lambda () {
        val out = test("""
            println(\{,x=>x}(1))
        """)
        assert(out.contains("1\n")) { out }
    }
    @Test
    fun pp_04_lambda () {
        val out = test(
            """
            println(\{ it }(10))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_05_lambda () {
        val out = test(
            """
            func f (g) {
                g(10)
            }
            println(f <- \{ it })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_06_it_it () {
        val out = test(
            """
            val x = \{ \{ it }(10) }()    ;; it1/it2
            println(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_07_lambda_call () {
        val out = test("""
            func f (v,g) {
                g(v)
            }
            val v = f(5) <- \{ it }
            println(v)
        """)
        assert(out == "5\n") { out }
    }
    @Test
    fun pp_08_lambda_call () {
        val out = test("""
            func f (g) {
                g()
            }
            val v = f( \{ 10 } )
            println(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_09_lambda_call () {
        val out = test("""
            println(\{, x => x }(10))
        """)
        assert(out == "10\n") { out }
    }

    // TEST

    @Test
    fun qq_01_test () {
        val out = test("""
            do {
                println(:1)
            }
            test {
                println(:2)
            }
            do {
                test {
                    println(:3)
                }
                println(:4)
            }
        """)
        assert(out == ":1\n:4\n") { out }
    }
    @Test
    fun qq_02_test () {
        TEST = true
        val out = test("""
            do {
                println(:1)
            }
            test {
                println(:2)
            }
            do {
                test {
                    println(:3)
                }
                println(:4)
            }
        """)
        assert(out == ":1\n:2\n:3\n:4\n") { out }
    }

    // TUPLE DOT

    @Test
    fun tt_01_dots() {
        val out = test(
            """
            val x = [nil,[10]]
            println(x, x[1], x[1][0])
        """
        )
        assert(out == "[nil,[10]]\t[10]\t10\n") { out }
    }
    @Test
    fun TODO_tt_02_index_tuple() {
        val out = test("""
            val t = [1,2,3]
            println(t.a, t.c)
        """)
        assert(out == "1\t3\n") { out }
    }
    @Test
    fun tt_03_index_dict() {
        val out = test("""
            val t = @[ (:x,1), (:y,2) ]
            println(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun tt_04_string() {
        val out = test("""
            var v = "abc"
            set v[#v] = 'a'
            set v[2] = 'b'
            println(v[0])
            `puts(${D}v.Dyn->Vector.buf);`
        """)
        assert(out == "a\nabba\n") { out }
    }
    @Test
    fun tt_05_string() {
        val out = test("""
            println("")
            println("a\tb")
            println("a\nb")
            println("a'\"b")
        """)
        assert(out == "#[]\na\tb\na\nb\na'\"b\n") { out }
    }
    @Test
    fun tt_06_dict_init_err() {
        val out = test("""
            var t = @[x,y]
            println(t.x, t.y)
        """)
        assert(out == "anon : (lin 2, col 24) : expected \"=\" : have \",\"\n") { out }
    }
    @Test
    fun tt_07_dict_init() {
        val out = test("""
            var t = @[x=1, y=2]
            println(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun tt_08_vector() {
        val out = test("""
            var v
            set v = #[]
            match true {
                |true {
                    set v[#v] = 10
                }
            }
            println(v)
        """)
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun tt_09_vector_concat() {
        val out = test("""
            var v1
            set v1 = #[1,2,3]
            var v2
            set v2 = #[4,5,6]
            println(v1 ++ v2)
        """, true)
        assert(out == "#[1,2,3,4,5,6]\n") { out }
    }

    // TAG CONSTRUCTOR / DECLARATION

    @Test
    fun uu_01_cons() {
        val out = test("println(:T [])")
        assert(out == ":T []\n") { out }
    }
    @Test
    fun uu_02_cons() {
        val out = test("""
            data :T = [v]
            val t = :T [10]
            println(t.v, t)
        """)
        assert(out == "10\t:T [10]\n") { out }
    }

    // PPP: PEEK, PUSH, POP

    @Test
    fun vv_01_ppp_peek() {
        val out = test("""
            $PLUS
            val v = #[1]
            set v[=] = 10
            println(v)
        """)
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun vv_02_ppp_peek() {
        val out = test("""
            $PLUS
            val v = #[10]
            println(v[=])
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun vv_03_ppp_push() {
        val out = test("""
            $PLUS
            val v = #[]
            set v[+] = 1
            println(v)
        """)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun TODO_COL_vv_04_ppp_push_err() {
        val out = test("""
            $PLUS
            val v = #[]
            v[+]
        """,)
        assert(out == "anon : (lin 4, col 41) : index error : out of bounds\n" +
                ":error\n") { out }
    }
    @Test
    fun vv_05_ppp_pop() {
        val out = test("""
            $PLUS
            var v = #[1]
            var x = v[-]
            println(#v, x)
        """)
        assert(out == "0\t1\n") { out }
    }
    @Test
    fun vv_06_ppp_pop_err() {
        val out = test("""
            $PLUS
            val v = #[1]
            set v[-] = 10   ;; cannot set v[-]
            println(v)
        """)
        assert(out == ("anon : (lin 4, col 13) : set error : expected assignable destination\n")) { out }
    }
    @Test
    fun vv_07_ppp() {
        val out = test("""
            $PLUS
            var v
            set v = #[]
            set v[+] = 1
            set v[+] = 2
            set v[=] = 20
            set v[+] = 3
            println(#v, v[=])
            val x = v[-]
            println(#v, v[=], x)
        """)
        assert(out == "3\t3\n2\t20\t3\n") { out }
    }
    @Test
    fun vv_08_ppp_debug() {
        val out = test("""
            $PLUS
            var v
            set v = #[10]
            println(v[#v - 1])
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun vv_09_ppp_debug() {
        val out = test("""
            $PLUS
            var v
            set v = #[10]
            println(v[-1+1])
        """)
        assert(out == "anon : (lin 5, col 24) : expected \"]\" : have \"1\"\n") { out }
    }
    @Test
    fun vv_10_ppp() {
        val out = test("""
            $PLUS
            val stk = [1]
            stk[-]
            println(stk, #stk)
        """)
        assert(out == "[nil]\t1\n") { out }
    }
    @Test
    fun vv_11_vector_size() {
        val out = test("""
            val v = #[]
            println(#v, v)
            set v[+] = 1
            set v[+] = 2
            println(#v, v)
            val top = v[-]
            println(#v, v, v[=], top)
        """, true)
        assert(out == "0\t#[]\n2\t#[1,2]\n1\t#[1]\t1\t2\n") { out }
    }

    // DATA

    @Test
    fun xx_01_data_string_to_tag() {
        val out = test("""
            data :A = [] {
                :B = [] {
                    :C = []
                }
            }
            println(to-tag-string(":A"), to-tag-string(":A.B"), to-tag-string(":A.B.C"))
        """, true)
        assert(out == ":A\t:A.B\t:A.B.C\n") { out }
    }

    // ==, ===, /=, =/=

    @Test
    fun xa_01_eqeqeq_tup() {
        val out = test(
            """
            println([1] === [1])
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun xa_02_op_eqeqeq_tup() {
        val out = test(
            """
            println([1] === [1])
            println([ ] === [1])
            println([1] =/= [1])
            println([1,[],[1,2,3]] === [1,[],[1,2,3]])
            println([nil,[[1,1],1]] === [nil,[[1,1],1]])
        """, true)
        assert(out == "true\nfalse\nfalse\ntrue\ntrue\n") { out }
    }
    @Test
    fun xa_03_op_eqeqeq_tup() {
        val out = test(
            """
            println([1,[1],1] === [1,[1],1])
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun xb_04_op_eqs_dic() {
        val out = test(
            """
            println(@[] ==  @[])
            println(@[] === @[])
            println(@[] /=  @[])
            println(@[] =/= @[])
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun xb_05_op_eqs_vec() {
        val out = test(
            """
            println(#[]  ==  #[])
            println(#[1] === #[1])
            println(#[1] /=  #[1])
            println(#[]  =/= #[])
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun xb_06_op_eqs_vec_dic_tup() {
        val out = test(
            """
            println(@[(:y,false)] === @[(:x,true)])
        """, true)
        assert(out == "false\n") { out }
    }
    @Test
    fun xb_07_op_eqs_vec_dic_tup() {
        val out = test(
            """
            println([#[],@[]] ==  [#[],@[]])
            println([#[],@[]] /=  [#[],@[]])
            println([#[1],@[(:y,false),(:x,true)]] === [#[1],@[(:x,true),(:y,false)]])
            println([#[],@[]] =/= [#[],@[]])
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun xb_08_valgrind() {
        val out = test(
            """
            val f = func (v) {
                do {
                    do {
                        do {
                            val x
                            println(x)
                            do {
                                nil
                            }
                            val y = x[0]
                        }
                    }
                }
            }
            do {
                f(@[(:y,false)])
            }
        """)
        assert(out == "nil\n" +
                " |  anon : (lin 17, col 17) : f(@[(:y,false)])\n" +
                " |  anon : (lin 11, col 37) : x[0]\n" +
                " v  index error : expected collection\n") { out }
    }
    @Test
    fun xb_09_xxx() {
        val out = test(
            """
            println([@[]] === [@[]])
        """, true)
        assert(out == "true\n") { out }
    }

    // TO-*

    @Test
    fun xc_01_tostring() {
        val out = test("""
            val s = to.string(10)
            println(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun xc_02_tonumber() {
        val out = test("""
            val n = to.number("10")
            println(type(n), n)
        """, true)
        assert(out == ":number\t10\n") { out }
    }
    @Test
    fun xc_03_tonumber_tostring() {
        val out = test("""
            val s = to.string(to.number("10"))
            println(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun xc_04_tovector() {
        val out = test("""
            coro T() {
                yield([1])
            }
            val t = coroutine(T)
            val v = to.vector(t)
            println(v)
        """, true)
        assert(out == "#[[1]]\n") { out }
    }
    @Test
    fun xc_05_tovector() {
        val out = test("""
            val v = do {
                val t = [[1],[2],[3]]
                to.vector(t)
            }
            println(v)
        """, true)
        assert(out == "#[[1],[2],[3]]\n") { out }
    }
    @Test
    fun xc_06_string_to_tag() {
        val out = test("""
            do :xyz
            println(to-tag-string(":x"))
            println(to-tag-string(":xyz"))
            println(to-tag-string("xyz"))
        """, true)
        assert(out == "nil\n:xyz\nnil\n") { out }
    }
    @Test
    fun xc_07_to_char() {
        val out = test("""
            println(to.char('a'))
            println(to.char(65))
            println(to.char("x"))
            println(to.char(""))
            println(to.char("ab"))
            println(to.char("\\n"))
            println(:ok)
        """, true)
        assert(out == "a\nA\nx\nnil\nnil\n\n\n:ok\n") { out }
    }
    @Test
    fun xc_07x_to_char() {
        val out = test("""
            val v = [1,2]
            println(:v, v, #v, v[0])
            ifs {
                (#v /= 2) => nil
                (v[0] /= '\\') => nil
            }
        """)
        assert(out == ":v\t[1,2]\t2\t1\n") { out }
    }
    @Test
    fun xc_08_totuple() {
        val out = test("""
            println(to.tuple([]))
            println(to.tuple(#[1,2]))
        """, true)
        assert(out == "[]\n[1,2]\n") { out }
    }
    @Test
    fun xc_08_todict() {
        val out = test("""
            println(to.dict([[:x,1],[:y,2]]))
            println(to.dict(#[[:x,1],[:y,2]]))
        """, true)
        assert(out == "@[(:x,1),(:y,2)]\n" +
                "@[(:x,1),(:y,2)]\n") { out }
    }
    @Test
    fun xc_09_todict() {
        val out = test("""
            println(to.dict([:x,:y]))
            println(to.dict([]))
        """, true)
        assert(out == "@[(:x,0),(:y,1)]\n" +
                "@[]\n") { out }
    }

    // PRELUDE

    @Test
    fun za_01_ok() {
        val out = test("""
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun za_02_tasks() {
        val out = test("""
            val ts = tasks()
            loop in {1=>10} {
                ;;dump(ts)
                do [ts]
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun TODO_za_03_in() {
        val out = test("""
            println(10 in? [1,2,3])
            println(10 in? [1,10,3])
        """, true)
        assert(out == "false\ntrue\n") { out }
    }
    @Test
    fun za_04_or() {
        val out = test("""
            func f () {
                if nil {
                    nil
                }
                nil
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun za_05_assert() {
        val out = test("""
            println(assert(10))
            assert(nil)
        """, true)
        assert(out == "10\n" +
                " |  anon : (lin 3, col 13) : assert(nil)\n" +
                " |  build/prelude-x.ceu : (lin 480, col 25) : error(#['a','s','s','e','r','t','i','o','n...\n" +
                " v  error : assertion error\n") { out }
    }
    @Test
    fun za_06_copy() {
        val out = test("""
            println(copy([1,2,3]))
            println(copy(#[1,2,3]))
            println(copy(@[(:k1,[1,2,3]), (1,#[])]))
        """, true)
        assert(out == "[1,2,3]\n#[1,2,3]\n@[(:k1,[1,2,3]),(1,#[])]\n") { out }
    }
    @Test
    fun za_07_assert() {
        val out = test("""
            catch :assert {
                assert([] is? :bool, "ok")
            }
            assert(1 is-not? :number)
        """, true)
        assert(out.contains(" |  anon : (lin 3, col 17) : assert(is'([],:bool),#['o','k'])\n" +
                " |  build/prelude-x.ceu : (lin 479, col 25) : error({{++}}(#['a','s','s','e','r','t','i'...\n" +
                " v  error : assertion error : ok\n")) { out }
    }
    @Test
    fun TODO_za_08_comp() {     // fp.*
        val out = test("""
            func square (x) {
                x**2
            }
            val quad = square <|< square
            println(quad(3))
        """, true)
        assert(out == "81\n") { out }
    }
    @Test
    fun za_09_assert() {
        val out = test("""
            assert(false, 10)
        """, true)
        assert(out == " |  anon : (lin 2, col 13) : assert(false,10)\n" +
                " |  build/prelude-x.ceu : (lin 481, col 17) : error(msg)\n" +
                " v  error : 10\n") { out }
    }
    @Test
    fun za_10_assert() {
        val out = test("""
            assert(false, :type [])
        """, true)
        assert(out == " |  anon : (lin 2, col 13) : assert(false,tag(:type,[]))\n" +
                " |  build/prelude-x.ceu : (lin 481, col 17) : error(msg)\n" +
                " v  error : :type []\n") { out }
    }

    // ORIGINAL

    @Test
    fun zb_01() {
        val out = test("""
            func g () {
            }
            coro bar () {
                do [g, coroutine(coro () {})]
                nil
            }
            val it = [g, coroutine(bar)]
            resume it[1]()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_02() {
        val out = test("""
            data :Iterator = [f,s,tp,i]
            func to-iter-coro (itr :Iterator) {
                val co = itr.s
                val v = resume co()
                ((status(co) /= :terminated) and v) or nil
            }
            func to-iter (v) {
                [to-iter-coro,  v]
            }
            
            func bar (v) {
                [to-iter-coro, v]
            }
            bar(coroutine(coro () {}))
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_03() {
        val out = test("""
            func g () {}
            func f (v) {
                [g, v]
            }
            func x () {
                val t = coro () {}
                f(t)
                nil
            }
            x()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_04_all() {
        val out = test("""
            task T (pos) {
                await (|true)
                println(pos)
            }
            spawn {
                val ts = tasks()
                do {
                    spawn T([]) in ts
                }
                await (|false)
            }
            broadcast(nil) in :global
        """, true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun zb_05_all() {
        val out = test("""
            task T (pos) {
                set ;;;task.;;;pub = func () { pos }
                await (false)
            }
            val t = spawn T ([1,2])
            println(t.pub())
        """, true)
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun zb_06_all() {
        val out = test("""
            task T () {
                do {
                    val x = []
                    set ;;;task.;;;pub = func () { x }
                }
            }
            spawn T ()
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 8, col 19) : T()\n" +
        //        "anon : (lin 5, col 30) : set error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun zb_07_all() {
        val out = test("""
            task U () {
                set ;;;task.;;;pub = func () {
                    10
                }
            }
            task T (u) {
                println(u.pub())
            }
            spawn T (spawn U())
        """, true)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 10, col 28) : U()\n" +
        //        "anon : (lin 2, col 23) : block escape error : incompatible scopes\n:error\n") { out }
    }
    @Test
    fun zb_08_all_valgrind() {
        val out = test("""
            task U () {
                set pub = func () {
                    10
                }
                await(|false)
            }
            task T (u) {
                println(u.pub())
                nil
            }
            spawn T (spawn U())
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun zb_08x_all_valgrind() {
        val out = test("""
            task U () {
                yield()
            }
            task T (u) {
                println(u)
            }
            spawn T (spawn U())
        """)
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun zb_10_all() {
        val out = test("""
            func f () {}
            spawn {
                f() where {}
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun zb_11_all() {
        val out = test("""
            spawn {
                loop {
                    await (10)
                    broadcast(tag(:pause, [])) in :global 
                    watching 10 {
                        await(|false)
                    }
                    broadcast(tag(:resume, [])) in :global 
                }
            }
            broadcast (10) in :global
            broadcast (10) in :global
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_12_all() {
        val out = test("""
            spawn {
                loop {
                    await (10)
                    broadcast (tag(:pause, [])) in :global
                    watching 10 {
                        await(|false)
                    }
                    broadcast (tag(:resume, [])) in :global
                    await (|true)
                }
            }
            broadcast (10) in :global
            broadcast (10) in :global
            broadcast (10) in :global
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_13_all_valgrind () {
        val out = test("""
            spawn {
                loop {
                    await(10)
                    println(:1)
                    watching 10 {
                        await(| false)
                    }
                    println(:2)
                }
            }
            broadcast (10) in :global    ;; :1
            broadcast (10) in :global    ;; :2 (not :1 again)
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun zb_14_all_term_coro () {
        val out = test("""
            task T () {
                println(:1)
                watching (|false) {
                    await (|true)
                }
                println(:2)
                ;;println(:t)
            }
            spawn {
                val ts = tasks()
                spawn T() in ts
                ;;println(:every)
                every :e {
                    ;;println(:while)
                    loop t in ts {
                        ;;println(t, detrack(t), status(detrack(t)))
                        assert(status(;;;detrack;;;(t)) /= :terminated)
                    }
                }
            }
            ;;println(:bcast)
            broadcast(:e) in :global
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun zb_15_tk_pre () {
        val out = test("""
            match v {
                is? :pointer => c-to-string(v)
                is? :number => 1
            }
        """)
        assert(out == "anon : (lin 2, col 19) : access error : variable \"v\" is not declared\n") { out }
    }
    @Test
    fun zb_16_self_kill () {
        val out = test("""
            spawn {
                loop {
                    println(:10)
                    spawn {
                        println(:a)
                        await (:E)
                        do {
                            println(:b)
                            broadcast(:E) in :global
                            println(:c)
                        }
                        println(:d)
                    }
                    println(:20)
                    await (:E)
                    println(:30)
                }
            }
            println(:1)
            broadcast (nil) in :global
            println(:2)
            broadcast (:E) in :global
            println(:3)
        """, true)
        assert(out == ":10\n:a\n:20\n:1\n:2\n:b\n:30\n:10\n:a\n:20\n:3\n") { out }
    }
    @Test
    fun zb_17_tasks_it() {
        val out = test("""
            var ts
            set ts = tasks()
            println(type(ts))
            var T
            set T = task (v) {
                set ;;;task.;;;pub = v
                val v' = yield(nil)
            }
            spawn T(1) in ts
            spawn T(2) in ts
            
            loop t1 in ts {
                loop in ts {
                    println(;;;detrack;;;(t1).pub, ;;;detrack;;;(it).pub)
                }
            }
             broadcast (2) in :global
        """, true)
        assert(out == ":tasks\n1\t1\n1\t2\n2\t1\n2\t2\n") { out }
    }
    @Test
    fun zb_18_all_defer() {
        val out = test("""
            coro F () {
                defer {
                    println(:x)
                }
                yield()
                defer {
                    println(:y)
                }
                yield()
            }
            do {
                val f = coroutine(F)
                resume f()
                resume f()
            }
        """)
        assert(out == ":y\n:x\n") { out }
    }
    @Test
    fun zb_19_every () {
        val out = test("""
            spawn {
                every :e {
                    loop in nil {
                    }
                }
            }
        """)
        assert(out == "anon : (lin 3, col 51) : access error : variable \"is'\" is not declared\n") { out }
    }
    @Test
    fun zb_20_all_line() {
        val out = test("""
        func f (co) {
            resume co()
        }
        coro C () {
            yield()
            var t = []
            yield(;;;drop;;;(t))
        }
        do {
            val co = coroutine (C)
            resume co()
            loop {
                val v = f(co)
                println(v)
                until true
            }
        }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun zb_21() {
        val out = test("""
        coro Take () {
            yield()
            loop i in {1 => 3} {
                yield("line")
            }
        }
        do {
            val take = create-resume(Take)
            coro Show () {
                var line = yield()
                loop {
                    while line
                    set line = yield()
                    println(line)
                }
            }
            coro Send (co, nxt) {
                loop v in to-iter(co) {
                    resume nxt(;;;drop;;;(v))
                }
            }
            create-resume(Send, take, create-resume(Show))
        }
        """, true)
        assert(out == "line\n" +
                "line\n") { out }
    }
    @Test
    fun zb_22_all() {
        val out = test("""
            task T () {
                set ;;;task.;;;pub = []
                await (|false)
            }
            func f (v1, v2) {}
            spawn {
                val ts = tasks()
                spawn T() in ts
                loop t in ts {
                    val x = ;;;detrack;;;(t)
                    x and true and true and f(x.pub, x.pub)
                }
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_23_all() {
        val out = test("""
            task T () {
                await (|false)
            }
            spawn {
                val ts = tasks()
                spawn T() in ts
                await (|true)
                catch |true {
                    loop b in ts {
                        error(;;;drop;;;(b))
                    }
                }
                nil
            }
            loop {
                broadcast (:X []) in :global
                until true
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }

    // PRELUDE / MATH

    @Test
    fun zc_01_min() {
        val out = test("""
            println(math.min(10,20), math.min(20,10))
        """, true)
        assert(out == "10\t10\n") { out }
    }
    @Test
    fun zc_02_max() {
        val out = test("""
            println(math.max(10,20), math.max(20,10))
        """, true)
        assert(out == "20\t20\n") { out }
    }
    @Test
    fun zc_03_between() {
        val out = test("""
            println(math.between(1,   [10,20]))
            println(math.between(100, [10,20]))
            println(math.between(15,  [10,20]))
        """, true)
        assert(out == "10\n20\n15\n") { out }
    }
    @Test
    fun zc_04_pi() {
        val out = test("""
            println(math.PI)
            println(math.sin(math.PI/2))
            println(math.cos(math.PI))
        """, true)
        assert(out == "3.14159\n1\n-1\n") { out }
    }
    @Test
    fun zc_05_floor() {
        val out = test("""
            println(math.floor(1.7))
            println(math.ceil(1.1))
            println(math.round(1.51))
        """, true)
        assert(out == "1\n2\n2\n") { out }
    }

    // PRELUDE / RANDOM

    @Test
    fun zd_01_random() {
        val out = test("""
            random.seed(0)
            println(random.next() % 100, random.next() % 100)
        """, true)
        assert(out == "83\t86\n") { out }
    }

    // MISC

    @Test
    fun zz_01_type() {
        val out = test("""
            println(static?(:number))
            println(static?(type([])))
            println(dynamic?(type(nil)))
            println(dynamic?(:vector))
            println(string?("oi"))
            println(string?(#[]))
            ;;println(type.nil?(nil))
            ;;println(type.dict?(nil))
        """, true)
        assert(out == "true\nfalse\nfalse\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun BUG_zz_02_track_bcast() {
        DEBUG = true
        val out = test("""
            $IS
            val B = task () {
                yield(nil)
            }
            val bs = tasks(5)
            spawn B() in bs
            func () {
                val b = next-tasks(bs,nil)
                broadcast(nil) in b
                next-tasks(bs,b)
            } ()
        """)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }
    @Test
    fun TODO_zz_03_double_awake() {
        DEBUG = true
        val out = test("""
            spawn {
                loop {
            println(false)
                    val t = spawn {
                        await(:X)
                    }
                    spawn {
                        loop {
                            yield()
                        }
                    }
                    await(t)
            println(true)
                    await(:X)
                }
            }
            broadcast(true)
        """)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }
    @Test
    fun zz_04_par_arg() {
        val out = test("""
            $IS
            task T (v) {
                par {
                    every :X {
                        do v
                    }
                } with {
                }
            }
            spawn T(100)
            broadcast(:X)
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_05_mem() {
        val out = test("""
            task T (v) {
                println(:ok)
                await(|it==:FIN)
            }
            val ts = tasks(1)
            spawn T() in ts
            spawn {
                loop {
                    await |it==:CHK {
                        var xxx = #[next-tasks(ts)]
                        ;;set xxx = nil
                    }
                }
            }
            spawn T() in ts
            broadcast(:CHK)
            broadcast(:FIN)
            spawn T() in ts
        """)
        assert(out == ":ok\n:ok\n") { out }
    }
}
