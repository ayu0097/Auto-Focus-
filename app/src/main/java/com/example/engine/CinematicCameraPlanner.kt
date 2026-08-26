package com.example.engine

import android.graphics.PointF
import android.graphics.RectF
import com.example.model.CropRect
import com.example.model.DetectedSubject
import com.example.model.FrameAnalysisResult
import com.example.model.SubjectType
import com.example.model.TrackingConfig
import com.example.model.TrackingMode
import com.example.model.VideoInfo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Calculates smooth, cinematic camera paths for 9:16 vertical reframing.
 * Implements dead-zone stabilization, critically damped spring smoothing,
 * lookahead velocity prediction, and rule-of-thirds headroom composition.
 */
class CinematicCameraPlanner {

    /**
     * Calculates the baseline 9:16 crop dimensions for a given video aspect ratio.
     * Normalized coordinates (0f..1f).
     */
    fun calculateBaseCropSize(videoInfo: VideoInfo, zoomFactor: Float = 1.0f): Pair<Float, Float> {
        val aspect = videoInfo.aspectRatio
        val targetAspect = 9f / 16f

        return if (aspect >= targetAspect) {
            // Landscape or wider than 9:16 (standard case: 16:9, 4:3, 21:9)
            // Normalized height is 1.0f, normalized width is (targetAspect / sourceAspect)
            val baseWidth = (targetAspect / aspect)
            val adjustedWidth = min(1.0f, baseWidth / zoomFactor)
            val adjustedHeight = min(1.0f, 1.0f / zoomFactor)
            Pair(adjustedWidth, adjustedHeight)
        } else {
            // Taller than 9:16 (e.g. 9:21)
            val baseHeight = (aspect / targetAspect)
            val adjustedHeight = min(1.0f, baseHeight / zoomFactor)
            val adjustedWidth = min(1.0f, 1.0f / zoomFactor)
            Pair(adjustedWidth, adjustedHeight)
        }
    }

    /**
     * Computes raw target crop window for a single frame given detected subjects or manual pins.
     */
    fun computeRawTargetCrop(
        frameTimeMs: Long,
        videoInfo: VideoInfo,
        subjects: List<DetectedSubject>,
        manualAnchor: PointF?,
        config: TrackingConfig
    ): Pair<CropRect, DetectedSubject?> {
        val (cropWidth, cropHeight) = calculateBaseCropSize(videoInfo, config.zoomFactor)

        // 1. If manual anchor exists, prioritize it
        if (manualAnchor != null) {
            val targetLeft = (manualAnchor.x - cropWidth / 2f).coerceIn(0f, 1f - cropWidth)
            val targetTop = (manualAnchor.y - cropHeight * (0.35f + config.verticalHeadroom)).coerceIn(0f, 1f - cropHeight)
            val pinSubject = DetectedSubject(
                id = 9999,
                type = SubjectType.MANUAL_PIN,
                boundingBox = RectF(manualAnchor.x - 0.05f, manualAnchor.y - 0.05f, manualAnchor.x + 0.05f, manualAnchor.y + 0.05f),
                confidence = 1.0f,
                label = "Manual Lock",
                isPrimary = true
            )
            return Pair(CropRect(targetLeft, targetTop, cropWidth, cropHeight), pinSubject)
        }

        // 2. Select primary subject based on tracking mode
        val primarySubject = selectPrimarySubject(subjects, config.mode)

        if (primarySubject != null) {
            val subjectCenter = primarySubject.centerX
            val subjectTop = primarySubject.boundingBox.top
            val subjectHeight = primarySubject.boundingBox.height()

            // Calculate horizontal framing (centered on subject with boundary clamping)
            val targetLeft = (subjectCenter - cropWidth / 2f).coerceIn(0f, 1f - cropWidth)

            // Calculate vertical framing with headroom (Rule of Thirds: eye/face at ~33%-38% from crop top)
            val desiredHeadY = when (primarySubject.type) {
                SubjectType.FACE -> subjectTop + subjectHeight * 0.3f
                SubjectType.PERSON_BODY -> subjectTop + subjectHeight * 0.2f
                else -> primarySubject.centerY
            }

            val targetTop = (desiredHeadY - cropHeight * (0.35f - config.verticalHeadroom)).coerceIn(0f, 1f - cropHeight)

            return Pair(CropRect(targetLeft, targetTop, cropWidth, cropHeight), primarySubject)
        }

        // 3. Fallback: Center framing
        val defaultLeft = ((1f - cropWidth) / 2f).coerceIn(0f, 1f - cropWidth)
        val defaultTop = ((1f - cropHeight) / 2f).coerceIn(0f, 1f - cropHeight)
        return Pair(CropRect(defaultLeft, defaultTop, cropWidth, cropHeight), null)
    }

