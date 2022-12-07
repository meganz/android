package mega.privacy.android.data.model

import android.webkit.MimeTypeMap
import mega.privacy.android.data.mapper.getMimeType
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
            val detectedType = getMimeType(
                extension) {
                MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension)
            }
            return MimeTypeList(detectedType, extension)
        }
    }
}