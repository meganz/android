package mega.privacy.android.data.model

import android.webkit.MimeTypeMap
import java.util.Locale

/**
 * Mime type list
 *
 * @property type
 * @property extension
 */
data class MimeTypeList(
    val type: String,
    val extension: String,
) {
    companion object {
        /**
         * Type for name
         *
         * @param name
         */
        fun typeForName(name: String?): MimeTypeList {
            val newName = name ?: ""
            val fixedName = newName.trim().lowercase(Locale.getDefault())
            var extension = ""
            val index = fixedName.lastIndexOf(".")
            if (index >= 0 && index + 1 < fixedName.length) {
                extension = fixedName.substring(index + 1)
            }
            val detectedType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: when (extension) {
                    "mkv" -> "video/x-matroska"
                    "heic" -> "image/heic"
                    "url" -> "web/url"
                    "webp" -> "image/webp"
                    else -> "application/octet-stream"
                }
            return MimeTypeList(detectedType, extension)
        }
    }
}