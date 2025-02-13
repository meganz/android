package mega.privacy.android.app.components.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.chat.RetryConnectionsAndSignalPresenceUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SessionViewModel @Inject constructor(
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val retryConnectionsAndSignalPresenceUseCase: RetryConnectionsAndSignalPresenceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state = _state.asStateFlow()

    private var retryConnectionsAndSignalPresenceJob: Job? = null

    fun checkSdkSession() {
        viewModelScope.launch {
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

    fun retryConnectionsAndSignalPresence() {
        // Prevent multiple calls at the same time and add a delay to avoid too frequent consecutive calls in a short time frame.
        if (retryConnectionsAndSignalPresenceJob?.isActive == true) return
        retryConnectionsAndSignalPresenceJob = viewModelScope.launch {
            Timber.d("Retry connections and signal presence")
            retryConnectionsAndSignalPresenceUseCase()
            delay(500L)
        }
    }
}