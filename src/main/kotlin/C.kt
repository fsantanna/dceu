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

        struct CEU_Value;
            struct CEU_Dynamic;
            struct CEU_Frame;        
            struct CEU_Block;
            struct CEU_Proto;
        struct CEU_Tags;
        struct CEU_Block;
            
        int ceu_as_bool (struct CEU_Value* v);
        int ceu_has_throw_clear (void);
                
        #define CEU_TAG_DEFINE(id,str)              \
            const int CEU_TAG_##id = __COUNTER__;   \
            CEU_Tags ceu_tag_##id = { str, NULL };
        #define CEU_TAG_INIT(id,str)                \
            ceu_tag_##id.next = CEU_TAGS;           \
            CEU_TAGS = &ceu_tag_##id;               \
            CEU_TAGS_MAX++;            
        struct CEU_Value ceu_tags_f (struct CEU_Frame* _2, int n, struct CEU_Value* args[]);
        char* ceu_tag_to_string (int tag);
        
        void ceu_block_free (struct CEU_Block* block);
        char* ceu_block_set (struct CEU_Block* dst, struct CEU_Dynamic* src, int isperm);
        
        void  ceu_coros_cleanup (struct CEU_Dynamic* coros);
        void  ceu_coros_destroy (struct CEU_Dynamic* coros, struct CEU_Dynamic* coro);
        char* ceu_coros_create  (struct CEU_Block* up, int max, struct CEU_Value* ret); 
        char* ceu_coro_create   (struct CEU_Value* task, struct CEU_Block* up, struct CEU_Value* ret);
        char* ceu_coro_create_in  (int* ok, struct CEU_Dynamic* coros, struct CEU_Value* task, struct CEU_Block* up, struct CEU_Value* ret);
        
        void ceu_bcast_enqueue (struct CEU_Dynamic** outer, struct CEU_Dynamic* dyn);
        void ceu_bcast_dequeue (struct CEU_Dynamic** outer, struct CEU_Dynamic* dyn);
        void ceu_bcast_dyns    (struct CEU_Dynamic* cur);
        void ceu_bcast_blocks  (struct CEU_Block* cur, struct CEU_Value* evt);
        void ceu_bcast_dyn     (struct CEU_Dynamic* cur, struct CEU_Value* evt);
        
        void  ceu_max_depth        (struct CEU_Dynamic* dyn, int n, struct CEU_Value* childs);
        int   ceu_dict_key_index   (struct CEU_Dynamic* col, struct CEU_Value* key);
        int   ceu_dict_empty_index (struct CEU_Dynamic* col);
        char* ceu_col_check        (struct CEU_Value* col, struct CEU_Value* idx);
        struct CEU_Dynamic* ceu_tuple_create (struct CEU_Block* hld, int n, struct CEU_Value* args);
        
        struct CEU_Dynamic* ceu_track_create (struct CEU_Dynamic* coro, struct CEU_Value* ret);
        struct CEU_Value ceu_track_to_coro (struct CEU_Value* track);
        
        struct CEU_Value ceu_op_eq_eq_f (struct CEU_Frame* frame, int n, struct CEU_Value* args[]);
    """ +
    """ // CEU_Value
        typedef enum CEU_VALUE {
            CEU_VALUE_NIL = 0,
            CEU_VALUE_TAG,
            CEU_VALUE_BOOL,
            CEU_VALUE_NUMBER,
            CEU_VALUE_POINTER,
            CEU_VALUE_DYNAMIC,  // all below are dynamic
            CEU_VALUE_FUNC,     // func frame
            CEU_VALUE_TASK,     // task frame
            CEU_VALUE_TUPLE,
            CEU_VALUE_DICT,
            CEU_VALUE_BCAST,    // all below are bcast
            CEU_VALUE_CORO,     // spawned task
            CEU_VALUE_COROS,    // pool of spawned tasks
            CEU_VALUE_TRACK
        } CEU_VALUE;
        
        typedef enum CEU_CORO_STATUS {
            CEU_CORO_STATUS_RESUMED = 0,
            CEU_CORO_STATUS_YIELDED,
            CEU_CORO_STATUS_TOGGLED,
            CEU_CORO_STATUS_TERMINATED
        } CEU_CORO_STATUS;        

        typedef struct CEU_Value {
            CEU_VALUE tag;
            union {
                //void nil;
                int Tag;
                int Bool;
                double Number;
                void* Pointer;
                struct CEU_Dynamic* Dyn;    // Func/Task/Tuple/Dict/Coro/Coros: allocates memory
            };
        } CEU_Value;
    """ +
    """ // CEU_Proto / CEU_Frame
        typedef struct CEU_Value (*CEU_Proto_F) (
            struct CEU_Frame* frame,
            int n,
            struct CEU_Value* args[]
        );
        
        typedef struct CEU_Proto {  // lexical func/task
            struct CEU_Frame* up;   // points to active frame
            CEU_Proto_F f;
            union {
                struct {
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
            CEU_VALUE tag;                  // required to switch over free/bcast
            struct CEU_Dynamic* next;       // next dyn to free (not used by coro in coros)
            struct CEU_Block*   hold;       // holding block to compare on set/move
            int isperm;                     // if hold is permanent and may not be reset to outer block
            union {
                CEU_Proto Proto;
                struct {
                    int n;                  // number of items
                    CEU_Value mem[0];       // beginning of CEU_Value[n]
                } Tuple;
                struct {
                    int n;                  // size of mem
                    CEU_Value (*mem)[0][2]; // beginning of CEU_Value[n][2]
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
                            uint8_t max;                // max number of instances
                            uint8_t cur;                // cur number of instances
                            uint8_t open;               // number of open iterators
                            struct CEU_Dynamic* first;  // coro->Bcast.Coro, first coro to bcast/free
                        } Coros;
                        struct {
                            struct CEU_Dynamic* coro;   // starts as CORO and may fall to NIL
                        } Track;
                    };
                } Bcast;
            };
        } CEU_Dynamic;
    """ +
    """ // CEU_Block
        typedef struct CEU_Block {
            int depth;                      // compare on set
            CEU_Dynamic* tofree;                // list of allocated data to free on exit
            struct {
                struct CEU_Block* block;        // nested block active
                struct CEU_Dynamic* dyn;        // first coro/coros in this block
            } bcast;
        } CEU_Block;
    """ +
    """ // CEU_Tags
        typedef struct CEU_Tags {
            char* name;
            struct CEU_Tags* next;
        } CEU_Tags;        
    """ +
    """ // GLOBALS
        struct CEU_Dynamic* ceu_proto_create (struct CEU_Block* hld, int tag, struct CEU_Frame* frame, CEU_Proto_F f, int n);        
        struct CEU_Dynamic* ceu_dict_create  (struct CEU_Block* hld, int n, struct CEU_Value (*args)[][2]);

        static CEU_Tags* CEU_TAGS = NULL;
        int CEU_TAGS_MAX = 0;
        ${this.tags.map { "CEU_TAG_DEFINE($it,\":$it\")\n" }.joinToString("")}

        int ceu_has_throw = 0;
        const CEU_Value CEU_ERR_ERROR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_error} };
        CEU_Value ceu_err;
        char ceu_err_error_msg[256];
        #define ceu_throw(v) { ceu_has_throw=1; ceu_err=v; ceu_acc=(CEU_Value){CEU_VALUE_NIL}; }
        
            //  - can pass further
            //  - cannot pass back
            //  - each catch condition:
            //      - must set its depth at the beginning 
            //      - must not yield
            //      - must deallocate at the end

        int ceu_has_bcast = 0;
        CEU_Value CEU_EVT_NIL = { CEU_VALUE_NIL }; 
        CEU_Value CEU_EVT_CLEAR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_clear} };
        CEU_Value* ceu_evt = &CEU_EVT_NIL;
            // set initial b/c of ceu_bcast_pre/pos
            // must be a pointer b/c it is mutable b/w bcast/yield
        
        CEU_Value ceu_acc;
    """ +
    """ // IMPLS
        int ceu_as_bool (CEU_Value* v) {
            return !(v->tag==CEU_VALUE_NIL || (v->tag==CEU_VALUE_BOOL && !v->Bool));
        }
        CEU_Value ceu_tags_f (CEU_Frame* _2, int n, CEU_Value* args[]) {
            assert(n == 1 && "bug found");
            return (CEU_Value) { CEU_VALUE_TAG, {.Tag=args[0]->tag} };
        }
        char* ceu_tag_to_string (int tag) {
            CEU_Tags* cur = CEU_TAGS;
            for (int i=0; i<CEU_TAGS_MAX-tag-1; i++) {
                cur = cur->next;
            }
            return cur->name;
        }
        int ceu_has_throw_clear (void) {
            return (ceu_has_throw > 0) || (ceu_has_bcast>0 && ceu_evt==&CEU_EVT_CLEAR);
        }
    """ +
    """ // BLOCK
        void ceu_block_free (CEU_Block* block) {
            while (block->tofree != NULL) {
                CEU_Dynamic* cur = block->tofree;
                switch (cur->tag) {
                    case CEU_VALUE_DICT:
                        free(cur->Dict.mem);
                        break;
                    case CEU_VALUE_CORO:
                        free(cur->Bcast.Coro.frame->mem);
                        free(cur->Bcast.Coro.frame);
                        break;
                }
                block->tofree = block->tofree->next;
                free(cur);
            }
        }
        
        char* ceu_block_set (CEU_Block* dst, CEU_Dynamic* src, int isperm) {
            switch (src->tag) {
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<src->Tuple.n; i++) {
                        if (src->Tuple.mem[i].tag > CEU_VALUE_DYNAMIC) {
                            char* err = ceu_block_set(dst, src->Tuple.mem[i].Dyn, isperm);
                            if (err != NULL) {
                                return err;
                            }
                        }
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<src->Dict.n; i++) {
                        if (src->Dict.mem[i][0]->tag > CEU_VALUE_DYNAMIC) {
                            char* err = ceu_block_set(dst, (*src->Dict.mem)[i][0].Dyn, isperm);
                            if (err != NULL) {
                                return err;
                            }
                        }
                        if (src->Dict.mem[i][1]->tag > CEU_VALUE_DYNAMIC) {
                            char* err = ceu_block_set(dst, (*src->Dict.mem)[i][1].Dyn, isperm);
                            if (err != NULL) {
                                return err;
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
                if (src->tag==CEU_VALUE_FUNC && src->Proto.up==NULL) {
                    // do not enqueue: global functions use up=NULL and are not malloc'ed
                } else {
                    { // remove from old block
                        if (src->hold != NULL) {
                            CEU_Block* old = src->hold;
                            if (src->tag > CEU_VALUE_BCAST) {
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
                        if (src->tag > CEU_VALUE_BCAST) {
                            ceu_bcast_enqueue(&dst->bcast.dyn, src);
                        }
                    }
                }
                src->hold = dst;
                src->isperm = src->isperm || isperm;
            } else if (src->hold->depth > dst->depth) {
                ceu_throw(CEU_ERR_ERROR);
                return "set error : incompatible scopes";
            } else {
                src->isperm = src->isperm || isperm;
            }
            return NULL;
        }
    """ +
    """ // BCAST - ENQUEUE - DEQUEUE
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
        #define CEU_BCAST_BREAK() { if (ceu_has_throw==1 && ceu_evt!=&CEU_EVT_CLEAR) { return; } }
        void ceu_bcast_pre (CEU_Value** prv, CEU_Value* evt) {
            *prv = ceu_evt;
            ceu_has_bcast++;
            ceu_evt = evt;
        }
        void ceu_bcast_pos (CEU_Value** prv, CEU_Value* evt) {
            ceu_has_bcast--;
            ceu_evt = *prv;
        }
        void ceu_bcast_blocks_aux (CEU_Block* cur) {
            while (cur != NULL) {
                CEU_Dynamic* dyn = cur->bcast.dyn;
                if (dyn != NULL) {
                    ceu_bcast_dyns(dyn);
                    CEU_BCAST_BREAK();
                }
                cur = cur->bcast.block;
            }
        }
        void ceu_bcast_blocks (CEU_Block* cur, CEU_Value* evt) {
            CEU_Value* prv;
            ceu_bcast_pre(&prv, evt);
            ceu_bcast_blocks_aux(cur);
            ceu_bcast_pos(&prv, evt);
        }
    """ +
    """ // BCAST_DYN
        void ceu_bcast_dyn_aux (CEU_Dynamic* cur) {
            if (cur->Bcast.status == CEU_CORO_STATUS_TERMINATED) {
                // do not awake terminated/running coro
                return;
            }
            if (cur->Bcast.status==CEU_CORO_STATUS_TOGGLED && ceu_evt!=&CEU_EVT_CLEAR) {
                // do not awake toggled coro, unless it is a CLEAR event
                return;
            }
            switch (cur->tag) {
                case CEU_VALUE_CORO: {
                    ceu_bcast_blocks_aux(cur->Bcast.Coro.block);
                    CEU_BCAST_BREAK();
                    if (cur->Bcast.status != CEU_CORO_STATUS_RESUMED) { // on resume, only awake blocks
                        CEU_Value arg = { CEU_VALUE_NIL };
                        CEU_Value* args[] = { &arg };
                        cur->Bcast.Coro.frame->proto->f(cur->Bcast.Coro.frame, 1, args);
                        CEU_BCAST_BREAK();
                    }
                    break;
                }
                case CEU_VALUE_COROS: {
                    cur->Bcast.Coros.open++;
                    ceu_bcast_dyns(cur->Bcast.Coros.first);
                    cur->Bcast.Coros.open--;
                    if (cur->Bcast.Coros.open == 0) {
                        ceu_coros_cleanup(cur);
                    }
                    CEU_BCAST_BREAK();
                    break;
                case CEU_VALUE_TRACK:
                    if (ceu_evt->tag==CEU_VALUE_POINTER && cur->Bcast.Track.coro==ceu_evt->Pointer) {
                        cur->Bcast.Track.coro = NULL; // tracked coro is terminating
                    }
                    break;
                }
            }
        }

        void ceu_bcast_dyn (CEU_Dynamic* cur, CEU_Value* evt) {
            CEU_Value* prv;
            ceu_bcast_pre(&prv, evt);
            ceu_bcast_dyn_aux(cur);
            ceu_bcast_pos(&prv, evt);
        }
        
        void ceu_bcast_dyns (CEU_Dynamic* cur) {
            while (cur != NULL) {
                CEU_Dynamic* nxt = cur->Bcast.next; // take nxt before cur is/may-be freed
                ceu_bcast_dyn_aux(cur);
                CEU_BCAST_BREAK();
                cur = nxt;
            }
        }
    """ +
    """ // COROS
        void ceu_coros_destroy (CEU_Dynamic* coros, CEU_Dynamic* coro) {
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
            free(coro->Bcast.Coro.frame->mem);
            free(coro->Bcast.Coro.frame);
            free(coro);
            coros->Bcast.Coros.cur--;
        }
        
        void ceu_coros_cleanup (CEU_Dynamic* coros) {
            CEU_Dynamic* cur = coros->Bcast.Coros.first;
            while (cur != NULL) {
                CEU_Dynamic* nxt = cur->Bcast.next;
                if (cur->Bcast.status == CEU_CORO_STATUS_TERMINATED) {
                    //assert(0 && "OK");
                    ceu_coros_destroy(coros, cur);
                }
                cur = nxt;
            }
        }
    """ +
    """ // TUPLE / DICT
        void ceu_max_depth (CEU_Dynamic* dyn, int n, CEU_Value* childs) {
            // new dyn should have at least the maximum depth among its children
            CEU_Block* hld = NULL;
            int max = -1;
            for (int i=0; i<n; i++) {
                CEU_Value* cur = &childs[i];
                if (cur->tag>CEU_VALUE_DYNAMIC && cur->Dyn->hold!=NULL) {
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
        
        int ceu_dict_key_index (CEU_Dynamic* col, CEU_Value* key) {
            for (int i=0; i<col->Dict.n; i++) {
                CEU_Value* args[] = { key, &(*col->Dict.mem)[i][0] };
                if (ceu_op_eq_eq_f(NULL, 2, args).Bool) {
                    return i;
                }
            }
            return -1;
        }        
        int ceu_dict_empty_index (CEU_Dynamic* col) {
            for (int i=0; i<col->Dict.n; i++) {
                if ((*col->Dict.mem)[i][0].tag == CEU_VALUE_NIL) {
                    return i;
                }
            }
            int old = col->Dict.n;
            int new = old * 2;
            col->Dict.n = new;
            col->Dict.mem = realloc(col->Dict.mem, new*2*sizeof(CEU_Value));
            assert(col->Dict.mem != NULL);
            memset(&(*col->Dict.mem)[old], 0, old*2*sizeof(CEU_Value));  // x[i]=nil
            return old;
        }        
        
        char* ceu_col_check (CEU_Value* col, CEU_Value* idx) {
            if (col->tag!=CEU_VALUE_TUPLE && col->tag!=CEU_VALUE_DICT) {                
                ceu_throw(CEU_ERR_ERROR);
                return "index error : expected collection";
            }
            if (col->tag == CEU_VALUE_TUPLE) {
                if (idx->tag != CEU_VALUE_NUMBER) {
                    ceu_throw(CEU_ERR_ERROR);
                    return "index error : expected number";
                }
                if (col->Dyn->Tuple.n <= idx->Number) {                
                    ceu_throw(CEU_ERR_ERROR);
                    return "index error : out of bounds";
                }
            }
            return NULL;
        }
    """ +
    """ // CREATES
        CEU_Dynamic* ceu_proto_create (CEU_Block* hld, int tag, CEU_Frame* frame, CEU_Proto_F f, int n) {
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic));
            assert(ret != NULL);
            *ret = (CEU_Dynamic) {
                tag, NULL, NULL, 0, {
                    .Proto = { frame, f, {.Task={n}} }
                }
            };
            assert(NULL == ceu_block_set(hld, ret, 1));  // 1=cannot escape this block b/c of upvalues
            return ret;
        }
        
        CEU_Dynamic* ceu_tuple_create (CEU_Block* hld, int n, CEU_Value* args) {
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic) + n*sizeof(CEU_Value));
            assert(ret != NULL);
            *ret = (CEU_Dynamic) { CEU_VALUE_TUPLE, NULL, NULL, 0, {.Tuple={n,{}}} };
            memcpy(ret->Tuple.mem, args, n*sizeof(CEU_Value));
            ceu_max_depth(ret, n, args);
            assert(NULL == ceu_block_set(hld, ret, 0));
            return ret;
        }
        
        CEU_Dynamic* ceu_dict_create (CEU_Block* hld, int n, CEU_Value (*args)[][2]) {
            int min = (n < 4) ? 4 : n; 
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic));
            assert(ret != NULL);
            CEU_Value (*mem)[][2] = malloc(min*2*sizeof(CEU_Value));
            assert(mem != NULL);
            memset(mem, 0, min*2*sizeof(CEU_Value));  // x[i]=nil
            *ret = (CEU_Dynamic) { CEU_VALUE_DICT, NULL, NULL, 0, {.Dict={min,mem}} };
            memcpy(mem, args, n*2*sizeof(CEU_Value));
            ceu_max_depth(ret, n*2, (CEU_Value*)args);
            assert(NULL == ceu_block_set(hld, ret, 0));
            return ret;
        }
        
        char* ceu_coros_create (CEU_Block* up, int max, CEU_Value* ret) {
            CEU_Dynamic* coros = malloc(sizeof(CEU_Dynamic));
            assert(coros != NULL);
            *coros = (CEU_Dynamic) {
                CEU_VALUE_COROS, NULL, NULL, 0, {
                    .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                        .Coros = { max, 0, 0, NULL}
                    } }
                }
            };            
            *ret = (CEU_Value) { CEU_VALUE_COROS, {.Dyn=coros} };
            
            // up is the enclosing block of "coroutine T", not of T
            // T would be the outermost possible scope, but we use up b/c
            // we cannot express otherwise
            
            assert(NULL == ceu_block_set(up, coros, 1));  // 1=cannot escape this block b/c of tasks

            return NULL;
        }
        
        char* ceu_coro_create (CEU_Value* task, CEU_Block* up, CEU_Value* ret) {
            if (task->tag != CEU_VALUE_TASK) {
                return "coroutine error : expected task";
            }
            
            CEU_Dynamic* coro = malloc(sizeof(CEU_Dynamic));
            assert(coro != NULL);
            CEU_Frame* frame = malloc(sizeof(CEU_Frame));
            assert(frame != NULL);
            char* mem = malloc(task->Dyn->Proto.Task.n);
            assert(mem != NULL);
            
            *coro = (CEU_Dynamic) {
                CEU_VALUE_CORO, NULL, NULL, 0, {
                    .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                        .Coro = { NULL, NULL, frame }
                    } }
                }
            };
            *frame = (CEU_Frame) { &task->Dyn->Proto, up, mem, {
                .Task = { coro, 0, { CEU_VALUE_NIL } }
            } };
            *ret = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=coro} };
            
            // up is the enclosing block of "coroutine T", not of T
            // T would be the outermost possible scope, but we use up b/c
            // we cannot express otherwise
            
            assert(NULL == ceu_block_set(up, coro, 1));  // 1=cannot escape this block b/c of upvalues

            return NULL;
        }
        
        char* ceu_coro_create_in (int* ok, CEU_Dynamic* coros, CEU_Value* task, CEU_Block* up, CEU_Value* ret) {
            if (coros->tag != CEU_VALUE_COROS) {
                return "coroutine error : expected coroutines";
            }
            if (coros->Bcast.Coros.max!=0 && coros->Bcast.Coros.cur==coros->Bcast.Coros.max) {
                *ok = 0;
                return NULL;
            }
            if (task->tag != CEU_VALUE_TASK) {
                return "coroutine error : expected task";
            }
            
            CEU_Dynamic* coro = malloc(sizeof(CEU_Dynamic));
            assert(coro != NULL);
            CEU_Frame* frame = malloc(sizeof(CEU_Frame));
            assert(frame != NULL);
            char* mem = malloc(task->Dyn->Proto.Task.n);
            assert(mem != NULL);
        
            *coro = (CEU_Dynamic) {
                CEU_VALUE_CORO, NULL, coros->hold, 1, { // no free
                    .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                        .Coro = { coros, NULL, frame }
                    } }
                }
            };
            *frame = (CEU_Frame) { &task->Dyn->Proto, up, mem, {
                .Task = { coro, 0, { CEU_VALUE_NIL } }
            } };
            *ret = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=coro} };
            
            ceu_bcast_enqueue(&coros->Bcast.Coros.first, coro);
            coros->Bcast.Coros.cur++;
            return NULL;
        }
        
        CEU_Dynamic* ceu_track_create (CEU_Dynamic* coro, CEU_Value* ret) {
            CEU_Dynamic* trk = malloc(sizeof(CEU_Dynamic));
            assert(trk != NULL);
            *trk = (CEU_Dynamic) {
                CEU_VALUE_TRACK, NULL, NULL, 0, {
                    .Bcast = { CEU_CORO_STATUS_YIELDED, NULL, {
                        .Track = coro
                    } }
                }
            };
            // at most coro->hld, same as pointer coro/coros, term bcast is limited to it
            CEU_Block* hld = (coro->Bcast.Coro.coros == NULL) ? coro->hold : coro->Bcast.Coro.coros->hold;
            assert(NULL == ceu_block_set(hld, trk, 1));
            *ret = (CEU_Value) { CEU_VALUE_TRACK, {.Dyn=trk} };
            return NULL;
        }
        
        CEU_Value ceu_track_to_coro (CEU_Value* track) {
            if (track->tag == CEU_VALUE_TRACK) {
                if (track->Dyn->Bcast.Track.coro == NULL) {
                    return (CEU_Value) { CEU_VALUE_NIL };
                } else {
                    return (CEU_Value) { CEU_VALUE_CORO, {.Dyn=track->Dyn->Bcast.Track.coro} };
                }
            } else {
                return *track;
            }
        }
    """ +
    """ // PRINT
        void ceu_print1 (CEU_Value* v) {
            switch (v->tag) {
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
                case CEU_VALUE_DICT:
                    printf("@[");
                    int comma = 0;
                    for (int i=0; i<v->Dyn->Dict.n; i++) {
                        if ((*v->Dyn->Dict.mem)[i][0].tag != CEU_VALUE_NIL) {
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
                    printf("func: %p", &v->Dyn->Proto);
                    break;
                case CEU_VALUE_TASK:
                    printf("task: %p", &v->Dyn->Proto);
                    break;
                case CEU_VALUE_CORO:
                    printf("coro: %p", &v->Dyn);
                    break;
                case CEU_VALUE_COROS:
                    printf("coros: %p", &v->Dyn);
                    break;
                case CEU_VALUE_TRACK:
                    printf("track: %p", &v->Dyn);
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        CEU_Value ceu_print_f (CEU_Frame* _1, int n, CEU_Value* args[]) {
            for (int i=0; i<n; i++) {
                if (i > 0) {
                    printf("\t");
                }
                ceu_print1(args[i]);
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Value ceu_println_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            ceu_print_f(frame, n, args);
            printf("\n");
            return (CEU_Value) { CEU_VALUE_NIL };
        }
    """ +
    """
        // EQ-NEQ
        CEU_Value ceu_op_eq_eq_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n == 2);
            CEU_Value* e1 = args[0];
            CEU_Value* e2 = args[1];
            int v = (e1->tag == e2->tag);
            if (v) {
                switch (e1->tag) {
                    case CEU_VALUE_NIL:
                        v = 1;
                        break;
                    case CEU_VALUE_TAG:
                        v = (e1->Tag == e2->Tag);
                        break;
                    case CEU_VALUE_BOOL:
                        v = (e1->Bool == e2->Bool);
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
                                    v = ceu_op_eq_eq_f(frame, 2, xs).Bool;
                                    if (!v) {
                                        break;
                                    }
                                }
                            }
                        }
                        break;
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
            return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=v} };
        }
        CEU_Value ceu_op_div_eq_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            CEU_Value v = ceu_op_eq_eq_f(frame, n, args);
            v.Bool = !v.Bool;
            return v;
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
        ${tops.map { it.first }.joinToString("")}
        ${tops.map { it.second }.joinToString("")}
    """ +
    """ // MAIN
        int main (void) {
            ${this.tags.map { "CEU_TAG_INIT($it,\":$it\")\n" }.joinToString("")}
            assert(CEU_TAG_nil == CEU_VALUE_NIL);
            do {
                {
                    static CEU_Dynamic ceu_tags = { 
                        CEU_VALUE_FUNC, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_tags_f, {0} }
                        }
                    };
                    static CEU_Dynamic ceu_print = { 
                        CEU_VALUE_FUNC, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_print_f, {0} }
                        }
                    };
                    static CEU_Dynamic ceu_println = { 
                        CEU_VALUE_FUNC, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_println_f, {0} }
                        }
                    };
                    static CEU_Dynamic ceu_op_eq_eq = { 
                        CEU_VALUE_FUNC, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_op_eq_eq_f, {0} }
                        }
                    };
                    static CEU_Dynamic ceu_op_div_eq = { 
                        CEU_VALUE_FUNC, NULL, NULL, 1, {
                            .Proto = { NULL, ceu_op_div_eq_f, {0} }
                        }
                    };
                    ceu_mem->tags      = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_tags}      };
                    ceu_mem->print     = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_print}     };
                    ceu_mem->println   = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_println}   };            
                    ceu_mem->op_eq_eq  = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_op_eq_eq}  };
                    ceu_mem->op_div_eq = (CEU_Value) { CEU_VALUE_FUNC, {.Dyn=&ceu_op_div_eq} };
                }
                ${this.code}
                return 0;
            } while (0);
            fprintf(stderr, "%s\n", ceu_err_error_msg);
            return 1;
        }
    """)
}
