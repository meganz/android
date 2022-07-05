package mega.privacy.android.app.presentation.settings.chat.model

import mega.privacy.android.domain.entity.ChatImageQuality

/**
 * Data class representing the state of the chat settings.
 *
 * @property imageQuality   Current chat image quality.
 */
data class SettingsChatState(val imageQuality: ChatImageQuality? = null)
