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

    #define CEU_ACC(v) ({           \
        CEU_Value ceu_tmp = v;      \
        ceu_gc_inc_val(ceu_tmp);    \
        ceu_gc_dec_val(ceu_acc);    \
        ceu_acc = ceu_tmp;          \
    })
    #define CEU_ACC_KEEP() ({                       \
        CEU_Value ceu_tmp = ceu_acc;                \
        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };    \
        ceu_tmp;                                    \
    })
    #define CEU_ACC_INC() ({        \
        ceu_gc_inc_val(ceu_acc);    \
        ceu_acc;                    \
    })
    """
    }
    fun h_enums (): String {
        return """
    #if CEU >= 3
    typedef enum CEU_ACTION {
        CEU_ACTION_INVALID = -1,    // default to force set
        CEU_ACTION_CALL,
        CEU_ACTION_RESUME,
        CEU_ACTION_ABORT,           // awake exe to finalize defers and release memory
    #if CEU >= 4
        //CEU_ACTION_TOGGLE,          // restore time to CEU_TIME_MIN after toggle
        CEU_ACTION_ERROR,           // awake task to catch error from nested task
    #endif
    } CEU_ACTION;
    #endif

    typedef enum CEU_VALUE {
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

    fun h_value_dyn (): String {
        return """
    #if CEU >= 4
    struct CEU_Exe_Task;
    #endif
    #if CEU >= 5
    struct CEU_Tasks;
    #endif
    
    typedef struct CEU_Value {
        CEU_VALUE type;
        union {
            //void nil;
            char* Error;            // NULL=value on stack, !NULL=value is this string
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
        CEU_Value tag;
        
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

    struct CEU_Clo;
    typedef void (*CEU_Proto) (struct CEU_Clo*, int n, CEU_Value args[]);

    #define _CEU_Clo_                   \
        _CEU_Dyn_                       \
        CEU_Proto proto;                \
        struct {                        \
            int its;                    \
            CEU_Value* buf;             \
        } upvs;

    typedef struct CEU_Clo {
        _CEU_Clo_
    } CEU_Clo;
    
    #if CEU >= 4
    typedef struct CEU_Clo_Task {
        _CEU_Clo_                       \
        struct CEU_Exe_Task* up_tsk;    \
    } CEU_Clo_Task;
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
        CEU_Value clo;                  \
        /*struct CEU_Frame frame;*/     \
        int pc;                         \
        struct CEUX* X;
        
    typedef struct CEU_Exe {
        _CEU_Exe_
    } CEU_Exe;
    #endif
    
    #if CEU >= 4
    typedef struct CEU_Links {
        struct {
            union CEU_Dyn* dyn;
            CEU_Block* blk;
        } up;
        struct {
            union CEU_Dyn* prv;
            union CEU_Dyn* nxt;
        } sd;
        struct {
            union CEU_Dyn* fst;
            union CEU_Dyn* lst;
        } dn;
    } CEU_Links;
    #endif
    
    #if CEU >= 5
        #define CEU_LNKS(dyn) ((dyn)->Any.type==CEU_VALUE_TASKS ? &(dyn)->Tasks.lnks : &(dyn)->Exe_Task.lnks)
    #else
        #define CEU_LNKS(dyn) (&((CEU_Exe_Task*) dyn)->lnks)
    #endif

    #if CEU >= 4
    typedef struct CEU_Exe_Task {
        _CEU_Exe_
        uint32_t time;      // last sleep time, only awakes if CEU_TIME>time 
        CEU_Value pub;
        CEU_Links lnks;
    } CEU_Exe_Task;
    #endif
    
    #if CEU >= 5
    typedef struct CEU_Tasks {
        _CEU_Dyn_
        int max;
        CEU_Links lnks;
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
    #if CEU >= 2
        struct CEU_Throw    Throw;
    #endif
    #if CEU >= 4
        struct CEU_Clo_Task Clo_Task;
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
    """
    }

    // GLOBALS
    fun x_globals (): String {
        return """
    CEU_Value ceu_acc = { CEU_VALUE_NIL };
    int CEU_BREAK = 0;
    #if 0
    #if CEU >= 4
    uint32_t CEU_TIME = 0;
    CEU_Exe_Task CEU_GLOBAL_TASK = {
        CEU_VALUE_EXE_TASK, 1, (CEU_Value) { CEU_VALUE_NIL },
        CEU_EXE_STATUS_YIELDED, {}, 0, NULL,
        0, {}, { {NULL,NULL}, {NULL,NULL}, {NULL,NULL} }
    };
    #endif
    #endif
    """
    }
    fun h_protos (): String {
        return """
    void ceu_pro_type (CEUX* X);
    int ceu_as_bool (CEU_Value v);
    CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn);

    int ceu_type_to_size (int type);

    void ceu_gc_inc_val (CEU_Value v);

    CEU_Value ceu_create_vector  (void);
    CEU_Value ceu_create_dict    (void);
    CEU_Value ceu_create_clo     (CEU_VALUE type, CEU_Proto proto, int pars, int upvs);
    #if CEU >= 4
    CEU_Value ceu_create_exe_task (CEU_Value clo, CEU_Dyn* up_dyn, CEU_Block* up_blk);
    CEU_Value ceu_create_track   (CEU_Exe_Task* task);
    #endif

    void ceu_print1 (CEU_Value v);
    CEU_Value _ceu_equals_equals_ (CEU_Value e1, CEU_Value e2);

    #if CEU >= 3
    int ceu_isexe_dyn (CEU_Dyn* dyn);
    int ceu_isexe_val (CEU_Value val);
    void ceu_abort_exe (CEU_Exe* exe);
    #endif
    #if CEU >= 4
    #define ceu_abort_dyn(a) ceu_abort_exe((CEU_Exe*)a)
    #define ceu_bcast_dyn(a,b,c,d) ceu_bcast_task(a,b,c,(CEU_Exe_Task*)d)
    int ceu_bcast_task (CEUX* X1, CEU_ACTION act, uint32_t now, CEU_Exe_Task* tsk2);
    int ceu_bcast_tasks (CEUX* X1, CEU_ACTION act, uint32_t now, CEU_Dyn* dyn2);
    int ceu_istask_dyn (CEU_Dyn* dyn);
    int ceu_istask_val (CEU_Value val);
    void ceu_dyn_unlink (CEU_Dyn* dyn);
    #endif
    #if CEU >= 5
    #undef ceu_abort_dyn
    #define ceu_abort_dyn(a) (a->Any.type==CEU_VALUE_TASKS ? ceu_abort_tasks((CEU_Tasks*)a) : ceu_abort_exe((CEU_Exe*)a))
    #undef ceu_bcast_dyn
    #define ceu_bcast_dyn(a,b,c,d) (d->Any.type==CEU_VALUE_TASKS ? ceu_bcast_tasks(a,b,c,d) : ceu_bcast_task(a,b,c,(CEU_Exe_Task*)d))
    void ceu_abort_tasks (CEU_Tasks* tsks);
    #endif
    """
    }
    fun dumps (): String {
        return """
#ifdef CEU_DEBUG
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
    void ceu_dump_val (CEU_Value v) {
        puts(">>>>>>>>>>>");
        ceu_print1(v);
        puts(" <<<");
        if (v.type > CEU_VALUE_DYNAMIC) {
            printf("    dyn   = %p\n", v.Dyn);
            printf("    type  = %d\n", v.type);
            printf("    refs  = %d\n", v.Dyn->Any.refs);
            //printf("    next  = %p\n", v.Dyn->Any.hld.next);
            printf("    ----\n");
            switch (v.type) {
        #if CEU >= 4
                case CEU_VALUE_EXE_TASK:
                    printf("    status = %d\n", v.Dyn->Exe_Task.status);
                    printf("    pc     = %d\n", v.Dyn->Exe_Task.pc);
                    printf("    pub    = %d\n", v.Dyn->Exe_Task.pub.type);
                    break;
        #endif
        #if CEU >= 5
                case CEU_VALUE_TASKS:
                    //printf("    up_blk = %p\n", v.Dyn->Tasks.up_blk);
                    //printf("    dn_tsk = %p\n", v.Dyn->Tasks.dn_tsk);
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
    void ceu_dump_dyn (CEU_Dyn* dyn) {
        ceu_dump_val(ceu_dyn_to_val(dyn));
    }
    void ceu_dump_block (CEU_Block* blk) {
        printf(">>> BLOCK: %p\n", blk);
        printf("    istop = %d\n", blk->istop);
        //printf("    up    = %p\n", blk->up.frame);
        CEU_Dyn* cur = blk->dn.dyns.first;
        while (cur != NULL) {
            ceu_dump_dyn(cur);
            CEU_Dyn* old = cur;
            //cur = old->Any.hld.next;
        }
    }
    #endif
#endif
    """
    }

    // EXIT / ERROR / ASSERT
    val c_error = """
    #if 0
    #define CEU_ERROR_RET(S) (CEU_ERROR_IS(S) ? (3+ceux_peek(S,(S)->n-3).Number) : 0)

    #define CEU_ERROR_CHK_VAL(cmd,v,pre) ({     \
        if (v.type == CEU_VALUE_ERROR) {        \
            ceu_error_e(X->S,v);                \
            CEU_ERROR_CHK_STK(cmd,pre);         \
        };                                      \
        v;                                      \
    })
    #define CEU_ERROR_THR_S(cmd,msg,pre) {      \
        ceu_error_s(X->S, msg);                 \
        CEU_ERROR_CHK_STK(cmd,pre);             \
    }
    #endif

    #if CEU <= 1
    #define CEU_ERROR_CHK_ACC(cmd,pre) {                                               \
        if (ceu_acc.type == CEU_VALUE_ERROR) {                                      \
            fprintf(stderr, " |  %s\n v  error : %s\n", pre, ceu_acc.Error);  \
            exit(0);                                                                \
        }                                                                           \
    }
    #define CEU_ERROR_CHK_PTR(cmd,ptr,pre) {                                               \
        if ((ptr) != NULL) {                                      \
            fprintf(stderr, " |  %s\n v  error : %s\n", pre, ptr);  \
            exit(0);                                                                \
        }                                                                           \
    }
    #else
    #define CEU_ERROR_CHK_STK(cmd,pre)      \
        if (ceu_error_chk_stk(X->S, pre)) { \
            cmd;                            \
        }
    int ceu_error_chk_stk (CEU_Stack* S, char* pre) {
        if (!CEU_ERROR_IS(S)) {
            return 0;
        } else {
            if (pre != NULL) {      // blocks check but do not add a message
                // [...,n,pay,err]
                CEU_Value n = ceux_peek(S, SS(-3));
                assert(n.type == CEU_VALUE_NUMBER);
                ceux_repl(S, SS(-3), (CEU_Value) { CEU_VALUE_NUMBER, {.Number=n.Number+1} });
                ceux_ins(S, SS(-3), (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=pre} });
                // [...,pre,n+1,pay,err]
            }
            return 1;
        }
    }
    #endif

    #if 0
    int ceu_error_e (CEU_Stack* S, CEU_Value e) {
        assert(e.type==CEU_VALUE_ERROR && e.Error!=NULL);
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_NUMBER, {.Number=0} });
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=e.Error} });
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_ERROR, {.Error=NULL} });
        return 3;
    }
    int ceu_error_s (CEU_Stack* S, char* s) {
        assert(s != NULL);
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_NUMBER, {.Number=0} });
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=s} });
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_ERROR });
        return 3;
    }
    int ceu_error_v (CEU_Stack* S, CEU_Value v) {
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_NUMBER, {.Number=0} });
        ceux_push(S, 1, v);
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_ERROR, {.Error=NULL} });
        return 3;
    }
    #endif

    void ceu_pro_error (CEU_Clo* _1, int n, CEU_Value args[]) {
    #if CEU < 2
        assert(n == 1);
        CEU_Value tag = args[0];
        assert(tag.type == CEU_VALUE_TAG);
        CEU_ACC (
            ((CEU_Value) { CEU_VALUE_ERROR, {.Error=ceu_tag_to_pointer(tag.Tag)} })
        );
        //ceu_gc_dec_val(tag);
    #else
        if (X->args == 0) {
            ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });
        }
        return ceu_error_v(X->S, ceux_peek(X->S, ceux_arg(X,0)));
    #endif
    }
    """

    // GC
    fun gc (): String {
        return """
#ifdef CEU_DEBUG
    struct {
        int alloc;
        int free;
    } CEU_GC = { 0, 0 };
    
    void ceu_dump_gc (void) {
        printf(">>> GC: %d\n", CEU_GC.alloc - CEU_GC.free);
        printf("    alloc = %d\n", CEU_GC.alloc);
        printf("    free  = %d\n", CEU_GC.free);
    }

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

    void ceu_gc_free (CEU_Dyn* dyn);
    
    void ceu_gc_dec_dyn (CEU_Dyn* dyn) {
        assert(dyn->Any.refs > 0);
        dyn->Any.refs--;
        if (dyn->Any.refs == 0) {
            ceu_gc_free(dyn);
        }
    }
    void ceu_gc_dec_val (CEU_Value val) {
        if (val.type < CEU_VALUE_DYNAMIC)
            return;
        ceu_gc_dec_dyn(val.Dyn);
    }

    void ceu_gc_dec_args (int n, CEU_Value args[]) {
        for (int i=0; i<n; i++) {
            ceu_gc_dec_val(args[i]);
        }
    }
    
    void ceu_gc_inc_dyn (CEU_Dyn* dyn) {
        assert(dyn->Any.refs < 255);
        dyn->Any.refs++;
    }
    void ceu_gc_inc_val (CEU_Value val) {
        if (val.type < CEU_VALUE_DYNAMIC)
            return;
        ceu_gc_inc_dyn(val.Dyn);
    }

    CEU_Value ceu_vector_get (CEU_Vector* vec, int i);
    void ceu_gc_free (CEU_Dyn* dyn) {
        switch (dyn->Any.type) {
            case CEU_VALUE_CLO_FUNC:
#if CEU >= 3
            case CEU_VALUE_CLO_CORO:
#endif
#if CEU >= 4
            case CEU_VALUE_CLO_TASK:
#endif
                for (int i=0; i<dyn->Clo.upvs.its; i++) {
                    ceu_gc_dec_val(dyn->Clo.upvs.buf[i]);
                }
                free(dyn->Clo.upvs.buf);
                break;
            case CEU_VALUE_TUPLE:       // buf w/ dyn
                for (int i=0; i<dyn->Tuple.its; i++) {
                    ceu_gc_dec_val(dyn->Tuple.buf[i]);
                }
                break;
            case CEU_VALUE_VECTOR:
                for (int i=0; i<dyn->Vector.its; i++) {
                    CEU_Value ret = ceu_vector_get(&dyn->Vector, i);
                    assert(ret.type != CEU_VALUE_ERROR);
                    ceu_gc_dec_val(ret);
                }
                free(dyn->Vector.buf);
                break;
            case CEU_VALUE_DICT:
                for (int i=0; i<dyn->Dict.max; i++) {
                    ceu_gc_dec_val((*dyn->Dict.buf)[i][0]);
                    ceu_gc_dec_val((*dyn->Dict.buf)[i][1]);
                }
                free(dyn->Dict.buf);
                break;
#if CEU >= 3
            case CEU_VALUE_EXE_CORO: {
#if CEU >= 4
            case CEU_VALUE_EXE_TASK:
#endif
                if (dyn->Exe.status != CEU_EXE_STATUS_TERMINATED) {
                    assert(dyn->Any.type == CEU_VALUE_EXE_CORO);
                    dyn->Any.refs++;            // currently 0->1: needs ->2 to prevent double gc
                    ceu_abort_exe((CEU_Exe*)dyn);
                    dyn->Any.refs--;
                }
                ceux_n_set(dyn->Exe.X->S, 0);
                ceu_gc_dec_val(dyn->Exe.clo);
#if CEU >= 4
                if (dyn->Any.type == CEU_VALUE_EXE_TASK) {
                    ceu_gc_dec_val(((CEU_Exe_Task*)dyn)->pub);
                    ceu_dyn_unlink(dyn);
                }
#endif
                free(dyn->Exe.X->S);
                free(dyn->Exe.X);
                break;
            }
#endif
#if CEU >= 5
            case CEU_VALUE_TASKS: {
                ceu_abort_tasks(&dyn->Tasks);
                ceu_dyn_unlink(dyn);
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
                val c1c = c1.idc()
                val ie1 = e1 ?: i1++
                val prv1 = last
                last = "&ceu_tag__$c1c"
                var i2 = 0
                """
                #define CEU_TAG_$c1c ($ie1)
                CEU_Tags_Names ceu_tag__$c1c = { CEU_TAG_$c1c, "$s1", $prv1 };
                """ + it1.second.map { it2 ->
                    val (s2,c2,e2) = tags.pub[':'+it1.first+'.'+it2.first]!!
                    val c2c = c2.idc()
                    assert(e2 == null)
                    i2++
                    val prv2 = last
                    last = "&ceu_tag__$c2c"
                    var i3 = 0
                    """
                    #define CEU_TAG_$c2c (($i2 << 8) | $ie1)
                    CEU_Tags_Names ceu_tag__$c2c = { CEU_TAG_$c2c, "$s2", $prv2 };
                    """ + it2.second.map { it3 ->
                        val (s3,c3,e3) = tags.pub[':'+it1.first+'.'+it2.first+'.'+it3.first]!!
                        assert(e3 == null)
                        i3++
                        val prv3 = last
                        last = "&ceu_tag__$c3"
                        var i4 = 0
                        """
                        #define CEU_TAG_$c3 (($i3 << 16) | ($i2 << 8) | $ie1)
                        CEU_Tags_Names ceu_tag__$c3 = { CEU_TAG_$c3, "$s3", $prv3 };
                        """ + it3.second.map { it4 ->
                            val (s4,c4,e4) = tags.pub[':'+it1.first+'.'+it2.first+'.'+it3.first+'.'+it4.first]!!
                            assert(e4 == null)
                            i4++
                            val prv4 = last
                            last = "&ceu_tag__$c4"
                            """
                            #define CEU_TAG_$c4 (($i4 << 24) | ($i3 << 16) | ($i2 << 8) | $ie1)
                            CEU_Tags_Names ceu_tag__$c4 = { CEU_TAG_$c4, "$s4", $prv4 };
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
        } +
        """
    char* ceu_tag_to_pointer (int tag) {
        CEU_Tags_Names* cur = CEU_TAGS;
        while (cur != NULL) {
            if (cur->tag == tag) {
                return cur->name;
            }
            cur = cur->next;
        }
        assert(0 && "bug found");
    }
    
    void ceu_pro_tag (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n==1 || n==2);
        if (n == 1) {
            CEU_Value dyn = args[0];
            if (dyn.type < CEU_VALUE_DYNAMIC) {
                CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));
            } else {
                CEU_ACC(dyn.Dyn->Any.tag);
            }
        } else {
            CEU_Value tag = args[0];
            CEU_Value dyn = args[1];
            assert(tag.type == CEU_VALUE_TAG);
            if (dyn.type < CEU_VALUE_DYNAMIC) {
                // nothing to set
            } else {
                dyn.Dyn->Any.tag = tag;
            }
            ceu_acc = dyn;  // do not gc_inc/dec
        }
        ceu_gc_dec_args(n, args);
    }            
        """
    }

    fun c_to (): String {
        return """
    // TO-TAG-STRING
    void ceu_pro_to_dash_tag_dash_string (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n == 1);
        CEU_Value str = args[0];
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
        CEU_ACC(ret);
        ceu_gc_dec_val(str);
    }
    
    // TO-STRING-*

    CEU_Value ceu_pointer_to_string (const char* ptr) {
        assert(ptr != NULL);
        CEU_Value str = ceu_create_vector();
        int len = strlen(ptr);
        for (int i=0; i<len; i++) {
            CEU_Value chr = { CEU_VALUE_CHAR, {.Char=ptr[i]} };
            ceu_vector_set(&str.Dyn->Vector, i, chr);
        }
        return str;
    }
    
    void ceu_pro_to_dash_string_dash_pointer (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n == 1);
        CEU_Value ptr = args[0];
        assert(ptr.type==CEU_VALUE_POINTER && ptr.Pointer!=NULL);
        CEU_ACC(ceu_pointer_to_string(ptr.Pointer));
    }

    void ceu_pro_to_dash_string_dash_tag (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n == 1);
        CEU_Value tag = args[0];
        assert(tag.type == CEU_VALUE_TAG);        
        CEU_ACC(ceu_pointer_to_string(ceu_tag_to_pointer(tag.Tag)));
    }

    void ceu_pro_to_dash_string_dash_number (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n == 1);
        CEU_Value num = args[0];
        assert(num.type == CEU_VALUE_NUMBER);
        
        char str[255];
        snprintf(str, 255, "%g", num.Number);
        assert(strlen(str) < 255);

        CEU_ACC(ceu_pointer_to_string(str));
    }
    """
    }
    fun c_impls (): String {
        return """
    #if 0
    CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn) {
        return (CEU_Value) { dyn->Any.type, {.Dyn=dyn} };
    }
    
    void ceu_pro_dump (CEUX* X) {
        assert(X->args == 1);
    #ifdef CEU_DEBUG
        ceu_dump_val(ceux_peek(X->S, ceux_arg(X,0)));
        return 0;
    #else
        return ceu_error_s(X->S, "debug is off");
    #endif
    }
    #endif

    int ceu_as_bool (CEU_Value v) {
        return !(v.type==CEU_VALUE_NIL || (v.type==CEU_VALUE_BOOL && !v.Bool));
    }
    void ceu_pro_type (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n==1 && "bug found");
        CEU_Value v = args[0];
        CEU_ACC(((CEU_Value) { CEU_VALUE_TAG, {.Tag=v.type} }));
        ceu_gc_dec_val(v);
    }
    
    CEU_Value _ceu_sup_ (CEU_Value sup, CEU_Value sub) {
        if (sup.type!=CEU_VALUE_TAG || sub.type!=CEU_VALUE_TAG) {
            return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} };
        }
        
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
    void ceu_pro_sup_question_ (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n == 2);
        CEU_ACC(_ceu_sup_(args[0], args[1]));
        ceu_gc_dec_args(n, args);
    }    
    """
    }
    fun tuple_vector_dict (): String {
        return """
    // TUPLE
    
    void ceu_tuple_set (CEU_Tuple* tup, int i, CEU_Value v) {
        ceu_gc_inc_val(v);
        ceu_gc_dec_val(tup->buf[i]);
        tup->buf[i] = v;
    }
    
    // VECTOR
    
    #define ceu_sizeof(type, member) sizeof(((type *)0)->member)
    int ceu_type_to_size (int type) {
        switch (type) {
            case CEU_VALUE_NIL:
            case CEU_VALUE_ERROR:
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
            default:
                return ceu_sizeof(CEU_Value, Dyn);
        }
    }
    
    CEU_Value ceu_vector_get (CEU_Vector* vec, int i) {
        assert(i>=0 && i<vec->its);
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
            ceu_gc_dec_val(ret);
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
                ceu_gc_inc_val(v);
                vec->its++;
                vec->buf[sz*vec->its] = '\0';
            } else {                            // set
                CEU_Value ret = ceu_vector_get(vec, i);
                assert(ret.type != CEU_VALUE_ERROR);
                ceu_gc_inc_val(v);
                ceu_gc_dec_val(ret);
                assert(i < vec->its);
            }
            memcpy(vec->buf + i*sz, (char*)&v.Number, sz);
        }
    }
    
    // DICT
    
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

    CEU_Value ceu_dict_get (CEU_Dict* dic, CEU_Value key) {
        int i;
        int ok = ceu_dict_key_to_index(dic, key, &i);
        if (ok) {
            return (*dic->buf)[i][1];
        } else {
            return (CEU_Value) { CEU_VALUE_NIL };
        }
    }
    
    char* ceu_dict_set (CEU_Dict* dic, CEU_Value key, CEU_Value val) {
        if (key.type == CEU_VALUE_NIL) {
            return "dict error : index cannot be nil";
        }
        int old;
        ceu_dict_key_to_index(dic, key, &old);
        if (old == -1) {
            old = dic->max;
            int new = MAX(5, old * 2);
            dic->max = new;
            dic->buf = realloc(dic->buf, new*2*sizeof(CEU_Value));
            assert(dic->buf != NULL);
            memset(&(*dic->buf)[old], 0, (new-old)*2*sizeof(CEU_Value));  // x[i]=nil
        }
        assert(old != -1);
        
        CEU_Value vv = ceu_dict_get(dic, key);
        
        if (val.type == CEU_VALUE_NIL) {
            ceu_gc_dec_val(vv);
            ceu_gc_dec_val(key);
            (*dic->buf)[old][0] = (CEU_Value) { CEU_VALUE_NIL };
        } else {
            ceu_gc_inc_val(val);
            ceu_gc_dec_val(vv);
            if (vv.type == CEU_VALUE_NIL) {
                ceu_gc_inc_val(key);
            }
            (*dic->buf)[old][0] = key;
            (*dic->buf)[old][1] = val;
        }
        return NULL;
    }
    
    // TUPLE / VECTOR / DICT

    char* ceu_col_check (CEU_Value col, CEU_Value idx) {
        if (col.type<CEU_VALUE_TUPLE || col.type>CEU_VALUE_DICT) {                
            return "index error : expected collection";
        }
        if (col.type != CEU_VALUE_DICT) {
            if (idx.type != CEU_VALUE_NUMBER) {
                return "index error : expected number";
            }
            if (col.type==CEU_VALUE_TUPLE && (idx.Number<0 || idx.Number>=col.Dyn->Tuple.its)) {                
                return "index error : out of bounds";
            }
            if (col.type==CEU_VALUE_VECTOR && (idx.Number<0 || idx.Number>col.Dyn->Vector.its)) {                
                return "index error : out of bounds";
            }
        }
        return NULL;
    }
    
    CEU_Value ceu_col_get (CEU_Value col, CEU_Value idx) {
        char* err = ceu_col_check(col,idx);
        if (err != NULL) {
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error=err} };
        }
        switch (col.type) {
            case CEU_VALUE_TUPLE:
                return col.Dyn->Tuple.buf[(int) idx.Number];
            case CEU_VALUE_VECTOR:
                return ceu_vector_get(&col.Dyn->Vector, idx.Number);
                break;
            case CEU_VALUE_DICT:
                return ceu_dict_get(&col.Dyn->Dict, idx);
            default:
                assert(0 && "bug found");
        }
    }
    
    char* ceu_col_set (CEU_Value col, CEU_Value idx, CEU_Value val) {
        char* err = ceu_col_check(col,idx);
        if (err != NULL) {
            return err;
        }
        switch (col.type) {
            case CEU_VALUE_TUPLE:
                ceu_tuple_set(&col.Dyn->Tuple, idx.Number, val);
                break;
            case CEU_VALUE_VECTOR:
                ceu_vector_set(&col.Dyn->Vector, idx.Number, val);
                break;
            case CEU_VALUE_DICT: {
                err = ceu_dict_set(&col.Dyn->Dict, idx, val);
                break;
            }
            default:
                assert(0 && "bug found");
        }
        return err;
    }
    
    void ceu_pro_next_dash_dict (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n==1 || n==2);
        CEU_Value dict = args[0];
        CEU_Value ret;
        if (dict.type != CEU_VALUE_DICT) {
            ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="next-dict error : expected dict"} };
        } else {
            CEU_Value key = (n == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : args[1];
            if (key.type == CEU_VALUE_NIL) {
                if (dict.Dyn->Dict.max == 0) {
                    ret = (CEU_Value) { CEU_VALUE_NIL };
                } else {
                    ret = (*dict.Dyn->Dict.buf)[0][0];
                }
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
        CEU_ACC(ret);
        ceu_gc_dec_args(n, args);
    }    
    """
    }
    fun creates (): String {
        return """
    CEU_Value ceu_create_tuple (int cpy, int n, CEU_Value args[]) {
        ceu_debug_add(CEU_VALUE_TUPLE);
        CEU_Tuple* ret = malloc(sizeof(CEU_Tuple) + n*sizeof(CEU_Value));
        assert(ret != NULL);
        *ret = (CEU_Tuple) {
            CEU_VALUE_TUPLE, 0, (CEU_Value) { CEU_VALUE_NIL },
            n, {}
        };
        if (cpy) {
            memcpy(ret->buf, args, n*sizeof(CEU_Value));
        } else {
            memset(ret->buf, 0, n*sizeof(CEU_Value));
        }
        return (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ret} };
    }
    
    void ceu_pro_tuple (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n == 1);
        CEU_Value arg = args[0];
        assert(arg.type == CEU_VALUE_NUMBER);
        CEU_ACC (
            ceu_create_tuple(0, arg.Number, args)
        );
    }
    
    CEU_Value ceu_create_vector (void) {
        ceu_debug_add(CEU_VALUE_VECTOR);
        CEU_Vector* ret = malloc(sizeof(CEU_Vector));
        assert(ret != NULL);
        char* buf = malloc(1);  // because of '\0' in empty strings
        assert(buf != NULL);
        buf[0] = '\0';
        *ret = (CEU_Vector) {
            CEU_VALUE_VECTOR, 0, (CEU_Value) { CEU_VALUE_NIL },
            0, 0, CEU_VALUE_NIL, buf
        };
        return (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=(CEU_Dyn*)ret} };
    }
    
    CEU_Value ceu_create_dict (void) {
        ceu_debug_add(CEU_VALUE_DICT);
        CEU_Dict* ret = malloc(sizeof(CEU_Dict));
        assert(ret != NULL);
        *ret = (CEU_Dict) {
            CEU_VALUE_DICT, 0, (CEU_Value) { CEU_VALUE_NIL },
            0, NULL
        };
        return (CEU_Value) { CEU_VALUE_DICT, {.Dyn=(CEU_Dyn*)ret} };
    }
    
    CEU_Value ceu_create_clo (CEU_VALUE type, CEU_Proto proto, int pars, int upvs) {
        ceu_debug_add(type);
        CEU_Clo* ret = malloc(CEU4(type==CEU_VALUE_CLO_TASK ? sizeof(CEU_Clo_Task) :) sizeof(CEU_Clo));
        assert(ret != NULL);
        CEU_Value* buf = malloc(upvs * sizeof(CEU_Value));
        assert(buf != NULL);
        for (int i=0; i<upvs; i++) {
            buf[i] = (CEU_Value) { CEU_VALUE_NIL };
        }
        *ret = (CEU_Clo) {
            type, 0, (CEU_Value) { CEU_VALUE_NIL },
            proto,
            { upvs, buf }
        };
        return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
    }

    #if 0
    #if CEU >= 4
    CEU_Value ceu_create_clo_task (CEU_Proto proto, int pars, int locs, int upvs, CEU_Exe_Task* up_tsk) {
        CEU_Value clo = ceu_create_clo(CEU_VALUE_CLO_TASK, proto, pars, locs, upvs);
        assert(clo.type == CEU_VALUE_CLO_TASK);
        clo.Dyn->Clo_Task.up_tsk = up_tsk;
        return clo;
    }
    #endif
    
    #if CEU >= 3
    CEU_Value ceu_create_exe (int type, int sz, CEU_Value clo) {
        ceu_debug_add(type);
        assert(clo.type==CEU_VALUE_CLO_CORO CEU4(|| clo.type==CEU_VALUE_CLO_TASK));
        ceu_gc_inc_val(clo);
        
        CEU_Exe* ret = malloc(sz);
        assert(ret != NULL);
        CEUX* X = malloc(sizeof(CEUX));
        CEU_Stack* S = malloc(sizeof(CEU_Stack));
        assert(X!=NULL && S!=NULL);
        S->n = 0;
        ceux_push(S, 1, clo);   // [clo]
        //S->buf = <dynamic>    // TODO
        *X = (CEUX) { S, 0, -1, -1, CEU_ACTION_INVALID, {.exe=ret} CEU4(COMMA CEU_TIME-1 COMMA NULL) };
            // X->up is set on resume, not here on creation

        *ret = (CEU_Exe) {
            type, 0, (CEU_Value) { CEU_VALUE_NIL },
            CEU_EXE_STATUS_YIELDED, clo, 0, X
        };
        
        return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
    }
    #endif
    
    #if CEU >= 4
    CEU_Value ceu_create_exe_task (CEU_Value clo, CEU_Dyn* up_dyn, CEU_Block* up_blk) {
    #if CEU >= 5
        int ceu_tasks_n (CEU_Tasks* tsks) {
            int n = 0;
            CEU_Exe_Task* cur = (CEU_Exe_Task*) tsks->lnks.dn.fst;
            while (cur != NULL) {
                n++;
                cur = (CEU_Exe_Task*) cur->lnks.sd.nxt;
            }
            return n;
        }
        if (!ceu_isexe_dyn(up_dyn)) {
            CEU_Tasks* tsks = (CEU_Tasks*) up_dyn;
            if (tsks->max!=0 && ceu_tasks_n(tsks)>=tsks->max) {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
        }
    #endif
        
        if (clo.type != CEU_VALUE_CLO_TASK) {
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error="spawn error : expected task"} };
        }

        CEU_Value ret = ceu_create_exe(CEU_VALUE_EXE_TASK, sizeof(CEU_Exe_Task), clo);
        CEU_Exe_Task* dyn = &ret.Dyn->Exe_Task;
        
        ceu_gc_inc_dyn((CEU_Dyn*) dyn);    // up_blk/tsks holds a strong reference

        dyn->time = CEU_TIME;
        dyn->pub = (CEU_Value) { CEU_VALUE_NIL };

        dyn->lnks = (CEU_Links) { {up_dyn,NULL}, {NULL,NULL}, {NULL,NULL} };

        if (CEU5(dyn!=NULL && ceu_isexe_dyn(up_dyn) &&) *up_blk==NULL) {
            dyn->lnks.up.blk = up_blk;    // only the first task points up
            *up_blk = (CEU_Dyn*) dyn;
        }
        
        if (up_dyn != NULL) {
            CEU_Links* up_lnks = CEU_LNKS(up_dyn);        
            if (up_lnks->dn.fst == NULL) {
                assert(up_lnks->dn.lst == NULL);
                up_lnks->dn.fst = (CEU_Dyn*) dyn;
            } else if (up_lnks->dn.lst != NULL) {
                CEU_LNKS(up_lnks->dn.lst)->sd.nxt = (CEU_Dyn*) dyn;
                dyn->lnks.sd.prv = up_lnks->dn.lst;
            }
            up_lnks->dn.lst = (CEU_Dyn*) dyn;
        }

        return ret;
    }
    #endif
    
    #if CEU >= 5
    CEU_Value ceu_create_tasks (int max, CEU_Exe_Task* up_tsk, CEU_Block* up_blk) {
        CEU_Tasks* ret = malloc(sizeof(CEU_Tasks));
        assert(ret != NULL);

        *ret = (CEU_Tasks) {
            CEU_VALUE_TASKS, 0, (CEU_Value) { CEU_VALUE_NIL },
            max, { {(CEU_Dyn*)up_tsk,NULL}, {NULL,NULL}, {NULL,NULL} }
        };
        
        ceu_gc_inc_dyn((CEU_Dyn*) ret);    // up_blk/tsks holds a strong reference

        {
            if (*up_blk == NULL) {
                ret->lnks.up.blk = up_blk;    // only the first task points up
                *up_blk = (CEU_Dyn*) ret;
            }
            if (up_tsk->lnks.dn.fst == NULL) {
                assert(up_tsk->lnks.dn.lst == NULL);
                up_tsk->lnks.dn.fst = (CEU_Dyn*) ret;
            } else if (up_tsk->lnks.dn.lst != NULL) {
                CEU_LNKS(up_tsk->lnks.dn.lst)->sd.nxt = (CEU_Dyn*) ret;
                ret->lnks.sd.prv = up_tsk->lnks.dn.lst;
            }
            up_tsk->lnks.dn.lst = (CEU_Dyn*) ret;
        }
        
        return (CEU_Value) { CEU_VALUE_TASKS, {.Dyn=(CEU_Dyn*)ret} };
    }
    #endif
    #endif
    """
    }
    fun print (): String {
        return """
    void ceu_print1 (CEU_Value v) {
        if (v.type > CEU_VALUE_DYNAMIC) {  // TAGS
            if (v.Dyn->Any.tag.type != CEU_VALUE_NIL) {
                ceu_print1(v.Dyn->Any.tag);
                printf(" ");
            }
        }
        switch (v.type) {
            case CEU_VALUE_NIL:
                printf("nil");
                break;
            case CEU_VALUE_ERROR:
                printf("error: %s", (v.Error==NULL ? "(null)" : v.Error));
                break;
            case CEU_VALUE_TAG:
                printf("%s", ceu_tag_to_pointer(v.Tag));
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
    #if 0
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
    #endif
            default:
                assert(0 && "bug found");
        }
    }
    void ceu_pro_print (CEU_Clo* _1, int n, CEU_Value args[]) {
        for (int i=0; i<n; i++) {
            if (i > 0) {
                printf("\t");
            }
            ceu_print1(args[i]);
        }
        CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
        ceu_gc_dec_args(n, args);
    }
    void ceu_pro_println (CEU_Clo* _1, int n, CEU_Value args[]) {
        ceu_pro_print(_1, n, args);
        printf("\n");
        CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
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
    void ceu_pro_equals_equals (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n == 2);
        CEU_ACC(_ceu_equals_equals_(args[0], args[1]));
        ceu_gc_dec_args(n, args);
    }
    void ceu_pro_slash_equals (CEU_Clo* _1, int n, CEU_Value args[]) {
        ceu_pro_equals_equals(_1, n, args);
        assert(ceu_acc.type == CEU_VALUE_BOOL);
        ceu_acc.Bool = !ceu_acc.Bool;
    }
    
    void ceu_pro_hash (CEU_Clo* _1, int n, CEU_Value args[]) {
        assert(n == 1);
        CEU_Value col = args[0];
        CEU_Value ret;
        if (col.type == CEU_VALUE_VECTOR) {
            ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=col.Dyn->Vector.its} };
        } else if (col.type == CEU_VALUE_TUPLE) {
            ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=col.Dyn->Tuple.its} };
        } else {
            ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="length error : not a vector"} };
        }
        CEU_ACC(ret);
        ceu_gc_dec_val(col);
    }
    """
    }

    val c_exes = """
        #if CEU >= 3
        int ceu_isexe_val (CEU_Value val) {
            return (val.type==CEU_VALUE_EXE_CORO CEU4(|| ceu_istask_val(val)));
        }
        int ceu_isexe_dyn (CEU_Dyn* dyn) {
            return (dyn->Any.type==CEU_VALUE_EXE_CORO CEU4(|| ceu_istask_dyn(dyn)));
        }
        void ceu_pro_coroutine (CEUX* X) {
            assert(X->args == 1);
            CEU_Value coro = ceux_peek(X->S, ceux_arg(X,0));
            CEU_Value ret;
            if (coro.type != CEU_VALUE_CLO_CORO) {
                return ceu_error_s(X->S, "coroutine error : expected coro");
            } else {
                ret = ceu_create_exe(CEU_VALUE_EXE_CORO, sizeof(CEU_Exe), coro);
            }
            ceux_push(X->S, 1, ret);
            return 1;
        }        

        void ceu_pro_status (CEUX* X) {
            assert(X->args == 1);
            CEU_Value exe = ceux_peek(X->S, ceux_arg(X,0));
            CEU_Value ret;
            if (exe.type!=CEU_VALUE_EXE_CORO CEU4(&& !ceu_istask_val(exe))) {
        #if CEU < 4
                return ceu_error_s(X->S, "status error : expected running coroutine");
        #else
                return ceu_error_s(X->S, "status error : expected running coroutine or task");
        #endif
            } else {
                ret = (CEU_Value) { CEU_VALUE_TAG, {.Tag=exe.Dyn->Exe.status + CEU_TAG_yielded - 1} };
            }
            ceux_push(X->S, 1, ret);
            return 1;
        }
        
        int ceu_exe_term (CEUX* X) {
            if (X->exe->status == CEU_EXE_STATUS_TERMINATED) {
                // leave -> outer ref -> gc_dec -> term
                return 0;
            }
            X->exe->status = CEU_EXE_STATUS_TERMINATED;
            int ret = 0;
    #if CEU >= 4
            if (X->exe->type == CEU_VALUE_EXE_TASK) {
                // task return value in pub(t)
                ceu_gc_dec_val(X->exe_task->pub);
                if (X->action==CEU_ACTION_ABORT || ceux_n_get(X->S)==0) {
                    X->exe_task->pub = (CEU_Value) { CEU_VALUE_NIL };
                } else {
                    X->exe_task->pub = ceux_peek(X->S, XX(-1));
                }
                ceu_gc_inc_val(X->exe_task->pub);
                
                // do not bcast aborted task b/c
                // it would awake parents that actually need to
                // respond/catch the error (thus not awake)
                if (X->action != CEU_ACTION_ABORT) {
                    CEU_Exe_Task* tsk = ((CEU_Exe_Task*) X->exe);
                    CEU_Dyn* up;
    #if CEU >= 5
                    if (tsk->lnks.up.dyn!=NULL && tsk->lnks.up.dyn->Any.type==CEU_VALUE_TASKS) {
                        // tsk <- pool <- tsk
                        up = CEU_LNKS(tsk->lnks.up.dyn)->up.dyn;
                    } else
    #endif
                    {
                        // tsk <- tsk
                        up = tsk->lnks.up.dyn;
                    }
                    if (up!=NULL && !CEU_ERROR_IS(X->S)) {
                        assert(CEU_TIME < UINT32_MAX);
                        CEU_TIME++;
                        int i = ceux_push(X->S, 1, ceu_dyn_to_val((CEU_Dyn*) X->exe));   // bcast myself
                        ret = ceu_bcast_dyn(X, CEU_ACTION_RESUME, CEU_TIME, up);
                        //assert(ret == 0);
                        assert(X->exe->refs >= 2);  // ensures that the unlink below is safe (otherwise call gc_inc)
                        ceux_rem_n(X->S, i, 1);
                    }
                    ceu_gc_dec_dyn((CEU_Dyn*) X->exe);  // only if natural termination
                }
                
                if (!CEU_ERROR_IS(X->S)) {
                    ceux_n_set(X->S, 0);
                }
                return ret;
            } else
    #endif
            {
                return ceux_rets(X);
            }
        }

        #if CEU >= 5
        void ceu_abort_tasks (CEU_Tasks* tsks) {
            if (tsks->lnks.up.dyn == NULL) {
                return;     // already unlinked/killed
            }
            CEU_Dyn* cur = ceu_task_get(tsks->lnks.dn.fst);
            while (cur != NULL) {
                ceu_abort_exe((CEU_Exe*) cur);
                CEU_Dyn* nxt = ceu_task_get(CEU_LNKS(cur)->sd.nxt);
                ceu_gc_dec_dyn(cur); // remove strong (block) ref
                    // - TODO: could affect nxt?
                    // no bc nxt is a strong (block) ref,
                    // so it is impossible that nxt reaches refs=0
                cur = nxt;
            }
        }
        #endif

        void ceu_abort_exe (CEU_Exe* exe) {
            assert(ceu_isexe_dyn((CEU_Dyn*) exe));
            switch (exe->status) {
                case CEU_EXE_STATUS_TERMINATED:
                    // do nothing;
                    break;
                case CEU_EXE_STATUS_RESUMED:
                    exe->status = CEU_EXE_STATUS_TERMINATED;
                    break;
        #if CEU >= 4
                case CEU_EXE_STATUS_TOGGLED:
        #endif
                case CEU_EXE_STATUS_YIELDED:
                {
                    // TODO - fake S/X - should propagate up to calling stack
                    // TODO - fake now - should receive as arg (not CEU_TIME)
                    CEU_Stack S = { 0, {} };
                    CEUX _X = { &S, -1, -1, -1, CEU_ACTION_INVALID, {.exe=NULL} CEU4(COMMA CEU_TIME COMMA NULL) };
                    CEUX* X = &_X;
                    ceux_push(&S, 1, ceu_dyn_to_val((CEU_Dyn*) exe));
                    // S: [co]
                    int ret = ceux_resume(X, 0, 0, CEU_ACTION_ABORT CEU4(COMMA CEU_TIME));
                    if (ret != 0) {
                        assert(CEU_ERROR_IS(&S) && "TODO: abort should not return");
                        assert(0 && "TODO: error in ceu_exe_kill");
                    }
                }
            }
        }
        #endif
    """
    val c_task = """ // TASK
        #if CEU >= 4
        void ceu_dyn_unlink (CEU_Dyn* dyn) {
            CEU_Links* me_lnks = CEU_LNKS(dyn);
            {   // UP-DYN-DN
                if (me_lnks->up.dyn != NULL) {
                    CEU_Links* up_lnks = CEU_LNKS(me_lnks->up.dyn);
                    me_lnks->up.dyn = NULL;
                    if (up_lnks->dn.fst == dyn) {
                        assert(me_lnks->sd.prv == NULL);
                        up_lnks->dn.fst = me_lnks->sd.nxt;
                    }
                    if (up_lnks->dn.lst == dyn) {
                        assert(me_lnks->sd.nxt == NULL);
                        up_lnks->dn.lst = me_lnks->sd.prv;
                    }
                }
            }
            {   // UP-BLK-DN
                if (me_lnks->up.blk != NULL) {
                    *me_lnks->up.blk = me_lnks->sd.nxt;
                    if (me_lnks->sd.nxt != NULL) {
                        CEU_LNKS(me_lnks->sd.nxt)->up.blk = me_lnks->up.blk;
                    }
                    me_lnks->up.blk = NULL; // also on ceux_block_leave (to prevent dangling pointer)
                }
            }
            {   // SD
                if (me_lnks->sd.prv != NULL) {
                    CEU_LNKS(me_lnks->sd.prv)->sd.nxt = me_lnks->sd.nxt;
                }
                if (me_lnks->sd.nxt != NULL) {
                    CEU_LNKS(me_lnks->sd.nxt)->sd.prv = me_lnks->sd.prv;
                }
                //me_lnks->sd.prv = me_lnks->sd.nxt = NULL;
                    // prv/nxt are never reached again:
                    //  - it is not a problem to keep the dangling pointers
                    // but we actually should not set them NULL:
                    //  - tsk might be in bcast_tasks which must call nxt
            }
            {   // DN
                CEU_Dyn* cur = me_lnks->dn.fst;
                if (me_lnks->dn.fst == NULL) {
                    assert(me_lnks->dn.lst == NULL);
                }
                while (cur != NULL) {
                    CEU_Links* dn_lnks = CEU_LNKS(cur);
                    dn_lnks->up.dyn = NULL;
                    cur = dn_lnks->sd.nxt;
                }
                me_lnks->dn.fst = me_lnks->dn.lst = NULL;
            }
        }
        
        int ceu_istask_dyn (CEU_Dyn* dyn) {
            return (dyn->Any.type == CEU_VALUE_EXE_TASK);
        }
        int ceu_istask_val (CEU_Value val) {
            return (val.type>CEU_VALUE_DYNAMIC) && ceu_istask_dyn(val.Dyn);
        }
        #endif
    """
    val c_bcast = """
        #if CEU >= 4
        int ceu_bcast_tasks (CEUX* X1, CEU_ACTION act, uint32_t now, CEU_Dyn* dyn2) {
            //assert(dyn2!=NULL && (dyn2->type==CEU_VALUE_EXE_TASK CEU5(|| dyn2->type==CEU_VALUE_TASKS)));
            int ret = 0;
            CEU_Links* lnks = CEU_LNKS(dyn2);
            CEU_Dyn* cur = ceu_task_get(lnks->dn.fst);
            while (cur != NULL) {
                ceu_gc_inc_dyn(cur);
                ret = ceu_bcast_dyn(X1, act, now, cur);
                CEU_Dyn* nxt = ceu_task_get(CEU_LNKS(cur)->sd.nxt);
                ceu_gc_dec_dyn(cur); // TODO: could affect nxt?
                if (ret != 0) {
                    break;
                }
                cur = nxt;
            }
            return ret;
        }
        int ceu_bcast_task (CEUX* X1, CEU_ACTION act, uint32_t now, CEU_Exe_Task* tsk2) {            
            // bcast order: DN -> ME
            //  - DN:  nested tasks
            //  - ME:  my yield point
            
            // X1: [evt]    // must keep as is at the end bc outer bcast pops it
            
            assert(tsk2!=NULL && tsk2->type==CEU_VALUE_EXE_TASK);
            assert(act == CEU_ACTION_RESUME);
            
            if (tsk2->status == CEU_EXE_STATUS_TERMINATED) {
                return 0;
            }
            
            ceu_gc_inc_dyn((CEU_Dyn*) tsk2);
            int ret = 0; // !=0 means error

            // DN
            if (act==CEU_ACTION_RESUME && tsk2->status==CEU_EXE_STATUS_TOGGLED) {
                // do nothing
            } else {
                ret = ceu_bcast_tasks(X1, act, now, (CEU_Dyn*) tsk2);
            }

            // ME
            if (tsk2->status != CEU_EXE_STATUS_YIELDED) {
                // do nothing
            } else if (tsk2 == &CEU_GLOBAL_TASK) {
                // do nothing
            } else {
                // either handle error or event
                // never both
                // even if error is caught, should not awake from past event
                if (ret != 0) {
                    // catch error from blocks above
                    assert(CEU_ERROR_IS(X1->S));
                    // [evt, (ret,err)]
                    ceux_push(X1->S, 1, ceu_dyn_to_val((CEU_Dyn*)tsk2));
                    int err = XX1(-ret-1);
                    ceux_dup_n(X1->S, err, ret);
                    // [evt, (ret,err), tsk, (ret,err)]
                    int ret2 = ceux_resume(X1, ret, 0, CEU_ACTION_ERROR, now);
                    if (ret2 == 0) {
                        ceux_pop_n(X1->S, ret);
                        // [evt]
                    } else {
                        // [evt, (ret,err)]
                    }
                    ret = ret2;
                } else if (tsk2->pc==0 || now>tsk2->time) {
                    // [evt]
                    ceux_push(X1->S, 1, ceu_dyn_to_val((CEU_Dyn*)tsk2));
                    ceux_dup(X1->S, XX1(-2));
                    // [evt,tsk,evt]
                    ret = ceux_resume(X1, 1 /* TODO-MULTI */, 0, CEU_ACTION_RESUME, now);
                    // [evt]
                }
            }
            
            ceu_gc_dec_dyn((CEU_Dyn*) tsk2);
            return ret;
        }

        int ceu_broadcast_global (void) {
            assert(CEU_TIME < UINT32_MAX);
            CEU_TIME++;
            int ret = ceu_bcast_tasks(CEU_GLOBAL_X, CEU_ACTION_RESUME, CEU_TIME, (CEU_Dyn*) &CEU_GLOBAL_TASK);
            return ret;
        }
        
        void ceu_pro_broadcast_plic_ (CEUX* X) {
            assert(X->args == 2);
            //ceu_bstk_assert(bstk);

            assert(CEU_TIME < UINT32_MAX);
            CEU_TIME++;

            CEU_Value xin = ceux_peek(X->S, ceux_arg(X,0));
            int ret;
            if (xin.type == CEU_VALUE_TAG) {
                if (xin.Tag == CEU_TAG_global) {
                    ret = ceu_bcast_tasks(X, CEU_ACTION_RESUME, CEU_TIME, (CEU_Dyn*) &CEU_GLOBAL_TASK);
                } else if (xin.Tag == CEU_TAG_task) {
                    if (X->exe_task == NULL) {
                        ret = ceu_bcast_tasks(X, CEU_ACTION_RESUME, CEU_TIME, (CEU_Dyn*) &CEU_GLOBAL_TASK);
                    } else {
                        ret = ceu_bcast_task(X, CEU_ACTION_RESUME, CEU_TIME, X->exe_task);
                    }
                } else {
                    ret = ceu_error_s(X->S, "broadcast error : invalid target");
                }
            } else {
                if (ceu_istask_val(xin)) {
                    if (xin.Dyn->Exe_Task.status == CEU_EXE_STATUS_TERMINATED) {
                        ret = 0;
                    } else {
                        ret = ceu_bcast_task(X, CEU_ACTION_RESUME, CEU_TIME, &xin.Dyn->Exe_Task);
                    }
                } else {
                    ret = ceu_error_s(X->S, "broadcast error : invalid target");
                }
            }

            return ret;
        }
        #endif
    """
    val c_tasks = """
        #if CEU >= 5
        void ceu_pro_next_dash_tasks (CEUX* X) {
            assert(X->args==1 || X->args==2);
            CEU_Value tsks = ceux_peek(X->S, ceux_arg(X,0));
            if (tsks.type != CEU_VALUE_TASKS) {
                return ceu_error_s(X->S, "next-tasks error : expected tasks");
            }
            CEU_Value key = (X->args == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : ceux_peek(X->S,ceux_arg(X,1));
            CEU_Dyn* nxt = NULL;
            switch (key.type) {
                case CEU_VALUE_NIL:
                    nxt = tsks.Dyn->Tasks.lnks.dn.fst;
                    break;
                case CEU_VALUE_EXE_TASK:
                    nxt = key.Dyn->Exe_Task.lnks.sd.nxt;
                    break;
                default:
                    return ceu_error_s(X->S, "next-tasks error : expected task");
            }
            if (nxt == NULL) {
                ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_NIL });
            } else {
                ceux_push(X->S, 1, ceu_dyn_to_val(nxt));
            }
            return 1;
        }
        void ceu_pro_tasks (CEUX* X) {
            assert(X->args <= 1);
            int max = 0;
            if (X->args == 1) {
                CEU_Value xmax = ceux_peek(X->S, ceux_arg(X,0));
                if (xmax.type!=CEU_VALUE_NUMBER || xmax.Number<=0) {                
                    return ceu_error_s(X->S, "tasks error : expected positive number");
                }
                max = xmax.Number;
            }
            CEU_Value ret = ceu_create_tasks(max, ceu_up_tsk(X), ceu_up_blk(X->S));
            ceux_push(X->S, 1, ret);
            return 1;
        }
        #endif
    """

    // GLOBALS
    fun c_globals (): String {
        val pres = GLOBALS.map {
            val id = it.idc()
            """
            CEU_Clo ceu_clo_$id = {
                CEU_VALUE_CLO_FUNC, 0, (CEU_Value) { CEU_VALUE_NIL },
                ceu_pro_$id, { 0, NULL }
            };
            CEU_Value ceu_glb_$id = {
                CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_clo_$id}
            };
            """
        }.joinToString("")
        val poss = vars.blk_to_dcls[outer]!!.map {
            val id = it.idtag.first.str.idc()
            """
            CEU_Value ceu_glb_$id = { CEU_VALUE_NIL };    
            """
        }.joinToString("")
        return pres + poss
    }

    // MAIN
    fun main (): String {
        return """
    ${this.pres.joinToString("")}
    
    int main (int ceu_argc, char** ceu_argv) {
        assert(CEU_TAG_nil == CEU_VALUE_NIL);
        
        ${do_while(this.code)}

        // uncaught throw
    #if CEU >= 2
        if (CEU_ERROR_IS(X->S)) {
            // [...,n,pay,err]
            CEU_Value n = ceux_peek(X->S, XX(-3));
            assert(n.type == CEU_VALUE_NUMBER);
            // ignore i=0 (main call)
            for (int i=1; i<n.Number; i++) {
                printf(" |  ");
                CEU_Value pre = ceux_peek(X->S, XX(-4-i));
                assert(pre.type==CEU_VALUE_POINTER && pre.Pointer!=NULL);
                printf("%s\n", (char*) pre.Pointer);
            }
            CEU_Value pay = ceux_peek(X->S, XX(-2));
            if (pay.type == CEU_VALUE_POINTER) {
                assert(pay.Pointer != NULL);
                printf(" v  %s\n", (char*) pay.Pointer);     // payload is primitive error
            } else {
                printf(" v  error : ");
                ceu_print1(ceux_peek(X->S, XX(-2)));
                puts("");
            }
        }
    #endif
        return 0;
    }
    """
    }

    return (
        h_includes() + h_defines() + h_enums() +
        h_value_dyn() + h_tags() +
        x_globals() + /* h_protos() +
        */
        gc() + c_tags() +
        c_error +
        c_impls() + /*
        // block-task-up, hold, bcast
        */
        eq_neq_len() +
        creates() + tuple_vector_dict() +
        c_to() + print() + dumps() + /*
        // throw, pointer-to-string
        (CEU>=3).cond { c_exes } +
        (CEU>=4).cond { c_task } +
        (CEU>=4).cond { c_bcast } +
        (CEU>=5).cond { c_tasks } +
        // isexe-coro-status-exe-kill, task, track
         */
        c_globals() +
        main()
    )
}
