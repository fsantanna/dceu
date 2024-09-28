package dceu

fun Pos.pre (): String {
    assert(this.lin>=0 && this.col>=0)
    return "^[${this.lin},${this.col}]"
}

fun Tk.dump (): String {
    return if (!DUMP) "" else {
        "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})"
    }
}
fun Expr.dump (): String {
    return if (!DUMP) "" else {
        this.tk.dump() + " | " + this.to_str().quote(15)
    }
}

fun Tk.fpre (pre: Boolean): String {
    return if (pre) this.pos.pre() else ""
}

@JvmName("Id_Tag_tostr")
fun Id_Tag.to_str (pre: Boolean = false): String {
    return this.first.fpre(pre) + this.first.str + this.second.cond { " " + it.fpre(pre) + it.str }
}

@JvmName("Clock_tostr")
fun Clock.to_str (pre: Boolean): String {
    return "<" + this.map { it.second.to_str(pre) + " " + it.first.str }.joinToString(",") + ">"
}

fun Patt.to_str (pre: Boolean = false): String {
    return when (this) {
        is Patt.None -> "(${Pair(this.id,this.tag).to_str(pre)} | ${this.pos.to_str(true)})"
        is Patt.One  -> "(${Pair(this.id,this.tag).to_str(pre)} | ${this.e.to_str(pre)} and ${this.pos.to_str(true)})"
        is Patt.Tup  -> TODO()
    }
}

fun Expr.to_str_x (pre: Boolean): String {
    return when (this) {
        is Expr.Do    -> this.es.to_str(pre)
        is Expr.Group -> this.es.to_str(pre)
        else          -> error("impossible case")
    }.let {
        "{\n" + it + "}"
    }
}

fun Expr.to_str (pre: Boolean = false): String {
    return when (this) {
        is Expr.Proto  -> {
            val mod = when {
                this.fake -> " :fake"
                this.nst  -> " :nested"
                else      -> ""
            }
            val pars = this.pars.map { it.idtag.to_str(pre) }.joinToString(",")
            "(" + this.tk.str + mod + " (" + pars + ") " + this.tag.cond{ it.str+" " } + this.blk.to_str_x(pre) + ")"
        }
        is Expr.Do     -> "do " + this.to_str_x(pre)
        is Expr.Group  -> "group " + this.to_str_x(pre)
        is Expr.Enclose -> "enclose' " + this.tag.str + " {\n" + this.es.to_str(pre) + "}"
        is Expr.Escape -> "escape(" + this.tag.str + this.e.cond { ","+it.to_str(pre) } + ")"
        is Expr.Dcl    -> {
            "(" + this.tk_.str + " " + this.idtag.to_str(pre) + this.src.cond { " = ${it.to_str(pre)}" } + ")"
        }
        is Expr.Set    -> "(set " + this.dst.to_str(pre) + " = " + this.src.to_str(pre) + ")"
        is Expr.If     -> "if " + this.cnd.to_str(pre) + " " + this.t.to_str_x(pre) + " else " + this.f.to_str_x(pre)
        is Expr.Loop   -> "loop' " + this.blk.to_str_x(pre)
        is Expr.Data   -> "(data " + this.tk.str + " = [" + this.ids.map { it.to_str() }.joinToString(",") + "])"
        is Expr.Drop   -> "drop(" + this.e.to_str(pre) + ")"

        is Expr.Catch  -> "catch " + this.tag.cond { it.str+" " } + this.blk.to_str_x(pre)
        is Expr.Defer  -> "defer " + this.blk.to_str_x(pre)

        is Expr.Yield  -> "yield(" + this.e.to_str(pre) + ")"
        is Expr.Resume -> "(resume (" + this.co.to_str(pre) + ")(" + this.args.map { it.to_str(pre) }.joinToString(",") + "))"

        is Expr.Spawn  -> "(spawn " + this.tsk.to_str(pre) + "(" + this.args.map { it.to_str(pre) }.joinToString(",") + ")" + this.tsks.cond { " in ${it.to_str(pre)}" } + ")"
        is Expr.Delay  -> "delay"
        is Expr.Pub    -> this.tsk.cond { it.to_str(pre)+"." } + "pub"
        is Expr.Toggle -> "(toggle ${this.tsk.to_str(pre)}(${this.on.to_str(pre)}))"
        is Expr.Tasks  -> "tasks(" + this.max.to_str(pre) + ")"

        is Expr.Nat    -> "```" + this.tk_.tag.cond { it+" " } + this.tk.str + "```"
        is Expr.Acc    -> this.ign.cond { "__" } + this.tk.str
        is Expr.Nil    -> this.tk.str
        is Expr.Tag    -> this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Char   -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Tuple  -> "[" + this.args.map { it.to_str(pre) }.joinToString(",") + "]"
        is Expr.Vector -> "#[" + this.args.map { it.to_str(pre) }.joinToString(",") + "]"
        is Expr.Dict   -> "@[" + this.args.map { "(${it.first.to_str(pre)},${it.second.to_str(pre)})" }.joinToString(",") + "]"
        is Expr.Index  -> this.col.to_str(pre) + "[" + this.idx.to_str(pre) + "]"
        is Expr.Call   -> this.clo.to_str(pre) + "(" + this.args.map { it.to_str(pre) }.joinToString(",") + ")"
                            // TODO: collapse broadcast'
    }.let {
        when {
            !pre -> it
            (it.length>0 && it[0]=='(') -> '(' + this.tk.pos.pre() + it.drop(1)
            else -> this.tk.pos.pre() + it
        }
    }
}

fun List<Expr>.to_str (pre: Boolean=false): String {
    return this.map { it.to_str(pre) }.joinToString(";\n") + ";\n"
}
