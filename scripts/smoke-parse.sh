# 前端冒烟测试：解析 smoke 样例并输出 AST
# 用法: ./scripts/smoke-parse.sh [样例文件]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

FILE="${1:-src/test/resources/smoke/add.tc}"

echo "==> smoke parse: $FILE"
mvn -q compile
CP="target/classes:$(mvn -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout dependency:build-classpath)"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
"$JAVA" -cp "$CP" com.compiler.parser.AstDumpMain "$FILE"

echo
echo "==> smoke junit"
mvn -q test -Dtest=SmokeParseTest
