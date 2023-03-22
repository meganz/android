package mega.privacy.android.app.presentation.clouddrive

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import javax.inject.Inject

/**
 * View Model class for [mega.privacy.android.app.main.FileLinkActivity]
 */
@HiltViewModel
class FileLinkViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {
    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value
}