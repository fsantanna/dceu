import org.junit.Test

class TMisc {
    @Test
    fun bcast2() {
        val out = ceu.all(
            """
            var co1
            set co1 = spawn task () {
                var co2
                set co2 = spawn task () {
println(99)
                    yield ()
                    println(2)
                }
                resume co2 ()
println(100)
                yield ()
                println(1)
            }
            resume co1 ()
println(101)
            broadcast nil
        """
        )
        assert(out == "2\n1\n") { out }
    }
}
