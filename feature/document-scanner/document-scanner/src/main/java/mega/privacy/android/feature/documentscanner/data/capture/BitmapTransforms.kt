package mega.privacy.android.feature.documentscanner.data.capture

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.max

/** Returns a copy rotated [degrees] clockwise, or the receiver itself when upright. */
internal fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/**
 * Returns a copy scaled so its longest edge is at most [maxEdge], preserving
 * aspect ratio. Returns the receiver unchanged when it already fits.
 */
internal fun Bitmap.scaledToMaxEdge(maxEdge: Int): Bitmap {
    val longest = max(width, height)
    if (longest <= maxEdge) return this
    val scale = maxEdge.toFloat() / longest
    val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}
