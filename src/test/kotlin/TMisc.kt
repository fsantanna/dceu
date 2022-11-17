import org.junit.Test

class TMisc {
    @Test
    fun func11() {
        val out = ceu.all(
            """
            func () {
                var fff
                set fff = func () {
                    println(1)
                }
                fff()
            } ()
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun func11_xx() {
        val out = ceu.all(
            """
            println(func (x) {
                var fff
                set fff = func (xxx) {
                    println(tags(xxx))
                    xxx
                }
                fff(x)
            } (10))
        """)
        assert(out == "#number\n10\n") { out }
    }
    @Test
    fun bcast2() {
        val out = ceu.all(
            """
            var co1
            set co1 = spawn task () {
                var co2
                set co2 = spawn task () {
                    yield ()
                    println(2)
                }
                resume co2 ()
                yield ()
                println(1)
            }
            resume co1 ()
            broadcast nil
        """
        )
        assert(out == "2\n1\n") { out }
    }
}
