package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.model.PhotoUiState
import javax.inject.Inject

class PhotoUiStateMapper @Inject constructor(
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) {

    operator fun invoke(photo: Photo): PhotoUiState {
        return when (photo) {
            is Photo.Image -> {
                PhotoUiState.Image(
                    id = photo.id,
                    albumPhotoId = photo.albumPhotoId,
                    parentId = photo.parentId,
                    name = photo.name,
                    isFavourite = photo.isFavourite,
                    creationTime = photo.creationTime,
                    modificationTime = photo.modificationTime,
                    thumbnailFilePath = photo.thumbnailFilePath,
                    previewFilePath = photo.previewFilePath,
                    fileTypeInfo = photo.fileTypeInfo,
                    size = photo.size,
                    isTakenDown = photo.isTakenDown,
                    isSensitive = photo.isSensitive,
                    isSensitiveInherited = photo.isSensitiveInherited,
                    base64Id = photo.base64Id,
                    restoreId = photo.restoreId,
                    label = photo.label,
                    nodeLabel = photo.nodeLabel,
                    exportedData = photo.exportedData,
                    isIncomingShare = photo.isIncomingShare,
                    isNodeKeyDecrypted = photo.isNodeKeyDecrypted,
                    serializedData = photo.serializedData,
                    isAvailableOffline = photo.isAvailableOffline,
                    versionCount = photo.versionCount,
                    description = photo.description,
                    tags = photo.tags
                )
            }

            is Photo.Video -> {
                PhotoUiState.Video(
                    id = photo.id,
                    albumPhotoId = photo.albumPhotoId,
                    parentId = photo.parentId,
                    name = photo.name,
                    isFavourite = photo.isFavourite,
                    creationTime = photo.creationTime,
                    modificationTime = photo.modificationTime,
                    thumbnailFilePath = photo.thumbnailFilePath,
                    previewFilePath = photo.previewFilePath,
                    fileTypeInfo = photo.fileTypeInfo,
                    size = photo.size,
                    isTakenDown = photo.isTakenDown,
                    isSensitive = photo.isSensitive,
                    isSensitiveInherited = photo.isSensitiveInherited,
                    base64Id = photo.base64Id,
                    restoreId = photo.restoreId,
                    label = photo.label,
                    nodeLabel = photo.nodeLabel,
                    exportedData = photo.exportedData,
                    isIncomingShare = photo.isIncomingShare,
                    isNodeKeyDecrypted = photo.isNodeKeyDecrypted,
                    serializedData = photo.serializedData,
                    isAvailableOffline = photo.isAvailableOffline,
                    versionCount = photo.versionCount,
                    description = photo.description,
                    tags = photo.tags,
                    duration = durationInSecondsTextMapper(duration = photo.fileTypeInfo.duration)
                )
            }
        }
    }
}
