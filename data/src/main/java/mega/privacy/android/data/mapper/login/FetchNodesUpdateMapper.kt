package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesTemporaryError
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
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
     * @param error [MegaError]. If null means there is a request update or the request finished.
     */
    operator fun invoke(request: MegaRequest?, error: MegaError?) =
        FetchNodesUpdate(getProgress(request), getTemporaryError(error))

    private fun getProgress(request: MegaRequest?) = Progress(request?.run {
        if (totalBytes > 0) {
            val progress = transferredBytes.toFloat() / totalBytes.toFloat()
            if (progress > 0.99 || progress < 0) 0.99F else progress
        } else {
            0F
        }
    } ?: 1F)

    private fun getTemporaryError(error: MegaError?) = error?.let {
        when (it.errorCode) {
            MegaError.API_EAGAIN -> {
                when (it.value.toInt()) {
                    MegaApiJava.RETRY_CONNECTIVITY -> {
                        FetchNodesTemporaryError.ConnectivityIssues
                    }
                    MegaApiJava.RETRY_SERVERS_BUSY -> {
                        FetchNodesTemporaryError.ServerIssues
                    }
                    MegaApiJava.RETRY_API_LOCK -> {
                        FetchNodesTemporaryError.APILock
                    }
                    MegaApiJava.RETRY_RATE_LIMIT -> {
                        FetchNodesTemporaryError.APIRate
                    }
                    else -> {
                        FetchNodesTemporaryError.ServerIssues
                    }
                }
            }
            else -> {
                FetchNodesTemporaryError.ServerIssues
            }
        }
    }
}