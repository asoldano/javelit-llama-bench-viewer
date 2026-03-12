package pages;

import io.javelit.core.Jt;
import model.BenchmarkEntry;
import service.PdfReportGenerator;
import service.PdfReportGenerator.ReportConfig;

import java.io.IOException;
import java.util.List;

/**
 * Report Generator page for creating PDF reports.
 */
public class ReportPage {

    private static PdfReportGenerator pdfGenerator;

    private static synchronized void ensureInitialized() {
        if (pdfGenerator == null) {
            pdfGenerator = new PdfReportGenerator();
        }
    }

    public static void app() {
        ensureInitialized();
        Jt.title("Report Generator");

        var state = Jt.sessionState();

        @SuppressWarnings("unchecked")
        List<BenchmarkEntry> benchmarks = (List<BenchmarkEntry>) state.get("benchmarks");

        @SuppressWarnings("unchecked")
        List<String> uploadedFiles = (List<String>) state.get("uploadedFileNames");

        if (benchmarks == null || benchmarks.isEmpty()) {
            Jt.warning("No benchmark data available. Please upload files on the [Upload page](_next:/upload).");
            return;
        }

        // Check for previously generated PDF
        @SuppressWarnings("unchecked")
        byte[] previousPdf = (byte[]) state.get("reportPdfBytes");

        if (previousPdf != null && previousPdf.length > 0) {
            Jt.success("A report has been generated. Preview below or generate a new one.");
            Jt.pdf(previousPdf).use();
            Jt.markdown("---");
        }

        // Report configuration form
        Jt.markdown("### Configure Report");

        var form = Jt.form().clearOnSubmit(false).use();

        String reportTitle = Jt.textInput("Report Title").placeholder("llama-bench Performance Report").use(form);

        boolean includeHardwareSummary = Jt.checkbox("Include Hardware & Build Summary").use(form);
        boolean includeModelOverview = Jt.checkbox("Include Model Overview").use(form);
        boolean includePPAnalysis = Jt.checkbox("Include Prompt Processing Analysis").use(form);
        boolean includeTGAnalysis = Jt.checkbox("Include Token Generation Analysis").use(form);
        boolean includeComparison = Jt.checkbox("Include Performance Comparison").use(form);
        boolean includeStatisticalDetails = Jt.checkbox("Include Statistical Details").use(form);
        boolean includeNotes = Jt.checkbox("Include Notes").use(form);

        String notes = "";
        if (includeNotes) {
            notes = Jt.textArea("Notes").placeholder("").use(form);
        }

        // Generate button - use onClick callback pattern with lambda that captures state
        final String finalReportTitle = reportTitle.isEmpty() ? "llama-bench Performance Report" : reportTitle;
        final String finalNotes = notes;
        final boolean finalIncludeHardwareSummary = includeHardwareSummary;
        final boolean finalIncludeModelOverview = includeModelOverview;
        final boolean finalIncludePPAnalysis = includePPAnalysis;
        final boolean finalIncludeTGAnalysis = includeTGAnalysis;
        final boolean finalIncludeComparison = includeComparison;
        final boolean finalIncludeStatisticalDetails = includeStatisticalDetails;
        final boolean finalIncludeNotes = includeNotes;

        var generateBtn = Jt.button("Generate PDF")
                .onClick(e -> {
                    try {
                        ReportConfig config = new ReportConfig();
                        config.setTitle(finalReportTitle);
                        config.setIncludeHardwareSummary(finalIncludeHardwareSummary);
                        config.setIncludeModelOverview(finalIncludeModelOverview);
                        config.setIncludePPAnalysis(finalIncludePPAnalysis);
                        config.setIncludeTGAnalysis(finalIncludeTGAnalysis);
                        config.setIncludeComparison(finalIncludeComparison);
                        config.setIncludeStatisticalDetails(finalIncludeStatisticalDetails);
                        config.setIncludeNotes(finalIncludeNotes);
                        config.setNotes(finalNotes);

                        byte[] pdfBytes = pdfGenerator.generateReport(benchmarks, uploadedFiles != null ? uploadedFiles : List.of(), config);

                        // Store in session state - use the captured 'state' variable
                        state.put("reportPdfBytes", pdfBytes);
                    } catch (IOException ex) {
                        Jt.error("Failed to generate PDF: " + ex.getMessage());
                    }
                })
                .use(form);

        // Check if PDF was just generated and show it
        @SuppressWarnings("unchecked")
        byte[] newlyGeneratedPdf = (byte[]) state.get("reportPdfBytes");
        if (newlyGeneratedPdf != null && newlyGeneratedPdf.length > 0) {
            Jt.success("PDF report generated successfully!");
            Jt.pdf(newlyGeneratedPdf).use();
        }

        // Link back to upload page
        Jt.markdown("---");
        Jt.pageLink("/upload").use();
    }
}
