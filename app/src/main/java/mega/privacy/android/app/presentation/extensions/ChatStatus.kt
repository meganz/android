package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.chat.ChatStatus

internal val ChatStatus.text: Int?
    get() = when (this) {
        ChatStatus.Online -> R.string.online_status
        ChatStatus.Offline -> R.string.offline_status
        ChatStatus.Away -> R.string.away_status
        ChatStatus.Busy -> R.string.busy_status
        ChatStatus.NoNetworkConnection -> R.string.error_server_connection_problem
        ChatStatus.Reconnecting -> R.string.invalid_connection_state
        ChatStatus.Connecting -> R.string.chat_connecting
    }