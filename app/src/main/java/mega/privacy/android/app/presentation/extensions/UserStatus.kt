package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.UserStatus

internal val UserStatus.color: Int
    get() = when (this) {
        UserStatus.Away -> R.color.orange_400
        UserStatus.Online -> R.color.lime_green_500
        UserStatus.Busy -> R.color.salmon_700
        else -> R.color.grey_700
    }

internal val UserStatus.text: Int
    get() = when (this) {
        UserStatus.Away -> R.string.away_status
        UserStatus.Online -> R.string.online_status
        UserStatus.Busy -> R.string.busy_status
        else -> R.string.offline_status
    }