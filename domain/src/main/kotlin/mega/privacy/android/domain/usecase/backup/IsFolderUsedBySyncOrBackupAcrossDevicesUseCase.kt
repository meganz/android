package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeRelationship
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.DetermineNodeRelationshipUseCase
import javax.inject.Inject

/**
 * Use case to check if a folder is used by Sync or Backup across devices
 */
class IsFolderUsedBySyncOrBackupAcrossDevicesUseCase @Inject constructor(
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val determineNodeRelationshipUseCase: DetermineNodeRelationshipUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {
    suspend operator fun invoke(
        nodeId: NodeId,
        shouldCheckCameraUploads: Boolean,
        shouldExcludeCurrentDevice: Boolean,
    ): FolderUsageResult {
        // Check if DCIMSelectionAsSyncBackup feature flag is enabled
        val isFeatureEnabled = runCatching {
            getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup)
        }.getOrElse {
            false
        }
        if (isFeatureEnabled.not()) {
            return FolderUsageResult.NotUsed
        }

        val backups = getBackupInfoUseCase()
                .filterNot { shouldExcludeCurrentDevice && it.deviceId == getDeviceIdUseCase() }
            .filter {
                it.type == BackupInfoType.BACKUP_UPLOAD
                        || it.type == BackupInfoType.TWO_WAY_SYNC
                        || (shouldCheckCameraUploads && it.type == BackupInfoType.CAMERA_UPLOADS || it.type == BackupInfoType.MEDIA_UPLOADS)
            }
        for (backup in backups) {
            when (backup.type) {
                BackupInfoType.CAMERA_UPLOADS -> {
                    checkCameraUploadsRelationship(nodeId, backup.rootHandle)?.let { return it }
                }

                BackupInfoType.MEDIA_UPLOADS -> {
                    checkMediaUploadsRelationship(nodeId, backup.rootHandle)?.let { return it }
                }

                else -> {
                    // If not related to Camera/Media Uploads, check all backup entries
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
