package mega.privacy.android.app.presentation.movenode.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.movenode.MoveRequestResult
import javax.inject.Inject

/**
 * Mapper implementation for General Movement Message node action
 * @param context as Application Context provided by dependency graph
 */
class MoveRequestMessageMapper @Inject constructor(
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
            is MoveRequestResult.Restoration -> TODO()
            is MoveRequestResult.RubbishMovement -> TODO()
        }
}