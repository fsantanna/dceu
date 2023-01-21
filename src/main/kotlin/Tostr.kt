fun Pos.pre (): String {
    assert(this.lin>=0 && this.col>=0)
    return "^[${this.lin},${this.col}]"
}

fun Expr.tostr (pre: Boolean = false): String {
    fun Tk.Id.tostr (): String {
        return "^".repeat(this.upv) + this.str
    }
    fun Expr.Do.anns (): String {
        return when {
             this.isnest &&  this.ishide -> ""
            !this.isnest && !this.ishide -> ":unnest "
            !this.isnest &&  this.ishide -> ":unnest :hide "
            else -> error("bug found")
        }
    }
    return when (this) {
        is Expr.Proto  -> this.tk.str + " (" + this.args.map { (id,tag) -> id.tostr() + tag.cond {" ${tag!!.str}"} }.joinToString(",") + ") " + this.fake.cond{":fake "} + this.body.tostr(pre)
        is Expr.Do     -> (this.tk.str=="do").cond{"do "} + this.anns() + "{\n" + this.es.tostr(pre) + "}"
        is Expr.Dcl    -> "var " + this.tk_.tostr() + this.tmp.cond { " :tmp" } + this.tag.cond{" "+it.str} + this.src.cond { " = ${it.tostr(pre)}" }
        is Expr.Set    -> "set " + this.dst.tostr(pre) + " = " + this.src.tostr(pre)
        is Expr.If     -> "if " + this.cnd.tostr(pre) + " " + this.t.tostr(pre) + " else " + this.f.tostr(pre)
        is Expr.While  -> "while " + this.cnd.tostr(pre) + " " + this.body.tostr(pre)
        is Expr.Catch  -> "catch " + this.cnd.tostr(pre) + " " + this.body.tostr(pre)
        is Expr.Defer  -> "defer " + this.body.tostr(pre)
        is Expr.Enum   -> "enum {\n" + this.tags.map {
            (tag,e) -> tag.str + e.cond { " = " + "`" + it.str + "`" }
        }.joinToString(",\n") + "\n}"
        is Expr.Data -> "data " + this.tk.str + " = [" + this.ids.map { it.first.str + (it.second?.str ?: "") }.joinToString(",") + "]"
        is Expr.Pass   -> "pass " + this.e.tostr(pre)

        is Expr.Spawn  -> "spawn " + this.tasks.cond{"in "+it.tostr(pre)+", "} + this.call.tostr(pre)
        is Expr.Bcast  -> "broadcast in " + this.xin.tostr(pre) + ", " + this.evt.tostr(pre)
        is Expr.Yield  -> "yield(" + this.arg.tostr(pre) + ")"
        is Expr.Resume -> "resume " + this.call.tostr(pre)
        is Expr.Toggle -> "toggle " + this.coro.tostr(pre) + "(" + this.on.tostr() + ")"
        is Expr.Pub    -> this.coro.tostr(pre) + "." + this.tk.str
        is Expr.Task   -> "task"

        is Expr.Nat    -> "```" + (this.tk_.tag ?: "") + " " + this.tk.str + "```"
        is Expr.Acc    -> this.tk_.tostr()
        is Expr.EvtErr -> this.tk.str
        is Expr.Nil    -> this.tk.str
        is Expr.Tag    -> this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Char   -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Tuple  -> "[" + this.args.map { it.tostr(pre) }.joinToString(",") + "]"
        is Expr.Vector -> "#[" + this.args.map { it.tostr(pre) }.joinToString(",") + "]"
        is Expr.Dict   -> "@[" + this.args.map { "(${it.first.tostr(pre)},${it.second.tostr(pre)})" }.joinToString(",") + "]"
        is Expr.Index  -> this.col.tostr(pre) + "[" + this.idx.tostr(pre) + "]"
        is Expr.Call   -> this.proto.tostr(pre) + "(" + this.args.map { it.tostr(pre) }.joinToString(",") + ")"
    }.let { if (pre) this.tk.pos.pre()+it else it }
}

fun List<Expr>.tostr (pre: Boolean=false): String {
    return this.map { it.tostr(pre) }.joinToString("\n") + "\n"
}
