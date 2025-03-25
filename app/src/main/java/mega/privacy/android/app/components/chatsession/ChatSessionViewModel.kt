package mega.privacy.android.app.components.chatsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.login.CheckChatSessionUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ChatSessionViewModel @Inject constructor(
    private val checkChatSessionUseCase: CheckChatSessionUseCase,
) : ViewModel() {

    private val _state: MutableStateFlow<ChatSessionState> =
        MutableStateFlow(ChatSessionState.Pending)
    val state = _state.asStateFlow()

    fun checkChatSession() {
        viewModelScope.launch {
            runCatching {
                checkChatSessionUseCase()
            }.onSuccess {
                _state.value = ChatSessionState.Valid
            }.onFailure {
                Timber.e(it, "Failed to refresh chat session")
                _state.value = ChatSessionState.Invalid
            }
        }
    }
}