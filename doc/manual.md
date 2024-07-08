# The Programming Language Ceu (v0.4)

* DESIGN
    * Structured Deterministic Concurrency
    * Event Signaling Mechanisms
    * Hierarchical Tags and Tuple Templates
    * Integration with C
* LEXICON
    * Keywords
    * Symbols
    * Operators
    * Identifiers
    * Literals
    * Comments
* TYPES
    * Basic Types
        - `nil` `bool` `char` `number` `tag` `pointer`
    * Collections
        - `tuple` `vector` `dict`
    * Execution Units
        - `func` `coro` `task` `exe-coro` `exe-task` `tasks`
    * User Types
* VALUES
    * Static Values
        - `nil` `bool` `char` `number` `tag` `pointer`
    * Dynamic Values
        - `tuple` `vector` `dict` (collections)
        - `func` `coro` `task` (prototypes)
        - `exe-coro` `exe-task` `tasks` (actives)
* STATEMENTS
    * Program, Sequences and Blocks
        - `;` `do` `defer`
    * Declarations and Assignments
        - `val` `var` `set`
    * Tag Enumerations and Tuple Templates
        - `enum` `data`
    * Calls, Operations and Indexing
        - `-x` `x+y` `f(...)` `-->`
        - `t[...]` `t.x` `t.pub` `t.(:X)` `t[=]`
        - `where` `thus`
    * Conditionals and Pattern Matching
        - `if` `ifs`
    * Loops and Iterators
        - `loop` `loop in`
    * Exceptions
        - `error` `catch`
    * Coroutine Operations
        - `coroutine` `status` `resume` `yield` `resume-yield-all` <!--`abort`-->
    * Task Operations
        - `pub` `spawn` `tasks` `status` `await` `broadcast` `toggle`
        - `spawn {}` `every` `par` `par-and` `par-or` `watching` `toggle {}`
* STANDARD LIBRARY
    * Primary Library
    * Auxiliary Library
* SYNTAX

<!-- CONTENTS -->

# DESIGN

Ceu is a [synchronous programming language][1] that reconciles *[Structured
Concurrency][2]* with *[Event-Driven Programming][3]* to extend classical
structured programming:

- Structured Deterministic Concurrency:
    - A set of structured primitives to lexically compose concurrent tasks
      (e.g., `spawn`, `par-or`, `toggle`).
    - A synchronous and deterministic scheduling policy, which provides
      predictable behavior and safe abortion of tasks.
- Event Signaling Mechanisms:
    - An `await` primitive to suspend a task and wait for events.
    - A `broadcast` primitive to signal events and awake awaiting tasks.

Ceu is inspired by [Esterel][4] and [Lua][5].

Follows an extended list of functionalities in Ceu:

- Dynamic typing
- Statements as expressions
- Dynamic collections (tuples, vectors, and dictionaries)
- Stackless coroutines (the basis of tasks)
- Restricted closures (upvalues must be final)
- Deferred statements (for finalization)
- Exception handling (error & catch)
- Hierarchical Tags and Tuple Templates (for data description)
- Seamless integration with C (source-level compatibility)

Ceu is in **experimental stage**.
Both the compiler and runtime can become very slow.

In the rest of this Section, we introduce the two key aspects of Ceu:
*Structured Deterministic Concurrency* and *Event Signaling Mechanisms*.
Then, we also introduce two other key aspects of the language, which do not
appear in other languages:
*Hierarchical Tags* and *Integration with C*.

[1]: https://fsantanna.github.io/sc.html
[2]: https://en.wikipedia.org/wiki/Structured_concurrency
[3]: https://en.wikipedia.org/wiki/Event-driven_programming
[4]: https://en.wikipedia.org/wiki/Esterel
[5]: https://en.wikipedia.org/wiki/Lua_(programming_language)

## Structured Deterministic Concurrency

