package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.login.TemporaryWaitingError

internal val TemporaryWaitingError.messageId: Int
    get() = when (this) {
        TemporaryWaitingError.ConnectivityIssues -> R.string.login_connectivity_issues
        TemporaryWaitingError.ServerIssues -> R.string.login_servers_busy
        TemporaryWaitingError.APILock -> R.string.login_API_lock
        TemporaryWaitingError.APIRate -> R.string.login_API_rate
    }