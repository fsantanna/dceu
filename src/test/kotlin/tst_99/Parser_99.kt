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
        assert(e.tostr() == "((1) thus { ceu_5 =>\n" +
                "if ceu_5 {\n" +
                "ceu_5\n" +
                "} else {\n" +
                "2\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun bb_02_bin_and() {
        N = 1
        val l = lexer("1 and 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((1) thus { ceu_5 =>\n" +
                "if ceu_5 {\n" +
                "2\n" +
                "} else {\n" +
                "ceu_5\n" +
                "}\n" +
                "})\n") { e.tostr() }
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
            ((((if true {
            false
            } else {
            true
            }) thus { ceu_27 =>
            if ceu_27 {
            false
            } else {
            ceu_27
            }
            })
            ) thus { ceu_77 =>
            if ceu_77 {
            ceu_77
            } else {
            true
            }
            })
            
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
            a or ((b or c) or d)
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == """
            ((a) thus { ceu_5 =>
            if ceu_5 {
            ceu_5
            } else {
            ((((b) thus { ceu_10 =>
            if ceu_10 {
            ceu_10
            } else {
            c
            }
            })
            ) thus { ceu_45 =>
            if ceu_45 {
            ceu_45
            } else {
            d
            }
            })
            
            }
            })
            
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
        assert(e.tostr() == "((1) thus { x =>\n" +
                "if x {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n\n") { e.tostr() }
    }
    @Test
    fun dd_03_if() {
        val l = lexer("if x:X=1 { x }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "((1) thus { x :X =>\n" +
                "if x {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n\n") { e.tostr() }
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
    fun de_0x_if_err() {
        val l = lexer("if false => 1 --> nil => 2")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "ERR") { e.tostr() }
    }
    @Test
    fun de_0x_if() {
        val l = lexer("if false => 1 --> nil nil")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "OK") { e.tostr() }
    }

    // IFS

    @Test
    fun ee_01_ifs() {
        val l = lexer("ifs { a=>1 else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((nil) thus { ceu_5 =>\n" +
                "if a {\n" +
                "1\n" +
                "} else {\n" +
                "if true {\n" +
                "0\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun ee_02_ifs() {
        val l = lexer("ifs { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((nil) thus { ceu_5 =>\n" +
                "nil\n" +
                "})\n") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected expression : have \"}\"")
    }
    @Test
    fun ee_03_ifs() {
        val l = lexer("ifs it=nil { else => it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((nil) thus { it =>\n" +
                "if true {\n" +
                "it\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n") { e.tostr() }
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
        assert(e.tostr() == "((v) thus { it =>\n" +
                "if a {\n" +
                "it\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun ee_06_ifs() {
        val l = lexer("ifs it=v { a{1} b=>it else{0} }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((v) thus { it =>\n" +
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
                "})\n") { e.tostr() }
    }
    @Test
    fun ee_07_ifs() {
        val l = lexer("ifs { f() => nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((nil) thus { ceu_5 =>\n" +
                "if f() {\n" +
                "nil\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n") { e.tostr() }
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

    // LOOP / ITER / WHILE / UNTIL

    @Test
    fun ef_01_iter() {
        val l = lexer("loop x in f { x }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_14 = iter(f))\n" +
                "loop {\n" +
                "(break if ((ceu_14[0](ceu_14)) thus { x =>\n" +
                "if {{==}}(x,nil) {\n" +
                "true\n" +
                "} else {\n" +
                "x\n" +
                "}\n" +
                "})\n" +
                ")\n" +
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
        assert(e.tostr() == """
            do {
            (val ceu_ste_23 = x)
            (var i = {{+}}(0,0))
            (val ceu_lim_23 = n)
            loop {
            (break(nil) if {{>=}}(i,ceu_lim_23))
            nil
            (set i = {{+}}(i,ceu_ste_23))
            }
            }
            """.trimIndent()) { e.tostr() }
    }


    // AS / YIELD / CATCH / DETRACK / THUS

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
        assert(e.tostr() == "((yield(nil)) thus { ceu_11 =>\n" +
                "nil\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun ff_03_catch() {
        val l = lexer("catch {} in {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "catch { ceu_5 => nil } in {\n" +
                "nil\n" +
                "}") { e.tostr() }
    }
    @Test
    fun ff_04_detrack() {
        val l = lexer("detrack(nil) as { x }")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "((detrack(nil)) thus { it =>\n" +
                "if it {\n" +
                "x\n" +
                "} else {\n" +
                "nil\n" +
                "}\n" +
                "})\n\n") { e.tostr() }
    }
    @Test
    fun ff_05_thus() {
        val l = lexer("f() thus { it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((f()) thus { it =>\n" +
                "it\n" +
                "})\n") { e.tostr() }
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
            if (({{/=}}(status(ceu_co_10),:terminated)) thus { ceu_56 =>
            if ceu_56 {
            ceu_56
            } else {
            {{/=}}(ceu_v_10,nil)
            }
            })
             {
            (set ceu_arg_10 = yield(drop(ceu_v_10)))
            } else {
            nil
            }
            (break if {{==}}(status(ceu_co_10),:terminated))
            }
            ceu_arg_10
            }
        """.trimIndent())
    }

    // SPAWN

    @Test
    fun hh_01_spawn_task() {
        val l = lexer("spawn task {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(spawn (task () :void {\n" +
                "nil\n" +
                "})(nil))") { e.tostr() }
    }
    @Test
    fun todo_hh_02_spawn_coro() {
        val l = lexer("spawn coro {}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "spawn (coro () {\n" +
                "nil\n" +
                "})(nil)") { e.tostr() }
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
                "})(nil))\n" +
                "(spawn (task () :void {\n" +
                "nil\n" +
                "})(nil))\n" +
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
            })(nil)))
            (val ceu_1_9 = (spawn (task () :void {
            nil
            })(nil)))
            loop {
            (break if (((({{==}}(status(ceu_0_9),:terminated)) thus { ceu_111 =>
            if ceu_111 {
            ((pub(ceu_0_9)) thus { ceu_120 =>
            if ceu_120 {
            ceu_120
            } else {
            true
            }
            })
            
            } else {
            ceu_111
            }
            })
            ) thus { ceu_238 =>
            if ceu_238 {
            ceu_238
            } else {
            (((({{==}}(status(ceu_1_9),:terminated)) thus { ceu_257 =>
            if ceu_257 {
            ((pub(ceu_1_9)) thus { ceu_266 =>
            if ceu_266 {
            ceu_266
            } else {
            true
            }
            })
            
            } else {
            ceu_257
            }
            })
            ) thus { ceu_384 =>
            if ceu_384 {
            ceu_384
            } else {
            false
            }
            })
            
            }
            })
            )
            yield(nil)
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
                "})(nil)))\n" +
                "(val ceu_1_9 = (spawn (task () :void {\n" +
                "nil\n" +
                "})(nil)))\n" +
                "loop {\n" +
                "(break(nil) if (({{==}}(status(ceu_0_9),:terminated)) thus { ceu_114 =>\n" +
                "if ceu_114 {\n" +
                "(({{==}}(status(ceu_1_9),:terminated)) thus { ceu_131 =>\n" +
                "if ceu_131 {\n" +
                "true\n" +
                "} else {\n" +
                "ceu_131\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "} else {\n" +
                "ceu_114\n" +
                "}\n" +
                "})\n" +
                ")\n" +
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
    fun jj_00_await_err() {
        val l = lexer("await")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected \"(\" : have end of file")
    }
    @Test
    fun jj_01_await() {
        val l = lexer("await()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "(break if ((yield(nil)) thus { ceu_5 =>\n" +
                "ceu_5\n" +
                "})\n" +
                ")\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jj_02_await() {
        val l = lexer("await(x)")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "(break if ((yield(nil)) thus { ceu_5 =>\n" +
                "is'(ceu_5,x)\n" +
                "})\n" +
                ")\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jj_03_await() {
        val l = lexer("await as { it }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "(break if ((yield(nil)) thus { it =>\n" +
                "it\n" +
                "})\n" +
                ")\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jj_04_await() {
        val l = lexer("await(:X) as { x:X => x.x>2 }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "loop {\n" +
                "(break if ((yield(nil)) thus { x :X =>\n" +
                "((is'(x,:X)) thus { ceu_68 =>\n" +
                "if ceu_68 {\n" +
                "{{>}}(x[:x],2)\n" +
                "} else {\n" +
                "ceu_68\n" +
                "}\n" +
                "})\n" +
                "\n" +
                "})\n" +
                ")\n" +
                "}") { e.tostr() }
    }
    @Test
    fun jj_05_task_err() {
        val l = lexer("await spawn T() in ts")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : await error : expected non-pool spawn")
    }
    @Test
    fun jj_06_task() {
        val l = lexer("await spawn T()")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "do {\n" +
                "(val ceu_14 = (spawn T(nil)))\n" +
                "loop {\n" +
                "(break(pub(ceu_14)) if {{==}}(status(ceu_14),:terminated))\n" +
                "yield(nil)\n" +
                "}\n" +
                "}") { e.tostr() }
    }

    // WATCHING

    @Test
    fun kk_01_watching() {
        val l = lexer("watching")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected expression : have end of file")
    }
    @Test
    fun kk_02_watching_err() {
        val l = lexer("watching(x)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \"{\" : have end of file")
    }
    @Test
    fun kk_03_watching_err() {
        val l = lexer("watching as {}")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 15) : expected \"{\" : have end of file")
    }
    @Test
    fun kk_04_watching_err() {
        val l = lexer("watching() as { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 22) : expected \"{\" : have end of file")
    }
    @Test
    fun kk_05_watching() {
        val l = lexer("watching as {} { }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr().contains("(break if (((({{==}}(status(ceu_0_85),:terminated))")) { e.tostr() }
    }
    @Test
    fun kk_06_watching() {
        val l = lexer("watching() as {x=>nil} { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr().contains("(break if ((yield(nil)) thus { x =>")) { e.tostr() }
    }
    @Test
    fun kk_07_watching() {
        val l = lexer("watching :E { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr().contains("is'(ceu_5,:E)")) { e.tostr() }
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
        assert(e.tostr() == "((x[0]) thus { ceu_12 =>\n" +
                "``` /* = */```\n" +
                "ceu_12[{{-}}({{#}}(ceu_12),1)]\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun vv_02_ppp() {
        val l = lexer("set x.y[=] = 10")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((x[:y]) thus { ceu_14 =>\n" +
                "(set ceu_14[{{-}}({{#}}(ceu_14),1)] = 10)\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun vv_03_ppp() {
        val l = lexer("x()[-]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((x()) thus { ceu_x_11 =>\n" +
                "``` /* - */```\n" +
                "((ceu_x_11[{{-}}({{#}}(ceu_x_11),1)]) thus { ceu_y_11 =>\n" +
                "(set ceu_x_11[{{-}}({{#}}(ceu_x_11),1)] = nil)\n" +
                "ceu_y_11\n" +
                "})\n" +
                "\n" +
                "})\n") { e.tostr() }
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
        assert(e.tostr() == "((t) thus { ceu_8 =>\n" +
                "``` /* + */```\n" +
                "ceu_8[{{#}}(ceu_8)]\n" +
                "})\n") { e.tostr() }
    }
    @Test
    fun vv_06_ppp() {
        val l = lexer("set t.x[+] = 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "((t[:x]) thus { ceu_14 =>\n" +
                "(set ceu_14[{{#}}(ceu_14)] = 1)\n" +
                "})\n") { e.tostr() }
    }
}
