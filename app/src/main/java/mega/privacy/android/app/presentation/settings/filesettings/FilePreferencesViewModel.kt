package mega.privacy.android.app.presentation.settings.filesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.filesettings.model.FilePreferencesState
import mega.privacy.android.domain.usecase.GetFolderVersionInfo
import mega.privacy.android.domain.usecase.MonitorConnectivity
import javax.inject.Inject

@HiltViewModel
class FilePreferencesViewModel @Inject constructor(
    private val getFolderVersionInfo: GetFolderVersionInfo,
    private val monitorConnectivity: MonitorConnectivity,
) : ViewModel() {
    private val state = MutableStateFlow(FilePreferencesState())
    val filePreferencesState: StateFlow<FilePreferencesState> = state
    val filePreferencesStateLiveData = state.asLiveData()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    init {
        viewModelScope.launch {
            val info = getFolderVersionInfo()
            state.update {
                it.copy(
                    numberOfPreviousVersions = info?.numberOfVersions,
                    sizeOfPreviousVersionsInBytes = info?.sizeOfPreviousVersionsInBytes
                )
            }
        }
    }

}