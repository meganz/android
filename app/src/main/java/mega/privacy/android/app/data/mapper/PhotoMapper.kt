package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.photos.Photo
import java.time.LocalDateTime

/**
 * The mapper class for converting the data entity to Photo.Image
 */
typealias ImageMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
) -> @JvmSuppressWildcards Photo

internal fun toImage(
    id: Long,
    parentId: Long,
    name: String,
    isFavourite: Boolean,
    creationTime: LocalDateTime,
    modificationTime: LocalDateTime,
    thumbnailFilePath: String?,
    previewFilePath: String?,
) = Photo.Image(
    id = id,
    parentId = parentId,
    name = name,
    isFavourite = isFavourite,
    creationTime = creationTime,
    modificationTime = modificationTime,
    thumbnailFilePath = thumbnailFilePath,
    previewFilePath = previewFilePath
)

/**
 * The mapper class for converting the data entity to Photo.Video
 */
typealias VideoMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards Int,
) -> @JvmSuppressWildcards Photo

internal fun toVideo(
    id: Long,
    parentId: Long,
    name: String,
    isFavourite: Boolean,
    creationTime: LocalDateTime,
    modificationTime: LocalDateTime,
    thumbnailFilePath: String?,
    previewFilePath: String?,
    duration: Int,
) = Photo.Video(
    id = id,
    parentId = parentId,
    name = name,
    isFavourite = isFavourite,
    creationTime = creationTime,
    modificationTime = modificationTime,
    thumbnailFilePath = thumbnailFilePath,
    previewFilePath = previewFilePath,
    duration = duration
)

