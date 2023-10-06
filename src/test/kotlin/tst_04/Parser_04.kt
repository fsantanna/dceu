package tst_04

import dceu.*
import org.junit.BeforeClass
import org.junit.Test

class Parser_04 {
    // TASK

    @Test
    fun aa_01_task() {
        val l = lexer("task (a,b) { 10 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.args.size==2)
        assert(e.tostr() == "(task (a,b) {\n10\n})") { e.tostr() }
    }
    @Test
    fun aa_02_task() {
        val l = lexer("""
            set t = task (v) {
                set v = yield((1)) { as it => nil }
                yield((2)) { as it => nil }
            }
            coroutine(t)
            set v = resume a(1)
            resume a(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == """
            set t = (task (v) {
            set v = yield(1) { as it =>
            nil
            }
            yield(2) { as it =>
            nil
            }
            })
            coroutine(t)
            set v = resume a(1)
            resume a(2)
            
        """.trimIndent()) { e.tostr() }
    }

    // SPAWN

    @Test
    fun bb_01_spawn_err() {
        val l = lexer("""
            spawn
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected expression : have end of file")
    }
    @Test
    fun bb_02_spawn_err() {
        val l = lexer("""
            spawn nil
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : invalid spawn : expected call")
    }
    @Test
    fun bb_03_spawn() {
        val l = lexer("""
            spawn T(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "spawn T(2)\n") { e.tostr() }
    }

    // BCAST

    @Test
    fun cc_01_bcast_err() {
        val l = lexer("""
            broadcast 1
        """)
        val parser = Parser(l)
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected expression : have end of file")
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \"in\" : have end of file")
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected \"(\" : have \"1\"")
    }
    @Test
    fun cc_02_bcast_err() {
        val l = lexer("""
            broadcast
        """)
        val parser = Parser(l)
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected expression : have end of file")
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \"in\" : have end of file")
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \"(\" : have end of file")
    }
    @Test
    fun cc_03_bcast_err() {
        val l = lexer("""
            broadcast (nil) in
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected expression : have end of file")
    }
    @Test
    fun cc_04_bcast_err() {
        val l = lexer("""
            broadcast in nil
        """)
        val parser = Parser(l)
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \",\" : have end of file")
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected \"(\" : have \"in\"")
    }
    @Test
    fun cc_0X_bcast_err() {
        val l = lexer("""
            broadcast (nil) in nil,
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 0) : expected expression : have \",\"")
    }
    @Test
    fun cc_05_bcast_err() {
        val l = lexer("""
            broadcast ([]) in nil
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "broadcast([]) in nil\n") { e.tostr() }
    }
    @Test
    fun cc_06_bcast() {
        val l = lexer("""
            broadcast(nil) in t
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "broadcast(nil) in t\n") { e.tostr() }
    }

    // PUB

    @Test
    fun dd_01_pub() {
        val l = lexer("""
            set pub = 10
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "set pub = 10\n") { e.tostr() }
    }
    @Test
    fun dd_02_pub() {
        val l = lexer("""
            pub(t)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "pub(t)\n") { e.tostr() }
    }
}
