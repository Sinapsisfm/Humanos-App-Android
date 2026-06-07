# On-Device AI Strategy

> humanOS Native Android -- Phase 3 Local Inference
> Last updated: 2026-06-06

## Overview

On-device AI enables humanOS to perform select AI tasks without network connectivity, reducing latency, improving privacy, and lowering API costs. This is a Phase 3 capability that complements (not replaces) the cloud-based AI pipeline via HumanOS and QueBot backends.

## Phase

**Phase 3** -- On-device AI is not part of MVP. It requires stable core infrastructure and meaningful use cases validated in production with cloud AI first.

## Runtime Options Evaluated

| Runtime | Pros | Cons | Verdict |
|---|---|---|---|
| **ONNX Runtime Mobile** | Cross-platform, custom model support, wide hardware coverage, mature | Larger binary size, manual model optimization needed | Primary choice for custom models |
| **MediaPipe** | Google-maintained, optimized pipelines, good for vision/NLP tasks, small footprint | Limited to pre-built pipelines, less flexibility for custom models | Use for vision tasks (image classification, object detection) |
| **Gemini Nano / AICore** | On-device generative AI, Google-optimized, no model management | Android 14+ only, Pixel/Samsung only (2024+), limited availability, API in beta | Future option when device coverage improves |

### Decision

- **ONNX Runtime Mobile** as the primary inference engine for custom models (embeddings, classification).
- **MediaPipe** for pre-built vision pipelines (image description, object detection).
- **Gemini Nano / AICore** monitored for future adoption when device coverage exceeds 30% of target users.

## Use Cases

### Phase 3 Use Cases

| Use Case | Runtime | Model | Input | Output |
|---|---|---|---|---|
| Text summarization | ONNX Runtime | Custom distilled summarizer (~50MB) | Text (up to 2048 tokens) | Summary text |
| Image description | MediaPipe | Image classification + captioning pipeline | Camera capture or gallery image | Description text + tags |
| Semantic search embeddings | ONNX Runtime | MiniLM-L6 or similar (~80MB) | Text query + document corpus | Embedding vectors for local similarity search |
| Voice transcription | ONNX Runtime | Whisper Tiny (~75MB) | Audio file (WAV/M4A) | Transcribed text |

### Future Use Cases (Phase 4+)

| Use Case | Runtime | Notes |
|---|---|---|
| On-device agent reasoning | Gemini Nano | Requires AICore availability |
| Document OCR | MediaPipe | Text recognition from photos |
| Sentiment analysis | ONNX Runtime | Classify capture emotional tone |
| Smart reply suggestions | Gemini Nano | Context-aware response drafts |

## Execution Mode

```kotlin
/**
 * Controls where AI inference runs for a given task.
 * Configured per-task in user settings, with sensible defaults.
 */
enum class AiExecutionMode {
    /** Run only on-device. Fail if local model unavailable. */
    LOCAL_ONLY,
    
    /** Run only via cloud API. Fail if no network. */
    REMOTE_ONLY,
    
    /** 
     * Prefer local when available and input is within model capacity.
     * Fall back to remote when local model unavailable, input exceeds
     * local capacity, or local confidence is below threshold.
     */
    HYBRID,
    
    /** AI disabled for this task. */
    OFF
}
```

### Default Execution Modes

| Use Case | Default Mode | Rationale |
|---|---|---|
| Text summarization | `HYBRID` | Local for short texts, remote for long/complex |
| Image description | `HYBRID` | Local for basic classification, remote for detailed captions |
| Semantic search | `LOCAL_ONLY` | Privacy-sensitive, latency-critical |
| Voice transcription | `HYBRID` | Local for short clips, remote for long recordings |
| Agent reasoning | `REMOTE_ONLY` | Requires full model capability |

## Gateway Interface

