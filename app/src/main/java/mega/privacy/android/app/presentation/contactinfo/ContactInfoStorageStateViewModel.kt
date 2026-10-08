package mega.privacy.android.app.presentation.contactinfo

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * Exposes the current [StorageState] to the Compose contact destinations so actions that upload
 * data (send file, share contact) can be gated behind the over disk quota paywall.
 */
@HiltViewModel
class ContactInfoStorageStateViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
) : ViewModel() {

    /**
     * Latest [StorageState] of the account.
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()
}
