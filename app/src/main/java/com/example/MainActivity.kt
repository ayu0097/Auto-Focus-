package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.ProcessingOverlay
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExportSuccessScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AutoReframeViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: AutoReframeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(state.errorMessage) {
                    state.errorMessage?.let { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (state.currentScreen) {
                        Screen.HOME -> HomeScreen(
                            onSelectVideo = { uri -> viewModel.selectVideoUri(uri) },
                            onLoadSample = { sampleType -> viewModel.loadSampleVideo(sampleType) },
                            onOpenGallery = { viewModel.navigateTo(Screen.GALLERY) },
                            recentExports = state.exportedVideos,
                            onPlayExport = { exportItem ->
                                viewModel.selectVideoUri(exportItem.fileUri)
                            },
                            onShareExport = { exportItem ->
                                viewModel.shareExport(exportItem, context)
                            }
                        )

                        Screen.EDITOR -> EditorScreen(
                            state = state,
                            onBack = { viewModel.navigateTo(Screen.HOME) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSeek = { pos -> viewModel.seekTo(pos) },
                            onSetPreviewMode = { mode -> viewModel.setPreviewMode(mode) },
                            onUpdateConfig = { cfg -> viewModel.updateTrackingConfig(cfg) },
                            onApplyPreset = { preset -> viewModel.applyPreset(preset) },
                            onSetManualAnchor = { point -> viewModel.setManualAnchorAtCurrentTime(point) },
                            onClearManualAnchor = { viewModel.clearManualAnchorAtCurrentTime() },
                            onReAnalyze = { viewModel.startAnalysis() },
                            onStartExport = { viewModel.startExport() }
                        )

                        Screen.EXPORT_SUCCESS -> ExportSuccessScreen(
                            state = state,
                            onBackToEditor = { viewModel.navigateTo(Screen.EDITOR) },
                            onHome = { viewModel.navigateTo(Screen.HOME) },
                            onSaveToGallery = { exportItem ->
                                viewModel.saveExportToGallery(exportItem, context)
                            },
                            onShare = { exportItem ->
                                viewModel.shareExport(exportItem, context)
                            }
                        )

                        Screen.GALLERY -> GalleryScreen(
                            exports = state.exportedVideos,
                            onBack = { viewModel.navigateTo(Screen.HOME) },
                            onPlay = { exportItem ->
                                viewModel.selectVideoUri(exportItem.fileUri)
                            },
                            onShare = { exportItem ->
                                viewModel.shareExport(exportItem, context)
                            },
                            onDelete = { exportItem ->
                                viewModel.deleteExport(exportItem)
                            }
                        )
                    }

                    // Processing Overlay Modal
                    if (state.isAnalyzing || state.isExporting || state.isGeneratingSample) {
                        ProcessingOverlay(
                            progress = state.progress,
                            onCancel = { viewModel.cancelProcessing() }
                        )
                    }
                }
            }
        }
    }
}

