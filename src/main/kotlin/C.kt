package dceu

fun Coder.main (tags: Tags): String {
    return ("" +
    """ // INCLUDES / DEFINES / ENUMS
        //#define CEU_DEBUG
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
        
        #if CEU >= 5
        #define CEU_HLD_BLOCK(dyn) ({ CEU_Block* blk=(dyn)->Any.hld.block; (dyn)->Any.type!=CEU_VALUE_EXE_TASK_IN ? blk : (((CEU_Tasks*)blk)->hld.block); }) 
        #define CEU_HLD_DYNS(dyn) ((dyn)->Any.type == CEU_VALUE_EXE_TASK_IN ? (&((CEU_Tasks*)((dyn)->Any.hld.block))->dyns) : (&(dyn)->Any.hld.block->dn.dyns)) 
        #else
        #define CEU_HLD_BLOCK(dyn) ((dyn)->Any.hld.block)
        #define CEU_HLD_DYNS(dyn) (&(dyn)->Any.hld.block->dn.dyns)
        #endif
        
        typedef enum CEU_HOLD {
            CEU_HOLD_FLEET = 0,     // not assigned, dst assigns
            CEU_HOLD_MUTAB,         // set and assignable to narrow 
            CEU_HOLD_IMMUT,         // set but not assignable (nested fun)
            CEU_HOLD_MAX
        } __attribute__ ((__packed__)) CEU_HOLD;
        _Static_assert(sizeof(CEU_HOLD) == 1, "bug found");
        
        typedef enum CEU_ARG {
        #if CEU >= 4
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
        #if CEU >= 4
            CEU_VALUE_REF,
        #endif
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
            //CEU_EXE_STATUS_TOGGLED,
            CEU_EXE_STATUS_RESUMED,
            CEU_EXE_STATUS_TERMINATED,
            CEU_EXE_STATUS_ABORTED,
        } CEU_EXE_STATUS;
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
            uint16_t depth;
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
            struct CEU_Tags_List* tags;     \
            union CEU_Dyn* tofree;          \
            struct {                        \
                CEU_HOLD type;              \
                CEU_Block* block;   /* tasks */ \
                union CEU_Dyn* prev;        \
                union CEU_Dyn* next;        \
            } hld;
            
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
        
        typedef CEU_Value (*CEU_Proto) (
            struct CEU_Frame* frame,
            int n,
            struct CEU_Value args[]
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
        CEU_Value ceu_type_f (CEU_Frame* _1, int n, CEU_Value args[]);
        int ceu_as_bool (CEU_Value v);

        CEU_Value ceu_tags_f (CEU_Frame* _1, int n, CEU_Value args[]);
        char* ceu_tag_to_string (int tag);
        int ceu_tag_to_size (int type);
                
        void ceu_dyn_rem_free (CEU_Dyn* dyn);
        void ceu_dyn_free (CEU_Dyn* dyn);
        void ceu_dyns_free (CEU_Dyns* dyns);
        
        void ceu_gc_inc (CEU_Value v);
        void ceu_gc_dec (CEU_Value v, int chk);

        void ceu_hold_add (CEU_Dyn* dyn, CEU_Block* blk CEU5(COMMA CEU_Dyns* dyns));
        void ceu_hold_rem (CEU_Dyn* dyn);

        int ceu_hold_set (CEU_Dyn** dst, int depth, CEU_HOLD hld_type, CEU_Dyn* src);
        
        CEU_Value ceu_create_tuple   (CEU_Block* hld, int n);
        CEU_Value ceu_create_vector  (CEU_Block* hld);
        CEU_Value ceu_create_dict    (CEU_Block* hld);
        CEU_Value ceu_create_clo     (int type, CEU_Block* hld, CEU_HOLD hld_type, CEU_Frame* frame, CEU_Proto proto, int upvs);
        #if CEU >= 4
        CEU_Value ceu_create_track   (CEU_Block* blk, CEU_Exe_Task* task);
        #endif

        CEU_Value ceu_tuple_set (CEU_Tuple* tup, int i, CEU_Value v CEU4(COMMA int ylds));

        CEU_Value ceu_vector_get (CEU_Vector* vec, int i);
        CEU_Value ceu_vector_set (CEU_Vector* vec, int i, CEU_Value v CEU4(COMMA int ylds));
        CEU_Value ceu_vector_from_c_string (CEU_Block* hld, const char* str);
        
        int ceu_dict_key_to_index (CEU_Dict* col, CEU_Value key, int* idx);
        CEU_Value ceu_dict_get (CEU_Dict* col, CEU_Value key);
        CEU_Value ceu_dict_set (CEU_Dict* col, CEU_Value key, CEU_Value val CEU4(COMMA int ylds));
        CEU_Value ceu_col_check (CEU_Value col, CEU_Value idx);

        void ceu_print1 (CEU_Frame* _1, CEU_Value v);
        CEU_Value ceu_op_equals_equals_f (CEU_Frame* _1, int n, CEU_Value args[]);

        #if CEU >= 2
        CEU_Value ceu_pointer_dash_to_dash_string_f (CEU_Frame* frame, int n, CEU_Value args[]);
        #endif
        #if CEU >= 4
        CEU_Exe_Task* ceu_task_up_task (CEU_Exe_Task* task);
        int ceu_istask_dyn (CEU_Dyn* dyn);
        int ceu_istask_val (CEU_Value val);
        CEU_Value ceu_toref (CEU_Value v);
        CEU_Value ceu_deref (CEU_Value v);
        #endif
    """ +
    """ // GC_COUNT / TAGS
        int ceu_gc_count = 0;
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
        #if CEU <= 1
        #define CEU_ISERR(v) (v.type == CEU_VALUE_ERROR)
        #define CEU_ERROR(blk,pre,err)  _ceu_error_(blk,pre,err)
        #define CEU_ASSERT(blk,err,pre) ceu_assert(blk,err,pre)
        #else
        #define CEU_ISERR(v) (v.type==CEU_VALUE_ERROR || v.type==CEU_VALUE_THROW)
        #define CEU_ERROR(blk,pre,err) {                       \
            if (err.type == CEU_VALUE_THROW) {                  \
                ceu_acc = err;                                  \
            } else {                                            \
                ceu_acc = _ceu_throw_(blk, err);                \
            }                                                   \
            CEU_Value ceu_str = ceu_pointer_to_string(blk,pre); \
            assert(ceu_vector_set(&ceu_acc.Dyn->Throw.stk.Dyn->Vector, ceu_acc.Dyn->Throw.stk.Dyn->Vector.its, ceu_str CEU4(COMMA 1)).type != CEU_VALUE_ERROR); \
            continue;                                           \
        }
        #define CEU_ASSERT(blk,err,pre) ({      \
            CEU_Value ceu_err = err;            \
            if (CEU_ISERR(ceu_err)) {           \
                CEU_ERROR(blk,pre,ceu_err);     \
            };                                  \
            ceu_err;                            \
        })
        #endif

        void ceu_exit (CEU_Block* blk) {
            if (blk == NULL) {
                exit(0);
            }
            CEU_Block* up = (blk->istop) ? blk->up.frame->up_block : blk->up.block;
            ceu_dyns_free(&blk->dn.dyns);
            return ceu_exit(up);
        }
        void _ceu_error_ (CEU_Block* blk, char* pre, CEU_Value err) {
            fprintf(stderr, "%s : %s\n", pre, err.Error);
            ceu_exit(blk);
        }
        CEU_Value ceu_assert (CEU_Block* blk, CEU_Value err, char* pre) {
            if (CEU_ISERR(err)) {
                _ceu_error_(blk, pre, err);
            }
            return err;
        }
        CEU_Value ceu_error_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n==1 && args[0].type==CEU_VALUE_TAG);
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error=ceu_tag_to_string(args[0].Tag)} };
        }        
    """ +
    """ // IMPLS
        CEU_Value ceu_dyn_to_val (CEU_Dyn* dyn) {
            return (CEU_Value) { dyn->Any.type, {.Dyn=dyn} };
        }
        
        void _ceu_dump_ (CEU_Value v) {
            puts(">>>>>>>>>>>");
            ceu_print1(NULL, v);
            puts(" <<<");
            if (v.type > CEU_VALUE_DYNAMIC) {
                printf("    dyn   = %p\n", v.Dyn);
                printf("    refs  = %d\n", v.Dyn->Any.refs);
                printf("    hold  = %d\n", v.Dyn->Any.hld.type);
                printf("    block = %p\n", CEU_HLD_BLOCK(v.Dyn));
                printf("    depth = %d\n", CEU_HLD_BLOCK(v.Dyn)->depth);
                printf("    next  = %p\n", v.Dyn->Any.hld.next);
                printf("    ----\n");
                switch (v.type) {
            #if CEU >= 4
                    case CEU_VALUE_EXE_TASK:
                        printf("    pub   = %d\n", v.Dyn->Exe_Task.pub.type);
                        break;
            #endif
            #if CEU >= 5
                    case CEU_VALUE_TASKS:
                        printf("    first = %p\n", v.Dyn->Tasks.dyns.first);
                        printf("    last  = %p\n", v.Dyn->Tasks.dyns.last);
                        break;
                    case CEU_VALUE_TRACK:
                        printf("    task  = %p\n", v.Dyn->Track.task);
                        break;
            #endif
                    default:
                        puts("TODO");
                }
            }
            puts("<<<<<<<<<<<");
        }
        CEU_Value ceu_dump_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n == 1);
            _ceu_dump_(args[0]);
            return (CEU_Value) { CEU_VALUE_NIL };
        }

        int ceu_as_bool (CEU_Value v) {
            return !(v.type==CEU_VALUE_NIL || (v.type==CEU_VALUE_BOOL && !v.Bool));
        }
        CEU_Value ceu_type_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n == 1 && "bug found");
            CEU_Value v = CEU4(ceu_deref)(args[0]);
            return (CEU_Value) { CEU_VALUE_TAG, {.Tag=v.type} };
        }
        CEU_Value ceu_sup_question__f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n >= 2);
            CEU_Value sup = args[0];
            CEU_Value sub = args[1];
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
        CEU_Value ceu_tags_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n >= 1);
            CEU_Value dyn = CEU4(ceu_deref)(args[0]);
            CEU_Tags_List* tags = (dyn.type < CEU_VALUE_DYNAMIC) ? NULL : dyn.Dyn->Any.tags;
            CEU_Value tag; // = (CEU_Value) { CEU_VALUE_NIL };
            if (n >= 2) {
                tag = args[1];
                assert(tag.type == CEU_VALUE_TAG);
            }
            switch (n) {
                case 1: {
                    int len = 0; {
                        CEU_Tags_List* cur = tags;
                        while (cur != NULL) {
                            len++;
                            cur = cur->next;
                        }
                    }
                    CEU_Value tup = ceu_create_tuple(frame->up_block, len);
                    {
                        CEU_Tags_List* cur = tags;
                        int i = 0;
                        while (cur != NULL) {
                            assert(ceu_tuple_set(&tup.Dyn->Tuple, i++, (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} } CEU4(COMMA 1)).type != CEU_VALUE_ERROR);
                            cur = cur->next;
                        }
                    }                    
                    return tup;
                }
                case 2: {   // check
                    CEU_Value ret = (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} };
                    CEU_Tags_List* cur = tags;
                    while (cur != NULL) {
                        CEU_Value args[] = {
                            tag,
                            (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} }
                        };
                        ret = ceu_sup_question__f(frame, 2, args);
                        if (ret.Bool) {
                            break;
                        }
                        cur = cur->next;
                    }
                    return ret;
                }
                default: {   // add/rem
                    assert(dyn.type > CEU_VALUE_DYNAMIC);
                    CEU_Value bool = args[2];
                    assert(bool.type == CEU_VALUE_BOOL);
                    if (bool.Bool) {   // add
                        CEU_Value chk = ceu_tags_f(frame, 2, args);
                        if (chk.Bool) {
                            return (CEU_Value) { CEU_VALUE_NIL };
                        } else {
                            CEU_Tags_List* v = malloc(sizeof(CEU_Tags_List));
                            assert(v != NULL);
                            v->tag = tag.Tag;
                            v->next = dyn.Dyn->Any.tags;
                            dyn.Dyn->Any.tags = v;
                            return dyn;
                        }
                    } else {            // rem
                        CEU_Value ret = (CEU_Value) { CEU_VALUE_NIL };
                        CEU_Tags_List** cur = &dyn.Dyn->Any.tags;
                        while (*cur != NULL) {
                            if ((*cur)->tag == tag.Tag) {
                                CEU_Tags_List* v = *cur;
                                *cur = v->next;
                                free(v);
                                ret = dyn;
                                break;
                            }
                            cur = &(*cur)->next;
                        }
                        return ret;
                    }
                }
            }
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
        CEU_Value ceu_string_dash_to_dash_tag_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n == 1);
            CEU_Value str = CEU4(ceu_deref)(args[0]);
            assert(str.type==CEU_VALUE_VECTOR && str.Dyn->Vector.unit==CEU_VALUE_CHAR);
            CEU_Tags_Names* cur = CEU_TAGS;
            while (cur != NULL) {
                if (!strcmp(cur->name,str.Dyn->Vector.buf)) {
                    return (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} };
                }
                cur = cur->next;
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
    """ +
    """ // GC
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
                        ceu_gc_dec(dyn->Clo.upvs.buf[i], 1);
                    }
                    break;
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<dyn->Tuple.its; i++) {
                        ceu_gc_dec(dyn->Tuple.buf[i], 1);
                    }
                    break;
                case CEU_VALUE_VECTOR:
                    for (int i=0; i<dyn->Vector.its; i++) {
                        CEU_Value ret = ceu_vector_get(&dyn->Vector, i);
                        assert(ret.type != CEU_VALUE_ERROR);
                        ceu_gc_dec(ret, 1);
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<dyn->Dict.max; i++) {
                        ceu_gc_dec((*dyn->Dict.buf)[i][0], 1);
                        ceu_gc_dec((*dyn->Dict.buf)[i][1], 1);
                    }
                    break;
            #if CEU >= 2
                case CEU_VALUE_THROW:
                    ceu_gc_dec(dyn->Throw.val, 1);
                    ceu_gc_dec(dyn->Throw.stk, 1);
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
                    ceu_gc_dec(ceu_dyn_to_val((CEU_Dyn*)dyn->Exe.frame.clo), 1);
                    break;
        #endif
        #if CEU >= 5
                case CEU_VALUE_TRACK:
                    // dyn->Track.task is a weak reference
                    break;
        #endif
                default:
                    assert(0);
                    break;
            }
            ceu_gc_count++;
            ceu_dyn_rem_free(dyn);
        }
        
        void ceu_gc_chk (CEU_Value v) {
            if (v.type > CEU_VALUE_DYNAMIC) {
                if (v.Dyn->Any.refs == 0) {
                    ceu_gc_free(v.Dyn);
                }
            }
        }

        // var x = ?        // var source
        // set x = ?        // set source
        // set x[...] = ?   //      - index value
        // set x[?] = ?     //      - index key
        // set x.pub = ?    //      - pub
        // [...?...]        // constructor argument     // TODO
        // f(?)             // call argument
        // closure
        
        void ceu_gc_inc (CEU_Value new) {
            if (new.type > CEU_VALUE_DYNAMIC) {
                new.Dyn->Any.refs++;
            }
        }
        
        void ceu_gc_dec (CEU_Value old, int chk) {
            if (old.type > CEU_VALUE_DYNAMIC) {
                old.Dyn->Any.refs--;
                if (chk) {
                    ceu_gc_chk(old);
                }
            }
        }
    """ +
    """ // BLOCK / FREE
        int ceu_tofree_n = 0;
        CEU_Dyn* ceu_tofree_dyn = NULL;
        
        void ceu_dyn_rem_free_chk (CEU_Dyn* dyn) {
        #if CEU >= 4
            if ((ceu_istask_dyn(dyn) CEU5(|| dyn->Any.type==CEU_VALUE_TASKS))&& ceu_tofree_n>0) {
                dyn->Any.tofree = ceu_tofree_dyn;
                ceu_tofree_dyn = dyn;
            } else
        #endif
            {
                ceu_dyn_rem_free(dyn);
            }
        }
        
        void ceu_dyn_rem_free (CEU_Dyn* dyn) {
            ceu_hold_rem(dyn);
            ceu_dyn_free(dyn);
        }

        void ceu_dyn_rem_free_glb (void) {
            if (ceu_tofree_n > 0) {
                return;
            }
            while (ceu_tofree_dyn != NULL) {
                CEU_Dyn* nxt = ceu_tofree_dyn->Any.tofree;
                ceu_dyn_rem_free(ceu_tofree_dyn);
                ceu_tofree_dyn = nxt;
            }
        }
        
        void ceu_dyn_free (CEU_Dyn* dyn) {
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
                case CEU_VALUE_EXE_CORO:
        #if CEU >= 4
                case CEU_VALUE_EXE_TASK:
        #endif
        #if CEU >= 5
                case CEU_VALUE_EXE_TASK_IN:
        #endif
                    free(dyn->Exe.mem);
                    break;
        #endif
        #if CEU >= 5
                case CEU_VALUE_TASKS:
                    ceu_dyns_free(&dyn->Tasks.dyns);
                    break;
                case CEU_VALUE_TRACK:
                    break;
        #endif
                default:
                    assert(0 && "bug found");
            }
            free(dyn);
        }
        
        void ceu_block_dump (CEU_Block* blk) {
            printf(">>> BLOCK: %p\n", blk);
            CEU_Dyn* cur = blk->dn.dyns.first;
            while (cur != NULL) {
                CEU_Value arg = ceu_dyn_to_val(cur);
                ceu_dump_f(NULL, 1, &arg);
                CEU_Dyn* old = cur;
                cur = old->Any.hld.next;
            }
        }
        void ceu_dyns_free (CEU_Dyns* dyns) {
            #if CEU >= 3
            // first finalize EXE
            CEU_Dyn* dyn = dyns->first;
            while (dyn != NULL) {
                if (dyn->Any.type==CEU_VALUE_EXE_CORO CEU4(|| ceu_istask_dyn(dyn))) { 
                    if (dyn->Exe.status < CEU_EXE_STATUS_TERMINATED) {
                        dyn->Exe.frame.clo->proto(&dyn->Exe.frame, CEU_ARG_ABORT, NULL);
                    }
                }
                #if CEU >= 5
                if (dyn->Any.type == CEU_VALUE_TASKS) {
                    CEU_Dyn* cur = dyn->Tasks.dyns.first;
                    while (cur != NULL) {
                        CEU_Dyn* nxt = cur->Any.hld.next;
                        if (cur->Exe.status < CEU_EXE_STATUS_TERMINATED) {
                            cur->Exe.frame.clo->proto(&cur->Exe.frame, CEU_ARG_ABORT, NULL);
                        }
                        cur = nxt;
                    }
                }
                #endif
                dyn = dyn->Any.hld.next;
            }
            #endif
            // then free mem
            {
                CEU_Dyn* dyn = dyns->first;
                while (dyn != NULL) {
                    CEU_Dyn* old = dyn;
                    dyn = old->Any.hld.next;
                    ceu_dyn_rem_free_chk(old);
                }
                dyns->first = NULL;
                dyns->last  = NULL;
            }
        }
    """ +
    """ // HOLD / DROP
        void ceu_hold_add (CEU_Dyn* dyn, CEU_Block* blk CEU5(COMMA CEU_Dyns* dyns)) {
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
        }
        void ceu_hold_rem (CEU_Dyn* dyn) {
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
            dyn->Any.hld.prev = NULL;
            dyn->Any.hld.next = NULL;
        }
        void ceu_hold_chg (CEU_Dyn* dyn, CEU_Block* blk CEU5(COMMA CEU_Dyns* dyns)) {
            ceu_hold_rem(dyn);
            ceu_hold_add(dyn, blk CEU5(COMMA dyns));
        }

        CEU_Value ceu_hold_chk_set (CEU4(int out COMMA) CEU_Block* dst_blk, CEU_HOLD dst_type, CEU_Value src, int nest, char* pre CEU4(COMMA int ylds)) {
            static char msg[256];
        #if CEU >= 4
            if (src.type==CEU_VALUE_REF && out) {   // out of non-yielding block
                strncpy(msg, pre, 256);
                strcat(msg, " : cannot copy reference out");
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error=msg} };
            } else
        #endif
            if (src.type < CEU_VALUE_DYNAMIC) {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
            
            CEU_Block* src_blk = CEU_HLD_BLOCK(src.Dyn);
            
        #if CEU >= 5
            if (
                src.Dyn->Any.type == CEU_VALUE_TRACK    &&
                src.Dyn->Track.task != NULL             &&
                CEU_HLD_BLOCK((CEU_Dyn*)src.Dyn->Track.task)->depth > dst_blk->depth
            ) {
                strncpy(msg, pre, 256);
                strcat(msg, " : cannot move track outside its task scope");
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error=msg} };
            } else 
        #endif
            if (src.Dyn->Any.hld.type == CEU_HOLD_FLEET) {
                if (src.Dyn->Any.refs-nest>0 && dst_blk->depth>src_blk->depth) {
                    strncpy(msg, pre, 256);
                    strcat(msg, " : cannot move to deeper scope with pending references");
                    return (CEU_Value) { CEU_VALUE_ERROR, {.Error=msg} }; // OK with CEU_HOLD_EVENT b/c never assigned
                } else {
                    // continue below
                }
            } else if (dst_blk==src_blk || dst_blk->depth>src_blk->depth) {
                return (CEU_Value) { CEU_VALUE_NIL };
            } else {
                strncpy(msg, pre, 256);
                strcat(msg, " : cannot copy reference to outer scope");
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error=msg} };
            };
            //printf(">>> %d %d -> %d\n", dst_blk->depth, src_blk->depth, src.Dyn->);

            int src_depth = src_blk->depth;
            int src_type  = src.Dyn->Any.hld.type;

            src.Dyn->Any.hld.type = MAX(src.Dyn->Any.hld.type,dst_type);
            if (dst_blk != CEU_HLD_BLOCK(src.Dyn)) {
        #if CEU >= 4
                assert((!ceu_istask_val(src) || src.Dyn->Exe.status!=CEU_EXE_STATUS_RESUMED) && "TODO: cannot move running task");
        #endif
                ceu_hold_chg(src.Dyn, dst_blk CEU5(COMMA &dst_blk->dn.dyns));
            }
            //printf(">>> %d -> %d\n", src_depth, src_blk->depth);
            if (src.Dyn->Any.hld.type==src_type && dst_blk->depth>=src_depth) {
                return (CEU_Value) { CEU_VALUE_NIL };
            }

            #define CEU_CHECK_ERROR_RETURN(v) { CEU_Value ret=v; if (ret.type==CEU_VALUE_ERROR) { return ret; } }

            switch (src.Dyn->Any.type) {
                case CEU_VALUE_CLO_FUNC:
        #if CEU >= 3
                case CEU_VALUE_CLO_CORO:
        #endif
                    for (int i=0; i<src.Dyn->Clo.upvs.its; i++) {
                        CEU_CHECK_ERROR_RETURN(ceu_hold_chk_set(CEU4(out COMMA) dst_blk, dst_type, src.Dyn->Clo.upvs.buf[i], 1, pre CEU4(COMMA ylds)));
                    }
                    break;
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<src.Dyn->Tuple.its; i++) {
                        CEU_CHECK_ERROR_RETURN(ceu_hold_chk_set(CEU4(out COMMA) dst_blk, dst_type, src.Dyn->Tuple.buf[i], 1, pre CEU4(COMMA ylds)));
                    }
                    break;
                case CEU_VALUE_VECTOR:
                    for (int i=0; i<src.Dyn->Vector.its; i++) {
                        CEU_CHECK_ERROR_RETURN(ceu_hold_chk_set(CEU4(out COMMA) dst_blk, dst_type, ceu_vector_get(&src.Dyn->Vector,i), 1, pre CEU4(COMMA ylds)));
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<src.Dyn->Dict.max; i++) {
                        CEU_CHECK_ERROR_RETURN(ceu_hold_chk_set(CEU4(out COMMA) dst_blk, dst_type, (*src.Dyn->Dict.buf)[i][0], 1, pre CEU4(COMMA ylds)));
                        CEU_CHECK_ERROR_RETURN(ceu_hold_chk_set(CEU4(out COMMA) dst_blk, dst_type, (*src.Dyn->Dict.buf)[i][1], 1, pre CEU4(COMMA ylds)));
                    }
                    break;
            #if CEU >= 2
                case CEU_VALUE_THROW:
                    CEU_CHECK_ERROR_RETURN(ceu_hold_chk_set(CEU4(out COMMA) dst_blk, dst_type, src.Dyn->Throw.val, 1, pre CEU4(COMMA ylds)));
                    CEU_CHECK_ERROR_RETURN(ceu_hold_chk_set(CEU4(out COMMA) dst_blk, dst_type, src.Dyn->Throw.stk, 1, pre CEU4(COMMA ylds)));
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
                    CEU_CHECK_ERROR_RETURN(ceu_hold_chk_set(CEU4(out COMMA) dst_blk, dst_type, ceu_dyn_to_val((CEU_Dyn*)src.Dyn->Exe.frame.clo), 1, pre CEU4(COMMA ylds)));
                    break;
        #endif
                default:
                    break; // not applicable
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        
        CEU_Value ceu_hold_chk_set_col (CEU_Dyn* col, CEU_Value v  CEU4(COMMA int ylds)) {
            if (v.type < CEU_VALUE_DYNAMIC) {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
            
            // col affects v:
            // [x,[1]] <-- moves v=[1] to v

            CEU_Value err = ceu_hold_chk_set(CEU4(1 COMMA) CEU_HLD_BLOCK(col), col->Any.hld.type, v, 0, "set error" CEU4(COMMA ylds));
            if (err.type==CEU_VALUE_ERROR && col->Any.hld.type!=CEU_HOLD_FLEET) {
                // must be second b/c chk_set above may modify v
                return err;
            }

            // v affects fleeting col with innermost scope
            if (col->Any.hld.type == CEU_HOLD_FLEET) {
                if (CEU_HLD_BLOCK(v.Dyn)->depth < CEU_HLD_BLOCK(col)->depth) {
                    return (CEU_Value) { CEU_VALUE_NIL };
                } else {
                    col->Any.hld.type = MAX(col->Any.hld.type, MIN(CEU_HOLD_FLEET,v.Dyn->Any.hld.type));
                    if (CEU_HLD_BLOCK(v.Dyn)->depth > CEU_HLD_BLOCK(col)->depth) {
                        ceu_hold_chg(col, CEU_HLD_BLOCK(v.Dyn) CEU5(COMMA CEU_HLD_DYNS(v.Dyn)));
                    }
                    return (CEU_Value) { CEU_VALUE_NIL };
                }
            } else {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
        }

        CEU_Value ceu_drop_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n == 1);
            CEU_Value src = args[0];
            CEU_Dyn* dyn = src.Dyn;
            
            // do not drop non-dyn or globals
            #if CEU >= 4
            if (src.type == CEU_VALUE_REF) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="drop error : value is not movable"} };
            } else
            #endif
            if (src.type < CEU_VALUE_DYNAMIC) {
                return (CEU_Value) { CEU_VALUE_NIL };
            } else if (CEU_HLD_BLOCK(dyn)->depth == 1) {
                return (CEU_Value) { CEU_VALUE_NIL };
            } else if (dyn->Any.hld.type == CEU_HOLD_FLEET) {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
            
            //printf(">>> %d\n", dyn->Any.refs);
            if (dyn->Any.hld.type == CEU_HOLD_IMMUT) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="drop error : value is not movable"} };
            }
            //if (dyn->Any.refs > 1) {
            //    return (CEU_Value) { CEU_VALUE_ERROR, {.Error="drop error : multiple references"} };
            //}
            dyn->Any.hld.type = CEU_HOLD_FLEET;
            //ceu_hold_chg(dyn, frame->up_block);

            switch (src.type) {
                case CEU_VALUE_CLO_FUNC:
        #if CEU >= 3
                case CEU_VALUE_CLO_CORO:
        #endif
                    for (int i=0; i<dyn->Clo.upvs.its; i++) {
                        CEU_Value ret = ceu_drop_f(frame, 1, &dyn->Clo.upvs.buf[i]);
                        if (ret.type == CEU_VALUE_ERROR) {
                            return ret;
                        }
                    }
                    break;
                case CEU_VALUE_TUPLE: {
                    for (int i=0; i<dyn->Tuple.its; i++) {
                        CEU_Value ret = ceu_drop_f(frame, 1, &dyn->Tuple.buf[i]);
                        if (ret.type == CEU_VALUE_ERROR) {
                            return ret;
                        }
                    }
                    break;
                }
                case CEU_VALUE_VECTOR: {
                    for (int i=0; i<dyn->Vector.its; i++) {
                        CEU_Value ret1 = ceu_vector_get(&dyn->Vector, i);
                        assert(ret1.type != CEU_VALUE_ERROR);
                        CEU_Value ret2 = ceu_drop_f(frame, 1, &ret1);
                        if (ret2.type == CEU_VALUE_ERROR) {
                            return ret2;
                        }
                    }
                    break;
                }
                case CEU_VALUE_DICT: {
                    for (int i=0; i<dyn->Dict.max; i++) {
                        CEU_Value ret0 = ceu_drop_f(frame, 1, &(*dyn->Dict.buf)[i][0]);
                        if (ret0.type == CEU_VALUE_ERROR) {
                            return ret0;
                        }
                        CEU_Value ret1 = ceu_drop_f(frame, 1, &(*dyn->Dict.buf)[i][1]);
                        if (ret1.type == CEU_VALUE_ERROR) {
                            return ret1;
                        }
                    }
                    break;
                }
        #if CEU >= 3
                case CEU_VALUE_EXE_CORO:
        #if CEU >= 4
                case CEU_VALUE_EXE_TASK:
        #endif
        #if CEU >= 5
                case CEU_VALUE_EXE_TASK_IN:
        #endif
                {
                    CEU_Value arg = ceu_dyn_to_val((CEU_Dyn*)dyn->Exe.frame.clo);
                    CEU_Value ret = ceu_drop_f(frame, 1, &arg);
                    if (ret.type == CEU_VALUE_ERROR) {
                        return ret;
                    }
                }
        #endif
                default:
                    break;
            }
            return (CEU_Value) { CEU_VALUE_NIL };;
        }        
    """ +
    """ // BCAST
    #if CEU >= 4
        CEU_Block* ceu_bcast_global (CEU_Block* blk) {
            if (blk->istop) {
                if (blk->up.frame->clo == NULL) {
                    return blk;
                } else if (blk->up.frame->clo->type == CEU_VALUE_CLO_FUNC) {
                    return ceu_bcast_global(blk->up.frame->up_block);
                } else {
                    return blk;     // outermost block in coro/task
                }
            } else if (blk->up.block != NULL) {
                return ceu_bcast_global(blk->up.block);
            } else {
                return blk;         // global scope
            }
        }
        
        CEU_Value ceu_bcast_blocks (CEU_Block* blk, CEU_Value evt);
        
        CEU_Value ceu_bcast_task (CEU_Exe_Task* task, CEU_Value evt) {
            CEU_Value ret = { CEU_VALUE_BOOL, {.Bool=1} };
            if (task->pc != 0) {    // not initial spawn
                ret = ceu_bcast_blocks(task->dn_block, evt);
            }
            if (task->status == CEU_EXE_STATUS_YIELDED) {
                if (CEU_ISERR(ret)) {
                    ret = task->frame.clo->proto(&task->frame, CEU_ARG_ERROR, &ret);
                } else {
                    ret = task->frame.clo->proto(&task->frame, 1, &evt);
                    if (task->status >= CEU_EXE_STATUS_TERMINATED) {
                        if (!CEU_ISERR(ret)) {      // bcast
                            CEU_Exe_Task* up_task = ceu_task_up_task(task);
                            CEU_Value evt2 = ceu_dyn_to_val((CEU_Dyn*)task);
                            if (up_task != NULL) {
                                // enclosing coro of enclosing block
                                ret = ceu_bcast_task(up_task, evt2);
                            } else { 
                                // enclosing block
                                ret = ceu_bcast_blocks(CEU_HLD_BLOCK((CEU_Dyn*)task), evt2);
                            }
                        }
        #if CEU >= 5
                        if (task->type == CEU_VALUE_EXE_TASK_IN) {
                            ceu_dyn_rem_free_chk((CEU_Dyn*)task);
                            if (!CEU_ISERR(ret)) {
                                ret = (CEU_Value) { CEU_VALUE_NIL };
                            }
                        }
        #endif
                    }
                }
            }
            return ret;
        }

        CEU_Value ceu_bcast_dyns (CEU_Dyns* dyns, CEU_Value evt) {
            CEU_Dyn* dyn = dyns->first;
            while (dyn != NULL) {
                switch (dyn->Any.type) {
                    case CEU_VALUE_EXE_TASK:
        #if CEU >= 5
                    case CEU_VALUE_EXE_TASK_IN:
        #endif
                    {
                        CEU_Value ret = ceu_bcast_task(&dyn->Exe_Task, evt); 
                        if (CEU_ISERR(ret)) {
                            return ret;
                        }
                        if (dyn->Exe_Task.status == CEU_EXE_STATUS_ABORTED) {
                            return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
                        }
                        break;
                    }
        #if CEU >= 5
                    case CEU_VALUE_TASKS: {
                        CEU_Value ret = ceu_bcast_dyns(&dyn->Tasks.dyns, evt);
                        if (CEU_ISERR(ret)) {
                            return ret;
                        }
                        break;
                    }
                    case CEU_VALUE_TRACK:
                        if (ceu_istask_val(evt) && dyn->Track.task==(CEU_Exe_Task*)evt.Dyn) {
                            dyn->Track.task = NULL; // tracked coro is terminating
                        }
                        break;
        #endif
                    default:
                        break; // not applicable
                }
                dyn = dyn->Any.hld.next;
            }
            return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
        }
        CEU_Value ceu_bcast_blocks (CEU_Block* blk, CEU_Value evt) {
            while (blk != NULL) {
                CEU_Value ret = ceu_bcast_dyns(&blk->dn.dyns, evt);
                if (CEU_ISERR(ret)) {
                    return ret;
                }
                blk = blk->dn.block;
            }
            return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
        }

        CEU_Value ceu_broadcast_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n >= 1);
            CEU_Value evt = args[0];
            CEU_Value ret;
            ceu_tofree_n++;
            if (n == 1) {
                ret = ceu_bcast_blocks(ceu_bcast_global(frame->up_block), ceu_toref(evt));
            } else {
                CEU_Value tsk = args[1];
                if (ceu_istask_val(tsk)) {
                    ret = ceu_bcast_task(&tsk.Dyn->Exe_Task, ceu_toref(evt));
                } else {
                    ret = (CEU_Value) { CEU_VALUE_ERROR, {.Error="expected task"} };
                }
            }
            ceu_tofree_n--;
            ceu_dyn_rem_free_glb();
            return ret;
        }        
    #endif
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
                case CEU_VALUE_CLO_FUNC:
        #if CEU >= 3
                case CEU_VALUE_CLO_CORO:
        #endif
                case CEU_VALUE_TUPLE:
                case CEU_VALUE_VECTOR:
                case CEU_VALUE_DICT:
        #if CEU >= 3
                case CEU_VALUE_EXE_CORO:
        #endif
                    return ceu_sizeof(CEU_Value, Dyn);
                default:
                    assert(0 && "bug found");
            }
        }
        
        CEU_Value ceu_tuple_set (CEU_Tuple* tup, int i, CEU_Value v CEU4(COMMA int ylds)) {
            ceu_gc_inc(v);
            ceu_gc_dec(tup->buf[i], 1);
            tup->buf[i] = v;
            return ceu_hold_chk_set_col((CEU_Dyn*)tup, v CEU4(COMMA ylds));
        }
        
        CEU_Value ceu_vector_get (CEU_Vector* vec, int i) {
            if (i<0 || i>=vec->its) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="index error : out of bounds"} };
            }
            int sz = ceu_tag_to_size(vec->unit);
            CEU_Value ret = (CEU_Value) { vec->unit };
            memcpy(&ret.Number, vec->buf+i*sz, sz);
            return ret;
        }
        
        CEU_Value ceu_vector_set (CEU_Vector* vec, int i, CEU_Value v CEU4(COMMA int ylds)) {
            if (v.type == CEU_VALUE_NIL) {           // pop
                assert(i == vec->its-1);
                CEU_Value ret = ceu_vector_get(vec, i);
                assert(ret.type != CEU_VALUE_ERROR);
                ceu_gc_dec(ret, 1);
                vec->its--;
                return ret;
            } else {
                CEU_Value err = ceu_hold_chk_set_col((CEU_Dyn*)vec, v CEU4(COMMA ylds));
                if (err.type == CEU_VALUE_ERROR) {
                    return err;
                }
                if (vec->its == 0) {
                    vec->unit = v.type;
                } else {
                    assert(v.type == vec->unit);
                }
                int sz = ceu_tag_to_size(vec->unit);
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
                    ceu_gc_dec(ret, 1);
                    assert(i < vec->its);
                }
                memcpy(vec->buf + i*sz, (char*)&v.Number, sz);
                return (CEU_Value) { CEU_VALUE_NIL };
            }
        }
        
        CEU_Value ceu_vector_from_c_string (CEU_Block* hld, const char* str) {
            CEU_Value vec = ceu_create_vector(hld);
            int N = strlen(str);
            for (int i=0; i<N; i++) {
                assert(ceu_vector_set(&vec.Dyn->Vector, vec.Dyn->Vector.its, (CEU_Value) { CEU_VALUE_CHAR, {.Char=str[i]} } CEU4(COMMA 0)).type != CEU_VALUE_ERROR);
            }
            return vec;
        }

        CEU_Value ceu_next_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n==1 || n==2);
            CEU_Value col = CEU4(ceu_deref)(args[0]);
            CEU_Value key = (n == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : CEU4(ceu_deref)(args[1]);
            switch (col.type) {
                case CEU_VALUE_DICT: {
                    if (key.type == CEU_VALUE_NIL) {
                        return (*col.Dyn->Dict.buf)[0][0];
                    }
                    for (int i=0; i<col.Dyn->Dict.max-1; i++) {     // -1: last element has no next
                        CEU_Value args[] = { key, (*col.Dyn->Dict.buf)[i][0] };
                        CEU_Value ret = ceu_op_equals_equals_f(NULL, 2, args);
                        assert(ret.type != CEU_VALUE_ERROR);
                        if (ret.Bool) {
                            return (*col.Dyn->Dict.buf)[i+1][0];
                        }
                    }
                    return (CEU_Value) { CEU_VALUE_NIL };
                }
        #if CEU >= 5
                case CEU_VALUE_TASKS: {
                    CEU_Dyn* nxt = NULL;
                    switch (key.type) {
                        case CEU_VALUE_NIL:
                            nxt = col.Dyn->Tasks.dyns.first;
                            break;
                        case CEU_VALUE_TRACK:
                            if (key.Dyn->Track.task==NULL || key.Dyn->Track.task->type!=CEU_VALUE_EXE_TASK_IN) {
                                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="next error : expected task in pool track"} };
                            }
                            nxt = key.Dyn->Track.task->hld.next;
                            break;
                        default:
                            return (CEU_Value) { CEU_VALUE_ERROR, {.Error="next error : expected task in pool track"} };
                    }
                    if (nxt == NULL) {
                        return (CEU_Value) { CEU_VALUE_NIL };
                    } else {
                        return ceu_create_track(frame->up_block, &nxt->Exe_Task);
                    }
                }
        #endif
                default:
                    return (CEU_Value) { CEU_VALUE_ERROR, {.Error="next error : expected collection"} };
            }                    
        }        
        int ceu_dict_key_to_index (CEU_Dict* col, CEU_Value key, int* idx) {
            *idx = -1;
            for (int i=0; i<col->max; i++) {
                CEU_Value cur = (*col->buf)[i][0];
                CEU_Value args[] = { key, cur };
                CEU_Value ret = ceu_op_equals_equals_f(NULL, 2, args);
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
        CEU_Value ceu_dict_set (CEU_Dict* col, CEU_Value key, CEU_Value val CEU4(COMMA int ylds)) {
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
                ceu_gc_dec(vv, 1);
                ceu_gc_dec(key, 1);
                (*col->buf)[old][0] = (CEU_Value) { CEU_VALUE_NIL };
                return (CEU_Value) { CEU_VALUE_NIL };
            } else {
                CEU_Value err1 = ceu_hold_chk_set_col((CEU_Dyn*)col, key CEU4(COMMA ylds));
                if (err1.type == CEU_VALUE_ERROR) {
                    return err1;
                }
                CEU_Value err2 = ceu_hold_chk_set_col((CEU_Dyn*)col, val CEU4(COMMA ylds));
                if (err2.type == CEU_VALUE_ERROR) {
                    return err2;
                }

                ceu_gc_inc(val);
                ceu_gc_dec(vv, 1);
                if (vv.type == CEU_VALUE_NIL) {
                    ceu_gc_inc(key);
                }
                (*col->buf)[old][0] = key;
                (*col->buf)[old][1] = val;
                return (CEU_Value) { CEU_VALUE_NIL };
            }
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
            CEU_Tuple* ret = malloc(sizeof(CEU_Tuple) + n*sizeof(CEU_Value));
            assert(ret != NULL);
            *ret = (CEU_Tuple) {
                CEU_VALUE_TUPLE, 0, NULL, NULL, { CEU_HOLD_FLEET, blk, NULL, NULL },
                n, {}
            };
            memset(ret->buf, 0, n*sizeof(CEU_Value));
            ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
            return (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ret} };
        }
        
        CEU_Value ceu_tuple_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n==1 && args[0].type==CEU_VALUE_NUMBER);
            return ceu_create_tuple(frame->up_block, args[0].Number);
        }
        
        CEU_Value ceu_create_vector (CEU_Block* blk) {
            CEU_Vector* ret = malloc(sizeof(CEU_Vector));
            assert(ret != NULL);
            char* buf = malloc(1);  // because of '\0' in empty strings
            assert(buf != NULL);
            buf[0] = '\0';
            *ret = (CEU_Vector) {
                CEU_VALUE_VECTOR, 0,  NULL, NULL, { CEU_HOLD_FLEET, blk, NULL, NULL },
                0, 0, CEU_VALUE_NIL, buf
            };
            ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
            return (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=(CEU_Dyn*)ret} };
        }
        
        CEU_Value ceu_create_dict (CEU_Block* blk) {
            CEU_Dict* ret = malloc(sizeof(CEU_Dict));
            assert(ret != NULL);
            *ret = (CEU_Dict) {
                CEU_VALUE_DICT, 0, NULL, NULL, { CEU_HOLD_FLEET, blk, NULL, NULL },
                0, NULL
            };
            ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
            return (CEU_Value) { CEU_VALUE_DICT, {.Dyn=(CEU_Dyn*)ret} };
        }
        
        CEU_Value _ceu_create_clo_ (int sz, int type, CEU_Block* blk, CEU_HOLD hld_type, CEU_Frame* frame, CEU_Proto proto, int upvs) {
            CEU_Clo* ret = malloc(sz);
            assert(ret != NULL);
            CEU_Value* buf = malloc(upvs * sizeof(CEU_Value));
            assert(buf != NULL);
            for (int i=0; i<upvs; i++) {
                buf[i] = (CEU_Value) { CEU_VALUE_NIL };
            }
            *ret = (CEU_Clo) {
                type, 0, NULL, NULL, { hld_type, blk, NULL, NULL },
                frame, proto, { upvs, buf }
            };
            ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
            return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
        }

        CEU_Value ceu_create_clo (int type, CEU_Block* blk, CEU_HOLD hld_type, CEU_Frame* frame, CEU_Proto proto, int upvs) {
            return _ceu_create_clo_(sizeof(CEU_Clo), type, blk, hld_type, frame, proto, upvs);
        }

        #if CEU >= 3
        CEU_Value ceu_create_clo_exe (int type, CEU_Block* blk, CEU_HOLD hld_type, CEU_Frame* frame, CEU_Proto proto, int upvs) {
            CEU_Value clo = _ceu_create_clo_(sizeof(CEU_Clo_Exe), type, blk, hld_type, frame, proto, upvs);
            clo.Dyn->Clo_Exe.mem_n = 0;
            return clo;
        }
        #endif

        #if CEU >= 3
        CEU_Value _ceu_create_exe_ (int type, int sz, CEU_Block* blk, CEU_Value clo CEU5(COMMA CEU_Dyns* dyns)) {
            assert(clo.type==CEU_VALUE_CLO_CORO CEU4(|| clo.type==CEU_VALUE_CLO_TASK));
            ceu_gc_inc(clo);
            
            CEU_Exe* ret = malloc(sz);
            assert(ret != NULL);
            char* mem = malloc(clo.Dyn->Clo_Exe.mem_n);
            assert(mem != NULL);
            
            int hld_type = (clo.Dyn->Clo.hld.type <= CEU_HOLD_MUTAB) ? CEU_HOLD_FLEET : clo.Dyn->Clo.hld.type;
            *ret = (CEU_Exe) {  // refs=1 b/c of ref in block for defers
                type, 1, NULL, NULL, { hld_type, blk, NULL, NULL },
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
            if (tasks->max==0 || ceu_tasks_n(tasks)<tasks->max) {
                CEU_Value ret = _ceu_create_exe_task_(CEU_VALUE_EXE_TASK_IN, blk, clo, &tasks->dyns);
                if (ret.type == CEU_VALUE_EXE_TASK_IN) {
                    ret.Dyn->Any.hld.block = (CEU_Block*) tasks; // point to tasks (vs enclosing block)
                }
                return ret;
            } else {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
        }
        CEU_Value ceu_create_tasks (CEU_Block* blk, int max) {
            CEU_Tasks* ret = malloc(sizeof(CEU_Tasks));
            assert(ret != NULL);

            *ret = (CEU_Tasks) {
                CEU_VALUE_TASKS, 1, NULL, NULL, { CEU_HOLD_FLEET, blk, NULL, NULL },
                max, { NULL, NULL }
            };
            
            ceu_hold_add((CEU_Dyn*)ret, blk, &blk->dn.dyns);
            return (CEU_Value) { CEU_VALUE_TASKS, {.Dyn=(CEU_Dyn*)ret} };
        }
        CEU_Value ceu_create_track (CEU_Block* blk, CEU_Exe_Task* task) {
            CEU_Track* ret = malloc(sizeof(CEU_Track));
            assert(ret != NULL);
            *ret = (CEU_Track) {
                CEU_VALUE_TRACK, 0, NULL, NULL, { CEU_HOLD_FLEET, blk, NULL, NULL },
                task
            };
            ceu_hold_add((CEU_Dyn*)ret, blk, &blk->dn.dyns);
            return (CEU_Value) { CEU_VALUE_TRACK, {.Dyn=(CEU_Dyn*)ret} };
        }
        
        CEU_Value ceu_tasks_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n <= 1);
            int max = 0;
            if (n == 1) {
                CEU_Value xmax = args[0];
                if (xmax.type!=CEU_VALUE_NUMBER || xmax.Number<=0) {                
                    return (CEU_Value) { CEU_VALUE_ERROR, {.Error="tasks error : expected positive number"} };
                }
                max = xmax.Number;
            }
            return ceu_create_tasks(frame->up_block, max);
        }
        #endif
    """ +
    """ // PRINT
        void ceu_print1 (CEU_Frame* _1, CEU_Value v) {
        #if CEU >= 4
            v = ceu_deref(v);
        #endif
            // no tags when _1==NULL (ceu_error_list_print)
            if (_1!=NULL && v.type>CEU_VALUE_DYNAMIC) {  // TAGS
                CEU_Value tup = ceu_tags_f(_1, 1, &v);
                assert(tup.type != CEU_VALUE_ERROR);
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
                ceu_dyn_rem_free(tup.Dyn);
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
        #if CEU >= 4
                case CEU_VALUE_REF:
                    assert(0 && "bug found");
                    break;
        #endif
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
        CEU_Value ceu_print_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            for (int i=0; i<n; i++) {
                if (i > 0) {
                    printf("\t");
                }
                ceu_print1(_1, args[i]);
            }
            return (CEU_Value) { CEU_VALUE_NIL };
        }
        CEU_Value ceu_println_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            ceu_print_f(_1, n, args);
            printf("\n");
            return (CEU_Value) { CEU_VALUE_NIL };
        }
    """ +
    """
        // EQ / NEQ / LEN
        CEU_Value ceu_op_equals_equals_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n == 2);
            CEU_Value e1 = CEU4(ceu_deref)(args[0]);
            CEU_Value e2 = CEU4(ceu_deref)(args[1]);
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
            #if CEU >= 4
                    case CEU_VALUE_REF:
                        assert(0 && "bug found");
                        break;
            #endif
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
        CEU_Value ceu_op_slash_equals_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            CEU_Value ret = ceu_op_equals_equals_f(_1, n, args);
            ret.Bool = !ret.Bool;
            return ret;
        }
        
        CEU_Value ceu_op_hash_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n == 1);
            CEU_Value v = CEU4(ceu_deref)(args[0]);
            if (v.type == CEU_VALUE_VECTOR) {
                return (CEU_Value) { CEU_VALUE_NUMBER, {.Number=v.Dyn->Vector.its} };
            } else if (v.type == CEU_VALUE_TUPLE) {
                return (CEU_Value) { CEU_VALUE_NUMBER, {.Number=v.Dyn->Tuple.its} };
            } else {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="length error : not a vector"} };
            }
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
                CEU_VALUE_THROW, 0, NULL, NULL, { CEU_HOLD_FLEET, blk, NULL, NULL },
                val, stk
            };
            
            ceu_hold_add((CEU_Dyn*)ret, blk CEU5(COMMA &blk->dn.dyns));
            assert(ceu_hold_chk_set_col((CEU_Dyn*)ret, val CEU4(COMMA 1)).type != CEU_VALUE_ERROR);
            assert(ceu_hold_chk_set_col((CEU_Dyn*)ret, stk CEU4(COMMA 1)).type != CEU_VALUE_ERROR);
            
            return (CEU_Value) { CEU_VALUE_THROW, {.Dyn=(CEU_Dyn*)ret} };
        }

        CEU_Value ceu_throw_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n == 1);
            return _ceu_throw_(frame->up_block, args[0]);
        }

        CEU_Value ceu_pointer_to_string (CEU_Block* blk, const char* ptr) {
            CEU_Value str = ceu_create_vector(blk);
            int len = strlen(ptr);
            for (int i=0; i<len; i++) {
                CEU_Value chr = { CEU_VALUE_CHAR, {.Char=ptr[i]} };
                ceu_vector_set(&str.Dyn->Vector, i, chr CEU4(COMMA 1));
            }
            return str;
        }

        CEU_Value ceu_pointer_dash_to_dash_string_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n == 1);
            assert(args[0].type == CEU_VALUE_POINTER);
            return ceu_pointer_to_string(frame->up_block, args[0].Pointer);
        }
        #endif
    """ +
    """ // COROUTINE / STATUS
        #if CEU >= 3
        CEU_Value ceu_coroutine_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n == 1);
            CEU_Value coro = CEU4(ceu_deref)(args[0]);
            if (coro.type != CEU_VALUE_CLO_CORO) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="coroutine error : expected coro"} };
            }
            return _ceu_create_exe_(CEU_VALUE_EXE_CORO, sizeof(CEU_Exe), frame->up_block, coro CEU5(COMMA &frame->up_block->dn.dyns));
        }
        CEU_Value ceu_status_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n == 1);
            CEU_Value exe = CEU4(ceu_deref)(args[0]);
            if (exe.type!=CEU_VALUE_EXE_CORO CEU4(&& !ceu_istask_val(exe))) {
        #if CEU < 4
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="status error : expected running coroutine"} };
        #else
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="status error : expected running coroutine or task"} };
        #endif
            }
            return (CEU_Value) { CEU_VALUE_TAG, {.Tag=exe.Dyn->Exe.status + CEU_TAG_yielded - 1} };
        }
        #endif
    """ +
    """ // ISTASK / PUB
        #if CEU >= 4
        CEU_Exe_Task* ceu_block_up_task (CEU_Block* blk) {
            if (blk->istop) {
                CEU_Exe_Task* up_task = blk->up.frame->exe_task;
                return (up_task!=NULL && ceu_istask_dyn((CEU_Dyn*)up_task)) ? up_task : NULL;                
            } else if (blk->up.block == NULL) {
                return NULL;
            } else {
                return ceu_block_up_task(blk->up.block);
            }
        }

        CEU_Exe_Task* ceu_task_up_task (CEU_Exe_Task* task) {
            return ceu_block_up_task(task->frame.up_block);
        }
        
        CEU_Value ceu_toref (CEU_Value v) {
            return (v.type < CEU_VALUE_DYNAMIC) ? v : (CEU_Value) { CEU_VALUE_REF, {.Dyn=v.Dyn} };
        }
        CEU_Value ceu_deref (CEU_Value v) {
            return (v.type == CEU_VALUE_REF) ? ceu_dyn_to_val(v.Dyn) : v;
        }
        
        int ceu_istask_dyn (CEU_Dyn* dyn) {
            return dyn->Any.type==CEU_VALUE_EXE_TASK CEU5(|| dyn->Any.type==CEU_VALUE_EXE_TASK_IN);
        }

        int ceu_istask_val (CEU_Value val) {
        #if CEU >= 4
            val = ceu_deref(val);
        #endif
            return (val.type>CEU_VALUE_DYNAMIC) && ceu_istask_dyn(val.Dyn);
        }

        CEU_Value ceu_pub_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            int i = 0;
            CEU_Exe_Task* tsk;
            if (n>0 && ceu_istask_val(CEU4(ceu_deref)(args[0]))) {
                tsk = &CEU4(ceu_deref)(args[0]).Dyn->Exe_Task;
                i = 1;
            } else {
                tsk = ceu_block_up_task(frame->up_block);
                if (tsk == NULL) {
                    return (CEU_Value) { CEU_VALUE_ERROR, {.Error="pub error : expected task"} };
                }
            }
            if (i == n) {   // GET
                return tsk->pub;
            } else {        // SET
                CEU_Value ret = ceu_hold_chk_set(CEU4(1 COMMA) tsk->dn_block, CEU_HOLD_MUTAB, args[i], 0, "set error", 1);
                if (ret.type == CEU_VALUE_ERROR) {
                    return ret;
                }
                ceu_gc_inc(args[i]);
                ceu_gc_dec(tsk->pub, 1);
                tsk->pub = args[i];
                return (CEU_Value) { CEU_VALUE_NIL };
            }
        }
        #endif
    """ +
    """ // TRACK
        #if CEU >= 5
        CEU_Value ceu_track_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n == 1);
            CEU_Value task = CEU4(ceu_deref)(args[0]);
            if (!ceu_istask_val(task)) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="track error : expected task"} };
            } else if (task.Dyn->Exe_Task.status >= CEU_EXE_STATUS_TERMINATED) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="track error : expected unterminated task"} };
            }
            //CEU_Block* blk = (ceu_depth(task->Dyn->up_dyns.dyns->up_block) > ceu_depth(frame->up_block)) ?
            //    task->Dyn->up_dyns.dyns->up_block : frame->up_block;
            return ceu_create_track(frame->up_block, (CEU_Exe_Task*)task.Dyn);
        }
        #endif
    """ +
    """ // GLOBALS
        int CEU_BREAK = 0;
        CEU_Block _ceu_block_ = { 0, 0, {.block=NULL}, { CEU4(NULL COMMA) {NULL,NULL} } };
        CEU_Frame _ceu_frame_ = { &_ceu_block_, NULL CEU3(COMMA {.exe=NULL}) };
        CEU_Frame* ceu_frame = &_ceu_frame_;

        CEU_Clo ceu_dump = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_dump_f, {0,NULL}
        };
        CEU_Clo ceu_error = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_error_f, {0,NULL}
        };
        CEU_Clo ceu_next = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_next_f, {0,NULL}
        };
        CEU_Clo ceu_print = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_print_f, {0,NULL}
        };
        CEU_Clo ceu_println = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_println_f, {0,NULL}
        };
        CEU_Clo ceu_sup_question_ = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_sup_question__f, {0,NULL}
        };
        CEU_Clo ceu_tags = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_tags_f, {0,NULL}
        };
        CEU_Clo ceu_tuple = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_tuple_f, {0,NULL}
        };
        CEU_Clo ceu_type = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_type_f, {0,NULL}
        };
        CEU_Clo ceu_op_equals_equals = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_op_equals_equals_f, {0,NULL}
        };
        CEU_Clo ceu_op_hash = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_op_hash_f, {0,NULL}
        };
        CEU_Clo ceu_op_slash_equals = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_op_slash_equals_f, {0,NULL}
        };
        CEU_Clo ceu_string_dash_to_dash_tag = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_string_dash_to_dash_tag_f, {0,NULL}
        };
        #if CEU >= 2
        CEU_Clo ceu_pointer_dash_to_dash_string = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_pointer_dash_to_dash_string_f, {0,NULL}
        };
        CEU_Clo ceu_throw = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_throw_f, {0,NULL}
        };
        #endif
        #if CEU >= 3
        CEU_Clo ceu_coroutine = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_coroutine_f, {0,NULL}
        };
        CEU_Clo ceu_status = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_status_f, {0,NULL}
        };
        #endif
        #if CEU >= 4
        CEU_Clo ceu_broadcast = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_broadcast_f, {0,NULL}
        };
        CEU_Clo ceu_pub = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_pub_f, {0,NULL}
        };
        #endif
        #if CEU >= 5
        CEU_Clo ceu_tasks = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_tasks_f, {0,NULL}
        };
        CEU_Clo ceu_track = { 
            CEU_VALUE_CLO_FUNC, 1, NULL, NULL, { CEU_HOLD_MUTAB, &_ceu_block_, NULL, NULL },
            &_ceu_frame_, ceu_track_f, {0,NULL}
        };
        #endif

        CEU_Value id_dump                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_dump}                    };
        CEU_Value id_error                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_error}                   };
        CEU_Value id_next                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_next}                    };
        CEU_Value id_print                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_print}                   };
        CEU_Value id_println                 = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_println}                 };
        CEU_Value id_tags                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_tags}                    };
        CEU_Value id_type                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_type}                    };
        CEU_Value id_tuple                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_tuple}                   };
        CEU_Value op_hash                    = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_op_hash}                 };
        CEU_Value id_sup_question_           = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_sup_question_}           };
        CEU_Value op_equals_equals           = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_op_equals_equals}        };
        CEU_Value op_slash_equals            = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_op_slash_equals}         };
        CEU_Value id_string_dash_to_dash_tag = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_string_dash_to_dash_tag} };
        #if CEU >= 2
        CEU_Value id_pointer_dash_to_dash_string = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_pointer_dash_to_dash_string} };
        CEU_Value id_throw                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_throw}                   };
        #endif
        #if CEU >= 3
        CEU_Value id_coroutine               = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_coroutine}               };
        CEU_Value id_status                  = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_status}                  };
        #endif
        #if CEU >= 4
        CEU_Value id_broadcast               = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_broadcast}               };
        CEU_Value id_pub                     = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_pub}                     };
        #endif
        #if CEU >= 5
        CEU_Value id_tasks                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_tasks}                   };
        CEU_Value id_track                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_track}                   };
        #endif
    """ +
    """ // MAIN
        int main (int ceu_argc, char** ceu_argv) {
            assert(CEU_TAG_nil == CEU_VALUE_NIL);
            CEU_Value ceu_acc;
            ${this.code}
            return 0;
        }
    """)
}
