package mega.privacy.android.feature.videoeditor.presentation.editor.render

import android.graphics.RectF
import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure preview-geometry math. Separated from the composable so the math can be
 * unit-tested in isolation.
 *
 * Coordinate system definitions:
 * - **canvas**: the BoxWithConstraints area the preview lives in. Outer clip.
 * - **box** (boxW × boxH): canvas-fit srcAspect rect; the AndroidView's layout
 *   size. Centred inside canvas.
 * - **visible-pre**: visible region inside the AndroidView's layout *before*
 *   the outer scale is applied. Equals the AndroidView for uncropped previews,
 *   or `(cropRectW × boxW, cropRectH × boxH)` for cropped previews.
 * - **outer scale**: applied by the outer graphicsLayer; magnifies (or shrinks)
 *   the rotated visible-pre rect so it fills the canvas.
 */
@Immutable
data class PreviewGeometry(
    val boxW: Float,
    val boxH: Float,
    val visibleWPre: Float,
    val visibleHPre: Float,
    val hasCanvas: Boolean,
)

/**
 * Compute the canvas-fit srcAspect rect (boxW × boxH) plus the pre-scale
 * visible dims, given canvas dims, source aspect ratio, whether the cropped
 * preview is showing, and the crop rect.
 *
 * `cropRect` is in normalised source coords (`[0..1]`); only its width/height
 * are used here.
 */
fun computePreviewGeometry(
    canvasWPx: Float,
    canvasHPx: Float,
    srcAspect: Float,
    showCroppedPreview: Boolean,
    cropRect: RectF,
): PreviewGeometry {
    val hasCanvas = canvasWPx > 0f && canvasHPx > 0f
    val tryH = if (srcAspect > 0f) canvasWPx / srcAspect else canvasHPx
    val boxW: Float
    val boxH: Float
    if (tryH <= canvasHPx) {
        boxW = canvasWPx
        boxH = tryH
    } else {
        boxH = canvasHPx
        boxW = canvasHPx * srcAspect
    }
    val cropW = (cropRect.right - cropRect.left).coerceIn(0f, 1f)
    val cropH = (cropRect.bottom - cropRect.top).coerceIn(0f, 1f)
    val visibleWPre = if (showCroppedPreview) cropW * boxW else boxW
    val visibleHPre = if (showCroppedPreview) cropH * boxH else boxH
    return PreviewGeometry(boxW, boxH, visibleWPre, visibleHPre, hasCanvas)
}

/**
 * Outer scale that fits the rotated `visible-pre` rect into canvas.
 *
 * Does NOT clamp to `≤ 1` — when the rotated cropped aspect happens to match
 * the canvas aspect better than the unrotated, we WANT to scale up to fill
 * (e.g., 16:9 crop rotated 90° in a 9:16 canvas).
 */
fun computeOuterScale(
    geometry: PreviewGeometry,
    canvasWPx: Float,
    canvasHPx: Float,
    rotationDegrees: Float,
): Float {
    val theta = rotationDegrees * (PI.toFloat() / 180f)
    val absCos = abs(cos(theta))
    val absSin = abs(sin(theta))
    val rotatedW = geometry.visibleWPre * absCos + geometry.visibleHPre * absSin
    val rotatedH = geometry.visibleWPre * absSin + geometry.visibleHPre * absCos
    if (rotatedW <= 0f || rotatedH <= 0f || !geometry.hasCanvas) return 1f
    return min(canvasWPx / rotatedW, canvasHPx / rotatedH)
}
