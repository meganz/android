package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Mapper to get fetch nodes progress and temporary errors and convert them into [FetchNodesUpdate].
 */
internal class FetchNodesUpdateMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param request [MegaRequest]. If null means the request finished.
     */
    operator fun invoke(request: MegaRequest?) =
        FetchNodesUpdate(getProgress(request))

    private fun getProgress(request: MegaRequest?) = Progress(request?.run {
        if (totalBytes > 0) {
            val progress = transferredBytes.toFloat() / totalBytes.toFloat()
            if (progress > 0.99 || progress < 0) 0.99F else progress
        } else {
            0F
        }
    } ?: 1F)
}