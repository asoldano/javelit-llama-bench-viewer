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
    private static final float PAGE_HEIGHT = 792f; // Letter height
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
        System.out.println("=== PDF GENERATION START ===");
        System.out.println("Entries count: " + entries.size());
        System.out.println("Uploaded files: " + uploadedFiles);

        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        System.out.println("Document and page created");

        PDPageContentStream currentStream = new PDPageContentStream(document, page);
        System.out.println("Content stream created");

        float yPosition = PAGE_HEIGHT - MARGIN;

        try {
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            System.out.println("Fonts loaded");

            // Use a helper method to write text at absolute position
            yPosition = writeLine(currentStream, fontBold, 18, MARGIN, yPosition, config.getTitle());
            System.out.println("Title written: " + config.getTitle());
            yPosition -= 20;

            yPosition = writeLine(currentStream, fontRegular, 10, MARGIN, yPosition, "Generated: " + Instant.now().toString());
            System.out.println("Timestamp written");

            yPosition = writeLine(currentStream, fontRegular, 10, MARGIN, yPosition, "Source files: " + String.join(", ", uploadedFiles));
            System.out.println("Source files written");
            yPosition -= SECTION_SPACING;
            System.out.println("Y after header: " + yPosition);

            // Hardware & Build Summary
            if (config.isIncludeHardwareSummary()) {
                System.out.println("=== Writing Hardware Summary ===");
                Result hwResult = appendHardwareSummary(document, currentStream, entries, yPosition, fontBold, fontRegular);
                currentStream = hwResult.stream;
                yPosition = hwResult.y;
                System.out.println("Hardware Summary done, Y now: " + yPosition);
            }

            // Model Overview
            if (config.isIncludeModelOverview()) {
                System.out.println("=== Writing Model Overview ===");
                Result modelResult = appendModelOverview(document, currentStream, entries, yPosition, fontBold, fontRegular);
                currentStream = modelResult.stream;
                yPosition = modelResult.y;
                System.out.println("Model Overview done, Y now: " + yPosition);
            }

            // PP Analysis
            if (config.isIncludePPAnalysis()) {
                System.out.println("=== Writing PP Analysis ===");
                Result ppResult = appendPPAnalysis(document, currentStream, entries, yPosition, fontBold, fontRegular);
                currentStream = ppResult.stream;
                yPosition = ppResult.y;
                System.out.println("PP Analysis done, Y now: " + yPosition);
            }

            // TG Analysis
            if (config.isIncludeTGAnalysis()) {
                System.out.println("=== Writing TG Analysis ===");
                Result tgResult = appendTGAnalysis(document, currentStream, entries, yPosition, fontBold, fontRegular);
                currentStream = tgResult.stream;
                yPosition = tgResult.y;
                System.out.println("TG Analysis done, Y now: " + yPosition);
            }

            // Comparison
            if (config.isIncludeComparison()) {
                System.out.println("=== Writing Comparison ===");
                Result compResult = appendComparison(document, currentStream, entries, yPosition, fontBold, fontRegular);
                currentStream = compResult.stream;
                yPosition = compResult.y;
                System.out.println("Comparison done, Y now: " + yPosition);
            }

            // Statistical Details
            if (config.isIncludeStatisticalDetails()) {
                System.out.println("=== Writing Statistical Details ===");
                Result statsResult = appendStatisticalDetails(document, currentStream, entries, yPosition, fontRegular);
                currentStream = statsResult.stream;
                yPosition = statsResult.y;
                System.out.println("Statistical Details done, Y now: " + yPosition);
            }

            // Notes
            if (config.isIncludeNotes() && config.getNotes() != null && !config.getNotes().isEmpty()) {
                System.out.println("=== Writing Notes ===");
                Result notesResult = appendNotes(document, currentStream, config.getNotes(), yPosition, fontRegular);
                currentStream = notesResult.stream;
                yPosition = notesResult.y;
                System.out.println("Notes done, Y now: " + yPosition);
            }

            System.out.println("=== All sections written ===");

        } catch (Exception e) {
            System.out.println("ERROR during PDF generation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            currentStream.close();
            System.out.println("Content stream closed");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            document.save(baos);
            System.out.println("Document saved, size: " + baos.size() + " bytes");
            document.close();
            return baos.toByteArray();
        }
    }

    // Helper to write a line at absolute position
    private float writeLine(PDPageContentStream cs, PDFont font, float fontSize, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private static class Result {
        PDPageContentStream stream;
        float y;
        Result(PDPageContentStream s, float y) { this.stream = s; this.y = y; }
    }

    private Result appendHardwareSummary(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        if (entries.isEmpty()) return new Result(cs, y);

        y = appendSectionHeader(document, cs, "Hardware & Build Summary", y, fontBold);
        y -= 5;

        BenchmarkEntry sample = entries.get(0);

        String[] lines = {
            "CPU: " + sample.getCpuInfo(),
            "GPU: " + sample.getGpuInfo(),
            "Backend: " + sample.getBackends(),
            "Build Commit: " + sample.getBuildCommit(),
            "Build Number: " + sample.getBuildNumber()
        };

        for (String line : lines) {
            if (y < MARGIN) {
                cs.close();
                PDPage newPage = new PDPage();
                document.addPage(newPage);
                cs = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
                y = PAGE_HEIGHT - MARGIN;
            }
            writeLine(cs, fontRegular, 10, MARGIN, y, line);
            y -= LINE_HEIGHT;
        }

        return new Result(cs, y - SECTION_SPACING);
    }

    private Result appendModelOverview(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        if (entries.isEmpty()) return new Result(cs, y);

        y = appendSectionHeader(document, cs, "Model Overview", y, fontBold);
        y -= 5;

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

            if (y < MARGIN) {
                cs.close();
                PDPage newPage = new PDPage();
                document.addPage(newPage);
                cs = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
                y = PAGE_HEIGHT - MARGIN;
            }

            String line = "- " + model + " (" + FormattingUtils.formatModelSize(sample.getModelSize()) + ", " +
                    FormattingUtils.formatParams(sample.getModelNParams()) + ")";
            writeLine(cs, fontRegular, 10, x, y, line);
            y -= LINE_HEIGHT;
        }

        return new Result(cs, y - SECTION_SPACING);
    }

    private Result appendPPAnalysis(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        y = appendSectionHeader(document, cs, "Prompt Processing (PP) Analysis", y, fontBold);
        return appendSummaryTable(document, cs, entries, "PP", y, fontRegular);
    }

    private Result appendTGAnalysis(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        y = appendSectionHeader(document, cs, "Token Generation (TG) Analysis", y, fontBold);
        return appendSummaryTable(document, cs, entries, "TG", y, fontRegular);
    }

    private Result appendComparison(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontBold, PDFont fontRegular) throws IOException {
        y = appendSectionHeader(document, cs, "Performance Comparison", y, fontBold);

        // Create a summary by model and depth
        Map<String, Map<Integer, BenchmarkEntry>> comparisonData = new LinkedHashMap<>();

        for (BenchmarkEntry e : entries) {
            String model = e.getModelType();
            int depth = e.getnDepth();
            comparisonData.computeIfAbsent(model, k -> new TreeMap<>());
            comparisonData.get(model).put(depth, e);
        }

        y = writeLine(cs, fontRegular, 10, MARGIN, y, "Throughput comparison across context depths:");

        for (Map.Entry<String, Map<Integer, BenchmarkEntry>> e : comparisonData.entrySet()) {
            String model = e.getKey();
            Map<Integer, BenchmarkEntry> depths = e.getValue();

            y = writeLine(cs, fontBold, 10, MARGIN + 20, y, model);

            for (Map.Entry<Integer, BenchmarkEntry> d : depths.entrySet()) {
                int depth = d.getKey();
                BenchmarkEntry entry = d.getValue();

                String line = "  Depth " + depth + ": PP=" + FormattingUtils.formatThroughput(entry.getAvgTs());
                if (y < MARGIN) {
                    cs.close();
                    PDPage newPage = new PDPage();
                    document.addPage(newPage);
                    cs = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
                    y = PAGE_HEIGHT - MARGIN;
                }
                y = writeLine(cs, fontRegular, 9, MARGIN + 30, y, line);
            }

            y -= SECTION_SPACING;
        }

        return new Result(cs, y);
    }

    private Result appendStatisticalDetails(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, float y, PDFont fontRegular) throws IOException {
        if (entries.isEmpty()) return new Result(cs, y);

        PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        y = appendSectionHeader(document, cs, "Statistical Details", y, fontBold);

        for (BenchmarkEntry e : entries) {
            if (y < MARGIN) {
                cs.close();
                PDPage newPage = new PDPage();
                document.addPage(newPage);
                cs = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
                y = PAGE_HEIGHT - MARGIN;
            }

            String line = e.getModelType() + " [" + e.getTestType() + ", d=" + e.getnDepth() +
                    "]: avg=" + FormattingUtils.formatThroughput(e.getAvgTs()) +
                    ", stddev=" + FormattingUtils.formatThroughput(e.getStddevTs());

            y = writeLine(cs, fontRegular, 9, MARGIN, y, line);
        }

        return new Result(cs, y);
    }

    private Result appendNotes(PDDocument document, PDPageContentStream cs, String notes, float y, PDFont fontRegular) throws IOException {
        PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        y = appendSectionHeader(document, cs, "Notes", y, fontBold);

        // Simple word wrapping
        String[] words = notes.split(" ");
        StringBuilder lineBuilder = new StringBuilder();

        for (String word : words) {
            if (lineBuilder.length() + word.length() > 80) {
                if (y < MARGIN) {
                    cs.close();
                    PDPage newPage = new PDPage();
                    document.addPage(newPage);
                    cs = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
                    y = PAGE_HEIGHT - MARGIN;
                }
                y = writeLine(cs, fontRegular, 10, MARGIN, y, lineBuilder.toString());
                lineBuilder = new StringBuilder();
            }
            if (lineBuilder.length() > 0) lineBuilder.append(" ");
            lineBuilder.append(word);
        }

        if (lineBuilder.length() > 0) {
            if (y < MARGIN) {
                cs.close();
                PDPage newPage = new PDPage();
                document.addPage(newPage);
                cs = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
                y = PAGE_HEIGHT - MARGIN;
            }
            y = writeLine(cs, fontRegular, 10, MARGIN, y, lineBuilder.toString());
        }

        return new Result(cs, y);
    }

    private Result appendSummaryTable(PDDocument document, PDPageContentStream cs, List<BenchmarkEntry> entries, String testType, float y, PDFont fontRegular) throws IOException {
        List<BenchmarkEntry> filtered = new ArrayList<>();
        for (BenchmarkEntry e : entries) {
            if (testType.equals(e.getTestType())) {
                filtered.add(e);
            }
        }

        if (filtered.isEmpty()) {
            return new Result(cs, writeLine(cs, fontRegular, 10, MARGIN, y, "No " + testType + " data available.") - LINE_HEIGHT);
        }

        // Sort by model and depth
        filtered.sort(Comparator.comparing(BenchmarkEntry::getModelType)
                .thenComparingInt(BenchmarkEntry::getnDepth));

        for (BenchmarkEntry e : filtered) {
            if (y < MARGIN) {
                cs.close();
                PDPage newPage = new PDPage();
                document.addPage(newPage);
                cs = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
                y = PAGE_HEIGHT - MARGIN;
            }

            String line = "- " + e.getModelFilename().substring(e.getModelFilename().lastIndexOf('/') + 1) +
                    ": depth=" + e.getnDepth() +
                    ", throughput=" + FormattingUtils.formatThroughput(e.getAvgTs()) +
                    ", stddev=" + FormattingUtils.formatThroughput(e.getStddevTs());

            y = writeLine(cs, fontRegular, 9, MARGIN, y, line);
        }

        return new Result(cs, y - SECTION_SPACING + LINE_HEIGHT);
    }

    private float appendSectionHeader(PDDocument document, PDPageContentStream cs, String title, float y, PDFont fontBold) throws IOException {
        if (y < MARGIN) {
            cs.close();
            PDPage newPage = new PDPage();
            document.addPage(newPage);
            cs = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
            y = PAGE_HEIGHT - MARGIN;
        }
        return writeLine(cs, fontBold, 12, MARGIN, y, title) + LINE_HEIGHT + 5;
    }
}
