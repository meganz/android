package mega.privacy.android.app.usecase.data

import android.content.Context
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.DBUtil

/**
 * Data class containing all the info related to a copy request.
 *
 * @property context Application context provided by the dependency graph
 * @property data which holds the amount of copy request and failed copy request
 */

data class CopyRequestResult @AssistedInject constructor(
    @ApplicationContext val context: Context,
    @Assisted val data: CopyRequestData,
) {
    /**
     * Assisted Injection Factory to create [CopyRequestResult]
     */
    @AssistedFactory
    interface CopyRequestResultFactory {
        /**
         * Creates [CopyRequestResult] by passing [CopyRequestData]
         */
        fun create(data: CopyRequestData): CopyRequestResult
    }

    /**
     * The number of successful copy
     */
    val successCount = data.count - data.errorCount

    /**
     * Checks whether copy request has data
     */
    private val hasNoData: Boolean = data.count == 0

    /**
     * Checks whether all copy request are errors
     */
    private val isAllRequestError: Boolean = data.errorCount == data.count

    /**
     * Checks whether all copy request are successful
     */
    private val isAllRequestSuccess: Boolean = data.errorCount == 0

    /**
     * Success message when there's no error
     */
    private val successMessage = context.resources.getQuantityString(
        R.plurals.general_copy_snackbar_success,
        data.count,
        data.count
    )

    /**
     * Failed message when everything's an error
     */
    private val failedMessage = context.resources.getQuantityString(
        R.plurals.general_copy_snackbar_fail,
        data.count,
        data.count
    )

    /**
     * Message when there is at least 1 success and 1 error
     */
    private val combinedMessage: String
        get() {
            val successPart = context.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                successCount,
                successCount
            )
            val failPart = context.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                data.errorCount,
                data.errorCount
            )
            return "$successPart$failPart"
        }

    /**
     * Returns copy request string based on data and error condition
     */
    fun getResultText(): String =
        when {
            hasNoData -> throw RuntimeException("No copied data found!")
            isAllRequestError -> failedMessage
            isAllRequestSuccess -> successMessage
            else -> combinedMessage
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
