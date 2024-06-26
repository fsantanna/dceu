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
        assert(e is Expr.Do && e.es.size==1)
        assert(e.tostr() == "do {\n(do nil)\n}") { e.tostr() }
    }
    @Test
    fun aa_03_empty_func() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.args.size==0)
        assert(e.tostr() == "(func () {\n(do nil)\n})") { e.tostr() }
    }
    @Test
    fun aa_04_empty_loop() {
        val l = lexer("loop { }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e.tostr() == "loop {\n(do nil)\n}") { e.tostr() }
    }

    // OPS: not, and, or

    @Test
    fun bb_01_bin_or() {
        N = 1
        val l = lexer("1 or 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_5 = 1)\n" +
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
                "(val ceu_5 = 1)\n" +
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
            (val ceu_74 = do {
            (val ceu_27 = if true {
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
            if ceu_74 {
            ceu_74
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
            (val ceu_5 = ```a```)
            if ceu_5 {
            ceu_5
            } else {
            do {
            (val ceu_42 = do {
            (val ceu_10 = ```b```)
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
        assert(e.tostr() == "(val f = (func () {\n(do nil)\n}))\n") { e.tostr() }
    }
    @Test
    fun cc_02_func_rec_err() {
        val l = lexer("func :rec () {}")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(func :rec () {\n(do nil)\n})\n") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected identifier : have \"(\"")
    }
    @Test
    fun cc_03_func_rec() {
        val l = lexer("func :rec f () {}")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "export [f] {\n" +
                "(var f)\n" +
                "(set f = (func () {\n" +
                "(do nil)\n" +
                "}))\n" +
                "}\n") { e.tostr() }
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
                "(do nil)\n" +
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
                "if a {\n" +
                "1\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "(do nil)\n" +
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
                "(do nil)\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }
    @Test
    fun ee_03_ifs() {
        val l = lexer("ifs nil { else => it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_16 = nil)\n" +
                "if true {\n" +
                "it\n" +
                "} else {\n" +
                "(do nil)\n" +
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
        val l = lexer("ifs v { a => it }")
        val parser = Parser(l)
        //val e = parser.expr()
        //assert(e.tostr() == "}") { e.tostr() }
        assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected \",\" : have \"=>\"")
    }
    @Test
    fun ee_06_ifs() {
        val l = lexer("ifs v { (,a) {1} (,b)=>v else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_38 = v)\n" +
                "(val it = ceu_38)\n" +
                "if a {\n" +
                "1\n" +
                "} else {\n" +
                "(val it = ceu_38)\n" +
                "if b {\n" +
                "v\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "(do nil)\n" +
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
                "if f() {\n" +
                "nil\n" +
                "} else {\n" +
                "(do nil)\n" +
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
                "if {{==}}(20) {\n" +
                "nil\n" +
                "} else {\n" +
                "(do nil)\n" +
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
            val x = ifs 20 {
                true => ifs {
                    == 20 => true   ;; err: no ifs expr
                }
            }
            println(x)
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(val x = do {\n" +
                "(val ceu_70 = 20)\n" +
                "(val it = ceu_70)\n" +
                "if {{===}}(it,true) {\n" +
                "do {\n" +
                "if {{==}}(20) {\n" +
                "true\n" +
                "} else {\n" +
                "(do nil)\n" +
                "}\n" +
                "}\n" +
                "} else {\n" +
                "(do nil)\n" +
                "}\n" +
                "})") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 4, col 21) : case error : expected ifs condition")
    }
    @Test
    fun ee_10_ifs() {
        val l = lexer("ifs v { x, => 10 }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_21 = v)\n" +
                "(val x = ceu_21)\n" +
                "if true {\n" +
                "10\n" +
                "} else {\n" +
                "(do nil)\n" +
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
                "(val it = 1)\n" +
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
                "(val it = 1)\n" +
                "x\n" +
                "}") { e.tostr() }
    }
    @Test
    fun oo_03_thus_err() {
        val l = tst_03.lexer(
            """
            1 thus { x => }
        """
        )
        val parser = Parser(l)
        val e = parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 2, col 27) : expected expression : have \"}\"")
        assert(e.tostr() == "do {\n" +
                "(val x = 1)\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun oo_04_thus() {
        val l = tst_04.lexer("""
            1 thus { it => nil }
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        //assert(e.tostr() == "((1) thus { it =>\nnil\n})\n") { e.tostr() }
        assert(e.tostr() == "do {\n" +
                "(val it = 1)\n" +
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
            { it => nil }
        """.trimIndent()
        )
        val parser = Parser(l)
        val e = parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : yield error : line break before expression")
        //assert(e.tostr() == "((yield(1)) thus { it =>\nnil\n})") { e.tostr() }
        assert(e.tostr() == "do {\n" +
                "(val it = yield(1))\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun oo_08_coro() {
        val l = tst_03.lexer(
            """
            set t = coro (v) {
                set v = yield((1)) ;;thus { it:X => it }
                yield((2)) thus { it => nil }
            }
            coroutine(t)
            set v = resume a(1)
            resume a(2)
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(set t = (coro (v) {\n" +
                "(set v = yield(1))\n" +
                "do {\n" +
                "(val it = yield(2))\n" +
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
                "(val ceu_14 = to-iter(f))\n" +
                "loop {\n" +
                "(val x = ceu_14[0](ceu_14))\n" +
                "(break(false) if {{==}}(x,nil))\n" +
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
                "(val ceu_ste_24 = x)\n" +
                "(var i = {{+}}(0,0))\n" +
                "(val ceu_lim_24 = n)\n" +
                "loop {\n" +
                "(break(false) if {{>=}}(i,ceu_lim_24))\n" +
                "(do nil)\n" +
                "(set i = {{+}}(i,ceu_ste_24))\n" +
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
                "(val ceu_ste_20 = 1)\n" +
                "(var it = {{+}}(1,0))\n" +
                "(val ceu_lim_20 = 10)\n" +
                "loop {\n" +
                "(break(false) if {{>}}(it,ceu_lim_20))\n" +
                "(do nil)\n" +
                "(set it = {{+}}(it,ceu_ste_20))\n" +
                "}\n" +
                "}") { e.tostr() }
    }

    // AS / YIELD / DETRACK / THUS

    @Test
    fun ff_01_yield() {
        val l = lexer("yield()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "yield(nil)") { e.tostr() }
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
                "(val ceu_11 = yield(nil))\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ff_05_thus() {
        val l = lexer("f() thus { it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val it = f())\n" +
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
        assert(e.tostr() == "catch (x :s, is'(x,:s)) {\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : invalid condition")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : innocuous identifier")
    }
    @Test
    fun fg_00_b_catch_err() {
        val l = lexer("catch :x:s {}")
        val parser = Parser(l)
        //parser.expr()
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expression error : innocuous expression")
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
        assert(e.tostr() == "catch (it, true) {\n" + "(do nil)\n" + "}") { e.tostr() }
    }
    @Test
    fun fg_02_catch() {
        val l = lexer("catch {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (ceu_5, true) {\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_03_catch() {
        val l = lexer("catch (x:X, x) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x :X, do {\n" +
                "(val ceu_33 = is'(x,:X))\n" +
                "if ceu_33 {\n" +
                "x\n" +
                "} else {\n" +
                "ceu_33\n" +
                "}\n" +
                "}) {\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_04_catch() {
        val l = lexer("catch (x:X) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x :X, is'(x,:X)) {\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 8) : catch error : innocuous identifier")
    }
    @Test
    fun fg_05_catch() {
        val l = lexer("catch (x,z) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x, z) {\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_06_catch() {
        val l = lexer("catch (:X,z) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (it :X, do {\n" +
                "(val ceu_32 = is'(it,:X))\n" +
                "if ceu_32 {\n" +
                "z\n" +
                "} else {\n" +
                "ceu_32\n" +
                "}\n" +
                "}) {\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_07_catch() {
        val l = lexer("catch :X {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (it :X, is'(it,:X)) {\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_08_catch() {
        val l = lexer("catch (,x) {}")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 9) : invalid pattern : expected \",\"")
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : innocuous identifier")
        val e = parser.expr()
        assert(e.tostr() == "catch (it, x) {\n" +
                "(do nil)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_09_catch() {
        val l = lexer("catch (,it>1) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (it, {{>}}(it,1)) {\n" +
                "(do nil)\n" +
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
            (val ceu_co_10 = f)
            (var ceu_arg_10 = nil)
            loop {
            (val ceu_v_10 = (resume (ceu_co_10)(ceu_arg_10)))
            if do {
            (val ceu_56 = {{/=}}(status(ceu_co_10),:terminated))
            if ceu_56 {
            ceu_56
            } else {
            {{/=}}(ceu_v_10,nil)
            }
            } {
            (set ceu_arg_10 = yield(ceu_v_10))
            } else {
            nil
            }
            (break if {{==}}(status(ceu_co_10),:terminated))
            }
            ceu_arg_10
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
                "(do nil)\n" +
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
                "(do nil)\n" +
                "})())\n" +
                "(spawn (task :nested () {\n" +
                "(do nil)\n" +
                "})())\n" +
                "loop {\n" +
                "yield(nil)\n" +
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
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected expression : have end of file")
    }
    @Test
    fun ja_04_await_err() {
        val l = lexer("await (x:X)")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(var x :X)\n" +
                "loop {\n" +
                "(set x = yield(nil))\n" +
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
                "(val ceu_15 = (spawn T() in ts))\n" +
                "if {{/=}}(status(ceu_15),:terminated) {\n" +
                "do {\n" +
                "(var it)\n" +
                "loop {\n" +
                "(set it = yield(nil))\n" +
                "(break if {{==}}(it,ceu_15))\n" +
                "}\n" +
                "delay\n" +
                "it\n" +
                "}\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "ceu_15.pub\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : await error : expected non-pool spawn")
    }
    @Test
    fun ja_06_task() {
        val l = lexer("await spawn T()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_12 = (spawn T()))\n" +
                "if {{/=}}(status(ceu_12),:terminated) {\n" +
                "do {\n" +
                "(var it)\n" +
                "loop {\n" +
                "(set it = yield(nil))\n" +
                "(break if {{==}}(it,ceu_12))\n" +
                "}\n" +
                "delay\n" +
                "it\n" +
                "}\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "ceu_12.pub\n" +
                "}") { e.tostr() }
    }

    // WATCHING

    @Test
    fun jc_01_watching() {
        val l = lexer("watching")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected expression : have end of file")
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
                await :2:ms
            }
        """)
        val parser = Parser(l)
        //parser.expr()
        //assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \"{\" : have end of file")
        //assert(trap { parser.expr() } == "anon : (lin 4, col 13) : expected tag : have \"}\"")
        assert(trap { parser.expr() } == "anon : (lin 4, col 13) : expected \"{\" : have \"}\"")
    }
    @Test
    fun jd_02_clock_err() {
        val l = lexer("""
            spawn {
                await :10:min:10:ms
            }
        """)
        val parser = Parser(l)
        //val e = parser.expr()
        //println(e.tostr())
        //assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \"{\" : have end of file")
        //assert(trap { parser.expr() } == "anon : (lin 4, col 13) : expected tag : have \"}\"")
        assert(trap { parser.expr() } == "anon : (lin 4, col 13) : expected \"{\" : have \"}\"")
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
    fun kk_04_toggle() {
        val l = lexer("toggle :v {}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        assert(out.contains("(set it = yield(nil))\n" +
                "(break if do {\n" +
                "(val ceu_149 = is'(it,:v))")) { out }
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
                "(do nil)\n" +
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
        assert(trap { parser.expr() } == "anon : (lin 1, col 12) : sufix operation error : expected surrounding parentheses")
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
                "(val ceu_8 :X = v)\n" +
                "ceu_8[:x]\n" +
                "}") { e.tostr() }
    }

    // LAMBDA

    @Test
    fun pp_01_lambda() {
        val l = lexer("\\{}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(func (ceu_5) {\n" +
                "nil\n" +
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
        val l = lexer("\\{:X => it}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(func (it :X) {\n" +
                "it\n" +
                "})") { e.tostr() }
    }
    @Test
    fun todo_LIN_COL_pp_04_lambda_err() {
        val l = lexer("\\{v :X}")
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
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected identifier : have \".\"")
    }

    // CONSTRUCTOR

    @Test
    fun uu_01_cons() {
        val l = lexer(":T []")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "tag([],:T,true)") { e.tostr() }
    }
    @Test
    fun uu_02_cons_err() {
        val l = lexer(":T 2")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 1) : expression error : innocuous expression")
    }

    // PPP: PEEK, PUSH, POP

    @Test
    fun vv_01_ppp() {
        val l = lexer("x[0][=]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_13 = x[0])\n" +
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
                "(val ceu_14 = x[:y])\n" +
                "(set ceu_14[{{-}}({{#}}(ceu_14),1)] = 10)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_03_ppp() {
        val l = lexer("x()[-]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_x_11 = x())\n" +
                "```/* - */```\n" +
                "do {\n" +
                "(val ceu_y_11 = ceu_x_11[{{-}}({{#}}(ceu_x_11),1)])\n" +
                "(set ceu_x_11[{{-}}({{#}}(ceu_x_11),1)] = nil)\n" +
                "ceu_y_11\n" +
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
                "(val ceu_8 = t)\n" +
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
                "(val ceu_14 = t[:x])\n" +
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
}
