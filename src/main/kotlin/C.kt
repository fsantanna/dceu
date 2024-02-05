package dceu

fun Coder.main (tags: Tags): String {

    // INCLUDES / DEFINES / ENUMS
    fun h_includes (): String {
        return """
    #include <stdio.h>
    #include <stdlib.h>
    #include <stddef.h>
    #include <stdint.h>
    #include <string.h>
    #include <assert.h>
    #include <stdarg.h>
    #include <time.h>
    #include <math.h>
    """
    }
    fun h_defines (): String {
        return """
    ${DEBUG.cond { "#define CEU_DEBUG" }}
    #define CEU $CEU
    #define CEU_MULTI $MULTI
    
    #undef MAX
    #undef MIN
    #define MAX(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a > _b ? _a : _b; })
    #define MIN(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a < _b ? _a : _b; })

    #define COMMA ,
    #if CEU >= 2
    #define CEU2(x) x
    #else
    #define CEU2(x)
    #endif
    #if CEU >= 3
    #define CEU3(x) x
    #else
    #define CEU3(x)
    #endif
    #if CEU >= 4
    #define CEU4(x) x
    #else
    #define CEU4(x)
    #endif
    #if CEU >= 5
    #define CEU5(x) x
    #else
    #define CEU5(x)
    #endif

    #if CEU >= 4
    //#define CEU_DEPTH_CHK(min,me,s) if ((min) < (me)) { assert(0&&"XXX"); s; }
    #define CEU_DEPTH_CHK(min,me,s) if ((min) < (me)) s
    #endif

    #if CEU >= 5
    #define CEU_HLD_BLOCK(dyn) ({ CEU_Block* blk=(dyn)->Any.hld.block; (dyn)->Any.type!=CEU_VALUE_EXE_TASK_IN ? blk : ((CEU_Block*)((CEU_Tasks*)blk)->hld.block); })
    #define CEU_HLD_DYNS(dyn) ((dyn)->Any.type == CEU_VALUE_EXE_TASK_IN ? (&((CEU_Tasks*)((dyn)->Any.hld.block))->dyns) : (&((CEU_Block*)(dyn)->Any.hld.block)->dn.dyns))
    #else
    //#define CEU_HLD_BLOCK(dyn) ((dyn)->Any.hld.block)
    //#define CEU_HLD_DYNS(dyn) (&((CEU_Block*)(dyn)->Any.hld.block)->dn.dyns)
    #endif
    """
    }
    fun h_enums (): String {
        return """
    typedef enum CEU_ARG {
        #if CEU >= 4
        CEU_ARG_TOGGLE = -3,    // restore time to CEU_TIME_MIN after toggle
        CEU_ARG_ERROR = -2,     // awake task to catch error from nested task
        #endif
        #if CEU >= 3
        CEU_ARG_ABORT = -1,     // awake task to finalize defers and release memory
        #endif
        CEU_ARG_ARGS  =  0      // 1, 2, ...
    } CEU_ARG;

    typedef enum CEU_VALUE {
        CEU_VALUE_BLOCK = -1,
        CEU_VALUE_NIL = 0,
        CEU_VALUE_ERROR,
        CEU_VALUE_TAG,
        CEU_VALUE_BOOL,
        CEU_VALUE_CHAR,
        CEU_VALUE_NUMBER,
        CEU_VALUE_POINTER,
        CEU_VALUE_DYNAMIC,    // all below are dynamic
        CEU_VALUE_CLO_FUNC,
        #if CEU >= 3
        CEU_VALUE_CLO_CORO,
        #endif
        #if CEU >= 4
        CEU_VALUE_CLO_TASK,
        #endif
        CEU_VALUE_TUPLE,
        CEU_VALUE_VECTOR,
        CEU_VALUE_DICT,
        #if CEU >= 3
        CEU_VALUE_EXE_CORO,
        #endif
        #if CEU >= 4
        CEU_VALUE_EXE_TASK,
        #endif
        #if CEU >= 5
        CEU_VALUE_EXE_TASK_IN,
        CEU_VALUE_TASKS,
        CEU_VALUE_TRACK,
        #endif
        CEU_VALUE_MAX
    } __attribute__ ((__packed__)) CEU_VALUE;
    _Static_assert(sizeof(CEU_VALUE) == 1, "bug found");

    #if CEU >= 3
    typedef enum CEU_EXE_STATUS {
        CEU_EXE_STATUS_YIELDED = 1,
        #if CEU >= 4
        CEU_EXE_STATUS_TOGGLED,
        #endif
        CEU_EXE_STATUS_RESUMED,
        CEU_EXE_STATUS_TERMINATED,
    } CEU_EXE_STATUS;
    #endif
    """
    }

    // CEU_Frame, CEU_Block, CEU_Value, CEU_Dyn, CEU_Tags_*
    fun h_frame_block (): String {
        return  """
    #if 0
    typedef struct CEU_Frame {          // call func / create task
        struct CEU_Block*    up_block;  // block enclosing this call/coroutine
        struct CEU_Clo*      clo;       // TODO: should be CEU_Value, but CEU_Exe holds CEU_Frame, which holds CEU_Clo
    #if CEU >= 3
        union {
            struct CEU_Exe*      exe;       // coro/task<->frame point to each other
            struct CEU_Exe_Task* exe_task;
        };
    #endif
    } CEU_Frame;
    #endif

    #if CEU >= 4
    typedef struct CEU_Dyns {           // list of allocated data to bcast/free
        union CEU_Dyn* first;
        union CEU_Dyn* last;
    } CEU_Dyns;
    #endif

    #if 0
    typedef struct CEU_Block {
        uint8_t  istop;
        union {
            //struct CEU_Frame* frame;    // istop = 1
            struct CEU_Block* block;    // istop = 0
        } up;
        struct {
    #if CEU >= 4
            struct CEU_Block* block;    // bcast
    #endif
            CEU_Dyns dyns;
        } dn;
    } CEU_Block;
    #endif
    """
    }
    fun h_value_dyn (): String {
        return """
    typedef struct CEU_Value {
        CEU_VALUE type;
        union {
            //void nil;
            char* Error;
            unsigned int Tag;
            int Bool;
            char Char;
            double Number;
            void* Pointer;
            union CEU_Dyn* Dyn;    // Func/Task/Tuple/Dict/Coro/Tasks: allocates memory
        };
    } CEU_Value;

    #define _CEU_Dyn_                   \
        CEU_VALUE type;                 \
        uint8_t refs;                   \
        struct CEU_Tags_List* tags;
        
    #if 0
        struct {                        \
            void* block;   /* block/tasks */ \
            union CEU_Dyn* prev;        \
            union CEU_Dyn* next;        \
        } hld;
    #endif
        
    typedef struct CEU_Any {
        _CEU_Dyn_
    } CEU_Any;

    typedef struct CEU_Tuple {
        _CEU_Dyn_
        int its;                // number of items
        CEU_Value buf[0];       // beginning of CEU_Value[n]
    } CEU_Tuple;

    typedef struct CEU_Vector {
        _CEU_Dyn_
        CEU_VALUE unit;         // type of each element
        int max;                // size of buf
        int its;                // number of items
        char* buf;              // resizable Unknown[n]
    } CEU_Vector;
    
    typedef struct CEU_Dict {
        _CEU_Dyn_
        int max;                // size of buf
        CEU_Value (*buf)[0][2]; // resizable CEU_Value[n][2]
    } CEU_Dict;

#if CEU >= 4
    typedef struct CEU_Stack {
        void* me;
        int on;
        struct CEU_Stack* up;
    } CEU_Stack;
#endif
    
    typedef int (*CEU_Proto) (CEUX X);

    #define _CEU_Clo_                   \
        _CEU_Dyn_                       \
        /*struct CEU_Frame* up_frame;*/ \
        CEU_Proto proto;                \
        int args;                       \
        int locs;                       \
        struct {                        \
            int its;                    \
            CEU_Value* buf;             \
        } upvs;

    typedef struct CEU_Clo {
        _CEU_Clo_
    } CEU_Clo;
    
    #if CEU >= 3
    typedef struct CEU_Clo_Exe {
        _CEU_Clo_
        int mem_n;      // space for locals
    } CEU_Clo_Exe;
    #endif
    
    #if CEU >= 2
    typedef struct CEU_Throw {
        _CEU_Dyn_
        CEU_Value val;
        CEU_Value stk;
    } CEU_Throw;
    #endif
    
    #if CEU >= 3
    #define _CEU_Exe_                   \
        _CEU_Dyn_                       \
        CEU_EXE_STATUS status;          \
        struct CEU_Frame frame;         \
        int pc;                         \
        char* mem;
    typedef struct CEU_Exe {
        _CEU_Exe_
    } CEU_Exe;
    #endif
    
    #if CEU >= 4
    typedef struct CEU_Exe_Task {
        _CEU_Exe_
        uint8_t time;
        CEU_Block* dn_block;
        CEU_Value pub;
    } CEU_Exe_Task;
    #endif
    
    #if CEU >= 5
    typedef struct CEU_Tasks {
        _CEU_Dyn_
        int max;
        CEU_Dyns dyns;
    } CEU_Tasks;
    typedef struct CEU_Track {
        _CEU_Dyn_
        CEU_Exe_Task* task;
    } CEU_Track;
    #endif

    typedef union CEU_Dyn {                                                                 
        struct CEU_Any      Any;
        struct CEU_Tuple    Tuple;
        struct CEU_Vector   Vector;
        struct CEU_Dict     Dict;
        struct CEU_Clo      Clo;
    #if CEU >= 3
        struct CEU_Clo_Exe  Clo_Exe;
    #endif
    #if CEU >= 2
        struct CEU_Throw    Throw;
    #endif
    #if CEU >= 3
        struct CEU_Exe      Exe;
    #endif
    #if CEU >= 4
        struct CEU_Exe_Task Exe_Task;
    #endif
    #if CEU >= 5
        struct CEU_Tasks    Tasks;
        struct CEU_Track    Track;
    #endif
    } CEU_Dyn;        
    """
    }
    fun h_tags (): String {
        return """
    typedef struct CEU_Tags_Names {
        int tag;
        char* name;
        struct CEU_Tags_Names* next;
    } CEU_Tags_Names;
    
    typedef struct CEU_Tags_List {
        int tag;
        struct CEU_Tags_List* next;
    } CEU_Tags_List;
    """
    }

    // GLOBALS
    fun c_globals (): String {
        return """
    int CEU_TIME_N = 0;
    uint8_t CEU_TIME_MIN = 0;
    uint8_t CEU_TIME_MAX = 0;
    int CEU_BREAK = 0;

#if CEU >= 4
    CEU_Stack CEU_BSTK = { NULL, 1, NULL };
    CEU_Value id_evt = { CEU_VALUE_NIL };
#endif
#if CEU >= 5
    CEU_Stack CEU_DSTK = { NULL, 1, NULL };
#endif
    //CEU_Block CEU_BLOCK = { 0, {.block=NULL}, { CEU4(NULL COMMA) {NULL,NULL} } };
    """
    }
    fun h_protos (): String {
        return """
    int ceu_type_f (CEUX X);
    int ceu_as_bool (CEU_Value v);
    CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn);

    int ceu_tags_f (CEUX X);
    char* ceu_tag_to_string (int tag);
    int ceu_type_to_size (int type);

    void ceu_gc_inc (CEU_Value v);

    //void ceu_hold_add (CEU_Dyn* dyn, CEU_Block* blk CEU5(COMMA CEU_Dyns* dyns));
    //void ceu_hold_rem (CEU_Dyn* dyn);

    CEU_Value ceu_create_tuple   (int n);
    CEU_Value ceu_create_vector  (void);
    CEU_Value ceu_create_dict    (void);
    CEU_Value ceu_create_clo     (CEU_Proto proto, int args, int locs, int upvs);
    #if CEU >= 4
    CEU_Value ceu_create_track   (CEU_Exe_Task* task);
    #endif

    void ceu_tuple_set (CEU_Tuple* tup, int i, CEU_Value v);

    CEU_Value ceu_vector_get (CEU_Vector* vec, int i);
    void ceu_vector_set (CEU_Vector* vec, int i, CEU_Value v);
    CEU_Value ceu_vector_from_c_string (const char* str);
    
    int ceu_dict_key_to_index (CEU_Dict* col, CEU_Value key, int* idx);
    CEU_Value ceu_dict_get (CEU_Dict* col, CEU_Value key);
    CEU_Value ceu_dict_set (CEU_Dict* col, CEU_Value key, CEU_Value val);
    CEU_Value ceu_col_check (CEU_Value col, CEU_Value idx);

    void ceu_print1 (CEU_Value v);
    CEU_Value _ceu_equals_equals_ (CEU_Value e1, CEU_Value e2);

    #if CEU >= 2
    int ceu_pointer_dash_to_dash_string_f (CEUX X);
    #endif
    #if CEU >= 3
    int ceu_isexe (CEU_Dyn* dyn);
    CEU_Value ceu_dyn_exe_kill (CEU5(CEU_Stack* dstk COMMA) CEU4(CEU_Stack* bstk COMMA) CEU_Dyn* dyn);
    #endif
    #if CEU >= 4
    CEU_Value ceu_bcast_task (CEU5(CEU_Stack* dstk COMMA) CEU_Stack* bstk, uint8_t now, CEU_Exe_Task* task, int n);
    CEU_Exe_Task* ceu_task_up_task (CEU_Exe_Task* task);
    int ceu_istask_dyn (CEU_Dyn* dyn);
    int ceu_istask_val (CEU_Value val);
    #endif
    """
    }
    fun dumps (): String {
        return """
#ifdef CEU_DEBUG
    struct {
        int alloc;
        int free;
        int gc;
    } CEU_GC = { 0, 0, 0 };
    
    void ceu_dump_gc (void) {
        printf(">>> GC: %d\n", CEU_GC.alloc - CEU_GC.free);
        printf("    alloc = %d\n", CEU_GC.alloc);
        printf("    free  = %d\n", CEU_GC.free);
        printf("    gc    = %d\n", CEU_GC.gc);
    }
    #if 0
    void ceu_dump_frame (CEU_Frame* frame) {
        printf(">>> FRAME: %p\n", frame);
        printf("    up_block = %p\n", frame->up_block);
        printf("    clo      = %p\n", frame->clo);
    #if CEU >= 4
        printf("    exe      = %p\n", frame->exe);
    #endif
    }
    #endif
    void ceu_dump_value (CEU_Value v) {
        puts(">>>>>>>>>>>");
        ceu_print1(v);
        puts(" <<<");
        if (v.type > CEU_VALUE_DYNAMIC) {
            printf("    dyn   = %p\n", v.Dyn);
            printf("    type  = %d\n", v.type);
            printf("    refs  = %d\n", v.Dyn->Any.refs);
            //printf("    block = %p\n", CEU_HLD_BLOCK(v.Dyn));
            //printf("    next  = %p\n", v.Dyn->Any.hld.next);
            printf("    ----\n");
            switch (v.type) {
        #if CEU >= 4
                case CEU_VALUE_EXE_TASK:
        #if CEU >= 5
                case CEU_VALUE_EXE_TASK_IN:
        #endif
                    printf("    in     = %d\n", (v.type != CEU_VALUE_EXE_TASK));
                    printf("    status = %d\n", v.Dyn->Exe_Task.status);
                    printf("    pc     = %d\n", v.Dyn->Exe_Task.pc);
                    printf("    pub    = %d\n", v.Dyn->Exe_Task.pub.type);
                    break;
        #endif
        #if CEU >= 5
                case CEU_VALUE_TASKS:
                    printf("    first  = %p\n", v.Dyn->Tasks.dyns.first);
                    printf("    last   = %p\n", v.Dyn->Tasks.dyns.last);
                    break;
                case CEU_VALUE_TRACK:
                    printf("    task   = %p\n", v.Dyn->Track.task);
                    break;
        #endif
                default:
                    puts("TODO");
            }
        }
        puts("<<<<<<<<<<<");
    }
    #if 0
    void ceu_dump_block (CEU_Block* blk) {
        printf(">>> BLOCK: %p\n", blk);
        printf("    istop = %d\n", blk->istop);
        //printf("    up    = %p\n", blk->up.frame);
        CEU_Dyn* cur = blk->dn.dyns.first;
        while (cur != NULL) {
            ceu_dump_value(ceu_dyn_to_val(cur));
            CEU_Dyn* old = cur;
            //cur = old->Any.hld.next;
        }
    }
    #endif
#endif
    """
    }

    // EXIT / ERROR / ASSERT
    fun c_exit_error (): String {
        return """
    #define CEU_ERR_OR(err,v) ({ CEU_Value ceu=v; assert(err.type!=CEU_VALUE_ERROR && "TODO: double error"); (err.type==CEU_VALUE_ERROR ? err : ceu); })
    #define CEU_ERROR_ASR(cmd,v,pre) ({         \
        if (v.type == CEU_VALUE_ERROR) {            \
            CEU_ERROR_THR(cmd,v.Error,pre);         \
        };                                          \
        v;                                          \
    })

    #if CEU <= 1
    #define CEU_ERROR_THR(cmd,msg,pre) {            \
        fprintf(stderr, "%s : %s\n", pre, msg);     \
        ceux_base(0);                               \
        exit(0);                                    \
    }
    #define CEU_ERROR_CHK(cmd,pre) {                    \
        CEU_Value v = ceux_peek(X(-1));                 \
        if (ceux_top()>0 && v.type==CEU_VALUE_ERROR) {  \
            CEU_ERROR_THR(cmd,v.Error,pre);             \
        }                                               \
    }
    #else
    #define CEU_ERROR_THR(cmd,msg,pre) {                                \
        ceux_push(1, (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=pre} });    \
        ceux_push(1, (CEU_Value) { CEU_VALUE_ERROR,   {.Error=msg}   });    \
        cmd;                                                                \
    }
    int ceu_error_chk (char* pre) {
        if (ceux_top()>0 && ceux_peek(X(-1)).type==CEU_VALUE_ERROR) {
            if (pre != NULL) {
                CEU_Value n = ceux_peek(X(-3));
                assert(n.type == CEU_VALUE_NUMBER);
                ceux_repl(X(-3), (CEU_Value) { CEU_VALUE_NUMBER, {.Number=n.Number+1} });
                ceux_shift(X(-3));
                ceux_repl(X(-3), (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=pre} });
            }
            return 1;
        } else {
            return 0;
        }
    }
    #define CEU_ERROR_CHK(cmd,pre)  \
        if (ceu_error_chk(pre)) {   \
            cmd;                    \
        }
    #endif

    int ceu_error_f (CEUX X) {
        assert(X.args == 1);
        CEU_Value arg = ceux_peek(ceux_arg(X,0));
        assert(arg.type == CEU_VALUE_TAG);
        CEU_Value ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error=ceu_tag_to_string(arg.Tag)} };
        ceux_push(1, ret);
        return 1;
    }        
    """
    }
    val c_throw = (CEU >= 2).cond { """
    #if CEU >= 2
    int ceu_throw_f (CEUX X) {
        assert(X.args == 1);
        ceux_push(1, (CEU_Value) { CEU_VALUE_NUMBER, {.Number=0} });
        ceux_push(1, ceux_peek(ceux_arg(X,0)));
        ceux_push(1, (CEU_Value) { CEU_VALUE_ERROR, {.Error=NULL} });
        return 3;
    }
    #endif
    """ }

    // GC
    fun gc (): String {
        return """
#ifdef CEU_DEBUG
    int CEU_DEBUG_TYPE[20] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    void ceu_debug_add (int type) {
    #ifdef CEU_DEBUG
        CEU_GC.alloc++;
    #endif
        CEU_DEBUG_TYPE[type]++;
        //printf(">>> type = %d | count = %d\n", type, CEU_DEBUG_TYPE[type]);
    }
    void ceu_debug_rem (int type) {
    #ifdef CEU_DEBUG
        CEU_GC.free++;
    #endif
        CEU_DEBUG_TYPE[type]--;
        //printf(">>> type = %d | count = %d\n", type, CEU_DEBUG_TYPE[type]);
    }
#else
    #define ceu_debug_add(x)
    #define ceu_debug_rem(x)
#endif

    void ceu_gc_dec_rec (CEU_Dyn* dyn);
    void ceu_gc_rem (CEU_Dyn* dyn);
    void ceu_gc_free (CEU_Dyn* dyn);
    
    void ceu_gc_dec (CEU_Value v) {
        if (v.type < CEU_VALUE_DYNAMIC)
            return;
        assert(v.Dyn->Any.refs > 0);
        v.Dyn->Any.refs--;
        if (v.Dyn->Any.refs == 0) {
    #if CEU >= 5
            assert(v.type != CEU_VALUE_EXE_TASK_IN);
    #endif
            ceu_gc_rem(v.Dyn);
    #ifdef CEU_DEBUG
            CEU_GC.gc++;
    #endif
        }
    }

    void ceu_gc_inc (CEU_Value v) {
        if (v.type < CEU_VALUE_DYNAMIC)
            return;
        assert(v.Dyn->Any.refs < 255);
        v.Dyn->Any.refs++;
    }

    ///
    
    void ceu_gc_rem (CEU_Dyn* dyn) {
    #if CEU >= 3
        CEU_Value ret = ceu_dyn_exe_kill(CEU5(NULL COMMA) CEU4(NULL COMMA) dyn);
        assert(ret.type!=CEU_VALUE_ERROR && "TODO: impossible case");
    #endif
        ceu_gc_dec_rec(dyn);
        //ceu_hold_rem(dyn);
        ceu_gc_free(dyn);
    }

    void ceu_gc_dec_rec (CEU_Dyn* dyn) {
        switch (dyn->Any.type) {
            case CEU_VALUE_CLO_FUNC:
    #if CEU >= 3
            case CEU_VALUE_CLO_CORO:
    #endif
    #if CEU >= 4
            case CEU_VALUE_CLO_TASK:
    #endif
                for (int i=0; i<dyn->Clo.upvs.its; i++) {
                    ceu_gc_dec(dyn->Clo.upvs.buf[i]);
                }
                break;
            case CEU_VALUE_TUPLE:
                for (int i=0; i<dyn->Tuple.its; i++) {
                    ceu_gc_dec(dyn->Tuple.buf[i]);
                }
                break;
            case CEU_VALUE_VECTOR:
                for (int i=0; i<dyn->Vector.its; i++) {
                    CEU_Value ret = ceu_vector_get(&dyn->Vector, i);
                    assert(ret.type != CEU_VALUE_ERROR);
                    ceu_gc_dec(ret);
                }
                break;
            case CEU_VALUE_DICT:
                for (int i=0; i<dyn->Dict.max; i++) {
                    ceu_gc_dec((*dyn->Dict.buf)[i][0]);
                    ceu_gc_dec((*dyn->Dict.buf)[i][1]);
                }
                break;
    #if CEU >= 3
            case CEU_VALUE_EXE_CORO:
    #if CEU >= 4
            case CEU_VALUE_EXE_TASK:
    #endif
    #if CEU >= 5
            case CEU_VALUE_EXE_TASK_IN:
    #endif
                ceu_gc_dec(ceu_dyn_to_val((CEU_Dyn*)dyn->Exe.frame.clo));
                break;
    #endif
    #if CEU >= 5
            case CEU_VALUE_TRACK:
                // dyn->Track.task is a weak reference
                break;
            case CEU_VALUE_TASKS:
                //assert(0 && "TODO: tasks should never reach refs==0");
                break;
    #endif
            default:
                assert(0);
                break;
        }
    }        

    void ceu_gc_free (CEU_Dyn* dyn) {
        while (dyn->Any.tags != NULL) {
            CEU_Tags_List* tag = dyn->Any.tags;
            dyn->Any.tags = tag->next;
            free(tag);
        }
        switch (dyn->Any.type) {
            case CEU_VALUE_CLO_FUNC:
#if CEU >= 3
            case CEU_VALUE_CLO_CORO:
#endif
#if CEU >= 4
            case CEU_VALUE_CLO_TASK:
#endif
                free(dyn->Clo.upvs.buf);
                break;
            case CEU_VALUE_TUPLE:       // buf w/ dyn
                break;
            case CEU_VALUE_VECTOR:
                free(dyn->Vector.buf);
                break;
            case CEU_VALUE_DICT:
                free(dyn->Dict.buf);
                break;
#if CEU >= 3
            case CEU_VALUE_EXE_CORO: {
#if CEU >= 4
            case CEU_VALUE_EXE_TASK:
#endif
#if CEU >= 5
            case CEU_VALUE_EXE_TASK_IN:
#endif
                free(dyn->Exe.mem);
                break;
            }
#endif
#if CEU >= 5
            case CEU_VALUE_TASKS: {
                CEU_Dyn* cur = dyn->Tasks.dyns.first;
                CEU_Dyn* nxt = NULL;
                while (cur != NULL) {
                    nxt = cur->Any.hld.next;
                    ceu_gc_free(cur);
                    cur = nxt;
                }
                break;
            }
            case CEU_VALUE_TRACK:
                break;
#endif
            default:
                assert(0 && "bug found");
        }
        ceu_debug_rem(dyn->Any.type);
        free(dyn);
    }        
    """
    }

    // TAGS
    fun c_tags (): String {
        return tags.pub.values.let {
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
        }
    }

    // CEUX
    val h1_ceux = """
    typedef struct CEUX {
        int base;   // index above args
        int args;   // number of args
    } CEUX;
    
    /*
     *  CLO
     *  args
     *  ----    <-- base
     *  upvs
     *  locs    <-- [b1,n1[ [b2,n2[ [...[
     *  block
     *  tmps
     *  block
     *  tmps
     */
    """
    val h2_ceux = """
    #define ceux_arg(X,i) (X.base - X.args + i)
    int X (int i);
    int ceux_top (void);
    void ceux_base (int base);
    void ceux_push (int inc, CEU_Value v);
    CEU_Value ceux_peek (int i);
    void ceux_repl (int i, CEU_Value v);
    void ceux_shift (int I);
    void ceux_drop (int n);        
    """
    val c_ceux = """
    #define CEU_VSTK_MAX $STACK
    CEU_Value ceux_buf[CEU_VSTK_MAX];
    int ceux_n = 0;
            
    int X (int i) {
        assert(i < 0);
        return ceux_n + i;
    };

    void ceux_dump (int n) {
        for (int i=n; i<ceux_n; i++) {
            printf(">>> [%d]: [%d] ", i, ceux_peek(i).type);
            ceu_print1(ceux_peek(i));
            puts("");
        }
    }
    int ceux_top (void) {
        return ceux_n;
    }
    void ceux_push (int inc, CEU_Value v) {
        assert(ceux_n<CEU_VSTK_MAX && "TODO: stack error");
        if (inc) {
            ceu_gc_inc(v);
        }
        ceux_buf[ceux_n++] = v;
    }
    CEU_Value ceux_pop (int dec) {
        assert(ceux_n>0 && "TODO: stack error");
        CEU_Value v = ceux_buf[--ceux_n];
        if (dec) {
            ceu_gc_dec(v);
        }
        return v;
    }
    CEU_Value ceux_peek (int i) {
        assert(i>=0 && i<ceux_n && "TODO: stack error");
        return ceux_buf[i];
    }
    void ceux_drop (int n) {
        assert(n<=ceux_n && "BUG: index out of range");
        for (int i=0; i<n; i++) {
            ceu_gc_dec(ceux_buf[--ceux_n]);
        }
    }
    void ceux_base (int base) {
        assert(base>=0 && base<=ceux_n && "TODO: stack error");
        for (int i=ceux_n; i>base; i--) {
            ceu_gc_dec(ceux_buf[--ceux_n]);
        }
    }
    void ceux_repl (int i, CEU_Value v) {
        assert(i>=0 && i<ceux_n && "TODO: stack error");
        ceu_gc_inc(v);
        ceu_gc_dec(ceux_buf[i]);
        ceux_buf[i] = v;
    }
    void ceux_copy (int i, int j) {
        assert(i>=0 && i<ceux_n && "TODO: stack error");
        assert(j>=0 && j<ceux_n && "TODO: stack error");
        assert(i!=j && "TODO: invalid move");
        ceu_gc_dec(ceux_buf[i]);
        ceux_buf[i] = ceux_buf[j];
    }
    void ceux_move (int i, int j) {
        assert(i>=0 && i<ceux_n && "TODO: stack error");
        assert(j>=0 && j<ceux_n && "TODO: stack error");
        assert(i!=j && "TODO: invalid move");
        ceu_gc_dec(ceux_buf[i]);
        ceux_buf[i] = ceux_buf[j];
        ceux_buf[j] = (CEU_Value) { CEU_VALUE_NIL };
    }

    void ceux_shift (int I) {
        assert(I>=0 && I<=ceux_n && "TODO: stack error");
        for (int i=ceux_n; i>I; i--) {
            ceux_buf[i] = ceux_buf[i-1];
        }
        ceux_buf[I] = (CEU_Value) { CEU_VALUE_NIL };
        ceux_n++;
    }
    
    // ceux_block_*
    //  - needs to clear locals on enter and leave
    //  - enter: initialize all vars to nil (prevents garbage)
    //  - leave: gc locals
    
    void ceux_block_enter (int base, int n) {
        // clear locals
        // TODO: use memset=0
        for (int i=0; i<n; i++) {
            ceux_repl(base+i, (CEU_Value) { CEU_VALUE_NIL });
        }        
        ceux_push(1, (CEU_Value) { CEU_VALUE_BLOCK });
    }
    
    void ceux_block_leave (int base, int n, int out) {
        // clear locals
        // TODO: use memset=0
        for (int i=0; i<n; i++) {
            ceux_repl(base+i, (CEU_Value) { CEU_VALUE_NIL });
        }

        int I = -1;
        for (int i=ceux_n-1; i>=0; i--) {
            if (ceux_peek(i).type == CEU_VALUE_BLOCK) {
                I = i;
                break;
            }
        }
        assert(I >= 0);
        
    #if CEU >= 2
        // in case of error, out must be readjusted to the error stack:
        // [BLOCK,...,n,pay,err]
        //  - ... - error messages
        //  - n   - number of error messages
        //  - pay - error payload
        //  - err - error value
        if (ceux_peek(X(-1)).type == CEU_VALUE_ERROR) {
            CEU_Value n = ceux_peek(X(-3));
            assert(n.type == CEU_VALUE_NUMBER);
            out = n.Number + 1 + 1 + 1;
        }
    #endif

        for (int i=0; i<out; i++) {
            ceux_copy(I+i, X(-i-1));
        }
        ceux_base(I + out);
    }
    
    int ceux_call (int inp, int out) {
        // [clo,args]
        CEU_Value clo = ceux_peek(X(-inp-1));
        if (clo.type != CEU_VALUE_CLO_FUNC) {
            ceux_push(1, (CEU_Value) { CEU_VALUE_ERROR, {.Error="call error : expected function"} });
            return 1;
        }

        // fill missing args with nils
        {
            int N = clo.Dyn->Clo.args - inp;
            //printf(">>> %d\inp", N);
            for (int i=0; i<N; i++) {
                ceux_push(1, (CEU_Value) { CEU_VALUE_NIL });
                inp++;
            }
        }

        int base = ceux_n;

        // [clo,args,?]
        //           ^ base

        for (int i=0; i<clo.Dyn->Clo.upvs.its; i++) {
            ceux_push(1, clo.Dyn->Clo.upvs.buf[i]);
        }
        for (int i=0; i<clo.Dyn->Clo.locs; i++) {
            ceux_push(1, (CEU_Value) { CEU_VALUE_NIL });
        }
        
        // [clo,args,upvs,locs]
        //           ^ base
        
        //CEU_Frame frame = { NULL, &clo.Dyn->Clo CEU3(COMMA {.exe=NULL}) };
        int ret = clo.Dyn->Clo.proto((CEUX) { base, inp });
        
    #if CEU >= 2
        // in case of error, out must be readjusted to the error stack:
        // [clo,args,upvs,locs,...,n,pay,err]
        //  - ... - error messages
        //  - n   - number of error messages
        //  - pay - error payload
        //  - err - error value
        if (ceux_peek(X(-1)).type == CEU_VALUE_ERROR) {
            CEU_Value n = ceux_peek(X(-3));
            assert(n.type == CEU_VALUE_NUMBER);
            out = n.Number + 1 + 1 + 1;
        }
    #endif

        // [clo,args,upvs,locs,rets]
        //           ^ base
        
        if (out == CEU_MULTI) {     // any rets is ok
            out = ret;
        } else if (ret < out) {     // less rets than requested
           // fill rets up to outs
            for (int i=0; i<out-ret; i++) {
                ceux_push(1, (CEU_Value) { CEU_VALUE_NIL });
            }
            out = out;
        } else {                    // enough rets than requested
            // ret >= out
            out = out;
        }
        
        // [clo,args,upvs,locs,out]
        //           ^ base
        
        // move rets to begin, replacing [clo,args,upvs,locs]
        for (int i=0; i<out; i++) {
            ceux_move(base-inp-1+i, ceux_n-out+i);
        }
        
        // [outs,x,x,x,x]
        //           ^ base

        ceux_base(base-inp-1+out);
        
        // [outs]
        //      ^ base
        
        return out;
    }
    """

    // IMPLS
    fun c_impls (): String {
        return """
    CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn) {
        return (CEU_Value) { dyn->Any.type, {.Dyn=dyn} };
    }
    
    int ceu_dump_f (CEUX X) {
        assert(X.args == 1);
    #ifdef CEU_DEBUG
        ceu_dump_value(ceux_peek(ceux_arg(X,0)));
        return 0;
    #else
        ceux_push(1, (CEU_Value) { CEU_VALUE_ERROR, {.Error="debug is off"} });
        return 1;
    #endif
    }

    int ceu_as_bool (CEU_Value v) {
        return !(v.type==CEU_VALUE_NIL || (v.type==CEU_VALUE_BOOL && !v.Bool));
    }
    int ceu_type_f (CEUX X) {
        assert(X.args==1 && "bug found");
        int type = ceux_peek(ceux_arg(X,0)).type;
        ceux_push(1, (CEU_Value) { CEU_VALUE_TAG, {.Tag=type} });
        return 1;
    }
    
    CEU_Value _ceu_sup_ (CEU_Value sup, CEU_Value sub) {
        assert(sup.type == CEU_VALUE_TAG);
        assert(sub.type == CEU_VALUE_TAG);
        
        //printf("sup=0x%08X vs sub=0x%08X\n", sup->Tag, sub->Tag);
        int sup0 = sup.Tag & 0x000000FF;
        int sup1 = sup.Tag & 0x0000FF00;
        int sup2 = sup.Tag & 0x00FF0000;
        int sup3 = sup.Tag & 0xFF000000;
        int sub0 = sub.Tag & 0x000000FF;
        int sub1 = sub.Tag & 0x0000FF00;
        int sub2 = sub.Tag & 0x00FF0000;
        int sub3 = sub.Tag & 0xFF000000;

        return (CEU_Value) { CEU_VALUE_BOOL, { .Bool =
            (sup0 == sub0) && ((sup1 == 0) || (
                (sup1 == sub1) && ((sup2 == 0) || (
                    (sup2 == sub2) && ((sup3 == 0) || (
                        (sup3 == sub3)
                    ))
                ))
            ))
        } };
    }
    int ceu_sup_question__f (CEUX X) {
        assert(X.args >= 2);
        CEU_Value sup = ceux_peek(ceux_arg(X,0));
        CEU_Value sub = ceux_peek(ceux_arg(X,1));
        CEU_Value ret = _ceu_sup_(sup, sub);
        ceux_push(1, ret);
        return 1;
    }
    
    CEU_Value _ceu_tags_all_ (CEU_Value dyn) {
        int len = 0; {
            CEU_Tags_List* cur = dyn.Dyn->Any.tags;
            while (cur != NULL) {
                len++;
                cur = cur->next;
            }
        }
        CEU_Value tup = ceu_create_tuple(len);
        {
            CEU_Tags_List* cur = dyn.Dyn->Any.tags;
            int i = 0;
            while (cur != NULL) {
                ceu_tuple_set(&tup.Dyn->Tuple, i++, (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} });
                cur = cur->next;
            }
        }
        return tup;
    }
        
    int ceu_tags_f (CEUX X) {
        assert(X.args >= 1);
        CEU_Value dyn = ceux_peek(ceux_arg(X,0));
        assert(dyn.type > CEU_VALUE_DYNAMIC);
        CEU_Value tag; // = (CEU_Value) { CEU_VALUE_NIL };
        if (X.args >= 2) {
            tag = ceux_peek(ceux_arg(X,1));
            assert(tag.type == CEU_VALUE_TAG);
        }
        
        CEU_Value f_chk () {
            CEU_Value ret = { CEU_VALUE_BOOL, {.Bool=0} };
            CEU_Tags_List* cur = dyn.Dyn->Any.tags;
            while (cur != NULL) {
                CEU_Value sub = { CEU_VALUE_TAG, {.Tag=cur->tag} };
                ret = _ceu_sup_(tag, sub);
                if (ret.Bool) {
                    break;
                }
                cur = cur->next;
            }
            return ret;
        }
        
        void f_set (int on) {
            if (on) {   // add
                CEU_Value has = f_chk();
                if (!has.Bool) {
                    CEU_Tags_List* v = malloc(sizeof(CEU_Tags_List));
                    assert(v != NULL);
                    v->tag = tag.Tag;
                    v->next = dyn.Dyn->Any.tags;
                    dyn.Dyn->Any.tags = v;
                }
            } else {            // rem
                CEU_Tags_List** cur = &dyn.Dyn->Any.tags;
                while (*cur != NULL) {
                    if ((*cur)->tag == tag.Tag) {
                        CEU_Tags_List* v = *cur;
                        *cur = v->next;
                        free(v);
                        break;
                    }
                    cur = &(*cur)->next;
                }
            }
        }
        
        switch (X.args) {
            case 1: {   // all tags
                CEU_Value ret = _ceu_tags_all_(dyn);
                ceux_push(1, ret);
                break;
            }
            case 2: {   // check tag
                CEU_Value ret = f_chk();
                ceux_push(1, ret);
                break;
            }
            default: {   // add/rem tag
                CEU_Value bool = ceux_peek(ceux_arg(X,2));
                assert(bool.type == CEU_VALUE_BOOL);
                f_set(bool.Bool);
                ceux_push(1, ceux_peek(ceux_arg(X,0)));  // keep dyn
                break;
            }
        }
        return 1;
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
    int ceu_string_dash_to_dash_tag_f (CEUX X) {
        assert(X.args == 1);
        CEU_Value str = ceux_peek(ceux_arg(X,0));
        assert(str.type==CEU_VALUE_VECTOR && str.Dyn->Vector.unit==CEU_VALUE_CHAR);
        CEU_Tags_Names* cur = CEU_TAGS;
        CEU_Value ret = (CEU_Value) { CEU_VALUE_NIL };
        while (cur != NULL) {
            if (!strcmp(cur->name,str.Dyn->Vector.buf)) {
                ret = (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} };
                break;
            }
            cur = cur->next;
        }
        ceux_push(1, ret);
        return 1;
    }


    CEU_Value ceu_pointer_dash_to_dash_string (const char* ptr) {
        CEU_Value str = ceu_create_vector();
        int len = strlen(ptr);
        for (int i=0; i<len; i++) {
            CEU_Value chr = { CEU_VALUE_CHAR, {.Char=ptr[i]} };
            ceu_vector_set(&str.Dyn->Vector, i, chr);
        }
        return str;
    }

    #if CEU >= 2
    int ceu_pointer_dash_to_dash_string_f (CEUX X) {
        assert(X.args == 1);
        CEU_Value ptr = ceux_peek(ceux_arg(X,0));
        assert(ptr.type == CEU_VALUE_POINTER);
        ceux_push(1, ceu_pointer_dash_to_dash_string(ptr.Pointer));
        return 1;
    }
    #endif
    """
    }
    fun tuple_vector_dict (): String {
        return """
    #define ceu_sizeof(type, member) sizeof(((type *)0)->member)
    int ceu_type_to_size (int type) {
        switch (type) {
            case CEU_VALUE_NIL:
                return 0;
            case CEU_VALUE_ERROR:
                return ceu_sizeof(CEU_Value, Error);
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
            default:
                return ceu_sizeof(CEU_Value, Dyn);
        }
    }
    
    CEU_Value ceu_col_get (CEU_Value col, CEU_Value key) {
        CEU_Value err = ceu_col_check(col,key);
        if (err.type == CEU_VALUE_ERROR) {
            return err;
        }
        switch (col.type) {
            case CEU_VALUE_TUPLE:
                return col.Dyn->Tuple.buf[(int) key.Number];
            case CEU_VALUE_VECTOR:
                return ceu_vector_get(&col.Dyn->Vector, key.Number);
                break;
            case CEU_VALUE_DICT:
                return ceu_dict_get(&col.Dyn->Dict, key);
            default:
                assert(0 && "bug found");
        }
    }
    
    CEU_Value ceu_col_set (CEU_Value col, CEU_Value key, CEU_Value val) {
        CEU_Value ok = { CEU_VALUE_NIL };
        switch (col.type) {
            case CEU_VALUE_TUPLE:
                ceu_tuple_set(&col.Dyn->Tuple, key.Number, val);
                break;
            case CEU_VALUE_VECTOR:
                ceu_vector_set(&col.Dyn->Vector, key.Number, val);
                break;
            case CEU_VALUE_DICT: {
                ok = ceu_dict_set(&col.Dyn->Dict, key, val);
                break;
            }
            default:
                assert(0 && "bug found");
        }
        return ok;
    }
    
    void ceu_tuple_set (CEU_Tuple* tup, int i, CEU_Value v) {
        ceu_gc_inc(v);
        ceu_gc_dec(tup->buf[i]);
        tup->buf[i] = v;
    }
    
    CEU_Value ceu_vector_get (CEU_Vector* vec, int i) {
        if (i<0 || i>=vec->its) {
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error="index error : out of bounds"} };
        }
        int sz = ceu_type_to_size(vec->unit);
        CEU_Value ret = (CEU_Value) { vec->unit };
        memcpy(&ret.Number, vec->buf+i*sz, sz);
        return ret;
    }
    
    void ceu_vector_set (CEU_Vector* vec, int i, CEU_Value v) {
        if (v.type == CEU_VALUE_NIL) {           // pop
            assert(i == vec->its-1);
            CEU_Value ret = ceu_vector_get(vec, i);
            assert(ret.type != CEU_VALUE_ERROR);
            ceu_gc_dec(ret);
            vec->its--;
        } else {
            if (vec->its == 0) {
                vec->unit = v.type;
            } else {
                assert(v.type == vec->unit);
            }
            int sz = ceu_type_to_size(vec->unit);
            if (i == vec->its) {           // push
                if (i == vec->max) {
                    vec->max = vec->max*2 + 1;    // +1 if max=0
                    vec->buf = realloc(vec->buf, vec->max*sz + 1);
                    assert(vec->buf != NULL);
                }
                ceu_gc_inc(v);
                vec->its++;
                vec->buf[sz*vec->its] = '\0';
            } else {                            // set
                CEU_Value ret = ceu_vector_get(vec, i);
                assert(ret.type != CEU_VALUE_ERROR);
                ceu_gc_inc(v);
                ceu_gc_dec(ret);
                assert(i < vec->its);
            }
            memcpy(vec->buf + i*sz, (char*)&v.Number, sz);
        }
    }
    
    CEU_Value ceu_vector_from_c_string (const char* str) {
        CEU_Value vec = ceu_create_vector();
        int N = strlen(str);
        for (int i=0; i<N; i++) {
            ceu_vector_set(&vec.Dyn->Vector, vec.Dyn->Vector.its, (CEU_Value) { CEU_VALUE_CHAR, {.Char=str[i]} });
        }
        return vec;
    }

    int ceu_next_dash_dict_f (CEUX X) {
        assert(X.args==1 || X.args==2);
        CEU_Value dict = ceux_peek(ceux_arg(X,0));
        CEU_Value ret;
        if (dict.type != CEU_VALUE_DICT) {
            ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="next-dict error : expected dict"} };
        } else {
            CEU_Value key = (X.args == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : ceux_peek(ceux_arg(X,1));
            if (key.type == CEU_VALUE_NIL) {
                ret = (*dict.Dyn->Dict.buf)[0][0];
            } else {
                ret = (CEU_Value) { CEU_VALUE_NIL };
                for (int i=0; i<dict.Dyn->Dict.max-1; i++) {     // -1: last element has no next
                    CEU_Value ok = _ceu_equals_equals_(key, (*dict.Dyn->Dict.buf)[i][0]);
                    assert(ok.type != CEU_VALUE_ERROR);
                    if (ok.Bool) {
                        ret = (*dict.Dyn->Dict.buf)[i+1][0];
                        break;
                    }
                }
            }
        }
        ceux_push(1, ret);
        return 1;
    }
    
#if CEU >= 5
    CEU_Value _ceu_next_tasks_f_ (CEUX X) {
        assert(n==1 || n==2);
        CEU_Value tsks = args[0];
        if (tsks.type != CEU_VALUE_TASKS) {
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error="next-tasks error : expected tasks"} };
        }
        CEU_Value key = (n == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : args[1];
        CEU_Dyn* nxt = NULL;
        switch (key.type) {
            case CEU_VALUE_NIL:
                nxt = tsks.Dyn->Tasks.dyns.first;
                break;
            case CEU_VALUE_TRACK:
                if (key.Dyn->Track.task==NULL) {
                    return (CEU_Value) { CEU_VALUE_NIL };
                } else if (key.Dyn->Track.task->type != CEU_VALUE_EXE_TASK_IN) {
                    return (CEU_Value) { CEU_VALUE_ERROR, {.Error="next-tasks error : expected task-in-pool track"} };
                }
                nxt = key.Dyn->Track.task->hld.next;
                break;
            default:
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="next-tasks error : expected task-in-pool track"} };
        }
        if (nxt == NULL) {
            return (CEU_Value) { CEU_VALUE_NIL };
        } else {
            -=- TODO: gc_inc -=-
            return ceu_create_track(&nxt->Exe_Task);
        }
    }
    CEU_Value ceu_next_tasks_f (CEUX X) {
        -=- TODO -=-
        CEU_Value ret = _ceu_next_tasks_f_(frame, n, args);
        ceu_gc_dec_args(n, args);
        return ret;
    }
#endif

    int ceu_dict_key_to_index (CEU_Dict* col, CEU_Value key, int* idx) {
        *idx = -1;
        for (int i=0; i<col->max; i++) {
            CEU_Value cur = (*col->buf)[i][0];
            CEU_Value ret = _ceu_equals_equals_(key, cur);
            assert(ret.type != CEU_VALUE_ERROR);
            if (ret.Bool) {
                *idx = i;
                return 1;
            } else {
                if (*idx==-1 && cur.type==CEU_VALUE_NIL) {
                    *idx = i;
                }
            }
        }
        return 0;
    }        
    CEU_Value ceu_dict_get (CEU_Dict* col, CEU_Value key) {
        int i;
        int ok = ceu_dict_key_to_index(col, key, &i);
        if (ok) {
            return (*col->buf)[i][1];
        } else {
            return (CEU_Value) { CEU_VALUE_NIL };
        }
    }        
    CEU_Value ceu_dict_set (CEU_Dict* col, CEU_Value key, CEU_Value val) {
        if (key.type == CEU_VALUE_NIL) {
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error="dict error : index cannot be nil"} };
        }
        int old;
        ceu_dict_key_to_index(col, key, &old);
        if (old == -1) {
            old = col->max;
            int new = MAX(5, old * 2);
            col->max = new;
            col->buf = realloc(col->buf, new*2*sizeof(CEU_Value));
            assert(col->buf != NULL);
            memset(&(*col->buf)[old], 0, (new-old)*2*sizeof(CEU_Value));  // x[i]=nil
        }
        assert(old != -1);
        
        CEU_Value vv = ceu_dict_get(col, key);
        
        if (val.type == CEU_VALUE_NIL) {
            ceu_gc_dec(vv);
            ceu_gc_dec(key);
            (*col->buf)[old][0] = (CEU_Value) { CEU_VALUE_NIL };
        } else {
            ceu_gc_inc(val);
            ceu_gc_dec(vv);
            if (vv.type == CEU_VALUE_NIL) {
                ceu_gc_inc(key);
            }
            (*col->buf)[old][0] = key;
            (*col->buf)[old][1] = val;
        }
        return (CEU_Value) { CEU_VALUE_NIL };
    }        
    
    CEU_Value ceu_col_check (CEU_Value col, CEU_Value idx) {
        if (col.type<CEU_VALUE_TUPLE || col.type>CEU_VALUE_DICT) {                
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error="index error : expected collection"} };
        }
        if (col.type != CEU_VALUE_DICT) {
            if (idx.type != CEU_VALUE_NUMBER) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="index error : expected number"} };
            }
            if (col.type==CEU_VALUE_TUPLE && (idx.Number<0 || idx.Number>=col.Dyn->Tuple.its)) {                
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="index error : out of bounds"} };
            }
            if (col.type==CEU_VALUE_VECTOR && (idx.Number<0 || idx.Number>col.Dyn->Vector.its)) {                
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="index error : out of bounds"} };
            }
        }
        return (CEU_Value) { CEU_VALUE_NIL };
    }
    """
    }
    fun creates (): String {
        return """
    CEU_Value ceu_create_tuple (int n) {
        ceu_debug_add(CEU_VALUE_TUPLE);
        CEU_Tuple* ret = malloc(sizeof(CEU_Tuple) + n*sizeof(CEU_Value));
        assert(ret != NULL);
        *ret = (CEU_Tuple) {
            CEU_VALUE_TUPLE, 0, NULL,
            n, {}
        };
        memset(ret->buf, 0, n*sizeof(CEU_Value));
        //ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
        return (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ret} };
    }
    
    int ceu_tuple_f (CEUX X) {
        assert(X.args == 1);
        CEU_Value arg = ceux_peek(ceux_arg(X,0));
        assert(arg.type == CEU_VALUE_NUMBER);
        CEU_Value ret = ceu_create_tuple(arg.Number);
        ceux_push(1, ret);
        return 1;
    }
    
    CEU_Value ceu_create_vector (void) {
        ceu_debug_add(CEU_VALUE_VECTOR);
        CEU_Vector* ret = malloc(sizeof(CEU_Vector));
        assert(ret != NULL);
        char* buf = malloc(1);  // because of '\0' in empty strings
        assert(buf != NULL);
        buf[0] = '\0';
        *ret = (CEU_Vector) {
            CEU_VALUE_VECTOR, 0,  NULL,
            0, 0, CEU_VALUE_NIL, buf
        };
        //ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
        return (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=(CEU_Dyn*)ret} };
    }
    
    CEU_Value ceu_create_dict (void) {
        ceu_debug_add(CEU_VALUE_DICT);
        CEU_Dict* ret = malloc(sizeof(CEU_Dict));
        assert(ret != NULL);
        *ret = (CEU_Dict) {
            CEU_VALUE_DICT, 0, NULL,
            0, NULL
        };
        //ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
        return (CEU_Value) { CEU_VALUE_DICT, {.Dyn=(CEU_Dyn*)ret} };
    }
    
    CEU_Value _ceu_create_clo_ (int sz, int type, CEU_Proto proto, int args, int locs, int upvs) {
        ceu_debug_add(type);
        CEU_Clo* ret = malloc(sz);
        assert(ret != NULL);
        CEU_Value* buf = malloc(upvs * sizeof(CEU_Value));
        assert(buf != NULL);
        for (int i=0; i<upvs; i++) {
            buf[i] = (CEU_Value) { CEU_VALUE_NIL };
        }
        *ret = (CEU_Clo) {
            type, 0, NULL,
            proto,
            args, locs, { upvs, buf }
        };
        //ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
        return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
    }

    CEU_Value ceu_create_clo (CEU_Proto proto, int args, int locs, int upvs) {
        return _ceu_create_clo_(sizeof(CEU_Clo), CEU_VALUE_CLO_FUNC, proto, args, locs, upvs);
    }

    #if CEU >= 3
    CEU_Value ceu_create_clo_exe (int type, CEU_Proto proto, int upvs) {
        CEU_Value clo = _ceu_create_clo_(sizeof(CEU_Clo_Exe), proto, upvs);
        clo.Dyn->Clo_Exe.mem_n = 0;
        return clo;
    }
    #endif

    #if CEU >= 3
    CEU_Value _ceu_create_exe_ (int type, int sz, CEU_Value clo CEU5(COMMA CEU_Dyns* dyns)) {
        ceu_debug_add(type);
        assert(clo.type==CEU_VALUE_CLO_CORO CEU4(|| clo.type==CEU_VALUE_CLO_TASK));
        ceu_gc_inc(clo);
        
        CEU_Exe* ret = malloc(sz);
        assert(ret != NULL);
        char* mem = malloc(clo.Dyn->Clo_Exe.mem_n);
        assert(mem != NULL);
        
        int hld_type = (clo.Dyn->Clo.hld.type <= CEU_HOLD_MUTAB) ? CEU_HOLD_FLEET : clo.Dyn->Clo.hld.type;
        *ret = (CEU_Exe) {
            type, 0, NULL, { hld_type, blk, NULL, NULL },
            CEU_EXE_STATUS_YIELDED, { blk, &clo.Dyn->Clo, {.exe=ret} }, 0, mem
        };
        
        ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA dyns));
        return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
    }
    #endif

    #if CEU >= 4
    CEU_Value _ceu_create_exe_task_ (int type, CEU_Block* blk, CEU_Value clo CEU5(COMMA CEU_Dyns* dyns)) {
        if (clo.type != CEU_VALUE_CLO_TASK) {
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error="spawn error : expected task"} };
        }
        CEU_Value ret = _ceu_create_exe_(type, sizeof(CEU_Exe_Task), blk, clo CEU5(COMMA dyns));
        ret.Dyn->Exe_Task.time = CEU_TIME_MAX;
        ret.Dyn->Exe_Task.dn_block = NULL;
        ret.Dyn->Exe_Task.pub = (CEU_Value) { CEU_VALUE_NIL };
        return ret;
    }

    CEU_Value ceu_create_exe_task (CEU_Block* blk, CEU_Value clo) {
        return _ceu_create_exe_task_(CEU_VALUE_EXE_TASK, blk, clo CEU5(COMMA &blk->dn.dyns));
    }
    #endif
    
    #if CEU >= 5
    CEU_Value ceu_create_exe_task_in (CEU_Block* blk, CEU_Value clo, CEU_Tasks* tasks) {
        int ceu_tasks_n (CEU_Tasks* tasks) {
            int n = 0;
            CEU_Dyn* dyn = tasks->dyns.first;
            while (dyn != NULL) {
                if (dyn->Exe_Task.status < CEU_EXE_STATUS_TERMINATED) {
                    n++;
                }
                dyn = dyn->Any.hld.next;
            }
            return n;
        }
        {
            CEU_Block* ts_blk = CEU_HLD_BLOCK((CEU_Dyn*)tasks);
            char* err = ceu_hold_set_msg(CEU_HOLD_CMD_TSKIN, clo, "spawn error", (ceu_hold_cmd){.Tskin={ts_blk}});
            if (err != NULL) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error=err} };
            }
        }
        if (tasks->max==0 || ceu_tasks_n(tasks)<tasks->max) {
            CEU_Value ret = _ceu_create_exe_task_(CEU_VALUE_EXE_TASK_IN, CEU_HLD_BLOCK((CEU_Dyn*)tasks), clo, &tasks->dyns);
            if (ret.type != CEU_VALUE_ERROR) {
                ret.Dyn->Exe_Task.hld.type = tasks->hld.type; // TODO: not sure 
                ret.Dyn->Any.hld.block = (void*) tasks; // point to tasks (vs enclosing block)
            }
            -=- TODO: gc_inc -=-
            return ret;
        } else {
            return (CEU_Value) { CEU_VALUE_NIL };
        }
    }
    CEU_Value ceu_create_tasks (CEU_Block* blk, int max) {
        ceu_debug_add(CEU_VALUE_TASKS);
        CEU_Tasks* ret = malloc(sizeof(CEU_Tasks));
        assert(ret != NULL);

        *ret = (CEU_Tasks) {
            CEU_VALUE_TASKS, 0, NULL, { blk, NULL, NULL },
            max, { NULL, NULL }
        };
        
        ceu_hold_add((CEU_Dyn*)ret, blk, &blk->dn.dyns);
        return (CEU_Value) { CEU_VALUE_TASKS, {.Dyn=(CEU_Dyn*)ret} };
    }
    CEU_Value ceu_create_track (CEU_Exe_Task* task) {
        ceu_debug_add(CEU_VALUE_TRACK);
        CEU_Track* ret = malloc(sizeof(CEU_Track));
        assert(ret != NULL);
        *ret = (CEU_Track) {
            CEU_VALUE_TRACK, 0, NULL,
            task
        };
        ceu_hold_add((CEU_Dyn*)ret, blk, &blk->dn.dyns);
        return (CEU_Value) { CEU_VALUE_TRACK, {.Dyn=(CEU_Dyn*)ret} };
    }
    
    CEU_Value ceu_tasks_f (CEUX X) {
        -=- TODO -=-
        assert(n <= 1);
        int max = 0;
        if (n == 1) {
            CEU_Value xmax = args[0];
            if (xmax.type!=CEU_VALUE_NUMBER || xmax.Number<=0) {                
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="tasks error : expected positive number"} };
            }
            max = xmax.Number;
        }
        -=- TODO: gc_inc -=-
        return ceu_create_tasks(max);
    }
    #endif
    """
    }
    fun print (): String {
        return """
    void ceu_print1 (CEU_Value v) {
        if (v.type > CEU_VALUE_DYNAMIC) {  // TAGS
            CEU_Value tup = _ceu_tags_all_(v);
            assert(tup.type == CEU_VALUE_TUPLE);
            int N = tup.Dyn->Tuple.its;
            if (N > 0) {
                if (N > 1) {
                    printf("[");
                }
                for (int i=0; i<N; i++) {
                    ceu_print1(tup.Dyn->Tuple.buf[i]);
                    if (i < N-1) {
                        printf(",");
                    }
                }
                if (N > 1) {
                    printf("]");
                }
                printf(" ");
            }
            ceu_gc_rem(tup.Dyn);
        }
        switch (v.type) {
            case CEU_VALUE_BLOCK:
                printf("(block sentinel)");
                break;
            case CEU_VALUE_NIL:
                printf("nil");
                break;
            case CEU_VALUE_ERROR:
                printf("error: %s", (v.Error==NULL ? "(null)" : v.Error));
                break;
            case CEU_VALUE_TAG:
                printf("%s", ceu_tag_to_string(v.Tag));
                break;
            case CEU_VALUE_BOOL:
                if (v.Bool) {
                    printf("true");
                } else {
                    printf("false");
                }
                break;
            case CEU_VALUE_CHAR:
                putchar(v.Char);
                break;
            case CEU_VALUE_NUMBER:
                printf("%g", v.Number);
                break;
            case CEU_VALUE_POINTER:
                printf("pointer: %p", v.Pointer);
                break;
            case CEU_VALUE_TUPLE:
                printf("[");
                for (int i=0; i<v.Dyn->Tuple.its; i++) {
                    if (i > 0) {
                        printf(",");
                    }
                    ceu_print1(v.Dyn->Tuple.buf[i]);
                }                    
                printf("]");
                break;
            case CEU_VALUE_VECTOR:
                if (v.Dyn->Vector.unit == CEU_VALUE_CHAR) {
                    printf("%s", v.Dyn->Vector.buf);
                } else {
                    printf("#[");
                    for (int i=0; i<v.Dyn->Vector.its; i++) {
                        if (i > 0) {
                            printf(",");
                        }
                        CEU_Value ret = ceu_vector_get(&v.Dyn->Vector, i);
                        assert(ret.type != CEU_VALUE_ERROR);
                        ceu_print1(ret);
                    }                    
                    printf("]");
                }
                break;
            case CEU_VALUE_DICT:
                printf("@[");
                int comma = 0;
                for (int i=0; i<v.Dyn->Dict.max; i++) {
                    if ((*v.Dyn->Dict.buf)[i][0].type != CEU_VALUE_NIL) {
                        if (comma != 0) {
                            printf(",");
                        }
                        comma = 1;
                        printf("(");
                        ceu_print1((*v.Dyn->Dict.buf)[i][0]);
                        printf(",");
                        ceu_print1((*v.Dyn->Dict.buf)[i][1]);
                        printf(")");
                    }
                }                    
                printf("]");
                break;
            case CEU_VALUE_CLO_FUNC:
                printf("func: %p", v.Dyn);
                if (v.Dyn->Clo.upvs.its > 0) {
                    printf(" | [");
                    for (int i=0; i<v.Dyn->Clo.upvs.its; i++) {
                        if (i > 0) {
                            printf(",");
                        }
                        ceu_print1(v.Dyn->Clo.upvs.buf[i]);
                    }
                    printf("]");
                }
                break;
    #if CEU >= 3
            case CEU_VALUE_CLO_CORO:
                printf("coro: %p", v.Dyn);
                break;
    #endif
    #if CEU >= 4
            case CEU_VALUE_CLO_TASK:
                printf("task: %p", v.Dyn);
                break;
    #endif
    #if CEU >= 3
            case CEU_VALUE_EXE_CORO:
                printf("exe-coro: %p", v.Dyn);
                break;
    #endif
    #if CEU >= 4
            case CEU_VALUE_EXE_TASK:
    #if CEU >= 5
            case CEU_VALUE_EXE_TASK_IN:
    #endif
                printf("exe-task: %p", v.Dyn);
                break;
    #endif
    #if CEU >= 5
            case CEU_VALUE_TASKS:
                printf("tasks: %p", v.Dyn);
                break;
            case CEU_VALUE_TRACK:
                printf("track: %p", v.Dyn);
                break;
    #endif
            default:
                assert(0 && "bug found");
        }
    }
    int ceu_print_f (CEUX X) {
        for (int i=0; i<X.args; i++) {
            if (i > 0) {
                printf("\t");
            }
            ceu_print1(ceux_peek(ceux_arg(X,i)));
        }
        return 0;
    }
    int ceu_println_f (CEUX X) {
        assert(0 == ceu_print_f(CEU5(_0 COMMA) CEU4(_1 COMMA) X));
        printf("\n");
        return 0;
    }
    """
    }
    fun eq_neq_len (): String {
        return  """
    CEU_Value _ceu_equals_equals_ (CEU_Value e1, CEU_Value e2) {
        int v = (e1.type == e2.type);
        if (v) {
            switch (e1.type) {
                case CEU_VALUE_NIL:
                    v = 1;
                    break;
                case CEU_VALUE_TAG:
                    v = (e1.Tag == e2.Tag);
                    break;
                case CEU_VALUE_BOOL:
                    v = (e1.Bool == e2.Bool);
                    break;
                case CEU_VALUE_CHAR:
                    v = (e1.Char == e2.Char);
                    break;
                case CEU_VALUE_NUMBER:
                    v = (e1.Number == e2.Number);
                    break;
                case CEU_VALUE_POINTER:
                    v = (e1.Pointer == e2.Pointer);
                    break;
                case CEU_VALUE_TUPLE:
                case CEU_VALUE_VECTOR:
                case CEU_VALUE_DICT:
                case CEU_VALUE_CLO_FUNC:
        #if CEU >= 3
                case CEU_VALUE_CLO_CORO:
        #endif
        #if CEU >= 4
                case CEU_VALUE_CLO_TASK:
        #endif
        #if CEU >= 3
                case CEU_VALUE_EXE_CORO:
        #endif
        #if CEU >= 4
                case CEU_VALUE_EXE_TASK:
        #endif
        #if CEU >= 5
                case CEU_VALUE_EXE_TASK_IN:
                case CEU_VALUE_TRACK:
        #endif
                    v = (e1.Dyn == e2.Dyn);
                    break;
                default:
                    assert(0 && "bug found");
            }
        }
        return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=v} };
    }
    int ceu_equals_equals_f (CEUX X) {
        assert(X.args == 2);
        CEU_Value ret = _ceu_equals_equals_(ceux_peek(ceux_arg(X,0)), ceux_peek(ceux_arg(X,1)));
        ceux_push(1, ret);
        return 1;
    }
    int ceu_slash_equals_f (CEUX X) {
        ceu_equals_equals_f(CEU5(_0 COMMA) CEU4(_1 COMMA) X);
        CEU_Value ret = ceux_pop(0);
        assert(ret.type == CEU_VALUE_BOOL);
        ret.Bool = !ret.Bool;
        ceux_push(1, ret);
        return 1;
    }
    
    int ceu_hash_f (CEUX X) {
        assert(X.args == 1);
        CEU_Value v = ceux_peek(ceux_arg(X,0));
        CEU_Value ret;
        if (v.type == CEU_VALUE_VECTOR) {
            ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=v.Dyn->Vector.its} };
        } else if (v.type == CEU_VALUE_TUPLE) {
            ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=v.Dyn->Tuple.its} };
        } else {
            ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="length error : not a vector"} };
        }
        ceux_push(1, ret);
        return 1;
    }
    """
    }

    // MAIN
    fun main (): String {
        return """
    ${this.pres.joinToString("")}
    
    int main (int ceu_argc, char** ceu_argv) {
        assert(CEU_TAG_nil == CEU_VALUE_NIL);
        char ceu_err_msg[255];
    #if CEU >= 4
       CEU_Stack* ceu_bstk = &CEU_BSTK;
    #endif
    #if CEU >= 5
       CEU_Stack* ceu_dstk = &CEU_DSTK;
    #endif
        
    #if 0
        // ... args ...
        {
            CEU_Value xxx = ceu_create_tuple(ceu_argc);
            for (int i=0; i<ceu_argc; i++) {
                CEU_Value vec = ceu_vector_from_c_string(ceu_argv[i]);
                ceu_tuple_set(&xxx.Dyn->Tuple, i, vec);
            }
            ceux_push(1, xxx);
        }
    #endif
    
        ${do_while(this.code)}

        // uncaught throw
        #if CEU >= 2
            if (ceux_peek(X(-1)).type == CEU_VALUE_ERROR) {
                assert(0 && "TODO");
                #if 0
                int iserr = (ceu_acc.Dyn->Throw.val.type == CEU_VALUE_ERROR);
                int N = ceu_acc.Dyn->Throw.stk.Dyn->Vector.its;
                CEU_Vector* vals = &ceu_acc.Dyn->Throw.stk.Dyn->Vector;
                for (int i=N-1; i>=0; i--) {
                    if (iserr && i==0) {
                        printf(" v  ");
                    } else {
                        printf(" |  ");
                    }
                    printf("%s", ceu_vector_get(vals,i).Dyn->Vector.buf);
                    if (iserr && i==0) {
                        printf(" : ");
                    } else {
                        puts("");
                    }
                }
                if (!iserr) {
                    printf(" v  throw error : ");
                }
                ceu_print1(ceu_frame, ceu_acc.Dyn->Throw.val);
                puts("");
                #endif
            }
        #endif

        ceux_base(0);
        return 0;
    }
    """
    }

    return (
        h_includes() + h_defines() + h_enums() +
        h1_ceux +
        h_frame_block() + h_value_dyn() + h_tags() +
        h2_ceux +
        c_globals() + h_protos() +
        dumps() + c_exit_error() + c_throw + gc() + c_tags() +
        c_ceux + c_impls() +
        // block-task-up, hold, bcast
        tuple_vector_dict() + creates() +
        print() + eq_neq_len() +
        // throw, pointer-to-string
        // isexe-coro-status-exe-kill, task, track
        main()
    )
}

