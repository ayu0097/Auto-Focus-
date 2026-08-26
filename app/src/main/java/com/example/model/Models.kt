package com.example.model

import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri

/**
 * Metadata for a selected video file.
 */
data class VideoInfo(
    val uri: Uri,
    val filePath: String? = null,
    val title: String = "Video",
    val width: Int = 1920,
    val height: Int = 1080,
    val durationMs: Long = 0L,
    val rotationDegrees: Int = 0,
    val frameRate: Float = 30f,
    val bitRate: Long = 0L,
    val sizeBytes: Long = 0L,
    val hasAudio: Boolean = true
) {
    val displayWidth: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) height else width

    val displayHeight: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) width else height

    val aspectRatio: Float
        get() = if (displayHeight > 0) displayWidth.toFloat() / displayHeight.toFloat() else 16f / 9f

    val isLandscape: Boolean
        get() = aspectRatio > 1.05f

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (durationMs % 1000) / 100
            return String.format("%02d:%02d.%d", minutes, seconds, millis)
        }
}

/**
 * Types of subjects identified by the Computer Vision pipeline.
 */
enum class SubjectType {
    FACE,
    PERSON_BODY,
    MOVING_OBJECT,
    MOTION_CLUSTER,
    MANUAL_PIN
}

/**
 * Subject detected in a specific frame.
 */
data class DetectedSubject(
    val id: Int,
    val type: SubjectType,
    val boundingBox: RectF, // Normalized 0f..1f (left, top, right, bottom)
    val confidence: Float = 1.0f,
    val label: String = "Subject",
    val isPrimary: Boolean = false,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f
) {
    val centerX: Float
        get() = boundingBox.centerX()

    val centerY: Float
        get() = boundingBox.centerY()

    val area: Float
        get() = boundingBox.width() * boundingBox.height()
}

/**
 * Normalized 9:16 crop rectangle within the source video frame (0f..1f).
 */
data class CropRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f

    fun toRectF(): RectF = RectF(left, top, right, bottom)

    companion object {
        val DEFAULT = CropRect(0.25f, 0f, 0.5f, 1f)
    }
}

/**
 * Analysis result for an individual sampled video frame.
 */
data class FrameAnalysisResult(
    val frameIndex: Int,
    val timestampMs: Long,
    val subjects: List<DetectedSubject>,
    val primarySubject: DetectedSubject?,
    val rawCrop: CropRect,
    val smoothedCrop: CropRect,
    val motionEnergy: Float = 0f
)

/**
 * AI Tracking Modes
 */
enum class TrackingMode(val displayName: String, val description: String) {
    SMART_AI("Smart AI Auto", "Intelligently balances face priority, human contours & action"),
    FACE_PRIORITY("Face & Speaker", "Locks onto detected faces with rule-of-thirds headroom"),
    ACTION_MOTION("Action & Motion", "Follows rapid movement, athletes, vehicles & sports"),
    MANUAL_KEYFRAMES("Manual Pinned", "Follows user-specified anchor points and keyframes")
}

/**
 * Preset camera tracking styles.
 */
enum class TrackingPreset(
    val displayName: String,
    val smoothness: Float,
    val deadZone: Float,
    val zoomFactor: Float
) {
    CINEMATIC("Cinematic Pan", 0.85f, 0.05f, 1.0f),
    ACTION_DYNAMIC("Dynamic Action", 0.50f, 0.03f, 1.05f),
    SNAPPY("Snappy Focus", 0.25f, 0.02f, 1.0f),
    LOCKED("Tripod Locked", 0.98f, 0.12f, 1.0f)
}

/**
 * Export resolution settings.
 */
enum class ExportResolution(val displayName: String, val width: Int, val height: Int, val defaultBitrate: Int) {
    HD_720P("720p HD (720x1280)", 720, 1280, 5_000_000),
    FHD_1080P("1080p Full HD (1080x1920)", 1080, 1920, 10_000_000),
    QHD_1440P("2K QHD (1440x2560)", 1440, 2560, 16_000_000)
}

/**
 * Configuration parameters for the tracking and framing engine.
 */
data class TrackingConfig(
    val mode: TrackingMode = TrackingMode.SMART_AI,
    val smoothness: Float = 0.75f, // 0.0 (instant) to 1.0 (very slow damped pan)
    val deadZone: Float = 0.04f,   // Threshold movement before camera begins tracking
    val zoomFactor: Float = 1.0f,  // 0.9 (tighter) to 1.3 (wider)
    val verticalHeadroom: Float = 0.08f, // Rule-of-thirds headroom offset (0 = center, 0.1 = upper third)
    val targetAspectRatio: Float = 9f / 16f,
    val exportResolution: ExportResolution = ExportResolution.FHD_1080P,
    val exportBitrate: Int = ExportResolution.FHD_1080P.defaultBitrate,
    val preserveAudio: Boolean = true
)

/**
 * Stages during analysis and rendering.
 */
enum class ProcessingStage(val stageTitle: String) {
    IDLE("Ready"),
    INITIALIZING("Analyzing Video Stream..."),
    EXTRACTING_FRAMES("Extracting Keyframes..."),
    DETECTING_SUBJECTS("Detecting Subjects & Motion..."),
    SMOOTHING_CAMERA("Calculating Cinematic Camera Path..."),
    RENDERING_VIDEO("Rendering 9:16 Vertical Video..."),
    MUXING_AUDIO("Muxing High-Fidelity Audio..."),
    FINALIZING("Finalizing Output..."),
    COMPLETED("Reframing Complete!"),
    ERROR("Processing Error")
}

/**
 * Progress update emitted during background processing.
 */
data class ProcessingProgress(
    val stage: ProcessingStage = ProcessingStage.IDLE,
    val progress: Float = 0f, // 0f .. 1f
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val statusMessage: String = "",
    val estimatedSecondsLeft: Int = 0,
    val isCancellable: Boolean = true
)

/**
 * Saved project or export record.
 */
data class ExportedVideoItem(
    val id: String,
    val title: String,
    val fileUri: Uri,
    val filePath: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeFormatted: String,
    val createdAt: Long = System.currentTimeMillis()
)
