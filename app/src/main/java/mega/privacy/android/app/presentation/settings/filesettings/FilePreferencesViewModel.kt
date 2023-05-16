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
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.EnableFileVersionsOption
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FilePreferencesViewModel @Inject constructor(
    private val getFolderVersionInfo: GetFolderVersionInfo,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFileVersionsOption: GetFileVersionsOption,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val enableFileVersionsOption: EnableFileVersionsOption,
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
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

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
            runCatching { getFileVersionsOption(forceRefresh = true) }
                .onSuccess { isDisableFileVersions ->
                    _state.update { it.copy(isFileVersioningEnabled = isDisableFileVersions.not()) }
                }
        }
    }

    /**
     * Reset versions info
     *
     */
    fun resetVersionsInfo() {
        _state.update {
            it.copy(
                numberOfPreviousVersions = 0,
                sizeOfPreviousVersionsInBytes = 0
            )
        }
    }

    /**
     * Enable file version option
     *
     * @param enable
     */
    fun enableFileVersionOption(enable: Boolean) = viewModelScope.launch {
        runCatching { enableFileVersionsOption(enabled = enable) }
            .onSuccess { _state.update { it.copy(isFileVersioningEnabled = enable) } }
            .onFailure { Timber.w("Exception enabling file versioning.", it) }
    }
}