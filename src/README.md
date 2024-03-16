# Compile from sources

1. Clone the source repository:

```
git clone https://github.com/fsantanna/dceu/
```

2. Install the Java SDK:

```
sudo apt install default-jdk
```

3. Open `IntelliJ IDEA` (version `2023.1.2`):
    - Open project/directory `dceu/`
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

3. Install `ceu`:

```
cd dceu/
make DIR=..
```

4. Use `ceu`:

```
../ceu build/hello-world.ceu
```
