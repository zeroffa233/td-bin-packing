#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
JAR_PATH="$SCRIPT_DIR/td-bin-packing.jar"
DEMO_OUTPUT_DIR="$SCRIPT_DIR/output/demo-showcase"

if [ "$#" -eq 0 ]; then
  rm -rf "$DEMO_OUTPUT_DIR"
  mkdir -p "$DEMO_OUTPUT_DIR"

  echo "== 示例 1：内置单例演示 =="
  (
    cd "$DEMO_OUTPUT_DIR"
    java -jar "$JAR_PATH"
  )
  echo "单例 HTML: $DEMO_OUTPUT_DIR/output/packing-visualization.html"
  echo

  echo "== 示例 2：batch 批量装箱 =="
  java -jar "$JAR_PATH" batch \
    "$SCRIPT_DIR/examples/batch/cases-batch.csv" \
    "$DEMO_OUTPUT_DIR/batch"
  echo "batch 汇总: $DEMO_OUTPUT_DIR/batch/batch-summary.csv"
  echo "batch HTML 目录: $DEMO_OUTPUT_DIR/batch/visualizations"
  echo

  echo "== 示例 3：box-sweep 箱型推荐 =="
  java -jar "$JAR_PATH" box-sweep \
    "$SCRIPT_DIR/examples/box-sweep/items.csv" \
    "$SCRIPT_DIR/examples/box-sweep/box-types.csv" \
    "$SCRIPT_DIR/examples/box-sweep/cases.csv" \
    "$DEMO_OUTPUT_DIR/recommendation"
  echo "推荐汇总: $DEMO_OUTPUT_DIR/recommendation/summary.csv"
  echo "推荐 HTML 目录: $DEMO_OUTPUT_DIR/recommendation/instances"
  echo

  echo "演示完成。可直接用浏览器打开上面的 HTML 文件查看效果。"
  exit 0
fi

if [ "$1" = "single" ]; then
  shift
  exec java -jar "$JAR_PATH" "$@"
fi

exec java -jar "$JAR_PATH" "$@"
