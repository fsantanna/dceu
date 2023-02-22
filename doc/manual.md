# The Programming Language Ceu

```
1. LEXICON
    1. Keywords
    2. Symbols
    3. Operators
    4. Identifiers
    5. Literals
    6. Comments
2. TYPES
    1. Basic Types
    2. Collections
    3. Execution Units
    4. User Types
3. VALUES
    1. Plain Values
    2. Dynamic Values
    3. Active Values
4. EXPRESSIONS
    1. Program and Blocks
    2. Declarations and Assignments
    3. Tag Enumerations and Tuple Templates
    4. Literals, Identifiers, and Constructors
    5. Calls, Operations, and Indexing
    6. Conditionals and Loops
    7. Exceptions
    8. Execution Units
    9. Operating Coroutines and Tasks
5. STANDARD LIBRARY
    1. Primary Library
    2. Auxiliary Library
X. EXTENSIONS
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
    coro            ;; coroutine prototype
    coroutine       ;; create coroutine
    data            ;; data declaration
    defer           ;; defer block
    do              ;; do block
    else            ;; else block
    enum            ;; enum declaration             (10)
    err             ;; exception variable
    every           ;; every block
    evt             ;; event variable
    false           ;; false value
    func            ;; function prototype
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
    resume          ;; resume coroutine
    set             ;; assign expression
    spawn           ;; spawn coroutine
    status          ;; coroutine status
    task            ;; task prototype/self identifier
    toggle          ;; toggle coroutine/block
    true            ;; true value
    until           ;; until loop modifier
    val             ;; constant declaration
    var             ;; variable declaration         (40)
    where           ;; where block
    with            ;; with block
    yield           ;; yield coroutine
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
    #[              ;; vector constructor
    @[              ;; dictionary constructor
    '   "   `       ;; character/string/native delimiters
    $               ;; native interpolation
    ^               ;; lexer annotation
```

## 1.3. Operators

The following operator symbols can be combined to form operator names in Ceu:

```
    +    -    *    /
    >    <    =    !
    |    &    ~    %
    #    @
