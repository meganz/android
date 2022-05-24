package mega.privacy.android.app.utils

import android.content.Context
import android.text.Spanned
import android.util.Base64
import androidx.annotation.ColorRes
import androidx.core.text.HtmlCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaStringMap

object StringUtils {

    @JvmStatic
    fun String.toSpannedHtmlText(): Spanned =
        HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)

    @JvmStatic
    fun String.isTextEmpty() = TextUtil.isTextEmpty(this)

    @JvmStatic
    fun String.toThrowable(): Throwable = Throwable(this)

    /**
     * Format String with HTML color tags
     *
     * @param context   Context required to get resources
     * @param tag       Tag to be replaced with font color
     * @param color     Color to be colored with
     * @return          New String with HTML
     */
    @JvmStatic
    fun String.formatColorTag(context: Context, tag: Char, @ColorRes color: Int): String =
        replace("[$tag]", "<font color='${ColorUtils.getColorHexString(context, color)}'>")
            .replace("[/$tag]", "</font>")

    /**
     * Decode the Base64-encoded data into a new formatted String
     */
    fun String.decodeBase64(): String =
        try {
            Base64.decode(this.trim(), Base64.DEFAULT).toString(Charsets.UTF_8)
        } catch (ignore: IllegalArgumentException) {
            Base64.decode(this.trim(), Base64.URL_SAFE).toString(Charsets.UTF_8)
        }

    /**
     * Encode String to Base64
     */
    fun String.encodeBase64(): String =
        Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    /**
     * Decode each alias within MegaStringMap into a Map<Long, String>
     */
    fun MegaStringMap.getDecodedAliases(): Map<Long, String> {
        val aliases = mutableMapOf<Long, String>()

        for (i in 0 until keys.size()) {
            val base64Handle = keys[i]
            val handle = MegaApiJava.base64ToUserHandle(base64Handle)
            try {
                aliases[handle] = get(base64Handle).decodeBase64()
            } catch (error: IllegalArgumentException) {
                logWarning(error.stackTraceToString())
            }
        }

        return aliases
    }


}
