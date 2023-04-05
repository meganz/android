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

    private val successCount = count - errorCount
    private val isSuccess = errorCount == 0
    private val isFailed = errorCount == count

    /**
     * Show strings based on count and errorCount when items have been deleted from RubbishBin
     * @param context [Context]
     */
    fun getResultText(context: Context): String = when {
        isSuccess -> {
            context.resources.getQuantityString(
                R.plurals.rubbish_bin_remove_items_snackbar_success,
                successCount,
                successCount
            )
        }
        isFailed -> {
            context.resources.getQuantityString(
                R.plurals.rubbish_bin_remove_items_snackbar_fail,
                errorCount,
                errorCount
            )
        }
        else -> {
            "${
                context.resources.getQuantityString(
                    R.plurals.rubbish_bin_remove_items_snackbar_success_concat,
                    successCount,
                    successCount
                )
            }${
                context.resources.getQuantityString(
                    R.plurals.rubbish_bin_remove_items_snackbar_fail_concat, errorCount, errorCount
                )
            }"
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
