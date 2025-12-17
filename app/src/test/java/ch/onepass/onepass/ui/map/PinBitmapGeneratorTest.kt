package ch.onepass.onepass.ui.map

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinBitmapGeneratorTest {

  @Test
  fun generateClusterBitmap_createsBitmapWithCorrectConfig() {
    val count = 5
    val bitmap = PinBitmapGenerator.generateClusterBitmap(count)

    assertNotNull(bitmap)
    assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    // Size check (64 * 2.0 scale = 128)
    assertEquals(128, bitmap.width)
    assertEquals(128, bitmap.height)
  }

  @Test
  fun generateClusterBitmap_handlesLargeNumbers() {
    val bitmap = PinBitmapGenerator.generateClusterBitmap(150)
    assertNotNull(bitmap)
    // Visual verification isn't possible in unit tests, but we ensure no crash on drawing "99+"
  }
}
