package mega.privacy.android.data.mapper.backup

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Mapper that converts a [MegaRequest] into an [Backup]
 */
internal class BackupMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param megaRequest [MegaRequest]
     * @return  [Backup]
     */
    operator fun invoke(megaRequest: MegaRequest) = with(megaRequest) {
        Backup(
            backupId = parentHandle,
            backupType = totalBytes.toInt(),
            targetNode = nodeHandle,
            localFolder = file,
            backupName = name,
            state = BackupState.fromValue(access),
            subState = numDetails,
            extraData = INVALID_NON_NULL_VALUE,
            targetFolderPath = INVALID_NON_NULL_VALUE
        )
    }

    private companion object {
        const val INVALID_NON_NULL_VALUE = "-1"
    }
}
