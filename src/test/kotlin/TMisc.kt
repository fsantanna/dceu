import org.junit.Test

class TMisc {
    @Test
    fun spawn_var() {
        val out = ceu.all(
            """
            var x
            spawn (task(){nil})(x) in coroutines()
            println(0)
        """
        )
        assert(out == "0\n") { out }
    }
    @Test
    fun bcast9() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                set v = yield nil
                throw #1                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            catch err==#1 {
                func () {
                    println(1)
                    broadcast 1
                    println(2)
                    broadcast 2
                    println(3)
                    broadcast 3
                }()
            }
            println(99)
        """
        )
        assert(out == "1\n2\n99\n") { out }
    }
    @Test
    fun bcast10() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                println(v)                
                set v = yield nil
                println(v)                
                set v = yield nil
                println(v)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            catch err==#1 {
                func () {
                    println(1)
                    broadcast 10
                    println(2)
                    broadcast [20]
                    println(3)
                    broadcast @[(30,30)]
                }()
            }
        """
        )
        assert(out == "1\n2\n99\n") { out }
    }
}
