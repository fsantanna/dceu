package dceu

class Clos (val outer: Expr.Do, val ups: Ups, val vars: Vars) {
    // Protos that cannot be closures:
    //  - they access at least 1 non-local free var w/o upval modifiers (globals are allowed)
    //  - for each var access ACC, we get its declaration DCL in block BLK
    //      - if ACC/DCL have no upval modifiers
    //      - we check if there's a func FUNC in between ACC -> [FUNC] -> BLK
    val protos_noclos = mutableSetOf<Expr.Proto>()

    // Upvars (var ^up) with refs (^^up):
    //  - at least one refs access the var
    //  - for each var access ACC, we get its declaration DCL and set here
    //  - in another round (Code), we assert that the DCL appears here
    //  - TODO: can also be used to warn for unused normal vars
    val vars_refs = mutableSetOf<Pair<Expr.Do,Expr.Dcl>>()

    // Set of uprefs within protos:
    //  - for each ^^ACC, we get the enclosing PROTOS and add ACC.ID to them
    val protos_refs = mutableMapOf<Expr.Proto,MutableSet<Expr.Dcl>>()

    // Blocks with nested coro/task accessing a variable:
    //  - declares a non-upval var that is accessed across coro/task
    val blks_nested = mutableSetOf<Expr.Do>()

    init {
        this.outer.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> this.blk.traverse()
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> this.src?.traverse()
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop  -> this.blk.traverse()
            is Expr.Break -> { this.cnd.traverse() ; this.e?.traverse() }
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()
            is Expr.Drop   -> this.e.traverse()

            is Expr.Catch  -> { this.cnd?.traverse() ; this.blk.traverse() }
            is Expr.Defer  -> this.blk.traverse()

            is Expr.Yield  -> this.arg.traverse()
            is Expr.Resume -> { this.co.traverse() ; this.arg.traverse() }

            is Expr.Spawn  -> { this.tsks?.traverse() ; this.tsk.traverse() ; this.arg.traverse() }
            is Expr.Pub    -> this.tsk?.traverse()
            is Expr.Bcast  -> this.call.traverse()
            is Expr.Dtrack -> this.tsk.traverse()
            is Expr.Toggle -> { this.tsk.traverse() ; this.on.traverse() }

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val (blk,dcl) = vars.get(this)
                when {
                    (dcl.id.upv==1 && this.tk_.upv==2) -> {
                        vars_refs.add(Pair(blk,dcl)) // UPVS_VARS_REFS

                        // UPVS_PROTOS_REFS
                        ups.all_until(this) { blk==it }
                            .filter { it is Expr.Proto }
                            .let { it as List<Expr.Proto> }
                            .forEach { proto ->
                                val set = protos_refs[proto] ?: mutableSetOf()
                                set.add(dcl)
                                if (protos_refs[proto] == null) {
                                    protos_refs[proto] = set
                                }
                            }
                    }
                    // UPVS_PROTOS_NOCLOS
                    (blk!=outer && dcl.id.upv==0 && this.tk_.upv==0) -> {
                        // access to normal noglb w/o upval modifier
                        ups.all_until(this) { it == blk }     // stop at enclosing declaration block
                            .filter { it is Expr.Proto }         // all crossing protos
                            .forEach {
                                it as Expr.Proto
                                protos_noclos.add(it)            // mark protos as noclos
                                if (it.tk.str != "func") {
                                    blks_nested.add(blk)         // mark dcl block with nested coro/task
                                }
                            }
                    }
                }
            }
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
            is Expr.Call   -> { this.clo.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
