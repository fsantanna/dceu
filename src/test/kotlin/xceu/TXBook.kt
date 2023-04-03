package xceu

import org.junit.Ignore
import org.junit.Test

class TXBook {

    // CHAPTER 1: Fundamental concepts

    // CHAPTER 1.1: Session and scripts

    @Test
    fun pg_2_square() {
        val out = ceu.all("""
            val square = func (x) {
                x * x
            }
            println(square(5))
        """, true)
        assert(out == "25\n") { out }
    }
    @Test
    fun pg_2_smaller() {
        val out = ceu.all("""
            val smaller = func (x,y) {
                if x < y { x } else { y }
            }
            println(smaller(1,2))
            println(smaller(1,1))
            println(smaller(2,1))
        """, true)
        assert(out == "1\n1\n1\n") { out }
    }
    @Test
    fun pg_3_square_smaller() {
        val out = ceu.all("""
            func square (x) {
                x * x
            }
            func smaller (x,y) {
                ((x < y) and x) or y
            }
            println(square(smaller(3,5)))
        """, true)
        assert(out == "9\n") { out }
    }
    @Test
    fun pg_3_square_smaller_a() {
        val out = ceu.all("""
            func square (x) {
                x * x
            }
            func smaller (x,y) {
                if (x < y) -> x -> y
            }
            println(square(smaller(3,5)))
        """, true)
        assert(out == "9\n") { out }
    }
    @Test
    fun pg_3_delta() {
        val out = ceu.all("""
            func delta (a,b,c) {
                ((b**2) - (4*(a*c))) // 2
            }
            println(delta(4.2,7,2.3))
        """, true)
        assert(out == "3.2187\n") { out }
    }

    // CHAPTER 1.2: Evaluation
    // ok

    // CHAPTER 1.3: Values
    // ok

    // CHAPTER 1.4: Functions

    @Test
    fun pg_11_currying() {
        val out = ceu.all("""
            func smallerc (^x) {
                func (y) {
                    if ^^x < y { ^^x } else { y }
                }
            }
            func plusc (^x) {
                func (y) {
                    ^^x+y
                }
            }
            println(smallerc(3)(5))
            println(plusc(3)(5))
        """, true)
        assert(out == "3\n8\n") { out }
    }
    @Test
    fun pg_12_twice_quad() {
        val out = ceu.all("""
            func square (x) {
                x**2
            }
            func twice (f,x) {
                f(f(x))
            }
            func quad (x) {
                twice(square,x)
            }
            println(twice(square, 2), quad(2))
        """, true)
        assert(out == "16\t16\n") { out }
    }
    @Test
    fun pg_12_twicec() {
        val out = ceu.all("""
            func square (x) {
                x**2
            }
            func twicec (^f) {
                func (v) {
                    ^^f(^^f(v))
                }
            }
            val quad = twicec(square)
            println(quad(2))""", true
        )
        assert(out == "16\n") { out }
    }
    @Test
    fun pg_13_curry() {
        val out = ceu.all("""
            func curry (^f) {
                func (^x) {
                    func (y) {
                        ^^f(^^x,y)
                    }
                }
            }
            val plusc = curry({+})
            val b = plusc(1)
            val c = b(2)
            println((plusc(1))(-4))
        """, true)
        assert(out == "-3\n") { out }
    }
    @Test
    fun pg_13_uncurry() {
        val out = ceu.all("""
            func plusc (^x) {
                func (y) {
                    ^^x + y
                }
            }
            func uncurry (^f) {
                func (x,y) {
                    ^^f(x)(y)
                }            
            }
            println(uncurry(plusc)(1,-4))
        """, true)
        assert(out == "-3\n") { out }
    }
    @Test
    fun pg_13_ops() {
        val out = ceu.all("""
            println({*}(1 + 3, 4))
        """, true)
        assert(out == "16\n") { out }
    }
    @Test
    fun pg_15_compose() {
        val out = ceu.all("""
            func compose (^f,^g) {
                func (v) {
                    ^^f(^^g(v))
                }
            }
            func square (x) {
                x**2
            }
            val quad = compose(square,square)
            println(quad(2))
        """, true)
        assert(out == "16\n") { out }
    }

