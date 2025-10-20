package mega.privacy.android.app.presence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.chat.RetryConnectionsAndSignalPresenceUseCase
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SignalPresenceViewModel @Inject constructor(
    private val retryConnectionsAndSignalPresenceUseCase: RetryConnectionsAndSignalPresenceUseCase,
) : ViewModel() {
    private val updatePresenceFlow: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private var presenceJob: Job? = null

    private fun trackPresence() {
        presenceJob = viewModelScope.launch {
            updatePresenceFlow
                .filterNotNull()
                .debounce(500L)
                .onStart {
                    Timber.d("Signaling presence due to presence flow start")
                    sendPresenceSignal()
                }
                .collect {
                    Timber.d("Signaling presence due to update presence flow")
                    sendPresenceSignal()
                }
        }
    }

    private suspend fun sendPresenceSignal() {
        try {
            retryConnectionsAndSignalPresenceUseCase()
        } catch (e: Exception) {
            Timber.e(e, "Error signaling presence")
        }
    }

    fun signalPresence() {
        if (presenceJob == null) {
            trackPresence()
        } else {
            updatePresenceFlow.update { it?.let { !it } ?: true }
        }
    }

}