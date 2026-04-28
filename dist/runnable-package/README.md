# td-bin-packing 交付包

## 环境要求

- Java 17 或以上版本

## 目录说明

- `td-bin-packing.jar`：可执行程序
- `run.sh`：统一启动脚本
- `examples/box-sweep/`：箱型推荐示例输入
- `examples/batch/cases-batch.csv`：批量装箱示例输入
- `output/`：建议输出目录

## 快速开始

直接运行全部内置示例：

```bash
./run.sh
```

这个命令会依次执行：

- 内置单例演示
- `batch` 批量装箱示例
- `box-sweep` 箱型推荐示例

运行完成后，会在 `output/demo-showcase/` 下生成 CSV 和 HTML 结果。

## 单独运行内置单例

```bash
./run.sh single
```

或直接运行：

```bash
java -jar td-bin-packing.jar
```

会在当前工作目录生成 `output/packing-visualization.html`。

## 运行箱型推荐示例

```bash
./run.sh box-sweep \
  examples/box-sweep/items.csv \
  examples/box-sweep/box-types.csv \
  examples/box-sweep/cases.csv \
  output/recommendation
```

输出内容：

- `output/recommendation/summary.csv`
- `output/recommendation/instances/<caseId>/box-*.html`

## 运行 batch 批量装箱示例

```bash
./run.sh batch examples/batch/cases-batch.csv output/batch
```

输出内容：

- `output/batch/batch-summary.csv`
- `output/batch/visualizations/<caseId>.html`

## 其他模式

真实订单实验模式：

```bash
./run.sh real-orders <数据目录> <输出目录> [采样数量] [随机种子]
```
