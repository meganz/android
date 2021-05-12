package mega.privacy.android.app.utils

import android.content.Context
import android.text.Spanned
import androidx.annotation.ColorRes
import androidx.core.text.HtmlCompat

object StringUtils {

    @JvmStatic
    fun String.toSpannedHtmlText(): Spanned =
        HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)

    @JvmStatic
    fun String.isTextEmpty(): Boolean =
        TextUtil.isTextEmpty(this)

    @JvmStatic
    fun String.formatColorTag(context: Context, tag: Char, @ColorRes color: Int): String =
        replace("[$tag]", "<font color='${ColorUtils.getColorHexString(context, color)}'>")
            .replace("[/$tag]", "</font>")
}
