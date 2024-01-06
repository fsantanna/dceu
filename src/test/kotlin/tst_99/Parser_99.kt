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
        assert(e.tostr() == "do {\nnil\n}") { e.tostr() }
    }
    @Test
    fun aa_03_empty_func() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.args.size==0)
        assert(e.tostr() == "(func () {\nnil\n})") { e.tostr() }
    }
    @Test
    fun aa_04_empty_loop() {
        val l = lexer("loop { }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e.tostr() == "loop {\nnil\n}") { e.tostr() }
    }

    // OPS: not, and, or

    @Test
    fun bb_01_bin_or() {
        N = 1
        val l = lexer("1 or 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if 1 {\n" +
                "1\n" +
                "} else {\n" +
                "2\n" +
                "}") { e.tostr() }
    }
    @Test
    fun bb_02_bin_and() {
        N = 1
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if 1 {\n" +
                "2\n" +
                "} else {\n" +
                "1\n" +
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
            (pass do {
            (pass if true {
            false
            } else {
            true
            })
            do (ceu_27) {
            if ceu_27 {
            false
            } else {
            ceu_27
            }
            }
            })
            do (ceu_124) {
            if ceu_124 {
            ceu_124
            } else {
            true
            }
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
            (pass ```a```)
            do (ceu_5) {
            if ceu_5 {
            ceu_5
            } else {
            do {
            (pass do {
            (pass ```b```)
            do (ceu_10) {
            if ceu_10 {
            ceu_10
            } else {
            ```c```
            }
            }
            })
            do (ceu_79) {
            if ceu_79 {
            ceu_79
            } else {
            ```d```
            }
            }
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
        assert(e.tostr() == "(val f = (func () {\nnil\n}))\n") { e.tostr() }
    }
    @Test
    fun cc_02_func_rec_err() {
        val l = lexer("func :rec () {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 11) : expected identifier : have \"(\"")
    }
    @Test
    fun cc_03_func_rec() {
        val l = lexer("func :rec f () {}")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "export [f] {\n" +
                "(var f)\n" +
                "(set f = (func () {\n" +
                "nil\n" +
                "}))\n" +
                "}\n") { e.tostr() }
    }

    // IF / ID-TAG

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
                "(pass 1)\n" +
                "do (x) {\n" +
                "if x {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n" +
                "}\n") { e.tostr() }
    }
    @Test
    fun dd_03_if() {
        val l = lexer("if x:X=1 { x }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "do {\n" +
                "(pass 1)\n" +
                "do (x :X) {\n" +
                "if x {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n" +
                "}\n") { e.tostr() }
    }
    @Test
    fun dd_04_if() {
        val l = lexer("if f() {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if f() {\n" +
                "nil\n" +
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
                "(pass nil)\n" +
                "do (ceu_5) {\n" +
                "if a {\n" +
                "1\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
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
                "(pass nil)\n" +
                "do (ceu_5) {\n" +
                "nil\n" +
                "}\n" +
                "}") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }
    @Test
    fun ee_03_ifs() {
        val l = lexer("ifs it=nil { else => it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass nil)\n" +
                "do (it) {\n" +
                "if true {\n" +
                "it\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
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
        val l = lexer("ifs it=v { a => it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass v)\n" +
                "do (it) {\n" +
                "if a {\n" +
                "it\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ee_06_ifs() {
        val l = lexer("ifs it=v { a{1} b=>it else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass v)\n" +
                "do (it) {\n" +
                "if a {\n" +
                "1\n" +
                "} else {\n" +
                "if b {\n" +
                "it\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
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
                "(pass nil)\n" +
                "do (ceu_5) {\n" +
                "if f() {\n" +
                "nil\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
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
        assert(trap { parser.expr() } == "anon : (lin 3, col 17) : case error : expected ifs condition")
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
        assert(trap { parser.expr() } == "anon : (lin 4, col 21) : case error : expected ifs condition")
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
                "(pass 1)\n" +
                "do (it) {\n" +
                "1\n" +
                "}\n" +
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
                "(pass 1)\n" +
                "do (it) {\n" +
                "x\n" +
                "}\n" +
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
                "(pass 1)\n" +
                "do (x) {\n" +
                "nil\n" +
                "}\n" +
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
                "(pass 1)\n" +
                "do (it) {\n" +
                "nil\n" +
                "}\n" +
                "}\n") { e.tostr() }
    }
    @Test
    fun oo_05_thus_thus() {
        val l = tst_04.lexer("""
            1 thus { it => 2 } thus { it => 3 }
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        //assert(e.tostr() == "((((1) thus { it =>\n" +
        //        "2\n" +
        //        "})) thus { it =>\n" +
        //        "3\n" +
        //        "})\n") { e.tostr() }
        //assert(e.tostr() == "(func (it) {\n" +
        //        "3\n" +
        //        "})((func (it) {\n" +
        //        "2\n" +
        //        "})(1))\n") { e.tostr() }
        assert(e.tostr() == "do {\n" +
                "(pass do {\n" +
                "(pass 1)\n" +
                "do (it) {\n" +
                "2\n" +
                "}\n" +
                "})\n" +
                "do (it) {\n" +
                "3\n" +
                "}\n" +
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
                "(pass yield(1))\n" +
                "do (it) {\n" +
                "nil\n" +
                "}\n" +
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
                "(pass yield(2))\n" +
                "do (it) {\n" +
                "nil\n" +
                "}\n" +
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
                "(val ceu_14 = iter(f))\n" +
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
                "(val ceu_ste_23 = x)\n" +
                "(var i = {{+}}(0,0))\n" +
                "(val ceu_lim_23 = n)\n" +
                "loop {\n" +
                "(break(false) if {{>=}}(i,ceu_lim_23))\n" +
                "nil\n" +
                "(set i = {{+}}(i,ceu_ste_23))\n" +
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
                "(val ceu_ste_18 = 1)\n" +
                "(var it = {{+}}(1,0))\n" +
                "(val ceu_lim_18 = 10)\n" +
                "loop {\n" +
                "(break(false) if {{>}}(it,ceu_lim_18))\n" +
                "nil\n" +
                "(set it = {{+}}(it,ceu_ste_18))\n" +
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
                "(pass yield(nil))\n" +
                "do (ceu_11) {\n" +
                "nil\n" +
                "}\n" +
                "}") { e.tostr() }
    }
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
    fun ff_05_thus() {
        val l = lexer("f() thus { it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass f())\n" +
                "do (it) {\n" +
                "it\n" +
                "}\n" +
                "}") { e.tostr() }
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

    // CATCH

    @Test
    fun fg_00_a_catch_err() {
        val l = lexer("catch x:s {}")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : invalid condition")
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : innocuous identifier")
    }
    @Test
    fun fg_00_b_catch_err() {
        val l = lexer("catch :x:s {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : catch error : invalid condition")
    }
    @Test
    fun fg_01_catch() {
        val l = lexer("catch () {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (ceu_8 => true) {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_02_catch() {
        val l = lexer("catch {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (ceu_6 => true) {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_03_catch() {
        val l = lexer("catch (x:X => x) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x :X => do {\n" +
                "(pass is'(x,:X))\n" +
                "do (ceu_49) {\n" +
                "if ceu_49 {\n" +
                "x\n" +
                "} else {\n" +
                "ceu_49\n" +
                "}\n" +
                "}\n" +
                "}) {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_04_catch() {
        val l = lexer("catch (x:X) {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : catch error : innocuous identifier")
    }
    @Test
    fun fg_05_catch() {
        val l = lexer("catch x=>z {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (x => z) {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_06_catch() {
        val l = lexer("catch (:X=>z) {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (it :X => do {\n" +
                "(pass is'(it,:X))\n" +
                "do (ceu_48) {\n" +
                "if ceu_48 {\n" +
                "z\n" +
                "} else {\n" +
                "ceu_48\n" +
                "}\n" +
                "}\n" +
                "}) {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_07_catch() {
        val l = lexer("catch :X {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (ceu_8 :X => is'(ceu_8,:X)) {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_08_catch() {
        val l = lexer("catch x {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (ceu_8 => is'(ceu_8,x)) {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun fg_09_catch() {
        val l = lexer("catch it>1 {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch (it => {{>}}(it,1)) {\n" +
                "nil\n" +
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
            (pass {{/=}}(status(ceu_co_10),:terminated))
            do (ceu_56) {
            if ceu_56 {
            ceu_56
            } else {
            {{/=}}(ceu_v_10,nil)
            }
            }
            } {
            (set ceu_arg_10 = yield(drop(ceu_v_10)))
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
        val l = lexer("spawn task {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(spawn (task () :void {\n" +
                "nil\n" +
                "})())") { e.tostr() }
    }
    @Test
    fun todo_hh_02_spawn_coro() {
        val l = lexer("spawn coro {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "spawn (coro () {\n" +
                "nil\n" +
                "})()") { e.tostr() }
    }
    @Test
    fun hh_03_bcast_in() {
        val l = lexer("""
            spawn task {
                broadcast(nil) in nil
            }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(spawn (task () :void {\n" +
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
                "(spawn (task () :void {\n" +
                "nil\n" +
                "})())\n" +
                "(spawn (task () :void {\n" +
                "nil\n" +
                "})())\n" +
                "loop {\n" +
                "yield(nil)\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ii_02_paror() {
        val l = lexer("par-or {} with {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            do {
            (val ceu_0_9 = (spawn (task () :void {
            nil
            })()))
            (val ceu_1_9 = (spawn (task () :void {
            nil
            })()))
            loop {
            (break if do {
            (pass do {
            (pass {{==}}(status(ceu_0_9),:terminated))
            do (ceu_107) {
            if ceu_107 {
            do {
            (pass pub(ceu_0_9))
            do (ceu_116) {
            if ceu_116 {
            ceu_116
            } else {
            true
            }
            }
            }
            } else {
            ceu_107
            }
            }
            })
            do (ceu_359) {
            if ceu_359 {
            ceu_359
            } else {
            do {
            (pass do {
            (pass {{==}}(status(ceu_1_9),:terminated))
            do (ceu_378) {
            if ceu_378 {
            do {
            (pass pub(ceu_1_9))
            do (ceu_387) {
            if ceu_387 {
            ceu_387
            } else {
            true
            }
            }
            }
            } else {
            ceu_378
            }
            }
            })
            do (ceu_630) {
            if ceu_630 {
            ceu_630
            } else {
            false
            }
            }
            }
            }
            }
            })
            yield(nil)
            nil
            }
            }
        """.trimIndent()) { e.tostr() }
    }
    @Test
    fun ii_03_parand() {
        val l = lexer("par-and {} with {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_0_9 = (spawn (task () :void {\n" +
                "nil\n" +
                "})()))\n" +
                "(val ceu_1_9 = (spawn (task () :void {\n" +
                "nil\n" +
                "})()))\n" +
                "loop {\n" +
                "(break(nil) if do {\n" +
                "(pass {{==}}(status(ceu_0_9),:terminated))\n" +
                "do (ceu_110) {\n" +
                "if ceu_110 {\n" +
                "do {\n" +
                "(pass {{==}}(status(ceu_1_9),:terminated))\n" +
                "do (ceu_127) {\n" +
                "if ceu_127 {\n" +
                "true\n" +
                "} else {\n" +
                "ceu_127\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "} else {\n" +
                "ceu_110\n" +
                "}\n" +
                "}\n" +
                "})\n" +
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
    fun ja_01_await() {
        val l = lexer("await ()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if false {\n" +
                "nil\n" +
                "} else {\n" +
                "loop {\n" +
                "(break if do {\n" +
                "(pass yield(nil))\n" +
                "do (ceu_5) {\n" +
                "if ceu_5 {\n" +
                "ceu_5\n" +
                "} else {\n" +
                "true\n" +
                "}\n" +
                "}\n" +
                "})\n" +
                "}\n" +
                "delay\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ja_02_await() {
        val l = lexer("await {:a}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(!out.contains("await-chk"))
        assert(out.contains("do (it) {"))
        assert(out.contains("do {\n:a\n}"))
    }
    @Test
    fun ja_03_await() {
        val l = lexer("await (x:X => z)")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(out.contains("(break if do {"))
        assert(out.contains("(pass yield(nil))"))
        assert(out.contains("do (x :X) {"))
        assert(out.contains("await-chk(__x,:X)")) { out }
        assert(out.contains("if ceu_58 {\nz\n}"))
    }
    @Test
    fun ja_04_await_err() {
        val l = lexer("await (x:X)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : await error : innocuous identifier")
    }
    @Test
    fun ja_04_await() {
        val l = lexer("await x:X {x}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(out.contains("(break if do {"))
        assert(out.contains("await-chk(__x,:X)"))
        assert(out.contains("do {\nx\n}"))
    }
    @Test
    fun ja_05_await() {
        val l = lexer("await (x=>z)")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(out.contains("(break if do {")) { out }
        assert(!out.contains("await-chk"))
        assert(out.contains("if z {\nawait-ret(x)\n} else {\nz\n}")) { out }
    }
    @Test
    fun ja_06_await() {
        val l = lexer("await (:X=>z) {:a}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        assert(out.contains("await-chk(__it,:X)")) { out }
        assert(out.contains("if ceu_61 {\nz\n} else {\nceu_61\n}\n")) { out }
        assert(out.contains("do {\n:a\n}")) { out }
    }
    @Test
    fun ja_07_await() {
        val l = lexer("await :X {:a}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(out.contains("(break if do {")) { out }
        assert(out.contains("(pass yield(nil))")) { out }
        assert(out.contains("do (it :X) {")) { out }
        assert(out.contains("await-chk(__it,:X)"))
        assert(out.contains("do {\n:a\n}"))
    }
    @Test
    fun ja_08_await() {
        val l = lexer("await x {:a}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(out.contains("(break if do {\n" +
                "(pass yield(nil))\n" +
                "do (it) {")) { out }
        assert(out.contains("await-chk(__it,x)"))
        assert(out.contains("do {\n:a\n}"))
    }
    @Test
    fun ja_09_await() {
        val l = lexer("await it>1 {:a}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(out.contains("(break if do {\n" +
                "(pass yield(nil))\n" +
                "do (it) {")) { out }
        assert(!out.contains("await-chk"))
        assert(out.contains("{{>}}(it,1)"))
        assert(out.contains("do {\n:a\n}"))
    }
    @Test
    fun ja_10_await_err() {
        val l = lexer("await x")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : expected \"{\" : have end of file")
    }
    @Test
    fun ja_11_await() {
        val l = lexer("await(x)")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(out.contains("(break if do {\n" +
                "(pass yield(nil))\n" +
                "do (ceu_5) {")) { out }
        assert(out.contains("await-chk(__ceu_5,x)")) { out }
        assert(out.contains("if ceu_289 {\nawait-ret(ceu_5)\n} else {\nceu_289\n}")) { out }
    }
    @Test
    fun ja_05_task_err() {
        val l = lexer("await spawn T() in ts")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : await error : expected non-pool spawn")
    }
    @Test
    fun ja_06_task() {
        val l = lexer("await spawn T()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_12 = (spawn T()))\n" +
                "loop {\n" +
                "(break(pub(ceu_12)) if {{==}}(status(ceu_12),:terminated))\n" +
                "yield(nil)\n" +
                "}\n" +
                "}") { e.tostr() }
    }

    // EVERY

    @Test
    fun jb_01_every() {
        val l = lexer("every :X {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if false {\n" +
                "nil\n" +
                "} else {\n" +
                "loop {\n" +
                "(break if do {\n" +
                "(pass yield(nil))\n" +
                "do (it :X) {\n" +
                "do {\n" +
                "(pass await-chk(__it,:X))\n" +
                "do (ceu_56) {\n" +
                "if ceu_56 {\n" +
                "loop {\n" +
                "nil\n" +
                "(break(false) if true)\n" +
                "}\n" +
                "} else {\n" +
                "ceu_56\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "})\n" +
                "}\n" +
                "delay\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jb_02_every() {
        val l = lexer("every (x:X) { println(x) }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if false {\n" +
                "nil\n" +
                "} else {\n" +
                "loop {\n" +
                "(break if do {\n" +
                "(pass yield(nil))\n" +
                "do (x :X) {\n" +
                "do {\n" +
                "(pass await-chk(__x,:X))\n" +
                "do (ceu_64) {\n" +
                "if ceu_64 {\n" +
                "loop {\n" +
                "println(x)\n" +
                "(break(false) if true)\n" +
                "}\n" +
                "} else {\n" +
                "ceu_64\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "})\n" +
                "}\n" +
                "delay\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jb_03_every() {
        val l = lexer("every :X { until true }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if false {\n" +
                "nil\n" +
                "} else {\n" +
                "loop {\n" +
                "(break if do {\n" +
                "(pass yield(nil))\n" +
                "do (it :X) {\n" +
                "do {\n" +
                "(pass await-chk(__it,:X))\n" +
                "do (ceu_58) {\n" +
                "if ceu_58 {\n" +
                "loop {\n" +
                "(break if true)\n" +
                "(break(false) if true)\n" +
                "}\n" +
                "} else {\n" +
                "ceu_58\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "})\n" +
                "}\n" +
                "delay\n" +
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
    fun jc_02_watching_err() {
        val l = lexer("watching(x)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \"{\" : have end of file")
    }
    @Test
    fun jc_03_watching_err() {
        val l = lexer("watching {}")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(!out.contains("await-chk"))
        assert(out.contains("(pass yield(nil))\n" +
                "do (ceu_5) {")) { out }
        assert(out.contains("if ceu_5 {\nceu_5\n} else {\ntrue\n}"))
    }
    @Test
    fun jc_04_watching_err() {
        val l = lexer("watching(nil)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 14) : expected \"{\" : have end of file")
    }
    @Test
    fun jc_05_watching() {
        val l = lexer("watching () { }")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(!out.contains("await-chk"))
        assert(out.contains("(pass yield(nil))\n" +
                "do (ceu_5) {")) { out }
        assert(out.contains("if ceu_5 {\nceu_5\n} else {\ntrue\n}"))
    }
    @Test
    fun jc_06_watching() {
        val l = lexer("watching (x=>y) { z }")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        //println(out)
        assert(!out.contains("await-chk"))
        assert(e.tostr().contains("(break if do {\n" +
                "(pass yield(nil))\n" +
                "do (x) {")) { e.tostr() }
    }
    @Test
    fun jc_07_watching() {
        val l = lexer("watching :E { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr().contains("await-chk(__ceu_5,:E)")) { e.tostr() }
    }

    // CLOCK

    @Test
    fun jd_01_clock_err() {
        val l = lexer("""
            spawn task {
                await :2:ms
            }
        """)
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \"{\" : have end of file")
        assert(trap { parser.expr() } == "anon : (lin 4, col 13) : expected tag : have \"}\"")
    }
    @Test
    fun jd_02_clock() {
        val l = lexer("await (:1:h :10:min :30:s :239:s)")
        val parser = Parser(l)
        val e = parser.expr()
        val out = e.tostr()
        println(out)
        assert(out.contains("var ceu_clk_5 = {{+}}({{*}}(1,3600000),{{+}}({{*}}(10,60000),{{+}}({{*}}(30,1000),{{*}}(239,1000)))))"))
        assert(out.contains("do (ceu_5 :Clock) {"))
        assert(out.contains("await-chk(__ceu_5,:Clock)"))
        assert(out.contains("{{>}}(ceu_clk_5,0)")) { out }
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
        //println(out)
        assert(out.contains("await-chk(__it,:v)"))
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
                "nil\n" +
                "})(20)") { e.tostr() }
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
    fun op_05_where() {
        val l = lexer("10+1 where { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "export [] {\n" +
                "nil\n" +
                "{{+}}(10,1)\n" +
                "}") { e.tostr() }
    }
    @Test
    fun todo_col_op_06_where() {
        val l = lexer("spawn T(v) where {nil}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(trap { parser.expr() } == "anon : (lin 1, col XX) : spawn error : expected call")
    }

    // CAST

    @Test
    fun oq_01_cast() {
        val l = lexer("v.(:X).x")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass v)\n" +
                "do (ceu_8 :X) {\n" +
                "ceu_8[:x]\n" +
                "}\n" +
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
        val e = parser.expr()
        assert(e.tostr() == "x[1]") { e.tostr() }
    }
    @Test
    fun tt_02_dot_err() {
        val l = lexer("x.1.2")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 3) : index error : ambiguous dot : use brackets")
    }

    // CONSTRUCTOR

    @Test
    fun uu_01_cons() {
        val l = lexer(":T []")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "tags([],:T,true)") { e.tostr() }
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
        val l = lexer("x.0[=]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass x[0])\n" +
                "do (ceu_12) {\n" +
                "```/* = */```\n" +
                "ceu_12[{{-}}({{#}}(ceu_12),1)]\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_02_ppp() {
        val l = lexer("set x.y[=] = 10")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass x[:y])\n" +
                "do (ceu_14) {\n" +
                "(set ceu_14[{{-}}({{#}}(ceu_14),1)] = 10)\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_03_ppp() {
        val l = lexer("x()[-]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass x())\n" +
                "do (ceu_x_11) {\n" +
                "```/* - */```\n" +
                "do {\n" +
                "(pass ceu_x_11[{{-}}({{#}}(ceu_x_11),1)])\n" +
                "do (ceu_y_11) {\n" +
                "(set ceu_x_11[{{-}}({{#}}(ceu_x_11),1)] = nil)\n" +
                "ceu_y_11\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_04_ppp() {
        val l = lexer("set x.0[-] = 1")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : set error : expected assignable destination")
    }
    @Test
    fun vv_05_ppp() {
        val l = lexer("t[+]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass t)\n" +
                "do (ceu_8) {\n" +
                "```/* + */```\n" +
                "ceu_8[{{#}}(ceu_8)]\n" +
                "}\n" +
                "}") { e.tostr() }
    }
    @Test
    fun vv_06_ppp() {
        val l = lexer("set t.x[+] = 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(pass t[:x])\n" +
                "do (ceu_14) {\n" +
                "(set ceu_14[{{#}}(ceu_14)] = 1)\n" +
                "}\n" +
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
