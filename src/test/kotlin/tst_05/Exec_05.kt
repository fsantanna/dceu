package tst_05

import dceu.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec_05 {
    // TASKS

    @Test
    fun kk_01_tasks() {
        val out = test("""
            println(tasks())
        """)
        assert(out.contains("tasks: 0x")) { out }
    }
    @Test
    fun kk_02_tasks() {
        val out = test("""
            println(type(tasks()))
        """)
        assert(out.contains(":tasks")) { out }
    }
    @Test
    fun kk_03_tasks() {
        val out = test("""
            val T = task () {
                println(:in)
            }
            val ts = tasks()
            spawn in ts, T()
            println(:out)
        """)
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun kk_04_tasks() {
        val out = test("""
            spawn in tasks(), (task () { println(:in) })()
            println(:out)
        """)
        assert(out == ":in\n:out\n") { out }
    }
    @Test
    fun kk_05_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) { nil }
                println(:in)
            }
            val ts = tasks()
            spawn in ts, T()
            println(:out)
            broadcast in :global, nil
        """)
        assert(out == ":out\n:in\n") { out }
    }
    @Test
    fun kk_06_tasks() {
        val out = test("""
            val T = task () {
                nil
            }
            val ts = tasks(1)
            val ok1 = spawn in ts, T()
            val ok2 = spawn in ts, T()
            println(ok1, ok2)
        """)
        assert(out == "true\ttrue\n") { out }
    }
    @Test
    fun kk_07_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) { nil }
            }
            val ts = tasks()
            val ok = spawn in ts, T()
            println(ok)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun kk_08_tasks() {
        val out = test("""
            val T = task () {
                yield(nil) { nil }
            }
            val ts = tasks(1)
            val ok1 = spawn in ts, T()
            val ok2 = spawn in ts, T()
            println(ok1, ok2)
        """)
        assert(out == "true\tfalse\n") { out }
    }
}
