package mega.privacy.android.app.presentation.settings.startSceen.model

import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Start screen settings state
 *
 * @property options
 * @property selectedScreen
 */
data class StartScreenSettingsState(
    val options: List<StartScreenOption>,
    val selectedScreen: StartScreen,
)
