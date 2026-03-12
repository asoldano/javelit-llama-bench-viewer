package service;

import model.BenchmarkEntry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import util.FormattingUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Generates PDF reports from benchmark data.
 */
public class PdfReportGenerator {

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = 612f; // Letter size
    private static final float LINE_HEIGHT = 14f;
    private static final float SECTION_SPACING = 20f;

    /**
     * Configuration for report generation.
     */
    public static class ReportConfig {
        private String title;
        private boolean includeHardwareSummary;
        private boolean includeModelOverview;
        private boolean includePPAnalysis;
        private boolean includeTGAnalysis;
        private boolean includeComparison;
        private boolean includeStatisticalDetails;
        private boolean includeNotes;
        private String notes;

        public ReportConfig() {
            this.title = "llama-bench Performance Report";
            this.includeHardwareSummary = true;
            this.includeModelOverview = true;
            this.includePPAnalysis = true;
            this.includeTGAnalysis = true;
            this.includeComparison = true;
            this.includeStatisticalDetails = false;
            this.includeNotes = false;
            this.notes = "";
        }

        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public boolean isIncludeHardwareSummary() { return includeHardwareSummary; }
        public void setIncludeHardwareSummary(boolean v) { this.includeHardwareSummary = v; }
        public boolean isIncludeModelOverview() { return includeModelOverview; }
        public void setIncludeModelOverview(boolean v) { this.includeModelOverview = v; }
        public boolean isIncludePPAnalysis() { return includePPAnalysis; }
        public void setIncludePPAnalysis(boolean v) { this.includePPAnalysis = v; }
        public boolean isIncludeTGAnalysis() { return includeTGAnalysis; }
        public void setIncludeTGAnalysis(boolean v) { this.includeTGAnalysis = v; }
        public boolean isIncludeComparison() { return includeComparison; }
        public void setIncludeComparison(boolean v) { this.includeComparison = v; }
        public boolean isIncludeStatisticalDetails() { return includeStatisticalDetails; }
        public void setIncludeStatisticalDetails(boolean v) { this.includeStatisticalDetails = v; }
        public boolean isIncludeNotes() { return includeNotes; }
        public void setIncludeNotes(boolean v) { this.includeNotes = v; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    /**
     * Generates a PDF report from the benchmark data.
     */
    public byte[] generateReport(List<BenchmarkEntry> entries, List<String> uploadedFiles, ReportConfig config) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        float yPosition = PAGE_WIDTH - MARGIN; // Start from top (PDF coordinates)

        try {
            // PDFBox 3.x uses Standard14Fonts.FontName enum with constructor
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // Title
            contentStream.beginText();
            contentStream.setFont(fontBold, 18);
            contentStream.setLeading(LINE_HEIGHT);
            contentStream.newLineAtOffset(MARGIN, yPosition - 20);
            contentStream.showText(config.getTitle());
            contentStream.endText();

            yPosition -= 40;

            // Generation timestamp
            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Generated: " + Instant.now().toString());
            contentStream.endText();

            yPosition -= LINE_HEIGHT;

            // Uploaded files
            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Source files: " + String.join(", ", uploadedFiles));
            contentStream.endText();

            yPosition -= SECTION_SPACING;

            // Hardware & Build Summary
            if (config.isIncludeHardwareSummary()) {
                yPosition = appendHardwareSummary(document, contentStream, entries, yPosition, fontBold, fontRegular);
            }

            // Model Overview
            if (config.isIncludeModelOverview()) {
                yPosition = appendModelOverview(document, contentStream, entries, yPosition, fontBold, fontRegular);
            }

            // PP Analysis
            if (config.isIncludePPAnalysis()) {
                yPosition = appendPPAnalysis(document, contentStream, entries, yPosition, fontBold, fontRegular);
            }

            // TG Analysis
            if (config.isIncludeTGAnalysis()) {
                yPosition = appendTGAnalysis(document, contentStream, entries, yPosition, fontBold, fontRegular);
            }

            // Comparison
            if (config.isIncludeComparison()) {
                yPosition = appendComparison(document, contentStream, entries, yPosition, fontBold, fontRegular);
            }

            // Statistical Details
            if (config.isIncludeStatisticalDetails()) {
                yPosition = appendStatisticalDetails(document, contentStream, entries, yPosition, fontRegular);
            }

            // Notes
            if (config.isIncludeNotes() && config.getNotes() != null && !config.getNotes().isEmpty()) {
                yPosition = appendNotes(document, contentStream, config.getNotes(), yPosition, fontRegular);
            }

        } finally {
            contentStream.close();
        }

        // Save to byte array using ByteArrayOutputStream
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            document.save(baos);
            document.close();
            return baos.toByteArray();
        }
    }

