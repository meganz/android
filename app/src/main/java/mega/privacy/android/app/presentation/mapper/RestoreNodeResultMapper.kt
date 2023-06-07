package mega.privacy.android.app.presentation.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.MultipleNodesRestoreResult
import mega.privacy.android.domain.entity.node.RestoreNodeResult
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import javax.inject.Inject

/**
 * Restore node result mapper
 *
 * @property context
 */
class RestoreNodeResultMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Invoke
     *
     * @param result
     */
    operator fun invoke(result: RestoreNodeResult): String {
        return when (result) {
            is MultipleNodesRestoreResult -> if (result.errorCount == 0) {
                context.resources.getQuantityString(
                    R.plurals.number_correctly_restored_from_rubbish,
                    result.successCount,
                    result.successCount
                )
            } else if (result.successCount == 0) {
                context.resources.getQuantityString(
                    R.plurals.number_incorrectly_restored_from_rubbish,
                    result.errorCount,
                    result.errorCount
                )
            } else if (result.errorCount == 1) {
                context.resources.getQuantityString(
                    R.plurals.nodes_correctly_and_node_incorrectly_restored_from_rubbish,
                    result.successCount,
                    result.successCount
                )
            } else if (result.successCount == 1) {
                context.resources.getQuantityString(
                    R.plurals.node_correctly_and_nodes_incorrectly_restored_from_rubbish,
                    result.successCount,
                    result.successCount
                )
            } else {
                "${
                    context.resources.getQuantityString(
                        R.plurals.number_correctly_restored_from_rubbish,
                        result.successCount,
                        result.successCount
                    )
                }. ${
                    context.resources.getQuantityString(
                        R.plurals.number_incorrectly_restored_from_rubbish,
                        result.errorCount,
                        result.errorCount
                    )
                }"
            }

            is SingleNodeRestoreResult -> if (result.successCount == 1) {
                if (result.destinationFolderName != null) {
                    context.getString(
                        R.string.context_correctly_node_restored,
                        result.destinationFolderName
                    )
                } else {
                    context.getString(R.string.context_correctly_moved)
                }
            } else {
                context.getString(R.string.context_no_restored)
            }
        }
    }
}