package mega.privacy.android.app.usecase.data

import mega.privacy.android.app.R
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getString

/**
 * Data class containing all the info related to a movement request.
 *
 * @property count      Number of requests.
 * @property errorCount Number of requests which finished with an error.
 */
data class RemoveRequestResult(
    val count: Int,
    val errorCount: Int
) {

    val successCount = count - errorCount
    val isSingleAction = count == 1
    val isSuccess = errorCount == 0

    fun getResultText(): String =
        when {
            isSingleAction && isSuccess -> {
                getString(R.string.context_correctly_removed)
            }
            isSingleAction -> {
                DBUtil.resetAccountDetailsTimeStamp()
                getString(R.string.context_no_removed)
            }
            isSuccess -> {
                DBUtil.resetAccountDetailsTimeStamp()
                getString(R.string.number_correctly_removed, count)
            }
            else -> {
                getString(R.string.number_correctly_removed, successCount) +
                        getString(R.string.number_no_removed, errorCount)
            }
        }
}
