package dceu

val HUB = -2

fun Expr.rets (sta: Static): Int {
    return when (this) {
        is Expr.Enum, is Expr.Data, is Expr.Defer -> 0
        is Expr.Export -> TODO()
        is Expr.Do -> if (this.es.size == 0) 0 else HUB
        is Expr.If -> HUB
        is Expr.Loop, is Expr.Break, is Expr.Skip, is Expr.Pass -> HUB
        is Expr.Resume, is Expr.Call -> MULTI
        is Expr.Catch -> HUB
        is Expr.Yield -> MULTI
        is Expr.Delay -> HUB
        is Expr.Toggle -> MULTI
        is Expr.Dcl -> if (this.src==null || sta.funs.contains(this.src)) 0 else 1
        is Expr.Set -> 1
        is Expr.Spawn, is Expr.Pub -> 1
        is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag -> 1
        is Expr.Bool, is Expr.Char, is Expr.Num, is Expr.Tuple -> 1
        is Expr.Vector, is Expr.Dict, is Expr.Index -> 1
        is Expr.Proto, is Expr.VA_len, is Expr.VA_idx -> 1
    }
}

class Rets (val outer: Expr.Call, val ups: Ups) {
    val pub: MutableMap<Expr,Int> = mutableMapOf()
        // how many values should Expr evaluate to?
        // -1: multi | n: n

    init {
        outer.traverse(1)
    }

    fun Expr.traverse (N: Int) {
        pub[this] = N
        when (this) {
            is Expr.Proto  -> this.blk.traverse(1)
            is Expr.Export -> this.blk.traverse(N)
            is Expr.Do     -> this.es.forEachIndexed { i,e ->
                val n = when {
                    (ups.pub[this] is Expr.Loop) -> 0
                    (i == this.es.lastIndex) -> N
                    (this.es[i+1] is Expr.Delay) -> if (i+1==this.es.lastIndex) N else 0
                    else -> 0
                }
                e.traverse(n)
            }
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
            is Expr.Loop  -> this.blk.traverse(N)
            is Expr.Break -> {
                val n = pub[ups.first(this) { it is Expr.Loop }!!]!!
                pub[this] = n
                this.cnd.traverse(1)
                this.e?.traverse(n)
            }
            is Expr.Skip   -> this.cnd.traverse(1)
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse(N)

            is Expr.Catch  -> {
                this.cnd.traverse(1)
                this.blk.traverse(N)
            }
            is Expr.Defer  -> this.blk.traverse(1)  // to assert it is not error

            is Expr.Yield  -> {
                this.args.forEach { it.traverse(1) }
            }
            is Expr.Resume -> {
                this.co.traverse(1)
                this.args.forEach { it.traverse(1) }
            }

            is Expr.Spawn  -> {
                this.tsks?.traverse(1)
                this.tsk.traverse(1)
                this.args.forEach { it.traverse(1) }
            }
            is Expr.Delay  -> {}
            is Expr.Pub    -> this.tsk?.traverse(1)
            is Expr.Toggle -> { this.tsk.traverse(1) ; this.on.traverse(1) }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.Nil    -> {}
            is Expr.Tag    -> {}
            is Expr.Bool   -> {}
            is Expr.Char   -> {}
            is Expr.Num    -> {}
            is Expr.Tuple  -> this.args.forEachIndexed { i,arg ->
                arg.traverse(/*if (i==this.args.lastIndex) MULTI else*/ 1)
            }
            is Expr.Vector -> this.args.forEachIndexed { i,arg ->
                arg.traverse(/*if (i==this.args.lastIndex) MULTI else*/ 1)
            }
            is Expr.Dict   -> this.args.forEach { (k,v) -> k.traverse(1) ; v.traverse(1) }
            is Expr.Index  -> {
                this.col.traverse(1)
                this.idx.traverse(1)
            }
            is Expr.Call   -> {
                this.clo.traverse(1)
                this.args.forEachIndexed { i,arg ->
                    arg.traverse(1)
                }
            }

            is Expr.VA_len -> {}
            is Expr.VA_idx -> this.idx.traverse(1)
        }
    }
}
