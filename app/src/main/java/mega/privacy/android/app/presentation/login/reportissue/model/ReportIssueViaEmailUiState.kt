package mega.privacy.android.app.presentation.login.reportissue.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueContentUiState
import mega.privacy.android.domain.entity.support.SupportEmailTicket

/**
 * Report issue state
 *
 * @property description content of the description text field
 * @property includeLogs whether to include logs
 * @property canSubmit validation result
 * @property error error message
 * @property sendEmailEvent event to send email
 *
 * @constructor Create empty Report issue state
 */
data class ReportIssueViaEmailUiState(
    override val description: String = "",
    override val includeLogsVisible: Boolean = true,
    override val includeLogs: Boolean = false,
    override val canSubmit: Boolean = false,
    override val error: Int? = null,
    val sendEmailEvent: StateEventWithContent<SupportEmailTicket> = consumed(),
) : ReportIssueContentUiState
