package mega.privacy.android.feature.sync.ui.mapper.sync

import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.ui.formatter.FolderConflictMessageFormatter
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.SyncRemoteFolderConflictEvent
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
    private val folderConflictMessageFormatter: FolderConflictMessageFormatter,
) {

    /**
     * @param nodeId The selected remote folder node ID
     * @param remoteFolderDisplayName Display name of the selected remote folder (for conflict copy)
     */
    suspend operator fun invoke(
        nodeId: NodeId,
        remoteFolderDisplayName: String,
    ): SyncValidityResult {
        val isFeatureEnabled = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup)
        }.getOrElse {
            Timber.e(it, "Error checking feature flag")
            false
        }

        if (!isFeatureEnabled) {
            return SyncValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(""),
                folderName = ""
            )
        }

        return runCatching {
            val result = isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId = nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false,
            )

            mapFolderUsageResultToSyncValidityResult(result, remoteFolderDisplayName)
        }.getOrElse { exception ->
            Timber.e(exception, "Error validating remote folder")
            SyncValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(""),
                folderName = ""
            )
        }
    }

    private fun mapFolderUsageResultToSyncValidityResult(
        result: FolderUsageResult,
        remoteFolderDisplayName: String,
    ): SyncValidityResult = when (result) {
        is FolderUsageResult.UsedByCameraUpload,
        is FolderUsageResult.UsedByCameraUploadChild,
        is FolderUsageResult.UsedByCameraUploadParent,
            -> {
            Timber.d("Remote folder conflicts with Camera Uploads folder")
            Analytics.tracker.trackEvent(SyncRemoteFolderConflictEvent)
            SyncValidityResult.ShowSnackbarMessage(
                folderConflictMessageFormatter.formatFromFolderUsage(
                    folderDisplayName = remoteFolderDisplayName,
                    folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
                    result = result,
                ).orEmpty()
            )
        }

        is FolderUsageResult.UsedByMediaUpload,
        is FolderUsageResult.UsedByMediaUploadChild,
        is FolderUsageResult.UsedByMediaUploadParent,
            -> {
            Timber.d("Remote folder conflicts with Media Uploads folder")
            Analytics.tracker.trackEvent(SyncRemoteFolderConflictEvent)
            SyncValidityResult.ShowSnackbarMessage(
                folderConflictMessageFormatter.formatFromFolderUsage(
                    folderDisplayName = remoteFolderDisplayName,
                    folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
                    result = result,
                ).orEmpty()
            )
        }

        is FolderUsageResult.UsedBySyncOrBackup,
        is FolderUsageResult.UsedBySyncOrBackupParent,
        is FolderUsageResult.UsedBySyncOrBackupChild,
            -> {
            Timber.d("Remote folder conflicts with Sync/Backup folder on another device")
            Analytics.tracker.trackEvent(SyncRemoteFolderConflictEvent)
            SyncValidityResult.ShowSnackbarMessage(
                folderConflictMessageFormatter.formatFromFolderUsage(
                    folderDisplayName = remoteFolderDisplayName,
                    folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
                    result = result,
                ).orEmpty()
            )
        }

        FolderUsageResult.NotUsed -> {
            SyncValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(""),
                folderName = ""
            )
        }
    }
}
