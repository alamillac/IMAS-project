#!/bin/sh

javac -classpath lib/jade.jar -d classes $(find src -name *.java)