    private float appendHardwareSummary(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        if (entries.isEmpty()) return y;

        y = appendSectionHeader(document, cs, "Hardware & Build Summary", y, fontBold);

        BenchmarkEntry sample = entries.get(0);

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("CPU: " + sample.getCpuInfo());
        cs.endText();
        y -= LINE_HEIGHT;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("GPU: " + sample.getGpuInfo());
        cs.endText();
        y -= LINE_HEIGHT;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Backend: " + sample.getBackends());
        cs.endText();
        y -= LINE_HEIGHT;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Build Commit: " + sample.getBuildCommit());
        cs.endText();
        y -= LINE_HEIGHT;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Build Number: " + sample.getBuildNumber());
        cs.endText();
        y -= LINE_HEIGHT - SECTION_SPACING;

        return y;
    }

    private float appendModelOverview(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        if (entries.isEmpty()) return y;

        y = appendSectionHeader(document, cs, "Model Overview", y, fontBold);

        Set<String> modelTypes = new LinkedHashSet<>();
        for (BenchmarkEntry e : entries) {
            modelTypes.add(e.getModelType());
        }

        float x = MARGIN + 20;
        for (String model : modelTypes) {
            BenchmarkEntry sample = null;
            for (BenchmarkEntry e : entries) {
                if (e.getModelType().equals(model)) {
                    sample = e;
                    break;
                }
            }

            cs.beginText();
            cs.setFont(fontRegular, 10);
            cs.newLineAtOffset(x, y);
            String line = "- " + model + " (" + FormattingUtils.formatModelSize(sample.getModelSize()) + ", " +
                    FormattingUtils.formatParams(sample.getModelNParams()) + ")";
            cs.showText(line);
            cs.endText();
            y -= LINE_HEIGHT;
        }

        y -= SECTION_SPACING - LINE_HEIGHT;
        return y;
    }

    private float appendPPAnalysis(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        y = appendSectionHeader(document, cs, "Prompt Processing (PP) Analysis", y, fontBold);
        y = appendSummaryTable(document, cs, entries, "PP", y, fontRegular);
        return y;
    }

    private float appendTGAnalysis(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        y = appendSectionHeader(document, cs, "Token Generation (TG) Analysis", y, fontBold);
        y = appendSummaryTable(document, cs, entries, "TG", y, fontRegular);
        return y;
    }

    private float appendComparison(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        y = appendSectionHeader(document, cs, "Performance Comparison", y, fontBold);

        // Create a summary by model and depth
        Map<String, Map<Integer, BenchmarkEntry>> comparisonData = new LinkedHashMap<>();

        for (BenchmarkEntry e : entries) {
            String model = e.getModelType();
            int depth = e.getnDepth();
            comparisonData.computeIfAbsent(model, k -> new TreeMap<>());
            comparisonData.get(model).put(depth, e);
        }

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Throughput comparison across context depths:");
        cs.endText();
        y -= LINE_HEIGHT;

        for (Map.Entry<String, Map<Integer, BenchmarkEntry>> e : comparisonData.entrySet()) {
            String model = e.getKey();
            Map<Integer, BenchmarkEntry> depths = e.getValue();

            cs.beginText();
            cs.setFont(fontBold, 10);
            cs.newLineAtOffset(MARGIN + 20, y);
            cs.showText(model);
            cs.endText();
            y -= LINE_HEIGHT;

            for (Map.Entry<Integer, BenchmarkEntry> d : depths.entrySet()) {
                int depth = d.getKey();
                BenchmarkEntry entry = d.getValue();

                String line = "  Depth " + depth + ": PP=" + FormattingUtils.formatThroughput(entry.getAvgTs());
                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(MARGIN + 30, y);
                cs.showText(line);
                cs.endText();
                y -= LINE_HEIGHT;
            }
        }

        y -= SECTION_SPACING - LINE_HEIGHT;
        return y;
    }

