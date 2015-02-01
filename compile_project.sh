#!/bin/sh

javac -XDignore.symbol.file=true -classpath "lib/jade.jar:lib/org/newdawn/slick.jar" -d classes $(find src -name *.java)