// DEBUG

// DEBUG / SPACES
fun xxx_01 (): String {
    return """
    #ifdef CEU_DEBUG
        int SPC = 0;
        void spc () {
            for (int i=0; i<SPC; i++) {
                printf("  ");
            }
        }
    #endif
    """
}

/*
    """ // BLOCK - TASK - UP
        CEU_Block* ceu_block_up_block (CEU_Block* blk) {
            if (blk->istop) {
                return blk;
            } else if (blk->up.block == NULL) {
                return blk;
            } else {
                return ceu_block_up_block(blk->up.block);
            }
        }
        int ceu_block_is_up_dn (CEU_Block* up, CEU_Block* dn) {
            if (up == dn) {
                return 1;
            } else if (dn->istop) {
                return ceu_block_is_up_dn(up, dn->up.frame->up_block);
            } else if (dn->up.block == NULL) {
                return 0;
            } else {
                return ceu_block_is_up_dn(up, dn->up.block);
            }
        }

    #if CEU >= 4
        CEU_Frame* ceu_block_up_frame (CEU_Block* blk) {
            if (blk->istop) {
                return blk->up.frame;
            } else if (blk->up.block == NULL) {
                return NULL;
            } else {
                return ceu_block_up_frame(blk->up.block);
            }
        }
        CEU_Frame* ceu_frame_up_frame (CEU_Frame* frame) {
            return ceu_block_up_frame(frame->up_block);
        }
        CEU_Exe_Task* ceu_block_up_task (CEU_Block* blk) {
            CEU_Frame* frame = ceu_block_up_frame(blk);
            if (frame == NULL) {
                return NULL;
            } else {
                CEU_Exe_Task* up = frame->exe_task;
                if (up!=NULL && ceu_istask_dyn((CEU_Dyn*)up)) {
                    return up;
                } else {
                    return ceu_block_up_task(frame->up_block);
                }
            }
        }
        CEU_Exe_Task* ceu_task_up_task (CEU_Exe_Task* task) {
            return ceu_block_up_task(task->frame.up_block);
        }

    #ifdef CEU_DEBUG
        int ceu_depth (CEU_Block* blk) {
            if (blk->istop) {
                return 1 + ceu_depth(blk->up.frame->up_block);
            } else if (blk->up.block == NULL) {
                return 0;
            } else {
                return 1 + ceu_depth(blk->up.block);
            }
        }
    #endif
    #endif
    """ +
    """ // HOLD
        void ceu_hold_add (CEU_Dyn* dyn, CEU_Block* blk CEU5(COMMA CEU_Dyns* dyns)) {
        #if 0
        #if CEU < 5
            CEU_Dyns* dyns = &blk->dn.dyns;
        #endif
            dyn->Any.hld.block = blk;
            if (dyns->first == NULL) {
                dyns->first = dyn;
            }
            if (dyns->last != NULL) {
                dyn->Any.hld.prev = dyns->last;
                dyns->last->Any.hld.next = dyn;
            }
            dyns->last = dyn;
        #endif
        }
        void ceu_hold_rem (CEU_Dyn* dyn) {
        #if 0
            CEU_Dyns* dyns = CEU_HLD_DYNS(dyn);
            if (dyns->first == dyn) {
                dyns->first = dyn->Any.hld.next;
            }
            if (dyns->last == dyn) {
                dyns->last = dyn->Any.hld.prev;
            }
            if (dyn->Any.hld.prev != NULL) {
                dyn->Any.hld.prev->Any.hld.next = dyn->Any.hld.next;
            }
            if (dyn->Any.hld.next != NULL) {
                dyn->Any.hld.next->Any.hld.prev = dyn->Any.hld.prev;
            }
            dyn->Any.hld.block = NULL;
            dyn->Any.hld.prev  = NULL;
            dyn->Any.hld.next  = NULL;
        #endif
        }
        void ceu_hold_chg (CEU_Dyn* dyn, CEU_Block* blk CEU5(COMMA CEU_Dyns* dyns)) {
            ceu_hold_rem(dyn);
            ceu_hold_add(dyn, blk CEU5(COMMA dyns));
        }
    """ +
    """ // BCAST
    #if CEU >= 4
        void ceu_stack_kill (CEU_Stack* stk, void* me) {
            if (stk == NULL) {
                return;
            }
            if (stk->me == me) {
                stk->on = 0;
            }
            return ceu_stack_kill(stk->up, me);
        }

        CEU_Block* ceu_bcast_outer (CEU_Block* blk) {
            if (blk->istop) {
                if (blk->up.frame->clo == NULL) {
                    return blk;
                } else if (blk->up.frame->clo->type == CEU_VALUE_CLO_FUNC) {
                    return ceu_bcast_outer(blk->up.frame->up_block);
                } else {
                    return blk;     // outermost block in coro/task
                }
            } else if (blk->up.block != NULL) {
                return ceu_bcast_outer(blk->up.block);
            } else {
                return blk;         // global scope
            }
        }

        CEU_Value ceu_bcast_blocks (CEU5(CEU_Stack* dstk COMMA) CEU_Stack* bstk, uint8_t now, CEU_Block* blk, CEU_Value* evt);
        CEU_Value ceu_bcast_dyns (CEU5(CEU_Stack* dstk COMMA) CEU_Stack* bstk, uint8_t now, CEU_Dyn* cur, CEU_Value* evt);

        CEU_Value ceu_bcast_task (CEU5(CEU_Stack* dstk COMMA) CEU_Stack* bstk, uint8_t now, CEU_Exe_Task* task, int n) {
            CEU_Value ret = { CEU_VALUE_BOOL, {.Bool=1} };

            // up_task may be aborted
            CEU_Exe_Task* up_task = ceu_task_up_task(task);
            CEU_Stack xstk0;
            CEU_Stack* xxstk0 = bstk;
            if (up_task != NULL) {
                xstk0 = (CEU_Stack) { CEU_HLD_BLOCK((CEU_Dyn*)up_task), 1, bstk };
                xxstk0 = &xstk0;
            }

            CEU_Stack xstk1 = { CEU_HLD_BLOCK((CEU_Dyn*)task), 1, xxstk0 };
            if (task->status == CEU_EXE_STATUS_TERMINATED) {
                return ret;
            } else if (n == CEU_ARG_ABORT) {
                ret = task->frame.clo->proto(CEU5(dstk COMMA) &xstk1, &task->frame, CEU_ARG_ABORT, NULL);
                if (!xstk1.on) {
                    return ret;
                }
                goto __CEU_FREE__;
            } else if (task->status == CEU_EXE_STATUS_TOGGLED) {
                return ret;
            }

            if (task->status==CEU_EXE_STATUS_RESUMED || task->pc!=0) {    // not initial spawn
    #if CEU >= 5
                CEU_Stack xstk2 = { task->dn_block, 1, &xstk1 };
    #else
                // no need to stack bc no dangling possible and bc aborted wont execute below
                #define xstk2 xstk1
    #endif
                ret = ceu_bcast_blocks(CEU5(dstk COMMA) &xstk2, now, task->dn_block, args);
                if (!xstk1.on) {
                    return ret;
                }
                if (!xstk2.on) {
                    return ret;
                }
            }

            #define ceu_time_lt(tsk,now) \
                ((CEU_TIME_MAX>=CEU_TIME_MIN || (tsk<CEU_TIME_MAX && now<CEU_TIME_MAX) || (tsk>CEU_TIME_MIN && now>CEU_TIME_MIN)) ? \
                    (tsk < now) : (tsk > now))

            if (task->status == CEU_EXE_STATUS_YIELDED) {
                if (CEU_ISERR(ret)) {
                    // catch error from blocks above
                    ret = task->frame.clo->proto(CEU5(dstk COMMA) &xstk1, &task->frame, CEU_ARG_ERROR, &ret);
                } else if (n!=CEU_ARG_TOGGLE && (task->pc==0 || ceu_time_lt(task->time,now))) {
                    ret = task->frame.clo->proto(CEU5(dstk COMMA) &xstk1, &task->frame, n, args);
                } else {
                    task->time = CEU_TIME_MIN;
                }
                if (!xstk1.on) {
                    return ret;
                }
            }

            // do not bcast aborted task (only terminated) b/c
            // it would awake parents that actually need to
            // respond/catch the error (thus not awake)
            assert(ret.type != CEU_VALUE_ERROR);    // TODO: chg below to CEU_ISERR?
            if (ret.type!=CEU_VALUE_THROW && task->status==CEU_EXE_STATUS_TERMINATED) {
                task->hld.type = CEU_HOLD_MUTAB;    // TODO: copy ref to deep scope
                CEU_Value evt2 = ceu_dyn_to_val((CEU_Dyn*)task);
                CEU_Value ret2;

                assert(CEU_TIME_N < 255);
                CEU_TIME_N++;
                uint8_t now = ++CEU_TIME_MAX;

                if (up_task!=NULL && xstk0.on) {
                    // enclosing coro of enclosing block
                    ret2 = ceu_bcast_task(CEU5(dstk COMMA) &xstk1, now, up_task, 1, &evt2);
                } else {
                    // enclosing block
                    ret2 = ceu_bcast_blocks(CEU5(dstk COMMA) &xstk1, now, CEU_HLD_BLOCK((CEU_Dyn*)task), &evt2);
                }
                CEU_TIME_N--;
                if (CEU_TIME_N == 0) {
                    CEU_TIME_MIN = now;
                }
                ret = CEU_ERR_OR(ret, ret2);
                if (!xstk1.on) {
                    return ret;
                }
                task->refs--;
    #if CEU >= 5
                ceu_stack_kill(dstk, task);
    #endif
                /* TODO: stack trace for error on task termination
                do {
                    CEU_ERROR_ASR(BUPC, ceux_peek(X(-1)), "FILE : (lin LIN, col COL) : ERR");
                } while (0);
                */
                goto __CEU_FREE__;
            }

            if (0) {
        __CEU_FREE__:
    #if CEU >= 5
                if (task->type == CEU_VALUE_EXE_TASK_IN) {
                    ceu_gc_rem((CEU_Dyn*)task);
                }
    #endif
            }
            return ret;
        }

        CEU_Value ceu_bcast_dyns (CEU5(CEU_Stack* dstk COMMA) CEU_Stack* bstk, uint8_t now, CEU_Dyn* cur, CEU_Value* evt) {
            if (cur == NULL) {
                return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
            }
            switch (cur->Any.type) {
                case CEU_VALUE_EXE_TASK: {
                    if (cur->Exe_Task.status == CEU_EXE_STATUS_TERMINATED) {
                        return ceu_bcast_dyns(CEU5(dstk COMMA) bstk, now, cur->Any.hld.prev, evt);
                    }
    #if CEU >= 5
                case CEU_VALUE_EXE_TASK_IN:
    #endif
                    CEU_Stack xstk = { cur->Exe_Task.dn_block, 1, bstk };
                    CEU_Value ret = ceu_bcast_dyns(CEU5(dstk COMMA) &xstk, now, cur->Any.hld.prev, evt);
                    if (!bstk->on || !xstk.on) {
                        return ret;
                    }
                    if (CEU_ISERR(ret)) {
                        return ret;
                    }
                    return ceu_bcast_task(CEU5(dstk COMMA) bstk, now, &cur->Exe_Task, (evt==NULL ? CEU_ARG_TOGGLE : 1), evt);
                }
    #if CEU >= 5
                case CEU_VALUE_TASKS: {
                    CEU_Value ret = ceu_bcast_dyns(CEU5(dstk COMMA) bstk, now, cur->Any.hld.prev, evt);
                    if (CEU_ISERR(ret)) {
                        return ret;
                    }
                    return ceu_bcast_dyns(CEU5(dstk COMMA) bstk, now, cur->Tasks.dyns.last, evt);
                }
                case CEU_VALUE_TRACK: {
                    CEU_Value ret = ceu_bcast_dyns(CEU5(dstk COMMA) bstk, now, cur->Any.hld.prev, evt);
                    if (CEU_ISERR(ret)) {
                        return ret;
                    }
                    if (evt!=NULL && ceu_istask_val(*evt) && cur->Track.task==(CEU_Exe_Task*)evt->Dyn) {
                        cur->Track.task = NULL; // tracked coro is terminating
                    }
                    return (CEU_Value) { CEU_VALUE_NIL };
                }
    #endif
                default:
                    return ceu_bcast_dyns(CEU5(dstk COMMA) bstk, now, cur->Any.hld.prev, evt);
            }
        }

        CEU_Value ceu_bcast_blocks (CEU5(CEU_Stack* dstk COMMA) CEU_Stack* bstk, uint8_t now, CEU_Block* cur, CEU_Value* evt) {
            if (cur == NULL) {
                return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
            }
            CEU_Stack xstk = { cur, 1, bstk };
            CEU_Value ret = ceu_bcast_dyns(CEU5(dstk COMMA) &xstk, now, cur->dn.dyns.last, evt);
            if (!xstk.on) {
                return ret;
            }
            if (CEU_ISERR(ret)) {
                return ret;
            }
            return ceu_bcast_blocks(CEU5(dstk COMMA) bstk, now, cur->dn.block, evt);
        }

        void ceu_bstk_assert (CEU_Stack* bstk) {
            if (bstk == NULL) {
                assert(0 && "TODO: cannot spawn or broadcast during abortion");
            } else if (bstk == &CEU_BSTK) {
                // ok
            } else {
                ceu_bstk_assert(bstk->up);
            }
        }

        void ceu_broadcast_f (CEU5(CEU_Stack* dstk COMMA) CEU_Stack* bstk, CEU_Frame* frame, CEUX X) {
            assert(n == 2);
            ceu_bstk_assert(bstk);

            assert(CEU_TIME_N < 255);
            CEU_TIME_N++;
            uint8_t now = ++CEU_TIME_MAX;

            CEU_Value evt = ceux_peek(base);
            if (evt.type > CEU_VALUE_DYNAMIC) {
                if (evt.Dyn->Any.hld.type == CEU_HOLD_FLEET) {
                    // keep the block, set MUTAB recursively
                    assert(NULL == ceu_hold_set_msg(CEU_HOLD_CMD_BCAST, evt, NULL, (ceu_hold_cmd){.Bcast={}}));
                }
            }
            -=- TODO -=-
            CEU_Value xin = args[1];
            CEU_Value ret;
            if (xin.type == CEU_VALUE_TAG) {
                if (xin.Tag == CEU_TAG_global) {
                    ret = ceu_bcast_blocks(CEU5(dstk COMMA) bstk, now, &CEU_BLOCK, args);
                } else if (xin.Tag == CEU_TAG_task) {
                    ret = ceu_bcast_blocks(CEU5(dstk COMMA) bstk, now, ceu_bcast_outer(frame->up_block), args);
                } else {
                    ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="invalid target"} };
                }
            } else {
                if (ceu_istask_val(xin)) {
                    ret = ceu_bcast_task(CEU5(dstk COMMA) bstk, now, &xin.Dyn->Exe_Task, 1, args);
                } else {
                    ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="invalid target"} };
                }
            }
            if (evt.type > CEU_VALUE_DYNAMIC) {
                ceu_gc_dec(evt);
            }

            CEU_TIME_N--;
            if (CEU_TIME_N == 0) {
                CEU_TIME_MIN = now;
            }
            return ret;
        }
    #endif
    """ +
    """ // ISEXE / COROUTINE / STATUS / EXE_KILL
        #if CEU >= 3
        int ceu_isexe (CEU_Dyn* dyn) {
            return (dyn->Any.type==CEU_VALUE_EXE_CORO CEU4(|| ceu_istask_dyn(dyn)));
        }

        CEU_Value ceu_coroutine_f (CEU_Frame* frame, CEUX X) {
            assert(n == 1);
            CEU_Value coro = args[0];
            if (coro.type != CEU_VALUE_CLO_CORO) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="coroutine error : expected coro"} };
            }
            return _ceu_create_exe_(CEU_VALUE_EXE_CORO, sizeof(CEU_Exe), coro CEU5(COMMA &frame->up_block->dn.dyns));
        }

        CEU_Value ceu_status_f (CEU_Frame* frame, CEUX X) {
            assert(n == 1);
            CEU_Value exe = args[0];
            CEU_Value ret;
            if (exe.type!=CEU_VALUE_EXE_CORO CEU4(&& !ceu_istask_val(exe))) {
        #if CEU < 4
                ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="status error : expected running coroutine"} };
        #else
                ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="status error : expected running coroutine or task"} };
        #endif
            } else {
                ret = (CEU_Value) { CEU_VALUE_TAG, {.Tag=exe.Dyn->Exe.status + CEU_TAG_yielded - 1} };
            }
            ceu_gc_dec_args(n, args);
            return ret;
        }

        CEU_Value ceu_dyn_exe_kill (CEU5(CEU_Stack* dstk COMMA) CEU4(CEU_Stack* bstk COMMA) CEU_Dyn* dyn) {
            CEU_Value ret = { CEU_VALUE_NIL };
        #if CEU >= 5
            if (dyn->Any.type == CEU_VALUE_TASKS) {
                CEU_Dyn* cur = dyn->Tasks.dyns.first;
                while (cur != NULL) {
                    CEU_Dyn* nxt = cur->Any.hld.next;
                    ret = CEU_ERR_OR(ret, ceu_dyn_exe_kill(CEU5(dstk COMMA) CEU4(bstk COMMA) cur));
                    cur = nxt;
                }
                return ret;
            }
            else
        #endif
            {
                if (ceu_isexe(dyn) && dyn->Exe.status<CEU_EXE_STATUS_TERMINATED) {
    #if CEU >= 4
                    if (ceu_istask_dyn(dyn)) {
                        ret = ceu_bcast_task(CEU5(dstk COMMA) bstk, CEU_TIME_MAX, &dyn->Exe_Task, CEU_ARG_ABORT, NULL);
                    } else
    #endif
                    {
                        ret = dyn->Exe.frame.clo->proto(CEU5(dstk COMMA) CEU4(bstk COMMA) &dyn->Exe.frame, CEU_ARG_ABORT, NULL);
                    }
                    //assert(!CEU_ISERR(ret) && "TODO: error on exe kill");
                    return ret;
                }
            }
            return ret;
        }
        #endif
    """ +
    """ // TASK
        #if CEU >= 4
        int ceu_istask_dyn (CEU_Dyn* dyn) {
            return dyn->Any.type==CEU_VALUE_EXE_TASK CEU5(|| dyn->Any.type==CEU_VALUE_EXE_TASK_IN);
        }
        int ceu_istask_val (CEU_Value val) {
            return (val.type>CEU_VALUE_DYNAMIC) && ceu_istask_dyn(val.Dyn);
        }
        #endif
    """ +
    """ // TRACK
        #if CEU >= 5
        CEU_Value ceu_track_f (CEU_Frame* frame, CEUX X) {
            assert(n == 1);
            CEU_Value task = args[0];
            if (!ceu_istask_val(task)) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="track error : expected task"} };
            } else if (task.Dyn->Exe_Task.status == CEU_EXE_STATUS_TERMINATED) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="track error : expected unterminated task"} };
            }
            //CEU_Block* blk = (ceu_dmin(task->Dyn->up_dyns.dyns->up_block) > ceu_dmin(frame->up_block)) ?
            //    task->Dyn->up_dyns.dyns->up_block : frame->up_block;
            return ceu_create_track((CEU_Exe_Task*)task.Dyn);
        }
        CEU_Value ceu_detrack_f (CEU_Stack* _0, CEU_Stack* _1, CEU_Frame* frame, CEUX X) {
            assert(n == 1);
            CEU_Value trk = args[0];
            if (trk.type != CEU_VALUE_TRACK) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="detrack error : expected track value"} };
            } else if (trk.Dyn->Track.task == NULL) {
                return (CEU_Value) { CEU_VALUE_NIL };
            } else {
                return ceu_dyn_to_val((CEU_Dyn*)trk.Dyn->Track.task);
            }
        }
        int ceu_dstk_isoff (CEU_Stack* dstk) {
            if (dstk == NULL) {
                return 0;
            } else if (!dstk->on) {
                return 1;
            } else {
                return ceu_dstk_isoff(dstk->up);
            }
        }
        #endif
    """ +
 */