package mega.privacy.android.domain.entity

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.PdfFileTypeInfo.extension
import mega.privacy.android.domain.entity.PdfFileTypeInfo.mimeType
import mega.privacy.android.domain.entity.UrlFileTypeInfo.extension
import mega.privacy.android.domain.entity.UrlFileTypeInfo.mimeType
import kotlin.time.Duration

/**
 * File type info
 */
@Polymorphic
sealed interface FileTypeInfo {
    /**
     * type of file
     */
    val mimeType: String

    /**
     * file extension
     */
    val extension: String

    /**
     * Is supported
     */
    val isSupported: Boolean get() = true
}

/**
 * File types that can be played
 */
sealed interface PlayableFileTypeInfo {
    /**
     * Duration
     */
    val duration: Duration
}

/**
 * Image file type info
 */
@Polymorphic
sealed interface ImageFileTypeInfo : FileTypeInfo

/**
 * Text editor file type info
 */
@Polymorphic
sealed interface TextEditorFileTypeInfo : FileTypeInfo

/**
 * Unknown file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data class UnknownFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
) : FileTypeInfo

/**
 * Pdf file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data object PdfFileTypeInfo : FileTypeInfo {
    override val mimeType = "application/pdf"
    override val extension = "pdf"
}

/**
 * Zip file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data class ZipFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
) : FileTypeInfo

/**
 * Url file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data object UrlFileTypeInfo : FileTypeInfo {
    override val mimeType = "web/url"
    override val extension = "url"
}

/**
 * Static image file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data class StaticImageFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
) : ImageFileTypeInfo

/**
 * Audio file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data class AudioFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
    override val duration: Duration,
) : FileTypeInfo, PlayableFileTypeInfo {
    override val isSupported = extension != "wma" && extension != "aif" && extension != "aiff"
            && extension != "iff" && extension != "oga" && extension != "3ga"
}

/**
 * Gif file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data class GifFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
) : ImageFileTypeInfo


/**
 * Raw file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data class RawFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
) : ImageFileTypeInfo

/**
 * svg file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data class SvgFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
) : ImageFileTypeInfo

/**
 * Text file type info
 *
 * @property mimeType
 * @property extension
 */
@Serializable
data class TextFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
) : TextEditorFileTypeInfo {
    companion object {
        /**
         * Max Size Openable Text File
         */
        const val MAX_SIZE_OPENABLE_TEXT_FILE = 20971520
    }
}

/**
 * Un mapped file type info
 *
 * @property extension
 */
@Serializable
data class UnMappedFileTypeInfo(
    override val extension: String,
) : TextEditorFileTypeInfo {
    override val mimeType = "application/octet-stream"
}

/**
 * Video file type info
 *
 * @property mimeType
 * @property extension
 * @constructor Create empty Video file type info
 */
@Serializable
data class VideoFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
    override val duration: Duration,
) : FileTypeInfo, PlayableFileTypeInfo {
    /**
     * Is supported
     */
    override val isSupported: Boolean = extension != "mpg" && extension != "wmv"
}
