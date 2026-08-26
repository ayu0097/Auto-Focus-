package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SampleVideoType
import com.example.model.ExportedVideoItem
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAI
import com.example.ui.theme.VioletAILight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSelectVideo: (Uri) -> Unit,
    onLoadSample: (SampleVideoType) -> Unit,
    onOpenGallery: () -> Unit,
    recentExports: List<ExportedVideoItem>,
    onPlayExport: (ExportedVideoItem) -> Unit,
    onShareExport: (ExportedVideoItem) -> Unit
) {
    val context = LocalContext.current
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectVideo(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(CyanNeon, VioletAI))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "AutoReframe Icon",
                                tint = DarkBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AutoReframe",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    letterSpacing = 0.2.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyanNeonSubtle)
                                        .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "9:16 AI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanNeon
                                    )
                                }
                            }
                            Text(
                                text = "Landscape ➔ Viral Vertical Shorts",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    if (recentExports.isNotEmpty()) {
                        IconButton(
                            onClick = onOpenGallery,
                            modifier = Modifier.testTag("open_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = "View Saved Exports",
                                tint = CyanNeon
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Hero Action Banner: Select Video
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(CyanNeon, VioletAI, IndigoTrack)),
                            RoundedCornerShape(26.dp)
                        )
                        .clickable { videoPickerLauncher.launch("video/*") }
                        .testTag("select_video_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Ambient Glowing Icon Box
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CyanNeonGlow, Color(0x10A855F7)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Import Video",
                                modifier = Modifier.size(40.dp),
                                tint = CyanNeon
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Import Any Landscape Video",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "AI automatically detects subjects, faces & action to create cinema-quality 9:16 vertical shorts for TikTok, Reels & Shorts.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Features Badges Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BadgePill("1080p HD")
                            Spacer(modifier = Modifier.width(8.dp))
                            BadgePill("Synced Audio")
                            Spacer(modifier = Modifier.width(8.dp))
                            BadgePill("AI Motion Lock")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("select_video_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = DarkBackground
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Choose Video to Reframe",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Quick 3-Step Guide Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WorkflowStep(number = "1", title = "Import 16:9")
                    Text("➔", color = TextMuted, fontSize = 14.sp)
                    WorkflowStep(number = "2", title = "AI Tracks")
                    Text("➔", color = TextMuted, fontSize = 14.sp)
                    WorkflowStep(number = "3", title = "Export 9:16")
                }
            }

            // 1-Click Interactive Demo Samples
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = VioletAILight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Try Instant Demo Clips",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "1-Click Test",
                            fontSize = 12.sp,
                            color = CyanNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(end = 6.dp)
                    ) {
                        items(SampleVideoType.values()) { sample ->
                            SampleClipCard(
                                sample = sample,
                                onClick = { onLoadSample(sample) }
                            )
                        }
                    }
                }
            }

            // Core AI Capabilities
            item {
                Column {
                    Text(
                        text = "Intelligent Studio Features",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FeatureGridItem(
                        icon = Icons.Default.Face,
                        title = "Smart Face & Presenter Focus",
                        description = "Automatically locks onto speaker faces with professional rule-of-thirds headroom bias",
                        accentColor = CyanNeon
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureGridItem(
                        icon = Icons.Default.DirectionsRun,
                        title = "Dynamic Motion Centroid Tracking",
                        description = "Follows rapid action of skaters, dancers, athletes and performers smoothly across frame",
                        accentColor = VioletAI
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureGridItem(
                        icon = Icons.Default.SlowMotionVideo,
                        title = "Cinematic Damped Camera",
                        description = "Simulates high-end steadycam operators with dead-zone stabilization to eliminate jitter",
                        accentColor = EmeraldSuccess
                    )
                }
            }

            // Recent Exports Carousel
            if (recentExports.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent 9:16 Exports (${recentExports.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "View All",
                                fontSize = 13.sp,
                                color = CyanNeon,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onOpenGallery() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        recentExports.take(3).forEach { item ->
                            RecentExportItemRow(
                                item = item,
                                onPlay = { onPlayExport(item) },
                                onShare = { onShareExport(item) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BadgePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

@Composable
private fun WorkflowStep(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(CyanNeonSubtle)
                .border(1.dp, CyanNeon, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun SampleClipCard(
    sample: SampleVideoType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("sample_clip_${sample.name.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (sample) {
                            SampleVideoType.SKATER_ACTION -> Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                            SampleVideoType.KEYNOTE_SPEAKER -> Brush.linearGradient(listOf(Color(0xFF0A0F1D), Color(0xFF1E1B4B)))
                            SampleVideoType.DYNAMIC_DANCE -> Brush.linearGradient(listOf(Color(0xFF180C1E), Color(0xFF3B0764)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (sample) {
                        SampleVideoType.SKATER_ACTION -> Icons.Default.DirectionsRun
                        SampleVideoType.KEYNOTE_SPEAKER -> Icons.Default.RecordVoiceOver
                        SampleVideoType.DYNAMIC_DANCE -> Icons.Default.AutoAwesome
                    },
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = when (sample) {
                        SampleVideoType.SKATER_ACTION -> CyanNeon
                        SampleVideoType.KEYNOTE_SPEAKER -> IndigoTrack
                        SampleVideoType.DYNAMIC_DANCE -> VioletAI
                    }
                )

                // 16:9 Source Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("16:9 • ${sample.durationSeconds}s", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = sample.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = sample.subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun FeatureGridItem(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RecentExportItemRow(
    item: ExportedVideoItem,
    onPlay: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .clickable(onClick = onPlay),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CyanNeonSubtle)
                .border(1.dp, CyanNeon.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.width}x${item.height} (9:16) • ${item.fileSizeFormatted}",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        IconButton(
            onClick = onPlay,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = CyanNeon
            )
        }

        IconButton(
            onClick = onShare,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = TextSecondary
            )
        }
    }
}
