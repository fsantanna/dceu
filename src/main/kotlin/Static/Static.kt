class Static (outer: Expr.Do, val ups: Ups, val vars: Vars) {
    // Dangerous function:
    //  - set vars in enclosing tasks
    //  - broadcast events
    // They cannot receive "evt" or "pub"
    //  - they are marked and cannot receive "evt"
    //  - otherwise, we do not check with ceu_block_set
    val funcs_unsafe = mutableSetOf<Expr.Proto>()
    val dos_unsafe = mutableSetOf<Expr.Do>()

    init {
        outer.traverse()
    }

    // set unsafe, do unsafe, spawn, bcast, resume, call unsafe
    fun Expr.set_up_unsafe() {
        val blk = ups.first(this) { it is Expr.Do } as Expr.Do
        dos_unsafe.add(blk)
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                this.body.traverse()
                if (dos_unsafe.contains(this.body)) {
                    funcs_unsafe.add(this)
                }
            }
            is Expr.Do     -> {
                this.es.forEach { it.traverse() }
                when {
                    ups.intask(this)       -> dos_unsafe.add(this)
                    dos_unsafe.contains(this) -> this.set_up_unsafe()
                }
            }
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

                // set to variable in enclosing unsafe block
                val acc = this.dst.base()
                when (acc) {
                    is Expr.Self -> {
                        // safe
                    }
                    is Expr.Acc -> {
                        val dcl = vars.get(this, acc.tk.str)!!
                        if (dos_unsafe.contains(dcl.blk)) {
                            this.set_up_unsafe()
                        }
                    }
                    else -> error("impossible case")
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.While  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()

            is Expr.Spawn  -> {
                this.set_up_unsafe()
                this.call.traverse()
                this.tasks?.traverse()
            }
            is Expr.Bcast  -> {
                this.set_up_unsafe()
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
            is Expr.Resume -> {
                this.set_up_unsafe()
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
            is Expr.Call   -> {
                this.proto.traverse()
                this.args.forEach { it.traverse() }

                when (this.proto) {
                    is Expr.Proto -> {
                        if (funcs_unsafe.contains(this.proto)) {
                            this.set_up_unsafe()
                        }
                    }
                    is Expr.Acc -> {
                        val xvar = vars.get(this, this.proto.tk.str)!!

                        when {
                            (xvar.dcl.tk.str == "var") -> this.set_up_unsafe()
                            (xvar.dcl.src is Expr.Proto) -> {
                                if (funcs_unsafe.contains(xvar.dcl.src)) {
                                    this.set_up_unsafe()
                                } else {
                                    // only safe case
                                }
                            }
                            else -> this.set_up_unsafe()
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
