package mega.privacy.android.app.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.appcompat.widget.DrawableUtils
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.MaterialShapeUtils
import com.google.android.material.shape.ShapeAppearanceModel
import mega.privacy.android.app.R
import kotlin.math.roundToInt


object ColorUtils {
    /** The alpha applied to the image in dark mode */
    const val DARK_IMAGE_ALPHA = 0.16f

    /**
     * Queries the theme of the given `context` for a theme color.
     *
     * @param context   the context holding the current theme.
     * @param attrResId the theme color attribute to resolve.
     * @return the theme color
     */
    @ColorInt
    @JvmStatic
    fun getThemeColor(context: Context, @AttrRes attrResId: Int): Int {
        val a = context.obtainStyledAttributes(intArrayOf(attrResId))
        return try {
            a.getColor(0, Color.MAGENTA)
        } finally {
            a.recycle()
        }
    }

    @JvmStatic
    fun tintIcon(context: Context, drawableId: Int, color: Int): Drawable {
        val icon = ContextCompat.getDrawable(context, drawableId)
        val drawableWrap = DrawableCompat.wrap(icon!!).mutate()
        DrawableCompat.setTint(drawableWrap, color)
        return drawableWrap
    }

    @JvmStatic
    fun tintIcon(context: Context, drawableId: Int): Drawable {
        return tintIcon(context, drawableId, getThemeColor(context, R.attr.colorControlNormal))
    }

    private fun doSetEditTextUnderlineColor(
        editText: EditText,
        reset: Boolean,
        color: Int,
    ) {
        var editTextBackground = editText.background ?: return
        if (DrawableUtils.canSafelyMutateDrawable(editTextBackground)) {
            editTextBackground = editTextBackground.mutate()
        }

        if (reset) {
            DrawableCompat.clearColorFilter(editTextBackground)
            editText.refreshDrawableState()
        } else {
            editTextBackground.colorFilter = AppCompatDrawableManager.getPorterDuffColorFilter(
                color, SRC_IN
            )
        }
    }

    @JvmStatic
    fun setEditTextUnderlineColor(editText: EditText, @ColorRes color: Int) {
        doSetEditTextUnderlineColor(
            editText, false, ContextCompat.getColor(editText.context, color)
        )
    }

    @JvmStatic
    fun setEditTextUnderlineColorAttr(editText: EditText, @AttrRes color: Int) {
        doSetEditTextUnderlineColor(editText, false, getThemeColor(editText.context, color))
    }

    @JvmStatic
    fun resetEditTextUnderlineColor(editText: EditText) {
        doSetEditTextUnderlineColor(editText, true, 0)
    }

    @JvmStatic
    fun getColorHexString(context: Context, @ColorRes color: Int): String {
        return String.format("#%06X", ContextCompat.getColor(context, color) and 0xFFFFFF)
    }

    @JvmStatic
    fun getThemeColorHexString(context: Context, @AttrRes attr: Int): String {
        return String.format("#%06X", getThemeColor(context, attr) and 0xFFFFFF)
    }

    /**
     * Get a MaterialShapeDrawable for the background of an sheet/dialog component, in the light of
     * its elevation and corner size
     *
     * @param context
     * @param elevation elevation in px
     * @param cornerSize rounded corner size in px
     */
    @JvmStatic
    fun getShapeDrawableForElevation(
        context: Context,
        elevation: Float = 0f,
        cornerSize: Float = 0f,
    ): MaterialShapeDrawable {
        val colorInt = getColorForElevation(context, elevation)
        val shapeAppearanceModel = ShapeAppearanceModel.builder().setTopLeftCornerSize(cornerSize)
            .setTopRightCornerSize(cornerSize).build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)

        shapeDrawable.setTint(colorInt)

