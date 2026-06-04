package mega.privacy.android.feature.videoeditor.presentation.editor.tool.crop

import android.graphics.RectF
import androidx.compose.runtime.Immutable

/**
 * Crop selection in normalised source coordinates (`[0..1] x [0..1]`).
 *
 * `rect` defaults to the whole frame; when `(0,0,1,1)` no Media3 Crop effect
 * is contributed at export. `aspectLock` (relative to source aspect) constrains
 * corner-drag resize; `selectedPreset` records which chip the user last picked
 * so the UI can highlight it even after a manual corner-drag.
 */
@Immutable
data class CropState(
    val rect: RectF = RectF(0f, 0f, 1f, 1f),
    val freeForm: Boolean = true,
    val aspectLock: Float? = null,
    val selectedPreset: CropPreset? = CropPreset.ORIGINAL,
) {
    val isFullFrame: Boolean
        get() = rect.left <= 0f && rect.top <= 0f && rect.right >= 1f && rect.bottom >= 1f
}

enum class CropPreset(val displayName: String, val ratio: Float?, val free: Boolean = false) {
    FREE("Free", null, free = true),
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    PORTRAIT_9_16("9:16", 9f / 16f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    STANDARD_4_3("4:3", 4f / 3f),
}

/**
 * Compute the centred crop rect for a preset against a source of size `srcW x srcH`,
 * in normalised source coordinates. FREE / ORIGINAL / unknown source → full frame.
 */
fun cropRectForPreset(preset: CropPreset, srcW: Int, srcH: Int): RectF {
    if (preset.free || preset == CropPreset.ORIGINAL ||
        srcW <= 0 || srcH <= 0 || preset.ratio == null
    ) {
        return RectF(0f, 0f, 1f, 1f)
    }
    val target = preset.ratio
    val srcRatio = srcW.toFloat() / srcH.toFloat()
    return if (target >= srcRatio) {
        val cropH = (srcW / target) / srcH
        val top = (1f - cropH) / 2f
        RectF(0f, top, 1f, top + cropH)
    } else {
        val cropW = (srcH * target) / srcW
        val left = (1f - cropW) / 2f
        RectF(left, 0f, left + cropW, 1f)
    }
}
