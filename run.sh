#!/bin/sh
set -e
cd "$(dirname "$0")"
SEP=:
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; esac
javac -encoding UTF-8 -d out -cp "lib/*" -sourcepath src src/Main.java
java --enable-native-access=ALL-UNNAMED -cp "out${SEP}lib/*" Main
