package dceu

fun Pos.pre (): String {
    assert(this.lin>=0 && this.col>=0)
    return "^[${this.lin},${this.col}]"
}

fun Expr.dump (): String {
    fun Tk.dump (): String {
        return "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})"
    }
    return if (!DUMP) "" else {
        this.tk.dump() + " | " + this.tostr().take(15).filter { it != '\n' }
    }
}

fun Expr.tostr (pre: Boolean = false): String {
    fun Tk.Id.tostr (): String {
        return "^".repeat(this.upv) + this.str
    }
    return when (this) {
        is Expr.Proto -> {
            val args = this.args.map { (id,tag) ->
                id.tostr() + tag.cond {" ${tag!!.str}"}
            }.joinToString(",")
            "(" + this.tk.str + " (" + args + ") " + this.body.tostr(pre) + ")"
        }
        is Expr.Do     -> (this.tk.str=="do").cond{"do "} + "{\n" + this.es.tostr(pre) + "}"
        is Expr.Dcl    -> {
            val xtmp = when (this.tmp) {
                true  -> " :tmp"
                false -> " :xtmp"
                else  -> ""
            }
            /*this.poly.cond { "poly " } +*/
            this.tk_.str + xtmp + " " + this.id.tostr() + this.tag.cond{" "+it.str} + this.src.cond { " = ${it.tostr(pre)}" }
        }
        is Expr.Set    -> "set " + this.dst.tostr(pre) + " = " + this.src.tostr(pre)
        is Expr.If     -> "if " + this.cnd.tostr(pre) + " " + this.t.tostr(pre) + " else " + this.f.tostr(pre)
        is Expr.Loop   -> "loop " + this.body.tostr(pre)
        is Expr.Break -> "break"
        is Expr.Enum   -> "enum {\n" + this.tags.map {
            (tag,e) -> tag.str + e.cond { " = " + "`" + it.str + "`" }
        }.joinToString(",\n") + "\n}"
        is Expr.Data   -> "data " + this.tk.str + " = [" + this.ids.map { it.first.str + (it.second?.str ?: "") }.joinToString(",") + "]"
        is Expr.Pass   -> "pass " + this.e.tostr(pre)
        is Expr.Drop   -> "drop(" + this.e.tostr(pre) + ")"

        is Expr.Nat    -> "```" + (this.tk_.tag ?: "") + " " + this.tk.str + "```"
        is Expr.Acc    -> this.tk_.tostr()
        is Expr.Nil    -> this.tk.str
        is Expr.Tag    -> this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Char   -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Tuple  -> "[" + this.args.map { it.tostr(pre) }.joinToString(",") + "]"
        is Expr.Vector -> "#[" + this.args.map { it.tostr(pre) }.joinToString(",") + "]"
        is Expr.Dict   -> "@[" + this.args.map { "(${it.first.tostr(pre)},${it.second.tostr(pre)})" }.joinToString(",") + "]"
        is Expr.Index  -> this.col.tostr(pre) + "[" + this.idx.tostr(pre) + "]"
        is Expr.Call   -> this.closure.tostr(pre) + "(" + this.args.map { it.tostr(pre) }.joinToString(",") + ")"
    }.let { if (pre) this.tk.pos.pre()+it else it }
}

fun List<Expr>.tostr (pre: Boolean=false): String {
    return this.map { it.tostr(pre) }.joinToString("\n") + "\n"
}
