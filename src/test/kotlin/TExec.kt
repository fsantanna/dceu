import org.junit.Test
import java.io.File

class TExec {

    fun all(inp: String): String {
        val lexer = Lexer("anon", inp.reader())
        val parser = Parser(lexer)
        val es = try {
            parser.exprs()
        } catch (e: Throwable) {
            return e.message!!
        }
        val coder = Coder(parser)
        val c = try {
            coder.expr(Expr.Do(Tk.Fix("",0,0),null,es))
        } catch (e: Throwable) {
            return e.message!!
        }
        File("out.c").writeText(c)
        val (ok2, out2) = exec("gcc -Werror out.c -o out.exe")
        if (!ok2) {
            return out2
        }
        val (_, out3) = exec("./out.exe")
        //println(out3)
        return out3
    }

    // PRINT

    @Test
    fun print1() {
        val out = all("""
            print([10])
        """.trimIndent()
        )
        assert(out == "[10]") { out }
    }
    @Test
    fun print2() {
        val out = all("""
            print(10)
            println(20)
        """.trimIndent()
        )
        assert(out == "1020\n") { out }
    }
    @Test
    fun print3() {
        val out = all("""
            println([[],[1,2,3]])
        """.trimIndent()
        )
        assert(out == "[[],[1,2,3]]\n") { out }
    }
    @Test
    fun print_err1() {
        val out = all("""
            println(1)
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun print_err2() {
        val out = all("""
            print(1)
            print()
            print(2)
            println()
            println(3)
        """.trimIndent()
        )
        assert(out == "12\n3\n") { out }
    }
    @Test
    fun print4() {
        val out = all("print(nil)")
        assert(out == "nil") { out }
    }
    @Test
    fun print5() {
        val out = all("print(true)")
        assert(out == "true") { out }
    }
    @Test
    fun print6() {
        val out = all("println(false)")
        assert(out == "false\n") { out }
    }

    // INDEX

    @Test
    fun index() {
        val out = all("""
            println([1,2,3][1])
        """.trimIndent()
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun index_err1() {
        val out = all("""
            println(1[1])
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 9) : index error : expected tuple\n") { out }
    }
    @Test
    fun index_err2() {
        val out = all("""
            println([1][[]])
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 13) : index error : expected number\n") { out }
    }
    @Test
    fun index_err3() {
        val out = all("""
            println([1][2])
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 13) : index error : out of bounds\n") { out }
    }

    // DCL

    @Test
    fun dcl() {
        val out = all("""
            var x
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }

    // SET

    @Test
    fun set1() {
        val out = all("""
            var x
            set x = [10]
            println(x)
        """.trimIndent()
        )
        assert(out == "[10]\n") { out }
    }
    @Test
    fun set2() {
        val out = all("""
            var x
            set x = [10,20,[30]]
            set x[1] = 22
            set x[2][0] = 33
            println(x)
        """.trimIndent()
        )
        assert(out == "[10,22,[33]]\n") { out }
    }
    @Test
    fun set_err1() {
        val out = all("""
            set 1 = 1
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : invalid set : invalid destination") { out }
    }
    @Test
    fun set_err2() {
        val out = all("""
            set [1] = 1
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : invalid set : invalid destination") { out }
    }
    @Test
    fun set_index() {
        val out = all("""
            var i
            set i = 1
            println([1,2,3][i])
        """.trimIndent()
        )
        assert(out == "2\n") { out }
    }

    // DO

    @Test
    fun do1() {  // set whole tuple?
        val out = all("""
            do {}
        """.trimIndent()
        )
        assert(out == "") { out }
    }
    @Test
    fun do2() {
        val out = all("""
            do {
                var a
                set a = 1
                println(a)
            }
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun do3() {
        val out = all("""
            var x
            set x = do {
                var a
                set a = 10
                a
            }
            print(x)
        """.trimIndent()
        )
        assert(out == "10") { out }
    }
    @Test
    fun do4() {
        val out = all("""
            var x
            set x = do {}
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }

    // SCOPE

    @Test
    fun scope1() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun scope_err2() {
        val out = all("""
            var x
            do {
                var a
                set a = [1,2,3]
                set x = a
            }
        """.trimIndent()
        )
        assert(out == "anon : (lin 5, col 13) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun todo_scope_scope3() {
        val out = all("""
            var x
            do {
                var a
                set a = [1,2,3] @x
                set x = a
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1,2,3]") { out }
    }
    @Test
    fun scope4() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                set x[1] = [4,5,6]
                do {
                    var y
                    set y = [10,20,30]
                    set y[1] = x[1]
                    set x[2] = y[1]
                }
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1,[4,5,6],[4,5,6]]\n") { out }
    }
    @Test
    fun scope5_err() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                var y
                set y = [10,20,30]
                set x[2] = y
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "anon : (lin 6, col 16) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun scope6() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                var y
                set y = 30
                set x[2] = y
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1,2,30]\n") { out }
    }
    @Test
    fun scope7() {
        val out = all("""
            var xs
            set xs = do {
                [10]
            }
            println(xs)
        """.trimIndent()
        )
        assert(out == "[10]\n") { out }
    }

    // IF

