package mega.privacy.android.app.fragments.settingsFragments.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.settingsFragments.download.model.DownloadSettingsState
import mega.privacy.android.domain.usecase.GetStorageDownloadAskAlways
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPath
import mega.privacy.android.domain.usecase.GetStorageDownloadLocation
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlways
import mega.privacy.android.domain.usecase.SetStorageDownloadLocation
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [DownloadSettingsFragment]
 */
@HiltViewModel
class DownloadSettingsViewModel @Inject constructor(
    private val setStorageDownloadAskAlways: SetStorageDownloadAskAlways,
    private val setStorageDownloadLocation: SetStorageDownloadLocation,
    private val getStorageDownloadAskAlways: GetStorageDownloadAskAlways,
    private val getStorageDownloadLocation: GetStorageDownloadLocation,
    private val getStorageDownloadDefaultPath: GetStorageDownloadDefaultPath,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadSettingsState())

    /**
     * UI State of Download Settings
     * @see DownloadSettingsState
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getStorageDownloadAskAlways().also { askAlways ->
                _uiState.update { it.copy(isAskAlwaysChecked = askAlways) }
            }
            setDownloadLocation(getStorageDownloadLocation())
        }
    }

    /**
     * Change user's download location to a specific folder
     */
    fun setDownloadLocation(location: String?) = viewModelScope.launch {
        try {
            val downloadLocation =
                if (location.isNullOrEmpty()) getStorageDownloadDefaultPath() else location
            setStorageDownloadLocation(downloadLocation)
            _uiState.update { it.copy(downloadLocationPath = downloadLocation) }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Invoke when user's ask for download location configuration changed
     */
    fun onStorageAskAlwaysChanged(isChecked: Boolean) = viewModelScope.launch {
        setStorageDownloadAskAlways(isChecked)
        _uiState.update { it.copy(isAskAlwaysChecked = isChecked) }
    }
}