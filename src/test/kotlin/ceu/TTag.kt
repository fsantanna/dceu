package ceu

import org.junit.Test

class TTag {

    @Test
    fun tag1() {
        val out = all("""
            println(@number ==  @number) 
            println(@bool   /=  @bool) 
            println(@task   ~== @coro) 
            println(@tuple  ~/= @nil) 
        """)
        assert(out == "true\nfalse\nfalse\ntrue\n") { out }
    }

    @Test
    fun tag2() {
        val out = all("""
            println(tags(@number))
            println(tags(1))
            println(tags(1) == tags(2))
            println(tags([]))
            tags([], @nil, @coro)
            println(tags([1,2,3]))
        """)
        assert(out == "[]\n[@number]\ntrue\n[]\n[@nil,@coro]\n") { out }
    }
}
