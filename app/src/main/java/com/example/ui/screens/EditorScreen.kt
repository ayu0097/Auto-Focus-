package com.example.ui.screens

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExportResolution
import com.example.model.TrackingConfig
import com.example.model.TrackingMode
import com.example.model.TrackingPreset
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.AmberMotion
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonGlow
import com.example.ui.theme.CyanNeonSubtle
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoTrack
import com.example.ui.theme.RoseRecord
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAI
import com.example.ui.theme.VioletAILight
import com.example.ui.viewmodel.PreviewMode
import com.example.ui.viewmodel.UiState
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: UiState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetPreviewMode: (PreviewMode) -> Unit,
    onUpdateConfig: (TrackingConfig) -> Unit,
    onApplyPreset: (TrackingPreset) -> Unit,
    onSetManualAnchor: (PointF) -> Unit,
    onClearManualAnchor: () -> Unit,
    onReAnalyze: () -> Unit,
    onStartExport: () -> Unit
) {
    val video = state.selectedVideo ?: return
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = video.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${video.displayWidth}x${video.displayHeight} (${if (video.isLandscape) "16:9" else "Source"})",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = " ➔ 9:16 Vertical",
                                fontSize = 11.sp,
                                color = CyanNeon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = onStartExport,
                        modifier = Modifier
                            .height(38.dp)
                            .padding(end = 8.dp)
                            .testTag("export_9_16_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = DarkBackground
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export 9:16",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // View Mode Selector Segmented Pill (Director vs 9:16 Output Preview)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Director View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (state.previewMode == PreviewMode.FULL_FRAME_DIRECTOR) CyanNeon else Color.Transparent)
                        .clickable { onSetPreviewMode(PreviewMode.FULL_FRAME_DIRECTOR) }
                        .testTag("mode_director_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = null,
                            tint = if (state.previewMode == PreviewMode.FULL_FRAME_DIRECTOR) DarkBackground else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Director (Full Frame)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.previewMode == PreviewMode.FULL_FRAME_DIRECTOR) DarkBackground else TextSecondary
                        )
                    }
                }

                // 9:16 Output Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (state.previewMode == PreviewMode.CROPPED_9_16) CyanNeon else Color.Transparent)
                        .clickable { onSetPreviewMode(PreviewMode.CROPPED_9_16) }
                        .testTag("mode_preview_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CropRotate,
                            contentDescription = null,
                            tint = if (state.previewMode == PreviewMode.CROPPED_9_16) DarkBackground else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "9:16 Output Preview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.previewMode == PreviewMode.CROPPED_9_16) DarkBackground else TextSecondary
                        )
                    }
                }
            }

            // Video Player Viewport Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(if (state.previewMode == PreviewMode.FULL_FRAME_DIRECTOR) 245.dp else 315.dp)
            ) {
                VideoPlayerView(
                    videoInfo = video,
                    playbackPositionMs = state.playbackPositionMs,
                    isPlaying = state.isPlaying,
                    previewMode = state.previewMode,
                    currentCrop = state.currentCrop,
                    subjects = state.currentSubjects,
                    primarySubject = state.currentPrimarySubject,
                    manualAnchor = state.manualAnchors[state.playbackPositionMs],
                    onSetManualAnchor = onSetManualAnchor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Timeline Controls Bar
            TimelineControlsBar(
                durationMs = video.durationMs,
                currentPositionMs = state.playbackPositionMs,
                isPlaying = state.isPlaying,
                manualAnchors = state.manualAnchors,
                onTogglePlayPause = onTogglePlayPause,
                onSeek = onSeek,
                onStepBack = { onSeek(max(0L, state.playbackPositionMs - 1000L)) },
                onStepForward = { onSeek(min(video.durationMs, state.playbackPositionMs + 1000L)) },
                onClearPin = onClearManualAnchor
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1-Touch Instant Presets Carousel
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1-Touch Camera Presets",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Instant Tuning",
                        fontSize = 11.sp,
                        color = CyanNeon
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(TrackingPreset.values()) { preset ->
                        val isSelected = state.trackingConfig.smoothness == preset.smoothness
                        FilterChip(
                            selected = isSelected,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(preset.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (preset) {
                                        TrackingPreset.CINEMATIC -> Icons.Default.AutoAwesome
                                        TrackingPreset.ACTION_DYNAMIC -> Icons.Default.DirectionsRun
                                        TrackingPreset.SNAPPY -> Icons.Default.MotionPhotosOn
                                        TrackingPreset.LOCKED -> Icons.Default.Lock
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary,
                                selectedContainerColor = CyanNeonSubtle,
                                selectedLabelColor = CyanNeon,
                                selectedLeadingIconColor = CyanNeon
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DarkBorder,
                                selectedBorderColor = CyanNeon
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control Tabs (AI Tracking / Framing & Camera / Export Quality)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = CyanNeon,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanNeon
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("AI Tracking", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Camera & Scale", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Quality", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Panels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> TrackingTabContent(
                        config = state.trackingConfig,
                        onUpdateConfig = onUpdateConfig,
                        onReAnalyze = onReAnalyze
                    )
                    1 -> FramingTabContent(
                        config = state.trackingConfig,
                        onUpdateConfig = onUpdateConfig
                    )
                    2 -> QualityTabContent(
                        config = state.trackingConfig,
                        onUpdateConfig = onUpdateConfig
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TimelineControlsBar(
    durationMs: Long,
    currentPositionMs: Long,
    isPlaying: Boolean,
    manualAnchors: Map<Long, PointF>,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onClearPin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, DarkBorder)))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Timecode & Pin state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasPinHere = manualAnchors.keys.any { kotlin.math.abs(it - currentPositionMs) < 500L }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimecode(currentPositionMs),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                    Text(
                        text = " / " + formatTimecode(durationMs),
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                if (hasPinHere) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RoseRecord.copy(alpha = 0.2f))
                            .clickable(onClick = onClearPin)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = RoseRecord, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove Pin", fontSize = 11.sp, color = RoseRecord, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Tap frame to pin track",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Slider
            val maxDur = max(1000L, durationMs).toFloat()
            Slider(
                value = currentPositionMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..maxDur,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeline_scrub_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = CyanNeon,
                    activeTrackColor = CyanNeon,
                    inactiveTrackColor = DarkSurfaceElevated
                )
            )

            // Playback buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onStepBack, modifier = Modifier.testTag("step_back_button")) {
                    Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Step Back 1s", tint = TextSecondary)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(CyanNeon)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = DarkBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = onStepForward, modifier = Modifier.testTag("step_forward_button")) {
                    Icon(imageVector = Icons.Default.FastForward, contentDescription = "Step Forward 1s", tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun TrackingTabContent(
    config: TrackingConfig,
    onUpdateConfig: (TrackingConfig) -> Unit,
    onReAnalyze: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("AI Tracking Priority Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackingMode.values().forEach { mode ->
                val isSelected = config.mode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceElevated else DarkSurface)
                        .border(1.dp, if (isSelected) CyanNeon else DarkBorder, RoundedCornerShape(12.dp))
                        .clickable { onUpdateConfig(config.copy(mode = mode)) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (mode) {
                            TrackingMode.SMART_AI -> Icons.Default.AutoAwesome
                            TrackingMode.FACE_PRIORITY -> Icons.Default.Face
                            TrackingMode.ACTION_MOTION -> Icons.Default.DirectionsRun
                            TrackingMode.MANUAL_KEYFRAMES -> Icons.Default.FilterCenterFocus
                        },
                        contentDescription = null,
                        tint = if (isSelected) CyanNeon else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                        Text(
                            text = mode.description,
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Button(
            onClick = onReAnalyze,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("reanalyze_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkSurfaceElevated,
                contentColor = CyanNeon
            )
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Re-Run AI Computer Vision Track", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FramingTabContent(
    config: TrackingConfig,
    onUpdateConfig: (TrackingConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Smoothness Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Camera Pan Smoothness", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${(config.smoothness * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
            }
            Text("Higher values create gentle, damped cinematic pans. Lower values react faster to sudden movements.", fontSize = 11.sp, color = TextMuted)
            Slider(
                value = config.smoothness,
                onValueChange = { onUpdateConfig(config.copy(smoothness = it)) },
                valueRange = 0.05f..0.95f,
                colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon, inactiveTrackColor = DarkSurfaceElevated)
            )
        }

        // Dead-zone Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dead-Zone Stabilization Threshold", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${(config.deadZone * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
            }
            Text("Prevents minor body micro-movements from causing distracting camera jitter.", fontSize = 11.sp, color = TextMuted)
            Slider(
                value = config.deadZone,
                onValueChange = { onUpdateConfig(config.copy(deadZone = it)) },
                valueRange = 0.01f..0.15f,
                colors = SliderDefaults.colors(thumbColor = VioletAI, activeTrackColor = VioletAI, inactiveTrackColor = DarkSurfaceElevated)
            )
        }

        // Framing Zoom Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Crop Framing Scale (Zoom)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(String.format("%.2fx", config.zoomFactor), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
            }
            Slider(
                value = config.zoomFactor,
                onValueChange = { onUpdateConfig(config.copy(zoomFactor = it)) },
                valueRange = 0.85f..1.30f,
                colors = SliderDefaults.colors(thumbColor = IndigoTrack, activeTrackColor = IndigoTrack, inactiveTrackColor = DarkSurfaceElevated)
            )
        }

        // Headroom Offset
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Vertical Headroom Bias", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(if (config.verticalHeadroom > 0) "+${(config.verticalHeadroom * 100).toInt()}% (Upper 1/3)" else "${(config.verticalHeadroom * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
            }
            Slider(
                value = config.verticalHeadroom,
                onValueChange = { onUpdateConfig(config.copy(verticalHeadroom = it)) },
                valueRange = -0.15f..0.20f,
                colors = SliderDefaults.colors(thumbColor = EmeraldSuccess, activeTrackColor = EmeraldSuccess, inactiveTrackColor = DarkSurfaceElevated)
            )
        }
    }
}

@Composable
private fun QualityTabContent(
    config: TrackingConfig,
    onUpdateConfig: (TrackingConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Export Resolution (9:16 Vertical)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportResolution.values().forEach { res ->
                val isSelected = config.exportResolution == res
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceElevated else DarkSurface)
                        .border(1.dp, if (isSelected) CyanNeon else DarkBorder, RoundedCornerShape(12.dp))
                        .clickable { onUpdateConfig(config.copy(exportResolution = res, exportBitrate = res.defaultBitrate)) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = if (isSelected) CyanNeon else TextMuted
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = res.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                        Text(
                            text = "Bitrate: ${res.defaultBitrate / 1_000_000} Mbps • Perfect for TikTok/Reels/Shorts",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", minutes, seconds, tenths)
}
