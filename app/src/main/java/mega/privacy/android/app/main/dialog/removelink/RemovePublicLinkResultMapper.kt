package mega.privacy.android.app.main.dialog.removelink

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.ResultCount
import javax.inject.Inject

/**
 * Remove public link result mapper
 *
 */
class RemovePublicLinkResultMapper @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * Invoke
     *
     * @param result
     * @return the string to show in the snackbar
     */
    operator fun invoke(result: ResultCount): String =
        if (result.errorCount == 0) context.resources.getQuantityString(
            R.plurals.context_link_removal_success,
            result.successCount,
            result.successCount
        )
        else context.resources.getQuantityString(
            R.plurals.context_link_removal_error,
            result.errorCount,
            result.errorCount
        )
}