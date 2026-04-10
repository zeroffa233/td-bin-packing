# td-bin-packing

一个基于 **Maven + Java 17** 的三维装箱示例项目，使用“**极点法（Extreme Point）+ 接触面积最大化**”实现贪心装箱。

## 实现内容

- 按物品体积降序排序。
- 为每个物品枚举 `6` 种正交旋转姿态。
- 在极点列表上搜索合法候选位置。
- 使用 AABB 进行边界检查和碰撞检测。
- 以“总接触面积最大”为主目标做贪心选择。
- 接触面积相同情况下，按 `z -> y -> x` 更小优先打破平局。
- 放置后更新极点，并清理重复/落入物品内部的无效极点。
- 输出装箱结果、坐标矩阵行和空间利用率。

## 目录结构

- `src/main/java/com/zeroffa/tdbinpacking/App.java`：示例入口。
- `src/main/java/com/zeroffa/tdbinpacking/solver/ExtremePointBinPacker.java`：核心算法。
- `src/main/java/com/zeroffa/tdbinpacking/model`：领域模型。

## 运行方式

```bash
mvn compile
mvn exec:java
```

运行后会在 `output/packing-visualization.html` 生成三维可视化页面。

## 三维可视化

- 可视化文件：`output/packing-visualization.html`
- 打开方式：浏览器直接打开该 HTML 文件
- 交互操作：鼠标左键旋转、滚轮缩放、右键拖拽平移
- 页面内容：箱体线框 + 每个物品的 3D 方块 + 坐标和尺寸列表 + 本次运行耗时
- 说明：页面使用 Three.js CDN，首次打开需要网络访问 `cdn.jsdelivr.net`

## 批量输入输出

可通过命令行参数执行批量装箱：

```bash
mvn compile
mvn exec:java -Dexec.args="batch input/cases-batch.csv"
```

- 第一个参数：输入 CSV 路径
- 第二个参数：输出目录（可省略，默认 `output/batch-output`）

输入 CSV 格式（支持表头）：

```text
caseId,containerX,containerY,containerZ,itemId,itemX,itemY,itemZ
case-1,10,10,10,A,5,4,3
case-1,10,10,10,B,4,4,4
case-2,12,10,8,P1,3,3,2
```

批量输出内容：

- `output/batch-output/batch-summary.csv`：每个 case 的运行时间、利用率等统计
- `output/batch-output/visualizations/<caseId>.html`：每个 case 的 3D 可视化结果

## 多箱型对比

当你有物料库、箱型库、以及多个装配实例时，可运行实例级箱型推荐：

```bash
mvn compile
mvn exec:java -Dexec.args="box-sweep input/items.csv input/box-types.csv input/cases.csv"
```

- `items.csv` 格式：

```text
itemId,itemX,itemY,itemZ
A,5,4,3
B,4,4,4
```

- `cases.csv` 格式（每行是某实例中某物品的需求数量）：

```text
caseId,itemId,quantity
assembly-001,A,2
assembly-001,B,1
assembly-002,A,1
assembly-002,C,3
```

- `box-types.csv` 格式：

```text
boxTypeId,containerX,containerY,containerZ
S,8,8,8
M,10,10,10
L,12,10,8
```

对比模式会：

- 针对每个实例，遍历所有箱型运行算法
- 在 `output/instance-recommendation/instances/<caseId>/` 下生成该实例所有箱型的 HTML
- 生成总汇总文件 `output/instance-recommendation/summary.csv`
- `summary.csv` 包含每个实例的推荐箱型，以及每个箱型的利用率列

## 算法说明

### 1. 初始化

- 箱体尺寸使用 `ContainerBox(sizeX, sizeY, sizeZ)` 表示。
- 物品使用 `Item(id, sizeX, sizeY, sizeZ)` 表示。
- 初始极点列表仅包含原点 `(0,0,0)`。

### 2. 候选评估

对于当前物品：

- 遍历所有极点。
- 遍历所有旋转姿态。
- 检查是否越界。
- 检查是否与已放置物品碰撞。
- 计算接触面积：
  - 与箱体六个壁面的贴合面积。
  - 与已放置物品表面的贴合面积。

### 3. 贪心选择

- 优先选择接触面积最大的候选。
- 若接触面积相同，则优先更靠底、靠角的极点：`z` 最小，其次 `y`，最后 `x`。

### 4. 极点更新

放入新物品后：

- 删除被使用的极点。
- 基于新物品的三个正向表面，向 `X=0 / Y=0 / Z=0` 方向做投影，生成最多 `6` 个新极点。
- 清理越界、重复、以及落在物品内部的无效极点。

## 示例输出

输出中每一行坐标矩阵采用如下形式：

```text
item=A, origin=[0,0,0], size=[5,4,3]
```

其中：

- `origin=[x,y,z]` 表示物品放置原点。
- `size=[dx,dy,dz]` 表示最终旋转后的三维尺寸。

## 可扩展方向

- 支持从文件或命令行读取箱体和物品数据。
- 为“无法放置当前物品”后的流程增加“跳过当前物品继续尝试”策略。
- 增加单元测试和更复杂的数据集。
- 引入更严格的极点支配关系裁剪，进一步提升性能。
