package mega.privacy.android.app.presentation.settings.reportissue.model

/**
 * Report issue state
 *
 * @property description content of the description text field
 * @property includeLogsVisible visibility of the include logs toggle
 * @property includeLogs checked state of the include logs toggle
 * @property canSubmit validation result
 * @property error string resource id for on screen error
 * @property result string resource id for result text
 * @property uploadProgress progress of the log upload
 * @constructor Create empty Report issue state
 */
data class ReportIssueState(
    val description: String = "",
    val includeLogsVisible: Boolean = false,
    val includeLogs: Boolean = false,
    val canSubmit: Boolean = false,
    val error: Int? = null,
    val result: SubmitIssueResult? = null,
    val uploadProgress: Float? = null,
)