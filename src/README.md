# Compile from sources

1. Clone the source repository:

```
git clone https://github.com/fsantanna/dceu/
```

2. Install `gcc`, `make`, and the Java SDK:

```
sudo apt install gcc make default-jdk
```

3. Open `IntelliJ IDEA` (version `2023.1.2`):
    - Open project/directory `dceu/`
    - Wait for all imports (takes long...)
    - Run self tests:
        - On the left pane, click tab `Project`:
            - Left click fold `dceu -> src -> main -> kotlin`
                - Double click file `Main.kt`
                    - set `var CEU = 1`
            - Left click fold `dceu -> src -> test -> kotlin`
                - Right click fold `test_01`
                - Left click `Run 'Testes in tst_01'`
            - Repeat the last two steps for `2`,`3`,`4`,`5`,`99` (skip `6`)
        - It is ok if tests with prefixes `TODO` and `BUG` fail.
    - Generate artifacts (maybe can already skip to next step?):
        - Click `File -> Project Structure -> Artifacts -> + -> JAR -> From modules with dependencies`
        - Click `Module -> dceu`
        - Click `OK`
        - Verify that `dceu:jar` appears at the top
        - Click `OK`
    - Rebuild artifacts:
        - Click `Build -> Build artifacts -> Build`

3. Install `ceu`:

```
cd dceu/
make DIR=..
```

4. Use `ceu`:

```
../ceu build/hello-world.ceu
```
