package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.FetchFolderNodesException

internal val FetchFolderNodesException.errorDialogTitleId: Int
    get() = when (this) {
        is FetchFolderNodesException.AccountTerminated -> R.string.general_error_folder_not_found
        is FetchFolderNodesException.LinkRemoved -> R.string.general_error_folder_not_found
        is FetchFolderNodesException.InvalidDecryptionKey -> R.string.general_error_word
        else -> R.string.general_error_word
    }

internal val FetchFolderNodesException.errorDialogContentId: Int
    get() = when (this) {
        is FetchFolderNodesException.AccountTerminated -> R.string.file_link_unavaible_delete_account
        is FetchFolderNodesException.LinkRemoved -> R.string.folder_link_unavaible_ToS_violation
        is FetchFolderNodesException.InvalidDecryptionKey -> R.string.general_error_invalid_decryption_key
        else -> R.string.general_error_folder_not_found
    }

internal val FetchFolderNodesException.snackBarMessageId: Int
    get() = R.string.general_error_folder_not_found