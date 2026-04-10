# --- 配置区：在此处修改你想要的种子 ---
export NEW_SEED="v3"  # 新的随机种子
export OUT_DIR="output/real-orders-experiment-${NEW_SEED}" # 输出目录，避免覆盖旧结果

# 2. 编译并运行 Java 实验
echo "🚀 开始运行实验..."
mvn compile exec:java -Dexec.mainClass="com.zeroffa.tdbinpacking.App" -Dexec.args="real-orders data ${OUT_DIR}"

# 3. 生成统计图表
echo "📊 正在生成图表..."
python3 scripts/plot_order_utilization.py \
    --input "${OUT_DIR}/order-utilization-comparison.csv" \
    --output-dir "${OUT_DIR}/plots"

# 4. 生成总结报告
echo "📝 正在生成报告..."
python3 scripts/generate_experiment_report.py \
    --experiment-dir "${OUT_DIR}" \
    --output "${OUT_DIR}/report.md"

echo "🎉 完成！报告位于: ${OUT_DIR}/report.md"