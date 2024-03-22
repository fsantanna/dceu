package tst_06

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_06 {

    // EXPORT

    @Test
    fun aa_01_export() {
        val out = test("""
            export [a] {
                val a = 10
            }
            export [x] {
                var x
                set x = a
            }
            print(x)
        """)
        assert(out == "10") { out }
    }
    @Test
    fun aa_02_export_err() {
        val out = test("""
            export [] {
                var a       ;; invisible
                set a = 10
            }
            var x
            set x = a
            print(x)
        """)
        assert(out == "anon : (lin 7, col 21) : access error : variable \"a\" is not declared\n") { out }
    }
    @Test
    fun aa_03_export() {
        val out = test("""
            val x = export [] {
                val a = []
                a
            }
            print(x)
        """)
        assert(out == "[]") { out }
    }
    @Test
    fun aa_04_export() {
        val out = test("""
            export [aaa] {
                val aaa = 10
            }
            export [bbb] {
                val bbb = 20
            }
            println(aaa,bbb)
        """)
        assert(out == "10\t20\n") { out }
    }
    @Test
    fun aa_05_export() {
        val out = test("""
            export [f] {
                val v = []
                val f = func () {
                    v
                }
                ;;println(v, f)
            }
            do {
                val x = f
                nil
            }
            println(:ok)
        """)
        assert(out == ":ok\n") { out }
    }
    @Test
    fun aa_06_export() {
        val out = test("""
            do {
                export [f] {
                    val v = []
                    val f = func () {
                        v
                    }
                    ;;println(v, f)
                }
                do {
                    val x = f
                    nil
                }
                println(:ok)
            }
        """)
        assert(out == ":ok\n") { out }
    }

    // TASKS

    @Test
    fun bb_01_group() {
        val out = test("""
            var T
            set T = task (v) {
                println(v)
            }
            var t
            set t = export [] {
                var v
                set v = 10
                spawn T(v)  
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun bb_02_group_no_more_spawn_export() {
        val out = test("""
            export [T] {
                var T
                set T = task (v) {
                    println(v)
                }
            }
            export [t] {
                var t
                set t = spawn export [] {
                    export [v] {
                        var v
                        set v = 10
                    }
                    T(v)
                }
            }
            println(type(t))
        """)
        //assert(out == "10\n:x-task\n") { out }
        assert(out == "anon : (lin 15, col 21) : call error : expected function\n" +
                ":error\n") { out }
    }
    @Test
    fun bb_03_group() {
        val out = test("""
            var f
            set f = func () {
                nil
            }
            spawn task () :fake {
                export [] {
                    f()
                }
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bb_04_group() {
        val out = test("""
            export [f] {
                var cur = nil
                val f = func () {
                    set cur = 65
                    cur
                }
            }
            val co = spawn (coro () {
                yield(nil)
                loop {
                    val v = f()
                    yield(v)
                }
            }) ()
            loop {
                var v = resume co()
                println(v)
                throw(99)
            }
        """)
        assert(out == "anon : (lin 19, col 17) : throw(99)\n" +
                "throw error : uncaught exception\n" +
                "65\n" +
                "99\n") { out }
    }
}
