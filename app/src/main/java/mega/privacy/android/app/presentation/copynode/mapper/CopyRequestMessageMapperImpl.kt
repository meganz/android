package mega.privacy.android.app.presentation.copynode.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
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
            request == null || request.hasNoData -> throw RuntimeException("No copied data found!")
            request.isAllRequestError -> {
                context.resources.getQuantityString(
                    R.plurals.general_copy_snackbar_fail,
                    request.count,
                    request.count
                )
            }
            request.isAllRequestSuccess -> {
                context.resources.getQuantityString(
                    R.plurals.general_copy_snackbar_success,
                    request.count,
                    request.count
                )
            }
            else -> {
                val success = context.resources.getQuantityString(
                    R.plurals.general_copy_snackbar_concat_success,
                    request.count - request.errorCount,
                    request.count - request.errorCount
                )
                val fail = context.resources.getQuantityString(
                    R.plurals.general_copy_snackbar_concat_fail,
                    request.errorCount,
                    request.errorCount
                )

                "$success$fail"
            }
        }
    }
}