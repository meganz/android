package mega.privacy.android.app.presentation.settings.startscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOptionMapper
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenSettingsState
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.SetStartScreenPreference
import javax.inject.Inject

@HiltViewModel
class StartScreenViewModel @Inject constructor(
    private val monitorStartScreenPreference: MonitorStartScreenPreference,
    private val setStartScreenPreference: SetStartScreenPreference,
    startScreenOptionMapper: StartScreenOptionMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(StartScreenSettingsState(
        options = StartScreen.values()
            .mapNotNull(startScreenOptionMapper),
        selectedScreen = null,
    ))

    val state: StateFlow<StartScreenSettingsState> = _state

    init {
        viewModelScope.launch {
            monitorStartScreenPreference()
                .collect { screen ->
                    _state.update { it.copy(selectedScreen = screen) }
                }
        }
    }

    fun newScreenClicked(newScreen: StartScreen) {
        viewModelScope.launch {
            setStartScreenPreference(newScreen)
        }
    }
}