package mega.privacy.android.domain.entity.photos

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import java.time.LocalDateTime

/**
 * A domain interface Node include Photos class, Video, Image
 */
sealed interface Photo {
    val id: Long
    val albumPhotoId: Long?
    val parentId: Long
    val name: String
    val isFavourite: Boolean
    val creationTime: LocalDateTime
    val modificationTime: LocalDateTime
    val thumbnailFilePath: String?
    val previewFilePath: String?
    val fileTypeInfo: FileTypeInfo
    val size: Long

    data class Video(
        override val id: Long,
        override val albumPhotoId: Long? = null,
        override val parentId: Long,
        override val name: String,
        override val isFavourite: Boolean,
        override val creationTime: LocalDateTime,
        override val modificationTime: LocalDateTime,
        override val thumbnailFilePath: String?,
        override val previewFilePath: String?,
        override val fileTypeInfo: VideoFileTypeInfo,
        override val size: Long = 0L,
    ) : Photo

    data class Image(
        override val id: Long,
        override val albumPhotoId: Long? = null,
        override val parentId: Long,
        override val name: String,
        override val isFavourite: Boolean,
        override val creationTime: LocalDateTime,
        override val modificationTime: LocalDateTime,
        override val thumbnailFilePath: String?,
        override val previewFilePath: String?,
        override val fileTypeInfo: FileTypeInfo,
        override val size: Long = 0L,
    ) : Photo
}
