package mega.privacy.android.app.presentation.copynode.mapper

import mega.privacy.android.app.presentation.copynode.CopyRequestResult

/**
 * Mapper to get Copy Request message after copying node
 * @see [CopyRequestResult]
 * @see [CopyRequestMessageMapperImpl] for this interface implementation
 */
fun interface CopyRequestMessageMapper {
    /**
     * Invoke and return copy request action message
     * @param request as [CopyRequestResult]
     */
    operator fun invoke(request: CopyRequestResult?): String
}