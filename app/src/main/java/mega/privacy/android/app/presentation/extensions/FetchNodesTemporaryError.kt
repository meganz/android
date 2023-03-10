package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.login.FetchNodesTemporaryError

internal val FetchNodesTemporaryError.messageId: Int
    get() = when (this) {
        FetchNodesTemporaryError.ConnectivityIssues -> R.string.login_connectivity_issues
        FetchNodesTemporaryError.ServerIssues -> R.string.login_servers_busy
        FetchNodesTemporaryError.APILock -> R.string.login_API_lock
        FetchNodesTemporaryError.APIRate -> R.string.login_API_rate
    }