#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

if [ "$#" -eq 0 ]; then
  echo "Usage: $0 <input.json> [output.json]" >&2
  echo "       $0 --input <input.json> --output <output.json>" >&2
  echo "       $0 --json '<json-string>' [--output <output.json>]" >&2
  echo "       cat input.json | $0 --stdin [--output <output.json>]" >&2
  exit 2
fi

mvn -q -DskipTests compile
java -cp target/classes com.zeroffa.tdbinpacking.cli.JsonPackingCli "$@"
