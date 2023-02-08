#!/bin/sh

# 1. Edit version:
#   - README.md
#   - Main.kt
#   - install.sh
#   - build.sh
# 2. Build version:
#   - XCEU = true
#   - Build artifacts...
#   - ./build.sh

# EDIT:
# - build.sh
# - install.sh
# - README.md
#
# BUILD:
# $ ./build.sh
# $ ls -l *.zip
#
# UPLOAD:
# - https://github.com/Freechains/README/releases/new
# - tag    = <version>
# - title  = <version>
# - Attach = { .zip, install.sh }
#
# TEST
# $ cd /data/freechains/bin/
# $ wget https://github.com/Freechains/README/releases/download/v0.10.0/install-v0.10.0.sh
# $ sudo sh install-v0.10.0.sh /usr/local/bin
# $ freechains --version
# $ ./start-sync-xx.sh      (crontab -e, see ssmtp)
# $ ./setup-post.sh         (only once)

VER=v0.1.0
DIR=/tmp/ceu-build/

rm -Rf $DIR
rm -f  /tmp/ceu-$VER.zip
mkdir -p $DIR

cp ceu.sh $DIR/ceu
cp xprelude.ceu $DIR/prelude.ceu
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
