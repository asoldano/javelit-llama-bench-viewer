# PRD: llama-bench Performance Analyzer

## 0. Implementation guardrails (read first)

This project **must be implemented against Javelit `0.86.0` only**.
Before writing application code, the implementer must study the official examples in the `javelit-0.86.0` tag and use them as the primary source of truth for structure and API usage:

- `examples/getting_started`
- `examples/multifile`
- `examples/multipage`
- `examples/FileUploaderExample.java`
- `examples/EchartsExample.java`
- `examples/TabsExample.java`
- `examples/PageLinkExample.java`
- `examples/FormsExample.java`
- `examples/pdf/PdfExample.java`

Official references:

- Examples tree: <https://github.com/javelit/javelit/tree/javelit-0.86.0/examples>
- Source tree: <https://github.com/javelit/javelit/tree/javelit-0.86.0>

### Critical rule

If this PRD and the official `javelit-0.86.0` sources/examples disagree, **the official sources/examples win**.
Do not invent convenience APIs, overloads, or builder methods that are not visible in the sources for `javelit-0.86.0`.

### Important version note

Some example files inside the `javelit-0.86.0` tag still contain `//DEPS io.javelit:javelit:0.85.0` in their header comments. That header must **not** be copied verbatim. For this project, always use:

```java
//DEPS io.javelit:javelit:0.86.0
```

### Practical coding rules

1. Use the official `multifile` example as the baseline for package layout and multi-file organization.
2. Use the official `multipage` example as the baseline for navigation and page registration.
3. Use the official `FileUploaderExample`, `EchartsExample`, `TabsExample`, `PageLinkExample`, `FormsExample`, and `PdfExample` as the baseline for those components.
4. Prefer explicit, simple code over framework cleverness.
5. Do **not** introduce custom classloading workarounds unless a concrete, reproducible issue is observed during implementation.
6. Do **not** rely on undocumented JBang/Javelit interactions.

---

## 1. Product Overview

**Name**: llama-bench Performance Analyzer  
**Framework**: Javelit `0.86.0`  
**Language**: Java 21+  
**Runtime**: local Javelit/JBang execution  
**UI Language**: English

### Purpose

Build a local interactive web app that lets a user:

1. upload one or more `llama-bench` JSON files,
2. inspect benchmark results through interactive charts and tables,
3. generate a downloadable PDF report.

### Problem Statement

`llama-bench` outputs rich JSON data, but interpreting multiple runs across models, quantizations, and context depths is cumbersome when done manually. The app should make the comparison visually obvious, especially for throughput degradation across context depth.

### Target User

A technical user running local LLM benchmarks who wants fast visual comparison for demos, analysis, and reporting.

---

## 2. Required Input Data

Input files are JSON files produced by `llama-bench`.
The application must accept **multiple files** in one session.

Each JSON file is expected to contain an array of benchmark entries.

### Fields expected in each benchmark entry

#### Build and hardware metadata

- `build_commit`: string
- `build_number`: integer
- `cpu_info`: string
- `gpu_info`: string
- `backends`: string

#### Model metadata

- `model_filename`: string
- `model_type`: string
- `model_size`: long
- `model_n_params`: long

#### Execution configuration

- `n_batch`: integer
- `n_ubatch`: integer
- `n_threads`: integer
- `n_gpu_layers`: integer
- `flash_attn`: boolean
- `type_k`: string
- `type_v`: string
- `use_mmap`: boolean

#### Benchmark identity

- `n_prompt`: integer
- `n_gen`: integer
- `n_depth`: integer

#### Benchmark result values

- `test_time`: string
- `avg_ts`: double
- `stddev_ts`: double
- `avg_ns`: long
- `stddev_ns`: long
- `samples_ts`: array of double
- `samples_ns`: array of long

### Benchmark type derivation

The app must derive the benchmark type as follows:

- `n_prompt > 0 && n_gen == 0` → `PP` (Prompt Processing / prefill)
- `n_prompt == 0 && n_gen > 0` → `TG` (Token Generation / decode)
- anything else → unsupported or ignored, with a visible warning

### Non-functional behavior for input

- malformed JSON must not crash the app;
- invalid files must produce a visible error message;
- valid files uploaded later in the session must append to existing session data.

---

## 3. Product Scope

