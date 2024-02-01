package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import nz.mega.sdk.MegaNode
import kotlin.time.Duration.Companion.seconds

/**
 * Map node to file type info
 */
typealias FileTypeInfoMapper = (@JvmSuppressWildcards MegaNode) -> @JvmSuppressWildcards FileTypeInfo

/**
 * Map node to file type info
 */
internal fun getFileTypeInfo(node: MegaNode, mimeTypeMapper: MimeTypeMapper): FileTypeInfo =
    with(getFileExtension(node)) {
        getFileTypeInfoForExtension(
            mimeType = mimeTypeMapper(this),
            extension = this,
            duration = node.duration
        )
    }

private fun getFileExtension(node: MegaNode) =
    node.name.substringAfterLast('.', "")

internal fun getFileTypeInfoForExtension(
    mimeType: String,
    extension: String,
    duration: Int,
) = when {
    mimeType.startsWith(PdfFileTypeInfo.mimeType) -> {
        PdfFileTypeInfo
    }

    mimeType.isZipMimeType() -> {
        ZipFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
        )
    }

    mimeType.startsWith("web/url") -> {
        UrlFileTypeInfo
    }

    extension.lowercase().isGifExtension() -> {
        GifFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
        )
    }

    extension.lowercase().isRawExtension() -> {
        RawFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
        )
    }

    extension.lowercase().isSVGExtension() -> {
        SvgFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
        )
    }

    mimeType.startsWith("image/") -> {
        StaticImageFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
        )
    }

    mimeType.isAudioMimeType(extension) -> {
        AudioFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
            duration = duration.seconds
        )
    }

    mimeType.isTextMimeType(extension) -> {
        TextFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
        )
    }

    mimeType.isVideoMimeType(extension) -> {
        VideoFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
            duration = duration.seconds
        )
    }

    mimeType.isUnMappedMimeType(extension) -> {
        UnMappedFileTypeInfo(extension = extension)
    }

    else -> {
        UnknownFileTypeInfo(
            mimeType = mimeType,
            extension = extension,
        )
    }
}

private fun String.isZipMimeType() = startsWith("multipart/x-zip") || startsWith("application/zip")
private fun String.isAudioMimeType(extension: String) =
    startsWith("audio/") || extension == "opus" || extension == "weba"

private fun String.isTextMimeType(extension: String) =
    startsWith("text/") || extension in textExtensions

private fun String.isUnMappedMimeType(extension: String) =
    startsWith("application/octet-stream") || extension.isBlank()

private fun String.isVideoMimeType(extension: String) =
    startsWith("video/") || extension == "vob"

private fun String.isGifExtension() = this == "gif"

private fun String.isRawExtension() = this in rawExtensions

private fun String.isSVGExtension() = this == "svg"

private val rawExtensions = listOf(
    "3fr", "arw", "bay",
    "cr2", "cr3", "crw",
    "ciff", "cs1", "dcr",
    "dng", "erf", "fff",
    "iiq", "k25", "kdc",
    "mef", "mos", "mrw",
    "nef", "nrw", "orf",
    "pef", "raf", "raw",
    "rw2", "rwl", "sr2",
    "srf", "srw", "x3f",
)

private val textExtensions = listOf(
    "txt", "css", "cgi",
    "ans", "jar", "py",
    "ascii", "java", "sql",
    "log", "class", "accdb",
    "wpd", "php", "db",
    "json", "php3", "dbf",
    "md", "php4", "mdb",
    "html", "php5", "pdb",
    "xml", "phtml", "c",
    "shtml", "inc", "cpp",
    "dhtml", "asp", "h",
    "js", "pl", "cs",
    "sh", "vb", "swift",
)
