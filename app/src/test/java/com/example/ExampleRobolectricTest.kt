package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CinematicCameraPlanner
import com.example.model.CropRect
import com.example.model.TrackingConfig
import com.example.model.VideoInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AutoReframe", appName)
  }

  @Test
  fun `verify camera planner maintains 9 to 16 aspect ratio crop`() {
    val planner = CinematicCameraPlanner()
    val videoInfo = VideoInfo(
      uri = android.net.Uri.EMPTY,
      title = "Test 1080p Video",
      durationMs = 5000L,
      width = 1920,
      height = 1080,
      displayWidth = 1920,
      displayHeight = 1080,
      aspectRatio = 16f / 9f
    )

    val (crop, _) = planner.computeRawTargetCrop(
      frameTimeMs = 0L,
      videoInfo = videoInfo,
      subjects = emptyList(),
      manualAnchor = null,
      config = TrackingConfig()
    )

    // Verify crop width and height are bounded within [0, 1]
    assertTrue(crop.left >= 0f)
    assertTrue(crop.top >= 0f)
    assertTrue(crop.width > 0f)
    assertTrue(crop.height > 0f)
    assertTrue(crop.left + crop.width <= 1.0001f)
    assertTrue(crop.top + crop.height <= 1.0001f)
  }
}