    /**
     * Selects the most prominent subject according to current TrackingMode.
     */
    private fun selectPrimarySubject(
        subjects: List<DetectedSubject>,
        mode: TrackingMode
    ): DetectedSubject? {
        if (subjects.isEmpty()) return null

        return when (mode) {
            TrackingMode.FACE_PRIORITY -> {
                subjects.filter { it.type == SubjectType.FACE }
                    .maxByOrNull { it.area * 0.6f + it.confidence * 0.4f }
                    ?: subjects.maxByOrNull { it.confidence }
            }
            TrackingMode.ACTION_MOTION -> {
                subjects.filter { it.type == SubjectType.MOTION_CLUSTER || it.type == SubjectType.MOVING_OBJECT }
                    .maxByOrNull { (it.velocityX * it.velocityX + it.velocityY * it.velocityY) * 0.5f + it.area * 0.5f }
                    ?: subjects.maxByOrNull { it.area }
            }
            TrackingMode.SMART_AI, TrackingMode.MANUAL_KEYFRAMES -> {
                // Smart AI balances face prominence, human presence, central proximity, and size
                subjects.maxByOrNull { subject ->
                    var score = subject.confidence * 0.3f + subject.area * 0.4f
                    if (subject.type == SubjectType.FACE) score += 0.4f
                    if (subject.type == SubjectType.PERSON_BODY) score += 0.25f
                    // Central bias: penalize extreme edge artifacts
                    val distFromCenter = abs(subject.centerX - 0.5f)
                    score += (1f - distFromCenter) * 0.15f
                    score
                }
            }
        }
    }

    /**
     * Applies two-pass bidirectional cinematic smoothing with dead-zone stabilization
     * across the entire timeline of analyzed frames.
     */
    fun smoothCameraTrajectory(
        rawResults: List<FrameAnalysisResult>,
        config: TrackingConfig,
        manualAnchors: Map<Long, PointF> = emptyMap()
    ): List<FrameAnalysisResult> {
        if (rawResults.isEmpty()) return rawResults
        if (rawResults.size == 1) {
            val item = rawResults[0]
            return listOf(item.copy(smoothedCrop = item.rawCrop))
        }

        val count = rawResults.size
        val smoothedLefts = FloatArray(count)
        val smoothedTops = FloatArray(count)

        // Smoothing parameter: map 0.0..1.0 to alpha filter coefficient
        // 0.0 -> alpha = 0.95 (snappy), 1.0 -> alpha = 0.04 (ultra smooth / slow damped pan)
        val alpha = (1.0f - config.smoothness.coerceIn(0.05f, 0.98f)).coerceIn(0.02f, 0.95f)
        val deadZone = config.deadZone.coerceIn(0.005f, 0.20f)

        // Forward Pass (Simulates live camera operator tracking subject)
        var currentLeft = rawResults[0].rawCrop.left
        var currentTop = rawResults[0].rawCrop.top
        var currentVelX = 0f
        var currentVelY = 0f

        for (i in 0 until count) {
            val raw = rawResults[i].rawCrop
            val targetLeft = raw.left
            val targetTop = raw.top

            // Check manual anchor lock
            val isManualAnchor = manualAnchors.containsKey(rawResults[i].timestampMs)
            if (isManualAnchor) {
                currentLeft = targetLeft
                currentTop = targetTop
                currentVelX = 0f
                currentVelY = 0f
            } else {
                // Dead-zone calculation: only move if target exceeds dead-zone threshold
                val deltaX = targetLeft - currentLeft
                val deltaY = targetTop - currentTop

                val effectiveDeltaX = if (abs(deltaX) > deadZone) deltaX - Math.signum(deltaX) * deadZone else 0f
                val effectiveDeltaY = if (abs(deltaY) > deadZone) deltaY - Math.signum(deltaY) * deadZone else 0f

                // Spring-damper momentum update
                currentVelX = currentVelX * 0.65f + effectiveDeltaX * alpha
                currentVelY = currentVelY * 0.65f + effectiveDeltaY * alpha

                currentLeft += currentVelX
                currentTop += currentVelY
            }

            // Clamp to frame bounds
            currentLeft = currentLeft.coerceIn(0f, 1f - raw.width)
            currentTop = currentTop.coerceIn(0f, 1f - raw.height)

            smoothedLefts[i] = currentLeft
            smoothedTops[i] = currentTop
        }

        // Backward Pass (Removes phase lag for professional lookahead reframing)
        if (config.smoothness > 0.3f && count > 4) {
            var backwardLeft = smoothedLefts[count - 1]
            var backwardTop = smoothedTops[count - 1]
            val backAlpha = alpha * 0.5f

            for (i in count - 2 downTo 0) {
                backwardLeft = backwardLeft * (1f - backAlpha) + smoothedLefts[i] * backAlpha
                backwardTop = backwardTop * (1f - backAlpha) + smoothedTops[i] * backAlpha

                val width = rawResults[i].rawCrop.width
                val height = rawResults[i].rawCrop.height

                smoothedLefts[i] = (smoothedLefts[i] * 0.55f + backwardLeft * 0.45f).coerceIn(0f, 1f - width)
                smoothedTops[i] = (smoothedTops[i] * 0.55f + backwardTop * 0.45f).coerceIn(0f, 1f - height)
            }
        }

        // Return updated frame analysis results
        return rawResults.mapIndexed { index, result ->
            val cropW = result.rawCrop.width
            val cropH = result.rawCrop.height
            val smoothCrop = CropRect(
                left = smoothedLefts[index],
                top = smoothedTops[index],
                width = cropW,
                height = cropH
            )
            result.copy(smoothedCrop = smoothCrop)
        }
    }

