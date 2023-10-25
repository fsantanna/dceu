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
                set v = yield((1)) ;;thus { it => nil }
                yield((2)) thus { it => nil }
            }
            coroutine(t)
            set v = resume a(1)
            resume a(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == """
            (set t = (task (v) {
            (set v = yield(1))
            ((yield(2)) thus { it =>
            nil
            })
            
            }))
            coroutine(t)
            (set v = (resume (a)(1)))
            (resume (a)(2))
            
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
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : spawn error : expected call")
    }
    @Test
    fun bb_03_spawn() {
        val l = lexer("""
            spawn T(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(spawn T(2))\n") { e.tostr() }
    }
    @Test
    fun bb_04_spawn_err() {
        val l = lexer("""
            spawn task () { nil } ()
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 19) : spawn error : unexpected \"task\"")
    }
    @Test
    fun bb_05_spawn() {
        val l = lexer("""
            spawn (task () { nil }) ()
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(spawn (task () {\n" +
                "nil\n" +
                "})(nil))\n") { e.tostr() }
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
        assert(e.tostr() == "(broadcast([]) in nil)\n") { e.tostr() }
    }
    @Test
    fun cc_06_bcast() {
        val l = lexer("""
            broadcast(nil) in t
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(broadcast(nil) in t)\n") { e.tostr() }
    }

    // PUB

    @Test
    fun dd_00_pub_a() {
        val l = lexer("""
            pub
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \"(\" : have end of file")
    }
    @Test
    fun dd_00_pub_b() {
        val l = lexer("""
            pub(
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected expression : have end of file")
    }
    @Test
    fun dd_00_pub_c() {
        val l = lexer("""
            pub(1
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \")\" : have end of file")
    }
    @Test
    fun dd_01_pub() {
        val l = lexer("""
            pub()
            set pub() = 10
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "pub()\n(set pub() = 10)\n") { e.tostr() }
    }
    @Test
    fun dd_02_pub() {
        val l = lexer("""
            pub(t)
            set pub(t) = 10
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "pub(t)\n(set pub(t) = 10)\n") { e.tostr() }
    }
    @Test
    fun dd_03_pub_tag() {
        val l = lexer("""
            task () :X {
                nil
            }
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(task () :X {\nnil\n})\n") { e.tostr() }
    }
    @Test
    fun dd_04_pub() {
        val l = lexer("""
            pub()()
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "pub()()\n") { e.tostr() }
    }
    @Test
    fun dd_05_pub() {
        val l = lexer("""
            set pub()() = 10
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 13) : set error : expected assignable destination")
    }
    @Test
    fun dd_06_pub() {
        val l = lexer("set pub = pub(x) + pub()")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 9) : expected \"(\" : have \"=\"")
    }
    @Test
    fun dd_07_pub() {
        val l = lexer("set pub() = pub(x) + pub()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(set pub() = {{+}}(pub(x),pub()))") { e.tostr() }
    }

    // VOID

    @Test
    fun ee_01_err() {
        val l = lexer("""
            func () :void {}
        """)
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 2, col 21) : expected \"{\" : have \":void\"")
    }
    @Test
    fun ee_02_task_err() {
        val l = lexer("""
            task :xxx () {}
        """.trimIndent())
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid task : unexpected \":xxx\"")
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected \"(\" : have \":xxx\"")
    }
    @Test
    fun ee_03_coro_err() {
        val l = lexer("coro (a,b) :void { 10 }")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 12) : expected \"{\" : have \":void\"")
    }
    @Test
    fun ee_04_task() {
        val l = lexer("task (a,b) :void { 10 }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(task (a,b) :void {\n10\n})") { e.tostr() }
    }

    // TOGGLE

    @Test
    fun ee_01_toggle() {
        val l = lexer("toggle t(true)")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(toggle t(true))\n") { e.tostr() }
    }
    @Test
    fun ee_02_toggle_err() {
        val l = lexer("toggle x")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 8) : invalid toggle : expected argument")
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected \"(\" : have end of file")
    }
    @Test
    fun ee_03_toggle_err() {
        val l = lexer("toggle")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have end of file")
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have end of file")
    }
    @Test
    fun ee_04_toggle_err() {
        val l = lexer("toggle x(1,2)")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 8) : invalid toggle : expected single argument")
        assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \")\" : have \",\"")
    }
    @Test
    fun ee_05_toggle_err() {
        val l = lexer("toggle f()")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 8) : invalid toggle : expected single argument")
        assert(trap { parser.expr() } == "anon : (lin 1, col 10) : expected expression : have \")\"")
    }
}
