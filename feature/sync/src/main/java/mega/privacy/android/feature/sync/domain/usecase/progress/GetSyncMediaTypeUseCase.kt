package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import java.util.Locale
import javax.inject.Inject

/**
 * Classifies a synced file into a [SyncMediaType] by reusing the app's shared file-type resolution.
 *
 * The shared resolution only recognises zip archives and has no office-document type, so other
 * archives and office documents are recovered from the resolved MIME type.
 */
internal class GetSyncMediaTypeUseCase @Inject constructor(
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
) {
    operator fun invoke(fileName: String): SyncMediaType =
        when (val info = getFileTypeInfoByNameUseCase(fileName)) {
            is ImageFileTypeInfo -> SyncMediaType.Image
            is VideoFileTypeInfo -> SyncMediaType.Video
            is AudioFileTypeInfo -> SyncMediaType.Audio
            is ZipFileTypeInfo -> SyncMediaType.Archive
            PdfFileTypeInfo, is TextFileTypeInfo -> SyncMediaType.Document
            UrlFileTypeInfo, is UnMappedFileTypeInfo -> SyncMediaType.Other
            is UnknownFileTypeInfo -> fromMimeType(info.mimeType)
        }

    private fun fromMimeType(rawMimeType: String): SyncMediaType {
        val mimeType = rawMimeType.substringBefore(';').lowercase(Locale.ROOT)
        return when {
            mimeType in ARCHIVE_MIME_TYPES -> SyncMediaType.Archive
            mimeType in DOCUMENT_MIME_TYPES -> SyncMediaType.Document
            DOCUMENT_MIME_TYPE_PREFIXES.any { mimeType.startsWith(it) } -> SyncMediaType.Document
            else -> SyncMediaType.Other
        }
    }

    private companion object {
        val ARCHIVE_MIME_TYPES = setOf(
            "application/x-rar-compressed",
            "application/rar",
            "application/vnd.rar",
            "application/x-7z-compressed",
            "application/x-tar",
            "application/gzip",
            "application/x-gzip",
            "application/x-bzip2",
            "application/x-gtar",
            "application/x-gtar-compressed",
            "application/x-xz",
            "application/zstd",
            "application/x-iso9660-image",
            "application/x-apple-diskimage",
            "application/vnd.ms-cab-compressed",
            "application/x-bittorrent",
        )

        val DOCUMENT_MIME_TYPES = setOf(
            "application/msword",
            "application/rtf",
            "application/vnd.apple.pages",
            "application/vnd.apple.numbers",
            "application/vnd.apple.keynote",
        )

        val DOCUMENT_MIME_TYPE_PREFIXES = listOf(
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "application/vnd.ms-word",
            "application/vnd.ms-works",
            "application/vnd.openxmlformats-officedocument.",
            "application/vnd.oasis.opendocument.",
        )
    }
}
