package mega.privacy.android.app.presentation.contact

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.ContactInfoActivity]
 */
@HiltViewModel
class ContactInfoViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
) : ViewModel() {
    /**
     * Get latest [StorageState] from [MonitorStorageStateEvent] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEvent.getState()
}