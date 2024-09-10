package dceu

class Optim (val outer: Expr.Do, val vars: Vars) {
    val outer1: Expr.Do
    init {
        val (_,out) = outer.blocks()
        outer1 = out as Expr.Do
    }

    fun Expr.blocks (): Pair<Boolean,Expr> {
        return when (this) {
            is Expr.Proto  -> {
                val (_, blk) = this.blk.blocks()
                Pair(false,
                    Expr.Proto(this.tk_, this.nst, this.fake, this.tag, this.pars, blk as Expr.Do)
                )
            }
            is Expr.Do     -> {
                val (reqs, subs) = this.es.map { it.blocks() }.unzip()
                val up = this.up.let {
                    it is Expr.Proto || it is Expr.If || it is Expr.Loop || it is Expr.Catch || it is Expr.Defer
                }
                val ret = if (this==outer || this.tag!=null || up || reqs.any()) {
                    Expr.Do(this.tk_, this.tag, subs)
                } else {
                    Expr.Group(Tk.Fix("group",this.tk.pos.copy()), subs)
                }
                Pair(false, ret)
            }
            is Expr.Escape -> {
                val (req1, e1) = if (this.e == null) Pair(false,null) else {
                    this.e.blocks()
                }
                Pair(req1, Expr.Escape(this.tk_, this.tag, e1))
            }
            is Expr.Group  -> {
                val (reqs, subs) = this.es.map { it.blocks() }.unzip()
                Pair(reqs.any(), Expr.Group(this.tk_, subs))
            }
            is Expr.Dcl    -> {
                val req1 = (this.tk.str=="val" || this.tk.str=="var")
                val (req2, src2) = if (this.src == null) Pair(false, null) else {
                    this.src.blocks()
                }
                val ret1 = Expr.Dcl(this.tk_, lex, idtag, src2)
                Pair(req1||req2, ret1)
            }
            is Expr.Set    -> {
                val (req1,dst) = this.dst.blocks()
                val (req2,src) = this.src.blocks()
                Pair(req1||req2, Expr.Set(this.tk_, dst, src))
            }
            is Expr.If     -> {
                val (req1, cnd) = this.cnd.blocks()
                val (req2, t)   = this.t.blocks()
                val (req3, f)   = this.f.blocks()
                Pair(req1||req2||req3, Expr.If(this.tk_, cnd, t as Expr.Do, f as Expr.Do))
            }
            is Expr.Loop   -> {
                val (req, blk) = this.blk.blocks()
                Pair(req, blk as Expr.Do)
            }
            is Expr.Data   -> Pair(false, this)
            is Expr.Drop   -> {
                val (req, e) = this.e.blocks()
                Pair(req, Expr.Drop(this.tk_, e, this.prime))
            }

            is Expr.Catch  -> {
                val (req, blk) = this.blk.blocks()
                Pair(req, Expr.Catch(this.tk_,this.tag,blk as Expr.Do))
            }
            is Expr.Defer  -> {
                val (req, blk) = this.blk.blocks()
                Pair(req, Expr.Defer(this.tk_, blk as Expr.Do))
            }

            is Expr.Yield  -> {
                val (_, e) = this.e.blocks()
                Pair(true, Expr.Yield(this.tk_, e))
            }
            is Expr.Resume -> {
                val (req1, co) = this.co.blocks()
                val (reqs2, args) = this.args.map { it.blocks() }.unzip()
                Pair(req1||reqs2.any(), Expr.Resume(this.tk_,co,args))
            }

            is Expr.Spawn  -> {
                val (req1, tsks) = if (this.tsks == null) Pair(false,null) else {
                    this.tsks.blocks()
                }
                val (req2, tsk)  = this.tsk.blocks()
                val (req3, args) = this.args.map { it.blocks() }.unzip()
                Pair(req1||req2||req3.any(), Expr.Spawn(this.tk_,tsks,tsk,args))
            }
            is Expr.Delay  -> {
                Pair(true, this)
            }
            is Expr.Pub    -> {
                val (req, tsk) = if (tsk == null) Pair(false, null) else {
                    this.tsk.blocks()
                }
                Pair(req, Expr.Pub(this.tk_,tsk))
            }
            is Expr.Toggle -> {
                val (req1, tsk) = this.tsk.blocks()
                val (req2, on)  = this.on.blocks()
                Pair(req1||req2, Expr.Toggle(this.tk_,tsk,on))
            }
            is Expr.Tasks  -> {
                val (req, max) = this.max.blocks()
                Pair(req, Expr.Tasks(this.tk_,max))
            }

            is Expr.Tuple  -> {
                val (reqs, args) = this.args.map { it.blocks() }.unzip()
                Pair(reqs.any(), Expr.Tuple(this.tk_,args))
            }
            is Expr.Vector  -> {
                val (reqs, args) = this.args.map { it.blocks() }.unzip()
                Pair(reqs.any(), Expr.Vector(this.tk_,args))
            }
            is Expr.Dict   -> {
                val (reqs, args) = this.args.map { (k,v) ->
                    val (req1, k) = k.blocks()
                    val (req2, v) = v.blocks()
                    Pair(req1||req2, Pair(k,v))
                }.unzip()
                Pair(reqs.any(), Expr.Dict(this.tk_, args))
            }
            is Expr.Index  -> {
                val (req1, col) = this.col.blocks()
                val (req2, idx) = this.idx.blocks()
                Pair(req1||req2, Expr.Index(this.tk_,col, idx))
            }
            is Expr.Call   -> {
                val (req1, clo)  = this.clo.blocks()
                val (req2, args) = this.args.map { it.blocks() }.unzip()
                Pair(req1||req2.any(), Expr.Call(this.tk_,clo,args))
            }

            is Expr.Nat, is Expr.Acc, is Expr.Nil, is Expr.Tag,
            is Expr.Bool, is Expr.Char, is Expr.Num -> Pair(false, this)
        }
    }
}
