package dceu

fun Expr.do_has_var (): Boolean {
    return this.dn_collect {
        when (it) {
            is Expr.Proto, is Expr.Do -> null
            is Expr.Dcl -> if (it.tk.str=="val" || it.tk.str=="var" || it.idtag.first.str=="it") listOf(Unit) else emptyList()
            is Expr.Defer, is Expr.Yield, is Expr.Spawn, is Expr.Delay, is Expr.Tasks -> listOf(Unit)
            else -> emptyList()
        }
    }.isNotEmpty()
}

fun Expr.has_escape (tag: String): Boolean {
    return this.dn_collect {
        when (it) {
            is Expr.Enclose -> if (it.tag.str == tag) null else emptyList()
            is Expr.Escape  -> if (it.tag.str == tag) listOf(Unit) else emptyList()
            else -> emptyList()
        }
    }.isNotEmpty()
}

fun Expr.prune (): Expr {
    return when (this) {
        is Expr.Proto -> {
            val pars = this.pars.map { it.prune() } as List<Expr.Dcl>
            val blk = this.blk.prune() as Expr.Do
            Expr.Proto(this.tk_, this.nst, this.fake, this.tag, pars, blk)
        }

        is Expr.Do -> {
            val es = this.es.map { it.prune() }
            val up = this.fup()
            val isup = (up is Expr.Proto || up is Expr.Catch || up is Expr.Defer)
            val req = (up===null || isup || this.es.any { it.do_has_var() })
            if (req) {
                Expr.Do(this.tk_, es)
            } else {
                Expr.Group(Tk.Fix("group", this.tk.pos.copy()), es)
            }
        }

        is Expr.Enclose -> {
            val blk = this.blk.prune()
            if (this.blk.has_escape(this.tag.str)) {
                Expr.Enclose(this.tk_, this.tag, blk)
            } else {
                blk
            }
        }
        is Expr.Escape -> Expr.Escape(this.tk_, this.tag, this.e?.prune())
        is Expr.Group -> Expr.Group(this.tk_, this.es.map { it.prune() })
        is Expr.Dcl -> Expr.Dcl(this.tk_, lex, idtag, this.src?.prune())

        is Expr.Set -> Expr.Set(this.tk_, this.dst.prune(), this.src.prune())
        is Expr.If -> Expr.If(this.tk_, this.cnd.prune(), this.t.prune(), this.f.prune())
        is Expr.Loop -> Expr.Loop(this.tk_, this.blk.prune())
        is Expr.Data -> this
        is Expr.Drop -> Expr.Drop(this.tk_, this.e.prune(), this.prime)
        is Expr.Catch -> Expr.Catch(this.tk_, this.tag, this.blk.prune() as Expr.Do)
        is Expr.Defer -> Expr.Defer(this.tk_, this.blk.prune() as Expr.Do)
        is Expr.Yield -> Expr.Yield(this.tk_, this.e.prune())
        is Expr.Resume -> Expr.Resume(this.tk_, this.co.prune(), this.args.map { it.prune() })
        is Expr.Spawn -> Expr.Spawn(this.tk_, this.tsks?.prune(), this.tsk.prune(), this.args.map { it.prune() })
        is Expr.Delay -> this
        is Expr.Pub -> Expr.Pub(this.tk_, this.tsk?.prune())
        is Expr.Toggle -> Expr.Toggle(this.tk_, this.tsk.prune(), this.on.prune())
        is Expr.Tasks -> Expr.Tasks(this.tk_, this.max.prune())

        is Expr.Tuple -> Expr.Tuple(this.tk_, this.args.map { it.prune() })
        is Expr.Vector -> Expr.Vector(this.tk_, this.args.map { it.prune() })
        is Expr.Dict -> {
            val args = this.args.map { (k, v) ->
                Pair(k.prune(), v.prune())
            }
            Expr.Dict(this.tk_, args)
        }

        is Expr.Index -> Expr.Index(this.tk_, this.col.prune(), this.idx.prune())
        is Expr.Call -> Expr.Call(this.tk_, this.clo.prune(), this.args.map { it.prune() })

        is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag,
        is Expr.Bool, is Expr.Char, is Expr.Num -> this
    }.let {
        if (it !== this) {
            it.n = this.n
            G.ns[it.n] = it
            if (this !== G.outer) {
                G.ups[it.n] = G.ups[this.n]!!
            }
        }
        it
    }
}
