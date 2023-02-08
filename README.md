# The Programming Language Ceu

Ceu is a synchronous programming language that reconciles *Structured
Concurrency* with *Event-Driven Programming*, extending classical structured
programming with three main functionalities:

- Structured Concurrency:
    - A set of structured primitives to compose concurrent tasks (e.g.,
      `spawn`, `par-or`, `toggle`).
    - A deterministic scheduling policy, which provides predictable behavior
      and safe abortion of tasks.
    - A container primitive to hold dynamic tasks, which automatically releases
      them on termination.
- Event Signaling Mechanisms:
    - An `await` primitive to suspend a task and wait for events.
    - A `broadcast` primitive to signal events and awake awaiting tasks.
- Lexical Memory Management:
    - Even dynamic allocation is attached to lexical blocks.
    - Strict escaping rules to preserve structure reasoning.
    - Garbage collection restricted to local references.

Ceu is inspired by Esterel and Lua.

Follows an extended list of functionalities:

- Dynamic typing
- Expression based (statements are expressions)
- Stackless coroutines (the basis of tasks)
- Restricted closures (upvalues must be explicit and final)
- Deferred expressions (for finalization)
- Exception handling
- Dynamic collections (tuples, vectors, and dictionaries)
- Hierarchical tuple templates (for data description and inheritance)

Ceu is in an **experimental stage**.
Both the compiler and runtime can become very slow.

# SYNTAX

## Basic Syntax

```
Block : `{´ { Expr } `}´
Expr  : ...
      | `do´ Block                                  ;; block
      | `val´ ID [TAG] [`=´ Expr]                   ;; declaration constant
      | `var´ ID [TAG] [`=´ Expr]                   ;; declaration variable
      | `set´ Expr `=´ Expr                         ;; assignment

      | `evt´ | `nil´ | `false` | `true´            ;; literals &
      | NAT | ID | TAG | CHAR | NUM                 ;; identifiers

      |  `[´ [List(Expr)] `]´                       ;; tuple
      | `#[´ [List(Expr)] `]´                       ;; vector
      | `@[´ [List(Key-Val)] `]´                    ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´

      | OP Expr                                     ;; op pre
      | `#´ Expr                                    ;; op pre length
      | Expr `[´ Expr `]´                           ;; op pos index
      | Expr `.´ (`pub´ | `status´)                 ;; op pos task field
      | Expr `.´ ID                                 ;; op pos dict field
      | Expr `(´ Expr `)´                           ;; op pos call
      | Expr OP Expr                                ;; op bin

      | `(´ Expr `)´                                ;; parenthesis
      | `pass´ Expr                                 ;; innocuous expression

      | `if´ Expr Block [`else´ Block]              ;; conditional
      | `loop´ `if´ Block                           ;; loop while
      | `loop´ `in´ :tasks Expr `,´ ID Block        ;; loop iterator tasks

      | `func´ `(´ [List(ID)] `)´ Block             ;; function
      | `coro´ `(´ [List(ID)] `)´ Block             ;; coroutine
      | `task´ `(´ [List(ID)] `)´ Block             ;; task

      | `defer´ Block                               ;; defer expressions
      | `catch´ Expr Block                          ;; catch exception
      | `throw´ `(´ Expr `)´                        ;; throw exception

      | `enum´ `{´ List(TAG [`=´ Expr]) `}´         ;; tags enum
      | `data´ Data                                 ;; tags hierarchy
            Data : TAG `=´ List(ID [TAG]) [`{´ { Data } `}´]

      | `spawn´ [`in´ Expr `,´] Call
      | `broadcast´ `in´ Expr `,´ Call
      | `yield´ [`:all´] `(´ Expr `)´
      | `resume´ Call
      | `toggle´ Call

List(x) : x { `,´ x }

ID    : [A-Za-z_][A-Za-z0-9_\'\?\!\-]*
TAG   : :[A-Za-z0-9\.\-]+
OP    : [+-*/><=!|&~%#]+
CHAR  : '.' | '\\.'
NUM   : [0-9][0-9A-Za-z\.]*
NAT   : `.*`
```

## Extended Syntax

```
Expr  : ...
      | `not´ Expr                                  ;; op not
      | Expr `[´ (`=´|`+´|`-´) `]´                  ;; ops peek,push,pop
      | Expr `.´ NUM                                ;; op tuple index
      | Expr (`or´|`and´|`is´|`is-not´) Expr        ;; op bin

      | `ifs´ `{´ {Case} [Else] `}´                 ;; conditionals
            Case : Expr `->´ (Expr | Block)
            Else : `else´ `->´ (Expr | Block)
      | `ifs´ Expr `{´ {Case} [Else] `}´            ;; switch + conditionals
            Case : [`==´ | `is´] Expr `->´ (Expr | Block)
            Else : `else´ `->´ (Expr | Block)

      | `loop´ Block                                ;; loop infinite
      | `loop´ `in´ Expr `,´ ID Block               ;; loop iterator
      | `loop´ `in´                                 ;; loop iterator numeric
            (`[´ | `(´)
            Expr `->´ Expr
            (`]´ | `)´)
            [`,´ :step Expr]
            `,´ ID Block

      | `func´ ID `(´ [List(ID)] `)´ Block
      | `coro´ ID `(´ [List(ID)] `)´ Block
      | `task´ ID `(´ [List(ID)] `)´ Block

      | `spawn´ Block
      | `await´ Await
      | `every´ Await Block
      | `awaiting´ Await Block
      | `par´ Block { `with´ Block }
      | `par-and´ Block { `with´ Block }
      | `par-or´ Block { `with´ Block }
      | `toggle´ Await `->´ Await Block

Await : [`:check-now`] (
            | `spawn´ Call
            | { Expr (`:h´|`:min´|`:s´|`:ms´) }
            | TAG `,´ Expr
            | Expr
        )
```
