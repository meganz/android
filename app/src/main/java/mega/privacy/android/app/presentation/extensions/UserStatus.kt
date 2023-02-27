package mega.privacy.android.app.presentation.extensions

import androidx.annotation.DrawableRes
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

@DrawableRes
internal fun UserStatus.iconRes(isLightTheme: Boolean): Int =
    when (this) {
        UserStatus.Online ->
            if (isLightTheme) R.drawable.ic_online_light
            else R.drawable.ic_online_dark_standard
        UserStatus.Away ->
            if (isLightTheme) R.drawable.ic_away_light
            else R.drawable.ic_away_dark_standard
        UserStatus.Busy ->
            if (isLightTheme) R.drawable.ic_busy_light
            else R.drawable.ic_busy_dark_standard
        else ->
            if (isLightTheme) R.drawable.ic_offline_light
            else R.drawable.ic_offline_dark_standard
    }
