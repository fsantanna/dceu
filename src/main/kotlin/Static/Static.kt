class Static (outer: Expr.Do, val ups: Ups, val vars: Vars) {
    // Dangerous function:
    //  - set vars in enclosing tasks
    //  - broadcast events
    // They cannot receive "evt" or "pub"
    //  - they are marked and cannot receive "evt"
    //  - otherwise, we do not check with ceu_block_set
    val funcs_unsafe = mutableSetOf<Expr.Proto>()

    init {
        outer.traverse()
    }

    // spawn, call unsafe, bcast, yield, resume, toggle,

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> this.body.traverse()
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> this.src?.traverse()
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
                val func = ups.first(this) { it is Expr.Proto && it.tk.str=="func" }
                if (func != null) {
                    val acc = this.dst.base()
                    val dcl = vars.get(this, acc.tk.str)!!
                    val intask = ups.first(dcl.blk) { it is Expr.Proto }.let { it!=null && it.tk.str!="func" }
                    //if (intask) {
                        funcs_unsafe.add(func as Expr.Proto)
                    //}
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
            is Expr.Bcast  -> {
                this.xin.traverse()
                this.evt.traverse()
                val func = ups.first(this) { it is Expr.Proto && it.tk.str=="func" }
                if (func != null) {
                    funcs_unsafe.add(func as Expr.Proto)
                }
            }
            is Expr.Yield  -> {
                if (!ups.intask(this)) {
                    err(this.tk, "yield error : expected enclosing coro or task")
                }
                this.arg.traverse()
            }
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> this.x.traverse()
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
