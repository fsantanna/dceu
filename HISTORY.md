v0.5 (oct'24)
-------------

- Additions:
    - lexical memory management (lex): back - experimental
    - statements
        - `drop` (lex): dettach value from block
        - `escape`: to escape from blocks
        - `return`: to return from functions
    - standard library
        - `random.bool`, `random.int`
- Changes:
    - exceptions: tag required as argument

v0.4 (jul'24)
-------------

- Additions
    - pattern matching: `match`, `await`, `loop`, `val`
    - statements
        - `group`: to group commands without a block
        - `test`: for tests
        - `match`: for pattern matching
    - tags: arithmetic and relational operations
- Changes
    - lambda syntax: `\{...}` -> `{\...}`
    - clock syntax: `:1:ms` -> `<1:ms>`
    - `break` `skip`: no nesting restrictions
    - standard library modules: `to.*`, `math.*`, `random.*`
- Removals
    - `...` variable arguments
    - `pass` (`do`) statement

v0.3 (mar'24)
-------------

- Additions
    - condition patterns (`ifs`, `catch`, `await`)
    - pipe operators `->` `-->` `<--` `<-`
- Changes
    - arrow clause `->` to `=>`
    - types `x-coro` `x-task` `x-tasks` -> `exe-coro` `exe-task` `tasks`
    - statements
        - `throw` -> `error`
        - `loop if` `loop until` -> `break if` `skip if` `until` `while`
        - `awaiting` -> `watching`
        - `pass` -> `do`
- Removals
    - lexical memory management for dynamic allocation
    - `evt` `err` variables
    - closure modifiers
    - `track` statement
    - `if` `ifs` condition declaration
    - tags `:tmp` `:check-now` `:fake`
- Full re-implementation

v0.2 (may'23)
-------------

- Additions
    - `--verbose` (compile option)
    - `...` as main program arguments
    - dynamic cast `:X v.x`
    - implicit tag declaration `val x = :X ...`
    - assignments in conditions: `if`, `ifs`, `until`
    - functions
        - `in?` / `in-not?` functions
        - `to-tag`
        - `===`, `=/=` for deep equality
        - `math-` `sin` `cos` `floor`
    - statements
        - `if ... -> ... -> ...
        - `loop-while` clause
        - `every-while-until` clause
        - `thus { ... }` (pipe operator)
        - `spawn coro`
    - lambda syntax:
        - `\x{ x }`
        - `f \{ ... }` (call w/o parens)
        - `it` as implicit arg (also for `loop`, `func`, `thus`)
    - iterators:
        - `func` iterator
        - tag modifiers `:all`, `:idx`, `:key`, `:val`
        - dict iterator defaults to `:key`
- Changes
    - `is`, `is-not` -> `is?`, `is-not?`
    - `ifs`: case separators are either `-> ...` or `{ ... }`
    - `do :unnset` -> `export`
    - `print` shows tags
    - `func :rec` modifier
    - `evt` and `detrack(...)` values cannot be assigned
    - better compilation time
- Removals
    - ` { :x ... }` block tags

v0.1 (mar'23)
-------------

- first release
