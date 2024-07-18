package mega.privacy.android.feature.sync.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.ui.model.SyncOption
import javax.inject.Inject

@HiltViewModel
internal class SettingsSyncViewModel @Inject constructor(
    monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val setSyncByWiFiUseCase: SetSyncByWiFiUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsSyncUiState(syncOption = SyncOption.WI_FI_ONLY))
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
    }

    fun setSyncByWiFi(option: SyncOption) {
        viewModelScope.launch {
            setSyncByWiFiUseCase(option == SyncOption.WI_FI_ONLY)
        }
    }
}