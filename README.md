# toyc-compiler

编译系统实践课程项目：将 **ToyC** 源程序编译为 **RISC-V32** 汇编代码。

## 项目简介

本项目实现一个多阶段编译器，流水线如下：

```
ToyC 源码 (stdin)
    → 词法/语法分析 (A) → AST
    → 语义分析 (B)     → 带符号信息的 AST
    → IR 生成 (C)      → 三地址码
    → 汇编生成 (D)     → RISC-V32 汇编 (stdout)
```

- **输入**：从标准输入读取 ToyC 源码
- **输出**：向标准输出写入 RISC-V32 汇编
- **可选参数**：`-opt` 开启优化（性能测试时使用）

本地测试示例：

```bash
java -jar target/toyc-rv32-1.0-SNAPSHOT.jar < input.tc > output.s
```

## 技术栈

| 项目 | 版本/工具 |
|------|-----------|
| 语言 | Java 21 |
| 构建 | Maven 3.9+ |
| 词法/语法 | ANTLR 4.13.1 |
| 目标平台 | RISC-V32 |

## 环境要求

- JDK 21
- Maven 3.9+
- （本地验证汇编时）RISC-V 工具链，如 `riscv64-unknown-elf-gcc` + QEMU / Spike

## 快速开始

### 克隆与构建

```bash
git clone <仓库地址>
cd toyc-compiler
mvn clean package
```

构建成功后生成可执行 JAR：`target/toyc-rv32-1.0-SNAPSHOT.jar`

### 运行

```bash
# 方式 1：JAR + 重定向
java -jar target/toyc-rv32-1.0-SNAPSHOT.jar < tests/hello.tc > out.s

# 方式 2：开启优化
java -jar target/toyc-rv32-1.0-SNAPSHOT.jar -opt < tests/hello.tc > out.s
```

### 仅编译（不打包）

```bash
mvn compile
```

## 项目结构

```
toyc-compiler/
├── pom.xml                          # Maven 构建配置
├── 任务要求.md                       # 课程任务与 ToyC 语言定义
├── 接口说明.md                       # 模块间接口契约（AST / IR / 符号表）
└── src/
    ├── main/
    │   ├── antlr4/com/compiler/parser/
    │   │   └── ToyC.g4              # ToyC 文法（ANTLR）
    │   └── java/com/compiler/
    │       ├── ast/                 # AST 节点与 AstVisitor（成员 A）
    │       ├── parser/              # 前端入口 ToyCFrontend、AstBuilder（成员 A）
    │       ├── semantic/            # 符号表与语义分析（成员 B）
    │       ├── ir/                  # 中间代码 IR（成员 C）
    │       ├── backend/             # IR → RISC-V 汇编（成员 D）
    │       ├── driver/              # 主程序 CompilerMain（成员 D 集成）
    │       └── utils/               # 公共工具
    └── test/java/com/compiler/      # 单元测试
```

## 模块分工

| 成员 | 分支建议 | 包路径 | 职责 |
|------|----------|--------|------|
| **A** | `dev-a` | `ast`, `parser` | ANTLR 文法、AST 定义、解析器 |
| **B** | `dev-b` | `semantic` | 符号表、类型检查、常量折叠 |
| **C** | `dev-c` | `ir` | 三地址码生成、短路求值、优化 |
| **D** | `dev-d` | `backend`, `driver` | 汇编生成、寄存器分配、集成与测试 |

各模块之间的接口约定详见 [接口说明.md](./接口说明.md)。

## 核心接口

### 前端入口（A 提供）

```java
CompUnitNode ast = ToyCFrontend.parse(sourceCode);
```

### 语义分析（B 实现）

```java
class SemanticVisitor implements AstVisitor<Void> { ... }
```

### IR 生成（C 实现）

```java
class IrVisitor implements AstVisitor<IrList> { ... }
```

### 主程序（D 集成）

主类：`com.compiler.driver.CompilerMain`（读取 stdin → 编译 → 输出汇编）

## 协作规范

1. **分支策略**：禁止直推 `main`，每人使用专属分支 `dev-a` / `dev-b` / `dev-c` / `dev-d`，每日合并前确保本地基本用例可跑通。
2. **接口冻结**：第 1 天必须确定 AST 节点结构与模块接口，变更需全员评审。
3. **错误处理**：采用 Fail-fast，语义错误直接 `throw new SemanticException(...)`，不做错误恢复。
4. **桩模块**：下游可先用 Stub 推进开发，不阻塞等待上游完成。


## 相关文档

- [任务要求.md](./任务要求.md) — ToyC 语言文法、评测方式与评分标准
- [接口说明.md](./接口说明.md) — AST / 符号表 / IR 模块契约

## 许可证

本项目为编译系统实践课程作业，仅供学习交流使用。
