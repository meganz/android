package mega.privacy.android.app.utils

import android.content.Context
import android.os.Build
import android.widget.TextView
import androidx.core.content.ContextCompat
import mega.privacy.android.app.utils.ColorUtils.getThemeColor

/**
 * Util class to add common functionalities related to styles.
 */
object StyleUtils {

    /**
     * Sets a text style if the current version supports textAppearance,
     * changes the text color if not.
     *
     * @param context         Current context.
     * @param textView        View in which the styled text has to be set.
     * @param textAppearance  The style to set as textAppearance
     * @param textColor       Color to set to the text in case the current version does not support textAppearance.
     * @param isTextAttrColor True if the color to set is an attr color, false otherwise.
     */
    @JvmStatic
    fun setTextStyle(
        context: Context,
        textView: TextView,
        textAppearance: Int,
        textColor: Int,
        isTextAttrColor: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(textAppearance)
        } else {
            textView.setTextColor(
                if (isTextAttrColor) getThemeColor(context, textColor)
                else ContextCompat.getColor(context, textColor)
            )
        }
    }

    /**
     * Sets a text style if the current version supports textAppearance,
     * changes the text color if not.
     *
     * @param context         Current context.
     * @param textAppearance  The style to set as textAppearance
     * @param textColor       Color to set to the text in case the current version does not support textAppearance.
     * @param isTextAttrColor True if the color to set is an attr color, false otherwise.
     */
    fun TextView.setTextStyle(
        context: Context,
        textAppearance: Int,
        textColor: Int,
        isTextAttrColor: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setTextAppearance(textAppearance)
        } else {
            setTextColor(
                if (isTextAttrColor) getThemeColor(context, textColor)
                else ContextCompat.getColor(context, textColor)
            )
        }
    }
}