package mega.privacy.android.domain.entity.photos

import mega.privacy.android.domain.entity.FileTypeInfo
import java.time.LocalDateTime

/**
 * A domain interface Node include Photos class, Video, Image
 */
sealed interface Photo {
    val id: Long
    val parentId: Long
    val name: String
    val isFavourite: Boolean
    val creationTime: LocalDateTime
    val modificationTime: LocalDateTime
    val thumbnailFilePath: String?
    val previewFilePath: String?
    val fileTypeInfo: FileTypeInfo

    data class Video(
        override val id: Long,
        override val parentId: Long,
        override val name: String,
        override val isFavourite: Boolean,
        override val creationTime: LocalDateTime,
        override val modificationTime: LocalDateTime,
        override val thumbnailFilePath: String?,
        override val previewFilePath: String?,
        val duration: Int,
        override val fileTypeInfo: FileTypeInfo,
    ) : Photo

    data class Image(
        override val id: Long,
        override val parentId: Long,
        override val name: String,
        override val isFavourite: Boolean,
        override val creationTime: LocalDateTime,
        override val modificationTime: LocalDateTime,
        override val thumbnailFilePath: String?,
        override val previewFilePath: String?,
        override val fileTypeInfo: FileTypeInfo,
    ) : Photo
}

