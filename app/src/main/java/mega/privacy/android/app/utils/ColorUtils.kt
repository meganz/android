package mega.privacy.android.app.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.drawable.Drawable
import android.widget.EditText
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.appcompat.widget.DrawableUtils
import androidx.core.graphics.drawable.DrawableCompat
import mega.privacy.android.app.R

object ColorUtils {
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
        @ColorRes color: Int
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
                editText.resources.getColor(color), SRC_IN
            )
        }
    }

    @JvmStatic
    fun setEditTextUnderlineColor(editText: EditText, @ColorRes color: Int) {
        doSetEditTextUnderlineColor(editText, false, color)
    }

    @JvmStatic
    fun resetEditTextUnderlineColor(editText: EditText) {
        doSetEditTextUnderlineColor(editText, true, 0)
    }

    @JvmStatic
    fun getColorHexString(context: Context, @ColorRes color: Int): String {
        return String.format("#%06X", context.resources.getColor(color) and 0xFFFFFF)
    }
}
