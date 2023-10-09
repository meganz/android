package mega.privacy.android.data.mapper.transfer

import nz.mega.sdk.MegaError.ErrorContexts
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * Mapper for converting [MegaTransfer] type into [ErrorContexts]
 */
internal class ErrorContextMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param transferType [MegaTransfer] type.
     * @return [ErrorContexts]
     */
    operator fun invoke(transferType: Int): ErrorContexts =
        when (transferType) {
            MegaTransfer.TYPE_DOWNLOAD -> ErrorContexts.API_EC_DOWNLOAD
            MegaTransfer.TYPE_UPLOAD -> ErrorContexts.API_EC_UPLOAD
            else -> ErrorContexts.API_EC_DEFAULT
        }
}