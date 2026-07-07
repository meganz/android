package mega.privacy.android.feature.documentscanner.data.boundary

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import javax.inject.Inject

/**
 * Rectifies a captured frame to the detected document boundary with a 4-point
 * perspective transform via [Matrix.setPolyToPoly] — no OpenCV, per the Phase 2
 * decision to build on the TFLite contracts alone.
 *
 * The [source] bitmap is expected upright (rotation already applied) and the
 * [DocumentBoundary] corners normalised to that same orientation. When the
 * boundary is null — a manual capture with no detection — the source is returned
 * unchanged so the user still gets the full frame.
 */
internal class PerspectiveWarper @Inject constructor() {

    fun warp(source: Bitmap, boundary: DocumentBoundary?): Bitmap {
        if (boundary == null) return source

        val quad = boundary.toPixelQuad(source.width, source.height)
        val (outWidth, outHeight) = quad.warpTargetSize()

        val srcPoints = floatArrayOf(
            quad.topLeft.x, quad.topLeft.y,
            quad.topRight.x, quad.topRight.y,
            quad.bottomRight.x, quad.bottomRight.y,
            quad.bottomLeft.x, quad.bottomLeft.y,
        )
        val dstPoints = floatArrayOf(
            0f, 0f,
            outWidth.toFloat(), 0f,
            outWidth.toFloat(), outHeight.toFloat(),
            0f, outHeight.toFloat(),
        )
        val matrix = Matrix().apply { setPolyToPoly(srcPoints, 0, dstPoints, 0, CORNER_COUNT) }

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        return output
    }

    private companion object {
        const val CORNER_COUNT = 4
    }
}
