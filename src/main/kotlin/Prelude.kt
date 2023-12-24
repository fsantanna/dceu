package dceu

val PLUS = """
    val {{+}} = func (v1, v2) {
        `:number (${D}v1.Number + ${D}v2.Number)`
    }    
    val {{-}} = func (v1, v2) {
        `:number (${D}v1.Number - ${D}v2.Number)`
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
            (v1 is? :tag)    and (v2 is? :tag)    { `:bool (${D}v1.Tag    > ${D}v2.Tag)` }
            (v1 is? :number) and (v2 is? :number) { `:bool (${D}v1.Number > ${D}v2.Number)` }
            else => throw(:error)
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
""".replace("\n", " ")

fun OR (v1:String, v2:String): String {
    N++
    return "((func (it_$N) { if it_$N { it_$N } else { $v2 } }) ($v1))"
}

fun AND (v1:String, v2:String): String {
    N++
    return "((func (it_$N) { if it_$N { $v2 } else { $v1 } }) ($v1))"
}

fun AWAIT (v:String="(type(it) /= :exe-task)"): String {
    return """
        loop {
            break if func (it) {
                ${AND(v, OR("it","true"))}
            } (yield(nil))
        }
    """.replace("\n", " ")
}

val IS = """
    func is' (v1,v2) {
        ifs {
            (v1 == v2)         { true  }
            (type(v2) /= :tag) { false }
            (type(v1) == v2)   { true  }
            tags(v1,v2)        { true  }
            else => false
        }
    }
""".replace("\n", " ")

val DETRACK = """
    val detrack'' = func (trk) {
        if detrack'(trk) {
            true
        } else {
            false
        }
    }    
""".replace("\n", " ")

val XAWAIT = """
func await-chk (evt, cnd) {
    ifs {
        (type(cnd) == :tag)      { evt is? cnd }
        (type(cnd) == :exe-task) { status(cnd) == :terminated }
        (type(cnd) == :track)    { detrack(cnd) == nil }
        else { false }
    }
}

func await-ret (evt) {
    val ok = `:bool ${D}evt.type>=CEU_VALUE_DYNAMIC || ceu_as_bool(${D}evt)`
    ok  or evt
}
""".replace("\n", " ")

