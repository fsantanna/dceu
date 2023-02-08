install:
	cp out/artifacts/dceu_jar/dceu.jar /x/ceu/ceu.jar
	cp build/ceu.sh /x/ceu/ceu
	cp build/xprelude.ceu /x/ceu/prelude.ceu
	ls -l /x/ceu/
	ceu --version
	ceu build/hello-world.ceu

one:
	ceu $(SRC).ceu -cc "-g"
