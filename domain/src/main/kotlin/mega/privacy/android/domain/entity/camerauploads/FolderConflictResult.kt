package mega.privacy.android.domain.entity.camerauploads

import mega.privacy.android.domain.entity.node.FolderUsageResult

/**
 * Wraps a [FolderUsageResult] conflict with the [CameraUploadFolderType] that was checked.
 */
data class FolderConflictResult(
    val folderUsageResult: FolderUsageResult,
    val cameraUploadFolderType: CameraUploadFolderType,
)
