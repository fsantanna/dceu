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
}
