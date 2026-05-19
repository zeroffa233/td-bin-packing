#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example/example_data"
OUTPUT_DIR="$ROOT_DIR/example/output"

mkdir -p "$OUTPUT_DIR"

cd "$ROOT_DIR"

mvn -q -DskipTests compile

for input_file in "$EXAMPLE_DIR"/*.json; do
  filename="$(basename "$input_file")"
  output_file="$OUTPUT_DIR/${filename%.json}.output.json"
  java -cp target/classes com.zeroffa.tdbinpacking.cli.JsonPackingCli \
    --input "$input_file" \
    --output "$output_file"
  echo "generated: $output_file"
done
