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
            getSuccessMessage(result.successCount)
        } else {
            context.getString(sharedResR.string.public_link_node_removal_error_message)
        }

    private fun getSuccessMessage(count: Int): String =
        if (count == 1) {
            context.getString(sharedResR.string.link_removed_success_message)
        } else {
            context.getString(sharedResR.string.links_removed_success_message)
        }
}