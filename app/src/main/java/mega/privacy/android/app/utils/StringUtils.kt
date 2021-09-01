package mega.privacy.android.app.utils

import android.text.Spanned
import androidx.core.text.HtmlCompat

object StringUtils {

    @JvmStatic
    fun String.toSpannedHtmlText(): Spanned =
        HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)

    fun String.isTextEmpty() = TextUtil.isTextEmpty(this)

    fun String.toThrowable(): Throwable = Throwable(this)
}
