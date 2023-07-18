# Lexical Memory Management

As described in the [design of Ceu](manual-out.md#1-design), lexical memory
management is one of the key aspects of the language:

- Even dynamic allocation is attached to lexical blocks.
- Strict escaping rules to preserve structure reasoning.
- Garbage collection restricted to local references only.

- goal
- basic concepts
- basic rules

## Dynamic Values

Ceu respects the lexical structure of the program even when dealing with
dynamic values, which allocate new memory.
Ceu supports 10 types with [dynamic values](manual-out#dynamic-values), which
are all subject to lexical memory management:

```
tuple | vector | dict                  ;; collections
func | coro | task                     ;; prototypes
x-coro | x-task | x-tasks | x-track    ;; active values (next section)
```

In the next example, the tuple `[1,2,3]` is a dynamic value that allocates
memory and is subject to lexical memory management.

```
do {
    val tup = [1,2,3]
    println(tup)
}
```

## Holding Blocks

A dynamic value is always attached to exactly one [block](manual-out.md#blocks)
at any given time.
The value can move between blocks, but remains attached to a single holding
block.
The value is automatically released from memory when the holding block
terminates.

In the next example, the tuple `[1,2,3]` is attached to the enclosing `do`
block and is automatically released from memory when the block terminates:

```
do {                        ;; holding block
    val tup = [1,2,3]       ;; dynamic tuple
    println(tup)
}                           ;; releases the tuple
```

## Holding States

```
HOLD-FLEETING       ;; not assigned, dst assigns
HOLD-MUTABLE        ;; set and assignable to narrow
HOLD-IMMUTABLE      ;; set but not assignable across unsafe (even if same/narrow)
HOLD-EVENT          ;;
```

## Constructors

When a dynamic value is first allocated through a constructor, it is attached
by default to the closest enclosing lexical block.

Each one of the 10 dynamic types have a primitive constructor to create values:

```
[...]               ;; tuple
#[...]              ;; vector
@[...]              ;; dict

func () { ... }     ;; func
coro () { ... }     ;; coro
task () { ... }     ;; task

coroutine(...)      ;; x-coro
spawn T(...)        ;; x-task
tasks(...)          ;; x-tasks
track(...)          ;; x-track
```

## Assignments

Examples:

```
do {
    do {
        [1,2,3]
    }
}
```

When a [dynamic value](#dynamic-values) is first assigned to a variable, it
becomes attached to the [block](#block) in which the variable is declared, and
the value cannot escape that block in further assignments or as return
expressions.

- TDrop.kt
- extend w design decisions
- doc as primitive, not copy-move
- refer to manual
- move -> drop
- all dyns have exactly 0 or 1 block
- some are fixed
- FLEET, VAR, FIXED
- protos, closures and never-closures
- protos, coros and tasks
- tracks, refs
- pubs
- events
