package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileLinkRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Implementation of [FileLinkRepository]
 */
internal class FileLinkRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val nodeMapper: NodeMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FileLinkRepository {

    override suspend fun getPublicNode(url: String) = withContext(ioDispatcher) {
        val result = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(Result.success(request))
                        }

                        MegaError.API_EBLOCKED -> {
                            continuation.resumeWith(Result.failure(PublicNodeException.LinkRemoved()))
                        }

                        MegaError.API_ETOOMANY -> {
                            continuation.resumeWith(Result.failure(PublicNodeException.AccountTerminated()))
                        }

                        MegaError.API_EINCOMPLETE -> {
                            continuation.resumeWith(Result.failure(PublicNodeException.DecryptionKeyRequired()))
                        }

                        else -> {
                            continuation.resumeWith(Result.failure(PublicNodeException.GenericError()))
                        }
                    }
                }
            )
            megaApiGateway.getPublicNode(url, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }

        nodeMapper(result.publicMegaNode)
    }
}