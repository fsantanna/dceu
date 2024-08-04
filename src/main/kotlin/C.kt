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

    #define CEU_ACC(v) ({               \
        CEU_Value ceu_old = ceu_acc;    \
        CEU_Value ceu_new = v;          \
        ceu_acc = (CEU_Value) { CEU_VALUE_NIL }; \
        ceu_gc_inc_val(ceu_new);        \
        ceu_gc_dec_val(ceu_old);        \
        ceu_acc = ceu_new;              \
        ceu_acc;                        \
    })
    #define CEU_ACC_KEEP() ({                       \
        CEU_Value ceu_tmp = ceu_acc;                \
        ceu_acc = (CEU_Value) { CEU_VALUE_NIL };    \
        ceu_tmp;                                    \
    })
    """
    }
    fun h_enums (): String {
        return """
    #if CEU >= 3
    typedef enum CEU_ACTION {
        CEU_ACTION_INVALID = -1,    // default to force set
        //CEU_ACTION_CALL,
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
            unsigned long Tag;
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

    #if CEU >= 3
        struct CEU_Exe;
    #endif
    struct CEU_Clo;
    typedef struct CEUX {
        struct CEU_Clo* clo;
    #if CEU >= 3
        union {
            struct CEU_Exe* exe;
    #if CEU >= 4
            struct CEU_Exe_Task* exe_task;
    #endif
        };
        CEU_ACTION act;
    #if CEU >= 4
        //uint32_t now;
        struct CEUX* up;
    #endif
    #endif
        int n;
        CEU_Value* args;
    } CEUX;
    typedef void (*CEU_Proto) (CEUX* x);

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
    
    #if CEU >= 3
    typedef struct CEU_Clo_Exe {
        _CEU_Clo_
        int mem_n;
    } CEU_Clo_Exe;    

    #define _CEU_Exe_                   \
        _CEU_Dyn_                       \
        CEU_Clo* clo;                   \
        CEU_EXE_STATUS status;          \
        int pc;                         \
        char* mem;
        
    typedef struct CEU_Exe {
        _CEU_Exe_
    } CEU_Exe;
    #endif
    
    #if CEU >= 4
    // block points to first task/tasks
    typedef union CEU_Dyn* CEU_Block;

    struct CEU_Exe_Task;
    typedef struct CEU_Links {
        struct {
            union {
                union CEU_Dyn* dyn;
                struct CEU_Exe_Task* tsk;     // (also to access outer var from nested)
    #if CEU >= 5
                struct CEU_Tasks* tsks;
    #endif
            };
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

    typedef struct CEU_Exe_Task {
        _CEU_Exe_
        uint32_t time;      // last sleep time, only awakes if CEU_TIME>time 
        CEU_Value pub;
        CEU_Links lnks;
    } CEU_Exe_Task;
    #endif
    
    #if CEU >= 5
        #define CEU_LNKS(dyn) ((dyn)->Any.type==CEU_VALUE_TASKS ? &(dyn)->Tasks.lnks : &(dyn)->Exe_Task.lnks)
    #else
        #define CEU_LNKS(dyn) (&((CEU_Exe_Task*) dyn)->lnks)
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
    #if CEU >= 3
        struct CEU_Clo_Exe  Clo_Exe;
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
    
    #define CEU_ESCAPE_NONE -1
    #define CEU_ERROR_NONE -1
    int CEU_ESCAPE = CEU_ESCAPE_NONE;
    int CEU_ERROR = CEU_ERROR_NONE;

    #if CEU >= 2
    CEU_Value CEU_ERROR_STACK;
    #endif

    #if CEU >= 4
    uint32_t CEU_TIME = 0;
    CEU_Exe_Task CEU_GLOBAL_TASK = {
        CEU_VALUE_EXE_TASK, 1, (CEU_Value) { CEU_VALUE_NIL },
        NULL, CEU_EXE_STATUS_YIELDED, 0, NULL,
        0, {}, { {{NULL},NULL}, {NULL,NULL}, {NULL,NULL} }
    };
    #endif
    """
    }
    fun dumps (): String {
        return """
#ifndef CEU_DEBUG
    void ceu_pro_dump (CEUX* X) {
        assert(0 && "DEBUG is off");
    }
#else
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
    void ceu_dump_dyn (CEU_Dyn* dyn) {
        ceu_dump_val(ceu_dyn_to_val(dyn));
    }
    void ceu_pro_dump (CEUX* X) {
        for (int i=0; i<X->n; i++) {
            ceu_dump_val(X->args[i]);
        }
        CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
        ceu_gc_dec_args(X->n, X->args);
    }
    #if 0
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
    #define CEU_ERROR_PTR(ptr) ({   \
        CEU_ERROR = CEU_TAG_nil;    \
        (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=ptr} }; \
    })

    #if CEU <= 1
    
    #define CEU_ERROR_CHK_ERR(cmd,pre) {        \
        if (CEU_ERROR != CEU_ERROR_NONE) {      \
            fprintf(stderr,                     \
                " |  %s\n v  error : %s\n",     \
                pre,                            \
                (CEU_ERROR == CEU_TAG_nil) ?    \
                    (char*)ceu_acc.Pointer :    \
                    (char*)ceu_tag_to_pointer(CEU_ERROR) \
            );                                  \
            exit(0);                            \
        }                                       \
    }
    
    #define CEU_ERROR_CHK_PTR(cmd,ptr,pre) {    \
        if ((ptr) != NULL) {                    \
            fprintf(stderr, " |  %s\n v  error : %s\n", pre, ptr); \
            exit(0);                            \
        }                                       \
    }
    
    void ceu_pro_error (CEUX* X) {
        assert(X->n == 1);
        CEU_Value tag = X->args[0];
        assert(tag.type == CEU_VALUE_TAG);
        CEU_ERROR = tag.Tag;
    }

    #else

    CEU_Value ceu_pointer_to_string (const char* ptr);
    CEU_Value ceu_create_vector (void);

    #define CEU_ERROR_CHK_ERR(cmd,pre) {            \
        if (CEU_ERROR != CEU_ERROR_NONE) {          \
            if (pre != NULL) { /* opt stack msg */  \
                ceu_vector_set (                    \
                    &CEU_ERROR_STACK.Dyn->Vector,       \
                    CEU_ERROR_STACK.Dyn->Vector.its,    \
                    ceu_pointer_to_string(pre)      \
                );                                  \
            }                                       \
            cmd;                                    \
        }                                           \
    }
    
    #define CEU_ERROR_CHK_PTR(cmd,ptr,pre) {        \
        if ((ptr) != NULL) {                        \
            CEU_ACC(CEU_ERROR_PTR(ptr));            \
            ceu_vector_set (                        \
                &CEU_ERROR_STACK.Dyn->Vector,       \
                CEU_ERROR_STACK.Dyn->Vector.its,    \
                ceu_pointer_to_string(pre)          \
            );                                      \
            cmd;                                    \
        }                                           \
    }
    
    void ceu_pro_error (CEUX* X) {
        CEU_Value tag; {
            if (X->n==0 || X->args[0].type==CEU_VALUE_NIL) {
                tag = (CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_nil} };
            } else {
                tag = X->args[0];
            }
        }
        assert(tag.type == CEU_VALUE_TAG);
        CEU_ERROR = tag.Tag;
        CEU_ACC (
            (X->n >= 2) ? X->args[1] : tag;
        );
        ceu_gc_dec_args(X->n, X->args);
    }

    #endif
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
    #if CEU >= 3
    void ceu_abort_exe (CEU_Exe* exe);
    #endif
    #if CEU >= 4
    void ceu_dyn_unlink (CEU_Dyn* dyn);
    #endif
    #if CEU >= 5
    void ceu_abort_tasks (CEU_Tasks* tsks);
    #endif

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
                    //assert(dyn->Any.type == CEU_VALUE_EXE_CORO);
                    dyn->Any.refs++;            // currently 0->1: needs ->2 to prevent double gc
                    ceu_abort_exe((CEU_Exe*)dyn);
                    dyn->Any.refs--;
                }
                ceu_gc_dec_dyn((CEU_Dyn*)dyn->Exe.clo);
