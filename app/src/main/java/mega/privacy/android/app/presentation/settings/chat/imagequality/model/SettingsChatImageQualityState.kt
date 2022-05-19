package mega.privacy.android.app.presentation.settings.chat.imagequality.model

import mega.privacy.android.app.domain.entity.ChatImageQuality

/**
 * Data class representing the state of the chat image quality setting.
 *
 * @property selectedQuality    Current chat image quality.
 * @property options            List of available options.
 */
data class SettingsChatImageQualityState(
    val selectedQuality: ChatImageQuality? = null,
    val options: List<ChatImageQuality> = ChatImageQuality.values().asList()
)
