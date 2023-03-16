package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.shares.AccessPermissionIntMapper
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaNode
import javax.inject.Inject

internal class NodeShareKeyResultMapperImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
    private val accessPermissionIntMapper: AccessPermissionIntMapper,
) : NodeShareKeyResultMapper {
    override fun invoke(
        megaNode: MegaNode,
    ): suspend (AccessPermission, String) -> Unit {
        return { permission, email ->
            withContext(ioDispatcher) {
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("NodeShareKeyResult") {}
                    megaApiGateway.setShareAccess(
                        megaNode,
                        email,
                        accessPermissionIntMapper(permission),
                        listener,
                    )
                    continuation.invokeOnCancellation {
                        megaApiGateway.removeRequestListener(listener)
                    }
                }
            }
        }
    }
}

