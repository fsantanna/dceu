package tst_04

import dceu.*
import org.junit.Test

class Parser_04 {
    // TASK

    @Test
    fun aa_01_task() {
        val l = lexer("task (a,b) { 10 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.pars.size==2)
        assert(e.to_str() == "(task (a,b) {\n10;\n})") { e.to_str() }
    }
    @Test
    fun aa_02_task() {
        val l = lexer("""
            set t = task (v) {
                set v = yield((1)) ;;thus { it => nil }
                yield((2)) ;;thus { it => nil }
            }
            coroutine(t)
            set v = resume a(1)
            resume a(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == """
            (set t = (task (v) {
            (set v = yield(1));
            yield(2);
            }));
            coroutine(t);
            (set v = (resume (a)(1)));
            (resume (a)(2));
            
        """.trimIndent()) { e.to_str() }
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
        assert(e.to_str() == "(spawn T(2));\n") { e.to_str() }
    }
    @Test
    fun bb_04_spawn_err() {
        val l = lexer("""
            spawn task () { nil } ()
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "(spawn (task () {\n" +
                "nil;\n" +
                "})());\n") { e.to_str() }
        //assert(trap { parser.exprs() } == "anon : (lin 2, col 19) : spawn error : unexpected \"task\"")
    }
    @Test
    fun bb_05_spawn() {
        val l = lexer("""
            spawn (task () { nil }) ()
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "(spawn (task () {\n" +
                "nil;\n" +
                "})());\n") { e.to_str() }
    }
    @Test
    fun todo_bb_06_spawn_dots_err() {   // TODO: ... should be allowed
        val l = lexer("""
            spawn T(...)
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 19) : spawn error : \"...\" is not allowed")
    }

    // DELAY

    @Test
    fun bj_01_delay() {
        val l = lexer("""
            delay
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "delay;\n") { e.to_str() }
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
        assert(trap { parser.exprs() } == "anon : (lin 2, col 35) : expected expression : have \",\"")
    }
    @Test
    fun cc_05_bcast_err() {
        val l = lexer("""
            broadcast ([]) in nil
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "broadcast'(nil,[]);\n") { e.to_str() }
    }
    @Test
    fun cc_06_bcast() {
        val l = lexer("""
            broadcast(nil) in t
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "broadcast'(t,nil);\n") { e.to_str() }
    }

    // PUB

    @Test
    fun dd_00_pub_a() {
        val l = lexer("""
            pub
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "pub;\n") { e.to_str() }
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \"(\" : have end of file")
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
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected \",\" : have end of file")
    }
    @Test
    fun dd_01_pub() {
        val l = lexer("""
            pub
            set pub = 10
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "pub;\n(set pub = 10);\n") { e.to_str() }
    }
    @Test
    fun dd_02_pub() {
        val l = lexer("""
            t.pub
            set t.pub = 10
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "t.pub;\n(set t.pub = 10);\n") { e.to_str() }
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
        assert(e.to_str() == "(task () :X {\nnil;\n});\n") { e.to_str() }
    }
    @Test
    fun dd_04_pub() {
        val l = lexer("""
            pub()
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "pub();\n") { e.to_str() }
    }
    @Test
    fun dd_05_pub() {
        val l = lexer("""
            set pub() = 10
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 13) : set error : expected assignable destination")
    }
    @Test
    fun dd_06_pub() {
        val l = lexer("set pub = x.pub + pub")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.to_str() == "(set pub = {{+}}(x.pub,pub))") { e.to_str() }
    }

    // TOGGLE

    @Test
    fun ee_01_toggle() {
        val l = lexer("toggle t(true)")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.to_str() == "(toggle t(true));\n") { e.to_str() }
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