The application contains exactly **three pages**:

1. **Upload & Overview**
2. **Performance Explorer**
3. **Report Generator**

This is a **local demo/analysis tool**.
There is no persistence layer, no authentication, and no server-side database.

---

## 4. Architecture

## 4.1 Runtime model

The application is a Javelit multipage app launched locally.
Use Javelit navigation for page registration and page switching.

### Entry point responsibilities

The main entry point must:

- declare dependencies with `//DEPS`;
- declare navigation with `Jt.navigation(...)`;
- register all pages with `Jt.page(path, runnable)`;
- mark the upload page as `.home()`;
- call `.use()` on navigation and then `.run()` on the selected page.

### Required dependency header

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.86.0
//DEPS org.icepear.echarts:echarts-java:1.0.7
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.apache.pdfbox:pdfbox:3.0.4
```

If PDFBox transitive dependencies need to be made explicit during implementation, add them only after observing an actual compile/runtime need.

## 4.2 Recommended code structure

Use a multi-file structure inspired by the official `multifile` and `multipage` examples:

```text
javelit-sample/
├── App.java
├── pages/
│   ├── UploadPage.java
│   ├── ExplorerPage.java
│   └── ReportPage.java
├── model/
│   └── BenchmarkEntry.java
├── service/
│   ├── BenchmarkJsonParser.java
│   ├── BenchmarkAggregator.java
│   ├── ChartFactory.java
│   └── PdfReportGenerator.java
└── util/
    └── FormattingUtils.java
```

## 4.3 State management

Use `Jt.sessionState()` to store cross-page session data.

Required session keys:

- `benchmarks` → `List<BenchmarkEntry>`
- `uploadedFileNames` → `List<String>`
- `uploadErrors` → `List<String>`
- `reportPdfBytes` → `byte[]` (only after report generation)

### Rule

All pages must tolerate missing session state and render a useful warning instead of failing.

---

## 5. Verified Javelit API usage rules

This section is normative. The implementation must follow these API shapes.

### Navigation and pages

Use:

```java
var currentPage = Jt.navigation(
    Jt.page("/upload", UploadPage::app).title("Upload & Overview").home(),
    Jt.page("/explorer", ExplorerPage::app).title("Performance Explorer"),
    Jt.page("/report", ReportPage::app).title("Report Generator")
).use();

currentPage.run();
```

### Session state

Use:

```java
var state = Jt.sessionState();
```

### File upload

Use a labeled uploader. The uploader requires a label.
Expected pattern:

```java
var uploadedFiles = Jt.fileUploader("Upload llama-bench JSON files")
    .type(List.of(".json"))
    .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.TRUE)
    .help("Upload one or more llama-bench JSON files")
    .use();
