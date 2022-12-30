fun Coder.main (): String {
    return ("" +
    """ // INCLUDES / PROTOS
        #include <stdio.h>
        #include <stdlib.h>
        #include <stddef.h>
        #include <stdint.h>
        #include <string.h>
        #include <assert.h>
        #include <stdarg.h>
        #include <math.h>

        #define MAX(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a > _b ? _a : _b; })
        #define MIN(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a < _b ? _a : _b; })

        struct CEU_Value;
            struct CEU_Dynamic;
            struct CEU_Frame;        
            struct CEU_Block;
            struct CEU_Proto;
        struct CEU_Tags_List;
        struct CEU_Tags_Names;
        struct CEU_Block;
        struct CEU_Error_List;

        typedef enum {
            CEU_RET_THROW = 0,
            CEU_RET_RETURN,
            CEU_RET_YIELD
        } CEU_RET;

        typedef enum {
            CEU_ARG_ERR = -2,
            CEU_ARG_EVT = -1,
            CEU_ARG_ARGS = 0    // 0,1,...
        } CEU_ARG;

        CEU_RET ceu_type_f (struct CEU_Frame* _1, int n, struct CEU_Value* args[]);
        int ceu_as_bool (struct CEU_Value* v);

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

        #define CEU_TAG_DEFINE(id,str)              \
            const int CEU_TAG_##id = __COUNTER__;   \
            CEU_Tags_Names ceu_tag_##id = { str, NULL };
        #define CEU_TAG_INIT(id,str)                \
            ceu_tag_##id.next = CEU_TAGS;           \
            CEU_TAGS = &ceu_tag_##id;               \
            CEU_TAGS_MAX++;            
        CEU_RET ceu_tags_f (struct CEU_Frame* _1, int n, struct CEU_Value* args[]);
        char* ceu_tag_to_string (int tag);
        
        CEU_RET ceu_block_set (struct CEU_Block* dst, struct CEU_Dynamic* src, int isperm);
        
        void  ceu_coros_cleanup  (struct CEU_Dynamic* coros);
        void  ceu_coros_destroy  (struct CEU_Dynamic* coros, struct CEU_Dynamic* coro);
        CEU_RET ceu_coros_create   (struct CEU_Block* hld, int max, struct CEU_Value* ret); 
        CEU_RET ceu_coro_create    (struct CEU_Block* hld, struct CEU_Value* task, struct CEU_Value* ret);
        CEU_RET ceu_coro_create_in (struct CEU_Block* hld, struct CEU_Dynamic* coros, struct CEU_Value* task, struct CEU_Value* ret, int* ok);
        
        void ceu_bcast_enqueue (struct CEU_Dynamic** outer, struct CEU_Dynamic* dyn);
        void ceu_bcast_dequeue (struct CEU_Dynamic** outer, struct CEU_Dynamic* dyn);
        void ceu_bcast_free (void);
        
        CEU_RET ceu_bcast_dyns   (struct CEU_Dynamic* cur, struct CEU_Value* evt);
        CEU_RET ceu_bcast_blocks (struct CEU_Block* cur, struct CEU_Value* evt);
        CEU_RET ceu_bcast_dyn    (struct CEU_Dynamic* cur, struct CEU_Value* evt);
        
        int ceu_tag_to_size (int type);
        void ceu_max_depth (struct CEU_Dynamic* dyn, int n, struct CEU_Value* childs);
        int ceu_dict_key_to_index (struct CEU_Dynamic* col, struct CEU_Value* key, int* idx);
        struct CEU_Value ceu_dict_get (struct CEU_Dynamic* col, struct CEU_Value* key);
        CEU_RET ceu_dict_set (struct CEU_Dynamic* col, struct CEU_Value* key, struct CEU_Value* val);
        CEU_RET ceu_col_check (struct CEU_Value* col, struct CEU_Value* idx);
        struct CEU_Dynamic* ceu_tuple_create (struct CEU_Block* hld, int n, struct CEU_Value* args);
        
        struct CEU_Dynamic* ceu_track_create (struct CEU_Dynamic* coro, struct CEU_Value* ret);
        struct CEU_Value ceu_track_to_coro (struct CEU_Value* track);
        
        void ceu_print1 (struct CEU_Value* v);
        CEU_RET ceu_op_equals_equals_f (struct CEU_Frame* frame, int n, struct CEU_Value* args[]);
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
            CEU_VALUE_FUNC,     // func frame
            CEU_VALUE_TASK,     // task frame
            CEU_VALUE_TUPLE,
            CEU_VALUE_VECTOR,
            CEU_VALUE_DICT,
            CEU_VALUE_BCAST,    // all below are bcast
            CEU_VALUE_CORO,     // spawned task
            CEU_VALUE_COROS,    // pool of spawned tasks
            CEU_VALUE_TRACK
        } CEU_VALUE;
        
        typedef enum CEU_CORO_STATUS {
            CEU_CORO_STATUS_YIELDED = 1,
            CEU_CORO_STATUS_TOGGLED,
            CEU_CORO_STATUS_RESUMED,
            CEU_CORO_STATUS_TERMINATED,
            CEU_CORO_STATUS_DESTROYED
        } CEU_CORO_STATUS;        

        typedef struct CEU_Value {
            CEU_VALUE type;
            union {
                //void nil;
                int Tag;
                int Bool;
                char Char;
                double Number;
                void* Pointer;
                struct CEU_Dynamic* Dyn;    // Func/Task/Tuple/Dict/Coro/Coros: allocates memory
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
            struct CEU_Frame* up;   // points to active frame
            CEU_Proto_F f;
            union {
                struct {
                    int awakes;     // if it should awake even from non-clear bcasts
                    int n;          // sizeof mem
                } Task;
            };
        } CEU_Proto;
        
        typedef struct CEU_Frame {    // call func / create task
            CEU_Proto* proto;
            struct CEU_Block* up;
            char* mem;
            union {
                struct {
                    struct CEU_Dynamic* coro;   // coro/frame point to each other
                    int pc;                     // next line to execute
                    CEU_Value pub;              // public value
                } Task;
            };
        } CEU_Frame;
    """ +
    """ // CEU_Dynamic
        typedef struct CEU_Dynamic {
            CEU_VALUE type;                 // required to switch over free/bcast
            struct CEU_Dynamic* next;       // next dyn to free (not used by coro in coros)
            struct CEU_Block*   hold;       // holding block to compare on set/move
            struct CEU_Tags_List* tags;     // linked list of tags
            int isperm;                     // if hold is permanent and may not be reset to outer block
            union {
                CEU_Proto Proto;
                struct {
                    int n;                  // number of items
                    CEU_Value mem[0];       // beginning of CEU_Value[n]
                } Tuple;
                struct {
                    int max;                // size of mem
                    int n;                  // number of items
                    CEU_VALUE type;
                    char* mem;              // resizable Unknown[n]
                } Vector;
                struct {
                    int max;                // size of mem
                    CEU_Value (*mem)[0][2]; // resizable CEU_Value[n][2]
                } Dict;
                struct {
                    enum CEU_CORO_STATUS status;
                    struct CEU_Dynamic* next;           // bcast->Bcast, next dyn to bcast
                    union {
                        struct {
                            struct CEU_Dynamic* coros;  // auto terminate / remove from coros
                            struct CEU_Block* block;    // first block to bcast
                            CEU_Frame* frame;
                        } Coro;
                        struct {
                            int max;                // max number of instances
                            int cur;                // cur number of instances
                            int open;               // number of open iterators
                            struct CEU_Dynamic* first;  // coro->Bcast.Coro, first coro to bcast/free
                        } Coros;
                        struct CEU_Dynamic* Track;   // starts as CORO and may fall to NIL
                    };
                } Bcast;
            };
        } CEU_Dynamic;
    """ +
    """ // CEU_Block
        typedef struct CEU_Block {
            int depth;                          // compare on set
            int ispub;                          // is top block inside task?
            CEU_Dynamic* tofree;                // list of allocated data to free on exit
            struct {
                struct CEU_Dynamic* up;         // enclosing active coro
                struct CEU_Block* block;        // nested block active
                struct CEU_Dynamic* dyn;        // first coro/coros in this block
            } bcast;
        } CEU_Block;
    """ +
    """ // CEU_Tags
        typedef struct CEU_Tags_Names {
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
        struct CEU_Dynamic* ceu_proto_create (struct CEU_Block* hld, int type, struct CEU_Frame* frame, CEU_Proto_F f, int awakes, int n);        
        struct CEU_Dynamic* ceu_dict_create  (struct CEU_Block* hld, int n, struct CEU_Value (*args)[][2]);

        static CEU_Tags_Names* CEU_TAGS = NULL;
        int CEU_TAGS_MAX = 0;
        ${this.tags.map { "CEU_TAG_DEFINE(${it.second},\"${it.first}\")\n" }.joinToString("")}

        const CEU_Value CEU_ERR_ERROR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_error} };
        CEU_Error_List* ceu_error_list = NULL;
        
        CEU_Value CEU_EVT_NIL = { CEU_VALUE_NIL }; 
        CEU_Value CEU_EVT_CLEAR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_clear} };
        
        CEU_RET ceu_ret = CEU_RET_RETURN;
        CEU_Value ceu_acc;
        
        int ceu_bcasting = 0;
        CEU_Dynamic* ceu_bcast_tofree = NULL;

        // TODO: remove (only here b/c we do not test CEU_CONTINUE_ON_CLEAR at compile time)
        const int ceu_n = 0;
        const CEU_Value* ceu_evt = &CEU_EVT_NIL;    // also b/c of `evt` outside task
    """ +
    """ // IMPLS
        int ceu_as_bool (CEU_Value* v) {
            return !(v->type==CEU_VALUE_NIL || (v->type==CEU_VALUE_BOOL && !v->Bool));
        }
        CEU_RET ceu_type_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(n == 1 && "bug found");
            ceu_acc = (CEU_Value) { CEU_VALUE_TAG, {.Tag=args[0]->type} };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_tags_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n >= 2);
            CEU_Value* dyn = args[0];
            CEU_Value* tag = args[1];
            assert(tag->type == CEU_VALUE_TAG);
            if (n == 2) {   // check
                ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} };
                if (dyn->type < CEU_VALUE_DYNAMIC) {
                    // no tags
                } else {
                    CEU_Tags_List* cur = dyn->Dyn->tags;
                    while (cur != NULL) {
                        if (cur->tag == tag->Tag) {
                            ceu_acc.Bool = 1;
                            break;
                        }
                        cur = cur->next;
                    }
                }
            } else if (n == 3) {    // add/rem
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
            }
            return CEU_RET_RETURN;
        }
        char* ceu_tag_to_string (int tag) {
            CEU_Tags_Names* cur = CEU_TAGS;
            for (int i=0; i<CEU_TAGS_MAX-tag-1; i++) {
                cur = cur->next;
            }
            return cur->name;
        }
    """ +
    """ // BLOCK
        void ceu_dyns_free (CEU_Dynamic* cur) {
            CEU_Dynamic* nxt = cur;
            while (nxt != NULL) {
                CEU_Dynamic* cur = nxt;
                nxt = cur->next;

                switch (cur->type) {
                    case CEU_VALUE_FUNC:
                    case CEU_VALUE_TASK:
                    case CEU_VALUE_TUPLE:
                    case CEU_VALUE_COROS:
                    case CEU_VALUE_TRACK:
                        break;
                    case CEU_VALUE_VECTOR:
                        free(cur->Vector.mem);
                        break;
                    case CEU_VALUE_DICT:
                        free(cur->Dict.mem);
                        break;
                    case CEU_VALUE_CORO:
                        cur->Bcast.status = CEU_CORO_STATUS_DESTROYED;
                        if (ceu_bcasting == 0) {
                            free(cur->Bcast.Coro.frame->mem);
                            free(cur->Bcast.Coro.frame);
                        }
                        break;
                    default:
                        assert(0 && "bug found");
                }
                
                if (cur->type<CEU_VALUE_BCAST || ceu_bcasting==0) {
                    while (cur->tags != NULL) {
                        CEU_Tags_List* x = cur->tags;
                        cur->tags = x->next;
                        free(x);
                    }
                    //printf(">>> %d\n", cur->type);
                    free(cur);
                } else {
                    cur->next = ceu_bcast_tofree;
                    ceu_bcast_tofree = cur;
                }
            }
        }

        void ceu_bcast_free (void) {
            if (ceu_bcasting > 0) {
                // do not free anything yet
            } else {
                ceu_dyns_free(ceu_bcast_tofree);
                ceu_bcast_tofree = NULL;
            }
        }

        CEU_RET ceu_block_set (CEU_Block* dst, CEU_Dynamic* src, int isperm) {
            switch (src->type) {
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<src->Tuple.n; i++) {
                        if (src->Tuple.mem[i].type > CEU_VALUE_DYNAMIC) {
                            if (CEU_RET_THROW == ceu_block_set(dst, src->Tuple.mem[i].Dyn, isperm)) {
                                return CEU_RET_THROW;
                            }
                        }
                    }
                    break;
                case CEU_VALUE_VECTOR:
                    if (src->Vector.type > CEU_VALUE_DYNAMIC) {
                        int sz = ceu_tag_to_size(src->Vector.type);
                        for (int i=0; i<src->Vector.n; i++) {
                            if (CEU_RET_THROW == ceu_block_set(dst, *(CEU_Dynamic**)(src->Vector.mem + i*sz), isperm)) {
                                return CEU_RET_THROW;
                            }
                        }
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<src->Dict.max; i++) {
                        if ((*src->Dict.mem)[i][0].type > CEU_VALUE_DYNAMIC) {
                            if (CEU_RET_THROW == ceu_block_set(dst, (*src->Dict.mem)[i][0].Dyn, isperm)) {
                                return CEU_RET_THROW;
                            }
                        }
                        if ((*src->Dict.mem)[i][1].type > CEU_VALUE_DYNAMIC) {
                            if (CEU_RET_THROW == ceu_block_set(dst, (*src->Dict.mem)[i][1].Dyn, isperm)) {
                                return CEU_RET_THROW;
                            }
                        }
                    }
                    break;
                case CEU_VALUE_TRACK:
                    break;
                default:
                    // others never move
                    assert((isperm || src->isperm) && "TODO");
                    break;
            }
            if (dst == src->hold) {
                src->isperm = src->isperm || isperm;
            } else if (src->hold==NULL || (!src->isperm && dst->depth<src->hold->depth)) {
                if (src->type==CEU_VALUE_FUNC && src->Proto.up==NULL) {
                    // do not enqueue: global functions use up=NULL and are not malloc'ed
                } else {
                    { // remove from old block
                        if (src->hold != NULL) {
                            if (src->type > CEU_VALUE_BCAST) {
                                ceu_bcast_dequeue(&src->hold->bcast.dyn, src);
                            }
                            { // remove from free list
                                CEU_Dynamic* prv = NULL;
                                CEU_Dynamic* cur = src->hold->tofree;
                                while (cur != NULL) {
                                    if (cur == src) {
                                        if (prv == NULL) {
                                            src->hold->tofree = cur->next;
                                        } else {
                                            prv->next = cur->next;
                                        }
                                        cur->next = NULL;
                                    }
                                    prv = cur;
                                    cur = cur->next;
                                }
                            }
                        }
                    }
                    { // add to new block
                        src->next = dst->tofree;
                        dst->tofree = src;
                        if (src->type > CEU_VALUE_BCAST) {
                            ceu_bcast_enqueue(&dst->bcast.dyn, src);
                        }
                    }
                }
                src->hold = dst;
                src->isperm = src->isperm || isperm;
            } else if (src->hold->depth>dst->depth && !(src->hold->ispub && src->hold->depth-1==dst->depth)) {
                CEU_THROW_MSG("\0 : set error : incompatible scopes");
                CEU_THROW_RET(CEU_ERR_ERROR);
            } else {
                src->isperm = src->isperm || isperm;
            }
            return CEU_RET_RETURN;
        }
    """ +
    """ // BCAST - ENQUEUE - DEQUEUE - FREE
        void ceu_bcast_enqueue (CEU_Dynamic** outer, CEU_Dynamic* dyn) {
            if (*outer == NULL) {
                *outer = dyn;
            } else {
                CEU_Dynamic* cur = *outer;
                while (cur->Bcast.next != NULL) {
                    cur = cur->Bcast.next;
                }
                cur->Bcast.next = dyn;
            }
        }
        void ceu_bcast_dequeue (CEU_Dynamic** outer, CEU_Dynamic* dyn) {
            if (*outer == dyn) {
                dyn->Bcast.next = NULL;
                *outer = dyn->Bcast.next;
            } else {
                CEU_Dynamic* cur = *outer;
                while (cur->Bcast.next != NULL) {
                    if (cur->Bcast.next == dyn) {
                        CEU_Dynamic* tmp = cur->Bcast.next->Bcast.next;
                        cur->Bcast.next->Bcast.next = NULL;
                        cur->Bcast.next = tmp;
                        break;
                    }
                    cur = cur->Bcast.next;
                }
            }
        }
    """ +
    """ // BCAST_BLOCKS
        CEU_RET ceu_bcast_blocks (CEU_Block* cur, CEU_Value* evt) {
            while (cur != NULL) {
                CEU_Dynamic* dyn = cur->bcast.dyn;
                if (dyn != NULL) {
                    if (ceu_bcast_dyns(dyn,evt) == CEU_RET_THROW) {
                        return CEU_RET_THROW;
                    }
                }
                cur = cur->bcast.block;
            }
            return CEU_RET_RETURN;
        }
    """ +
    """ // BCAST_DYN
        CEU_RET ceu_bcast_dyn (CEU_Dynamic* cur, CEU_Value* evt) {
            if (cur->Bcast.status >= CEU_CORO_STATUS_TERMINATED) {
                return CEU_RET_RETURN;
            }
            if (cur->Bcast.status==CEU_CORO_STATUS_TOGGLED && evt!=&CEU_EVT_CLEAR) {
                // do not awake toggled coro, unless it is a CLEAR event
                return CEU_RET_RETURN;
            }
            switch (cur->type) {
                case CEU_VALUE_CORO: {
                    if (evt!=&CEU_EVT_CLEAR && !cur->Bcast.Coro.frame->proto->Task.awakes) {
                        return CEU_RET_RETURN;
                    } else {
                        // step (1)
                        int ret = ceu_bcast_blocks(cur->Bcast.Coro.block, evt);
                            // CEU_RET_THROW: step (5) may 'catch' 
                        
                        // step (5)
                        if (cur->Bcast.status==CEU_CORO_STATUS_YIELDED || (cur->Bcast.status==CEU_CORO_STATUS_TOGGLED && evt==&CEU_EVT_CLEAR)) {
                            int arg = (ret == CEU_RET_THROW) ? CEU_ARG_ERR : CEU_ARG_EVT;
                            CEU_Value* args[] = { evt };
                            ret = cur->Bcast.Coro.frame->proto->f(cur->Bcast.Coro.frame, arg, args);
                        }
                        return MIN(ret, CEU_RET_RETURN);
                    }
                }
                case CEU_VALUE_COROS: {
                    cur->Bcast.Coros.open++;
                    int ret = ceu_bcast_dyns(cur->Bcast.Coros.first, evt);
                    cur->Bcast.Coros.open--;
                    if (cur->Bcast.Coros.open == 0) {
                        ceu_coros_cleanup(cur);
                    }
                    return ret;
                case CEU_VALUE_TRACK:
                    if (evt->type==CEU_VALUE_CORO && cur->Bcast.Track==evt->Dyn) {
                        cur->Bcast.Track = NULL; // tracked coro is terminating
                    }
                    return CEU_RET_RETURN;
                }
                default:
                    assert(0 && "bug found");
            }
            assert(0 && "bug found");
        }

        CEU_RET ceu_bcast_dyns (CEU_Dynamic* cur, CEU_Value* evt) {
            while (cur != NULL) {
                CEU_Dynamic* nxt = cur->Bcast.next; // take nxt before cur is/may-be freed
                if (ceu_bcast_dyn(cur,evt) == CEU_RET_THROW) {
                    return CEU_RET_THROW;
                }
                cur = nxt;
            }
            return CEU_RET_RETURN;
        }
    """ +
    """ // COROS
        void ceu_coros_destroy (CEU_Dynamic* coros, CEU_Dynamic* coro) {
            if (coro->Bcast.status == CEU_CORO_STATUS_DESTROYED) {
                return;
            }
            CEU_Dynamic* cur = coros->Bcast.Coros.first;
            if (cur == coro) {
                coros->Bcast.Coros.first = coro->Bcast.next;
            } else {
                CEU_Dynamic* prv = cur;
                while (prv != NULL) {
                    if (prv->Bcast.next == coro) {
                        break;
                    }
                    prv = prv->Bcast.next;
                }
                cur = prv->Bcast.next;
                prv->Bcast.next = coro->Bcast.next;
            }
            assert(cur == coro);
            cur->Bcast.status = CEU_CORO_STATUS_DESTROYED;
            if (ceu_bcasting == 0) {
                // pending bcast inside coro, let it free later
                free(coro->Bcast.Coro.frame->mem);
                free(coro->Bcast.Coro.frame);
                free(coro);
            } else {
                coro->next = ceu_bcast_tofree;
                ceu_bcast_tofree = coro;
            }
            coros->Bcast.Coros.cur--;
        }
        
        void ceu_coros_cleanup (CEU_Dynamic* coros) {
            CEU_Dynamic* cur = coros->Bcast.Coros.first;
            while (cur != NULL) {
                CEU_Dynamic* nxt = cur->Bcast.next;
                if (cur->Bcast.status >= CEU_CORO_STATUS_TERMINATED) {
                    //assert(0 && "OK");
                    ceu_coros_destroy(coros, cur);
                }
                cur = nxt;
            }
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
                case CEU_VALUE_FUNC:
                case CEU_VALUE_TASK:
                case CEU_VALUE_TUPLE:
                case CEU_VALUE_VECTOR:
                case CEU_VALUE_DICT:
                case CEU_VALUE_BCAST:
                case CEU_VALUE_CORO:
                case CEU_VALUE_COROS:
                case CEU_VALUE_TRACK:
                    return ceu_sizeof(CEU_Value, Dyn);
                default:
                    assert(0 && "bug found");
            }
        }
        
        void ceu_max_depth (CEU_Dynamic* dyn, int n, CEU_Value* childs) {
            // new dyn should have at least the maximum depth among its children
            CEU_Block* hld = NULL;
            int max = -1;
            for (int i=0; i<n; i++) {
                CEU_Value* cur = &childs[i];
                if (cur->type>CEU_VALUE_DYNAMIC && cur->Dyn->hold!=NULL) {
                    if (max < cur->Dyn->hold->depth) {
                        max = cur->Dyn->hold->depth;
                        hld = cur->Dyn->hold;
                    }
                }
            }
            if (hld != NULL) {
                dyn->hold = hld;
                dyn->next = hld->tofree;
                hld->tofree = dyn;
            }
        }
        
        CEU_RET ceu_vector_get (CEU_Dynamic* vec, int i) {
            if (i<0 || i>=vec->Vector.n) {
                CEU_THROW_MSG("\0 : index error : out of bounds");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            int sz = ceu_tag_to_size(vec->Vector.type);
            ceu_acc = (CEU_Value) { vec->Vector.type };
            memcpy(&ceu_acc.Number, vec->Vector.mem+i*sz, sz);
            return CEU_RET_RETURN;
        }
        
        void ceu_vector_set (CEU_Dynamic* vec, int i, CEU_Value v) {
            if (v.type == CEU_VALUE_NIL) {           // pop
                assert(i == vec->Vector.n-1);
                vec->Vector.n--;
            } else {
                if (i == 0) {
                    vec->Vector.type = v.type;
                } else {
                    assert(v.type == vec->Vector.type);
                }
                int sz = ceu_tag_to_size(vec->Vector.type);
                if (i == vec->Vector.n) {           // push
                    if (i == vec->Vector.max) {
                        vec->Vector.max = vec->Vector.max*2 + 1;    // +1 if max=0
                        vec->Vector.mem = realloc(vec->Vector.mem, vec->Vector.max*sz + 1);
                        assert(vec->Vector.mem != NULL);
                    }
                    vec->Vector.n++;
                    vec->Vector.mem[sz*vec->Vector.n] = '\0';
                } else {                            // set
                    assert(i < vec->Vector.n);
                }
                memcpy(vec->Vector.mem + i*sz, (char*)&v.Number, sz);
            }
        }
        
        CEU_RET ceu_next_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            CEU_Value NIL = (CEU_Value) { CEU_VALUE_NIL };
            assert(n==1 || n==2);
            CEU_Value* col = args[0];
            CEU_Value* key = (n == 1) ? &NIL : args[1];
            assert(col->type == CEU_VALUE_DICT);
            for (int i=0; i<col->Dyn->Dict.max; i++) {
                CEU_Value* args[] = { key, &(*col->Dyn->Dict.mem)[i][0] };
                assert(CEU_RET_RETURN == ceu_op_equals_equals_f(NULL, 2, args));
                if (ceu_acc.Bool) {
                    key = &NIL;
                } else if (key->type == CEU_VALUE_NIL) {
                    ceu_acc = (*col->Dyn->Dict.mem)[i][0];
                    return CEU_RET_RETURN;
                }
            }
            ceu_acc = NIL;
            return CEU_RET_RETURN;
        }        
        int ceu_dict_key_to_index (CEU_Dynamic* col, CEU_Value* key, int* idx) {
            *idx = -1;
            for (int i=0; i<col->Dict.max; i++) {
                CEU_Value* cur = &(*col->Dict.mem)[i][0];
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
        CEU_Value ceu_dict_get (CEU_Dynamic* col, CEU_Value* key) {
            int i;
            int ok = ceu_dict_key_to_index(col, key, &i);
            if (ok) {
                return (*col->Dict.mem)[i][1];
            } else {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
        }        
        CEU_RET ceu_dict_set (CEU_Dynamic* col, CEU_Value* key, CEU_Value* val) {
            assert(key->type != CEU_VALUE_NIL);     // TODO
            int old;
            ceu_dict_key_to_index(col, key, &old);
            if (old == -1) {
                old = col->Dict.max;
                int new = old * 2;
                col->Dict.max = new;
                col->Dict.mem = realloc(col->Dict.mem, new*2*sizeof(CEU_Value));
                assert(col->Dict.mem != NULL);
                memset(&(*col->Dict.mem)[old], 0, old*2*sizeof(CEU_Value));  // x[i]=nil
            }
            assert(old != -1);
            
            if (val->type == CEU_VALUE_NIL) {
                (*col->Dict.mem)[old][0] = (CEU_Value) { CEU_VALUE_NIL };
            } else {
                (*col->Dict.mem)[old][0] = *key;
                (*col->Dict.mem)[old][1] = *val;
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
                if (col->type==CEU_VALUE_TUPLE && (idx->Number<0 || idx->Number>=col->Dyn->Tuple.n)) {                
                    CEU_THROW_MSG("\0 : index error : out of bounds");
                    CEU_THROW_RET(CEU_ERR_ERROR);
                }
                if (col->type==CEU_VALUE_VECTOR && (idx->Number<0 || idx->Number>col->Dyn->Vector.n)) {                
                    CEU_THROW_MSG("\0 : index error : out of bounds");
                    CEU_THROW_RET(CEU_ERR_ERROR); // accepts v[#v]
                }
            }
            return CEU_RET_RETURN;
        }
    """ +
    """ // CREATES
        CEU_Dynamic* ceu_proto_create (CEU_Block* hld, int type, CEU_Frame* frame, CEU_Proto_F f, int awakes, int n) {
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic));
            assert(ret != NULL);
            *ret = (CEU_Dynamic) {
                type, NULL, NULL, NULL, 0, {
                    .Proto = { frame, f, {.Task={awakes,n}} }
                }
            };
            assert(CEU_RET_RETURN == ceu_block_set(hld, ret, 1));  // 1=cannot escape this block b/c of upvalues
            return ret;
        }
        
        CEU_Dynamic* ceu_tuple_create (CEU_Block* hld, int n, CEU_Value* args) {
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic) + n*sizeof(CEU_Value));
            assert(ret != NULL);
            *ret = (CEU_Dynamic) { CEU_VALUE_TUPLE, NULL, NULL, NULL, 0, {.Tuple={n,{}}} };
            if (args != NULL) {
                memcpy(ret->Tuple.mem, args, n*sizeof(CEU_Value));
                ceu_max_depth(ret, n, args);
            }
            assert(CEU_RET_RETURN == ceu_block_set(hld, ret, 0));
            return ret;
        }
        
        CEU_Dynamic* ceu_vector_create (CEU_Block* hld, CEU_VALUE type, int n, CEU_Value* args) {
            int sz = ceu_tag_to_size(type);
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic));
            assert(ret != NULL);
            char* mem = malloc(n*sz + 1);  // +1 '\0'
            assert(mem != NULL);
            *ret = (CEU_Dynamic) { CEU_VALUE_VECTOR, NULL, NULL, NULL, 0, {.Vector={n,n,type,mem}} };
            ceu_max_depth(ret, n, args);
            for (int i=0; i<n; i++) {
                assert(args[i].type == type);
                memcpy(ret->Vector.mem + i*sz, (char*)&args[i].Number, sz);
            }
            ret->Vector.mem[n*sz] = '\0';
            assert(CEU_RET_RETURN == ceu_block_set(hld, ret, 0));
            return ret;
        }
        
        CEU_Dynamic* ceu_dict_create (CEU_Block* hld, int n, CEU_Value (*args)[][2]) {
            int min = (n < 4) ? 4 : n; 
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic));
            assert(ret != NULL);
            CEU_Value (*mem)[][2] = malloc(min*2*sizeof(CEU_Value));
            assert(mem != NULL);
            memset(mem, 0, min*2*sizeof(CEU_Value));  // x[i]=nil
            *ret = (CEU_Dynamic) { CEU_VALUE_DICT, NULL, NULL, NULL, 0, {.Dict={min,mem}} };
            if (args != NULL) {
                for (int i=0; i<n; i++) {
                    ceu_dict_set(ret, &(*args)[i][0], &(*args)[i][1]);
                }
                ceu_max_depth(ret, n*2, (CEU_Value*)args);
            }
            assert(CEU_RET_RETURN == ceu_block_set(hld, ret, 0));
            return ret;
        }
        
        CEU_RET ceu_coros_create (CEU_Block* hld, int max, CEU_Value* ret) {
            CEU_Dynamic* coros = malloc(sizeof(CEU_Dynamic));
            assert(coros != NULL);
            *coros = (CEU_Dynamic) {
                CEU_VALUE_COROS, NULL, NULL, NULL, 0, {
                    .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                        .Coros = { max, 0, 0, NULL}
                    } }
                }
            };            
            *ret = (CEU_Value) { CEU_VALUE_COROS, {.Dyn=coros} };
            
            // hld is the enclosing block of "coroutines()", not of T
            // T would be the outermost possible scope, but we use hld b/c
            // we cannot express otherwise
            
            assert(CEU_RET_RETURN == ceu_block_set(hld, coros, 1));  // 1=cannot escape this block b/c of tasks

            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_coro_create (CEU_Block* hld, CEU_Value* task, CEU_Value* ret) {
            if (task->type != CEU_VALUE_TASK) {
                CEU_THROW_MSG("\0 : coroutine error : expected task");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            
            CEU_Dynamic* coro = malloc(sizeof(CEU_Dynamic));
            assert(coro != NULL);
            CEU_Frame* frame = malloc(sizeof(CEU_Frame));
            assert(frame != NULL);
            char* mem = malloc(task->Dyn->Proto.Task.n);
            assert(mem != NULL);
            
            *coro = (CEU_Dynamic) {
                CEU_VALUE_CORO, NULL, NULL, NULL, 0, {
                    .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                        .Coro = { NULL, NULL, frame }
                    } }
                }
            };
            *frame = (CEU_Frame) { &task->Dyn->Proto, hld, mem, {
                .Task = { coro, 0, { CEU_VALUE_NIL } }
            } };
            *ret = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=coro} };
            
            // hld is the enclosing block of "coroutine T", not of T
            // T would be the outermost possible scope, but we use hld b/c
            // we cannot express otherwise
            
            assert(CEU_RET_RETURN == ceu_block_set(hld, coro, 1));  // 1=cannot escape this block b/c of upvalues

            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_coro_create_in (CEU_Block* hld, CEU_Dynamic* coros, CEU_Value* task, CEU_Value* ret, int* ok) {
            if (coros->type != CEU_VALUE_COROS) {
                CEU_THROW_MSG("\0 : coroutine error : expected coroutines");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            if (coros->Bcast.Coros.max!=0 && coros->Bcast.Coros.cur==coros->Bcast.Coros.max) {
                *ok = 0;
                return CEU_RET_RETURN;
            }
            if (task->type != CEU_VALUE_TASK) {
                CEU_THROW_MSG("\0 : coroutine error : expected task");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            
            CEU_Dynamic* coro = malloc(sizeof(CEU_Dynamic));
            assert(coro != NULL);
            CEU_Frame* frame = malloc(sizeof(CEU_Frame));
            assert(frame != NULL);
            char* mem = malloc(task->Dyn->Proto.Task.n);
            assert(mem != NULL);
        
            *coro = (CEU_Dynamic) {
                CEU_VALUE_CORO, NULL, coros->hold, NULL, 1, { // no free
                    .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                        .Coro = { coros, NULL, frame }
                    } }
                }
            };
            *frame = (CEU_Frame) { &task->Dyn->Proto, hld, mem, {
                .Task = { coro, 0, { CEU_VALUE_NIL } }
            } };
            *ret = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=coro} };
            
            ceu_bcast_enqueue(&coros->Bcast.Coros.first, coro);
            coros->Bcast.Coros.cur++;
            return CEU_RET_RETURN;
        }
        
        CEU_Dynamic* ceu_track_create (CEU_Dynamic* coro, CEU_Value* ret) {
            CEU_Dynamic* trk = malloc(sizeof(CEU_Dynamic));
            assert(trk != NULL);
            *trk = (CEU_Dynamic) {
                CEU_VALUE_TRACK, NULL, NULL, NULL, 0, {
                    .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                        .Track = coro
                    } }
                }
            };
            // at most coro->hld, same as pointer coro/coros, term bcast is limited to it
            CEU_Block* hld = (coro->Bcast.Coro.coros == NULL) ? coro->hold : coro->Bcast.Coro.coros->hold;
            assert(CEU_RET_RETURN == ceu_block_set(hld, trk, 1));
            *ret = (CEU_Value) { CEU_VALUE_TRACK, {.Dyn=trk} };
            return NULL;
        }
        
        CEU_Value ceu_track_to_coro (CEU_Value* track) {
            if (track->type == CEU_VALUE_TRACK) {
                if (track->Dyn->Bcast.Track == NULL) {
                    return (CEU_Value) { CEU_VALUE_NIL };
                } else {
                    return (CEU_Value) { CEU_VALUE_CORO, {.Dyn=track->Dyn->Bcast.Track} };
                }
            } else {
                return *track;
            }
        }
    """ +
    """ // PRINT
        void ceu_print1 (CEU_Value* v) {
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
                    for (int i=0; i<v->Dyn->Tuple.n; i++) {
                        if (i > 0) {
                            printf(",");
                        }
                        ceu_print1(&v->Dyn->Tuple.mem[i]);
                    }                    
                    printf("]");
                    break;
                case CEU_VALUE_VECTOR:
                    if (v->Dyn->Vector.type == CEU_VALUE_CHAR) {
                        printf("%s", v->Dyn->Vector.mem);
                    } else {
                        printf("#[");
                        for (int i=0; i<v->Dyn->Vector.n; i++) {
                            if (i > 0) {
                                printf(",");
                            }
                            assert(CEU_RET_RETURN == ceu_vector_get(v->Dyn, i));
                            CEU_Value ceu_accx = ceu_acc;
                            ceu_print1(&ceu_accx);
                        }                    
                        printf("]");
                    }
                    break;
                case CEU_VALUE_DICT:
                    printf("@[");
                    int comma = 0;
                    for (int i=0; i<v->Dyn->Dict.max; i++) {
                        if ((*v->Dyn->Dict.mem)[i][0].type != CEU_VALUE_NIL) {
                            if (comma != 0) {
                                printf(",");
                            }
                            comma = 1;
                            printf("(");
                            ceu_print1(&(*v->Dyn->Dict.mem)[i][0]);
                            printf(",");
                            ceu_print1(&(*v->Dyn->Dict.mem)[i][1]);
                            printf(")");
                        }
                    }                    
                    printf("]");
                    break;
                case CEU_VALUE_FUNC:
                    printf("func: %p", v->Dyn);
                    break;
                case CEU_VALUE_TASK:
                    printf("task: %p", v->Dyn);
                    break;
                case CEU_VALUE_CORO:
                    printf("coro: %p", v->Dyn);
                    break;
                case CEU_VALUE_COROS:
                    printf("coros: %p", v->Dyn);
                    break;
                case CEU_VALUE_TRACK:
                    printf("track: %p", v->Dyn);
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
                ceu_print1(args[i]);
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_println_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(CEU_RET_RETURN == ceu_print_f(frame, n, args));
            printf("\n");
            ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
            return CEU_RET_RETURN;
        }
    """ +
    """
        // EQ-NEQ-LEN-COPY
        CEU_RET ceu_op_equals_equals_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
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
                        v = (e1->Dyn == e2->Dyn);
                        if (v) {
                            // OK
                        } else {
                            v = (e1->Dyn->Tuple.n==e2->Dyn->Tuple.n);
                            if (v) {
                                for (int i=0; i<e1->Dyn->Tuple.n; i++) {
                                    CEU_Value* xs[] = { &e1->Dyn->Tuple.mem[i], &e2->Dyn->Tuple.mem[i] };
                                    assert(CEU_RET_RETURN == ceu_op_equals_equals_f(frame, 2, xs));
                                    if (!ceu_acc.Bool) {
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case CEU_VALUE_VECTOR:
                    case CEU_VALUE_DICT:
                    case CEU_VALUE_FUNC:
                    case CEU_VALUE_TASK:
                    case CEU_VALUE_CORO:
                    case CEU_VALUE_COROS:
                        v = (e1->Dyn == e2->Dyn);
                        break;
                    default:
                        assert(0 && "bug found");
                }
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=v} };
            return CEU_RET_RETURN;
        }
        CEU_RET ceu_op_slash_equals_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(CEU_RET_RETURN == ceu_op_equals_equals_f(frame, n, args));
            ceu_acc.Bool = !ceu_acc.Bool;
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_op_hash_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            assert(n == 1);
            if (args[0]->type != CEU_VALUE_VECTOR) {
                CEU_THROW_MSG("\0 : length error : not a vector");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=args[0]->Dyn->Vector.n} };
            return CEU_RET_RETURN;
        }
        
        CEU_RET ceu_move_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* src = args[0];
            CEU_Dynamic* dyn = src->Dyn;
            switch (src->type) {
                case CEU_VALUE_TUPLE: {
                    dyn->isperm = 0;
                    for (int i=0; i<dyn->Tuple.n; i++) {
                        CEU_Value* args[1] = { &dyn->Tuple.mem[i] };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, 1, args));
                    }
                    ceu_acc = *src;
                    break;
                }
                case CEU_VALUE_VECTOR: {
                    dyn->isperm = 0;
                    for (int i=0; i<dyn->Vector.n; i++) {
                        assert(CEU_RET_RETURN == ceu_vector_get(dyn, i));
                        CEU_Value ceu_accx = ceu_acc;
                        CEU_Value* args[1] = { &ceu_accx };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, 1, args));
                    }
                    ceu_acc = *src;
                    break;
                }
                case CEU_VALUE_DICT: {
                    dyn->isperm = 0;
                    for (int i=0; i<dyn->Dict.max; i++) {
                        CEU_Value* args0[1] = { &(*dyn->Dict.mem)[i][0] };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, 1, args0));
                        CEU_Value* args1[1] = { &(*dyn->Dict.mem)[i][1] };
                        assert(CEU_RET_RETURN == ceu_move_f(frame, 1, args1));
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
        CEU_RET ceu_copy_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* src = args[0];
            CEU_Dynamic* old = src->Dyn;
            switch (src->type) {
                case CEU_VALUE_TUPLE: {
                    CEU_Value args1[old->Tuple.n];
                    for (int i=0; i<old->Tuple.n; i++) {
                        CEU_Value* args2[1] = { &old->Tuple.mem[i] };
                        assert(CEU_RET_RETURN == ceu_copy_f(frame, 1, args2));
                        args1[i] = ceu_acc;
                    }
                    CEU_Dynamic* new = ceu_tuple_create(old->hold, old->Tuple.n, args1);
                    assert(new != NULL);
                    ceu_acc = (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=new} };
                    break;
                }
                case CEU_VALUE_VECTOR: {
                    CEU_Dynamic* new = ceu_vector_create(old->hold, old->Vector.type, 0, NULL);
                    assert(new != NULL);
                    CEU_Value ret = { CEU_VALUE_VECTOR, {.Dyn=new} };
                    // TODO: memcpy if type!=DYN
                    for (int i=0; i<old->Vector.n; i++) {
                        assert(CEU_RET_RETURN == ceu_vector_get(old, i));
                        CEU_Value ceu_accx = ceu_acc;
                        CEU_Value* args[1] = { &ceu_accx };
                        assert(CEU_RET_RETURN == ceu_copy_f(frame, 1, args));
                        ceu_vector_set(new, i, ceu_acc);
                    }
                    ceu_acc = ret;
                    break;
                }
                case CEU_VALUE_DICT: {
                    CEU_Dynamic* new = ceu_dict_create(old->hold, old->Dict.max, NULL);
                    assert(new != NULL);
                    CEU_Value ret = { CEU_VALUE_DICT, {.Dyn=new} };
                    for (int i=0; i<old->Dict.max; i++) {
                        CEU_Value* args0[1] = { &(*old->Dict.mem)[i][0] };
                        assert(CEU_RET_RETURN == ceu_copy_f(frame, 1, args0));
                        (*ret.Dyn->Dict.mem)[i][0] = ceu_acc;
                        CEU_Value* args1[1] = { &(*old->Dict.mem)[i][1] };
                        assert(CEU_RET_RETURN == ceu_copy_f(frame, 1, args1));
                        (*ret.Dyn->Dict.mem)[i][1] = ceu_acc;
                    }
                    ceu_acc = ret;
                    break;
                }
                default:
                    ceu_acc = *src;
                    break;
            }
            return CEU_RET_RETURN;
        }
    """ +
    """ // FUNCS
        typedef struct {
            ${GLOBALS.map { "CEU_Value $it;\n" }.joinToString("")}
            ${this.mem}
        } CEU_Proto_Mem_${this.outer.n};
        CEU_Proto_Mem_${this.outer.n} _ceu_mem_;
        CEU_Proto_Mem_${this.outer.n}* ceu_mem = &_ceu_mem_;
        CEU_Proto_Mem_${this.outer.n}* ceu_mem_${this.outer.n} = &_ceu_mem_;
        //CEU_Proto _ceu_proto_ = { NULL, {}, {} };
        CEU_Frame _ceu_frame_ = { NULL, 0, (char*) &_ceu_mem_ };
        CEU_Frame* ceu_frame = &_ceu_frame_;
        ${tops.first.joinToString("")}
        ${tops.second.joinToString("")}
        ${tops.third.joinToString("")}
    """ +
    """ // MAIN
        int main (void) {
            ${this.tags.map { "CEU_TAG_INIT(${it.second},\"${it.first}\")\n" }.joinToString("")}
            assert(CEU_TAG_nil == CEU_VALUE_NIL);
            do {
                {
                    static CEU_Dynamic ceu_type = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_type_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_tags = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_tags_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_print = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_print_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_println = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_println_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_op_equals_equals = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_op_equals_equals_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_op_slash_equals = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_op_slash_equals_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_op_hash = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_op_hash_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_move = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_move_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_copy = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_copy_f, {{0}} }
                        }
                    };
                    static CEU_Dynamic ceu_next = { 
                        CEU_VALUE_FUNC, NULL, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_next_f, {{0}} }
                        }
                    };
                    ceu_mem->type    = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_type}    };
                    ceu_mem->tags    = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_tags}    };
                    ceu_mem->print   = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_print}   };
                    ceu_mem->println = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_println} };            
                    ceu_mem->op_hash = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_op_hash} };
                    ceu_mem->move    = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_move}    };
                    ceu_mem->copy    = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_copy}    };
                    ceu_mem->next    = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_next}    };
                    ceu_mem->op_equals_equals = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_op_equals_equals} };
                    ceu_mem->op_slash_equals  = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_op_slash_equals}  };
                }
                ${this.code}
                return 0;
            } while (0);
            {
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
                ceu_print1(&ceu_accx);
                puts("");
            }
            return 1;
        }
    """)
}
