package dceu

fun optim_blocks () {
    fun Expr.traverse (): Pair<Boolean,Expr> {
        return when (this) {
            is Expr.Proto -> {
                val (_, blk) = this.blk.traverse()
                val (_, pars) = this.pars.map { it.traverse() }.unzip()
                Pair(false,
                    Expr.Proto(this.tk_, this.nst, this.fake, this.tag, pars as List<Expr.Dcl>, blk as Expr.Do)
                )
            }

            is Expr.Do -> {
                val (reqs, subs) = this.es.map { it.traverse() }.unzip()
                val up = this.fup()
                val isup = (up is Expr.Proto || up is Expr.Loop || up is Expr.Catch || up is Expr.Defer)
                val ret = if (up === null || this.tag !== null || isup || reqs.any { it }) {
                    Expr.Do(this.tk_, this.tag, subs)
                } else {
                    Expr.Group(Tk.Fix("group", this.tk.pos.copy()), subs)
                }
                Pair(false, ret)
            }

            is Expr.Escape -> {
                val (req1, e1) = if (this.e === null) Pair(false, null) else {
                    this.e.traverse()
                }
                Pair(req1, Expr.Escape(this.tk_, this.tag, e1))
            }

            is Expr.Group -> {
                val (reqs, subs) = this.es.map { it.traverse() }.unzip()
                Pair(reqs.any { it }, Expr.Group(this.tk_, subs))
            }

            is Expr.Dcl -> {
                val req1 = (this.tk.str=="val" || this.tk.str=="var" || this.idtag.first.str=="it")
                val (req2, src2) = if (this.src === null) Pair(false, null) else {
                    this.src.traverse()
                }
                val ret1 = Expr.Dcl(this.tk_, lex, idtag, src2)
                Pair(req1 || req2, ret1)
            }

            is Expr.Set -> {
                val (req1, dst) = this.dst.traverse()
                val (req2, src) = this.src.traverse()
                Pair(req1 || req2, Expr.Set(this.tk_, dst, src))
            }

            is Expr.If -> {
                val (req1, cnd) = this.cnd.traverse()
                val (req2, t) = this.t.traverse()
                val (req3, f) = this.f.traverse()
                Pair(req1 || req2 || req3, Expr.If(this.tk_, cnd, t, f))
            }

            is Expr.Loop -> {
                val (req, blk) = this.blk.traverse()
                Pair(req, Expr.Loop(this.tk_, blk as Expr.Do))
            }

            is Expr.Data -> Pair(false, this)
            is Expr.Drop -> {
                val (req, e) = this.e.traverse()
                Pair(req, Expr.Drop(this.tk_, e, this.prime))
            }

            is Expr.Catch -> {
                val (req, blk) = this.blk.traverse()
                Pair(req, Expr.Catch(this.tk_, this.tag, blk as Expr.Do))
            }

            is Expr.Defer -> {
                val (_, blk) = this.blk.traverse()
                Pair(true, Expr.Defer(this.tk_, blk as Expr.Do))
            }

            is Expr.Yield -> {
                val (_, e) = this.e.traverse()
                Pair(true, Expr.Yield(this.tk_, e))
            }

            is Expr.Resume -> {
                val (req1, co) = this.co.traverse()
                val (reqs2, args) = this.args.map { it.traverse() }.unzip()
                Pair(req1 || reqs2.any { it }, Expr.Resume(this.tk_, co, args))
            }

            is Expr.Spawn -> {
                val (_, tsks) = if (this.tsks === null) Pair(false, null) else {
                    this.tsks.traverse()
                }
                val (_, tsk) = this.tsk.traverse()
                val (_, args) = this.args.map { it.traverse() }.unzip()
                Pair(true, Expr.Spawn(this.tk_, tsks, tsk, args))
            }

            is Expr.Delay -> {
                Pair(true, this)
            }

            is Expr.Pub -> {
                val (req, tsk) = if (tsk === null) Pair(false, null) else {
                    this.tsk.traverse()
                }
                Pair(req, Expr.Pub(this.tk_, tsk))
            }

            is Expr.Toggle -> {
                val (req1, tsk) = this.tsk.traverse()
                val (req2, on) = this.on.traverse()
                Pair(req1 || req2, Expr.Toggle(this.tk_, tsk, on))
            }

            is Expr.Tasks -> {
                val (req, max) = this.max.traverse()
                Pair(req, Expr.Tasks(this.tk_, max))
            }

            is Expr.Tuple -> {
                val (reqs, args) = this.args.map { it.traverse() }.unzip()
                Pair(reqs.any { it }, Expr.Tuple(this.tk_, args))
            }

            is Expr.Vector -> {
                val (reqs, args) = this.args.map { it.traverse() }.unzip()
                Pair(reqs.any { it }, Expr.Vector(this.tk_, args))
            }

            is Expr.Dict -> {
                val (reqs, args) = this.args.map { (k, v) ->
                    val (req1, k) = k.traverse()
                    val (req2, v) = v.traverse()
                    Pair(req1 || req2, Pair(k, v))
                }.unzip()
                Pair(reqs.any { it }, Expr.Dict(this.tk_, args))
            }

            is Expr.Index -> {
                val (req1, col) = this.col.traverse()
                val (req2, idx) = this.idx.traverse()
                Pair(req1 || req2, Expr.Index(this.tk_, col, idx))
            }

            is Expr.Call -> {
                val (req1, clo) = this.clo.traverse()
                val (req2, args) = this.args.map { it.traverse() }.unzip()
                Pair(req1 || req2.any { it }, Expr.Call(this.tk_, clo, args))
            }

            is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag,
            is Expr.Bool, is Expr.Char, is Expr.Num -> Pair(false, this)
        }.let {
            val (_,me) = it
            if (me !== this) {
                me.n = this.n
                G.ns[me.n] = me
                if (this !== G.outer) {
                    G.ups[me.n] = G.ups[this.n]!!
                }
            }
            it
        }
    }
    G.outer = G.outer!!.traverse().second as Expr.Do
}
