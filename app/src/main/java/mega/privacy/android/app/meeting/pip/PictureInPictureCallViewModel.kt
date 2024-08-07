package mega.privacy.android.app.meeting.pip

import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.call.GetCallRemoteVideoUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLocalVideoUpdatesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel to handle Picture in Picture call screen
 */
@HiltViewModel
class PictureInPictureCallViewModel @Inject constructor(
    private val getCallRemoteVideoUpdatesUseCase: GetCallRemoteVideoUpdatesUseCase,
    private val getChatLocalVideoUpdatesUseCase: GetChatLocalVideoUpdatesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PictureInPictureCallUiState())

    /**
     * UI state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * set chat id
     */
    fun setChatId(chatId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(chatId = chatId) }
        }

    }

    /**
     * set current speaker id
     */
    fun setClientAndPeerId(clientId: Long, peerId: Long) {
        if (_uiState.value.clientId == clientId && uiState.value.peerId == peerId) return
        viewModelScope.launch {
            _uiState.update { it.copy(clientId = clientId, peerId = peerId) }
        }
    }

    /**
     * get video updates of chat
     */
    fun getVideoUpdates(): Flow<Pair<Size, ByteArray>> {
        _uiState.value.run {
            val flow = if (clientId == -1L) {
                getChatLocalVideoUpdatesUseCase(chatId)
            } else {
                getCallRemoteVideoUpdatesUseCase(chatId, clientId, true)
            }
            return flow.onCompletion {
                Timber.d("Flow Completed for Client $clientId and Peer $peerId")
            }.map {
                Timber.d("Flow Mapped for Client $clientId and Peer $peerId  $it")
                Size(it.width, it.height) to it.byteBuffer
            }
        }
    }

    /**
     * cancel video updates job
     */
    fun cancelVideUpdates() {
        _uiState.update { it.copy(isVideoOn = false) }
    }

    /**
     * cancel video updates job
     */
    fun showVideoUpdates() {
        if (_uiState.value.isVideoOn) return
        _uiState.update { it.copy(isVideoOn = true) }
    }
}
