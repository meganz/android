package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper from mega node to method to get thumbnail from server
 */
internal class ThumbnailFromServerMapper @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
) {
    /**
     * Mapper from mega node to method to get thumbnail from server
     * @param megaNode [MegaNode]
     * @return a suspend block to be executed to get thumbnail from server
     */
    operator fun invoke(
        megaNode: MegaNode,
    ): suspend (String) -> String {
        return { thumbnailPath ->
            withContext(ioDispatcher) {
                suspendCancellableCoroutine { continuation ->
                    val listener =
                        continuation.getRequestListener("ThumbnailFromServerMapper") { thumbnailPath }
                    megaApiGateway.getThumbnail(megaNode, thumbnailPath, listener)
                    continuation.invokeOnCancellation {
                        megaApiGateway.removeRequestListener(listener)
                    }
                }
            }
        }
    }
}