#if CEU >= 4
                if (dyn->Any.type == CEU_VALUE_EXE_TASK) {
                    ceu_gc_dec_val(((CEU_Exe_Task*)dyn)->pub);
                    ceu_dyn_unlink(dyn);
                }
#endif
                free(dyn->Exe.mem);
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
    
    void ceu_tag_set (CEU_Value tag, CEU_Value dyn) {
        ceu_gc_inc_val(tag);
        assert(dyn.type >= CEU_VALUE_DYNAMIC);
        assert(tag.type==CEU_VALUE_NIL || tag.type==CEU_VALUE_TAG);
        dyn.Dyn->Any.tag = tag;
    }
    
    void ceu_pro_tag (CEUX* X) {
        assert(X->n==1 || X->n==2);
        if (X->n == 1) {
            CEU_Value dyn = X->args[0];
            if (dyn.type < CEU_VALUE_DYNAMIC) {
                CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));
            } else {
                CEU_ACC(dyn.Dyn->Any.tag);
            }
        } else {
            CEU_Value tag = X->args[0];
            CEU_Value dyn = X->args[1];
            ceu_tag_set(tag, dyn);
            CEU_ACC(dyn);
        }
        ceu_gc_dec_args(X->n, X->args);
    }            
        """
    }

    fun c_to (): String {
        return """
    // TO-TAG-STRING
    void ceu_pro_to_dash_tag_dash_string (CEUX* X) {
        assert(X->n == 1);
        CEU_Value str = X->args[0];
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
    
    void ceu_pro_to_dash_string_dash_pointer (CEUX* X) {
        assert(X->n == 1);
        CEU_Value ptr = X->args[0];
        assert(ptr.type==CEU_VALUE_POINTER && ptr.Pointer!=NULL);
        CEU_ACC(ceu_pointer_to_string(ptr.Pointer));
    }

    void ceu_pro_to_dash_string_dash_tag (CEUX* X) {
        assert(X->n == 1);
        CEU_Value tag = X->args[0];
        assert(tag.type == CEU_VALUE_TAG);        
        CEU_ACC(ceu_pointer_to_string(ceu_tag_to_pointer(tag.Tag)));
    }

    void ceu_pro_to_dash_string_dash_number (CEUX* X) {
        assert(X->n == 1);
        CEU_Value num = X->args[0];
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
    CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn) {
        return (CEU_Value) { dyn->Any.type, {.Dyn=dyn} };
    }
    
    int ceu_is_string (CEU_Value v) {
        return (v.type == CEU_VALUE_VECTOR) && (v.Dyn->Vector.its > 0) && (v.Dyn->Vector.unit == CEU_VALUE_CHAR);
    }
    
    int ceu_as_bool (CEU_Value v) {
        return !(v.type==CEU_VALUE_NIL || (v.type==CEU_VALUE_BOOL && !v.Bool));
    }
    void ceu_pro_type (CEUX* X) {
        assert(X->n==1 && "bug found");
        CEU_Value v = X->args[0];
        CEU_ACC(((CEU_Value) { CEU_VALUE_TAG, {.Tag=v.type} }));
        ceu_gc_dec_val(v);
    }
    
    int _ceu_sup_ (uint32_t sup, uint32_t sub) {
        //printf("sup=0x%08X vs sub=0x%08X\n", sup->Tag, sub->Tag);
        int sup0 = sup & 0x000000FF;
        int sup1 = sup & 0x0000FF00;
        int sup2 = sup & 0x00FF0000;
        int sup3 = sup & 0xFF000000;
        int sub0 = sub & 0x000000FF;
        int sub1 = sub & 0x0000FF00;
        int sub2 = sub & 0x00FF0000;
        int sub3 = sub & 0xFF000000;

        return 
            (sup0 == sub0) && ((sup1 == 0) || (
                (sup1 == sub1) && ((sup2 == 0) || (
                    (sup2 == sub2) && ((sup3 == 0) || (
                        (sup3 == sub3)
                    ))
                ))
            ));
    }
    void ceu_pro_sup_question_ (CEUX* X) {
        assert(X->n == 2);
        CEU_Value sup = X->args[0];
        CEU_Value sub = X->args[1];
        if (sup.type!=CEU_VALUE_TAG || sub.type!=CEU_VALUE_TAG) {
            CEU_ACC (
                ((CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} })
            );
        } else {
            CEU_ACC (
                ((CEU_Value) { CEU_VALUE_BOOL, {.Bool=_ceu_sup_(X->args[0].Tag,X->args[1].Tag)} })
            );
        }
        ceu_gc_dec_args(X->n, X->args);
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
        if (i<0 || i>=vec->its) {
            return CEU_ERROR_PTR("index error : out of bounds");
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
            assert(CEU_ERROR == CEU_ERROR_NONE);
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
                assert(CEU_ERROR == CEU_ERROR_NONE);
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
            assert(CEU_ERROR == CEU_ERROR_NONE);
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
            return CEU_ERROR_PTR(err);
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
    
    void ceu_pro_next_dash_dict (CEUX* X) {
        assert(X->n==1 || X->n==2);
        CEU_Value dict = X->args[0];
        CEU_Value ret;
        if (dict.type != CEU_VALUE_DICT) {
            ret = CEU_ERROR_PTR("next-dict error : expected dict");
        } else {
            CEU_Value key = (X->n == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : X->args[1];
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
                    assert(CEU_ERROR == CEU_ERROR_NONE);
                    if (ok.Bool) {
                        ret = (*dict.Dyn->Dict.buf)[i+1][0];
                        break;
                    }
                }
            }
        }
        CEU_ACC(ret);
        ceu_gc_dec_args(X->n, X->args);
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
    
    void ceu_pro_tuple (CEUX* X) {
        assert(X->n == 1);
        CEU_Value arg = X->args[0];
        assert(arg.type == CEU_VALUE_NUMBER);
        CEU_ACC (
            ceu_create_tuple(0, arg.Number, X->args)
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
    
    CEU_Value _ceu_create_clo_ (CEU_VALUE type, int size, CEU_Proto proto, int pars, int upvs) {
        ceu_debug_add(type);
        CEU_Clo* ret = malloc(size);
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

    CEU_Value ceu_create_clo_func (CEU_Proto proto, int pars, int upvs) {
        return _ceu_create_clo_(CEU_VALUE_CLO_FUNC, sizeof(CEU_Clo), proto, pars, upvs);
    }

    #if CEU >= 3
    CEU_Value ceu_create_clo_coro (CEU_Proto proto, int pars, int upvs, int mem_n) {
        CEU_Value clo = _ceu_create_clo_(CEU_VALUE_CLO_CORO, sizeof(CEU_Clo_Exe), proto, pars, upvs);
        clo.Dyn->Clo_Exe.mem_n = mem_n;
        return clo;
    }

    CEU_Value ceu_create_exe (int type, int sz, CEU_Value clo) {
        ceu_debug_add(type);
        assert(clo.type==CEU_VALUE_CLO_CORO CEU4(|| clo.type==CEU_VALUE_CLO_TASK));
        ceu_gc_inc_val(clo);
        
        CEU_Exe* ret = malloc(sz);
        char* mem = malloc(clo.Dyn->Clo_Exe.mem_n);
        assert(ret!=NULL && mem!=NULL);

        *ret = (CEU_Exe) {
            type, 0, (CEU_Value) { CEU_VALUE_NIL },
            &clo.Dyn->Clo, CEU_EXE_STATUS_YIELDED, 0, mem
        };
        
        return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
    }
    #endif
    
    #if CEU >= 4
    CEU_Value ceu_create_clo_task (CEU_Proto proto, int pars, int upvs, int mem_n) {
        CEU_Value clo = _ceu_create_clo_(CEU_VALUE_CLO_TASK, sizeof(CEU_Clo_Exe), proto, pars, upvs);
        clo.Dyn->Clo_Exe.mem_n = mem_n;
        return clo;
    }

    #if CEU >= 5
    int ceu_isexe_dyn (CEU_Dyn* dyn);
    #endif
    
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
            return CEU_ERROR_PTR("spawn error : expected task");
        }

        CEU_Value ret = ceu_create_exe(CEU_VALUE_EXE_TASK, sizeof(CEU_Exe_Task), clo);
        CEU_Exe_Task* dyn = &ret.Dyn->Exe_Task;
        
        ceu_gc_inc_dyn((CEU_Dyn*) dyn);
            // up_blk/tsks holds a strong reference
            // removed on natural termination

        dyn->time = CEU_TIME;
        dyn->pub = (CEU_Value) { CEU_VALUE_NIL };

        dyn->lnks = (CEU_Links) { {{up_dyn},NULL}, {NULL,NULL}, {NULL,NULL} };

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
    
    #if CEU >= 5        
    CEU_Exe_Task* ceu_task_up (CEUX* X);
    CEU_Value ceu_create_tasks (CEUX* X, CEU_Block* up_blk, CEU_Value max) {
        CEU_Exe_Task* up_tsk = ceu_task_up(X);

        int xmax = 0;
        if (max.type == CEU_VALUE_NIL) {
            // ok
        } else if (max.type==CEU_VALUE_NUMBER && max.Number>0) {
            xmax = max.Number;
        } else {
            return CEU_ERROR_PTR("tasks error : expected positive number");
        }

        CEU_Tasks* ret = malloc(sizeof(CEU_Tasks));
        assert(ret != NULL);

        *ret = (CEU_Tasks) {
            CEU_VALUE_TASKS, 0, (CEU_Value) { CEU_VALUE_NIL },
            xmax, { {{(CEU_Dyn*)up_tsk},NULL}, {NULL,NULL}, {NULL,NULL} }
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
                        assert(CEU_ERROR == CEU_ERROR_NONE);
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
    void ceu_pro_print (CEUX* X) {
        for (int i=0; i<X->n; i++) {
            if (i > 0) {
                printf("\t");
            }
            ceu_print1(X->args[i]);
        }
        CEU_ACC((X->n > 0) ? X->args[0] : (CEU_Value) { CEU_VALUE_NIL });
        ceu_gc_dec_args(X->n, X->args);
    }
    void ceu_pro_println (CEUX* X) {
        ceu_pro_print(X);
        printf("\n");
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
    void ceu_pro_equals_equals (CEUX* X) {
        assert(X->n == 2);
        CEU_ACC(_ceu_equals_equals_(X->args[0], X->args[1]));
        ceu_gc_dec_args(X->n, X->args);
    }
    void ceu_pro_slash_equals (CEUX* X) {
        ceu_pro_equals_equals(X);
        assert(ceu_acc.type == CEU_VALUE_BOOL);
        ceu_acc.Bool = !ceu_acc.Bool;
    }
    
    void ceu_pro_hash (CEUX* X) {
        assert(X->n == 1);
        CEU_Value col = X->args[0];
        CEU_Value ret;
        if (col.type == CEU_VALUE_VECTOR) {
            ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=col.Dyn->Vector.its} };
        } else if (col.type == CEU_VALUE_TUPLE) {
            ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=col.Dyn->Tuple.its} };
        } else {
            ret = CEU_ERROR_PTR("length error : not a vector");
        }
        CEU_ACC(ret);
        ceu_gc_dec_val(col);
    }
    """
    }

    val c_exes = """
    #if CEU >= 3
        int ceu_isexe_val (CEU_Value val) {
            return (val.type==CEU_VALUE_EXE_CORO CEU4(|| val.type==CEU_VALUE_EXE_TASK));
        }
        int ceu_isexe_dyn (CEU_Dyn* dyn) {
            return (dyn->Any.type==CEU_VALUE_EXE_CORO CEU4(|| dyn->Any.type==CEU_VALUE_EXE_TASK));
        }
        void ceu_pro_coroutine (CEUX* X) {
            assert(X->n == 1);
            CEU_Value clo = X->args[0];
            CEU_Value ret;
            if (clo.type != CEU_VALUE_CLO_CORO) {
                ret = CEU_ERROR_PTR("coroutine error : expected coro");
            } else {
                ret = ceu_create_exe(CEU_VALUE_EXE_CORO, sizeof(CEU_Exe), clo);
            }
            ceu_gc_dec_val(clo);
            CEU_ACC(ret);
        }        

        void ceu_pro_status (CEUX* X) {
            assert(X->n == 1);
            CEU_Value exe = X->args[0];
            CEU_Value ret;
            if (exe.type!=CEU_VALUE_EXE_CORO CEU4(&& exe.type!=CEU_VALUE_EXE_TASK)) {
        #if CEU < 4
                ret = CEU_ERROR_PTR("status error : expected running coroutine");
        #else
                ret = CEU_ERROR_PTR("status error : expected running coroutine or task");
        #endif
            } else {
                ret = (CEU_Value) { CEU_VALUE_TAG, {.Tag=exe.Dyn->Exe.status + CEU_TAG_yielded - 1} };
            }
            ceu_gc_dec_val(exe);
            CEU_ACC(ret);
        }

    #if CEU >= 4
        void ceu_bcast_task (CEU_Exe_Task* tsk, uint32_t now, CEU_Value* evt);
    #endif

        void ceu_exe_term (CEUX* X) {
            if (X->exe->status == CEU_EXE_STATUS_TERMINATED) {
                return;
            }
            X->exe->status = CEU_EXE_STATUS_TERMINATED;
    #if CEU >= 4
            if (X->exe->type != CEU_VALUE_EXE_TASK) {
                return;
            }
            if (X->act == CEU_ACTION_ABORT) {
                return;
            }
            
            ceu_gc_dec_val(X->exe_task->pub);
            X->exe_task->pub = ceu_acc;
            ceu_gc_inc_val(X->exe_task->pub);
            
            // do not bcast aborted task b/c
            // it would awake parents that actually need to
            // respond/catch the error (thus not awake)
            CEU_Exe_Task* tsk = ((CEU_Exe_Task*) X->exe);
            CEU_Exe_Task* up;
    #if CEU >= 5
            if (tsk->lnks.up.dyn!=NULL && tsk->lnks.up.dyn->Any.type==CEU_VALUE_TASKS) {
                // tsk <- pool <- tsk
                up = CEU_LNKS(tsk->lnks.up.dyn)->up.tsk;
            } else
    #endif
            {
                // tsk <- tsk
                up = tsk->lnks.up.tsk;
                assert(up->type == CEU_VALUE_EXE_TASK);
            }
            assert(up != NULL);
            assert(CEU_TIME < UINT32_MAX);
            CEU_TIME++;
            CEU_Value evt = ceu_dyn_to_val((CEU_Dyn*) X->exe);
            ceu_bcast_task(up, CEU_TIME, &evt);
            ceu_gc_dec_dyn((CEU_Dyn*) X->exe);  // only if natural termination
    #endif
        }

    #if CEU >= 4
    #if CEU >= 5
        #define ceu_abort_dyn(a) (a->Any.type==CEU_VALUE_TASKS ? ceu_abort_tasks((CEU_Tasks*)a) : ceu_abort_exe((CEU_Exe*)a))
    #else
        #define ceu_abort_dyn(a) ceu_abort_exe((CEU_Exe*)a)
    #endif
    #endif

    #if CEU >= 5
        CEU_Dyn* ceu_task_get (CEU_Dyn* cur);
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
                    int error = CEU_ERROR;
                    CEU_ERROR = CEU_ERROR_NONE;
                    CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
                    CEU_Value args[1];
                    CEUX ceux = {
                        exe->clo,
                        {.exe = (CEU_Exe*) exe},
                        CEU_ACTION_ABORT,
        #if CEU >= 4
                        NULL,   // TODO: no access to up(ceux)
        #endif
                        0,
                        args
                    };
                    exe->clo->proto(&ceux);
                    assert(CEU_ERROR==CEU_ERROR_NONE && "TODO: error in abort");
                    CEU_ERROR = error;

                    #if 0
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
                    #endif
                }
            }
        }
        #endif
    """
    val c_task = """ // TASK
        #if CEU >= 4
        CEU_Exe_Task* ceu_task_up (CEUX* X) {
            if (X->exe!=NULL && X->exe->type==CEU_VALUE_EXE_TASK) {
                return (CEU_Exe_Task*) X->exe;
            } else if (X->up == NULL) {
                return &CEU_GLOBAL_TASK;
            } else {
                return ceu_task_up(X->up);
            }
        }
        
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
        #if CEU >= 5
        #define ceu_bcast_dyn(a,b,c,d)      \
            assert(b == CEU_ACTION_RESUME); \
            (a->Any.type==CEU_VALUE_TASKS ? ceu_bcast_tasks(a,b,c,d) : ceu_bcast_task((CEU_Exe_Task*)a,c,d))
        #else
        #define ceu_bcast_dyn(a,b,c,d)      \
            assert(b == CEU_ACTION_RESUME); \
            ceu_bcast_task((CEU_Exe_Task*)a,c,d)
        #endif
            
        void ceu_bcast_task (CEU_Exe_Task* tsk, uint32_t now, CEU_Value* evt);

        CEU_Dyn* ceu_task_get (CEU_Dyn* cur) {
            while (cur!=NULL && CEU5(cur->Any.type!=CEU_VALUE_TASKS &&) cur->Exe_Task.status==CEU_EXE_STATUS_TERMINATED) {
                cur = CEU_LNKS(cur)->sd.nxt;
            }
            return cur;
        }
    
        void ceu_bcast_tasks (CEU_Dyn* up, CEU_ACTION act, uint32_t now, CEU_Value* evt) {
            ceu_gc_inc_dyn(up);
            CEU_Links* lnks = CEU_LNKS(up);
            CEU_Dyn* dn = ceu_task_get(lnks->dn.fst);
            while (dn != NULL) {
                ceu_gc_inc_dyn(dn);
                ceu_bcast_dyn(dn, act, now, evt);
                CEU_Dyn* nxt = ceu_task_get(CEU_LNKS(dn)->sd.nxt);
                ceu_gc_dec_dyn(dn); // TODO: could affect nxt?
                if (CEU_ERROR != CEU_ERROR_NONE) {
                    break;
                }
                dn = nxt;
            }
            ceu_gc_dec_dyn(up);
        }
        void ceu_bcast_task (CEU_Exe_Task* tsk, uint32_t now, CEU_Value* evt) {            
            // bcast order: DN -> ME
            //  - DN:  nested tasks
            //  - ME:  my yield point
            
            assert(tsk!=NULL && tsk->type==CEU_VALUE_EXE_TASK);            
            if (tsk->status == CEU_EXE_STATUS_TERMINATED) {
                return;
            }
            
            ceu_gc_inc_dyn((CEU_Dyn*) tsk);

            // DN
            if (tsk->status == CEU_EXE_STATUS_TOGGLED) {
                // do nothing
            } else {
                ceu_bcast_tasks((CEU_Dyn*) tsk, CEU_ACTION_RESUME, now, evt);
                //assert(ceu_acc.type != CEU_VALUE_ERROR);
            }

            // ME
            if (tsk->status != CEU_EXE_STATUS_YIELDED) {
                // do nothing
            } else if (tsk == &CEU_GLOBAL_TASK) {
                // do nothing
            } else if (CEU_ERROR != CEU_ERROR_NONE) {
                // even if error is caught, should not awake from past event
                CEUX ceux = {
                    tsk->clo,
                    {.exe_task = tsk},
                    CEU_ACTION_ERROR,
                    NULL,   // TODO: no access to up(ceux)
                    1,
                    NULL
                };
                tsk->clo->proto(&ceux);
            } else if (tsk->pc==0 || now>tsk->time) {
                ceu_gc_inc_val(*evt);
                CEUX ceux = {
                    tsk->clo,
                    {.exe_task = tsk},
                    CEU_ACTION_RESUME,
                    NULL,   // TODO: no access to up(ceux)
                    1,
                    evt
                };
                tsk->clo->proto(&ceux);
            }
            
            ceu_gc_dec_dyn((CEU_Dyn*) tsk);
        }

        void ceu_pro_broadcast_plic_ (CEUX* X) {
            assert(X->n == 2);
            //ceu_bstk_assert(bstk);

            assert(CEU_TIME < UINT32_MAX);
            CEU_TIME++;

            CEU_Value xin = X->args[0];
            CEU_Value evt = X->args[1];
            
            if (xin.type == CEU_VALUE_TAG) {
                if (xin.Tag == CEU_TAG_global) {
                    ceu_bcast_tasks((CEU_Dyn*) &CEU_GLOBAL_TASK, CEU_ACTION_RESUME, CEU_TIME, &evt);
                } else if (xin.Tag == CEU_TAG_task) {
                    ceu_bcast_tasks((CEU_Dyn*) ceu_task_up(X), CEU_ACTION_RESUME, CEU_TIME, &evt);
                } else {
                    CEU_ACC(CEU_ERROR_PTR("broadcast error : invalid target"));
                }
            } else {
                if (ceu_istask_val(xin)) {
                    if (xin.Dyn->Exe_Task.status == CEU_EXE_STATUS_TERMINATED) {
                        // nothing
                    } else {
                        ceu_bcast_task(&xin.Dyn->Exe_Task, CEU_TIME, &evt);
                    }
                } else {
                    CEU_ACC(CEU_ERROR_PTR("broadcast error : invalid target"));
                }
            }
            ceu_gc_dec_args(X->n, X->args);
        }
        
        CEU_Value ceu_broadcast_global (CEU_Value evt) {
            ceu_gc_inc_val(evt);
            CEU_Value glb = { CEU_VALUE_TAG, {.Tag=CEU_TAG_global} };
            CEU_Value args[] = { glb, evt };
            CEUX X = {
                NULL,
                {NULL}, CEU_ACTION_INVALID,
                NULL,
                2,
                args
            };
            ceu_pro_broadcast_plic_(&X);
            return ceu_acc;
        }

        #endif
    """
    val c_tasks = """
        #if CEU >= 5
        void ceu_pro_next_dash_tasks (CEUX* X) {
            assert(X->n==1 || X->n==2);
            CEU_Value tsks = X->args[0];
            if (tsks.type != CEU_VALUE_TASKS) {
                CEU_ACC(CEU_ERROR_PTR("next-tasks error : expected tasks"));
                goto _END_;
            }

            CEU_Value key = (X->n == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : X->args[1];
            CEU_Dyn* nxt = NULL;
            switch (key.type) {
                case CEU_VALUE_NIL:
                    nxt = tsks.Dyn->Tasks.lnks.dn.fst;
                    break;
                case CEU_VALUE_EXE_TASK:
                    nxt = key.Dyn->Exe_Task.lnks.sd.nxt;
                    break;
                default:
                    CEU_ACC(CEU_ERROR_PTR("next-tasks error : expected task"));
                    goto _END_;
            }
            if (nxt == NULL) {
                CEU_ACC((CEU_Value) { CEU_VALUE_NIL });
            } else {
                CEU_ACC(ceu_dyn_to_val(nxt));
            }
            
        _END_:
            ceu_gc_dec_args(X->n, X->args);
        }
        #endif
    """

    // GLOBALS
    fun c_globals (): String {
        val pres = GLOBALS.map {
            val id = it.idc()
            """
            CEU_Clo ceu_clo_$id = {
                CEU_VALUE_CLO_FUNC, 1, (CEU_Value) { CEU_VALUE_NIL },
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
    ${this.pres.unzip().let { (mems,protos) ->
        mems.joinToString("") + protos.joinToString("")
    }}
    
    int main (int ceu_argc, char** ceu_argv) {
        assert(CEU_TAG_nil == CEU_VALUE_NIL);
        
    #if CEU >= 2
        CEU_ERROR_STACK = ceu_create_vector();
        ceu_gc_inc_val(CEU_ERROR_STACK);
    #endif

    #if CEU >= 4
        CEUX _ceux_ = {
            NULL, {NULL}, CEU_ACTION_INVALID, NULL, 0, NULL
        };
        CEUX* ceux = &_ceux_;
    #endif
        
        do {
            ${this.code}
        } while (0);

    #if CEU >= 2
        // uncaught throw
        if (CEU_ERROR != CEU_ERROR_NONE) {
            CEU_Vector* vec = &CEU_ERROR_STACK.Dyn->Vector;
            for (int i=vec->its-1; i>=0; i--) {
                CEU_Value s = ceu_vector_get(vec, i);
                printf(" |  ");
                if (s.type == CEU_VALUE_POINTER) {
                    puts((char*) s.Pointer);
                } else {
                    assert(ceu_is_string(s));
                    puts(s.Dyn->Vector.buf);
                }
            }
            printf(" v  ");
            if (ceu_is_string(ceu_acc)) {
                ceu_print1(ceu_acc);
            } else if (ceu_acc.type == CEU_VALUE_POINTER) {
                printf("%s", (char*) ceu_acc.Pointer);
            } else {
                printf("error : ");
                ceu_print1(ceu_acc);
            }
            puts("");
        }
        ceu_gc_dec_val(CEU_ERROR_STACK);
    #endif
        
        CEU_ACC(((CEU_Value) { CEU_VALUE_NIL }));
        return 0;
    }
    """
    }

    return (
        h_includes() + h_defines() + h_enums() +
        h_value_dyn() + h_tags() + x_globals() +
        gc() + c_tags() + c_error + c_impls() +
        eq_neq_len() + creates() + tuple_vector_dict() +
        c_to() + print() + dumps() +
        (CEU>=3).cond { c_exes } +
        (CEU>=4).cond { c_task } +
        (CEU>=4).cond { c_bcast } +
        (CEU>=5).cond { c_tasks } +
        c_globals() + main()
    )
}