    // CHAPTER 1.5: Definitions

    @Test
    fun todo_ifs_pg_18_signum() {
        val out = ceu.all("""
            func signum (x) {
                ifs {
                    x < 0  -> -1
                    x > 0  ->  1
                    else -> 0
                }
            }
            println(signum(10), signum(-9), signum(0))
        """, true)
        assert(out == "1\t-1\t0\n") { out }
    }
    @Test
    fun pg_19_fact() {
        val out = ceu.all("""
            func fact (x) {
                if x == 0 {
                    1
                } else {
                    x * fact(x - 1)
                }
            }
            println(fact(5))
        """, true)
        assert(out == "120\n") { out }
    }

    // CHAPTER 1.6: Types

    @Test // 1.6.2: Type classes
    @Ignore
    fun todo_poly_mult() {
        val out = ceu.all("""
            poly val mult
            println(10 {mult} 20)
            println([1,2] {mult} 2)
            println(2 {mult} [1,2])
            func fact (x) {
                ifs {
                    x < 0  -> throw(:error)
                    x == 0 -> 1
                    else -> x * fact(x - 1)
                }
            }
            println(fact(-1))
        """, true)
        assert(out == "anon : (lin 9, col 21) : fact({-}(1))\n" +
                "anon : (lin 4, col 31) : throw(:error)\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }

    // CHAPTER 1.7: Specifications
    // ok

    // CHAPTER 2: Simple datatypes

    // CHAPTER 2.1: Booleans

    @Test
    fun todo_ifs_pg_30_bool() {
        val out = ceu.all("""
            func fact (x) {
                ifs {
                    x < 0  -> throw(:error)
                    x == 0 -> 1
                    else -> x * fact(x - 1)
                }
            }
            println(fact(-1))
        """, true)
        assert(out == "anon : (lin 9, col 21) : fact({-}(1))\n" +
                "anon : (lin 4, col 31) : throw(:error)\n" +
                "throw error : uncaught exception\n" +
                ":error\n") { out }
    }
    @Test
    fun pg_31_short() {
        val out = ceu.all("""
            println((false and throw(:error)) or true)
            println(true or throw(:error))
        """, true)
        assert(out == "true\ntrue\n") { out }
    }
    @Test
    @Ignore // class Eq:  ===  =/=  (polymorphic, unlike ~= ~/=)
    fun todo_pg_32_eq_poly() {
    }
    @Test
    @Ignore // class Ord:  ===  =/=  (polymorphic, unlike ~= ~/=)
    fun todo_pg_32_ord_poly() {
    }
    @Test
    fun pg_33_leap1() {
        val out = ceu.all("""
            func leapyear? (y) {
                if (y % 100) == 0 {
                    (y % 400) == 0
                } else {
                    (y % 4) == 0
                }
            }
            println(leapyear?(1980))
            println(leapyear?(1979))
        """, true)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun pg_33_leap2() {
        val out = ceu.all("""
            func leapyear? (y) {
                ((y % 100) == 0) and ((y % 400) == 0) or ((y % 4) == 0)
            }
            println(leapyear?(1980))
            println(leapyear?(1979))
        """, true)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun pg_34_tri() { // :Triangle.Equilateral
        val out = ceu.all("""
            data :Tri = [] {
                :Equ = []
                :Iso = []
                :Sca = []
                :Err = []
            }
            func analyse (x,y,z) {
                ifs {
                    (x+y) <= z -> :Tri.Err
                    x == z     -> :Tri.Equ
                    x == y     -> :Tri.Iso
                    y == z     -> :Tri.Iso
                    else       -> :Tri.Sca
                }
             }
            println(analyse(10,20,30))
            println(analyse(10,20,25))
            println(analyse(10,20,20))
            println(analyse(10,10,10))
        """, true)
        assert(out == ":Tri.Err\n:Tri.Sca\n:Tri.Iso\n:Tri.Equ\n") { out }
    }

    // CHAPTER 2.2: Characters
    // TODO

}
