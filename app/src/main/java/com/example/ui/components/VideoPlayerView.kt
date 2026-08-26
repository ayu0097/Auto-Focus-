package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.PointF
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CropRect
import com.example.model.DetectedSubject
import com.example.model.SubjectType
import com.example.model.VideoInfo
import com.example.ui.theme.AmberMotion
import com.example.ui.theme.CropFrameBorder
import com.example.ui.theme.CropOverlayDim
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SubjectBoxFace
import com.example.ui.theme.SubjectBoxPerson
import com.example.ui.theme.VioletAI
import com.example.ui.viewmodel.PreviewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun VideoPlayerView(
    videoInfo: VideoInfo,
    playbackPositionMs: Long,
    isPlaying: Boolean,
    previewMode: PreviewMode,
    currentCrop: CropRect,
    subjects: List<DetectedSubject>,
    primarySubject: DetectedSubject?,
    manualAnchor: PointF?,
    onSetManualAnchor: (PointF) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val retriever = remember(videoInfo.uri) {
        MediaMetadataRetriever().apply {
            try {
                setDataSource(context, videoInfo.uri)
            } catch (ignored: Exception) {}
        }
    }

    DisposableEffect(videoInfo.uri) {
        onDispose {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    // Extract frame bitmap for current timestamp
    LaunchedEffect(playbackPositionMs, videoInfo.uri) {
        withContext(Dispatchers.IO) {
            try {
                val timeUs = playbackPositionMs * 1000L
                val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeUs)
                if (bmp != null) {
                    currentFrameBitmap = bmp
                }
            } catch (ignored: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBackground)
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .testTag("video_player_viewport"),
        contentAlignment = Alignment.Center
    ) {
        if (previewMode == PreviewMode.FULL_FRAME_DIRECTOR) {
            // Full Frame Director Mode (Shows entire 16:9 frame + interactive 9:16 crop frame + subject boxes)
            DirectorFullFrameView(
                videoInfo = videoInfo,
                bitmap = currentFrameBitmap,
                currentCrop = currentCrop,
                subjects = subjects,
                primarySubject = primarySubject,
                manualAnchor = manualAnchor,
                onSetManualAnchor = onSetManualAnchor,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Cropped 9:16 Vertical Preview Mode (Shows exact final 9:16 video output)
            CroppedVerticalPreview(
                videoInfo = videoInfo,
                bitmap = currentFrameBitmap,
                currentCrop = currentCrop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Mode overlay badge (Top-Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC090C15))
                .border(1.dp, if (previewMode == PreviewMode.CROPPED_9_16) CyanNeon else VioletAI, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (previewMode == PreviewMode.FULL_FRAME_DIRECTOR) "DIRECTOR (16:9 FULL)" else "9:16 VERTICAL OUTPUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (previewMode == PreviewMode.CROPPED_9_16) CyanNeon else VioletAI,
                letterSpacing = 0.5.sp
            )
        }

        // Active Track Status badge (Top-Right)
        if (primarySubject != null) {
            val badgeColor = when (primarySubject.type) {
                SubjectType.FACE -> SubjectBoxFace
                SubjectType.PERSON_BODY -> SubjectBoxPerson
                SubjectType.MANUAL_PIN -> CyanNeon
                else -> AmberMotion
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC090C15))
                    .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "● ${primarySubject.label}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
private fun DirectorFullFrameView(
    videoInfo: VideoInfo,
    bitmap: Bitmap?,
    currentCrop: CropRect,
    subjects: List<DetectedSubject>,
    primarySubject: DetectedSubject?,
    manualAnchor: PointF?,
    onSetManualAnchor: (PointF) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(videoInfo) {
                // Tap to set tracking focus at touch point
                detectTapGestures { offset ->
                    val normX = (offset.x / size.width).coerceIn(0f, 1f)
                    val normY = (offset.y / size.height).coerceIn(0f, 1f)
                    onSetManualAnchor(PointF(normX, normY))
                }
            }
            .pointerInput(videoInfo) {
                // Drag to dynamically reposition framing box
                detectDragGestures { change, _ ->
                    val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                    val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                    onSetManualAnchor(PointF(normX, normY))
                    change.consume()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val containerW = maxWidth.value
        val containerH = maxHeight.value

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw source video frame
            if (bitmap != null && !bitmap.isRecycled) {
                val imgBitmap = bitmap.asImageBitmap()
                drawImage(
                    image = imgBitmap,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(width.toInt(), height.toInt())
                )
            } else {
                drawRect(Color(0xFF0F172A), size = size)
            }

            // 2. Compute 9:16 Crop Rectangle in Canvas Pixel Coordinates
            val cropPxLeft = currentCrop.left * width
            val cropPxTop = currentCrop.top * height
            val cropPxWidth = currentCrop.width * width
            val cropPxHeight = currentCrop.height * height
            val cropPxRight = cropPxLeft + cropPxWidth
            val cropPxBottom = cropPxTop + cropPxHeight

            // 3. Draw Dark Dim Mask outside 9:16 Crop
            // Left dim
            if (cropPxLeft > 0) {
                drawRect(
                    color = CropOverlayDim,
                    topLeft = Offset(0f, 0f),
                    size = Size(cropPxLeft, height)
                )
            }
            // Right dim
            if (cropPxRight < width) {
                drawRect(
                    color = CropOverlayDim,
                    topLeft = Offset(cropPxRight, 0f),
                    size = Size(width - cropPxRight, height)
                )
            }
            // Top dim
            if (cropPxTop > 0) {
                drawRect(
                    color = CropOverlayDim,
                    topLeft = Offset(cropPxLeft, 0f),
                    size = Size(cropPxWidth, cropPxTop)
                )
            }
            // Bottom dim
            if (cropPxBottom < height) {
                drawRect(
                    color = CropOverlayDim,
                    topLeft = Offset(cropPxLeft, cropPxBottom),
                    size = Size(cropPxWidth, height - cropPxBottom)
                )
            }

            // 4. Draw 9:16 Crop Framing Rectangle
            drawRect(
                color = CropFrameBorder,
                topLeft = Offset(cropPxLeft, cropPxTop),
                size = Size(cropPxWidth, cropPxHeight),
                style = Stroke(width = 2.5f)
            )

            // Rule of thirds subtle grid inside crop
            val thirdW = cropPxWidth / 3f
            val thirdH = cropPxHeight / 3f
            val gridColor = Color(0x3300F0FF)
            drawLine(gridColor, Offset(cropPxLeft + thirdW, cropPxTop), Offset(cropPxLeft + thirdW, cropPxBottom), strokeWidth = 1f)
            drawLine(gridColor, Offset(cropPxLeft + thirdW * 2, cropPxTop), Offset(cropPxLeft + thirdW * 2, cropPxBottom), strokeWidth = 1f)
            drawLine(gridColor, Offset(cropPxLeft, cropPxTop + thirdH), Offset(cropPxRight, cropPxTop + thirdH), strokeWidth = 1f)
            drawLine(gridColor, Offset(cropPxLeft, cropPxTop + thirdH * 2), Offset(cropPxRight, cropPxTop + thirdH * 2), strokeWidth = 1f)

            // Corner Reticles / Brackets on 9:16 Crop
            val cornerLen = min(cropPxWidth, cropPxHeight) * 0.12f
            val cornerStroke = 4f
            val cornerColor = CyanNeon

            // Top-Left
            drawLine(cornerColor, Offset(cropPxLeft, cropPxTop), Offset(cropPxLeft + cornerLen, cropPxTop), strokeWidth = cornerStroke)
            drawLine(cornerColor, Offset(cropPxLeft, cropPxTop), Offset(cropPxLeft, cropPxTop + cornerLen), strokeWidth = cornerStroke)
            // Top-Right
            drawLine(cornerColor, Offset(cropPxRight, cropPxTop), Offset(cropPxRight - cornerLen, cropPxTop), strokeWidth = cornerStroke)
            drawLine(cornerColor, Offset(cropPxRight, cropPxTop), Offset(cropPxRight, cropPxTop + cornerLen), strokeWidth = cornerStroke)
            // Bottom-Left
            drawLine(cornerColor, Offset(cropPxLeft, cropPxBottom), Offset(cropPxLeft + cornerLen, cropPxBottom), strokeWidth = cornerStroke)
            drawLine(cornerColor, Offset(cropPxLeft, cropPxBottom), Offset(cropPxLeft, cropPxBottom - cornerLen), strokeWidth = cornerStroke)
            // Bottom-Right
            drawLine(cornerColor, Offset(cropPxRight, cropPxBottom), Offset(cropPxRight - cornerLen, cropPxBottom), strokeWidth = cornerStroke)
            drawLine(cornerColor, Offset(cropPxRight, cropPxBottom), Offset(cropPxRight, cropPxBottom - cornerLen), strokeWidth = cornerStroke)

            // 5. Draw Detected Subject Bounding Boxes
            for (subject in subjects) {
                val box = subject.boundingBox
                val boxL = box.left * width
                val boxT = box.top * height
                val boxW = (box.right - box.left) * width
                val boxH = (box.bottom - box.top) * height

                val isPrimary = subject.id == primarySubject?.id
                val boxColor = when (subject.type) {
                    SubjectType.FACE -> SubjectBoxFace
                    SubjectType.PERSON_BODY -> SubjectBoxPerson
                    SubjectType.MANUAL_PIN -> CyanNeon
                    else -> AmberMotion
                }

                // Draw bounding box
                drawRect(
                    color = boxColor.copy(alpha = if (isPrimary) 0.9f else 0.4f),
                    topLeft = Offset(boxL, boxT),
                    size = Size(boxW, boxH),
                    style = Stroke(
                        width = if (isPrimary) 2.5f else 1.5f,
                        pathEffect = if (!isPrimary) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
                    )
                )

                // Draw tracking center dot & crosshair
                val cx = boxL + boxW / 2f
                val cy = boxT + boxH / 2f
                drawCircle(boxColor, radius = if (isPrimary) 5f else 3f, center = Offset(cx, cy))

                if (isPrimary) {
                    // Small crosshair around primary subject
                    drawLine(boxColor, Offset(cx - 10f, cy), Offset(cx + 10f, cy), strokeWidth = 1.5f)
                    drawLine(boxColor, Offset(cx, cy - 10f), Offset(cx, cy + 10f), strokeWidth = 1.5f)
                }
            }
        }
    }
}

@Composable
private fun CroppedVerticalPreview(
    videoInfo: VideoInfo,
    bitmap: Bitmap?,
    currentCrop: CropRect,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .fillMaxSize()
        ) {
            val width = size.width
            val height = size.height

            if (bitmap != null && !bitmap.isRecycled) {
                val srcW = bitmap.width
                val srcH = bitmap.height

                val cropPxLeft = (currentCrop.left * srcW).toInt().coerceIn(0, srcW - 1)
                val cropPxTop = (currentCrop.top * srcH).toInt().coerceIn(0, srcH - 1)
                val cropPxWidth = (currentCrop.width * srcW).toInt().coerceIn(1, srcW - cropPxLeft)
                val cropPxHeight = (currentCrop.height * srcH).toInt().coerceIn(1, srcH - cropPxTop)

                val imgBitmap = bitmap.asImageBitmap()
                drawImage(
                    image = imgBitmap,
                    srcOffset = IntOffset(cropPxLeft, cropPxTop),
                    srcSize = IntSize(cropPxWidth, cropPxHeight),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(width.toInt(), height.toInt())
                )
            } else {
                drawRect(Color(0xFF0B132B), size = size)
            }

            // Subtle vertical 9:16 safe area guide overlay
            val safeMarginH = width * 0.08f
            val safeMarginV = height * 0.12f
            val safeColor = Color(0x18FFFFFF)
            drawRect(
                color = safeColor,
                topLeft = Offset(safeMarginH, safeMarginV),
                size = Size(width - safeMarginH * 2, height - safeMarginV * 2),
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            )
        }
    }
}
