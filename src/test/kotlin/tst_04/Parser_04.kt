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
                set v = yield((1)) { nil }
                yield((2)) { nil }
            }
            coroutine(t)
            set v = resume a(1)
            resume a(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == """
            set t = (task (v) {
            set v = yield(1) {
            nil
            }
            yield(2) {
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
            broadcast
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \"in\" : have end of file")
    }
    @Test
    fun cc_02_bcast_err() {
        val l = lexer("""
            broadcast in
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected expression : have end of file")
    }
    @Test
    fun cc_03_bcast_err() {
        val l = lexer("""
            broadcast in nil
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \",\" : have end of file")
    }
    @Test
    fun cc_04_bcast_err() {
        val l = lexer("""
            broadcast in nil,
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected expression : have end of file")
    }
    @Test
    fun cc_05_bcast_err() {
        val l = lexer("""
            broadcast in nil, []
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "broadcast in nil, []\n") { e.tostr() }
    }

    // TASKS

    @Test
    fun dd_01_tasks_err() {
        val l = lexer("""
            spawn in nil, {}
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 27) : expected expression : have \"{\"")
    }
    @Test
    fun dd_02_tasks_err() {
        val l = lexer("""
            spawn in ()
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected expression : have \")\"")
    }
    @Test
    fun dd_03_tasks_err() {
        val l = lexer("""
            spawn in nil, f
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : invalid spawn : expected call")
    }
    @Test
    fun dd_04_tasks() {
        val l = lexer("""
            spawn in ts, T()
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "spawn in ts, T()\n") { e.tostr() }
    }

}
