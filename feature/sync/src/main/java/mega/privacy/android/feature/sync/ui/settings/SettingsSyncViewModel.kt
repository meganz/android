package mega.privacy.android.feature.sync.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ClearSyncDebrisUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncDebrisSizeInBytesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.GetSyncFrequencyUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.SetSyncFrequencyUseCase
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.model.SyncOption
import mega.privacy.android.shared.resources.R
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SettingsSyncViewModel @Inject constructor(
    monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val setSyncByWiFiUseCase: SetSyncByWiFiUseCase,
    private val getSyncDebrisSizeUseCase: GetSyncDebrisSizeInBytesUseCase,
    private val clearSyncDebrisUseCase: ClearSyncDebrisUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getSyncFrequencyUseCase: GetSyncFrequencyUseCase,
    private val setSyncFrequencyUseCase: SetSyncFrequencyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsSyncUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            monitorSyncByWiFiUseCase().collect { syncByWifi ->
                _uiState.update {
                    it.copy(
                        syncOption = if (syncByWifi) {
                            SyncOption.WI_FI_ONLY
                        } else {
                            SyncOption.WI_FI_OR_MOBILE_DATA
                        }
                    )
                }
            }
        }
        fetchSyncDebris()
        loadSyncFrequency()
    }

    private fun loadSyncFrequency() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(SyncFeatures.SyncFrequencySettings)
            }.onSuccess { isSyncFrequencyEnabled ->
                if (isSyncFrequencyEnabled) {
                    runCatching {
                        getSyncFrequencyUseCase()
                    }.onSuccess { frequencyInMinutes ->
                        _uiState.update {
                            it.copy(syncFrequency = SyncFrequency.fromMinutes(frequencyInMinutes))
                        }
                    }
                }
                _uiState.update {
                    it.copy(showSyncFrequency = isSyncFrequencyEnabled)
                }
            }.onFailure(Timber::e)
        }
    }

    fun handleAction(action: SettingsSyncAction) {
        when (action) {
            is SettingsSyncAction.SyncOptionSelected -> {
                setSyncByWiFi(action.option)
            }

            is SettingsSyncAction.ClearDebrisClicked -> {
                clearSyncDebris()
            }

            SettingsSyncAction.SnackbarShown -> {
                _uiState.update {
                    it.copy(snackbarMessage = null)
                }
            }

            is SettingsSyncAction.SyncFrequencySelected -> {
                viewModelScope.launch {
                    runCatching {
                        setSyncFrequencyUseCase(action.frequency.minutes)
                    }.onFailure {
                        Timber.e(it)
                    }
                }
                _uiState.update {
                    it.copy(
                        syncFrequency = action.frequency,
                        snackbarMessage = R.string.settings_sync_option_updated_message
                    )
                }
            }
        }
    }

    private fun setSyncByWiFi(option: SyncOption) {
        viewModelScope.launch {
            runCatching {
                setSyncByWiFiUseCase(option == SyncOption.WI_FI_ONLY)
            }.onSuccess {
                _uiState.update {
                    it.copy(snackbarMessage = R.string.settings_sync_option_updated_message)
                }
            }.onFailure(Timber::e)
        }
    }

    private fun clearSyncDebris() {
        viewModelScope.launch {
            runCatching {
                clearSyncDebrisUseCase()
            }.onSuccess {
                _uiState.update {
                    it.copy(snackbarMessage = R.string.settings_sync_debris_cleared_message)
                }
                fetchSyncDebris()
            }.onFailure(Timber::e)
        }
    }

    private fun fetchSyncDebris() {
        viewModelScope.launch {
            runCatching {
                getSyncDebrisSizeUseCase()
            }.onSuccess { debrisSize ->
                _uiState.update {
                    it.copy(syncDebrisSizeInBytes = debrisSize)
                }
            }.onFailure(Timber::e)
        }
    }
}