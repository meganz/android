package mega.privacy.android.app.presentation.settings.reportissue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.entity.SubmitIssueRequest
import mega.privacy.android.app.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.app.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.app.domain.usecase.GetSupportEmail
import mega.privacy.android.app.domain.usecase.MonitorConnectivity
import mega.privacy.android.app.domain.usecase.SubmitIssue
import mega.privacy.android.app.presentation.extensions.getStateFlow
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueState
import mega.privacy.android.app.presentation.settings.reportissue.model.SubmitIssueResult
import javax.inject.Inject

/**
 * Report issue view model
 *
 * @property areSdkLogsEnabled
 * @property areChatLogsEnabled
 * @property submitIssue
 * @property ioDispatcher
 *
 * @param monitorConnectivity
 * @param savedStateHandle
 *
 * @property state current view state
 */
@HiltViewModel
class ReportIssueViewModel @Inject constructor(
    private val areSdkLogsEnabled: AreSdkLogsEnabled,
    private val areChatLogsEnabled: AreChatLogsEnabled,
    private val submitIssue: SubmitIssue,
    private val getSupportEmail: GetSupportEmail,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorConnectivity: MonitorConnectivity,
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
        monitorConnectivity().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var submitReportJob: Job? = null

    private val _state = MutableStateFlow(ReportIssueState())

    val state: StateFlow<ReportIssueState> = _state

    init {
        viewModelScope.launch(ioDispatcher) {
            merge(
                description.map {
                    { state: ReportIssueState ->
                        state.copy(
                            description = it,
                            canSubmit = it.isNotEmpty()
                        )
                    }
                },
                includeLogsVisible.map {
                    { state: ReportIssueState -> state.copy(includeLogsVisible = it) }
                },
                includeLogs.map {
                    { state: ReportIssueState -> state.copy(includeLogs = it) }
                },
            ).collect {
                _state.update(it)
            }
        }

        viewModelScope.launch(ioDispatcher) {
            combine(
                areSdkLogsEnabled(),
                areChatLogsEnabled()
            ) { sdk, chat -> sdk || chat }
                .collectLatest {
                    includeLogsVisible.update { _ -> it }
                    includeLogs.update { _ -> it }
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
            submitReportJob = viewModelScope.launch(ioDispatcher) {
                submitIssue(SubmitIssueRequest(description.value, includeLogs.value))
                    .cancellable()
                    .onCompletion { error ->
                        onSubmitCompleted(error)
                    }.collect { progress ->
                        _state.update { it.copy(uploadProgress = progress.floatValue) }
                    }
            }
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    private suspend fun onSubmitCompleted(error: Throwable?) {
        when (error) {
            is CancellationException -> {
                _state.update { it.copy(uploadProgress = null) }
            }
            null -> {
                _state.update {
                    it.copy(
                        result = SubmitIssueResult.Success,
                        uploadProgress = null
                    )
                }
            }
            else -> {
                _state.update {
                    it.copy(
                        result = SubmitIssueResult.Failure(getSupportEmail()),
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

    /**
     * Intercept navigation
     *
     * @return true if handled by the view model, else false to indicate
     * that the caller may proceed with the navigation
     */
    fun interceptNavigation() = description.value.isNotEmpty()
        .also { intercept ->
            _state.update { it.copy(navigationRequested = intercept) }
        }

    /**
     * Navigation cancelled
     *
     */
    fun navigationCancelled() = _state.update { it.copy(navigationRequested = false) }

}
