package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType

/**
 * A data holder for camera upload folder icon updates
 * @property nodeHandle
 * @property cameraUploadFolderType
 */
data class CameraUploadFolderIconUpdate(
    val nodeHandle: Long,
    val cameraUploadFolderType: CameraUploadFolderType,
)
