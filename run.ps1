$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
javac -encoding UTF-8 -d out -cp 'lib/*' -sourcepath src src/Main.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java --enable-native-access=ALL-UNNAMED -cp 'out;lib/*' Main
exit $LASTEXITCODE
