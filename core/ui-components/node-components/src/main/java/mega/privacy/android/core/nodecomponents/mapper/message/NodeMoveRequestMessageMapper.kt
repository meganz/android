package mega.privacy.android.core.nodecomponents.mapper.message

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.shared.nodes.R as NodesR
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Mapper implementation for General Movement Message node action
 * @param context as Application Context provided by dependency graph
 */
class NodeMoveRequestMessageMapper @Inject constructor(
    @ApplicationContext val context: Context,
) {
    /**
     * Invoke the Mapper
     */
    operator fun invoke(request: MoveRequestResult): String =
        when (request) {
            is MoveRequestResult.GeneralMovement -> {
                when {
                    request.hasNoData -> throw RuntimeException("No moved data found!")
                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.move_node_general_snackbar_fail,
                            request.count,
                            request.count
                        )
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.move_node_general_snackbar_success,
                            request.count,
                            request.count
                        )
                    }

                    else -> {
                        val success = context.resources.getQuantityString(
                            sharedResR.plurals.move_node_snackbar_concat_success,
                            request.count - request.errorCount,
                            request.count - request.errorCount
                        )
                        val fail = context.resources.getQuantityString(
                            sharedResR.plurals.move_node_snackbar_concat_fail,
                            request.errorCount,
                            request.errorCount
                        )

                        "$success$fail"
                    }
                }
            }

            is MoveRequestResult.RubbishMovement -> {
                when {
                    request.isSingleAction && request.isSuccess -> {
                        context.getString(NodesR.string.context_correctly_moved_to_rubbish)
                    }

                    request.isSingleAction -> {
                        context.getString(NodesR.string.context_no_moved)
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.count_items_moved_to_rubbish,
                            request.count,
                            request.count
                        )
                    }

                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.count_items_failed_move_to_rubbish,
                            request.errorCount,
                            request.errorCount,
                        )
                    }

                    request.errorCount == 1 -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.items_moved_and_one_failed_to_rubbish,
                            request.successCount,
                            request.successCount,
                        )
                    }

                    request.successCount == 1 -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.one_moved_and_items_failed_to_rubbish,
                            request.errorCount,
                            request.errorCount,
                        )
                    }

                    else -> {
                        val success = context.resources.getQuantityString(
                            sharedResR.plurals.count_items_moved_to_rubbish,
                            request.successCount,
                            request.successCount
                        )

                        val failed = context.resources.getQuantityString(
                            sharedResR.plurals.count_items_failed_move_to_rubbish,
                            request.errorCount,
                            request.errorCount,
                        )
                        "$success. $failed"
                    }
                }
            }

            is MoveRequestResult.DeleteMovement -> {
                when {
                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.rubbish_remove_items_snackbar_success,
                            request.count,
                            request.count,
                        )
                    }

                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.rubbish_remove_items_snackbar_fail,
                            request.errorCount,
                            request.errorCount
                        )
                    }

                    else -> "${
                        context.resources.getQuantityString(
                            sharedResR.plurals.rubbish_remove_items_snackbar_concat_success,
                            request.count,
                            request.count
                        )
                    }${
                        context.resources.getQuantityString(
                            sharedResR.plurals.rubbish_remove_items_snackbar_concat_fail,
                            request.errorCount,
                            request.errorCount
                        )
                    }"
                }
            }

            is MoveRequestResult.Copy -> {
                when {
                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.copy_node_general_snackbar_fail,
                            request.count,
                            request.count
                        )
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.copy_node_general_snackbar_success,
                            request.count,
                            request.count
                        )
                    }

                    else -> {
                        "${
                            context.resources.getQuantityString(
                                sharedResR.plurals.copy_node_snackbar_concat_success,
                                request.count - request.errorCount,
                                request.count - request.errorCount
                            )
                        }${
                            context.resources.getQuantityString(
                                sharedResR.plurals.copy_node_snackbar_concat_fail,
                                request.errorCount,
                                request.errorCount
                            )
                        }"
                    }
                }
            }

            is MoveRequestResult.ShareMovement -> {
                when {
                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.cloud_drive_sharing_folder_snackbar_failed,
                            request.count,
                            request.count
                        )
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.cloud_drive_sharing_folder_snackbar_success,
                            request.count,
                            request.count
                        )
                    }

                    else -> {
                        "${
                            context.resources.getQuantityString(
                                sharedResR.plurals.cloud_drive_sharing_folder_snackbar_success,
                                request.count - request.errorCount,
                                request.count - request.errorCount
                            )
                        }${
                            context.resources.getQuantityString(
                                sharedResR.plurals.cloud_drive_sharing_folder_snackbar_concat_failed,
                                request.errorCount,
                                request.errorCount
                            )
                        }"
                    }
                }
            }

            is MoveRequestResult.RemoveOffline -> {
                context.resources.getString(sharedResR.string.remove_from_offline_success_message)
            }

            is MoveRequestResult.Restore -> {
                when {
                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.node_restored_from_rubbish_bin_error_message,
                            request.count,
                            request.count
                        )
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.node_restored_from_rubbish_bin_success_message,
                            request.count,
                            request.count
                        )
                    }

                    else -> {
                        "${
                            context.resources.getQuantityString(
                                sharedResR.plurals.node_restored_from_rubbish_bin_success_message,
                                request.count - request.errorCount,
                                request.count - request.errorCount
                            )
                        }, ${
                            context.resources.getQuantityString(
                                sharedResR.plurals.node_restored_from_rubbish_bin_error_message,
                                request.errorCount,
                                request.errorCount
                            )
                        }"
                    }
                }
            }
        }
}