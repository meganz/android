package mega.privacy.android.app.presentation.requeststatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.requeststatus.model.RequestStatusProgressUiState
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.requeststatus.MonitorRequestStatusProgressEventUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for request status progress bar
 */
@HiltViewModel
class RequestStatusProgressViewModel @Inject constructor(
    private val monitorRequestStatusProgressEventUseCase: MonitorRequestStatusProgressEventUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestStatusProgressUiState())

    /**
     * UI state for request status progress bar
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.RequestStatusProgressDialog)
            }.onSuccess { enabled ->
                if (enabled) {
                    monitorRequestStatusProgressEventUseCase()
                        .catch { throwable ->
                            Timber.e(throwable)
                            // Dismiss dialog on error
                            _uiState.update {
                                it.copy(progress = -1L)
                            }
                        }.collect { event ->
                            _uiState.update {
                                it.copy(
                                    progress = event.number
                                )
                            }
                        }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}