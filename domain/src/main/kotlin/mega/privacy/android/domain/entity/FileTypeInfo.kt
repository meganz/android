package mega.privacy.android.domain.entity

/**
 * File type info
 */
sealed interface FileTypeInfo {
    /**
     * type of file
     */
    val type: String

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
 * @property type
 * @property extension
 */
data class UnknownFileTypeInfo(
    override val type: String,
    override val extension: String,
) : FileTypeInfo

/**
 * Pdf file type info
 *
 * @property type
 * @property extension
 */
object PdfFileTypeInfo : FileTypeInfo {
    override val type = "application/pdf"
    override val extension = "pdf"
}

/**
 * Zip file type info
 *
 * @property type
 * @property extension
 */
data class ZipFileTypeInfo(
    override val type: String,
    override val extension: String,
) : FileTypeInfo

/**
 * Url file type info
 *
 * @property type
 * @property extension
 */
object UrlFileTypeInfo : FileTypeInfo {
    override val type = "web/url"
    override val extension = "url"
}

/**
 * Static image file type info
 *
 * @property type
 * @property extension
 */
data class StaticImageFileTypeInfo(
    override val type: String,
    override val extension: String,
) : ImageFileTypeInfo

/**
 * Audio file type info
 *
 * @property type
 * @property extension
 */
data class AudioFileTypeInfo(
    override val type: String,
    override val extension: String,
    /**
     * Duration
     */
    val duration: Int,
) : FileTypeInfo

/**
 * Gif file type info
 *
 * @property type
 * @property extension
 */
data class GifFileTypeInfo(
    override val type: String,
    override val extension: String,
) : ImageFileTypeInfo


/**
 * Raw file type info
 *
 * @property type
 * @property extension
 */
data class RawFileTypeInfo(
    override val type: String,
    override val extension: String,
) : ImageFileTypeInfo

/**
 * Text file type info
 *
 * @property type
 * @property extension
 */
data class TextFileTypeInfo(
    override val type: String,
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
    override val type = "application/octet-stream"
}

/**
 * Video file type info
 *
 * @property type
 * @property extension
 * @constructor Create empty Video file type info
 */
data class VideoFileTypeInfo(
    override val type: String,
    override val extension: String,
    /**
     * Duration
     */
    val duration: Int,
) : FileTypeInfo
