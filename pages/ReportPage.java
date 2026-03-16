package pages;

import io.javelit.core.Jt;
import model.BenchmarkEntry;
import service.PdfReportGenerator;
import service.PdfReportGenerator.ReportConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Check if generate button was clicked - use session state flag
        Boolean generateRequested = (Boolean) state.get("generateReportRequested");

        // Read form values from session state (persisted by clearOnSubmit(false))
        @SuppressWarnings("unchecked")
        Map<String, Object> formValues = (Map<String, Object>) state.get("formValues");
        if (formValues == null) {
            formValues = new HashMap<>();
        }

        if (generateRequested != null && generateRequested) {
            try {
                System.out.println("=== GENERATE BUTTON CLICKED ===");
                System.out.println("Benchmarks count: " + (benchmarks != null ? benchmarks.size() : 0));
                System.out.println("Uploaded files: " + (uploadedFiles != null ? uploadedFiles : "null"));

                // Read form values that were submitted - use default true for checkboxes if not present
                String reportTitleValue = (String) formValues.get("reportTitle");
                if (reportTitleValue == null) reportTitleValue = "";

                String hwVal = (String) formValues.get("includeHardwareSummary");
                boolean includeHardwareSummaryVal = hwVal != null ? Boolean.parseBoolean(hwVal) : true;
                String modelVal = (String) formValues.get("includeModelOverview");
                boolean includeModelOverviewVal = modelVal != null ? Boolean.parseBoolean(modelVal) : true;
                String ppVal = (String) formValues.get("includePPAnalysis");
                boolean includePPAnalysisVal = ppVal != null ? Boolean.parseBoolean(ppVal) : true;
                String tgVal = (String) formValues.get("includeTGAnalysis");
                boolean includeTGAnalysisVal = tgVal != null ? Boolean.parseBoolean(tgVal) : true;
                String compVal = (String) formValues.get("includeComparison");
                boolean includeComparisonVal = compVal != null ? Boolean.parseBoolean(compVal) : true;
                String statsVal = (String) formValues.get("includeStatisticalDetails");
                boolean includeStatisticalDetailsVal = statsVal != null ? Boolean.parseBoolean(statsVal) : false;
                String notesChkVal = (String) formValues.get("includeNotes");
                boolean includeNotesVal = notesChkVal != null ? Boolean.parseBoolean(notesChkVal) : false;
                String notesVal = (String) formValues.get("notes");
                if (notesVal == null) notesVal = "";

                System.out.println("Form values - Title: " + reportTitleValue +
                    ", HW: " + includeHardwareSummaryVal +
                    ", Model: " + includeModelOverviewVal +
                    ", PP: " + includePPAnalysisVal +
                    ", TG: " + includeTGAnalysisVal +
                    ", Comp: " + includeComparisonVal);

                ReportConfig config = new ReportConfig();
                config.setTitle(reportTitleValue.isEmpty() ? "llama-bench Performance Report" : reportTitleValue);
                config.setIncludeHardwareSummary(includeHardwareSummaryVal);
                config.setIncludeModelOverview(includeModelOverviewVal);
                config.setIncludePPAnalysis(includePPAnalysisVal);
                config.setIncludeTGAnalysis(includeTGAnalysisVal);
                config.setIncludeComparison(includeComparisonVal);
                config.setIncludeStatisticalDetails(includeStatisticalDetailsVal);
                config.setIncludeNotes(includeNotesVal);
                config.setNotes(notesVal);

                System.out.println("Calling PdfReportGenerator.generateReport()...");
                byte[] pdfBytes = pdfGenerator.generateReport(benchmarks, uploadedFiles != null ? uploadedFiles : List.of(), config);
                System.out.println("PDF generated, size: " + pdfBytes.length + " bytes");

                // Store in session state and reset flag
                state.put("reportPdfBytes", pdfBytes);
                state.put("generateReportRequested", false);
            } catch (IOException ex) {
                System.out.println("ERROR generating PDF: " + ex.getMessage());
                ex.printStackTrace();
                Jt.error("Failed to generate PDF: " + ex.getMessage());
            }
        }

        // Report configuration form
        Jt.markdown("### Configure Report");

        var form = Jt.form().clearOnSubmit(false).use();

        Jt.textInput("reportTitle").placeholder("llama-bench Performance Report").key("titleInput").use(form);

        Jt.checkbox("includeHardwareSummary").key("hwCheckbox").use(form);
        Jt.checkbox("includeModelOverview").key("modelCheckbox").use(form);
        Jt.checkbox("includePPAnalysis").key("ppCheckbox").use(form);
        Jt.checkbox("includeTGAnalysis").key("tgCheckbox").use(form);
        Jt.checkbox("includeComparison").key("compCheckbox").use(form);
        Jt.checkbox("includeStatisticalDetails").key("statsCheckbox").use(form);
        Jt.checkbox("includeNotes").key("notesCheckbox").use(form);

        Jt.textArea("notes").placeholder("Add any notes...").key("notesInput").use(form);

        // Generate button - sets a flag in state to trigger generation on next render
        var generateBtn = Jt.formSubmitButton("Generate PDF")
                .onClick(e -> {
                    state.put("generateReportRequested", true);
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
