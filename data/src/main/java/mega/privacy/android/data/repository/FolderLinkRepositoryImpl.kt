package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FolderLoginStatusMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FolderLinkRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Implementation of [FolderLinkRepository]
 */
class FolderLinkRepositoryImpl @Inject constructor(
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val folderLoginStatusMapper: FolderLoginStatusMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FolderLinkRepository {

    override suspend fun fetchNodes() = withContext(ioDispatcher) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request))
                    } else {
                        continuation.failWithError(error)
                    }
                }
            )
            megaApiFolderGateway.fetchNodes(listener)
            continuation.invokeOnCancellation {
                megaApiFolderGateway.removeRequestListener(listener)
            }
        }

    }

    override suspend fun loginToFolder(folderLink: String) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _: MegaRequest, error: MegaError ->
                    continuation.resumeWith(Result.success(folderLoginStatusMapper(error)))
                }
            )
            megaApiFolderGateway.loginToFolder(folderLink, listener)
            continuation.invokeOnCancellation {
                megaApiFolderGateway.removeRequestListener(listener)
            }
        }
    }
}