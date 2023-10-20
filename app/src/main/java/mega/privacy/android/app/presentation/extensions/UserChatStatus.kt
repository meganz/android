package mega.privacy.android.app.presentation.extensions

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.UserChatStatus

internal val UserChatStatus.color: Int
    get() = when (this) {
        UserChatStatus.Away -> R.color.orange_400
        UserChatStatus.Online -> R.color.lime_green_500
        UserChatStatus.Busy -> R.color.salmon_700
        else -> R.color.grey_700
    }

internal val UserChatStatus.text: Int
    get() = when (this) {
        UserChatStatus.Away -> R.string.away_status
        UserChatStatus.Online -> R.string.online_status
        UserChatStatus.Busy -> R.string.busy_status
        else -> R.string.offline_status
    }

@DrawableRes
internal fun UserChatStatus.vectorRes(isLightTheme: Boolean) =
    when (this) {
        UserChatStatus.Online ->
            if (isLightTheme) R.drawable.ic_online_user_chat_status_light
            else R.drawable.ic_online_user_chat_status_dark

        UserChatStatus.Away ->
            if (isLightTheme) R.drawable.ic_away_user_chat_status_light
            else R.drawable.ic_away_user_chat_status_dark

        UserChatStatus.Busy ->
            if (isLightTheme) R.drawable.ic_busy_user_chat_status_light
            else R.drawable.ic_busy_user_chat_status_dark

        else ->
            if (isLightTheme) R.drawable.ic_offline_user_chat_status_light
            else R.drawable.ic_offline_user_chat_status_dark
    }

@DrawableRes
internal fun UserChatStatus.iconRes(isLightTheme: Boolean): Int =
    when (this) {
        UserChatStatus.Online ->
            if (isLightTheme) R.drawable.ic_online_light
            else R.drawable.ic_online_dark_standard

        UserChatStatus.Away ->
            if (isLightTheme) R.drawable.ic_away_light
            else R.drawable.ic_away_dark_standard

        UserChatStatus.Busy ->
            if (isLightTheme) R.drawable.ic_busy_light
            else R.drawable.ic_busy_dark_standard

        else ->
            if (isLightTheme) R.drawable.ic_offline_light
            else R.drawable.ic_offline_dark_standard
    }

internal fun UserChatStatus.isValid() =
    this in arrayOf(
        UserChatStatus.Online,
        UserChatStatus.Away,
        UserChatStatus.Busy,
        UserChatStatus.Offline
    )

internal fun UserChatStatus.isAwayOrOffline() =
    this in arrayOf(UserChatStatus.Offline, UserChatStatus.Away)
