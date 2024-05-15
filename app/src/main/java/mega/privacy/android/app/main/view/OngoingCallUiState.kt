package mega.privacy.android.app.main.view

import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatCall

/**
 * Call in progress UI state
 *
 * @property currentCall Current call
 * @property isShown Is show
 * @property themeMode Theme mode
 */
data class OngoingCallUiState(
    val currentCall: ChatCall? = null,
    val isShown: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System
)