    private float appendStatisticalDetails(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontRegular) throws IOException {
        if (entries.isEmpty()) return y;

        PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        y = appendSectionHeader(document, cs, "Statistical Details", y, fontBold);

        for (BenchmarkEntry e : entries) {
            String line = e.getModelType() + " [" + e.getTestType() + ", d=" + e.getnDepth() +
                    "]: avg=" + FormattingUtils.formatThroughput(e.getAvgTs()) +
                    ", stddev=" + FormattingUtils.formatThroughput(e.getStddevTs());

            cs.beginText();
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(line);
            cs.endText();
            y -= LINE_HEIGHT;

            if (y < MARGIN) {
                // Add new page
                PDPage newPage = new PDPage();
                document.addPage(newPage);
                cs.close();
                cs = new PDPageContentStream(document, newPage);
                y = PAGE_WIDTH - MARGIN;
            }
        }

        return y;
    }

    private float appendNotes(PDDocument document, PDPageContentStream cs, String notes, float y, PDFont fontRegular) throws IOException {
        PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        y = appendSectionHeader(document, cs, "Notes", y, fontBold);

        // Simple word wrapping
        String[] words = notes.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.length() + word.length() > 80) {
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(line.toString());
                cs.endText();
                y -= LINE_HEIGHT;
                line = new StringBuilder();
            }
            if (line.length() > 0) line.append(" ");
            line.append(word);
        }

        if (line.length() > 0) {
            cs.beginText();
            cs.setFont(fontRegular, 10);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(line.toString());
            cs.endText();
            y -= LINE_HEIGHT;
        }

        return y;
    }

    private float appendSummaryTable(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, String testType, float y, PDFont fontRegular) throws IOException {
        List<BenchmarkEntry> filtered = new ArrayList<>();
        for (BenchmarkEntry e : entries) {
            if (testType.equals(e.getTestType())) {
                filtered.add(e);
            }
        }

        if (filtered.isEmpty()) {
            cs.beginText();
            cs.setFont(fontRegular, 10);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText("No " + testType + " data available.");
            cs.endText();
            return y - LINE_HEIGHT;
        }

        // Sort by model and depth
        filtered.sort(Comparator.comparing(BenchmarkEntry::getModelType)
                .thenComparingInt(BenchmarkEntry::getnDepth));

        for (BenchmarkEntry e : filtered) {
            String line = "- " + e.getModelFilename().substring(e.getModelFilename().lastIndexOf('/') + 1) +
                    ": depth=" + e.getnDepth() +
                    ", throughput=" + FormattingUtils.formatThroughput(e.getAvgTs()) +
                    ", stddev=" + FormattingUtils.formatThroughput(e.getStddevTs());

            cs.beginText();
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(line);
            cs.endText();
            y -= LINE_HEIGHT;

            if (y < MARGIN) {
                PDPage newPage = new PDPage();
                document.addPage(newPage);
                cs.close();
                cs = new PDPageContentStream(document, newPage);
                y = PAGE_WIDTH - MARGIN;
            }
        }

        return y - SECTION_SPACING + LINE_HEIGHT;
    }

    private float appendSectionHeader(PDDocument document, PDPageContentStream cs, String title, float y, PDFont fontBold) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, 12);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(title);
        cs.endText();
        return y - LINE_HEIGHT - 5;
    }
}
