package tst_99

import dceu.*
import org.junit.Test

class Parser_99 {
    // EMPTY IF / BLOCK

    @Test
    fun aa_01_empty_if() {
        val l = lexer("if true { 1 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n} else {\nnil\n}") { e.tostr() }
    }
    @Test
    fun aa_02_empty_do() {  // set whole tuple?
        val l = lexer("do{}")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Do && e.es.size==0)
        assert(e.tostr() == "do {\n\n}") { e.tostr() }
    }
    @Test
    fun aa_03_empty_func() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.pars.size==0)
        assert(e.tostr() == "(func () {\n\n})") { e.tostr() }
    }
    @Test
    fun aa_04_empty_loop() {
        val l = lexer("loop { }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e.tostr() == "loop {\n\n}") { e.tostr() }
    }

    // OPS: not, and, or

    @Test
    fun bb_01_bin_or() {
        N = 1
        val l = lexer("1 or 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_5) = 1)\n" +
                "if ceu_5 {\n" +
                "ceu_5\n" +
                "} else {\n" +
                "2\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun bb_02_bin_and() {
        N = 1
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_5) = 1)\n" +
                "if ceu_5 {\n" +
                "2\n" +
                "} else {\n" +
                "ceu_5\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun bb_03_not() {
        N = 1
        val l = lexer("not true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if true {\nfalse\n} else {\ntrue\n}") { e.tostr() }
    }
    @Test
    fun bb_04_bin_not_or_and() {
        N = 1
        val l = lexer("((not true) and false) or true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            (val (ceu_76) = do {
            (val (ceu_27) = if true {
            false
            } else {
            true
            })
            if ceu_27 {
            false
            } else {
            ceu_27
            }
            })
            if ceu_76 {
            ceu_76
            } else {
            true
            }
            }
        """.trimIndent()) { e.tostr() }
    }
    @Test
    fun bb_05_pre() {
        val l = lexer("- not - 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{{-}}(if {{-}}(1) {\nfalse\n} else {\ntrue\n})") { e.tostr() }
    }
    @Test
    fun bb_06_pre() {
        val l = lexer("""
            `a` or ((`b` or `c`) or `d`)
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            (val (ceu_5) = ```a```)
            if ceu_5 {
            ceu_5
            } else {
            do {
            (val (ceu_42) = do {
            (val (ceu_10) = ```b```)
            if ceu_10 {
            ceu_10
            } else {
            ```c```
            }
            })
            if ceu_42 {
            ceu_42
            } else {
            ```d```
            }
            }
            }
            }
        """.trimIndent()) { e.tostr() }
    }

    // OPS: is?, is-not?, in?, in-not?

    @Test
    fun bc_01_pre() {
        val l = lexer("a is? b ; c is-not? d ; e in? f ; g in-not? h")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "is'(a,b)\nis-not'(c,d)\nin'(e,f)\nin-not'(g,h)\n") { e.tostr() }
    }

    // FUNC / DCL / REC

    @Test
    fun cc_01_func() {
        val l = lexer("func f () {}")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(val (f) = (func () {\n\n}))\n") { e.tostr() }
    }
    @Test
    fun cc_02_func_rec_err() {
        val l = lexer("func :rec () {}")
        val parser = Parser(l)
        //val e = parser.exprs()
        //assert(e.tostr() == "(func () {\n\n})\n") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected identifier : have \"(\"")
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected \"(\" : have \":rec\"")
    }
    @Test
    fun cc_03_func_rec() {
        val l = lexer("func f () {}")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(val (f) = (func () {\n" +
                "\n" +
                "}))\n") { e.tostr() }
    }

    // IF / ID-TAG

    /*
    @Test
    fun dd_01_if() {
        val l = lexer("if x:X { :ok }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : expected \"=\" : have \"{\"")
    }
    @Test
    fun dd_02_if() {
        val l = lexer("if x=1 { x }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "do {\n" +
                "(val x = 1)\n" +
                "if x {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n") { e.tostr() }
    }
    @Test
    fun dd_03_if() {
        val l = lexer("if x:X=1 { x }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "do {\n" +
                "(val x :X = 1)\n" +
                "if x {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n") { e.tostr() }
    }
     */
    @Test
    fun dd_04_if() {
        val l = lexer("if f() {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if f() {\n" +
                "\n" +
                "} else {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }

    // IF / =>

    @Test
    fun de_01_if() {
        val l = lexer("if false => 1 => 2")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "if false {\n" +
                "1\n" +
                "} else {\n" +
                "2\n" +
                "}\n") { e.tostr() }
    }
    @Test
    fun de_02_if() {
        val l = lexer("if false { 1 } => 2")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 16) : expected expression : have \"=>\"")
    }
    @Test
    fun de_03_if_err() {
        val l = lexer("(if true => 1)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 14) : expected \"=>\" : have \")\"")
    }
    @Test
    fun de_04_if_err() {
        val l = lexer("if false => 1 --> nil => 2")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 15) : expected \"=>\" : have \"-->\"")
    }

    // IFS

    @Test
    fun ee_01_ifs() {
        val l = lexer("ifs { a=>1 else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_19) = a)\n" +
                "if ceu_19 {\n" +
                "1\n" +
                "} else {\n" +
                "(val (ceu_20) = true)\n" +
                "if ceu_20 {\n" +
                "0\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ee_02_ifs() {
        val l = lexer("ifs { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }
    @Test
    fun ee_03_ifs() {
        val l = lexer("match nil { else => it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_16_0) = nil)\n" +
                "(val (ceu_9) = ceu_16_0)\n" +
                "(val (ceu_16) = true)\n" +
                "if ceu_16 {\n" +
                "it\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ee_04_ifs_err() {
        val l = lexer("ifs { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \"{\" : have \"}\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \"->\" : have \"}\"")
    }
    @Test
    fun ee_05_ifs() {
        val l = lexer("match v { a => it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_14_0) = v)\n" +
                "(val (a) = ceu_14_0)\n" +
                "(val (ceu_14) = true)\n" +
                "if ceu_14 {\n" +
                "it\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 13) : expected \",\" : have \"=>\"")
    }
    @Test
    fun ee_06_ifs() {
        val l = lexer("match v { (|a) {1} (|b)=>v else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_36_0) = v)\n" +
                "(val (it) = ceu_36_0)\n" +
                "(val (ceu_36) = a)\n" +
                "if ceu_36 {\n" +
                "1\n" +
                "} else {\n" +
                "(val (it) = ceu_36_0)\n" +
                "(val (ceu_37) = b)\n" +
                "if ceu_37 {\n" +
                "v\n" +
                "} else {\n" +
                "(val (ceu_28) = ceu_36_0)\n" +
                "(val (ceu_38) = true)\n" +
                "if ceu_38 {\n" +
                "0\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ee_07_ifs() {
        val l = lexer("ifs { f() => nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_16) = f())\n" +
                "if ceu_16 {\n" +
                "nil\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ee_08_ifs_nocnd() {
        val l = lexer("""
            ifs {
                == 20 => nil   ;; err: no ifs expr
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_18) = {{==}}(20))\n" +
                "if ceu_18 {\n" +
                "nil\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 3, col 17) : case error : expected ifs condition")
    }
    @Test
    fun ee_08x_ifs_nocnd() {
        val l = lexer("""
            ifs {
                (,nil) => nil   ;; err: no ifs expr
            }
        """)
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 3, col 18) : expected expression : have \",\"")
    }
    @Test
    fun ee_09_ifs_nocnd() {
        val l = lexer("""
            val x = match 20 {
                true => ifs {
                    == 20 => true   ;; err: no ifs expr
                }
            }
            println(x)
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(val (x) = do {\n" +
                "(val (ceu_77_0) = 20)\n" +
                "(val (it) = ceu_77_0)\n" +
                "(val (ceu_77) = {{===}}(it,true))\n" +
                "if ceu_77 {\n" +
                "do {\n" +
                "(val (ceu_42) = {{==}}(20))\n" +
                "if ceu_42 {\n" +
                "true\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "})") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 4, col 21) : case error : expected ifs condition")
    }
    @Test
    fun ee_10_ifs() {
        val l = lexer("match v { x => 10 }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_14_0) = v)\n" +
                "(val (x) = ceu_14_0)\n" +
                "(val (ceu_14) = true)\n" +
                "if ceu_14 {\n" +
                "10\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }

    // IFS / MULTI

    @Test
    fun ef_01_ifs_err() {
        val l = lexer("match (1,...) { (1,2) => 10 }")
        val parser = Parser(l)
        //val e = parser.expr()
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : match error : unexpected \"...\"")
    }
    @Test
    fun ef_02_ifs() {
        val l = lexer("match (1,2) { (1,2) => 10 }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_53_0) = 1)\n" +
                "(val (ceu_53_1) = 2)\n" +
                "(val (it) = ceu_53_0)\n" +
                "(val (ceu_32) = ceu_53_1)\n" +
                "(val (ceu_53) = do {\n" +
                "(val (ceu_96) = {{===}}(it,1))\n" +
                "if ceu_96 {\n" +
                "{{===}}(ceu_32,2)\n" +
                "} else {\n" +
                "ceu_96\n" +
                "}\n" +
                "})\n" +
                "if ceu_53 {\n" +
                "10\n" +
                "} else {\n" +
                "\n" +
                "}\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }

    // THUS AS

    @Test
    fun oo_01_thus_err() {
        val l = tst_03.lexer(
            """
            1 thus { 1 }
        """
        )
        val parser = Parser(l)
        val e = parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 2, col 22) : expected identifier : have \"1\"")
        assert(e.tostr() == "do {\n" +
                "(val (it) = 1)\n" +
                "1\n" +
                "}") { e.tostr() }
    }
    @Test
    fun oo_02_thus_err() {
        val l = tst_03.lexer(
            """
            1 thus { x }
        """
        )
        val parser = Parser(l)
        val e = parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 2, col 24) : expected \"=>\" : have \"}\"")
        assert(e.tostr() == "do {\n" +
                "(val (it) = 1)\n" +
                "x\n" +
                "}") { e.tostr() }
    }
    @Test
    fun oo_03_thus_err() {
        val l = tst_03.lexer(
            """
            1 thus { ,x => }
        """
        )
        val parser = Parser(l)
        val e = parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 2, col 27) : expected expression : have \"}\"")
        assert(e.tostr() == "do {\n" +
                "(val (x) = 1)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun oo_04_thus() {
        val l = tst_04.lexer("""
            1 thus { ,it => nil }
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        //assert(e.tostr() == "((1) thus { it =>\nnil\n})\n") { e.tostr() }
        assert(e.tostr() == "do {\n" +
                "(val (it) = 1)\n" +
                "nil\n" +
                "}\n") { e.tostr() }
    }
    @Test
    fun BUG_oo_05_thus_thus() {
        val l = tst_04.lexer("""
            1 thus { it => 2 } thus { it => 3 }
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "do {\n" +
                "(val it = do {\n" +
                "(val it = 1)\n" +
                "2\n" +
                "})\n" +
                "3\n" +
                "}\n") { e.tostr() }
    }
    @Test
    fun oo_06_yield_err() {
        val l = tst_03.lexer(
            """
            yield(1) thus
        """
        )
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 3, col 9) : expected \"{\" : have end of file")
        //val e = parser.expr()
        //assert(e.tostr() == "yield(1)")
    }
    @Test
    fun oo_07_yield() {
        val l = tst_03.lexer(
            """
            yield
            (1) thus
            { ,it => nil }
        """.trimIndent()
        )
        val parser = Parser(l)
        val e = parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : yield error : line break before expression")
        //assert(e.tostr() == "((yield(1)) thus { it =>\nnil\n})") { e.tostr() }
        assert(e.tostr() == "do {\n" +
                "(val (it) = yield(1))\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun oo_08_coro() {
        val l = tst_03.lexer(
            """
            set t = coro (v) {
                set v = yield((1)) ;;thus { it:X => it }
                yield((2)) thus { ,it => nil }
            }
            coroutine(t)
            set v = resume a(1)
            resume a(2)
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        println(e.size)
        assert(e.tostr() == "(set t = (coro (v) {\n" +
                "(set v = yield(1))\n" +
                "do {\n" +
                "(val (it) = yield(2))\n" +
                "nil\n" +
                "}\n" +
                "}))\n" +
                "coroutine(t)\n" +
                "(set v = (resume (a)(1)))\n" +
                "(resume (a)(2))\n") { e.tostr() }
    }

    // LOOP / ITER / WHILE / UNTIL / NUMERIC

    @Test
    fun ef_01_iter() {
        val l = lexer("loop x in f { x }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_14 :Iterator) = to-iter(f))\n" +
                "loop {\n" +
                "(val (x) = ceu_14[:f](ceu_14))\n" +
                "(break(nil) if {{==}}(ceu_14[:f],nil))\n" +
                "x\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ef_02_while() {
        val l = lexer("loop { while (i<10) }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "(break if if {{<}}(i,10) {\n" +
                "false\n" +
                "} else {\n" +
                "true\n" +
                "})\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ef_03_until() {
        val l = lexer("loop { until (x==1) }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "(break if {{==}}(x,1))\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ef_04_numeric() {
        val l = lexer("""
            loop i in {0 => n{ :step +x {
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_ste_21) = x)\n" +
                "(var (i) = {{+}}(0,0))\n" +
                "(val (ceu_lim_21) = n)\n" +
                "loop {\n" +
                "(break(false) if {{>=}}(i,ceu_lim_21))\n" +
                "(set i = {{+}}(i,ceu_ste_21))\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ef_05_numeric() {
        val l = lexer("""
            loop in {1 => 10} {
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_ste_17) = 1)\n" +
                "(var (it) = {{+}}(1,0))\n" +
                "(val (ceu_lim_17) = 10)\n" +
                "loop {\n" +
                "(break(false) if {{>}}(it,ceu_lim_17))\n" +
                "(set it = {{+}}(it,ceu_ste_17))\n" +
                "}\n" +
                "}") { e.tostr() }
    }

    // AS / YIELD / DETRACK / THUS

    @Test
    fun ff_01_yield() {
        val l = lexer("yield()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "yield()") { e.tostr() }
    }
    @Test
    fun ff_02_yield() {
        val l = lexer("yield() thus { }")
        val parser = Parser(l)
        val e = parser.expr()
        //assert(e.tostr() == "((yield(nil)) thus { ceu_11 =>\n" +
        //        "nil\n" +
        //        "})") { e.tostr() }
        assert(e.tostr() == "do {\n" +
                "(val (it) = yield())\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ff_05_thus() {
        val l = lexer("f() thus { it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (it) = f())\n" +
                "it\n" +
                "}") { e.tostr() }
    }
    /*
    @Test
    fun ff_04_detrack() {
        val l = lexer("detrack(nil) { x }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(detrack(nil) { it =>\n" +
                "x\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun ff_06_detrack() {
        val l = lexer("detrack(nil) { x1 => nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(detrack(nil) { x1 =>\n" +
                "nil\n" +
                "})") { e.tostr() }
    }
     */

    // CATCH

    @Test
    fun fg_00_a_catch_err() {
        val l = lexer("catch (x:s) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x :s | is'(x,:s)) {\n" +
                "\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : invalid condition")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : innocuous identifier")
    }
    @Test
    fun fg_00_b_catch_err() {
        val l = lexer("catch :x:s {}")
        val parser = Parser(l)
        //parser.expr()
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected \"{\" : have \":s\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expression error : innocuous expression")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : invalid condition")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected \"{\" : have \":s\"")
    }
    @Test
    fun fg_01_catch() {
        val l = lexer("catch () {}")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 8) : expected expression : have \")\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 8) : pattern error : unexpected \")\"")
        val e = parser.expr()
        assert(e.tostr() == "catch (it | true) {\n" + "\n" + "}") { e.tostr() }
    }
    @Test
    fun fg_02_catch() {
        val l = lexer("catch {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (ceu_5 | true) {\n" +
                "\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_03_catch() {
        val l = lexer("catch (x:X | x) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x :X | do {\n" +
                "(val (ceu_47) = is'(x,:X))\n" +
                "if ceu_47 {\n" +
                "x\n" +
                "} else {\n" +
                "ceu_47\n" +
                "}\n" +
                "}) {\n" +
                "\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_04_catch() {
        val l = lexer("catch (x:X) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x :X | is'(x,:X)) {\n" +
                "\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 8) : catch error : innocuous identifier")
    }
    @Test
    fun fg_05_catch() {
        val l = lexer("catch (x|z) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x | z) {\n" +
                "\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_06_catch() {
        val l = lexer("catch (:X|z) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (it :X | do {\n" +
                "(val (ceu_46) = is'(it,:X))\n" +
                "if ceu_46 {\n" +
                "z\n" +
                "} else {\n" +
                "ceu_46\n" +
                "}\n" +
                "}) {\n" +
                "\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_07_catch() {
        val l = lexer("catch :X {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (it :X | is'(it,:X)) {\n" +
                "\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_08_catch() {
        val l = lexer("catch (|x) {}")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 9) : invalid pattern : expected \",\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : innocuous identifier")
        val e = parser.expr()
        assert(e.tostr() == "catch (it | x) {\n" +
                "\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_09_catch() {
        val l = lexer("catch (|it>1) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (it | {{>}}(it,1)) {\n" +
                "\n" +
                "}") { e.tostr() }
    }

    // RESUME-YIELD-ALL

    @Test
    fun gg_01_resume_yield_all_err() {
        val l = lexer("resume-yield-all f")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 18) : resume-yield-all error : expected call")
    }
    @Test
    fun gg_02_resume_yield_all_err() {
        val l = lexer("resume-yield-all f(1,2)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 18) : resume-yield-all error : invalid number of arguments")
    }
    @Test
    fun gg_03_resume_yield_all() {
        val l = lexer("resume-yield-all f()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            (val (ceu_co_11) = f)
            (var (ceu_arg_11) = nil)
            (var (ceu_v_11))
            loop {
            (set ceu_v_11 = (resume (ceu_co_11)(ceu_arg_11)))
            (break(ceu_v_11) if {{==}}(status(ceu_co_11),:terminated))
            (set ceu_arg_11 = yield(ceu_v_11))
            }
            }
        """.trimIndent()) { e.tostr() }
    }
    @Test
    fun todo_gg_04_resume_err() {   // TODO: multi args should be allowed
        val l = tst_03.lexer("""
            resume-yield-all nil(1,2)
        """)
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 2, col 30) : resume-yield-all error : invalid number of arguments")
    }

    // SPAWN

    @Test
    fun hh_01_spawn_task() {
        val l = lexer("spawn {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(spawn (task :nested () {\n" +
                "\n" +
                "})())") { e.tostr() }
    }
    @Test
    fun hh_02_spawn_coro() {
        val l = lexer("spawn coro {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \"(\" : have \"{\"")
        //val e = parser.expr()
        //assert(e.tostr() == "spawn (coro () {\n" +
        //        "nil\n" +
        //        "})()") { e.tostr() }
    }
    @Test
    fun hh_03_bcast_in() {
        val l = lexer("""
            spawn {
                broadcast(nil) in nil
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(spawn (task :nested () {\n" +
                "broadcast'(nil,nil)\n" +
                "})())") { e.tostr() }
    }

    // PAR / PAR-OR

    @Test
    fun ii_01_par() {
        val l = lexer("par {} with {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(spawn (task :nested () {\n" +
                "\n" +
                "})())\n" +
                "(spawn (task :nested () {\n" +
                "\n" +
                "})())\n" +
                "loop {\n" +
                "yield()\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ii_04_par_err() {
        val l = lexer("par {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected \"with\" : have end of file")
    }

    // AWAIT

    @Test
    fun ja_00_await_err() {
        val l = lexer("await")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected expression : have end of file")
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected \"{\" : have end of file")
    }
    @Test
    fun ja_04_await_err() {
        val l = lexer("await (x:X)")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(var (x :X))\n" +
                "loop {\n" +
                "(set x = yield())\n" +
                "(break if is'(x,:X))\n" +
                "}\n" +
                "delay\n" +
                "x\n" +
                "}") { e.tostr() }
    }
    @Test
    fun TODO_ja_10_await_err() {    // EOF msg
        val l = lexer("await x")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : expected \"{\" : have end of file")
    }
    @Test
    fun ja_05_task_err() {
        val l = lexer("await spawn T() in ts")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_16) = (spawn T() in ts))\n" +
                "if {{/=}}(status(ceu_16),:terminated) {\n" +
                "do {\n" +
                "(var (it))\n" +
                "loop {\n" +
                "(set it = yield())\n" +
                "(break if {{==}}(it,ceu_16))\n" +
                "}\n" +
                "delay\n" +
                "it\n" +
                "}\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "ceu_16.pub\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : await error : expected non-pool spawn")
    }
    @Test
    fun ja_06_task() {
        val l = lexer("await spawn T()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_13) = (spawn T()))\n" +
                "if {{/=}}(status(ceu_13),:terminated) {\n" +
                "do {\n" +
                "(var (it))\n" +
                "loop {\n" +
                "(set it = yield())\n" +
                "(break if {{==}}(it,ceu_13))\n" +
                "}\n" +
                "delay\n" +
                "it\n" +
                "}\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "ceu_13.pub\n" +
                "}") { e.tostr() }
    }

    // WATCHING

    @Test
    fun jc_01_watching() {
        val l = lexer("watching")
        val parser = Parser(l)
        //parser.expr()
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected \"{\" : have end of file")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected expression : have end of file")
    }
    @Test
    fun TODO_jc_02_watching_err() {
        val l = lexer("watching(x)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \"{\" : have end of file")
    }
    @Test
    fun jc_04_watching_err() {
        val l = lexer("watching(nil)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 14) : expected \"{\" : have end of file")
    }
    @Test
    fun TODO_jc_07_watching() { // 2x it is? :E
        val l = lexer("watching :E { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr().contains("(break if is'(it,:E))")) { e.tostr() }
    }

    // CLOCK

    @Test
    fun jd_01_clock_err() {
        val l = lexer("""
            spawn {
                await 2:ms
            }
        """)
        val parser = Parser(l)
        //parser.expr()
        assert(trap { parser.expr() } == "anon : (lin 3, col 24) : expected \"{\" : have \":ms\"")
    }
    @Test
    fun jd_02_clock_err() {
        val l = lexer("""
            spawn {
                await <10:min, 10:z>
            }
        """)
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 3, col 34) : invalid clock unit : unexpected \":z\"")
    }
    @Test
    fun jd_03_clock() {
        val l = lexer("""
            spawn {
                await <10:min, x:s>
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(spawn (task :nested () {\n" +
                "do {\n" +
                "(var (ceu_clk_16) = {{+}}({{*}}(10,60000),{{*}}(x,1000)))\n" +
                "loop {\n" +
                "(val (ceu_evt_16 :Clock) = yield())\n" +
                "if is'(ceu_evt_16,:Clock) {\n" +
                "(set ceu_clk_16 = {{-}}(ceu_clk_16,ceu_evt_16[:ms]))\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "(break if {{<=}}(ceu_clk_16,0))\n" +
                "}\n" +
                "delay\n" +
                "}\n" +
                "})())") { e.tostr() }
    }

    // TOGGLE

    @Test
    fun kk_01_toggle_err() {
        val l = lexer("toggle f()")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 10) : expected expression : have \")\"")
    }
    @Test
    fun kk_02_toggle_err() {
        val l = lexer("toggle v {")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 10) : expected \"(\" : have \"{\"")
    }
    @Test
    fun kk_03_toggle_err() {
        val l = lexer("toggle :v {")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \"}\" : have end of file")
    }
    @Test
    fun TODO_kk_04_toggle() {
        val l = lexer("toggle :v {}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        assert(out.contains("TODO")) { out }
    }

    // METHODS

    @Test
    fun oo_01_method() {
        val l = lexer("10->f()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10)") { e.tostr() }
    }
    @Test
    fun oo_02_method() {
        val l = lexer("10->f")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10)") { e.tostr() }
    }
    @Test
    fun oo_03_method() {
        val l = lexer("10->f(20)")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10,20)") { e.tostr() }
    }
    @Test
    fun oo_04_method() {
        val l = lexer("f() <- 10")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10)") { e.tostr() }
    }
    @Test
    fun oo_05_method() {
        val l = lexer("f<-10")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10)") { e.tostr() }
    }
    @Test
    fun oo_06_method() {
        val l = lexer("f(10)<-(20)")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10,20)") { e.tostr() }
    }
    @Test
    fun oo_07_method() {
        val l = lexer("(10->f)<-20")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10,20)") { e.tostr() }
    }
    @Test
    fun oo_08_method() {
        val l = lexer("(func() {}) <- 20")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(func () {\n" +
                "\n" +
                "})(20)") { e.tostr() }
    }
    @Test
    fun oo_09_method_err() {
        val out = test("""
            10->10
        """)
        assert(out == " |  anon : (lin 2, col 17) : 10(10)\n" +
                " v  call error : expected function\n") { out }
    }

    // PIPE / WHERE

    @Test
    fun op_01_pipe() {
        val l = lexer("10-->f()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10)") { e.tostr() }
    }
    @Test
    fun op_02_pipe() {
        val l = lexer("10-->f->g")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "g(10,f)") { e.tostr() }
    }
    @Test
    fun op_03_pipe() {
        val l = lexer("10-->(f<--20)")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "f(10,20)") { e.tostr() }
    }
    @Test
    fun op_04_pipe_where_err() {
        val l = lexer("10+1 --> f where { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "f({{+}}(10,1))\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 12) : sufix operation error : expected surrounding parentheses")
    }
    @Test
    fun todo_op_05_where() {    // export (not do)
        val l = lexer("10+1 where { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "export [] {\n" +
                "nil\n" +
                "{{+}}(10,1)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun TODO_col_op_06_where() {
        val l = lexer("spawn T(v) where {nil}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(trap { parser.expr() } == "anon : (lin 1, col XX) : spawn error : expected call")
    }
    @Test
    fun op_07_pipe() {
        val l = lexer("""
            do f<--10->g
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(do f(g(10)))") { e.tostr() }
    }

    // CAST

    @Test
    fun oq_01_cast() {
        val l = lexer("v.(:X).x")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_8 :X) = v)\n" +
                "ceu_8[:x]\n" +
                "}") { e.tostr() }
    }

    // LAMBDA

    @Test
    fun pp_01_lambda() {
        val l = lexer("\\{}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(func (it) {\n" +
                "\n" +
                "})") { e.tostr() }
    }
    @Test
    fun pp_02_lambda() {
        val l = lexer("\\{it}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(func (it) {\n" +
                "it\n" +
                "})") { e.tostr() }
    }
    @Test
    fun pp_03_lambda() {
        val l = lexer("\\{,:X => it}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(func (it :X) {\n" +
                "it\n" +
                "})") { e.tostr() }
    }
    @Test
    fun todo_LIN_COL_pp_04_lambda_err() {
        val l = lexer("\\{,v :X}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "anon : (lin X, col Y) : expression error : innocuous expression\n") { e.tostr() }
    }

    // TUPLE DOT

    @Test
    fun tt_01_dot() {
        val l = lexer("x.1")
        val parser = Parser(l)
        //val e = parser.expr()
        //assert(e.tostr() == "x[1]") { e.tostr() }
        assert(trap { parser.expr() } == "anon : (lin 1, col 3) : expected identifier : have \"1\"")
    }
    @Test
    fun tt_02_dot_err() {
        val l = lexer("x.1.2")
        val parser = Parser(l)
        //val e = parser.expr()
        //assert(e.tostr() == "x.1.2") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 3) : index error : ambiguous dot : use brackets")
        assert(trap { parser.expr() } == "anon : (lin 1, col 3) : expected identifier : have \"1.2\"")
    }
    @Test
    fun tt_03_dot() {
        val l = lexer("x . a")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Index && e.col is Expr.Acc && e.idx is Expr.Tag)
        assert(e.tostr() == "x[:a]") { e.tostr() }
    }
    @Test
    fun tt_04_dot_err() {
        val l = lexer("x . .")
        val parser = Parser(l)
        //assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected identifier : have \".\"")
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : invalid field : unexpected \".\"")
    }

    // CONSTRUCTOR

    @Test
    fun uu_01_cons() {
        val l = lexer(":T []")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "tag(:T,[])") { e.tostr() }
    }
    @Test
    fun uu_02_cons_err() {
        val l = lexer(":T 2")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 1) : expression error : innocuous expression")
    }
    @Test
    fun uu_03_tags() {
        val l = lexer("val x = :T [1]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(val (x :T) = tag(:T,[1]))") { e.tostr() }
    }
    @Test
    fun uu_04_tags() {
        val l = lexer("val (x,y) = (:T [1], :U [2])")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(val (x :T,y :U) = tag(:T,[1]),tag(:U,[2]))") { e.tostr() }
    }

    // PPP: PEEK, PUSH, POP

    @Test
    fun vv_01_ppp() {
        val l = lexer("x[0][=]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_13) = x[0])\n" +
                "```/* = */```\n" +
                "ceu_13[{{-}}({{#}}(ceu_13),1)]\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_02_ppp() {
        val l = lexer("set x.y[=] = 10")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_14) = x[:y])\n" +
                "(set ceu_14[{{-}}({{#}}(ceu_14),1)] = 10)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_03_ppp() {
        val l = lexer("x()[-]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_x_12) = x())\n" +
                "```/* - */```\n" +
                "do {\n" +
                "(val (ceu_y_12) = ceu_x_12[{{-}}({{#}}(ceu_x_12),1)])\n" +
                "(set ceu_x_12[{{-}}({{#}}(ceu_x_12),1)] = nil)\n" +
                "ceu_y_12\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_04_ppp() {
        val l = lexer("set x[0][-] = 1")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : set error : expected assignable destination")
    }
    @Test
    fun vv_05_ppp() {
        val l = lexer("t[+]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_8) = t)\n" +
                "```/* + */```\n" +
                "ceu_8[{{#}}(ceu_8)]\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_06_ppp() {
        val l = lexer("set t.x[+] = 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val (ceu_14) = t[:x])\n" +
                "(set ceu_14[{{#}}(ceu_14)] = 1)\n" +
                "}") { e.tostr() }
    }

    // DATA

    @Test
    fun xx_01_data() {
        val l = lexer("""
            data :A = [] {
                :B = [] {
                    :C = []
                }
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(data :A = [])\n" +
                "(data :A.B = [])\n" +
                "(data :A.B.C = [])\n" +
                "}") { e.tostr() }
    }

    // PATT

    @Test
    fun ww_01_patt() {
        val l = lexer(
            """
            ()
        """
        )
        val parser = Parser(l)
        val p = parser.patt()
        assert(Patts_Any_tostr(p) == "(it | true)") { Patts_Any_tostr(p) }
    }
    @Test
    fun ww_02_patt() {
        val l = lexer(
            """
            (it)
        """
        )
        val parser = Parser(l)
        val p = parser.patt()
        assert(Patts_Any_tostr(p) == "(it | true)") { Patts_Any_tostr(p) }
    }
    @Test
    fun ww_03_patt() {
        val l = lexer("""
            (it|)
        """)
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 2, col 17) : expected expression : have \")\"")
    }
    @Test
    fun ww_04_patt() {
        val l = lexer("""
            (|)
        """)
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 2, col 15) : expected expression : have \")\"")
    }
    @Test
    fun ww_05_patt() {
        val l = lexer("""
            (|true)
        """)
        val parser = Parser(l)
        val p = parser.patt()
        assert(Patts_Any_tostr(p) == "(it | true)") { Patts_Any_tostr(p) }
    }
}
