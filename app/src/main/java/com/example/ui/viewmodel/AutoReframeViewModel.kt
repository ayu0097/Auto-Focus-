package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.CinematicCameraPlanner
import com.example.engine.ComputerVisionTracker
import com.example.engine.SampleVideoGenerator
import com.example.engine.SampleVideoType
import com.example.engine.VideoExportPipeline
import com.example.model.CropRect
import com.example.model.DetectedSubject
import com.example.model.ExportedVideoItem
import com.example.model.FrameAnalysisResult
import com.example.model.ProcessingProgress
import com.example.model.ProcessingStage
import com.example.model.TrackingConfig
import com.example.model.TrackingPreset
import com.example.model.VideoInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class Screen {
    HOME,
    EDITOR,
    EXPORT_SUCCESS,
    GALLERY
}

enum class PreviewMode {
    FULL_FRAME_DIRECTOR,
    CROPPED_9_16
}

data class UiState(
    val currentScreen: Screen = Screen.HOME,
    val selectedVideo: VideoInfo? = null,
    val trackingConfig: TrackingConfig = TrackingConfig(),
    val analysisResults: List<FrameAnalysisResult> = emptyList(),
    val isAnalyzing: Boolean = false,
    val isExporting: Boolean = false,
    val isGeneratingSample: Boolean = false,
    val progress: ProcessingProgress = ProcessingProgress(),
    val playbackPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val previewMode: PreviewMode = PreviewMode.FULL_FRAME_DIRECTOR,
    val currentCrop: CropRect = CropRect.DEFAULT,
    val currentPrimarySubject: DetectedSubject? = null,
    val currentSubjects: List<DetectedSubject> = emptyList(),
    val manualAnchors: Map<Long, PointF> = emptyMap(),
    val exportedVideos: List<ExportedVideoItem> = emptyList(),
    val latestExport: ExportedVideoItem? = null,
    val errorMessage: String? = null
)

class AutoReframeViewModel(application: Application) : AndroidViewModel(application) {

