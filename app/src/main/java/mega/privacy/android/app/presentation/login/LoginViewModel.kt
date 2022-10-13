package mega.privacy.android.app.presentation.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import javax.inject.Inject

/**
 * View Model for [LoginFragment]
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
) : ViewModel() {

    /**
     * Get latest value of [StorageState]
     */
    fun getStorageState() = monitorStorageStateEvent.getState()
}