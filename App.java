///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.86.0
//DEPS org.icepear.echarts:echarts-java:1.0.7
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.apache.pdfbox:pdfbox:3.0.4
//SOURCES model/BenchmarkEntry.java
//SOURCES service/BenchmarkJsonParser.java
//SOURCES service/BenchmarkAggregator.java
//SOURCES service/ChartFactory.java
//SOURCES service/PdfReportGenerator.java
//SOURCES util/FormattingUtils.java
//SOURCES pages/UploadPage.java
//SOURCES pages/ExplorerPage.java
//SOURCES pages/ReportPage.java

import io.javelit.core.Jt;

/**
 * llama-bench Performance Analyzer - A Javelit application for analyzing
 * llama-bench JSON benchmark results.
 */
public class App {

    public static void main(String[] args) {
        var currentPage = Jt.navigation(
                Jt.page("/upload", UploadPage::app).title("Upload & Overview").home(),
                Jt.page("/explorer", ExplorerPage::app).title("Performance Explorer"),
                Jt.page("/report", ReportPage::app).title("Report Generator")
        ).use();

        currentPage.run();
    }

    /**
     * Upload and Overview page.
     */
    public static class UploadPage {
        public static void app() {
            pages.UploadPage.app();
        }
    }

    /**
     * Performance Explorer page.
     */
    public static class ExplorerPage {
        public static void app() {
            pages.ExplorerPage.app();
        }
    }

    /**
     * Report Generator page.
     */
    public static class ReportPage {
        public static void app() {
            pages.ReportPage.app();
        }
    }
}
