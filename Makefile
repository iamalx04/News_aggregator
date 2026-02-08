CLASSPATH = "libs/*:."

MAIN = main.Aggregator

.PHONY: build run clean

build:
	javac -cp $(CLASSPATH) main/*.java

run:
	java -cp $(CLASSPATH) $(MAIN) $(ARGS)

clean:
	rm -f main/*.class