package mega.privacy.android.feature.sync.domain.entity

sealed interface FolderUsageResult {
    object NotUsed : FolderUsageResult
    object UsedByCameraUpload : FolderUsageResult
    data class UsedBySyncOrBackup(val deviceId: String?) : FolderUsageResult
}
