package mega.privacy.android.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.megachat.ChatActivity]
 *
 * @property isConnected True if the app has some network connection, false otherwise.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    monitorConnectivity: MonitorConnectivity,
) : ViewModel() {

    /**
     * Get latest [StorageState] from [MonitorStorageStateEvent] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEvent.getState()

    val isConnected =
        monitorConnectivity().stateIn(viewModelScope, SharingStarted.Eagerly, false).value
}