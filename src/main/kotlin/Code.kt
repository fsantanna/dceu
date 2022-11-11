class Coder (parser_: Parser) {
    val parser = parser_

    // block: String -> current enclosing block for normal allocation
    // ret: Pair<block,var> -> enclosing assignment with destination block and variable
    fun fset(tk: Tk, ret: Pair<String, String>?, src: String): String {
        return if (ret == null) "" else fset(tk, ret.first, ret.second, src)
    }
    fun fset(tk: Tk, ret_block: String, ret_var: String, src: String): String {
        return """
            if ($src.tag == CEU_TYPE_TUPLE) {
                if ($src.tuple->block->depth > $ret_block->depth) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${tk.pos.lin}, col ${tk.pos.col}) : set error : incompatible scopes", 256);
                    break;
                }
            }
            $ret_var = $src;
    
        """.trimIndent()
    }

    fun Tk.Id.fromOp (): String {
        val MAP = mapOf(
            Pair('+', "plus"),
            Pair('-', "minus"),
            Pair('*', "mul"),
            Pair('/', "div"),
            Pair('>', "gt"),
            Pair('<', "lt"),
            Pair('=', "eq"),
            Pair('!', "not"),
            Pair('|', "or"),
            Pair('&', "and"),
        )
        return if (this.str[0] != '{') this.str else {
            "op_" + this.str.drop(1).dropLast(1).toList().map { MAP[it] }.joinToString("_")
        }
    }

    fun Expr.code(block: String?, set: Pair<String, String>?): String {
        return when (this) {
            is Expr.Block -> {
                val depth = if (block == null) 0 else "$block->depth+1"
                """
                { // BLOCK
                    assert($depth < UINT8_MAX);
                    CEU_Block ceu_block_$n = { $depth, NULL };
                    if (ceu_block_global == NULL) {
                        ceu_block_global = &ceu_block_$n;
                    }    
                    do {
                        ${this.es.code("(&ceu_block_$n)", set)}
                    } while (0);
                    ceu_block_free(&ceu_block_$n);
                    if (ceu_throw != CEU_THROW_NONE) {
                        break;
                    }
                }
                
                """.trimIndent()
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp()
                """
                // DCL
                CEU_Value $id = { CEU_TYPE_NIL };
                CEU_Block* _${id}_ = $block;   // can't be static b/c recursion
                ${fset(this.tk, set, id)}            
                    
            """.trimIndent()
            }
            is Expr.Set -> {
                val (scp, dst) = when (this.dst) {
                    is Expr.Index -> Pair(
                        "ceu_col_${this.dst.n}.tuple->block",
                        "ceu_col_${this.dst.n}.tuple->buf[(int) ceu_idx_${this.dst.n}.number]"
                    )

                    is Expr.Acc -> {
                        val id = this.dst.tk_.fromOp()
                        Pair("_${id}_", id)     // x = src / block of _x_
                    }

                    else -> error("bug found")
                }
                """
                { // SET
                    CEU_Value ceu_$n;
                    ${this.dst.code(block, null)}
                    ${this.src.code(block, Pair(scp, "ceu_$n"))}
                    $dst = ceu_$n;
                    ${fset(this.tk, set, "ceu_$n")}
                }
                    
                """.trimIndent()
                //Pair(s1+pre+s2+pos, "ceu_$n")
            }
            is Expr.If -> """
            { // IF
                CEU_Value ceu_cnd_$n;
                ${this.cnd.code(block, Pair(block!!, "ceu_cnd_$n"))}
                int ceu_ret_$n; {
                    switch (ceu_cnd_$n.tag) {
                        case CEU_TYPE_NIL:  { ceu_ret_$n=0; break; }
                        case CEU_TYPE_BOOL: { ceu_ret_$n=ceu_cnd_$n.bool; break; }
                        default: {                
                            ceu_throw = CEU_THROW_RUNTIME;
                            strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.cnd.tk.pos.lin}, col ${this.cnd.tk.pos.col}) : if error : invalid condition", 256);
                            break; // need to break again below
                        }
                    }
                    if (ceu_throw != CEU_THROW_NONE) {
                        break;  // break in switch above wont escape
                    }
                }
                if (ceu_ret_$n) {
                    ${this.t.code(block, set)}
                } else {
                    ${this.f.code(block, set)}
                }
            }
            
        """.trimIndent()
            is Expr.While -> """
            { // WHILE
                CEU_Value ceu_ret_$n;
                while (1) {
                    {
                        CEU_Value ceu_cnd_$n;
                        ${this.cnd.code(block, Pair(block!!, "ceu_cnd_$n"))}
                        int ceu_ret_$n; {
                            switch (ceu_cnd_$n.tag) {
                                case CEU_TYPE_NIL:  { ceu_ret_$n=0; break; }
                                case CEU_TYPE_BOOL: { ceu_ret_$n=ceu_cnd_$n.bool; break; }
                                default: {                
                                    ceu_throw = CEU_THROW_RUNTIME;
                                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.cnd.tk.pos.lin}, col ${this.cnd.tk.pos.col}) : if error : invalid condition", 256);
                                    break; // need to break again below
                                }
                            }
                            if (ceu_throw != CEU_THROW_NONE) {
                                break;  // break in switch above wont escape
                            }
                        }
                        if (!ceu_ret_$n) {
                            break;
                        }
                    }
                    ${this.body.code(block, Pair(block, "ceu_ret_$n"))}
                }
                ${fset(this.tk, set, "ceu_ret_$n")}            
            }
                
        """.trimIndent()
            is Expr.Func -> """
                CEU_Value ceu_func_$n (CEU_Block* ceu_scope, int ceu_n, CEU_Value* ceu_args[]) {
                    int ceu_i = 0;
                    ${
                        this.args.map {
                            """
                            CEU_Value ${it.str} = { CEU_TYPE_NIL };
                            if (ceu_i < ceu_n) {
                                ${it.str} = *ceu_args[ceu_i];
                            }
                            ceu_i++;
                            
                            """.trimIndent()
                        }.joinToString("")
                    }
                    CEU_Value ceu_$n;
                    do {
                        ${this.body.code(null, Pair("ceu_scope", "ceu_$n"))}
                    } while (0);
                    return ceu_$n;
                }
                ${fset(this.tk, set, "((CEU_Value) { CEU_TYPE_FUNC, {.func=ceu_func_$n} })")}            
    
            """.trimIndent()
            is Expr.Throw -> """
                { // THROW
                    CEU_Value ceu_ex_$n;
                    ${this.ex.code(block, Pair(block!!, "ceu_ex_$n"))}
                    ${this.arg.code(block, Pair("ceu_block_global", "ceu_throw_arg"))}  // arg scope to be set in catch set
                    if (ceu_ex_$n.tag == CEU_TYPE_NUMBER) {
                        ceu_throw = ceu_ex_$n.number;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception", 256);
                    } else {                
                        ceu_throw = CEU_THROW_RUNTIME;
                        strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : invalid exception : expected number", 256);
                    }
                    break;
                }
        
            """.trimIndent()
            is Expr.Catch -> {
                val scp = if (set != null) set.first else block!!
                """
                CEU_Throw ceu_catch_n_$n; {
                    CEU_Value ceu_catch_$n;
                    ${this.catch.code(block, Pair(block!!, "ceu_catch_$n"))}
                    assert(ceu_catch_$n.tag == CEU_TYPE_NUMBER && "catch error : invalid exception : expected number");
                    ceu_catch_n_$n = ceu_catch_$n.number;
                }
                do {
                    ${this.body.code(block, set)}
                } while (0);
                if (ceu_throw != CEU_THROW_NONE) {          // pending throw
                    if (ceu_throw == ceu_catch_n_$n) {      // CAUGHT: reset throw, set arg
                        ${fset(this.tk, set, "ceu_throw_arg")}
                        if (ceu_throw_arg.tag==CEU_TYPE_TUPLE && ceu_block_global!=$scp) {
                            // assign ceu_throw_arg to set.first
                            ceu_block_move(ceu_throw_arg.tuple, ceu_block_global, $scp);
                        }
                        ceu_throw = CEU_THROW_NONE;
                    } else {                                // UNCAUGHT: escape to outer
                        break;
                    }
                }
                """.trimIndent()
            }
            is Expr.Spawn -> TODO()
            is Expr.Resume -> TODO()
            is Expr.Yield -> TODO()

            is Expr.Nat -> {
                val (ids,body) = this.tk.str.drop(1).dropLast(1).let {
                    var ret = ""
                    var i = 0

                    var lin = 1
                    var col = 1
                    fun read (): Char {
                        //assert(i < it.length) { "bug found" }
                        if (i >= it.length) {
                            parser.lexer.err(tk, "native error : (lin $lin, col $col) : unterminated token")
                        }
                        val x = it[i++]
                        if (x == '\n') {
                            lin++; col=0
                        } else {
                            col++
                        }
                        return x
                    }

                    val ids = mutableListOf<String>()
                    while (i < it.length) {
                        ret += if (it[i] != '$') read() else {
                            read()
                            val (l,c) = Pair(lin,col)
                            var id = ""
                            var x = read()
                            while (x.isLetterOrDigit() || x=='_') {
                                id += x
                                x = read()
                            }
                            if (id.length == 0) {
                                parser.lexer.err(tk, "native error : (lin $l, col $c) : invalid identifier")
                            }
                            ids.add(id)
                            "($id.number)$x"
                        }
                    }
                    Pair(ids,ret)
                }
                """
                {
                    float ceu_f_$n (void) {
                        ${ids.map { "$it.tag = CEU_TYPE_NUMBER;\n" }.joinToString("") }
                        $body
                        return 0;
                    }
                    CEU_Value ceu_$n = { CEU_TYPE_NUMBER, {.number=ceu_f_$n()} };
                    if (ceu_throw != CEU_THROW_NONE) {
                        break;
                    }
                    ${fset(this.tk, set, "ceu_$n")}
                }
                """.trimIndent()
            }
            is Expr.Acc -> fset(this.tk, set, this.tk_.fromOp())
            is Expr.Nil -> fset(this.tk, set, "((CEU_Value) { CEU_TYPE_NIL })")
            is Expr.Tag -> {
                val tag = this.tk.str.drop(1)
                """
                    #ifndef CEU_TAG_$tag
                    #define CEU_TAG_$tag //__COUNTER__
                    static CEU_Tags ceu_tag_$tag = { "@$tag", NULL };
                    ceu_tag_$tag.next = CEU_TAGS;
                    CEU_TAGS = &ceu_tag_$tag;
                    CEU_TAGS_MAX++;
                    #endif
                    //{fset(this.tk, set, "((CEU_Value) { CEU_TYPE_TAG, {._tag_=CEU_TAG_$tag} })")}
                    ${fset(this.tk, set, "((CEU_Value) { CEU_TYPE_TAG, {._tag_=ceu_tag_from_string(\"@$tag\")} })")}
                """.trimIndent()
            }
            is Expr.Bool -> fset(
                this.tk,
                set,
                "((CEU_Value) { CEU_TYPE_BOOL, {.bool=${if (this.tk.str == "true") 1 else 0}} })"
            )
            is Expr.Num -> fset(this.tk, set, "((CEU_Value) { CEU_TYPE_NUMBER, {.number=${this.tk.str}} })")
            is Expr.Tuple -> {
                assert(this.args.size <= 256) { "bug found" }
                val scp = if (set == null) block!! else set.first
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code(block, Pair(scp, "ceu_${i}_$n"))
                }.joinToString("")
                """
                { // TUPLE
                    ${this.args.mapIndexed { i, _ -> "CEU_Value ceu_${i}_$n;\n" }.joinToString("")}
                    $args
                    CEU_Value ceu_sta_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_${i}_$n" }.joinToString(",")}
                    };
                    CEU_Value* ceu_dyn_$n = malloc(${this.args.size} * sizeof(CEU_Value));
                    assert(ceu_dyn_$n != NULL);
                    memcpy(ceu_dyn_$n, ceu_sta_$n, ${this.args.size} * sizeof(CEU_Value));
                    CEU_Value_Tuple* ceu_$n = malloc(sizeof(CEU_Value_Tuple));
                    assert(ceu_$n != NULL);
                    *ceu_$n = (CEU_Value_Tuple) { $scp, $scp->tofree, ceu_dyn_$n, ${this.args.size} };
                    $scp->tofree = ceu_$n;
                    ${fset(this.tk, set, "((CEU_Value) { CEU_TYPE_TUPLE, {.tuple=ceu_$n} })")}
                }
    
                """.trimIndent()
            }
            is Expr.Index -> """
            //{ // INDEX    // (removed {} b/c set uses col[idx])
                CEU_Value ceu_col_$n;
                ${this.col.code(block, Pair(block!!, "ceu_col_$n"))}
                if (ceu_col_$n.tag != CEU_TYPE_TUPLE) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : index error : expected tuple", 256);
                    break;
                }
                                
                CEU_Value ceu_idx_$n;
                ${this.idx.code(block, Pair(block, "ceu_idx_$n"))}
                if (ceu_idx_$n.tag != CEU_TYPE_NUMBER) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : expected number", 256);
                    break;
                }
                
                if (ceu_col_$n.tag != CEU_TYPE_TUPLE) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : index error : expected tuple", 256);
                    break;
                }
                
                if (ceu_col_$n.tuple->n <= ceu_idx_$n.number) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : index error : out of bounds", 256);
                    break;
                }

                ${fset(this.tk, set, "ceu_col_$n.tuple->buf[(int) ceu_idx_$n.number]")}
            //}
            
        """.trimIndent()
            is Expr.Call -> """
            { // CALL
                CEU_Value ceu_f_$n;
                ${this.f.code(block, Pair(block!!, "ceu_f_$n"))}
                if (ceu_f_$n.tag != CEU_TYPE_FUNC) {                
                    ceu_throw = CEU_THROW_RUNTIME;
                    strncpy(ceu_throw_msg, "${tk.pos.file} : (lin ${this.f.tk.pos.lin}, col ${this.f.tk.pos.col}) : call error : expected function", 256);
                    break;
                }
                ${
                    this.args.mapIndexed { i, _ ->
                        "CEU_Value ceu_${i}_$n;\n"
                    }.joinToString("")
                }
                ${
                    this.args.mapIndexed { i, it ->
                        it.code(block, Pair(block, "ceu_${i}_$n"))
                    }.joinToString("")
                }
                CEU_Value* ceu_args_$n[] = { ${this.args.mapIndexed { i, _ -> "&ceu_${i}_$n" }.joinToString(",")} };
                CEU_Value ceu_$n = ceu_f_$n.func(
                    ${if (set == null) block else set.first},
                    ${this.args.size},
                    ceu_args_$n
                );
                if (ceu_throw != CEU_THROW_NONE) {
                    break;
                }
                ${fset(this.tk, set, "ceu_$n")}
            }

        """.trimIndent()
        }
    }

    fun List<Expr>.code(block: String, set: Pair<String, String>?): String {
        return this.mapIndexed { i, it ->
            it.code(block, if (i == this.size - 1) set else null) + "\n"
        }.joinToString("")
    }

    fun expr (es: Expr.Block): String {
        return """
            #include <stdio.h>
            #include <stdlib.h>
            #include <stdint.h>
            #include <string.h>
            #include <assert.h>
            #include <stdarg.h>
    
            typedef enum CEU_TYPE {
                CEU_TYPE_NIL,
                CEU_TYPE_TAG,
                CEU_TYPE_BOOL,
                CEU_TYPE_NUMBER,
                CEU_TYPE_TUPLE,
                CEU_TYPE_FUNC
            } CEU_TYPE;
            
            struct CEU_Value;
            struct CEU_Block;
            struct CEU_Stack;
            
            typedef struct CEU_Value_Tuple {
                struct CEU_Block* block;        // compare on set
                struct CEU_Value_Tuple* nxt;    // next in block->tofree
                struct CEU_Value* buf;
                uint8_t n;
            } CEU_Value_Tuple;
            typedef struct CEU_Value {
                int tag;
                union {
                    //void nil;
                    int _tag_;
                    int bool;
                    float number;
                    CEU_Value_Tuple* tuple;
                    struct CEU_Value (*func) (struct CEU_Block* ret, int n, struct CEU_Value* args[]);
                };
            } CEU_Value;
            
            typedef struct CEU_Block {
                uint8_t depth;              // compare on set
                CEU_Value_Tuple* tofree;    // list of allocated tuples to free on exit
            } CEU_Block;
            void ceu_block_free (CEU_Block* block) {
                while (block->tofree != NULL) {
                    CEU_Value_Tuple* cur = block->tofree;
                    block->tofree = block->tofree->nxt;
                    free(cur->buf);
                    free(cur);
                }
            }
            void ceu_block_move (CEU_Value_Tuple* V, CEU_Block* FR, CEU_Block* TO) {
                CEU_Value_Tuple* prv = NULL;
                CEU_Value_Tuple* cur = FR->tofree;
                while (cur != NULL) {
                    if (cur == V) {
                        if (prv == NULL) {
                            FR->tofree = NULL;
                        } else {
                            prv->nxt = cur->nxt;
                        }              
                        //assert(0 && "OK");
                        cur->block = TO;
                        cur->nxt = TO->tofree;
                        TO->tofree = cur;
                        break;
                    }
                    prv = cur;
                    cur = cur->nxt;
                }
            }
    
            typedef struct CEU_Tags {
                char* name;
                struct CEU_Tags* next;
            } CEU_Tags;
            static CEU_Tags* CEU_TAGS = NULL;
            int CEU_TAGS_MAX = 0;
            char* ceu_tag_to_string (int tag) {
                CEU_Tags* cur = CEU_TAGS;
                for (int i=0; i<CEU_TAGS_MAX-tag-1; i++) {
                    cur = cur->next;
                }
                return cur->name;
            }
            int ceu_tag_from_string (char* name) {
                int ret = 0;
                CEU_Tags* cur = CEU_TAGS;
                while (cur!=NULL && strcmp(cur->name,name)) {
                    cur = cur->next;
                    ret++;
                }
                return CEU_TAGS_MAX-1-ret;
            }
            CEU_Value ceu_tags (CEU_Block* ret, int n, CEU_Value* args[]) {
                assert(n == 1 && "bug found");
                return (CEU_Value) { CEU_TYPE_TAG, {._tag_=args[0]->tag} };
            }
            
            void ceu_print1 (CEU_Value* v) {
                switch (v->tag) {
                    case CEU_TYPE_NIL:
                        printf("nil");
                        break;
                    case CEU_TYPE_TAG:
                        printf("%s", ceu_tag_to_string(v->_tag_));
                        break;
                    case CEU_TYPE_BOOL:
                        if (v->bool) {
                            printf("true");
                        } else {
                            printf("false");
                        }
                        break;
                    case CEU_TYPE_NUMBER:
                        printf("%g", v->number);
                        break;
                    case CEU_TYPE_TUPLE:
                        printf("[");
                        for (int i=0; i<v->tuple->n; i++) {
                            if (i > 0) {
                                printf(",");
                            }
                            ceu_print1(&v->tuple->buf[i]);
                        }                    
                        printf("]");
                        break;
                    case CEU_TYPE_FUNC:
                        printf("func: %p", v->func);
                        break;
                    default:
                        assert(0 && "bug found");
                }
            }
            CEU_Value ceu_print (CEU_Block* ret, int n, CEU_Value* args[]) {
                for (int i=0; i<n; i++) {
                    ceu_print1(args[i]);
                }
                return (CEU_Value) { CEU_TYPE_NIL };
            }
            CEU_Value ceu_println (CEU_Block* ret, int n, CEU_Value* args[]) {
                ceu_print(ret, n, args);
                printf("\n");
                return (CEU_Value) { CEU_TYPE_NIL };
            }
            
            CEU_Value ceu_op_eq_eq (CEU_Block* ret, int n, CEU_Value* args[]) {
                assert(n == 2);
                CEU_Value* e1 = args[0];
                CEU_Value* e2 = args[1];
                int v = (e1->tag == e2->tag);
                if (v) {
                    switch (e1->tag) {
                        case CEU_TYPE_NIL:
                            v = 1;
                            break;
                        case CEU_TYPE_TAG:
                            v = (e1->_tag_ == e2->_tag_);
                            break;
                        case CEU_TYPE_BOOL:
                            v = (e1->bool == e2->bool);
                            break;
                        case CEU_TYPE_NUMBER:
                            v = (e1->number == e2->number);
                            break;
                        case CEU_TYPE_TUPLE:
                            v = (e1->tuple == e2->tuple);
                            break;
                        case CEU_TYPE_FUNC:
                            v = (e1->func == e2->func);
                            break;
                        default:
                            assert(0 && "bug found");
                    }
                }
                return (CEU_Value) { CEU_TYPE_BOOL, {.bool=v} };
            }
            CEU_Value ceu_op_not_eq (CEU_Block* ret, int n, CEU_Value* args[]) {
                CEU_Value v = ceu_op_eq_eq(ret, n, args);
                v.bool = !v.bool;
                return v;
            }

            CEU_Value tags      = { CEU_TYPE_FUNC, {.func=ceu_tags}      };
            CEU_Value print     = { CEU_TYPE_FUNC, {.func=ceu_print}     };
            CEU_Value println   = { CEU_TYPE_FUNC, {.func=ceu_println}   };            
            CEU_Value op_eq_eq  = { CEU_TYPE_FUNC, {.func=ceu_op_eq_eq}  };
            CEU_Value op_not_eq = { CEU_TYPE_FUNC, {.func=ceu_op_not_eq} };

            typedef enum {
                CEU_THROW_NONE = 0,
                CEU_THROW_RUNTIME
            } CEU_Throw;
            CEU_Throw ceu_throw = CEU_THROW_NONE;
            CEU_Value ceu_throw_arg;
            CEU_Block* ceu_block_global = NULL;     // used as throw scope. then, catch fixes it
            char ceu_throw_msg[256];
    
            int main (void) {
                {             
                    #define CEU_TAG_nil //__COUNTER__
                    static CEU_Tags ceu_tag_nil = { "@nil", NULL };
                    ceu_tag_nil.next = CEU_TAGS;
                    CEU_TAGS = &ceu_tag_nil;
                    CEU_TAGS_MAX++;
        
                    #define CEU_TAG_tag //__COUNTER__
                    static CEU_Tags ceu_tag_tag = { "@tag", NULL };
                    ceu_tag_tag.next = CEU_TAGS;
                    CEU_TAGS = &ceu_tag_tag;
                    CEU_TAGS_MAX++;
                
                    #define CEU_TAG_bool //__COUNTER__
                    static CEU_Tags ceu_tag_bool = { "@bool", NULL };
                    ceu_tag_bool.next = CEU_TAGS;
                    CEU_TAGS = &ceu_tag_bool;
                    CEU_TAGS_MAX++;
    
                    #define CEU_TAG_number //__COUNTER__
                    static CEU_Tags ceu_tag_number = { "@number", NULL };
                    ceu_tag_number.next = CEU_TAGS;
                    CEU_TAGS = &ceu_tag_number;
                    CEU_TAGS_MAX++;
    
                    #define CEU_TAG_tuple //__COUNTER__
                    static CEU_Tags ceu_tag_tuple = { "@tuple", NULL };
                    ceu_tag_tuple.next = CEU_TAGS;
                    CEU_TAGS = &ceu_tag_tuple;
                    CEU_TAGS_MAX++;
    
                    #define CEU_TAG_func //__COUNTER__
                    static CEU_Tags ceu_tag_func = { "@func", NULL };
                    ceu_tag_func.next = CEU_TAGS;
                    CEU_TAGS = &ceu_tag_func;
                    CEU_TAGS_MAX++;
                }
                //assert(CEU_TAG_nil == CEU_TYPE_NIL);

                do {
                    ${es.code(null, null)}
                    return 0;
                } while (0);
                fprintf(stderr, "%s\n", ceu_throw_msg);
                return 1;
            }
        """.trimIndent()
    }
}