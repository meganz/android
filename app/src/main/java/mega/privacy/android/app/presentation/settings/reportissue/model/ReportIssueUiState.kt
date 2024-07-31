package mega.privacy.android.app.presentation.settings.reportissue.model

/**
 * Report issue state
 *
 * @property result string resource id for result text
 * @property uploadProgress progress of the log upload
 * @constructor Create empty Report issue state
 */
data class ReportIssueUiState(
    override val description: String = "",
    override val includeLogsVisible: Boolean = true,
    override val includeLogs: Boolean = false,
    override val canSubmit: Boolean = false,
    override val error: Int? = null,
    val result: SubmitIssueResult? = null,
    val uploadProgress: Float? = null,
) : ReportIssueContentUiState
