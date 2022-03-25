package mega.privacy.android.app.usecase.data

import mega.privacy.android.app.R
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getString

/**
 * Data class containing all the info related to a copy request.
 *
 * @property count              Number of requests.
 * @property errorCount         Number of requests which finished with an error.
 * @property isForeignNode      True if should show a foreign storage over quota warning, false otherwise.
 */
data class CopyRequestResult(
    val count: Int,
    val errorCount: Int,
    val isForeignNode: Boolean
) {

    val successCount = count - errorCount
    val isSingleAction: Boolean = count == 1
    val isSuccess: Boolean = errorCount == 0

    fun getResultText(): String =
        when {
            count == 1 && isSuccess -> getString(R.string.context_correctly_copied)
            count == 1 -> getString(R.string.context_no_copied)
            isSuccess -> getString(R.string.number_correctly_copied, count)
            else -> getString(
                R.string.number_correctly_copied,
                successCount
            ) + getString(R.string.number_no_copied, errorCount)
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
