package pages;

import io.javelit.components.media.FileUploaderComponent;
import io.javelit.core.Jt;
import io.javelit.core.JtContainer;
import io.javelit.core.JtUploadedFile;
import model.BenchmarkEntry;
import service.BenchmarkAggregator;
import service.BenchmarkJsonParser;
import util.FormattingUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Upload page for llama-bench JSON files.
 */
public class UploadPage {

    private static BenchmarkJsonParser parser;
    private static BenchmarkAggregator aggregator;

    private static synchronized void ensureInitialized() {
        if (parser == null) {
            parser = new BenchmarkJsonParser();
        }
        if (aggregator == null) {
            aggregator = new BenchmarkAggregator();
        }
    }

    public static void app() {
        ensureInitialized();
        Jt.title("llama-bench Performance Analyzer");

        Jt.markdown("""
                Upload one or more llama-bench JSON files to analyze benchmark performance.
                The app will visualize throughput across different context depths and models.
                """);

        // File uploader
        var uploadedFiles = Jt.fileUploader("Upload llama-bench JSON files")
                .type(Arrays.asList(".json"))
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.TRUE)
                .help("Upload one or more llama-bench JSON files")
                .use();

        // Process uploads if any
        List<String> errors = new ArrayList<>();
        Set<String> uploadedFileNames = new HashSet<>();

        var state = Jt.sessionState();

        @SuppressWarnings("unchecked")
        List<BenchmarkEntry> existingBenchmarks = (List<BenchmarkEntry>) state.get("benchmarks");
        if (existingBenchmarks == null) {
            existingBenchmarks = new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        List<String> existingFileNames = (List<String>) state.get("uploadedFileNames");
        if (existingFileNames == null) {
            existingFileNames = new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        List<String> existingErrors = (List<String>) state.get("uploadErrors");
        if (existingErrors == null) {
            existingErrors = new ArrayList<>();
        }

        // Process newly uploaded files
        if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
            for (JtUploadedFile file : uploadedFiles) {
                String fileName = file.filename();
                try {
                    List<BenchmarkEntry> newEntries = parser.parseJson(new String(file.content()), fileName);
                    existingBenchmarks.addAll(newEntries);
                    if (!existingFileNames.contains(fileName)) {
                        existingFileNames.add(fileName);
                    }
                } catch (Exception e) {
                    String errorMsg = "Error parsing " + fileName + ": " + e.getMessage();
                    errors.add(errorMsg);
                    existingErrors.add(errorMsg);
                }
            }

            // Update session state
            state.put("benchmarks", existingBenchmarks);
            state.put("uploadedFileNames", existingFileNames);
            state.put("uploadErrors", existingErrors);
        }

        // Show upload errors
        if (!errors.isEmpty()) {
            for (String err : errors) {
                Jt.error(err);
            }
        } else if (!existingErrors.isEmpty()) {
            for (String err : existingErrors) {
                Jt.warning("Previous error: " + err);
            }
        }

        // Show overview metrics
        List<BenchmarkEntry> benchmarks = existingBenchmarks;

        if (benchmarks == null || benchmarks.isEmpty()) {
            Jt.warning("No benchmark data uploaded yet. Upload JSON files to get started.");
            return;
        }

        int distinctModels = aggregator.countDistinctModels(benchmarks);
        int totalRows = benchmarks.size();
        int distinctHardware = aggregator.countDistinctHardwareConfigs(benchmarks);

        Jt.markdown("### Overview");

        var cols = Jt.columns(3).use();
        Jt.text("**" + distinctModels + "**").use(cols.col(0));
        Jt.text("Distinct Models").use(cols.col(0));
        Jt.text("**" + totalRows + "**").use(cols.col(1));
        Jt.text("Total Benchmark Rows").use(cols.col(1));
        Jt.text("**" + distinctHardware + "**").use(cols.col(2));
        Jt.text("Hardware Configs").use(cols.col(2));

        // Hardware/Build summary
        Jt.markdown("### Hardware & Build Summary");

        var cpuInfo = benchmarks.get(0).getCpuInfo();
        var gpuInfo = benchmarks.get(0).getGpuInfo();
        var backends = benchmarks.get(0).getBackends();
        var buildCommit = benchmarks.get(0).getBuildCommit();
        var buildNumber = benchmarks.get(0).getBuildNumber();

        var expanderCpu = Jt.expander("CPU Information").use();
        Jt.text(cpuInfo).use(expanderCpu);

        var expanderGpu = Jt.expander("GPU Information").use();
        Jt.text(gpuInfo).use(expanderGpu);

        var expanderBackend = Jt.expander("Backend").use();
        Jt.text(backends).use(expanderBackend);

        var expanderCommit = Jt.expander("Build Commit").use();
        Jt.text(buildCommit).use(expanderCommit);

        var expanderNumber = Jt.expander("Build Number").use();
        Jt.text(String.valueOf(buildNumber)).use(expanderNumber);

        List<String> sourceFiles = aggregator.getUniqueSourceFiles(benchmarks);
        if (!sourceFiles.isEmpty()) {
            var expanderFiles = Jt.expander("Uploaded Files").use();
            Jt.text(String.join(", ", sourceFiles)).use(expanderFiles);
        }

        // Tabs for data view - use key to track selection via session state
        String tabsKey = "uploadPageTabs";
        var tabs = Jt.tabs(Arrays.asList("Raw Data", "Summary Table")).key(tabsKey).use();

        // Get selected tab from session state (default to first tab)
        @SuppressWarnings("unchecked")
        String selectedTab = (String) state.get(tabsKey);
        if (selectedTab == null || selectedTab.isEmpty()) {
            selectedTab = "Raw Data";
        }

        if ("Raw Data".equals(selectedTab)) {
            renderRawDataTable(benchmarks);
        } else {
            renderSummaryTable(benchmarks);
        }

        // Link to explorer page
        Jt.markdown("### Next Steps");
        Jt.pageLink("/explorer").use();
    }

