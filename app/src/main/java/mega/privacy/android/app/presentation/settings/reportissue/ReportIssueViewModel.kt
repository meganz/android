package mega.privacy.android.app.presentation.settings.reportissue

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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.getStateFlow
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueUiState
import mega.privacy.android.app.presentation.settings.reportissue.model.SubmitIssueResult
import mega.privacy.android.domain.entity.SubmitIssueRequest
import mega.privacy.android.domain.usecase.GetSupportEmailUseCase
import mega.privacy.android.domain.usecase.SubmitIssueUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.logging.AreChatLogsEnabledUseCase
import mega.privacy.android.domain.usecase.logging.AreSdkLogsEnabledUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Report issue view model
 *
 * @property areSdkLogsEnabledUseCase
 * @property areChatLogsEnabledUseCase
 * @property submitIssueUseCase
 *
 * @param monitorConnectivityUseCase
 * @param savedStateHandle
 *
 * @property uiState current view state
 */
@HiltViewModel
class ReportIssueViewModel @Inject constructor(
    private val areSdkLogsEnabledUseCase: AreSdkLogsEnabledUseCase,
    private val areChatLogsEnabledUseCase: AreChatLogsEnabledUseCase,
    private val submitIssueUseCase: SubmitIssueUseCase,
    private val getSupportEmailUseCase: GetSupportEmailUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    internal val descriptionKey = "DESCRIPTION"
    internal val includeLogsVisibleKey = "INCLUDE_LOGS_VISIBLE"
    internal val includeLogsKey = "INCLUDE_LOGS"

    private val description = savedStateHandle.getStateFlow(
        viewModelScope,
        descriptionKey,
        ""
    )

    private val includeLogsVisible = savedStateHandle.getStateFlow(
        viewModelScope,
        includeLogsVisibleKey,
        false
    )

    private val includeLogs = savedStateHandle.getStateFlow(
        viewModelScope,
        includeLogsKey,
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
                            canSubmit = it.isNotEmpty()
                        )
                    }
                },
                includeLogsVisible.map {
                    { state: ReportIssueUiState -> state.copy(includeLogsVisible = it) }
                },
                includeLogs.map {
                    { state: ReportIssueUiState -> state.copy(includeLogs = it) }
                },
            ).collect {
                _uiState.update(it)
            }
        }

        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.PermanentLogging)) {
                includeLogsVisible.update { _ -> true }
                includeLogs.update { _ -> true }
            } else {
                combine(
                    areSdkLogsEnabledUseCase(),
                    areChatLogsEnabledUseCase()
                ) { sdk, chat -> sdk || chat }
                    .collectLatest {
                        includeLogsVisible.update { _ -> it }
                        includeLogs.update { _ -> it }
                    }
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

}
