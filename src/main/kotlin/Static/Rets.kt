package dceu

class Rets (val outer: Expr.Call, val ups: Ups) {
    val pub: MutableMap<Expr,Int> = mutableMapOf()
        // how many values should Expr evaluate to?
        // 99: multi | n: n

    init {
        outer.traverse(1)
    }

    fun Expr.traverse (N: Int) {
        pub[this] = N
        when (this) {
            is Expr.Proto  -> this.blk.traverse(MULTI)
            is Expr.Export -> this.blk.traverse(N)
            is Expr.Do     -> this.es.forEachIndexed { i,e -> e.traverse(if (i==this.es.lastIndex) N else 0) }
            is Expr.Dcl    -> this.src?.traverse(1)
            is Expr.Set    -> {
                this.dst.traverse(0)
                this.src.traverse(1)
            }
            is Expr.If     -> {
                this.cnd.traverse(1)
                this.t.traverse(N)
                this.f.traverse(N)
            }
            is Expr.Loop  -> this.blk.traverse(0)
            is Expr.Break -> {
                val n = pub[ups.first(this) { it is Expr.Loop }!!]!!
                pub[this] = n
                this.cnd.traverse(1)
                this.e?.traverse(n)
            }
            is Expr.Skip   -> this.cnd.traverse(1)
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse(0)

            is Expr.Catch  -> {
                this.cnd.traverse(1)
                this.blk.traverse(N)
            }
            is Expr.Defer  -> this.blk.traverse(0)

            is Expr.Yield  -> {
                this.arg.traverse(TODO())
            }
            is Expr.Resume -> {
                this.co.traverse(TODO())
                this.arg.traverse(TODO())
            }

            is Expr.Spawn  -> {
                this.tsks?.traverse(TODO())
                this.tsk.traverse(TODO())
                this.args.forEach { it.traverse(TODO()) }
            }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse(TODO())
            is Expr.Dtrack -> this.blk.traverse(TODO())
            is Expr.Toggle -> { this.tsk.traverse(TODO()) ; this.on.traverse(TODO()) }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEachIndexed { i,arg ->
                arg.traverse(if (i==this.args.lastIndex) MULTI else 1)
            }
            is Expr.Vector -> this.args.forEachIndexed { i,arg ->
                arg.traverse(if (i==this.args.lastIndex) MULTI else 1)
            }
            is Expr.Dict   -> this.args.forEach { (k,v) -> k.traverse(1) ; v.traverse(1) }
            is Expr.Index  -> {
                this.col.traverse(1)
                this.idx.traverse(1)
            }
            is Expr.Call   -> {
                this.clo.traverse(1)
                this.args.forEachIndexed { i,arg ->
                    arg.traverse(if (i==this.args.lastIndex) MULTI else 1)
                }
            }
        }
    }
}