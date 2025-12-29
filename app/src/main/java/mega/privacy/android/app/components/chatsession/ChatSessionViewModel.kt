package mega.privacy.android.app.components.chatsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.CheckChatSessionUseCase
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ChatSessionViewModel @Inject constructor(
    private val checkChatSessionUseCase: CheckChatSessionUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatSessionUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.SingleActivity)
            }.onSuccess {
                _state.update { state ->
                    state.copy(isSingleActivityEnabled = it)
                }
            }.onFailure {
                Timber.e(it, "Failed to check if single activity feature is enabled")
            }
        }
    }

    /**
     * Check if Chat SDK session exists
     * @param optimistic If true, assumes that the SDK session exists while waiting for a response. That way it starts showing the content immediately
     */
    fun checkChatSession(optimistic: Boolean = false) {
        if (optimistic && _state.value.sessionState == ChatSessionState.Pending) {
            _state.update { state ->
                state.copy(sessionState = ChatSessionState.Valid)
            }
        }
        viewModelScope.launch {
            runCatching {
                checkChatSessionUseCase()
            }.onSuccess {
                _state.update { state ->
                    state.copy(sessionState = ChatSessionState.Valid)
                }
            }.onFailure {
                Timber.e(it, "Failed to refresh chat session")
                _state.update { state ->
                    state.copy(sessionState = ChatSessionState.Invalid)
                }
            }
        }
    }
}