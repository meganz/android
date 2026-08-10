package mega.privacy.android.app.usecase.data

import android.content.Context
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.shared.resources.R as SharedR

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
                SharedR.plurals.rubbish_remove_items_snackbar_success,
                successCount,
                successCount
            )
        }
        isFailed -> {
            context.resources.getQuantityString(
                SharedR.plurals.rubbish_remove_items_snackbar_fail,
                errorCount,
                errorCount
            )
        }
        else -> {
            "${
                context.resources.getQuantityString(
                    SharedR.plurals.rubbish_remove_items_snackbar_concat_success,
                    successCount,
                    successCount
                )
            }${
                context.resources.getQuantityString(
                    SharedR.plurals.rubbish_remove_items_snackbar_concat_fail, errorCount, errorCount
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
