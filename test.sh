#!/bin/sh
set -e
cd "$(dirname "$0")"
JUNIT=lib/junit-platform-console-standalone-6.1.3.jar
SEP=:
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; esac
GAMEPAD="lib/jamepad-2.30.0.0.jar${SEP}lib/gdx-jnigen-loader-2.2.0.jar"

rm -rf out out-test coverage
javac -encoding UTF-8 -d out -cp "$GAMEPAD" $(find src -name '*.java')
javac -encoding UTF-8 -d out-test -cp "out${SEP}${JUNIT}${SEP}${GAMEPAD}" $(find test -name '*.java')

if [ "$1" = "--coverage" ]; then
    java --enable-native-access=ALL-UNNAMED -javaagent:lib/org.jacoco.agent-0.8.15-runtime.jar=destfile=coverage/jacoco.exec \
        -jar "$JUNIT" execute --class-path "out${SEP}out-test${SEP}${GAMEPAD}" --scan-class-path --disable-banner
    java -jar lib/org.jacoco.cli-0.8.15-nodeps.jar report coverage/jacoco.exec \
        --classfiles out --sourcefiles src --html coverage/html --csv coverage/coverage.csv --encoding UTF-8
    echo "Rapport de couverture : coverage/html/index.html"
else
    java --enable-native-access=ALL-UNNAMED -jar "$JUNIT" execute --class-path "out${SEP}out-test${SEP}${GAMEPAD}" \
        --scan-class-path --disable-banner
fi