```

Operators names cannot clash with reserved symbols (e.g., `->`).

## 1.4. Identifiers

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
(see [Operators](#TODO)).
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

The literal `nil` is the single value of the [*nil*](#TODO) type.

The literals `true` and `false` are the values of the [*bool*](#TODO) type.

A [*tag*](#TODO) type literal starts with a colon (`:`) and is followed by
letters, digits, dots (`.`), or dashes (`-`).
A dot or dash must be followed by a letter or digit.

A [*number*](#TODO) type literal starts with a digit and is followed by digits,
letters, and dots (`.`), and adheres to the [C standard](#TODO).

A [*char*](#TODO) type literal is a single or backslashed (`\`) character
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

The function `type` returns the type of a value as a [tag](#TODO):

```
type(10)  --> :number
type('x') --> :char
```

## 2.1. Basic Types

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
Each tag is internally associated with a natural number that represents a
unique value in a global enumeration.
Tags are also known as *symbols* or *atoms* in other programming languages.
Tags can be explicitly [enumerated](#TODO) to interface with [native
expressions](#TODO).
Tags can form [hierachies](#TODO) to represent user types and describe
[tuple templates](#TODO).

## 2.2. Collections

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

## 2.3. Execution Units

Ceu provide 3 types of execution units, functions, coroutines, and tasks:

```
func    coro    task
x-coro  x-task  x-tasks  x-track
```

The `func` type represents [function prototypes](#TODO).

The `coro` type represents [coroutine prototypes](#TODO), while the `x-coro`
type represents [active coroutines](#TODO).

The `task` type represents [task prototypes](#TODO), while the `x-task` type
represents [active tasks](#TODO).
The `x-tasks` type represents [task pools](#TODO) holding active tasks.
The `x-track` type represents [track references](#TODO) pointing to active
tasks.

Execution units are described in [Section TODO](#TODO).

## 2.4. User Types

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

# 3. VALUES

As a dynamic language, each value in Ceu carries extra information, such as its
own type.

## 3.1. Plain Values

A *plain value* does not require dynamic allocation since it only carries extra
information about its type.
All [basic types](#TODO) have plain values:

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
This is also valid for [coroutines](#TODO) and [tasks](#TODO).
This restriction permits that terminating blocks deallocate all dynamic values
attached to them.

Ceu also provides an explicit [move](#TODO) operation to reattach a dynamic
value to an outer scope.

Nevertheless, a dynamic value is still subject to garbage collection, given
that it may loose all references to it, even with its enclosing block active.

## 3.3. Active Values

An *active value* corresponds to an active coroutine, task, pool of tasks,
or tracked reference:

```
x-coro  x-task  x-tasks  x-track
```

An active value is still a dynamic value, with all properties described above.
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
Block : `{´ { Expr } `}´
```

A sequence of expressions evaluate to its last expression.

### 4.1.1. Blocks

A block delimits a lexical scope for variables and dynamic values:
A variable is only visible to expressions in the block in which it was
declared.
A dynamic value cannot escape the block in which it was created (e.g., from
assignments or returns), unless it is [moved](#TODO) out.
For this reason, when a block terminates, all memory that was allocated inside
it is automatically reclaimed.
This is also valid for [coroutines](#TODO) and [tasks](#TODO), which are
attached to the block in which they were created.

A block is not an expression by itself, but it can be turned into one by
prefixing it with an explicit `do`:

```
Do : `do´ [:unnest[-hide]] Block   ;; an explicit block expression
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

Blocks also appear in compound statements, such as [conditionals](#TODO),
[loops](#TODO), and many others.

### 4.1.2. Defer

A deferred block executes only when its enclosing block terminates:

```
Defer : `defer´ Block
```

Deferred expression execute in reverse order in which they appear in the source
code.

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

### 4.1.3. Pass

The `pass` expression permits that an innocuous expression is used in the
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

## 4.2. Declarations and Assignments

Regardless of being dynamically typed, all variables in Ceu must be declared
before use:

```
Val : `val´ ID [TAG] [`=´ Expr]
Var : `var´ ID [TAG] [`=´ Expr]
```

The difference between `val` and `var` is that a `val` is immutable, while a
`var` declaration can be modified by further `set` expressions:

```
`set´ Expr `=´ Expr
```

The optional initialization expression assigns an initial value to the
variable, which is set to `nil` otherwise.

The `val` modifier forbids that a name is reassigned, but it does not prevent
that [dynamic values](#TODO) are modified.

Optionally, a declaration can be associated with a [tuple template](#TODO) tag,
which allows the variable to be indexed by a field name, instead of a numeric
position.
Note that the variable is not guaranteed to hold a value matching the template,
not even a tuple is guaranteed.
The template association is static but with no runtime guarantees.

`TODO: evt`

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

## 4.3. Tag Enumerations and Tuple Templates

Tags are global identifiers that need not to be predeclared.
However, they may be explicitly delcared when used as enumerations or tuple
templates.

### 4.3.1. Tag Enumerations

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

### 4.3.2. Tuple Templates

A `data` declaration associates a tag with a tuple template, which associates
tuple positions with field identifiers:

```
Temp : `data´ Data
            Data : TAG `=´ `[´ List(ID [TAG]) `]´
                    [`{´ { Data } `}´]
```

Then, a [variable declaration](#TODO) can specify a tuple template and hold a
tuple that can be accessed by field.

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

Based on [tags and sub-tags](#TODO), tuple templates can define hierarchies and
reuse fields from parents.
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

## 4.4. Literals, Identifiers, and Constructors

[Literals](#TODO) (for the simple types) and [identifiers](#TODO) (for
variables and operators) are the most basic expressions of Ceu:

```
Basic : `nil´ | `false´ | `true´
      | NAT | TAG | CHR | NUM | ID
      | `...´
      | `err´ | `evt´
```

The symbol `...` represents the variable arguments (*varargs*) a function
receives in a call.
In the context of a [function](#TODO) that expects varargs, it evaluates to a
tuple holding the varargs.
In other scenarios, accessing `...` raises an error.
When `...` is the last argument of a call, its tuple is expanded as the last
arguments.

The variables `err` and `evt` have special scopes and are automatically setup
in the context of [`throw`](#TODO) and [`broadcast`](#TODO) expressions,
respectively.

Ceu provides constructors for [collections](#TODO) to allocate tuples, vectors,
and dictionaries:

```
Cons : `[´ [List(Expr)] `]´             ;; tuple
     | `#[´ [List(Expr)] `]´            ;; vector
     | `@[´ [List(Key-Val)] `]´         ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´
     | TAG `[´ [List(Expr)] `]´         ;; tagged tuple
```

Tuples (`[...]`) and vectors (`#[...]`) are built with a list of expressions.

Dictionaries (`@[...]`) are built with a list of pairs of expressions
(`(key,val)`), in which each pair maps a key to a value.
The first expression is the key, and the second is the value.
If the key is a tag, the alternate syntax `tag=val` may be used (omitting the
tag `:`).

