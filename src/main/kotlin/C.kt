fun Expr.Block.main (): String {
    return ("" +
    """
        #include <stdio.h>
        #include <stdlib.h>
        #include <stdint.h>
        #include <string.h>
        #include <assert.h>
        #include <stdarg.h>

        struct CEU_Value;
        struct CEU_Block;
        struct CEU_Stack;
        struct CEU_Value_Coro;
        
        // all dynamic data must start with this struct
        // CEU_Tuple, CEU_Value_Coro
        typedef struct CEU_Dynamic {
            struct CEU_Dynamic* next;   // next in block->tofree
            struct CEU_Block* block;    // compare on set, compare on move
        } CEU_Dynamic;
    """ +
    """
        /* VALUE */

        typedef enum CEU_VALUE {
            CEU_VALUE_NIL,
            CEU_VALUE_TAG,
            CEU_VALUE_BOOL,
            CEU_VALUE_NUMBER,
            CEU_VALUE_TUPLE,
            CEU_VALUE_FUNC,
            CEU_VALUE_TASK,     // task prototype
            CEU_VALUE_CORO      // spawned task
        } CEU_VALUE;
        
        typedef struct CEU_Value (*CEU_Value_Func) (struct CEU_Block* ret, int n, struct CEU_Value* args[]);
        typedef struct CEU_Value_Task {
            struct CEU_Value (*func) (struct CEU_Value_Coro* coro, struct CEU_Block* ret, int n, struct CEU_Value* args[]);
            int size;   // buffer w/ locals
        } CEU_Value_Task;

        typedef struct CEU_Value_Tuple {
            CEU_Dynamic dyn;    // tuple is dynamic
            uint8_t n;          // number of items
            char mem[0];        // beginning of CEU_Value[n]
        } CEU_Value_Tuple;
        typedef struct CEU_Value {
            int tag;
            union {
                //void nil;
                int _tag_;
                int bool;
                float number;
                CEU_Value_Tuple* tuple;
                CEU_Value_Func func;
                CEU_Value_Task* task;
                struct CEU_Value_Coro* coro;
            };
        } CEU_Value;
    """ +
    """
        /* BLOCK */

        typedef struct CEU_Block {
            uint8_t depth;          // compare on set
            CEU_Dynamic* tofree;    // list of allocated data to free on exit
        } CEU_Block;
        void ceu_block_free (CEU_Block* block) {
            while (block->tofree != NULL) {
                CEU_Dynamic* cur = block->tofree;
                block->tofree = block->tofree->next;
                free(cur);
            }
        }
        void ceu_block_move (CEU_Dynamic* V, CEU_Block* FR, CEU_Block* TO) {
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
                    cur->block = TO;
                    cur->next = TO->tofree;
                    TO->tofree = cur;
                    break;
                }
                prv = cur;
                cur = cur->next;
            }
        }
    """ +
    """
        /* CORO */

        typedef enum CEU_CORO_STATUS {
            //CEU_CORO_POOL_STATUS,
            //CEU_CORO_STATUS_SPAWNED,
            CEU_CORO_STATUS_RESUMED,
            CEU_CORO_STATUS_YIELDED,
            //CEU_CORO_STATUS_PAUSED,
            //CEU_CORO_STATUS_DYING,
            CEU_CORO_STATUS_TERMINATED
        } CEU_CORO_STATUS;
        
        typedef struct CEU_Value_Coro {
            CEU_Dynamic dyn;            // coro is dynamic
            CEU_CORO_STATUS status;
            CEU_Value_Task* task;             // (Stack* stack, CUE_Coro* coro, void* evt);
            int pc;                     // next line to execute
            char mem[];                 // beginning of locals
        } CEU_Value_Coro;
    """ +
    """
        /* TAGS */

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
            return (CEU_Value) { CEU_VALUE_TAG, {._tag_=args[0]->tag} };
        }
    """ +
    """
        /* PRINT */
        void ceu_print1 (CEU_Value* v) {
            switch (v->tag) {
                case CEU_VALUE_NIL:
                    printf("nil");
                    break;
                case CEU_VALUE_TAG:
                    printf("%s", ceu_tag_to_string(v->_tag_));
                    break;
                case CEU_VALUE_BOOL:
                    if (v->bool) {
                        printf("true");
                    } else {
                        printf("false");
                    }
                    break;
                case CEU_VALUE_NUMBER:
                    printf("%g", v->number);
                    break;
                case CEU_VALUE_TUPLE:
                    printf("[");
                    for (int i=0; i<v->tuple->n; i++) {
                        if (i > 0) {
                            printf(",");
                        }
                        ceu_print1(&((CEU_Value*)v->tuple->mem)[i]);
                    }                    
                    printf("]");
                    break;
                case CEU_VALUE_FUNC:
                    printf("func: %p", v->func);
                    break;
                case CEU_VALUE_TASK:
                    printf("func: %p", v->task);
                    break;
                case CEU_VALUE_CORO:
                    printf("func: %p", v->coro);
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        CEU_Value ceu_print (CEU_Block* ret, int n, CEU_Value* args[]) {
            for (int i=0; i<n; i++) {
                ceu_print1(args[i]);
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Value ceu_println (CEU_Block* ret, int n, CEU_Value* args[]) {
            ceu_print(ret, n, args);
            printf("\n");
            return (CEU_Value) { CEU_VALUE_NIL };
        }
    """ +
    """
        // ==  !=
        CEU_Value ceu_op_eq_eq (CEU_Block* ret, int n, CEU_Value* args[]) {
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
                        v = (e1->_tag_ == e2->_tag_);
                        break;
                    case CEU_VALUE_BOOL:
                        v = (e1->bool == e2->bool);
                        break;
                    case CEU_VALUE_NUMBER:
                        v = (e1->number == e2->number);
                        break;
                    case CEU_VALUE_TUPLE:
                        v = (e1->tuple == e2->tuple);
                        break;
                    case CEU_VALUE_FUNC:
                        v = (e1->func == e2->func);
                        break;
                    case CEU_VALUE_TASK:
                        v = (e1->task == e2->task);
                        break;
                    case CEU_VALUE_CORO:
                        v = (e1->coro == e2->coro);
                        break;
                    default:
                        assert(0 && "bug found");
                }
            }
            return (CEU_Value) { CEU_VALUE_BOOL, {.bool=v} };
        }
        CEU_Value ceu_op_not_eq (CEU_Block* ret, int n, CEU_Value* args[]) {
            CEU_Value v = ceu_op_eq_eq(ret, n, args);
            v.bool = !v.bool;
            return v;
        }
    """ +
    """
        // THROW
        typedef enum {
            CEU_THROW_NONE = 0,
            CEU_THROW_RUNTIME
        } CEU_Throw;
        CEU_Throw ceu_throw = CEU_THROW_NONE;
        CEU_Value ceu_throw_arg;
        CEU_Block* ceu_block_global = NULL;     // used as throw scope. then, catch fixes it
        char ceu_throw_msg[256];
    """ +
    """
        // MAIN
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
            //assert(CEU_TAG_nil == CEU_VALUE_NIL);

            int ceu_brk = 0;
            while (!ceu_brk) {
                ceu_brk = 1;
                typedef struct {
                    CEU_Value tags;
                    CEU_Value print;
                    CEU_Value println;            
                    CEU_Value op_eq_eq;
                    CEU_Value op_not_eq;
                    ${this.mem()}
                } CEU_Func_$n;
                CEU_Func_$n _ceu_mem_;
                CEU_Func_$n* ceu_mem = &_ceu_mem_;
                CEU_Func_$n* ceu_mem_$n = &_ceu_mem_;
                {
                    ceu_mem->tags      = (CEU_Value) { CEU_VALUE_FUNC, {.func=ceu_tags}      };
                    ceu_mem->print     = (CEU_Value) { CEU_VALUE_FUNC, {.func=ceu_print}     };
                    ceu_mem->println   = (CEU_Value) { CEU_VALUE_FUNC, {.func=ceu_println}   };            
                    ceu_mem->op_eq_eq  = (CEU_Value) { CEU_VALUE_FUNC, {.func=ceu_op_eq_eq}  };
                    ceu_mem->op_not_eq = (CEU_Value) { CEU_VALUE_FUNC, {.func=ceu_op_not_eq} };
                }
                ${this.code (
                    ArrayDeque (
                        listOf(Pair(n, mutableSetOf("tags", "print", "println", "op_eq_eq", "op_not_eq")))
                    ),
                    null,
                    null
                )}
                return 0;
            }
            fprintf(stderr, "%s\n", ceu_throw_msg);
            return 1;
        }
    """)
}
