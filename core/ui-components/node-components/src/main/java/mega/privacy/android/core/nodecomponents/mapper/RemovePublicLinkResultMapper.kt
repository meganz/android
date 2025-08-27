package mega.privacy.android.core.nodecomponents.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.node.ResultCount
import mega.privacy.android.shared.resources.R as sharedResR
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
        if (result.errorCount == 0) {
            context.resources.getQuantityString(
                sharedResR.plurals.context_link_removal_success,
                result.successCount,
                result.successCount
            )
        } else {
            context.resources.getQuantityString(
                sharedResR.plurals.context_link_removal_error,
                result.errorCount,
                result.errorCount
            )
        }
}