In structured concurrency, the life cycle of processes or tasks respect the
structure of the source code in hierarchical blocks.
In this sense, tasks in Ceu are treated in the same way as local variables in
structured programming:
When a [block](#blocks) of code terminates or goes out of scope, all of its
[local variables](#declarations) and [tasks](#active-values) are deallocated
and become inaccessible to enclosing blocks.
In addition, tasks are properly aborted and finalized by [deferred
statements](#defer).

Tasks in Ceu are built on top of [coroutines](#active-values), which unlike OS
threads, have a predictable run-to-completion semantics, in which they execute
uninterruptedly up to an explicit [yield](#yield) or [await](#await) operation.

The next example illustrates structured concurrency, abortion of tasks, and
deterministic scheduling.
The example uses a `par-or` to spawn two concurrent tasks:
    one that terminates after 10 seconds, and
    another one that increments variable `n` every second, showing its value on
    termination:

<!-- pico/tst/counter.ceu -->

```
spawn {
    par-or {
        await <10:s>
    } with {
        var n = 0
        defer {
            println("I counted ", n)    ;; invariably outputs 9
        }
        every <1:s> {
            set n = n + 1
        }
    }
}
```

The [`par-or`](parallel-blocks) is a structured mechanism that combines tasks
in blocks and rejoins as a whole when one of its tasks terminates,
automatically aborting the others.

The [`every`](every-block) loop in the second task iterates exactly 9 times
before the first task awakes and terminates the composition.
For this reason, the second task is aborted before it has the opportunity to
awake for the 10th time, but its `defer` statement still executes and outputs
`"I counted 9"`.

Since they are based on coroutines, tasks are expected to yield control
explicitly, which makes scheduling entirely deterministic.
In addition, tasks awake in the order they appear in the source code, which
makes the scheduling order predictable.
This rule allows us to infer that the example invariably outputs `9`, no matter
how many times we re-execute it.
Likewise, if the order of the two tasks inside the `par-or` were inverted, the
example would always output `10`.

## Event Signaling Mechanisms

Tasks can communicate through events as follows:

- The [`await`](#await) statement suspends a task until it matches an event
  condition.
- The [`broadcast`](#broadcast) statement signals an event to all awaiting
  tasks.

<img src="bcast.png" align="right"/>

Active tasks form a dynamic tree representing the structure of the program, as
illustrated in the figure.
This three is traversed on every broadcast in a predictable way, since it
respects the lexical structure of the program:
A task has exactly one active block at a time, which is first traversed `(1)`.
The active block has a list of active tasks, which are traversed in sequence
`(2,3)`, and exactly one nested block, which is traversed after the nested
tasks `(4)`.
After the nested blocks and tasks are traversed, the outer task itself is
traversed at its single yielded execution point `(5)`.
A broadcast traversal runs to completion before proceeding to the next
statement, just like a function call.

The next example illustrates event broadcasts and tasks traversal.
The example uses an `watching` statement to observe an event condition while
executing a nested task.
When the condition is satisfied, the nested task is aborted:

<!-- pico/tst/ticks.ceu -->

```
spawn {
    watching :done {
        par {
            every :tick {
                println(:tick-A)        ;; always awakes first
            }
        } with {
            every :tick {
                println(:tick-B)        ;; always awakes last
            }
        }
    }
    println(:done)
}
broadcast(:tick)                        ;; --> :tick-A, :tick-B
broadcast(:tick)                        ;; --> :tick-A, :tick-B
broadcast(:done)                        ;; --> :done
println(:the-end)                       ;; --> :the-end
```

The main block has an outermost `spawn` task, which awaits `:done`, and has two
nested tasks awaiting `:tick` events.
Then, the main block broadcasts three events in sequence.
The first two `:tick` events awake the nested tasks respecting the structure of
the program, printing `:tick-A` and `:tick-B` in this order.
The last event aborts the `watching` block and prints `:done`, before
terminating the main block.

## Hierarchical Tags and Tuple Templates

### Hierarchical Tags

Another key aspect of Ceu is its tag type, which is similar to *symbols* or
*atoms* in other programming languages.
A [tag](#basic-type) is a basic type that represents unique values in a
human-readable form.
Any identifier prefixed with a colon (`:`) is a valid tag that is guaranteed to
be unique in comparison to others (i.e., `:x == :x` and `:x /= :y`).
Just like the number `10`, the tag `:x` is a value in itself and needs not to
be declared.
Tags are typically used as keys in dictionaries (e.g., `:x`, `:y`), or as
enumerations representing states (e.g., `:pending`, `:done`).

The next example uses tags as keys in a dictionary:

<!-- dceu/src/test/02-tags.ceu -->

```
val pos = @[]               ;; a new dictionary
set pos[:x] = 10
set pos.y   = 20            ;; equivalent to pos[:y]=20
println(pos.x, pos[:y])     ;; --> 10, 20
```

Tags can also be used to "tag" dynamic objects, such as dictionaries and
tuples, to support the notion of user types in Ceu.
For instance, the call `tag(:Pos,pos)` associates the tag `:Pos` with the
value `pos`, such that the query `tag(pos)` returns `:Pos`.

As an innovative feature, tags can describe user type hierarchies by splitting
identifiers with (`.`).
For instance, a tag such as `:T.A.x` matches the types `:T`, `:T.A`, and
`:T.A.x` at the same time, as verified by function `sup?`:

<!-- dceu/src/test/02-tags.ceu -->

```
sup?(:T,     :T.A.x)    ;; --> true  (:T is a supertype of :T.A.x)
sup?(:T.A,   :T.A.x)    ;; --> true
sup?(:T.A.x, :T.A.x)    ;; --> true
sup?(:T.A.x, :T)        ;; --> false (:T.A.x is *not* a supertype of :T)
sup?(:T.A,   :T.B)      ;; --> false
```

The next example illustrates hierarchical tags combined with the functions
`tag` and `sup?`:

<!-- dceu/src/test/02-tags.ceu -->

```
val x = []                      ;; an empty tuple
tag(:T.A, x)                    ;; x is of user type :T.A
println(tag(x))                 ;; --> :T.A
println(sup?(:T,   tag(x)))     ;; --> true
println(sup?(:T.A, tag(x)))     ;; --> true
println(sup?(:T.B, tag(x)))     ;; --> false
println(x is? :T)               ;; --> true  (equivalent to sup?(:T,tag(x)))
```

In the example, `x` is set to user type `:T.A`, which is compatible with types
`:T` and `:T.A`, but not with type `:T.B`.

### Hierarchical Tuple Templates

Ceu also provides a `data` construct to associate a tag with a tuple template
that enumerates field identifiers.
Templates provide field names for tuples, which become similar to *structs* in
C or *classes* in Java.
Each field identifier in a data declaration corresponds to a numeric index in
the tuple, which can then be indexed by field or by number interchangeably.
The next example defines a template `:Pos`, which serves the same purpose as
the dictionary of the first example, but now using tuples:

<!-- dceu/src/test/03-templates.ceu -->

```
data :Pos = [x,y]       ;; a template `:Pos` with fields `x` and `y`
val pos :Pos = [10,20]  ;; declares that `pos` satisfies template `:Pos`
println(pos.x, pos.y)   ;; --> 10, 20
```

In the example, `pos.x` is equivalent to `pos[0]`, and `pos.y` is equivalent to
`pos[1]`.

The template mechanism of Ceu can also describe a tag hierarchy to support
data inheritance, akin to class hierarchies in Object-Oriented Programming.
A `data` description can be suffixed with a block to nest templates, in which
inner tags reuse fields from outer tags.
The next example illustrates an `:Event` super-type, in which each sub-type
appends additional data to the template:

<!-- dceu/src/test/03-templates.ceu -->

```
data :Event = [ts] {            ;; All events carry a timestamp
    :Key = [key]                ;; :Event.Key [ts,key] is a sub-type of :Event [ts]
    :Mouse = [pos :Pos] {       ;; :Event.Mouse [ts, pos :Pos]
        :Motion = []            ;; :Event.Mouse.Motion [ts, pos :Pos]
        :Button = [but]         ;; :Event.Mouse.Button [ts, pos :Pos, but]
    }
}

val but = :Event.Mouse.Button [0, [10,20], 1]   ;; [ts,[x,y],but]
println(but.ts, but.pos.y, but is :Event.Mouse) ;; --> 0, 20, true
```

Considering the last two lines, a declaration such as
    `val x = :T [...]` is equivalent to
    `val x :T = tag(:T, [...])`,
which not only tags the tuple with the appropriate user type, but also declares
that the variable satisfies the template.

## Integration with C

`TODO`

<!--
The compiler of Ceu converts an input program into an output in C, which is
further compiled to a final executable file.
For this reason, Ceu has source-level compatibility with C, allowing it to
embed native expressions in programs.

- gcc
- :pre
- $x.Tag
- tag,char,bool,number C types
- C errors
-->

# LEXICON

## Keywords

Keywords cannot be used as [variable identifiers](#identifiers).

The following keywords are reserved in Ceu:

<!--
    export              ;; export block
    poly                ;; TODO
-->

```
    and                 ;; and operator                     (00)
    await               ;; await event
    break               ;; loop break
    broadcast           ;; broadcast event
    catch               ;; catch exception
    coro                ;; coroutine prototype
    coroutine           ;; coroutine creation
    data                ;; data declaration
    defer               ;; defer block
    delay               ;; delay task
    do                  ;; do block
    else                ;; else block                       (10)
    enum                ;; enum declaration
    error               ;; throw error
    every               ;; every block
    false               ;; false value
    func                ;; function prototype
    group               ;; group block
    if                  ;; if block
    ifs                 ;; ifs block
    in                  ;; in keyword
    in?                 ;; in? operator
    in-not?             ;; in-not? operator                 (20)
    is?                 ;; is? operator
    is-not?             ;; is-not? operator
    it                  ;; implicit parameter
    loop                ;; loop block
    match               ;; match block
    nil                 ;; nil value
    not                 ;; not operator
    or                  ;; or operator
    par-and             ;; par-and block
    par-or              ;; par-or block
    par                 ;; par block                        (30)
    pub                 ;; public variable
    resume              ;; resume coroutine
    resume-yield-all    ;; resume coroutine
    set                 ;; assign expression
    skip                ;; loop skip
    spawn               ;; spawn coroutine
    task                ;; task prototype/self identifier
    tasks               ;; task pool
    test                ;; test block
    thus                ;; thus pipe block
    toggle              ;; toggle coroutine/block           (40)
    true                ;; true value
    until               ;; until loop condition
    val                 ;; constant declaration
    var                 ;; variable declaration
    watching            ;; watching block
    where               ;; where block
    while               ;; while loop condition
    with                ;; with block
    yield               ;; yield coroutine                  (49)
```

## Symbols

The following symbols are reserved in Ceu:

<!--
    ...             ;; variable function/program arguments
-->

```
    {   }           ;; block/operators delimeters
    (   )           ;; expression delimeters
    [   ]           ;; index/constructor delimeters
    #[              ;; vector constructor delimeter
    @[              ;; dictionary constructor delimeter
    \               ;; lambda declaration
    =               ;; assignment separator
    =>              ;; catch/if/ifs/loop/lambda/thus clauses
    <- ->           ;; method calls
    <-- -->         ;; pipe calls
    ;               ;; sequence separator
    ,               ;; argument/constructor separator
    .               ;; index/field discriminator
    '   "   `       ;; character/string/native delimiters
    $               ;; native interpolation
    ^               ;; lexer preprocessor
```

## Operators

The following operator symbols can be combined to form operator names in Ceu:

```
    +    -    *    /
    %    >    <    =
    |    &    ~
```

Operators names cannot clash with reserved symbols (e.g., `->`).

Examples:

```
|>
<|
+++
```

The following keywords are also reserved as special operators:

```
    not     and     or
    in?     in-not?
    is?     is-not?
```

Operators can be used in prefix or infix notations in
[operations](#calls-and-operations).

## Identifiers

Ceu uses identifiers to refer to variables and operators:

```
ID : [A-Za-z_][A-Za-z0-9_'?!-]*     ;; letter/under/digit/quote/quest/excl/dash
   | `{´ OP `}´                     ;; operator enclosed by braces as identifier
OP : [+-*/%><=|&~]+                 ;; see Operators
```

A variable identifier starts with a letter or underscore (`_`) and is followed
by letters, digits, underscores, single quotes (`'`), question marks (`?`),
exclamation marks (`!`), or dashes (`-`).
A dash must be followed by a letter.

Note that dashes are ambiguous with the minus operator.
For this reason, (i) the minus operation requires spaces between non-numeric
operands (e.g., `x - a`), and (ii) variables with common parts in identifiers
are rejected (e.g., `x` vs `x-a` vs `a-x`).

An operator identifier is a sequence of operator symbols
(see [Operators](#operators)).
An operator can be used as a variable identifier when enclosed by braces (`{`
and `}`).

Examples:

```
x               ;; simple var id
my-value        ;; var with dash
empty?          ;; var with question
map'            ;; var with prime
>               ;; simple op id
++              ;; op with multi chars
{{-}}           ;; op as var id
x-1             ;; invalid identifier (read as `x - 1`)
```

## Literals

Ceu provides literals for *nil*, *booleans*, *tags*, *numbers*, *characters*,
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

The literal `nil` is the single value of the [*nil*](#basic-types) type.

The literals `true` and `false` are the only values of the [*bool*](#basic-types)
type.

A [*tag*](#basic-types) type literal starts with a colon (`:`) and is followed
by letters, digits, dots (`.`), or dashes (`-`).
A dot or dash must be followed by a letter.

A [*number*](#basic-types) type literal starts with a digit and is followed by
digits, letters, and dots (`.`), and is represented as a *C float*.

A [*char*](#basic-types) type literal is a single or backslashed (`\`)
character enclosed by single quotes (`'`), and is represented as a *C char*.

A string literal is a sequence of characters enclosed by double quotes (`"`).
It is expanded to a [vector](#collection-values) of character literals, e.g.,
`"abc"` expands to `#['a','b','c']`.

A native literal is a sequence of characters interpreted as C code enclosed by
multiple back quotes (`` ` ``).
The same number of backquotes must be used to open and close the literal.
Native literals are detailed next.

All literals are valid [values](#values) in Ceu.

Examples:

```
nil                 ;; nil literal
false               ;; bool literal
:X.Y                ;; tag literal
1.25                ;; number literal
'a'                 ;; char literal
"Hello!"            ;; string literal
`puts("hello");`    ;; native literal
```

### Tags

The following tags are pre-defined in Ceu:

```
    ;; type enumeration

    :nil :tag :bool :char :number :pointer          ;; basic types
    :tuple :vector :dict                            ;; collections
    :func :coro :task                               ;; prototypes
    :exe-coro :exe-task                             ;; active coro/task
    :tasks                                          ;; task pool

    :ceu :pre                                       ;; native ceu value/pre code
    :yielded :toggled :resumed :terminated          ;; coro/task status
    :h :min :s :ms                                  ;; time unit
    :idx :key :val                                  ;; iterator modifier
    :global :task                                   ;; broadcast target

    :dynamic :error :nested                         ;; internal use
```

### Native Literals

A native literal can specify a tag modifier as follows:

```
`:<type> <...>`
`:ceu <...>`
`:pre <...>`
`<...>`
```

The `:<type>` modifier assumes that the C code in `<...>` evaluates to an
expression of the given type and converts it to Ceu.
The `:ceu` modifier assumes that the code is already a value in Ceu and does
not convert it.

The `:pre` modifier or lack of modifier assumes that the code is a C statement
that does not evaluate to an expression.
With the `:pre` modifier, the statement is placed at the top of the
[C output file](#TODO), such that it can include pre declarations.

Native literals can evaluate Ceu variable identifiers using a dollar sign
prefix (`$`) and a dot suffix (`.`) with one of the desired basic types:
    `.Tag`, `.Bool`, `.Char`, `.Number`, `.Pointer`.

Examples:

```
val n = `:number 10`            ;; native 10 is converted to Ceu number
val x = `:ceu $n`               ;; `x` is set to Ceu `n` as is
`printf("> %f\n", $n.Number);`  ;; outputs `n` as a number
```

## Comments

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

# TYPES

Ceu provides dynamic types such that values carry their own types during
execution.

The function `type` returns the type of a value as a [tag](#basic-types):

```
type(10)  --> :number
type('x') --> :char
```

## Basic Types

Ceu has 6 basic types:

```
nil    bool    char    number    tag    pointer
```

The `nil` type represents the absence of values with its single value
[`nil`](#literals).

The `bool` type represents boolean values with [`true`](#literals) and
[`false`](#literals).
In a boolean context, `nil` and `false` are interpreted as `false` and all
other values from all other types are interpreted as `true`.

The `char` type represents [character literals](#literals).

The `number` type represents real numbers (i.e., *C floats*) with
[number literals](#literals).

The `tag` type represents [tag identifiers](#literals).
Each tag is internally associated with a natural number that represents a
unique value in a global enumeration.
Tags can be explicitly [enumerated](#tag-enumerations-and-tuple-templates) to
interface with [native expressions](#literals).
Tags can form [hierarchies](#hierarchical-tags) to represent
[user types](#user-types) and describe
[tuple templates](#tag-enumerations-and-tuple-templates).

The `pointer` type represents opaque native pointer values from [native
literals](#literals).

## Collections

Ceu provides 3 types of collections:

```
tuple    vector    dict
```

The `tuple` type represents a fixed collection of heterogeneous values, in
which each numeric index, starting at `0`, holds a value of a (possibly)
different type.

The `vector` type represents a variable collection of homogeneous values, in
which each numeric index, starting at `0`,  holds a value of the same type.
Once the first index is assigned, its type becomes the type of the vector,
which further assignments must respect.

The `dict` type (dictionary) represents a variable collection of heterogeneous
values, in which each index (or key) of any type maps to a value of a
(possibly) different type.

Examples:

```
[1, 'a', nil]           ;; a tuple with 3 values
#[1, 2, 3]              ;; a vector of numbers
@[(:x,10), (:y,20)]     ;; a dictionary with 2 mappings
```

## Execution Units

Ceu provide 3 types of execution units: functions, coroutines, and tasks:

```
func      coro      task
exe-coro  exe-task  tasks
```

The `func` type represents [function prototypes](#prototype-values).

The `coro` type represents [coroutine prototypes](#prototype-values), while the
`exe-coro` type represents [active coroutines](#active-values).

The `task` type represents [task prototypes](#prototype-values), while the
`exe-task` type represents [active tasks](#active-values).
The `tasks` type represents [task pools](#active-values) holding active tasks.

## User Types

Values of non-basic types (i.e., collections and execution units) can be
associated with [tags](#basic-types) that represent user types.

The function [`tag`](#types-and-tags) associates tags with values:

```
val x = []          ;; an empty tuple
tag(:T, x)          ;; x is now of user type :T
println(tag(x))     ;; --> :T
```

Tags form [type hierarchies](hierarchical-tags) based on the dots in their
identifiers, i.e., `:T.A` and `:T.B` are sub-types of `:T`.
Tag hierarchies can nest up to 4 levels.

The function [`sup?`](#types-and-tags) checks super-type relations between
tags:

```
println(sup?(:T, :T.A)    ;; --> true
println(sup?(:T.A, :T)    ;; --> false
println(sup?(:T.A, :T.B)  ;; --> false
```

The function [`is?`](#operator-is) checks if values match types or tags:

```
val x = []              ;; an empty tuple
tag(:T.A, x)            ;; x is now of user type :T.A
println(x is? :tuple)   ;; --> true
println(x is? :T)       ;; --> true
```

User types do not require to be predeclared, but can appear in [tuple
template](#tag-enumerations-and-tuple-templates) declarations.

# VALUES

As a dynamically-typed language, each value in Ceu carries extra information,
such as its own type.

## Static Values

A *static value* does not require dynamic allocation.
All [basic types](#basic-types) have [literal](#literals) values:

```
Types : nil | bool | char | number | tag | pointer
Lits  : `nil´ | `false´ | `true´ | CHR | NUM | TAG | NAT
```

Static values are immutable and are transferred between variables and across
blocks as a whole copies without any restrictions.

## Dynamic Values

A *dynamic value* requires dynamic allocation since its internal data is either
variable or too big to fit as a static value.
The following types have dynamic values:

```
Colls  : tuple | vector | dict          ;; collections
Protos : func | coro | task             ;; prototypes
Actvs  : exe-coro | exe-task | tasks    ;; active values (next section)
```

Unlike static values, dynamic values are mutable and are transferred between
variables and across blocks through references.
As a consequence, multiple references may point to the same mutable value.

Ceu uses reference counting to determine the life cycle of dynamic values.
When the reference counter reaches zero, the dynamic value is immediately
deallocated from memory.
Note that mutually referenced values are never deallocated.
Therefore, programmers need to break reference cycles manually.

### Collection Values

Ceu provides constructors for [collections](#collections) to allocate tuples,
vectors, and dictionaries:

```
Cons : `[´ [List(Expr)] `]´             ;; tuple
     | `#[´ [List(Expr)] `]´            ;; vector
     | `@[´ [List(Key-Val)] `]´         ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´
     | STR
     | TAG `[´ [List(Expr)] `]´         ;; tagged tuple
```

Tuples (`[...]`) and vectors (`#[...]`) are built providing a list of
expressions.

Dictionaries (`@[...]`) are built providing a list of pairs of expressions
(`(key,val)`), in which each pair maps a key to a value.
The first expression is the key, and the second is the associated value.
If the key is a tag, the alternate syntax `tag=val` may be used (omitting the
tag colon prefix `:`).

A [string literal](#literals) expands to a vector of character literals.

A tuple constructor may also be prefixed with a tag, which associates the tag
with the tuple, e.g., `:X [...]` is equivalent to `tag(:X, [...])`.
Tag constructors are typically used in conjunction with
[tuple templates](#tag-enumerations-and-tuple-templates)

Examples:

```
[1,2,3]             ;; a tuple
:Pos [10,10]        ;; a tagged tuple
#[1,2,3]            ;; a vector
"abc"               ;; a character vector ['a','b','c']
@[(:x,10), x=10]    ;; a dictionary with equivalent key mappings
```

### Prototype Values

Ceu supports functions, coroutines, and tasks as prototype values:

```
Func : `func´ `(´ [List(ID [TAG])] `)´ Block
Coro : `coro´ `(´ [List(ID [TAG])] `)´ Block
Task : `task´ `(´ [List(ID [TAG])] `)´ Block
```

Parameter declarations are equivalent to immutable `val`
[declarations](#declarations) and can also be associated with
[tuple template](#tag-enumerations-and-tuple-templates) tags.

<!--
The last parameter can be the symbol
[`...`](#declarations), which captures as a tuple all
remaining arguments of a call.

The symbol `...` represents the variable arguments (*varargs*) a function
receives in a call.
In the context of a [function](#prototype-values) that expects varargs, it
evaluates to a tuple holding the varargs.
In other scenarios, it evaluates to a tuple holding the program arguments.
When `...` is the last argument of a call, its tuple is expanded as the last
arguments.
-->

The associated block executes when the unit is [invoked](#TODO).
Each argument in the invocation is evaluated and copied to the parameter
identifier, which becomes an local variable in the execution block.

A *closure* is a prototype that accesses variables from outer blocks, known as
*upvalues*.
Ceu supports a restricted form of closures, in which *upvalues* must be
immutable (thus declared with the modifier [`val`](#declarations)).

Examples:

```
func (v) { v }          ;; a function
coro () { yield() }     ;; a coroutine
task () { await(:X) }   ;; a task

func (v1) {             ;; a closure
    func () {
        v1              ;; v1 is an upvalue
    }
}
```

#### Lambda Prototype

For simple `func` prototypes, Ceu supports the lambda notation:

```
Lambda : `\´ `{´ [`,´ ID [TAG] `=>´]  { Expr [`;´] }`}´
```

The expression `\{ ,<id> <tag> => <es> }` expands to

```
func (<id> <tag>) {
    <es>
}
```

If the identifier is omitted, it assumes the single implicit parameter `it`.

Examples:

```
val f = \{ ,x => x+x }  ;; f doubles its argument
println(\{it}(10))      ;; prints 10
```

### Active Values

An *active value* corresponds to an active coroutine, task, or task pool:

```
exe-coro  exe-task  tasks
```

Active coroutines and tasks are running instances of
[prototypes](#prototype-values) that can suspend their execution before they
terminate.
After they suspend, coroutines and tasks retain their execution state and can
be resumed later from their previous suspension point.

Coroutines and tasks have 4 possible status:

- `yielded`: idle and ready to be resumed
- `toggled`: ignoring resumes (only for tasks)
- `resumed`: currently executing
- `terminated`: terminated and unable to resume

The main difference between coroutines and tasks is how they resume execution:

- A coroutine resumes explicitly from a [resume operation](#resume).
- A task resumes implicitly from a [broadcast operation](#broadcast).

Before a coroutine or task is [deallocated](#dynamic-values), it is implicitly
aborted, and all active [defer statements](#defer) execute automatically in
reverse order.

A task is lexically attached to the block in which it is created, such that
when the block terminates, the task is implicitly terminated (triggering active
defers).

A task pool groups related active tasks as a collection.
A task that lives in a pool is lexically attached to the block in which the
pool is created, such that when the block terminates, all tasks in the pool are
implicitly terminated (triggering active defers).

The operations on [coroutines](#coroutine-operations) and
[tasks](#tasks-operations) are discussed further.

Examples:

```
coro C () { <...> }         ;; a coro prototype `C`
val c = coroutine(C)        ;; is instantiated as `c`
resume c()                  ;; and resumed explicitly

val ts = tasks()            ;; a task pool `ts`
task T () { <...> }         ;; a task prototype `T`
val t = spawn T() in ts     ;; is instantiated as `t` in pool `ts`
broadcast(:X)               ;; broadcast resumes `t`
```

# STATEMENTS

Ceu is an expression-based language in which all statements are expressions and
evaluate to a value.

## Program, Sequences and Blocks

A program in Ceu is a sequence of statements (expressions), and a block is a
sequence of expressions enclosed by braces (`{` and `}´):

```
Prog  : { Expr [`;´] }
Block : `{´ { Expr [`;´] } `}´
```
Each expression in a sequence may be separated by an optional semicolon (`;´).
A sequence of expressions evaluate to its last expression.

<!-- TODO: ; to remove ambiguity -->

<!--
The symbol
[`...`](#declarations) stores the program arguments
as a tuple.
-->

### Blocks

A block delimits a lexical scope for
[variables](#declarations) and [tasks](#active-values):
A variable is only visible to expressions in the block in which it was
declared.
A task is automatically terminated when the block in which it was created
terminates.

A block is not an expression by itself, but it can be turned into one by
prefixing it with an explicit `do`:

```
Do : `do´ Block       ;; an explicit block statement
```

Blocks also appear in compound statements, such as
[conditionals](#conditionals-and-pattern-matching),
[loops](#loops-and-iterators), and many others.

Examples:

```
do {                    ;; block prints :ok and evals to 1
    println(:ok)
    1
}

do {
    val a = 1           ;; `a` is only visible in the block
    <...>
}
a                       ;; ERR: `a` is out of scope

do {
    spawn T()           ;; spawns task T and attaches it to the block
    <...>
}                       ;; aborts spawned task
```

### Groups

A `group` is a nested sequence of expressions:

```
Group : group `{´ { Expr [`;´] } `}´
```

Unlike [blocks](#blocks), a group does not create a new scope for variables
and tasks.
Therefore, all nested declarations remain active as if they were declared on
the enclosing block.

Examples:

```
group {
    val x = 10
    val y = x * 2
}
println(x, y)       ;; --> 10 20
```

### Defers

A `defer` block executes only when its enclosing block terminates:

```
Defer : `defer´ Block
```

Deferred blocks execute in reverse order in which they appear in the source
code.

Examples:

```
do {
    println(1)
    defer {
        println(2)      ;; last to execute
    }
    defer {
        println(3)
    }
    println(4)
}                       ;; --> 1, 4, 3, 2
```

## Declarations and Assignments

### Declarations

All variables in Ceu must be declared before use:

```
Val : `val´ ID [TAG] [`=´ Expr]         ;; constants
Var : `var´ ID [TAG] [`=´ Expr]         ;; variables
```

`TODO: patterns`

The optional initialization expression assigns an initial value to the
variable, which is set to `nil` otherwise.

The difference between `val` and `var` is that a `val` is immutable, while a
`var` declaration can be modified by further `set` statements:
The `val` modifier forbids that its name is reassigned, but it does not prevent
a [dynamic value](#dynamic-values) it is holding to be modified.

Optionally, a declaration can be associated with a
[tuple template](#tag-enumerations-and-tuple-templates) tag, which allows the
variable to be indexed by a field name, instead of a numeric position.
Note that the variable is not guaranteed to hold a value matching the template.
The template association is static but with no runtime guarantees.

If the declaration omits the template tag, but the initialization expression is
a [tag constructor](#collection-values), then the variable assumes this tag
template, i.e., `val x = :X []` expands to `val x :X = :X []`.

An [execution unit](#execution-units) [prototype](#prototype-values) can be
declared as an immutable variable as follows:

```
Proto : `func´ ID `(´ [List(ID)] `)´ Block
      | `coro´ ID `(´ [List(ID)] `)´ Block
      | `task´ ID `(´ [List(ID)] `)´ Block
```

### Assignments

The `set` statement assigns the value in the right of `=` to the location in
the left:

```
Set : `set´ Expr `=´ Expr
```

`TODO: valid locations - acc/idx/pub`

Examples:

```
var x
set x = 20              ;; OK

val y = [10]
set y = 0               ;; ERR: cannot reassign `y`
set y[0] = 20           ;; OK

val p1 :Pos = [10,20]   ;; (assumes :Pos has fields [x,y])
println(p1.x)           ;; --> 10

val p2 = :Pos [10,20]   ;; (assumes :Pos has fields [x,y])
println(p2.y)           ;; --> 20
```

## Tag Enumerations and Tuple Templates

[Tags](#hierarchical-tags) are global identifiers that need not to be
predeclared.
However, they may be explicitly declared when used as enumerations or tuple
templates.

### Tag Enumerations

An `enum` groups related tags together as a sequence:

```
Enum : `enum´ `{´ List(TAG) `}´
     | `enum´ TAG `{´ List(ID) `}´
```

The first variation declares the tags in the given list.
The second variation declares as tags the identifiers in the list with the
given tag as a prefix, separated by a dash (`-`).

Enumerations can be used to interface with external libraries that use
constants to represent a group of related values (e.g., key symbols).
When [converted to numbers](#TODO-to.number), the tags in enumerations are
guaranteed to form a sequence.

Examples:

```
enum { :x, :y, :z } ;; declares :x, :y, :z in sequence
to.number(:x)       ;; --> 100
to.number(:y)       ;; --> 101
to.number(:z)       ;; --> 102

enum :Key {
    Left,           ;; declares :Key-Left (200)
    Up,             ;; declares :Key-Up   (201)
    Right,          ;; ...
    Down,           ;; ...
}
```

### Tuple Templates

A `data` declaration associates a tag with a tuple template, which associates
tuple positions with field identifiers:

```
Template : `data´ Data
                Data : TAG `=´ `[´ List(ID [TAG]) `]´
                    [`{´ { Data } `}´]
```

After the keyword `data`, a declaration expects a tag followed by `=` and a
template.
A template is surrounded by brackets (`[´ and `]´) to represent the tuple, and
includes a list of identifiers, each mapping an index into a field.
Each field can be followed by a tag to represent nested templates.

A [variable declaration](#declarations) can specify a tuple template and hold a
tuple that can be accessed by field.

Examples:

```
data :Pos = [x,y]                       ;; a flat template
val pos :Pos = [10,20]                  ;; pos uses :Pos as template
println(pos.x, pos.y)                   ;; --> 10, 20

data :Dim = [w,h]
data :Rect = [pos :Pos, dim :Dim]       ;; a nested template
val r1 :Rect = [pos, [100,100]]         ;; r uses :Rect as template
println(r1.dim, r1.pos.x)               ;; --> [100,100], 10

val r2 = :Rect [[0,0],[10,10]]          ;; combining tag template/constructor
println(r2 is? :Rect, r2.dim.h)         ;; --> true, 10
```

Based on [tags and sub-tags](#user-types), tuple templates can define
hierarchies and reuse fields from parents.
A declaration can be followed by a list of sub-templates enclosed by curly
braces (`{` and `}`), which can nest to at most 4 levels.
Each nested tag identifier assumes an implicit prefix of its super-tag, e.g.,
in the context of tag `:X`, a sub-tag `:A` is actually `:X.A`.
Templates are reused by concatenating a sub-template after its corresponding
super-templates, e.g., `:X.A [a]` with `:X [x]` becomes `:X.A [x,a]`.

Examples:

```
data :Event = [ts] {            ;; All events carry a timestamp
    :Key = [key]                ;; :Event.Key [ts,key] is a sub-type of :Event [ts]
    :Mouse = [pos :Pos] {       ;; :Event.Mouse [ts, pos :Pos]
        :Motion = []            ;; :Event.Mouse.Motion [ts, pos :Pos]
        :Button = [but]         ;; :Event.Mouse.Button [ts, pos :Pos, but]
    }
}

val but = :Event.Mouse.Button [0, [10,20], 1]
val evt :Event = but
println(evt.ts, but.pos.y)      ;; --> 0, 20
```

## Calls, Operations and Indexing

### Calls and Operations

In Ceu, calls and operations are equivalent, i.e., an operation is a call that
uses an [operator](#operatos) with prefix or infix notation:

```
Expr : OP Expr                      ;; unary operation
     | Expr OP Expr                 ;; binary operation
     | Expr `(´ [List(Expr)] `)´    ;; function call
```

Operations are interpreted as function calls, i.e., `x + y` is equivalent to
`{+} (x, y)`.

A call expects an expression of type [`func`](#prototype-values) and an
optional list of expressions as arguments enclosed by parenthesis.
A call transfers control to the function, which runs to completion and returns
a value, which substitutes the call.

As discussed in [Identifiers](#identifiers), the binary minus requires spaces
around it to prevent ambiguity with identifiers containing dashes.

Examples:

```
#vec            ;; unary operation
x - 10          ;; binary operation
{{-}}(x,10)     ;; operation as call
f(10,20)        ;; normal call
```

#### Pipe Calls

A pipe is an alternate notation to call a function:

```
Expr : Expr (`<--` | `<-` | `->` | `-->` ) Expr
```

The operators `<--` and `<-` pass the argument in the right to the function in
the left, while the operators `->` and `-->` pass the argument in the left to
the function in the right.

The single pipe operators `<-` and `->` have higher
[precedence](@precedence-and-associativity) than the double pipe operators
`<--` and `-->`.

If the receiving function is actually a call, then the pipe operator inserts
the extra argument into the call either as first (`->` and `-->`) or last (`<-`
and `<--`).


Examples:

```
f <-- 10 -> g   ;; equivalent to `f(g(10))`
t -> f(10)      ;; equivalent to `f(t,10)`
```

### Indexes and Fields

[Collections](#collections) in Ceu are accessed through indexes or fields:

```
Expr : Expr `[´ Expr `]´        ;; Index
     | Expr `.´ ID              ;; Field
     | Expr `.´ `pub´ | `pub´   ;; Pub
```

An index operation expects a collection and an index enclosed by brackets (`[`
and `]`).
For tuples and vectors, the index must be an number.
For dictionaries, the index can be of any type.
The operation evaluates to the current value in the given collection index, or
`nil` if non existent.

A field operation expects a dictionary or a tuple template, a dot separator
(`.`), and a field identifier.
If the collection is a dictionary `d`, the field must be a
[tag literal](#literals) `k` (with the colon prefix `:` omitted), which is
equivalent to the index operation `v[:k]`.
If the collection is a [tuple template](#tag-enumerations-and-tuple-templates)
`t`, the field must be an identifier that maps to a template index `i`, which
is equivalent to the index operation `t[i]`.

A `pub` operation accesses the public field of an [active task](#active-values)
and is discussed [further](#task-operations).

Examples:

```
tup[3]              ;; tuple access by index
vec[i]              ;; vector access by index

dict[:x]            ;; dict access by index
dict.x              ;; dict access by field

val t :T            ;; tuple template
t.x

val t = spawn T()
t.pub               ;; public field of task
```

#### Template Casting

An expression can be suffixed with a tag between parenthesis such that it is
cast into a tuple template:

```
Expr : Expr `.´ `(´ TAG `)´
```

Examples:

```
data :Pos = [x,y]
val p = <...>
println(p.(:Pos).x)     ;; `p` is cast to `:Pos`
```

#### Peek, Push, Pop

The *ppp operators* (peek, push, pop) manipulate a vector as a stack:

```
Expr : Expr `[´ (`=´|`+´|`-´) `]´
```

A peek operation `vec[=]` sets or gets the last element of a vector.
The push operation `vec[+]` adds a new element to the end of a vector.
The pop operation `vec[-]` gets and removes the last element of a vector.

Examples:

```
val stk = #[1,2,3]
println(stk[=])         ;; --> 3
set stk[=] = 30
println(stk)            ;; --> #[1, 2, 30]
println(stk[-])         ;; --> 30
println(stk)            ;; --> #[1, 2]
set stk[+] = 3
println(stk)            ;; --> #[1, 2, 3]
```

### Where and Thus Clauses

Any expression can be suffixed by `where` and `thus` clauses:

```
Expr : Expr `where´ Block
     | Expr `thus´ `{´ [ID [TAG] `=>´]  { Expr [`;´] }`}´
```

A `where` clause executes its block before the prefix expression and is allowed
to declare variables that can be used by the expression.

A `thus` clause assigns the result of the prefix expression into the given
identifier, and then executes a block.
If the identifier is omitted, it assumes the implicit identifier `it`.
Like in [declarations](#declarations), the identifier can be associated with
[tuple template](#tag-enumerations-and-tuple-templates) tags.
A clause such as `e1 thus { v => e2 }` is equivalent to `e1 -> \{ v => e2 }`,
which combines [pipes](#pipe-calls) and [lambda](#lambda-prototype)
expressions.

Examples:

```
var x = (2 * y) where { var y=10 }  ;; x=20
(x * x) thus { x2 => println(x2) }  ;; --> 400
```

### Precedence and Associativity

Operations in Ceu can be combined in complex expressions with the following
precedence priority (from higher to lower):

```
1. suffix (left associative)
    - call:         `f()` `{op}()`
    - index:        `t[i]` `t[=]` `t[+]` `t[-]`
    - field:        `t.x` `t.pub` `t.(:T)`
2. inner (left associative)
    - single pipe:  `v->f` `f<-v`
2. prefix (right associative)
    - unary:        `not v` `#t` `-x`
    - constructor:  `:T []` (see [Collection Values](#collection-values))
3. infix (left associative)
    - binary        `x*y` `r++s` `a or b`
4. outer operations (left associative)
    - double pipe:  `v-->f` `f<--v`
    - where:        `v where {...}`
    - thus:         `v thus {...}`
```

All operations are left associative, except prefix operations, which are right
associative.
Note that all binary operators have the same precedence.
Therefore, expressions with different operators but with the same precedence
require parenthesis for disambiguation:

```
Expr : `(´ Expr `)´
```

Examples:

```
#f(10).x        ;; # ((f(10)) .x)
x + 10 - 1      ;; ERR: requires parenthesis
- x + y         ;; (-x) + y
x or y or z     ;; (x or y) or z
```

## Conditionals and Pattern Matching

### Conditionals

Ceu supports conditionals as follows:

```
If  : `if´ Expr (Block | `=>´ Expr)
        [`else´  (Block | `=>´ Expr)]
Ifs : `ifs´ `{´ {Case} [Else] `}´
        Case :  Expr  (Block | `=>´ Expr)
        Else : `else´ (Block | `=>´ Expr)
```

An `if` tests a condition expression and executes one of the two possible
branches.
If the condition is [true](#basic-types), the `if` executes the first branch.
Otherwise, it executes the optional `else` branch, which defaults to `nil`.
A branch can be either a [block](#blocks) or a simple expression prefixed
by the arrow symbol `=>`.

An `ifs` supports multiple conditions, which are tested in sequence, until one
is satisfied, executing its associated branch.
Otherwise, it executes the optional `else` branch, which defaults to `nil`.

Examples:

```
val max = if x>y => x => y

ifs {
    x > y => x
    x < y => y
    else  => error("values are equal")
}
```

### Pattern Matching

An `ifs` also supports a head expression to be compared in test cases using
patterns:

```
Ifs : <see above>
    | `ifs´ Expr `{´ {Case} [Else] `}´
        Case :  Patt  (Block | `=>´ Expr)
        Else : `else´ (Block | `=>´ Expr)

Patt : [`(´] (Cons | Oper | Full) [`)´]
        Cons : Expr
        Oper : OP [Expr]
        Full : [ID] [TAG] [`,´ [Expr]]
```

A pattern is enclosed by optional parenthesis and has three possible forms:

- A *constructor pattern* `Cons` compares the head value against the given
    expression using the operator [`===`](#deep-equality-operators).
    The expression must be either a [static literal](#static-values) or a
    [collection constructor](#collection-values).
- An *operation pattern* `Oper` calls the given predicate passing the head
    value and the optional expression.
- A *full pattern* `Full` is composed of a
    [variable declaration](#declarations] with identifier and tag, and a
    condition expression.
    The pattern assigns the head expression to the variable, which is visible
    by the condition expression and case branch.
    The pattern checks if the head expression matches the given tag using the
    operator [`is?`](#operator-is] and if the condition expression is
    satisfied.
    The identifier, tag and condition are all optional, but the identifier
    cannot appear alone, requiring a compaining tag or `,`.
    The identifier defaults to `it`.

Note that a tag pattern, such as `:X`, satisfies both the `Cons` and `Full`
forms.
Nevertheless, it always assumes the `Full` form and uses the operator `is?` to
match against the head expression.

Examples:

```
ifs f() {
    [1,2,3]    => println("f() === [1,2,3]")
    >= g()     => println("f() >= g()")
    :tuple     => println("f() is? :tuple")
    :T, g(it)  => println("it=f(), (it is? :T) and g(it)")
    x,         => println("f() = ", x)
    else {
        error("impossible case")
    }
}
```

## Loops and Iterators

Ceu supports loops and iterators as follows:

```
Loop : `loop´ [ID [TAG]] [`in´ (Range | Expr)] Block

Skip  : `skip´ `if´ Expr

Break : `break´ [`(´ Expr `)´] `if´ Expr
      | `until´ Expr
      | `while´ Expr
```

A `loop` executes a block of code continuously until a termination condition is
met.

The optional `in` clause extends a loop with an iterator expression and an
optional control variable.
The iterator can be a [numeric range](#numeric-ranges) or an expression that
evaluates to an [iterator tuple](#TODO).
If the variable identifier is omitted, it assumes the implicit identifier `it`.
If the `in` clause is omitted, but not the the variable identifier, then the
loop iterates over the variable from `0` to infinity.

The loop block may contain control clauses to restart or terminate the loop.
These clauses must be placed at the exact same nesting level of the loop block,
but not at lower levels.
The `skip` clause jumps back to the next loop iteration if the given condition
is true.
The `break`, `until`, or `while` terminate the loop if the given condition is
met.
The loop as a whole evaluates to the terminating condition, or to the optional
expression in the case of a `break`.

Examples:

```
var i = 0
loop {
    set i = i + 1
    skip if (i % 2) == 1
    println(i)          ;; --> 2,4,6,8,10
    while i <= 10
}
```

```
loop {
    do {
        break if true   ;; ERR: invalid nesting level
    }
}
```

```
loop i {
    println(i)          ;; --> 0,1,2,...
}
```

### Numeric Ranges

In a numeric loop, the `in` clause specifies an interval `x => y` to iterate
over.
The clause chooses open or closed range delimiters, and an optional signed
`:step` expression to apply at each iteration:

```
Range : (`{´ | `}´) Expr `=>` Expr (`{´ | `}´) [`:step` [`+´|`-´] Expr]
```

The open delimiters `}x` and `y{` exclude the given numbers from the interval,
while the closed delimiters `{x` and `y}` include them.

The loop terminates when `x` reaches or surpasses `y` considering the step sign
direction, which defaults to `+1`.
At each step, the loop control variable assumes the current iteration value.
After each step, the step is added to `x` and compared against `y`.

Note that for an open delimiter, the loop initializes the given number to its
successor or predecessor, depending on the step direction.

```
loop in {0 => 5{ {
    println(it)     ;; --> 0,1,2,3,4
}

loop v in }3 => 0} :step -1 {
    println(v)      ;; --> 2, 1, 0
}
```

### Iterator Tuples

In an iterator loop, the `in` clause specifies an expression that must evaluate
to an iterator tuple `[f,...]` [tagged](#user-types) as `:Iterator`.
If the given expression is not an iterator tuple, the loop tries to transform
it calling `to-iter` implicitly.

The iterator tuple must hold a step function `f` at index `0`, followed by any
other state required to operate.
The function `f` expects the iterator tuple itself as argument, and returns its
next value or `nil` to signal termination.

The loop calls the step function at each iteration passing the given iterator
tuple, and assigns the result to the loop control variable.
When the result is `nil`, the loop terminates.

The function [`to-iter`](#iterator) in the
[auxiliary library](#auxiliary-library) creates iterators from iterables, such
as vectors, coroutines, and task pools, such that they can be traversed in
loops.

Examples:

```
loop v in [10,20,30] {          ;; implicit to-iter([10,20,30])
    println(v)                  ;; --> 10,20,30
}


func num-iter (N) {
    val f = func (t) {
        val v = t[2]
        set t[2] = v + 1
        ((v < N) and v) or nil
    }
    :Iterator [f, N, 0]
}
loop in num-iter(5) {
    println(it)                 ;; --> 0,1,2,3,4
}
```

## Exceptions

The `error` expression raises an exception that aborts the execution of all
enclosing blocks up to a matching `catch` block.

```
Error : `error´ `(´ Expr `)´
Catch : `catch´ [Patt] Block
```

An `error` propagates upwards and aborts all enclosing [blocks](#blocks) and
[execution units](#prototype-values) (functions, coroutines, and tasks) on the
way.
When crossing an execution unit, an `error` jumps back to the original calling
site and continues to propagate upwards.

A `catch` executes its associated block normally, but also registers a
[condition pattern](#pattern-matching) to be compared against the exception
value when an `error` is crossing it.
If they match, the exception is caught and the `catch` terminates and evaluates
to exception value, also aborting its associated block, and properly triggering
nested [`defer`](#defer) statements.

Examples:

```
val x = catch :Error {
    error(:Error)
    println("unreachable")
}
println(x)              ;; --> :Error
```

```
catch 1 {               ;; catches
    defer {
        println(1)
    }
    catch 2 {           ;; no catches
        defer {
            println(2)
        }
        error(1)        ;; throws
        ;; unreachable
    }
    ;; unreachable
}                       ;; --> 2, 1
```

```
func f () {
    catch :Err.One {                  ;; catches specific error
        defer {
            println(1)
        }
        error(:Err.Two ["err msg"])   ;; throws another error
    }
}
catch :Err {                          ;; catches generic error
    defer {
        println(2)
    }
    f()
    ;; unreachable
}                                     ;; --> 1, 2
```

## Coroutine Operations

The API for coroutines has the following operations:

- [`coroutine`](#coroutine-create): creates a new coroutine from a prototype
- [`status`](#coroutine-status): consults the coroutine status
- [`resume`](#spawn): starts or resumes a coroutine
- [`yield`](#yield): suspends the resumed coroutine

<!--
5. [`abort`](#TODO): `TODO`
-->

Note that `yield` is the only operation that is called from the coroutine
itself, all others are called from the user code controlling the coroutine.
The `resume` and `yield` operations transfer values between themselves,
similarly to calls and returns in functions.

Examples:

```
coro C (x) {                ;; first resume
    println(x)              ;; --> 10
    val w = yield(x + 1)    ;; returns 11, second resume, receives 12
    println(w)              ;; --> 12
    w + 1                   ;; returns 13
}
val c = coroutine(C)        ;; creates `c` from prototype `C`
val y = resume c(10)        ;; starts  `c`, receives `11`
val z = resume c(y+1)       ;; resumes `c`, receives `13`
println(status(c))          ;; --> :terminated
```

```
coro C () {
    defer {
        println("aborted")
    }
    yield()
}
do {
    val c = coroutine(C)
    resume c()
}                           ;; --> aborted
```

### Coroutine Create

The operation `coroutine` creates a new [active coroutine](#active-values) from
a [coroutine prototype](#prototype-values):

```
Create : `coroutine´ `(´ Expr `)´
```

The operation `coroutine` expects a coroutine prototype (type
[`coro`](#execution-units)) and returns its active reference (type
[`exe-coro`](#execution-units)).

Examples:

```
coro C () {
    <...>
}
val c = coroutine(C)
println(C, c)   ;; --> coro: 0x... / exe-coro: 0x...
```

<!--
5. [`abort`](#TODO): `TODO`
-->

### Coroutine Status

The operation `status` returns the current state of the given active coroutine:

```
Status : `status´ `(´ Expr `)´
```

As described in [Active Values](#active-values), a coroutine has 3 possible
status:

1. `yielded`: idle and ready to be resumed
2. `resumed`: currently executing
3. `terminated`: terminated and unable to be resumed

Examples:

```
coro C = coro () {
    yield()
}
val c = coroutine(C)
println(status(c))      ;; --> :yielded
resume c()
println(status(c))      ;; --> :yielded
resume c()
println(status(c))      ;; --> :terminated
```

### Resume

The operation `resume` executes a coroutine starting from its last suspension
point:

```
Resume : `resume´ Expr `(´ [Expr] `)´
```

The operation `resume` expects an active coroutine, and resumes it, passing an
optional argument.
The coroutine executes until it yields or terminates.
The `resume` evaluates to the argument of `yield` or to the coroutine return
value.

```
coro C () {
    println(:1)
    yield()
    println(:2)
}
val co = coroutine(C)
resume co()     ;; --> 1
resume co()     ;; --> 2
```

### Yield

The operation `yield` suspends the execution of a running coroutine:

```
Yield : `yield´ `(´ [Expr] `)´
```

An `yield` expects an expression between parenthesis that is returned to whom
resumed the coroutine.
Eventually, the suspended coroutine is resumed again with a value and the whole
`yield` is substituted by that value.

<!--
If the resume came from a [`broadcast`](#broadcast), then the given expression is
lost.
-->

### Resume/Yield All

The operation `resume-yield-all´ continuously resumes the given active
coroutine, collects its yields, and yields upwards each value, one at a time.
It is typically use to delegate a job of an outer coroutine transparently to an
inner coroutine:

```
All : `resume-yield-all´ Expr `(´ [Expr] `)´
```

The operation expects an active coroutine and an optional initial resume value
between parenthesis, which defaults to `nil`.
A `resume-yield-all <co> (<arg>)` expands as follows:

```
do {
    val co  = <co>                  ;; given active coroutine
    var arg = <arg>                 ;; given initial value (or nil)
    loop {
        val v = resume co(arg)      ;; resumes with current arg
        if (status(co) /= :terminated) or (v /= nil) {
            set arg = yield(v)      ;; takes next arg from upwards
        }
        until (status(co) == :terminated)
    }
    arg
}
```

The loop in the expansion continuously resumes the target coroutine with a
given argument, collects its yielded value, yields the same value upwards.
Then, it expects to be resumed with the next target value, and loops until the
target coroutine terminates.

Examples:

```
coro G (b1) {                           ;; b1=1
    coro L (c1) {                       ;; c1=4
        val c2 = yield(c1+1)            ;; y(5), c2=6
        val c3 = yield(c2+1)            ;; y(7), c3=8
        c3                              ;; 8
    }
    val l = coroutine(L)
    val b2 = yield(b1+1)                ;; y(2), b2=3
    val b3 = resume-yield-all l(b2+1)   ;; b3=9
    val b4 = yield(b3+1)                ;; y(10)
    b4
}

val g = coroutine(G)
val a1 = resume g(1)                    ;; g(1), a1=2
val a2 = resume g(a1+1)                 ;; g(3), a2=5
val a3 = resume g(a2+1)                 ;; g(6), a3=7
val a4 = resume g(a3+1)                 ;; g(8), a4=8
val a5 = resume g(a4+1)                 ;; g(9), a5=10
println(a1, a2, a3, a4, a5)             ;; --> 2, 5, 7, 8, 10
```

## Task Operations

The API for tasks has the following operations:

- [`spawn`](#spawn): creates and resumes a new task from a prototype
- [`tasks`](#task-pools): creates a task pool
- [`status`](#task-status): consults the task status
- [`pub`](#public-field): exposes the task public field
- [`await`](#await): yields the resumed task until it matches an event
- [`broadcast`](#broadcast): broadcasts an event and awake all tasks
- [`toggle`](#toggle): either ignore or accept awakes

<!--
5. [`abort`](#TODO): `TODO`
-->

Examples:

```
task T (x) {
    set pub = x                 ;; sets 1 or 2
    val n = await(:number)      ;; awaits a number broadcast
    println(pub + n)            ;; --> 11 or 12
}
val t1 = spawn T(1)
val t2 = spawn T(2)
println(t1.pub, t2.pub)         ;; --> 1, 2
broadcast(10)                   ;; awakes all tasks passing 10
```

```
task T () {
    val n = await(:number)
    println(n)
}
val ts = tasks()                ;; task pool
do {
    spawn T() in ts             ;; attached to outer pool,
    spawn T() in ts             ;; not to enclosing block
}
broadcast(10)                   ;; --> 10, 10
```

### Spawn

A spawn creates and starts an [active task](#active-values) from a
[task prototype](#prototypes):

```
Spawn : `spawn´ Expr `(´ [List(Expr)] `)´ [`in´ Expr]
```
    
The task receives an optional list of arguments.

A spawn receives expects a task protoype, an optional list of arguments, and an
optional pool to hold the task.
The operation returns a reference to the active task.

Examples:

```
task T (v, vs) {                ;; task prototype accepts 2 args
    <...>
}
val t = spawn T(10, [1,2,3])    ;; starts task passing args
println(t)                      ;; --> exe-task 0x...
```

### Task Pools

The `tasks` operation creates a [task pool](#active-values) to hold
[active tasks](#active-values):

```
Pool : `tasks´ `(´ Expr `)´
```

The operation receives an optional expression with the maximum number of task
instances to hold.
If the pool is full, a further spawn fails and returns `nil`.

Examples:

```
task T () {
    <...>
}
val ts = tasks(1)               ;; task pool
val t1 = spawn T() in ts        ;; success
val t2 = spawn T() in ts        ;; failure
println(ts, t1, t2)             ;; --> tasks: 0x... / exe-task 0x... / nil
```

### Task Status

The operation `status` returns the current state of the given active task:

```
Status : `status´ `(´ Expr `)´
```

As described in [Active Values](#active-values), a task has 4 possible status:

- `yielded`: idle and ready to be resumed
- `toggled`: ignoring resumes
- `resumed`: currently executing
- `terminated`: terminated and unable to be resumed

Examples:

```
task T = task () {
    await(,true)
}
val t = spawn T()
println(status(t))      ;; --> :yielded
toggle t(false)
broadcast(nil)
println(status(t))      ;; --> :toggled
toggle t(true)
broadcast(nil)
println(status(t))      ;; --> :terminated
```

### Public Fields

Tasks expose a public variable `pub` that is accessible externally:

```
Pub : `pub´ | Expr `.´ `pub´
```

The variable is accessed internally as `pub`, and externally as a
[field operation](#indexes-and-fields) `x.pub`, where `x` refers to the task.

When the task terminates, the public field assumes the final task value.

Examples:

```
task T () {
    set pub = 10
    await(,true)
    println(pub)    ;; --> 20
    30              ;; final task value
}
val t = spawn T()
println(t.pub)      ;; --> 10
set t.pub = 20
broadcast(nil)
println(t.pub)      ;; --> 30
```

### Await

The operation `await` suspends the execution of a running task until an event
[broadcast](#broadcast) matches the condition pattern:

```
Await : `await´ Patt [`{´ Block `}´]

Patt  : [`(´] (Cons | Oper | Full | Clock) [`)´]
            Clock : [TAG `:h´] [TAG `:min´] [TAG `:s´] [TAG `:ms´]
```

Whenever an event is broadcast, it is compared against an `await` pattern.
If it matches, the task is resumed and executes statement after the `await`.

An `await` accepts an optional block that can access the event value when the
condition pattern matches.
If the block is omitted, the pattern requires parenthesis.

In addition to standard [`ifs` patterns](#pattern-matching), an `await` also
accepts a clock timer that matches the pattern when the given time elapses.
A clock pattern is the sum of the given time units and tag prefixes.
The tag prefix must be numeric (e.g., `:10`) or represent a variable identifier
(e.g., `:x`).

Examples:

```
await(,false)                   ;; never awakes
await(:key, it.code==:escape)   ;; awakes on :key with press=:release
await(:1:h:10:min:30:s)         ;; awakes after the specified time
await e, {                      ;; awakes on any event
    println(e)                  ;; and shows it
}
```

### Broadcasts

The operation `broadcast` signals an event to awake [awaiting](#await) tasks:

```
Bcast : `broadcast´ `(´ Expr `)´ [`in´ Expr]
```

A `broadcast` expects an event expression and an optional target.
The event is matched against the patterns in `await` operations, which
determines the tasks to awake.

The special event `:Clock [ms]` advances timer patterns in await conditions,
in which `ms` corresponds to the number of milliseconds to advance.

The target expression, with the options as follows, restricts the scope of the
broadcast:

- `:task`: restricts the broadcast to nested tasks in the current task, which
    is also the default behavior if the target is omitted;
- `:global`: does not restrict the broadcast, which considers the program as a
    whole;
- otherwise, target must be a task, which restricts the broadcast to it and its
    nested tasks.

Examples:

```
<...>
task T () {
    <...>
    val x = spawn X()
    <...>
    broadcast(e) in :task       ;; restricted to enclosing task `T`
    broadcast(e)                ;; restricted to enclosing task `T`
    broadcast(e) in x           ;; restricted to spawned `x`
    broadcast(e) in :global     ;; no restrictions
}
```

### Toggle

The operation `toggle` configures an active task to ignore or consider further
`broadcast` operations:

```
Toggle : `toggle´ Expr `(´ Expr `)´
```

A `toggle` expects an active task and a [boolean](#basic-types) value
between parenthesis, which is handled as follows:

- `false`: the task ignores further broadcasts;
- `true`: the task checks further broadcasts.

### Syntactic Block Extensions

Ceu provides some syntactic block extensions to work with tasks more
effectively.
The extensions expand to standard task operations.

#### Spawn Blocks

A `spawn` block starts an anonymous nested task:

```
Spawn : `spawn´ Block
```

The task cannot be assigned or referred explicitly.
Also, any access to `pub` refers to the enclosing task.

The `spawn` extension expands as follows:

```
spawn (task () {
    <Block>
}) ()
```

Except regarding `pub` access, the extension is equivalent to this expansion.

<!--
The `:nested` annotation is an internal mechanism to indicate that nested task
is anonymous and unassignable.
-->

Examples:

```
spawn {
    await(:X)
    println(":X occurred")
}
```

#### Every Blocks

An `every` block is a loop that makes an iteration whenever an await condition
is satisfied:

```
Every : `every´ Patt Block
```

The `every` extension expands as follows:

```
loop {
    await <Patt> {
        <Block>
    }
}
```

Examples:

```
every :1:s {
    println("1 more second has elapsed")
}
```

```
every x :X, f(x) {
    println(":X satisfies f(x)")
}
```

#### Parallel Blocks

A parallel block spawns multiple anonymous tasks:

```
Par     : `par´     Block { `with´ Block }
Par-And : `par-and´ Block { `with´ Block }
Par-Or  : `par-or´  Block { `with´ Block }
```

A `par` never rejoins, even if all spawned tasks terminate.
A `par-and` rejoins only after all spawned tasks terminate.
A `par-or` rejoins as soon as any spawned task terminates, aborting the others.

The `par` extension expands as follows:

```
do {
    spawn {
        <Block-1>       ;; first task
    }
    <...>
    spawn {
        <Block-N>       ;; Nth task
    }
    await(,false)       ;; never rejoins
}
```

The `par-and` extension expands as follows:

```
do {
    val t1 = spawn {
        <Block-1>       ;; first task
    }
    <...>
    val tN = spawn {
        <Block-N>       ;; Nth task
    }
    await(, status(t1)==:terminated and ... and status(tN)==:terminated)
}
```

A `par-or { <es1> } with { <es2> }` expands as follows:

```
do {
    val t1 = spawn {
        <Block-1>       ;; first task
    }
    <...>
    val tN = spawn {
        <Block-N>       ;; Nth task
    }
    await(, (status(t1)==:terminated and t1.pub) or
            <...> or
            (status(tN)==:terminated and tN.pub))
}
```

Examples:

```
par {
    every :1:s {
        println("1 second has elapsed")
    }
} with {
    every :1:min {
        println("1 minute has elapsed")
    }
} with {
    every :1:h {
        println("1 hour has elapsed")
    }
}
println("never reached")
```

```
par-or {
    await(:1:s)
} with {
    await(:X)
    println(":X occurred before 1 second")
}
```

```
par-and {
    await(:X)
} with {
    await(:Y)
}
println(":X and :Y have occurred")
```

#### Watching Blocks

An `watching` block executes a given block until an await condition is
satisfied:

```
Watching : `watching´ Patt Block
```

A `watching` extension expands as follows:

```
par-or {
    await(<Patt>)
} with {
    <Block>
}
```

Examples:

```
watching :1:s {
    every :X {
        println("one more :X occurred before 1 second")
    }
}
```

#### Toggle Blocks

A `toggle` block executes a given block and [toggles](#toggle) it when a
broadcast event matches the given tag:

```
Toggle : `toggle´ TAG Block
```

The control event must be a tagged tuple with the given tag, holding a single
boolean value to toggle the block, e.g.:

- `:X [true]`  activates the block.
- `:X [false]` deactivates the block.

The `toggle` extension expands as follows:

```
do {
    val t = spawn {
        <Block>
    }
    if status(t) /= :terminated {
        watching (,it==t) {
            loop {
                await(<TAG>, not it[0])
                toggle t(false)
                await(<TAG>, it[0])
                toggle t(true)
            }
        }
    }
    t.pub
}
```

The given block executes normally, until a `false` is received, toggling it
off.
Then, when a `true` is received, it toggles the block on.
The whole composition terminates when the task representing the given block
terminates.

<!-- ---------------------------------------------------------------------- -->

# STANDARD LIBRARY

## Primary Library

The primary library provides primitive functions and operations:

- [`#`](#length-operator):
    consults the length of a tuple or vector
- [`==`,`/=`](#equality-operators):
    compare if values are equal
- [`>`,`>=`,`<=`,`<`](#relational-operators):
    perform relational operations
- [`+`,`-`,`*`,`/`](#arithmetic-operators):
    perform arithmetic operations
- [`and`,`not`,`or`](#logical-operatos):
    perform logical operations
- [`coroutine`, `status`, `tasks`](#coroutine-create):
    [coroutine](#coroutine-status) and [task](#task-status) operations
- [`error`](#exceptions):
    raises an exception
- [`math-cos`,`math-floor`,`math-sin`](#mathematical-operations):
    perform mathematical operations
- [`next-dict`,`next-tasks`](#next-operations):
    traverse dictionaries and task pools
- [`print`,`println`](#print):
    output values to the screen
- [`random-seed`,`random-next`](#random-numbers):
    generate random numbers
- [`sup?`](#types-and-tags):
    checks if a tag is a supertype of another
- [`tag`](#types-and-tags):
    gets and sets a value tag
- [`to-bool`,`to-number`,`to-string`,`to-tag`](#type-conversions):
    <!--`to-char`-->
    perform type conversion operations
- [`type`](#types-and-tags):
    consults the type of a value

`TODO: tuple, tag-or`

The primary library is primitive in the sense that it cannot be written in Ceu
itself.

### Length Operator

```
func {{#}} (v)      ;; --> :number
```

The operator `#` returns the length of the received tuple or vector.

Examples:

```
val tup = []
val vec = #[1,2,3]
println(#tup, #vec)     ;; --> 0 / 3
```

### Equality Operators

```
func {{==}} (v1, v2)    ;; --> :bool
func {{/=}} (v1, v2)    ;; --> :bool
```

The operators `==` and `/=` compare two values `v1` and `v2` to check if they
are equal or not equal.
The operator `==` returns `true` if the values are equal and `false` otherwise.
The operator `/=` is the negation of `==`.

To be considered equal, first the values must be of the same type.
In addition, [static values](#static-values) are compared *by value*, while
[dynamic values](#dynamic-values) and [active values](#active-values) are
compared *by reference*.

Examples:

```
1 == 1          ;; --> true
1 /= 1          ;; --> false
1 == '1'        ;; --> false
[1] == [1]      ;; --> false
```

```
val t1 = [1]
val t2 = t1
t1 == t2        ;; --> true
```

### Relational Operators

```
func {{>}}  (v1 ,v2)    ;; --> :bool
func {{>=}} (v1 ,v2)    ;; --> :bool
func {{<=}} (v1 ,v2)    ;; --> :bool
func {{<}}  (v1, v2)    ;; --> :bool
```

The operators `>`, `>=`, `<=` and `<` perform the standard relational
operations of *greater than*, *greater or equal than*, *less than*, and
*less or equal then*, respectively.

Examples:

```
1 > 2       ;; --> false
2 >= 1      ;; --> true
1 <= 1      ;; --> true
1 < 2       ;; --> true
```

### Arithmetic Operators

```
func {{+}} (v1, v2)     ;; --> :number
func {{-}} (v1 [,v2])   ;; --> :number
func {{*}} (v1 ,v2)     ;; --> :number
func {{/}} (v1 ,v2)     ;; --> :number
func {{%}} (v1 ,v2)     ;; --> :number
```

The operators `+`, `-`, `*` and `/` perform the standard arithmetics operations
of *addition*, *subtraction*, *multiplication*, and *division*, respectively.

The operator `%` performs the *remainder* operation.

The operator `-` is also used as the unary minus when it prefixes an
expression.

`TODO: *-*, //`

Examples:

```
1 + 2       ;; --> 3
1 - 2       ;; --> -1
2 * 3       ;; --> 6
5 / 2       ;; --> 2.5
5 % 2       ;; --> 1
-20         ;; --> -20
```

### Logical Operators

```
func not (v)
func and (v1, v2)
func or  (v1, v2)
```

The logical operators `not`, `and`, and `or` are functions with a special
syntax to be used as prefix (`not`) and infix operators (`and`,`or`).

A `not` receives a value `v` and is equivalent to the code as follows:

```
if v { false } else { true }
```

The operators `and` and `or` returns one of their operands `v1` or `v2`.

An `and` is equivalent to the code as follows:

```
do {
    val x = v1
    if x { v2 } else { x }
}
```

An `or` is equivalent to the code as follows:

```
do {
    val x = v1
    if x { x } else { v2 }
}
```

Examples:

```
not not nil     ;; --> false
nil or 10       ;; --> 10
10 and nil      ;; --> nil
```

### Types and Tags

```
func type (v)           ;; --> :type
func sup? (tag1, tag2)  ;; --> :bool
func string-to-tag (s)  ;; --> :tag
func tag (t, v)         ;; --> v
func tag (v)            ;; --> :tag
```

The function `type` receives a value `v` and returns its [type](#types) as one
of the tags that follows:
    `:nil`, `:tag`, `:bool`, `:char`, `:number`, `:pointer`,
    `:tuple`, `:vector`, `:dict`,
    `:func`, `:coro`, `:task`,
    `:exe-coro`, `:exe-task`, `tasks`.

The function `sup?` receives tags `tag1` and `tag2`, and returns if `tag1` is
a [super-tag](#hierarchical-tags) of `tag2`.

The function `tag` sets or queries tags associated with values of
[user types](#user-types).
To set or unset a tag, the function receives a value `v`, a tag `t`, and a
boolean `set` to set or unset the tag.
The function returns the same value passed to it.
To query a tag, the function receives a value `v`, a tag `t` to check, and
returns a boolean to answer if the tag (or any sub-tag) is associated with the
value.

Examples:

```
type(10)                ;; --> :number
val x = tag(:X, [])     ;; value x=[] is associated with tag :X
tag(x)                  ;; --> :X
```

### Type Conversions

```
func to-bool    (v)  ;; --> :bool
func to-tag     (v)  ;; --> :tag
func to-number  (v)  ;; --> :number
func to-pointer (v)  ;; --> :pointer
func to-string  (v)  ;; --> "string"
```

<!--
func to-char   (v)  ;; -> :char
to-char(65)         ;; -> 'A'
-->

The conversion functions receive any value `v` and try to convert it to a value
of the specified type.
If the conversion is not possible, the function returns `nil`.

`TODO: explain all possibilities`

Examples:

```
to-bool(nil)        ;; --> false
to-number("10")     ;; --> 10
to-number([10])     ;; --> nil
to-tag(":number")   ;; --> :number
to-string(10)       ;; --> "10"
```

### Next Operations

```
func next-dict  (dic,  cur)  ;; --> nxt
func next-tasks (tsks, cur)  ;; --> nxt
```

The `next` operations allow to traverse collections step by step.

The function `next-dict` receives a dictionary `dic`, a key `cur`, and returns
the key `nxt` that follows `cur`.
If `cur` is `nil`, the function returns the initial key.
The function returns `nil` if there are no reamining keys to enumerate.

The function `next-tasks` receives a task pool `tsks`, a task `cur`, and
returns task `nxt` that follows `cur`.
If `cur` is `nil`, the function returns the initial task.
The function returns `nil` if there are no reamining tasks to enumerate.

Examples:

```
val d = [(:k1,10), (:k2,20)]
val k1 = next-dict(d)
val k2 = next-dict(d, k1)
println(k1, k2)     ;; --> :k1 / :k2
```

```
val ts = tasks()
spawn T() in ts     ;; tsk1
spawn T() in ts     ;; tsk2
val t1 = next-tasks(ts)
val t2 = next-tasks(ts, t1)
println(t1, t2)     ;; --> tsk1 / tsk2
```

### Print

```
func print (...)
func println (...)
```

The functions `print` and `println` outputs the given values and return `nil`.

Examples:

```
println(1, :x, [1,2,3])     ;; --> 1   :x   [1,2,3]
```

### Mathematical Operations

```
func math-cos   (v)     ;; --> :number
func math-floor (v)     ;; --> :number
func math-sin   (v)     ;; --> :number
```

The functions `math-sin` and `math-cos` compute the sine and cossine of the
given number in radians, respectively.

The function `math-floor` return the integral floor of a given real number.

Examples:

```
math-sin(3.14)          ;; --> 0
math-cos(3.14)          ;; --> -1
math-floor(10.14)       ;; --> 10
```

### Random Numbers

```
func random-seed (n)
func random-next ()     ;; --> :number
```

`TODO`

## Auxiliary Library

- [`===`,`=/=`](#deep-equality-operators):
    compare if values are deeply equal
- [`in?`,`in-not?`](#in-operators):
    check if value is in collection
- [`is?`,`is-not?`](#is-operators):
    check if values are compatible
- [`:Iterator`,`to-iter`,`to-set`,`to-vector`](#iterator-operations):
    perform iterator operations

`TODO: ++, <++, <|<`
`TODO: min, max, between`
`TODO: assert, copy, string?`

### Deep Equality Operators

```
func {===} (v1, v2)  ;; --> :bool
func {=/=} (v1, v2)  ;; --> :bool
```

The operators `===` and `=/=` deeply compare two values `v1` and `v2` to check
if they are equal or not equal.
The operator `===` returns `true` if the values are deeply equal and `false`
otherwise.
The operator `=/=` is the negation of `===`.

Except for [collections](#collections), deep equality behaves the same as
[equality](#equality-operators).
To be considered deeply equal, collections must be of the same type, have the
same [user tags](#user-types), and all indexes and values must be deeply equal.

Examples:

```
1 === 1                 ;; --> true
1 =/= 1                 ;; --> false
1 === '1'               ;; --> false
#[1] === #[1]           ;; --> true
@[(:x,1),(:y,2)] =/=
@[(:y,2),(:x,1)]        ;; --> false
```

### In Operators

```
func in? (v, vs)
func in-not? (v, vs)
```

The operators `in?` and `in-not?` are functions with a special syntax to be
used as infix operators.

The operator `in?` checks if `v` is part of [collection](#collections) `vs`.
For tuples and vectors, the values are checked.
For dictionaries, the indexes are checked.

The operator `in-not?` is the negation of `in?`.

Examples:

```
10 in? [1,10]            ;; true
20 in? #[1,10]           ;; false
10 in? @[(1,10)]         ;; false
```

### Is Operators

```
func is? (v1, v2)
func is-not? (v1, v2)
```

The operators `is?` and `is-not?` are functions with a special syntax to be
used as infix operators.

The operator `is?` checks if `v1` matches `v2` as follows:

```
ifs {
    (v1 === v2)        => true
    (type(v1) == v2)   => true
    (type(v2) == :tag) => sup?(v2, tag(v1))
    else => false
}
```

The operator `is-not?` is the negation of `is?`.

Examples:

```
10 is? :number          ;; --> true
10 is? nil              ;; --> false
tag(:X,[]) is? :X       ;; --> true
```

### Iterator Operations

[Iterator loops](#iterators) in Ceu rely on the `:Iterator` template and
`to-iter` constructor function as follows:

```
data :Iterator = [f]
func to-iter (v, tp)       ;; --> :Iterator [f]
```

The function `to-iter` receives an iterable `v`, an optional modifier `tp`, and
returns an iterator.
The returned iterator is a tuple template in which the first field is a
function that, when called, returns the next element of the original iterable
`v`.
The iterator function must return `nil` to signal that there are no more
elements to traverse in the iterable.

The function `to-iter` accepts the following iterables and modifiers:

- Tuples:
    - On each call, the iterator returns the next tuple element.
    - Modifiers:
        - `:all`: returns each index and value as a pair `[i,v]`
        - `:idx`: returns each numeric index
        - `:val`: returns the value on each index **(default)**
- Vectors:
    - On each call, the iterator returns the next vector element.
    - Modifiers:
        - `:all`: returns each index and value as a pair `[i,v]`
        - `:idx`: returns each numeric index
        - `:val`: returns the value on each index **(default)**
- Dictionaries:
    - On each call, the iterator returns the next dictionary element.
    - Modifiers:
        - `:all`: returns each key and value as a pair `[k,v]`
        - `:key`: returns each key **(default)**
        - `:val`: returns the value on each key
- Functions:
    - On each call, the iterator simply calls the original function.
- Active Coroutine
    - On each call, the iterator resumes the coroutine and returns its yielded
      value. If the coroutine is terminated, it returns `nil`.

```
func to-set    (col)    ;; --> :dict
func to-vector (col)    ;; --> :vector
```

`TODO`

<!--
- :Iterator
data :Iterator = [f,s,tp,i]
-->

# SYNTAX

```
Prog  : { Expr [`;´] }
Block : `{´ { Expr [`;´] } `}´
Expr  : `do´ Block                                      ;; explicit block
      | `do´ Expr                                       ;; innocuous expression
      | `defer´ Block                                   ;; defer statements
      | `(´ Expr `)´                                    ;; parenthesis

      | `val´ ID [TAG] [`=´ [TAG] Expr]                 ;; decl constant
      | `var´ ID [TAG] [`=´ [TAG] Expr]                 ;; decl variable

      | `func´ ID `(´ [List(ID)] [`...´] `)´ Block      ;; decl func
      | `coro´ ID `(´ [List(ID)] [`...´] `)´ Block      ;; decl coro
      | `task´ ID `(´ [List(ID)] [`...´] `)´ Block      ;; decl task

      | `set´ Expr `=´ Expr                             ;; assignment

      | `enum´ `{´ List(TAG [`=´ Expr]) `}´             ;; tags enum
      | `data´ Data                                     ;; tags templates
            Data : TAG `=´ `[´ List(ID [TAG]) `]´
                    [`{´ { Data } `}´]

      | `nil´ | `false´ | `true´                        ;; literals &
      | NAT | TAG | CHR | NUM                           ;; identifiers
      | ID | `pub´ | `...´

      | `[´ [List(Expr)] `]´                            ;; tuple
      | `#[´ [List(Expr)] `]´                           ;; vector
      | `@[´ [List(Key-Val)] `]´                        ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´
      | STR                                             ;; string
      | TAG `[´ [List(Expr)] `]´                        ;; tagged tuple

      | OP Expr                                         ;; pre ops
      | Expr OP Expr                                    ;; bin ops
      | Expr `(´ [List(Expr)] `)´                       ;; call

      | Expr `[´ Expr `]´                               ;; index
      | Expr `.´ ID                                     ;; dict field
      | Expr `.´ `pub´                                  ;; task pub
      | Expr `.´ `(´ TAG `)´                            ;; cast

      | Expr `[´ (`=´|`+´|`-´) `]´                      ;; stack peek,push,pop
      | Expr (`<--` | `<-` | `->` | `-->` ) Expr        ;; pipes
      | Expr [`where´ Block | `thus´ [ID] Block]        ;; where/thus clauses

      | `if´ Expr (Block | `=>´ Expr)                   ;; conditional
        [`else´  (Block | `=>´ Expr)]

      | `ifs´ `{´ {Case} [Else] `}´ ;; conditionals     ;; conditionals
            Case :  Expr  (Block | `=>´ Expr)
            Else : `else´ (Block | `=>´ Expr)

      | `ifs´ Expr `{´ {Case} [Else] `}´                ;; pattern matching
            Case :  Patt  (Block | `=>´ Expr)
            Else : `else´ (Block | `=>´ Expr)

      | `loop´ [ID [TAG]] [`in´ (Range | Expr)] Block   ;; loops
            Range : (`{´ | `}´) Expr `=>` Expr (`{´ | `}´) [`:step` [`+´|`-´] Expr]
      | `skip´ `if´ Expr                                ;; loop jump back
      | `break´ [`(´ Expr `)´] `if´ Expr                ;; loop escape
      | `until´ Expr
      | `while´ Expr

      | `catch´ [Patt] Block                            ;; catch exception
      | `error´ `(´ Expr `)´                            ;; throw exception

      | `func´ `(´ [List(ID)] `)´ Block                 ;; anon function
      | `coro´ `(´ [List(ID)] `)´ Block                 ;; anon coroutine
      | `task´ `(´ [List(ID)] `)´ Block                 ;; anon task
      | `\´ `{´ [ID [TAG] `=>´]  { Expr [`;´] }`}´      ;; anon function

      | `status´ `(´ Expr `)´                           ;; coro/task status

      | `coroutine´ `(´ Expr `)´                        ;; create coro
      | `yield´ `(´ [Expr] `)´                          ;; yield from coro
      | `resume´ Expr `(´ [Expr] `)´                    ;; resume coro
      | `resume-yield-all´ Expr `(´ [Expr] `)´          ;; resume-yield nested coro

      | `spawn´ Expr `(´ [List(Expr)] `)´ [`in´ Expr]   ;; spawn task
      | `tasks´ `(´ Expr `)´                            ;; task pool
      | `await´ Patt [`{´ Block `}´]                    ;; await event
      | `broadcast´ `(´ Expr `)´ [`in´ Expr]            ;; broadcast event
      | `toggle´ Expr `(´ Expr `)´                      ;; toggle task

      | `spawn´ Block                                   ;; spawn nested task
      | `every´ Patt Block                              ;; await event in loop
      | `watching´ Patt Block                           ;; abort on event
      | `par´ Block { `with´ Block }                    ;; spawn tasks
      | `par-and´ Block { `with´ Block }                ;; spawn tasks, rejoin on all
      | `par-or´ Block { `with´ Block }                 ;; spawn tasks, rejoin on any
      | `toggle´ TAG Block                              ;; toggle task on/off on tag

Patt : [`(´] (Cons | Oper | Full | Clock) [`)´]
        Cons  : Expr
        Oper  : OP [Expr]
        Full  : [ID] [TAG] [`,´ [Expr]]
        Clock : [TAG `:h´] [TAG `:min´] [TAG `:s´] [TAG `:ms´]

List(x) : x { `,´ x }                                   ;; comma-separated list

ID    : [`^´|`^^´] [A-Za-z_][A-Za-z0-9_\'\?\!\-]*       ;; identifier variable (`^´ upval)
      | `{´ OP `}´                                      ;; identifier operation
TAG   : :[A-Za-z0-9\.\-]+                               ;; identifier tag
OP    : [+-*/><=!|&~%#@]+                               ;; identifier operation
      | `not´ | `or´ | `and´ | `is?´ | `is-not?´ | `in?´ | `in-not?´
CHR   : '.' | '\\.'                                     ;; literal character
NUM   : [0-9][0-9A-Za-z\.]*                             ;; literal number
NAT   : `.*`                                            ;; native expression
STR   : ".*"                                            ;; string expression
```
