package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeRelationship
import mega.privacy.android.domain.extension.TimeCache
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.DetermineNodeRelationshipUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Use case to check if a folder is used by Sync or Backup across devices
 */
class IsFolderUsedBySyncOrBackupAcrossDevicesUseCase @Inject constructor(
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val determineNodeRelationshipUseCase: DetermineNodeRelationshipUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {
    private val cachedBackups = TimeCache(CACHE_DURATION) {
        getBackupInfoUseCase()
    }

    suspend operator fun invoke(
        nodeId: NodeId,
        shouldCheckCameraUploads: Boolean,
        shouldExcludeCurrentDevice: Boolean,
        useCache: Boolean,
    ): FolderUsageResult {
        // Check if DCIMSelectionAsSyncBackup feature flag is enabled
        val isFeatureEnabled = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup)
        }.getOrElse {
            false
        }
        if (isFeatureEnabled.not()) {
            return FolderUsageResult.NotUsed
        }

        val allBackups = if (useCache) {
            cachedBackups.get()
        } else {
            cachedBackups.refresh()
        }

        val currentDeviceId = if (shouldExcludeCurrentDevice) {
            getDeviceIdUseCase()
        } else {
            null
        }

        val backups = allBackups
            .filterNot { shouldExcludeCurrentDevice && currentDeviceId != null && it.deviceId == currentDeviceId }
            .filter {
                it.type == BackupInfoType.BACKUP_UPLOAD
                        || it.type == BackupInfoType.TWO_WAY_SYNC
                        || (shouldCheckCameraUploads && (it.type == BackupInfoType.CAMERA_UPLOADS || it.type == BackupInfoType.MEDIA_UPLOADS))
            }

        for (backup in backups) {
            when (backup.type) {
                BackupInfoType.CAMERA_UPLOADS -> {
                    val result = checkCameraUploadsRelationship(nodeId, backup.rootHandle)
                    result?.let { return it }
                }

                BackupInfoType.MEDIA_UPLOADS -> {
                    val result = checkMediaUploadsRelationship(nodeId, backup.rootHandle)
                    result?.let { return it }
                }

                else -> {
                    // If not related to Camera/Media Uploads, check all backup entries
                    val relationship = determineNodeRelationshipUseCase(nodeId, backup.rootHandle)
                    when (relationship) {
                        NodeRelationship.ExactMatch -> {
                            return FolderUsageResult.UsedBySyncOrBackup(backup.deviceId)
                        }

                        NodeRelationship.TargetIsAncestor -> {
                            return FolderUsageResult.UsedBySyncOrBackupChild(backup.deviceId)
                        }

                        NodeRelationship.TargetIsDescendant -> {
                            return FolderUsageResult.UsedBySyncOrBackupParent(backup.deviceId)
                        }

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
        val relationship = determineNodeRelationshipUseCase(nodeId, cameraUploadsNodeId)
        return when (relationship) {
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
        val relationship = determineNodeRelationshipUseCase(nodeId, mediaUploadsNodeId)
        return when (relationship) {
            NodeRelationship.ExactMatch -> FolderUsageResult.UsedByMediaUpload
            NodeRelationship.TargetIsAncestor -> FolderUsageResult.UsedByMediaUploadChild
            NodeRelationship.TargetIsDescendant -> FolderUsageResult.UsedByMediaUploadParent
            else -> null
        }
    }
}

private val CACHE_DURATION = 5.minutes