    @Test
    fun if1() {
        val out = all("""
            var x
            set x = if (true) { 1 }
            println(x)
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun if2() {
        val out = all("""
            var x
            set x = 10
            set x = if false { 1 }
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun if3() {
        val out = all("""
            var x
            set x = 10
            set x = if (nil) {} else { 1 }
            println(x)
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun if_err() {
        val out = all("""
            if [] {}
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 4) : if error : invalid condition\n") { out }
    }

    // FUNC / CALL

    @Test
    fun func1() {
        val out = all("""
            var f
            set f = func () { }
            var x
            set x = f()
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun func2() {
        val out = all("""
            var f
            set f = func () {
                1
            }
            var x
            set x = f()
            println(x)
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun func3() {
        val out = all("""
            var f
            set f = func (x) {
                x
            }
            var x
            set x = f(10)
            println(x)
        """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun func4() {
        val out = all("""
            var f
            set f = func (x) {
                x
            }
            var x
            set x = f()
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun func5_err() {
        val out = all("""
            var f
            set f = func (x) {
                [x]
            }
            var x
            set x = f(10)
            println(x)
        """.trimIndent()
        )
        assert(out == "[10]\n") { out }
        //assert(out.contains("set error : incompatible scopes")) { out }
    }
    @Test
    fun todo_scope_func6() {
        val out = all("""
            var f
            set f = func (x,s) {
                [x]@s
            }
            var x
            set x = f(10)
            println(x)
        """.trimIndent()
        )
        assert(out == "[10]\n") { out }
    }
    @Test
    fun func7_err() {
        val out = all("1(1)")
        assert(out == "anon : (lin 1, col 1) : call error : expected function\n") { out }
    }

    // LOOP / BREAK

    @Test
    fun loop1() {
        val out = all("""
            println(loop { break 1 })
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun loop2() {
        val out = all("""
            println(loop { []; break 1 })
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun loop3() {
        val out = all("""
            println(loop { break [1] })
        """.trimIndent()
        )
        assert(out == "[1]\n") { out }
    }
    @Test
    fun loop4() {
        val out = all("""
            println(loop {
                var x
                set x = [1] -- memory released
                break 1
            })
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun loop5_err() {
        val out = all("""
            println(loop {
                var x
                set x = [1]
                break x
            })
        """.trimIndent()
        )
        assert(out == "anon : (lin 4, col 11) : set error : incompatible scopes\n") { out }
    }

    // THROW / CATCH

    @Test
    fun catch1() {
        val out = all("""
            catch 1 {
                throw 1
                println(9)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun catch2_err() {
        val out = all("""
            catch 1 {
                throw 5
                println(9)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == "anon : (lin 2, col 5) : throw error : uncaught exception\n") { out }
    }
    @Test
    fun catch3() {
        val out = all("""
            var x
            set x = catch 1 {
                catch 2 {
                    throw (1,10)
                    println(9)
                }
                println(9)
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun catch4() {
        val out = all("""
            var f
            set f = func () {
                catch 1 {
                    throw 2
                    println(91)
                }
                println(9)
            }
            catch 2 {
                catch 1 {
                    f()
                    println(92)
                }
                println(93)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun catch5_err() {
        val out = all("""
            catch 1 {
                throw []
                println(9)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == "anon : (lin 2, col 5) : throw error : invalid exception : expected number\n") { out }
    }
    @Test
    fun catch6_err() {
        val out = all("""
            catch 1 {
                var x
                set x = []
                throw (1, x)
                println(9)
            }
            println(1)
        """.trimIndent()
        )
        assert(out == "anon : (lin 4, col 15) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun catch7() {
        val out = all("""
            do {
                println(catch 2 {
                    throw (2,[10])
                    println(9)
                })
            }
        """.trimIndent()
        )
        assert(out == "[10]\n") { out }
    }
    @Test
    fun catch8() {
        val out = all("""
            var x
            set x = catch 1 {
                var y
                set y = catch 2 {
                    throw (2,[10])
                    println(9)
                }
                --println(1)
                y
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "anon : (lin 9, col 5) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun catch9() {
        val out = all("""
            catch 1 {
                catch 2 {
                    catch 3 {
                        catch 4 {
                            println(1)
                            throw 3
                            println(99)
                        }
                        println(99)
                    }
                    println(2)
                    throw 1
                    println(99)
                }
                println(99)
            }
            println(3)
        """.trimIndent()
        )
        assert(out == "1\n2\n3\n") { out }
    }

    // NATIVE

    @Test
    fun native1() {
        val out = all("""
            native {
                printf("xxx\n");
            }
        """.trimIndent()
        )
        assert(out == "xxx\n") { out }
    }
    @Test
    fun native2() {
        val out = all("""
            var x
            set x = native {
                return 1.5;
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "1.5\n") { out }
    }
    @Test
    fun native3() {
        val out = all("""
            var x
            set x = native {}
            println(x)
        """.trimIndent()
        )
        assert(out == "0\n") { out }
    }
    @Test
    fun native4() {
        val out = all("""
            var x
            set x = 10
            set x = native {
                printf(">>> %g\n", ${D}x);
                return ${D}x*2;
            }
            println(x)
        """.trimIndent()
        )
        assert(out == ">>> 10\n20\n") { out }
    }
    @Test
    fun native5() {
        val out = all("""
            var x
            set x = 10
             native {
                ${D}x = 20;
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "20\n") { out }
    }
    @Test
    fun native6_err() {
        val out = all("""
             native {
                ${D} 
             }
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : native error : (lin 2, col 4) : invalid identifier") { out }
    }
    @Test
    fun native7_err() {
        val out = all("""
             native {
             
                ${D}{x.y}
                
             }
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : native error : (lin 3, col 4) : invalid identifier") { out }
    }
    @Test
    fun native8_err() {
        val out = all("""
            native (${D})
        """.trimIndent()
        )
        assert(out == "anon : (lin 1, col 1) : native error : (lin 1, col 2) : unterminated token") { '.'+out+'.' }
    }
}