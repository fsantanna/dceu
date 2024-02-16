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
    """
    }
    fun h_enums (): String {
        return """
    #if CEU >= 3
    typedef enum CEU_ACTION {
        CEU_ACTION_CALL,
        CEU_ACTION_ABORT,           // awake exe to finalize defers and release memory
    #if CEU >= 4
        CEU_ACTION_TOGGLE,          // restore time to CEU_TIME_MIN after toggle
        CEU_ACTION_ERROR ,          // awake task to catch error from nested task
    #endif
    } CEU_ACTION;
    #endif

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
    val h_block = """
    typedef struct CEU_Block {
        struct {
            struct CEU_Block* block;
            struct {
                struct CEU_Exe_Task* fst;   // bcast from here
                struct CEU_Exe_Task* lst;   // link from here
            } tasks;
        } dn;
    } CEU_Block;
    """
    fun h_value_dyn (): String {
        return """
    typedef struct CEU_Value {
        CEU_VALUE type;
        union {
            //void nil;
            CEU4(CEU_Block* Block;)
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

    struct CEUX;
    typedef int (*CEU_Proto) (struct CEUX* X);

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
        CEU_Value clo;                  \
        /*struct CEU_Frame frame;*/     \
        int pc;                         \
        struct CEUX* X;
        
    typedef struct CEU_Exe {
        _CEU_Exe_
    } CEU_Exe;
    #endif
    
    #if CEU >= 4
    typedef struct CEU_Exe_Task {
        _CEU_Exe_
        uint8_t time;
        CEU_Value pub;
        struct {
            CEU_Block* up;      // bcast task termination
            CEU_Block* dn;      // bcast nested tasks
        } blocks;
        struct {
            struct CEU_Exe_Task* prv;
            struct CEU_Exe_Task* nxt;
        } tasks;
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
    """
    }
    fun h_protos (): String {
        return """
    int ceu_type_f (CEUX* X);
    int ceu_as_bool (CEU_Value v);
    CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn);

    int ceu_tags_f (CEUX* X);
    char* ceu_tag_to_string (int tag);
    int ceu_type_to_size (int type);

    void ceu_gc_inc (CEU_Value v);

    CEU_Value ceu_create_tuple   (int n);
    CEU_Value ceu_create_vector  (void);
    CEU_Value ceu_create_dict    (void);
    CEU_Value ceu_create_clo     (CEU_Proto proto, int args, int locs, int upvs);
    #if CEU >= 4
    CEU_Value ceu_create_exe_task (CEU_Value clo, CEU_Block* block_up);
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
    int ceu_pointer_dash_to_dash_string_f (CEUX* X);
    #endif
    #if CEU >= 3
    int ceu_isexe_val (CEU_Value val);
    #endif
    #if CEU >= 4
    int ceu_bcast_task (CEUX* X1, uint8_t now, CEU_Exe_Task* task2);
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
            //printf("    next  = %p\n", v.Dyn->Any.hld.next);
            printf("    ----\n");
            switch (v.type) {
        #if CEU >= 4
                case CEU_VALUE_EXE_TASK:
                    printf("    status = %d\n", v.Dyn->Exe_Task.status);
                    printf("    pc     = %d\n", v.Dyn->Exe_Task.pc);
                    printf("    pub    = %d\n", v.Dyn->Exe_Task.pub.type);
                    printf("    block  = [%p,%p]\n", v.Dyn->Exe_Task.blocks.up, v.Dyn->Exe_Task.blocks.dn);
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
    val c_error = """
    #define CEU_ERROR_ASR(cmd,v,pre) ({     \
        if (v.type == CEU_VALUE_ERROR) {    \
            ceu_error_e(X->S,v);            \
            CEU_ERROR_CHK(cmd,pre);         \
        };                                  \
        v;                                  \
    })

    #if CEU <= 1
    #define CEU_ERROR_CHK(cmd,pre) {                    \
        if (ceux_top(X->S)>0 && ceux_peek(X->S,XX(-1)).type==CEU_VALUE_ERROR) {     \
            CEU_Value msg = ceux_peek(X->S, XX(-2));    \
            assert(msg.type == CEU_VALUE_POINTER);      \
            fprintf(stderr, " |  %s\n v  error : %s\n", pre, (char*) msg.Pointer);  \
            ceux_base(X->S, 0);                         \
            exit(0);                                    \
        }                                               \
    }
    #else
    #define CEU_ERROR_CHK(cmd,pre)      \
        if (ceu_error_chk(X->S, pre)) { \
            cmd;                        \
        }
    int ceu_error_chk (CEU_Stack* S, char* pre) {
        CEU_Value err = ceux_peek(S, SS(-1));
        if (ceux_top(S)==0 || err.type!=CEU_VALUE_ERROR) {
            return 0;
        } else {
            if (pre != NULL) {      // blocks check but do not add a message
                // [...,n,pay,err]
                CEU_Value n = ceux_peek(S, SS(-3));
                assert(n.type == CEU_VALUE_NUMBER);
                ceux_repl(S, SS(-3), (CEU_Value) { CEU_VALUE_NUMBER, {.Number=n.Number+1} });
                ceux_shift(S, SS(-3));
                ceux_repl(S, SS(-4), (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=pre} });
                // [...,pre,n+1,pay,err]
            }
            return 1;
        }
    }
    #endif

    int ceu_error_e (CEU_Stack* S, CEU_Value e) {
        assert(e.type == CEU_VALUE_ERROR);
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_NUMBER, {.Number=0} });
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_POINTER, {.Pointer=e.Error} });
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_ERROR, {.Error=NULL} });
        return 3;
    }
    int ceu_error_s (CEU_Stack* S, char* s) {
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

    int ceu_error_f (CEUX* X) {
        assert(X->args == 1);
    #if CEU < 2
        CEU_Value arg = ceux_peek(X->S, ceux_arg(X,0));
        assert(arg.type == CEU_VALUE_TAG);
        return ceu_error_s(X->S, ceu_tag_to_string(arg.Tag));
    #else
        return ceu_error_v(X->S, ceux_peek(X->S, ceux_arg(X,0)));
    #endif
    }        
    """

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

    void ceu_gc_free (CEU_Dyn* dyn);
    
    void ceu_gc_dec (CEU_Value v) {
        if (v.type < CEU_VALUE_DYNAMIC)
            return;
        assert(v.Dyn->Any.refs > 0);
        v.Dyn->Any.refs--;
        if (v.Dyn->Any.refs == 0) {
            ceu_gc_free(v.Dyn);
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
                for (int i=0; i<dyn->Clo.upvs.its; i++) {
                    ceu_gc_dec(dyn->Clo.upvs.buf[i]);
                }
                free(dyn->Clo.upvs.buf);
                break;
            case CEU_VALUE_TUPLE:       // buf w/ dyn
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
                free(dyn->Vector.buf);
                break;
            case CEU_VALUE_DICT:
                for (int i=0; i<dyn->Dict.max; i++) {
                    ceu_gc_dec((*dyn->Dict.buf)[i][0]);
                    ceu_gc_dec((*dyn->Dict.buf)[i][1]);
                }
                free(dyn->Dict.buf);
                break;
#if CEU >= 3
            case CEU_VALUE_EXE_CORO: {
#if CEU >= 4
            case CEU_VALUE_EXE_TASK:
#endif
                if (dyn->Exe.status < CEU_EXE_STATUS_TERMINATED) {
                    // TODO - fake S/X - should propagate up to calling stack
                    CEU_Stack S = { 0, {} };
                    CEUX _X = { &S, 0, 0, CEU_ACTION_ABORT, {.exe=NULL} };
                    CEUX* X = &_X;
                    ceux_push(&S, 1, ceu_dyn_to_val(dyn));
                    dyn->Any.refs++;    // currently 0->1: needs ->2 to prevent double gc
                    // S: [co]
                    int ret = ceux_resume(X, 0, 0);
                    dyn->Any.refs--;
                    if (ret != 0) {
                        int iserr = ceux_peek(&S,XX(-1)).type == CEU_VALUE_ERROR;
                        assert(iserr && "TODO: abort should not return");
                        assert(0 && "TODO: error in ceu_exe_kill");
                    }
                }
                ceux_base(dyn->Exe.X->S, 0);
                ceu_gc_dec(dyn->Exe.clo);
                free(dyn->Exe.X->S);
                free(dyn->Exe.X);
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
    #define CEU_STACK_MAX $STACK
    typedef struct CEU_Stack {
        int n;
        CEU_Value buf[CEU_STACK_MAX];
    } CEU_Stack;
    
    typedef struct CEUX {
        CEU_Stack* S;
        int base;   // index above args
        int args;   // number of args
    #if CEU >= 3
        CEU_ACTION action;
        union {
            struct CEU_Exe* exe;
    #if CEU >= 3
            struct CEU_Exe_Task* exe_task;
    #endif
        };
    #endif
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
    #define ceux_arg(X,i) (X->base - X->args + i)
    #define XX(v)  ({ assert(v<=0); X->S->n+v; })
    #define XX1(v) ({ assert(v<=0); X1->S->n+v; })
    #define XX2(v) ({ assert(v<=0); X2->S->n+v; })
    #define SS(v)  ({ assert(v<=0); S->n+v;    })
    int ceux_top (CEU_Stack* S);
    void ceux_base (CEU_Stack* S, int base);
    CEU_Value ceux_pop (CEU_Stack* S, int dec);
    void ceux_push (CEU_Stack* S, int inc, CEU_Value v);
    CEU_Value ceux_peek (CEU_Stack* S, int i);
    void ceux_repl (CEU_Stack* S, int i, CEU_Value v);
    void ceux_shift (CEU_Stack* S, int I);
    void ceux_drop (CEU_Stack* S, int n);        
    int ceux_resume (CEUX* X1, int inp, int out);
    """
    val c_ceux = """
    void ceux_dump (CEU_Stack* S, int n) {
        printf(">>> DUMP | n=%d | S=%p\n", S->n, S);
        for (int i=n; i<S->n; i++) {
            printf(">>> [%d]: [%d] ", i, ceux_peek(S,i).type);
            ceu_print1(ceux_peek(S,i));
            puts("");
        }
    }
    int ceux_top (CEU_Stack* S) {
        return S->n;
    }
    void ceux_push (CEU_Stack* S, int inc, CEU_Value v) {
        assert(S->n<CEU_STACK_MAX && "TODO: stack error");
        if (inc) {
            ceu_gc_inc(v);
        }
        S->buf[S->n++] = v;
    }
    CEU_Value ceux_pop (CEU_Stack* S, int dec) {
        assert(S->n>0 && "TODO: stack error");
        CEU_Value v = S->buf[--S->n];
        if (dec) {
            ceu_gc_dec(v);
        }
        return v;
    }
    CEU_Value ceux_peek (CEU_Stack* S, int i) {
        assert(i>=0 && i<S->n && "TODO: stack error");
        return S->buf[i];
    }
    void ceux_drop (CEU_Stack* S, int n) {
        assert(n<=S->n && "BUG: index out of range");
        for (int i=0; i<n; i++) {
            ceu_gc_dec(S->buf[--S->n]);
        }
    }
    void ceux_base (CEU_Stack* S, int base) {
        assert(base>=0 && base<=S->n && "TODO: stack error");
        for (int i=S->n; i>base; i--) {
            ceu_gc_dec(S->buf[--S->n]);
        }
    }
    void ceux_repl (CEU_Stack* S, int i, CEU_Value v) {
        assert(i>=0 && i<S->n && "TODO: stack error");
        ceu_gc_inc(v);
        ceu_gc_dec(S->buf[i]);
        S->buf[i] = v;
    }
    void ceux_dup (CEU_Stack* S, int i) {
        ceux_push(S, 1, ceux_peek(S,i));
    }
    void ceux_copy (CEU_Stack* S, int i, int j) {
        assert(i>=0 && i<S->n && "TODO: stack error");
        assert(j>=0 && j<S->n && "TODO: stack error");
        assert(i!=j && "TODO: invalid move");
        ceu_gc_dec(S->buf[i]);
        S->buf[i] = S->buf[j];
        ceu_gc_inc(S->buf[i]);
    }
    void ceux_move (CEU_Stack* S, int i, int j) {
        assert(i>=0 && i<S->n && "TODO: stack error");
        assert(j>=0 && j<S->n && "TODO: stack error");
        if (i == j) {
            // nothing to change
        } else {
            ceu_gc_dec(S->buf[i]);
            S->buf[i] = S->buf[j];
            S->buf[j] = (CEU_Value) { CEU_VALUE_NIL };
        }
    }

    void ceux_shift (CEU_Stack* S, int i) {
        // [...,x,...]
        //      ^ i
        assert(i>=0 && i<=S->n && "TODO: stack error");
        for (int j=S->n; j>i; j--) {
            S->buf[j] = S->buf[j-1];
        }
        S->buf[i] = (CEU_Value) { CEU_VALUE_NIL };
        S->n++;
        // [...,nil,x,...]
        //       ^ i
    }
    
    // ceux_block_*
    //  - needs to clear locals on enter and leave
    //  - enter: initialize all vars to nil (prevents garbage)
    //  - leave: gc locals
    
    void ceux_block_enter (CEU_Stack* S, int base, int n) {
        // clear locals
        // TODO: use memset=0
        for (int i=0; i<n; i++) {
            ceux_repl(S, base+i, (CEU_Value) { CEU_VALUE_NIL });
        }
    #if CEU >= 4
        CEU_Block* blk = malloc(sizeof(CEU_Block));
        *blk = (CEU_Block) { {NULL, {NULL,NULL}} };
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_BLOCK, {.Block=blk} });
    #else
        ceux_push(S, 1, (CEU_Value) { CEU_VALUE_BLOCK });
    #endif
    }
    
    #if CEU >= 4
    CEU_Block* ceux_block (CEU_Stack* S, int dir) {
        for (
            int i = (dir == 1) ? 0 : S->n-1;
            (dir == 1) ? i<S->n : i>=0;
            i = i + ((dir == 1) ? 1 : -1)
        ) {
            CEU_Value v = ceux_peek(S, i);
            if (v.type == CEU_VALUE_BLOCK) {
                return v.Block;
            }
        }
        assert(0 && "bug found: no block found");
    }
    #endif
    
    void ceux_block_leave (CEU_Stack* S, int base, int n, int out) {
        // clear locals
        // TODO: use memset=0
        for (int i=0; i<n; i++) {
            ceux_repl(S, base+i, (CEU_Value) { CEU_VALUE_NIL });
        }

        int I = -1;
        for (int i=S->n-1; i>=0; i--) {
            CEU_Value blk = ceux_peek(S,i);
            if (blk.type == CEU_VALUE_BLOCK) {
    #if CEU >= 4
                for (
                    CEU_Exe_Task* tsk = blk.Block->dn.tasks.fst;
                    tsk != NULL;
                    tsk = tsk->tasks.nxt
                ) {
                    tsk->blocks.up = NULL;
                }
                free(blk.Block);
    #endif
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
        if (ceux_peek(S,SS(-1)).type == CEU_VALUE_ERROR) {
            CEU_Value n = ceux_peek(S,SS(-3));
            assert(n.type == CEU_VALUE_NUMBER);
            out = n.Number + 1 + 1 + 1;
        }
    #endif

        for (int i=0; i<out; i++) {
            ceux_move(S, I+i, SS(-out+i));
        }
        ceux_base(S, I + out);
    }
    
    int ceux_call_pre (CEU_Stack* S, CEU_Clo* clo, int* inp) {
        // fill missing args with nils
        {
            int N = clo->args - *inp;
            for (int i=0; i<N; i++) {
                ceux_push(S, 1, (CEU_Value) { CEU_VALUE_NIL });
                (*inp)++;
            }
        }
        
        int base = S->n;

        // [clo,args,?]
        //           ^ base

        // place upvs+locs
        {
            for (int i=0; i<clo->upvs.its; i++) {
                ceux_push(S, 1, clo->upvs.buf[i]);
            }
            for (int i=0; i<clo->locs; i++) {
                ceux_push(S, 1, (CEU_Value) { CEU_VALUE_NIL });
            }
        }
        // [clo,args,upvs,locs]
        //           ^ base

        return base;
    }
    
    int ceux_call_pos (CEU_Stack* S, int ret, int* out) {
        // in case of error, out must be readjusted to the error stack:
        // [clo,args,upvs,locs,...,n,pay,err]
        //  - ... - error messages
        //  - n   - number of error messages
        //  - pay - error payload
        //  - err - error value
        if (ret>0 && ceux_peek(S,SS(-1)).type == CEU_VALUE_ERROR) {
            CEU_Value n = ceux_peek(S,SS(-3));
            assert(n.type == CEU_VALUE_NUMBER);
            *out = n.Number + 1 + 1 + 1;
            return 1;
        }
         
        if (*out == CEU_MULTI) {     // any rets is ok
            *out = ret;
        } else if (ret < *out) {     // less rets than requested
           // fill rets up to outs
            for (int i=0; i<*out-ret; i++) {
                ceux_push(S, 1, (CEU_Value) { CEU_VALUE_NIL });
            }
        } else if (ret > *out) {     // more rets than requested
            for (int i=*out; i<ret; i++) {
                ceux_pop(S, 1);
            }
        } else { // ret == out      // exact rets requested
            // ok
        }
        return 0;
    }
    
    int ceux_call (CEUX* X1, int inp, int out) {
        // [clo,inps]
        CEU_Value clo = ceux_peek(X1->S, XX1(-inp-1));
        if (CEU3(X1->exe==NULL &&) clo.type!=CEU_VALUE_CLO_FUNC) {
            return ceu_error_s(X1->S, "call error : expected function");
        }

        int base = ceux_call_pre(X1->S, &clo.Dyn->Clo, &inp);

        // [clo,args,upvs,locs]
        //           ^ base

        CEUX X2 = *X1; {
            X2.base = base;
            X2.args = inp;
        }
        int ret = clo.Dyn->Clo.proto(&X2);
        
        // [clo,args,upvs,locs,rets]
        //           ^ base
        
        ceux_call_pos(X1->S, ret, &out);        
        
        // [clo,args,upvs,locs,out]
        //           ^ base
        
        // move rets to begin, replacing [clo,args,upvs,locs]
        {
            for (int i=0; i<out; i++) {
                ceux_move(X1->S, base-inp-1+i, X1->S->n-out+i);
            }

            // [outs,x,x,x,x]
            //           ^ base
            ceux_base(X1->S, base-inp-1+out);
        }
        // [outs]
        //      ^ base
        
        return out;
    }
    
#if CEU >= 3
    int ceux_resume (CEUX* X1, int inp, int out) {
        // X1: [exe,inps]
        assert(inp<=1 && "TODO: varargs resume");

        CEU_Value exe = ceux_peek(X1->S, XX1(-inp-1));
        if (!ceu_isexe_val(exe) || exe.Dyn->Exe.status!=CEU_EXE_STATUS_YIELDED) {
            return ceu_error_s(X1->S, "resume error : expected yielded coro");
        }
        assert(exe.Dyn->Exe.clo.type==CEU_VALUE_CLO_CORO CEU4(|| exe.Dyn->Exe_Task.clo.type==CEU_VALUE_CLO_TASK));
        CEU_Clo* clo = &exe.Dyn->Exe.clo.Dyn->Clo;
        
        // X1: [exe,inps]
        // X2: [...]
        CEUX* X2 = exe.Dyn->Exe.X;                                        
        for (int i=0; i<inp; i++) {                                                 
            ceux_push(X2->S, 1, ceux_peek(X1->S,XX1(-i-1)));                               
        }
        ceu_gc_inc(exe);
        ceux_base(X1->S, XX1(-inp-1));
        // X1: []
        // X2: [...,inps]
        
        // first resume: place upvs+locs
        if (exe.Dyn->Exe.pc == 0) {
            X2->base = ceux_call_pre(X2->S, clo, &inp);
            // X2: [args,upvs,locs]
            //           ^ base
        } else {
            //X2->base = <already set>
            // X2: [args,upvs,locs,...,inps]
            //           ^ base
        }
        X2->args = inp;
        X2->action = X1->action;

        int ret = clo->proto(X2);
        
        // X2: [args,upvs,lovs,...,rets]
        
        int err = ceux_call_pos(X2->S, ret, &out);        
        
        // X1: [exe]
        // X2: [args,upvs,lovs,...,rets]

        for (int i=0; i<out; i++) {
            ceux_push(X1->S, 1, ceux_peek(X2->S,XX2(-out)+i));                               
        }
        if (err) {
            ceux_base(X2->S, 0);
        } else {
            ceux_base(X2->S, XX2(-out));
        }
        // X1: [outs]
        // X2: []
        
        ceu_gc_dec(exe);
        return out;
    }
#endif

#if CEU >= 4
    int ceux_spawn (CEUX* X1, uint8_t now, int inp) {
        // X1: [clo,inps]
        assert(inp<=1 && "TODO: varargs spawn");
        
        CEU_Value clo = ceux_peek(X1->S, XX1(-inp-1));
        if (clo.type != CEU_VALUE_CLO_TASK) {
            return ceu_error_s(X1->S, "spawn error : expected task");
        }

        CEU_Value exe = ceu_create_exe_task(clo, ceux_block(X1->S,-1));
        if (exe.type == CEU_VALUE_ERROR) {
            return ceu_error_e(X1->S, exe);
        }        
        assert(exe.Dyn->Exe_Task.clo.type == CEU_VALUE_CLO_TASK);
        
        ceux_repl(X1->S, XX1(-inp-1), exe);
        // X1: [exe,inps]
        
        ceu_gc_inc(exe);    // keep exe alive to return it  
        int ret = ceux_resume(X1, inp, 0);
        // X1: []
        
        if (ret > 0) {
            // error
        } else {
            ret = 1;
            ceux_push(X1->S, 1, exe);        // returns exe to caller
            // X1: [exe]
        }
        ceu_gc_dec(exe);    // dec after push above
        
        return ret;
    }
#endif
    """

    // IMPLS
    fun c_impls (): String {
        return """
    CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn) {
        return (CEU_Value) { dyn->Any.type, {.Dyn=dyn} };
    }
    
    int ceu_dump_f (CEUX* X) {
        assert(X->args == 1);
    #ifdef CEU_DEBUG
        ceu_dump_value(ceux_peek(X->S, ceux_arg(X,0)));
        return 0;
    #else
        return ceu_error_s(X->S, "debug is off");
    #endif
    }

    int ceu_as_bool (CEU_Value v) {
        return !(v.type==CEU_VALUE_NIL || (v.type==CEU_VALUE_BOOL && !v.Bool));
    }
    int ceu_type_f (CEUX* X) {
        assert(X->args==1 && "bug found");
        int type = ceux_peek(X->S, ceux_arg(X,0)).type;
        ceux_push(X->S, 1, (CEU_Value) { CEU_VALUE_TAG, {.Tag=type} });
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
    int ceu_sup_question__f (CEUX* X) {
        assert(X->args >= 2);
        CEU_Value sup = ceux_peek(X->S, ceux_arg(X,0));
        CEU_Value sub = ceux_peek(X->S, ceux_arg(X,1));
        CEU_Value ret = _ceu_sup_(sup, sub);
        ceux_push(X->S, 1, ret);
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
        
    int ceu_tags_f (CEUX* X) {
        assert(X->args >= 1);
        CEU_Value dyn = ceux_peek(X->S, ceux_arg(X,0));
        assert(dyn.type > CEU_VALUE_DYNAMIC);
        CEU_Value tag; // = (CEU_Value) { CEU_VALUE_NIL };
        if (X->args >= 2) {
            tag = ceux_peek(X->S, ceux_arg(X,1));
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
        
        switch (X->args) {
            case 1: {   // all tags
                CEU_Value ret = _ceu_tags_all_(dyn);
                ceux_push(X->S, 1, ret);
                break;
            }
            case 2: {   // check tag
                CEU_Value ret = f_chk();
                ceux_push(X->S, 1, ret);
                break;
            }
            default: {   // add/rem tag
                CEU_Value bool = ceux_peek(X->S, ceux_arg(X,2));
                assert(bool.type == CEU_VALUE_BOOL);
                f_set(bool.Bool);
                ceux_dup(X->S, ceux_arg(X,0));  // keep dyn
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
    int ceu_string_dash_to_dash_tag_f (CEUX* X) {
        assert(X->args == 1);
        CEU_Value str = ceux_peek(X->S, ceux_arg(X,0));
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
        ceux_push(X->S, 1, ret);
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
    int ceu_pointer_dash_to_dash_string_f (CEUX* X) {
        assert(X->args == 1);
        CEU_Value ptr = ceux_peek(X->S, ceux_arg(X,0));
        assert(ptr.type == CEU_VALUE_POINTER);
        ceux_push(X->S, 1, ceu_pointer_dash_to_dash_string(ptr.Pointer));
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

    int ceu_next_dash_dict_f (CEUX* X) {
        assert(X->args==1 || X->args==2);
        CEU_Value dict = ceux_peek(X->S, ceux_arg(X,0));
        CEU_Value ret;
        if (dict.type != CEU_VALUE_DICT) {
            return ceu_error_s(X->S, "next-dict error : expected dict");
        } else {
            CEU_Value key = (X->args == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : ceux_peek(X->S, ceux_arg(X,1));
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
        ceux_push(X->S, 1, ret);
        return 1;
    }
    
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
        return (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ret} };
    }
    
    int ceu_tuple_f (CEUX* X) {
        assert(X->args == 1);
        CEU_Value arg = ceux_peek(X->S, ceux_arg(X,0));
        assert(arg.type == CEU_VALUE_NUMBER);
        CEU_Value ret = ceu_create_tuple(arg.Number);
        ceux_push(X->S, 1, ret);
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
        return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
    }

    CEU_Value ceu_create_clo (CEU_Proto proto, int args, int locs, int upvs) {
        return _ceu_create_clo_(sizeof(CEU_Clo), CEU_VALUE_CLO_FUNC, proto, args, locs, upvs);
    }

    #if CEU >= 3
    CEU_Value ceu_create_clo_exe (int type, CEU_Proto proto, int args, int locs, int upvs) {
        CEU_Value clo = _ceu_create_clo_(sizeof(CEU_Clo_Exe), type, proto, args, locs, upvs);
        clo.Dyn->Clo_Exe.mem_n = 0;
        return clo;
    }
    #endif

    #if CEU >= 3
    CEU_Value ceu_create_exe (int type, int sz, CEU_Value clo) {
        ceu_debug_add(type);
        assert(clo.type==CEU_VALUE_CLO_CORO CEU4(|| clo.type==CEU_VALUE_CLO_TASK));
        ceu_gc_inc(clo);
        
        CEU_Exe* ret = malloc(sz);
        assert(ret != NULL);
        CEUX* X = malloc(sizeof(CEUX));
        CEU_Stack* S = malloc(sizeof(CEU_Stack));
        assert(X!=NULL && S!=NULL);
        S->n = 0;
        //S->buf = <dynamic>    // TODO
        X->S = S;
        //X->base = <first resume>
        //X->args = <first resume>
        //X->action = <resume>
        X->exe = ret;

        *ret = (CEU_Exe) {
            type, 0, NULL,
            CEU_EXE_STATUS_YIELDED, clo, 0, X
        };
        
        //ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA dyns));
        return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
    }
    #endif

    #if CEU >= 4
    CEU_Value ceu_create_exe_task (CEU_Value clo, CEU_Block* block_up) {
        if (clo.type != CEU_VALUE_CLO_TASK) {
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error="spawn error : expected task"} };
        }
        CEU_Value ret = ceu_create_exe(CEU_VALUE_EXE_TASK, sizeof(CEU_Exe_Task), clo);
        CEU_Exe_Task* dyn = &ret.Dyn->Exe_Task;
        dyn->time = CEU_TIME_MAX;
        dyn->blocks.up = block_up;
        dyn->blocks.dn = NULL;
        dyn->tasks.prv = NULL;
        dyn->tasks.nxt = NULL;
        dyn->pub = (CEU_Value) { CEU_VALUE_NIL };

        ceu_gc_inc(ret);    // block holds a strong reference
        if (block_up->dn.tasks.fst == NULL) {
            block_up->dn.tasks.fst = dyn;
        } else {
            assert(block_up->dn.tasks.lst != NULL);
            block_up->dn.tasks.lst->tasks.nxt = dyn;
            dyn->tasks.prv = block_up->dn.tasks.lst;
        }
        block_up->dn.tasks.lst = &ret.Dyn->Exe_Task;

        return ret;
    }
    #endif
    
    #if CEU >= 5
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
            ceu_gc_free(tup.Dyn);
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
    int ceu_print_f (CEUX* X) {
        for (int i=0; i<X->args; i++) {
            if (i > 0) {
                printf("\t");
            }
            ceu_print1(ceux_peek(X->S, ceux_arg(X,i)));
        }
        return 0;
    }
    int ceu_println_f (CEUX* X) {
        assert(0 == ceu_print_f(X));
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
    int ceu_equals_equals_f (CEUX* X) {
        assert(X->args == 2);
        CEU_Value ret = _ceu_equals_equals_(ceux_peek(X->S, ceux_arg(X,0)), ceux_peek(X->S, ceux_arg(X,1)));
        ceux_push(X->S, 1, ret);
        return 1;
    }
    int ceu_slash_equals_f (CEUX* X) {
        ceu_equals_equals_f(X);
        CEU_Value ret = ceux_pop(X->S, 0);
        assert(ret.type == CEU_VALUE_BOOL);
        ret.Bool = !ret.Bool;
        ceux_push(X->S, 1, ret);
        return 1;
    }
    
    int ceu_hash_f (CEUX* X) {
        assert(X->args == 1);
        CEU_Value v = ceux_peek(X->S, ceux_arg(X,0));
        CEU_Value ret;
        if (v.type == CEU_VALUE_VECTOR) {
            ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=v.Dyn->Vector.its} };
        } else if (v.type == CEU_VALUE_TUPLE) {
            ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=v.Dyn->Tuple.its} };
        } else {
            return ceu_error_s(X->S, "length error : not a vector");
        }
        ceux_push(X->S, 1, ret);
        return 1;
    }
    """
    }

    val c_exes = """
        int ceu_isexe_val (CEU_Value val) {
            return (val.type==CEU_VALUE_EXE_CORO CEU4(|| ceu_istask_val(val)));
        }
        int ceu_coroutine_f (CEUX* X) {
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

        int ceu_status_f (CEUX* X) {
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
        
        void ceu_exe_term (CEU_Exe* exe) {
            exe->status = CEU_EXE_STATUS_TERMINATED;
    #if CEU >= 4
            if (exe->type == CEU_VALUE_EXE_TASK) {
                CEU_Exe_Task* tsk = (CEU_Exe_Task*) exe;
                if (tsk->tasks.prv != NULL) {
                    tsk->tasks.prv->tasks.nxt = tsk->tasks.nxt;
                }
                if (tsk->tasks.nxt != NULL) {
                    tsk->tasks.nxt->tasks.prv = tsk->tasks.prv;
                }
                if (tsk->blocks.up != NULL) {
                    if (tsk->blocks.up->dn.tasks.fst == tsk) {
                        tsk->blocks.up->dn.tasks.fst = tsk->tasks.nxt;
                    }
                    if (tsk->blocks.up->dn.tasks.lst == tsk) {
                        tsk->blocks.up->dn.tasks.lst = tsk->tasks.prv;
                    }
                    // block held a strong reference
                    ceu_gc_dec(ceu_dyn_to_val((CEU_Dyn*)exe));
                }
            }
    #endif
        }
    """
    val c_task = """ // TASK
        int ceu_istask_dyn (CEU_Dyn* dyn) {
            return dyn->Any.type==CEU_VALUE_EXE_TASK CEU5(|| dyn->Any.type==CEU_VALUE_EXE_TASK_IN);
        }
        int ceu_istask_val (CEU_Value val) {
            return (val.type>CEU_VALUE_DYNAMIC) && ceu_istask_dyn(val.Dyn);
        }
    """
    val c_bcast = """
        int ceu_bcast_blocks (CEUX* X, uint8_t now, CEU_Block* blk);
        int ceu_bcast_tasks (CEUX* X1, uint8_t now, CEU_Exe_Task* task2);
        
        int ceu_bcast_task (CEUX* X1, uint8_t now, CEU_Exe_Task* task2) {
            if (task2->status == CEU_EXE_STATUS_TERMINATED) {
                return 0;
            } else if (X1->action == CEU_ACTION_ABORT) {
                task2->X->action = CEU_ACTION_ABORT;
                return task2->clo.Dyn->Clo.proto(task2->X);
            } else if (task2->status == CEU_EXE_STATUS_TOGGLED) {
                return 0;
            }

            int ret = 0;    // !=0 means error in nested bcast
            
            if (task2->status==CEU_EXE_STATUS_RESUMED || task2->pc!=0) {    // not initial spawn
                ret = ceu_bcast_blocks(task2->X, now, task2->blocks.dn);
            }

            #define ceu_time_lt(tsk,now) \
                ((CEU_TIME_MAX>=CEU_TIME_MIN || (tsk<CEU_TIME_MAX && now<CEU_TIME_MAX) || (tsk>CEU_TIME_MIN && now>CEU_TIME_MIN)) ? \
                    (tsk < now) : (tsk > now))

            if (task2->status == CEU_EXE_STATUS_YIELDED) {
                if (ret != 0) {
                    // catch error from blocks above
                    assert(ceux_peek(X1->S,XX1(-1)).type == CEU_VALUE_ERROR);
                    task2->X->action = CEU_ACTION_ERROR;
                    ret = task2->clo.Dyn->Clo.proto(task2->X);
                } else if (X1->action!=CEU_ACTION_TOGGLE && (task2->pc==0 || ceu_time_lt(task2->time,now))) {
                    task2->X->action = X1->action;
                    ret = task2->clo.Dyn->Clo.proto(task2->X);
                } else {
                    task2->time = CEU_TIME_MIN;
                }
            }

            // do not bcast aborted task (only terminated) b/c
            // it would awake parents that actually need to
            // respond/catch the error (thus not awake)
            if (ret==0 && task2->status==CEU_EXE_STATUS_TERMINATED) {
                ceux_push(X1->S, 1, ceu_dyn_to_val((CEU_Dyn*)task2));

                assert(CEU_TIME_N < 255);
                CEU_TIME_N++;
                uint8_t now = ++CEU_TIME_MAX;

                CEU_Exe_Task* up_task = NULL; //ceu_task_up_task(task2);
                assert(0 && "TODO");
                if (up_task != NULL) {
                    // enclosing coro of enclosing block
                    ret = ceu_bcast_task(X1, now, up_task);
                } else {
                    // enclosing block
                    ret = ceu_bcast_blocks(X1, now, task2->blocks.up);
                }
                
                CEU_TIME_N--;
                if (CEU_TIME_N == 0) {
                    CEU_TIME_MIN = now;
                }
                
                ceux_pop(X1->S, 1);
                
                /* TODO: stack trace for error on task termination
                do {
                    CEU_ERROR_ASR(BUPC, ceux_peek(XX(-1)), "FILE : (lin LIN, col COL) : ERR");
                } while (0);
                */
            }

            return ret;
        }

        int ceu_bcast_tasks (CEUX* X1, uint8_t now, CEU_Exe_Task* task2) {
            if (task2 == NULL) {
                return 0;
            }
            int ret = 0;    // !=0 is error
            if (task2->status != CEU_EXE_STATUS_TERMINATED) {
                task2->X->action = X1->action;
                ret = ceu_bcast_task(X1, now, task2);
            }
            if (ret == 0) {
                ret = ceu_bcast_tasks(X1, now, task2->tasks.nxt);
            }
            return ret;
        }

        int ceu_bcast_blocks (CEUX* X, uint8_t now, CEU_Block* cur) {
            if (cur == NULL) {
                return 0;
            }
            int ret = ceu_bcast_tasks(X, now, cur->dn.tasks.fst);
            if (ret != 0) {
                return ret;
            }
            return ceu_bcast_blocks(X, now, cur->dn.block);
        }

        int ceu_broadcast_plic__f (CEUX* X) {
            assert(X->args == 2);
            //ceu_bstk_assert(bstk);

            assert(CEU_TIME_N < 255);
            CEU_TIME_N++;
            uint8_t now = ++CEU_TIME_MAX;

            CEU_Value evt = ceux_peek(X->S, ceux_arg(X,0));
            CEU_Value xin = ceux_peek(X->S, ceux_arg(X,1));
            int ret;
            if (xin.type == CEU_VALUE_TAG) {
                if (xin.Tag == CEU_TAG_global) {
                    ret = ceu_bcast_blocks(X, now, ceux_block(X->S,1));
                } else if (xin.Tag == CEU_TAG_task) {
                    if (X->exe_task == NULL) {
                        ret = ceu_bcast_blocks(X, now, ceux_block(X->S,1));
                    } else {
                        ret = ceu_bcast_task(X, now, X->exe_task);
                    }
                } else {
                    ret = ceu_error_s(X->S, "broadcast error : invalid target");
                }
            } else {
                if (ceu_istask_val(xin)) {
                    ret = ceu_bcast_task(X, now, &xin.Dyn->Exe_Task);
                } else {
                    ret = ceu_error_s(X->S, "broadcast error : invalid target");
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
    """

    // MAIN
    fun main (): String {
        return """
    ${this.pres.joinToString("")}
    
    int main (int ceu_argc, char** ceu_argv) {
        assert(CEU_TAG_nil == CEU_VALUE_NIL);
        
    #if 0
        // ... args ...
        {
            CEU_Value xxx = ceu_create_tuple(ceu_argc);
            for (int i=0; i<ceu_argc; i++) {
                CEU_Value vec = ceu_vector_from_c_string(ceu_argv[i]);
                ceu_tuple_set(&xxx.Dyn->Tuple, i, vec);
            }
            ceux_push(X->S, 1, xxx);
        }
    #endif
    
        CEU_Stack S = { 0, {} };
        CEUX _X = { &S, 0, 0 CEU3(COMMA CEU_ACTION_CALL COMMA {.exe=NULL}) };
        CEUX* X = &_X;
        
        ${do_while(this.code)}

        // uncaught throw
    #if CEU >= 2
        if (ceux_peek(X->S,XX(-1)).type == CEU_VALUE_ERROR) {
            // [...,n,pay,err]
            CEU_Value n = ceux_peek(X->S, XX(-3));
            assert(n.type == CEU_VALUE_NUMBER);
            // ignore i=0 (main call)
            for (int i=1; i<n.Number; i++) {
                printf(" |  ");
                CEU_Value pre = ceux_peek(X->S, XX(-4-i));
                assert(pre.type == CEU_VALUE_POINTER);
                printf("%s\n", (char*) pre.Pointer);
            }
            CEU_Value pay = ceux_peek(X->S, XX(-2));
            if (pay.type == CEU_VALUE_POINTER) {
                printf(" v  %s\n", (char*) pay.Pointer);     // payload is primitive error
            } else {
                printf(" v  error : ");
                ceu_print1(ceux_peek(X->S, XX(-2)));
                puts("");
            }
        }
    #endif

        ceux_base(X->S, 0);
        return 0;
    }
    """
    }

    return (
        h_includes() + h_defines() + h_enums() +
        (CEU>=3).cond { h_block } +
        h_value_dyn() + h_tags() +
        h1_ceux + h2_ceux +
        c_globals() + h_protos() +
        dumps() + c_error + gc() + c_tags() +
        c_ceux + c_impls() +
        // block-task-up, hold, bcast
        tuple_vector_dict() + creates() +
        print() + eq_neq_len() +
        // throw, pointer-to-string
        (CEU>=3).cond { c_exes } +
        (CEU>=4).cond { c_task } +
        (CEU>=4).cond { c_bcast } +
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
    """ // TRACK
        #if CEU >= 5
        CEU_Value ceu_track_f (CEU_Frame* frame, CEUX* X) {
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
        CEU_Value ceu_detrack_f (CEU_Frame* frame, CEUX* X) {
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
        #endif
    """ +
 */
