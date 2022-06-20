package mega.privacy.android.app.domain.entity

/**
 * Submit issue request
 *
 * @property description
 * @property includeLogs
 */
data class SubmitIssueRequest(val description: String, val includeLogs: Boolean)
