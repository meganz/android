package mega.privacy.android.app.presentation.settings.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.chat.model.SettingsChatState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import javax.inject.Inject

/**
 * View model for [SettingsChatFragment].
 */
@HiltViewModel
class SettingsChatViewModel @Inject constructor(
    private val getChatImageQuality: GetChatImageQuality,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsChatState())
    val state: StateFlow<SettingsChatState> = _state

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

    init {
        viewModelScope.launch(ioDispatcher) {
            getChatImageQuality().map { quality ->
                { state: SettingsChatState -> state.copy(imageQuality = quality) }
            }.collect {
                _state.update(it)
            }
        }
    }
}