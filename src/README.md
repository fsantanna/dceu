# Compile from sources

1. Clone the source repository:

```
$ git clone https://github.com/fsantanna/dyn-lex/
```

2. Install the Java SDK:

```
sudo apt install default-jdk
```

3. Open `IntelliJ IDEA` (version `2023.1.2`):
    - Open project/directory `dyn-lex/`
    - Wait for all imports (takes long...)
    - Run self tests:
        - On the left pane, click tab `Project`:
            - Right click fold `dceu -> src -> test -> kotlin`
            - Click "Run 'All Tests'"
    - Generate artifacts (maybe can already skip to next step?):
        - Click `File -> Project Structure -> Artifacts -> + -> JAR -> From modules with dependencies`
        - Click `Module -> dceu`
        - Click `OK`
        - Verify that `dceu:jar` appears at the top
        - Click `OK`
    - Rebuild artifacts:
        - Click `Build -> Build artifacts -> Build`

3. Install `dyn-lex`:

```
$ cd dceu/
$ make
```

4. Use `dyn-lex`:

```
$ ./dceu build/hello-world.ceu
```

5. Use `dyn-lex` with SDL:

```
$ cd pico/
$ git clone https://github.com/fsantanna/pico-sdl sdl/
$ cd tst/
$ ../../dceu --lib=pico all.ceu
```
