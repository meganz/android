package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.backup.GetBackupInfoUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadsSyncHandlesUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderUsageResult
import javax.inject.Inject

class IsFolderUsedBySyncOrBackupAcrossDevicesUseCase @Inject constructor(
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val getCameraUploadsSyncHandlesUseCase: GetCameraUploadsSyncHandlesUseCase,
) {
    suspend operator fun invoke(nodeId: NodeId): FolderUsageResult {
        // Check if the folder is used by Camera Uploads
        getCameraUploadsSyncHandlesUseCase()?.let { (primaryHandle, secondaryHandle) ->
            if (nodeId.longValue == primaryHandle || nodeId.longValue == secondaryHandle) {
                return FolderUsageResult.UsedByCameraUpload
            }
        }

        // If not, check if it's used by any backup across devices
        return getBackupInfoUseCase()
            .find { it.rootHandle == nodeId }
            ?.let { matchingBackup -> FolderUsageResult.UsedBySyncOrBackup(matchingBackup.deviceId) }
            ?: FolderUsageResult.NotUsed
    }
}
