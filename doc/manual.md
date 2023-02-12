# 1. LEXICAL RULES

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
    +     -     *     /
    >     <     =     !
    |     &     ~     %     #
```

Operators names cannot clash with reserved symbols.

## 1.4. Identifiers

Ceu uses identifiers to refer to variables and operators:

```
VAR ::= [A-Za-z_][A-Za-z0-9_'?!-]*      ;; letter/under/digit/quote/quest/excl/dash
OP  ::= [+-*/><=!|&~%#]+                ;; see Operators
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
NIL  ::= nil
BOOL ::= true | false
TAG  ::= :[A-Za-z0-9\.\-]+      ;; colon + leter/digit/dot/dash
NUM  ::= [0-9][0-9A-Za-z\.]*    ;; digit/letter/dot
CHR  ::= '.' | '\.'             ;; single/backslashed character
STR  ::= ".*"                   ;; string expression
NAT  ::= `.*`                   ;; native expression
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

A native literal is a sequence of characters enclosed by back quotes (`````).

Nat:
- $
- :ret

Examples:

```
nil             ;; nil literal
false           ;; bool literal
:X.Y            ;; tag literal
1.25            ;; number literal
'a'             ;; char literal
"Hello!"        ;; string literal
`sin($x)`       ;; native literal
```

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
      | NAT | ID | TAG | CHR | NUM                      ;; identifiers

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
CHR   : '.' | '\\.'                                     ;; literal character
NUM   : [0-9][0-9A-Za-z\.]*                             ;; literal number
NAT   : `.*`                                            ;; native expression
```

## Extended Syntax

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

