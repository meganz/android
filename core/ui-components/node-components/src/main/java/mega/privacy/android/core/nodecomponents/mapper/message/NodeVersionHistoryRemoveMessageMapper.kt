package mega.privacy.android.core.nodecomponents.mapper.message

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import javax.inject.Inject

/**
 * Mapper implementation for Version History Remove Message
 *
 * @param context as Application Context provided by dependency graph
 */
class NodeVersionHistoryRemoveMessageMapper @Inject constructor(
    @ApplicationContext val context: Context,
) {
    /**
     * Invoke the Mapper
     *
     * @param result as Throwable
     */
    operator fun invoke(result: Throwable?): String = when (result) {
        is VersionsNotDeletedException -> {
            val versionsDeleted =
                result.totalRequestedToDelete - result.totalNotDeleted
            val firstLine = context.getString(
                NodesR.string.version_history_deleted_erroneously
            )
            val secondLine = context.resources.getQuantityString(
                NodesR.plurals.versions_deleted_succesfully,
                versionsDeleted,
                versionsDeleted
            )
            val thirdLine = context.resources.getQuantityString(
                NodesR.plurals.versions_not_deleted,
                result.totalNotDeleted,
                result.totalNotDeleted
            )
            "$firstLine\n$secondLine\n$thirdLine"
        }

        is Throwable -> context.getString(sharedR.string.general_text_error)

        else -> context.getString(NodesR.string.version_history_deleted)
    }
}