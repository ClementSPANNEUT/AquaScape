param([switch]$Restart)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$options = @()
if ($Restart) { $options += '--restart' }
java dev/DevLoop.java @options
exit $LASTEXITCODE
