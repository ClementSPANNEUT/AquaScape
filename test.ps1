param([switch]$Coverage)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$junit = 'lib/junit-platform-console-standalone-6.1.3.jar'
$gamepad = 'lib/jamepad-2.30.0.0.jar;lib/gdx-jnigen-loader-2.2.0.jar'

Remove-Item -Recurse -Force out, out-test, coverage -ErrorAction SilentlyContinue
javac -encoding UTF-8 -d out -cp $gamepad (Get-ChildItem -Recurse src -Filter *.java).FullName
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
javac -encoding UTF-8 -d out-test -cp "out;$junit;$gamepad" (Get-ChildItem -Recurse test -Filter *.java).FullName
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($Coverage) {
    java --enable-native-access=ALL-UNNAMED -javaagent:lib/org.jacoco.agent-0.8.15-runtime.jar=destfile=coverage/jacoco.exec `
        -jar $junit execute --class-path "out;out-test;$gamepad" --scan-class-path --disable-banner
    $tests = $LASTEXITCODE
    java -jar lib/org.jacoco.cli-0.8.15-nodeps.jar report coverage/jacoco.exec `
        --classfiles out --sourcefiles src --html coverage/html --csv coverage/coverage.csv --encoding UTF-8
    Write-Output 'Rapport de couverture : coverage/html/index.html'
    exit $tests
}
java --enable-native-access=ALL-UNNAMED -jar $junit execute --class-path "out;out-test;$gamepad" `
    --scan-class-path --disable-banner
exit $LASTEXITCODE
