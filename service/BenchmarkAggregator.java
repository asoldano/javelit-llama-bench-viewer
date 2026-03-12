package service;

import model.BenchmarkEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Aggregates and summarizes benchmark data for display.
 */
public class BenchmarkAggregator {

    /**
     * Grouping key for aggregated statistics.
     */
    public static class GroupKey {
        private final String modelType;
        private final String testType;
        private final int nDepth;
        private final String hardwareSignature;

        public GroupKey(String modelType, String testType, int nDepth, String hardwareSignature) {
            this.modelType = modelType;
            this.testType = testType;
            this.nDepth = nDepth;
            this.hardwareSignature = hardwareSignature;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GroupKey)) return false;
            GroupKey groupKey = (GroupKey) o;
            return nDepth == groupKey.nDepth &&
                    modelType.equals(groupKey.modelType) &&
                    testType.equals(groupKey.testType) &&
                    hardwareSignature.equals(groupKey.hardwareSignature);
        }

        @Override
        public int hashCode() {
            int result = modelType.hashCode();
            result = 31 * result + testType.hashCode();
            result = 31 * result + nDepth;
            result = 31 * result + hardwareSignature.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "GroupKey{" +
                    "model='" + modelType + '\'' +
                    ", testType='" + testType + '\'' +
                    ", depth=" + nDepth +
                    '}';
        }
    }

    /**
     * Aggregated statistics for a group of benchmarks.
     */
    public static class GroupStats {
        private final String modelType;
        private final String testType;
        private final int nDepth;
        private final String hardwareSignature;
        private final double avgTs;
        private final double stddevTs;
        private final int rowCount;
        private final List<BenchmarkEntry> entries;

        public GroupStats(String modelType, String testType, int nDepth, String hardwareSignature,
                          double avgTs, double stddevTs, int rowCount, List<BenchmarkEntry> entries) {
            this.modelType = modelType;
            this.testType = testType;
            this.nDepth = nDepth;
            this.hardwareSignature = hardwareSignature;
            this.avgTs = avgTs;
            this.stddevTs = stddevTs;
            this.rowCount = rowCount;
            this.entries = entries;
        }

        // Getters
        public String getModelType() { return modelType; }
        public String getTestType() { return testType; }
        public int getnDepth() { return nDepth; }
        public String getHardwareSignature() { return hardwareSignature; }
        public double getAvgTs() { return avgTs; }
        public double getStddevTs() { return stddevTs; }
        public int getRowCount() { return rowCount; }
        public List<BenchmarkEntry> getEntries() { return entries; }
    }

    /**
     * Groups benchmarks by model type, test type, depth, and hardware.
     */
    public Map<GroupKey, GroupStats> groupByDimensions(List<BenchmarkEntry> entries) {
        Map<GroupKey, List<BenchmarkEntry>> grouped = new HashMap<>();

        for (BenchmarkEntry entry : entries) {
            String testType = entry.getTestType();
            if ("UNKNOWN".equals(testType)) continue;

            GroupKey key = new GroupKey(
                    entry.getModelType(),
                    testType,
                    entry.getnDepth(),
                    entry.getHardwareSignature()
            );

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        Map<GroupKey, GroupStats> result = new HashMap<>();
        for (Map.Entry<GroupKey, List<BenchmarkEntry>> e : grouped.entrySet()) {
            List<BenchmarkEntry> groupEntries = e.getValue();
            double avgTs = groupEntries.stream().mapToDouble(BenchmarkEntry::getAvgTs).average().orElse(0.0);
            double stddevTs = groupEntries.stream().mapToDouble(BenchmarkEntry::getStddevTs).average().orElse(0.0);

            result.put(e.getKey(), new GroupStats(
                    e.getKey().modelType,
                    e.getKey().testType,
                    e.getKey().nDepth,
                    e.getKey().hardwareSignature,
                    avgTs,
                    stddevTs,
                    groupEntries.size(),
                    groupEntries
            ));
        }

        return result;
    }

    /**
     * Gets all unique model types from the entries.
     */
    public List<String> getUniqueModelTypes(List<BenchmarkEntry> entries) {
        return entries.stream()
                .map(BenchmarkEntry::getModelType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets all unique hardware signatures from the entries.
     */
    public List<String> getUniqueHardwareSignatures(List<BenchmarkEntry> entries) {
        return entries.stream()
                .map(BenchmarkEntry::getHardwareSignature)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets all unique depths from the entries, sorted ascending.
     */
    public List<Integer> getUniqueDepths(List<BenchmarkEntry> entries) {
        return entries.stream()
                .map(BenchmarkEntry::getnDepth)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Filters entries by model type and test type.
     */
    public List<BenchmarkEntry> filterByModelAndTestType(
            List<BenchmarkEntry> entries,
            Set<String> selectedModels,
            String testTypeFilter) {

        return entries.stream()
                .filter(e -> selectedModels.isEmpty() || selectedModels.contains(e.getModelType()))
                .filter(e -> {
                    if ("Both".equals(testTypeFilter)) return true;
                    if ("Prompt Processing".equals(testTypeFilter)) return "PP".equals(e.getTestType());
                    if ("Token Generation".equals(testTypeFilter)) return "TG".equals(e.getTestType());
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets data points for charting: depth vs avg_ts, grouped by model.
     */
    public Map<String, List<BenchmarkEntry>> getDataPointsByModel(
            List<BenchmarkEntry> entries, String testType) {

        return entries.stream()
                .filter(e -> testType.equals(e.getTestType()))
                .collect(Collectors.groupingBy(
                        BenchmarkEntry::getModelType,
                        Collectors.toList()
                ));
    }

    /**
     * Gets distinct backends from the entries.
     */
    public List<String> getUniqueBackends(List<BenchmarkEntry> entries) {
        return entries.stream()
                .map(BenchmarkEntry::getBackends)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets distinct CPU info from the entries.
     */
    public List<String> getUniqueCpus(List<BenchmarkEntry> entries) {
        return entries.stream()
                .map(BenchmarkEntry::getCpuInfo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets distinct GPU info from the entries.
     */
    public List<String> getUniqueGpus(List<BenchmarkEntry> entries) {
        return entries.stream()
                .map(BenchmarkEntry::getGpuInfo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets distinct build commits from the entries.
     */
    public List<String> getUniqueBuildCommits(List<BenchmarkEntry> entries) {
        return entries.stream()
                .map(BenchmarkEntry::getBuildCommit)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets the maximum build number from the entries.
     */
    public int getMaxBuildNumber(List<BenchmarkEntry> entries) {
        return entries.stream()
                .mapToInt(BenchmarkEntry::getBuildNumber)
                .max()
                .orElse(0);
    }

    /**
     * Gets all unique source file names from the entries.
     */
    public List<String> getUniqueSourceFiles(List<BenchmarkEntry> entries) {
        return entries.stream()
                .map(BenchmarkEntry::getSourceFile)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Counts distinct models (by modelType).
     */
    public int countDistinctModels(List<BenchmarkEntry> entries) {
        return getUniqueModelTypes(entries).size();
    }

    /**
     * Counts distinct hardware configurations.
     */
    public int countDistinctHardwareConfigs(List<BenchmarkEntry> entries) {
        return getUniqueHardwareSignatures(entries).size();
    }
}
