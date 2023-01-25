class Static (val outer: Expr.Do, val ups: Ups, val vars: Vars) {
    // funcs that set vars in enclosing tasks
    //  - they are marked and cannot receive "evt"
    //  - otherwise, we do not check with ceu_block_set
    val funcs_vars_tasks = mutableSetOf<Expr.Proto>()

    init {
        this.outer.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> this.body.traverse()
            is Expr.Do -> this.es.forEach { it.traverse() }
            is Expr.Dcl -> this.src?.traverse()
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
                val func = ups.first(this) { it is Expr.Proto && it.tk.str=="func" }
                if (func != null) {
                    val acc = this.dst.base()
                    val dcl = vars.get(this, acc.tk.str)!!
                    val intask = ups.first(dcl.blk) { it is Expr.Proto }.let { it!=null && it.tk.str!="func" }
                    if (intask) {
                        funcs_vars_tasks.add(func as Expr.Proto)
                    }
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.While  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()

            is Expr.Spawn  -> { this.call.traverse() ; this.tasks?.traverse() }
            is Expr.Bcast  -> { this.xin.traverse() ; this.evt.traverse() }
            is Expr.Yield  -> {
                if (!ups.intask(this)) {
                    err(this.tk, "yield error : expected enclosing coro or task")
                }
                this.arg.traverse()
            }
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> {
                this.x.traverse()
                if (this.x is Expr.Self) {
                    val ok = (ups.first_true_x(this,this.x.tk.str) != null)
                    if (!ok) {
                        err(this.tk, "${this.tk.str} error : expected enclosing task")
                    }
                }
            }
            is Expr.Self   -> {
                if (ups.true_x_c(this, this.tk.str) == null) {
                    err(this.tk, "task error : missing enclosing task")
                }
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.EvtErr -> {}
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEach{ it.traverse() }
            is Expr.Vector -> this.args.forEach{ it.traverse() }
            is Expr.Dict   -> this.args.forEach { it.first.traverse() ; it.second.traverse() }
            is Expr.Index  -> {
                this.col.traverse()
                this.idx.traverse()
            }
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
