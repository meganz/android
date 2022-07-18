package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import nz.mega.sdk.MegaNode

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
            extension = this
        )
    }

private fun getFileExtension(node: MegaNode) =
    node.name.substringAfterLast('.', "")

private fun getFileTypeInfoForExtension(
    mimeType: String,
    extension: String,
) = when {
    mimeType.startsWith(PdfFileTypeInfo.type) -> {
        PdfFileTypeInfo
    }
    mimeType.isZipMimeType() -> {
        ZipFileTypeInfo(
            type = mimeType,
            extension = extension,
        )
    }
    mimeType.startsWith("web/url") -> {
        UrlFileTypeInfo(
            type = mimeType,
            extension = extension,
        )
    }
    extension.isGifExtension() -> {
        GifFileTypeInfo(
            type = mimeType,
            extension = extension,
        )
    }
    mimeType.startsWith("image/") -> {
        StaticImageFileTypeInfo(
            type = mimeType,
            extension = extension,
        )
    }
    mimeType.isAudioMimeType(extension) -> {
        AudioFileTypeInfo(
            type = mimeType,
            extension = extension,
        )
    }
    mimeType.isTextMimeType(extension) -> {
        TextFileTypeInfo(
            type = mimeType,
            extension = extension,
        )
    }
    mimeType.isUnMappedMimeType(extension) -> {
        UnMappedFileTypeInfo(extension = extension)
    }
    else -> {
        UnknownFileTypeInfo(
            type = mimeType,
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

private fun String.isGifExtension() = (this == "gif") || (this == "webp")

private val textExtensions = listOf( //Text
    "txt",
    "ans",
    "ascii",
    "log",
    "wpd",
    "json",
    "md",
    "html",
    "xml",
    "shtml",
    "dhtml",
    "js",
    "css",
    "jar",
    "java",
    "class",
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
    "swift"
)