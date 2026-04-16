package mega.privacy.android.feature.transfers.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.transfers.GetMaxDownloadConnectionsUseCase
import mega.privacy.android.domain.usecase.transfers.GetMaxTransferConnectionsRangeUseCase
import mega.privacy.android.domain.usecase.transfers.GetMaxUploadConnectionsUseCase
import mega.privacy.android.domain.usecase.transfers.SetMaxDownloadConnectionsUseCase
import mega.privacy.android.domain.usecase.transfers.SetMaxUploadConnectionsUseCase
import mega.privacy.android.feature.transfers.presentation.settings.model.TransfersSettingsUiState
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the transfers settings screen.
 *
 * @property getMaxDownloadConnectionsUseCase [GetMaxDownloadConnectionsUseCase]
 * @property getMaxUploadConnectionsUseCase [GetMaxUploadConnectionsUseCase]
 * @property setMaxDownloadConnectionsUseCase [SetMaxDownloadConnectionsUseCase]
 * @property setMaxUploadConnectionsUseCase [SetMaxUploadConnectionsUseCase]
 * @property getMaxTransferConnectionsRangeUseCase [GetMaxTransferConnectionsRangeUseCase]
 * @property uiState [TransfersSettingsUiState]
 */
@HiltViewModel
class TransfersSettingsViewModel @Inject constructor(
    private val getMaxDownloadConnectionsUseCase: GetMaxDownloadConnectionsUseCase,
    private val getMaxUploadConnectionsUseCase: GetMaxUploadConnectionsUseCase,
    private val setMaxDownloadConnectionsUseCase: SetMaxDownloadConnectionsUseCase,
    private val setMaxUploadConnectionsUseCase: SetMaxUploadConnectionsUseCase,
    private val getMaxTransferConnectionsRangeUseCase: GetMaxTransferConnectionsRangeUseCase,
) : ViewModel() {

    private val maxDownloadConnectionsFlow = MutableStateFlow<Int?>(null)
    private val maxUploadConnectionsFlow = MutableStateFlow<Int?>(null)

    /**
     * UI state for the transfers settings screen.
     * Starts as [TransfersSettingsUiState.Loading] and transitions to
     * [TransfersSettingsUiState.Data] once the current max connections are fetched.
     */
    val uiState: StateFlow<TransfersSettingsUiState> by lazy {
        combine(
            flow { emit(getMaxDownloadConnectionsUseCase()) }.catch { Timber.e(it) },
            flow { emit(getMaxUploadConnectionsUseCase()) }.catch { Timber.e(it) },
            flow { emit(getMaxTransferConnectionsRangeUseCase()) }.catch { Timber.e(it) },
            maxDownloadConnectionsFlow,
            maxUploadConnectionsFlow,
        ) { initialDownload, initialUpload, range, newDownload, newUpload ->
            TransfersSettingsUiState.Data(
                maxDownloadConnections = newDownload ?: initialDownload,
                maxUploadConnections = newUpload ?: initialUpload,
                maxTransferConnectionsRange = range,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, TransfersSettingsUiState.Loading)
    }

    /**
     * Set the maximum number of download connections.
     *
     * @param connections the new value.
     */
    fun setMaxDownloadConnections(connections: Int) {
        viewModelScope.launch {
            runCatching { setMaxDownloadConnectionsUseCase(connections) }
                .onFailure { Timber.e(it) }
                .onSuccess { maxDownloadConnectionsFlow.update { connections } }
        }
    }

    /**
     * Set the maximum number of upload connections.
     *
     * @param connections the new value.
     */
    fun setMaxUploadConnections(connections: Int) {
        viewModelScope.launch {
            runCatching { setMaxUploadConnectionsUseCase(connections) }
                .onFailure { Timber.e(it) }
                .onSuccess { maxUploadConnectionsFlow.update { connections } }
        }
    }
}
