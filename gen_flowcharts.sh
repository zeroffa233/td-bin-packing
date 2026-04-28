rm -rf output/flowcharts-src output/flowcharts-svg && mkdir -p output/flowcharts-src output/flowcharts-svg && awk '
BEGIN { in_block=0; idx=0; out="" }
/^```dot$/ {
  in_block=1;
  idx++;
  if (idx==1) out="output/flowcharts-src/01-total-flow.dot";
  else if (idx==2) out="output/flowcharts-src/02-input-normalization.dot";
  else if (idx==3) out="output/flowcharts-src/03-normal-packing-flow.dot";
  else if (idx==4) out="output/flowcharts-src/04-lcl-rules-flow.dot";
  else if (idx==5) out="output/flowcharts-src/05-logistic-shortcut-flow.dot";
  else if (idx==6) out="output/flowcharts-src/06-output-and-validation.dot";
  next;
}
/^```$/ {
  if (in_block) {
    in_block=0;
    out="";
  }
  next;
}
in_block && out != "" { print > out }
' 流程图.md && for f in output/flowcharts-src/*.dot; do
  base=$(basename "$f" .dot)
  dot -Tsvg "$f" -o "output/flowcharts-svg/${base}.svg"
done && ls output/flowcharts-svg
