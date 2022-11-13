import org.junit.Test
import java.io.File

class TBook {

    // CHAPTER 1.1

    @Test
    fun pg_2_square() {
        val out = all("""
            var square
            set square = func (x) {
                x * x
            }
            println(square(5))
        """, true)
        assert(out == "25\n") { out }
    }
    @Test
    fun pg_2_smaller() {
        val out = all("""
            var smaller
            set smaller = func (x,y) {
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
        val out = all("""
            var square
            set square = func (x) {
                x * x
            }
            var smaller
            set smaller = func (x,y) {
                if x < y { x } else { y }
            }
            println(square(smaller(3,5)))
        """, true)
        assert(out == "9\n") { out }
    }
    @Test
    fun pg_3_delta() {
        val out = all("""
            var delta
            set delta = func (a,b,c) {
                ((b**2) - (4*a*c)) // 2
            }
            println(delta(4.2,7,2.3))
        """, true)
        assert(out == "3.2187\n") { out }
    }
}
