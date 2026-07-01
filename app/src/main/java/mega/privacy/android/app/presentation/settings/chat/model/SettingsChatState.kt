package mega.privacy.android.app.presentation.settings.chat.model

import mega.privacy.android.domain.entity.ChatImageQuality

/**
 * Data class representing the state of the chat settings.
 *
 * @property imageQuality   Current chat image quality.
 * @property isRichLinkEnabled   Whether rich link is enabled.
 * @property chatVideoQuality   Current chat video (attachment) upload quality, expressed as the
 * [mega.privacy.android.domain.entity.VideoQuality] value. Null until loaded.
 */
data class SettingsChatState(
    val imageQuality: ChatImageQuality? = null,
    val isRichLinkEnabled: Boolean = false,
    val chatVideoQuality: Int? = null,
)
