#!/bin/sh

javac -classpath "lib/jade.jar:lib/org/newdawn/slick.jar" -d classes $(find src -name *.java)
