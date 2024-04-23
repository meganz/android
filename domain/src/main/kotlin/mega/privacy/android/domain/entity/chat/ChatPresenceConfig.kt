package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Domain entity for MEGAChatPresenceConfig
 *
 * @property onlineStatus The online status specified in the settings.
 * @property isAutoAwayEnabled Whether the auto-away setting is enabled or disabled.
 * @property autoAwayTimeout Number of seconds to change the online status to away.
 * @property isPersist Indicates if the online status will persist after going offline and/or closing the app.
 * @property isPending Indicates if the presence configuration is pending to be confirmed by server.
 * @property isLastGreenVisible Indicates if the last green is visible to other users.
 */
data class ChatPresenceConfig(
    val onlineStatus: UserChatStatus,
    val isAutoAwayEnabled: Boolean,
    val autoAwayTimeout: Long,
    val isPersist: Boolean,
    val isPending: Boolean,
    val isLastGreenVisible: Boolean,
)
