# 前端冒烟测试：解析 smoke 样例并输出 AST
# 用法: .\scripts\smoke-parse.ps1 [样例文件]
param(
    [string]$File = "src/test/resources/smoke/add.tc"
)

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "==> smoke parse: $File"
mvn -q compile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$cpFile = Join-Path $env:TEMP "toyc-cp.txt"
mvn -q -DincludeScope=runtime "-Dmdep.outputFile=$cpFile" dependency:build-classpath
$cp = "target/classes;" + (Get-Content $cpFile -Raw).Trim()
java -cp $cp com.compiler.parser.AstDumpMain $File
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "==> smoke junit"
mvn -q test -Dtest=SmokeParseTest
exit $LASTEXITCODE
