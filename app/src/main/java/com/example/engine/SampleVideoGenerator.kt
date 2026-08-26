package com.example.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sample video scenario types.
 */
enum class SampleVideoType(
    val title: String,
    val subtitle: String,
    val durationSeconds: Int,
    val width: Int,
    val height: Int
) {
    SKATER_ACTION(
        title = "Action Skater (16:9)",
        subtitle = "Fast horizontal motion, jumping and speed changes",
        durationSeconds = 6,
        width = 1280,
        height = 720
    ),
    KEYNOTE_SPEAKER(
        title = "Keynote Speaker (16:9)",
        subtitle = "Speaker pacing across stage with gestures and pauses",
        durationSeconds = 8,
        width = 1280,
        height = 720
    ),
    DYNAMIC_DANCE(
        title = "Dynamic Fitness & Dance",
        subtitle = "Dynamic vertical & lateral leaps, multi-direction movement",
        durationSeconds = 6,
        width = 1280,
        height = 720
    )
}

/**
 * Generates valid, playable landscape MP4 video clips with rich animated subjects
 * to test automatic 9:16 vertical reframing and tracking instantly.
 */
class SampleVideoGenerator(private val context: Context) {

    suspend fun generateSampleVideo(
        type: SampleVideoType,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "sample_videos").apply { mkdirs() }
        val outputFile = File(outputDir, "${type.name.lowercase()}_sample.mp4")

        if (outputFile.exists() && outputFile.length() > 50_000) {
            onProgress(1.0f)
            return@withContext outputFile
        }

        val width = type.width
        val height = type.height
        val fps = 30
        val totalFrames = type.durationSeconds * fps
        val bitRate = 4_000_000

        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(mimeType)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val frameDurationUs = 1_000_000L / fps

        // Graphics paints
        val bgPaint = Paint().apply { isAntiAlias = true }
        val subjectPaint = Paint().apply { isAntiAlias = true }
        val accentPaint = Paint().apply { isAntiAlias = true }
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 28f
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        try {
            for (frame in 0 until totalFrames) {
                val progress = frame.toFloat() / totalFrames.toFloat()
                onProgress(progress)

                val canvas = inputSurface.lockHardwareCanvas()
                val t = frame.toFloat() / fps.toFloat() // seconds

                drawFrameScene(canvas, type, t, width, height, bgPaint, subjectPaint, accentPaint, textPaint)
                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder output buffers
                var outputDone = false
                while (!outputDone) {
                    val status = encoder.dequeueOutputBuffer(bufferInfo, 2000L)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        outputDone = true
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) throw RuntimeException("Format changed twice")
                        val newFormat = encoder.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (status >= 0) {
                        val encodedData = encoder.getOutputBuffer(status)
                        if (encodedData != null && muxerStarted) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size != 0) {
                                bufferInfo.presentationTimeUs = frame * frameDurationUs
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(status, false)
                    }
                }
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()
            var eos = false
            while (!eos) {
                val status = encoder.dequeueOutputBuffer(bufferInfo, 5000L)
                if (status >= 0) {
                    val encodedData = encoder.getOutputBuffer(status)
                    if (encodedData != null && muxerStarted && bufferInfo.size != 0) {
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(status, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        eos = true
                    }
                } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    eos = true
                }
            }
        } finally {
            try {
                encoder.stop()
                encoder.release()
            } catch (ignored: Exception) {}

            try {
                if (muxerStarted) {
                    muxer.stop()
                    muxer.release()
                }
            } catch (ignored: Exception) {}
        }

