package mega.privacy.android.app.presentation.bottomsheet

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.modalbottomsheet.QRCodeSaveBottomSheetDialogFragment]
 */
@HiltViewModel
class QrCodeSaveBottomSheetDialogViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
) : ViewModel() {

    /**
     * Get latest value of [StorageState]
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()
}