```kotlin
/**
 * Abstracts the on-device AI runtime. Feature modules depend on this
 * interface, not on ONNX/MediaPipe/AICore directly.
 */
interface LocalAiGateway {

    /** Check which AI capabilities are available on this device */
    suspend fun getAvailableCapabilities(): Set<AiCapability>
    
    /** Check if a specific model is downloaded and ready */
    suspend fun isModelReady(model: AiModelId): Boolean
    
    /** Download/update a model. Returns progress flow. */
    fun downloadModel(model: AiModelId): Flow<DownloadProgress>
    
    /** Delete a downloaded model to free storage */
    suspend fun deleteModel(model: AiModelId): Boolean
    
    /** Get total storage used by downloaded models */
    suspend fun getModelsStorageBytes(): Long
    
    /** Run inference with the specified model */
    suspend fun <I, O> infer(
        model: AiModelId,
        input: I,
        outputType: KClass<O>
    ): AiResult<O>
    
    /** Run inference with execution mode routing */
    suspend fun <I, O> inferWithMode(
        task: AiTask<I, O>,
        input: I,
        mode: AiExecutionMode
    ): AiResult<O>
}

enum class AiCapability {
    TEXT_SUMMARIZATION,
    IMAGE_DESCRIPTION,
    SEMANTIC_EMBEDDINGS,
    VOICE_TRANSCRIPTION,
    GENERATIVE_TEXT       // Gemini Nano
}

data class AiModelId(
    val name: String,          // e.g., "summarizer-v1", "whisper-tiny"
    val version: Int,
    val runtime: AiRuntime
)

enum class AiRuntime {
    ONNX, MEDIAPIPE, AICORE
}

sealed class AiResult<out T> {
    data class Success<T>(
        val output: T,
        val executedOn: AiExecutionLocation,  // LOCAL or REMOTE
        val latencyMs: Long,
        val confidence: Float?                // 0.0-1.0 if applicable
    ) : AiResult<T>()
    
    data class Fallback<T>(
        val output: T,
        val localError: Throwable,            // why local failed
        val executedOn: AiExecutionLocation    // always REMOTE
    ) : AiResult<T>()
    
    data class Error(
        val error: Throwable,
        val attemptedLocations: Set<AiExecutionLocation>
    ) : AiResult<Nothing>()
}

enum class AiExecutionLocation { LOCAL, REMOTE }
```

## Model Management

### Storage

- Models stored in app-internal storage (`context.filesDir/models/`).
- Not backed up (excluded via `android:allowBackup` rules).
- Total model budget: 300MB maximum. User warned at 200MB.

### Download Strategy

- Models are **not bundled** with the APK (would bloat Play Store download).
- Models downloaded on first use or via explicit "Download AI Models" in Settings.
- Downloads require Wi-Fi by default (configurable to allow mobile data).
- Download progress shown in notification on `sync` channel.

### Update Strategy

- Model versions checked against server manifest during periodic sync.
- Updates downloaded in background via WorkManager when new version available.
- Old model kept until new model verified (atomic swap).
- User notified of available updates, not force-updated.

## Module Structure

```
core-ai/
  src/main/kotlin/.../ai/
    LocalAiGateway.kt              -- public interface
    AiExecutionMode.kt             -- execution mode enum
    AiTask.kt                      -- task definitions
    AiResult.kt                    -- result sealed class
    model/
      AiModelId.kt
      AiModelManifest.kt           -- server manifest DTO
      DownloadProgress.kt
    runtime/
      OnnxRuntimeExecutor.kt       -- ONNX Runtime wrapper
      MediaPipeExecutor.kt         -- MediaPipe wrapper
      AiCoreExecutor.kt            -- Gemini Nano wrapper (stub until available)
    routing/
      ExecutionRouter.kt           -- decides local vs remote based on mode + availability
    storage/
      ModelStorageManager.kt       -- download, cache, delete models
    di/
      AiModule.kt                  -- Hilt bindings
```

## Performance Budgets

| Metric | Target | Measurement |
|---|---|---|
| Text summarization latency | < 2 seconds for 512-token input | On Pixel 7 equivalent |
| Image description latency | < 3 seconds for 1080p image | On Pixel 7 equivalent |
| Embedding generation | < 500ms per query | On Pixel 7 equivalent |
| Voice transcription | < 1.5x real-time | 30-second clip on Pixel 7 equivalent |
| Model cold load | < 5 seconds | First inference after app start |
| Memory overhead | < 200MB during inference | Peak RSS delta |
| Battery impact | < 2% per 100 inferences | Estimated via Battery Historian |

## Privacy Considerations

- All on-device inference happens locally. No data sent to any server during `LOCAL_ONLY` execution.
- In `HYBRID` mode, data is sent to cloud only when local inference fails or is unavailable. User is informed.
- Semantic search embeddings are stored locally only, never synced to server.
- Voice transcription audio files are not retained after transcription unless user explicitly saves the capture.
- Model telemetry (latency, error rates) is collected in aggregate without input/output content.

## Testing

- **Unit tests**: Mock `LocalAiGateway` interface. Test routing logic in `ExecutionRouter`.
- **Instrumented tests**: Use small test models (< 5MB) to verify ONNX/MediaPipe integration on emulator.
- **Performance tests**: Benchmark suite on reference devices (Pixel 7, Samsung Galaxy S23).
- **CI**: AI integration tests tagged `@RequiresDevice` and run on physical device farm, not emulator.

## References

- ADR-0002: Module architecture (core-ai module)
- BACKGROUND_EXECUTION.md: WorkManager for model downloads
- PERMISSIONS_STRATEGY.md: No special permissions needed for on-device AI
- [ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/)
- [MediaPipe for Android](https://developers.google.com/mediapipe/solutions)
- [Gemini Nano / AICore](https://developer.android.com/ai/aicore)
