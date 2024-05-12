package dceu

val PLUS = """
    val {{+}} = func (v1, v2) {
        `:number (${D}v1.Number + ${D}v2.Number)`
    }    
    val {{-}} = func (v1, v2) {
        if v2 == nil {
            `:number - ${D}v1.Number`
        } else {
            `:number (${D}v1.Number - ${D}v2.Number)`
        }
    }    
""".replace("\n", " ")

val MULT = """
    val {{*}} = func (v1, v2) {
        `:number (${D}v1.Number * ${D}v2.Number)`
    }    
    val {{/}} = func (v1, v2) {
        `:number (${D}v1.Number / ${D}v2.Number)`
    }    
""".replace("\n", " ")

val COMP = """
    func {{>}} (v1,v2) {
        ifs {
            (type(v1) == :tag)    and (type(v2) == :tag)    { `:bool (${D}v1.Tag    > ${D}v2.Tag)` }
            (type(v1) == :number) and (type(v2) == :number) { `:bool (${D}v1.Number > ${D}v2.Number)` }
            else => error(:error)
        }
    }
    func {{<}} (v1,v2) {
        not ((v1 == v2) or (v1 > v2))
    }
    func {{>=}} (v1,v2) {
        (v1 == v2) or (v1 > v2)
    }
    func {{<=}} (v1,v2) {
        (v1 == v2) or (v1 < v2)
    }
    func {{===}} (v1,v2) {
        (v1 == v2)
    }
""".replace("\n", " ")

fun OR (v1:String, v2:String): String {
    N++
    return "do { val it_$N = $v1 ; if it_$N { it_$N } else { $v2 } }"
}

fun AND (v1:String, v2:String): String {
    N++
    return "do { val it_$N = $v1 ; if it_$N { $v2 } else { $v1 } }"
}

fun AWAIT (v:String="(type(it) /= :exe-task)"): String {
    return """
        loop {
            val it = yield(nil)
            break if ${AND(v, OR("it","true"))}
        }
    """.replace("\n", " ")
}

val IS = """
    func is' (v1,v2) {
        ifs {
            (v1 == v2)         { true  }
            (type(v1) == v2)   { true  }
            (type(v2) == :tag) { sup?(v2, tag(v1)) }
            else => false
        }
    }
""".replace("\n", " ")

val ASR = """
    func assert (v, msg) {
        if v => v => error(msg)
    }    
"""