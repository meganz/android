package mega.privacy.android.app.presentation.settings.chat.model

import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig

/**
 * Data class representing the state of the chat settings.
 *
 * @property imageQuality   Current chat image quality.
 * @property isRichLinkEnabled   Whether rich link is enabled.
 * @property chatVideoQuality   Current chat video (attachment) upload quality, expressed as the
 * [mega.privacy.android.domain.entity.VideoQuality] value. Null until loaded.
 * @property chatPresenceConfig   Current chat presence configuration. Null until loaded.
 * @property rootNodeExists   Whether the root node exists.
 */
data class SettingsChatState(
    val imageQuality: ChatImageQuality? = null,
    val isRichLinkEnabled: Boolean = false,
    val chatVideoQuality: Int? = null,
    val chatPresenceConfig: ChatPresenceConfig?,
    val rootNodeExists: Boolean,
)
