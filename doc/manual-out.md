# The Programming Language Ceu

- <a href="#design">1.</a> DESIGN
    - <a href="#structured-deterministic-concurrency">1.1.</a> Structured Deterministic Concurrency
    - <a href="#event-signaling-mechanisms">1.2.</a> Event Signaling Mechanisms
    - <a href="#lexical-memory-management">1.3.</a> Lexical Memory Management
    - <a href="#hierarchical-tags">1.4.</a> Hierarchical Tags
- <a href="#lexicon">2.</a> LEXICON
    - <a href="#keywords">2.1.</a> Keywords
    - <a href="#symbols">2.2.</a> Symbols
    - <a href="#operators">2.3.</a> Operators
    - <a href="#identifiers">2.4.</a> Identifiers
    - <a href="#literals">2.5.</a> Literals
    - <a href="#comments">2.6.</a> Comments
- <a href="#types">3.</a> TYPES
    - <a href="#basic-types">3.1.</a> Basic Types
        - `nil` `bool` `char` `number` `pointer` `tag`
    - <a href="#collections">3.2.</a> Collections
        - `tuple` `vector` `dict`
    - <a href="#execution-units">3.3.</a> Execution Units
        - `func` `coro` `task` `x-coro` `x-task` `x-tasks` `x-track`
    - <a href="#user-types">3.4.</a> User Types
- <a href="#values">4.</a> VALUES
    - <a href="#literal-values">4.1.</a> Literal Values
        - `nil` `bool` `tag` `number` `char` `pointer`
    - <a href="#dynamic-values">4.2.</a> Dynamic Values
        - `tuple` `vector` `dict` `func` `coro` `task`
    - <a href="#active-values">4.3.</a> Active Values
        - `x-coro` `x-task` `x-tasks` `x-track`
- <a href="#statements">5.</a> STATEMENTS
    - <a href="#program-sequences-and-blocks">5.1.</a> Program, Sequences and Blocks
        - `;` `do` `defer` `pass`
    - <a href="#variables-declarations-and-assignments">5.2.</a> Variables, Declarations and Assignments
        - `val` `var` `set` `...` `err` `evt`
    - <a href="#tag-enumerations-and-tuple-templates">5.3.</a> Tag Enumerations and Tuple Templates
        - `enum` `data`
    - <a href="#calls-operations-and-indexing">5.4.</a> Calls, Operations and Indexing
        - `f(...)` `x+y` `t[...]` `t.x`
    - <a href="#conditionals-and-loops">5.5.</a> Conditionals and Loops
        - `if` `ifs` `loop` `loop if` `loop until` `loop in`
    - <a href="#exceptions">5.6.</a> Exceptions
        - `throw` `catch`
    - <a href="#coroutine-operations">5.7.</a> Coroutine Operations
        - `coroutine` `yield` `resume` `toggle` `kill` `status` `spawn` `resume-yield-all`
    - <a href="#task-operations">5.8.</a> Task Operations
        - `pub` `spawn` `await` `broadcast` `track` `detrack` `tasks` `spawn in`
        - `loop in` `every` `spawn {}` `awaiting` `toggle {}` `par` `par-and` `par-or`
- <a href="#standard-library">6.</a> STANDARD LIBRARY
    - <a href="#primary-library">6.1.</a> Primary Library
    - <a href="#auxiliary-library">6.2.</a> Auxiliary Library
- <a href="#syntax">7.</a> SYNTAX
    - <a href="#basic-syntax">7.1.</a> Basic Syntax
    - <a href="#extended-syntax">7.2.</a> Extended Syntax

<!-- CONTENTS -->

<a name="design"/>

# 1. DESIGN

Ceu is a [synchronous programming language][1] that reconciles *[Structured
Concurrency][2]* with *[Event-Driven Programming][3]*.
Ceu extends classical structured programming with three main functionalities:

- Structured Deterministic Concurrency:
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

Ceu is inspired by [Esterel][4] and [Lua][5].

Follows an extended list of functionalities:

- Dynamic typing
- Statements as expressions
- Dynamic collections (tuples, vectors, and dictionaries)
- Stackless coroutines (the basis of tasks)
- Restricted closures (upvalues must be explicit and final)
- Deferred statements (for finalization)
- Exception handling (throw & catch)
- Hierarchical tuple templates (for data description with inheritance)
- Seamless integration with C (source-level compatibility)

[1]: https://en.wikipedia.org/wiki/Synchronous_programming_language
[2]: https://en.wikipedia.org/wiki/Structured_concurrency
[3]: https://en.wikipedia.org/wiki/Event-driven_programming
[4]: https://en.wikipedia.org/wiki/Esterel
[5]: https://en.wikipedia.org/wiki/Lua_(programming_language)

<a name="structured-deterministic-concurrency"/>

## 1.1. Structured Deterministic Concurrency

