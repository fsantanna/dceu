package dceu

class Unsafe (outer: Expr.Do, val ups: Ups, val vars: Vars) {
    // Dangerous function:
    //  - set vars in enclosing tasks
    //  - broadcast events
    //  - yield
    // They cannot receive "evt" or "pub" or "detrack" or dcl ":tmp"
    //  - they are marked and cannot receive "evt"
    //  - otherwise, we do not check with ceu_block_set
    val funcs = mutableSetOf<Expr.Proto>()
    val dos   = mutableSetOf<Expr.Do>()

    init {
        outer.traverse()
        var old = 0
        var cur = funcs.size + dos.size
        while (cur > old) {
            outer.traverse()
            old = cur
            cur = funcs.size + dos.size
        }
    }

    fun chk_up_safe (e: Expr): Boolean {
        val blk = ups.first_block(e)!!
        return !this.dos.contains(blk)
    }

    fun Expr.set_up_unsafe() {
        val up = ups.pub[this]
        if (up != null) {
            val blk = ups.first(up) { it is Expr.Do } as Expr.Do
            dos.add(blk)
        }
    }

    fun Expr.traverse () {
        when (this) {
            is Expr.Proto  -> {
                this.body.traverse()
                if (dos.contains(this.body)) {
                    funcs.add(this)
                }
            }
            is Expr.Do     -> {
                this.es.forEach { it.traverse() }
                when {
                    //ups.intask(this) -> dos.add(this)
                    dos.contains(this)  -> this.set_up_unsafe()
                }
            }
            is Expr.Dcl    -> {
                if (this.tmp==true && !chk_up_safe(this)) {
                    err(this.tk, "invalid declaration : unsafe \":tmp\"")
                }
                this.src?.traverse()
            }
            is Expr.Set    -> {
                this.dst.traverse()
                this.src.traverse()

                // set to variable in enclosing unsafe block
                val acc = this.dst.base()
                val (blk,_) = vars.get(acc)
                if (dos.contains(blk)) {
                    this.set_up_unsafe()
                }
            }
            is Expr.If     -> { this.cnd.traverse() ; this.t.traverse() ; this.f.traverse() }
            is Expr.Loop   -> this.body.traverse()
            is Expr.XBreak -> {}
            is Expr.Catch  -> { this.cnd.traverse() ; this.body.traverse() }
            is Expr.Defer  -> this.body.traverse()
            is Expr.Enum   -> {}
            is Expr.Data   -> {}
            is Expr.Pass   -> this.e.traverse()
            is Expr.Drop   -> this.e.traverse()

            is Expr.Nat    -> {}
            is Expr.Acc    -> {}
            is Expr.Err -> {}
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
                        if (funcs.contains(this.proto)) {
                            this.set_up_unsafe()
                        }
                    }
                    is Expr.Acc -> {
                        val (_,dcl) = vars.get(this.proto)
                        when {
                            (dcl.tk.str == "var") -> this.set_up_unsafe()
                            GLOBALS.contains(dcl.id.str) -> {} // SAFE
                            (dcl.src is Expr.Proto) -> {
                                if (funcs.contains(dcl.src)) {
                                    this.set_up_unsafe()
                                } else {
                                    // SAFE
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
