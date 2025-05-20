package tst_04

import dceu.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_04 {
    @Test
    fun aa_04_task_err() {
        val out = test(
            """
            val T = func (v) { nil }
            T()
        """
        )
        assert(
            out == " |  anon : (lin 3, col 13) : T()\n" +
                    " v  throw : expected function\n"
        ) { out }
    }
    @Test
    fun df_03_bcast_throw() {
        DEBUG = true
        val out = test(
            """
            spawn (func () {
                spawn (func () {
                    await(true)
                    await(true)
                    print(:ok)
                    throw(:XXX)
                }) ()
                spawn (func () {
                    await(true)
                    emit (nil) in :global
                }) ()
                loop' {
                    await(true)
                }
            }) ()            
            emit(true)
        """
        )
        assert(
            out == ":ok\n" +
                    " |  anon : (lin 17, col 13) : emit'(:task,nil)\n" +
                    " |  anon : (lin 11, col 21) : emit'(:global,nil)\n" +
                    " |  anon : (lin 7, col 21) : throw(:XXX)\n" +
                    " v  throw : :XXX\n"
        ) { out }
    }
    @Test
    fun dd_16_tags() {
        val out = test(
            """
            val T = func () {
                func' (it) {
                    print(sup?(:X,tag(it)))
                } (yield(nil))
            }
            spawn T()
            emit (tag(:X,[]))
        """
        )
        assert(out == "true\n") { out }
    }
    @Test
    fun kk_00_x_pub_err() {
        val out = test("""
            func.pub
        """)
        //assert(out == "anon : (lin 2, col 18) : pub throw : expected enclosing task") { out }
        //assert(out == "anon : (lin 2, col 13) : task throw : missing enclosing task") { out }
        assert(out == "anon : (lin 2, col 18) : expected \"(\" : have \".\"\n") { out }
    }
    @Test
    fun ll_10_nested() {
        val out = test(
            """
            spawn (func () :X {
                set pub.x = nil
            }) ()
            print(:ok)
        """
        )
        assert(out == "anon : (lin 2, col 29) : declaration throw : data :X is not declared\n") { out }
    }
    @Test
    fun mm_05_defer() {
        val out = test(
            """
            func () {
                defer {
                    await(true)   ;; no yield inside defer
                }
            }
            print(1)
        """
        )
        assert(out == "anon : (lin 4, col 21) : yield throw : unexpected enclosing defer\n") { out }
    }

    // ALIEN SCOPE
    @Test
    fun cd_03_bcast_pub_arg() {
        val out = test(
            """
            val T = func () {
                val evt = await(true)
                set pub = evt
                print(:in, pub)
                :ok
            }
            val t = spawn T() 
            do {
                val e = []
                emit(e)
            }
            print(:out, t.pub)
        """
        )
        //assert(out == " |  anon : (lin 8, col 17) : emit'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : emit'(e,:task)\n" +
        //        " v  anon : (lin 4, col 21) : set throw : cannot hold alien reference\n") { out }
        assert(out == ":in\t[]\n:out\t:ok\n") { out }
    }
    @Test
    fun cd_04_bcast_copy() {
        val out = test("""
            val T = func () {
                val evt = await(true)
                val v = copy(evt)
                print(v)
            }
            spawn T()
            emit([1,2,3])
        """, true)
        assert(out == "[1,2,3]\n") { out }
    }

    // DELAY

    @Test
    fun bj_01_delay_err() {
        val out = test(
            """
            func () {
                func' () {
                    delay
                }
            }
        """
        )
        assert(out.contains("anon : (lin 4, col 21) : delay throw : expected enclosing task\n")) { out }
        //assert(out == ("anon : (lin 4, col 21) : access throw : variable \"delay\" is not declared\n")) { out }
    }
    @Test
    fun bj_01x_delay_err() {
        val out = test(
            """
            delay
        """
        )
        assert(out.contains("anon : (lin 2, col 13) : delay throw : expected enclosing task\n")) { out }
        //assert(out == ("anon : (lin 4, col 21) : access throw : variable \"delay\" is not declared\n")) { out }
    }
    @Test
    fun bj_02y_delay() {
        val out = test(
            """
            spawn (func () {
                await(true)
                print(2)
            }) ()
            ;;print(:1)
            emit(true)
            ;;print(:2)
            print(:ok)
        """
        )
        assert(out == "2\n:ok\n") { out }
    }
    @Test
    fun bj_02_delay() {
        val out = test(
            """
            spawn (func () {
                await(true)
                await(true)
                await(true)
                print(1)
            }) ()
            spawn (func () {
                await(true)
                print(2)
            }) ()
            spawn (func () {
                await(true)
                await(true)
                print(3)
            }) ()
            emit(true)
            print(:ok)
        """
        )
        assert(out == "2\n3\n1\n:ok\n") { out }
    }
    @Test
    fun bj_02x_delay() {
        val out = test(
            """
            spawn (func () {
                await(true)
                print(2)
            }) ()
            spawn (func () {
                await(true)
                await(true)
                print(3)
            }) ()
            emit(true)
            print(:ok)
        """
        )
        assert(out == "2\n3\n:ok\n") { out }
    }
    @Test
    fun bj_03_delay() {
        val out = test(
            """
            spawn (func () {
                await(true)
                delay
                await(true)
                delay
                await(true)
                print(1)
            }) ()
            spawn (func () {
                await(true)
                print(2)
            }) ()
            spawn (func () {
                await(true)
                delay
                await(true)
                print(3)
            }) ()
            print(:a)
            emit(true)
            print(:b)
            emit(true)
            print(:ok)
        """
        )
        assert(out == ":a\n2\n:b\n1\n3\n:ok\n") { out }
    }
    @Test
    fun bj_03_par() {
        val out = test(
            """
            spawn (func () {
                spawn (func () {
                    print(1)
                }) ()
                spawn (func () {
                    print(2)
                }) ()
                print(3)
            }) ()
        """
        )
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun bj_04_toggle() {
        DEBUG = true
        val out = test(
            """
            $PLUS
            var i = 0
            enclose' :break {
                loop' {
                    emit(true)
                    if i == 254 {
                        escape(:break, nil)
                    } else {nil}
                    set i = i + 1
                }
            }
            val t = spawn (func () {
                print(`:number CEU_TIME`)
                await(true)
                delay
                print(:ok)
            }) ()
            toggle t (false)
            emit(true)
            toggle t (true)
            emit(true)
        """
        )
        assert(out == "255\n:ok\n") { out }
    }
    @Test
    fun bj_05_spawn_spawn() {
        val out = test(
            """
            spawn (func () {
                spawn (func (v) {
                    print(v)
                }) (10)
                nil
            }) ()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun bj_06_delay() {
        val out = test("""
            spawn (func () {
                val v = do {
                    ;;;do;;; 100
                    delay
                }
                print(v)
            }) ()
        """)
        assert(out == "100\n") { out }
    }
    @Test
    fun bj_07_delay() {
        DEBUG = true
        val out = test("""
            spawn (func () {
                val v = enclose' :break {
                    loop' {
                        escape(:break,100) ;;;if true;;;
                    }
                    delay
                }
                print(v)
                ;;dump(v)
            }) ()
        """)
        assert(out == "100\n") { out }
    }

    // THROW / CATCH

    @Test
    fun ee_00x_throw() {
        val out = test(
            """
            catch :z ;;;(it|false);;; {
                spawn (func () {
                    throw(:xxx)
                })()
            }
            print(10)
        """
        )
        assert(out == " |  anon : (lin 3, col 17) : (spawn (func () { throw(:xxx); })())\n" +
                " |  anon : (lin 4, col 21) : throw(:xxx)\n" +
                " v  throw : :xxx\n") { out }
    }
    @Test
    fun ee_00y_throw() {
        val out = test(
            """
            catch ;;;(it|true);;; {
                spawn (func () {
                    throw(:xxx)
                })()
            }
            print(10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun ee_01_throw() {
        val out = test(
            """
            catch :xxx ;;;(it | :xxx);;; {
                spawn (func () {
                    await(true)
                })()
                spawn (func () {
                    throw(:xxx)
                })()
            }
            print(10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun ee_02_throw() {
        val out = test(
            """
            spawn (func () {
                throw(:err)
            })()
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : (spawn (func () { throw(:err); })())\n" +
                    " |  anon : (lin 3, col 17) : throw(:err)\n" +
                    " v  throw : :err\n"
        ) { out }
    }
    @Test
    fun ee_03_throw() {
        val out = test(
            """
            spawn (func () {
                await(true)
                throw(:err)
            })()
            emit(true)
        """
        )
        assert(
            out == " |  anon : (lin 6, col 13) : emit'(:task,nil)\n" +
                    " |  anon : (lin 4, col 17) : throw(:err)\n" +
                    " v  throw : :err\n"
        ) { out }
    }
    @Test
    fun ee_04_throw() {
        val out = test(
            """
            spawn (func () {
                await(true)
                throw(:err)
            })()
            emit(true)
        """
        )
        assert(
            out == " |  anon : (lin 6, col 13) : emit'(:task,nil)\n" +
                    " |  anon : (lin 4, col 17) : throw(:err)\n" +
                    " v  throw : :err\n"
        ) { out }
    }
    @Test
    fun ee_05_throw() {
        val out = test(
            """
            spawn (func () {
                spawn( func () {
                    await(true)
                    throw(:err)
                })()
                await(true)
            })()
            emit(true)
        """
        )
        assert(
            out == " |  anon : (lin 9, col 13) : emit'(:task,nil)\n" +
                    " |  anon : (lin 5, col 21) : throw(:err)\n" +
                    " v  throw : :err\n"
        ) { out }
    }
    @Test
    fun ee_06_throw() {
        val out = test(
            """
            spawn (func () {
                await(true)
                throw(:err)
            })()
            spawn (func () {
                nil
            })()
            ;;emit(true)
        """
        )
        //assert(out == " |  anon : (lin 9, col 13) : emit'(nil,:task)\n" +
        //        " |  anon : (lin 4, col 17) : throw(:err)\n" +
        //        " v  throw : :err\n") { out }
        assert(
            out == " |  anon : (lin 6, col 13) : (spawn (func () { nil; })())\n" +
                    " |  anon : (lin 4, col 17) : throw(:err)\n" +
                    " v  throw : :err\n"
        ) { out }
    }
    @Test
    fun ee_08_throw() {
        val out = test(
            """
            spawn (func () {
                await(true)
                nil
            })()
            spawn (func () {
                await(true)
                await(true)
                throw(:err)
            })()
            emit(true)
            ;;emit(true)
        """
        )
        assert(
            out == " |  anon : (lin 11, col 13) : emit'(:task,nil)\n" +
                    " |  anon : (lin 9, col 17) : throw(:err)\n" +
                    " v  throw : :err\n"
        ) { out }
    }
    @Test
    fun ee_09_bcast() {
        val out = test(
            """
            val T = func () {
                await(true)
                throw(:err)
            }
            spawn T()
            spawn T()
            emit(true)
        """
        )
        assert(
            out == " |  anon : (lin 8, col 13) : emit'(:task,nil)\n" +
                    " |  anon : (lin 4, col 17) : throw(:err)\n" +
                    " v  throw : :err\n"
        ) { out }
    }
    @Test
    fun ee_10_bcast() {
        val out = test(
            """
            val T = func (v) {
                val e = await(true) ;;thus { it => it }
                print(v,e)                
            }
            spawn T(10)
            ;;catch {
                ;;func' () {
                    emit ([])
                ;;}()
            ;;}
        """
        )
        assert(out == "10\t[]\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : (func () { emit [] })()\n" +
        //        " |  anon : (lin 9, col 21) : emit []\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot hold event reference\n") { out }
        //assert(out == " |  anon : (lin 8, col 17) : (func () { emit [] })()\n" +
        //        " |  anon : (lin 9, col 21) : emit []\n" +
        //        " v  anon : (lin 3, col 36) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 9, col 21) : emit'([])\n" +
        //        " v  anon : (lin 3, col 36) : block escape throw : cannot copy reference out\n") { out }
    }
    @Test
    fun ee_10_bcast_err() {
        val out = test(
            """
            val T = func (v) {
                if (v == 11) {
                    await(true)
                } else {nil}
                val e = await(true)
                print(v,e)                
            }
            spawn T(10)
            spawn T(11)
            catch ;;;(it| true);;; {
                ;;func' () {
                    emit ([])
                ;;}()
                emit ([])
            }
        """
        )
        assert(
            out == "10\t[]\n" +
                    "11\t[]\n"
        ) { out }
        //assert(out == "[]\n") { out }
        //assert(out == "10\t[]\n" +
        //        " |  anon : (lin 13, col 21) : emit'([])\n" +
        //        " v  anon : (lin 6, col 17) : declaration throw : cannot copy reference out\n") { out }
    }
    @Test
    fun ee_10_bcast_err_xx() {
        val out = test(
            """
            val T = func (v) {
                val e = await(true)
                await(true)
                print(v,e)                
            }
            spawn T(10)
            do {
                val x
                emit ([])
            }
            emit(true)
        """
        )
        assert(out == "10\t[]\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : emit'([],:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 10, col 17) : emit'([],:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot hold alien reference\n") { out }
    }
    @Test
    fun ee_10_bcast_err2() {
        val out = test(
            """
            val T = func (v) {
                val e = await(true)
                print(v,e)                
            }
            spawn T(10)
            do {
                val e = []
                emit (e)
            }
        """
        )
        assert(out == "10\t[]\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : emit'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : emit'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot hold alien reference\n") { out }
    }
    @Test
    fun ee_12_bcast() {
        val out = test(
            """
            var co1 = spawn (func () {
                var co2 = spawn (func () {
                    ${AWAIT()}
                    throw(:throw)
                })()
                ${AWAIT()}
                print(1)
            })()
            emit(true)
        """
        )
        assert(
            out == " |  anon : (lin 10, col 13) : emit'(:task,nil)\n" +
                    " |  anon : (lin 5, col 21) : throw(:throw)\n" +
                    " v  throw : :throw\n"
        ) { out }
    }
    @Test
    fun ee_14_throw() {     // catch from nesting and emit
        val out = test(
            """
            spawn (func () {
                catch :e1 ;;;(it| it==:e1 );;;{  ;; catch 1st (yes catch)
                    spawn (func () {
                        await(true)
                        print(222)
                        throw(:e1)                  ;; throw
                    }) ()
                    loop' { await(true) } ;;thus { it => nil }
                }
                print(333)
            }) ()
            catch ;;;(it| true);;; {   ;; catch 2nd (no catch)
                print(111)
                emit(true)
                print(444)
            }
            print(:END)
        """
        )
        assert(
            out == "111\n" +
                    "222\n" +
                    "333\n" +
                    "444\n" +
                    ":END\n"
        ) { out }
    }
    @Test
    fun ee_15_throw() {
        val out = test(
            """
            spawn (func () {
                catch :z ;;;(it| false);;; {
                    await(true)
                }
                print(999)
            }) ()
            catch ;;;(it| true);;; {
                throw(:x)
            }
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }

    // SCOPE / BCAST

    // BCAST / DSTK,BSTK=NULL
    // todo_*
    //  - Option 1: should actually accept
    //  - Option 2: should at least raise an exception and not panic program

    @Test
    fun TODO_gh_01_coro_defer_bcast_err() {
        val out = test(
            """
            val f = func' (v) {
                print(:4)
                emit(true)
                print(:a)
            }
            spawn (func () {
                var x = coroutine(coro' () {
                    print(:1)
                    defer {
                        print(:3)
                        f()
                    }
                    await(true)
                })
                resume x()
                set x = nil
                print(:2)
            }) ()
            print(:b)
        """
        )
        assert(out.contains(": Assertion `0 && \"TODO: cannot spawn or emit during abortion\"'")) { out }
    }
    @Test
    fun TODO_gh_02_coro_defer_spawn_err() {
        val out = test(
            """
            val f = func' (v) {
                spawn (func () {
                    nil
                }) ()
            }
            spawn (func () {
                var x = coroutine(coro' () {
                    defer {
                        f()
                    }
                    await(true)
                })
                resume x()
                set x = nil
            }) ()
        """
        )
        assert(out.contains(": Assertion `0 && \"TODO: cannot spawn or emit during abortion\"'")) { out }
    }

    // STATUS

    @Test
    fun hh_01_status() {
        val out = test(
            """
            val t = spawn (func () {
                nil
            }) ()
            print(status(t))
        """
        )
        assert(out == ":terminated\n") { out }
        //assert(out == " v  anon : (lin 5, col 21) : status(t) : status throw : expected running coroutine or task\n") { out }
    }
    @Test
    fun hh_02_status() {
        val out = test(
            """
            val t = spawn (func () {
                await(true)
            }) ()
            print(status(t))
        """
        )
        assert(out == ":yielded\n") { out }
    }
    @Test
    fun hh_04_status() {            // TODO: return track for both task task_in / awake with throw?
        val out = test(
            """
            val t = spawn (func () {
                await(true)
            }) ()
            emit(true)
            print(status(t))
        """
        )
        assert(out == ":terminated\n") { out }
    }

    // TASK / BCAST IN

    @Test
    fun jj_01_bcast_in_err() {
        val out = test(
            """
            emit (nil) in nil
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : emit'(nil,nil)\n" +
                    " v  throw : invalid target\n"
        ) { out }
    }
    @Test
    fun jj_02_bcast_in_task() {
        val out = test(
            """
            val T = func (v) {
                ${AWAIT()}
                print(v)
            }
            val t1 = spawn T (1)
            val t2 = spawn T (2)
            emit (:x) in t1
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun jj_03_bcast_in_self() {
        val out = test(
            """
            val T = func (v) {
                ${AWAIT()}
                print(v)
                spawn( func () {
                    print(${AWAIT()})
                }) ()
                emit(10)
            }
            val t1 = spawn T(1)
            val t2 = spawn T(2)
            do {
                emit (nil) in t1
            }
        """
        )
        assert(out == "1\n10\n") { out }
    }

    // DROP / MOVE / OUT

    @Test
    fun jj_01_bcast_move() {
        val out = test(
            """
            var T = func () {
                val e = await(true) ;;thus { it => it}
                do {
                    var v = e
                    print(v)
                }
            }
            var t = spawn T()
            ;;print(:1111)
            var e = []
            emit (;;;drop;;;(e))
            print(e)
            """
        )
        assert(out == "[]\n[]\n") { out }
        //assert(out == "[]\nnil\n") { out }
        //assert(out == "anon : (lin 10, col 13) : emit move(e)\n" +
        //        "anon : (lin 4, col 17) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : emit'(drop(e))\n" +
        //        " v  anon : (lin 3, col 38) : block escape throw : cannot copy reference out\n") { out }
    }
    @Test
    fun jj_02_task_nest() {
        val out = test(
            """
            spawn( func (v1) {
                spawn (func (v2) {
                    spawn( func (v3) {
                        print(v1,v2,v3)
                        nil
                    })(3)
                })(2)
            })(1)
        """
        )
        //assert(out == "1\t2\t3\n" +
        //        " |  anon : (lin 2, col 19) : (task (v1) { spawn (task (v2) { spawn (task (...)\n" +
        //        " |  anon : (lin 3, col 23) : (task (v2) { spawn (task (v3) { print(v1,v2...)\n" +
        //        " v  anon : (lin 3, col 33) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "1\t2\t3\n") { out }
    }

    // PUB

    @Test
    fun kk_07_pub_tag() {
        val out = test(
            """
            data :X = [x]
            val T = func () :X {
                set pub = [10]
                print(pub.x)
            }
            spawn T()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_08_pub_tag() {
        val out = test(
            """
            data :Z = [z]
            data :X = [x:Z]
            val T = func () :X {
                set pub = [[10]]
                print(pub.x.z)
            }
            spawn T()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_09_pub_tag() {
        val out = test(
            """
            data :X = [x]
            val T = func () :X {
                set pub = [10]
                await(true)
            }
            val t :X = spawn T()
            print(t.pub.x)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_10_pub_tag() {
        val out = test(
            """
            data :Z = [z]
            data :X = [x:Z]
            val T = func () :X {
                set pub = [[10]]
                await(true)
            }
            val t :X = spawn T()
            print(t.pub.x.z)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_11_task_tag_err() {
        val out = test(
            """
            val T = func () :X {
                nil
            }
            print(:ok)
        """
        )
        assert(out == "anon : (lin 2, col 30) : declaration throw : data :X is not declared\n") { out }
        //assert(out == ":ok\n") { out }
    }
    @Test
    fun kk_12_pub() {
        val out = test(
            """
            pub.x
        """
        )
        assert(out == "anon : (lin 2, col 13) : pub throw : expected enclosing task\n") { out }
    }
    @Test
    fun kk_13_pub() {
        val out = test(
            """
            spawn (func () {
                set pub.x = 10
                nil
            }) ()
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : (spawn (func () { (set pub[:x] = 10); nil...\n" +
                    " |  anon : (lin 3, col 21) : pub[:x]\n" +
                    " v  throw : expected collection\n"
        ) { out }
    }
    @Test
    fun kk_14_pub() {
        val out = test(
            """
            val t = spawn (func () {
                set pub = await(true)
                ;;set pub = evt
                pub
            }) ()
            do {
                val x
                emit([:x])
            }
            print(t.pub)
        """
        )
        //assert(out == " |  anon : (lin 9, col 17) : emit'([],:task)\n" +
        //        " v  anon : (lin 4, col 21) : set throw : cannot hold alien reference\n") { out }
        assert(out == "[:x]\n") { out }
    }
    @Test
    fun kk_15_pub() {
        val out = test(
            """
            val t = spawn (func () {
                set pub = 0
                await(true)
            }) ()
            set t.pub = 10
            print(t.pub)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_16_pub() {
        val out = test("""
            var t
            set t = func (v1) {
                set pub = v1
                val evt = await(true)
                set pub = pub + evt
                pub
            }
            var a = spawn (t) (1)
            print(a.pub)
            emit(2) in a
            print(a.pub)
        """, true)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun kk_17_pub_err() {
        val out = test("""
            val t = func () {
                set pub = []
            }
            var a = spawn (t) ()
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
        //assert(out == "anon : (lin 11, col 25) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set throw : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 5, col 28) : t()\n" +
        //        "anon : (lin 2, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun kk_18_pub_err() {
        val out = test("""
            val T = func () {
                set pub = []
                await(true)
            }
            var x
            do {
                var a = spawn (T) ()
                set x = a.pub
                nil
            }
            print(x)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 9, col 21) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == "anon : (lin 11, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 8, col 32) : t()\n" +
        //        "anon : (lin 3, col 29) : set throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun kk_19_pub() {
        val out = test("""
            var t
            set t = func () {
                ;;;set pub =;;; 10
            }
            var a = spawn t()
            print(a.pub + a.pub)
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun kk_20_pub_pool() {
        val out = test("""
            var T
            set T = func () {
                set pub = [10]  ;; valgrind test
            }
            spawn T()
            print(1)
        """)
        assert(out == "1\n") { out }
        //assert(out == "anon : (lin 6, col 19) : T()\n" +
        //        "anon : (lin 3, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun kk_21_pub_pool() {
        val out = test("""
            var T
            set T = func () {
                ;;;do;;; pub ;; useless test
                nil
            }
            spawn T()
            print(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun kk_22_pub_err() {
        val out = test("""
            var T
            set T = func () {
                set ;;;task.;;;pub = [10]
                await(true)
            }
            var y
            do {
                var t = spawn (T) ()
                var x
                set x = t.pub  ;; pub expose
                set y = t.pub  ;; incompatible scopes
            }
            print(999)
        """)
        assert(out == "999\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set throw : incompatible scopes\n:throw\n") { out }
        //assert(out == "anon : (lin 13, col 27) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
    }
    @Test
    fun kk_23_pub_index_err() {
        val out = test("""
            var T
            set T = func () {
                set ;;;task.;;;pub = [10]
                await(true)
            }
            var t = spawn(T)()
            var x
            set x = t.pub   ;; no expose
            print(x)
        """)
        assert(out == "[10]\n") { out }
        //assert(out == "anon : (lin 9, col 17) : set throw : incompatible scopes\n:throw\n") { out }
        //assert(out == "anon : (lin 11, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
    }
    @Test
    fun kk_24_pub_index_err() {
        val out = test("""
            var T
            set T = func () {
                set ;;;task.;;;pub = [10]
                await(true)
            }
            var t = spawn(T)()
            print(t.pub)   ;; no expose
        """)
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        assert(out == "[10]\n") { out }
    }
    @Test
    fun kk_25_pub_index_err() {
        val out = test("""
            var T
            set T = func () {
                set ;;;task.;;;pub = [[@[(:x,10)]]]
                await(true)
            }
            var t = spawn T ()
            print(t.pub[0][0][:x])   ;; no expose
        """)
        assert(out == "10\n") { out }
        //assert(out == "anon : (lin 10, col 27) : invalid index : cannot expose dynamic \"pub\" field\n:throw\n") { out }
    }
    @Test
    fun kk_27_pub_task_err() {
        val out = test("""
        spawn func () { 
            var y
            set y = do {     
                var ceu_spw_54     
                set ceu_spw_54 = spawn func () {         
                    set ;;;task.;;;pub = [2]             
                    await(true)         
                }()        
                ;;await(true)     
                ;;print(ceu_spw_54.pub)     
                ceu_spw_54.pub        
            }     
            print(y) 
        }()
        emit (nil)
        """)
        assert(out == "[2]\n") { out }
       // assert(out == "anon : (lin 2, col 15) : (task () :fake { var y set y = do { var ceu_s...)\n" +
       //         "anon : (lin 4, col 21) : block escape throw : incompatible scopes\n" +
       //         ":throw\n") { out }
        //assert(out == "anon : (lin 16, col 9) : emit nil\n" +
        //        "anon : (lin 12, col 28) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
    }
    @Test
    fun kk_28_pub_err() {
        val out = test("""
            var t
            set t = func (v) {
                set ;;;task.;;;pub = v
                var evt = await(true)
                var x
                set x = [2]
                set ;;;task.;;;pub = x
                set evt = await(true)
                set ;;;task.;;;pub = @[(:y,copy(evt))]
                ;;;move;;;(;;;task.;;;pub)
            }
            var a = spawn (t) ([1])
            print(a.pub)
            emit(true) in a
            print(a.pub)
            emit([3]) in a
            print(a.pub)
        """, true)
        assert(out == "[1]\n[2]\n@[(:y,[3])]\n") { out }
        //assert(out == "TODO\n") { out }
    }
    @Test
    fun kk_29_pub_out() {
        val out = test("""
            var t = func () {
                set ;;;task.;;;pub = []
                await(true)
                nil
            }
            var a = spawn (t) ()
            print(a.pub)
            a.pub
        """)
        assert(out == "[]\n") { out }
    }
    @Test
    fun kk_30_pub_out_err() {
        val out = test("""
            var t = func () {
                set ;;;task.;;;pub = []
                ;;;task.;;;pub
            }
            var a = spawn (t) ()
            print(a.pub)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 6, col 28) : t()\n" +
        //        "anon : (lin 2, col 29) : block escape throw : incompatible scopes\n" +
        //        ":throw\n") { out }
    }

    // ORIGINAL / PUB / EXPOSE

    @Test
    fun kj_01_expose() {
        val out = test(
            """
            var t = func () {
                set pub = []
                await(true)
                nil
            }
            var a = spawn (t) ()
            var x = a.pub
            print(x)
        """
        )
        //assert(out == "anon : (lin 8, col 13) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        assert(out == "[]\n") { out }
        //assert(out == " v  anon : (lin 8, col 13) : declaration throw : cannot copy reference out\n") { out }
    }
    @Test
    fun kj_02_expose_err() {
        val out = test(
            """
            val x = do {
                var t = func () {
                    set pub = []
                    await(true)
                    nil
                }
                var a = spawn (t) ()
                a.pub
            }
            print(x)
        """
        )
        //assert(out == " v  anon : (lin 2, col 21) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " v  anon : (lin 2, col 21) : block escape throw : reference has immutable scope\n") { out }
        assert(out == "[]\n") { out }
    }
    @Test
    fun kj_03_expose_err() {
        val out = test(
            """
            var f = func' (t) {
                do { do {
                var p = t.pub   ;; ok
                set p = t.pub   ;; ok  ;;ERROR: spawn A and f() are not comparable
                set p = p       ;; ok
                print(p)      ;; ok
                ;;p               ;; ok
                nil
                } }
            }
            var t = func () {
                set pub = []
                await(true)
                nil
            }
            var a = spawn (t) ()
            f(a)
            nil
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 15, col 13) : f(a)\n" +
        //        "anon : (lin 2, col 30) : block escape throw : incompatible scopes\n" +
        //        "[]\n" +
        //        ":throw\n") { out }
        //assert(out == "anon : (lin 15, col 13) : f(a)\n" +
        //        "anon : (lin 3, col 17) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " |  anon : (lin 18, col 13) : f(a)\n" +
        //        " v  anon : (lin 4, col 17) : declaration throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 18, col 13) : f(a)\n" +
        //        " v  anon : (lin 5, col 21) : set throw : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun kj_05_expose_err() {
        val out = test(
            """
            var f = func' (t) {
                var p = t.pub   ;; ok
                p               ;; ok
            }
            var t = func () {
                set pub = []
                await(true)
                nil
            }
            var a = spawn (t) ()
            var x = f(a)        ;; no
            print(x)
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 12, col 21) : f(a)\n" +
        //        "anon : (lin 2, col 30) : block escape throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == "anon : (lin 12, col 21) : f(a)\n" +
        //        "anon : (lin 3, col 17) : declaration throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " |  anon : (lin 12, col 21) : f(a)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 12, col 21) : f(a)\n" +
        //        " v  anon : (lin 2, col 30) : block escape throw : reference has immutable scope\n") { out }
    }
    @Test
    fun kj_06_expose_err() {
        val out = test(
            """
            var p
            var f = func' (t) {
                set p = t.pub   ;; no
            }
            var t = func () {
                set pub = []
                await(true)
                nil
            }
            var a = spawn (t) ()
            val x = f(a)
            print(x)
        """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 12, col 13) : f(a)\n" +
        //        "anon : (lin 4, col 21) : set throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " |  anon : (lin 12, col 21) : f(a)\n" +
        //        " v  anon : (lin 4, col 21) : set throw : cannot assign reference to outer scope\n") { out }
    }
    @Test
    fun TODO_kj_07_pub_func() {
        val out = test(
            """
            var t
            set t = func (v) {
                set pub = v
                var f
                set f = func' () {
                    pub           ;; TODO: crosses func
                }
                print(f())
            }
            var a = spawn (t)(1)
        """
        )
        //assert(out == "1\n") { out }
        assert(out == "anon : (lin 7, col 21) : pub throw : expected enclosing task\n") { out }
    }
    @Test
    fun TODO_kj_08_pub_func_expose() {
        val out = test(
            """
            var t = func (v) {
                set pub = v
                var f = func' () {
                    pub   ;; should be ok bc f is called inside t
                }
                print(f())
            }
            var a = spawn (t) ([1])
        """
        )
        assert(out == "anon : (lin 5, col 21) : pub throw : expected enclosing task\n") { out }
        //assert(out == "[1]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
    }
    @Test
    fun kj_09_pub() {
        val out = test(
            """
            var T = func (v) {
                set pub = @[]
                nil
            }
            val t = spawn T()
            print(status(t))
        """
        )
        assert(out == ":terminated\n") { out }
    }
    @Test
    fun kj_10_pub_expose() {
        val out = test(
            """
            val f = func' (t) {
                print(t)
            }
            val T = func () {
                set pub = []
                await(true)
                nil
            }
            val t = spawn T()
            f(t.pub)
            print(:ok)
        """
        )
        assert(out == "[]\n:ok\n") { out }
    }
    @Test
    fun kj_12_pub_pass() {
        val out = test("""
            val S = func (t) {
                print(t)
                await(true)
            }
            val T = func () {
                set ;;;task.;;;pub = [1,2,3]
                spawn S(;;;task.;;;pub)
                nil
            }
            spawn T()
        """)
        assert(out == "[1,2,3]\n") { out }
    }
    @Test
    fun kj_13_pub_err_expose() {
        val out = test("""
            var t
            set t = func () {
                set pub = []
                pub
            }
            var a = spawn (t) ()
            var x
            set x = a.pub   ;; seria bom que o pub guardasse o retorno,
                            ;; uma vez que ele já se "desfez" (gc_dec) do pub
                            ;; poderia ser uma union pub/ret
            print(x)
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 6, col 28) : t()\n" +
        //        "anon : (lin 3, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun kj_14_pub_err_expose() {
        val out = test("""
            var t
            set t = func () {
                set ;;;task.;;;pub = @[]
                pub
            }
            var a = spawn (t) ()
            var x
            set x = a.pub
            print(x)
        """)
        assert(out == "@[]\n") { out }
        //assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic \"pub\" field\n") { out }
        //assert(out == "anon : (lin 6, col 28) : t()\n" +
        //        "anon : (lin 3, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun kj_15_pub_err_expose() {
        val out = test("""
            var t
            set t = func () {
                set ;;;task.;;;pub = #[]
                pub
            }
            var a = spawn (t) ()
            var x
            set x = a.pub
            print(x)
        """)
        assert(out == "#[]\n") { out }
        //assert(out == "anon : (lin 6, col 28) : t()\n" +
        //        "anon : (lin 3, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun kj_16_pub_nopub() {
        val out = test("""
            var U
            set U = func () {
                set pub = func' () {
                    10
                }
                pub
            }
            var T
            set T = func (u) {
                print(type(u.pub))
            }
            spawn T (spawn U())
            print(:ok)
        """)
        assert(out == ":func\n:ok\n") { out }
        //assert(out == "anon : (lin 12, col 28) : U()\n" +
        //        "anon : (lin 3, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun kj_17_pub_nopub() {
        val out = test("""
            var U
            set U = func () {
                set ;;;task.;;;pub = [10]
            }
            var T
            set T = func (u) {
                nil ;;print(u.pub.0)
            }
            spawn T (spawn U())
            print(:ok)
        """)
        //assert(out == "anon : (lin 10, col 28) : U()\n" +
        //        "anon : (lin 3, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun kj_18_pub_nopub() {
        val out = test("""
            var U
            set U = func () {
                var x
                set x = [10]
            }
            var T
            set T = func (u) {
                nil ;;print(u.pub.0)
            }
            spawn T (spawn U())
            print(:ok)
        """)
        //assert(out == "anon : (lin 11, col 28) : U()\n" +
        //        "anon : (lin 3, col 29) : block escape throw : incompatible scopes\n:throw\n") { out }
        assert(out == ":ok\n") { out }
    }
    @Test
    fun kj_19_func_expose() {
        val out = test("""
            var t = func (v) {
                set ;;;task.;;;pub = v
                var f = func' (p) {
                    p
                }
                print(f(;;;task.;;;pub))
            }
            var a = spawn (t) ([1])
        """, true)
        assert(out == "[1]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
    }
    @Test
    fun kj_20_pub_func_tst() {
        val out = test("""
            var t = func (v) {
                set ;;;task.;;;pub = v
                var xxx
                nil
            }
            var a = spawn (t) ([])
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun kj_21_pub_func_expose() {
        val out = test("""
            var f = func' (t) {
                t.pub
            }
            var T = func () {
                set ;;;task.;;;pub = []
                await(true)
            }
            var t = spawn (T) ()
            print(f(t))
        """)
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 13, col 20) : a([1])\n" +
        //        "anon : (lin 9, col 25) : f()\n" +
        //        "anon : (lin 7, col 26) : invalid pub : cannot expose dynamic \"pub\" field\n:throw\n") { out }
        //assert(out == "anon : (lin 10, col 21) : f(t)\n" +
        //        "anon : (lin 2, col 30) : block escape throw : incompatible scopes\n" +
        //        ":throw\n") { out }
    }

    // PUB / DATA

    @Test
    fun BUG_ki_01_data_pub() {
        val out = test("""
            data :T = [x,y]
            var t :T = spawn func () {
                set ;;;task.;;;pub = [10,20]
                nil
            } ()
            data :X = [a:T]
            var x :X = [t]
            print(x.a.pub.y)  // TODO: combine Pub/Index
        """, true)
        assert(out == "20\n") { out }
    }

    // TOGGLE

    @Test
    fun pp_01_toggle_err() {
        val out = test(
            """
            toggle 1(true)
        """
        )
        assert(
            out == " |  anon : (lin 2, col 13) : (toggle 1(true))\n" +
                    " v  throw : expected yielded task\n"
        ) { out }
    }
    @Test
    fun pp_02_toggle() {
        val out = test(
            """
            val T = func () {
                await(true)
                print(10)
            }
            val t = spawn T()
            toggle t (false)
            print(1)
            emit(true)
            emit(true)
            toggle t (true)
            print(2)
            emit(true)
        """
        )
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun pp_03_toggle_defer() {
        val out = test(
            """
            var T
            set T = func () {
                defer {
                    print(10)
                }
                ${AWAIT()}
                print(999)
            }
            var t
            set t = spawn T()
            toggle t (false)
            print(1)
            emit (nil)
            print(2)
        """
        )
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun pp_04_toggle_err() { // should be rt throw
        val out = test(
            """
            val T = func () {
                nil
            }
            val t = spawn T()
            toggle t (false)
        """
        )
        assert(
            out == " |  anon : (lin 6, col 13) : (toggle t(false))\n" +
                    " v  throw : expected yielded task\n"
        ) { out }
    }
    @Test
    fun pp_05_toggle_nest() {
        val out = test(
            """
            val T = func () {
                spawn (func () {
                    ${AWAIT()}
                    print(3)
                }) ()
                ${AWAIT()}
                print(4)
            }
            print(1)
            val t = spawn T()
            toggle t (false)
            emit (nil)
            print(2)
            toggle t (true)
            emit (nil)
        """
        )
        assert(out == "1\n2\n3\n4\n") { out }
    }

    // ORIGINAL

    @Test
    fun zz_12_bcast() {
        val out = test(
            """
        """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_13_bcast() {
        val out = test(
            """
            var T = func () {
                do {
                    print(yield(nil))
                }
            }
            var t = spawn T()
            ;;print(:1111)
            emit ([])
            ;;print(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 9, col 13) : emit []\n" +
        //        "anon : (lin 4, col 17) : declaration throw : incompatible scopes\n:throw\n") { out }
    }
    @Test
    fun zz_14_bcast() {
        val out = test(
            """
            var T = func () {
                do {
                    var v =
                        await(true) ;;thus { it => it }
                    print(v)
                }
            }
            var t = spawn T()
            ;;print(:1111)
            var e = []
            emit (e)
            ;;print(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : emit e\n" +
        //        " v  anon : (lin 5, col 25) : resume throw : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : emit'(e)\n" +
        //        " v  anon : (lin 5, col 36) : block escape throw : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_15_bcast_err() {
        val out = test(
            """
            var T = func () {
                var v = await(true)
                print(v)
            }
            var t = spawn T()
            do {
                val a
                do {
                    val b
                    do {
                        var e = []
                        emit (e)
                    }
                }
            }
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == "anon : (lin 10, col 35) : emit throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " |  anon : (lin 9, col 13) : emit e\n" +
        //        " v  anon : (lin 3, col 25) : resume throw : incompatible scopes\n") { out }
        //assert(out == " |  anon : (lin 9, col 17) : emit e\n" +
        //        " v  anon : (lin 3, col 25) : resume throw : cannot receive assigned reference\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : emit'(e)\n" +
        //        " v  anon : (lin 3, col 36) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : emit'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 13, col 25) : emit'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot hold alien reference\n") { out }
    }
    @Test
    fun zz_15_bcast_okr() {
        val out = test(
            """
            var T = func () {
                print(yield(nil))
            }
            var t = spawn T()
            do {
                var e = []
                emit (e)
            }
            """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_16_bcast_err() {
        val out = test(
            """
            var T = func () {
                var v =
                    func' (it) {it} (yield(nil))
                print(v)
            }
            var t = spawn T()
            ;;print(:1111)
            do {
                val a
                do {
                    val b
                    var e = []
                    emit (e)
                }
            }
            ;;print(:2222)
            """
        )
        assert(out == "[]\n") { out }
        //assert(out == " |  anon : (lin 11, col 17) : emit e\n" +
        //        " v  anon : (lin 4, col 17) : resume throw : cannot receive assigned reference\n") { out }
        //assert(out == "anon : (lin 11, col 39) : emit throw : incompatible scopes\n" +
        //        ":throw\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : emit'(e,:task)\n" +
        //        " v  anon : (lin 4, col 28) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : emit'(e,:task)\n" +
        //        " |  anon : (lin 4, col 17) : (func' (it) { it })(yield(nil))\n" +
        //        " v  anon : (lin 4, col 27) : block escape throw : cannot copy reference out\n") { out }
        //assert(out == " |  anon : (lin 14, col 21) : emit'(e,:task)\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot hold alien reference\n") { out }
    }
    @Test
    fun zz_17_bcast() {
        DEBUG = true
        val out = test(
            """
            var T1 = func () {
                await(true) ;;thus { it => nil}
                spawn( func () {                ;; GC = task (no more)
                    val xevt = await(true) ;;thus { it => it}
                    print(:1)
                    var v = xevt
                } )()
                nil
            }
            var t1 = spawn T1()
            var T2 = func () {
                await(true) ;;thus { it => nil}
                val xevt = await(true) ;;thus { it => nil}
                ;;print(:2)
                do {
                    var v = xevt
                    ;;print(:evt, v, xevt)
                }
            }
            var t2 = spawn T2()
            emit ([])                      ;; GC = []
            print(`:number CEU_GC.free`)
            """
        )
        assert(out == "2\n") { out }
        //assert(out == "anon : (lin 20, col 13) : emit []\n" +
        //        "anon : (lin 16, col 17) : declaration throw : incompatible scopes\n" +
        //        ":2\n" +
        //        ":throw\n") { out }
    }
    @Test
    fun zz_18_bcast_tuple_func_ok() {
        val out = test(
            """
            var fff = func' (v) {
                print(v)
            }
            var T = func () {
                fff(yield(nil))
            }
            spawn T()
            emit ([1])
        """
        )
        assert(out == "[1]\n") { out }
    }
    @Test
    fun zz_19_bcast_tuple_func_no() {
        val out = test(
            """
            var f = func' (v) {  ;; *** v is no longer fleeting ***
                val x = v[0]    ;; v also holds x, both are fleeting -> unsafe
                print(x)      ;; x will be freed and v would contain dangling pointer
            }
            var T = func () {
                f(yield(nil)) ;;thus { it => it})
            }
            spawn T()
            emit ([[1]])
        """
        )
        //assert(out == " |  anon : (lin 10, col 13) : emit'([[1]])\n" +
        //        " v  anon : (lin 7, col 30) : block escape throw : cannot copy reference out\n") { out }
        assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 10, col 13) : emit'([[1]])\n" +
        //        " |  anon : (lin 7, col 17) : f((await(true) thus { it => it }) )\n" +
        //        " v  anon : (lin 3, col 17) : declaration throw : cannot move pending reference in\n") { out }
    }
    @Test
    fun zz_19_bcast_tuple_func_ok_not_fleet() {
        val out = test(
            """
            var f = func' (v) {
                val x = v[0]    ;; v also holds x, both are fleeting -> unsafe
                print(x)      ;; x will be freed and v would contain dangling pointer
            }
            var T = func () {
                val xevt = await(true) ;;thus { it => it}   ;; NOT FLEETING (vs prv test)
                f(xevt)
            }
            spawn T()
            emit ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 11, col 13) : emit [[1]]\n" +
        //        " v  anon : (lin 7, col 17) : declaration throw : cannot hold event reference\n") { out }
        //assert(out == " |  anon : (lin 11, col 13) : emit'([[1]])\n" +
        //        " v  anon : (lin 7, col 38) : block escape throw : cannot copy reference out\n") { out }
    }
    @Test
    fun zz_20_bcast_tuple_func_ok() {
        val out = test(
            """
            var f = func' (v) {
                print(v[0])
            }
            var T = func () {
                f(yield(nil))
            }
            spawn T()
            emit ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 11, col 13) : emit'([[1]])\n" +
        //        " |  anon : (lin 8, col 44) : f(it)\n" +
        //        " v  anon : (lin 3, col 27) : declaration throw : cannot move pending reference in\n") { out }
    }
    @Test
    fun zz_20_bcast_tuple_func_no() {
        val out = test(
            """
            val f = func' (v) {
                func' (x) {
                    val y = x
                    print(y)
                } (v[0])
            }
            var T = func () {
                f(yield(nil))
            }
            spawn T()
            emit ([[1]])
        """
        )
        assert(out == "[1]\n") { out }
        //assert(out == " |  anon : (lin 12, col 13) : emit'([[1]])\n" +
        //        " |  anon : (lin 9, col 44) : f(it)\n" +
        //        " v  anon : (lin 4, col 21) : declaration throw : cannot move pending reference in\n") { out }
    }
    @Test
    fun zz_20_bcast_tuple_func_nox() {
        val out = test(
            """
            val f = func' (v) {
                val g = func' (x) {
                    print(x)
                    x
                }
                g(v[0])
            }
            var T = func () {
                f(yield(nil)) ;;thus { it => f(it) }
            }
            spawn T()
            emit ([[1]])
        """
        )
        assert(out == "[1]\n")
        //assert(out == " |  anon : (lin 12, col 13) : emit'([[1]])\n" +
        //        " |  anon : (lin 9, col 44) : f(it)\n" +
        //        " |  anon : (lin 6, col 17) : g(v[0])\n" +
        //        " v  anon : (lin 3, col 31) : argument throw : cannot move pending reference in\n") { out }
    }
    @Test
    fun zz_21_bcast_tuple_func_ok() {
        val out = test(
            """
            val f = func' (v) {
                val x = v[0]
                print(x)
            }
            var g = func' (v) {
                val xevt = v
                f(xevt)
            }
            g([[1]])
        """
        )
        assert(out == "[1]\n") { out }
    }
    @Test
    fun zz_22_pool_throw() {
        val out = test(
            """
            print(1)
            catch :ok ;;;(it| it==:ok);;; {
                print(2)
                spawn (func () {
                    print(3)
                    ${AWAIT()}
                    print(6)
                    throw(:ok)
                }) ()
                spawn (func () {
                    print(4)
                    ${AWAIT()}
                    print(999)
                }) ()
                print(5)
                emit(true)
                print(9999)
            }
            print(7)
        """
        )
        assert(out == "1\n2\n3\n4\n5\n6\n7\n") { out }
    }
    @Test
    fun zz_23_valgrind() {
        val out = test(
            """
            spawn( func () {
                val t = []
                spawn( func () {
                    await(true)
                    print(t)
                }) ()
                await(true)
                nil
            }) ()
            emit ([])
        """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun zz_24_valgrind() {
        val out = test(
            """
            spawn (func () {
                do {
                    await(true)
                    nil
                }
                val y
            }) ()
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_25_escape_break () {
        val out = test(
            """
            spawn (func () {
                print(enclose' :break { loop' {
                    val t = [10]
                    if t[0] {
                        escape(:break,t[0])
                    } else {nil}
                    await(true)
                }})
            }) ()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun zz_26_xceu() {
        val out = test("""
            spawn func () {
                do {
                    spawn func () {
                        await(true)
                    } ()
                    await(true)
                }
                emit( [[]] )
                ;;await true
            }()
            emit (nil)
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_27_xceu() {
        val out = test("""
            spawn func () {
                do {
                    spawn func () {
                        await(true)
                    } ()
                    await(true)
                }
                emit( tag(:x, []) )
            }()
            emit (nil)
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_28_xceu () {
        val out = test("""
            spawn func () {
                do {
                    spawn func () {
                        await(true)
                    }()
                    await(true)
                    nil
                }
                do {
                    nil
                }
            } ()
            emit (nil)
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_29_xceu () {
        val out = test("""
            spawn func () {
                loop' {
                    var evt = await(true);
                    enclose' :break {
                        loop' {
                            if evt==10 {
                                escape(:break,nil)
                            } else {nil}
                            set evt = await(true)
                        }
                    }
                    print(:1)
                    var t = spawn func () {
                        var evt2 = await(true);
                        enclose' :break {
                            loop' {
                                if evt2==10 {
                                    escape(:break,nil)
                                } else {nil}
                                set evt2 = await(true)
                            }
                        }
                    } ()
                    enclose' :break {
                        loop' {
                            if status(t)==:terminated {
                                escape(:break,nil)
                            } else {nil}
                            set evt = await(true)
                            delay
                        }
                    }
                    print(:2)
                }
            } ()
            emit (10)    ;; :1
            emit (10)    ;; :2 (not :1 again)
        """, true)
        assert(out == ":1\n:2\n") { out }
    }
    @Test
    fun zz_31_kill() {
        val out = test("""
            spawn func () {
                do {
                    val t1 = spawn func () {
                        ${AWAIT()}
                        print(1)
                    } ()
                    spawn func () {
                        defer { print(3) }
                        ${AWAIT()}
                        print(2)
                    } ()
                    ${AWAIT ("t1")}
                }
                print(999)
            } ()
            emit (nil)
        """, true)
        assert(out == "1\n3\n999\n") { out }
    }
    @Test
    fun zz_32_all() {
        val out = test("""
            val T = (func () {
                set ;;;task.;;;pub = []
                await(true)
            })
            val f = (func' (v) {
                nil
            })
            do {
                val xxx = spawn T()
                do {
                    val zzz = xxx
                    nil
                }
                f(xxx.pub)
            }
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }

    // ORIGINAL / DATA / EVT

    @Test
    fun z1_01_data_await() {
        val out = test(
            """
            data :E = [x,y]
            spawn (func () {
                func' (it :E) {
                    print(it.x)
                } (yield(nil))
            } )()
            emit (tag(:E, [10,20]))
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun z1_02_data_await() {
        val out = test(
            """
            data :E = [x,y]
            data :F = [i,j]
            spawn (func () {
                func' (it :E) {
                    print(it.x)
                } (yield(nil))
                func' (it :F) {
                    print(it.j)
                } (yield(nil))
            } )()
            emit (tag(:E, [10,20]))
            emit (tag(:F, [10,20]))
        """
        )
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun z1_03_data_pub_err() {
        val out = test(
            """
            func () :T { nil }
            print(:ok)
        """
        )
        assert(out == "anon : (lin 2, col 22) : declaration throw : data :T is not declared\n") { out }
        //assert(out == ":ok\n") { out }
    }
    @Test
    fun z1_04_data_pub() {
        val out = test(
            """
            data :T = [x,y]
            spawn( func () :T {
                set pub = [10,20]
                print(pub.x)
            }) ()
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun z1_05_data_pub_err() {
        val out = test(
            """
            var t = spawn (func () {
                await(true)
            } )()
            print(t.pub.y)
        """
        )
        assert(
            out == " |  anon : (lin 5, col 21) : t.pub[:y]\n" +
                    " v  throw : expected collection\n"
        ) { out }
    }
    @Test
    fun z1_06_data_pub() {
        val out = test(
            """
            data :T = [x,y]
            var t :T = spawn( func () {
                set pub = [10,20]
                await(true)
            }) ()
            print(t.pub.y)
        """
        )
        assert(out == "20\n") { out }
    }

    // EXTRA

    @Test
    fun z2_01_valgrind() {
        val out = test(
            """
            spawn (func () {
                spawn (func () {
                    await(true)
                    await(true)
                    await(true)
                    print(:1)
                }) ()
                spawn (func () {
                    await(true)
                    print(:2)
                }) ()
                spawn (func () {
                    await(true)
                    await(true)
                    print(:3)
                }) ()
                await(true)
                await(true)
                print(:ok)
            }) ()
            emit(true)
        """
        )
        assert(out == ":2\n:3\n:1\n:ok\n") { out }
    }
    @Test
    fun z2_02_parand() {
        val out = test(
            """
            do {
                spawn (func () {
                    func' (it) {
                        false
                    } (yield(nil))
                    print(999)
                }) (nil)
                val x = spawn (func () {
                    nil
                }) (nil)
                nil
            }
        """
        )
        assert(out == "999\n") { out }
    }
    @Test
    fun z2_03_nested_func() {
        val out = test(
            """
            val T = func (x) {
                val f = func' () {
                    x
                }
                await(true)
                print(f())
            }
            spawn T([])
            emit(true)
        """
        )
        assert(out == "[]\n") { out }
    }
    @Test
    fun z2_04_nested_func() {
        val out = test(
            """
            val T = func (x) {
                await(true)
                val y = 10
                val f = func' () {
                    y
                }
                print(f())
            }
            spawn T([])
            emit(true)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun z2_03_skip_valgrind() {
        val out = test(
            """
            spawn (func () {
                spawn (func () {
                    loop' {
                        enclose' :skip {
                            await(true)
                            if (func' () {
                                true
                            } ()) {
                                escape(:skip,nil)
                            } else {nil}
                        }
                    }
                }) ()
                spawn (func () {
                    nil
                }) ()
                await(true)
            }) ()
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun z2_04_op_is() {
        val out = test(
            """
            print(is?(1,  :number))
            print(is?(:x, :number))
        """, true
        )
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun z2_05_99() {
        val out = test(
            """
            spawn (func () {
                spawn (func () {
                    await(true)
                }) ()
                spawn (func () {
                    await(true)
                }) ()
                enclose' :break {
                    loop' {
                        await(true)
                        escape(:break, nil) ;; if true
                    }
                }
            }) ()
            emit(true)
            print(:ok)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun zz_08_99() {
        val out = test("""
            val T = func (x, y) {
                print(:a, x)
                await(true)
                print(:b, x)
            }            
            spawn T(1, 2)
            emit(10)
        """)
        assert(out == ":a\t1\n:b\t1\n") { out }
    }
    @Test
    fun zz_09_99_double_awake() {
        val out = test("""
            spawn (func () {
                loop' {
                    await(true) ; delay
                    print(false)
                    val t = spawn (func () {
                        await(true) ; delay
                    }) ()
                    ${AWAIT("t")}
                    delay
                    print(true)
                    emit(:Y)
                }
            }) ()
            emit(:X)
            emit(:X)
        """)
        assert(out == "false\ntrue\n") { out }
    }
    @Test
    fun zz_10_optim() {
        val out = test("""
            spawn (func () {
                print(enclose' :break {
                    loop' {
                        await(true)
                        escape(:break, nil) ;; if true
                    }
                    delay
                    nil
                })
            }) ()
            emit(true)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun zz_11_valgrind() {
        val out = test("""
            spawn (func () {
                spawn (func () {
                    loop' {
                        do {
                            (var it)
                            (set it = yield(nil))
                        }
                    }
                }) ()
                spawn (func () {
                    nil
                }) ()
                await(true)
            }) ()
            print(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
}

