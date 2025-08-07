package mega.privacy.android.app.presentation.settings.startscreen.model

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Start screen settings state
 */
sealed interface StartScreenSettingsState {

    data object Loading : StartScreenSettingsState

    /**
     * Data class representing the state of start screen settings.
     *
     * @property options
     * @property selectedScreen
     */
    data class LegacyData(
        val options: List<StartScreenOption<StartScreen>>,
        val selectedScreen: StartScreen?,
    ) : StartScreenSettingsState

    /**
     * Data class representing the state of start screen settings.
     *
     * @property options
     * @property selectedScreen
     */
    data class Data(
        val options: List<StartScreenOption<NavKey>>,
        val selectedScreen: NavKey?,
    ) : StartScreenSettingsState

}