#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

INPUT_DIR=${1:-input}
OUTPUT_DIR=${2:-output}

if [ ! -d "$INPUT_DIR" ]; then
  echo "Input directory not found: $INPUT_DIR" >&2
  exit 2
fi

mkdir -p "$OUTPUT_DIR"
mvn -q -DskipTests compile

count=0
success_count=0
failed_count=0
for input_path in "$INPUT_DIR"/*.json; do
  if [ ! -e "$input_path" ]; then
    break
  fi
  base_name=$(basename "$input_path" .json)
  output_path="$OUTPUT_DIR/${base_name}.output.json"
  if java -cp target/classes com.zeroffa.tdbinpacking.cli.JsonPackingCli --input "$input_path" --output "$output_path"; then
    echo "$input_path -> $output_path [OK]"
    success_count=$((success_count + 1))
  else
    echo "$input_path -> $output_path [FAILED]"
    failed_count=$((failed_count + 1))
  fi
  count=$((count + 1))
done

if [ "$count" -eq 0 ]; then
  echo "No json files found in: $INPUT_DIR" >&2
  exit 1
fi

echo "Completed $count json request(s). Success: $success_count. Failed: $failed_count. Output directory: $OUTPUT_DIR"
