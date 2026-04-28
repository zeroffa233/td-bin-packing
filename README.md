# td-bin-packing

Java 8 compatible 3D bin-packing service for the current JSON interface.

The current executable flow is:

1. Read one packing request as JSON.
2. Normalize request dimensions and order lines.
3. Split items by `groupId`.
4. Run the business packing flow for each group.
5. Return the interface response JSON with selected container codes.

## Main Code Layout

- `src/main/java/com/zeroffa/tdbinpacking/api`: request and response DTOs.
- `src/main/java/com/zeroffa/tdbinpacking/api/PackingApi.java`: program boundary API for JSON-in/JSON-out integration.
- `src/main/java/com/zeroffa/tdbinpacking/cli`: JSON command line entry and parser.
- `src/main/java/com/zeroffa/tdbinpacking/service`: request normalization, grouping, LCL rules, legality checks, and response assembly.
- `src/main/java/com/zeroffa/tdbinpacking/solver/ExtremePointBinPacker.java`: reusable extreme-point packing algorithm.
- `src/main/java/com/zeroffa/tdbinpacking/model`: core geometry and packing model.
- `src/main/java/com/zeroffa/tdbinpacking/sample/SampleValidationRunner.java`: local sample validation runner.

## Build

```bash
mvn -q -DskipTests compile
```

The project is compiled with `maven.compiler.release=8`.

## Run With JSON

Use the helper script:

```bash
./scripts/pack-json.sh input/input1.json output/input1.output.json
./scripts/pack-json.sh --input input/input1.json --output output/input1.output.json
```

Pass JSON directly:

```bash
./scripts/pack-json.sh --json '{"items":[],"containers":[],"orders":[],"lclRules":[],"unpackJudge":0}' --output output/direct.output.json
```

Read JSON from standard input:

```bash
cat input/input1.json | ./scripts/pack-json.sh --stdin --output output/stdin.output.json
```

Run all JSON files from one directory:

```bash
./scripts/pack-json-batch.sh input output
```

Batch mode writes one response per input file, named `<input-name>.output.json`. Requests whose response `code` is not `0` are marked as `[FAILED]` in the console summary, but their response JSON is still written to the output directory.

## Embed As API

For integration into another Java project, call the facade directly:

```java
import com.zeroffa.tdbinpacking.api.PackingApi;

String outputJson = new PackingApi().packJson(inputJson);
```

`PackingApi` is the program boundary API. CLI scripts are only wrappers for local validation and batch sample execution.

You can also run the CLI class directly after compilation:

```bash
java -cp target/classes com.zeroffa.tdbinpacking.cli.JsonPackingCli --input input/input1.json --output output/input1.output.json
```

## Input Notes

- The CLI accepts `orders`, matching the sample document.
- The CLI also accepts `order`, matching the internal DTO name.
- Dimensions are decimal values from the interface and are scaled internally by `10000`.
- `capacityRate` supports both ratio style values such as `0.97` and percent style values such as `97`.

## Sample Validation

Run the hardcoded sample-one validation:

```bash
mvn -q -DskipTests compile
java -cp target/classes com.zeroffa.tdbinpacking.sample.SampleValidationRunner
```

The exact packing result may differ from the reference output, but the response format and successful container assignment should be valid.
