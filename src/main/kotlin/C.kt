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
        struct CEU_Dyns;
        struct CEU_BStack;

        typedef enum {
            CEU_RET_THROW = 0,  // going up with throw
            CEU_RET_RETURN,
            CEU_RET_YIELD
        } CEU_RET;

        typedef enum {
            CEU_HOLD_NON = 0,   // not assigned, dst assigns
            CEU_HOLD_VAR,       // set and assignable to narrow 
            CEU_HOLD_FIX,       // set but not assignable across unsafe (even if same/narrow)
            CEU_HOLD_PUB,
            CEU_HOLD_EVT,
            CEU_HOLD_MAX
        } CEU_HOLD;

        typedef enum {
            CEU_ARG_ERR = -2,
            CEU_ARG_EVT = -1,
            CEU_ARG_ARGS = 0    // 0,1,...
        } CEU_ARG;

        CEU_RET ceu_type_f (struct CEU_Frame* _1, struct CEU_BStack* _2, int n, struct CEU_Value* args[]);
        int ceu_as_bool (struct CEU_Value* v);
        
        #define CEU_ISGLBDYN(dyn) (dyn->up_dyns.dyns==NULL || dyn->up_dyns.dyns->up_block==ceu_block_global)

        #define CEU_TYPE_NCAST(v) (v>CEU_VALUE_DYNAMIC && v<CEU_VALUE_BCAST)
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
        #define CEU_CONTINUE_ON_CLEAR() { if (ceu_n==-1 && ceu_evt==&CEU_EVT_CLEAR) { continue; } }
        #define CEU_CONTINUE_ON_CLEAR_THROW() { CEU_CONTINUE_ON_CLEAR(); CEU_CONTINUE_ON_THROW(); }

        CEU_RET ceu_tags_f (struct CEU_Frame* _1, struct CEU_BStack* _2, int n, struct CEU_Value* args[]);
        char* ceu_tag_to_string (int tag);
        int ceu_string_dash_to_dash_tag (char* str);
        
        void ceu_dyn_free (struct CEU_Dyn* dyn);
        void ceu_dyns_free (struct CEU_Dyns* dyns);
        void ceu_block_free (struct CEU_Block* block);
        
        void ceu_gc_inc (struct CEU_Value* v);
        void ceu_gc_dec (struct CEU_Value* v, int chk);

        void ceu_hold_add (struct CEU_Dyns* dyns, struct CEU_Dyn* dyn);
        void ceu_hold_rem (struct CEU_Dyn* dyn);
        CEU_RET ceu_block_set (struct CEU_Dyn* src, struct CEU_Dyns* dst_dyns, CEU_HOLD dst_tphold);
        
        void ceu_tasks_create (struct CEU_Dyns* hld, int max, struct CEU_Value* ret); 
        CEU_RET ceu_x_create (struct CEU_Dyns* hld, struct CEU_Value* task, struct CEU_Value* ret);
        CEU_RET ceu_x_create_in (struct CEU_Dyn* tasks, struct CEU_Value* task, struct CEU_Value* ret, int* ok);
        struct CEU_Dyn* ceu_vector_create (struct CEU_Dyns* hld);
        struct CEU_Dyn* ceu_tuple_create (struct CEU_Dyns* hld, int n);
        
        void ceu_bstack_clear (struct CEU_BStack* bstack, struct CEU_Block* block);
        CEU_RET ceu_bcast_dyns   (struct CEU_BStack* bstack, struct CEU_Dyns* dyns, struct CEU_Value* evt);
        CEU_RET ceu_bcast_blocks (struct CEU_BStack* bstack, struct CEU_Block* cur, struct CEU_Value* evt, int* killed);
        CEU_RET ceu_bcast_dyn    (struct CEU_BStack* bstack, struct CEU_Dyn* cur, struct CEU_Value* evt);
        
        int ceu_tag_to_size (int type);
        void ceu_max_depth (struct CEU_Dyn* dyn, int n, struct CEU_Value* childs);
        CEU_RET ceu_vector_get (struct CEU_Dyn* vec, int i);
        CEU_RET ceu_vector_set (struct CEU_Dyn* vec, int i, struct CEU_Value v);
        struct CEU_Dyn* ceu_vector_from_c_string (struct CEU_Dyns* hld, const char* str);
        
        int ceu_dict_key_to_index (struct CEU_Dyn* col, struct CEU_Value* key, int* idx);
        struct CEU_Value ceu_dict_get (struct CEU_Dyn* col, struct CEU_Value* key);
        CEU_RET ceu_dict_set (struct CEU_Dyn* col, struct CEU_Value* key, struct CEU_Value* val);
        CEU_RET ceu_col_check (struct CEU_Value* col, struct CEU_Value* idx);
        
        CEU_RET ceu_tuple_set (struct CEU_Dyn* tup, int i, struct CEU_Value v);

        void ceu_track_create (struct CEU_Dyns* hld, struct CEU_Dyn* x, struct CEU_Value* ret);
        
        void ceu_print1 (struct CEU_Frame* _1, struct CEU_Value* v);
        CEU_RET ceu_op_equals_equals_f (struct CEU_Frame* _1, struct CEU_BStack* _2, int n, struct CEU_Value* args[]);
    """ +
    """ // CEU_Value
        typedef enum CEU_VALUE {
            CEU_VALUE_NIL = 0,
            CEU_VALUE_TAG,
            CEU_VALUE_BOOL,
            CEU_VALUE_CHAR,
            CEU_VALUE_NUMBER,
            CEU_VALUE_POINTER,
            CEU_VALUE_DYNAMIC,  // all below are dynamic
            CEU_VALUE_P_FUNC,     // prototypes func, coro, task
            CEU_VALUE_P_CORO,
            CEU_VALUE_P_TASK,
            CEU_VALUE_TUPLE,
            CEU_VALUE_VECTOR,
            CEU_VALUE_DICT,
            CEU_VALUE_BCAST,    // all below are bcast
            CEU_VALUE_X_CORO,   // spawned coro, task, tasks
            CEU_VALUE_X_TASK,
            CEU_VALUE_X_TASKS,
            CEU_VALUE_X_TRACK
        } CEU_VALUE;
        
        typedef enum CEU_X_STATUS {
            CEU_X_STATUS_YIELDED = 1,
            CEU_X_STATUS_TOGGLED,
            CEU_X_STATUS_RESUMED,
            CEU_X_STATUS_TERMINATED
        } CEU_X_STATUS;        

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
            struct CEU_BStack* bstack,
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
            union {
                struct {
                    int n_mem;      // sizeof mem
                } X;
            };
        } CEU_Proto;
        
        typedef struct CEU_Frame {          // call func / create task
            CEU_Proto* proto;
            struct CEU_Block* up_block;     // block enclosing this call/coroutine
            char* mem;
            union {
                struct {
                    struct CEU_Dyn* x;      // coro,task/frame point to each other
                    int pc;                 // next line to execute
                    CEU_Value pub;          // public value
                } X;
            };
        } CEU_Frame;
    """ +
    """ // CEU_Dyn
        typedef struct CEU_Dyns {
            int max;
            int its;
            struct CEU_Dyn** buf;
            struct CEU_Block* up_block;
        } CEU_Dyns;
        
        typedef struct CEU_Dyns_I {
            struct CEU_Dyns* dyns;  // (block or tasks) w/ up_block to block
            int i;                  // position in block/tasks vector
        } CEU_Dyns_I;

        typedef struct CEU_BStack {
            struct CEU_Block* block;        // block enclosing bcast, if null, block terminated, traversed the stack and reset it
            struct CEU_BStack* prev;        // pending previous bcast
        } CEU_BStack;

        typedef struct CEU_Dyn {
            CEU_VALUE type;                 // required to switch over free/bcast
            CEU_Dyns_I up_dyns;
            struct CEU_Tags_List* tags;     // linked list of tags
            CEU_HOLD tphold;                // if up_hold is permanent and may not be reset to outer block
            union {
                struct {
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
                } Ncast;
                struct {
                    enum CEU_X_STATUS status;
                    union {
                        struct {
                            struct CEU_Dyn* up_tasks;   // auto terminate / remove from tasks
                            struct CEU_Block* dn_block; // first block to bcast
                            CEU_Frame* frame;
                        } X;
                        struct {
                            int max;
                            CEU_Dyns dyns;
                        } Tasks;
                        struct CEU_Dyn* Track;  // starts as CORO and may fall to NIL
                    };
                } Bcast;
            };
        } CEU_Dyn;
    """ +
    """ // CEU_Block
        typedef struct CEU_Block {
            int depth;                  // compare on set
            int ispub;                  // is top block inside task?
            struct CEU_Dyn* up_x;    // enclosing active coro
            struct CEU_Dyns dn_dyns;    // list of allocated data to bcast/free
            struct CEU_Block* dn_block; // nested block active
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
        struct CEU_Dyn* ceu_proto_create (int type, struct CEU_Proto proto, struct CEU_Dyns* hld, CEU_HOLD tphold);
        struct CEU_Dyn* ceu_dict_create  (struct CEU_Dyns* hld);

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
        
        CEU_Value CEU_EVT_NIL = { CEU_VALUE_NIL }; 
        CEU_Value CEU_EVT_CLEAR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_clear} };
        
        CEU_RET ceu_ret = CEU_RET_RETURN;
        CEU_Value ceu_acc;
        
        CEU_BStack* ceu_bstack = NULL;
        CEU_Dyn* ceu_bcast_tofree = NULL;

        // TODO: remove (only here b/c we do not test CEU_CONTINUE_ON_CLEAR at compile time)
        const int ceu_n = 0;
        const CEU_Value* ceu_evt = &CEU_EVT_NIL;    // also b/c of `evt` outside task
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
        
        int ceu_as_bool (CEU_Value* v) {
            return !(v->type==CEU_VALUE_NIL || (v->type==CEU_VALUE_BOOL && !v->Bool));
        }
        CEU_RET ceu_type_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1 && "bug found");
            ceu_acc = (CEU_Value) { CEU_VALUE_TAG, {.Tag=args[0]->type} };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_sup_question__f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
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
        CEU_RET ceu_tags_f (CEU_Frame* frame, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n >= 1);
            CEU_Value* dyn = args[0];
            CEU_Value* tag = NULL;
            if (n >= 2) {
                tag = args[1];
                assert(tag->type == CEU_VALUE_TAG);
            }
            switch (n) {
                case 1: {
                    int len = 0; {
                        CEU_Tags_List* cur = dyn->Dyn->tags;
                        while (cur != NULL) {
                            len++;
                            cur = cur->next;
                        }
                    }
                    CEU_Dyn* tup = ceu_tuple_create(&frame->up_block->dn_dyns, len);
                    {
                        CEU_Tags_List* cur = dyn->Dyn->tags;
                        int i = 0;
                        while (cur != NULL) {
                            assert(CEU_RET_RETURN == ceu_tuple_set(tup, i++, (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} }));
                            cur = cur->next;
                        }
                    }                    
                    ceu_acc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=tup} };
                    break;
                }
                case 2: {   // check
                    ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} };
                    if (dyn->type < CEU_VALUE_DYNAMIC) {
                        // no tags
                    } else {
                        CEU_Tags_List* cur = dyn->Dyn->tags;
                        while (cur != NULL) {
                            CEU_Value x = (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} };
                            CEU_Value* args[] = { tag, &x };
                            assert(CEU_RET_RETURN == ceu_sup_question__f(frame, _2, 2, args));
                            if (ceu_acc.Bool) {
                                break;
                            }
                            cur = cur->next;
                        }
                    }
                    break;
                }
                case 3: {   // add/rem
                    assert(dyn->type > CEU_VALUE_DYNAMIC);
                    CEU_Value* bool = args[2];
                    assert(bool->type == CEU_VALUE_BOOL);
                    if (bool->Bool) {   // add
                        ceu_tags_f(frame, _2, 2, args);
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
        CEU_RET ceu_string_dash_to_dash_tag_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* str = args[0];
            assert(str->type==CEU_VALUE_VECTOR && str->Dyn->Ncast.Vector.type==CEU_VALUE_CHAR);
            CEU_Tags_Names* cur = CEU_TAGS;
            while (cur != NULL) {
                if (!strcmp(cur->name,str->Dyn->Ncast.Vector.buf)) {
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
                case CEU_VALUE_P_CORO:
                case CEU_VALUE_P_TASK:
                    for (int i=0; i<dyn->Ncast.Proto.upvs.its; i++) {
                        ceu_gc_dec(&dyn->Ncast.Proto.upvs.buf[i], 1);
                    }
                    break;
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<dyn->Ncast.Tuple.its; i++) {
                        ceu_gc_dec(&dyn->Ncast.Tuple.buf[i], 1);
                    }
                    break;
                case CEU_VALUE_VECTOR:
                    for (int i=0; i<dyn->Ncast.Vector.its; i++) {
                        CEU_Value xacc = ceu_acc;
                        assert(CEU_RET_RETURN == ceu_vector_get(dyn, i));
                        ceu_gc_dec(&ceu_acc, 1);
                        ceu_acc = xacc;
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<dyn->Ncast.Dict.max; i++) {
                        ceu_gc_dec(&(*dyn->Ncast.Dict.buf)[i][0], 1);
                        ceu_gc_dec(&(*dyn->Ncast.Dict.buf)[i][1], 1);
                    }
                    break;
                case CEU_VALUE_X_CORO:
                case CEU_VALUE_X_TASKS:
                case CEU_VALUE_X_TRACK:
                    // TODO: currently not gc'ed
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
            if (dyn->Ncast.refs == 0) {
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
            if (!CEU_TYPE_NCAST(new->type)) {
                return;
            }
            new->Dyn->Ncast.refs++;
        }
        
        void ceu_gc_dec (struct CEU_Value* old, int chk) {
            if (!CEU_TYPE_NCAST(old->type)) {
                return;
            }
            old->Dyn->Ncast.refs--;
            if (chk) {
                ceu_gc_chk(old->Dyn);
            }
        }
    """ +
    """ // BLOCK
        int ceu_hold_hole (CEU_Dyns* dyns) {
            // TODO: should check if dyns is currently being traversed,
            //       in which case should return that there's no hole,
            //       otherwise new dyn might be allocated and traversed
            //       right away
            for (int i=0; i<dyns->max; i++) {
                if (dyns->buf[i] == NULL) {
                    return i;
                }
            }
            return -1;
        }
        
        void ceu_hold_add (CEU_Dyns* dyns, CEU_Dyn* dyn) {
            int I = -1;
            if (dyns->max > dyns->its) {            // end of list
                I = dyns->its++;
            } else {
                I = ceu_hold_hole(dyns);
                if (I == -1) {                      // grow list
                    dyns->max++; // = 1 + dyns->max*2;
                    dyns->buf = realloc(dyns->buf, dyns->max * sizeof(CEU_Dyns*));
                    assert(dyns->buf != NULL);
                    I = dyns->its++;
                }
            }
            assert(I >= 0);
            dyns->buf[I] = dyn;
            dyn->up_dyns = (CEU_Dyns_I) { dyns, I };
        }
        void ceu_hold_rem (CEU_Dyn* dyn) {
            CEU_Dyns_I* up = &dyn->up_dyns;
            assert(up->dyns != NULL);
            assert(up->dyns->buf != NULL);
            up->dyns->buf[up->i] = NULL;
            up->dyns = NULL;
        }

        void ceu_dyn_free (CEU_Dyn* dyn) {
            while (dyn->tags != NULL) {
                CEU_Tags_List* tag = dyn->tags;
                dyn->tags = tag->next;
                free(tag);
            }
            switch (dyn->type) {
                case CEU_VALUE_P_FUNC:
                case CEU_VALUE_P_CORO:
                case CEU_VALUE_P_TASK:
                    free(dyn->Ncast.Proto.upvs.buf);
                    break;
                case CEU_VALUE_TUPLE:       // buf w/ dyn
                case CEU_VALUE_X_TRACK:
                    break;
                case CEU_VALUE_VECTOR:
                    free(dyn->Ncast.Vector.buf);
                    break;
                case CEU_VALUE_DICT:
                    free(dyn->Ncast.Dict.buf);
                    break;
                case CEU_VALUE_X_CORO:
                case CEU_VALUE_X_TASK:
                    if (dyn->Bcast.X.dn_block != NULL) {
                        ceu_block_free(dyn->Bcast.X.dn_block);
                    }
                    free(dyn->Bcast.X.frame->mem);
                    free(dyn->Bcast.X.frame);
                    break;
                case CEU_VALUE_X_TASKS:
                    ceu_dyns_free(&dyn->Bcast.Tasks.dyns);
                    break;
                default:
                    assert(0 && "bug found");
            }
            free(dyn);
        }
        
        void ceu_dyns_free (CEU_Dyns* dyns) {
            for (int i=0; i<dyns->its; i++) {
                if (dyns->buf[i] != NULL) {
                    ceu_dyn_free(dyns->buf[i]);
                }
            }
            {
                free(dyns->buf);
                dyns->max = dyns->its = 0;
                dyns->buf = NULL;
            }
        }
        
        void ceu_block_free (CEU_Block* block) {
            ceu_dyns_free(&block->dn_dyns);
            if (block->dn_block != NULL) {
                ceu_block_free(block->dn_block);
                block->dn_block = NULL;
            }
        }

        int ceu_block_chk (CEU_Dyns* dst, CEU_Dyn* src) {
            if (src->tphold==CEU_HOLD_NON || src->tphold==CEU_HOLD_EVT) {
                return 1;
            } else if (CEU_ISGLBDYN(src)) {
                return 1;
            } else if (dst == src->up_dyns.dyns) {          // same block
                return 1;
            } else if (src->up_dyns.dyns==NULL || dst->up_block->depth >= src->up_dyns.dyns->up_block->depth) {
                return 1;
            } else {
                //printf(">>> dst=%d >= src=%d\n", dst->up_block->depth, src->up_dyns.dyns->up_block->depth);
                return 0;
            }
        }
        
        int ceu_block_hld (CEU_HOLD dst, CEU_HOLD src) {
            static const int x[CEU_HOLD_MAX][CEU_HOLD_MAX] = {
                { 1, 1, 1, 1, 1 },     // src = NON
                { 1, 1, 1, 1, 0 },     // src = VAR
                { 1, 1, 0, 1, 0 },     // src = FIX
                { 1, 0, 9, 1, 9 },     // src = PUB
                { 1, 0, 9, 9, 1 }      // src = EVT
            };
            //printf(">>> src=%d dst=%d = %d\n", src, dst, x[src][dst]);
            assert(x[src][dst] != 9);
            return x[src][dst] == 1;
        }

        void ceu_block_rec (CEU_Dyns* dst, CEU_Dyn* src, CEU_HOLD tphold) {
            if (CEU_ISGLBDYN(src)) {
                return;
            }
            src->tphold = MAX(src->tphold,tphold);
            if (dst == NULL) {
                // caller: do not set block (only tphold)
            } else {
                if (src->up_dyns.dyns==NULL || dst->up_block->depth < src->up_dyns.dyns->up_block->depth) {
                    if (src->up_dyns.dyns != NULL) {
                        ceu_hold_rem(src);
                    }
                    ceu_hold_add(dst, src);
                }
            }
            switch (src->type) {
                case CEU_VALUE_P_FUNC:
                case CEU_VALUE_P_CORO:
                case CEU_VALUE_P_TASK:
                    for (int i=0; i<src->Ncast.Proto.upvs.its; i++) {
                        if (src->Ncast.Proto.upvs.buf[i].type > CEU_VALUE_DYNAMIC) {
                            ceu_block_rec(dst, src->Ncast.Proto.upvs.buf[i].Dyn, tphold);
                        }
                    }
                    break;
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<src->Ncast.Tuple.its; i++) {
                        if (src->Ncast.Tuple.buf[i].type > CEU_VALUE_DYNAMIC) {
                            ceu_block_rec(dst, src->Ncast.Tuple.buf[i].Dyn, tphold);
                        }
                    }
                    break;
                case CEU_VALUE_VECTOR:
                    if (src->Ncast.Vector.type > CEU_VALUE_DYNAMIC) {
                        int sz = ceu_tag_to_size(src->Ncast.Vector.type);
                        for (int i=0; i<src->Ncast.Vector.its; i++) {
                            ceu_block_rec(dst, *(CEU_Dyn**)(src->Ncast.Vector.buf + i*sz), tphold);
                        }
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<src->Ncast.Dict.max; i++) {
                        if ((*src->Ncast.Dict.buf)[i][0].type > CEU_VALUE_DYNAMIC) {
                            ceu_block_rec(dst, (*src->Ncast.Dict.buf)[i][0].Dyn, tphold);
                        }
                        if ((*src->Ncast.Dict.buf)[i][1].type > CEU_VALUE_DYNAMIC) {
                            ceu_block_rec(dst, (*src->Ncast.Dict.buf)[i][1].Dyn, tphold);
                        }
                    }
                    break;
            }
        }
        
        CEU_RET ceu_block_set (CEU_Dyn* src, CEU_Dyns* dst_dyns, CEU_HOLD dst_tphold) {
            //assert(dst_dyns != NULL);     // x :tmp [0] = []
            // dst might be NULL when assigning to orphan tuple
            //printf("> %d %d\n", ceu_block_chk(dst_dyns,src), ceu_block_hld(dst_tphold,src->tphold));
            if ((dst_dyns==NULL || ceu_block_chk(dst_dyns,src)) && ceu_block_hld(dst_tphold,src->tphold)) {
                ceu_block_rec(dst_dyns, src, dst_tphold);
            } else {
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            return CEU_RET_RETURN;            
        }
    """ +
    """ // BCAST - TRAVERSE
        void ceu_bstack_clear (CEU_BStack* bstack, CEU_Block* block) {
            CEU_BStack* cur = bstack;
            while (cur != NULL) {
                if (cur->block == block) {
                    cur->block = NULL;
                }
                cur = cur->prev;
            }
            if (block->dn_block != NULL) {
                ceu_bstack_clear(bstack, block->dn_block);
            }
        }

        #define SPC_EQU(s) { for (int i=0; i<SPC; i++) putchar(' '); s; }
        #define SPC_INC(s) { SPC+=2; for (int i=0; i<SPC; i++) putchar(' '); s; }
        #define SPC_DEC(s) { for (int i=0; i<SPC; i++) putchar(' '); SPC-=2; s; }
        static int SPC = 0;
        
        CEU_RET ceu_bcast_blocks (CEU_BStack* bstack, CEU_Block* cur, CEU_Value* evt, int* killed) {
//SPC_INC(printf(">>> ceu_bcast_blocks = %p\n", cur));
            while (cur != NULL) {
//SPC_INC(printf(">>> ceu_bcast_block = %p\n", cur));
                CEU_BStack xbstack = { cur, bstack };
                int ret = ceu_bcast_dyns(&xbstack, &cur->dn_dyns, evt);
                if (xbstack.block == NULL) {
//SPC_DEC(printf("<<< ceu_bcast_block = %p\n", cur));
//SPC_DEC(printf("<<< ceu_bcast_blocks = %p\n", cur));
                    if (killed != NULL) {
                        *killed = 1;
                    }
                    return ret;
                }
                if (ret == CEU_RET_THROW) {
//SPC_DEC(printf("<<< ceu_bcast_block = %p\n", cur));
//SPC_DEC(printf("<<< ceu_bcast_blocks = %p\n", cur));
                    return CEU_RET_THROW;
                }
//SPC_DEC(printf("<<< ceu_bcast_block = %p\n", cur));
                cur = cur->dn_block;
            }
//SPC_DEC(printf("<<< ceu_bcast_blocks = %p\n", cur));
            return CEU_RET_RETURN;
        }
 
        CEU_RET ceu_bcast_dyn (CEU_BStack* bstack, CEU_Dyn* cur, CEU_Value* evt) {
//SPC_INC(printf(">>> ceu_bcast_dyn = %p\n", cur));
            if (cur->Bcast.status == CEU_X_STATUS_TERMINATED) {
//SPC_DEC(printf("<a< ceu_bcast_dyn = %p\n", cur));
                return CEU_RET_RETURN;
            }
            if (cur->Bcast.status==CEU_X_STATUS_TOGGLED && evt!=&CEU_EVT_CLEAR) {
                // do not awake toggled coro, unless it is a CLEAR event
//SPC_DEC(printf("<b< ceu_bcast_dyn = %p\n", cur));
                return CEU_RET_RETURN;
            }
            switch (cur->type) {
                case CEU_VALUE_X_CORO:
                case CEU_VALUE_X_TASK: {
                    if (evt!=&CEU_EVT_CLEAR && cur->type!=CEU_VALUE_X_TASK) {
//SPC_DEC(printf("<c< ceu_bcast_dyn = %p\n", cur));
                        return CEU_RET_RETURN;
                    } else {
                        // step (1)
                        CEU_BStack xbstack = { cur->up_dyns.dyns->up_block, bstack };
                        int killed = 0;
                        int ret = ceu_bcast_blocks(&xbstack, cur->Bcast.X.dn_block, evt, &killed);
                        if (xbstack.block==NULL || killed) {
//SPC_DEC(printf("<d< ceu_bcast_dyn = %p [XXX] (killed=%d)\n", cur, killed));
                            return ret;
                        }
                        // CEU_RET_THROW: step (5) may 'catch' 
                        
                        // step (5)
                        if (cur->Bcast.status==CEU_X_STATUS_YIELDED || evt==&CEU_EVT_CLEAR) {
                            int arg = (ret == CEU_RET_THROW) ? CEU_ARG_ERR : CEU_ARG_EVT;
                            CEU_Value* args[] = { evt };
//SPC_EQU(printf(">>> awake %p\n", cur));
                            ret = cur->Bcast.X.frame->proto->f(cur->Bcast.X.frame, bstack, arg, args);
//SPC_EQU(printf("<<< awake %p\n", cur));
                        }
//SPC_DEC(printf("<e< ceu_bcast_dyn = %p\n", cur));
                        return MIN(ret, CEU_RET_RETURN);
                    }
                }
                case CEU_VALUE_X_TASKS: {
//SPC_DEC(printf("<f< ceu_bcast_dyn = %p\n", cur));
                    return ceu_bcast_dyns(bstack, &cur->Bcast.Tasks.dyns, evt);
                case CEU_VALUE_X_TRACK:
                    if (evt->type==CEU_VALUE_X_TASK && cur->Bcast.Track==evt->Dyn) {
                        cur->Bcast.Track = NULL; // tracked coro is terminating
                    }
//SPC_DEC(printf("<g< ceu_bcast_dyn = %p\n", cur));
                    return CEU_RET_RETURN;
                }
                default:
                    assert(0 && "bug found");
            }
            assert(0 && "bug found");
        }

        CEU_RET ceu_bcast_dyns (CEU_BStack* bstack, CEU_Dyns* dyns, CEU_Value* evt) {
//SPC_INC(printf(">>> ceu_bcast_dyns [blk=%p]\n", dyns->up_block));
            CEU_BStack xbstack = { dyns->up_block, bstack };   // all dyns have the same enclosing block, which is checked after each bcast
            int N = dyns->its;  // move out of loop, do not traverse incoming dyns
            for (int i=0; i<N; i++) {
                CEU_Dyn* cur = dyns->buf[i];
                if (cur==NULL || cur->type<CEU_VALUE_BCAST) {
                    // dead or not bcastable
                } else {
                    int ret = ceu_bcast_dyn(&xbstack, cur, evt);
                    if (xbstack.block == NULL) { 
//SPC_DEC(printf("<<< ceu_bcast_dyns [blk=%p]\n", dyns->up_block));
                        return ret;
                    }
                    if (ret == CEU_RET_THROW) {
//SPC_DEC(printf("<<< ceu_bcast_dyns [blk=%p]\n", dyns->up_block));
                        return CEU_RET_THROW;
                    }
                }
            }
//SPC_DEC(printf("<<< ceu_bcast_dyns [blk=%p]\n", dyns->up_block));
            return CEU_RET_RETURN;
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
                case CEU_VALUE_P_CORO:
                case CEU_VALUE_P_TASK:
                case CEU_VALUE_TUPLE:
                case CEU_VALUE_VECTOR:
                case CEU_VALUE_DICT:
                case CEU_VALUE_BCAST:
                case CEU_VALUE_X_CORO:
                case CEU_VALUE_X_TASK:
                case CEU_VALUE_X_TASKS:
                case CEU_VALUE_X_TRACK:
                    return ceu_sizeof(CEU_Value, Dyn);
                default:
                    assert(0 && "bug found");
            }
        }
        
        void ceu_max_depth (CEU_Dyn* dyn, int n, CEU_Value* childs) {
            // new dyn should have at least the maximum depth among its children
            CEU_Dyns* hld = NULL;
            int max = -1;
            for (int i=0; i<n; i++) {
                CEU_Value* cur = &childs[i];
                if (cur->type>CEU_VALUE_DYNAMIC && cur->Dyn->up_dyns.dyns->up_block!=NULL) {
                    if (max < cur->Dyn->up_dyns.dyns->up_block->depth) {
                        max = cur->Dyn->up_dyns.dyns->up_block->depth;
                        hld = cur->Dyn->up_dyns.dyns;
                    }
                }
            }
            if (hld != NULL) {
                ceu_hold_add(hld, dyn);
            }
        }
        
        CEU_RET ceu_block_set_mutual (CEU_Dyn* dst, CEU_Dyn* src) {
            if (dst->tphold == CEU_HOLD_NON) {
                if (src->tphold == CEU_HOLD_NON) {
                    return CEU_RET_RETURN;
                } else {
                    return ceu_block_set(dst, src->up_dyns.dyns, src->tphold);
                }
            } else {
                return ceu_block_set(src, dst->up_dyns.dyns, dst->tphold);
            }
        }

        CEU_RET ceu_tuple_set (CEU_Dyn* tup, int i, CEU_Value v) {
            ceu_gc_inc(&v);
            ceu_gc_dec(&tup->Ncast.Tuple.buf[i], 1);
            tup->Ncast.Tuple.buf[i] = v;
            return (v.type < CEU_VALUE_DYNAMIC) ? CEU_RET_RETURN : ceu_block_set_mutual(tup, v.Dyn);
                //ceu_block_set(v.Dyn, tup->up_dyns.dyns, tup->tphold);
        }
        
        CEU_RET ceu_vector_get (CEU_Dyn* vec, int i) {
            if (i<0 || i>=vec->Ncast.Vector.its) {
                CEU_THROW_MSG("\0 : index error : out of bounds");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            int sz = ceu_tag_to_size(vec->Ncast.Vector.type);
            ceu_acc = (CEU_Value) { vec->Ncast.Vector.type };
            memcpy(&ceu_acc.Number, vec->Ncast.Vector.buf+i*sz, sz);
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_vector_set (CEU_Dyn* vec, int i, CEU_Value v) {
            if (v.type == CEU_VALUE_NIL) {           // pop
                assert(i == vec->Ncast.Vector.its-1);
                assert(CEU_RET_RETURN == ceu_vector_get(vec, i));
                ceu_gc_dec(&ceu_acc, 1);
                vec->Ncast.Vector.its--;
                return CEU_RET_RETURN;
            } else {
                if (vec->Ncast.Vector.its == 0) {
                    vec->Ncast.Vector.type = v.type;
                } else {
                    assert(v.type == vec->Ncast.Vector.type);
                }
                int sz = ceu_tag_to_size(vec->Ncast.Vector.type);
                if (i == vec->Ncast.Vector.its) {           // push
                    if (i == vec->Ncast.Vector.max) {
                        vec->Ncast.Vector.max = vec->Ncast.Vector.max*2 + 1;    // +1 if max=0
                        vec->Ncast.Vector.buf = realloc(vec->Ncast.Vector.buf, vec->Ncast.Vector.max*sz + 1);
                        assert(vec->Ncast.Vector.buf != NULL);
                    }
                    ceu_gc_inc(&v);
                    vec->Ncast.Vector.its++;
                    vec->Ncast.Vector.buf[sz*vec->Ncast.Vector.its] = '\0';
                } else {                            // set
                    assert(CEU_RET_RETURN == ceu_vector_get(vec, i));
                    ceu_gc_inc(&v);
                    ceu_gc_dec(&ceu_acc, 1);
                    assert(i < vec->Ncast.Vector.its);
                }
                memcpy(vec->Ncast.Vector.buf + i*sz, (char*)&v.Number, sz);
                return (v.type < CEU_VALUE_DYNAMIC) ? CEU_RET_RETURN : ceu_block_set_mutual(vec, v.Dyn);
                    //ceu_block_set(v.Dyn, vec->up_dyns.dyns, vec->tphold);
            }
        }
        
        CEU_Dyn* ceu_vector_from_c_string (CEU_Dyns* hld, const char* str) {
            CEU_Dyn* vec = ceu_vector_create(hld);
            int N = strlen(str);
            for (int i=0; i<N; i++) {
                assert(CEU_RET_RETURN == ceu_vector_set(vec, vec->Ncast.Vector.its, (CEU_Value) { CEU_VALUE_CHAR, str[i] }));
            }
            return vec;
        }

        CEU_RET ceu_next_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            CEU_Value NIL = (CEU_Value) { CEU_VALUE_NIL };
            assert(n==1 || n==2);
            CEU_Value* col = args[0];
            CEU_Value* key = (n == 1) ? &NIL : args[1];
            assert(col->type == CEU_VALUE_DICT);
            for (int i=0; i<col->Dyn->Ncast.Dict.max; i++) {
                CEU_Value* args[] = { key, &(*col->Dyn->Ncast.Dict.buf)[i][0] };
                assert(CEU_RET_RETURN == ceu_op_equals_equals_f(NULL, NULL, 2, args));
                if (ceu_acc.Bool) {
                    key = &NIL;
                } else if (key->type == CEU_VALUE_NIL) {
                    ceu_acc = (*col->Dyn->Ncast.Dict.buf)[i][0];
                    return CEU_RET_RETURN;
                }
            }
            ceu_acc = NIL;
            return CEU_RET_RETURN;
        }        
        int ceu_dict_key_to_index (CEU_Dyn* col, CEU_Value* key, int* idx) {
            *idx = -1;
            for (int i=0; i<col->Ncast.Dict.max; i++) {
                CEU_Value* cur = &(*col->Ncast.Dict.buf)[i][0];
                CEU_Value* args[] = { key, cur };
                assert(CEU_RET_RETURN == ceu_op_equals_equals_f(NULL, NULL, 2, args));
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
                return (*col->Ncast.Dict.buf)[i][1];
            } else {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
        }        
        CEU_RET ceu_dict_set (CEU_Dyn* col, CEU_Value* key, CEU_Value* val) {
            //assert(key->type != CEU_VALUE_NIL);     // TODO
            int old;
            ceu_dict_key_to_index(col, key, &old);
            if (old == -1) {
                old = col->Ncast.Dict.max;
                int new = MAX(5, old * 2);
                col->Ncast.Dict.max = new;
                col->Ncast.Dict.buf = realloc(col->Ncast.Dict.buf, new*2*sizeof(CEU_Value));
                assert(col->Ncast.Dict.buf != NULL);
                memset(&(*col->Ncast.Dict.buf)[old], 0, (new-old)*2*sizeof(CEU_Value));  // x[i]=nil
                int ret1 = (key->type < CEU_VALUE_DYNAMIC) ? CEU_RET_RETURN : ceu_block_set_mutual(col, key->Dyn);
                    //ceu_block_set(key->Dyn, col->up_dyns.dyns, col->tphold);
                int ret2 = (val->type < CEU_VALUE_DYNAMIC) ? CEU_RET_RETURN : ceu_block_set_mutual(col, val->Dyn);
                    //ceu_block_set(val->Dyn, col->up_dyns.dyns, col->tphold);
                assert(MIN(ret1,ret2) != CEU_RET_THROW);
            }
            assert(old != -1);
            
            CEU_Value vv = ceu_dict_get(col, key);
            
            if (val->type == CEU_VALUE_NIL) {
                ceu_gc_dec(&vv, 1);
                ceu_gc_dec(key, 1);
                (*col->Ncast.Dict.buf)[old][0] = (CEU_Value) { CEU_VALUE_NIL };
            } else {
                ceu_gc_inc(val);
                ceu_gc_dec(&vv, 1);
                if (vv.type == CEU_VALUE_NIL) {
                    ceu_gc_inc(key);
                }
                (*col->Ncast.Dict.buf)[old][0] = *key;
                (*col->Ncast.Dict.buf)[old][1] = *val;
            }
            
            return CEU_RET_RETURN;                  // TODO
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
                if (col->type==CEU_VALUE_TUPLE && (idx->Number<0 || idx->Number>=col->Dyn->Ncast.Tuple.its)) {                
                    CEU_THROW_MSG("\0 : index error : out of bounds");
                    CEU_THROW_RET(CEU_ERR_ERROR);
                }
                if (col->type==CEU_VALUE_VECTOR && (idx->Number<0 || idx->Number>col->Dyn->Ncast.Vector.its)) {                
                    CEU_THROW_MSG("\0 : index error : out of bounds");
                    CEU_THROW_RET(CEU_ERR_ERROR); // accepts v[#v]
                }
            }
            return CEU_RET_RETURN;
        }
    """ +
    """ // CREATES
        CEU_Dyn* ceu_proto_create (int type, CEU_Proto proto, CEU_Dyns* hld, CEU_HOLD tphold) {
            CEU_Dyn* ret = malloc(sizeof(CEU_Dyn));
            assert(ret != NULL);
            proto.upvs.buf = malloc(proto.upvs.its * sizeof(CEU_Value));
            assert(proto.upvs.buf != NULL);
            for (int i=0; i<proto.upvs.its; i++) {
                proto.upvs.buf[i] = (CEU_Value) { CEU_VALUE_NIL };
            }
            *ret = (CEU_Dyn) {
                type, {NULL,-1}, NULL, tphold, {
                    .Ncast = { 0, {.Proto=proto} }
                }
            };
            //assert(CEU_RET_RETURN == ceu_block_set(ret, hld, tphold));
            ceu_hold_add(hld, ret);
            return ret;
        }
        
        CEU_Dyn* ceu_tuple_create (CEU_Dyns* hld, int n) {
            CEU_Dyn* ret = malloc(sizeof(CEU_Dyn) + n*sizeof(CEU_Value));
            assert(ret != NULL);
            *ret = (CEU_Dyn) {
                CEU_VALUE_TUPLE, {NULL,-1}, NULL, CEU_HOLD_NON, {
                    .Ncast = { 0, {.Tuple={n,{}} } }
                }
            };
            memset(ret->Ncast.Tuple.buf, 0, n*sizeof(CEU_Value));
            ceu_hold_add(hld, ret);
            return ret;
        }
        
        CEU_Dyn* ceu_vector_create (CEU_Dyns* hld) {
            CEU_Dyn* ret = malloc(sizeof(CEU_Dyn));
            assert(ret != NULL);
            char* buf = malloc(1);  // because of '\0' in empty strings
            assert(buf != NULL);
            buf[0] = '\0';
            *ret = (CEU_Dyn) {
                CEU_VALUE_VECTOR, {NULL,-1}, NULL, CEU_HOLD_NON, {
                    .Ncast = { 0, {.Vector={0,0,CEU_VALUE_NIL,buf}} }
                }
            };
            ceu_hold_add(hld, ret);
            return ret;
        }
        
        CEU_Dyn* ceu_dict_create (CEU_Dyns* hld) {
            CEU_Dyn* ret = malloc(sizeof(CEU_Dyn));
            assert(ret != NULL);
            *ret = (CEU_Dyn) {
                CEU_VALUE_DICT, {NULL,-1}, NULL, CEU_HOLD_NON, {
                    .Ncast = { 0, {.Dict={0,NULL}} }
                }
            };
            ceu_hold_add(hld, ret);
            return ret;
        }
        
        void ceu_tasks_create (CEU_Dyns* hld, int max, CEU_Value* ret) {
            CEU_Dyn* tasks = malloc(sizeof(CEU_Dyn));
            assert(tasks != NULL);
            CEU_Block* blk = (hld == NULL) ? NULL : hld->up_block;
            *tasks = (CEU_Dyn) {
                CEU_VALUE_X_TASKS, {NULL,-1}, NULL, CEU_HOLD_FIX, {
                    .Bcast = { CEU_X_STATUS_YIELDED, {
                        .Tasks = { max, {0,0,NULL,blk} }
                    } }
                }
            };            
            *ret = (CEU_Value) { CEU_VALUE_X_TASKS, {.Dyn=tasks} };
            
            // hld is the enclosing block of "tasks()", not of T
            // T would be the outermost possible scope, but we use hld b/c
            // we cannot express otherwise
            ceu_hold_add(hld, tasks);
        }
        
        CEU_RET ceu_x_create (CEU_Dyns* hld, CEU_Value* X, CEU_Value* ret) {
            if (X->type==CEU_VALUE_P_CORO || X->type==CEU_VALUE_P_TASK) {
                // ok
            } else {
                CEU_THROW_MSG("\0 : spawn error : expected coro or task");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            ceu_gc_inc(X);
            
            CEU_Dyn* x = malloc(sizeof(CEU_Dyn));
            assert(x != NULL);
            CEU_Frame* frame = malloc(sizeof(CEU_Frame));
            assert(frame != NULL);
            char* mem = malloc(X->Dyn->Ncast.Proto.X.n_mem);
            assert(mem != NULL);
            
            int tag = (X->type == CEU_VALUE_P_CORO) ? CEU_VALUE_X_CORO : CEU_VALUE_X_TASK;
            *x = (CEU_Dyn) {
                tag, {NULL,-1}, NULL, CEU_HOLD_FIX, {
                    .Bcast = { CEU_X_STATUS_YIELDED, {
                        .X = { NULL, NULL, frame }
                    } }
                }
            };
            CEU_Block* blk = (hld == NULL) ? NULL : hld->up_block;
            *frame = (CEU_Frame) { &X->Dyn->Ncast.Proto, blk, mem, {
                .X = { x, 0, { CEU_VALUE_NIL } }
            } };
            *ret = (CEU_Value) { tag, {.Dyn=x} };
            
            // hld is the enclosing block of "coroutine T", not of T
            // T would be the outermost possible scope, but we use hld b/c
            // we cannot express otherwise
            ceu_hold_add(hld, x);
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_x_create_in (CEU_Dyn* tasks, CEU_Value* task, CEU_Value* ret, int* ok) {
            if (tasks->type != CEU_VALUE_X_TASKS) {
                CEU_THROW_MSG("\0 : coroutine error : expected tasks");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            *ok = !(
                (tasks->Bcast.Tasks.max != 0) &&
                (tasks->Bcast.Tasks.dyns.its >= tasks->Bcast.Tasks.max) &&
                (ceu_hold_hole(&tasks->Bcast.Tasks.dyns) == -1)
            );
            if (!*ok) {
                return CEU_RET_RETURN;
            }
            if (task->type != CEU_VALUE_P_TASK) {
                CEU_THROW_MSG("\0 : coroutine error : expected task");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            ceu_gc_inc(task);
            
            CEU_Dyn* x = malloc(sizeof(CEU_Dyn));
            assert(x != NULL);
            CEU_Frame* frame = malloc(sizeof(CEU_Frame));
            assert(frame != NULL);
            char* mem = malloc(task->Dyn->Ncast.Proto.X.n_mem);
            assert(mem != NULL);
        
            *x = (CEU_Dyn) {
                CEU_VALUE_X_TASK, {NULL,-1}, NULL, CEU_HOLD_EVT, {
                    .Bcast = { CEU_X_STATUS_YIELDED, {
                        .X = { tasks, NULL, frame }
                    } }
                }
            };
            *frame = (CEU_Frame) { &task->Dyn->Ncast.Proto, tasks->Bcast.Tasks.dyns.up_block, mem, {
                .X = { x, 0, { CEU_VALUE_NIL } }
            } };
            *ret = (CEU_Value) { CEU_VALUE_X_TASK, {.Dyn=x} };
            
            ceu_hold_add(&tasks->Bcast.Tasks.dyns, x);
            return CEU_RET_RETURN;
        }
        
        void ceu_track_create (CEU_Dyns* hld, CEU_Dyn* x, CEU_Value* ret) {
            CEU_Dyn* trk = malloc(sizeof(CEU_Dyn));
            assert(trk != NULL);
            *trk = (CEU_Dyn) {
                CEU_VALUE_X_TRACK, {NULL,-1}, NULL, CEU_HOLD_NON, {
                    .Bcast = { CEU_X_STATUS_YIELDED, {
                        .Track = x
                    } }
                }
            };
            ceu_hold_add(hld, trk);
            *ret = (CEU_Value) { CEU_VALUE_X_TRACK, {.Dyn=trk} };
        }
    """ +
    """ // PRINT
        void ceu_print1 (CEU_Frame* _1, CEU_Value* v) {
            // no tags when _1==NULL (ceu_error_list_print)
            if (_1!=NULL && v->type>CEU_VALUE_DYNAMIC) {  // TAGS
                CEU_Value* args[1] = { v };
                int ok = ceu_tags_f(_1, NULL, 1, args);
                CEU_Value tup = ceu_acc;
                assert(ok == CEU_RET_RETURN);
                int N = tup.Dyn->Ncast.Tuple.its;
                if (N > 0) {
                    if (N > 1) {
                        printf("[");
                    }
                    for (int i=0; i<N; i++) {
                        ceu_print1(_1, &tup.Dyn->Ncast.Tuple.buf[i]);
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
                    for (int i=0; i<v->Dyn->Ncast.Tuple.its; i++) {
                        if (i > 0) {
                            printf(",");
                        }
                        ceu_print1(_1, &v->Dyn->Ncast.Tuple.buf[i]);
                    }                    
                    printf("]");
                    break;
                case CEU_VALUE_VECTOR:
                    if (v->Dyn->Ncast.Vector.type == CEU_VALUE_CHAR) {
                        printf("%s", v->Dyn->Ncast.Vector.buf);
                    } else {
                        printf("#[");
                        for (int i=0; i<v->Dyn->Ncast.Vector.its; i++) {
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
                    for (int i=0; i<v->Dyn->Ncast.Dict.max; i++) {
                        if ((*v->Dyn->Ncast.Dict.buf)[i][0].type != CEU_VALUE_NIL) {
                            if (comma != 0) {
                                printf(",");
                            }
                            comma = 1;
                            printf("(");
                            ceu_print1(_1, &(*v->Dyn->Ncast.Dict.buf)[i][0]);
                            printf(",");
                            ceu_print1(_1, &(*v->Dyn->Ncast.Dict.buf)[i][1]);
                            printf(")");
                        }
                    }                    
                    printf("]");
                    break;
                case CEU_VALUE_P_FUNC:
                    printf("func: %p", v->Dyn);
                    break;
                case CEU_VALUE_P_CORO:
                    printf("coro: %p", v->Dyn);
                    break;
                case CEU_VALUE_P_TASK:
                    printf("task: %p", v->Dyn);
                    break;
                case CEU_VALUE_X_CORO:
                    printf("x-coro: %p", v->Dyn);
                    break;
                case CEU_VALUE_X_TASK:
                    printf("x-task: %p", v->Dyn);
                    break;
                case CEU_VALUE_X_TASKS:
                    printf("x-tasks: %p", v->Dyn);
                    break;
                case CEU_VALUE_X_TRACK:
                    printf("x-track: %p", v->Dyn);
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        CEU_RET ceu_print_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            for (int i=0; i<n; i++) {
                if (i > 0) {
                    printf("\t");
                }
                ceu_print1(_1, args[i]);
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_println_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(CEU_RET_RETURN == ceu_print_f(_1, _2, n, args));
            printf("\n");
            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
            return CEU_RET_RETURN;
        }
    """ +
    """
        // EQ / NEQ / LEN / COROS / MOVE / COPY / THROW / TRACK
        CEU_RET ceu_op_equals_equals_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
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
                    /*
                    case CEU_VALUE_TUPLE:
                        v = (e1->Dyn == e2->Dyn);
                        if (v) {
                            // OK
                        } else {
                            v = (e1->Dyn->Ncast.Tuple.its==e2->Dyn->Ncast.Tuple.its);
                            if (v) {
                                for (int i=0; i<e1->Dyn->Ncast.Tuple.its; i++) {
                                    CEU_Value* xs[] = { &e1->Dyn->Ncast.Tuple.buf[i], &e2->Dyn->Ncast.Tuple.buf[i] };
                                    assert(CEU_RET_RETURN == ceu_op_equals_equals_f(_1, _2, 2, xs));
                                    if (!ceu_acc.Bool) {
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    */
                    case CEU_VALUE_TUPLE:
                    case CEU_VALUE_VECTOR:
                    case CEU_VALUE_DICT:
                    case CEU_VALUE_P_FUNC:
                    case CEU_VALUE_P_CORO:
                    case CEU_VALUE_P_TASK:
                    case CEU_VALUE_X_CORO:
                    case CEU_VALUE_X_TASK:
                    case CEU_VALUE_X_TASKS:
                    case CEU_VALUE_X_TRACK:
                        v = (e1->Dyn == e2->Dyn);
                        break;
                    default:
                        assert(0 && "bug found");
                }
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=v} };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_op_slash_equals_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(CEU_RET_RETURN == ceu_op_equals_equals_f(_1, _2, n, args));
            ceu_acc.Bool = !ceu_acc.Bool;
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_op_hash_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            if (args[0]->type == CEU_VALUE_VECTOR) {
                ceu_acc = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=args[0]->Dyn->Ncast.Vector.its} };
            } else if (args[0]->type == CEU_VALUE_TUPLE) {
                ceu_acc = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=args[0]->Dyn->Ncast.Tuple.its} };
            } else {
                CEU_THROW_MSG("\0 : length error : not a vector");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_coroutine_f (CEU_Frame* frame, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* coro = args[0];
            if (coro->type != CEU_VALUE_P_CORO) {
                CEU_THROW_MSG("\0 : coroutine error : expected coro");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            return ceu_x_create(&frame->up_block->dn_dyns, coro, &ceu_acc);
        }
        
        CEU_RET ceu_status_f (CEU_Frame* frame, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* coro = args[0];
            if (coro->type!=CEU_VALUE_X_CORO && coro->type!=CEU_VALUE_X_TASK) {
                CEU_THROW_MSG("\0 : status error : expected coroutine");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_TAG, {.Tag=coro->Dyn->Bcast.status + CEU_TAG_yielded - 1} };
            return CEU_RET_RETURN;
        }

        CEU_RET ceu_tasks_f (CEU_Frame* frame, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n <= 1);
            int max = 0;
            if (n == 1) {
                CEU_Value* xmax = args[0];
                if (xmax->type!=CEU_VALUE_NUMBER || xmax->Number<=0) {                
                    CEU_THROW_MSG("tasks error : expected positive number");
                    CEU_THROW_RET(CEU_ERR_ERROR);
                }
                max = xmax->Number;
            }
            ceu_tasks_create(&frame->up_block->dn_dyns, max, &ceu_acc);
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_detrack_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* track = args[0];
            if (track->type != CEU_VALUE_X_TRACK) {                
                CEU_THROW_MSG("detrack error : expected track value");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            if (track->Dyn->Bcast.Track == NULL) {
                ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
            } else {
                ceu_acc = (CEU_Value) { CEU_VALUE_X_TASK, {.Dyn=track->Dyn->Bcast.Track} };
            }
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_move_f (CEU_Frame* frame, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* src = args[0];
            CEU_Dyn* dyn = src->Dyn;
            if (src->type > CEU_VALUE_DYNAMIC) {
                if (dyn->tphold >= CEU_HOLD_FIX) {
                    CEU_THROW_MSG("move error : value is not movable");
                    CEU_THROW_RET(CEU_ERR_ERROR);
                }
                dyn->tphold = CEU_HOLD_NON;
                ceu_hold_rem(dyn);
                ceu_hold_add(&frame->up_block->dn_dyns, dyn);
            }
            switch (src->type) {
                case CEU_VALUE_P_FUNC:
                case CEU_VALUE_P_CORO:
                case CEU_VALUE_P_TASK:
                    for (int i=0; i<dyn->Ncast.Proto.upvs.its; i++) {
                        CEU_Value* args[1] = { &dyn->Ncast.Proto.upvs.buf[i] };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, _2, 1, args));
                    }
                    ceu_acc = *src;
                    break;
                case CEU_VALUE_TUPLE: {
                    for (int i=0; i<dyn->Ncast.Tuple.its; i++) {
                        CEU_Value* args[1] = { &dyn->Ncast.Tuple.buf[i] };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, _2, 1, args));
                    }
                    ceu_acc = *src;
                    break;
                }
                case CEU_VALUE_VECTOR: {
                    for (int i=0; i<dyn->Ncast.Vector.its; i++) {
                        assert(CEU_RET_RETURN == ceu_vector_get(dyn, i));
                        CEU_Value ceu_accx = ceu_acc;
                        CEU_Value* args[1] = { &ceu_accx };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, _2, 1, args));
                    }
                    ceu_acc = *src;
                    break;
                }
                case CEU_VALUE_DICT: {
                    for (int i=0; i<dyn->Ncast.Dict.max; i++) {
                        CEU_Value* args0[1] = { &(*dyn->Ncast.Dict.buf)[i][0] };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, _2, 1, args0));
                        CEU_Value* args1[1] = { &(*dyn->Ncast.Dict.buf)[i][1] };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, _2, 1, args1));
                    }
                    ceu_acc = *src;
                    break;
                }
                default:
                    ceu_acc = *src;
                    break;
            }
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_copy_f (CEU_Frame* frame, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* src = args[0];
            CEU_Dyn* old = src->Dyn;
            switch (src->type) {
                case CEU_VALUE_TUPLE: {
                    CEU_Dyn* new = ceu_tuple_create(&frame->up_block->dn_dyns, old->Ncast.Tuple.its);
                    assert(new != NULL);
                    new->tphold = CEU_HOLD_NON;
                    for (int i=0; i<old->Ncast.Tuple.its; i++) {
                        CEU_Value* args[1] = { &old->Ncast.Tuple.buf[i] };
                        assert(CEU_RET_RETURN == ceu_copy_f(frame, _2, 1, args));
                        assert(CEU_RET_RETURN == ceu_tuple_set(new, i, ceu_acc));
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=new} };
                    break;
                }
                case CEU_VALUE_VECTOR: {
                    CEU_Dyn* new = ceu_vector_create(&frame->up_block->dn_dyns);
                    assert(new != NULL);
                    new->tphold = CEU_HOLD_NON;
                    for (int i=0; i<old->Ncast.Vector.its; i++) {
                        assert(CEU_RET_RETURN == ceu_vector_get(old, i));
                        CEU_Value ceu_accx = ceu_acc;
                        CEU_Value* args[1] = { &ceu_accx };
                        assert(CEU_RET_RETURN == ceu_copy_f(frame, _2, 1, args));
                        assert(CEU_RET_RETURN == ceu_vector_set(new, i, ceu_acc));
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=new} };
                    break;
                }
                case CEU_VALUE_DICT: {
                    CEU_Dyn* new = ceu_dict_create(&frame->up_block->dn_dyns);
                    assert(new != NULL);
                    new->tphold = CEU_HOLD_NON;
                    for (int i=0; i<old->Ncast.Dict.max; i++) {
                        {
                            CEU_Value* args[1] = { &(*old->Ncast.Dict.buf)[i][0] };
                            assert(CEU_RET_RETURN == ceu_copy_f(frame, _2, 1, args));
                        }
                        CEU_Value key = ceu_acc;
                        if (key.type == CEU_VALUE_NIL) {
                            continue;
                        }
                        {
                            CEU_Value* args[1] = { &(*old->Ncast.Dict.buf)[i][1] };
                            assert(CEU_RET_RETURN == ceu_copy_f(frame, _2, 1, args));
                        }
                        CEU_Value val = ceu_acc;
                        ceu_dict_set(new, &key, &val);
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_DICT, {.Dyn=new} };
                    break;
                }
                case CEU_VALUE_X_TRACK:
                    ceu_track_create(&frame->up_block->dn_dyns, old->Bcast.Track, &ceu_acc);
                    break;
                case CEU_VALUE_P_FUNC:
                case CEU_VALUE_P_CORO:
                case CEU_VALUE_P_TASK:
                case CEU_VALUE_X_CORO:
                case CEU_VALUE_X_TASK:
                case CEU_VALUE_X_TASKS:
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
                    assert(CEU_RET_RETURN == ceu_tags_f(frame, _2, 3, args));
                    cur = cur->next;
                }
                ceu_acc = new;
            }
            return CEU_RET_RETURN;
        }

        CEU_RET ceu_throw_f (CEU_Frame* _1, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_THROW_MSG("throw error : uncaught exception");
            ceu_acc = *args[0];
            #if 0
            if (ceu_acc.type > CEU_VALUE_DYNAMIC) {
                if (!ceu_block_hld(CEU_HOLD_EVT,ceu_acc.Dyn->tphold)) {
                    CEU_THROW_MSG("\0 : throw error : incompatible scopes");
                    CEU_THROW_RET(CEU_ERR_ERROR);
                }
                ceu_block_rec(NULL, ceu_acc.Dyn, CEU_HOLD_EVT);
            }
            #endif
            ceu_gc_inc(&ceu_acc);
            return CEU_RET_THROW;
        }

        CEU_RET ceu_track_f (CEU_Frame* frame, CEU_BStack* _2, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* task = args[0];
            if (task->type != CEU_VALUE_X_TASK) {                
                CEU_THROW_MSG("track error : expected task");
                CEU_THROW_RET(CEU_ERR_ERROR);
            } else if (task->Dyn->Bcast.status == CEU_X_STATUS_TERMINATED) {                
                CEU_THROW_MSG("track error : expected unterminated task");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            ceu_track_create(&frame->up_block->dn_dyns, task->Dyn, &ceu_acc);
            return CEU_RET_RETURN;
        }
    """ +
    """ // FUNCS
        typedef struct {
            ${GLOBALS.map { "CEU_Value ${it.id2c(null)};\n" }.joinToString("")}
            ${this.mem}
        } CEU_Proto_Mem_${this.outer.n};
        CEU_Proto_Mem_${this.outer.n} _ceu_mem_;
        CEU_Proto_Mem_${this.outer.n}* ceu_mem = &_ceu_mem_;
        CEU_Proto_Mem_${this.outer.n}* ceu_mem_${this.outer.n} = &_ceu_mem_;
        //CEU_Proto _ceu_proto_ = { NULL, {}, {} };
        CEU_Frame _ceu_frame_ = { NULL, NULL, (char*) &_ceu_mem_, {} };
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
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_copy_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_tasks = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_tasks_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_coroutine = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_coroutine_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_detrack = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_detrack_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_move = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_move_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_next = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_next_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_print = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_print_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_println = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_println_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_status = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_status_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_sup_question_ = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_sup_question__f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_tags = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_tags_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_throw = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_throw_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_track = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_track_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_type = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_type_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_op_equals_equals = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_op_equals_equals_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_op_hash = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_op_hash_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_op_slash_equals = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_op_slash_equals_f, {0,NULL}, {{0}} }
                        }
                    };
                    static CEU_Dyn ceu_string_dash_to_dash_tag = { 
                        CEU_VALUE_P_FUNC, {NULL,-1}, NULL, 1, 1, {
                            .Proto = { NULL, ceu_string_dash_to_dash_tag_f, {0,NULL}, {{0}} }
                        }
                    };

                    ceu_mem->copy       = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_copy}         };
                    ceu_mem->coroutine  = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_coroutine}    };
                    ceu_mem->detrack    = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_detrack}      };
                    ceu_mem->move       = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_move}         };
                    ceu_mem->next       = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_next}         };
                    ceu_mem->print      = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_print}        };
                    ceu_mem->println    = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_println}      };            
                    ceu_mem->status     = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_status}       };
                    ceu_mem->tags       = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_tags}         };
                    ceu_mem->tasks      = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_tasks}        };
                    ceu_mem->throw      = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_throw}        };
                    ceu_mem->track      = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_track}        };
                    ceu_mem->type       = (CEU_Value) { CEU_VALUE_P_FUNC, {.Dyn=&ceu_type}         };
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
