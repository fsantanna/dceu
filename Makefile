install:
	cp out/artifacts/dceu_jar/dceu.jar /usr/local/bin/ceu.jar
	cp build/ceu.sh /usr/local/bin/ceu
	cp build/xprelude.ceu /usr/local/bin/prelude.ceu
	ls -l /usr/local/bin/ceu*
	ceu --version
	ceu build/hello-world.ceu

one:
	ceu $(SRC).ceu -cc "-g"
