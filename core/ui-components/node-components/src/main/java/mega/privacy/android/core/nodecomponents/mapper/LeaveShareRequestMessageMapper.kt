package mega.privacy.android.core.nodecomponents.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.node.MoveRequestResult
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Mapper implementation for Delete Movement Message while leave share
 * @param context as Application Context provided by dependency graph
 */
class LeaveShareRequestMessageMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Invoke the Mapper
     * @param request [MoveRequestResult.DeleteMovement]
     */
    operator fun invoke(request: MoveRequestResult.DeleteMovement) =
        when {
            request.isSuccess -> {
                context.resources.getQuantityString(
                    sharedResR.plurals.shared_items_incoming_shares_snackbar_leaving_shares_success,
                    request.count,
                    request.count,
                )
            }

            request.isAllRequestError -> {
                context.resources.getQuantityString(
                    sharedResR.plurals.shared_items_incoming_shares_snackbar_leaving_shares_fail,
                    request.errorCount,
                    request.errorCount
                )
            }

            else -> "${
                context.resources.getQuantityString(
                    sharedResR.plurals.shared_items_incoming_shares_snackbar_leaving_shares_success_concat,
                    request.count,
                    request.count
                )
            }${
                context.resources.getQuantityString(
                    sharedResR.plurals.shared_items_incoming_shares_snackbar_leaving_shares_fail_concat,
                    request.errorCount,
                    request.errorCount
                )
            }"
        }
}