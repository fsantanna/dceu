package dceu

class Defers (outer: Expr.Do, val ups: Ups) {
    val pub: MutableMap<Expr.Do, MutableMap<Expr.Defer,String?>> = mutableMapOf()

    init {
        outer.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto -> this.body.traverse()
            is Expr.Export -> this.body.traverse()
            is Expr.Do -> {
                pub[this] = mutableMapOf()
                this.es.forEach { it.traverse() }
            }
            is Expr.Dcl -> this.src?.traverse()
            is Expr.Set -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.XLoop  -> this.body.traverse()
            is Expr.XBreak -> {}
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> {
                pub[ups.first_block(this)]!![this] = null
                this.body.traverse()
            }
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()
            is Expr.Drop   -> this.e.traverse()

            is Expr.Spawn  -> { this.call.traverse() ; this.tasks?.traverse() }
            is Expr.Bcast  -> { this.xin.traverse() ; this.evt.traverse() }
            is Expr.Yield  -> this.arg.traverse()
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> this.x.traverse()
            is Expr.Self   -> {}

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
