install:
	mkdir -p $(DIR)
	cp out/artifacts/dceu_jar/dceu.jar $(DIR)/ceu.jar
	cp build/ceu.sh $(DIR)/ceu
	cp build/xprelude.ceu $(DIR)/prelude.ceu
	ls -l $(DIR)/
	$(DIR)/ceu --version
	$(DIR)/ceu build/hello-world.ceu
