```
1. LEXICON
    1. Keywords         4. Identifiers
    2. Symbols          5. Literals
    3. Operators        6. Comments
2. TYPES
    1. Simple Types
    2. Collections
    3. Code Abstractions
3. VALUES
4. EXPRESSIONS
    1. Block
A. SYNTAX
    1. Basic Syntax
    2. Extended Syntax
```

<!--
Ceu is a expression-based, dynamically-typed synchronous programming language
that reconciles Structured Concurrency with Event-Driven Programming.

Ceu extends classical structured programming with three main functionalities:

# 0. DESIGN

## 0.1. Basics

## 0.2. Lexical Memory Management

## 0.3. Structured Concurrency

- coro vs task
- tasks, tracks
- bcast, await
- synchronous

## 0.3. Tags

    Structured Concurrency:
        A set of structured primitives to compose concurrent tasks (e.g., spawn, par-or, toggle).
        A synchronous and deterministic scheduling policy, which provides predictable behavior and safe abortion of tasks.
        A container primitive to hold dynamic tasks, which automatically releases them on termination.
    Event Signaling Mechanisms:
        An await primitive to suspend a task and wait for events.
        A broadcast primitive to signal events and awake awaiting tasks.
    Lexical Memory Management:
        Even dynamic allocation is attached to lexical blocks.
        Strict escaping rules to preserve structure reasoning.
        Garbage collection restricted to local references only.

Ceu is inspired by Esterel and Lua.

Follows an extended list of functionalities:

    Dynamic typing
    Expression based (statements are expressions)
    Stackless coroutines (the basis of tasks)
    Restricted closures (upvalues must be explicit and final)
    Deferred expressions (for finalization)
    Exception handling
    Dynamic collections (tuples, vectors, and dictionaries)
    Hierarchical tuple templates (for data description with inheritance)
    Seamless integration with C (source-level compatibility)

Ceu is in experimental stage. Both the compiler and runtime can become very slow.
-->

# 1. LEXICON

## 1.1. Keywords

The following keywords are reserved in Ceu:

```
    and             ;; and operator                 (00)
    await           ;; await event
    awaiting        ;; awaiting block
    broadcast       ;; broadcast event
    catch           ;; catch exception
    coro            ;; coroutine declaration
    data            ;; data declaration
    defer           ;; defer block
    do              ;; do block
    else            ;; else block
    enum            ;; enum declaration             (10)
    err             ;; exception variable
    every           ;; every block
    evt             ;; event variable
    false           ;; false value
    func            ;; function declaration
    if              ;; if block
    ifs             ;; ifs block
    in              ;; in keyword
    is              ;; is operator
    is-not          ;; is-not operator              (20)
    loop            ;; loop block
    nil             ;; nil value
    not             ;; not operator
    or              ;; or operator
    par             ;; par block
    par-and         ;; par-and block
    par-or          ;; par-or block
    pass            ;; innocuous expression
    poly            ;; TODO
    pub             ;; public variable              (30)
    resume          ;; resume expression
    set             ;; assign expression
    spawn           ;; spawn expression
    status          ;; status variable
    task            ;; task declaration/identifier
    toggle          ;; toggle expression/block
    true            ;; true value
    until           ;; until loop modifier
    val             ;; constant declaration
    var             ;; variable declaration         (40)
    where           ;; where block
    with            ;; with block
    yield           ;; yield expression
```

Keywords cannot be used as variable identifiers.

## 1.2. Symbols

The following symbols are reserved in Ceu:

```
    {   }           ;; block/operators delimeters
    (   )           ;; expression delimeters
    [   ]           ;; index/constructor delimeters
    =               ;; assignment separator
    ->              ;; iterator/ifs/toggle clause
    ;               ;; sequence separator
    ,               ;; argument/constructor separator
    .               ;; index/field discriminator
    ...             ;; variable arguments
    #               ;; vector constructor
    @               ;; dictionary constructor
    '   "   `       ;; character/string/native delimiters
    $               ;; native interpolation
    ^               ;; lexer annotation
```

## 1.3. Operators

The following operator symbols can be combined to form operator names in Ceu:

```
    +    -    *    /
    >    <    =    !
    |    &    ~    %    #
