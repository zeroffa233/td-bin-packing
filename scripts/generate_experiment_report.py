#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import statistics
from collections import Counter
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ResultRow:
    order_id: str
    actual_box_id: str
    actual_utilization: float
    algorithm_box_id: str
    algorithm_utilization: float
    status: str

    @property
    def ratio(self) -> float | None:
        if self.actual_utilization <= 0:
            return None
        return self.algorithm_utilization / self.actual_utilization


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="为真实订单实验生成 Markdown 中文报告")
    parser.add_argument(
        "--experiment-dir",
        type=Path,
        default=Path("output/real-orders-experiment"),
        help="Experiment output directory",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("output/real-orders-experiment/report.md"),
        help="Markdown report output path",
    )
    parser.add_argument(
        "--sample-size",
        type=int,
        default=1000,
        help="Order sample size used in this experiment",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=20260321,
        help="Random seed used in this experiment",
    )
    return parser.parse_args()


def load_results(csv_path: Path) -> list[ResultRow]:
    rows: list[ResultRow] = []
    with csv_path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        for raw in reader:
            rows.append(
                ResultRow(
                    order_id=raw["orderId"],
                    actual_box_id=raw["actualBoxId"],
                    actual_utilization=float(raw["actualUtilization"]),
                    algorithm_box_id=raw["algorithmBoxId"],
                    algorithm_utilization=float(raw["algorithmUtilization"]),
                    status=raw["status"],
                )
            )
    return rows


def count_csv_rows(csv_path: Path) -> int:
    with csv_path.open("r", encoding="utf-8", newline="") as handle:
        return max(sum(1 for _ in handle) - 1, 0)


def format_float(value: float) -> str:
    return f"{value:.6f}"