```

### Layout containers

Use:

- `Jt.columns(int)`
- `Jt.tabs(List<String>)`
- `Jt.expander(String)`
- `Jt.container()`
- `Jt.form()`

### Standard inputs

Use:

- `Jt.checkbox(String)`
- `Jt.radio(String, List<T>)`
- `Jt.selectbox(String, List<T>)`
- `Jt.textInput(String)`
- `Jt.textArea(String)`
- `Jt.formSubmitButton(String)`

### Tables

Use one of the supported table patterns:

- `Jt.table(List<T>)`
- `Jt.table(T[])`
- `Jt.tableFromArrayColumns(Map<String, T[]>)`
- `Jt.tableFromListColumns(Map<String, List<T>>)`

For this application, prefer `Jt.table(List<T>)` for object rows and `Jt.tableFromListColumns(...)` for matrix-like summary tables.

### Charts

Use `Jt.echarts(...)` with `org.icepear.echarts` chart objects.

### PDF preview

Use:

- `Jt.pdf(byte[])` for generated in-memory report bytes

### Page navigation links

Use:

- `Jt.pageLink("/explorer")`
- `Jt.pageLink("/report")`

Do not use deprecated or unsupported page APIs.

---

## 6. Data Model

## 6.1 Core model

Define a `BenchmarkEntry` Java record or immutable class with these fields:

```text
buildCommit: String
buildNumber: int
cpuInfo: String
gpuInfo: String
backends: String
modelFilename: String
modelType: String
modelSize: long
modelNParams: long
nBatch: int
nUbatch: int
nThreads: int
nGpuLayers: int
flashAttn: boolean
typeK: String
typeV: String
useMmap: boolean
nPrompt: int
nGen: int
nDepth: int
testTime: String
avgTs: double
stddevTs: double
avgNs: long
stddevNs: long
samplesTs: List<Double> or double[]
samplesNs: List<Long> or long[]
```

## 6.2 Derived fields / helper logic

The domain layer must also expose helper logic for:

- `testType()` → `PP` / `TG` / `UNKNOWN`
- `modelSizeGiB()`
- `modelParamsBillions()`
- `hardwareSignature()` → normalized string combining CPU/GPU/backend

---

## 7. Page Specifications

## 7.1 Page 1 — Upload & Overview

**Route**: `/upload`  
**Goal**: ingest files, validate input, summarize dataset, and make the rest of the app usable.

### UI requirements

The page must render:

1. title and short description;
2. multi-file JSON uploader;
3. upload validation feedback;
4. overview metrics;
5. hardware/build summary section;
6. raw data tab;
7. summary table tab;
8. link to explorer page.

### Components

#### Header

- `Jt.title("llama-bench Performance Analyzer")`
- `Jt.markdown(...)`

#### Upload area

- `Jt.fileUploader("Upload llama-bench JSON files")`
- accepts `.json`
- accepts multiple files

#### Overview metrics

Render 3 columns using `Jt.columns(3)` with at least:

- distinct model count
- total benchmark row count
- distinct hardware configuration count

#### Hardware/build summary

Use one or more `Jt.expander(...)` sections to show:

- CPU
- GPU
- backend
- build commit
- build number
- source file name(s)

#### Tabs

Use `Jt.tabs(List.of("Raw Data", "Summary Table"))`.

##### Raw Data tab

Show a row-per-entry table including at least:

- file name
- model type
- test type
- `n_depth`
- `avg_ts`
- `stddev_ts`
- `n_batch`
- `n_ubatch`
- `flash_attn`
- `type_k`
- `type_v`

##### Summary Table tab

Show an aggregated table grouped by:

- model type
- test type
- context depth

At minimum, provide a compact table with the average throughput per depth.

### Behavior

- successful upload appends data to existing session state;
- duplicate file names are allowed, but rows must still remain distinguishable;
- malformed files produce `Jt.error(...)` and are skipped;
- when no valid data exists, render `Jt.warning(...)` and keep page stable.

---

## 7.2 Page 2 — Performance Explorer

**Route**: `/explorer`  
**Goal**: visually compare performance behavior across models and context depths.

### Preconditions

If no uploaded benchmark data exists in session state, render:

- a warning message,
- a page link back to `/upload`.

### UI requirements

The page must render:

1. title;
2. filtering controls;
3. three analysis tabs;
4. sample detail section;
5. link to report page.

### Filters

Use simple, explicit controls:

- model selection via repeated `Jt.checkbox(...)`
- test mode selector via `Jt.radio("Test Type", List.of("Prompt Processing", "Token Generation", "Both"))`

Optional later enhancement:

- hardware selector
- backend selector

### Analysis tabs

Use `Jt.tabs(List.of("Prompt Processing", "Token Generation", "Comparison"))`.

#### Prompt Processing tab

Render a line chart where:

- X axis = `n_depth`
- Y axis = `avg_ts`
- series = selected models
- only `PP` rows are included

#### Token Generation tab

Render a line chart where:

- X axis = `n_depth`
- Y axis = `avg_ts`
- series = selected models
- only `TG` rows are included

#### Comparison tab

Render either:

- grouped bars comparing PP and TG for the same selected model(s), or
- two clearly distinguished lines per model if that is simpler and more legible

The implementer should optimize for readability, not chart cleverness.

### Sample details section

Use `Jt.expander("Sample Details")` and display a table with sample-level values for the filtered view, including at least:

- model type
- test type
- `n_depth`
- sample index
- `samples_ts` value

### Chart behavior requirements

- sorted by increasing `n_depth`
- ignore unsupported rows
- show useful tooltip labels
- remain stable even if only one model or one depth is available
- show an info/warning message if a selected chart has no matching data

---

## 7.3 Page 3 — Report Generator

**Route**: `/report`  
**Goal**: generate a PDF summary report from the currently uploaded data.

### Preconditions

If no uploaded benchmark data exists, render a warning and a page link back to `/upload`.

### UI requirements

The page must render:

1. title;
2. report configuration form;
3. generate button;
4. generated PDF preview.

### Form contents

Use `Jt.form()` and include:

- report title via `Jt.textInput("Report Title")`
- section toggles via `Jt.checkbox(...)`
- notes via `Jt.textArea("Notes")`
- submit via `Jt.formSubmitButton("Generate PDF")`

### Default sections

Provide toggles for these sections:

- Hardware & Build Summary
- Model Overview
- Prompt Processing Analysis
- Token Generation Analysis
- Performance Comparison
- Statistical Details
- Notes

All should default to enabled except optional Notes.

### Generated PDF content

The PDF report is text- and table-based.
Chart image embedding is **not required**.

Minimum content:

1. report title
2. generation timestamp
3. uploaded file names
4. hardware/build summary
5. model overview
6. PP summary table
7. TG summary table
8. comparison summary
9. optional notes

### Preview behavior

After generation, store bytes in session state and render using:

```java
Jt.pdf(pdfBytes).use();
```

---

## 8. Aggregation and Reporting Rules

## 8.1 Grouping rules

The app must support grouping by:

- model type
- test type
- context depth
- hardware signature

## 8.2 Summary metrics

At minimum compute:

- average throughput (`avg_ts` from benchmark rows)
- standard deviation (`stddev_ts` from benchmark rows)
- number of rows per group
- min/max depth available per model

## 8.3 Comparison logic

Comparison views must be based on aligned dimensions.
Avoid comparing rows together when their benchmark identity differs in meaningful ways, especially if one of these changes:

- model type
- test type
- `n_depth`
- hardware signature

If the app compares unlike configurations, it must clearly indicate that in the UI or report.

---

## 9. Error Handling Requirements

The app must never crash the full page because of one bad upload or one missing data slice.

### Required user-visible failures

Use Javelit status components:

- `Jt.error(...)` for malformed JSON, failed PDF generation, or unrecoverable local issues
- `Jt.warning(...)` for missing data, unsupported rows, or empty filtered results
- `Jt.success(...)` for successful report generation
- `Jt.info(...)` for non-blocking hints

### Required resilience cases

Handle all of the following:

- empty upload result
- invalid JSON syntax
- JSON array with missing fields
- unsupported benchmark rows
- filtered chart with no results
- PDF generation failure
- session state not initialized yet

---

## 10. Non-Goals

Out of scope:

- cloud deployment
- authentication
- database persistence
- live execution of `llama-bench`
- CSV import/export
- chart image export inside the PDF
- background job system
- multi-user collaborative state

---

## 11. Acceptance Criteria

The implementation is accepted when all of the following are true:

1. the app runs locally against Javelit `0.86.0`;
2. the app uses verified Javelit APIs compatible with the `javelit-0.86.0` sources/examples;
3. one or more `llama-bench` JSON files can be uploaded successfully;
4. uploaded data is visible in raw and summarized form;
5. PP and TG views are clearly separated;
6. charts render correctly for at least one model across multiple depths;
7. the report page generates a valid PDF and previews it inline;
8. the app remains stable when uploads are malformed or incomplete;
9. the implementation does not depend on undocumented classpath/classloading tricks unless they were explicitly discovered and documented during implementation.

---

## 12. Implementation Notes for the Coding Agent

### Do this first

Before writing production code, inspect the official `javelit-0.86.0` examples listed at the start of this document.

### Do not do this

- do not assume example headers are version-correct;
- do not use APIs that are not visible in the official source tree;
- do not invent convenience helpers like unlabeled uploader builders;
- do not introduce speculative JBang/Javelit classloader workarounds.

### Prefer this style

- small page classes with `app()` methods;
- small service classes for parsing, aggregation, chart creation, and PDF generation;
- explicit session-state reads/writes;
- straightforward Java records for table rows and report rows.

---

## 13. Suggested First Milestone Order

1. skeleton multipage app with `/upload`, `/explorer`, `/report`
2. JSON upload and parsing
3. session state integration
4. raw data table
5. aggregated summary table
6. PP/TG charts
7. report form
8. PDF generation and preview
9. polish and edge-case handling
