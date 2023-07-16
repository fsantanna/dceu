package dceu

fun Coder.main (tags: Tags): String {
    return ("" +
    """ // INCLUDES / PROTOS
        //#define CEU_DEBUG
        #include <stdio.h>
        #include <stdlib.h>
        #include <stddef.h>
        #include <stdint.h>
        #include <string.h>
        #include <assert.h>
        #include <stdarg.h>
        #include <time.h>
        #include <math.h>

        #undef MAX
        #undef MIN
        #define MAX(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a > _b ? _a : _b; })
        #define MIN(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a < _b ? _a : _b; })

        struct CEU_Value;
            struct CEU_Dyn;
            struct CEU_Frame;        
            struct CEU_Block;
            struct CEU_Proto;
        struct CEU_Tags_List;
        struct CEU_Tags_Names;
        struct CEU_Block;
        struct CEU_Error_List;

        typedef enum {
            CEU_RET_THROW = 0,  // going up with throw
            CEU_RET_RETURN
        } CEU_RET;

        typedef enum {
            CEU_HOLD_FLEETING = 0,  // not assigned, dst assigns
            CEU_HOLD_MUTABLE,       // set and assignable to narrow 
            CEU_HOLD_IMMUTABLE,     // set but not assignable across unsafe (even if same/narrow)
            CEU_HOLD_MAX
        } CEU_HOLD;

        typedef enum {
            CEU_ARG_ERR = -2,
            CEU_ARG_ARGS = 0    // 0,1,...
        } CEU_ARG;

        CEU_RET ceu_type_f (struct CEU_Frame* _1, int n, struct CEU_Value* args[]);
        int ceu_as_bool (struct CEU_Value* v);
        
        #define CEU_ISGLBDYN(dyn) (dyn->hold.up_block == ceu_block_global)

        #define CEU_THROW_MSG(msg) {                       \
            static CEU_Error_List err = { msg, 0, NULL };  \
            err.shown = 0;                                 \
            if (ceu_error_list != NULL) {                  \
                err.next = ceu_error_list;                 \
            }                                              \
            ceu_error_list = &err;                         \
        }
        #define CEU_THROW_DO_MSG(v,s,msg) { CEU_THROW_MSG(msg); CEU_THROW_DO(v,s); }
        #define CEU_THROW_DO(v,s) { ceu_ret=CEU_RET_THROW; ceu_acc=v; s; }
        #define CEU_THROW_RET(v) { ceu_acc=v; return CEU_RET_THROW; }
        #define CEU_CONTINUE_ON_THROW_MSG(msg) {    \
            if (ceu_ret == CEU_RET_THROW) {         \
                CEU_THROW_MSG(msg);                  \
                continue;                           \
            }                                       \
        }
        #define CEU_CONTINUE_ON_THROW() { if (ceu_ret==CEU_RET_THROW) { continue; } }

        CEU_RET ceu_tags_f (struct CEU_Frame* _1, int n, struct CEU_Value* args[]);
        char* ceu_tag_to_string (int tag);
        int ceu_string_dash_to_dash_tag (char* str);
        
        void ceu_dyn_free (struct CEU_Dyn* dyn);
        void ceu_dyns_free (struct CEU_Block* dyns);
        void ceu_block_free (struct CEU_Block* block);
        
        void ceu_gc_inc (struct CEU_Value* v);
        void ceu_gc_dec (struct CEU_Value* v, int chk);

        void ceu_hold_add (struct CEU_Block* dyns, struct CEU_Dyn* dyn);
        void ceu_hold_rem (struct CEU_Dyn* dyn);
        void ceu_block_set (struct CEU_Dyn* src, struct CEU_Block* dst_blk, CEU_HOLD dst_tphold);
        
        struct CEU_Dyn* ceu_vector_create (struct CEU_Block* hld);
        struct CEU_Dyn* ceu_tuple_create (struct CEU_Block* hld, int n);
        
        int ceu_tag_to_size (int type);
        void ceu_max_depth (struct CEU_Dyn* dyn, int n, struct CEU_Value* childs);
        CEU_RET ceu_vector_get (struct CEU_Dyn* vec, int i);
        int ceu_vector_set (struct CEU_Dyn* vec, int i, struct CEU_Value v);
        struct CEU_Dyn* ceu_vector_from_c_string (struct CEU_Block* hld, const char* str);
        
        int ceu_dict_key_to_index (struct CEU_Dyn* col, struct CEU_Value* key, int* idx);
        struct CEU_Value ceu_dict_get (struct CEU_Dyn* col, struct CEU_Value* key);
        int ceu_dict_set (struct CEU_Dyn* col, struct CEU_Value* key, struct CEU_Value* val);
        CEU_RET ceu_col_check (struct CEU_Value* col, struct CEU_Value* idx);
        
        int ceu_tuple_set (struct CEU_Dyn* tup, int i, struct CEU_Value v);

        void ceu_print1 (struct CEU_Frame* _1, struct CEU_Value* v);
        CEU_RET ceu_op_equals_equals_f (struct CEU_Frame* _1, int n, struct CEU_Value* args[]);
    """ +
    """ // CEU_Value
        typedef enum CEU_VALUE {
            CEU_VALUE_NIL = 0,
            CEU_VALUE_TAG,
            CEU_VALUE_BOOL,
            CEU_VALUE_CHAR,
            CEU_VALUE_NUMBER,
            CEU_VALUE_POINTER,
            CEU_VALUE_DYNAMIC,    // all below are dynamic
            CEU_VALUE_P_FUNC,     // prototypes func, coro, task
            CEU_VALUE_TUPLE,
            CEU_VALUE_VECTOR,
            CEU_VALUE_DICT
        } CEU_VALUE;
        
        typedef struct CEU_Value {
            CEU_VALUE type;
            union {
                //void nil;
                unsigned int Tag;
                int Bool;
                char Char;
                double Number;
                void* Pointer;
                struct CEU_Dyn* Dyn;    // Func/Task/Tuple/Dict/Coro/Tasks: allocates memory
            };
        } CEU_Value;
    """ +
    """ // CEU_Proto / CEU_Frame
        typedef CEU_RET (*CEU_Proto_F) (
            struct CEU_Frame* frame,
            int n,
            struct CEU_Value* args[]
        );
        
        typedef struct CEU_Proto {  // lexical func/task
            struct CEU_Frame* up_frame;   // points to active frame
            CEU_Proto_F f;
            struct {
                int its;     // number of upvals
                CEU_Value* buf;
            } upvs;
        } CEU_Proto;
        
        typedef struct CEU_Frame {          // call func / create task
            struct CEU_Dyn* proto;
            struct CEU_Block* up_block;     // block enclosing this call/coroutine
            char* mem;
        } CEU_Frame;
    """ +
    """ // CEU_Dyn
        typedef struct CEU_Hold {
            struct CEU_Block* up_block;
            struct CEU_Dyn* nxt_dyn;
            CEU_HOLD type;
        } CEU_Hold;

        typedef struct CEU_Dyn {
            CEU_VALUE type;                 // required to switch over free/bcast
            CEU_Hold hold;
            struct CEU_Tags_List* tags;     // linked list of tags
            int refs;                       // number of refs to it (free when 0)
            union {
                CEU_Proto Proto;            // func, coro, task
                struct {
                    int its;                // number of items
                    CEU_Value buf[0];       // beginning of CEU_Value[n]
                } Tuple;
                struct {
                    int max;                // size of buf
                    int its;                // number of items
                    CEU_VALUE type;
                    char* buf;              // resizable Unknown[n]
                } Vector;
                struct {
                    int max;                // size of buf
                    CEU_Value (*buf)[0][2]; // resizable CEU_Value[n][2]
                } Dict;
            };
        } CEU_Dyn;
    """ +
    """ // CEU_Block
        typedef struct CEU_Block {
            int depth;                  // compare on set
            int ispub;                  // is top block inside task?
            struct CEU_Frame* up_frame; // enclosing active frame
            struct CEU_Block* dn_block; // nested block active
            struct CEU_Dyn* fst_dyn;    // list of allocated data to bcast/free
        } CEU_Block;
    """ +
    """ // CEU_Tags
        typedef struct CEU_Tags_Names {
            int tag;
            char* name;
            struct CEU_Tags_Names* next;
        } CEU_Tags_Names;
        
        typedef struct CEU_Tags_List {
            int tag;
            struct CEU_Tags_List* next;
        } CEU_Tags_List;

        typedef struct CEU_Error_List {
            char* msg;
            int shown;
            struct CEU_Error_List* next;
        } CEU_Error_List;
    """ +
    """ // GLOBALS
        struct CEU_Dyn* ceu_proto_create (int type, struct CEU_Proto proto, struct CEU_Block* hld, CEU_HOLD tphold);
        struct CEU_Dyn* ceu_dict_create  (struct CEU_Block* hld);

        int ceu_gc_count = 0;
        void* ceu_block_global = NULL;
        
        ${ tags.pub.values.let {
            fun f1 (l: List<List<String>>): List<Pair<String, List<List<String>>>> {
                return l
                    .groupBy { it.first() }
                    .toList()
                    .map {
                        Pair(it.first, it.second.map { it.drop(1) }.filter{it.size>0})
                    }
            }
            /*
            fun <T> f2 (l: List<Pair<String, List<List<String>>>>): List<Pair<String, T>> {
                return l.map {
                    val x = f1(it.second)
                    Pair(it.first, f2(x))
                }
            }
            */
            val l1 = it.map { it.first }.map { it.drop(1).split('.') }
            val l2 = f1(l1)
            val l3 = l2.map {
                val a = f1(it.second)
                val b = a.map {
                    val i = f1(it.second)
                    val j = i.map {
                        val x = f1(it.second)
                        Pair(it.first, x)
                    }
                    Pair(it.first, j)
                }
                Pair(it.first, b)
            }
            //println(l3)

            var last = "NULL"
            var i1 = 0
            l3.map { it1 ->
                val (s1,c1,e1) = tags.pub[':'+it1.first]!!
                val ie1 = e1 ?: i1++
                val prv1 = last
                last = "&ceu_tag_$c1"
                var i2 = 0
                """
                #define CEU_TAG_$c1 ($ie1)
                CEU_Tags_Names ceu_tag_$c1 = { CEU_TAG_$c1, "$s1", $prv1 };
                """ + it1.second.map { it2 ->
                    val (s2,c2,e2) = tags.pub[':'+it1.first+'.'+it2.first]!!
                    assert(e2 == null)
                    i2++
                    val prv2 = last
                    last = "&ceu_tag_$c2"
                    var i3 = 0
                    """
                    #define CEU_TAG_$c2 (($i2 << 8) | $ie1)
                    CEU_Tags_Names ceu_tag_$c2 = { CEU_TAG_$c2, "$s2", $prv2 };
                    """ + it2.second.map { it3 ->
                        val (s3,c3,e3) = tags.pub[':'+it1.first+'.'+it2.first+'.'+it3.first]!!
                        assert(e3 == null)
                        i3++
                        val prv3 = last
                        last = "&ceu_tag_$c3"
                        var i4 = 0
                        """
                        #define CEU_TAG_$c3 (($i3 << 16) | ($i2 << 8) | $ie1)
                        CEU_Tags_Names ceu_tag_$c3 = { CEU_TAG_$c3, "$s3", $prv3 };
                        """ + it3.second.map { it4 ->
                            val (s4,c4,e4) = tags.pub[':'+it1.first+'.'+it2.first+'.'+it3.first+'.'+it4.first]!!
                            assert(e4 == null)
                            i4++
                            val prv4 = last
                            last = "&ceu_tag_$c4"
                            """
                            #define CEU_TAG_$c4 (($i4 << 24) | ($i3 << 16) | ($i2 << 8) | $ie1)
                            CEU_Tags_Names ceu_tag_$c4 = { CEU_TAG_$c4, "$s4", $prv4 };
                            """
                        }
                        .joinToString("")
                    }
                    .joinToString("")
                 }
                 .joinToString("")
            }
            .joinToString("") + """
                CEU_Tags_Names* CEU_TAGS = $last;
            """
        }}

        const CEU_Value CEU_ERR_ERROR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_error} };
        CEU_Error_List* ceu_error_list = NULL;
        
        CEU_RET ceu_ret = CEU_RET_RETURN;
        CEU_Value ceu_acc;        
    """ +
    """ // IMPLS
        void ceu_error_list_print (void) {
            CEU_Error_List* cur = ceu_error_list;
            while (cur != NULL) {
                char* msg = (cur->msg[0] == '\0') ? cur->msg+1 : cur->msg;
                if (cur->next!=NULL && cur->next->msg[0]=='\0') {
                    fprintf(stderr, "%s", msg);
                } else {
                    fprintf(stderr, "%s\n", msg);
                }
                if (cur->shown) {
                    fprintf(stderr, "-=- duplicate exception : stop now -=-\n");
                    break;
                }
                cur->shown = 1;
                cur = cur->next;
            }
            CEU_Value ceu_accx = ceu_acc;
            ceu_print1(NULL, &ceu_accx);
            puts("");
        }
        
        CEU_Value* ceu_dyn_to_val (CEU_Dyn* dyn) {
            static CEU_Value val;
            val = (CEU_Value) { dyn->type, .Dyn=dyn };
            return &val;
        }
        
        int ceu_as_bool (CEU_Value* v) {
            return !(v->type==CEU_VALUE_NIL || (v->type==CEU_VALUE_BOOL && !v->Bool));
        }
        CEU_RET ceu_type_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(n == 1 && "bug found");
            ceu_acc = (CEU_Value) { CEU_VALUE_TAG, {.Tag=args[0]->type} };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_sup_question__f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(n >= 2);
            CEU_Value* sup = args[0];
            CEU_Value* sub = args[1];
            assert(sup->type == CEU_VALUE_TAG);
            assert(sub->type == CEU_VALUE_TAG);
            
            //printf("sup=0x%08X vs sub=0x%08X\n", sup->Tag, sub->Tag);
            int sup0 = sup->Tag & 0x000000FF;
            int sup1 = sup->Tag & 0x0000FF00;
            int sup2 = sup->Tag & 0x00FF0000;
            int sup3 = sup->Tag & 0xFF000000;
            int sub0 = sub->Tag & 0x000000FF;
            int sub1 = sub->Tag & 0x0000FF00;
            int sub2 = sub->Tag & 0x00FF0000;
            int sub3 = sub->Tag & 0xFF000000;
            
            ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, { .Bool =
                (sup0 == sub0) && ((sup1 == 0) || (
                    (sup1 == sub1) && ((sup2 == 0) || (
                        (sup2 == sub2) && ((sup3 == 0) || (
                            (sup3 == sub3)
                        ))
                    ))
                ))
            } };

            return CEU_RET_RETURN;
        }
        CEU_RET ceu_tags_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n >= 1);
            CEU_Value* dyn = args[0];
            CEU_Tags_List* tags = (dyn->type < CEU_VALUE_DYNAMIC) ? NULL : dyn->Dyn->tags;
            CEU_Value* tag = NULL;
            if (n >= 2) {
                tag = args[1];
                assert(tag->type == CEU_VALUE_TAG);
            }
            switch (n) {
                case 1: {
                    int len = 0; {
                        CEU_Tags_List* cur = tags;
                        while (cur != NULL) {
                            len++;
                            cur = cur->next;
                        }
                    }
                    CEU_Dyn* tup = ceu_tuple_create(frame->up_block, len);
                    {
                        CEU_Tags_List* cur = tags;
                        int i = 0;
                        while (cur != NULL) {
                            assert(ceu_tuple_set(tup, i++, (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} }));
                            cur = cur->next;
                        }
                    }                    
                    ceu_acc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=tup} };
                    break;
                }
                case 2: {   // check
                    ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} };
                    CEU_Tags_List* cur = tags;
                    while (cur != NULL) {
                        CEU_Value x = (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} };
                        CEU_Value* args[] = { tag, &x };
                        assert(CEU_RET_RETURN == ceu_sup_question__f(frame, 2, args));
                        if (ceu_acc.Bool) {
                            break;
                        }
                        cur = cur->next;
                    }
                    break;
                }
                case 3: {   // add/rem
                    assert(dyn->type > CEU_VALUE_DYNAMIC);
                    CEU_Value* bool = args[2];
                    assert(bool->type == CEU_VALUE_BOOL);
                    if (bool->Bool) {   // add
                        ceu_tags_f(frame, 2, args);
                        if (ceu_acc.Bool) {
                            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                        } else {
                            CEU_Tags_List* v = malloc(sizeof(CEU_Tags_List));
                            assert(v != NULL);
                            v->tag = tag->Tag;
                            v->next = dyn->Dyn->tags;
                            dyn->Dyn->tags = v;
                            ceu_acc = *dyn;
                        }
                    } else {            // rem
                        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                        CEU_Tags_List** cur = &dyn->Dyn->tags;
                        while (*cur != NULL) {
                            if ((*cur)->tag == tag->Tag) {
                                CEU_Tags_List* v = *cur;
                                *cur = v->next;
                                free(v);
                                ceu_acc = *dyn;
                                break;
                            }
                            cur = &(*cur)->next;
                        }
                    }
                    break;
                }
            }
            return CEU_RET_RETURN;
        }
        char* ceu_tag_to_string (int tag) {
            CEU_Tags_Names* cur = CEU_TAGS;
            while (cur != NULL) {
                if (cur->tag == tag) {
                    return cur->name;
                }
                cur = cur->next;
            }
            assert(0 && "bug found");
        }
        CEU_RET ceu_string_dash_to_dash_tag_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* str = args[0];
            assert(str->type==CEU_VALUE_VECTOR && str->Dyn->Vector.type==CEU_VALUE_CHAR);
            CEU_Tags_Names* cur = CEU_TAGS;
            while (cur != NULL) {
                if (!strcmp(cur->name,str->Dyn->Vector.buf)) {
                    ceu_acc = (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} };
                    return CEU_RET_RETURN;
                }
                cur = cur->next;
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
            return CEU_RET_RETURN;
        }
    """ +
    """ // GC
        void ceu_gc_free (CEU_Dyn* dyn) {
            switch (dyn->type) {
                case CEU_VALUE_P_FUNC:
                    for (int i=0; i<dyn->Proto.upvs.its; i++) {
                        ceu_gc_dec(&dyn->Proto.upvs.buf[i], 1);
                    }
                    break;
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<dyn->Tuple.its; i++) {
                        ceu_gc_dec(&dyn->Tuple.buf[i], 1);
                    }
                    break;
                case CEU_VALUE_VECTOR:
                    for (int i=0; i<dyn->Vector.its; i++) {
                        CEU_Value xacc = ceu_acc;
                        assert(CEU_RET_RETURN == ceu_vector_get(dyn, i));
                        ceu_gc_dec(&ceu_acc, 1);
                        ceu_acc = xacc;
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<dyn->Dict.max; i++) {
                        ceu_gc_dec(&(*dyn->Dict.buf)[i][0], 1);
                        ceu_gc_dec(&(*dyn->Dict.buf)[i][1], 1);
                    }
                    break;
                default:
                    assert(0);
                    break;
            }
            ceu_gc_count++;
            ceu_hold_rem(dyn);
            ceu_dyn_free(dyn);
        }
        
        void ceu_gc_chk (CEU_Dyn* dyn) {
            assert(dyn->type > CEU_VALUE_DYNAMIC);
            if (dyn->refs == 0) {
                ceu_gc_free(dyn);
            }
        }

        // var x = ?        // var source
        // set x = ?        // set source
        // set x[...] = ?   //      - index value
        // set x[?] = ?     //      - index key
        // set x.pub = ?    //      - pub
        // [...?...]        // constructor argument     // TODO
        // f(?)             // call argument
        // closure
        
        void ceu_gc_inc (struct CEU_Value* new) {
            if (new->type > CEU_VALUE_DYNAMIC) {
                new->Dyn->refs++;
            }
        }
        
        void ceu_gc_dec (struct CEU_Value* old, int chk) {
            if (old->type > CEU_VALUE_DYNAMIC) {
                old->Dyn->refs--;
                if (chk) {
                    ceu_gc_chk(old->Dyn);
                }
            }
        }
    """ +
    """ // BLOCK
        void ceu_hold_add (CEU_Block* blk, CEU_Dyn* dyn) {
            assert(dyn->hold.nxt_dyn == NULL);
            dyn->hold.up_block = blk;
            dyn->hold.nxt_dyn = blk->fst_dyn;
            blk->fst_dyn = dyn;
        }
        void ceu_hold_rem (CEU_Dyn* dyn) {
            CEU_Dyn** ptr = &dyn->hold.up_block->fst_dyn;
            do {
                if (*ptr == dyn) {
                    *ptr = dyn->hold.nxt_dyn;
                    break;
                }
            } while (ptr = &(*ptr)->hold.nxt_dyn);
            dyn->hold.nxt_dyn = NULL;
            dyn->hold.up_block = NULL;
        }

        void ceu_dyn_free (CEU_Dyn* dyn) {
            while (dyn->tags != NULL) {
                CEU_Tags_List* tag = dyn->tags;
                dyn->tags = tag->next;
                free(tag);
            }
            switch (dyn->type) {
                case CEU_VALUE_P_FUNC:
                    free(dyn->Proto.upvs.buf);
                    break;
                case CEU_VALUE_TUPLE:       // buf w/ dyn
                    break;
                case CEU_VALUE_VECTOR:
                    free(dyn->Vector.buf);
                    break;
                case CEU_VALUE_DICT:
                    free(dyn->Dict.buf);
                    break;
                default:
                    assert(0 && "bug found");
            }
            free(dyn);
        }
        
        void ceu_dyns_free (CEU_Block* blk) {
            CEU_Dyn* cur = blk->fst_dyn;
            while (cur != NULL) {
                CEU_Dyn* old = cur;
                cur = old->hold.nxt_dyn;
                ceu_dyn_free(old);
            }
            blk->fst_dyn = NULL;
        }
        
        void ceu_block_free (CEU_Block* block) {
            ceu_dyns_free(block);
            if (block->dn_block != NULL) {
                ceu_block_free(block->dn_block);
                block->dn_block = NULL;
            }
        }

        int ceu_depth (CEU_Block* blk) {
            int base = (blk->up_frame->up_block == NULL) ? 0 : ceu_depth(blk->up_frame->up_block);
            return base + blk->depth;
        }
        
        void ceu_block_set (CEU_Dyn* src, CEU_Block* dst, CEU_HOLD tphold) {
            if (CEU_ISGLBDYN(src)) {
                return;
            }
            src->hold.type = MAX(src->hold.type,tphold);
            if (dst == NULL) {
                // caller: do not set block (only tphold)
            } else {
                if (ceu_depth(dst) < ceu_depth(src->hold.up_block)) {
                    ceu_hold_rem(src);
                    ceu_hold_add(dst, src);
                } else {
                    return;
                }
            }
            switch (src->type) {
                case CEU_VALUE_P_FUNC:
                    for (int i=0; i<src->Proto.upvs.its; i++) {
                        if (src->Proto.upvs.buf[i].type > CEU_VALUE_DYNAMIC) {
                            ceu_block_set(src->Proto.upvs.buf[i].Dyn, dst, tphold);
                        }
                    }
                    break;
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<src->Tuple.its; i++) {
                        if (src->Tuple.buf[i].type > CEU_VALUE_DYNAMIC) {
                            ceu_block_set(src->Tuple.buf[i].Dyn, dst, tphold);
                        }
                    }
                    break;
                case CEU_VALUE_VECTOR:
                    if (src->Vector.type > CEU_VALUE_DYNAMIC) {
                        int sz = ceu_tag_to_size(src->Vector.type);
                        for (int i=0; i<src->Vector.its; i++) {
                            ceu_block_set(*(CEU_Dyn**)(src->Vector.buf + i*sz), dst, tphold);
                        }
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<src->Dict.max; i++) {
                        if ((*src->Dict.buf)[i][0].type > CEU_VALUE_DYNAMIC) {
                            ceu_block_set((*src->Dict.buf)[i][0].Dyn, dst, tphold);
                        }
                        if ((*src->Dict.buf)[i][1].type > CEU_VALUE_DYNAMIC) {
                            ceu_block_set((*src->Dict.buf)[i][1].Dyn, dst, tphold);
                        }
                    }
                    break;
            }
        }
        
        int ceu_block_chk (CEU_Value* src, CEU_Block* dst_dyns, CEU_HOLD dst_tphold) {
            if (src->type > CEU_VALUE_DYNAMIC) {
                //printf(">>> dst=%d >= src=%d\n", ceu_depth(dst_dyns), ceu_depth(src->Dyn->hold.up_block));
            }
            if (src->type < CEU_VALUE_DYNAMIC) {
                return 1;
            } else if (dst_dyns == NULL) {
                return 1;
            } else {
                // ceu_block_chk_depth
                if (src->Dyn->hold.type == CEU_HOLD_FLEETING) {
                    return 1;
                } else if (CEU_ISGLBDYN(src->Dyn)) {
                    return 1;
                } else if (dst_dyns == src->Dyn->hold.up_block) {          // same block
                    return 1;
                } else if (ceu_depth(dst_dyns) >= ceu_depth(src->Dyn->hold.up_block)) {
                    return 1;
                } else {
                    //printf("<<< dst=%d >= src=%d\n", ceu_depth(dst_dyns), ceu_depth(src->Dyn->hold.up_block));
                    return 0;
                }
            }
        }
        int ceu_block_chk_set (CEU_Value* src, CEU_Block* dst_dyns, CEU_HOLD dst_tphold) {
            if (!ceu_block_chk(src, dst_dyns, dst_tphold)) {
                return 0;
            }
            if (src->type > CEU_VALUE_DYNAMIC) {
                ceu_block_set(src->Dyn, dst_dyns, dst_tphold);
            }
            return 1;
        }
    """ +
    """ // TUPLE / VECTOR / DICT
        #define ceu_sizeof(type, member) sizeof(((type *)0)->member)
        int ceu_tag_to_size (int type) {
            switch (type) {
                case CEU_VALUE_NIL:
                    return 0;
                case CEU_VALUE_TAG:
                    return ceu_sizeof(CEU_Value, Tag);
                case CEU_VALUE_BOOL:
                    return ceu_sizeof(CEU_Value, Bool);
                case CEU_VALUE_CHAR:
                    return ceu_sizeof(CEU_Value, Char);
                case CEU_VALUE_NUMBER:
                    return ceu_sizeof(CEU_Value, Number);
                case CEU_VALUE_POINTER:
                    return ceu_sizeof(CEU_Value, Pointer);
                case CEU_VALUE_P_FUNC:
                case CEU_VALUE_TUPLE:
                case CEU_VALUE_VECTOR:
                case CEU_VALUE_DICT:
                    return ceu_sizeof(CEU_Value, Dyn);
                default:
                    assert(0 && "bug found");
            }
        }
        
        void ceu_max_depth (CEU_Dyn* dyn, int n, CEU_Value* childs) {
            // new dyn should have at least the maximum depth among its children
            CEU_Block* hld = NULL;
            int max = -1;
            for (int i=0; i<n; i++) {
                CEU_Value* cur = &childs[i];
                if (cur->type>CEU_VALUE_DYNAMIC && cur->Dyn->hold.up_block!=NULL) {
                    if (max < ceu_depth(cur->Dyn->hold.up_block)) {
                        max = ceu_depth(cur->Dyn->hold.up_block);
                        hld = cur->Dyn->hold.up_block;
                    }
                }
            }
            if (hld != NULL) {
                ceu_hold_add(hld, dyn);
            }
        }
        
        int ceu_block_chk_set_mutual (CEU_Dyn* src, CEU_Dyn* dst) {
            if (dst->hold.type == CEU_HOLD_FLEETING) {
                if (src->hold.type == CEU_HOLD_FLEETING) {
                    return 1;
                } else {
                    return ceu_block_chk_set(ceu_dyn_to_val(dst), src->hold.up_block, src->hold.type);
                }
            } else {
                return ceu_block_chk_set(ceu_dyn_to_val(src), dst->hold.up_block, dst->hold.type);
            }
        }

        int ceu_tuple_set (CEU_Dyn* tup, int i, CEU_Value v) {
            ceu_gc_inc(&v);
            ceu_gc_dec(&tup->Tuple.buf[i], 1);
            tup->Tuple.buf[i] = v;

            if (v.type > CEU_VALUE_DYNAMIC) {
                if (v.Dyn->hold.type!=CEU_HOLD_FLEETING && ceu_depth(v.Dyn->hold.up_block) > ceu_depth(tup->hold.up_block)) {
                    ceu_hold_rem(tup);
                    ceu_hold_add(v.Dyn->hold.up_block, tup);
                }
            }

            return ((v.type<CEU_VALUE_DYNAMIC) && ceu_block_chk(&v,tup->hold.up_block,tup->hold.type)) || ceu_block_chk_set_mutual(v.Dyn,tup);
        }
        
        CEU_RET ceu_vector_get (CEU_Dyn* vec, int i) {
            if (i<0 || i>=vec->Vector.its) {
                CEU_THROW_MSG("\0 : index error : out of bounds");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            int sz = ceu_tag_to_size(vec->Vector.type);
            ceu_acc = (CEU_Value) { vec->Vector.type };
            memcpy(&ceu_acc.Number, vec->Vector.buf+i*sz, sz);
            return CEU_RET_RETURN;
        }
        
        int ceu_vector_set (CEU_Dyn* vec, int i, CEU_Value v) {
            if (v.type == CEU_VALUE_NIL) {           // pop
                assert(i == vec->Vector.its-1);
                assert(CEU_RET_RETURN == ceu_vector_get(vec, i));
                ceu_gc_dec(&ceu_acc, 1);
                vec->Vector.its--;
                return 1;
            } else {
                if (v.type > CEU_VALUE_DYNAMIC) {
                    if (ceu_depth(v.Dyn->hold.up_block) > ceu_depth(vec->hold.up_block)) {
                        ceu_hold_rem(vec);
                        ceu_hold_add(v.Dyn->hold.up_block, vec);
                    }
                }
                
                if (vec->Vector.its == 0) {
                    vec->Vector.type = v.type;
                } else {
                    assert(v.type == vec->Vector.type);
                }
                int sz = ceu_tag_to_size(vec->Vector.type);
                if (i == vec->Vector.its) {           // push
                    if (i == vec->Vector.max) {
                        vec->Vector.max = vec->Vector.max*2 + 1;    // +1 if max=0
                        vec->Vector.buf = realloc(vec->Vector.buf, vec->Vector.max*sz + 1);
                        assert(vec->Vector.buf != NULL);
                    }
                    ceu_gc_inc(&v);
                    vec->Vector.its++;
                    vec->Vector.buf[sz*vec->Vector.its] = '\0';
                } else {                            // set
                    assert(CEU_RET_RETURN == ceu_vector_get(vec, i));
                    ceu_gc_inc(&v);
                    ceu_gc_dec(&ceu_acc, 1);
                    assert(i < vec->Vector.its);
                }
                memcpy(vec->Vector.buf + i*sz, (char*)&v.Number, sz);
                return ((v.type < CEU_VALUE_DYNAMIC) && ceu_block_chk(&v,vec->hold.up_block,vec->hold.type)) || ceu_block_chk_set_mutual(v.Dyn,vec);
                    //ceu_block_set(v.Dyn, vec->hold.up_block, vec->hold.type);
            }
        }
        
        CEU_Dyn* ceu_vector_from_c_string (CEU_Block* hld, const char* str) {
            CEU_Dyn* vec = ceu_vector_create(hld);
            int N = strlen(str);
            for (int i=0; i<N; i++) {
                assert(CEU_RET_RETURN == ceu_vector_set(vec, vec->Vector.its, (CEU_Value) { CEU_VALUE_CHAR, str[i] }));
            }
            return vec;
        }

        CEU_RET ceu_next_dash_dict_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            CEU_Value NIL = (CEU_Value) { CEU_VALUE_NIL };
            assert(n==1 || n==2);
            CEU_Value* col = args[0];
            CEU_Value* key = (n == 1) ? &NIL : args[1];
            assert(col->type == CEU_VALUE_DICT);
            for (int i=0; i<col->Dyn->Dict.max; i++) {
                CEU_Value* args[] = { key, &(*col->Dyn->Dict.buf)[i][0] };
                assert(CEU_RET_RETURN == ceu_op_equals_equals_f(NULL, 2, args));
                if (ceu_acc.Bool) {
                    key = &NIL;
                } else if (key->type == CEU_VALUE_NIL) {
                    ceu_acc = (*col->Dyn->Dict.buf)[i][0];
                    return CEU_RET_RETURN;
                }
            }
            ceu_acc = NIL;
            return CEU_RET_RETURN;
        }        
        int ceu_dict_key_to_index (CEU_Dyn* col, CEU_Value* key, int* idx) {
            *idx = -1;
            for (int i=0; i<col->Dict.max; i++) {
                CEU_Value* cur = &(*col->Dict.buf)[i][0];
                CEU_Value* args[] = { key, cur };
                assert(CEU_RET_RETURN == ceu_op_equals_equals_f(NULL, 2, args));
                if (ceu_acc.Bool) {
                    *idx = i;
                    return 1;
                } else {
                    if (*idx==-1 && cur->type==CEU_VALUE_NIL) {
                        *idx = i;
                    }
                }
            }
            return 0;
        }        
        CEU_Value ceu_dict_get (CEU_Dyn* col, CEU_Value* key) {
            int i;
            int ok = ceu_dict_key_to_index(col, key, &i);
            if (ok) {
                return (*col->Dict.buf)[i][1];
            } else {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
        }        
        int ceu_dict_set (CEU_Dyn* col, CEU_Value* key, CEU_Value* val) {

            if (key->type > CEU_VALUE_DYNAMIC) {
                if (ceu_depth(key->Dyn->hold.up_block) > ceu_depth(col->hold.up_block)) {
                    ceu_hold_rem(col);
                    ceu_hold_add(key->Dyn->hold.up_block, col);
                }
                assert(ceu_block_chk_set_mutual(key->Dyn, col));
            } else {
                assert(ceu_block_chk_set(&ceu_acc, col->hold.up_block, col->hold.type));
            }
            if (val->type > CEU_VALUE_DYNAMIC) {
                if (ceu_depth(val->Dyn->hold.up_block) > ceu_depth(col->hold.up_block)) {
                    ceu_hold_rem(col);
                    ceu_hold_add(val->Dyn->hold.up_block, col);
                }
            }

            //assert(key->type != CEU_VALUE_NIL);     // TODO
            int old;
            ceu_dict_key_to_index(col, key, &old);
            if (old == -1) {
                old = col->Dict.max;
                int new = MAX(5, old * 2);
                col->Dict.max = new;
                col->Dict.buf = realloc(col->Dict.buf, new*2*sizeof(CEU_Value));
                assert(col->Dict.buf != NULL);
                memset(&(*col->Dict.buf)[old], 0, (new-old)*2*sizeof(CEU_Value));  // x[i]=nil
            }
            assert(old != -1);
            
            CEU_Value vv = ceu_dict_get(col, key);
            
            if (val->type == CEU_VALUE_NIL) {
                ceu_gc_dec(&vv, 1);
                ceu_gc_dec(key, 1);
                (*col->Dict.buf)[old][0] = (CEU_Value) { CEU_VALUE_NIL };
            } else {
                ceu_gc_inc(val);
                ceu_gc_dec(&vv, 1);
                if (vv.type == CEU_VALUE_NIL) {
                    ceu_gc_inc(key);
                }
                int ret1 = ((key->type < CEU_VALUE_DYNAMIC) && ceu_block_chk(key,col->hold.up_block,col->hold.type)) || ceu_block_chk_set_mutual(key->Dyn,col);
                    //ceu_block_set(key->Dyn, col->hold.up_block, col->hold.type);
                int ret2 = ((val->type < CEU_VALUE_DYNAMIC) && ceu_block_chk(val,col->hold.up_block,col->hold.type)) || ceu_block_chk_set_mutual(val->Dyn,col);
                    //ceu_block_set(val->Dyn, col->hold.up_block, col->hold.type);
                if (!(ret1 && ret2)) {
                    return 0;
                }
                (*col->Dict.buf)[old][0] = *key;
                (*col->Dict.buf)[old][1] = *val;
            }
            
            return 1;
        }        
        
        CEU_RET ceu_col_check (CEU_Value* col, CEU_Value* idx) {
            if (col->type<CEU_VALUE_TUPLE || col->type>CEU_VALUE_DICT) {                
                CEU_THROW_MSG("\0 : index error : expected collection");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            if (col->type != CEU_VALUE_DICT) {
                if (idx->type != CEU_VALUE_NUMBER) {
                    CEU_THROW_MSG("\0 : index error : expected number");
                    CEU_THROW_RET(CEU_ERR_ERROR);
                }
                if (col->type==CEU_VALUE_TUPLE && (idx->Number<0 || idx->Number>=col->Dyn->Tuple.its)) {                
                    CEU_THROW_MSG("\0 : index error : out of bounds");
                    CEU_THROW_RET(CEU_ERR_ERROR);
                }
                if (col->type==CEU_VALUE_VECTOR && (idx->Number<0 || idx->Number>col->Dyn->Vector.its)) {                
                    CEU_THROW_MSG("\0 : index error : out of bounds");
                    CEU_THROW_RET(CEU_ERR_ERROR); // accepts v[#v]
                }
            }
            return CEU_RET_RETURN;
        }
    """ +
    """ // CREATES
        CEU_Dyn* ceu_proto_create (int type, CEU_Proto proto, CEU_Block* hld, CEU_HOLD tphold) {
            CEU_Dyn* ret = malloc(sizeof(CEU_Dyn));
            assert(ret != NULL);
            proto.upvs.buf = malloc(proto.upvs.its * sizeof(CEU_Value));
            assert(proto.upvs.buf != NULL);
            for (int i=0; i<proto.upvs.its; i++) {
                proto.upvs.buf[i] = (CEU_Value) { CEU_VALUE_NIL };
            }
            *ret = (CEU_Dyn) {
                type, {NULL,NULL,tphold}, NULL, 0, {
                    .Proto = proto
                }
            };
            //assert(CEU_RET_RETURN == ceu_block_set(ret, hld, tphold));
            ceu_hold_add(hld, ret);
            return ret;
        }
        
        CEU_Dyn* ceu_tuple_create (CEU_Block* hld, int n) {
            CEU_Dyn* ret = malloc(sizeof(CEU_Dyn) + n*sizeof(CEU_Value));
            assert(ret != NULL);
            *ret = (CEU_Dyn) {
                CEU_VALUE_TUPLE, {NULL,NULL,CEU_HOLD_FLEETING}, NULL, 0, {
                    .Tuple = {n,{}}
                }
            };
            memset(ret->Tuple.buf, 0, n*sizeof(CEU_Value));
            ceu_hold_add(hld, ret);
            return ret;
        }
        
        CEU_RET ceu_tuple_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n==1 && args[0]->type==CEU_VALUE_NUMBER);
            CEU_Dyn* tup = ceu_tuple_create(frame->up_block, args[0]->Number);
            ceu_acc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=tup} };
        }
        
        CEU_Dyn* ceu_vector_create (CEU_Block* hld) {
            CEU_Dyn* ret = malloc(sizeof(CEU_Dyn));
            assert(ret != NULL);
            char* buf = malloc(1);  // because of '\0' in empty strings
            assert(buf != NULL);
            buf[0] = '\0';
            *ret = (CEU_Dyn) {
                CEU_VALUE_VECTOR, {NULL,NULL,CEU_HOLD_FLEETING}, NULL, 0, {
                    .Vector = {0,0,CEU_VALUE_NIL,buf}
                }
            };
            ceu_hold_add(hld, ret);
            return ret;
        }
        
        CEU_Dyn* ceu_dict_create (CEU_Block* hld) {
            CEU_Dyn* ret = malloc(sizeof(CEU_Dyn));
            assert(ret != NULL);
            *ret = (CEU_Dyn) {
                CEU_VALUE_DICT, {NULL,NULL,CEU_HOLD_FLEETING}, NULL, 0, {
                    .Dict = {0,NULL}
                }
            };
            ceu_hold_add(hld, ret);
            return ret;
        }
        
    """ +
    """ // PRINT
        void ceu_print1 (CEU_Frame* _1, CEU_Value* v) {
            // no tags when _1==NULL (ceu_error_list_print)
            if (_1!=NULL && v->type>CEU_VALUE_DYNAMIC) {  // TAGS
                CEU_Value* args[1] = { v };
                int ok = ceu_tags_f(_1, 1, args);
                CEU_Value tup = ceu_acc;
                assert(ok == CEU_RET_RETURN);
                int N = tup.Dyn->Tuple.its;
                if (N > 0) {
                    if (N > 1) {
                        printf("[");
                    }
                    for (int i=0; i<N; i++) {
                        ceu_print1(_1, &tup.Dyn->Tuple.buf[i]);
                        if (i < N-1) {
                            printf(",");
                        }
                    }
                    if (N > 1) {
                        printf("]");
                    }
                    printf(" ");
                }
                ceu_hold_rem(tup.Dyn);
                ceu_dyn_free(tup.Dyn);
            }
            switch (v->type) {
                case CEU_VALUE_NIL:
                    printf("nil");
                    break;
                case CEU_VALUE_TAG:
                    printf("%s", ceu_tag_to_string(v->Tag));
                    break;
                case CEU_VALUE_BOOL:
                    if (v->Bool) {
                        printf("true");
                    } else {
                        printf("false");
                    }
                    break;
                case CEU_VALUE_CHAR:
                    putchar(v->Char);
                    break;
                case CEU_VALUE_NUMBER:
                    printf("%g", v->Number);
                    break;
                case CEU_VALUE_POINTER:
                    printf("pointer: %p", v->Pointer);
                    break;
                case CEU_VALUE_TUPLE:
                    printf("[");
                    for (int i=0; i<v->Dyn->Tuple.its; i++) {
                        if (i > 0) {
                            printf(",");
                        }
                        ceu_print1(_1, &v->Dyn->Tuple.buf[i]);
                    }                    
                    printf("]");
                    break;
                case CEU_VALUE_VECTOR:
                    if (v->Dyn->Vector.type == CEU_VALUE_CHAR) {
                        printf("%s", v->Dyn->Vector.buf);
                    } else {
                        printf("#[");
                        for (int i=0; i<v->Dyn->Vector.its; i++) {
                            if (i > 0) {
                                printf(",");
                            }
                            assert(CEU_RET_RETURN == ceu_vector_get(v->Dyn, i));
                            CEU_Value ceu_accx = ceu_acc;
                            ceu_print1(_1, &ceu_accx);
                        }                    
                        printf("]");
                    }
                    break;
                case CEU_VALUE_DICT:
                    printf("@[");
                    int comma = 0;
                    for (int i=0; i<v->Dyn->Dict.max; i++) {
                        if ((*v->Dyn->Dict.buf)[i][0].type != CEU_VALUE_NIL) {
                            if (comma != 0) {
                                printf(",");
                            }
                            comma = 1;
                            printf("(");
                            ceu_print1(_1, &(*v->Dyn->Dict.buf)[i][0]);
                            printf(",");
                            ceu_print1(_1, &(*v->Dyn->Dict.buf)[i][1]);
                            printf(")");
                        }
                    }                    
                    printf("]");
                    break;
                case CEU_VALUE_P_FUNC:
                    printf("func: %p", v->Dyn);
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        CEU_RET ceu_print_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            for (int i=0; i<n; i++) {
                if (i > 0) {
                    printf("\t");
                }
                ceu_print1(_1, args[i]);
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_println_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(CEU_RET_RETURN == ceu_print_f(_1, n, args));
            printf("\n");
            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
            return CEU_RET_RETURN;
        }
    """ +
    """
        // EQ / NEQ / LEN / COROS / DROP / COPY / THROW / TRACK
        CEU_RET ceu_op_equals_equals_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(n == 2);
            CEU_Value* e1 = args[0];
            CEU_Value* e2 = args[1];
            int v = (e1->type == e2->type);
            if (v) {
                switch (e1->type) {
                    case CEU_VALUE_NIL:
                        v = 1;
                        break;
                    case CEU_VALUE_TAG:
                        v = (e1->Tag == e2->Tag);
                        break;
                    case CEU_VALUE_BOOL:
                        v = (e1->Bool == e2->Bool);
                        break;
                    case CEU_VALUE_CHAR:
                        v = (e1->Char == e2->Char);
                        break;
                    case CEU_VALUE_NUMBER:
                        v = (e1->Number == e2->Number);
                        break;
                    case CEU_VALUE_POINTER:
                        v = (e1->Pointer == e2->Pointer);
                        break;
                    case CEU_VALUE_TUPLE:
                    case CEU_VALUE_VECTOR:
                    case CEU_VALUE_DICT:
                    case CEU_VALUE_P_FUNC:
                        v = (e1->Dyn == e2->Dyn);
                        break;
                    default:
                        assert(0 && "bug found");
                }
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=v} };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_op_slash_equals_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(CEU_RET_RETURN == ceu_op_equals_equals_f(_1, n, args));
            ceu_acc.Bool = !ceu_acc.Bool;
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_op_hash_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(n == 1);
            if (args[0]->type == CEU_VALUE_VECTOR) {
                ceu_acc = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=args[0]->Dyn->Vector.its} };
            } else if (args[0]->type == CEU_VALUE_TUPLE) {
                ceu_acc = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=args[0]->Dyn->Tuple.its} };
            } else {
                CEU_THROW_MSG("\0 : length error : not a vector");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_drop_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* src = args[0];
            CEU_Dyn* dyn = src->Dyn;
            
            // do not drop non-dyn or globals
            if (src->type < CEU_VALUE_DYNAMIC) {
                return CEU_RET_RETURN;
            } else if (ceu_depth(dyn->hold.up_block) == 1) {
                return CEU_RET_RETURN;
            }
            
            //printf(">>> %d\n", dyn->refs);
            if (dyn->hold.type >= CEU_HOLD_IMMUTABLE) {
                CEU_THROW_MSG("\0 : drop error : value is not movable");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            if (dyn->refs > 1) {
                CEU_THROW_MSG("\0 : drop error : multiple references");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            dyn->hold.type = CEU_HOLD_FLEETING;
            ceu_hold_rem(dyn);
            ceu_hold_add(frame->up_block, dyn);

            switch (src->type) {
                case CEU_VALUE_P_FUNC:
                    for (int i=0; i<dyn->Proto.upvs.its; i++) {
                        CEU_Value* args[1] = { &dyn->Proto.upvs.buf[i] };
                        int ret = ceu_drop_f(frame, 1, args);
                        if (ret != CEU_RET_RETURN) {
                            return ret;
                        }
                    }
                    break;
                case CEU_VALUE_TUPLE: {
                    for (int i=0; i<dyn->Tuple.its; i++) {
                        CEU_Value* args[1] = { &dyn->Tuple.buf[i] };
                        //assert(CEU_RET_RETURN == ceu_drop_f(frame, 1, args));
                        int ret = ceu_drop_f(frame, 1, args);
                        if (ret != CEU_RET_RETURN) {
                            return ret;
                        }
                    }
                    break;
                }
                case CEU_VALUE_VECTOR: {
                    for (int i=0; i<dyn->Vector.its; i++) {
                        assert(CEU_RET_RETURN == ceu_vector_get(dyn, i));
                        CEU_Value ceu_accx = ceu_acc;
                        CEU_Value* args[1] = { &ceu_accx };
                        //assert(CEU_RET_RETURN == ceu_drop_f(frame, 1, args));
                        int ret = ceu_drop_f(frame, 1, args);
                        if (ret != CEU_RET_RETURN) {
                            return ret;
                        }
                    }
                    break;
                }
                case CEU_VALUE_DICT: {
                    for (int i=0; i<dyn->Dict.max; i++) {
                        CEU_Value* args0[1] = { &(*dyn->Dict.buf)[i][0] };
                        //assert(CEU_RET_RETURN == ceu_drop_f(frame, 1, args0));
                        int ret0 = ceu_drop_f(frame, 1, args0);
                        if (ret0 != CEU_RET_RETURN) {
                            return ret0;
                        }
                        CEU_Value* args1[1] = { &(*dyn->Dict.buf)[i][1] };
                        //assert(CEU_RET_RETURN == ceu_drop_f(frame, 1, args1));
                        int ret1 = ceu_drop_f(frame, 1, args1);
                        if (ret1 != CEU_RET_RETURN) {
                            return ret1;
                        }
                    }
                    break;
                }
                default:
                    break;
            }
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_copy_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* src = args[0];
            CEU_Dyn* old = src->Dyn;
            switch (src->type) {
                case CEU_VALUE_TUPLE: {
                    CEU_Dyn* new = ceu_tuple_create(frame->up_block, old->Tuple.its);
                    assert(new != NULL);
                    new->hold.type = CEU_HOLD_FLEETING;
                    for (int i=0; i<old->Tuple.its; i++) {
                        CEU_Value* args[1] = { &old->Tuple.buf[i] };
                        assert(CEU_RET_RETURN == ceu_copy_f(frame, 1, args));
                        assert(ceu_tuple_set(new, i, ceu_acc));
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=new} };
                    break;
                }
                case CEU_VALUE_VECTOR: {
                    CEU_Dyn* new = ceu_vector_create(frame->up_block);
                    assert(new != NULL);
                    new->hold.type = CEU_HOLD_FLEETING;
                    for (int i=0; i<old->Vector.its; i++) {
                        assert(CEU_RET_RETURN == ceu_vector_get(old, i));
                        CEU_Value ceu_accx = ceu_acc;
                        CEU_Value* args[1] = { &ceu_accx };
                        assert(CEU_RET_RETURN == ceu_copy_f(frame, 1, args));
                        assert(CEU_RET_RETURN == ceu_vector_set(new, i, ceu_acc));
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=new} };
                    break;
                }
                case CEU_VALUE_DICT: {
                    CEU_Dyn* new = ceu_dict_create(frame->up_block);
                    assert(new != NULL);
                    new->hold.type = CEU_HOLD_FLEETING;
                    for (int i=0; i<old->Dict.max; i++) {
                        {
                            CEU_Value* args[1] = { &(*old->Dict.buf)[i][0] };
                            assert(CEU_RET_RETURN == ceu_copy_f(frame, 1, args));
                        }
                        CEU_Value key = ceu_acc;
                        if (key.type == CEU_VALUE_NIL) {
                            continue;
                        }
                        {
                            CEU_Value* args[1] = { &(*old->Dict.buf)[i][1] };
                            assert(CEU_RET_RETURN == ceu_copy_f(frame, 1, args));
                        }
                        CEU_Value val = ceu_acc;
                        ceu_dict_set(new, &key, &val);
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_DICT, {.Dyn=new} };
                    break;
                }
                case CEU_VALUE_P_FUNC:
                    assert(0 && "TODO: not supported");
                default:
                    ceu_acc = *src;
                    break;
            }
            if (src->type > CEU_VALUE_DYNAMIC) {
                CEU_Tags_List* cur = src->Dyn->tags;
                CEU_Value new = ceu_acc;
                while (cur != NULL) {
                    CEU_Value tag = (CEU_Value) { CEU_VALUE_TAG,  {.Tag=cur->tag} };
                    CEU_Value tru = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
                    CEU_Value* args[] = { &new, &tag, &tru };
                    assert(CEU_RET_RETURN == ceu_tags_f(frame, 3, args));
                    cur = cur->next;
                }
                ceu_acc = new;
            }
            return CEU_RET_RETURN;
        }

        CEU_RET ceu_throw_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_THROW_MSG("throw error : uncaught exception");
            ceu_acc = *args[0];
            ceu_gc_inc(&ceu_acc);
            return CEU_RET_THROW;
        }

    """ +
    """ // FUNCS
        typedef struct {
            ${GLOBALS.map { "CEU_Value ${it.id2c(null)};\n" }.joinToString("")}
            ${mem.expr(outer).second}
        } CEU_Proto_Mem_${this.outer.n};
        CEU_Proto_Mem_${this.outer.n} _ceu_mem_;
        CEU_Proto_Mem_${this.outer.n}* ceu_mem = &_ceu_mem_;
        CEU_Proto_Mem_${this.outer.n}* ceu_mem_${this.outer.n} = &_ceu_mem_;
        //CEU_Proto _ceu_proto_ = { NULL, {}, {} };
        CEU_Frame _ceu_frame_ = { NULL, NULL, (char*) &_ceu_mem_ };
        CEU_Frame* ceu_frame = &_ceu_frame_;
        ${tops.first.joinToString("")}
        ${tops.second.joinToString("")}
        ${tops.third.joinToString("")}
    """ +
    """ // MAIN
        int main (int ceu_argc, char** ceu_argv) {
            assert(CEU_TAG_nil == CEU_VALUE_NIL);
            do {
                {
                    static CEU_Dyn ceu_copy = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_copy_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_next_dash_dict = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_next_dash_dict_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_print = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_print_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_println = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_println_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_sup_question_ = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_sup_question__f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_tags = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_tags_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_throw = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_throw_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_tuple = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_tuple_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_type = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_type_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_op_equals_equals = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_op_equals_equals_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_op_hash = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_op_hash_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_op_slash_equals = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_op_slash_equals_f, {0,NULL} }
                        }
                    };
                    static CEU_Dyn ceu_string_dash_to_dash_tag = { 
                        CEU_VALUE_P_FUNC, {NULL,NULL,CEU_HOLD_MUTABLE}, NULL, 1, {
                            .Proto = { NULL, ceu_string_dash_to_dash_tag_f, {0,NULL} }
                        }
                    };

                    ceu_mem->copy       = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_copy}         };
                    ceu_mem->next_dash_dict = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_next_dash_dict}         };
                    ceu_mem->print      = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_print}        };
                    ceu_mem->println    = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_println}      };            
                    ceu_mem->tags       = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_tags}         };
                    ceu_mem->throw      = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_throw}        };
                    ceu_mem->type       = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_type}         };
                    ceu_mem->tuple      = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_tuple}         };
                    ceu_mem->op_hash    = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_op_hash}      };
                    ceu_mem->sup_question_    = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_sup_question_}     };
                    ceu_mem->op_equals_equals = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_op_equals_equals} };
                    ceu_mem->op_slash_equals  = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_op_slash_equals}  };
                    ceu_mem->string_dash_to_dash_tag = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_string_dash_to_dash_tag}  };
                }
                ${this.code}
                return 0;
            } while (0);
            return 1;
        }
    """)
}
