package mega.privacy.android.feature.sync.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ClearSyncDebrisUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncDebrisSizeInBytesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByChargingUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByChargingUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.GetSyncFrequencyUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.SetSyncFrequencyUseCase
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption
import mega.privacy.android.shared.resources.R
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SettingsSyncViewModel @Inject constructor(
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase,
    private val setSyncByWiFiUseCase: SetSyncByWiFiUseCase,
    private val setSyncByChargingUseCase: SetSyncByChargingUseCase,
    private val getSyncDebrisSizeUseCase: GetSyncDebrisSizeInBytesUseCase,
    private val clearSyncDebrisUseCase: ClearSyncDebrisUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getSyncFrequencyUseCase: GetSyncFrequencyUseCase,
    private val setSyncFrequencyUseCase: SetSyncFrequencyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsSyncUiState())
    val uiState = _uiState.asStateFlow()

    init {
        monitorSyncSettings()
        fetchSyncDebris()
        loadSyncFrequency()
    }

    private fun monitorSyncSettings() {
        viewModelScope.launch {
            combine(
                monitorSyncByWiFiUseCase().catch { Timber.e("Error Monitoring Wifi Setting $it") },
                monitorSyncByChargingUseCase().catch { Timber.e("Error Monitoring Charging Setting $it") },
            ) { wifiSettings, batterySettings ->
                wifiSettings to batterySettings
            }.collect { (wiFiOnly, syncOnlyWhenCharging) ->

                val syncConnectionType = when {
                    wiFiOnly -> SyncConnectionType.WiFiOnly
                    else -> SyncConnectionType.WiFiOrMobileData
                }

                val syncPowerOption = when {
                    syncOnlyWhenCharging -> SyncPowerOption.SyncOnlyWhenCharging
                    else -> SyncPowerOption.SyncAlways
                }
                _uiState.update {
                    it.copy(
                        syncConnectionType = syncConnectionType,
                        syncPowerOption = syncPowerOption,
                    )
                }
            }
        }
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
            is SettingsSyncAction.SyncConnectionTypeSelected -> {
                setSyncNetworkOption(action.option)
            }

            is SettingsSyncAction.SyncPowerOptionSelected -> {
                setSyncPowerOption(action.option)
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

    private fun setSyncNetworkOption(option: SyncConnectionType) {
        viewModelScope.launch {
            runCatching {
                when (option) {
                    SyncConnectionType.WiFiOnly -> {
                        setSyncByWiFiUseCase(true)
                    }

                    SyncConnectionType.WiFiOrMobileData -> {
                        setSyncByWiFiUseCase(false)
                    }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(snackbarMessage = R.string.settings_sync_option_updated_message)
                }
            }.onFailure(Timber::e)
        }
    }

    private fun setSyncPowerOption(option: SyncPowerOption) {
        viewModelScope.launch {
            runCatching {
                when (option) {
                    SyncPowerOption.SyncAlways -> {
                        setSyncByChargingUseCase(false)
                    }

                    SyncPowerOption.SyncOnlyWhenCharging -> {
                        setSyncByChargingUseCase(true)
                    }
                }
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
