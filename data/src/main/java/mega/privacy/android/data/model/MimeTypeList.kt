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
        private const val MAX_SIZE_OPENABLE_TEXT_FILE = 20971520

        /**
         * List of extension text files
         */
        private val TEXT_EXTENSIONS = listOf(
            //Text
            "txt",
            "ans",
            "ascii",
            "log",
            "wpd",
            "json",
            "md",
            //Web data
            "html",
            "xml",
            "shtml",
            "dhtml",
            "js",
            "css",
            "jar",
            "java",
            "class",
            //Web lang
            "php",
            "php3",
            "php4",
            "php5",
            "phtml",
            "inc",
            "asp",
            "pl",
            "cgi",
            "py",
            "sql",
            "accdb",
            "db",
            "dbf",
            "mdb",
            "pdb",
            "c",
            "cpp",
            "h",
            "cs",
            "sh",
            "vb",
            "swift",
            "org"
        )

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

    /**
     * Checks if a file is openable in Text editor.
     * All the contemplated extension are supported by Web client, so mobile clients should try
     * to support them too.
     * @return True if the file is openable, false otherwise.
     */
    private val isValidTextFileType
        get() = type.startsWith("text/plain")
                //File extensions considered as plain text
                || TEXT_EXTENSIONS.contains(extension)
                //Files without extension
                || type.startsWith("application/octet-stream")

    /**
     * Checks if a file is openable in Text editor.
     * It's openable if its size is not bigger than MAX_SIZE_OPENABLE_TEXT_FILE.
     *
     * @return True if the file is openable, false otherwise.
     */
    fun isOpenableTextFile(fileSize: Long) =
        isValidTextFileType && fileSize <= MAX_SIZE_OPENABLE_TEXT_FILE

}