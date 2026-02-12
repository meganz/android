package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeRelationship
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.node.DetermineNodeRelationshipUseCase
import javax.inject.Inject

/**
 * Use case to check if a folder is used by Sync or Backup across devices
 */
class IsFolderUsedBySyncOrBackupAcrossDevicesUseCase @Inject constructor(
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val getCameraUploadsSyncHandlesUseCase: GetCameraUploadsSyncHandlesUseCase,
    private val determineNodeRelationshipUseCase: DetermineNodeRelationshipUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
) {
    suspend operator fun invoke(
        nodeId: NodeId,
        shouldCheckCameraUploads: Boolean,
        shouldExcludeCurrentDevice: Boolean,
    ): FolderUsageResult {
        if (shouldCheckCameraUploads) {
            // Check if the folder is related to Camera Uploads or Media Uploads
            getCameraUploadsSyncHandlesUseCase()?.let { (primaryHandle, secondaryHandle) ->
                // Check primary (Camera Uploads) handle
                checkCameraUploadsRelationship(nodeId, NodeId(primaryHandle))?.let { return it }
                // Check secondary (Media Uploads) handle
                checkMediaUploadsRelationship(nodeId, NodeId(secondaryHandle))?.let { return it }
            }
        }

        // If not related to Camera/Media Uploads, check all backup entries
        val backups =
            getBackupInfoUseCase()
                .filterNot { shouldExcludeCurrentDevice && it.deviceId == getDeviceIdUseCase() }
                .filter { it.type == BackupInfoType.BACKUP_UPLOAD || it.type == BackupInfoType.TWO_WAY_SYNC }
        for (backup in backups) {
            val relationship =
                determineNodeRelationshipUseCase(nodeId, backup.rootHandle)
            when (relationship) {
                NodeRelationship.ExactMatch ->
                    return FolderUsageResult.UsedBySyncOrBackup(backup.deviceId)

                NodeRelationship.TargetIsAncestor ->
                    return FolderUsageResult.UsedBySyncOrBackupChild(backup.deviceId)

                NodeRelationship.TargetIsDescendant ->
                    return FolderUsageResult.UsedBySyncOrBackupParent(backup.deviceId)

                else -> { /* continue checking other backups */
                }
            }
        }

        return FolderUsageResult.NotUsed
    }

    /**
     * Checks the relationship between the given node and the Camera Uploads folder.
     * Returns the appropriate [FolderUsageResult] or null if no relationship exists.
     */
    private suspend fun checkCameraUploadsRelationship(
        nodeId: NodeId,
        cameraUploadsNodeId: NodeId,
    ): FolderUsageResult? {
        return when (determineNodeRelationshipUseCase(nodeId, cameraUploadsNodeId)) {
            NodeRelationship.ExactMatch -> FolderUsageResult.UsedByCameraUpload
            NodeRelationship.TargetIsAncestor -> FolderUsageResult.UsedByCameraUploadChild
            NodeRelationship.TargetIsDescendant -> FolderUsageResult.UsedByCameraUploadParent
            else -> null
        }
    }

    /**
     * Checks the relationship between the given node and the Media Uploads folder.
     * Returns the appropriate [FolderUsageResult] or null if no relationship exists.
     */
    private suspend fun checkMediaUploadsRelationship(
        nodeId: NodeId,
        mediaUploadsNodeId: NodeId,
    ): FolderUsageResult? {
        return when (determineNodeRelationshipUseCase(nodeId, mediaUploadsNodeId)) {
            NodeRelationship.ExactMatch -> FolderUsageResult.UsedByMediaUpload
            NodeRelationship.TargetIsAncestor -> FolderUsageResult.UsedByMediaUploadChild
            NodeRelationship.TargetIsDescendant -> FolderUsageResult.UsedByMediaUploadParent
            else -> null
        }
    }
}
