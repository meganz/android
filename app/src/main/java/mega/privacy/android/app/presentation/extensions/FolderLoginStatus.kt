package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus

internal val FolderLoginStatus.errorDialogTitleId: Int
    get() = R.string.general_error_word

internal val FolderLoginStatus.errorDialogContentId: Int
    get() = when (this) {
        FolderLoginStatus.INCORRECT_KEY -> R.string.link_broken
        else -> R.string.general_error_folder_not_found
    }

internal val FolderLoginStatus.snackBarMessageId: Int
    get() = R.string.general_error_folder_not_found