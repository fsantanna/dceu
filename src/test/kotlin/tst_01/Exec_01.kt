package tst_01

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_01 {
    @Test
    fun native4_err() {
        val out = test(
            """
            var x
            set x = ``` ```
            print(x)
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun native7_err() {
        val out = test(
            """
             `
             
                $D{x.y}
                
             `
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : native throw : (lin 3, col 4) : invalid identifier\n") { out }
    }
    @Test
    fun TODO_native8() {    // cannot write C -> Ceu
        val out = test(
            """
            var x
            set x = 0
            var f
            set f = func' () {
                ```
                    ${D}x.Number = 20;
                ```
            }
            f()
            print(x)
        """
        )
        assert(out == "20\n") { out }
    }
    @Test
    fun native10_err() {
        val out = test(
            """
            ` ($D) `
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : native throw : (lin 1, col 4) : invalid identifier\n") { out }
    }
    @Test
    fun TODO_native17() {    // cannot write C -> Ceu
        val out = test(
            """
            func' () {
                val x = 1
                val y = `:number ${D}x.Number`
                ```
                    ${D}x.Number = 2;
                ```
                print(x,y)
            }()
        """
        )
        assert(out == "2\t1\n") { out }
    }
    @Test
    fun on_18_nat_loc() {
        val out = test("""
            val n = 10
            val f = func' () {
                val i = 5
                print(:i, i, `:number ${D}i.Number`)
                n
            }
            f()
        """)
        assert(out == ":i\t5\t5\n") { out }
    }
    @Test
    fun on_19_nat_glb() {
        val out = test("""
            `:pre int v = 10;`
            val f = func' () {
                `:number v`
            }
            print(f())
        """)
        assert(out == "10\n") { out }
    }

    // OPERATORS

    @Test
    fun ops_oth() {
        val out = test(
            """
            print(2**3)
            print(8//3)
            print(8%3)
        """, true
        )
        assert(out == "8\n2\n2\n") { out }
    }

    @Test
    fun gg_15_tags() {
        val out = test(
            """
            var t = tag(:T,   [])
            var s = tag(:T.S, [])
            print(to-number(:T), to-number(:T.S))
            print(sup?(:T,tag(t)), sup?(:T.S,tag(t)))
            print(sup?(:T,tag(s)), sup?(:T.S,tag(s)))
        """, true
        )
        assert(out == "15\t271\ntrue\tfalse\ntrue\ttrue\n") { out }
    }
    @Test
    fun gg_16_tags() {
        val out = test(
            """
            ;;;do;;; :A
            ;;;do;;; :A.I
            ;;;do;;; :A.I.X
            ;;;do;;; :A.I.Y
            ;;;do;;; :A.J
            ;;;do;;; :A.J.X
            ;;;do;;; :B
            ;;;do;;; :B.I
            ;;;do;;; :B.I.X
            ;;;do;;; :B.I.X.a
            print(sup?(:A, :A.I))
            print(sup?(:A, :A.I.X))
            print(sup?(:A.I.X, :A.I.Y))
            print(sup?(:A.J, :A.I.Y))
            print(sup?(:A.I.X, :A))
            print(sup?(:B, :B.I.X.a))
        """
        )
        assert(out == "true\ntrue\nfalse\nfalse\nfalse\ntrue\n") { out }
    }

    // CLOSURE / ESCAPE / FUNC / UPVALS

    @Test
    fun clo25_compose() {
        val out = test(
            """
            var comp = func' (f) {
                func' (v) {
                    f(v)
                }
            }
            var f = func' (x) {
                x
            }
            var ff = comp(f)
            print(ff(2))
        """,
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun clo26_compose() {
        val out = test(
            """
            var comp = func' (f,g) {
                func' (v) {
                    f(g(v))
                }
            }
            var f = func' (x) {
                x
            }
            var ff = comp(f,f)
            print(ff(2))
        """,
        )
        assert(out == "2\n") { out }
    }

    // NESTED

    @Test
    fun pq_01_nested() {
        val out = test("""
            do {
                var x = 10
                val g = func' :nested () {
                    set x = 100
                }
                g()
                print(x)
            }
        """
        )
        //assert(out == "100\n") { out }
        assert(out == "anon : (lin 4, col 31) : expected \"(\" : have \":nested\"\n") { out }
    }

    //  MEM-GC-REF-COUNT

    @Test
    fun gc_01() {
        DEBUG = true
        val out = test(
            """
            do {
                val xxx = []    ;; gc'd by block
                ;;nil
            }
            `ceu_dump_gc();`
            ;;print(`:number CEU_GC.free`)
        """
        )
        assert(out == ">>> GC: 2\n" +
                "    alloc = 3\n" +
                "    free  = 1\n"
        ) { out }
    }
    @Test
    fun gc_01x() {
        DEBUG = true
        val out = test(
            """
            var xxx = []
            set xxx = []
            `ceu_dump_gc();`
            ;;print(`:number CEU_GC.free`)
        """
        )
        //assert(out == "1\n") { out }
        assert(out == ">>> GC: 3\n" +
                "    alloc = 4\n" +
                "    free  = 1\n") { out }
    }
    @Test
    fun gc_02() {
        DEBUG = true
        val out = test(
            """
            ;;;do;;; []  ;; ;;;not;;; checked
            ;;;do;;; []  ;; ;;;not;;; checked
            ;;;do;;; nil
            `ceu_dump_gc();`
            ;;print(`:number CEU_GC.free`)
        """
        )
        //assert(out == "2\n") { out }
        //assert(out == "0\n") { out }
        assert(out == ">>> GC: 2\n" +
                "    alloc = 4\n" +
                "    free  = 2\n") { out }
    }
    @Test
    fun gc_03_cycle() {
        DEBUG = true
        val out = test(
            """
            var x = [nil]
            var y = [x]
            set x[0] = y
            set x = nil
            set y = nil
            `ceu_dump_gc();`
            ;;print(`:number CEU_GC.free`)
        """
        )
        //assert(out == "0\n") { out }
        assert(out == ">>> GC: 4\n" +
                "    alloc = 4\n" +
                "    free  = 0\n") { out }
    }
    @Test
    fun gc_04() {
        DEBUG = true
        val out = test(
            """
            var x = []
            var y = [x]
            set x = nil
            print(`:number CEU_GC.free`)
            set y = nil
            print(`:number CEU_GC.free`)
        """
        )
        assert(out == "0\n2\n") { out }
    }
    @Test
    fun gc_05() {
        DEBUG = true
        val out = test(
            """
            var x = []
            do {
                var y = x
            }
            set x = nil
            print(`:number CEU_GC.free`)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "0\n") { out }
    }
    @Test
    fun gc_06() {
        DEBUG = true
        val out = test(
            """
            var x = [[],[]]
            set x = nil
            print(`:number CEU_GC.free`)
        """
        )
        assert(out == "3\n") { out }
    }
    @Test
    fun gc_07() {
        DEBUG = true
        val out = test(
            """
            var f = func' (v) {
                v
            }
            #( #[ f([1]) ] )
            ;;;do;;; nil
            print(`:number CEU_GC.free`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "0\n") { out }
    }
    @Test
    fun gc_07x() {
        DEBUG = true
        val out = test(
            """
            ;;;do;;; #[ [1] ]
            ;;;do;;; nil
            print(`:number CEU_GC.free`)
        """
        )
        assert(out == "2\n") { out }
        //assert(out == "0\n") { out }
    }
    @Test
    fun gc_07y() {
        DEBUG = true
        val out = test(
            """
            var f = func' (v) {
                [2]
            }
            f([1])
            ;;;do;;; nil
            print(`:number CEU_GC.free`)
        """
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun gc_08() {
        DEBUG = true
        val out = test(
            """
            do {
                val out = do {
                    val ins = [1,2,3]
                    ;;;drop;;;(ins)
                }   ;; gc'd by block
                print(`:number CEU_GC.free`, `:number CEU_GC.free`)
            }
            print(`:number CEU_GC.free`, `:number CEU_GC.free`)
        """
        )
        assert(out == "0\t0\n1\t1\n") { out }
        //assert(out == "0\t0\n0\t0\n") { out }
    }
    @Test
    fun gc_09_err() {
        val out = test(
            """
            var out
            set out = do {
                var ins
                set ins = [1,2,3]
                ins
            }
            print(out)
        """
        )
        //assert(out == "anon : (lin 3, col 23) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun gc_10() {
        val out = test(
            """
            do {
                do {
                    var v = []
                    ;;;do;;; ;;;drop;;;(v)
                }
                print(`:number CEU_GC.free`)
            }
            print(`:number CEU_GC.free`)
        """, true
        )
        assert(out == "0\n1\n") { out }
        //assert(out == "1\n1\n") { out }
        //assert(out == "0\n0\n") { out }
    }
    @Test
    fun gc_11() {
        val out = test(
            """
            var f = func' (v) {
                v   ;; not captured, should be checked after call
            }
            f([])   ;; v is not captured
            ;; [] not captured, should be checked 
            print(`:number CEU_GC.free`)
        """
        )
        //assert(out == "anon : (lin 7, col 21) : f([10])\nanon : (lin 3, col 30) : set throw : incompatible scopes\n") { out }
        //assert(out == "1\n") { out }
        assert(out == "0\n") { out }
    }
    @Test
    fun gc_12() {
        val out = test(
            """
            print([])
            nil
            print(`:number CEU_GC.free`)
        """
        )
        //assert(out == "[]\n2\n") { out }
        assert(out == "[]\n1\n") { out }
    }
    @Test
    fun gc_15_arg() {
        val out = test(
            """
            var f = func' (v) {
                nil
            }
            f([])
            print(`:number CEU_GC.free`)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun gc_16_grow() {
        DEBUG = true
        val out = test("""
            val t = []
            do {
                val x = [t]
                ;;nil
            }
            do {
                val x = [t]
                ;;nil
            }
            do {
                val x = [t]
                ;;nil
            }
            dump(t)
        """)
        assert(out.contains("refs  = 2")) { out }
        //assert(out.contains("refs  = 3")) { out }
    }
    @Test
    fun gc_17_pool() {
        DEBUG = true
        val out = test("""
            do {
                var t1 = []
                do {
                    val t2 = t1
                    set t1 = nil
                }
                do {
                    ;;val t3 = []
                    print(`:number CEU_GC.free`)
                }
            }
        """)
        //assert(out == "0\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun gc_18_pool() {
        DEBUG = true
        val out = test("""
            do {
                var t1 = []
                do {
                    val t2 = t1
                    set t1 = nil
                }
                do {
                    val t3 = []
                    print(`:number CEU_GC.free`)
                }
            }
        """)
        assert(out == "1\n") { out }
    }

    // MISC

    @Test
    fun id_c() {
        val out = test(
            """
            var xxx
            set xxx = func' () {nil}
            print(xxx())
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun clo1() {
        val out = test(
            """
            var f
            set f = func' (x) {
                func' (y) {
                    if x { x } else { y }
                }
            }
            print(f(3)(1))
        """
        )
        assert(out == "3\n") { out }
    }

    // DATA / TEMPLATE

    @Test
    fun tplate01_err() {
        val out = test(
            """
            data :T = []
            data :T = []
        """, true
        )
        assert(out == "anon : (lin 3, col 18) : data throw : data :T is already declared\n") { out }
    }
    @Test
    fun tplate02_err() {
        val out = test(
            """
            data :T = []
            var t :T
            print(t.x)
        """
        )
        assert(out == "anon : (lin 4, col 23) : index throw : undeclared data field :x\n") { out }
    }
    @Test
    fun tplate03_err() {
        val out = test(
            """
            data :T = []
            var v :U
            print(v)
        """, true
        )
        //assert(out == "anon : (lin 3, col 19) : declaration throw : data :U is not declared\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun tplate04() {
        val out = test(
            """
            data :T = [x,y]
            var t :T
            set t = [1,2]
            print(t.x, t.y)
        """, true
        )
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun tplate05() {
        val out = test(
            """
            data :T = [x,y]
            var t :T
            set t = [1,2]
            print(t.x, t.y)
        """, true
        )
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun tplate06() {
        val out = test(
            """
            data :T = [x,y]
            data :T.S = [z]
            var s :T.S = [1,2,3]
            print(s.x,s.y,s.z)
        """, true
        )
        assert(out == "1\t2\t3\n") { out }
    }
    @Test
    fun tplate07() {
        val out = test(
            """
            data :T = [x,y]
            data :T.S = [z]
            var s :T.S = [1,2,3]
            var t :T = s
            var x :T.S = t
            print(s)
            print(t)
            print(x)
        """, true
        )
        assert(out == "[1,2,3]\n[1,2,3]\n[1,2,3]\n") { out }
    }
    @Test
    fun tplate08() {
        val out = test(
            """
            data :T = [x,y]
            data :T.S = [z]
            var t :T = tag(:T, [])
            var s :T.S
            set s = tag(:T.S, [])
            print(sup?(:T,tag(t)), sup?(:T.S,tag(t)))
            print(sup?(:T,tag(s)), sup?(:T.S,tag(s)))
        """, true
        )
        assert(out == "true\tfalse\ntrue\ttrue\n") { out }
    }
    @Test
    fun tplate09_err() {
        val out = test(
            """
            data :T = [x,x]
        """, true
        )
        assert(out == "anon : (lin 2, col 18) : data throw : found duplicate ids\n") { out }
    }
    @Test
    fun tplate10_err() {
        val out = test(
            """
            data :T   = [x,y]
            data :T.S = [x]
        """, true
        )
        assert(out == "anon : (lin 3, col 18) : data throw : found duplicate ids\n") { out }
    }
    @Test
    fun tplate11_err() {
        val out = test(
            """
            data :T.S = [x]
        """, true
        )
        assert(out == "anon : (lin 2, col 18) : tag throw : parent tag :T is not declared\n") { out }
    }
    @Test
    fun tplate12_err() {
        val out = test(
            """
            data :T = [x:U]
        """, true
        )
        assert(out == "anon : (lin 2, col 25) : data throw : data :U is not declared\n") { out }
    }
    @Test
    fun tplate12() {
        val out = test(
            """
            data :T = [v]
            data :U = [t:T]
            var u :U = [[10]]
            print(u.t.v)
        """, true
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun tplate13_err() {
        val out = test(
            """
            data :T = [v]
            data :U = [t:T]
            var u :U = [[10]]
            print(u.t.X)
        """, true
        )
        assert(out == "anon : (lin 5, col 25) : index throw : undeclared data field :X\n") { out }
        //assert(out == "anon : (lin 5, col 21) : index throw : expected number\n" +
        //        ":throw") { out }
    }
    @Test
    fun tplate14_err() {
        val out = test(
            """
            data :T = [v]
            data :U = [t:T]
            var u :U = [[10]]
            print(u.X.v)
        """, true
        )
        assert(out == "anon : (lin 5, col 23) : index throw : undeclared data field :X\n") { out }
    }
    @Test
    fun tplate15_err() {
        val out = test(
            """
            data :T = [v]
            data :U = [t:T,X]
            var u :U = [[10]]
            print(u.X.v)
        """, true
        )
        assert(out == " |  anon : (lin 5, col 21) : u[:X]\n" +
                " v  throw : out of bounds\n") { out }
    }
    @Test
    fun tplate16() {
        val out = test(
            """
            data :U = [a]
            data :T = [x,y]
            data :T.S = [z:U]
            var s :T.S
            set s = tag(:T.S, [1,2,tag(:U,[3])])
            print(sup?(:T,tag(s)), tag(s.z)==:U)
            set s.z = tag(:U, [10])
            print(sup?(:T,tag(s)), sup?(:U, tag(s.z)))
        """, true
        )
        assert(out == "true\ttrue\ntrue\ttrue\n") { out }
    }
    @Test
    fun tplate16x() {
        val out = test("""
            val x = tag(:x, [tag(:y,[ ])])
            print(sup?(:x,tag(x)))
            print(:ok)
        """)
        assert(out == "true\n:ok\n") { out }
    }
    @Test
    fun tplate17_func() {
        val out = test(
            """
            data :T = [x,y]
            var f = func' (t:T) {
                t.x
            }
            print(f([1,99]))
        """, true
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun tplate18_tup() {
        val out = test(
            """
            data :T = [v]
            val t :T = [[1,2,3]]
            print(t.v[1])
        """, true
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun tplate19_err() {
        val out = test(
            """
            val f = func' (x :X) { x.s }
            print(f([]))
        """
        )
        //assert(out == "anon : (lin 2, col 29) : declaration throw : data :X is not declared\n") { out }
        assert(out == " |  anon : (lin 2, col 36) : x[:s]\n" +
                " v  throw : expected number\n") { out }
    }
    @Test
    fun pp_20_tplate_func() {
        val out = test(
            """
            data :X = [s]
            val f = func' (x :X) {
                print(x.s)
            }
            f([10])
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun pp_21_tplate_question() {
        val out = test("""
            data :T = [x?]
            val t :T = [10]
            print(t.x?, :x?)
        """)
        assert(out == "10\t:x?\n") { out }
    }
    @Test
    fun pp_22_tplate_question() {
        val out = test("""
            data :T = [set]
            val t :T = [10]
            print(t.set, :set)
        """)
        assert(out == "10\t:set\n") { out }
    }

    // COPY / tuple

    @Test
    fun qq_01_copy() {
        val out = test("""
            print(copy(10), copy([]))
        """, true)
        assert(out == "10\t[]\n") { out }
    }
    @Test
    fun qq_02_copy() {
        val out = test("""
            val t = [1,2,3]
            val u = copy(t)
            print(u == t)
        """, true)
        assert(out == "false\n") { out }
    }
    @Test
    fun qq_03_copy() {
        val out = test("""
            val t1 = [1,2,3]
            val t2 = copy(t1)
            val t3 = t1
            set t1[2] = 999
            set t2[0] = 10
            print(t1)
            print(t2)
            print(t3)
        """, true)
        assert(out == "[1,2,999]\n[10,2,3]\n[1,2,999]\n") { out }
    }
    @Test
    fun qq_03x_copy() {
        val out = test("""
            func' () {
                val t1
            }
            val t1 = [1,2,3]
            print(t1)
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun qq_04_copy() {
        val out = test("""
            var f
            set f = func' (v) {
                ;;print(v)
                if v > 0 {
                    copy([f(v - 1)])
                } else {
                    0
                }
            }
            print(f(3))
        """, true)
        assert(out == "[[[0]]]\n") { out }
    }
    @Test
    fun qq_05_copy() {
        val out = test("""
            val out = do {
                val ins = [1,2,3]
                copy(ins)
            }
            print(out)
        """, true)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun qq_06_copy() {
        val out = test("""
            var x = [1,2,3]
            do {
                val y = copy(x)
                do {
                    set x = y
                }
            }
            print(x)
        """, true)
        assert(out == "[1,2,3]\n") { out }
        //assert(out == "anon : (lin 6, col 25) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
    }
    @Test
    fun qq_07_copy() {
        val out = test("""
            var x = [1,2,3]
            do {
                val y = copy(x)
                do {
                    set x = copy(y)
                }
            }
            print(x)
        """, true)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun qq_08_copy() {
        val out = test("""
            var v
            do {
                var x = [1,2,3]
                do {
                    val y = copy(x)
                    do {
                        set x = copy(y)
                        ;;`printf(">>> %d\n", ceu_mem->x.Dyn->hld_type);`
                        set v = x       ;; err
                    }
                }
            }
            print(v)
        """, true)
        assert(out == "[1,2,3]\n") { out }
        //assert(out == "anon : (lin 10, col 29) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
    }
    @Test
    fun TODO_qq_09_copy() {     // copy closure
        val out = test("""
            var f = func' (a) {
                func' () {
                    a
                }
            }
            var g = do {
                var t = [1]
                var i = copy(f(t))
                set t[0] = 10
                ;;;move;;;(i)
            }
            print(g())
        """, true)
        assert(out == "[1]\n") { out }
    }

    // COPY / vector

    @Test
    fun qr_01_copy_vector () {
        val out = test("""
            val v = #[1,2,3]
            val x = do ;;;export [];;; {
                val i = v[#v - 1]
                set v[#v - 1] = nil
                i
            }
            print(x, #v)
        """, true)
        assert(out == "3\t2\n") { out }
    }
    @Test
    fun qr_02_copy_vector() {
        val out = test("""
            val t1 = #[]        ;; [1,2]
            set t1[#t1] = 1
            val t2 = t1         ;; [1,2]
            val t3 = copy(t1)   ;; [1,20]
            set t1[#t1] = 2
            set t3[#t3] = 20
            print(t1)
            print(t2)
            print(t3)
        """, true)
        assert(out == "#[1,2]\n#[1,2]\n#[1,20]\n") { out }
    }

    // COPY / dict

    @Test
    fun qs_01_copy_dict() {
        val out = test("""
            val t1 = @[]
            set t1[:x] = 1
            val t2 = t1
            val t3 = copy(t1)
            set t1[:y] = 2
            set t3[:y] = 20
            print(t1)
            print(t2)
            print(t3)
        """, true)
        assert(out == "@[(:x,1),(:y,2)]\n@[(:x,1),(:y,2)]\n@[(:x,1),(:y,20)]\n") { out }
    }

    // COPY / tags

    @Test
    fun TODO_qt_01_copy_tags() {
        val out = test("""
            val t = tag(:x, [])
            val s = copy(t)
            print(s)
        """, true)
        assert(out == ":x []\n") { out }
    }

    // TYPE-*

    @Test
    fun rr_01_type() {
        val out = test("""
            print(type-static?(:number))
            print(type-static?(type([])))
            print(type-dynamic?(type(nil)))
            print(type-dynamic?(:vector))
        """, true)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }

    // OPTIMIZATION / CODE

    @Test
    fun ss_01_code_unused() {
        val out = test("""
            var f = func' () {
                nil
            }
            val g = func' () {
                f()
            }
            val h = func' () {
                h()
            }
            val i = func' () {
                42
            }
            print(`:ceu ${D}g`)
            print(`:ceu ${D}h`)
            print(i())
            print(`:ceu ${D}f`)
        """)
        assert(out.contains("nil\nnil\n42\nfunc: 0x")) { out }
    }

    // GROUP

    @Test
    fun tt_01_group() {
        val out = test("""
            group ;;;[a];;; {
                val a = 10
            }
            group ;;;[x];;; {
                var x
                set x = a
            }
            print(x)
        """)
        assert(out == "10") { out }
    }
    @Test
    fun tt_02_export_err() {
        val out = test("""
            ;;export [] {
            do {
                var a       ;; invisible
                set a = 10
            }
            var x
            set x = a
            print(x)
        """)
        assert(out == "anon : (lin 8, col 21) : access throw : variable \"a\" is not declared\n") { out }
    }
    @Test
    fun tt_03_export() {
        val out = test("""
            val x = group ;;;[];;; {
                val a = []
                a
            }
            print(x)
        """)
        assert(out == "[]") { out }
    }
    @Test
    fun tt_04_export() {
        val out = test("""
            group ;;;[aaa];;; {
                val aaa = 10
            }
            group ;;;[bbb];;; {
                val bbb = 20
            }
            print(aaa,bbb)
        """)
        assert(out == "10\t20\n") { out }
    }
    @Test
    fun tt_05_export() {
        val out = test("""
            group ;;;[f];;; {
                val v = []
                val f = func' () {
                    v
                }
                ;;print(v, f)
            }
            do {
                val x = f
                nil
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun tt_06_export() {
        val out = test("""
            do {
                group ;;;[f];;; {
                    val v = []
                    val f = func' () {
                        v
                    }
                    ;;print(v, f)
                }
                do {
                    val x = f
                    nil
                }
                print(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun tt_07_export() {
        val out = test("""
            val f
            f(group {
                nil
            })
        """)
        //assert(out == "anon : (lin 3, col 15) : group throw : unexpected context\n") { out }
        assert(out == " |  anon : (lin 3, col 13) : f(group { nil; })\n" +
                " v  throw : expected function\n") { out }
    }
    @Test
    fun tt_08_group() {
        val out = test("""
            group {
                group {
                    nil;
                };
            };
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun tt_09_group() {
        val out = test("""
            group {
                group {
                    val a = :a
                };
                val b = :b
            };
            print(a, b)
        """)
        assert(out == ":a\t:b\n") { out }
    }

    // ALL

    @Test
    fun zz_02_use_bef_dcl_func() {
        val out = test("""
            var f
            set f = func' () {
                print(v)
            }
            var v
            set v = 10
            f()
        """)
        //assert(out == "anon : (lin 4, col 25) : access throw : variable \"v\" is not declared\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun zz_03_func_scope() {
        val out = test("""
            val f = func' (v) {
                if v == nil {
                    1
                } else {
                    f(v[0])
                }
            }
            val t = [[nil]]
            print(f(t))
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun zz_04_arthur() {
        val out = test("""
            val tree1 = @[
                (:left, @[
                    (:left, nil),
                    (:right, nil)
                ]),
                (:right, nil)
            ]
            val itemCheck = func' (tree) {
                if tree == nil {
                    1
                }
                else {
                    itemCheck(tree[:left]) + itemCheck(tree[:right])
                }
            }
            print(itemCheck(tree1))
        """, true)
        assert(out == "3\n") { out }
    }
    @Test
    fun zz_05_dup_ids() {
        val out = test("""
            val f = func' (x,y) {
                y
            }
            print(f(1,2,3))
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun zz_opt_01() {
        val out = test(
            """
            print(do {
                var x
                set x = [0]
                x
            })
        """
        )
        //assert(out == "anon : (lin 2, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 5, col 17) : return throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[0]\n") { out }
    }
    @Test
    fun zz_07_iter() {
        val out = test("""
            val a = 1
            val b = 2
            do {
                val x = a
                print(b)
            }
        """)
        //assert(out == "anon : (lin 2, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 5, col 17) : return throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 2, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "2\n") { out }
    }
    @Test
    fun zz_08_nonlocs() {
        val out = test("""
            func' () {
                x
            }
        """)
        assert(out == "anon : (lin 3, col 17) : access throw : variable \"x\" is not declared\n") { out }
    }
}
