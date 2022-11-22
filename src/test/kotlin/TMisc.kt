import org.junit.Test

class TMisc {
    @Test
    fun bcast11() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                do {
                    set v = evt
                    yield nil
                    set v = evt
                }
                do {
                    println(v)
                    set v = yield nil
                    println(v)
                }
            }
            var co
            set co = coroutine tk
            broadcast 1
            broadcast 2
            broadcast 3
            broadcast 4
        """
        )
        assert(out == "1\n2\n2\n3\n") { out }
    }
    @Test
    fun pool12_err_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield nil }
            var ts
            set ts = coroutines()
            spawn T() in ts
            while xxx in ts {
                var yyy
                while zzz in ts {
                    set yyy = zzz
                }
                set yyy = xxx
            }
        """
        )
        assert(out == "anon : (lin 10, col 31) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun pool13_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield nil }
            var ts
            set ts = coroutines()
            spawn T() in ts
            while xxx in ts {
                var yyy
                while zzz in ts {
                    nil
                }
                set yyy = xxx
            }
            println(1)
        """
        )
        assert(out == "1\n") { out }
    }
}
