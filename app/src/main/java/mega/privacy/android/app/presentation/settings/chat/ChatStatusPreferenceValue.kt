package mega.privacy.android.app.presentation.settings.chat

import mega.privacy.android.domain.entity.contacts.UserChatStatus

private const val STATUS_OFFLINE_VALUE = 1
private const val STATUS_AWAY_VALUE = 2
private const val STATUS_ONLINE_VALUE = 3
private const val STATUS_BUSY_VALUE = 4

/**
 * Maps this [UserChatStatus] to the raw value persisted by the chat status list preference
 * (see R.array.settings_chat_status_values), or null if the status has no entry.
 */
internal fun UserChatStatus.toChatStatusPreferenceValue(): Int? = when (this) {
    UserChatStatus.Offline -> STATUS_OFFLINE_VALUE
    UserChatStatus.Away -> STATUS_AWAY_VALUE
    UserChatStatus.Online -> STATUS_ONLINE_VALUE
    UserChatStatus.Busy -> STATUS_BUSY_VALUE
    UserChatStatus.Invalid -> null
}

/**
 * Maps a raw chat status list preference value (see R.array.settings_chat_status_values) to
 * a [UserChatStatus], or null if the value is unknown.
 */
internal fun Int.toUserChatStatus(): UserChatStatus? = when (this) {
    STATUS_OFFLINE_VALUE -> UserChatStatus.Offline
    STATUS_AWAY_VALUE -> UserChatStatus.Away
    STATUS_ONLINE_VALUE -> UserChatStatus.Online
    STATUS_BUSY_VALUE -> UserChatStatus.Busy
    else -> null
}
