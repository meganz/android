package mega.privacy.android.app.usecase.data

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.DBUtil

/**
 * Data class containing all the info related to a movement request.
 *
 * @property count      Number of requests.
 * @property errorCount Number of requests which finished with an error.
 */
data class RemoveRequestResult(
    val count: Int,
    val errorCount: Int,
) {

    val successCount = count - errorCount
    val isSingleAction = count == 1
    val isSuccess = errorCount == 0

    fun getResultText(context: Context): String =
        when {
            isSingleAction && isSuccess -> {
                context.getString(R.string.context_correctly_removed)
            }
            isSingleAction -> {
                context.getString(R.string.context_no_removed)
            }
            isSuccess -> {
                context.getString(R.string.number_correctly_removed, count)
            }
            else -> {
                context.getString(R.string.number_correctly_removed, successCount) +
                        context.getString(R.string.number_no_removed, errorCount)
            }
        }

    /**
     * Resets the account details timestamp if some request finished with success.
     */
    fun resetAccountDetailsIfNeeded() {
        if (successCount > 0) {
            DBUtil.resetAccountDetailsTimeStamp()
        }
    }
}
