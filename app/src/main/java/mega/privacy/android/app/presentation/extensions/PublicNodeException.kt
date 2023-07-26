package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.PublicNodeException

internal val PublicNodeException.errorDialogTitleId: Int
    get() = when (this) {
        is PublicNodeException.LinkRemoved, is PublicNodeException.AccountTerminated -> {
            R.string.general_error_file_not_found
        }

        else -> {
            R.string.general_error_word
        }
    }

internal val PublicNodeException.errorDialogContentId: Int
    get() = when (this) {
        is PublicNodeException.AccountTerminated -> {
            R.string.file_link_unavaible_delete_account
        }

        is PublicNodeException.LinkRemoved -> {
            R.string.file_link_unavaible_ToS_violation
        }

        is PublicNodeException.InvalidDecryptionKey -> {
            R.string.link_broken
        }

        else -> {
            R.string.general_error_file_not_found
        }
    }