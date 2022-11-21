import org.junit.Test

class TMisc {
    @Test
    fun catchA() {
        val out = ceu.all(
            """
            catch do {
                err==#x
            } {
                throw #x
                println(9)
            }
            println(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun catchB() {
        val out = ceu.all(
            """
            var x
            catch do {
                set x = err
                err==#x
            } {
                throw [#x]
                println(9)
            }
            println(x)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun catchC() {
        val out = ceu.all(
            """
            catch err==#x {
                throw [#x]
                println(9)
            }
            println(err)
        """
        )
        assert(out == "1\n") { out }
    }
}
