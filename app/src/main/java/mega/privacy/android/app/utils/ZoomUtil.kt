package mega.privacy.android.app.utils

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ZoomPanelBinding

object ZoomUtil {

    // Zoom level
    const val ZOOM_OUT_3X = -3
    const val ZOOM_OUT_2X = -2
    const val ZOOM_OUT_1X = -1
    const val ZOOM_DEFAULT = 0
    const val ZOOM_IN_1X = 1

    // Span count portrait
    private const val SPAN_COUNT_PORTRAIT_OUT_3X = 24
    private const val SPAN_COUNT_PORTRAIT_OUT_2X = 12
    private const val SPAN_COUNT_PORTRAIT_OUT_1X = 5
    private const val SPAN_COUNT_PORTRAIT_DEFAULT = 3
    private const val SPAN_COUNT_PORTRAIT_IN_1X = 1

    // Span count landscape
    private const val SPAN_COUNT_LANDSCAPE_OUT_3X = 45
    private const val SPAN_COUNT_LANDSCAPE_OUT_2X = 24
    private const val SPAN_COUNT_LANDSCAPE_OUT_1X = SPAN_COUNT_PORTRAIT_DEFAULT
    private const val SPAN_COUNT_LANDSCAPE_DEFAULT = SPAN_COUNT_PORTRAIT_DEFAULT
    private const val SPAN_COUNT_LANDSCAPE_IN_1X = SPAN_COUNT_PORTRAIT_IN_1X

    private fun getPortraitSpanCount(zoom: Int) = when (zoom) {
        ZOOM_IN_1X -> SPAN_COUNT_PORTRAIT_IN_1X
        ZOOM_DEFAULT -> SPAN_COUNT_PORTRAIT_DEFAULT
        ZOOM_OUT_1X -> SPAN_COUNT_PORTRAIT_OUT_1X
        ZOOM_OUT_2X -> SPAN_COUNT_PORTRAIT_OUT_2X
        ZOOM_OUT_3X -> SPAN_COUNT_PORTRAIT_OUT_3X
        else -> SPAN_COUNT_PORTRAIT_DEFAULT
    }

    private fun getLandscapeSpanCount(zoom: Int) = when (zoom) {
        ZOOM_IN_1X -> SPAN_COUNT_LANDSCAPE_IN_1X
        ZOOM_DEFAULT -> SPAN_COUNT_LANDSCAPE_DEFAULT
        ZOOM_OUT_1X -> SPAN_COUNT_LANDSCAPE_OUT_1X
        ZOOM_OUT_2X -> SPAN_COUNT_LANDSCAPE_OUT_2X
        ZOOM_OUT_3X -> SPAN_COUNT_LANDSCAPE_OUT_3X
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

    fun showHidePanel(shouldShow: Boolean, panel: View) {
        panel.visibility = if (shouldShow) View.VISIBLE else View.GONE
    }

    fun disableButton(button: ImageView, context: Context) {
        button.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white_alpha_054))
    }

    fun enableButton(button: ImageView, context: Context) {
        button.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
    }

    fun getIcSelectedWidth(zoom: Int, context: Context) = when(zoom) {

    }
}