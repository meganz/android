package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.PdfFileTypeInfo.extension
import mega.privacy.android.domain.entity.PdfFileTypeInfo.mimeType
import mega.privacy.android.domain.entity.UrlFileTypeInfo.extension
import mega.privacy.android.domain.entity.UrlFileTypeInfo.mimeType
import kotlin.time.Duration

/**
 * File type info
 */
sealed interface FileTypeInfo {
    /**
     * type of file
     */
    val mimeType: String

    /**
     * file extension
     */
    val extension: String
}

/**
 * Image file type info
 */
sealed interface ImageFileTypeInfo : FileTypeInfo

/**
 * Text editor file type info
 */
sealed interface TextEditorFileTypeInfo : FileTypeInfo

/**
 * Unknown file type info
 *
 * @property mimeType
 * @property extension
 */
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
object PdfFileTypeInfo : FileTypeInfo {
    override val mimeType = "application/pdf"
    override val extension = "pdf"
}

/**
 * Zip file type info
 *
 * @property mimeType
 * @property extension
 */
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
object UrlFileTypeInfo : FileTypeInfo {
    override val mimeType = "web/url"
    override val extension = "url"
}

/**
 * Static image file type info
 *
 * @property mimeType
 * @property extension
 */
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
data class AudioFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
    /**
     * Duration
     */
    val duration: Duration,
) : FileTypeInfo

/**
 * Gif file type info
 *
 * @property mimeType
 * @property extension
 */
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
data class TextFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
) : TextEditorFileTypeInfo

/**
 * Un mapped file type info
 *
 * @property extension
 */
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
data class VideoFileTypeInfo(
    override val mimeType: String,
    override val extension: String,
    /**
     * Duration
     */
    val duration: Duration,
) : FileTypeInfo
