import org.junit.Test

class TMisc {
    @Test
    fun funcOK() {
        val out = ceu.all(
            """
            func (xxx) {
                println(xxx)
                func () {
                    println(xxx)
                }()
            }(10)
        """)
        assert(out == "10\n10\n") { out }
    }
}
