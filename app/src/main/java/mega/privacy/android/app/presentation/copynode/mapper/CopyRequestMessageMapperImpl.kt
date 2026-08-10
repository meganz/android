package mega.privacy.android.app.presentation.copynode.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.shared.resources.R as SharedR
import javax.inject.Inject

/**
 * Mapper implementation for [CopyRequestMessageMapper]
 */
class CopyRequestMessageMapperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : CopyRequestMessageMapper {
    /**
     * Invoke and return copy request action message
     */
    override operator fun invoke(request: CopyRequestResult?): String {
        return when {
            request == null || request.hasNoData -> {
                context.resources.getQuantityString(
                    SharedR.plurals.copy_node_general_snackbar_fail,
                    0,
                    0
                )
            }
            request.isAllRequestError -> {
                context.resources.getQuantityString(
                    SharedR.plurals.copy_node_general_snackbar_fail,
                    request.count,
                    request.count
                )
            }
            request.isAllRequestSuccess -> {
                context.resources.getQuantityString(
                    SharedR.plurals.copy_node_general_snackbar_success,
                    request.count,
                    request.count
                )
            }
            else -> {
                val success = context.resources.getQuantityString(
                    SharedR.plurals.copy_node_snackbar_concat_success,
                    request.count - request.errorCount,
                    request.count - request.errorCount
                )
                val fail = context.resources.getQuantityString(
                    SharedR.plurals.copy_node_snackbar_concat_fail,
                    request.errorCount,
                    request.errorCount
                )

                "$success$fail"
            }
        }
    }
}