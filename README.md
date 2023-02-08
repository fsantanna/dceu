# The Programming Language Ceu

Ceu is a synchronous programming language that reconciles *Structured
Concurrency* with *Event-Driven Programming*, extending classical structured
programming with three main functionalities:

- Structured Concurrency:
    - A set of structured primitives to compose concurrent tasks (e.g.,
      `spawn`, `par-or`, `toggle`).
    - A synchronous and deterministic scheduling policy, which provides
      predictable behavior and safe abortion of tasks.
    - A container primitive to hold dynamic tasks, which automatically releases
      them on termination.
- Event Signaling Mechanisms:
    - An `await` primitive to suspend a task and wait for events.
    - A `broadcast` primitive to signal events and awake awaiting tasks.
- Lexical Memory Management:
    - Even dynamic allocation is attached to lexical blocks.
    - Strict escaping rules to preserve structure reasoning.
    - Garbage collection restricted to local references only.

Ceu is inspired by Esterel and Lua.

Follows an extended list of functionalities:

- Dynamic typing
- Expression based (statements are expressions)
- Stackless coroutines (the basis of tasks)
- Restricted closures (upvalues must be explicit and final)
- Deferred expressions (for finalization)
- Exception handling
- Dynamic collections (tuples, vectors, and dictionaries)
- Hierarchical tuple templates (for data description with inheritance)

Ceu is in an **experimental stage**.
Both the compiler and runtime can become very slow.

# INSTALL

First, you need to install `java`:

```
$ sudo apt install default-jre
```

Then, you are ready to install `ceu`:

```
$ wget https://github.com/fsantanna/dceu/releases/download/v0.1.0/install-v0.1.0.sh
$ sh install-v0.1.0.sh ./ceu/  # (you may change the destination directory)
```

Finally, execute `ceu`:

```
$ ./ceu/ceu ./ceu/hello-world.ceu
[0,hello]
[1,world]
```

# PICO-CEU

The best way to try Ceu is through `pico-ceu`, a graphical library based on
SDL:

- https://github.com/fsantanna/pico-ceu

# SYNTAX

## Basic Syntax

```
Block : `{´ { Expr } `}´
Expr  : ...
      | `do´ Block                                      ;; block
      | `val´ ID [TAG] [`=´ Expr]                       ;; declaration constant
      | `var´ ID [TAG] [`=´ Expr]                       ;; declaration variable
      | `set´ Expr `=´ Expr                             ;; assignment

      | `evt´ | `nil´ | `false` | `true´                ;; literals &
      | NAT | ID | TAG | CHAR | NUM                     ;; identifiers

      |  `[´ [List(Expr)] `]´                           ;; tuple
      | `#[´ [List(Expr)] `]´                           ;; vector
      | `@[´ [List(Key-Val)] `]´                        ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´

      | OP Expr                                         ;; op pre
      | `#´ Expr                                        ;; op pre length
      | Expr `[´ Expr `]´                               ;; op pos index
      | Expr `.´ (`pub´ | `status´)                     ;; op pos task field
      | Expr `.´ ID                                     ;; op pos dict field
      | Expr `(´ Expr `)´                               ;; op pos call
      | Expr OP Expr                                    ;; op bin

      | `(´ Expr `)´                                    ;; parenthesis
      | `pass´ Expr                                     ;; innocuous expression

      | `if´ Expr Block [`else´ Block]                  ;; conditional
      | `loop´ `if´ Block                               ;; loop while
      | `loop´ `in´ :tasks Expr `,´ ID Block            ;; loop iterator tasks

      | `func´ `(´ [List(ID)] `)´ Block                 ;; function
      | `coro´ `(´ [List(ID)] `)´ Block                 ;; coroutine
      | `task´ `(´ [List(ID)] `)´ Block                 ;; task

      | `defer´ Block                                   ;; defer expressions
      | `catch´ Expr Block                              ;; catch exception
      | `throw´ `(´ Expr `)´                            ;; throw exception

      | `enum´ `{´ List(TAG [`=´ Expr]) `}´             ;; tags enum
      | `data´ Data                                     ;; tags hierarchy
            Data : TAG `=´ List(ID [TAG]) [`{´ { Data } `}´]

      | `spawn´ [`in´ Expr `,´] Expr `(´ Expr `)´       ;; spawn coro/task
      | `broadcast´ `in´ Expr `,´  Expr `(´ Expr `)´    ;; broadcast event
      | `yield´ [`:all´] `(´ Expr `)´                   ;; yield from coro/task
      | `resume´ Expr `(´ Expr `)´                      ;; resume coro/task
      | `toggle´ Call                                   ;; toggle task

List(x) : x { `,´ x }                                   ;; comma-separated list

ID    : [A-Za-z_][A-Za-z0-9_\'\?\!\-]*                  ;; identifier variable
TAG   : :[A-Za-z0-9\.\-]+                               ;; identifier tag
OP    : [+-*/><=!|&~%#]+                                ;; identifier operation
CHAR  : '.' | '\\.'                                     ;; literal character
NUM   : [0-9][0-9A-Za-z\.]*                             ;; literal number
NAT   : `.*`                                            ;; native expression
```

## Extended Syntax

```
Expr  : ...
      | `not´ Expr                                      ;; op not
      | Expr `[´ (`=´|`+´|`-´) `]´                      ;; ops peek,push,pop
      | Expr `.´ NUM                                    ;; op tuple index
      | Expr (`or´|`and´|`is´|`is-not´) Expr            ;; op bin

      | `ifs´ `{´ {Case} [Else] `}´                     ;; conditionals
            Case : Expr `->´ (Expr | Block)
            Else : `else´ `->´ (Expr | Block)
      | `ifs´ Expr `{´ {Case} [Else] `}´                ;; switch + conditionals
            Case : [`==´ | `is´] Expr `->´ (Expr | Block)
            Else : `else´ `->´ (Expr | Block)

      | `loop´ Block                                    ;; loop infinite
      | `loop´ `in´ Expr `,´ ID Block                   ;; loop iterator
      | `loop´ `in´                                     ;; loop iterator numeric
            (`[´ | `(´)
            Expr `->´ Expr
            (`]´ | `)´)
            [`,´ :step Expr]
            `,´ ID Block

      | `func´ ID `(´ [List(ID)] `)´ Block              ;; declaration func
      | `coro´ ID `(´ [List(ID)] `)´ Block              ;; declaration coro
      | `task´ ID `(´ [List(ID)] `)´ Block              ;; declaration task

      | `spawn´ Block                                   ;; spawn anonymous task
      | `await´ Await                                   ;; await event
      | `every´ Await Block                             ;; await event in loop
      | `awaiting´ Await Block                          ;; abort on event
      | `par´ Block { `with´ Block }                    ;; spawn tasks
      | `par-and´ Block { `with´ Block }                ;; spawn tasks, rejoin on all
      | `par-or´ Block { `with´ Block }                 ;; spawn tasks, rejoin on any
      | `toggle´ Await `->´ Await Block                 ;; toggle task on/off on events

Await : [`:check-now`] (
            | `spawn´ Call
            | { Expr (`:h´|`:min´|`:s´|`:ms´) }
            | TAG `,´ Expr
            | Expr
        )
```
