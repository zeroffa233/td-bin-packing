# 示例运行

目录说明：

- `example/example_data/`：典型示例输入
- `example/output/`：示例输出目录
- `example/run_demo.sh`：批量运行示例脚本

运行方式：

```bash
./example/run_demo.sh
```

脚本会先执行：

```bash
mvn -q -DskipTests compile
```

然后使用 CLI 入口逐个处理 `example/example_data/*.json`，输出到 `example/output/`。
