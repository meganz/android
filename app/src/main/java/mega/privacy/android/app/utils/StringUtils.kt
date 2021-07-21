package mega.privacy.android.app.utils

import android.content.Context
import android.text.Spanned
import android.util.Base64
import androidx.annotation.ColorRes
import androidx.core.text.HtmlCompat
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaStringMap

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

    /**
     * Decode the Base64-encoded data into a new formatted String
     */
    fun String.decodeBase64(): String =
        Base64.decode(this, Base64.DEFAULT).toString(Charsets.UTF_8)

    /**
     * Decode each alias within MegaStringMap into a Map<Long, String>
     */
    fun MegaStringMap.getDecodedAliases(): Map<Long, String> {
        val aliases = mutableMapOf<Long, String>()

        for (i in 0 until keys.size()) {
            val base64Handle = keys[i]
            val handle = MegaApiJava.base64ToUserHandle(base64Handle)
            val alias = get(base64Handle).decodeBase64()
            aliases[handle] = alias
        }

        return aliases
    }
}
