# The Programming Language Ceu

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
- Expression based (statements are expressions)
- Stackless coroutines (the basis of tasks)
- Restricted closures (upvalues must be explicit and final)
- Deferred statements (for finalization)
- Exception handling
- Dynamic collections (tuples, vectors, and dictionaries)
- Hierarchical tuple templates (for data description with inheritance)
- Seamless integration with C (source-level compatibility)

- DESIGN OF CEU:
    - https://github.com/fsantanna/dceu/blob/main/doc/manual-out.md#1-design

Ceu is in **experimental stage**.
Both the compiler and runtime can become very slow.

[1]: https://en.wikipedia.org/wiki/Synchronous_programming_language
[2]: https://en.wikipedia.org/wiki/Structured_concurrency
[3]: https://en.wikipedia.org/wiki/Event-driven_programming
[4]: https://en.wikipedia.org/wiki/Esterel
[5]: https://en.wikipedia.org/wiki/Lua_(programming_language)

# Manual

- https://github.com/fsantanna/dceu/blob/main/doc/manual-out.md

# Install

1. Install `gcc` and `java`:

```
$ sudo apt install gcc default-jre
```

2. Install `ceu`:

```
$ wget https://github.com/fsantanna/dceu/releases/download/v0.1.0/install-v0.1.0.sh
$ sh install-v0.1.0.sh ./ceu/
```

- You may want to
    - add `./ceu/` to your `PATH`
    - modify `./ceu/` to another destination

3. Execute `ceu`:

```
$ ./ceu/ceu ./ceu/hello-world.ceu
[0,hello]
[1,world]
```

# pico-ceu

The best way to try Ceu is through [`pico-ceu`][6], a graphical library based
on [SDL][7]:

1. Install `SDL`:

```
$ sudo apt install libsdl2-dev libsdl2-image-dev libsdl2-mixer-dev libsdl2-ttf-dev libsdl2-gfx-dev
```

1. Clone `pico-ceu`:

```
$ cd ceu/
$ git clone https://github.com/fsantanna/pico-ceu pico/
```

2. Clone `pico-sdl`:

```
$ cd pico/
$ git clone https://github.com/fsantanna/pico-sdl sdl/
```

3. Execute `pico-ceu`:

```
$ cd ../../ # back to your initial working directory
$ ./ceu/ceu --lib=pico ./ceu/pico/tst/par.ceu
```

- Your directory hierarchy should become as follows:

```
+ ceu/
|---+ pico/
    |---+ sdl/
```

[6]: https://github.com/fsantanna/pico-ceu
[7]: https://www.libsdl.org/

# Resources

- A toy Problem: Drag, Click, or Cancel
    - https://fsantanna.github.io/toy.html
    - Run with `pico-ceu`:
        - `ceu --lib=pico ./ceu/pico/tst/click-drag-cancel.ceu`
- Comparison with JS generators:
    - https://github.com/fsantanna/dceu/blob/main/src/test/kotlin/xceu/TXJS.kt
- A simple but complete 2D game in Ceu:
    - https://github.com/fsantanna/pico-ceu-rocks
    - Clone, cd to it, and run with `pico-ceu`:
        - `ceu --lib=pico main.ceu`
