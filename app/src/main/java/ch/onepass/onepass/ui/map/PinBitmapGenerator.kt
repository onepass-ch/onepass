package ch.onepass.onepass.ui.map

import android.graphics.*

/**
 * Utility object for generating Bitmaps used as annotations on the Mapbox map, specifically for
 * event clusters.
 */
object PinBitmapGenerator {

  /**
   * Generates a circular [Bitmap] for a cluster or stack annotation, displaying the count.
   *
   * @param count The number of events in the cluster.
   * @return A [Bitmap] object representing the cluster pin.
   */
  fun generateClusterBitmap(count: Int): Bitmap {
    val scale = 2.0f // Density factor for better resolution on various screens
    val size = 64f * scale
    val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f
    val radius = size / 2.5f

    // Draw Circle Background (OnePass Purple)
    paint.color = Color.parseColor("#6200EE") // Replace with your theme color
    paint.style = Paint.Style.FILL
    canvas.drawCircle(cx, cy, radius, paint)

    // Draw Border
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f * scale
    canvas.drawCircle(cx, cy, radius, paint)

    // Draw Text
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = 24f * scale
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    // Handle large counts (display "99+" for counts over 99)
    val text = if (count > 99) "99+" else count.toString()
    val textBounds = Rect()
    paint.getTextBounds(text, 0, text.length, textBounds)
    // Center the text vertically
    val textY = cy - textBounds.exactCenterY()

    canvas.drawText(text, cx, textY, paint)

    return bitmap
  }
}
