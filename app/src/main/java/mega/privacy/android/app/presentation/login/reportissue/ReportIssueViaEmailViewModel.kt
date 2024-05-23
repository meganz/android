package mega.privacy.android.app.presentation.login.reportissue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.reportissue.model.ReportIssueViaEmailUiState
import mega.privacy.android.domain.usecase.support.CreateSupportTicketEmailUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Report issue view model
 */
@HiltViewModel
class ReportIssueViaEmailViewModel @Inject constructor(
    private val createSupportTicketEmailUseCase: CreateSupportTicketEmailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportIssueViaEmailUiState())

    /**
     * Public UI state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Set description
     *
     * @param newDescription
     */
    fun setDescription(newDescription: String) {
        _uiState.update {
            it.copy(
                description = newDescription,
                canSubmit = newDescription.isNotBlank(),
                error = null
            )
        }
    }

    /**
     * Create email template
     *
     */
    fun submit() {
        viewModelScope.launch {
            if (!uiState.value.canSubmit) {
                _uiState.update {
                    it.copy(
                        error = R.string.settings_help_report_issue_description_label
                    )
                }
                return@launch
            }
            runCatching {
                createSupportTicketEmailUseCase(
                    emailBody = uiState.value.description,
                    includeLogs = uiState.value.includeLogs
                )
            }.onSuccess { emailTicket ->
                _uiState.update {
                    it.copy(
                        sendEmailEvent = triggered(emailTicket)
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Send email event consumed
     */
    fun onSendEmailEventConsumed() {
        _uiState.update {
            it.copy(
                sendEmailEvent = consumed()
            )
        }
    }

    /**
     * Set include logs enabled
     *
     * @param enabled
     */
    fun setIncludeLogs(enabled: Boolean) {
        _uiState.update {
            it.copy(
                includeLogs = enabled
            )
        }
    }

}
