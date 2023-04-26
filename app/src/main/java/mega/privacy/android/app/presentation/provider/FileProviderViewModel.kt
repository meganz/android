package mega.privacy.android.app.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.providers.FileProviderActivity]
 */
@HiltViewModel
class FileProviderViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val stopCameraUploadUseCase: StopCameraUploadUseCase,
) : ViewModel() {

    /**
     * Get latest value of [StorageState]
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()

    /**
     * Stop camera upload
     */
    fun stopCameraUpload() = viewModelScope.launch {
        stopCameraUploadUseCase()
    }

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value
}
