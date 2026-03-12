package service;

import model.BenchmarkEntry;
import org.icepear.echarts.Line;
import org.icepear.echarts.charts.line.LineSeries;
import org.icepear.echarts.components.coord.cartesian.CategoryAxis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory for creating ECharts charts for benchmark visualization.
 */
public class ChartFactory {

    /**
     * Creates a line chart showing throughput vs depth for the specified test type.
     * @param entries the benchmark entries to plot
     * @param testType "PP" or "TG"
     * @return Line instance ready to be rendered
     */
    public Line createThroughputVsDepthChart(List<BenchmarkEntry> entries, String testType) {
        // Group by model type
        Map<String, List<BenchmarkEntry>> byModel = entries.stream()
                .filter(e -> testType.equals(e.getTestType()))
                .collect(Collectors.groupingBy(BenchmarkEntry::getModelType));

        if (byModel.isEmpty()) {
            return createEmptyChart("No data available for " + testType);
        }

        Line lineChart = new Line()
                .setTooltip("item")
                .setLegend();

        // Collect all unique depths across all models
        Set<Integer> allDepthsSet = new HashSet<>();
        for (List<BenchmarkEntry> modelEntries : byModel.values()) {
            for (BenchmarkEntry e : modelEntries) {
                allDepthsSet.add(e.getnDepth());
            }
        }
        List<String> allDepths = allDepthsSet.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.toList());

        // Add series for each model
        for (Map.Entry<String, List<BenchmarkEntry>> e : byModel.entrySet()) {
            String modelType = e.getKey();
            List<BenchmarkEntry> modelEntries = e.getValue();

            // Sort by depth ascending
            modelEntries.sort(Comparator.comparingInt(BenchmarkEntry::getnDepth));

            // Build data array aligned with allDepths
            Map<String, Double> depthToThroughput = new HashMap<>();
            for (BenchmarkEntry be : modelEntries) {
                depthToThroughput.put(String.valueOf(be.getnDepth()), be.getAvgTs());
            }

            List<Double> dataValues = allDepths.stream()
                    .map(d -> depthToThroughput.getOrDefault(d, 0.0))
                    .collect(Collectors.toList());

            lineChart.addXAxis(new CategoryAxis().setData(allDepths.toArray(new String[0])))
                    .addYAxis()
                    .addSeries(new LineSeries()
                            .setName(modelType)
                            .setData(dataValues.stream().map(Double::doubleValue).toArray(Number[]::new)));
        }

        return lineChart;
    }

    /**
     * Creates a comparison chart showing both PP and TG for selected models.
     * @param entries the benchmark entries to plot
     * @return Line instance ready to be rendered
     */
    public Line createComparisonChart(List<BenchmarkEntry> entries) {
        // Group by model type, then separate PP and TG
        Map<String, List<BenchmarkEntry>> byModel = entries.stream()
                .filter(e -> !"UNKNOWN".equals(e.getTestType()))
                .collect(Collectors.groupingBy(BenchmarkEntry::getModelType));

        if (byModel.isEmpty()) {
            return createEmptyChart("No data available for comparison");
        }

        Line lineChart = new Line()
                .setTooltip("item")
                .setLegend();

        // Collect all unique depths across all models and test types
        Set<Integer> allDepthsSet = new HashSet<>();
        for (List<BenchmarkEntry> modelEntries : byModel.values()) {
            for (BenchmarkEntry e : modelEntries) {
                allDepthsSet.add(e.getnDepth());
            }
        }
        List<String> allDepths = allDepthsSet.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.toList());

        // Add series for PP and TG separately
        for (Map.Entry<String, List<BenchmarkEntry>> e : byModel.entrySet()) {
            String modelType = e.getKey();
            List<BenchmarkEntry> modelEntries = e.getValue();

            Map<String, Double> ppData = new HashMap<>();
            Map<String, Double> tgData = new HashMap<>();

            for (BenchmarkEntry be : modelEntries) {
                if ("PP".equals(be.getTestType())) {
                    ppData.put(String.valueOf(be.getnDepth()), be.getAvgTs());
                } else if ("TG".equals(be.getTestType())) {
                    tgData.put(String.valueOf(be.getnDepth()), be.getAvgTs());
                }
            }

            if (!ppData.isEmpty()) {
                List<Double> ppValues = allDepths.stream()
                        .map(d -> ppData.getOrDefault(d, 0.0))
                        .collect(Collectors.toList());

                lineChart.addXAxis(new CategoryAxis().setData(allDepths.toArray(new String[0])))
                        .addYAxis()
                        .addSeries(new LineSeries()
                                .setName(modelType + " (PP)")
                                .setData(ppValues.stream().map(Double::doubleValue).toArray(Number[]::new)));
            }

            if (!tgData.isEmpty()) {
                List<Double> tgValues = allDepths.stream()
                        .map(d -> tgData.getOrDefault(d, 0.0))
                        .collect(Collectors.toList());

                lineChart.addXAxis(new CategoryAxis().setData(allDepths.toArray(new String[0])))
                        .addYAxis()
                        .addSeries(new LineSeries()
                                .setName(modelType + " (TG)")
                                .setData(tgValues.stream().map(Double::doubleValue).toArray(Number[]::new)));
            }
        }

        return lineChart;
    }

    private Line createEmptyChart(String message) {
        // Return an empty chart with just the message as a series name
        Line line = new Line();
        line.addXAxis(new String[]{""});
        line.addYAxis();
        line.addSeries(new LineSeries().setName(message).setData(new Number[0]));
        return line;
    }
}
