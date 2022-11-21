fun Coder.main (): String {
    return ("" +
    """ // INCLUDES
        #include <stdio.h>
        #include <stdlib.h>
        #include <stdint.h>
        #include <string.h>
        #include <assert.h>
        #include <stdarg.h>
        #include <math.h>

    """ +
    """ // VALUE
        typedef enum CEU_VALUE {
            CEU_VALUE_NIL = 0,
            CEU_VALUE_TAG,
            CEU_VALUE_BOOL,
            CEU_VALUE_NUMBER,
            CEU_VALUE_POINTER,
            CEU_VALUE_FUNC,     // func prototype
            CEU_VALUE_TASK,     // task prototype
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
        struct CEU_Proto;        
        struct CEU_Block;        

        typedef struct CEU_Value {
            CEU_VALUE tag;
            union {
                //void nil;
                int Tag;
                int Bool;
                double Number;
                void* Pointer;
                struct CEU_Proto* Proto;    // Func/Task 
                struct CEU_Dynamic* Dyn;    // Tuple/Dict/Coro/Coros: allocates memory
            };
        } CEU_Value;

        typedef struct CEU_Proto {
            struct CEU_Proto* up;   // static lexical scope above
            void* mem;              // local variables are external to proto
            union {
                struct CEU_Value (*Func) (
                    struct CEU_Proto* func,
                    struct CEU_Block* ret,
                    int n,
                    struct CEU_Value* args[]
                );
                struct {
                    struct CEU_Value (*f) (
                        struct CEU_Dynamic* coro,   // coro->Bcast.Coro
                        struct CEU_Block* ret,
                        int n,
                        struct CEU_Value* args[]
                    );
                    int size;                       // local mem must be allocated for each coro
                } Task;
            };
        } CEU_Proto;
        
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
                            struct CEU_Proto* task;     // task->Task
                            int pc;                     // next line to execute
                            char mem[0];                // beginning of locals
                        } Coro;
                        struct {
                            uint8_t n;                  // number of open iterators
                            struct CEU_Dynamic* first;  // coro->Bcast.Coro, first coro to bcast/free
                        } Coros;
                    };
                } Bcast;
            };
        } CEU_Dynamic;
        
        int ceu_as_bool (CEU_Value* v) {
            return !(v->tag==CEU_VALUE_NIL || (v->tag==CEU_VALUE_BOOL && !v->Bool));
        }
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
        #if 0
        void ceu_block_move (CEU_Dynamic* V, CEU_Block* FR, CEU_Block* TO) {
            assert(V->tag == CEU_VALUE_TUPLE && "bug found");
            CEU_Dynamic* prv = NULL;
            CEU_Dynamic* cur = FR->tofree;
            while (cur != NULL) {
                if (cur == V) {
                    if (prv == NULL) {
                        FR->tofree = NULL;
                    } else {
                        prv->next = cur->next;
                    }              
                    //assert(0 && "OK");
                    cur->hold = TO;
                    cur->next = TO->tofree;
                    TO->tofree = cur;
                    break;
                }
                prv = cur;
                cur = cur->next;
            }
        }
        #endif
    """ +
    """ // BCAST / COROS
        void ceu_bcast_dyns (CEU_Dynamic* cur, CEU_Value* arg);
        void ceu_bcast_blocks (CEU_Block* cur, CEU_Value* arg) {
            while (cur != NULL) {
                CEU_Dynamic* dyn = cur->bcast.dyn;
                if (dyn != NULL) {
                    ceu_bcast_dyns(dyn, arg);
                }
                cur = cur->bcast.block;
            }
        }
        
        void ceu_bcast_dyns (CEU_Dynamic* cur, CEU_Value* arg) {
            while (cur != NULL) {
                CEU_Dynamic* nxt = cur->Bcast.next; // take nxt before cur is/may-be freed
                switch (cur->tag) {
                    case CEU_VALUE_CORO: {
                        if (cur->Bcast.Coro.status != CEU_CORO_STATUS_YIELDED) {
                            // skip
                        } else {
                            ceu_bcast_blocks(cur->Bcast.Coro.block, arg);
                            CEU_Value* args[] = { arg };
                            cur->Bcast.Coro.task->Task.f(cur, NULL, 1, args);
                        }
                        break;
                    }
                    case CEU_VALUE_COROS: {
                        ceu_bcast_dyns(cur->Bcast.Coros.first, arg);
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

        char* ceu_coro_create (CEU_Block* hld, CEU_Value* task, CEU_Value* ret) {
            if (task->tag != CEU_VALUE_TASK) {
                return "coroutine error : expected task";
            }
            CEU_Dynamic* coro = malloc(sizeof(CEU_Dynamic) + task->Proto->Task.size);
            assert(coro != NULL);
            *coro = (CEU_Dynamic) {
                CEU_VALUE_CORO, hld->tofree, hld, {
                    .Bcast = { NULL, {.Coro = {CEU_CORO_STATUS_YIELDED,NULL,NULL,task->Proto,0} } }
                }
            };
            ceu_bcast_enqueue(&hld->bcast.dyn, coro);
            hld->tofree = coro;
            *ret = ((CEU_Value) { CEU_VALUE_CORO, {.Dyn=coro} });
            return NULL;
        }

        char* ceu_coros_create (CEU_Dynamic* coros, CEU_Value* task, CEU_Value* ret) {
            if (task->tag != CEU_VALUE_TASK) {
                return "coroutine error : expected task";
            }
            CEU_Dynamic* coro = malloc(sizeof(CEU_Dynamic) + task->Proto->Task.size);
            assert(coro != NULL);
            *coro = (CEU_Dynamic) {
                CEU_VALUE_CORO, NULL, NULL, { // no free, no block
                    .Bcast = { NULL, {.Coro = {CEU_CORO_STATUS_YIELDED,coros,NULL,task->Proto,0} } }
                }
            };
            ceu_bcast_enqueue(&coros->Bcast.Coros.first, coro);
            *ret = ((CEU_Value) { CEU_VALUE_CORO, {.Dyn=coro} });
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
        
        static CEU_Tags* CEU_TAGS = NULL;
        int CEU_TAGS_MAX = 0;
        
        char* ceu_tag_to_string (int tag) {
            CEU_Tags* cur = CEU_TAGS;
            for (int i=0; i<CEU_TAGS_MAX-tag-1; i++) {
                cur = cur->next;
            }
            return cur->name;
        }
              
        CEU_Value ceu_tags_f (CEU_Proto* _1, CEU_Block* _2, int n, CEU_Value* args[]) {
            assert(n == 1 && "bug found");
            return (CEU_Value) { CEU_VALUE_TAG, {.Tag=args[0]->tag} };
        }
        CEU_Proto ceu_tags = { NULL, NULL, {.Func=ceu_tags_f} };
        ${this.tags.map { "CEU_TAG_DEFINE($it,\"#$it\")\n" }.joinToString("")}
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
                    printf("func: %p", &v->Proto);
                    break;
                case CEU_VALUE_TASK:
                    printf("task: %p", &v->Proto);
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
        CEU_Value ceu_print_f (CEU_Proto* _1, CEU_Block* _2, int n, CEU_Value* args[]) {
            for (int i=0; i<n; i++) {
                if (i > 0) {
                    printf("\t");
                }
                ceu_print1(args[i]);
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Proto ceu_print = { NULL, NULL, {.Func=ceu_print_f} };
        CEU_Value ceu_println_f (CEU_Proto* func, CEU_Block* ret, int n, CEU_Value* args[]) {
            ceu_print.Func(func, ret, n, args);
            printf("\n");
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Proto ceu_println = { NULL, NULL, {.Func=ceu_println_f} };
    """ +
    """
        // EQ-NEQ
        CEU_Value ceu_op_eq_eq_f (CEU_Proto* func, CEU_Block* ret, int n, CEU_Value* args[]) {
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
                                    v = ceu_op_eq_eq_f(func, ret, 2, xs).Bool;
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
        CEU_Proto ceu_op_eq_eq = { NULL, NULL, {.Func=ceu_op_eq_eq_f} };
        CEU_Value ceu_op_div_eq_f (CEU_Proto* func, CEU_Block* ret, int n, CEU_Value* args[]) {
            CEU_Value v = ceu_op_eq_eq.Func(func, ret, n, args);
            v.Bool = !v.Bool;
            return v;
        }
        CEU_Proto ceu_op_div_eq = { NULL, NULL, {.Func=ceu_op_div_eq_f} };
    """ +
    """ // TUPLE / DICT
        CEU_Dynamic* ceu_tuple_create (CEU_Block* hld, int n, CEU_Value* args) {
            CEU_Dynamic* ret = malloc(sizeof(CEU_Dynamic) + n*sizeof(CEU_Value));
            if (ret == NULL) {
                return NULL;
            }
            assert(ret != NULL);
            *ret = (CEU_Dynamic) { CEU_VALUE_TUPLE, (hld==NULL ? NULL : hld->tofree), hld, {.Tuple={n,{}}} };
            memcpy(ret->Tuple.mem, args, n*sizeof(CEU_Value));
            hld->tofree = ret;
            return ret;
        }

        CEU_Dynamic* ceu_dict_create (CEU_Block* hld, int n, CEU_Value (*args)[][2]) {
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
            *ret = (CEU_Dynamic) { CEU_VALUE_DICT, (hld==NULL ? NULL : hld->tofree), hld, {.Dict={min,mem}} };
            if (args != NULL) {
                memcpy(mem, args, n*2*sizeof(CEU_Value));
            }
            hld->tofree = ret;
            return ret;
        }

        int ceu_dict_key_index (CEU_Dynamic* col, CEU_Value* key) {
            for (int i=0; i<col->Dict.n; i++) {
                CEU_Value* args[] = { key, &(*col->Dict.mem)[i][0] };
                if (ceu_op_eq_eq_f(&ceu_op_eq_eq, NULL, 2, args).Bool) {
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
    """ +
    """
        // THROW
        int ceu_has_throw = 0;
        CEU_Value CEU_THROW_ERROR = { CEU_VALUE_TAG, {.Tag=CEU_TAG_error} };
        CEU_Value CEU_THROW_NIL = { CEU_VALUE_NIL };
        CEU_Value* ceu_throw = &CEU_THROW_NIL;
        char ceu_throw_error_msg[256];
        CEU_Block ceu_throw_block = { 0, NULL, {NULL,NULL} };
            //  - can pass further
            //  - cannot pass back
            //  - each catch condition:
            //      - must set its depth at the beginning 
            //      - must not yield
            //      - must deallocate at the end
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
            ${this.tags.map { "CEU_TAG_INIT($it,\"#$it\")\n" }.joinToString("")}
            assert(CEU_TAG_nil == CEU_VALUE_NIL);
            do {
                {
                    ceu_mem->tags      = (CEU_Value) { CEU_VALUE_FUNC, {.Proto=&ceu_tags}      };
                    ceu_mem->print     = (CEU_Value) { CEU_VALUE_FUNC, {.Proto=&ceu_print}     };
                    ceu_mem->println   = (CEU_Value) { CEU_VALUE_FUNC, {.Proto=&ceu_println}   };            
                    ceu_mem->op_eq_eq  = (CEU_Value) { CEU_VALUE_FUNC, {.Proto=&ceu_op_eq_eq}  };
                    ceu_mem->op_div_eq = (CEU_Value) { CEU_VALUE_FUNC, {.Proto=&ceu_op_div_eq} };
                }
                ${this.code}
                return 0;
            } while (0);
            fprintf(stderr, "%s\n", ceu_throw_error_msg);
            ceu_block_free(&ceu_throw_block);
            ceu_throw = &CEU_THROW_NIL;
            return 1;
        }
    """)
}