A tuple constructor may also be prefixed with a tag, which associates the tag
with the tuple, e.g., `:X [...]` is equivalent to `tags([...], :X, true)`.
Tag constructors are typically used in conjunction with [tuple templates](#TODO)

Examples:

```
10                  ;; a nil expression
:x                  ;; a tag expression
{++}                ;; an op as an expression
[(:x,10), x=10]     ;; a dictionary with equivalent key mappings
:Pos [10,10]        ;; a tagged tuple
```

## 4.5. Calls, Operations, and Indexing

### 4.5.1. Calls and Operations

In Ceu, calls and operations are equivalent, i.e., an operation is a call that
uses an [operator](#TODO) with prefix or infix notation:

```
Call : OP Expr                      ;; unary operation
     | Expr OP Expr                 ;; binary operation
     | Expr `(´ [List(Expr)] `)´    ;; function call
```

Operations are interpreted as function calls, i.e., `x + y` is equivalent to
`{+} (x, y)`.

A call expects an expression of type [`func`](#TODO) and an optional list of
expressions as arguments enclosed by parenthesis (`(` and `)`).
Each argument is expected to match a parameter of the function declaration.
A call transfers control to the function, which runs to completion and returns
control with a value, which substitutes the call.

As discussed in [Identifiers](#TODO), the binary minus requires spaces around
it to prevent ambiguity with identifiers containing dashes.

Examples:

```
#vec            ;; unary operation
x - 10          ;; binary operation
{-}(x,10)       ;; operation as call
f(10,20)        ;; normal call
```

### 4.5.2. Indexes and Fields

Collections in Ceu are ([tuples](#TODO), [vectors](#TODO), and
[dictionaries](#TODO)) are accessed through indexes or fields:

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
For a dictionary `v`, and a [tag literal](#TODO) `k` (with the colon `:`
omitted), the operation expands to `v[:k]`.

`TODO: tuple template`

A [task](#TODO) `t` also relies on a field operation to access its public
field `pub` (i.e., `t.pub`).

Examples:

```
tup[3]      ;; tuple access by index
tup.3       ;; tuple access by numeric field

vec[i]      ;; vector access by index

dict[:x]    ;; dict access by index
dict.x      ;; dict access by field

t.pub        ;; task public field
```

### 4.5.3. Precedence and Associativity

Operations in Ceu can be combined in complex expressions with the following
precedence priority (from higher to lower):

``
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

## 4.6. Conditionals and Loops

### 4.6.1. Conditionals

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

### 4.6.2. Loops and Iterators

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

There is no `break` expression in Ceu, which can be substituted by a proper
test condition or [`throw-catch`](#TODO) pair.

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

#### 4.6.2.1. Iterators

Ceu supports generic iterators, tasks iterators, and numeric iterators as
follows:

```
Iter : `loop´ `in´ Expr `,´ ID Block            ;; generic iterator
     | `loop´ `in´ :tasks Expr `,´ ID Block     ;; tasks iterator
     | `loop´ `in´                              ;; numeric iterator
            (`[´ | `(´)
            Expr `->´ Expr
            (`]´ | `)´)
            [`,´ :step Expr]
            `,´ ID Block
```

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
val x-or-y =        ;; max between x and y
    if x > y {
        x
    } else {
        y
    }

var i = 0
loop if i<5 {       ;; --> 0,1,2,3,4
    println(i)
    set i = i + 1
}
```

Ceu also provides syntactic extensions for [`ifs`](#TODO) with multiple
conditions and [`loop in`](#TODO) iterators.

## 4.7. Exceptions

A `throw` raises an exception that terminates all enclosing blocks up to a
matching `catch` block:

```
Throw : `throw´ `(´ Expr `)´
Catch : `catch´ Expr Block
```

A `throw` receives an expression that is assigned to the special variable
`err`, which is visible to enclosing `catch` statements.
A `throw` is propagated upwards and aborts all enclosing blocks and [execution
units](#TODO) on the way.
When crossing an execution unit, a `throw` jumps back to the calling site and
continues to propagate upwards.

A `catch` executes its associated block normally, but also registers a catch
expression to be compared against `err` when a `throw` is crossing it.
If they match, the exception is caught and the `catch` terminates, aborting its
associated block, and properly triggering nested [`defer`](#TODO) expressions.

To match an exception, the `catch` expression can access `err` and needs to
evaluate to `true`.
If the matching expression `x` is of type [tag](#TODO), it expands to match
`err is x`, allowing to check [tuple templates](#TODO).

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

## 4.8. Execution Units

Ceu supports functions, coroutines, and tasks as execution units:

```
Func : `func´ `(´ [List(ID)] `)´ Block
Coro : `coro´ `(´ [List(ID)] `)´ Block
Task : `task´ `(´ [List(ID)] `)´ Block
```

Each keyword is followed by an optional list of identifiers as parameters
enclosed by parenthesis (`(` and `)`).
The last parameter can be the symbol `...`, which captures as a tuple all
remaining arguments of a call.
The associated block executes when the unit is [called](#TODO).
Each argument in the call is evaluated and copied to the parameter identifier,
which becomes a local variable in the execution block.

### 4.8.1. Functions

A `func` is a conventional function or subroutine, which blocks the caller and
runs to completion, finally returning a value to the caller, which resumes
execution.

### 4.8.2. Coroutines

The `coro` and `task` are coroutine prototypes that, when instantiated, can
suspend themselves in the middle of execution, before they terminate.
A coroutine retains its execution state and can be resumed from the suspension
point.

The basic API for coroutines has 6 operations:

1. [`coroutine`](#TODO): creates a new coroutine from a prototype
2. [`yield`](#TODO): suspends the resumed coroutine
3. [`resume`](#TODO): starts or resumes a coroutine from its current suspension point
4. [`toggle`](#TODO): `TODO`
5. [`kill`](#TODO): `TODO`
6. [`status`](#TODO): returns the coroutine status

Note that `yield` is the only operation that is called from the coroutine
itself, all others are called from the user code controlling the coroutine.
Just like call arguments and return values from functions, the `yield` and
`resume` operations can transfer values between themselves.

A coroutine has 4 possible status:

1. `yielded`: idle and ready to be resumed
2. `toggled`: paused and ignoring resumes
3. `resumed`: currently executing
4. `terminated`: terminated and unable to be resumed

A coroutine is [attached](#TODO) to the enclosing block in which it was
created.
This means that it is possible that a coroutine goes out of scope with the
yielded status.
In this case, the coroutine body is aborted and nested [`defer`](#TODO)
expressions are properly triggered.

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

### 4.8.3. Tasks

A `task` is a coroutine prototype that, when instantiated, awakes automatically
from [event broadcasts](#TODO) without an explicit `resume`.
When awaking, tasks have access to the special variable `evt` set from
broadcasts.

A task can refer to itself with the identifier `task`.

A task has a public `pub` variable that can be accessed as a [field](#TODO):
    internally as `task.pub`, and
    externally as `x.pub` where `x` is a reference to the task.

A task can be spawned in a [pool](#TODO) of anonymous tasks, which will
control the task lifecycle and automatically release it from memory on
termination.
In this case, the task is also attached to the block in which the pool is
declared.

A task can be [tracked](#TODO) from outside with a safe reference to it.
When a task terminates, it broadcasts an event that clears all of its tracked
references.

In addition to the coroutines API, tasks also rely on the following operations:

1. [`spawn`](#TODO): creates and resumes a new task from a prototype
2. [`await`](#TODO): yields the resumed task until it matches an event
3. [`broadcast`](#TODO): broadcasts an event to all tasks

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

## 4.9. Operating Coroutines and Tasks

### 4.9.1. Create, Resume, Spawn

The operation `coroutine` creates a new coroutine from a [protoype](#TODO).
The operation `resume` executes a coroutine starting from its last suspension
point.
The operation `spawn` creates and resumes a coroutine:

```
Create : `coroutine´ `(´ Expr `)´
Resume : `resume´ Expr `(´ Expr `)´
Spawn  : `spawn´ Expr `(´ Expr `)´
```

The operation `coroutine` expects a coroutine prototype (type [`coro`](#TODO)
or [`task`](#TODO)) and returns an active coroutine (type [`x-coro`](#TODO) or
[`x-task`](#TODO)).

The operation `resume` expects an active coroutine, and resumes it.
The coroutine executes until it yields or terminates.
The `resume` evaluates to the argument of `yield` or to the coroutine return
value.

The operation `spawn T(...)` expands to operations `coroutine` and `resume` as
follows: `resume (coroutine(T))(e)`.

### 4.9.2. Yield and Await

The operations `yield` and `await` suspend the execution of coroutines and
tasks:

```
Yield : `yield´ `(´ Expr `)´
Y-All : `yield´ `:all´ Expr
Await : `await´ [`:check-now`] (
            | Expr
            | TAG `,´ Expr
            | { Expr (`:h´|`:min´|`:s´|`:ms´) }
        )
```

#### 4.9.2.1 Yield

An `yield` suspends the running coroutine and expects an expression between
parenthesis (`(` and `)`) that is returned to whom resumed the coroutine.
If the resume came from a [`broadcast`](#TODO), then the given expression is
lost.
Eventually, the suspended coroutine is resumed again with a value and the whole
`yield` is substituted by that value.

An `yield :all` continuously resumes the given active coroutine, and yields
each of its values upwards.
The expression `yield :all <co>` is equivalent to the expansion as follows:

```
loop in iter(<co>), <v> {
    yield(<v>)
}
```

The expansion transforms the active coroutine into an [iterator](#TODO), which
resumes the coroutine until it terminates.
For each resume iteration, it collects the yielded values from `<co>` into `<v>`.
Each collected value is yielded upwards.

#### 4.9.2.2 Await

An `await` suspends the running coroutine until a condition is true.
In its simplest form `await <e>`, it expands as follows:

```
yield()                 ;; ommit if :check-now is set
loop if not <e> {
    yield ()
}
```

The expansion yields while the condition is false.
When the optional tag `:check-now` is set, the condition is tested immeditally,
and the coroutine may not yield at all.

The `await` is expected to be used in conjuntion with [event broadcasts](#TODO),
allowing the condition expression to query the variable `evt` with the
occurring event.

      | `status´ `(´ Expr `)´                           ;; coro status
      | `toggle´ Call                                   ;; toggle task
      | `broadcast´ [`in´ Expr `,´] Expr                ;; broadcast event
      | `tasks´ `(´ Expr `)´                            ;; pool of tasks
      | `spawn´ `in´ Expr `,´ Expr `(´ Expr `)´         ;; spawn task in pool

<!-- ---------------------------------------------------------------------- -->

<!--

# EXTENSIONS

## Operations

Ceu
      | `not´ Expr                                      ;; op not
      | Expr (`or´|`and´|`is´|`is-not´) Expr            ;; op bin
      | Expr `[´ (`=´|`+´|`-´) `]´                      ;; ops peek,push,pop

not, or, and are really special
-->

# A. SYNTAX

## A.1. Basic Syntax

```
Prog  : { Expr }
Block : `{´ { Expr } `}´
Expr  : `do´ [:unnest[-hide]] Block                     ;; explicit block
      | `defer´ Block                                   ;; defer expressions
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
      | `loop´ `in´ :tasks Expr `,´ ID Block            ;; loop iterator tasks

      | `catch´ Expr Block                              ;; catch exception
      | `throw´ `(´ Expr `)´                            ;; throw exception

      | `func´ `(´ [List(ID)] `)´ Block                 ;; function
      | `coro´ `(´ [List(ID)] `)´ Block                 ;; coroutine
      | `task´ `(´ [List(ID)] `)´ Block                 ;; task

      | `coroutine´ `(´ Expr `)´                        ;; create coro
      | `status´ `(´ Expr `)´                           ;; coro status
      | `yield´ `(´ Expr `)´                            ;; yield from coro
      | `resume´ Expr `(´ Expr `)´                      ;; resume coro
      | `toggle´ Call                                   ;; toggle coro
      | `broadcast´ [`in´ Expr `,´] Expr                ;; broadcast event
      | `tasks´ `(´ Expr `)´                            ;; pool of tasks
      | `spawn´ `in´ Expr `,´ Expr `(´ Expr `)´         ;; spawn task in pool

List(x) : x { `,´ x }                                   ;; comma-separated list

ID    : [A-Za-z_][A-Za-z0-9_\'\?\!\-]*                  ;; identifier variable
      | `{´ OP `}´                                      ;; identifier operation
TAG   : :[A-Za-z0-9\.\-]+                               ;; identifier tag
OP    : [+-*/><=!|&~%#@]+                               ;; identifier operation
CHR   : '.' | '\\.'                                     ;; literal character
NUM   : [0-9][0-9A-Za-z\.]*                             ;; literal number
NAT   : `.*`                                            ;; native expression
```

## A.2. Extended Syntax

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

      | `func´ ID `(´ [List(ID)] `)´ Block              ;; declaration func
      | `coro´ ID `(´ [List(ID)] `)´ Block              ;; declaration coro
      | `task´ ID `(´ [List(ID)] `)´ Block              ;; declaration task

      | `spawn´ Expr `(´ Expr `)´                       ;; spawn coro
      | `spawn´ Block                                   ;; spawn anonymous task
      | `yield´ `:all´ Expr                             ;; yield from other coro
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
