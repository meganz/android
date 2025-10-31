package mega.privacy.android.feature.photos.model

import androidx.compose.runtime.Immutable
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import java.time.LocalDateTime

/**
 * A stable UI representation of [mega.privacy.android.domain.entity.photos.Photo].
 */
@Immutable
sealed interface PhotoUiState {
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
    val isTakenDown: Boolean
    val isSensitive: Boolean
    val isSensitiveInherited: Boolean
    val base64Id: String?

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
        override val base64Id: String? = null,
        override val size: Long = 0L,
        override val isTakenDown: Boolean = false,
        override val isSensitive: Boolean = false,
        override val isSensitiveInherited: Boolean = false,
    ) : PhotoUiState

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
        override val base64Id: String? = null,
        override val size: Long = 0L,
        override val isTakenDown: Boolean = false,
        override val isSensitive: Boolean = false,
        override val isSensitiveInherited: Boolean = false,
    ) : PhotoUiState
}