    /**
     * Interpolates the precise crop rectangle at an arbitrary playback timestamp (milliseconds).
     * Uses cubic Hermite / smoothstep interpolation between neighboring keyframe results.
     */
    fun getCropAtTimestamp(
        timestampMs: Long,
        analysisResults: List<FrameAnalysisResult>,
        defaultCrop: CropRect = CropRect.DEFAULT
    ): CropRect {
        if (analysisResults.isEmpty()) return defaultCrop
        if (analysisResults.size == 1) return analysisResults[0].smoothedCrop

        // Binary search or linear search for closest framing segment
        var lowerIdx = 0
        var upperIdx = analysisResults.size - 1

        if (timestampMs <= analysisResults[0].timestampMs) return analysisResults[0].smoothedCrop
        if (timestampMs >= analysisResults[upperIdx].timestampMs) return analysisResults[upperIdx].smoothedCrop

        while (lowerIdx <= upperIdx) {
            val mid = (lowerIdx + upperIdx) / 2
            val midTime = analysisResults[mid].timestampMs

            if (midTime == timestampMs) {
                return analysisResults[mid].smoothedCrop
            } else if (midTime < timestampMs) {
                lowerIdx = mid + 1
            } else {
                upperIdx = mid - 1
            }
        }

        val i0 = max(0, upperIdx)
        val i1 = min(analysisResults.size - 1, lowerIdx)

        val f0 = analysisResults[i0]
        val f1 = analysisResults[i1]

        val t0 = f0.timestampMs
        val t1 = f1.timestampMs

        if (t1 == t0) return f0.smoothedCrop

        // Normalized fraction [0..1]
        val rawT = (timestampMs - t0).toFloat() / (t1 - t0).toFloat()
        // Smoothstep easing: 3t^2 - 2t^3 for cinematic fluidity
        val t = (rawT * rawT * (3f - 2f * rawT)).coerceIn(0f, 1f)

        val c0 = f0.smoothedCrop
        val c1 = f1.smoothedCrop

        val interpLeft = c0.left + (c1.left - c0.left) * t
        val interpTop = c0.top + (c1.top - c0.top) * t
        val interpWidth = c0.width + (c1.width - c0.width) * t
        val interpHeight = c0.height + (c1.height - c0.height) * t

        return CropRect(
            left = interpLeft.coerceIn(0f, 1f - interpWidth),
            top = interpTop.coerceIn(0f, 1f - interpHeight),
            width = interpWidth,
            height = interpHeight
        )
    }
}