    private val cvTracker = ComputerVisionTracker(application)
    private val cameraPlanner = CinematicCameraPlanner()
    private val exportPipeline = VideoExportPipeline(application)
    private val sampleGenerator = SampleVideoGenerator(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null
    private var exportJob: Job? = null
    private var playbackJob: Job? = null

    init {
        loadSavedExports()
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
        if (screen != Screen.EDITOR) {
            pausePlayback()
        }
    }

    fun selectVideoUri(uri: Uri) {
        viewModelScope.launch {
            val videoInfo = cvTracker.extractVideoInfo(uri)
            _uiState.update {
                it.copy(
                    selectedVideo = videoInfo,
                    currentScreen = Screen.EDITOR,
                    analysisResults = emptyList(),
                    manualAnchors = emptyMap(),
                    playbackPositionMs = 0L,
                    isPlaying = false
                )
            }
            startAnalysis(videoInfo)
        }
    }

    fun loadSampleVideo(type: SampleVideoType) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingSample = true,
                    progress = ProcessingProgress(
                        stage = ProcessingStage.INITIALIZING,
                        statusMessage = "Preparing ${type.title} sample..."
                    )
                )
            }

            try {
                val file = sampleGenerator.generateSampleVideo(type) { p ->
                    _uiState.update {
                        it.copy(
                            progress = ProcessingProgress(
                                stage = ProcessingStage.INITIALIZING,
                                progress = p,
                                statusMessage = "Generating ${type.title} (${(p * 100).toInt()}%)..."
                            )
                        )
                    }
                }

                val uri = Uri.fromFile(file)
                val videoInfo = cvTracker.extractVideoInfo(uri).copy(title = type.title)
                _uiState.update {
                    it.copy(
                        isGeneratingSample = false,
                        selectedVideo = videoInfo,
                        currentScreen = Screen.EDITOR,
                        analysisResults = emptyList(),
                        manualAnchors = emptyMap(),
                        playbackPositionMs = 0L,
                        isPlaying = false
                    )
                }
                startAnalysis(videoInfo)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingSample = false,
                        errorMessage = "Failed to load sample: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun startAnalysis(videoInfo: VideoInfo? = _uiState.value.selectedVideo) {
        if (videoInfo == null) return

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    progress = ProcessingProgress(
                        stage = ProcessingStage.INITIALIZING,
                        statusMessage = "Starting AI subject detection..."
                    )
                )
            }

            val results = cvTracker.analyzeVideo(
                videoInfo = videoInfo,
                config = _uiState.value.trackingConfig,
                manualAnchors = _uiState.value.manualAnchors
            ) { progressUpdate ->
                _uiState.update { it.copy(progress = progressUpdate) }
            }

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    analysisResults = results
                )
            }

            updateCurrentFrameState(_uiState.value.playbackPositionMs)
            startPlaybackLoop()
        }
    }

    fun updateTrackingConfig(newConfig: TrackingConfig) {
        _uiState.update { it.copy(trackingConfig = newConfig) }

        // Recalculate camera smoothing immediately without full re-analysis
        val currentResults = _uiState.value.analysisResults
        if (currentResults.isNotEmpty()) {
            viewModelScope.launch {
                val reSmoothed = cameraPlanner.smoothCameraTrajectory(
                    rawResults = currentResults,
                    config = newConfig,
                    manualAnchors = _uiState.value.manualAnchors
                )
                _uiState.update { it.copy(analysisResults = reSmoothed) }
                updateCurrentFrameState(_uiState.value.playbackPositionMs)
            }
        }
    }

    fun applyPreset(preset: TrackingPreset) {
        val currentConfig = _uiState.value.trackingConfig
        val updated = currentConfig.copy(
            smoothness = preset.smoothness,
            deadZone = preset.deadZone,
            zoomFactor = preset.zoomFactor
        )
        updateTrackingConfig(updated)
    }

    fun setPreviewMode(mode: PreviewMode) {
        _uiState.update { it.copy(previewMode = mode) }
    }

    fun setManualAnchorAtCurrentTime(normalizedPoint: PointF) {
        val currentPos = _uiState.value.playbackPositionMs
        val updatedAnchors = _uiState.value.manualAnchors.toMutableMap()
        // Snap to nearest 100ms
        val snappedTime = (currentPos / 100L) * 100L
        updatedAnchors[snappedTime] = normalizedPoint

        _uiState.update { it.copy(manualAnchors = updatedAnchors) }

        // Re-smooth trajectory with new anchor
        val currentResults = _uiState.value.analysisResults
        if (currentResults.isNotEmpty()) {
            val video = _uiState.value.selectedVideo ?: return
            val config = _uiState.value.trackingConfig

            val updatedRaw = currentResults.map { frame ->
                val anchor = updatedAnchors[frame.timestampMs]
                val (rawCrop, primary) = cameraPlanner.computeRawTargetCrop(
                    frameTimeMs = frame.timestampMs,
                    videoInfo = video,
                    subjects = frame.subjects,
                    manualAnchor = anchor,
                    config = config
                )
                frame.copy(rawCrop = rawCrop, primarySubject = primary)
            }

            val reSmoothed = cameraPlanner.smoothCameraTrajectory(
                rawResults = updatedRaw,
                config = config,
                manualAnchors = updatedAnchors
            )

            _uiState.update { it.copy(analysisResults = reSmoothed) }
            updateCurrentFrameState(currentPos)
        }
    }

    fun clearManualAnchorAtCurrentTime() {
        val currentPos = _uiState.value.playbackPositionMs
        val updatedAnchors = _uiState.value.manualAnchors.toMutableMap()
        val nearestKey = updatedAnchors.keys.minByOrNull { kotlin.math.abs(it - currentPos) }
        if (nearestKey != null && kotlin.math.abs(nearestKey - currentPos) < 500L) {
            updatedAnchors.remove(nearestKey)
            _uiState.update { it.copy(manualAnchors = updatedAnchors) }
            updateTrackingConfig(_uiState.value.trackingConfig)
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        _uiState.update { it.copy(isPlaying = true) }
        startPlaybackLoop()
    }

    fun pausePlayback() {
        _uiState.update { it.copy(isPlaying = false) }
        playbackJob?.cancel()
    }

    fun seekTo(positionMs: Long) {
        val duration = _uiState.value.selectedVideo?.durationMs ?: 5000L
        val clamped = positionMs.coerceIn(0L, duration)
        _uiState.update { it.copy(playbackPositionMs = clamped) }
        updateCurrentFrameState(clamped)
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val updateInterval = 33L // ~30 fps
            while (isActive && _uiState.value.isPlaying) {
                val duration = _uiState.value.selectedVideo?.durationMs ?: 5000L
                var nextPos = _uiState.value.playbackPositionMs + updateInterval
                if (nextPos >= duration) {
                    nextPos = 0L // Loop playback
                }
                _uiState.update { it.copy(playbackPositionMs = nextPos) }
                updateCurrentFrameState(nextPos)
                delay(updateInterval)
            }
        }
    }

    private fun updateCurrentFrameState(timestampMs: Long) {
        val results = _uiState.value.analysisResults
        if (results.isEmpty()) return

        val crop = cameraPlanner.getCropAtTimestamp(timestampMs, results)

        // Find nearest frame analysis for subjects
        val nearestFrame = results.minByOrNull { kotlin.math.abs(it.timestampMs - timestampMs) }

        _uiState.update {
            it.copy(
                currentCrop = crop,
                currentPrimarySubject = nearestFrame?.primarySubject,
                currentSubjects = nearestFrame?.subjects ?: emptyList()
            )
        }
    }

    fun startExport() {
        val video = _uiState.value.selectedVideo ?: return
        val results = _uiState.value.analysisResults
        if (results.isEmpty()) return

        pausePlayback()
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    progress = ProcessingProgress(
                        stage = ProcessingStage.INITIALIZING,
                        statusMessage = "Starting 9:16 video export..."
                    )
                )
            }

            val exportedItem = exportPipeline.exportVerticalVideo(
                videoInfo = video,
                analysisResults = results,
                config = _uiState.value.trackingConfig
            ) { prog ->
                _uiState.update { it.copy(progress = prog) }
            }

            if (exportedItem != null) {
                val updatedList = listOf(exportedItem) + _uiState.value.exportedVideos
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        latestExport = exportedItem,
                        exportedVideos = updatedList,
                        currentScreen = Screen.EXPORT_SUCCESS
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export was cancelled or encountered an error."
                    )
                }
            }
        }
    }

    fun cancelProcessing() {
        analysisJob?.cancel()
        exportJob?.cancel()
        _uiState.update {
            it.copy(
                isAnalyzing = false,
                isExporting = false,
                isGeneratingSample = false,
                progress = ProcessingProgress()
            )
        }
    }

    fun saveExportToGallery(item: ExportedVideoItem, context: Context) {
        viewModelScope.launch {
            try {
                val file = File(item.filePath)
                if (!file.exists()) {
                    Toast.makeText(context, "Export file not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AutoReframe")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val itemUri = resolver.insert(collection, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { outStream ->
                        file.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(itemUri, values, null, null)
                    }

                    Toast.makeText(context, "Saved to device Movies/AutoReframe!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareExport(item: ExportedVideoItem, context: Context) {
        try {
            val file = File(item.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share 9:16 Vertical Video"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteExport(item: ExportedVideoItem) {
        File(item.filePath).delete()
        _uiState.update {
            it.copy(exportedVideos = it.exportedVideos.filter { v -> v.id != item.id })
        }
    }

    private fun loadSavedExports() {
        viewModelScope.launch {
            val exportDir = File(getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_MOVIES), "AutoReframe")
            if (exportDir.exists() && exportDir.isDirectory) {
                val files = exportDir.listFiles { file -> file.extension.equals("mp4", ignoreCase = true) } ?: emptyArray()
                val list = files.sortedByDescending { it.lastModified() }.map { file ->
                    val mb = file.length().toDouble() / (1024.0 * 1024.0)
                    ExportedVideoItem(
                        id = file.name,
                        title = file.nameWithoutExtension.replace('_', ' '),
                        fileUri = Uri.fromFile(file),
                        filePath = file.absolutePath,
                        durationMs = 5000L,
                        width = 1080,
                        height = 1920,
                        fileSizeFormatted = String.format("%.1f MB", mb),
                        createdAt = file.lastModified()
                    )
                }
                _uiState.update { it.copy(exportedVideos = list) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
