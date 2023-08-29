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

        #if CEU >= 3
        #define CEU_CHECK_FREE() {                  \
            if (ceu_n == CEU_ARG_FREE) {            \
                continue;                           \
            }                                       \
        }
        #endif

        typedef enum CEU_HOLD {
            CEU_HOLD_FLEET = 0,     // not assigned, dst assigns
            CEU_HOLD_MUTAB,         // set and assignable to narrow 
            CEU_HOLD_IMMUT,         // set but not assignable (nested fun)
            CEU_HOLD_MAX
        } __attribute__ ((__packed__)) CEU_HOLD;
        _Static_assert(sizeof(CEU_HOLD) == 1);
        
        typedef enum CEU_ARG {
            CEU_ARG_FREE = -1,
            CEU_ARG_ARGS =  0   // 1, 2, ...
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
            CEU_VALUE_TUPLE,
            CEU_VALUE_VECTOR,
            CEU_VALUE_DICT,
        #if CEU >= 2
            CEU_VALUE_THROW,
        #endif
        #if CEU >= 3
            CEU_VALUE_EXE_CORO,
        #endif
            CEU_VALUE_MAX
        } __attribute__ ((__packed__)) CEU_VALUE;
        _Static_assert(sizeof(CEU_VALUE) == 1);
        
        #if CEU >= 3
        typedef enum CEU_EXE_STATUS {
            CEU_EXE_STATUS_YIELDED = 1,
            //CEU_EXE_STATUS_TOGGLED,
            CEU_EXE_STATUS_RESUMED,
            CEU_EXE_STATUS_TERMINATED
        } CEU_EXE_STATUS;
        #endif
    """ +
    """ // CEU_Frame, CEU_Block
        typedef struct CEU_Frame {          // call func / create task
            struct CEU_Block*    up_block;  // block enclosing this call/coroutine
            struct CEU_Clo*      clo;
        #if CEU >= 3
            struct CEU_Exe_Coro* exe;       // coro/task<->frame point to each other
        #endif
        } CEU_Frame;

        typedef struct CEU_Block {
            uint16_t depth;
            uint8_t  istop;
            union {
                struct CEU_Frame* frame;    // istop = 1
                struct CEU_Block* block;    // istop = 0
            } up;
            union CEU_Dyn* dyns;            // list of allocated data to bcast/free
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
            CEU_HOLD hld_type;              \
            CEU_HOLD hld_depth;             \
            union CEU_Dyn** hld_prev;       \
            union CEU_Dyn* hld_next;        \
            struct CEU_Tags_List* tags;
            
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

        typedef struct CEU_Clo {  // lexical func/coro/task
            _CEU_Dyn_
            struct CEU_Frame* up_frame;   // points to active frame
            CEU_Proto proto;
            struct {
                int its;     // number of upvals
                CEU_Value* buf;
            } upvs;
            union {
            #if CEU >= 3
                struct {
                    int n_mem;  // space for locals
                } Exe;
            #endif
            };
        } CEU_Clo;
        
        #if CEU >= 2
        typedef struct CEU_Throw {
            _CEU_Dyn_
            CEU_Value val;
            CEU_Value stk;
        } CEU_Throw;
        #endif
        
        #if CEU >= 3
        typedef struct CEU_Exe_Coro {
            _CEU_Dyn_
            CEU_EXE_STATUS status;
            struct CEU_Frame frame;
            int pc;
            char* mem;
        } CEU_Exe_Coro;
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
        #if CEU >= 3
            struct CEU_Exe_Coro Coro;
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
                
        void ceu_dyn_free (CEU_Dyn* dyn);
        void ceu_block_free (CEU_Block* blk);
        
        void ceu_gc_inc (CEU_Value v);
        void ceu_gc_dec (CEU_Value v, int chk);

        void ceu_hold_add (CEU_Dyn* dyn, CEU_Dyn** blk);
        void ceu_hold_rem (CEU_Dyn* dyn);

        int ceu_hold_set (CEU_Dyn** dst, int depth, CEU_HOLD hld_type, CEU_Dyn* src);
        
        CEU_Value ceu_tuple_create   (CEU_Block* hld, int n);
        CEU_Value ceu_vector_create  (CEU_Block* hld);
        CEU_Value ceu_dict_create    (CEU_Block* hld);
        CEU_Value ceu_clo_create     (int type, CEU_Block* hld, CEU_HOLD hld_type, CEU_Frame* frame, CEU_Proto proto, int upvs);

        int ceu_tuple_set (CEU_Tuple* tup, int i, CEU_Value v);

        CEU_Value ceu_vector_get (CEU_Vector* vec, int i);
        int ceu_vector_set (CEU_Vector* vec, int i, CEU_Value v);
        CEU_Value ceu_vector_from_c_string (CEU_Block* hld, const char* str);
        
        int ceu_dict_key_to_index (CEU_Dict* col, CEU_Value key, int* idx);
        CEU_Value ceu_dict_get (CEU_Dict* col, CEU_Value key);
        int ceu_dict_set (CEU_Dict* col, CEU_Value key, CEU_Value val);
        
        CEU_Value ceu_col_check (CEU_Value col, CEU_Value idx);

        void ceu_print1 (CEU_Frame* _1, CEU_Value v);
        CEU_Value ceu_op_equals_equals_f (CEU_Frame* _1, int n, CEU_Value args[]);

        #if CEU >= 2
        CEU_Value ceu_pointer_dash_to_dash_string_f (CEU_Frame* frame, int n, CEU_Value args[]);
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
        void ceu_exit (CEU_Block* blk) {
            if (blk == NULL) {
                exit(0);
            }
            CEU_Block* up = (blk->istop) ? blk->up.frame->up_block : blk->up.block;
            ceu_block_free(blk);
            return ceu_exit(up);
        }
        void _ceu_error_ (CEU_Block* blk, char* pre, CEU_Value err) {
            fprintf(stderr, "%s : %s\n", pre, err.Error);
            ceu_exit(blk);
        }
        CEU_Value ceu_assert (CEU_Block* blk, CEU_Value err, char* pre) {
            if (err.type == CEU_VALUE_ERROR) {
                _ceu_error_(blk, pre, err);
            }
            return err;
        }
        CEU_Value ceu_error_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n==1 && args[0].type==CEU_VALUE_TAG);
            return (CEU_Value) { CEU_VALUE_ERROR, {.Error=ceu_tag_to_string(args[0].Tag)} };
        }
        
        #if CEU <= 1
        #define CEU_ERROR(blk,pre,err)  _ceu_error_(blk,pre,err)
        #define CEU_ASSERT(blk,err,pre) ceu_assert(blk,err,pre)
        #else
        #define CEU_ERROR(blk,pre,err) {                       \
            if (err.type == CEU_VALUE_THROW) {                  \
                ceu_acc = err;                                  \
            } else {                                            \
                ceu_acc = _ceu_throw_(blk, err);                \
            }                                                   \
            CEU_Value ceu_str = ceu_pointer_to_string(blk,pre); \
            assert(ceu_vector_set(&ceu_acc.Dyn->Throw.stk.Dyn->Vector, ceu_acc.Dyn->Throw.stk.Dyn->Vector.its, ceu_str)); \
            continue;                                           \
        }
        #define CEU_ASSERT(blk,err,pre) ({      \
            CEU_Value ceu_err = err;            \
            if (ceu_err.type==CEU_VALUE_ERROR || ceu_err.type==CEU_VALUE_THROW) {  \
                CEU_ERROR(blk,pre,ceu_err);     \
            };                                  \
            ceu_err;                            \
        })
        #endif
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
                printf("    hold  = %d\n", v.Dyn->Any.hld_type);
                printf("    depth = %d\n", v.Dyn->Any.hld_depth);
                printf("    prev  = %p\n", v.Dyn->Any.hld_prev);
                printf("    &next = %p\n", &v.Dyn->Any.hld_next);
                printf("    next  = %p\n", v.Dyn->Any.hld_next);
            }
            puts("<<<<<<<<<<<");
        }
        CEU_Value ceu_dump_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n == 1);
            _ceu_dump_(args[0]);
        }

        int ceu_as_bool (CEU_Value v) {
            return !(v.type==CEU_VALUE_NIL || (v.type==CEU_VALUE_BOOL && !v.Bool));
        }
        CEU_Value ceu_type_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n == 1 && "bug found");
            return (CEU_Value) { CEU_VALUE_TAG, {.Tag=args[0].type} };
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
            CEU_Value dyn = args[0];
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
                    CEU_Value tup = ceu_tuple_create(frame->up_block, len);
                    {
                        CEU_Tags_List* cur = tags;
                        int i = 0;
                        while (cur != NULL) {
                            assert(ceu_tuple_set(&tup.Dyn->Tuple, i++, (CEU_Value) { CEU_VALUE_TAG, {.Tag=cur->tag} }));
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
                case 3: {   // add/rem
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
            CEU_Value str = args[0];
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
                default:
                    assert(0);
                    break;
            }
            ceu_gc_count++;
            ceu_hold_rem(dyn);
            ceu_dyn_free(dyn);
        }
        
        void ceu_gc_chk (CEU_Dyn* dyn) {
            assert(dyn->Any.type > CEU_VALUE_DYNAMIC);
            if (dyn->Any.refs == 0) {
                ceu_gc_free(dyn);
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
                    ceu_gc_chk(old.Dyn);
                }
            }
        }
    """ +
    """ // BLOCK
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
                    if (dyn->Coro.status != CEU_EXE_STATUS_TERMINATED) {
                        dyn->Coro.frame.clo->proto(&dyn->Coro.frame, CEU_ARG_FREE, NULL);
                    }
                    free(dyn->Coro.mem);
                    break;
        #endif
                default:
                    assert(0 && "bug found");
            }
            free(dyn);
        }
        
        void ceu_block_free (CEU_Block* blk) {
            CEU_Dyn* cur = blk->dyns;
            while (cur != NULL) {
                CEU_Dyn* old = cur;
                cur = old->Any.hld_next;
                ceu_dyn_free((CEU_Dyn*)old);
            }
            blk->dyns = NULL;
        }
    """ +
    """ // HOLD / DROP
        void ceu_hold_add (CEU_Dyn* dyn, CEU_Dyn** nxt) {
            dyn->Any.hld_prev = nxt;
            dyn->Any.hld_next = *nxt;
            if (*nxt != NULL) {
                (*nxt)->Any.hld_prev = &dyn->Any.hld_next;
            }
            *nxt = dyn;
        }
        void ceu_hold_rem (CEU_Dyn* dyn) {
            *(dyn->Any.hld_prev) = dyn->Any.hld_next;
            if (dyn->Any.hld_next != NULL) {
                dyn->Any.hld_next->Any.hld_prev = dyn->Any.hld_prev;
            }
            dyn->Any.hld_prev = NULL;
            dyn->Any.hld_next = NULL;
        }
        void ceu_hold_chg (CEU_Dyn* dyn, CEU_Dyn** nxt, int depth) {
            dyn->Any.hld_depth = depth;
            ceu_hold_rem(dyn);
            ceu_hold_add(dyn, nxt);
        }

        int ceu_hold_chk_set (CEU_Dyn** dst, int depth, CEU_HOLD hld_type, CEU_Value src) {
            if (src.type < CEU_VALUE_DYNAMIC) {
                return 1;
            } else if (src.Dyn->Any.hld_type == CEU_HOLD_FLEET) {
                if (src.Dyn->Any.refs>0 && depth>src.Dyn->Any.hld_depth) {
                    return 0;   // cant move to deeper scope with pending refs
                } else {
                    // continue below
                }
            } else if (depth >= src.Dyn->Any.hld_depth) {
                return 1;
            } else {
                //printf(">>> dst=%d >= src=%d\n", depth, src.Dyn->Any.hld_depth);
                return 0;
            };
            //printf(">>> %d %d -> %d\n", depth, src.Dyn->Any.hld_depth, src.Dyn->);

            src.Dyn->Any.hld_type = MAX(src.Dyn->Any.hld_type,hld_type);
            int src_depth = src.Dyn->Any.hld_depth;
            if (depth != src.Dyn->Any.hld_depth) {
                ceu_hold_chg(src.Dyn, dst, depth);
            }
            //printf(">>> %d -> %d\n", src_depth, src.Dyn->Any.hld_depth);
            if (depth >= src_depth) {
                return 1;
            }

            switch (src.Dyn->Any.type) {
                case CEU_VALUE_CLO_FUNC:
        #if CEU >= 3
                case CEU_VALUE_CLO_CORO:
        #endif
                    for (int i=0; i<src.Dyn->Clo.upvs.its; i++) {
                        if (!ceu_hold_chk_set(dst, depth, hld_type, src.Dyn->Clo.upvs.buf[i])) {
                            return 0;
                        }
                    }
                    break;
                case CEU_VALUE_TUPLE:
                    for (int i=0; i<src.Dyn->Tuple.its; i++) {
                        if (!ceu_hold_chk_set(dst, depth, hld_type, src.Dyn->Tuple.buf[i])) {
                            return 0;
                        }
                    }
                    break;
                case CEU_VALUE_VECTOR:
                    for (int i=0; i<src.Dyn->Vector.its; i++) {
                        if (!ceu_hold_chk_set(dst, depth, hld_type, ceu_vector_get(&src.Dyn->Vector,i))) {
                            return 0;
                        }
                    }
                    break;
                case CEU_VALUE_DICT:
                    for (int i=0; i<src.Dyn->Dict.max; i++) {
                        if (!ceu_hold_chk_set(dst, depth, hld_type, (*src.Dyn->Dict.buf)[i][0])) {
                            return 0;
                        }
                        if (!ceu_hold_chk_set(dst, depth, hld_type, (*src.Dyn->Dict.buf)[i][1])) {
                            return 0;
                        }
                    }
                    break;
            #if CEU >= 2
                case CEU_VALUE_THROW:
                    if (!ceu_hold_chk_set(dst, depth, hld_type, src.Dyn->Throw.val)) {
                        return 0;
                    }
                    if (!ceu_hold_chk_set(dst, depth, hld_type, src.Dyn->Throw.stk)) {
                        return 0;
                    }
                    break;
            #endif
            }
            return 1;
        }
        
        int ceu_hold_chk_set_col (CEU_Dyn* col, CEU_Value v) {
            if (v.type < CEU_VALUE_DYNAMIC) {
                return 1;
            }
            
            // col affects v:
            // [x,[1]] <-- moves v=[1] to v
            if (
                ceu_hold_chk_set(&col->Any.hld_next, col->Any.hld_depth, col->Any.hld_type, v) ||
                (col->Any.hld_type == CEU_HOLD_FLEET)
            ) {
                // ok
            } else {
                return 0;
            }
                     
            // v affects fleeting col with innermost scope
            if (col->Any.hld_type == CEU_HOLD_FLEET) {
                if (v.Dyn->Any.hld_depth < col->Any.hld_depth) {
                    return 1;
                } else {
                    col->Any.hld_type = MAX(col->Any.hld_type, MIN(CEU_HOLD_FLEET,v.Dyn->Any.hld_type));
                    if (v.Dyn->Any.hld_depth > col->Any.hld_depth) {
                        ceu_hold_chg(col, v.Dyn->Any.hld_prev, v.Dyn->Any.hld_depth);
                    }
                    return 1;
                }
            } else {
                return 1;
            }
        }

        CEU_Value ceu_drop_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n == 1);
            CEU_Value src = args[0];
            CEU_Dyn* dyn = src.Dyn;
            
            // do not drop non-dyn or globals
            if (src.type < CEU_VALUE_DYNAMIC) {
                return (CEU_Value) { CEU_VALUE_NIL };
            } else if (dyn->Any.hld_depth == 1) {
                return (CEU_Value) { CEU_VALUE_NIL };
            } else if (dyn->Any.hld_type == CEU_HOLD_FLEET) {
                return (CEU_Value) { CEU_VALUE_NIL };
            }
            
            //printf(">>> %d\n", dyn->Any.refs);
            if (dyn->Any.hld_type == CEU_HOLD_IMMUT) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="drop error : value is not movable"} };
            }
            //if (dyn->Any.refs > 1) {
            //    return (CEU_Value) { CEU_VALUE_ERROR, {.Error="drop error : multiple references"} };
            //}
            dyn->Any.hld_type = CEU_HOLD_FLEET;
            //ceu_hold_chg(dyn, &frame->up_block->dyns, frame->up_block->depth);

            switch (src.type) {
                case CEU_VALUE_CLO_FUNC:
        #if CEU >= 3
                case CEU_VALUE_CLO_CORO:
        #endif
                    for (int i=0; i<dyn->Clo.upvs.its; i++) {
                        CEU_Value args[1] = { dyn->Clo.upvs.buf[i] };
                        CEU_Value ret = ceu_drop_f(frame, 1, args);
                        if (ret.type == CEU_VALUE_ERROR) {
                            return ret;
                        }
                    }
                    break;
                case CEU_VALUE_TUPLE: {
                    for (int i=0; i<dyn->Tuple.its; i++) {
                        CEU_Value args[1] = { dyn->Tuple.buf[i] };
                        CEU_Value ret = ceu_drop_f(frame, 1, args);
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
                        CEU_Value args[1] = { ret1 };
                        CEU_Value ret2 = ceu_drop_f(frame, 1, args);
                        if (ret2.type == CEU_VALUE_ERROR) {
                            return ret2;
                        }
                    }
                    break;
                }
                case CEU_VALUE_DICT: {
                    for (int i=0; i<dyn->Dict.max; i++) {
                        CEU_Value args0[1] = { (*dyn->Dict.buf)[i][0] };
                        CEU_Value ret0 = ceu_drop_f(frame, 1, args0);
                        if (ret0.type == CEU_VALUE_ERROR) {
                            return ret0;
                        }
                        CEU_Value args1[1] = { (*dyn->Dict.buf)[i][1] };
                        CEU_Value ret1 = ceu_drop_f(frame, 1, args1);
                        if (ret1.type == CEU_VALUE_ERROR) {
                            return ret1;
                        }
                    }
                    break;
                }
                default:
                    break;
            }
            return (CEU_Value) { CEU_VALUE_NIL };;
        }        
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
                    return ceu_sizeof(CEU_Value, Dyn);
                default:
                    assert(0 && "bug found");
            }
        }
        
        int ceu_tuple_set (CEU_Tuple* tup, int i, CEU_Value v) {
            ceu_gc_inc(v);
            ceu_gc_dec(tup->buf[i], 1);
            tup->buf[i] = v;
            return ceu_hold_chk_set_col((CEU_Dyn*)tup, v);
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
        
        int ceu_vector_set (CEU_Vector* vec, int i, CEU_Value v) {
            if (v.type == CEU_VALUE_NIL) {           // pop
                assert(i == vec->its-1);
                CEU_Value ret = ceu_vector_get(vec, i);
                assert(ret.type != CEU_VALUE_ERROR);
                ceu_gc_dec(ret, 1);
                vec->its--;
                return 1;
            } else if (!ceu_hold_chk_set_col((CEU_Dyn*)vec, v)) {
                return 0;
            } else {
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
                return 1;
            }
        }
        
        CEU_Value ceu_vector_from_c_string (CEU_Block* hld, const char* str) {
            CEU_Value vec = ceu_vector_create(hld);
            int N = strlen(str);
            for (int i=0; i<N; i++) {
                assert(ceu_vector_set(&vec.Dyn->Vector, vec.Dyn->Vector.its, (CEU_Value) { CEU_VALUE_CHAR, {.Char=str[i]} }));
            }
            return vec;
        }

        CEU_Value ceu_next_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n==1 || n==2);
            CEU_Value col = args[0];
            CEU_Value key = (n == 1) ? ((CEU_Value) { CEU_VALUE_NIL }) : args[1];
            assert(col.type == CEU_VALUE_DICT);
            for (int i=0; i<col.Dyn->Dict.max; i++) {
                CEU_Value args[] = { key, (*col.Dyn->Dict.buf)[i][0] };
                CEU_Value ret = ceu_op_equals_equals_f(NULL, 2, args);
                assert(ret.type != CEU_VALUE_ERROR);
                if (ret.Bool) {
                    key = (CEU_Value) { CEU_VALUE_NIL };
                } else if (key.type == CEU_VALUE_NIL) {
                    return (*col.Dyn->Dict.buf)[i][0];
                }
            }
            return (CEU_Value) { CEU_VALUE_NIL };
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
        int ceu_dict_set (CEU_Dict* col, CEU_Value key, CEU_Value val) {
            //assert(key.type != CEU_VALUE_NIL);     // TODO
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
                return 1;
            } else if (!ceu_hold_chk_set_col((CEU_Dyn*)col, key)) {
                return 0;
            } else if (!ceu_hold_chk_set_col((CEU_Dyn*)col, val)) {
                return 0;
            } else {
                ceu_gc_inc(val);
                ceu_gc_dec(vv, 1);
                if (vv.type == CEU_VALUE_NIL) {
                    ceu_gc_inc(key);
                }
                (*col->buf)[old][0] = key;
                (*col->buf)[old][1] = val;
                return 1;
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
        CEU_Value ceu_tuple_create (CEU_Block* blk, int n) {
            CEU_Tuple* ret = malloc(sizeof(CEU_Tuple) + n*sizeof(CEU_Value));
            assert(ret != NULL);
            *ret = (CEU_Tuple) {
                CEU_VALUE_TUPLE, 0, CEU_HOLD_FLEET, blk->depth, NULL, NULL, NULL,
                n, {}
            };
            memset(ret->buf, 0, n*sizeof(CEU_Value));
            ceu_hold_add((CEU_Dyn*)ret, &blk->dyns);
            return (CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=(CEU_Dyn*)ret} };
        }
        
        CEU_Value ceu_tuple_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n==1 && args[0].type==CEU_VALUE_NUMBER);
            return ceu_tuple_create(frame->up_block, args[0].Number);
        }
        
        CEU_Value ceu_vector_create (CEU_Block* blk) {
            CEU_Vector* ret = malloc(sizeof(CEU_Vector));
            assert(ret != NULL);
            char* buf = malloc(1);  // because of '\0' in empty strings
            assert(buf != NULL);
            buf[0] = '\0';
            *ret = (CEU_Vector) {
                CEU_VALUE_VECTOR, 0, CEU_HOLD_FLEET, blk->depth, NULL, NULL, NULL,
                0, 0, CEU_VALUE_NIL, buf
            };
            ceu_hold_add((CEU_Dyn*)ret, &blk->dyns);
            return (CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=(CEU_Dyn*)ret} };
        }
        
        CEU_Value ceu_dict_create (CEU_Block* blk) {
            CEU_Dict* ret = malloc(sizeof(CEU_Dict));
            assert(ret != NULL);
            *ret = (CEU_Dict) {
                CEU_VALUE_DICT, 0, CEU_HOLD_FLEET, blk->depth, NULL, NULL, NULL,
                0, NULL
            };
            ceu_hold_add((CEU_Dyn*)ret, &blk->dyns);
            return (CEU_Value) { CEU_VALUE_DICT, {.Dyn=(CEU_Dyn*)ret} };
        }
        
        CEU_Value ceu_clo_create (int type, CEU_Block* blk, CEU_HOLD hld_type, CEU_Frame* frame, CEU_Proto proto, int upvs) {
            CEU_Clo* ret = malloc(sizeof(CEU_Clo));
            assert(ret != NULL);
            CEU_Value* buf = malloc(upvs * sizeof(CEU_Value));
            assert(buf != NULL);
            for (int i=0; i<upvs; i++) {
                buf[i] = (CEU_Value) { CEU_VALUE_NIL };
            }
            *ret = (CEU_Clo) {
                type, 0, hld_type, blk->depth, NULL, NULL, NULL,
                frame, proto, { upvs, buf }
        #if CEU >= 3
                , 0 // set from outside
        #endif
            };
            ceu_hold_add((CEU_Dyn*)ret, &blk->dyns);
            return (CEU_Value) { type, {.Dyn=(CEU_Dyn*)ret } };
        }        

        #if CEU >= 3
        CEU_Value ceu_exe_create (CEU_Block* blk, CEU_Value clo) {
            assert(clo.type==CEU_VALUE_CLO_CORO /*|| clo.type==CEU_VALUE_P_TASK*/);
            ceu_gc_inc(clo);
            
            CEU_Exe_Coro* ret = malloc(sizeof(CEU_Dyn));
            assert(ret != NULL);
            char* mem = malloc(clo.Dyn->Clo.Exe.n_mem);
            assert(mem != NULL);
            
            int hld_type = (clo.Dyn->Clo.hld_type <= CEU_HOLD_MUTAB) ? CEU_HOLD_FLEET : clo.Dyn->Clo.hld_type;
            *ret = (CEU_Exe_Coro) {
                CEU_VALUE_EXE_CORO, 1, hld_type, blk->depth, NULL, NULL, NULL,
                CEU_EXE_STATUS_YIELDED, { blk, &clo.Dyn->Clo, ret }, 0, mem
            };
            
            ceu_hold_add((CEU_Dyn*)ret, &blk->dyns);
            return (CEU_Value) { CEU_VALUE_EXE_CORO, {.Dyn=(CEU_Dyn*)ret } };
        }
        #endif        
    """ +
    """ // PRINT
        void ceu_print1 (CEU_Frame* _1, CEU_Value v) {
            // no tags when _1==NULL (ceu_error_list_print)
            if (_1!=NULL && v.type>CEU_VALUE_DYNAMIC) {  // TAGS
                CEU_Value args[1] = { v };
                CEU_Value tup = ceu_tags_f(_1, 1, args);
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
                ceu_hold_rem(tup.Dyn);
                ceu_dyn_free(tup.Dyn);
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
        #if CEU >= 2
                case CEU_VALUE_THROW:
                    printf("throw: %p | ", v.Dyn);
                    ceu_print1(_1, v.Dyn->Throw.val);
                    break;
        #endif
        #if CEU >= 3
                case CEU_VALUE_EXE_CORO:
                    printf("x-coro: %p", v.Dyn);
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
            CEU_Value e1 = args[0];
            CEU_Value e2 = args[1];
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
                        v = (e1.Dyn == e2.Dyn);
                        break;
            #if CEU >= 2
                    case CEU_VALUE_THROW:
                        v = (e1.Dyn == e2.Dyn);
                        break;
            #endif
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
            if (args[0].type == CEU_VALUE_VECTOR) {
                return (CEU_Value) { CEU_VALUE_NUMBER, {.Number=args[0].Dyn->Vector.its} };
            } else if (args[0].type == CEU_VALUE_TUPLE) {
                return (CEU_Value) { CEU_VALUE_NUMBER, {.Number=args[0].Dyn->Tuple.its} };
            } else {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="length error : not a vector"} };
            }
        }
    """ +
    """
        // IS' / IS-NOT' / THROW / POINTER-TO-STRING
        #if CEU >= 2
        CEU_Value ceu_is_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            assert(n == 2);
            if (ceu_op_equals_equals_f(_1, n, args).Bool) {
                return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
            }
            if (ceu_type_f(_1, 1, &args[1]).Tag != CEU_VALUE_TAG) {
                return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=0} };
            }
            if (ceu_type_f(_1, 1, &args[0]).Tag == args[1].Tag) {
                return (CEU_Value) { CEU_VALUE_BOOL, {.Bool=1} };
            }
            return ceu_tags_f(_1, n, args);
        }
        
        CEU_Value ceu_is_not_f (CEU_Frame* _1, int n, CEU_Value args[]) {
            CEU_Value ret = ceu_is_f(_1, n, args);
            ret.Bool = !ret.Bool;
            return ret;
        }

        CEU_Value _ceu_throw_ (CEU_Block* blk, CEU_Value val) {
            CEU_Value stk = ceu_vector_create(blk);
            
            ceu_gc_inc(val);
            ceu_gc_inc(stk);

            CEU_Throw* ret = malloc(sizeof(CEU_Throw));
            assert(ret != NULL);
            *ret = (CEU_Throw) {
                CEU_VALUE_THROW, 0, CEU_HOLD_FLEET, blk->depth, NULL, NULL, NULL,
                val, stk
            };
            
            ceu_hold_add((CEU_Dyn*)ret, &blk->dyns);
            ceu_hold_chk_set_col((CEU_Dyn*)ret, val);
            ceu_hold_chk_set_col((CEU_Dyn*)ret, stk);
            
            return (CEU_Value) { CEU_VALUE_THROW, {.Dyn=(CEU_Dyn*)ret} };
        }

        CEU_Value ceu_throw_f (CEU_Frame* frame, int n, CEU_Value args[]) {
            assert(n == 1);
            return _ceu_throw_(frame->up_block, args[0]);
        }

        CEU_Value ceu_pointer_to_string (CEU_Block* blk, const char* ptr) {
            CEU_Value str = ceu_vector_create(blk);
            int len = strlen(ptr);
            for (int i=0; i<len; i++) {
                CEU_Value chr = { CEU_VALUE_CHAR, {.Char=ptr[i]} };
                ceu_vector_set(&str.Dyn->Vector, i, chr);
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
            CEU_Value coro = args[0];
            if (coro.type != CEU_VALUE_CLO_CORO) {
                return (CEU_Value) { CEU_VALUE_ERROR, {.Error="coroutine error : expected coro"} };
            }
            return ceu_exe_create(frame->up_block, coro);
        }
        #if 0
        CEU_Value ceu_status_f (CEU_Frame* frame, int n, CEU_Value* args[]) {
            assert(n == 1);
            CEU_Value* coro = args[0];
            ceu_deref(coro);
            if (coro->type!=CEU_VALUE_EXE_CORO && coro->type!=CEU_VALUE_X_TASK) {
                CEU_THROW_MSG("\0 : status error : expected coroutine");
                CEU_THROW_RET(CEU_ERR_ERROR);
            }
            ceu_acc = (CEU_Value) { CEU_VALUE_TAG, {.Tag=coro->Dyn->Bcast.status + CEU_TAG_yielded - 1} };
            return CEU_RET_RETURN;
        }
        #endif
        #endif
    """ +
    """ // GLOBALS
        CEU_Block _ceu_block_ = { 0, 0, {.block=NULL}, NULL };
        CEU_Frame _ceu_frame_ = { &_ceu_block_, NULL CEU3(COMMA NULL) };
        CEU_Frame* ceu_frame = &_ceu_frame_;

        CEU_Clo ceu_dump = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_dump_f, {0,NULL}
        };
        CEU_Clo ceu_error = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_error_f, {0,NULL}
        };
        CEU_Clo ceu_next = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_next_f, {0,NULL}
        };
        CEU_Clo ceu_print = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_print_f, {0,NULL}
        };
        CEU_Clo ceu_println = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_println_f, {0,NULL}
        };
        CEU_Clo ceu_sup_question_ = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_sup_question__f, {0,NULL}
        };
        CEU_Clo ceu_tags = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_tags_f, {0,NULL}
        };
        CEU_Clo ceu_tuple = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_tuple_f, {0,NULL}
        };
        CEU_Clo ceu_type = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_type_f, {0,NULL}
        };
        CEU_Clo ceu_op_equals_equals = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_op_equals_equals_f, {0,NULL}
        };
        CEU_Clo ceu_op_hash = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_op_hash_f, {0,NULL}
        };
        CEU_Clo ceu_op_slash_equals = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_op_slash_equals_f, {0,NULL}
        };
        CEU_Clo ceu_string_dash_to_dash_tag = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_string_dash_to_dash_tag_f, {0,NULL}
        };
        #if CEU >= 2
        CEU_Clo ceu_is = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_is_f, {0,NULL}
        };
        CEU_Clo ceu_is_not = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_is_not_f, {0,NULL}
        };
        CEU_Clo ceu_pointer_dash_to_dash_string = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_pointer_dash_to_dash_string_f, {0,NULL}
        };
        CEU_Clo ceu_throw = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_throw_f, {0,NULL}
        };
        #endif
        #if CEU >= 3
        CEU_Clo ceu_coroutine = { 
            CEU_VALUE_CLO_FUNC, 1, CEU_HOLD_MUTAB, 1, NULL, NULL, NULL,
            &_ceu_frame_, ceu_coroutine_f, {0,NULL}
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
        CEU_Value id_is_plic_                = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_is}                      };
        CEU_Value id_is_dash_not_plic_       = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_is_not}                  };
        CEU_Value id_pointer_dash_to_dash_string = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_pointer_dash_to_dash_string} };
        CEU_Value id_throw                   = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_throw}                   };
        #endif
        #if CEU >= 3
        CEU_Value id_coroutine               = (CEU_Value) { CEU_VALUE_CLO_FUNC, {.Dyn=(CEU_Dyn*)&ceu_coroutine}               };
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
