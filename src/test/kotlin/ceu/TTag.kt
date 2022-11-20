package ceu

import org.junit.Ignore
import org.junit.Test

class TTag {

    @Ignore
    @Test
    fun tag1() {
        val out = all("""
            println(#number ==  #number) 
            println(#bool   /=  #bool) 
            println(#task   ~== #coro) 
            println(#tuple  ~/= #nil) 
            println(#pointer /=  #bool) 
        """)
        assert(out == "true\nfalse\nfalse\ntrue\ntrue\n") { out }
    }

    @Ignore
    @Test
    fun tag2() {
        val out = all("""
            println(tags(#number))
            println(tags(1))
            println(tags(1) == tags(2))
            println(tags([]))
            tags([], #nil, #coro)
            println(tags([1,2,3]))
        """)
        assert(out == "[]\n[#number]\ntrue\n[]\n[#nil,#coro]\n") { out }
    }
}
