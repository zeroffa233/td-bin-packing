# Three-Dimension Bin Packing Algorithm

面向企业的启发式 3D 装箱算法，基于极点法（Extreme Point）实现单箱调度，并在核心算法外层设计订单标准化、候选箱筛选、多箱调度、拼箱规则编排与结果组装等业务能力。

![Java](https://img.shields.io/badge/Java-8+-3776AB?style=flat-square)
![Build](https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square)
![Algorithm](https://img.shields.io/badge/Algorithm-Extreme%20Point-0F766E?style=flat-square)

## 项目特性

- 面向订单请求的 3D 装箱接口，而非单纯算法 Demo
- 以 `ExtremePointBinPacker` 作为单箱原子能力，支持多箱循环装箱
- 支持 `stores` 箱型过滤、`sequence + volume` 候选箱优先级排序
- 支持 `csQty` 取余后的散件装箱、`allowDown` 姿态限制
- 支持 `maxWeight` 与 `capacityRate` 的装后合法性校验
- 支持 `groupId` 分组调度、`carrierService` 路径分流、`lclRules` 拼箱编排
- 提供 JSON CLI 入口，便于本地联调、脚本接入与批量验证

## 适用场景

适合需要将装箱算法嵌入企业仓配或订单履约流程的场景，例如：

- 电商订单履约
- 多品牌/多仓箱型推荐
- 快递与物流两类装箱路径分流
- 带业务规则的多箱装载决策

## 核心流程

项目先做标准化，再进入核心装箱逻辑：

1. 解析请求并校验 `items / containers / order(s)`
2. 通过 `itemCode + unitCode` 关联商品主数据
3. 对 `quantity` 执行 `csQty` 取余，只让散件进入调度
4. 按 `groupId` 分组，并依据 `carrierService` 选择装箱路径
5. 过滤并排序候选箱，调用极点法完成单箱求解
6. 进行重量与容量合法性校验，必要时切换箱型或进入下一箱
7. 汇总为接口响应结果

流程图见：

- [总流程图](flowcharts/flowcharts-svg/01-total-flow.svg)
- [输入标准化](flowcharts/flowcharts-svg/02-input-normalization.svg)
- [常规装箱流程](flowcharts/flowcharts-svg/03-normal-packing-flow.svg)
- [拼箱规则流程](flowcharts/flowcharts-svg/04-lcl-rules-flow.svg)
- [物流快捷路径](flowcharts/flowcharts-svg/05-logistic-shortcut-flow.svg)
- [结果输出与校验](flowcharts/flowcharts-svg/06-output-and-validation.svg)

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+

### 编译

```bash
mvn -q -DskipTests compile
```

### 运行单个 JSON 请求

```bash
java -cp target/classes com.zeroffa.tdbinpacking.cli.JsonPackingCli \
  --input example/example_data/input1.json
```

也可以输出到文件：

```bash
java -cp target/classes com.zeroffa.tdbinpacking.cli.JsonPackingCli \
  --input example/example_data/input1.json \
  --output example/output/input1.output.json
```

### 批量运行示例

```bash
./example/run_demo.sh
```

脚本会：

- 编译项目
- 遍历 `example/example_data/*.json`
- 将结果输出到 `example/output/`

## 输入输出约定

### 请求结构

顶层字段：

- `items`：商品主数据
- `containers`：候选箱型
- `order` 或 `orders`：订单明细，当前代码同时兼容两种字段名
- `lclRules`：拼箱规则
- `unpackJudge`：结果判定参数

最关键的字段约束：

- `itemCode + unitCode` 必须能命中唯一商品
- `quantity % csQty` 的余数才会进入装箱算法
- `allowDown=0` 时仅允许有限姿态旋转
- `stores` 会参与箱型可用性过滤
- `sequence` 越小，候选箱优先级越高

最小请求示例：

```json
{
    "items": [
        {
            "code": "G4410201",
            "unitCode": "EA",
            "itemCategory1": "正品",
            "itemCategory2": "非易碎",
            "length": 0.215,
            "width": 0.121,
            "height": 0.067,
            "weight": 0.508,
            "csQty": 8,
            "allowDown": 1
        }
    ],
    "containers": [
        {
            "code": "LRLC0004W",
            "name": "巴欧C4物流箱",
            "length": 0.24,
            "width": 0.15,
            "height": 0.12,
            "capacityRate": 97,
            "sequence": 3,
            "quantity": 999,
            "stores": "巴欧"
        }
    ],
    "order": [
        {
            "shipmentDetailId": 2026041601001,
            "shipmentCode": "TSHD2026041200015736",
            "itemCode": "G4410201",
            "unitCode": "EA",
            "itemName": "货品-G4410201",
            "quantity": 1,
            "stores": "巴欧",
            "carrierService": 0,
            "isTaoX": 0,
            "groupId": 0
        }
    ],
    "lclRules": [
        {
            "sequence": 7,
            "code": "NO_LIMIT",
            "name": "不限制"
        }
    ],
    "unpackJudge": 0
}
```

### 响应结构

返回结果包含：

- `code` / `msg`：执行状态
- `containerCount`：本次请求使用的箱数
- `containerUtilizations`：各箱利用率
- `averageUtilization`：平均利用率
- `data.infos`：逐商品的装箱结果

示例输出：

```json
{
    "code": 0,
    "msg": "OK",
    "containerCount": 1,
    "containerUtilizations": [0.4035],
    "averageUtilization": 0.4035,
    "data": {
        "infos": [
            {
                "shipmentDetailId": 2026041601001,
                "shipmentCode": "TSHD2026041200015736",
                "waveId": null,
                "itemCode": "G4410201",
                "itemName": "货品-G4410201",
                "quantity": 1,
                "batch": null,
                "lot": null,
                "manufactureDate": null,
                "buildCode": "BUILD-BAO-01",
                "zoneCode": "ZONE-BAO-01",
                "stores": "巴欧",
                "containerSequence": 1,
                "containerCode": "LRLC0004W"
            }
        ]
    }
}
```

## 项目结构

```text
.
├── src/main/java/com/zeroffa/tdbinpacking
│   ├── api/        # 请求/响应模型与 JSON 编解码
│   ├── cli/        # CLI 入口
│   ├── model/      # 核心装箱数据模型
│   ├── service/    # 标准化、编排、调度、校验与响应组装
│   ├── solver/     # 极点法 3D 装箱核心算法
│   └── sample/     # 本地样例验证
├── example/        # 示例输入与运行脚本
├── flowcharts/     # DOT 源文件与 SVG 流程图
└── *.md            # 设计、参数、流程与样例文档
```

## 关键模块

- `solver/ExtremePointBinPacker.java`
    - 核心单箱装箱能力
- `service/RequestNormalizer.java`
    - 请求校验、商品关联、散件展开与标准化
- `service/GroupPackingCoordinator.java`
    - `groupId` 维度的总调度入口
- `service/ExpressPackingPlanner.java`
    - 常规快递路径装箱编排
- `service/LogisticShortcutPlanner.java`
    - 物流快捷路径
- `service/LclRuleOrchestrator.java`
    - 拼箱规则主流程

## 文档导航

- [参数列表](参数列表.md)
- [程序流程图说明](流程图.md)
- [程序设计文档](程序设计文档.md)
- [样例集合](样例.md)
- [示例运行说明](example/README.md)

## 当前定位

这是一个偏业务化的 3D 装箱实现，重点不只在几何求解本身，还在于把企业履约规则稳定地包裹在算法外围。如果你希望二次开发，建议优先阅读：

1. `程序设计文档.md`
2. `参数列表.md`
3. `流程图.md`
4. `src/main/java/com/zeroffa/tdbinpacking/service/`
