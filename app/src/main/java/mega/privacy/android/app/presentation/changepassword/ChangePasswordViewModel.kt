package mega.privacy.android.app.presentation.changepassword

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.MonitorConnectivity
import javax.inject.Inject

@HiltViewModel
internal class ChangePasswordViewModel @Inject constructor(
    private val monitorConnectivity: MonitorConnectivity,
) : ViewModel() {
    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value
}