In structured concurrency, the life cycle of processes or tasks respect the
structure of the source code in blocks.
In this sense, tasks in Ceu are treated in the same way as local variables in
structured programming:
When a [block](#blocks) of code terminates or goes out of scope, all of its
[local variables](variables-declarations-and-assignments) and
[tasks](#active-values) become inaccessible to enclosing blocks.

In addition, tasks are automatically aborted and properly finalized (by
[deferred statements](#defer)).

Tasks in Ceu are built on top of [coroutines](#active-values), which unlike OS
threads, have a predictable run-to-completion semantics, in which they execute
uninterruptedly up to an explicit [yield](#yield) or [await](#await) operation.

The next example illustrates structured concurrency, abortion of tasks, and
deterministic scheduling.
The example uses a `par-or` to spawn two concurrent tasks:
    one that terminates after 10 seconds, and
    another that increments variable `n` every second, showing its value on
    termination:

```
spawn {
    par-or {
        await 10:s
    } with {
        var n = 0
        defer {
            println("I counted ", n)    ;; invariably outputs 9
        }
        every 1:s {
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

Being coroutines, tasks are expected to yield control explicitly, which makes
scheduling entirely deterministic.
In addition, tasks awake in the order they appear in the source code, which
makes the scheduling order predictable.
This rule allows us to infer that the example invariably outputs `9`, no matter
how many times we execute it.

<a name="event-signaling-mechanisms"/>

## 1.2. Event Signaling Mechanisms

Tasks can communicate through events as follows:

- The [`await`](#await) statement suspends a task until it matches an event
  condition.
- The [`broadcast`](#broadcast) statement signals an event to all awaiting
  tasks.

<img src="bcast.png" align="right"/>

The active tasks form a dynamic tree representing the structure of the program,
as illustrated in the figure.
The three is traversed on every broadcast in a predictable way, since it
respects the lexical structure of the program:
A task has exactly one active block at a time, which is first traversed `(1)`.
The active block has a list of active tasks, which are traversed in sequence
`(2,3)`, and exactly one nested block, which is traversed after the nested
tasks `(4)`.
After the nested blocks and tasks are traversed, the outer task itself is
traversed at its single yielded execution point `(5)`.
A broadcast traversal runs to completion before proceeding to the next
statement, just like a function call.

The next example illustrates event broadcasts and the tasks traversal.
The example uses an `awaiting` statement to observe an event condition while
executing a nested task.
When the condition is satisfied, the nested task is aborted:

```
spawn {
    awaiting evt==:done {
        par {
            every evt==:tick {
                println(":tick-1")      ;; always awakes first
            }
        } with {
            every evt==:tick {
                println(":tick-2")      ;; always awakes last
            }
        }
    }
    println(:done)
}
broadcast :tick                         ;; <-- :tick-1, tick-2
broadcast :tick                         ;; <-- :tick-1, tick-2
broadcast :done                         ;; <-- :done
println("the end")                      ;; <-- the end
```

The main block has an outermost `spawn` task, which awaits `:done`, and has two
nested tasks awaiting `:tick` events.
Then, the main block broadcasts three events in sequence.
The first two `:tick` events awake the nested tasks respecting the structure of
the program, printing `:tick-1` and `:tick-2` in this order.
The last event aborts the `awaiting` block and prints `:done`, before
terminating the main block.

<a name="lexical-memory-management"/>

## 1.3. Lexical Memory Management

Ceu respects the lexical structure of the program also when dealing with
dynamic allocation of memory.
Every [dynamic value](#dynamic-values) is attached to the [block](#block) in
which it was first assigned and cannot escape it in further assignments or as
return expressions.
This is valid not only for [collections](#constructors) (tuples, vectors, and
dictionaries), but also to [closures](#prototypes),
[coroutines](#active-values), and [tasks](#active-values).
This restriction ensures that terminating blocks (and consequently tasks)
deallocate all memory it allocates at once.
More importantly, it provides static means to reason about the program.
To overcome this restriction, Ceu also provides an explicit [move](#TODO)
operation to reattach a dynamic value to an outer scope.

<!--
The next example illustrates event broadcasts and the tasks traversal.

```
println("the end")
```

- GC
-->

<a name="hierarchical-tags"/>

## 1.4. Hierarchical Tags

`TODO`

<!--
Just like local variables, 
For instance, if ...

- coro vs task
- tasks, tracks
- bcast, await
- synchronous

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

<a name="lexicon"/>

# 2. LEXICON

<a name="keywords"/>

## 2.1. Keywords

The following keywords are reserved in Ceu:

```
    and                 ;; and operator                 (00)
    await               ;; await event
    awaiting            ;; awaiting block
    broadcast           ;; broadcast event
    catch               ;; catch exception
    coro                ;; coroutine prototype
    coroutine           ;; create coroutine
    data                ;; data declaration
    defer               ;; defer block
    detrack             ;; detrack task
    do                  ;; do block                     (10)
    else                ;; else block
    enum                ;; enum declaration
    err                 ;; exception variable
    every               ;; every block
    evt                 ;; event variable
    false               ;; false value
    func                ;; function prototype
    if                  ;; if block
    ifs                 ;; ifs block
    in                  ;; in keyword                   (20)
    is                  ;; is operator
    is-not              ;; is-not operator
    loop                ;; loop block
    nil                 ;; nil value
    not                 ;; not operator
    or                  ;; or operator
    par                 ;; par block
    par-and             ;; par-and block
    par-or              ;; par-or block
    pass                ;; innocuous expression         (30)
    poly                ;; TODO
    pub                 ;; public variable
    resume              ;; resume coroutine
    resume-yield-all    ;; resume coroutine
    set                 ;; assign expression
    spawn               ;; spawn coroutine
    status              ;; coroutine status
    task                ;; task prototype/self identifier
    tasks               ;; pool of tasks
    toggle              ;; toggle coroutine/block       (40)
    track               ;; track task
    true                ;; true value
    until               ;; until loop modifier
    val                 ;; constant declaration
    var                 ;; variable declaration
    where               ;; where block
    with                ;; with block
    yield               ;; yield coroutine              (48)
```

Keywords cannot be used as variable identifiers.

<a name="symbols"/>

## 2.2. Symbols

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
    #[              ;; vector constructor
    @[              ;; dictionary constructor
    '   "   `       ;; character/string/native delimiters
    $               ;; native interpolation
    ^               ;; lexer annotation
```

<a name="operators"/>

## 2.3. Operators

The following operator symbols can be combined to form operator names in Ceu:

```
    +    -    *    /
    >    <    =    !
    |    &    ~    %
    #    @
```

Operators names cannot clash with reserved symbols (e.g., `->`).

<a name="identifiers"/>

## 2.4. Identifiers

Ceu uses identifiers to refer to variables and operators:

```
ID : [A-Za-z_][A-Za-z0-9_'?!-]*      ;; letter/under/digit/quote/quest/excl/dash
   | `{´ OP `}´                      ;; operator enclosed by braces as identifier
OP : [+-*/><=!|&~%#@]+               ;; see Operators
```

A variable identifier starts with a letter or underscore (`_`) and is followed
by letters, digits, underscores, single quotes (`'`), question marks (`?`),
exclamation marks (`!`), or dashes (`-`).
A dash must be followed by a letter or digit.

Note that dashes are ambiguous with the minus operator.
For this reason, (i) the minus operation requires spaces between operands
(e.g., `x - 1`), and (ii) co-existing variables with common parts in
identifiers are rejected (e.g., `x` vs `x-1` vs `a-x`).

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
{-}             ;; op as var id
```

<a name="literals"/>

## 2.5. Literals

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

The literal `nil` is the single value of the [*nil*](#basic-types) type.

The literals `true` and `false` are the only values of the [*bool*](#basic-types)
type.

A [*tag*](#basic-types) type literal starts with a colon (`:`) and is followed
by letters, digits, dots (`.`), or dashes (`-`).
A dot or dash must be followed by a letter or digit.

A [*number*](#basic-types) type literal starts with a digit and is followed by
digits, letters, and dots (`.`), and is represented as a *C float*.

A [*char*](#basic-types) type literal is a single or backslashed (`\`)
character enclosed by single quotes (`'`), and is representaed as a *C char*.

A string literal is a sequence of characters enclosed by double quotes (`"`).
It is expanded to a [vector](#collections) of character literals, e.g., `"abc"`
expands to `#['a','b','c']`.

A native literal is a sequence of characters enclosed by multiple back quotes
(`` ` ``).
The same number of backquotes must be used to open and close the literal.
`TODO: $, :type, :pre, :ceu`

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
`:number sin($x)`   ;; native with type and interpolation
```

<a name="comments"/>

## 2.6. Comments

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

<a name="types"/>

# 3. TYPES

Ceu is a dynamic language in which values carry their own types during
execution.

The function `type` returns the type of a value as a [tag](#basic-types):

```
type(10)  --> :number
type('x') --> :char
```

<a name="basic-types"/>

## 3.1. Basic Types

Ceu has 6 basic types:

```
nil    bool    char    number    pointer    tag
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

The `pointer` type represents opaque native pointer values from [native
literals](#literals).

The `tag` type represents [tag identifiers](#literals).
Each tag is internally associated with a natural number that represents a
unique value in a global enumeration.
Tags are also known as *symbols* or *atoms* in other programming languages.
Tags can be explicitly [enumerated](#tag-enumerations-and-tuple-templates) to
interface with [native expressions](#literals).
Tags can form [hierarchies](#tag-enumerations-and-tuple-templates) to represent
user types and describe [tuple templates](#tag-enumerations-and-tuple-templates).

<a name="collections"/>

## 3.2. Collections

Ceu has 3 collection types:

```
tuple    vector    dict
```

The `tuple` type represents a fixed collection of heterogeneous values, in
which each numeric index, starting at `0`, holds a value of a (possibly)
different type.

The `vector` type represents a variable collection of homogeneous values, in
which each numeric index, starting at `0`,  holds a value of the same type.

The `dict` type (dictionary) represents a variable collection of heterogeneous
values, in which each index of any type maps to a value of a (possibly)
different type.

Examples:

```
[1, 'a', nil]           ;; a tuple with 3 values
#[1, 2, 3]              ;; a vector of numbers
@[(:x,10), (:y,20)]     ;; a dictionary with 2 mappings
```

<a name="execution-units"/>

## 3.3. Execution Units

Ceu provide 3 types of execution units, functions, coroutines, and tasks:

```
func    coro    task
x-coro  x-task  x-tasks  x-track
```

The `func` type represents [function prototypes](#prototypes).

The `coro` type represents [coroutine prototypes](#prototypes), while the
`x-coro` type represents [active coroutines](#active-values).

The `task` type represents [task prototypes](#prototypes), while the `x-task`
type represents [active tasks](#active-values).
The `x-tasks` type represents [task pools](#active-values) holding active
tasks.
The `x-track` type represents [track references](#active-values) pointing to
active tasks.

<a name="user-types"/>

## 3.4. User Types

Values from non-basic types (i.e., collections and execution units) can be
associated with [tags](#TODO) that represent user types.

The function [`tags`](#TODO) associates tags with values, and also checks if a
value is of the given tag:

```
val x = []              ;; an empty tuple
tags(x, :T, true)       ;; x is now of user type :T
println(tags(x,:T))     ;; --> true
```

Tags form type hierarchies based on the dots in their identifiers, i.e., `:T.A`
and `:T.B` are subtypes of `:T`.
Tag hierarchies can nest up to 4 levels.

The function [`is`](#TODO) checks if values match types or tags:

```
val x = []              ;; an empty tuple
tags(x, :T.A, true)     ;; x is now of user type :T.A
println(x is :tuple)    ;; --> true
println(x is :T)        ;; --> true
```

User types do not require to be predeclared, but can appear in [tuple
template](#TODO) declarations.

<a name="values"/>

# 4. VALUES

As a dynamic language, each value in Ceu carries extra information, such as its
own type.

<a name="literal-values"/>

## 4.1. Literal Values

A *literal value* does not require dynamic allocation since it only carries
extra information about its type.
All [basic types](#basic-types) have [literal](#literals) values:

```
Types : nil | bool | char | number | pointer | tag
Lits  : `nil´ | `false´ | `true´ | TAG | NUM | CHR | STR | NAT
```

Literals are immutable and are copied between variables and blocks as a whole
without any restrictions.

<a name="dynamic-values"/>

## 4.2. Dynamic Values

A *dynamic value* requires dynamic allocation since its internal data is too
big to fit in a literal value.
The following types have dynamic values:

```
Colls  : tuple | vector | dict                  ;; collections
Protos : func | coro | task                     ;; prototypes
Actvs  : x-coro | x-task | x-tasks | x-track    ;; active values (next section)
```

Dynamic values are mutable and are manipulated through references, allowing
that multiple aliases refer to the same value.

Dynamic values are always attached to the enclosing [block](#blocks) in which
they were first assigned, and cannot escape to outer blocks in further
assignments or as return expressions.
This is also valid for active [coroutines](#active-values) and
[tasks](#active-values).
This restriction permits that terminating blocks deallocate all dynamic values
attached to them.

Ceu also provides an explicit [move](#TODO) operation to reattach a dynamic
value to an outer scope.

Nevertheless, a dynamic value is still subject to garbage collection, given
that it may loose all references to it, even with its enclosing block active.

<a name="constructors"/>

### 4.2.1. Constructors

Ceu provides constructors for [collections](#collections) to allocate tuples,
vectors, and dictionaries:

```
Cons : `[´ [List(Expr)] `]´             ;; tuple
     | `#[´ [List(Expr)] `]´            ;; vector
     | `@[´ [List(Key-Val)] `]´         ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´
     | TAG `[´ [List(Expr)] `]´         ;; tagged tuple
```

Tuples (`[...]`) and vectors (`#[...]`) are built providing a list of
expressions.

Dictionaries (`@[...]`) are built providing a list of pairs of expressions
(`(key,val)`), in which each pair maps a key to a value.
The first expression is the key, and the second is the value.
If the key is a tag, the alternate syntax `tag=val` may be used (omitting the
tag `:`).

A tuple constructor may also be prefixed with a tag, which associates the tag
with the tuple, e.g., `:X [...]` is equivalent to `tags([...], :X, true)`.
Tag constructors are typically used in conjunction with
[tuple templates](#tag-enumerations-and-tuple-templates)

Examples:

```
[1,2,3]             ;; a tuple
:Pos [10,10]        ;; a tagged tuple
#[;; a vector
[(:x,10), x=10]     ;; a dictionary with equivalent key mappings
```

<a name="prototypes"/>

### 4.2.2. Prototypes

Ceu supports functions, coroutines, and tasks as prototype values:

```
Func : `func´ `(´ [List(ID)] [`...´] `)´ Block
Coro : `coro´ `(´ [List(ID)] [`...´] `)´ Block
Task : `task´ `(´ [List(ID)] [`...´] `)´ Block
```

Each keyword is followed by an optional list of identifiers as parameters
enclosed by parenthesis.

The last parameter can be the symbol
[`...`](#variables-declarations-and-assignments), which captures as a tuple all
remaining arguments of a call.

The associated block executes when the unit is [invoked](#TODO).
Each argument in the invocation is evaluated and copied to the parameter
identifier, which becomes a local variable in the execution block.

`TODO: closures (reason why dynamic)`

<a name="active-values"/>

## 4.3. Active Values

An *active value* corresponds to an active coroutine, task, pool of tasks,
or tracked reference:

```
x-coro  x-task  x-tasks  x-track
```

An active value is still a dynamic value, with all properties described above.

Active coroutines and tasks (`x-coro` and `x-task`) are running instances of
[prototypes](#prototypes) that can suspend themselves in the middle of
execution, before they terminate.
Tasks are also considered coroutines (but not the other way around).
A coroutine retains its execution state and can be
[resumed](#create-resume-spawn) from its current suspension point.

Coroutines have 4 possible status:

1. `yielded`: idle and ready to be resumed
2. `toggled`: ignoring resumes
3. `resumed`: currently executing
4. `terminated`: terminated and unable to be resumed

A coroutine is attached to the enclosing [block](#block) in which it was
instantiated.
This means that it is possible that a coroutine goes out of scope with the
yielded status.
In this case, the coroutine body is aborted and nested [`defer`](#defer)
expressions are properly triggered.

Unlike coroutines, a task can also awake automatically from
[event broadcasts](#broadcast) without an explicit `resume`.
It can also be spawned in a [pool](#pools-of-tasks) of anonymous tasks
(`x-tasks`), which will control the task life cycle and automatically release
it from memory on termination.
In this case, the task is also attached to the block in which the pool is
declared.
Finally, a task can be [tracked](#track-and-detrack) from outside with a safe
reference to it (`x-track`).
A track is set to `nil` when its referred task terminates or goes out of scope.
This is all automated by the Ceu runtime.

The operations on [coroutines](#coroutine-operations) and
[tasks](#tasks-operations) are discussed further.

<a name="statements"/>

# 5. STATEMENTS

Ceu is an expression-based language in which all statements are expressions and
evaluate to a value.

<a name="program-sequences-and-blocks"/>

## 5.1. Program, Sequences and Blocks

A program in Ceu is a sequence of statements (expressions), and a block is a
sequence of expressions enclosed by braces (`{` and `}´):

```
Prog  : { Expr [`;´] }
Block : `{´ { Expr [`;´] } `}´
```
Each expression in a sequence may be separated by an optional semicolon (`;´).
A sequence of expressions evaluate to its last expression.

<a name="blocks"/>

### 5.1.1. Blocks

A block delimits a lexical scope for variables and dynamic values:
A variable is only visible to expressions in the block in which it was
declared.
A dynamic value cannot escape the block in which it was created (e.g., from
assignments or returns), unless it is [moved](#TODO) out.
For this reason, when a block terminates, all memory that was allocated inside
it is automatically reclaimed.
This is also valid for active [coroutines](#active-values) and
[tasks](#active-values), which are attached to the block in which they were
first assigned, and are aborted on termination.

A block is not an expression by itself, but it can be turned into one by
prefixing it with an explicit `do`:

```
Do : `do´ [:unnest[-hide]] Block   ;; an explicit block statement
```

`TODO: unnest, hide`

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

Blocks also appear in compound statements, such as
[conditionals](#conditionals), [loops](#loops-and-iterators), and many others.

<a name="defer"/>

### 5.1.2. Defer

A `defer` block executes only when its enclosing block terminates:

```
Defer : `defer´ Block
```

Deferred statements execute in reverse order in which they appear in the
source code.

Example:

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

<a name="pass"/>

### 5.1.3. Pass

The `pass` statement permits that an innocuous expression is used in the
middle of a block:

```
Pass : `pass´ Expr
```

Example:

```
do {
    1           ;; ERR: innocuous expression
    pass 1      ;; OK:  innocuous but explicit
    ...
}
```

<a name="variables-declarations-and-assignments"/>

## 5.2. Variables, Declarations and Assignments

Regardless of being dynamically typed, all variables in Ceu must be declared
before use:

```
Val : `val´ ID [TAG] [`=´ Expr]         ;; constants
Var : `var´ ID [TAG] [`=´ Expr]         ;; variables
Spc : `...´ | `err´ | `evt´             ;; special variables
```

The difference between `val` and `var` is that a `val` is immutable, while a
`var` declaration can be modified by further `set` statements:

```
`set´ Expr `=´ Expr
```

The optional initialization expression assigns an initial value to the
variable, which is set to `nil` otherwise.

The `val` modifier forbids that a name is reassigned, but it does not prevent
that [dynamic values](#dynamic-values) are modified.

Optionally, a declaration can be associated with a [tuple
template](#tag-enumerations-and-tuple-templates) tag, which allows the variable
to be indexed by a field name, instead of a numeric position.
Note that the variable is not guaranteed to hold a value matching the template,
not even a tuple is guaranteed.
The template association is static but with no runtime guarantees.

The symbol `...` represents the variable arguments (*varargs*) a function
receives in a call.
In the context of a [function](#prototypes) that expects varargs, it evaluates
to a tuple holding the varargs.
In other scenarios, accessing `...` raises an error.
When `...` is the last argument of a call, its tuple is expanded as the last
arguments.

The variables `err` and `evt` have special scopes and are automatically setup
in the context of [`throw`](#exceptions) and [`broadcast`](#broadcast)
statements, respectively.

Examples:

```
var x
set x = 20              ;; OK

val y = [10]
set y = 0               ;; ERR: cannot reassign `y`
set y[0] = 20           ;; OK

val pos :Pos = [10,20]  ;; assumes :Pos has fields [x,y]
println(pos.x)          ;; <-- 10
```

<a name="tag-enumerations-and-tuple-templates"/>

## 5.3. Tag Enumerations and Tuple Templates

Tags are global identifiers that need not to be predeclared.
However, they may be explicitly delcared when used as enumerations or tuple
templates.

<a name="tag-enumerations"/>

### 5.3.1. Tag Enumerations

An `enum` groups related tags in sequence so that they are associated with
numbers in the same order:

```
Enum : `enum´ `{´ List(TAG [`=´ Expr]) `}´
```

Optionally, a tag may receive an explicit numeric value, which is implicitly
incremented for tags in sequence.

Enumerations can be used to interface with external libraries that use
constants to represent a group of related values (e.g., key symbols).

Examples:

```
enum {
    :Key-Left = `:number KEY_LEFT`  ;; explicitly associates with C enumeration
    :Key-Right                      ;; implicitly associates with remaining
    :Key-Up                         ;; keys in sequence
    :Key-Down
}
if lib-key-pressed() == :Key-Up {
    ;; lib-key-pressed is an external library
    ;; do something if key UP is pressed
}
```

<a name="tuple-templates"/>

### 5.3.2. Tuple Templates

A `data` declaration associates a tag with a tuple template, which associates
tuple positions with field identifiers:

```
Temp : `data´ Data
            Data : TAG `=´ `[´ List(ID [TAG]) `]´
                    [`{´ { Data } `}´]
```

Then, a [variable declaration](#variables-declarations-and-assignments) can
specify a tuple template and hold a tuple that can be accessed by field.

After the keyword `data`, a declaration expects a tag followed by `=` and a
template.
A template is surrounded by brackets (`[´ and `]´) to represent the tuple, and
includes a list of identifiers, each mapping an index into a field.
Each field can be followed by a tag to represent nested templates.

Examples:

```
data :Pos = [x,y]                       ;; a flat template
val pos :Pos = [10,20]                  ;; pos uses :Pos as template
println(pos.x, pos.y)                   ;; <-- 10, 20

data :Dim = [w,h]
data :Rect = [pos :Pos, dim :Dim]       ;; a nested template
val r1 :Rect = [pos, [100,100]]         ;; r uses :Rect as template
println(r1.dim, r1.pos.x)               ;; <-- [100,100], 10

val r2 :Rect = :Rect [[0,0],[10,10]]    ;; combining tag template/constructor
println(r2 is :Rect, r2.dim.h)          ;; <-- true, 0
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
    :Key = [key] {              ;; :Event.Key [ts,key] is a sub-type of :Event [ts]
        :Dn = []                ;; :Event.Key.Dn [ts,key] and :Event.Key.Up [ts,key]
        :Up = []                ;;  are sub-types of :Event [ts]
    }
    :Mouse = [pos :Pos] {       ;; :Event.Mouse [ts, pos :Pos]
        :Motion = []            ;; :Event.Mouse.Motion [ts, pos :Pos]
        :Button = [but] {       ;; :Event.Mouse.Button [ts, pos :Pos, but]
            :Dn = []            ;; :Event.Mouse.Button.Dn [ts, pos :Pos, but]
            :Up = []            ;; :Event.Mouse.Button.Up [ts, pos :Pos, but]
        }
    }
}

val but :Event.Mouse.Button.Dn = [0, [10,20], 1]
val evt :Event = but
println(evt.ts, but.pos.y)      ;; <-- 0, 20
```

<a name="calls-operations-and-indexing"/>

## 5.4. Calls, Operations and Indexing

<a name="calls-and-operations"/>

### 5.4.1. Calls and Operations

In Ceu, calls and operations are equivalent, i.e., an operation is a call that
uses an [operator](#operatos) with prefix or infix notation:

```
Call : OP Expr                      ;; unary operation
     | Expr OP Expr                 ;; binary operation
     | Expr `(´ [List(Expr)] `)´    ;; function call
```

Operations are interpreted as function calls, i.e., `x + y` is equivalent to
`{+} (x, y)`.

A call expects an expression of type [`func`](#prototypes) and an optional list
of expressions as arguments enclosed by parenthesis.
Each argument is expected to match a parameter of the function declaration.
A call transfers control to the function, which runs to completion and returns
control with a value, which substitutes the call.

As discussed in [Identifiers](#identifiers), the binary minus requires spaces
around it to prevent ambiguity with identifiers containing dashes.

Examples:

```
#vec            ;; unary operation
x - 10          ;; binary operation
{-}(x,10)       ;; operation as call
f(10,20)        ;; normal call
```

<a name="indexes-and-fields"/>

### 5.4.2. Indexes and Fields

[Collections](#collections) in Ceu (tuples, vectors, and dictionaries) are
accessed through indexes or fields:

```
Index : Expr `[´ Expr `]´
Field : Expr `.´ (NUM | ID | `pub´)
```

An index operation expects an expression as a collection, and an index enclosed
by brackets (`[` and `]`).
For tuples and vectors, the index must be an number.
For dictionaries, the index can be of any type.

The operation evaluates to the current value the collection holds on the index,
or `nil` if non existent.

A field operation expects an expression as a collection, a dot separator (`.`),
and a field identifier.
A field operation expands to an index operation as follows:
For a tuple or vector `v`, and a numeric identifier `i`, the operation expands
to `v[i]`.
For a dictionary `v`, and a [tag literal](#literals) `k` (with the colon `:`
omitted), the operation expands to `v[:k]`.

`TODO: tuple template`

A [task](#active-values) `t` also relies on a field operation to access its
public field `pub` (i.e., `t.pub`).

Examples:

```
tup[3]      ;; tuple access by index
tup.3       ;; tuple access by numeric field

vec[i]      ;; vector access by index

dict[:x]    ;; dict access by index
dict.x      ;; dict access by field

t.pub        ;; task public field
```

<a name="precedence-and-associativity"/>

### 5.4.3. Precedence and Associativity

Operations in Ceu can be combined in complex expressions with the following
precedence priority (from higher to lower):

```
1. sufix  operations       ;; t[0], x.i, f(x)
2. prefix operations       ;; -x, #t
3. binary operations       ;; x + y
```

Currently, binary operators in Ceu have no precedence or associativity rules,
requiring parenthesis for disambiguation:

```
Parens : `(´ Expr `)´
```

Examples:

```
#f(10).x        ;; # ((f(10)) .x)
x + 10 - 1      ;; ERR: requires parenthesis
- x + y         ;; (-x) + y
```

<a name="conditionals-and-loops"/>

## 5.5. Conditionals and Loops

<a name="conditionals"/>

### 5.5.1. Conditionals

Ceu supports conditionals as follows:

```
If  : `if´ Expr Block [`else´ Block]
Ifs : `ifs´ `{´ {Case} [Else] `}´
        Case : Expr `->´ (Expr | Block)
        Else : `else´ `->´ (Expr | Block)
    | `ifs´ Expr `{´ {Case} [Else] `}´
        Case : [`==´ | `is´] Expr `->´ (Expr | Block)
        Else : `else´ `->´ (Expr | Block)
```

An `if` tests a boolean expression and, if true, executes the associated block.
Otherwise, it executes the optional `else` block.

Ceu also supports `ifs` to test multiple conditions.
The first variation is a simple expansion to nested ifs, i.e.:

```
ifs {
    <cnd1> -> <exp1>
    <cnd2> -> <exp2>
    else   -> <exp3>
}
```

expands to

```
if <cnd1> {
    <exp1>
} else {
    if <cnd2> {
        <exp2>
    } else {
        <exp3>
    }
}
```

The second variation of `ifs` also receives a matching expression to switch
over and apply multiple tests.
Each test can start with `==` or `is`, which implies that the matching
expression is hidden on the left, i.e.:

```
ifs f(x) {
    is :T -> <exp1>
    == 10 -> <exp2>
    g()   -> <exp3>
    else  -> <exp4>
}
```

expands to

```
do {
    val x' = f(x)   ;; f(x) is only evaluated once
    if x' is :T {
        <exp1>
    } else {
        if x' == 10 {
            <exp2>
        } else {
            if f() {
                <exp3>
            } else {
                <exp4>
            }
        }
    }
}                   ;; evaluates to whatever the if evaluates
```

Examples:

```
val x-or-y =        ;; max between x and y
    if x > y {
        x
    } else {
        y
    }

ifs :T.Y [] {
    is :T.X -> println(:T.X)
    is :T.Y -> println(:T.Y)    ;; <-- :T.Y
    is :T.Z -> println(:T.Z)
}
```

<a name="loops-and-iterators"/>

### 5.5.2. Loops and Iterators

Ceu supports loops and iterators as follows:

```
Loop : `loop´ `if´ Expr Block       ;; loop if (while loop)
     | `loop´ Block                 ;; infinite loop
     | `loop´ Block `until´ Expr    ;; loop until
     | `loop´ `in´ <...>            ;; iterators
```

A `loop if` tests a boolean expression and, if true, executes an iteration of
the associated block, before testing the condition again.
When the condition is false, the loop terminates.

There is no `break` statement in Ceu, which can be substituted by a proper
test condition or [`throw-catch`](#exceptions) pair.

All other loops and iterators may be expressed in terms of `loop if`.
For instance, an infinite loop uses `true` as its condition.

A `loop-until` executes at least one loop iteration, and terminates when the
given condition is true.
The condition expression may use variables defined inside the loop.
A `loop { <es> } until <e>` expands to

```
do {
    var cnd = false
    loop if not cnd {
        <es>
        set cnd = <e>   ;; <e> may use variables defined in <es>
    }
    cnd                 ;; evaluates to the final condition
}
```

Examples:

```
var i = 0
loop if i<5 {       ;; --> 0,1,2,3,4
    println(i)
    set i = i + 1
}

loop {
} until 
```

<a name="iterators"/>

#### 5.5.2.1. Iterators

Ceu supports generic iterators, tasks iterators, and numeric iterators as
follows:

```
Iter : `loop´ `in´ Expr `,´ ID Block            ;; generic iterator
     | `loop´ `in´                              ;; numeric iterator
            (`[´ | `(´)
            Expr `->´ Expr
            (`]´ | `)´)
            [`,´ :step Expr]
            `,´ ID Block
```

`TODO`

```
do {
    val it :Iterator = <it>
    assert(it is :Iterator, "expected :Iterator")
    loop {
        val i = it.f(it)
        if i /= nil {
            <b>
        }
    } until (i == nil)
}
```

Examples:

```
TODO
```

<a name="exceptions"/>

## 5.6. Exceptions

A `throw` raises an exception that terminates all enclosing blocks up to a
matching `catch` block:

```
Throw : `throw´ `(´ Expr `)´
Catch : `catch´ Expr Block
```

A `throw` receives an expression that is assigned to the special variable
`err`, which is visible to enclosing `catch` statements.
A `throw` is propagated upwards and aborts all enclosing [blocks](#blocks) and
[execution units](#prototypes) (functions, coroutines, and tasks) on the way.
When crossing an execution unit, a `throw` jumps back to the calling site and
continues to propagate upwards.

A `catch` executes its associated block normally, but also registers a catch
expression to be compared against `err` when a `throw` is crossing it.
If they match, the exception is caught and the `catch` terminates, aborting its
associated block, and properly triggering nested [`defer`](#defer) statements.

To match an exception, the `catch` expression can access `err` and needs to
evaluate to `true`.
If the matching expression `x` is of type [tag](#basic-types), it expands to
match `err is x`, allowing to check [tuple
templates](#tag-enumerations-and-tuple-templates).

Examples:

```
catch err == 1 {        ;; catches
    defer {
        println(1)
    }
    catch err == 2 {    ;; no catches
        defer {
            println(2)
        }
        throw(1)        ;; throws
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
        throw(:Err.Two ["err msg"])   ;; throws another error
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



<a name="coroutine-operations"/>

## 5.7. Coroutine Operations

The basic API for coroutines has 6 operations:

1. [`coroutine`](#create-resume-spawn): creates a new coroutine from a prototype
2. [`yield`](#yield): suspends the resumed coroutine
3. [`resume`](#create-resume-spawn): starts or resumes a coroutine from its current suspension point
4. [`toggle`](#toggle): either ignore or acknowledge resumes
5. [`kill`](#TODO): `TODO`
6. [`status`](#status): returns the coroutine status

Note that `yield` is the only operation that is called from the coroutine
itself, all others are called from the user code controlling the coroutine.
Just like call arguments and return values from functions, the `yield` and
`resume` operations can transfer values between themselves.

Examples:

```
coro F (a) {                ;; first resume
    println(a)              ;; --> 10
    val c = yield(a + 1)    ;; returns 11, second resume, receives 12
    println(c)              ;; --> 12
    c + 1                   ;; returns 13
}
val f = coroutine(F)        ;; creates `f` from prototype `F`
val b = resume f(10)        ;; starts  `f`, receives `11`
val d = resume f(b+1)       ;; resumes `f`, receives `13`
println(status(f))          ;; --> :terminated
```

```
coro F () {
    defer {
        println("aborted")
    }
    yield()
}
do {
    val f = coroutine(F)
    resume f()
}                           ;; --> aborted
```

<a name="create-resume-spawn"/>

### 5.7.1. Create, Resume, Spawn

The operation `coroutine` creates a new coroutine from a
[prototype](#prototypes).
The operation `resume` executes a coroutine starting from its last suspension
point.
The operation `spawn` creates and resumes a coroutine:

```
Create : `coroutine´ `(´ Expr `)´
Resume : `resume´ Expr `(´ Expr `)´
Spawn  : `spawn´ Expr `(´ Expr `)´
```

The operation `coroutine` expects a coroutine prototype (type
[`coro`](#execution-units) or [`task`](#execution-units)) and returns an active
coroutine (type [`x-coro`](#execution-units) or [`x-task`](#executions-units)).

The operation `resume` expects an active coroutine, and resumes it.
The coroutine executes until it yields or terminates.
The `resume` evaluates to the argument of `yield` or to the coroutine return
value.

The operation `spawn T(...)` expands to operations `coroutine` and `resume` as
follows: `resume (coroutine(T))(e)`.

<a name="status"/>

### 5.7.2. Status

The operation `status` returns the status of the given active coroutine:

```
Status : `status´ `(´ Expr `)´
```

As described in [Active Values](#active-values), a coroutine has 4 possible
status:

1. `yielded`: idle and ready to be resumed
2. `toggled`: ignoring resumes
3. `resumed`: currently executing
4. `terminated`: terminated and unable to be resumed

<a name="yield"/>

### 5.7.3. Yield

The operation `yield` suspends the execution of a running coroutine:

```
Yield : `yield´ `(´ Expr `)´
```

An `yield` expects an expression between parenthesis that is returned to whom
resumed the coroutine.
Eventually, the suspended coroutine is resumed again with a value and the whole
`yield` is substituted by that value.

<!--
If the resume came from a [`broadcast`](#broadcast), then the given expression is
lost.
-->

<a name="resumeyield-all"/>

### 5.7.4. Resume/Yield All

The operation `resume-yield-all´ continuously resumes the given active
coroutine, collects its yields, and yields upwards each value, one at a time.
It is typically use to delegate job of an outer coroutine transparently to an
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
    } until (status(co) == :terminated)
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
println(a1, a2, a3, a4, a5)             ;; <-- 2, 5, 7, 8, 10
```

<a name="toggle"/>

### 5.7.5. Toggle

The operation `toggle` configures an active coroutine to ignore or acknowledge
further `resume` operations:

```
Toggle : `toggle´ Expr `(´ Expr `)´
```

A `toggle` expects an active coroutine and a [boolean](#basic-types) value
between parenthesis.
If the toggle is set to `true`, the coroutine will ignore further `resume`
operations, otherwise it will execute normally.

<a name="task-operations"/>

## 5.8. Task Operations

A task can refer to itself with the identifier `task`.

A task has a public `pub` variable that can be accessed as a
[field](#indexes-and-fields):
    internally as `task.pub`, and
    externally as `x.pub` where `x` is a reference to the task.

In addition to the coroutines API, tasks also rely on the following operations:

1. [`spawn`](#create-resume-spawn): creates and resumes a new task from a prototype
2. [`await`](#await): yields the resumed task until it matches an event
3. [`broadcast`](#broadcast): broadcasts an event to all tasks

Examples:

```
task T (x) {
    set task.pub = x            ;; sets 1 or 2
    await :number               ;; awakes from broadcast
    println(task.pub + evt)     ;; --> 11 or 12
}
val t1 = spawn T(1)
val t2 = spawn T(2)
println(t1.pub, t2.pub)         ;; --> 1, 2
broadcast 10                    ;; evt = 10
```

```
task T () {
    await true
}
val tsk = spawn T()
val trk = track(tsk)
println(tsk, detrack(trk))      ;; --> x-task: 0x...    x-task: 0x...
broadcast true                  ;; terminates task, clears track
println(tsk, detrack(trk))      ;; --> x-task: 0x...    x-task: nil
```

```
task T () {
    await :number
    println(evt)
}
val ts = tasks()                ;; pool of tasks
do {
    spawn in ts, T()            ;; attached to outer pool,
    spawn in ts, T()            ;; not to enclosing block
}
broadcast 10                    ;; --> 10 \n 10
```

<a name="await"/>

### 5.8.1. Await

The operation `await` suspends the execution of a running task until a
condition is true:

```
Await : `await´ [`:check-now`] (
            | Expr
            | TAG [`,´ Expr]
            | [Expr `:h´] [Expr `:min´] [Expr `:s´] [Expr `:ms´]
        )
```

An `await` is expected to be used in conjunction with [event
broadcasts](#broadcast), allowing the condition expression to query the
variable `evt` with the occurring event.

All await variations are expansions based on `yield`.

An `await <e>` expands as follows:

```
yield()                 ;; omit if :check-now is set
loop if not <e> {
    yield ()
}
```

The expansion yields while the condition is false.
When the optional tag `:check-now` is set, the condition is tested immediately,
and the coroutine may not yield at all.

An `await <tag>, <e>` expands as follows:

```
yield() ;; (unless :check-now)
loop if not ((evt is <tag>) [and <e>]) {
    yield ()
}
```

The expansion yields until the `evt` is of the given tag.
The optional `<e>` is also required to be true if provided.

Given a time expression, an `await <time>` sleeps for a number of milliseconds
and expands as follows:

```
val ms = <...>              ;; time expression
loop if ms > 0 {
    await :frame            ;; assumes a :frame event
    set ms = ms - evt[0]    ;;  with the elapsed ms at [0]
}
```

The expansion yields until the expected number of milliseconds elapses from
occurrences of `:frame` events representing the passage of time.
The time expression expects the format `<e>:h <e>:min <e>:s <e>:ms` and is
converted to milliseconds.

`TODO: configurable :frame event`

Examples:

```
await true                          ;; awakes on any broadcast
await :key, evt.press==:release     ;; awakes on :key with press=:release
await 1:h 10:min 30:s               ;; awakes after the specified time
```

<a name="broadcast"/>

### 5.8.2. Broadcast
<a name="track-and-detrack"/>

### 5.8.3. Track and Detrack
<a name="pools-of-tasks"/>

### 5.8.4. Pools of Tasks
<a name="sintax-extensions-blocks"/>

### 5.8.5. Sintax Extensions Blocks
<a name="every-block"/>

#### 5.8.5.1. Every Block

An `every` block is a loop that makes an iteration whenever an await condition
is satisfied:

```
Every : `every´ <awt> Block
```

An `every <awt> { <es> }` expands to a loop as follows:

```
loop {
    await <awt>
    <es>
}
```

Any [`await`](#await) variation can be used as `<awt>`.
It is assumed that `<es>` does not `await` to satisfy the meaning of "every".

Examples:

```
every 1:s {
    println("1 more second has elapsed")
}
```

<a name="spawn-blocks"/>

#### 5.8.5.2. Spawn Blocks

A spawn block spawns an anonymous task:

<a name="parallel-blocks"/>

#### 5.8.5.3. Parallel Blocks

A parallel block spawns multiple anonymous tasks concurrently:

```
Par     : `par´     Block { `with´ Block }
Par-And : `par-and´ Block { `with´ Block }
Par-Or  : `par-or´  Block { `with´ Block }
```

A `par` never rejoins, even if all spawned tasks terminate.
A `par-and` rejoins when all spawned tasks terminate.
A `par-or` rejoins when any spawned task terminates, aborting the others.

A `par { <es1> } with { <es2> }` expands as follows:

```
do {
    spawn {
        <es1>           ;; first task
    }
    spawn {
        <es2>           ;; second task
    }
    await false         ;; never rejoins
}

```

A `par-and { <es1> } with { <es2> }` expands as follows:

```
do {
    val t1 = spawn {
        <es1>           ;; first task
    }
    val t2 = spawn {
        <es2>           ;; second task
    }
    await :check-now (  ;; rejoins when all tasks terminate
        status(t1)==:terminated and status(t2)==:terminated
    )
}
```

A `par-or { <es1> } with { <es2> }` expands as follows:

```
do {
    val t1 = spawn {
        <es1>           ;; first task
    }
    val t2 = spawn {
        <es2>           ;; second task
    }
    await :check-now (  ;; rejoins when any task terminates
        status(t1)==:terminated or status(t2)==:terminated
    )
}                       ;; aborts other active tasks
```

Examples:

```
TODO
```

<a name="awaiting-block"/>

#### 5.8.5.4. Awaiting Block
<a name="toggle-block"/>

#### 5.8.5.5. Toggle Block

<!-- ---------------------------------------------------------------------- -->

<!--
      | `broadcast´ [`in´ Expr `,´] Expr                ;; broadcast event
      | `tasks´ `(´ Expr `)´                            ;; pool of tasks
      | `spawn´ `in´ Expr `,´ Expr `(´ Expr `)´         ;; spawn task in pool
      | `loop´ `in´ :tasks Expr `,´ ID Block     ;; tasks iterator

      | `awaiting´ Await Block                          ;; abort on event
      | `toggle´ Await `->´ Await Block                 ;; toggle task on/off on events

Operations
      | `not´ Expr                                      ;; op not
      | Expr (`or´|`and´|`is´|`is-not´) Expr            ;; op bin
      | Expr `[´ (`=´|`+´|`-´) `]´                      ;; ops peek,push,pop
        not, or, and are really special
-->

<a name="standard-library"/>

# 6. STANDARD LIBRARY

<a name="primary-library"/>

## 6.1. Primary Library

`TODO`

<a name="auxiliary-library"/>

## 6.2. Auxiliary Library

`TODO`

<a name="syntax"/>

# 7. SYNTAX

<a name="basic-syntax"/>

## 7.1. Basic Syntax

```
Prog  : { Expr [`;´] }
Block : `{´ { Expr [`;´] } `}´
Expr  : `do´ [:unnest[-hide]] Block                     ;; explicit block
      | `defer´ Block                                   ;; defer statements
      | `pass´ Expr                                     ;; innocuous expression

      | `val´ ID [TAG] [`=´ Expr]                       ;; declaration constant
      | `var´ ID [TAG] [`=´ Expr]                       ;; declaration variable
      | `set´ Expr `=´ Expr                             ;; assignment

      | `enum´ `{´ List(TAG [`=´ Expr]) `}´             ;; tags enum
      | `data´ Data                                     ;; tags templates
            Data : TAG `=´ `[´ List(ID [TAG]) `]´
                    [`{´ { Data } `}´]

      | `nil´ | `false´ | `true´                        ;; literals &
      | NAT | TAG | CHR | NUM                           ;; identifiers
      | ID | `err´ | `evt´ | `...´

      |  `[´ [List(Expr)] `]´                           ;; tuple
      | `#[´ [List(Expr)] `]´                           ;; vector
      | `@[´ [List(Key-Val)] `]´                        ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´

      | `(´ Expr `)´                                    ;; parenthesis
      | OP Expr                                         ;; pre op
      | Expr OP Expr                                    ;; bin op
      | Expr `(´ [List(Expr)] `)´                       ;; pos call

      | Expr `[´ Expr `]´                               ;; pos index
      | Expr `.´ ID                                     ;; pos dict field
      | Expr `.´ `pub´                                  ;; pos task pub

      | `if´ Expr Block [`else´ Block]                  ;; conditional
      | `loop´ `if´ Expr Block                          ;; loop while

      | `catch´ Expr Block                              ;; catch exception
      | `throw´ `(´ Expr `)´                            ;; throw exception

      | `func´ `(´ [List(ID)] `)´ Block                 ;; function
      | `coro´ `(´ [List(ID)] `)´ Block                 ;; coroutine
      | `task´ `(´ [List(ID)] `)´ Block                 ;; task

      | `coroutine´ `(´ Expr `)´                        ;; create coro
      | `status´ `(´ Expr `)´                           ;; coro status
      | `yield´ `(´ Expr `)´                            ;; yield from coro
      | `resume´ Expr `(´ Expr `)´                      ;; resume coro
      | `toggle´ Expr `(´ Expr `)´                      ;; toggle coro

      | `broadcast´ [`in´ Expr `,´] Expr                ;; broadcast event
      | `track´ `(´ Expr `)´                            ;; track task
      | `detrack´ `(´ Expr `)´                          ;; detrack task
      | `tasks´ `(´ Expr `)´                            ;; pool of tasks
      | `spawn´ `in´ Expr `,´ Expr `(´ Expr `)´         ;; spawn task in pool
      | `loop´ `in´ :tasks Expr `,´ ID Block            ;; loop iterator tasks

List(x) : x { `,´ x }                                   ;; comma-separated list

ID    : [A-Za-z_][A-Za-z0-9_\'\?\!\-]*                  ;; identifier variable
      | `{´ OP `}´                                      ;; identifier operation
TAG   : :[A-Za-z0-9\.\-]+                               ;; identifier tag
OP    : [+-*/><=!|&~%#@]+                               ;; identifier operation
CHR   : '.' | '\\.'                                     ;; literal character
NUM   : [0-9][0-9A-Za-z\.]*                             ;; literal number
NAT   : `.*`                                            ;; native expression
```

<a name="extended-syntax"/>

## 7.2. Extended Syntax

```
Expr  : Expr' [`where´ Block]                           ;; where clause
Expr' : STR
      | `not´ Expr                                      ;; op not
      | Expr `[´ (`=´|`+´|`-´) `]´                      ;; ops peek,push,pop
      | Expr `.´ NUM                                    ;; op tuple index
      | Expr (`or´|`and´|`is´|`is-not´) Expr            ;; op bin
      | TAG `[´ [List(Expr)] `]´                        ;; tagged tuple

      | `ifs´ `{´ {Case} [Else] `}´                     ;; conditionals
            Case : Expr `->´ (Expr | Block)
            Else : `else´ `->´ (Expr | Block)
      | `ifs´ Expr `{´ {Case} [Else] `}´                ;; switch + conditionals
            Case : [`==´ | `is´] Expr `->´ (Expr | Block)
            Else : `else´ `->´ (Expr | Block)

      | `loop´ Block                                    ;; loop infinite
      | `loop´ Block `until´ Expr                       ;; loop until
      | `loop´ `in´ Expr `,´ ID Block                   ;; loop iterator
      | `loop´ `in´                                     ;; loop iterator numeric
            (`[´ | `(´)
            Expr `->´ Expr
            (`]´ | `)´)
            [`,´ :step Expr]
            `,´ ID Block

      | `func´ ID `(´ [List(ID)] [`...´] `)´ Block      ;; declaration func
      | `coro´ ID `(´ [List(ID)] [`...´] `)´ Block      ;; declaration coro
      | `task´ ID `(´ [List(ID)] [`...´] `)´ Block      ;; declaration task

      | `spawn´ Expr `(´ Expr `)´                       ;; spawn coro
      | `spawn´ Block                                   ;; spawn anonymous task
      | `await´ Await                                   ;; await event
      | `resume-yield-all´ Expr `(´ Expr `)´            ;; resume-yield nested coro
      | `every´ Await Block                             ;; await event in loop
      | `awaiting´ Await Block                          ;; abort on event
      | `par´ Block { `with´ Block }                    ;; spawn tasks
      | `par-and´ Block { `with´ Block }                ;; spawn tasks, rejoin on all
      | `par-or´ Block { `with´ Block }                 ;; spawn tasks, rejoin on any
      | `toggle´ Await `->´ Await Block                 ;; toggle task on/off on events

Await : [`:check-now`] (                                ;; check before yield
            | Expr                                      ;; await condition
            | TAG `,´ Expr                              ;; await tag
            | [Expr `:h´] [Expr `:min´] [Expr `:s´] [Expr `:ms´] ;; await time
            | `spawn´ Expr `(´ Expr `)´                 ;; await task
        )

STR   : ".*"                                            ;; string expression
```
