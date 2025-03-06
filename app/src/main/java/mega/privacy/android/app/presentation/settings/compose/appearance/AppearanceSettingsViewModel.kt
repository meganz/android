package mega.privacy.android.app.presentation.settings.compose.appearance

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.presentation.settings.compose.appearance.model.AppearanceSettingsState
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor() : ViewModel() {
    val state: StateFlow<AppearanceSettingsState>
        field: MutableStateFlow<AppearanceSettingsState> = MutableStateFlow(
            AppearanceSettingsState.Data(
                themeMode = "Theme mode",
                showHiddenItems = false,
            )
        )

    fun onShowHiddenItemsToggled(newValue: Boolean) {
        state.value = (state.value as AppearanceSettingsState.Data).copy(showHiddenItems = newValue)
    }

    fun onThemeModeSelected(mode: String) {
        state.value = (state.value as AppearanceSettingsState.Data).copy(themeMode = mode)
    }
}