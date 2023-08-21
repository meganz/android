package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import javax.inject.Inject


/**
 * WaitingRoomActivity view model.
 *
 * @property monitorConnectivityUseCase                             [MonitorConnectivityUseCase]
 * @property state                                                  Current view state as [WaitingRoomState]
 */
@HiltViewModel
class WaitingRoomViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(WaitingRoomState())
    val state: StateFlow<WaitingRoomState> = _state

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Sets chat id
     *
     * @param newChatId                 Chat id.
     */
    fun setChatId(newChatId: Long) {
        if (newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
        }
    }

    fun onMicEnabled(enable: Boolean) {
        _state.update {
            it.copy(
                micEnabled = enable
            )
        }
    }

    fun onCameraEnabled(enable: Boolean) {
        _state.update {
            it.copy(
                cameraEnabled = enable
            )
        }
    }

    fun onSpeakerEnabled(enable: Boolean) {
        _state.update {
            it.copy(
                speakerEnabled = enable
            )
        }
    }
}