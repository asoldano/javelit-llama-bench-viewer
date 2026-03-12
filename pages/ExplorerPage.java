package pages;

import io.javelit.core.Jt;
import model.BenchmarkEntry;
import service.ChartFactory;
import util.FormattingUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Performance Explorer page with charts and filtering.
 */
public class ExplorerPage {

    public static void app() {
        Jt.title("Performance Explorer");

        // LOGGING: inizio esecuzione pagina explorer
        Jt.markdown("---");
        Jt.markdown("**EXPLORER PAGE DEBUG**");
        Jt.markdown("---");

        var state = Jt.sessionState();

        Jt.markdown(String.format("**[LOG]** Session ID: %s", System.identityHashCode(state)));
        Jt.markdown(String.format("**[LOG]** Session state keys: %s", state.keySet()));

        @SuppressWarnings("unchecked")
        List<BenchmarkEntry> benchmarks = (List<BenchmarkEntry>) state.get("benchmarks");

        System.out.println("benchmarks: " + benchmarks);
        if (benchmarks == null) {
            Jt.error("**[LOG]** benchmarks = NULL (non presente nella session state)");
            Jt.warning("No benchmark data available. Please upload files on the [Upload page](_next:/upload).");

            // Debug: mostra cosa c'è effettivamente nella sessione
            Jt.markdown("**Session State Contents:**");
            for (String key : state.keySet()) {
                Object val = state.get(key);
                Jt.markdown(String.format("- `%s` -> %s (type: %s)", key, val == null ? "null" : val.toString().substring(0, Math.min(50, val.toString().length())), val != null ? val.getClass().getSimpleName() : "null"));
            }
            return;
        }

        Jt.markdown(String.format("**[LOG]** benchmarks = %s entries (tipo: %s)", benchmarks.size(), benchmarks.getClass().getSimpleName()));

        if (benchmarks.isEmpty()) {
            Jt.error("[LOG] benchmarks è una lista vuota");
            Jt.warning("Benchmark list is empty. Upload files on the [Upload page](_next:/upload).");
            return;
        }

        // Mostra info sui primi benchmark
        Jt.markdown("**[LOG]** Primo benchmark entry:");
        BenchmarkEntry first = benchmarks.get(0);
        Jt.markdown(String.format("- modelType: %s", first.getModelType()));
        Jt.markdown(String.format("- testType: %s (da getTestType()))", first.getTestType()));
        Jt.markdown(String.format("- nDepth: %d", first.getnDepth()));
        Jt.markdown(String.format("- avgTs: %.3f", first.getAvgTs()));

        // Mostra tutti i model types disponibili
        List<String> modelTypes = benchmarks.stream()
                .map(BenchmarkEntry::getModelType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        Jt.markdown(String.format("**[LOG]** Model Types trovati: %s", modelTypes));

        // Mostra distribuzione test types
        long ppCount = benchmarks.stream().filter(b -> "PP".equals(b.getTestType())).count();
        long tgCount = benchmarks.stream().filter(b -> "TG".equals(b.getTestType())).count();
        long unknownCount = benchmarks.stream().filter(b -> "UNKNOWN".equals(b.getTestType()) || b.getTestType() == null).count();
        Jt.markdown(String.format("**[LOG]** Distribuzione test types: PP=%d, TG=%d, UNKNOWN=%d", ppCount, tgCount, unknownCount));

        // Istanzia ChartFactory
        ChartFactory chartFactory = new ChartFactory();
        Jt.markdown("[LOG] ChartFactory istanziata");

        // Filtering controls
        Jt.markdown("### Filters");

        // Model selection checkboxes - track state via session
        Set<String> selectedModelsSet = new HashSet<>();

        @SuppressWarnings("unchecked")
        List<String> savedSelection = (List<String>) state.get("explorerSelectedModels");
        if (savedSelection != null) {
            selectedModelsSet.addAll(savedSelection);
        } else {
            // Default: select all models on first load
            selectedModelsSet.addAll(modelTypes);
        }

        for (String model : modelTypes) {
            Jt.checkbox(model).use();
        }

        // After checkboxes are rendered, read the actual state from session/params
        // For now, use all models if none explicitly selected via checkbox interaction
        if (selectedModelsSet.isEmpty()) {
            selectedModelsSet.addAll(modelTypes);
        }

        // Make final for lambda usage
        Set<String> selectedModels = Collections.unmodifiableSet(selectedModelsSet);

        // Test type radio - default to "Both"
        List<String> testTypeOptions = Arrays.asList("Prompt Processing", "Token Generation", "Both");
        String testTypeRadioRaw = Jt.radio("Test Type", testTypeOptions).use();

        // Default to "Both" if nothing selected
        String testTypeRadio = (testTypeRadioRaw == null || testTypeRadioRaw.isEmpty()) ? "Both" : testTypeRadioRaw;

        // Filter data based on selections (inline from BenchmarkAggregator.filterByModelAndTestType)
        List<BenchmarkEntry> filteredData = benchmarks.stream()
                .filter(e -> selectedModels.isEmpty() || selectedModels.contains(e.getModelType()))
                .filter(e -> {
                    if ("Both".equals(testTypeRadio)) return true;
                    if ("Prompt Processing".equals(testTypeRadio)) return "PP".equals(e.getTestType());
                    if ("Token Generation".equals(testTypeRadio)) return "TG".equals(e.getTestType());
                    return true;
                })
                .collect(Collectors.toList());

        if (filteredData.isEmpty()) {
            Jt.warning("No data matches the current filters. Try adjusting your selection.");
            return;
        }

        // Analysis tabs - use key to track selection via session state
        String tabsKey = "explorerPageTabs";
        var tabs = Jt.tabs(Arrays.asList("Prompt Processing", "Token Generation", "Comparison")).key(tabsKey).use();

        // Get selected tab from session state (default to first tab)
        @SuppressWarnings("unchecked")
        String selectedTab = (String) state.get(tabsKey);
        if (selectedTab == null || selectedTab.isEmpty()) {
            selectedTab = "Prompt Processing";
        }

        if ("Prompt Processing".equals(selectedTab)) {
            renderPPChart(filteredData, chartFactory);
        } else if ("Token Generation".equals(selectedTab)) {
            renderTGChart(filteredData, chartFactory);
        } else {
            renderComparisonChart(filteredData, chartFactory);
        }

        // Sample details expander
        var sampleDetailsExpander = Jt.expander("Sample Details").use();
        Jt.markdown(renderSampleDetailsTable(filteredData)).use(sampleDetailsExpander);

        // Link to report page
        Jt.markdown("### Generate Report");
        Jt.pageLink("/report").use();
    }

    private static void renderPPChart(List<BenchmarkEntry> filteredData, ChartFactory chartFactory) {
        List<BenchmarkEntry> ppData = filteredData.stream()
                .filter(b -> "PP".equals(b.getTestType()))
                .collect(Collectors.toList());

        if (ppData.isEmpty()) {
            Jt.warning("No Prompt Processing data available for the selected filters.");
            return;
        }

        var chart = chartFactory.createThroughputVsDepthChart(ppData, "PP");
        Jt.echarts(chart).use();
    }

    private static void renderTGChart(List<BenchmarkEntry> filteredData, ChartFactory chartFactory) {
        List<BenchmarkEntry> tgData = filteredData.stream()
                .filter(b -> "TG".equals(b.getTestType()))
                .collect(Collectors.toList());

        if (tgData.isEmpty()) {
            Jt.warning("No Token Generation data available for the selected filters.");
            return;
        }

        var chart = chartFactory.createThroughputVsDepthChart(tgData, "TG");
        Jt.echarts(chart).use();
    }

    private static void renderComparisonChart(List<BenchmarkEntry> filteredData, ChartFactory chartFactory) {
        // Get all valid data for comparison
        List<BenchmarkEntry> comparisonData = filteredData.stream()
                .filter(b -> !"UNKNOWN".equals(b.getTestType()))
                .collect(Collectors.toList());

        if (comparisonData.isEmpty()) {
            Jt.warning("No data available for comparison.");
            return;
        }

        var chart = chartFactory.createComparisonChart(comparisonData);
        Jt.echarts(chart).use();
    }

    private static String renderSampleDetailsTable(List<BenchmarkEntry> filteredData) {
        // Collect sample-level details
        List<SampleDetail> details = new ArrayList<>();

        int idx = 0;
        for (BenchmarkEntry b : filteredData) {
            List<Double> samplesTs = b.getSamplesTs();
            if (samplesTs != null && !samplesTs.isEmpty()) {
                for (int i = 0; i < samplesTs.size(); i++) {
                    details.add(new SampleDetail(
                            FormattingUtils.truncateFilename(b.getModelType(), 25),
                            b.getTestType(),
                            b.getnDepth(),
                            i,
                            String.format("%.3f", samplesTs.get(i))
                    ));
                }
            }
            idx++;

            // Limit to first few entries for performance
            if (idx > 10) break;
        }

        if (details.isEmpty()) {
            return "No sample data available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("| Model | Test Type | Depth | Sample # | Throughput |\n");
        sb.append("|-------|-----------|-------|----------|------------|\n");

        for (SampleDetail d : details) {
            sb.append("| ").append(d.model()).append(" | ")
                    .append(d.testType()).append(" | ")
                    .append(d.depth()).append(" | ")
                    .append(d.sampleIndex()).append(" | ")
                    .append(d.throughput()).append(" |\n");
        }

        return sb.toString();
    }

    private record SampleDetail(String model, String testType, int depth, int sampleIndex, String throughput) {}
}
