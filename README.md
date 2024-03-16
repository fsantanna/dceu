# The Programming Language Ceu (v0.3)

Ceu is a [synchronous programming language][1] that reconciles *[Structured
Concurrency][2]* with *[Event-Driven Programming][3]* to extend classical
structured programming:

- Structured Deterministic Concurrency:
    - A set of structured primitives to lexically compose concurrent tasks
      (e.g., `spawn`, `par-or`, `toggle`).
    - A synchronous and deterministic scheduling policy, which provides
      predictable behavior and safe abortion of tasks.
- Event Signaling Mechanisms:
    - An `await` primitive to suspend a task and wait for events.
    - A `broadcast` primitive to signal events and awake awaiting tasks.

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
- Exception handling (throw & catch)
- Hierarchical Tags and Tuple Templates (for data description)
- Seamless integration with C (source-level compatibility)

Ceu is in **experimental stage**.
Both the compiler and runtime can become very slow.

[1]: https://fsantanna.github.io/sc.html
[2]: https://en.wikipedia.org/wiki/Structured_concurrency
[3]: https://en.wikipedia.org/wiki/Event-driven_programming
[4]: https://en.wikipedia.org/wiki/Esterel
[5]: https://en.wikipedia.org/wiki/Lua_(programming_language)

# Hello World!

Displays `Hello World!` every second, until 10 seconds elapse:

```
spawn {
    watching :10:s {
        every :1:s {
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
wget https://github.com/fsantanna/dceu/releases/download/v0.3.0/install-v0.3.0.sh
sh install-v0.3.0.sh ./ceu/
```

- You may want to
    - add `./ceu/` to your `PATH`
    - modify `./ceu/` to another destination

3. Execute `ceu`:

```
./ceu/ceu ./ceu/hello-world.ceu
hello
world
```

# pico-ceu

The best way to try Ceu is through [`pico-ceu`][6], a graphical library based
on [SDL][7]:

1. Install `SDL`:

```
sudo apt install libsdl2-dev libsdl2-image-dev libsdl2-mixer-dev libsdl2-ttf-dev libsdl2-gfx-dev
```

1. Clone `pico-ceu`:

```
cd ceu/
git clone https://github.com/fsantanna/pico-ceu pico/
```

2. Clone `pico-sdl`:

```
cd pico/
git clone https://github.com/fsantanna/pico-sdl sdl/
```

3. Execute `pico-ceu`:

```
cd tst/
../../ceu --lib=pico all.ceu
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
    - Run with `pico-ceu` in `ceu/pico/tst/`:
        - `../../ceu --lib=pico click-drag-cancel-x.ceu`
- Comparison with JS generators:
    - https://github.com/fsantanna/dceu/blob/main/src/test/kotlin/xceu/TXJS.kt
- A simple but complete 2D game in Ceu:
    - https://github.com/fsantanna/pico-ceu-rocks
    - Clone in `ceu/pico/`, cd to it, and run with `pico-ceu`:
        - `../../ceu --lib=pico main.ceu`
- Academic publications:
    - http://ceu-lang.org/chico/#ceu