        return shapeDrawable
    }

    /**
     * Get the view background color in Dark Mode according to its elevation
     * @param context Context
     * @param elevation the elevation value of the View
     */
    @JvmStatic
    fun getColorForElevation(context: Context, elevation: Float) =
        ElevationOverlayProvider(context).compositeOverlayWithThemeSurfaceColorIfNeeded(
            elevation
        )

    /**
     * Set elevation with right color for light/dark mode
     *
     * @param elevation Elevation value of the view
     */
    @JvmStatic
    @RequiresApi(VERSION_CODES.LOLLIPOP)
    fun View.setElevationWithColor(elevation: Float) {
        MaterialShapeUtils.setElevation(this, elevation)
        setElevation(elevation)
        if (elevation != 0F) {
            setBackgroundColor(getColorForElevation(context, elevation))
        } else {
            setBackgroundColor(android.R.color.transparent)
        }
    }

    /**
     * Set status bar text and icon colours for good visibility in light/dark mode accordingly
     */
    @JvmStatic
    fun setStatusBarTextColor(activity: Activity) {
        setStatusBarTextColor(activity, activity.window)
    }

    /**
     * Set status & navigation bar text & icon colours for good visibility in light/dark mode
     * accordingly.
     *
     * @param context context
     * @param window window of activity/dialog
     */
    @JvmStatic
    fun setStatusBarTextColor(context: Context, window: Window?) {
        val decor: View = window?.decorView ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            if (!Util.isDarkMode(context)) {
                val wic: WindowInsetsController? = decor.windowInsetsController
                wic?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS,
                    APPEARANCE_LIGHT_STATUS_BARS
                )
                wic?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_NAVIGATION_BARS,
                    APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            @Suppress("DEPRECATION")
            if (Util.isDarkMode(context)) {
                decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        decor.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    }
                    else -> {
                        decor.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or 0x00000010
                    }
                }
            }
        }
    }

    /**
     * Apply the alpha (between 0.0 and 1.0) to the imageView if in dark mode.
     */
    @JvmStatic
    fun setImageViewAlphaIfDark(
        context: Context,
        imageView: ImageView,
        alpha: Float = DARK_IMAGE_ALPHA,
    ) {
        if (!Util.isDarkMode(context) || alpha < 0f || alpha > 1f) return
        imageView.imageAlpha = (255 * alpha).roundToInt()
    }

    /**
     * Set appearance (text color, underline color, highlight color) for an error aware text input.
     *
     * @param editText the text input
     * @param error whether it's in error state
     */
    @JvmStatic
    fun setErrorAwareInputAppearance(editText: EditText, error: Boolean) {
        if (error) {
            editText.setTextColor(getThemeColor(editText.context, R.attr.colorError))
            setEditTextUnderlineColorAttr(editText, R.attr.colorError)
            editText.highlightColor =
                ContextCompat.getColor(editText.context, R.color.teal_100_teal_050)
        } else {
            editText.setTextColor(getThemeColor(editText.context, android.R.attr.textColorPrimary))
            resetEditTextUnderlineColor(editText)
            editText.highlightColor =
                getThemeColor(editText.context, android.R.attr.textColorHighlight)
        }
    }

    /**
     * Under dark mode, status bar's color should be change along with app bar layout's background color.
     *
     * @param activity Current activity.
     * @param withElevation If current activity should have elevation.
     */
    @JvmStatic
    fun changeStatusBarColorForElevation(activity: Activity, withElevation: Boolean) {
        // Only for dark mode.
        if (!Util.isDarkMode(activity)) return

        if (withElevation) {
            val elevation: Float = activity.resources.getDimension(R.dimen.toolbar_elevation)
            val toolbarElevationColor = getColorForElevation(activity, elevation)
            RunOnUIThreadUtils.post { activity.window.statusBarColor = toolbarElevationColor }
        } else {
            RunOnUIThreadUtils.post {
                activity.window.statusBarColor =
                    activity.resources.getColor(android.R.color.transparent, null)
            }
        }
    }

    /**
     * Under dark mode, status bar's color should be change along with app bar layout's background color.
     *
     * @param activity Current activity.
     * @param resId The id of the color res.
     */
    @JvmStatic
    fun changeStatusBarColor(activity: Activity, resId: Int) {
        RunOnUIThreadUtils.post {
            activity.window.statusBarColor = ContextCompat.getColor(activity, resId)
        }
    }
}
