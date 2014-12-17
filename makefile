default: Main.class

agent: Main.class

Main.class: Main.java
	javac Main.java

run:
	java Main

clean:
	rm *.class