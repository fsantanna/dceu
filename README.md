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
