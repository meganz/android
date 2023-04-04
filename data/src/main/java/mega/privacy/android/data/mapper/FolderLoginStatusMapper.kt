package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Map [MegaError] to [FolderLoginStatus]
 */
internal class FolderLoginStatusMapper @Inject constructor() {

    operator fun invoke(error: MegaError): FolderLoginStatus {
        return when (error.errorCode) {
            MegaError.API_OK -> FolderLoginStatus.SUCCESS
            MegaError.API_EINCOMPLETE -> FolderLoginStatus.API_INCOMPLETE
            MegaError.API_EARGS -> FolderLoginStatus.INCORRECT_KEY
            else -> FolderLoginStatus.ERROR
        }
    }
}
