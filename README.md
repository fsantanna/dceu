# The Programming Language Ceu (v0.5)

Ceu is a [synchronous programming language][1] that reconciles *[Structured
Concurrency][2]* with *[Event-Driven Programming][3]* in order to extend
classical structured programming with three main functionalities:

- Structured Deterministic Concurrency:
    - A set of structured primitives to lexically compose concurrent tasks
      (e.g., `spawn`, `par-or`, `toggle`).
    - A synchronous and deterministic scheduling policy, which provides
      predictable behavior and safe abortion of tasks.
    - A container primitive to hold dynamic tasks, which automatically releases
      them as they terminate.
- Event Signaling Mechanisms:
    - An `await` primitive to suspend a task and wait for events.
    - A `broadcast` primitive to signal events and awake awaiting tasks.
- Lexical Memory Management *(experimental)*:
    - A lexical policy to manage dynamic allocation automatically.
    - A set of strict escaping rules to preserve structured reasoning.
    - A reference-counter collector for deterministic reclamation.

Ceu is inspired by [Esterel][4] and [Lua][5].

Follows a summary of the main ideas in the design of Ceu:

- https://github.com/fsantanna/dceu/blob/main/doc/manual-out.md#1-design

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

# Hello World!

During 10 seconds, displays `Hello World!` every second:

```
spawn {
    watching <10:s> {
        every <1:s> {
            println("Hello World!")
        }
    }
}
```

# Manual

- https://github.com/fsantanna/dceu/blob/main/doc/manual-out.md

# Install

1. Install `gcc` and `java`:

```
sudo apt install gcc default-jre
```

2. Install `ceu`:

```
wget https://github.com/fsantanna/dceu/releases/download/v0.5.0/install-v0.5.0.sh
sh install-v0.5.0.sh ./ceu/
```

- We assume that you add `./ceu/` (the full path) to your environment `$PATH`.

3. Execute `ceu`:

```
ceu ./ceu/hello-world.ceu
hello
world
```

# pico-ceu

The best way to try Ceu is through `pico-ceu`, a graphical library based on
[SDL][7]:

- <https://github.com/fsantanna/pico-ceu>

# Resources

- A toy Problem: Drag, Click, or Cancel
    - https://fsantanna.github.io/toy.html
    - Run with `pico-ceu` in `ceu/pico/tst/`:
        - `ceu --lib=pico click-drag-cancel-x.ceu`
- Comparison with JS generators:
    - https://github.com/fsantanna/dceu/blob/main/src/test/kotlin/tst_99/JS_99.kt
- A simple but complete 2D game in Ceu:
    - https://github.com/fsantanna/pico-ceu-rocks
    - Clone in `ceu/pico/`, `cd` to it, and run with `pico-ceu`:
        - `ceu --lib=pico main.ceu`
- Academic publications:
    - http://ceu-lang.org/chico/#ceu
- Mailing list (JOIN US!):
    - https://groups.google.com/g/ceu-lang

[1]: https://fsantanna.github.io/sc.html
[2]: https://en.wikipedia.org/wiki/Structured_concurrency
[3]: https://en.wikipedia.org/wiki/Event-driven_programming
[4]: https://en.wikipedia.org/wiki/Esterel
[5]: https://en.wikipedia.org/wiki/Lua_(programming_language)
[6]: https://github.com/fsantanna/pico-ceu
[7]: https://www.libsdl.org/
