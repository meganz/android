package mega.privacy.android.app.presentation.settings.reportissue

import mega.privacy.android.shared.resources.R as sharedR
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getStateFlow
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueUiState
import mega.privacy.android.app.presentation.settings.reportissue.model.SubmitIssueResult
import mega.privacy.android.domain.entity.SubmitIssueRequest
import mega.privacy.android.domain.usecase.GetSupportEmailUseCase
import mega.privacy.android.domain.usecase.SubmitIssueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Report issue view model
 *
 * @property submitIssueUseCase
 *
 * @param monitorConnectivityUseCase
 * @param savedStateHandle
 *
 * @property uiState current view state
 */
@HiltViewModel
class ReportIssueViewModel @Inject constructor(
    private val submitIssueUseCase: SubmitIssueUseCase,
    private val getSupportEmailUseCase: GetSupportEmailUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val description = savedStateHandle.getStateFlow(
        viewModelScope,
        DESCRIPTION_KEY,
        ""
    )

    private val includeLogs = savedStateHandle.getStateFlow(
        viewModelScope,
        INCLUDE_LOGS_KEY,
        false
    )

    private val isConnected =
        monitorConnectivityUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var submitReportJob: Job? = null

    private val _uiState = MutableStateFlow(ReportIssueUiState())

    val uiState: StateFlow<ReportIssueUiState> = _uiState

    init {
        viewModelScope.launch {
            merge(
                description.map {
                    { state: ReportIssueUiState ->
                        state.copy(
                            description = it,
                            canSubmit = it.isNotBlank()
                        )
                    }
                },
                includeLogs.map {
                    { state: ReportIssueUiState -> state.copy(includeLogs = it) }
                },
            ).collect {
                _uiState.update(it)
            }
        }
    }

    /**
     * Set description
     *
     * @param newDescription
     */
    fun setDescription(newDescription: String) {
        description.update { newDescription }
        _uiState.update {
            it.copy(
                error = null
            )
        }
    }

    /**
     * Set include logs enabled
     *
     * @param enabled
     */
    fun setIncludeLogsEnabled(enabled: Boolean) {
        includeLogs.update { enabled }
    }

    /**
     * Submit report
     *
     */
    fun submit() {
        if (isConnected.value) {
            if (uiState.value.description.length < MINIMUM_CHARACTERS) {
                _uiState.update {
                    it.copy(
                        error = sharedR.string.report_issue_error_minimum_characters
                    )
                }
                return
            }
            if (submitReportJob?.isActive != true) {
                submitReportJob = viewModelScope.launch {
                    try {
                        submitIssueUseCase(SubmitIssueRequest(description.value, includeLogs.value))
                            .cancellable()
                            .onCompletion { error ->
                                onSubmitCompleted(error)
                            }
                            .catch { Timber.e(it) }
                            .collect { progress ->
                                _uiState.update { it.copy(uploadProgress = progress.floatValue) }
                            }
                    } catch (exception: Throwable) {
                        onSubmitCompleted(exception)
                    }
                }
            }
        } else {
            _uiState.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    private suspend fun onSubmitCompleted(error: Throwable?) {
        when (error) {
            is CancellationException -> {
                _uiState.update { it.copy(uploadProgress = null) }
            }

            null -> {
                _uiState.update {
                    it.copy(
                        result = SubmitIssueResult.Success,
                        uploadProgress = null,
                        canSubmit = false
                    )
                }
            }

            else -> {
                _uiState.update {
                    it.copy(
                        result = SubmitIssueResult.Failure(getSupportEmailUseCase()),
                        uploadProgress = null
                    )
                }
            }
        }
    }

    /**
     * Cancel upload
     *
     */
    fun cancelUpload() {
        submitReportJob?.cancel()
    }

    companion object {
        internal const val DESCRIPTION_KEY = "DESCRIPTION"
        internal const val INCLUDE_LOGS_KEY = "INCLUDE_LOGS"
        private const val MINIMUM_CHARACTERS = 10
    }
}
