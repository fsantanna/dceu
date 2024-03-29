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
        this.tk.dump() + " | " + this.tostr().take(15).filter { it !in listOf('\n','{','}') }
    }
}

fun Tk.fpre (pre: Boolean): String {
    return if (pre) this.pos.pre() else ""
}

fun Pair<Tk.Id,Tk.Tag?>.tostr (pre: Boolean = false): String {
    return this.first.fpre(pre) + this.first.str + this.second.cond { " " + it.fpre(pre) + it.str }
}

fun Expr.tostr (pre: Boolean = false): String {
    return when (this) {
        is Expr.Proto  -> {
            val args = this.args.map { it.tostr(pre) }.joinToString(",")
            "(" + this.tk.str + this.nst.cond { " :nested" } + this.rec.cond { " :rec" } + " (" + args + ") " + this.tag.cond{ it.str+" " } + this.blk.tostr(pre) + ")"
        }
        is Expr.Export -> "export [" + this.ids.joinToString(",") + "] {\n" + this.blk.es.tostr(pre) + "}"
        is Expr.Do     -> {
            when (this.tk.str) {
                "do" -> "do {\n" + this.es.tostr(pre) + "}"
                else -> "{\n" + this.es.tostr(pre) + "}"
            }
        }
        is Expr.Dcl    -> {
            "(" + this.tk_.str + " " + this.idtag.tostr(pre) + this.src.cond { " = ${it.tostr(pre)}" } + ")"
        }
        is Expr.Set    -> "(set " + this.dst.tostr(pre) + " = " + this.src.tostr(pre) + ")"
        is Expr.If     -> "if " + this.cnd.tostr(pre) + " " + this.t.tostr(pre) + " else " + this.f.tostr(pre)
        is Expr.Loop   -> "loop " + this.blk.tostr(pre)
        is Expr.Break  -> "(break" + this.e.cond { "("+it.tostr(pre)+")" } + " if " + this.cnd.tostr(pre) + ")"
        is Expr.Skip   -> "(skip if " + this.cnd.tostr(pre) + ")"
        is Expr.Enum   -> "enum {\n" + this.tags.map {
            (tag,e) -> tag.str + e.cond { " = " + "`" + it.str + "`" }
        }.joinToString(",\n") + "\n}"
        is Expr.Data   -> "(data " + this.tk.str + " = [" + this.ids.map { it.tostr() }.joinToString(",") + "])"
        is Expr.Pass   -> "(do " + this.e.tostr(pre) + ")"

        is Expr.Catch  -> {
            val cnd = this.cnd.es.let {
                //println(it.tostr())
                assert(it.size == 2)
                (it[0] as Expr.Dcl).idtag.tostr(pre) + ", " + it[1].tostr(pre)
            }
            "catch (" + cnd + ") " + this.blk.tostr(pre)
        }
        is Expr.Defer  -> "defer " + this.blk.tostr(pre)

        is Expr.Yield  -> "yield(" + this.arg.tostr(pre) + ")"
        is Expr.Resume -> "(resume (" + this.co.tostr(pre) + ")(" + this.arg.tostr(pre) + "))"

        is Expr.Spawn  -> "(spawn " + this.tsk.tostr(pre) + "(" + this.args.map { it.tostr(pre) }.joinToString(",") + ")" + this.tsks.cond { " in ${this.tsks!!.tostr(pre)}" } + ")"
        is Expr.Delay  -> "delay"
        is Expr.Pub    -> this.tsk.cond { it.tostr(pre)+"." } + "pub"
        is Expr.Toggle -> "(toggle ${this.tsk.tostr(pre)}(${this.on.tostr(pre)}))"

        is Expr.Nat    -> "```" + this.tk_.tag.cond { it+" " } + this.tk.str + "```"
        is Expr.Acc    -> this.ign.cond { "__" } + this.tk.str
        is Expr.Nil    -> this.tk.str
        is Expr.Tag    -> this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Char   -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Tuple  -> "[" + this.args.map { it.tostr(pre) }.joinToString(",") + "]"
        is Expr.Vector -> "#[" + this.args.map { it.tostr(pre) }.joinToString(",") + "]"
        is Expr.Dict   -> "@[" + this.args.map { "(${it.first.tostr(pre)},${it.second.tostr(pre)})" }.joinToString(",") + "]"
        is Expr.Index  -> this.col.tostr(pre) + "[" + this.idx.tostr(pre) + "]"
        is Expr.Call   -> this.clo.tostr(pre) + "(" + this.args.map { it.tostr(pre) }.joinToString(",") + ")"   // TODO: collapse broadcast'
    }.let {
        when {
            !pre           -> it
            (it[0] == '(') -> '(' + this.tk.pos.pre() + it.drop(1)
            else           -> this.tk.pos.pre() + it
        }
    }
}

fun List<Expr>.tostr (pre: Boolean=false): String {
    return this.map { it.tostr(pre) }.joinToString("\n") + "\n"
}
