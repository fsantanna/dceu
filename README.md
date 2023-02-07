# SYNTAX

```
Block : `{´ { Expr } `}´
Expr  : `do´ Block                              ;; block
      | `val´ ID [TAG] [`=´ Expr]               ;; declaration constant
      | `var´ ID [TAG] [`=´ Expr]               ;; declaration variable
      | `set´ Expr `=´ Expr                     ;; assignment

      | `evt´ | `nil´ | `false` | `true´        ;; constants
      | NAT | ID | TAG | CHAR | NUM             ;; literals

      |  `[´ [List(Expr)] `]´                   ;; tuple
      | `#[´ [List(Expr)] `]´                   ;; vector
      | `@[´ [List(Key-Val)] `]´                ;; dictionary
            Key-Val : ID `=´ Expr
                    | `(´ Expr `,´ Expr `)´

      | `if´ Expr Block [`else´ Block]          ;; conditional
      | `catch´ Expr Block                      ;; catch exception
      | `defer´ Block                           ;; defer expressions
      | `pass´ Expr                             ;; innocuous expression

      | `loop´ Block                            ;; loop infinite
      | `loop´ `if´ Block                       ;; loop while
      | `loop´ `in´ Expr `,´ ID Block           ;; loop iterator
      | `loop´ `in´ :tasks Expr `,´ ID Block    ;; loop iterator tasks
      | `loop´ `in´                             ;; loop iterator numeric
            (`[´ | `(´)
            Expr `->´ Expr
            (`]´ | `)´)
            [`,´ :step Expr]
            `,´ ID Block

      | `func´ `(´ [List(ID)] `)´ Block         ;; function
      | `coro´ `(´ [List(ID)] `)´ Block         ;; coroutine
      | `task´ `(´ [List(ID)] `)´ Block         ;; task

      | `enum´ `{´ List(TAG [`=´ Expr]) `}´     ;; tags enum
      | `data´ Data                             ;; tags hierarchy
            Data : TAG `=´ List(ID [TAG]) [`{´ { Data } `}´]

      | `spawn´ `in´
      | `broadcast´
      | `yield´
      | `resume´
      | `toggle´

;; DERIVED

      | `ifs´ `{´ {Case} [Else] `}´             ;; conditionals
            Case : Expr `->´ (Expr | Block)
            Else : `else´ `->´ (Expr | Block)
      | `ifs´ Expr `{´ {Case} [Else] `}´        ;; switch + conditionals
            Case : [`==´ | `is´] Expr `->´ (Expr | Block)
            Else : `else´ `->´ (Expr | Block)

      | `await´
      | `every´
      | `par´
      | `par-and´
      | `par-or´
      | `awaiting´
      | `func / coro / task´
      | `toggle´

List(x) : x { `,´ x }
```
