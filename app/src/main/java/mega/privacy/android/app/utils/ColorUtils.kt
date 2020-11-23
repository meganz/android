package mega.privacy.android.app.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.appcompat.widget.DrawableUtils
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.shape.MaterialShapeDrawable
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
        val icon = context.getDrawable(drawableId)
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
        color: Int
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
        doSetEditTextUnderlineColor(editText, false, editText.resources.getColor(color))
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
        return String.format("#%06X", context.resources.getColor(color) and 0xFFFFFF)
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
        cornerSize: Float = 0f
    ): MaterialShapeDrawable {
        val colorInt = getColorForElevation(context, elevation)
        val shapeAppearanceModel = ShapeAppearanceModel.builder().setTopLeftCornerSize(cornerSize)
            .setTopRightCornerSize(cornerSize).build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)

        shapeDrawable.setTint(colorInt)

        return shapeDrawable
    }

    @JvmStatic
    fun getColorForElevation(context: Context, elevation: Float) =
        ElevationOverlayProvider(context).compositeOverlayWithThemeSurfaceColorIfNeeded(
            elevation
        )

    /**
     * Set status bar text and icon colours for good visibility in light/dark mode accordingly
     */
    @JvmStatic
    fun setStatusBarTextColor(activity: Activity) {
        val decor: View = activity.window.decorView
        if (Util.isDarkMode(activity)) {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    /**
     * Apply the alpha (between 0.0 and 1.0) to the imageView if in dark mode.
     */
    @JvmStatic
    fun setImageViewAlphaIfDark(
        context: Context,
        imageView: ImageView,
        alpha: Float = DARK_IMAGE_ALPHA
    ) {
        if (!Util.isDarkMode(context) || alpha < 0f || alpha > 1f) return
        imageView.imageAlpha = (255 * alpha).roundToInt()
    }
}
