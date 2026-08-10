package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.model.PhotoUiState
import javax.inject.Inject

class PhotoMapper @Inject constructor() {

    operator fun invoke(photoUiState: PhotoUiState): Photo {
        return when (photoUiState) {
            is PhotoUiState.Image -> {
                Photo.Image(
                    id = photoUiState.id,
                    albumPhotoId = photoUiState.albumPhotoId,
                    parentId = photoUiState.parentId,
                    name = photoUiState.name,
                    isFavourite = photoUiState.isFavourite,
                    creationTime = photoUiState.creationTime,
                    modificationTime = photoUiState.modificationTime,
                    thumbnailFilePath = photoUiState.thumbnailFilePath,
                    previewFilePath = photoUiState.previewFilePath,
                    fileTypeInfo = photoUiState.fileTypeInfo,
                    size = photoUiState.size,
                    isTakenDown = photoUiState.isTakenDown,
                    isSensitive = photoUiState.isSensitive,
                    isSensitiveInherited = photoUiState.isSensitiveInherited,
                    base64Id = photoUiState.base64Id,
                    restoreId = photoUiState.restoreId,
                    label = photoUiState.label,
                    nodeLabel = photoUiState.nodeLabel,
                    exportedData = photoUiState.exportedData,
                    isIncomingShare = photoUiState.isIncomingShare,
                    isNodeKeyDecrypted = photoUiState.isNodeKeyDecrypted,
                    serializedData = photoUiState.serializedData,
                    isAvailableOffline = photoUiState.isAvailableOffline,
                    versionCount = photoUiState.versionCount,
                    description = photoUiState.description,
                    tags = photoUiState.tags
                )
            }

            is PhotoUiState.Video -> {
                Photo.Video(
                    id = photoUiState.id,
                    albumPhotoId = photoUiState.albumPhotoId,
                    parentId = photoUiState.parentId,
                    name = photoUiState.name,
                    isFavourite = photoUiState.isFavourite,
                    creationTime = photoUiState.creationTime,
                    modificationTime = photoUiState.modificationTime,
                    thumbnailFilePath = photoUiState.thumbnailFilePath,
                    previewFilePath = photoUiState.previewFilePath,
                    fileTypeInfo = photoUiState.fileTypeInfo,
                    size = photoUiState.size,
                    isTakenDown = photoUiState.isTakenDown,
                    isSensitive = photoUiState.isSensitive,
                    isSensitiveInherited = photoUiState.isSensitiveInherited,
                    base64Id = photoUiState.base64Id,
                    restoreId = photoUiState.restoreId,
                    label = photoUiState.label,
                    nodeLabel = photoUiState.nodeLabel,
                    exportedData = photoUiState.exportedData,
                    isIncomingShare = photoUiState.isIncomingShare,
                    isNodeKeyDecrypted = photoUiState.isNodeKeyDecrypted,
                    serializedData = photoUiState.serializedData,
                    isAvailableOffline = photoUiState.isAvailableOffline,
                    versionCount = photoUiState.versionCount,
                    description = photoUiState.description,
                    tags = photoUiState.tags
                )
            }
        }
    }
}
