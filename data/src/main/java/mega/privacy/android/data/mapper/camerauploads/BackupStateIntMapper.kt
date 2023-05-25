package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.BackupState
import javax.inject.Inject

/**
 * Mapper that converts a [BackupState] into an [Integer]
 */
internal class BackupStateIntMapper @Inject constructor() {
    operator fun invoke(backupState: BackupState) = when (backupState) {
        BackupState.INVALID -> -1
        BackupState.ACTIVE -> 1
        BackupState.FAILED -> 2
        BackupState.TEMPORARILY_DISABLED -> 3
        BackupState.DISABLED -> 4
        BackupState.PAUSE_UPLOADS -> 5
        BackupState.PAUSE_DOWNLOADS -> 6
        BackupState.PAUSE_ALL -> 7
        BackupState.DELETED -> 8
    }
}
