package tst_03

import dceu.*
import org.junit.Test

class Parser_03 {

    // EXPORT

    @Test
    fun oo_01_export_err() {
        val l = tst_02.lexer("export {}")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 8) : expected \"[\" : have \"{\"")
    }
    @Test
    fun oo_02_export_err() {
        val l = tst_02.lexer("export [:x] {}")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 9) : expected identifier : have \":x\"")
    }
    @Test
    fun oo_03_export() {
        val l = tst_02.lexer("export [] { nil }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Export && e.ids.isEmpty() && e.body.es.size==1)
        assert(e.tostr() == "export [] {\nnil\n}") { e.tostr() }
    }
    @Test
    fun oo_04_export() {
        val l = tst_02.lexer("export [x] { nil }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Export && e.ids.first()=="x" && e.body.es.size==1)
        assert(e.tostr() == "export [x] {\nnil\n}") { e.tostr() }
    }
    @Test
    fun oo_05_export() {
        val l = tst_02.lexer("export [x,y] { nil }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Export && e.ids.last()=="y" && e.body.es.size==1 && e.ids.size==2)
        assert(e.tostr() == "export [x,y] {\nnil\n}") { e.tostr() }
    }

    // EVT / ERR / PUB

    @Test
    fun aa_01_evt () {
        val l = lexer(" evt ")
        val parser = Parser(l)
        val e = parser.expr_prim()
        //assert(e is Expr.EvtErr && e.tk.str == "evt")
        assert(e is Expr.Acc && e.tk.str == "evt")
    }
    @Test
    fun aa_02_err () {
        val l = lexer(" err ")
        val parser = Parser(l)
        val e = parser.expr_prim()
        //assert(e is Expr.EvtErr && e.tk.str == "err")
        assert(e is Expr.Acc && e.tk.str == "err")
    }
    @Test
    fun aa_03_pub() {
        val l = lexer("x . pub")
        val parser = Parser(l)
        val e = parser.expr()
        //assert(e is Expr.Pub && e.x is Expr.Acc)
        assert(e.tostr() == "x.pub") { e.tostr() }
    }
    @Test
    fun aa_04_set_evt_err() {
        val l = lexer("set evt = nil")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid set : expected assignable destination")
    }

    // PUB / INDEX

    @Test
    fun index5_err() {
        val l = lexer("x . .")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected \"pub\" : have \".\"")
    }
    @Test
    fun index6_err() {
        val l = lexer("x . 2")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected \"pub\" : have \"2\"")
    }
    @Test
    fun pub7_err() {
        val l = lexer("set pub = x.pub + pub")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected expression : have \"pub\"")
    }
    @Test
    fun pub8() {
        val l = lexer("set task.pub = x.pub + task.pub")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "set task.pub = {{+}}(x.pub,task.pub)") { e.tostr() }
    }

    // CORO / TASK

    @Test
    fun expr_func3_err() {
        val l = lexer("coro (a,b) :fake { 10 }")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 12) : expected \"{\" : have \":fake\"")
    }
    @Test
    fun expr_task4_err() {
        val l = lexer("coro (a,b) :xxx { 10 }")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 12) : expected \"{\" : have \":xxx\"")
    }
    @Test
    fun expr_task5() {
        val l = lexer("task (a,b) { 10 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.args.size==2)
        assert(e.tostr() == "(task (a,b) {\n10\n})") { e.tostr() }
    }

    // TASK / YIELD / RESUME

    @Test
    fun task1() {
        val l = lexer("""
            set t = task (v) {
                set v = yield((1)) 
                yield((2)) 
            }
            coroutine(t)
            set v = resume a(1)
            resume a(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == """
            set t = (task (v) {
            set v = yield(1)
            yield(2)
            })
            coroutine(t)
            set v = resume a(1)
            resume a(2)
            
        """.trimIndent()) { e.tostr() }
    }
    @Test
    fun task2_err() {
        val l = lexer("""
            resume a
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : invalid resume : expected call")
    }
    @Test
    fun yield1_err() {
        val l = lexer("""
            yield
            1
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 2, col 1) : expected \"(\" : have \"1\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : yield error : line break before expression")
    }
    @Test
    fun yield2_err() {
        val l = lexer("""
            yield
            (1)
        """.trimIndent())
        val parser = Parser(l)
        val e = parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : yield error : line break before expression")
        assert(e.tostr() == "yield(1)")
    }
    @Test
    fun func3_err() {
        val l = lexer("""
            func () :fake {}
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected \"{\" : have \":fake\"")
    }
    @Test
    fun task4_err() {
        val l = lexer("""
            task :xxx () {}
        """.trimIndent())
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid task : unexpected \":xxx\"")
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected \"(\" : have \":xxx\"")
    }

    // BROADCAST

    @Test
    fun bcast_coro1_err() {
        val l = lexer("broadcast in")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 13) : expected expression : have end of file")
    }
    @Test
    fun bcast_coro2_err() {
        val l = lexer("broadcast in x")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 15) : expected \",\" : have end of file")
    }
    @Test
    fun bcast_coro3_err() {
        val l = lexer("broadcast in x,")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 16) : expected expression : have end of file")
    }
    @Test
    fun bcast_coro4() {
        val l = lexer("broadcast in x, 10")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "broadcast in x, 10\n") { e.tostr() }
    }

    // TOGGLE

    @Test
    fun toggle1() {
        val l = lexer("toggle t(true)")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "toggle t(true)\n") { e.tostr() }
    }
    @Test
    fun toggle2_err() {
        val l = lexer("toggle x")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : invalid toggle : expected argument")
    }
    @Test
    fun toggle3_err() {
        val l = lexer("toggle")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have end of file")
    }
    @Test
    fun toggle4_err() {
        val l = lexer("toggle x(1,2)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : invalid toggle : expected single argument")
    }
    @Test
    fun toggle5_err() {
        val l = lexer("toggle f()")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : invalid toggle : expected single argument")
    }

    // DEFER

    @Test
    fun defer() {
        val l = lexer("defer { nil }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "defer {\nnil\n}\n") { e.tostr() }
    }

    // COROS

    //@Ignore // now expands to complex C code
    @Test
    fun tasks1() {
        val l = lexer("""
            var ts
            set ts = tasks()
            loop t in :tasks ts {
                nil
            }
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "var ts\nset ts = tasks()\nlopp in :tasks ts, t {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun tasks2_err() {
        val l = lexer("""
            var ts
            set ts = tasks()
            loop [] in :tasks ts {
                nil
            }
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 4, col 18) : invalid loop : unexpected [")
    }
    @Test
    fun tasks3_err() {
        val l = lexer("""
            var ts
            set ts = tasks()
            loop x in {
                nil
            }
        """)
        val parser = Parser(l)
        //assert(trap { parser.exprs() } == "anon : (lin 4, col 23) : expected \":tasks\" : have \"{\"")
        assert(trap { parser.exprs() } == "anon : (lin 4, col 23) : invalid loop : unexpected {")
    }
    @Test
    fun tasks4_err() {
        val l = lexer("""
            spawn in nil, {}
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 27) : expected expression : have \"{\"")
    }
    @Test
    fun tasks5_err() {
        val l = lexer("""
            spawn in ()
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected expression : have \")\"")
    }
    @Test
    fun tasks6_err() {
        val l = lexer("""
            spawn in nil, f
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : invalid spawn : expected call")
    }
    @Test
    fun tasks7_err() {
        val l = lexer("""
            spawn
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 3, col 9) : expected expression : have end of file")
    }

    // ITER

    @Test
    fun iter1_err() {
        val l = lexer("""
            loop in 1 {
                nil
            }
        """)
        val parser = Parser(l)
        //assert(trap { parser.exprs() } == "anon : (lin 2, col 21) : expected \":tasks\" : have \"1\"")

        assert(trap { parser.exprs() } == "anon : (lin 2, col 21) : invalid loop : unexpected 1")
    }
    @Test
    fun iter2_err() {
        val l = lexer("""
            loop in :tasks {
                nil
            }
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 28) : expected expression : have \"{\"")
    }

    // TRACK

    @Test
    fun track1() {
        val l = lexer("""
            track(x)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "track(x)\n") { e.tostr() }
    }

}
