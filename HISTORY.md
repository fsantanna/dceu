v0.2 (may'23)
-------------

- Additions
    - `...` as main program arguments
    - dynamic cast `:X v.x`
    - implicit tag declaration `val x = :X ...`
    - assignments in conditions: `if`, `ifs`, `until`
    - functions
        - `in?` function
        - `string-to-tag`
        - `===`, `=/=` for deep equality
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
- Removals
    - ` { :x ... }` block tags

v0.1 (mar'23)
-------------

- first release
