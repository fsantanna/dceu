package tst_03

import dceu.*
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_03 {
    @Test
    fun zz_04_tags() {
        val out = test("""
            val co = coro () {
                yield(:x)
            }
            println(:y)
        """)
        assert(out == ":y\n") { out }
    }
}
