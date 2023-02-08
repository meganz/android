package mega.privacy.android.app.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import mega.privacy.android.app.R

/**
 * a kt class to maintain the zoom constants and zoom basic reused operations.
 */
object ZoomUtil {

    // Zoom level
    const val ZOOM_OUT_2X = -2
    const val ZOOM_OUT_1X = -1
    const val ZOOM_DEFAULT = 0
    const val ZOOM_IN_1X = 1

    @JvmStatic
    var PHOTO_ZOOM_LEVEL = ZOOM_DEFAULT

    @JvmStatic
    var IMAGES_ZOOM_LEVEL = ZOOM_DEFAULT

    @JvmStatic
    var MEDIA_ZOOM_LEVEL = ZOOM_DEFAULT

    @JvmStatic
    var ALBUM_ZOOM_LEVEL = ZOOM_DEFAULT

    // Span count portrait
    private const val SPAN_COUNT_PORTRAIT_OUT_2X = 12
    private const val SPAN_COUNT_PORTRAIT_OUT_1X = 5
    private const val SPAN_COUNT_PORTRAIT_DEFAULT = 3
    private const val SPAN_COUNT_PORTRAIT_IN_1X = 1

    // Span count landscape
    private const val SPAN_COUNT_LANDSCAPE_OUT_2X = 24
    private const val SPAN_COUNT_LANDSCAPE_OUT_1X = 9
    private const val SPAN_COUNT_LANDSCAPE_DEFAULT = 5
    private const val SPAN_COUNT_LANDSCAPE_IN_1X = 2

    private fun getPortraitSpanCount(zoom: Int) = when (zoom) {
        ZOOM_IN_1X -> SPAN_COUNT_PORTRAIT_IN_1X
        ZOOM_DEFAULT -> SPAN_COUNT_PORTRAIT_DEFAULT
        ZOOM_OUT_1X -> SPAN_COUNT_PORTRAIT_OUT_1X
        ZOOM_OUT_2X -> SPAN_COUNT_PORTRAIT_OUT_2X
        else -> SPAN_COUNT_PORTRAIT_DEFAULT
    }

    private fun getLandscapeSpanCount(zoom: Int) = when (zoom) {
        ZOOM_IN_1X -> SPAN_COUNT_LANDSCAPE_IN_1X
        ZOOM_DEFAULT -> SPAN_COUNT_LANDSCAPE_DEFAULT
        ZOOM_OUT_1X -> SPAN_COUNT_LANDSCAPE_OUT_1X
        ZOOM_OUT_2X -> SPAN_COUNT_LANDSCAPE_OUT_2X
        else -> SPAN_COUNT_LANDSCAPE_DEFAULT
    }

    fun getSpanCount(isPortrait: Boolean, zoom: Int) = if (isPortrait)
        getPortraitSpanCount(zoom)
    else
        getLandscapeSpanCount(zoom)

    fun getMargin(context: Context, zoom: Int) = when (zoom) {
        ZOOM_DEFAULT, ZOOM_IN_1X -> context.resources.getDimensionPixelSize(R.dimen.cu_fragment_image_margin_large)
        ZOOM_OUT_1X -> context.resources.getDimensionPixelSize(R.dimen.cu_fragment_image_margin_small)
        else -> 0
    }

    fun disableButton(context: Context, menuItem: MenuItem) {
        menuItem.icon?.let {
            DrawableCompat.setTint(it, ContextCompat.getColor(context, R.color.grey_038_white_038))
            menuItem.setEnabled(false)
        }
    }

    fun enableButton(context: Context, menuItem: MenuItem) {
        menuItem.icon?.let {
            DrawableCompat.setTint(it, ColorUtils.getThemeColor(context, R.attr.colorControlNormal))
            menuItem.setEnabled(true)
        }
    }

    fun getSelectedFrameWidth(context: Context, zoom: Int) = when (zoom) {
        ZOOM_DEFAULT, ZOOM_IN_1X -> context.resources.getDimensionPixelSize(R.dimen.cu_fragment_ic_selected_size_large)
        ZOOM_OUT_1X -> context.resources.getDimensionPixelSize(R.dimen.cu_fragment_ic_selected_size_small)
        else -> 0
    }

    fun getSelectedFrameMargin(context: Context, zoom: Int) = when (zoom) {
        ZOOM_DEFAULT, ZOOM_IN_1X -> context.resources.getDimensionPixelSize(R.dimen.cu_fragment_ic_selected_margin_large)
        ZOOM_OUT_1X -> context.resources.getDimensionPixelSize(R.dimen.cu_fragment_ic_selected_margin_small)
        else -> 0
    }

    fun needReload(currentZoom: Int, zoom: Int): Boolean {
        return if (currentZoom == ZOOM_OUT_2X || currentZoom == ZOOM_IN_1X) true else zoom == ZOOM_OUT_2X || zoom == ZOOM_IN_1X
    }

    fun setMargin(context: Context, params: ViewGroup.MarginLayoutParams, zoom: Int) {
        if (zoom == ZOOM_IN_1X) {
            params.rightMargin = 0
            params.leftMargin = 0
        } else {
            val margin = getMargin(context, zoom)
            params.leftMargin = margin
            params.rightMargin = margin
        }
    }

    fun getItemWidth(context: Context, outMetrics: DisplayMetrics, zoom: Int, spanCount: Int, isPortrait: Boolean) =
        if (zoom == ZOOM_IN_1X && isPortrait) {
            outMetrics.widthPixels
        } else if (zoom == ZOOM_IN_1X) {
            (outMetrics.widthPixels - getMargin(
                context,
                zoom
            ) * spanCount * 2) / spanCount
        } else {
            ((outMetrics.widthPixels - getMargin(
                context,
                zoom
            ) * spanCount * 2) - getMargin(context, zoom) * 2) / spanCount
        }
}