    private static void renderRawDataTable(List<BenchmarkEntry> benchmarks) {
        List<TableRow> rows = benchmarks.stream()
                .map(b -> new TableRow(
                        FormattingUtils.truncateFilename(b.getSourceFile(), 30),
                        b.getModelType(),
                        b.getTestType(),
                        b.getnDepth(),
                        FormattingUtils.formatThroughput(b.getAvgTs()),
                        FormattingUtils.formatThroughput(b.getStddevTs()),
                        b.getnBatch(),
                        b.getnUbatch(),
                        FormattingUtils.formatBoolean(b.isFlashAttn()),
                        b.getTypeK(),
                        b.getTypeV()
                ))
                .collect(Collectors.toList());

        Jt.table(rows).use();
    }

    private static void renderSummaryTable(List<BenchmarkEntry> benchmarks) {
        // Group by model, test type, and depth
        Map<String, List<BenchmarkEntry>> grouped = benchmarks.stream()
                .filter(b -> !"UNKNOWN".equals(b.getTestType()))
                .collect(Collectors.groupingBy(
                        b -> b.getModelType() + " | " + b.getTestType() + " | d=" + b.getnDepth()
                ));

        List<SummaryRow> rows = new ArrayList<>();
        for (var e : grouped.entrySet()) {
            List<BenchmarkEntry> group = e.getValue();
            String[] parts = e.getKey().split(" \\| ");
            String modelType = parts[0];
            String testType = parts[1];
            int depth = Integer.parseInt(parts[2].substring(4));

            double avgThroughput = group.stream()
                    .mapToDouble(BenchmarkEntry::getAvgTs)
                    .average()
                    .orElse(0.0);

            rows.add(new SummaryRow(modelType, testType, depth, FormattingUtils.formatThroughput(avgThroughput)));
        }

        // Sort by model, test type, then depth
        rows.sort(Comparator.comparing((SummaryRow r) -> r.modelType())
                .thenComparing(r -> r.testType())
                .thenComparingInt(r -> r.depth()));

        Jt.table(rows).use();
    }

    // Record classes for table rows
    private record TableRow(String file, String modelType, String testType, int depth,
                            String avgTs, String stddevTs, int nBatch, int nUbatch,
                            String flashAttn, String typeK, String typeV) {}

    private record SummaryRow(String modelType, String testType, int depth, String avgThroughput) {}
}
