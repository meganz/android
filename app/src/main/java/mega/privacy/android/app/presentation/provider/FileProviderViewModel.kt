package mega.privacy.android.app.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.providers.FileProviderActivity]
 */
@HiltViewModel
class FileProviderViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
) : ViewModel() {

    /**
     * Get latest value of [StorageState]
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()

    /**
     * Stop camera uploads
     */
    fun stopCameraUploads() = viewModelScope.launch {
        runCatching { stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable) }
            .onFailure { Timber.d(it) }
    }

    /**
     * Is connected
     */
    fun isConnected() = isConnectedToInternetUseCase()
}
