package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import model.BenchmarkEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses llama-bench JSON files into BenchmarkEntry objects.
 */
public class BenchmarkJsonParser {

    /**
     * Parses JSON content and returns the list of benchmark entries.
     * @param jsonContent the JSON string to parse
     * @param sourceFile the source file name for tracking
     * @return list of parsed benchmark entries
     * @throws JsonParseException if the JSON is malformed or missing required fields
     */
    public List<BenchmarkEntry> parseJson(String jsonContent, String sourceFile) throws JsonParseException {
        try {
            JsonArray jsonArray = JsonParser.parseString(jsonContent).getAsJsonArray();

            List<BenchmarkEntry> entries = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject obj = jsonArray.get(i).getAsJsonObject();
                BenchmarkEntry entry = parseEntry(obj, sourceFile);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (Exception e) {
            throw new JsonParseException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a single benchmark entry from a JSON object.
     */
    private BenchmarkEntry parseEntry(JsonObject obj, String sourceFile) {
        try {
            // Build and hardware metadata
            String buildCommit = getString(obj, "build_commit", "");
            int buildNumber = getInt(obj, "build_number", 0);
            String cpuInfo = getString(obj, "cpu_info", "");
            String gpuInfo = getString(obj, "gpu_info", "");
            String backends = getString(obj, "backends", "");

            // Model metadata
            String modelFilename = getString(obj, "model_filename", "");
            String modelType = getString(obj, "model_type", "");
            long modelSize = getLong(obj, "model_size", 0L);
            long modelNParams = getLong(obj, "model_n_params", 0L);

            // Execution configuration
            int nBatch = getInt(obj, "n_batch", 0);
            int nUbatch = getInt(obj, "n_ubatch", 0);
            int nThreads = getInt(obj, "n_threads", 0);
            int nGpuLayers = getInt(obj, "n_gpu_layers", 0);
            boolean flashAttn = getBoolean(obj, "flash_attn", false);
            String typeK = getString(obj, "type_k", "");
            String typeV = getString(obj, "type_v", "");
            boolean useMmap = getBoolean(obj, "use_mmap", false);

            // Benchmark identity
            int nPrompt = getInt(obj, "n_prompt", 0);
            int nGen = getInt(obj, "n_gen", 0);
            int nDepth = getInt(obj, "n_depth", 0);

            // Benchmark result values
            String testTime = getString(obj, "test_time", "");
            double avgTs = getDouble(obj, "avg_ts", 0.0);
            double stddevTs = getDouble(obj, "stddev_ts", 0.0);
            long avgNs = getLong(obj, "avg_ns", 0L);
            long stddevNs = getLong(obj, "stddev_ns", 0L);

            List<Double> samplesTs = getListOfDoubles(obj, "samples_ts");
            List<Long> samplesNs = getListOfLongs(obj, "samples_ns");

            return new BenchmarkEntry(
                    buildCommit, buildNumber, cpuInfo, gpuInfo, backends,
                    modelFilename, modelType, modelSize, modelNParams,
                    nBatch, nUbatch, nThreads, nGpuLayers, flashAttn,
                    typeK, typeV, useMmap,
                    nPrompt, nGen, nDepth,
                    testTime, avgTs, stddevTs, avgNs, stddevNs,
                    samplesTs, samplesNs,
                    sourceFile
            );
        } catch (Exception e) {
            throw new JsonParseException("Failed to parse benchmark entry: " + e.getMessage(), e);
        }
    }

    // Helper methods for safe JSON extraction
    private String getString(JsonObject obj, String key, String defaultValue) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int getInt(JsonObject obj, String key, int defaultValue) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long getLong(JsonObject obj, String key, long defaultValue) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double getDouble(JsonObject obj, String key, double defaultValue) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBoolean() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private List<Double> getListOfDoubles(JsonObject obj, String key) {
        try {
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                return new ArrayList<>();
            }
            JsonArray arr = obj.getAsJsonArray(key);
            List<Double> result = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement el = arr.get(i);
                if (el.isJsonPrimitive()) {
                    result.add(el.getAsDouble());
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Long> getListOfLongs(JsonObject obj, String key) {
        try {
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                return new ArrayList<>();
            }
            JsonArray arr = obj.getAsJsonArray(key);
            List<Long> result = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement el = arr.get(i);
                if (el.isJsonPrimitive()) {
                    result.add(el.getAsLong());
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
