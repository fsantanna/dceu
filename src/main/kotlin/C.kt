package dceu

fun Coder.main (tags: Tags): String {
    return ("" +
    """ // INCLUDES / DEFINES / ENUMS
        ${DEBUG.cond { "#define CEU_DEBUG" }}
        #define CEU $CEU
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
        #if CEU >= 2
            CEU_VALUE_THROW,
        #endif
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
    """ +
    """ // DEBUG / SPACES
    #ifdef CEU_DEBUG
        int SPC = 0;
        void spc () {
            for (int i=0; i<SPC; i++) {
                printf("  ");
            }
        }
    #endif
    """ +
    """ // CEU_Frame, CEU_Block
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

        typedef struct CEU_Dyns {           // list of allocated data to bcast/free
            union CEU_Dyn* first;
            union CEU_Dyn* last;
        } CEU_Dyns;

        typedef struct CEU_Block {
            uint8_t  istop;
            union {
                struct CEU_Frame* frame;    // istop = 1
                struct CEU_Block* block;    // istop = 0
            } up;
            struct {
        #if CEU >= 4
                struct CEU_Block* block;    // bcast
        #endif
                CEU_Dyns dyns;
            } dn;
        } CEU_Block;
    """ +
    """   // CEU_Value, CEU_Dyn
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
        
        typedef void (*CEU_Proto) (
            CEU5(CEU_Stack* dstk COMMA)
            CEU4(CEU_Stack* bstk COMMA)
            struct CEU_Frame* frame,
            int n
        );

        #define _CEU_Clo_                   \
            _CEU_Dyn_                       \
            struct CEU_Frame* up_frame;     \
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
    """ +
    """ // GLOBALS
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
        CEU_Block CEU_BLOCK = { 0, {.block=NULL}, { CEU4(NULL COMMA) {NULL,NULL} } };
        CEU_Frame CEU_FRAME = { &CEU_BLOCK, NULL CEU3(COMMA {.exe=NULL}) };                
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
    """ +
    """ // PROTOS
        int ceu_top (void);
        void ceu_push (CEU_Value v);
        CEU_Value ceu_peek (int i);
        void ceu_pop (int i);

        void ceu_type_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n);
        int ceu_as_bool (CEU_Value v);
        CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn);

        void ceu_tags_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n);
        char* ceu_tag_to_string (int tag);
        int ceu_type_to_size (int type);

        void ceu_hold_add (CEU_Dyn* dyn, CEU_Block* blk CEU5(COMMA CEU_Dyns* dyns));
        void ceu_hold_rem (CEU_Dyn* dyn);

        CEU_Value ceu_create_tuple   (CEU_Block* hld, int n);
        CEU_Value ceu_create_vector  (CEU_Block* hld);
        CEU_Value ceu_create_dict    (CEU_Block* hld);
        CEU_Value ceu_create_clo     (CEU_Block* hld, CEU_Frame* frame, CEU_Proto proto, int upvs);
        #if CEU >= 4
        CEU_Value ceu_create_track   (CEU_Block* blk, CEU_Exe_Task* task);
        #endif

        void ceu_tuple_set (CEU_Tuple* tup, int i, CEU_Value v);

        CEU_Value ceu_vector_get (CEU_Vector* vec, int i);
        void ceu_vector_set (CEU_Vector* vec, int i, CEU_Value v);
        CEU_Value ceu_vector_from_c_string (CEU_Block* hld, const char* str);
        
        int ceu_dict_key_to_index (CEU_Dict* col, CEU_Value key, int* idx);
        CEU_Value ceu_dict_get (CEU_Dict* col, CEU_Value key);
        CEU_Value ceu_dict_set (CEU_Dict* col, CEU_Value key, CEU_Value val);
        CEU_Value ceu_col_check (CEU_Value col, CEU_Value idx);

        void ceu_print1 (CEU_Frame* _1, CEU_Value v);
        CEU_Value _ceu_op_equals_equals_ (CEU_Value e1, CEU_Value e2);

        #if CEU >= 2
        void ceu_pointer_to_string_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n);
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
    """ +
    """ // DUMPS
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
        void ceu_dump_frame (CEU_Frame* frame) {
            printf(">>> FRAME: %p\n", frame);
            printf("    up_block = %p\n", frame->up_block);
            printf("    clo      = %p\n", frame->clo);
        #if CEU >= 4
            printf("    exe      = %p\n", frame->exe);
        #endif
        }
        void ceu_dump_value (CEU_Value v) {
            puts(">>>>>>>>>>>");
            ceu_print1(NULL, v);
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
        void ceu_dump_block (CEU_Block* blk) {
            printf(">>> BLOCK: %p\n", blk);
            printf("    istop = %d\n", blk->istop);
            printf("    up    = %p\n", blk->up.frame);
            CEU_Dyn* cur = blk->dn.dyns.first;
            while (cur != NULL) {
                ceu_dump_value(ceu_dyn_to_val(cur));
                CEU_Dyn* old = cur;
                //cur = old->Any.hld.next;
            }
        }
        
        void ceu_dump_stack (void) {
            for (int i=0; i<ceu_top(); i++) {
                printf(">>> %d\n", i);
                ceu_dump_value(ceu_peek(i));
            }
        }
    #endif
    """ +
    """ // TAGS
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
    """ +
    """ // EXIT / ERROR / ASSERT
        #define CEU_ERR_OR(err,v) ({ CEU_Value ceu=v; assert(!(CEU_ISERR(err) && CEU_ISERR(ceu)) && "TODO: double error"); (CEU_ISERR(err) ? err : ceu); })
        #if CEU <= 1
        #define CEU_ISERR(v) (v.type == CEU_VALUE_ERROR)
        #define CEU_ERROR(blk,pre,err)  _ceu_error_(blk,pre,err)
        #define CEU_ASSERT(blk,err,pre) ceu_assert(blk,err,pre)
        #else
        #define CEU_ISERR(v) (v.type==CEU_VALUE_ERROR || v.type==CEU_VALUE_THROW)
        #define CEU_ERROR_PUSH(pre,err) {                   \
            assert(err.type == CEU_VALUE_THROW);            \
            assert (                                        \
                ceu_vector_set (                            \
                    &err.Dyn->Throw.stk.Dyn->Vector,        \
                    err.Dyn->Throw.stk.Dyn->Vector.its,     \
                    _ceu_pointer_to_string_(CEU_HLD_BLOCK(err.Dyn),pre) \
                ).type != CEU_VALUE_ERROR                   \
            );                                              \
        }
        #define CEU_ERROR(blk,pre,err) {            \
            if (err.type == CEU_VALUE_THROW) {      \
                CEU_REPL(err);                       \
            } else {                                \
                CEU_REPL(_ceu_throw_(blk, err));     \
            }                                       \
            CEU_ERROR_PUSH(pre,ceu_acc);            \
            continue;                               \
        }
        #define CEU_ASSERT(blk,err,pre) ({      \
            CEU_Value ceu_err = err;            \
            if (CEU_ISERR(ceu_err)) {           \
                CEU_ERROR(blk,pre,ceu_err);     \
            };                                  \
            ceu_err;                            \
        })
        #endif

        void ceu_exit (CEU5(CEU_Stack* dstk COMMA) CEU4(CEU_Stack* bstk COMMA) CEU_Block* blk) {
            if (blk == NULL) {
                exit(0);
            }
            CEU_Block* up = (blk->istop) ? blk->up.frame->up_block : blk->up.block;
            //ceu_gc_rem_all(CEU5(dstk COMMA) CEU4(bstk COMMA) blk);
            return ceu_exit(CEU5(dstk COMMA) CEU4(bstk COMMA) up);
        }
        void _ceu_error_ (CEU5(CEU_Stack* dstk COMMA) CEU4(CEU_Stack* bstk COMMA) CEU_Block* blk, char* pre, CEU_Value err) {
            fprintf(stderr, "%s : %s\n", pre, err.Error);
            ceu_exit(CEU5(bstk COMMA) CEU4(bstk COMMA) blk);
        }
        CEU_Value ceu_assert (CEU5(CEU_Stack* dstk COMMA) CEU4(CEU_Stack* bstk COMMA) CEU_Block* blk, CEU_Value err, char* pre) {
            if (CEU_ISERR(err)) {
                _ceu_error_(CEU5(dstk COMMA) CEU4(bstk COMMA) blk, pre, err);
            }
            return err;
        }
        void ceu_error_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            assert(n == 1);
            CEU_Value arg = ceu_peek(-1);
            assert(arg.type == CEU_VALUE_TAG);
            CEU_Value ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error=ceu_tag_to_string(arg.Tag)} };
            ceu_pop(-n);
            ceu_push(ret);
        }        
    """ +
    """ // GC
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
            assert(!CEU_ISERR(ret) && "TODO: impossible case");
        #endif
            ceu_gc_dec_rec(dyn);
            ceu_hold_rem(dyn);
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
            #if CEU >= 2
                case CEU_VALUE_THROW:
                    ceu_gc_dec(dyn->Throw.val);
                    ceu_gc_dec(dyn->Throw.stk);
                    break;
            #endif
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
    #if CEU >= 2
                case CEU_VALUE_THROW:
                    break;
    #endif
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
    """ +
    """ // CEU_VSTK
        #define CEU_VSTK_MAX 16
        CEU_Value ceu_vstk[CEU_VSTK_MAX];
        int ceu_vstk_n = 0;
        int ceu_top (void) {
            return ceu_vstk_n;
        }
        void ceu_push (CEU_Value v) {
            ceu_gc_inc(v);
            assert(ceu_vstk_n<CEU_VSTK_MAX && "TODO: stack overflow");
            ceu_vstk[ceu_vstk_n++] = v;
        }
        CEU_Value ceu_peek (int i) {
            int I = (i>=0) ? i : ceu_vstk_n+i;
            assert(I<ceu_vstk_n && "BUG: index out of range");
            return ceu_vstk[I];
        }
        void ceu_pop (int i) {
            int I = (i>=0) ? ceu_vstk_n-i : -i;
            assert(i<=ceu_vstk_n && "BUG: index out of range");
            for (int x=0; x<I; x++) {
                ceu_gc_dec(ceu_vstk[--ceu_vstk_n]);
            }
        }
    """ +
    """ // IMPLS
        CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn) {
            return (CEU_Value) { dyn->Any.type, {.Dyn=dyn} };
        }
        
        void ceu_dump_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            assert(n == 1);
        #ifdef CEU_DEBUG
            ceu_dump_value(ceu_peek(-1));
            ceu_pop(-n);
            ceu_push((CEU_Value) { CEU_VALUE_NIL });
        #else
            ceu_pop(-n);
            ceu_push((CEU_Value) { CEU_VALUE_ERROR, {.Error="debug is off"} });
        #endif
        }

        int ceu_as_bool (CEU_Value v) {
            return !(v.type==CEU_VALUE_NIL || (v.type==CEU_VALUE_BOOL && !v.Bool));
        }
        void ceu_type_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            assert(n == 1 && "bug found");
            int type = ceu_peek(-1).type;
            ceu_pop(-n);
            ceu_push((CEU_Value) { CEU_VALUE_TAG, {.Tag=type} });
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
        void ceu_sup_question__f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            assert(n >= 2);
            CEU_Value sup = ceu_peek(-2);
            CEU_Value sub = ceu_peek(-1);
            CEU_Value ret = _ceu_sup_(sup, sub);
            ceu_pop(-n);
            ceu_push(ret);
        }
        
        CEU_Value _ceu_tags_all_ (CEU_Value dyn) {
            int len = 0; {
                CEU_Tags_List* cur = dyn.Dyn->Any.tags;
                while (cur != NULL) {
                    len++;
                    cur = cur->next;
                }
            }
            CEU_Value tup = ceu_create_tuple(NULL, len);
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
            
        void ceu_tags_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
            assert(n >= 1);
            CEU_Value dyn = ceu_peek(-n+0);
            assert(dyn.type > CEU_VALUE_DYNAMIC);
            CEU_Value tag; // = (CEU_Value) { CEU_VALUE_NIL };
            if (n >= 2) {
                tag = ceu_peek(-n+1);
                assert(tag.type == CEU_VALUE_TAG);
            }
            
            CEU_Value f_chk () {
                CEU_Value ret;
                CEU_Tags_List* cur = dyn.Dyn->Any.tags;
                while (cur != NULL) {
                    CEU_Value sub = { CEU_VALUE_TAG, {.Tag=cur->tag} };
                    CEU_Value ret = _ceu_sup_(tag, sub);
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
            
            CEU_Value ret;
            switch (n) {
                case 1: {   // all tags
                    ret = _ceu_tags_all_(dyn);
                    break;
                }
                case 2: {   // check tag
                    ret = f_chk();
                    break;
                }
                default: {   // add/rem tag
                    CEU_Value bool = ceu_peek(-n+2);
                    assert(bool.type == CEU_VALUE_BOOL);
                    f_set(bool.Bool);
                    ceu_gc_inc(dyn);
                    ret = dyn;
                    break;
                }
            }
            ceu_pop(-n);
            ceu_push(ret);
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
        void ceu_string_to_tag_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            assert(n == 1);
            CEU_Value str = ceu_peek(-n);
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
            ceu_pop(-n);
            ceu_push(ret);
        }
    """ +
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
                    CEU_ASSERT(BUPC, ceu_peek(-1), "FILE : (lin LIN, col COL) : ERR");
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

        void ceu_broadcast_f (CEU5(CEU_Stack* dstk COMMA) CEU_Stack* bstk, CEU_Frame* frame, int n) {
            assert(n == 2);
            ceu_bstk_assert(bstk);

            assert(CEU_TIME_N < 255);
            CEU_TIME_N++;
            uint8_t now = ++CEU_TIME_MAX;
            
            CEU_Value evt = ceu_peek(-n);
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
    """ // TUPLE / VECTOR / DICT
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
        
        CEU_Value ceu_vector_from_c_string (CEU_Block* hld, const char* str) {
            CEU_Value vec = ceu_create_vector(hld);
            int N = strlen(str);
            for (int i=0; i<N; i++) {
                ceu_vector_set(&vec.Dyn->Vector, vec.Dyn->Vector.its, (CEU_Value) { CEU_VALUE_CHAR, {.Char=str[i]} });
            }
            ceu_gc_inc(vec);
            return vec;
        }

        void ceu_next_dict_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
            assert(n==1 || n==2);
            CEU_Value dict = ceu_peek(-n);
            CEU_Value ret;
            if (dict.type != CEU_VALUE_DICT) {
                ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="next-dict error : expected dict"} };
            } else {
                CEU_Value key = (n == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : ceu_peek(-n+1);
                if (key.type == CEU_VALUE_NIL) {
                    ret = (*dict.Dyn->Dict.buf)[0][0];
                } else {
                    ret = (CEU_Value) { CEU_VALUE_NIL };
                    for (int i=0; i<dict.Dyn->Dict.max-1; i++) {     // -1: last element has no next
                        CEU_Value ok = _ceu_op_equals_equals_(key, (*dict.Dyn->Dict.buf)[i][0]);
                        assert(ok.type != CEU_VALUE_ERROR);
                        if (ok.Bool) {
                            ret = (*dict.Dyn->Dict.buf)[i+1][0];
                            break;
                        }
                    }
                }
            }
            ceu_pop(-n);
            ceu_push(ret);
        }
        
    #if CEU >= 5
        CEU_Value _ceu_next_tasks_f_ (CEU_Frame* frame, int n) {
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
                return ceu_create_track(frame->up_block, &nxt->Exe_Task);
            }
        }
        CEU_Value ceu_next_tasks_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
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
                CEU_Value ret = _ceu_op_equals_equals_(key, cur);
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
    """ +
    """ // CREATES
        CEU_Value ceu_create_tuple (CEU_Block* blk, int n) {
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
        
        void ceu_tuple_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
            assert(n == 1);
            CEU_Value arg = ceu_peek(-1);
            assert(arg.type == CEU_VALUE_NUMBER);
            CEU_Value ret = ceu_create_tuple(frame->up_block, arg.Number);
            ceu_pop(-n);
            ceu_push(ret);
        }
        
        CEU_Value ceu_create_vector (CEU_Block* blk) {
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
        
        CEU_Value ceu_create_dict (CEU_Block* blk) {
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
        
        CEU_Value _ceu_create_clo_ (int sz, int type, CEU_Block* blk, CEU_Frame* frame, CEU_Proto proto, int upvs) {
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
                frame, proto, { upvs, buf }
            };
            //ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
            return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
        }

        CEU_Value ceu_create_clo (CEU_Block* blk, CEU_Frame* frame, CEU_Proto proto, int upvs) {
            return _ceu_create_clo_(sizeof(CEU_Clo), CEU_VALUE_CLO_FUNC, blk, frame, proto, upvs);
        }

        #if CEU >= 3
        CEU_Value ceu_create_clo_exe (int type, CEU_Block* blk, CEU_Frame* frame, CEU_Proto proto, int upvs) {
            CEU_Value clo = _ceu_create_clo_(sizeof(CEU_Clo_Exe), blk, frame, proto, upvs);
            clo.Dyn->Clo_Exe.mem_n = 0;
            return clo;
        }
        #endif

        #if CEU >= 3
        CEU_Value _ceu_create_exe_ (int type, int sz, CEU_Block* blk, CEU_Value clo CEU5(COMMA CEU_Dyns* dyns)) {
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
        CEU_Value ceu_create_track (CEU_Block* blk, CEU_Exe_Task* task) {
            ceu_debug_add(CEU_VALUE_TRACK);
            CEU_Track* ret = malloc(sizeof(CEU_Track));
            assert(ret != NULL);
            *ret = (CEU_Track) {
                CEU_VALUE_TRACK, 0, NULL, { blk, NULL, NULL },
                task
            };
            ceu_hold_add((CEU_Dyn*)ret, blk, &blk->dn.dyns);
            return (CEU_Value) { CEU_VALUE_TRACK, {.Dyn=(CEU_Dyn*)ret} };
        }
        
        CEU_Value ceu_tasks_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
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
            return ceu_create_tasks(frame->up_block, max);
        }
        #endif
    """ +
    """ // PRINT
        void ceu_print1 (CEU_Frame* _1, CEU_Value v) {
            // no tags when _1==NULL (ceu_error_list_print)
            if (_1!=NULL && v.type>CEU_VALUE_DYNAMIC) {  // TAGS
                CEU_Value tup = _ceu_tags_all_(v);
                assert(tup.type == CEU_VALUE_TUPLE);
                int N = tup.Dyn->Tuple.its;
                if (N > 0) {
                    if (N > 1) {
                        printf("[");
                    }
                    for (int i=0; i<N; i++) {
                        ceu_print1(_1, tup.Dyn->Tuple.buf[i]);
                        if (i < N-1) {
                            printf(",");
                        }
                    }
                    if (N > 1) {
                        printf("]");
                    }
                    printf(" ");
                }
            }
            switch (v.type) {
                case CEU_VALUE_NIL:
                    printf("nil");
                    break;
                case CEU_VALUE_ERROR:
                    printf("%s", v.Error);
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
                        ceu_print1(_1, v.Dyn->Tuple.buf[i]);
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
                            ceu_print1(_1, ret);
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
                            ceu_print1(_1, (*v.Dyn->Dict.buf)[i][0]);
                            printf(",");
                            ceu_print1(_1, (*v.Dyn->Dict.buf)[i][1]);
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
                            ceu_print1(_1, v.Dyn->Clo.upvs.buf[i]);
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
        #if CEU >= 2
                case CEU_VALUE_THROW:
                    printf("throw: %p | ", v.Dyn);
                    ceu_print1(_1, v.Dyn->Throw.val);
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
        void ceu_print_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            for (int i=0; i<n; i++) {
                if (i > 0) {
                    printf("\t");
                }
                ceu_print1(_2, ceu_peek(-n+i));
            }
            ceu_pop(-n);
            ceu_push((CEU_Value) { CEU_VALUE_NIL });
        }
        void ceu_println_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            ceu_print_f(CEU5(_0 COMMA) CEU4(_1 COMMA) _2, n);
            printf("\n");
        }
    """ +
    """
        // EQ / NEQ / LEN
        CEU_Value _ceu_op_equals_equals_ (CEU_Value e1, CEU_Value e2) {
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
            #if CEU >= 2
                    case CEU_VALUE_THROW:
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
        void ceu_op_equals_equals_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            assert(n == 2);
            CEU_Value ret = _ceu_op_equals_equals_(ceu_peek(-n), ceu_peek(-n+1));
            ceu_pop(-n);
            ceu_push(ret);
        }
        void ceu_op_slash_equals_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            ceu_op_equals_equals_f(CEU5(_0 COMMA) CEU4(_1 COMMA) _2, n);
            CEU_Value ret = ceu_peek(-1);
            ret.Bool = !ret.Bool;
            ceu_pop(-1);
            ceu_push(ret);
        }
        
        void ceu_op_hash_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* _2, int n) {
            assert(n == 1);
            CEU_Value v = ceu_peek(-n);
            CEU_Value ret;
            if (v.type == CEU_VALUE_VECTOR) {
                ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=v.Dyn->Vector.its} };
            } else if (v.type == CEU_VALUE_TUPLE) {
                ret = (CEU_Value) { CEU_VALUE_NUMBER, {.Number=v.Dyn->Tuple.its} };
            } else {
                ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="length error : not a vector"} };
            }
            ceu_pop(-n);
            ceu_push(ret);
        }
    """ +
    """
        // THROW / POINTER-TO-STRING
        #if CEU >= 2
        CEU_Value _ceu_throw_ (CEU_Block* blk, CEU_Value val) {
            CEU_Value stk = ceu_create_vector(blk);
            
            ceu_gc_inc(val);
            ceu_gc_inc(stk);

            CEU_Throw* ret = malloc(sizeof(CEU_Throw));
            assert(ret != NULL);
            *ret = (CEU_Throw) {
                CEU_VALUE_THROW, 0, NULL, { blk, NULL, NULL },
                val, stk
            };
            
            ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
            
            return (CEU_Value) { CEU_VALUE_THROW, {.Dyn=(CEU_Dyn*)ret} };
        }

        CEU_Value ceu_throw_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
            -=- TODO -=-
            assert(n == 1);
            return _ceu_throw_(frame->up_block, args[0]);
        }

        CEU_Value _ceu_pointer_to_string_ (CEU_Block* blk, const char* ptr) {
            CEU_Value str = ceu_create_vector(blk);
            int len = strlen(ptr);
            for (int i=0; i<len; i++) {
                CEU_Value chr = { CEU_VALUE_CHAR, {.Char=ptr[i]} };
                ceu_vector_set(&str.Dyn->Vector, i, chr);
            }
            return str;
        }

        CEU_Value ceu_pointer_to_string_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
            assert(n == 1);
            assert(args[0].type == CEU_VALUE_POINTER);
            return _ceu_pointer_to_string_(frame->up_block, args[0].Pointer);
        }
        #endif
    """ +
    """ // ISEXE / COROUTINE / STATUS / EXE_KILL
        #if CEU >= 3
        int ceu_isexe (CEU_Dyn* dyn) {
            return (dyn->Any.type==CEU_VALUE_EXE_CORO CEU4(|| ceu_istask_dyn(dyn)));
        }
        
        CEU_Value ceu_coroutine_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
            assert(n == 1);
            CEU_Value coro = args[0];
            if (coro.type != CEU_VALUE_CLO_CORO) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="coroutine error : expected coro"} };
            }
            return _ceu_create_exe_(CEU_VALUE_EXE_CORO, sizeof(CEU_Exe), frame->up_block, coro CEU5(COMMA &frame->up_block->dn.dyns));
        }
        
        CEU_Value ceu_status_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
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
        CEU_Value ceu_track_f (CEU5(CEU_Stack* _0 COMMA) CEU4(CEU_Stack* _1 COMMA) CEU_Frame* frame, int n) {
            assert(n == 1);
            CEU_Value task = args[0];
            if (!ceu_istask_val(task)) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="track error : expected task"} };
            } else if (task.Dyn->Exe_Task.status == CEU_EXE_STATUS_TERMINATED) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="track error : expected unterminated task"} };
            }
            //CEU_Block* blk = (ceu_dmin(task->Dyn->up_dyns.dyns->up_block) > ceu_dmin(frame->up_block)) ?
            //    task->Dyn->up_dyns.dyns->up_block : frame->up_block;
            return ceu_create_track(frame->up_block, (CEU_Exe_Task*)task.Dyn);
        }
        CEU_Value ceu_detrack_f (CEU_Stack* _0, CEU_Stack* _1, CEU_Frame* frame, int n) {
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
    """ // FUNCS
        CEU_Clo ceu_dump = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_dump_f, {0,NULL}
        };
        CEU_Clo ceu_error = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_error_f, {0,NULL}
        };
        CEU_Clo ceu_next_dict = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_next_dict_f, {0,NULL}
        };
        CEU_Clo ceu_print = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_print_f, {0,NULL}
        };
        CEU_Clo ceu_println = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_println_f, {0,NULL}
        };
        CEU_Clo ceu_sup_question_ = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_sup_question__f, {0,NULL}
        };
        CEU_Clo ceu_tags = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_tags_f, {0,NULL}
        };
        CEU_Clo ceu_tuple = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_tuple_f, {0,NULL}
        };
        CEU_Clo ceu_type = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_type_f, {0,NULL}
        };
        CEU_Clo ceu_op_equals_equals = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_op_equals_equals_f, {0,NULL}
        };
        CEU_Clo ceu_op_hash = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_op_hash_f, {0,NULL}
        };
        CEU_Clo ceu_op_slash_equals = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_op_slash_equals_f, {0,NULL}
        };
        CEU_Clo ceu_string_to_tag = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_string_to_tag_f, {0,NULL}
        };
        #if CEU >= 2
        CEU_Clo ceu_pointer_to_string = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_pointer_to_string_f, {0,NULL}
        };
        CEU_Clo ceu_throw = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_throw_f, {0,NULL}
        };
        #endif
        #if CEU >= 3
        CEU_Clo ceu_coroutine = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_coroutine_f, {0,NULL}
        };
        CEU_Clo ceu_status = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_status_f, {0,NULL}
        };
        #endif
        #if CEU >= 4
        CEU_Clo ceu_broadcast = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_broadcast_f, {0,NULL}
        };
        #endif
        #if CEU >= 5
        CEU_Clo ceu_tasks = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_tasks_f, {0,NULL}
        };
        CEU_Clo ceu_track = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_track_f, {0,NULL}
        };
        CEU_Clo ceu_next_tasks = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_next_tasks_f, {0,NULL}
        };
        CEU_Clo ceu_detrack = { 
            CEU_VALUE_CLO_FUNC, 1, NULL,
            &CEU_FRAME, ceu_detrack_f, {0,NULL}
        };
        #endif

        CEU_Value id_dump                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_dump}                    };
        CEU_Value id_error                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_error}                   };
        CEU_Value id_next_dash_dict          = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_next_dict}               };
        CEU_Value id_print                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_print}                   };
        CEU_Value id_println                 = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_println}                 };
        CEU_Value id_tags                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_tags}                    };
        CEU_Value id_type                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_type}                    };
        CEU_Value id_tuple                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_tuple}                   };
        CEU_Value op_hash                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_op_hash}                 };
        CEU_Value id_sup_question_           = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_sup_question_}           };
        CEU_Value op_equals_equals           = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_op_equals_equals}        };
        CEU_Value op_slash_equals            = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_op_slash_equals}         };
        CEU_Value id_string_dash_to_dash_tag = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_string_to_tag}           };
        #if CEU >= 2
        CEU_Value id_pointer_dash_to_dash_string = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_pointer_to_string}   };
        CEU_Value id_throw                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_throw}                   };
        #endif
        #if CEU >= 3
        CEU_Value id_coroutine               = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_coroutine}               };
        CEU_Value id_status                  = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_status}                  };
        #endif
        #if CEU >= 4
        CEU_Value id_broadcast_plic_         = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_broadcast}               };
        #endif
        #if CEU >= 5
        CEU_Value id_tasks                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_tasks}                   };
        CEU_Value id_detrack_plic_           = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_detrack}                 };
        CEU_Value id_track                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_track}                   };
        CEU_Value id_next_dash_tasks         = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_next_tasks}              };
        #else
        CEU_Value id_detrack_plic_           = (CEU_Value) { CEU_VALUE_NIL };   // bc of detrack'' in prelude        
        #endif
    """ +
    """ // MAIN
        int main (int ceu_argc, char** ceu_argv) {
            assert(CEU_TAG_nil == CEU_VALUE_NIL);
            char ceu_err_msg[255];
        #if CEU >= 4
           CEU_Stack* ceu_bstk = &CEU_BSTK;
        #endif
        #if CEU >= 5
           CEU_Stack* ceu_dstk = &CEU_DSTK;
        #endif
            CEU_Frame* ceu_frame = &CEU_FRAME;
            ${this.code}
            return 0;
        }
    """)
}