def relative(path: Path, root: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def build_report(experiment_dir: Path, output_path: Path, sample_size: int, seed: int) -> str:
    csv_path = experiment_dir / "order-utilization-comparison.csv"
    cleaned_item_path = experiment_dir / "cleaned-data" / "item.cleaned.csv"
    cleaned_box_path = experiment_dir / "cleaned-data" / "box.cleaned.csv"
    plots_dir = experiment_dir / "plots"
    sample_dir = experiment_dir / "sample-orders"

    rows = load_results(csv_path)
    ok_rows = [row for row in rows if row.status == "OK"]
    status_counter = Counter(row.status for row in rows)

    actual_values = [row.actual_utilization for row in ok_rows]
    algorithm_values = [row.algorithm_utilization for row in ok_rows]
    ratio_values = [row.ratio for row in ok_rows if row.ratio is not None]
    improved_count = sum(1 for row in ok_rows if row.algorithm_utilization > row.actual_utilization)
    worsened_count = sum(1 for row in ok_rows if row.algorithm_utilization < row.actual_utilization)
    equal_count = len(ok_rows) - improved_count - worsened_count

    sample_order_dirs = sorted(path for path in sample_dir.iterdir() if path.is_dir()) if sample_dir.exists() else []
    sample_html_files = sorted(path for path in sample_dir.rglob("*.html")) if sample_dir.exists() else []
    plot_files = sorted(path for path in plots_dir.glob("*.png")) if plots_dir.exists() else []

    root = output_path.parent
    lines: list[str] = []
    lines.append("# 真实订单实验报告")
    lines.append("")
    lines.append("## 1. 实验概览")
    lines.append("")
    lines.append(f"- 实验输出目录：`{experiment_dir}`")
    lines.append(f"- 结果 CSV：`{relative(csv_path, root)}`")
    lines.append(f"- 清洗后的物品目录：`{relative(cleaned_item_path, root)}`")
    lines.append(f"- 清洗后的箱型目录：`{relative(cleaned_box_path, root)}`")
    lines.append(f"- 图表目录：`{relative(plots_dir, root)}`")
    lines.append(f"- 样例 HTML 目录：`{relative(sample_dir, root)}`")
    lines.append("")
    lines.append("## 2. 实验设置")
    lines.append("")
    lines.append("- 数据来源：`data/orders.csv`、`data/item.csv`、`data/box.csv`")
    lines.append(f"- 订单抽样规模：`{sample_size}`")
    lines.append(f"- 订单抽样方式：固定随机种子 `{seed}` 的蓄水池抽样")
    lines.append("- HTML 样例导出规模：`10` 个有效订单")
    lines.append("- 物品尺寸缩放规则：原始尺寸统一乘 `10000`，再四舍五入取整")
    lines.append("- 箱型尺寸清洗规则：")
    lines.append("  - 删除长、宽、高任一为 `0` 的行")
    lines.append("  - 若 `length < 1` 或 `width < 1`，则视为单位为米，先整体乘 `100` 转为厘米")
    lines.append("  - 之后所有箱型尺寸再统一乘 `100` 并取整")
    lines.append("- 预处理逻辑：")
    lines.append("  - 若订单中包含属性为 `bag` 的商品（通常为编织袋、手提袋），这些商品会被直接移除，不参与装箱计算")
    lines.append("  - 若订单中包含属性为 `box` 的商品（礼盒），且数量超过 `1` 个，则视为复杂结构跳过该订单")
    lines.append("  - 若订单中仅含 `1` 个 `box` 商品，尝试将其作为内箱，优先装入其他普通商品")
    lines.append("- 算法选箱规则：按清洗后箱体积从小到大遍历，选择第一个“所有物品（剔除 `bag` 后）均可放入且装箱结果可行”的箱型")
    lines.append("")
    lines.append("## 3. CSV 字段说明")
    lines.append("")
    lines.append("| 字段 | 含义 |")
    lines.append("|---|---|")
    lines.append("| `orderId` | `orders.csv` 中的订单标识 |")
    lines.append("| `actualBoxId` | 生产数据中实际使用的箱型编号 |")
    lines.append("| `actualUtilization` | `物品总体积 / 实际箱体积` |")
    lines.append("| `algorithmBoxId` | 装箱算法选择的箱型编号 |")
    lines.append("| `algorithmUtilization` | `物品总体积 / 算法所选箱体积` |")
    lines.append("| `status` | 处理状态，当前正常行为 `OK` |")
    lines.append("")
    lines.append("## 4. 实验处理流程")
    lines.append("")
    lines.append("1. 按上述规则清洗 `item.csv` 和 `box.csv`，并将尺寸统一转换到整数空间。")
    lines.append("2. 读取 `orders.csv`，按相邻且相同的 `order-id` 聚合成订单块。")
    lines.append("3. 使用固定随机种子，从全部订单块中抽样 `1000` 个订单。")
    lines.append("4. 对每个抽样订单汇总物品数量，并基于真实箱型计算实际利用率。")
    lines.append("5. 按候选箱型体积从小到大遍历；对每个候选箱运行 3D 装箱算法，直到找到第一个可行箱。")
    lines.append("6. 将实际利用率、算法利用率、算法所选箱型和处理状态写入结果 CSV。")
    lines.append("7. 再从有效订单中随机抽取 `10` 个，分别导出“实际箱装载结果”和“算法箱装载结果”的 HTML。")
    lines.append("")
    lines.append("## 5. 关键指标")
    lines.append("")
    lines.append(f"- 结果 CSV 总行数：`{len(rows)}`")
    lines.append(f"- 有效 `OK` 行数：`{len(ok_rows)}`")
    lines.append(f"- 清洗后物品数：`{count_csv_rows(cleaned_item_path)}`")
    lines.append(f"- 清洗后箱型数：`{count_csv_rows(cleaned_box_path)}`")
    lines.append(f"- 实际利用率平均值：`{format_float(statistics.fmean(actual_values))}`")
    lines.append(f"- 算法利用率平均值：`{format_float(statistics.fmean(algorithm_values))}`")
    lines.append(f"- 比值平均值（`algorithm / actual`）：`{format_float(statistics.fmean(ratio_values))}`")
    lines.append(f"- 比值中位数（`algorithm / actual`）：`{format_float(statistics.median(ratio_values))}`")
    lines.append(f"- 算法优于实际的订单数：`{improved_count}`（占比 `{format_float(improved_count / len(ok_rows))}`）")
    lines.append(f"- 算法劣于实际的订单数：`{worsened_count}`（占比 `{format_float(worsened_count / len(ok_rows))}`）")
    lines.append(f"- 两者相同的订单数：`{equal_count}`（占比 `{format_float(equal_count / len(ok_rows))}`）")
    lines.append("")
    lines.append("### 状态分布")
    lines.append("")
    lines.append("| 状态 | 数量 |")
    lines.append("|---|---:|")
    for status, count in sorted(status_counter.items()):
        lines.append(f"| `{status}` | {count} |")
    lines.append("")
    lines.append("## 6. 输出文件结构")
    lines.append("")
    lines.append("```text")
    lines.append(f"{experiment_dir.name}/")
    lines.append("├── cleaned-data/")
    lines.append("│   ├── box.cleaned.csv")
    lines.append("│   └── item.cleaned.csv")
    lines.append("├── order-utilization-comparison.csv")
    lines.append("├── plots/")
    for path in plot_files:
        lines.append(f"│   ├── {path.name}")
    lines.append("├── report.md")
    lines.append("└── sample-orders/")
    for order_dir in sample_order_dirs[:3]:
        lines.append(f"    ├── {order_dir.name}/")
        htmls = sorted(path.name for path in order_dir.glob("*.html"))
        for html_name in htmls:
            lines.append(f"    │   ├── {html_name}")
    if len(sample_order_dirs) > 3:
        lines.append(f"    └── ...（共 `{len(sample_order_dirs)}` 个样例订单文件夹）")
    lines.append("```")
    lines.append("")
    lines.append("## 7. 图表文件")
    lines.append("")
    for path in plot_files:
        lines.append(f"- `{relative(path, root)}`")
    lines.append("")
    lines.append("## 8. 结果解读")
    lines.append("")
    lines.append(f"- 在当前样本中，算法利用率平均值为 `{format_float(statistics.fmean(algorithm_values))}`，高于实际箱利用率平均值 `{format_float(statistics.fmean(actual_values))}`。")
    lines.append(f"- 利用率比值 `algorithm / actual` 的平均值为 `{format_float(statistics.fmean(ratio_values))}`，说明算法所选箱型在平均意义上达到实际利用率的 `{format_float(statistics.fmean(ratio_values) * 100)}%`。")
    lines.append(f"- 在 `{len(ok_rows)}` 个有效订单中，有 `{improved_count}` 个订单的算法利用率高于实际利用率。")
    lines.append("- 如需逐单分析，可结合结果 CSV 与样例订单 HTML 可视化进一步查看。")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    args = parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    report = build_report(args.experiment_dir, args.output, args.sample_size, args.seed)
    args.output.write_text(report, encoding="utf-8")
    print(f"报告已写入：{args.output.resolve()}")


if __name__ == "__main__":
    main()
