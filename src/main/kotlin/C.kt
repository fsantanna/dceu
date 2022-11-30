fun Coder.main (): String {
    return ("" +
    """ // INCLUDES
        #include <stdio.h>
        #include <stdlib.h>
        #include <stddef.h>
        #include <stdint.h>
        #include <string.h>
        #include <assert.h>
        #include <stdarg.h>
        #include <math.h>
    """ +
    """ // CEU_Value
        typedef enum CEU_VALUE {
            CEU_VALUE_NIL = 0,
            CEU_VALUE_TAG,
            CEU_VALUE_BOOL,
            CEU_VALUE_NUMBER,
            CEU_VALUE_POINTER,
            CEU_VALUE_FUNC,     // func frame
            CEU_VALUE_TASK,     // task frame
            CEU_VALUE_TUPLE,
            CEU_VALUE_DICT,
            CEU_VALUE_CORO,     // spawned task
            CEU_VALUE_COROS     // pool of spawned tasks
        } CEU_VALUE;
        
        typedef enum CEU_CORO_STATUS {
            CEU_CORO_STATUS_RESUMED,
            CEU_CORO_STATUS_YIELDED,
            CEU_CORO_STATUS_TERMINATED
        } CEU_CORO_STATUS;        

        struct CEU_Dynamic;
        struct CEU_Frame;        
        struct CEU_Block;        

        typedef struct CEU_Value {
            CEU_VALUE tag;
            union {
                //void nil;
                int Tag;
                int Bool;
                double Number;
                void* Pointer;
                struct CEU_Frame* Frame;    // Func/Task 
                struct CEU_Dynamic* Dyn;    // Tuple/Dict/Coro/Coros: allocates memory
            };
        } CEU_Value;
        
        int ceu_as_bool (CEU_Value* v) {
            return !(v->tag==CEU_VALUE_NIL || (v->tag==CEU_VALUE_BOOL && !v->Bool));
        }
    """ +
    """ // CEU_Frame
        typedef struct CEU_Frame {
            struct CEU_Frame* up;   // active frame above
            void* mem;              // active local variables
            union {
                struct CEU_Value (*Func) (
                    struct CEU_Frame* func,
                    int depth,
                    int n,
                    struct CEU_Value* args[]
                );
                struct {
                    struct CEU_Value (*f) (
                        struct CEU_Dynamic* coro,   // coro->Bcast.Coro
                        int depth,
                        int n,
                        struct CEU_Value* args[]
                    );
                    int size;                       // local mem must be allocated for each coro
                } Task;
            };
        } CEU_Frame;
    """ +
    """ // TAGS
        #define CEU_TAG_DEFINE(id,str)              \
            const int CEU_TAG_##id = __COUNTER__;   \
            CEU_Tags ceu_tag_##id = { str, NULL };
        #define CEU_TAG_INIT(id,str)                \
            ceu_tag_##id.next = CEU_TAGS;           \
            CEU_TAGS = &ceu_tag_##id;               \
            CEU_TAGS_MAX++;
            
        typedef struct CEU_Tags {
            char* name;
            struct CEU_Tags* next;
        } CEU_Tags;
        
        CEU_Value ceu_tags_f (CEU_Frame* _1, int _2, int n, CEU_Value* args[]) {
            assert(n == 1 && "bug found");
            return (CEU_Value) { CEU_VALUE_TAG, {.Tag=args[0]->tag} };
        }

        static CEU_Tags* CEU_TAGS = NULL;
        int CEU_TAGS_MAX = 0;        
        CEU_Frame ceu_tags = { NULL, NULL, {.Func=ceu_tags_f} };
        ${this.tags.map { "CEU_TAG_DEFINE($it,\":$it\")\n" }.joinToString("")}

        char* ceu_tag_to_string (int tag) {
            CEU_Tags* cur = CEU_TAGS;
            for (int i=0; i<CEU_TAGS_MAX-tag-1; i++) {
                cur = cur->next;
            }
            return cur->name;
        }              
    """ +
    """ // THROW / ERR / EVT
        int ceu_has_bcast = 0;
        int ceu_has_throw = 0;
        CEU_Value CEU_ERR_ERROR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_error} };
        CEU_Value CEU_ERR_NIL = { CEU_VALUE_NIL };
        CEU_Value* ceu_err = &CEU_ERR_NIL;
        char ceu_err_error_msg[256];
        CEU_Value CEU_EVT_CLEAR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_clear} };
        CEU_Value CEU_EVT_NIL = { CEU_VALUE_NIL };
        CEU_Value* ceu_evt = &CEU_EVT_NIL;
        int ceu_has_throw_clear (void) {
            return (ceu_has_throw > 0) || (ceu_has_bcast>0 && ceu_evt==&CEU_EVT_CLEAR);
        }
    """ +
    """ // CEU_Dynamic
        typedef struct CEU_Dynamic {
            CEU_VALUE tag;                  // required to switch over free/bcast
            struct CEU_Dynamic* next;       // next dyn to free (not used by coro in coros)
            struct CEU_Block*   hold;       // holding block to compare on set/move
            union {
                struct {
                    int n;                  // number of items
                    CEU_Value mem[0];       // beginning of CEU_Value[n]
                } Tuple;
                struct {
                    int n;                  // size of mem
                    CEU_Value (*mem)[0][2]; // beginning of CEU_Value[n][2]
                } Dict;
                struct {
                    struct CEU_Dynamic* next;           // bcast->Bcast, next dyn to bcast
                    union {
                        struct {
                            enum CEU_CORO_STATUS status;
                            struct CEU_Dynamic* coros;  // auto terminate / remove from coros
                            struct CEU_Block* block;    // first block to bcast
                            struct CEU_Frame task;      // task->Task
                            int pc;                     // next line to execute
                            CEU_Value pub;
                            char __mem[0];              // beginning of locals, will be allocated here, but accessed through this->task->mem
                        } Coro;
                        struct {
                            uint8_t max;                // max number of instances
                            uint8_t cur;                // cur number of instances
                            uint8_t open;               // number of open iterators
                            struct CEU_Dynamic* first;  // coro->Bcast.Coro, first coro to bcast/free
                        } Coros;
                    };
                } Bcast;
            };
        } CEU_Dynamic;
    """ +
    """ // BLOCK
        typedef struct CEU_Block {
            uint8_t depth;                      // compare on set
            CEU_Dynamic* tofree;                // list of allocated data to free on exit
            struct {
                struct CEU_Block* block;        // nested block active
                struct CEU_Dynamic* dyn;        // first coro/coros in this block
            } bcast;
        } CEU_Block;
        void ceu_block_free (CEU_Block* block) {
            while (block->tofree != NULL) {
                CEU_Dynamic* cur = block->tofree;
                if (cur->tag == CEU_VALUE_DICT) {
                    free(cur->Dict.mem);
                }
                block->tofree = block->tofree->next;
                free(cur);
            }
        }
        void ceu_bcast_enqueue (CEU_Dynamic** outer, CEU_Dynamic* dyn);
        char* ceu_block_set (CEU_Block* dst, CEU_Value* src) {
            switch (src->tag) {
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<src->Dyn->Tuple.n; i++) {
                        ceu_block_set(dst, &src->Dyn->Tuple.mem[i]);
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<src->Dyn->Dict.n; i++) {
                        ceu_block_set(dst, &(*src->Dyn->Dict.mem)[i][0]);
                        ceu_block_set(dst, &(*src->Dyn->Dict.mem)[i][1]);
                    }
                    break;
                case CEU_VALUE_CORO:
                case CEU_VALUE_COROS:
                    break;
                default:
                    return NULL;    // nothing to be done for non-dyn
            }
            if (src->Dyn->hold == NULL) {
                src->Dyn->hold = dst;
                src->Dyn->next = dst->tofree;
                dst->tofree = src->Dyn;
                if (src->tag >= CEU_VALUE_CORO) {  // any Coro/Coros
                    ceu_bcast_enqueue(&dst->bcast.dyn, src->Dyn);
                }
            } else if (src->Dyn->hold->depth > dst->depth) {
                ceu_has_throw = 1;
                ceu_err = &CEU_ERR_ERROR;
                return "set error : incompatible scopes";
            }
            return NULL;
        }

        //  - can pass further
        //  - cannot pass back
        //  - each catch condition:
        //      - must set its depth at the beginning 
        //      - must not yield
        //      - must deallocate at the end
        CEU_Block ceu_err_block = { 0, NULL, {NULL,NULL} };
        CEU_Block ceu_evt_block = { 0, NULL, {NULL,NULL} };
    """ +
    """ // BCAST
        void ceu_bcast_dyns (CEU_Dynamic* cur);
        void ceu_bcast_blocks_aux (CEU_Block* cur) {
            assert(ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR);
            while (cur != NULL) {
                CEU_Dynamic* dyn = cur->bcast.dyn;
                if (dyn != NULL) {
                    assert(ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR);
                    ceu_bcast_dyns(dyn);
                    if (ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR) {
                        // ok
                    } else {
                        // coros threw exception and didn't catch it
                        // nested blocks must be killed as well
                        // stop now, clean up comes soon from :clear
                        return;
                    }
                }
                cur = cur->bcast.block;
            }
        }

        void* ceu_bcast_blocks (CEU_Block* cur, CEU_Value* evt) {
            assert(ceu_has_throw==0 || evt==&CEU_EVT_CLEAR);
            char* err = ceu_block_set(&ceu_evt_block, evt);
            if (err != NULL) {
                return err;
            }
            CEU_Value* prv = ceu_evt;
            ceu_has_bcast++;
            ceu_evt = evt;
            assert(ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR);
            ceu_bcast_blocks_aux(cur);
            ceu_has_bcast--;
            int isclr = (ceu_evt == &CEU_EVT_CLEAR);
            ceu_evt = prv;
            if (ceu_has_throw==0 || isclr) {
                // ok
            } else {
                // whole bcast threw exception and didn't catch it
                // must not clean up now
                // stop now, clean up comes soon form :clear
                return NULL;
            }
            if (ceu_has_bcast == 0) {
                ceu_block_free(&ceu_evt_block);
            }
            return NULL;
        }
        
        void ceu_bcast_dyns (CEU_Dynamic* cur) {
            assert(ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR);
            while (cur != NULL) {
                CEU_Dynamic* nxt = cur->Bcast.next; // take nxt before cur is/may-be freed
                switch (cur->tag) {
                    case CEU_VALUE_CORO: {
                        if (cur->Bcast.Coro.status != CEU_CORO_STATUS_YIELDED) {
                            // skip
                        } else {
                            assert(ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR);
                            ceu_bcast_blocks_aux(cur->Bcast.Coro.block);
                            // if nested block threw uncaught exception, awake myself next to catch it
                            //assert(ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR);
                            CEU_Value arg = { CEU_VALUE_NIL };
                            CEU_Value* args[] = { &arg };
                            cur->Bcast.Coro.task.Task.f(cur, 1, args);
                            if (ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR) {
                                // ok
                            } else {
                                // cur threw exception and didn't catch it
                                // brothers must be killed as well
                                // stop now, clean up comes soon from :clear
                                return;
                            }
                        }
                        break;
                    }
                    case CEU_VALUE_COROS: {
                        assert(ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR);
                        ceu_bcast_dyns(cur->Bcast.Coros.first);
                        if (ceu_has_throw==0 || ceu_evt==&CEU_EVT_CLEAR) {
                            // ok
                        } else {
                            // cur threw exception and didn't catch it
                            // brothers must be killed as well
                            // stop now, clean up comes soon from :clear
                            return;
                        }
                        break;
                    }
                }
                cur = nxt;
            }
        }
        
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
    """ +
    """ // COROS
        char* ceu_coro_create (CEU_Value* task, CEU_Value* ret) {
            if (task->tag != CEU_VALUE_TASK) {
                return "coroutine error : expected task";
            }
            CEU_Dynamic* coro = malloc(sizeof(CEU_Dynamic) + task->Frame->Task.size);
            assert(coro != NULL);
            task->Frame->mem = coro->Bcast.Coro.__mem;
            *coro = (CEU_Dynamic) {
                CEU_VALUE_CORO, NULL, NULL, {
                    .Bcast = { NULL, {.Coro = {CEU_CORO_STATUS_YIELDED,NULL,NULL,*(task->Frame),0} } }
                }
            };
            *ret = ((CEU_Value) { CEU_VALUE_CORO, {.Dyn=coro} });
            return NULL;
        }

        char* ceu_coros_create (int* ok, CEU_Dynamic* coros, CEU_Value* task, CEU_Value* ret) {
            if (coros->Bcast.Coros.max!=0 && coros->Bcast.Coros.cur==coros->Bcast.Coros.max) {
                *ok = 0;
                return NULL;
            }
            if (task->tag != CEU_VALUE_TASK) {
                return "coroutine error : expected task";
            }
            CEU_Dynamic* coro = malloc(sizeof(CEU_Dynamic) + task->Frame->Task.size);
            assert(coro != NULL);
            task->Frame->mem = coro->Bcast.Coro.__mem;
            *coro = (CEU_Dynamic) {
                CEU_VALUE_CORO, NULL, coros->hold, { // no free
                    .Bcast = { NULL, {.Coro = {CEU_CORO_STATUS_YIELDED,coros,NULL,*(task->Frame),0} } }
                }
            };
            ceu_bcast_enqueue(&coros->Bcast.Coros.first, coro);
            *ret = ((CEU_Value) { CEU_VALUE_CORO, {.Dyn=coro} });
            coros->Bcast.Coros.cur++;
            return NULL;
        }

        char* ceu_coros_destroy (CEU_Dynamic* coros, CEU_Dynamic* coro) {
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
            free(coro);
            coros->Bcast.Coros.cur--;
        }
        
        char* ceu_coros_cleanup (CEU_Dynamic* coros) {
            CEU_Dynamic* cur = coros->Bcast.Coros.first;
            while (cur != NULL) {
                CEU_Dynamic* nxt = cur->Bcast.next;
                if (cur->Bcast.Coro.status == CEU_CORO_STATUS_TERMINATED) {
                    //assert(0 && "OK");
                    ceu_coros_destroy(coros, cur);
                }
                cur = nxt;
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
                    printf("func: %p", &v->Frame);
                    break;
                case CEU_VALUE_TASK:
                    printf("task: %p", &v->Frame);
                    break;
                case CEU_VALUE_CORO:
                    printf("coro: %p", &v->Dyn);
                    break;
                case CEU_VALUE_COROS:
                    printf("coros: %p", &v->Dyn);
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        CEU_Value ceu_print_f (CEU_Frame* _1, int _2, int n, CEU_Value* args[]) {
            for (int i=0; i<n; i++) {
                if (i > 0) {
                    printf("\t");
                }
                ceu_print1(args[i]);
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Frame ceu_print = { NULL, NULL, {.Func=ceu_print_f} };
        CEU_Value ceu_println_f (CEU_Frame* func, int depth, int n, CEU_Value* args[]) {
            ceu_print.Func(func, depth, n, args);
            printf("\n");
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Frame ceu_println = { NULL, NULL, {.Func=ceu_println_f} };
    """ +
    """
        // EQ-NEQ
        CEU_Value ceu_op_eq_eq_f (CEU_Frame* func, int depth, int n, CEU_Value* args[]) {
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
                                    v = ceu_op_eq_eq_f(func, depth, 2, xs).Bool;
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
        CEU_Frame ceu_op_eq_eq = { NULL, NULL, {.Func=ceu_op_eq_eq_f} };
        CEU_Value ceu_op_div_eq_f (CEU_Frame* func, int depth, int n, CEU_Value* args[]) {
            CEU_Value v = ceu_op_eq_eq.Func(func, depth, n, args);
            v.Bool = !v.Bool;
            return v;
        }
        CEU_Frame ceu_op_div_eq = { NULL, NULL, {.Func=ceu_op_div_eq_f} };
    """ +
    """ // TUPLE / DICT
        void ceu_max_depth (CEU_Dynamic* dyn, int n, CEU_Value* childs) {
            // new dyn should have at least the maximum depth among its children
            CEU_Block* hld = NULL;
            int max = -1;
            for (int i=0; i<n; i++) {
                CEU_Value* cur = &childs[i];
                if (cur->tag>=CEU_VALUE_TUPLE && cur->Dyn->hold!=NULL) {
                    if (max < cur->Dyn->hold->depth) {
                        max = cur->Dyn->hold->depth;
                        hld = cur->Dyn->hold;
                    }
                }
            }
            if (max != NULL) {
                dyn->hold = hld;
                dyn->next = hld->tofree;
                hld->tofree = dyn;
            }
        }

        CEU_Dynamic* ceu_tuple_create (int n, CEU_Value* args) {
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic) + n*sizeof(CEU_Value));
            if (ret == NULL) {
                return NULL;
            }
            assert(ret != NULL);
            *ret = (CEU_Dynamic) { CEU_VALUE_TUPLE, NULL, NULL, {.Tuple={n,{}}} };
            memcpy(ret->Tuple.mem, args, n*sizeof(CEU_Value));
            ceu_max_depth(ret, n, args);
            return ret;
        }

        CEU_Dynamic* ceu_dict_create (int n, CEU_Value (*args)[][2]) {
            int min = (n < 4) ? 4 : n; 
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic));
            if (ret == NULL) {
                return NULL;
            }
            CEU_Value (*mem)[][2] = malloc(min*2*sizeof(CEU_Value));
            if (mem == NULL) {
                free(ret);
                return NULL;
            }
            memset(mem, 0, min*2*sizeof(CEU_Value));  // x[i]=nil
            *ret = (CEU_Dynamic) { CEU_VALUE_DICT, NULL, NULL, {.Dict={min,mem}} };
            memcpy(mem, args, n*2*sizeof(CEU_Value));
            ceu_max_depth(ret, n*2, (CEU_Value*)args);
            return ret;
        }

        int ceu_dict_key_index (CEU_Dynamic* col, CEU_Value* key) {
            for (int i=0; i<col->Dict.n; i++) {
                CEU_Value* args[] = { key, &(*col->Dict.mem)[i][0] };
                if (ceu_op_eq_eq_f(&ceu_op_eq_eq, 2, args).Bool) {
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
                ceu_has_throw = 1;
                ceu_err = &CEU_ERR_ERROR;
                return "index error : expected collection";
            }
            if (col->tag == CEU_VALUE_TUPLE) {
                if (idx->tag != CEU_VALUE_NUMBER) {
                    ceu_has_throw = 1;
                    ceu_err = &CEU_ERR_ERROR;
                    return "index error : expected number";
                }
                if (col->Dyn->Tuple.n <= idx->Number) {                
                    ceu_has_throw = 1;
                    ceu_err = &CEU_ERR_ERROR;
                    return "index error : out of bounds";
                }
            }
            return NULL;
        }
    """ +
    """ // FUNCS
        typedef struct {
            ${GLOBALS.map { "CEU_Value $it;\n" }.joinToString("")}
            ${this.mem}
        } CEU_Func_${this.outer.n};
        CEU_Func_${this.outer.n} _ceu_mem_;
        CEU_Func_${this.outer.n}* ceu_mem = &_ceu_mem_;
        CEU_Func_${this.outer.n}* ceu_mem_${this.outer.n} = &_ceu_mem_;
        ${tops.map { it.first }.joinToString("")}
        ${tops.map { it.second }.joinToString("")}
    """ +
    """ // MAIN
        int main (void) {
            ${this.tags.map { "CEU_TAG_INIT($it,\":$it\")\n" }.joinToString("")}
            assert(CEU_TAG_nil == CEU_VALUE_NIL);
            do {
                {
                    ceu_mem->tags      = (CEU_Value) { CEU_VALUE_FUNC, {.Frame=&ceu_tags}      };
                    ceu_mem->print     = (CEU_Value) { CEU_VALUE_FUNC, {.Frame=&ceu_print}     };
                    ceu_mem->println   = (CEU_Value) { CEU_VALUE_FUNC, {.Frame=&ceu_println}   };            
                    ceu_mem->op_eq_eq  = (CEU_Value) { CEU_VALUE_FUNC, {.Frame=&ceu_op_eq_eq}  };
                    ceu_mem->op_div_eq = (CEU_Value) { CEU_VALUE_FUNC, {.Frame=&ceu_op_div_eq} };
                }
                ${this.code}
                return 0;
            } while (0);
            fprintf(stderr, "%s\n", ceu_err_error_msg);
            ceu_block_free(&ceu_err_block);
            ceu_err = &CEU_ERR_NIL;
            return 1;
        }
    """)
}
