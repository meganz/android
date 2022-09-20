package mega.privacy.android.app.presentation.settings.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.app.presentation.settings.calls.model.SettingsCallsState
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.usecase.GetCallsSoundNotifications
import mega.privacy.android.domain.usecase.SetCallsSoundNotifications
import javax.inject.Inject

/**
 * View model for [SettingsCallsFragment].
 */
@HiltViewModel
class SettingsCallsViewModel @Inject constructor(
    private val getCallsSoundNotifications: GetCallsSoundNotifications,
    private val setCallsSoundNotifications: SetCallsSoundNotifications,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsCallsState())
    val uiState: StateFlow<SettingsCallsState> = _uiState


    init {
        viewModelScope.launch(ioDispatcher) {
            getCallsSoundNotifications().map { result ->
                { state: SettingsCallsState -> state.copy(soundNotifications = result) }
            }.collect {
                _uiState.update(it)
            }
        }
    }

    /**
     * Enable or disable sound notifications.
     *
     * @param soundNotifications The sound notifications status.
     */
    fun setNewCallsSoundNotifications(
        soundNotifications: CallsSoundNotifications,
    ) {
        viewModelScope.launch {
            kotlin.runCatching {
                setCallsSoundNotifications(soundNotifications)
            }
        }
    }

}