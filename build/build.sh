#!/bin/sh

# 1. Edit version:
#   - README.md
#   - Main.kt
#   - install.sh
#   - build.sh
# 2. Build:
#   - XCEU = true
#   - Build artifacts...
#   - ./build.sh
# 3. Upload:
#   - https://github.com/fsantanna/dceu/releases/new
#   - tag    = <version>
#   - title  = <version>
#   - Attach = { .zip, install.sh }

VER=v0.2.1
DIR=/tmp/ceu-build/

rm -Rf $DIR
rm -f  /tmp/ceu-$VER.zip
mkdir -p $DIR

cp ceu.sh $DIR/ceu
cp prelude.ceu $DIR/prelude.ceu
cp hello-world.ceu $DIR/
cp ../out/artifacts/dceu_jar/dceu.jar $DIR/ceu.jar

cd /tmp/
zip ceu-$VER.zip -j ceu-build/*

echo "-=-=-=-"

cd -

cd $DIR/
./ceu --version
echo "-=-=-=-"
./ceu hello-world.ceu
echo "-=-=-=-"

cd -
cp install.sh install-$VER.sh
cp /tmp/ceu-$VER.zip .

ls -l install-*.sh ceu-*.zip
echo "-=-=-=-"
unzip -t ceu-$VER.zip
