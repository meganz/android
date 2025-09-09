package mega.privacy.android.core.nodecomponents.mapper.message

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.domain.entity.node.MoveRequestResult
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
                            R.plurals.general_move_node_snackbar_fail,
                            request.count,
                            request.count
                        )
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            R.plurals.general_move_node_snackbar_success,
                            request.count,
                            request.count
                        )
                    }

                    else -> {
                        val success = context.resources.getQuantityString(
                            R.plurals.general_move_node_snackbar_concat_success,
                            request.count - request.errorCount,
                            request.count - request.errorCount
                        )
                        val fail = context.resources.getQuantityString(
                            R.plurals.general_move_node_snackbar_concat_fail,
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
                        context.getString(R.string.context_correctly_moved_to_rubbish)
                    }

                    request.isSingleAction -> {
                        context.getString(R.string.context_no_moved)
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            R.plurals.number_correctly_moved_to_rubbish,
                            request.count,
                            request.count
                        )
                    }

                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            R.plurals.number_incorrectly_moved_to_rubbish,
                            request.errorCount,
                            request.errorCount,
                        )
                    }

                    request.errorCount == 1 -> {
                        context.resources.getQuantityString(
                            R.plurals.nodes_correctly_and_node_incorrectly_moved_to_rubbish,
                            request.successCount,
                            request.successCount,
                        )
                    }

                    request.successCount == 1 -> {
                        context.resources.getQuantityString(
                            R.plurals.node_correctly_and_nodes_incorrectly_moved_to_rubbish,
                            request.errorCount,
                            request.errorCount,
                        )
                    }

                    else -> {
                        val success = context.resources.getQuantityString(
                            R.plurals.number_correctly_moved_to_rubbish,
                            request.successCount,
                            request.successCount
                        )

                        val failed = context.resources.getQuantityString(
                            R.plurals.number_incorrectly_moved_to_rubbish,
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
                            R.plurals.rubbish_bin_remove_items_snackbar_success,
                            request.count,
                            request.count,
                        )
                    }

                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            R.plurals.rubbish_bin_remove_items_snackbar_fail,
                            request.errorCount,
                            request.errorCount
                        )
                    }

                    else -> "${
                        context.resources.getQuantityString(
                            R.plurals.rubbish_bin_remove_items_snackbar_success_concat,
                            request.count,
                            request.count
                        )
                    }${
                        context.resources.getQuantityString(
                            R.plurals.rubbish_bin_remove_items_snackbar_fail_concat,
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
                            R.plurals.general_copy_snackbar_fail,
                            request.count,
                            request.count
                        )
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            R.plurals.general_copy_snackbar_success,
                            request.count,
                            request.count
                        )
                    }

                    else -> {
                        "${
                            context.resources.getQuantityString(
                                R.plurals.general_copy_snackbar_concat_success,
                                request.count - request.errorCount,
                                request.count - request.errorCount
                            )
                        }${
                            context.resources.getQuantityString(
                                R.plurals.general_copy_snackbar_concat_fail,
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
                            R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_failed,
                            request.count,
                            request.count
                        )
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_success,
                            request.count,
                            request.count
                        )
                    }

                    else -> {
                        "${
                            context.resources.getQuantityString(
                                R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_success,
                                request.count - request.errorCount,
                                request.count - request.errorCount
                            )
                        }${
                            context.resources.getQuantityString(
                                R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_failed_concat,
                                request.errorCount,
                                request.errorCount
                            )
                        }"
                    }
                }
            }

            is MoveRequestResult.RemoveOffline -> {
                context.resources.getString(R.string.file_removed_offline)
            }

            is MoveRequestResult.Restore -> {
                when {
                    request.isAllRequestError -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.number_incorrectly_restored_from_rubbish,
                            request.count,
                            request.count
                        )
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            sharedResR.plurals.number_correctly_restored_from_rubbish,
                            request.count,
                            request.count
                        )
                    }

                    else -> {
                        "${
                            context.resources.getQuantityString(
                                sharedResR.plurals.number_correctly_restored_from_rubbish,
                                request.count - request.errorCount,
                                request.count - request.errorCount
                            )
                        }, ${
                            context.resources.getQuantityString(
                                sharedResR.plurals.number_incorrectly_restored_from_rubbish,
                                request.errorCount,
                                request.errorCount
                            )
                        }"
                    }
                }
            }
        }
}