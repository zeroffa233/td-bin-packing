#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import math
from dataclasses import dataclass
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


@dataclass(frozen=True)
class OrderUtilization:
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
    parser = argparse.ArgumentParser(
        description="Plot actual vs algorithm utilization charts from order-utilization-comparison.csv"
    )
    parser.add_argument(
        "--input",
        type=Path,
        default=Path("output/real-orders-experiment/order-utilization-comparison.csv"),
        help="Input CSV path",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("output/real-orders-experiment/plots"),
        help="Directory for generated charts",
    )
    return parser.parse_args()


def load_rows(csv_path: Path) -> list[OrderUtilization]:
    rows: list[OrderUtilization] = []
    with csv_path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        for raw in reader:
            if raw["status"] != "OK":
                continue
            rows.append(
                OrderUtilization(
                    order_id=raw["orderId"],
                    actual_box_id=raw["actualBoxId"],
                    actual_utilization=float(raw["actualUtilization"]),
                    algorithm_box_id=raw["algorithmBoxId"],
                    algorithm_utilization=float(raw["algorithmUtilization"]),
                    status=raw["status"],
                )
            )
    rows.sort(key=lambda row: (row.actual_utilization, row.algorithm_utilization, row.order_id))
    return rows


def configure_style() -> None:
    plt.style.use("seaborn-v0_8-whitegrid")
    plt.rcParams.update(
        {
            "figure.figsize": (14, 7),
            "figure.dpi": 160,
            "axes.titlesize": 15,
            "axes.labelsize": 12,
            "legend.fontsize": 11,
            "xtick.labelsize": 9,
            "ytick.labelsize": 10,
        }
    )


def plot_utilization_lines(rows: list[OrderUtilization], output_path: Path) -> None:
    x = list(range(1, len(rows) + 1))
    actual = [row.actual_utilization for row in rows]
    algorithm = [row.algorithm_utilization for row in rows]

    fig, ax = plt.subplots()
    ax.plot(x, actual, color="#4C78A8", linewidth=1.8, label="Actual utilization")
    ax.plot(x, algorithm, color="#F58518", linewidth=1.8, label="Algorithm utilization")
    ax.set_title("Utilization Comparison by Order (sorted by actual utilization)")
    ax.set_xlabel("Order rank")
    ax.set_ylabel("Utilization")
    ax.legend(loc="upper left")
    ax.set_xlim(1, len(rows))
    fig.tight_layout()
    fig.savefig(output_path, bbox_inches="tight")
    plt.close(fig)


def plot_normalized_lines(rows: list[OrderUtilization], output_path: Path) -> None:
    normalized_actual = [1.0 for _ in rows]
    normalized_algorithm = [row.ratio if row.ratio is not None else math.nan for row in rows]
    x = list(range(1, len(rows) + 1))

    fig, ax = plt.subplots()
    ax.plot(x, normalized_actual, color="#4C78A8", linewidth=1.8, linestyle="--", label="Actual / Actual = 1")
    ax.plot(x, normalized_algorithm, color="#E45756", linewidth=1.8, label="Algorithm / Actual")
    ax.set_title("Normalized Utilization by Order (normalized by actual utilization)")
    ax.set_xlabel("Order rank")
    ax.set_ylabel("Normalized utilization")
    ax.legend(loc="upper left")
    ax.set_xlim(1, len(rows))
    fig.tight_layout()
    fig.savefig(output_path, bbox_inches="tight")
    plt.close(fig)


def plot_ratio_box(rows: list[OrderUtilization], output_path: Path) -> None:
    ratios = [row.ratio for row in rows if row.ratio is not None]

    fig, ax = plt.subplots(figsize=(8, 7), dpi=160)
    ax.boxplot(
        ratios,
        vert=True,
        patch_artist=True,
        boxprops={"facecolor": "#72B7B2", "alpha": 0.75},
        medianprops={"color": "#222222", "linewidth": 2},
        whiskerprops={"color": "#444444"},
        capprops={"color": "#444444"},
        flierprops={"marker": "o", "markersize": 3, "markerfacecolor": "#E45756", "alpha": 0.45},
    )
    ax.axhline(1.0, color="#E45756", linestyle="--", linewidth=1.4, label="Parity line = 1")
    ax.set_title("Distribution of Algorithm / Actual Utilization")
    ax.set_ylabel("Algorithm / Actual")
    ax.set_xticks([1])
    ax.set_xticklabels(["Orders"])
    ax.legend(loc="upper right")
    fig.tight_layout()
    fig.savefig(output_path, bbox_inches="tight")
    plt.close(fig)


def main() -> None:
    args = parse_args()
    configure_style()

    rows = load_rows(args.input)
    if not rows:
        raise SystemExit(f"No usable rows found in {args.input}")

    args.output_dir.mkdir(parents=True, exist_ok=True)
    plot_utilization_lines(rows, args.output_dir / "line-utilization-comparison.png")
    plot_normalized_lines(rows, args.output_dir / "line-normalized-by-actual.png")
    plot_ratio_box(rows, args.output_dir / "box-algorithm-actual-ratio.png")

    ratios = [row.ratio for row in rows if row.ratio is not None]
    print(f"Loaded OK orders: {len(rows)}")
    print(f"Output directory: {args.output_dir.resolve()}")
    print(f"Ratio mean: {sum(ratios) / len(ratios):.6f}")
    print(f"Ratio median estimate source count: {len(ratios)}")


if __name__ == "__main__":
    main()
