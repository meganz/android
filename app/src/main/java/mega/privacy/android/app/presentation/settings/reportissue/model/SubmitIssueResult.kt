package mega.privacy.android.app.presentation.settings.reportissue.model

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault

/**
 * Submit issue result
 *
 */
sealed class SubmitIssueResult {
    /**
     * Get result string
     *
     * @param context
     * @return the message to display to the user
     */
    abstract fun getResultString(context: Context): String

    object Success : SubmitIssueResult() {
        override fun getResultString(context: Context) =
            context.getString(R.string.settings_help_report_issue_success)
    }

    /**
     * Failure
     *
     * @property supportEmail
     */
    class Failure(private val supportEmail: String) : SubmitIssueResult() {
        override fun getResultString(context: Context) = context.getFormattedStringOrDefault(
            R.string.settings_help_report_issue_error,
            supportEmail,
        )
    }
}
