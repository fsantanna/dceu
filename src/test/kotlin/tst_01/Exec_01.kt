package tst_01

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_01 {
    // INDEX / TUPLE

    @Test
    fun cc_tuple14_drop_out() {
        val out = test(
            """
            val out = do {
                val ins = [1,2,3]
                ;;;drop;;;(ins)
            }
            print(out)
        """
        )
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun cc_15_tuple_call_scope() {
        val out = test(
            """
            val f = func' (v) {
                v
            }
            f([10])
            val x = f([10])
            print(f([10]))
        """
        )
        //assert(out == "anon : (lin 7, col 21) : f([10])\nanon : (lin 3, col 30) : set throw : incompatible scopes\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun cc_tuple15x_call_scope() {
        val out = test(
            """
            val f = func' (v) {
                v
            }
            print(f(10))
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_tuple16_drop() {
        val out = test(
            """
            val v = do {
                ;;;drop;;;([[1,2]])
            }
            print(v)
        """
        )
        assert(out == "[[1,2]]\n") { out }
        //assert(out == "anon : (lin 4, col 13) : invalid drop : expected assignable expression") { out }
    }
    @Test
    fun cc_vector17_drop() {
        val out = test(
            """
            val ttt = #[#[1,2]]
            print(ttt)
        """
        )
        assert(out == "#[#[1,2]]\n") { out }
    }
    @Test
    fun cc_dict18_drop() {
        val out = test(
            """
            val v = do {
                @[(:v,@[(:v,2)])]
            }
            print(v)
        """
        )
        assert(out == "@[(:v,@[(:v,2)])]\n") { out }
    }
    @Test
    fun cc_vector19_print() {
        val out = test(
            """
            print(#[#[1,2]])
        """
        )
        assert(out == "#[#[1,2]]\n") { out }
    }
    @Test
    fun cc_vector20() {
        val out = test(
            """
            val v = #[10]
            print(v[#v - 1])
        """, true
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_24_tuple() {
        val out = test(
            """
            val t = tuple(3)
            print(t)
            set t[1] = 10
            print(t)
        """
        )
        assert(out == "[nil,nil,nil]\n[nil,10,nil]\n") { out }
    }
    @Test
    fun cc_24x_tuple() {
        val out = test(
            """
            val t = tuple(3)
            print(t)
            print(set t[1] = 10)
        """
        )
        assert(out == "[nil,nil,nil]\n10\n") { out }
    }

    // DROP / HOLD / LEX

    @Test
    fun cm_00_drop () {
        val out = test(
            """
            val f = func' () {
                [0,'a']
            }
            print(f())
        """
        )
        assert(out == "[0,a]\n") { out }
    }
    @Test
    fun cm_01_drop () {
        val out = test(
            """
            val f = func' () {
                [0,'a']
            }
            val g = func' () {
                var v = f()
                ;;;drop;;;(v)
            }
            print(g())
        """
        )
        assert(out == "[0,a]\n") { out }
    }
    @Test
    fun cm_01x_drop () {
        val out = test(
            """
            val g = do {
                val v = do {
                    val x = [0,'a']
                    ;;;drop;;;(x)
                }
                ;;;drop;;;(v)
            }
            print(g)
        """
        )
        assert(out == "[0,a]\n") { out }
    }
    @Test
    fun cm_02_drop () {
        val out = test(
            """
            val t = [[1]]
            val s = ;;;drop;;;(t[0])
            val d = @[(1,[1])]
            val e = ;;;drop;;;(d[1])
            print(t,t[0],s)
            print(d,d[1],e)
        """
        )
        //assert(out == "[nil]\tnil\t[1]\n@[(1,nil)]\tnil\t[1]\n") { out }
        assert(out == "[[1]]\t[1]\t[1]\n@[(1,[1])]\t[1]\t[1]\n") { out }
    }
    @Test
    fun cm_03_drop() {
        val out = test(
            """
        var t = []
        do {
            val x = ;;;drop;;;(t)
            print(:x, x)
        }
        print(:t, t)
        """
        )
        assert(out == ":x\t[]\n:t\t[]\n") { out }
        //assert(out == ":x\t[]\n:t\tnil\n") { out }
    }
    @Test
    fun cm_04() {
        val out = test(
            """
            var f = func' (t) {
                var x = [;;;drop;;;(t)]
                ;;;drop;;;(x)
            }
            print(f([1,2,3]))
        """
        )
        assert(out == "[[1,2,3]]\n") { out }
    }
    @Test
    fun cm_05() {
        val out = test(
            """
            val t1 = [1]
            val t2 = [;;;drop;;;(t1)]
            val t3 = ;;;drop;;;(t2)
            print(t1, t2, t3)
        """
        )
        //assert(out == "nil\tnil\t[[1]]\n") { out }
        assert(out == "[1]\t[[1]]\t[[1]]\n") { out }
    }
    @Test
    fun cm_05x() {
        val out = test(
            """
            val t1 = [1]
            val t2 = [t1]
            print(t2)
        """
        )
        assert(out == "[[1]]\n") { out }
    }
    @Test
    fun cc_07a_global() {
        //DEBUG = true
        val out = test("""
            val e = func' () {
                nil
            }
            val g = func' () {
                e
            }
            print(g())
        """)
        assert(out.contains("func: 0x")) { out }
    }
    @Test
    fun cc_07_global() {
        //DEBUG = true
        val out = test("""
            val e = func' () {nil}
            ;;dump(e)
            val g = func' () {
                val co = [e]
                ;;;drop;;;(co)
            }
            val x = g()
            print(x)
        """)
        assert(out.contains("[func: 0x")) { out }
    }
    @Test
    fun cc_07y_global() {
        //DEBUG = true
        val out = test("""
            val g = func' () {
                val x
                10
            }
            print(g())
        """)
        assert(out == ("10\n")) { out }
    }
    @Test
    fun cc_07x_global() {
        val out = test("""
            val e = func' () {nil}
            ;;dump(e)
            val g = func' () {
                val co = [e]
                print(:e,e)
                (co)
            }
            val x = g()
            print(x)
        """)
        assert(out.contains("[func: 0x")) { out }
    }
    @Test
    fun cc_08_drop() {
        val out = test("""
            val F = func' (x) {
                ;;print(:1, x)
                func' () {
                    ;;print(:3, x)
                    x
                }
            }
            do {
                val x = []
                val f = F(x)
                ;;print(:2, f)
                print(f())
            }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun cc_08x_drop() {
        val out = test("""
            val F = func' (x) {
                func' () {
                    nil
                }
            }
            do {
                val x = :x
                val f = F(:arg)
                print(:f, f)
            }
        """)
        assert(out.contains(":f\tfunc: 0x")) { out }
    }
    @Test
    fun cc_09_drop_nest() {
        val out = test(
            """
            val f = func' (v) {
                ;; consumes v
                10
            }
            do {
                val x = []
                val y = f(;;;drop;;;(x))
                print(x, y)
            }
        """
        )
        //assert(out == "nil\t10\n") { out }
        assert(out == "[]\t10\n") { out }
    }
    @Test
    fun cc_09x_drop_nest() {
        val out = test(
            """
            val f = func' () {
                nil
            }
            f(nil)
            print(:ok)
        """
        )
        //assert(out == "nil\t10\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cc_09x_call_arg() {
        val out = test(
            """
            val f = func' (v) {
                1
            }
            f([])
            ;;dump(f)
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun cc_10_drop_multi_err() {
        val out = test("""
            val x = do {
                val t1 = [1,2,3]
                val t2 = t1
                ;;;drop;;;(t1)        ;; ~ERR~: `t1` has multiple references
            }                   ;; not a problem b/c gc_dec does not chk current block
            print(x)
        """)
        //assert(out == "anon : (lin 5, col 22) : drop throw : value contains multiple references\n") { out }
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun cc_10_drop_multi_err_why() {
        val out = test("""
            val t = [1,[99],3]
            do {
                val y = do {
                    val x = t[1]
                    ;;;drop;;;(x)
                }
                print(y)
            }
            ;;`ceu_gc_collect();`
            print(t)
        """)
        //assert(out == "anon : (lin 6, col 26) : drop throw : value contains multiple references\n") { out }
        //assert(out == "anon : (lin 4, col 25) : block escape throw : cannot move pending reference in\n") { out }
        assert(out == "[99]\n" +
                "[1,[99],3]\n") { out }
    }
    @Test
    fun cc_11_drop_deep() {
        val out = test("""
            do {
                val t1 = [1]
                do {
                    val t2 = ;;;drop;;;(t1)
                    print(t2)
                }
                print(t1)
            }
        """)
        //assert(out == "[1]\nnil\n") { out }
        assert(out == "[1]\n[1]\n") { out }
    }
    @Test
    fun cc_12_drop_deep() {
        val out = test("""
            do {
                val t1 = [1]
                val t2 = t1
                do {
                    val t3 = ;;;drop;;;(t1)
                    print(t2)
                }
                print(t1)
            }
        """)
        //assert(out == "anon : (lin 6, col 21) : declaration throw : cannot move pending reference in\n") { out }
        //assert(out == "anon : (lin 6, col 35) : drop throw : value contains multiple references\n") { out }
        //assert(out == "[1]\nnil\n") { out }
        assert(out == "[1]\n[1]\n") { out }
    }
    @Test
    fun cc_13_drop_cycle() {
        val out = test(
            """
            val z = do {
                var x = [nil]
                var y = [x]
                set x[0] = y
                ;;;drop;;;(x)
            }
            print(z[0][0] == z)
        """
        )
        //assert(out == "anon : (lin 6, col 22) : drop throw : value contains multiple references\n") { out }
        assert(out == "true\n") { out }
    }
    @Test
    fun cc_13_drop_cycle_x() {
        val out = test(
            """
            val z = do {
                var x = [nil]
                var y = [x]
                set x[0] = y
                ;;;;;;drop;;;;;;(x)
            }
            print(z[0][0] == z)
        """
        )
        assert(out == "true\n") { out }
    }
    @Test
    fun cc_13a_drop_cycle() {
        val out = test("""
            var x = [nil]
            var y = [x]
            set x[0] = y
            print(x[0] == y[0][0])
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun cc_14_drop_cycle() {
        val out = test(
            """
            val z = do {
                var x = [nil]
                var y = [x]
                set x[0] = y
                ;;;do drop;;;(x)
                y
            }
            print(z[0][0] == z)
        """
        )
        assert(out == "true\n") { out }
    }
    @Test
    fun cc_15_drop_passd() {
        DEBUG = true
        val out = test(
            """
            val f = func' (v) {
                ;;;drop;;;(v)
            }
            print(f([1]))
        """
        )
        //assert(out == "anon : (lin 3, col 22) : drop throw : fleeting argument\n") { out }
        assert(out == "[1]\n") { out }
    }

    // DICT

    @Test
    fun dict7_init() {
        val out = test("""
            var t = @[x=1, y=2]
            print(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun dd_dict3() {
        val out = test(
            """
            val t = @[(:x,1)]
            print(t[:y])
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun dd_dict4() {
        val out = test(
            """
            val t = @[(:x,1)]
            set t[:x] = 2
            print(t[:x])
        """
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun dd_dict5() {
        val out = test(
            """
            val t = @[]
            set t[:x] = 1
            set t[:y] = 2
            print(t)
        """
        )
        assert(out == "@[(:x,1),(:y,2)]\n") { out }
    }
    @Test
    fun dd_11_dict_set() {
        val out = test(
            """
            val v = @[]
            do {
                set v[[]] = true
            }
            print(v)
        """
        )
        assert(out == "@[([],true)]\n") { out }
    }
    @Test
    fun dd_12_dict_set_err() {
        val out = test(
            """
            val v = @[]
            do {
                val k = []
                set v[k] = true
            }
            print(v)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : store throw : cannot assign reference to outer scope\n") { out }
        assert(out == "@[([],true)]\n") { out }
    }
    @Test
    fun dd_dict13_key_nil() {
        val out = test("""
            var x
            set x = @[(nil,10)]
            print(x[nil])
        """)
        //assert(out == "anon : (lin 3, col 21) : dict throw : index cannot be nil\n") { out }
        assert(out == " |  anon : (lin 3, col 21) : @[(nil,10)]\n" +
                " v  throw : index cannot be nil\n") { out }
    }
    @Test
    fun dd_13_dict_key_nil() {
        val out = test("""
            val x = @[]
            set x[nil] = 10
            print(x[nil])
        """)
        //assert(out == "anon : (lin 3, col 17) : dict throw : index cannot be nil\n") { out }
        assert(out == " |  anon : (lin 3, col 17) : x[nil]\n" +
                " v  throw : index cannot be nil\n") { out }
    }
    @Test
    fun dd_14_dict() {
        val out = test("""
            val tree1 = @[
                (:left, @[
                    (:left, 1),
                    (:right, 2)
                ]),
                (:right, 3)
            ]

            val tree2 = @[
                left= @[
                    left=1,
                    right=2
                ],
                right=3
            ]
            print(tree1)
            print(tree2)
        """)
        assert(out == "@[(:left,@[(:left,1),(:right,2)]),(:right,3)]\n" +
                "@[(:left,@[(:left,1),(:right,2)]),(:right,3)]\n") { out }
    }

    // DICT / NEXT

    @Test
    fun de_00_dict_next() {
        val out = test("""
            val t = @[]
            print(next-dict(t))
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun de_01_dict_next() {
        val out = test("""
            val t = @[]
            print(t[nil])
            set t[nil] = 1
        """)
        //assert(out == "anon : (lin 4, col 17) : dict throw : index cannot be nil\n" + "nil\n") { out }
        assert(out == " |  anon : (lin 4, col 17) : t[nil]\n" +
                " v  throw : index cannot be nil\n" +
                "nil\n") { out }
    }
    @Test
    fun de_02_next() {
        val out = test(
            """
            val t = @[]
            set t[:x] = 1
            set t[:y] = 2
            var k
            set k = next-dict(t)
            print(k, t[k])
            set k = next-dict(t,k)
            print(k, t[k])
            set k = next-dict(t,k)
            print(k, t[k])
        """
        )
        assert(out == ":x\t1\n:y\t2\nnil\tnil\n") { out }
    }
    @Test
    fun de_02x_next() {
        val out = test(
            """
            val t = @[(:x,1),(:y,2)]
            print(next-dict(t,:x))
        """
        )
        assert(out == ":y\n") { out }
    }
    @Test
    fun de_04_next() {
        val out = test("""
            next-dict(nil)
        """)
        //assert(out == "anon : (lin 2, col 13) : next-dict(nil) : next-dict throw : expected dict\n") { out }
        assert(out == " |  anon : (lin 2, col 13) : next-dict(nil)\n" +
                " v  throw : expected dict\n") { out }
    }

    // VECTOR

    @Test
    fun ee_vector1() {
        val out = test(
            """
            print(#[])
        """
        )
        assert(out == "#[]\n") { out }
    }
    @Test
    fun vector2() {
        val out = test(
            """
            print(type(#[]))
            print(#[1,2,3])
        """
        )
        assert(out == ":vector\n#[1,2,3]\n") { out }
    }
    @Test
    fun vector3_err() {
        val out = test(
            """
            var v
            set v = #[]
            ;;print(#v)
            set v[#v] = 10
            ;;print(v)
            set v[5] = 10   ;; throw
        """
        )
        //assert(out == "anon : (lin 7, col 17) : index throw : out of bounds\n0\n#[10]\n") { out }
        assert(out == " |  anon : (lin 7, col 17) : v[5]\n" +
                " v  throw : out of bounds\n") { out }
    }
    @Test
    fun vector4() {
        val out = test(
            """
            print(#(#[1,2,3]))
        """
        )
        assert(out == "3\n") { out }
    }
    @Test
    fun vector5() {
        val out = test(
            """
            val v = #[1,2,3]
            print(#v, v)
            set v[#v] = 4
            set v[#v] = 5
            print(#v, v)
            set v[#v - 1] = nil
            print(#v, v)
            ;;set #v = 2       ;; throw
        """, true
        )
        assert(out == "3\t#[1,2,3]\n5\t#[1,2,3,4,5]\n4\t#[1,2,3,4]\n") { out }
    }
    @Test
    fun vector6a_err() {
        val out = test(
            """
            #[1,nil,3]  ;; v[2] = nil, but #v===1
        """
        )
        assert(out.contains("ceu_vector_set: Assertion `i == vec->its-1' failed.")) { out }
        //assert(out.contains("ceux_vector: Assertion `v.type != CEU_VALUE_NIL' failed.")) { out }
    }
    @Test
    fun vector6b_err() {
        val out = test(
            """
            #[1,'a',3]  ;; different type
        """
        )
        assert(out.contains("ceu_vector_set: Assertion `v.type == vec->unit' failed.")) { out }
    }
    @Test
    fun vector7_err() {
        val out = test(
            """
            #1
        """
        )
        //assert(out == "anon : (lin 2, col 13) : {{#}}(1) : length throw : not a vector\n") { out }
        assert(out == " |  anon : (lin 2, col 13) : {{#}}(1)\n" +
                " v  throw : not a vector\n") { out }
    }
    @Test
    fun vector8_err() {
        val out = test(
            """
            var v
            set v = #[1,2,3]
            print(v[#v])   ;; err
        """
        )
        //assert(out == "anon : (lin 4, col 23) : index throw : out of bounds\n") { out }
        assert(out == " |  anon : (lin 4, col 21) : v[{{#}}(v)]\n" +
                " v  throw : out of bounds\n") { out }
    }
    @Test
    fun vector9_err() {
        val out = test(
            """
            set #v = 0
        """
        )
        assert(out == "anon : (lin 2, col 13) : set throw : expected assignable destination\n") { out }
    }
    @Test
    fun vector13_add() {
        val out = test(
            """
            do {       
                val ceu_ifs_17 = true    
                val v = #[]
                if true {                                                           
                    set v[{{#}}(v)] = 10                                              
                } else {                                                            
                    nil
                }
                print(v)
            }
        """
        )
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun vector14_err() {
        val out = test(
            """
            ;;val v
            set v[#v-1] = nil
        """, true
        )
        assert(out == "anon : (lin 3, col 17) : access throw : variable \"v\" is not declared\n") { out }
    }
    @Test
    fun vector15_err() {
        val out = test(
            """
            val v
            v[#v-1]
        """, true
        )
        //assert(out == "anon : (lin 3, col 16) : access throw : \"v-1\" is ambiguous with \"v\"") { out }
        //assert(out == "anon : (lin 3, col 15) : {{#}}(v) : length throw : not a vector\n") { out }
        assert(out == " |  anon : (lin 3, col 15) : {{#}}(v)\n" +
                " v  throw : not a vector\n") { out }
    }
    @Test
    fun vector16_copy() {
        val out = test("""
            val t1 = #[]
            set t1[#t1] = 1
            print(t1)
        """, true)
        assert(out == "#[1]\n") { out }
    }
    @Test
    fun ee_17_vector_nil_err() {
        val out = test("""
            val t = #[nil,nil,10]
            print(#t, t[0])
        """)
        //assert(out.contains("ceu_vector_set: Assertion `v.type != CEU_VALUE_NIL' failed")) { out }
        assert(out.contains("ceu_vector_set: Assertion `i == vec->its-1' failed.")) { out }
    }
    @Test
    fun BUG_ee_18_vector_f() {
        val out = test("""
            func' f () { ('z',0) }
            val t = #['a', (f())]   ;; need way to adjust arity to 1
            print(t)
        """)
        assert(out.contains("ceux_vector: Assertion `v.type != CEU_VALUE_NIL' failed")) { out }
    }

    // STRINGS / CHAR

    @Test
    fun string1() {
        val out = test(
            """
            val v = #['a','b','c']
            set v[#v] = 'a'
            set v[2] = 'b'
            print(v[0])
            `puts(${D}v.Dyn->Vector.buf);`
            print(v)
        """
        )
        assert(out == "a\nabba\nabba\n") { out }
    }

    // ARGC / ARGV

    @Test
    fun ff_01_argc() {
        val out = test("""
            print(ARGS)
        """)
        assert(out == "[./out.exe]\n") { out }
    }

    // SET

    @Test
    fun set1() {
        val out = test(
            """
            val x = [10]
            print(x)
        """
        )
        assert(out == "[10]\n") { out }
    }
    @Test
    fun set2() {
        val out = test(
            """
            val x = [10,20,[30]]
            set x[1] = 22
            set x[2][0] = 33
            print(x)
        """
        )
        assert(out == "[10,22,[33]]\n") { out }
    }
    @Test
    fun set_err1() {
        val out = test(
            """
            set 1 = 1
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : set throw : expected assignable destination\n") { out }
    }
    @Test
    fun set_err2() {
        val out = test(
            """
            set [1] = 1
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : set throw : expected assignable destination\n") { out }
    }
    @Test
    fun set_index() {
        val out = test(
            """
            val i = 1
            print([1,2,3][i])
        """
        )
        assert(out == "2\n") { out }
    }

    // DO

    @Test
    fun do1() {  // set whole tuple?
        val out = test(
            """
            do { nil }
            ;;print(ARGS)
        """
        )
        assert(out == "") { out }
    }
    @Test
    fun do_01x() {
        val out = test("""
            print(do {})
        """)
        //assert(out == "nil\n") { out }
        //assert(out == "\n") { out }
        assert(out == "anon : (lin 2, col 25) : expected expression : have \"}\"\n") { out }
    }
    @Test
    fun do_02x() {
        val out = test("""
            print(do {nil}, nil)
        """)
        assert(out == "nil\tnil\n") { out }
    }
    @Test
    fun do2() {
        val out = test(
            """
            do {
                val a = 1
                print(a)
            }
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun do3() {
        val out = test(
            """
            val x = do {
                val a = 10
                a
            }
            print(x)
        """
        )
        assert(out == "10") { out }
    }
    @Test
    fun do4() {
        val out = test(
            """
            val x = do {nil}
            print(x)
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun do5() {
        val out = test(
            """
            do {
                val x
            }
            do {
                val x
            }
            print(1)
        """
        )
        assert(out == "1\n") { out }
    }

    @Test
    fun if5() {
        val out = test(
            """
            if true {
                val aaa = 10
            } else {
                val xxx = 10
            }
            if true {
                val bbb = 20
            } else {
                val yyy = 20
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun export8() {
        val out = test(
            """
            do {
                val v = []
                val f = func' () {
                    v                   ;; holds outer v
                }
                do {
                    val x = f           ;; nested do, but could be in par block from bcast
                    ;;nil
                }
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    // SCOPE

    @Test
    fun scope1() {
        val out = test(
            """
            var x
            do {
                set x = [1,2,3]
            }
            print(x)
        """
        )
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun scope_err2() {
        val out = test(
            """
            var x
            do {
                var a
                set a = [1,2,3]
                set x = a           ;; err: x<a
            }
            print(x)
        """
        )
        //assert(out == "anon : (lin 3, col 13) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set throw : cannot assign reference to outer scope\n") { out }
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun scope4() {
        val out = test(
            """
            var x
            do {
                set x = [1,2,3]
                set x[1] = [4,5,6]
                do {
                    val y = [10,20,30]
                    set y[1] = x[1]
                    set x[2] = y[1]
                }
            }
            print(x)
        """
        )
        assert(out == "[1,[4,5,6],[4,5,6]]\n") { out }
    }
    @Test
    fun scope4x() {
        val out = test(
            """
            val x = [0]
            do {
                set x[0] = [1]
                nil
            }
            print(x)
        """
        )
        assert(out == "[[1]]\n") { out }
    }
    @Test
    fun scope5_err() {
        val out = test("""
            var x
            do {
                set x = [1,2,3]
                var y
                set y = [10,20,30]
                set x[2] = y
            }
            print(x)
        """)
        //assert(out == "anon : (lin 7, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 7, col 21) : store throw : cannot assign reference to outer scope\n") { out }
        assert(out == "[1,2,[10,20,30]]\n") { out }
    }
    @Test
    fun scope6() {
        val out = test(
            """
            var x
            do {
                set x = [1,2,3]
                var y
                set y = 30
                set x[2] = y
            }
            print(x)
        """
        )
        assert(out == "[1,2,30]\n") { out }
    }
    @Test
    fun scope7() {
        val out = test(
            """
            var xs
            set xs = do {
                [10]
            }
            print(xs)
        """
        )
        assert(out == "[10]\n") { out }
    }
    @Test
    fun scope9_err() {
        val out = test(
            """
            var x
            do {
                var a
                set a = @[(1,[])]
                set x = a
            }
            print(x)
        """
        )
        assert(out == "@[(1,[])]\n") { out }
        //assert(out == "anon : (lin 6, col 21) : set throw : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun scope10x() {
        DEBUG = true
        val out = test(
            """
            var out
            do {
                var x
                set x = []
                ;;dump(x)
                set out = [x]   ;; err
            }
            print(1)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 7, col 21) : set throw : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun scope10_err() {
        val out = test("""
            var out
            do {
                val x = []
                set out = [x]   ;; err
            }
            print(1)
        """)
        //assert(out == "anon : (lin 5, col 21) : set throw : cannot assign reference to outer scope\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun scope11_err() {
        val out = test(
            """
            var out
            do {
                var x
                set x = []
                set out = #[x]
            }
            print(1)
        """
        )
        //assert(out == "anon : (lin 6, col 21) : set throw : cannot assign reference to outer scope\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun scope12_err() {
        val out = test(
            """
            var out
            do {
                var x
                set x = []
                set out = @[(1,x)]
            }
            print(1)
        """
        )
        //assert(out == "anon : (lin 6, col 21) : set throw : cannot assign reference to outer scope\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun scope13_tuple_err() {
        val out = test(
            """
            val v = do {
                val x = []
                [x]         ;; invalid return
            }
            print(v)
        """
        )
        //assert(out == "anon : (lin 2, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[[]]\n") { out }
    }
    @Test
    fun scope15_global_func() {
        val out = test(
            """
            val f = func' () { nil }
            val g = func' (v) {
                [f, v]
            }
            val tup = do {
                val v = []
                val x = g(v)
                print(x)
            }
        """
        )
        assert(out.contains("[func: 0x")) { out }
    }
    @Test
    fun scope15x_global_func() {
        val out = test(
            """
            val t = []
            val x = do {
                [t]
            }
            print(x)
        """
        )
        assert(out.contains("[[]]\n")) { out }
    }
    @Test
    fun scope16_glb_vs_tup() {
        val out = test(
            """
            val g = func' () { nil }
            val f = func' (v) {
                [g, v]
            }
            do {
                val t = []
                f(t)
                nil
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope17_glb_vs_tup() {
        val out = test(
            """
            val f = func' () {
                nil
            }
            do {
                val t = []
                ;;;do;;; [f, t]
                nil
            }
            do {
                val t = []
                ;;;do;;; [f, t]
                nil
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope19_tup() {
        val out = test(
            """
            do {
                val t1 = []
                do {
                    val t2 = []
                    ;;;do;;; [t1,[],t2]
                    nil
                }
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope19x_tup() {
        val out = test("""
            val t1 = []
            do {
                val t2 = []
                ;;;do;;; [t1,t2]
                nil
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope19x_tup_err() {
        val out = test(
            """
            do {
                val t1 = []
                do {
                    val t2 = []
                    ;;;do;;; [t1,[],t2]
                }
            }
            print(:ok)
        """
        )
        //assert(out == "anon : (lin 4, col 17) : block escape throw : cannot copy reference out\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope19_leak() {
        val out = test(
            """
            val t1 = [9]
            do {
                ;;;do;;; [t1]
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope20_vec() {
        val out = test(
            """
            do {
                val t1 = []
                do {
                    val t2 = []
                    ;;;do;;; #[t1,[],t2]
                    nil
                }
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope21_dict() {
        val out = test(
            """
            do {
                val t1 = []
                do {
                    val t2 = []
                    ;;;do;;; @[(t1,t2)]
                    nil
                }
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope22a_tup() {
        val out = test(
            """
            val d = [nil]
            do {
                val t2 = []
                set d[0] = t2
                nil
            }
            print(1)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : store throw : cannot assign reference to outer scope\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun scope22b_vec() {
        val out = test(
            """
            val d = [nil]
            do {
                val t2 = #[]
                set d[0] = t2
                nil
            }
            print(1)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : store throw : cannot assign reference to outer scope\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun scope22c_dic() {
        val out = test(
            """
            val d = @[]
            do {
                val t2 = []
                set d[t2] = 10
                nil
            }
            print(1)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : store throw : cannot assign reference to outer scope\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun scope22d_dic() {
        val out = test(
            """
            val d = @[]
            do {
                val t2 = []
                set d[10] = t2
                nil
            }
            print(1)
        """
        )
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 5, col 21) : store throw : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun scope22x_dict() {
        val out = test(
            """
            do {
                val t1 = []
                val d = @[(t1,t1)]
                do {
                    val t2 = []
                    set d[t2] = t2
                    nil
                }
            }
            print(:ok)
        """
        )
        //assert(out == "anon : (lin 7, col 25) : store throw : cannot assign reference to outer scope\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun scope22y_dict() {
        val out = test(
            """
            val t1 = []
            val d = @[(t1,t1)]
            do {
                val t2 = []
                set d[:x] = t2
                nil
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 6, col 21) : store throw : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun scope22z_dict() {
        val out = test(
            """
            val xxx = []
            val yyy = @[(xxx,xxx)]
            print(yyy)
            throw(:ok)
        """
        )
        assert(out == " |  anon : (lin 5, col 13) : throw(:ok)\n" +
                " v  throw : :ok\n" +
                "@[([],[])]\n") { out }
    }
    @Test
    fun scope22z_tuple() {
        val out = test(
            """
            val xxx = [1]
            val yyy = [xxx,xxx]
            print(yyy)
            throw(:ok)
        """
        )
        assert(out == " |  anon : (lin 5, col 13) : throw(:ok)\n" +
                " v  throw : :ok\n" +
                "[[1],[1]]\n") { out }
    }
    @Test
    fun scope26_args() {
        val out = test(
            """
            val f = func' (v) {
                [v, [2]]
            }
            do {
                val v = [1]
                val x = f(v)
                print(x)
            }
        """
        )
        assert(out == "[[1],[2]]\n") { out }
    }
    @Test
    fun scope26y_args() {
        val out = test(
            """
            val f = func' (v) {
                [v, [2]]
            }
            val v = [1]
            val x = f(v)
            print(x)
        """
        )
        assert(out == "[[1],[2]]\n") { out }
    }
    @Test
    fun scope26z_args() {
        val out = test(
            """
            val v = [1]
            val x = do {
                [v, [2]]
            }
            print(x)
        """
        )
        assert(out == "[[1],[2]]\n") { out }
    }
    @Test
    fun scope26x_args_err() {
        val out = test(
            """
            val f = func' (v) {
                [v, [2]]
            }
            val y = do {
                val v = [1]
                val x = f(v)
                x
            }
            print(y)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[[1],[2]]\n") { out }
    }
    @Test
    fun scope27_glb_vs_tup_err() {
        val out = test("""
            val f = func' (t) {
                val x = []
                ;;dump(x)
                set t[0] = x
                ;;dump(x)
                t
            }
            print(f([nil]))
        """)
        //assert(out == "anon : (lin 2, col 30) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[[]]\n") { out }
    }
    @Test
    fun scope28_err() {
        val out = test(
            """
            var f = func' (v) {
                [v]
            }
            var g = do {
                val t = [1]
                f(t)
            }
            print(g)
        """
        )
        //assert(out == "anon : (lin 5, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[[1]]\n") { out }
    }
    @Test
    fun scope29() {
        val out = test("""
            val f = func' (v) {
                v
            }
            val x = [2]
            val y = f([x])
            print(y)

        """)
        assert(out == "[[2]]\n") { out }
    }
    @Test
    fun scope29x() {
        val out = test("""
            val f = func' (v) {
                v
            }
            do {
                val x = [2]
                print(f([x]))
            }
        """)
        assert(out == "[[2]]\n") { out }
    }
    @Test
    fun scope30_cyc() {
        val out = test("""
            val cycle = func' (v) {
                set v[3] = v
                v
            }
            var a = [1]
            var d = do {
                var b = [2]
                var c = cycle([a,b,[3],nil])
                ;;;drop;;;(c)
            }
            ;;print(d)  ;; OK: [[1],[2],[3],*]
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 10, col 22) : drop throw : value contains multiple references\n") { out }
    }
    @Test
    fun scope30x_cyc() {
        val out = test("""
            val cycle = func' (v) {
                set v[3] = v
                v
            }
            var a = [1]
            var d = do {
                var b = [2]
                var c = cycle([a,b,[3],nil])
                ;;;drop;;;(c)
            }
            ;;print(d)  ;; OK: [[1],[2],[3],*]
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // SCOPE / INNER

    @Test
    fun ll_01_fleet_tuple_func() {
        val out = test("""
            val f = func' (v) {
                print(v[0])
            }
            f([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun ll_02_fleet_tuple_func() {
        val out = test("""
            val g = func' (v) {
                print(v)
            }
            val f = func' (v) {
                print(v[0])
            }
            f([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun ll_03_fleet_tuple_func_err() {
        val out = test("""
            var g = func' (v) {
                val v' = v
                nil
            }
            g([[1]])
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun ll_04_fleet_tuple_func_err() {
        val out = test("""
            val f = func' (v) {
                print(v[0])
            }
            var g = func' (v) {
                val evt = v
                f(evt)
            }
            g([[1]])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun ll_05_nest() {
        val out = test("""
            var f = func' (v) {  ;; (but they are in the same block)
                ;;dump(v)
                val x = v[0]    ;; v also holds x, both are fleeting -> unsafe
                print(x)      ;; x will be freed and v would contain dangling pointer
            }
            f([[1]])
        """)
        assert(out == "[1]\n") { out }
        //assert(out == "anon : (lin 3, col 17) : declaration throw : cannot move pending reference in\n") { out }
    }
    @Test
    fun ll_05_nest_err() {
        val out = test("""
            var f = func' (v) {
                val x = v[0]    ;; v also holds x, both are fleeting -> unsafe
                ;;print(x)      ;; x will be freed and v would contain dangling pointer
                v
            }
            print(f([[1]]))
        """)
        //assert(out == "anon : (lin 2, col 30) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[[1]]\n") { out }
    }

    @Test
    fun ll_06_xxx() {
        val out = test("""
            val g = func' (v) {
                ;;dump(v)
                print(v)
            }
            val f = func' (v) {
                ;;dump(v)
                g(v)
            }
            f([1])
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun ll_07_xxx() {
        val out = test("""
            val g = func' (v) {
                print(v)
            }
            val f = func' (v) {
                ;;dump(v)
                g(v)
                ;;dump(v)
                g(v)
            }
            f([1])
        """)
        assert(out == "[1]\n[1]\n") { out }
    }
    @Test
    fun ll_08_xxx() {
        val out = test("""
            val g = func' (v) {
                ;;dump(v)
                val k = v
                ;;dump(v)
                print(v)
                ;;dump(v)
            }
            val f = func' (v) {
                ;;dump(v)
                g(v)
                ;;dump(v)
                print(v)
            }
            f([1])
        """)
        assert(out == "[1]\n[1]\n") { out }
    }

    // REMOVED: THUS / SCOPE / :FLEET / :fleet

    /*
    @Test
    fun mm_00_err() {
        val out = test("""
            val x
            do (x) {
                nil
            }
        """
        )
        assert(out == "anon : (lin 3, col 17) : declaration throw : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun mm_01_tmp() {
        val out = test(
            """
            var x
            do [1,2,3]
            do (a) {
                set x = a
            }
            print(x)
        """
        )
        //assert(out == "[1,2,3]\n") { out }
        assert(out == "anon : (lin 5, col 21) : set throw : cannot copy reference out\n") { out }
    }
    @Test
    fun mm_01_tmp_err() {
        val out = test(
            """
            var x
            do [1,2,3]
            do (a) {
                set x = drop(a)
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
                do [1,2,3]
                do (a) {
                    a
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
            do(nil)
            do (it) {
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
            do (nil)
            do (it) {
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
            do [0]
            do (x) {
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
                do ([])
                do (x) {
                    if x { x } else { [] }
                }
            }
            print(v)
        """)
        //assert(out == "anon : (lin 3, col 20) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun mm_05_tmp_x() {
        val out = test("""
            val v = do {
                do ([])
                do (x) {
                    if x { drop(x) } else { [] }
                }
            }
            print(v)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 4, col 33) : drop throw : value is not movable\n") { out }
    }
    */

    @Test
    fun mm_06_tmp_err() {
        val out = test("""
            val v = do {
                val x = []
                if x { x } else { [] }
            }
            print(v)
        """)
        //assert(out == "anon : (lin 2, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun mm_07_and_or() {
        val out = test("""
            val t = func' () { print(:t) ; true  }
            val f = func' () { print(:f) ; false }
            print(${AND("t()", "f()")})
            print(${OR("t()", "f()")})
            print(${AND("[]", "false")})
            print(${OR("false", "[]")})
        """)
        assert(out == ":t\n:f\nfalse\n:t\ntrue\nfalse\n[]\n") { out }
    }

    // IF

    @Test
    fun if1_err() {
        val out = test(
            """
            var x
            set x = if (true) { 1 }
            print(x)
        """
        )
        assert(out == "anon : (lin 4, col 13) : expected \"else\" : have \"print\"\n") { out }
        //assert(out == "1\n") { out }
    }
    @Test
    fun if2_err() {
        val out = test(
            """
            var x
            set x = 10
            set x = if false { 1 }
            print(x)
        """
        )
        assert(out == "anon : (lin 5, col 13) : expected \"else\" : have \"print\"\n") { out }
        //assert(out == "nil\n") { out }
    }
    @Test
    fun if3_err_ok() {
        val out = test(
            """
            var x
            set x = 10
            set x = if (nil) {nil} else { 1 }
            print(x)
        """.trimIndent()
        )
        //assert(out == "anon : (lin 3, col 13) : if throw : invalid condition\n") { out }
        //assert(out == "anon : (lin 3, col 19) : expected expression : have \"}\"\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun if3x_err_ok() {
        val out = test(
            """
            var x
            set x = 10
            set x = if (false) {1} else {nil}
            print(x)
        """.trimIndent()
        )
        //assert(out == "anon : (lin 3, col 13) : if throw : invalid condition\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun if4_err() {
        val out = test(
            """
            print(if [] {nil} else {nil})
        """.trimIndent()
        )
        //assert(out == "anon : (lin 1, col 4) : if throw : invalid condition\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun if5_hld() {
        val out = test(
            """
            if [] {nil} else {nil}
            print(1)
        """.trimIndent()
        )
        //assert(out == "anon : (lin 1, col 4) : if throw : invalid condition\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun if6_else() {
        val out = test("""
            val v = [1]
            print(:v,v,{{#}}(v),v[0])
            val x = {{/=}}(v[0],'\\')
        """)
        //assert(out == "anon : (lin 1, col 4) : if throw : invalid condition\n") { out }
        assert(out == ":v\t[1]\t1\t1\n") { out }
    }

    // FUNC / CALL

    @Test
    fun oo_01_func() {
        val out = test("""
            val f = func' (v) { v }
            val x = f(10)
            print(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oo_01x_func() {
        val out = test("""
            val f = func' () {
                print(:ok)
            }
            f()
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun oo_02_func0_err() {
        val out = test(
            """
            val x
            val f = func' (x) { nil }
            print(:no)
        """
        )
        //assert(out == "anon : (lin 3, col 27) : declaration throw : variable \"x\" is already declared\n") { out }
        assert(out == ":no\n") { out }
    }
    @Test
    fun func1() {
        val out = test(
            """
            val f = func' () { nil }
            val x = f()
            print(x)
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun func2() {
        val out = test(
            """
            var f
            set f = func' () {
                1
            }
            var x
            set x = f()
            print(x)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun func3() {
        val out = test(
            """
            var f
            set f = func' (xxx) {
                ;;print(xxx)
                xxx
            }
            var yyy
            set yyy = f(10)
            print(yyy)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun func4() {
        val out = test(
            """
            val f = func' (x) {
                x
            }
            val x = f()
            print(x)
        """
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun func5_err() {
        val out = test(
            """
            var f
            set f = func' (x) {
                [x]
            }
            var x
            set x = f(10)
            print(x)
        """
        )
        assert(out == "[10]\n") { out }
        //assert(out.contains("set throw : incompatible scopes")) { out }
    }
    @Test
    fun func7_err() {
        val out = test("1(1)")
        assert(out == " |  anon : (lin 1, col 1) : 1(1)\n" +
                " v  throw : expected function\n") { out }
    }
    @Test
    fun func8() {
        val out = test(
            """
            ;;;do;;; 1
            ;;;do;;; (1)
        """
        )
        //assert(out == "anon : (lin 2, col 2) : call throw : \"(\" in the next line") { out }
        assert(out == "") { out }
    }
    @Test
    fun func9() {
        val out = test(
            """
            var f
            set f = func' (a,b) {
                [a,b]
            }
            print(f())
            print(f(1))
            print(f(1,2))
            print(f(1,2,3))
        """
        )
        assert(out == "[nil,nil]\n[1,nil]\n[1,2]\n[1,2]\n") { out }
    }
    @Test
    fun func10_err() {
        val out = test(
            """
            f()
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : access throw : variable \"f\" is not declared\n") { out }
    }
    @Test
    fun func11() {
        val out = test(
            """
            print(func' (x) {
                var fff
                set fff = func' (xxx) {
                    print(type(xxx))
                    xxx
                }
                fff(x)
            } (10))
        """
        )
        assert(out == ":number\n10\n") { out }
    }
    @Test
    fun func12() {
        val out = test(
            """
            val fff = func' (xxx) {
                print(type(xxx))
                xxx
            }
            print(func' () {
                fff(10)
            } ())
        """
        )
        assert(out == ":number\n10\n") { out }
    }
    @Test
    fun func13() {
        val out = test(
            """
            func' () {
                var fff
                set fff = func' () {
                    print(1)
                }
                fff()
            } ()
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun func14() {
        val out = test(
            """
            print(func' (x) {
                var fff
                set fff = func' (xxx) {
                    print(type(xxx))
                    xxx
                }
                fff(x)
            } (10))
        """
        )
        assert(out == ":number\n10\n") { out }
    }
    @Test
    fun func15() {
        val out = test(
            """
            func' (xxx) {
                print(xxx)
                func' () {
                    print(xxx)
                }()
            }(10)
        """
        )
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun func16_print() {
        val out = test(
            """
            var f
            set f = print
            f(false)
        """
        )
        assert(out == "false\n") { out }
    }
    @Test
    fun func17_ff() {
        val out = test(
            """
            var f
            f()()
        """
        )
        assert(out == " |  anon : (lin 3, col 13) : f()\n" +
                " v  throw : expected function\n") { out }
    }
    @Test
    fun func18_rec() {
        val out = test(
            """
            val f = func' () {
                f()
            }
            print(f)
        """
        )
        assert(out.contains("func: 0x")) { out }
    }
    @Test
    fun nn_19_func_out() {
        val out = test("""
        val f = func' (x) {
            x()
        }
        val F = func' () {
            []
        }
        do {
            val l = f(F)
            print(l)
            nil
        }
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun nn_20_func_err() {
        DEBUG = true
        val out = test("""
            val f = func' (v) {
                ;;dump(v)
                val x = v
                nil
            }
            print(f([[nil]][0]))  ;; err
            ;;`ceu_gc_collect();`
        """)
        //assert(out == "anon : (lin 2, col 27) : argument throw : cannot receive pending reference\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun nn_20x_func_err() {
        DEBUG = true
        val out = test("""
            val f = func' (v) {
                ;;dump(v)
                val x = v
                nil
            }
            print(f([[nil]][0]))  ;; err
            ;;`ceu_gc_collect();`
        """)
        //assert(out == "anon : (lin 2, col 27) : argument throw : cannot receive pending reference\n") { out }
        assert(out == "nil\n") { out }
    }
    @Test
    fun nn_21_func() {
        val out = test("""
            val f = func' (v) {
                1
            }
            val t = [[nil]]
            print(f(t[0]))        ;; 1
            print(f([[nil]][0]))  ;; err
        """)
        //assert(out == "anon : (lin 2, col 27) : argument throw : cannot receive pending reference\n1\n") { out }
        assert(out == "1\n1\n") { out }
    }
    @Test
    fun nn_22_func_block() {
        val out = test("""
            val f = func' (v) {
                do {
                    val x = nil
                }
            }
            f(nil)
            print(:ok)
        """)
        //assert(out == "anon : (lin 4, col 26) : block escape throw : cannot copy reference out\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun nn_23_pipe() {
        val out = test("""
            $PLUS
            val f = func' (v) { -v }
            print(f(10))
        """)
        assert(out == "-10\n") { out }
    }
    @Test
    fun nn_24_func_args() {
        val out = test(
            """
            val f = func' (a,b) {
                b
            }
            print(f(1,2))
        """
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun nn_25_func_dup() {
        val out = test(
            """
            do {
                val f = func' () {nil}
                f()
            }
            do {
                val f = func' () {nil}
                f()
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    // NATIVE

    @Test
    fun native1() {
        val out = test(
            """
            ```
                printf("xxx\n");
            ```
        """
        )
        assert(out == "xxx\n") { out }
    }
    @Test
    fun native2_num() {
        val out = test(
            """
            var x
            set x = `:number 1.5`
            print(x)
            print(`:number 1.5`)
        """
        )
        assert(out == "1.5\n1.5\n") { out }
    }
    @Test
    fun native3_str() {
        val out = test(
            """
            var x
            set x = `:pointer "ola"`
            ```
                puts(${D}x.Pointer);
            ```
        """
        )
        assert(out == "ola\n") { out }
    }
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
    fun native5() {
        val out = test(
            """
            var x
            set x = 10
            set x =  ```:number
                (printf(">>> %g\n", ${D}x.Number),
                ${D}x.Number*2)
            ```
            print(x)
        """
        )
        assert(out == ">>> 10\n20\n") { out }
    }
    @Test
    fun TODO_native6() {    // cannot write C -> Ceu
        val out = test(
            """
            var x
            set x = 1
            ```
                ${D}x.Number = 20;
            ```
            print(x)
        """
        )
        assert(out == "20\n") { out }
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
    fun native9_err() {
        val out = test(
            """
             `
                $D 
             `
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : native throw : (lin 2, col 4) : invalid identifier\n") { out }
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
    fun native11_pointer() {
        val out = test(
            """
            var f
            set f = func' () {
                `:pointer
                    "ola"
                `
            }
            var g
            set g = func' (x) {
                `
                    printf("%s\n", (char*)${D}x.Pointer);
                `
            }
            var x
            set x = f()
            g(x)
        """
        )
        assert(out == "ola\n") { out }
    }
    @Test
    fun native12_pointer() {
        val out = test(
            """
            print(`:pointer"oi"`)
        """
        )
        assert(out.contains("pointer: 0x")) { out }
    }
    @Test
    fun TODO_native13_pre_visible() {
        val out = test(
            """
            ```
            int Z = 1;          // should it be visible...
            ```
            var f
            set f = func' () {
                `:number Z`     ;; ...here?
            }
            print(f())
        """
        )
        //assert(out == "1\n") { out }
        assert(out.contains("throw: ‘Z’ undeclared")) { out }
    }
    @Test
    fun native14_char() {
        val out = test(
            """
            var c
            set c = `:char 'x'`
            `putchar(${D}c.Char);`
        """
        )
        assert(out == "x") { out }
    }
    @Test
    fun native15_func() {
        val out = test(
            """
            var f = func' (v) {
                print(v)
                v
            }
            ;;;do;;; f
            var f' = `:ceu ${D}f`
            print(f'(10))
        """
        )
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun native16_func() {
        val out = test("""
            val n = `:number 10`                ;; native 10 is converted to `dyn-lex` number
            val x = `:ceu ${D}n`                ;; `x` is set to `dyn-lex` `n` as is
            `printf("> %f\n", ${D}n.Number);`   ;; outputs `n` as a number
        """)
        assert(out == "> 10.000000\n") { out }
    }
    @Test
    fun BUG_native_XXX() {
        val out = test(
            """
            var x
            set x = 10
            set x =  `:number ${D}x.Number /*XXX*/`
            print(x)
        """
        )
        assert(out == "10\n") { out }
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
    @Test
    fun TODO_on_20_nat_error() {
        val out = test("""
            `CEU_ERROR_CHK_PTR(continue, "C throw");`
        """)
        assert(out == " |  anon : (lin 2, col 13) : ```ceu_error_s(ceux->S, .C throw.);```\n" +
                " v  throw : C throw\n") { out }
    }

    // OPERATORS

    @Test
    fun op_umn0() {
        val out = test(
            """
            val f = func' (v1, v2) {
                print(v1,v2)
            }
            f(10)
        """
        )
        assert(out == "10\tnil\n") { out }
    }
    @Test
    fun op_umn() {
        val out = test(
            """
            print(-10)
        """, true
        )
        assert(out == "-10\n") { out }
    }
    @Test
    fun op_id1() {
        val out = test(
            """
            print({{-}}(10,4))
        """, true
        )
        assert(out == "6\n") { out }
    }
    @Test
    fun op_arithX() {
        val out = test("""
            print(((10 + -20)*2)/5)
        """, true
        )
        assert(out == "-4\n") { out }
    }
    @Test
    fun op_cmp() {
        val out = test(
            """
            print(1 > 2)
            print(1 < 2)
            print(1 == 1)
            print(1 /= 1)
            print(2 >= 1)
            print(2 <= 1)
        """, true
        )
        assert(out == "false\ntrue\ntrue\nfalse\ntrue\nfalse\n") { out }
    }
    @Test
    fun op_eq() {
        val out = test(
            """
            print(1 == 1)
            print(1 /= 1)
        """
        )
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun op_prec_ok() {
        val out = test(
            """
            print(2 + 3 + 1)
        """, true
        )
        assert(out == "6\n") { out }
    }
    @Test
    fun op_assoc() {
        val out = test(
            """
            print((2 * 3) - 1)
        """, true
        )
        assert(out == "5\n") { out }
    }
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
    fun ops_id() {
        val out = test(
            """
            val add = func' (x,y) {
                x + y
            }
            print(10 {{add}} 20)
        """, true
        )
        assert(out == "30\n") { out }
    }
    @Test
    fun oo_xx_op_set() {
        val out = test("""
            val {{-}} = 10
            val {{+}} = {{-}}
            print({{+}})
        """)
        assert(out == "10\n") { out }
    }
    @Test

    // ==, ===, /=, =/=

    @Test
    fun pp_01_op_eqeq_tup() {
        val out = test(
            """
            print([1] == [1])
            print([ ] == [1])
            print([1] /= [1])
            print([1,[],[1,2,3]] == [1,[],[1,2,3]])
        """
        )
        assert(out == "false\nfalse\ntrue\nfalse\n") { out }
    }
    @Test
    fun pp_02_op_eqeq_tup() {
        val out = test(
            """
            print([1,[1],1] == [1,[1],1])
        """
        )
        assert(out == "false\n") { out }
    }
    @Test
    fun pp_03_op_eqs_dic() {
        val out = test(
            """
            print(@[] == @[])
            print(@[] /= @[])
        """
        )
        assert(out == "false\ntrue\n") { out }
    }
    @Test
    fun pp_04_op_eqs_vec() {
        val out = test(
            """
            print(#[] ==  #[])
            print(#[] /=  #[])
        """
        )
        assert(out == "false\ntrue\n") { out }
    }
    @Test
    fun pp_05_op_eqs_vec_dic_tup() {
        val out = test(
            """
            print([#[],@[]] == [#[],@[]])
            print([#[],@[]] /= [#[],@[]])
        """
        )
        assert(out == "false\ntrue\n") { out }
    }

    // to-number, to-string, to-tag, to-tag-string, to-pointer

    @Test
    fun qq_01_tostring() {
        val out = test("""
            `static char x[] = "abc";`
            print(to-string(`:pointer x`))
        """, true
        )
        assert(out == "abc\n") { out }
    }
    @Test
    fun tostring1() {
        val out = test(
            """
            var s
            set s = to-string(1234)
            print(type(s), s)
        """, true
        )
        assert(out == ":vector\t1234\n") { out }
    }
    @Test
    fun tonumber2() {
        val out = test(
            """
            var n
            set n = to-number(#['1','0'])
            print(type(n), n)
        """, true
        )
        assert(out == ":number\t10\n") { out }
    }
    @Test
    fun tonumber_tostring3() {
        val out = test(
            """
            var s
            set s = to-string(to-number(#['1','0']))
            print(type(s), s)
        """, true
        )
        assert(out == ":vector\t10\n") { out }
    }
    @Test
    fun ff_01_string_to_tag() {
        val out = test(
            """
            ;;;do;;; :xyz
            print(to-tag-string(#[':','x']))
            print(to-tag(#[':','x','y','z']))
            print(to-tag-string(#['x','y','z']))
            print(to-tag(:abc))
        """, true
        )
        assert(out == "nil\n:xyz\nnil\n:abc\n") { out }
    }
    @Test
    fun ff_02_string_to_tag() {
        val out = test(
            """
            data :A = []
            data :A.B = []
            data :A.B.C = []
            print(to-tag-string(#[':','A']), to-tag-string(#[':','A','.','B']), to-tag-string(#[':','A','.','B','.','C']))
        """
        )
        assert(out == ":A\t:A.B\t:A.B.C\n") { out }
    }
    @Test
    fun ff_03_string_to_tag() {
        val out = test(
            """
            val x = to-tag-string(#[':','x'])
            print(x == :x)
            val y = to-tag-string(#[':','y'])
            print(y)
        """
        )
        assert(out == "true\nnil\n") { out }
    }
    @Test
    fun ff_04_tostring_pointer() {
        val out = test("""
            val ptr = `:pointer "abc"`
            val str = to-string-pointer(ptr)
            print(str)
        """)
        assert(out == "abc\n") { out }
    }
    @Test
    fun ff_05_tostring_number() {
        val out = test("""
            val str = to-string-number(10)
            print(str)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ff_06_string_to_tag() {
        val out = test("""
            ;;;do;;; :xyz
            print(to-tag-string(":x"))
            print(to-tag-string(":xyz"))
            print(to-tag-string("xyz"))
        """)
        assert(out == "nil\n:xyz\nnil\n") { out }
    }
    @Test
    fun ff_07_string_to_tag() {
        val out = test("""
            data :A = []
            data :A.B = []
            data :A.B.C = []
            print(to-tag-string(":A"), to-tag-string(":A.B"), to-tag-string(":A.B.C"))
        """)
        assert(out == ":A\t:A.B\t:A.B.C\n") { out }
    }
    @Test
    fun ff_08_string_to_pointer() {
        val out = test("""
            val s = #['a','l','o']
            val p = to-pointer(s)
            `printf(">>> %s\n", (char*) ${D}p.Pointer);`
        """, true)
        assert(out == ">>> alo\n") { out }
    }
    @Test
    fun ff_09_tag_to_pointer() {
        val out = test("""
            val p = to-pointer(:ola)
            `printf(">>> %s\n", (char*) ${D}p.Pointer);`
        """, true)
        assert(out == ">>> :ola\n") { out }
    }

    // TYPE

    @Test
    fun type1() {
        val out = test(
            """
            var t
            set t = type(1)
            print(t)
            print(type(t))
            print(type(type(t)))
            print(type(:x))
        """
        )
        assert(out == ":number\n:tag\n:tag\n:tag\n") { out }
    }

    // TAGS

    @Test
    fun gg_01_tags() {
        val out = test(
            """
            print(:xxx)
            print(:xxx == :yyy)
            print(:xxx /= :yyy)
            print(:xxx == :xxx)
            print(:xxx /= :xxx)
        """
        )
        assert(out == ":xxx\nfalse\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun gg_02_tags() {
        val out = test(
            """
            func' () {
                print(:xxx)
            }()
            func' () {
                print(:xxx)
            }()
        """
        )
        assert(out == ":xxx\n:xxx\n") { out }
    }
    @Test
    fun gg_03_tags() {
        val out = test(
            """
            func' () {
                print(:Xxx.Yyy)
            }()
            func' () {
                print(:a.b.c)
            }()
        """
        )
        assert(out == "anon : (lin 3, col 25) : tag throw : parent tag :Xxx is not declared\n") { out }
    }
    @Test
    fun gg_04_tags() {
        val out = test(
            """
            func' () {
                print(:Xxx)
            }()
            func' () {
                print(:1)
            }()
        """
        )
        assert(out == ":Xxx\n:1\n") { out }
    }
    @Test
    fun TODO_gg_05_tags_err() {
        val out = test(
            """
            print(tag())
        """
        )
        assert(out.contains("ceu_tag_f: Assertion `ceux->args==1 || ceux->args==2' failed")) { out }
    }
    @Test
    fun gg_06_tags() {
        val out = test(
            """
            print(tag([]))
        """
        )
        assert(out.contains("nil\n")) { out }
        //assert(out.contains("[]\n")) { out }
    }
    @Test
    fun gg_07_tags() {
        val out = test("""
            print(tag(:2,1))   ;; throw message
        """)
        //assert(out == "false\n") { out }
        //assert(out == "1\n") { out }
        assert(out.contains("ceu_tag_set: Assertion `dyn.type >= CEU_VALUE_DYNAMIC' failed.")) { out }
    }
    @Test
    fun gg_08_tags_err() {
        val out = test("""
            print(tag(tag(2,[])))
        """)
        //assert(out == "2\n") { out }
        //assert(out.contains("Assertion `tag.type == CEU_VALUE_TAG'")) { out }
        assert(out.contains("ceu_tag_set: Assertion `tag.type==CEU_VALUE_NIL || tag.type==CEU_VALUE_TAG' failed.")) { out }
    }
    @Test
    fun gg_09_tags_err() {
        val out = test("""
            print(tag(:x,[]))
            print(tag(1,[]))
        """)
        //assert(out == ":x []\n1 []\n") { out }
        //assert(out.contains("Assertion `bool.type == CEU_VALUE_BOOL' failed")) { out }
        assert(out.contains("ceu_tag_set: Assertion `tag.type==CEU_VALUE_NIL || tag.type==CEU_VALUE_TAG' failed.")) { out }
    }
    @Test
    fun gg_10_tags() {
        val out = test(
            """
            var t
            set t = []
            var x1
            set x1 = tag(:x,t)
            var x2
            set x2 = tag(:x,t)
            print(x1, x2, x1==t)
            set x1 = tag(nil,t)
            set x2 = tag(nil,t)
            print(x1, x2, x1==t)
        """
        )
        assert(out == ":x []\t:x []\ttrue\n[]\t[]\ttrue\n") { out }
    }
    @Test
    fun gg_11_tags() {
        val out = test(
            """
            val t = []
            val x1 = tag(:x,t)
            val x2 = tag(:x,t)
            print(x1, x2, x1==t, x2==t)
        """
        )
        assert(out == ":x []\t:x []\ttrue\ttrue\n") { out }
    }
    @Test
    fun gg_12_tags() {
        val out = test(
            """
            var t
            set t = []
            tag(:x,t)
            print(tag(t) == :x)
            tag(:y,t)
            print(tag(t) == :y)
            tag(nil,t)
            print(tag(t) == :y)
        """
        )
        assert(out == "true\ntrue\nfalse\n") { out }
    }
    @Test
    fun gg_13_tags() {
        val out = test(
            """
            print(:x-a-x, :i.j.a)
        """
        )
        assert(out == "anon : (lin 2, col 29) : tag throw : parent tag :i.j is not declared\n") { out }
    }
    @Test
    fun gg_14_tags() {
        val out = test(
            """
            print(:x-a-x, :i-j-a)
        """
        )
        assert(out == ":x-a-x\t:i-j-a\n") { out }
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
    @Test
    fun gg_17_tags() {
        DEBUG = true
        val out = test(
            """
            var t = []
            tag(:X, t)
            tag(:Y, t)
            tag(:Z, t)
            ;;print(tag(t))
            var f = func' (ts) {
                print(ts)
            }
            f(tag(t))
            print(`:number CEU_GC.free`)
        """
        )
        //assert(out == "[:Z,:Y,:X]\n2\n") { out }
        assert(out == ":Z\n0\n") { out }
    }
    @Test
    fun gg_18_tags() {
        val out = test("""
            var t = []
            print(tag(:X, t))
        """)
        assert(out == ":X []\n") { out }
    }

    // TAGS / OPERATIONS

    @Test
    fun gh_01_tags() {
        val out = test("""
            :x :y
            print(:y - 1)
            print(1 + :x)
            print(:x <= :y)
            print(:x > :y)
            print(:y - :x)
            print(:x + :y)
        """, true)
        assert(out == " |  build/prelude-0.ceu : (lin 61, col 17) : throw(:throw)\n" +
                " v  throw : :throw\n" +
                ":x\n" +
                ":y\n" +
                "true\n" +
                "false\n" +
                "1\n") { out }
    }

    // CLOSURE / ESCAPE / FUNC / UPVALS

    @Test
    fun clo3_err() {
        val out = test(
            """
            x     ;; upvar can't be in global
        """
        )
        assert(out == "anon : (lin 2, col 13) : access throw : variable \"x\" is not declared\n") { out }
    }
    @Test
    fun clo4_err() {
        val out = test(
            """
            x     ;; upref can't be in global
        """
        )
        assert(out == "anon : (lin 2, col 13) : access throw : variable \"x\" is not declared\n") { out }
    }
    @Test
    fun clo5_err() {
        val out = test(
            """
            val g = 10
            var f
            set f = func' (x) {
                set x = []  ;; err: cannot reassign
                func' () {
                    x == g
                }
            }
            print(f([])())
        """
        )
        //assert(out == "anon : (lin 6, col 21) : set throw : cannot reassign an upval") { out }
        assert(out == "anon : (lin 5, col 17) : set throw : destination is immutable\n") { out }
    }
    @Test
    fun clo6_err() {
        val out = test(
            """
            var g
            set g = 10
            var f
            set f = func' (x) {
                func' () {
                    set x = []  ;; err: cannot reassign
                    ;;x + g
                }
            }
            print(f([])())
        """
        )
        //assert(out == "anon : (lin 7, col 25) : set throw : cannot reassign an upval") { out }
        assert(out == "anon : (lin 7, col 21) : set throw : destination is immutable\n") { out }
    }
    @Test
    fun clo8_err() {
        val out = test(
            """
            do {
                x     ;; err: no associated upvar
            }
        """
        )
        assert(out == "anon : (lin 3, col 17) : access throw : variable \"x\" is not declared\n") { out }
    }
    @Test
    fun clo11() {
        val out = test("""
            val f = do {
                val x = []
                ;;print(x)
                func' () {   ;; block_set(1)
                    x       ;; because of x
                }           ;; err: scope on return
            }
            print(f())
        """
        )
        //assert(out == "anon : (lin 3, col 21) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 3, col 21) : block escape throw : reference has immutable scope\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun clo11a_err() {
        val out = test("""
            val f = do {
                var x = []
                ;;print(x)
                func' () {   ;; block_set(1)
                    x       ;; because of x
                }           ;; err: scope on return
            }
            print(f())
        """
        )
        //assert(out == "anon : (lin 3, col 21) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 3, col 21) : block escape throw : reference has immutable scope\n") { out }
        assert(out == "anon : (lin 6, col 21) : access throw : outer variable \"x\" must be immutable\n") { out }
    }
    @Test
    fun clo11b_err() {
        val out = test("""
            val f = do {
                var x = []
                ;;print(x)
                func' () {   ;; block_set(1)
                    set x = nil
                }
            }
            print(f())
        """
        )
        //assert(out == "anon : (lin 3, col 21) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 3, col 21) : block escape throw : reference has immutable scope\n") { out }
        assert(out == "anon : (lin 6, col 25) : access throw : outer variable \"x\" must be immutable\n") { out }
    }
    @Test
    fun clo12_err() {
        val out = test(
            """
            var f
            set f = func' (x) {
                func' () {   ;; block_set(1)
                    x       ;; because of x
                }           ;; err: scope on return
            }
            print(f(10)())
        """
        )
        //assert(out == "anon : (lin 3, col 30) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == "anon : (lin 3, col 30) : block escape throw : reference has immutable scope\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun clo13_err() {
        val out = test(
            """
            val g = 10
            var f
            set f = func' (x) {
                func' () {       ;; block_set(0)
                    x + g     ;; all (non-global) upvals are marked
                }
            }
            print(f(20)())
        """, true
        )
        assert(out == "30\n") { out }
    }
    @Test
    fun clo14() {
        val out = test(
            """
            val g = 10
            var f
            set f = func' (x) {
                func' () {
                    x[0] + g
                }
            }
            print(f([20])())
        """, true
        )
        assert(out == "30\n") { out }
    }
    @Test
    fun clo15() {
        val out = test(
            """
            var f
            set f = func' (x) {
                ;;;val :fleet z =;;; func' (y) {
                    [x,y]
                }
                ;;dump(z)
                ;;z
            }
            print(f([10])(20))
        """
        )
        assert(out == "[[10],20]\n") { out }
    }
    @Test
    fun clo16() {
        val out = test(
            """
            var f
            set f = func' () {
                val x = 10     ;; TODO: needs initialization
                func' (y) {
                    [x,y]
                }
            }
            print(f()(20))
        """
        )
        assert(out == "[10,20]\n") { out }
    }
    @Test
    fun clo17() {
        val out = test(
            """
            var f
            set f = func' (x) {
                print(:1, x)
                func' () {
                    print(:2, x)
                    x
                }
            }
            print(:3, f(10)())
        """
        )
        assert(out == ":1\t10\n:2\t10\n:3\t10\n") { out }
    }
    @Test
    fun clo18() {
        val out = test(
            """
            var f
            set f = func' (x) {
                func' () {
                    x
                }
            }
            print(f(10)())
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun clo19() {
        val out = test(
            """
            val curry = func' (fff) {
                ;;print(:1, fff)
                func' (xxx) {
                    ;;print(:2, fff, xxx)
                    func' (yyy) {
                        ;;print(:3, fff, xxx, yyy)
                        fff(xxx,yyy)
                    }
                }
            }

            val f = func' (a,b) {
                [a,b]
            }
            val f'  = curry(f)
            val f'' = f'(1)
            print(f''(2))
        """
        )
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun clo20() {
        val out = test(
            """
            var curry
            set curry = func' (fff) {
                func' (xxx) {
                    func' (yyy) {
                        fff(xxx,yyy)
                    }
                }
            }
            var f = func' (a,b) {
                [a,b]
            }
            print(curry(f)(1)(2))
        """
        )
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun clo21_err() {
        val out = test(
            """
            var f = func' (a) {
                func' () {
                    a
                }
            }
            var g = do {
                val t = [1]
                f(t)
            }
            print(g())
        """
        )
        //assert(out == "anon : (lin 7, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[1]\n") { out }
    }
    @Test
    fun tup22_err() {
        val out = test(
            """
            do {
                val t = []
                ;;;do;;; [t]
            }
            print(:ok)
        """
        )
        //assert(out == "anon : (lin 2, col 13) : block escape throw : cannot copy reference out\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun clo23_err() {
        val out = test(
            """
            var f = func' (a) {
                func' () {
                    a
                }
            }
            var g = do {
                var t = [1]
                ;;;drop;;;(f(t))
            }
            print(g())
        """
        )
        //assert(out == "anon : (lin 7, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[1]\n") { out }
    }
    @Test
    fun clo23x_err() {
        val out = test(
            """
            var f = func' (v) {
                [v]
            }
            var g = do {
                var t = [1]
                ;;;drop;;;(f(t))
            }
            print(g)
        """
        )
        //assert(out == "anon : (lin 7, col 21) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[[1]]\n") { out }
    }
    @Test
    fun clo23x() {
        val out = test(
            """
            var f = func' (a) {
                func' () {
                    a
                }
            }
            var g = do {
                var t = [1]
                ;;;drop;;;(f(;;;drop;;;(t)))
            }
            print(g())
        """
        )
        assert(out == "[1]\n") { out }
    }
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
    @Test
    fun clo27_escape_err() {
        val out = test(
            """
            val x = 10
            val f = func' (y) {
                val g = func' () {
                    y
                }
                ;;;drop;;;(g)
            }
            print(f(1)())
            """,
        )
        //assert(out == "anon : (lin 7, col 22) : drop throw : value is not movable\n") { out }
        assert(out == "1\n") { out }
    }
    @Test
    fun pp_28_clo_print() {
        val out = test("""
            val f = func' (x) {
                func' () {
                    x[0]
                }
            }
            print(f([20]))
        """)
        assert(out.contains("func: 0x")) { out }
        assert(out.contains(" | [[20]]")) { out }
    }
    @Test
    fun pp_29_func_escape() {
        val out = test(
            """
            val f = func' () {
                func' () {
                    print(:ok)
                }
            }
            f()()
        """
        )
        assert(out == ":ok\n") { out }
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
