# V1.0.0 API 使用文档

本版本提供两种调用方式：

1. Java 函数 API：适合被其他 Java 项目直接依赖并在进程内调用。
2. 可执行 JAR：适合命令行、脚本、批处理或非 Java 系统通过进程调用。

## 1. 交付文件

```text
release/V1.0.0/
├── lib/
│   └── td-bin-packing-api-1.0.0.jar
├── tests/
│   ├── input/
│   └── output/
└── docs/
```

JAR 使用 Java 8 目标版本编译。

## 2. Java 函数 API

### 2.1 导入 JAR

如果调用方是 Maven 项目，可以先把 JAR 安装到本地 Maven 仓库：

```bash
mvn install:install-file \
  -Dfile=release/V1.0.0/lib/td-bin-packing-api-1.0.0.jar \
  -DgroupId=com.zeroffa \
  -DartifactId=td-bin-packing-api \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

然后在调用方 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.zeroffa</groupId>
    <artifactId>td-bin-packing-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

如果调用方不是 Maven 项目，也可以直接把 `td-bin-packing-api-1.0.0.jar` 加入 classpath。

### 2.2 调用方式

程序边界 API 是：

```java
com.zeroffa.tdbinpacking.api.PackingApi
```

示例：

```java
import com.zeroffa.tdbinpacking.api.PackingApi;

public class Demo {
    public static void main(String[] args) {
        String inputJson = "{...}";
        String outputJson = new PackingApi().packJson(inputJson);
        System.out.println(outputJson);
    }
}
```

方法签名：

```java
public String packJson(String inputJson)
```

输入是完整请求 JSON 字符串，输出是完整响应 JSON 字符串。

## 3. 可执行 JAR

### 3.1 文件输入和文件输出

```bash
java -jar release/V1.0.0/lib/td-bin-packing-api-1.0.0.jar \
  --input release/V1.0.0/tests/input/input1.json \
  --output /tmp/input1.output.json
```

也支持位置参数：

```bash
java -jar release/V1.0.0/lib/td-bin-packing-api-1.0.0.jar \
  release/V1.0.0/tests/input/input1.json \
  /tmp/input1.output.json
```

### 3.2 JSON 字符串输入

```bash
java -jar release/V1.0.0/lib/td-bin-packing-api-1.0.0.jar \
  --json '{"items":[],"containers":[],"orders":[],"lclRules":[],"unpackJudge":0}'
```

### 3.3 标准输入

```bash
cat release/V1.0.0/tests/input/input1.json | \
java -jar release/V1.0.0/lib/td-bin-packing-api-1.0.0.jar --stdin
```

## 4. 响应字段

顶层字段：

- `code`：状态码，`0` 表示成功，`1` 表示失败或业务规则不满足。
- `msg`：状态说明。
- `containerCount`：本次输出实际使用的箱子数量。
- `containerUtilizations`：每个箱子的空间利用率数组，数组索引与 `containerSequence` 顺序一致。
- `averageUtilization`：所有已用箱子的平均利用率。
- `data.infos`：订单明细到箱子的分配结果。

`data.infos[].containerSequence` 从 `1` 开始编号。

## 5. 验证命令

```bash
java -jar release/V1.0.0/lib/td-bin-packing-api-1.0.0.jar \
  --input release/V1.0.0/tests/input/input1.json
```

该命令会在标准输出打印响应 JSON。
