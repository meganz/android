package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesException
import mega.privacy.android.domain.exception.login.FetchNodesUnknownStatus

internal val FetchNodesException.error: Int
    get() = when (this) {
        is FetchNodesErrorAccess -> megaException.getErrorStringId()
        is FetchNodesUnknownStatus -> megaException.getErrorStringId()
        else -> R.string.general_error
    }