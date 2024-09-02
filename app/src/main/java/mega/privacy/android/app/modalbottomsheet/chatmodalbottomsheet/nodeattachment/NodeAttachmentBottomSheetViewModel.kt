package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model.ChatAttachmentUiEntity
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model.NodeAttachmentBottomSheetUiState
import mega.privacy.android.domain.usecase.chat.IsAnonymousModeUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model class for [NodeAttachmentBottomSheetDialogFragment]
 */
@HiltViewModel
internal class NodeAttachmentBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val isAnonymousModeUseCase: IsAnonymousModeUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    /**
     * Node Id from [SavedStateHandle]
     */
    val chatId = savedStateHandle.get<Long>(CHAT_ID) ?: -1L
    val messageId = savedStateHandle.get<Long>(MESSAGE_ID) ?: -1L

    /**
     * private mutable UI state
     */
    private val _uiState = MutableStateFlow(NodeAttachmentBottomSheetUiState())

    /**
     * public immutable UI State for view
     */
    val uiState = _uiState.asStateFlow()

    init {
        monitorConnectivity()
        loadChatFile()
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collectLatest { isOnline -> _uiState.update { it.copy(isOnline = isOnline) } }
        }
    }

    private fun loadChatFile() {
        viewModelScope.launch {
            runCatching {
                val chatFile = getChatFileUseCase(chatId, messageId, 0)
                requireNotNull(chatFile)
                _uiState.update {
                    it.copy(
                        item = ChatAttachmentUiEntity(
                            nodeId = chatFile.id,
                            name = chatFile.name,
                            size = chatFile.size,
                            thumbnailPath = chatFile.thumbnailPath,
                            isInAnonymousMode = isAnonymousModeUseCase(),
                            isAvailableOffline = isAvailableOfflineUseCase(chatFile)
                        ),
                        isLoading = false
                    )
                }
            }.onFailure {
                handleError()
            }
        }
    }

    private fun handleError() {
        _uiState.update {
            it.copy(
                errorEvent = triggered(true)
            )
        }
    }

    fun onErrorEventConsumed() =
        _uiState.update { state -> state.copy(errorEvent = consumed()) }

    companion object {
        const val CHAT_ID = "chat_id"
        const val MESSAGE_ID = "message_id"
    }
}

