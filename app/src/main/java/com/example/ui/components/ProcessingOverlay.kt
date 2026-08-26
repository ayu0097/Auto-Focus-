package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ProcessingProgress
import com.example.model.ProcessingStage
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseRecord
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAI

@Composable
fun ProcessingOverlay(
    progress: ProcessingProgress,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarScale"
    )

    Dialog(
        onDismissRequest = { /* Modal, cannot dismiss by clicking outside */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Brush.linearGradient(listOf(CyanNeon, VioletAI)), RoundedCornerShape(24.dp))
                .testTag("processing_dialog"),
            color = DarkSurface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Radar / Processing Animation Visual
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(1.5.dp, DarkBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer rotating cyan ring
                    CircularProgressIndicator(
                        progress = { progress.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .size(90.dp)
                            .rotate(rotation),
                        color = CyanNeon,
                        strokeWidth = 3.5.dp,
                        trackColor = Color(0x2200F0FF),
                        strokeCap = StrokeCap.Round
                    )

                    // Center icon
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = "Processing Video",
                        modifier = Modifier.size(36.dp),
                        tint = VioletAI
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stage Title
                Text(
                    text = progress.stage.stageTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status message
                Text(
                    text = progress.statusMessage,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CyanNeon,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Percentage and Frame count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress.progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )

                    if (progress.totalFrames > 0) {
                        Text(
                            text = "Frame ${progress.currentFrame} / ${progress.totalFrames}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    if (progress.estimatedSecondsLeft > 0) {
                        Text(
                            text = "~${progress.estimatedSecondsLeft}s left",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step status timeline indicators
                StageStepRow(
                    title = "AI Subject & Motion Detect",
                    isComplete = progress.stage.ordinal > ProcessingStage.DETECTING_SUBJECTS.ordinal,
                    isActive = progress.stage == ProcessingStage.DETECTING_SUBJECTS || progress.stage == ProcessingStage.EXTRACTING_FRAMES
                )
                Spacer(modifier = Modifier.height(6.dp))
                StageStepRow(
                    title = "Cinematic 9:16 Framing & Smooth",
                    isComplete = progress.stage.ordinal > ProcessingStage.SMOOTHING_CAMERA.ordinal,
                    isActive = progress.stage == ProcessingStage.SMOOTHING_CAMERA
                )
                Spacer(modifier = Modifier.height(6.dp))
                StageStepRow(
                    title = "Hardware Video Render & Audio Mux",
                    isComplete = progress.stage.ordinal > ProcessingStage.MUXING_AUDIO.ordinal,
                    isActive = progress.stage == ProcessingStage.RENDERING_VIDEO || progress.stage == ProcessingStage.MUXING_AUDIO
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel Button
                if (progress.isCancellable) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cancel_processing_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RoseRecord
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(RoseRecord.copy(alpha = 0.5f), RoseRecord.copy(alpha = 0.5f)))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Processing", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StageStepRow(
    title: String,
    isComplete: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconColor = when {
            isComplete -> EmeraldSuccess
            isActive -> CyanNeon
            else -> DarkBorder
        }

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconColor
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            color = when {
                isComplete -> TextPrimary
                isActive -> CyanNeon
                else -> TextMuted
            },
            fontWeight = if (isActive || isComplete) FontWeight.Medium else FontWeight.Normal
        )
    }
}
