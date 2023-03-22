package mega.privacy.android.app.presentation.contact

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.FileContactListActivity]
 */
@HiltViewModel
class FileContactListViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
) : ViewModel() {
    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()
}