        outputFile
    }

    private fun drawFrameScene(
        canvas: Canvas,
        type: SampleVideoType,
        t: Float,
        width: Int,
        height: Int,
        bgPaint: Paint,
        subjectPaint: Paint,
        accentPaint: Paint,
        textPaint: Paint
    ) {
        val totalDuration = type.durationSeconds.toFloat()
        val normTime = (t / totalDuration).coerceIn(0f, 1f)

        when (type) {
            SampleVideoType.SKATER_ACTION -> {
                // Background: Skatepark sunset gradient
                bgPaint.color = Color.rgb(15, 23, 42)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Ground & Ramp
                accentPaint.color = Color.rgb(30, 41, 59)
                canvas.drawRect(0f, height * 0.75f, width.toFloat(), height.toFloat(), accentPaint)

                // Ramps
                val rampPath = Path().apply {
                    moveTo(0f, height * 0.75f)
                    quadTo(width * 0.2f, height * 0.75f, width * 0.3f, height * 0.55f)
                    lineTo(width * 0.35f, height * 0.75f)
                    close()
                }
                accentPaint.color = Color.rgb(51, 65, 85)
                canvas.drawPath(rampPath, accentPaint)

                // Skater position (moves across screen left to right, jumps on ramp)
                val skaterX = width * (0.15f + 0.70f * (0.5f - 0.5f * cos(normTime * 2 * Math.PI.toFloat())))
                val jumpY = if (skaterX in (width * 0.25f)..(width * 0.65f)) {
                    val jumpPhase = ((skaterX - width * 0.25f) / (width * 0.4f)).coerceIn(0f, 1f)
                    -sin(jumpPhase * Math.PI.toFloat()) * height * 0.25f
                } else 0f

                val skaterY = height * 0.65f + jumpY

                // Draw Skater Figure
                // Body & Hoodie
                subjectPaint.color = Color.rgb(0, 240, 255) // Cyan jacket
                canvas.drawRoundRect(skaterX - 25f, skaterY - 60f, skaterX + 25f, skaterY + 10f, 12f, 12f, subjectPaint)

                // Head & Helmet
                subjectPaint.color = Color.rgb(255, 215, 0)
                canvas.drawCircle(skaterX, skaterY - 80f, 22f, subjectPaint)
                // Helmet
                accentPaint.color = Color.rgb(244, 63, 94)
                canvas.drawArc(skaterX - 24f, skaterY - 104f, skaterX + 24f, skaterY - 65f, 180f, 180f, true, accentPaint)

                // Legs & Pants
                subjectPaint.color = Color.rgb(30, 58, 138)
                canvas.drawRect(skaterX - 18f, skaterY + 10f, skaterX - 4f, skaterY + 50f, subjectPaint)
                canvas.drawRect(skaterX + 4f, skaterY + 10f, skaterX + 18f, skaterY + 50f, subjectPaint)

                // Skateboard
                accentPaint.color = Color.rgb(234, 88, 12)
                canvas.drawRoundRect(skaterX - 45f, skaterY + 52f, skaterX + 45f, skaterY + 62f, 4f, 4f, accentPaint)
                // Wheels
                accentPaint.color = Color.rgb(226, 232, 240)
                canvas.drawCircle(skaterX - 30f, skaterY + 68f, 6f, accentPaint)
                canvas.drawCircle(skaterX + 30f, skaterY + 68f, 6f, accentPaint)

                canvas.drawText("Action Skater (16:9 Source) • Speed: ${(skaterX / 10).toInt()} km/h", 40f, 60f, textPaint)
            }

            SampleVideoType.KEYNOTE_SPEAKER -> {
                // Background: Conference stage with illuminated LED screen
                bgPaint.color = Color.rgb(10, 15, 29)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Stage Screen backdrop
                accentPaint.color = Color.rgb(26, 38, 66)
                canvas.drawRoundRect(width * 0.1f, height * 0.15f, width * 0.9f, height * 0.65f, 16f, 16f, accentPaint)

                // Stage Floor
                accentPaint.color = Color.rgb(20, 27, 45)
                canvas.drawRect(0f, height * 0.70f, width.toFloat(), height.toFloat(), accentPaint)

                // Speaker walking across stage
                val speakerX = width * (0.30f + 0.40f * (0.5f - 0.5f * cos(normTime * 3 * Math.PI.toFloat())))
                val speakerY = height * 0.60f
                val gesture = sin(t * 4f) * 15f

                // Speaker Body & Blazer
                subjectPaint.color = Color.rgb(99, 102, 241) // Indigo Suit
                canvas.drawRoundRect(speakerX - 30f, speakerY - 70f, speakerX + 30f, speakerY + 30f, 14f, 14f, subjectPaint)

                // Head & Face (Realistic skin tone)
                subjectPaint.color = Color.rgb(245, 195, 155)
                canvas.drawCircle(speakerX, speakerY - 95f, 24f, subjectPaint)
                // Hair
                accentPaint.color = Color.rgb(40, 25, 15)
                canvas.drawArc(speakerX - 25f, speakerY - 120f, speakerX + 25f, speakerY - 80f, 180f, 180f, true, accentPaint)

                // Gesturing Hands
                subjectPaint.color = Color.rgb(245, 195, 155)
                canvas.drawCircle(speakerX - 40f, speakerY - 20f + gesture, 10f, subjectPaint)
                canvas.drawCircle(speakerX + 40f, speakerY - 20f - gesture, 10f, subjectPaint)

                // Pants
                accentPaint.color = Color.rgb(15, 23, 42)
                canvas.drawRect(speakerX - 22f, speakerY + 30f, speakerX - 4f, speakerY + 95f, accentPaint)
                canvas.drawRect(speakerX + 4f, speakerY + 30f, speakerX + 22f, speakerY + 95f, accentPaint)

                canvas.drawText("Tech Keynote 2026 • AI Vision & Smart Framing", 40f, 60f, textPaint)
            }

            SampleVideoType.DYNAMIC_DANCE -> {
                // Studio backdrop
                bgPaint.color = Color.rgb(18, 12, 30)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Floor
                accentPaint.color = Color.rgb(35, 20, 55)
                canvas.drawRect(0f, height * 0.72f, width.toFloat(), height.toFloat(), accentPaint)

                // Dancer with dynamic jumps
                val danceX = width * (0.25f + 0.50f * (0.5f - 0.5f * sin(normTime * 4 * Math.PI.toFloat())))
                val leapY = -abs(sin(t * 3f)) * height * 0.20f
                val danceY = height * 0.58f + leapY

                // Dancer outfit (Vibrant Violet / Magenta)
                subjectPaint.color = Color.rgb(217, 70, 239)
                canvas.drawRoundRect(danceX - 26f, danceY - 60f, danceX + 26f, danceY + 20f, 12f, 12f, subjectPaint)

                // Head
                subjectPaint.color = Color.rgb(240, 185, 140)
                canvas.drawCircle(danceX, danceY - 82f, 20f, subjectPaint)

                // Dynamic limbs
                accentPaint.color = Color.rgb(168, 85, 247)
                canvas.drawRect(danceX - 20f, danceY + 20f, danceX - 4f, danceY + 75f, accentPaint)
                canvas.drawRect(danceX + 4f, danceY + 20f, danceX + 20f, danceY + 75f, accentPaint)

                canvas.drawText("Studio Dance & Movement • Dynamic Tracking Test", 40f, 60f, textPaint)
            }
        }
    }
}