```

Operators names cannot clash with reserved symbols.

## 1.4. Identifiers

Ceu uses identifiers to refer to variables and operators:

```
ID : [A-Za-z_][A-Za-z0-9_'?!-]*      ;; letter/under/digit/quote/quest/excl/dash
OP : [+-*/><=!|&~%#]+                ;; see Operators
```

A variable identifier starts with a letter or underscore (`_`) and is followed
by letters, digits, underscores, single quotes (`'`), question marks (`?`),
exclamation marks (`!`), or dashes (`-`).
A dash must be followed by a letter or digit.

An operator identifier is a sequence of operator symbols
(see [Operators](#TODO)).

Examples:

```
x               ;; simple var id
my-value        ;; var with dash
empty?          ;; var with question
map'            ;; var with prime
>               ;; simple op id
++              ;; op with multi chars
```

## 1.5. Literals

Ceu provides literals for *nils*, *booleans*, *numbers*, *characters*,
*strings*, and *native expressions*:

```
NIL  : nil
BOOL : true | false
TAG  : :[A-Za-z0-9\.\-]+      ;; colon + leter/digit/dot/dash
NUM  : [0-9][0-9A-Za-z\.]*    ;; digit/letter/dot
CHR  : '.' | '\.'             ;; single/backslashed character
STR  : ".*"                   ;; string expression
NAT  : `.*`                   ;; native expression
```

The literal `nil` is the single value of the [*nil* type](#TODO).

The literals `true` and `false` are the values of the [*bool* type](#TODO).

A [*tag* type](#TODO) literal starts with a colon (`:`) and is followed by
letters, digits, dots (`.`), or dashes (`-`).
A dot or dash must be followed by a letter or digit.

A [*number* type](#TODO) literal starts with a digit and is followed by digits,
letters, and dots (`.`), and adheres to the [C standard](#TODO).

A [*char* type](#TODO) literal is a single or backslashed (`\`) character
enclosed by single quotes (`'`), and adheres to the [C standard](#TODO).

A string literal is a sequence of characters enclosed by double quotes (`"`).
It is expanded to a [vector](#TODO) of character literals, e.g., `"abc"`
expands to `#['a','b','c']`.

A native literal is a sequence of characters enclosed by multiple back quotes
(`` ` ``).
The same number of backquotes must be used to open and close the literal.
`TODO: $, :type, :pre, :ceu`

Examples:

```
nil                 ;; nil literal
false               ;; bool literal
:X.Y                ;; tag literal
1.25                ;; number literal
'a'                 ;; char literal
"Hello!"            ;; string literal
`puts("hello");`    ;; native literal
`:number sin($x)`   ;; native with type and interpolation
```

## 1.6. Comments

Ceu provides single-line and multi-line comments.

Single-line comments start with double semi-colons (`;;`) and run until the end
of the line.

Multi-line comments use balanced semi-colons, starting with three or more
semi-colons and running until the same number of semi-colons.
Multi-line comments can contain sequences of semi-colons, as long as they are
shorter than the opening sequence.

Examples:

```
;; a comment        ;; single-line comment
;;;                 ;; multi-line comment
;; a
;; comment
;;;
```

# 2. TYPES

Ceu is a dynamic language in which values carry their own types during
execution.

## 2.1. Simple Types

Ceu has 6 basic types:

```
nil    bool    char    number    pointer    tag
```

The `nil` type represents the absence of values with its single value
[`nil`](#TODO).

The `bool` type represents boolean values with [`true`](#TODO) and
[`false`](#TODO).
In a boolean context, `nil` is interpreted as `false` and all other values from
all other types are interpreted as `true`.

The `char` type represents [character literals](#TODO).

The `number` type represents real numbers (i.e., *C floats*) with
[number literals](#TODO).

The `pointer` type represents opaque native pointer values from [native
literals](#TODO).

The `tag` type represents [tag identifiers](#TODO).
Each tag represents a unique value in a global enumeration.
Tags are also known as *symbols* or *atoms* in other programming languages.
`TODO: tag hierarchy, subtyping`

## 2.2. Collections

Ceu has 3 collection types:

```
tuple    vector    dict
```

The `tuple` type represents a fixed collection of heterogeneous values, in
which each numeric index holds a value of a (possibly) different type.

The `vector` type represents a variable collection of homogeneous values, in
which each numeric index holds a value of the same type.

The `dict` type (dictionary) represents a variable collection of heterogeneous
values, in which each index of any type maps to a value of a (possibly)
different type.

Examples:

```
[1, 'a', nil]           ;; a tuple with 3 values
#[1, 2, 3]              ;; a vector of numbers
@[(:x,10), (:y,20)]     ;; a dictionary with 2 mappings
```

## 2.3. Code Abstractions

Ceu provide 3 types of code abstractions:

```
func    coro    task
x-coro  x-task  x-tasks  x-track
```

The `func` type represents [functions](#TODO) (subroutines).

The `coro` type represents [coroutines](#TODO), while the `x-coro` type
represents spawned coroutines.

The `task` type represents [tasks](#TODO), while the `x-task` type represents
spawned tasks.
The `x-tasks` type represents [task pools](#TODO) holding running tasks.
The `x-track` type represents [track references](#TODO) pointing to running
tasks.

Code abstractions are described in [Section TODO](#TODO).

# 3. VALUES

As a dynamic language, each value in Ceu carries extra information, such as its
own type.

## 3.1. Plain Values

A *plain value* does not require dynamic allocation since it only carries extra
information about its type.
The following types have plain values:

```
nil    bool    char    number    pointer    tag
```

Plain values are immutable and are copied between variables and blocks as a
whole without any restrictions.

## 3.2. Dynamic Values

A *dynamic value* requires dynamic allocation since its internal data is too
big to fit in a plain value.
The following types have dynamic values:

```
tuple    vector    dict
func     coro      task
x-coro   x-task    x-tasks   x-track
```

Dynamic values are mutable and are manipulated through references, allowing
that multiple aliases refer to the same value.

Dynamic values are always attached to the enclosing [block](#TODO) in which
they were created, and cannot escape to outer blocks in assignments or as
return expressions.
This restriction permits that terminating blocks deallocate all dynamic values
attached to them.
Ceu also provides an explicit [move](#TODO) operation to reattach a dynamic
value to an outer scope.
Nevertheless, a dynamic value is still subject to garbage collection, given
that it may loose all references to it, even with its enclosing block active.

## 3.3. Running Values

A *running value* corresponds to an active coroutine, task, pool of tasks,
or tracked reference:

```
x-coro  x-task  x-tasks  x-track
```

A running value is still a dynamic value, with all properties described above.
In addition, it also requires to run a finalization routine when going out of
scope in order to terminate active blocks.
An `x-track` is set to `nil` when its referred task terminates or goes out of
scope.
This is all automated by Ceu.

# 4. EXPRESSIONS

Ceu is an expression-based language in which all statements are expressions and
evaluate to a value.

## 4.1. Program and Blocks

A program in Ceu is a sequence of expressions, and a block is a sequence of
expressions enclosed by braces (`{` and `}´):

```
Prog  : { Expr }
Block : `{` { Expr } `}`
```

A sequence of expressions evaluate to its last expression.

### Blocks

A Block delimits a lexical scope for variables and dynamic values:
A variable is only visible to expressions in the block in which it was
declared.
A dynamic value cannot escape the block in which it was created (e.g., from
assignments or returns), unless it is [moved](#TODO) out.
For this reason, when a block terminates, all memory that was allocated inside
it is automatically reclaimed.

A block is not an expression by itself, but it can be turned into one by
prefixing it with an explicit `do`:

```
Do : `do´ Block       ;; an explicit block expression
```

Examples:

```
do {                    ;; block prints :ok and evals to 1
    println(:ok)
    1
}

do {
    val a = 1           ;; `a` is only visible in the block
}
a                       ;; ERR: `a` is out of scope

var x
do {
    set x = [1,2,3]     ;; ERR: tuple cannot be assigned to outer block
    #[1,2,3]            ;; ERR: vector cannot return from block
}

do {
    move(#[1,2,3])      ;; OK
}
```

Blocks also appear in compound statements, such as [conditionals](#TODO),
[loops](#TODO), and many others.

## 3.2. Declarations and Assignments

Regardless of being dynamically typed, all variables in Ceu must be declared
before use:

```
Val : `val´ ID [TAG] [`=´ Expr]
Var : `var´ ID [TAG] [`=´ Expr]
```

`TODO: tag, evt`

The difference between `val` and `var` is that a `val` is immutable, while a
`var` declaration can be modified by further `set` expressions:

```
`set´ Expr `=´ Expr
```

The optional initialization expression assigns an initial value to the
variable, which is set to `nil` otherwise.

The `val` modifier forbids that a name is reassigned, but it does not prevent
that dynamic values are modified.

Examples:

```
var x
set x = 20      ;; OK

val y = [10]
set y = 0       ;; ERR: cannot reassign `y`
set y[0] = 20   ;; OK
```

## 3.3. Conditionals and Loops

Ceu supports conditionals and loops as follows:

```
`if´ Expr Block [`else´ Block]
`loop´ `if´ Block
```

An `if` tests a boolean expression and, if true, executes the associated block.
Otherwise, it executes the optional `else` block.

A `loop if` tests a boolean expression and, if true, executes an iteration of
the associated block, before testing the condition again.
When the condition is false, the loop terminates.
There is no `break` expression in Ceu, which can be substituted by a proper
test condition or [`throw`-`catch`](#TODO) pair.

Examples:

```
val x-or-y =        ;; max between x and y
    if x > y {
        x
    } else {
        y
    }

var i = 0           ;; prints 0,1,2,3,4
loop if i<5 {
    println(i)
    set i = i + 1
}
```

Ceu also provides syntactic extensions for [`ifs`](#TODO) with multiple
conditions and [`loop in`](#TODO) iterators.

## 3.4. Literals, Identifiers, and Constructors

[Literals](#TODO) (for the simple types) and [identifiers](#TODO) (for
variables and operators) are the most basic expressions of Ceu:

```
Basic : `nil´ | `false` | `true´
      | NAT | TAG | CHR | NUM
      | ID | `err´ | `evt´
```

Ceu provides constructors for [collections](#TODO) to allocate tuples, vectors,
and dictionaries:

```
Cons : `[´ [List(Expr)] `]´             ;; tuple
     | `#[´ [List(Expr)] `]´            ;; vector
     | `@[´ [List(Key-Val)] `]´         ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´
```

Tuples (`[...]`) and vectors (`#[...]`) are described as a list of expressions.

Dictionaries (`@[...]`) are described as a list of pairs of expressions
(`(key,val)`), in which each pair maps a key to a value.
The first expression is the key, and the second is the value.
If the key is a tag, the alternate syntax `tag=val` may be used (omitting the
tag `:`).

Examples:

```
10                  ;; a nil expression
:x                  ;; a tag expression
[(:x,10), x=10]     ;; a dictionary with equivalent key mappings
```

## 3.5. Calls, Operations, and Indexing

### Calls and Operations

In Ceu, calls and operations are equivalent, i.e., an operation is a call that
uses an operator with a special syntax:

```
Call : OP Expr              ;; unary operation
     | `#´ Expr             ;; length operation
     | Expr `(´ Expr `)´    ;; function call
     | Expr OP Expr         ;; binary operation
```

<!--
     | `{´ OP `}` `(´ Expr `)´                         ;; pos op call
     | Expr `{´ Expr `}` Expr                          ;; op bin
     | `{´ OP `}´   -- TODO: basic expr
-->

`TODO: x-y`

Examples:

```
-x
#vec
f(10,20)
x + 10
```

### Fields and Indexes

      | Expr `[´ Expr `]´                               ;; op pos index
      | Expr `.´ (`pub´ | `status´)                     ;; op pos task field
      | Expr `.´ ID                                     ;; op pos dict field

### Precedence

# A. SYNTAX

## A.1. Basic Syntax

```
Prog  : { Expr }
Block : `{´ { Expr } `}´
Expr  : ...
      | `do´ Block                                      ;; explicit block
      | `val´ ID [TAG] [`=´ Expr]                       ;; declaration constant
      | `var´ ID [TAG] [`=´ Expr]                       ;; declaration variable
      | `set´ Expr `=´ Expr                             ;; assignment

      | `nil´ | `false` | `true´                        ;; literals &
      | NAT | TAG | CHR | NUM                           ;; identifiers
      | ID | `err´ | `evt´

      |  `[´ [List(Expr)] `]´                           ;; tuple
      | `#[´ [List(Expr)] `]´                           ;; vector
      | `@[´ [List(Key-Val)] `]´                        ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´

      | OP Expr                                         ;; pre op
      | `#´ Expr                                        ;; pre length
      | Expr `(´ Expr `)´                               ;; pos call
      | Expr OP Expr                                    ;; bin op

      | Expr `[´ Expr `]´                               ;; pos index
      | Expr `.´ (`pub´ | `status´)                     ;; pos task field
      | Expr `.´ ID                                     ;; pos dict field

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
CHR   : '.' | '\\.'                                     ;; literal character
NUM   : [0-9][0-9A-Za-z\.]*                             ;; literal number
NAT   : `.*`                                            ;; native expression
```

## A.2. Extended Syntax

```
Expr  : ...
      | STR
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

STR   : ".*"                                            ;; string expression
```

