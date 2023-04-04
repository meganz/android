package mega.privacy.android.app.fragments.settingsFragments.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.settingsFragments.download.model.DownloadSettingsState
import mega.privacy.android.domain.usecase.GetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPathUseCase
import mega.privacy.android.domain.usecase.GetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadLocationUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [DownloadSettingsFragment]
 */
@HiltViewModel
class DownloadSettingsViewModel @Inject constructor(
    private val setStorageDownloadAskAlwaysUseCase: SetStorageDownloadAskAlwaysUseCase,
    private val setStorageDownloadLocationUseCase: SetStorageDownloadLocationUseCase,
    private val getStorageDownloadAskAlwaysUseCase: GetStorageDownloadAskAlwaysUseCase,
    private val getStorageDownloadLocationUseCase: GetStorageDownloadLocationUseCase,
    private val getStorageDownloadDefaultPathUseCase: GetStorageDownloadDefaultPathUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadSettingsState())

    /**
     * UI State of Download Settings
     * @see DownloadSettingsState
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getStorageDownloadAskAlwaysUseCase().also { askAlways ->
                _uiState.update { it.copy(isAskAlwaysChecked = askAlways) }
            }
            setDownloadLocation(getStorageDownloadLocationUseCase())
        }
    }

    /**
     * Change user's download location to a specific folder
     */
    fun setDownloadLocation(location: String?) = viewModelScope.launch {
        try {
            val downloadLocation =
                if (location.isNullOrEmpty()) getStorageDownloadDefaultPathUseCase() else location
            setStorageDownloadLocationUseCase(downloadLocation)
            _uiState.update { it.copy(downloadLocationPath = downloadLocation) }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Invoke when user's ask for download location configuration changed
     */
    fun onStorageAskAlwaysChanged(isChecked: Boolean) = viewModelScope.launch {
        setStorageDownloadAskAlwaysUseCase(isChecked)
        _uiState.update { it.copy(isAskAlwaysChecked = isChecked) }
    }
}