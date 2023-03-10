package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesTemporaryError
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Default implementation of [FetchNodesUpdateMapper].
 */
internal class FetchNodesUpdateMapperImpl @Inject constructor() : FetchNodesUpdateMapper {

    override fun invoke(request: MegaRequest, error: MegaError?) =
        FetchNodesUpdate(getProgress(request), getTemporaryError(error))

    private fun getProgress(request: MegaRequest) = with(request) {
        Progress(
            if (totalBytes > 0) {
                val progress = transferredBytes.toFloat() / totalBytes.toFloat()
                if (progress > 0.99 || progress < 0) 1F else progress
            } else {
                0F
            }
        )
    }

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