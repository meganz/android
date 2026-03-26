package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class MonitorCrossDeviceFolderConflictsUseCase @Inject constructor(
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    private companion object {
        val POLL_INTERVAL = 10.minutes
    }

    operator fun invoke(): Flow<FolderUsageResult?> = flow {
        if (!isFeatureEnabled()) {
            emit(null)
            return@flow
        }

        while (true) {
            val conflict = findConflict()
            emit(conflict)
            delay(POLL_INTERVAL)
        }
    }

    private suspend fun isFeatureEnabled(): Boolean {
        return getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup)
    }

    /**
     * Checks both primary and secondary folders for conflicts.
     * Returns the first conflict found, or null if everything is valid.
     */
    private suspend fun findConflict(): FolderUsageResult? = runCatching {
        // 1. Check Primary Folder
        val primaryHandle = getUploadFolderHandleUseCase(CameraUploadFolderType.Primary)
        val primaryResult = checkFolderUsage(primaryHandle)

        if (isConflictForPrimary(primaryResult)) {
            return@runCatching primaryResult
        }

        // 2. Check Secondary Folder (if enabled)
        if (isMediaUploadsEnabledUseCase()) {
            val secondaryHandle = getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)
            val secondaryResult = checkFolderUsage(secondaryHandle)

            if (isConflictForSecondary(secondaryResult)) {
                return@runCatching secondaryResult
            }
        }

        null // No conflicts found
    }.getOrNull()

    private suspend fun checkFolderUsage(handle: Long): FolderUsageResult {
        return isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
            nodeId = NodeId(handle),
            isSyncFolderSelection = false,
            shouldExcludeCurrentDevice = false,
            useCache = true,
        )
    }

    private fun isConflictForPrimary(result: FolderUsageResult): Boolean {
        // It is NOT a conflict if it's unused, or used by valid Camera Upload types
        val isValidUsage = result is FolderUsageResult.NotUsed ||
                result is FolderUsageResult.UsedByCameraUpload ||
                result is FolderUsageResult.UsedByCameraUploadParent ||
                result is FolderUsageResult.UsedByCameraUploadChild

        return !isValidUsage
    }

    private fun isConflictForSecondary(result: FolderUsageResult): Boolean {
        // It is NOT a conflict if it's unused, or used by valid Media Upload types
        val isValidUsage = result is FolderUsageResult.NotUsed ||
                result is FolderUsageResult.UsedByMediaUpload ||
                result is FolderUsageResult.UsedByMediaUploadParent ||
                result is FolderUsageResult.UsedByMediaUploadChild

        return !isValidUsage
    }
}
