package ceu

import Expr
import Parser
import org.junit.Ignore
import org.junit.Test
import tostr

class TParser {

    // EXPR.VAR / EVT / ERR

    @Test
    fun expr_var () {
        val l = lexer(" x ")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Acc && e.tk.str == "x")
    }
    @Test
    fun expr_var_err1 () {
        val l = lexer(" { ")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 2) : expected expression : have \"{\"")
    }
    @Test
    fun expr_var_err2 () {
        val l = lexer("  ")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 3) : expected expression : have end of file")
    }
    @Test
    fun evt3 () {
        val l = lexer(" evt ")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.EvtErr && e.tk.str == "evt")
    }
    @Test
    fun err4 () {
        val l = lexer(" err ")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.EvtErr && e.tk.str == "err")
    }

    // EXPR.PARENS

    @Test
    fun expr_parens() {
        val l = lexer(" ( a ) ")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Acc && e.tk.str == "a")
    }
    @Test
    fun expr_parens_err() {
        val l = lexer(" ( a  ")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 7) : expected \")\" : have end of file")
    }

    // EXPR.NUM / EXPR.NIL / EXPR.BOOL

    @Test
    fun expr_num() {
        val l = lexer(" 1.5F ")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }
    @Test
    fun expr_nil() {
        val l = lexer("nil")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Nil && e.tk.str == "nil")
    }
    @Test
    fun expr_true() {
        val l = lexer("true")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Bool && e.tk.str == "true")
    }
    @Test
    fun expr_false() {
        val l = lexer("false")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Bool && e.tk.str == "false")
    }
    @Test
    fun expr_char() {
        val l = lexer("'x'")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Char && e.tk.str == "'x'")
    }

    // EXPR.ECALL

    @Test
    fun expr_call1() {
        val l = lexer(" f (1.5F, x) ")
        val parser = Parser(l)
        val e = parser.exprSufs()
        assert(e is Expr.Call && e.tk.str=="f" && e.proto is Expr.Acc && e.args.size==2)
    }
    @Test
    fun expr_call2() {
        val l = lexer(" f() ")
        val parser = Parser(l)
        val e = parser.exprSufs()
        assert(e is Expr.Call && e.proto.tk.str=="f" && e.proto is Expr.Acc && e.args.size==0)
    }
    @Test
    fun expr_call3() {
        val l = lexer(" f(x,8)() ")
        val parser = Parser(l)
        val e = parser.exprSufs()
        assert(e is Expr.Call && e.proto is Expr.Call && e.args.size==0)
        assert(e.tostr() == "f(x,8)()")
    }
    @Test
    fun expr_call_err1() {
        val l = lexer("f (999 ")
        val parser = Parser(l)
        assert(trap { parser.exprSufs() } == "anon : (lin 1, col 8) : expected \")\" : have end of file")
    }
    @Test
    fun expr_call_err2() {
        val l = lexer(" f ({ ")
        val parser = Parser(l)
        assert(trap { parser.exprSufs() } == "anon : (lin 1, col 5) : expected expression : have \"{\"")
    }

    // TUPLE

    @Test
    fun expr_tuple1() {
        val l = lexer(" [ 1.5F, x] ")
        val parser = Parser(l)
        val e = parser.exprSufs()
        assert(e is Expr.Tuple && e.args.size==2)
    }
    @Test
    fun expr_tuple2() {
        val l = lexer("[[],[1,2,3]]")
        val parser = Parser(l)
        val e = parser.exprSufs()
        assert(e.tostr() == "[[],[1,2,3]]")
    }
    @Test
    fun expr_tuple_err() {
        val l = lexer("[{")
        val parser = Parser(l)
        assert(trap { parser.exprSufs() } == "anon : (lin 1, col 2) : expected expression : have \"{\"")
    }
    @Test
    fun tuple4() {
        val l = lexer("[1.5F,] ")
        val parser = Parser(l)
        val e = parser.exprSufs()
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
        val e = parser.exprSufs()
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
        val l = lexer("v[{#}(v)]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "v[{#}(v)]") { e.tostr() }
    }

    // EXPR.INDEX / PUB

    @Test
    fun index1() {
        val l = lexer("x[10]")
        val parser = Parser(l)
        val e = parser.exprSufs()
        assert(e is Expr.Index && e.col is Expr.Acc && e.idx is Expr.Num)
    }
    @Test
    fun index2_err() {
        val l = lexer("x[10")
        val parser = Parser(l)
        assert(trap { parser.exprSufs() } == "anon : (lin 1, col 5) : expected \"]\" : have end of file")
    }
    @Test
    fun pub3() {
        val l = lexer("x . pub")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Pub && e.x is Expr.Acc)
        assert(e.tostr() == "x.pub") { e.tostr() }
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
        assert(trap { parser.exprSufs() } == "anon : (lin 1, col 5) : expected \"pub\" : have \".\"")
    }
    @Test
    fun index6_err() {
        val l = lexer("x . 2")
        val parser = Parser(l)
        assert(trap { parser.exprSufs() } == "anon : (lin 1, col 5) : expected \"pub\" : have \"2\"")
    }
    @Test
    fun pub7_err() {
        val l = lexer("set pub = x.pub + pub")
        val parser = Parser(l)
        assert(trap { parser.exprSufs() } == "anon : (lin 1, col 5) : expected expression : have \"pub\"")
    }
    @Test
    fun pub8() {
        val l = lexer("set task.pub = x.pub + task.pub")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "set task.pub = {+}(x.pub,task.pub)") { e.tostr() }
    }

    // EXPRS

    @Test
    fun exprs_call() {
        val l = lexer("f ()")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.size==1 && es[0] is Expr.Call && es[0].tostr() == "f()")
        assert(es.tostr() == "f()\n")
    }
    @Test
    fun exprs_call_err() {
        val l = lexer("f")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.tostr() == "f\n")
    }

    // EXPRS

    @Test
    fun exprs_seq1() {
        val l = lexer("; f () ; g () h()\ni() ;\n;")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.tostr() == "f()\ng()\nh()\ni()\n") { es.tostr() }
    }
    @Test
    fun exprs_seq2() {
        val l = lexer("; f () \n (1) ; h()\ni() ;\n;")
        val parser = Parser(l)
        //assert(es.tostr() == "f()\n1\nh()\ni()\n") { es.tostr() }
        assert(ceu.trap { parser.exprs() } == "anon : (lin 2, col 3) : invalid expression : innocuous expression")
    }
    @Test
    fun exprs_seq2a() {
        val l = lexer("; f () \n pass (1) ; h()\ni() ;\n;")
        val parser = Parser(l)
        // TODO: ambiguous
        val es = parser.exprs()
        assert(es.tostr() == "f()\npass 1\nh()\ni()\n") { es.tostr() }
        //assert(ceu.trap { parser.exprs() } == "anon : (lin 2, col 3) : call error : \"(\" in the next line")
    }
    @Test
    fun exprs_seq3() {
        val l = lexer("var v2\n[tp,v1,v2]")
        val parser = Parser(l)
        // TODO: ambiguous
        val es = parser.exprs()
        assert(es.tostr() == "var v2\n[tp,v1,v2]\n") { es.tostr() }
    }

    // EXPR.DCL

    @Test
    fun expr_dcl_var() {
        val l = lexer("var x")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Dcl && e.tk.str=="var" && e.id.str=="x")
        assert(e.tostr() == "var x")
    }
    @Test
    fun expr_dcl_val() {
        val l = lexer("val x")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Dcl && e.tk.str=="val" && e.id.str=="x")
        assert(e.tostr() == "val x")
    }
    @Test
    fun expr_dcl_err() {
        val l = lexer("var [10]")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 5) : expected identifier : have \"[\"")
    }
    @Test
    fun expr_dcl3() {
        val l = lexer("var x = 1")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Dcl && e.id.str == "x" && e.src is Expr.Num)
        assert(e.tostr() == "var x = 1")
    }

    // EXPR.SET

    @Test
    fun expr_set() {
        val l = lexer("set x = [10]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Set && e.tk.str == "set")
        assert(e.tostr() == "set x = [10]")
    }
    @Test
    fun expr_err1() {  // set number?
        val l = lexer("set 1 = 1")
        val parser = Parser(l)
        //val e = parser.exprN()
        //assert(e.tostr() == "set 1 = 1")
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid set : invalid destination")
    }
    @Test
    fun expr_err2() {  // set whole tuple?
        val l = lexer("set [1] = 1")
        val parser = Parser(l)
        //val e = parser.exprN()
        //assert(e.tostr() == "set [1] = 1")
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid set : invalid destination")
    }
    @Test
    fun set_nil_err() {
        val l = lexer("set nil = nil")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid set : invalid destination")
    }
    @Test
    fun set_if_err() {
        val l = lexer("set (if true {nil} else {nil}) = nil")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid set : invalid destination")
    }
    @Test
    fun set_evt_err() {
        val l = lexer("set evt = nil")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid set : invalid destination")
    }
    @Test
    fun set_err_err() {
        val l = lexer("set err = nil")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : invalid set : invalid destination")
    }

    // IF

    @Test
    fun expr_if1() {  // set whole tuple?
        val l = lexer("if true { 1 } else { 0 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n} else {\n0\n}") { e.tostr() }
    }
    @Test
    fun expr_if2_err() {
        val l = lexer("if true { 1 }")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 14) : expected \"else\" : have end of file")
    }

    // DO

    @Test
    fun expr_do1_err() {
        val l = lexer("do{}")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 4) : expected expression : have \"}\"")
    }
    @Test
    fun expr_do2() {
        val l = lexer("do { var a; set a=1; print(a) }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Do && e.es.size==3)
        assert(e.tostr() == "do {\nvar a\nset a = 1\nprint(a)\n}") { e.tostr() }
    }

    // EXPORT

    @Test
    fun expr_export1_err() {
        val l = lexer("export {}")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 8) : expected \"[\" : have \"{\"")
    }
    @Test
    fun expr_export2_err() {
        val l = lexer("export [:x] {}")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 9) : expected identifier : have \":x\"")
    }
    @Test
    fun expr_export3() {
        val l = lexer("export [] { nil }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Export && e.ids.isEmpty() && e.body.es.size==1)
        assert(e.tostr() == "export [] {\nnil\n}") { e.tostr() }
    }
    @Test
    fun expr_export4() {
        val l = lexer("export [x] { nil }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Export && e.ids.first().str=="x" && e.body.es.size==1)
        assert(e.tostr() == "export [x] {\nnil\n}") { e.tostr() }
    }
    @Test
    fun expr_export5() {
        val l = lexer("export [x,y] { nil }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Export && e.ids.last().str=="y" && e.body.es.size==2)
        assert(e.tostr() == "export [x,y] {\nnil\n}") { e.tostr() }
    }

    // GROUP
/*
    @Test
    fun group1_err() {
        val l = lexer("do :x {}")
        val parser = Parser(l)
        //assert(trap { parser.exprPrim() } == "anon : (lin 1, col 1) : invalid group : unexpected \":x\"")
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 4) : expected \"{\" : have \":x\"")
    }
    @Test
    fun group2() {
        val l = lexer("do :unnest { pass 1 ; 2 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Do && e.es.size==2)
        assert(e.tostr() == "do :unnest {\npass 1\n2\n}") { e.tostr() }
    }
    @Test
    fun group3() {
        val l = lexer("do :unnest-hide { pass x; pass y; z; }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Do && e.es.size==3)
        assert(e.tostr() == "do :unnest-hide {\npass x\npass y\nz\n}") { e.tostr() }
    }
*/

    // FUNC

    @Test
    fun expr_func1_err() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 10) : expected expression : have \"}\"")
    }
    @Test
    fun expr_func2() {
        val l = lexer("func (a,b) { 10 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Proto && e.args.size==2)
        assert(e.tostr() == "func (a,b) {\n10\n}") { e.tostr() }
    }
    @Test
    fun expr_func3_err() {
        val l = lexer("coro (a,b) :fake { 10 }")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 12) : expected \"{\" : have \":fake\"")
    }
    @Test
    fun expr_task4_err() {
        val l = lexer("coro (a,b) :xxx { 10 }")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 12) : expected \"{\" : have \":xxx\"")
    }
    @Test
    fun expr_task5() {
        val l = lexer("task (a,b) { 10 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Proto && e.args.size==2)
        assert(e.tostr() == "task (a,b) {\n10\n}") { e.tostr() }
    }
    @Test
    fun pp_06_func_dots() {
        val l = lexer("func (...) { println(...) }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Proto && e.args.size==1)
        assert(e.tostr() == "func (...) {\nprintln(...)\n}") { e.tostr() }
    }
    @Test
    fun pp_07_func_args_err() {
        val l = lexer("func (1) { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 7) : expected identifier : have \"1\"")
    }
    @Test
    fun pp_08_func_args_err() {
        val l = lexer("func (..., a) { nil }")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 10) : expected \")\" : have \",\"")
    }
    @Test
    fun pp_09_func_args_err() {
        val l = lexer("println(...,a)")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 12) : expected \")\" : have \",\"")
    }
    @Test
    fun pp_10_func_args_err() {
        val l = lexer("var ...")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 5) : invalid declaration : unexpected ...")
    }
    @Test
    fun pp_11_func_args_err() {
        val l = lexer("set ... = 10")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 5) : invalid set : unexpected ...")
    }

    // LOOP

    @Test
    fun qq_01_loop_err() {
        val l = lexer("loop { pass nil }")
        val parser = Parser(l)
        val e1 = parser.expr() as Expr.Do
        val e2 = e1.es.last() as Expr.Loop
        assert(e2.body.tostr() == "{\npass nil\n}") { e2.body.tostr() }
    }
    @Test
    fun qq_02_loop_err() {
        val l = lexer("loop until {")
        val parser = Parser(l)
        assert(trap { parser.exprBins() } == "anon : (lin 1, col 12) : expected expression : have \"{\"")
    }
    @Test
    fun qq_03_loop_err() {
        val l = lexer("loop x { }")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 6) : expected \"{\" : have \"x\"")
    }

    // NATIVE

    @Test
    fun native1() {
        val l = lexer("""
            ```
                printf("xxx\n");
            ```
        """.trimIndent())
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Nat)
        assert(e.tostr() == "``` \n    printf(\"xxx\\n\");\n```") { "."+e.tostr()+"." }
    }
    @Test
    fun native2_err() {
        val l = lexer("""
            native ``
                printf("xxx\n");
            ```
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 3, col 1) : native error : expected \"``\"")
    }
    @Test
    fun native3_err() {
        val l = lexer("""
            native ``
                printf("xxx\n");
        """)
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 4, col 9) : native error : expected \"``\"")
    }
    @Test
    fun native4_err() {
        val l = lexer("""
            native `:
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 8) : tag error : expected identifier")
    }
    @Test
    fun native5() {
        val l = lexer("""
            ```:ola```
        """.trimIndent())
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Nat)
        assert(e.tostr() == "```:ola ```") { "."+e.tostr()+"." }
    }

    // BINARY / UNARY / OPS

    @Test
    fun bin1_err() {
        val l = lexer("(10+)")
        val parser = Parser(l)
        assert(trap { parser.exprBins() } == "anon : (lin 1, col 5) : expected expression : have \")\"")
    }
    @Test
    fun bin2() {
        val l = lexer("10+1")
        val parser = Parser(l)
        val e = parser.exprBins()
        assert(e is Expr.Call)
        assert(e.tostr() == "{+}(10,1)") { e.tostr() }
    }
    @Test
    fun bin3() {
        val l = lexer("10/=1")
        val parser = Parser(l)
        val e = parser.exprBins()
        assert(e is Expr.Call)
        assert(e.tostr() == "{/=}(10,1)") { e.tostr() }
    }
    @Test
    fun pre1() {
        val l = lexer("- - 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{-}({-}(1))") { e.tostr() }
    }
    @Test
    fun bin4() {
        val l = lexer("(1 + 2) + 3")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{+}({+}(1,2),3)") { e.tostr() }
    }
    @Test
    fun pre_pos1() {
        val l = lexer("-x[0]")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "{-}(x[0])") { e.tostr() }
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
            set t = task (v) {
            set v = yield(1)
            yield(2)
            }
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
        assert(ceu.trap { parser.expr() } == "anon : (lin 2, col 1) : expected \"(\" : have \"1\"")
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

    @Ignore // now expands to complex C code
    @Test
    fun tasks1() {
        val l = lexer("""
            var ts
            set ts = tasks()
            loop in :tasks ts, t {
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
            loop in :tasks ts, 1 {
                nil
            }
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 4, col 32) : expected identifier : have \"1\"")
    }
    @Test
    fun tasks3_err() {
        val l = lexer("""
            var ts
            set ts = tasks()
            loop in :tasks x {
                nil
            }
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 4, col 30) : expected \",\" : have \"{\"")
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

    // ENUM

    @Test
    fun enum01() {
        val l = lexer("""
            enum {
                :x = `1000`,
                :y, :z,
                :a = `10`,
                :b, :c
            }
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "enum {\n:x = `1000`,\n:y,\n:z,\n:a = `10`,\n:b,\n:c\n}\n") { e.tostr() }
    }
    @Test
    fun enum02_err() {
        val l = lexer("""
            enum { :x=1 }
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected native : have \"1\"")
    }
    @Test
    fun enum03_err() {
        val l = lexer("""
            enum { :x, 1 }
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 24) : expected tag : have \"1\"")
    }

    // TEMPLATE

    @Test
    fun tplate00() {
        val l = lexer("""
            data :T = [x,y]
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "data :T = [x,y]\n") { e.tostr() }
    }
    @Test
    fun tplate01() {
        val l = lexer("""
            var t :T = [1,2]
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "var t :T = [1,2]\n") { e.tostr() }
    }
    @Test
    fun tplate02_err() {
        val l = lexer("""
            data X [x,y]
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 18) : expected tag : have \"X\"")
    }
    @Test
    fun tplate03_err() {
        val l = lexer("""
            data :X [x,y]
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 21) : expected \"=\" : have \"[\"")
    }
    @Test
    fun tplate04_err() {
        val l = lexer("""
            data :X = nil
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 23) : expected \"[\" : have \"nil\"")
    }
    @Test
    fun tplate05_err() {
        val l = lexer("""
            data :X = [1,2]
        """)
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 2, col 24) : expected identifier : have \"1\"")
    }
    @Test
    fun tplate06() {
        val l = lexer("""
            data :U = [t:T]
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "data :U = [t:T]\n") { e.tostr() }
    }

    // POLY
    /*
    @Test
    fun gg_01_poly_err() {
        val l = lexer("poly x")
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 6) : poly error : expected var or set")
    }
    @Test
    fun gg_02_poly_err() {
        val l = lexer("poly var x = 1")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 12) : expected expression : have \"=\"")
    }
    @Test
    fun gg_03_poly() {
        val l = lexer("poly var x")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr(false) == "poly var x") { e.tostr() }
    }
    @Test
    fun gg_04_poly_err() {
        val l = lexer("poly set x = 1")
        val parser = Parser(l)
        assert(trap { parser.exprs() } == "anon : (lin 1, col 14) : expected \"func\" : have \"1\"")
    }
    @Test
    fun gg_05_poly_set_tag() {
        val l = lexer("poly set min :number = 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr(false) == "set min[:number] = 1") { e.tostr() }
    }
    @Test
    fun gg_06_poly_set_func() {
        val l = lexer("poly set f = func () { nil }")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr(false) == "poly var x") { e.tostr() }
    }
    @Test
    fun gg_07_poly_set_err() {
        val l = lexer("poly set f.x :number = 10")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr(false) == "poly var x") { e.tostr() }
    }
    */
}
