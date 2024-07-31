package mega.privacy.android.app.presentation.settings.reportissue.model

/**
 * Report issue content state
 *
 * @property description content of the description text field
 * @property includeLogsVisible visibility of the include logs toggle
 * @property includeLogs whether to include logs
 * @property canSubmit validation result
 * @property error error message
 *
 */
interface ReportIssueContentUiState {
    val description: String
    val includeLogsVisible: Boolean
    val includeLogs: Boolean
    val canSubmit: Boolean
    val error: Int?
}
