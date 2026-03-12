package util;

/**
 * Utility class for formatting benchmark data.
 */
public class FormattingUtils {

    /**
     * Formats throughput value with units.
     */
    public static String formatThroughput(double tokensPerSecond) {
        return String.format("%.2f tok/s", tokensPerSecond);
    }

    /**
     * Formats model size in human-readable form.
     */
    public static String formatModelSize(long bytes) {
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gb >= 1.0) {
            return String.format("%.2f GB", gb);
        }
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.2f MB", mb);
    }

    /**
     * Formats parameter count in human-readable form.
     */
    public static String formatParams(long params) {
        double billions = params / 1_000_000_000.0;
        if (billions >= 1.0) {
            return String.format("%.2fB", billions);
        }
        double millions = params / 1_000_000.0;
        return String.format("%.2fM", millions);
    }

    /**
     * Formats context depth with K suffix for large values.
     */
    public static String formatDepth(int depth) {
        if (depth >= 1024) {
            double k = depth / 1024.0;
            if (k == (int) k) {
                return ((int) k) + "K";
            }
            return String.format("%.1fK", k);
        }
        return String.valueOf(depth);
    }

    /**
     * Formats a timestamp for display.
     */
    public static String formatTimestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return "N/A";
        }
        // Simple truncation: show date and time without timezone details
        if (isoTimestamp.length() > 19) {
            return isoTimestamp.substring(0, 19).replace('T', ' ');
        }
        return isoTimestamp;
    }

    /**
     * Truncates a long filename for display.
     */
    public static String truncateFilename(String filename, int maxLength) {
        if (filename == null || filename.length() <= maxLength) {
            return filename;
        }
        // Show start and end of filename
        int half = (maxLength - 3) / 2;
        return filename.substring(0, half) + "..." + filename.substring(filename.length() - half);
    }

    /**
     * Formats a boolean as Yes/No.
     */
    public static String formatBoolean(boolean value) {
        return value ? "Yes" : "No";
    }
}
