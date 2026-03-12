package model;

import java.util.List;

/**
 * Represents a single benchmark entry from llama-bench JSON output.
 */
public class BenchmarkEntry {
    // Build and hardware metadata
    private final String buildCommit;
    private final int buildNumber;
    private final String cpuInfo;
    private final String gpuInfo;
    private final String backends;

    // Model metadata
    private final String modelFilename;
    private final String modelType;
    private final long modelSize;
    private final long modelNParams;

    // Execution configuration
    private final int nBatch;
    private final int nUbatch;
    private final int nThreads;
    private final int nGpuLayers;
    private final boolean flashAttn;
    private final String typeK;
    private final String typeV;
    private final boolean useMmap;

    // Benchmark identity
    private final int nPrompt;
    private final int nGen;
    private final int nDepth;

    // Benchmark result values
    private final String testTime;
    private final double avgTs;
    private final double stddevTs;
    private final long avgNs;
    private final long stddevNs;
    private final List<Double> samplesTs;
    private final List<Long> samplesNs;

    // Source file name (not in JSON, added for tracking)
    private final String sourceFile;

    public BenchmarkEntry(String buildCommit, int buildNumber, String cpuInfo, String gpuInfo, String backends,
                          String modelFilename, String modelType, long modelSize, long modelNParams,
                          int nBatch, int nUbatch, int nThreads, int nGpuLayers, boolean flashAttn,
                          String typeK, String typeV, boolean useMmap,
                          int nPrompt, int nGen, int nDepth,
                          String testTime, double avgTs, double stddevTs, long avgNs, long stddevNs,
                          List<Double> samplesTs, List<Long> samplesNs,
                          String sourceFile) {
        this.buildCommit = buildCommit;
        this.buildNumber = buildNumber;
        this.cpuInfo = cpuInfo;
        this.gpuInfo = gpuInfo;
        this.backends = backends;
        this.modelFilename = modelFilename;
        this.modelType = modelType;
        this.modelSize = modelSize;
        this.modelNParams = modelNParams;
        this.nBatch = nBatch;
        this.nUbatch = nUbatch;
        this.nThreads = nThreads;
        this.nGpuLayers = nGpuLayers;
        this.flashAttn = flashAttn;
        this.typeK = typeK;
        this.typeV = typeV;
        this.useMmap = useMmap;
        this.nPrompt = nPrompt;
        this.nGen = nGen;
        this.nDepth = nDepth;
        this.testTime = testTime;
        this.avgTs = avgTs;
        this.stddevTs = stddevTs;
        this.avgNs = avgNs;
        this.stddevNs = stddevNs;
        this.samplesTs = samplesTs;
        this.samplesNs = samplesNs;
        this.sourceFile = sourceFile;
    }

    // Getters
    public String getBuildCommit() { return buildCommit; }
    public int getBuildNumber() { return buildNumber; }
    public String getCpuInfo() { return cpuInfo; }
    public String getGpuInfo() { return gpuInfo; }
    public String getBackends() { return backends; }
    public String getModelFilename() { return modelFilename; }
    public String getModelType() { return modelType; }
    public long getModelSize() { return modelSize; }
    public long getModelNParams() { return modelNParams; }
    public int getnBatch() { return nBatch; }
    public int getnUbatch() { return nUbatch; }
    public int getnThreads() { return nThreads; }
    public int getnGpuLayers() { return nGpuLayers; }
    public boolean isFlashAttn() { return flashAttn; }
    public String getTypeK() { return typeK; }
    public String getTypeV() { return typeV; }
    public boolean isUseMmap() { return useMmap; }
    public int getnPrompt() { return nPrompt; }
    public int getnGen() { return nGen; }
    public int getnDepth() { return nDepth; }
    public String getTestTime() { return testTime; }
    public double getAvgTs() { return avgTs; }
    public double getStddevTs() { return stddevTs; }
    public long getAvgNs() { return avgNs; }
    public long getStddevNs() { return stddevNs; }
    public List<Double> getSamplesTs() { return samplesTs; }
    public List<Long> getSamplesNs() { return samplesNs; }
    public String getSourceFile() { return sourceFile; }

    /**
     * Derives the test type from n_prompt and n_gen values.
     * @return "PP" for prompt processing, "TG" for token generation, "UNKNOWN" otherwise
     */
    public String getTestType() {
        if (nPrompt > 0 && nGen == 0) {
            return "PP";
        } else if (nPrompt == 0 && nGen > 0) {
            return "TG";
        }
        return "UNKNOWN";
    }

    /**
     * Returns model size in GiB.
     */
    public double getModelSizeGiB() {
        return modelSize / (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * Returns model parameters in billions.
     */
    public double getModelParamsBillions() {
        return modelNParams / 1_000_000_000.0;
    }

    /**
     * Returns a normalized hardware signature string.
     */
    public String getHardwareSignature() {
        return cpuInfo + " | " + gpuInfo + " | " + backends;
    }

    @Override
    public String toString() {
        return "BenchmarkEntry{" +
                "modelType='" + modelType + '\'' +
                ", testType='" + getTestType() + '\'' +
                ", nDepth=" + nDepth +
                ", avgTs=" + avgTs +
                '}';
    }
}
