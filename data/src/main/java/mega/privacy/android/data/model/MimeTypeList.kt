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
                extension
            ) {
                MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension)
            }
            return MimeTypeList(detectedType, extension)
        }
    }

    /**
     * Return if type is video
     */
    val isVideo
        get() = type.startsWith("video/")

    /**
     * Check if MimeType is Video
     */
    val isVideoMimeType
        get() =
            type.startsWith("video/") || extension == "vob"

    /**
     * Returns if type is non supported video
     */
    val isVideoNotSupported get() = extension == "mpg" || extension == "wmv"

    /**
     * Check if MimeType is Mp4 or Video
     */
    val isMp4Video get() = type.startsWith("video/") || extension == "mp4"


    /**
     * Check if MimeType is Zip
     */
    val isZip
        get() = (type.startsWith("application/zip") || type.startsWith("multipart/x-zip"))


    /**
     * Check if MimeType is image
     */
    val isImage
        get() = type.startsWith("image/")

    /**
     * Return if type is Audio
     */
    val isAudio get() = type.startsWith("audio/") || extension == "opus" || extension == "weba"

    /**
     * Return if type is Non supported audio
     */
    val isAudioNotSupported
        get() = extension == "wma" || extension == "aif" || extension == "aiff"
                || extension == "iff" || extension == "oga" || extension == "3ga"

}