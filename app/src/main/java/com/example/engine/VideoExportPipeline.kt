package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import com.example.model.CropRect
import com.example.model.ExportedVideoItem
import com.example.model.FrameAnalysisResult
import com.example.model.ProcessingProgress
import com.example.model.ProcessingStage
import com.example.model.TrackingConfig
import com.example.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance Video Export Engine that renders smoothly reframed 9:16 vertical MP4 files
 * with hardware AVC / H.264 encoding and synchronized audio track passthrough.
 */
class VideoExportPipeline(private val context: Context) {

    private val cameraPlanner = CinematicCameraPlanner()

    /**
     * Renders and exports the 9:16 vertical video.
     */
    suspend fun exportVerticalVideo(
        videoInfo: VideoInfo,
        analysisResults: List<FrameAnalysisResult>,
        config: TrackingConfig,
        onProgress: (ProcessingProgress) -> Unit
    ): ExportedVideoItem? = withContext(Dispatchers.IO) {
        val outWidth = config.exportResolution.width
        val outHeight = config.exportResolution.height
        val fps = 30
        val bitRate = config.exportBitrate

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "AutoReframe").apply { mkdirs() }
        val finalOutputFile = File(exportDir, "Reframe_9_16_${timeStamp}.mp4")
        val tempVideoOnlyFile = File(context.cacheDir, "temp_video_track_${System.currentTimeMillis()}.mp4")

        onProgress(
            ProcessingProgress(
                stage = ProcessingStage.INITIALIZING,
                progress = 0.05f,
                statusMessage = "Configuring 9:16 Hardware Encoder (${outWidth}x${outHeight})..."
            )
        )

