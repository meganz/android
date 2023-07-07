package mega.privacy.android.app.main.dialog.chatstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.usecase.chat.GetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.chat.SetCurrentUserStatusUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal data class ChatStatusViewModel @Inject constructor(
    private val getCurrentUserStatusUseCase: GetCurrentUserStatusUseCase,
    private val setCurrentUserStatusUseCase: SetCurrentUserStatusUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatStatusUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { getCurrentUserStatusUseCase() }.onSuccess { status ->
                _state.update { it.copy(status = status) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun setUserStatus(status: UserStatus) {
        viewModelScope.launch {
            runCatching { setCurrentUserStatusUseCase(status) }
                .onFailure { Timber.e(it) }
            _state.update { it.copy(shouldDismiss = true) } // we don't care about result, this is previous logic
        }
    }
}

/**
 * Chat status ui state
 *
 * @property status
 * @property shouldDismiss
 */
data class ChatStatusUiState(
    val status: UserStatus = UserStatus.Invalid,
    val shouldDismiss: Boolean = false,
)