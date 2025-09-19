package mega.privacy.android.core.nodecomponents.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.node.MultipleNodesRestoreResult
import mega.privacy.android.domain.entity.node.RestoreNodeResult
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.shared.resources.R as sharedResR
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
            is MultipleNodesRestoreResult -> {
                when {
                    result.errorCount == 0 -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.node_restored_from_rubbish_bin_success_message,
                            result.successCount,
                            result.successCount
                        )
                    }

                    result.successCount == 0 -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.node_restored_from_rubbish_bin_error_message,
                            result.errorCount,
                            result.errorCount
                        )
                    }

                    result.errorCount == 1 -> {
                        "${
                            context.resources.getQuantityString(
                                sharedResR.plurals.node_restored_from_rubbish_bin_error_message,
                                result.errorCount,
                                result.errorCount
                            )
                        }. ${
                            context.resources.getQuantityString(
                                sharedResR.plurals.node_restored_from_rubbish_bin_success_message,
                                result.successCount,
                                result.successCount
                            )
                        }"
                    }

                    result.successCount == 1 -> {
                        "${
                            context.resources.getQuantityString(
                                sharedResR.plurals.node_restored_from_rubbish_bin_success_message,
                                result.successCount,
                                result.successCount
                            )
                        }. ${
                            context.resources.getQuantityString(
                                sharedResR.plurals.node_restored_from_rubbish_bin_error_message,
                                result.errorCount,
                                result.errorCount
                            )
                        }"
                    }

                    else -> {
                        "${
                            context.resources.getQuantityString(
                                sharedResR.plurals.node_restored_from_rubbish_bin_success_message,
                                result.successCount,
                                result.successCount
                            )
                        }. ${
                            context.resources.getQuantityString(
                                sharedResR.plurals.node_restored_from_rubbish_bin_error_message,
                                result.errorCount,
                                result.errorCount
                            )
                        }"
                    }
                }
            }

            is SingleNodeRestoreResult -> {
                if (result.successCount == 1) {
                    if (result.destinationFolderName != null) {
                        context.getString(
                            sharedResR.string.node_restored_from_rubbish_bin_to_location,
                            result.destinationFolderName
                        )
                    } else {
                        context.getString(sharedResR.string.node_moved_success_message)
                    }
                } else {
                    context.getString(sharedResR.string.node_restore_error_message)
                }
            }
        }
    }
}