        val durationMs = max(1000L, videoInfo.durationMs)
        val totalFrames = ((durationMs / 1000.0) * fps).toInt().coerceAtLeast(1)
        val frameDurationUs = 1_000_000L / fps

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoInfo.uri)
        } catch (e: Exception) {
            onProgress(
                ProcessingProgress(
                    stage = ProcessingStage.ERROR,
                    statusMessage = "Failed to open video source: ${e.localizedMessage}"
                )
            )
            return@withContext null
        }

        // Configure Video Encoder
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mimeType, outWidth, outHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var videoTrackIdx = -1
        var muxerStarted = false

        val startTime = System.currentTimeMillis()
        val bitmapPaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        try {
            encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            // Temporary video-only muxer
            muxer = MediaMuxer(tempVideoOnlyFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            onProgress(
                ProcessingProgress(
                    stage = ProcessingStage.RENDERING_VIDEO,
                    progress = 0.10f,
                    statusMessage = "Rendering 9:16 vertical frames..."
                )
            )

            // Render loop frame-by-frame
            for (frameIdx in 0 until totalFrames) {
                if (!coroutineContext.isActive) {
                    tempVideoOnlyFile.delete()
                    return@withContext null
                }

                val frameTimeMs = min(durationMs, (frameIdx * 1000L) / fps)
                val frameTimeUs = frameTimeMs * 1000L

                // 1. Get Crop Window at this timestamp
                val crop = cameraPlanner.getCropAtTimestamp(frameTimeMs, analysisResults)

                // 2. Extract frame from source video
                val sourceBitmap = try {
                    retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(frameTimeUs)
                } catch (e: Exception) {
                    null
                }

                // 3. Draw cropped region to encoder surface
                val canvas = inputSurface.lockHardwareCanvas()
                canvas.drawColor(Color.BLACK)

                if (sourceBitmap != null) {
                    val srcW = sourceBitmap.width
                    val srcH = sourceBitmap.height

                    val cropPxLeft = (crop.left * srcW).toInt().coerceIn(0, srcW - 1)
                    val cropPxTop = (crop.top * srcH).toInt().coerceIn(0, srcH - 1)
                    val cropPxWidth = (crop.width * srcW).toInt().coerceIn(1, srcW - cropPxLeft)
                    val cropPxHeight = (crop.height * srcH).toInt().coerceIn(1, srcH - cropPxTop)

                    val srcRect = Rect(cropPxLeft, cropPxTop, cropPxLeft + cropPxWidth, cropPxTop + cropPxHeight)
                    val dstRect = Rect(0, 0, outWidth, outHeight)

                    canvas.drawBitmap(sourceBitmap, srcRect, dstRect, bitmapPaint)
                    sourceBitmap.recycle()
                } else {
                    // Fallback test pattern if frame couldn't be extracted
                    val placeholderPaint = Paint().apply {
                        color = Color.DKGRAY
                    }
                    canvas.drawRect(0f, 0f, outWidth.toFloat(), outHeight.toFloat(), placeholderPaint)
                }

                inputSurface.unlockCanvasAndPost(canvas)

                // 4. Drain encoder buffers
                var outputDone = false
                while (!outputDone) {
                    val status = encoder.dequeueOutputBuffer(bufferInfo, 2000L)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        outputDone = true
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            videoTrackIdx = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (status >= 0) {
                        val encodedBuffer = encoder.getOutputBuffer(status)
                        if (encodedBuffer != null && muxerStarted) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size != 0) {
                                bufferInfo.presentationTimeUs = frameIdx * frameDurationUs
                                muxer.writeSampleData(videoTrackIdx, encodedBuffer, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(status, false)
                    }
                }

                // Progress & ETA
                val renderProgress = 0.10f + 0.70f * ((frameIdx + 1).toFloat() / totalFrames.toFloat())
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (frameIdx + 1).toFloat() / totalFrames.toFloat()
                val estTotal = if (fraction > 0.03f) (elapsed / fraction).toLong() else 0L
                val remainingSec = max(0, ((estTotal - elapsed) / 1000).toInt())

                onProgress(
                    ProcessingProgress(
                        stage = ProcessingStage.RENDERING_VIDEO,
                        progress = renderProgress,
                        currentFrame = frameIdx + 1,
                        totalFrames = totalFrames,
                        statusMessage = "Reframing frame ${frameIdx + 1}/$totalFrames (9:16 vertical)",
                        estimatedSecondsLeft = remainingSec
                    )
                )
            }

            // Signal End of Stream
            encoder.signalEndOfInputStream()
            var eos = false
            while (!eos) {
                val status = encoder.dequeueOutputBuffer(bufferInfo, 5000L)
                if (status >= 0) {
                    val encodedBuffer = encoder.getOutputBuffer(status)
                    if (encodedBuffer != null && muxerStarted && bufferInfo.size != 0) {
                        muxer.writeSampleData(videoTrackIdx, encodedBuffer, bufferInfo)
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
                retriever.release()
            } catch (ignored: Exception) {}

            try {
                encoder?.stop()
                encoder?.release()
            } catch (ignored: Exception) {}

            try {
                if (muxerStarted) {
                    muxer?.stop()
                    muxer?.release()
                }
            } catch (ignored: Exception) {}
        }

        // 5. Audio Muxing Phase (Pass-through original audio track)
        onProgress(
            ProcessingProgress(
                stage = ProcessingStage.MUXING_AUDIO,
                progress = 0.85f,
                statusMessage = "Synchronizing original audio track..."
            )
        )

        val muxSuccess = muxAudioAndVideoTracks(
            context = context,
            sourceUri = videoInfo.uri,
            renderedVideoFile = tempVideoOnlyFile,
            finalOutputFile = finalOutputFile,
            durationMs = durationMs
        )

        tempVideoOnlyFile.delete()

        val finalFile = if (muxSuccess && finalOutputFile.exists() && finalOutputFile.length() > 0) {
            finalOutputFile
        } else {
            // Fallback to video-only file if muxing encountered format difference
            if (tempVideoOnlyFile.exists()) tempVideoOnlyFile else finalOutputFile
        }

        onProgress(
            ProcessingProgress(
                stage = ProcessingStage.COMPLETED,
                progress = 1.0f,
                statusMessage = "Export complete!"
            )
        )

        val formattedSize = formatFileSize(finalFile.length())

        ExportedVideoItem(
            id = "export_${System.currentTimeMillis()}",
            title = "9:16 Vertical - ${videoInfo.title}",
            fileUri = Uri.fromFile(finalFile),
            filePath = finalFile.absolutePath,
            durationMs = durationMs,
            width = outWidth,
            height = outHeight,
            fileSizeFormatted = formattedSize
        )
    }

    /**
     * Muxes the newly rendered 9:16 video track together with the original audio track.
     */
    private fun muxAudioAndVideoTracks(
        context: Context,
        sourceUri: Uri,
        renderedVideoFile: File,
        finalOutputFile: File,
        durationMs: Long
    ): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        return try {
            videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(renderedVideoFile.absolutePath)

            var videoTrackIndex = -1
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    break
                }
            }

            if (videoTrackIndex < 0) {
                // If video extractor failed, just copy file
                renderedVideoFile.copyTo(finalOutputFile, overwrite = true)
                return true
            }

            videoExtractor.selectTrack(videoTrackIndex)
            val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)

            // Setup audio extractor from source
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            try {
                audioExtractor = MediaExtractor()
                audioExtractor.setDataSource(context, sourceUri, null)

                for (i in 0 until audioExtractor.trackCount) {
                    val format = audioExtractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioFormat = format
                        break
                    }
                }
                if (audioTrackIndex >= 0) {
                    audioExtractor.selectTrack(audioTrackIndex)
                }
            } catch (e: Exception) {
                audioTrackIndex = -1
            }

            muxer = MediaMuxer(finalOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = if (audioTrackIndex >= 0 && audioFormat != null) {
                muxer.addTrack(audioFormat)
            } else -1

            muxer.start()

            // Copy Video Samples
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = videoExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = videoExtractor.sampleFlags

                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Copy Audio Samples if available
            if (audioTrackIndex >= 0 && muxerAudioTrack >= 0 && audioExtractor != null) {
                val maxAudioTimeUs = durationMs * 1000L
                while (true) {
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTime = audioExtractor.sampleTime
                    if (sampleTime > maxAudioTimeUs) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTime
                    bufferInfo.flags = audioExtractor.sampleFlags

                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    audioExtractor.advance()
                }
            }

            true
        } catch (e: Exception) {
            // Fallback: If muxing audio failed, copy video-only file
            try {
                renderedVideoFile.copyTo(finalOutputFile, overwrite = true)
                true
            } catch (copyEx: Exception) {
                false
            }
        } finally {
            try { videoExtractor?.release() } catch (ignored: Exception) {}
            try { audioExtractor?.release() } catch (ignored: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
