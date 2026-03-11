package mega.privacy.android.feature.sync.ui.mapper.sync

import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

/**
 * Mapper for validating remote folder selection for Sync/Backup against Camera/Media Uploads.
 *
 * Checks if the selected remote folder or any of its ancestors/descendants are already
 * selected for Camera/Media Uploads on any device.
 */
class SyncRemoteFolderValidityMapper @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase,
) {

    /**
     * Validates remote folder selection for Sync/Backup against Camera/Media Uploads.
     *
     * Checks if the selected remote folder or any of its ancestors/descendants are already
     * selected for Camera/Media Uploads.
     *
     * @param nodeId The selected remote folder node ID
     * @return [SyncValidityResult] indicating validation outcome
     */
    suspend operator fun invoke(nodeId: NodeId): SyncValidityResult {
        // Check if DCIMSelectionAsSyncBackup feature flag is enabled
        val isFeatureEnabled = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup)
        }.getOrElse {
            Timber.e(it, "Error checking feature flag")
            false
        }

        if (!isFeatureEnabled) {
            // Feature flag is disabled, skip Camera/Media Uploads validation
            return SyncValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(""),
                folderName = ""
            )
        }

        // Use the domain use case to check folder usage
        // We only check Camera/Media Uploads (not Sync/Backup) for remote folder selection
        // and exclude current device since we're checking cross-device conflicts
        return runCatching {
            val result = isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId = nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false,
            )

            mapFolderUsageResultToSyncValidityResult(result)
        }.getOrElse { exception ->
            Timber.e(exception, "Error validating remote folder")
            SyncValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(""),
                folderName = ""
            )
        }
    }

    /**
     * Maps [FolderUsageResult] to the appropriate [SyncValidityResult].
     * Only Camera/Media Uploads conflicts are relevant for remote folder selection.
     */
    private fun mapFolderUsageResultToSyncValidityResult(
        result: FolderUsageResult,
    ): SyncValidityResult = when (result) {
        // Camera Uploads conflicts
        is FolderUsageResult.UsedByCameraUpload,
        is FolderUsageResult.UsedByCameraUploadChild,
        is FolderUsageResult.UsedByCameraUploadParent,
            -> {
            Timber.d("Remote folder conflicts with Camera Uploads folder")
            SyncValidityResult.ShowSnackbar(
                messageResId = sharedR.string.error_folder_part_of_camera_uploads
            )
        }

        // Media Uploads conflicts
        is FolderUsageResult.UsedByMediaUpload,
        is FolderUsageResult.UsedByMediaUploadChild,
        is FolderUsageResult.UsedByMediaUploadParent,
            -> {
            Timber.d("Remote folder conflicts with Media Uploads folder")
            SyncValidityResult.ShowSnackbar(
                messageResId = sharedR.string.error_folder_part_of_media_uploads
            )
        }

        // Sync/Backup conflicts on other devices
        is FolderUsageResult.UsedBySyncOrBackup,
        is FolderUsageResult.UsedBySyncOrBackupParent,
        is FolderUsageResult.UsedBySyncOrBackupChild,
            -> {
            Timber.d("Remote folder conflicts with Sync/Backup folder on another device")
            SyncValidityResult.ShowSnackbar(
                messageResId = sharedR.string.error_folder_part_of_sync_or_backup
            )
        }

        // No conflicts
        FolderUsageResult.NotUsed -> {
            SyncValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(""),
                folderName = ""
            )
        }
    }
}
