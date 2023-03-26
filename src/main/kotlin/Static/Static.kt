class Static (outer: Expr.Do, val ups: Ups, val vars: Vars) {

    init {
        outer.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> this.body.traverse()
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> this.src?.traverse()
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
                if (this.dst is Expr.Acc) {
                    val dcl = vars.get(this, this.dst.tk_.str)!!
                    if (dcl.dcl.tk.str == "val") {
                        err(this.tk, "invalid set : destination is immutable")
                    }
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> {
                val up = ups.pub[this]
                assert(up is Expr.Do && up.es.last().n==this.n) { "bug found: invalid do-loop" }
                this.body.es.last().let {
                    if (it.is_innocuous()) {
                        err(it.tk, "invalid expression : innocuous expression")
                    }
                }
                this.body.traverse()
            }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()

            is Expr.Spawn  -> {
                this.call.traverse()
                this.tasks?.traverse()
            }
            is Expr.Bcast  -> {
                this.xin.traverse()
                this.evt.traverse()
            }
            is Expr.Yield  -> {
                if (!ups.intask(this)) {
                    err(this.tk, "yield error : expected enclosing coro or task")
                }
                this.arg.traverse()
            }
            is Expr.Resume -> {
                this.call.traverse()
            }
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> this.x.traverse()
            is Expr.Self   -> {
                if (ups.true_x_c(this, this.tk.str) == null) {
                    err(this.tk, "task error : missing enclosing task")
                }
            }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                if (this.tk.str == "_") {
                    err(this.tk, "access error : cannot access \"_\"")
                }
            }
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
            is Expr.Call   -> {
                this.proto.traverse()
                this.args.forEach { it.traverse() }
            }
        }
    }
}
