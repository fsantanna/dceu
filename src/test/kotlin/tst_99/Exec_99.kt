package tst_99

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_99 {
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
            println(not nil and not false)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun bb_03_or_and() {
        val out = test("""
            println(1 or throw(5))
            println(1 and 2)
            println(nil and 2)
            println(nil or 2)
        """)
        assert(out == "1\n2\nnil\n2\n") { out }
    }
    @Test
    fun bb_03_or_and_no() {
        val out = test("""
            println(1 or throw(5))
            println(nil or 2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bb_03_or_and_ok() {
        val out = test("""
^[2,13]^[2,13]println(^[2,23](^[2,21]1 thus { ceu_6 =>
^[3,25](if ^[3,28]ceu_6 ^[3,34]{
^[4,29]ceu_6
} else ^[5,32]{
^[2,26]^[2,26]throw(^[2,32]5)
})
})
)
^[3,13]^[3,13]println(^[3,25](^[3,21]nil thus { ceu_41 =>
^[4,25](if ^[4,28]ceu_41 ^[4,35]{
^[5,29]ceu_41
} else ^[6,32]{
^[3,28]2
})
})
)
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
                    tags(v1,v2)        => true
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
    fun bc_03_in() {
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
            func :rec f (v) {
                if v /= 0 {
                    println(v)
                    f(v - 1)
                }
            }
            f(3)
        """)
        assert(out == "3\n2\n1\n") { out }
    }

    // IF / ID-TAG

    @Test
    fun cj_01_if() {
        val out = test("""
            val v = if x=1 { x }
            println(v)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun cj_02_if() {
        val out = test("""
            data :X = [x]
            val i = if v:X=[1] { v.x }
            println(i)
        """)
        assert(out == "1\n") { out }
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
    fun dd_04_if() {
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
                println(it)
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
        assert(out == "anon : (lin 5, col 17) : declaration error : variable \"it\" is already declared\n") { out }
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
        assert(out == "anon : (lin 4, col 17) : declaration error : variable \"it\" is already declared\n") { out }

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
            val x = ifs 20 {
                == 10 => false
                == 20 => true
                else  => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_04_ifs() {
        val out = test("""
            var x = ifs it=20 {
                it == 10 => false
                true     => true
                it == 20 => false
                else     => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ff_05_ifs() {
        val out = test("""
            val x = ifs it=20 {
                it == 10 => false
                else     => true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun TODO_ff_06_ifs_is() {
        val out = test("""
            val t = :X []
            val x = ifs t {
                :Y   => false
                :X   => true
                else => false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun TODO_ff_07_ifs() {
        val out = test("""
            var x = ifs it=20 {
                it is? 10 => false
                true  => true
                it is? 20 => false
                else  => false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun TODO_08_ifs() {
        val out = test("""
            data :T = []
            val x = ifs 10 {
                true => :T []
                is? 0 => nil
            }
            println(x)
        """)
        assert(out == "anon : (lin 5, col 21) : access error : variable \"is'\" is not declared\n") { out }
    }
    @Test
    fun TODO_ff_09_ifs() {
        val out = test("""
            var x = ifs it=20 {
                it in? [1,20,1] => true
                else  => false
            }
            println(x)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun todo_ff_10_ifs() {  // plain value omits ==
        val out = test("""
            var x = ifs 20 {
                :no => false
                10  => false
                20  => true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }

    // THUS / SCOPE / :FLEET / :fleet

    @Test
    fun mm_01_tmp() {
        val out = test(
            """
            var x
            do {
                [1,2,3] thus { a =>
                    set x = a
                }
            }
            println(x)
        """
        )
        //assert(out == "[1,2,3]\n") { out }
        assert(out == "anon : (lin 5, col 25) : set error : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_01_tmp_err() {
        val out = test(
            """
            var x
            do {
                [1,2,3] thus { a =>
                    set x = drop(a)
                }
            }
            println(x)
        """
        )
        //assert(out == "[1,2,3]\n") { out }
        assert(out == "anon : (lin 5, col 34) : drop error : value is not movable\n") { out }
    }
    @Test
    fun mm_01_tmp_ok() {
        val out = test(
            """
            val x = do {
                [1,2,3] thus { a =>
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
            nil thus { it =>
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
            nil thus { it =>
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
            [0] thus { x =>
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
                [] thus { x =>
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
                [] thus { x =>
                    if x { drop(x) } else { [] }
                }
            }
            println(v)
        """)
        //assert(out == "[]\n") { out }
        assert(out == "anon : (lin 4, col 33) : drop error : value is not movable\n") { out }
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
        assert(out == "anon : (lin 2, col 21) : block escape error : cannot copy reference out\n") { out }
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
                v[0] thus { it =>
                    println(it)
                }
            }
            var g = func (v) {
                val evt = v
                f(evt)
            }
            g([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun mm_09_yield_err() {
        val out = test("""
            resume (coro () {
                yield(nil) thus { it => set it = nil }
            }) ()
        """)
        assert(out == "anon : (lin 3, col 41) : set error : destination is immutable\n") { out }
    }
    @Test
    fun mm_10_yield_err() {
        val out = test("""
            resume (coro () {
                yield(nil) thus { it => yield(nil) thus { x => nil } }
            }) ()
        """)
        assert(out == "anon : (lin 3, col 41) : yield error : unexpected enclosing thus\n") { out }
    }
    @Test
    fun mm_11_resume_yield() {
        val out = test("""
            $PLUS
            val CO = coro () {
                yield(nil) thus { it => 
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
                yield(v1) thus { x => x }
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
                yield(nil) thus { it =>
                    println(tags(it,:X)) ;; drop(it)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co(tags([],:X,true))
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun mm_14_yield_as() {
        val out = test("""
            val CO = coro () {
                yield(nil) thus { v =>
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
                yield(nil) thus { it :T =>
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
                val v = yield(nil) thus { x => x }
                yield(nil) ;;thus { it => nil }
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
        //assert(out == " |  anon : (lin 11, col 24) : t(v)\n" +
        //        " v  anon : (lin 3, col 25) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : resume (t)(v)\n" +
        //        " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
        assert(out == " |  anon : (lin 13, col 25) : (resume (t)(v))\n" +
                " v  anon : (lin 3, col 36) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_17_catch_yield_err() {
        val out = test("""
            coro () {
                catch ( it => do {
                    yield(nil) thus { it => nil }
                } )
                {
                    throw(:e1)
                }
            }
        """)
        assert(out == "anon : (lin 4, col 40) : declaration error : variable \"it\" is already declared\n") { out }
    }
    @Test
    fun mm_18_it() {
        val out = test("""
            val CO = coro () {
                yield(nil) thus { it =>
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
                yield(nil) thus { x =>
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
                yield(nil) thus { x =>
                    println(:it, x)
                }
            }
            val co = coroutine(CO)
            resume co()
            resume co([])
        """,)
        assert(out == "anon : (lin 3, col 35) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun mm_21_it_data() {
        val out = test("""
            data :X = [x]
            val CO = coro () {
                yield(nil) thus { x :X =>
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
                yield(nil) thus { x =>
                    yield(nil) thus { x =>
                        x
                    }
                }
            }
        """,)
        //assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing yield\n") thus { out }
        assert(out == "anon : (lin 4, col 39) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun mm_23_scope() {
        val out = test("""
            val T = coro () {
                val v = yield(nil) thus { it => 
                    println(it)
                    10
                }
                yield(nil) ;;thus { it => nil }
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
    }
    @Test
    fun mm_24_yield() {
        val out = test("""
            coro () {
                yield(nil) thus { x =>
                    yield(nil) thus { y => nil }
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : unexpected enclosing thus\n") { out }
    }
    @Test
    fun mm_25_gc_bcast() {
        val out = test("""
            var tk = task () {
                yield(nil) thus { it =>
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
            println(`:number CEU_GC_COUNT`)
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
                    yield(nil) ;;thus { it => nil }
                    10
                } )()
                yield (nil) thus { it => println(pub(it)) }
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
                yield(nil) thus { it => it}
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
        //assert(out == ":1\n:2\n1\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : broadcast e\n" +
        //        " v  anon : (lin 4, col 17) : resume error : cannot receive assigned reference\n") { out }
        //assert(out == "anon : (lin 11, col 39) : broadcast error : incompatible scopes\n" +
        //        ":error\n") { out }
        assert(out == " |  anon : (lin 14, col 21) : broadcast'(e,:task)\n" +
                " v  anon : (lin 4, col 28) : block escape error : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_28_data_await() {
        val out = test("""
            data :E = [x,y]
            spawn (task () {
                yield(nil) thus { it :E =>
                    println(it.x)
                }
            } )()
            broadcast (tags([10,20], :E, true))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_29_data_await() {
        val out = test("""
            data :E = [x,y]
            data :F = [i,j]
            spawn (task () {
                yield(nil) thus { it :E =>
                    println(it.x)
                }
                yield(nil) thus { it :F =>
                    println(it.j)
                }
            } )()
            broadcast (tags([10,20], :E, true))
            broadcast (tags([10,20], :F, true))
        """)
        assert(out == "10\n20\n") { out }
    }

    // LOOP / ITER / NUMERIC FOR

    @Test
    fun fg_01_iter() {
        val out = test("""
            $PLUS
            func iter (v) { v }
            func f (t) {
                if t.1 == t.2 {
                    nil
                } else {
                    set t.1 = t.1 + 1
                    t.1
                }
            }
            val it = [f, 0, 5]
            loop v in it {
                println(v)
            }
        """)
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun fg_02_iter() {
        val out = test("""
            $PLUS
            func iter (v) { v }
            func f (t) {
                if t.1 == t.2 {
                    nil
                } else {
                    set t.1 = t.1 + 1
                    t.1
                }
            }
            val it = [f, 0, 5]
            loop v in it {
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
        assert(out.contains("track: 0x")) { out }
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

    // LOOP / RET

    @Test
    fun fi_01_ret() {
        val out = test("""
            println(loop i in {0 => 1} {
                nil
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
                nil
            })
        """, true)
        assert(out == "false\n") { out }
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
                break(drop(t)) if true
            }
            println(x)
        """, true)
        assert(out == (" v  anon : (lin 7, col 13) : declaration error : cannot copy reference out\n")) { out }
    }
    @Test
    fun fj_02_iter() {
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
                nil
                yield() thus { it => println(it);it }
                nil
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
                nil
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
                set pub() = [10]
                yield(nil)
            }
            var t = spawn T ()
            var x = track(t)
            detrack(x) as { println(:1) }
            broadcast( nil )
            detrack(x) as { println(999) }
            println(:2)
        """)
        assert(out == ":1\n:2\n") { out }
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
                nil
            }
            coro bar (x1) {
                val x3 = yield(x1+1)
                val x8 = resume-yield-all coroutine(foo) (x3+1)
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
            spawn task {
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
    fun todo_ii_02_spawn_coro() {
        val out = test("""
            val co = spawn (coro () {
                println(1)
                yield()
                println(3)
            }) ()
            println(2)
            resume co()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun todo_ii_03_spawn_coro() {
        val out = test("""
            val co = spawn coro {
                println(1)
                yield()
                println(3)
            }
            println(2)
            resume co()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun ii_04_spawn() {
        val out = test("""
            spawn task {
                spawn task {
                    println(1)
                }
                nil
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_ii_05_coro() {
        val out = test("""
            val co = spawn coro {
                println(1)
                val v = yield()
                println(v)
            }
            resume co(10)
        """)
        assert(out == "1\n10\n") { out }
    }
    @Test
    fun ii_06_spawn_defer() {
        val out = test("""
            spawn task {
                do {
                    val t1 = spawn task {
                        ${AWAIT()}
                        println(1)
                    }
                    spawn task {
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
            spawn task {
                spawn task {
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
            spawn task {
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
            spawn task {
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
            spawn task {
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
            spawn task {
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
            spawn task {
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
            spawn task {
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
            spawn task {
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
    fun jj_08a_parand() {
        val out = test("""
            spawn task {
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
            spawn task {
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
            spawn task {
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
    fun jj_11_paror_defer() {
        val out = test("""
            spawn task {
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
            spawn task {
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
            spawn task {
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

    // AWAIT

    @Test
    fun kk_01_await() {
        val out = test("""
            $IS ; $XAWAIT
            task T () {
                await(it => it is? :x)
                println(1)
            }
            spawn T()
            broadcast (tags([],:x,true))
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun kk_02_await() {
        val out = test("""
            $IS ; $XAWAIT
            spawn task {
                println(0)
                await ( (it/=nil) and (it[:type]==:x) )
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
            $IS ; $XAWAIT
            data :x = []
            spawn task {
                println(0)
                await(:x)
                println(99)
            }
            do {
                println(1)
                broadcast (tags([], :y, true))
                println(2)
                broadcast (tags([], :x, true))
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun kk_04_await() {
        val out = test("""
            $IS ; $XAWAIT
            data :x = []
            spawn task {
                println(0)
                await(:x)
                println(99)
            }
            do {
                println(1)
                broadcast (tags([], :y, true))
                println(2)
                broadcast (tags([], :x, true))
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
            spawn task {
                loop {
                    await {
                        println(it)
                    }
                }
            }
            broadcast (@[])
        """)
        assert(out == "@[]\n") { out }
    }
    @Test
    fun kk_07_await() {
        val out = test("""
            spawn task {
                await {
                    println(it)
                }
                await {
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
            $IS ; $XAWAIT
            spawn task {
                await (it==2)
                println(2)
                await (it==1)
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
            $IS ; $XAWAIT
            data :X = []
            spawn task {
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
            $IS ; $XAWAIT
            spawn task {
                println(await(true))
            }
            do {
                val e = []
                broadcast(e)
            }
        """)
        assert(out == "true\n") { out }
    }

    // AWAIT / TASK

    @Test
    fun kl_01_await_task() {
        val out = test("""
            task T (v) {
                [v]
            }
            spawn task {
                val v = await spawn T(1)
                println(v)
            }
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun kl_02_await_task() {
        val out = test("""
            spawn task {
                task T () {
                    val v = await()
                    [v]
                }
                spawn task {
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
            spawn task {
                val t = spawn task {
                    println(:1)
                }
                await(t)
                println(:2)
            }
            println(:3)
        """, true)
        assert(out == ":1\n:2\n:3\n") { out }
    }

    // EVERY

    @Test
    fun km_01_every() {
        val out = test(
            """
            $IS ; $XAWAIT
            task T () {
                println(:1)
                every true {
                    until true
                    throw(999)
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
            $IS ; $XAWAIT
            task T () {
                println(:1)
                every true {
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
            $IS ; $XAWAIT
            data :X = []
            spawn task {
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

    // CLOCK

    @Test
    fun km_01_clock() {
        val out = test("""
            $IS ; $PLUS ; $MULT ; $COMP ; $XAWAIT
            data :Clock = [ms]
            spawn task {
                await (:2:ms)
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
            $IS ; $PLUS ; $MULT ; $COMP ; $XAWAIT
            data :Clock = [ms]
            spawn task {
                var x = 10
                every :x:ms {
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

    // WATCHING

    @Test
    fun ll_01_watching() {
        val out = test("""
            $IS ; $XAWAIT
            spawn task {
                watching (it==1) {
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

    // TOGGLE

    @Test
    fun mm_01_toggle() {
        val out = test("""
            task T (v) {
                set pub() = v
                toggle :Show {
                    println(pub())
                    every (it => (it is? :dict) and (it.sub==:draw)) {
                        println(it.v)
                    }
                }
            }
            spawn T(0)
            broadcast(@[(:sub,:draw),(:v,1)])
            broadcast(:Show [false])
            broadcast(@[(:sub,:draw),(:v,99)])
            broadcast(:Show [true])
            broadcast(@[(:sub,:draw),(:v,2)])
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun mm_02_toggle() {
        val out = test("""
            task T (v) {
                set pub() = v
                toggle :Show {
                    println(pub())
                    every :draw {
                        println(it.0)
                    }
                }
            }
            spawn T (0)
            broadcast (tags([1],     :draw, true))
            broadcast (tags([false], :Show, true))
            broadcast (tags([99],    :draw, true))
            broadcast (tags([true],  :Show, true))
            broadcast (tags([2],     :draw, true))
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun mm_03_toggle() {
        val out = test("""
            spawn task {
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
            func f (v,x) { v+x }
            val v = 10->f(20)
            println(v)
        """)
        assert(out == "30\n") { out }
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
                set pub() = [10]
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
                set pub() = [10]
                yield(nil)
            }
            val t = spawn T(nil)
            pub(t) thus { ceu_94 :X =>
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
            loop t in :tasks ts {
                println(type(t))
            }
        """)
        assert(out == "10\n:x-track\n") { out }
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
            println(\{x=>x+x}(2))
        """)
        assert(out.contains("4\n")) { out }
    }
    @Test
    fun pp_03_lambda () {
        val out = test("""
            println(\{x=>x}(1))
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

    // TUPLE DOT

    @Test
    fun tt_01_dots() {
        val out = test(
            """
            val x = [nil,[10]]
            println(x, x.1, x.1[0])
        """
        )
        assert(out == "[nil,[10]]\t[10]\t10\n") { out }
    }

    // CONSTRUCTOR

    @Test
    fun uu_01_cons() {
        val out = test("println(:T [])")
        assert(out == ":T []\n") { out }
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
    fun todo_COL_vv_04_ppp_push_err() {
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

    // DATA

    @Test
    fun xx_01_data_string_to_tag() {
        val out = test("""
            data :A = [] {
                :B = [] {
                    :C = []
                }
            }
            println(string-to-tag(":A"), string-to-tag(":A.B"), string-to-tag(":A.B.C"))
        """, true)
        assert(out == ":A\t:A.B\t:A.B.C\n") { out }
    }

    // PRELUDE

    @Test
    fun zz_01_ok() {
        val out = test("""
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_02_tasks() {
        val out = test("""
            val ts = tasks()
            loop in {1=>10} {
                ;;dump(ts)
                pass [ts]
            }
            println(:ok)
        """, true)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun TODO_zz_03_in() {
        val out = test("""
            println(10 in? [1,2,3])
            println(10 in? [1,10,3])
        """, true)
        assert(out == "false\ntrue\n") { out }
    }
    // TYPE-*
    @Test
    fun zz_04_type() {
        val out = test("""
            println(type-static?(:number))
            println(type-static?(type([])))
            println(type-dynamic?(type(nil)))
            println(type-dynamic?(:vector))
        """, true)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }
}
