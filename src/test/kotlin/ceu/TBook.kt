import org.junit.Ignore
import org.junit.Test

class TBook {

    // CHAPTER 1.1: Fundamental concepts

    @Test
    fun pg_2_square() {
        val out = all(
            """
            var square
            set square = func (x) {
                x * x
            }
            println(square(5))
        """, true
        )
        assert(out == "25\n") { out }
    }
    @Test
    fun pg_2_smaller() {
        val out = all(
            """
            var smaller
            set smaller = func (x,y) {
                if x < y { x } else { y }
            }
            println(smaller(1,2))
            println(smaller(1,1))
            println(smaller(2,1))
        """, true
        )
        assert(out == "1\n1\n1\n") { out }
    }
    @Test
    fun pg_3_square_smaller() {
        val out = all(
            """
            var square
            set square = func (x) {
                x * x
            }
            var smaller
            set smaller = func (x,y) {
                if x < y { x } else { y }
            }
            println(square(smaller(3,5)))
        """, true
        )
        assert(out == "9\n") { out }
    }
    @Test
    fun pg_3_delta() {
        val out = all(
            """
            var delta
            set delta = func (a,b,c) {
                ((b**2) - (4*a*c)) // 2
            }
            println(delta(4.2,7,2.3))
        """, true
        )
        assert(out == "3.2187\n") { out }
    }

    // CHAPTER 1.4: Functions

    @Test
    @Ignore
    fun todo_pg_11_currying() {
        val out = all(
            """
            var smallerc
            set smallerc = func (x) {
                func (y) {
                    if x < y { x } else { y }
                }
            }
            var plusc
            set plusc = func (x) {
                func (y) {
                    x+y
                }
            }
            println(smallerc(3)(5))
            println(plus(3)(5))
        """, true
        )
        assert(out == "3\n8\n") { out }
    }
    @Test
    @Ignore
    fun todo_pg_12_twice_quad() {
        val out = all(
            """
            var square
            set square = func (x) {
                x**2
            }
            var twice
            set twice = func (f,x) {
                f(f(x))
            }
            var quad
            set quad = func (x) {
                twice(square)
            }
            println(twice(square, 2), quad(2))
        """, true
        )
        assert(out == "16\t16\n") { out }
    }
    @Test
    @Ignore
    fun todo_pg_12_twicec() {
        val out = all(
            """
            var square
            set square = func (x) {
                x**2
            }
            var twicec
            set twicec = func (f) {
                func (v) {
                    f(f(v))
                }
            }
            var quad
            set quad = twicec(square)
            println(quad(2))
        """, true
        )
        assert(out == "16\t16\n") { out }
    }
    @Test
    @Ignore
    fun todo_pg_13_curry() {
        val out = all(
            """
            var curry
            set curry = func (f) {
                func (x) {
                    func (y) {
                        f(x,y)
                    }
                }
            }
            var plusc
            set plusc = curry({+})
            println(plusc(1)(-4))
        """, true
        )
        assert(out == "-3\n") { out }
    }
    @Test
    @Ignore
    fun todo_pg_13_uncurry() {
        val out = all(
            """
            var plusc
            set plusc = func (x) {
                func (y) {
                    x + y
                }
            }
            var uncurry
            set uncurry = func (f) {
                func (x,y) {
                    f(x)(y)
                }            
            }
            println(uncurry(plusc)(1,-4))
        """, true
        )
        assert(out == "-3\n") { out }
    }
    @Test
    fun pg_13_ops() {
        val out = all(
            """
            println({*}(1 + 3, 4))
        """, true
        )
        assert(out == "16\n") { out }
    }
    @Test
    @Ignore
    fun todo_pg_15_compose() {
        val out = all(
            """
            var compose
            set compose = func (f,g) {
                func (v) {
                    f(g(v))
                }
            }
            var square
            set square = func (x) {
                x**2
            }
            var quad
            set quad = compose(square,square)
            println(quad(2))
        """, true
        )
        assert(out == "16\n") { out }
    }

    // CHAPTER 1.5: Definitions

    @Test
    fun pg_18_signum() {
        val out = all(
            """
            var signum
            set signum = func (x) {
                if x < 0 {
                    -1
                } else {
                    if x > 0 {
                        1
                    } else {
                        0
                    }
                }
            }
            println(signum(10), signum(-9), signum(0))
        """, true
        )
        assert(out == "1\t-1\t0\n") { out }
    }
    @Test
    fun pg_19_fact() {
        val out = all(
            """
            var fact
            set fact = func (x) {
                if x == 0 {
                    1
                } else {
                    x * fact(x-1)
                }
            }
            println(fact(5))
        """, true
        )
        assert(out == "120\n") { out }
    }

    // CHAPTER 1.6: Types

    @Test
    fun pg_23_fact() {
        val out = all(
            """
            var fact
            set fact = func (x) {
                if x < 0 {
                    throw @error
                } else {nil}
                if x == 0 {
                    1
                } else {
                    x * fact(x-1)
                }
            }
            println(fact(-1))
        """, true
        )
        assert(out == "anon : (lin 5, col 21) : throw error : uncaught exception\n") { out }
    }
}
