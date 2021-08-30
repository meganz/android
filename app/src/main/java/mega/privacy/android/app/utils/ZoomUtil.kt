package mega.privacy.android.app.utils

import android.content.Context
import android.widget.Switch
import mega.privacy.android.app.R

object ZoomUtil {

    // Zoom level
    const val ZOOM_OUT_3X = -3
    const val ZOOM_OUT_2X = -2
    const val ZOOM_OUT_1X = -1
    const val ZOOM_DEFAULT = 0
    const val ZOOM_IN_1X = 1

    fun getPortraitSpanCount(zoom: Int) = when (zoom) {
        ZOOM_IN_1X -> 1
        ZOOM_DEFAULT -> 3
        ZOOM_OUT_1X -> 5
        ZOOM_OUT_2X -> 12
        ZOOM_OUT_3X -> 24
        else -> 3
    }

    fun getLandscapeSpanCount(zoom: Int) = when (zoom) {
        ZOOM_IN_1X -> 1
        ZOOM_DEFAULT -> 3
        ZOOM_OUT_1X -> 5
        ZOOM_OUT_2X -> 12
        ZOOM_OUT_3X -> 24
        else -> 3
    }

    fun getSpanCount(isPortrait: Boolean, zoom: Int) = if (isPortrait)
        getPortraitSpanCount(zoom)
    else
        getLandscapeSpanCount(zoom)

    fun getMargin(context: Context, zoom: Int) =
        when (zoom) {
            ZOOM_DEFAULT -> context.resources.getDimensionPixelSize(R.dimen.cu_fragment_image_margin_large)
            ZOOM_OUT_1X -> context.resources.getDimensionPixelSize(R.dimen.cu_fragment_image_margin_small)
            else -> 0
        }

}