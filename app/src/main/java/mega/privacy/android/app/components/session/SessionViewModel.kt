package mega.privacy.android.app.components.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.login.CheckChatSessionUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SessionViewModel @Inject constructor(
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val checkChatSessionUseCase: CheckChatSessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state = _state.asStateFlow()

    // important don't call 2 use cases in parallel
    // it can cause refresh session hang, I'm not sure why
    fun checkSdkSession(shouldCheckChatSession: Boolean = false) {
        viewModelScope.launch {
            if (shouldCheckChatSession) {
                runCatching {
                    checkChatSessionUseCase()
                }.onSuccess {
                    _state.update { state ->
                        state.copy(isChatSessionValid = true)
                    }
                }.onFailure {
                    Timber.e(it, "Failed to check chat session")
                    _state.update { state ->
                        state.copy(isChatSessionValid = false)
                    }
                }
            }

            runCatching {
                rootNodeExistsUseCase()
            }.onSuccess {
                _state.update { state ->
                    state.copy(isRootNodeExists = it)
                }
            }.onFailure {
                Timber.e(it, "Failed to check if root node exists")
                _state.update { state ->
                    state.copy(isRootNodeExists = false)
                }
            }
        }
    }
}