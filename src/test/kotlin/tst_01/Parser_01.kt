package tst_01

import dceu.*
import org.junit.Test

class Parser_01 {
    // VAR

    @Test
    fun aa_01_var () {
        val l = lexer(" x ")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Acc && e.tk.str == "x")
    }
    @Test
    fun aa_02_var_err () {
        val l = lexer(" { ")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 2) : expected expression : have \"{\"")
    }
    @Test
    fun aa_03_var_err () {
        val l = lexer("  ")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 3) : expected expression : have end of file")
    }
    @Test
    fun aa_04_evt () {
        val l = lexer(" evt ")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Acc && e.tk.str == "evt")
    }
    @Test
    fun aa_05_err () {
        val l = lexer(" err ")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Acc && e.tk.str == "err")
    }

    // PARENS

    @Test
    fun bb_01_expr_parens() {
        val l = lexer(" ( a ) ")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Acc && e.tk.str == "a")
    }
    @Test
    fun bb_02_expr_parens_err() {
        val l = lexer(" ( a  ")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 7) : expected \")\" : have end of file")
    }
    @Test
    fun bb_03_op_prec_err() {
        val l = lexer("println(2 * 3 - 1)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 15) : binary operation error : expected surrounding parentheses")
    }
    @Test
    fun bb_04_op_prec_ok() {
        val l = lexer("println(2 * (3 - 1))")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "println({{*}}(2,{{-}}(3,1)))")
    }

    // NUM / NIL / BOOL

    @Test
    fun cc_01_num() {
        val l = lexer(" 1.5F ")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }
    @Test
    fun cc_02_nil() {
        val l = lexer("nil")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Nil && e.tk.str == "nil")
    }
    @Test
    fun cc_03_true() {
        val l = lexer("true")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Bool && e.tk.str == "true")
    }
    @Test
    fun cc_04_false() {
        val l = lexer("false")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Bool && e.tk.str == "false")
    }
    @Test
    fun cc_05_char() {
        val l = lexer("'x'")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Char && e.tk.str == "'x'")
    }

    // CALL

    @Test
    fun dd_01_call() {
        val l = lexer(" f (1.5F, x) ")
        val parser = Parser(l)
        val e = parser.expr_4_suf()
        assert(e is Expr.Call && e.tk.str=="f" && e.clo is Expr.Acc && e.args.size==2)
    }
    @Test
    fun dd_02_call() {
        val l = lexer(" f() ")
        val parser = Parser(l)
        val e = parser.expr_4_suf()
        assert(e is Expr.Call && e.clo.tk.str=="f" && e.clo is Expr.Acc && e.args.size==0)
    }
    @Test
    fun dd_03_call() {
        val l = lexer(" f(x,8)() ")
        val parser = Parser(l)
        val e = parser.expr_4_suf()
        assert(e is Expr.Call && e.clo is Expr.Call && e.args.size==0)
        assert(e.tostr() == "f(x,8)()")
    }
    @Test
    fun dd_04_call_err() {
        val l = lexer("f (999 ")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 8) : expected \",\" : have end of file")
    }
    @Test
    fun dd_05_call_err() {
        val l = lexer(" f ({ ")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected expression : have \"{\"")
    }

    // TUPLE

    @Test
    fun ee_01_tuple() {
        val l = lexer(" [ 1.5F, x] ")
        val parser = Parser(l)
        val e = parser.expr_4_suf()
        assert(e is Expr.Tuple && e.args.size==2)
    }
    @Test
    fun ee_02_tuple() {
        val l = lexer("[[],[1,2,3]]")
        val parser = Parser(l)
        val e = parser.expr_4_suf()
        assert(e.tostr() == "[[],[1,2,3]]")
    }
    @Test
    fun ee_03_tuple_err() {
        val l = lexer("[{")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 2) : expected expression : have \"{\"")
    }
    @Test
    fun ee_04_tuple() {
        val l = lexer("[1.5F,] ")
        val parser = Parser(l)
        val e = parser.expr_4_suf()
        assert(e is Expr.Tuple && e.args.size==1)
    }

    // DICT

    @Test
    fun dict1() {
        val l = lexer(" @[ (1,x) , (:number,2) ] ")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Dict && e.args.size==2)
    }
    @Test
    fun dict2() {
        val l = lexer("@[(:dict,@[]), (:tuple,[1,2,3])]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "@[(:dict,@[]),(:tuple,[1,2,3])]")
    }
    @Test
    fun dict3_err() {
        val l = lexer("@[({")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 4) : expected expression : have \"{\"")
    }
    @Test
    fun dict4_err() {
        val l = lexer("@[(")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 4) : expected expression : have end of file")
    }
    @Test
    fun dict5_err() {
        val l = lexer("@[(1,{")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected expression : have \"{\"")
    }
    @Test
    fun dict6() {
        val l = lexer("@[(1.5F,1),] ")
        val parser = Parser(l)
        val e = parser.expr_4_suf()
        assert(e is Expr.Dict && e.args.size==1)
    }
    @Test
    fun dict7_err() {
        val l = lexer("@[(1,1]")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected \")\" : have \"]\"")
    }

    // VECTOR

    @Test
    fun vector1() {
        val l = lexer(" #[ 1,x , :number,2 ] ")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Vector && e.args.size==4)
    }
    @Test
    fun vector2() {
        val l = lexer("#[:dict,#[], :tuple,[1,2,3]]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "#[:dict,#[],:tuple,[1,2,3]]")
    }
    @Test
    fun vector3_err() {
        val l = lexer("#[({")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 4) : expected expression : have \"{\"")
    }
    @Test
    fun vector4() {
        val l = lexer("v[{{#}}(v)]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "v[{{#}}(v)]") { e.tostr() }
    }

    // INDEX

    @Test
    fun index1() {
        val l = lexer("x[10]")
        val parser = Parser(l)
        val e = parser.expr_4_suf()
        assert(e is Expr.Index && e.col is Expr.Acc && e.idx is Expr.Num)
    }
    @Test
    fun index2_err() {
        val l = lexer("x[10")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected \"]\" : have end of file")
    }
    @Test
    fun index4_err() {
        val l = lexer("x . a")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "x[:a]") { e.tostr() }
        //assert(trap { parser.exprSufs() } == "anon : (lin 1, col 5) : expected \"pub\" : have \"a\"")
    }
    @Test
    fun index5_err() {
        val l = lexer("x . .")
        val parser = Parser(l)
        //assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected identifier : have \".\"")
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : invalid field : unexpected \".\"")
    }
    @Test
    fun index6_err() {
        val l = lexer("x . 2")
        val parser = Parser(l)
        assert(trap { parser.expr_4_suf() } == "anon : (lin 1, col 5) : expected identifier : have \"2\"")
    }

    // EXPRS

    @Test
    fun exprs_call() {
        val l = lexer("f ()")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.size==1 && es[0] is Expr.Call && es[0].tostr() == "f()")
        assert(es.tostr() == "f();\n")
    }
    @Test
    fun exprs_call_err() {
        val l = lexer("f")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.tostr() == "f;\n")
    }

    // EXPRS

    @Test
    fun exprs_seq1() {
        val l = lexer("; f () ; g () h()\ni() ;\n;")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.tostr() == "f();\ng();\nh();\ni();\n") { es.tostr() }
    }
    @Test
    fun exprs_seq2() {
        val l = lexer("; f () \n (1) ; h()\ni() ;\n;")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.tostr() == "f();\n1;\nh();\ni();\n") { es.tostr() }
        //assert(trap { parser.exprs() } == "anon : (lin 2, col 3) : expression error : innocuous expression")
    }
    @Test
    fun exprs_seq2a() {
        val l = lexer("; f () \n ;;;do;;; (1) ; h()\ni() ;\n;")
        val parser = Parser(l)
        // TODO: ambiguous
        val es = parser.exprs()
        assert(es.tostr() == "f();\n1;\nh();\ni();\n") { es.tostr() }
        //assert(ceu.trap { parser.exprs() } == "anon : (lin 2, col 3) : call error : \"(\" in the next line")
    }
    @Test
    fun exprs_seq3() {
        val l = lexer("var v2\n[tp,v1,v2]")
        val parser = Parser(l)
        // TODO: ambiguous
        val es = parser.exprs()
        assert(es.tostr() == "(var v2);\n[tp,v1,v2];\n") { es.tostr() }
    }

    // DCL

    @Test
    fun expr_dcl_var() {
        val l = lexer("var x")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Dcl && e.tk.str=="var" && e.idtag.first.str=="x")
        assert(e.tostr() == "(var x)")
    }
    @Test
    fun expr_dcl_val() {
        val l = lexer("val x")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Dcl && e.tk.str=="val" && e.idtag.first.str=="x")
        assert(e.tostr() == "(val x)")
    }
    @Test
    fun expr_dcl_err() {
        val l = lexer("var [10]")
        val parser = Parser(l)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 5) : expected identifier : have \"[\"")
    }
    @Test
    fun expr_dcl3() {
        val l = lexer("var x = 1")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Dcl && e.idtag.first.str == "x" && e.src is Expr.Num)
        assert(e.tostr() == "(var x = 1)")
    }

    // SET

    @Test
    fun expr_set() {
        val l = lexer("set x = [10]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Set && e.tk.str == "set")
        assert(e.tostr() == "(set x = [10])") { e.tostr() }
    }
    @Test
    fun expr_err1() {  // set number?
        val l = lexer("set 1 = 1")
        val parser = Parser(l)
        //val e = parser.exprN()
        //assert(e.tostr() == "set 1 = 1")
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : set error : expected assignable destination")
    }
    @Test
    fun expr_err2() {  // set whole tuple?
        val l = lexer("set [1] = 1")
        val parser = Parser(l)
        //val e = parser.exprN()
        //assert(e.tostr() == "set [1] = 1")
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : set error : expected assignable destination")
    }
    @Test
    fun set_nil_err() {
        val l = lexer("set nil = nil")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : set error : expected assignable destination")
    }
    @Test
    fun set_if_err() {
        val l = lexer("set (if true {nil} else {nil}) = nil")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : set error : expected assignable destination")
    }
    @Test
    fun set_evt_err() {
        val l = lexer("set evt = nil")
        val parser = Parser(l)
        assert(parser.expr() is Expr.Set)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : set error : expected assignable destination")
    }
    @Test
    fun set_err_err() {
        val l = lexer("set err = nil")
        val parser = Parser(l)
        assert(parser.expr() is Expr.Set)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 1) : set error : expected assignable destination")
    }

    // IF

    @Test
    fun expr_if1() {  // set whole tuple?
        val l = lexer("if true { 1 } else { 0 }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1;\n} else {\n0;\n}") { e.tostr() }
    }
    @Test
    fun expr_if2_err() {
        val l = lexer("if true { 1 }")
        val parser = Parser(l)
        //val e = parser.expr_prim()
        //assert(e is Expr.If)
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 14) : expected \"else\" : have end of file")
    }
    @Test
    fun if3() {
        val l = lexer("if f() {nil} else {nil}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.If)
        assert(e.tostr() == "if f() {\nnil;\n} else {\nnil;\n}") { e.tostr() }
        //assert(trap { parser.expr_prim() } == "anon : (lin 2, col 16) : access error : variable \"f\" is not declared")
    }

    // DO

    @Test
    fun expr_do1_err() {
        val l = lexer("do{}")
        val parser = Parser(l)
        //val e = parser.expr()
        //assert(e is Expr.Do && e.es.isEmpty())
        //assert(e.tostr() == "do {\n\n}") { e.tostr() }
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 4) : expected expression : have \"}\"")
    }
    @Test
    fun expr_do2() {
        val l = lexer("do { var a; set a=1; print(a) }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Do && e.es.size==3)
        assert(e.tostr() == "do {\n(var a);\n(set a = 1);\nprint(a);\n}") { e.tostr() }
    }
    @Test
    fun expr_do3() {
        val l = lexer("do ;;;(a);;; { print(a) }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Do /*&& e.arg!=null*/)
        //assert(e.tostr() == "do (a) {\nprint(a)\n}") { e.tostr() }
        assert(e.tostr() == "do {\nprint(a);\n}") { e.tostr() }
    }
    @Test
    fun expr_do4() {
        val l = lexer("do { do { nil } ; val x ; val y }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Do)
        assert(e.tostr() == "do {\n" +
                "do {\n" +
                "nil;\n" +
                "};\n" +
                "(val x);\n" +
                "(val y);\n" +
                "}") { e.tostr() }
    }

    // FUNC

    @Test
    fun expr_func1_err() {
        val l = lexer("func () {nil}")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Proto)
        assert(e.tostr() == "(func () {\nnil;\n})") { e.tostr() }
        //assert(trap { parser.expr_prim() } == "anon : (lin 1, col 10) : expected expression : have \"}\"")
    }
    @Test
    fun expr_func2() {
        val l = lexer("func (a,b) { 10; }")
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Proto && e.pars.size==2)
        assert(e.tostr() == "(func (a,b) {\n10;\n})") { e.tostr() }
    }
    @Test
    fun pp_07_func_args_err() {
        val l = lexer("func (1) { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected identifier : have \"1\"")
    }
    @Test
    fun pp_12_func_nested() {
        val l = lexer(
            """
            func :nested () { nil }
        """
        )
        val parser = Parser(l)
        //val e = parser.expr()
        //assert(e.tostr() == "(func :nested () {\nnil\n})") { e.tostr() }
        assert(trap { parser.expr() } == "anon : (lin 2, col 18) : expected \"(\" : have \":nested\"")
    }
    @Test
    fun pp_13_minus() {
        val l = lexer("""
            val f = func (v) { -v }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(val f = (func (v) {\n" +
                "{{-}}(v);\n" +
                "}))") { e.tostr() }
    }

    // FUNC / :REC

    @Test
    fun pq_01_rec() {
        val l = lexer("""
            val f = func () { }
        """)
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 2, col 31) : expected expression : have \"}\"")
    }
    @Test
    fun pq_02_rec() {
        val l = lexer("""
            val f = func () { nil }
        """)
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "(val f = (func () {\n" +
                "nil;\n" +
                "}))") { e.tostr() }
    }

    // LOOP

    @Test
    fun qq_01_loop_err() {
        val l = lexer("loop { ;;;do;;; nil }")
        val parser = Parser(l)
        val e1 = parser.expr() as Expr.Loop
        assert(e1.blk.tostr() == "{\nnil;\n}") { e1.blk.tostr() }
    }
    @Test
    fun qq_02_loop_err() {
        val l = lexer("loop until {")
        val parser = Parser(l)
        assert(trap { parser.expr_1_bin() } == "anon : (lin 1, col 6) : expected \"{\" : have \"until\"")
        //assert(trap { parser.expr_1_bin() } == "anon : (lin 1, col 12) : expected expression : have \"{\"")
    }
    @Test
    fun qq_03_loop_err() {
        val l = tst_02.lexer("loop x { }")
        val parser = Parser(l)
        //assert(trap { parser.expr_prim() } == "anon : (lin 1, col 7) : invalid loop : unexpected x")
        assert(trap { parser.expr_prim() } == "anon : (lin 1, col 6) : expected \"{\" : have \"x\"")
    }

    // BREAK

    @Test
    fun rr_01_break_err() {
        val l = lexer("break")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected \"if\" : have end of file")
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : expected \"(\" : have end of file")
    }
    @Test
    fun rr_02_break_err() {
        val l = lexer("skip 1")
        val parser = Parser(l)
        val e = parser.expr() as Expr.Skip
        assert(e.tostr() == "skip") { e.tostr() }
        //assert(trap { parser.expr_1_bin() } == "anon : (lin 1, col 6) : expected \"if\" : have \"1\"")
    }
    @Test
    fun rr_03_break_err() {
        val l = lexer("break (1)")
        val parser = Parser(l)
        val e = parser.expr() as Expr.Break
        assert(e.tostr() == "break(1)") { e.tostr() }
        //assert(trap { parser.expr_1_bin() } == "anon : (lin 1, col 10) : expected \"if\" : have end of file")
    }
    @Test
    fun rr_04_break_err() {
        val l = lexer("break (1) if")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 13) : expected expression : have end of file")
        //assert(trap { parser.exprs() } == "anon : (lin 1, col 13) : expected expression : have end of file")
    }
    @Test
    fun rr_05_break() {
        val l = lexer("skip if true")
        val parser = Parser(l)
        //val e = parser.exprs()
        //assert(e.tostr() == "(skip if true)") { e.tostr() }
        assert(trap { parser.exprs() } == "anon : (lin 1, col 13) : expected \"{\" : have end of file")
    }
    @Test
    fun rr_06_break() {
        val l = lexer("break if nil")
        val parser = Parser(l)
        //val e = parser.expr() as Expr.Break
        //assert(e.tostr() == "(break if nil)") { e.tostr() }
        //assert(trap { parser.expr() } == "anon : (lin 1, col 20) : expected \"(\" : have end of file")
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected \"(\" : have \"if\"")
    }
    @Test
    fun rr_07_skip() {
        val l = lexer("skip ;;;if nil;;;")
        val parser = Parser(l)
        val e = parser.expr() as Expr.Skip
        assert(e.tostr() == "skip") { e.tostr() }
    }

    // NATIVE

    @Test
    fun native0() {
        val l = lexer("`a`")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "```a```") { e.tostr() }
    }
    @Test
    fun native1() {
        val l = lexer(
            """
            ```
                printf("xxx\n");
            ```
        """.trimIndent()
        )
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Nat)
        assert(e.tostr() == "```\n    printf(\"xxx\\n\");\n```") { "."+e.tostr()+"." }
    }
    @Test
    fun native2_err() {
        val l = lexer(
            """
            native ``
                printf("xxx\n");
            ```
        """.trimIndent()
        )
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 3, col 1) : native error : expected \"``\"")
    }
    @Test
    fun native3_err() {
        val l = lexer(
            """
            native ``
                printf("xxx\n");
        """
        )
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 4, col 9) : native error : expected \"``\"")
    }
    @Test
    fun native4_err() {
        val l = lexer(
            """
            native `:
        """.trimIndent()
        )
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : tag error : expected identifier")
    }
    @Test
    fun native5() {
        val l = lexer(
            """
            ```:ola```
        """.trimIndent()
        )
        val parser = Parser(l)
        val e = parser.expr_prim()
        assert(e is Expr.Nat)
        assert(e.tostr() == "```:ola ```") { "."+e.tostr()+"." }
    }

    // BINARY / UNARY / OPS

    @Test
    fun bin1_err() {
        val l = lexer("(10+)")
        val parser = Parser(l)
        assert(trap { parser.expr_1_bin() } == "anon : (lin 1, col 5) : expected expression : have \")\"")
    }
    @Test
    fun bin2() {
        val l = lexer("10+1")
        val parser = Parser(l)
        val e = parser.expr_1_bin()
        assert(e is Expr.Call)
        assert(e.tostr() == "{{+}}(10,1)") { e.tostr() }
    }
    @Test
    fun bin3() {
        val l = lexer("10/=1")
        val parser = Parser(l)
        val e = parser.expr_1_bin()
        assert(e is Expr.Call)
        assert(e.tostr() == "{{/=}}(10,1)") { e.tostr() }
    }
    @Test
    fun pre1() {
        val l = lexer("- - 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{{-}}({{-}}(1))") { e.tostr() }
    }
    @Test
    fun bin4() {
        val l = lexer("(1 + 2) + 3")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{{+}}({{+}}(1,2),3)") { e.tostr() }
    }
    @Test
    fun pre_pos1() {
        val l = lexer("-x[0]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "{{-}}(x[0])") { e.tostr() }
    }

    // TEMPLATE

    @Test
    fun tplate00() {
        val l = lexer(
            """
            data :T = [x,y]
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(data :T = [x,y]);\n") { e.tostr() }
    }
    @Test
    fun tplate01() {
        val l = lexer(
            """
            var t :T = [1,2]
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(var t :T = [1,2]);\n") { e.tostr() }
    }
    @Test
    fun tplate02_err() {
        val l = lexer(
            """
            data X [x,y]
        """
        )
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 18) : expected tag : have \"X\"")
    }
    @Test
    fun tplate03_err() {
        val l = lexer(
            """
            data :X [x,y]
        """
        )
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 21) : expected \"=\" : have \"[\"")
    }
    @Test
    fun tplate04_err() {
        val l = lexer(
            """
            data :X = nil
        """
        )
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected \"[\" : have \"nil\"")
    }
    @Test
    fun tplate05_err() {
        val l = lexer(
            """
            data :X = [1,2]
        """
        )
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 24) : expected identifier : have \"1\"")
    }
    @Test
    fun tplate06() {
        val l = lexer(
            """
            data :U = [t:T]
        """
        )
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "(data :U = [t :T]);\n") { e.tostr() }
    }

    // GROUP

    @Test
    fun tt_01_export_err() {
        val l = lexer("group {}")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected \"[\" : have \"{\"")
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : expected expression : have \"}\"")
    }
    @Test
    fun BUG_tt_02_export_err() {
        val l = lexer("group ;;;[:x];;; {}")
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected identifier : have \":x\"")
        assert(trap { parser.expr() } == "anon : (lin 1, col 19) : expected expression : have \"}\"")
    }
    @Test
    fun tt_03_export() {
        val l = lexer("group ;;;[];;; { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Group /*&& e.ids.isEmpty()*/ && e.es.size==1)
        assert(e.tostr() == "group {\nnil;\n}") { e.tostr() }
    }
    @Test
    fun tt_04_export() {
        val l = lexer("group ;;;[x];;; { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Group /*&& e.ids.first()=="x"*/ && e.es.size==1)
        //assert(e.tostr() == "group [x] {\nnil\n}") { e.tostr() }
        assert(e.tostr() == "group {\nnil;\n}") { e.tostr() }
    }
    @Test
    fun tt_05_export() {
        val l = lexer("group ;;;[x,y];;; { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Group /*&& e.ids.last()=="y"*/ && e.es.size==1 /*&& e.ids.size==2*/)
        //assert(e.tostr() == "group [x,y] {\nnil\n}") { e.tostr() }
        assert(e.tostr() == "group {\nnil;\n}") { e.tostr() }
    }

    // DASH

    @Test
    fun vv_01_dash_num() {
        val l = lexer("""
            val v-1
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "{{-}}((val v),1);\n") { e.tostr() }
    }
    @Test
    fun vv_02_dash_num() {
        val l = lexer("""
            println(:X-1)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "println({{-}}(:X,1));\n") { e.tostr() }
    }
    @Test
    fun vv_03_dash_let() {
        val l = lexer("""
            println(:X-a)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "println(:X-a);\n") { e.tostr() }
    }

    // INNOCUOUS

    @Test
    fun yy_01_innoc() {
        val l = lexer("""
            do {
                1
            }
            nil
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "do {\n1;\n};\nnil;\n") { e.tostr() }
        //assert(trap { parser.exprs() } == "anon : (lin 2, col 13) : expression error : innocuous expression")
    }
    @Test
    fun yy_02_innoc() {
        val l = lexer("""
            do {
                var x
                set x = [0]
                x   ;; escape but no access
            }
            println(1)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        //assert(e.tostr() == "do { 1; };\nnil;\n") { e.tostr() }
        //assert(trap { parser.exprs() } == "anon : (lin 2, col 13) : expression error : innocuous expression")
    }
    @Test
    fun yy_03_innoc() {
        val l = lexer("""
            do {
                do {
                    1
                }
                nil
            }
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        //assert(trap { parser.exprs() } == "anon : (lin 3, col 17) : expression error : innocuous expression")
    }
}
