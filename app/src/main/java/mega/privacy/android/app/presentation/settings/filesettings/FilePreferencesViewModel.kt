package mega.privacy.android.app.presentation.settings.filesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.filesettings.model.FilePreferencesState
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetFolderVersionInfo
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import javax.inject.Inject

@HiltViewModel
class FilePreferencesViewModel @Inject constructor(
    private val getFolderVersionInfo: GetFolderVersionInfo,
    private val monitorConnectivity: MonitorConnectivity,
    private val getFileVersionsOption: GetFileVersionsOption,
    private val monitorUserUpdates: MonitorUserUpdates,
) : ViewModel() {
    private val _state = MutableStateFlow(FilePreferencesState())

    /**
     * State
     */
    val state = _state.asStateFlow()

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
            _state.update {
                it.copy(
                    numberOfPreviousVersions = info?.numberOfVersions,
                    sizeOfPreviousVersionsInBytes = info?.sizeOfPreviousVersionsInBytes
                )
            }
        }
        viewModelScope.launch {
            monitorUserUpdates()
                .filter { it == UserChanges.DisableVersions }
                .collect {
                    getFileVersionOption()
                }
        }
        getFileVersionOption()
    }

    private fun getFileVersionOption() {
        viewModelScope.launch {
            val isDisableFileVersions = getFileVersionsOption(foreRefresh = true)
            _state.update { it.copy(isFileVersioningEnabled = isDisableFileVersions.not()) }
        }
    }
}