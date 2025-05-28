package tst_99

import dceu.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_99 {
    @Test
    fun bc_03_is() {
        val out = test("""
            val t = []
            tag(:x,t)
            print(t is? :x)
            tag(:y,t)
            print(t is-not? :y)
            tag(nil,t)
            print(t is-not? :x)
        """, true)
        assert(out == "true\nfalse\ntrue\n") { out }
    }

    // EMPTY IF / BLOCK

    @Test
    fun aa_01_if() {
        val out = test("""
            val x = if (true) { 1 }
            print(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_02_do() {
        val out = test("""
            print(do {})
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
            print(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun aa_04_if() {
        val out = test("""
            var x
            set x = 10
            set x = if (nil) {} else { 1 }
            print(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_05_if() {
        val out = test("""
            print(if [] {})
        """)
        //assert(out == "anon : (lin 1, col 4) : if throw : invalid condition\n") { out }
        assert(out == "nil\n") { out }
        //assert(out == "\n") { out }
    }
    @Test
    fun aa_06_if() {
        val out = test("""
            print(if false { true })
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun aa_07_func() {
        val out = test("""
            print(func () {} ())
        """)
        assert(out == "nil\n") { out }
        //assert(out == "\n") { out }
    }

    // AS / DO

    @Test
    fun ab_01_yield() {
        val out = test("""
            val x = if (true) { 1 }
            print(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ab_02_do_escape() {
        val out = test("""
            val v = do :x {
                do :y {
                    escape(:y, 10)
                    print(:no)
                }
            }
            print(v)
        """)
        assert(out == "10\n") { out }
    }

    // OPS: not, and, or

    @Test
    fun bb_01_op_or_and() {
        val out = test("""
            print(true or print(1))
            print(false and print(1))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun bb_02_op_not() {
        val out = test("""
            print(true and (not false))
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_02x_op_not() {
        val out = test("""
            print(not nil and not false)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_03_or_and() {
        val out = test("""
            print(1 or throw(5))
            print(1 and 2)
            print(nil and 2)
            print(nil or 2)
        """)
        assert(out == "1\n2\nnil\n2\n") { out }
    }
    @Test
    fun bb_03_or_and_no() {
        val out = test("""
            print(1 or throw(5))
            print(nil or 2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bb_03_or_and_ok() {
        val out = test("""
            print(
                (1 thus { \ceu_6 =>
                    (if ceu_6 {
                       ceu_6
                    } else {
                        throw(5)
                    })
                })
            )
            print((nil thus { \ceu_41 =>
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
            print(true and ([] or []))
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun bb_05_and_and() {
        val out = test("""
            val v = true and
                true and 10
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun bb_06_op_plus_plus() {
        val out = test("""
            $PLUS
            val v = 5 +
                5 + 10
            print(v)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun bb_07_ops() {
        val out = test("""
            print({{or}}(false, true))
            print({{not}}(true))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun bb_08_ops() {
        val out = test("""
            print({{not}}())
        """)
        assert(out == "anon : (lin 2, col 21) : operation throw : invalid number of arguments\n") { out }
    }

    // LEX / PARSER / EXPANSION

    @Test
    fun bc_01_or_lex() {
        val out = test("""
            val x = do {
                [] or false
            }
            print(x)
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun bc_02_thus_lex() {
        val out = test("""
            val x = do {
                [] thus { it }
            }
            print(x)
        """)
        //assert(out == " |  anon : (lin 2, col 13) : (val x = do { do { (val it = []); it; }; })\n" +
        //        " v  throw : cannot copy reference out\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun bc_03_op_thus_tuple_lex() {
        val out = test("""
            val x = do {
                [] thus { drop(it) }
            }
            print(x)
        """)
        assert(out == "[]\n") { out }
    }

    // IF / ID-TAG

    @Test
    fun cj_02_if() {
        val out = test("""
            data :X = [x]
            val i = if [10] { \v:X => v.x }
            print(i)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cj_03_ifs() {
        val out = test("""
            data :X = [x]
            val i = ifs {
                [10] { \v:X => v.x }
            }
            print(i)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cj_04_ifs() {
        val out = test("""
            data :X = [x]
            val i = match nil {
                |[10] { \v:X => v.x }
            }
            print(i)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cj_05_ifs() {
        val out = test("""
            match nil {
                |10 { \v => print(v) }
            }
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
                print(it)     ;; dcl from last to first
            }            
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ee_02_it() {
        val out = test("""
            val it
            print(it)
            do {
                val it = 10
            }            
        """)
        //assert(out == "anon : (lin 5, col 21) : declaration throw : variable \"it\" is already declared\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun ee_03_it() {
        val out = test("""
            val it
            do {
                val it = 10
            }            
            print(it)
        """)
        assert(out == "nil\n") { out }
        //assert(out == "anon : (lin 4, col 21) : declaration throw : variable \"it\" is already declared\n") { out }
    }
    @Test
    fun ee_04_it() {
        val out = test("""
            val it = 10
            print(__it)
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
            print(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ff_02_ifs() {
        val out = test("""
            val x = ifs { true=> `:number 1` }
            print(x)
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
            print(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_12_ifs() {
        val out = test("""
            ifs {
                true => throw()
            }
        """)
        assert(out == " |  anon : (lin 3, col 25) : throw()\n" +
                " v  throw : nil\n") { out }
    }

    // MATCH

    @Test
    fun ff_03x_ifs() {
        val out = test("""
            val x = match 20 {
                == 20 => true
            }
            print(x)
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
            print(x)
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
            print(x)
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
            print(x)
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
            print(x)
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
            print(x)
        """)
        assert(out == "anon : (lin 5, col 21) : access throw : variable \"is'\" is not declared\n") { out }
    }
    @Test
    fun ff_09_ifs() {
        val out = test("""
            var x = match 20 {
                in? [1,20,1] => true
                else  => false
            }
            print(x)
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
            print(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_10x_ifs() {
        val out = test("""
            $IS ; $COMP
            match 20 {
                :no => print(:no)
            }
            print(:ok)
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
            print(x)
        """)
        assert(out == "true\n") { out }
    }

    // IFS / ORIGINAL

    @Test
    fun fg_01_ifs() {
        val out = test("""
            var x = match ;;;it=;;;20 {
                in-not? [1,1] => true
                else  => false
            }
            print(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun fg_02_ifs() {
        val out = test("""
            val x = match [] {
                it|true => it
            }
            print(x)
        """)
        //assert(out == "anon : (lin 2, col 21) : block escape throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun fg_03_ifs() {
        val out = test("""
            val x = match [] {
                it|true => drop(it)
            }
            print(x)
        """, true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun fg_04_ifs () {
        val out = test("""
            val x = ifs {
                true => it
            }
            print(x)
        """, true)
        assert(out == "anon : (lin 3, col 25) : access throw : variable \"it\" is not declared\n") { out }
    }
    @Test
    fun TODO_fg_05_ifs () {
        val out = test("""
            val x = ifs {
                v=10 => v
            }
            print(x)
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
            print(x)
        """)
        //assert(out == "anon : (lin 3, col 17) : expected expression : have \"{\"") { out }
        //assert(out == "anon : (lin 3, col 17) : access throw : variable \"{{and}}\" is not declared") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun fg_07_ifs () {
        val out = test("""
            val x = match 4 {
                is? nil => false
                in? [4] => true
            }
            print(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun fg_08_ifs () {
        val out = test("""
            and nil
        """)
        //assert(out == "anon : (lin 3, col 17) : expected expression : have \"{\"") { out }
        assert(out == "anon : (lin 2, col 13) : access throw : variable \"{{and}}\" is not declared\n") { out }
    }
    @Test
    fun fg_09_ifs () {
        val out = test("""
            val x = match "oi" {
                {{string?}} { true }
                else => false
            }
            print(x)
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
            print(x)
            ;;;
            val x:X = v[0]
            if x==10 {
                val y:Y = v[1]
                if y==20 {
                    :ok, true, 2
            ;;;
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fh_01_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match [10,20] {
                [30,40] => throw(:no)
                [10,20] => :ok
            }
            print(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fh_01x_ifs () {
        val out = test("""
            $IS ; $COMP
            match [10] {
                [30] => throw(:no)
                [10] => print(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fh_02_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match 1 {
                [1,2] => throw(:no)     ;; 2 compares to nil
                1 => :ok
            }
            print(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fh_02x_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match 1 {
                [1,nil] => (:ok)     ;; 2 compares to nil
                1 => throw(:no)
            }
            print(x)
        """)
        //assert(out == ":ok\n") { out }
        assert(out == " |  anon : (lin 5, col 22) : throw(:no)\n" +
                " v  throw : :no\n") { out }
    }
    @Test
    fun fh_03_ifs () {
        val out = test("""
            $IS ; $COMP
            val x = match [1,2] {
                1 => throw(:no)
                [1,2] => :ok
            }
            print(x)
        """)
        assert(out == ":ok\n") { out }
    }

    // IFS / NO CATCH ALL

    @Test
    fun fi_00_catch_all() {
        val out = test("""
            $ASR
            data :T = [v]
            match [10] {
                do (t :T) => print(t.v)
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun fi_01_ifs_no_catch_all() {
        val out = test("""
            $ASR ; $IS ; $COMP
            data :T = [v]
            var x = match [20] {
                false         => throw()
                ;;(t :T| false) {}
                do (t :T)
                (|t.v == 10)  => throw()
                (|t.v == 20)  => :ok
                else          => throw()
            }
            print(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fi_02_ifs_no_catch_all() {
        val out = test("""
            $ASR ; $IS ; $COMP
            data :T = [v]
            var x = match [20] {
                false         => throw()
                ;;(t :T| false) {}
                do (t :T)
                (|t.v == 10)  => throw()
                (|t.v == 20)  => :ok
                else          => throw()
            }
            print(x)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fi_03_ifs_no_catch_all() {
        val out = test("""
            $ASR ; $IS ; $COMP
            data :T = [v]
            var x = match [20] {
                false         => throw()
                ;;(t :T| false) {}
                do (t :T) {
                    print(:ok)
                }
                (|t.v == 10)  => throw()
                (|t.v == 20)  => :ok
                else          => throw()
            }
            print(x)
        """)
        assert(out == ":ok\n:ok\n") { out }
    }
    @Test
    fun fi_03x_ifs_no_catch_all() {
        val out = test("""
            $ASR ; $IS ; $COMP
            data :T = [v]
            var x = match :T [20] {
                false         => throw()
                ;;(t :T| false) {}
                do (t :T) {
                    print(:ok)
                }
                (|t.v == 10)  => throw()
                (|t.v == 20)  => :ok
                else          => throw()
            }
            print(x)
        """)
        assert(out == ":ok\n:ok\n") { out }
    }
    @Test
    fun fi_04_ifs_no_catch_all() {
        val out = test("""
            ifs {
                do {
                    val v = 10
                    print(:1)
                }
                (v == 10) => print(:2)
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
                    print(:1)
                }
                (v == 10) => (:2)
            }
            print(x)
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun fi_07_ifs_no_catch_all() {
        val out = test("""
            $IS ; $COMP
            data :T = [v]
            var x = ifs {
                false => throw()
                do {
                    val t :T = [20]
                    print(:ok)
                }
                (t.v == 10)  => throw()
                (t.v == 20)  => :ok
                else          => throw()
            }
            print(x)
        """)
        assert(out == ":ok\n:ok\n") { out }
    }

    // PATTS / TUPLES / DCL

    @Test
    fun fj_00() {
        val out = test("""
            $COMP
            match [1,2] {
                [x,y] => print(x,y)
            }
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun fj_01() {
        val out = test("""
            $COMP
            match [1,2] {
                [10,x]  => throw()
                [1,2,3] => throw()
                [1,2]   => print(:ok)
                else    => throw()
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_02() {
        val out = test("""
            $COMP
            match [1,2] {
                [1] => print(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_03() {
        val out = test("""
            $COMP
            match [1,2] {
                [x] => print(x)
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun fj_04() {
        val out = test("""
            $COMP
            match [1,2] {
                [x,|false] => throw()
                [|it==1,y] => print(y)
            }
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun fj_05() {
        val out = test("""
            $IS ; $COMP
            match :X [] {
                :X [1] => throw()
                :Y []  => throw()
                :X []  => print(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_06() {
        val out = test("""
            $COMP
            match [1,[:x,:y],2] {
                [1,xy,3]    => throw()
                [1,[x,y],2] => print(x,y)
            }
        """)
        assert(out == ":x\t:y\n") { out }
    }
    @Test
    fun fj_07_err() {
        val out = test("""
            $COMP
            match nil {
                [] => print(:ok)
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fj_08_val() {
        val out = test("""
            $COMP ; $ASR
            val [x,y]
            print(x,y)
        """)
        assert(out == "nil\tnil\n") { out }
    }
    @Test
    fun fj_09_val() {
        val out = test("""
            $COMP ; $ASR
            val [x,y] = [1,2]
            print(x,y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun fj_10_val() {
        val out = test("""
            match [10,[20]] {
                do [x,[y]] => print(x,y)
            }
            print(:ok)
        """, true)
        assert(out == "10\t20\n:ok\n") { out }
    }
    @Test
    fun fj_11_it_it() {
        val out = test("""
            val v = match 10 {
                |it+1 { \it => it }
            }
            print(v)
        """, true)
        assert(out == "11\n") { out }
    }
    @Test
    fun fj_12_val_err() {
        val out = test("""
            $COMP ; $ASR
            val [x,1] = [1,2]
            print(x,y)
        """)
        assert(out == "anon : (lin 4, col 23) : access throw : variable \"y\" is not declared\n") { out }
    }
    @Test
    fun fj_13_val_err() {
        val out = test("""
            $COMP ; $ASR
            val [x,1] = [1,2]
            print(x)
        """)
        assert(out == " |  anon : (lin 3, col 19) : assert({{==}}(ceu_patt,1),:Patt)\n" +
                " |  anon : (lin 2, col 597) : throw(msg)\n" +
                " v  throw : :Patt\n") { out }
    }

    // CATCH

    @Test
    fun gg_01_catch() {
        val out = test("""
            var x
            set x = catch :x {
                throw(:z, [])
                print(9)
            }[0]
            print(x)
        """, true)
        assert(out == " |  anon : (lin 4, col 17) : throw(:z,[])\n" +
                " v  throw : []\n") { out }
    }
    @Test
    fun gg_02_catch() {
        val out = test("""
            func f (v) {
                false
            }
            catch :z ;;;|false;;; {
                catch :z ;;;err|f(err);;; {
                    throw(:x, [])
                }
            }
            print(`:number CEU_GC.free`)
            print(:ok)
        """)
        assert(out == " |  anon : (lin 7, col 21) : throw(:x,[])\n" +
                " v  throw : []\n") { out }
    }
    @Test
    fun gg_03_catch() {
        val out = test("""
            var x
            set x = catch :x {
                catch :2 {
                    throw(:x, tag(:x, [10]))
                    print(9)
                }
                print(9)
            }[0]
            print(:gc, `:number CEU_GC.free`) ;; TODO: not checked
            print(:x, x)
        """, true)
        assert(out == ":gc\t1\n:x\t10\n") { out }
    }
    @Test
    fun gg_04_catch_err() {
        val out = test("""
            catch :x ;;;err|err==[];;; {
                var x
                set x = []
                throw(:z,x)
                print(9)
            }
            print(1)
        """, true)
        //assert(out == "anon : (lin 5, col 28) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 27) : block escape throw : incompatible scopes\n" +
        //        "anon : (lin 5, col 17) : throw(x)\n" +
        //        "throw throw : uncaught exception\n" +
        //        ":throw\n") { out }
        assert(out == " |  anon : (lin 5, col 17) : throw(:z,x)\n" +
                " v  throw : []\n") { out }
    }
    @Test
    fun gg_05_catch() {
        val out = test("""
            do {
                print(catch :x {
                    throw(:x, tag(:x,[10]))
                    print(9)
                })
            }
        """, true)
        assert(out == ":x [10]\n") { out }
    }
    @Test
    fun gg_06_catch() {
        val out = test("""
            catch :y ;;;|false;;; {
                catch {
                    throw(:x, [10])
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun gg_07_catch() {
        val out = test("""
            var x
            set x = catch :x {
                var y
                set y = catch ;;;|true;;; {
                    throw(:z, [10])
                    print(9)
                }
                ;;print(1)
                y
            }
            print(x)
        """.trimIndent(), true)
        assert(out == " |  anon : (lin 2, col 5) : x\n" +
                " v  throw : cannot copy reference out\n") { out }
        //assert(out == "[10]\n") { out }
    }
    @Test
    fun gg_08_loop_() {
        val out = test("""
            print(catch :x { loop { throw(:x, tag(:x,[1])) }}[0])
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun gg_09_loop() {
        val out = test("""
            print(catch :x { loop { throw(:x,tag(:x,[1])) }}[0])
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun gg_10_loop_() {
        val out = test("""
            print(catch :2 { loop { throw(:2,tag(:2,[1])) }})
        """, true)
        assert(out == ":2 [1]\n") { out }
    }
    @Test
    fun gg_11_loop() {
        val out = test("""
            print(catch :x { loop {
                var x
                set x = [1] ;; memory released
                throw(:x, tag(:x,[1]))
            }}[0])
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun gg_12_loop_err() {
        val out = test("""
            print(catch :x { loop {
                var x
                set x = [1]
                throw(:x, tag(:x,x))
            }})
        """, true)
        assert(out == ":x [1]\n") { out }
        //assert(out == "anon : (lin 4, col 14) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 1, col 33) : block escape throw : incompatible scopes\n" +
        //        "anon : (lin 4, col 5) : throw(tag(x,:x,true))\n" +
        //        "throw throw : uncaught exception\n" +
        //        ":throw\n") { out }
    }
    @Test
    fun gg_13_catch() {
        val out = test("""
            catch :x ;;;err|err===[];;; {
                throw(:x,[])
                print(9)
            }
            print(1)
        """, true)
        assert(out == "1\n") { out }
    }

    // ENUM / TAGS / TEMPLATES

    @Test
    fun hi_01_tags() {
        val out = test("""
            val x  = tag(:X,[])
            val xy = tag(:Y, tag(:X,[]))
            print(x, xy)
        """)
        //assert(out == ":X []\t[:Y,:X] []\n") { out }
        assert(out == ":X []\t:Y []\n") { out }
    }
    @Test
    fun hi_02_tags() {
        val out = test("""
            data :T = [x]
            val x = :T [1]
            print(x, tag(x))
        """)
        assert(out == ":T [1]\t:T\n") { out }
    }
    @Test
    fun hi_03_tags() {
        val out = test("""
            data :T = [x]
            val x = :T [1]
            val y = x.(:T).x
            print(y)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun hi_04_tags() {
        val out = test("""
            data :T = [x]
            val x = :T [1]
            print(x.x, tag(x))
        """)
        assert(out == "1\t:T\n") { out }
    }
    @Test
    fun hi_04x_tags() {
        val out = test("""
            $ASR
            data :T = [x]
            val [a:T,b:T] = [:T [1], :T [2]]
            print(a.x, b.x)
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
            print(t.pub.x)
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
            print(t.(:T).pub)
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
            print(p)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun hi_08_tags_ops() {
        val out = test("""
            :x :y
            print(:y - 1)
            print(1 + :x)
            print(:x <= :y)
            print(:x > :y)
            print(:y - :x)
            print(:x + :y)
        """, true)
        assert(out == ":x\n" +
                ":y\n" +
                "true\n" +
                "false\n" +
                "1\n" +
                " |  anon : (lin 8, col 24) : {{+}}(:x,:y)\n" +
                " |  build/prelude-x.ceu : (lin 8, col 17) : throw(:throw)\n" +
                " v  throw : :throw\n") { out }
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
            print(t is? :T, t is? :T.S)
            print(s is? :T, s is? :T.S)
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
            print(s is? :T, s.z is? :U)
            set s.z = :U [10]
            print(s is? :T.S, s.z is? :U)
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
            print(a.a, b.t, c.q)
            print(a is? :T, b is? :T.C, c is? :T.C.Q.Y)
        """, true)
        assert(out == "20\t30\t50\ntrue\tfalse\ttrue\n") { out }
    }
    @Test
    fun TODO_hj_04_tplate() {
        val out = test("""
            data :T = [x,y]
            val t :T = [x=1,y=2]    ;; TODO: syntax sugar
            set t.x = 3
            print(t)      ;; [x=3,y=2]
        """, true)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun hj_05_tplate_err() {
        val out = test("""
            data :T = [v]
            data :U = [t:T,X]
            var u :U = [[10]]
            print(u.X.v)
        """, true)
        //assert(out == "anon : (lin 5, col 25) : index throw : field \"X\" is not a data") { out }
        assert(out == " |  anon : (lin 5, col 21) : u[:X]\n" +
                " v  throw : out of bounds\n") { out }
    }
    @Test
    fun hj_06_tplate_tup() {
        val out = test("""
            data :T = [v]
            val t :T = [[1,2,3]]
            print(t.v[1])
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
            print(x)
            print(x.v)
            print(to-tag-string(":T.A.z"))
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
            print(v)
        """)
        //assert(out == "10\n") { out }
        assert(out == "anon : (lin 4, col 19) : expected \"{\" : have \":T\"\n") { out }
    }
    @Test
    fun hj_09_tplate_nest() {
        val out = test("""
            data :X = [v, t=[a,b]]
            val x :X = [10, [1,2]]
            print(x.v, x.t.a, x.t.b)
        """)
        assert(out == "10\t1\t2\n") { out }
    }
    @Test
    fun hj_10_tplate_nest() {
        val out = test("""
            data :X = [v, t=[a,z=[i,j]]]
            val x :X = [10, [:a,[1,2]]]
            print(x.v, x.t.a, x.t.z.j)
        """)
        assert(out == "10\t:a\t2\n") { out }
    }
    @Test
    fun hj_11_tplate_nest() {
        val out = test("""
            data :X = [v, t :T=[a,b]]
            val x :X = [10, [1,2]]
            val t :T = x.t
            print(t.a, t.b)
        """)
        assert(out == "1\t2\n") { out }
    }

    // ENUM

    @Test
    fun ii_01_enum() {
        val out = test(
            """
            ;;;do;;; :antes
            enum {
                :x ;;;= `1000`;;;,
                :y, :z,
                :a;;; = `10`;;;,
                :b, :c
            }
            ;;;do;;; :meio
            enum {
                :i;;; = `100`;;;,
                :j,
            }
            ;;;do;;; :depois
            val n = to.number(:antes)
            print (
                to.number(:antes) - n,
                to.number(:x) - n,
                to.number(:y) - n,
                to.number(:z) - n,
                to.number(:a) - n,
                to.number(:b) - n,
                to.number(:c) - n,
                to.number(:meio) - n,
                to.number(:i) - n,
                to.number(:j) - n,
                to.number(:depois) - n
            )
        """, true
        )
        assert(out == "0\t1\t2\t3\t4\t5\t6\t7\t-6\t8\t9\n") { out }
        //assert(out == "15\t1000\t1001\t1002\t10\t11\t12\t16\t100\t101\t17\n") { out }
    }
    @Test
    fun ii_02_enum() {
        val out = test(
            """
            enum :X {
                a, b
                ;;:x = `1000`,
                ;;:y = `1000`,
            }
            print(:tag, to.number(:X)<to.number(:X-a), :X-a, :X-b)
        """, true)
        assert(out == ":tag\ttrue\t:X-a\t:X-b\n") { out }
        //assert(out == ":tag\t:y\t:1000\t:y\n") { out }
    }
    @Test
    fun ii_03_enum() {
        val out = test(
            """
            enum {
                :x.y
            }
            print(:tag, :x, :1000, :y)
        """
        )
        assert(out == "anon : (lin 3, col 17) : enum throw : enum tag cannot contain '.'\n") { out }
    }
    @Test
    fun ii_04_enum() {
        val out = test(
            """
            enum { :x, :y }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ii_05_enum() {
        val out = test("""
            ;;;do;;; :antes
            enum {
                :x ;;;= `1000`;;;,
                :y, :z,
                :a ;;;= `10`;;;,
                :b, :c
            }
            ;;;do;;; :meio
            enum {
                :i ;;;= `100`;;;,     ;; ignored b/c of itr.i in to-iter-tuple
                :j,
            }
            ;;;do;;; :depois
            val t = [:antes, :x, :y, :z, :a, :b, :c, :meio, :i, :j, :depois]
            loop [i,v] in to.iter(t, [:idx,:val]) {
                set t[i] = to.number(v)
            }
            print(t)
        """, true)
        assert(out == "[46,47,48,49,50,51,52,53,40,54,55]\n") { out }
        //assert(out == "[42,1000,1001,1002,10,11,12,43,36,101,44]\n") { out }
    }

    // BREAK / SKIP / RETURN

    @Test
    fun ja_01_break_err() {
        val out = test("""
            break()
        """)
        assert(out == "anon : (lin 2, col 13) : escape throw : expected matching enclosing block\n") { out }
    }
    @Test
    fun ja_02_skip_err() {
        val out = test("""
            skip()
        """)
        assert(out == "anon : (lin 2, col 13) : escape throw : expected matching enclosing block\n") { out }
    }
    @Test
    fun ja_03_return_err() {
        val out = test("""
            return()
        """)
        assert(out == "anon : (lin 2, col 13) : escape throw : expected matching enclosing block\n") { out }
    }
    @Test
    fun ja_04_break() {
        val out = test("""
            print(loop { break(10) })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ja_05_return() {
        val out = test("""
            func f () {
                loop {
                    return(10)
                }
            }
            print(f())
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ja_06_break_not_optim() {
        val out = test("""
            loop {
                if false {
                    break()
                }
                ``` // asserts that :break is not optimized out
                CEU_ESCAPE = CEU_TAG_break;
                continue;
                ```
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ja_07_skip_optim() {
        val out = test("""
            loop {
                ;;break()
                ``` // asserts that :skip is optimized out
                CEU_ESCAPE = CEU_TAG_skip;
                continue;
                ```
            }
            print(:ok)
        """)
        assert(out.contains("main: Assertion `CEU_ESCAPE == CEU_ESCAPE_NONE' failed.\n")) { out }
    }
    @Test
    fun ja_08_return_optim() {
        val out = test("""
            func () {
                ``` // asserts that :return is optimized out
                CEU_ESCAPE = CEU_TAG_return;
                continue;
                ```
            } ()
            print(:ok)
        """)
        assert(out.contains(": Assertion `CEU_ESCAPE == CEU_ESCAPE_NONE' failed.\n")) { out }
    }

    @Test
    fun jj_01_break() {
        val out = test("""
            loop {
                if true {
                    break(nil)
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_02_break() {
        val out = test("""
            loop {
                break(nil)
                do {
                    skip()
                }
            }
            print(:out)
        """)
        assert(out == ":out\n") { out }
        //assert(out == "anon : (lin 4, col 21) : skip throw : expected immediate parent loop\n") { out }
    }
    @Test
    fun jj_03_skip() {
        val out = test("""
            var ok = false
            loop {
                if ok {
                    break(nil)
                } else { nil }
                set ok = true
                skip()
            }
            print(:out)
        """)
        assert(out == ":out\n") { out }
    }
    @Test
    fun jj_04_break() {
        val out = test("""
            do {
                loop {
                    print(:in)
                    if true {
                        break(nil)
                    }
                }
            }
            print(:out)
        """
        )
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun jj_05_break_err() {
        val out = test("""
            loop {
                func () {
                    break (nil)
                }
            }
        """)
        //assert(out == "anon : (lin 4, col 21) : break throw : expected immediate parent loop\n") { out }
        //assert(out == "anon : (lin 4, col 21) : break throw : expected parent loop\n") { out }
        assert(out == "anon : (lin 4, col 21) : escape throw : expected matching enclosing block\n") { out }
    }
    @Test
    fun jj_06_break_sum() {
        val out = test("""
            $PLUS
            var sum = func (n) {                                                            
                var i = n                                                                   
                var s = 0                                                                   
                loop {                                                                      
                    if i == 0 {
                        break(s)
                    } else {nil}
                    set s = s + i                                                           
                    set i = i - 1                                                           
                }                                                                           
            }                                                                               
            print(sum(5))                                                                
        """)
        assert(out == "15\n") { out }
    }

    // ERROR / ASSERT

    @Test
    fun kk_01_error() {
        val out = test("""
            throw(:X [:x])
        """)
        assert(out == " |  anon : (lin 2, col 13) : throw(tag(:X,[:x]))\n" +
                " v  throw : :X [:x]\n") { out }
    }
    @Test
    fun kk_02_error() {
        val out = test("""
            catch {
                throw(:X [:x])
            } thus {
                print(it)
            }
        """)
        assert(out == ":X [:x]\n") { out }
    }
    @Test
    fun za_09_assert() {
        val out = test("""
            assert(false, 10)
        """, true)
        assert(out == " |  anon : (lin 2, col 13) : assert(false,10)\n" +
                " |  build/prelude-x.ceu : (lin 42, col 30) : throw(:throw,msg)\n" +
                " v  throw : 10\n") { out }
    }
    @Test
    fun za_10_assert() {
        val out = test("""
            assert(false, :type [])
        """, true)
        assert(out == " |  anon : (lin 2, col 13) : assert(false,tag(:type,[]))\n" +
                " |  build/prelude-x.ceu : (lin 41, col 30) : throw(msg)\n" +
                " v  throw : :type []\n") { out }
    }
    @Test
    fun za_11_assert() {
        val out = test("""
            val v = catch :X {
                assert(false, :X [:x])
            }
            print(v)
        """, true)
        assert(out == ":X [:x]\n") { out }
    }

    // THUS / SCOPE / :FLEET / :fleet

    @Test
    fun mm_01_tmp() {
        val out = test(
            """
            var x
            do {
                [1,2,3] thus { \a =>
                    set x = a
                }
            }
            print(x)
        """
        )
        assert(out == "[1,2,3]\n") { out }
        //assert(out == " v  anon : (lin 5, col 25) : set throw : cannot assign reference to outer scope\n") { out }
        //assert(out == " |  anon : (lin 5, col 25) : x\n" +
        //        " v  throw : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_01_tmp_err() {
        val out = test(
            """
            var x
            do {
                [1,2,3] thus { \a =>
                    set x = drop(a)
                }
            }
            print(x)
        """
        )
        assert(out == "[1,2,3]\n") { out }
        //assert(out == "anon : (lin 5, col 34) : drop throw : value is not movable\n") { out }
    }
    @Test
    fun mm_01_tmp_ok() {
        val out = test(
            """
            val x = do {
                [1,2,3] thus { \a =>
                    drop(a)
                }
            }
            print(x)
        """
        )
        assert(out == "[1,2,3]\n") { out }
        //assert(out == "anon : (lin 5, col 25) : set throw : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_02_thus_err() {
        val out = test("""
            var x
            nil thus { \it =>
                set x = 10  ;; err
            }
            print(x)
        """)
        //assert(out == "anon : (lin 4, col 17) : set throw : destination across thus\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_03_thus_err() {
        val out = test("""
            var x
            nil thus { \it =>
                set x = it  ;; err
                print(x)
            }
        """)
        //assert(out == "anon : (lin 4, col 17) : set throw : destination across thus\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun mm_04_tmp() {
        val out = test(
            """
            [0] thus { \x =>
                set x[0] = []
                print(x)
            }
        """
        )
        assert(out == "[[]]\n") { out }
    }
    @Test
    fun mm_05_tmp() {
        val out = test("""
            val v = do {
                [] thus { \x =>
                    if x { x } else { [] }
                }
            }
            print(v)
        """)
        //assert(out == " |  anon : (lin 2, col 13) : (val v = do { do { (val x = []); if x { x;...\n" +
        //        " v  throw : cannot copy reference out\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun mm_05_tmp_x() {
        val out = test("""
            val v = do {
                [] thus { \x =>
                    if x { drop(x) } else { [] }
                }
            }
            print(v)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 4, col 33) : drop throw : value is not movable\n") { out }
    }
    @Test
    fun mm_06_tmp_err() {
        val out = test("""
            val v = do {
                val x = []
                if x { x } else { [] }
            }
            print(v)
        """)
        //assert(out == " v  anon : (lin 2, col 21) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == "[]\n") { out }
        assert(out == " |  anon : (lin 2, col 13) : (val v = do { (val x = []); if x { x; } el...\n" +
                " v  throw : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_07_and_or() {
        val out = test("""
            val t = func () { print(:t) ; true  }
            val f = func () { print(:f) ; false }
            print(${AND("t()", "f()")})
            print(${OR("t()", "f()")})
            print(${AND("[]", "false")})
            print(${OR("false", "[]")})
        """)
        assert(out == ":t\n:f\nfalse\n:t\ntrue\nfalse\n[]\n") { out }
    }
    @Test
    fun mm_08_fleet_tuple_func_err() {
        val out = test("""
            var f = func (v) {
                v[0] thus { \it =>
                    print(it)
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
                await(true) thus { \it => set it = nil }
            }) ()
        """)
        assert(out == "anon : (lin 3, col 42) : set throw : destination is immutable\n") { out }
    }
    @Test
    fun mm_10_yield_err() {
        val out = test("""
            resume (coroutine (coro () {
                await(true) thus { \it => await(true) thus { \x => nil } }
            })) ()
            print(:ok)
        """)
        //assert(out == "anon : (lin 3, col 41) : yield throw : unexpected enclosing func\n") { out }
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 3, col 41) : yield throw : unexpected enclosing thus\n") { out }
    }
    @Test
    fun mm_11_resume_yield() {
        val out = test("""
            $PLUS
            val CO = coro () {
                await(true) thus {\ it => 
                    print(it)
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
                yield(v1) thus { \ x => x }
            }
            val co = coroutine(CO)
            val v1 = resume co(10)
            val v2 = resume co(v1)
            print(v2)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_13_tags() {
        val out = test("""
            val CO = coro () {
                await(true) thus { \ it =>
                    print(sup?(:X,tag(it))) ;; drop(it)
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
                await(true) thus {\ v =>
                    print(v)
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
                await(true) thus { \it :T =>
                    it[0]
                }
            }
            print(:ok)
        """)
        //assert(out == "anon : (lin 3, col 38) : declaration throw : data :T is not declared\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun mm_16_scope() {
        val out = test("""
            val T = coro () {
                val v = await(true) thus { \x => x }
                await(true) ;;thus { \it => nil }
                print(v)                
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
        //assert(out == "[]\n") { out }
        assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
                " v  throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 41) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " |  anon : (lin 3, col 41) : (func (x) { x })(yield(nil))\n" +
        //        " v  anon : (lin 3, col 41) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 25) : resume throw : cannot receive alien reference\n") { out }
    }
    @Test
    fun mm_17_catch_yield_err() {
        val out = test("""
            coro () {
                catch ;;;(it| do {
                    await(true) thus {\ it => nil }
                } );;;
                {
                    throw(:e1)
                }
            }
            print(:ok)
        """)
        //assert(out == "anon : (lin 4, col 39) : declaration throw : variable \"it\" is already declared\n") { out }
        //assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing catch\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun mm_17a_catch_yield_err() {
        val out = test("""
            coro () {
                catch ;;;;(it| do {
                    ;;;do;;; it
                    await(true) thus { \it => nil }
                } );;;;
                {
                    throw(:e1)
                }
            }
            print(:ok)
        """)
        //assert(out == "anon : (lin 5, col 39) : declaration throw : variable \"it\" is already declared\n") { out }
        //assert(out == "anon : (lin 5, col 21) : yield throw : unexpected enclosing catch\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun mm_18_it() {
        val out = test("""
            val CO = coro () {
                await(true) thus { \it =>
                    print(:it, it)
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
                await(true) thus { \x =>
                    print(:it, x)
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
                await(true) thus {\ x =>
                    print(:it, x)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co([])
        """,)
        assert(out == "anon : (lin 3, col 36) : declaration throw : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun mm_21_it_data() {
        val out = test("""
            data :X = [x]
            val CO = coro () {
                await(true) thus { \x :X =>
                    print(:it, x.x)
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
                await(true) thus { \x =>
                    await(true) thus { \x =>
                        x
                    }
                }
            }
        """,)
        //assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing yield\n") thus { out }
        assert(out == "anon : (lin 4, col 40) : declaration throw : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun mm_23_scope() {
        val out = test("""
            val T = coro () {
                val v = await(true) thus { \it => 
                    print(it)
                    10
                }
                await(true) ;;thus {\ it => nil }
                print(v)                
            }
            val t = coroutine(T)
            resume t()
            do {
                val v = []
                resume t(drop(v))
            }
            resume t()
        """)
        assert(out == "[]\n10\n") { out }
        //assert(out == " |  anon : (lin 14, col 17) : (resume (t)(v))\n" +
        //        " v  anon : (lin 3, col 25) : resume throw : cannot receive alien reference\n") { out }
    }
    @Test
    fun mm_24_yield() {
        val out = test("""
            coro () {
                await(true) thus {\ x =>
                    await(true) thus { \y => nil }
                }
            }
            print(:ok)
        """)
        //assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing func\n") { out }
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing thus\n") { out }
    }
    @Test
    fun mm_25_gc_bcast() {
        DEBUG = true
        val out = test("""
            var tk = task () {
                await(true) thus {\ it =>
                    do {
                        val xxx = it
                        nil
                    }
                }
                nil
                ;;print(:out)
            }
            var co = spawn tk ()
            emit ([])
            print(`:number CEU_GC.free`)
        """)
        //assert(out == "0\n") { out }
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 11, col 13) : emit []\n" +
        //        "anon : (lin 5, col 21) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
    }
    @Test
    fun mm_26_term() {
        val out = test("""
            spawn( task () {
                val t = spawn (task () {
                    await(true) ;;thus { \it => nil }
                    10
                } )()
                yield (nil) thus {\ it => print(it.pub) }
            } )()
            emit(true)
            print(:ok)
       """)
        assert(out == "10\n:ok\n") { out }
    }
    @Test
    fun mm_27_bcast_err() {
        val out = test(
            """
            var T = task () {
                var v =
                await(true) thus { \it => it}
                print(v)
            }
            var t = spawn T()
            ;;print(:1111)
            do {
                val a
                do {
                    val b
                    var e = []
                    emit (drop(e))
                }
            }
            ;;print(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : emit e\n" +
        //        " v  anon : (lin 4, col 17) : resume throw : cannot receive assigned reference\n") { out }
        //assert(out == "anon : (lin 11, col 39) : emit throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : emit'(e,:task)\n" +
        //        " |  anon : (lin 4, col 33) : (func (it) { it })(yield(nil))\n" +
        //        " v  anon : (lin 4, col 33) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : emit'(e,:task)\n" +
        //        " v  anon : (lin 4, col 33) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : emit'(e,:task)\n" +
        //        " v  anon : (lin 4, col 35) : declaration throw : cannot hold alien reference\n") { out }
    }
    @Test
    fun mm_28_data_await() {
        val out = test("""
            data :E = [x,y]
            spawn (task () {
                await(true) thus {\ it :E =>
                    print(it.x)
                }
            } )()
            emit (tag(:E, [10,20]))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_29_data_await() {
        val out = test("""
            data :E = [x,y]
            data :F = [i,j]
            spawn (task () {
                await(true) thus { \it :E =>
                    print(it.x)
                }
                await(true) thus { \it :F =>
                    print(it.j)
                }
            } )()
            emit (tag(:E, [10,20]))
            emit (tag(:F, [10,20]))
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
            print(v)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun mm_31_bcast_nil() {
        val out = test("""
            spawn (task () {
                print(yield(nil))
                print(:ok)
            } )()
            emit()
        """)
        assert(out == "nil\n:ok\n") { out }
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
                print(v)
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
                print(v)
                break() ;; if true
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
                print(t)
            }
        """, true)
        //assert(out.contains("track: 0x")) { out }
        assert(out.contains("exe-task: 0x")) { out }
    }
    @Test
    fun fh_02_num() {
        val out = test("""
            loop i in {0 => 1} {
                print(i)
            }
        """, true)
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun fg_05_dict_iter_nil() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop k in to-iter(t,:key) {
                print(k)
            }
        """, true)
        assert(out == ":x\n:y\n:z\n") { out }
    }
    @Test
    fun fg_06_dict_iter_val() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop v in to-iter(t,:val) {
                print(v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun fg_07_dict_iter_key() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop k in to-iter(t,:key) {
                print(k)
            }
        """, true)
        assert(out == ":x\n:y\n:z\n") { out }
    }
    @Test
    fun fg_08_dict_iter_all() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop [v,k] in to-iter(t,[:val,:key]) {
                print(k,v)
            }
        """, true)
        assert(out == ":x\t1\n:y\t2\n:z\t3\n") { out }
    }
    @Test
    fun fg_08x_dict_iter_all() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop [k,v] in to-iter(t) {
                print(k,v)
            }
        """, true)
        assert(out == ":x\t1\n:y\t2\n:z\t3\n") { out }
    }
    @Test
    fun fg_09_dict_iter() {
        val out = test("""
            val v =
                match 0 {
                    |true => []
                }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fg_10_vect_iter_nil() {
        val out = test("""
            loop v in #[10, 20, 30] {
                print(v)
            }
        """, true)
        assert(out == "10\n20\n30\n") { out }
    }
    @Test
    fun fg_11_vect_iter_val() {
        val out = test("""
            val t = #[10, 20, 30]
            loop i in to-iter(t,:idx) {
                print(i)
            }
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun fg_12_vect_iter_all() {
        val out = test("""
            loop [v,i] in to-iter(#[10, 20, 30], [:val,:idx]) {
                print(i,v)
            }
        """, true)
        assert(out == "0\t10\n1\t20\n2\t30\n") { out }
    }
    @Test
    fun fg_13_vect_iter_idx() {
        val out = test("""
            val t = #[1, 2, 3]
            loop v in to-iter(t,:idx) {
                print(v)
            }
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun fg_14_vect_iter_err() {
        val out = test("""
            val t = #[1, 2, 3]
            loop [i in to-iter(t) {
                print(i, v)
            }
        """, true)
        //assert(out == "anon : (lin 3, col 36) : expected \",\" : have \"{\"") { out }
        //assert(out == "anon : (lin 3, col 30) : expected identifier : have \"(\"") { out }
        //assert(out == "anon : (lin 3, col 18) : expected \"in\" : have \"[\"\n") { out }
        assert(out == "anon : (lin 3, col 21) : expected \",\" : have \"in\"\n") { out }
    }
    @Test
    fun fg_15_dict_iter() {
        val out = test("""
            val t = @[x=1, y=2, z=3]
            loop [v,k] in to-iter(t,[:val,:key]) {
                print(k, v)
            }
        """, true)
        assert(out == ":x\t1\n:y\t2\n:z\t3\n") { out }
    }
    @Test
    fun fg_16_string_concat() {
        val out = test("""
            $PLUS ; $COMP
            func f (v1, v2) {
                ;;print(:X, v1, v2)
                loop i in {0 => #v2{ {
                    ;;print(i, v2[i])
                    set v1[+] = v2[i]
                }
                v1
            }
            val s = #[]
            val t = #[1]
            print(f(s,t))
        """)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun fg_16x_string_concat() {
        val out = test("""
            val s = #[]
            ;;print(#['9'])
            s <++ #['1']
            s <++ #['2']
            s <++ #['3']
            print(s)
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
            print(g())
        """, true)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun fg_18_string() {
        val out = test("""
            val v = ""
            print(v)
            `printf(">%s<\n", ${D}v.Dyn->Vector.buf);`
        """)
        assert(out == "#[]\n><\n") { out }
    }
    @Test
    fun fg_19_tuple_size() {
        val out = test("""
            val t = [1, 2, 3]
            print(#t)
        """)
        assert(out == "3\n") { out }
    }
    @Test
    fun fg_20_tuple_iter() {
        val out = test("""
            val t = [1, 2, 3]
            loop [v,k] in to-iter(t,[:val,:key]) {
                print([k,v])
            }
        """, true)
        assert(out == "[0,1]\n[1,2]\n[2,3]\n") { out }
    }
    @Test
    fun fg_21_dict_iter_it() {
        val out = test("""
            loop in @[x=1, y=2, z=3] {
                print(it)
            }
        """, true)
        //assert(out == ":x\n:y\n:z\n") { out }
        //assert(out == "1\n2\n3\n") { out }
        assert(out == "[:x,1]\n[:y,2]\n[:z,3]\n") { out }
    }
    @Test
    fun fg_22_tuple_iter_tag() {
        val out = test("""
            data :T = [v]
            val t = [[1], [2], [3]]
            loop v:T in to-iter(t) {
                print(v.v)
            }
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun fg_23_loop_num() {
        val out = test("""
            loop i {
                print(i)
                until i == 3
            }
        """, true)
        assert(out == "0\n1\n2\n3\n") { out }
    }
    @Test
    fun fg_24_loop_num() {
        val out = test("""
            print(:0)
            loop a in }0 => 1} {
                print(a)
            }
            print(:1)
            loop b in }0 => 3{ {
                print(b)
            }
            print(:2)
            loop c in {0 => 4} :step +2 {
                print(c)
            }
            print(:3)
            loop d in }2 => 0} :step -1 {
                print(d)
            }
            print(:4)
            loop in {0 => -2{ :step -1 {
                print(:x)
            }
            print(:5)
            loop in {1 => 2} {
                print(:y)
            }
            print(:6)
        """, true)
        assert(out == ":0\n1\n:1\n1\n2\n:2\n0\n2\n4\n:3\n1\n0\n:4\n:x\n:x\n:5\n:y\n:y\n:6\n") { out }
    }
    @Test
    fun fg_25_loop_num_it() {
        val out = test("""
            loop in {0 => 1} {
                print(it)
            }
        """, true)
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun fg_26_loop_num() {
        val out = test("""
            $PLUS ; $COMP
            loop in {0 => -2{ {
                nil
            }
            loop in {1 => 2} {
                nil
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
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
                print(v)
            }
        """, true)
        assert(out == "0\n1\n2\n3\n4\n") { out }
    }
    @Test
    fun fx_02_iter_err() {
        val out = test("""
            loop v in nil {
                print(v)
            }
        """, true)
        //assert(out.contains("assertion throw : expected :Iterator")) { out }
        assert(out.contains(" |  anon : (lin 2, col 23) : to-iter(nil)\n" +
                " |  build/prelude-x.ceu : (lin 199, col 28) : throw(:throw,#['i','n','v','a','l','i','d'...\n" +
                " v  throw : invalid collection\n")) { out }
    }
    @Test
    fun fx_03_iter() {
        val out = test("""
            val y = loop x in to-iter([1,2,3]) {
            until x == 2 }
            print(y)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun fx_04_iter() {
        val out = test("""
            val y = loop x in [1,2,3] {
            until x == 4 }
            print(y)
        """, true)
        assert(out == "false\n") { out }
        //assert(out == "nil\n") { out }
    }
    @Test
    fun fx_05_iter_it() {
        val out = test("""
            val y = loop in to-iter([1,2,3]) {
            until it == 4 }
            print(y)
        """, true)
        assert(out == "false\n") { out }
        //assert(out == "nil\n") { out }
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
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 13, col 33) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
    }
    @Test
    fun TODO_fx_07_drop_prime() {
        // drop' was removed
        // this is an example in which drop' is required
        val out = test("""
            val F = func (x) {
                coro () {
                    yield(drop ;;;';;;(x))  ;; x is an upval
                } --> {
                    to-iter(it)
                }
            }
            do {
                val x = []
                val itr :Iterator = F(drop(x))
                print(itr.f(itr))
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
                        yield(drop(pos))
                    }
                } --> {
                    to-iter(it)
                }
            }
            do {
                val x :Iterator = F()
                x.f(x) --> { }
                x.f(x)
            }
            print(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fx_09_iter_nil() {
        val out = test("""
            val t = [1,nil,3]
            loop v in t {
                print(v)
            }
        """, true)
        //assert(out == "1\nnil\n3\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun fx_10_eq() {
        val out = test("""
            val t1 = [1,nil,3]
            val t2 = [1,nil,4]
            print(t1 === t2)
        """, true)
        assert(out == "false\n") { out }
    }
    @Test
    fun fx_11_iter() {
        val out = test("""
            func f (t) {
                if t[1] == t[2] {
                    nil
                } else {
                    set t[1] = t[1] + 1
                    t[1]
                }
            }
            val it = :Iterator [f, 0, 5]
            loop v in it {
                until not v
                print(v)
            }
        """, true)
        assert(out == "1\n2\n3\n4\n5\n") { out }
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
            print(t2)
            val t3 = #[]
            loop v in to-iter(t2) {
                set t3[+] = v
            }
            print(t3)
        """, true)
        assert(out == "#[[1],[2],[3]]\n" +
                "#[[1],[2],[3]]\n") { out }
    }
    @Test
    fun fh_01x_iter() {
        val out = test("""
            val t2 = #[[1],[2],[3]]
            val t3 = #[]
            loop v in to-iter(t2) {
                set t3[+] = v
            }
            print(t3)
        """, true)
        assert(out == "#[[1],[2],[3]]\n") { out }
    }
    @Test
    fun fh_01y_iter() {
        val out = test("""
            val t2 = [1,2,3]
            val t3 = #[]
            loop [v,i] in to-iter(t2,[:val,:idx]) {
                ;;print(i, v)
                set t3[+] = v
            }
            print(t3)
        """, true)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun fh_01z_iter() {
        val out = test("""
            $PLUS ; $ASR
            func iter-tuple (itr) {
                val i = itr[2]
                if i == #itr[1] {
                    set itr[0] = nil
                    nil
                } else {
                    set itr[2] = i + 1
                    [i, itr[1][i]]
                }
            }
            func to-iter (v) { v }
            val t2 = [1,2,3]
            val t3 = #[]
            data :Iterator = [f,s,i]
            loop [i,v] in [iter-tuple, t2, 0, [:idx,:val]] {
                ;;print(i, v)
                set t3[+] = v
            }
            print(t3)
        """)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun fh_02() {
        val out = test("""
            coro genFunc () {
                var v1 = [0,'a']
                yield(drop(v1))
                var v2 = [1,'b']
                yield(drop(v2))
            }
            loop v in genFunc {
                print(v)
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
            f(drop(co))
        }
        val x = g()
        print(x)
        """, true)
        assert(out.contains("[func: 0x")) { out }
    }
    @Test
    fun fh_04_drop() {
        val out = test("""
            val F = func (x) {
                val co = coroutine (coro () {
                    await(true)
                    x
                })
                resume co()
                drop(co)
            }
            do {
                val x = []
                val co = F(x)
                print(resume co())
            }
        """)
        assert(out == "[]\n") { out }
    }

    // ITER / NEXT

    @Test
    fun TODO_multi_fi_01_iter_next() {
        val out = test("""
            val itr :Iterator = to-iter([1,2,3,4])
            print(itr[0](itr))
            print(itr.f(itr))
            print(next(itr))
            print(itr->next())
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
            print(co->next(1))
            print(co->next(2))
            print(co->next(3))
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }

    // LOOP / RET / UNTIL

    @Test
    fun fi_01_ret() {
        val out = test("""
            print(loop i in {0 => 1} {
                ;;;do;;; nil
            })
        """, true)
        assert(out == "false\n") { out }
    }
    @Test
    fun fi_02_ret() {
        val out = test("""
            print(loop i in {0 => 1} {
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
            print(loop i in ts {
                ;;;do;;; nil
            })
        """, true)
        //assert(out == "nil\n") { out }
        assert(out == "false\n") { out }
    }
    @Test
    fun fi_04_ret() {
        val out = test("""
            val ts = tasks()
            spawn ((task(){yield()})()) in ts
            print(loop i in ts {
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
                print(it)
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
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun fi_07_until() {
        val out = test("""
            print(loop {
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
                print(x)
            until x == 3
            }
            print(99)
        """, true)
        assert(out == "1\n2\n3\n99\n") { out }
    }
    @Test
    fun fi_09_until() {
        val out = test("""
            var x = 0
            val v = loop {
                set x = x + 1
                print(x)
                until x == 3
                until false
            }
            print(v)
        """, true)
        assert(out == "1\n2\n3\ntrue\n") { out }
    }
    @Test
    fun fi_10_until() {
        val out = test("""
            print(0)
            loop {
                print(1)
                until true
                print(2)
            }
            print(3)
        """)
        assert(out == "0\n1\n3\n") { out }
    }
    @Test
    fun fi_11_until() {
        val out = test("""
            print(0)
            var x = false
            loop {
                print(1)
                until x
                set x = true
                print(2)
            }
            print(3)
        """)
        assert(out == "0\n1\n2\n1\n3\n") { out }
    }
    @Test
    fun fi_12_until() {
        val out = test("""
            print(0)
            var x = false
            loop {
                print(1)
                until x
                set x = true
                print(2)
                until x
                print(3)
                until x
                print(4)
            }
            print(5)
        """)
        assert(out == "0\n1\n2\n5\n") { out }
    }
    @Test
    fun fi_13_until() {
        val out = test("""
            var x = 0
            loop {
                set x = x + 1
                print(x)
                until v = (x == 3)  ;; TODO: declare var on until?
                print(v)
            }
            print(99)
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
                print(v1)
                while v2=f()  ;; TODO: declare var on while?
                print(v2)
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
            print(v)
        """, true)
        //assert(out == "nil\n") { out }
        assert(out == "false\n") { out }
    }
    @Test
    fun fi_16_while() {
        val out = test("""
            val v = loop { while false ;;;do;;; nil }
            print(v)
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
                break(;;;drop;;;(t)) ;;if true
            }
            print(x)
        """, true)
        //assert(out == (" v  anon : (lin 7, col 13) : declaration throw : cannot copy reference out\n")) { out }
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
                break(copy(t)) ;;if true
            }
            print(x)
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
                print(v)
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
                    print(i)
                    throw(:x)
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
                print(i)
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
                print(i)
            }
        """, true)
        //assert(out == "anon : (lin 12, col 57) : resume throw : expected yielded task\n1\n2\n3\n:throw\n") { out }
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
                print(i)
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
            print(to.vector(coroutine(T)))
        """, true)
        assert(out == "#[1,2,3]\n") { out }
    }
    @Test
    fun fk_06x_iter() {
        val out = test("""
            func fff (col, tp) {
                val ret = #[]
                loop v in to-iter(col,tp) {
                    set ret[+] = v
                }
                drop(ret)
            }
            print(fff([1]))
        """, true)
        //tst_99.Exec_99#fg_07_ifs
        assert(out == "#[1]\n") { out }
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
            print(resume co())
            print(resume co())
            print(resume co())
            print(resume co())
        """, true)
        //assert(out == "anon : (lin 11, col 21) : resume throw : expected yielded coro\n1\n2\n3\n:throw\n") { out }
        assert(out == "1\n" +
                "2\n" +
                "3\n" +
                " |  anon : (lin 11, col 21) : (resume (co)())\n" +
                " v  throw : expected yielded coro\n") { out }
    }

    // AS / YIELD / CATCH / DETRACK / THUS

    @Test
    fun gg_01_yield() {
        val out = test("""
            val CO = coro () {
                yield() thus {
                    print(it)
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
                print(v)
            }
            val t1 = spawn T(1)
            val t2 = spawn T(2)
            emit(true)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun gg_03_yield() {
        val out = test("""
            val CO = coro () {
                val x = yield()
                print(x)
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
                ;;;do;;; nil
                yield() thus { \it => print(it);it }
                ;;;do;;; nil
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
        //        " v  anon : (lin 5, col 17) : block escape throw : cannot move pending reference in\n") { out }
        assert(out == "[]\n[]\n")
    }
    @Test
    fun gg_05_yield() {
        val out = test("""
            val CO = coro () {
                ;;;do;;; nil
                yield() thus {}
                nil
            }
            val co1 = coroutine(CO)
            val co2 = coroutine(CO)
            resume co1()
            resume co2()
            resume co1([])
            resume co2([])
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun gg_06_detrack() {
        val out = test("""
            val T = task () {
                set pub = [10]
                await(true)
            }
            var t = spawn T ()
            ;;var x = track(t)
            ;;;detrack(x);;; do { print(:1) }
            emit( nil )
            ;;detrack(x) { print(999) }
            print(status(t))
            print(:2)
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
                if status(co) == :terminated {
                    break()
                }
                print(v)
            }
            print()
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
                ;;print(:x8, x8, x8+1)
                val x10 = yield(x8+1)
                nil
            }
            val co = coroutine(bar)
            val x2 = resume co(1)
            ;;print(:x2, x2)
            val x5 = resume co(x2+1)
            ;;print(:x5, x5)
            val x7 = resume co(x5+1)
            ;;print(:x7, x7)
            val x9 = resume co(x7+1)
            ;;print(:x9, x9)
            val xN = resume co(x9+1)
            ;;print(:xN, xN)
            print(x2, x5, x7, x9, xN)
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
                if status(co) == :terminated {
                    break ()
                }
                print(v)
            }
            print()
        """)
        assert(out == "xaby\n") { out }
    }

    // SPAWN

    @Test
    fun ii_01_spawn_task() {
        val out = test("""
            spawn {
                print(1)
                yield()
                print(3)
            }
            print(2)
            emit(true)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun TODO_ii_02_spawn_coro() {
        val out = test("""
            val co = coroutine (coro () {   ;; spawn coro
                print(1)
                yield()
                print(3)
            })
            resume co()
            print(2)
            resume co()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun ii_03_spawn_coro() {
        val out = test("""
            val co = coroutine(coro () {   ;; spawn coro
                print(1)
                yield()
                print(3)
            })
            resume co()
            print(2)
            resume co()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun ii_04_spawn() {
        val out = test("""
            spawn {
                spawn {
                    print(1)
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
                print(1)
                val v = yield()
                print(v)
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
                        print(1)
                    }
                    spawn {
                        defer { print(3) }
                        ${AWAIT()}
                        print(2)
                    }
                    ${AWAIT("it==t1")}
                    nil
                }
                print(:ok)
            }
            emit( nil)
        """)
        assert(out == "1\n3\n:ok\n") { out }
    }
    @Test
    fun ii_07_spawn() {
        val out = test("""
            spawn {
                spawn {
                    yield ()
                    print(1)
                }
                yield ()
                print(2)
            }
            emit (nil)
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
                            val evt = await(true);
                            if type(evt)/=:exe-task {
                                set ok1=false
                            } else { nil }
                        } 
                    }
                    ;;yield()
                    do { var ok2; set ok2=true; loop { until not ok2 ; val evt=await(true); if type(evt)/=:exe-task { set ok2=false } else { nil } } }
                    ;;yield()
                    print(1)
                } with {
                    do { var ok3; set ok3=true; loop { until not ok3 ; val evt=await(true); if type(evt)/=:exe-task { set ok3=false } else { nil } } }
                    ;;yield()
                    print(2)
                } with {
                    print(3)
                }
            } ()
            emit( nil )
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
        assert(out == "anon : (lin 3, col 27) : access throw : variable \"ts\" is not declared\n") { out }
    }

    // SPAWN / NESTED

    @Test
    fun TODO_ij_01_nested() {
        val out = test("""
            task :nested () {
                nil
            }
            print(:ok)
        """)
        //assert(out == "anon : (lin 2, col 13) : task :nested throw : expected enclosing spawn\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun TODO_ij_02_nested() {
        val out = test("""
            val t = spawn (task :nested () {
                nil
            })()
            print(type(t))
        """)
        //assert(out == "anon : (lin 2, col 21) : spawn task :nested throw : expected immediate enclosing block\n") { out }
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
        assert(out == "anon : (lin 3, col 17) : spawn task :nested throw : cannot escape enclosing block\n") { out }
    }
    @Test
    fun ij_04_nested() {
        val out = test("""
            ;;do {
                spawn (task :nested () {
                    print(:ok)
                })()
                nil
            ;;}
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ij_05_task_pub_fake() {
        val out = test("""
            task T () {
                set ;;;task.;;;pub = 10
                print(;;;task.;;;pub)
                spawn {
                    print(;;;task.;;;pub)
                    await (|false)
                }
                nil
            }
            spawn T()
            emit (nil) in :global
        """)
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun ij_06_task_pub_fake() {
        val out = test("""
            data :T = [x]
            spawn {
                task T () :T {
                    set pub = [10]
                    spawn {
                        print(pub.x)
                    }
                }
                spawn T()
            }
        """)
        assert(out == "10\n") { out }
    }

    // PAR / PAR-AND / PAR-OR

    @Test
    fun jj_01_par_err() {
        val out = test("""
            par {
                print(1)
            } with {
                print(2)
            }
        """)
        //assert(out == "anon : (lin 5, col 29) : :nested throw : expected enclosing prototype\n") { out }
        assert(out == "anon : (lin 2, col 13) : yield throw : expected enclosing coro or task\n") { out }
    }
    @Test
    fun jj_02_par() {
        val out = test("""
            spawn {
                par {
                    print(1)
                } with {
                    print(2)
                }
                print(999)
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
                    print(1)
                } with {
                    print(2)
                } with {
                    yield()
                    yield()
                    print(3)
                }
                print(:ok)
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
                print(v)
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_05_parand() {
        val out = test("""
            spawn {
                par-and {
                    print(1)
                } with {
                    print(2)
                }
                print(:ok)
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
                print(v)
            }
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun jj_07_paror() {
        val out = test("""
            spawn {
                par-or {
                    yield()
                    yield()
                    yield()
                    print(1)
                } with {
                    yield()
                    print(2)
                } with {
                    yield()
                    yield()
                    print(3)
                }
                print(:ok)
            }
            emit(true)
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
                    print(1)
                } with {
                    yield()
                    print(2)
                } with {
                    yield()
                    yield()
                    print(3)
                }
                print(:ok)
            }
            emit(true)
            emit(true)
            emit(true)
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
                    print(1)
                } with {
                    yield()
                    print(2)
                } with {
                    yield()
                    yield()
                    print(3)
                }
                print(:ok)
            }
            emit(true)
            emit(true)
            emit(true)
        """)
        assert(out == "2\n3\n1\n:ok\n") { out }
    }
    @Test
    fun jj_09_paror_defer() {
        val out = test("""
            spawn {
                par-or {
                    ${AWAIT()}
                    print(1)
                } with {
                    defer { print(3) }
                    ${AWAIT()}
                    print(2)
                }
                print(:ok)
            }
            emit(true)
        """)
        assert(out == "1\n3\n:ok\n") { out }
    }
    @Test
    fun jj_10_paror_defer() {
        val out = test("""
            spawn {
                par-or {
                    defer { print(3) }
                    ${AWAIT()}
                    print(999)
                } with {
                    print(2)
                }
                print(:ok)
            }
            emit (nil)
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
                    print(999)
                } with {
                    nil
                }
                nil
            }
            emit (nil)
        """)
        assert(out == "999\n") { out }
    }
    @Test
    fun jj_11_paror_defer() {
        val out = test("""
            spawn {
                par-or {
                    defer { print(1) }
                    ${AWAIT()}
                    ${AWAIT()}
                    print(999)
                } with {
                    ${AWAIT()}
                    print(2)
                } with {
                    defer { print(3) }
                    ${AWAIT()}
                    ${AWAIT()}
                    print(999)
                }
                print(999)
            }
            emit (nil)
        """)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun jj_11_parand_defer() {
        val out = test("""
            spawn {
                par-and {
                    yield()
                    print(1)
                } with {
                    print(2)
                } with {
                    yield()
                    print(3)
                }
                print(:ok)
            }
            emit (nil)
        """)
        assert(out == "2\n1\n3\n:ok\n") { out }
    }
    @Test
    fun jj_12_parand_defer() {
        val out = test("""
            spawn {
                par-and {
                    defer { print(1) }
                    ${AWAIT()}
                    ${AWAIT()}
                    print(1)
                } with {
                    ${AWAIT()}
                    print(2)
                } with {
                    defer { print(3) }
                    ${AWAIT()}
                    ${AWAIT()}
                    print(3)
                }
                print(:ok)
            }
            emit (nil)
            emit (nil)
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
                emit([])
            }
            print(:ok)
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
            print(:ok)
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
                emit([])
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun BUG_jj_16_par_bcast() {     // bcast in outer of :nested
        val out = test("""
            spawn {
                par-or {
                    val e = yield()
                    print(e)
                } with {
                    emit(:1)
                }
            }
            print(:2)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun jj_17_par_tasks() {
        val out = test("""
            spawn task () {
                ^[9,29]await(true)                                          
            }()                                                       
            spawn task () {                                           
                ^[9,29]await(true)                       
            }()
            print(1)
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
                print(x)
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
                print(x)
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
                print(x)
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
            emit (:x) in :global
            print(1)
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
            emit (true) in :global
            print(1)
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
                emit (tag(:frame, [40])) in :global 
                emit (tag(:draw, [])) in :global
            }
            print(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_24_parand_immediate() {
        val out = test("""
            spawn task () {
                par-and {
                    print(1)
                } with {
                    print(2)
                }
                print(999)
            } ()
        """, true)
        assert(out == "1\n2\n999\n") { out }
    }
    @Test
    fun jj_25_paror_valgrind() {
        val out = test("""
            spawn {
                par-or {
                    loop { await(true) }
                } with {
                    par-or {
                        yield()
                    } with {
                        loop { await(true) }
                    }
                }
            }
            emit (nil) in :global
            print(1)
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
                    print(:0)
                    await (|it==x)
                    print(:2)
                } with {
                    print(:1)
                    emit (nil) in t
                }
                print(:3)
            }
            print(:4)
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
                    print(:0)
                    await (|it==x)
                    print(:2)
                } with {
                    print(:1)
                    emit (nil) in t
                }
                print(:3)
            }
            print(:4)
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
                print(1)
            }
            spawn T()
            emit (tag(:x,[]))
            print(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun kk_02_await() {
        val out = test("""
            $IS
            spawn {
                print(0)
                await ( |(it/=nil) and (it[:type]==:x) )
                print(99)
            }
            do {
                print(1)
                emit (@[(:type,:y)])
                print(2)
                emit (@[(:type,:x)])
                print(3)
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
                print(0)
                await(:x)
                print(99)
            }
            do {
                print(1)
                emit (tag(:y, []))
                print(2)
                emit (tag(:x, []))
                print(3)
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
                print(0)
                await(:x)
                print(99)
            }
            do {
                print(1)
                emit (tag(:y, []))
                print(2)
                emit (tag(:x, []))
                print(3)
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
        assert(out == "anon : (lin 3, col 13) : yield throw : expected enclosing coro or task\n") { out }
    }
    @Test
    fun kk_06_await() {
        val out = test("""
            spawn {
                loop {
                    await (|true) {
                        print(it)
                    }
                }
            }
            emit (@[])
        """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun kk_06x_await() {
        val out = test("""
            spawn {
                loop {
                    await {
                        print(it)
                    }
                }
            }
            emit (@[])
        """)
        //assert(out == "anon : (lin 4, col 27) : expected expression : have \"{\"\n") { out }
        assert(out == "@[]\n") { out }
    }
    @Test
    fun kk_07_await() {
        val out = test("""
            spawn {
                await (|true) {
                    print(it)
                }
                await (|true) {
                    print(it)
                }
            }
            emit (:1)
            emit (:2)
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun kk_08_await() {
        val out = test("""
            $COMP
            spawn {
                await (2)
                print(2)
                await (==1)
                print(1)
            }
            emit (1)
            emit (2)
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
                        print(it)
                    }
                }
            }
            emit(:X [])
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun kk_10_await_escape() {
        val out = test("""
            $IS
            spawn {
                print(await())
            }
            do {
                val e = []
                emit(drop(e))
            }
        """)
        assert(out == "[]\n") { out }
        //assert(out == "true\n") { out }
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
                emit(drop(e))
            }
            print(:ok)
        """)
        //assert(out == " |  anon : (lin 10, col 17) : emit'(e,:task)\n" +
        //        " v  anon : (lin 4, col 27) : argument throw : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 5, col 21) : yield throw : unexpected enclosing func\n") { out }
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
                print(:1)
            }
            emit(true)
            print(:2)
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun kk_13_await_drag() {
        val out = test("""
            $IS
            spawn {
                val click = await(:X) {
                    print(:it, it)
                    it
                }
                print(:click, click)
            }            
            emit(:X [1,2])
            print(nil)
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
                    print(it.v)
                }
            }
            emit(:T [:ok])
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
                    print(it.x)
                }
            }
            emit (:E [10,20]) in :global
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ka_02_await_data() {
        val out = test("""
            data :E = [x,y]
            spawn {
                await :E| it.y==20 {
                    print(it.x)
                }
            }
            emit (:E [10,10]) in :global 
            print(:mid)
            emit (:E [10,20]) in :global
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
                    print(it.x)
                }
                await :F| it.i==10 {
                    print(it.j)
                }
            }
            emit(:E [10,20]) in :global 
            emit(:F [10,20]) in :global
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
                print(v)
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
                    print(v)
                }
                emit(2)
            }
        """)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun kl_03_await_task() {
        val out = test("""
            spawn {
                val t = spawn {
                    print(:1)
                }
                await(|it==t)
                print(:2)
            }
            print(:3)
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
                print(:1)
                every (|true) {
                    until true
                    throw(999)
                }
                print(:2)
            }
            spawn T()
            emit (nil)
        """)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun km_02_every() {
        val out = test(
            """
            $IS
            task T () {
                print(:1)
                every (|true) {
                    until false
                    print(:xxx)
                }
                print(:2)
            }
            spawn T()
            emit (nil)
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
            print(:ok)
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
                emit(drop(e))
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 5, col 21) : yield throw : unexpected enclosing func\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : emit'(e,:task)\n" +
        //        " v  anon : (lin 4, col 28) : argument throw : cannot copy reference out\n") { out }
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
                        ;;;do;;; rect
                    }
                }
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun km_05x_every() {
        val out = test("""
            spawn (task () {
                var rect = []
                spawn (task :nested () {
                    print(rect)
                }) ()
            }) ()
            print(:ok)
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
                    print(it.v)
                }
            }
            emit(:T [:ok])
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun km_07_every() {
        val out = test("""
            spawn {
                print(0)
                every :x {
                    print(it[0])
                }
            }
            do {
                print(1)
                emit (tag(:x, [10])) in :global 
                print(2)
                emit (tag(:y, [20])) in :global
                print(3)
                emit (tag(:x, [30])) in :global
                print(4)
            }
        """, true)
        assert(out == "0\n1\n10\n2\n3\n30\n4\n") { out }
    }
    @Test
    fun km_08_every_clk() {
        val out = test("""
            spawn task () {
                every <10:s> {
                    print(10)
                }
            }()
            print(0)
            emit (tag(:Clock, [5000])) in :global 
            print(1)
            emit (tag(:Clock, [5000]))
            print(2)
            emit (tag(:Clock, [10000])) in :global 
            print(3)
        """, true)
        assert(out == "0\n1\n10\n2\n10\n3\n") { out }
    }
    @Test
    fun TODO_km_09_every_clk_multi() { // awake twice from single bcast
        val out = test("""
            spawn task () {
                every <10:s> {
                    print(10)
                }
            }()
            print(0)
            emit in :global, tag(:Clock, [20000])
            print(1)
        """, true)
        assert(out == "0\n10\n10\n1") { out }
    }
    @Test
    fun km_10_await_clk() {
        val out = test("""
            spawn task () {
                loop {
                    await <10:s>
                    print(999)
                }
            }()
            print(0)
            emit (tag(:Clock, [5000])) in :global
            print(1)
            emit (tag(:Clock, [5000])) in :global 
            print(2)
        """, true)
        assert(out == "0\n1\n999\n2\n") { out }
    }
    @Test
    fun km_11_every() {
        val out = test(
            """
            $IS
            task T () {
                val v = every (|true) {
                    until :ok
                }
                print(v)
            }
            spawn T()
            emit (nil)
        """)
        assert(out == ":ok\n") { out }
    }

    // CLOCK

    @Test
    fun km_01_clock() {
        val out = test("""
            $IS ; $PLUS ; $MULT ; $COMP
            data :Clock = [ms]
            spawn {
                await <2:ms>
                print(:ok)
            }
            print(:0)
            emit(:Clock [1])
            print(:1)
            emit(:Clock [1])
            print(:2)
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
                    print(:x, x)
                    set x = x - 1
                }
                print(:ok)
            }
            print(:0)
            emit(:Clock [5])
            emit(:Clock [5])
            print(:1)
            emit(:Clock [5])
            emit(:Clock [5])
            print(:2)
            emit(:Clock [5])
            emit(:Clock [5])
            print(:3)
        """)
        assert(out == ":0\n:x\t10\n:1\n:x\t9\n:2\n:x\t8\n:3\n") { out }
    }

    // AWAIT / ORIGINAL

    @Test
    fun kn_01_await_task() {
        val out = test("""
            spawn {
                await spawn { 1 }
                print(1)
            }
            print(2)
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun kn_02_await_task() {
        val out = test("""
            spawn {
                spawn {
                    yield ()
                    print(1)
                    emit(true) in :global
                    print(3)
                }
                yield ()
                print(2)
            }
            emit(true) in :global
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
                print(y)
            }
            emit (nil) in :global
        """, true)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun kn_04_await_task_err() {
        val out = test("""
            spawn {
                val ts = tasks()
                var x = await spawn nil() in ts
            }
        """)
        //assert(out == "anon : (lin 2, col 27) : expected non-pool spawn : have \"spawn\"") { out }
        assert(out == " |  anon : (lin 5, col 14) : (spawn (func :fake () { group { (val ts =...\n" +
                " |  anon : (lin 4, col 31) : (spawn nil() in ts)\n" +
                " v  throw : expected task\n") { out }
    }
    @Test
    fun kn_05_await_task_rets() {
        val out = test("""
            spawn {
                var x = await spawn {
                    var y = []
                    y
                }
                print(x)
            }
        """, true)
        //assert(out.contains("[]\n")) { out }
        //assert(out.contains("anon : (lin 3, col 53) : block escape throw : incompatible scopes")) { out }
        assert(out == " |  anon : (lin 8, col 14) : (spawn (func :fake () { group { (var x = ...\n" +
                " |  anon : (lin 3, col 17) : (var x = do { (val ceu_spw = (spawn (func...\n" +
                " v  throw : cannot copy reference out\n") { out }
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
                print(x,y,z)
            }
            emit(true) in :global
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
                print(999)
            }
            print(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun TODO_kn_08_await_now() {    // :check-now removed
        val out = test("""
            spawn {
                print(1)
                await( ;;;:check-now;;;| true)
                print(2)
            }
            print(3)
        """, true)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun kn_09_await_notfalse() {
        val out = test("""
            spawn {
                print(1)
                await (|10)
                print(2)
            }
            emit(true) in :global
        """, true)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun TODO_kn_10_task_pub_fake_err() {
        val out = test("""
            spawn {
                watching evt|evt==:a {
                    every evt|evt==:b {
                        print(;;;task.;;;pub)    ;; no enclosing task
                    }
                }
            }
            print(1)
        """)
        //assert(out == "anon : (lin 5, col 33) : task throw : missing enclosing task") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun kn_11_task_pub_fake() {
        val out = test("""
            spawn (task () {
                set ;;;task.;;;pub = 1
                watching evt| evt==:a {
                    every evt| evt==:b {
                        print(;;;task.;;;pub)
                    }
                }
            }) ()
             emit (:b) in :global
             emit (:b)
             emit (:a)
             emit (:b) in :global
        """, true)
        assert(out == "1\n1\n") { out }
    }
    @Test
    fun kn_12_task_tup_status() {
        val out = test("""
            task T () {}
            val ts = [spawn T()]
            print(status(ts[0]))
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
                    defer { print(:z) }
                    print(:x)
                    ${AWAIT()}
                    print(:y)
                    ${AWAIT()}
                    print(999)
                }
                print(:A)
            }
            print(1)
            emit (nil)
            print(2)
            emit (1)
            print(3)
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
        assert(out == "anon : (lin 3, col 31) : access throw : variable \"{{*}}\" is not declared\n") { out }
    }
    @Test
    fun ll_03_watching_clk() {
        val out = test("""
            spawn {
                watching <10:s> {
                    defer { print(10) }
                    await (|false)
                    print(1)
                }
                print(999)
            }
            print(0)
            emit (tag(:Clock,[5000])) in :global 
            print(1)
            emit (tag(:Clock, [5000]) )
            print(2)
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
            print(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun ll_05_watching() {
        val out = test("""
            task T () {
                watching (|throw(:throw)) {
                    await (|false)
                }
            }            
            spawn T() in tasks()
            emit (nil)
        """, true)
        assert(out == " |  anon : (lin 8, col 13) : emit'(:task,nil)\n" +
                " |  anon : (lin 3, col 28) : throw(:throw)\n" +
                " v  throw : :throw\n") { out }
    }
    @Test
    fun BUG_ll_06_watching_track() {
        val out = test("""
            task T () {
                set ;;;task.;;;pub = [10]
                await (:evt)
                print(:end)
            }
            val t = spawn(T)()
            val x = ;;;track;;;(t)
            spawn {
                watching ;;;:check-now;;; |it==x {
                    print(x.pub[0])
                    emit(true) in :global
                    print(x.pub[0])
                    emit(:evt) in :global          ;; BUG: same tick as watching?
                    print(:nooo)   ;; never printed
                    await (|false)
                }
                print(status(x))
            }
            print(:ok)
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
                    emit (:evt) in :global
                    print(:nooo)
                }
                print(status(x))
            }
            print(:ok)
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
                print(:ok)
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
                    print(pub)
                    every (it| (it is? :dict) and (it.sub==:draw)) {
                        print(it.v)
                    }
                }
            }
            spawn T(0)
            emit(@[(:sub,:draw),(:v,1)])
            emit(:Show [false])
            emit(:Show [false])
            emit(@[(:sub,:draw),(:v,99)])
            emit(:Show [true])
            emit(:Show [true])
            emit(@[(:sub,:draw),(:v,2)])
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun mm_02_toggle() {
        val out = test("""
            task T (v) {
                set pub = v
                toggle :Show {
                    print(pub)
                    every :draw {
                        print(it[0])
                    }
                }
            }
            spawn T (0)
            emit (tag(:draw, [1]))
            emit (tag(:Show, [false]))
            emit (tag(:Show, [false]))
            emit (tag(:draw, [99]))
            emit (tag(:Show, [true]))
            emit (tag(:Show, [true]))
            emit (tag(:draw, [2]))
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun mm_03_toggle() {
        val out = test("""
            $IS
            spawn {
                val x = toggle :Show {
                    10
                }
                print(x)
            }
            print(:ok)
        """)
        assert(out == "10\n:ok\n") { out }
    }

    // METHODS

    @Test
    fun oo_07_method() {
        val out = test("""
            func f (v) { v() }
            val v = f <- {10} thus { it }
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oo_08_method() {
        val out = test("""
            func f (x,y) { y(x) }
            val v = 10 -> f <- {it} thus { it }
            print(v)
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
            print(v)
        """)
        assert(out == "30\n") { out }
    }
    @Test
    fun op_02_pipe() {
        val out = test("""
            func g (v) { v }
            func f (v) { g }
            val v = 10-->f->g
            print(v)
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
            print(v)
        """)
        assert(out == "-11\n") { out }
    }
    @Test
    fun op_04_thus() {
        val out = test(
            """
            val x = 1 --> {
                it
            }
            print(x)
        """,true)
        assert(out == "1\n") { out }
    }
    @Test
    fun TODO_op_05_thus_err() {
        val out = test(
            """
            val x = [] --> { \x =>
                x   ;; TODO: x redeclared
            }
            print(x)
        """,true)
        //assert(out == "anon : (lin 2, col 31) : declaration throw : variable \"x\" is already declared\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun op_05_thus() {
        val out = test(
            """
            val y = [] --> {\ x =>
                x
            }
            print(y)
        """,true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun op_06_thus() {
        val out = test(
            """
            val x = {
                it
            } <-- 1
            print(x)
        """,true)
        assert(out == "1\n") { out }
    }
    @Test
    fun op_07_thus() {
        val out = test(
            """
            val x = {\y =>
                y
            } <-- []
            print(x)
        """,true)
        assert(out == "[]\n") { out }
    }
    @Test
    fun op_08_thus() {
        val out = test(
            """
            val x = 2 --> { it + 1 } --> { it * 2 }
            print(x)
        """,true)
        assert(out == "6\n") { out }
    }
    @Test
    fun op_09_thus() {
        val out = test(
            """
            val x = { it + 1 } <-- { it * 2 } <-- 2
            print(x)
        """,true)
        assert(out == "5\n") { out }
    }

    // CAST

    @Test
    fun oq_01_cast() {
        val out = test("""
            data :X = [x]
            val t = [[10]]
            print(t[0].(:X).x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oq_02_cast() {
        val out = test("""
            data :X = [x]
            val t = [[[10]]]
            print(t[0].(:X).x)
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
                await(true)
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
            print(x) where {
                val x = 1
            }
            val z = (y + 10) where {
                val y = 20
            }
            print(z)
        """)
        assert(out == "1\n30\n") { out }
    }
    @Test
    fun oq_02_where() {
        val out = test(
            """
            task T (v) {
                print(v)
            }
            val t = (spawn T(v where { val v = 10 }))
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 5, col 34) : set throw : incompatible scopes\n") { out }
    }
    @Test
    fun oq_03_where() {
        val out = test(
            """
            coro T (v) {
                print(v)
            }
            (val t = spawn T(v)) where { val v = 10 }
            print(t)
        """)
        assert(out == "anon : (lin 6, col 21) : access throw : variable \"t\" is not declared\n") { out }
    }
    @Test
    fun op_04_where() {
        val out = test(
            """
            $PLUS
            val z = y + 10 where {
                val y = 20
            }
            print(z)
        """)
        //assert(out == "anon : (lin 2, col 21) : access throw : variable \"y\" is not declared") { out }
        assert(out == "30\n") { out }
    }
    @Test
    fun op_05_where() {
        val out = test("""
            val x = y
                where {
                    val y = 10
                }
            print(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oq_06_where() {
        val out = test(
            """
            task T (v) {
                print(v)
            }
            val t = (spawn T(v where { val v = 10 }))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun op_07_where() {
        val out = test(
            """
            task T (v) {
                print(v)
            }
            val t = (spawn T(v)) where {
                val v = 10
            }
            print(type(t))
        """)
        assert(out == "10\n" +
                " |  anon : (lin 5, col 13) : (val t = do { (val v = 10); (spawn T(v)); })\n" +
                " v  throw : cannot copy reference out\n") { out }
    }
    @Test
    fun op_07x_where() {
        val out = test(
            """
            task T (v) {
                print(v)
            }
            val t = spawn T(v where {
                val v = 10
            })
            print(type(t))
        """)
        assert(out == "10\n:exe-task\n") { out }
    }
    @Test
    fun todo_iter_op_08_where() {
        val out = test(
            """
            task T (v) {
                print(v)
                yield()
            }
            val ts = tasks()
            (spawn T(v) in ts) where {
                val v = 10
            }
            loop t in ts {
                print(type(t))
            }
        """, true)
        assert(out == "10\n:exe-task\n") { out }
    }

    // LAMBDA

    @Test
    fun pp_01_lambda () {
        val out = test("""
            print({ it })
        """)
        assert(out.contains("func: 0x")) { out }
    }
    @Test
    fun pp_02_lambda () {
        val out = test("""
            $PLUS
            print({\x=>x+x}(2))
        """)
        assert(out.contains("4\n")) { out }
    }
    @Test
    fun pp_03_lambda () {
        val out = test("""
            print({\x=>x}(1))
        """)
        assert(out.contains("1\n")) { out }
    }
    @Test
    fun pp_04_lambda () {
        val out = test(
            """
            print({ it }(10))
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
            print(f <- { it })
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_06_it_it () {
        val out = test(
            """
            val x = { { it }(10) }()    ;; it1/it2
            print(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_07_lambda_call () {
        val out = test("""
            func f (v,g) {
                g(v)
            }
            val v = f(5) <- { it }
            print(v)
        """)
        assert(out == "5\n") { out }
    }
    @Test
    fun pp_08_lambda_call () {
        val out = test("""
            func f (g) {
                g()
            }
            val v = f( { 10 } )
            print(v)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_09_lambda_call () {
        val out = test("""
            print({\ x => x }(10))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_10_lambda_call () {
        val out = test("""
            $PLUS
            print({\x,y => x+y }(10,30))
        """)
        assert(out == "40\n") { out }
    }

    // TEST

    @Test
    fun qq_01_test () {
        val out = test("""
            do {
                print(:1)
            }
            test {
                print(:2)
            }
            do {
                test {
                    print(:3)
                }
                print(:4)
            }
        """)
        assert(out == ":1\n:4\n") { out }
    }
    @Test
    fun qq_02_test () {
        TEST = true
        val out = test("""
            do {
                print(:1)
            }
            test {
                print(:2)
            }
            do {
                test {
                    print(:3)
                }
                print(:4)
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
            print(x, x[1], x[1][0])
        """
        )
        assert(out == "[nil,[10]]\t[10]\t10\n") { out }
    }
    @Test
    fun TODO_tt_02_index_tuple() {
        val out = test("""
            val t = [1,2,3]
            print(t.a, t.c)
        """)
        assert(out == "1\t3\n") { out }
    }
    @Test
    fun tt_03_index_dict() {
        val out = test("""
            val t = @[ (:x,1), (:y,2) ]
            print(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun tt_04_string() {
        val out = test("""
            var v = "abc"
            set v[#v] = 'a'
            set v[2] = 'b'
            print(v[0])
            `puts(${D}v.Dyn->Vector.buf);`
        """)
        assert(out == "a\nabba\n") { out }
    }
    @Test
    fun tt_05_string() {
        val out = test("""
            print("")
            print("a\tb")
            print("a\nb")
            print("a'\"b")
        """)
        assert(out == "#[]\na\tb\na\nb\na'\"b\n") { out }
    }
    @Test
    fun tt_06_dict_init_err() {
        val out = test("""
            var t = @[x,y]
            print(t.x, t.y)
        """)
        assert(out == "anon : (lin 2, col 24) : expected \"=\" : have \",\"\n") { out }
    }
    @Test
    fun tt_07_dict_init() {
        val out = test("""
            var t = @[x=1, y=2]
            print(t.x, t.y)
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
            print(v)
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
            print(v1 ++ v2)
        """, true)
        assert(out == "#[1,2,3,4,5,6]\n") { out }
    }

    // TAG CONSTRUCTOR / DECLARATION

    @Test
    fun uu_01_cons() {
        val out = test("print(:T [])")
        assert(out == ":T []\n") { out }
    }
    @Test
    fun uu_02_cons() {
        val out = test("""
            data :T = [v]
            val t = :T [10]
            print(t.v, t)
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
            print(v)
        """)
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun vv_02_ppp_peek() {
        val out = test("""
            $PLUS
            val v = #[10]
            print(v[=])
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun vv_03_ppp_push() {
        val out = test("""
            $PLUS
            val v = #[]
            set v[+] = 1
            print(v)
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
        assert(out == "anon : (lin 4, col 41) : index throw : out of bounds\n" +
                ":throw\n") { out }
    }
    @Test
    fun vv_05_ppp_pop() {
        val out = test("""
            $PLUS
            var v = #[1]
            var x = v[-]
            print(#v, x)
        """)
        assert(out == "0\t1\n") { out }
    }
    @Test
    fun vv_06_ppp_pop_err() {
        val out = test("""
            $PLUS
            val v = #[1]
            set v[-] = 10   ;; cannot set v[-]
            print(v)
        """)
        assert(out == ("anon : (lin 4, col 13) : set throw : expected assignable destination\n")) { out }
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
            print(#v, v[=])
            val x = v[-]
            print(#v, v[=], x)
        """)
        assert(out == "3\t3\n2\t20\t3\n") { out }
    }
    @Test
    fun vv_08_ppp_debug() {
        val out = test("""
            $PLUS
            var v
            set v = #[10]
            print(v[#v - 1])
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun vv_09_ppp_debug() {
        val out = test("""
            $PLUS
            var v
            set v = #[10]
            print(v[-1+1])
        """)
        assert(out == "anon : (lin 5, col 24) : expected \"]\" : have \"1\"\n") { out }
    }
    @Test
    fun vv_10_ppp() {
        val out = test("""
            $PLUS
            val stk = [1]
            stk[-]
            print(stk, #stk)
        """)
        assert(out == "[nil]\t1\n") { out }
    }
    @Test
    fun vv_11_vector_size() {
        val out = test("""
            val v = #[]
            print(#v, v)
            set v[+] = 1
            set v[+] = 2
            print(#v, v)
            val top = v[-]
            print(#v, v, v[=], top)
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
            print(to-tag-string(":A"), to-tag-string(":A.B"), to-tag-string(":A.B.C"))
        """, true)
        assert(out == ":A\t:A.B\t:A.B.C\n") { out }
    }
    @Test
    fun xx_01_data_nested () {
        val out = test("""
            data :T = [t=[a,b]]
            val t :T = [[1,10]]
            print(t.t, t.t.b)
        """)
        assert(out == "[1,10]\t10\n") { out }
    }
    @Test
    fun xx_02_data_nested () {
        val out = test("""
            data :Lim = [p1=[l,c], p2=[l,c]]
            val v :Lim = [[1,1],[2,2]]
            print(v, v.p2.l)
        """)
        assert(out == "[[1,1],[2,2]]\t2\n") { out }
    }

    // ==, ===, /=, =/=

    @Test
    fun xa_00_eqeqeq_tup() {
        val out = test(
            """
            print(do {
                val' it = [1]
                do {
                    var v = it
                }
                it
            })
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun xa_01_eqeqeq_tup() {
        val out = test(
            """
            print([1] === [1])
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun xa_02_op_eqeqeq_tup() {
        val out = test(
            """
            print([1] === [1])
            print([ ] === [1])
            print([1] =/= [1])
            print([1,[],[1,2,3]] === [1,[],[1,2,3]])
            print([nil,[[1,1],1]] === [nil,[[1,1],1]])
        """, true)
        assert(out == "true\nfalse\nfalse\ntrue\ntrue\n") { out }
    }
    @Test
    fun xa_03_op_eqeqeq_tup() {
        val out = test(
            """
            print([1,[1],1] === [1,[1],1])
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun xb_04_op_eqs_dic() {
        val out = test(
            """
            print(@[] ==  @[])
            print(@[] === @[])
            print(@[] /=  @[])
            print(@[] =/= @[])
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun xb_05_op_eqs_vec() {
        val out = test(
            """
            print(#[]  ==  #[])
            print(#[1] === #[1])
            print(#[1] /=  #[1])
            print(#[]  =/= #[])
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun xb_06_op_eqs_vec_dic_tup() {
        val out = test(
            """
            print(@[(:y,false)] === @[(:x,true)])
        """, true)
        assert(out == "false\n") { out }
    }
    @Test
    fun xb_07_op_eqs_vec_dic_tup() {
        val out = test(
            """
            print([#[],@[]] ==  [#[],@[]])
            print([#[],@[]] /=  [#[],@[]])
            print([#[1],@[(:y,false),(:x,true)]] === [#[1],@[(:x,true),(:y,false)]])
            print([#[],@[]] =/= [#[],@[]])
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
                            print(x)
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
                " v  throw : expected collection\n") { out }
    }
    @Test
    fun xb_09_xxx() {
        val out = test(
            """
            print([@[]] === [@[]])
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun xb_10_eqs() {
        val out = test("""
            func g (v1', v2') {
                ;;print(:XXX)
                ;;dump(v1') ; dump(v1'[0])
                ;;dump(v2') ; dump(v2'[0])
                not (
                    loop [i,v] in to-iter(v1',[:idx,:val]) {
                        while eq(v2'[i], v)
                    }
                )
            }
            val f = func (v1,v2) {
                g(v1,v2) and g(v2,v1)
            }
            val d1 = [[10]]
            print(f(d1, [[10]]))
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun xb_11_eqs() {
        val out = test("""
            func g (v) {
                ;;dump(x)
                val x = v[0]
                ;;dump(x)
                true
            }
            val f = func (v) {
                g(v) and g(v)
            }
            print(f([[10]]))
        """)
        assert(out == "true\n") { out }
    }

    // TO-*

    @Test
    fun xc_01_tostring() {
        val out = test("""
            val s = to.string(10)
            print(type(s), s)
        """, true)
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun xc_02_tonumber() {
        val out = test("""
            val n = to.number("10")
            print(type(n), n)
        """, true)
        assert(out == ":number\t10\n") { out }
    }
    @Test
    fun xc_03_tonumber_tostring() {
        val out = test("""
            val s = to.string(to.number("10"))
            print(type(s), s)
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
            print(v)
        """, true)
        assert(out == "#[[1]]\n") { out }
    }
    @Test
    fun xc_05_tovector() {
        val out = test("""
            val v = do {
                val t = [[1],[2],[3]]
                to.vector(drop(t))
            }
            print(v)
        """, true)
        assert(out == "#[[1],[2],[3]]\n") { out }
    }
    @Test
    fun xc_05x_tovector() {
        val out = test("""
            val v = do {
                val t = [[1],[2],[3]]
                drop(to.vector(t))
            }
            print(v)
        """, true)
        assert(out == " |  anon : (lin 2, col 13) : (val v = do { (val t = [[1],[2],[3]]); dro...\n" +
                " v  throw : cannot copy reference out\n") { out }
    }
    @Test
    fun xc_06_string_to_tag() {
        val out = test("""
            ;;;do;;; :xyz
            print(to-tag-string(":x"))
            print(to-tag-string(":xyz"))
            print(to-tag-string("xyz"))
        """, true)
        assert(out == "nil\n:xyz\nnil\n") { out }
    }
    @Test
    fun xc_07_to_char() {
        val out = test("""
            print(to.char('a'))
            print(to.char(65))
            print(to.char("x"))
            print(to.char(""))
            print(to.char("ab"))
            print(to.char("\\n"))
            print(:ok)
        """, true)
        assert(out == "a\nA\nx\nnil\nnil\n\n\n:ok\n") { out }
    }
    @Test
    fun xc_07x_to_char() {
        val out = test("""
            val v = [1,2]
            print(:v, v, #v, v[0])
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
            print(to.tuple([]))
            print(to.tuple(#[1,2]))
        """, true)
        assert(out == "[]\n[1,2]\n") { out }
    }
    @Test
    fun xc_08_todict() {
        val out = test("""
            print(to.dict([[:x,1],[:y,2]]))
            print(to.dict(#[[:x,1],[:y,2]]))
        """, true)
        assert(out == "@[(:x,1),(:y,2)]\n" +
                "@[(:x,1),(:y,2)]\n") { out }
    }
    @Test
    fun xc_09_todict() {
        val out = test("""
            print(to.dict([:x,:y]))
            print(to.dict([]))
        """, true)
        assert(out == "@[(:x,0),(:y,1)]\n" +
                "@[]\n") { out }
    }

    // PRELUDE

    @Test
    fun za_01_ok() {
        val out = test("""
            print(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun za_02_tasks() {
        val out = test("""
            val ts = tasks()
            loop in {1=>10} {
                ;;dump(ts)
                ;;;do;;; [ts]
            }
            print(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun TODO_za_03_in() {
        val out = test("""
            print(10 in? [1,2,3])
            print(10 in? [1,10,3])
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
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun za_05_assert() {
        val out = test("""
            print(assert(10))
            assert(nil)
        """, true)
        assert(out == "10\n" +
                " |  anon : (lin 3, col 13) : assert(nil)\n" +
                " |  build/prelude-x.ceu : (lin 40, col 30) : throw(:throw,#['a','s','s','e','r','t','i'...\n" +
                " v  throw : assertion throw\n") { out }
    }
    @Test
    fun za_06_copy() {
        val out = test("""
            print(copy([1,2,3]))
            print(copy(#[1,2,3]))
            print(copy(@[(:k1,[1,2,3]), (1,#[])]))
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
                " |  build/prelude-x.ceu : (lin 39, col 30) : throw(:throw,xx-cat-move(xx-cat-move(#[],#...\n" +
                " v  throw : assertion throw : ok\n")) { out }
    }
    @Test
    fun TODO_za_08_comp() {     // fp.*
        val out = test("""
            func square (x) {
                x**2
            }
            val quad = square <|< square
            print(quad(3))
        """, true)
        assert(out == "81\n") { out }
    }

    // ORIGINAL

    @Test
    fun zb_01() {
        val out = test("""
            func g () {
            }
            coro bar () {
                ;;;do;;; [g, coroutine(coro () {})]
                nil
            }
            val it = [g, coroutine(bar)]
            resume it[1]()
            print(:ok)
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
            print(:ok)
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
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_04_all() {
        val out = test("""
            task T (pos) {
                await (|true)
                print(pos)
            }
            spawn {
                val ts = tasks()
                do {
                    spawn T([]) in ts
                }
                await (|false)
            }
            emit(true) in :global
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
            print(t.pub())
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
            print(:ok)
        """)
        assert(out == " |  anon : (lin 8, col 13) : (spawn T())\n" +
                " |  anon : (lin 5, col 37) : pub\n" +
                " v  throw : cannot copy reference out\n") { out }
        //assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 8, col 19) : T()\n" +
        //        "anon : (lin 5, col 30) : set throw : incompatible scopes\n:throw\n") { out }
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
                print(u.pub)
            }
            spawn T (spawn U())
        """, true)
        //assert(out == "10\n") { out }
        assert(out == "nil\n") { out }
        //assert(out == "anon : (lin 10, col 28) : U()\n" +
        //        "anon : (lin 2, col 23) : block escape throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun BUG_zb_08_all_valgrind() {
        val out = test("""
            task U () {
                set pub = func () {
                    10
                }
                await(|false)
            }
            task T (u) {
                print(u.pub())
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
                print(u)
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
            print(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun zb_11_all() {
        val out = test("""
            spawn {
                loop {
                    await (10)
                    emit(tag(:pause, [])) in :global 
                    watching 10 {
                        await(|false)
                    }
                    emit(tag(:resume, [])) in :global 
                }
            }
            emit (10) in :global
            emit (10) in :global
            print(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_12_all() {
        val out = test("""
            spawn {
                loop {
                    await (10)
                    emit (tag(:pause, [])) in :global
                    watching 10 {
                        await(|false)
                    }
                    emit (tag(:resume, [])) in :global
                    await (|true)
                }
            }
            emit (10) in :global
            emit (10) in :global
            emit (10) in :global
            print(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zb_13_all_valgrind () {
        val out = test("""
            spawn {
                loop {
                    await(10)
                    print(:1)
                    watching 10 {
                        await(| false)
                    }
                    print(:2)
                }
            }
            emit (10) in :global    ;; :1
            emit (10) in :global    ;; :2 (not :1 again)
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun zb_14_all_term_coro () {
        val out = test("""
            task T () {
                print(:1)
                watching (|false) {
                    await (|true)
                }
                print(:2)
                ;;print(:t)
            }
            spawn {
                val ts = tasks()
                spawn T() in ts
                ;;print(:every)
                every :e {
                    ;;print(:while)
                    loop t in ts {
                        ;;print(t, detrack(t), status(detrack(t)))
                        assert(status(;;;detrack;;;(t)) /= :terminated)
                    }
                }
            }
            ;;print(:bcast)
            emit(:e) in :global
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
        assert(out == "anon : (lin 2, col 19) : access throw : variable \"v\" is not declared\n") { out }
    }
    @Test
    fun zb_16_self_kill () {
        val out = test("""
            spawn {
                loop {
                    print(:10)
                    spawn {
                        print(:a)
                        await (:E)
                        do {
                            print(:b)
                            emit(:E) in :global
                            print(:c)
                        }
                        print(:d)
                    }
                    print(:20)
                    await (:E)
                    print(:30)
                }
            }
            print(:1)
            emit (nil) in :global
            print(:2)
            emit (:E) in :global
            print(:3)
        """, true)
        assert(out == ":10\n:a\n:20\n:1\n:2\n:b\n:30\n:10\n:a\n:20\n:3\n") { out }
    }
    @Test
    fun zb_17_tasks_it() {
        val out = test("""
            var ts
            set ts = tasks()
            print(type(ts))
            var T
            set T = task (v) {
                set ;;;task.;;;pub = v
                val v' = await(true)
            }
            spawn T(1) in ts
            spawn T(2) in ts
            
            loop t1 in ts {
                loop in ts {
                    print(;;;detrack;;;(t1).pub, ;;;detrack;;;(it).pub)
                }
            }
             emit (2) in :global
        """, true)
        assert(out == ":tasks\n1\t1\n1\t2\n2\t1\n2\t2\n") { out }
    }
    @Test
    fun zb_18_all_defer() {
        val out = test("""
            coro F () {
                defer {
                    print(:x)
                }
                yield()
                defer {
                    print(:y)
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
        //assert(out == "anon : (lin 3, col 51) : access throw : variable \"is'\" is not declared\n") { out }
        assert(out == "anon : (lin 4, col 27) : access throw : variable \"is'\" is not declared\n") { out }
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
            yield(drop(t))
        }
        do {
            val co = coroutine (C)
            resume co()
            loop {
                val v = f(co)
                print(v)
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
        coro Show () {
            var line = yield()
            loop {
                while line
                set line = yield()
                print(line)
            }
        }
        coro Send (co, nxt) {
            loop v in to-iter(co) {
                resume nxt(drop(v))
            }
            nil
        }
        do {
            create-resume(Take) thus { \take =>
                create-resume(Show) thus { \show =>
                    create-resume(Send, take, show)
                }
            }
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
            print(:ok)
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
                catch ;;;|true;;; {
                    loop b in ts {
                        throw(:x,;;;drop;;;(b))
                    }
                }
                nil
            }
            loop {
                emit (:X []) in :global
                until true
            }
            print(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }

    // PRELUDE / MATH

    @Test
    fun zc_01_min() {
        val out = test("""
            print(math.min(10,20), math.min(20,10))
        """, true)
        assert(out == "10\t10\n") { out }
    }
    @Test
    fun zc_02_max() {
        val out = test("""
            print(math.max(10,20), math.max(20,10))
        """, true)
        assert(out == "20\t20\n") { out }
    }
    @Test
    fun zc_03_between() {
        val out = test("""
            print(math.between(10, 1, 20))
            print(math.between(10, 100, 20))
            print(math.between(10, 15, 20 ))
        """, true)
        assert(out == "10\n20\n15\n") { out }
    }
    @Test
    fun zc_04_pi() {
        val out = test("""
            print(math.PI)
            print(math.sin(math.PI/2))
            print(math.cos(math.PI))
        """, true)
        assert(out == "3.14159\n1\n-1\n") { out }
    }
    @Test
    fun zc_05_floor() {
        val out = test("""
            print(math.floor(1.7))
            print(math.ceil(1.1))
            print(math.round(1.51))
        """, true)
        assert(out == "1\n2\n2\n") { out }
    }

    // PRELUDE / RANDOM

    @Test
    fun zd_01_random() {
        val out = test("""
            random.seed(0)
            print(random.next() % 100, random.next() % 100)
        """, true)
        assert(out == "83\t86\n") { out }
    }

    // MISC

    @Test
    fun zz_01_type() {
        val out = test("""
            print(static?(:number))
            print(static?(type([])))
            print(dynamic?(type(nil)))
            print(dynamic?(:vector))
            print(string?("oi"))
            print(string?(#[]))
            ;;print(type.nil?(nil))
            ;;print(type.dict?(nil))
        """, true)
        assert(out == "true\nfalse\nfalse\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun BUG_zz_02_track_bcast() {
        DEBUG = true
        val out = test("""
            $IS
            val B = task () {
                await(true)
            }
            val bs = tasks(5)
            spawn B() in bs
            func () {
                val b = next-tasks(bs,nil)
                emit(true) in b
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
            print(false)
                    val t = spawn {
                        await(:X)
                    }
                    spawn {
                        loop {
                            yield()
                        }
                    }
                    await(t)
            print(true)
                    await(:X)
                }
            }
            emit(true)
        """)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }
    @Test
    fun BUG_zz_04_par_arg() {
        val out = test("""
            $IS
            task T (v) {
                par {
                    every :X {
                        ;;;do;;; v      ;; TODO: upval is param
                    }
                } with {
                }
            }
            spawn T(100)
            emit(:X)
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_05_mem() {
        val out = test("""
            task T (v) {
                print(:ok)
                await(|it==:FIN)
            }
            val ts = tasks(1)
            spawn T() in ts
            spawn {
                loop {
                    await |it==:CHK {
                        var xxx = #[;;;next-tasks(ts);;;]
                        ;;set xxx = nil
                        drop(xxx)
                    }
                }
            }
            spawn T() in ts
            emit(:CHK)
            emit(:FIN)
            spawn T() in ts
        """)
        assert(out == ":ok\n:ok\n") { out }
    }
    @Test
    fun zz_06_fake() {
        val out = test("""
            task T () {
                val t = 10
                task S () {
                    print(t)
                }
                spawn {
                    print(:1)
                    await spawn S ()
                    print(:2)
                }
                await(|false)
            }
            spawn T()
        """)
        assert(out == ":1\n10\n:2\n") { out }
    }
    @Test
    fun zz_07_fake() {
        val out = test("""
            task T () {
                val t = 10
                task S () {
                    print(t)
                }
                spawn {
                    spawn S()
                }
            }
            spawn T()
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun zz_08_fake() {
        val out = test("""
            spawn {
                task T () {
                    set pub = [10]
                    spawn {
                        print(pub[0])
                    }
                }
                spawn T()
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun zz_10_coro_err() {
        val out = test("""
            func f (co1, xco2) {
                val' xco1 = coroutine(CO1)
                resume xco1()
            }
            coro CO2 () {
                nil
            }
            coro CO1 () {
                var x
                do {
                    val y = []
                    set x = y
                }
            }
            print(resume (f(CO1, coroutine(CO2))) ())
        """)
        assert(out == " |  anon : (lin 16, col 29) : f(CO1,coroutine(CO2))\n" +
                " |  anon : (lin 4, col 17) : (resume (xco1)())\n" +
                " |  anon : (lin 13, col 25) : x\n" +
                " v  throw : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_11_js_x_03() {
        val out = test("""
            $PLUS ; $COMP
            val v = #[]
            loop i in {1 => 2} {
                val' x = v
            }
            print(v)
        """)
        assert(out == "#[]\n") { out }
    }
}
