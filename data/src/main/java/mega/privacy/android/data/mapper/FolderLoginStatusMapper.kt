package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import nz.mega.sdk.MegaError

/**
 * Map [MegaError] to [FolderLoginStatus]
 */
typealias FolderLoginStatusMapper = (@JvmSuppressWildcards MegaError) -> @JvmSuppressWildcards FolderLoginStatus

internal fun toFolderLoginStatus(error: MegaError): FolderLoginStatus {
    return when (error.errorCode) {
        MegaError.API_OK -> FolderLoginStatus.SUCCESS
        MegaError.API_EINCOMPLETE -> FolderLoginStatus.API_INCOMPLETE
        MegaError.API_EARGS -> FolderLoginStatus.INCORRECT_KEY
        else -> FolderLoginStatus.ERROR
    }
}
