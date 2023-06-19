class Clos (val outer: Expr.Do, val ups: Ups, val vars: Vars) {
    // Protos that cannot be closures:
    //  - they access at least 1 free var w/o upval modifiers
    //  - for each var access ACC, we get its declaration DCL in block BLK
    //      - if ACC/DCL have no upval modifiers
    //      - we check if there's a func FUNC in between ACC -> [FUNC] -> BLK
    val protos_noclos = mutableSetOf<Expr>()

    // Upvars (var ^up) with refs (^^up):
    //  - at least one refs access the var
    //  - for each var access ACC, we get its declaration DCL and set here
    //  - in another round (Code), we assert that the DCL appears here
    //  - TODO: can also be used to warn for unused normal vars
    val vars_refs = mutableSetOf<Var>()

    // Set of uprefs within protos:
    //  - for each ^^ACC, we get the enclosing PROTOS and add ACC.ID to them
    val protos_refs = mutableMapOf<Expr.Proto,MutableSet<String>>()

    init {
        this.outer.traverse()
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> this.body.traverse()
            is Expr.Export -> this.body.traverse()
            is Expr.Do     -> this.es.forEach { it.traverse() }
            is Expr.Dcl    -> this.src?.traverse()
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.body.traverse()
            is Expr.XBreak -> {}
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()

            is Expr.Spawn  -> { this.call.traverse() ; this.tasks?.traverse() }
            is Expr.Bcast  -> { this.xin.traverse() ; this.evt.traverse() }
            is Expr.Yield  -> this.arg.traverse()
            is Expr.Resume -> this.call.traverse()
            is Expr.Toggle -> { this.task.traverse() ; this.on.traverse() }
            is Expr.Pub    -> {}
            is Expr.Self   -> {}

            is Expr.Nat    -> {}
            is Expr.Acc    -> {
                val xvar = vars.get(this, this.tk.str)!!
                when {
                    (xvar.dcl.id.upv==1 && this.tk_.upv==2) -> {
                        vars_refs.add(xvar) // UPVS_VARS_REFS

                        // UPVS_PROTOS_REFS
                        ups.all_until(this) { xvar.blk ==it }
                            .filter { it is Expr.Proto }
                            .let { it as List<Expr.Proto> }
                            .forEach { proto ->
                                val set = protos_refs[proto] ?: mutableSetOf()
                                set.add(this.tk.str)
                                if (protos_refs[proto] == null) {
                                    protos_refs[proto] = set
                                }
                            }
                    }
                    // UPVS_PROTOS_NOCLOS
                    (xvar.blk !=outer && xvar.dcl.id.upv==0 && this.tk_.upv==0) -> {
                        // access to normal noglb w/o upval modifier
                        ups.all_until(this) { it == xvar.blk }       // stop at enclosing declaration block
                            .filter { it is Expr.Proto }            // all crossing protos
                            .forEach { protos_noclos.add(it) }        // mark them as noclos
                    }
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
            is Expr.Call   -> { this.proto.traverse() ; this.args.forEach { it.traverse() } }
        }
    }
}
