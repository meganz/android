package mega.privacy.android.app.utils

import android.widget.TextView

/**
 * Util class to add common functionalities related to styles.
 */
object StyleUtils {

    /**
     * Sets a text style
     *
     * @param textView        View in which the styled text has to be set.
     * @param textAppearance  The style to set as textAppearance
     */
    @JvmName("setTextStyleJava")
    @JvmStatic
    fun setTextStyle(
        textView: TextView,
        textAppearance: Int,
    ) = textView.setTextAppearance(textAppearance)

    /**
     * Sets a text style
     *
     * @param textAppearance  The style to set as textAppearance
     */
    fun TextView.setTextStyle(
        textAppearance: Int,
    ) = setTextAppearance(textAppearance)
}